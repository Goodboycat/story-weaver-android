"""Terrain autotile sets (8 tiles: 4 shape + 4 full variants) and 16-tile NESW line sets.
One table row = one complete set. Row order = draw priority (later cuts into earlier)."""
import numpy as np
from core import *

SAFE = set('grass flowers tallgrass sand dirt mud ash snow flat moss swamp fungal blight stars void water deep bone coral bubbles cloud'.split())

def variants(style, rp, acc=None, seed=0):
    out = []
    for k in range(4):
        if style in SAFE: t = texture(style, rp, seed + k * 13, acc)
        else:
            t = texture(style, rp, seed, acc)
            if k:
                x0, y0 = 3 + ri(k, 0, seed + 5, 8), 3 + ri(k, 1, seed + 5, 8)
                for i in range(4): t[y0 + (i + 1) // 2, x0 + i] = rp[0 if k == 1 else (3 if k == 2 else 1)]
        out.append(np.dstack([t, np.full((T, T), 255, np.uint8)]))
    return out

def make_terrain(style, color, edge, a=None, i=2, r=3, s=0, f=None, h=6, fs='strata'):
    rp = ramp(color); vs = variants(style, rp, a, s); base = vs[0]
    top = lambda x, y: tuple(int(v) for v in base[y, x, :3])
    if edge == 'soft': sh = autotile(top, i, r, 'soft', rp)
    elif edge == 'none': sh = autotile(top, 0, 0, 'none', rp)
    elif edge == 'sunk':
        sh = autotile(top, 1, 3, 'sunk', rp, face_h=3, face_fn=lambda x, y, row, hh: rp[4] if row == hh - 1 else (0, 0, 0))
        for t in sh:
            m = (t[:, :, :3].sum(2) == 0) & (t[:, :, 3] == 255); t[m, 3] = 95
    else:
        fr = ramp(f or color)
        ft = texture('bigbrick' if fs == 'bigbrick' else 'brick', fr, s + 3)
        def face(x, y, row, hh):
            if fs in ('brick', 'bigbrick'): return tuple(int(v) for v in ft[(row + 1) % T, x])
            if fs == 'trunks': return fr[3] if x % 5 == 2 else (fr[1] if x % 5 == 3 else (14, 26, 26))
            if fs == 'planks': return fr[0] if x % 4 == 3 else (fr[3] if row == 0 else fr[2])
            if fs == 'smooth': return fr[3] if row == 0 else (fr[1] if row >= hh - 2 else fr[2])
            if fs == 'glow': return fr[4] if (x + row) % 5 == 0 else (fr[3] if row < 2 else fr[1])
            if rnd(x // 2, 0, s + 9) > 0.78 and row > 0: return fr[0]
            return fr[3] if row == 0 else (fr[2] if row < hh * 0.55 else fr[1])
        sh = autotile(top, i, r, 'raised', rp, face_h=h, face_fn=face, lip=(rp[3], rp[0], rp[1]))
    return sh[:4] + vs

TR = 'f=#7A5234,h=5,fs=trunks,i=1,r=4'
WL = 'h=9,i=0,r=0,fs='
RF = 'h=7,i=0,r=1,fs='
# name style colour edge category options
TABLE = f'''
void void #1A1228 soft dimension
starfield stars #2A2A6A soft dimension a=#FFE9A0
ether stars #2E7A8A soft dimension a=#C8FFF0,s=4
sand sand #E8D28A soft ground
white_sand sand #F2EAD0 soft ground
red_sand sand #D98A55 soft ground
black_sand sand #4A4658 soft ground
dunes dunes #E3C272 soft ground
salt_flat cracked #E9E4DA soft ground
dirt dirt #A9793F soft ground
dry_earth cracked #B98D5A soft ground
clay dirt #C0694A soft ground
mud mud #6E5238 soft ground
gravel cobble #9A968E soft ground s=4
rock_ground rock #8C8A91 soft ground
basalt rock #4B4A57 soft ground s=2
ash ash #6B6670 soft ground
scorched cracked #3D3238 soft ground s=3
grass grass #58B04A soft ground
lush_grass grass #3FA85A soft ground s=1
dark_grass grass #2F7D4C soft ground s=2
dry_grass grass #B5B24E soft ground s=3
autumn_grass grass #C98B3A soft ground s=4
tall_grass tallgrass #3E9A45 soft ground
flower_meadow flowers #5DB550 soft ground a=#F2647A
bluebell_meadow flowers #55AC58 soft ground a=#6A8BF0,s=2
sunflower_meadow flowers #62B048 soft ground a=#F5C83A,s=5
moss moss #5E8F3A soft ground
swamp swamp #556B3A soft ground
tundra grass #9AA88C soft ground s=6
snow snow #DCE8F2 soft ground
ice ice #9FD4EE soft ground
farmland farmland #8A5A35 soft ground i=1,r=1
crops crop #7A5632 soft ground i=1,r=1
wheat wheat #8A6A30 soft ground a=#E8C24A,i=1,r=1
seabed sand #4E86A0 soft ground s=7
coral_reef coral #3F8FA8 soft ground a=#F27A8E
fungal fungal #6A4E8A soft ground a=#6FE8D8
bone_field bone #D8D0B8 soft ground
blight blight #4A2E5E soft dimension a=#B43AE0
shadow_ground ash #2A2438 soft dimension s=5
crystal_ground crystal #7AD0E8 soft dimension
amethyst_ground crystal #A070E0 soft dimension
cloud cloud #E4EEF8 soft dimension r=4
storm_cloud cloud #6A7088 soft dimension r=4,s=3
dream_ground cloud #F0B8E0 soft dimension s=6
fey_grass flowers #48C8A0 soft dimension a=#F0A0F8,s=8
ember_ground lava #5A2A22 soft dimension
circuit_ground circuit #1E4A4A soft dimension a=#5AF0C8,i=0,r=0
clockwork_ground metal #A8843A soft dimension i=0,r=0
flesh_ground scales #A85A6A soft dimension
mirror_ground glass #C0D8E8 soft dimension i=0,r=0
path dirt #D0A868 soft path s=8
sand_path sand #E8D8A8 soft path s=3
cobble_road cobble #A8A49A soft path i=1,r=2
brick_road brick #B8704A soft path i=1,r=1
stone_plaza tiles #C8C2B4 soft path i=0,r=0
snow_path snow #B8C8D8 soft path s=5
dark_road cobble #4A4458 soft path i=1,r=2,s=2
gold_road brick #E0B040 soft path i=1,r=1
red_brick_road brick #A84A3A soft path i=1,r=1,s=2
white_road cobble #E4E0D8 soft path i=1,r=2,s=3
moss_road cobble #7A9A6A soft path i=1,r=2,s=4
marble_plaza tiles #ECE8F0 soft path i=0,r=0,s=2
dark_plaza tiles #4A4658 soft path i=0,r=0,s=3
ash_path dirt #6A6468 soft path s=6
ice_path snow #A8DCF0 soft path s=7
wood_boardwalk planks #B8844A soft path i=0,r=0
water water #3F8FE0 sunk liquid
shallow_water water #6AC0EA sunk liquid s=2
deep_water deep #2A5CB8 soft liquid r=4
swamp_water water #4E7A5A sunk liquid s=3
ice_water water #8AD0E0 sunk liquid s=4
lava lava #E8501E sunk liquid
acid bubbles #8AD83A sunk liquid
poison bubbles #9A4AC8 sunk liquid s=2
tar bubbles #2A2630 sunk liquid s=3
quicksilver water #B8C4D0 sunk liquid s=5
ether_stream water #58E0D0 sunk liquid s=6
void_pool stars #3A1A6A sunk liquid a=#E0A0FF
honey bubbles #E8A82A sunk liquid s=4
sewage water #6A7A3A sunk liquid s=7
wood_floor planks #B8844A none floor
dark_wood_floor planks #7A5234 none floor s=2
light_wood_floor planks #D8B078 none floor s=3
parquet_floor planks_v #A8743A none floor
stone_floor slab #9A98A0 none floor
dungeon_floor cobble #6A6878 none floor s=3
cave_floor rock #7A6A5A soft floor s=4
tile_floor tiles #E0DCD0 none floor
checker_floor checker #8A8A9A none floor
marble_floor tiles #E8E4F0 none floor s=2
sandstone_floor slab #D8B878 none floor s=2
brick_floor brick #A8604A none floor
red_carpet carpet #B8303A none floor
blue_carpet carpet #3A58B8 none floor
green_carpet carpet #3A8A4A none floor
purple_carpet carpet #7A3AA8 none floor
gold_floor tiles #E0B840 none floor s=3
tatami_floor tatami #C8C078 none floor
metal_floor metal #7A8490 none floor
grate_floor grate #5A6068 none floor
glass_floor glass #A8E0E8 none floor
rune_floor rune #4A4A6A none floor a=#6AE8F8
dark_rune_floor rune #3A2A4A none floor a=#D85AF0
ice_floor ice #B8E4F4 none floor s=2
obsidian_floor obsidian #2E2A3E none floor
bone_floor bone #C8C0A8 none floor s=2
moss_stone_floor slab #7A8A6A none floor s=4
coral_floor scales #E88A9A none floor
cloud_floor cloud #F4F8FF none floor s=4
circuit_floor circuit #2A2A4A none floor a=#F85AA8
cliff rock #9A8A78 raised cliff f=#8A7460
grass_plateau grass #62B852 raised cliff f=#9A7A58,s=5
sand_plateau sand #EAD694 raised cliff f=#C8A060
red_mesa sand #D88A58 raised cliff f=#B05A3A
snow_plateau snow #E4EEF6 raised cliff f=#8A98B0
ice_cliff ice #A8DCF0 raised cliff f=#6AA8D8,fs=smooth
volcanic_cliff rock #4A444E raised cliff f=#3A2E36,s=3
blight_cliff blight #4E3462 raised cliff f=#2E2040,a=#B43AE0
crystal_cliff crystal #8AD8EE raised cliff f=#4A98C8,fs=glow
cloud_bank cloud #F0F6FF raised cliff f=#A8B8D8,h=4,fs=smooth,r=4
floating_island grass #6AC060 raised cliff f=#7A6048,h=7,s=7
mountain rock #A8A4AC raised cliff f=#6E6A78,h=7,s=6
snow_mountain snow #EEF4FA raised cliff f=#7E8AA0,h=7,s=8
dark_mountain rock #5A5468 raised cliff f=#3A3448,h=7,s=9
moss_cliff moss #6A9A50 raised cliff f=#7A7468,s=4
bone_cliff rock #E0D8C0 raised cliff f=#A89E88,s=10
ember_cliff rock #6A3A30 raised cliff f=#3A2020,s=11,a=#F0701E
coral_cliff rock #F08A90 raised cliff f=#B85A70,s=12
forest canopy #3E9448 raised forest {TR}
pine_forest pines #2E7A58 raised forest {TR},r=3
jungle canopy #2A8A4E raised forest {TR},s=2
autumn_forest canopy #D8843A raised forest {TR},s=3
cherry_forest canopy #F0A0C0 raised forest {TR},s=4
snow_forest pines #C8DCE8 raised forest {TR},r=3,s=2
dead_forest canopy #6A5E58 raised forest {TR},s=5,f=#3E3430
blight_forest canopy #5A3A7A raised forest {TR},s=6,f=#2E2238
mushroom_forest canopy #C85A8A raised forest {TR},s=7,f=#E0D8C0
fey_forest canopy #48C8B0 raised forest {TR},s=8,f=#6A5080
crystal_forest crystal #B890F0 raised forest f=#6A4AA8,h=5,fs=glow,i=1
kelp_forest canopy #2E8A7A raised forest {TR},s=9,f=#1E5A5A
bamboo_forest pines #7AC050 raised forest {TR},r=3,s=4,f=#9AB060
golden_forest canopy #E8C040 raised forest {TR},s=10
palm_forest canopy #58B048 raised forest {TR},s=11,f=#B89060
ember_forest canopy #C8402A raised forest {TR},s=12,f=#3A2A2A
frost_pine_forest pines #8AB8D8 raised forest {TR},r=3,s=6,f=#5A6A88
shadow_forest canopy #2E2A48 raised forest {TR},s=13,f=#1E1A28
stone_wall slab #6A6874 raised wall {WL}brick,f=#9A98A4
brick_wall flat #6A3A30 raised wall {WL}brick,f=#B8604A
wood_wall flat #5A3E28 raised wall {WL}planks,f=#A8743E
plaster_wall flat #8A7A60 raised wall {WL}smooth,f=#E8DCC0
dungeon_wall flat #2E2C3A raised wall {WL}bigbrick,f=#5A586A
cave_wall rock #4A4038 raised wall f=#6E5E50,h=8,i=0,r=2,s=2
sandstone_wall flat #A88848 raised wall {WL}bigbrick,f=#E0C080
ice_wall ice #7AB8E0 raised wall {WL}smooth,f=#B8E0F4
dark_wall flat #1E1A2A raised wall {WL}bigbrick,f=#3E3450
marble_wall flat #B8B4C8 raised wall {WL}smooth,f=#F0ECF8
metal_wall metal #4A5460 raised wall {WL}planks,f=#8A96A4
crystal_wall crystal #6AB8E0 raised wall {WL}glow,f=#A8E8F8
hedge canopy #3A8A40 raised wall f=#2A6A34,h=4,fs=smooth,i=1,r=2,s=4
bone_wall bone #B8B098 raised wall {WL}planks,f=#E0D8C0
coral_wall coral #C85A78 raised wall f=#F08AA0,h=8,i=0,r=2,a=#FFE0A0
log_wall flat #4E3622 raised wall {WL}planks,f=#8A5E34,s=3
moss_wall moss #4A5A44 raised wall {WL}brick,f=#7A8A74
gold_wall flat #A8782A raised wall {WL}bigbrick,f=#F0C850
cloud_wall cloud #C8D8F0 raised wall f=#F4F8FF,h=8,fs=smooth,i=0,r=3
tech_wall circuit #1E2A3A raised wall {WL}glow,f=#3A5A7A,a=#5AF0C8
paper_wall flat #5A4030 raised wall {WL}planks,f=#F0E8D0,s=5
flesh_wall scales #7A3A4A raised wall f=#B86A7A,h=8,i=0,r=2
roof_red shingles #C8483A raised roof {RF}smooth,f=#E8DCC0
roof_blue shingles #3A68C0 raised roof {RF}smooth,f=#E8E4D8
roof_green shingles #3A8A58 raised roof {RF}smooth,f=#E0D8C0
roof_purple shingles #6A3A98 raised roof {RF}brick,f=#4A4458
roof_thatch thatch #D0A850 raised roof {RF}planks,f=#A8743E,r=2
roof_slate shingles #5A6070 raised roof {RF}brick,f=#9A98A4
roof_tile rooftile #D07A3A raised roof {RF}smooth,f=#F0E0B8
roof_snow snow #E8F0F8 raised roof {RF}planks,f=#8A5E34,r=2
roof_gold rooftile #E8B830 raised roof {RF}smooth,f=#F4F0E8
roof_jade rooftile #3AA890 raised roof {RF}planks,f=#C8483A
roof_orange shingles #E08A3A raised roof {RF}smooth,f=#E8DCC0
roof_black shingles #34303E raised roof {RF}brick,f=#6A6878
roof_pink shingles #E88AB0 raised roof {RF}smooth,f=#F4ECE8
roof_teal rooftile #2F6A70 raised roof {RF}planks,f=#F0E8D8
roof_copper rooftile #B87333 raised roof {RF}brick,f=#C8A070
roof_moss thatch #6A9A48 raised roof {RF}planks,f=#8A6A44,r=2
'''

def build_terrains(atlas):
    atlas.section('terrain')
    for p, row in enumerate(TABLE.strip().splitlines()):
        f = row.split()
        kw = {}
        for o in (f[5].split(',') if len(f) > 5 else []):
            k, v = o.split('='); kw[k] = v if k in ('a', 'f', 'fs') else int(v)
        atlas.terrain(f[0], make_terrain(f[1], f[2], f[3], **kw), p, cat=f[4], c=f[2])

# ---------------------------------------------------------------- line sets, index = N1 E2 S4 W8
def line_mask(bits, half):
    m = np.zeros((T, T), bool); a, b = 8 - half, 8 + half
    m[a:b, a:b] = True
    if bits & 1: m[0:b, a:b] = True
    if bits & 4: m[a:T, a:b] = True
    if bits & 8: m[a:b, 0:b] = True
    if bits & 2: m[a:b, a:T] = True
    return m

STY = dict(road='dirt', river='water', glow='flat', carpet='carpet', bridge='planks', wall='brick', hedge='canopy', stakes='planks_v', pipe='flat')

def make_line(kind, color, half, c2=None):
    rp = ramp(color); r2 = ramp(c2 or color); out = []
    for bits in range(16):
        t = np.zeros((T, T, 4), np.uint8)
        def put(x, y, c):
            if 0 <= x < T and 0 <= y < T: t[y, x] = (*c, 255)
        if kind == 'fence':
            for y0 in (6, 10):
                if bits & 8:
                    for x in range(0, 8): put(x, y0, rp[3]); put(x, y0 + 1, rp[1])
                if bits & 2:
                    for x in range(8, 16): put(x, y0, rp[3]); put(x, y0 + 1, rp[1])
            if bits & 1:
                for y in range(0, 6): put(7, y, rp[3]); put(8, y, rp[1])
            if bits & 4:
                for y in range(10, 16): put(7, y, rp[3]); put(8, y, rp[1])
            for y in range(2, 14):
                for x in range(5, 11):
                    put(x, y, OUT if x in (5, 10) or y in (2, 13) else (r2[4] if y == 3 else (rp[2] if x < 8 else rp[1])))
        else:
            m = line_mask(bits, half); tex = texture(STY[kind], rp)
            nb = lambda x, y: bool(m[y, x]) if 0 <= x < T and 0 <= y < T else True
            for y in range(T):
                for x in range(T):
                    if not m[y, x]: continue
                    c = tuple(int(v) for v in tex[y, x])
                    u, d, l, r = nb(x, y - 1), nb(x, y + 1), nb(x - 1, y), nb(x + 1, y)
                    if kind in ('wall', 'hedge', 'stakes'):
                        if not (d and l and r): c = OUT
                        elif not u: c = rp[4]
                        elif not nb(x, y + 3): c = rp[1]
                    elif not (u and d and l and r):
                        c = {'river': rp[4] if not (u and l) else rp[1], 'carpet': r2[3], 'bridge': r2[1], 'pipe': rp[0]}.get(kind, rp[1] if not (d and r) else rp[3])
                    elif kind == 'glow': c = rp[4] if (x + y) % 3 == 0 else rp[3]
                    elif kind == 'pipe' and not (nb(x, y - 2) and nb(x - 2, y)): c = rp[4]
                    put(x, y, c)
        out.append(t)
    return out

LINES = '''
trail road #C8A060 2
road road #D0B078 4
stone_road road #A8A4A0 4
dark_trail road #5A4A5E 3
snow_trail road #B8C8DC 3
sand_trail road #F0E0B0 3
gold_trail road #E8C040 3
stream river #4A9AE8 2
river river #3F8FE0 4
lava_stream river #F0601E 3
swamp_stream river #4E7A5A 3
poison_stream river #9A4AC8 3
ether_river river #58E0D0 3
ice_stream river #A8E0F0 3
wood_bridge bridge #B8844A 5 #6A4628
stone_bridge bridge #A8A4AC 5 #6A6874
rope_bridge bridge #C8A870 3 #8A6A40
crystal_bridge bridge #A8E0F0 4 #6AA8D8
wood_fence fence #B8844A 0
white_fence fence #E8E4D8 0
dark_fence fence #4A3E4E 0
iron_fence fence #5A5E6A 0
gold_fence fence #E0B040 0
low_stone_wall wall #9A98A4 3
castle_rampart wall #7A7888 4
sandstone_rampart wall #D8B878 4
dark_rampart wall #3E3450 4
ice_rampart wall #A8D8F0 4
hedge_row hedge #3A8A40 4
thorn_row hedge #5A3A6A 4
palisade stakes #8A5E34 3
pipe pipe #7A8A94 2
copper_pipe pipe #C8783A 2
red_runner carpet #B8303A 4 #E8C040
blue_runner carpet #3A58B8 4 #E8E4D8
purple_runner carpet #7A3AA8 4 #E8C040
ley_line glow #5AD8F0 1
corruption_vein glow #B43AE0 1
lava_crack glow #F0701E 1
fissure glow #2A2230 2
mud_trail road #7A5A3C 3
ash_trail road #6A6468 3
moss_trail road #7A9A6A 3
acid_stream river #8AD83A 3
tar_stream river #2E2A34 3
blood_stream river #B02A34 3
bone_fence fence #E0D8C0 0
bamboo_fence fence #8AB850 0
moss_rampart wall #6A8A5A 4
red_brick_rampart wall #A84A3A 4
marble_rampart wall #ECE8F0 4
flower_hedge hedge #D86AA0 4
snow_hedge hedge #D8E8F0 4
bone_palisade stakes #D8D0B8 3
green_runner carpet #2F8A50 4 #E8C040
black_runner carpet #2E2A38 4 #B8303A
frost_line glow #C8F0FF 1
nature_line glow #6AE06A 1
'''

def build_lines(atlas):
    atlas.section('lines')
    for row in LINES.strip().splitlines():
        f = row.split()
        atlas.lineset(f[0], make_line(f[1], f[2], int(f[3]), f[4] if len(f) > 4 else None), kind=f[1])
