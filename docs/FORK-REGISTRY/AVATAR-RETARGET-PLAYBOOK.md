# Avatar retargeting and multi-clip build — practical playbook

Everything learned while proving K7 (2026-11-12): how to take a character and a
set of motion clips whose rigs do not match, and produce one plain `.glb` that
the Filament-based Android viewer plays as five named mood animations. This is
the "how", not the "which model" (that is the acquisition plan).

Companion files: `AVATAR-MODEL-ACQUISITION-PLAN.md` (which sources to use),
`TODO-AUDIT.md` K7 (the decision record), the working script
`tools/colab/build_avatar_moods.py`, and the proof in
`screenshots/avatar-moods-2026-11-12/`.

## The problem in one paragraph

The character and the clips both use `mixamorig:*` bone names and the same
hierarchy, so it is tempting to copy each bone's local rotation by name and be
done. That does not work: the two rigs have very different rest (bind) poses.
Measured on the 100Avatars character against the Mixamo clips, the same-named
bones differed at rest by up to 180 degrees (`mixamorig:LeftUpLeg`) and 132
degrees (`mixamorig:LeftShoulder`). Copying the local rotation verbatim rotates
each bone in the wrong frame, and the body comes out twisted. A render of that
naive attempt showed a character with its head thrown back and spine bent; it
was discarded.

## What actually works: the VRM Add-on T-pose route (Route A)

The VRM Add-on for Blender normalises both skeletons through a canonical T-pose
before it computes the motion transfer, so the original bind-pose differences
cancel out. That is the whole trick. The route, exactly as implemented in
`tools/colab/build_avatar_moods.py`:

1. Install the add-on as an extension and enable it with the *namespaced*
   module name: `addon_utils.enable("bl_ext.user_default.vrm", ...)`. The bare
   name `io_scene_vrm` does not resolve in Blender 4.2 when the add-on was
   installed as an extension; that mismatch cost a debugging round.
2. Import the character with `bpy.ops.import_scene.vrm(filepath=...)`, not the
   plain glTF importer. The VRM importer is what creates the model record the
   next steps need.
3. Switch the model to VRM 1.0. A VRM 0.x character is imported fine, but the
   VRMA importer refuses it with "Please set the version of VRM to 1.0". The
   switch is a property write, not an operator:
   `armature.data.vrm_addon_extension.spec_version = "1.0"`. Note the data is on
   the armature's **data**, not on the object and not on the scene; the object's
   `vrm_addon_extension` only holds `axis_translation`, which is why probing
   `hasattr(obj, "vrm")` misleads.
4. Map the bones to the humanoid slots:
   `bpy.ops.vrm.assign_vrm1_humanoid_human_bones_automatically(
   armature_object_name=armature.name)`. On the Mixamo-named rig this mapped 55
   slots. Guard the export against a bad mapping (the script refuses below 20).
5. For each clip, `bpy.ops.import_scene.vrma(filepath=...,
   armature_object_name=armature.name)`. This retargets the clip onto the rig
   and leaves a `Humanoid` action on the armature. It requires the clip to carry
   the `VRMC_vrm_animation` extension (both the tk256ailab set and the Mixamo
   conversions do).
6. Rename that action to the mood key, give it `use_fake_user = True`, push it
   onto its own NLA track, then clear the active action.
7. Export with `export_animation_mode="ACTIONS"`.

## glTF export: getting exactly N clips and no extras

The exporter in `ACTIONS` mode writes every action that is bound to the
armature, whether as the active action or on an NLA track. The VRM import leaves
a `T-Pose` action bound to the rig, and that is exactly why the first build came
out with six animations instead of five. Setting `use_fake_user = False` is not
enough because the action is still bound; you must unbind it
(`animation_data.action = None`) and **delete** it (`bpy.data.actions.remove`).

Verified options for the plain-GLB, Filament-consumer case:

- `export_animation_mode="ACTIONS"` — one animation per stashed action.
- `export_force_sampling=True` — bake every frame; required for anything driven
  by constraints or that is not plain keyframes.
- `export_anim_slide_to_zero=True` — each clip starts at t=0, which the viewer
  wants; otherwise clips share a timeline offset.
- `export_yup=True` — glTF Y-up; the default, keep it.

