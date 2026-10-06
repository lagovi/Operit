# Письмо себе в следующей сессии

> Точка входа: работа начинается с этого файла —
> `docs/FORK-REGISTRY/HANDOFF.md` (корень репозитория `/home/uzzzver/operit-fork/Operit`).
> Прочитать сверху вниз, затем идти по таблице порядка чтения ниже.

**От:** агент, который делал англоязычный форк Operit
**Кому:** мне же, чистый контекст, та же задача
**Дата:** 2026-10-06, HEAD `21325169`, ветка `feat/english-only-build`
(код `1c0ed81d`: включает `cddb9906` revert + сабмодуль
`terminal@26d4964`; сверх него только docs-коммиты)
**Как читать:** статус ниже — правда на сейчас, читать первым. Разделы
с пометкой SUPERSEDED/CLOSED — история, не трогать. Хеш кодового коммита
сверять с `git log` — docs-коммиты поведение не меняют.

## Статус: что есть

- STT = GigaAM (sherpa удалён полностью, M5 `e763e8ef`); движок принят
  (M6: 497 фраз, RTF 1.05); кастомные CTC — по HF-ссылке. На телефоне модели
  СЕЙЧАС НЕТ (свежая A-установка; для будущих STT-тестов качать 225 МБ
  заново — на SD умеет, доказано в M6).
- Перевод рантайм-китайского готов полностью: T1 (кэш + поверхности),
  T1-Market (`ca511255`), T2 batch-prefetch (`03d51d5f`), T3 Google-фалбэк
  (`5a76aa4d`, одобрен пользователем). Карта слоя: батч
  `ConversationService.translateTexts` + `EnhancedAIService.translateTexts`,
  промпт `FunctionalPrompts.translationBatchUserPrompt`, кэш/парсинг
  `data/translation/CachedTranslator.kt` (+ `TranslationPrefetch.kt`,
  `GoogleTranslateFallback.kt`), UI `TranslatedText.kt` /
  `TranslatedMarkdown.kt` + `prefetchRuntimeTranslations` в
  `ToolResultDisplay.kt`, триггеры в `UnifiedMarketScreen.kt`,
  `UnifiedMarketDetailEntryScreen.kt`, `PluginTabContent.kt`, `OperitApp.kt`,
  диалог `RemoteAnnouncementDialog.kt`. Аватар-инициалы, бейджи, юзернеймы —
  осознанно не переводятся (идентификаторы).
- Терминал РАБОТАЕТ (сломан был во всех сборках форка с `bf48f418`,
  воскрешён 05.10): PM извлекает 41 .so (`useLegacyPackaging=true`,
  `cddb9906`) + спящий фалбэк извлечения из APK в сабмодуле (`26d4964`,
  не срабатывает при `true`, безвреден). Приёмка 15:12 — живой шелл,
  дерево 358 МБ, `.operit_installed_ok`; повторная проверка 06.10 на
  llm-io-сборке — снова живой `~ $`. Сборка:
  `/home/uzzzver/operit-fork/apk/2026-10-05_10-51-45Z/` (CI `37299257049`,
  APK 287 МБ).
- LLM I/O Log ГОТОВ (`1c0ed81d`, CI `37351728353`, приёмка 06.10,
  APK-папка `2026-10-05_17-52-47Z`): таблица `llm_io_log` (миграция
  v21→v22), хук в `TokenTrackingAIService.sendMessage` пишет запрос
  (JSON) + сырой ответ + токены + latency + ошибку; экран в Settings
  после Token Usage Statistics (список + фильтр по модели + диалог
  деталей + Copy/Share + Clear + тумблер); крышка 500 строк / 8 МБ.
  Живое доказательство: 2 TRANSLATION-строки от T2-префетча
  (in=108/out=1235 @ 6387 мс), статистика токенов сошлась (2 запроса).
- Shizuku v13.6.0 стоит на телефоне, Operit авторизован, уровень Debugger
  активен (рецепт рестарта после ребута — в разделе Shizuku ниже).
- Supervision Mode: КОД ГОТОВ (`63dadf6e`, аудит 0), приёмка открыта
  (CI `37443922062` + телефон недоступен — см. задачу 2).
- Размеры честно: APK A-сборки 287 МБ (мерено). Installed для неё НЕ
  перемерен (эпохи до реверта: 435 МБ при `true`, 369 МБ при `false`) —
  перемерить `du` при случае, не гадать. Леждер «что закрыто и почему»:
  `docs/FORK-REGISTRY/research-size.md` (секция 2026-10-05 — читать ПЕРЕД
  любой новой работой по размеру, иначе повторишь закрытые круги).
