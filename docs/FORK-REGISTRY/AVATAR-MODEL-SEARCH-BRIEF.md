# Brief: find ready-made avatar models that fit this app

This is a search brief. Hand the block between the markers to a model that can
browse the web and read model repositories. It asks for ready-made assets, not
for advice on making them.

---

## What I am looking for

I maintain an Android chat app with a live 3D avatar of the assistant. I need
ready-to-use avatar models whose animation set covers a fixed list of emotional
moods. The engine and its constraints are described below; the task is to find
actual downloadable models and return direct links with evidence that they meet
those constraints.

## Moods the app exposes

The assistant shows one animation per mood. The five moods are fixed by the
product and cannot be renamed:

| mood key | what it must convey |
| --- | --- |
| `angry` | irritation, scolding, arms crossed or clenched, head shakes, stomping |
| `happy` | joy, nodding, clapping, bouncing, smiling posture |
| `shy` | embarrassment, looking away, hands near face, shrinking posture |
| `aojiao` | Japanese "tsundere": pouting, turning away, then softening; head tilt, huff |
| `cry` | sadness, sobbing, head down, covering face, wiping eyes |

The app also has a generic emotion set used elsewhere: idle, listening,
thinking, happy, sad, confused, surprised. Clips covering those are a bonus, not
a requirement.

A model does **not** have to ship clips literally named `angry`, `happy`, etc. I
map each mood to any clip myself. What matters is that the shipped clip set
contains, for every one of the five moods, at least one clip whose motion
credibly reads as that mood when played on a standing character. A walk cycle
is not a happy mood. A static two-frame pose is weak unless the pose itself
carries the emotion.

## Hard technical constraints

The app renders glTF with Google Filament. A model is accepted only if all of
these hold:

1. **Format:** `.glb` or self-contained `.gltf`. Textures must be embedded
   (PNG or JPEG). No Draco compression, no KTX2, no WebP textures, no Meshopt.
2. **One scene root.** Exactly one top-level node in `scenes[].nodes` that
   contains both the mesh and the skeleton. Two sibling roots (a common export
   bug) break the camera framing.
3. **Skeletal animation.** The animation must be driven by a skinned rig with
   real joint animation. Morph-target-only animation (a "morph cube" style
   asset) is not acceptable: it reads as a deforming object, not a character.
4. **Sane scale.** The model should be roughly human-sized at rest, about
   1.6-1.9 m tall for a standing character, or clearly consistent units with a
   root scale. Centimetre geometry without a `0.01` root scale is a defect.
5. **Real motion, not stills.** Mood clips should be 1-3 seconds of actual
   movement. Two-frame "poses" of about 0.07 s are effectively static and I will
   consider them a partial match at best.
6. **In place for mood clips.** Locomotion clips translating the character
   across the floor are fine for walk/run, but a mood reaction should stay in
   place; if the clip has root motion, say so.
7. **Licence must allow redistribution.** I run an open-source fork and
   optionally publish asset packs. I need a model whose licence permits
   redistribution (CC0, CC-BY, MIT, Apache-2.0, or a clear permissive licence).
   State the exact licence and where it is stated. Proprietary or
   "free to download but not redistributable" assets are useless even if they
   look right.
8. **Reasonable size.** Under about 25 MB per model after download. Smaller is
   better; this ships inside an app.

## Where to look

Please search broadly and report what you actually find, with links:

- Quaternius (quaternius.com) - they publish CC0 low-poly characters with
  animation sets, check their "Ultimate Animated Character Pack" and similar.
- Kenney (kenney.nl) - CC0 game assets, check for animated characters
  (Kenney's animated characters are often morph-based, so verify the rig).
- Mixamo (mixamo.com) - large library of animation clips, but the licence
  restricts redistribution of the animations themselves; report clearly whether
  a Mixamo-derived model can legally be redistributed, because this determines
  usability.
- Sketchfab, filtered to downloadable and to CC licences.
- Ready Player Me / VRM public avatars; note that VRM is glTF-based but with
  extensions the app may not support, so say so if the model relies on them.
- Poly Pizza (poly.pizza), Khronos glTF Sample Models,
  Three.js examples assets.
- OpenGameArt.org, itch.io free asset packs with mood/emote animations.
- GitHub repositories that ship rigged glTF characters with emotional clips.

Also search specifically for assets that already ship **emote or mood animation
packs** for a rigged humanoid, since that is the rare and valuable case:
keywords around "emote animations glTF", "mood animations rigged character CC0",
"facial expression and body gesture animation pack glb", "VRM emotion clip".

## What to return for every candidate

For each model, a short structured entry:

- Name, author, direct download link.
- Licence, with the URL of the licence statement.
- Format, embedded textures yes/no, extensions used.
- Approximate file size and triangle count if visible.
- Rig type (bones, if stated) and whether it is humanoid/Mixamo-compatible.
- **The full list of animation clip names**, copied verbatim.
- A per-mood verdict for angry, happy, shy, aojiao and cry: which clip covers
  it, or "no".
- Any constraint above that it violates.

Rank the results: first the models that cover all five moods and satisfy every
constraint, then those that cover most moods with a permissible licence, then
everything else. Be explicit about which candidates you could not verify.

Do not fabricate clip lists or licences. If a page does not state the licence or
the animation names, write "not stated" rather than guessing.

---
