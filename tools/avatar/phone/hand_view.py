"""Hand close-ups: one row per timestamp, one column per yaw."""
import os, sys, math
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import numpy as np
import mood_collage as mc
from PIL import Image, ImageDraw

def shot(prims, pts, cols_fn, center, span, yaw, size, ss=2, light=(0.45,0.55,0.70)):
    W = H = size * ss
    img = Image.new("RGB", (W, H), (250, 250, 252))
    drw = ImageDraw.Draw(img)
    scale = 0.92 * W / span
    l1 = np.array(light, float); l1 /= np.linalg.norm(l1)
    l2 = np.array([-0.7, 0.2, 0.4]); l2 /= np.linalg.norm(l2)
    tris, xyzs, colss = [], [], []
    for prim, v in zip(prims, pts):
        p = mc._rot(v, center, yaw)
        x = (p[:, 0] - center[0]) * scale + W / 2
        y = H / 2 - (p[:, 1] - center[1]) * scale
        idx = prim["idx"].reshape(-1, 3)
        tris.append(np.stack([x[idx], y[idx]], -1))
        xyzs.append(p[idx])
        colss.append(cols_fn(prim, idx))
    tr = np.concatenate(tris, 0); xyz = np.concatenate(xyzs, 0); cols = np.concatenate(colss, 0)
    n = np.cross(xyz[:,1]-xyz[:,0], xyz[:,2]-xyz[:,0]); ln = np.linalg.norm(n, axis=1)
    k = ln > 1e-9
    tr, xyz, cols, n, ln = tr[k], xyz[k], cols[k], n[k], ln[k]
    nn = n / ln[:, None]
    shade = 0.34 + 0.52*np.abs(nn@l1) + 0.20*np.abs(nn@l2)
    order = np.argsort(xyz[:,:,2].mean(1))
    for ti in order:
        t = tr[ti]
        c = tuple(int(min(255, max(0, ch*shade[ti]))) for ch in cols[ti])
        drw.polygon([(t[0,0],t[0,1]),(t[1,0],t[1,1]),(t[2,0],t[2,1])], fill=c, outline=c)
    return img.resize((size, size), Image.LANCZOS)

def main(glb, clip, prefix, size=260):
    G = mc.Glb(glb); S = mc.Scene(G); prims = S.meshes()
    idx = mc._bone_index(S)
    anim = S.animations[clip][0]
    dur = S.animations[clip][1]
    ts = [0.2, 0.9, 1.8, 2.8]
    rows = []
    for t in ts:
        w = S.pose(t, anim)
        pts = [S.skin(p, w) for p in prims]
        ha = w[idx['mixamorig:LeftHand']][:3, 3]
        cells = []
        for yaw in (0.0, math.pi/2, math.pi, -math.pi/2):
            cells.append(shot(prims, pts, S.colors, ha, 0.16, yaw, size))
        row = Image.new("RGB", (size*4, size))
        for k, c in enumerate(cells):
            row.paste(c, (k*size, 0))
        rows.append(row)
    out = Image.new("RGB", (size*4, size*len(rows)))
    for k, r in enumerate(rows):
        out.paste(r, (0, k*size))
    out.save(prefix + '.png')
    print('wrote', prefix + '.png', out.size)

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], sys.argv[3])
