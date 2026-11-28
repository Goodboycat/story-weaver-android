"""Big creatures (32x32 = 2x2 tiles, atlas entry carries w/h) + a few 16x16 magical ones. Four directions; left = mirrored right.
Archetypes: brute (golems, giants), dragon, treant, kraken (top view), hydra, elemental, winged (a `quad` + wings), mimic."""
from core import *
from sprites import INK, BONE, STEEL, GOLD, px, humanoid
from creatures import _four, _top, quad, flyer, WH, REDE, YEL, R

def B(): return C(32, 32)
def tl(c, x0, y0, x1, y1, rp, w0=2.0, w1=None):
    """thick tapered limb"""
    n = int(max(abs(x1 - x0), abs(y1 - y0), 1)); w1 = w0 if w1 is None else w1
    for i in range(n + 1):
        t = i / n; c.ell(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, w0 + (w1 - w0) * t, w0 + (w1 - w0) * t, rp)

def brute(col, top=None, glow=None, one_eye=False, club=False, tusk=False, helm=None, crystal=None, belly=None, eye=INK):
    rp = ramp(col); g = ramp(glow)[3] if glow else None; e = g or eye
    def f(d):
        c = B(); hx = 15.5 + (3 if d == 'r' else 0)
        c.box(10, 23, 14, 29, rp); c.box(17, 23, 21, 29, rp); c.rect(9, 29, 14, 30, rp[1]); c.rect(17, 29, 22, 30, rp[1])
        if club and d != 'r': tl(c, 26, 23, 28, 9, ramp('7a5230'), 1, 2.5)
        if d != 'r': c.ell(6, 17, 3, 5.5, rp); c.ell(25, 17, 3, 5.5, rp); c.ell(6, 23, 2.5, 2.5, rp); c.ell(25, 23, 2.5, 2.5, rp)
        c.ell(15.5, 17, 8, 7, rp)
        if belly and d != 'u': c.ell(15.5 + (2 if d == 'r' else 0), 19, 4.5, 4, ramp(belly))
        if g: c.line(12, 14, 15, 18, g); c.line(15, 18, 13, 22, g); c.line(19, 13, 18, 16, g)
        if crystal:
            for x in (7, 24, 12): c.tri(x, 6 if x != 12 else 9, 13, 2, ramp(crystal))
        c.ell(hx, 8, 5, 4.5, ramp(helm) if helm else rp)
        if top: t = ramp(top); c.ell(hx, 5, 5, 2, t); c.ell(7, 12, 3, 1.5, t); c.ell(24, 12, 3, 1.5, t)
        if d == 'r':
            if club: tl(c, 19, 23, 26, 10, ramp('7a5230'), 1, 2.5)
            c.ell(17, 18, 3, 5.5, rp); c.ell(18, 23, 2.5, 2.5, rp)
            if helm: c.rect(19, 8, 23, 9, INK); c.p(21, 8, e); c.p(22, 8, e)
            elif one_eye: c.ell(21.5, 8, 1.5, 1.5, ramp('f4f4f4')); c.p(22, 8, e)
            else: c.rect(21, 7, 22, 8, e)
            if tusk: c.p(22, 11, WH); c.p(22, 10, WH)
        if d == 'd':
            if helm: c.rect(12, 8, 19, 9, INK); px(c, [(13, 8), (14, 8), (17, 8), (18, 8)], e)
            elif one_eye: c.ell(15.5, 8, 2.5, 2, ramp('f4f4f4')); c.rect(15, 8, 16, 9, e)
            else: c.rect(12, 7, 13, 8, e); c.rect(18, 7, 19, 8, e); c.rect(13, 11, 18, 11, rp[0])
            if tusk: px(c, [(13, 11), (13, 10), (18, 11), (18, 10)], WH)
        return c
    return _four(f)