- План по размеру: `docs/TODO/offline-assets-move/` (Q1 rootfs — ЗАВЕРШЁН
  FAIL; остались subpack/apktool SD-first-run; Q2 on-demand — нужен
  хостинг). Релизного хостинга payload до сих пор нет.

## Статус: что делать (порядок — решение пользователя; зависимость одна:
супервизия пишется В лог, поэтому лог раньше неё; у каждой задачи — DoD)

1. `llm-io-log` — ГОТОВ 2026-10-05 (`1c0ed81d`, CI `37351728353`,
   приёмка на телефоне, APK-папка `2026-10-05_17-52-47Z`). Реализация:
   сущность `LlmIoLogEntity` (`llm_io_log`, миграция v21→v22),
   `LlmIoLogDao` + `LlmIoLogRepository` (крышка 500 строк / 8 МБ тел,
   prune on insert, DataStore-тумблер, `formatRequest` через org.json),
   хук в `TokenTrackingAIService.sendMessage` (общий `runCollectBridge`
   для обоих stream-врапперов; пишет ПОСЛЕ последнего чанка; ошибки и
   cancel тоже пишутся; пробы с `recordTokenUsage=false` не пишутся;
   тела только в Room, в logcat — лишь исход). Метка функции —
   construction-time `functionTag` (`MultiServiceManager` передаёт
   `functionType.name`, ad-hoc — `AD_HOC`, остальное — `CHAT`; доказанная
   точность: инстансы кешируются per-FunctionType). Экран
   `ui/features/llmio/` (список + фильтр + диалог деталей с
   копированием/шарингом + Clear), пункт после Token Usage Statistics.
   Наблюдение приёмки: лог пишет СЫРОЙ ответ с `<think>`-блоками
   (translateTexts режет их ниже по течению для показа, в лог идёт
   сырьё — так и задумано для дебага).
 2. `supervision-mode` — КОД ГОТОВ 2026-10-06 (`63dadf6e`, аудит 0;
   детали и зафиксированный JSONL-формат — в шапке
   `docs/TODO/supervision-mode/index.md`). Что внутри:
   `FunctionType.SUPERVISION` (маппинг/строка FunctionalConfig авто через
   `values()`, + ветка connection-test + имя/описание + 4 строки);
   промпт в `FunctionalPrompts` (EN-only, строгий JSON); новый
   `services/core/SupervisionObserver.kt` (дайджест user+tools+final,
   вызов напрямую `AIService.sendMessage` с `ServiceLease` от
   `acquireServiceForFunction`, `recordTokenUsage=false`, одна обогащённая
   строка в `llm_io_log`, тост только непустого comment, cooldown 30 с);
   хук в `MessageProcessingDelegate` (конец main-flow после finalize,
   имена тулов копятся рядом со счётчиком, cap 50); тост через новый
   колбэк `showToastMessage` (прокинут из `ChatServiceCore` в
   `UiStateDelegate.showToast`); тумблер `SupervisionPreferences`
   (default OFF) + свитч в SettingsScreen; Room v22→v23 (5 nullable колонок
   вердикта); `supervisionJsonl(limit)` API + вердикт в Share-тексте.
   ОТКРЫТО: CI `37447057893` (диспатч 10:02Z, на актуальном коде) + приёмка
   на телефоне (всплывашка + строка `function=SUPERVISION`).
   ЛОВУШКА 06.10: первый диспатч (`37443922062`, зелёный) ушёл ДО push и
   собрал код без фичи (в dex нет SupervisionObserver — проверено) —
   диспатчить только после push. Телефон на 10:0xZ НЕДОСТУПЕН (скан портов
   пуст — отладка выключена, нужен пользователь).
   Известное ограничение v1: в дайджесте имена тулов БЕЗ аргументов
   и результатов (нужны хуки глубже, отдельная задача).
 3. Live-fire при случае (код готов, верифицирован конструкцией, не огнём):
   (a) префетч Market-списка/детайла — когда `static.operit.app` поднимется
   (таймаутил с сети телефона 04.10; проверять с телефона:
   `run-as ... files/usr/bin/busybox wget -O - https://static.operit.app`
   или открыть Market и смотреть пусто/не пусто). Частично доказан
   живьём 06.10: T2-батч для Packages дал 2 строки в `llm_io_log`
   (TRANSLATION) — механика та же, не доказан только CDN-путь;
   (b) Google-фалбэк без модели — снять/не назначать дефолтную модель и
   открыть Packages (осторожно: debug-пресет назначает модель автоматически —
   сначала найти в коде, где её снять: `ModelConfigManager`; если снять
   нельзя без ломания пресета — зафиксировать и отложить).
 4. QuickJS keep-fix (вытек из R8-вердикта ниже). В `app/proguard-rules.pro`
   сейчас есть keep ТОЛЬКО для `JsEngine$JsToolCallInterface` (:47,
   сверено 06.10) —
   для JNI-класса НЕТ НИЧЕГО, вот и `NoSuchMethodError ...onCall`. Добавить:
   `-keep class com.ai.assistance.operit.core.tools.javascript.QuickJsNativeHostDispatcher { *; }`
   (сигнатура жертвы: `onCall(method: String, argsJson: String?): String?`,
   `QuickJsNativeHostDispatcher.kt:21`; зовёт `QuickJsNativeBridge.nativeCreate`
   через `QuickJsNativeRuntime.kt:55` ← `OperitQuickJsEngine.kt:29`).
   Затем повторить мини-эксперимент: `isMinifyEnabled` в `debug`, CI,
   холодный старт на телефоне БЕЗ `Application Error`. До успеха
   минификацию не включать НИГДЕ — `release`/`nightly` делят конфиг и,
   видимо, сломаны так же.
