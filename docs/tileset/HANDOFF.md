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
- Outputs: `app/src/main/assets/tileset/master.png` (the master sheet), `atlas.json`
  (`terrains{name:{i,n,p,cat,c}}`, `lines{name:{i}}`, `objects{name:{i,w,h}}`,
  `sprites{name:{i}}` + 4 dirs; `i` = tile index, x=i%64, y=i/64), `docs/tileset/demo.png`.

## DONE
1. Engine (core.py), terrain + line generators, renderer, build script, first master sheet.

## TODO (in order)
2. **Props** (`props.py`, use `C` canvas + `atlas.obj(name, canvas, cat=..)`): trees (oak, pine,
   palm, dead, cherry, snowy, crystal, giant mushroom; 2x3 tiles), bushes, rocks, flowers, stumps,
   signs, wells, crates, barrels, chests (closed/open), torches, campfire, graves, statues, pillars,
   doors (wood/iron/gold/dark, open+closed, facing N/E/S/W), stairs up/down in 4 directions,
   ladders, windows, furniture (bed, table, chair x4 dirs, shelf, counter, stove, throne, altar,
   anvil, cauldron), dungeon (spikes, lever, pressure plate, cage, web, bones, portal, rift).
3. **Kingdoms** (`kingdoms.py`): one parametric building generator (palette + roof shape + trim)
   x 16 kingdoms: Human, Elven, Dwarven, Orc, Desert, Frost, Eastern, Merfolk, Sky/Celestial,
   Undead, Beastfolk, Fey, Gnome/Clockwork, Dragon, Nomad, Corrupted/Dark. Each: house S/M/L, shop,
   inn, temple, tower, gate, castle (4x4), banner, market stall, lamp, plus a 1-tile world-map icon
   (village / town / city / castle / ruin) for zoomed-out overworlds.
4. **Dimensions**: portals, floating rocks, star props, crystal spires, gears, coral, cloud props
   for Astral, Void, Feywild, Shadow, Elemental Fire/Water/Air/Earth, Dream, Mechanus, Spirit,
   Mirror, Abyssal Sea, Underdark. Ground terrains for these already sit in TERRAINS.
5. **People + creatures** (`sprites.py`): one parametric humanoid (skin, hair, outfit, hat, held
   item) drawn for down/left/right/up -> `atlas.sprite(name, [d,l,r,u])`. ~60 classes/races/jobs
   and ~80 creatures (slime, wolf, bear, spider, bat, skeleton, ghost, golem, dragon, wyvern,
   elemental, treant, kraken, griffin, corrupted knight...). No demons.
6. **Kotlin**: `data/TileAtlas.kt` (load master.png + atlas.json from assets once, cache Bitmap),
   add `tile: String?`, `line: String?`, `obj: String?` to `GridCell` (+ encode/decodeGrid in
   `Models.kt`), port `render.py` into the Canvas draw in `GridMapTab.kt` (~l.930, use
   `drawImage(srcOffset, srcSize, dstOffset, dstSize, filterQuality = None)`), sprite name on
   `MapToken` with `dir`, a tile palette picker grouped by `cat`, raise the 30x30 grid cap.
7. **AI**: extend `MapPaintOp`/`MapPaintJsonParser` so every op accepts `"tile"` instead of
   colour/emoji, plus new ops `place` (object), `line` with `"line":"river"`, `building`
   (roof terrain footprint + door), `biome` (noise fill with weighted terrains + scatter props) and
   `kingdom` (walls + roads + N buildings). Put the catalogue in the prompt built in
   `AiParserService.kt` (~l.150-170, replacing `palettePromptGuide`): names grouped by `cat`,
   never indices. Tell the model: layer order ground -> liquid -> path -> raised -> lines ->
   objects -> tokens; edits must return only the ops that change. `summarizeMapForPrompt` must list
   tile names per region so edits work.
8. Write `docs/tileset/AI_WORLD_GUIDE.md` (recipes: overworld route, town, interior, dungeon,
   kingdom, dimension) and embed a short version in the system prompt.
9. Grow to 2000+ tiles by adding rows to the tables (palette swaps are one line each), then check
   the GitHub Actions APK build.

Not yet verified: the art has not been reviewed by eye on a device; check `docs/tileset/demo.png`.
