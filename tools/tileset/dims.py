"""Dimension props (cat dim_<name>). One row in DIMS = shared kit (portal, floating rocks, spire, orb, wisp) + SPECIAL extras."""
from core import *
from props import speck, cap, fin, glow, px, portal, tree_dead, flame, STONE, SNOW, MOSS

def float_rock(col, top, big=True, seed=0, gem=None):
    s = 32 if big else 16; c = C(s, s); r = ramp(col); t = ramp(top); cx = s / 2 - .5; ty = s * .34
    for y in range(s):
        for x in range(s):
            d = (y - ty) / (s * .5)
            if 0 <= d <= 1:
                w = (s / 2 - 2) * (1 - d) ** .8 + (rnd(x // 2, y // 3, seed) - .5) * 2
                if abs(x - cx) <= w:
                    k = (x - cx) / max(1, w); c.p(x, y, r[3] if k < -.4 else r[1] if k > .4 else r[2])
                    if rnd(x, y, seed + 3) < .08: c.p(x, y, r[1])
    c.ell(cx, ty, s / 2 - 2, s * .11 + 1, t); speck(c, t, seed, .15)
    if gem:
        g = ramp(gem)
        for i in range(3 if big else 1):
            x = int(cx - 5 + i * 5) if big else int(cx); y = int(ty + 5 + i % 2 * 3)
            c.p(x, y, g[4]); c.p(x + 1, y, g[3]); c.p(x, y + 1, g[2])
    fin(c); c.shadow(s - 2, s // 4, s - s // 4 - 1, 50); return c

def spire(col, seed=0, tip=None, h=32):
    c = C(16, h); r = ramp(col)
    for (cx, y0, hw) in ((7, 2, 4), (3, h - 16, 2.5), (12, h - 12, 2.5)) if h == 32 else ((7, 2, 4),):
        for y in range(y0, h - 2):
            w = hw * min(1, (y - y0 + 1) / 6)
            for x in range(int(math.ceil(cx - w)), int(cx + w) + 1): c.p(x, y, r[4] if x < cx - w * .4 else r[3] if x < cx else r[2] if x < cx + w * .5 else r[1])
        c.p(cx, y0 + 3, (255, 255, 255))
    if tip: cap(c, tip, 2, seed)
    return fin(c, (h - 2, 2, 13))

def orb(col, stand='#5a5f78'):
    c = C(); c.box(5, 12, 10, 14, stand); c.rect(6, 10, 9, 11, ramp(stand)[2]); c.ell(7.5, 6, 4, 4, col); c.p(6, 4, (255, 255, 255)); c.p(7, 4, ramp(col)[4])
    fin(c, (14, 3, 12)); glow(c, 7.5, 6, 6.5, ramp(col)[3], 55); return c

def wisp(col, seed=0):
    c = C(); r = ramp(col); c.ell(7.5, 6.5, 3.2, 3.5, r)
    for i, (x, y) in enumerate(((7, 10), (8, 11), (7, 12), (8, 13), (9, 14))): c.p(x + (seed % 2) * (i % 2), y, r[3 if i < 3 else 2])
    c.p(6, 6, (255, 255, 255)); c.p(9, 6, (255, 255, 255)); glow(c, 7.5, 7, 6, r[3], 60); return c

def star(col, kind=0):
    c = C(); r = ramp(col); w = (255, 255, 255)
    if kind == 0: pts = [(8, 8, 5)]
    elif kind == 1: pts = [(4, 5, 2), (11, 9, 3), (6, 12, 1)]
    else: pts = [(11, 5, 3)]
    for x, y, n in pts:
        for i in range(1, n + 1):
            col2 = r[4] if i < n - 1 else r[3]
            for dx, dy in ((i, 0), (-i, 0), (0, i), (0, -i)): c.p(x + dx, y + dy, col2)
        if n > 2:
            for dx, dy in ((1, 1), (-1, 1), (1, -1), (-1, -1)): c.p(x + dx, y + dy, r[3])
        c.p(x, y, w)
    if kind == 2:
        for i in range(1, 9): c.p(11 - i, 5 + i, r[4 if i < 3 else 3 if i < 6 else 2]); c.p(12 - i, 5 + i, r[2], 160)
    glow(c, pts[0][0], pts[0][1], pts[0][2] + 1, r[3], 50); return c

def cloud(col='#f4f6fc', w=2, seed=0):
    c = C(16 * w, 16); r = ramp(col); W = 16 * w
    for i in range(w * 2 + 1):
        x = 4 + i * (W - 9) / (w * 2); c.ell(x, 9 - (3 if i % 2 else 0) * (1 if 0 < i < w * 2 else 0), 4, 4, r)
    c.rect(3, 9, W - 4, 12, r[2]); c.rect(4, 13, W - 5, 13, r[1])
    for x in range(3, W - 3): c.p(x, 12, r[1]) if rnd(x, 0, seed) < .5 else None
    return fin(c)

def gear(col, big=False, seed=0):
    s = 32 if big else 16; c = C(s, s); r = ramp(col); cx = s / 2 - .5; R = s / 2 - 3; n = 12 if big else 8
    for y in range(s):
        for x in range(s):
            dx, dy = x - cx, y - cx; d = math.hypot(dx, dy); a = math.atan2(dy, dx)
            tooth = math.cos(a * n + seed) > .2
            if d <= R + (2 if tooth else 0) and d >= R * .32:
                c.p(x, y, r[3] if dx + dy < -R * .5 else r[1] if dx + dy > R * .6 else r[2])
                if abs(d - R * .68) < .6: c.p(x, y, r[1])
            elif d < R * .32 and big and d > 1.5: c.p(x, y, r[0])
    return fin(c, (s - 2, s // 4, s - s // 4))

def pipe(col, mask):
    """NESW bitmask like line sets"""
    c = C(); r = ramp(col); sh = (r[3], r[4], r[2], r[2], r[1], r[0])
    for i, v in enumerate(sh):
        if mask & 1: c.rect(5 + i, 0, 5 + i, 7, v)
        if mask & 4: c.rect(5 + i, 8, 5 + i, 15, v)
    for i, v in enumerate(sh):
        if mask & 8: c.rect(0, 5 + i, 7 + (0 if mask & 5 else 3), 5 + i, v)
        if mask & 2: c.rect(8 - (0 if mask & 5 else 3), 5 + i, 15, 5 + i, v)
    if bin(mask).count('1') != 2 or mask in (3, 6, 12, 9): c.box(4, 4, 11, 11, r); c.p(6, 6, r[4]); c.p(9, 9, r[0])
    for b, (x0, y0, x1, y1) in ((1, (4, 1, 11, 2)), (4, (4, 13, 11, 14)), (8, (1, 4, 2, 11)), (2, (13, 4, 14, 11))):
        if mask & b: c.rect(x0, y0, x1, y1, r[1]); c.p(x0, y0, r[3])
    return fin(c)

def machine(col, kind):
    c = C(16, 32); r = ramp(col); br = ramp('#c8a040')
    if kind == 'boiler':
        c.ell(7.5, 10, 6, 6, r); c.box(2, 10, 13, 28, r); c.rect(2, 16, 13, 16, r[1]); c.rect(2, 23, 13, 23, r[1]); c.box(5, 19, 10, 22, '#3a2a28'); c.rect(6, 20, 9, 21, (250, 160, 50))
        c.ell(7.5, 10, 2, 2, '#e8e8f0'); c.p(8, 9, (200, 40, 40)); c.rect(11, 1, 12, 5, r[1]); px(c, [(x, 28) for x in (3, 6, 9, 12)], br[3])
    elif kind == 'piston':
        c.box(3, 20, 12, 29, r); c.rect(6, 6, 9, 19, br[2]); c.rect(6, 6, 6, 19, br[4]); c.rect(9, 6, 9, 19, br[1]); c.box(2, 2, 13, 6, r); px(c, ((4, 22), (11, 22), (4, 27), (11, 27)), br[4])
    else:  # clock tower
        c.box(3, 12, 12, 29, r); c.tri(7.5, 1, 6, 6, br); c.ell(7.5, 12, 5, 5, '#f0ece0'); c.line(7, 12, 7, 9, (40, 40, 50)); c.line(7, 12, 10, 12, (40, 40, 50)); c.rect(6, 23, 9, 29, r[0]); px(c, ((4, 20), (11, 20)), br[4])
    return fin(c, (30, 2, 13))

def coral(col, kind=0, seed=0):
    c = C(); r = ramp(col)
    if kind == 0:
        for i, (x, h) in enumerate(((3, 7), (6, 11), (9, 9), (12, 6))):
            c.rect(x, 14 - h, x + 1, 14, r[2]); c.rect(x, 14 - h, x, 14, r[3]); c.p(x, 13 - h, r[4]); c.p(x + 1, 13 - h, r[3])
            if h > 7: c.rect(x + 2, 14 - h + 3, x + 3, 14 - h + 4, r[2]); c.p(x + 3, 14 - h + 2, r[3])
    elif kind == 1:
        c.ell(7.5, 9, 6, 5, r)
        for y in range(4, 15):
            for x in range(1, 15):
                if c.get(x, y) and (x + y * 2 + int(3 * math.sin(y))) % 4 == 0: c.p(x, y, r[1])
    else:
        for i in range(7):
            a = math.pi * (0.1 + i * .8 / 6); c.line(7, 14, round(7.5 - math.cos(a) * 7), round(14 - math.sin(a) * (8 + i % 2 * 3)), r[3 if i % 2 else 2])
        c.rect(6, 12, 9, 14, r[1])
    return fin(c, (14, 3, 12))

def kelp(col='#3f9a5a', seed=0):
    c = C(16, 32); r = ramp(col)
    for i, x0 in enumerate((4, 8, 12)):
        for y in range(4 + i * 3, 30):
            x = x0 + round(math.sin(y * .45 + i * 2 + seed) * 1.5); c.p(x, y, r[3]); c.p(x + 1, y, r[1 if y % 5 else 2])
            if y % 6 == i: c.p(x + 2, y, r[2]); c.p(x - 1, y + 1, r[3])
    return fin(c, (30, 3, 12))

def shell(col, kind=0):
    c = C(); r = ramp(col)
    if kind == 0:
        c.ell(7.5, 10, 5, 4, r)
        for i in range(-2, 3): c.line(7 + i, 14, 7 + i * 2, 6, r[1])
        c.rect(6, 13, 9, 14, r[1])
    elif kind == 1:
        c.ell(8, 10, 4.5, 4, r); c.ell(5, 8, 2.5, 2.5, r); c.p(8, 10, r[1]); c.p(9, 9, r[1]); c.p(10, 10, r[1]); c.p(9, 11, r[1]); c.p(4, 7, r[4])
    else:
        c.ell(7.5, 11, 5.5, 3, r); c.ell(7.5, 8, 5.5, 3.5, r); c.rect(3, 10, 12, 10, (50, 40, 60)); c.ell(7.5, 10, 1.5, 1.5, '#f8f8ff'); c.p(7, 9, (255, 255, 255))
    return fin(c, (14, 3, 12))

def wreck():
    c = C(48, 32); w = ramp('#6a4a34'); d = ramp('#4a3426')
    for y in range(14, 29):
        for x in range(3, 45):
            t = (y - 14) / 14; l = 3 + t * 9 + (x * .12); rr = 44 - t * 6
            if l <= x <= rr - (x > 30) * (rnd(x // 3, y // 3, 5) < .35) * 99:
                c.p(x, y, w[3] if (y - 14) % 4 == 0 else w[1] if (y - 14) % 4 == 3 or (x + (y // 4) * 5) % 9 == 0 else w[2])
    for x in (14, 22, 30): c.rect(x, 17, x + 2, 19, (30, 26, 40))
    c.rect(20, 2, 21, 14, d[2]); c.rect(20, 2, 20, 14, d[3]); c.line(21, 4, 31, 9, d[1]); c.rect(12, 6, 28, 6, d[2])
    for y in range(7, 13):
        for x in range(13, 20):
            if rnd(x, y, 9) < .7: c.p(x, y, (200, 196, 180) if (x + y) % 3 else (160, 156, 146))
    px(c, ((6, 27), (9, 28), (38, 27), (41, 28)), ramp(MOSS)[2]); cap(c, '#4f8a6a', 1, 3, y_max=16)
    return fin(c, (30, 6, 42))

def anchor():
    c = C(); r = ramp('#6a7080'); c.rect(7, 3, 8, 13, r[2]); c.rect(7, 3, 7, 13, r[3]); c.rect(4, 6, 11, 6, r[2]); c.ell(7.5, 2.5, 1.6, 1.6, r)
    for i in range(4): c.p(3 + i, 13 - i // 2 + (i < 1) * -2, r[2]); c.p(12 - i, 13 - i // 2 + (i < 1) * -2, r[1])
    c.rect(4, 13, 11, 14, r[1]); c.p(3, 10, r[3]); c.p(12, 10, r[2]); return fin(c, (14, 3, 12))

def bubbles(col='#a8e0f8'):
    c = C(); r = ramp(col)
    for x, y, s in ((4, 11, 2), (10, 8, 3), (6, 4, 1.5), (12, 13, 1)):
        for a in range(16): c.p(round(x + math.cos(a * .4) * s), round(y + math.sin(a * .4) * s), r[3], 220)
        c.p(x - s / 2, y - s / 2, (255, 255, 255))
    return c

def giant_flower(col, seed=0):
    c = C(16, 32); st = ramp('#3f9a4a'); r = ramp(col)
    for y in range(10, 30): x = 7 + round(math.sin(y * .4 + seed)); c.p(x, y, st[3]); c.p(x + 1, y, st[1])
    c.ell(4, 22, 2.5, 1.5, st); c.ell(11, 18, 2.5, 1.5, st)
    for a in range(6): c.ell(7.5 + math.cos(a * 1.047) * 4, 7 + math.sin(a * 1.047) * 4, 2.4, 2.4, r)
    c.ell(7.5, 7, 2, 2, '#f8e060'); fin(c, (30, 4, 11)); glow(c, 7.5, 7, 8, r[3], 40); return c

def fairy_ring():
    c = C(32, 32)
    for i in range(10):
        a = i * .628; x = round(15.5 + math.cos(a) * 11); y = round(17 + math.sin(a) * 9); col = ramp(('#e85a8a', '#f0e070', '#a070e8')[i % 3])
        c.rect(x, y, x, y + 2, (240, 232, 210)); c.rect(x - 1, y - 1, x + 1, y, col[2]); c.p(x - 1, y - 1, col[4])
    c.outline()
    for i in range(14): a = i * .449; c.p(15.5 + math.cos(a) * 6, 17 + math.sin(a) * 5, (255, 250, 200), 200)
    return c

def shard(col, kind=0, h=16):
    c = C(16, h); r = ramp(col); pts = (((8, 2, 3),), ((5, 5, 2), (10, 3, 2.5)), ((4, 8, 2), (8, 4, 2), (12, 9, 1.5)))[kind % 3]
    for cx, y0, hw in pts:
        for y in range(y0, h - 2):
            t = (y - y0) / (h - 2 - y0); w = hw * (min(1, t * 4) if t < .7 else (1 - t) * 3.3)
            for x in range(int(math.ceil(cx - w)), int(cx + w) + 1): c.p(x, y, r[4] if x <= cx - 1 else r[3] if x <= cx else r[1])
        c.line(cx - 1, y0 + 2, cx - 1, y0 + 5, (255, 255, 255))
    return fin(c, (h - 2, 3, 12))

def mirror_pane(frame='#c8c8d8'):
    c = C(16, 32); c.box(2, 2, 13, 29, frame); g = ramp('#a8d8f0'); c.rect(4, 4, 11, 27, g[2])
    for i in range(-24, 12, 6):
        for y in range(4, 28):
            x = i + (27 - y) // 2 + 4
            if 4 <= x <= 11: c.p(x, y, g[4])
            if 4 <= x + 1 <= 11: c.p(x + 1, y, g[3])
    return fin(c, (30, 2, 13))

def geyser(col, base, h=32):
    c = C(16, h); r = ramp(col); b = ramp(base); c.ell(7.5, h - 4, 6, 2.5, b); c.ell(7.5, h - 5, 3, 1.2, r)
    for y in range(3, h - 5):
        w = .8 + 3.4 * ((y - 3) / (h - 8)) ** .7 + rnd(y, 0, 4) * .8
        for x in range(int(8 - w), int(8 + w)): c.p(x, y, r[4] if abs(x - 7.5) < 1 else r[3] if rnd(x, y, 2) < .6 else r[2])
    px(c, ((3, 4), (12, 6), (2, 10), (13, 12)), r[3]); c.outline(); glow(c, 7.5, 8, 7, r[3], 45); return c

def whirlwind(col='#d8e8f4'):
    c = C(16, 32); r = ramp(col)
    for y in range(3, 30):
        w = 6.5 * (1 - (y - 3) / 30) + .5; o = math.sin(y * .7) * 1.5
        for x in range(int(7.5 + o - w), int(7.5 + o + w) + 1): c.p(x, y, r[4] if (x + y * 2) % 5 == 0 else r[1] if y % 3 == 0 else r[2], 230)
    return fin(c, (30, 4, 11))

def stone_arch(col, top=None, seed=0):
    c = C(32, 32); r = ramp(col)
    for y in range(3, 30):
        for x in range(2, 30):
            dx = (x - 15.5); o = (dx / 13.5) ** 2 + ((y - 30) / 27) ** 2; i = (dx / 7.5) ** 2 + ((y - 30) / 17) ** 2
            if o <= 1 and i > 1:
                c.p(x, y, r[3] if dx < -6 or y < 6 else r[1] if dx > 9 else r[2])
                if rnd(x // 2, y // 2, seed) < .12: c.p(x, y, r[1])
    if top: cap(c, top, 2, seed)
    return fin(c)

def torii(col='#c83a30'):
    c = C(32, 32); r = ramp(col); d = ramp('#3a3038')
    for x in (6, 23): c.rect(x, 8, x + 2, 29, r[2]); c.rect(x, 8, x, 29, r[3]); c.rect(x + 2, 8, x + 2, 29, r[1]); c.rect(x - 1, 28, x + 3, 29, d[2])
    c.rect(1, 4, 30, 6, d[2]); c.rect(0, 3, 31, 3, d[3]); c.rect(3, 7, 28, 8, r[2]); c.rect(4, 13, 27, 14, r[2]); c.rect(4, 13, 27, 13, r[3]); c.rect(14, 9, 17, 12, r[1])
    return fin(c)

def glowshroom(col, big=False, seed=0):
    c = C(16, 32 if big else 16); r = ramp(col); st = ramp('#c8c0d8'); h = c.h
    if big:
        c.rect(6, 12, 9, 29, st[2]); c.rect(6, 12, 6, 29, st[3]); c.rect(9, 12, 9, 29, st[1]); c.ell(7.5, 9, 7, 6, r); c.rect(1, 12, 14, 15, (0, 0, 0)); [c.clear(x, y) for x in range(16) for y in range(13, 16) if not 6 <= x <= 9]
        c.rect(2, 12, 13, 12, r[1]); px(c, ((4, 6), (9, 5), (11, 9), (6, 9)), r[4])
    else:
        for x, y, s in ((4, 9, 3), (11, 7, 2.5), (8, 12, 2)):
            c.rect(x, y, x, 14, st[2]); c.ell(x, y, s, s * .7, r); c.p(x - 1, y - 1, r[4])
    fin(c, (h - 2, 3, 12)); glow(c, 7.5, 8, 8, r[3], 45); return c

def lantern(col, post='#3a3440'):
    c = C(16, 32); p = ramp(post); r = ramp(col); c.rect(7, 12, 8, 29, p[2]); c.rect(5, 29, 10, 30, p[1]); c.box(4, 4, 11, 12, post); c.rect(5, 6, 10, 10, r[3]); c.rect(6, 7, 9, 9, r[4]); c.rect(3, 3, 12, 3, p[3]); c.rect(6, 2, 9, 2, p[2])
    fin(c, (30, 4, 11)); glow(c, 7.5, 8, 7, r[3], 55); return c

def dream_door(col='#b890e0'):
    c = C(16, 32); c.box(3, 4, 12, 27, col); r = ramp(col); c.rect(5, 6, 10, 14, r[1]); c.rect(5, 17, 10, 25, r[1]); c.p(10, 16, (250, 220, 100)); c.rect(2, 28, 13, 29, ramp('#f4f6fc')[2])
    for x in range(1, 15, 3): c.ell(x + 1, 29, 2, 1.5, '#f4f6fc')
    fin(c); glow(c, 7.5, 16, 9, r[4], 35); return c

def moon(col='#f0e8b0'):
    c = C(); c.ell(7.5, 7.5, 6, 6, col)
    for y in range(16):
        for x in range(16):
            if (x - 10.5) ** 2 + (y - 5.5) ** 2 < 26: c.clear(x, y)
    c.outline(); glow(c, 6, 9, 8, ramp(col)[3], 40); return c

def vent(col, base):
    c = C(); b = ramp(base); c.ell(7.5, 10, 6, 4, b); c.ell(7.5, 9.5, 3.5, 2, col); r = ramp(col); c.p(7, 9, r[4]); c.p(8, 10, r[4]); fin(c); glow(c, 7.5, 8, 6, r[3], 50); return c

DIMS = {  # name: (energy colour, rock, rock top, portal frame)
    'astral': ('#8aa0f8', '#5a5a98', '#b8a8f0', '#c8c8e8'), 'void': ('#7a3ad0', '#2a2438', '#3e3458', '#3a3448'),
    'feywild': ('#f070c0', '#8a7a5a', '#5ac85a', '#6aa84a'), 'shadow': ('#6a6a8a', '#3a3848', '#4a4860', '#2e2c3a'),
    'dream': ('#f0a8e0', '#b8a8d8', '#f4f0fc', '#f0e8f8'), 'spirit': ('#70e8d0', '#7a8a88', '#a8d8c8', '#c83a30'),
    'mirror': ('#c0e8f8', '#a8b0c8', '#e8f0f8', '#d8d8e8'), 'fire': ('#f87028', '#4a3030', '#6a3a30', '#3a2a2a'),
    'water': ('#3f9af0', '#5a7a98', '#70c8d8', '#6a8ab0'), 'air': ('#c8e8f8', '#b8c0d0', '#f4f6fc', '#e0e8f4'),
    'earth': ('#c8a040', '#8a6a48', '#6a9a48', '#7a6a58'), 'mechanus': ('#f0c040', '#7a6a58', '#b87333', '#b87333'),
    'abyssal': ('#40d8c8', '#3a4a68', '#3f8a7a', '#4a5a78'), 'underdark': ('#a070f0', '#4a4458', '#5a5470', '#4a4458'),
}

def special(n, e, rock, top):
    if n == 'astral': return [('star', star('#f8f0a0'), 0), ('star_cluster', star('#c8d8ff', 1), 0), ('comet', star('#a8e0ff', 2), 0), ('moon', moon(), 0), ('star_blue', star('#a0c0ff'), 0)]
    if n == 'void': return [('tear', geyser(e, '#1e1a28'), 1), ('shard', shard('#4a3a70', 0), 1), ('shards', shard('#4a3a70', 2), 1), ('vent', vent(e, '#1e1a28'), 0), ('pillar', shard('#2e2840', 0, 32), 1)]
    if n == 'feywild': return [('flower_pink', giant_flower('#f070a8'), 1), ('flower_blue', giant_flower('#70a8f0', 1), 1), ('flower_gold', giant_flower('#f0c040', 2), 1), ('fairy_ring', fairy_ring(), 0), ('glowshroom', glowshroom('#f0a0e0'), 0), ('arch', stone_arch('#8a7a5a', MOSS), 0)]
    if n == 'shadow': return [('tree', tree_dead('#2e2a3c', 3), 1), ('lantern', lantern('#8a8ac8'), 1), ('shard', shard('#3a3850', 1), 1), ('vent', vent('#9a9ac8', '#2a2836'), 0), ('arch', stone_arch('#3a3848', None, 2), 0)]
    if n == 'dream': return [('cloud', cloud('#fbe8f8'), 0), ('cloud_small', cloud('#fbe8f8', 1), 0), ('door', dream_door(), 0), ('moon', moon('#f8d0f0'), 0), ('star', star('#f8e0a0'), 0), ('cloud_big', cloud('#e8e0fc', 3, 2), 0)]
    if n == 'spirit': return [('torii', torii(), 0), ('lantern', lantern('#70e8d0', '#8a8d96'), 1), ('wisp_blue', wisp('#70b8f8', 1), 0), ('wisp_white', wisp('#e8f0f8'), 0), ('tree', tree_dead('#c8d8d8', 5), 1)]
    if n == 'mirror': return [('pane', mirror_pane(), 1), ('pane_gold', mirror_pane('#e8b830'), 1), ('shard', shard('#c0e8f8', 0), 1), ('shards', shard('#c0e8f8', 2), 1), ('shard_tall', shard('#d8f0fc', 0, 32), 1), ('shard_pair', shard('#c0e8f8', 1), 1)]
    if n == 'fire': return [('geyser', geyser('#f87028', '#3a2a2a'), 1), ('vent', vent('#f8a030', '#3a2a2a'), 0), ('obsidian', shard('#2a2434', 1), 1), ('obsidian_tall', shard('#2a2434', 0, 32), 1), ('ember_rock', float_rock('#4a3030', '#f87028', False, 3), 1), ('flame_small', geyser('#f8b040', '#4a3030', 16), 1)]
    if n == 'water': return [('spout', geyser('#70b8f8', '#3f6aa8'), 1), ('bubbles', bubbles(), 0), ('coral', coral('#f08a70'), 1), ('shell', shell('#f0d8c8'), 0), ('whirlpool', vent('#a8d8f8', '#3f6aa8'), 0), ('ice_shard', shard('#a8d8f8', 1), 1)]
    if n == 'air': return [('cloud', cloud(), 0), ('cloud_small', cloud(w=1), 0), ('cloud_big', cloud(w=3, seed=1), 0), ('whirlwind', whirlwind(), 1), ('cloud_storm', cloud('#8a90a8'), 0), ('arch', stone_arch('#d8e0ec', '#f4f6fc', 4), 0)]
    if n == 'earth': return [('arch', stone_arch('#8a6a48', MOSS, 1), 0), ('geode', float_rock('#6a5a50', '#b070e8', False, 2, '#e0b0ff'), 1), ('pillar', shard('#8a6a48', 0, 32), 1), ('crystals', shard('#e8c050', 2), 1), ('mud_vent', vent('#6a4a30', '#8a6a48'), 0)]
    if n == 'mechanus':
        out = [('gear_big', gear('#b87333', True), 1), ('gear', gear('#c8a040'), 1), ('gear_iron', gear('#7d8494', seed=1), 1), ('gear_big_iron', gear('#7d8494', True, 2), 1), ('boiler', machine('#b87333', 'boiler'), 1), ('piston', machine('#7d8494', 'piston'), 1), ('clock', machine('#8a6a58', 'clock'), 1)]
        return out + [('pipe_%d' % m, pipe('#b87333', m), 1) for m in range(1, 16)]
    if n == 'abyssal': return [('coral_red', coral('#e85a5a'), 1), ('coral_brain', coral('#f08ab0', 1), 1), ('coral_fan', coral('#b070e8', 2), 1), ('coral_gold', coral('#f0c040'), 1), ('kelp', kelp(), 0), ('kelp_dark', kelp('#2f6a5a', 2), 0),
                               ('shell', shell('#f0d0c0'), 0), ('shell_spiral', shell('#e8b890', 1), 0), ('clam_pearl', shell('#a890c8', 2), 0), ('wreck', wreck(), 1), ('anchor', anchor(), 1), ('bubbles', bubbles(), 0)]
    if n == 'underdark': return [('glowshroom', glowshroom('#70e8c0'), 0), ('glowshroom_purple', glowshroom('#b070f0'), 0), ('glowshroom_big', glowshroom('#70e8c0', True), 1), ('glowshroom_big_purple', glowshroom('#b070f0', True), 1),
                                 ('glow_spire', spire('#5a5470', 2, '#70e8c0'), 1), ('crystal_vein', float_rock('#4a4458', '#5a5470', False, 4, '#70e8c0'), 1), ('arch', stone_arch('#4a4458', None, 6), 0)]
    return []

def build(A):
    A.section('dimensions')
    for i, (n, (e, rock, top, frame)) in enumerate(DIMS.items()):
        cat = 'dim_' + n; o = lambda k, c, solid=1: A.obj('%s_%s' % (n, k), c, cat=cat, solid=solid)
        o('portal', portal(e, frame), 0); o('portal_small', portal(e, frame, 16), 0)
        o('float_rock', float_rock(rock, top, True, i, e if i % 2 else None)); o('float_rock_small', float_rock(rock, top, False, i + 7))
        o('spire', spire(e, i)); o('spire_small', spire(e, i, h=16)); o('orb', orb(e)); o('wisp', wisp(e, i), 0)
        for k, c, solid in special(n, e, rock, top): o(k, c, solid)
