# Письмо себе в следующей сессии

> Точка входа: работа начинается с этого файла —
> `docs/FORK-REGISTRY/HANDOFF.md` (корень репозитория `/home/uzzzver/operit-fork/Operit`).
> Прочитать сверху вниз, затем идти по таблице порядка чтения ниже.

**От:** агент, который делал англоязычный форк Operit
**Кому:** мне же, чистый контекст, та же задача
**Дата:** 2026-10-08 (ночь, большая ревизия HANDOFF), HEAD `06d7327d`,
ветка `feat/english-only-build`
(код: `1c0ed81d` llm-io-log + `63dadf6e` supervision + фиксы +
`b4ff4a30` QuickJS keep + `c8bda3c4`/`acc9eb3a` 5a/5b + `50dc014a`
R8-mapping + `f3ce47f1`/`5cb753fe` задача 10 + `cddb9906` revert +
`97ad4913`/`a112091f`/`55e18396` тесты (Android Tests зелёный) +
`a4fddb40` subpack Q1 + сабмодуль `terminal@26d4964`;
сверх кода только docs-коммиты).
Что изменила ревизия 08.10: шапка/пули/телефон приведены к правде
(снято противоречие «задача 11 открыта и закрыта одновременно»);
заведены задачи 12 (Q1 пп.3–4: apktool/desktop) и 13 (Q2 on-demand)
с нуля до cold clarity; уточнены 6 и 8; в чит-шит внесены рецепт грантов
через shell, КОНФЛИКТ направлений дрейфа тапов (M6 против 08.10),
ловушка `unzip -l`, рецепт стектрейсов из HTML-отчётов, состояние fresh
install; леджер research-size п.3 и таблица обновлены (subpack-механика
готова, R8 — done, дрейф размера android.apk на Drive).
ОТКРЫТО: 6 (terminal-env setup, нужны батарея+сеть), 8 (совместное ручное,
включая E2E экспорта в leaf), 12 ЧАСТИЧНО ((a) apktool удалён И замерен 08.10 — ЗАКРЫТА;
(b) desktop удалён И замерен 08.10 — ЗАКРЫТА; живые helper APK,
templates/emoji — в Q2 с картой читателей),
 13 ЧАСТИЧНО (subpack ГОТОВ: код+релиз+замер 08.10; LIVE-FIRE 09.10:
 закачка ДОКАЗАНА size+sha, репак УПАЛ R8-крашем — фикс `b54dffbe`
 запушен, CI УПЁРСЯ: Actions disabled на форке, нужен владелец).
ЗАКРЫТО: всё остальное (1–5a/5b, 7a, 9, 10, 11, 3a/3b, 4,
subpack Q1-механика).
 Решения пользователя 08.10: keystore НЕ делаем (жизнь на гитхабе,
 debug-сборок достаточно); хостинг Q2 — GitHub Releases в том же репо,
 первый релиз вместе с Q2-кодом; ручное тестирование — позже, когда
 всё отшлифовано. Телефон заряжен (08.10 утром 81%), сборка subpack
 (CI `37709173132`) установлена 04:13Z чисто, визард пройден, чат пуст.
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
- Supervision Mode ПРИНЯТ 06.10 (`63dadf6e` + фиксы, CI `37467783125`,
  APK-папка `2026-10-06_13-03-03Z`): тумблер + карточка в FunctionalConfig
  глазами, 7 живых SUPERVISION-строк (6×ok), 3 комментария ушли в showToast.
  Тост ПОЙМАН живьём 08.10 (задача 11: белый кард + id=8, текст совпал) —
  остаточного риска больше нет. Известное ограничение v1: в дайджесте имена
  тулов БЕЗ аргументов и результатов (нужны хуки глубже, отдельная задача —
  не заведена; приоритет осознанно ниже размера: сначала закрыть 12 и 13).
- Subpack Q1-механика ГОТОВА 08.10 (коммит `a4fddb40`, CI `37709173132`
  success, APK-папка `2026-10-08_01-06-04Z` + `CHANGES.md`, 247299182 Б):
  новый `core/subpack/SubpackStorage.kt` (leaf `subpack`, stage из assets
  с overwrite, `migrateTo`), стейджинг `ApkEditor.fromAsset`/
  `ExeEditor.fromAsset` переведён с cacheDir на leaf (фалбэк cacheDir
  сохранён), в Settings → Data & Permissions добавлен
  `SubpackStorageSection` (тот же move-механизм, что у STT-модели;
  5 строк в default-бакете, аудит 0). Приёмка: строка рендерится с путём
  leaf; переезд internal → external → memory card без ошибок, каталог
  `/storage/5982-1724/.../files/subpack/` создан; шаблоны в APK этой
  сборки на месте (`assets/subpack/android.apk` 48 МБ — ДРЕЙФ против
  24.2 МБ в реестре, Drive-архив вырос; `windows.zip` 11 МБ). НЕ покрыто:
  файлы в leaf после реального экспорта (нужен web-контент + flow подписи
  — отложено в задачу 8, редирект меняет только каталог). Временный
  windows-экспорт (`cacheDir/windows_export_temp`) осознанно не тронут.
