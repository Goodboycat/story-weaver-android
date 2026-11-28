"""Kingdom buildings. One row in KINGDOMS = one whole set (15 objects). building() is the parametric generator."""
from core import *
from props import speck, cap, fin, glow, icon, SNOW

def wall_tex(c, x0, y0, x1, y1, col, style, seed=0):
    rp = ramp(col); c.box(x0, y0, x1, y1, rp)
    for y in range(y0 + 1, y1):
        for x in range(x0 + 1, x1):
            d = y - y0; v = None
            if style == 'brick':
                if d % 3 == 2 or (x + (d // 3 % 2) * 3) % 6 == 0: v = 1
            elif style == 'stone':
                if d % 4 == 3 or (x + (d // 4 % 2) * 4) % 8 == 0: v = 1
                elif rnd(x, y, seed) < .08: v = 3
            elif style == 'plank':
                if x % 4 == 0: v = 1
                elif rnd(x, y, seed) < .06: v = 3
            elif style == 'log':
                v = 1 if d % 4 == 3 else 3 if d % 4 == 0 else None
            elif rnd(x, y, seed) < .07: v = 1 if rnd(y, x, seed) < .5 else 3
            if v: c.p(x, y, rp[v])

def roof(c, x0, y0, x1, y1, P, style, seed=0):
    """styles: gable thatch pagoda flat dome ribdome cone tent leaf"""
    rp = ramp(P['roof']); tr = ramp(P['trim']); n = max(1, y1 - y0); cx = (x0 + x1) / 2; half = (x1 - x0) / 2
    if style in ('gable', 'thatch', 'pagoda'):
        for y in range(y0, y1 + 1):
            t = (y - y0) / n
            ins = round(3 * (1 - t)) if style == 'gable' else round((5 if style == 'thatch' else 6) * (1 - t) ** 2)
            for x in range(x0 + ins, x1 - ins + 1):
                k = (y - y0) % 4; col = rp[2]
                if style == 'thatch':
                    r = rnd(x, y // 3, seed); col = rp[1] if r < .25 else rp[3] if r > .8 else rp[2]
                elif k == 0: col = rp[3]
                elif k == 3 or (x + ((y - y0) // 4) * 3) % 6 == 0: col = rp[1]
                if x > x1 - ins - 2 and col != rp[1]: col = rp[1]
                if y == y1: col = tr[1] if style == 'pagoda' else rp[0]
                if y == y0: col = tr[3] if style == 'pagoda' else rp[4]
                c.p(x, y, col)
        if style == 'pagoda':
            c.clear(x0, y1); c.clear(x1, y1); c.p(x0, y1 - 1, tr[3]); c.p(x1, y1 - 1, tr[1])
            c.p(x0 + 5, y0 - 1, tr[4]); c.p(x1 - 5, y0 - 1, tr[4])
    elif style == 'flat':
        wall_tex(c, x0, y0 + 3, x1, y1, P['roof'], 'stone', seed)
        if n >= 10: c.rect(x0 + 2, y0 + 5, x1 - 2, y1 - 3, rp[1])
        if n >= 10 and x1 - x0 >= 20:   # skylight hatch + vent so the slab is not empty
            g = ramp(P['glass']); hx = x0 + 5 + seed % 3; hy = y0 + 6
            c.rect(hx, hy, hx + 6, hy + 4, tr[1]); c.rect(hx + 1, hy + 1, hx + 5, hy + 3, g[2]); c.line(hx + 3, hy + 1, hx + 3, hy + 3, tr[1]); c.p(hx + 1, hy + 1, g[4])
            vx = x1 - 8 - seed % 2; c.rect(vx, hy + 1, vx + 3, hy + 4, tr[2]); c.rect(vx, hy + 1, vx + 3, hy + 1, tr[4]); c.p(vx + 1, hy + 3, tr[0]); c.p(vx + 2, hy + 3, tr[0])
        for x in range(x0, x1 + 1):
            if ((x - x0) // 3) % 2 == 0:
                for y in range(y0, y0 + 3): c.p(x, y, rp[4] if y == y0 else rp[1] if (x - x0) % 3 == 2 else rp[2])
    elif style in ('dome', 'ribdome', 'leaf'):
        t = C(c.w, c.h)
        if style == 'leaf': t.ell(cx, (y0 + y1) / 2, half, n / 2, rp)
        else: t.ell(cx, y1, half, n, rp)
        for y in range(max(0, y0 - 1), y1 + 1):
            for x in range(c.w):
                if t.a[y, x, 3]:
                    v = t.a[y, x]
                    if style == 'ribdome' and abs(x - cx) % 4 < 1: v = (*rp[1], 255)
                    c.a[y, x] = v
        if style == 'leaf':
            speck(c, rp, seed, .16)
            for i in range(int(half)): c.p(x0 + 3 + ri(i, 1, seed, int(half * 2 - 6)), y0 + 2 + ri(i, 2, seed, max(1, n - 5)), tr[3])
        else:
            for x in range(x0, x1 + 1): c.p(x, y1, tr[1]); c.p(x, y1 - 1, tr[3])
            c.p(cx, y0 - 1, tr[3]); c.p(cx, y0 - 2, tr[4]); c.p(cx + .5, y0 - 1, tr[2])
    else:  # cone / tent
        fl = ramp(P['flag'])
        for y in range(y0, y1 + 1):
            w = max(.5, half * (y - y0) / n)
            for x in range(int(math.floor(cx - w)), int(math.ceil(cx + w)) + 1):
                col = rp[3] if x < cx - w * .3 else rp[2] if x <= cx + w * .3 else rp[1]
                if style == 'tent':
                    if int(abs(x - cx) // 3) % 2: col = fl[3] if x < cx else fl[1]
                    if y == y1 and (x - x0) % 4 == 3: continue
                elif (y - y0) % 4 == 3: col = rp[1] if col != rp[1] else rp[0]
                if y == y1: col = rp[0]
                c.p(x, y, col)
        c.p(cx, y0 - 1, tr[3]); c.p(cx, y0 - 2, tr[4])
    if P.get('spots'):
        for i in range(int(half * n / 32)):
            x = x0 + 2 + ri(i, 5, seed, int(half * 2 - 3)); y = y0 + 2 + ri(i, 6, seed, max(1, n - 5))
            if c.get(x, y) and c.get(x + 1, y + 1): c.rect(x, y, x + 1, y + 1, (250, 246, 236))
    if P.get('ridge') == 'spikes' and style in ('gable', 'thatch', 'flat'):
        for x in range(x0 + 5, x1 - 3, 5): c.p(x, y0 - 1, tr[3]); c.p(x, y0 - 2, tr[4]); c.p(x + 1, y0 - 1, tr[1])

def window(c, cx, y, P):
    tr = ramp(P['trim']); g = ramp(P['glass'])
    c.rect(cx - 3, y, cx + 2, y + 5, tr[1]); c.rect(cx - 2, y + 1, cx + 1, y + 4, g[2])
    c.p(cx - 2, y + 1, g[4]); c.p(cx - 1, y + 1, g[3]); c.p(cx - 2, y + 2, g[3]); c.p(cx + 1, y + 4, g[1]); c.rect(cx - 3, y + 5, cx + 2, y + 5, tr[3])

def door(c, cx, y1, P, w=6, h=9):
    tr = ramp(P['trim']); d = ramp(P['door']); x0 = cx - w // 2; x1 = x0 + w - 1; y0 = y1 - h + 1
    c.rect(x0 - 1, y0 - 1, x1 + 1, y1, tr[1]); c.box(x0, y0, x1, y1, d)
    for y in range(y0 + 1, y1): c.p(cx, y, d[1])
    c.p(x1 - 1, y0 + h // 2, tr[4])
    if P.get('arch'):
        for x in (x0, x1): c.p(x, y0, tr[1])
    c.rect(x0 - 1, y0 - 1, x1 + 1, y0 - 1, tr[3])

def building(w, h, P, rs=None, seed=0, wall_h=None, m=0, sign=None, chimney=False, cols=False, emblem=False, raw=False, dw=6, dh=9):
    """w,h in tiles. P = palette dict (wall ws roof rs trim door glass flag + flags). m = side margin px (towers)."""
    W, H = w * 16, h * 16; c = C(W, H); rs = rs or P['rs']; tr = ramp(P['trim'])
    wh = wall_h or {2: 13, 3: 20, 4: 26}[h]; wy1 = H - 2; wy0 = wy1 - wh + 1; bx0 = 2 + m; bx1 = W - 3 - m
    wall_tex(c, bx0, wy0, bx1, wy1, P['wall'], P['ws'], seed)
    bs = ramp(P.get('base', P['trim'])); c.rect(bx0, wy1 - 1, bx1, wy1, bs[1]); c.rect(bx0, wy1 - 1, bx1, wy1 - 1, bs[2])
    if P.get('timber'):
        for x in [bx0, bx1] + list(range(bx0 + 15, bx1 - 8, 16)): c.rect(x, wy0, x, wy1, tr[2] if x != bx1 else tr[1])
        c.rect(bx0, wy0 + wh // 2 + 1, bx1, wy0 + wh // 2 + 1, tr[2])
    if cols:
        mb = ramp('#e4e4ec')
        for x in range(bx0 + 2, bx1 - 1, 7):
            if abs(x + 1 - W // 2) > dw // 2 + 2: c.rect(x, wy0 + 3, x, wy1, mb[3]); c.rect(x + 1, wy0 + 3, x + 1, wy1, mb[1]); c.rect(x - 1, wy1 - 1, x + 2, wy1, mb[2])
    rows = [wy0 + 5] + ([wy0 + 14] if wh >= 24 else []) + ([wy0 + 23] if wh >= 38 else [])
    for b in ([W // 2] if m else [i * 16 + 8 for i in range(w)]):
        for y in rows:
            if (abs(b - W / 2) < 7 and y + 6 > wy1 - dh - 1) or cols: continue
            window(c, b, y, P)
    door(c, W // 2, wy1, P, dw, dh)
    ry0 = 3 if (rs in ('dome', 'ribdome', 'cone', 'tent', 'pagoda') or P.get('ridge')) else 1
    ov = 1 if m == 0 else min(m - 1, 3); ry1 = wy0 + (2 if rs != 'flat' else 0)
    for x in range(bx0 + 1, bx1): c.p(x, ry1 + 1, ramp(P['wall'])[1])
    roof(c, bx0 - ov, ry0, bx1 + ov, ry1, P, rs, seed)
    if chimney or P.get('chimney'):
        x = bx1 - 7; y = ry0 + 3; wall_tex(c, x, y - 4 + (0 if ry0 > 1 else 2), x + 4, y + 4, P.get('chim', '#8a8d96'), 'brick', seed); c.rect(x, y - 4 + (0 if ry0 > 1 else 2), x + 4, y - 4 + (0 if ry0 > 1 else 2), (60, 56, 70))
    if sign:
        y = ry1 - 9; c.box(W // 2 - 5, y, W // 2 + 4, y + 7, '#c89a60'); icon(c, sign, W // 2 - 3, y + 1)
    if emblem:
        e = ramp(P.get('emblem', P['flag'])); x = W // 2; y = max(ry0 + 4, ry1 - 9)
        for d in range(-2, 3):
            for k in range(-(2 - abs(d)), 3 - abs(d)): c.p(x + k, y + d, e[4] if d < 0 else e[2])
        c.p(x, y, (255, 255, 255))
    if P.get('snow'): cap(c, SNOW, 2, seed, y_max=wy0)
    return c if raw else fin(c)

def banner(P, tall=True):
    c = C(16, 32); fl = ramp(P['flag']); tr = ramp(P['trim'])
    c.rect(4, 3, 4, 29, (90, 70, 50)); c.rect(5, 3, 5, 29, (60, 46, 36)); c.p(4, 2, tr[4]); c.p(5, 2, tr[3]); c.rect(3, 4, 12, 4, tr[2])
    for y in range(5, 19):
        for x in range(6, 13):
            if y > 15 and abs(x - 9) < y - 15: continue
            c.p(x, y, fl[3] if x < 8 else fl[1] if x > 10 else fl[2])
    c.rect(8, 8, 10, 10, tr[3]); c.p(9, 9, tr[4]); c.rect(6, 5, 12, 5, fl[4])
    return fin(c, (30, 2, 8))

def stall(P, seed=0):
    c = C(32, 32); fl = ramp(P['flag']); wd = ramp('#a8703a')
    for x in (3, 28): c.rect(x, 12, x, 29, wd[1]); c.rect(x - 1 if x > 9 else x + 1, 12, x - 1 if x > 9 else x + 1, 29, wd[3])
    c.box(4, 20, 27, 29, wd); c.rect(4, 20, 27, 21, wd[4])
    for i, x in enumerate(range(6, 26, 4)):
        g = ramp(['#d8443c', '#e8b830', '#58a848', '#b05ad0', '#e88030'][(i + seed) % 5]); c.rect(x, 17, x + 2, 19, g[2]); c.p(x, 17, g[4]); c.p(x + 2, 19, g[1])
    for y in range(3, 14):
        for x in range(1, 31):
            if y == 13 and (x % 6) in (0, 5): continue
            if y < 5 and (x < 5 - y + 2 or x > 26 + y - 2): continue
            col = fl if (x // 3) % 2 else ramp('#f0ece0'); c.p(x, y, col[3] if y < 6 else col[1] if y > 11 else col[2])
    return fin(c, (30, 3, 28))

def lamp(P):
    c = C(16, 32); tr = ramp(P['trim']); li = P.get('light', (255, 220, 120)); g = ramp(P['glass'])
    c.rect(7, 12, 8, 29, tr[1]); c.rect(7, 12, 7, 29, tr[3]); c.rect(5, 29, 10, 30, tr[1]); c.rect(6, 28, 9, 28, tr[2])
    c.rect(5, 5, 10, 11, tr[1]); c.rect(6, 6, 9, 10, li); c.p(7, 7, (255, 255, 255)); c.p(8, 7, (255, 255, 240)); c.rect(4, 4, 11, 4, tr[2]); c.rect(6, 3, 9, 3, tr[3]); c.p(7, 2, tr[4]); c.p(8, 2, tr[3])
    fin(c, (30, 4, 11)); glow(c, 7.5, 8, 6, li, 60); return c

def mini(c, x, y1, w, hh, P, rs, rh=4):
    """tiny house for world icons: wall x..x+w-1, bottom y1"""
    wl = ramp(P['wall']); rp = ramp(P['roof']); y0 = y1 - hh + 1
    c.box(x, y0, x + w - 1, y1, wl); c.p(x + w // 2, y1, ramp(P['door'])[1]); c.p(x + w // 2, y1 - 1, ramp(P['door'])[2])
    if rs == 'flat':
        c.rect(x - 1, y0 - 2, x + w, y0 - 1, rp[2]); c.rect(x - 1, y0 - 2, x + w, y0 - 2, rp[3])
        for i in range(x - 1, x + w + 1, 2): c.p(i, y0 - 3, rp[3])
    elif rs in ('dome', 'ribdome', 'leaf'):
        t = C(16, 16); t.ell(x + (w - 1) / 2, y0 - 1, w / 2 + .5, rh, rp)
        for yy in range(0, y0):
            for xx in range(16):
                if t.a[yy, xx, 3]: c.a[yy, xx] = t.a[yy, xx]
    else:
        for i in range(rh):
            ww = (w / 2 + 1) * (i + 1) / rh; cx = x + (w - 1) / 2
            for xx in range(int(math.ceil(cx - ww)), int(math.floor(cx + ww)) + 1): c.p(xx, y0 - rh + i, rp[3] if xx < cx - .6 else rp[1] if xx > cx + ww * .4 else rp[2])

def icons(P):
    rs = P['rs']; tw = P['tower']; out = {}
    c = C(); mini(c, 4, 13, 8, 5, P, rs, 5); out['village'] = fin(c)
    c = C(); mini(c, 1, 10, 6, 4, P, rs); mini(c, 8, 13, 7, 5, P, rs); out['town'] = fin(c)
    c = C(); mini(c, 6, 9, 4, 6, P, tw, 3); mini(c, 1, 13, 5, 4, P, rs, 3); mini(c, 10, 13, 5, 4, P, rs, 3); mini(c, 5, 14, 6, 4, P, rs, 3); out['city'] = fin(c)
    c = C(); fl = ramp(P['flag']); wl = ramp(P['wall'])
    c.box(3, 8, 12, 14, wl); mini(c, 1, 14, 4, 9, P, tw, 3); mini(c, 11, 14, 4, 9, P, tw, 3)
    for x in range(4, 12, 2): c.p(x, 7, wl[3])
    c.rect(7, 11, 8, 14, ramp(P['door'])[1]); c.rect(7, 2, 7, 7, (70, 56, 46)); c.rect(8, 2, 10, 4, fl[2]); c.p(8, 2, fl[4]); out['castle'] = fin(c)
    c = C(); g = ramp(tuple(int(v * .6 + 140 * .4) for v in wl[2]))
    c.box(2, 9, 6, 14, g); c.box(9, 6, 12, 14, g); c.rect(6, 12, 9, 14, g[1]); c.clear(2, 9); c.clear(3, 9); c.clear(6, 9); c.clear(12, 6); c.clear(12, 7); c.clear(9, 6)
    c.p(13, 14, g[2]); c.p(14, 14, g[1]); c.p(1, 14, g[2]); c.p(7, 11, g[3]); c.p(4, 12, g[1]); c.p(10, 10, g[1]); out['ruin'] = fin(c)
    return out

def gate(P, seed=0):
    c = C(48, 48); Q = dict(P, roof=P['wall'], ridge=None, spots=None); wl = ramp(P['wall'])
    ws = P['ws'] if P['ws'] in ('stone', 'brick', 'log') else 'stone'
    wall_tex(c, 2, 18, 45, 46, P['wall'], ws, seed); roof(c, 2, 13, 45, 19, Q, 'flat', seed)
    for y in range(24, 47):
        for x in range(17, 31):
            if y < 27 and (x < 17 + (27 - y) or x > 30 - (27 - y)): continue
            c.p(x, y, (34, 30, 46) if y < 30 else (52, 46, 62))
    for x in range(17, 31): c.clear(x, 46); c.clear(x, 45) if 18 < x < 29 else None
    tr = ramp(P['trim'])
    for y in range(23, 47): c.p(16, y, tr[2]); c.p(31, y, tr[1])
    c.rect(18, 22, 29, 23, tr[2]); c.rect(20, 21, 27, 21, tr[3])
    for ox in (0, 34):
        t = building(1, 3, P, rs=P['tower'], seed=seed + ox, wall_h=30, m=0, raw=True, dw=0, dh=0)
        for y in range(48):
            for x in range(16):
                if 6 <= x <= 9 and y > 36 and t.a[y, x, 3]: t.a[y, x] = (*wl[2], 255)
        c.blit(fin(t), ox - 1 if ox == 0 else ox - 1, 0)
    fl = ramp(P['flag']); c.rect(23, 3, 23, 13, (80, 64, 50)); c.rect(24, 3, 28, 6, fl[2]); c.rect(24, 3, 28, 3, fl[4]); c.p(28, 6, fl[1])
    return fin(c)

def castle(P, seed=0):
    c = building(4, 4, dict(P, ridge=None, spots=None, roof=P['wall']), rs='flat', seed=seed, wall_h=36, raw=True, dw=10, dh=13)
    k = building(2, 3, P, rs=P['rs'] if P['rs'] not in ('leaf',) else P['tower'], seed=seed + 1, wall_h=18, m=4, raw=True, dw=0, dh=0)
    kk = C(32, 30); kk.a = k.a[:30].copy(); c.blit(fin(kk), 16, 0)
    for ox in (-5, 37):
        t = building(2, 4, P, rs=P['tower'], seed=seed + ox, wall_h=44, m=5, raw=True, dw=0, dh=0); wl = ramp(P['wall'])
        for y in range(52, 62):
            for x in range(12, 20): t.a[y, x] = (*wl[2 if (x + y) % 5 else 1], 255)
        c.blit(fin(t), ox, 0)
    fl = ramp(P['flag']); c.rect(31, 0, 31, 3, (80, 64, 50))
    return fin(c)

def K(wall, ws, roofc, rs, trim, doorc, glass, flag, tower, **kw):
    return dict(wall=wall, ws=ws, roof=roofc, rs=rs, trim=trim, door=doorc, glass=glass, flag=flag, tower=tower, **kw)

KINGDOMS = {
    'human': K('#e8dcc0', 'plaster', '#c8503c', 'gable', '#7a4a2a', '#8a5a30', '#8ad0f0', '#3c6ad8', 'cone', timber=1, chimney=1),
    'elven': K('#e8ecd8', 'plaster', '#4fae58', 'leaf', '#c8b060', '#6a8a4a', '#b8f0d0', '#58c890', 'cone', arch=1, temple='leaf'),
    'dwarven': K('#8a8d96', 'stone', '#5a5f78', 'flat', '#c88030', '#6a4a30', '#f0c060', '#c03c3c', 'flat', chimney=1, chim='#5a5f68'),
    'orc': K('#7a5a40', 'log', '#9a7a48', 'thatch', '#d8d0b8', '#4a3424', '#f0a040', '#a03030', 'flat', ridge='spikes', base='#5a4030'),
    'desert': K('#e0c890', 'plaster', '#d0a860', 'flat', '#a86a3a', '#7a4a2a', '#5a4a40', '#3fa8c8', 'dome', arch=1, temple='dome', big='dome', roof2='#3fa8c8'),
    'frost': K('#b8c8d8', 'stone', '#5a7ab0', 'gable', '#4a5a78', '#6a4a30', '#f0e090', '#e8f0fa', 'cone', snow=1, chimney=1),
    'eastern': K('#f0e8d8', 'plaster', '#2f6a70', 'pagoda', '#c03a30', '#a03028', '#f0e0a0', '#c03a30', 'pagoda', timber=1),
    'merfolk': K('#b8e0d8', 'plaster', '#f08aa0', 'ribdome', '#e8d8a0', '#3f8a9a', '#60d0e0', '#3fa8c8', 'ribdome', arch=1),
    'sky': K('#f4f4fa', 'plaster', '#f0d060', 'dome', '#a8c0f0', '#d8b040', '#a0e0ff', '#f8f8ff', 'cone', arch=1, base='#c8d0e8', light=(200, 240, 255)),
    'undead': K('#6a6878', 'stone', '#3a3448', 'gable', '#b8c8a8', '#2a2430', '#80f0a0', '#5a3a78', 'cone', ridge='spikes', base='#4a4858', light=(140, 250, 170)),
    'beastfolk': K('#b08a58', 'log', '#d8b860', 'thatch', '#7a4a2a', '#5a3a26', '#f0c060', '#e08030', 'cone'),
    'fey': K('#f0d8e8', 'plaster', '#d85aa8', 'dome', '#8a5ac8', '#6a3a9a', '#f8f0a0', '#f8a0d8', 'dome', spots=1, arch=1, light=(250, 200, 255)),
    'gnome': K('#c8a070', 'brick', '#b87333', 'gable', '#d8c060', '#5a4030', '#f0e080', '#3fa890', 'flat', chimney=1, chim='#b87333'),
    'dragon': K('#8a4a40', 'stone', '#c03028', 'pagoda', '#e8b830', '#4a2420', '#f8a040', '#e8b830', 'cone', ridge='spikes', base='#5a3030', light=(255, 170, 80)),
    'nomad': K('#d8c8a8', 'plaster', '#c86a3a', 'tent', '#3c6ad8', '#8a5a30', '#5a4a40', '#e8b830', 'tent', base='#a89070'),
    'swamp': K('#7a8a60', 'log', '#5a7a3a', 'thatch', '#3a4a2a', '#4a3a26', '#c8f080', '#8ac040', 'cone', base='#4a5238', light=(190, 240, 130)),
    'crystal': K('#d8e8f8', 'stone', '#8a6ae0', 'dome', '#58c8f0', '#4a5aa8', '#f0f8ff', '#b890f0', 'cone', arch=1, base='#a8b8e0', light=(190, 220, 255)),
    'pirate': K('#b8905a', 'log', '#3a4a6a', 'gable', '#e8e0d0', '#5a3a26', '#f0c060', '#2a2a34', 'flat', chimney=1, base='#6a5038'),
    'jungle': K('#c8b078', 'brick', '#3a8a48', 'pagoda', '#e8b830', '#6a4a2a', '#f0e090', '#e04a3a', 'flat', base='#8a7a50', temple='flat'),
    'dark': K('#4a4658', 'stone', '#2a2438', 'gable', '#8a3ac8', '#1e1a28', '#c060f0', '#6a2a9a', 'cone', ridge='spikes', base='#2e2a3a', light=(200, 110, 250)),
}

def kingdom(A, name, P, seed=0):
    cat = 'kingdom_' + name; o = lambda n, c, solid=1: A.obj(name + '_' + n, c, cat=cat, solid=solid)
    big = P.get('big', P['rs']); B = dict(P, roof=P.get('roof2', P['roof'])) if P.get('big') else P
    o('house_s', building(2, 2, P, seed=seed)); o('house_m', building(3, 3, P, seed=seed + 1)); o('house_l', building(4, 3, P, seed=seed + 2))
    o('shop', building(3, 3, P, seed=seed + 3, sign='potion')); o('inn', building(4, 3, P, seed=seed + 4, sign='inn'))
    o('temple', building(3, 3, B, rs=P.get('temple', big), seed=seed + 5, cols=True, emblem=True, wall_h=18))
    o('tower', building(2, 4, B, rs=P['tower'], seed=seed + 6, wall_h=42, m=5)); o('gate', gate(B, seed + 7), 0); o('castle', castle(B, seed + 8))
    o('banner', banner(P)); o('stall', stall(P, seed)); o('lamp', lamp(P))
    for n, c in icons(P).items(): o('icon_' + n, c, 0)

def build(A):
    A.section('kingdoms')
    for i, (name, P) in enumerate(KINGDOMS.items()): kingdom(A, name, P, seed=i * 11)
