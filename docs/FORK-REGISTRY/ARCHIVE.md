# HANDOFF Archive (moved out of HANDOFF in the 11.10 cleanup)

> **Language rule (user directive, 15.10): talk to the user in Russian.
> All agent-internal docs are in English.**

Closed sections in full. HANDOFF keeps only one-line statuses pointing
here. Do not delete: the negative-results ledger lives in HANDOFF.

---

## Closed tasks 9–18 (bodies from HANDOFF)

   9. Workspace chat — CLOSED 07.10 as upstream behavior (code-reading
   investigation, no install needed). Mechanism: workspace is a
   FULLSCREEN overlay (`AIChatScreen.kt:1243` `isWorkspaceVisible` +
   layout below, `fillMaxSize` over everything incl. the input) at
   `showWebView=true`; enabled ONLY by the Code/CodeOff icon button in
   the topbar (`AIChatScreen.kt:818-843` block, `onWorkspaceButtonClick`→
   `toggleWebView` at `:821` — the only entry in code). Answers:
   (1) upstream, not a regression (all machinery untouched upstream code
   with Chinese comments, zero fork-registry edits on workspace);
   (2) an exit EXISTS — the same Code icon in the topbar (topbar lives
   above the overlay, reachable); do NOT work: BACK (no BackHandler in
   AIChatScreen at all), switching chats (overlay held deliberately,
   `ChatViewModel.kt:689`), arrows/swipes/FAB — all UNDER the overlay;
   (3) chat without workspace — yes, normal (proven by acceptances;
   `isWorkspaceOpen` derives from `history.workspace.isNotBlank()`,
   `ChatViewModel.kt:383-387`). What happened 05.10: the agent apparently
   tapped the CodeOff icon and got a fullscreen preview with no visible
   exit — the classic no-BackHandler trap. No fix needed (upstream), but
   if the trap repeats for the user — file a task for a BACK handler on
   the overlay, anchors here.
   10. Empty-chat UX — ACCEPTED 07.10 (commits `f3ce47f1`+`5cb753fe`,
   CI `37581930601` success, APK dir `2026-10-07_06-31-20Z` + `CHANGES.md`).
   Implementation: bottom bar `AIChatScreen.kt:1094` branches on
   `currentChatId.isNullOrBlank()` — instead of `ChatInputBottomBar` a
   `+ New Chat` button (`R.string.new_chat`, exists), one tap → straight
   `createNewChat()` (setAsCurrentChat by default) → input appears.
   DELIBERATE SPEC DEVIATION: the task said "button opens the Chat
   History menu", built direct creation (fewer taps, honester label);
   menu available via the clock button. Both send guards
   (`AIChatScreen.kt:1822`, waifu `:1693`) stay as safety nets — decision
   recorded here. Acceptance: empty state without false controls
   (screenshot), tap → "Ping Test" chat + input, Say PONG. → PONG.
   ACCEPTANCE TRAP: button hitbox notably ABOVE the glyph (center taps
   at 1330 and cluster 1295–1365 — misses; one of 1150–1400 worked while
   combing; send same story: glyph 1447, hit 1410). Rule for chat-screen
   bottom: aim ~40–100px above visual center; misses look like a "dead
   button" (no ripple), fixed only by combing, not force. ~1.5 h burned
   on the false "onClick doesn't fire" trail — same mechanics as
   calibration: pointer_location shows 1:1, but the hitbox lives higher.
   11. Supervision toast — CAUGHT LIVE 08.10, CLOSED. Turn: supervision
   was OFF (fresh install of task 10) — taps on the toggle (`Settings` →
   Supervision Mode) missed THREE times (taps reach the screen — the
   neighbor row opens; the switch itself untappable, cause unknown — do
   NOT fix the UI, bypass below). Bypass: the toggle is SharedPreferences
   `supervision_preferences.xml`, key `SupervisionPreferences.kt`
   (`supervision_enabled`, default false; NOT in DataStore —
   `functional_configs` holds only bindings); XML written via
   `run-as ... tee` with the app killed, after restart the toggle is
   purple (screenshot). "Say" turn → reply "What would you like me to
   say?" → 25-frame burst: frames 3–7 — white card "The driver correctly
   asked for clarification." with ×, frame 8 — translucent fade, then
   clean field. In DB (`app_database` + `-shm` + `-wal` — all three!)
   row id=8 SUPERVISION verdict=ok, comment matches the card verbatim.
   DoD closed: PNGs (`/tmp/opencode/t11/toast_proof.png`, fade
   `toast_fading.png`) + id=8. Proofs in /tmp (won't survive a notebook
   reboot — re-pull if needed: toast lasts ~5 s, burst method
   reproducible). History (how we caught it): render — stock
   `ChatToastHost` (`AIChatScreen.kt:1459`, top center, always mounted),
   ~5 s duration (`estimateToastDurationMs`); method — back-to-back
   `screencap` burst ~2 s/frame, 20–30 frames + `md5sum` dedup (E7 + "Toast
   hunting" cheat sheet); NOT every turn yields a comment (empty = nothing
   to say = no toast by design).
   12. Q1 items 3–4: apktool.toolpkg and desktop/helper/templates (was
   OPEN; internal order: 3(a) first, then 4(b); CI ~25 min per round,
   acceptance — charged phone).
     (a) apktool.toolpkg (~26 MB): DECISION 08.10 — DELETE (not move).
     Recon showed: reads from assets only
     (`PackageManager.scanAssetPackages:1261` →
     `loadToolPkgFromAsset:2029` unpacking the WHOLE archive to cache
     `prepareToolPkgAssetCache:1993` — i.e. ~27 MB in APK + ~27 MB cache
     for a feature `enabled_by_default: false`); external `.toolpkg`
     import already works (`PackageManagerScreen:320` +
     `scanExternalPackages:1276`); `reconcileToolPkgCaches:902-944`
     deletes stale cache of old installs itself; zero code refs to the
     bundle (`OfflinePayloads.kt:19` was the only one), zero tests. Move:
     `apktool` crossed out of
     `tools/example_packages/packages_whitelist.txt` (CI sync `normal`
     mode no longer packs; the script deletes stale outputs itself;
     `test`-mode pr-check packs everything by construction — unbroken;
     `npm run build:examples:github` touches only the github example).
     `examples/apktool/` untouched. DoD CLOSED 08.10: CI `37722980127`
     success, APK 247299182 → 220307769 B (−26991413 B — matched the
     26985716 B `apktool.toolpkg` to byte precision), `assets/packages/`
     now .js only (1429723 B), zero `apktool` occurrences in APK
     (`python3 zipfile`). Dir:
     `/home/uzzzver/operit-fork/apk/2026-10-08_03-29-43Z/` + `CHANGES.md`.
     Never installed on the phone (nothing in APK deletion can break:
     zero code refs, absence acceptance optional at next install).
     (b) desktop.apk (6.5 MB) + helper APKs (shizuku/accessibility ~3–5 MB)
     + templates/emoji/js (~14 MB): DISSECTED 08.10, different decisions.
     `desktop.apk`+`desktop_version.txt` — DEAD WEIGHT (zero refs in the
     whole repo except size tables; `assets/README.md` documents only
     shizuku): deleted via `git rm`, DoD CLOSED — CI `37726800230`
     success, APK 220307769 → 214210621 B (−6097148), one `desktop*`
     occurrence left is a widget xml (not it). Dir `2026-10-08_04-17-45Z/`
     + `CHANGES.md`. shizuku.apk/accessibility.apk — ALIVE (read on
     demand: `ShizukuInstaller.extractApkFromAssets:37` → cacheDir →
     install intent; `UIHierarchyManager:89-91` same; versions from
     `*_version.txt`): not moved in Q1 (no hosting = APK doesn't shrink,
     code out of proportion to 5 MB) — go to Q2 on-demand (task 13),
     reader map already here. templates (9.8 MB, 187 files) — project
     seed: `WorkspaceUtils.copyTemplateFiles:682` copies `templates/$name`
     from assets to workspaceDir on creation (runtime reads files, not
     assets) — Q2 too (download seed instead of bundle). emoji (3.6 MB) —
     `CustomEmojiRepository:298` (`emoji` assets dir) — Q2. js/ (1.4 MB:
     pako/CryptoJS/Jimp/UINode/OkHttp3/katex/terser — `JsAssetLoader`,
     `JsEmbeddedLibraryLoader`, `LatexMathMlConverter:10`,
     `ToolPkgJsAstMinifier:50`) — HOT JS-engine runtime, offline: do NOT
     touch at all (neither Q1 nor Q2). BAN honored: `examples/apktool/`
     untouched (that was (a)). (c) Do NOT touch (standing): OCR/engines
     (separate decision), `lib/` and dex (untouchable — E2), rootfs
     (FAIL — E3, Q2 only), aapt2 doubles (wontfix — breaks
     `setup_android_env.sh`, SIZE-006), js/ assets (hot runtime, above).
     desktop.apk dissection 08.10 (from archive `2026-10-08_03-29-43Z`,
     unpacked to `/tmp`, never returned to repo): 161 entries, 6.5 MB;
     classes.dex 13.5 MB + classes2.dex 4 MB (non-compact), obfuscated res
     names (`Qr.xml`, `-6.webp` — R8), Kotlin+Compose, only native is
     `libandroidx.graphics.path.so` (4 ABIs); dex strings:
     `com.ai.assistance.operit.desktop` (`DesktopScreen`, `AppIconCard`,
     `OperitDesktopTheme`) — an upstream companion launcher attached to
     nothing: no installer, no intent, no docs. Deletion re-confirmed —
     do not restore.
   13. Q2 on-demand: payload out of APK entirely (was OPEN, after 12;
   hosting RESOLVED 08.10 — GitHub Releases in THE SAME repo, see 7c).
   SUBPACK DONE 08.10 (commit `a8d61d75`, CI `37772193743` success):
   `OfflinePayload.SUBPACK_ANDROID/WINDOWS` (`util/OfflinePayloads.kt`,
   URL `.../releases/download/v1.12.1+4/...`, size+sha from APK CI
   `37726800230`), `SubpackStorage.ensureTemplate` (leaf,
   `RemoteAssetFetcher`, Q1 copies recognized by size+sha — no re-download,
   refusal returned into `onComplete`, NO fallback to assets);
   `ExportDialogs` both flows moved to provision-then-use (android:
   `fromAsset`→`fromFile`, progress 0.05–0.30; windows: staged copy to
   `windows_export_temp`, rest of flow untouched); CI no longer pulls
   `subpack.zip` from Drive (`download_android_dependencies.sh` +
   `prepare_android_dependencies.py` cleaned, cache keys hash the scripts
   — they rebuild themselves); release `v1.12.1+4` created TOGETHER with
   the code (both files uploaded, sha matched the baked ones); APK
   214210621 → 177474049 B (−36736572, `assets/subpack` empty — verified
   `zipfile`), dir `2026-10-08_11-45-43Z/` + `CHANGES.md`. DoD NOT closed:
   clean install + export to the end (phone 08.10 daytime UNAVAILABLE:
   ports 36255/38571/5555/37000–45200 closed — need a fresh port from the
   user). Q2 remainder: helper APK (map in 12b), templates/emoji — separate
   passes with the same template (payload object → ensure → caller →
   release → measure); `apktool.toolpkg` already out of APK (12a) —
   hosting it as a download is optional, unneeded for size. Rootfs — last
   and only if proot starts from an external leaf (E3 against; separate
   experiment with rollback). Template for the rest (worked on subpack,
   repeat literally): (a) `OfflinePayload` object (leaf filename = staged
   copy name if there was one — then migration is free; size+sha via
   `python3 zipfile`+sha256 from the last APK that still had the file);
   (b) `ensureX` suspend in the Storage class via `RemoteAssetFetcher`
   (refusal outward, no fallback); (c) caller to provision-then-use
   (pass progress, call inside an already-suspend context); (d) remove the
   source from CI/prepare (cache keys rebuild themselves); (e)
   `gh release create` — ONLY with files, never empty (7c: version tag
   `vX.Y.Z`, one file per payload, short note); note starts with the ALPHA
   banner (100% vibe-coded, stability is upstream's) + `--prerelease`
   flag (user decision 10.10, stands for all fork releases); (f) CI →
   `zipfile` measure → dir+`CHANGES.md` → download acceptance on clean
   install (phone+port). Order: helper APK → templates/emoji (reader map
   in 12b). Reference: `OfflinePayload.GIGAAM` (`util/OfflinePayloads.kt`)
   + downloader `util/RemoteAssetFetcher.kt` (resume, Range, SHA-256) +
   registration in `OfflinePayload.all`.
   LIVE-FIRE 09.10 (build `2026-10-08_11-45-43Z`, clean install; phone
   after bootloader unlock, `192.168.1.69:42323`, root, see cheat sheet),
   battery 61–67%: DOWNLOAD PROVEN — `files/subpack/apk_editor_android.apk`
   48139093 B + sha256 `c56b23a8...` matched the baked ones
   (`SUBPACK_ANDROID`); the phone reached GitHub Releases itself. REPACK
   FAILED: `ExceptionInInitializerError` `ExtraFieldUtils.<clinit>` —
   `AsiExtraField` has no `<init>` (R8 cleaned the constructor; crashes at
   `copyZipEntry:339` `outEntry.time = ...`, flow `ExportDialogs.kt:875`).
   FIX — constructor keep (`app/proguard-rules.pro`, commit `b54dffbe`
   pushed): true cause, not fallback; path audit — no other reflection
   (apksig/apkparser/zip4j already `{ *; }`, sevenz not on the export
   path). CI NOT STARTED: 422 "Actions has been disabled for this
   repository" on both workflows; repo flag enabled=true, repo not
   disabled/archived — block above the repo, owner needed (Settings →
   Actions). No local SDK (`/opt` has only platform-tools) — nowhere to
   build. Acceptance remainder after CI fix: install `-r`, re-show the
   folder with a FINGER in HTML Packager, export to the end + offline
   repeat with WiFi off (leaf already templated — no re-download owed).
   SAF LESSON FOR THE CHEAT SHEET: folder rows and the USE button in
   DocumentsUI are deaf to adb (`tap`/ENTER/SPACE/CENTER — proven by
   combing; TAB/DPAD focus walks, toolbar/dialogs/file taps work).
   Working: "Create folder" via toolbar (tap/ENTER) — the picker enters
   the new folder itself; USE — finger only. User confirmation 09.10:
   "the panel is alive, adb just can't".
   CLOSE-OUT 10.10: DoD FULLY CLOSED. Build `2026-10-10_00-41-11Z` (CI
   `38010108122`, APK 179691879 B): clean install → 48 MB download →
   repack → zipalign → sign → `Download/Operit/exports/WebApp_*.apk`
   48094313 B → `adb install` SUCCESS (`com.example.webproject`);
   offline repeat (internet closed to the app only via iptables
   owner-match — `svc wifi` FORBIDDEN, it kills ADB) successful in
   seconds, logcat shows zero re-downloads, byte-identical size. Finger
   NO LONGER NEEDED AT ALL: root taps via sendevent (`/dev/input/event3`,
   MT-B, 1:1 to dump coordinates) indistinguishable from a finger —
   proven on USE+Allow (`ci/script/phone_lab.sh`, acceptance encoded in
   `ci/script/accept_q2_export.sh`, syntax checked, first full run — on
   the next payload).
   syntax checked, first full run — on
   the next payload).
   14. Onboarding: one screen instead of three (IMPLEMENTED in code 10.10,
   on-device acceptance pending; user decision: long agreement texts out,
   tour behind a separate button). New
   `ui/features/permission/screens/OnboardingScreen.kt`: top card "Your
   responsibility" (`onboarding_liability_*`: all good and bad is the
   user's responsibility, the developer answers for nothing) + "What can
   it do?" button (tour — `FeatureTourDialog`, 3 intro pages, out of
   flow); then 4 base permissions as cards (same intent handlers); then 4
   levels (STANDARD/ACCESSIBILITY/DEBUGGER/ROOT; ADMIN was never offered in
   UI before either); always-active Continue at the bottom — no choice
   writes STANDARD. `MainActivity`: Agreement/Guide branches replaced by
   one (`!accepted || level==null → OnboardingScreen`, onComplete writes
   `acceptCurrentAgreement()` + `checkPermissionLevelSet()` + as before).
   Deleted: `PermissionGuideScreen.kt`, dead strings (welcome_*, warning_*,
   complete, confirm, step labels); `AgreementScreen.kt` KEPT (agreement
   display from Toolbox, `OperitScreens.kt:686`). UI anchor UI-001 moved to
   the new file. Top-up 10.10 (user decision): market modal
   `MarketAgreementDialog` (block on first market entry + "Market
   Agreement" item in Mine) DELETED whole — dialog files and
   `MarketAgreementPreferences.kt`, `market_agreement_*` strings, branch
   in `UnifiedMarketScreen.kt`; instead one sentence in the onboarding
   card (`onboarding_plugins_note`: third-party plugins execute their
   authors' code — install at your own risk). Acceptance script adapted:
   step 3 now waits for "Your responsibility", swipes "Continue" into
   view, taps, waits for "New Chat". First live run 10.10 (APK
   `20261010_094027Z`, CI 38042218605): root/install/grants/onboarding —
   OK (new onboarding works); then two bugs, both in script/flow, not the
   app: (1) `cd` in the script pointed out of repo root — fixed;
   (2) a popped Announcement modal stole taps — by user decision the modal
   DELETED whole (`RemoteAnnouncementDialog` + `RemoteAnnouncementRepository`
   + `RemoteAnnouncementPreferences` + branch in `OperitApp`; the script
   keeps a "Got it" guard just in case); (3) picker: `Select Folder`
   sometimes doesn't open on first tap (sendevent flake, known) — script
   retries up to 3. Acceptance state by night 10.10: PROVEN in three runs
   (APKs 094027Z, 132740Z, 160304Z): onboarding (Continue → chat), picker,
   folder-pick, 48 MB download, repack. NOT PROVEN (needs a green build):
   tour dialog (open/page/close), level choice, permission rows via taps,
   no market/Announcement modal on first entry. Script by night: expectation
   model (`plab_expect_text` — each stage declares the expected screen,
   failure prints screen + FAIL, no hangs), heartbeat in long waits, wake
   + screen_off_timeout with restore-trap, install with cause + retry,
   output-APK basename. CLOSE-OUT 14, night 10.10 (APK `20261010_215026Z`,
   manual session, battery 34→~28%): tour — open/1-2-3/Close (back to
   onboarding); levels — Debug visibly selected (purple frame + filled
   radio, screenshot `/tmp/opencode/t14_levels.png`), then Standard +
   Continue → chat, `android_permission_preferences.preferences_pb`
   records `preferred_permission_level=STANDARD`, consent
   `accepted_agreement_version` in place; Storage Permission row via tap
   opens system "All files access" (toggle ON — appops survives
   `pm clear`); market — pink FAB (hitbox (643,1280), not the glyph!)
   leads STRAIGHT to the feed with no Agreement/countdown (first entry);
   Announcement never popped on fresh installs (×2 runs — straight
   onboarding). DoD FULLY CLOSED. Side: runtime translation alive on
   15-code (`楼层限制器`→"Floor Limiter"+EN description). Phone left: fresh
   215026Z install, onboarding passed, STANDARD, chat ready. Evening run
   10.10 (APK `20261010_132740Z`, CI 38055794385, 15-code): script brought
   to shape (visible steps, adb-timeout 90s, install with cause+retry),
   but acceptance stalled: install flake (manual OK) → rerun died on
   transport (`error: closed`), phone 192.168.1.69 unpingable, adb
   offline, LAN scan for 5555 (.2–.254) empty. Need the user: check the
   phone (power/WiFi/IP). Next: CI build (announcement-removal + logos +
   script) and full acceptance.
   ACCEPTANCE 15 (APK `20261010_132740Z`, CI 38055794385): onboarding/
   picker/folder/download/repack — OK, but signing fell: PKCS12 unwrap
   (SecretKeyFactory PBES2 unavailable) + JKS getInstance fail. A/B vs APK
   094027Z (signs OK) + dex compare (BC classes 5433=5433, refs 7913=7913)
   + keystore md5 matched → not content but first-match: inserted
   bundled-BC shadows the platform one, its service instantiates but falls
   on load, JCA never tries the rest. Fix: `KeyStoreHelper` rewritten to
   loadVerifiedKeystore (per-type provider iteration with alias check,
   final error as before), provider insert/remove gone entirely,
   `ApkReverseEngineer` moved to the new loader. Not a fallback:
   verification mandatory, total failure visible as before. DoD: clean
   install → Continue → chat at once; separately: tour opens/pages/
   closes, levels selectable, permissions tap into system settings.
   15. Chinese integrations — remove ALL (CODE DONE evening 10.10, user
   decision "remove everything" executed; left: green CI build already
   there — `20261010_170331Z` (CI `38070179633`, incl. insert+iterate and
   diagnostics), needs FULL script acceptance on it + the "NOT PROVEN"
   items from task 14. Below — inventory (as before removal) and the
   implementation record at the item end. (All paths from repo root, sizes
   — bytes in APK `2026-10-10_00-41-11Z` via `python3 zipfile` unless
   stated):
   (a) QQ Bot (Tencent QQ OpenAPI, CN-market only: needs QQ credentials):
   `examples/qqbot/` (~520 KB sources), in whitelist
   `tools/example_packages/packages_whitelist.txt` → packed into
   `assets/packages/` + installed to toolpkg_cache; UI — Toolbox tile
   "QQ Bot Settings". Path — like 12a (apktool): cross out of whitelist,
   stale cache cleaned by `reconcileToolPkgCaches` itself, DoD — CI
   measure (`python3 zipfile`, `*qqbot*` zero). UNCLEAR: exact .toolpkg
   size (build with the sync script and weigh before deciding);
   (b) CN-provider presets — REMOVE (zero size, clean interface):
   `provider_baidu/provider_alipay_bailing/provider_aliyun` (`app/.../res/
   values/strings.xml:4138+`), Baidu/Xiaomi endpoints
   (`ApiProviderConfigCollect.kt:90,152`, `ModelConfigData.kt:24`,
   `MimoProvider.kt`, `MimoVoiceProvider.kt`), baidu.com test URL
   (`ToolTesterScreen.kt:326`), alipay: scheme in Token-WebView
   (`TokenConfigWebViewScreen.kt:108`, `WebViewConfig.kt:126`), test_desc
   strings (`strings.xml:3045`). Remove fully: types from
   `ApiProviderType`, UI branches, strings, default endpoints;
   (c) CN-app map — REMOVE (`StandardUITools.kt:65-123,248-249`):
   WeChat/QQ/Weibo/Taobao/Baidu-maps/bilibili/Tencent-video/QQ-music/
   QQ-mail/Tencent-news → package names. Remove whole (an EN user doesn't
   need Chinese-app openers); verify the tool doesn't fall without the map
   (default branch/error instead of the CN list);
   (d) HuaweiCloud-maven in export templates — REMOVE
   (`assets/templates/{android,flutter}/.../settings.gradle.kts`,
   `build.gradle.kts`): a CN mirror; before deleting prove an export-
   project build without it (or roll back);
   (e) Verified NOT Chinese (don't touch): OCR engine — Google MLKit
   `com.google.mlkit:text-recognition:16.0.0` (`gradle/libs.versions.toml:18`,
   `app/build.gradle.kts:649`, `libmlkit_google_ocr_pipeline.so` 9.5 MB,
   NO zh/ja/ko script packs in deps; active model — LATIN ONLY
   (`OCRUtils.kt:31,52`: `latinRecognizer`,
   `TextRecognizerOptions.DEFAULT_OPTIONS`; CJK recognizers are separate
   artifacts, not linked; confirmed by code read 10.10); NO CJK fonts in
   assets (only `terminal/.../jetbrains_mono_nerd_font_regular.ttf`);
   `res/` — only `values`+`values-night` (no values-zh); MNN
   (`libMNN.so` 20.9 MB + Wrapper 5.3 MB) loads a LIVE `llm/mnn`
   (`MNNLibraryLoader.kt:33,37`, `MNNModule.kt`) — removal only together
   with the MNN-LLM feature (user decision, NOT a free win; no dead
   MNN-STT left in code — verified by grep 10.10); NO push SDKs
   (Huawei/Xiaomi/Oppo/Vivo/JPush) in deps.
   IMPLEMENTATION 10.10 (code, in a separate CI build after acceptance
   14): enum `ApiProviderType` −16 (BAIDU/ALIYUN/XUNFEI/ZHIPU/BAICHUAN/
   MOONSHOT/MIMO/DEEPSEEK/SILICONFLOW/IFLOW/INFINIAI/ALIPAY_BAILING/
   DOUBAO/PPINFRA/NOVITA/MINIMAX); deleted Deepseek/Kimi/Mimo/Qwen/Doubao
   provider files + their TTS four (SiliconFlow/MiniMax/Mimo/DoubaoVoice
   Provider) and SILICONFLOW_TTS/MINIMAX_TTS/MIMO_TTS/DOUBAO_TTS types;
   factories (`AIServiceFactory`, `VoiceServiceFactory`,
   `ModelListFetcher`) — branches cut (silent where else was, fixed by
   compiler where exhaustive); collects: `ApiProviderConfigCollect` −16
   presets, `ModelPricingDefaultsCollect` (set+map),
   `ScrapedModelPricingRowsCollect` −142 lines + empty blocks,
   `ModelThinkingConfigDefaultsCollect` −CN rules (DEEPSEEK/MOONSHOT/
   MIMO/DOUBAO/ALIYUN/SILICONFLOW/ZHIPU/opencode-zhipu) + minimax tails;
   QQBot: whitelist line + `examples/qqbot/`; `AutoGlmOneClickToolScreen`
   (Zhipu one-click) + route + Toolbox item + strings; DeepSeek
   token-webview cluster (`DeepseekApiConstants/JsScripts/
   DeepseekJsInterface`, dead); alipay:/weixin: schemes in both webview
   clients; baidu URL and desc in ToolTester; CN-app map in
   `StandardUITools` (whole CN block + QQ/WeChat/TikTok/Temu/bilibili/
   keep); aliyun/huaweicloud/tuna/bfsu/tencent mirrors from export
   templates (gradle + setup scripts, official upstream left); defaults →
   OpenAI (`ApiPreferences`, `ModelConfigManager`, `ApiConfigDelegate`,
   `UrlConfig`, `ModelConfigData`); old configs migrate to OPENAI_GENERIC
   persistently (`migratePreferencesFromVersionFour`, store version 4→5,
   keys/endpoints kept); TTS type read via runCatching→SIMPLE_TTS;
   `saveDeepSeekConfiguration` generalized to `saveInitialConfiguration`;
   `enableDeepSeekWebSearch` deleted along the whole chain; provider_* /
   speech_services_cn_* / enable_deepseek_* strings wiped; tests fixed
   (ThinkingQuality — CN cases deleted, third-party now UNSUPPORTED; other
   fixtures DEEPSEEK→OPENAI); provider logos `assets/model_logos/` −16 CN
   folders; PROMPT-002 and its 5 allowlist entries deleted (file dead);
   3 shortened CN prompt lines pre-allowlisted. UNTOUCHED: wire-protocol
   comments, free-model chat trigger, minimax routing branch of OpenCode
   (aggregator machinery), non-whitelist `examples/` (not packed to APK).
   16. Install-size round 2 — CLOSED by user decision 10.10 (don't touch);
   inventory below kept as reference, not a plan. Base: APK 179.7 MB,
   installed_linux: assets ~155 MB stored + copies in `files/` (rootfs
   61 MB tarball + ~358 MB unpack, toolpkg_cache ~31 MB) — research-size
   numbers, remeasure on a provisioned device before work (`run-as ...
   du` over `files/*`: objectbox, translation_cache, media_pool,
   skill_repo_zip_pool, image_pool, custom_emoji, logs — weigh what they
   really are, write here). Delete candidates (all — user decision,
   numbers from 10.10 APK): avatar stack ~14.5 MB (`libfilament-jni`+
   `libgltfio-jni` 5.2, `libdragonbones_native` 2.6, `libMmdWrapper` 5.1,
   `libFbxWrapper` 1.6) — only with the avatar feature; FFmpeg ~26 MB
   (`libavcodec` 15.7, `libavfilter` 5.2, `libavformat` 5.1) — only with
   Toolbox "FFmpeg Toolbox" + `ffmpeg.js` toolpkg; llama stack ~23 MB
   (`libllama-common` 12.6, `libllama` 7.9, `libggml-*` 3.1) — only with
   GGUF inference; MNN ~26 MB — see 15(e); MLKit 9.5 MB — only with OCR
   (live feature: `OCRUtils`, screen-OCR in the floater). SD/out-of-APK
   candidates (technical, no feature kill): helper APK
   (`assets/shizuku.apk` 2.5 + `accessibility.apk` 2.7 MB, readers
   `ShizukuInstaller:37`, `UIHierarchyManager:89-91`) and templates
   (11 MB, `WorkspaceUtils.copyTemplateFiles:682`) + emoji (3.7 MB,
   `CustomEmojiRepository:298`) — same Q2 template as task 13;
   toolpkg_cache (move after the SubpackStorage mechanics); aapt2 doubles
   in templates (2×4.5 MB) — DO NOT TOUCH (breaks export, SIZE-006). NEVER
   TOUCH AT ALL: rootfs (E3: proot from external leaf won't start —
   experiment with pre-proof and rollback, separate), `lib/`+dex except
   listed (E2), `assets/js` 1.4 MB (hot JS runtime), keystore assets,
   `bridge/`, `operit_shell_exec`. Round DoD: "module → MB in APK → MB
   installed → decision" table here + per taken item — like 12a
   (delete+measure) or 13 (on-demand+`accept_q2_export.sh`-style
   acceptance). ACCEPTANCE 15 CLOSED night 10.10: `accept10` on
   `20261010_215026Z` (15-code + evict-fix) — PASS=10 FAIL=0:
   install/grants/onboarding/picker/folder/48 MB download/repack/sign
   (`WebApp_1791671054137.apk` 48094313 B)/leaf-intact/offline-no-redownload.
   A/B "094027Z signs" no longer needed (mechanism proven directly).
   17. Export-signing regression (CLOSED night 10.10; BLOCKER lifted).
   Symptom was: `Export Failed` — `Unable to load keystore`, `JKS not
   found`; output `WebApp_*.apk` stays UNSIGNED (`keytool -printcert
   -jarfile` → `Not a signed jar file`). PROVEN (measurements, not
   guesses): (a) keystore byte-intact (md5 device == md5
   `app/src/main/assets/pkcs12.keystore`); (b) BC classes in dex identical
   (5433=5433, refs 7913=7913 — only via `python3 zipfile`, no `unzip` on
   the box); (c) [CANCELLED by close-out 17: keytool probably checked a
   stale file, trap (N13) — do not quote] APK 094027Z allegedly signed,
   132740Z/160304Z — not; (d) passwords/alias are constants
   (`ExportDialogs.kt:862-870`: file from `getOrCreateKeystore`,
   `android`/`androidkey`/`android` — code unchanged); (e) bundled-BC
   insert rejected by the system (`insertProviderAt` → −1, duplicate "BC"
   name forbidden — so the a2764e9c insert was always a no-op, platform BC
   did the work); (f) iteration showed: `KeyStore.PKCS12` offers only one
   "BC" (platform), nobody offers JKS (normal on Android, not an error).
   NOT PROVEN (the mystery core): same platform BC, file, password — old
   builds open the key, new ones don't. Refuted: file corruption, cut
   classes, JKS, password/alias, templates, download. Open: process state
   (which providers really stand — old builds had no logs); platform flake
   across reboots (phone vanished from LAN in the evening and came back).
   Loose ends: (1) RESOLVED night 10.10: the `inserted at −1` line at 20:40
   was accept8 on `170331Z` (code with insert), no anomaly: pre-170331Z
   builds lack the line (verified by grepping dex of all APKs for the exact
   phrase); the confusion was my error — I twice wrote accept8 rides
   160304Z; (2) JKS path always dead (no provider on Android) — fix PKCS12
   only. ROOT FOUND night 10.10 (diagnostics `20261010_203519Z`, CI
   `38084332791`, accept9 run: onboarding/picker/folder/download/repack —
   OK, signing — no; log `files/logs/operit.log` via run-as): keystore is
   PBES2/PBKDF2/AES-256-CBC (verified locally with `openssl pkcs12 -info`,
   file dated 09.09), and the phone has NO PBES2 SecretKeyFactory in all
   of JCA (direct probe — unavailable; `KeyStore.PKCS12` offers only
   platform BC 1.61; old-style resolves to the same). Bundled-BC insert
   rejected (−1, name "BC" taken by platform). History: pre-`a2764e9c`
   code did remove+insert (upstream mechanics) and signing worked;
   `a2764e9c` removed remove ("don't return" — old memo item was WRONG) +
   added keep — since then platform only → PBES2 dead. A/B "094027Z signs"
   — SUSPICIOUS (keytool may have checked a stale `WebApp_*.apk`, trap
   (N13)): re-verify with a clean run. FIX (commit after diagnostics, CI
   `38089152700`): `registerBouncyCastleProvider` does remove+insert again
   (evict platform from the "BC" slot + insert bundled at 1 + log the slot
   class); keep `org.bouncycastle.**` in place (`proguard-rules.pro:77`);
   `loadVerifiedKeystore` iteration stays verification. DoD on green:
   clean run + keytool on EXACTLY the fresh file (name+time) + install.
   CLOSE-OUT 17 (APK `20261010_215026Z`, CI `38089152700`, accept10
   PASS=10 FAIL=0): log shows `BC/1.78` first (platform evicted),
   `PBES2 SecretKeyFactory offered by BC`, `PKCS12 loaded via BC` ×2;
   fresh `WebApp_1791671054137.apk` (01:24, 48094313 B): keytool —
   self-signed Signer #1 CN=1, `adb install` — Success. 15-commit innocent
   (nothing to bisect); A/B 094027Z — stale-file artifact, never quote.
   18. Fork identity (user decision 10.10: B+C now, D later — D DROPPED
   15.10, forget it). B — attribution string `fork_attribution`
   ("English-only community fork of Operit (upstream:
   github.com/AAswordman/Operit). ALPHA build, use at your own risk."):
   in the onboarding card (after plugins-note, small type) + in About
   under the version (no fork mention there at all before). C — debug
   label "Operit Debug" → "Operit EN" (`app/build.gradle.kts` resValue;
   clone flavor untouched).
   CLOSE-OUT 18 (APK `20261011_123918Z`, `accept_bc_identity.sh` + `scen/
   bc_identity.uaw`): PASS=8 FAIL=0 — attribution on onboarding and About,
   "Operit EN" label in launcher, old label gone. En route fixed Continue
   tap (swipe→tap→chat-check cycle).
   bc_identity.uaw`): PASS=8 FAIL=0 — attribution on onboarding and About,
   "Operit EN" label in launcher, old label gone. En route fixed Continue
   tap (swipe→tap→chat-check cycle).

---

## What was done, so it never redos

The previous agent's diagnosis was **wrong**, and that's the main thing
to know. It blamed leftover Chinese on hardcoded literals. In fact
`values-en` was full (7659/7659), `:app` hardcode cleaned — yet Chinese
kept appearing. Four independent causes, each looking like "missing
translation":

1. **Unsupported system locale** leaked into `Configuration` → Android
   fell back to the default bucket, which was Chinese. Now the default
   bucket is English, and everything unsupported resolves to `en`.
2. **Services read strings from the application context**, which below
   Android 13 doesn't reconfigure on language change. They read via
   `getLocalizedContext`.
3. **Values written to DataStore on first launch** stay in that launch's
   language. Hence `默认配置`, `小欧`. Heals itself on clean install.
4. **ToolPkg plugin** didn't get `useEnglish` in the payload → fell to `zh`.

Fifth, found on-device: **notification channel names can't be fixed by
resource.** The system captures them at first creation forever. Fixed
only by a versioned suffix in the channel id.

Confirmed on SM-A207F at system locale `ru-RU`: first screen English,
CJK = 0 across UI and all phone notifications.

**Size:** 439 → 296 MB. Mainly — STT model (141 MB) moved online, locales
(7 MB), non-Latin OCR models and Chinese tokenizer (4.8 MB).

**Verified vs not:**

- Debug build compiles, R8 (`minifyNightlyWithR8`) passes in CI.
- R8-minified build **never run on device** — no signed APK.
- `check_strings.py` and `check_localizations.py` fixed, but mean
  anything real only once a second locale appears.

---

## Open questions 1–2 (SUPERSEDED/CLOSED, history)

## Open question 1: further distribution trimming (SUPERSEDED 2026-10-03)

Table below — state on 30.09 (APK 296 MB). Live:
`docs/FORK-REGISTRY/research-size.md` + plan `docs/TODO/offline-assets-move/`.
OCR verdict below partly outdated: non-Latin ML Kit removed (SIZE-002),
Latin OCR alive (`OCRUtils`), engine+model stay.

296 MB, biggest blocks in APK:

| block | MB | what | my view |
|---|---|---|---|
| dex | 72 | all logic + 3 HTTP stacks | R8 on, real win unmeasured |
| rootfs Ubuntu | 64 | proot image for terminal | online candidate, breaks "works out of the box" |
| lib | 57 | native libs | already arm64-only, nothing to squeeze |
| `subpack/` | 37 | `android.apk` + `windows.zip` for the export feature | online candidate |
| `packages/` | 28 | of which 27 is `apktool.toolpkg` | online candidate |
| **OCR (ML Kit)** | **5.5** | see below | full-removal candidate |
| other assets | 20 | emoji gifs, MathJax, fonts | |

**On OCR — a non-obvious conclusion, verify before deciding.** It is
**1.49 MB of weights** (the model really is that small — quantized
MobileNet + LSTM) plus a **10 MB engine**, which is universal: I found
code for 23 scripts in it, including Devanagari, whose model I deleted.
The engine is useless without the model, the model without the engine —
**no partial win**. Either all of ML Kit (5.5 MB in APK, 11.3 MB
on-device) or nothing. Recognition is fully local, no cloud, doesn't run
in background. Four consumers, all on explicit action: attachment
screenshot, floating OCR window, agent "read the picture" tool, PDF scan.

**Careful with `useLegacyPackaging = true`** in `app/build.gradle.kts:528`.
It keeps `.so` compressed in the APK, unpacked at install. APK gets
bigger, on-device footprint smaller. An "shrink download" vs "shrink
install" fork — choose deliberately.

Work order if taken up: first run `python3 ci/script/fork_audit.py`
(you'll see hotspot files — where fork and upstream conflict), then
decide block by block, remeasuring APK each time.

---

## Open question 2: GigaAM-Multilingual STT replacement (CLOSED 2026-10-03)

Implemented: `GigaAMSpeechProvider`, direct ORT runtime instead of sherpa
(variant (b) below), VAD chunker instead of streaming (phrases, not a
stream), 225 MB model on-demand with consent. Remainder — M6 acceptance
(see status above), not implementation.

User picked `fussraider/GigaAM-Multilingual-sherpa-onnx-ctc`. I verified —
the pick is right, but **it's not a link swap, it's an engine swap**, and
three things break "just swap it":
**1. The app has no runtime.** It ships `libsherpa-ncnn-jni.so` (5.1 MB)
and Kotlin API `com.k2fsa.sherpa.ncnn.*`. The model is **sherpa-onnx**
format — a different JNI (`libsherpa-onnx-jni.so`) and API
(`com.k2fsa.sherpa.onnx.*`). Both `.so` come from the `jniLibs.zip`
archive downloaded by `ci/script/download_android_dependencies.sh` from
Google Drive — i.e. **an upstream artifact I cannot add a file to**.
Options: (a) ask upstream to add sherpa-onnx JNI — slow, out of our
control; (b) run ONNX directly through the already-present
`com.microsoft.onnxruntime:onnxruntime-android:1.17.1`. There is a sample:
`OnnxSileroVad.kt` already works via onnxruntime. CTC with a 71-symbol
vocab → greedy decode is trivial: argmax → collapse repeats → drop blank.
Features: 16 kHz, 64 mels, n_fft=320, win=320, hop=160, subsampling=4.
This is the **preferred path**, it drops the upstream dependency;
(c) build `libsherpa-onnx-jni.so` locally and vendor it into an
`llm/mnn`-like module, as done with `libsherpa-mnn-jni.so`.
**2. The model is offline, the app streams.** Current
`SherpaSpeechProvider` is an `OnlineModelConfig` with encoder/decoder/
joiner, pushing partials via `recognitionResultFlow`
(`partialResults = true`). The export README says outright: *NeMo-CTC
**offline** format*. So either write a separate
`OfflineRecognizer.fromTransducer`/`fromCTC` wrapper and switch the UI to
"record → recognize", or live with end-of-utterance delay: live chat
transcription arrives in chunks, the user is used to seeing text as they
speak. This is a product decision — **ask the user** before implementing.
**3. The model is bigger than what it replaces.**

| variant | size | vs current 141 MB |
|---|---|---|
| `model.int8.onnx` (220M, 16 layer, d_model 768) | **225 MB** | +84 MB |
| `large/model.int8.onnx` (600M, 24 layer) | **592 MB** | +451 MB |
| `model.onnx` fp32 | 885 MB | not mobile |

So **Russian quality grows, and so does distribution size.** Directly
conflicts with open question 1. Tell the user explicitly. Good news: the
model is already online (I moved STT online), so it's a download, not APK
weight. And it **works first-class in Russian**: WER 7.1% on Common Voice
vs 9.1% Whisper large v3, 4.4% on FLEURS vs 3.1% (Whisper better there).
Symbol tokenizer, not sentence-piece — Russian is a first-class vocab
citizen with Latin: `a-z`, `а-я` + `ё` + Kazakh `і ғ қ ң ү ұ һ ә ө`.
71 tokens total, `<blk>` last. Pin the revision as done in
`SttModelRepository`:
`9f5a77e8975211abe8511693accd3a63ee1e9f43`, files `model.int8.onnx` +
`tokens.txt`. **Note:** `libsherpa-ncnn-jni.so` (5.1 MB) becomes unneeded
after the switch — a separate win, but only if `SherpaMnnSpeechProvider`
doesn't share it (see `llm/mnn/src/main/jniLibs/arm64-v8a/
libsherpa-mnn-jni.so` — a different lib, it stays).

---

## Acceptance histories T1–R8, CTC, deferred/dropped

## Custom CTC checkpoint acceptance (2026-10-03, device R9TN601D6GJ)

- Feature commits: `a5d60d76` (CtcModelConfig core) + `b1312e43` (HF add
  flow) + fixes `9676e3d5` (prefs brace), `dfc4c873` (Hub ?blobs=true +
  sha256 field). CI `37081015984` green, APK ~293 MB reinstalled
  (uninstall first: every CI build signs with a fresh debug key).
- Accepted on device: picker renders, add-dialog resolves
  `i2z1/gigaam-multilingual-ctc-onnx-int8` (failed before the blobs fix
  with "no content digest" — that's how the bug was found), full pipeline
  (tokens pin, 225 MB verified download, graph-vocab handshake on the
  phone's ORT), auto-select "In use", delete with fallback to built-in,
  registry back to `[]` with no leftover files.
- Wizard replay cheat-sheet: agreement tap, tour next x3, welcome next,
  grant perms via adb (READ_MEDIA_AUDIO + SYSTEM_ALERT_WINDOW appop +
  battery whitelist matter), Check status, Continue through Incomplete,
  Standard level, Confirm, wait out the announcement countdown. Keyguard:
  swipe alone doesn't unlock, use KEYCODE_WAKEUP + `wm dismiss-keyguard`;
  dreaming re-locks, and `settings put secure screensaver_enabled 0`
  stops it.

## Deferred item decoder (2026-10-03, from /home/uzzzver/operit-fork/screenshots2/)

Screenshots 1-8 reviewed frame by frame. Internal labels T1/U1/D12 exist
in no artifact (registry, HANDOFF, user notes, git log); what follows is
reconstructed from screenshots, unknowns marked as unknowns.

- RAW TOOL JSON (fixed `0add89c4`): AI Chat renders a tool call as a row;
  tapping opens `ToolResultDetailDialog` in
  `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/part/ToolResultDisplay.kt`,
  showing the result string verbatim. Screenshot
  `Screenshot_20261001-015352_Operit Debug.jpg` shows
  `daily_life:device_status Execution Successful` with a single-line
  `{"success":true,"message":"...Chinese...",...}` dump. Fixed by
  `splitEnvelope()` in the new `ToolResultFormat.kt` (same package): the
  message line heads the dialog, the same JSON follows indented, non-JSON
  passes through untouched. Unit-tested (`ToolResultFormatTest`, 4 tests).
- T1 NETWORK CHINESE (CLOSED 2026-10-04, see T1/T2/T3 sections below):
  default-LLM translation + SHA-256 source cache + batch prefetch +
  Google fallback. The `工具执行时发生意外错误` prefix lives in
  `examples/*.ts|*.js` (tool runtime, not app code) — covered by
  failure-body translation, never edit examples.
- DRAWER OVERLAP (already fixed, not U1): screenshot `..._012728...`
  shows the `Not Running` badge landing on the `Permissions` label in the
  drawer. Fixed earlier: `DrawerContent.kt` (~line 81 comment) replaced
  the word badge with a dot. If the overlap still shows on a new build,
  reopen with a fresh screenshot; otherwise closed.
- MARKET NO-RETRY (CLOSED 2026-10-04): the top-right search icon was
  tapped on device and opens plugin search ("Search plugins").
  Pull-to-refresh remains the only list recovery, matching the empty-state
  copy — not a dead button. Same empty state seen 04.10 with a real
  `SocketTimeoutException` to `static.operit.app:443` (phone-network side,
  transient).
- U1 / D12 (unknown): no referent in any file, screenshot, or note. Do
  not treat the drawer overlap or market screen as U1/D12 without user
  confirmation. Ask the user with the screenshot filenames above.
- Announcement notice (`..._011954...`): first-run dialog text comes from
  the network (upstream notice), not app resources; was out of scope,
  TRANSLATED 2026-10-04 (T2: prefetch in `OperitApp.kt` + `TranslatedText`
  in `RemoteAnnouncementDialog.kt`, countdown split). Accepted on device
  twice, fully English.

## Dropped 2026-10-03 (user directive: strike, revisit in manual testing)

- Dead network IconButton: no referent confirmed in code or screenshots
  (Market has pull-to-refresh; search support is wired). Dropped.
- U1 / D12: no referent in any artifact. Dropped. If either resurfaces in
  manual testing, file with a screenshot filename and screen path.

## T1 runtime translation (2026-10-03, commit aa4234a5, CI 37089771706)

- Decision (user): translate via the default LLM model, cache
  translations, hash detects updates. Applies to all runtime Chinese the
  app can meet.
- Implemented: `data/translation/CachedTranslator.kt` (SHA-256 key, CJK
  detect, JSON file store capped at 2000 entries, in-memory store for
  tests) + first surface: tool-result dialog message line in
  `ToolResultDisplay.kt` via `EnhancedAIService.translateText` (the
  TRANSLATION functional model, i.e. the default model unless the user set
  a dedicated one). Envelope body stays byte-identical for copy-paste.
- Tests: `CachedTranslatorTest` 6/6 standalone (passthrough,
  translate-once, key stability/uniqueness, CJK detect, file roundtrip).
- Open surfaces (same pattern: `CachedTranslator.containsCjk` gate +
  LaunchedEffect + cached translate): plugin names/descriptions DONE
  (`d54af436`), Market listings NOT done. Tool error prefix lives in
  `examples/*.ts|*.js`, covered by failure-body translation, no action.
- Traps met: a `Write` of `\uXXXX` escapes produced literal CJK chars plus
  a stray U+263A widening the regex range; fixed via python with pure
  escapes, verified by non-ASCII grep. Second trap: constructor param
  named `translate` shadowed member `fun translate` = infinite recursion;
  renamed to `fetchFresh`.

## T1 acceptance (2026-10-03, CI 37096445114, apk 2026-10-03_04-24-40Z)

- Packages card `楼层限制器` renders as `Floor Limiter` + `Slice the
  latest N context layers, ...` on the phone; cache
  `files/translation_cache/v2.json` holds clean entries, no think blocks.
- Trap log: `Write` of `\uXXXX` escapes produced literal CJK + stray
  U+263A (fixed via python, verified by non-ASCII grep); ctor param named
  `translate` shadowed member `fun translate` = infinite recursion
  (renamed to `fetchFresh`); `run-as "a; b"` multi-command quoting is
  unreliable, verify state with single commands; `&&` chains stop on
  failed `cp` when disk is full (an install was silently skipped this way
  — always check `lastUpdateTime` after install); uiautomator regex
  parsing breaks on quotes/newlines in text, use a real XML parser.
- Disk: / at 96%, /tmp/opencode/apk* cleaned; keep an eye on it.

## T1-Market acceptance (2026-10-04, CI 37164035232, apk 2026-10-04_00-08-59Z)

- Commit `ca511255`: Market cards (title+description), detail header
  title, About body via new `TranslatedMarkdown` (same CJK gate + cache,
  markdown renderer kept; code blocks excluded at call site),
  `TranslatedText` gained `onTextLayout` passthrough (comment
  expand/collapse). APK 373 MB, size unchanged. `fork_audit` green
  (0 new CJK).
- Accepted on device (fresh install, wizard replay, announcement
  countdown waited): Packages `楼层限制器` -> `Floor Limiter` live, screen
  CJK=0; Market list loads over network, English entries byte-intact, no
  overlap; detail screen (header, badges, metrics, About markdown,
  Comments(22)) renders, no crash; `files/translation_cache/v2.json`
  written (3 KB); logcat has no FATAL.
- Known-accepted, by design: avatar initials stay CJK (single-glyph
  identifiers derived from title, like usernames); badges (type/category/
  version) untranslated; review reasons are resource-based.
- Coverage gap, assessed ~zero risk: Market listings seen were all
  English, so the Market-card CJK branch never fired live — but it's the
  identical `TranslatedText` proven on Packages cards in the same build.
- Device traps new: wireless debugging died mid-session (port closed, full
  37000-45200 scan empty); fix = re-pair from the phone (user reads
  pairing port + code), `echo CODE | adb pair IP:PAIRPORT`, then rescan
  for the adb port (this time `41599`). `uiautomator dump` started dying
  with SIGKILL (exit 137, 0-byte xml) after many dumps — sleep/wakeup +
  adb reconnect did NOT help; fallback that works: `screencap -p` +
  visual PNG read (the agent CAN see screenshots via Read).

## T2 batch acceptance (2026-10-04, CI 37169808187, apk 2026-10-04_02-02-07Z)

- Commit `03d51d5f`: `translateTexts` batch (JSON in/out, ~6000-char
  chunks), `prefetchRuntimeTranslations` triggers (Market page, detail +
  all comments, installed plugins, announcement), `TranslationPrefetch`
  marks so batch and per-item never duplicate, sync cache peek in
  TranslatedText/Markdown (no Chinese flash on hit), announcement dialog
  translated (countdown split so no request fires per second). Tests
  16/16 standalone, `fork_audit` exit 0.
- Accepted on device (fresh install): post-onboarding notice fully
  English ("Announcement ... Got it", batch visible in logcat thinking),
  Packages `楼层限制器` flipped to Floor Limiter with zero taps after the
  batch landed, `v2.json` written (1.2 KB), 0 FATAL in last 300 logcat
  lines.
- Gap: upstream `static.operit.app:443` timed out from the phone network
  (`SocketTimeoutException`, VM log), so Market listings were empty and
  the list/detail prefetch never fired live. Same helper as the proven
  Packages/announcement prefetches; live-fire when the CDN is reachable.
- Device traps new: screen sleep freezes screencap (status-bar clock
  stands still — always WAKEUP first and compare md5); wizard bottom-arrow
  touch target is y~1378, not the glyph (~1416); location check needs the
  master switch (`settings put secure location_mode 3`), grants alone show
  ✗; `logcat -d` unfiltered hangs the shell — always `logcat -t N`.
- Audit lesson: `fork_audit.py` exit code is the gate, not grep output —
  earlier "green" claims read grep's `$?`. Also fixed while here: two
  LABEL-CONCAT violations (About storage subtitle, custom-STT status
  line, both mine) and two ANCHOR-LOST registry anchors (SIZE-004 now
  points at `CustomSttModels.ensureAcoustic`, SIZE-008 anchor updated to
  the Location enum). CJK-NEW stays 0 — new Chinese prompt text goes
  through review.
- R8 experiment: `:app:assembleNightly` compiles + minifies fine; the run
  failed only at `signRotatedNightlyApk` (no RELEASE_STORE_FILE in CI =
  known keystore gap). Minified-debug measurement still open, runs after
  T2.

## T3 fallback acceptance (2026-10-04, CI 37201078764, apk 2026-10-04_12-08-20Z)

- Commit `5a76aa4d` (user explicitly approved the fallback, lifting the
  no-fallback rule for this): `GoogleTranslateFallback` (keyless gtx
  endpoint, JSON sentence parser) + `fetchWithFallback` wiring in single
  and batch paths (primary error rethrown when both fail, nothing masked).
  Tests 19/19 standalone, `fork_audit` exit 0.
- Accepted on device (fresh install): announcement again fully English
  with split countdown, Packages batch flip with zero taps ("Turn
  Limiter"), `v2.json` written (1.2 KB), 0 FATAL. Fallback branch did NOT
  fire (primary healthy) — verified by construction: gtx reachable from
  the phone and returns exactly the parsed shape
  (`[[["test","测试",...]],null,"zh-CN",...]` via phone curl), parser
  unit-tested. Live-fire pending a no-model scenario.

## Shizuku on the test phone (2026-10-04, user suggestion)

- Installed `moe.shizuku.privileged.api` v13.6.0 (APK from
  `rikkaapps/Shizuku` releases via `gh release download`). Dhizuku was
  skipped: it needs device-owner, which fails on a provisioned phone with
  accounts; Operit expects the Shizuku API anyway.
- Started WITHOUT in-app pairing: Shizuku app shows the exact starter
  under "View command" — `adb shell <apk-dir>/lib/arm64/libshizuku.so`,
  apk dir from `pm path moe.shizuku.privileged.api`. Result: "Shizuku is
  running, Version 13.5, adb".
- Authorized Operit: drawer Permissions tile (= ShizukuDemoScreen) ->
  DEBUGGER tab -> "Grant Shizuku Permission" -> system dialog "Allow
  Operit Debug to access Shizuku?" -> "Allow all the time". Status now
  "Shizuku Service Granted", Debugger level "Currently in Use".
- After a reboot Shizuku dies; restart = re-run the same libshizuku.so
  command over adb (no pairing needed). Re-check "Shizuku is running" in
  the Shizuku app afterwards.
- Remaining red on that screen: "Operit Terminal Not Granted" (separate
  terminal-env setup, not Shizuku).

## M6 speech acceptance (2026-10-05, T3 build 12-08-20Z, audiobook via user)

- Dictation: Toolbox -> Speech Recognition, language `ru`, ~31 min of
  audiobook over the room: **497 phrases + 93 blanks, 0 `Utterance decode
  failed`**, engine never fell, session stopped by button. Speed from
  `files/logs/operit.log` (`Phrase (X s audio in Y ms)`): 1046.3 s audio
  in 993.2 s wall = **RTF 1.05**, median 948 ms per 1 s audio, p90
  1006 ms. Holds live speech with a small margin.
- Quality: far field + `Microphone processing = Calls (echo
  cancellation)` — rough but coherent Russian narrative (screenshot
  m6_39). Without the book's reference text WER is uncountable; if quality
  matters — try default mic processing instead of Calls.
- Language field: `ru/en` dropdown works; defaults to `en`, resets to
  `en` on every screen entry (screen state, not a bug).
- Consent: Download button in settings downloads AT ONCE with no dialog;
  consent (`GigaAMConsentDialog`) lives in toolbox dictation — by design
  (consent where "speak" was pressed).
- SD round-trip: internal -> emulated -> **SD card** -> emulated ->
  internal. All copies byte-exact (`model.int8.onnx` 224762512,
  `tokens.txt` 391). Engine init FROM CARD proven: `GigaAM initialized`
  03:33 with empty internal and no `Model files absent`. Source files
  deleted, but EMPTY `stt_model` folders stay behind at the old place
  (minor, follow-up candidate).
- Tap trap (NEW): in scrolled settings lists and dropdown menus taps land
  ~75–100px ABOVE requested (proven with `pointer_location`: request
  (182,700) -> point (182,625)). Missed Download in "Add from link"
  twice, one move went to the wrong row. Workaround: aim +80..100 below
  the visual (+compensation verified: request (182,765) hit Download and
  STARTED the download) or pick menus with DPAD_UP/DOWN+CENTER —
  deterministic (returned to internal that way). Drawer/tabs/buttons —
  1:1, don't touch. `pointer_location 1` shows the point, remember to
  switch off.
- Session time (AUTHORIZED 2026-10-05, user confirmed): clock jump 18:1x
  -> 03:0x is an 8–9 h pause for model-limit recovery, phone clocks ran
  TRUE all along. Log silence 18:43 -> 03:06 — book ended, phone idle.
  Dictation itself per log stamps 18:12:56 -> 18:43:53 (~31 min, 497
  phrases). Battery 69 -> 39% for everything (screen + 225 MB + ORT +
  idle).
- Bonus find (not UI, logcat only, pre-existing): `E/AnrMonitor: Main
  thread not responding: 1230ms - ...ANR!` — hardcoded upstream Chinese
  in the log. Doesn't affect UI; separate follow-up candidate, not M6.

## R8 experiment verdict: REJECTED (2026-10-05, CI 37206856650)

- Experiment: `isMinifyEnabled + isShrinkResources` in `debug`
  (`f37ff8a1`), green build, APK 373487996 -> 333371794 bytes (−40.1 MB,
  −10.7%).
- Minified debug CRASHES ON START on the phone: `Application Error`,
  `java.lang.NoSuchMethodError: no non-static method
  "Lcom/ai/assistance/operit/core/tools/javascript/
  QuickJsNativeHostDispatcher;.onCall(Ljava/lang/String;Ljava/lang/String;)
  Ljava/lang/String;"` from `QuickJsNativeBridge.nativeCreate` <-
  `QuickJsNativeRuntime$Companion.create (QuickJsNativeRuntime.kt:55)` <-
  `OperitQuickJsEngine.runtime$lambda$0 (OperitQuickJsEngine.kt:29)`. R8
  cleaned/renamed the method the native code calls via JNI; keep rules
  for QuickJS in `app/proguard-rules.pro` INSUFFICIENT (the build.gradle
  comment about "existing keep rules" is wrong).
- Worse: `release {}` and `create("nightly")` in the same file use THE
  SAME minification — release and nightly builds are presumably broken
  the same way (minified never run on device, NOT ONCE). Separate task:
  write keeps for QuickJS JNI (class + onCall signature), then repeat the
  experiment; no minification anywhere until then.
- Revert merged (`ce29250f`, tree = accepted T3), phone got T3 back
  (re-downloaded from CI 37201078764 — user deleted the
  `apk/2026-10-04_12-08-20Z` dir from disk; installed 04:04:38, wizard
  passed, starts clean). Only the R8 dir left in `apk/` (user deleted the
  rest).

## Size work acceptance (2026-10-03)

- Model to SD: location preset to card before download, 225 MB downloaded
  to `/storage/5982-1724/.../stt_model` (214 MB on disk), engine
  initialized from SD (`GigaAM initialized` in logcat), dictation session
  started and stopped cleanly. Internal `/data/data` has no `stt_model`.
- tflite+mediapipe removal (CI 37127299796, apk 2026-10-03_13-46-05Z):
  APK 386 -> 373 MB, lib 43 -> 41 .so, neither .so in the new APK.
- About storage card on device: Total 472 MB, internal 472 used / 15191
  free, card 0 used / 1028 free (fresh install, model not re-downloaded).
- Trap: kotlinc resolves .java stubs but emits no .class for them; runtime
  needs javac compilation (with coroutines on cp for the prefs stub).


