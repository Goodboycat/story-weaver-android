"""Props (objects). Each part is a function part_x(A) listed in PARTS; build() runs them all.
Add new parts in new chats: write part_xxx(A), append to PARTS. Sizes are multiples of 16."""
from core import *

WOOD = '#7a4a2a'; STONE = '#8a8d96'; SNOW = '#e8f0fa'

def speck(c, rp, seed, dens=0.10):
    """leaf / grain noise: nudge base-colour pixels one ramp step up or down"""
    rp = ramp(rp)
    for y in range(c.h):
        for x in range(c.w):
            if c.a[y, x, 3] and tuple(c.a[y, x, :3]) == rp[2]:
                r = rnd(x, y, seed)
                if r < dens: c.p(x, y, rp[3])
                elif r > 1 - dens: c.p(x, y, rp[1])

def cap(c, rp, depth=2, seed=0, y_max=999):
    """snow / moss cap: recolour the top `depth` px of every column"""
    rp = ramp(rp)
    for x in range(c.w):
        n = 0
        for y in range(min(c.h, y_max)):
            if c.a[y, x, 3]:
                d = depth + ri(x, 0, seed, 2)
                if n < d: c.p(x, y, rp[4] if n < d - 1 else rp[3])
                n += 1
                if n >= d: break

def thick(c, x0, y0, x1, y1, rp, w=2):
    rp = ramp(rp)
    for i in range(w):
        c.line(x0 + i, y0, x1 + i, y1, rp[3] if i == 0 else rp[1] if i == w - 1 else rp[2])

def fin(c, sh=None):
    c.outline()
    if sh: c.shadow(*sh)
    return c

def trunk(c, x0, y0, x1, y1, rp=WOOD):
    rp = ramp(rp); c.box(x0, y0, x1, y1, rp)
    for y in range(y0 + 1, y1, 3): c.p(x0 + 1 + (y % 2), y, rp[1])
    c.p(x0 - 1, y1, rp[1]); c.p(x1 + 1, y1, rp[1]); c.p(x0 - 1, y1 - 1, rp[2]); c.p(x1 + 1, y1 - 1, rp[1])

# ------------------------------------------------------------------ trees (2x3 tiles = 32x48)
def tree_round(leaf, wood=WOOD, seed=0, snow=False, dots=None):
    c = C(32, 48); trunk(c, 13, 30, 18, 44, wood)
    for cx, cy, rx, ry in ((15.5, 25, 10, 6), (8.5, 20, 7, 7), (22.5, 20, 7, 7), (15.5, 16, 12, 11), (11, 10, 7, 6), (20, 9, 7, 6)):
        c.ell(cx, cy, rx, ry, leaf)
    speck(c, leaf, seed, 0.14)
    if dots:
        dr = ramp(dots)
        for y in range(3, 30):
            for x in range(2, 30):
                if c.get(x, y) and y < 29 and rnd(x, y, seed + 9) < 0.09: c.p(x, y, dr[3]); c.p(x + 1, y, dr[2])
    if snow: cap(c, SNOW, 3, seed, 26)
    return fin(c, (44, 8, 23))

def tree_pine(leaf, wood=WOOD, seed=0, snow=False):
    c = C(32, 48); trunk(c, 14, 37, 17, 45, wood); lr = ramp(leaf)
    for y0, y1, half in ((22, 39, 13), (12, 28, 10.5), (3, 17, 7.5)):
        c.tri(15.5, y0, y1, half, lr)
        for x in range(32):          # ragged dark hem under each tier
            if c.get(x, y1) and (x + seed) % 3: c.p(x, y1, lr[0])
            if c.get(x, y1 - 1) and (x + seed) % 3 == 0: c.p(x, y1 - 1, lr[1])
        if snow:
            for y in range(y0, y0 + 5):
                for x in range(32):
                    if c.get(x, y) and abs(x - 15.5) < (y - y0) * half / (y1 - y0) + 1 and rnd(x, y, seed) < 0.8: c.p(x, y, ramp(SNOW)[4 if x < 16 else 3])
    speck(c, leaf, seed, 0.10)
    return fin(c, (45, 9, 22))

def tree_palm(leaf='#3fae4a', wood='#b08a55', seed=0):
    c = C(32, 48); wr = ramp(wood); lr = ramp(leaf)
    for y in range(13, 46):
        x = 14 + round(3 * math.sin((y - 13) / 32 * 1.7))
        for i in range(4): c.p(x + i, y, wr[1] if y % 3 == 0 else wr[3 if i == 0 else 1 if i == 3 else 2])
    for ang in (-2.9, -2.3, -1.57, -0.85, -0.25, 0.35, 2.8):
        px, py = 15.0, 12.0
        for t in range(14):
            a = ang + (0.09 * t if math.cos(ang) >= 0 else -0.09 * t) * (1 if abs(ang + 1.57) > 0.2 else 0)
            px += math.cos(a) * 1.0; py += math.sin(a) * 1.0 + t * 0.035
            w = 3 if 2 < t < 10 else 2
            for i in range(w): c.p(px, py + i, lr[3 if i == 0 else 1 if i == w - 1 else 2])
            if t % 2: c.p(px, py + w, lr[0])
    for cx, cy in ((13, 14), (17, 15), (15, 16)): c.ell(cx, cy, 1.5, 1.5, '#6b4423')
    return fin(c, (45, 9, 22))