def dragon(col, belly='e8d090', wingc=None, bone=False, legs=True, eye=YEL, horn=None):
    rp = ramp(col); bl = ramp(belly); wg = ramp(wingc or rp[1]); hn = ramp(horn)[3] if horn else BONE
    def wing(c, cx, cy, s):
        c.ell(cx, cy, 6, 8, wg)
        for k in (-4, 0, 4): c.line(cx - s * 5, cy - 7, cx + k, cy + 7, rp[1])
        for x in (cx - 3, cx + 1, cx + 5): c.clear(int(x), int(cy + 8)); c.clear(int(x), int(cy + 7))
    def f(d):
        c = B()
        if d == 'r':
            tl(c, 8, 20, 1, 27, rp, 2.5, .6)
            c.box(9, 24, 11, 29, rp); c.rect(9, 29, 12, 29, hn)
            if legs: c.box(16, 24, 18, 29, rp); c.rect(16, 29, 19, 29, hn)
            c.ell(13, 19, 8, 6, rp); c.rect(8, 23, 18, 24, bl[2]); tl(c, 19, 16, 23, 10, rp, 2.5, 2)
            c.ell(25, 8, 4, 3, rp); c.ell(28.5, 9, 2.5, 2, rp); c.p(30, 8, INK); c.rect(26, 6, 27, 7, eye); c.line(22, 5, 20, 2, hn); c.line(23, 5, 21, 2, hn)
            c.ell(11, 9, 8, 6, wg)
            for k in (4, 10, 16): c.line(18, 13, k, 4 + (k % 3), rp[1])
            return c
        wing(c, 6, 12, -1); wing(c, 25, 12, 1)
        if d == 'u': tl(c, 15.5, 24, 15.5, 30, rp, 2.5, .6)
        c.ell(11, 27, 2.5, 1.5, rp); c.ell(20, 27, 2.5, 1.5, rp); c.ell(15.5, 19, 6, 8, rp)
        if d == 'd': c.ell(15.5, 21, 3.5, 5, bl)
        else: px(c, [(15, y) for y in range(12, 26, 2)] + [(16, y) for y in range(12, 26, 2)], rp[0])
        c.ell(15.5, 9.5, 4.5, 4, rp); c.line(12, 6, 10, 2, hn); c.line(19, 6, 21, 2, hn); c.line(11, 6, 9, 2, hn); c.line(20, 6, 22, 2, hn)
        if d == 'd': c.ell(15.5, 13, 3, 2.5, rp); px(c, [(14, 14), (17, 14)], INK); c.rect(12, 9, 13, 10, eye); c.rect(18, 9, 19, 10, eye)
        return c
    return _four(f)

def treant(leaf, bark='7a5a3a', bare=False, snow=False, eye=YEL):
    bk = ramp(bark); lf = ramp(leaf)
    def f(d):
        c = B(); s = 3 if d == 'r' else 0
        tl(c, 10, 18, 4, 23, bk, 1.5, 1); tl(c, 21, 18, 27, 23, bk, 1.5, 1)
        c.box(11, 13, 20, 28, bk); px(c, [(9, 29), (10, 28), (10, 29), (21, 28), (21, 29), (22, 29), (15, 29)], bk[1])
        for y in range(15, 28, 3): c.p(12 + y % 4, y, bk[0]); c.p(18 - y % 3, y + 1, bk[0])
        if bare:
            for x1, y1 in ((4, 3), (10, 1), (16, 2), (22, 1), (28, 4), (7, 8), (25, 8)): tl(c, 15.5, 13, x1, y1, bk, 1.3, .5)
        else:
            c.ell(6, 11, 5, 4, lf); c.ell(25, 11, 5, 4, lf); c.ell(15.5, 8, 11, 7, lf)
            for i in range(40): c.p(5 + ri(i, 1, 7, 22), 2 + ri(i, 2, 7, 12), lf[1 + ri(i, 3, 7, 3)]) if c.get(5 + ri(i, 1, 7, 22), 2 + ri(i, 2, 7, 12)) else None
        if snow: c.ell(15.5, 3.5, 8, 2, ramp('f0f4f8')); c.ell(6, 8, 3, 1.2, ramp('f0f4f8')); c.ell(25, 8, 3, 1.2, ramp('f0f4f8'))
        if d != 'u':
            c.rect(13 + s, 18, 14 + s, 19, eye); c.rect(17 + s, 18, 18 + s, 19, eye) if d == 'd' else None
            c.rect(13 + s, 23, 18 if d == 'd' else 19, 23, INK); c.p(13 + s, 22, INK)
        return c
    return _four(f)

