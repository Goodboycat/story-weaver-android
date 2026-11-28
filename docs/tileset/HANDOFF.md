# Tileset + AI Map Architect - HANDOFF (read this first)

Goal: replace the emoji/colour map tab with a GBA-style (FireRed-like) 16px tileset that an API AI
drives from plain text ("make a snowy kingdom with a river"), including edits to an existing map.

## Hard rules from the owner
- Art is PROCEDURAL (Python + numpy + Pillow in `tools/tileset/`). No image-generation models.
- Grid is uniform 16x16, zero gaps / borders / labels, transparent background for overlays.
- NO demon content, ever. Delete any you find. Use "Corrupted Kingdom" / "Dark Kingdom" instead.
  Still to remove in app code: the ogre/goblin emoji used as monster defaults (`MapPaintEngine.kt`
  ~l.563 and ~l.682, `AiParserService.kt` l.48/157/373/644, `EntitiesTab.kt` l.158,
  `EmojiPickerDialog.kt` l.131-132) -> swap for a neutral beast emoji / tileset sprite.
- Everything faces FOUR directions (sprites: down, left, right, up).
- Pull before push, push after every task. Never ask the owner questions; decide.
- Each chat has < 75k tokens. Do TWO or THREE lettered tasks below per chat (a props part costs ~8k tokens:
  one write, one build, one preview look, one fix), build, look at
  `docs/tileset/props.png` / `demo.png`, move the task from TODO to DONE here, commit, push, stop.
  Do not `cat` whole source files: use `grep -n "^def \|^class "` and `sed -n` on the lines you need.

## The tile-saving trick (what the owner "forgot")
Quarter-tile ("mini / A2-style") autotiling. Each terrain stores only 8 tiles:
`[0]=isolated (4 outer corners) [1]=horizontal run (top+bottom edges) [2]=vertical run (left+right
edges) [3]=cross (4 inner corners) [4..7]=four full variants`. The renderer cuts each map cell into
four 8x8 quadrants and picks the source tile per quadrant from 3 neighbours (see
`tools/tileset/render.py`, ~20 lines). That replaces the classic 47-tile blob set: every edge, inner
and outer corner, in every direction, from 4 shape tiles. Linear things (roads, rivers, fences,
walls) use a 16-tile set indexed by a NESW bitmask (N=1,E=2,S=4,W=8). Left-facing sprites can be a
mirrored right-facing one. Terrains have a priority `p`: a cell connects to neighbours with `p >=`
its own, and the highest lower neighbour is drawn underneath the transparent rim.

## Files
- `tools/tileset/core.py`  ramps, tileable textures (`shade_map` styles), autotile mask builder,
  sprite canvas `C` (ell/box/tri/outline/shadow/flip), `Atlas` packer (64 tiles wide).
- `tools/tileset/terrains.py`  TERRAINS + LINES tables (one text row = one whole set).
- `tools/tileset/render.py`  reference renderer = the spec for the Kotlin port.
- `tools/tileset/build.py`  builds everything. Run: `python3 tools/tileset/build.py`.
- `tools/tileset/props.py`  objects. One `part_xxx(A)` function per task, appended to `PARTS`.
  Helpers: `speck` (leaf noise), `cap` (snow/moss top), `thick` (thick line), `fin` (outline+shadow),
  `trunk`. Register with `A.obj(name, canvas, cat='..', solid=0|1)`; canvas size = multiples of 16.
- `tools/tileset/preview.py`  `python3 tools/tileset/preview.py <cat> [scale] [cols]` -> `/tmp/preview_<cat>.png`:
  zoomed sheet of ONE category. Use this to review a new part (props.png is too small to judge).
- `tools/tileset/demo_extra.py`  props placed on the demo map (`get`) + contact sheet of every
  object on grass (`sheet` -> `docs/tileset/props.png`, 3x scale). Review that image after each task.
- Outputs: `app/src/main/assets/tileset/master.png` (the master sheet), `atlas.json`
  (`terrains{name:{i,n,p,cat,c}}`, `lines{name:{i}}`, `objects{name:{i,w,h}}`,
  `sprites{name:{i}}` + 4 dirs; `i` = tile index, x=i%64, y=i/64), `docs/tileset/demo.png`.

