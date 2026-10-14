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
ОТКРЫТО: 8 (остались 5a/5b глазами — тело заточено под модель без
контекста), совместное ручное с пользователем (ждёт его слова),
БЭКЛОГ: Antigravity DONE 13.10 (остался ручной sign-in пользователя);
PaddleOCR DONE 14.10 (CI `38499450049`, сборка `20261014_025827Z`;
латиница 12/12 + русский 3/3-видимых; «обрезанные хвосты» оказались
кривым truth (картинки шире канваса), модель читает ровно пиксели); следующий —
прокси провайдера (SOCKS5 + HTTP/HTTPS, план в `PORT_QUEUE.md`).
  ЗАКРЫТО 11–12.10: 19 (очередь `PORT_QUEUE.md`: batch-1 + 17 ручных/
  cherry-pick портов; CI зелёные; остался только Antigravity — см. выше),
  20 (справка: офлайн-топики 12 экранов + «?» в аппбаре (FAB проиграл
  колонкам FAB — скриншот-доказательство); тур `screen_tour.uaw` 102 шага
  PASS на `20261012_104102Z`),
  21 (OCR-baseline: ML Kit Latin 11.5 МБ, 5/5 точно + русский мусор
  (ожидаемо); движок 1–5 с; PaddleOCR исследован — вердикт ниже).
  PADDLEOCR-ВЕРДИКТ 12.10 (текущий движок: 11.5 МБ, латиница 100%,
  офлайн, 0 интеграции): PP-OCRv6 имеет официальный Android SDK
  (`deploy/ppocr-android`, ONNX Runtime, minSdk 26) tiers tiny 1.5M /
  small 7.7M / medium 34.5M, 50 языков одной моделью — но реалистичный
  след small на устройстве 30–55 МБ (det+rec+ORT .so) против 11.5 МБ,
  плюс JNI-интеграция. Агент рекомендовал НЕ БРАТЬ; пользователь 12.10
  решил: НУЖЕН (русский). Статус: БЭКЛОГ, план в `PORT_QUEUE.md`
  (ppocr-sdk + модели в Q2 on-demand; DoD: латиница не хуже + русский
  разборчив + след замерен).
  ЗАКРЫТО 11.10 ночью: 6 (строка зелёная, опрос PASS=4 FAIL=0 —
  CLOSE-OUT в теле), плюс ранее: 18 (B+C PASS=8; D отложен).
12 (см. ниже — дело сделано в Q2),
13 (Q2 ЗАКРЫТ ЦЕЛИКОМ 11.10: subpack + helper APK + templates/emoji;
rootfs НЕ берём — E3 против, решение в силе).
  ЗАКРЫТО 10.10 ночью: 14 (см. CLOSE-OUT в теле), 15 (полная приёмка
  `accept10` PASS=10 FAIL=0 на `20261010_215026Z` — онбординг/picker/папка/
  скачивание/репак/подпись/offline-повтор), 17 (корень PBES2 + evict-фикс,
  доказательство в теле).
  Q2-ПРОДОЛЖЕНИЕ 10–11.10 (детали — ARCHIVE.md, здесь итоги): helper
  APK on-demand (`HelperApkStorage.kt`, релиз, `git rm` 4 ассета; APK
  `20261011_015941Z`, CI `38103675524`, −3443520 Б, walker PASS=6 FAIL=0;
  по пути walker добрал 4 бага: fd3-stdin, bare-bounds, WAIT_PKG,
  SCROLL_TO — все в memo); seeds templates/emoji (`AssetSeedStorage.kt`,
  `seed-templates.zip` 4227058 + `seed-emoji.zip` 3614642, `git rm` trees;
  APK `20261011_141729Z`, CI `38146579269`, −8208126 Б, PASS=8 FAIL=0).
  Нарушения по пути (честно): пуш на красном ANCHOR-LOST (SIZE-006
  перенесена на WorkspaceUtils) + registry.json с indent=2 (переделано,
  amend + force-with-lease своего docs-коммита). Q2 ЗАКРЫТ целиком.
12 (a/b apktool+desktop — ЗАКРЫТЫ 08.10, CI `37722980127`/`37726800230`),
 13 (subpack DoD ЗАКРЫТ 10.10 на `2026-10-10_00-41-11Z`; подпись чинена
 в задаче 17).
ЗАКРЫТО: всё остальное (1–5a/5b, 7a, 9, 10, 11, 3a/3b, 4,
subpack Q1-механика, 16 install-size — не трогаем, решение 10.10).
 Решения пользователя 08.10: keystore НЕ делаем (жизнь на гитхабе,
 debug-сборок достаточно); хостинг Q2 — GitHub Releases в том же репо,
 первый релиз вместе с Q2-кодом; ручное тестирование — позже, когда
 всё отшлифовано.
