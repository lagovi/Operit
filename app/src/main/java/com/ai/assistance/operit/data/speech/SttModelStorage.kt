package com.ai.assistance.operit.data.speech

import android.content.Context
import com.ai.assistance.operit.util.AppDataLocation
import com.ai.assistance.operit.util.OfflinePayload
import java.io.File

/**
 * Storage for the downloaded GigaAM model.
 *
 * Thin wrapper over [AppDataLocation], which owns the location choice for every
 * large payload; this class only adds the speech-specific parts, namely the
 * payload description and the move that happens when the user changes location.
 */
class SttModelStorage(private val context: Context) {

    private val locations = AppDataLocation(context)

    private val payload = OfflinePayload.GIGAAM

    fun currentLocation(): AppDataLocation.Location = locations.read()

    fun availableLocations(): List<AppDataLocation.Location> = locations.available()

    fun isExternal(): Boolean = locations.isExternal(locations.read())

    fun label(location: AppDataLocation.Location): String = locations.label(location)

    fun describe(location: AppDataLocation.Location): String =
        locations.describe(location, payload.leaf)

    /** Directory for the current location, created if needed. Null when unusable. */
    fun currentDir(): File? = locations.dirFor(locations.read(), payload.leaf)

    /**
     * Whether both model files are present at their exact sizes.
     *
     * Size only, deliberately not SHA-256: hashing 225 MB on every settings
     * visit would stall the UI, and the fetcher already verifies the digest of
     * every byte it writes. A size mismatch means absent or corrupt, and the
     * next download repairs it.
     *
     * @return the directory when downloaded, null otherwise
     */
    fun isDownloaded(): File? {
        val dir = locations.dirFor(locations.read(), payload.leaf) ?: return null
        val intact = payload.assets.all { asset ->
            val file = File(dir, asset.name)
            file.isFile && file.length() == asset.sizeBytes
        }
        return if (intact) dir else null
    }

    fun dirFor(location: AppDataLocation.Location): File? =
        locations.dirFor(location, payload.leaf)

    /**
     * Move the model to [target] if the user has already downloaded it, so
     * changing the location never costs another 214 MB download.
     *
     * @return false when the move did not happen, leaving the setting untouched
     */
    fun migrateTo(target: AppDataLocation.Location): Boolean {
        val moved = locations.migrate(
            target = target,
            leaf = payload.leaf,
            files = payload.assets.map { it.name },
            requiredBytes = payload.downloadBytes
        )
        if (moved) locations.write(target)
        return moved
    }
}