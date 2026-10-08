# Size research: measured numbers and hard limits

## 2026-10-05 ledger: what changed and what is DEAD (read this first)

Two levers from the 10-03 table below are now CLOSED by device evidence.
Do not re-propose them without new facts.

1. **`useLegacyPackaging=false` — REVERTED (`cddb9906`).**
   Measured 10-03 (`bf48f418`, CI `37086798853`): APK 293 -> 386 MB (+93),
   installed 435 -> 369 MB (−66). Looked like a pure win for installed size
   (the user's priority) and was kept. On 10-05 it killed the terminal:
   with no extraction, `lib/arm64/` on the phone is EMPTY, so
   `TerminalManager.linkNativeLibs()` (submodule `:terminal`,
   `applicationInfo.nativeLibraryDir` only) created zero `bash/proot/busybox`
   links and every session died instantly
   (`execve(.../files/usr/bin/bash) failed: No such file or directory`,
   exit 1, black screen, 30 s `Session initialization timeout`).
   Repair attempt B1 (submodule `26d4964`, `resolveNativeLibSource`:
   extract the 5 terminal .so from our own APK into `files/usr/bin`) got the
   links created and then hit the next wall: `execve bash: Permission denied`.
   Same files execute fine under `run-as` (domain `runas_app`, EXIT=0) but not
   from the app domain (`untrusted_app`); both files are `app_data_file`
   (`ls -Z`), `/data` has no `noexec`, no avc in logcat (dontaudit). Verdict:
   Samsung A20s forbids `untrusted_app` from executing `app_data_file`, so
   terminal binaries MUST come from PM-extracted `lib/` with the system label.
   Revert A-build (CI `37299257049`): APK 373 -> 287 MB, terminal accepted on
   device 10-05 15:12 (live `$`, 358 MB tree, `.operit_installed_ok`).
   Installed footprint of the A-build was NOT remeasured — re-run
   `adb shell du -sh` on the package dir before quoting a number
   (pre-revert eras: 435 MB with `true`, 369 MB with `false`).
   Consequence for future size work: native libs are untouchable as long as
   the terminal execs prebuilts. Any new `false`-style attempt must first prove
   exec works from its target location on the Samsung test phone.
2. **Rootfs to external storage — FAILED (Q1 verdict 10-05, protocol in
   `docs/TODO/offline-assets-move/2_WorkQueue.md` item 1).** 358 MB unpacked
   tree copied with `busybox cp -a` (app uid, via `run-as`) to EXTERNAL_APP
   (`/storage/emulated/0/...`, FUSE): files copy, symlinks do not — hundreds
   of `can't create symlink ... Operation not permitted` (`etc/ssl/certs`,
   `etc/alternatives`, merged-/usr `bin/lib/sbin`, `dev/std*`). SDCARD_APP
   (`/storage/5982-1724/...`): `ln -s` fails immediately (`Permission
   denied`). A proot Ubuntu needs its symlinks; dereferencing (`-L`) would
   double the tree (~700 MB, no fit on the 980 MB card) and still break
   dangling links (`dev/std*`, `mtab`). So: rootfs stays internal, the
   61 MB tarball stays in the APK until Q2 hosting exists. Still movable
   (pure data, no symlinks, `AppDataLocation` infra already in the app):
   subpack (~59 MB), apktool (~26 MB) — queue items 2–3.
3. **Update 2026-10-08 — что изменилось:**
   - subpack Q1 п.2: МЕХАНИКА ГОТОВА (`a4fddb40`, CI `37709173132`,
     APK 247299182 Б, дельта +10 КБ к задаче 10). `SubpackStorage`
     (leaf `subpack`), стейджинг из cacheDir переведён на leaf
     (фалбэк сохранён), пикер в Settings → Data & Permissions.
     Принято на телефоне: переезд internal → external → SD
     (`/storage/5982-1724/.../files/subpack/` создан), ошибок нет.
     Файлы из APK НЕ УБРАНЫ (это Q2); выигрыш installed — только через
     переезд на SD, выигрыш APK — ноль. E2E «файлы в leaf после экспорта»
     отложено в совместное ручное тестирование.
   - R8: DONE (не blocked). Keep-fix `b4ff4a30`, minify debug ON навсегда
     (решение пользователя), mapping-артефакты каждого зелёного CI.
   - ДРЕЙФ РАЗМЕРОВ на Drive (замерено 08.10 в APK CI `37709173132`):
     `assets/subpack/android.apk` — 48 МБ против 24.2 МБ в таблице ниже.
     Архив апстрима ВЫРОС вдвое; цифры таблицы 03.10 для subpack
     протухли — перед Q2 перемерить все payload байт-в-байт
     (`python zipfile`, не `unzip -l` — тот молча врёт нулём).
   - apktool Q1 п.3 ЗАКРЫТ УДАЛЕНИЕМ 08.10 (HANDOFF 12a): `apktool`
     вычеркнут из `tools/example_packages/packages_whitelist.txt`,
     CI sync больше не пакует `examples/apktool/` в `assets/packages/`.
     Основания: `enabled_by_default: false`, чтение только из assets
     с распаковкой в кэш (~27 МБ × 2 installed), импорт `.toolpkg`
     извне уже работает, stale-кэш чистит `reconcileToolPkgCaches`,
     ссылок/тестов ноль. DoD ЗАКРЫТ: CI `37722980127` success,
     APK 247299182 → 220307769 Б (−26991413, сошлось с 26985716 Б
     тулкита), `assets/packages/` — 1429723 Б чистого .js, папка
     `2026-10-08_03-29-43Z`. `desktop.apk`+`desktop_version.txt`
     УДАЛЕНЫ 08.10 тем же заходом (HANDOFF 12b: ноль ссылок,
     `git rm`; CI `37726800230`: 220307769 → 214210621 Б, −6097148;
     папка `2026-10-08_04-17-45Z`). Живые остатки для Q2 с картой
     читателей: helper APK shizuku/accessibility 5.4 МБ
     (`ShizukuInstaller:37`, `UIHierarchyManager:89-91`), templates
     9.8 МБ (`WorkspaceUtils.copyTemplateFiles:682`), emoji 3.6 МБ
     (`CustomEmojiRepository:298`); js/ 1.4 МБ — горячий рантайм,
     НЕ ТРОГАТЬ. Desktop Q1 п.4 ЗАКРЫТ,
   - Q2-SUBPACK ГОТОВ 08.10 (HANDOFF 13): `SUBPACK_ANDROID/WINDOWS` +
     `ensureTemplate`, оба flow `ExportDialogs`, CI без `subpack.zip`,
     релиз `v1.12.1+4` (2 файла, sha сошлись), CI `37772193743`:
     APK 214210621 → 177474049 Б (−36736572), папка
     `2026-10-08_11-45-43Z`. DoD-приёмка закачки ЖДЁТ ТЕЛЕФОН
     (порты 08.10 днём закрыты). Итог ночи: 247 → 177 МБ (−70 МБ).
     (HANDOFF 12b), Q2 on-demand открыт (хостинг РЕШЁН: GitHub Releases
     в том же репо, HANDOFF задача 13), GigaAM на SD (done, M6).

## 2026-10-03: internal-storage prospects, ranked (APK anatomy at e9c7e0e1)

Status update 2026-10-08: rows 2, 3, 4 DONE; packaging lever and rootfs
DEAD (details in the ledger above and in HANDOFF E1–E3); row 1 PART
(subpack mechanics done, apktool/desktop + Q2 open — HANDOFF 12/13).
Do not re-investigate DONE rows; do not re-propose DEAD ones.

| # | lever | internal saving | cost | status 2026-10-06 |
|---|-------|----------------:|------|-------------------|
| 1 | Heavy assets out of the APK (`assets` stored 155 MB) + their `files/` copies (92 MB: rootfs 61, toolpkg_cache 31) | up to ~150–240 MB | hosting or SD-first-run flow + File-based loading; biggest work | PART: rootfs DEAD on device (E3, stays internal); subpack MECHANICS DONE 08.10 (`a4fddb40`, leaf+picker accepted, files still in APK — payoff only via SD move until Q2); apktool REMOVED+MEASURED 08.10 (CI `37722980127`: 247299182 → 220307769 Б); desktop.apk REMOVED+MEASURED 08.10 (CI `37726800230`: → 214210621 Б); живые остатки (helper APK/templates/emoji) → Q2; Q2 OPEN (HANDOFF 13, hosting decided) |
| 2 | GigaAM model (225 MB) to SD via the existing picker | 225 MB | zero code; operational, needs the SD present | DONE (M6 2026-10-05: SD round-trip byte-identical, engine inits from card) |
| 3 | Drop `tensorflow.lite` + `mediapipe.tasks.text` (comment says "if needed", zero code references) | ~11.5 MB | two dependency lines; CI-verifiable | DONE (CI `37127299796`: APK 386 -> 373 MB, both .so gone from APK) |
| 4 | R8/minify for debug builds too (dex stored ~75 MB over 43 files, no minification today) | est. 25–35 MB | slower CI builds; measure like the packaging lever | DONE 08.10 (keep-fix `b4ff4a30`, minify debug ON forever, −40.1 MB measured: 287428400 -> 247288710; mapping artifacts per green CI) |
| 5 | oat/dexopt | follows 4 | — | follows 4 |
| — | res/arsc, aapt2 dupes | negligible / wontfix | — | — |

Before quoting ANY installed-size number for the current build, re-measure
(the A-build footprint was never remeasured after the revert):

```
adb shell du -sh /data/app/~~*/com.ai.assistance.operit.debug*   # exact path via: pm path <pkg>
```

Reference eras (do not quote as current): 435 MB with `true`, 369 MB with
`false`, APK 287 MB after revert (CI `37299257049`).

Note on the anatomy below: it was measured under `useLegacyPackaging=false`
(stored == raw in `lib`); after the revert to `true`, APK-side numbers for
`lib` changed, device-side extraction returned. Re-measure before trusting.

Breakdown of `assets` stored: rootfs 64 MB, `subpack/android.apk` 48 MB,
`apktool.toolpkg` 27 MB, `subpack/windows.zip` 11 MB, `desktop.apk` 6.5 MB,
aapt2 ×2 9.4 MB, helper APKs 5.4 MB, templates/emoji/js ~14 MB.

Verified still-used (do NOT cut without a product decision): MNN 27.5 MB
(`MNNProvider` + download screen), llama.cpp ~25 MB (`llm/llama` module +
`getLlamaLocalModels`), ffmpeg ~27 MB (`FFmpegUtil`, `MediaPoolManager`),
ML Kit OCR 10 MB (`OCRUtils`), FBX/MMD/filament ~13 MB (avatar factories),
quickjs/busybox/bash/ripgrep/objectbox (terminal/tooling).

Fresh-install `/data/data` is ~100 MB (rootfs copy 61 + toolpkg_cache 31),
before any model download. The earlier 4.4 GB `du` reading was an artifact
(run-as + symlink traversal), not real usage; per-dir numbers above add up.

Written so nobody repeats the measurements. Every number here was produced by a
command recorded in this file, not estimated. Re-run a command before trusting a
number that has since gone stale.

## The headline correction

The app occupies **435 MB on the phone**, not the 296 MB its APK advertises.

```
$ adb shell du -sh /data/app/~~*/com.ai.assistance.operit.debug*
435M    base.apk 297 MB + lib/arm64 152 MB
```

`useLegacyPackaging = true` in `app/build.gradle.kts` makes the package manager
extract the native libraries, and they are *also* inside the APK. The native code
is therefore paid for twice:

| | size |
|---|---:|
| `lib/arm64-v8a` compressed inside the APK | 58.5 MB (45 files) |
| `lib/arm64-v8a` extracted next to the APK | 151.5 MB |
| paid twice, total | ~210 MB |

Everything recorded in `SIZE-004` … `SIZE-007`, mine included, optimised the APK
download size. That was a real improvement and the right thing to do for the
first pass, but it left this untouched. The user has since said installed size is
what matters.

## Where the bytes are, in the debug build at 0200905f

Compressed size inside the APK, top-level grouping:

| group | compressed | raw |
|---|---:|---:|
| `assets` | 148.0 MB | 185.7 MB |
| `lib` | 58.5 MB | 151.5 MB |
| `classes*.dex` (9 files) | ~39 MB | ~120 MB |

Largest assets, compressed:

| asset | size | movable |
|---|---:|---|
| `ubuntu-noble-aarch64-pd-v4.18.0.tar.xz` | 61.2 MB | yes |
| `subpack/android.apk` | 24.2 MB | yes |
| `subpack/windows.zip` | 10.9 MB | yes |
| `packages/apktool.toolpkg` | 25.7 MB | yes |
| `packages/*` other toolpkgs | 0.7 MB | yes |
| `desktop.apk` | 5.8 MB | yes |
| `templates` | 4.3 MB | yes |
| `emoji` | 3.5 MB | yes |
| `shizuku.apk` + `accessibility.apk` | 3.3 MB | yes |
| `js` (KaTeX) | 2.5 MB | yes |
| `mlkit-google-ocr-models` | 1.2 MB | partly |
| `operit.png` | 1.3 MB | yes |

Movable total: **~139 MB**.

## What cannot move, and why

Native libraries and dex. The dynamic linker resolves `.so` files from the
package manager's own `libDir` under `/data/app`; that directory is not external
storage and the app cannot redirect it. Nothing in the code blocks a packaging
change, though: every load uses `System.loadLibrary`, never `System.load` with a
path, so there is no code assuming an extracted `.so` on disk.

```
$ grep -rE 'System\.load\(|System\.loadLibrary\(' --include=*.kt app/src/main/java
```

Only `System.loadLibrary` appears.

## The packaging lever, not yet measured

Setting `useLegacyPackaging = false` should stop the 152 MB extraction. The
counterweight: AGP then stores the libraries **uncompressed and page-aligned** so
they can be mapped straight out of the APK, which grows the APK by roughly 93 MB
while removing 152 MB of installed footprint. Net expected saving near 59 MB of
installed size, paid for with a larger download.

## Measured 2026-10-03: the lever works (commit bf48f418, CI 37086798853)

| | APK download | installed on phone |
|---|---:|---:|
| `useLegacyPackaging = true` | 293 MB | 435 MB (base.apk ~297 + lib/arm64 152) |
| `useLegacyPackaging = false` | 386 MB | 369 MB (base.apk 369 + lib 16K, no extraction) |
| delta | +93 MB | −66 MB |

Measured with `adb shell du -sh` on the package dir after a clean install.
All prebuilt `jniLibs` aligned without a build failure.

> SUPERSEDED 2026-10-05: the flag did NOT stay `false`. It was reverted to
> `true` (`cddb9906`) because the terminal cannot start without PM-extracted
> native libs (Samsung blocks app-domain exec of `app_data_file`; see the
> 2026-10-05 ledger at the top of this file). Numbers above are the 10-03
> `false` era — do not quote them as current.

## Provenance of the build inputs

Nothing large is tracked in git, so this had to be established:

| input | where it comes from |
|---|---|
| `lib/arm64-v8a` (151.5 MB raw) | `jniLibs.zip` on upstream's Google Drive, unpacked by `ci/script/prepare_android_dependencies.py` |
| `assets/subpack` (35 MB) | `subpack.zip` on the same Google Drive account |
| `assets/packages/apktool.toolpkg` (25.7 MB) | packed from `examples/apktool/` by `tools/example_packages/sync_example_packages.py`; its four jars are `jadx-runtime-android.jar` 14 MB, `apktool-runtime-android.jar` 5.0 MB, `android-framework.jar` 4.3 MB, `apk-reverse-helper-runtime-android.jar` 3.1 MB |
| terminal rootfs (61.2 MB) | `terminal/src/main/assets/`, in our own submodule |

The fork cannot modify the Google Drive archives, which is why the subpackages
would need a download source this fork controls before they could be moved.

## Libraries that should disappear

| library | size | why |
|---|---:|---|
| `libsherpa-ncnn-jni.so` | 5.05 MB | STT-002 replaces the ncnn recogniser |
| `libsherpa-mnn-jni.so` | 3.76 MB | same, the MNN variant |
| `libMNN.so` | 22.0 MB | MNN runtime, only used by the MNN recogniser |
| `libMNNWrapper.so` | 5.5 MB | same |
| `libFbxWrapper.so` | 1.7 MB | FBX import, `avator/fbx` |

`libonnxruntime.so` 16 MB must stay: GigaAM runs through it.

## Speech model facts

Model: `i2z1/gigaam-multilingual-ctc-onnx-int8`, revision
`ba9011bfb2e52cacad5a12e5d9346392d7825c76`. `model.int8.onnx` is 224 762 512
bytes, SHA-256 `5f584553e20db1af7e0fff98728b6904b670eb6488b88da8e87cda7870a6235f`.

`fussraider/GigaAM-Multilingual-sherpa-onnx-ctc` publishes byte-identical
weights: all 959 ONNX initializers match exactly, and the 10 557 differing bytes
are protobuf serialisation plus the `language` metadata string. i2z1 was chosen
because it ships `export-multilingual-ctc.py`, which documents the method as
`quantize_dynamic` with `QUInt8` weights.

Do **not** use `DmitrySharonov/GigaAM-Multilingual-ONNX`: FP32 only at 844 MB,
despite a "Quantized (6)" badge, and its `large_ctc/multilingual_large_ctc.onnx`
is truncated to 3.4 MB of the expected 2.34 GB.

## Test material

`tools/gigaam/run_port_tests.sh` runs the 12 frontend and recogniser tests with
only a JDK and kotlinc, no Android SDK. Four of them need the real checkpoint and
are skipped otherwise:

```
GIGAAM_MODEL_DIR=/path/to/dir   # holds model.int8.onnx and tokens.txt
GIGAAM_TEST_WAV=/path/to/example.wav
```

The audio is the official GigaAM test clip, from the URL in
`gigaam.utils.download_short_audio`:

```
https://cdn.chatwm.opensmodel.sberdevices.ru/GigaAM/example.wav
```

The transcript on that clip is Pushkin and reads
`ничьих не требуя похвал счастлив уж я надеждой сладкой ... у лукоморья дуб зеленый`.