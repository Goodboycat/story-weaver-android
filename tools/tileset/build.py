"""python3 tools/tileset/build.py -> master.png + atlas.json + demo.png"""
import json, os, sys
import numpy as np
from PIL import Image
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
from core import *
import terrains, render
ROOT = os.path.join(HERE, '..', '..')
OUTD = os.path.join(ROOT, 'app/src/main/assets/tileset'); DOC = os.path.join(ROOT, 'docs/tileset')
os.makedirs(OUTD, exist_ok=True); os.makedirs(DOC, exist_ok=True)
A = Atlas(); terrains.build_terrains(A); terrains.build_lines(A)
for mod in ('props', 'kingdoms', 'dims', 'sprites'):
    try: __import__(mod).build(A)
    except ImportError: pass
img = A.image()
at = dict(tile=T, cols=A.COLS, rows=img.shape[0] // T, dirs=['down', 'left', 'right', 'up'], sections=A.sections,
          terrains=A.terrains, lines=A.lines, objects=A.objects, sprites=A.sprites)
Image.fromarray(img).save(os.path.join(OUTD, 'master.png'))
json.dump(at, open(os.path.join(OUTD, 'atlas.json'), 'w'), separators=(',', ':'))
bg = np.zeros_like(img); bg[:] = (255, 0, 255, 255); render.over(bg, img, 0, 0)   # magenta-key copy for tools without alpha
Image.fromarray(bg).convert('RGB').save(os.path.join(DOC, 'master_magenta.png'))
# ---- demo map
W, H = 40, 26; ter = {}; ln = {}
def R(x0, y0, x1, y1, n):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1): ter[(x, y)] = n
def Ln(x0, y0, x1, y1, n):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1): ln[(x, y)] = n
R(0, 0, 39, 25, 'grass'); R(9, 0, 13, 9, 'sand'); R(0, 6, 11, 9, 'sand'); R(0, 0, 10, 6, 'water'); R(0, 0, 6, 3, 'deep_water')
R(15, 1, 22, 5, 'forest'); R(18, 4, 20, 7, 'forest'); R(24, 1, 30, 4, 'grass_plateau'); R(26, 3, 28, 6, 'grass_plateau')
R(32, 1, 38, 5, 'cliff'); R(34, 2, 36, 3, 'snow_plateau'); R(3, 12, 8, 16, 'water'); R(5, 14, 6, 14, 'grass')
R(11, 11, 12, 20, 'path'); R(11, 15, 22, 16, 'path'); R(15, 11, 19, 13, 'flower_meadow'); R(14, 19, 19, 22, 'tall_grass')
R(24, 9, 32, 16, 'stone_wall'); R(25, 10, 31, 15, 'wood_floor'); R(28, 12, 31, 15, 'red_carpet'); R(28, 16, 28, 16, 'wood_floor'); R(27, 10, 27, 13, 'stone_wall')
R(33, 7, 39, 14, 'ash'); R(34, 8, 38, 12, 'lava'); R(34, 17, 39, 21, 'snow'); R(36, 18, 38, 20, 'ice'); R(24, 19, 31, 24, 'blight'); R(26, 20, 29, 22, 'poison')
R(1, 23, 9, 25, 'cobble_road'); R(1, 19, 4, 22, 'roof_red'); R(6, 19, 9, 22, 'roof_blue'); R(33, 23, 39, 25, 'starfield'); R(35, 23, 37, 24, 'cloud')
R(13, 24, 22, 25, 'dungeon_floor'); R(16, 24, 17, 25, 'dungeon_wall')
Ln(14, 9, 22, 9, 'wood_fence'); Ln(22, 9, 22, 11, 'wood_fence'); Ln(13, 17, 23, 17, 'river'); Ln(21, 17, 21, 23, 'river'); Ln(14, 6, 14, 8, 'castle_rampart'); Ln(14, 6, 17, 6, 'castle_rampart')
Ln(33, 15, 39, 15, 'iron_fence'); Ln(24, 18, 31, 18, 'hedge_row'); Ln(0, 11, 9, 11, 'trail'); Ln(26, 23, 30, 23, 'corruption_vein')
objs = []; sps = []
try: import demo_extra; objs, sps = demo_extra.get(A)
except ImportError: pass
m = render.render(img, at, W, H, ter, ln, objs, sps)
Image.fromarray(m).resize((W * T * 2, H * T * 2), Image.NEAREST).convert('RGB').save(os.path.join(DOC, 'demo.png'))
if objs: demo_extra.sheet(img, at, os.path.join(DOC, 'props.png'))
print('tiles', A.count, 'sheet', img.shape[1], 'x', img.shape[0], 'terrains', len(A.terrains), 'lines', len(A.lines), 'objects', len(A.objects), 'sprites', len(A.sprites))
