"""Zoomed review of terrains: python3 tools/tileset/preview_terrain.py name1 name2 ... -> /tmp/preview_terrain.png (5x4 blob of each on grass)"""
import sys, os, json
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
import render
from core import T
ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'app', 'src', 'main', 'assets', 'tileset')
at = json.load(open(os.path.join(ROOT, 'atlas.json'))); img = np.array(Image.open(os.path.join(ROOT, 'master.png')).convert('RGBA'))
names = sys.argv[1:]; per = 7; W = per * 6; H = 6 * ((len(names) + per - 1) // per)
ter = {(i, j): 'grass' for i in range(W) for j in range(H)}
for k, n in enumerate(names):
    ox = (k % per) * 6; oy = (k // per) * 6
    for i in range(1, 6):
        for j in range(1, 5):
            if (i, j) not in ((1, 1), (5, 4)): ter[(ox + i, oy + j)] = n
m = render.render(img, at, W, H, ter, {}, [], [])
Image.fromarray(m).resize((W * T * 2, H * T * 2), Image.NEAREST).convert('RGB').save('/tmp/preview_terrain.png'); print('/tmp/preview_terrain.png')
