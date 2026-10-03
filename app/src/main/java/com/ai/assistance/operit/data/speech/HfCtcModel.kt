package com.ai.assistance.operit.data.speech

import com.ai.assistance.operit.api.speech.CtcModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * One file inside a Hugging Face repo, as reported by the Hub API.
 *
 * @param sha256 content hash for LFS files, empty when the Hub has none
 *   (small text files like `tokens.txt` are usually stored inline in git,
 *   so the API reports no digest for them).
 */
data class HfModelFile(
    val path: String,
    val sizeBytes: Long,
    val sha256: String,
    val downloadUrl: String,
)

/**
 * A resolved add-candidate: exactly one acoustic checkpoint plus exactly one
 * token table. Resolution refuses to guess — a repo with several `.onnx`
 * files needs a direct file link instead.
 */
data class HfCtcCandidate(
    val repoId: String,
    val revision: String,
    val modelFile: HfModelFile,
    val tokensFile: HfModelFile,
)

/**
 * A user-added local checkpoint, pinned so later upstream edits cannot
 * silently swap the bytes under a working setup.
 *
 * The acoustic file is verified against the LFS digest from the Hub API.
 * The token table usually has no server-side digest, so its SHA-256 is
 * pinned from what was observed at add time (trust on first use of a repo
 * the user explicitly chose) and re-checked on every later download.
 */
@Serializable
data class CustomSttModel(
    val id: String,
    val displayName: String,
    val repoId: String,
    val revision: String,
    val modelPath: String,
    val tokensPath: String,
    val modelSizeBytes: Long,
    val modelSha256: String,
    val tokensSha256: String,
    val config: CtcModelConfig = CtcModelConfig.GIGAAM_DEFAULT,
    val addedAtEpochMs: Long = 0L,
)

/** A pasted link, split into the repo, the pinned revision if any, and the file it points at. */
data class ParsedHfLink(
    val repoId: String,
    val revision: String?,
    val preferredPath: String?,
)

/**
 * Turns a pasted link into a pinned [HfCtcCandidate] via the public Hub API.
 *
 * Accepts a bare `owner/repo`, a repo page, or a direct file page
 * (`/blob/<rev>/<path>`); the revision defaults to `main`. Only public repos
 * work — there is no token to offer a private one, and failing loudly here
 * beats a mysterious 401 mid-download.
 */
class HfModelResolver(private val http: OkHttpClient = OkHttpClient()) {

    /**
     * @return the repo id plus the revision pinned by the link, if any
     */
    fun parseLink(raw: String): Result<ParsedHfLink> {
        val input = raw.trim().trimEnd('/')
        if (input.isEmpty()) {
            return Result.failure(IllegalArgumentException("empty link"))
        }
        val withoutScheme = input
            .removePrefix("https://")
            .removePrefix("http://")
        val withoutHost = if (withoutScheme.startsWith("huggingface.co/")) {
            withoutScheme.removePrefix("huggingface.co/")
        } else {
            withoutScheme
        }
        val parts = withoutHost.split('/').filter { it.isNotEmpty() }
        if (parts.size < 2 || parts.any { it == "." || it == ".." }) {
            return Result.failure(
                IllegalArgumentException("expected owner/repo or a huggingface.co link")
            )
        }
        val repoId = "${parts[0]}/${parts[1]}"
        if (!REPO_ID.matches(repoId)) {
            return Result.failure(IllegalArgumentException("not a valid repo id: $repoId"))
        }
        // /blob/<rev>/<path...> or /resolve/<rev>/<path...> pins both.
        val revision = if (parts.size > 3 && (parts[2] == "blob" || parts[2] == "resolve")) {
            parts[3].takeIf { it.isNotEmpty() }
        } else {
            null
        }
        val preferredPath = if (parts.size > 4 && (parts[2] == "blob" || parts[2] == "resolve")) {
            parts.drop(4).joinToString("/").takeIf { it.isNotEmpty() }
        } else {
            null
        }
        return Result.success(
            ParsedHfLink(repoId = repoId, revision = revision, preferredPath = preferredPath)
        )
    }

