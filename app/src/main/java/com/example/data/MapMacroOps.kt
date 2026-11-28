package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * High-level map ops for the AI ("building", "biome", "kingdom", tileset "line"). Each one expands into plain
 * [MapPaintOp.Paint] ops carrying [MapLayerMarker] names, so the paint engine itself stays unchanged.
 * Names are checked against the atlas when it is loaded ([TileAtlas.peek]); nothing here uses tile indices.
 */
object MapMacroOps {
    private val LINE_TOOLS = setOf("line", "draw_line", "path", "corridor", "river", "road", "fence", "wall", "stream", "bridge")
    private val LINE_ALIAS = mapOf(
        "wall" to "low_stone_wall", "fence" to "wood_fence", "bridge" to "wood_bridge", "path" to "trail",
        "rampart" to "castle_rampart", "hedge" to "hedge_row",
    )
    private val NO_PROP_CATS = setOf("liquid", "wall", "cliff", "roof", "forest")
    private val KINGDOM_PARTS = listOf(
        "inn" to (4 to 3), "shop" to (3 to 3), "house_m" to (3 to 3), "temple" to (3 to 3), "house_s" to (2 to 2),
        "house_l" to (4 to 3), "house_s" to (2 to 2), "stall" to (2 to 2), "house_m" to (3 to 3), "tower" to (2 to 4),
    )

    private fun JSONObject.str(vararg keys: String): String? {
        for (k in keys) {
            if (!has(k) || isNull(k)) continue
            val v = optString(k, "").trim()
            if (v.isNotEmpty() && !v.equals("null", true) && !v.equals("none", true)) return v
        }
        return null
    }

    private fun clean(raw: String?): String? = MapLayerMarker.of("", raw)

    /** Closest real atlas name for [raw] (exact, then contains); unchanged when the atlas is not loaded. */
    private fun pick(raw: String?, names: Set<String>?): String? {
        val n = clean(raw) ?: return null
        if (names == null || names.isEmpty() || n in names) return n
        return names.firstOrNull { it.startsWith(n) } ?: names.firstOrNull { it.contains(n) }
            ?: names.filter { n.contains(it) }.maxByOrNull { it.length }
    }

    fun terrain(raw: String?): String? = pick(raw, TileAtlas.peek()?.terrains?.keys)
    fun line(raw: String?): String? = pick(clean(raw)?.let { LINE_ALIAS[it] } ?: raw, TileAtlas.peek()?.lines?.keys)
    fun obj(raw: String?): String? = pick(raw, TileAtlas.peek()?.objects?.keys)
    fun sprite(raw: String?): String? = pick(raw, TileAtlas.peek()?.sprites?.keys)

    /** Terrain per cell and cells holding a line / object, from the existing map plus the ops parsed so far. */
    private val ground = HashMap<Pair<Int, Int>, String>()
    private val blocked = HashSet<Pair<Int, Int>>()
    private var maxGridW: Int = 2000
    private var maxGridH: Int = 2000

    /** Call once before parsing a script. [grid] = the map being edited (null / empty when it is wiped first). */
    fun begin(grid: Map<String, GridCell>?, maxWidth: Int = 2000, maxHeight: Int = 2000) {
        maxGridW = maxWidth.coerceAtLeast(1)
        maxGridH = maxHeight.coerceAtLeast(1)
        ground.clear()
        blocked.clear()
        val atlas = TileAtlas.peek()
        grid?.forEach { (k, c) ->
            val xy = k.split(',')
            val x = xy.getOrNull(0)?.trim()?.toIntOrNull() ?: return@forEach
            val y = xy.getOrNull(1)?.trim()?.toIntOrNull() ?: return@forEach
            c.tile?.let { ground[x to y] = it }
            if (c.line != null) blocked += x to y
            c.obj?.let { n ->
                val ob = atlas?.objects?.get(n)
                blocked += Box(x, y, x + (ob?.w ?: 1) - 1, y + (ob?.h ?: 1) - 1).cells()
            }
        }
    }

