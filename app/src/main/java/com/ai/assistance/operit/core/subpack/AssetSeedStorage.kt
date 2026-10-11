package com.ai.assistance.operit.core.subpack

import android.content.Context
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.RemoteAsset
import com.ai.assistance.operit.util.RemoteAssetFetcher
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

/**
 * Seed trees (`templates/`, `emoji/`): each ships as one zip in the fork's
 * release, downloads into `filesDir/seed/` on first use and unpacks next to
 * the zip. Readers work off the unpacked tree as plain files — the AssetManager
 * tree walk is gone together with the bundled copies.
 *
 * Freshness: `.seed-ok` inside the tree holds the zip's sha256; a mismatch
 * (new payload version) re-unpacks. A failure is returned, never thrown:
 * callers own the error surface (startup seeding skips, project creation
 * shows the dialog error).
 */
class AssetSeedStorage(private val context: Context) {

    companion object {
        private const val TAG = "AssetSeedStorage"
        private const val SEED_DIR = "seed"
        private const val MARKER_SUFFIX = ".seed-ok"
    }

    /**
     * Ensures the unpacked tree for [asset] exists and returns its root
     * (e.g. `filesDir/seed/templates/` for `templates.zip`).
     */
    suspend fun ensureSeed(
        asset: RemoteAsset,
        progress: RemoteAssetFetcher.ProgressListener? = null
    ): Result<File> {
        val seedDir = File(context.filesDir, SEED_DIR)
        val zipResult = RemoteAssetFetcher(context)
            .ensureAssets(seedDir, listOf(asset), progress)
        if (zipResult.isFailure) return Result.failure(zipResult.exceptionOrNull()!!)
        val zipFile = File(seedDir, asset.name)
        val treeRoot = File(seedDir, asset.name.removeSuffix(".zip"))
        // Marker lives NEXT TO the tree, not inside it: readers list the
        // tree root and must never see bookkeeping files.
        val marker = File(seedDir, asset.name + MARKER_SUFFIX)
        return try {
            if (!isFresh(treeRoot, marker, asset.sha256)) {
                unpack(zipFile, treeRoot)
                marker.writeText(asset.sha256)
            }
            Result.success(treeRoot)
        } catch (e: IOException) {
            AppLogger.e(TAG, "Failed to unpack seed ${asset.name}", e)
            Result.failure(e)
        }
    }

    private fun isFresh(treeRoot: File, marker: File, sha256: String): Boolean {
        return treeRoot.isDirectory && marker.isFile &&
            runCatching { marker.readText().trim() }.getOrNull() == sha256
    }

    private fun unpack(zipFile: File, treeRoot: File) {
        if (treeRoot.exists()) treeRoot.deleteRecursively()
        treeRoot.mkdirs()
        val rootPath = treeRoot.canonicalPath
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                // Zip-slip guard: every entry must stay inside the tree.
                val target = File(treeRoot, entry.name)
                if (!target.canonicalPath.startsWith("$rootPath/") &&
                    target.canonicalPath != rootPath
                ) {
                    throw IOException("Seed entry escapes the tree: ${entry.name}")
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
        AppLogger.d(TAG, "Seed ${zipFile.name} unpacked to ${treeRoot.absolutePath}")
    }
}
