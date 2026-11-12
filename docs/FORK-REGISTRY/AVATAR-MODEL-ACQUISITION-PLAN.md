# Plan: acquire avatar models and motion for the five moods

Status: planning. Written 2026-11-10 after cross-checking the two external
reports against the measured facts of this fork. Sources marked "verified" were
fetched in this session (HTTP 200 and content inspected); "reported" means the
external report claims it and it has not been checked here yet.

Related: `AVATAR-MODEL-SEARCH-BRIEF.md` (what we ask for),
`TODO-AUDIT.md` K4/K5 (measured model reality), `AVATAR-3D-RESEARCH.md`
(formats and the asset repository layout).

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
