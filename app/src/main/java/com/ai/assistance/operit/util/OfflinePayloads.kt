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
 * | templates, emoji, js | 8.0 MiB | yes | project gallery and KaTeX |
 *
 * Gone from the table because Q2 removed them from the APK (see the objects
 * below): `subpack/android.apk`, `subpack/windows.zip` (release `v1.12.1+4`,
 * 2026-10-08), `apktool.toolpkg` (dropped from the whitelist, HANDOFF 12a),
 * `desktop.apk` (deleted, HANDOFF 12b), `shizuku.apk` + `accessibility.apk`
 * (on-demand into cacheDir, release `v1.12.1+4`).
 * The terminal rootfs is not listed as an object yet: it has to lose its
 * `context.assets.open` call first, and the proot tree may not start from an
 * external leaf at all (E3). Adding an entry before that work is done would
 * make the registry lie about what the build contains.
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
    val unpackedBytes: Long,
    /** Version shown in "bundled vs installed" comparisons, if the payload has one. */
    val version: String? = null
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

        /**
         * APK export template, provisioned from the fork's own release into the
         * `subpack` leaf on first export. The staged file keeps the Q1 name
         * (`apk_editor_android.apk`), so copies staged by earlier builds are
         * recognised by size+sha and never downloaded again.
         *
         * Bytes are the `assets/subpack/android.apk` entry of the last APK
         * that still bundled it (CI `37726800230`).
         */
        val SUBPACK_ANDROID = OfflinePayload(
            id = "subpack-android",
            title = "APK export template",
            leaf = "subpack",
            assets = listOf(
                RemoteAsset(
                    name = "apk_editor_android.apk",
                    url = "$RELEASE_BASE/v1.12.1+4/subpack-android.apk",
                    sizeBytes = 48139093L,
                    sha256 = "c56b23a841a736e028aeec8bee6b48a086b668143ce103095042e622750b86b4"
                )
            ),
            unpackedBytes = 48139093L
        )

        /**
         * Windows export template, same mechanics as [SUBPACK_ANDROID].
         * Bytes are the `assets/subpack/windows.zip` entry of CI `37726800230`.
         */
        val SUBPACK_WINDOWS = OfflinePayload(
            id = "subpack-windows",
            title = "Windows export template",
            leaf = "subpack",
            assets = listOf(
                RemoteAsset(
                    name = "exe_editor_windows.zip",
                    url = "$RELEASE_BASE/v1.12.1+4/subpack-windows.zip",
                    sizeBytes = 11412717L,
                    sha256 = "9c2d3e3b5e862334e24f4008572f239c380a73f23c5940054bf3a5142ca46888"
                )
            ),
            unpackedBytes = 11412717L
        )

        val all: List<OfflinePayload> = listOf(
            GIGAAM, SUBPACK_ANDROID, SUBPACK_WINDOWS, HELPER_SHIZUKU, HELPER_A11Y
        )

        /**
          * Shizuku helper APK, provisioned from the fork's own release into
          * `cacheDir` on first install press. The staged file keeps the
          * historical name (`shizuku.apk`), so copies extracted by earlier
          * builds are recognised by size+sha and never downloaded again.
          *
          * Bytes are the `assets/shizuku.apk` entry of the last APK that
          * still bundled it; version is the former `shizuku_version.txt`
          * content, now hardcoded (bump together with the release file).
          */
        val HELPER_SHIZUKU = OfflinePayload(
            id = "helper-shizuku",
            title = "Shizuku helper app",
            leaf = "cache",
            assets = listOf(
                RemoteAsset(
                    name = "shizuku.apk",
                    url = "$RELEASE_BASE/v1.12.1+4/helper-shizuku.apk",
                    sizeBytes = 2571773L,
                    sha256 = "6e273ab0e991c4e79bc8b1bbb9b9dd739ccac1a8712a541a214078886b7b790f"
                )
            ),
            unpackedBytes = 2571773L,
            version = "13.6.0"
        )

        /**
          * Accessibility provider APK, same mechanics as [HELPER_SHIZUKU].
          * Bytes are the `assets/accessibility.apk` entry of the last APK
          * that still bundled it; version is the former
          * `accessibility_version.txt` content.
          */
        val HELPER_A11Y = OfflinePayload(
            id = "helper-a11y",
            title = "Accessibility provider app",
            leaf = "cache",
            assets = listOf(
                RemoteAsset(
                    name = "accessibility.apk",
                    url = "$RELEASE_BASE/v1.12.1+4/helper-accessibility.apk",
                    sizeBytes = 2763980L,
                    sha256 = "b6bac271c33a41b6cc39840d122286488cebc57be2e74b39aed5bb0d0b1c5212"
                )
            ),
            unpackedBytes = 2763980L,
            version = "1.5"
        )

        /** One download base for every payload: no per-file host to mistype. */
        private const val RELEASE_BASE = "https://github.com/lagovi/Operit/releases/download"
    }
}