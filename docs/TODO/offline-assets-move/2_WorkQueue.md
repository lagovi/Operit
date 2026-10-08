# Очередь работ

## Очередь 1: SD-first-run (без хостинга, только код + механика мест)

 1. **rootfs**: СНАЧАЛА эксперимент на устройстве, потом код (разведка
    2026-10-04, см. ниже). **ЗАВЕРШЁН 2026-10-05, вердикт FAIL (см. ниже) —
    НЕ ПЕРЕЗАПУСКАТЬ без новых фактов.** При первом запуске
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
      РЕШЕНО пользователем 2026-10-05: вариант A. Revert
      `useLegacyPackaging=true` (`cddb9906`, CI `37299257049` зелёный, APK
      287318168 байт) — PM извлёк 41 .so в `lib/arm64/` (проверено на
      телефоне), ссылки `bash/proot` ведут в `lib/arm64` (путь A, фалбэк B1
      спит). Приёмка 15:12: сессия `setup-check` жива, баннер Ubuntu, промпт
      `~ $`, дерево 358 МБ, `.operit_installed_ok` на месте. B1-коммит
      `26d4964` в сабмодуле оставлен (безвреден, страхует будущие флипы).
    - ВЕРДИКТ ЭКСПЕРИМЕНТА 2026-10-05: FAIL, обе внешние локации.
      EXTERNAL_APP (`/storage/emulated/0/...`, FUSE): файлы копируются,
      симлинки — нет (сотни `can't create symlink ... Operation not
      permitted`: `etc/ssl/certs`, `etc/alternatives`, критичные
      merged-/usr `bin/lib/sbin`, `dev/std*`). SDCARD_APP
      (`/storage/5982-1724/...`): `ln -s` запрещён сразу (`Permission
      denied`, каталог остался пуст). Без симлинков дерево не загрузится;
      обход `-L` (разыменование) удваивает размер (~700 МБ, не влезет в
      980 МБ карты) и всё равно ломает висячие ссылки (`dev/std*`, `mtab`).
      Итог честно: rootfs остаётся internal; тарболл 61 МБ остаётся в APK
      до Q2/хостинга; SD-механика (`AppDataLocation` уже есть) достаётся
      subpack/apktool — чистым данным симлинки не нужны. Мусор эксперимента
      вычищен (`q1_ubuntu_ext`, `qtest` удалены).
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
 2. **subpack (android.apk, windows.zip)**: В РАБОТЕ 08.10 (коммит
    `a4fddb40`, CI `37709173132`): новый `core/subpack/SubpackStorage.kt`
    (leaf `subpack`, stage из assets с overwrite, `migrateTo`), стейджинг
    `ApkEditor.fromAsset`/`ExeEditor.fromAsset` переведён с cacheDir на leaf
    (фалбэк cacheDir сохранён), в Settings → Data & Permissions добавлен
    `SubpackStorageSection` (тот же move-механизм, что у STT-модели, без
    речевой специфики; 5 новых строк в default-бакете). Временный
    windows-экспорт (`cacheDir/windows_export_temp`) не тронут — transient.
    Файлы в APK ПОКА ОСТАЮТСЯ (это Q1-механика; вынос — Q2 с хостингом).
    Источник файлов — upstream Google Drive через
    `ci/script/prepare_android_dependencies.py` (в дереве только `.keep`).
    Потенциал: ~59 МБ (через переезд на SD сейчас, из APK — в Q2).
    ПРИЁМКА 08.10 (сборка `2026-10-08_01-06-04Z`, CI `37709173132`,
    247299182 Б): визард чистый; строка «Export templates storage»
    рендерится с путём leaf; переезд internal → external → memory card
    без ошибок, каталог `/storage/5982-1724/.../files/subpack/` создан;
    в APK этой сборки шаблоны на месте (`assets/subpack/android.apk`
    48 МБ — новее цифры в реестре, `windows.zip` 11 МБ). НЕ покрыто:
    файлы в leaf после реального экспорта (нужен web-контент + flow
    подписи — оставлено совместному ручному тестированию; редирект
    только меняет каталог, фалбэк cacheDir на месте). Попутная находка:
    на экране Settings тапы садятся ~на 100px ниже (как низ drawer) —
    маленькую кнопку Change взял только прицелом на 100 выше; меню-попап
    при этом 1:1. Записано в чит-шит.
3. **apktool.toolpkg**: ЗАКРЫТО УДАЛЕНИЕМ 08.10 (решение «удалить», не
   переезд — фича `enabled_by_default: false`, ниша; чтение шло только из
   assets через `scanAssetPackages`+`loadToolPkgFromAsset` с распаковкой
   всего архива в кэш, т.е. ~27 МБ в APK + ~27 МБ кэша; внешний импорт
   `.toolpkg` уже есть — `PackageManagerScreen:320` +
   `scanExternalPackages`, `reconcileToolPkgCaches:902-944` сам трёт
   stale-кэш, кодовых ссылок и тестов — ноль). Ход: строка `apktool`
   вычеркнута из `tools/example_packages/packages_whitelist.txt`
   (CI sync в `normal`-режиме больше не пакует; stale-выходы скрипт
   удаляет сам; `test`-режим pr-check пакует всё по построению —
   не ломается). `examples/apktool/` НЕ тронут (рантайм тулов).
   DoD ЗАКРЫТ 08.10: CI `37722980127` success, APK 247299182 →
   220307769 Б (−26991413 Б — сошлось с 26985716 Б тулкита),
   `assets/packages/` — 1429723 Б чистого .js, `apktool`-вхождений
   в APK ноль. Папка `2026-10-08_03-29-43Z/` + `CHANGES.md`.
4. **desktop.apk, helper APKs, templates/emoji/js**: третья очередь, ~20 МБ.

## Очередь 2: on-demand download (нужен хостинг)

5. Создать GitHub Releases hosting для payload (сейчас не создан).
6. Описать каждый payload как `OfflinePayload` (образец: `GIGAAM`) с
   URL/size/sha, качать через `RemoteAssetFetcher` в `AppDataLocation`-leaf.
7. Удалить файлы из `app/src/main/assets`, убрав их из APK полностью.

## Не делать

- Общий aapt2 на два шаблона (ломает `setup_android_env.sh`, SIZE-006).
- Трогать OCR-модели и `lib/` (отдельные решения).
