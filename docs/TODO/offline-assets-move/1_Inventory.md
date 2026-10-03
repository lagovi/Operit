# Инвентарь: что лежит в assets (замер e9c7e0e1, stored = место во внутренней памяти)

| asset | stored | raw | куда |
|---|---:|---:|---|
| `ubuntu-noble-aarch64-pd-v4.18.0.tar.xz` | 61–64 MB | 64 MB | терминал, копируется в `files/` при первом запуске (дубль 61+64=125 МБ) |
| `subpack/android.apk` | ~24 MB сжато, 48 raw | 48 MB | сабпакеты |
| `packages/apktool.toolpkg` | ~26 MB | 27 MB | реверс APK, четыре jar почти не жмутся |
| `subpack/windows.zip` | ~11 MB | 11 MB | сабпакеты |
| `desktop.apk` | ~6 MB | 6.5 MB | helper |
| `templates/android+flutter/.../aapt2` ×2 | 9.4 MB | 9.4 MB | сборка шаблонов на устройстве (wontfix: общий файл ломает SCRIPT_DIR) |
| `accessibility.apk` + `shizuku.apk` | ~5 MB | 5.4 MB | helper |
| `templates`, `emoji`, `js` (KaTeX) | ~10 MB | ~10 MB | мелочь третьей очереди |
| `mlkit-google-ocr-models` | ~1 MB | 1.2 MB | OCR (латиница оставлена, фича живая) |

Итого подвижного: ~150 МБ stored + 92 МБ копий в `files/` (rootfs + toolpkg_cache).
Неподвижное: aapt2-дубли (решение SIZE-006), OCR-модели (фича живая).
