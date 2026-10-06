---
For_Agent: РЕАЛИЗОВАНО 2026-10-06, коммит `63dadf6e` (аудит 0). Приёмка НЕ
закрыта: CI `37443922062` + телефон (всплывашка + строка в логе). Ниже —
исходный план + зафиксированные при реализации решения.
---

# Режим надзора (supervision mode)

## Implementation notes (2026-10-06, коммит `63dadf6e`)

Что сделано сверх/вместо плана:

- Хук — только на конце хода (main-flow после `finalizeMessageAndNotify`,
  до очистки счётчиков), а не на каждом `onToolInvocation`: колбэк несёт
  только имя тула (без аргументов/результата), поэтому per-tool дайджесты
  были бы пустыми. Имена тулов копятся в `currentTurnToolNamesByChatId`
  (cap 50) и уходят в дайджест списком.
- Известное ограничение v1: в дайджесте/JSONL есть имена тулов, но НЕТ
  аргументов вызовов и результатов. Захват аргументов требует хуков глубже
  (`AIToolHandler`), это отдельная задача, не часть приёмки v1.
- Вызов наблюдателя — `recordTokenUsage=false` (как пробы): сырая строка НЕ
  дублируется автологом, токены не пачкают статистику. Вместо этого хук
  пишет ОДНУ обогащённую строку сам (сырой ответ + вердикт + комментарий).
- Запрет рекурсии — структурный, не флагом: вызывается напрямую
  `AIService.sendMessage` арендованного сервиса, turn-loop и
  `EnhancedAIService` в пути нет, наблюдатель не может наблюдать сам себя.
  Флаг не нужен, это зафиксировано здесь чтобы не «чинили».
- Отдельный `MultiServiceManager` внутри `SupervisionObserver` (менеджер
  чата приватный). Свежесть привязки: перед каждым наблюдением сверяется
  `getConfigMappingForFunction(SUPERVISION)`, при смене —
  `refreshServiceForFunction`. Тумблер — `SupervisionPreferences`
  (SharedPreferences + StateFlow, default OFF), свитч в SettingsScreen
  рядом с Functional Model (план говорил туда).
- JSONL-экспорт — `LlmIoLogRepository.supervisionJsonl(limit)` (готовое API,
  UI-экрана пока нет — follow-up). Формат строки (фиксирован):
  `{"digest": {"driver_model", "user", "tools", "tool_count", "final"},
  "driver_call": {"tool_names"}, "corrected_call": {...}|null,
  "comment": string, "model_ids": {"supervisor", "driver"},
  "verdict": "ok|corrected|parse_error|observer_error",
  "latency_ms": n|null, "timestamp_ms": n}`.
  Строки без дайджеста пропускаются, не фабрикуются.
- Миграция Room v22→v23 (`MIGRATION_22_23`): 5 nullable колонок
  (`supervisionComment/CorrectedCall/Verdict/Digest/DriverModel`).
- Тост — только непустой `comment` (сырьё при parse_error пользователю не
  показывается), cooldown 30 с на чат, cap коммента 500 символов.
- Приёмка (открыта): включить тумблер, отправить чат-ход, увидеть всплывашку
  (если наблюдателю есть что сказать) + строку `function=SUPERVISION` в
  LLM I/O Log с вердиктом; аудит 0 (уже зелёный на коммите).

# Режим надзора (supervision mode)

## Original State

Чат всегда ведёт одна модель (привязка CHAT). Боковые вызовы существуют
(заголовки/саммари через `getAIServiceForFunction(SUMMARY)` в
`MessageCoordinationDelegate.kt:1633`, извлечение памяти, генерация
персоны), но ни один не наблюдает живой ход и не комментирует его.
Всплывающие сообщения в чате уже есть: `ChatToastHost(event: ChatToastEvent?)`
(`ui/features/chat/components/ChatToastHost.kt:43`, верх по центру,
автозакрытие) с очередью через `toastEvent` в
`ui/features/chat/viewmodel/UiStateDelegate.kt`. Привязка «какая модель за
какую функцию» уже есть: `FunctionType` (`data/model/FunctionType.kt`) +
`FunctionalConfigManager` + экран `FunctionalConfigScreen.kt`.

## Intent

Режим, в котором рулит простая (дешёвая) модель — путь CHAT не меняется, а
рядом умная модель-наблюдатель получает срезы транскрипта (сообщение юзера,
вызовы тулов и их результаты, черновик/финал ответа) и отвечает
СТРУКТУРИРОВАННЫМ JSON: `comment` (что происходит / что не так — идёт во
всплывающее сообщение) и `corrected_call` (null, если вызов водителя верен;
иначе — как именно нужно было осуществить вызов: имя тула + аргументы).
Наблюдатель только пишет: без тулов, без действий, read-only. Включается
тумблером, умная модель выбирается пользователем (одна; несколько
наблюдателей — позже, не в первой версии).

Цели две. Первая — аналитика работы модели-водителя: как часто и по каким
тулам наблюдатель её поправляет, какие классы ошибок. Вторая — удобный съём
качественных трейсов для дообучения своей модели: связки
(контекст → вызов водителя → исправленный вызов + комментарий) с экспортом в
JSONL. Хранилище трейсов — лог из `../llm-io-log/index.md`, наблюдатель туда
пишет разобранные поля.

## Scope

