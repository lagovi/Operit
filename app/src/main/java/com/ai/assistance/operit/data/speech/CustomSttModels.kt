package com.ai.assistance.operit.data.speech

import android.content.Context
import com.ai.assistance.operit.api.speech.CtcModelConfig
import com.ai.assistance.operit.api.speech.GigaAMRecognizer
import com.ai.assistance.operit.api.speech.GigaAMTokenVocabulary
import com.ai.assistance.operit.util.AppDataLocation
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.RemoteAsset
import com.ai.assistance.operit.util.RemoteAssetFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * User-added local checkpoints, kept next to the built-in model under
 * `stt_model/custom/<id>` so a location move carries them along.
 *
 * Adding is a small pipeline, and every stage can say no:
 * 1. the token table downloads first, because it is tiny;
 * 2. its SHA-256 is pinned into the entry (trust on first use);
 * 3. the table must parse as `<symbol> <id>` with contiguous ids;
 * 4. the graph must open and emit exactly one logit row per token id on
 *    synthetic input — a vocab mismatch is the common silent-garbage
 *    failure, and this catches it before the entry exists.
 *
 * Only then is the entry stored; the heavy acoustic file downloads lazily
 * on first selection through the shared verified fetcher.
 */
class CustomSttModels(private val context: Context) {

    private val locations = AppDataLocation(context)
    private val fetcher = RemoteAssetFetcher(context)
    private val http = okhttp3.OkHttpClient()
    private val store = Json { ignoreUnknownKeys = true }

    fun list(): List<CustomSttModel> = readRegistry().sortedBy { it.addedAtEpochMs }

    fun get(id: String): CustomSttModel? = readRegistry().firstOrNull { it.id == id }

    /** Directory holding this entry's files under [location], if usable. */
    fun dirFor(entry: CustomSttModel, location: AppDataLocation.Location): File? =
        locations.dirFor(location, "$CUSTOM_LEAF/${entry.id}")

    /** The entry's files under the current location, or null when incomplete. */
    fun downloadedDir(entry: CustomSttModel): File? {
        val dir = dirFor(entry, locations.read()) ?: return null
        val model = File(dir, MODEL_FILE_NAME)
        if (!model.isFile || model.length() != entry.modelSizeBytes) return null
        val tokens = File(dir, TOKENS_FILE_NAME)
        if (!tokens.isFile || !pinsMatch(tokens, entry.tokensSha256)) return null
        return dir
    }

    /**
     * Runs the add pipeline for a resolved candidate. The display name
     * defaults to the checkpoint filename without its extension.
     */
    suspend fun addFromCandidate(
        candidate: HfCtcCandidate,
        displayName: String? = null,
        config: CtcModelConfig = CtcModelConfig.GIGAAM_DEFAULT,
    ): Result<CustomSttModel> = withContext(Dispatchers.IO) {
        try {
            val id = idFor(candidate)
            if (get(id) != null) {
                return@withContext Result.failure(
                    IOException("${candidate.repoId} is already added")
                )
            }
            val dir = dirFor(
                CustomSttModel(
                    id = id,
                    displayName = "",
                    repoId = candidate.repoId,
                    revision = candidate.revision,
                    modelPath = candidate.modelFile.path,
                    tokensPath = candidate.tokensFile.path,
                    modelSizeBytes = candidate.modelFile.sizeBytes,
                    modelSha256 = candidate.modelFile.sha256,
                    tokensSha256 = "",
                ),
                locations.read(),
            ) ?: return@withContext Result.failure(
                IOException("no usable storage for the model")
            )

            // Stage 1+2: the tiny token table first, then pin what arrived.
            // The shared fetcher cannot do this one: small git-stored files
            // carry no size or digest on the Hub, and it requires both.
            val tokensFile = File(dir, TOKENS_FILE_NAME)
            dir.mkdirs()
            downloadSmall(candidate.tokensFile.downloadUrl, tokensFile).getOrElse {
                return@withContext Result.failure(it)
            }

            // Stage 3: the table must be a real vocabulary.
            val vocabulary = try {
                GigaAMTokenVocabulary.parse(
                    tokensFile.inputStream(),
                    blankOverride = config.blankId,
                )
            } catch (e: Exception) {
                tokensFile.delete()
                return@withContext Result.failure(
                    IOException("${candidate.tokensFile.path} is not a <symbol> <id> token table", e)
                )
            }

            val entry = CustomSttModel(
                id = id,
                displayName = displayName?.trim().orEmpty().ifEmpty {
                    candidate.modelFile.path.substringAfterLast('/').removeSuffix(".onnx")
                },
                repoId = candidate.repoId,
                revision = candidate.revision,
                modelPath = candidate.modelFile.path,
                tokensPath = candidate.tokensFile.path,
                modelSizeBytes = candidate.modelFile.sizeBytes,
                modelSha256 = candidate.modelFile.sha256,
                tokensSha256 = sha256(tokensFile),
                config = config,
                addedAtEpochMs = System.currentTimeMillis(),
            )

            // The acoustic file downloads lazily, but it is big and the user
            // is waiting on this screen, so start it right away.
            ensureAcoustic(entry).getOrElse { return@withContext Result.failure(it) }

            // Stage 4: the graph must agree with the table before the entry exists.
            try {
                verifyGraphAgreesWithVocabulary(entry, vocabulary.size)
            } catch (e: Exception) {
                File(dir, MODEL_FILE_NAME).delete()
                return@withContext Result.failure(e)
            }

            writeRegistry(readRegistry() + entry)
            Result.success(entry)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Cannot add ${candidate.repoId}", e)
            Result.failure(e)
        }
    }

