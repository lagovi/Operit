# Plan: acquire avatar models and motion for the five moods

Status: planning. Written 2026-11-10 after cross-checking the two external
reports against the measured facts of this fork. Sources marked "verified" were
fetched in this session (HTTP 200 and content inspected); "reported" means the
external report claims it and it has not been checked here yet.

Related: `AVATAR-MODEL-SEARCH-BRIEF.md` (what we ask for),
`TODO-AUDIT.md` K4/K5 (measured model reality), `AVATAR-3D-RESEARCH.md`
(formats and the asset repository layout), `AVATAR-RETARGET-PLAYBOOK.md` (the
verified how-to for combining a character with clips into one multi-mood `.glb`).

## What is fixed by the product

The five moods are `angry`, `happy`, `shy`, `aojiao`, `cry`. The renderer maps
clips by name in the Assistant → Avatar screen, so a library clip can be renamed
to the mood key at packaging time. We can convert formats, repack textures,
merge a motion library onto a character, rescale and fix roots. We cannot fix a
licence, and we cannot invent motion that was never captured.

The measured reality of the current presets (TODO-AUDIT K4): Xbot, Fox, Soldier,
Morph Cube carry locomotion and tech-demo stills only. None has a real anger,
shyness or crying clip. So all current presets are to be replaced.

## The two-part answer this plan is built on

Three of the five moods (shy, aojiao, cry) barely exist in generic packs. The
reliable way to cover them is a character under one permissive licence plus a
motion library under another permissive licence. Two licence halves are fine as
long as both allow redistribution and derivatives.

## Inspection results from the first download batch (2026-11-10)

Downloaded locally to `/tmp/opencode/models/vrma/` and `/tmp/opencode/models/m2m/`
(scratch, not committed). What was checked by parsing the files, not by trusting
the report:

- **VRMA set (11 files, MIT).** Each is a valid glTF 2.0 binary with one
  animation, 51 nodes, 52 channels, length 3.93 s, and a large quaternion spread
  (~1.0), so they contain real motion, not placeholder poses. All 11 have
  distinct sha256. `Blush` (shy) and `Sad` (cry) really exist and are animated,
  which is the rare part.
- **Mesh2Motion emotions/emotes (CC0).** The `.blend` files are genuine Blender
  data (zstd-compressed, ~1 MB unpacked each). There is no human GLB in the
  repository, only Blender sources, and no Blender is available on this machine,
  so exporting them is a separate step on a machine with Blender.
- **incarna mirror (MIT, `andrewsegas/incarna`).** Ships `actions.json` with the
  exact tag → clip → face mapping. Confirmed: `angry -> Angry`, `sad -> Sad`,
  `shy -> Blush`, `clap -> Clapping`, `think -> Thinking`, `lookaround ->
  LookAround`. It also records `happy` as face-only, which is why a body clip
  (`Clapping`) is used for happy there.

## Tier 1 - download first (best coverage, verified permissive)

1. **Mesh2Motion human animations - CC0. Verified.**
   - https://github.com/Mesh2Motion/mesh2motion-assets
   - Licence statement (verified, 2026-11-10):
     https://github.com/Mesh2Motion/mesh2motion-app/blob/main/LICENSE-CC0.MD -
     "All 3d models, blend files, rigs, animations - CC0 1.0 Universal".
   - What it is: per-clip Blender source (`.blend`) on a humanoid rig, organised
     in folders. Verified folder contents include `emotions/` and `emotes/`.
   - Verified clips relevant to moods:
     - angry: `emotions/animation-human-angry.blend`
     - cry: `emotions/animation-human-sit-ground-crying.blend`,
       `emotions/animation-human-tired-hunched.blend` (weak cry fallback)
     - shy: `emotions/animation-human-shiver.blend`,
       `emotions/animation-human-tired-hunched.blend`
     - happy: `emotes/animation-human-clapping.blend`,
       `emotes/animation-human-greeting.blend`,
       `emotes/animation-human-head-nod.blend`,
       `emotes/animation-human-victory.blend`,
       `locomotion/animation-human-walk-happy.blend`
     - aojiao: `emotions/animation-human-reject.blend` (turn away / refuse)
       combined with `angry` (pout then soften) - closest available.
   - Gap: shy and aojiao have no dedicated clip; they need a fold of `reject`
     plus `shiver`, or a small hand edit. There is no separate "blush".
   - Format problem for us: Blender sources, not GLB. We export them ourselves
     onto a chosen character rig.

