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

# ------------------------------------------------------------------ village (2b)
GOLD = '#e8b830'; IRON = '#7d8494'; DARKWOOD = '#5a3a26'; CLOTH = '#c8b080'
FIRE = ((255, 240, 150), (250, 180, 50), (230, 90, 30))
ICO = {'W': (248, 244, 232), 'Y': (240, 200, 60), 'R': (216, 60, 70), 'S': (70, 76, 92), 'B': (80, 130, 230), 'N': (150, 90, 40), 'n': (226, 170, 90),
       'G': (70, 160, 80), 'D': (60, 40, 30)}
ICONS = {'text': ('DDDDDD', '......', 'DDDD.D', '......', 'DD.DDD'), 'inn': ('.WWWW.', 'YYYYDD', 'YYYY.D', 'YYYYDD', 'YYYY..'),
         'smith': ('SSSSSS', '.SSSSS', '..SS..', '.SSSS.', 'SSSSSS'), 'potion': ('..WW..', '..SS..', '.RRRR.', 'RRWRRR', '.RRRR.'),
         'food': ('......', '.nnnn.', 'nNnNnn', 'NNNNNN', '......'), 'none': ()}

def px(c, pts, col):
    for x, y in pts: c.p(x, y, col)

def icon(c, name, ox, oy):
    for j, row in enumerate(ICONS[name]):
        for i, ch in enumerate(row):
            if ch != '.': c.p(ox + i, oy + j, ICO[ch])

def flame(c, cx, y, h=5):
    """flame 4px wide, base row y, centre columns cx..cx+1"""
    for i in range(h):
        half = 1 if i < h - 2 else 0
        for x in range(cx - half, cx + 2 + half):
            if i == h - 1 and x != cx + (y % 2): continue
            c.p(x, y - i, FIRE[0] if (x in (cx, cx + 1) and i < 2) else FIRE[1] if (x in (cx, cx + 1) or i == 0) else FIRE[2])

def glow(c, cx, cy, r, col=(255, 220, 120), al=70):
    for y in range(int(cy - r), int(cy + r) + 1):
        for x in range(int(cx - r), int(cx + r) + 1):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r and not c.get(x, y): c.p(x, y, col, al)

def sign(ic='text', board='#b98a55', hang=False):
    c = C()
    if hang:
        c.box(1, 1, 2, 14, DARKWOOD); c.rect(3, 2, 13, 2, ramp(IRON)[1]); px(c, ((6, 3), (12, 3)), ramp(IRON)[3]); c.box(4, 4, 14, 11, board); icon(c, ic, 6, 5)
        return fin(c, (14, 1, 6))
    c.box(7, 10, 8, 14, DARKWOOD); c.box(2, 2, 13, 10, board); icon(c, ic, 5, 4)
    return fin(c, (14, 5, 10))

def sign_arrow():
    c = C(); c.box(7, 3, 8, 14, DARKWOOD); c.box(2, 4, 11, 8, '#b98a55'); r = ramp('#b98a55')
    px(c, ((12, 5), (12, 6), (13, 6)), r[2]); c.p(12, 7, r[1]); c.rect(4, 6, 9, 6, ICO['D'])
    return fin(c, (14, 5, 10))

def well(roof=None):
    c = C(16, 32); s = ramp(STONE); c.box(2, 20, 13, 29, STONE)
    for y in (23, 26): c.rect(2, y, 13, y, s[1])
    for x, y in ((5, 21), (10, 21), (7, 24), (12, 24), (4, 27), (9, 27)): c.rect(x, y, x, y + 1, s[1])
    c.ell(7.5, 20, 6.3, 3, '#a8abb4'); c.ell(7.5, 20, 4.3, 1.7, '#2f5fc0', flat=True); px(c, ((6, 20), (7, 19)), (150, 200, 250))
    top = 6 if roof else 11
    c.box(2, top, 3, 19, DARKWOOD); c.box(12, top, 13, 19, DARKWOOD); c.box(2, top, 13, top + 1, WOOD)
    c.line(7, top + 2, 7, 15, CLOTH and (200, 180, 130)); c.box(6, 15, 9, 18, WOOD); c.rect(6, 16, 9, 16, ramp(IRON)[1])
    if roof:
        c.tri(7.5, 0, 6, 8, roof); r = ramp(roof)
        for y in (3, 5): px(c, [(x, y) for x in range(16) if c.get(x, y) and (x + y) % 3 == 0], r[1])
    else: px(c, ((14, top), (15, top), (15, top + 1), (15, top + 2)), ramp(IRON)[2])
    return fin(c, (30, 2, 13))

