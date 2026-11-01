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
  - [x] On-device acceptance in CI `41251376299` / commit `dfdc111e`: presets
    load from the fork, Xbot installs and renders, staging copy is removed,
    draw and compose save into the avatar slot, the global portrait and the
    assistant preset list open. APK archived in `apk/20261031_231010Z/`.
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

## I. Generated avatars through the Colab Qwen endpoint

Context: the OpenAI-compatible adapter in `tools/colab/qwen_openai_server.py`
was verified on a T4 on 1 November 2026, returning a valid 512x512 RGBA PNG in
67 seconds for a request identical to the one the app sends. The app has not
yet produced an avatar through it live.

- [ ] I1. Point Settings -> Avatar generation -> Backend at the tunnel and
      confirm the dialog shows the returned portrait in the Create tab.
- [ ] I2. Try the endpoint on every avatar kind the app offers: character card,
      group card, per-card user portrait, global user portrait, assistant
      avatar. Record which prompts produce usable square portraits and which
      come back unusable.
- [ ] I3. Build a prompt set for all avatar kinds and both style sets chosen
      earlier (see `AVATAR-RESEARCH.md`), generate the full set through the
      model, and review the results on the device.
- [ ] I4. Decide what to do with the generated set: keep it local, publish it
      to `lagovi/OperitAvatarAssets` as an optional pack, or discard. Do not
      publish anything before the licence question in I5 is settled.
- [ ] I5. Check that outputs of Qwen-Image-2.1 under the `qwen-research`
      licence may be redistributed, and record the answer in
      `COLAB-AND-QWEN-IMAGE.md`. This blocks I4.
- [ ] I6. Measure the cost of a full set: seconds per image, total session
      time, and whether the T4 session survives the whole run.

## J. Video models on a free-tier T4, for testing

Not started. All of these are claimed to work on a free T4, none verified
here yet. Purpose is to find out which ones can produce an animated assistant
avatar from an existing portrait, as a candidate path next to the glTF presets
in `AVATAR-3D-RESEARCH.md`.

| Model | Params | Task | Reported VRAM |
| --- | --- | --- | --- |
| Wan 2.1 T2V | 1.3B | text-to-video 480p | 8-11 GB |
| CogVideoX-2b | 2B | t2v, i2v | 4-10 GB fp16 + offload |
| CogVideoX-5b | 5B | t2v, i2v | 7-14 GB INT8 / sequential offload |
| LTX-Video | 2B, distilled 13B | t2v, i2v | fits 15-16 GB, model card claims |
| Wan 2.2 TI2V via Wan2GP | 5B | t2v, i2v 480p | 10-14 GB |

Sources supplied by the user and confirmed to run on free T4:

- `Wan-AI/Wan2.1-T2V-1.3B`, run via
  `Isi-dev/Google-Colab_Notebooks/blob/main/Wan2_1_1_3B_T2V_Free.ipynb`
  and `colab.research.google.com/drive/1x5ch4kcTj1mAeSEcMniwrNwX3jITngHE`.
- `THUDM/CogVideoX-2b`, official free-T4 notebook
  `colab.research.google.com/drive/1pCe5s0bC_xuXbBlpvIH1z0kfdTLQPzCS`.
- `THUDM/CogVideoX-5b`, same notebook for t2v, plus INT8
  `drive/1DUffhcjrU-uz7_cpuJO3E_D4BaJT7OPa` and i2v
  `drive/17CqYCqSwz39nZAX2YyonDxosVKUZGzcX`.
- `Lightricks/LTX-Video`, notebooks `LTX_Video_*` and
  `LTXV_0_9_7_13B_Distilled_*` in `Isi-dev/Google-Colab_Notebooks`.
- Wan 2.2 TI2V 5B through
  `Square-Zero-Labs/Wan2GP-on-Colab`, `wan2gp-google-colab.ipynb`.

Notebook collections: `Isi-dev/Google-Colab_Notebooks`,
`Square-Zero-Labs/Wan2GP-on-Colab`.

- [ ] J1. Pilot one model end to end on a T4 and record real VRAM, wall time
      per clip, output length and resolution. CogVideoX-2b has the official
      notebook and the lowest claim, so it is the cheapest first test.
- [ ] J2. Test image-to-video from an existing avatar portrait, because that is
      the only use that matters here; text-to-video alone does not produce an
      avatar of the user's assistant.
- [ ] J3. Compare against the glTF route already shipped: file size, load time
      on the phone, whether the phone can even play the produced format.
- [ ] J4. Write the findings into `AVATAR-3D-RESEARCH.md` and decide whether
      any of it belongs in the app.

Operating rule for both sections: stop the session with `colab stop -s <name>`
the moment the run is finished. See `AGENTS.md`.

## Verification

- [x] V1. `python3 ci/script/fork_audit.py --no-color --no-hotspots` (pass, 44 anchors, 0 lost).
- [x] V2. CI green (`41218273980`, commit `5da03469`).
- [x] V3. On-device smoke test at 360 dp / font scale 1.0 (terminal Home toolbar anchors not reached; setup anchor verified).