5. Мелкие follow-up, каждый — один маленький коммит:
   (a) пустые `stt_model`-папки после переезда: `AppDataLocation.migrate()`
   (`util/AppDataLocation.kt`) копирует файлы и удаляет их, но source dir
   не трогает — добавить удаление опустевшего source (пустой `File.delete()`
   безопасен: на непустом вернёт false);
   (b) `AnrMonitor` хардкод-китай ТОЛЬКО в logcat: точная строка
   `util/AnrMonitor.kt:208` (`"... - 可能发生ANR!"`) — заменить хвост на
   английский, один коммит, UI не affected;
   (c) дропдаун языка диктовки сбрасывается в `en`: состояние экрана
   `ui/features/toolbox/screens/speechtotext/SpeechToTextScreen.kt:126`
   (`remember`, не персистент) — чинить персистом ТОЛЬКО если пользователь
   пожалуется (сейчас: осознанно не баг).
6. НОВОЕ — ретест Shizuku-терминала: на экране разрешений красное
   `Operit Terminal Not Granted` (terminal-env setup) висело пока терминал
   был мёртв. Терминал ожил — проверить, ушло ли само; если нет — завести
   задачу с путём экрана (`ShizukuDemoScreen.kt`) и текстом ошибки.
7. Инфра, не код: workflow `Android Tests` (файл
   `.github/workflows/android-tests.yml`, `setup-android` + `sdkmanager`) —
   текст ошибки НЕ зафиксирован, перед чинкой воспроизвести через `gh`;
   нет keystore для release/nightly (`signRotatedNightlyApk` ждёт
   `RELEASE_STORE_FILE`); нет Releases-хостинга под Q2 (агент МОЖЕТ создать
   `gh release` сам, но только с добра пользователя — какой репозиторий
   и какие ассеты).
 8. Ручное тестирование пользователем: dead button / U1 / D12 вычеркнуты
   без референта — если всплывут, заводить с именем скриншота и путём
   экрана. Market search icon проверен (открывает поиск) — закрыт.
   Дальнее поле STT — только если пользователь пожалуется (M6 закрыта).
 9. НОВОЕ — разобраться с workspace-чатом (блокирует будущие ручные
   приёмки чата, приоритет низкий, пока хватает функциональных вызовов).
   Факты 06.10: пустое состояние чата + send = тост «Please create a new
   chat»; счётчик «0» создаёт беседу; новая беседа требует workspace
   (Create Default) и дальше показывает ТОЛЬКО WebView-превью без поля
   ввода; таб «Operit» задизейблен; стрелки стрипа, свайпы, FAB-Unbind,
   BACK из превью не выводят; SHARE-intent с текстом уходит в
   аттачменты (`SharedFileHandler`), а не в новый чат. Неизвестно:
   это поведение апстрима или регрессия форка; есть ли выход из
   workspace-режима; можно ли чат вообще без workspace. DoD: ответ на
   эти три вопроса + минимальный рецепт «как вручную отправить сообщение
   в чат» в чит-шит выше (или задача на фикс, если регрессия).

## Статус: окружение