def crate(wood='#b07a44'):
    c = C(); c.box(2, 4, 13, 13, wood); w = ramp(wood); c.box(3, 5, 12, 12, w)
    for x in (6, 9): c.rect(x, 6, x, 11, w[1])
    c.line(4, 11, 11, 6, w[0]); c.box(2, 4, 13, 5, w[3:] + w[2:3] + w[1:2] + w[:1] if False else wood); c.rect(2, 12, 13, 13, w[1])
    px(c, ((3, 4), (12, 4), (3, 13), (12, 13)), ramp(IRON)[1])
    return fin(c, (14, 2, 13))

def barrel(wood='#9a6238', top=None):
    c = C(); c.ell(7.5, 9, 5, 5.6, wood); c.rect(4, 4, 11, 4, ramp(wood)[2]); w = ramp(wood)
    for x in (5, 8, 10): c.rect(x, 6, x, 12, w[1])
    for y in (6, 12): px(c, [(x, y) for x in range(16) if c.get(x, y)], ramp(IRON)[1]); c.p(5, y, ramp(IRON)[3])
    c.ell(7.5, 4, 4, 1.6, top or '#c89860', flat=True); px(c, ((6, 4), (9, 4)) if not top else ((6, 3),), ramp(top or '#c89860')[4 if top else 1])
    return fin(c, (14, 3, 12))

def sack(col=CLOTH, grain=None):
    c = C(); c.ell(7.5, 10, 5, 4, col); r = ramp(col)
    if grain: c.ell(7.5, 6.5, 3.4, 1.5, grain, flat=True); px(c, ((6, 6), (9, 7), (8, 6)), ramp(grain)[4]); px(c, ((4, 7), (11, 7)), r[3])
    else: c.box(6, 5, 9, 6, col); px(c, ((5, 4), (7, 3), (9, 4), (10, 3)), r[3]); c.rect(6, 7, 9, 7, (150, 90, 40))
    px(c, ((5, 10), (6, 12), (10, 11)), r[1])
    return fin(c, (14, 3, 12))

def pot(col='#b8683a', tall=False, broken=False, band=None):
    c = C()
    if tall: c.ell(7.5, 9.5, 3.6, 4.6, col); c.box(5, 3, 10, 4, col); c.rect(6, 5, 9, 5, ramp(col)[1])
    else: c.ell(7.5, 10, 4.6, 3.8, col); c.box(5, 5, 10, 6, col)
    y = 3 if tall else 5; c.rect(6, y, 9, y, ramp(col)[0])
    if band: px(c, [(x, 9) for x in range(16) if c.get(x, 9)], ramp(band)[2]); px(c, [(x, 10) for x in range(16) if c.get(x, 10) and x % 2], ramp(band)[3])
    if broken:
        for x, yy in ((9, 5), (10, 5), (10, 6), (9, 6), (11, 7), (10, 7), (11, 8), (8, 5)): c.clear(x, yy)
        px(c, ((9, 7), (10, 8), (9, 8)), ramp(col)[0]); px(c, ((13, 13), (12, 12), (13, 12)), ramp(col)[2])
    return fin(c, (14, 4, 11))