2. **tk256ailab/vrm-viewer VRMA set - MIT. Verified.**
   - https://github.com/tk256ailab/vrm-viewer/tree/main/VRMA
   - Licence (verified): MIT, "Copyright (c) 2025 TK256".
   - What it is: 11 clips, each a valid glTF 2.0 binary (`.vrma`, one animation,
     51 nodes, 52 channels), aimed at VRM humanoid. Verified by parsing the
     files: all 11 are distinct (different sha256), not placeholders.
   - Verified clip list: `Angry`, `Blush`, `Clapping`, `Goodbye`, `Jump`,
     `LookAround`, `Relax`, `Sad`, `Sleepy`, `Surprised`, `Thinking`.
   - Per-mood fit: angry = `Angry`; happy = `Clapping` or `Jump`; shy =
     `Blush` (hands near face, the "tereru" motion - this is the rare one and it
     exists); cry = `Sad`; aojiao = `Blush` + `Angry` + `LookAround` stitched, or
     `Angry` then soften.
   - This is the best single file set for **shy**, which almost every other
     source lacks. It is motion-only; it needs a VRM character.
   - Same set mirrored in `andrewsegas/incarna` (reported) with an
     `actions.json` that already binds a mood tag to both a clip and a face
     blendshape (e.g. shy = `Blush` + relaxed face). Useful as a mapping
     reference, not as a download.

3. **Quaternius Universal Animation Library 1 + 2 - CC0. Reported.**
   - https://quaternius.com/packs/universalanimationlibrary.html and
     https://quaternius.itch.io/universal-animation-library
   - ~85 usable clips on a Quaternius mannequin (GLB/FBX/Blend). Covers
     locomotion, combat and some emotes (`Dance_Loop`, `Punch...`,
     `Counter Angry`). Happy and angry yes; shy / aojiao / cry no.
   - Purpose here: bulk locomotion and idle plus base character meshes, not the
     rare moods.

## Tier 2 - for the rare moods (mocap databases, permissive)

4. **CMU Motion Capture Database - free for any use. Reported, site verified.**
   - http://mocap.cs.cmu.edu/ (search) and the index
     https://github.com/una-dinosauria/cmu-mocap/blob/master/cmu-mocap-index-text.txt
   - Named targets to pull, from the external report (needs confirming against
     the index and by watching the BVH):
     - cry: 79_72 crying, 79_71 sad
     - angry: 18_10, 18_11 quarrel - angry hand gestures; 79_74 upset
     - shy: 79_73 scared (timid posture), partial only
     - aojiao: no clean clip; build from 79_74 upset (turn away) + a huff
   - Raw ASF/AMC/BVH; needs retarget and trim. Heavy but the licence is the most
     permissive of all and the cry/angry clips are direct hits.

5. **MPI Emotional Body Expressions Database - CC BY 4.0. Reported.**
   - https://pmc.ncbi.nlm.nih.gov/articles/PMC4252031/
   - 1447 stationary mocap clips, labelled by emotion: anger, joy, shame/
     embarrassment (shy), pride/contempt (aojiao base), sadness (cry), plus
     surprise and fear. BVH/MVNX. This is the cleanest **labelled** emotion set
     and maps almost one-to-one onto our moods. It is stationary (standing
     expression), which suits an avatar that mostly stands.

6. **100STYLE Dataset - CC BY 4.0. Reported, site verified.**
   - https://www.ianxmason.com/100style/
   - Style-tagged BVH: `Angry_ID`, `Depressed_ID` (cry), `Elated_ID` (happy),
     `Proud_ID` (aojiao base), `ShieldedRight`/`CrowdAvoidance` (shy, closed
     posture). Locomotion-flavoured (walking in a mood) rather than standing
     emotes, so better as a backup than a first pick.

