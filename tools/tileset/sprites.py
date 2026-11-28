"""People + creatures. 16x16, four directions (down, left, right, up); left = mirrored right.
One row in PEOPLE = one humanoid: name: (skin, hair, hair_style, outfit, hat, item, extras)."""
from core import *

INK = (34, 28, 44); BONE = (226, 220, 196); STEEL = (176, 186, 200); GOLD = (236, 190, 60); WOODC = (134, 90, 50)
SKIN = dict(fair='f2c9a0', tan='d9a273', brown='a56a43', dark='74482e', pale='e6e2ea', orc='7fa35a', goblin='9ab84e',
            blue='78b4d8', teal='5fc0a8', grey='9a9aa6', bone='e2dcc4', red='c8705a', lizard='5fa070', fur='c89a5e',
            furgrey='a0a0a8', furwhite='ece8e0', furdark='5a4638', bark='8a6a48', violet='9a82c8', rot='8fa58a', gold='e8c860')
HAIR = dict(brown='6b4226', black='2e2a34', blond='e6c35a', red='b8482a', white='e8e8ee', grey='9a9aa2', blue='4a78c8',
            green='4a9a4e', pink='e68ab0', purple='7a4ab0', teal='3aa89a', orange='e08a30', silver='c8d0dc')

def _col(v, table):
    return ramp(table.get(v, v) if isinstance(v, str) else v)

def px(c, pts, col):
    for x, y in pts: c.p(x, y, col)

def _item(name, col):
    """item in the right hand for the DOWN view (hand at x=11,y=11); returns canvas"""
    c = C(); w = ramp(WOODC); st = ramp(STEEL); k = ramp(col) if col else st
    if name == 'sword': c.rect(13, 5, 13, 10, st[3]); c.p(13, 4, st[4]); c.rect(12, 11, 14, 11, GOLD); c.p(13, 12, w[1])
    elif name == 'dagger': c.rect(13, 8, 13, 10, st[3]); c.rect(12, 11, 14, 11, w[1])
    elif name in ('staff', 'spear', 'broom', 'scythe', 'hoe', 'trident', 'banner'):
        c.rect(13, 3, 13, 14, w[2])
        if name == 'staff': c.rect(12, 2, 14, 3, k[2]); c.p(13, 1, k[3]); c.p(12, 2, k[4])
        if name == 'spear': c.rect(13, 1, 13, 3, st[3]); c.p(12, 3, st[2]); c.p(14, 3, st[2])
        if name == 'broom': c.rect(12, 1, 14, 4, ramp('d8b860')[2]); c.rect(12, 4, 14, 4, ramp('d8b860')[1])
        if name == 'scythe': c.rect(10, 2, 13, 2, st[3]); c.p(10, 3, st[2]); c.p(9, 4, st[2])
        if name == 'hoe': c.rect(11, 3, 13, 3, st[2]); c.p(11, 4, st[1])
        if name == 'trident': c.rect(12, 3, 14, 3, k[2]); px(c, [(12, 1), (12, 2), (14, 1), (14, 2), (13, 1), (13, 2)], k[3])
        if name == 'banner': c.rect(10, 2, 12, 5, k[2]); c.rect(10, 5, 12, 5, k[1]); c.p(11, 3, k[4])
    elif name == 'bow': px(c, [(12, 4), (13, 5), (14, 6), (14, 7), (14, 8), (14, 9), (14, 10), (13, 11), (12, 12)], w[2]); c.rect(12, 5, 12, 11, BONE)
    elif name == 'shield': c.rect(11, 9, 14, 13, k[2]); c.rect(11, 9, 14, 9, k[3]); c.rect(14, 10, 14, 13, k[1]); c.clear(11, 13); c.clear(14, 13); c.rect(12, 10, 13, 11, GOLD)
    elif name == 'axe': c.rect(13, 5, 13, 13, w[2]); c.rect(11, 5, 12, 7, st[3]); c.p(11, 8, st[2]); c.p(14, 6, st[2])
    elif name == 'hammer': c.rect(13, 6, 13, 13, w[2]); c.rect(11, 4, 14, 6, st[1]); c.rect(11, 4, 14, 4, st[3])
    elif name == 'pick': c.rect(13, 5, 13, 13, w[2]); c.rect(11, 5, 14, 5, st[3]); c.p(10, 6, st[2]); c.p(14, 6, st[2])
    elif name == 'book': c.rect(11, 10, 14, 13, k[2]); c.rect(11, 10, 11, 13, k[1]); c.rect(12, 11, 13, 11, GOLD)
    elif name == 'lute': c.rect(11, 10, 13, 13, w[3]); c.p(12, 11, INK); c.line(13, 9, 14, 6, w[1])
    elif name == 'wand': c.rect(13, 8, 13, 11, w[1]); c.p(13, 7, k[4]); px(c, [(12, 7), (14, 7), (13, 6)], k[3])
    elif name == 'basket': c.rect(11, 11, 14, 13, ramp('c8a060')[2]); c.rect(11, 13, 14, 13, ramp('c8a060')[1]); c.rect(12, 10, 13, 10, k[2])
    elif name == 'rod': c.line(12, 12, 14, 3, w[2]); c.rect(14, 4, 14, 9, BONE); c.p(14, 10, k[2])
    elif name == 'torch': c.rect(13, 8, 13, 12, w[1]); c.rect(12, 5, 14, 7, (240, 140, 40)); c.p(13, 4, (250, 220, 90)); c.p(13, 6, (250, 220, 90))
    elif name == 'mug': c.rect(12, 10, 13, 12, w[3]); c.rect(12, 10, 13, 10, (240, 240, 230)); c.p(14, 11, w[1])
    elif name == 'flask': c.rect(12, 11, 14, 13, k[2]); c.p(13, 10, STEEL); c.p(13, 9, w[1]); c.p(12, 11, k[4])
    elif name == 'club': c.rect(13, 8, 13, 12, w[1]); c.rect(12, 4, 14, 8, w[2]); c.p(12, 5, w[4])
    elif name == 'orb': c.rect(12, 10, 14, 12, k[2]); c.p(12, 10, k[4]); c.p(14, 12, k[1])
    elif name == 'lantern': c.p(13, 9, st[1]); c.rect(12, 10, 14, 12, st[1]); c.p(13, 11, (250, 220, 90))
    elif name == 'bag': c.rect(11, 10, 14, 13, k[2]); c.rect(12, 10, 13, 10, k[0]); c.rect(11, 13, 14, 13, k[1])
    return c