def chest(body=WOOD, trim=IRON, opened=False):
    c = C(); b = ramp(body); t = ramp(trim)
    if opened:
        c.box(2, 1, 13, 6, b[:1] + b[:4]); c.rect(3, 2, 12, 5, b[0]); c.rect(3, 6, 12, 8, (30, 22, 30))
        px(c, ((4, 7), (6, 6), (8, 7), (10, 6), (11, 7)), FIRE[0]); px(c, ((5, 7), (9, 7), (7, 6)), ICO['Y']); c.box(2, 9, 13, 13, body)
    else:
        c.box(2, 4, 13, 8, b[1:] + b[4:]); c.clear(2, 4); c.clear(13, 4); c.box(2, 8, 13, 13, body); c.rect(2, 8, 13, 8, b[0])
    for x in (3, 12): c.rect(x, 9 if opened else 4, x, 13, t[2]); c.p(x, 9 if opened else 5, t[4])
    c.rect(2, 13, 13, 13, t[1]); y = 10 if opened else 8; c.box(7, y, 8, y + 2, GOLD); c.p(7, y + 1, (40, 30, 30))
    return fin(c, (14, 2, 13))

def torch(wall=False):
    c = C()
    if wall: thick(c, 5, 14, 7, 8, WOOD); c.box(6, 7, 9, 8, IRON); flame(c, 7, 6)
    else: c.box(7, 8, 8, 14, WOOD); c.box(6, 6, 9, 7, IRON); px(c, ((6, 14), (9, 14)), ramp(WOOD)[1]); flame(c, 7, 5)
    fin(c, None if wall else (15, 5, 10)); glow(c, 7.5, 4, 4.2)
    return c

def campfire(lit=True):
    c = C(); thick(c, 3, 12, 11, 9, WOOD); thick(c, 3, 9, 11, 12, DARKWOOD)
    for x, y in ((2, 13), (5, 14), (9, 14), (12, 13), (13, 10), (1, 10)): c.box(x, y, x + 1, y, STONE)
    if lit: flame(c, 7, 10, 7); px(c, ((5, 9), (10, 8)), FIRE[2])
    else: px(c, ((6, 10), (7, 11), (9, 10), (8, 9)), (60, 56, 64)); px(c, ((7, 10),), FIRE[2]); px(c, ((7, 6), (8, 4), (7, 2)), (200, 200, 210))
    fin(c)
    if lit: glow(c, 7.5, 8, 6.5)
    return c

def lamp_post(lit=True, col='#3a3f4c'):
    c = C(16, 32); c.box(7, 11, 8, 29, col); c.box(5, 28, 10, 30, col); c.box(6, 10, 9, 11, col); c.box(4, 3, 11, 9, col)
    c.rect(5, 4, 10, 8, (250, 224, 110) if lit else (120, 136, 160)); c.rect(7, 4, 8, 8, (255, 248, 200) if lit else (150, 166, 190)); c.tri(7.5, 0, 3, 5, col)
    fin(c, (30, 4, 11))
    if lit: glow(c, 7.5, 6, 7.5, al=55)
    return c

def hay_bale():
    c = C(); h = '#d8b84a'; c.box(2, 5, 13, 13, h); speck(c, h, 3, 0.22)
    for x in (5, 10): c.rect(x, 5, x, 13, (150, 90, 40))
    px(c, ((1, 12), (14, 11), (1, 8), (14, 6)), ramp(h)[3])
    return fin(c, (14, 2, 13))

def cart(load=None):
    c = C(32, 16); c.box(4, 4, 25, 10, '#a8703a'); w = ramp('#a8703a')
    for x in (9, 14, 19): c.rect(x, 5, x, 9, w[1])
    thick(c, 26, 8, 31, 6, DARKWOOD)
    if load: c.ell(14.5, 4, 9.5, 2.6, load); speck(c, load, 5, 0.2)
    for cx in (8.5, 21.5):
        c.ell(cx, 11, 3.6, 3.6, DARKWOOD); c.ell(cx, 11, 2, 2, '#b98a55', flat=True); c.p(int(cx), 11, ICO['D']); c.p(int(cx) + 1, 11, ICO['D'])
    return fin(c, (15, 4, 26))