def tree_dead(wood='#6b5a4e', seed=0):
    c = C(32, 48); trunk(c, 13, 20, 18, 44, wood)
    for x0, y0, x1, y1, w in ((14, 22, 5, 10, 3), (16, 22, 26, 8, 3), (15, 20, 15, 3, 3), (8, 14, 3, 14, 2), (9, 13, 10, 5, 2),
                              (23, 12, 29, 13, 2), (22, 13, 21, 4, 2), (15, 9, 10, 2, 2), (16, 10, 20, 1, 2), (13, 30, 8, 26, 2)):
        thick(c, x0, y0, x1, y1, wood, w)
    c.p(15, 33, ramp(wood)[0]); c.p(16, 33, ramp(wood)[0]); c.p(15, 34, ramp(wood)[0])   # knot hole
    return fin(c, (44, 9, 22))

def tree_crystal(col='#58c8f0', base='#5a5f78', seed=0):
    c = C(32, 48); cr = ramp(col)
    for cx, y0, half in ((7.5, 20, 4), (24.5, 15, 4.5), (11.5, 10, 4.5), (20.5, 24, 4), (15.5, 2, 6)):
        c.tri(cx, y0, 43, half, cr)
        for y in range(y0 + 2, 42, 4): c.p(cx - 1, y, cr[4])
        c.p(cx, y0, cr[4]); c.p(cx, y0 + 1, cr[4])
    for cx, cy, rx, ry in ((8, 43, 5, 3), (23, 43, 6, 3), (15.5, 44, 7, 3)): c.ell(cx, cy, rx, ry, base)
    return fin(c, (46, 6, 25))

def tree_mushroom(capc='#d8443c', stem='#e8dcc0', seed=0):
    c = C(32, 48)
    c.ell(15.5, 20, 14, 13, capc)
    for y in range(23, 48):
        for x in range(32): c.clear(x, y)
    gr = ramp('#c9b78f')
    for x in range(3, 29):
        if c.get(x, 22): c.p(x, 23, gr[1 if x % 2 else 3]); c.p(x, 22, ramp(capc)[0])
    c.box(12, 24, 19, 44, stem); c.ell(15.5, 43, 6, 2.5, stem)
    for cx, cy, r in ((9, 14, 2.5), (18, 10, 3), (24, 17, 2), (14, 18, 1.5), (5, 19, 1.2)): c.ell(cx, cy, r, r * 0.8, '#fff4e0', flat=True)
    return fin(c, (45, 8, 23))

# ------------------------------------------------------------------ small nature props (16x16 unless noted)
def bush(leaf, seed=0, dots=None, snow=False):
    c = C()
    for cx, cy, rx, ry in ((7.5, 10, 6, 4), (4.5, 8, 3.5, 3.5), (10.5, 8, 4, 3.5), (7.5, 6.5, 4, 3.5)): c.ell(cx, cy, rx, ry, leaf)
    speck(c, leaf, seed, 0.12)
    if dots:
        for x, y in ((4, 8), (8, 6), (11, 9), (6, 11), (9, 11)): c.p(x, y, ramp(dots)[3]); c.p(x, y + 1, ramp(dots)[1])
    if snow: cap(c, SNOW, 2, seed)
    return fin(c, (14, 3, 12))

def rock(col=STONE, big=False, seed=0, top=None):
    c = C()
    if big: c.ell(7.5, 9, 6.5, 5, col); c.ell(5.5, 7, 4, 4, col); c.ell(10, 8, 3.5, 3.5, col)
    else: c.ell(7.5, 10.5, 4.5, 3, col); c.ell(6.5, 9.5, 3, 2.5, col)
    r = ramp(col); c.p(8, 10, r[0]); c.p(9, 11, r[0]); c.p(5, 9 if big else 11, r[1])
    if top: cap(c, top, 2, seed)
    return fin(c, (14, 3, 12))

