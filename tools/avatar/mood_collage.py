#!/usr/bin/env python3
"""Offline glTF/GLB mood-animation inspector and contact-sheet renderer.

Purpose: verify that a glb actually animates the way a preset claims, without
Blender, a GPU, or the phone. It parses the glb itself, samples every animation
at N evenly spaced times, skins the mesh on the CPU, renders each sample with a
small orthographic rasterizer and packs nine samples per page into a 3x3
contact sheet with the timestamp drawn on every cell.

This is the agent-side check described in docs/FORK-REGISTRY/AVATAR-MODEL-CATALOGUE.md.
The final acceptance still happens in the app on the phone.

Usage:
  python3 tools/avatar/mood_collage.py inspect <model.glb> [...]
  python3 tools/avatar/mood_collage.py collage <model.glb> --out DIR
      [--clip NAME ...] [--frames 18] [--cell 340]
"""

import argparse
import json
import math
import os
import struct
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

DEFAULT_FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
COMP = {5120: ("b", 1), 5121: ("B", 1), 5122: ("h", 2), 5123: ("H", 2), 5125: ("I", 4), 5126: ("f", 4)}
NCOMP = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT4": 16}


class Glb:
    def __init__(self, path):
        raw = open(path, "rb").read()
        if raw[:4] != b"glTF":
            raise SystemExit(f"{path}: not a glb")
        off = 12
        self.json = None
        self.bin = b""
        while off < len(raw):
            ln, ty = struct.unpack_from("<II", raw, off)
            off += 8
            chunk = raw[off:off + ln]
            off += ln
            if ty == 0x4E4F534A:
                self.json = json.loads(chunk)
            elif ty == 0x004E4942:
                self.bin = chunk
        if self.json is None:
            raise SystemExit(f"{path}: no json chunk")
        self.path = path

    def accessor(self, idx):
        a = self.json["accessors"][idx]
        fmt, size = COMP[a["componentType"]]
        n = NCOMP[a["type"]]
        count = a["count"]
        bv = self.json["bufferViews"][a["bufferView"]]
        base = bv.get("byteOffset", 0) + a.get("byteOffset", 0)
        stride = bv.get("byteStride") or n * size
        if stride == n * size:
            arr = np.frombuffer(self.bin, dtype=fmt, count=count * n, offset=base).reshape(count, n)
        else:
            arr = np.empty((count, n), dtype=fmt)
            for i in range(count):
                arr[i] = np.frombuffer(self.bin, dtype=fmt, count=n, offset=base + i * stride)
        out = arr.astype(np.float64)
        if a.get("normalized"):
            ct = a["componentType"]
            if ct == 5121:
                out /= 255.0
            elif ct == 5123:
                out /= 65535.0
            elif ct == 5120:
                out = np.maximum(out / 127.0, -1.0)
            elif ct == 5122:
                out = np.maximum(out / 32767.0, -1.0)
        return out

    def image(self, idx):
        img = self.json["images"][idx]
        if "bufferView" in img:
            bv = self.json["bufferViews"][img["bufferView"]]
            start = bv.get("byteOffset", 0)
            data = self.bin[start:start + bv["byteLength"]]
            import io
            return Image.open(io.BytesIO(data)).convert("RGB")
        return None


def quat_mat(q):
    x, y, z, w = q
    n = math.sqrt(x * x + y * y + z * z + w * w) or 1.0
    x, y, z, w = x / n, y / n, z / n, w / n
    return np.array([
        [1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w), 0.0],
        [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w), 0.0],
        [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y), 0.0],
        [0.0, 0.0, 0.0, 1.0],
    ])


def slerp(a, b, t):
    a = np.asarray(a, float)
    b = np.asarray(b, float)
    d = float(np.dot(a, b))
    if d < 0:
        b = -b
        d = -d
    if d > 0.9995:
        r = a + t * (b - a)
    else:
        th = math.acos(max(-1.0, min(1.0, d)))
        s = math.sin(th)
        r = math.sin((1 - t) * th) / s * a + math.sin(t * th) / s * b
    return r / (np.linalg.norm(r) or 1.0)


def node_trs(node):
    if "matrix" in node:
        return np.array(node["matrix"], float).reshape(4, 4).T
    m = np.eye(4)
    t = np.array(node.get("translation", [0, 0, 0]), float)
    r = np.array(node.get("rotation", [0, 0, 0, 1]), float)
    s = np.array(node.get("scale", [1, 1, 1]), float)
    m[:3, :3] = quat_mat(r)[:3, :3] @ np.diag(s)
    m[:3, 3] = t
    return m