- APK каждой проверенной сборки: `/home/uzzzver/operit-fork/apk/<UTC-дата-старт-CI>/`
  + `CHANGES.md` (3–6 строк дельты). Сейчас там ТРИ папки:
  `2026-10-04_13-47-09Z` (R8-эксперимент, rejected),
  `2026-10-05_10-51-45Z` (A-сборка, терминал) и `2026-10-05_17-52-47Z`
  (llm-io-log, 287387420 байт, CI `37351728353`). Пользователь чистит сам.
  Удалённые докачиваются с CI без пересборки: T3
  `gh run download 37201078764 --dir ...` (внутри
  `operit-android-34/apk/debug/app-debug.apk`, 373487996 байт), T2
  `37169808187`, T1-Market `37164035232` (имена каталогов внутри плывут —
  искать `find -name "*.apk"`; качать с запасом таймаута: 287 МБ ~5+ мин).
  Правило уже в `AGENTS.md`.
- Workflow `Android Build` на push срабатывает ТОЛЬКО для `main`
  (`on.push.branches: [main]`) — для feature-ветки запускать вручную:
  `gh workflow run "Android Build" --ref feat/english-only-build`.
- Приёмочный лабиринт чата 05.10 (не баг фичи, UX онбординга — знать,
  чтобы не терять час): пустое состояние → send тостит «Please create
  a new chat»; счётчик «0» создаёт беседу; новая беседа ТРЕБУЕТ workspace
  (Create Default) и дальше показывает ТОЛЬКО WebView-превью без поля
  ввода; таб «Operit» задизейблен, стрелки/свайпы/FAB-Unbind/BACK из
  превью не выводят. Живой чат для ручной отправки так и не получен —
  строки в лог дали ФУНКЦИОНАЛЬНЫЕ вызовы (T2-префетч при навигации),
  этого хватило для приёмки. Если понадобится ручной чат — искать выход
  из workspace-режима, не повторять круг.
- Телефон `R9TN601D6GJ` полностью под управлением агента (policy ниже в
  силе); USB мёртв, adb только по WiFi; порт МЕНЯЕТСЯ при каждой смерти
  отладки (45687 → 41599 → 36201 → 44657) — НИКОГДА не хардкодить, последнее
  известное значение протухает. При обрыве — переспаривание (рецепт в
  чит-шите, пользователя просить только порт+код). Пакеты:
  `com.ai.assistance.operit.debug` (Operit), `moe.shizuku.privileged.api`.
  СОСТОЯНИЕ 2026-10-06 04:05: жив (`192.168.1.63:44657`), 91%.
  Стоит llm-io-log-сборка (CI `37351728353`, install 21:40, визард
  пройден заново после wipe), терминал жив (`~ $`), llm-io-log принят
  (2 TRANSLATION-строки + диалог). Висит workspace-беседа «New
  Conversati...» (см. лабиринт выше) — для чистых тестов чата удалять
  через Chat History Management.
   Умирал на 1% посреди работы 14:23 — тяжёлые операции (распаковка, копии
   сотен МБ) только с запасом заряда.
   2026-10-06 ~09:35Z: телефон НЕДОСТУПЕН — `adb connect` refused, скан
   `192.168.1.63` `37000-45200`+`5555` пуст = wireless debugging выключена.
   Только пользователь может включить (или дать новый IP).
- LAN gateway для тестов (модель, перевод): `http://192.168.1.55:20128/v1`,
  ключ и модель зашиты пресетом ТОЛЬКО в debug (`ModelConfigManager`,
  `BuildConfig.DEBUG`-ветка). В релиз не тащить.
- Диск `/` 91% — следить; `/tmp/opencode` чистить после сборок
  (временные APK качать в `/tmp/opencode/<имя>`, удалять сразу после install).

---

## Чит-шит устройства и CI (читать перед любой приёмкой)