- Размеры честно: минифицированный debug 08.10 — 247299182 Б
  (CI `37709173132`; дельта +10640 Б к задаче 10 — subpack-код почти
  ничего не весит). R8-mapping каждого зелёного CI — в артефакте
  `operit-android-mapping-<run>`. Installed для текущих сборок НЕ перемерен
  (эпохи до реверта: 435 МБ при `true`, 369 МБ при `false` — см.
  методологию в E2, цифры только по package dir, БЕЗ /data/data!). Перед
  любой цитатой installed — рецепт из E2 (ОБА дерева), не гадать. Леждер
  «что закрыто и почему»: `docs/FORK-REGISTRY/research-size.md` (леджер
  2026-10-05 + обновление 08.10 — читать ПЕРЕД любой новой работой
  по размеру, иначе повторишь закрытые круги).
  ЧТО ОТКРЫТО (не лезть в закрытое выше): Q2 on-demand — задача 13
  (хостинг РЕШЁН: GitHub Releases в том же репо; кандидаты: subpack
  59.5 МБ, helper APK shizuku/accessibility 5.4 МБ, templates 9.8 МБ,
  emoji 3.6 МБ — карта читателей в задаче 12(b)). Детали:
  `docs/TODO/offline-assets-move/2_WorkQueue.md` (Q1: п.1 rootfs FAIL,
  п.2 subpack МЕХАНИКА ГОТОВА, п.3 apktool УДАЛЁН+ЗАМЕРЕН,
  п.4 desktop УДАЛЁН+ЗАМЕРЕН; очередь 2 — пп.5–7).
  затем Q2 on-demand — задача 13 (хостинг РЕШЁН: GitHub Releases в том
  же репо). Детали: `docs/TODO/offline-assets-move/2_WorkQueue.md`
  (Q1: п.1 rootfs FAIL, п.2 subpack МЕХАНИКА ГОТОВА, п.3 apktool УДАЛЁН,
  п.4 OPEN; очередь 2 — пп.5–7).

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
 2. `supervision-mode` — ПРИНЯТ 2026-10-06 (код `63dadf6e` + фиксы
   `21e6e986`/`25540fdf`, аудит 0, CI `37467783125` зелёный, APK-папка
   `2026-10-06_13-03-03Z` + `CHANGES.md`). Приёмка на телефоне (сборка
   16:51Z, визард, тумблер ВКЛ): карточка Supervision Observer в
   FunctionalConfig (бинд Default) + тумблер — глазами; 7 живых
   SUPERVISION-строк в `llm_io_log` (1×parse_error до think-fix — JSON был
   внутри `<think>`, затем 6×ok после `stripThinkBlocks`); 3 комментария
   прошли через `showToastMessage`→`UiStateDelegate.showToast` (строки
   79/83/85: вердикты ok + тексты). ОСТАТОЧНЫЙ РИСК вынесен в задачу 11
   ниже (там метод ловли и DoD) — рендер
   через stock `ChatToastHost` (смонтирован безусловно,
    `AIChatScreen.kt:1459`), инстансы ядер сверены (один MAIN core,
   один UiStateDelegate), но пиксельного пруфа нет. Детали и JSONL-формат —
   в шапке `docs/TODO/supervision-mode/index.md`.
   Уроки приёмки (не повторять): (а) БД тянуть ВМЕСТЕ с -wal/-shm, иначе
   свежие строки не видны (ложный «хук не пишет»); (б) flash-lite-драйвер
   на тул-ходах зацикливается (list_files/use_package/terminal/code_runner,
   счётчик 39) и жрёт API — для приёмки только тривиальные безтуловые
   ходы + Deny/X при зацикливании (отменённые ходы не судятся —
   по дизайну); (в) permission-диалоги требуют ручных Allow (Standard).
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
    История приёмки 06.10 (сжато): диспатч до push собрал код без фичи
    (`37443922062` впустую); `37447057893` поймал 3 ошибки компиляции
    (починены в `21e6e986`/`25540fdf`); установка think-fix-сборки зависла
    на streamed install + телефон выпадал из сети; со второго захода
    (порт 38571) всё прошло. Рецепт живого чата — в окружении выше
    («Рецепт живого чата»).
    Известное ограничение v1: в дайджесте имена тулов БЕЗ аргументов
    и результатов (нужны хуки глубже, отдельная задача).
   3. Live-fire при случае (код готов, верифицирован конструкцией, не огнём):
    (a) ПРЕФЕТЧ ДОКАЗАН ЖИВЬЁМ 07.10 на supervision-сборке: открыт Packages →
    карточка `楼层限制器` сырым китайским → через ~12 с сама флипнулась в
    "Message Limiter" (ноль тапов); в `llm_io_log` строка id=93 TRANSLATION
    in=108/out=1062 @ 5133 мс с батчем `{"1":"楼层限制器","2":"截取最近..."}`.
    CDN-ЛИСТИНГИ по-прежнему blocked (сервер): Market (розовая иконка-магазин
    в Packages → диалог Agreement countdown → скролл до конца → кластер по
    "I have read and agree") открылся, но список пуст после спиннера;
    в logcat `UnifiedMarketBrowseVM: SocketTimeoutException ... static.operit
    .app/188.114.97.0:443 after 15000ms` — при этом busybox-wget с телефона
    до того же хоста дал HTTP 404 (резолв 188.114.96.3): разные edge-ноды,
    одна отвечает, другая нет. Вывод: чинить в коде нечего, ждать сторону
    сервера; CDN-путь использует тот же доказанный хелпер;
   (b) Google-фалбэк без модели — ДОКАЗАН ЖИВЬЁМ 07.10 (ветка впервые
    стрельнула в проде). Метод (вариант 2, без ломания пресета и без UI):
    конфиг DataStore — JSON внутри `model_configs.preferences_pb`
    (байты endpoint лежат открыто); бэкап `model_configs.pb` →
    same-length замена endpoint (45→45 байт, protobuf-цел:
    `http://192.168.1.55:20128/...` → `http://127.0.0.1:99999999/...`,
    невалидный порт = мгновенный отказ) через
    `run-as ... tee files/datastore/model_configs.preferences_pb` →
    force-stop → Packages → китайская карточка → возврат бэкапа +
    force-stop + проверка байтов. Побочно выяснено: вариант 1 (удалить
    Default Config в UI) НЕВОЗМОЖЕН — конфиг один, кнопки удаления нет;
    вариант с тапом в поле endpoint тоже мёртв — Compose-поле не
    фокусируется тапами (ADB IME активна, клавиатуры нет, `input text`
    некуда). Доказательства: `llm_io_log` id=4,5,6 TRANSLATION, ошибка
    `Invalid URL port: "99999999"`, ~31 с (5 ретраев); карточка
    `楼层限制器` при этом флипнулась в свежий "floor limiter" (НЕ кэш —
    `translation_cache/v2.json` текста не содержит; НЕ primary —
    утренний primary-вариант был "Message Limiter"). Значит, рендер пришёл
    из `GoogleTranslateFallback`. DoD закрыт: primary-ошибка + успех
    фалбэка + скрин (`/tmp/opencode/3b/s14_after.png`). Телефон возвращён
    на живой пресет (байты сверены), чат-экран после рестарта на месте.
  4. QuickJS keep-fix — ЭКСПЕРИМЕНТ УДАЛСЯ 07.10 (коммит `b4ff4a30`,
    CI `37551624980` success 25m17s, APK-папка `2026-10-07_00-22-30Z` +
    `CHANGES.md`). Найдено: жертва живёт в модуле `:quickjs`
    (`quickjs/.../javascript/QuickJsNativeHostDispatcher.kt:21`, НЕ в app);
    механизм — `quickjs_jni.cpp:296` `GetMethodID "onCall"` по имени, R8
    слеп (E1 именно так и падал). Правило добавлено в `proguard-rules.pro`
    рядом с JsToolCallInterface + поправлен вводящий в заблуждение коммент
    в `build.gradle.kts` (тот самый «уже есть keep rules»). Замеры:
    APK 287428400 → 247288710 (−40.1 МБ, −14%, как в E1 до байта класса
    эффекта); dex 5 файлов (было 43), `onCall`×154 + имя класса×14 в пуле
    строк; холодный старт чист (визард на английском, в logcat ни
    NoSuchMethodError, ни FATAL).     Побочный выигрыш: release/nightly УЖЕ
    были с minify=true и, видимо, падали так же — keep чинит и их (на
    устройстве минифицированное до этого не гонялось НИ РАЗУ).
    РЕШЕНИЕ 07.10 (пользователь: «реализуй лучший вариант»): minify в debug
    ОСТАЁТСЯ навсегда. Доведено до ума тремя вещами: (а) mapping.txt грузится
    в артефакты CI (`50dc014a`, отдельный `operit-android-mapping-<run>`,
    проверен живьём на CI `37571249649`: 12503593 Б, retrace-рабочий —
    в нём видно, что класс диспетчера kept, а лямбды обфусцированы);
    (б) аудит `getIdentifier` по всему app: все 5 мест — системный
    `status_bar_height/dimen/android` (шейкеру ресурсов недоступен, риск
    shrinkResources НУЛЕВОЙ в текущем коде); (в) доказательство полноты
    keep-fix: `GetMethodID "onCall"` резолвится в КОНСТРУКТОРЕ `QuickJsVm`
    (`quickjs_jni.cpp:296`, т.е. в `nativeCreate` при старте движка) —
    каждый чистый холодный старт исполняет ровно тот вызов, на котором
    падал E1. Tool Test Center для live-fire JS→host не подошёл (тестирует
    хост-тулы, не JS-движок) — и не нужен: резолв доказан, инвокейшн-путь
    кодом не менялся. EXPERIMENT-коммент в build.gradle при следующей
    правке этого места заменить на постоянный.
 5. Мелкие follow-up — СДЕЛАНО 07.10, принято на финальной сборке
    (CI `37555506347`, APK-папка `2026-10-07_01-07-20Z`):
    визард пройден полностью до живого чата; PONG. и ACK. отвечены;
    TRANSLATION-строка id=1 (announcement-батч на свежей установке);
    Supervision включён тумблером + строка id=6 verdict=ok; терминал жив
    (41 .so, `bash -c echo` → TERM_OK); в logcat ни NoSuchMethodError,
    ни FATAL. Поведенческий пруф 5a (переезд на SD) и 5b (хвосты ANR)
    не триггерился — принято конструкцией + аудит 0. Детали приёмки
    (калибровки тапов, STOP-ловушка) — в чит-шите и E10 ниже.
    Что сделано (по одному коммиту):
    (a) пустые `stt_model`-папки после переезда: в `migrate()`
    (`util/AppDataLocation.kt`, после `present.forEach { it.delete() }`) —
    удаление опустевшего source dir (пустой `delete()` безопасен);
    (b) `AnrMonitor` хардкод-китай в logcat: `:208` → "possible ANR!",
    заодно `:219` → "warning" (тот же монитор, тот же китай);
    (c) дропдаун языка диктовки (`SpeechToTextScreen.kt:126`) — БЕЗ ИЗМЕНЕНИЙ,
    чинить персистом ТОЛЬКО если пользователь пожалуется.
  6. Ретест Shizuku-терминала — ПРОВЕРЕН 07.10, КРАСНОЕ ОСТАЛОСЬ
    (ОТКРЫТА, приоритет низкий — на красное ничто не влияет: шелл жив,
    Shizuku авторизован; делать только с запасом батареи И сети).
    DEBUGGER-таб (`ShizukuDemoScreen.kt`,
    drawer → плитка Permissions → таб DEBUGGER): Storage/Overlay/Battery/
    Location — Granted, **Operit Terminal — Not Granted (красным)**.
    Сам терминал при этом жив — перепроверен на финальной сборке
    (CI `37555506347`: PM извлёк 41 .so, `files/usr/bin/bash` на месте,
    `adb exec-out run-as ... bash -c 'echo TERM_OK'` → TERM_OK).
    Вывод: красное — про незавершённый terminal-env setup, не про Shizuku и
    не про шелл. Что делать (по порядку, стоп на первом гаснущем):
    (а) Toolbox → Command Terminal → Start Setup, дождаться конца
    (это КАЧАЕТ окружения — только с запасом батареи/сети; кнопка Skip =
    onBack, сессию НЕ создаёт — ловушка в чит-шите); (б) если красное
    осталось — в том же DEBUGGER-табе сверить остальные строки глазами
    (скрин) и копать флаг setup-complete в коде (`ShizukuDemoScreen.kt`
    + кто выставляет terminal-granted), не Shizuku. DoD: строка зелёная
    ИЛИ точное имя флага/условия, которое её держит, записанное сюда.
    Старый скрин статуса (s20_debug2.png, /tmp 07.10) протух — не искать.
  7. Инфра, по пунктам (код не трогать, пока не закрыт 7a):
    (a) `Android Tests` падает на setup — ДИАГНОЗ ГОТОВ 07.10, чинить:
    ран `37582044964` (ручной диспатч), шаг `Set up Android SDK`:
    `Warning: Failed to find package 'tools'` → `sdkmanager failed with
    exit code 1`. Причина известна: экшен `android-actions/setup-android@v4`
    по дефолту ставит пакеты `"tools platform-tools"`, а пакет `tools`
    удалён из SDK-репозитория. ФИКС УЖЕ ЕСТЬ В СОСЕДНЕМ ФАЙЛЕ:
    `android-build.yml:104-111` переопределяет `packages: platform-tools`
    с комментом-объяснением — скопировать тот же `with:` в
    `android-tests.yml:88-90`, задиспатчить `Android Tests`, проверить
    зелёный. РЕЗУЛЬТАТ 07.10: setup зелёный с первого рана (ран
    `37638827764`) — фикс верен. Но workflow валится ДАЛЬШЕ на
    `compileDebugUnitTestKotlin`: гниль апстрим-тестов, чинится по одному:
    (1) `McpConfigImportParserTest` — НАША вина (форк-i18n `610d9f0b`
    добавил парсеру `context: Context`, тест звал старый `parse(config)`;
    починено `97ad4913` — stub `mock<Context>` с `getString→""`,
    mockito-kotlin — штатный паттерн тестов); (2) `Deepseek...Test:244`
    (зовёт protected `createRequestBody` — апстрим закрыл в `2ef5a013`;
    починено exposed-сабклассом) + `XaiProviderReasoningTest` (ссылает
    удалённые `XaiReasoningMapper/xaiModelSupportsReasoningEffort` —
    3 мёртвых метода вырезаны, валидный тест дефолтов оставлен);
    `a112091f`, ран `37678790496`. Тот ран скомпилировался, но уронил
    6 тестов из 1310 — все шесть разобраны по стектрейсам из артефакта
    `operit-android-test-reports-9` (HTML `<pre>`, качать
    `gh run download <run> -n operit-android-test-reports-9`):
    (i) ТРИ — одна болезнь «Method X in android.util.Log not mocked»:
    ColdStream (наш `persistIoLog:549` → `AppLogger.e` маскирует
    CancellationException), CachedTranslator-наш (`parseResponse:43`),
    Xai-defaults (`getModelsListUrl:63` → `AppLogger.d`) — лечение везде
    тест-стороной штатным паттерном (как `OpenAiToolCallHistoryTest`:
    `@Before disable both flags / @After restore`), прод-код НЕ трогать
    (в проде Log.e работает, семантика cancel-пути верна);
    (ii) FunctionType — НАШЕ (`63dadf6e` добавил 12-й SUPERVISION, тест
    ждал 11) — обновить счётчик+список тест-стороной;
    (iii) ReleasedDecoder — upstream-тест документирует НЕреализованное
    намерение (`1_migration_paths.md`: legacy без провайдера скипать):
    `decode("FILE_BINDING")` давал provider FILE — прод-фикс
    `require(known != null)` (прямых звателей `decode(` кроме
    `decodeOrNull` нет; миграция `ApiPreferences.kt:513` скипает null
    штатно с варнингом); (iv) JsToolPkg:62 — тест ждал старый стиль
    `ToolPkg.registerX`, мост ушёл на method-table
    (`registerX: toolPkgApi.method().since(`) — ассёрты за реальностью.
    Коммит `55e18396`, ран `37682838742` — ЗЕЛЁНЫЙ 07.10 (success).
    7a ЗАКРЫТА: setup починен, сюита выполняется (1310 тестов, 0 падений).
    Если сюита покраснеет снова — правило то же: каждый новый `e: file`
    в `--log-failed` → `git log` файла: наше → чинить код, апстрим →
    чинить минимально тест, НЕ продакшн.
    (b) Keystore — РЕШЕНО 08.10, НЕ НУЖЕН: публикации в магазине не будет,
    жизнь — на гитхабе, туда встают и обычные debug-сборки (ставятся
    вручную, для гитхаба этого достаточно). Пользователь разрешил сделать
    ключ, но и разрешил не делать — не делаем (лишняя сущность без
    пользы). Если позже захочется подписанных релизов — завести заново.
    (c) Хостинг Q2 — РЕШЕНО 08.10: тот же репозиторий, GitHub Releases.
    Практика: релиз на тег версии (`vX.Y.Z`), ассеты — по одному файлу
    на payload (`subpack-android.apk`, `subpack-windows.zip`,
    `apktool.toolpkg`, ...), к релизу — короткая записка что внутри.
    Создавать релиз заранее без файлов бессмысленно — первый релиз
    появится вместе с готовым Q2-кодом скачивания.
  8. Совместное ручное тестирование с пользователем (ОТКРЫТА, ждёт
    пользователя — начать только когда он скажет; агентская часть:
    держать этот список полным и свежим). Что уже вычеркнуто без
    референта (если всплывёт — заводить с именем скриншота и путём экрана):
    dead button / U1 / D12; Market search icon проверен (открывает поиск).
    Дальнее поле STT — только если пользователь пожалуется (M6 закрыта).
    Отложенное агентское E2E (нужен живой палец/глаз или долгие flow,
    код готов и принят частично):
    (а) файлы в subpack-leaf после РЕАЛЬНОГО экспорта: открыть чат с
    web-контентом → ExportPlatformDialog → Android/Windows export до конца
    → проверить `files/subpack/apk_editor_android.apk` /
    `exe_editor_windows.zip` в ТЕКУЩЕМ leaf (internal или SD — где стоит
    пикер) через `run-as ls`; (б) поведение 5a/5b (переезд STT на SD
    глазами + ANR-хвосты в logcat) — принято конструкцией, живой пруф
    не триггерился; (в) задача 6, если красное уцелеет после setup.
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