def sample_channel(glb, anim, ch, t):
    s = anim["samplers"][ch["sampler"]]
    times = glb.accessor(s["input"])[:, 0]
    vals = glb.accessor(s["output"])
    interp = s.get("interpolation", "LINEAR")
    if interp == "CUBICSPLINE":
        vals = vals.reshape(len(times), -1, vals.shape[-1])[:, 1, :]
    if t <= times[0]:
        return vals[0]
    if t >= times[-1]:
        return vals[-1]
    i = int(np.searchsorted(times, t) - 1)
    i = max(0, min(i, len(times) - 2))
    span = times[i + 1] - times[i]
    u = 0.0 if span <= 0 else (t - times[i]) / span
    if interp == "STEP":
        return vals[i]
    a, b = vals[i], vals[i + 1]
    if ch["target"]["path"] == "rotation":
        return slerp(a, b, u)
    return a + (b - a) * u


class Scene:
    def __init__(self, glb):
        self.glb = glb
        self.g = glb.json
        self.nodes = self.g.get("nodes", [])
        self.parent = {}
        for i, n in enumerate(self.nodes):
            for c in n.get("children", []):
                self.parent[c] = i
        self.animations = {}
        for a in self.g.get("animations", []):
            times = glb.accessor(a["samplers"][0]["input"])[:, 0]
            self.animations[a.get("name", "anim")] = (a, float(times[-1]))
        self.tex = {}
        for i, mat in enumerate(self.g.get("materials", [])):
            pbr = mat.get("pbrMetallicRoughness", {})
            ref = pbr.get("baseColorTexture")
            if ref:
                img = glb.image(self.g["textures"][ref["index"]]["source"])
                if img is not None:
                    self.tex[i] = img
            elif "baseColorFactor" in pbr:
                self.flat = getattr(self, "flat", {})
                self.flat[i] = tuple(int(c * 255) for c in pbr["baseColorFactor"][:3])

    def clip_names(self):
        return list(self.animations.keys())

    def pose(self, t, anim):
        local = [node_trs(n) for n in self.nodes]
        if anim is not None:
            a = anim
            for ch in a["channels"]:
                path = ch["target"]["path"]
                if path not in ("translation", "rotation", "scale"):
                    continue
                node = ch["target"]["node"]
                v = sample_channel(self.glb, a, ch, t)
                m = local[node]
                if path == "translation":
                    m[0][3], m[1][3], m[2][3] = v
                elif path == "scale":
                    m[0][:3] /= np.linalg.norm(m[0][:3]) or 1
                    m[1][:3] /= np.linalg.norm(m[1][:3]) or 1
                    m[2][:3] /= np.linalg.norm(m[2][:3]) or 1
                    m[0][:3] *= v[0]
                    m[1][:3] *= v[1]
                    m[2][:3] *= v[2]
                else:
                    s = np.array([np.linalg.norm(m[i][:3]) for i in range(3)])
                    r = quat_mat(v)
                    for i in range(3):
                        m[i][:3] = r[i][:3] * s[i]
        world = [None] * len(self.nodes)

        def w(i):
            if world[i] is None:
                m = local[i]
                if i in self.parent:
                    m = w(self.parent[i]) @ m
                world[i] = m
            return world[i]

        for i in range(len(self.nodes)):
            w(i)
        return world

    def meshes(self):
        out = []
        for mi, mesh in enumerate(self.g.get("meshes", [])):
            for prim in mesh["primitives"]:
                if prim.get("mode", 4) != 4:
                    continue
                pos = self.glb.accessor(prim["attributes"]["POSITION"])
                idx = self.glb.accessor(prim["indices"])[:, 0].astype(np.int64) if "indices" in prim else np.arange(len(pos))
                uv = self.glb.accessor(prim["attributes"]["TEXCOORD_0"]) if "TEXCOORD_0" in prim["attributes"] else None
                jnt = self.glb.accessor(prim["attributes"]["JOINTS_0"]).astype(np.int64) if "JOINTS_0" in prim["attributes"] else None
                wgt = self.glb.accessor(prim["attributes"]["WEIGHTS_0"]) if "WEIGHTS_0" in prim["attributes"] else None
                out.append(dict(pos=pos, idx=idx, uv=uv, jnt=jnt, wgt=wgt, mat=prim.get("material")))
        return out

    def skin(self, prim, world):
        pos = prim["pos"]
        if prim["jnt"] is None:
            return pos
        skin = self.g["skins"][0]
        joints = skin["joints"]
        ibm = self.glb.accessor(skin["inverseBindMatrices"]).reshape(-1, 4, 4)
        ibm = np.transpose(ibm, (0, 2, 1))
        jm = np.array([world[j] @ ibm[k] for k, j in enumerate(joints)])
        w = prim["wgt"]
        w = w / np.maximum(w.sum(axis=1, keepdims=True), 1e-8)
        res = np.zeros_like(pos)
        for k in range(prim["jnt"].shape[1]):
            mats = jm[prim["jnt"][:, k]]
            res += w[:, k:k + 1] * np.einsum("nij,nj->ni", mats[:, :3, :3], pos) + w[:, k:k + 1] * mats[:, :3, 3]
        return res

    def colors(self, prim, tris):
        mat = prim["mat"]
        if mat in self.tex and prim["uv"] is not None:
            img = self.tex[mat]
            W, H = img.size
            pix = np.asarray(img)
            uv = prim["uv"]
            tri = uv[tris]
            u = np.clip(((tri[:, :, 0].mean(axis=1) % 1.0) * (W - 1)).astype(int), 0, W - 1)
            v = np.clip(((tri[:, :, 1].mean(axis=1) % 1.0) * (H - 1)).astype(int), 0, H - 1)
            return pix[v, u]
        base = getattr(self, "flat", {}).get(mat, (190, 190, 195))
        return np.tile(np.array(base), (len(tris), 1))