**adb.** USB мёртв (`lsusb` пуст), только WiFi. Порт меняется сам
(45687 → 41599 → 36201) и отладка гаснет в простое — никогда не хардкодить
порт. Обрыв: попросить у пользователя pairing-порт + код с экрана телефона,
`echo CODE | adb pair 192.168.1.63:PAIRPORT`, затем скан adb-порта
(python+socket, `37000-45200` + `5555`) и `adb connect IP:PORT`. Скан пустой
= отладка выключена, только пользователь может включить.
**CI.** Push в feature-ветку workflow НЕ стартует — только ручной dispatch:
`gh workflow run "Android Build" --ref feat/english-only-build`
(для nightly-минификации добавить `-f gradle_task=":app:assembleNightly"`).
ДИСПАТЧ ТОЛЬКО ПОСЛЕ PUSH: раннер чекаутит origin, локальные коммиты не
видит (06.10: диспатч до push собрал старый код, зелёный CI впустую,
перезапускали — `37443922062` vs `37447057893`).
Ждать: `gh run watch <id> --exit-status --interval 60`. Артефакт:
`gh run download <id> --dir ...`, имя каталога внутри меняется
(`operit-android-31/33/34`) — искать `find -name "*.apk"`. Папка APK =
UTC `createdAt` рана + `CHANGES.md` (3–6 строк дельты).
**Установка.** Каждый CI-билд с новым debug-ключом — сначала `uninstall`,
потом `install`, проверить `dumpsys package ... | grep lastUpdateTime`.
`&&`-цепочки скрывают пропущенный install — проверять время. Streamed
install 373 МБ иногда не стартует с первого раза (пустой dumpsys —
ставить заново с полным выводом, не `tail -1`). Временные APK — только в
`/tmp/opencode/`, удалять сразу после install (диск 91%).
**Визард** (координаты 720x1560): WAKEUP + dismiss-keyguard, `monkey -p
com.ai.assistance.operit.debug -c android.intent.category.LAUNCHER 1`,
согласие (360,1371) после countdown, тур/приветствие next (650,1378),
гранты по adb (см. ниже), Check (360,950), next (650,1378), Incomplete
Continue (~500,905), Standard (100,465 или 360,461), Confirm (360,1206),
ждать countdown объявления. Тач-таргет нижней стрелки — y~1378, НЕ глиф
(~1416). Гранты: `pm grant READ_EXTERNAL_STORAGE + RECORD_AUDIO`,
`appops set SYSTEM_ALERT_WINDOW + MANAGE_EXTERNAL_STORAGE allow`,
`dumpsys deviceidle whitelist +PKG`, `pm grant FINE/COARSE_LOCATION`,
`settings put secure location_mode 3` (иначе Location ✗), `screensaver_enabled 0`.
**Экран.** `uiautomator dump` дохнет с SIGKILL (exit 137, 0-байт xml) —
не чинится. Только `screencap -p` + чтение PNG через Read (агент ВИДИТ
скриншоты). Чёрный кадр = поймал переход анимации, не смерть: подождать
3 с и переснять. Экран спит — кадры протухают (часы стоят): WAKEUP первым
делом и сравнивать md5. `run-as` — только одиночные команды (кавычки с `;`
ненадёжны); НЕ забывать `adb shell` — голый `ls /storage/...` идёт на
ноутбук, а не на телефон (дало ложную «потерю данных» в M6). `logcat` —
только `logcat -t N`, полный `-d` вешает shell. `adb pull` БЕЗ `-q`
(такого флага нет — команда падает). `input text`: пробел — ТОЛЬКО `%s`;
`%20` НЕ раскодируется и уходит литералом в поле ввода.
**Тапы.** В скролленных списках настроек и дропдаун-меню точка касания
садится на ~75–100px ВЫШЕ запрошенного Y (доказано `settings put system
pointer_location 1`: запрос (182,700) дал точку (182,625)). Симптомы: промах
мимо Download в «Add from link», выбор не той строки переезда. Обход:
целиться на +80..100 ниже визуального центра (проверено: запрос (182,765)
попал в Download и стартовал закачку); меню выбирать клавишами
`KEYCODE_DPAD_UP/DOWN + DPAD_CENTER` — детерминировано, без координат
(так вернулся на internal). Drawer/табы/большие кнопки — 1:1, компенсации
не надо. `pointer_location 1` показывает точку касания, после отладки
выключить обратно в 0 (загрязняет скрины и сдвигает вёрстку вниз).
Дополнение 05.10: смещение НЕСТАБИЛЬНО между экранами (drawer +70 вниз,
Setup −70 вверх, Terminal-home ~1:1/+35) — поправку НЕ переносить, на каждом
экране калибровать заново; надёжно покрывает кластер из 3 тапов с шагом 70
через цель. Побочка: тап по ⌨ включает «ADB Keyboard {ON}»-полосу.
Текст кнопок объявлений МЕНЯЕТСЯ между сборками («I see» / «Got it» /
«Understood») — целиться по позиции, не по тексту.
Кнопка Skip в Environment Setup = onBack (терминал НЕ стартует!); сессию
создаёт «+» (рабочая точка 678,195). Сам SetupScreen при входе молча создаёт
сессию `setup-check` — инициализация окружения (ссылки bin) идёт уже от
открытия экрана.
**Не доверяй виду `files/usr/bin`:** applet-ссылки (`ash`, `tar`...) создаются
вслепую и висят даже когда самих бинарников нет — проверять ЦЕЛИ (`ls -la`),
а не имена. Пустой `lib/arm64/` в установке = PM не извлекал .so
(`useLegacyPackaging=false`) = терминал мёртв, смотри леджер ниже.
`ls /data/app/...` — только точный путь из `pm path` (шаблон `~~*` через
adb shell не раскрывается).
`du` по proot-дереву врёт на маунт-стабах: `ubuntu/sdcard` (mode 000) и
`ubuntu/storage/emulated/0` недоступны снаружи — это НОРМА (внутри proot
с `-0` проходятся), не потеря данных и не баг. `cp -a` через них
проскочит с ошибками — проверять размер итога, а не код выхода вслепую.
Таймаут инициализации сессии 30 с ПРОТИВ минутной распаковки: `Session
initialization timeout` убивает pty вместе с недокачанным деревом —
ретраить и ЖДАТЬ, прогресс виден по `proot-distro/` через `run-as`.
Батарея: проверять `dumpsys battery` ПЕРВЫМ делом; яркость в 1
(`screen_brightness 1` + manual mode); тяжёлое (распаковка, копии) только
с запасом — телефон умирал на 1% посреди работы (14:23, `No route to host`).
Каждый CI debug-билд = новый ключ: uninstall → install → `lastUpdateTime` →
визард заново. Визард ПЕРЕЖИВАЕТ ребут/смерть (согласие сохранилось).
**Часы.** Дельтам статус-бара между далёкими командами НЕ верить: сессия
может стоять на паузе 8–9 ч (лимиты модели), часы при этом идут верно —
в M6 прыжок 18:1x -> 03:0x был именно паузой, подтверждено пользователем.
Длительности — ТОЛЬКО по меткам времени в логах (`files/logs/operit.log`
через `run-as cat`); `adb shell date` в начале сессии — для порядка.
Батарея за сессию M6: 69 -> 39%.
**Тесты без Gradle** (SDK нет): `/tmp/opencode/kotlinc/bin/kotlinc` +
`JAVA_HOME=/tmp/opencode/jdk`, cp: `json.jar junit.jar hamcrest.jar
kotlinx-coroutines-core-jvm.jar kotlin-stdlib.jar (+ okhttp.jar okio.jar
для translation-файлов)`, стабы из `/tmp/opencode/stubs`
(`AppLogger.kt`; .java-стабы компилировать javac отдельно — kotlinc не
даёт .class для .java). Запуск: `java -cp <out:jars> org.junit.runner.JUnitCore
<TestClass>`. Тестовые Kotlin-файлы — только ASCII: `grep -nP '[^\x00-\x7F]'`
обязан быть пуст (иначе CJK-NEW в аудите). `\uXXXX` в моих Edit-командах
РАСКРЫВАЮТСЯ в литералы — писать тесты с CJK только через python-конвертер,
в команде не должно быть подстроки backslash+u+hex (строить через
`'\\u%04x' % ord`, проверять grep до и после).
**Аудит** — ворота только `python3 ci/script/fork_audit.py` REAL exit code
(не `$?` grep). CJK-NEW обязан быть 0 (китай даже в KDoc/тестах — на
ревью); конкатенация `stringResource()+...` запрещена (LABEL-CONCAT) —
только формат-ресурсы `%1$s`; якоря registry.json обязаны существовать
(править вместе с кодом: ANCHOR-LOST).

