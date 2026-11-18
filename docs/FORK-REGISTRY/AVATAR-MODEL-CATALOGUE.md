# Avatar model catalogue

Status: living document, last updated 2026-11-17. This is the single place that
answers: which avatar models the fork has, what each one can do, under which
licence, and who verified it.

Related: `AVATAR-MODEL-ACQUISITION-PLAN.md` (what to buy or build next),
`AVATAR-RETARGET-PLAYBOOK.md` (how a character and clips were merged),
`TODO-AUDIT.md` K4/K5 (the product rule that no cloud model is bundled).

## Phone re-check after the finger-curl fix (2026-11-18)

Rose, Kate and Jennifer were downloaded on the test phone from the published
assets (with the 2.6x finger curl in `angry`, `aojiao`, `cry`, `shy`). `angry`
was replayed for each: the hands grip the waist instead of showing a flat
plate, and the framed width stays within a few pixels of the rest pose (Rose
270, Kate 272-283, Jennifer 267-278). The other four moods on these three
models are still pending; the matrix above keeps the earlier PASS marks from
the offline renders, not from the phone.

## Where the list comes from

The in-app gallery is driven entirely by the asset repository
`https://github.com/lagovi/OperitAvatarAssets`, file `presets.json`
(`operit.avatar.presets.v1`, version 3). Nine presets exist, all `type: gltf`;
the app only downloads entries with a model path. The catalogue carries `id`,
`type`, `name`, `summary`, `model`, `license`, `licenseFile`, `sizeBytes`,
`sha256`, `animations`, `defaultAnimation` and `thumbnail`.

Two consequences that matter for verification:

- `presets.json` has no field that binds a mood to a clip. For `rose`, `kate`
  and `jennifer` the clip names are literally `angry`, `happy`, `shy`,
  `aojiao`, `cry`, so the default name-based mapping works without extra data.
  Every other preset has to be mapped by hand in the app.
- The catalogue is public and ships to users. Anything not licensed for
  redistribution cannot be added to it, only kept locally for testing.

## How a model is verified

Two independent checks, both recorded per model and per mood:

1. **Agent check, offline.** `tools/avatar/mood_collage.py` parses the `.glb`
   itself (nodes, skin, inverse bind matrices, samplers), samples every
   animation at 18 evenly spaced times, skins the mesh on the CPU and renders
   each sample with an orthographic rasterizer. Nine samples per page are packed
   into a 3x3 contact sheet with the timestamp burned into every cell, two pages
   per clip. The pages are then read by a vision model to judge whether the
   motion actually matches the mood. This does not replace the app: it catches
   empty, static, collapsed or mis-retargeted clips before a build.
2. **Human check, on the phone.** The acceptance gate. Push the model into
   `Android/data/com.ai.assistance.operit.debug/files/avatars/<Name>/model.glb`,
   open menu -> Assistant Config -> Current Model, expand Mood Trigger Mapping,
   set Mapped Action to the clip and press Preview. The procedure is described
   in `internal-assets/mixamo-benchmark/README.md`.

## Catalogue

| id | name | file size | licence | clips (length) | moods |
| --- | --- | --- | --- | --- | --- |
| `gltf.rose` | Rose | 2 518 352 B | CC0 1.0 character (100Avatars, Polygonal Mind) / MIT motion (tk256ailab VRMA set) | angry, happy, shy, aojiao, cry (3.958 s each) | all five |
| `gltf.kate` | Kate | 1 660 540 B | same as Rose | same five (3.958 s each) | all five |
| `gltf.jennifer` | Jennifer | 2 151 576 B | same as Rose | same five (3.958 s each) | all five |
| `gltf.xbot` | Xbot | 2 930 032 B | three.js examples, Mixamo export (terms need review) | agree 1.833 s, headShake 2.567 s, idle 2.500 s, run 0.700 s, sad_pose 0.067 s, sneak_pose 0.067 s, walk 0.967 s | none |
| `gltf.soldier` | Soldier | 2 160 468 B | three.js examples, Mixamo export (terms need review) | Idle 1.967 s, Run 0.700 s, TPose 0.033 s, Walk 1.033 s | none |
| `gltf.fox` | Fox | 162 920 B | CC0 1.0 model / CC BY 4.0 rig and conversion | Survey 3.417 s, Walk 0.708 s, Run 1.158 s | none |
| `gltf.cesiumman` | Cesium Man | 438 044 B | CC BY 4.0 | anim 2.000 s (walk) | none |
| `gltf.riggedfigure` | Rigged Figure | 50 116 B | CC BY 4.0 | anim 1.250 s (walk) | none |
| `gltf.cube` | Morph Cube | 6 752 B | CC0 1.0 | Square 4.200 s (morph targets) | none |

Geometry read from the files: Rose 4 123 vertices / 5 622 triangles, Kate
2 778 / 3 960, Jennifer 3 944 / 5 372, Xbot 7 325 / 11 186 plus a 109 / 190
sub-mesh, Soldier 12 473 / 20 840 plus 15 901 / 28 272, Fox 1 728 / 576, Cesium
Man 3 273 / 4 672, Rigged Figure 370 / 256, Cube 24 / 12. Rose, Kate and
Jennifer each have 68 nodes, one skin with 65 joints, one material and one
embedded PNG texture.

## Mood verification matrix

`agent` = offline contact sheets reviewed by a vision model on 2026-11-17.
`device` = the human check above. A cell is only "done" when both are filled.

