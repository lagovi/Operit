package com.ai.assistance.operit.core.avatar.impl.gltf.view

import android.content.Context
import com.ai.assistance.operit.util.AppLogger
import org.json.JSONObject
import java.io.File

/**
 * Live tuning for the glTF preview, so that framing and posing hypotheses can be tested on a
 * device without rebuilding the app.
 *
 * The values live in a JSON file inside the app's files directory. The renderer re-reads the file
 * every time it reloads a model and whenever the surface is tapped, so an edit made through adb
 * takes effect immediately:
 *
 *     adb shell "run-as com.ai.assistance.operit.debug sh -c \
 *       'cat > files/gltf_tuner.json'" < local.json
 *     adb shell input tap <x> <y>     # on the preview
 *
 * Only the fields present in the file are applied; everything absent keeps its normal value, so a
 * tuner file with one key changes exactly one thing. This exists to answer questions like "does
 * framing from the mesh or from the bones put the model in view", and is deliberately a plain file
 * rather than app state: it must survive process restarts and must not leak into the shipped
 * defaults.
 */
object GltfTuner {

    private const val TAG = "GltfTuner"
    private const val FILE_NAME = "gltf_tuner.json"

    /**
     * Bounds source for the camera fit. `mesh` uses `asset.boundingBox`, `bones` uses the world
     * positions of the joints, `auto` picks bones for skinned assets and mesh otherwise.
     */
    enum class BoundsSource { AUTO, MESH, BONES }

    data class Settings(
        val boundsSource: BoundsSource = BoundsSource.AUTO,
        /** Multiply the computed fit distance. 1.0 leaves it as computed. */
        val distanceMultiplier: Float? = null,
        /** Vertical field of view in degrees, used when fitting. */
        val verticalFovDegrees: Float? = null,
        /** Extra margin around the model, as a multiplier of the fit distance. */
        val fitMargin: Float? = null,
        /** Override the camera target height in model units, instead of the bounds centre. */
        val targetY: Float? = null,
        /** Override the target X and Z in model units. */
        val targetX: Float? = null,
        val targetZ: Float? = null,
        /** Skip transformToUnitCube when true. */
        val skipUnitCube: Boolean? = null,
        /** Disable the model-space orbit fallback when true, relying on the camera orbit alone. */
        val disableModelOrbit: Boolean? = null
    ) {
        /** True when the file asked for something, so the renderer can log that it is in test mode. */
        val isActive: Boolean
            get() =
                distanceMultiplier != null ||
                    verticalFovDegrees != null ||
                    fitMargin != null ||
                    targetY != null ||
                    targetX != null ||
                    targetZ != null ||
                    skipUnitCube != null ||
                    disableModelOrbit != null ||
                    boundsSource != BoundsSource.AUTO
    }

    @Volatile
    private var cached: Settings = Settings()

    @Volatile
    private var cachedStamp: Long = -1L

    fun fileFor(context: Context): File = File(context.filesDir, FILE_NAME)

    /** Re-reads the file when it changed on disk since the last call. */
    fun settings(context: Context): Settings {
        val file = fileFor(context)
        if (!file.isFile) {
            cached = Settings()
            cachedStamp = -1L
            return cached
        }
        // Timestamp alone is not enough: a rewrite within the same second keeps lastModified
        // while the content changes, so the size participates in the key as well.
        val stamp = file.lastModified() * 31 + file.length()
        if (stamp == cachedStamp) {
            return cached
        }
        cached = runCatching { parse(file.readText()) }
            .onFailure { error -> AppLogger.w(TAG, "Ignoring invalid tuner file: ${error.message}") }
            .getOrDefault(Settings())
        cachedStamp = stamp
        if (cached.isActive) {
            AppLogger.i(TAG, "Applied live tuner: $cached")
        }
        return cached
    }

    /** Forces the next [settings] call to re-read the file, used on a preview tap. */
    fun invalidate() {
        cachedStamp = -1L
        cached = Settings()
    }

    private fun parse(text: String): Settings {
        if (text.isBlank()) {
            return Settings()
        }
        val root = JSONObject(text)
        val source =
            root.optString("boundsSource").trim().lowercase().let { value ->
                when (value) {
                    "mesh" -> BoundsSource.MESH
                    "bones" -> BoundsSource.BONES
                    "auto" -> BoundsSource.AUTO
                    "" -> BoundsSource.AUTO
                    else -> BoundsSource.AUTO
                }
            }
        return Settings(
            boundsSource = source,
            distanceMultiplier = root.optNullableFloat("distanceMultiplier"),
            verticalFovDegrees = root.optNullableFloat("verticalFovDegrees"),
            fitMargin = root.optNullableFloat("fitMargin"),
            targetY = root.optNullableFloat("targetY"),
            targetX = root.optNullableFloat("targetX"),
            targetZ = root.optNullableFloat("targetZ"),
            skipUnitCube = root.optNullableBoolean("skipUnitCube"),
            disableModelOrbit = root.optNullableBoolean("disableModelOrbit")
        )
    }

    private fun JSONObject.optNullableFloat(key: String): Float? {
        if (!has(key) || isNull(key)) {
            return null
        }
        val value = optDouble(key, Double.NaN)
        return if (value.isNaN()) null else value.toFloat()
    }

    private fun JSONObject.optNullableBoolean(key: String): Boolean? {
        if (!has(key) || isNull(key)) {
            return null
        }
        return optBoolean(key)
    }
}
