"""Zoomed review image of one object category: python3 tools/tileset/preview.py village [scale] [cols] -> /tmp/preview_<cat>.png"""
import sys, os, json
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
import render
from core import T
ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'app', 'src', 'main', 'assets', 'tileset')
cat = sys.argv[1]; sc = int(sys.argv[2]) if len(sys.argv) > 2 else 6; cols = int(sys.argv[3]) if len(sys.argv) > 3 else 16
at = json.load(open(os.path.join(ROOT, 'atlas.json'))); img = np.array(Image.open(os.path.join(ROOT, 'master.png')).convert('RGBA'))
if cat.startswith('sprites'):   # preview.py sprites[:<cat>] -> every sprite, 4 directions each (down, left, right, up)
    import demo_extra; out = '/tmp/preview_sprites.png'
    demo_extra.sprite_sheet(img, at, out, max(1, cols // 4), sc, cat.split(':')[1] if ':' in cat else None); print(out); sys.exit()
objs = []; x = 0; y = 0; band = 1
for n, o in at['objects'].items():
    if o.get('cat') != cat: continue
    if x + o['w'] > cols: x = 0; y += band; band = 1
    objs.append((n, x, y)); x += o['w']; band = max(band, o['h'])
H = y + band; ter = {(i, j): 'grass' for i in range(cols) for j in range(H)}
m = render.render(img, at, cols, H, ter, {}, objs, [])
out = '/tmp/preview_%s.png' % cat
Image.fromarray(m).resize((cols * T * sc, H * T * sc), Image.NEAREST).convert('RGB').save(out); print(out, len(objs))
