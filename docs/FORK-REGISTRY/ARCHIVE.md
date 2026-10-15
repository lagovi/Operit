# HANDOFF Archive (вынесено из HANDOFF при чистке 11.10)

Закрытые разделы полностью. В HANDOFF остались только
однострочные статусы со ссылкой сюда. Не удалять: леджер
отрицательных результатов живёт в HANDOFF.

---

## Закрытые задачи 9–18 (тела из HANDOFF)

   9. Workspace-чат — ЗАКРЫТ 07.10 как поведение апстрима (расследование
    чтением кода, установка не понадобилась). Механизм: workspace — это
    ПОЛНОЭКРАННЫЙ оверлей (`AIChatScreen.kt:1243` `isWorkspaceVisible` +
    Layout ниже, `fillMaxSize` поверх всего, включая поле ввода) при
    `showWebView=true`; включается ТОЛЬКО кнопкой-иконкой Code/CodeOff
    в топбаре (блок `AIChatScreen.kt:818-843`, вызов
    `onWorkspaceButtonClick`→`toggleWebView` в `:821` — единственный
    вход в коде). Ответы: (1) апстрим, не регрессия (вся машинерия —
    нетронутый upstream-код с китайскими комментариями, в registry правок
    форка по workspace ноль); (2) выход ЕСТЬ — та же иконка Code в топбаре
    (топбар живёт выше оверлея, доступен); НЕ работает: BACK (нет
    BackHandler в AIChatScreen вообще), смена чатов (оверлей держится
    специально, `ChatViewModel.kt:689`), стрелки/свайпы/FAB — всё это
    ПОД оверлеем; (3) чат без workspace — да, норма (доказано приёмками;
    `isWorkspaceOpen` выводится из `history.workspace.isNotBlank()`,
    `ChatViewModel.kt:383-387`). Что было 05.10: агент, видимо, тапнул
    иконку CodeOff и получил фулскрин-превью без видимого выхода —
    классическая ловушка отсутствия BACK-хендлера. Фикс не требуется
    (апстрим), но если ловушка повторится у пользователя — завести задачу
    на BACK-хендлер для оверлея, якоря здесь.
  10. UX пустого чата — ПРИНЯТ 07.10 (коммиты `f3ce47f1`+`5cb753fe`,
    CI `37581930601` success, APK-папка `2026-10-07_06-31-03Z` + `CHANGES.md`).
    Реализация: в bottom bar `AIChatScreen.kt:1094` ветвь по
    `currentChatId.isNullOrBlank()` — вместо `ChatInputBottomBar` кнопка
    `+ New Chat` (`R.string.new_chat`, существует), один тап — сразу
    `createNewChat()` (setAsCurrentChat по дефолту) → появляется поле ввода.
    ОТКЛОНЕНИЕ ОТ СПЕКИ (осознанно): в задаче было «кнопка открывает
    Chat History-меню», сделано прямое создание (меньше тапов, метка
    честнее); меню доступно через кнопку-«часы». Оба гарда send
    (`AIChatScreen.kt:1822`, waifu `:1693`) оставлены как safety nets — решение
    зафиксировано здесь. Приёмка: пустое состояние без ложных контролов
    (скрин), тап → чат «Ping Test» + поле ввода, Say PONG. → PONG.
    ЛОВУШКА ПРИЁМКИ (E11): хитбокс кнопки заметно ВЫШЕ глифа (тапы в центр
    1330 и кластер 1295–1365 — мимо; сработал один из 1150–1400 при
    прочёсывании; у send та же история: глиф 1447, хит 1410). Правило для
    низа чат-экрана: целиться на ~40–100px выше визуального центра;
    промахи выглядят как «кнопка мертва» (без ripple), чинится только
    прочёсыванием, не силой. Потрачено ~1.5 ч на ложный след «onClick
    не стреляет» — механика та же, что калибровка: pointer_location
    показывает 1:1, а хитбокс живёт выше.
  11. Тост супервизии — ПОЙМАН ЖИВЬЁМ 08.10, ЗАКРЫТА. Ход: supervision
    была ВЫКЛ (свежая установка задачи 10) — тапы по тумблеру
    (`Settings` → Supervision Mode) ТРИЖДЫ не сработали (тапы по экрану
    при этом доходят — соседняя строка открывается; сам свитч тапом не
    берётся, причина не выяснена — НЕ чинить UI, обход ниже). Обход:
    тумблер — это SharedPreferences `supervision_preferences.xml`, ключ
    `SupervisionPreferences.kt` (`supervision_enabled`, дефолт false;
    в DataStore его НЕТ — `functional_configs` хранит только биндинги);
    XML записан через `run-as ... tee` при убитом приложении, после
    рестарта тумблер фиолетовый (скрин). Ход «Say» → ответ
    «What would you like me to say?» → burst 25 кадров: кадры 3–7 —
    белый кард «The driver correctly asked for clarification.» с ×,
    кадр 8 — полупрозрачное гашение, дальше чистое поле. В БД
    (`app_database` + `-shm` + `-wal` все три!) строка id=8 SUPERVISION
    verdict=ok, комментарий совпадает с кардом один в один. DoD закрыт:
    PNG (`/tmp/opencode/t11/toast_proof.png`, гашение
    `toast_fading.png`) + id=8. Пруфы лежат в /tmp (не переживёт ребут
    ноутбука — при нужде перестянуть: тост висит ~5 с, метод бурста
    воспроизводим). Историческая справка (как ловили): рендер — stock
    `ChatToastHost` (`AIChatScreen.kt:1459`, верх по центру, монтируется
    безусловно), длительность ~5 с (`estimateToastDurationMs`); метод —
    burst-серия back-to-back `screencap` ~2 с/кадр 20–30 шт +
    `md5sum`-дедуп (E7 + «Ловля тостов» в чит-шите); НЕ каждый ход даёт
    комментарий (пустой = нечего сказать = тоста нет по дизайну).
  12. Q1 п.3–4: apktool.toolpkg и desktop/helper/templates (ОТКРЫТА;
    порядок внутри: сначала п.3(a), потом п.4(b);
    CI ~25 мин на круг, приёмка — телефон с зарядом).
    (a) apktool.toolpkg (~26 МБ): РЕШЕНИЕ ПРИНЯТО 08.10 — УДАЛИТЬ
    (не переезжать). Разведка показала: чтение только из assets
    (`PackageManager.scanAssetPackages:1261` →
    `loadToolPkgFromAsset:2029` с распаковкой ВСЕГО архива в кэш
    `prepareToolPkgAssetCache:1993` — т.е. ~27 МБ в APK + ~27 МБ кэша
    за фичу `enabled_by_default: false`); внешний `.toolpkg`-импорт
    уже работает (`PackageManagerScreen:320` +
    `scanExternalPackages:1276`); `reconcileToolPkgCaches:902-944`
    сам удаляет stale-кэш старых установок; кодовых ссылок на
    бандл — ноль (`OfflinePayloads.kt:19` был единственной),
    тестов — ноль. Ход: `apktool` вычеркнут из
    `tools/example_packages/packages_whitelist.txt` (CI sync
    `normal`-режим больше не пакует; stale-выходы скрипт удаляет сам;
    `test`-режим pr-check пакует всё по построению — не ломается;
    `npm run build:examples:github` трогает только пример github).
    `examples/apktool/` НЕ тронут. DoD ЗАКРЫТ 08.10: CI `37722980127`
    success, APK 247299182 → 220307769 Б (−26991413 Б — сошлось с
    26985716 Б `apktool.toolpkg` до байта точности), `assets/packages/`
    теперь только .js (1429723 Б), `apktool`-вхождений в APK ноль
    (`python3 zipfile`). Папка:
    `/home/uzzzver/operit-fork/apk/2026-10-08_03-29-43Z/` + `CHANGES.md`.
    На телефоне не ставилась (удаление из APK ломать нечему:
    кодовых ссылок ноль, приёмка отсутствия — опционально при
    следующем install).
    (b) desktop.apk (6.5 МБ) + helper APKs (shizuku/accessibility ~3–5 МБ)
    + templates/emoji/js (~14 МБ): РАЗОБРАНО 08.10, решения разные.
    `desktop.apk`+`desktop_version.txt` — МЁРТВЫЙ ВЕС (ноль ссылок
    во всём репозитории кроме таблиц размеров; `assets/README.md`
    документирует только shizuku): удалены `git rm`, DoD ЗАКРЫТ —
    CI `37726800230` success, APK 220307769 → 214210621 Б (−6097148),
    `desktop*`-вхождений остался один виджет-xml (не оно). Папка
    `2026-10-08_04-17-45Z/` + `CHANGES.md`.
    shizuku.apk/accessibility.apk — ЖИВЫЕ (читаются по требованию:
    `ShizukuInstaller.extractApkFromAssets:37` → cacheDir → install
    intent; `UIHierarchyManager:89-91` так же; версии из
    `*_version.txt`): в Q1 не выносятся (без хостинга APK не похудеет,
    код под 5 МБ несоразмерен) — идут в Q2 on-demand (задача 13),
    карта читателей уже здесь. templates (9.8 МБ, 187 файлов) —
    сид проектов: `WorkspaceUtils.copyTemplateFiles:682` копирует
    `templates/$name` из assets в workspaceDir при создании (рантайм
    читает файлы, не assets) — тоже Q2 (скачать сид вместо бандла).
    emoji (3.6 МБ) — `CustomEmojiRepository:298` (`emoji`-каталог
    assets) — Q2. js/ (1.4 МБ: pako/CryptoJS/Jimp/UINode/OkHttp3/
    katex/terser — `JsAssetLoader`, `JsEmbeddedLibraryLoader`,
    `LatexMathMlConverter:10`, `ToolPkgJsAstMinifier:50`) — ГОРЯЧИЙ
    рантайм JS-движка, offline: НЕ ТРОГАТЬ вообще (ни Q1, ни Q2).
    ЗАПРЕТ исполнен: `examples/apktool/` не тронут (это было (a)).
    (c) НЕ трогать (действует дальше): OCR/движки (решение отдельно),
    `lib/` и dex (неприкосновенны — E2), rootfs (FAIL — E3, только Q2),
    aapt2-дубли (wontfix — ломает `setup_android_env.sh`, SIZE-006),
    js/ assets (горячий рантайм, см. выше).
    Вскрытие desktop.apk 08.10 (из архива `2026-10-08_03-29-43Z`,
    распакован в `/tmp`, в репо не возвращался): 161 entry, 6.5 МБ;
    classes.dex 13.5 МБ + classes2.dex 4 МБ (некомпакт), обфусцированные
    res-имена (`Qr.xml`, `-6.webp` — R8), Kotlin+Compose, из нативного
    только `libandroidx.graphics.path.so` (4 ABI); dex-строки:
    `com.ai.assistance.operit.desktop` (`DesktopScreen`, `AppIconCard`,
    `OperitDesktopTheme`) — апстримный companion-лаунчер, ни к чему
    не пришитый: ни инсталлера, ни интента, ни документации.
    Удаление подтверждено повторно — не восстанавливать.
  13. Q2 on-demand: убрать payload из APK совсем (ОТКРЫТА, после 12;
    хостинг РЕШЁН 08.10 — GitHub Releases в ТОМ ЖЕ репозитории, см. 7c).
    SUBPACK ГОТОВ 08.10 (коммит `a8d61d75`, CI `37772193743` success):
    `OfflinePayload.SUBPACK_ANDROID/WINDOWS` (`util/OfflinePayloads.kt`,
    URL `.../releases/download/v1.12.1+4/...`, size+sha с APK
    CI `37726800230`), `SubpackStorage.ensureTemplate` (leaf,
    `RemoteAssetFetcher`, Q1-копии узнаются по size+sha — докачки нет,
    отказ возвращается в `onComplete`, фалбэка на assets НЕТ);
    `ExportDialogs` оба flow переведены на provision-then-use
    (android: `fromAsset`→`fromFile`, прогресс 0.05–0.30;
    windows: staged-копия в `windows_export_temp`, остальной flow
    не тронут); CI больше не тянет `subpack.zip` с Drive
    (`download_android_dependencies.sh` + `prepare_android_dependencies.py`
    вычищены, кэш-ключи хешируют скрипты — пересоберутся сами);
    релиз `v1.12.1+4` создан ВМЕСТЕ с кодом (оба файла залиты,
    sha сошлись с зашитыми); APK 214210621 → 177474049 Б (−36736572,
    `assets/subpack` пуст — проверено `zipfile`), папка
    `2026-10-08_11-45-43Z/` + `CHANGES.md`. НЕ ЗАКРЫТ DoD: чистая
    установка + экспорт до конца (телефон 08.10 днём НЕДОСТУПЕН:
    порты 36255/38571/5555/37000–45200 закрыты — нужен свежий порт
    от пользователя). Остаток Q2: helper APK (карта в 12b),
    templates/emoji — отдельными заходами по тому же лекалу
    (payload-объект → ensure → caller → релиз → замер);
    `apktool.toolpkg` из APK уже удалён (12a) — хостить его как
    скачиваемый опционально, для размера не нужно. Rootfs — последним
    и только если proot с внешнего leaf стартует (E3 против;
    отдельный эксперимент с откатом). Лекало для остальных payload
    (отработано на subpack, повторять буквально): (а) объект
    `OfflinePayload` (имя файла В leaf = имя staged-копии, если была, —
    тогда миграция бесплатна; size+sha снять `python3 zipfile`+sha256
    с последнего APK где файл ещё был); (б) `ensureX` suspend
    в Storage-классе через `RemoteAssetFetcher` (отказ — наружу,
    фалбэка нет); (в) caller на provision-then-use (прогресс
    прокинуть, вызывать внутри уже-suspend контекста); (г) убрать
    источник из CI/prepare (кэш-ключи сами пересоберутся);
    (д) `gh release create` — ТОЛЬКО с файлами, пустых не делать
    (7c: тег версии `vX.Y.Z`, по файлу на payload, короткая записка);
    в начало записки — ALPHA-плашка (100% vibe-coded, стабильность —
    апстрим) + флаг `--prerelease` (решение пользователя 10.10,
    действует для всех релизов форка);
    (е) CI → замер `zipfile` → папка+`CHANGES.md` → приёмка закачки
    на чистой установке (телефон+порт). Порядок: helper APK →
    templates/emoji (карта читателей в 12b).
    Образец: `OfflinePayload.GIGAAM` (`util/OfflinePayloads.kt`) +
    качалка `util/RemoteAssetFetcher.kt` (докачка, Range, SHA-256) +
    регистрация в `OfflinePayload.all`. Шаги: (а) описать каждый payload
    (`subpack-android.apk`, `subpack-windows.zip`, `apktool.toolpkg`,
    далее по очереди) с URL/size/sha — URL появятся только после (б);
    (б) создать ПЕРВЫЙ релиз вручную (`gh release create vX.Y.Z`,
    практика именования — в 7c) и залить файлы; пустых релизов не делать;
    (в) удалить файлы из `app/src/main/assets` (и из Drive-зависимого
    prepare-шага, если он их кладёт) + перевести чтение на File/leaf
    (Q1-механика уже даёт leaf — переиспользовать `SubpackStorage` /
    наследников, не плодить вторую); (г) миграция для существующих
    установок: staged-копии из Q1-leaf подхватываются без повторной
    закачки (сверить размер/sha, докачать недостающее). DoD: APK следующей
    сборки МИНУС payload-МБ (замерить `python3 -c zipfile`, не `unzip -l` —
    ловушка в чит-шите) + чистая установка качает payload с релиза
    и работает offline после. Порядок payload: subpack → apktool →
    desktop/helpers → templates. Rootfs (61 МБ тарболл) — последним и
     только если proot с внешнего leaf стартует (E3 говорит «нет» для
     дерева; тарболл≠дерево — pre-seed в internal может и пройти, но это
     отдельный эксперимент с откатом).
    LIVE-FIRE 09.10 (сборка `2026-10-08_11-45-43Z`, чистая установка;
    телефон после разблокировки bootloader, `192.168.1.69:42323`
    (далее связь — постоянный `192.168.1.69:5555`, root, см. чит-шит),
    батарея 61–67%): ЗАКАЧКА ДОКАЗАНА — `files/subpack/
    apk_editor_android.apk` 48139093 Б + sha256 `c56b23a8...` сошлись
    с зашитыми (`SUBPACK_ANDROID`); телефон дотянулся до GitHub
    Releases сам. РЕПАК УПАЛ: `ExceptionInInitializerError`
    `ExtraFieldUtils.<clinit>` — у `AsiExtraField` нет `<init>`
    (R8 вычистил конструктор; падает `copyZipEntry:339`
    `outEntry.time = ...`, flow `ExportDialogs.kt:875`).
    ФИКС — keep конструкторов (`app/proguard-rules.pro`, коммит
    `b54dffbe` запушен): настоящая причина, не фолбэк; аудит пути —
    другой рефлексии нет (apksig/apkparser/zip4j уже `{ *; }`,
    sevenz на export-пути нет). CI НЕ ЗАПУЩЕН: 422 «Actions has been
    disabled for this repository» на оба workflow; repo-флаг
    enabled=true, репо не disabled/archived — блок выше репозитория,
    нужен владелец (Settings → Actions). Локального SDK нет
    (в `/opt` только platform-tools) — собрать негде.
    Остаток приёмки после починки CI: install `-r`, в HTML Packager
    перепоказать папку ПАЛЬЦЕМ, экспорт до конца + offline-повтор
    при выключенном WiFi (leaf уже с шаблоном — докачки быть
    не должно). SAF-ВЫВОД ДЛЯ ЧИТ-ШИТА: строки папок и кнопка USE
    в DocumentsUI глухи к adb (`tap`/ENTER/SPACE/CENTER —
    проверено прочёсыванием; фокус TAB/DPAD ходит, тулбар/диалоги/
    тапы по файлам работают). Рабочее: «Создать папку» тулбаром
    (тап/ENTER) — пикер сам входит в новую папку; USE — только
    пальцем. Подтверждение пользователя 09.10: «панелька живая,
    через adb просто нельзя».
    CLOSE-OUT 10.10: DoD ЗАКРЫТ полностью. Сборка `2026-10-10_00-41-11Z`
    (CI `38010108122`, APK 179691879 Б): чистый install → закачка 48 МБ
    → репак → zipalign → подпись → `Download/Operit/exports/
    WebApp_*.apk` 48094313 Б → `adb install` УСПЕШНО
    (`com.example.webproject`); offline-повтор (интернет закрыт только
    приложению через iptables owner-match — `svc wifi` НЕЛЬЗЯ, он убивает
    ADB) успешен за секунды, в logcat ни одной докачки, размер
    байт-в-байт тот же. Палец больше НЕ НУЖЕН ВООБЩЕ: root-тапы через
    sendevent (`/dev/input/event3`, MT-B, 1:1 к координатам дампа)
    неотличимы от пальца — доказано на USE+Allow (`ci/script/
    phone_lab.sh`, приёмка закодирована в `ci/script/
    accept_q2_export.sh`, синтаксис проверен, первый полный прогон —
    на следующем payload).
  14. Onboarding: один экран вместо трёх (РЕАЛИЗОВАНО в коде 10.10,
  приёмка на устройстве — pending; решение пользователя: длинные тексты
  соглашения долой, тур за отдельной кнопкой). Новый
  `ui/features/permission/screens/OnboardingScreen.kt`: сверху карточка
  «Your responsibility» (`onboarding_liability_*`: всё хорошее и плохое —
  ответственность пользователя, разработчик не отвечает ни за что) +
  кнопка «What can it do?» (тур — `FeatureTourDialog`, 3 intro-страницы,
  вне флоу); дальше 4 базовых разрешения карточками (те же интент-обработчики);
  дальше 4 уровня (STANDARD/ACCESSIBILITY/DEBUGGER/ROOT, ADMIN в UI не
  предлагался и раньше); внизу всегда активный Continue — без выбора
  пишется STANDARD. `MainActivity`: ветки Agreement/Guide заменены одной
  (`!accepted || level==null → OnboardingScreen`, onComplete пишет
  `acceptCurrentAgreement()` + `checkPermissionLevelSet()` + дальше как
  раньше). Удалены: `PermissionGuideScreen.kt`, мёртвые строки
  (welcome_*, warning_*, complete, confirm, step-метки); `AgreementScreen.kt`
  ОСТАВЛЕН (показ соглашения из Toolbox, `OperitScreens.kt:686`).
  Якорь UI-001 переведён на новый файл. Добивка 10.10 (решение
  пользователя): модалка маркета `MarketAgreementDialog` (блок на первом
  входе в маркет + пункт «Market Agreement» в Mine) УДАЛЕНА целиком —
  файлы диалога и `MarketAgreementPreferences.kt`, строки
  `market_agreement_*`, ветка в `UnifiedMarketScreen.kt`; вместо неё одно
  предложение в карточке онбординга (`onboarding_plugins_note`: сторонние
  плагины выполняют код своих авторов — установка на свой риск).
  Скрипт приёмки адаптирован: шаг 3 теперь ждёт «Your responsibility»,
  свайпами доводит «Continue» до видимости, тапает, ждёт «New Chat».
  Первый живой прогон 10.10 (APK `20261010_094027Z`, CI 38042218605):
  root/install/grants/onboarding — OK (новый онбординг работает);
  дальше два бага, оба в скрипте/флоу, не в приложении: (1) `cd` в скрипте
  вёл не в корень репо — починен; (2) всплывшая Announcement-модалка
  уводила тапы мимо — по решению пользователя модалка УДАЛЕНА целиком
  (`RemoteAnnouncementDialog` + `RemoteAnnouncementRepository` +
  `RemoteAnnouncementPreferences` + ветка в `OperitApp`; в скрипте оставлена
  защита от «Got it» на всякий случай); (3) picker: `Select Folder`
  иногда не открывается с первого тапа (sendevent-флак, известный) —
  в скрипте повтор до 3 раз. Состояние приёмки 14 к ночи 10.10:
  ДОКАЗАНО тремя прогонами (APK 094027Z, 132740Z, 160304Z):
  onboarding (Continue → чат), picker, folder-pick, download 48 МБ,
  repack. НЕ ДОКАЗАНО (нужен зелёный билд): диалог тура
  (открыть/листать/закрыть), выбор уровней, строки разрешений тапами,
  отсутствие модалки маркета и Announcement на первом входе.
  Скрипт к ночи: модель ожиданий (`plab_expect_text` — каждый этап
  заявляет ожидаемый экран, провал печатает экран + FAIL, висения нет),
  heartbeat в долгих ожиданиях, wake + screen_off_timeout с restore-trap,
  install с причиной + повтор, basename выходного APK.
  CLOSE-OUT 14, ночь 10.10 (APK `20261010_215026Z`, ручная сессия,
  батарея 34→~28%): тур — открыть/1-2-3/Close (возврат на онбординг);
  уровни — Debug выбран ВИДНО (пурпурная рамка + залитое радио, скрин
  `/tmp/opencode/t14_levels.png`), затем Standard + Continue → чат,
  в `android_permission_preferences.preferences_pb` записано
  `preferred_permission_level=STANDARD`, согласие
  `accepted_agreement_version` на месте; строка Storage Permission тапом
  открывает системную «All files access» (тумблер Вкл. — appops переживает
  `pm clear`); маркет — розовая FAB (хитбокс (643,1280), не глиф!) ведёт
  СРАЗУ в ленту без Agreement/countdown (первый вход); Announcement на
  свежей установке не вылезал ни разу (×2 прогона — сразу онбординг).
  DoD ЗАКРЫТ полностью. Попутно: рантайм-перевод жив на 15-коде
  (`楼层限制器`→«Floor Limiter»+EN-описание). Телефон оставлен: свежая
  установка 215026Z, онбординг пройден, STANDARD, чат готов.
  Прогон 10.10 вечером (APK `20261010_132740Z`, CI 38055794385, 15-код):
  скрипт доведён до ума (шаги видны, adb-timeout 90с, install с причиной+
  повтор), но приёмка встала: install-флап (вручную ок) → повторный прогон
  умер на транспорте (`error: closed`), телефон 192.168.1.69 не пингуется,
  adb offline, скана LAN по 5555 (.2–.254) — пусто. Нужен пользователь:
  проверить телефон (питание/WiFi/IP). Следом: CI-билд (announcement-
  removal + логотипы + скрипт) и полная приёмка.
  ПРИЁМКА 15 (APK `20261010_132740Z`, CI 38055794385): onboarding/picker/
  folder/download/repack — OK, но подпись упала: PKCS12 unwrap
  (SecretKeyFactory PBES2 unavailable) + JKS getInstance fail. A/B против
  APK 094027Z (подписывает OK) + сравнение dex (BC-классы 5433=5433, refs
  7913=7913) + keystore md5 совпал → дело не в контенте, а в first-match:
  вставленный bundled-BC затеняет платформенный, его сервис инстанцируется,
  но на load падает, остальных JCA уже не пробует. Фикс: `KeyStoreHelper`
  переписан на loadVerifiedKeystore (итерация по провайдерам типа с
  проверкой aliases, итоговая ошибка как раньше), insert/remove провайдеров
  убраны вовсе, `ApkReverseEngineer` переведён на новый лоадер. Это не
  fallback: верификация обязательна, тотальный провал виден как раньше. DoD: чистая установка → Continue
  → сразу чат; отдельно: тур открывается/листается/закрывается, уровни
  выбираются, разрешения тапаются в системные настройки.
  15. Китайские интеграции — вынести ВСЁ (КОД ГОТОВ 10.10 вечером,
  решение пользователя «убрать всё» выполнено; осталось: зелёный CI-билд
  уже есть — `20261010_170331Z` (CI `38070179633`, включает insert+iterate
  и диагностику), нужна ПОЛНАЯ приёмка скриптом на нём + проверка
  пунктов «НЕ ДОКАЗАНО» из задачи 14. Ниже — инвентарь (как было до
  выноса) и запись реализации в конце пункта.
  (все пути — от корня репо, размеры — байты в APK `2026-10-10_00-41-11Z`
  через `python3 zipfile`, если не сказано иное):
  (а) QQ Bot (Tencent QQ OpenAPI, только CN-рынок: нужны QQ-credentials):
  `examples/qqbot/` (исходники ~520 КБ), в whitelist
  `tools/example_packages/packages_whitelist.txt` → пакуется в
  `assets/packages/` + ставится в toolpkg_cache; UI — плитка Toolbox
  «QQ Bot Settings». Путь — как 12a (apktool): вычеркнуть из whitelist,
  stale-кэш чистит сам `reconcileToolPkgCaches`, DoD — замер CI
  (`python3 zipfile`, `*qqbot*` ноль). НЕЯСНО: точный размер .toolpkg
  (собрать sync-скриптом и взвесить до решения);
  (б) Пресеты CN-провайдеров — УБРАТЬ (размера ноль, чистый интерфейс):
  `provider_baidu/provider_alipay_bailing/provider_aliyun` (`app/.../res/
  values/strings.xml:4138+`), endpoints Baidu/Xiaomi
  (`ApiProviderConfigCollect.kt:90,152`, `ModelConfigData.kt:24`,
  `MimoProvider.kt`, `MimoVoiceProvider.kt`), тестовый URL baidu.com
  (`ToolTesterScreen.kt:326`), схема alipay: в Token-WebView
  (`TokenConfigWebViewScreen.kt:108`, `WebViewConfig.kt:126`), строки
  test_desc (`strings.xml:3045`). Убрать полностью: типы из
  `ApiProviderType`, ветки UI, строки, дефолтные endpoints;
  (в) Карта CN-приложений — УБРАТЬ (`StandardUITools.kt:65-123,248-249`):
  微信/QQ/微博/淘宝/百度地图/bilibili/腾讯视频/QQ音乐/QQ邮箱/腾讯新闻 →
  package names. Убрать целиком (EN-пользователю открывалки китайских
  приложений не нужны); проверить, не упадёт ли инструмент без карты
  (дефолтная ветка/ошибка вместо CN-списка);
  (г) HuaweiCloud-maven в шаблонах экспорта — УБРАТЬ
  (`assets/templates/{android,flutter}/.../settings.gradle.kts`,
  `build.gradle.kts`): зеркало для CN; перед удалением доказать на
  сборке экспортного проекта без него (иначе откат);
  (д) Проверено НЕ китайское (не трогать): OCR-движок — Google MLKit
  `com.google.mlkit:text-recognition:16.0.0` (`gradle/libs.versions.toml:18`,
  `app/build.gradle.kts:649`, `libmlkit_google_ocr_pipeline.so` 9.5 МБ,
  скриптовых пакетов zh/ja/ko в зависимостях НЕТ; активная модель —
  ТОЛЬКО латинская (`OCRUtils.kt:31,52`: `latinRecognizer`,
  `TextRecognizerOptions.DEFAULT_OPTIONS`; CJK-распознаватели — отдельные
  артефакты, не подключены; подтверждено чтением кода 10.10); шрифтов CJK в assets
  НЕТ (только `terminal/.../jetbrains_mono_nerd_font_regular.ttf`);
  `res/` — только `values`+`values-night` (values-zh НЕТ); MNN
  (`libMNN.so` 20.9 МБ + Wrapper 5.3 МБ) грузит ЖИВОЙ `llm/mnn`
  (`MNNLibraryLoader.kt:33,37`, `MNNModule.kt`) — удаление только
  вместе с фичей MNN-LLM (решение пользователя, НЕ free win;
  мёртвого MNN-STT в коде не осталось — проверено grep 10.10);
  push-SDK (Huawei/Xiaomi/Oppo/Vivo/JPush) в зависимостях НЕТ.
  РЕАЛИЗАЦИЯ 10.10 (код, в отдельном CI-билде после приёмки 14):
  enum `ApiProviderType` −16 (BAIDU/ALIYUN/XUNFEI/ZHIPU/BAICHUAN/MOONSHOT/
  MIMO/DEEPSEEK/SILICONFLOW/IFLOW/INFINIAI/ALIPAY_BAILING/DOUBAO/PPINFRA/
  NOVITA/MINIMAX); удалены файлы Deepseek/Kimi/Mimo/Qwen/Doubao-провайдеров
  + их TTS-четвёрка (SiliconFlow/MiniMax/Mimo/DoubaoVoiceProvider) и типы
  SILICONFLOW_TTS/MINIMAX_TTS/MIMO_TTS/DOUBAO_TTS; фабрики (`AIServiceFactory`,
  `VoiceServiceFactory`, `ModelListFetcher`) — ветки вырезаны (там где был
  else — тихо, где exhaustive — правилось по компилятору); коллекты:
  `ApiProviderConfigCollect` −16 пресетов, `ModelPricingDefaultsCollect`
  (set+map), `ScrapedModelPricingRowsCollect` −142 строки + пустые блоки,
  `ModelThinkingConfigDefaultsCollect` −CN-правила (DEEPSEEK/MOONSHOT/MIMO/
  DOUBAO/ALIYUN/SILICONFLOW/ZHIPU/opencode-zhipu) + minimax-хвосты;
  QQBot: строка whitelist + `examples/qqbot/`; `AutoGlmOneClickToolScreen`
  (Zhipu one-click) + роут + Toolbox-пункт + строки; DeepSeek token-webview
  кластер (`DeepseekApiConstants/JsScripts/DeepseekJsInterface`, мёртвый);
  alipay:/weixin:-схемы в обоих webview-клиентах; baidu-URL и desc в
  ToolTester; карта CN-приложений в `StandardUITools` (весь CN-блок + QQ/
  WeChat/TikTok/Temu/bilibili/keep); зеркала aliyun/huaweicloud/tuna/bfsu/
  tencent из шаблонов экспорта (gradle + setup-скрипты, остался официальный
  upstream); дефолты → OpenAI (`ApiPreferences`, `ModelConfigManager`,
  `ApiConfigDelegate`, `UrlConfig`, `ModelConfigData`); старые конфиги
  мигрируются в OPENAI_GENERIC персистентно (`migratePreferencesFromVersionFour`,
  версия стора 4→5, ключи/эндпоинты сохраняются); TTS-тип читается через
  runCatching→SIMPLE_TTS; `saveDeepSeekConfiguration` обобщена в
  `saveInitialConfiguration`; `enableDeepSeekWebSearch` удалён по всей
  цепочке; строки provider_*/speech_services_cn_*/enable_deepseek_* стёрты;
  тесты поправлены (ThinkingQuality — CN-кейсы удалены, third-party теперь
  UNSUPPORTED; остальные фикстуры DEEPSEEK→OPENAI); логотипы провайдеров
  `assets/model_logos/` −16 CN-папок; PROMPT-002 и 5 его
  allowlist-записей удалены (файл мёртв); 3 укороченные CN-строки промптов
  до-allowlistены. НЕ тронуто: комментарии про wire-протоколы, model-name
  триггер free-модели в чате, minimax-ветка роутинга OpenCode (машинерия
  агрегатора), не-whitelist примеры в `examples/` (в APK не пакуются).
  16. Install-size раунд 2 — ЗАКРЫТА решением пользователя 10.10
  (не трогаем); инвентарь ниже оставлен как справка, не план. База: APK 179.7 МБ, installed_linux: assets ~155 МБ stored
  + копии в `files/` (rootfs 61 МБ тарболл + распаковка ~358 МБ,
  toolpkg_cache ~31 МБ) — цифры research-size, перед работой перемерить
  на настроенном устройстве (`run-as ... du` по `files/*`: objectbox,
  translation_cache, media_pool, skill_repo_zip_pool, image_pool,
  custom_emoji, logs — сколько реально весят, записать сюда).
  Кандидаты delete (все — решение пользователя, цифры из APK 10.10):
  avatar-стек ~14.5 МБ (`libfilament-jni`+`libgltfio-jni` 5.2,
  `libdragonbones_native` 2.6, `libMmdWrapper` 5.1, `libFbxWrapper` 1.6)
  — только вместе с фичей аватаров; FFmpeg ~26 МБ (`libavcodec` 15.7,
  `libavfilter` 5.2, `libavformat` 5.1) — только вместе с Toolbox
  «FFmpeg Toolbox» + `ffmpeg.js` toolpkg; llama-стек ~23 МБ
  (`libllama-common` 12.6, `libllama` 7.9, `libggml-*` 3.1) — только
  вместе с GGUF-инференсом; MNN ~26 МБ — см. 15(д); MLKit 9.5 МБ —
  только вместе с OCR (фича живая: `OCRUtils`, screen-OCR во флоатере).
  Кандидаты на SD/out-of-APK (технические, без убийства фич):
  helper APK (`assets/shizuku.apk` 2.5 + `accessibility.apk` 2.7 МБ,
  читатели `ShizukuInstaller:37`, `UIHierarchyManager:89-91`) и
  templates (11 МБ, `WorkspaceUtils.copyTemplateFiles:682`) + emoji
  (3.7 МБ, `CustomEmojiRepository:298`) — тем же Q2-лекалом задачи 13;
  toolpkg_cache (переезд вслед за SubpackStorage-механикой);
  aapt2-дубли в templates (2×4.5 МБ) — НЕ ТРОГАТЬ (ломает экспорт,
  SIZE-006). НЕ ТРОГАТЬ ВООБЩЕ: rootfs (E3: proot с внешнего leaf не
  стартует — эксперимент с преддоказательством и откатом, отдельно),
  `lib/`+dex кроме перечисленного (E2), `assets/js` 1.4 МБ (горячий
  рантайм JS), keystore assets, `bridge/`, `operit_shell_exec`.
  DoD раунда: таблица «модуль → МБ в APK → МБ installed → решение»
  здесь + по каждому взятому — как 12a (удаление+замер) или 13
  (on-demand+приёмка `accept_q2_export.sh`-стилем).
  ПРИЁМКА 15 ЗАКРЫТА ночью 10.10: `accept10` на `20261010_215026Z`
  (15-код + evict-фикс) — PASS=10 FAIL=0: install/grants/onboarding/
  picker/folder/download 48 МБ/repack/sign (`WebApp_1791671054137.apk`
  48094313 Б)/leaf-intact/offline-без-докачки. A/B «094027Z подписывает»
  больше не нужно (механизм доказан прямо).
  17. Регрессия подписи экспорта (ЗАКРЫТА 10.10 ночью; БЛОКЕР снят).
  Симптом был: `Export Failed` — `Unable to load keystore`,
  `JKS not found`; выходной `WebApp_*.apk` остаётся НЕподписанным
  (`keytool -printcert -jarfile` → `Not a signed jar file`).
  ДОКАЗАНО (замеры, не догадки): (а) keystore побайтово цел (md5 device ==
  md5 `app/src/main/assets/pkcs12.keystore`); (б) BC-классы в dex
  идентичны (5433=5433, refs 7913=7913 — только через `python3 zipfile`,
  `unzip` в системе НЕТ);   (в) [ОТМЕНЕНО закрытием 17: вероятно, keytool проверял несвежий файл,
  ловушка (т) — не цитировать] APK 094027Z якобы подписывал, 132740Z/
  160304Z — нет; (г) пароли/алиас — константы
  (`ExportDialogs.kt:862-870`: файл из `getOrCreateKeystore`, `android`/
  `androidkey`/`android` — код не менялся); (д) вставка bundled-BC
  отклоняется системой (`insertProviderAt` → −1, дубликат имени «BC»
  запрещён — значит вставка из a2764e9c всегда была no-op, работал
  платформенный BC); (е) итерация показала: `KeyStore.PKCS12` предлагает
  только один «BC» (платформенный), JKS не предлагает никто (на Android
  норма, не ошибка).
  НЕ ДОКАЗАНО (ядро загадки): один и тот же платформенный BC, файл,
  пароль — в старых сборках открывает ключ, в новых нет. Опровергнуто:
  порча файла, вырезанные классы, JKS, пароль/алиас, шаблоны, скачивание.
  Под вопросом: отличие состояния процесса (какие провайдеры реально
  стоят — в старых сборках логов не было); флейк платформы через
  перезагрузки (телефон вечером пропадал из LAN и вернулся).
  Открытые концы: (1) RESOLVED 10.10 ночью: строка `inserted at −1`
  в 20:40 — это был accept8 на `170331Z` (код со вставкой), никакой
  аномалии: сборка ДО 170331Z такой строки не содержит (проверено grep
  точной фразы по dex всех APK); путаница была из моей ошибки —
  я дважды писал что accept8 едет на 160304Z; (2) JKS-путь мёртв всегда
  (на Android провайдера нет) — чинить только PKCS12.
  КОРЕНЬ НАЙДЕН 10.10 ночью (диагностика `20261010_203519Z`, CI
  `38084332791`, прогон accept9: onboarding/picker/folder/download/
  repack — OK, подпись — нет; лог `files/logs/operit.log` через run-as):
  keystore — PBES2/PBKDF2/AES-256-CBC (проверено локально `openssl pkcs12
  -info`, файл от 09.09), а на телефоне НЕТ PBES2 SecretKeyFactory
  ВО ВСЁМ JCA (прямой проб — unavailable; `KeyStore.PKCS12` предлагает
  только платформенный BC 1.61; old-style резолвится туда же). Вставка
  bundled-BC отклоняется (−1, имя «BC» занято платформой). История:
  до-`a2764e9c` код делал remove+insert (upstream-механика) и подпись
  работала; `a2764e9c` убрал remove («не возвращать» — memo (ж) был
  НЕВЕРЕН) + добавил keep — с тех пор только платформа → PBES2
  мёртв. A/B «094027Z подписывает» — ПОДОЗРИТЕЛЬНО (возможно, keytool
  проверял несвежий `WebApp_*.apk`, ловушка (т)): переподтвердить
  чистым прогоном. ФИКС (коммит после диагностики, CI `38089152700`):
  `registerBouncyCastleProvider` снова remove+insert (evict платформы
  из «BC»-слота + вставка bundled на 1 + лог класса в слоте); keep
  `org.bouncycastle.**` на месте (`proguard-rules.pro:77`); итерация
  `loadVerifiedKeystore` остаётся верификацией. DoD по зелёному:
  чистый прогон + keytool ТОЧНО свежего файла (имя+время) + install.
  ЗАКРЫТИЕ 17 (APK `20261010_215026Z`, CI `38089152700`, accept10
  PASS=10 FAIL=0): в логе `BC/1.78` первый (платформа evicted),
  `PBES2 SecretKeyFactory offered by BC`, `PKCS12 loaded via BC` ×2;
  свежий `WebApp_1791671054137.apk` (01:24, 48094313 Б): keytool —
  Signer #1 самоподпись CN=1, `adb install` — Success.   15-коммит
  невиновен (бисектить нечего); A/B 094027Z — артефакт несвежего файла,
  больше не цитировать.
  18. Идентичность форка (решение пользователя 10.10: B+C сейчас, D позже).
  B — строка атрибуции `fork_attribution` («English-only community fork
  of Operit (upstream: github.com/AAswordman/Operit). ALPHA build, use at
  your own risk.»): в карточке онбординга (после plugins-note, мелким
  кеглем) + в About под версией (там упоминания форка не было вообще).
  C — debug-ярлык «Operit Debug» → «Operit EN» (`app/build.gradle.kts`
  resValue; clone-флейвор не тронут). D (переименование пакета
  `...operit.*` ради сосуществования с апстримом) — СНЯТ 15.10
  (решение пользователя: забыть, не делать вообще).
  CLOSE-OUT 18 (APK `20261011_123918Z`, `accept_bc_identity.sh` + `scen/
  bc_identity.uaw`): PASS=8 FAIL=0 — атрибуция на онбординге и в About,
  ярлык «Operit EN» в лаунчере, старого ярлыка нет. По пути чинен тап
  Continue (цикл свайп→тап→проверка чата).


---

## Что сделано, чтобы не переделывать

## Что сделано, чтобы не переделывать

Диагноз предыдущего агента был **неверен**, и это главное, что нужно знать.
Он объяснял остатки китайского хардкод-литералами. На деле `values-en` был
полон (7659/7659), хардкод в `:app` вычищен — а китайский всё равно появлялся.
Четыре независимые причины, каждая выглядит как «незаполненный перевод»:

1. **Неподдерживаемая системная локаль** протекала в `Configuration` → Android
   откатывался на дефолтный бакет, который был китайским. Теперь дефолтный
   бакет — английский, и всё, что не поддерживается, резолвится в `en`.
2. **Сервисы читали строки из application context**, который на Android <13 не
   переконфигурируется при смене языка. Читают через `getLocalizedContext`.
3. **Значения, записанные в DataStore при первом запуске**, остаются на языке
   того запуска. Отсюда `默认配置`, `小欧`. При чистой установке чинится само.
4. **Плагин ToolPkg** не получал `useEnglish` в payload → падал в `zh`.

Пятая, найденная уже на устройстве: **имя канала уведомлений не лечится
ресурсом.** Система захватывает его при первом создании навсегда. Лечится
только версионным суффиксом в id канала.

Подтверждено на устройстве SM-A207F при системной локали `ru-RU`:
первый экран английский, CJK = 0 во всём UI и во всех уведомлениях телефона.

**Размер:** 439 → 296 МБ. Основное — STT-модель (141 МБ) вынесена в онлайн,
локали (7 МБ), нелатинские OCR-модели и китайский токенизатор (4.8 МБ).

**Что проверено, а что нет:**

- Debug-сборка компилируется, R8 (`minifyNightlyWithR8`) проходит в CI.
- R8 под минификацией **не прогонялся на устройстве** — нет подписанного APK.
- `check_strings.py` и `check_localizations.py` починены, но по-настоящему
  начнут что-то значить только когда появится вторая локаль.

---


---

## Открытые вопросы 1–2 (SUPERSEDED/CLOSED, история)

## Открытый вопрос 1: дальнейшее урезание дистрибутива (SUPERSEDED 2026-10-03)

Таблица ниже — состояние на 30.09 (APK 296 МБ). Актуально:
`docs/FORK-REGISTRY/research-size.md` + план `docs/TODO/offline-assets-move/`.
OCR-решение ниже устарело частично: нелатинский ML Kit удалён (SIZE-002),
латинский OCR жив (`OCRUtils`), движок+модель остаются.

296 МБ, из них в APK крупнейшие блоки:

| блок | МБ | что это | мой взгляд |
|---|---|---|---|
| dex | 72 | вся логика + 3 HTTP-стека | R8 включён, реальный выигрыш не измерен |
| rootfs Ubuntu | 64 | proot-образ для терминала | кандидат в онлайн, но ломает «работает из коробки» |
| lib | 57 | нативные библиотеки | уже arm64-only, ужать нечем |
| `subpack/` | 37 | `android.apk` + `windows.zip` для фичи экспорта | кандидат в онлайн |
| `packages/` | 28 | из них 27 — `apktool.toolpkg` | кандидат в онлайн |
| **OCR (ML Kit)** | **5.5** | см. ниже | кандидат на удаление целиком |
| assets прочее | 20 | emoji-гифки, MathJax, шрифты | |

**Про OCR — неочевидный вывод, проверь перед решением.** Он состоит из
**весов 1.49 МБ** (модель реально такая маленькая — квантованный MobileNet +
LSTM) и **движка 10 МБ**, который универсальный: я нашёл в нём код для 23
письменностей, включая Devanagari, для которого модель я удалил. Движок
бесполезен без модели, модель бесполезна без движка — **частичного выигрыша
нет**. Либо весь ML Kit (5.5 МБ в APK, 11.3 МБ на устройстве), либо ничего.
Распознавание при этом полностью локальное, облака нет, в фоне не работает.
Четыре потребителя, все по явному действию: вложение-скриншот, плавающее
OCR-окно, инструмент агента «прочитать картинку», скан-PDF.

**Осторожно с `useLegacyPackaging = true`** в `app/build.gradle.kts:517`.
Из-за него `.so` сжаты в APK и распаковываются при установке. APK станет
больше, расход места на устройстве меньше. Это развилка «уменьшить загрузку»
против «уменьшить установку» — выбирай осознанно.

Порядок работы, если браться: сначала прогон
`python3 ci/script/fork_audit.py` (увидишь hotspot-файлы — где форк и апстрим
конфликтуют), потом решай по одному блоку, каждый раз меряя APK заново.

---

## Открытый вопрос 2: замена STT на GigaAM-Multilingual (CLOSED 2026-10-03)

Реализовано: провайдер `GigaAMSpeechProvider`, прямой ORT-рантайм вместо
sherpa (вариант (б) ниже), VAD-чанкер вместо стриминга (фразы, не поток),
модель 225 МБ в on-demand с consent. Остаток — M6-приёмка (см. статус
сверху), не реализация.

Пользователь выбрал `fussraider/GigaAM-Multilingual-sherpa-onnx-ctc`.
Я проверил — выбор правильный, но **это не замена ссылки, а замена движка**,
и вот три вещи, которые сломают «просто поменять»:

**1. Рантайма в приложении нет.** Сейчас в APK `libsherpa-ncnn-jni.so` (5.1 МБ)
и Kotlin API `com.k2fsa.sherpa.ncnn.*`. Модель в формате **sherpa-onnx** —
это другой JNI (`libsherpa-onnx-jni.so`) и другой API
(`com.k2fsa.sherpa.onnx.*`). Оба `.so` приезжают из архива `jniLibs.zip`,
который качает `ci/script/download_android_dependencies.sh` с Google Drive —
то есть **это артефакт апстрима, и добавить туда файл я не могу**. Варианты:
- (а) просить апстрим добавить sherpa-onnx JNI — долго, вне нашего контроля;
- (б) гонять ONNX напрямую через уже присутствующий
  `com.microsoft.onnxruntime:onnxruntime-android:1.17.1`. Образец есть:
  `OnnxSileroVad.kt` уже работает через onnxruntime. CTC с 71-символьным
  словарём → greedy decode тривиален: argmax → схлопывание повторов → выкинуть
  blank. Фичи: 16 кГц, 64 мела, n_fft=320, win=320, hop=160, subsampling=4.
  Это **предпочтительный путь**, он снимает зависимость от апстрима;
- (в) собрать `libsherpa-onnx-jni.so` локально и вендорить в `llm/mnn`-подобный
  модуль, как уже сделано с `libsherpa-mnn-jni.so`.

**2. Модель офлайновая, приложение стриминговое.** Текущий
`SherpaSpeechProvider` — `OnlineModelConfig` с encoder/decoder/joiner,
пушит частичные результаты через `recognitionResultFlow`
(`partialResults = true`). В README экспорта прямо сказано:
*NeMo-CTC **offline** format*. Значит либо
- писать отдельный `OfflineRecognizer.fromTransducer`/`fromCTC`-обвязку и
  менять UI на «запись → распознавание», либо
- мириться с задержкой в конце высказывания: живая расшифровка в чате
  раздаётся по кускам, пользователь привык видеть текст по мере речи.

Это продуктовое решение, **спроси у пользователя** перед реализацией.

**3. Модель больше той, что заменяет.**

| вариант | размер | против текущих 141 МБ |
|---|---|---|
| `model.int8.onnx` (220M, 16 layer, d_model 768) | **225 МБ** | +84 МБ |
| `large/model.int8.onnx` (600M, 24 layer) | **592 МБ** | +451 МБ |
| `model.onnx` fp32 | 885 МБ | не для мобилы |

То есть **качество на русском растёт, а размер дистрибутива тоже растёт.**
Это прямо конфликтует с открытым вопросом 1. Скажи пользователю явно.
Хорошая новость: модель уже в онлайне (я вынес STT в онлайн), так что это
скачивание, а не вес APK. И она **первоклассно работает по-русски**: WER 7.1%
на Common Voice против 9.1% у Whisper large v3, 4.4% на FLEURS против 3.1%
(Whisper тут лучше). Токенизатор символьный, а не sentence-piece — русский не
приклеен сбоку, а в словаре наравне с латиницей: `a-z`, `а-я` + `ё` +
казахские `і ғ қ ң ү ұ һ ә ө`. Всего 71 токен, `<blk>` последний.

Закрепить ревизию, как принято в `SttModelRepository`:
`9f5a77e8975211abe8511693accd3a63ee1e9f43`, файлы `model.int8.onnx` + `tokens.txt`.

**Учти:** `libsherpa-ncnn-jni.so` (5.1 МБ) после перехода станет не нужен —
это отдельный выигрыш, но только если `SherpaMnnSpeechProvider` его не делит
(посмотри `llm/mnn/src/main/jniLibs/arm64-v8a/libsherpa-mnn-jni.so` — это
другая библиотека, она останется).

---


---

## Истории приёмок T1–R8, CTC, deferred/dropped

## Custom CTC checkpoint acceptance (2026-10-03, device R9TN601D6GJ)

- Feature commits: `a5d60d76` (CtcModelConfig core) + `b1312e43` (HF add flow)
  + fixes `9676e3d5` (prefs brace), `dfc4c873` (Hub ?blobs=true + sha256 field).
  CI `37081015984` green, APK ~293 MB reinstalled (uninstall first: every CI
  build signs with a fresh debug key).
- Accepted on device: picker renders, add-dialog resolves
  `i2z1/gigaam-multilingual-ctc-onnx-int8` (failed before the blobs fix with
  "no content digest", which is how the bug was found), full pipeline
  (tokens pin, 225 MB verified download, graph-vocab handshake on the
  phone's ORT), auto-select "In use", delete with fallback to built-in,
  registry back to `[]` with no leftover files.
- Wizard replay cheat-sheet: agreement tap, tour next x3, welcome next, grant
  perms via adb (READ_MEDIA_AUDIO + SYSTEM_ALERT_WINDOW appop + battery
  whitelist matter), Check status, Continue through Incomplete, Standard
  level, Confirm, wait out the 公告 countdown. Keyguard: swipe alone does not
  unlock, use KEYCODE_WAKEUP + `wm dismiss-keyguard`; dreaming re-locks, and
  `settings put secure screensaver_enabled 0` stops it.

## Deferred item decoder (2026-10-03, from /home/uzzzver/operit-fork/screenshots2/)

Screenshots 1-8 reviewed frame by frame. Internal labels T1/U1/D12 exist in
no artifact (registry, HANDOFF, user notes, git log); what follows is
reconstructed from the screenshots, with unknowns marked as unknowns.

- RAW TOOL JSON (fixed `0add89c4`): AI Chat renders a tool call as a row;
  tapping it opens `ToolResultDetailDialog` in
  `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/part/ToolResultDisplay.kt`,
  which showed the result string verbatim. Screenshot
  `Screenshot_20261001-015352_Operit Debug.jpg` shows
  `daily_life:device_status Execution Successful` with a single-line
  `{"success":true,"message":"获取设备状态成功",...}` dump. Fixed by
  `splitEnvelope()` in the new `ToolResultFormat.kt` (same package): the
  message line heads the dialog, the same JSON follows indented, non-JSON
  passes through untouched. Unit-tested (`ToolResultFormatTest`, 4 tests).
- T1 NETWORK CHINESE (CLOSED 2026-10-04, см. T1/T2/T3 разделы ниже): перевод
  дефолтной LLM + кэш по SHA-256 источника + batch-prefetch +
  Google-фалбэк. Префикс `工具执行时发生意外错误` живёт в
  `examples/*.ts|*.js` (рантайм тулов, не код приложения) — покрыт
  переводом failure body, править examples не надо.
- DRAWER OVERLAP (already fixed, not U1): screenshot `..._012728...` shows the
  `Not Running` badge landing on the `Permissions` label in the drawer. Fixed
  earlier: `DrawerContent.kt` (~line 81 comment) replaced the word badge with
  a dot. If the overlap is still visible on a new build, reopen with a fresh
  screenshot; otherwise closed.
- MARKET NO-RETRY (CLOSED 2026-10-04): the top-right search icon was tapped
  on device and opens plugin search ("Search plugins"). Pull-to-refresh
  remains the only list recovery, which matches the empty-state copy — not
  a dead button. The same empty state seen 04.10 with a real
  `SocketTimeoutException` to `static.operit.app:443` (phone-network side,
  transient).
- U1 / D12 (unknown): no referent found in any file, screenshot, or note.
  Do not treat the drawer overlap or the market screen as U1/D12 without
  user confirmation. Ask the user with the screenshot filenames above.
- Announcement 公告 (`..._011954...`): first-run dialog text comes from the
  network (upstream notice), not from app resources; was out of scope,
  TRANSLATED 2026-10-04 (T2: prefetch in `OperitApp.kt` + `TranslatedText`
  in `RemoteAnnouncementDialog.kt`, countdown split). Accepted on device
  twice, fully English.

## Dropped 2026-10-03 (user directive: strike, revisit in manual testing)

- Dead network IconButton: no referent confirmed in code or screenshots
  (Market has pull-to-refresh; search support is wired). Dropped.
- U1 / D12: no referent in any artifact. Dropped. If either resurfaces in
  manual testing, file it with a screenshot filename and screen path.

## T1 runtime translation (2026-10-03, commit aa4234a5, CI 37089771706)

- Decision (user): translate via the default LLM model, cache translations,
  hash detects updates. Applies to all runtime Chinese the app can meet.
- Implemented: `data/translation/CachedTranslator.kt` (SHA-256 key, CJK
  detect, JSON file store capped at 2000 entries, in-memory store for
  tests) + first surface: the tool result dialog message line in
  `ToolResultDisplay.kt` via `EnhancedAIService.translateText` (the
  TRANSLATION functional model, i.e. the default model unless the user set a
  dedicated one). Envelope body stays byte-identical for copy-paste.
- Tests: `CachedTranslatorTest` 6/6 standalone (passthrough, translate-once,
  key stability/uniqueness, CJK detect, file roundtrip).
- Open surfaces (same pattern: `CachedTranslator.containsCjk` gate +
  LaunchedEffect + cached translate): plugin names/descriptions DONE
  (`d54af436`), Market listings NOT done. Tool error prefix lives in
  `examples/*.ts|*.js`, covered by failure-body translation, no action.
- Trap met: a `Write` of `\uXXXX` escapes produced literal CJK chars plus a
  stray U+263A widening the regex range; fixed via python with pure escapes,
  verified by non-ASCII grep. Second trap: constructor param named
  `translate` shadowed the member `fun translate` = infinite recursion;
  renamed to `fetchFresh`.

## T1 acceptance (2026-10-03, CI 37096445114, apk 2026-10-03_04-24-40Z)

- Packages card `楼层限制器` renders as `Floor Limiter` + `Slice the latest N
  context layers, ...` on the phone; cache `files/translation_cache/v2.json`
  holds clean entries, no think blocks.
- Trap log: a `Write` of `\uXXXX` escapes produced literal CJK + stray U+263A
  (fixed via python, verified by non-ASCII grep); ctor param named
  `translate` shadowed member `fun translate` = infinite recursion (renamed to
  `fetchFresh`); `run-as "a; b"` multi-command quoting is unreliable, verify
  state with single commands; `&&` chains stop on failed `cp` when the disk
  is full (an install was silently skipped this way — always check
  `lastUpdateTime` after install); uiautomator regex parsing breaks on
  quotes/newlines in text, use a real XML parser.
- Disk: / at 96%, /tmp/opencode/apk* cleaned; keep an eye on it.

## T1-Market acceptance (2026-10-04, CI 37164035232, apk 2026-10-04_00-08-59Z)

- Commit `ca511255`: Market cards (title+description), detail header title,
  About body via new `TranslatedMarkdown` (same CJK gate + cache, markdown
  renderer kept; code blocks excluded at call site), comment bodies,
  installed `PackageItem`, `MarketManageItemCard`, MCP descriptions.
  `TranslatedText` gained `onTextLayout` passthrough (comment expand/collapse).
  APK 373 MB, size unchanged. `fork_audit` green (0 new CJK).
- Accepted on device (fresh install, wizard replay, 公告 countdown waited):
  Packages `楼层限制器` -> `Floor Limiter` live, screen CJK=0; Market list
  loads over network, English entries byte-intact, no overlap; detail screen
  (header, badges, metrics, About markdown, Comments(22)) renders, no crash;
  `files/translation_cache/v2.json` written (3 KB); logcat has no FATAL.
- Known-accepted, by design: avatar initials stay CJK (single-glyph
  identifiers derived from title, like usernames); badges (type/category/
  version) untranslated; review reasons are resource-based.
- Coverage gap, assessed ~zero risk: Market listings seen were all English,
  so the Market-card CJK branch did not fire live — but it is the identical
  `TranslatedText` proven on Packages cards in the same build.
- Device traps new: wireless debugging died mid-session (port closed, full
  37000-45200 scan empty); fix = re-pair from the phone (user reads
  pairing port + code), `echo CODE | adb pair IP:PAIRPORT`, then rescan for
  the adb port (this time `41599`). `uiautomator dump` started dying with
  SIGKILL (exit 137, 0-byte xml) after many dumps — sleep/wakeup +
  adb reconnect did NOT help; fallback that works: `screencap -p` + visual
  read of the PNG (the agent CAN see screenshots via the Read tool).

## T2 batch acceptance (2026-10-04, CI 37169808187, apk 2026-10-04_02-02-07Z)

- Commit `03d51d5f`: `translateTexts` batch (JSON in/out, ~6000-char chunks),
  `prefetchRuntimeTranslations` triggers (Market page, detail + all comments,
  installed plugins, announcement),   `TranslationPrefetch` marks so batch and
  per-item never duplicate, sync cache peek in TranslatedText/Markdown (no
  Chinese flash on hit), announcement dialog translated (countdown split so
  no request fires per second). Tests 16/16 standalone, `fork_audit` exit 0.
- Accepted on device (fresh install): post-onboarding notice fully English
  ("Announcement ... Got it", batch visible in logcat thinking), Packages
  `楼层限制器` flipped to Floor Limiter with zero taps after the batch
  landed, `v2.json` written (1.2 KB), 0 FATAL in last 300 logcat lines.
- Gap: upstream `static.operit.app:443` timed out from the phone network
  (`SocketTimeoutException`, VM log), so Market listings were empty and the
  list/detail prefetch did not fire live. Same helper as the proven
  Packages/announcement prefetches; live-fire when the CDN is reachable.
- Device traps new: screen sleep freezes screencap (status-bar clock stands
  still — always WAKEUP first and compare md5); wizard bottom-arrow touch
  target is at y~1378, not on the glyph (~1416); location check needs the
  master switch (`settings put secure location_mode 3`), grants alone show
  ✗; `logcat -d` unfiltered hangs the shell — always `logcat -t N`.
- Audit lesson: `fork_audit.py` exit code is the gate, not grep output —
  earlier "green" claims read grep's `$?`. Also fixed while here: two
  LABEL-CONCAT violations (About storage subtitle, custom-STT status line,
  both mine) and two ANCHOR-LOST registry anchors (SIZE-004 now points at
  `CustomSttModels.ensureAcoustic`, SIZE-008 anchor updated to the Location
  enum). CJK-NEW stays 0 — new Chinese prompt text goes through review.
- R8 experiment: `:app:assembleNightly` compiles + minifies fine; the run
  failed only at `signRotatedNightlyApk` (no RELEASE_STORE_FILE in CI =
  known keystore gap). Minified-debug measurement still open, runs after T2.

## T3 fallback acceptance (2026-10-04, CI 37201078764, apk 2026-10-04_12-08-20Z)

- Commit `5a76aa4d` (user explicitly approved the fallback, lifting the
  no-fallback rule for this): `GoogleTranslateFallback` (keyless gtx
  endpoint, JSON sentence parser) + `fetchWithFallback` wiring in single
  and batch paths (primary error rethrown when both fail, nothing masked).
  Tests 19/19 standalone, `fork_audit` exit 0.
- Accepted on device (fresh install): announcement again fully English with
  split countdown, Packages batch flip with zero taps ("Turn Limiter"),
  0 FATAL. Fallback branch did NOT fire (primary healthy) — verified by
  construction: gtx reachable from the phone and returns exactly the parsed
  shape (`[[["test","测试",...]],null,"zh-CN",...]` via phone curl),
  parser unit-tested. Live-fire pending a no-model scenario.

## Shizuku on the test phone (2026-10-04, user suggestion)

- Installed `moe.shizuku.privileged.api` v13.6.0 (APK from
  `rikkaapps/Shizuku` releases via `gh release download`). Dhizuku was
  skipped: it needs device-owner, which fails on a provisioned phone with
  accounts; Operit expects the Shizuku API anyway.
- Started WITHOUT in-app pairing: Shizuku app shows the exact starter under
  "View command" — `adb shell <apk-dir>/lib/arm64/libshizuku.so`, apk dir
  from `pm path moe.shizuku.privileged.api`. Result: "Shizuku is running,
  Version 13.5, adb".
- Authorized Operit: drawer Permissions tile (= ShizukuDemoScreen) ->
  DEBUGGER tab -> "Grant Shizuku Permission" -> system dialog "Allow Operit
  Debug to access Shizuku?" -> "Allow all the time". Status now "Shizuku
  Service Granted", Debugger level "Currently in Use".
- After a reboot Shizuku dies; restart = re-run the same libshizuku.so
  command over adb (no pairing needed). Re-check "Shizuku is running" in
  the Shizuku app afterwards.
- Remaining red on that screen: "Operit Terminal Not Granted" (separate
  terminal-env setup, not Shizuku).

## M6 speech acceptance (2026-10-05, T3 build 12-08-20Z, audiobook via user)

- Dictation: Toolbox -> Speech Recognition, language `ru`, ~31 мин аудиокниги
  через комнату: **497 фраз + 93 blank, 0 `Utterance decode failed`**, движок
  не упал, сессия остановлена кнопкой. Скорость из `files/logs/operit.log`
  (`Phrase (X s audio in Y ms)`): 1046.3 с аудио за 993.2 с стены = **RTF
  1.05**, медиана 948 мс на 1 с аудио, p90 1006 мс. Живую речь держит с
  небольшим запасом.
- Качество: дальнее поле + `Microphone processing = Calls (echo
  cancellation)` — грубовато, но связный русский нарратив (скрин m6_39).
  Без эталонного текста книги WER не посчитать; если качество важно —
  попробовать дефолтную обработку микрофона вместо Calls.
- Поле языка: дропдаун `ru/en` работает; на дефолте `en`, при каждом входе на
  экран сбрасывается в `en` (состояние экрана, не баг).
- Consent: кнопка Download в настройках качает СРАЗУ без диалога; consent
  (`GigaAMConsentDialog`) живёт в toolbox-диктовке — так задумано (консент
  там, где нажали «говорить»).
- SD туда-обратно: internal -> emulated -> **SD-карта** -> emulated ->
  internal. Все копии побайтово (`model.int8.onnx` 224762512, `tokens.txt`
  391). Инит движка С КАРТЫ доказан: `GigaAM initialized` 03:33 при пустом
  internal и без `Model files absent`. Файлы источника удаляются, но ПУСТЫЕ
  папки `stt_model` остаются на старом месте (minor, кандидат в follow-up).
- Ловушка тапов (НОВАЯ): в скролленных списках настроек и дропдаун-меню тапы
  садятся на ~75–100px ВЫШЕ запрошенного (доказано `pointer_location`:
  запрос (182,700) -> точка (182,625)). Дважды промахнулся мимо Download в
  «Add from link», один переезд ушёл не на ту строку. Обход: целиться на
  +80..100 ниже визуала (+компенсация проверена: запрос (182,765) попал в
  Download и СТАРТОВАЛ закачку) либо меню выбирать DPAD_UP/DOWN+CENTER —
  детерминировано (так вернулся на internal). Drawer/табы/кнопки — 1:1,
  не трогать. `pointer_location 1` показывает точку, не забыть выключить.
- Время в сессии (РАЗРЕШЕНО 2026-10-05, подтвердил пользователь): прыжок
  часов 18:1x -> 03:0x — это 8–9 ч паузы на восстановление лимитов модели,
  часы телефона шли ВЕРНО всё время. Тишина в логе 18:43 -> 03:06 — книга
  кончилась, телефон idle. Сама диктовка по меткам лога 18:12:56 ->
  18:43:53 (~31 мин, 497 фраз). Батарея 69 -> 39% за всё время (экран +
  225 МБ + ORT + простой).
- Бонус-находка (не UI, только logcat, pre-existing): `E/AnrMonitor: Main
  thread not responding: 1230ms - 可能发生ANR!` — хардкод-китай в логе
  апстрима. На UI не влияет; кандидат в отдельный follow-up, не M6.

## R8 experiment verdict: REJECTED (2026-10-05, CI 37206856650)

- Эксперимент: `isMinifyEnabled + isShrinkResources` в `debug` (`f37ff8a1`),
  сборка зелёная, APK 373487996 -> 333371794 байт (−40.1 МБ, −10.7%).
- На телефоне минифицированный debug ПАДАЕТ НА СТАРТЕ: `Application Error`,
  `java.lang.NoSuchMethodError: no non-static method
  "Lcom/ai/assistance/operit/core/tools/javascript/
  QuickJsNativeHostDispatcher;.onCall(Ljava/lang/String;Ljava/lang/String;)
  Ljava/lang/String;"` из `QuickJsNativeBridge.nativeCreate` <-
  `QuickJsNativeRuntime$Companion.create (QuickJsNativeRuntime.kt:55)` <-
  `OperitQuickJsEngine.runtime$lambda$0 (OperitQuickJsEngine.kt:29)`. R8
  вычистил/переименовал метод, который зовёт нативный код через JNI;
  keep-правил для QuickJS в `app/proguard-rules.pro` НЕДОСТАТОЧНО
  (комментарий в build.gradle про «уже есть keep rules» — неверен).
- Хуже: `release {}` и `create("nightly")` в том же файле используют ТУ ЖЕ
  минификацию — релизные и nightly-сборки, видимо, сломаны так же (на
  устройстве минифицированное не гонялось НИ РАЗУ). Отдельной задачей:
  дописать keep для QuickJS JNI (класс + сигнатура onCall), затем повторить
  эксперимент; до этого минификацию не включать нигде.
- Revert влит (`ce29250f`, дерево = принятое T3), на телефон возвращён T3
  (скачан заново с CI 37201078764 — папку `apk/2026-10-04_12-08-20Z`
  пользователь удалил с диска; установлен 04:04:38, визард пройден,
  стартует без ошибок). В `apk/` осталась только R8-папка (остальные удалил
  пользователь).

## Size work acceptance (2026-10-03)

- Model to SD: location preset to card before download, 225 MB downloaded to
  `/storage/5982-1724/.../stt_model` (214 MB on disk), engine initialized
  from SD (`GigaAM initialized` in logcat), dictation session started and
  stopped cleanly. Internal `/data/data` has no `stt_model`.
- tflite+mediapipe removal (CI 37127299796, apk 2026-10-03_13-46-05Z):
  APK 386 -> 373 MB, lib 43 -> 41 .so, neither .so in the new APK.
- About storage card on device: Total 472 MB, internal 472 used / 15191
  free, card 0 used / 1028 free (fresh install, model not re-downloaded).
- Trap: kotlinc resolves .java stubs but emits no .class for them; runtime
  needs them compiled with javac (with coroutines on cp for prefs stub).
