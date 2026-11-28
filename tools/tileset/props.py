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

# ------------------------------------------------------------------ doors, stairs, windows (2d)
DARKIN = (30, 24, 36)

def door(col=WOOD, trim=IRON, d='s', opened=False):
    """s/n = front view (n = inside face with crossbar), e = thin side view on the east edge, w = flip of e"""
    if d == 'w': return door(col, trim, 'e', opened).flip()
    c = C(); r = ramp(col); t = ramp(trim)
    if d == 'e':
        c.box(11, 1, 13, 15, STONE)
        if opened: c.rect(12, 3, 12, 15, DARKIN); c.box(3, 4, 10, 6, col); c.box(3, 6, 10, 15, col); c.rect(3, 9, 10, 9, t[1]); c.p(4, 11, hx(GOLD))
        else: c.box(11, 3, 13, 15, col); c.rect(11, 6, 13, 6, t[2]); c.rect(11, 12, 13, 12, t[2]); c.p(10, 10, hx(GOLD))
        return fin(c)
    c.box(2, 1, 13, 15, STONE); c.clear(2, 1); c.clear(13, 1)
    if opened: c.rect(4, 3, 11, 15, DARKIN); c.box(4, 3, 5, 15, col); c.rect(6, 13, 11, 15, (58, 48, 60)); c.p(5, 10, hx(GOLD))
    else:
        c.box(4, 3, 11, 15, col)
        for x in (6, 9): c.rect(x, 4, x, 15, r[1])
        if d == 'n': c.box(4, 8, 11, 9, trim)
        else: c.rect(4, 5, 11, 5, t[2]); c.rect(4, 12, 11, 12, t[2]); c.box(9, 8, 10, 9, GOLD)
    return fin(c)

def stairs(col=STONE, up=True, d='n'):
    """full-tile floor piece; d = the side you walk towards (up: higher side, down: deeper side)"""
    c = C(); r = ramp(col); c.rect(0, 0, 15, 15, r[1])
    for i in range(5):
        y = 13 - i * 3; k = (i if up else 4 - i)
        top = r[min(4, 1 + k)] if up else r[max(0, 3 - i)]
        c.rect(1, y, 14, y + 1, top); c.rect(1, y + 2, 14, y + 2, r[0] if up else DARKIN)
        if not up and i == 4: c.rect(1, 0, 14, 2, DARKIN)
    c.rect(0, 0, 0, 15, r[3] if up else r[0]); c.rect(15, 0, 15, 15, r[0])
    return c.rot({'n': 0, 'w': 1, 's': 2, 'e': 3}[d])

def ladder(up=True, wood='#a8703a'):
    c = C(); w = ramp(wood)
    if not up: c.box(2, 2, 13, 13, STONE); c.rect(4, 4, 11, 11, DARKIN)
    y0 = 0 if up else 3
    for x in (4, 10): c.box(x, y0, x + 1, 15 if up else 10, wood)
    for y in range(y0 + 2, 15 if up else 10, 3): c.rect(6, y, 9, y, w[3]); c.rect(6, y + 1, 9, y + 1, w[1])
    return fin(c)

def trapdoor(opened=False, wood='#8a5a34'):
    c = C(); c.box(2, 2, 13, 13, wood); w = ramp(wood)
    if opened: c.rect(3, 5, 12, 12, DARKIN); c.box(2, 2, 13, 4, wood); px(c, ((5, 7), (5, 9), (5, 11), (9, 7), (9, 9), (9, 11)), w[2]); c.rect(6, 8, 8, 8, w[1]); c.rect(6, 10, 8, 10, w[1])
    else:
        for x in (5, 8, 11): c.rect(x, 3, x, 12, w[1])
        c.rect(2, 4, 13, 4, ramp(IRON)[2]); c.rect(2, 11, 13, 11, ramp(IRON)[2]); c.box(7, 7, 8, 8, IRON)
    return fin(c)

