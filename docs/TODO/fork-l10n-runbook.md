# Runbook: перевод форка Operit на английский с нуля (после обновления upstream)

Инструкция для агента. Цель: после мержа новой версии upstream снова получить сборку, где **весь пользовательский UI читаем на английском**, и проверить её на устройстве. Все шаги проверены на коммитах `610d9f0b..6344e44a` (см. `git log` и «Прогресс» в `docs/TODO/doit-fork.md`).

Принципы:
- Целевая локаль — только **English** (`values-en`). Китайский (`values/`) — исходник ключей, не трогаем. Прочие локали (es/ja/ko/…) не заполняем — вне скоупа.
- Ничего не выдумывать вручную: всё через проверяемые скрипты и CI. Не добавлять fallback-логику в код (жёсткое правило проекта).
- Не гнать: каждая правка ключей верифицируется до пуша. Пуш в `main` триггерит CI-сборку (concurrency `android-build-push-main`, старая отменяется) — не пушить лишний раз во время нужной сборки.

---

## 0. База

- Апстрим: `AAswordman/Operit`. Форк: `lagovi/Operit`. Workflow: `.github/workflows/android-build.yml` (job `android-apk-debug`, артефакт `operit-android-N`, ~20–40 мин).
- Деплой: `ci/script/deploy_latest_apk.sh [out_dir] [run_id]` — качает APK последней успешной сборки и ставит через adb.
- Тест-устройство: Samsung SM-A207F (Android 11), 720x1560, по USB. Тонкости — см. конец («Телефон: шпаргалка»).

## 1. Инвентаризация: что переведено

```bash
python3 tools/string/check_strings.py        # целостность локалей; интересует "英文: 完整"
python3 tools/string/count_ui_strings.py     # китайские строки в коде → tools/string/chinese_strings_detailed.txt
```

- `check_strings.py` покажет недостающие en-ключи (новые в upstream) → их надо перевести.
- `chinese_strings_detailed.txt` — список хардкод-китайских литералов в `.kt`. Отсеять внутренние (см. §3), остальное — в ресурсы.

## 2. Дозаполнение values-en (новые ключи upstream)

Механика «пакетного ИИ-перевода» (уже встроена в tooling):

```bash
python3 tools/string/check_strings.py --export-tsv /tmp/en_missing.tsv   # zh → tsv для ИИ
# → передать TSV ИИ-агенту на перевод → /tmp/en_missing_filled.tsv (3 колонки: key, zh, en)
python3 tools/string/check_strings.py --import-tsv /tmp/en_missing_filled.tsv --lang en
```

- ИИ-агент получает TSV целиком, переводит в стиле существующей en-локали (короткие UI-строки, Title Case для заголовков, сохранять `%1$s`/`%1$d` и термины: Tool Package, Skill, MCP, Workspace, Token, Character Card).
- После импорта: повторный `check_strings.py` → `英文: 完整`.
- Ревью: `git diff app/src/main/res/values-en/strings.xml | head -100` — проверить формат спецсимволов (`&amp;`, `\'`, `\n`).

## 3. Экстракция хардкод-китайских строк из кода

Для каждого файла из `chinese_strings_detailed.txt`:

**Оставить как есть (не трогать):**
- матчеры/regex для парсинга (эмоции, роли «用户/助手», «问题/解决方案», regex команд);
- LLM-промпты и тексты, уходящие в контекст модели (даты/дни недели в промптах, суффиксы разметки);
- сообщения только в лог (AppLogger) и dev-`require()`;
- нативные названия языков, имена голосов TTS, URL, JSON-ключи, HTML-атрибуты, ключи DataStore.

**Извлекать (пользователь видит):** Text/тосты/ошибки в UI, label'ы в диалогах, contentDescription, сообщения Exception доходящие до UI, подписи в селекторах.

Механика (как в коммите `610d9f0b`):
1. В Kotlin заменить литерал на `stringResource(R.string.<key>)` (composable) или `context.getString(R.string.<key>, args)` (если есть Context в скоупе).
2. Если класса/функции нет Context и это не composable — строку НЕ трогать (или аккуратно пробросить параметром от composable-вызывающего, см. пример `MarketBrowseList`).
3. Ключи дописать в `values/strings.xml` (zh-исходник) и `values-en/strings.xml` (перевод) перед `</resources>`. Формат: `<string name="k">…</string>`, одинаковый набор ключей в обоих файлах.
4. Приватные non-composable хелперы, вызываемые только из composable, можно пометить `@Composable` и использовать `stringResource`.

**Ловушки компиляции (уже словлены дважды — проверять статически до пуша):**
- `stringResource` НЕЛЬЗЯ внутри `try/catch` → выносить все вызовы наружу (пример: `relativeTime` в UnifiedMarketScreen).
- `stringResource` НЕЛЬЗЯ в `LazyListScope`-лямбде (тело `LazyColumn { }`, `groupedMarketItems`) → вычислить выше и передать параметром.
- `stringResource` можно в inline-лямбдах (`ifEmpty`, `let`, `when`).
- `@Composable`-функцию нельзя вызывать из non-composable кода.
- Если поменял тип таблицы вариантов (`List<Pair<String,String>>` → `List<Pair<String,Int>>`), обнови ВСЕ использования (`grep`).