def _rot(p, c, yaw):
    if not yaw:
        return p
    cy = math.cos(yaw)
    sy = math.sin(yaw)
    out = p.copy()
    x = p[:, 0] - c[0]
    z = p[:, 2] - c[2]
    out[:, 0] = c[0] + x * cy + z * sy
    out[:, 2] = c[2] - x * sy + z * cy
    return out


def render(scene, anim_name, times, cell_w, cell_h, ss=2, yaw=0.0, fit="clip"):
    """Render frames and page them. fit='clip' frames the whole clip, 'rest'
    frames the un-animated pose so scale drift between rest and animation stays
    visible, 'none' keeps the caller's camera when times is (t, center, span)."""
    hit = scene.animations.get(anim_name)
    anim = hit[0] if hit else None
    prims = scene.meshes()
    frames = []
    for t in times:
        world = scene.pose(t, anim)
        pts = [scene.skin(prim, world) for prim in prims]
        frames.append((t, world, pts, np.concatenate(pts, axis=0)))
    if fit == "rest":
        rw = scene.pose(0.0, None)
        ref = np.concatenate([scene.skin(prim, rw) for prim in prims], axis=0)
        lo, hi = ref.min(0), ref.max(0)
    else:
        lo = np.min(np.stack([f[3] for f in frames]), axis=(0, 1))
        hi = np.max(np.stack([f[3] for f in frames]), axis=(0, 1))
    c = (lo + hi) / 2
    h = max(hi[1] - lo[1], 1e-6)
    w = max(hi[0] - lo[0], 1e-6)
    scale = 0.92 * min((cell_w * ss) / w, (cell_h * ss) / h)
    light1 = np.array([0.45, 0.55, 0.70])
    light1 = light1 / np.linalg.norm(light1)
    light2 = np.array([-0.70, 0.20, 0.40])
    light2 = light2 / np.linalg.norm(light2)

    pages = []
    for t, world, pts, _ in frames:
        img = Image.new("RGB", (cell_w * ss, cell_h * ss), (250, 250, 252))
        drw = ImageDraw.Draw(img)
        W, H = img.size
        floor = H / 2 - (0.0 - c[1]) * scale
        if 0 <= floor < H:
            drw.line([(0, floor), (W, floor)], fill=(214, 216, 220), width=max(1, ss))
        tris_all = []
        cols_all = []
        for prim, v in zip(prims, pts):
            p = _rot(v, c, yaw)
            x = (p[:, 0] - c[0]) * scale + W / 2
            y = H / 2 - (p[:, 1] - c[1]) * scale
            idx = prim["idx"].reshape(-1, 3)
            tri_xyz = p[idx]
            screen = np.stack([x[idx], y[idx]], axis=-1)
            tris_all.append((screen, tri_xyz))
            cols_all.append(scene.colors(prim, idx))
        screens = np.concatenate([t[0] for t in tris_all], 0)
        xyz = np.concatenate([t[1] for t in tris_all], 0)
        cols = np.concatenate(cols_all, 0)
        n = np.cross(xyz[:, 1] - xyz[:, 0], xyz[:, 2] - xyz[:, 0])
        ln = np.linalg.norm(n, axis=1)
        keep = ln > 1e-9
        screens, xyz, cols, n, ln = screens[keep], xyz[keep], cols[keep], n[keep], ln[keep]
        nn = n / ln[:, None]
        l1 = np.abs(nn @ light1)
        l2 = np.abs(nn @ light2)
        area = np.abs((screens[:, 1, 0] - screens[:, 0, 0]) * (screens[:, 2, 1] - screens[:, 0, 1])
                      - (screens[:, 2, 0] - screens[:, 0, 0]) * (screens[:, 1, 1] - screens[:, 0, 1]))
        shade = 0.34 + 0.52 * l1 + 0.20 * l2
        order = np.argsort(xyz[:, :, 2].mean(axis=1))
        for ti in order:
            tri = screens[ti]
            col = tuple(int(min(255, max(0, ch * shade[ti]))) for ch in cols[ti])
            drw.polygon([(tri[0, 0], tri[0, 1]), (tri[1, 0], tri[1, 1]), (tri[2, 0], tri[2, 1])],
                        fill=col, outline=col)
        pages.append(img.resize((cell_w, cell_h), Image.LANCZOS))
    return pages


