package com.ai.assistance.operit.data.speech

import android.content.Context
import com.ai.assistance.operit.util.AppDataLocation
import com.ai.assistance.operit.util.OfflinePayload
import java.io.File
import java.util.zip.ZipFile

/**
 * Storage for the downloaded Nemotron model and its on-demand runtime.
 *
 * Same shape as [ParakeetModelStorage] but a different leaf
 * (`stt_nemotron`), so the checkpoints never share files. The location
 * setting itself is global ([AppDataLocation.read]), so every payload lives
 * in the same place.
 *
 * One extra step versus the other engines: the sherpa-onnx JNI library is
 * not in the APK. The download brings the official static-link AAR, this
 * class extracts the arm64 `.so` next to the weights and drops the AAR, and
 * the provider `System.load`s the extracted file. Keeping the AAR would cost
 * 38 MB for three ABIs the app never ships.
 */
class NemotronModelStorage(private val context: Context) {

    private val locations = AppDataLocation(context)

    private val payload = OfflinePayload.NEMOTRON

    fun currentLocation(): AppDataLocation.Location = locations.read()

    fun availableLocations(): List<AppDataLocation.Location> = locations.available()

    fun isExternal(): Boolean = locations.isExternal(locations.read())

    fun label(location: AppDataLocation.Location): String = locations.label(location)

    fun describe(location: AppDataLocation.Location): String =
        locations.describe(location, payload.leaf)

    /** Directory for the current location, created if needed. Null when unusable. */
    fun currentDir(): File? = locations.dirFor(locations.read(), payload.leaf)

    fun dirFor(location: AppDataLocation.Location): File? =
        locations.dirFor(location, payload.leaf)

    /**
     * The extracted runtime library, or null when it is not in [dir] at its
     * exact size. This is what the provider hands to `System.load`.
     */
    fun runtimeLib(dir: File): File? {
        val lib = File(dir, NemotronModelFiles.RUNTIME_LIB_FILE_NAME)
        return if (lib.isFile && lib.length() == NemotronModelFiles.RUNTIME_LIB_BYTES) lib else null
    }

    /**
     * Whether the weights and the extracted runtime are present.
     *
     * Size only, deliberately not SHA-256: hashing 682 MB on every settings
     * visit would stall the UI, and the fetcher already verifies the digest of
     * every byte it writes.
     */
    fun isDownloaded(): File? {
        val dir = locations.dirFor(locations.read(), payload.leaf) ?: return null

        val weightsIntact = payload.assets.all { asset ->
            val file = File(dir, asset.name)
            file.isFile && file.length() == asset.sizeBytes
        }
        if (!weightsIntact) return null

        return if (runtimeLib(dir) != null) dir else null
    }

    /**
     * Extract [NemotronModelFiles.RUNTIME_AAR_FILE_NAME]'s arm64 library next
     * to the weights and delete the archive. Idempotent: an already-extracted
     * library at the right size short-circuits.
     *
     * @throws IllegalStateException when the AAR is missing or has no arm64 member
     */
    fun ensureRuntime(dir: File): File {
        runtimeLib(dir)?.let { return it }

        val aar = File(dir, NemotronModelFiles.RUNTIME_AAR_FILE_NAME)
        check(aar.isFile) { "missing ${aar.absolutePath}" }
        val lib = File(dir, NemotronModelFiles.RUNTIME_LIB_FILE_NAME)

        ZipFile(aar).use { zip ->
            val entry = zip.getEntry(NemotronModelFiles.RUNTIME_AAR_ENTRY)
                ?: error("${aar.name} has no ${NemotronModelFiles.RUNTIME_AAR_ENTRY}")
            zip.getInputStream(entry).use { input ->
                lib.outputStream().use { output -> input.copyTo(output) }
            }
        }
        check(lib.length() == NemotronModelFiles.RUNTIME_LIB_BYTES) {
            "extracted ${lib.name} is ${lib.length()} B, expected ${NemotronModelFiles.RUNTIME_LIB_BYTES}"
        }
        aar.delete()
        return lib
    }

    /**
     * Move the model to [target] if the user has already downloaded it, so
     * changing the location never costs another 682 MB download.
     *
     * The files are moved from where they actually are, not from where the
     * global setting points. The transient AAR is included in the move list
     * only if a failed earlier run left it behind; [AppDataLocation] skips
     * names that are not present.
     *
     * @return false when the move did not happen, leaving the setting untouched
     */
    fun migrateTo(target: AppDataLocation.Location): Boolean {
        val movedNames = payload.assets.map { it.name } +
            listOf(
                NemotronModelFiles.RUNTIME_LIB_FILE_NAME,
                NemotronModelFiles.RUNTIME_AAR_FILE_NAME,
            )
        val holder = locations.available().firstOrNull { location ->
            val dir = locations.dirFor(location, payload.leaf)
            dir != null && movedNames.any { File(dir, it).isFile }
        }
        // The move carries the extracted library too, so the free-space check
        // is the unpacked size, not the transfer size.
        if (holder == null) {
            return locations.migrate(target, payload.leaf, emptyList(), payload.unpackedBytes)
        }
        return locations.migrateFrom(
            source = holder,
            target = target,
            leaf = payload.leaf,
            files = movedNames,
            requiredBytes = payload.unpackedBytes
        )
    }
}