---

## С чего начать читать проект, в таком порядке

Не начинай с `AGENTS.md` и не начинай с кода. Порядок выбран так, чтобы к
моменту, когда ты дойдёшь до конца, ты уже знал **почему** всё написано
именно так, иначе будешь чинить не то.

| # | Файл | Зачем именно он |
|---|---|---|
| 1 | **этот файл** | состояние, решения, чит-шит устройства/CI, открытые пункты |
| 2 | `docs/FORK-REGISTRY/index.md` | что запускать и что означает каждый код проверки |
| 3 | `docs/FORK-REGISTRY/registry.json` | ~26 правок форка, у каждой `rationale` и `anchors` (счёт от 30.09 устарел) |
| 4 | `docs/FORK-REGISTRY/re-audit-l10n.md` | **исправленный диагноз.** Главный документ, если что-то снова по-китайски |
| 5 | `docs/FORK-REGISTRY/re-audit-prompts.md` | какие промпты видит пользователь, а какие нет |
| 6 | `docs/FORK-REGISTRY/re-audit-layout.md` | почему английский ломает вёрстку и что ещё не починено |
| 7 | `docs/TODO/fork-l10n-runbook.md` | механика CI и устройства. **Диагноз в нём неверен**, помечено в шапке |
| 8 | `docs/TODO/remote_content_translation/index.md` | план про серверный китайский, **реализован полностью** (T1 + Market + T2 batch + T3 fallback, включая announcement) |