## DONE
1. Engine (core.py), terrain + line generators, renderer, build script, first master sheet.
2a. Props / nature (`part_nature`, 67 objects, cat `nature`): 21 trees 2x3 (oak, oak_dark, autumn,
    golden, cherry, apple, snowy_oak, blight, fey, pine, pine_dark, snowy, palm, dead, dead_ash,
    crystal x3, giant_mushroom x3), 5 boulders 2x2, 2 cacti 1x2, 2 logs 2x1, 10 bushes, 9 rocks
    (3 ore), 3 stumps, 3 mushrooms, 8 flowers, reeds x2, lilypads x2. Sheet is now 2111 tiles.
2b. Props / village (`part_village`, 47 objects, cat `village`): signs (wood, blank, post_e/w, hanging inn/smith/
    potion/food), wells 1x2 (stone, roof, roof_blue), crates, barrels, sacks, pots, chests wood/iron/gold
    `_closed`/`_open`, torch_stand, torch_wall, campfire_lit/out, lamp_post(_lit) 1x2, hay_bale, carts 2x1,
    fence_gate_closed/open, mailbox, bench, scarecrow 1x2, troughs, woodpile. New helpers in props.py: `px`
    (pixel list), `icon` (6x5 pixel-art glyph from `ICONS`), `flame`, `glow` (alpha halo, call AFTER `fin`).
2c. Props / monument (`part_monument`, 35 objects, cat `monument`): graves x6, statues 1x2 (knight, mage, king,
    guardian, broken, dark), pillars 1x2 (stone, marble, broken, dark, vine, sand), obelisks 1x2 x3, fountains 2x2
    x3, shrines x3, standing stones x3, stone_circle 2x2, rubble x4. Shared colours: MARBLE, DARKSTONE, MOSS, RUNE.
2d. Props / doors (`part_doors`, 61 objects, cat `door`): `door_{wood,iron,gold,dark}_{closed,open}_{n,e,s,w}`
    (s = outside face, n = inside face with crossbar, e/w = thin side view on that edge; closed = solid),
    `stairs_{stone,wood}_{up,down}_{n,e,s,w}` (opaque full-tile floor pieces, rotated), ladder_up/down,
    trapdoor(_open), windows x6 (wood, stone, lit, dark, barred, stained), archway 2x2 x3 (walk-through).
2e. Props / furniture (`part_furniture`, 87 objects, cat `furniture`): bed_single/double_{red,blue,green,purple,white}
    (1x2 / 2x2), table 2x1 (plain, cloth, dark, feast), table_round, table_small(_mug/_candle/_book/_orb),
    chair_{n,e,s,w} + chair_cushion_*, stools, shelf_{jars,plates,books}, bookshelf 1x2 x5, counter_{wood,stone,dark}_{l,m,r}
    (line-like: l + m.. + r), stove, fireplaces, thrones 1x2 x4, altars x3, anvil, forge_lit/out, cauldrons x4, looms,
    rug_round_* 1x1 + rug_* 2x2 (solid=0, no outline), wardrobes, mirrors, barrel_rack 2x1, weapon_rack x3,
    plant pots, bathtub 2x1. Shared colours: LINEN, RED, BLUE, GREEN, PURPLE, LIGHTWOOD, INK.
2f. Props / dungeon (`part_dungeon`, 57 objects, cat `dungeon`): spikes_up/down, lever_left/right, pressure_plate(_gold)_up/down,
    cage(_bones) 1x2, web_{nw,ne,se,sw}, bones_pile/skull/ribs, skull_pile, chains x3, portal_{blue,green,purple,red,gold} 1x2 +
    portal_small_* 1x1, rift x3 1x2, rune_circle x4 2x2 (square+diamond knot - never a pentagram), braziers x4, sarcophagus x4 1x2,
    iron_gate_closed/open, crystal_cluster x4, stalagmites x5. Sheet is now 2521 tiles, 354 objects.

## TODO (in order, 2-3 lettered tasks per chat; push after each)
Naming: snake_case, variant as suffix (`door_wood_closed_s`). Directions suffix `_n/_e/_s/_w`.

### 2. Props (rest) - `props.py`, one `part_*` each
- (all props parts done; add more `part_*` only if a later task needs them)

### 3. Kingdoms - `kingdoms.py` (new file; `build(A)` is already auto-called by build.py)
- 3a. Parametric building generator `building(w, h, pal, roof, trim)` + kingdoms Human, Elven,
  Dwarven, Orc. Per kingdom: house S (2x2) / M (3x3) / L (4x3), shop, inn, temple, tower (2x4),
  gate (3x3), castle (4x4), banner, market stall, lamp, + 1-tile world icons village / town /
  city / castle / ruin. Names `human_house_s`, `human_icon_town`, cat `kingdom_human`.