## 4. Edge-to-edge / читаемость

Приложение edge-to-edge (`Theme.kt: setDecorFitsSystemWindows(false)`). Основной контент защищён инсетами в `AppContent.kt`, а экраны вне него — нет. На устройстве это = нижние кнопки под системной навигацией, недоступные и невидимые.
- Проверять новые полноэкранные экраны на `systemBarsPadding()` у корневого контейнера (уже исправлено: AgreementScreen, PermissionGuideScreen).
- Длинные label'ы в Row SpaceBetween: `Modifier.weight(1f)` + `maxLines=1` + `TextOverflow.Ellipsis` (пример: UnifiedMarketScreen).

## 5. Сборка и деплой

```bash
git push origin main                                   # триггерит Android Build
gh run watch <run_id> --exit-status --interval 60      # фоном, ждать
# при failure: gh run view <run_id> --log-failed | grep "e: file" — там ошибки компиляции
ci/script/deploy_latest_apk.sh                         # скачать+установить
# при INSTALL_FAILED_UPDATE_INCOMPATIBLE: adb uninstall <pkg> && adb install <apk> (данные слетят → §6 восстановление)
```

## 6. Настройка тест-устройства (после чистой установки)

```bash
PKG=com.ai.assistance.operit.debug
# разрешения
adb shell pm grant $PKG android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant $PKG android.permission.WRITE_EXTERNAL_STORAGE
adb shell appops set $PKG MANAGE_EXTERNAL_STORAGE allow
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
adb shell dumpsys deviceidle whitelist +$PKG
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
# язык приложения = en и конфиг модели — прямым патчем DataStore (см. шпаргалку ниже),
# готовые файлы лежат на машине агента: /tmp/user_prefs_patched.pb, /tmp/mc2_patched.pb
# (endpoint http://192.168.1.55:20128/v1/chat/completions, модель googleai/gemini-3.5-flash-lite)
```

Онбординг (уже на английском): соглашение (кнопка активна после 5с) → тур 1-3 (стрелки Next; свайпы отключены) → Welcome → Basic Permissions → Permission Level: Standard → Complete → закрыть китайское remote-объявление (это серверный контент, не баг).

## 7. Верификация на устройстве

Чеклист (всё через `uiautomator dump` + grep; скриншоты могут не читаться):
1. Соглашение/тур/permissions — английский, кнопки доступны (bounds не обрезаны у y=1422).
2. Главный чат, drawer-меню, Settings, Model Config, Packages — английский.
3. Сквозной тест ИИ: включить автосоздание чата (см. шпаргалку), ввести «ok», Send → в `files/logs/operit.log` хвост: `回合完成`, `TokenStatisticsDelegate … Input/Output` — конвейер работает.
4. `check_strings.py` локально: `英文: 完整`; `count_ui_strings.py`: остаток только из «белого списка» §3.

## Телефон: шпаргалка (критично, экономит часы)

- **Ввод текста**: Samsung IME ДУБЛИРУЕТ каждый символ при `input text` и бродкастах ADBKeyboard. Приём: ввести, затем ровно столько backspace (keyevent 67), сколько лишних (1 backspace = удаляет 2 символа). ADBKeyboard (`com.android.adbkeyboard/.AdbIME`) принимает `am broadcast -a ADB_INPUT_TEXT --es msg 'word'` (без пробелов; с пробелами ломает парсинг am).
- **Патч DataStore через run-as** (app force-stopped): proto формат Preferences: верх = repeated Pair(1); Pair = string name(1) + Value value(2); Value = вложенное сообщение (bool→field1 `0x08`, string→field2 `0x0a`). Скаляр напрямую в Pair писать НЕЛЬЗЯ — будет `CorruptionException: Value not set` и краш приложения. Рабочий пример bool-true: `0a190a13<19 байт имени>12020801`. Скрипт-пример: `/tmp/patch_model_config.py` (на машине агента).
- **Координаты тача**: dump-пространство 0..1422 при экране 720x1560; для полноэкранных окон/диалогов physical_y = dump_y + 54; для главного окна чата — как правило raw. Пробовать оба варианта. Тап по координатам берём из bounds узла.
- **uiautomator dump** периодически отдаёт старое/пустое (`null root node`, `already registered`): всегда сверять, что дамп изменился (размер/контент), прежде чем делать вывод «тап не сработал».
- Экран гаснет быстро: `adb shell settings put system screen_off_timeout 600000`; пробуждение: `input keyevent KEYCODE_WAKEUP` + `82`.
- Системная локаль телефона ru-RU; приложение берёт язык из своей настройки (datastore `app_language`) — en применяется при старте.
- Remote-объявление (公告) приходит с апстрим-сервера на китайском — это НЕ ресурсы приложения, не «исправлять» в коде.

## Память агента

В `~/.codebuddy/projects/home-uzzzver-operit-fork-Operit/memory/` есть готовые заметки: `reference_phone_testing.md` (все квирки телефона), `project_fork_goal.md` (цель/пайплайн), `reference_tool_quirks.md` (bash-команды могут исполняться дважды — проверять состояние перед повтором; Edit/Read иногда падают — ретрай или python через Bash). Читать в начале сессии.
