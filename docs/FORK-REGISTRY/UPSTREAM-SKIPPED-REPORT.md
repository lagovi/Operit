# Refused upstream commits — what they did and why

The fork classifies every upstream commit in `upstream-port-ledger.json`. This report
explains the 62 commits marked `skipped`: the functionality they carry and the concrete
reason each one was refused. It is generated from the ledger, so the reasons match the
machine-readable record exactly.

Five releases were also closed as `superseded` (their content arrived through a later fork
commit) and one commit stays `deferred` — `b6dc07d3`, the upstream revert of the Antigravity
OAuth provider that the fork deliberately keeps. That one is an owner decision, not a refusal.

## ToolPkg floating windows (14)

Upstream introduced a ToolPkg floating-window subsystem (host, controller, route render sessions, JS bridge, API version 1.1.0 gate) across these commits. The fork has no `ToolPkgFloatingWindow*` classes, no `Registry.ui.floatingWindows`, and its `ToolPkgApiVersion` stops at 1.0.1. Porting any single commit would reference types that do not exist, so the group is refused as a whole until the feature is decided.

| Upstream | Subject | Why refused |
|---|---|---|
| `07c81250` | docs(todo): 规划 ToolPkg 插件悬浮窗 API 对齐 Operit2 | Planning documents under docs/TODO/toolpkg_floating_window for the unported feature. |
| `0a9bf73a` | fix(toolpkg): 首渲染失败时关闭已添加的悬浮窗宿主 | Closes the floating-window host on first-render failure inside ToolPkgFloatingWindowSession.kt, which the fork lacks. |
| `15872226` | feat(toolpkg): 插件悬浮窗宿主与控制器 | Floating-window host/controller/options/surface classes do not exist here; head of the unported Operit2-alignment feature. |
| `20da8d7d` | fix(examples): 修正 floating_window_demo 的类型导入相对路径 | Fixes the types import depth in examples/floating_window_demo, which does not exist in the fork. |
| `460f73f2` | docs(toolpkg): 补悬浮窗 API 完整文档 | Documentation for the floating-window registry/API; the underlying feature is not in the fork. |
| `4ac0f8af` | fix(toolpkg): 补 ServiceLifecycleOwner 跨包 import | Adds a ServiceLifecycleOwner import to ToolPkgFloatingWindowHost.kt, a file the fork never had. |
| `712aa3ae` | test+docs(toolpkg): 悬浮窗单测、开发者文档与冒烟示例 | Smoke example for the floating-window API; absent and depends on the unported runtime. |
| `97d80ad9` | docs(toolpkg): 补悬浮窗 API 完整文档与可运行示例包 | Floating-window API docs and the floating_window_demo package; neither exists in the fork. |
| `a157f86d` | fix(examples): floating_window_demo 加入示例包同步白名单 | Adds floating_window_demo to the example-package whitelist, but that demo is not present in the fork. |
| `a6dfd065` | test+docs(toolpkg): 悬浮窗单测与开发者文档 | Unit test plus developer docs for ToolPkgFloatingWindowController, which does not exist in the fork. |
| `b32531b9` | fix(toolpkg): honor floating window close result and isolation contract | The entire ToolPkg floating-window subsystem does not exist in this fork. |
| `bd7ef735` | feat(toolpkg): 悬浮窗 route 渲染会话与生产装配 | Floating-window route render session and production wiring, both absent from the fork. |
| `e480df7d` | feat(toolpkg): 同步悬浮窗接口类型与 1.1.0 版本门禁 | Bumps ToolPkg API to 1.1.0 and adds floating-window interface types; fork's ToolPkgApiVersion stops at 1.0.1. |
| `f9d604ce` | feat(toolpkg): 悬浮窗 JS 桥接与权限能力 | Adds the ToolPkg.ui.floatingWindows JS bridge; depends on the unported floating-window runtime. |

## Annual report (7)

