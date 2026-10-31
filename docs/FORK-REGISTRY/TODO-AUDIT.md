# UI/UX TZ audit — persistent work list

Status legend: `[ ]` open, `[~]` in progress, `[x]` done, `[!]` blocked.
Update this file after every completed item. Do not rely on chat context.

## A. About and updates

- [x] A1. Remove the `Beta Program` row and its help copy.
- [x] A2. Remove the beta/nightly patch-update path.
- [x] A3. Point update checks at the fork repository, not `AAswordman/Operit`.
- [x] A4. Make `about_website` display the fork project, keep upstream attribution.

## B. AI Chat element help

- [x] B1. Anchor `toggles` on the chat settings popup.
- [x] B2. Anchor `export` on the workspace export menu item.
- [x] B3. Anchor `computer`.
- [x] B4. Anchor `model` in the classic input bar.
- [x] B5. `queue` is covered by the send/stop entry; removed the stale duplicate.

## C. Terminal element help

- [x] C1. Add element anchors for output, input, extra keys, settings, setup without
      making `:terminal` depend on the app module.

## D. Model config, speech services, permissions element help

- [x] D1. Model config anchors + catalog entries.
- [x] D2. Speech services anchors + catalog entries.
- [x] D3. Permission centre anchors + catalog entries.
- [x] D4. Workspace setup anchors/guide.

## E. Memory Base

- [x] E1. Explain non-obvious search weights in the search-settings dialog.
- [x] E2. Fix the `Cancel`/`Reset defaults` button layout at 360 dp.

## F. Phase 3 — settings and model configuration

- [x] F1. Advanced connection test: request URL, HTTP status, latency, response
      head, parsed error.
- [x] F2. Modality detection button with confirmation before writing capabilities.
- [x] F3. Constrained Settings subtitles to two lines and kept chat rows weight-based; still needs on-device 360 dp confirmation.
- [x] F4. Module centre with STT, OCR and MNN entry points.
- [x] F5. Per-component cookie clearing.
- [x] F6. Verified: backup/restore is its own screen and has no Voice Wake-up tab.

## G. Phase 4 — packages, prompts, avatars, workflow

- [x] G1. Clear installed/available/updatable status text in package lists.
- [x] G2. System Prompt Monitor in `ModelPromptsSettingsScreen.kt`: assembled
      prompt view plus safe toggles for managed parts.
- [~] G3. Avatar presets and a user-friendly avatar workflow. This is the next large task, but only after the current backlog and full regression testing are complete. Start with research and a written options proposal for user approval:
  - define which avatars are needed (character portraits, group avatars, assistant avatar, style variants);
  - clarify how they are supposed to be created or chosen today, and why the current flow is unclear;
  - compare implementation options: bundled presets, fork-hosted downloadable presets, optional third-party image services, local import/crop improvements, user-provided images only, and self-creation of a new avatar (drawing, generative prompt, or local composition) as a mandatory option;
  - document privacy, licensing, network and storage consequences of each option;
  - do not implement any option until the user approves one.
  - [x] Research written in `docs/FORK-REGISTRY/AVATAR-RESEARCH.md`; the user
    chose fork-hosted presets, all avatar kinds, two preset sets, and all three
    creation modes.
  - [x] Animated Assistant avatars researched in
    `docs/FORK-REGISTRY/AVATAR-3D-RESEARCH.md`; six glTF presets published to
    `lagovi/OperitAvatarAssets` with a `presets.json` catalog and per-model
    `LICENSE.txt`.
  - [x] Gallery implemented: Presets / Device / Create / Backend tabs; catalog
    fetched from `lagovi/OperitAvatarAssets`, sha256 verified, model installed
    into `AvatarRepository`; entry points wired for character card, group card,
    per-card user portrait, global user portrait and the assistant avatar list.
  - [x] Create implemented: draw, local composition, and generation through an
    explicit OpenAI-compatible images endpoint.
  - [ ] On-device acceptance of the whole gallery flow in one signed-off build.
  - [ ] Implement the Assistant import doctor and the Blender guide/skill.
- [x] G4. Workflow guided explanation for triggers, variables and tool steps.

## H. Acceptance follow-ups

- [x] H1. Rename the Tool Tester download fixture label or its destination.
- [x] H2. Verified the workspace File Manager rows are already compact (8 dp vertical padding, 20 dp icon, single-line ellipsis).
- [x] H3. Check the help banner at 360 dp; compact it if it wraps badly.
- [x] H4. Build on CI, install, smoke-test, archive APK with `CHANGES.md`.
  - [x] First CI `41166171224` green; APK installed after a clean uninstall.
  - [x] Smoke-tested Module center, model diagnostics (HTTP 200, latency 3467 ms),
    memory weight descriptions/button layout and Prompt Monitor toggles.
  - [x] Found that the help tap interceptor covered only screen content, not the
    top bar, so `computer`/`workspace` anchors did not open their cards.
    Fixed in `AppContent.kt`; CI `41215247651` is building the fix.
  - [x] CI `41215247651` green; installed. Top-bar anchors now open their cards.
  - [x] Second bug found during that test: the Scaffold-wide interceptor also
    swallowed taps on the help banner, so Exit/Screen guide/All topics were dead.
    Fixed in `HelpMode.kt` by excluding the banner rectangle; CI
    `41218273980` is building the fix.
  - [x] Installed `41218273980`; Exit, Screen guide and All topics all work.
  - [x] Terminal setup help opens its card after the rootfs download.
  - [x] Archived as `/home/uzzzver/operit-fork/apk/20261031_160912Z/` with
    `CHANGES.md`, APK sha256 `d3a14d17135db5b749b46a9a57920aedc75ad2528ab20596b38c52f027775f16`.

## Verification

- [x] V1. `python3 ci/script/fork_audit.py --no-color --no-hotspots` (pass, 44 anchors, 0 lost).
- [x] V2. CI green (`41218273980`, commit `5da03469`).
- [x] V3. On-device smoke test at 360 dp / font scale 1.0 (terminal Home toolbar anchors not reached; setup anchor verified).
