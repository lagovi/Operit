package com.ai.assistance.operit.util

import android.content.Context
import android.os.Environment
import com.ai.assistance.operit.R
import java.io.File

/**
 * Where the app keeps its bulky, user-supplied files.
 *
 * The user asked for large payloads to live on a memory card rather than in the
 * phone's internal storage, so the choice is a setting and not a constant. It
 * applies to every payload — the speech model, the terminal root filesystem, the
 * APK and Windows subpackages, the helper APKs — so that switching once moves
 * all of them rather than one at a time.
 *
 *  - [INTERNAL] `filesDir`. Always available, fastest random reads. Removed when
 *    the app is uninstalled.
 *  - [EXTERNAL_APP] `getExternalFilesDir(null)`. No permission needed, removed
 *    on uninstall. Despite the name this is usually the phone's own emulated
 *    storage, not a memory card.
 *  - [SDCARD_APP] the app folder on the removable memory card, when one is
 *    mounted. No permission needed, removed on uninstall. This is the option
 *    the memory-card setting is for; [EXTERNAL_APP] alone does not reach it.
 *  - [EXTERNAL_SHARED] `Download/Operit`. Visible in a file manager and survives
 *    reinstall, so re-downloading hundreds of megabytes after an update is not
 *    necessary. Needs the storage permission the app already requests.
 *
 * What cannot be moved here: native libraries. The dynamic linker resolves them
 * from the package manager's own `libDir` under `/data/app`, which external
 * storage is not, so `lib/` stays in the APK no matter what this is set to.
 * Also excluded: dex, and anything the app reads through `AssetManager`, which
 * only ever sees the APK. Payloads that need to move therefore have to be
 * unpacked into the APK's asset directory during the build, which is what
 * `sync_example_packages.py` and the subpack archive already do upstream — this
 * class only governs where the *unpacked* copies live at runtime.
 */
class AppDataLocation(private val context: Context) {

    enum class Location { INTERNAL, EXTERNAL_APP, SDCARD_APP, EXTERNAL_SHARED }

    fun read(): Location {
        val raw = prefs.getString(KEY_LOCATION, null) ?: return Location.INTERNAL
        return runCatching { Location.valueOf(raw) }.getOrElse {
            AppLogger.e(TAG, "Unknown stored location '$raw', using internal", it)
            Location.INTERNAL
        }
    }

    fun write(location: Location) {
        prefs.edit().putString(KEY_LOCATION, location.name).apply()
    }

    /**
     * @param leaf directory name for the payload inside the chosen root
     * @return the directory, created if needed, or null when that location is
     *   unusable right now — for example no external volume, or the shared
     *   folder not writable because permission was refused.
     */
    fun dirFor(location: Location, leaf: String): File? {
        val root = when (location) {
            Location.INTERNAL -> context.filesDir
            Location.EXTERNAL_APP -> context.getExternalFilesDir(null) ?: run {
                AppLogger.e(TAG, "No external app directory available")
                return null
            }
            Location.SDCARD_APP -> sdcardRoot() ?: run {
                AppLogger.w(TAG, "No removable memory card mounted")
                return null
            }
            Location.EXTERNAL_SHARED -> OperitPaths.operitRootDir()
        }
        val dir = File(root, leaf)
        if (dir.isDirectory) return dir
        if (dir.mkdirs()) return dir
        AppLogger.e(TAG, "Cannot create ${dir.absolutePath}")
        return null
    }

    /** Locations that are usable now, in the order they should be offered. */
    fun available(): List<Location> =
        Location.entries.filter { dirFor(it, PROBE_DIR) != null }

    /** True when the payload would not occupy internal storage. */
    fun isExternal(location: Location): Boolean = location != Location.INTERNAL

    fun label(location: Location): String = when (location) {
        Location.INTERNAL -> context.getString(R.string.stt_storage_internal)
        Location.EXTERNAL_APP -> context.getString(R.string.stt_storage_external_app)
        Location.SDCARD_APP -> context.getString(R.string.stt_storage_sdcard)
        Location.EXTERNAL_SHARED -> context.getString(R.string.stt_storage_external_shared)
    }

    /**
     * The app folder on the removable card, or null when no card is mounted.
     *
     * The first entry of `getExternalFilesDirs` is always the primary volume,
     * which is the phone itself on virtually every device; the card, if any,
     * is one of the rest. Anything not reported removable is skipped, so an
     * OEM that lists two emulated volumes cannot mislead this into offering
     * internal storage under the memory-card name.
     */
    private fun sdcardRoot(): File? = runCatching {
        pickRemovable(context.getExternalFilesDirs(null)) { file ->
            Environment.isExternalStorageRemovable(file)
        }
    }.getOrElse {
        AppLogger.w(TAG, "Cannot list external volumes", it)
        null
    }



    fun describe(location: Location, leaf: String): String {
        val dir = dirFor(location, leaf)
        return if (dir == null) {
            context.getString(R.string.stt_storage_unavailable)
        } else {
            context.getString(R.string.stt_storage_location_value, label(location), dir.absolutePath)
        }
    }

    /**
     * Move [files] from the current location to [target] under [leaf].
     *
     * Files are copied rather than renamed so a cross-volume move works, and the
     * originals are removed only once every copy succeeded. Returns false when
     * the destination does not have room, in which case the setting is left
     * pointing at the old location.
     */
    fun migrate(
        target: Location,
        leaf: String,
        files: List<String>,
        requiredBytes: Long
    ): Boolean {
        val from = read()
        if (from == target) return true

        val source = dirFor(from, leaf)
        val destination = dirFor(target, leaf)
        if (source == null || destination == null) return false
        if (source.canonicalPath == destination.canonicalPath) return true

        val present = files.map { File(source, it) }.filter { it.isFile }
        if (present.isNotEmpty()) {
            val needed = present.sumOf { it.length() }
            if (needed > 0 && destination.usableSpaceBytes() < needed) {
                AppLogger.e(
                    TAG,
                    "Not moving ${present.size} file(s) needing ${needed / 1048576} MiB: " +
                        "${destination.absolutePath} has ${destination.usableSpaceBytes() / 1048576} MiB free"
                )
                return false
            }
            // File.copyTo throws rather than returning a Result, so each copy is
            // wrapped individually to find out which one failed.
            val copied = present.map { file ->
                runCatching { file.copyTo(File(destination, file.name), overwrite = true) }.isSuccess
            }
            if (copied.any { !it }) {
                AppLogger.e(TAG, "Move to ${destination.absolutePath} failed, keeping current location")
                return false
            }
            present.forEach { it.delete() }
            // Drop the emptied source dir so no hollow stt_model-style folders
            // linger. delete() on a non-empty dir returns false, no throw.
            if (source.listFiles()?.isEmpty() == true) {
                source.delete()
            }
        }

        write(target)
        return true
    }

    private fun File.usableSpaceBytes(): Long = runCatching { usableSpace }.getOrDefault(0L)

    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "AppDataLocation"
        private const val PREFS_NAME = "app_data_location"
        private const val KEY_LOCATION = "location"
        private const val PROBE_DIR = ".probe"

        /**
         * First removable volume after the primary one, ignoring nulls and
         * non-directories. Pure so the selection rule is unit-testable.
         */
        internal fun pickRemovable(
            dirs: Array<File?>?,
            isRemovable: (File) -> Boolean,
        ): File? {
            if (dirs == null || dirs.size < 2) return null
            return dirs.drop(1).firstOrNull { dir ->
                dir != null && dir.isDirectory &&
                    runCatching { isRemovable(dir) }.getOrDefault(false)
            }
        }
    }
}