class Off(C):
    """canvas whose drawing is shifted 1px down, so head gear may use row -1"""
    def p(s, x, y, c, al=255): C.p(s, x, y + 1, c, al)

def _hat(c, d, hat, k, hr):
    """d in 'd','r','u'; k = hat ramp; coordinates before the global 1px shift down"""
    if hat == 'hood':
        c.rect(5, 1, 10, 1, k[3]); c.rect(4, 2, 11, 3, k[2])
        if d == 'd': c.rect(4, 4, 4, 8, k[1]); c.rect(11, 4, 11, 8, k[1]); c.rect(5, 4, 10, 4, k[1])
        if d == 'r': c.rect(4, 4, 7, 8, k[2]); c.rect(4, 8, 7, 8, k[1]); c.rect(8, 4, 11, 4, k[1])
        if d == 'u': c.rect(4, 4, 11, 8, k[2]); c.rect(7, 2, 8, 7, k[1])
    elif hat in ('wizard', 'witch'):
        c.rect(6, 1, 9, 2, k[2]); c.rect(7, 0, 8, 0, k[3]); c.p(9, -1, k[2]); c.rect(3, 3, 12, 3, k[1]); c.rect(5, 2, 10, 2, k[2])
        if hat == 'wizard' and d != 'u': c.p(7, 1, GOLD)
        if hat == 'witch': c.rect(5, 2, 10, 2, ramp('7a4ab0')[2])
    elif hat in ('helmet', 'horned', 'greathelm'):
        c.rect(5, 1, 10, 1, k[4]); c.rect(4, 2, 11, 4, k[2]); c.rect(4, 4, 11, 4, k[1]); c.rect(5, 2, 7, 2, k[4])
        if d == 'd': c.p(4, 5, k[1]); c.p(11, 5, k[1]); c.rect(7, 5, 8, 5, k[1])
        if d == 'r': c.rect(4, 5, 6, 6, k[1])
        if d == 'u': c.rect(4, 5, 11, 7, k[2]); c.rect(4, 7, 11, 7, k[1])
        if hat == 'greathelm':
            if d == 'd': c.rect(4, 5, 11, 8, k[2]); c.rect(5, 6, 10, 6, INK); c.rect(7, 5, 8, 8, k[3]); c.rect(4, 8, 11, 8, k[1])
            if d == 'r': c.rect(4, 5, 11, 8, k[2]); c.rect(8, 6, 11, 6, INK); c.rect(4, 8, 11, 8, k[1])
            if d == 'u': c.rect(4, 8, 11, 8, k[1])
        if hat == 'horned':
            if d != 'r': px(c, [(3, 2), (3, 1), (2, 0), (12, 2), (12, 1), (13, 0)], BONE)
            else: px(c, [(7, 2), (7, 1), (6, 0)], BONE)
        if hat == 'helmet' and hr is not None: c.rect(7, 0, 8, 1, hr[2]) if d != 'r' else c.rect(5, 0, 9, 0, hr[2])
    elif hat == 'crown':
        c.rect(5, 2, 10, 2, GOLD); px(c, [(5, 1), (7, 1), (8, 1), (10, 1)], GOLD); px(c, [(7, 2)] if d == 'r' else [(7, 2), (8, 2)], k[2])
    elif hat == 'tiara':
        c.rect(5, 2, 10, 2, GOLD); c.p(7 if d != 'r' else 10, 1, k[3])
    elif hat in ('cap', 'feather'):
        c.rect(5, 1, 10, 1, k[3]); c.rect(4, 2, 11, 2, k[2]); c.rect(4, 3, 11, 3, k[1])
        if d == 'r': c.rect(12, 3, 13, 3, k[1])
        if hat == 'feather': px(c, [(10, 0), (11, -1), (10, 1)], (230, 70, 60))
    elif hat == 'straw':
        c.rect(5, 1, 10, 2, k[3]); c.rect(2, 3, 13, 3, k[2]); c.rect(3, 4, 12, 4, k[1]) if d == 'u' else None; c.rect(5, 2, 10, 2, k[1])
    elif hat == 'bandana':
        c.rect(5, 1, 10, 1, k[3]); c.rect(4, 2, 11, 3, k[2]); c.rect(4, 3, 11, 3, k[1])
        if d == 'r': px(c, [(3, 3), (3, 4), (2, 5)], k[2])
        if d == 'u': px(c, [(7, 4), (8, 4), (7, 5), (9, 5)], k[1])
    elif hat == 'circlet':
        c.rect(4, 3, 11, 3, GOLD)
        if d == 'd': c.rect(7, 3, 8, 3, k[3])
    elif hat == 'turban':
        c.rect(5, 0, 10, 0, k[3]); c.rect(4, 1, 11, 3, k[2]); c.rect(4, 3, 11, 3, k[1]); c.line(5, 1, 9, 3, k[1])
        if d == 'd': c.rect(7, 2, 8, 2, GOLD)
    elif hat == 'chef':
        c.rect(4, -1, 11, 1, (244, 244, 240)); c.rect(5, 2, 10, 2, (210, 210, 214)); px(c, [(4, -1), (11, -1)], (210, 210, 214))
    elif hat == 'tricorn':
        c.rect(5, 1, 10, 2, k[2]); c.rect(3, 3, 12, 3, k[1]); px(c, [(3, 2), (12, 2)], k[1]); c.p(7 if d != 'r' else 10, 2, BONE) if d != 'u' else None
    elif hat == 'mask':
        if d == 'd': c.rect(4, 7, 11, 8, k[2]); c.rect(5, 8, 10, 8, k[1])
        if d == 'r': c.rect(8, 7, 11, 8, k[2])
        c.rect(5, 1, 10, 1, k[2]); c.rect(4, 2, 11, 3, k[2]); c.rect(4, 4, 11, 4, k[1]) if d == 'u' else None
    elif hat == 'antlers':
        pts = [(4, 1), (3, 0), (3, -1), (5, -1), (11, 1), (12, 0), (12, -1), (10, -1)] if d != 'r' else [(7, 1), (7, 0), (6, -1), (8, -1)]
        px(c, pts, ramp(WOODC)[3])
    elif hat == 'leaf':
        c.rect(4, 2, 11, 2, k[2]); px(c, [(5, 1), (7, 1), (9, 1), (10, 2)], k[3]); px(c, [(6, 2), (9, 2)], (230, 90, 110))