def kraken(col, sucker='f0c0c8', eye=YEL):
    rp = ramp(col); sk = ramp(sucker)[3]
    def f():
        c = B()
        for i, (x1, y1, mx) in enumerate(((2, 12, 3), (29, 12, 28), (1, 24, 5), (30, 24, 26), (7, 30, 9), (24, 30, 22), (13, 31, 13), (18, 31, 18))):
            tl(c, 15.5, 16, mx, 18 + i, rp, 2.2, 1.6); tl(c, mx, 18 + i, x1, y1, rp, 1.6, .6); c.p(mx, 18 + i, sk); c.p((mx + x1) // 2, (18 + i + y1) // 2, sk)
        c.ell(15.5, 9, 6.5, 8.5, rp); px(c, [(13, 4), (17, 6), (14, 9), (18, 3)], rp[1])
        c.rect(11, 14, 12, 15, eye); c.rect(19, 14, 20, 15, eye); c.p(12, 15, INK); c.p(19, 15, INK)
        return c
    return _top(f)

def hydra(col, belly='d8d890', eye=REDE):
    rp = ramp(col)
    def f(d):
        c = B(); s = 4 if d == 'r' else 0
        if d != 'd': tl(c, 10, 24, 2, 29, rp, 2.5, .6)
        for x in (9, 14, 19): c.box(x, 26, x + 2, 30, rp)
        c.ell(15.5, 22, 9, 6, rp)
        if d != 'u': c.ell(15.5 + s, 24, 5, 3, ramp(belly))
        for x0, x1, y1 in ((10, 4, 9), (21, 27, 9), (15.5, 15.5, 5)):
            x1 = min(27, x1 + s); tl(c, x0, 19, x1, y1, rp, 3.2, 2.6); c.ell(x1, y1 - 1, 3.6, 3, rp)
            if d != 'u': c.ell(x1 + (2 if d == 'r' else 0), y1 + 1.5, 2.4, 1.6, rp)   # jaw / snout
            if d == 'd': px(c, [(x1 - 1.5, y1 - 2), (x1 + 1.5, y1 - 2)], eye); c.rect(int(x1 - 1), y1 + 2, int(x1 + 1.5), y1 + 2, INK)
            if d == 'r': c.p(x1 + 1, y1 - 2, eye); c.rect(int(x1 + 2), y1 + 2, int(x1 + 4), y1 + 2, INK)
        return c
    return _four(f)

def elemental(col, core='ffffff', rocks=None):
    rp = ramp(col); k = ramp(core)[3]
    def f(d):
        c = C()
        for y in range(9, 15):
            w = (15 - y) * .7
            for x in range(int(8 - w), int(8 + w)): c.p(x + (y % 2 if y > 11 else 0), y, rp[1 + (x + y) % 3])
        c.ell(7.5, 8, 4.5, 3, rp); c.ell(7.5, 4.5, 3, 3, rp)                      # torso + head (not one diamond blob)
        for sx in (-1, 1):                                                        # arms hanging out to the sides, fists low (raised fists read as horns - keep them low)
            ax = int(7.5 + sx * 4); bx = int(7.5 + sx * 6); c.line(ax, 7, bx, 8, rp[2]); c.line(ax, 8, bx, 9, rp[1]); c.ell(7.5 + sx * 6, 10, 1.5, 1.5, rp)
        px(c, [(7, 1), (4, 2), (11, 3)], rp[4])
        if rocks: r = ramp(rocks); px(c, [(4, 4), (5, 4), (10, 9), (11, 9), (6, 11), (3, 8)], r[1]); px(c, [(4, 3), (10, 8)], r[3])
        if d == 'd': px(c, [(6, 4), (6, 5), (9, 4), (9, 5)], k); c.rect(6, 8, 9, 9, k)
        if d == 'r': px(c, [(8, 4), (8, 5), (10, 4), (10, 5)], k)
        return c
    return _four(f)

def winged(base, wing, horn=None, long=0):
    wg = ramp(wing); out = []
    for i, b in enumerate(quad(**base)):
        c = C()
        if i in (0, 3): c.ell(2, 7, 2, 3.5, wg); c.ell(13, 7, 2, 3.5, wg)
        else: c.ell(6.5, 5.5, 3.5, 3, wg); px(c, [(4, 7), (6, 8), (8, 7)], wg[1])
        c.outline()
        if i == 0: c.blit(b)
        else: b.blit(c); c = b
        if i in (1, 2): c = c.flip() if i == 1 else c
        if horn:
            h = ramp(horn)[3]
            if i == 0: px(c, [(7, 3 - long), (8, 2 - long), (8, 1 - long)], h)
            if i == 2: px(c, [(14, 3 - long), (15, 2 - long)], h)
            if i == 1: px(c, [(1, 3 - long), (0, 2 - long)], h)
        out.append(c)
    out[1] = out[1].flip() if not horn else out[2].flip()
    return out

def mimic(col, trim='e0b040'):
    rp = ramp(col); t = ramp(trim)
    def f(d):
        c = C(); c.box(2, 8, 13, 13, rp); c.box(2, 2, 13, 6, rp); c.rect(2, 4, 13, 4, t[2]); c.rect(2, 11, 13, 11, t[2]); c.rect(3, 7, 12, 7, INK)
        if d != 'u':
            px(c, [(x, 7) for x in range(3, 13, 2)] + [(x, 8) for x in range(4, 13, 2)], WH); px(c, [(5, 6), (10, 6)], YEL)
            c.rect(7, 8, 8, 10, REDE); c.p(7, 11, REDE)
        else: c.rect(7, 9, 8, 10, t[3])
        return c
    return _four(f)

HORSE = dict(tail='horse', sz=1, long=2)
CREATURES2 = {
    'golem_stone': R(brute, col='8a8a90', glow='60d0f0'), 'golem_iron': R(brute, col='6a7484', helm='8a94a4', glow='f0a040'),
    'golem_ice': R(brute, col='a8dcf0', crystal='e0f8ff', eye=(40, 80, 160)), 'golem_lava': R(brute, col='4a3a3a', glow='f87030', top='f85020'),
    'golem_crystal': R(brute, col='7a5ab0', crystal='e0a0f8', glow='f0c0ff'), 'golem_moss': R(brute, col='7a7a70', top='5a9a3a', glow='b0f060'),
    'golem_clay': R(brute, col='b87a50', eye=INK), 'golem_gold': R(brute, col='e0b040', glow='ffffff', helm='f0d060'),
    'golem_sand': R(brute, col='e0c888', top='c8a860'), 'living_statue': R(brute, col='d8d8dc', helm='c0c0c8', eye=(60, 200, 220)),
    'troll': R(brute, col='6a9a5a', tusk=True, club=True, belly='a8c088'), 'troll_cave': R(brute, col='7a7a8a', tusk=True, belly='a8a8b8', eye=REDE),
    'troll_frost': R(brute, col='b8d8e8', tusk=True, club=True, top='f0f4f8'), 'cyclops': R(brute, col='d0a070', one_eye=True, club=True, belly='e8c8a0'),
    'giant': R(brute, col='e0b088', top='6a4226', club=True, belly='8a6a4a'), 'giant_frost': R(brute, col='a0c0e0', top='f0f4f8', belly='6080b0'),
    'giant_fire': R(brute, col='c86a48', top='f89030', glow='f8d040'), 'yeti': R(brute, col='f0f0f4', belly='c8d8e8', eye=(60, 120, 200)),
    'corrupted_champion': R(brute, col='3a3448', helm='4a4458', glow='c040e0', top='2a2434'),
    'dragon_red': R(dragon, col='c83a30'), 'dragon_green': R(dragon, col='4a9a40', belly='d0e090'), 'dragon_blue': R(dragon, col='3a70c8', belly='c0e0f0'),
    'dragon_black': R(dragon, col='3a3644', belly='7a6a8a', eye=REDE), 'dragon_white': R(dragon, col='e8ecf4', belly='c0d8f0', wingc='b0c8e8', eye=(60, 120, 220)),
    'dragon_gold': R(dragon, col='e0b040', belly='f8e8a0', wingc='e88a30'), 'dragon_bone': R(dragon, col='d8d0b8', belly='b0a890', wingc='8a8474', eye=(80, 240, 120)),
    'dragon_crystal': R(dragon, col='9a6ad0', belly='e0c0f8', wingc='60c0e0'),
    'wyvern': R(dragon, col='7a8a4a', belly='d0c890', legs=False), 'wyvern_swamp': R(dragon, col='4a6a5a', belly='a0b890', legs=False, eye=REDE),
    'treant': R(treant, leaf='4a9a3a'), 'treant_autumn': R(treant, leaf='d87a28'), 'treant_snowy': R(treant, leaf='5a8a6a', snow=True, eye=(120, 220, 250)),
    'treant_dead': R(treant, leaf='5a4a3a', bark='5a4a44', bare=True, eye=REDE), 'treant_blossom': R(treant, leaf='f0a0c0', bark='8a5a4a'),
    'kraken': R(kraken, col='c85a70'), 'kraken_deep': R(kraken, col='3a4a8a', sucker='80e0e0', eye=(120, 250, 220)),
    'hydra': R(hydra, col='4a8a6a'), 'hydra_swamp': R(hydra, col='6a6a3a', belly='c8b870', eye=YEL), 'hydra_frost': R(hydra, col='80b8e0', belly='e0f0f8', eye=WH),
    'elemental_fire': R(elemental, col='f07030', core='fff0a0'), 'elemental_water': R(elemental, col='3a90e0', core='e0f8ff'),
    'elemental_earth': R(elemental, col='8a6a4a', core='f0e060', rocks='6a6a6a'), 'elemental_air': R(elemental, col='c8e0f0', core='ffffff'),
    'elemental_ice': R(elemental, col='90d8f0', core='ffffff', rocks='e0f8ff'), 'elemental_lightning': R(elemental, col='f0e040', core='ffffff'),
    'elemental_shadow': R(elemental, col='4a3a5a', core='e060f0'), 'elemental_nature': R(elemental, col='5aa84a', core='f8f0a0', rocks='f080b0'),
    'griffin': R(winged, base=dict(col='d8a850', ear='point', tail='thin', sz=1, snout='ecc040', mane='f0ece0'), wing='f0ece0'),
    'hippogriff': R(winged, base=dict(col='8a6a4a', snout='ecc040', **HORSE), wing='b89a6a', long=2),
    'pegasus': R(winged, base=dict(col='f4f0ec', mane='c8d8f0', tailc='c8d8f0', **HORSE), wing='f8f8fc', long=2),
    'unicorn': R(winged, base=dict(col='f8f4f0', mane='f0a0d0', tailc='a0c8f0', **HORSE), wing='f8f4f0', horn='f0d040', long=2),
    'chimera': R(winged, base=dict(col='c88a40', ear='round', tail='thin', sz=1, mane='8a4a28', tailc='4a8a4a'), wing='7a4a5a', horn='e8dcc0'),
    'mimic': R(mimic, col='8a5a30'), 'mimic_gold': R(mimic, col='e0b040', trim='f8f0c0'), 'mimic_iron': R(mimic, col='7a8494', trim='c8d0dc'),
    'gargoyle': R(flyer, col='8a8a94', wing='6a6a74', bat=True, ear=True, sz=1, eye=YEL), 'phoenix': R(flyer, col='f07030', wing='f8c040', beak='f8e080', sz=2, wing2='f85030'),
    'thunderbird': R(flyer, col='3a5ac8', wing='f0e040', beak='f8f0a0', sz=2, wing2='ffffff'), 'roc': R(flyer, col='b89a6a', wing='8a6a4a', beak='ecc040', sz=2),
    'ice_bird': R(flyer, col='c0e8f8', wing='80c0f0', beak='f0f8ff', sz=2, wing2='ffffff'),
}

def build(A):
    A.section('creatures2')
    for name, (fn, kw) in CREATURES2.items():
        A.sprite(name, fn(**kw), cat='creature_big' if fn in (brute, dragon, treant, kraken, hydra) else 'creature')
    A.sprite('living_armor', humanoid('grey', None, '8a94a8', hat='greathelm', item='sword', eye=(240, 200, 60)), cat='creature')
    A.sprite('living_armor_dark', humanoid('grey', None, '3a3448', hat='greathelm', item='axe', eye=(200, 60, 220)), cat='creature')