7. **Bandai Namco Research Motion Dataset 1 + 2 - CC BY-NC 4.0. Reported,
   repo verified.**
   - https://github.com/BandaiNamcoResearchInc/Bandai-Namco-Research-Motiondataset
   - Huge (400k frames), professional actors, style labels: `happy`, `angry`,
     `sad`, `proud`, `not-confident`, `active`, etc. Style `not-confident` is a
     strong shy, `proud` a strong aojiao base, `sad` the cry.
   - Blocker: CC BY-NC 4.0 forbids commercial use. The app itself is free and
     open source, so this is arguably fine for shipping **as a non-commercial
     asset pack with attribution and a non-commercial notice**, but it must be
     decided deliberately (see Licensing decision below). Best motion quality of
     the academic sets and covers all five moods.

## Tier 3 - characters to put the motion on (permissive)

8. **VRoid Studio sample models - CC0** (`AvatarSample_A` etc.), anime
   characters with blendshapes for happy/angry/sad/surprised. Glue onto the VRMA
   set directly (same VRM humanoid rig).
9. **ToxSam / 100Avatars, open-source-avatars - CC0 / CC-BY. Reported, repo
   verified.** https://github.com/toxsam/open-source-avatars - a registry of
   VRM avatars with a machine-readable list (`projects.json`) and bone-mapping
   docs. Cheapest way to get many redistributable characters.
10. **Mirai Komachi VRM - free for non-commercial. Reported.** The rig the
    Bandai Namco data was captured against, so retargeting is exact.
11. **Quaternius base characters / KayKit Adventurers - CC0**, stylised low-poly
    meshes, 5-15k triangles, good pairs for the Quaternius motion.

## Explicitly blocked (do not download for shipping)

- **Adobe Mixamo** (and its conversions: Three.js Xbot/Soldier, `clawatar`
  VRMA): forbids redistributing the raw animation/character data. Our Xbot and
  Soldier presets already come from these; they must not be republished.
- **Ubisoft La Forge LAFAN1 and ZeroEGGS**: CC BY-NC-ND 4.0 - the ND module
  forbids derivative `.glb` and even forbids retargeting into our own model.
- **Pixiv / VRoid "7 Free VRM Animations"**: restrictive on redistributing the
  raw files in third-party repositories.

## Licensing decision that needs to be made once

The app is a free, open-source, non-commercial fork. Two licence classes are
therefore open to us:

- **Fully permissive route (CC0 / MIT / CC BY):** ship Mesh2Motion (CC0) +
  VRMA set (MIT) + Quaternius (CC0) + VRoid/100Avatars characters (CC0). This
  needs no legal judgement, covers angry/happy/cry well and shy via `Blush`.
  Cost: aojiao has to be assembled (reject + pout) rather than downloaded.
- **Non-commercial route (CC BY-NC):** add Bandai Namco (and MPI is CC BY, fine
  either way). This gives the best motion and a real shy/not-confident and proud
  set. Cost: the asset pack carries a non-commercial notice and must be kept out
  of any commercial redistribution; it also constrains anyone who forks further.

Recommended: build the **fully permissive route first**, and only reach for the
NC route if the assembled aojiao looks wrong on the device. The permissive route
already satisfies "every mood has a credible clip".

## Internal-only download, kept on purpose (not for shipping)

The user asked to also grab licence-restricted models purely to test quality
internally, and decided they are **kept, not deleted**, because many builds and
internal tests are expected and Mixamo's licence permits download and use. They
stay outside the shipped asset pack: never committed to the app or the asset
repository, never included in a released APK, but retained locally for repeated
testing. Worth having on hand:

- **Mixamo** emotion clips ("Angry Gesture", "Defeated", "Thinking", etc.) - the
  widest set of clean standing emotes, instantly readable. A quality benchmark
  for how a mood should look, and a target to imitate with a permissive clip.
- **ZeroEGGS / LAFAN1** for gesture richness - same rule: internal benchmark
  only, kept locally.

The value is calibration: once a Mixamo "Sad" has been seen on the phone, we can
judge whether the CC0 `Sad.vrma` reads as well, and only then commit to a
permissive clip or invest in hand-fixing. Because these stay available, the
comparison can be repeated on any future build instead of being redone once.

Source used for the benchmark: `dongping-chen/clawatar` (`public/animations/`).
The repository code is MIT; its README states the 162 animations are Mixamo,
"non-commercial use, credit required". Downloaded (kept locally at
`/tmp/opencode/models/mixamo-benchmark/`, not committed), each verified to parse
as glTF with real motion:

