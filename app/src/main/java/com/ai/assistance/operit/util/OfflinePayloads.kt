package com.ai.assistance.operit.util

import com.ai.assistance.operit.data.speech.GigaAMModelFiles
import java.io.File

/**
 * The bulky payloads that do not belong inside the APK.
 *
 * They share one storage location, one consent step and one download path, so
 * adding the next payload means adding one object rather than a subsystem.
 *
 * Measured on the debug build at HEAD 0200905f, compressed size inside the APK:
 *
 * | payload | in APK | movable | note |
 * |---|---:|---|---|
 * | terminal rootfs | 61.2 MiB | yes | already extracted to filesDir on first terminal use |
 * | subpack/android.apk | 24.2 MiB | yes | APK editor, read via `ApkEditor.fromAsset` |
 * | subpack/windows.zip | 10.9 MiB | yes | EXE editor, read via `AssetManager` |
 * | apktool.toolpkg | 25.7 MiB | yes | the jars inside are already zips, so it barely compresses |
 * | helper APKs | 9.1 MiB | yes | desktop, accessibility, shizuku |
 * | templates, emoji, js | 8.0 MiB | yes | project gallery and KaTeX |
 *
 * None of these are listed as objects yet, because each needs its own
 * investigation before it can be removed from the APK: the terminal rootfs has
 * to lose its `context.assets.open` call, the subpackages need a download source
 * the fork controls, and the toolpkg needs a decision on whether the feature is
 * wanted at all. Adding an entry before that work is done would make the
 * registry lie about what the build contains.
 *
 * Not listed, because nothing can be done about them: `lib/arm64-v8a` and the dex
 * files. The linker resolves native libraries from the package manager's own
 * `libDir` under `/data/app`; that is not external storage and the app cannot
 * redirect it.
 */
class OfflinePayload(
    val id: String,
    val title: String,
    val leaf: String,
    val assets: List<RemoteAsset>,
    /** Size after unpacking, which is what the payload really costs on the phone. */
    val unpackedBytes: Long
) {
    val downloadBytes: Long get() = assets.sumOf { it.sizeBytes }

    fun isCompleteIn(dir: File): Boolean = assets.all { File(dir, it.name).isFile }

    companion object {
        /**
         * The 214 MiB GigaAM CTC checkpoint. Listed here as well as in
         * `GigaAMModelFiles` so that one registry answers "what will this app
         * ask me to download, and where does it put it".
         */
        val GIGAAM = OfflinePayload(
            id = "stt",
            title = "GigaAM speech model",
            leaf = "stt_model",
            assets = GigaAMModelFiles.ASSETS,
            unpackedBytes = GigaAMModelFiles.TOTAL_BYTES
        )

        val all: List<OfflinePayload> = listOf(GIGAAM)
    }
}