The clip name in the exported file comes from the action name (here also the
NLA track name, set to the same mood key). Verify the result by parsing the GLB
JSON chunk and asserting the animation names are exactly the five mood keys;
that check caught the stray T-Pose.

## Headless Blender on the Colab VM

- Blender is not installed; fetch the official tarball (4.2.3 LTS was used):
  `wget .../Blender4.2/blender-4.2.3-linux-x64.tar.xz && tar xf`. The desktop
  libs (`libx11-6 libxi6 libgl1 libxkbcommon0 ...`) are needed even for `-b`.
- The VRM Add-on is installed as an *extension*: download the release zip, then
  `bpy.ops.extensions.package_install_files(filepath=zip, repo="user_default")`
  inside Blender. It then appears in `bpy.ops.import_scene` as `vrm` and `vrma`.
- A CPU session is enough; Blender here is pure geometry math and Workbench
  rendering, no GPU.
- Run every script with `blender -b --python script.py -- <args>`; the script
  reads its arguments after `--`.
- The Colab CLI runs Python through the kernel: `colab exec -s <name> -f
  script.py`. Wrap shell work in `subprocess.run`, because `colab exec` takes
  Python, not a shell string.
- Close the session (`colab stop -s <name>`) the moment the work is done; the
  quota rule in `AGENTS.md` applies.

## Visual QA: confirming a retarget by eye

Never trust a retarget until it is rendered. The cheap headless recipe that was
used:

- Import the built GLB, pick the mesh with the most vertices (a stray helper
  mesh may be present), hide the others from render.
- Compute world-space bounds after `view_layer.update()`, frame an **orthographic**
  camera with `camera.data.type = "ORTHO"` and `ortho_scale = maxdim * margin`.
  (The skill packs document perspective auto-framing in code and only describe
  ortho in prose; the ortho framing had to be written directly.)
- `BLENDER_WORKBENCH` engine, `film_transparent = True`, render stills at a few
  points along each clip (start, 25/50/75 %, end) and tile them into a
  contact sheet. This is what exposed both the twisted naive retarget and, later,
  confirmed the clean Route A poses.
- Also verify on the phone in the real viewer: select the model, confirm it
  renders and frames itself, confirm it holds its rest pose on load (two frames
  differ by 0 pixels), confirm the Mapped Action dropdown lists every clip, then
  hit Preview and confirm motion (frame-to-frame pixel differences in the
  thousands, and the pose visibly leaves the rest pose).

## Things that did not work, so nobody repeats them

- Copying local bone rotations by name across mismatched rest poses: twisted
  body (see above).
- A hand-written matrix retarget against the glTF JSON without T-pose
  normalisation: also bent poses; the formula family is right in principle
  (`delta_world = pose_src · rest_src⁻¹; pose_tgt = delta_world · rest_tgt`)
  but applying it without a common reference pose and without the object's
  `matrix_world` (glTF import carries a Z-up→Y-up rotation) does not reproduce
  the add-on's result. Using the add-on is strictly less error-prone.
- Importing the VRMA with the plain glTF importer: it splits the clip into one
  action per bone and creates no armature, which is useless for retargeting.
- Importing a VRM whose `licenseName` is "Other" with no licence URL: the add-on
  refuses with `LicenseConfirmationRequired`. This is a feature, not a bug — it
  keeps unclear-licence models out.
- Relying on `hasattr(obj, "vrm")`: the real data lives at
  `obj.data.vrm_addon_extension` for an armature.

## Note on third-party "Blender skill" packs

The skill packs that circulate for agents (EL4CTEO/blender-skills,
arjun988/blender-skills, DirectorGunner/AI-SKILL-blender and similar) were
surveyed. Only EL4CTEO actually contains retargeting code; the rest are generic
MCP drivers, manual mirrors, or explicitly disclaim retargeting. Their useful,
version-independent takeaways are small and are already folded into this
document: the glTF `ACTIONS` export semantics and the "bound actions leak into
the export" trap, `use_fake_user` on clip actions, NLA push-down/stash, and the
Workbench ortho-for-QA idea. Most of the packs target Blender 5.x, which uses a
slotted-action API that does not exist in 4.2, so their code is not directly
reusable here.