Файлы, которые будешь править, когда дойдёшь до задач:

> SUPERSEDED 2026-10-03: `SttModelRepository.kt` и `SherpaSpeechProvider.kt`
> удалены (M5 `e763e8ef`). Актуальная карта: движок —
> `api/speech/GigaAMSpeechProvider.kt`, распознаватель —
> `api/speech/GigaAMRecognizer.kt`, конфиг чекпоинта —
> `api/speech/CtcModelConfig.kt`, HF-резолв и реестр —
> `data/speech/HfCtcModel.kt` + `data/speech/CustomSttModels.kt`, перевод —
> `data/translation/CachedTranslator.kt`, места хранения —
> `util/AppDataLocation.kt`, замер места — `util/AppStorageUsage.kt`.

```
app/src/main/java/com/ai/assistance/operit/
  util/LocaleUtils.kt                     ← язык по умолчанию, здесь всё решение про English-only
  util/RemoteAssetFetcher.kt              ← общий загрузчик: докачка, Range, SHA-256
  data/speech/SttModelRepository.kt       ← УДАЛЁН, см. пометку выше
  api/speech/SherpaSpeechProvider.kt      ← УДАЛЁН, см. пометку выше
  api/speech/SpeechServiceFactory.kt      ← выбор движка
  api/speech/SpeechService.kt             ← контракт, который надо соблюсти
  ui/features/settings/screens/SpeechToTextScreen.kt   ← UI настроек распознавания
app/src/main/res/values/strings.xml      ← единственный строковый бакет, здесь английский
app/build.gradle.kts                      ← localeFilters, useLegacyPackaging=true (:517, cddb9906; см. E2 — НЕ флипать вслепую), зависимости
terminal/                                 ← САБМОДУЛЬ, форк lagovi/OperitTerminalCore
```

---

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

## Отрицательные результаты — не повторять (ledger)

Каждый пункт: гипотеза → метод (как проверяли) → улики → вердикт.
Перепроверять только при новых фактах (другой телефон, другой Android,
другая политика SELinux). Полные протоколы — по ссылкам, здесь минимум,
достаточный чтобы НЕ делать заново.

- **E1. R8-минификация debug (FAIL).** Гипотеза: срезать ~40 МБ минификацией.
  Метод: `isMinifyEnabled + isShrinkResources` в `debug` (`f37ff8a1`),
  CI `37206856650` зелёный, APK 373487996 -> 333371794 (−40.1 МБ).
  Улики с телефона: падение на старте `Application Error`,
  `java.lang.NoSuchMethodError: ...QuickJsNativeHostDispatcher;.onCall
  (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;` из
  `QuickJsNativeBridge.nativeCreate` (`QuickJsNativeRuntime.kt:55` ←
  `OperitQuickJsEngine.kt:29`). В `app/proguard-rules.pro` для JNI-класса
  keep НЕТ (есть только для `JsEngine$JsToolCallInterface`).
  Вердикт: REJECTED, revert `ce29250f`. Бонус-улика: `release`/`nightly`
  делят конфиг минификации — видимо, сломаны так же (на устройстве
  минифицированное не гонялось НИ РАЗУ). Следующий шаг — задача 4 выше
  (keep-fix), не «включить и посмотреть».
- **E2. `useLegacyPackaging=false` ради installed (FAIL с пользой).**
  Гипотеза: убрать 152 МБ извлечённых .so. Метод: флип (`bf48f418`),
  CI `37086798853`: APK 293 -> 386 МБ (+93), installed 435 -> 369 МБ (−66).
  Улика отложенная (05.10, Q1): `lib/arm64/` на телефоне ПУСТ,
  `TerminalManager.linkNativeLibs()` ищет .so только в `nativeLibraryDir`
  → ссылок ноль → `execve(.../files/usr/bin/bash) failed: No such file or
  directory`, exit 1, чёрный экран. Ремонт B1 (сабмодуль `26d4964`,
  извлечение 5 .so из APK в `files/usr/bin`, CI `37259656106` зелёный):
  ссылки создались, но `execve bash: Permission denied` — файлы
  `-rwx--x--x` `app_data_file`, `run-as` (домен `runas_app`) исполняет
  (EXIT=0), приложение (`untrusted_app`) — нет; `/data` без `noexec`, avc
  подавлены. Вердикт: Samsung запрещает exec из `files/`; терминальные
  бинарники обязаны приходить из PM-извлечения. Revert `cddb9906`
  (CI `37299257049`, APK 287 МБ), приёмка 15:12 — живой шелл. B1-коммит
  оставлен в сабмодуле как спящий фалбэк. Следствие для размера: нативные
  библиотеки неприкосновенны, пока терминал exec'ит пребилды; любой новый
  `false`-флип сначала доказывает exec на этом Samsung.
