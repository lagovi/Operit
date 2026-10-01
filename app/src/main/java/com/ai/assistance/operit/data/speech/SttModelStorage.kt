package com.ai.assistance.operit.data.speech

import android.content.Context
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.util.OperitPaths
import java.io.File

/**
 * Where the downloaded GigaAM model lives.
 *
 * The user asked for the option of keeping 214 MB on a microSD card rather than
 * in the phone's internal storage, so the location is a setting instead of a
 * constant. Three choices, in the order they are offered:
 *
 *  - [INTERNAL] `filesDir`. Always available, fastest random reads, and the only
 *    choice that survives nothing — it goes when the app is uninstalled.
 *  - [EXTERNAL_APP] `getExternalFilesDir(null)`. Needs no permission and is on the
 *    SD card whenever the user has moved app storage there, which is the common
 *    configuration on a phone with a removable card. Also removed on uninstall.
 *  - [EXTERNAL_SHARED] `Download/Operit/stt_model`. Visible in a file manager and
 *    survives uninstall, so a 214 MB download is not repeated after a reinstall.
 *    Needs the storage permission the app already requests.
 *
 * Reading the model off a removable card is slower than from internal storage,
 * but ONNX Runtime memory-maps the weights and only faults in the pages an
 * inference touches, so the cost is paid once at load rather than per utterance.
 *
 * Switching locations migrates the existing files instead of re-downloading, so
 * the user is never asked to fetch 214 MB twice.
 */
class SttModelStorage(private val context: Context) {

    enum class Location { INTERNAL, EXTERNAL_APP, EXTERNAL_SHARED }

    fun read(): Location {
        val raw = prefs.getString(KEY_LOCATION, null) ?: return Location.INTERNAL
        return runCatching { Location.valueOf(raw) }.getOrElse {
            AppLogger.e(TAG, "Unknown stored model location '$raw', falling back to internal", it)
            Location.INTERNAL
        }
    }

    fun write(location: Location) {
        prefs.edit().putString(KEY_LOCATION, location.name).apply()
    }

    /** Directory for the current location, created if needed. Null when unusable. */
    fun currentDir(): File? = dirFor(read())

    fun dirFor(location: Location): File? {
        val dir = when (location) {
            Location.INTERNAL -> File(context.filesDir, OperitPaths.MODEL_DIR_NAME)
            Location.EXTERNAL_APP -> context.getExternalFilesDir(null)
                ?.let { File(it, OperitPaths.MODEL_DIR_NAME) }
            Location.EXTERNAL_SHARED -> File(OperitPaths.operitRootDir(), OperitPaths.MODEL_DIR_NAME)
        } ?: return null
        return if (dir.isDirectory || dir.mkdirs()) dir else null
    }

    /**
     * Point the setting at [target] and move any model already downloaded.
     *
     * Returns false when files could not be moved, in which case the setting is
     * left pointing at the old location and the caller should report the failure
     * rather than silently leaving a half-copied directory behind.
     */
    fun migrateTo(target: Location): Boolean {
        val from = read()
        if (from == target) return true

        val source = dirFor(from) ?: return false
        val destination = dirFor(target) ?: return false
        if (source.canonicalPath == destination.canonicalPath) return true

        if (isPopulated(source) && !isPopulated(destination)) {
            val required = requiredBytes()
            val free = destination.usableSpaceBytes()
            if (free < required) {
                AppLogger.e(
                    TAG,
                    "Cannot move the model: $destination has ${free / 1048576} MiB free, " +
                        "needs ${required / 1048576} MiB"
                )
                return false
            }
            val moved = source.listFiles()?.all { file ->
                file.copyTo(File(destination, file.name), overwrite = true).isSuccess
            } ?: false
            if (!moved) {
                AppLogger.e(TAG, "Failed to move the model from $source to $destination")
                return false
            }
            source.listFiles()?.forEach { it.delete() }
        }

        write(target)
        return true
    }

    /** Bytes a complete model needs, used to refuse a move that cannot fit. */
    private fun requiredBytes(): Long =
        GigaAMModelFiles.ASSETS.sumOf { it.sizeBytes }

    private fun isPopulated(dir: File): Boolean =
        GigaAMModelFiles.ASSETS.all { File(dir, it.name).isFile }

    /** Visible label for the settings UI, including the resolved path. */
    fun describe(location: Location): String {
        val dir = dirFor(location)
        return if (dir == null) {
            context.getString(
                com.ai.assistance.operit.R.string.stt_storage_unavailable
            )
        } else {
            context.getString(
                com.ai.assistance.operit.R.string.stt_storage_location_value,
                locationLabel(location),
                dir.absolutePath
            )
        }
    }

    fun locationLabel(location: Location): String = when (location) {
        Location.INTERNAL -> context.getString(com.ai.assistance.operit.R.string.stt_storage_internal)
        Location.EXTERNAL_APP -> context.getString(com.ai.assistance.operit.R.string.stt_storage_external_app)
        Location.EXTERNAL_SHARED -> context.getString(com.ai.assistance.operit.R.string.stt_storage_external_shared)
    }

    /** True when the choice would keep the model off internal storage. */
    fun isExternal(location: Location): Boolean = location != Location.INTERNAL

    private val prefs
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun File.usableSpaceBytes(): Long = runCatching { usableSpace }.getOrDefault(0L)

    companion object {
        private const val TAG = "SttModelStorage"
        private const val PREFS_NAME = "stt_model_storage"
        private const val KEY_LOCATION = "location"
    }
}