def boulder(col=STONE, seed=0, top=None):
    c = C(32, 32)
    for cx, cy, rx, ry in ((15.5, 19, 13, 9), (10, 14, 8, 8), (20, 13, 8, 7), (15, 10, 7, 5)): c.ell(cx, cy, rx, ry, col)
    r = ramp(col)
    for x0, y0, x1, y1 in ((14, 9, 16, 16), (16, 16, 13, 22), (22, 12, 24, 19), (7, 15, 9, 20)): c.line(x0, y0, x1, y1, r[0])
    speck(c, col, seed, 0.06)
    if top: cap(c, top, 3, seed)
    return fin(c, (28, 5, 26))

def flowers(col, seed=0, leaf='#3f9a3f'):
    c = C(); lr = ramp(leaf); fr = ramp(col)
    for i, (x, y) in enumerate(((3, 5), (11, 4), (7, 9), (12, 11), (3, 12))):
        x += ri(i, 0, seed, 2); c.p(x, y + 2, lr[1]); c.p(x, y + 3, lr[2]); c.p(x - 1, y + 3, lr[3])
        for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)): c.p(x + dx, y + dy, fr[3] if dy <= 0 else fr[1])
        c.p(x, y, (250, 224, 96))
    return c            # no outline: reads as ground cover

def stump(wood=WOOD, moss=None):
    c = C(); c.box(4, 7, 11, 13, wood); c.ell(7.5, 7, 4, 2.2, '#d9b77e', flat=True)
    rr = ramp('#d9b77e'); c.p(7, 7, rr[1]); c.p(8, 7, rr[1]); c.p(5, 7, rr[1]); c.p(10, 7, rr[1])
    w = ramp(wood); c.p(3, 13, w[1]); c.p(12, 13, w[1]); c.p(6, 10, w[1]); c.p(9, 11, w[1])
    if moss: c.p(4, 8, ramp(moss)[3]); c.p(5, 9, ramp(moss)[2]); c.p(4, 9, ramp(moss)[2]); c.p(11, 12, ramp(moss)[3])
    return fin(c, (14, 3, 12))

def log(wood=WOOD):
    c = C(32, 16); c.box(5, 6, 27, 12, wood); w = ramp(wood)
    for x in range(8, 26, 4): c.p(x, 8, w[1]); c.p(x + 1, 8, w[1]); c.p(x + 2, 10, w[1])
    c.ell(5, 9, 2.5, 3.2, '#d9b77e', flat=True); c.p(5, 9, ramp('#d9b77e')[1]); c.ell(27, 9, 1.5, 3.2, w[1], flat=True)
    return fin(c, (14, 5, 27))

def cactus(col='#4f9a4a', flower=None):
    c = C(16, 32); c.box(6, 6, 10, 28, col); c.box(2, 12, 4, 19, col); c.box(2, 18, 6, 20, col); c.box(12, 9, 14, 17, col); c.box(10, 16, 14, 18, col)
    r = ramp(col)
    for y in range(8, 28, 3): c.p(8, y, r[4]); c.p(3, min(y + 5, 18), r[4]); c.p(13, min(y + 3, 15), r[4])
    for x, y in ((6, 6), (10, 6), (2, 12), (4, 12), (12, 9), (14, 9)): c.clear(x, y)
    if flower: c.ell(8, 5, 1.6, 1.4, flower); c.p(13, 8, ramp(flower)[3])
    return fin(c, (29, 3, 13))

def reeds(col='#6f9a48', tip='#7a4a2a'):
    c = C(); r = ramp(col)
    for x, y0 in ((3, 5), (6, 2), (9, 4), (12, 6), (7, 7)):
        for y in range(y0, 15): c.p(x + (1 if y < y0 + 3 and x % 2 else 0), y, r[3 if y < y0 + 4 else 2 if y < 12 else 1])
        if x % 3 == 0: c.p(x, y0 - 1, ramp(tip)[2]); c.p(x, y0, ramp(tip)[2]); c.p(x, y0 + 1, ramp(tip)[1])
    return c

def lilypad(col='#4fae58', flower=None):
    c = C(); c.ell(7.5, 8.5, 5, 3.5, col); c.clear(7, 5); c.clear(8, 5); c.clear(8, 6); c.clear(7, 6); c.p(7, 9, ramp(col)[1]); c.p(5, 8, ramp(col)[4])
    if flower: c.ell(9.5, 8, 1.6, 1.4, flower); c.p(9, 8, (250, 224, 96))
    return c.outline(ramp(col)[0])

def shroom(capc='#d8443c'):
    c = C(); c.box(7, 10, 8, 13, '#e8dcc0'); c.ell(7.5, 8.5, 3.5, 2.4, capc); c.p(6, 8, (255, 244, 224)); c.p(9, 7, (255, 244, 224))
    c.box(11, 12, 11, 13, '#e8dcc0'); c.ell(11, 11, 1.6, 1.2, capc)
    return fin(c)