def _bone_index(scene):
    return {n.get("name"): i for i, n in enumerate(scene.g.get("nodes", [])) if n.get("name")}


def _arm_metrics(scene, world, idx):
    """Wrist bend and elbow flexion in degrees for both arms, or None."""
    out = {}
    for side in ("Left", "Right"):
        names = [f"mixamorig:{side}Arm", f"mixamorig:{side}ForeArm", f"mixamorig:{side}Hand",
                 f"mixamorig:{side}HandMiddle1"]
        if not all(n in idx for n in names):
            continue
        up = world[idx[names[0]]][:3, 3]
        el = world[idx[names[1]]][:3, 3]
        ha = world[idx[names[2]]][:3, 3]
        mi = world[idx[names[3]]][:3, 3]

        def angle(a, b):
            na = np.linalg.norm(a)
            nb = np.linalg.norm(b)
            if na < 1e-9 or nb < 1e-9:
                return 0.0
            return float(np.degrees(np.arccos(np.clip(a @ b / na / nb, -1, 1))))

        out[side] = (180.0 - angle(up - el, ha - el), angle(ha - el, mi - ha))
    return out


def audit(scene):
    """Report scale, ground and joint-angle anomalies per clip.

    Designed to catch what a silhouette cannot: a body that changes size when
    the animation starts, feet below the floor, a wrist bent past human range.
    Thresholds are deliberately loose so that only real defects fire."""
    idx = _bone_index(scene)
    prims = scene.meshes()

    def frame_stats(anim, t):
        world = scene.pose(t, anim)
        v = np.concatenate([scene.skin(p, world) for p in prims], axis=0)
        lo, hi = v.min(0), v.max(0)
        body = None
        if "mixamorig:Hips" in idx and "mixamorig:Head" in idx:
            body = float(np.linalg.norm(world[idx["mixamorig:Hips"]][:3, 3] - world[idx["mixamorig:Head"]][:3, 3]))
        return dict(h=float(hi[1] - lo[1]), w=float(hi[0] - lo[0]), ymin=float(lo[1]), body=body,
                    arms=_arm_metrics(scene, world, idx))

    rest = frame_stats(None, 0.0)
    problems = 0
    for name, (anim, dur) in scene.animations.items():
        ts = np.linspace(0, dur, 18)
        frames = [frame_stats(anim, float(t)) for t in ts]
        hs = [f["h"] for f in frames]
        ws = [f["w"] for f in frames]
        ymin = min(f["ymin"] for f in frames)
        body = [f["body"] for f in frames if f["body"] is not None]
        notes = []
        if rest["body"] and body:
            ratio = (min(body) / rest["body"], max(body) / rest["body"])
            if ratio[0] < 0.95 or ratio[1] > 1.05:
                notes.append(f"SCALE body {ratio[0]:.2f}..{ratio[1]:.2f} of rest")
        hratio = (min(hs) / rest["h"], max(hs) / rest["h"])
        if hratio[0] < 0.75 or hratio[1] > 1.25:
            notes.append(f"HEIGHT {hratio[0]:.2f}..{hratio[1]:.2f} of rest")
        if ymin < -0.03 * rest["h"]:
            notes.append(f"FLOOR min y {ymin:.3f} ({ymin / rest['h'] * 100:.0f}% of height)")
        for side in ("Left", "Right"):
            vals = [f["arms"][side][1] for f in frames if f["arms"].get(side)]
            if vals:
                mean = float(np.mean(vals))
                worst = float(np.max(vals))
                if mean > 35.0 or worst > 75.0:
                    notes.append(f"WRIST {side} mean {mean:.0f} max {worst:.0f} deg")
        for side in ("Left", "Right"):
            vals = [f["arms"][side][0] for f in frames if f["arms"].get(side)]
            if vals:
                mean = float(np.mean(vals))
                if mean > 150.0:
                    notes.append(f"ELBOW {side} mean {mean:.0f} deg (folded shut)")
        if notes:
            problems += 1
        print(f"  {name:14s} dur {dur:5.3f}s  h {hratio[0]:.2f}..{hratio[1]:.2f}  "
              f"w {min(ws) / rest['w']:.2f}..{max(ws) / rest['w']:.2f}  "
              f"{'FAIL ' + '; '.join(notes) if notes else 'ok'}")
    return problems