An upstream-only feature: a hand-drawn annual usage report with a Room data collector, a TypeScript frontend, a drawer entry badge and CI to build its assets. None of the `annualreport` packages, strings or assets exist in the fork, and the feature is not wanted here.

| Upstream | Subject | Why refused |
|---|---|---|
| `1afb42c8` | feat(annual-report): lock the drawer entry until 2027-01-03 and add a hidden dev entry | Locks/hides the drawer entry for the upstream annual report until 2027; fork has no annual-report code. |
| `331a687a` | feat(annual-report): add the hand-drawn entry badge to the drawer header | Annual-report drawer badge; the feature and string are absent here. |
| `7d69c1ec` | docs(annual-report): document report data and build workflow | Annual-report data-contract/CI documentation; no annual-report directory exists in the fork. |
| `8e6267c4` | feat(annual-report): read local usage data and host the report in a WebView | Annual-report data collector/DAO/Room/WebView host; none of the annualreport packages exist in the fork. |
| `abfa5fe2` | docs(annual-report): document unlock timing and release handover | Annual-report handover documentation; the feature and its docs tree are absent from the fork. |
| `c808efbf` | feat(annual-report): add the hand-drawn annual report frontend | Entire annual-report TypeScript frontend; the fork does not ship or want this feature. |
| `c29a855a` | ci: build the annual report assets in GitHub workflows | CI wiring for building annual-report assets; the fork has no annual-report package. |

## APK reverse toolkit (2)

The `apk_reverse` package and its bundled apktool/jadx runtime jars are not shipped by the fork, so the version bump and the artifact rebuild have no target.

| Upstream | Subject | Why refused |
|---|---|---|
| `33f8ff7c` | chore(apktool): rebuild runtime artifacts | Rebuilds apktool/jadx runtime jars for the unshipped apk_reverse toolkit; no apk_reverse package here. |
| `83acf075` | feat(apk_reverse): upgrade APK reverse toolkit to 1.1.0 | Upgrades the APK-reverse toolkit to 1.1.0; the fork ships no apk_reverse package. |

## Nested conversation folders (4)

A database-level redesign of the chat history tree (nested folders, independent ordering, Room migration and its tests). The fork never took the feature, so the follow-up test alignment, layout compaction and the Room v21 revert are all meaningless here.

| Upstream | Subject | Why refused |
|---|---|---|
| `614b022a` | feat(chat): add nested conversation folders and independent ordering | too large, DB migration; not taken |
| `7fc3da9c` | fix(chat): compact the history tree sidebar layout | Cosmetic compaction of the history sidebar, whose layout diverged heavily in this fork and depends on the absent R.string.long_press_manage. |
| `8b9d8539` | test(chat): align entity conversion checks with folder identity | nested-folders only |
| `de01cd35` | revert(chat): restore Room v21 for Operit2 imports | part of the nested-folders revert; not taken |

## Marked media probe verification (1)

Adds a verification contract with closed-form markers around `MediaCapabilityProbe`, which the fork does not have; `ModelConnectionTestOutcome.UNVERIFIED` is likewise absent. Related commit 375a3615 is already recorded as superseded by the fork's own tester.

| Upstream | Subject | Why refused |
|---|---|---|
| `db60f985` | fix(settings): verify media probes with closed-form markers | MediaCapabilityProbe.kt does not exist in this fork and ModelConnectionTestOutcome/UNVERIFIED is absent from ModelConfigConnectionTester.kt, so the entire marked-media feature is not part of this codebase. |

## Market gate on ToolPkg API 1.x (1)

Gates package installation by a ToolPkg API version range that the fork does not advertise; without the 1.2.0 execution-context implementation the gate would reject packages for a capability the fork lacks.