    /** Records what [op] paints so later macros (biome props, kingdom) can respect it. */
    fun note(op: MapPaintOp) {
        val (marker, pts) = when (op) {
            is MapPaintOp.Paint -> op.colorHex to op.points
            is MapPaintOp.Rect -> op.colorHex to (if (op.hollow) Box(op.x1, op.y1, op.x2, op.y2).border() else Box(op.x1, op.y1, op.x2, op.y2).cells())
            is MapPaintOp.Circle -> op.colorHex to (if (!op.filled) emptyList() else Box(op.cx - op.radius, op.cy - op.radius, op.cx + op.radius, op.cy + op.radius)
                .cells().filter { (x, y) -> (x - op.cx) * (x - op.cx) + (y - op.cy) * (y - op.cy) <= op.radius * op.radius })
            else -> return
        }
        if (marker == null || pts.size > 500_000) return
        when {
            marker.startsWith(MapLayerMarker.TILE) -> {
                val name = marker.removePrefix(MapLayerMarker.TILE)
                pts.forEach { ground[it] = name }
                blocked -= pts.toSet()
            }
            marker.startsWith(MapLayerMarker.LINE) -> blocked += pts
            marker.startsWith(MapLayerMarker.OBJ) -> {
                val ob = TileAtlas.peek()?.objects?.get(marker.removePrefix(MapLayerMarker.OBJ))
                pts.forEach { (x, y) -> blocked += Box(x, y, x + (ob?.w ?: 1) - 1, y + (ob?.h ?: 1) - 1).cells() }
            }
        }
    }

    private fun paint(prefix: String, name: String?, pts: List<Pair<Int, Int>>): MapPaintOp? =
        if (name == null || pts.isEmpty()) null
        else MapPaintOp.Paint(points = pts, colorHex = prefix + name, brushSize = 1)

    private fun tiles(name: String?, pts: List<Pair<Int, Int>>) = paint(MapLayerMarker.TILE, name, pts)
    private fun lines(name: String?, pts: List<Pair<Int, Int>>) = paint(MapLayerMarker.LINE, name, pts)
    private fun objs(name: String?, pts: List<Pair<Int, Int>>) = paint(MapLayerMarker.OBJ, name, pts)

    private class Box(val x1: Int, val y1: Int, val x2: Int, val y2: Int) {
        val w get() = x2 - x1 + 1
        val h get() = y2 - y1 + 1
        fun cells(): List<Pair<Int, Int>> = buildList { for (y in y1..y2) for (x in x1..x2) add(x to y) }
        fun border(): List<Pair<Int, Int>> = cells().filter { (x, y) -> x == x1 || x == x2 || y == y1 || y == y2 }
    }

    private fun box(o: JSONObject): Box {
        val limX = (maxGridW - 1).coerceAtLeast(0)
        val limY = (maxGridH - 1).coerceAtLeast(0)
        val ax = o.optInt("x1", o.optInt("x", 0)).coerceIn(0, limX)
        val ay = o.optInt("y1", o.optInt("y", 0)).coerceIn(0, limY)
        val bx = (if (o.has("x2")) o.optInt("x2", ax) else ax + o.optInt("w", o.optInt("width", 4)) - 1).coerceIn(0, limX)
        val by = (if (o.has("y2")) o.optInt("y2", ay) else ay + o.optInt("h", o.optInt("height", 4)) - 1).coerceIn(0, limY)
        return Box(min(ax, bx), min(ay, by), max(ax, bx), max(ay, by))
    }

