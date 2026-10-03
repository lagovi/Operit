# Письмо себе в следующей сессии

**От:** агент, который делал англоязычный форк Operit
**Кому:** мне же, чистый контекст, та же задача
**Дата:** 2026-09-30, коммит `930f2543`, ветка `feat/english-only-build`

---

## С чего начать читать проект, в таком порядке

Не начинай с `AGENTS.md` и не начинай с кода. Порядок выбран так, чтобы к
моменту, когда ты дойдёшь до конца, ты уже знал **почему** всё написано
именно так, иначе будешь чинить не то.

| # | Файл | Зачем именно он |
|---|---|---|
| 1 | **этот файл** | состояние, решения и два открытых вопроса |
| 2 | `docs/FORK-REGISTRY/index.md` | что запускать и что означает каждый код проверки |
| 3 | `docs/FORK-REGISTRY/registry.json` | 21 правка форка, у каждой `rationale` и `anchors` |
| 4 | `docs/FORK-REGISTRY/re-audit-l10n.md` | **исправленный диагноз.** Главный документ, если что-то снова по-китайски |
| 5 | `docs/FORK-REGISTRY/re-audit-prompts.md` | какие промпты видит пользователь, а какие нет |
| 6 | `docs/FORK-REGISTRY/re-audit-layout.md` | почему английский ломает вёрстку и что ещё не починено |
| 7 | `docs/TODO/fork-l10n-runbook.md` | механика CI и устройства. **Диагноз в нём неверен**, помечено в шапке |
| 8 | `docs/TODO/remote_content_translation/index.md` | план про серверный китайский, **не реализован** |

Файлы, которые будешь править, когда дойдёшь до задач:

```
app/src/main/java/com/ai/assistance/operit/
  util/LocaleUtils.kt                     ← язык по умолчанию, здесь всё решение про English-only
  util/RemoteAssetFetcher.kt              ← общий загрузчик: докачка, Range, SHA-256
  data/speech/SttModelRepository.kt       ← описание STT-модели: URL, размер, хеш
  api/speech/SherpaSpeechProvider.kt      ← текущий движок распознавания
  api/speech/SpeechServiceFactory.kt      ← выбор движка
  api/speech/SpeechService.kt             ← контракт, который надо соблюсти
  ui/features/settings/screens/SpeechToTextScreen.kt   ← UI настроек распознавания
app/src/main/res/values/strings.xml      ← единственный строковый бакет, здесь английский
app/build.gradle.kts                      ← localeFilters, R8, зависимости
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

## Открытый вопрос 1: дальнейшее урезание дистрибутива

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

## Открытый вопрос 2: замена STT на GigaAM-Multilingual

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