def humanoid(skin, hair, outfit, hat=None, item=None, hs='short', hatc=None, itemc=None, pants=None, belt=None, ears=None, beard=None,
             tusks=False, tail=None, wings=None, short=False, skull=False, cape=None, legs='legs', eye=INK, snout=None, shoes='4a3628',
             apron=None, glow=None):
    sk = _col(skin, SKIN); hr = _col(hair, HAIR) if hair else None; of = ramp(outfit); pt = ramp(pants) if pants else ramp(of[0])
    k = ramp(hatc) if hatc else of; sh = ramp(shoes)[2]; o = 1 if short else 0
    out = {}
    for d in 'dru':
        c = C()
        # wings / cape / tail behind
        if wings and d != 'u':
            wr = ramp(wings); pts = [(x, y) for x in (1, 2, 3) for y in range(7, 12)] + [(2, 6), (3, 6), (2, 12)]
            if d == 'd': pts = pts + [(15 - x, y) for x, y in pts]
            else: pts = [(x + 2, y) for x, y in pts]
            for x, y in pts: c.p(x, y + o, wr[4] if (x + y) % 3 else wr[3], 200)
        if cape and d == 'd': cr = ramp(cape); c.rect(4, 9 + o, 11, 13, cr[1])
        if tail and d == 'd': px(c, [(11, 13), (12, 13), (12, 12), (13, 11)], ramp(tail)[2])
        if tail and d == 'r': px(c, [(5, 13), (4, 13), (3, 12), (3, 11), (2, 10)], ramp(tail)[2])
        # legs
        if legs == 'legs':
            if d == 'r':
                if not short: c.rect(6, 13, 9, 13, pt[2])
                c.rect(6, 14, 7, 14, sh); c.rect(8, 14, 10, 14, sh)
            else:
                if not short: c.rect(5, 13, 10, 13, pt[2]); c.rect(7, 13, 8, 13, pt[1])
                c.rect(5, 14, 6, 14, sh); c.rect(9, 14, 10, 14, sh)
        elif legs == 'robe':
            x0, x1 = (6, 9) if d == 'r' else (5, 10)
            c.rect(x0, 13, x1, 14, of[2]); c.rect(x0 - 1, 14, x1 + 1, 14, of[1])
        elif legs == 'fish':
            tr = ramp(tail or '3aa89a')
            if d == 'r': c.rect(5, 13, 9, 13, tr[2]); c.rect(3, 14, 7, 14, tr[2]); px(c, [(2, 13), (2, 14), (1, 12), (1, 14)], tr[3])
            else: c.rect(6, 13, 9, 13, tr[2]); c.rect(7, 14, 8, 14, tr[1]); px(c, [(5, 14), (6, 14), (9, 14), (10, 14), (4, 13), (11, 13)], tr[3])
        elif legs == 'ghost':
            x0, x1 = (6, 9) if d == 'r' else (5, 10)
            c.rect(x0, 13, x1, 13, of[2]); px(c, [(x0, 14), (x0 + 2, 14), (x1 - 1, 14)], of[1])
        # torso + arms
        ty = 9 + o
        if d == 'r':
            c.rect(6, ty, 9, 12, of[2]); c.rect(6, 12, 9, 12, of[1]); c.rect(7, ty + 1, 8, 11, of[1]); c.p(8, 12 if short else 11, sk[2]); c.p(9, ty, of[3])
            if belt: c.rect(6, 12, 9, 12, ramp(belt)[2])
            if cape: cr = ramp(cape); c.rect(5, ty, 6, 13, cr[2]); c.rect(5, 13, 6, 13, cr[1])
            if apron: c.rect(9, ty + 1, 9, 13, ramp(apron)[2])
        else:
            c.rect(5, ty, 10, 12, of[2]); c.rect(5, 12, 10, 12, of[1]); c.rect(5, ty, 10, ty, of[3])
            c.rect(4, ty, 4, 11, of[1]); c.rect(11, ty, 11, 11, of[1]); c.p(4, 11 + o, sk[2]); c.p(11, 11 + o, sk[2])
            if belt: c.rect(5, 12, 10, 12, ramp(belt)[2]); (c.rect(7, 12, 8, 12, GOLD) if d == 'd' else None)
            if apron and d == 'd': ar = ramp(apron); c.rect(6, ty + 1, 9, 13, ar[2]); c.rect(6, 13, 9, 13, ar[1])
            if d == 'd' and not apron: c.rect(7, ty, 8, ty, sk[1]) if not cape else c.rect(7, ty, 8, ty, GOLD)
            if cape and d == 'u': cr = ramp(cape); c.rect(5, ty, 10, 13, cr[2]); c.rect(5, 13, 10, 13, cr[1]); c.rect(7, ty, 8, 13, cr[1])
        if tail and d == 'u': px(c, [(8, 12), (9, 13), (10, 13), (11, 12)], ramp(tail)[2])
        if wings and d == 'u':
            wr = ramp(wings)
            for x in (2, 3, 4, 5, 10, 11, 12, 13):
                for y in range(7, 12): c.p(x, y + o, wr[4] if (x + y) % 3 else wr[3], 200)
        # head
        hy = o
        c.rect(4, 2 + hy, 11, 8 + hy, sk[2]); c.rect(4, 5 + hy, 11, 6 + hy, sk[3]); c.rect(5, 8 + hy, 10, 8 + hy, sk[1])
        for x, y in ((4, 2), (11, 2), (4, 8), (11, 8)): c.clear(x, y + hy)
        sub = Off()   # head decorations (face, hair, ears, hat): drawn at hy=0, blitted after the global shift
        if d == 'd':
            if skull: px(sub, [(6, 5), (6, 6), (9, 5), (9, 6), (6, 8), (8, 8), (10, 8)], INK); px(sub, [(7, 7), (8, 7)], sk[0])
            else: px(sub, [(6, 6), (9, 6)], eye); px(sub, [(6, 5), (9, 5)], sk[2])
            if glow: px(sub, [(6, 6), (9, 6)], glow)
            if snout: sr = ramp(snout); sub.rect(6, 7, 9, 8, sr[3]); sub.rect(7, 7, 8, 7, INK)
            if tusks: px(sub, [(6, 8), (9, 8)], (250, 250, 240))
            if beard: br = _col(beard, HAIR); sub.rect(5, 8, 10, 9, br[2]); sub.rect(4, 7, 4, 8, br[2]); sub.rect(11, 7, 11, 8, br[2]); sub.rect(6, 10, 9, 10, br[1]); px(sub, [(5, 7), (10, 7)], br[2])
        if d == 'r':
            if skull: px(sub, [(9, 5), (9, 6), (10, 8), (8, 8)], INK)
            else: sub.p(9, 6, eye)
            if glow: sub.p(9, 6, glow)
            if snout: sr = ramp(snout); sub.rect(10, 7, 12, 8, sr[3]); sub.p(12, 7, INK)
            if tusks: sub.p(10, 8, (250, 250, 240))
            if beard: br = _col(beard, HAIR); sub.rect(8, 8, 11, 9, br[2]); sub.rect(9, 10, 10, 10, br[1]); sub.p(7, 7, br[2])
        if hr is not None and hs != 'bald':
            sub.rect(5, 1, 10, 1, hr[3]); sub.rect(4, 2, 11, 3, hr[2])
            if hs == 'spiky': px(sub, [(5, 0), (7, 0), (9, 0), (11, 1), (4, 1)], hr[3])
            if d == 'd':
                px(sub, [(4, 4), (5, 4), (10, 4), (11, 4), (4, 5), (11, 5)], hr[2])
                if hs in ('long', 'pony'): sub.rect(4, 6, 4, 9, hr[1]); sub.rect(11, 6, 11, 9, hr[1])
                if hs == 'long': sub.rect(3, 4, 3, 9, hr[2]); sub.rect(12, 4, 12, 9, hr[2])
                if hs == 'bun': sub.rect(7, 0, 8, 0, hr[2])
                if hs == 'fringe': sub.rect(4, 4, 11, 4, hr[2]); sub.rect(5, 5, 6, 5, hr[1])
            if d == 'r':
                sub.rect(4, 4, 6, 6, hr[2]); sub.rect(7, 4, 9, 4, hr[2]); sub.p(4, 7, hr[1])
                if hs == 'long': sub.rect(3, 3, 6, 10, hr[2]); sub.rect(3, 10, 6, 10, hr[1])
                if hs == 'pony': px(sub, [(3, 4), (2, 5), (2, 6), (2, 7), (3, 8)], hr[2])
                if hs == 'bun': sub.rect(3, 2, 3, 3, hr[2]); sub.p(2, 2, hr[1])
            if d == 'u':
                sub.rect(4, 4, 11, 7, hr[2]); sub.rect(5, 7, 10, 7, hr[1]); sub.rect(7, 2, 8, 6, hr[1]) if hs != 'spiky' else None
                if hs == 'long': sub.rect(3, 4, 12, 9, hr[2]); sub.rect(4, 10, 11, 10, hr[1]); sub.rect(7, 3, 8, 9, hr[1])
                if hs == 'pony': sub.rect(7, 7, 8, 11, hr[2]); sub.p(8, 11, hr[1])
                if hs == 'bun': sub.rect(7, 0, 8, 0, hr[2])
        if ears == 'elf':
            px(sub, [(3, 6), (2, 5), (12, 6), (13, 5)] if d != 'r' else [(6, 6), (5, 5), (4, 4)], sk[2])
        if ears in ('cat', 'wolf', 'fox'):
            er = hr or sk
            if d != 'r': px(sub, [(4, 1), (5, 1), (4, 0), (10, 1), (11, 1), (11, 0)], er[2]); (px(sub, [(5, 1), (10, 1)], (240, 170, 170)) if d == 'd' else None)
            else: px(sub, [(6, 1), (7, 1), (6, 0), (9, 1), (9, 0)], er[2])
        if ears == 'rabbit':
            er = hr or sk
            if d != 'r': sub.rect(5, -1, 5, 1, er[3]); sub.rect(10, -1, 10, 1, er[3])
            else: sub.rect(6, -1, 6, 1, er[3]); sub.rect(8, -1, 8, 1, er[2])
        if ears == 'fin':
            fr = ramp(tail or '3aa89a'); px(sub, [(3, 5), (3, 6), (2, 5), (12, 5), (12, 6), (13, 5)] if d != 'r' else [(5, 5), (4, 5), (4, 6), (3, 4)], fr[3])
        if ears == 'crest':
            fr = ramp(tail or 'c84a3a'); px(sub, [(7, 0), (8, 0), (7, 1), (8, 1)] if d != 'r' else [(5, 0), (6, 1), (4, 2), (3, 3), (7, 0)], fr[2])
        if hat: _hat(sub, d, hat, k, hr)
        # global 1px shift down: feet touch the bottom, row 0 is free for hat tips / outline
        c.a = np.roll(c.a, 1, axis=0); c.a[0] = 0
        c.blit(sub, 0, hy)
        if item:
            it = _item(item, itemc)
            it.a = np.roll(it.a, 1, axis=0)
            if d == 'd': c.blit(it)
            if d == 'u': c.blit(it.flip())
            if d == 'r': c.blit(it, -3 if item in ('shield', 'book', 'basket', 'bag') else -2, 0)
        c.outline()
        out[d] = c
    return [out['d'], out['r'].flip(), out['r'], out['u']]