## Статус: окружение

- APK каждой проверенной сборки: `/home/uzzzver/operit-fork/apk/<UTC-дата-старт-CI>/`
  + `CHANGES.md` (3–6 строк дельты). Сейчас там ВОСЕМЬ папок:
  `2026-10-04_13-47-09Z` (R8-эксперимент, rejected),
  `2026-10-05_10-51-45Z` (A-сборка, терминал), `2026-10-05_17-52-47Z`
  (llm-io-log, 287387420 байт, CI `37351728353`),
  `2026-10-06_13-03-03Z` (supervision, 287428400 байт, CI `37467783125`)
  `2026-10-07_00-22-30Z` (minify-эксперимент: keep-fix, 247288710 байт,
  CI `37551624980`, cold start clean), `2026-10-07_01-07-20Z` (финал 4+5:
  247288542 байт, CI `37555506347`, ПОЛНАЯ приёмка),
  `2026-10-07_06-31-03Z` (задача 10: 247288246 байт, CI `37581930601`,
  e2e PONG) и `2026-10-08_01-06-04Z` (subpack Q1: 247299182 байт,
  CI `37709173132`, пикер+переезд приняты).
  Пользователь чистит сам (диск `/` уже 98% — КРИТИЧНО, чистка нужна
  до следующей серии сборок).
  Удалённые докачиваются с CI без пересборки: T3
  `gh run download 37201078764 --dir ...` (внутри
  `operit-android-34/apk/debug/app-debug.apk`, 373487996 байт), T2
  `37169808187`, T1-Market `37164035232` (имена каталогов внутри плывут —
  искать `find -name "*.apk"`; качать с запасом таймаута: 287 МБ ~5+ мин).
  Правило уже в `AGENTS.md`.