    /** 4-connected path (no diagonal steps) so NESW line tiles join up. */
    fun orthoPath(x0: Int, y0: Int, x1: Int, y1: Int): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        var px = x0
        var py = y0
        MapPaintEngine.bresenhamLine(x0, y0, x1, y1).forEach { (x, y) ->
            if (x != px && y != py) out += x to py
            out += x to y
            px = x
            py = y
        }
        return out.distinct()
    }

    /** Returns the expanded ops, or null when [o] is not a macro (the normal parser handles it). */
    fun expand(o: JSONObject): List<MapPaintOp>? {
        val tool = (o.str("tool", "type") ?: return null).lowercase()
        return when (tool) {
            "building", "house", "room", "interior" -> building(o, tool)
            "biome", "noise", "terrain_mix", "wilderness" -> biome(o)
            "kingdom", "town", "village", "city", "settlement" -> kingdom(o)
            in LINE_TOOLS -> tilesetLine(o, tool)
            else -> null
        }
    }

    private fun tilesetLine(o: JSONObject, tool: String): List<MapPaintOp>? {
        val hasOther = o.str("colorHex", "color", "bg", "emoji", "tile", "terrain", "obj", "object") != null
        val raw = o.str("line") ?: tool.takeIf { !hasOther && it in setOf("river", "road", "fence", "wall", "stream", "bridge") }
        val name = line(raw) ?: return null
        val pts = mutableListOf<Pair<Int, Int>>()
        val arr: JSONArray? = o.optJSONArray("points")
        if (arr != null && arr.length() >= 2) {
            var prev: Pair<Int, Int>? = null
            for (i in 0 until arr.length()) {
                val p = arr.optJSONArray(i)?.takeIf { it.length() >= 2 }?.let { it.optInt(0) to it.optInt(1) }
                    ?: arr.optJSONObject(i)?.let { it.optInt("x") to it.optInt("y") } ?: continue
                val from = prev
                if (from == null) pts += p else pts += orthoPath(from.first, from.second, p.first, p.second)
                prev = p
            }
        } else {
            pts += orthoPath(o.optInt("x1", 0), o.optInt("y1", 0), o.optInt("x2", 0), o.optInt("y2", 0))
        }
        return listOfNotNull(lines(name, pts.distinct()))
    }

    /** Exterior: roof terrain footprint + one wall row + door (+ windows). Interior (`floor` given): floor + wall ring + door. */
    private fun building(o: JSONObject, tool: String): List<MapPaintOp> {
        val b = box(o)
        val doorX = (o.optInt("doorX", b.x1 + b.w / 2)).coerceIn(b.x1, b.x2)
        val door = obj(o.str("door") ?: "door_wood_closed_s")
        val interior = tool == "room" || tool == "interior" || o.optBoolean("interior", false) || o.str("floor") != null
        val out = mutableListOf<MapPaintOp?>()
        if (interior) {
            val floor = terrain(o.str("floor") ?: "wood_floor")
            out += tiles(floor, b.cells())
            out += tiles(terrain(o.str("wall") ?: "stone_wall"), b.border())
            out += tiles(floor, listOf(doorX to b.y2))
        } else {
            out += tiles(terrain(o.str("roof") ?: "roof_red"), Box(b.x1, b.y1, b.x2, max(b.y1, b.y2 - 1)).cells())
            out += tiles(terrain(o.str("wall") ?: "plaster_wall"), (b.x1..b.x2).map { it to b.y2 })
            if (b.w >= 4) {
                val win = listOf(b.x1 + 1, b.x2 - 1).filter { abs(it - doorX) >= 1 }.distinct()
                out += objs(obj(o.str("window") ?: "window_wood"), win.map { it to b.y2 })
            }
        }
        out += objs(door, listOf(doorX to b.y2))
        return out.filterNotNull()
    }

    /** "terrains"/"props": ["grass", ...] | [{"tile":"grass","weight":3}] | {"grass":3, ...} */
    private fun weighted(o: JSONObject, key: String, vararg nameKeys: String): List<Pair<String, Double>> {
        val out = mutableListOf<Pair<String, Double>>()
        val raw = o.opt(key)
        when (raw) {
            is JSONArray -> for (i in 0 until raw.length()) {
                val item = raw.opt(i)
                if (item is JSONObject) {
                    val n = item.str(*nameKeys, "name") ?: continue
                    out += n to item.optDouble("weight", item.optDouble("w", 1.0))
                } else if (item is String) out += item to 1.0
            }
            is JSONObject -> raw.keys().forEach { k -> out += k to raw.optDouble(k, 1.0) }
            is String -> raw.split(',').forEach { if (it.isNotBlank()) out += it.trim() to 1.0 }
            else -> {}
        }
        return out.filter { it.second > 0.0 }
    }

    private fun hash(ix: Int, iy: Int, seed: Long): Double {
        var h = seed xor (ix.toLong() * 374761393L) xor (iy.toLong() * 668265263L)
        h = (h xor (h ushr 13)) * 1274126177L
        h = h xor (h ushr 16)
        return (h and 0xFFFFL).toDouble() / 65535.0
    }

    private fun noise(x: Double, y: Double, seed: Long): Double {
        val ix = kotlin.math.floor(x).toInt()
        val iy = kotlin.math.floor(y).toInt()
        val fx = x - ix
        val fy = y - iy
        val sx = fx * fx * (3 - 2 * fx)
        val sy = fy * fy * (3 - 2 * fy)
        val a = hash(ix, iy, seed) + (hash(ix + 1, iy, seed) - hash(ix, iy, seed)) * sx
        val b = hash(ix, iy + 1, seed) + (hash(ix + 1, iy + 1, seed) - hash(ix, iy + 1, seed)) * sx
        return a + (b - a) * sy
    }

    /** Smooth-noise patches: terrains listed first take the "lowest" areas; weights are honoured exactly. */
    private fun biome(o: JSONObject): List<MapPaintOp> {
        val b = box(o)
        val seed = o.optLong("seed", 42L)
        val scale = o.optDouble("scale", 5.0).coerceIn(1.5, 24.0)
        val atlas = TileAtlas.peek()
        val terr = weighted(o, "terrains", "tile", "terrain").mapNotNull { (n, w) -> terrain(n)?.let { it to w } }
            .ifEmpty { listOfNotNull(terrain(o.str("tile", "terrain"))?.let { it to 1.0 }) }
        val out = mutableListOf<MapPaintOp?>()
        val terrainAt = HashMap<Pair<Int, Int>, String>()
        if (terr.isNotEmpty()) {
            val sorted = b.cells().sortedBy { (x, y) ->
                noise(x / scale, y / scale, seed) + 0.5 * noise(x * 2 / scale, y * 2 / scale, seed + 7)
            }
            val total = terr.sumOf { it.second }
            var start = 0
            var acc = 0.0
            terr.forEachIndexed { i, (name, w) ->
                acc += w
                val end = if (i == terr.lastIndex) sorted.size else (sorted.size * acc / total).toInt().coerceIn(start, sorted.size)
                val part = sorted.subList(start, end)
                part.forEach { terrainAt[it] = name }
                out += tiles(name, part.toList())
                start = end
            }
        }
        val props = weighted(o, "props", "obj", "object", "prop").mapNotNull { (n, w) -> obj(n)?.let { it to w } }
        if (props.isNotEmpty()) {
            val density = o.optDouble("density", 0.08).coerceIn(0.0, 0.6)
            val rng = Random(seed + 99)
            val total = props.sumOf { it.second }
            val used = HashSet<Pair<Int, Int>>()
            val placed = LinkedHashMap<String, MutableList<Pair<Int, Int>>>()
            b.cells().forEach { (x, y) ->
                if (rng.nextDouble() >= density) return@forEach
                var r = rng.nextDouble() * total
                val name = props.firstOrNull { r -= it.second; r <= 0.0 }?.first ?: props.last().first
                val ob = atlas?.objects?.get(name)
                val foot = Box(x, y, x + (ob?.w ?: 1) - 1, y + (ob?.h ?: 1) - 1)
                if (foot.x2 > b.x2 || foot.y2 > b.y2) return@forEach
                val ok = foot.cells().all { c ->
                    val t = terrainAt[c]
                    c !in used && (t != null || c !in blocked) &&
                        ((t ?: ground[c])?.let { atlas?.terrains?.get(it)?.cat } ?: "") !in NO_PROP_CATS
                }
                if (!ok) return@forEach
                used += foot.cells()
                placed.getOrPut(name) { mutableListOf() } += x to y
            }
            placed.forEach { (name, pts) -> out += objs(name, pts) }
        }
        return out.filterNotNull()
    }

    /** Walled settlement: optional ground, rampart ring with a south gate, main + cross road, castle, N buildings. */
    private fun kingdom(o: JSONObject): List<MapPaintOp> {
        val b = box(o)
        val atlas = TileAtlas.peek()
        var k = clean(o.str("kingdom", "style", "name")) ?: "human"
        if (atlas != null && "${k}_house_s" !in atlas.objects) {
            k = atlas.objects.keys.firstOrNull { it.endsWith("_house_s") && it.contains(k) }?.removeSuffix("_house_s") ?: "human"
        }
        val rng = Random(o.optLong("seed", 42L))
        val out = mutableListOf<MapPaintOp?>()
        val reserved = HashSet<Pair<Int, Int>>()
        val gx = (b.x1 + b.x2) / 2
        val midY = (b.y1 + b.y2) / 2
        val walled = o.optBoolean("walls", true) && b.w >= 8 && b.h >= 8
        o.str("ground", "tile", "terrain")?.let { out += tiles(terrain(it), b.cells()) }
        if (walled) {
            val ring = b.border().filter { (x, y) -> !(y == b.y2 && abs(x - gx) <= 1) }
            out += lines(line(o.str("wall") ?: "castle_rampart"), ring)
            reserved += b.border()
        }
        val road = line(o.str("road") ?: "road")
        val hasCastle = o.optBoolean("castle", true) && b.w >= 10 && b.h >= 12
        val roadTop = if (hasCastle) b.y1 + 5 else b.y1 + 1
        val roadCells = ((roadTop..b.y2).map { gx to it } + ((b.x1 + 1)..(b.x2 - 1)).map { it to midY }).distinct()
        out += lines(road, roadCells)
        reserved += roadCells
        if (hasCastle) {
            val c = Box(gx - 2, b.y1 + 1, gx + 1, b.y1 + 4)
            out += objs(obj("${k}_castle"), listOf(c.x1 to c.y1))
            reserved += Box(c.x1 - 1, c.y1, c.x2 + 1, c.y2 + 1).cells()
        }
        if (walled) {
            out += objs(obj("${k}_gate"), listOf(gx - 1 to b.y2 - 2))
            reserved += Box(gx - 2, b.y2 - 3, gx + 2, b.y2).cells()
        }
        val want = o.optInt("count", o.optInt("buildings", 6)).coerceIn(0, 64)
        val inner = Box(b.x1 + 1, b.y1 + 1, b.x2 - 1, b.y2 - 1)
        val spots = inner.cells().shuffled(rng)
        val roadSet = roadCells.toHashSet()
        val houses = HashSet<Pair<Int, Int>>()
        val spurs = LinkedHashSet<Pair<Int, Int>>()
        var placed = 0
        var tries = 0
        while (placed < want && tries < want * 3) {
            val (part, size) = KINGDOM_PARTS[tries % KINGDOM_PARTS.size]
            tries++
            val name = obj("${k}_$part") ?: continue
            val ob = atlas?.objects?.get(name)
            val w = ob?.w ?: size.first
            val h = ob?.h ?: size.second
            val spot = spots.firstOrNull { (x, y) ->
                x + w - 1 <= inner.x2 && y + h <= inner.y2 + 1 &&
                    Box(x - 1, y - 1, x + w, y + h).cells().none { it in reserved && !(it.second == y + h && it in roadSet) }
            } ?: continue
            out += objs(name, listOf(spot))
            val foot = Box(spot.first, spot.second, spot.first + w - 1, spot.second + h - 1).cells()
            reserved += foot
            houses += foot
            placed++
            // Door is bottom-centre: walk from the cell below it to the nearest road without crossing a building.
            val door = (spot.first + (w - 1) / 2) to (spot.second + h)
            if (door.second <= inner.y2 && door !in roadSet) {
                val target = roadSet.minByOrNull { abs(it.first - door.first) + abs(it.second - door.second) }
                val spur = target?.let { t ->
                    listOf(
                        orthoPath(door.first, door.second, door.first, t.second) + orthoPath(door.first, t.second, t.first, t.second),
                        orthoPath(door.first, door.second, t.first, door.second) + orthoPath(t.first, door.second, t.first, t.second),
                    ).firstOrNull { path -> path.none { it in houses || (it in reserved && it !in roadSet) } }
                }
                if (spur != null) {
                    spurs += spur
                    roadSet += spur
                    reserved += spur
                }
            }
        }
        out += lines(road, spurs.toList())
        return out.filterNotNull()
    }
}

