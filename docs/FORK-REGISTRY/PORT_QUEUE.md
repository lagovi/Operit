# Port queue: upstream → fork (заведена 11.10, решение пользователя)

Практика для долгоживущего localization/size-форка: wholesale-merge НЕ
делаем (воскресит удалённые локали/ассеты/CN-провайдеры), rebase 200
коммитов НЕ делаем (ломает протестированную линейку). Только точечные
порты. Fork-point: `1ed8687d` (07.09). Статусы: TODO / PORTED / MANUAL
(ручной перенос) / REJECTED (сознательно не берём) / BLOCKED (ждёт другой порт).

## Batch 1 — чистые (файлы не пересекаются с нашими)

| Upstream | Что | Статус |
|---|---|---|
| `43858cdf` ANR thinking-regex (`ChatUtils.kt`) | PORTED 11.10 |
| `7d1b7274` SQLite bind limit (`ChatContentDao.kt`) | PORTED 11.10 |
| `04eaa397` JS MIME (`WebChatHttpBridge.kt`) | PORTED 11.10 |
| `37696251` xAI draw URL rename (покрывает `5ffa0cf4`) | PORTED 11.10 |
| `671b8081` video_url Responses (`OpenAIResponsesProvider.kt` + тест) | PORTED 11.10 |
| `6152d820` версия 51/1.12.2 (руками, 2 строки) | PORTED 11.10 |
| `af743726` CI `packages: ''` | ЭКВИВАЛЕНТ: у нас уже `packages: platform-tools` с комментом — не трогать |

## Batch 2 — ручной перенос (пересечение с нашими файлами)

| Upstream | Что | Статус |
|---|---|---|
| `45d63331` clipboard-gate (ручной перенос + тесты) | PORTED 11.10, ждёт CI |
| `1ee5dbb4` порядок tool-результатов (`EnhancedAIService.kt` наш) | TODO |
| `72c1216b`+`19b50643`+`f2414493` DB/config repair (наш `AppDatabase.kt`, строки в удалённые локали — портировать строки только в `values/`) | TODO |
| `40081a2a` DataStore repair (нужен `PreferencesHealthManager.kt` из batch-2 выше) | BLOCKED |
| `62ea0c3e` perf-мониторы (наши `PackageDetailsDialog/SettingsScreen/OperitScreens`, строки) + `4e6ae68a` поверх | TODO |
| `6d5e8ae8` floating crash-protection (наш `FloatingChatService.kt`) | TODO |
| `567f245a` group-orchestration дубли (наш `MessageProcessingDelegate.kt`) | TODO |
| `21212530` dialogue review (наш `EnhancedAIService.kt`, `ModelConfig*`) | TODO |
| `1e89d4f2` composedsl курсор (наш `ToolPkgComposeDslScreen.kt`) | TODO |
| `a19d7ef8`+`6bb2a245` unpaired toolcalls (наши `Deepseek/KimiProvider` УДАЛЕНЫ — портировать только в живые провайдеры) | TODO |
| `2112033a`+`9cf8283e`+`72731683` copy-preview (наш `ChatArea.kt`) | TODO |
| `99830874` tool round markup (наш `EnhancedAIService.kt`) | TODO |
| `e295a82a` summary-секции (наши `EnhancedAIService`, промпты, строки) | TODO |
| `863fb0d8` message_persisted (нужен `ToolPkgChatMessageHookBridge.kt` — файла нет в форке; искать породивший коммит) | BLOCKED |
| `a7676fa3` (dev) Antigravity OAuth (поверх наших CN-вырезов, 5k реформат) | TODO (решать: нужен ли провайдер) |

## Не берём (REJECTED, сознательно)

- Все `values*/strings.xml`-добавления апстрима — только через ручной порт в наш `values/` (строки тащить по мере портов выше, не скопом).
- `ea819096` README-раскладка (противоречит нашей; наш README остаётся).
- woff2 Material Symbols bundle (`f9eded98`, `808c76d2`) — против SIZE-целей.
- `83acf075`/`c2333185` apk_reverse тулкит, `33f8ff7c` apktool-артефакты — удалённый нами инструмент.
- `92327665` proxy-история (трогает удалённые Deepseek/Kimi + model_logos-svg; брать только если портируем соседние провайдерные фиксы).
- i18n-коммиты (`a51f23b4`, `a46e0c62`, `d1a28473`, `98241209`, `f72b20f6`, `2f31888b`) — нечего локализовывать, форк English-only.
- Dev draw-пакеты (`29d67812`, `ef1bf4b6`, `14c8afdc`) — draw не трогаем.