def fence_gate(opened=False):
    c = C(); c.box(1, 3, 2, 13, DARKWOOD); c.box(13, 3, 14, 13, DARKWOOD)
    if opened: c.box(3, 2, 4, 12, WOOD); c.box(4, 4, 6, 5, WOOD); c.box(4, 9, 6, 10, WOOD); c.p(12, 8, ramp(IRON)[2])
    else:
        for y in (5, 10): c.box(3, y, 12, y + 1, WOOD)
        c.line(3, 10, 12, 6, ramp(WOOD)[1]); c.line(3, 9, 12, 5, ramp(WOOD)[3]); px(c, ((11, 8), (12, 8)), ramp(IRON)[3])
    return fin(c, (14, 1, 14))

def mailbox(col='#c03c3c'):
    c = C(); c.box(7, 9, 8, 14, DARKWOOD); c.box(3, 4, 12, 9, col); c.clear(3, 4); c.clear(12, 4); c.rect(5, 6, 9, 6, (40, 30, 30))
    px(c, ((13, 4), (13, 5), (13, 6), (14, 4)), ICO['Y'])
    return fin(c, (14, 5, 10))

def bench(wood='#a8703a'):
    c = C(); c.box(2, 4, 13, 6, wood); c.box(2, 8, 13, 10, wood); w = ramp(wood)
    for x in (3, 12): c.rect(x, 7, x, 7, w[1]); c.box(x - (x > 8), 11, x + (x < 8), 13, DARKWOOD)
    for x in (6, 9): c.rect(x, 4, x, 6, w[1])
    return fin(c, (14, 2, 13))

def scarecrow():
    c = C(16, 32); c.box(7, 12, 8, 30, DARKWOOD); c.box(1, 13, 14, 14, DARKWOOD); c.box(4, 12, 11, 21, '#a85a3a')
    for x in (4, 6, 9, 11): c.rect(x, 22, x, 23, ICO['Y'])
    c.rect(4, 17, 11, 17, (90, 60, 40)); c.box(5, 14, 6, 15, '#4a8ac0'); c.ell(7.5, 7.5, 3.6, 3.6, '#d8c080'); px(c, ((6, 7), (9, 7)), ICO['D'])
    px(c, ((6, 9), (7, 10), (8, 10), (9, 9)), ICO['D']); c.tri(7.5, 0, 4, 3.2, '#7a5a2a'); c.box(2, 4, 13, 4, '#7a5a2a')
    px(c, ((0, 12), (0, 14), (0, 15), (15, 12), (15, 14), (15, 15), (1, 15), (14, 15)), ICO['Y'])
    return fin(c, (30, 4, 11))

def trough(fill='#3f78d8'):
    c = C(); c.box(1, 7, 14, 12, '#8a5a34'); c.rect(2, 8, 13, 9, hx(fill)); px(c, ((4, 8), (5, 8), (9, 9)), ramp(fill)[4])
    c.box(2, 13, 3, 13, DARKWOOD); c.box(12, 13, 13, 13, DARKWOOD)
    return fin(c, (14, 2, 13))

def woodpile():
    c = C()
    for cx, cy in ((3.5, 11.5), (7.5, 11.5), (11.5, 11.5), (5.5, 8), (9.5, 8), (7.5, 4.5)):
        c.ell(cx, cy, 2.4, 2.4, WOOD, flat=True); c.ell(cx, cy, 1.5, 1.5, '#d9b77e', flat=True); c.p(int(cx), int(cy), ramp('#d9b77e')[1])
    return fin(c, (14, 2, 13))

