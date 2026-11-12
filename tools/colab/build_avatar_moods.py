"""Build a single multi-animation avatar GLB from a VRM character and VRMA clips.

This is the verified "Route A" pipeline (see
docs/FORK-REGISTRY/AVATAR-MODEL-ACQUISITION-PLAN.md and TODO-AUDIT.md K7):

  1. import the character through the VRM Add-on importer, so the armature gets
     a VRM0/VRM1 model record;
  2. migrate the model to VRM 1.0 (the VRMA importer only accepts 1.0);
  3. map the rig's bones to the VRM 1.0 humanoid slots;
  4. for each VRMA clip, let `import_scene.vrma` retarget it onto the rig (the
     importer normalises the rest pose through a T-pose, which is what makes it
     work across rigs whose bind poses differ), rename the produced "Humanoid"
     action to the mood key, and stash it on its own NLA track;
  5. export every stashed action as a separate glTF animation.

Run headless on the Colab VM:

    blender -b --python build_avatar_moods.py -- \
        --character /content/work/ava1.vrm \
        --clip angry=/content/work/mixamo-benchmark/0_Angry.vrma \
        --clip cry=/content/work/mixamo-benchmark/22_Crying.vrma \
        --out /content/work/avatar_moods.glb

The character must be a VRM whose licence the VRM Add-on accepts on import (a
missing licence URL blocks the import). VRMA clips must carry the
`VRMC_vrm_animation` extension; plain glTF animation files are not retargeted by
this path.
"""

import argparse
import os
import sys

import addon_utils
import bpy


def parse_args():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    p = argparse.ArgumentParser()
    p.add_argument("--character", required=True)
    p.add_argument("--clip", action="append", default=[],
                   help="mood=/path/to/clip.vrma, repeat once per mood")
    p.add_argument("--out", required=True)
    return p.parse_args(argv)


def enable_vrm_addon():
    addon_utils.enable("bl_ext.user_default.vrm", default_set=True, persistent=True)


def humanoid_slot_count(armature):
    hb = armature.data.vrm_addon_extension.vrm1.humanoid.human_bones
    count = 0
    for prop in hb.bl_rna.properties:
        if prop.type != "POINTER":
            continue
        try:
            slot = getattr(hb, prop.identifier)
            if getattr(slot, "node", None):
                count += 1
        except AttributeError:
            continue
    return count


def main():
    args = parse_args()
    clips = []
    for spec in args.clip:
        name, _, path = spec.partition("=")
        if not name or not path:
            raise SystemExit(f"bad --clip {spec!r}, expected name=/path")
        clips.append((name, path))
    if not clips:
        raise SystemExit("no --clip given")

    bpy.ops.wm.read_factory_settings(use_empty=True)
    enable_vrm_addon()

    bpy.ops.import_scene.vrm(filepath=args.character)
    armature = next(o for o in bpy.data.objects if o.type == "ARMATURE")

    ext = armature.data.vrm_addon_extension
    ext.spec_version = "1.0"
    bpy.ops.vrm.assign_vrm1_humanoid_human_bones_automatically(
        armature_object_name=armature.name)
    mapped = humanoid_slot_count(armature)
    if mapped < 20:
        raise SystemExit(f"humanoid mapping too small ({mapped}); refusing to export")
    print(f"humanoid bones mapped: {mapped}")

    armature.animation_data_create()
    # The VRM import leaves a T-Pose action bound to the rig; the glTF exporter
    # in ACTIONS mode exports every action bound to the armature, so the T-Pose
    # would appear as an extra clip. Unbind and delete every pre-existing action.
    armature.animation_data.action = None
    for act in list(bpy.data.actions):
        act.use_fake_user = False
        bpy.data.actions.remove(act)

    created = []
    for name, path in clips:
        result = bpy.ops.import_scene.vrma(
            filepath=path, armature_object_name=armature.name)
        if result != {"FINISHED"}:
            raise SystemExit(f"VRMA import failed for {name}: {result}")
        act = armature.animation_data.action
        if act is None:
            raise SystemExit(f"VRMA import produced no action for {name}")
        act.name = name
        act.use_fake_user = True
        track = armature.animation_data.nla_tracks.new()
        track.name = name
        track.strips.new(name, int(act.frame_range[0]), act)
        armature.animation_data.action = None
        created.append(name)

    bpy.ops.export_scene.gltf(
        filepath=args.out,
        export_format="GLB",
        use_selection=False,
        export_animations=True,
        export_animation_mode="ACTIONS",
        export_force_sampling=True,
        export_anim_slide_to_zero=True,
        export_yup=True,
    )
    print("created actions:", created)
    print("wrote", args.out, os.path.getsize(args.out), "bytes")


if __name__ == "__main__":
    main()
