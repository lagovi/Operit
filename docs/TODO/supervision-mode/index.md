---
For_Agent: рабочая задача из очереди, не реализована. Режим надзора: дешёвая модель ведёт чат, умная комментирует во всплывашках.
---

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
вызовы тулов и их результаты, черновик/финал ответа) и комментирует
происходящее короткими всплывающими сообщениями. Наблюдатель только
комментирует: без тулов, без действий, read-only. Включается тумблером,
умная модель выбирается пользователем.

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
- Комментарии — в существующую очередь `toastEvent` → `ChatToastHost`, новый
  тип события не нужен, достаточно текста. Лимиты: максимум N комментариев
  на ход (например 3), cooldown между ними, при выключенном тумблере хук
  молчит полностью (ноль оверхеда).
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
- Системный оверлей (`FloatingChatService`) для этого НЕ использовать —
  комментарии живут внутри чата (`ChatToastHost`), оверлей затрагивает
  разрешения и чужой UI.
