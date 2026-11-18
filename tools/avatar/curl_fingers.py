#!/usr/bin/env python3
"""Strengthen or weaken the finger curl of a GLB animation offline.

The MIT mood clips shipped with the mood avatars bend the fingers only about
thirty degrees, which is too little for the long-armed Rose, Kate and Jennifer
rigs: with the wrist bent sixty degrees the hand reads as a flat plate. The
curves themselves are correct, so this tool scales the curl angle of every
finger rotation key instead of inventing a new pose.

The rotation is scaled relative to the bone's rest rotation, so the thumb (whose
rest rotation is already fifty degrees) keeps its rest orientation and only its
curl about that rest is amplified.

Usage:
    python3 curl_fingers.py model.glb out.glb --factor 2.6 [--clips angry,shy]
"""

import argparse
import json
import math
import struct
import sys

import numpy as np

FINGER_PARTS = ("Thumb", "Index", "Middle", "Ring", "Pinky")


def is_finger(name):
    if not name or "Hand" not in name:
        return False
    return any(name.endswith(p + str(k)) for p in FINGER_PARTS for k in (1, 2, 3, 4))


def quat_mul(a, b):
    ax, ay, az, aw = a
    bx, by, bz, bw = b
    return np.array([
        aw * bx + ax * bw + ay * bz - az * by,
        aw * by - ax * bz + ay * bw + az * bx,
        aw * bz + ax * by - ay * bx + az * bw,
        aw * bw - ax * bx - ay * by - az * bz,
    ])


def quat_conj(q):
    return np.array([-q[0], -q[1], -q[2], q[3]])


def quat_axis_angle(q):
    n = np.linalg.norm(q)
    if n == 0:
        return np.array([0.0, 0.0, 1.0]), 0.0
    x, y, z, w = q / n
    w = max(-1.0, min(1.0, w))
    angle = 2.0 * math.acos(w)
    s = math.sqrt(max(0.0, 1.0 - w * w))
    if s < 1e-8:
        return np.array([0.0, 0.0, 1.0]), 0.0
    return np.array([x, y, z]) / s, angle


def axis_angle_quat(axis, angle):
    s = math.sin(angle / 2.0)
    return np.array([axis[0] * s, axis[1] * s, axis[2] * s, math.cos(angle / 2.0)])


def scale_quat(q, rest, factor):
    delta = quat_mul(quat_conj(rest), q)
    axis, angle = quat_axis_angle(delta)
    scaled = axis_angle_quat(axis, angle * factor)
    out = quat_mul(rest, scaled)
    return out / (np.linalg.norm(out) or 1.0)


def read_glb(path):
    raw = open(path, "rb").read()
    if raw[:4] != b"glTF":
        raise SystemExit(f"{path}: not a glb")
    off = 12
    js = None
    bin_chunk = b""
    while off < len(raw):
        ln, ty = struct.unpack_from("<II", raw, off)
        off += 8
        chunk = raw[off:off + ln]
        off += ln
        if ty == 0x4E4F534A:
            js = json.loads(chunk)
        elif ty == 0x004E4942:
            bin_chunk = bytearray(chunk)
    if js is None:
        raise SystemExit(f"{path}: no json chunk")
    return js, bin_chunk


def write_glb(path, js, bin_chunk):
    jb = json.dumps(js, separators=(",", ":")).encode()
    jb += b" " * ((4 - len(jb) % 4) % 4)
    bb = bytes(bin_chunk)
    bb += b"\0" * ((4 - len(bb) % 4) % 4)
    total = 12 + 8 + len(jb) + 8 + len(bb)
    out = bytearray()
    out += b"glTF" + struct.pack("<II", 2, total)
    out += struct.pack("<II", len(jb), 0x4E4F534A) + jb
    out += struct.pack("<II", len(bb), 0x004E4942) + bb
    open(path, "wb").write(out)


def accessor_slice(js, idx):
    a = js["accessors"][idx]
    if a["componentType"] != 5126 or a["type"] != "VEC4":
        return None
    bv = js["bufferViews"][a["bufferView"]]
    base = bv.get("byteOffset", 0) + a.get("byteOffset", 0)
    stride = bv.get("byteStride") or 16
    return base, a["count"], stride


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("src")
    ap.add_argument("dst")
    ap.add_argument("--factor", type=float, default=2.6)
    ap.add_argument("--clips", default="")
    args = ap.parse_args()

    js, bin_chunk = read_glb(args.src)
    nodes = js.get("nodes", [])
    rest = {}
    for i, n in enumerate(nodes):
        if "rotation" in n:
            rest[i] = np.array(n["rotation"], dtype=np.float64)
    only = {c.strip() for c in args.clips.split(",") if c.strip()}

    touched = 0
    keys = 0
    for anim in js.get("animations", []):
        if only and anim.get("name") not in only:
            continue
        for ch in anim["channels"]:
            if ch["target"]["path"] != "rotation":
                continue
            node = ch["target"]["node"]
            name = nodes[node].get("name", "")
            if not is_finger(name):
                continue
            r = rest.get(node, np.array([0.0, 0.0, 0.0, 1.0]))
            sl = accessor_slice(js, anim["samplers"][ch["sampler"]]["output"])
            if sl is None:
                continue
            base, count, stride = sl
            touched += 1
            for i in range(count):
                off = base + i * stride
                q = np.frombuffer(bytes(bin_chunk[off:off + 16]), dtype="<f4").astype(np.float64)
                q = scale_quat(q, r, args.factor)
                bin_chunk[off:off + 16] = np.asarray(q, dtype="<f4").tobytes()
                keys += 1
    write_glb(args.dst, js, bin_chunk)
    print(f"{args.src} -> {args.dst}: {touched} finger curves, {keys} keys, factor {args.factor}")


if __name__ == "__main__":
    sys.exit(main())