/** The tileset catalogue as prompt text: names grouped by category, never indices. */
object MapTileCatalog {
    private val KINGDOM_PARTS = "house_s 2x2, house_m 3x3, house_l 4x3, shop 3x3, inn 4x3, temple 3x3, tower 2x4, gate 3x3, " +
        "castle 4x4, banner 1x2, stall 2x2, lamp 1x2, icon_village, icon_town, icon_city, icon_castle, icon_ruin"

    private fun sized(o: TileAtlas.Obj): String = if (o.w == 1 && o.h == 1) o.name else "${o.name} ${o.w}x${o.h}"

    /** Optional categories -> words in the request that pull them in. Terrains, lines, nature, village are always sent. */
    private val CAT_WORDS = mapOf(
        "furniture" to "interior|inside|indoor|room|house|home|inn|tavern|shop|store|bed|table|kitchen|library|hall|throne|furnitur|cabin|hut",
        "door" to "door|stair|ladder|window|interior|inside|indoor|room|house|dungeon|castle|cave|crypt|tower|gate|arch",
        "dungeon" to "dungeon|cave|cavern|crypt|tomb|trap|prison|jail|lair|mine|ruin|catacomb|sewer|vault|treasure|maze|labyrinth",
        "monument" to "grave|cemeter|statue|temple|ruin|shrine|fountain|plaza|town|city|kingdom|castle|pillar|monument|altar|church|park",
        "people" to "npc|people|person|villager|guard|character|party|hero|merchant|king|queen|soldier|army|populat|token|citizen|bandit|crowd",
        "creature" to "monster|creature|animal|beast|enemy|enemies|encounter|wolf|wolves|token|populat|wildlife|slime|spider|bird|fish|undead|battle|fight",
        "creature_big" to "dragon|boss|giant|golem|troll|treant|kraken|hydra|titan|colossus|huge|yeti|cyclops|wyvern",
    )
    private const val DIM_WORDS = "dimension|plane|realm|portal|otherworld"
    private val DIM_ALIAS = mapOf(
        "fire" to "fire|lava|volcan|magma|inferno", "water" to "underwater|ocean|sea floor|coral|abyss", "air" to "sky|cloud|air",
        "earth" to "earth|crystal cav|geode", "shadow" to "shadow|dark", "feywild" to "fey|fairy|faerie", "dream" to "dream|nightmare",
        "spirit" to "spirit|ghost|afterlife", "mechanus" to "mechan|clockwork|gear|machine|factory", "underdark" to "underdark|underground|deep",
        "abyssal" to "abyss|deep sea|trench", "astral" to "astral|star|space|cosmic", "void" to "void|nothing", "mirror" to "mirror|glass",
    )

