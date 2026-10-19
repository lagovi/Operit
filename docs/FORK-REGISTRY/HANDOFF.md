# Letter to myself in the next session

> Entry point: start with this file —
> `docs/FORK-REGISTRY/HANDOFF.md` (repo root `/home/uzzzver/operit-fork/Operit`).
> Read top to bottom, then follow the reading-order table below.

**From:** the agent building the English-only Operit fork
**To:** me, fresh context, same job
**Date:** 2026-10-16 (HANDOFF self-revision: rakes, task clarity,
negative detail), HEAD `f4009f75`,
branch `feat/english-only-build`

> **Language rule (user directive, 15.10): talk to the user in Russian.
> All agent-internal docs are in English (token efficiency). Code comments,
> logs and docs in English; user-visible strings English-only (the fork
> is English-only).**

Stack since 08.10: ports batch-1+2 + Antigravity `92b4cc63` + PaddleOCR
`96e27adb` + proxy `2c245bf4`+`bd47afae` + `transcribe_file` `17007bad` +
Parakeet `82bac318` + fixes `f0ad404a`/`5c4c627e` + docs; details —
PORT_QUEUE.md, APK dirs `20261012_104102Z`/`20261013_025022Z`/
`20261014_025827Z`/`20261014_062241Z`/`20261015_194503Z`/
`20261016_034748Z`/`20261016_085455Z`/`20261016_104352Z` (CURRENT, on
phone now).
(Code: `1c0ed81d` llm-io-log + `63dadf6e` supervision + fixes +
`b4ff4a30` QuickJS keep + `c8bda3c4`/`acc9eb3a` 5a/5b + `50dc014a`
R8-mapping + `f3ce47f1`/`5cb753fe` task 10 + `cddb9906` revert +
`97ad4913`/`a112091f`/`55e18396` tests (Android Tests green) +
`a4fddb40` subpack Q1 + submodule `terminal@9d01a2e` (English-only
re-applied 18.10, see N65; rootfs from the host provider, see N69);
docs-only commits on top.)

OPEN (full breakdown in §8; summary here):
(a) 5a/5b — CLOSED 15.10 night by the agent alone via root (decision
15.10; close-outs in §8); Windows export blocked — no Windows machine;
(b) joint manual testing with the user — NO checklist exists and none
is owed right now (clarified with the user 16.10: the phrase meant
"poke at the polished whole when the agent says ready" + far-field STT
quality judgment ONLY if the user complains about dictation). What the
user owes at this moment: NOTHING. Later, on call: Antigravity sign-in
(c) + exploratory testing of a build the agent declares ready. Agent
part: keep this list truthful; say "ready" only after a green
CI + install + wizard + smoke (chat opens, no FATAL).
(c) manual Google sign-in to Antigravity — user will do it later
(what to verify after — PORT_QUEUE.md, Antigravity acceptance);
(d) STT regression after ORT 1.17.1→1.21.1 — DONE 16.10 night (3/7
absolute reference, RTF 1.12, close-out in §8);
(e) Parakeet preset — DONE 16.10 night (int8, full preset, matrix:
GigaAM 3/7+3/7, Parakeet chunk 4/7, EN perfect, whole-RU OOM → 20 s
guard; close-out in §8);
(f) proxy tails — CLOSED 16.10 (`6594bc2c`, user directive: when a
proxy is configured, ALL provider traffic goes through it). Token
exchange/refresh, Antigravity email/project discovery/onboarding,
model-list (all three fetchers), quota/usage, Codex token revocation
and both login dialogs now honor the config proxy; providers carry it
from the factory. Only the OAuth loopback callback servers stay direct
(phone-local listeners, not provider traffic). Lab proxies
.55:18080/10808 block LAN — end-to-end only via our own stand
(restore recipe — memo (N35)).
  CLOSED 11–12.10: 19 (queue `PORT_QUEUE.md`: batch-1 + 17 manual/
  cherry-pick ports; CIs green), 20 (help: 12 offline topics + `?` in the
  appbar; tour `screen_tour.uaw` 102 steps PASS on `20261012_104102Z`),
  21 (OCR baseline: ML Kit Latin 11.5 MB, 5/5 exact + Russian garbage
  (expected); engine 1–5 s).
  PADDLEOCR VERDICT 12.10 (historical, not a plan): PP-OCRv6 has an
  official Android SDK (`deploy/ppocr-android`, ONNX Runtime, minSdk 26).
  Agent recommended AGAINST; user 12.10 decided: TAKE (Russian).
  Status 14.10: DONE (see PORT_QUEUE.md).
  CLOSED night 11.10: 6 (line green, poll PASS=4 FAIL=0 — close-out in
  body), 18 (B+C PASS=8; D dropped 15.10 — forget it).
  12 (done inside Q2, see below), 13 (Q2 CLOSED WHOLE 11.10: subpack +
  helper APK + templates/emoji; rootfs NOT taken — E3 stands).
  CLOSED night 10.10: 14 (close-out in body), 15 (full `accept10`
  PASS=10 FAIL=0 on `20261010_215026Z`), 17 (PBES2 root + evict fix).
  Q2 FOLLOW-UP 10–11.10 (details — ARCHIVE.md): helper APK on-demand
  (`HelperApkStorage.kt`, release, `git rm` 4 assets; APK
  `20261011_015941Z`, CI `38103675524`, −3443520 B, walker PASS=6 FAIL=0;
  walker picked up 4 bugs en route: fd3-stdin, bare-bounds, WAIT_PKG,
  SCROLL_TO — all in memo); seeds templates/emoji (`AssetSeedStorage.kt`,
  `seed-templates.zip` 4227058 + `seed-emoji.zip` 3614642, `git rm` trees;
  APK `20261011_141729Z`, CI `38146579269`, −8208126 B, PASS=8 FAIL=0).
  Q2 CLOSED whole.
  12 (a/b apktool+desktop — CLOSED 08.10, CIs `37722980127`/`37726800230`),
  13 (subpack DoD CLOSED 10.10 on `2026-10-10_00-41-11Z`).
