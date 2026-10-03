# Очередь работ

## Очередь 1: SD-first-run (без хостинга, только код + механика мест)

1. **rootfs**: при первом запуске копировать/распаковывать не в `files/`, а в
   `AppDataLocation`-leaf (`terminal_rootfs`), по умолчанию INTERNAL, с
   возможностью переезда на SD тем же пикером, что у STT-модели. Чтение —
   через `File`, не `assets.open`. Выигрыш: до 125 МБ.
2. **subpack (android.apk, windows.zip)**: то же самое, leaf `subpack`.
   Источник сейчас — upstream Google Drive через
   `ci/script/prepare_android_dependencies.py`; архивы при сборке остаются,
   меняется только место распаковки. Выигрыш: ~59 МБ.
3. **apktool.toolpkg**: кандидат на удаление, а не на переезд: собирается
   скриптом `tools/example_packages/sync_example_packages.py` из
   `examples/apktool/`, jar уже zip и почти не жмутся. Проверить, что
   реверс работает из внешнего leaf, иначе удалить из APK. Выигрыш: ~26 МБ.
4. **desktop.apk, helper APKs, templates/emoji/js**: третья очередь, ~20 МБ.

## Очередь 2: on-demand download (нужен хостинг)

5. Создать GitHub Releases hosting для payload (сейчас не создан).
6. Описать каждый payload как `OfflinePayload` (образец: `GIGAAM`) с
   URL/size/sha, качать через `RemoteAssetFetcher` в `AppDataLocation`-leaf.
7. Удалить файлы из `app/src/main/assets`, убрав их из APK полностью.

## Не делать

- Общий aapt2 на два шаблона (ломает `setup_android_env.sh`, SIZE-006).
- Трогать OCR-модели и `lib/` (отдельные решения).
