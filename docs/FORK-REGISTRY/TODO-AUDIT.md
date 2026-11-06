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

## H2. Avatar section in the theme editor — not a code defect

Closed as a false alarm. On 1 November 2026 the avatar block was reported
unreachable in `Settings` -> `Theme and Appearance` -> Chat tab, and several
debug builds were spent looking for a layout defect.

The section was never broken. A magenta marker painted at the top of
`ThemeSettingsAvatarSection` is clearly visible at `scroll value=772`, and
`uiautomator` lists `Avatar Customization`, `User Avatar`, `Global User
Avatar`, both `Reset` buttons and `Global User Name`. Tapping the avatar
circle (content-desc `Default Avatar`) opens the gallery dialog with
`Presets / Device / Create / Backend`.

What made it look broken: verification scrolled in jumps of 700-1000 px, which
skipped the block, and `uiautomator` returned truncated trees at those scroll
positions — the node count dropped to 17-20 and the section was simply absent
from the dump. Slow swipes of about 150-250 px land on it every time.

Practical note for future acceptance: scroll the theme editor gently and check
for `Avatar Customization` text before concluding anything is missing.

- [x] H2.1. Confirmed the section renders and the gallery opens.
- [x] H2.2. Diagnostics removed in `c915fc71`.
- [x] H2.3. Verified on device at scroll 772.

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

## K. Avatar configuration screen and preinstalled models — 2026-11-06

Work on the Assistant Config -> Avatar Configuration screen, driven by the
user's own look at the preinstalled models.

- [x] K1. `Mood Trigger Mapping` moved below `Current Model`. The mapping is
      meaningless before a model is chosen, because the animation list comes
      from the model. Commit `c9662af2`.
- [x] K2. The glTF camera distance is computed instead of fixed. The old
      constant `BASE_CAMERA_DISTANCE = 0.5` with a default scale of 0.5 put the
      camera 0.25 units from a model normalised to a unit cube, i.e. inside it.
      `updateFittedCameraDistance()` now derives the distance from the model
      span, the viewport aspect and a 45 degree vertical field of view, with a
      12% margin. The slider still multiplies that distance, so 1.0 means
      "fits exactly".
- [x] K2.1. `asset.boundingBox` turned out to be useless for skinned meshes: it
      reports unskinned vertex bounds, which for Xbot is 0.9 cm across. Framing
      on it produced distance 0.03 and an empty preview. Bounds now come from
      the world positions of the skeleton bones.
- [x] K2.2. The orbit pivot was hardcoded to y=0 while the camera targets the
      model centre, so changing pitch/yaw threw the model out of frame. The
      pivot now follows the target.
- [x] K3. The preset's `defaultAnimation` was dropped twice: `installGltfPreset`
      never accepted it and `GltfPersistenceDelegate.scanDirectory` never read
      it, so after a restart the controller got null and the preview showed the
      file's rest pose. Xbot rests in a T-pose, which is what made it look
      broken. The clip is now stored as `preset_default_animation.txt`.

### Live tuning the glTF preview — 2026-11-06

Chasing framing defects through CI is too slow: one hypothesis costs a build,
an install and a phone reset. `GltfTuner` (`core/avatar/impl/gltf/view/GltfTuner.kt`)
reads a JSON file from the app files directory and overrides the framing inputs.
The renderer re-reads it on every model load and on a tap on the preview.

    cat > /tmp/t.json <<'EOF'
    { "boundsSource": "bones", "distanceMultiplier": 1.0, "verticalFovDegrees": 45 }
    EOF
    adb -s 192.168.1.69:5555 shell "run-as com.ai.assistance.operit.debug sh -c 'cat > files/gltf_tuner.json'" < /tmp/t.json
    adb -s 192.168.1.69:5555 shell input tap <preview-x> <preview-y>

Supported keys, all optional; anything absent keeps the normal value:

| key | meaning |
| --- | --- |
| `boundsSource` | `auto` (bones for skinned, mesh otherwise), `mesh`, `bones` |
| `distanceMultiplier` | scales the computed fit distance, 1.0 = as computed |
| `verticalFovDegrees` | vertical field of view used for fitting |
| `fitMargin` | extra margin as a multiplier, default 1.12 |
| `targetX`, `targetY`, `targetZ` | override the camera target in model units |
| `skipUnitCube` | skip `transformToUnitCube` |
| `disableModelOrbit` | do not rotate the model on top of the camera orbit |

What the tuner already established on Xbot, so nobody repeats it:

- `skipUnitCube: true` makes a skinned model **disappear**. The unit-cube
  transform is what puts its geometry where the camera can see it, even though
  it scales the mesh and not the joints. Keeping it on is correct.
- With the transform on, framing from the mesh bounds and framing from the bone
  bounds both produce a visible model after normalisation, because the two spans
  agree. The automatic choice therefore resolves to the mesh.
- On Xbot the model renders at about 39-49% of the preview height, horizontally
  centred, and `distanceMultiplier` between 0.5 and 1.0 scales it predictably.
  `targetY` shifts it vertically only above roughly 30 units, because the model
  sits at a large Y offset (centre near Y=29 in authoring units).
- The app writes its own log to `files/logs/operit.log`; `adb logcat` shows
  almost nothing from this code, so read that file instead:
  `adb shell "run-as com.ai.assistance.operit.debug cat files/logs/operit.log"`.