def part_nature(A):
    A.section('props_nature'); o = lambda n, c, **m: A.obj(n, c, cat='nature', **m)
    for n, c in (('tree_oak', tree_round('#3f9a3f')), ('tree_oak_dark', tree_round('#2f7a45', seed=3)), ('tree_autumn', tree_round('#d9822b', seed=5)),
                 ('tree_golden', tree_round('#e0b83a', seed=6)), ('tree_cherry', tree_round('#f2a6c4', '#6b4034', 2, dots='#ffe6f0')),
                 ('tree_apple', tree_round('#46a046', seed=4, dots='#e03c3c')), ('tree_snowy_oak', tree_round('#3a8a5a', seed=7, snow=True)),
                 ('tree_blight', tree_round('#6a4a8a', '#3a3040', 8)), ('tree_fey', tree_round('#58d8c0', '#8a6ab0', 9, dots='#fff0a0')),
                 ('tree_pine', tree_pine('#2f7f52')), ('tree_pine_dark', tree_pine('#255f4a', seed=1)), ('tree_snowy', tree_pine('#2f7860', seed=2, snow=True)),
                 ('tree_palm', tree_palm()), ('tree_dead', tree_dead()), ('tree_dead_ash', tree_dead('#4a4248', 1)),
                 ('tree_crystal', tree_crystal()), ('tree_crystal_violet', tree_crystal('#b070f0')), ('tree_crystal_ember', tree_crystal('#f08040', '#4a3a3a')),
                 ('giant_mushroom', tree_mushroom()), ('giant_mushroom_blue', tree_mushroom('#4a8ae0')), ('giant_mushroom_glow', tree_mushroom('#58e0b0', '#c8f0e0'))):
        o(n, c, solid=1)
    for n, c in (('boulder', boulder()), ('boulder_mossy', boulder(top='#5fa04a')), ('boulder_snowy', boulder('#9aa4b4', top=SNOW)),
                 ('boulder_sand', boulder('#c8a070')), ('boulder_obsidian', boulder('#3a3448'))): o(n, c, solid=1)
    for n, c in (('cactus', cactus()), ('cactus_flower', cactus(flower='#f060a0'))): o(n, c, solid=1)
    o('log', log(), solid=1); o('log_dark', log('#4f3a30'), solid=1)
    for n, c in (('bush', bush('#3f9a3f')), ('bush_dark', bush('#2f7a45', 1)), ('bush_berry', bush('#3f9a3f', 2, '#e03c3c')), ('bush_blueberry', bush('#3a8a5a', 3, '#5a6af0')),
                 ('bush_flower', bush('#46a046', 4, '#f8e060')), ('bush_autumn', bush('#d9822b', 5)), ('bush_snowy', bush('#3a8a5a', 6, snow=True)),
                 ('bush_dry', bush('#b0a060', 7)), ('bush_blight', bush('#6a4a8a', 8)), ('bush_fey', bush('#58d8c0', 9, '#fff0a0')),
                 ('rock', rock()), ('rock_big', rock(big=True)), ('rock_mossy', rock(big=True, top='#5fa04a')), ('rock_snowy', rock('#9aa4b4', True, top=SNOW)),
                 ('rock_sand', rock('#c8a070', True)), ('rock_dark', rock('#3a3448', True)), ('rock_ore_gold', None), ('rock_ore_iron', None), ('rock_ore_gem', None),
                 ('stump', stump()), ('stump_mossy', stump(moss='#5fa04a')), ('stump_dark', stump('#4f3a30')),
                 ('mushrooms', shroom()), ('mushrooms_blue', shroom('#4a8ae0')), ('mushrooms_brown', shroom('#a8703a'))):
        if c is None:
            col = dict(gold='#f0c83a', iron='#c86a4a', gem='#48d8e8')[n.split('_')[-1]]; c = rock('#6a6d78', True)
            for x, y in ((5, 7), (9, 9), (7, 11), (11, 7)): c.p(x, y, ramp(col)[4]); c.p(x + 1, y, ramp(col)[2]); c.p(x, y + 1, ramp(col)[1])
        o(n, c, solid=1)
    for n, col in (('flowers_red', '#e03c3c'), ('flowers_yellow', '#f0c83a'), ('flowers_blue', '#5a8af0'), ('flowers_white', '#f0f0f8'),
                   ('flowers_purple', '#a060e0'), ('flowers_pink', '#f080b0'), ('flowers_orange', '#f08a30'), ('flowers_ghost', '#a0f0e0')):
        o(n, flowers(col, len(n)), solid=0)
    o('reeds', reeds(), solid=0); o('reeds_dry', reeds('#b0a060'), solid=0); o('lilypad', lilypad(), solid=0); o('lilypad_flower', lilypad(flower='#f8a0c8'), solid=0)

PARTS = [part_nature]

def build(A):
    for p in PARTS: p(A)