def part_village(A):
    A.section('props_village'); o = lambda n, c, **m: A.obj(n, c, cat='village', **m)
    for n, c in (('sign_wood', sign()), ('sign_blank', sign('none')), ('sign_post_e', sign_arrow()), ('sign_post_w', sign_arrow().flip()),
                 ('sign_inn', sign('inn', hang=True)), ('sign_smith', sign('smith', hang=True)), ('sign_potion', sign('potion', hang=True)),
                 ('sign_food', sign('food', hang=True)), ('well_stone', well()), ('well_roof', well('#b04a3a')), ('well_roof_blue', well('#4a6ab0')),
                 ('crate', crate()), ('crate_dark', crate('#6a4a34')), ('barrel', barrel()), ('barrel_water', barrel(top='#3f78d8')),
                 ('barrel_dark', barrel('#5a4038')), ('sack', sack()), ('sack_grain', sack(grain='#e0b040')), ('sack_flour', sack('#e8e0d0', '#f8f8f0')),
                 ('pot', pot()), ('pot_tall', pot(tall=True)), ('pot_blue', pot('#4a78b8', band='#f0e0a0')), ('pot_broken', pot(broken=True)),
                 ('chest_wood_closed', chest()), ('chest_wood_open', chest(opened=True)), ('chest_iron_closed', chest(IRON, '#4a5060')),
                 ('chest_iron_open', chest(IRON, '#4a5060', True)), ('chest_gold_closed', chest(GOLD, '#b06a18')), ('chest_gold_open', chest(GOLD, '#b06a18', True)),
                 ('torch_stand', torch()), ('campfire_lit', campfire()), ('campfire_out', campfire(False)), ('lamp_post', lamp_post(False)),
                 ('lamp_post_lit', lamp_post()), ('hay_bale', hay_bale()), ('cart', cart()), ('cart_hay', cart('#d8b84a')), ('cart_goods', cart('#a06a8a')),
                 ('fence_gate_closed', fence_gate()), ('mailbox', mailbox()), ('bench', bench()), ('scarecrow', scarecrow()),
                 ('trough_water', trough()), ('trough_feed', trough('#c8a040')), ('woodpile', woodpile())):
        o(n, c, solid=1)
    o('torch_wall', torch(True), solid=0); o('fence_gate_open', fence_gate(True), solid=0)

# ------------------------------------------------------------------ monuments (2c)
MARBLE = '#d8d8e0'; DARKSTONE = '#4a4658'; MOSS = '#5fa04a'; RUNE = (120, 230, 250)

def grave(kind='stone', col=STONE, moss=False):
    c = C(); r = ramp(col)
    if kind == 'cross': c.box(7, 2, 8, 13, col); c.box(4, 5, 11, 6, col)
    elif kind == 'broken':
        c.box(4, 8, 11, 13, col)
        for x, y in ((4, 8), (5, 8), (6, 8), (4, 9), (10, 8), (11, 8), (11, 9), (8, 8)): c.clear(x, y)
        c.line(7, 9, 6, 12, r[0]); c.box(12, 12, 14, 13, col); c.p(1, 13, r[2]); c.p(2, 13, r[1])
    else:
        c.box(4, 4, 11, 13, col); c.clear(4, 4); c.clear(11, 4); c.rect(5, 3, 10, 3, r[3])
        for y, x1 in ((6, 9), (8, 8), (10, 9)): c.rect(6, y, x1, y, r[0])
    if moss: cap(c, MOSS, 2, 4); px(c, ((4, 11), (5, 12), (4, 12)), ramp(MOSS)[2])
    px(c, [(x, 14) for x in range(3, 13)], ramp('#7a5a3a')[2]); px(c, ((5, 14), (9, 14)), ramp('#7a5a3a')[1])
    return fin(c, (15, 3, 12))

def pedestal(c, col, y=23):
    c.box(4, y, 11, y + 1, col); c.box(3, y + 2, 12, y + 6, col); c.rect(4, y + 4, 11, y + 4, ramp(col)[1])