| file | duration | maps to |
| --- | --- | --- |
| `0_Angry` | 19.2 s | angry |
| `94_Angry Gesture` | 2.0 s | angry |
| `116_Happy Hand Gesture` | 2.6 s | happy |
| `49_Joyful Jump` | 1.8 s | happy |
| `19_Clapping` | 1.2 s | happy |
| `22_Crying` | 6.3 s | cry |
| `142_Sad Idle` | 3.6 s | cry / sad |
| `26_Defeat` | 6.6 s | cry / sad |
| `53_Loser` | 3.3 s | cry / sad |
| `64_Rejected` | 4.6 s | aojiao base |
| `56_No` | 4.5 s | aojiao base |
| `144_Shaking Head No` | 1.6 s | aojiao |
| `145_Shrugging` | 1.9 s | shy |
| `127_Leaning` | 3.0 s | shy |
| `156_Thankful` | 2.8 s | shy / soft |
| `161_Waving` | 2.9 s | happy / greeting |
| `118_Head Nod Yes` | 2.3 s | happy |

`Crying` and `Crying_2` are byte-identical in the source; one copy is enough.

## Order of work: look first, then touch Blender

The user's rule, decided 2026-11-12: before any Blender work on a model, it has
to be checked on the phone as-is - does it show up at all, and does the user
like how it looks. Only a model that renders and is visually acceptable is worth
retargeting or editing in Blender. This avoids investing Blender/Colab time in a
character that is rejected on sight or does not render for a structural reason.

The fox is the precedent: it did not render at all (centimetre authoring under
two scene roots) and was fixed in the data with a root scale, no Blender needed,
and then judged on the phone (see TODO-AUDIT.md, "Fox resolved in the data").

## Concrete next actions

1. Download Mesh2Motion emissions + emotes + relevant locomotion `.blend`, and
   the 11 VRMA files. Inspect each in Blender / a glTF viewer and rate it against
   the five-mood table. This is a local research batch, no push.
2. Download VRoid sample + one 100Avatars character; retarget the VRMA set onto
   it (VRM Add-on for Blender imports both); export one test `.glb` with clips
   renamed `angry`, `happy`, `shy`, `aojiao`, `cry`; put it on the phone through
   the existing preset path and look at it.
3. If aojiao or shy reads poorly, pull the CMU 79_72/18_10/79_73 and MPI
   `shame`/`pride` clips and compare.
4. Only if permissive still falls short, decide the CC BY-NC question for
   Bandai Namco and record the decision in K4.
5. Download Mixamo/ZeroEGGS for the internal benchmark, keep them locally
   (not committed, not shipped) so the comparison can be repeated on later
   builds.

## Source-by-source assessment (2026-11-12, after the K7 build)

This section was written after the retarget pipeline was proven end to end (see
`AVATAR-RETARGET-PLAYBOOK.md`). It says, for every source gathered earlier, how
well it fits the five moods, what has to be finished by hand, and the exact
procedure. "Verified" means inspected here; "reported" means only claimed by an
external report and not checked. The five moods are angry, happy, shy, aojiao,
cry.

The pipeline that consumes the result takes one character plus one clip per
mood and writes a single `.glb` with five named animations. So the practical
question per source is only: can it give a **VRM-compatible humanoid clip** or a
**character**, and how much work is between the raw download and that clip.

### 1. tk256ailab VRMA set — MIT (best single motion source)

Verified: 11 files, each a glTF binary carrying `VRMC_vrm_animation`, one
animation with 52 channels, length 3.93 s, distinct sha256, real motion.
Clips: `Angry`, `Blush`, `Clapping`, `Goodbye`, `Jump`, `LookAround`, `Relax`,
`Sad`, `Sleepy`, `Surprised`, `Thinking`.

- **Fit.** angry = `Angry`; happy = `Clapping` or `Jump`; shy = `Blush` (the
  rare "tereru"/flustered motion almost no other permissive source has);
  cry = `Sad`; aojiao = the weakest — no dedicated clip, so it needs `Angry`
  then soften, or `Blush` + `LookAround` stitched, or accept `Angry` as
  "tsundere annoyance". Four of five are strong, aojiao is the gap.
