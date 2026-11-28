"""Reference map renderer - the exact algorithm to port to Kotlin."""
import numpy as np
T = 16

def tile(img, idx, cols=64):
    y, x = divmod(idx, cols)
    return img[y * T:(y + 1) * T, x * T:(x + 1) * T]

def over(dst, src, x, y):
    h, w = src.shape[:2]; H, W = dst.shape[:2]
    x0, y0, x1, y1 = max(x, 0), max(y, 0), min(x + w, W), min(y + h, H)
    if x0 >= x1 or y0 >= y1: return
    s = src[y0 - y:y1 - y, x0 - x:x1 - x].astype(float); d = dst[y0:y1, x0:x1].astype(float)
    a = s[:, :, 3:4] / 255
    d[:, :, :3] = s[:, :, :3] * a + d[:, :, :3] * (1 - a)
    d[:, :, 3:4] = np.maximum(d[:, :, 3:4], s[:, :, 3:4])
    dst[y0:y1, x0:x1] = d.astype(np.uint8)

def h2(x, y):
    n = (x * 73856093) ^ (y * 19349663)
    return (n ^ (n >> 7)) & 0x7FFFFFFF

Q = ((0, 0, -1, -1), (8, 0, 1, -1), (0, 8, -1, 1), (8, 8, 1, 1))  # quadrant x, y, neighbour dx, dy

def render(img, atlas, W, H, terrain, lines=None, objects=(), sprites=(), default=None):
    """terrain {(x,y):name}; lines {(x,y):name}; objects [(name,x,y)] top-left; sprites [(name,x,y,dir 0-3 = down,left,right,up)]"""
    out = np.zeros((H * T, W * T, 4), np.uint8); TR = atlas['terrains']
    tn = lambda x, y: terrain.get((min(max(x, 0), W - 1), min(max(y, 0), H - 1)), default)
    for y in range(H):
        for x in range(W):
            me = tn(x, y)
            if me is None: continue
            t = TR[me]
            for qx, qy, dx, dy in Q:
                ns = (tn(x + dx, y), tn(x, y + dy), tn(x + dx, y + dy))
                ch, cv, cd = [n is not None and TR[n]['p'] >= t['p'] for n in ns]
                if ch and cv and cd: src = t['i'] + 4 + h2(x, y) % t['n']
                else:
                    src = t['i'] + (3 if ch and cv else 1 if ch else 2 if cv else 0)
                    low = [n for n, c in zip(ns, (ch, cv, cd)) if n is not None and not c]
                    if low:  # lower neighbour shows through the transparent rim
                        u = max(low, key=lambda n: TR[n]['p'])
                        over(out, tile(img, TR[u]['i'] + 4)[qy:qy + 8, qx:qx + 8], x * T + qx, y * T + qy)
                over(out, tile(img, src)[qy:qy + 8, qx:qx + 8], x * T + qx, y * T + qy)
    for (x, y), name in (lines or {}).items():
        bits = sum(b for b, (dx, dy) in ((1, (0, -1)), (2, (1, 0)), (4, (0, 1)), (8, (-1, 0))) if lines.get((x + dx, y + dy)) == name)
        over(out, tile(img, atlas['lines'][name]['i'] + bits), x * T, y * T)
    OB = atlas['objects']
    for name, x, y in sorted(objects, key=lambda o: o[2] + OB[o[0]]['h']):
        o = OB[name]
        for j in range(o['h']):
            for i in range(o['w']): over(out, tile(img, o['i'] + j * 64 + i), (x + i) * T, (y + j) * T)
    for name, x, y, d in sprites: over(out, tile(img, atlas['sprites'][name]['i'] + d), x * T, y * T)
    return out