- 3b. Desert, Frost, Eastern, Merfolk.
- 3c. Sky/Celestial, Undead, Beastfolk, Fey.
- 3d. Gnome/Clockwork, Dragon, Nomad, Corrupted/Dark (no demon imagery).

### 4. Dimensions - `props.py` `part_dimension_*` (cat `dim_<name>`)
- 4a. Astral, Void, Feywild, Shadow, Dream, Spirit, Mirror: portals, floating rocks, star props,
  crystal spires, cloud props.
- 4b. Elemental Fire/Water/Air/Earth, Mechanus (gears, pipes), Abyssal Sea (coral, kelp, shells,
  wreck), Underdark (glow fungi, stalagmites). Ground terrains for all already sit in TERRAINS.

### 5. People + creatures - `sprites.py` (new file, auto-called)
- 5a. Parametric humanoid `humanoid(skin, hair, outfit, hat, item)` drawn down / right / up, left =
  flip of right -> `atlas.sprite(name, [d, l, r, u], cat=..)`; add sprites to `demo_extra.get`
  (second return value: `(name, x, y, dir)`) and to the contact sheet. First 20 classes/jobs.
- 5b. 40 more humanoids: races (elf, dwarf, orc, beastfolk, merfolk, undead...) + jobs.
- 5c. Creatures 1: ~40 (slime, wolf, bear, boar, spider, bat, rat, snake, skeleton, ghost, zombie...).
- 5d. Creatures 2: ~40 (golem, dragon, wyvern, elemental, treant, kraken, griffin, corrupted
  knight...). No demons.

### 6. Kotlin
- 6a. `data/TileAtlas.kt`: load `tileset/master.png` + `atlas.json` from assets once, cache Bitmap.
- 6b. `GridCell` gets `tile: String?`, `line: String?`, `obj: String?` (+ encode/decodeGrid in
  `Models.kt`, backward compatible); `MapToken` gets `sprite` + `dir`.
- 6c. Port `render.py` into the Canvas draw in `GridMapTab.kt` (~l.930) with
  `drawImage(srcOffset, srcSize, dstOffset, dstSize, filterQuality = None)`.
- 6d. Tile palette picker grouped by `cat`; raise the 30x30 grid cap.
- 6e. Remove ogre/goblin emoji defaults (file list in "Hard rules").

### 7. AI
- 7a. `MapPaintOp` / `MapPaintJsonParser`: every op accepts `"tile"`; new ops `place` (object) and
  `line` (`"line":"river"`).
- 7b. New ops `building` (roof terrain footprint + door), `biome` (noise fill with weighted
  terrains + scatter props), `kingdom` (walls + roads + N buildings).
- 7c. Prompt in `AiParserService.kt` (~l.150-170, replacing `palettePromptGuide`): catalogue names
  grouped by `cat`, never indices; layer order ground -> liquid -> path -> raised -> lines ->
  objects -> tokens; edits return only the ops that change. `summarizeMapForPrompt` lists tile
  names per region so edits work.

### 8. Docs
- 8a. `docs/tileset/AI_WORLD_GUIDE.md` (recipes: overworld route, town, interior, dungeon, kingdom,
  dimension) + short version embedded in the system prompt.

### 9. Finish
- 9a. Add palette-swap rows to the tables where coverage is thin (sheet is already > 2000 tiles).
- 9b. Check the GitHub Actions APK build and fix what breaks.

## Known art notes (fix when touching the file)
- `tree_snowy` (pine): snow only shows on the top tier; widen the snow band on the lower tiers.
- `stairs_*`: steps read as flat stripes; add a side stringer / perspective taper.
- `window_stained`: centre rosette is hidden by the frame cross; `trapdoor_open` ladder is hard to read.
- `web_*` are faint on light ground (fine on dark dungeon floor); `brazier_*` legs read a bit like a basket.
- `door_*_closed_e/w` are a 3px strip by design (wall edge); thicken to 4px if invisible on device.
- Flowers / reeds are thin at 1x; fine as ground cover, thicken if they vanish on device.

Not yet verified: the art has not been reviewed on a device; check `docs/tileset/demo.png` and
`docs/tileset/props.png`.
