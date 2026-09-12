## Глобальная задача
### Что нужно исправить
В процессе беглого тестирования выявил следующие проблемы:
- непереведенный китайский местами,
- включать английский нужно наугад,
- в некоторых местах текст не влазит в поля,
- приложение занимает очень много места. 

### Соображения другого агента

**Архитектура**: Operit использует стандартные Android string-ресурсы. Файл по умолчанию/источник истины — `app/src/main/res/values/strings.xml`, и он написан на **китайском** (а не на английском, как можно было бы ожидать). Переводы лежат в `values-en`, `values-es`, `values-ko`, `values-ms`, `values-id`, `values-pt-rBR`, `values-ro` [1](#1-0) . Само переключение языка обрабатывается функцией `LocaleUtils.setAppLanguage()`, которая использует `AppCompatDelegate.setApplicationLocales()` на Android 13+ или вручную патчит `Configuration`/`Resources` на более старых версиях [2](#1-1) .

**Появление непереведённого китайского текста** имеет две отдельные причины:

1. **Неполные переводы для конкретных локалей.** Только `en`, `ko`, `es` помечены как "Complete" во внутреннем трекинге проекта; `ms`, `id`, `pt-BR`, `ro` помечены как "Available" (то есть менее строго поддерживаются) [3](#1-2) . Android автоматически откатывается к ресурсу по умолчанию (китайскому), когда ключ отсутствует в файле конкретной локали — это классический механизм, порождающий "случайные" вкрапления китайского в остальном англоязычном интерфейсе.
2. **Захардкоженные китайские строки прямо в Kotlin-коде**, минующие `stringResource()`/`R.string`, из-за чего механизм перевода к ним вообще не применяется. Разработчики в курсе этой проблемы: в репозитории есть отдельный внутренний инструмент `tools/string/count_ui_strings.py`, который сканирует все `.kt`-файлы на предмет "сырых" китайских строковых литералов (исключая логи), чтобы измерить масштаб именно этой проблемы [4](#1-3) .

Есть также `tools/string/check_strings.py` — скрипт в духе CI, который сравнивает набор ключей во всех директориях `values-*` с китайским источником, чтобы находить отсутствующие/лишние ключи по языкам [5](#1-4) , плюс `fill_missing_translations.py` для дозаполнения пробелов. То есть у мейнтейнеров есть инструментарий для этого, но он явно не отлавливает всё (например, PR #796 с попыткой добавить румынский язык был отклонён из-за несоответствия "локализационному гейту", согласно `docs/TODO/romanian_locale/index.md`) [6](#1-5) .

**Текст, не влезающий в поля**: во многих Compose `Text()` действительно используются `maxLines`/`TextOverflow.Ellipsis` вместе с `.weight()` или ограничениями `.widthIn(max = ...)` (например, в `AgentChatInputSection.kt`, `UnifiedMarketDetailScreen.kt`) [7](#1-6) [8](#1-7) . Но поскольку переведённые строки (португальский, индонезийский, развёрнутые английские формулировки) часто заметно длиннее лаконичных китайских оригиналов, любое место, где используется фиксированная `.width()` вместо `.weight()`/`wrapContentWidth`/`widthIn(max=)`, либо где отсутствуют `maxLines`/эллипсис, будет визуально обрезаться или переполняться после перевода — это широко распространённый структурный риск на всей поверхности из ~7000+ строковых ресурсов, а не единичный баг.

#### Перспективы форкнуть и исправить для себя

Технически форк вполне реален — это стандартное Gradle/Compose-приложение под Android, под лицензией LGPLv3 [9](#1-8) , причём инструменты локализации (`check_strings.py`, `fill_missing_translations.py`, `count_ui_strings.py`) уже встроены в репозиторий именно для этого класса проблем. Реалистичный план форк-и-фикс включал бы:

- Запуск `count_ui_strings.py` для выявления всех захардкоженных китайских литералов в Kotlin UI-коде, с последующим переносом их в `strings.xml` + `stringResource()`.
- Запуск `check_strings.py` для целевой локали, чтобы найти недостающие ключи, и использование/доработку `fill_missing_translations.py` или ручной перевод для закрытия пробелов.
- Аудит layout-кода на предмет элементов `Text`/`Button`/`OutlinedTextField` с фиксированной шириной без `maxLines`+`TextOverflow.Ellipsis` или гибких width-модификаторов, и добавление их там, где не хватает.

Это большая, постоянно требующая внимания поверхность работы (тысячи строк в 7+ локалях плюс разбросанный по коду хардкод), а не разовый патч — стоит рассчитывать на регулярную задачу, а не на единичное исправление, особенно учитывая, что апстрим продолжает добавлять новые фичи и строки сначала на китайском.

##### Citations

**File:** app/src/main/res/values/strings.xml (L1104-1111)
```text
    <!-- Language Settings -->
    <string name="language_settings">语言设置</string>
    <string name="language_settings_desc">更改应用界面显示语言</string>
    <string name="change_language">更改语言</string>
    <string name="select_language">选择语言</string>
    <string name="language_info">更改语言后，应用将自动重启以应用新的语言设置</string>
    <string name="language_changing">正在切换语言...</string>
    <string name="language_changed">语言已更改，正在重启应用</string>
```

**File:** app/src/main/res/values/strings.xml (L1122-1122)
```text
        <p><b>1. 适用范围与协议版本</b><br>本协议适用于 Operit 官方发布的 Android 客户端及其可选在线功能。使用本应用即表示您已阅读并同意当前版本。本应用在启动时记录您确认的协议版本；协议发生实质更新时，应用将要求您重新确认后方可继续使用。仓库中的开源代码许可由根目录 <code>LICENSE</code> 所载 GNU 宽通用公共许可证第三版（LGPLv3）规定；本协议不排除或缩减适用法律及开源许可证赋予您的权利。</p>
```

**File:** app/src/main/java/com/ai/assistance/operit/util/LocaleUtils.kt (L135-194)
```kotlin
    fun setAppLanguage(context: Context, languageCode: String) {
        
        try {
            val manager = UserPreferencesManager.getInstance(context)
            runBlocking(Dispatchers.IO) {
                manager.saveAppLanguage(languageCode)
            }
        } catch (e: Exception) {
            AppLogger.e("LocaleUtils", "保存应用语言设置失败: $languageCode", e)
        }

        // 根据 languageCode 获取相应的 Locale
        val localeToSet = getLocaleForLanguageCode(languageCode, context)
        
        // 设置默认语言
        Locale.setDefault(localeToSet)
        
        // 根据Android版本应用语言设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用AppCompatDelegate API
            val localeList = LocaleListCompat.create(localeToSet)
            AppCompatDelegate.setApplicationLocales(localeList)
        } else {
            // 较旧版本Android使用资源配置
            try {
                val config = Configuration(context.resources.configuration)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val localeList = LocaleList(localeToSet)
                    LocaleList.setDefault(localeList)
                    config.setLocales(localeList)
                } else {
                    config.locale = localeToSet
                }
                
                // 更新上下文资源配置
                @Suppress("DEPRECATION")
                context.resources.updateConfiguration(config, context.resources.displayMetrics)
                
                // 尝试更新Activity
                try {
                    val ctx = context.applicationContext
                    if (ctx is ContextWrapper) {
                        val baseContext = ctx.baseContext
                        if (baseContext != null) {
                            @Suppress("DEPRECATION")
                            baseContext.resources.updateConfiguration(
                                config, 
                                baseContext.resources.displayMetrics
                            )
                        }
                    }
                } catch (e: Exception) {
                    // 忽略无法更新的上下文
                }
            } catch (e: Exception) {
                // 错误时静默处理
            }
        }
    }
```

**File:** app/src/main/res/xml/locales_config.xml (L1-11)
```text
<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="zh"/>
    <locale android:name="en"/>
    <locale android:name="ko"/>
    <locale android:name="es"/>
    <locale android:name="ms"/>
    <locale android:name="id"/>
    <locale android:name="pt-BR"/>
    <locale android:name="ro"/>
</locale-config> 
```

**File:** tools/string/count_ui_strings.py (L225-266)
```python
def count_chinese_strings_in_file(file_path: Path) -> Dict:
    """统计单个文件中的中文字符串"""
    result = {
        'file': str(file_path.relative_to('D:\\Code\\prog\\assistance')),
        'total': 0,
        'excluded_comments': 0,
        'excluded_logs': 0,
        'excluded_long_strings': 0,
        'remaining': 0,
        'examples': [],
        'items': []
    }

    try:
        content = file_path.read_text(encoding='utf-8', errors='ignore')
 
        literals = _iter_kotlin_string_literals(content)
        for lit in literals:
            string_content = lit['value']
            if not re.search(r'[\u4e00-\u9fff]', string_content):
                continue

            result['total'] += 1

            if _is_in_log_call(content, lit['start']):
                result['excluded_logs'] += 1
                continue

            mock_line = '"' + string_content + '"'
            if should_exclude_string(content, mock_line, (0, len(mock_line))):
                if len(string_content) > 100:
                    result['excluded_long_strings'] += 1
                continue

            item = {
                'line': lit['line'],
                'col': lit['col'],
                'string': '"' + _format_string_for_report(string_content) + '"',
                'length': len(string_content)
            }
            result['items'].append(item)

```

**File:** tools/string/check_strings.py (L14-53)
```python
LANG_DIR_RE = re.compile(r"^values-([a-z]{2,3})(?:-r([A-Z0-9]{2,3}))?$")
LANG_LABELS = {
    "zh": "中文",
    "en": "英文",
    "ko": "韩语",
    "es": "西班牙语",
    "pt-BR": "葡萄牙语(巴西)",
    "ms": "马来语",
    "id": "印尼语",
}


def _repo_root() -> str:
    return os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))


def _dir_to_language_code(dir_name: str):
    if dir_name == "values":
        return "zh"
    match = LANG_DIR_RE.fullmatch(dir_name)
    if not match:
        return None
    language = match.group(1).lower()
    region = match.group(2)
    if language == "in":
        language = "id"
    return f"{language}-{region.upper()}" if region else language


def _language_label(language_code: str) -> str:
    return LANG_LABELS.get(language_code, language_code)


def _discover_files(repo_root: str):
    res_dir = os.path.join(repo_root, "app", "src", "main", "res")
    files = {}

    default_file = os.path.join(res_dir, "values", "strings.xml")
    if os.path.exists(default_file):
        files["zh"] = default_file
```

**File:** docs/TODO/romanian_locale/index.md (L11-17)
```markdown
## Original State

The application has seven registered locales and no Romanian resource directory or in-app Romanian option. Pull request #796 attempted to add Romanian resources, but its locale configuration, resource keys, and placeholders do not satisfy the repository localization gate.

## Intent

Add Romanian as a complete application locale from the upstream Chinese resource source. The implementation does not reuse the candidate translation text from pull request #796.
```

**File:** app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/agent/AgentChatInputSection.kt (L914-921)
```kotlin
                                    Text(
                                        text = modelLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 160.dp),
                                    )
```

**File:** app/src/main/java/com/ai/assistance/operit/ui/features/packages/market/UnifiedMarketDetailScreen.kt (L763-768)
```kotlin
        Text(
            text = if (action.isLoading) action.loadingLabel ?: action.label else action.label,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
```


####Ниже — план для агента.

##### Существующая инфраструктура

В проекте уже есть три Python-скрипта в `tools/string/`, специально предназначенные для этой задачи:
- `check_strings.py` — сравнивает ключи во всех `values-*` директориях с китайским исходником, находит расхождения [1](#2-0) 
- `count_ui_strings.py` — сканирует `.kt`-файлы на предмет захардкоженных китайских строковых литералов, минующих систему ресурсов [2](#2-1) 
- `fill_missing_translations.py` — автоматически переводит отсутствующие строки через AI (OpenAI-совместимый API), используя `values/strings.xml` как эталон, удаляет "устаревшие" ключи и дозаполняет пробелы батчами с параллелизмом [3](#2-2) [4](#2-3) 

Агенту нужно опереться на них, а не писать логику с нуля.


Репозиторий: AAswordman/Operit (Android/Kotlin, Jetpack Compose).

Цель: (A) устранить непереведённый китайский текст, случайно проскакивающий в других локалях, и (B) исправить места UI, где локализованный текст не влезает в отведённое поле (обрезается/переполняется).

###### Часть A: Синхронизация и заполнение переводов

1. В корне репозитория запустить `python tools/string/check_strings.py`, чтобы получить полный отчёт о недостающих/лишних ключах по каждой локали (`values-en`, `values-es`, `values-ko`, `values-ms`, `values-id`, `values-pt-rBR`, `values-ro`) относительно эталона `app/src/main/res/values/strings.xml`. Сохранить вывод как baseline-отчёт.

2. Запустить `python tools/string/fill_missing_translations.py --report-only`, чтобы увидеть количество недостающих строк для каждого целевого языка без вызова AI. Сверить с отчётом из шага 1.

3. Настроить конфигурацию AI-провайдера в `tools/github/.env` (переменные `AI_BASE_URL`, `AI_API_KEY`, опционально `AI_MODEL`, `AI_TEMPERATURE`) — без этого `fill_missing_translations.py` не сможет переводить (см. `tools/string/fill_missing_translations.py`, функция `main()`, где считываются эти переменные окружения).

4. Выполнить сухой прогон на небольшом количестве строк: `python tools/string/fill_missing_translations.py --targets <lang>,... --dry-run --limit 20`, проверить качество и формат сгенерированных переводов (сохранение плейсхолдеров `%1$s`, escape-последовательностей, HTML-тегов внутри `<string>`).

5. После проверки качества выполнить полный прогон для всех локалей без `--dry-run`, чтобы дозаполнить реально отсутствующие строки:
   `python tools/string/fill_missing_translations.py --targets en,es,ko,ms,id,pt-BR,ro`
   Скрипт сам удалит устаревшие ключи (присутствующие в переводе, но отсутствующие в китайском эталоне) и добавит недостающие, не трогая уже существующие переводы.

6. Повторно запустить `python tools/string/check_strings.py`, чтобы убедиться, что расхождение ключей между локалями закрыто (0 missing / 0 stale).

7. Отдельно проработать хардкод: запустить `python tools/string/count_ui_strings.py` по всему `app/src/main/java/**/*.kt`, получить список файлов и строк с "сырыми" китайскими литералами вне логов/комментариев/длинных строк-исключений. Для каждого найденного случая:
   - Добавить соответствующий ключ в `app/src/main/res/values/strings.xml` (китайский текст) с осмысленным именем (`can use tools/string/add_string.py` для добавления в едином формате, если он поддерживает такую операцию — проверить `tools/string/add_string.py` перед использованием).
   - Заменить в Kotlin-коде прямой литерал на `stringResource(R.string.<key>)` (в Compose) или `context.getString(R.string.<key>)` (вне Compose).
   - Повторить прогон `fill_missing_translations.py` для новых ключей, чтобы они получили переводы во всех локалях.

8. Не переносить/не изменять уже открытые незакрытые попытки (см. `docs/TODO/romanian_locale/index.md` — там описано, что PR #796 для румынского языка был отклонён за несоответствие "локализационному гейту" репозитория; при работе с локалью `ro` учитывать эти требования отдельно, не копируя текст из того PR).

###### Часть B: Исправление обрезки/переполнения текста в UI

1. Найти все Composable-функции `Text(...)`, `Button(...)`, `OutlinedTextField(...)`, `DropdownMenuItem(...)` и аналогичные, использующие `stringResource(...)` в качестве текста, по всему `app/src/main/java/com/ai/assistance/operit/ui/**`.

2. Для каждого найденного места проверить:
   - Задан ли фиксированный `.width(...)` модификатор без `maxLines`/`TextOverflow.Ellipsis` — заменить на `.widthIn(max = ...)`, `.weight(1f)` в `Row`/`Column`, или `wrapContentWidth()`, либо добавить `maxLines = 1` + `overflow = TextOverflow.Ellipsis`, если обрезание допустимо по смыслу UI. Примеры правильного паттерна уже есть в коде: `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/style/input/agent/AgentChatInputSection.kt` (строки ~914-921) и `app/src/main/java/com/ai/assistance/operit/ui/features/packages/market/UnifiedMarketDetailScreen.kt` (строки ~763-768) — использовать их как референс.
   - Особое внимание — экраны настроек, диалоги, кнопки и чипы/бейджи с короткими фиксированными контейнерами, так как переводы на португальский/индонезийский/английский обычно длиннее китайского оригинала.

3. Приоритизировать проверку экранов, которые содержат больше всего строк из `strings.xml` (настройки, маркетплейс пакетов, чат) — начать с директорий `app/src/main/java/com/ai/assistance/operit/ui/features/settings/`, `.../packages/market/`, `.../chat/`.

4. Для каждого исправленного места протестировать визуально хотя бы на двух локалях с длинным текстом (например `pt-BR` и `en`), переключая язык через настройки приложения (см. `LocaleUtils.setAppLanguage()` в `app/src/main/java/com/ai/assistance/operit/util/LocaleUtils.kt`), чтобы подтвердить, что текст не обрезается неожиданно и не выходит за пределы контейнера.

5. Составить итоговый отчёт (список изменённых файлов по Части A и Части B) в PR description, включая:
   - Сколько строк было переведено и для каких локалей.
   - Сколько хардкод-литералов было заменено на ресурсы.
   - Список экранов/компонентов, где были скорректированы constraints ширины/`maxLines`.

###### Citations

**File:** tools/string/check_strings.py (L14-53)
```python
LANG_DIR_RE = re.compile(r"^values-([a-z]{2,3})(?:-r([A-Z0-9]{2,3}))?$")
LANG_LABELS = {
    "zh": "中文",
    "en": "英文",
    "ko": "韩语",
    "es": "西班牙语",
    "pt-BR": "葡萄牙语(巴西)",
    "ms": "马来语",
    "id": "印尼语",
}


def _repo_root() -> str:
    return os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))


def _dir_to_language_code(dir_name: str):
    if dir_name == "values":
        return "zh"
    match = LANG_DIR_RE.fullmatch(dir_name)
    if not match:
        return None
    language = match.group(1).lower()
    region = match.group(2)
    if language == "in":
        language = "id"
    return f"{language}-{region.upper()}" if region else language


def _language_label(language_code: str) -> str:
    return LANG_LABELS.get(language_code, language_code)


def _discover_files(repo_root: str):
    res_dir = os.path.join(repo_root, "app", "src", "main", "res")
    files = {}

    default_file = os.path.join(res_dir, "values", "strings.xml")
    if os.path.exists(default_file):
        files["zh"] = default_file
```

**File:** tools/string/count_ui_strings.py (L225-266)
```python
def count_chinese_strings_in_file(file_path: Path) -> Dict:
    """统计单个文件中的中文字符串"""
    result = {
        'file': str(file_path.relative_to('D:\\Code\\prog\\assistance')),
        'total': 0,
        'excluded_comments': 0,
        'excluded_logs': 0,
        'excluded_long_strings': 0,
        'remaining': 0,
        'examples': [],
        'items': []
    }

    try:
        content = file_path.read_text(encoding='utf-8', errors='ignore')
 
        literals = _iter_kotlin_string_literals(content)
        for lit in literals:
            string_content = lit['value']
            if not re.search(r'[\u4e00-\u9fff]', string_content):
                continue

            result['total'] += 1

            if _is_in_log_call(content, lit['start']):
                result['excluded_logs'] += 1
                continue

            mock_line = '"' + string_content + '"'
            if should_exclude_string(content, mock_line, (0, len(mock_line))):
                if len(string_content) > 100:
                    result['excluded_long_strings'] += 1
                continue

            item = {
                'line': lit['line'],
                'col': lit['col'],
                'string': '"' + _format_string_for_report(string_content) + '"',
                'length': len(string_content)
            }
            result['items'].append(item)

```

**File:** tools/string/fill_missing_translations.py (L553-620)
```python
def main() -> int:
    repo_root = _repo_root()
    default_res_dir = repo_root / "app" / "src" / "main" / "res"
    default_source = default_res_dir / "values" / "strings.xml"
    default_env = repo_root / "tools" / "github" / ".env"

    parser = argparse.ArgumentParser(
        prog="fill_missing_translations.py",
        description="Use zh strings.xml as the baseline and fill missing translations for other languages.",
    )
    parser.add_argument("--env", default=str(default_env), help="Path to .env file")
    parser.add_argument("--res-dir", default=str(default_res_dir), help="Android res directory")
    parser.add_argument("--source-file", default=str(default_source), help="Source zh strings.xml")
    parser.add_argument(
        "--targets",
        default="",
        help="Comma-separated language codes or values-* dirs. Default: auto-discover existing language dirs. Use all,ms,id to include new languages.",
    )
    parser.add_argument("--batch-size", type=int, default=80, help="Strings per AI request")
    parser.add_argument(
        "--concurrency",
        type=int,
        default=12,
        help="Number of concurrent AI batch requests per target language",
    )
    parser.add_argument("--limit", type=int, default=0, help="Max missing strings per target to process")
    parser.add_argument("--dry-run", action="store_true", help="Translate but do not write files")
    parser.add_argument("--report-only", action="store_true", help="Only print missing counts, do not call AI")
    parser.add_argument("--model", default="", help="Override AI model from env")
    parser.add_argument(
        "--temperature",
        type=float,
        default=None,
        help="Override AI temperature from env",
    )
    args = parser.parse_args()

    if args.batch_size <= 0:
        print("[X] --batch-size must be > 0", file=sys.stderr)
        return 2
    if args.concurrency <= 0:
        print("[X] --concurrency must be > 0", file=sys.stderr)
        return 2
    if args.limit < 0:
        print("[X] --limit must be >= 0", file=sys.stderr)
        return 2

    res_dir = Path(args.res_dir).resolve()
    source_file = Path(args.source_file).resolve()
    if not source_file.exists():
        print(f"[X] source file not found: {source_file}", file=sys.stderr)
        return 2
    if not res_dir.exists():
        print(f"[X] res directory not found: {res_dir}", file=sys.stderr)
        return 2

    try:
        source_entries = _load_source_entries(source_file)
        targets = _resolve_targets(res_dir, args.targets)
    except Exception as e:
        print(f"[X] {e}", file=sys.stderr)
        return 2

    if not targets:
        print("[X] no target languages found", file=sys.stderr)
        return 2

    source_key_set = {entry.name for entry in source_entries}
```

**File:** tools/string/fill_missing_translations.py (L676-770)
```python
    had_failures = False

    for target, missing_entries in target_missing:
        if not missing_entries:
            continue

        _log(f"\n[LANG] {target.code} -> {target.file_path}")
        batches = _chunked(missing_entries, args.batch_size)
        worker_count = min(args.concurrency, len(batches))
        _log(f"[AI] rolling submit {len(batches)} batches with concurrency {worker_count}")
        completed_batches = 0
        completed_strings = 0
        failed_batches: list[int] = []
        stop_submitting = False

        with concurrent.futures.ThreadPoolExecutor(max_workers=worker_count) as executor:
            active_futures: dict[concurrent.futures.Future, tuple[int, int]] = {}
            next_batch_index = 1

            def submit_batch(batch_index: int) -> None:
                batch = batches[batch_index - 1]
                _log(
                    f"[QUEUE] {target.code}: batch {batch_index}/{len(batches)} queued ({len(batch)} strings)"
                )
                future = executor.submit(
                    _translate_and_render_batch,
                    index=batch_index,
                    base_url=base_url,
                    api_key=api_key,
                    model=model,
                    temperature=temperature,
                    target=target,
                    batch=batch,
                )
                active_futures[future] = (batch_index, len(batch))

            while next_batch_index <= len(batches) and len(active_futures) < worker_count:
                submit_batch(next_batch_index)
                next_batch_index += 1

            while active_futures:
                done, _ = concurrent.futures.wait(
                    active_futures.keys(),
                    return_when=concurrent.futures.FIRST_COMPLETED,
                )

                for future in done:
                    queued_index, queued_size = active_futures.pop(future)
                    try:
                        batch_index, batch, rendered_lines, translated = future.result()
                        completed_batches += 1
                        completed_strings += len(batch)

                        if args.dry_run:
                            sample_key = batch[0].name
                            _log(
                                f"[DRY-RUN] {target.code}: batch {batch_index}/{len(batches)} prepared {len(rendered_lines)} strings"
                            )
                            _log(f"[DRY-RUN] sample: {sample_key} = {translated[sample_key]}")
                        else:
                            _append_missing_strings(target.file_path, rendered_lines)
                            _log(
                                f"[WRITE] {target.code}: batch {batch_index}/{len(batches)} appended {len(rendered_lines)} strings"
                            )
                    except Exception as e:
                        failed_batches.append(queued_index)
                        had_failures = True
                        stop_submitting = True
                        _log(
                            f"[FAIL] {target.code}: batch {queued_index}/{len(batches)} failed ({queued_size} strings): {e}"
                        )

                while (
                    not stop_submitting
                    and next_batch_index <= len(batches)
                    and len(active_futures) < worker_count
                ):
                    submit_batch(next_batch_index)
                    next_batch_index += 1

        if failed_batches:
            _log(
                f"[SUMMARY] {target.code}: completed {completed_batches}/{len(batches)} batches, "
                f"wrote {completed_strings} strings, failed batches={failed_batches[:20]}"
            )
            if len(failed_batches) > 20:
                _log(f"[SUMMARY] {target.code}: ... and {len(failed_batches) - 20} more failed batches")
        else:
            _log(
                f"[SUMMARY] {target.code}: completed all {completed_batches} batches, wrote {completed_strings} strings"
            )

    if had_failures:
        _log("\n[WARNING] finished with failed batches; rerun will continue from remaining missing strings")
        return 1
```




#### Структурированный список действий по сокращению размера после форка

##### Безопасные (не теряют функциональность)

**1. Ограничить набор ML Kit языковых моделей.** Сейчас подключены отдельные пакеты распознавания текста для китайского, японского, корейского и деванагари [4](#4-3) .
- *Последствие*: заметное сокращение размера, если урезать до реально нужных языков.
- Мне нужен только русский и английский

##### Средние по риску (частично режут фичи или усложняют доставку)

**2. Перевести бандлы STT-моделей из "вшито в APK" в "скачивается при первом запуске".** Sherpa-NCNN zh-en модель (энкодер ~127 МБ) и Silero VAD сейчас скачиваются на этапе сборки Gradle и упаковываются прямо в assets APK через задачи `syncSttModelAssets`/`syncMainAssets`.
- *Последствие*: APK становится легче на сотню+ МБ.
- *Последствие/риск*: локальный (offline) голосовой ввод перестаёт быть "из коробки" — требует сети при первом использовании, усложняет установку в оффлайн-среде, добавляет код верификации/докачки (аналогично тому, как уже сделано для LLM-моделей MNN/llama).
- Мне нужен только русский и английский


###### Citations

**File:** app/build.gradle.kts (L113-181)
```text
val sttModelAssetsManifestFile = layout.projectDirectory.file("config/stt-model-assets.properties")
val generatedSttModelAssetsDir = layout.buildDirectory.dir("generated/stt-model-assets")
val generatedMainAssetsDir = layout.buildDirectory.dir("generated/main-assets")

val syncSttModelAssets by tasks.registering {
    description = "Downloads and verifies generated assets for local STT recognition."
    group = "build setup"

    inputs.file(sttModelAssetsManifestFile)
    outputs.dir(generatedSttModelAssetsDir)
    outputs.upToDateWhen { false }

    doLast {
        val manifestFile = sttModelAssetsManifestFile.asFile
        val assets = parseSttModelAssetManifest(manifestFile)
        val outputRoot = generatedSttModelAssetsDir.get().asFile
        outputRoot.mkdirs()

        val outputRootPath = outputRoot.toPath().toAbsolutePath().normalize()
        val expectedFiles = mutableSetOf<File>()

        assets.forEach { asset ->
            val destinationPath = outputRootPath.resolve(asset.targetPath).normalize()
            require(destinationPath.startsWith(outputRootPath)) {
                "STT model asset target escapes generated assets directory: ${asset.targetPath}"
            }
            val destination = destinationPath.toFile()
            expectedFiles.add(destination.canonicalFile)

            if (!verifySttModelAsset(destination, asset)) {
                if (destination.exists() && !destination.delete()) {
                    error("Unable to replace invalid STT model asset: ${destination.path}")
                }
                downloadSttModelAsset(asset, destination)
            }

            require(verifySttModelAsset(destination, asset)) {
                "STT model asset verification failed after sync: ${asset.targetPath}"
            }
        }

        outputRoot.walkBottomUp()
            .filter { it.isFile && it.canonicalFile !in expectedFiles }
            .forEach { file ->
                require(file.delete()) {
                    "Unable to remove stale STT model asset: ${file.path}"
                }
            }
        outputRoot.walkBottomUp()
            .filter { it.isDirectory && it != outputRoot && it.list()?.isEmpty() == true }
            .forEach { directory ->
                require(directory.delete()) {
                    "Unable to remove empty STT model asset directory: ${directory.path}"
                }
            }
    }
}

val syncMainAssets by tasks.registering(Sync::class) {
    description = "Assembles application assets with verified generated STT model files."
    group = "build setup"
    dependsOn(syncSttModelAssets)

    from("src/main/assets") {
        exclude("models/**")
    }
    from(generatedSttModelAssetsDir)
    into(generatedMainAssetsDir)
}
```

**File:** app/build.gradle.kts (L250-252)
```text
        release {
            isMinifyEnabled = false
            isShrinkResources = false
```

**File:** app/build.gradle.kts (L373-381)
```text
    implementation("com.github.jelmerk:hnswlib-core:1.2.1")
    implementation(project(":dragonbones"))
    implementation(project(":terminal"))
    implementation(project(":mnn"))
    implementation(project(":llama"))
    implementation(project(":mmd"))
    implementation(project(":fbx"))
    implementation(project(":showerclient"))
    implementation(project(":quickjs"))
```

---

## Прогресс (2026-09-12)

### Сделано
- **Этап 0 (пайплайн)**: форк → GitHub Actions (`Android Build`, assembleDebug) → артефакт → `adb install` → телефон SM-A207F. Добавлен `ci/script/deploy_latest_apk.sh`.
- **Часть A (переводы)**:
  - en-локаль: 7659/7659 ключей (check_strings.py: `英文: 完整`).
  - Извлечено ~110 хардкод-китайских литералов из 19 kt-файлов в strings.xml (zh+en), +99 новых ключей. Пропущены осознанно: матчеры эмоций/парсинга, LLM-промпты, лог-сообщения, имена голосов TTS, нативные названия языков.
  - Удалён orphan-ключ `follow_chat_model_for_all_functions` (был только в values-ja, не используется).
  - Исправлен `tools/string/count_ui_strings.py` (хардкод Windows-пути).
- **Часть B (читаемость)**:
  - UnifiedMarketScreen: ellipsis для notification label.
  - **Найдено на устройстве**: кнопки онбординга (соглашение, гид разрешений) уходят под system nav bar (edge-to-edge без systemBarsPadding) — исправлено в AgreementScreen и PermissionGuideScreen.
  - Тур-онбординг: `userScrollEnabled=false` — только кнопки Next.
- **Тестовая среда на телефоне**: модель `googleai/gemini-3.5-flash-lite` через прокси 192.168.1.55:20128 (OpenAI-совм.), язык English, разрешения выданы. Конфиг писался прямым патчем DataStore proto через run-as (Samsung IME дублирует ввод).

### Известные ограничения
- Удалённое объявление (RemoteAnnouncement) приходит с апстрим-сервера на китайском — вне ресурсов приложения.
- McpConfigImportParser: китайские ошибки валидации (нет Context; требует смены архитектуры).
- Прочие локали (es/ko/id/pt-BR/ms/ja/ro) не дозаполнены (~100-7400 ключей) — вне скоупа (цель: EN).
- Телефон: системная локаль ru-RU; app_language=en применяется из datastore при старте.

---

## Прогресс (2026-09-12)

### Сделано
- **Этап 0 (пайплайн)**: форк → GitHub Actions (`Android Build`, assembleDebug) → артефакт → `adb install` → телефон SM-A207F. Добавлен `ci/script/deploy_latest_apk.sh`.
- **Часть A (переводы)**:
  - en-локаль: 7659/7659 ключей (check_strings.py: `英文: 完整`).
  - Извлечено ~110 хардкод-китайских литералов из 19 kt-файлов в strings.xml (zh+en), +99 новых ключей. Пропущены осознанно: матчеры эмоций/парсинга, LLM-промпты, лог-сообщения, имена голосов TTS, нативные названия языков.
  - Удалён orphan-ключ `follow_chat_model_for_all_functions` (был только в values-ja, не используется).
  - Исправлен `tools/string/count_ui_strings.py` (хардкод Windows-пути).
- **Часть B (читаемость)**:
  - UnifiedMarketScreen: ellipsis для notification label.
  - **Найдено на устройстве**: кнопки онбординга (соглашение, гид разрешений) уходят под system nav bar (edge-to-edge без systemBarsPadding) — исправлено в AgreementScreen и PermissionGuideScreen.
  - Тур-онбординг: `userScrollEnabled=false` — только кнопки Next.
- **Тестовая среда на телефоне**: модель `googleai/gemini-3.5-flash-lite` через прокси 192.168.1.55:20128 (OpenAI-совм.), язык English, разрешения выданы. Конфиг писался прямым патчем DataStore proto через run-as (Samsung IME дублирует ввод).

### Известные ограничения
- Удалённое объявление (RemoteAnnouncement) приходит с апстрим-сервера на китайском — вне ресурсов приложения.
- McpConfigImportParser: китайские ошибки валидации (нет Context; требует смены архитектуры).
- Прочие локали (es/ko/id/pt-BR/ms/ja/ro) не дозаполнены (~100-7400 ключей) — вне скоупа (цель: EN).
- Телефон: системная локаль ru-RU; app_language=en применяется из datastore при старте.
