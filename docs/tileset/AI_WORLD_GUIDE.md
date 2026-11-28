# AI World Guide (Map Architect)

How the API AI turns plain text ("make a snowy kingdom with a river") into a tile map, and how to edit one.
Code: `ai/AiParserService.kt` (`mapArchitectSystemPrompt`), `data/MapMacroOps.kt` (macro ops + `MapTileCatalog`),
`data/MapPaintEngine.kt` (shape ops, layering), short recipes in `data/GuidesContent.kt` (`MAP_AI_RECIPES`).

## What the AI returns
One JSON object: `width`, `height`, `clearFirst`, `clearTokens`, `summary`, `operations[]`, `tokens[]`.
Names only, never tile indices. Unknown names are snapped to the closest catalogue name (prefix, then contains);
if nothing matches, the op is dropped.

## Layers
A cell has three layers: terrain (`tile`), line (`line`), object (`obj`). Painting a terrain resets the cell;
lines and objects stack on top. Order of ops: ground -> liquid -> path terrain -> raised (cliff, forest, walls,
roofs) -> lines -> objects -> tokens.

## Ops
| op | keys | notes |
|---|---|---|
| rect / rect_border / circle / paint / scatter / flood_fill / checker | box or points + ONE of `tile`, `line`, `obj` | legacy `colorHex` + `emoji` still work |
| line | `x1,y1,x2,y2` or `points` polyline + `line` | tileset lines are made 4-connected so NESW tiles join |
| place | `obj`, `x,y` or `points` | x,y = top-left cell of the object |
| building | box, `roof`, `wall`, `door`, `window`, `doorX`; `floor` => interior | exterior = roof footprint + wall row + door bottom-centre |
| biome | box, `terrains` {name: weight}, `scale`, `props` {name: weight}, `density`, `seed` | smooth-noise patches; first terrain = lowest ground; props avoid liquid / wall / cliff / roof / forest |
| kingdom | box, `kingdom`, `ground`, `wall`, `walls`, `road`, `castle`, `count`, `seed` | rampart ring + south gate + roads + castle + N buildings |
| erase | box | clears all layers |

Tokens: `{label, sprite, emoji, kind, x, y}`; `sprite` is a name from the sprites catalogue, `emoji` the fallback.

## Editing an existing map
The user prompt carries `CURRENT MAP STATE` (tile names per region, objects, tokens). In edit mode the AI sets
`clearFirst=false` and returns only the ops that change something, e.g. "add a bridge over the river at the road"
-> one `paint` with `line: wood_bridge`.

## Recipes
- Overworld route: biome (grass 5, tall_grass 2, + props trees/bushes/rocks, density 0.05-0.1) -> forest or cliff terrain blocks along two edges -> water circle or river line -> one road or trail line from edge to edge -> bridge cell where it crosses water -> 1-3 landmarks (sign, well, shrine, kingdom icon_* objects for far-away places).
- Town / village: grass or dirt ground -> kingdom op (walls false for a village) OR hand-place <kingdom>_* buildings along a road line with 1 free cell between them -> plaza terrain (stone_plaza) with a fountain or well -> props from the village category (lamp posts, crates, benches, stalls) -> fence line around fields of farmland / crops / wheat.
- Interior: fill the grid with a wall terrain -> building op with `floor` per room (rooms share walls) -> doors (door_*_n/e/s/w on the wall cell) -> furniture along the walls (beds, tables, shelves, counters l+m+r), rugs in the middle, fireplace or stove on the top wall.
- Dungeon: fill with dungeon_wall or cave_wall -> rooms as rect of dungeon_floor / cave_floor -> 1-2 wide corridors as line with a floor `tile` (brushSize 1-2) -> doors and iron gates in the corridor mouths -> dungeon props (chests, braziers, spikes, levers, sarcophagus, webs in corners) -> hazards as liquid terrain (lava, poison, deep_water) -> stairs object as entrance and exit.
- Kingdom capital: 24x24 or bigger: ground biome -> kingdom op filling most of the grid -> a river or moat outside the rampart -> farms and a road leaving the south gate. Match the style to the people: human, elven, dwarven, orc, desert, frost, eastern, merfolk, sky, undead, beastfolk, fey, gnome, dragon, nomad, dark.
- Dimension / plane: ground from the dimension terrain category (starfield, void, ether, cloud_floor, dream_ground, mirror_ground, ember_ground...) -> islands as circles of floating_island or a solid terrain -> ley_line / ether_river / crystal_bridge lines joining the islands -> <dimension>_* objects (portal, float_rock, spire, orb, wisp + the specials). Dimensions: astral, void, feywild, shadow, dream, spirit, mirror, fire, water, air, earth, mechanus, abyssal, underdark.
- Snow, desert, swamp, volcano: same as the overworld route with that ground family (snow / snow_path / ice / snow_forest; sand / dunes / sand_path / red_mesa; swamp / swamp_water / mud; ash / scorched / basalt / lava) and matching props (tree_snowy, cactus, tree_dead, rock variants).

## Content rule
No demon / hellish content. Evil realms are the Corrupted Kingdom or Dark Kingdom (`dark`, `undead` kingdoms,
`shadow` / `void` dimensions).

## Not done / ideas
- `biome` without `terrains` only scatters props: it reads the ground from the map being edited plus every earlier op in the same script (rect / circle / paint / macros) and skips liquid, wall, cliff, roof, forest, lines and objects.
- `kingdom` joins each building door (bottom-centre) to the nearest road with a spur.
- The system prompt only lists the object / sprite categories whose keywords appear in the request (`MapTileCatalog.wantedCats`); terrains, lines, nature, village and kingdoms are always listed. Unlisted names still snap.
- `kingdom` places buildings on a shuffled free-spot search, not along streets; doors may not face a road.
- Multi-tile objects are not clipped at the map edge.
