"""Story Weaver procedural tileset - core primitives (16px GBA-style pixel art)."""
import colorsys, math
import numpy as np

T = 16

def hx(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))

def _shift(h, target, amt):
    d = ((target - h + 0.5) % 1.0) - 0.5
    return (h + d * amt) % 1.0

def ramp(base, n=5, spread=1.0):
    """5-step hue-shifted ramp, index 0 = darkest, 2 = base, 4 = lightest."""
    if isinstance(base, (list, tuple)) and len(base) == n and isinstance(base[0], tuple):
        return list(base)
    r, g, b = [c / 255 for c in (hx(base) if isinstance(base, str) else base)]
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    out = []
    for i in range(n):
        t = (i - (n - 1) / 2) / ((n - 1) / 2)
        if t < 0:
            ll = l * (1 + t * 0.52 * spread)
            hh = _shift(h, 0.70, -t * 0.10)
            ss = min(1.0, s * (1 - t * 0.15))
        else:
            ll = l + (1 - l) * t * 0.48 * spread
            hh = _shift(h, 0.15, t * 0.08)
            ss = s * (1 - t * 0.18)
        rr, gg, bb = colorsys.hls_to_rgb(hh, max(0.03, min(0.97, ll)), ss)
        out.append((int(rr * 255), int(gg * 255), int(bb * 255)))
    return out

def rnd(x, y, s=0):
    n = (x * 374761393 + y * 668265263 + s * 362437 + 0x9E3779B9) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n ^= n >> 16
    return n / 4294967296.0

def ri(x, y, s, n):
    return int(rnd(x, y, s) * n) % n

OUT = (24, 20, 37)  # global outline colour

# ---------------------------------------------------------------- textures
def _voronoi(seed, npts):
    pts = [(rnd(i, 1, seed) * T, rnd(i, 2, seed) * T) for i in range(npts)]
    cell = np.zeros((T, T), int); edge = np.zeros((T, T), float)
    for y in range(T):
        for x in range(T):
            ds = []
            for i, (px, py) in enumerate(pts):
                dx = abs(x - px); dx = min(dx, T - dx)
                dy = abs(y - py); dy = min(dy, T - dy)
                ds.append((dx * dx + dy * dy, i))
            ds.sort()
            cell[y, x] = ds[0][1]
            edge[y, x] = math.sqrt(ds[1][0]) - math.sqrt(ds[0][0])
    return cell, edge

def _smooth(seed, scale=4):
    """tileable value noise 16x16 in 0..1"""
    n = T // scale
    g = np.array([[rnd(i, j, seed) for i in range(n)] for j in range(n)])
    out = np.zeros((T, T))
    for y in range(T):
        for x in range(T):
            fx, fy = x / scale, y / scale
            x0, y0 = int(fx) % n, int(fy) % n
            x1, y1 = (x0 + 1) % n, (y0 + 1) % n
            tx, ty = fx - int(fx), fy - int(fy)
            tx = tx * tx * (3 - 2 * tx); ty = ty * ty * (3 - 2 * ty)
            out[y, x] = (g[y0, x0] * (1 - tx) + g[y0, x1] * tx) * (1 - ty) + (g[y1, x0] * (1 - tx) + g[y1, x1] * tx) * ty
    return out

