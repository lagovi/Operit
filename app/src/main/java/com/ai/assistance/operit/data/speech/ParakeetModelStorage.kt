package com.ai.assistance.operit.data.speech

import android.content.Context
import com.ai.assistance.operit.util.AppDataLocation
import com.ai.assistance.operit.util.OfflinePayload
import java.io.File

/**
 * Storage for the downloaded Parakeet model.
 *
 * Same shape as [SttModelStorage] but a different leaf (`stt_parakeet`),
 * so the two checkpoints never share files. The location setting itself
 * is global ([AppDataLocation.read]), so both engines live in the same
 * place; moving one moves the setting for both.
 */
class ParakeetModelStorage(private val context: Context) {

    private val locations = AppDataLocation(context)

    private val payload = OfflinePayload.PARAKEET

    fun currentLocation(): AppDataLocation.Location = locations.read()

    fun availableLocations(): List<AppDataLocation.Location> = locations.available()

    fun isExternal(): Boolean = locations.isExternal(locations.read())

    fun label(location: AppDataLocation.Location): String = locations.label(location)

    fun describe(location: AppDataLocation.Location): String =
        locations.describe(location, payload.leaf)

    /** Directory for the current location, created if needed. Null when unusable. */
    fun currentDir(): File? = locations.dirFor(locations.read(), payload.leaf)

    /**
     * Whether all model files are present at their exact sizes.
     *
     * Size only, deliberately not SHA-256: hashing 670 MB on every settings
     * visit would stall the UI, and the fetcher already verifies the digest of
     * every byte it writes.
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
     * changing the location never costs another 670 MB download.
     *
     * The files are moved from where they actually are, not from where the
     * global setting points: the setting is shared with other payloads, so
     * moving one can strand another's files behind.
     *
     * @return false when the move did not happen, leaving the setting untouched
     */
    fun migrateTo(target: AppDataLocation.Location): Boolean {
        val holder = locations.available().firstOrNull { location ->
            val dir = locations.dirFor(location, payload.leaf)
            dir != null && payload.assets.any { File(dir, it.name).isFile }
        }
        if (holder == null) {
            return locations.migrate(target, payload.leaf, emptyList(), payload.downloadBytes)
        }
        return locations.migrateFrom(
            source = holder,
            target = target,
            leaf = payload.leaf,
            files = payload.assets.map { it.name },
            requiredBytes = payload.downloadBytes
        )
    }
}