- **Rework needed.** Almost none on the motion side. These load directly through
  `import_scene.vrma` (they already carry the extension). For aojiao, the rework
  is authoring: trim/stitch two clips into one mood clip, or accept a single
  clip. Stitching is done in Blender by placing two actions back to back on one
  NLA track before export, then renaming the track to `aojiao`.
- **Procedure.** Choose clips, run `tools/colab/build_avatar_moods.py` with
  `--clip angry=.../Angry.vrma` etc. against a CC0 character. For aojiao,
  concatenate two actions first (add a small script step that appends the second
  action's keys after the first action's last frame on the same track).
- **Verdict.** Primary motion source. This is what the shipping build should
  use for angry/happy/shy/cry, and the base for aojiao.

### 2. Quaternius Universal Animation Library 1 + 2 — CC0 (characters + bulk motion)

Reported, not yet fetched here. Large clip library (~85 usable) on a Quaternius
mannequin, GLB/FBX/Blend. Locomotion, combat, some emotes (`Dance_Loop`,
`Punch`, `Counter Angry`), plus stylised low-poly base characters.

- **Fit.** Characters: good, CC0, small, and easy to pair. Motion: happy and
  angry yes, shy/aojiao/cry no. So it is a **character and filler** source, not
  a mood source.
- **Rework needed.** Its rig is Quaternius' own, not VRM humanoid, so its
  animations do not drop into our pipeline; only its **meshes** are directly
  useful, and those need to be rigged to a VRM/Mixamo humanoid before they can
  carry the VRMA clips. Rigging a mesh from scratch is real work (weight
  painting, bone mapping), so treat Quaternius characters as a fallback if the
  CC0 VRM characters (below) are rejected visually.
- **Procedure.** Prefer a ready-rigged VRM character over a Quaternius mesh. If
  a Quaternius character is chosen, import it, bind it to a humanoid armature
  (or retarget its own rig to VRM humanoid with the add-on), then use it as the
  `--character` for the same build.
- **Verdict.** Secondary, for variety in characters; not a mood clip source.

### 3. Mesh2Motion human animations — CC0 (rich emotion motion, heavy rework)

Verified by opening the files: 8 `.blend` sources here, each **zstd-compressed**
and saved by **Blender 5.3** (header `BLENDER17-01v0...`). Two real obstacles,
both measured, not guessed:

- the files are zstd streams; Blender's own open fails on the compressed
  original, so they must be decompressed with `zstd -d` first;
- even decompressed, the save format is **5.3**, and Blender 4.2 (what the
  Colab session uses) refuses a file from a newer major version. Opening them
  needs **Blender 5.2/5.3**, which is downloadable (`download.blender.org/
  release/Blender5.3/`).

Verified on 2026-11-12 with Blender 5.3.0: the `.blend` files open **directly,
without decompressing first** (5.3 reads the zstd stream itself). The rig is a
147-bone control rig (`DRV_*`/`CTRL_*`/`POLE_*`/`FK_*`) with the deform bones
named close to humanoid (`DRV_hips`, `DRV_spine`, `DRV_head`, `DRV_upper_arm_L`,
`DRV_forearm_L`, `DRV_thigh_L`, `foot_l`, `hand_l`); the file carries a single
action (`Angry` etc.) and a 0-vertex `Mannequin` placeholder mesh, i.e. motion
only, no skin. Note the 5.x action API change: `action.fcurves` no longer exists,
fcurves live under `action.layers[].strips[].channelbags[].fcurves`.

Clips here: `angry`, `clapping`, `greeting`, `head-nod`, `reject` (aojiao base),
`shiver` (shy), `sit-ground-crying` (cry), `tired-hunched` (weak shy/cry).

- **Fit.** Best **hand-authored emotion** set of the permissive options: a real
  angry, a real crying, a reject turn-away for aojiao, a shiver for shy. This is
  closer to "an actor performing an emotion" than the game-clip VRMA set.
  Gap: no dedicated shy blush (shiver is the nearest), and aojiao is "reject",
  which reads as refusal, not sulky-cute.
