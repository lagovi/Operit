# Brief: find avatar models with the right animations

This is a search brief. Hand the block between the markers to a model that can
browse the web. It asks for actual downloadable assets, not for advice on
making them.

The previous version of this brief listed many technical constraints and found
almost nothing. That was a mistake on my side: we can repair the file ourselves.
Conversion, texture packing, root-node fixes and unit rescaling are all work we
do routinely. Asking only for files that pass every check up front filtered out
good art. This version asks for what we genuinely cannot fix.

---

## What I am looking for

I maintain an Android chat app with a live 3D avatar. The avatar has five
emotional moods, and each mood is played as an animation clip on the character.

I need characters whose animation set covers those five moods. I can convert
formats, re-export, repack textures, merge animation libraries onto a character,
and adjust scale. So the only things that really matter are:

1. **The licence allows what I do with it** (see below).
2. **The mood animations exist somewhere in a form I can bring along.**

Everything else is my problem to solve, not yours. Please do not reject a
candidate because of format, texture, scale, node structure or file size - list
it and note the issue instead.

## Moods the app exposes

The five moods are fixed by the product and cannot be renamed:

| mood key | what it must convey |
| --- | --- |
| `angry` | irritation, scolding, arms crossed or clenched, head shakes, stomping |
| `happy` | joy, nodding, clapping, bouncing, smiling posture |
| `shy` | embarrassment, looking away, hands near face, shrinking posture |
| `aojiao` | Japanese "tsundere": pouting, turning away, then softening; head tilt, huff |
| `cry` | sadness, sobbing, head down, covering face, wiping eyes |

A model does **not** have to ship clips literally named `angry`, `happy`, etc. I
map each mood to any clip myself, across a whole library if need be. What I need
is that for every one of the five moods there is at least one clip somewhere in
the candidate set whose motion credibly reads as that mood when played on a
standing character. A walk cycle is not a happy mood. A single static pose is
weak unless the pose itself carries the emotion.

Clips for idle, listening, thinking, sad, confused and surprised are a bonus,
not a requirement.

## What I can fix myself - do not filter on these

- **Format.** FBX, BVH, Blend, VRM, glTF, GLB, OBJ - any of these is fine. I can
  convert to a single self-contained `.glb`.
- **Textures.** Missing, external, tiled or unpainted textures are fine. If the
  licence is clear I download and pack them. If the textures are unusable I can
  regenerate comparable ones and publish them under my own licence.
- **Compression.** Draco, Meshopt, KTX2, WebP - I can decompress and re-export.
- **Node structure.** Two sibling roots, missing root node, centimetre units
  without a root scale - all fixable on export.
- **Skeletal vs morph.** Morph-only animation is not ideal, but say so and I
  will judge; do not discard the entry.
- **File size.** 100 MB is fine if it is the right asset; I can decimate.
- **Style.** Low-poly, stylised, cartoon, realistic - all acceptable. The avatar
  is small on screen.

## What I cannot fix, and therefore what matters

1. **Licence.** This is the blocking requirement. I run an open-source fork and
   may publish asset packs, so the licence must allow redistribution and
   derivative works - CC0, CC-BY, MIT, Apache-2.0, or an equally permissive
   licence. For each candidate state the exact licence and the URL where it is
   stated. If a source forbids redistribution (Mixamo is the classic example)
   say so explicitly and clearly; that single fact decides whether I can use the
   asset at all.
2. **The animations themselves.** Motion data that exists and is licensed is the
   hard part. If the mood clips are not in the same file as the character, that
   is fine and useful - an animation library aimed at retargeting is a great
   answer, as long as I can see the clip names and know the licence.

Please judge licences carefully for two-part answers: a character under one
licence plus an animation library under another is fine, but both halves must
allow redistribution.

## Where to look

Search broadly, and report what you actually find with links:

- **Emote, mood and gesture animation libraries** for rigged humanoids. This is
  the most valuable thing you can find: packs of social/emotional body animation
  with clip names and a permissive licence. Keywords: "emote animation pack
  glTF", "social animation pack CC0", "body gesture animation library
  retargeting", "emotion animation mocap CC0".
- **Mocap databases with permissive terms.** The CMU Graphics Lab Motion Capture
  Database is free for any use; check whether its gestures include expressive
  emotes, and how the clips are named. Look for similar academic or open mocap
  sets (e.g. open motion datasets on GitHub, BVH libraries).
- **Quaternius** (quaternius.com) - CC0 low-poly characters and the Universal
  Animation Library 1 and 2. Important: get the **actual clip names**: open the
  files or the in-page viewer and transcribe the lists, and say explicitly which
  emotes exist.
- **KayKit / Kay Lousberg** (kaylousberg.itch.io) - CC0 characters and the
  Character Animations pack, 76+ clips. Again, transcribe the real clip names.
- **Kenney** (kenney.nl) - CC0. Do not discard on rig type; report it.
- **Sketchfab**, filtered to downloadable and CC licences.
- **Ready Player Me / VRoid / VRM public avatars.** VRM is glTF-based; I can
  handle the extensions. What matters is whether the licence allows
  redistribution and whether emotion or gesture clips exist for that rig.
- **Poly Pizza, Khronos glTF Sample Models, Three.js examples.** Note that
  Mixamo-derived Three.js models carry Mixamo terms, so state that clearly.
- **OpenGameArt.org, itch.io** - free asset packs with emote animations.
- **GitHub** repositories that ship rigged characters or emote libraries with a
  clear licence (MIT, Apache, CC0).

## What to return for every candidate

For each candidate, a short structured entry:

- Name, author, direct download link (or the page that leads to one).
- Licence, with the URL of the licence statement. Be precise: if the character
  and the animations have different licences, list both.
- What it is: a character with clips, an animation library, or a bare mocap set.
- Format as shipped (state it, but this is not a reason to reject).
- **The animation clip names, copied verbatim.** If the full list is not
  published, open the archive and extract it; if that is impossible, say what
  you could verify and what you could not. Do not write "various animations".
- A per-mood verdict for angry, happy, shy, aojiao and cry: which clip covers
  it, or "no". Be honest when a mood has no match - that is the single most
  useful thing in the report.
- Anything about the licence that would stop me from shipping it.
- Technical notes are welcome but secondary: format, root nodes, texture
  situation, scale, size. State them as observations, not as disqualifiers.

Rank the results by how much of the five-mood set they cover with a permissive
licence. A CC0 animation library with real emote clips outranks a nicely
packaged character that only has locomotion. Put anything with unclear or
restrictive licence at the end, clearly labelled.

Do not fabricate clip lists or licences. If a page does not state the licence or
the animation names, write "not stated" rather than guessing.

---

## Why the technical requirements disappeared

Kept here so the reasoning is not lost on the next reader.

The earlier brief demanded one scene root, embedded PNG textures, no Draco or
KTX2, human scale at rest and under 25 MB. These came from defects we hit while
shipping: the camera framed models poorly when a file had two roots, and
centimetre units without a root scale made the avatar tiny. But those are
properties of the *file*, and we control the file: we re-export it.

The two things we do not control are the licence and whether the motion was ever
captured. A CC0 animated character with an awkward export is worth more to us
than a clean file we are not allowed to ship. Hence the new split.
