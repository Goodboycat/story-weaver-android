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
    sp = [('villager_m', 12, 14, 0), ('farmer', 15, 20, 2), ('knight', 28, 17, 0), ('king', 30, 13, 0), ('guard', 27, 16, 2), ('mage', 17, 12, 1),
          ('fisher', 9, 13, 1), ('merfolk_f', 5, 4, 0), ('frost_hunter', 35, 19, 2), ('corrupted_knight', 25, 21, 2), ('skeleton_folk', 15, 24, 2),
          ('elf_m', 19, 6, 0), ('dwarf_miner', 32, 4, 2), ('pirate', 8, 8, 1), ('merchant', 3, 24, 0), ('catfolk', 12, 18, 3),
          ('wolf', 17, 21, 1), ('slime_green', 19, 19, 0), ('bat', 18, 24, 0), ('dragon_red', 36, 13, 0), ('golem_ice', 38, 17, 0), ('crab', 7, 7, 2)]
    return [w for w in want if w[0] in A.objects], [s for s in sp if s[0] in A.sprites]

def sprite_sheet(img, at, path, per=6):
    """every sprite, 4 directions each (down, left, right, up), atlas order; 3x scale -> docs/tileset/sprites.png"""
    sp = []; ter = {}; x = 0; y = 0; band = 1
    for n, o in at['sprites'].items():
        w, h = o.get('w', 1), o.get('h', 1)
        if x + w * 4 > per * 4: x = 0; y += band; band = 1
        sp += [(n, x + d * w, y, d) for d in range(4)]; x += w * 4; band = max(band, h)
    H = y + band; ter = {(i, j): 'grass' if (i // 4 + j) % 2 else 'path' for i in range(per * 4) for j in range(H)}
    m = render.render(img, at, per * 4, H, ter, {}, [], sp)
    Image.fromarray(m).resize((per * 4 * T * 3, H * T * 3), Image.NEAREST).convert('RGB').save(path)

def sheet(img, at, path, cols=40):
    """every object laid out on grass with its name order = atlas order; 3x scale"""
    objs = []; x = 0; y = 0; band = 1
    for n, o in at['objects'].items():
        if x + o['w'] > cols: x = 0; y += band; band = 1
        objs.append((n, x, y)); x += o['w']; band = max(band, o['h'])
    H = y + band; ter = {(i, j): 'grass' for i in range(cols) for j in range(H)}
    m = render.render(img, at, cols, H, ter, {}, objs, [])
    Image.fromarray(m).resize((cols * T * 3, H * T * 3), Image.NEAREST).convert('RGB').save(path)
