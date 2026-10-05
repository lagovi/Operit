# Очередь работ

## Очередь 1: SD-first-run (без хостинга, только код + механика мест)

 1. **rootfs**: СНАЧАЛА эксперимент на устройстве, потом код (разведка
    2026-10-04, см. ниже). **ЗАБЛОКИРОВАНО 2026-10-05: терминал мёртв во всех
    сборках форка — см. диагноз ниже.** При первом запуске
    копировать/распаковывать не в `files/`, а в `AppDataLocation`-leaf
    (`terminal_rootfs`), по умолчанию INTERNAL, с возможностью переезда на
    SD тем же пикером, что у STT-модели. Чтение — через `File`, не
    `assets.open`. Выигрыш: до 125 МБ.
    - ДИАГНОЗ 2026-10-05 (T3-сборка на R9TN601D6GJ, лог `TerminalManager`):
      любая сессия умирает мгновенно —
      `execve(.../files/usr/bin/bash) failed: No such file or directory`,
      exit 1, чёрный экран, затем `Session initialization timeout` (30 с).
      Причина: наш SIZE-флип `useLegacyPackaging=false` (`bf48f418`)
      перестал извлекать .so в `lib/arm64/` (на телефоне пусто, проверено),
      а `TerminalManager.linkNativeLibs()` ищет `libbash/libbusybox/
      liboperit_proot/liboperit_loader/libsudo.so` ТОЛЬКО в
      `applicationInfo.nativeLibraryDir` — ссылок ноль, сессия не стартует.
      В APK все 5 .so НА МЕСТЕ (T3: `unzip -l`, `stored`, bash 1696448,
      busybox 1498688, proot 256864, loader 1632, sudo 2). Апстрим не
      affected (у них `true`). Сломано с `bf48f418`, терминал в форке не
      открывался ни разу — acceptance T1/T2/T3 его не покрывали.
    - Развилка (размер vs терминал; приоритет пользователя — installed):
      B1 (выбран пользователем, реализован `26d4964`, CI `37259656106` зелёный,
      приёмка ПРОВАЛЕНА в стадии 2): `resolveNativeLibSource` в
      `linkNativeLibs` извлекает 5 .so из APK в `files/usr/bin`, все ссылки
      `bash/proot/busybox/loader/sudo` созданы (проверено `ls -la` 06:49).
      Но сессия всё равно умирает: `execve(.../files/usr/bin/bash) failed:
      Permission denied`, exit 1. Файлы `-rwx--x--x`, `run-as` (домен
      `runas_app`) исполняет их нормально (EXIT=0); падает только exec из
      домена приложения (`untrusted_app`). Оба файла `app_data_file`
      (`ls -Z`), `/data` без `noexec`, avc в logcat подавлены (dontaudit).
      Вывод: Samsung A20s запрещает `untrusted_app` исполнять
      `app_data_file` (апстрим с `useLegacyPackaging=true` не affected —
      PM кладёт .so в `lib/` с исполняемой меткой). B1 на этом (и, вероятно,
      всех Samsung) недостаточен в принципе: из `files/` исполнять нельзя.
      Остались: A — revert `useLegacyPackaging=true` (`app/build.gradle.kts`):
      APK ~373→~283 МБ, installed +~66 МБ, терминал работает (конфигурация
      апстрима, риск минимален). C (`extractNativeLibs=true` при `false`)
      бессмыслен: проигрывает A и по APK, и равен по installed. Drop —
      терминал+Q1 хороним, размеры сохраняем (не рекомендуется — мёртвая
      фича в форке). Без решения пользователя — эксперимент стоит.
    - Тарболл лежит в АССЕТЕ САБМОДУЛЯ:
      `terminal/src/main/assets/ubuntu-noble-aarch64-pd-v4.18.0.tar.xz`
      (61.2 MiB в APK). Вынос из APK = правка сабмодуля
      (`lagovi/OperitTerminalCore`, origin наш, upstream AAswordman) +
      bump gitlink в суперпроекте; либо исключение ассета в упаковке.
    - Путь `filesDir/usr/var/lib/proot-distro/installed-rootfs/ubuntu`
      захардкожен в ~10 местах ДВУХ репозиториев: сабмодуль
      `TerminalManager.kt` (54-56, 768-775 `generateStartScript`),
      `CacheManager.kt` (33-36, 250, 329, 430-431, 449-451),
      `FtpServerManager.kt` (40-41, 44-45), `SettingsViewModel.kt`
      (131, 141), `LocalTerminalProvider.kt` (47-49),
      `SSHTerminalProvider.kt` (115-116, 242-244),
      `UbuntuDocumentsProvider.kt` (65), `LocalFileSystemProvider.kt`
      (28-32); :app `util/PathMapper.kt` (20-24),
      `data/backup/RawSnapshotBackupManager.kt` (522-525). Единый источник
      пути обязан жить в `:terminal` (:app от него зависит, не наоборот).
    - РИСК-ФАКТОР (не кодировать вслепую): rootfs на съёмной SD (FAT/exFAT,
      FUSE, noexec, нет симлинков/прав) почти наверняка НЕ СТАРТУЕТ под
      proot; под вопросом даже EXTERNAL_APP. STT-модель на SD работает,
      потому что это данные для ORT, а не исполняемое дерево.
    - Эксперимент (на принятой T3-сборке; состояние на 2026-10-05: свежая
      переустановка, терминал ни разу не стартовал — дерево не распаковано):
      в UI запустить терминал, дождаться распаковки
      из ассета, скопировать дерево в кандидатную локацию
      (EXTERNAL_APP, затем SDCARD_APP), поднять proot с подменённым
      `UBUNTU_PATH` и проверить старт. Только при успехе — писать
      `TerminalRootfsStorage` + миграцию + пикер. При провале итог честно:
      rootfs остаётся internal, выигрыш только из Q2 (минус из APK), а
      SD-механика достаётся subpack/apktool (чистые данные).
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
