"""Creatures. 16x16 (big ones 32x32), four directions (down, left, right, up); left = mirrored right.
One row in CREATURES = one creature: name: (archetype function, kwargs). Archetypes: slime, quad (four-legged), flyer, bug (top view,
rotated), snake, floater, fowl, frog, turtle, fish. A new creature = one new row."""
from core import *
from sprites import INK, BONE, STEEL, GOLD, px

WH = (250, 250, 250); REDE = (220, 50, 50); YEL = (250, 220, 70)

def _four(fn, alpha=255):
    o = {d: fn(d) for d in 'dru'}
    for c in o.values():
        c.outline()
        if alpha < 255: c.a[:, :, 3] = (c.a[:, :, 3].astype(int) * alpha // 255).astype(np.uint8)
    return [o['d'], o['r'].flip(), o['r'], o['u']]

def _top(fn, outline=True):
    """true top view drawn facing DOWN (head at the bottom), rotated for the other directions"""
    c = fn(); r = c.rot(1); u = c.rot(2)
    for k in (c, r, u):
        if outline: k.outline()
    return [c, r.flip(), r, u]

def slime(col, crown=False, big=0, eye=INK, core=None):
    rp = ramp(col)
    def f(d):
        c = C(); r = 5 + big
        c.ell(7.5, 9.5, r, 4.5 + big * .5, rp)
        for x in range(16):
            c.clear(x, 14); c.clear(x, 15)
            if c.get(x, 13): c.p(x, 13, rp[1])
        px(c, [(5, 7), (6, 6), (5, 6)], rp[4])
        if core: px(c, [(7, 9), (8, 9), (7, 10), (8, 10)], ramp(core)[3])
        if d == 'd': px(c, [(5, 10), (5, 11), (10, 10), (10, 11)], eye)
        if d == 'r': px(c, [(9, 10), (9, 11), (12, 10), (12, 11)], eye)
        if crown: px(c, [(5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4), (5, 3), (7, 3), (8, 3), (10, 3), (5, 2), (10, 2)], GOLD); c.p(7, 4, REDE)
        return c
    return _four(f)

def _ears(c, ear, rp, side, dy=0, inner=None):
    L = {'point': [(0, 4), (0, 3), (1, 4)], 'round': [(0, 4), (1, 4), (0, 5)], 'long': [(1, 1), (1, 2), (1, 3), (1, 4)],
         'droop': [(-1, 6), (-1, 7), (-1, 8)], 'none': []}[ear]
    if side:   # side view: one ear at x=11
        for x, y in L: c.p(11 - (x if ear != 'droop' else -1), y + dy - (0 if ear != 'droop' else 1), rp[1] if ear == 'droop' else rp[2])
    else:
        for x, y in L:
            c.p(4 + x, y + dy, rp[2]); c.p(11 - x, y + dy, rp[2])

def quad(col, ear='point', tail='bushy', sz=0, snout=None, belly=None, tusk=False, stripe=None, mane=None, horn=None, legc=None,
         long=0, eye=INK, tailc=None, spots=None):
    rp = ramp(col); sn = ramp(snout)[2] if snout else rp[3]; lg = ramp(legc) if legc else rp; tc = ramp(tailc) if tailc else rp
    hc = ramp(horn)[3] if horn and horn not in ('antler',) else BONE
    def f(d):
        c = C(); s = sz * .5
        if d == 'r':
            if tail == 'bushy': c.ell(1.5, 8, 1.5, 2.5, tc)
            if tail == 'thin': c.line(2, 9, 1, 5, tc[1]); c.p(1, 4, tc[0])
            if tail == 'stub': px(c, [(1, 8), (2, 8)], tc[3])
            if tail == 'horse': c.line(2, 7, 1, 12, tc[1]); c.line(1, 8, 0, 12, tc[0])
            for x in (5, 10): c.rect(x, 12, x, 14, lg[0])
            for x in (3, 12): c.rect(x, 12, x, 14, lg[1])
            c.ell(7, 9, 5 + s, 3 + s, rp)
            if belly: c.rect(4, 11, 10, 11, ramp(belly)[3])
            if stripe:
                for x in (4, 6, 8, 10): c.line(x, 7, x, 9, ramp(stripe)[1])
            if spots: px(c, [(4, 8), (7, 7), (9, 9), (6, 10)], ramp(spots)[3])
            if long: c.rect(10, 6 - long, 12, 9, rp[2]); c.line(10, 6 - long, 10, 8, rp[3])
            if mane: c.ell(10.5, 7 - long, 2, 3.5, ramp(mane))
            c.ell(12, 6.5 - long, 2.5 + s, 2.5, rp)
            c.rect(14, 7 - long, 15, 8 - long, sn); c.p(15, 7 - long, INK); c.p(13, 6 - long, eye)
            _ears(c, ear, rp, 1, -long)
            if horn == 'antler': px(c, [(12, 3 - long), (12, 2 - long), (11, 1 - long), (13, 1 - long), (13, 0)], BONE)
            elif horn: px(c, [(13, 3 - long), (14, 2 - long)], hc)
            if tusk: c.p(14, 9, WH)
            return c
        for x in (4, 5, 10, 11): c.rect(x, 12, x, 14, lg[1])
        if d == 'u': c.ell(7.5, 6.5 - long, 3.5 + s, 2.5, rp); _ears(c, ear, rp, 0, -long)
        if d == 'u' and horn == 'antler': px(c, [(4, 3), (4, 2), (3, 1), (5, 1), (11, 3), (11, 2), (10, 1), (12, 1)], BONE)
        elif d == 'u' and horn: px(c, [(3, 4), (2, 3), (12, 4), (13, 3)], hc)
        c.ell(7.5, 9.5, 4.5 + s, 4, rp)
        if stripe:
            for y in (7, 9, 11): c.line(4, y, 11, y, ramp(stripe)[1])
        if spots: px(c, [(5, 9), (9, 8), (8, 11), (6, 11)], ramp(spots)[3])
        if d == 'u':
            if mane: c.ell(7.5, 7, 3.5, 2.5, ramp(mane))
            if tail == 'bushy': c.ell(7.5, 11.5, 1.5, 2.5, tc); c.p(7, 13, tc[4])
            if tail in ('thin', 'horse'): c.line(7, 9, 7, 13, tc[0]); c.line(8, 9, 8, 12 + (tail == 'horse'), tc[1])
            if tail == 'stub': px(c, [(7, 11), (8, 11), (7, 12), (8, 12)], tc[4])
            return c
        if belly: c.rect(6, 11, 9, 12, ramp(belly)[3])
        if mane: c.ell(7.5, 7 - long, 5, 4.5, ramp(mane))
        c.ell(7.5, 7.5 - long, 3.5 + s, 3, rp)
        c.rect(6, 9 - long, 9, 10 - long, sn); c.rect(7, 9 - long, 8, 9 - long, INK)
        px(c, [(5, 7 - long), (10, 7 - long)], eye)
        _ears(c, ear, rp, 0, -long)
        if horn == 'antler': px(c, [(4, 3), (4, 2), (3, 1), (5, 1), (11, 3), (11, 2), (10, 1), (12, 1)], BONE)
        elif horn: px(c, [(3, 4 - long), (2, 3 - long), (12, 4 - long), (13, 3 - long)], hc)
        if tusk: px(c, [(5, 10), (10, 10)], WH)
        return c
    return _four(f)

def flyer(col, wing=None, sz=0, beak=None, ear=False, eye=INK, bat=False, thin=False, wing2=None, stripe=None):
    rp = ramp(col); wg = ramp(wing or col); bk = ramp(beak)[2] if beak else None
    def f(d):
        c = C()
        if d == 'r':
            px(c, [(4, 9), (3, 10), (4, 10)], rp[1])
            c.ell(8, 9, 1 if thin else 3 + sz * .5, 2.5, rp)
            c.ell(11, 7, 2, 2, rp); c.p(12, 7, eye)
            if bk: px(c, [(13, 7), (14, 8)], bk)
            if ear: px(c, [(10, 4), (11, 4)], rp[1])
            c.ell(6.5, 5, 3.5, 3 + sz * .5, wg)
            if wing2: c.ell(6.5, 5, 1.5, 1.5, ramp(wing2))
            if bat: px(c, [(4, 8), (6, 8), (8, 8)], wg[0])
            if stripe: c.line(7, 8, 7, 10, ramp(stripe)[1]); c.line(9, 8, 9, 10, ramp(stripe)[1])
            return c
        for cx in (3, 12):
            c.ell(cx, 7, 3, 3 + sz * .5, wg)
            if wing2: c.ell(cx, 7, 1.5, 1.5, ramp(wing2))
            if bat:
                for x in (cx - 2, cx, cx + 2): c.clear(x, 10 + sz // 2); c.clear(x, 11)
        c.ell(7.5, 8.5, 1 if thin else 2.5 + sz * .5, 3.5, rp)
        if stripe: c.line(6, 9, 9, 9, ramp(stripe)[1]); c.line(6, 11, 9, 11, ramp(stripe)[1])
        if ear: px(c, [(6, 4), (9, 4), (6, 5), (9, 5)], rp[1])
        if thin: px(c, [(6, 3), (9, 3), (7, 4), (8, 4)], rp[0])
        if d == 'd':
            px(c, [(6, 7), (9, 7)], eye)
            if bk: px(c, [(7, 8), (8, 8), (7, 9)], bk)
        if bk: px(c, [(6, 12), (9, 12)], bk)
        return c
    return _four(f)

def bug(col, legs=4, claws=False, sting=False, sz=0, mark=None, eye=REDE, legc=None, shell=False):
    rp = ramp(col); lg = ramp(legc)[1] if legc else ramp(rp[0])[0]
    def f():
        c = C()
        if sting: c.line(7, 3, 7, 1, rp[1]); c.line(8, 3, 8, 1, rp[2]); px(c, [(9, 0), (10, 1)], rp[1]); c.p(10, 2, REDE)
        c.ell(7.5, 6, 3 + sz * .5, 3 + sz * .5, rp)
        if shell: c.line(7, 3, 7, 9, rp[0]); px(c, [(6, 4), (5, 5)], rp[4])
        if mark: px(c, [(7, 5), (8, 5), (7, 6), (8, 6), (7, 4)], ramp(mark)[3])
        c.ell(7.5, 10.5, 2.5, 2, rp)
        px(c, [(6, 11), (9, 11)], eye)
        if claws:
            c.ell(3.5, 12.5, 1.5, 1.5, rp); c.ell(11.5, 12.5, 1.5, 1.5, rp); c.clear(3, 14); c.clear(12, 14)
        else: px(c, [(7, 13), (8, 13)], rp[0])
        c.outline()
        ys = (3, 6, 9) if legs == 3 else (2, 5, 8, 11)   # legs: plain dark strokes, no outline (keeps them thin)
        for i, y in enumerate(ys[:3] if claws else ys):
            for x, yy in ((5, y + 1), (4, y), (3, y), (2, y), (1, y + 1), (1, y + 2)):
                for X in (x, 15 - x):
                    if not c.get(X, yy): c.p(X, yy, lg)
        return c
    return _top(f, outline=False)

def snake(col, belly='e8dca0', hood=False, pat=None, eye=YEL):
    rp = ramp(col); bl = ramp(belly)
    def f(d):
        c = C(); hx = 7.5 if d != 'r' else 9.5
        c.ell(7.5, 11, 5.5, 3, rp)
        for x in range(4, 12): c.p(x, 11, rp[0])
        if pat:
            for x in (3, 6, 9, 12): c.p(x, 9 + (x % 2), ramp(pat)[3]); c.p(x, 13 - (x % 2), ramp(pat)[3])
        c.rect(int(hx), 6, int(hx) + 1, 10, rp[2])
        if d != 'u': c.rect(int(hx), 7, int(hx) + 1, 10, bl[3])
        if hood: c.ell(hx, 6, 3.5, 2.5, rp)
        c.ell(hx, 4.5, 2, 2, rp)
        if hood and d == 'u': px(c, [(7, 6), (8, 6), (7, 7), (8, 7)], bl[3])
        if d == 'd': px(c, [(6, 4), (9, 4)], eye); px(c, [(7, 7), (8, 8)], REDE)
        if d == 'r': c.p(10, 4, eye); px(c, [(12, 5), (13, 6)], REDE)
        return c
    return _four(f)

def floater(col, eye=INK, tent=None, alpha=255, skull=False, flame=None, mouth=False, big_eye=None):
    rp = ramp(col)
    def f(d):
        c = C()
        if flame:
            fl = ramp(flame); c.ell(7.5, 6, 5.5, 5.5, fl); px(c, [(7, 0), (8, 1), (4, 1), (11, 2), (3, 3)], fl[3])
        c.ell(7.5, 6.5, 4.5, 4.5, rp)
        if not skull:
            for y in range(7, 12):
                for x in range(3, 13): c.p(x, y, rp[3] if x < 5 else rp[1] if x > 10 else rp[2])
            for x in range(3, 13):
                if x % 3 != 2: c.p(x, 12, rp[1])
                if x % 3 == 0: c.p(x, 13, rp[1])
        else:
            c.rect(5, 10, 10, 12, rp[2]); px(c, [(6, 12), (8, 12)], INK)
        if tent:
            t = ramp(tent)
            for i, x in enumerate((4, 6, 9, 11)): c.line(x, 11, x + (i % 2) * 2 - 1, 14, t[1 + i % 2])
        if d == 'd':
            if big_eye: c.ell(7.5, 6.5, 2.5, 2.5, ramp('f4f4f4')); c.rect(7, 6, 8, 7, big_eye)
            else: px(c, [(5, 6), (5, 7), (6, 6), (6, 7), (9, 6), (9, 7), (10, 6), (10, 7)] if skull else [(5, 6), (5, 7), (10, 6), (10, 7)], eye)
            if mouth: px(c, [(7, 9), (8, 9), (7, 10), (8, 10)], eye)
        if d == 'r':
            if big_eye: c.ell(10, 6.5, 2, 2.5, ramp('f4f4f4')); c.rect(11, 6, 11, 7, big_eye)
            else: px(c, [(9, 6), (9, 7), (12, 6), (12, 7)], eye)
            if mouth: px(c, [(11, 9), (11, 10)], eye)
        return c
    return _four(f, alpha)

def fowl(col, beak='e8a030', comb=None, tailc=None, sz=0):
    rp = ramp(col); bk = ramp(beak)[2]; tc = ramp(tailc or col)
    def f(d):
        c = C()
        if d == 'r':
            px(c, [(2, 6), (3, 7), (2, 7), (3, 8), (2, 8), (1, 5)], tc[1])
            c.ell(7, 10, 4 + sz, 3.5, rp); c.ell(10.5, 5.5, 2.5, 2.5, rp)
            px(c, [(13, 6), (14, 6)], bk); c.p(11, 5, INK); px(c, [(6, 14), (8, 14)], bk)
            px(c, [(5, 9), (6, 10), (7, 10)], rp[1])
            if comb: px(c, [(10, 2), (11, 2), (12, 3)], ramp(comb)[2]); c.p(12, 8, ramp(comb)[2])
            return c
        c.ell(7.5, 10, 4 + sz, 3.5, rp); c.ell(7.5, 5.5, 2.5, 2.5, rp); px(c, [(6, 14), (9, 14)], bk)
        if comb: px(c, [(7, 2), (8, 2), (7, 1)], ramp(comb)[2])
        if d == 'd':
            px(c, [(6, 5), (9, 5)], INK); px(c, [(7, 6), (8, 6), (7, 7)], bk)
            if comb: c.p(8, 8, ramp(comb)[2])
        else: px(c, [(7, 8), (8, 8), (7, 9), (8, 9), (7, 7)], tc[1])
        return c
    return _four(f)

def frog(col, belly='e8e0a8', spots=None):
    rp = ramp(col)
    def f(d):
        c = C()
        c.ell(3.5, 12, 2, 1.5, rp); c.ell(11.5, 12, 2, 1.5, rp)
        c.ell(7.5, 10, 4.5, 3.5, rp)
        if d == 'r':
            c.ell(11, 6.5, 1.5, 1.5, rp); c.p(11, 6, INK); c.rect(9, 9, 12, 9, rp[0]); c.rect(8, 11, 11, 12, ramp(belly)[3])
        else:
            c.ell(5, 6.5, 1.5, 1.5, rp); c.ell(10, 6.5, 1.5, 1.5, rp)
            if d == 'd': px(c, [(5, 6), (10, 6)], INK); c.rect(5, 9, 10, 9, rp[0]); c.rect(6, 11, 9, 12, ramp(belly)[3])
        if spots: px(c, [(6, 8), (9, 10) if d == 'u' else (9, 8), (4, 10)], ramp(spots)[3])
        return c
    return _four(f)

def turtle(shell, skin='7fae5a', spike=False):
    rp = ramp(shell); sk = ramp(skin)
    def f():
        c = C()
        for x, y in ((3, 4), (12, 4), (3, 11), (12, 11)): c.ell(x, y, 1.5, 1.5, sk)
        c.ell(7.5, 12.5, 2, 2, sk); px(c, [(6, 13), (9, 13)], INK); px(c, [(7, 1), (8, 1)], sk[1])
        c.ell(7.5, 7, 5, 5, rp)
        px(c, [(7, 4), (8, 4), (5, 6), (10, 6), (7, 7), (8, 7), (5, 9), (10, 9), (7, 10), (8, 10)], WH if spike else rp[0])
        return c
    return _top(f)

def fish(col, fin=None, belly=None, teeth=False, long=0):
    rp = ramp(col); fn = ramp(fin or col)
    def f():
        c = C()
        c.tri(7.5, 3, 0, 3, fn)
        c.a[0:4] = c.a[0:4][::-1].copy()
        px(c, [(3, 7), (2, 8), (12, 7), (13, 8), (3, 8), (12, 8)], fn[1])
        c.ell(7.5, 8.5, 3 - long * .5, 5 + long * .5, rp)
        if belly: px(c, [(7, 6), (8, 6), (7, 8), (8, 8), (7, 10), (8, 10)], ramp(belly)[3])
        px(c, [(5, 11), (10, 11)], WH); px(c, [(5, 12), (10, 12)], INK)
        if teeth: px(c, [(6, 14), (8, 14), (9, 14)], WH)
        return c
    return _top(f)

R = lambda fn, **kw: (fn, kw)
CREATURES = {
    # slimes
    'slime_green': R(slime, col='6cc84a'), 'slime_blue': R(slime, col='4a9ae8'), 'slime_red': R(slime, col='e0504a'),
    'slime_purple': R(slime, col='a060d8'), 'slime_gold': R(slime, col='ecc040'), 'slime_ice': R(slime, col='bfe8f4', core='ffffff'),
    'slime_dark': R(slime, col='4a4458', eye=REDE), 'slime_king': R(slime, col='4a9ae8', crown=True, big=1),
    # beasts
    'wolf': R(quad, col='8a8a94', belly='d8d8dc'), 'wolf_white': R(quad, col='e8e8ee', eye=(60, 120, 200)),
    'wolf_dire': R(quad, col='4a4654', sz=1, eye=REDE, belly='7a7684'), 'fox': R(quad, col='e07a30', belly='f4ece0', tailc='f0c090', snout='f4ece0'),
    'bear': R(quad, col='7a5236', ear='round', tail='stub', sz=2, snout='c8a078'), 'bear_polar': R(quad, col='f0f0ec', ear='round', tail='stub', sz=2),
    'boar': R(quad, col='6a4a3a', tail='thin', sz=1, tusk=True, snout='c89888', mane='4a3428'),
    'deer': R(quad, col='b8824a', ear='point', tail='stub', horn='antler', long=1, belly='f0e0c0', tailc='f4f0e8'),
    'doe': R(quad, col='c8925a', ear='point', tail='stub', long=1, belly='f0e0c0', spots='f4ecd8'),
    'horse': R(quad, col='8a5a34', tail='horse', sz=1, long=2, mane='3a2a20', tailc='3a2a20', legc='5a3a24'),
    'horse_white': R(quad, col='f0ece4', tail='horse', sz=1, long=2, mane='d8d0c0', tailc='d8d0c0'),
    'horse_black': R(quad, col='3a3640', tail='horse', sz=1, long=2, mane='24222a', tailc='24222a'),
    'cow': R(quad, col='f0ece4', ear='droop', tail='thin', sz=2, horn='e8dcc0', spots='3a3238', snout='e8a8a0'),
    'bull': R(quad, col='5a3a2a', ear='droop', tail='thin', sz=2, horn='e8dcc0', snout='c89888'),
    'sheep': R(quad, col='f4f0e8', ear='droop', tail='stub', sz=2, snout='4a4248', legc='4a4248'),
    'goat': R(quad, col='d8d0c0', ear='droop', tail='stub', horn='8a8478', long=1),
    'pig': R(quad, col='f0a8a8', ear='point', tail='stub', sz=1, snout='e08888'),
    'cat': R(quad, col='e89a40', tail='thin', sz=-1, stripe='b86a20', eye=(60, 140, 60)), 'cat_black': R(quad, col='3a3640', tail='thin', sz=-1, eye=YEL),
    'cat_white': R(quad, col='f4f0ec', tail='thin', sz=-1, eye=(60, 120, 200)),
    'dog': R(quad, col='c8904a', ear='droop', tail='thin', belly='f0e0c0'), 'dog_spotted': R(quad, col='f0ece4', ear='droop', tail='thin', spots='4a4248'),
    'rat': R(quad, col='8a7a70', ear='round', tail='thin', sz=-2, tailc='e0a0a0', snout='e0a0a0'),
    'rat_giant': R(quad, col='6a5a54', ear='round', tail='thin', tailc='e0a0a0', snout='e0a0a0', eye=REDE),
    'rabbit': R(quad, col='e8e0d4', ear='long', tail='stub', sz=-2), 'rabbit_brown': R(quad, col='a87a50', ear='long', tail='stub', sz=-2, tailc='f4f0e8'),
    'lion': R(quad, col='e0b050', ear='round', tail='thin', sz=1, mane='a86a28', tailc='a86a28'),
    'tiger': R(quad, col='e88a30', ear='round', tail='thin', sz=1, stripe='3a2a20', belly='f4ece0'),
    'panther': R(quad, col='34303c', ear='round', tail='thin', sz=1, eye=YEL),
    # flyers
    'bat': R(flyer, col='5a4a6a', wing='4a3a5a', bat=True, ear=True, eye=REDE), 'bat_giant': R(flyer, col='7a3a3a', wing='5a2a34', bat=True, ear=True, sz=2, eye=YEL),
    'crow': R(flyer, col='2e2a38', beak='e8a030'), 'owl': R(flyer, col='a87a4a', wing='8a6238', beak='e8a030', ear=True, eye=YEL, sz=1),
    'eagle': R(flyer, col='6a4a30', wing='5a3e28', beak='ecc040', sz=2), 'dove': R(flyer, col='f4f4f8', beak='e8a030'),
    'parrot': R(flyer, col='e04a40', wing='3a8ae0', beak='ecc040', wing2='ecc040'),
    'bee': R(flyer, col='ecc040', wing='d8ecf8', stripe='2e2a34', sz=-1), 'wasp_giant': R(flyer, col='e8a030', wing='d8ecf8', stripe='2e2a34', sz=1, eye=REDE),
    'butterfly': R(flyer, col='3a3038', wing='e86aa0', thin=True, wing2='f8e070'), 'butterfly_blue': R(flyer, col='3a3038', wing='4a8ae8', thin=True, wing2='c8ecf8'),
    'firefly': R(flyer, col='4a4038', wing='f0f0a0', thin=True, wing2='f8f840', sz=-1),
    # bugs (top view)
    'spider': R(bug, col='4a4250'), 'spider_giant': R(bug, col='6a3a3a', sz=2, mark='e04040', eye=YEL), 'spider_cave': R(bug, col='c8c0b0', sz=1, eye=REDE),
    'crab': R(bug, col='e0603a', legs=3, claws=True, eye=INK), 'crab_blue': R(bug, col='4a8ac8', legs=3, claws=True, eye=INK),
    'beetle': R(bug, col='3a7a5a', legs=3, shell=True, sz=1, eye=INK), 'beetle_gold': R(bug, col='e0b040', legs=3, shell=True, sz=1, eye=INK),
    'scorpion': R(bug, col='c88a40', legs=3, claws=True, sting=True), 'scorpion_black': R(bug, col='3a3644', legs=3, claws=True, sting=True),
    'ant_giant': R(bug, col='8a3a2a', legs=3, eye=INK),
    # snakes
    'snake': R(snake, col='5aa84a'), 'viper': R(snake, col='8a4a8a', pat='e8d060', eye=REDE), 'cobra': R(snake, col='c8a050', hood=True),
    'snake_sea': R(snake, col='3a8ac8', belly='d8f0f8', pat='e8f0a0'),
    # floaters
    'specter': R(floater, col='d8e4f0', alpha=200, mouth=True), 'wraith': R(floater, col='3a3448', eye=REDE, alpha=225),
    'will_o_wisp': R(floater, col='a0f0e0', flame='50c8d8', eye=WH), 'fire_spirit': R(floater, col='f8c040', flame='f06030', eye=INK),
    'jellyfish': R(floater, col='f0a0d0', tent='d070b0', alpha=215), 'jellyfish_blue': R(floater, col='90c8f8', tent='5a90e0', alpha=215),
    'skull_floating': R(floater, col='e8e0c8', skull=True), 'skull_flaming': R(floater, col='e8e0c8', skull=True, flame='40c060'),
    'watcher_eye': R(floater, col='8a5a9a', tent='6a3a7a', big_eye=(200, 40, 40)),
    # small animals
    'chicken': R(fowl, col='f4f0e8', comb='e04040'), 'rooster': R(fowl, col='c8703a', comb='e04040', tailc='2e5a4a', sz=1), 'chick': R(fowl, col='f4d860', sz=-1),
    'duck': R(fowl, col='f4f0e8', beak='f0a020'), 'duck_mallard': R(fowl, col='8a6a4a', beak='ecc040', tailc='2e7a4a'), 'penguin': R(fowl, col='34384a', beak='f0a020', tailc='34384a'),
    'frog': R(frog, col='5ab84a'), 'toad': R(frog, col='8a7a4a', spots='5a4a2a'), 'frog_poison': R(frog, col='3a8ae0', spots='f0e040', belly='2a4a90'),
    'turtle': R(turtle, shell='4a8a4a'), 'turtle_sea': R(turtle, shell='3a8a9a', skin='8ac8b0'), 'turtle_spiked': R(turtle, shell='8a4a3a', skin='c8b060', spike=True),
    'fish': R(fish, col='e88a30', belly='f8e0b0'), 'fish_blue': R(fish, col='4a8ae0', fin='f0d040'), 'fish_silver': R(fish, col='c0c8d4'),
    'piranha': R(fish, col='c84a40', fin='7a2a2a', teeth=True), 'eel': R(fish, col='4a5a3a', long=2, belly='c8d060'), 'shark': R(fish, col='7a8a9a', long=1, teeth=True, belly='e8ecf0'),
}

def build(A):
    A.section('creatures')
    for name, (fn, kw) in CREATURES.items():
        A.sprite(name, fn(**kw), cat='creature')