- Workflow `Android Build` на push срабатывает ТОЛЬКО для `main`
  (`on.push.branches: [main]`) — для feature-ветки запускать вручную:
  `gh workflow run "Android Build" --ref feat/english-only-build`.
- Рецепт живого чата (доказан 06.10 дважды, старый лабиринт через счётчик
  «0» больше не использовать): кнопка-«часы» в полоске под шапкой (~55,232)
  → слайд-меню Chat History → + New Chat (~218,355) → чат с полем ввода
  и модельным рядом готов к отправке. Свежая установка тоже сразу даёт
  поле ввода (workspace-ловушка 05.10 там не воспроизвелась). Тап по «часам»
  иногда не срабатывает с первого раза — повторить; свайп от левого края
  открывает НЕ то меню (app drawer), не путать.
- Старый лабиринт 05.10 (история, не повторять): пустое состояние → send
  тостит «Please create a new chat»; счётчик «0» создаёт беседу; новая
  беседа ТРЕБОВАЛА workspace (Create Default) и показывала ТОЛЬКО
  WebView-превью без поля ввода; таб «Operit» задизейблен,
  стрелки/свайпы/FAB-Unbind/BACK из превью не выводили. Тогда живой чат
  для ручной отправки так и не был получен — строки в лог дали
  ФУНКЦИОНАЛЬНЫЕ вызовы (T2-префетч), этого хватило для приёмки.
