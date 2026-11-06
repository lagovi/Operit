# 3D avatar formats and how they are made

Companion to `AVATAR-RESEARCH.md`. This file covers the animated avatars used
by the Assistant (desktop pet and voice call), not the square portrait images.

## What the app can load

| Format | Type | What it is | Animations |
| --- | --- | --- | --- |
| glTF / GLB | 3D | Mesh + skeleton + optional clips | named clips, mapped to moods |
| FBX | 3D | Modeling interchange format | clips after conversion |
| MMD (.pmx/.pmd + .vmd) | 3D | MikuMikuDance character + motion | motions, not clips |
| DragonBones | 2D | Skeleton of textured parts | named timelines |
| WebP | 2D | Frame sequence | frames per state |
| MP4 | 2D | Video clips | looped states |

The renderer discovers animation names at load time and maps them by name in
the Assistant → Avatar screen. `AvatarEmotion` has seven states: idle,
listening, thinking, happy, sad, confused, surprised. A model that only has one
clip still works, it just shows the same motion for every state.

## How each format is produced

### glTF / GLB

The practical modern path: build or rig in Blender, export glTF 2.0. A model
is a mesh, a skeleton with weighted vertices, and named animation clips. Ready
made humanoid rigs exist (Ready Player Me, Mixamo, VRoid), so the expensive
part is the character, not the format. Export must avoid extensions the
renderer does not implement: Draco compression, WebP textures, KHR_materials_*
beyond basic PBR. Plain embedded .glb is the safe target.

### FBX

Same authoring tools, different container. Autodesk format, closed spec, but
Blender and most DCC tools read and write it. In this app it is a secondary
option to glTF because export settings are fiddly and external textures are
common. Converting FBX to glTF in Blender is the cleaner route.

### MMD

PMX/PMD are MikuMikuDance models with their own rigging conventions and toon
materials. Motions are separate `.vmd` files. Creating one requires MMD-aware
tools (PMXEditor, Blender MMD Tools). The result is culturally specific and
licensing is often restrictive per model. Good for enthusiasts, not the
default recommendation.

### DragonBones

2D cutout animation: a texture atlas plus a skeleton of bones that move image
parts. The DragonBones editor (online, or old desktop 5.6.3) exports the JSON
and texture files the app imports. This is the cheapest way to get an animated
character that looks hand drawn, and it needs no 3D rigging skill.

### WebP sequence and MP4

Not models but frames. A WebP sequence is a set of images per state; an MP4 is
one short clip per state. Both can be produced from any 2D or 3D source by
rendering frames (Blender, After Effects, or a video generator). Because there
is still no tracking, each state is just a looping clip, and file size grows
with resolution and length.

## Making something new, ranked by practicality

1. **Generate a series of images, then turn them into a WebP sequence.** The
   app already supports WebP states. An image model can produce a consistent
   character in several expressions; a small loop of 6–12 frames per state is
   enough for a pet. No 3D knowledge, no local heavy tools.
2. **Generate video per state, use MP4.** Same idea with motion. The risk is
   temporal consistency and size; short 2–4 second loops are sensible.
3. **Build a humanoid in Blender, rig with a standard skeleton, export GLB.**
   Best quality and real reactive animation, but needs real 3D work. This is
   where a guided workflow helps.
4. **DragonBones in the 2D editor.** Good middle ground for hand-drawn looks.
5. **MMD.** Keep as an import path for users who already have models.

## What "a helper for your own model" can be

Two levels, both useful without pretending to be a 3D editor.

**Import doctor.** After the user picks a file or zip, inspect it before it
reaches the renderer: does the glTF declare extensions we cannot load, are
textures missing, is it a zip with the right structure, which animations were
found. Report the exact problem and, where possible, repack. This turns today's
silent failure into a clear next step.

**Blender guide and skill for the agent.** Ship a written guide plus an agent
skill that walks a user through Blender: install the glTF exporter, use a
standard rig, create the seven mood clips, export a self-contained GLB. If the
user runs Blender locally with the MCP bridge, the same agent can drive it.
This is a document and prompt package, not code running inside Operit.

## What to put in the fork repository

Repository: `https://github.com/lagovi/OperitAvatarAssets`, branch `master`.