CLOSED: everything else (1–5 except behavioral 5a/5b — open, see §8;
7a, 9, 10, 11, 3a/3b, 4, subpack Q1 mechanics, 16 install-size —
don't touch, decision 10.10).
  Standing user decisions: no keystore (life on GitHub, debug builds are
  enough); Q2 hosting — GitHub Releases in the same repo, first release
  together with Q2 code; manual testing — later, when everything polished.
**How to read:** the status below is truth-as-of-now, read first. Sections
marked SUPERSEDED/CLOSED are history, don't touch. Verify code-commit
hashes against `git log` — docs commits change no behavior.

## Fresh-session memo (read second — everything to not step on again)

** Link.** Only `adb -s 192.168.1.69:5555` (classic ADB-TCP, raised by root
`su -c 'setprop service.adb.tcp.port 5555; stop adbd; start adbd'`).
Survives reboots: Termux + Termux:Boot run the same script
(`~/.termux/boot/adb-tcp.sh`, root granted once) — proven by 10.10 reboot
test (port back after ~75 s). USB dead (`lsusb` empty). `device not
found` after idleness is normal — first `adb connect 192.168.1.69:5555`,
it reconnects (16.10). Port open and pingable but `adb connect` hangs —
`adb kill-server; adb start-server` (stale server, 09.10). Pairing port ≠ adb port: connect to pairing gives
refused/hang — ask the user for the real one or take 5555. The wireless-
debugging TLS port is dead since 5555 was enabled (as intended).
**(N1) Root.** Magisk `su` works (`su -c id` → uid=0), incl. from Termux
(`run-as com.termux su -c id`). BUT: `/data/adb/service.d` is unwritable
even as root with full capabilities, in Permissive and via `su -mm` —
firmware cuts it; don't fix Magisk autostart, the Termux:Boot bypass above
is the way.
**(N2) Fresh-install grants** (shell set, sufficient): `pm grant PKG
READ/WRITE_EXTERNAL_STORAGE + FINE/COARSE_LOCATION + RECORD_AUDIO`,
`appops set PKG MANAGE_EXTERNAL_STORAGE + SYSTEM_ALERT_WINDOW allow`,
`dumpsys deviceidle whitelist +PKG`. Without them the wizard loops
"Incomplete → Continue" — not a bug.
**(N3) Taps.** Main tool — root sendevent taps: `/dev/input/event3`
(`ILITEK_TDDI`, MT-B protocol), coordinates 1:1 with dump coordinates
(proven, incl. y=1394–1407). Tap recipe: slot 0 → BTN_TOUCH(330) 1 →
tracking_id N++ → X(53) → Y(54) → major(48)=9 → pressure(58)=50 → SYN →
pause 0.12 → tracking −1 → BTN_TOUCH 0 → SYN. Ready wrapper:
`ci/script/phone_lab.sh` (`plab_tap`, `plab_tap_text`, `plab_wait_text`,
`plab_assert_leaf_file`). `input tap` works INSIDE the app but is DEAF in
system DocumentsUI (folder rows and USE button — proven by combing
single/double/long taps) — sendevent only there (no finger needed at all).
Keyboard: TAB walks focus (incl. list rows after a tap), DPAD_UP/DOWN —
rows, ENTER presses normal buttons, BUT on DocumentsUI USE/rows
ENTER/SPACE/CENTER are deaf (ENTER on USE steals focus).
**(N4) Screen reading.** `uiautomator dump` + XML parsing (texts/bounds) is
primary; screenshots (`screencap -p` + Read) — visuals only. Dump dying
with SIGKILL — fall back to screenshots. Bottom drift (E11): hitboxes
40–100px ABOVE glyphs — on misses comb upward, don't hammer one spot.
**(N5) Files on the phone.** Only `adb exec-out run-as PKG` (no other way
in); pull DB as three files (`app_database`+`-wal`+`-shm`); `input text`
— only `%s`-safe strings; `/sdcard/Download` (shared) survives
reinstalls, but `Android/data/<pkg>` on the card does NOT (wiped on
uninstall — E26: models must be re-downloaded after every install, always
re-verify with `ls`). The app uid changes per install (`u0_a286` →
`u0_a287` → `u0_a288` observed): for file ops use `run-as`, never
root `cp`/`chown`. Root-owned adb pushes read fine through FUSE anyway
(`chmod` display on sdcardfs lies, ignore it).
**(N6) CI/builds.** Workflow is manual-dispatch ONLY and ONLY AFTER PUSH
(runner checks out origin). Every CI build = new debug key: `uninstall`
before every install (`-r` gives UPDATE_INCOMPATIBLE). APK dir =
UTC run start + `CHANGES.md` (3–6 delta lines). Measure with
`python3 zipfile`, NOT `unzip -l` (lies). Always pull the mapping
artifact. 10.10 GitHub itself disabled Actions on the fork (banner
"disabled because of its Actions usage", dispatch 422; we have no
schedules — abuse-control heuristic on a pile of long runs): fixed ONLY
by the owner button on the Actions tab, API/toggles don't help.
**(N7) Disk `/` chronically 95–100%.** `df` before pulling artifacts;
stale `/tmp/gh-artifact.*.zip` and duplicate models go first; toolkit
(`jdk/kotlinc/venv`) and `gigaam-model` are untouchable.
**(N8) Rules.** No-fallback: fix the true cause (examples: keep
commons-compress instead of try/catch around setTime; keep BC + don't
delete the platform provider instead of swallowing exceptions). Commits:
code and docs separately; audit gate
(`fork_audit.py --no-color --no-hotspots`, CJK-NEW 0) before push.
**(N9) Negative experiments (verified, DO NOT repeat):**
(a) adb taps on DocumentsUI — deaf (see N3); (b) Magisk service.d —
unwritable (see N1); (c) `svc wifi disable` for offline tests KILLS the
ADB transport (link is WiFi-only!): offline = only iptables owner-match
on the app uid (`iptables -A OUTPUT -m owner --uid-owner <uid> -j REJECT`,
remove with `-D`); (d) `install -r` across CI builds —
UPDATE_INCOMPATIBLE (keys differ); (e) BACK from picker/screens loses
remember-state (re-pick folder/flow; the system URI grant stays alive);
(f) JKS keystore on Android does NOT exist, period (no provider, never
will be — fix only the PKCS12 path); (g) `removeProvider("BC")` outside
an immediate re-insert of bundled-BC in its place is FORBIDDEN (bare
removal left the process with no PKCS12 at all, live-fire 09.10); the
remove+insert pair itself is the working mechanism (upstream, task 17).
**(N10) `pkill -f <pattern>` kills your OWN tool shell** (pattern matches
the command line) — find PIDs via `ps`, `kill <pid>`. SAME for
`ps aux | grep <pattern> | xargs kill`: if the pattern string is in your
own command (script names!), grep sees the wrapper and suicides (caught
twice — silent suicide, no output); kill only by PID from a file written
at launch.
**(N11) No `unzip` on the box at all**: everything via `python3 zipfile`;
code lives in `classesN.dex`, UI strings in resources.arsc (their absence
in dex is normal, not "stripped"); short substrings (`inserted`,
`offered`) in dex-grep are noise — search long exact phrases only.
**(N12) `sed 'A,Bd'` + line-number edits**: numbers drift after deletions
— verify bounds by reading; fix dangling `"""`, stray `\`, eaten closers
by hand + verify with python parsing; one-line python JSON edits: check
TYPE before writing (I once replaced cjk-allowlist dict with a key list
— saved by `git checkout`, redid); `grep -mN` cuts matches —
"no lines" must be rechecked without `-m`.
**(N13) `adb install` of 179 MB flaps transiently** — script has the cause
+ retry, success only on `Success`; phone SLEEPS during waits (empty
dumps): `plab_wake` + `screen_off_timeout 1800000` with restore-trap on
EXIT; empty dump ≠ empty screen — wake first; modals on top
(Announcement/Got it) steal taps — dismiss cycles before each nav step;
clean install does NOT touch `/sdcard`: other people's `WebApp_*.apk` survive runs
— match name/time/signature, don't grab the first one; `gh workflow run
--ref` checks out HEAD at JOB START — verify `headSha`
(`gh run view --json`), or you test the wrong code; APK dir name =
run UTC `createdAt`, not download time; long `adb` calls need `timeout`
(install 600s), all of `_adb` in `timeout 90` (transport died with
`error: closed` and hung silently); background runs — nohup+log only,
poll in ≤10 min chunks, NEVER a 25-min blocking call (the user will cut
it and be right); typo in `rm -rf` nearly wiped `assets/models` (saved
by `.keep` via `git checkout`) — verify rm paths with `git status`
IMMEDIATELY; `if (false)` and stubs forbidden even temporarily — delete
the block whole; EXIT trap doesn't fire on `kill -9`: if the screen
stops dimming — check `settings get system screen_off_timeout` and
restore by hand.
**(N14) UI walker (10.10 night, no more hand taps — unreproducible):**
engine `ci/script/uawalk.sh` + scenarios `ci/script/scen/*.uaw`
(commands TAP/TAP_AT/EXPECT/WAIT_PKG/TYPE/BACK/HIDEKB/SWIPE_UP/DOWN/
SCROLL_TO/SLEEP/DUMP; `uaw_elements` — element inventory for authoring);
every step declares tap+expectation, mismatch = ABORT with step number,
command, expectation, real screen (package+texts) and dump path
(`/tmp/uaw/dump_stepN.xml`). `adb shell` EATS stdin: read the scenario
from fd 3 only, or step 1 runs and then silent end (caught live).
System installer speaks locale language: track screens by WAIT_PKG
(`packageinstaller`) only, texts are locale-dependent;
`appops set REQUEST_INSTALL_PACKAGES allow` goes into acceptance setup
or the installer stalls on a dialog. Icons without text nodes (hamburger,
FAB, chevron) — TAP_AT by dump coordinates only; TAP needs EXACT text
match (EXPECT is substring: label "1. Install Provider App" masks a
missing button — expand the step first). Scroll position after BACK is
unpredictable — SCROLL_TO instead of blind swipe; drop the keyboard
before Confirm with HIDEKB (BACK only if `mInputShown=true`, or it kills
the dialog). Pass bare numbers to size asserts (the function pads spaces
itself — " 2571773 " triple-spaces into a mismatch on a correct file).
Chat screen anchor = "Enter question" (the input), NOT "New Chat": a chat
with history/bound workspace has no New Chat button in texts, 18 BACKs
walked past it (E-lesson; walker scenario start EXPECTs — by input too).
SetupScreen of the terminal module: default selection = silent no-op
(E13) — verify checked=true in dumps, blind taps toggle; eternal
Interrupt does NOT mean "downloading" (fire-and-forget with no callback)
— check `apt history.log` + `su -c ps` (root sees all); short path to
env setup: DEBUGGER → "Configure Terminal Environment" (it writes what's
missing itself) → Expand → "Go to Terminal Configuration". BACK stack:
a bound workspace catches BACK (Preview screen) — exit via toggle
671 110; monkey doesn't go home, it resumes the top of the stack
(poll loops account for that). proot-install forensics: `history.log`
(what apt installed), `su -c ps` (what really runs),
`NodeJS环境检查` in operit.log (pnpm/python/pip by name every
Permissions entry).
**(N15) Long-job protocol (11.10, `phone_lab.sh`):** `plab_status_init/
status/final` write `<epoch> START/STEP/FINAL` to a status file;
`plab_eta` computes pace and ETA; `plab_wait_final <file> <timeout>`
sleeps in 30 s chunks and returns EARLY on FINAL — no more blind sleeps
(ETA → wait_final → tail file); `_adb` retries and heals stale servers
itself (kill/start/connect), the caller sees an error only when the
phone is truly unreachable.
**(N16) CI:** `concurrency cancel-in-progress: true` — two dispatches to
one branch KILL each other (lost build 38198538271). Serial only:
dispatch → green/red → next. headSha check after each.
**(N17) Instrumentation under minified debug:** R8 cuts the ServiceLoader
chain (coroutines-test → DefaultImpls → LazyKt — rabbit hole, 4 CI cycles
wasted). Verdict: don't run instrumented tests; removing
mockito.android/coroutines-test from androidTest stays (unused deps);
baseline method = Tool Tester UI. `uninstall` removes ONLY the named
package: before reinstall remove `$PKG` AND `$PKG.test`, or stale
test-APK + signature conflict (SecurityException) and false crashes.
Signatures: both APKs must come from ONE build (BOTH:APP_AND_TEST);
the ephemeral debug key is new every run — APKs from different runs
don't install together. Walker: `uaw_find` always exits 0 (empty = none)
— check the string, not `$?`; new `MAYBE_TAP` only for genuinely-optional
UI (empty chat); EXPECT only topic-unique headers (match with screen
title = false PASS); after TAP_AT 49 110 prove the drawer
(`EXPECT User Manual`); SCROLL_TO UP = down the list, DOWN = up; `TAP ?`
by text hits the appbar IconButton (671,110).
**(N18) Missed taps = false timings:** take button coordinates from a
fresh dump before the tap-out (Retest moved 957→1152); timing only from
logcat stamps (`-v time`), not wall wrappers.
**(N19) OCR baseline method (11.10):** Tool Tester → OCR Read (read_file
without intent → OCRUtils LOW) + rotating `/sdcard/Download/Operit/test/
test_image.png`; pictures in `/tmp/opencode/ocr_base/`, results in
`/tmp/opencode/ocr_results/`; Russian pictures = garbage (Latin engine,
expected, SIZE-002).
**(N20) Drawer has no text nodes on some items:** navigate by visible
labels (AI Chat/Memory Base/...); screens without items (Terminal =
Command Terminal card, Tool Permission Settings = Settings row) —
routes in `screen_tour.uaw`; tour start = BACKs to chat (stack after
previous runs is unpredictable).
**(N21) debug applicationId = `com.ai.assistance.operit.debug`** (not
bare!); before install remove BOTH (`...debug` + `...debug.test`), or
INSTALL_FAILED_UPDATE_INCOMPATIBLE from someone else's ephemeral
signature.
**(N22) Onboarding 13.10:** `Check Permission Status` only refreshes;
the continue row (`Select Permission Level` + `Continue`) is BELOW the
fold — swipe up; rows without text nodes — hit clickable-box coordinates;
`input text` types into the FOCUSED field — after a focus miss the text
goes to search/elsewhere (fix with DELs, cursor to end via right-edge
tap).
**(N23) Don't break other people's Default Config** on the lab phone:
accept a new provider by selecting in the editor, then RESTORE
provider/endpoint/model as-was (key survives switching intact, save
visible in logcat `保存API设置`); verify the save line.
**(N24) Tool Tester:** tapping a row only OPENS the dialog, the run is
the Retest button; results may overflow — read Detailed Result.
**(N25) R8 vs JNI:** a ctor called only from native
(OrtException(int, String) from libonnxruntime) is dead to R8 — keep is
mandatory, or a managed error becomes a native abort (WIN DEATH, no
FATAL).
**(N26) Native crash without FATAL:** look for `F/<pkg>` + `WIN DEATH`
in logcat; root cause is in the first lines (here: JNI NewObjectV +
NoSuchMethodError from R8 + old ORT vs IR-10 model).
**(N27) .so archaeology — evidence only:** `__sfp_handle_exceptions`
exists in NO libc++ on the device (verified byte-wise in dynsym) —
provider is system/new NDK only; era-match new .so with a UND-vs-export
script BEFORE shipping, not by guessing; `cd X && A & B &` — && binds
only A, B/C run in the old cwd (binaries in repo root 13.10, fixed by
amend).
**(N28) .so forensics in detail (14.10, PaddleOCR):** the box has NO
readelf/patchelf/unzip/objdump — parse everything by hand in python
(ELF64: e_phoff/e_phnum → PT_DYNAMIC → DT_STRTAB/SYMTAB/SYMENT/HASH →
nchain symbols; SONAME/DT_NEEDED/UND-vs-export were verified this way).
Era-match recipe for a new .so BEFORE shipping: parse its UND symbols
and diff against our APK libc++ export (take from built APK:
`lib/arm64-v8a/libc++_shared.so`); norm — 0 missing C++-like except
`__cxa_atexit/__cxa_finalize` (bionic provides those, not libc++).
Proven that way: quickbird 4.5.3 imports `__sfp_handle_exceptions`
(UND FUNC GLOBAL) and NOBODY exports it (full dynsym scan of both
AAR libc++ and phone `libc++.so`/`libc.so` — the string is only in
.comment, not dynsym) → conclusion: symbol provider is system/new NDK
only, isolating the importer is pointless (E16). Official 4.5.5: 377
UND, all resolve — taken. `clang version` strings inside .so are dating
garbage (9.0.9 on a fresh build) — trust only import/export pairs.
Model inventory in the same spirit: HF API
`api/models?author=PaddlePaddle&search=<lang>` + `siblings` field
(has inference.onnx/yml?) + `usedStorage` (bytes) + dict from yml
(`PostProcess: → character_dict:`, count coverage with unicodedata:
LATIN/CYRILLIC/Ё). Proven 13.10: v6-rec is en+zh only, Russian maxes
at v5 (cyrillic dict 850: latin 115 + cyrillic 409).
**(N29) OCR acceptance in detail (14.10):** Tool Tester → Tool Test
Center → `OCR Read` row (first tap only OPENS the dialog; the run is
Retest; results — Detailed Result section, `take(1000)` — scroll).
`read_file` on .png takes branch 3 (`StandardFileSystemTools.kt`
~1100–1160: 1 = direct_image link, 2 = intent only, 3 = OCR; test
intent is empty → straight to OCR). First run pulls ~36 MB
(`files/ocr/`: det/rec/yml/so; sha+size checked by `isIntact` — stale
files replace themselves, never clean by hand). Cold 1202 ms, warm runs
~3–4 s (det ~2 s + rec ~1–1.5 s) — stamps in the app log
(`files/logs/operit.log` via `run-as cat`, PaddleOcrEngine tags;
AppLogger.d goes to file AND logcat). Fixtures: `/tmp/opencode/
ocr_base/*.png`, truth in sibling `.txt` (NOT in repo — if lost,
regenerate and LOOK WITH EYES: E18). Before diffing remember dump
HTML-escapes (`&gt;` = `>`).
**(N30) git/shell traps (all live 12–14.10):** `git diff A B C` with three
refs is NOT a B..C diff — it shows B as a merge (for upstream lines give
`git show <commit> -- <file>`); `cd X && A & B &` — && binds only A, B
and C run in the OLD cwd (54+8 MB of binaries landed in repo root);
cleanup — `git rm` + `git commit --amend` (not pushed yet), before every
push `git show --stat HEAD | grep -E 'aar|onnx|\.so'`; `git add -A`
drags stray files — read `git status --short` ALWAYS; `replaceAll` over
JSON/providers — verify the count (`grep -c`); `git checkout <ref> --
<paths>` for new upstream files — then CJK-scan and comment translation
before commit.
**(N31) CI background protocol (worked out 12–14.10, do NOT improvise):**
call 1 — `source phone_lab.sh && plab_status_init <file> <label>`;
call 2 — `setsid nohup bash -c 'poll-loop' & disown` (poller writes
`<time> <status>` every 60 s and `FINAL` at the end; `&` on the WHOLE
`&&`-chain takes source along — so source in a SEPARATE call, or
`plab_wait_final: command not found`); call 3 — same source +
`plab_wait_final <file> <timeout>` (30 s chunks, early-wake on FINAL).
Call timeout = wait timeout (up to 2400000). Status filenames per task
(`pd_ci<N>.status`), delete stale ones.
**(N32) Gestures 14.10:** `input swipe` on the lab phone is DEAD (content
doesn't move, though `input tap/keyevent/text` work) — scroll only with
a sendevent finger (`/tmp/opencode/se_swipe2.py`: slot 0, tracking id,
one adb call per event with 0.12 s pauses; batches in one `sh -c` work
every other time). Side effect: swipes over an OPEN keyboard type garbage
into fields (caught: `⁹` in username, `ýýþþþþ` in proxyHost) — kill the
IME (`input keyevent 4`) before swiping, verify values by eye after.
No-scroll fallback: IME Next chain (`keyevent 66`) jumps forward across
fields with autoscroll; DEL erases only with cursor at END (tap the
right edge; cursor-at-start + DEL = silence).
**(N33) `route=` in AIHttpTrace lies for SOCKS** (shows the TARGET, not
the proxy — `Route.socketAddress` semantics of this OkHttp version) —
nearly sent us on a false "DIRECT-leak" hunt. Decisive method: kill-test
(kill the proxy → the error must name its host:port;
`Failed: failed to connect to /192.168.1.55 (port 11080)` = traffic goes
via proxy, period). Positive proof = the proxy's own log (CONNECT
host:port), not route=.
**(N34) Three 14.10 rakes:** ① `pkill -f <path>` kills YOUR OWN shell if
the path literal is in the same command line (kill by PID via a separate
`pgrep`); ② DataStore JSON is written with `encodeDefaults=false`:
DIRECT/default values are ABSENT from the file (not a bug — read absence
as default); ③ the JDK SOCKS client always offers methods {0x00, 0x02}
— a test proxy must pick 0x00 when offered, or it breaks no-auth clients
(`SOCKS : authentication failed`).
**(N35) Proxy stand 14.10:** lab proxies 192.168.1.55:18080 (HTTP) and
:10808 (SOCKS5) block LAN targets by policy — proven by curl from the
box: outside 200, to gateway :20128 — empty reply/000 (E22). Notebook is
.55 itself, phone on the same LAN — end-to-end ran via our own permissive
forwarders (HTTP-forward of absolute-URI + SOCKS5 RFC1928/1929 with a
REQUIRE_AUTH flag for the auth test). The stand lived in
`/tmp/opencode/testproxy/` (NOT in repo, by decision) — /tmp survival
across box reboots is NOT guaranteed: before the next proxy acceptance
check the ports, if lost rewrite in 15 minutes (gist above; CONNECT /
auth-ok logs are the positive routing proof).
**(N41) Does not exist — number retired.** Old revisions referenced
"memo (N41)" for the proxy-stand restore recipe; that recipe always
lived here at (N35). The pointer is fixed; the number stays empty so
old commit messages don't collide with a new item.
**(N36) Dump-driven taps (primary method since 16.10).** Recipe that
replaced all coordinate guessing: `uiautomator dump /sdcard/ui.xml` →
pull → python `ElementTree` → find node by exact text → tap the CENTER
of its bounds → sleep → re-dump → verify the new screen. One-liner
pattern per step (dump+pull+parse in one bash call, tap in the next).
Pointer_location is only for one-off drift calibration, DISABLE right
after (`settings put system pointer_location 0`) — it overlays the
screen and spams toasts. Row positions shift after every swipe and
dialog buttons move per dialog: a tap that worked a minute ago can land
in a neighboring row (Functional instead of Speech, Add-from-link
instead of Download — both caught live 15–16.10).
**(N37) Tool Tester heavy rows (16.10).** All five `transcribe_file`
rows share the tool id: the results map is keyed by id, so run rows ONE
AT A TIME and read the dialog right after its own run. NEVER "Test all"
with model rows: the file group runs rows IN PARALLEL and parallel
225/670 MB decodes will OOM the 2.8 GB phone. Fixed row paths: the wav
must be at the row's path — to run other content, `cp` it over
`test_audio.wav` (backup first, e.g. `test_audio_ru.wav`) and restore
after; reinstalls never touch `/sdcard/Download`, so files persist.
Transcribe timing in `operit.log` INCLUDES the model open (652 MB load
~20 s): short-file RTF is load-dominated, say so when quoting.
**(N38) ORT dtype traps (16.10, caught by box validation first).** The
Parakeet joint wants `targets`/`target_length` as INT32 (`IntBuffer`),
not int64 — ORT throws `InvalidArgument` otherwise. Encoder
`audio_signal` float `[1,128,T]` + `length` int64 is fine. Validate any
new ORT graph on the box BEFORE the CI cycle: `/tmp/opencode/venv`
has numpy+onnx+onnxruntime (box python is PEP 668
externally-managed — never `pip install` into system). Validation
scratch lives in `/tmp/opencode/`, NEVER in the repo.
**(N39) Kotlin `.format` binds to ONE literal (16.10).** `"a %d " +
"b".format(x)` formats only `"b"` — placeholders in the first literal
stay raw (shipped once, caught on device: guard message showed
`%.1f`). Parenthesize the concatenation first, or use templates.
**(N40) NeMo frontend numbers (16.10, all verified against onnx-asr).**
Encoder output is `[1,1024,F]` — transpose defensively, the code does.
Vocab decode must mirror the reference exactly (join pieces, trim,
collapse spaces — NO special-token filtering, or device text diverges
from validation). Preemph keeps sample 0 (`y[0]=x[0]`, the ONNX
variant, not NumPy's `0.03*x[0]` — one sample, but match the shipped
preprocessor). The 128-mel bank is GENERATED by the published
`melscale_fbanks` slaney/slaney formula at runtime — `fbanks.npz` is
build-generated, NOT in git (GitHub contents API 404s on the data dir;
don't hunt it).
**(N49) Box web/research traps (16.10).** HF `resolve/main/<file>`
needs the EXACT filename — guessed `test_wavs/0.wav` 404d, real is
`en.wav` (list via the `/api/models/<repo>/tree/main?recursive=true`
endpoint first). `curl` works for HF/GitHub raw; GitHub code search
needs auth (don't bother). `WebFetch` of huge generated sources is
fine (nemo.py, asr.py, preprocessors) — read the decode loop from the
reference, don't reconstruct from memory (the blank-state-discard rule
was the subtle part).
**(N42) `gh` CLI shapes that work (16.10).** Artifact names DRIFT per
run (`operit-android-99` → `-101/-102/-103`): always list first via
`gh api repos/.../actions/runs/<run>/artifacts --jq
'.artifacts[].name'`. Multi-field `--json a,b --jq '{.a} {.b}'` is
INVALID jq — use one `--json X --jq .X` call per field. The N31
poller template works only with the single-field form inside the loop.
**(N43) Silent phone death diagnostics (16.10, E25).** LMK kill
signature: launcher on screen + `ps` shows 0 + NO dropbox entry + app
log ends mid-run. Not a crash — don't hunt FATAL. Confirm with
`monkey -p PKG ...` + `ps | grep -c` (1 = alive). logcat rotates:
`-t 200` is useless 25 min later — use `-t 2000` or better the
persistent `files/logs/operit.log` via `run-as cat`. Transcribe rows
log one line per run (`transcribed %s (%s/%s): %d non-blank of %d
chunks, %.1f s audio in %d ms`) — grep it instead of re-opening the
dialog.
**(N44) Guard behavior (16.10).** Parakeet whole mode over 20 s throws
loudly → Tool Tester shows FAILED with the message as content
(`runTest` maps any exception to FAILED). A guard or error path is
verified by: FAILED text on screen + `ps` alive + no new model output.
**(N45) Fresh-install wizard on 16.10 builds.** Same flow as the cheat
sheet, but drive it with dump bounds (Check / Standard radio /
Continue all found by text). Grants via the full shell set BEFORE
monkey-launch; all 4 ✓ on first Check. No announcement modal on recent
builds — don't wait for one, go straight to chat.
**(N46) Disk triage (16.10, `/` 99%).** Mine to delete freely:
/tmp status files, poll logs, validation scripts, downloaded
scratch `.onnx`, `/tmp/opencode/para_apk*`, `pd_apk*`, seed zips.
NEVER touch: `~/operit-fork/apk/` archive (~1.7 GB, user's), toolkit
(`jdk/kotlinc/venv`), `gigaam-model`. Mapping artifact is REQUIRED
per build (rule) — when the disk refuses the unzip, clean MY scratch
first, then escalate to the user, never the archive.
**(N47) Phone truth (16.10 close).** Build `20261016_104352Z`
(`lastUpdateTime` 14:13); location SD; Parakeet 670 MB on SD
downloaded; GigaAM NOT downloaded (wiped by the last reinstall, never
re-pulled); chat ready, Default Config reseeded, untouched.
`/sdcard/Download/Operit/test/`: `test_audio.wav` = RU benchmark
(3.6 MB), `test_audio_en.wav` = EN Kennedy (185 KB),
`test_audio_ru.wav` = RU spare. Day 16.10 added: "Ping Test" chat
(LLM proof) + terminal env setup skipped (3 setup-check sessions).
**(N53) Login flows (Antigravity/Codex/GitHub) — how they actually work.**
Browser is EXTERNAL: `Intent(Intent.ACTION_VIEW, authorizationUrl)` with
`FLAG_ACTIVITY_NEW_TASK`. No embedded browser for OAuth — by design, so
the password is typed on the provider's real page and the app never
sees it. No cookies are stored by the app: the loopback callback server
(`AntigravityOAuthLoopbackCallbackServer` / `CodexOAuthLoopbackCallbackServer`)
receives a one-shot authorization code, exchanges it via the provider's
token endpoint for access+refresh tokens, and stores only those tokens
plus email/project-id/account-id in encrypted prefs. On re-connect:
access token is reused while fresh (Antigravity 1 min skew, Codex 5 min),
then a single guarded refresh via `refreshMutex`; failure → "login again".
Logout: Codex revokes the refresh token server-side (proxied since
16.10 day) then clears prefs; Antigravity clears prefs only (nothing to
revoke upstream). The in-app WebView (`WebSessionBrowserScreen`) is a
separate feature with its own `CookieManager` and a "Clear cookies"
button — it has nothing to do with OAuth.
**(N54) In-app browser for OAuth — optional idea (user, 17.10).** Use
the built-in WebView with a per-browser proxy (OkHttp/WebView proxy) so
OAuth goes through the configured proxy even on networks where the
system browser can't reach the provider. Strictly scoped to that
browser only — do not touch the system-browser path or the loopback
callback server. Deferred; needs a design pass (cookie isolation, JS
challenge pages, captcha) before any code. Do NOT pick up without an
explicit user task.
**(N55) Windows remote control — what's inside (DONE 18.10).** It is a
t o o l p k g, not app code: `examples/windows_control/` (manifest
`toolpkg_id com.operit.windows_bundle`, version `0.2.0`, resource
`pc_agent_zip`), entry `dist/main.js`, subpackage `windows_control`
(`src/packages/windows_control.ts`). The bundle ships a Node.js 18+ PC
agent `resources/pc_agent/operit-pc-agent/` (launcher
`operit_pc_agent.bat` → `scripts/launch_agent.ps1`; server
`src/server.js`, default bind `127.0.0.1:58321`, token ALWAYS required
and auto-generated if absent). Mobile side sets env
`WINDOWS_AGENT_BASE_URL` + `WINDOWS_AGENT_TOKEN` (both required;
`WINDOWS_AGENT_DEFAULT_SHELL`, `WINDOWS_AGENT_TIMEOUT_MS` optional) and
gets tools: `windows_exec`, `windows_process_start/read/write/terminate/list`,
`windows_test_connection`, `windows_check_connection`, `read/edit/write`.
PC API: `POST /api/command/execute`, `/api/file/list|read|read_segment|write|edit|read_base64|write_base64`,
`/api/startup/apply_recommended_bind`, plus public `GET /api/health|config|presets|startup/state`.
Net effect: the PHONE runs PowerShell/CMD and file ops on the PC over
LAN. This is the reverse direction of the phone export and is NOT the
same as `exportWindowsApp`. Strings in `app/src/main/res/values/strings.xml:7825`
(`tool_windows_control_one_click`).
**(N56) Windows subpack template — what it is (DONE 18.10).** `OfflinePayload.SUBPACK_WINDOWS`
asset URL `.../v1.12.1+4/subpack-windows.zip`, `sizeBytes 11412717`
(11.4 MB zip), sha256 `9c2d3e3b...a46888` — RE-DOWNLOADED and verified
byte-for-byte against that hash. Unpacked 27 MB. It is a Flutter
Windows desktop shell (NOT Tauri/Electron): `assistance_subpack.exe`
(311 KB, PE32+ GUI x64, 6 sections), `flutter_windows.dll` (18.5 MB),
`webview_windows_plugin.dll` + `WebView2Loader.dll` (WebView2),
`url_launcher_windows_plugin.dll`, `data/app.so` (Dart AOT,
`package:assistance_subpack/main.dart` + `web_server.dart`),
`data/icudtl.dat`, `data/flutter_assets/` incl. `assets/web_content/{index.html,main.js,styles.css}`
and `assets/operit.png`. Leaked build path `D:/Code/prog/assistance_subpack/`;
branding strings "Operit Subpack", "Made By Operit". So the shell runs an
embedded Dart HTTP server that serves `web_content` into a WebView2
window. Export flow (`ExportDialogs.kt:894`): stage template → optional
icon swap via `ExeEditor` → copy project into `data/flutter_assets/assets/web_content`
→ re-zip to `Download/Operit/exports`. Per-file hashes: exe
`3e5957d48fffe121a82fbc46445d7957718fed59e490444744338818e980cb5d`,
`flutter_windows.dll` `9096bfdcab5d1241f6ff1b0d6d5144ae996ba4cfb918acfc84962e5d462a3d08`,
`app.so` `7004db2f6108fd1bd9c8856bc958bd38028aad39d674c7a2e31fcc735f914c57`.
**(N57) Remote Windows host access (user, 17.10).** SSH reachable:
`artur@192.168.1.66` password `123321` (plain `ssh` needs a password
helper — `sshpass` absent; use the installed `paramiko` instead:
`python3 -c "import paramiko..."`). Windows 10, PowerShell 5.1, .NET
present, Edge installed; `winget`/`choco`/`python` are NOT on PATH.
RDP is up (N58). `artur` is in BUILTIN\Administrators; WebView2 Runtime
155.0.4283.60 + Edge 155.0.4283.45 are installed (so Windows export has
its runtime). Node.js was absent until the user authorised its install.
**USER DIRECTIVE 18.10: the agent may do ANYTHING on this Windows
machine (full unrestricted permission) — install software, change
settings, run the PC agent, etc. No per-action confirmation needed.**
Installed Node.js v24.22.0 LTS (silent MSI) at the user's suggestion
("новee than 18"); `node -v` verified.
**(N59) VirusTotal scan — DONE (18.10).** Both artifacts were already
on VT (submitted ~2026-03). `subpack-windows.zip`
`9c2d3e3b5e862334e24f4008572f239c380a73f23c5940054bf3a5142ca46888`:
0 malicious / 0 suspicious / 68 undetected / 8 unsupported (clean).
`assistance_subpack.exe`
`3e5957d48fffe121a82fbc46445d7957718fed59e490444744338818e980cb5d`:
**1 malicious** / 0 suspicious / 56 undetected / 9 timeout / 6 failure /
4 unsupported — the single hit is APEX 6.769 (20260413) → "Malicious",
a generic heuristic on an unsigned freshly-built Flutter launcher; all
mainstream engines (Microsoft, Kaspersky, ESET, Google, etc.) are
undetected. Read as a likely false positive from one engine, not proof
of cleanliness — the exe is unsigned. Keyed via user's VT API key for
this session (not stored). Was blocked earlier because anonymous submit
is impossible: `POST /api/v3/files` without `x-apikey` → 401, web UI
`POST /ui/files` → 429 `RecaptchaRequiredError`.
**(N58) VNC/RDP to the Windows host — DONE.** RDP enabled on
`192.168.1.66` (port 3389 open, firewall rule `Remote Desktop`), login
`artur`/`123321`. Agent side: `xfreerdp` (installed via `sudo apt`),
driven through a persistent `Xvfb :99` (1280x800x24) started by the
agent; screenshot via `xwd -root` + PIL. Active session PID confirmed,
non-black pixels captured — desktop is live and controllable. Use
`DISPLAY=:99 xdotool` for clicks/keys (install xdotool if missing).
Stand up Xvfb + xfreerdp first in every new session.
**(N60) PC agent end-to-end on Windows — PC SIDE DONE (18.10).** The
`windows_control` toolpkg's PC half was deployed to `192.168.1.66` and
verified from the Linux box:
`C:\Users\artur\operit-pc-agent` (staged from
`examples/windows_control/resources/pc_agent/operit-pc-agent` via zip +
`Expand-Archive`), `npm install` done (node-pty needed
`npm install-scripts approve node-pty` + `npm rebuild node-pty` under
npm 11's blocked-scripts default; win32-x64 prebuilds present).
`data/config.json` written with `bindAddress 192.168.1.66`, port 58321;
token auto-generated (kept OUT of git — read it from that file). Intended
flow (`operit_pc_agent.bat`/`launch_agent.ps1`) opens a browser and needs
an interactive session, so it was made persistent as Scheduled Task
`OperitPcAgent` (`run_agent.bat` → `node src\server.js` with stdout/err
redirected to `logs\agent.*.log`, `/SC ONSTART /RU artur /RP ***
/RL HIGHEST`). Firewall rule `Operit PC Agent` TCP-58321 added. Proofs
over LAN: `GET /api/health` 200, `POST /api/command/execute`
(`Write-Output OPERIT-E2E-OK; [Environment]::MachineName`) → exitCode 0,
stdout `OPERIT-E2E-OK\r\nHP-630`, wrong token → 401, `POST /api/file/read`
→ package.json content. **BUG FOUND:** starting the agent with a broken
stdout (WMI create, no redirect) sends it into an `EPIPE`
`uncaughtException` loop that rewrote `agent.runtime.log` to **389 MB in
~25 s** (logger's `console.error` rethrows → handler logs again). The
supported launcher redirects, so it is a robustness gap, not a normal
path, but fatal on disk if triggered. **SECURITY NOTE:** `GET /api/config`
is unauthenticated and returns `apiToken` in cleartext — any LAN host can
read it; only token-gated endpoints are protected.
**(N60b) PC agent end-to-end on Windows — PHONE SIDE DONE (18.10).** The
`windows_control` toolpkg is present on the phone both as an APK asset
(`packages/windows_control.toolpkg`, seen in the load log) and in
`files/toolpkg_cache/com.operit.windows_bundle-2ca0f3a6/`. Env was set by
writing `shared_prefs/env_preferences.xml` while the app was force-stopped
(`WINDOWS_AGENT_BASE_URL=http://192.168.1.66:58321`, `WINDOWS_AGENT_TOKEN`,
`..._DEFAULT_SHELL=powershell`, `..._TIMEOUT_MS=30000`; file and reader in
`EnvPreferences.kt`, name `env_preferences`). Enabling was the missing
step: the manifest container `com.operit.windows_bundle` is imported but
the subpackage `windows_control` declares **`enabledByDefault: false`**
(`examples/windows_control/src/packages/windows_control.ts:12`), so
`isPackageEnabled("windows_control")` is false and the setup card shows
"Package not imported or env incomplete". The proper user path is the
screen's **"Step 3 → Paste and Apply"** (`pasteConfigAndApply →
saveConfigAndActivatePackage → ensureImportedAndUsed`, which calls
`enablePackage("windows_control")`). It was enabled for the test by adding
`"windows_control"` to `imported_packages` and
`{"windows_control":true}` to `toolpkg_subpackage_states` in
`shared_prefs/com.ai.assistance.operit.core.tools.PackageManager.xml`
(key names per `PackageManager.kt`: `ENABLED_PACKAGES_KEY="imported_packages"`,
`TOOLPKG_SUBPACKAGE_STATES_KEY="toolpkg_subpackage_states"`). With that,
Toolbox → Windows Quick Setup → **Connected**, showing `Package Version
0.2.2`, `Agent Version 0.2.2`, `Duration 107`, `Command preset:whoami`. The
PC agent log confirms the phone originated it:
`GET /api/health` then `POST /api/command/execute {"hasPreset":true,
"preset":"whoami",...} → cmd.exe /c "chcp 65001>nul & whoami", 107 ms`. So
phone → LAN → PC agent → process execution works end to end. Testing notes:
`adb shell input text` CANNOT type the config JSON (double quotes are
stripped, only the first segment survives) so the field must be filled by
a real keyboard/paste; and a chat attempt to invoke `windows_exec` for
`echo OPERIT-PHONE-E2E` was inconclusive (assistant body not exposed in the
UI dump, no matching agent log line) — the screen's `preset:whoami` already
proves arbitrary-command plumbing (same `/api/command/execute`), so this is
not blocking. ADB needed `adb kill-server; adb connect 192.168.1.69:5555`
to clear an `unauthorized` state; now authorized.
**(N61) Chat model down → harness reports it (USER, 18.10) — DONE 18.10.**
Requirement (also PORT_QUEUE "Backlog — added 18.10"): the API chat model
(Gemini Flash Latest) can be down; the harness used to read an empty
assistant reply as a pass. USER DIRECTIVE (18.10, second pass): a missing
reply must be RECORDED IN THE FINAL REPORT, not abort the run — a blank
reply means the model is unavailable, not that the UI is broken.
Implemented in `ci/script/uawalk.sh`: `REPLY_BASE` (record the current
assistant reply-body row count), `EXPECT_REPLY [secs]` (wait for a NEW reply
body within secs, default 120; on timeout it is NOT fatal — `uaw_note`
records "model unavailable: no assistant reply body appeared ..."). New
`uaw_note`/`uaw_notes` collect notes; `uaw_run` prints them at the end of
every scenario. `NOTE_IF_PRESENT <text>` records an unexpected on-screen
text (e.g. the "Request Failed" dialog) without aborting. `EXPECT_ABSENT`
was dropped (it aborted; replaced by the note-based approach).
Reply bodies are non-clickable full-width Views whose text is in
`content-desc` (N51), so `uaw_count_reply_rows` counts nodes with
non-empty content-desc, `clickable!=true`, `x1<=60`, `x2>=500` (tool-call
chips share the width but are clickable, so excluded). New scenario
`ci/script/scen/chat_smoke.uaw` (`EXPECT Enter your question...` →
`REPLY_BASE` → type/`TAP Send` → `EXPECT_REPLY 120` →
`NOTE_IF_PRESENT Request Failed`) and runner
`ci/script/accept_chat_smoke.sh` (no reinstall; launches the installed
build, passes onboarding if present, then walks the scenario and prints the
notes in the summary; exit stays 0).
KEY FINDING (live, on the test phone): the model-down symptom is an
**empty reply with NO dialog** — blocking the gateway
(`iptables -I OUTPUT -p tcp -d 192.168.1.55 --dport 20128 -j REJECT`)
makes okhttp log `EnhancedAIService: 发送消息时发生错误: Connection ...
Failed to connect ... 20128` and then the turn is finalized as complete
(`finalizeMessage notifyTurnComplete=true`) with only the "Operit" label
and no body. `EnhancedAIService.kt:1266` only sets
`InputProcessingState.Error` (→ the "Request Failed" ErrorDialog,
`AIChatScreen.kt:694` + `ErrorDialog.kt`) when `!providerStreamCollectionStarted`,
which is false once the stream collector is running — so a connection
failure never pops the dialog; the blank-reply note is therefore the one
that fires. NB: the Linux test host IS 192.168.1.55, so a whole-IP REJECT
(no `--dport`) also kills ADB (source .55); use `--dport 20128`. Recovery
from such a lock-out: tunnel ADB through HP-630, which is not blocked —
`python3 /tmp/opencode/phone_tunnel.py` (paramiko `direct-tcpip`
127.0.0.1:15555 → 192.168.1.69:5555), then `adb connect 127.0.0.1:15555`
and delete the rule (ADB auth is key-based, so the changed network source
is fine). Verified both paths: healthy turn PASSES in ~51 s, notes none,
exit 0; blocked turn finishes with the "model unavailable" note, runner
exit 0 (report shows the note).

**(N62) Export templates: Chinese default page translated, release re-uploaded
(18.10).** The only remaining Chinese in the two export templates was the
default `web_content` starter page (`index.html` + `main.js`, the diagnostic
CORS/storage/download bench titled "AI工作区"; `styles.css` had none). Both
were re-uploaded to release `v1.12.1+4` as English:
`subpack-android.apk` 48 059 795 B /
sha256 `932bfb5304e7db0e313dc4e8f03dd20387355c755c35812be467d8c47c9e11af`
(was 48 139 093 / `c56b23a8…`), `subpack-windows.zip` 11 422 731 B /
sha256 `b7d080a64c791d6a00fbe3511178bf702cefb890d93867e5bf705213a9a4c180`
(was 11 412 717 / `9c2d3e3b…`). `OfflinePayloads.kt` `SUBPACK_ANDROID` /
`SUBPACK_WINDOWS` updated to the new sha+size, so any device holding the old
staged copy re-downloads once. Key finding about reach: the Android export
**drops** `assets/flutter_assets/assets/web_content/` and writes the user's
project in its place (`ApkReverseEngineer.repackageApkWithWebContent` skips
that prefix), so the Android default page is never shipped — translating it is
cosmetic on the template artifact. The Windows export instead extracts the
whole template then copies the project over it without deleting extras, so its
default `index.html` would show for any export whose project has no
`index.html`; that was the real leak. Verified the rebuilt archives keep every
untouched entry byte-identical and in the same order (only the two files
changed). Page content lives in `/tmp/opencode/en_web/` (regenerate from the
released asset if lost). Verified end to end on the test phone (CI build
`39104317586`, APK archived `20261018_063108Z`): HtmlPackager (Toolbox →
HTML Packager) exported a project to
`Download/Operit/exports/Web_Project_20261018_104007.zip`; its
`data/flutter_assets/assets/web_content/main.js` sha256 is
`dfada5ab02c65f232d25b98baa74bcb6a961c7c32924bae957ae4649ef59733d`, i.e.
byte-identical to the new English template, and no `web_content` file
contains CJK. A project copied over the extracted template **without** an
`index.html` now leaves the English `<title>AI Workspace</title>` instead
of the old Chinese page. UI-driving note: this phone's DocumentsUI does
not navigate into a folder on `input tap`/`input swipe` (rows highlight but
never open) — worked around by putting `index.html` in the picked folder
root (`files/workspace/`) so the entry file is auto-selected; the
workspace provider root is `/data/data/<pkg>/files/workspace`
(`WorkspaceDocumentsProvider`). The Android side was re-checked too because
the template APK was re-zipped: HtmlPackager → Android produced
`Download/Operit/exports/WebApp_1792309997281.apk` (48 098 734 B, signed
`META-INF/ANDROIDK.*`), its `web_content` holds only the user's files (the
template defaults are dropped at repack, as designed, no CJK), and it
installs and launches on the phone showing the user page. So the re-zipped
`subpack-android.apk` still parses, repacks, signs and installs.
**(N63) PC agent EPIPE log spiral — FIXED (18.10).** N60's bug: the launcher
can start the agent with no console, so a write to `process.stdout`/`stderr`
hits a closed pipe; with no `error` listener Node escalates the EPIPE into
`uncaughtException`, whose handler (server.js) logs to the console again, so
the runtime log grew to 389 MB in ~25 s. Fix in
`examples/windows_control/resources/pc_agent/operit-pc-agent/src/server.js`:
attach an `error` listener to both streams that ignores EPIPE and reports any
other stream error at most once (so it cannot re-enter the handler). No
fallback: the log sink is declared best-effort, which is what it is.
Reproduced and confirmed with a harness over a real closed pipe using the
actual `logger.js`: without the listener, 1000 intended ticks produced
829 322 B (each tick logged again from `uncaughtException`); with it, 46 890 B
(one line per tick). `node --check` passes. Ships inside the toolpkg
(`manifest.json` resource `pc_agent_zip` → `resources/pc_agent/operit-pc-agent`),
so the fix reaches a PC on the next agent re-deploy (needs a fresh APK build
to embed the updated toolpkg). Deployed to the test host `HP-630` too:
`C:\Users\artur\operit-pc-agent\src\server.js` now has the fix
(sha256 `3394B576…`), the scheduled task restarted clean, and `GET
/api/health` (pid 4696, v0.2.2) + `POST /api/command/execute` (`whoami`
→ `hp-630\artur`, 83 ms) still work — no regression. Host-side repro note:
creating the agent via WMI (`Win32_Process.Create`, "no redirect") does
**not** reproduce the spiral on Windows — with no console handle Node's
writes are inert (old and fixed both logged 486 B in 12 s); the spiral needs
a pipe whose reader closed, which is the Linux harness case above.
**(N64) EPIPE fix embedded in an APK (build `39118388660`, 18.10).** CI
build `39118388660` at `880924cf` (with the N63 pc-agent fix in the
toolpkg) is green; artifact `operit-android-107` (160 295 263 B zip)
extracted to `/home/uzzzver/operit-fork/apk/20261018_101319Z/app-debug.apk`
(163 165 767 B, sha256
`7be2140ba6bb7af942b9d3593334d2b876172c4bf304874b42d548cc8380fd02`) with
`CHANGES.md`. This is the first accepted APK that carries the fixed
pc-agent, so a PC deploying the agent from this build gets the listener.
**(N65) Terminal submodule English-only regression — FIXED (18.10).** The
terminal module's Compose UI hardcoded ~65 Chinese literals (SSH config
screen, setup warnings, mirror display names, the terminal copy menu), so
they rendered Chinese no matter the app language. The English-only change
(`terminal@1df5639`, 30.09) fixed this and was pinned by `5f483aeb`. The
performance-monitor port `f6991f96` then re-pinned the submodule at the
upstream perf branch `0458689` (PTY child pid), which predates the i18n
commit → Chinese returned. FIX: cherry-pick `1df5639` on top of `0458689`
as `lagovi/OperitTerminalCore` branch `fix/terminal-english-strings`
(commit `ecb93f2`), which keeps the PTY child pid AND the English strings.
The `26d4964` APK-extraction fallback is deliberately NOT carried (only
needed under the rejected `useLegacyPackaging=false`). Registry: I18N-002.
Effect: SSH config screen, env-setup rows, mirror source names and the
terminal long-press "Copy" are English. Re-verify: `python3
ci/script/fork_audit.py` (0 new Chinese literals in Kotlin) + a CI build +
a phone check of the SSH config screen.
**(N66) PC agent settings endpoints were world-open — FIXED (18.10).**
With the agent bound to the LAN address (so the phone can reach it),
`GET /api/config` handed the api token to anyone who could reach the port,
and `POST /api/config` let them rewrite it (bind/port/token/allowedPresets);
neither checked anything. Only command/process/file required the token. FIX
(user decision: restrict settings to local access): the settings endpoints
— `GET`/`POST /api/config`, `GET /api/presets`, `GET /api/startup/state`,
`POST /api/startup/apply_recommended_bind` — now answer only to the agent's
own machine: loopback (127.0.0.0/8, `::1`, and the `::ffff:` mapped form)
or one of the host's non-internal IPv4 addresses (a PC browser that opened
the console through the LAN address still works). `/api/health` stays
remote-readable (the phone's health check). The phone calls only
health/command/process/file, so remote control is unchanged. Verified with
a mock-request harness: loopback and the host's own address → 200, a remote
address → 403 for the settings paths, health → 200. NOT taken (user chose
otherwise): never returning the token and making the console ask for it.
Registry SEC-001; `README.md` documents the split. NB: the agent ships
inside the tool package, so the fix reaches a PC only after a CI build and
a redeploy of the agent.
**(N67) Accepted build `39143043794` (18.10).** Archived at
`/home/uzzzver/operit-fork/apk/20261018_160038Z/` (`app-debug.apk`
163 129 867 B, sha256
`fcfc829ea12b83f46dd80996373ad3e63c68b862d76a10377b31c2679d92ba6d`),
CI green at `b5fdd791`. Carries the terminal English-only re-pin
(I18N-002 / N65), the PC-agent local-only settings (SEC-001 / N66) and the
N61 harness change. Phone acceptance: the CI debug signature changed, so
the app had to be uninstalled first; on the test phone it installs and
shows the English onboarding. The shipped APK contains the terminal
English strings (`OpenSSH Server`, `SSH Connection Config`, `SSH Mode
Warning`) and none of the former Chinese literals (`清华源`,
`启用反向挂载`, `SSH 连接配置`, `密码认证`, `暂无SSH配置`, …), pulled
straight from the packaged APK. NB: the reinstall wiped app data, so the
phone-side `windows_control` re-verify (item 7) needs the environment and
the PC agent re-deployed.
**(N68) Phone→PC `windows_control` re-verified on build `39143043794`
(18.10).** After the reinstall wiped app data, the env and the enabled
subpackage were restored the N60b way (app force-stopped, written via
`run-as`): `shared_prefs/env_preferences.xml` = `WINDOWS_AGENT_BASE_URL`
`http://192.168.1.66:58321`, `WINDOWS_AGENT_TOKEN`, `..._DEFAULT_SHELL`
`powershell`, `..._TIMEOUT_MS` `30000`; `...PackageManager.xml` =
`imported_packages` gains `windows_control` and `toolpkg_subpackage_states`
`{"windows_control":true}`. Toolbox → Windows Quick Setup then shows
**Connected**, package/agent `0.2.2`, `preset:whoami` in 82 ms. PC side
(`C:\Users\artur\operit-pc-agent\logs\agent.runtime.log`) shows the matching
`GET /api/health` and `POST /api/command/execute` (`whoami`,
`tokenProvided:true`, `exitCode 0`, 82 ms). Item 7 CLOSED. The live UI check
also confirms the English-only UI on this build.
**(N69) Terminal Ubuntu rootfs moved out of the APK (18.10, item 2 / Q2
tail).** The last movable heavy asset, the 61.2 MiB
`ubuntu-noble-aarch64-pd-v4.18.0.tar.xz`, is no longer bundled. It is
downloaded on demand from the fork's own release `terminal-rootfs-v1`
(asset `ubuntu-noble-aarch64-pd-v4.18.0.tar.xz`, 64133552 B, sha256
`91acaa786b8e2fbba56a9fd0f8a1188cee482b5c7baeed707b29ddaa9a294daa`; uploader
`ci/script/publish_payload_release.sh`, new). Mechanics: `:terminal` no
longer reads the archive from `context.assets`; a new seam
`RootfsArchiveProvider` (`terminal/.../RootfsArchiveProvider.kt`, commit
`terminal@9d01a2e`, branch `feat/rootfs-host-provider`) is installed by
`:app` in `OperitApplication.onCreate` (`TerminalRootfs.provider`). The
archive is requested only when Ubuntu is not yet installed
(`TerminalRootfs.isInstalled`, the single source for the installed path and
the archive name); after the install script unpacks the tree it deletes the
archive, so an installed terminal never re-downloads. `:app`
`TerminalRootfsDownload` (singleton, implements `RootfsArchiveProvider`)
wraps `RemoteAssetFetcher` (resume/Range/sha256) and exposes a `StateFlow`;
`TerminalRootfsGate` (`ui/components`) wraps both :app terminal entry points
(`ComputerScreen`, `ToolboxScreen.TerminalToolScreen`) with consent →
progress → retry, and only creates `TerminalManager` once the archive is on
the device. `OfflinePayload.TERMINAL_ROOTFS` is registered in
`OfflinePayload.all`. NOTE: E3 still stands — the *installed* tree stays in
internal `filesDir`; only the *source* of the archive changed. MEASURED:
CI `39173414126` (commit `04ceace1`) GREEN, APK 163129867 → 98986580 B
(−64143287 B, exactly the removed archive); the packaged APK no longer
contains `ubuntu-noble-aarch64-pd-v4.18.0.tar.xz` (still ships
`assets/setup_fake_sysdata.sh`). Archived
`/home/uzzzver/operit-fork/apk/20261018_224236Z/` + `CHANGES.md`. Acceptance
(fresh install → Toolbox terminal → consent → download → Ubuntu unpacks;
no re-download on relaunch) — see N70 below.
**(N70) Build `39173414126` / `20261018_224236Z` ACCEPTED on the phone
(19.10, item 3).** Installed on the a20s (uninstall first: the CI debug key
differs per build, so app data was wiped; user confirmed the test phone is
disposable). Onboarding completed with the English screens. Terminal rootfs
acceptance: the first entry to the terminal (top-bar "AI Computer") shows the
`TerminalRootfsGate` consent card; Download → `files/ubuntu-noble-…tar.xz.part`
grew to 64 133 552 B, then the final archive appeared; `TerminalManager`
logged `Rootfs archive ready`; the shell logged `Ubuntu not installed,
installing...` then `Ubuntu installed -> Ubuntu 24.04.1 LTS`;
`.operit_installed_ok` was written and the install script deleted the archive.
After force-stop + relaunch, the gate was Ready with no consent card and no
archive reappeared (no re-download); the terminal reached a live
`root@localhost:~#` proot shell (Ubuntu banner, python3 present). Full
`ci/script/scen/full_tour.uaw`: 113 steps, 683 s, 0 aborts, notes none; no
FATAL in logcat. Item 3 CLOSED.
**(N50) Full UI tour 16.10 day (build `20261016_104352Z`).**
`ci/script/scen/full_tour.uaw` (screen_tour + SAVEDUMP per screen):
113 steps PASS, 740 s wall, zero aborts, zero dropbox crashes. Dumps in
run dir only (`/tmp/uaw/`, NOT archived — element lists live in the
16.10 test report, not here). Timing profile: TAP ~12 s avg (dump +
search + tap + sleeps dominate), EXPECT ~3.7 s, BACK ~3.6 s,
SCROLL_TO ~8.2 s, SAVEDUMP ~4.2 s. Speedups, if ever needed: smart
waits instead of fixed `sleep 2`, not fewer checks.
**(N51) Chat/UI text traps (16.10, all burned once).** `adb input text`
wants `%s` for spaces — `%20` lands LITERALLY in the field (TYPE now
auto-encodes spaces — uawalk.sh). Chat Send shows `clickable=false` in
dumps but taps fine (Compose quirk, don't "fix" the scenario by
skipping it). Assistant message bodies expose as `content-desc`, NOT
`text` — `plab_texts` now collects both (phone_lab.sh); any future
EXPECT on chat content relies on this. Terminal canvas exposes NOTHING
to uiautomator (no command, no output) — verify via `plab_shot`
screenshot (phone_lab.sh), read the pixels.
**(N52) Live function proofs 16.10 day (same build).** LLM: prompt sent
from chat, title req 200 in 22.5 s ("Ping Test"), message stream 200 /
2969 bytes in ~24 s, reply rendered; Default Config =
OPENAI_GENERIC → `http://192.168.1.55:20128` model
`gemini/gemini-flash-lite-latest` (gateway thinking time, not app
latency). Terminal: Ubuntu proot `root@localhost`, `echo T2-OK` →
`T2-OK` (screenshot). STT: Tool Tester "Para Transcribe EN" re-ran
TODAY — Kennedy quote exact; live mic: GigaAM→Parakeet switch works,
Start = "engine initialized / Recognizing...", Stop = "Not
recognizing...". TTS: screen only (audio unverifiable without ears).
File Manager lists `/sdcard`. Permissions Refresh re-reads statuses
(Terminal row Not Granted = expected). About updates = "Already on the
latest version: 1.12.2". GigaAM demo shows "not initialized" (model
not downloaded — N47, not a bug).

## Status: what exists

- STT = two on-device engines. GigaAM (sherpa fully removed, M5
  `e763e8ef`; M6: 497 phrases, RTF 1.05; custom CTC via HF link) and
  Parakeet TDT (`82bac318`: int8 670 MB, leaf `stt_parakeet`,
  `LOCAL_PARAKEET` preset). Benchmarks, all on device: GigaAM chunk 3/7
  RTF 1.08 / whole 3/7 RTF 1.47; Parakeet chunk 4/7 RTF 1.83; Parakeet
  EN chunk+whole perfect; Parakeet whole over 20 s refused loudly
  (E25). Tool Tester rows: STT Transcribe (giga/chunk), STT Transcribe
  Whole, Para Transcribe, Para Transcribe Whole, Para Transcribe EN —
  all on `transcribe_file` with engine/mode params (N37). Benchmark
  files OUTSIDE the repo: `~/operit-fork/wav/benchmark_v2.wav` (RU,
  mono 48 kHz, 37.5 s; no backup — do not delete); EN sample lives on
  the phone only (sherpa `en.wav`, Kennedy, 3.8 s). Scoring: GigaAM
  renders Cyrillic, Parakeet Latin — same semantic rubric,
  script-aware (§8). Re-run benchmarks ONLY on: ORT bump, model-rev
  change, frontend/loop edit — otherwise the numbers stand.
- Runtime Chinese translation fully done: T1 (cache + surfaces), T1-Market
  (`ca511255`), T2 batch prefetch (`03d51d5f`), T3 Google fallback
  (`5a76aa4d`, user-approved). Layer map: batch
  `ConversationService.translateTexts` + `EnhancedAIService.translateTexts`,
  prompt `FunctionalPrompts.translationBatchUserPrompt`, cache/parsing
  `data/translation/CachedTranslator.kt` (+ `TranslationPrefetch.kt`,
  `GoogleTranslateFallback.kt`), UI `TranslatedText.kt` /
  `TranslatedMarkdown.kt` + `prefetchRuntimeTranslations` in
  `ToolResultDisplay.kt`, triggers in `UnifiedMarketScreen.kt`,
  `UnifiedMarketDetailEntryScreen.kt`, `PluginTabContent.kt`, `OperitApp.kt`,
  dialog `RemoteAnnouncementDialog.kt`. Avatar initials, badges, usernames
  deliberately untranslated (identifiers).
- Terminal WORKS (broken in all fork builds since `bf48f418`, resurrected
  05.10): PM extracts 41 .so (`useLegacyPackaging=true`, `cddb9906`). The
  APK-extraction fallback (`26d4964`) was dropped on 18.10 when the pin was
  moved to `ecb93f2` (see N65): it only fires when `nativeLibraryDir` is
  empty, which only `useLegacyPackaging=false` produces, and that lever is
  rejected (SIZE-007). Terminal setup CLOSED 11.10 (poll
  `accept_term7.log` round 3 "Granted", PASS=4 FAIL=0): red
  "Operit Terminal" line = `isOperitTerminalInstalled`
  (`PermissionLevelCard.kt` → `ShizukuDemoScreen.kt:147` →
  `DemoStateManager.kt:227,460`); green ONLY with pnpm AND python AND pip
  (`DemoStateManager.kt:364-403`, check runs every Permissions entry,
  log line `NodeJS环境检查`). DEFAULT-SELECTION TRAP (E13, live 11.10):
  SetupScreen (`terminal/.../terminal/ui/SetupScreen.kt:143-207`)
  preselects packages ONLY if found INSTALLED (found ones are then
  skipped by the `!= INSTALLED` filter); missing ones are NOT selected —
  Start Setup with defaults = near-no-op (only pip-mirror echos leave),
  screen returns home instantly with eternal Interrupt, looks like success
  but downloads nothing (proven: empty apt history.log, idle bash only).
  RIGHT PATH (two entries): (a) Toolbox → Command Terminal → check
  nodejs + python categories (Select All right side; verify checked=true
  in dump — blind taps toggle!) → Start Setup → Notice OK → real
  download under Interrupt; (b) shorter: DEBUGGER tab → "Configure
  Terminal Environment" card (writes what's missing itself) → Expand →
  "Go to Terminal Configuration" → same SetupScreen.
- Shizuku v13.6.0 on the phone, Operit authorized, Debugger level active
  (restart recipe after reboot — Shizuku section in ARCHIVE.md).
- LLM I/O Log DONE (`1c0ed81d`, CI `37351728353`, APK `2026-10-05_17-52-47Z`):
  table `llm_io_log` (migration v21→v22), hook in
  `TokenTrackingAIService.sendMessage` writes request JSON + raw response +
  tokens + latency + error; screen in Settings after Token Usage Statistics
  (list + model filter + detail dialog + Copy/Share + Clear + toggle);
  cap 500 rows / 8 MB.
- Supervision Mode ACCEPTED 06.10 (`63dadf6e` + fixes, CI `37467783125`,
  APK `2026-10-06_13-03-03Z`): toggle + card in FunctionalConfig by eye,
  7 live SUPERVISION rows (6×ok). Toast CAUGHT live 08.10 (white card +
  id=8, text matched) — no residual risk. Known v1 limit: digest has tool
  NAMES without args/results (needs deeper hooks, separate task — never
  filed; priority deliberately below size work).
- Subpack Q1 mechanics DONE 08.10 (`a4fddb40`, CI `37709173132`,
  APK `2026-10-08_01-06-04Z`, 247299182 B): `core/subpack/SubpackStorage.kt`
  (leaf `subpack`, stage from assets with overwrite, `migrateTo`); staging
  `ApkEditor.fromAsset`/`ExeEditor.fromAsset` moved from cacheDir to leaf
  (cacheDir fallback kept); Settings → Data & Permissions has
  `SubpackStorageSection` (same move mechanics as the STT model).
  Acceptance: row renders with leaf path; internal → external → memory
  card move clean, dir `/storage/5982-1724/.../files/subpack/` created.
- Sizes honest: minified debug 08.10 — 247299182 B (CI `37709173132`).
  R8 mapping of every green CI in artifact `operit-android-mapping-<run>`.
  Installed NOT remeasured for current builds (epochs before revert:
  435 MB at `true`, 369 MB at `false` — methodology in E2: BOTH trees,
  package dir + `/data/data/<pkg>`, never guess). Size ledger
  `docs/FORK-REGISTRY/research-size.md` — read BEFORE any new size work.
- Unit tests: `Android Tests` green 07.10 (1310 tests, `55e18396`, run
  `37682838742`). Rule for red: each new `e: file` in `--log-failed` →
  `git log` the file: ours → fix code, upstream → minimally fix the test,
  NEVER production. Keystore: RESOLVED 08.10, NOT NEEDED (no store
  release; GitHub + hand-installed debug builds suffice).
  Q2 hosting RESOLVED 08.10: same repo, GitHub Releases. Practice: release
  per version tag (`vX.Y.Z`), one asset per payload, short note of
  contents; never create empty releases (first appears with ready Q2
  download code); all fork releases `--prerelease` + ALPHA banner
  (100% vibe-coded, stability is upstream's).

## §8. Open work (order matters not; each has a DoD)

Joint manual testing with the user (NO checklist exists and none is
owed right now — clarified with the user 16.10; see OPEN (b). Agent
part: keep this list truthful; "ready" only after green CI + install
+ wizard + smoke). Crossed out without a
referee (if one resurfaces — file with screenshot name + screen path):
dead button / U1 / D12; Market search icon verified (opens search).
Far-field STT only if the user complains (M6 closed).
(a) subpack-leaf files after a REAL export — PROVEN 10.10 (Android;
Windows export never run): HTML Packager → ExportPlatformDialog →
Android export to the end → `files/subpack/apk_editor_android.apk`
48139093 B + sha `c56b23a8...` (size+sha matched), output APK installs;
(CLOSE-OUT task 13 for details).
(b) 5a/5b by eye — CLOSED 15.10 night, agent alone via root (decision
15.10). 5a — STT model move to SD: no reinstall needed (CURRENT proxy
build `20261014_062241Z` on phone since 14.10 09:40, model never pulled —
already a clean field).
Download: Settings → Speech Services → STT tab → Download button
(after scrolling; tapping the "Not downloaded" row does nothing).
Tap calibration that session: request lands ~50px ABOVE glyph here
(pointer_location proof: request (153,795) → point (155,742); top rows
~1:1). Download ran with NO consent dialog (consent from an earlier
epoch persists) — "Downloading speech model... 0%" → finished in
~2 min; files byte-exact (`model.int8.onnx` 224762512, `tokens.txt`
391, same as M6). Move: Change → "Memory card (app folder)" →
"Moving..." → done ~20:47. Proofs: UI path row now
"Memory card (app folder)"; card
`/storage/5982-1724/Android/data/.../files/stt_model/` holds both
files byte-exact; internal `files/stt_model` GONE; app log
`D/GigaAMSpeechProvider: GigaAM initialized` 20:59 (model exists ONLY
on card — init-from-card proven). Live: Toolbox → Speech Recognition →
Start → "engine initialized / Recognizing..." ~19 min (21:01–21:20),
stopped cleanly by button; 0 phrases (quiet room — fact, not failure;
no `Utterance decode failed`). "Sings" attempt: benchmark wav pushed to
/sdcard/Download, but NOTHING can play it — box has no players, phone
has no music player (chooser offers only Operit EN + Termux), Termux
has none, `tinyplay` can't open PCM (`cannot open device`), `cplay`
permission-denied. Recorded for the STT-benchmark methodology question:
candidate (1) "play into mic" needs a player first (`pkg install sox`
in Termux via RUN_COMMAND is the cheapest path). Phone at 5a time: STT
model on SD, dictation idle, Default Config untouched. (Phone truth
has moved on since — always read (N47), never this line.)
5b — ANR tails: `AnrMonitor` thresholds (`AnrMonitor.kt:47-48`):
warning >500 ms, "possible ANR!" >1000 ms. Scroll load 15.10 21:30–
21:33 (6 fast sendevent swipes over the 25-item Packages list, ~40 s):
ZERO new AnrMonitor lines (logcat last 1000 + `operit.log` — latest
stays 19:46). Historical (fact, not "clean"): 111 stall lines total —
54 warnings (500–1000 ms) + 57 possible-ANR (max 5765 ms); 110 dated
10-14 (install/acceptance day) + 1 on 10-15 19:46 (2196 ms,
unattended — before this session, unattributed). Monitor's own strings
still hardcoded Chinese (`检测到ANR!`, `检测到重复的ANR` — known M6
follow-up, unchanged). Task 6 CLOSED.
STT benchmark for the ORT 1.17.1→1.21.1 regression (given by user 15.10;
RAN 16.10 night on command — close-out below). File
`~/operit-fork/wav/benchmark_v2.wav` (mono, 48 kHz, 37.5 s, 3.6 MB).
Spoken text: "В дистрибутиве Omarchy под
Hyprland мы запустили браузер Helium с аппаратным ускорением через
VA-API и настроили Neovim под виндовые хоткеи. Сейчас мы тестируем
модель GigaAM в связке с PipeWire, замеряя inference и потребление
памяти в btop." (Russian — the recognition target is Russian audio;
this paragraph stays Russian on purpose.) Fuzzy scoring (any variant in
parens = 100% hit): omarchy (омарчи, омачи); hyperland (хайперлэнд,
хайперланд); helium (хелиум, хилиум); pipewire (пайпваер, пайпвайр);
neovim (неовим); btop (битоп); inference (инференс).
METHODOLOGY ANSWER (was the open question; resolved by implementation,
not guessing): the mic path cannot take files and nothing on either
box can play audio (no players on box/phone/Termux; tinyplay/cplay
blocked — E24 below). Implemented the file path instead: `transcribe_file`
tool (`17007bad`: strict `WavFileReader` PCM16-mono + `PcmResampler` to
16 kHz + the SAME 512-frame VAD walk and chunker the mic uses, one
`transcribe()` per chunk) + Tool Tester row "STT Transcribe" (mirrors
OCR Read; test file `$testBaseDir/test_audio.wav` =
`/sdcard/Download/Operit/test/`, pushed via adb — NOT
`/sdcard/Operit/test/`, that was a wrong first push). No backup of the
benchmark in repo — never delete/move it.
RESULT 16.10 (APK `20261015_194503Z`, CI `38796418126` green, headSha
match; fresh install + wizard + 225 MB re-download; transcript:
"дистрибутивы у матче под хайплнд мы запустили браузер хилиум с
аппаратным ускорением через эй эйпиай и настроили невим подвиндовые
хокке сейчас мы тестируем модель гигаээм в связке с пайа замеряя
инференс и потребление памяти в битоп"; log: 6 non-blank of 6 chunks,
37.5 s audio in 42185 ms = RTF 1.12): strict score 3/7 — HIT helium
(хилиум), btop (битоп), inference (инференс); MISS omarchy ("у матче"),
hyperland ("хайплнд", near), neovim ("невим", near), pipewire
("пайа" — cut mid-word, VAD chunk boundary). VERDICT: engine healthy
under ORT 1.21.1 (no crash, no decode failure, sane timing); misses are
rare-word model quality (near-misses + one boundary cut), NOT ORT
breakage — and breakage is unprovable either way: no 1.17.1 baseline
exists (benchmark postdates the bump) and 1.17.1 cannot load this IR-10
model at all (E17). This 3/7 is the absolute reference for future runs.
PARAKEET PRESET (user task 16.10, DONE night 16.10). Model:
nvidia/parakeet-tdt-0.6b-v3 — FastConformer+TDT, 25 European languages
incl. ru (NOT English-only), vocab 8193 (blank 8192). Quant verdict:
int8 (user's tested quant — correct: Q8 LS-clean 1.94 vs 1.95 F32; fp16
is 2x bytes with no ARM CPU win; fp32 2.5 GB uninstallable; int4
unverified). Files from `istupakov/parakeet-tdt-0.6b-v3-onnx` @
`8f23f0c0` (fused decoder_joint = 1 ORT session): encoder int8
652183999 B (sha 6139d2fa…), joint int8 18202004 B (sha eea7483e…),
vocab.txt 93939 B (sha d5854467…). Phone fit: 2.8 GB RAM / 13 GB
internal free / 980 MB SD — SD only, ~300 MB headroom.
Implementation (`82bac318` + fixes `f0ad404a`/`5c4c627e`; APKs
`20261016_034748Z`/`085455Z`/`104352Z`, CIs green, headSha matched):
ParakeetFrontend (nemo128 mel, exact port), ParakeetVocabulary,
ParakeetRecognizer (encoder + TDT greedy loop, validated against the
onnx-asr reference — including two int32-vs-int64 dtype traps the
validation caught first), ParakeetModelFiles/Storage/Download (leaf
`stt_parakeet`), ParakeetSpeechProvider (full SpeechService clone),
LOCAL_PARAKEET in factory/prefs/UI (cycle, dropdown, model section,
consent), transcribe_file engine/mode params, 5 Tool Tester rows. TDT
loop and I/O names taken from onnx-asr `nemo.py`/`asr.py` (encoder
`audio_signal`+`length` → `outputs`+`encoded_lengths`; joint
`encoder_outputs`+`targets`(int32)+`target_length`(int32)+states →
logits+states; blank advances `max(dur,1)` without state update).
BENCHMARK MATRIX (same Russian wav + sherpa EN sample), all on device:
GigaAM chunk 3/7 RTF 1.08 (same text as the 15.10 run — stable);
GigaAM whole 3/7 RTF 1.47 (drops the Hyprland segment — segmentation
matters); Parakeet chunk 4/7 RTF 1.83 ("мачи~/—/Helium✓/Pipewire✓/
Newwin~/BTOP✓/inference✓"); Parakeet whole on 37.5 s = SILENT APP
DEATH (E25); Parakeet EN chunk+whole = PERFECT Kennedy quote (25–28 s
for 3.8 s — model-load time included). Scoring note: Parakeet renders
Latin-script terms (Helium/Pipewire/BTOP), GigaAM Cyrillic — same
semantic rubric, script-aware. VERDICT: Parakeet wins this benchmark
(4 vs 3) and takes English perfectly; int8 quant confirmed as the phone
quant (only viable one); whole mode capped at 20 s by guard (E25).
Phone state at matrix time: both models on SD, location SD. NOTE 16.10
later: the guard-fix reinstall wiped the card app folders (E26) — phone
truth moved to (N47): Parakeet on SD only, GigaAM not downloaded.
(c) Manual Google sign-in to Antigravity — user does it later alone.
After: quotas, model list, chat via Antigravity (PORT_QUEUE.md).
(d) Proxy tails (deliberate, not bugs): Antigravity `getModelsList`
direct; login-over-proxy only if it fits the scheme (don't demand a user
account for it).

## Status: environment

- Every build-checked APK: `/home/uzzzver/operit-fork/apk/<UTC-CI-start>/`
  + `CHANGES.md` (3–6 delta lines). Recent (older — same dir, CHANGES.md
  each):
  `20261010_215026Z` (evict-fix 17: 179391284 Б, CI `38089152700`,
  accept10 PASS=10 — closed 14/15/17; LAST signing build),
  `20261011_015941Z` (Q2 helper-APK: 175947764 Б, CI `38103675524`,
  walker PASS=6),
  `20261011_123918Z` (B+C identity: 175947900 Б, CI `38140082342`, PASS=8),
  `20261011_141729Z` (Q2 seeds: 167739638 Б, CI `38146579269`, PASS=8),
  `20261012_104102Z` (ports batch-1+2 + help: 168064813 Б, CI
  `38224488511`; tour 102 steps PASS, OCR baseline 5/5),
  `20261013_025022Z` (Antigravity: 168086401 Б, CI `38332016760`;
  OAuth reaches Google; Default Config restored),
  `20261014_025827Z` (PaddleOCR: 163082823 Б, CI `38499450049`;
  12/12 + Russian),
  `20261014_062241Z` (provider proxy: 163102271 Б, CI `38520434647`;
  DIRECT+HTTP+SOCKS5(+auth)+dead-proxy acceptance, Default Config
  restored),
  `20261015_194503Z` (transcribe_file: 163105731 Б, CI `38796418126`;
  GigaAM 3/7 file benchmark),
  `20261016_034748Z` (Parakeet preset: 163162083 Б, CI `38847296082`;
  matrix build),
  `20261016_085455Z` (move/guard fixes: 163163419 Б, CI `38876872783`),
  `20261016_104352Z` (guard message fix: 163163615 Б, CI `38889147136` —
  CURRENT; on phone now: Parakeet on SD, guard verified).
  Disk `/` 99% CRITICAL (N46: clean MY scratch first, then escalate —
  never the `apk/` archive): `/tmp` cleaned by agent (stale
  `gh-artifact.*.zip`, model dupes, own screenshots/dumps; toolkit and
  `gigaam-model` untouchable), rest — user. Artifacts re-downloadable
  from CI without rebuild (`gh run download <run> -n <name> --dir ...`;
  inner dir names drift — `find -name "*.apk"`; big timeouts).
  Rule already in `AGENTS.md`.
- Workflow `Android Build` fires on push ONLY for `main`
  (`on.push.branches: [main]`) — feature branch needs manual:
  `gh workflow run "Android Build" --ref feat/english-only-build`.
- Live-chat recipe (proven 06.10 twice): clock-button in the strip under
  the header (~55,232) → Chat History slide-menu → + New Chat (~218,355)
  → chat with input ready. Fresh install gives input immediately. Clock
  tap sometimes misses first time — repeat; left-edge swipe opens the
  WRONG menu (app drawer), don't confuse.
- Test phone fully agent-managed (policy below in force); USB dead, WiFi
  adb only (N42 for the reconnect recipe); packages:
  `com.ai.assistance.operit.debug` (Operit),
  `moe.shizuku.privileged.api`. NEVER hardcode the adb port (it dies in
  idleness; re-ask/scan). Grants/state: model preset re-seeds on fresh
  install (endpoint/key/model from `ModelConfigManager` DEBUG branch),
  supervision OFF by default; battery check `dumpsys battery` FIRST;
  brightness 1 for night work; heavy work only with charge headroom.
  Phone truth 16.10 close — see (N47).
- LAN gateway for tests (model, translation): `http://192.168.1.55:20128/v1`,
  key+model baked into preset ONLY in debug (`ModelConfigManager`,
  `BuildConfig.DEBUG` branch). Never carry to release.
- Disk `/` 99% — CRITICAL (N46); `/tmp/opencode` cleaned after builds
  (temp APKs to `/tmp/opencode/<name>`, delete right after install).

---

## Device+CI cheat sheet (read before any acceptance)

**adb.** USB dead, WiFi only. Port changes on its own, debugging dies
idle — never hardcode. TRAP: user gives the pairing port — `adb connect`
to it gives `Connection refused`; the real adb port comes separately.
Don't guess: try connect, on refuse re-ask/scan. Since 09.10: classic
PERMANENT ADB via root (`su -c 'setprop service.adb.tcp.port 5555'` +
`stop adbd; start adbd` → `adb connect 192.168.1.69:5555`); Termux:Boot
re-raises it after reboot (`~/.termux/boot/adb-tcp.sh`, proven by reboot
test). Local trick: port open and pingable but `adb connect` hangs —
`adb kill-server; adb start-server` (stale server).
**CI.** Push to feature branch does NOT start the workflow — manual
dispatch only: `gh workflow run "Android Build" --ref feat/english-only-build`.
DISPATCH ONLY AFTER PUSH (runner checks out origin; 06.10: pre-push
dispatch built stale code, green CI wasted). Wait: background protocol
(N31), or `gh run watch <id> --exit-status --interval 60`. Artifact:
`gh run download <id> -n <name> --dir ...`, inner dir name drifts —
`find -name "*.apk"`. APK dir = run UTC `createdAt` + `CHANGES.md`.
10.10: GitHub disabled Actions on the fork itself (abuse heuristic on a
pile of long manual runs) — fixed ONLY by the owner button on the
Actions tab, not programmatically.
**Install.** Every CI build has a new debug key — `uninstall` first, then
`install`, verify `dumpsys package ... | grep lastUpdateTime`. `&&`
chains hide a skipped install — check the time. A reinstall wipes
`Android/data/<pkg>` on the SD card too (E26): every on-card model must
be re-downloaded after every install — never assume, always `ls` first.
Streamed install of 287 MB sometimes doesn't start first try (empty dumpsys — reinstall with
full output, not `tail -1`) or HANGS midway — after a timeout do NOT
count as installed, verify `dumpsys`/reinstall. Temp APKs only in
`/tmp/opencode/`, delete right after install.
**Wizard** (720x1560; dump-driven variant in (N45) is primary since
16.10 — fixed coords below are fallback): WAKEUP + dismiss-keyguard, `monkey -p
com.ai.assistance.operit.debug -c android.intent.category.LAUNCHER 1`,
consent (360,1371) after countdown, tour/greeting next (650,1378), adb
grants (below), Check (360,950), next (650,1378), Incomplete Continue
(~500,905), Standard (100,465 or 360,461), Confirm (360,1206), wait out
the announcement countdown. Bottom-arrow touch target y~1378, NOT the
glyph (~1416). Grants: `pm grant READ_EXTERNAL_STORAGE + RECORD_AUDIO`,
`appops set SYSTEM_ALERT_WINDOW + MANAGE_EXTERNAL_STORAGE allow`,
`dumpsys deviceidle whitelist +PKG`, `pm grant FINE/COARSE_LOCATION`,
`settings put secure location_mode 3` (or Location ✗),
`screensaver_enabled 0`. 08.10 addendum (proven): the "→ → Incomplete →
Continue → list" loop is ENDLESS without grants — not a bug, the wizard
wants permissions. Issue the shell set (proven sufficient):
`pm grant PKG READ_EXTERNAL_STORAGE + WRITE_EXTERNAL_STORAGE +
ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION`,
`appops set PKG MANAGE_EXTERNAL_STORAGE + SYSTEM_ALERT_WINDOW allow`,
`dumpsys deviceidle whitelist +PKG`. Then open the Storage row (system
"All files access" — already ON, as it should be), go back, tap Check
Permission Status — all 4 ✓, then → goes. UI shows × until revisit+Check
despite granted perms — stale, not refusal, don't flinch.
**Screen.** `uiautomator dump` to `/sdcard/ui.xml` + pull + python XML
parsing is PRIMARY since 15–16.10 (works reliably on the lab phone —
E4 describes the OLD device, ignore its verdict): texts AND bounds,
tap the CENTER of the dumped bounds, re-dump after every tap, never
reuse coordinates across screens or dialogs (Retest moves per dialog —
two misses 16.10 from stale coords). Screenshots (`screencap -p` +
Read) are visuals-only backup. Dump dying with SIGKILL (exit 137,
0-byte xml) still happens occasionally — then fall back to screenshots.
Black frame = caught a transition animation, not death:
wait 3 s and reshoot; identical black frames share md5 (7714 B — compare
without opening). BUT: black frame twice in a row = screen SLEPT
mid-session (check `dumpsys display | grep mScreenState`): cure
`settings put secure screensaver_enabled 0` +
`settings put system screen_off_timeout 1800000`, then WAKEUP. On fresh
install the wizard screensaver grant may not apply — check first on night
acceptance. Sleeping screen = stale frames (clock stands): WAKEUP first,
compare md5. `run-as` — single commands only (quoting with `;` is
unreliable); don't forget `adb shell` — bare `ls /storage/...` runs on
the notebook, not the phone (gave a false "data loss" in M6).
**Pull DB as THREE files:** `databases/app_database` + `-wal` + `-shm`
(06.10: without WAL the fresh `llm_io_log` rows are INVISIBLE — false
"hook doesn't write", half an hour wasted). Same basename side by side,
open the main file in sqlite3. `logcat` — only `logcat -t N`, full `-d`
hangs the shell. `adb pull` has NO `-q` (command fails). `input text`:
space is ONLY `%s`; `%20` is NOT decoded and lands literally. Worse:
`%sX` is dangerous too — `%PONG` landed literally (only `%s` is valid);
after `input text` ALWAYS screenshot and read eyes-on before send.
**Toast hunting** (`ChatToastHost`, top center): duration
`estimateToastDurationMs` — ~5 s for a short comment; single screenshots
won't catch it (~50 misses 06.10). Method: back-to-back `screencap`
series (no sleep, ~2 s/frame, 20–30), then `md5sum` — glue identical,
read different (toast = white card over the strip). Post time =
`timestampMs` + `latencyMs` of the log row — compute the window from it.
**Taps.** In scrolled settings lists and dropdown menus the touch lands
~75–100px ABOVE/BELOW the requested Y (proven with `pointer_location 1`;
DIRECTION IS UNSTABLE across screens/days/builds — M6 measured up,
08.10 measured down, chat bottom has its own geometry). Since 16.10 the
primary method is dump-bounds tapping (N36) — no guessing at all; the
cluster-combing below is fallback. Rule: never trust
direction — recalibrate per screen/session (pointer_location or 1 probe
tap with md5 check), take small targets with a vertical ±100 cluster at
step 50, treat popup menus as 1:1 until proven otherwise. Bottom chat
targets (send, New Chat): hitboxes ~40–100px ABOVE glyphs; a miss looks
like a "dead button" (no ripple) — comb vertically, don't fix code.
Tapping ⌨ enables the "ADB Keyboard {ON}" strip. Announcement button
text CHANGES between builds ("I see" / "Got it" / "Understood") — aim by
position, not text. Skip in Environment Setup = onBack (terminal does NOT
start!); sessions are created by "+" (working point 678,195).
**Market:** Packages → pink store icon → Agreement dialog (first a
"Please wait... (N)" countdown, button inactive) → wait for active
button → SCROLL text to the end → 3-tap cluster at step 70 on the button
(single taps missed twice 07.10). Consent survives screen restart.
**Tool runs on Standard (model-driver acceptance):** EVERY tool call
raises Permission Request (Deny / Allow / Always-allow link); one-shot
Allow is NOT remembered across different tools — dialog cascade. For
acceptance send TRIVIAL tool-free turns ("Say PONG."); if the driver
loops tools (counter 39, burns API) — Deny + red X (stop is instant even
from under a dialog: Deny first, then X). A cancelled turn is NOT
observed by supervision (hook only on clean finalize — by design), the
log keeps `error='cancelled'`.
**Don't trust `files/usr/bin` looks:** applet symlinks (`ash`, `tar`...)
are created blind and dangle when binaries are absent — check TARGETS
(`ls -la`), not names. Empty `lib/arm64/` in the install = PM didn't
extract .so (`useLegacyPackaging=false`) = dead terminal, see ledger.
`ls /data/app/...` — exact path from `pm path` only (`~~*` doesn't expand
via adb shell). `du` on the proot tree lies on mount stubs:
`ubuntu/sdcard` (mode 000) and `ubuntu/storage/emulated/0` are
unreachable from outside — NORMAL (passable inside proot with `-0`), not
data loss/bug. `cp -a` across them exits with errors — verify result
size, not exit code blindly. **Terminal bash stdout via
`adb shell run-as` is LOST** — use `adb exec-out run-as ...` (no pty)
for proofs. **APK contents ONLY via `python3 -c zipfile`**. **Crashed
JVM-test traces live in the HTML artifact:** workflow uploads
`operit-android-test-reports-<N>` (name via
`gh api repos/.../actions/runs/<run>/artifacts --jq '.artifacts[].name'`),
`gh run download <run> -n <name> --dir ...`, traces in
`app/build/reports/tests/testDebugUnitTest/classes/<Class>.html` in
`<pre>` tags (python+re+html.unescape; NO XML results in the artifact).
`--log-failed` gives one-liners only.
**Fresh install = clean field:** chats wiped, supervision OFF
(`supervision_preferences.xml` absent, default false), model preset
re-seeded from the DEBUG branch, announcement dialog pops over the first
screen (button text drifts — aim ~548,1113). Session-init timeout 30 s
VS minute-long unpack: `Session initialization timeout` kills pty with a
half-unpacked tree — retry and WAIT, progress visible via `proot-distro/`
through `run-as`. Battery: `dumpsys battery` FIRST; brightness 1
(`screen_brightness 1` + manual mode); heavy work (unpack, copies) only
with headroom — the phone died at 1% mid-work. Every CI debug build =
new key: uninstall → install → `lastUpdateTime` → wizard again. Wizard
SURVIVES reboot/death (consent kept).
**Clocks.** Don't trust status-bar deltas across far-apart commands: the
session may pause 8–9 h (model limits) while clocks run true — the M6
jump was exactly such a pause, user-confirmed. Durations ONLY from log
stamps (`files/logs/operit.log` via `run-as cat`); `adb shell date` at
session start for order. Battery over M6: 69 → 39%.
**No-Gradle tests** (no SDK): `/tmp/opencode/kotlinc/bin/kotlinc` +
`JAVA_HOME=/tmp/opencode/jdk`, cp: `json.jar junit.jar hamcrest.jar
kotlinx-coroutines-core-jvm.jar kotlin-stdlib.jar (+ okhttp.jar okio.jar
for translation files)`, stubs from `/tmp/opencode/stubs`
(`AppLogger.kt`; compile .java stubs with javac separately — kotlinc
emits no .class for .java). Run: `java -cp <out:jars>
org.junit.runner.JUnitCore <TestClass>`. Test Kotlin files ASCII-only:
`grep -nP '[^\x00-\x7F]'` must be empty (or CJK-NEW in audit).
`\uXXXX` in Edit commands EXPANDS to literals — write CJK tests only via
a python converter, no backslash+u+hex substring in the command (build
via `'\\u%04x' % ord`, grep before and after).
**Audit** — gate is the REAL exit code of
`python3 ci/script/fork_audit.py` (not `$?` of grep). CJK-NEW must be 0
(Chinese even in KDoc/tests fails review); `stringResource()+...`
concatenation banned (LABEL-CONCAT) — format resources `%1$s` only;
registry.json anchors must exist (edit with code: ANCHOR-LOST).

---

## Project reading order

Don't start with `AGENTS.md` or code. The order is chosen so that by the
end you know WHY everything is written the way it is, or you'll fix the
wrong thing.

| # | File | Why this one |
|---|---|---|
| 1 | **this file** | state, decisions, device/CI cheat sheet, open items |
| 2 | `docs/FORK-REGISTRY/index.md` | what to run and what each check code means |
| 3 | `docs/FORK-REGISTRY/registry.json` | ~26 fork edits, each with `rationale` and `anchors` |
| 4 | `docs/FORK-REGISTRY/re-audit-l10n.md` | **corrected diagnosis.** Main doc if Chinese reappears |
| 5 | `docs/FORK-REGISTRY/re-audit-prompts.md` | which prompts the user sees and which not |
| 6 | `docs/FORK-REGISTRY/re-audit-layout.md` | why English breaks layout and what's still unfixed |
| 7 | `docs/TODO/fork-l10n-runbook.md` | CI+device mechanics. **Diagnosis inside is wrong**, marked in header |
| 8 | `docs/TODO/remote_content_translation/index.md` | server-Chinese plan, **fully implemented** (T1 + Market + T2 batch + T3 fallback, incl. announcement) |

Files you'll edit when you reach the tasks:

> SUPERSEDED 2026-10-03: `SttModelRepository.kt` and `SherpaSpeechProvider.kt`
> deleted (M5 `e763e8ef`). Current map: engines —
> `api/speech/GigaAMSpeechProvider.kt` + `api/speech/ParakeetSpeechProvider.kt`
> (both full SpeechService clones), recognizers —
> `api/speech/GigaAMRecognizer.kt` (CTC) + `api/speech/ParakeetRecognizer.kt`
> (TDT: encoder + fused joint, greedy loop) + `api/speech/ParakeetFrontend.kt`
> (nemo128 mel) + `api/speech/ParakeetVocabulary.kt`, checkpoint config —
> `api/speech/CtcModelConfig.kt` (generic), built-in manifests + storage +
> download — `data/speech/GigaAMModelFiles.kt` / `SttModelStorage.kt` /
> `GigaAMModelDownload.kt` and the `Parakeet*` triple (leaf
> `stt_parakeet`), HF resolve + registry —
> `data/speech/HfCtcModel.kt` + `data/speech/CustomSttModels.kt` (CTC
> custom checkpoints only; Parakeet is preset-only), file transcription —
> `api/speech/LocalSpeechTranscriber.kt` (engine/mode params) +
> `api/speech/WavFileReader.kt`, translation —
> `data/translation/CachedTranslator.kt`, storage spots —
> `util/AppDataLocation.kt` (+ `migrateFrom`), space metering —
> `util/AppStorageUsage.kt`.

```
app/src/main/java/com/ai/assistance/operit/
  util/LocaleUtils.kt                     ← default language, the whole English-only decision lives here
  util/RemoteAssetFetcher.kt              ← shared downloader: resume, Range, SHA-256
  data/speech/SttModelRepository.kt       ← DELETED, see note above
  api/speech/SherpaSpeechProvider.kt      ← DELETED, see note above
  api/speech/SpeechServiceFactory.kt      ← engine picker
  api/speech/SpeechService.kt             ← contract to honor
  ui/features/settings/screens/SpeechToTextScreen.kt   ← recognition settings UI
  app/src/main/res/values/strings.xml     ← the only string bucket, English here
  app/build.gradle.kts                    ← localeFilters, useLegacyPackaging=true (:528, cddb9906; see E2 — NEVER flip blindly), deps
  terminal/                               ← SUBMODULE, fork lagovi/OperitTerminalCore
```

---

## Closed work — see ARCHIVE.md

## Negative results — do not repeat (ledger)

Each item: hypothesis → method (how verified) → evidence → verdict.
Re-verify only on new facts (different phone, different Android,
different SELinux policy). Full protocols — via links; here the minimum
sufficient to NOT redo.

- **E1. R8-minified debug (FAIL).** Hypothesis: shave ~40 MB via minify.
  Method: `isMinifyEnabled + isShrinkResources` in `debug` (`f37ff8a1`),
  CI `37206856650` green, APK 373487996 → 333371794 (−40.1 MB).
  Phone evidence: startup crash `Application Error`,
  `java.lang.NoSuchMethodError: ...QuickJsNativeHostDispatcher;.onCall
  (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;` from
  `QuickJsNativeBridge.nativeCreate` (`QuickJsNativeRuntime.kt:55` ←
  `OperitQuickJsEngine.kt:29`). `app/proguard-rules.pro` had NO keep for
  the JNI class (only `JsEngine$JsToolCallInterface`). Verdict: REJECTED,
  revert `ce29250f`. Bonus: `release`/`nightly` share the minify config —
  presumably broken the same way (minified never run on device, NOT ONCE).
  Next step was task 4 (keep-fix), not "enable and see". RESOLVED 07.10:
  keep-fix (`b4ff4a30`) worked — CI `37551624980` success, APK −40.1 MB,
  clean cold start, `onCall` in dex. Details in ARCHIVE.md task 4.
- **E2. `useLegacyPackaging=false` for installed size (FAIL, useful).**
  Hypothesis: drop 152 MB of extracted .so. Method: flip (`bf48f418`),
  CI `37086798853`: APK 293 → 386 MB (+93), installed 435 → 369 MB (−66).
  Delayed evidence (05.10, Q1): phone `lib/arm64/` EMPTY,
  `TerminalManager.linkNativeLibs()` looks for .so only in
  `nativeLibraryDir` → zero links → `execve(.../files/usr/bin/bash)
  failed: No such file or directory`, exit 1, black screen. Repair B1
  (submodule `26d4964`, extract 5 .so from APK to `files/usr/bin`, CI
  `37259656106` green): links created, but `execve bash: Permission
  denied` — files `-rwx--x--x` `app_data_file`, `run-as` (domain
  `runas_app`) executes (EXIT=0), the app (`untrusted_app`) doesn't;
  `/data` has no `noexec`, avc suppressed. Verdict: Samsung forbids exec
  from `files/`; terminal binaries must come from PM extraction. Revert
  `cddb9906` (CI `37299257049`, APK 287 MB), acceptance 15:12 — live
  shell. B1 (`26d4964`) was kept as a sleeping fallback until 18.10, then
  dropped with the pin `ecb93f2` (N65) — it only fires under the rejected
  `false` flip. Consequence
  for size: native libs untouchable while the terminal execs prebuilds;
  any new `false` flip first proves exec on this Samsung. METHODOLOGY
  (matters for future measurements): epochs 435/369 measured with
  `adb shell du -sh` ONLY on the package dir (`/data/app/~~*/<pkg>`,
  exact path from `pm path`; `~~*` doesn't expand via adb shell):
  split 435 = base.apk 297 + lib/arm64 152. `/data/data/<pkg>` (DB,
  files, unpacked rootfs tree 358 MB!) was NOT counted — the true full
  footprint is bigger. Installed recipe: `du -sh` BOTH trees (package
  dir + `/data/data/<pkg>`) on the same install, quote both numbers
  separately, or epoch comparison is invalid.
- **E3. Rootfs to external storage (FAIL, both locations).** Hypothesis:
  358 MB tree lives on SD. Method: `busybox cp -a` (app uid, `run-as`)
  from `files/usr/var/lib/proot-distro/installed-rootfs/ubuntu` to
  EXTERNAL_APP (`/storage/emulated/0/...`, FUSE), then `ln -s` probe to
  SDCARD_APP (`/storage/5982-1724/...`, 980 MB card). Evidence: hundreds
  of `can't create symlink ... Operation not permitted` (`etc/ssl/certs`,
  `etc/alternatives`, merged-/usr `bin/lib/sbin`, `dev/std*`); on SD
  `ln: Permission denied` immediately, dir empty. `-L` workaround rejected
  by math: ~700 MB dupes (ESTIMATE, not measured: tree size ×2 minus
  dedup gain — won't fit 980 MB card with headroom) + dangling links
  (`dev/std*`, `mtab`) dead anyway. Verdict: rootfs stays internal;
  SD mechanics for clean data only (subpack/apktool). Garbage
  (`q1_ubuntu_ext`, `qtest`) removed from phone.
- **E4. `uiautomator dump` (FAIL, tooling).** Method: dozens of calls —
  stable SIGKILL, exit 137, 0-byte xml; not fixed by sleep/reconnect.
  Verdict: `screencap -p` + PNG read only (historical note: dumps work
  from the current lab phone — E4 describes the OLD device).
- **E5. Dhizuku (SKIP).** Needs device-owner, fails on a phone with
  accounts; Operit expects the Shizuku API anyway. Took Shizuku 13.6.0
  directly, recipe in ARCHIVE.md Shizuku section.
- **E6. Others' archives (WONTFIX).** `jniLibs.zip`/`subpack.zip` on
  upstream Google Drive (`prepare_android_dependencies.py`) — can't add a
  file there; Q2 hosting must be ours. `sherpa-onnx` JNI unreachable the
  same way — hence GigaAM runs direct via ORT, not sherpa (variant (b)).
- **E7. Catching 5-second toasts with single screenshots (FAIL, method).**
  Hypothesis: "send a turn, wait, screenshot — see the toast". Method
  06.10: ~50 `screencap` around 3 proven posts (`llm_io_log` ok verdicts
  + non-empty comments, `timestampMs`+`latencyMs` give the exact window)
  — 0 hits: `estimateToastDurationMs` (~5 s per short comment) is shorter
  than a typical manual-screenshot interval. Verdict: toasts catchable
  only via a continuous back-to-back `screencap` series (~2 s/frame,
  20–30) + `md5sum` dedup (identical glued, different read). Compute the
  post window from the log row, don't guess.
- **E8. Checking `llm_io_log` without WAL (FAIL, method).** Hypothesis:
  "pull `databases/app_database` and read rows". Method 06.10: no fresh
  rows — false "hook doesn't write", half an hour of debugging a
  nonexistent bug. Evidence: Room in WAL mode, fresh commits live in
  `-wal`/`-shm`. Verdict: always pull THREE files under one basename
  (`app_database`, `-wal`, `-shm`), open the main one in sqlite3. In
  cheat sheet.
- **E9. flash-lite tool turn as supervision acceptance (FAIL, model
  behavior).** Hypothesis: "a tool turn gives a rich digest and true
  corrected_call". Method 06.10: "What files are in the current
  directory?" → driver looped list_files/use_package/
  super_admin:terminal/code_runner (tool-call counter 39, each new tool
  a new Permission Request, one-shot Allow not remembered); finished only
  via Deny + X. A cancelled turn is NOT observed (hook only on clean
  finalize). Evidence: `llm_io_log` CHAT rows with `<tool_*>` calls,
  `error='cancelled'` on the cut one. Verdict: acceptance with TRIVIAL
  tool-free turns ONLY ("Say PONG."); tool turns are a separate story
  with manual Allow-babysitting and API spend. Design side effect: v1
  digest (tool names without args) would be blind on such turns — args in
  hooks needed all the more (see v1 limit in supervision status).
- **E10. Send-button cluster (FAIL, button mechanics).** Hypothesis:
  "a 3-tap cluster is more reliable than one". Method 07.10: 3 taps on
  send (1410/1447/1480) — the turn left, but `llm_io_log` shows CHAT with
  `error='cancelled'` @403 ms + TITLE_GENERATION cancelled too: the send
  button becomes STOP while streaming, and the cluster's later taps kill
  the just-sent turn. Verdict: send gets EXACTLY one tap, then hands off
  for 20+ s; a cancelled turn yields a Prompt card with empty Response
  (don't confuse with a hang). (Old E11 "dead button" merged into N4.)
- **E12. A/B "094027Z signs, 132740Z doesn't" (FAIL, artifact).**
  Hypothesis 10.10: 15-code broke signing (pre-15 builds, post doesn't).
  Method: keytool -printcert on the output `WebApp_*.apk` after runs on
  both builds. Evidence: the "signing" verdict for 094027Z came WITHOUT
  name/time check of the output file — and `/sdcard/Download/Operit/
  exports/` SURVIVES reinstalls (memo N13): keytool almost surely checked
  a STALE signed file from a past epoch and credited the current build.
  Control: no clean 094027Z run checking exactly the fresh file ever ran.
  Verdict: A/B invalid, 15-code innocent (proven otherwise — task 17:
  root in `a2764e9c`, before 15-code). Rule: keytool ONLY on the file
  from the current run (name from log + ls -la time), or trap (N13).
- **E13. Start Setup with default selection (FAIL, silent no-op).**
  Hypothesis 11.10: "pressed Start Setup — so it installs" (terminal
  showed Interrupt, looked like downloading). Method: after tap —
  apt history.log + `su -c ps` + byte-wise rootfs ls + `NodeJS环境检查`
  log. Evidence: history.log ends at `apt upgrade` 16:44 UTC (before the
  tap — that's setup-check on SCREEN OPEN); zero apt lines after the tap;
  only idle bash in processes (no apt/curl/node); check log 20:13–22:30
  stable `pnpm:false, python:true(preinstalled), pip:false`. Code cause
  (`SetupScreen.kt:143-207`): selected=true is set ONLY for found
  packages, and found ones are skipped by the `!= INSTALLED` filter —
  with defaults only pip-mirror echos go into commands. The screen
  instantly returns home with eternal Interrupt (no completion flag —
  fire-and-forget `sendCommand`), indistinguishable from success.
  Verdict: without manual checkbox choice (verify checked=true in dump —
  blind taps toggle!) setup is a no-op; fix by choosing nodejs+python
  categories + a real run (task 6).
- **E14. JKS keystore on Android (FAIL, platform).** Hypothesis: "PKCS12
  fell — try JKS". Method: `loadVerifiedKeystore` iteration over both
  types + `Security.getProviders("KeyStore.JKS")` in a live process.
  Evidence: NOBODY offers JKS (`offered by: null`, old-style "JKS not
  found") — the provider doesn't exist on Android, period; that's normal,
  not an error. Verdict: fix only the PKCS12 path; leave the JKS branch
  in code as is (it documents the absence), burn no time.
- **E15. "bundled-BC insert works" (FAIL, assumption).** Hypothesis
  (`a2764e9c`, 10.10): inserting bundled-BC first is enough, don't touch
  the platform. Method: log the insert's return code in a live process.
  Evidence: `insertProviderAt` → `-1` twice (20:40, 00:36): while the
  platform holds the "BC" name the insert is rejected — the code ran on
  bare platform BC, which lacks PBES2 (proven by direct SecretKeyFactory
  probe + `openssl pkcs12 -info` of the file: PBES2/AES-256). Verdict:
  ALWAYS log the insert outcome + verify the slot class
  (`Security.getProvider("BC").javaClass`); a taken name — only via
  remove+insert (upstream mechanics + class keeps, task 17).
- **E16. Isolated libc++_paddle soname (FAIL, 14.10).** Hypothesis: the
  process already holds an old libc++ under the same soname, so dlopen
  binds OpenCV to it and dies on `__sfp_handle_exceptions` — isolate the
  pair: byte-wise .dynstr patch `libc++_shared.so`→`libc++_paddle.so`
  (exactly 16→16 bytes) in both .so. Method: python patch asserting the
  single occurrence + ELF check (DT_SONAME/DT_NEEDED honestly point at
  each other — proven by parsing), upload to release `ocr-paddleocr-v1`,
  strict load order (cxx first, then opencv). Evidence: phone died with
  the SAME `cannot locate symbol`; `maps` showed `libc++_paddle.so`
  loaded and executable (loading worked!); full dynsym scan of both
  AAR libc++ (2330 symbols) — the symbol is NOWHERE (the match was in
  .comment, not export); phone system `libc++.so`/`libc.so` — neither.
  Verdict: NO libc++ exported the symbol — there was nobody to isolate;
  patch reverted, assets deleted from the release (`delete-asset`),
  official 4.5.5 taken (proven era-match). Lesson: first prove WHO
  exports, then isolate the importer — not reverse.
- **E17. R8 vs JNI-only ctor (FAIL→FIX, 14.10).** Hypothesis (silent):
  since `ai.onnxruntime` already ships minified for STT, no keep needed
  (SIZE instinct). Method: keep only `com.paddle.ocr.**`. Evidence:
  first OCR run — instant process death with NO FATAL and NO Java trace;
  logcat only `F/<pkg>` + `WIN DEATH`; root cause in the first lines of
  the DEBUG dump: `JNI DETECTED ERROR IN APPLICATION: JNI NewObjectV
  called with pending exception NoSuchMethodError:
  OrtException.<init>(ILjava/lang/String;)V` — native tried to build an
  exception for a managed load error (det IR 10 vs ORT 1.17.1), while R8
  removed the ctor as "unused" (nobody calls it from Java — only
  native). Verdict: verbatim SDK keep (`ai.onnxruntime.**`) + ORT bump
  to 1.21.1 (needed anyway: IR 10 requires ≥1.19). Rule: anything
  constructed ONLY from native is dead to R8 — keep without discussion;
  hunt native aborts without FATAL as in (N26).
- **E18. "Model clips line tails" (FALSE ALARM, 14.10).** Hypothesis:
  Paddle deterministically drops endings (`1234567890`→`123456`,
  `булок,`→`бул`) — det box squeezes edges; candidate fix
  `detUnclipRatio` 1.5→2.0. Method before fixing: open the pictures WITH
  EYES (Read tool). Evidence: both PNGs are clipped by the canvas itself
  (1200px wide, the line runs off-edge — pixels `7890`/`ок,` are NOT in
  the file); model output == visible pixels letter-exact; 2x upscale
  changes nothing (not resolution). Verdict: model right, truth crooked
  — `.txt` fixed to pixels, unclip hypothesis DROPPED (recorded so it
  never gets "fixed" again); Paddle score 12/12 + Russian 3/3. Rule:
  fixtures with eyes BEFORE tuning models; truth files derive from
  pixels, not source strings.
- **E19. Binaries in repo root (MISTAKE, 13.10).** Mechanism: `cd
  /tmp/opencode && curl -o det & curl -o rec & curl -o aar & wait` —
  `&&` bound `cd` only to the first curl, the other two downloaded into
  cwd (repo root): `opencv.aar` 54 MB + `paddle_rec.onnx` 8 MB;
  `git add -A` put them in the commit (`96e27adb`). Evidence:
  `git status --short` (always read). Cleanup: `git rm` +
  `git commit --amend` (not pushed yet) + `git show --stat HEAD |
  grep -E 'aar|onnx|\.so'` check. Rule: background `&` commands only
  with absolute `-o`; binary guard before push — habit.
- **E20. "SOCKS test goes DIRECT" (FALSE ALARM, 14.10).** Hypothesis:
  Test Connection under a SOCKS5 config goes direct (trace stubbornly
  showed `route=192.168.1.55:20128`) — i.e. factory/tester ignore the
  proxy; candidate root cause: own client bypassing `AIServiceFactory`.
  Method: kill-test (drop 11081/11080 → read the error). Evidence: the
  tester goes THROUGH `AIServiceFactory.createService` (code), persisted
  config was SOCKS5/11081 (DataStore bytes), and with a dead proxy the UI
  honestly shows `Failed: failed to connect to /192.168.1.55 (port
  11080)` — traffic goes via proxy, no leak; `route=` for SOCKS routes in
  this OkHttp version prints the target address, not the proxy (display
  quirk, not routing). Verdict: don't trust display fields, prove routing
  with kill-test + the proxy's own log (CONNECT).
- **E21. Garbage in proxyHost (MISTAKE, 14.10).** Mechanism: DataStore
  showed `proxyHost = "192.168.1.55ýýþþþþ"` (field never touched by hand;
  earlier `⁹` caught in username). Likely cause: swipes over an open
  keyboard typed chars into the focused field. Evidence: byte-wise parse
  of `.preferences_pb` (JSON inside; defaults omitted — see (N34)②).
  Cleanup: field cleared and retyped by hand via UI; result — 0 proxy
  fields in DataStore, config as-was. Rule: kill IME before swipes (see
  (N32)), verify field values by eye after every gesture series.
- **E22. Lab proxies block LAN (ENV FAIL, 14.10).** Hypothesis (first):
  our HTTP client goes through proxies crookedly (Test Connection via
  .55:18080 gave `unexpected end of stream`, DIRECT — Success). Method:
  same request with curl from the box, direct and via proxy. Evidence:
  direct — 401 (gateway alive, wants key); via :18080 — 000 in 0.07 s
  (not a timeout — reset); verbose: `example.com` via proxy gives 200,
  but `192.168.1.55:20128` — `Empty reply from server`; SOCKS5 :10808 —
  same picture (outside 200, LAN 000). Verdict: code innocent — proxies
  filter private addresses (anti-SSRF policy); nothing to fix, prove
  end-to-end with our own stand (see (N35)). Rule: "proxy works" and
  "proxy admits the target" are two different facts — verify both before
  blaming code.
- **E23. Own stand broke no-auth (STAND FAIL, 14.10).** Hypothesis
(silent): a forwarder demanding auth when 0x02 is offered is a normal
strict proxy. Method: updated socks5_fwd (pick 0x02 when present) +
Test Connection without creds. Evidence: UI —
`Failed: SOCKS : authentication failed`; cause — JDK SOCKS ALWAYS
offers {0x00, 0x02} (see (N34)③), and a real server must prefer 0x00
when the client offers it. Verdict: stand fixed (pick 0x00 when
present; REQUIRE_AUTH only as a separate flag/port for the auth test);
side win — the UI error names the cause verbatim, diagnostics are good.
- **E24. Playing audio into the phone mic (DEAD END, 15–16.10).**
Hypothesis: play benchmark_v2.wav from the notebook/phone speaker into
the mic (methodology candidate 1 for the STT benchmark). Method:
checked every player: box has none (`aplay/paplay/ffplay` absent);
phone has no music player (VIEW intent offers only Operit EN +
Termux); Termux has none (`mpv/ffplay/sox` absent); `tinyplay` cannot
open PCM (`cannot open device '/dev/snd/pcmC0D0p'` — AudioFlinger owns
it); `cplay` permission-denied. Verdict: speaker playback needs a
player installed first (`pkg install sox` in Termux is the cheapest
path) — until then, file input goes through code (`transcribe_file`,
`17007bad`), not air.
- **E25. Parakeet whole-file on 37.5 s kills the app silently (16.10).**
Method: Tool Tester "Para Transcribe Whole" on benchmark_v2.wav (37.5 s).
Evidence: ~20 min of decoding, then the launcher on screen, app process
gone (ps=0); NO dropbox crash entry (last is 10-14), no FATAL, app log
ends mid-run — classic LMK kill, not a code crash (2.8 GB RAM phone,
652 MB int8 + full-sequence conformer activations in one session.run).
Short whole files work fine (EN 3.8 s whole = perfect, 25 s). Verdict:
size-dependent OOM, not a whole-path bug. Fix (`f0ad404a`): whole mode
refuses Parakeet audio over 20 s loudly ("file is 37.5 s, whole mode
supports up to 20 s on this phone; use chunk mode" — verified on device
as clean FAILED + app alive; also fixed a `.format`-on-second-literal
bug, `5c4c627e`). 20 s matches MAX_UTTERANCE_SAMPLES: production never
decodes more at once.
- **E26. Reinstall wipes `Android/data/<pkg>` on the SD card too (16.10).**
"SD survives reinstall" holds only for shared storage
(`/sdcard/Download`). After the guard-fix reinstall both models were
gone from the card and had to be re-downloaded (670 + 225 MB). Never
assume on-card payloads across installs; re-verify with `ls`.
- **E27. Shared global location strands the second payload (16.10).**
`AppDataLocation` has one setting for all payloads. Moving Parakeet to
SD flipped it; GigaAM's files stayed internal while its section read
SD → "not downloaded", and its own Change→SD was a no-op (`from ==
target` → true, nothing moved). Fixed (`f0ad404a`): new
`migrateFrom(source, …)` + both STT storages move files from where
they actually are (holder search); existing `migrate()` behavior
unchanged. The GigaAM files were ferried by hand (run-as cp) before
the fix shipped.

## Open questions 1–2 — closed, history in ARCHIVE.md
  (live: `docs/FORK-REGISTRY/research-size.md`).

## Rules I followed that break the build

- `app/src/main/res/values/strings.xml` — the **only** string bucket, and
  it's English. Qualified locales don't exist at all. A key added to
  `values-en/` points at a nonexistent resource.
- `stringResource` is banned in `try/catch`, in `LazyListScope` lambdas,
  and must NOT take `Any?` — it takes `Any`. Resolve nullables in place
  with `?:`. Caught this twice already.
- Labels assemble via format resources, not concatenation. Both forms
  (`stringResource(X) + ":"` and `"${stringResource(X)}: $v"`) are gate-
  checked (LABEL-CONCAT).
- `localeFilters` lives on `androidResources`, **not** `defaultConfig`,
  not buildType. Cost two CI cycles.
- The project bans fallback logic. `runCatching {}` with silent `""` does
  not count as "error handling" — it's exactly the bug-hiding class.
- Comment edits in English, even next to Chinese ones.
- `out/` is gitignored, never commit APKs.

## Don't

- Don't run `check_strings.py` expecting "英文: 完整" — that label is gone,
  the source is called `default`.
- Don't trust `count_ui_strings.py`: it doesn't walk `:terminal`, which is
  why 65 strings lived there unnoticed. Replacement — the `CJK-NEW` audit
  check.
- Don't fork the `terminal` submodule back to upstream. It's on
  `lagovi/OperitTerminalCore`; it has an `upstream` remote for merges.
- Don't commit until `python3 ci/script/fork_audit.py` is green.

---

## Test phone policy (2026-10-02, user directive)

Device `R9TN601D6GJ` (adb) is a dedicated test phone, fully managed by the
LLM agent. Anything may be done with it without asking the user first:
uninstall (wiping data), install, change settings, download models, run
acceptance. Do not ask for confirmation for device operations on this phone.

## Acceptance histories T1–R8, CTC, deferred/dropped — see ARCHIVE.md


