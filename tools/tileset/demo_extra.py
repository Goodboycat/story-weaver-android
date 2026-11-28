"""Objects / sprites scattered on the demo map + a contact sheet of every object (docs/tileset/props.png)."""
import os
import numpy as np
from PIL import Image
import render
from core import T

def get(A):
    want = [('tree_oak', 15, 1), ('tree_pine', 18, 1), ('tree_oak_dark', 20, 2), ('tree_cherry', 16, 10), ('tree_apple', 13, 12), ('tree_palm', 10, 5), ('tree_palm', 12, 2),
            ('tree_snowy', 34, 17), ('bush_snowy', 39, 21), ('rock_snowy', 35, 21), ('tree_dead_ash', 33, 6), ('tree_blight', 24, 19), ('tree_dead', 30, 21),
            ('bush_blight', 27, 24), ('boulder', 22, 20), ('bush_berry', 14, 14), ('bush', 20, 14), ('rock', 13, 18), ('stump', 17, 18), ('log', 15, 23),
            ('flowers_red', 14, 20), ('flowers_yellow', 16, 21), ('lilypad_flower', 4, 13), ('lilypad', 7, 15), ('reeds', 3, 16), ('cactus', 2, 7), ('rock_sand', 6, 8),
            ('tree_crystal', 37, 22), ('giant_mushroom', 22, 11), ('mushrooms', 21, 13), ('rock_ore_gold', 33, 3)]
    return [w for w in want if w[0] in A.objects], []

def sheet(img, at, path, cols=40):
    """every object laid out on grass with its name order = atlas order; 3x scale"""
    objs = []; x = 0; y = 0; band = 1
    for n, o in at['objects'].items():
        if x + o['w'] > cols: x = 0; y += band; band = 1
        objs.append((n, x, y)); x += o['w']; band = max(band, o['h'])
    H = y + band; ter = {(i, j): 'grass' for i in range(cols) for j in range(H)}
    m = render.render(img, at, cols, H, ter, {}, objs, [])
    Image.fromarray(m).resize((cols * T * 3, H * T * 3), Image.NEAREST).convert('RGB').save(path)