def H(skin, hair, hs, outfit, hat=None, item=None, **kw):
    return dict(skin=skin, hair=hair, hs=hs, outfit=outfit, hat=hat, item=item, **kw)

PEOPLE = {
    # ---- 5a: jobs / classes (human)
    'villager_m': H('fair', 'brown', 'short', '5a8a4a', pants='6b5a3a'),
    'villager_f': H('fair', 'red', 'long', 'c8648a', legs='robe', apron='ece8d8'),
    'child_m': H('tan', 'black', 'spiky', '4a8ac8', short=True),
    'child_f': H('fair', 'blond', 'pony', 'e8b040', short=True, legs='robe'),
    'elder_m': H('fair', 'white', 'bald', '8a7a6a', item='staff', itemc='8a5a2a', beard='white', legs='robe'),
    'elder_f': H('tan', 'grey', 'bun', '7a6a9a', legs='robe', item='basket', itemc='c84a3a'),
    'farmer': H('tan', 'brown', 'short', 'b08a4a', 'straw', 'hoe', hatc='e0c060', pants='4a6a9a'),
    'merchant': H('fair', 'black', 'short', '9a4ab0', 'turban', 'bag', hatc='e8e0d0', itemc='c8a050', belt='e0b040', legs='robe'),
    'innkeeper': H('fair', 'brown', 'bald', 'c8b090', None, 'mug', beard='brown', apron='f0ece0', pants='5a4630'),
    'cook': H('fair', 'black', 'short', 'f0f0ec', 'chef', apron='e0e0dc', pants='4a4a5a'),
    'blacksmith': H('brown', 'black', 'bald', '6a5a50', None, 'hammer', beard='black', apron='4a3a30', pants='3a3a44'),
    'miner': H('tan', None, 'short', '8a6a4a', 'helmet', 'pick', hatc='d0b040', pants='4a4a5a'),
    'fisher': H('tan', 'grey', 'short', '4a8a9a', 'straw', 'rod', hatc='d8c070', itemc='c84a3a', pants='5a5040'),
    'hunter': H('tan', 'brown', 'short', '5a7a3a', 'feather', 'bow', hatc='4a6a2a', pants='6a5030'),
    'sailor': H('tan', 'blond', 'short', 'f0f0f0', 'bandana', hatc='c83a3a', pants='3a5a9a', belt='3a5a9a'),
    'pirate': H('tan', 'black', 'long', 'a03030', 'tricorn', 'sword', hatc='2e2a34', pants='3a3a44', belt='e0b040', beard='black'),
    'guard': H('fair', 'brown', 'short', '5a7ab0', 'helmet', 'spear', hatc='b0bac8', pants='4a4a5a', belt='6a4a2a'),
    'soldier': H('tan', 'black', 'short', 'a04040', 'helmet', 'sword', hatc='9aa4b4', pants='4a4a5a'),
    'knight': H('fair', 'blond', 'short', 'b8c2d0', 'greathelm', 'sword', hatc='b8c2d0', pants='8a94a4', cape='b03030', shoes='6a7484'),
    'paladin': H('fair', 'blond', 'short', 'e8e4d0', 'helmet', 'shield', hatc='e8d070', itemc='e8e4d0', pants='c8c0a0', cape='3a6ac8', shoes='b0a060'),
    'archer': H('fair', 'red', 'pony', '4a8a4a', 'hood', 'bow', pants='6a5030'),
    'ranger': H('tan', 'brown', 'short', '4a6a3a', 'hood', 'dagger', hatc='3a5a2a', pants='5a4a30', cape='3a5a2a'),
    'rogue': H('fair', 'black', 'short', '3a3a4a', 'hood', 'dagger', hatc='2e2e3a', pants='2e2e3a', belt='8a3a3a'),
    'ninja': H('tan', 'black', 'short', '2e3040', 'mask', 'dagger', hatc='2e3040', pants='26283a', belt='b03a3a'),
    'mage': H('fair', 'blue', 'long', '4a5ac8', 'wizard', 'staff', itemc='60d0f0', legs='robe', belt='e0b040'),
    'wizard_elder': H('fair', 'white', 'long', '6a4ab0', 'wizard', 'staff', itemc='f0d060', legs='robe', beard='white'),
    'witch': H('pale', 'purple', 'long', '3a2e4a', 'witch', 'broom', hatc='3a2e4a', legs='robe'),
    'priest': H('fair', 'brown', 'short', 'f0ece0', None, 'book', itemc='b03a3a', legs='robe', belt='e0b040'),
    'cleric': H('tan', 'blond', 'long', 'f0ece0', 'circlet', 'staff', hatc='60a0f0', itemc='f0d060', legs='robe', belt='4a8ac8'),
    'monk': H('tan', 'black', 'bald', 'd08030', legs='robe', belt='6a3a1a'),
    'druid': H('tan', 'green', 'long', '6a8a4a', 'antlers', 'staff', itemc='60c060', legs='robe', beard='brown'),
    'necromancer': H('pale', 'black', 'long', '2e2a3a', 'hood', 'staff', hatc='2e2a3a', itemc='70e080', legs='robe', glow=(110, 230, 130)),
    'alchemist': H('fair', 'orange', 'spiky', '7a9a6a', None, 'flask', itemc='60d080', apron='8a6a4a', pants='4a4a5a'),
    'scholar': H('fair', 'grey', 'short', '8a5a3a', None, 'book', itemc='3a6ab0', legs='robe'),
    'bard': H('fair', 'blond', 'long', 'c84a6a', 'feather', 'lute', hatc='4a8a5a', pants='e0c050'),
    'barbarian': H('tan', 'red', 'long', '8a6a4a', 'horned', 'axe', hatc='8a8a94', pants='6a4a30', beard='red', belt='4a3020'),
    'king': H('fair', 'grey', 'short', 'b03030', 'crown', 'orb', hatc='e04040', itemc='e0b040', legs='robe', cape='7a2a8a', beard='grey', belt='e0b040'),
    'queen': H('fair', 'blond', 'long', '8a3ab0', 'crown', hatc='60c0f0', legs='robe', cape='b03030', belt='e0b040'),
    'prince': H('fair', 'blond', 'short', '3a6ac8', 'tiara', 'sword', hatc='e04040', pants='f0ece0', cape='b03030'),
    'princess': H('fair', 'pink', 'long', 'f0a0c0', 'tiara', hatc='60c0f0', legs='robe', belt='f0f0f0'),
    'noble_m': H('fair', 'black', 'short', '6a2a4a', 'feather', hatc='6a2a4a', pants='2e2a34', belt='e0b040'),
    'noble_f': H('fair', 'brown', 'bun', '3a8a8a', legs='robe', belt='e0b040', item='bag', itemc='e0d0f0'),
    'torchbearer': H('brown', 'black', 'short', '8a7050', None, 'torch', pants='4a4030'),
    'herald': H('fair', 'brown', 'short', 'e0c040', 'cap', 'banner', hatc='b03030', itemc='b03030', pants='b03030'),
    # ---- 5b: races
    'elf_m': H('fair', 'blond', 'long', '4a9a6a', None, 'bow', ears='elf', pants='6a5a3a', belt='e0c050'),
    'elf_f': H('fair', 'silver', 'long', '60b0a0', 'circlet', 'staff', hatc='60f0c0', itemc='80f0c0', ears='elf', legs='robe'),
    'elf_dark': H('violet', 'white', 'long', '3a2e5a', None, 'dagger', ears='elf', pants='2e2a3a', eye=(200, 60, 80)),
    'elf_wood': H('tan', 'green', 'pony', '6a7a3a', 'leaf', 'spear', hatc='4a9a4a', ears='elf', pants='5a4a30'),
    'dwarf_m': H('tan', 'red', 'short', '8a4a2a', 'helmet', 'axe', hatc='a0a0a8', beard='red', short=True, belt='e0b040'),
    'dwarf_f': H('tan', 'orange', 'pony', '6a6a9a', None, 'hammer', short=True, belt='e0b040'),
    'dwarf_miner': H('brown', 'black', 'short', '6a5a4a', 'helmet', 'pick', hatc='d0b040', beard='black', short=True),
    'dwarf_king': H('tan', 'white', 'short', 'a03a3a', 'crown', 'hammer', hatc='4ac0e0', beard='white', short=True, cape='5a3a8a'),
    'gnome': H('fair', 'white', 'short', '4a8ac8', 'wizard', 'wand', hatc='d04040', itemc='f0e060', beard='white', short=True),
    'gnome_tinker': H('fair', 'orange', 'spiky', '8a8a5a', 'cap', 'hammer', hatc='6a6a74', short=True, apron='6a4a30'),
    'halfling': H('fair', 'brown', 'fringe', '6a9a4a', None, 'basket', itemc='e06040', short=True, pants='8a6a3a'),
    'orc_m': H('orc', 'black', 'spiky', '6a4a30', None, 'club', tusks=True, pants='4a3a2a', belt='2e2a24'),
    'orc_f': H('orc', 'black', 'pony', '8a5a3a', None, 'spear', tusks=True, pants='4a3a2a'),
    'orc_warrior': H('orc', None, 'bald', '5a5a64', 'horned', 'axe', hatc='7a7a84', tusks=True, pants='3a3a44'),
    'orc_shaman': H('orc', 'white', 'long', '7a4a6a', 'antlers', 'staff', itemc='e06040', tusks=True, legs='robe'),
    'goblin': H('goblin', None, 'bald', '7a5a3a', None, 'dagger', ears='elf', short=True, eye=(200, 40, 40)),
    'goblin_archer': H('goblin', None, 'bald', '5a6a3a', 'hood', 'bow', hatc='6a4a30', ears='elf', short=True),
    'catfolk': H('fur', 'orange', 'short', '4a6ab0', None, None, ears='cat', tail='d08a40', snout='f0d8b0', pants='5a4a3a'),
    'catfolk_rogue': H('furgrey', 'grey', 'short', '3a3a4a', None, 'dagger', ears='cat', tail='8a8a94', snout='e0e0e4', pants='2e2e3a', eye=(80, 180, 80)),
    'wolffolk': H('furgrey', 'grey', 'short', '6a4a3a', None, 'spear', ears='wolf', tail='7a7a84', snout='d0d0d8', pants='4a3a2a'),
    'foxfolk': H('fur', 'orange', 'short', 'b03a3a', None, 'wand', itemc='60c0f0', ears='fox', tail='e08030', snout='f4ece0', legs='robe'),
    'rabbitfolk': H('furwhite', 'white', 'short', 'e090b0', None, 'basket', itemc='e08030', ears='rabbit', snout='f8f4f0', legs='robe'),
    'bearfolk': H('furdark', 'black', 'short', '6a8a5a', None, 'club', ears='cat', snout='a08060', pants='4a3a2a'),
    'lizardfolk': H('lizard', None, 'bald', '8a6a3a', None, 'spear', ears='crest', tail='4a8a5a', snout='7ac088', pants='5a4a2a', eye=(230, 200, 60)),
    'dragonborn': H('red', None, 'bald', 'b8a040', None, 'sword', ears='crest', tail='a04a3a', snout='e09070', pants='5a3a2a', cape='3a3a6a', eye=(240, 220, 80)),
    'birdfolk': H('furwhite', 'blue', 'spiky', '4a8ac8', None, 'bow', snout='e8b040', wings='6aa0e0', pants='e8b040', shoes='e8a030'),
    'merfolk_m': H('blue', 'teal', 'short', '78b4d8', None, 'trident', itemc='e0c050', ears='fin', legs='fish', tail='3a9a8a'),
    'merfolk_f': H('teal', 'pink', 'long', 'e080a0', 'tiara', hatc='f0f0f0', ears='fin', legs='fish', tail='4a7ac8'),
    'fairy': H('fair', 'pink', 'bun', '90d870', None, 'wand', itemc='f0e070', wings='c0f0f0', short=True, ears='elf', legs='robe'),
    'pixie': H('violet', 'blue', 'spiky', 'f0c0e0', None, None, wings='f0d0f0', short=True, ears='elf', legs='robe'),
    'dryad': H('bark', 'green', 'long', '5a9a4a', 'leaf', hatc='70b850', legs='robe', eye=(60, 120, 50)),
    'angel': H('fair', 'blond', 'long', 'f4f0e0', 'circlet', 'sword', wings='f8f8f0', legs='robe', belt='e0c050'),
    'skeleton_folk': H('bone', None, 'bald', '6a6a70', None, 'sword', skull=True, pants='4a4a50', shoes='d0c8b0'),
    'skeleton_mage': H('bone', None, 'bald', '4a3a6a', 'hood', 'staff', hatc='4a3a6a', itemc='70e080', skull=True, legs='robe'),
    'zombie_folk': H('rot', 'black', 'short', '6a6a50', pants='4a4a40', eye=(240, 240, 200)),
    'vampire': H('pale', 'black', 'short', '2e2a34', None, None, cape='a02a3a', pants='2e2a34', eye=(200, 40, 50), belt='a02a3a'),
    'ghost_folk': H('pale', 'white', 'long', 'c8d8e8', legs='ghost', eye=(60, 90, 140)),
    'corrupted_knight': H('grey', None, 'bald', '3a3044', 'greathelm', 'sword', hatc='3a3044', pants='2a2434', cape='5a2a6a', shoes='2a2434', glow=(200, 80, 230)),
    'dark_mage': H('grey', 'white', 'long', '3a2a4a', 'hood', 'orb', hatc='3a2a4a', itemc='a050e0', legs='robe', glow=(200, 80, 230)),
    'golemfolk': H('grey', None, 'bald', '8a8a90', None, None, pants='6a6a70', shoes='5a5a60', glow=(90, 220, 240)),
    'nomad': H('brown', 'black', 'short', 'd0b080', 'turban', 'spear', hatc='c84a3a', legs='robe', belt='8a4a2a'),
    'desert_dancer': H('brown', 'black', 'pony', 'e0a030', 'circlet', hatc='e04060', legs='robe', belt='c03a5a'),
    'frost_hunter': H('fair', 'white', 'short', 'd8e4f0', 'hood', 'spear', hatc='f0f4f8', pants='8aa0b8', shoes='6a5a4a'),
    'samurai': H('tan', 'black', 'bun', 'a03030', 'helmet', 'sword', hatc='3a3a44', pants='2e2e3a', belt='e0b040'),
    'shrine_maiden': H('fair', 'black', 'long', 'f4f0ec', None, 'wand', itemc='f0f0f0', legs='robe', belt='c83030'),
    'sky_knight': H('fair', 'silver', 'short', 'e0e8f4', 'helmet', 'spear', hatc='e8d070', wings='e8f0f8', pants='a0b0c8', shoes='e8d070'),
}

def build(A):
    A.section('sprites')
    for name, kw in PEOPLE.items():
        A.sprite(name, humanoid(**kw), cat='people')
