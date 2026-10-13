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
| `1ee5dbb4` порядок tool-результатов (cherry-pick -n, чистый) | PORTED 11.10, ждёт CI |
| `72c1216b` DB health repair (cherry-pick + AppDatabase резолв + 55 строк в EN) | PORTED 11.10, ждёт CI |
| `19b50643` DB health collapse + `907a4c77` weight-import fix (cherry-pick + EN) | PORTED 11.10, ждёт CI |
| `f2414493` config repair (cherry-pick + 14 rewords + 15 строк в EN) | PORTED 11.10, ждёт CI |
| `40081a2a` DataStore domain repair (cherry-pick -n, чистый) | PORTED 11.10, ждёт CI |
| `62ea0c3e` perf-мониторы (cherry-pick + обе строки Settings + 50 строк EN + terminal-submodule PTY pid) | PORTED 11.10, ждёт CI |
| `4e6ae68a` CPU sampling поверх | PORTED 11.10, ждёт CI |
| `6d5e8ae8` floating crash-protection (cherry-pick -n, auto-merge чистый) | PORTED 11.10, ждёт CI |
| `567f245a` group-orchestration дубли (ручной, threaded через sendMessageInternal) | PORTED 11.10, ждёт CI |
| `21212530` dialogue review (cherry-pick + 4 строки EN) | PORTED 11.10, ждёт CI |
| `1e89d4f2` composedsl курсор (cherry-pick -n, чистый) | PORTED 11.10, ждёт CI |
| `a19d7ef8` unpaired toolcalls (cherry-pick, мёртвые провайдеры отброшены) | PORTED 11.10, ждёт CI |
| `6bb2a245` unmatched tool history ( wholesale 6 файлов + EN, через промежуточные переименования) | PORTED 11.10, ждёт CI |
| `2112033a`+`9cf8283e` copy-preview (cherry-pick + ручной резолв конфликта) | PORTED 11.10, ждёт CI |
| `72731683` copy-preview read-only field (cherry-pick + ручной резолв) | PORTED 11.10, ждёт CI |
| `99830874` tool round markup (ручной + тест) | PORTED 11.10, ждёт CI |
| `e295a82a` summary-секции (cherry-pick + строки вручную в EN) | PORTED 11.10, ждёт CI |
| `863fb0d8` message_persisted (файл создан самим коммитом; cherry-pick -n, чистый) | PORTED 11.10, ждёт CI |
| `a7676fa3` (dev) Antigravity OAuth | DONE 13.10 (`92b4cc63`+`d2246757`, CI `38332016760` green, сборка `20261013_025022Z`) |
## Antigravity приёмка 13.10 (телефон, сборка 20261013_025022Z)
- PASS: `Antigravity (Google)` в диалоге провайдеров; блок входа
  (Sign in with Google + fixed endpoint + `gemini-3.7-flash` по умолчанию);
  OAuth доходит до accounts.google.com (лого Antigravity, скриншот);
  loopback+PKCE в logcat; падений нет.
- По пути: наш `GeminiProvider` держал `modelName/apiEndpoint/apiKeyProvider/JSON/customHeaders` в private (апстрим protected) + не имел `unwrapStreamingPayload` — расширено до апстримного вида, оба parse-сайта завернуты в хук.
- Пропущен осознанно дрейф (provider-groups, DeepSeek/Moonshot, isEmbeddedLocalProvider, юнит-тесты — их CI не гоняет).
- Телефон возвращён как был: Default Config = OPENAI_GENERIC + 192.168.1.55 + ключ цел (сейв в logcat).
- РУЧНОЙ ШАГ ПОЛЬЗОВАТЕЛЯ: завершить Google sign-in, проверить квоты/список моделей/чат через Antigravity.
## Бэклог (решения пользователя 12.10)
- **Antigravity**: DONE 13.10 (см. приёмку выше). Остался только ручной
  шаг пользователя (завершить Google sign-in).
- **PaddleOCR**: заменить/добавить движок PP-OCRv6-small (50 языков,
  вкл. русский). Путь: модуль `deploy/ppocr-android/ppocr-sdk` (ONNX
  Runtime, minSdk 26) + det/rec ONNX моделями в Q2 on-demand
  (как subpack: zip в релиз, ensure в filesDir, on-demand).
  Ориентир следа: 30–55 МБ вне APK. Baseline текущего (11.5 МБ,
  латиница 100%, 1–5 с) держать до приёмки Paddle: те же 5 картинок
  + русская должны читаться; DoD: точность ≥ текущей по латинице
  + русский разборчив + след замерен. Русский baseline-мусор
  (`/tmp/opencode/ocr_base/russian.png`) — в деле.
- **Прокси провайдера** (решение 13.10): в настройках провайдера —
  опция «подключение через прокси», типы SOCKS5 и HTTP/HTTPS.
  План: поля в `ModelConfigData` (host/port/type, креды — в key pool?),
  UI-блок в `ModelApiSettingsSection`, проброс в `AIServiceFactory`
  (OkHttp `Proxy(HTTP)` нативно; SOCKS5 без auth нативно, с auth —
  свой SocketFactory), плюс OAuth-клиенты (Codex/Antigravity).
  Нюанс: MNN/llama локальные — исключить. DoD: чат через
  HTTP-прокси и через SOCKS5 (с/без auth) глазами на телефоне.

## Не берём (REJECTED, сознательно)

- Все `values*/strings.xml`-добавления апстрима — только через ручной порт в наш `values/` (строки тащить по мере портов выше, не скопом).
- `ea819096` README-раскладка (противоречит нашей; наш README остаётся).
- woff2 Material Symbols bundle (`f9eded98`, `808c76d2`) — против SIZE-целей.
- `83acf075`/`c2333185` apk_reverse тулкит, `33f8ff7c` apktool-артефакты — удалённый нами инструмент.
- `92327665` proxy-история (трогает удалённые Deepseek/Kimi + model_logos-svg; брать только если портируем соседние провайдерные фиксы).
- i18n-коммиты (`a51f23b4`, `a46e0c62`, `d1a28473`, `98241209`, `f72b20f6`, `2f31888b`) — нечего локализовывать, форк English-only.
- Dev draw-пакеты (`29d67812`, `ef1bf4b6`, `14c8afdc`) — draw не трогаем.

## Долг: terminal-подмодуль (не блокирует)
- Указатель `26d4964a`→`04586898` (нужен `pty.pid` для perf-монитора) принёс
  49 hardcoded CN-строк (SSH-экраны, имена зеркал). Это код чужого репозитория
  (сабмодуль) — править там нельзя (CI пересоберёт чистое). Внесены в
  `cjk-allowlist.json` как upstream-долг. Полный EN-проход terminal — отдельной
  задачей, когда будем форкать terminal (апстрим сам начал values-en).