- **E3. Rootfs на внешнее хранилище (FAIL, обе локации).** Гипотеза: дерево
  358 МБ жить на SD. Метод: `busybox cp -a` (uid приложения, `run-as`) из
  `files/usr/var/lib/proot-distro/installed-rootfs/ubuntu` в EXTERNAL_APP
  (`/storage/emulated/0/...`, FUSE), затем `ln -s`-проба на SDCARD_APP
  (`/storage/5982-1724/...`, карта 980 МБ). Улики: сотни `can't create
  symlink ... Operation not permitted` (`etc/ssl/certs`, `etc/alternatives`,
  merged-/usr `bin/lib/sbin`, `dev/std*`); на SD `ln: Permission denied`
  сразу, каталог пуст. Обход `-L` отвергнут расчётом: ~700 МБ дублей (не
  влезет) + висячие ссылки (`dev/std*`, `mtab`) всё равно мёртвы. Вердикт:
  rootfs остаётся internal; SD-механика — только чистым данным
  (subpack/apktool). Мусор (`q1_ubuntu_ext`, `qtest`) удалён с телефона.
- **E4. `uiautomator dump` (FAIL, инструмент).** Метод: десятки вызовов —
  стабильно SIGKILL, exit 137, 0-байт xml; не лечится сном/реконнектом.
  Вердикт: только `screencap -p` + чтение PNG (агент видит скрины).
- **E5. Dhizuku (SKIP).** Требует device-owner, падает на телефоне с
  аккаунтами; Operit ждёт Shizuku API anyway. Взят Shizuku 13.6.0
  напрямую, рецепт в разделе ниже.
- **E6. Чужие архивы (WONTFIX).** `jniLibs.zip`/`subpack.zip` лежат на
  Google Drive апстрима (`prepare_android_dependencies.py`) — добавить туда
  файл нельзя; Q2-хостинг нужен свой. `sherpa-onnx` JNI оттуда же
  недоступен — потому GigaAM гоняется напрямую через ORT, а не через
  sherpa (вариант (б), M5).

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

**Осторожно с `useLegacyPackaging = true`** в `app/build.gradle.kts:508`.
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

## Правила, которые я соблюдал и которые ломают сборку

- `app/src/main/res/values/strings.xml` — **единственный** строковый бакет, и
  он на английском. Квалифицированных локалей нет вообще. Ключ, добавленный
  в `values-en/`, даст ссылку на несуществующий ресурс.
- `stringResource` **нельзя** в `try/catch`, в лямбде `LazyListScope`, и
  **нельзя передавать `Any?`** — принимает `Any`. Nullable разрешай на месте
  через `?:`. Словил это уже дважды.
- Метки собираются **формат-ресурсами**, не конкатенацией. Обе формы
  (`stringResource(X) + ":"` и `"${stringResource(X)}: $v"`) проверяются
  гейтом `LABEL-CONCAT`.
- `localeFilters` живёт на `androidResources`, **не** на `defaultConfig` и не
  на buildType. Я потратил на это два CI-цикла.
- Проект запрещает fallback-логику. `runCatching {}` с молчаливым `""` в
  порядке не считать «обработкой ошибки» — это ровно тот класс, что прячет
  баги.
- Правки комментариев — на английском, даже если соседние на китайском.
- `out/` в `.gitignore`, не коммить APK.

## Чего не делать

- Не запускай `check_strings.py` в ожидании «英文: 完整» — такого ярлыка
  больше нет, источник называется `default`.
- Не верь `count_ui_strings.py`: он не обходит модуль `:terminal`, из-за чего
  65 строк там прожили незамеченными. Замена — проверка `CJK-NEW` в аудите.
- Не форкай `terminal` submodule обратно на апстрим. Он уже на
  `lagovi/OperitTerminalCore`; внутри есть remote `upstream` для мержей.
- Не коммить, пока `python3 ci/script/fork_audit.py` не зелёный.

---

## Test phone policy (2026-10-02, user directive)

Device `R9TN601D6GJ` (adb) is a dedicated test phone, fully managed by the
LLM agent. Anything may be done with it without asking the user first:
uninstall (wiping data), install, change settings, download models, run
acceptance. Do not ask for confirmation for device operations on this phone.

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
