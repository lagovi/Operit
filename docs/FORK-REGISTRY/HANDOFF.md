# Письмо себе в следующей сессии

> Точка входа: работа начинается с этого файла —
> `docs/FORK-REGISTRY/HANDOFF.md` (корень репозитория `/home/uzzzver/operit-fork/Operit`).
> Прочитать сверху вниз, затем идти по таблице порядка чтения ниже.

**От:** агент, который делал англоязычный форк Operit
**Кому:** мне же, чистый контекст, та же задача
**Дата:** 2026-09-30, коммит `930f2543`, ветка `feat/english-only-build`
**Статус на 2026-10-04, код на `5a76aa4d` (читать первым, остальное ниже —
история; разделы с пометкой SUPERSEDED/CLOSED устарели, но оставлены как
контекст. Хеш — последний кодовый коммит; docs-коммиты сверх него поведение
не меняют и хеш не двигают):**

- STT = GigaAM (sherpa удалён полностью, см. M5 `e763e8ef`); движок тюнится,
  кастомные CTC-модели добавляются по HF-ссылке, модель живёт на SD.
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
- Shizuku v13.6.0 стоит на телефоне, Operit авторизован, уровень Debugger
  активен (рецепт рестарта после ребута — в разделе Shizuku ниже).
- Актуальные числа размера: `docs/FORK-REGISTRY/research-size.md`
  (installed 369 МБ после `useLegacyPackaging=false`, APK 373 МБ после
  удаления tflite/mediapipe). Таблица блоков от 30.09 ниже — SUPERSEDED.
- План работ по размеру: `docs/TODO/offline-assets-move/` (очередь 1:
  rootfs → subpack → apktool на SD-first-run; очередь 2: on-demand при
  появлении хостинга). Релизного хостинга payload до сих пор нет.
- Открытое на следующую сессию:
  1. M6-приёмка речи: DONE 2026-10-05 (раздел M6 ниже). Остаток-ноль.
     Дальше только если пользователь пожалуется на качество дальнего поля.
  2. R8-замер: ОТКЛОНЁН 2026-10-05 (раздел R8 ниже). Минифицированный debug
     падает на старте, revert уже влит (`ce29250f`). Не включать без починки
     keep-правил; отдельной задачей можно починить и перемерить.
  3. Очередь 1 offline-assets-move по `docs/TODO/offline-assets-move/`.
  4. Live-fire при случае (код готов, верифицирован конструкцией, не огнём):
     префетч Market-списка/детайла когда поднимется `static.operit.app`
     (таймаутил с сети телефона 04.10); Google-фалбэк при сценарии без
     модели; R8-сборка на устройстве после п.2.
  5. Инфра, не код: workflow `android-tests` падает на setup SDK; нет
     keystore для release/nightly; нет Releases hosting для payload.
  6. Ручное тестирование пользователем: dead button / U1 / D12 вычеркнуты
     без референта — если всплывут, заводить с именем скриншота и путём
     экрана. Market search icon проверен (открывает поиск) — закрыт.
  7. НОВОЕ 2026-10-04 (очередь пользователя, планы в `docs/TODO/`, не
     реализованы): `llm-io-log` — лог всех обращений к LLM (запрос+ответ)
     в Room с экраном просмотра и фильтром по модели, хук в
     `TokenTrackingAIService.sendMessage`; `supervision-mode` — режим
     надзора (дешёвая модель ведёт чат, умная комментирует через
     `ChatToastHost`), новый `FunctionType.SUPERVISION`, хук на границах
     хода в `MessageProcessingDelegate`. Порядок после R8/Q1 — по решению
     пользователя.
- APK каждой проверенной сборки: `/home/uzzzver/operit-fork/apk/<UTC-дата-старт-CI>/`
  + `CHANGES.md` (разница с предыдущей). Сейчас там 3 папки (окт-03
  удалены): `2026-10-04_00-08-59Z`, `2026-10-04_02-02-07Z`,
  `2026-10-04_12-08-20Z`. Правило уже в `AGENTS.md`.
- Телефон `R9TN601D6GJ` полностью под управлением агента (policy ниже в
  силе); USB мёртв, adb только по WiFi; порт МЕНЯЕТСЯ (был 45687, 41599,
  сейчас `192.168.1.63:36201`), при обрыве — переспаривание (рецепт в
  чит-шите ниже, пользователя просить только порт+код). Пакеты:
  `com.ai.assistance.operit.debug` (Operit), `moe.shizuku.privileged.api`.
- LAN gateway для тестов (модель, перевод): `http://192.168.1.55:20128/v1`,
  ключ и модель зашиты пресетом ТОЛЬКО в debug (`ModelConfigManager`,
  `BuildConfig.DEBUG`-ветка). В релиз не тащить.
- Диск `/` 92% (было 96%) — следить; `/tmp/opencode` чистить после сборок.

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
Ждать: `gh run watch <id> --exit-status --interval 60`. Артефакт:
`gh run download <id> --dir ...`, имя каталога внутри меняется
(`operit-android-31/33/34`) — искать `find -name "*.apk"`. Папка APK =
UTC `createdAt` рана + `CHANGES.md` (3–6 строк дельты).
**Установка.** Каждый CI-билд с новым debug-ключом — сначала `uninstall`,
потом `install`, проверить `dumpsys package ... | grep lastUpdateTime`.
`&&`-цепочки скрывают пропущенный install — проверять время.
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
скриншоты). Экран спит — кадры протухают (часы стоят): WAKEUP первым делом
и сравнивать md5. `run-as` — только одиночные команды (кавычки с `;`
ненадёжны). `logcat` — только `logcat -t N`, полный `-d` вешает shell.
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
- Часы телефона врали (~9.5 ч назад: показывал 17:44 04.10 при реальных
  ~02:2x 05.10), NTP поправил посреди сессии на 03:0x. Длительность сессии —
  по меткам лога (18:12:56 -> 18:43:53+), не по статус-бару. Батарея
  69 -> 39% за сессию (экран + 225 МБ + ORT).
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
