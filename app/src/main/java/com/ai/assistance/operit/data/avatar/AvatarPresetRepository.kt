package com.ai.assistance.operit.data.avatar

import android.content.Context
import com.ai.assistance.operit.core.avatar.impl.gltf.model.GltfAvatarModel
import com.ai.assistance.operit.util.AppLogger
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Remote avatar preset catalog.
 *
 * The catalog lives in a public GitHub repository so presets never inflate the
 * APK. Only `presets.json` is fetched to show the gallery; a model is
 * downloaded when the user actually picks it, then verified against the
 * catalog sha256 before it reaches the renderer.
 */
object AvatarPresetRepository {

    const val DEFAULT_CATALOG_URL =
        "https://raw.githubusercontent.com/lagovi/OperitAvatarAssets/master/presets.json"

    private const val DEFAULT_BASE_URL =
        "https://raw.githubusercontent.com/lagovi/OperitAvatarAssets/master/"

    private const val TAG = "AvatarPresetRepository"

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .build()
    }

    @Serializable
    data class Catalog(
        val schema: String = "",
        val version: Int = 1,
        val updatedAt: String? = null,
        val presets: List<Preset> = emptyList()
    )

    @Serializable
    data class Preset(
        val id: String,
        val type: String,
        val name: String,
        val summary: String = "",
        val model: String,
        val license: String = "",
        @SerialName("licenseFile") val licenseFile: String? = null,
        val sizeBytes: Long = 0L,
        val sha256: String = "",
        val animations: List<String> = emptyList(),
        val defaultAnimation: String? = null,
        val thumbnail: String? = null
    ) {
        val isGltf: Boolean
            get() = type.equals("gltf", ignoreCase = true)
    }

    suspend fun fetchCatalog(
        catalogUrl: String = DEFAULT_CATALOG_URL
    ): Result<Catalog> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(catalogUrl).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Catalog request failed: HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                val catalog = json.decodeFromString(Catalog.serializer(), body)
                require(catalog.presets.isNotEmpty()) { "Catalog has no presets" }
                catalog
            }
        }.onFailure {
            AppLogger.w(TAG, "Failed to load avatar catalog from $catalogUrl", it)
        }
    }

    fun baseUrlFor(catalogUrl: String): String {
        val trimmed = catalogUrl.substringBeforeLast('/')
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    /**
     * Absolute URL for a preset thumbnail. Returns null when the preset has no
     * thumbnail, so the caller can draw a placeholder instead of requesting it.
     */
    fun thumbnailUrlFor(catalogUrl: String, preset: Preset): String? {
        val path = preset.thumbnail?.takeIf { it.isNotBlank() } ?: return null
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            baseUrlFor(catalogUrl) + path
        }
    }

    /**
     * Downloads a glTF preset into `filesDir/avatar_presets/<id>/` and verifies
     * the sha256 from the catalog. The verified file is what the renderer gets.
     */
    suspend fun downloadGltfPreset(
        context: Context,
        catalogUrl: String,
        preset: Preset,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<GltfAvatarModel> = withContext(Dispatchers.IO) {
        runCatching {
            require(preset.isGltf) { "Unsupported preset type: ${preset.type}" }
            require(preset.model.isNotBlank()) { "Preset has no model path" }

            val baseUrl = baseUrlFor(catalogUrl)
            val url = baseUrl + preset.model
            val destinationDir = File(context.filesDir, "avatar_presets/${preset.id}")
            destinationDir.mkdirs()
            val destination = File(destinationDir, "model.glb")

            if (destination.isFile && preset.sha256.isNotBlank()) {
                val existingHash = sha256Of(destination)
                if (existingHash.equals(preset.sha256, ignoreCase = true)) {
                    return@runCatching buildModel(preset, destination)
                }
                destination.delete()
            }

            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Download failed: HTTP ${response.code}")
                }
                val body = response.body ?: error("Download returned no body")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    destination.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, total)
                        }
                    }
                }
            }

            if (preset.sha256.isNotBlank()) {
                val actual = sha256Of(destination)
                check(actual.equals(preset.sha256, ignoreCase = true)) {
                    "Checksum mismatch for ${preset.id}: expected ${preset.sha256}, got $actual"
                }
            }

            writeLicense(context, baseUrl, preset)
            buildModel(preset, destination)
        }.onFailure {
            AppLogger.w(TAG, "Failed to download avatar preset ${preset.id}", it)
        }
    }

    fun downloadedModelOrNull(context: Context, preset: Preset): GltfAvatarModel? {
        val file = File(context.filesDir, "avatar_presets/${preset.id}/model.glb")
        if (!file.isFile) return null
        return buildModel(preset, file)
    }

    private suspend fun writeLicense(context: Context, baseUrl: String, preset: Preset) {
        val licenseFile = preset.licenseFile ?: return
        val destination = File(context.filesDir, "avatar_presets/${preset.id}/LICENSE.txt")
        runCatching {
            val request = Request.Builder().url(baseUrl + licenseFile).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { destination.writeText(it) }
                }
            }
        }.onFailure {
            AppLogger.w(TAG, "Failed to fetch license for ${preset.id}", it)
        }
    }

    private fun buildModel(preset: Preset, file: File): GltfAvatarModel {
        return GltfAvatarModel(
            id = preset.id,
            name = preset.name,
            basePath = file.parentFile?.absolutePath.orEmpty(),
            modelFile = file.name,
            defaultAnimation = preset.defaultAnimation,
            declaredAnimationNames = preset.animations
        )
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