The log line `Camera auto-fit[SOURCE]` reports which bounds were used and the
resulting distance, so the effect of a value is visible in `adb logcat -s
GltfSurfaceView`. This is a testing aid: it reads a file the user would have to
create by hand, and the app behaves normally without it.

### What the preinstalled models actually are — 2026-11-06

Measured from the files, not guessed:

| model | clips | verdict |
| --- | --- | --- |
| Xbot | agree, headShake, idle, run, sad_pose, sneak_pose, walk | Structurally sound. Rest pose is a T-pose. `sad_pose` and `sneak_pose` are 2-frame 0.07 s stills, not motion. Walk/run/sneak carry root translation. |
| Fox | Survey, Walk, Run | Two scene roots (skeleton and mesh are siblings), geometry in centimetres with no root scale. |
| Morph Cube | Square | Morph-target cube only, node scale 100 and a combined X/Y rotation. Morph targets move only top-face vertices. |
| Soldier | Idle, Run, TPose, Walk | Sound; `TPose` is a 2-frame still. |

**Mood coverage is the real problem, not the rendering.** None of the four
expresses the five product moods (angry, happy, shy, aojiao, cry) with a
dedicated clip. At best Xbot can be faked: `agree` reads as a weak happy,
`headShake` as a weak aojiao. There is no anger, shyness or crying clip
anywhere. These presets are locomotion and tech demos, not character avatars.

- [ ] K4. Replace the preinstalled presets with models that ship mood clips.
      All current ones are to be dropped. Search brief for an external
      research model: `AVATAR-MODEL-SEARCH-BRIEF.md`.
- [x] K5. The camera fit is accepted on Xbot: with the defaults the model
      renders centred horizontally at about 39% of the preview height and is
      fully visible. The tuner stays as a testing aid for the models still to
      come; it does nothing unless its file exists.
- [ ] K6. Decide whether the avatar preview should always play the default clip,
      so a T-posed rig never shows its rest pose.

## Status snapshot — 2026-11-05

Written down so the next session does not have to re-derive it by reading
sections I and J again.

**Everything except I and J is finished.** Sections A through H are closed, and
the theme-editor avatar scare is closed as a false alarm (section H2). There is
no open code defect anywhere in the backlog. What remains is two Colab-dependent
research tracks, and both need a GPU VM that cannot currently be rented.

**Section I in one paragraph.** `tools/colab/qwen_openai_server.py` is written
and was proved on a real T4: a request byte-for-byte like the one
`AvatarImageGenerator` sends (`512x512`, `b64_json`) came back as a valid
512x512 RGBA PNG, 210800 bytes, in 67 s. The OkHttp timeouts that killed that
request were fixed in `ac8cf2d7` (30 s connect, 10 min read, 1 min write, 12 min
call). What has never happened is a generation started *from the installed app
on the phone*. I1 answers exactly that question and nothing else. I2-I3 are
breadth (every avatar kind, both style sets). I4 is a decision, not work, and it
is blocked by I5, the licence question: Qwen-Image-2.1 ships under
`qwen-research`, and whether its outputs may be redistributed is still unknown.
I6 is cost measurement.

**Section J in one paragraph.** Not started, no code exists. The goal is an
animated avatar built from an existing portrait, which is why J2 (image-to-video)
matters more than text-to-video: a clip made from a text prompt is not a
portrait of the user's assistant. Five candidates are listed with their claimed
VRAM, and the recommended first pilot is CogVideoX-2b because it has an official
free-T4 notebook and the lowest claim. J3 compares any result against the glTF
presets already shipped, and the phone must actually be able to play the format.

**Why nothing moved since 02.11.** The Colab account is drained. On 02.11 the
web page showed no CPU quota left while CPU sessions were still being granted,
and `colab new --gpu T4` keeps answering "Service Unavailable". A fresh account
gets roughly 60 CPU hours or roughly 5.5 GPU hours from the same pool, the
figures are not fixed, and only the Colab web page shows the real remainder. On
05.11 the environment still could not rent a T4. Likely cause of the drain:
leftover GPU and CPU sessions that the idle reaper never reclaimed; all
server-side sessions were released on 02.11 and `colab sessions` reported none.

**Practical trap, worth remembering.** `colab sessions` lists server-side
assignments by endpoint id, while `colab stop -s <name>` only knows the name it
was created under. A leftover assignment therefore answers "Session not found"
while still burning quota. The release procedure that works is written down in
`COLAB-AND-QWEN-IMAGE.md` (GET the endpoint to obtain a token, then POST with
that token in `X-Goog-Colab-Token`; expect 204).

**What to do first when the quota is back.** Check the Colab web page, not the
CLI. If a GPU is available, run I1 only: start the T4, run
`tools/colab/setup_colab.sh`, start `tools/colab/qwen_openai_server.py`, open the
SSH tunnel, point the app's Backend field at it and generate one portrait.
Everything else in I and J is only worth doing after that single end-to-end path
is seen working from the phone.

## Verification

- [x] V1. `python3 ci/script/fork_audit.py --no-color --no-hotspots` (pass, 44 anchors, 0 lost).
- [x] V2. CI green (`41218273980`, commit `5da03469`).
- [x] V3. On-device smoke test at 360 dp / font scale 1.0 (terminal Home toolbar anchors not reached; setup anchor verified).