| Upstream | Subject | Why refused |
|---|---|---|
| `b078c742` | fix: gate market installation by ToolPkg API compatibility (#1474) | depends on ToolPkg API 1.x absent in fork |

## Draw provider tooling (4)

Development tooling for image generation experiments: renaming `xai_draw` to `spacexai_draw` with a preference migration, animated GIF output for GPT Image 2.6, GPT Image support in the OpenAI draw path, and the multipart fix that followed it. The fork keeps the original `xai_draw` naming and does not take this experimental provider surface.

| Upstream | Subject | Why refused |
|---|---|---|
| `14c8afdc` | feat(draw): modernize SpaceXAI and prompt inputs | Core renames xai_draw to spacexai_draw with a preference migration; the fork deliberately keeps xai_draw. |
| `1957624d` | feat(draw): support GPT Image 2.6 animated GIF output | draw dev tooling, rejected by policy |
| `29d67812` | fix(draw): upload OpenAI URL references as multipart | Fix for the OpenAI edits multipart path introduced by ef1bf4b6, which is not in the fork. |
| `c6b65b1a` | fix(draw): support GPT Image models in OpenAI draw | draw dev tooling, rejected by policy |

## Localization of stripped locales (8)

Every `values-XX` locale is removed from this fork and the default bucket is English, so localizing non-English strings, renaming locale-only labels, or removing orphaned `values-ja`/`values-es` strings has no effect. The English README rewrite is already the fork's state, and `README.zh-CN.md` is deliberately absent.

| Upstream | Subject | Why refused |
|---|---|---|
| `720ea9d1` | feat(ui): localize Thinking Configuration editor hardcoded strings | The fork already extracted these hardcoded Chinese strings in 610d9f0b and values-en no longer exists here. |
| `98241209` | fix(i18n): add localized skill tab label | Adds package_tab_skills only to non-default locales, none of which ship here. |
| `a46e0c62` | fix(i18n): localize plugin market labels | Touches only values-es and values-ms market labels; both locales are stripped from the fork. |
| `a51f23b4` | fix(i18n): rename package management tab labels | Renames tab/category labels across stripped locales plus the Chinese default; a wording choice the fork keeps its own English labels for. |
| `b3069485` | fix(l10n): remove orphaned ja string breaking lintVitalRelease | Removes an orphan string from values-ja, but the fork stripped every values-XX locale, so the lintVital failure cannot occur. |
| `d1a28473` | fix(i18n): rename Chinese skills tab label | Renames the Chinese skills tab label; our default bucket is English and the tab already uses R.string.skills. |
| `f72b20f6` | fix(i18n): add Japanese cache hit rate labels | Adds Japanese cache-hit labels to values-ja, a locale the fork removed. |
| `ea819096` | docs(readme): switch default README to English (move Chinese to README.zh-CN.md) | Switches the default README to upstream English and adds README.zh-CN.md; our README is already English and README.zh-CN.md is intentionally absent. |

## Upstream planning docs, CI and release housekeeping (13)

Files under `docs/TODO/...` and the upstream release/CI plumbing. The fork keeps its own English documentation tree and its own workflows, so batch checklists, PR-coordination notes, the PR-base-branch trigger, the obsolete SDK-tools skip and the version bumps carry no value.

| Upstream | Subject | Why refused |
|---|---|---|
| `0b21ef92` | docs: coordinate PR and device validation for the third batch | Upstream PR-batch coordination notes under docs/TODO, a tree the fork does not carry. |
| `162e8c28` | docs: plan third PR fix integration | Adds only docs/TODO/pr_fixes_batch3_20261106/index.md, an upstream planning note; this fork keeps no docs/TODO batch plans. |
| `66be5a9c` | docs(chat): record accepted sidebar verification | Only edits docs/TODO/compact_chat_history_tree, a tree the fork does not carry. |
| `728cef62` | docs: record verified stability integration and terminal dependency | Moves the upstream terminal submodule pin plus docs/TODO notes; the fork's terminal submodule is a separate repo. |
| `7d83ae2e` | docs: add missing Trendshift badges | docs only, rejected |
| `df35a53e` | docs: 新增 DataStore 偏好文件损坏恢复指南 | Chinese end-user troubleshooting guide for a past DataStore crash; fork writes its own English docs. |
| `e80c6d8a` | docs: record third PR batch scope and device checks | Adds docs/TODO batch checklists plus a Chinese user guide; fork keeps neither. |
| `f1bbcd80` | feat(docs): 完善 ToolPkg 开发者文档并更新 SandboxPackage_DEV | Restructures upstream package-dev docs; the fork maintains its own docs/doc-src. |
| `af743726` | ci: skip obsolete Android SDK tools package | Our android-build.yml and android-tests.yml already set packages: platform-tools on setup-android. |
| `f73cca99` | ci: run PR Check for pull requests into dev (#1392) | Upstream CI trigger fix renaming the PR base branch; the fork maintains its own pr-check.yml. |
| `aeb569bf` | chore(release): bump version to 1.12.2+2 | release bump, fork keeps its own version |
| `dd882918` | chore(release): bump version to 1.12.2+1 | release bump |
| `31646978` | Merge pull request #1210 from luojiaping/docs/readme-badges | Adds Trendshift badges linking to upstream; our README has its own fork badges. |

## Already present or duplicate (5)

The change is already in the tree: the fork's screens never had the invalid Compose import, the AutoGLM layout is balanced, the message-insert log noise is not user-facing, and the Antigravity OAuth provider was already ported here as `92b4cc63`/`d2246757`.

| Upstream | Subject | Why refused |
|---|---|---|
| `07680a72` | fix(settings): drop invalid Compose weight import | Duplicate of 1cc38b6d; the invalid import is already absent in our screen. |
| `1cc38b6d` | fix(settings): remove invalid Compose weight import | Removes an invalid Compose weight import from ContextSummarySettingsScreen that our file already does not have. |
| `1ea5133f` | fix(toolpkg): reduce message injection log noise | Purely removes log noise from the message_insert example toolpkg; no user-facing behaviour change. |
| `6f231956` | fix(ui): close AutoGLM execution layout | Adds a missing closing brace that this fork's AutoGlmToolScreen never lost; the file compiles balanced. |
| `a7676fa3` | feat(provider): add Antigravity OAuth login and fingerprint spoofing (#1257) | Antigravity OAuth provider is already carried by fork commits 92b4cc63/d2246757; nothing new to take. |

## Terminal submodule pointer bump (1)

Bumps the terminal git submodule to an upstream commit. The fork's `.gitmodules` points the submodule at `lagovi/OperitTerminalCore` on its own branch, and the relevant core fixes were already applied there as `ead64e3` and `f581eb5`.

| Upstream | Subject | Why refused |
|---|---|---|
| `333c3eb3` | fix(terminal): update core for Android 8-10 PTY compatibility | Only bumps the terminal git submodule pointer, and this fork's .gitmodules points it at lagovi/OperitTerminalCore on its own branch. |

## Provider restructure tied to removed subsystems (2)

Two large commits whose targets do not exist here. `1140f8d5` builds on the DeepSeek provider that the fork deleted in `08d8c1e6`; its provider-agnostic halves are already present. `1124d173` is the ToolPkg execution-session and subtask-policy redesign; its terminal half landed in `42f8bbf60`, and the rest would require advertising ToolPkg API 1.2.0, which the fork does not implement.

| Upstream | Subject | Why refused |
|---|---|---|
| `1140f8d5` | DeepSeek Responses: commentary 改写为 reasoning item + 正文实时流式输出 | The DeepSeek provider was removed from the fork (08d8c1e6d, china-out). The provider-agnostic parts of this commit (Claude thinking gating, toolCallName matching, Gemini per-request thinking state) are already present in the fork; the JS dispatch simplification conflicts with the fork's per-call ToolPkg session tracking. |
| `1124d173` | fix: complete terminal and subagent feedback corrections | The terminal half landed in 42f8bbf60 (TerminalCommandOutcome, sessionAlive, timeout-as-failure). The rest is the ToolPkg execution-session and subtask-policy redesign; the fork does not implement that subsystem, so advertising ToolPkg API 1.2.0 would be false and the 4023-line JS/subagent rewrite has no target here. |

Total: 62 skipped commits.