- **Rework needed.** Considerable, in three steps: (1) decompress with `zstd`;
  (2) open in Blender 5.3, not 4.2; (3) the rig is Mesh2Motion's humanoid, not
  the target character's, so each clip must be retargeted onto the chosen
  character. The VRM Add-on T-pose route works only between VRM-humanoid rigs;
  a Mesh2Motion rig first needs its bones mapped to a humanoid (auto-assign in
  the add-on), then its action exported to VRMA or baked onto the character.
- **Procedure.** In Blender 5.3: decompress, open, auto-map bones to VRM
  humanoid, then either export each clip as an animation-only glTF and run it
  through the same `import_scene.vrma` step, or bake the action onto the target
  armature directly with `nla.bake(visual_keying=True)` and stash it. Because
  4.2 cannot open the files, this step needs a second Blender in the session
  (or a 5.3 session), which is extra setup but not a blocker.
- **Verdict.** The quality ceiling for angry/cry/aojiao if the VRMA set feels
  too "game-like". Expensive: two Blenders and a retarget per clip. Worth it
  only for the moods where the VRMA set visibly disappoints on the phone.

### 4. CMU Motion Capture Database — free, most permissive (targeted mocap)

Reported clip names to pull: cry `79_72`, sad `79_71`; angry `18_10`, `18_11`,
`79_74`; shy/timid `79_73`; aojiao has no clean clip (build from `79_74` + a
huff). Raw ASF/AMC/BVH.

- **Fit.** The cry and angry clips are direct, natural hits. shy is "scared",
  only partial. aojiao is absent. Licence is the most permissive of all.
- **Rework needed.** Heavy: BVH/AMC must be imported into Blender, retargeted to
  the character's humanoid, trimmed to a few seconds, and cleaned (the raw
  capture is long and has no looping). No skin, no character — motion only.
- **Procedure.** Import the chosen BVH in Blender, map its joints to the
  character's humanoid, `nla.bake` onto the character, trim, stash, export. Same
  tail as Mesh2Motion.
- **Verdict.** Reach for this only if cry or angry from the VRMA set is
  rejected. It is the "borrow a real performance" option.

### 5. MPI Emotional Body Expressions — CC BY (labelled emotions)

Reported: 1447 stationary mocap clips labelled anger / joy / shame (shy) /
pride (aojiao base) / sadness (cry). BVH/MVNX. Standing expression, which suits
a mostly-standing avatar.

- **Fit.** The cleanest one-to-one emotional labelling; "shame" and "pride"
  directly target our two hardest moods (shy, aojiao).
- **Rework needed.** Same as CMU: import, retarget to humanoid, trim, bake. CC BY
  means attribution is required in the shipped credits.
- **Procedure.** Identical tail to CMU. Pick one clip per mood by label, retarget
  and bake onto the character.
- **Verdict.** Best fallback specifically for shy and aojiao if the permissive
  clips read poorly. Requires the attribution plumbing.

### 6. 100STYLE Dataset — CC BY (styled locomotion)

Reported: `Angry_ID`, `Depressed_ID` (cry), `Elated_ID` (happy), `Proud_ID`
(aojiao), closed postures for shy. Style-tagged BVH but **locomotion**
(walking in a mood), not standing emotes.

- **Fit.** Poor for a standing avatar preview that mostly stands still: walking
  expressions would look odd in the preview. Angry/proud labels are useful
  concepts, but the delivery is a walk.
- **Rework needed.** High, same mocap retarget as CMU, plus the walk has to be
  cut to a standing beat.
- **Verdict.** Backup only. Use if a "mood" is better conveyed as a short walk
  cycle, which the current preview does not suit.

### 7. Bandai Namco Research Motion Dataset 1 + 2 — CC BY-NC 4.0 (best quality, licence wall)

Reported: huge (400k frames), professional actors, style labels `happy`,
`angry`, `sad`, `proud`, `not-confident` (strong shy), etc.

- **Fit.** Best motion quality and covers all five moods, including a genuine
  `not-confident` shy and a `proud` aojiao base.
- **Blocker.** CC BY-NC 4.0 forbids commercial use. The app is free and open
  source, so shipping as a non-commercial asset pack with attribution is
  arguably allowed, but this is a deliberate licensing decision (K4), not a
  technical step, and it constrains anyone who forks further.