    /**
     * Lists the repo and picks the checkpoint plus the token table.
     *
     * The acoustic file must carry an LFS digest and size, otherwise the later
     * download could not be verified and the candidate is rejected. The token
     * table is matched by exact filename, preferring a sibling of the model
     * file's directory when several exist.
     */
    suspend fun resolve(link: String, revision: String? = null): Result<HfCtcCandidate> =
        withContext(Dispatchers.IO) {
            val parsed = parseLink(link).getOrElse { return@withContext Result.failure(it) }
            val repoId = parsed.repoId
            val preferredPath = parsed.preferredPath
            val rev = revision ?: parsed.revision ?: DEFAULT_REVISION
            val siblings = listFiles(repoId, rev).getOrElse { return@withContext Result.failure(it) }

            val onnxFiles = siblings.filter {
                it.path.endsWith(".onnx", ignoreCase = true)
            }
            if (onnxFiles.isEmpty()) {
                return@withContext Result.failure(
                    IOException("no .onnx file in $repoId@$rev")
                )
            }
            val modelInfo = if (preferredPath != null) {
                onnxFiles.firstOrNull { it.path == preferredPath }
                    ?: return@withContext Result.failure(
                        IOException("$preferredPath is not an .onnx file of $repoId@$rev")
                    )
            } else if (onnxFiles.size > 1) {
                return@withContext Result.failure(
                    IOException(
                        "several checkpoints in $repoId@$rev; " +
                            "paste a direct link to one .onnx file: " +
                            onnxFiles.take(5).joinToString { it.path }
                    )
                )
            } else {
                onnxFiles.single()
            }
            if (modelInfo.sha256.isEmpty() || modelInfo.sizeBytes <= 0) {
                return@withContext Result.failure(
                    IOException(
                        "${modelInfo.path} has no content digest on the Hub; " +
                            "only LFS-stored checkpoints can be verified after download"
                    )
                )
            }
            val tokensInfo = pickTokens(siblings, modelInfo)
                ?: return@withContext Result.failure(
                    IOException("no tokens.txt next to ${modelInfo.path} in $repoId@$rev")
                )
            Result.success(
                HfCtcCandidate(
                    repoId = repoId,
                    revision = rev,
                    modelFile = modelInfo,
                    tokensFile = tokensInfo,
                )
            )
        }

    private fun listFiles(repoId: String, revision: String): Result<List<HfModelFile>> {
        // blobs=true is what makes the Hub attach the LFS digests; without
        // it every file looks unverifiable. Seen live on device.
        val url = "https://huggingface.co/api/models/$repoId/revision/$revision?blobs=true"
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        return try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(
                        IOException("hub lookup failed for $repoId@$revision: HTTP ${response.code}")
                    )
                }
                val body = response.body?.string().orEmpty()
                Result.success(parseSiblings(repoId, revision, body))
            }
        } catch (e: Exception) {
            Result.failure(IOException("hub lookup failed for $repoId@$revision", e))
        }
    }

    internal fun parseSiblings(repoId: String, revision: String, body: String): List<HfModelFile> {
        // A truncated or HTML body is not a file listing; the caller reports
        // "no .onnx file", which points at the repo rather than at JSON.
        val siblings = runCatching {
            JSONObject(body).optJSONArray("siblings")
        }.getOrNull() ?: return emptyList()
        return (0 until siblings.length()).mapNotNull { i ->
            val entry = siblings.optJSONObject(i) ?: return@mapNotNull null
            val path = entry.optString("rfilename").takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            val lfs = entry.optJSONObject("lfs")
            // Current Hub shape is {"sha256", "size"}; "oid" is the older
            // name for the same digest, kept as a fallback.
            val digest = lfs?.optString("sha256").orEmpty()
                .ifEmpty { lfs?.optString("oid").orEmpty() }
            HfModelFile(
                path = path,
                sizeBytes = lfs?.optLong("size", -1L)?.takeIf { it >= 0 }
                    ?: entry.optLong("size", -1L).takeIf { it >= 0 }
                    ?: -1L,
                sha256 = digest,
                downloadUrl = "https://huggingface.co/$repoId/resolve/$revision/$path",
            )
        }
    }

    private fun pickTokens(siblings: List<HfModelFile>, model: HfModelFile): HfModelFile? {
        val tables = siblings.filter {
            it.path.substringAfterLast('/').equals("tokens.txt", ignoreCase = true)
        }
        if (tables.isEmpty()) return null
        if (tables.size == 1) return tables.single()
        // Prefer the table sitting next to the checkpoint.
        val modelDir = model.path.substringBeforeLast('/', "")
        return tables.firstOrNull { it.path.substringBeforeLast('/', "") == modelDir }
            ?: tables.firstOrNull { it.path.substringBeforeLast('/', "").isEmpty() }
            ?: tables.first()
    }

    private companion object {
        const val DEFAULT_REVISION = "main"
        val REPO_ID = Regex("[A-Za-z0-9._-]+/[A-Za-z0-9._-]+")
    }
}