def shade_map(style, seed=0):
    """returns (shade idx 16x16 int 0..4, accent mask 16x16 int: 0 none, 1 accent, 2 accent-light)"""
    s = np.full((T, T), 2, int); a = np.zeros((T, T), int)
    def P(x, y, v): s[y % T, x % T] = v
    def A(x, y, v=1): a[y % T, x % T] = v
    R = lambda i, j, n: ri(i, j, seed * 7 + 3, n)
    if style in ('grass', 'flowers', 'tallgrass'):
        for i in range(7 if style != 'tallgrass' else 14):
            x, y = R(i, 0, T), R(i, 1, T)
            P(x, y, 1); P(x + 2, y, 1); P(x + 1, y - 1, 3)
            if style == 'tallgrass': P(x + 1, y - 2, 3); P(x, y + 1, 0)
        for i in range(5): P(R(i, 2, T), R(i, 3, T), 3)
        if style == 'flowers':
            for i in range(4):
                x, y = (R(i, 4, 4) + i * 4) % T, (R(i, 5, 6) + i * 5 + 2) % T
                A(x, y, 2); A(x - 1, y); A(x + 1, y); A(x, y - 1); A(x, y + 1)
    elif style == 'sand':
        for i in range(12): P(R(i, 0, T), R(i, 1, T), 3)
        for i in range(7): P(R(i, 2, T), R(i, 3, T), 1)
        for i in range(2):
            x, y = R(i, 4, T), R(i, 5, T)
            for k in range(4): P(x + k, y + (k // 2), 1)
    elif style == 'dunes':
        for y in range(T):
            for x in range(T):
                v = math.sin((y + 2.5 * math.sin(x * math.pi / 8)) * math.pi / 4)
                s[y, x] = 3 if v > 0.55 else (1 if v < -0.8 else 2)
    elif style in ('dirt', 'mud', 'ash'):
        for i in range(9):
            x, y = R(i, 0, T), R(i, 1, T); P(x, y, 1); P(x + 1, y, 1)
        for i in range(8): P(R(i, 2, T), R(i, 3, T), 3)
        if style == 'mud':
            n = _smooth(seed + 5)
            s[n > 0.68] = 1; s[n > 0.8] = 0
    elif style in ('rock', 'cracked', 'cobble', 'lava', 'scales'):
        npts = {'rock': 6, 'cracked': 5, 'cobble': 10, 'lava': 5, 'scales': 14}[style]
        cell, edge = _voronoi(seed + 11, npts)
        for y in range(T):
            for x in range(T):
                c = cell[y, x]; e = edge[y, x]
                if style == 'rock':
                    s[y, x] = 0 if e < 0.9 else 1 + (c * 7 + seed) % 3
                elif style == 'cracked':
                    s[y, x] = 0 if e < 0.8 else (3 if e > 4 and rnd(x, y, seed) > 0.8 else 2)
                elif style == 'cobble':
                    s[y, x] = 0 if e < 1.0 else (2 + (c % 2) if e > 1.9 else 1 + (c % 2))
                elif style == 'scales':
                    s[y, x] = 0 if e < 0.9 else (3 if e > 2.6 else 2 - (c % 2))
                else:
                    s[y, x] = 4 if e < 0.9 else (3 if e < 2.0 else (1 if (c + seed) % 3 == 0 and e > 3 else 2))
    elif style in ('water', 'deep'):
        for i in range(5 if style == 'water' else 3):
            x, y, L = R(i, 0, T), (R(i, 1, 3) + i * 3 + seed) % T, 3 + R(i, 2, 3)
            for k in range(L): P(x + k, y, 3)
            P(x + 1, y, 4); P(x + L, y + 1, 1); P(x + L + 1, y + 1, 1)
    elif style == 'snow':
        s[:] = 3
        for i in range(6): P(R(i, 0, T), R(i, 1, T), 2)
        for i in range(4): P(R(i, 2, T), R(i, 3, T), 4)
        x, y = R(9, 0, T), R(9, 1, T); P(x, y, 2); P(x + 1, y, 2); P(x + 2, y + 1, 2)
    elif style in ('ice', 'glass', 'obsidian'):
        for y in range(T):
            for x in range(T):
                d = (x + y) % 8
                if d == 0 and rnd((x - y) // 3, 0, seed) > 0.35: s[y, x] = 3
                if d == 1 and rnd((x - y) // 3, 1, seed) > 0.75: s[y, x] = 4
        for i in range(2):
            x, y = R(i, 0, T), R(i, 1, T)
            for k in range(4): P(x + k, y + k // 2 + (k % 2), 1)
    elif style in ('brick', 'bigbrick'):
        bh, bw = (4, 8) if style == 'brick' else (8, 16)
        for y in range(T):
            row = y // bh
            for x in range(T):
                xx = (x + (row % 2) * (bw // 2)) % bw
                if y % bh == bh - 1 or xx == bw - 1: s[y, x] = 0
                else:
                    b = ((x + (row % 2) * (bw // 2)) // bw) % (T // bw * 2 or 1)
                    v = 1 if rnd(b, row, seed) > 0.7 else 2
                    s[y, x] = 3 if y % bh == 0 and v == 2 else v
    elif style in ('planks', 'planks_v'):
        for y in range(T):
            row = y // 4
            j = R(row, 9, T)
            for x in range(T):
                if y % 4 == 3: s[y, x] = 0
                elif x == j: s[y, x] = 0
                else:
                    r = rnd(x // 3, y, seed)
                    s[y, x] = 1 if r > 0.86 else (3 if r < 0.12 else 2)
        if style == 'planks_v': s = s.T.copy()
    elif style in ('tiles', 'checker'):
        for y in range(T):
            for x in range(T):
                c = ((x // 8) + (y // 8)) % 2
                if style == 'checker': s[y, x] = 1 + 2 * c
                else: s[y, x] = 2 + c if rnd(x // 8, y // 8, seed) > 0.2 else 2
                if x % 8 == 7 or y % 8 == 7: s[y, x] = 1 if style == 'tiles' else s[y, x]
                elif style == 'tiles' and (x % 8 == 0 or y % 8 == 0): s[y, x] = min(4, s[y, x] + 1)
    elif style == 'slab':
        for y in range(T):
            for x in range(T):
                if x == 15 or y == 15: s[y, x] = 0
                elif x == 0 or y == 0: s[y, x] = 3
                elif rnd(x, y, seed) > 0.93: s[y, x] = 1
        x, y = R(0, 0, 10) + 2, R(0, 1, 8) + 3
        for k in range(4): P(x + k, y + (k + 1) // 2, 1)
    elif style in ('stars', 'void'):
        s[:] = 1
        n = _smooth(seed + 2)
        s[n > 0.62] = 2 if style == 'stars' else 1
        s[n < 0.3] = 0
        if style == 'stars':
            for i in range(5): P(R(i, 0, T), R(i, 1, T), 4)
            for i in range(4): P(R(i, 2, T), R(i, 3, T), 3)
            x, y = R(8, 0, T), R(8, 1, T); A(x, y, 2); A(x - 1, y); A(x + 1, y); A(x, y - 1); A(x, y + 1)
        else:
            for i in range(2): P(R(i, 0, T), R(i, 1, T), 2)
    elif style == 'cloud':
        s[:] = 3
        for i in range(4):
            cx, cy = R(i, 0, T), R(i, 1, T)
            for k in range(-3, 4):
                P(cx + k, cy - (1 if abs(k) < 3 else 0), 4); P(cx + k, cy + 2, 2)
            P(cx - 1, cy - 2, 4); P(cx, cy - 2, 4); P(cx + 1, cy - 2, 4)
    elif style in ('moss', 'swamp', 'fungal', 'blight'):
        n = _smooth(seed + 1)
        s[n > 0.6] = 1; s[n < 0.32] = 3
        for i in range(6): P(R(i, 0, T), R(i, 1, T), 0 if style == 'swamp' else 3)
        if style in ('fungal', 'blight'):
            for i in range(4):
                x, y = R(i, 2, T), R(i, 3, T); A(x, y, 2); A(x + 1, y, 1); A(x, y + 1, 1)
    elif style == 'crystal':
        for y in range(T):
            for x in range(T):
                v = ((x + y) // 4 + 2 * ((x - y) % T // 4)) % 4
                s[y, x] = [1, 2, 3, 2][v]
                if (x + y) % 4 == 0 and (x - y) % 8 == 0: s[y, x] = 4
    elif style == 'metal':
        for y in range(T):
            for x in range(T):
                if x % 16 == 15 or y % 8 == 7: s[y, x] = 0
                elif x % 16 == 0 or y % 8 == 0: s[y, x] = 3
        for (x, y) in ((2, 2), (12, 2), (2, 10), (12, 10)): P(x, y, 4); P(x + 1, y + 1, 0)
    elif style == 'grate':
        for y in range(T):
            for x in range(T):
                s[y, x] = 0 if (x % 4 in (1, 2) and y % 4 in (1, 2)) else (3 if x % 4 == 0 or y % 4 == 0 else 2)
    elif style == 'carpet':
        for y in range(T):
            for x in range(T):
                d = abs(x % 8 - 4) + abs(y % 8 - 4)
                s[y, x] = 3 if d == 3 else (1 if d == 0 else 2)
    elif style == 'tatami':
        for y in range(T):
            for x in range(T):
                s[y, x] = 2 + (1 if (y % 2 == 0 and rnd(x // 2, y, seed) > 0.3) else 0)
                if y == 15 or x == 15: s[y, x] = 0
    elif style in ('crop', 'wheat'):
        for y in range(T):
            for x in range(T):
                if x % 4 == 1:
                    s[y, x] = 3 if y % 4 < 2 else 1
                    if style == 'wheat' and y % 4 == 0: a[y, x] = 2
                    if style == 'wheat' and y % 4 == 1: a[y, x] = 1
                elif x % 4 in (0, 2) and y % 4 == 1:
                    if style == 'wheat': a[y, x] = 1
                    else: s[y, x] = 3
                else: s[y, x] = 1 if x % 4 == 3 else 2
    elif style == 'farmland':
        for y in range(T):
            for x in range(T):
                s[y, x] = [3, 2, 1, 0][y % 4] if rnd(x, y, seed) > 0.1 else 2
    elif style == 'circuit':
        s[:] = 1
        for y in range(T):
            for x in range(T):
                if (y % 8 == 3 and rnd(x // 4, y // 8, seed) > 0.3) or (x % 8 == 5 and rnd(x // 8, y // 4, seed + 1) > 0.4):
                    s[y, x] = 3
        for i in range(3):
            x, y = R(i, 0, 2) * 8 + 5, R(i, 1, 2) * 8 + 3; A(x, y, 2); P(x + 1, y, 4); P(x - 1, y, 4)
    elif style == 'shingles':
        for y in range(T):
            row = y // 4
            for x in range(T):
                xx = (x + (row % 2) * 2) % 4
                if y % 4 == 3: s[y, x] = 0 if xx != 1 else 1
                elif xx == 3 and y % 4 >= 1: s[y, x] = 1
                elif y % 4 == 0: s[y, x] = 3
    elif style == 'thatch':
        for y in range(T):
            for x in range(T):
                r = rnd(x, y // 2, seed)
                s[y, x] = 3 if r > 0.7 else (1 if r < 0.25 else 2)
                if y % 5 == 4: s[y, x] = 0 if r > 0.3 else 1
    elif style == 'rooftile':
        for y in range(T):
            for x in range(T):
                s[y, x] = [3, 2, 2, 1][x % 4]
                if y % 8 == 7: s[y, x] = 0
    elif style == 'canopy':
        s[:] = 1
        for i in range(9):
            cx, cy = (R(i, 0, 5) + i * 5) % T, (R(i, 1, 5) + i * 7) % T
            for dy in range(-2, 3):
                for dx in range(-2, 3):
                    d = dx * dx + dy * dy
                    if d <= 5:
                        v = 3 if (dx + dy) < -1 else (2 if dx + dy < 2 else 1)
                        if d <= 1 and dx + dy <= 0: v = 4 if dx + dy < 0 else 3
                        P(cx + dx, cy + dy, v)
                    elif d <= 8 and dx + dy > 1: P(cx + dx, cy + dy, 0)
    elif style == 'pines':
        s[:] = 0
        for i, (cx, cy) in enumerate(((3, 1), (11, 3), (7, 9), (15, 11), (2, 14))):
            for k in range(6):
                w = k // 2 + (k % 2)
                for dx in range(-w, w + 1):
                    P(cx + dx, cy + k, 3 if dx < 0 else (2 if dx == 0 else 1))
            P(cx, cy, 4)
    elif style == 'bone':
        s[:] = 3
        for i in range(3):
            x, y = R(i, 0, T), R(i, 1, T)
            for k in range(5): P(x + k, y + (k * (1 + i % 2)) // 2, 1)
        for i in range(6): P(R(i, 2, T), R(i, 3, T), 2)
    elif style == 'bubbles':
        for i in range(4):
            x, y = (R(i, 0, 4) + i * 4) % T, (R(i, 1, 5) + i * 5) % T
            for dx, dy in ((0, -1), (1, -1), (-1, 0), (2, 0), (-1, 1), (2, 1), (0, 2), (1, 2)): P(x + dx, y + dy, 3 if dy < 1 else 1)
            P(x, y, 4)
    elif style == 'coral':
        n = _smooth(seed + 4)
        s[n > 0.6] = 3; s[n < 0.3] = 1
        for i in range(5):
            x, y = R(i, 0, T), R(i, 1, T); A(x, y, 2); A(x, y + 1, 1); A(x - 1, y + 1, 1); A(x + 1, y + 1, 1)
    elif style == 'rune':
        for y in range(T):
            for x in range(T):
                if x == 15 or y == 15: s[y, x] = 0
                elif x == 0 or y == 0: s[y, x] = 3
        for (x, y) in ((5, 4), (6, 4), (9, 4), (10, 4), (7, 5), (8, 5), (7, 6), (8, 6), (5, 7), (10, 7), (5, 8), (10, 8), (6, 9), (9, 9), (7, 10), (8, 10)):
            A(x, y, 1 if (x + y) % 3 else 2)
    elif style == 'flat':
        for i in range(5): P(R(i, 0, T), R(i, 1, T), 3 if i % 2 else 1)
    return s, a

def texture(style, rp, seed=0, acc=None):
    rp = ramp(rp)
    s, a = shade_map(style, seed)
    out = np.zeros((T, T, 3), np.uint8)
    for y in range(T):
        for x in range(T):
            out[y, x] = rp[int(s[y, x])]
    if acc is not None:
        ar = ramp(acc)
        for y in range(T):
            for x in range(T):
                if a[y, x]: out[y, x] = ar[2] if a[y, x] == 1 else ar[4]
    return out

# ---------------------------------------------------------------- autotile shapes
def _disc(r):
    k = int(math.ceil(r))
    return [(dx, dy) for dy in range(-k, k + 1) for dx in range(-k, k + 1) if dx * dx + dy * dy <= (r + 0.3) ** 2]

def _erode(m, r):
    if r <= 0: return m.copy()
    out = np.ones_like(m)
    for dx, dy in _disc(r): out &= np.roll(np.roll(m, dy, 0), dx, 1)
    return out

def _dilate(m, r):
    if r <= 0: return m.copy()
    out = np.zeros_like(m)
    for dx, dy in _disc(r): out |= np.roll(np.roll(m, dy, 0), dx, 1)
    return out

# neighbourhoods for the 5 mini-autotile source tiles (3x3 presence, rows top->bottom)
NB = [
    [[0, 0, 0], [0, 1, 0], [0, 0, 0]],  # 0 isolated  -> outer corners
    [[0, 0, 0], [1, 1, 1], [0, 0, 0]],  # 1 horizontal -> top/bottom edges
    [[0, 1, 0], [0, 1, 0], [0, 1, 0]],  # 2 vertical   -> left/right edges
    [[0, 1, 0], [1, 1, 1], [0, 1, 0]],  # 3 cross      -> inner corners
    [[1, 1, 1], [1, 1, 1], [1, 1, 1]],  # 4 full
]
PAD = 16

def shape_mask(nb, inset, radius):
    g = np.kron(np.array(nb, bool), np.ones((T, T), bool))
    g = np.pad(g, PAD, mode='edge')
    m = _dilate(_erode(g, inset + radius), radius)
    return m  # 80x80, centre cell at [32:48]

def autotile(top_fn, inset=2, radius=3, edge='soft', rp=None, face_h=0, face_fn=None, lip=None):
    """Build the 5 autotile source tiles. top_fn(x,y)->rgb for surface.
    edge: soft | raised | sunk | none.  Returns list of 5 RGBA 16x16."""
    tiles = []
    rp = ramp(rp) if rp is not None else None
    for nb in NB:
        m = shape_mask(nb, inset, radius)
        out = np.zeros((T, T, 4), np.uint8)
        for y in range(T):
            for x in range(T):
                X, Y = x + 32, y + 32
                if not m[Y, X]: continue
                col = None
                if edge == 'raised':
                    depth = None
                    for k in range(1, face_h + 1):
                        if not m[Y + k, X]: depth = k; break
                    if depth is not None:
                        col = face_fn(x, y, face_h - depth, face_h)  # row index from top of face
                        if depth == 1: col = OUT
                        if not m[Y, X - 1] or not m[Y, X + 1]: col = OUT
                    else:
                        col = top_fn(x, y)
                        # rim of the top surface
                        if not m[Y - 1, X] or not m[Y, X - 1] or not m[Y, X + 1]: col = lip[1] if lip else OUT
                        elif not m[Y - 2, X] or not m[Y, X - 2]: col = lip[0] if lip else col
                        elif face_h and not m[Y + face_h + 1, X]: col = lip[2] if lip else col
                elif edge == 'sunk':
                    depth = None
                    for k in range(1, face_h + 1):
                        if not m[Y - k, X]: depth = k; break
                    if depth is not None:
                        col = face_fn(x, y, depth - 1, face_h)
                    else:
                        col = top_fn(x, y)
                        if not m[Y, X - 1] or not m[Y, X + 1] or not m[Y + 1, X]: col = rp[0]
                else:
                    col = top_fn(x, y)
                    if edge == 'soft':
                        near = not (m[Y - 1, X] and m[Y + 1, X] and m[Y, X - 1] and m[Y, X + 1])
                        if near: col = rp[0] if (not m[Y + 1, X] or not m[Y, X + 1]) else rp[1]
                        elif not (m[Y - 2, X] and m[Y, X - 2]): col = rp[3]
                out[y, x] = (*col, 255)
        tiles.append(out)
    return tiles

# ---------------------------------------------------------------- sprite canvas
class C:
    def __init__(s, w=16, h=16):
        s.w, s.h = w, h
        s.a = np.zeros((h, w, 4), np.uint8)
    def p(s, x, y, c, al=255):
        x = int(x); y = int(y)
        if 0 <= x < s.w and 0 <= y < s.h and c is not None: s.a[y, x] = (c[0], c[1], c[2], al)
    def get(s, x, y):
        return s.a[y, x, 3] > 0 if 0 <= x < s.w and 0 <= y < s.h else False
    def clear(s, x, y):
        if 0 <= x < s.w and 0 <= y < s.h: s.a[y, x] = 0
    def rect(s, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1): s.p(x, y, c)
    def box(s, x0, y0, x1, y1, rp):
        rp = ramp(rp)
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                c = rp[2]
                if y == y0 or x == x0: c = rp[3]
                if y == y1 or x == x1: c = rp[1]
                s.p(x, y, c)
    def ell(s, cx, cy, rx, ry, rp, flat=False):
        rp = ramp(rp)
        for y in range(int(cy - ry - 1), int(cy + ry + 2)):
            for x in range(int(cx - rx - 1), int(cx + rx + 2)):
                nx, ny = (x - cx) / (rx + 0.01), (y - cy) / (ry + 0.01)
                d = nx * nx + ny * ny
                if d <= 1.0:
                    if flat: s.p(x, y, rp[2]); continue
                    t = -(nx * 0.6 + ny * 0.8)
                    i = 2 + (1 if t > 0.35 else 0) + (1 if t > 0.8 else 0) - (1 if t < -0.3 else 0) - (1 if t < -0.75 else 0)
                    s.p(x, y, rp[max(0, min(4, i))])
    def line(s, x0, y0, x1, y1, c):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1):
            s.p(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), c)
    def tri(s, cx, y0, y1, half, rp):
        """upward triangle apex (cx,y0) base at y1 with half-width"""
        rp = ramp(rp)
        for y in range(y0, y1 + 1):
            w = half * (y - y0) / max(1, (y1 - y0))
            for x in range(int(math.floor(cx - w)), int(math.ceil(cx + w)) + 1):
                c = rp[3] if x < cx - w * 0.3 else (rp[2] if x <= cx + w * 0.3 else rp[1])
                s.p(x, y, c)
    def outline(s, col=OUT, diag=False):
        m = s.a[:, :, 3] > 0
        add = []
        for y in range(s.h):
            for x in range(s.w):
                if m[y, x]: continue
                nb = ((1, 0), (-1, 0), (0, 1), (0, -1)) + (((1, 1), (-1, -1), (1, -1), (-1, 1)) if diag else ())
                for dx, dy in nb:
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < s.w and 0 <= yy < s.h and m[yy, xx]: add.append((x, y)); break
        for x, y in add: s.a[y, x] = (*col, 255)
        return s
    def inline(s, col=OUT):
        """darken opaque pixels on the canvas border so sprites touching the edge still read"""
        return s
    def shadow(s, y=14, x0=4, x1=11, al=70):
        for x in range(x0, x1 + 1):
            if not s.get(x, y): s.a[y, x] = (0, 0, 0, al)
            if x0 < x < x1 and not s.get(x, y + 1) and y + 1 < s.h: s.a[y + 1, x] = (0, 0, 0, al)
        return s
    def blit(s, o, ox=0, oy=0):
        for y in range(o.h):
            for x in range(o.w):
                if o.a[y, x, 3] and 0 <= x + ox < s.w and 0 <= y + oy < s.h: s.a[y + oy, x + ox] = o.a[y, x]
        return s
    def flip(s):
        o = C(s.w, s.h); o.a = s.a[:, ::-1].copy(); return o
    def rot(s, k):
        o = C(s.h if k % 2 else s.w, s.w if k % 2 else s.h); o.a = np.rot90(s.a, k).copy(); return o
    def recolor(s, frm, to):
        m = (s.a[:, :, 0] == frm[0]) & (s.a[:, :, 1] == frm[1]) & (s.a[:, :, 2] == frm[2]) & (s.a[:, :, 3] > 0)
        s.a[m, 0], s.a[m, 1], s.a[m, 2] = to
        return s
    def tiles(s):
        """split into 16x16 tiles row-major"""
        return [s.a[y:y + T, x:x + T].copy() for y in range(0, s.h, T) for x in range(0, s.w, T)]

# ---------------------------------------------------------------- atlas packer
class Atlas:
    COLS = 64
    def __init__(s):
        s.rows = []          # list of np rows (16 x COLS*16)
        s.shelves = {}; s.nrow = 0   # {block height: [next x, row]}
        s.sections = []      # (name, row_start)
        s.terrains = {}; s.lines = {}; s.objects = {}; s.sprites = {}; s.meta = {}
        s.count = 0
    def _ensure(s, rows):
        while len(s.rows) < rows: s.rows.append(np.zeros((T, s.COLS * T, 4), np.uint8))
    def section(s, name):
        s.sections.append((name, s.nrow))
    def place(s, tiles, w, h=1):
        """place w*h tiles (row-major list) as one block, returns index of top-left.
        Shelf packing: one open shelf per block height, so mixed sizes do not waste rows."""
        sh = s.shelves.get(h)
        if sh is None or sh[0] + w > s.COLS:
            sh = s.shelves[h] = [0, s.nrow]; s.nrow += h; s._ensure(s.nrow)
        for i, t in enumerate(tiles):
            x, y = sh[0] + i % w, sh[1] + i // w
            s.rows[y][:, x * T:(x + 1) * T] = t
        idx = sh[1] * s.COLS + sh[0]
        sh[0] += w; s.count += len(tiles)
        return idx
    def terrain(s, name, tiles8, prio, **meta):
        s.terrains[name] = dict(i=s.place(tiles8, len(tiles8)), n=len(tiles8) - 4, p=prio, **meta)
    def lineset(s, name, tiles16, **meta):
        s.lines[name] = dict(i=s.place(tiles16, 16), **meta)
    def obj(s, name, canvas_or_tile, **meta):
        if isinstance(canvas_or_tile, C):
            c = canvas_or_tile; w, h = c.w // T, c.h // T
            s.objects[name] = dict(i=s.place(c.tiles(), w, h), w=w, h=h, **meta)
        else:
            s.objects[name] = dict(i=s.place([canvas_or_tile], 1), w=1, h=1, **meta)
    def sprite(s, name, four, **meta):
        """four canvases: down, left, right, up"""
        w, h = four[0].w // T, four[0].h // T
        if w == 1 and h == 1: s.sprites[name] = dict(i=s.place([c.a for c in four], 4), **meta); return
        tl = [c.a[j * T:(j + 1) * T, k * T:(k + 1) * T].copy() for j in range(h) for c in four for k in range(w)]
        s.sprites[name] = dict(i=s.place(tl, 4 * w, h), w=w, h=h, **meta)
    def image(s):
        return np.concatenate(s.rows, 0)