- **Rework needed.** Mocap retarget as CMU/MPI, plus a written non-commercial
  notice in the credits.
- **Verdict.** Only if the fully permissive route visibly fails for a mood. Do
  not take this path by accident; record the decision in K4 first.

### 8. VRoid Studio sample models — CC0 (characters)

Reported CC0 anime characters with expressions (happy/angry/sad/surprised). Note
the caveat found while building: a VRM whose `licenseName` is "Other" with no
licence URL is refused by the VRM Add-on (`LicenseConfirmationRequired`). Only a
sample whose metadata states CC0 (or another allowed licence) will import.

- **Fit.** Ready-rigged VRM humanoid characters, so they take the VRMA clips
  directly through the proven pipeline with zero rigging. Strongest character
  option.
- **Rework needed.** None if the licence is CC0 and the metadata says so. Pick,
  verify the licence in the file, run the build.
- **Verdict.** Primary character source. The only work is choosing one the user
  likes visually.

### 9. 100Avatars / ToxSam open-source-avatars — CC0 / CC-BY (characters)

Verified: the registry lists 300 avatars with per-avatar JSON and a VRM
download URL; the sampled "Devil" (Polygonal Mind) has explicit
`licenseName: CC0` and imported and animated correctly as the K7 test subject.

- **Fit.** Ready-rigged VRM humanoids, CC0, many to choose from. Same zero-rigging
  benefit as VRoid.
- **Rework needed.** None if CC0; for CC-BY entries, add attribution.
- **Verdict.** Primary character source alongside VRoid. The Devil was only a
  functional test subject; the shipping character is a visual choice for the
  user, avoiding ones whose theme clashes with the moods (devil horns, etc.).

### 10. Mirai Komachi VRM — non-commercial (exact rig for Bandai Namco)

Reported: the rig the Bandai Namco data was captured against, so retargeting is
exact. Free for non-commercial use.

- **Fit.** Only relevant if the Bandai Namco (CC BY-NC) route is taken; then it
  gives a perfect retarget.
- **Rework needed.** None for retarget; but it inherits the NC licence.
- **Verdict.** Tie it to decision 7; not needed on the permissive route.

### What this adds up to

- **Permissive route (recommended first):** character from VRoid sample or
  100Avatars (CC0); motion from the tk256ailab VRMA set for angry/happy/shy/cry;
  aojiao assembled (Angry + soften, or Blush + LookAround). This needs no new
  Blender versions and no rigging — only the proven build plus, for aojiao, a
  small stitch step.
- **If a mood reads weak on the phone:** Mesh2Motion (needs zstd + Blender 5.3 +
  retarget) or CMU/MPI (needs mocap retarget) for that one mood. Both are more
  work and each adds its own tail (extra Blender, or attribution).
- **Only then:** Bandai Namco CC BY-NC, with the licence decision written down.

## Cloud catalog and in-app import (implemented 2026-11-12)

The three mood avatars above are published in `lagovi/OperitAvatarAssets`
(`presets/gltf/{rose,kate,jennifer}/model.glb`), each 100Avatars CC0 characters
(Polygonal Mind) with five retargeted moods from the tk256ailab VRMA set (MIT).
The build used Blender 5.3.0 on Colab with the same
`tools/colab/build_avatar_moods.py`; the VRMA clips are Angry, Jump, Blush,
Thinking, Sad mapped to angry/happy/shy/aojiao/cry.

Every preset in the catalog now carries a `thumbnail` path
(`presets/gltf/<id>/thumbnail.png`, 512x512, rendered headless with Workbench
and auto-centred). The app fetches `presets.json`, and the cloud gallery
(`AvatarPresetGalleryDialog`, opened by the cloud icon next to the model
dropdown in Assistant Config) draws `thumbnail` next to `name` and `summary`.
`AvatarPresetRepository.thumbnailUrlFor` turns the catalog-relative path into an
absolute URL; Coil (with the app's disk cache) loads and caches it, so the list
is browsable offline after the first view.

This is the "import from cloud" flow: open Assistant Config, tap the cloud
icon, the list of repository models appears with a preview and a one-line
summary, tap one to download and install it into the avatar list.