- Новый `FunctionType.SUPERVISION` в `data/model/FunctionType.kt` + маппинг в
  `FunctionalConfigManager.kt` + строка выбора модели в
  `FunctionalConfigScreen.kt` (шаблон — существующие функциональные ряды).
  Промпт наблюдателя — в `core/config/FunctionalPrompts.kt` рядом с
  остальными (роль: «ты наблюдатель, комментируй коротко, что делает агент и
  есть ли подозрительное»).
- Хук наблюдателя на границах хода в
  `services/core/MessageProcessingDelegate.kt` (`onToolInvocation`,
  `finalizeMessageAndNotify`, `onTurnComplete`) — fire-and-forget корутина с
  арендованным сервисом (`MultiServiceManager.getServiceForFunction(
  SUPERVISION)` → `ServiceLease`, шаблон — `ConversationService.translateText`
  в `api/chat/enhance/ConversationService.kt:1102`). Вызов — напрямую через
  `AIService.sendMessage` (ad-hoc `PromptTurn` SYSTEM+USER), НЕ через
  `EnhancedAIService` и не через turn-loop, иначе рекурсия.
- Что слать: компактный дайджест (последнее сообщение юзера, имя+аргументы
  вызова тула, краткий результат, финал ответа), а не всю историю — иначе
  цена наблюдателя съест выигрыш от дешёвого водителя.
- Формат ответа — строгий JSON `{"comment": string, "corrected_call":
  object|null}`. Системный промпт требует ровно JSON без markdown-обёрток;
  парсер срывает ```json fences, не распарсилось — запись с ошибкой парсинга
  в лог, во всплывашку ничего (сырьё пользователю не показывать).
- Во всплывашку (`toastEvent` → `ChatToastHost`) — только `comment`;
  `corrected_call` — в запись лога; UI-деталь «как надо было» по тапу — не в
  первой версии. Лимиты: максимум N комментариев на ход (например 3),
  cooldown между ними, при выключенном тумблере хук молчит полностью
  (ноль оверхеда).
- Аналитика (цель 1): агрегация поправок по тулам и классам ошибок. Минимум —
  данные в Room (см. `../llm-io-log/index.md`); экран — при реализации,
  кандидат — вкладка рядом с tokenstats.
- Трейсы для файнтюна (цель 2): экспорт JSONL
  (digest, driver_call, corrected_call, comment, model_ids обеих моделей);
  точный формат зафиксировать в doc при реализации.
- Тумблер режима — в настройках рядом с функциональными моделями
  (`SettingsScreen.kt` → `settings_functional_model`), состояние в
  DataStore (`data/preferences/*Preferences.kt`).
- Вызовы наблюдателя логируются в лог LLM-трафика (см.
  `../llm-io-log/index.md`) с `function=SUPERVISION`.
- Стрings только в `values/strings.xml`, метки — формат-ресурсами, никакого
  CJK в коде (гейт `fork_audit.py`: CJK-NEW = 0).

## Файлы

| файл | роль |
|---|---|
| `app/src/main/java/com/ai/assistance/operit/data/model/FunctionType.kt` | добавить SUPERVISION |
| `app/src/main/java/com/ai/assistance/operit/data/preferences/FunctionalConfigManager.kt` | привязка функции к конфигу |
| `app/src/main/java/com/ai/assistance/operit/ui/features/settings/screens/FunctionalConfigScreen.kt` | выбор умной модели |
| `app/src/main/java/com/ai/assistance/operit/core/config/FunctionalPrompts.kt` | системный промпт наблюдателя |
| `app/src/main/java/com/ai/assistance/operit/services/core/MessageProcessingDelegate.kt` | хук на границах хода |
| `app/src/main/java/com/ai/assistance/operit/api/chat/enhance/MultiServiceManager.kt` | аренда сервиса под наблюдение |
| `app/src/main/java/com/ai/assistance/operit/api/chat/enhance/ConversationService.kt` | шаблон ad-hoc вызова (~L1102) |
| `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/ChatToastHost.kt` | показ комментариев |
| `app/src/main/java/com/ai/assistance/operit/ui/features/chat/viewmodel/UiStateDelegate.kt` | очередь toastEvent |
| `app/src/main/java/com/ai/assistance/operit/ui/features/settings/screens/SettingsScreen.kt` | тумблер режима |

## Риски

- Рекурсия: вызов наблюдателя обязан идти мимо turn-loop и мимо хука
  наблюдения (прямой `AIService.sendMessage`), иначе наблюдатель начнёт
  наблюдать сам себя. Запретить флагом в контексте вызова.
- Цена: умная модель на каждом ходе — дорого; дайджест короткий, лимиты
  жёсткие, режим строго opt-in с явным выбором модели пользователем.
- Конкуренция с драйвером: `ServiceLease` уже умеет параллельные сервисы,
  но длинный комментарий не должен тормозить ответ водителя — полностью
  асинхронно, показ по готовности.
- Запрет на fallback-логику в силе: ошибка наблюдателя = просто нет
  комментария, без маскировки и без ретраев-молчунов.
- `corrected_call` никогда не исполняется автоматически — только запись в
  лог. Иначе наблюдатель тихо станет вторым водителем, а это другой режим
  с другими рисками.
- Системный оверлей (`FloatingChatService`) для этого НЕ использовать —
  комментарии живут внутри чата (`ChatToastHost`), оверлей затрагивает
  разрешения и чужой UI.