Catalog `presets.json` with `schema: operit.avatar.presets.v1`. Each entry has
`id`, `type`, `name`, `summary`, `model`, `license`, `licenseFile`,
`sizeBytes`, `sha256`, `animations`, `defaultAnimation`.

Seeded glTF presets:

| id | model | size | clips |
| --- | --- | --- | --- |
| gltf.fox | Fox | 163 KB | Survey, Walk, Run |
| gltf.xbot | Xbot | 2.9 MB | agree, headShake, idle, run, sad_pose, sneak_pose, walk |
| gltf.soldier | Soldier | 2.2 MB | Idle, Run, TPose, Walk |
| gltf.cesiumman | Cesium Man | 438 KB | one unnamed clip |
| gltf.riggedfigure | Rigged Figure | 50 KB | one unnamed clip |
| gltf.cube | Morph Cube | 7 KB | Square |

Folders for `dragonbones`, `webp`, `mp4`, `mmd` are present but empty. Each
preset carries its own `LICENSE.txt`; the Xbot and Soldier entries are Mixamo
exports taken from the three.js examples and their terms should be reviewed
before broader redistribution.

## Consequences

- Network: the app contacts GitHub only when the user opens the preset gallery
  or downloads a model. The catalog is small; models are a few megabytes.
- Storage: downloaded models live in app storage and can be removed. The
  repository itself is free to host.
- Licensing: CC0 and CC BY models are safe with attribution. Mixamo exports
  need a check before redistribution; DragonBones and MMD models frequently
  forbid redistribution, so those folders should only receive verified assets.
- APK size: unchanged, because nothing is embedded.

## Screening a glTF avatar without running the app

These checks came out of dissecting the six preinstalled presets. Each one is
decidable by reading the GLB with a few lines of Python, and each corresponds to
a defect that was actually seen on the device. A model that fails any of them is
unusable no matter how good its screenshots look.

Read the JSON chunk (12-byte header, then chunk length and type, JSON first).

| # | Check | Fails when | What you see on device |
| --- | --- | --- | --- |
| 1 | `len(scenes[0].nodes) == 1` | two or more scene roots | Mesh and skeleton are siblings, so framing and posing break. Fox shipped this way. |
| 2 | Root scale present when geometry is in centimetres | `mesh size` is in the tens or hundreds and the root has no `scale` | Model is metres off; camera fit lands inside or far outside it. |
| 3 | World size plausible: after multiplying mesh bounds by the root scale, the largest axis is between about 0.5 and 3 | anything else | Model is a speck or fills nothing. Cube had `scale 100` on a 0.02 mesh; Xbot reports 0.018 world units for a 1.8 m mannequin. |
| 4 | Every primitive has `JOINTS_0` | a skinned character without skin attributes, or morph-only geometry | The thing deforms as an object, not as a character. Morph Cube only moves 12 top-face vertices. |
| 5 | Every animation duration is at least about 0.3 s | a clip of 0.03-0.07 s | It is a static pose, not motion. Xbot's `sad_pose` and `sneak_pose` are 0.067 s; Soldier's `TPose` is 0.033 s. |
| 6 | Clip names, read literally, cover the five moods | no clip can plausibly read as angry, happy, shy, aojiao, cry | This is the decisive one, and no structural repair can fix it. |

Measured on the shipped presets, for reference:

| model | roots | world size | skin | clips | shortest clip |
| --- | --- | --- | --- | --- | --- |
| Xbot | 1 | 0.018 (needs 0.01 root) | yes | agree, headShake, idle, run, sad_pose, sneak_pose, walk | 0.067 s |
| Fox | 2 | 25 x 79 x 155 cm | yes | Survey, Walk, Run | 0.708 s |
| Soldier | 1 | 184 cm mesh, 0.01 root | yes (2 skins) | Idle, Run, TPose, Walk | 0.033 s |
| Morph Cube | 1 | 2 m via `scale 100` | no, morph only | Square | 4.2 s |

Check 6 is the one that matters most here. Structural checks 1-5 describe
whether a model will *render*; they do not make it a mood avatar. Xbot passes
1-4 and still cannot express anger, shyness or crying, because its clips are a
locomotion and gesture demo. A model for this app has to ship clips that
actually embody the five moods; there is no way to derive that from the file
structure, only from the clip list and from watching them.
