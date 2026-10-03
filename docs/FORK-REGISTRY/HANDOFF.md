# Письмо себе в следующей сессии

**От:** агент, который делал англоязычный форк Operit
**Кому:** мне же, чистый контекст, та же задача
**Дата:** 2026-09-30, коммит `930f2543`, ветка `feat/english-only-build`
**Статус на 2026-10-03, коммит `d248fe88` (читать первым, остальное ниже —
история; разделы с пометкой SUPERSEDED устарели, но оставлены как контекст):**

- STT = GigaAM (sherpa удалён полностью, см. M5 `e763e8ef`); движок тюнится,
  кастомные CTC-модели добавляются по HF-ссылке, модель живёт на SD.
- Актуальные числа размера: `docs/FORK-REGISTRY/research-size.md`
  (installed 369 МБ после `useLegacyPackaging=false`, APK 373 МБ после
  удаления tflite/mediapipe). Таблица блоков от 30.09 ниже — SUPERSEDED.
- План работ по размеру: `docs/TODO/offline-assets-move/` (очередь 1:
  rootfs → subpack → apktool на SD-first-run; очередь 2: on-demand при
  появлении хостинга). Релизного хостинга payload до сих пор нет.
- Открытое на следующую сессию (подробности — в конце файла, свежие разделы):
  1. M6-приёмка речи: диктовка через custom-модель с реальным аудио, замер
     скорости декода на телефоне, переезд SD туда-обратно, поле языка, фразы
     аудиокниги. Требует пользователя (ночь/речь), не агента в одиночку.
  2. T1-остаток: Market-листинги (`ui/features/packages/market/`) тем же
     паттерном (`TranslatedText`); префикс ошибок тулов уже покрыт диалогом.
  3. R8-эксперимент на debug (оценка −25–35 МБ dex): один CI-билд с
     минификацией, замер `AppStorageUsage` на телефоне, откат если плохо.
  4. Инфра, не код: workflow `android-tests` падает на setup SDK; нет keystore
     для release/nightly; нет Releases hosting для payload.
  5. Ручное тестирование пользователем: dead button / U1 / D12 вычеркнуты без
     референта — если всплывут, заводить с именем скриншота и путём экрана.
- APK каждой проверенной сборки: `/home/uzzzver/operit-fork/apk/<UTC-дата-старт-CI>/`
  + `CHANGES.md` (разница с предыдущей). Правило уже в `AGENTS.md`.
- Телефон `R9TN601D6GJ` полностью под управлением агента (policy ниже в
  силе); USB нестабилен, adb только по WiFi `192.168.1.63:45687`.
- LAN gateway для тестов (модель, перевод): `http://192.168.1.55:20128/v1`,
  ключ и модель зашиты пресетом ТОЛЬКО в debug (`ModelConfigManager`,
  `BuildConfig.DEBUG`-ветка). В релиз не тащить.

---

## С чего начать читать проект, в таком порядке

Не начинай с `AGENTS.md` и не начинай с кода. Порядок выбран так, чтобы к
моменту, когда ты дойдёшь до конца, ты уже знал **почему** всё написано
именно так, иначе будешь чинить не то.

| # | Файл | Зачем именно он |
|---|---|---|
| 1 | **этот файл** | состояние, решения и два открытых вопроса |
| 2 | `docs/FORK-REGISTRY/index.md` | что запускать и что означает каждый код проверки |
| 3 | `docs/FORK-REGISTRY/registry.json` | ~26 правок форка, у каждой `rationale` и `anchors` (счёт от 30.09 устарел) |
| 4 | `docs/FORK-REGISTRY/re-audit-l10n.md` | **исправленный диагноз.** Главный документ, если что-то снова по-китайски |
| 5 | `docs/FORK-REGISTRY/re-audit-prompts.md` | какие промпты видит пользователь, а какие нет |
| 6 | `docs/FORK-REGISTRY/re-audit-layout.md` | почему английский ломает вёрстку и что ещё не починено |
| 7 | `docs/TODO/fork-l10n-runbook.md` | механика CI и устройства. **Диагноз в нём неверен**, помечено в шапке |
| 8 | `docs/TODO/remote_content_translation/index.md` | план про серверный китайский, **реализован как T1 2026-10-03** (`CachedTranslator` + surfaces; остаток: Market-листинги) |

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
app/build.gradle.kts                      ← localeFilters, useLegacyPackaging=false, зависимости
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
- T1 NETWORK CHINESE (решено пользователем 2026-10-03, реализовано, остаток:
  Market-листинги): перевод дефолтной LLM + кэш по SHA-256 источника
  (`data/translation/CachedTranslator.kt`). Закрыты: диалог результатов
  (message + failure bodies, `ToolResultDisplay.kt`), имена и описания
  плагинов (`TranslatedText`, `PluginTabContent.kt`). Префикс
  `工具执行时发生意外错误` живёт в `examples/*.ts|*.js` (рантайм тулов, не
  код приложения) — покрыт переводом failure body, править examples не надо.
  Остаток: `ui/features/packages/market/` тем же `TranslatedText`.
- DRAWER OVERLAP (already fixed, not U1): screenshot `..._012728...` shows the
  `Not Running` badge landing on the `Permissions` label in the drawer. Fixed
  earlier: `DrawerContent.kt` (~line 81 comment) replaced the word badge with
  a dot. If the overlap is still visible on a new build, reopen with a fresh
  screenshot; otherwise closed.
- MARKET NO-RETRY (suspect for the dead-network-button report): screenshot
  `..._013521...` (Market screen, `UnifiedMarket*` under
  `ui/features/packages/market/`) shows `No scripts or packages available /
  Refresh or try again later` with a connection-failure toast, and the only
  recovery is pull-to-refresh (`PullToRefreshBox` in `MarketBrowseList.kt`
  ~line 262) — no retry button, and the top-right search icon's behaviour on
  this screen is unverified. NOT confirmed as dead; needs a device re-check:
  open Market with network, tap the search icon, confirm it opens search.
- U1 / D12 (unknown): no referent found in any file, screenshot, or note.
  Do not treat the drawer overlap or the market screen as U1/D12 without
  user confirmation. Ask the user with the screenshot filenames above.
- Announcement 公告 (`..._011954...`): first-run dialog text comes from the
  network (upstream notice), not from app resources; out of scope for the
  English-only work unless the user says otherwise.

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