**Как читать:** статус ниже — правда на сейчас, читать первым. Разделы
с пометкой SUPERSEDED/CLOSED — история, не трогать. Хеш кодового коммита
сверять с `git log` — docs-коммиты поведение не меняют.

## Fresh-session memo (читать вторым — всё, чтобы не наступать снова)

**Связь.** Только `adb -s 192.168.1.69:5555` (классический ADB-TCP,
поднят root-командой `su -c 'setprop service.adb.tcp.port 5555;
stop adbd; start adbd'`). Перезагрузок НЕ боится: Termux + Termux:Boot
выполняют тот же скрипт (`~/.termux/boot/adb-tcp.sh`, root выдан один
раз) — доказано ребут-тестом 10.10 (порт вернулся через ~75 с). USB
мёртв (`lsusb` пуст). Если порт открыт и пингуется, а `adb connect`
висит — `adb kill-server; adb start-server` (stale server, 09.10).
Pairing-порт ≠ adb-порт: connect на pairing даёт refused/вис — не гадать,
просить у пользователя настоящий или брать 5555. TLS-порт беспроводной
отладки мёртв с момента включения 5555 (так и надо).
**Root.** Magisk `su` есть (`su -c id` → uid=0), в т.ч. из Termux
(`run-as com.termux su -c id`). НО: `/data/adb/service.d` не пишется
даже root с полными capabilities, в Permissive и из `su -mm` — режет
прошивка; Magisk-автозагрузку не чинить, обход — Termux:Boot выше.
**Гранты свежей установки** (shell-набор, достаточен): `pm grant PKG
READ/WRITE_EXTERNAL_STORAGE + FINE/COARSE_LOCATION + RECORD_AUDIO`,
`appops set PKG MANAGE_EXTERNAL_STORAGE + SYSTEM_ALERT_WINDOW allow`,
`dumpsys deviceidle whitelist +PKG`. Без них визард крутится по кругу
«Incomplete → Continue» — это не баг.
**Нажатия.** Основной инструмент — root-тапы sendevent:
`/dev/input/event3` (`ILITEK_TDDI`, протокол MT-B), координаты 1:1
к координатам дампа (доказано, в т.ч. y=1394–1407). Рецепт тапа:
slot 0 → BTN_TOUCH(330) 1 → tracking_id N++ → X(53) → Y(54) →
major(48)=9 → pressure(58)=50 → SYN → пауза 0.12 → tracking −1 →
BTN_TOUCH 0 → SYN. Готовая обёртка: `ci/script/phone_lab.sh`
(`plab_tap`, `plab_tap_text`, `plab_wait_text`, `plab_assert_leaf_file`).
`input tap` работает ВНУТРИ приложения, но ГЛУХ в системном DocumentsUI
(строки папок и кнопка USE — проверено прочёсыванием одиночных/двойных/
долгих тапов) — там только sendevent (палец больше не нужен вообще).
Клавиатурно: TAB ходит по фокусу (включая строки списка после тапа),
DPAD_UP/DOWN — по строкам, ENTER жмёт обычные кнопки, НО на USE/строках
DocumentsUI ENTER/SPACE/CENTER глухи (ENTER на USE уводит фокус).
**Чтение экрана.** `uiautomator dump` + парсинг XML (тексты/границы) —
основное; скриншоты (`screencap -p` + Read) — только для визуала.
Если dump дохнет с SIGKILL — фолбэк на скриншоты. Дрейф низа (E11):
хитбоксы на 40–100px ВЫШЕ глифов — при промахах прочёсывать вверх,
не давить в ту же точку.
**Файлы на телефоне.** Только `adb exec-out run-as PKG` (других путей
внутрь нет); БД тащить тремя файлами (`app_database`+`-wal`+`-shm`);
`input text` — только `%s`-безопасные строки; `/sdcard` переживает
переустановку (фиxture `/sdcard/q2html/index.html` на месте).
**CI/сборки.** Workflow только ручной dispatch И ТОЛЬКО ПОСЛЕ PUSH
(раннер чекаутит origin). Каждый CI-билд — новый debug-ключ:
перед install всегда `uninstall` (`-r` даёт UPDATE_INCOMPATIBLE).
Папка APK = UTC-старт рана + `CHANGES.md` (3–6 строк дельты).
Замер — `python3 zipfile`, НЕ `unzip -l` (врёт). Mapping-артефакт
тянуть всегда. 10.10 GitHub сам отключал Actions на форке (баннер
«disabled because of its Actions usage», dispatch 422; расписаний
у нас нет — эвристика на пачку долгих ранов): лечится ТОЛЬКО кнопкой
на вкладке Actions (владелец), API/тумблеры не помогают.
**Диск `/` хронически 95–100%.** Перед качанием артефактов смотреть
`df`; протухшие `/tmp/gh-artifact.*.zip` и дубли моделей — первые
кандидаты; тулкит (`jdk/kotlinc/venv`) и `gigaam-model` НЕ трогать.
**Правила.** No-fallback: чинить настоящую причину (примеры: keep
commons-compress вместо try/catch вокруг setTime; keep BC + не удалять
платформенный провайдер вместо глотания исключений). Коммиты: код и
доки — отдельно; audit-гейт (`fork_audit.py --no-color --no-hotspots`,
CJK-NEW 0) перед push.
**Негативные эксперименты (проверено, НЕ повторять):**
(a) adb-тапы по DocumentsUI — глухи (см. Нажатия); (б) Magisk service.d —
не пишется (см. Root); (в) `svc wifi disable` для offline-теста УБИВАЕТ
ADB-транспорт (связь только по WiFi!): offline — только iptables
owner-match на uid приложения (`iptables -A OUTPUT -m owner
--uid-owner <uid> -j REJECT`, снять `-D`); (г) `install -r` между CI-
сборками — UPDATE_INCOMPATIBLE (ключи разные); (д) BACK из пикера/
экранов теряет remember-состояние (папку/флоу выбирать заново,
URI-грант в системе при этом жив); (е) JKS-keystore на Android НЕ
существует в принципе (провайдера нет и не будет — чинить только
PKCS12-путь);   (ж) `removeProvider("BC")` вне связки с немедленной вставкой
  bundled-BC на его место — запрещён (голое удаление оставляло процесс
  без PKCS12 вообще, live-fire 09.10); сама связка remove+insert —
  рабочий механизм (upstream, задача 17): платформа в «BC»-слоте это
  урезанный форк без PBES2, и только evict даёт место полному bundled; (з) тумблеры
  Settings/API против 422 Actions — не работают, только кнопка на
  вкладке Actions; (и) `unzip -l` врёт нулями на этих APK.
  Сессия 10.10 вечер (приёмка+подпись, всё свежее):
  (к) `pkill -f <паттерн>` убивает СОБСТВЕННЫЙ шелл инструмента
  (паттерн совпадает с командной строкой) — PID искать `ps` и `kill <pid>`;
  ТО ЖЕ относится к `ps aux | grep <паттерн> | xargs kill`: если строка
  паттерна есть в твоей же команде (имена скриптов!), grep видит обёртку
  и убивает себя (поймано дважды — тихий суицид без вывода); kill только
  по PID из файла, записанного при запуске;
  (л) `unzip` НЕТ в системе вообще: всё через `python3 zipfile`; код —
  в `classesN.dex`, UI-строки — в resources.arsc (их отсутствие в dex —
  норма, не «вырезано»); короткие подстроки (`inserted`, `offered`) в
  dex-греп — шум, искать только длинные точные фразы;
  (м) `sed 'A,Bd'` + правки по номерам строк: после удаления номера
  плывут — границы проверять чтением; пустые `"""`-блоки, висячие `\`
  и съеденные закрытия чинить вручную + verify python-парсингом;
  (н) однострочные python-правки JSON: тип проверять ДО записи
  (я снёс cjk-allowlist, заменив dict списком ключей — спас
  `git checkout`, переделывал заново); (о) `grep -mN` режет matches —
  «строк нет» перепроверять без `-m`; (п) `adb install` 179 МБ флапает
  транзиентом — в скрипте причина + повтор, успех только по `Success`;
  (р) телефон СПИТ во время ожиданий (пустые дампы): `plab_wake` +
  `screen_off_timeout 1800000` с restore-trap на EXIT; пустой дамп ≠
  пустой экран — сначала будить; (с) модалки поверх (Announcement/Got it)
  уводят тапы мимо — dismiss-циклы перед каждым шагом навигации;
  (т) чистая установка НЕ трогает `/sdcard`: чужие `WebApp_*.apk`
  переживают прогоны — сверять имя/время/подпись, не хватать первый
  попавшийся; (у) `gh workflow run --ref` чекаутит HEAD на момент СТАРТА
  джобы — `headSha` сверять (`gh run view --json`), иначе тестируешь не
  тот код; (ф) имя APK-папки — UTC-старт рана (`createdAt`), не время
  скачивания; (х) долгие `adb`-вызовы — в `timeout` (install 600с),
  весь `_adb` — в `timeout 90` (транспорт умирал с `error: closed`
  и вешал прогон молча); (ц) фоновые прогоны — только nohup+лог, опрос
  чанками ≤10 мин, НИКОГДА блокирующий вызов на 25 мин (пользователь
  оборвёт и будет прав); (ч) опечатка в `rm -rf` чуть не снесла
  `assets/models` (спас `.keep` через `git checkout`) — rm-пути сверять
  `git status` СРАЗУ;   (ш) `if (false)` и заглушки запрещены даже
  временно — только полное удаление блока; (щ) trap EXIT в скрипте не
  срабатывает при `kill -9`: если экран перестал гаснуть — проверить
  `settings get system screen_off_timeout` и вернуть вручную.
  UI-walker (10.10 ночь, больше руками НЕ тапать — невоспроизводимо):
  движок `ci/script/uawalk.sh` + сценарии `ci/script/scen/*.uaw`
  (команды TAP/TAP_AT/EXPECT/WAIT_PKG/TYPE/BACK/HIDEKB/SWIPE_UP/DOWN/
  SCROLL_TO/SLEEP/DUMP; `uaw_elements` — инвентарь элементов для
  авторства); каждый шаг заявляет тап+ожидание, расхождение — ABORT с
  номером шага, командой, ожиданием, реальным экраном (пакет+тексты)
  и путём к дампу (`/tmp/uaw/dump_stepN.xml`);
  (э) `adb shell` ЖРЁТ stdin: сценарий читать только с fd 3, иначе
  выполняется шаг 1 и молча конец (поймано живьём);
  (ю) системный установщик говорит ПО-РУССКИ («Установить это
  приложение?»): трекинг экрана — только WAIT_PKG (`packageinstaller`),
  тексты локаль-зависимы; `appops set REQUEST_INSTALL_PACKAGES allow`
  — в сетап приёмки, иначе установщик упирается в диалог;
  (я) иконки без текстового узла (hamburger, FAB, chevron) — только
  TAP_AT по координатам из дампа; TAP ищет ТОЧНОЕ совпадение текста
  (EXPECT — подстроку: label «1. Install Provider App» маскирует
  отсутствие кнопки — раскрывать шаг сначала);
  (аа) позиция скролла после BACK непредсказуема — SCROLL_TO вместо
  слепого свайпа; клавиатуру перед Confirm снимать HIDEKB (BACK только
  если `mInputShown=true`, иначе убьёт диалог);
  (аб) в assert размера передавать голое число (функция сама ставит
  пробелы — « 2571773 » даёт тройные и mismatch на верном файле);
  (ав) якорь чат-экрана — «Enter question» (строка ввода), НЕ «New Chat»:
  у чата с историей/привязанным workspace кнопки New Chat в текстах нет,
  18 BACKов прошли мимо (E-урок, walker: стартовый EXPECT сценариев —
  тоже по вводу);
  (аг) SetupScreen terminal-модуля: дефолтный выбор = тихий no-op (E13),
  чекбоксы проверять checked=true в дампе, слепые тапы тогглят;
  вечный Interrupt НЕ означает «качает» (fire-and-forget без колбэка) —
  проверять `apt history.log` + `su -c ps` (root видит всё);
  (ад) короткий путь к env-setup: DEBUGGER → «Configure Terminal
  Environment» (сама пишет чего нет) → Expand → «Go to Terminal
  Configuration»;
  (ае) BACK-стек: привязанный workspace ловит BACK (экран Preview) —
  выход тогглом 671 110; monkey НЕ идёт домой, а resume'ит верхний экран
  стека (опрос в цикле это учитывает);
  (аж) криминалистика proot-установки: `history.log` (что ставил apt),
  `su -c ps` (что реально бежит), `NodeJS环境检查` в operit.log
  (pnpm/python/pip поимённо каждый вход на Permissions);
  (аи) протокол длинных работ (11.10, `phone_lab.sh`): `plab_status_init/
  status/final` пишут `<epoch> START/STEP/FINAL` в статус-файл;
  `plab_eta` считает темп и ETA (`done 3/24, elapsed 410s, ETA ~52m`);
  `plab_wait_final <file> <timeout>` спит чанками по 30 с и
  возвращается ДОСРОЧНО по FINAL — слепых снов больше нет, сон всегда
  осознанный (ETA → wait_final → tail файла); `_adb` теперь ретраит и
  сам лечит stale server (kill/start/connect), caller видит ошибку
  только когда телефон правда недоступен;
  (ай) CI: `concurrency cancel-in-progress: true` — два диспатча на одну
  ветку УБИВАЮТ друг друга (потеряна сборка 38198538271). Только серийно:
  диспатч → зелёный/красный → следующий. Проверка headSha после каждого;
  (ак) instrumentation под минифицированным debug: R8 вырезает цепочку
  ServiceLoader (coroutines-test → DefaultImpls → LazyKt — кротовая нора,
  4 цикла CI впустую). Вердикт: инструментальные тесты не гонять;
  удаление mockito.android/coroutines-test из androidTest — оставить
  (неиспользуемые зависимости); метод baseline — Tool Tester UI;
  (ал) uninstall сносит ТОЛЬКО указанный пакет: перед переустановкой
  сносить `$PKG` И `$PKG.test`, иначе старый test-APK + сигнатурный
  конфликт (SecurityException) и ложные краши;
  (ам) подписи: оба APK должны быть из ОДНОЙ сборки (BOTH:APP_AND_TEST);
  эфемерный debug-ключ новый каждый ран — APK из разных ранов вместе
  не ставить;
  (ан) walker: `uaw_find` всегда exit 0 (пусто = нет) — проверять строку,
  не $?; новый `MAYBE_TAP` только для genuinely-optional UI (пустой чат);
  EXPECT только топико-уникальными заголовками (совпадение с титулом
  экрана = ложный PASS); после TAP_AT 49 110 доказывать drawer
  (`EXPECT User Manual`); SCROLL_TO UP — вниз по списку, DOWN — вверх;
  `TAP ?` по тексту бьёт в IconButton аппбара (671,110);
  (ао) промахнувшийся тап = ложные замеры времени: координаты кнопок
  брать свежим дампом перед тап-аутом (Retest уехал с 957 на 1152);
  тайминг — только по меткам logcat (`-v time`), не по wall-обёрткам;
  (ап) OCR-baseline метод (11.10): Tool Tester → OCR Read (read_file без
  intent → OCRUtils LOW) + подмена `/sdcard/Download/Operit/test/
  test_image.png` по кругу; картинки в `/tmp/opencode/ocr_base/`,
  результаты в `/tmp/opencode/ocr_results/`; русские картинки — мусор
  (латинский движок, ожидаемо, SIZE-002);
  (ар) drawer без текстовых нод у части пунктов: вести по видимым
  лейблам (AI Chat/Memory Base/...), экраны без пунктов (Terminal =
  карточка Command Terminal, Tool Permission Settings = строка Settings)
  — маршруты в `screen_tour.uaw`; старт тура — BACKи до чата
  (стек после прошлых прогонов непредсказуем);
  (ас) debug applicationId = `com.ai.assistance.operit.debug` (не голый!);
  перед install сносить ОБА (`...debug` + `...debug.test`), иначе
  INSTALL_FAILED_UPDATE_INCOMPATIBLE от чужой эфемерной подписи;
  (ат) онбординг 13.10: `Check Permission Status` только рефрешит;
  continue-ряд (`Select Permission Level` + `Continue`) НИЖЕ фолда —
  свайп вверх; ряды без текстовых нод — бить по координатам
  clickable-боксов; `input text` печатает в СФОКУСИРОВАННОЕ поле —
  после промаха фокуса текст уходит в поиск/не туда (чинить DELами,
  курсор в конец тапом по правому краю);
  (ау) чужой Default Config на лаб-телефоне не ломать: приёмку нового
  провайдера делать выбором в редакторе, затем ВОЗВРАЩАТЬ
  provider/endpoint/model как было (ключ при переключении цел,
  сейв виден в logcat `保存API设置`); проверять сейв-строку;
  (аф) Tool Tester: тап по ряду только ОТКРЫВАЕТ диалог, прогон — кнопка
  Retest; результат может не влезть — смотреть Detailed Result;
  (ах) R8 и JNI: ctor, который зовёт только натив (OrtException(int,
  String) из libonnxruntime), для R8 мёртв — keep обязателен, иначе
  managed-ошибка превращается в нативный abort (WIN DEATH без FATAL);
  (ац) native crash без FATAL: искать `F/<pkg>` + `WIN DEATH` в logcat,
  первопричина — в первых строках (здесь: JNI NewObjectV +
  NoSuchMethodError из-за R8 + старый ORT против IR-10 модели);
  (ач) .so-археология — только по evidence: `__sfp_handle_exceptions` нет ни в
  одном libc++ на устройстве (проверено побайтово в dynsym) — провайдер
  только система/новый NDK; era-match проверять скриптом UND-vs-экспорт
  до заливки, не гаданием; `cd X && A & B &` — && вяжет только A,
  B/C бегут в старом cwd (бинарники в корне репо 13.10, чинилось
  amend'ом).

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
  6. Terminal-env setup (ЗАКРЫТА 11.10 ночью — CLOSE-OUT ниже;
  МЕХАНИЗМ (выяснен чтением кода 11.10, не гадать): красная строка
  «Operit Terminal» = `isOperitTerminalInstalled` (`PermissionLevelCard.kt`
  → `ShizukuDemoScreen.kt:147` → `DemoStateManager.kt:227,460`); флаг
  ставит `DemoStateManager.kt:364-403`: в общей proot-сессии выполняются
  `command -v pnpm`, `command -v python|python3`, `command -v pip|pip3` —
  зелёная ТОЛЬКО когда есть все три (pnpm И python И pip). Проверка бежит
  при каждом входе на Permissions-экран (видно в logcat как
  «NodeJS环境检查 - pnpm: …»).
  ЛОВУШКА ДЕФОЛТОВ (E13, живьём 11.10): в SetupScreen
  (`terminal/.../terminal/ui/SetupScreen.kt:143-207`) пакеты предвыбраны
  ТОЛЬКО если checkPackageInstalled нашёл их (INSTALLED → selected=true,
  и такие потом пропускаются фильтром `!= INSTALLED`); отсутствующие
  НЕ выбраны. Start Setup без ручного выбора = почти no-op (уходят
  только echo-настройки pip-зеркал), экран мгновенно возвращается домой
  с вечной кнопкой Interrupt — выглядит как «установка прошла», на деле
  не качалось ничего (доказано: apt history.log пуст после upgrade,
  в процессах только idle bash, см. E13).
  ПРАВИЛЬНЫЙ ПУТЬ (два входа): (а) Toolbox → Command Terminal →
  выбрать чекбоксы категорий nodejs + python (Select All справа,
  y~509 и ~782 — проверить checked=true в дампе, слепые тапы тогглят!) →
  Start Setup → Notice OK → терминал с Interrupt (качает по-настоящему);
  (б) короче: DEBUGGER-таб → карточка «Configure Terminal Environment»
  (сама пишет чего нет: «Need to configure NodeJS and pip») → Expand →
  «Go to Terminal Configuration» → тот же SetupScreen. Подтверждение
  выбора команд — диалог showSetupDialog (перед отправкой).
  ИСТОРИЯ (как закрыли 11.10): первая «установка» была холостой (E13),
  настоящая с выбранными nodejs+python шла ~40 мин; опрос
  `ci/script/accept_terminal_setup.sh` — раунд 3 «Granted», PASS=4 FAIL=0.
  CLOSE-OUT 6 (11.10 ночь, опрос `accept_term7.log`): раунд 3 —
  «Operit Terminal = [Granted]», PASS=4 FAIL=0. Флаг выставился сам
  по `command -v pnpm+python+pip` — установка с выбранными категориями
  доставила всё недостающее. ЗАКРЫТА.
  DoD: строка зелёная. Запасной DoD (если зелёной не будет): точная
  причина из apt/pty (какой пакет упал и почему) + запись сюда.
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
  (а) файлы в subpack-leaf после РЕАЛЬНОГО экспорта — ДОКАЗАНО 10.10
  (Android; Windows export не гонялся): HTML Packager → ExportPlatformDialog
  → Android export до конца → `files/subpack/apk_editor_android.apk`
  48139093 Б + sha `c56b23a8...` (size+sha сошлись), выходной APK ставится;
  палец не понадобился (sendevent-тапы); подробности — CLOSE-OUT задачи 13;
  (б) 5a/5b глазами — НЕ ДОКАЗАНО, делать так (оба — walker-сценарий +
  файловые пруфы, как seeds): 5a — переезд STT-модели на SD: чистая
  установка (модели НЕТ — качать 225 МБ заново: согласие + докачка,
  только с запасом батареи/сети) → Settings → SpeechToText → переезд на
  memory card → глазами: строка пути + каталог на карте + модель поёт
  (короткая фраза в диктовку); DoD: путь leaf + файлы + живое
  распознавание. 5b — хвосты ANR: `AnrMonitor` пишет в logcat
  «possible ANR!» (`:208`) и «warning» (`:219`); триггер — быстрый
  скролл длинных списков (Settings/Packages) 30–60 с + чтение logcat
  (`logcat -t N`, не полный `-d`); DoD: строки есть/нет + контекст
  (что делали, сколько скроллили) записан сюда — важен сам факт, а не
  «чисто»; (в) задача 6 — ЗАКРЫТА (см. п.6).
  9–18. ЗАКРЫТЫ (тела — в `docs/FORK-REGISTRY/ARCHIVE.md`,
  ниже только доказательства): 9 workspace-чат (поведение апстрима);
  10 UX пустого чата (`f3ce47f1`+`5cb753fe`); 11 тост супервизии (пойман
  живьём 08.10); 12 apktool+desktop удалены и замерены (08.10, CI
  `37722980127`/`37726800230`); 13 subpack on-demand DoD (сборка
  `2026-10-10_00-41-11Z`); 14 онбординг (accept10 на `20261010_215026Z`);
  15 китайские интеграции (accept10 PASS=10); 16 install-size (решением
  пользователя); 17 подпись-evct (`38089152700`, accept10 PASS=10);
  18 B+C идентичность (`20261011_123918Z`, PASS=8; D отложен).

## Статус: окружение

- APK каждой проверенной сборки: `/home/uzzzver/operit-fork/apk/<UTC-дата-старт-CI>/`
  + `CHANGES.md` (3–6 строк дельты). Сейчас там ДВАДЦАТЬ ДВЕ папки:
  `2026-10-04_13-47-09Z` (R8-эксперимент, rejected),
  `2026-10-05_10-51-45Z` (A-сборка, терминал), `2026-10-05_17-52-47Z`
  (llm-io-log, 287387420 байт, CI `37351728353`),
  `2026-10-06_13-03-03Z` (supervision, 287428400 байт, CI `37467783125`)
  `2026-10-07_00-22-30Z` (minify-эксперимент: keep-fix, 247288710 байт,
  CI `37551624980`, cold start clean), `2026-10-07_01-07-20Z` (финал 4+5:
  247288542 байт, CI `37555506347`, ПОЛНАЯ приёмка),
  `2026-10-07_06-31-03Z` (задача 10: 247288246 байт, CI `37581930601`,
  e2e PONG), `2026-10-08_01-06-04Z` (subpack Q1: 247299182 байт,
  CI `37709173132`, пикер+переезд приняты), `2026-10-08_03-29-43Z`
  (12a apktool: 220307769 байт, CI `37722980127`),
  `2026-10-08_04-17-45Z` (12b desktop: 214210621 байт, CI `37726800230`),
  `2026-10-08_11-45-43Z` (Q2 subpack: 177474049 байт, CI `37772193743`),
  `2026-10-09_17-58-51Z` (фикс репака: 177496617 байт, CI `37970071214`),
  `2026-10-10_00-41-11Z` (фикс подписи: 179691879 байт, CI `38010108122`,
  DoD 13 закрыт — ПОСЛЕДНЯЯ подписывающая сборка),
  `20261010_094027Z` (онбординг+модалка маркета: 179672423 байт,
  CI `38042218605`, подпись OK — A/B-доказательство),
  `20261010_132740Z` (15-код: 179388168 байт, CI `38055794385`,
  подпись СЛОМАНА — см. задачу 17),
  `20261010_160304Z` (15-код без вставки-BC: 179389416 байт,
  CI `38066077820`, подпись СЛОМАНА),
  `20261010_170331Z` (вставка+итерация+диагностика: 179391088 байт,
  CI `38070179633`, приёмка accept8 — подпись СЛОМАНА, зато дала
  диагностику PBES2),
  `20261010_203519Z` (та же диагностика, фикс компиляции javax.crypto:
  179392124 байта, CI `38084332791`, приёмка accept9 — подпись СЛОМАНА,
  лог и доказал корень),
  `20261010_215026Z` (evict-фикс 17: 179391284 байта, CI `38089152700`,
  accept10 PASS=10 FAIL=0 — закрыла 14/15/17),
  `20261011_015941Z` (Q2 helper-APK on-demand: 175947764 байта,
  CI `38103675524`, walker-приёмка PASS=6 FAIL=0 — ТЕКУЩАЯ; на телефоне
  стоит она, чат, STANDARD; батарея ночью 8% — телефон спит на зарядке,
  тяжёлую работу (задача 6) только после заряда),
  `20261011_123918Z` (B+C идентичность: 175947900 байт, CI `38140082342`
  зелёный; принят walker'ом PASS=8 FAIL=0),
  `20261011_141729Z` (Q2 seeds: 167739638 байт, CI `38146579269`,
  walker-приёмка PASS=8 FAIL=0 — ТЕКУЩАЯ; на телефоне стоит она).
  Диск `/` хронически 95–100%: `/tmp` чистит агент (протухшие
  `gh-artifact.*.zip`, дубли моделей, свои скриншоты/дампы; тулкит и
  `gigaam-model` не трогать), остальное — пользователь.
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
(TLS-порт после этого отвалился сам — так и надо, лишний).
10.10: автозагрузка РЕШЕНА через Termux + Termux:Boot (оба стоят;
`~/.termux/boot/adb-tcp.sh`: sleep 15 + setprop 5555 + restart adbd;
root для Termux выдан пользователем один раз; оба пакета в battery
whitelist). ДОКАЗАНО перезагрузкой 10.10: 5555 вернулся сам через ~75 с,
uptime 1 мин. Magisk `/data/adb/service.d` был тупиком (не пишется даже
root — режет прошивка). Локальный трюк 09.10:
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
10.10: GitHub сам отключил Actions на форке (баннер «disabled because of
its Actions usage», dispatch отвечал 422; расписаний у нас нет — сработал
эвристика abuse-контроля на пачке долгих ручных ранов). Лечится ТОЛЬКО
кнопкой подтверждения на вкладке Actions в браузере (владелец),
программно не снимается (API говорит enabled, толку ноль).
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

## Что сделано, чтобы не переделывать — см. ARCHIVE.md

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
- **E12. A/B «094027Z подписывает, 132740Z — нет» (FAIL, артефакт).**
  Гипотеза 10.10: 15-код сломал подпись (до-15 собирает, после — нет).
  Метод: keytool -printcert выходного `WebApp_*.apk` после прогонов на
  обеих сборках. Улики: «подписывающий» вердикт 094027Z получен БЕЗ
  сверки имени/времени выходного файла — а `/sdcard/Download/Operit/
  exports/` ПЕРЕЖИВАЕТ переустановки (memo (т)): keytool почти наверняка
  проверил НЕСВЕЖИЙ подписанный файл прошлой эпохи и приписал успех
  текущей сборке. Контрольный: ни один чистый прогон 094027Z с проверкой
  именно свежего файла не проводился. Вердикт: A/B недействителен, 15-код
  невиновен (доказано иначе — задачей 17: корень в `a2764e9c`, до 15-кода).
  Правило: keytool — ТОЛЬКО файлу из текущего прогона (имя из лога +
  ls -la время), иначе ловушка (т).
- **E13. Start Setup с дефолтным выбором (FAIL, тихий no-op).** Гипотеза
  11.10: «нажал Start Setup — значит, ставится» (терминал показывал
  Interrupt, казалось — качает). Метод: после тапа — apt history.log +
  `su -c ps` + побайтовый ls rootfs + лог `NodeJS环境检查`. Улики:
  history.log кончается на `apt upgrade` 16:44 UTC (ещё до тапа —
  это setup-check при ОТКРЫТИИ экрана); после тапа ни одной apt-строки;
  в процессах только idle bash (apt/curl/node отсутствуют); лог проверок
  20:13–22:30 стабильно `pnpm:false, python:true(предустановлен),
  pip:false`. Причина в коде (`SetupScreen.kt:143-207`): selected=true
  ставится ТОЛЬКО найденным пакетам, а найденные пропускаются фильтром
  `!= INSTALLED` — с дефолтом в команды уходят лишь echo зеркал pip.
  Экран при этом мгновенно возвращается домой с вечным Interrupt (флага
  завершения нет — fire-and-forget `sendCommand`), неотличимо от успеха.
  Вердикт: без ручного выбора чекбоксов (проверять checked=true в дампе —
  слепые тапы тогглят!) setup — no-op; чинить выбором nodejs+python
  категорий + реальным прогоном (задача 6).
- **E14. JKS-keystore на Android (FAIL, платформы).** Гипотеза: «упал
  PKCS12 — попробуем JKS». Метод: итерация `loadVerifiedKeystore` по
  обоим типам + `Security.getProviders("KeyStore.JKS")` в живом процессе.
  Улики: JKS не предлагает НИКТО (`offered by: null`, old-style «JKS not
  found») — на Android этого провайдера нет в принципе, это норма, а не
  ошибка. Вердикт: чинить только PKCS12-путь; JKS-ветку в коде оставить
  как есть (не удалять — она документирует отсутствие), время не жечь.
- **E15. «Вставка bundled-BC работает» (FAIL, допущение).** Гипотеза
  (`a2764e9c`, 10.10): достаточно вставить bundled-BC первым, платформу
  не трогать. Метод: лог return-кода вставки в живом процессе.
  Улики: `insertProviderAt` → `-1` дважды (20:40, 00:36): пока платформа
  держит имя «BC», вставка отклоняется — код работал на чистом
  платформенном BC, у которого нет PBES2 (доказано прямым пробом
  SecretKeyFactory + `openssl pkcs12 -info` файла: PBES2/AES-256).
  Вердикт: исход вставки ВСЕГДА писать в лог + сверять класс в слоте
  (`Security.getProvider("BC").javaClass`); занятое имя — только через
  remove+insert (upstream-механика + keep-классов, задача 17).

## Открытые вопросы 1–2 — закрыты, история в ARCHIVE.md
  (актуально: `docs/FORK-REGISTRY/research-size.md`).

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

## Истории приёмок T1–R8, CTC, deferred/dropped — см. ARCHIVE.md