def sheet(model, clip, pages, out_dir, cell_w, cell_h, font):
    os.makedirs(out_dir, exist_ok=True)
    n = len(pages)
    per = 9
    made = []
    for p in range(0, n, per):
        chunk = pages[p:p + per]
        cols = 3
        rows = 3
        pad = 8
        head = 44
        W = cols * cell_w + (cols + 1) * pad
        H = head + rows * cell_h + (rows + 1) * pad
        canvas = Image.new("RGB", (W, H), (232, 234, 238))
        drw = ImageDraw.Draw(canvas)
        title = f"{os.path.basename(model)} · {clip} · cells {p + 1}-{p + len(chunk)} of {n}"
        drw.text((pad, 12), title, fill=(20, 20, 24), font=font)
        for k, (t, img) in enumerate(chunk):
            r, c = divmod(k, cols)
            x = pad + c * (cell_w + pad)
            y = head + pad + r * (cell_h + pad)
            canvas.paste(img, (x, y))
            drw.rectangle([x, y, x + cell_w - 1, y + cell_h - 1], outline=(120, 124, 132))
            label = f"t={t:5.2f}s"
            drw.rectangle([x, y, x + 96, y + 20], fill=(0, 0, 0))
            drw.text((x + 4, y + 3), label, fill=(255, 255, 255), font=font)
        name = f"{model}_{clip}_p{p // per + 1}.png"
        path = os.path.join(out_dir, name)
        canvas.save(path)
        made.append(path)
    return made


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    ins = sub.add_parser("inspect")
    ins.add_argument("glb", nargs="+")
    aud = sub.add_parser("audit")
    aud.add_argument("glb", nargs="+")
    col = sub.add_parser("collage")
    col.add_argument("glb")
    col.add_argument("--out", required=True)
    col.add_argument("--clip", action="append", default=None)
    col.add_argument("--frames", type=int, default=18)
    col.add_argument("--cell", type=int, default=340)
    col.add_argument("--yaw", type=float, default=0.0)
    col.add_argument("--fit", choices=["clip", "rest"], default="clip")
    a = ap.parse_args()

    if a.cmd == "inspect":
        for path in a.glb:
            sc = Scene(Glb(path))
            print(os.path.basename(path))
            for name, (anim, dur) in sc.animations.items():
                print(f"  {name:16s} {dur:6.3f}s channels={len(anim['channels'])}")
            for p in sc.meshes():
                print(f"  mesh verts={len(p['pos'])} tris={len(p['idx']) // 3} skinned={p['jnt'] is not None}")
        return

    if a.cmd == "audit":
        total = 0
        for path in a.glb:
            base = os.path.splitext(os.path.basename(path))[0]
            name = os.path.basename(os.path.dirname(os.path.abspath(path))) if base in ("model", "avatar") else base
            try:
                sc = Scene(Glb(path))
            except Exception as error:
                print(f"{name}: cannot parse: {error}")
                total += 1
                continue
            print(f"{name} ({path})")
            total += audit(sc)
        print(f"clips with findings: {total}")
        return

    font = ImageFont.truetype(DEFAULT_FONT, 18)
    sc = Scene(Glb(a.glb))
    clips = a.clip or sc.clip_names()
    base = os.path.splitext(os.path.basename(a.glb))[0]
    names = os.path.basename(os.path.dirname(os.path.abspath(a.glb))) if base in ("model", "avatar") else base
    for clip in clips:
        dur = sc.animations[clip][1]
        times = [dur * i / (a.frames - 1) for i in range(a.frames)] if a.frames > 1 else [dur / 2]
        pages = render(sc, clip, times, a.cell, int(a.cell * 1.3), yaw=a.yaw, fit=a.fit)
        for path in sheet(names, clip, list(zip(times, pages)), a.out, a.cell, int(a.cell * 1.3), font):
            print(path)


if __name__ == "__main__":
    main()