    private fun hit(words: String, req: String) = Regex("\\b(?:$words)").containsMatchIn(req)

    /** Categories worth sending for [request]; null = send everything (no request, or nothing recognised in it). */
    fun wantedCats(request: String?): Set<String>? {
        val req = request?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
        val out = mutableSetOf<String>()
        CAT_WORDS.forEach { (cat, words) -> if (hit(words, req)) out += cat }
        val allDims = hit(DIM_WORDS, req)
        TileAtlas.peek()?.objectCats?.keys?.filter { it.startsWith("dim_") }?.forEach { cat ->
            val d = cat.removePrefix("dim_")
            if (allDims || hit(DIM_ALIAS[d] ?: d, req)) out += cat
        }
        // Nothing recognised (other language, very short ask): only then is the full catalogue worth its tokens.
        if (out.isEmpty() && !Regex("\\b(?:forest|river|lake|village|farm|island|desert|snow|swamp|mountain|road|beach|field|camp|wood|jungle|coast|meadow|hill)").containsMatchIn(req)) return null
        return out
    }

    fun promptGuide(request: String? = null): String {
        val want = wantedCats(request)
        val skipped = mutableListOf<String>()
        fun keep(cat: String) = want == null || cat in want || cat == "nature" || cat == "village" || cat.startsWith("kingdom_")
        val a = TileAtlas.peek() ?: return "TILESET CATALOGUE: not loaded - use colorHex + emoji cells instead of tile names."
        return buildString {
            appendLine("TILESET CATALOGUE (use these exact names; never invent names, never use numbers):")
            appendLine("TERRAINS (\"tile\", fills the whole cell, edges blend automatically):")
            a.terrainCats.forEach { (cat, items) -> appendLine("  $cat: " + items.joinToString(", ") { it.name }) }
            appendLine("LINES (\"line\", auto-connecting, drawn over the terrain): " + a.lines.keys.joinToString(", "))
            appendLine("OBJECTS (\"obj\", x,y = TOP-LEFT cell, WxH in cells when bigger than 1x1):")
            val kingdoms = mutableListOf<String>()
            a.objectCats.forEach { (cat, items) ->
                if (cat.startsWith("kingdom_")) kingdoms += cat.removePrefix("kingdom_")
                else if (!keep(cat)) skipped += cat
                else appendLine("  $cat: " + items.joinToString(", ") { sized(it) })
            }
            if (kingdoms.isNotEmpty()) {
                appendLine("  kingdom buildings = <kingdom>_<part>, e.g. human_inn. Kingdoms: " + kingdoms.joinToString(", "))
                appendLine("    parts: $KINGDOM_PARTS (door is bottom-centre)")
            }
            appendLine("SPRITES (token \"sprite\"; 4 facings handled by the app):")
            a.spriteCats.forEach { (cat, items) ->
                if (keep(cat)) appendLine("  $cat: " + items.joinToString(", ") { it.name }) else skipped += cat
            }
            if (skipped.isNotEmpty()) {
                appendLine("NOT LISTED (not needed for this request): " + skipped.joinToString(", ") +
                    ". If you still need one, use a plain guess such as bed_single_red, wolf, villager - close names are snapped.")
            }
        }.trim()
    }
}