- Телефон `R9TN601D6GJ` полностью под управлением агента (policy ниже в
  силе); USB мёртв, adb только по WiFi; порт МЕНЯЕТСЯ при каждой смерти
  отладки (45687 → 41599 → 36201 → 44657) — НИКОГДА не хардкодить, последнее
   известное значение протухает. При обрыве — переспаривание (рецепт в
   чит-шите, пользователя просить только порт+код). Пакеты:
   `com.ai.assistance.operit.debug` (Operit), `moe.shizuku.privileged.api`.
    СОСТОЯНИЕ утро 08.10 (~04:15Z): сборка subpack Q1 (CI `37709173132`,
    fresh install 04:13 после uninstall — новый debug-ключ, визард ПРОЙДЕН
    полностью: agreement → тур → permissions через shell-гранты (см.
    чит-шит) → Debug level → чат ПУСТ (кнопка + New Chat, задача 10 живьём).
    Модельный пресет засидился (fresh state), supervision ВЫКЛ (дефолт
    false, файл `supervision_preferences.xml` отсутствует — включать
    только если тест требует: рецепт через `run-as tee` в задаче 11).
    Телефон оставлен в Settings (строка subpack, location = memory card).
    Батарея 81%, заряжается. Screensaver выключен + screen_off_timeout
    30 мин. Порт 36255 (вечер 07.10 дал пользователь после смерти 38571).
    Закрыты: 1–5a/5b, 7a (ран `37682838742` зелёный), 9, 10, 11, 3a/3b, 4,
    subpack Q1-механика. Открыто: 6, 8, 12, 13 (см. шапку).
   `192.168.1.63` `37000-45200`+`5555` пуст = wireless debugging выключена.
   Только пользователь может включить (или дать новый IP).
   День 06.10: порты менялись 4 раза (43874→43873→35449→38571); обрывы
   (`offline`, `No route to host`) лечились переподключением/новым портом
   от пользователя. Вечером связь стабильна (38571).
- LAN gateway для тестов (модель, перевод): `http://192.168.1.55:20128/v1`,
  ключ и модель зашиты пресетом ТОЛЬКО в debug (`ModelConfigManager`,
  `BuildConfig.DEBUG`-ветка). В релиз не тащить.
- Диск `/` 98% (08.10, было 96%) — КРИТИЧНО; `/tmp/opencode` чистить после сборок
  (временные APK качать в `/tmp/opencode/<имя>`, удалять сразу после install).

---

## Чит-шит устройства и CI (читать перед любой приёмкой)

**adb.** USB мёртв (`lsusb` пуст), только WiFi. Порт меняется сам
(45687 → 41599 → 36201 → 44657 → 43873 → 35449 → 38571 → 36255
— 06.10 четырежды за день, 07.10 вечером 38571 умер, пользователь дал 36255) и отладка гаснет в простое — никогда не хардкодить
порт. ЛОВУШКА 06.10: пользователь даёт pairing-порт (был 43874) —
`adb connect` на него даёт `Connection refused`; настоящий adb-порт
приходит отдельно (43873). Не гадать: пробовать connect, при отказе —
переспросить/скан. Обрыв: попросить у пользователя pairing-порт + код
с экрана телефона,
`echo CODE | adb pair 192.168.1.63:PAIRPORT`, затем скан adb-порта
(python+socket, `37000-45200` + `5555`) и `adb connect IP:PORT`. Скан пустой
= отладка выключена, только пользователь может включить.
09.10: root через Magisk ЕСТЬ (`su -c id` uid=0, полные capabilities).
Включён классический ПОСТОЯННЫЙ ADB: `su -c 'setprop service.adb.tcp.port
5555'` + `stop adbd; start adbd` → `adb connect 192.168.1.69:5555`
(TLS-порт после этого отвалился сам — так и надо, лишний). Порт держится
до ПЕРЕЗАГРУЗКИ телефона. Автозагрузка НЕВОЗМОЖНА: `/data/adb/service.d`
не пишется даже root (оба неймспейса, даже в Permissive — режет прошивка).
После ребута — одна строка в Termux на телефоне:
`su -c 'setprop service.adb.tcp.port 5555; stop adbd; start adbd'`,
затем с ноутбука `adb connect 192.168.1.69:5555`. Локальный трюк 09.10:
порт открыт и пингуется, а `adb connect` висит — лечится
`adb kill-server; adb start-server` (stale server).
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
install 287 МБ иногда не стартует с первого раза (пустой dumpsys —
ставить заново с полным выводом, не `tail -1`); а иногда ЗАВИСАЕТ
посередине (06.10: 10-мин таймаут, состояние пакета неизвестно) — после
таймаута НЕ считать установленным, проверить `dumpsys`/переставить.
 Временные APK — только в `/tmp/opencode/`, удалять сразу после install
(диск 98%).
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
Дополнение 08.10 (доказано на subpack-сборке): цикл «→ → Incomplete →
Continue → список» БЕСКОНЕЧЕН без грантов — это не баг, визард требует
разрешений. Выдать shell-набором (проверено достаточным):
`pm grant PKG READ_EXTERNAL_STORAGE + WRITE_EXTERNAL_STORAGE +
ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION`,
`appops set PKG MANAGE_EXTERNAL_STORAGE + SYSTEM_ALERT_WINDOW allow`,
`dumpsys deviceidle whitelist +PKG`. После этого зайти В строку Storage
(открывается системная «All files access» — там уже ВКЛ, так и должно
быть), вернуться назад, тапнуть Check Permission Status — все 4 ✓
(«All basic permissions granted»), дальше → идёт. UI до revisit+Check
показывает × несмотря на выданные гранты — не пугаться, это stale,
а не отказ.
**Экран.** `uiautomator dump` дохнет с SIGKILL (exit 137, 0-байт xml) —
не чинится. Только `screencap -p` + чтение PNG через Read (агент ВИДИТ
скриншоты). Чёрный кадр = поймал переход анимации, не смерть: подождать
3 с и переснять; одинаковые чёрные кадры имеют одинаковый md5 (7714 Б —
можно сверять, не открывая). НО той же ночью: чёрный кадр 2 раза подряд =
экран УСНУЛ посреди сессии (проверять `dumpsys display | grep mScreenState`):
лечение `settings put secure screensaver_enabled 0` +
`settings put system screen_off_timeout 1800000`, затем WAKEUP. На свежей
установке грант screensaver из визарда может быть не применён — проверять
первым делом при ночной приёмке. Экран спит — кадры протухают (часы стоят): WAKEUP первым
делом и сравнивать md5. `run-as` — только одиночные команды (кавычки с `;`
ненадёжны); НЕ забывать `adb shell` — голый `ls /storage/...` идёт на
ноутбук, а не на телефон (дало ложную «потерю данных» в M6).
**БД тянуть ТРИ файла:** `databases/app_database` + `-wal` + `-shm`
(06.10: без WAL свежие строки `llm_io_log` НЕ ВИДНЫ — ложный «хук не
пишет», полчаса впустую). Класть рядом с одинаковым basename, открывать
sqlite3 основной файл. `logcat` —
только `logcat -t N`, полный `-d` вешает shell. `adb pull` БЕЗ `-q`
(такого флага нет — команда падает). `input text`: пробел — ТОЛЬКО `%s`;
`%20` НЕ раскодируется и уходит литералом в поле ввода. Хитрее: `%sX`
тоже опасно — `%PONG` ушёл литералом `%PONG` (валиден только `%s`);
после `input text` ВСЕГДА скрин и чтение набранного глазами до send.
**Ловля тостов** (`ChatToastHost`, верх по центру): длительность
`estimateToastDurationMs` — ~5 с на короткий коммент; одиночными скринами
не поймать (06.10: ~50 мимо). Метод: серия back-to-back `screencap`
(без sleep, ~2 с/кадр, 20–30 шт), затем `md5sum` — одинаковые склеить,
отличающиеся читать (тост = белый кард поверх стрипа). Время поста тоста
= `timestampMs` + `latencyMs` строки в `llm_io_log` — считать окно оттуда.
**Тапы.** В скролленных списках настроек и дропдаун-меню точка касания
садится на ~75–100px ВЫШЕ запрошенного Y (доказано `settings put system
pointer_location 1`: запрос (182,700) дал точку (182,625)). Симптомы: промах
мимо Download в «Add from link», выбор не той строки переезда. Обход:
целиться на +80..100 ниже визуального центра (проверено: запрос (182,765)
попал в Download и стартовал закачку); меню выбирать клавишами
`KEYCODE_DPAD_UP/DOWN + DPAD_CENTER` — детерминировано, без координат
 (так вернулся на internal). Drawer ВЕРХ (плитки Packages/Permissions,
 гамбургер, Settings внизу) — 1:1, компенсации не надо; НИЗ drawer
 (ряды AI Features: Memory Base ~745, Toolbox ~868) — садится НИЖЕ
 на ~70–120 (07.10: запрос 745 открыл Toolbox; поправка 05.10 «+70 вниз»
 подтверждается). Нижние ряды целить ВЫШЕ цели на ~100 и сверять
 результат. `pointer_location 1` показывает точку касания, после отладки
 выключить обратно в 0 (загрязняет скрины и сдвигает вёрстку вниз).
 Дополнение 05.10: смещение НЕСТАБИЛЬНО между экранами (drawer низ +70..120
 вниз, Setup −70 вверх, Terminal-home ~1:1/+35) — поправку НЕ переносить,
 на каждом экране калибровать заново; надёжно покрывает кластер из 3 тапов
 с шагом 70 через цель. Дополнение 08.10: экран Settings, низ списка (кнопка Change строки
 subpack) — тоже садится НИЖЕ на ~100 (5 тапов в глиф мимо, сработал
 прицел на 100 выше); выпадающее меню-попап при этом 1:1. Мелкие кнопки
 внизу списков целить выше сразу, не жечь 5 попыток.
 ВНИМАНИЕ, КОНФЛИКТ НАПРАВЛЕНИЙ (не сглаживать): M6 05.10 на ТОМ ЖЕ
 классе экранов намеряло ОБРАТНОЕ — касание ВЫШЕ запроса на 75–100
 (запрос (182,700) → точка (182,625), компенсация прицелом НИЖЕ).
 08.10 — касание НИЖЕ запроса (~100). Направление дрейфа НЕСТАБИЛЬНО
 даже внутри Settings (разные дни/сборки/зоны экрана). Правило:
 направлению не верить вообще — на каждом экране и каждой сессии
 калибровать заново (pointer_location или 1 пробный тап с md5-сверкой),
 мелкие цели брать вертикальным кластером ±100 с шагом 50, попап-меню
 считать 1:1 пока не доказано обратное.
 Дополнение 07.10 (E11): НИЗ ЧАТ-экрана (send,
 кнопка New Chat) — хитбоксы живут на ~40–100px ВЫШЕ глифов (send: глиф
 1447 → хит 1410; New Chat: глиф 1330, сработал один из прочёса 1150–1400);
 pointer_location при этом показывает 1:1 — верить ХИТБОКСУ, не координатам.
 Промах выглядит как «кнопка мертва» (нет ripple): не чинить код, а
 прочёсывать вертикаль шагом 50. Побочка: тап по ⌨ включает
 «ADB Keyboard {ON}»-полосу.
Текст кнопок объявлений МЕНЯЕТСЯ между сборками («I see» / «Got it» /
«Understood») — целиться по позиции, не по тексту.
 Кнопка Skip в Environment Setup = onBack (терминал НЕ стартует!); сессию
 создаёт «+» (рабочая точка 678,195). Сам SetupScreen при входе молча создаёт
 сессию `setup-check` — инициализация окружения (ссылки bin) идёт уже от
 открытия экрана.
**Market:** путь — Packages → розовая иконка-магазин → диалог Agreement
(сначала countdown «Please wait... (N)», кнопка неактивна) → дождаться
активной кнопки → СКРОЛЛ текста до конца (секции 3–4) → кластер из 3 тапов
с шагом 70 по кнопке (одиночные тапы дважды ушли в молоко 07.10).
Согласие переживает перезапуск экрана (диалог не повторялся).
**Тул-ходы на Standard (приёмка с моделью-драйвером):** КАЖДЫЙ вызов тула
поднимает Permission Request (Deny / Allow / Always allow-ссылка);
one-shot Allow не запоминается между разными тулами — каскад диалогов
(list_files → use_package → super_admin:terminal → code_runner...).
Для приёмки слать ТРИВИАЛЬНЫЕ безтуловые ходы («Say PONG.»); если драйвер
ушёл в тул-цикл (счётчик 39, жрёт API) — Deny + красный X (стоп ходит
мгновенно, даже из-под диалога: сначала Deny, потом X). Отменённый ход
НЕ наблюдается супервизором (хук только на чистом finalize — по дизайну),
в логе останется строка `error='cancelled'`.
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
**Stdout терминального bash через `adb shell run-as` ТЕРЯЕТСЯ** (07.10:
`bash -c echo TERM_OK` → пусто при exit 0; редиректы в файл тоже
съедаются кавычками через 3 слоя shell) — для пруфов использовать
`adb exec-out run-as ...` (без pty): `TERM_OK` приходит чисто.
**Состав APK смотреть ТОЛЬКО `python3 -c zipfile`** (08.10: `unzip -l`
на 247-МБ APK вернул 0 строк без ошибки — молча ничего; python
`ZipFile.namelist()` + `getinfo().file_size` работает всегда).
**Стектрейсы упавших JVM-тестов — в HTML-артефакте:** workflow грузит
`operit-android-test-reports-<N>` (имя узнать:
`gh api repos/lagovi/Operit/actions/runs/<run>/artifacts --jq
'.artifacts[].name'`), качать
`gh run download <run> -n <имя> --dir ...`, трейсы — в
`app/build/reports/tests/testDebugUnitTest/classes/<Class>.html`
в тегах `<pre>` (доставать python+re+html.unescape; XML-результатов
в артефакте НЕТ). `--log-failed` даёт только однострочники.
**Fresh install = чистое поле:** чаты стёрты, supervision ВЫКЛ
(`supervision_preferences.xml` отсутствует, дефолт false), модельный
пресет засидился заново (endpoint/key/model из `ModelConfigManager`
DEBUG-ветки), announcement-диалог вылезет поверх первого экрана
(текст кнопки плывёт — целиться по позиции ~548,1113).
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
   РАЗВЯЗКА 07.10: keep-fix (`b4ff4a30`) сработал — CI `37551624980`
   success, APK −40.1 МБ, холодный старт чист, `onCall` в dex на месте.
   Детали — в задаче 4; открытый вопрос там же (minify в debug навсегда?).
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
   МЕТОДОЛОГИЯ (важно для будущих замеров): эпохи 435/369 мерены
   `adb shell du -sh` ТОЛЬКО по package dir (`/data/app/~~*/<pkg>`, точный
   путь — из `pm path`; шаблон `~~*` через adb shell не раскрывается):
   разбивка 435 = base.apk 297 + lib/arm64 152. `/data/data/<pkg>` (БД,
   files, распакованное rootfs-дерево 358 МБ!) НЕ СЧИТАЛСЯ — настоящий
   полный футпринт больше. Рецепт installed: `du -sh` ОБОИХ деревьев
   (package dir + `/data/data/<pkg>`) на одной и той же установке и
   цитировать оба числа раздельно, иначе сравнение с эпохами некорректно.
- **E3. Rootfs на внешнее хранилище (FAIL, обе локации).** Гипотеза: дерево
  358 МБ жить на SD. Метод: `busybox cp -a` (uid приложения, `run-as`) из
  `files/usr/var/lib/proot-distro/installed-rootfs/ubuntu` в EXTERNAL_APP
  (`/storage/emulated/0/...`, FUSE), затем `ln -s`-проба на SDCARD_APP
  (`/storage/5982-1724/...`, карта 980 МБ). Улики: сотни `can't create
  symlink ... Operation not permitted` (`etc/ssl/certs`, `etc/alternatives`,
  merged-/usr `bin/lib/sbin`, `dev/std*`); на SD `ln: Permission denied`
   сразу, каталог пуст. Обход `-L` отвергнут расчётом: ~700 МБ дублей
   (ОЦЕНКА, не замер: размер дерева ×2 минус выгода от схлопывания —
   в 980 МБ карты не влезет с запасом) + висячие ссылки (`dev/std*`,
   `mtab`) всё равно мёртвы. Вердикт:
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
- **E7. Ловля 5-секундных тостов одиночными скринами (FAIL, метод).**
  Гипотеза: «отправлю ход, подожду, сниму скрин — увижу тост». Метод 06.10:
  ~50 `screencap` вокруг 3 доказанных постов (`llm_io_log` вердикты ok +
  непустые комментарии, `timestampMs`+`latencyMs` дают точное окно) — 0
  попаданий: длительность `estimateToastDurationMs` (~5 с на короткий
  коммент) меньше типичного интервала между ручными скринами. Вердикт:
  тосты ловятся только непрерывной серией back-to-back `screencap`
  (~2 с/кадр, 20–30 шт) + `md5sum`-дедуп (одинаковые склеить, отличающиеся
  читать). Окно поста вычислять из строки лога, не гадать.
- **E8. Проверка `llm_io_log` без WAL (FAIL, метод).** Гипотеза: «стяну
  `databases/app_database` и посмотрю строки». Метод 06.10: свежих строк
  нет — ложный вывод «хук не пишет», полчаса отладки несуществующего бага.
  Улика: Room в WAL-режиме, свежие коммиты живут в `-wal`/`-shm`. Вердикт:
  тянуть всегда ТРИ файла одним basename (`app_database`, `-wal`, `-shm`),
  открывать sqlite3 основной. Внесено в чит-шит.
- **E9. Тул-ход flash-lite как приёмка супервизии (FAIL, поведение модели).**  Гипотеза: «ход с тулами даст богатый дайджест и верный corrected_call».
  Метод 06.10: «What files are in the current directory?» → драйвер ушёл
  в цикл list_files/use_package/super_admin:terminal/code_runner
  (счётчик тул-вызовов 39, каждый новый тул — новый Permission Request,
  one-shot Allow не запоминается); завершить удалось только Deny + X.
  Отменённый ход НЕ наблюдается (хук только на чистом finalize). Улика:
  строки `llm_io_log` function=CHAT с `<tool_*>` вызовами, `error=
  'cancelled'` у оборванного. Вердикт: приёмку делать ТОЛЬКО тривиальными
  безтуловыми ходами («Say PONG.»); тул-ходы — отдельная история с ручным
  Allow-бабиситтингом и расходом API. Побочка для дизайна: дайджест v1
  (имена тулов без аргументов) на таких ходах был бы слепым — аргументы
   в хуках нужны тем более (см. ограничение v1 в задаче 2).
- **E11. «Мёртвая» кнопка, которая жива (FAIL, калибровка).** Гипотеза:
  «onClick не стреляет» (код textbook, тоггл-механизм доказан через
  кнопку-«часы», состояние чистое — а меню не открывается). Метод 07.10:
  ~15 тапов в глиф кнопки New Chat (центр/края/кластеры/лонг-пресс,
  mid-touch захват доказал касание ровно (360,1330) Prs 1.0) — ноль
  эффекта, ноль ripple; force-stop + свежий процесс — то же. Развязка:
  вертикальное прочёсывание 1150–1400 — один из тапов создал чат
  («Ping Test» + поле ввода + PONG e2e). Улика задним числом: у send та
  же картина (глиф 1447, хит 1410). Вердикт: хитбоксы низа чат-экрана
  живут на ~40–100px ВЫШЕ глифов; промах выглядит как мёртвая кнопка.
  Правило: низ чата целить с запасом вверх; при «мертве» — прочёсывание
  шагом 50, не разбор кода. Потрачено ~1.5 ч на ложный след.
- **E10. Кластер по кнопке send (FAIL, механика кнопки).** Гипотеза:
  «кластер из 3 тапов надёжнее одиночного». Метод 07.10: 3 тапа по send
  (1410/1447/1480) — ход ушёл, но в `llm_io_log` строка CHAT с
  `error='cancelled'` @403 мс + рядом TITLE_GENERATION тоже cancelled:
  кнопка send превращается в STOP пока идёт стрим, и последующие тапы
  кластера убивают только что отправленный ход. Вердикт: по send —
  ТОЛЬКО один тап, затем руки прочь минимум 20 с; отменённый ход даёт
  карточку Prompt с пустым Response (не путать с зависшим).
- **Калибровка чата 07.10 (факт, не теория):** одиночные тапы в глиф
  send (670,1447) стабильно мимо (4+ попыток), срабатывает (670,1410) —
  на этом экране тапы садятся ~35px НИЖЕ цели (или хитбокс выше глифа).
  Доказано pointer_location: запрос (670,1447) = касание (670,1447)
  1:1, но кнопка не срабатывает — значит, дело не в сдвиге координат,
  а в геометрии хитбокса. Метод захвата касания: `input swipe x y x y 800
  & sleep 0.4; screencap` — крест и X:/Y: в кадре. Аномалия drawer той
  же ночью: тап (150,745) (ряд Memory Base) открыл Toolbox — за низкие
  ряды drawer целиться с запасом и сверять результат, не верить глазам.

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
