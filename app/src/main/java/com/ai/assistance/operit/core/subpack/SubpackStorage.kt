package com.ai.assistance.operit.core.subpack

import android.content.Context
import com.ai.assistance.operit.util.AppDataLocation
import com.ai.assistance.operit.util.AssetCopyUtils
import java.io.File

/**
 * Staging for the APK/EXE export templates (`subpack/android.apk`,
 * `subpack/windows.zip`, ~35 MB unpacked).
 *
 * Same mechanics as the speech model ([SttModelStorage] pattern): the files
 * are staged from the APK into an [AppDataLocation] leaf instead of the
 * cache dir, so the user can move them to the memory card with the same
 * picker. Q1 only moves the runtime copies; the originals stay in the APK
 * until the Q2 on-demand download replaces them.
 */
class SubpackStorage(private val context: Context) {

    private val locations = AppDataLocation(context)

    fun currentLocation(): AppDataLocation.Location = locations.read()

    fun availableLocations(): List<AppDataLocation.Location> = locations.available()

    fun isExternal(): Boolean = locations.isExternal(locations.read())

    fun label(location: AppDataLocation.Location): String = locations.label(location)

    fun describe(location: AppDataLocation.Location): String =
        locations.describe(location, LEAF)

    /** Staging directory for the current location, created if needed. */
    fun currentDir(): File? = locations.dirFor(locations.read(), LEAF)

    /**
     * Copies [assetPath] (e.g. `subpack/android.apk`) into the leaf,
     * overwriting a stale copy. Returns the staged file, or null when the
     * leaf directory itself is unavailable.
     */
    fun stageAsset(assetPath: String, stagedName: String): File? {
        val dir = currentDir() ?: return null
        return runCatching {
            AssetCopyUtils.copyAssetToFile(
                context,
                assetPath,
                File(dir, stagedName),
                overwrite = true
            )
        }.getOrNull()
    }

    /**
     * Moves staged templates to [target]. Only files actually present move;
     * an empty leaf just flips the setting.
     */
    fun migrateTo(target: AppDataLocation.Location): Boolean {
        val moved = locations.migrate(
            target = target,
            leaf = LEAF,
            files = STAGED_FILES,
            requiredBytes = STAGED_BYTES
        )
        if (moved) locations.write(target)
        return moved
    }

    companion object {
        const val LEAF = "subpack"

        /** Staged file names, including the per-editor prefix. */
        val STAGED_FILES = listOf("apk_editor_android.apk", "exe_editor_windows.zip")

        /** Rough upper bound of both templates, used for the free-space check. */
        const val STAGED_BYTES = 40L * 1024 * 1024
    }
}