| model | angry | happy | shy | aojiao | cry |
| --- | --- | --- | --- | --- | --- |
| Rose | agent OK, pages 1-2 | agent OK, pages 1-2 | agent OK, pages 1-2 | agent OK, pages 1-2 | agent OK, pages 1-2 |
| Kate | agent OK, page 1 | agent OK, page 1 | agent OK, page 1 | agent OK, page 1 | agent OK, page 1 |
| Jennifer | agent OK, page 1 | agent OK, page 1 | agent OK, page 1 | agent OK, page 1 | agent OK, page 1 |

Device check: pending for all fifteen pairs.

What the agent check saw:

- **angry** - hands on hips, weight shifting, foot tap, head turns; reads as
  annoyed/assertive for all three characters.
- **happy** - hands clasped or gesturing at the chest, light sway, one leg
  crossed behind; reads as cheerful.
- **shy** - one hand covering the cheek, body turned away, one leg drawn back;
  reads as embarrassed.
- **aojiao** - both arms raised, head turned away, then a dismissive "hmph"
  outward gesture; reads as a tsundere tantrum.
- **cry** - one hand at the face wiping, the other arm stretched out, body
  rocking; reads as crying.

Rose was reviewed on both pages of every clip (18 samples, the whole 3.958 s).
Kate and Jennifer use the same retargeted motion set and were reviewed on the
first page of every clip; their difference is the character mesh and rig fit,
which the renders confirm (no collapsed joints, no detached limbs, no texture
mix-up). Extended checks of pages 2 for Kate and Jennifer are cheap to repeat
with the command below.

## Per-model notes

- **Rose** is the reference mood avatar: textured anime humanoid, clean rig,
  all five clips present, no default animation (so nothing plays on load,
  matching rule K6).
- **Kate** has a simpler mesh (flat torso, blocky boots) but the rig deforms
  correctly; the face is static geometry, so emotion is carried by the body.
- **Jennifer** has deliberately oversized head and hair. It works, but in
  `aojiao` the head turn plus the hair mass hides the face for most of the clip;
  the motion is still legible. This is the weakest of the three visually.
- **Xbot** is a mannequin. `sad_pose` and `sneak_pose` are 0.067 s stills, not
  animations (confirmed in the contact sheet), so they cannot express a mood.
  The rest are locomotion and agree/headshake gestures.
- **Soldier** is a textured humanoid with locomotion only; `TPose` is a 0.033 s
  still.
- **Fox** is a quadruped with a head-look `Survey`, `Walk` and `Run`; it faces
  +X, so renders need `--yaw` to see the profile head-on.
- **Cesium Man** and **Rigged Figure** are single-walk sample assets, useful as
  rig references, not as avatars.
- **Morph Cube** has no skeleton; its only animation drives morph target
  weights, which `mood_collage.py` does not apply, so the render is a static
  square by design. It exists as a renderer smoke test and must be checked in
  the app.

## Assets that are not in the catalogue

- `internal-assets/mixamo-benchmark/avatar_moods_mixamo.glb` (1 236 072 B,
  owned by the maintainer, outside the repository): the 100Avatars CC0 "Devil"
  with five Mixamo clips named by mood. Clip lengths: angry 2.000 s, happy
  1.875 s, shy 3.000 s, aojiao 1.625 s, cry 6.250 s. Mixamo terms forbid
  redistribution, so this is a local comparison build only; it is the fastest
  way to put the five moods on a phone without a rebuild. Agent check on
  2026-11-17: `happy` (joyful jump, feet leave the ground) and `aojiao` (head
  shake, shrug) are legible; `cry` is a long crouched sob; `angry`
  (`94_Angry Gesture`) renders as a nearly static stance with clenched fists;
  `shy` (`127_Leaning`) is an arms-behind-the-head stretch and does not read as
  embarrassment. So this build is usable for happy/aojiao/cry smoke tests, not
  as a reference for angry or shy. The Devil mesh itself is far from the
  product style.
- `internal-assets/mixamo-benchmark/clips/`: 18 converted VRMA clips, same
  restriction.
- `screenshots/` holds earlier test builds (`avatar_moods.glb`,
  `fox_fixed.glb`), also outside git.

## Licences and shippability

- Rose, Kate, Jennifer: CC0 character plus MIT motion. Redistributable, already
  published in the public asset repository.
- Fox: CC0 model, CC BY 4.0 rig/conversion. Needs attribution.
- Cesium Man, Rigged Figure: CC BY 4.0. Needs attribution.
- Morph Cube: CC0.
- Xbot, Soldier: three.js example bundle, Mixamo export. The three.js repo is
  MIT, but the characters are third-party demo assets; their README explicitly
  asks to review Mixamo terms before redistributing. Treat as demo material.
- Mixamo benchmark: not redistributable at all, local only.

## Reproducing the agent check

    python3 tools/avatar/mood_collage.py inspect <model.glb> [...]
    python3 tools/avatar/mood_collage.py collage <model.glb> --out /tmp/moods \
        [--clip angry] [--frames 18] [--cell 340] [--yaw 1.57]

`--frames 18` produces two 3x3 pages per clip (the mode used above); `--frames
9` produces one page and is enough for locomotion clips. The tool needs only
Python 3 with numpy and Pillow; it does not use Blender, a GPU or the phone.
Its limitations: morph targets are not applied, CUBICSPLINE samplers are
approximated by their middle key, and shading is flat with a texture-average
face color, so it is a motion check, not a look check.

## Open items

- Device verification of all fifteen mood pairs is still pending and is the
  acceptance gate for any change to the preset catalogue.
- `presets.json` lacks a `moods` field. If more mood-capable models appear,
  either keep the mood-key naming convention or add the mapping to the schema
  and the app together.
- Xbot and Soldier licences need a decision before any wider redistribution.
