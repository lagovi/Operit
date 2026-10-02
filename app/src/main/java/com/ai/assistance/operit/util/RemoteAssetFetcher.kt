package com.ai.assistance.operit.util

import android.content.Context
import com.ai.assistance.operit.R
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * One file of an on-demand payload, described up front so the app can decide
 * whether to fetch it and can tell a complete download from a partial one.
 */
data class RemoteAsset(
        val name: String,
        val url: String,
        val sizeBytes: Long,
        val sha256: String
)

/**
 * Fetch a set of [RemoteAsset]s into a target directory on first use.
 *
 * Fork: several large payloads used to be bundled in the APK, which made every
 * install pay for features most users never open. The model download manager
 * already did this for MNN checkpoints, but each feature had its own copy of the
 * logic and no way to resume. This is the shared version: resumable via HTTP
 * Range, integrity-checked per file, and idempotent, so an interrupted download
 * is continued rather than restarted.
 *
 * A file is considered present only when it exists at the expected size AND
 * hashes correctly. Anything else is refetched. There is no partial-success
 * state: either every file is verified or the caller gets a failure it must
 * surface, because a half-present model set is not a state a recognizer can use.
 */
class RemoteAssetFetcher(private val context: Context) {

    private val client = OkHttpClient.Builder().retryOnConnectionFailure(true).build()

    /** Progress callback, invoked on [Dispatchers.IO]. */
    fun interface ProgressListener {
        fun onProgress(downloadedBytes: Long, totalBytes: Long, currentFile: String)
    }

    /**
     * Ensure every asset in [assets] exists under [targetDir] and is intact.
     *
     * @param targetDir directory the assets are materialised into
     * @param assets the files to fetch
     * @param progress optional progress callback
     * @return the target directory, or a failure describing what could not be fetched
     */
    suspend fun ensureAssets(
            targetDir: File,
            assets: List<RemoteAsset>,
            progress: ProgressListener? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            targetDir.mkdirs()
            if (!targetDir.isDirectory) {
                return@withContext Result.failure(
                        IOException(context.getString(R.string.stt_model_dir_unavailable))
                )
            }

            val missing = assets.filterNot { isIntact(File(targetDir, it.name), it) }
            if (missing.isEmpty()) {
                return@withContext Result.success(targetDir)
            }

            val totalBytes = assets.sumOf { it.sizeBytes }
            var downloadedBytes = assets.sumOf { it.sizeBytes } - missing.sumOf { it.sizeBytes }

            for (asset in missing) {
                val destination = File(targetDir, asset.name)
                if (!download(asset, destination)) {
                    return@withContext Result.failure(
                            IOException(
                                    context.getString(R.string.stt_model_download_failed, asset.name)
                            )
                    )
                }
                downloadedBytes += asset.sizeBytes
                progress?.onProgress(downloadedBytes, totalBytes, asset.name)
            }

            Result.success(targetDir)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to provision remote assets", e)
            Result.failure(e)
        }
    }

    private fun isIntact(file: File, asset: RemoteAsset): Boolean {
        if (!file.isFile || file.length() != asset.sizeBytes) return false
        return sha256(file) == asset.sha256.lowercase()
    }

    private suspend fun CoroutineScope.download(asset: RemoteAsset, destination: File): Boolean {
        val partial = File(destination.parentFile, "${destination.name}$PARTIAL_SUFFIX")
        val alreadyHave = if (partial.isFile) partial.length() else 0L

        val request =
                Request.Builder()
                        .url(asset.url)
                        .apply {
                            if (alreadyHave > 0) {
                                header("Range", "bytes=$alreadyHave-")
                            }
                        }
                        .build()

        val response =
                try {
                    client.newCall(request).execute()
                } catch (e: IOException) {
                    AppLogger.e(TAG, "Request failed for ${asset.name}", e)
                    return false
                }

        // 206 means the server honoured our range and the partial file is still
        // valid. 200 means it sent the whole body, so the partial must be replaced.
        val appending = response.code == 206 && alreadyHave > 0
        if (!response.isSuccessful && response.code != 206) {
            response.close()
            AppLogger.e(TAG, "HTTP ${response.code} for ${asset.name}")
            return false
        }

        return try {
            response.body?.byteStream()?.use { input ->
                partial.outputStream().let { output ->
                    if (appending) {
                        output.channel.position(alreadyHave)
                    }
                    // Chunked instead of copyTo() so cancelling the coroutine
                    // actually stops the transfer. The partial file stays and
                    // the next run resumes it with Range, so cancel is free.
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return false

            if (partial.length() != asset.sizeBytes) {
                AppLogger.e(
                        TAG,
                        "Size mismatch for ${asset.name}: got ${partial.length()}, expected ${asset.sizeBytes}"
                )
                return false
            }

            val digest = sha256(partial)
            if (digest != asset.sha256.lowercase()) {
                AppLogger.e(TAG, "Checksum mismatch for ${asset.name}: $digest")
                return false
            }

            if (!partial.renameTo(destination)) {
                AppLogger.e(TAG, "Unable to move verified download into place: ${asset.name}")
                return false
            }
            true
        } catch (e: IOException) {
            AppLogger.e(TAG, "Transfer failed for ${asset.name}", e)
            false
        } finally {
            response.close()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream: InputStream ->
            val buffer = ByteArray(BUFFER_SIZE)
            var read = stream.read(buffer)
            while (read > 0) {
                digest.update(buffer, 0, read)
                read = stream.read(buffer)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        private const val TAG = "RemoteAssetFetcher"
        private const val BUFFER_SIZE = 64 * 1024
        private const val PARTIAL_SUFFIX = ".part"
    }
}