def statue(kind, col=STONE):
    c = C(16, 32); r = ramp(col); pedestal(c, col)
    if kind == 'broken':
        c.box(5, 17, 7, 22, col); c.box(8, 19, 10, 22, col); c.p(5, 17, r[0]); c.clear(7, 17); c.ell(13, 28.5, 2, 1.6, col); px(c, ((1, 29), (2, 29)), r[2])
        return fin(c, (30, 2, 13))
    wide = kind in ('king', 'guardian')
    c.box(4 if wide else 5, 11, 11 if wide else 10, 22, col); c.rect(7, 17, 8, 22, r[1]); c.ell(7.5, 7.5, 2.7, 2.9, col)
    c.box(3, 12, 4, 17, col); c.box(11, 12, 12, 17, col); px(c, ((6, 7), (9, 7)), r[0])
    if kind == 'knight':
        c.rect(7, 11, 8, 21, r[4]); c.rect(5, 13, 10, 13, r[4]); c.rect(5, 6, 10, 6, r[1]); px(c, ((7, 3), (8, 3), (8, 4), (7, 4)), r[3])
    elif kind == 'mage':
        c.tri(7.5, 1, 5, 4, col); c.box(13, 4, 13, 22, col); c.ell(13, 3, 1.5, 1.5, RUNE, flat=True); c.rect(5, 20, 10, 22, r[3])
    elif kind == 'king':
        px(c, ((5, 3), (7, 3), (8, 3), (10, 3), (7, 2), (8, 2)), r[4]); c.rect(5, 4, 10, 4, r[4]); c.p(7, 1, hx(GOLD)); c.p(8, 1, hx(GOLD)); c.rect(4, 11, 11, 12, r[3]); c.box(13, 9, 13, 22, col); c.p(13, 8, r[4])
    else:
        c.ell(4.5, 16.5, 3, 4, col); c.p(4, 16, r[4]); c.p(5, 17, r[0]); c.rect(5, 5, 10, 5, r[1]); c.box(12, 5, 12, 22, col); px(c, ((12, 3), (12, 4), (11, 4), (13, 4)), r[4])
    return fin(c, (30, 2, 13))

