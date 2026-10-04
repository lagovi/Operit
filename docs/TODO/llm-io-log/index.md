---
For_Agent: рабочая задача из очереди, не реализована. Лог обращений к LLM с просмотром и фильтром по модели.
---

# Лог обращений к LLM

## Original State

Сегодня LLM-трафик приложения виден только фрагментами, собрать «что модель
увидела и что ответила» можно лишь реконструкцией:

1. **Сеть.** Низкоуровневые провайдеры (`api/chat/llmprovider/OpenAIProvider.kt`,
   `GeminiProvider.kt`, `ClaudeProvider.kt`, `DeepseekProvider.kt` и др.) ходят
   через общий `OkHttpClient` из `AIServiceFactory.kt`. `LlmNetworkEventListener`
   пишет в logcat тег `AIHttpTrace` только тайминги (callStart/dns/connect),
   тел нет. `util/HttpLogSanitizer.kt` сознательно вымарывает значения
   (`[omitted]`) — в логах остаются URL и заголовки.
2. **Учёт токенов.** `TokenTrackingAIService.kt` оборачивает каждый
   `sendMessage`, `RequestTracker` пишет в `TokenUsageRepository.record()` —
   только счётчики и цены (`data/stats/`, экран
   `ui/features/tokenstats/TokenUsageStatisticsScreen.kt`). Промптов и ответов
   там нет.
3. **Точка входа чата.** `EnhancedAIService.sendMessage()` (~L903) →
   `AIMessageManager.sendMessage` → `MessageProcessingDelegate` собирает
   `Stream<String>` в Room и Compose. Единой точки «запрос начался / ответ
   готов», пишущей тела, не существует.

Отладка странного ответа модели сейчас — это logcat + догадки.

## Intent

Писать каждый LLM-запрос (id модели, время, system+user ходы, параметры) и
каждый ответ (полный текст, usage, задержка, ошибки) в локальное хранилище и
показывать в удобном экране внутри приложения с фильтром по используемой
модели. Экспорт записи. Хранилище с крышкой, чтобы лог не съел диск.

## Scope

- Хук в `TokenTrackingAIService.sendMessage` — центральный декоратор, видит
  все провайдеры и оба конца (запросные `PromptTurn` + собранный ответ из
  `TrackingStream.collect`). Альтернатива — `EnhancedAIService.sendMessage`,
  но она выше провайдеров функциональных моделей; решить при реализации,
  покрыть оба пути (чат + `getServiceForFunction`, иначе перевод/саммари
  выпадут из лога).
- Хранение — новая Room-сущность в `data/db/AppDatabase.kt` (миграция v22:
  `llm_io_log`: id, timestamp, function/CHAT-ad-hoc, modelId, configId,
  request JSON, response TEXT, promptTokens/completionTokens, latencyMs,
  error). DAO рядом с `data/dao/TokenUsageDao.kt`. Писать вне главного потока,
  стрим не блокировать.
- Крышка: последние N записей (например 500) или M мегабайт, prune при вставке.
  Настройка вкл/выкл + кнопка «очистить».
- Экран просмотра: список (время, модель, первые строки ответа, бейдж ошибки),
  детали записи (запрос/ответ раздельно, копирование), фильтр по modelId
  (выпадающий список реально встречавшихся моделей). Паттерн брать с
  `TokenUsageStatisticsScreen.kt`. Попасть в него — из настроек моделей.
- Ключи и секреты через `HttpLogSanitizer`, тела пользовательского контента —
  как есть (это устройство пользователя, его же данные).
- Стрings только в `values/strings.xml` (единственный бакет), метки —
  формат-ресурсами (`%1$s`), никакого CJK даже в комментариях (гейт
  `fork_audit.py`: CJK-NEW обязан быть 0).

## Файлы

| файл | роль |
|---|---|
| `app/src/main/java/com/ai/assistance/operit/api/chat/llmprovider/TokenTrackingAIService.kt` | хук записи (обёртка всех sendMessage) |
| `app/src/main/java/com/ai/assistance/operit/api/chat/EnhancedAIService.kt` | второй конец хука (путь чата), ~L903 |
| `app/src/main/java/com/ai/assistance/operit/api/chat/enhance/MultiServiceManager.kt` | путь функциональных моделей (TRANSLATION/SUMMARY) |
| `app/src/main/java/com/ai/assistance/operit/data/db/AppDatabase.kt` | новая сущность + миграция v22 |
| `app/src/main/java/com/ai/assistance/operit/data/dao/TokenUsageDao.kt` | образец DAO |
| `app/src/main/java/com/ai/assistance/operit/data/stats/TokenUsageRepository.kt` | образец record-пути |
| `app/src/main/java/com/ai/assistance/operit/ui/features/tokenstats/TokenUsageStatisticsScreen.kt` | образец экрана статистики |
| `app/src/main/java/com/ai/assistance/operit/util/HttpLogSanitizer.kt` | санитизация секретов |
| `app/src/main/java/com/ai/assistance/operit/data/model/FunctionType.kt` | различие CHAT / функциональных вызовов в записях |

## Риски

- Рост диска: крышка + prune обязательны с первой версии, иначе лог за месяц
  съест гигабайты на длинных чатах.
- Производительность: запись в Room только фоном; сборка ответа не должна
  ждать вставку.
- Супервизия (см. `../supervision-mode/index.md`) пишет свои вызовы сюда же:
  `function=SUPERVISION` + разобранные поля (comment, corrected_call JSON,
  verdict ok/corrected, ошибка парсинга). Экспорт JSONL с фильтрами
  (function=SUPERVISION, has_correction) — это датасет для файнтюна;
  формат экспорта зафиксировать в doc супервизии при реализации.
- Не писать тела в logcat (там их сегодня нет осознанно) — только в Room.