    /** Downloads (or resumes) the acoustic file, verified by LFS digest. */
    suspend fun ensureAcoustic(
        entry: CustomSttModel,
        progress: RemoteAssetFetcher.ProgressListener? = null,
    ): Result<File> {
        val dir = dirFor(entry, locations.read())
            ?: return Result.failure(IOException("no usable storage for the model"))
        val downloadUrl =
            "https://huggingface.co/${entry.repoId}/resolve/${entry.revision}/${entry.modelPath}"
        return fetcher.ensureAssets(
            targetDir = dir,
            assets = listOf(
                RemoteAsset(
                    name = MODEL_FILE_NAME,
                    url = downloadUrl,
                    sizeBytes = entry.modelSizeBytes,
                    sha256 = entry.modelSha256,
                )
            ),
            progress = progress,
        )
    }

    /**
     * Opens the graph on synthetic silence and checks the output width
     * against the token table. Zero frames of real audio involved: this is
     * a shape handshake, not a quality judgement.
     */
    private fun verifyGraphAgreesWithVocabulary(entry: CustomSttModel, vocabSize: Int) {
        val dir = dirFor(entry, locations.read())
            ?: throw IOException("no usable storage for the model")
        val recognizer = GigaAMRecognizer.openCustom(
            modelFile = File(dir, MODEL_FILE_NAME),
            tokensFile = File(dir, TOKENS_FILE_NAME),
            config = entry.config,
        )
        try {
            val frames = PROBE_FRAMES
            val features = FloatArray(entry.config.nMels * frames)
            val logProbs = recognizer.run(features, frames)
                ?: throw IOException(
                    "${entry.repoId}: the graph returned no output for $frames frames"
                )
            val encodedFrames = (frames + entry.config.subsamplingFactor - 1) /
                entry.config.subsamplingFactor
            if (logProbs.size != encodedFrames * vocabSize) {
                throw IOException(
                    "${entry.repoId}: the graph emits ${logProbs.size / encodedFrames} " +
                        "logits per frame but the token table holds $vocabSize ids; " +
                        "this checkpoint does not match its tokens.txt"
                )
            }
        } finally {
            recognizer.close()
        }
    }

    /** Removes the entry and deletes its files. Unknown ids are a no-op. */
    suspend fun remove(id: String): Unit = withContext(Dispatchers.IO) {
        val entry = get(id)
        writeRegistry(readRegistry().filterNot { it.id == id })
        entry?.let { dirFor(it, locations.read())?.deleteRecursively() }
    }

    /**
     * Moves every downloaded entry from [source] to [target], mirroring
     * [SttModelStorage]. Entries that were never downloaded have no files
     * and need no move. The source is a parameter rather than the live
     * setting because the built-in move flips the setting first.
     */
    fun migrateTo(
        source: AppDataLocation.Location,
        target: AppDataLocation.Location,
    ): Boolean {
        if (source == target) return true
        var ok = true
        for (entry in readRegistry()) {
            val from = dirFor(entry, source)
            if (from == null || !File(from, MODEL_FILE_NAME).isFile) continue
            val to = locations.dirFor(target, "$CUSTOM_LEAF/${entry.id}")
            if (to == null) {
                ok = false
                continue
            }
            to.mkdirs()
            for (name in listOf(MODEL_FILE_NAME, TOKENS_FILE_NAME)) {
                val source = File(from, name)
                if (source.isFile && !source.renameTo(File(to, name))) {
                    ok = false
                }
            }
        }
        return ok
    }

    private fun idFor(candidate: HfCtcCandidate): String {
        val base = "${candidate.repoId}@${candidate.revision}:${candidate.modelFile.path}"
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(80)
        return base.ifEmpty { "custom-${System.currentTimeMillis()}" }
    }

    private fun pinsMatch(file: File, sha256: String): Boolean {
        if (sha256.isEmpty()) return file.isFile
        return sha256(file).equals(sha256, ignoreCase = true)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * A few kilobytes at most, so no resume and no progress: one GET whose
     * body must be small and non-empty, otherwise the link is wrong.
     */
    private fun downloadSmall(url: String, destination: File): Result<Unit> {
        return try {
            okhttp3.Request.Builder().url(url).build().let { request ->
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return Result.failure(
                            IOException("download failed for $url: HTTP ${response.code}")
                        )
                    }
                    val body = response.body?.bytes()
                    if (body == null || body.isEmpty() || body.size > MAX_SMALL_BYTES) {
                        return Result.failure(
                            IOException("unexpected token table at $url")
                        )
                    }
                    destination.writeBytes(body)
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("download failed for $url", e))
        }
    }

    private fun registryFile(): File = File(context.filesDir, REGISTRY_PATH)

    private fun readRegistry(): List<CustomSttModel> {
        val file = registryFile()
        if (!file.isFile) return emptyList()
        return try {
            store.decodeFromString(ListSerializer(CustomSttModel.serializer()), file.readText())
        } catch (e: Exception) {
            AppLogger.e(TAG, "Cannot read the custom model registry", e)
            emptyList()
        }
    }

    private fun writeRegistry(entries: List<CustomSttModel>) {
        val file = registryFile()
        file.parentFile?.mkdirs()
        file.writeText(store.encodeToString(ListSerializer(CustomSttModel.serializer()), entries))
    }

    companion object {
        const val MODEL_FILE_NAME = "model.onnx"
        const val TOKENS_FILE_NAME = "tokens.txt"

        private const val TAG = "CustomSttModels"
        private const val REGISTRY_PATH = "stt_custom/registry.json"
        private const val CUSTOM_LEAF = "stt_model/custom"
        private const val PROBE_FRAMES = 400
        private const val MAX_SMALL_BYTES = 4 * 1024 * 1024
    }
}
