# Size research: measured numbers and hard limits

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

**Do not record the outcome from this estimate.** It has not been built. The
prebuilt `jniLibs` come from an upstream archive and not all of them may be
page-alignable, in which case the build fails outright. Measure before claiming.

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