def window(frame=WOOD, kind='glass'):
    c = C(); c.box(3, 3, 12, 12, frame); g = {'glass': (130, 190, 230), 'lit': (250, 220, 110), 'dark': (50, 56, 84)}.get(kind, (130, 190, 230))
    c.rect(4, 4, 11, 11, g)
    if kind == 'stained':
        for i, col in enumerate(((216, 60, 70), (240, 200, 60), (80, 130, 230), (70, 160, 80))): c.rect(4 + i % 2 * 4, 4 + i // 2 * 4, 7 + i % 2 * 4, 7 + i // 2 * 4, col)
        c.ell(7.5, 7.5, 1.6, 1.6, '#f0f0f8', flat=True)
    else: px(c, ((5, 5), (6, 5), (5, 6), (9, 9)), (240, 250, 255) if kind != 'dark' else (90, 100, 140))
    f = ramp(frame)
    if kind == 'barred':
        for x in (5, 7, 9, 11): c.rect(x, 4, x, 11, ramp(IRON)[0])
    else: c.rect(7, 4, 8, 11, f[2]); c.rect(4, 7, 11, 8, f[2])
    c.box(2, 12, 13, 13, frame)
    return fin(c)

def archway(col=STONE, vine=False):
    c = C(32, 32); r = ramp(col); c.box(1, 6, 30, 31, col); c.ell(15.5, 8, 14.5, 7, col); speck(c, col, 2, 0.08)
    for y in range(10, 32):
        for x in range(8, 24):
            if y >= 18 or ((x - 15.5) / 8) ** 2 + ((y - 18) / 8) ** 2 <= 1: c.clear(x, y)
    for y in (12, 18, 24): c.rect(1, y, 7, y, r[1]); c.rect(24, y, 30, y, r[1])
    c.box(13, 2, 18, 6, col); c.p(15, 4, r[4]); c.p(16, 4, r[4])
    if vine: cap(c, MOSS, 3, 5, 12)
    return fin(c)

def part_doors(A):
    A.section('props_doors'); o = lambda n, c, **m: A.obj(n, c, cat='door', **m)
    for mat, col, trim in (('wood', WOOD, IRON), ('iron', IRON, '#4a5060'), ('gold', GOLD, '#b06a18'), ('dark', '#4a3848', '#8a3a4a')):
        for st in ('closed', 'open'):
            for d in 'nesw': o('door_%s_%s_%s' % (mat, st, d), door(col, trim, d, st == 'open'), solid=int(st == 'closed'))
    for mat, col in (('stone', STONE), ('wood', '#a8703a')):
        for ud in ('up', 'down'):
            for d in 'nesw': o('stairs_%s_%s_%s' % (mat, ud, d), stairs(col, ud == 'up', d), solid=0)
    for n, c in (('ladder_up', ladder()), ('ladder_down', ladder(False)), ('trapdoor', trapdoor()), ('trapdoor_open', trapdoor(True)),
                 ('window_wood', window()), ('window_stone', window(STONE)), ('window_lit', window(WOOD, 'lit')), ('window_dark', window(STONE, 'dark')),
                 ('window_barred', window(STONE, 'barred')), ('window_stained', window(STONE, 'stained'))): o(n, c, solid=0)
    o('archway', archway(), solid=0); o('archway_vine', archway(vine=True), solid=0); o('archway_dark', archway(DARKSTONE), solid=0)

# ------------------------------------------------------------------ furniture (task 2e)
LINEN = '#e8e4d8'; RED = '#b83a3a'; BLUE = '#3a62b8'; GREEN = '#3f8a4a'; PURPLE = '#7a4aa8'; LIGHTWOOD = '#a8703a'
INK = (40, 28, 30)

def bed(col=RED, double=False, wood=WOOD):
    w = 32 if double else 16; c = C(w, 32); x1 = w - 3; r = ramp(col)
    c.box(2, 3, x1, 28, wood); c.rect(3, 6, x1 - 1, 27, ramp(LINEN)[2]); c.box(2, 1, x1, 5, wood)
    for x in ((4,), (4, 18))[double]: c.box(x, 7, x + (9 if double else 7), 11, LINEN)
    c.box(3, 13, x1 - 1, 27, col); c.rect(3, 13, x1 - 1, 14, ramp(LINEN)[3]); c.rect(3, 15, x1 - 1, 15, r[4])
    for x in range(5, x1 - 1, 4): c.p(x, 18 + (x % 3), r[1]); c.p(x + 1, 23 - (x % 2), r[3])
    c.box(2, 27, x1, 29, wood)
    return fin(c, (30, 3, x1 - 1))

def table(wood=LIGHTWOOD, cloth=None):
    c = C(32, 16); r = ramp(wood)
    for x in (2, 27): c.box(x, 9, x + 2, 13, wood)
    c.rect(2, 10, 29, 11, r[0]); c.box(1, 3, 30, 9, wood)
    for x in (8, 16, 24): c.rect(x, 4, x, 8, r[1])
    if cloth: c.box(9, 3, 22, 10, cloth); c.rect(10, 5, 21, 5, ramp(cloth)[4]); c.rect(10, 8, 21, 8, ramp(cloth)[4])
    return fin(c, (14, 2, 29))

def table_small(wood=LIGHTWOOD, item=None, rnd_=False):
    c = C(); r = ramp(wood)
    if rnd_: c.box(6, 9, 9, 13, wood); c.rect(4, 13, 11, 13, r[1]); c.ell(7.5, 6.5, 6.5, 4, wood); c.ell(7.5, 6, 5, 2.6, r[3:] + r[2:5], flat=True) if 0 else None
    else:
        for x in (2, 12): c.box(x, 9, x + 1, 13, wood)
        c.box(1, 3, 14, 9, wood); c.rect(2, 10, 13, 10, r[0])
    if item == 'mug': c.box(6, 3, 8, 6, '#c8a060'); c.p(9, 4, ramp('#c8a060')[1]); c.rect(6, 3, 8, 3, ICO['W'])
    if item == 'candle': c.rect(7, 3, 8, 6, ICO['W']); c.p(7, 2, FIRE[1]); c.p(7, 1, FIRE[0])
    if item == 'book': c.box(5, 4, 10, 7, BLUE); c.rect(6, 5, 9, 5, ICO['W'])
    if item == 'orb': c.ell(7.5, 4, 2.6, 2.6, '#8ad0f0'); c.rect(6, 7, 9, 7, ramp(GOLD)[2])
    return fin(c, (14, 2, 13))

def chair(d='s', wood=LIGHTWOOD, seat=None):
    c = C(); r = ramp(wood); st = seat or wood
    if d in 'ew':
        c.box(4, 2, 5, 13, wood); c.box(10, 10, 11, 13, wood); c.box(4, 8, 11, 10, st)
        return fin(c, (14, 3, 12)) if d == 'e' else fin(c, (14, 3, 12)).flip()
    for x in (4, 10): c.box(x, 11, x + 1, 13, wood)
    if d == 's': c.box(4, 2, 11, 8, wood); c.rect(6, 4, 6, 7, r[0]); c.rect(9, 4, 9, 7, r[0]); c.box(4, 8, 11, 11, st)
    else: c.box(4, 7, 11, 10, st); c.box(4, 3, 11, 11, wood); c.rect(6, 5, 6, 9, r[1]); c.rect(9, 5, 9, 9, r[1]); c.rect(4, 3, 11, 3, r[4])
    return fin(c, (14, 3, 12))

def stool(wood=LIGHTWOOD):
    c = C(); thick(c, 5, 9, 4, 13, wood); thick(c, 9, 9, 10, 13, wood); c.ell(7.5, 8, 4.4, 2.4, wood)
    return fin(c, (14, 4, 11))

def shelf(kind='jars', wood=WOOD):
    c = C(); r = ramp(wood); c.rect(1, 2, 14, 13, INK); c.box(1, 1, 14, 2, wood)
    for y in (7, 13): c.rect(1, y, 14, y, r[3]); c.p(1, y, r[1]); c.p(14, y, r[1])
    c.rect(1, 2, 1, 13, r[2]); c.rect(14, 2, 14, 13, r[1])
    cols = (RED, BLUE, GREEN, GOLD, PURPLE)
    for k, y in enumerate((6, 12)):
        for i, x in enumerate(range(3, 13, 3 if kind != 'plates' else 4)):
            b = ramp(cols[(i + k * 2) % 5])
            if kind == 'jars': c.rect(x, y - 2, x + 1, y, b[2]); c.p(x, y - 2, b[4]); c.p(x, y - 3, ICO['W'])
            elif kind == 'plates': c.ell(x + 0.5, y - 1.5, 1.6, 1.6, LINEN)
            else: c.rect(x, y - 3 + (i % 2), x, y, b[2]); c.rect(x + 1, y - 2, x + 1, y, b[3])
    return fin(c)

def bookshelf(wood=WOOD, kind='books', seed=0):
    c = C(16, 32); c.box(1, 2, 14, 29, wood); cols = (RED, BLUE, GREEN, GOLD, PURPLE, '#c8b080')
    for y in (4, 10, 16, 22):
        c.rect(3, y, 12, y + 4, INK)
        for x in range(3, 13):
            if kind == 'empty' or rnd(x, y, seed) < 0.1: continue
            b = ramp(cols[ri(x, y, seed, 6)])
            if kind == 'potions':
                if x % 3 == 2: continue
                c.rect(x, y + 2, x, y + 4, b[2 + x % 2]); c.p(x, y + 1, ICO['W']) if x % 3 == 0 else None
            else: h = 3 + ri(x, y, seed + 1, 2); c.rect(x, y + 5 - h, x, y + 4, b[1 + x % 2 * 2]); c.p(x, y + 6 - h, b[4])
    if kind == 'empty': thick(c, 4, 17, 8, 20, LINEN, 1); c.p(10, 26, ramp(GOLD)[2])
    c.rect(2, 27, 13, 28, ramp(wood)[1])
    return fin(c, (30, 2, 13))

def counter(piece='m', wood=LIGHTWOOD, top='#d8c8a8'):
    c = C(); x0 = 2 if piece != 'm' else 0; r = ramp(wood); t = ramp(top)
    c.rect(x0, 7, 15, 13, r[2]); c.rect(x0, 13, 15, 13, r[1]); c.rect(x0, 8, 15, 8, r[0])
    for x in range(x0 + 2, 16, 4): c.rect(x, 9, x, 12, r[1])
    c.rect(x0, 3, 15, 6, t[3]); c.rect(x0, 3, 15, 3, t[4]); c.rect(x0, 7, 15, 7, t[1])
    if piece != 'm': c.rect(x0, 4, x0, 13, r[3]); c.p(x0, 3, t[4])
    c = fin(c, (14, x0, 15))
    return c.flip() if piece == 'r' else c

def stove():
    c = C(); i = ramp(IRON); c.box(10, 0, 12, 4, '#5a5f6c'); c.box(2, 4, 13, 13, '#5a5f6c'); c.rect(2, 4, 13, 6, i[2]); c.rect(2, 4, 13, 4, i[4])
    c.rect(4, 5, 6, 5, i[0]); c.rect(8, 5, 9, 5, i[0]); c.rect(4, 9, 11, 12, INK); c.rect(5, 10, 10, 12, FIRE[2]); px(c, ((6, 11), (8, 11), (9, 12), (7, 12)), FIRE[1]); c.p(7, 11, FIRE[0])
    for x in (6, 9): c.rect(x, 9, x, 12, INK)
    return fin(c, (14, 2, 13))

def fireplace(lit=True, col=STONE):
    c = C(); r = ramp(col); c.box(1, 3, 14, 13, col); c.box(0, 1, 15, 3, col)
    px(c, ((2, 6), (3, 6), (12, 9), (13, 9), (2, 11), (12, 5), (3, 9)), r[1]); c.rect(4, 6, 11, 13, INK); px(c, ((4, 6), (11, 6)), r[1])
    thick(c, 5, 13, 10, 12, DARKWOOD, 1); c.rect(5, 13, 10, 13, ramp(DARKWOOD)[2])
    if lit: flame(c, 7, 12, 6); px(c, ((5, 12), (10, 12)), FIRE[2])
    fin(c, (14, 1, 14))
    if lit: glow(c, 7.5, 14, 3.2)
    return c

def throne(frame=GOLD, cush=RED):
    c = C(16, 32); f = ramp(frame)
    c.box(3, 3, 12, 22, frame); c.tri(7.5, 0, 3, 3, frame); px(c, ((3, 2), (12, 2), (3, 1), (12, 1)), f[3]); c.box(5, 6, 10, 18, cush); c.p(7, 2, ICO['R']); c.p(8, 2, ICO['R'])
    c.box(2, 18, 13, 24, cush); c.rect(2, 23, 13, 24, f[1]); c.box(1, 15, 3, 25, frame); c.box(12, 15, 14, 25, frame); c.box(2, 25, 13, 28, frame)
    return fin(c, (29, 2, 13))

def altar(col=STONE, cloth=LINEN, trim=GOLD):
    c = C(); c.box(2, 7, 13, 13, col); c.box(1, 5, 14, 7, cloth); c.rect(6, 5, 9, 10, ramp(cloth)[2]); c.rect(6, 10, 9, 10, ramp(trim)[2]); px(c, ((7, 7), (8, 7), (7, 8), (8, 8)), ramp(trim)[3])
    for x in (3, 12): c.rect(x, 2, x, 4, ICO['W']); c.p(x, 1, FIRE[1]); c.p(x, 0, FIRE[0])
    return fin(c, (14, 2, 13))

def anvil():
    c = C(); i = ramp('#5a6070'); c.rect(3, 12, 12, 13, i[1]); c.rect(5, 9, 10, 11, i[2]); c.rect(3, 5, 13, 8, i[2]); c.rect(3, 5, 13, 5, i[4]); c.rect(0, 6, 2, 7, i[3]); c.p(0, 7, None)
    c.clear(0, 7); c.rect(3, 8, 13, 8, i[0]); c.rect(6, 9, 6, 11, i[3]); c.rect(3, 12, 12, 12, i[3])
    return fin(c, (14, 3, 12))

def forge(lit=True):
    c = C(); r = ramp(STONE); c.box(1, 5, 14, 13, STONE); c.box(4, 0, 11, 3, DARKSTONE); c.rect(5, 4, 10, 4, INK)
    c.rect(3, 6, 12, 9, INK); px(c, ((2, 11), (6, 12), (11, 11), (13, 12), (8, 11)), r[1])
    for x in range(3, 13): c.p(x, 9 - (x % 2), FIRE[2] if lit else (70, 60, 66)); c.p(x, 9, FIRE[1] if lit and x % 3 else (90, 40, 40))
    if lit: px(c, ((5, 7), (8, 7), (10, 8)), FIRE[0])
    fin(c, (14, 1, 14))
    if lit: glow(c, 7.5, 4.5, 2.5)
    return c

def cauldron(liq='#58c048'):
    c = C(); b = '#3a3d48'; c.rect(4, 13, 5, 13, ramp(b)[1]); c.rect(10, 13, 11, 13, ramp(b)[1]); c.ell(7.5, 9, 5.6, 4.4, b); c.box(2, 4, 13, 6, b)
    c.rect(3, 5, 12, 5, ramp(liq)[2]); c.rect(4, 4, 11, 4, ramp(liq)[3]); px(c, ((5, 4), (9, 5)), ramp(liq)[4]); px(c, ((6, 2), (10, 1)), ramp(liq)[4])
    return fin(c, (14, 3, 12))

def loom(col=BLUE):
    c = C(); r = ramp(col)
    for x in range(4, 12): c.rect(x, 3, x, 12, ICO['W'] if x % 2 else (190, 186, 176))
    c.rect(4, 8, 11, 12, r[2]); px(c, [(x, y) for x in range(4, 12) for y in range(8, 13) if (x + y) % 3 == 0], r[4]); c.rect(4, 8, 11, 8, r[1])
    c.box(2, 1, 3, 13, WOOD); c.box(12, 1, 13, 13, WOOD); c.box(1, 1, 14, 2, WOOD); c.box(1, 12, 14, 13, WOOD)
    return fin(c, (14, 1, 14))

def rug(col=RED, rnd_=True, trim=GOLD):
    w = 16 if rnd_ else 32; c = C(w, w); r = ramp(col); t = ramp(trim)
    if rnd_: c.ell(7.5, 7.5, 7.4, 6.4, r[1:2] * 5, flat=True); c.ell(7.5, 7.5, 6.2, 5.2, t[2:3] * 5, flat=True); c.ell(7.5, 7.5, 5.2, 4.2, r[2:3] * 5, flat=True); c.ell(7.5, 7.5, 2.2, 1.6, t[3:4] * 5, flat=True)
    else:
        c.rect(2, 3, 29, 28, r[1]); c.rect(3, 4, 28, 27, t[2]); c.rect(4, 5, 27, 26, r[2]); c.rect(7, 8, 24, 23, r[3]); c.rect(8, 9, 23, 22, r[2])
        for k in range(6): c.rect(15 - k, 10 + k, 16 + k, 10 + k, t[3]); c.rect(15 - k, 21 - k, 16 + k, 21 - k, t[3])
        c.rect(13, 14, 18, 17, r[1])
        for y in range(3, 29, 2): c.p(1, y, t[3]); c.p(30, y, t[3])
    return c

def wardrobe(wood=WOOD):
    c = C(16, 32); r = ramp(wood); c.box(1, 4, 14, 29, wood); c.box(0, 2, 15, 4, wood)
    for x in (2, 8): c.box(x, 6, x + 5, 25, r[1:] + r[4:]); c.rect(x + 1, 8, x + 4, 14, r[1]); c.rect(x + 1, 17, x + 4, 23, r[1])
    px(c, ((6, 16), (9, 16)), ramp(GOLD)[3]); c.rect(1, 27, 14, 27, r[0])
    return fin(c, (30, 1, 14))

def mirror(tall=False, frame=GOLD):
    c = C(16, 32 if tall else 16); g = '#a8d8f0'; G = ramp(g)
    if tall:
        c.box(4, 27, 11, 29, frame); c.box(7, 24, 8, 27, frame); c.ell(7.5, 13, 5.6, 11, frame); c.ell(7.5, 13, 4.2, 9.6, G[2:3] * 5, flat=True)
        for k in range(5): c.p(5 + k, 12 - k, G[4]); c.p(7 + k, 17 - k, G[4]); c.p(6 + k, 20 - k, G[3])
        return fin(c, (30, 3, 12))
    c.ell(7.5, 7.5, 5.6, 6.6, frame); c.ell(7.5, 7.5, 4.2, 5.2, G[2:3] * 5, flat=True)
    for k in range(4): c.p(5 + k, 8 - k, G[4]); c.p(7 + k, 10 - k, G[4])
    return fin(c)

def barrel_rack():
    c = C(32, 16); w = '#9a6238'; r = ramp(w)
    c.box(1, 12, 30, 13, DARKWOOD)
    for cx in (6.5, 15.5, 24.5):
        c.ell(cx, 7, 4.6, 4.6, w); c.ell(cx, 7, 3, 3, r[3:4] * 5, flat=True); c.ell(cx, 7, 1.2, 1.2, r[1:2] * 5, flat=True); c.p(int(cx + 0.5), 10, INK)
        px(c, ((int(cx - 4), 12), (int(cx + 5), 12)), ramp(DARKWOOD)[4])
    return fin(c, (14, 2, 29))

def weapon_rack(kind='swords'):
    c = C(); i = ramp(IRON); g = ramp(GOLD); lw = ramp(LIGHTWOOD)
    c.box(0, 11, 15, 13, WOOD); c.box(0, 5, 1, 13, WOOD); c.box(14, 5, 15, 13, WOOD); c.rect(2, 6, 13, 6, ramp(WOOD)[1])
    for k, x in enumerate((3, 7, 11)):
        if kind == 'swords' or (kind == 'mixed' and k == 0):
            c.rect(x, 1, x + 1, 8, i[3]); c.rect(x + 1, 1, x + 1, 8, i[1]); c.p(x, 0, i[4]); c.rect(x - 1, 8, x + 2, 8, g[2]); c.rect(x, 9, x + 1, 10, (60, 40, 36))
        elif kind == 'spears' or k == 1:
            c.rect(x, 3, x + 1, 10, lw[2]); c.rect(x + 1, 3, x + 1, 10, lw[1]); c.rect(x, 0, x + 1, 2, i[4]); c.p(x - 1, 2, i[2]); c.p(x + 2, 2, i[2]); c.p(x + 1, 0, i[2])
        else:
            c.rect(x, 2, x, 10, lw[2]); c.rect(x - 1, 1, x + 2, 4, i[3]); c.rect(x + 2, 0, x + 2, 5, i[4]); c.rect(x - 1, 4, x + 2, 4, i[1])
    return fin(c, (14, 1, 14))

def plant_pot(leaf='#3f9a3f', flower=None):
    c = C(); c.box(5, 9, 10, 13, '#b8683a'); c.rect(5, 9, 10, 9, ramp('#b8683a')[4]); c.ell(7.5, 5.5, 4.6, 4, leaf); speck(c, leaf, 3, 0.2)
    if flower: px(c, ((5, 4), (9, 3), (7, 6), (10, 6)), ramp(flower)[3])
    return fin(c, (14, 4, 11))

def bathtub():
    c = C(32, 16); c.box(2, 5, 29, 12, LINEN); c.rect(4, 6, 27, 9, ramp('#58a8e0')[2]); px(c, ((7, 7), (8, 7), (15, 8), (16, 8), (22, 7), (23, 7)), ramp('#58a8e0')[4])
    c.rect(2, 5, 29, 5, ICO['W']); c.rect(4, 13, 5, 13, ramp(GOLD)[2]); c.rect(26, 13, 27, 13, ramp(GOLD)[2]); c.rect(28, 2, 28, 4, ramp(GOLD)[3]); c.p(27, 2, ramp(GOLD)[3])
    return fin(c, (14, 3, 28))

def part_furniture(A):
    A.section('props_furniture'); o = lambda n, c, s=1: A.obj(n, c, cat='furniture', solid=s)
    for n, col in (('red', RED), ('blue', BLUE), ('green', GREEN), ('purple', PURPLE), ('white', '#d0ccc0')):
        o('bed_single_' + n, bed(col)); o('bed_double_' + n, bed(col, True))
    o('table', table()); o('table_cloth', table(cloth=LINEN)); o('table_dark', table(DARKWOOD)); o('table_feast', table(cloth=RED))
    o('table_round', table_small(rnd_=True)); o('table_small', table_small())
    for it in ('mug', 'candle', 'book', 'orb'): o('table_small_' + it, table_small(item=it))
    for d in 'nesw': o('chair_' + d, chair(d)); o('chair_cushion_' + d, chair(d, DARKWOOD, RED))
    o('stool', stool()); o('stool_dark', stool(DARKWOOD))
    for k in ('jars', 'plates', 'books'): o('shelf_' + k, shelf(k))
    o('bookshelf', bookshelf()); o('bookshelf_b', bookshelf(seed=7)); o('bookshelf_dark', bookshelf(DARKWOOD, seed=3)); o('bookshelf_potions', bookshelf(kind='potions')); o('bookshelf_empty', bookshelf(kind='empty'))
    for mat, w, t in (('wood', LIGHTWOOD, '#d8c8a8'), ('stone', STONE, MARBLE), ('dark', DARKWOOD, '#8a6a4a')):
        for pc in 'lmr': o('counter_%s_%s' % (mat, pc), counter(pc, w, t))
    o('stove', stove()); o('fireplace_lit', fireplace()); o('fireplace_out', fireplace(False)); o('fireplace_dark_lit', fireplace(col=DARKSTONE))
    o('throne_gold', throne()); o('throne_stone', throne(STONE, BLUE)); o('throne_dark', throne(DARKSTONE, PURPLE)); o('throne_wood', throne(LIGHTWOOD, GREEN))
    o('altar', altar()); o('altar_dark', altar(DARKSTONE, PURPLE, '#c8c8d8')); o('altar_nature', altar(MOSS, '#e8dcc0', '#d86a8a'))
    o('anvil', anvil()); o('forge_lit', forge()); o('forge_out', forge(False))
    o('cauldron_green', cauldron()); o('cauldron_purple', cauldron('#a858d8')); o('cauldron_stew', cauldron('#c88a48')); o('cauldron_water', cauldron('#58a8e0'))
    o('loom', loom()); o('loom_red', loom(RED))
    for n, col in (('red', RED), ('blue', BLUE), ('green', GREEN), ('purple', PURPLE)): o('rug_round_' + n, rug(col), 0); o('rug_' + n, rug(col, False), 0)
    o('wardrobe', wardrobe()); o('wardrobe_dark', wardrobe(DARKWOOD)); o('mirror', mirror()); o('mirror_tall', mirror(True)); o('mirror_silver', mirror(frame='#b8bcc8'))
    o('barrel_rack', barrel_rack()); o('weapon_rack_swords', weapon_rack()); o('weapon_rack_spears', weapon_rack('spears')); o('weapon_rack_mixed', weapon_rack('mixed'))
    o('plant_pot', plant_pot()); o('plant_pot_flower', plant_pot(flower='#e85a8a')); o('bathtub', bathtub())

# ------------------------------------------------------------------ dungeon (task 2f)
BONE = '#e0d8c0'

def spikes(up=True):
    c = C(); i = ramp(IRON); c.box(1, 2, 14, 14, '#55525e'); c.rect(2, 3, 13, 13, ramp('#55525e')[1])
    for y in (4, 8, 12):
        for x in (3, 7, 11):
            c.rect(x, y, x + 1, y, INK)
            if up: c.rect(x, y - 2, x + 1, y - 1, i[3]); c.p(x + 1, y - 1, i[1]); c.p(x, y - 3, i[4])
    return c

def lever(right=False):
    c = C(); c.box(4, 10, 11, 13, STONE); c.rect(6, 10, 9, 10, INK); thick(c, 7, 10, 3, 4, IRON); c.ell(3.5, 3.5, 1.6, 1.6, RED)
    c = fin(c, (14, 4, 11))
    return c.flip() if right else c

def plate(down=False, col=STONE):
    c = C(); r = ramp(col); c.rect(2, 3, 13, 13, r[0])
    if down: c.box(3, 5, 12, 12, r[1:2] + r[:4]); c.rect(4, 6, 11, 11, r[1])
    else: c.box(3, 3, 12, 11, col); c.rect(3, 12, 12, 12, r[1]); c.rect(5, 5, 10, 9, r[3]); c.rect(6, 6, 9, 8, r[2])
    return c

def cage(kind='empty'):
    c = C(16, 32); i = ramp(IRON); c.box(1, 26, 14, 29, IRON); c.rect(2, 8, 13, 25, (20, 18, 28)); c.a[8:26, 2:14, 3] = 110
    if kind == 'bones': c.ell(7.5, 22, 2.4, 2, BONE); px(c, ((7, 22), (9, 22)), INK); c.rect(4, 25, 11, 25, ramp(BONE)[2])
    for x in (2, 5, 8, 11, 13): c.rect(x, 7, x, 25, i[3] if x % 2 else i[2])
    c.box(1, 5, 14, 7, IRON); c.box(3, 3, 12, 5, IRON); c.rect(7, 0, 8, 2, i[2]); c.rect(1, 16, 14, 16, i[1])
    return fin(c, (30, 2, 13))

def web(k=0):
    c = C(); w = (236, 236, 244)
    for x1, y1 in ((15, 0), (14, 6), (9, 10), (5, 14), (0, 15)): c.line(0, 0, x1, y1, w)
    for r_ in (5, 9, 13):
        pts = [(r_, 0), (int(r_ * .86), int(r_ * .36)), (int(r_ * .62), int(r_ * .62)), (int(r_ * .36), int(r_ * .86)), (0, r_)]
        for a, b in zip(pts, pts[1:]): c.line(a[0], a[1], b[0], b[1], w)
    c.a[:, :, 3] = np.where(c.a[:, :, 3] > 0, 200, 0)
    return c.rot(-k)

def bones(kind='pile'):
    c = C(); b = ramp(BONE)
    if kind == 'skull': c.ell(7.5, 8, 3.6, 3.2, BONE); c.rect(6, 11, 9, 12, b[2]); px(c, ((6, 8), (9, 8), (6, 9), (9, 9)), INK); c.p(7, 10, INK); px(c, ((6, 12), (8, 12)), b[0])
    elif kind == 'skulls':
        for cx, cy in ((4.5, 10), (10.5, 10), (7.5, 6)): c.ell(cx, cy, 3, 2.8, BONE); px(c, ((int(cx - 1), int(cy)), (int(cx + 2), int(cy))), INK); c.p(int(cx + .5), int(cy + 2), b[0])
    elif kind == 'ribs':
        c.rect(3, 6, 12, 6, b[3])
        for x in (4, 6, 8, 10): c.rect(x, 7, x, 11, b[2]); c.p(x, 12, b[1])
        c.ell(13, 6, 1.6, 1.6, BONE)
    else:
        thick(c, 3, 11, 11, 7, BONE); thick(c, 4, 7, 11, 12, BONE); px(c, ((2, 11), (3, 12), (12, 6), (12, 8), (3, 6), (4, 6), (12, 12), (12, 13)), b[4]); c.ell(7.5, 12, 2, 1.4, BONE)
    return fin(c, (14, 3, 12))

def chains(kind='hang'):
    c = C(); i = ramp(IRON)
    for x in ((4, 11) if kind != 'single' else (7,)):
        for y in range(0, 12, 2): c.p(x, y, i[3]); c.p(x + (y // 2) % 2 * 0 + 1, y + 1, i[1]) if 0 else c.p(x, y + 1, i[1])
        if kind == 'shackle': c.box(x - 1, 11, x + 1, 13, IRON); c.p(x, 12, INK)
        else: c.p(x, 12, i[4])
    return c.outline()

def portal(col='#58a8f0', frame=STONE, sz=32):
    c = C(16, sz); r = ramp(col); cy = sz / 2 + (1 if sz == 32 else 0); ry = sz / 2 - 3
    c.ell(7.5, cy, 6.6, ry, frame); c.ell(7.5, cy, 4.8, ry - 2, r[1:2] * 5, flat=True)
    for y in range(sz):
        for x in range(16):
            nx, ny = (x - 7.5) / 4.8, (y - cy) / (ry - 2); d = nx * nx + ny * ny
            if d < 1: v = (math.atan2(ny, nx) * 1.6 + d * 5) % 3; c.p(x, y, r[4] if v < 0.7 else r[3] if v < 1.5 else r[2] if d > .25 else r[4])
    if sz == 32: c.box(2, 27, 13, 29, frame); px(c, ((3, 9), (12, 9), (2, 17), (13, 17), (7, 3), (8, 3)), RUNE)
    fin(c, (sz - 2, 2, 13)); glow(c, 7.5, cy, 8.5, r[3], 50)
    return c

def rift(col='#b860e8'):
    c = C(16, 32); r = ramp(col); pts = ((8, 2), (6, 8), (9, 13), (6, 19), (8, 24), (7, 29))
    for k, ((a, b), (d, e)) in enumerate(zip(pts, pts[1:])):
        for w in range(-2, 3):
            if k in (0, 4) and abs(w) == 2: continue
            c.line(a + w, b, d + w, e, r[4] if w == 0 else r[3] if abs(w) == 1 else r[1])
    for x, y in ((3, 6), (12, 10), (2, 16), (12, 21), (4, 26)): c.p(x, y, r[4])
    c.outline(); glow(c, 7.5, 15, 7, r[3], 45)
    return c

def rune_circle(col=RUNE):
    """arcane ring: two rings, a square + diamond knot, 8 rune ticks (deliberately NOT a pentagram)"""
    c = C(32, 32); d = tuple(int(v * .6) for v in col); E = lambda a, rx, ry: (round(15.5 + rx * math.cos(math.radians(a))), round(15.5 + ry * math.sin(math.radians(a))))
    for a in range(360): c.p(*E(a, 13, 11.5), col); c.p(*E(a, 10, 8.8), d)
    for off, cc in ((0, col), (45, d)):
        P = [E(off + 90 * k, 10, 8.8) for k in range(5)]
        for a, b in zip(P, P[1:]): c.line(a[0], a[1], b[0], b[1], cc)
    for k in range(8): c.p(*E(k * 45 + 22, 11.5, 10.2), (255, 255, 255))
    c.ell(15.5, 15.5, 2, 1.6, [col] * 5, flat=True); glow(c, 15.5, 15.5, 4.5, col, 60)
    return c

def brazier(lit=True, fire=FIRE):
    c = C(); thick(c, 5, 8, 4, 13, IRON); thick(c, 9, 8, 10, 13, IRON); c.box(7, 8, 8, 13, IRON); c.box(3, 6, 12, 8, '#5a5f6c'); c.rect(4, 6, 11, 6, INK)
    if lit:
        for x in range(4, 12): c.p(x, 6, fire[2]); c.p(x, 5 - (x % 2), fire[1])
        px(c, ((6, 4), (7, 3), (8, 4), (9, 3), (7, 2), (8, 2)), fire[1]); px(c, ((7, 4), (8, 5), (7, 5), (8, 3), (6, 5), (9, 5)), fire[0]); c.p(8, 1, fire[2])
    else: px(c, ((5, 6), (7, 6), (10, 6)), (90, 84, 90))
    fin(c, (14, 4, 11))
    if lit: glow(c, 7.5, 3.5, 4.2, fire[1], 60)
    return c

def sarcophagus(col=STONE, trim=GOLD, opened=False):
    c = C(16, 32); r = ramp(col); t = ramp(trim); c.box(2, 3, 13, 28, col); c.rect(2, 27, 13, 28, r[0])
    if opened: c.rect(4, 5, 11, 25, INK); c.ell(7.5, 8, 2, 2, BONE); px(c, ((7, 8), (9, 8)), INK); c.rect(6, 11, 9, 17, ramp(BONE)[1]); c.rect(7, 11, 7, 22, ramp(BONE)[3])
    else:
        c.box(3, 4, 12, 25, r[2:] + r[3:]); c.ell(7.5, 8.5, 2.6, 2.8, trim); px(c, ((6, 8), (9, 8)), INK); c.rect(5, 12, 10, 12, t[2]); c.rect(7, 12, 8, 21, t[2]); c.rect(5, 16, 10, 16, t[3]); c.rect(4, 23, 11, 23, t[1])
    return fin(c, (30, 2, 13))

def iron_gate(opened=False):
    c = C(); i = ramp(IRON)
    if opened:
        for x in (0, 1, 14, 15): c.rect(x, 1, x, 14, i[3 if x in (0, 14) else 1])
        for x in range(2, 14, 3): c.rect(x, 0, x + 1, 2, i[2]); c.p(x, 3, i[4])
        return c.outline()
    for x in range(1, 15, 3): c.rect(x, 0, x + 1, 14, i[3]); c.rect(x + 1, 0, x + 1, 14, i[1])
    for y in (3, 10): c.rect(0, y, 15, y, i[2]); c.rect(0, y + 1, 15, y + 1, i[0])
    return c.outline()

def crystals(col='#58c8f0', glowing=True):
    c = C(); r = ramp(col)
    for cx, y0, h in ((4, 6, 2), (11, 5, 2), (7.5, 1, 3)): c.tri(cx, y0, 13, h, r); c.line(int(cx), y0 + 1, int(cx), 12, r[4])
    c.ell(7.5, 13, 5.5, 1.4, DARKSTONE, flat=True)
    fin(c, (14, 3, 12))
    if glowing: glow(c, 7.5, 8, 7, r[3], 40)
    return c

def stalagmite(col='#6a6470', big=False, tip=None):
    c = C(16, 32 if big else 16); H = c.h; r = ramp(col)
    c.tri(7.5, 2, H - 3, 4.4 if big else 3.6, r); c.tri(3.5, H - 9, H - 3, 2, r); c.tri(12, H - 7, H - 3, 1.8, r)
    for y in range(5, H - 4, 3): c.p(7 + (y % 2), y, r[1]); c.p(6, y + 1, r[4])
    if tip: cap(c, tip, 2, 5, H // 2)
    return fin(c, (H - 2, 3, 12))

def part_dungeon(A):
    A.section('props_dungeon'); o = lambda n, c, s=0: A.obj(n, c, cat='dungeon', solid=s)
    o('spikes_up', spikes()); o('spikes_down', spikes(False)); o('lever_left', lever(), 1); o('lever_right', lever(True), 1)
    o('pressure_plate_up', plate()); o('pressure_plate_down', plate(True)); o('pressure_plate_gold_up', plate(col=GOLD)); o('pressure_plate_gold_down', plate(True, GOLD))
    o('cage', cage(), 1); o('cage_bones', cage('bones'), 1)
    for k, d in enumerate(('nw', 'ne', 'se', 'sw')): o('web_' + d, web(k))
    for k in ('pile', 'skull', 'skulls', 'ribs'): o('bones_' + k if k != 'skulls' else 'skull_pile', bones(k))
    o('chains', chains()); o('chains_shackle', chains('shackle')); o('chain_single', chains('single'))
    for n, col in (('blue', '#58a8f0'), ('green', '#58d878'), ('purple', '#b860e8'), ('red', '#e86058'), ('gold', '#f0c848')):
        o('portal_' + n, portal(col), 0); o('portal_small_' + n, portal(col, DARKSTONE, 16), 0)
    o('rift', rift()); o('rift_void', rift('#5858c8')); o('rift_light', rift('#f0e8a0'))
    o('rune_circle', rune_circle()); o('rune_circle_red', rune_circle((250, 110, 100))); o('rune_circle_gold', rune_circle((250, 210, 100))); o('rune_circle_green', rune_circle((120, 240, 140)))
    o('brazier_lit', brazier(), 1); o('brazier_out', brazier(False), 1); o('brazier_blue', brazier(fire=((220, 250, 255), (100, 190, 250), (50, 100, 220))), 1); o('brazier_green', brazier(fire=((230, 255, 200), (120, 230, 110), (40, 150, 70))), 1)
    o('sarcophagus', sarcophagus(), 1); o('sarcophagus_open', sarcophagus(opened=True), 1); o('sarcophagus_sand', sarcophagus('#c8a868', '#3a8ab8'), 1); o('sarcophagus_dark', sarcophagus(DARKSTONE, '#b8bcc8'), 1)
    o('iron_gate_closed', iron_gate(), 1); o('iron_gate_open', iron_gate(True))
    for n, col in (('blue', '#58c8f0'), ('purple', '#b070e8'), ('green', '#60d890'), ('red', '#e86870')): o('crystal_cluster_' + n, crystals(col), 1)
    o('stalagmite', stalagmite(), 1); o('stalagmite_big', stalagmite(big=True), 1); o('stalagmite_ice', stalagmite('#88b8d8', tip=SNOW), 1); o('stalagmite_ice_big', stalagmite('#88b8d8', True, SNOW), 1); o('stalagmite_moss', stalagmite(tip=MOSS), 1)

PARTS = [part_nature, part_village, part_monument, part_doors, part_furniture, part_dungeon]

def build(A):
    for p in PARTS: p(A)