def pillar(col=STONE, broken=False, vine=False):
    c = C(16, 32); r = ramp(col); c.box(3, 27, 12, 29, col); c.box(4, 25, 11, 26, col); top = 15 if broken else 6
    c.box(5, top, 10, 24, col)
    for x in (7, 9): c.rect(x, top + 1, x, 24, r[1])
    if broken:
        for x, y in ((5, 15), (6, 15), (5, 16), (10, 15), (8, 15)): c.clear(x, y)
        c.ell(13, 28, 2, 1.8, col); c.box(0, 28, 2, 29, col)
    else: c.box(4, 4, 11, 5, col); c.box(3, 2, 12, 3, col)
    if vine:
        v = ramp(MOSS)
        for i, y in enumerate(range(4, 28)): x = 5 + (i * 2 % 10 if (i // 5) % 2 == 0 else 9 - i * 2 % 10) % 6; c.p(x, y, v[2 + i % 2]); c.p(x + 1, y, v[1]) if i % 3 == 0 else None
    return fin(c, (30, 2, 13))

def obelisk(col=DARKSTONE, rune=RUNE):
    c = C(16, 32); c.box(3, 26, 12, 29, col); c.box(5, 7, 10, 25, col); c.tri(7.5, 2, 6, 2.5, col)
    for x, y in ((7, 10), (8, 10), (7, 12), (8, 14), (7, 15), (8, 15), (7, 18), (8, 19), (7, 21), (8, 22)): c.p(x, y, rune)
    return fin(c, (30, 2, 13))

def fountain(col=STONE, water='#3f8ae0'):
    c = C(32, 32); w = ramp(water); c.ell(15.5, 22, 13.5, 7.5, col); c.ell(15.5, 20.5, 11, 5, water, flat=True)
    for x, y in ((8, 20), (11, 23), (21, 22), (24, 19), (18, 24), (13, 18)): c.p(x, y, w[4]); c.p(x + 1, y, w[3])
    c.box(14, 11, 17, 21, col); c.ell(15.5, 10, 6, 2.6, col); c.ell(15.5, 9.4, 4.4, 1.4, water, flat=True); c.box(15, 4, 16, 8, col)
    for x, y in ((15, 3), (16, 2), (14, 4), (17, 4), (13, 6), (18, 6), (10, 12), (21, 12), (9, 15), (22, 15), (12, 9), (19, 9)): c.p(x, y, w[4])
    for x, y in ((10, 13), (21, 13), (9, 16), (22, 16), (9, 17), (22, 17)): c.p(x, y, w[3])
    return fin(c, (30, 4, 27))

def shrine(roof='#b04a3a', col=STONE):
    c = C(); c.box(3, 13, 12, 14, col); c.box(4, 7, 11, 12, col); c.rect(6, 9, 9, 12, (40, 32, 44)); c.p(7, 11, FIRE[0]); c.p(8, 12, FIRE[1]); c.p(7, 12, ICO['W'])
    c.tri(7.5, 2, 6, 7, roof); c.rect(7, 1, 8, 1, GOLD and hx(GOLD))
    return fin(c, (15, 2, 13))

def menhir(col=STONE, rune=None, moss=False):
    c = C(); c.ell(7.5, 8, 3.2, 6.2, col); c.box(5, 9, 10, 13, col); r = ramp(col); c.line(9, 4, 8, 9, r[1])
    if rune: px(c, ((6, 6), (7, 6), (7, 7), (6, 9), (7, 10), (6, 10)), rune)
    if moss: cap(c, MOSS, 2, 2)
    return fin(c, (14, 3, 12))

def stone_circle(col=STONE):
    c = C(32, 32)
    for i, (x, y) in enumerate(((13, 0), (2, 6), (23, 6), (0, 16), (26, 16), (6, 22), (20, 22))):
        m = C(8, 10); m.ell(3, 4, 2.6, 4.2, col); m.box(1, 5, 5, 8, col); c.blit(m, x, y)
    c.ell(15.5, 17, 4, 2.4, DARKSTONE, flat=True); px(c, ((14, 17), (16, 16), (17, 18)), RUNE)
    return fin(c)

def rubble(col=STONE, seed=0):
    c = C()
    for i, (x, y, rx, ry) in enumerate(((5, 11, 3.4, 2.6), (10.5, 11.5, 3, 2.2), (8, 8.5, 2.6, 2), (13, 13, 1.4, 1), (2, 13, 1.2, 0.9))):
        c.ell(x + ri(i, 0, seed, 2), y, rx, ry, col)
    c.box(4, 6, 6, 7, col)
    return fin(c, (14, 2, 13))

def part_monument(A):
    A.section('props_monument'); o = lambda n, c, **m: A.obj(n, c, cat='monument', **m)
    for n, c in (('grave_stone', grave()), ('grave_cross', grave('cross')), ('grave_broken', grave('broken')), ('grave_mossy', grave(moss=True)),
                 ('grave_dark', grave(col=DARKSTONE)), ('grave_cross_wood', grave('cross', WOOD)),
                 ('statue_knight', statue('knight')), ('statue_mage', statue('mage')), ('statue_king', statue('king', MARBLE)),
                 ('statue_guardian', statue('guardian')), ('statue_broken', statue('broken')), ('statue_dark', statue('knight', DARKSTONE)),
                 ('pillar_stone', pillar()), ('pillar_marble', pillar(MARBLE)), ('pillar_broken', pillar(broken=True)), ('pillar_dark', pillar(DARKSTONE)),
                 ('pillar_vine', pillar(vine=True)), ('pillar_sand', pillar('#c8a070')), ('obelisk', obelisk()), ('obelisk_sand', obelisk('#c8a070', (90, 60, 40))),
                 ('obelisk_ember', obelisk('#3a3038', (250, 140, 50))), ('fountain', fountain()), ('fountain_marble', fountain(MARBLE)),
                 ('fountain_dry', fountain(STONE, '#6a6d78')), ('shrine', shrine()), ('shrine_blue', shrine('#4a6ab0', MARBLE)), ('shrine_moss', shrine('#4a8a4a', '#7a8a78')),
                 ('standing_stone', menhir()), ('standing_stone_rune', menhir(DARKSTONE, RUNE)), ('standing_stone_mossy', menhir(moss=True)),
                 ('stone_circle', stone_circle()), ('rubble', rubble()), ('rubble_dark', rubble(DARKSTONE, 1)), ('rubble_marble', rubble(MARBLE, 2)),
                 ('rubble_sand', rubble('#c8a070', 3))):
        o(n, c, solid=1)

PARTS = [part_nature, part_village, part_monument]

def build(A):
    for p in PARTS: p(A)
