package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class MapColorSwatch(
    val hex: String,
    val name: String,
    val category: String,
)

object MapColorPalette {
    val swatches: List<MapColorSwatch> = listOf(
        // Stone / Dungeon / Slate
        MapColorSwatch("#0F172A", "Midnight", "Stone"),
        MapColorSwatch("#1E293B", "Obsidian", "Stone"),
        MapColorSwatch("#334155", "Dark Slate", "Stone"),
        MapColorSwatch("#475569", "Cobble Gray", "Stone"),
        MapColorSwatch("#64748B", "Stone Wall", "Stone"),
        MapColorSwatch("#94A3B8", "Ash Stone", "Stone"),
        MapColorSwatch("#CBD5E1", "Marble", "Stone"),
        MapColorSwatch("#F8FAFC", "Snow", "Stone"),

        // Earth / Wood / Desert
        MapColorSwatch("#3F2E21", "Dark Oak", "Earth"),
        MapColorSwatch("#5C4033", "Tavern Floor", "Earth"),
        MapColorSwatch("#78350F", "Timber", "Earth"),
        MapColorSwatch("#9A3412", "Terracotta", "Earth"),
        MapColorSwatch("#B45309", "Copper Dirt", "Earth"),
        MapColorSwatch("#D97706", "Sandstone", "Earth"),
        MapColorSwatch("#EAB308", "Gold", "Earth"),
        MapColorSwatch("#FDE68A", "Desert Sand", "Earth"),

        // Nature / Flora
        MapColorSwatch("#064E3B", "Deep Pines", "Nature"),
        MapColorSwatch("#14532D", "Forest", "Nature"),
        MapColorSwatch("#15803D", "Grassland", "Nature"),
        MapColorSwatch("#22C55E", "Meadow", "Nature"),
        MapColorSwatch("#4D7C0F", "Moss", "Nature"),
        MapColorSwatch("#65A30D", "Swamp", "Nature"),
        MapColorSwatch("#84CC16", "Acid Slime", "Nature"),

        // Water / Ice / Sky
        MapColorSwatch("#0C4A6E", "Abyss Water", "Water"),
        MapColorSwatch("#1E3A8A", "Deep Ocean", "Water"),
        MapColorSwatch("#1D4ED8", "River Blue", "Water"),
        MapColorSwatch("#0284C7", "Shallow Water", "Water"),
        MapColorSwatch("#0F766E", "Lagoon", "Water"),
        MapColorSwatch("#06B6D4", "Arcane Water", "Water"),
        MapColorSwatch("#7DD3FC", "Glacier Ice", "Water"),

        // Fire / Lava / Blood
        MapColorSwatch("#7F1D1D", "Blood", "Fire"),
        MapColorSwatch("#991B1B", "Brick Red", "Fire"),
        MapColorSwatch("#DC2626", "Lava Red", "Fire"),
        MapColorSwatch("#EA580C", "Magma", "Fire"),
        MapColorSwatch("#FB923C", "Ember", "Fire"),

        // Arcane / Magic / Void
        MapColorSwatch("#3B0764", "Shadow Void", "Arcane"),
        MapColorSwatch("#4C1D95", "Royal Arcane", "Arcane"),
        MapColorSwatch("#7C3AED", "Mystic Violet", "Arcane"),
        MapColorSwatch("#A855F7", "Portal Purple", "Arcane"),
        MapColorSwatch("#DB2777", "Crystal Rose", "Arcane"),
    )

    val hexList: List<String> = swatches.map { it.hex }

    private val namedColorLookup: Map<String, String> = buildMap {
        swatches.forEach { swatch ->
            put(swatch.name.lowercase(), swatch.hex)
            put(swatch.name.lowercase().replace(" ", "_"), swatch.hex)
        }
        put("black", "#0F172A")
        put("white", "#F8FAFC")
        put("gray", "#64748B")
        put("grey", "#64748B")
        put("stone", "#64748B")
        put("wall", "#334155")
        put("slate", "#334155")
        put("floor", "#5C4033")
        put("wood", "#78350F")
        put("brown", "#78350F")
        put("dirt", "#B45309")
        put("sand", "#FDE68A")
        put("yellow", "#EAB308")
        put("green", "#15803D")
        put("grass", "#15803D")
        put("tree", "#14532D")
        put("blue", "#1D4ED8")
        put("water", "#0284C7")
        put("ocean", "#1E3A8A")
        put("ice", "#7DD3FC")
        put("cyan", "#06B6D4")
        put("red", "#DC2626")
        put("lava", "#EA580C")
        put("fire", "#DC2626")
        put("orange", "#EA580C")
        put("purple", "#7C3AED")
        put("pink", "#DB2777")
        put("magic", "#A855F7")
    }

    fun normalizeColorHex(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val lower = trimmed.lowercase()
        if (lower == "null" || lower == "none" || lower == "transparent" || lower == "clear") {
            return null
        }
        namedColorLookup[lower]?.let { return it }

        val withoutHash = trimmed.removePrefix("#")
        if (withoutHash.length == 3 && withoutHash.all { it.isHexDigit() }) {
            val r = withoutHash[0]
            val g = withoutHash[1]
            val b = withoutHash[2]
            return "#$r$r$g$g$b$b".uppercase()
        }
        if (withoutHash.length == 6 && withoutHash.all { it.isHexDigit() }) {
            return "#${withoutHash.uppercase()}"
        }
        if (withoutHash.length == 8 && withoutHash.all { it.isHexDigit() }) {
            return "#${withoutHash.uppercase()}"
        }
        return null
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    fun palettePromptGuide(): String = buildString {
        appendLine("COLOR SELECTION RULES:")
        appendLine("- You can use ANY valid 6-digit #RRGGBB hex color you want for background block colors (or null for transparent).")
        appendLine("- Suggested tabletop palette swatches by theme:")
        swatches.groupBy { it.category }.forEach { (cat, items) ->
            appendLine("  $cat: " + items.joinToString(", ") { "${it.name}=${it.hex}" })
        }
    }.trim()
}

sealed interface MapPaintOp {
    data class Rect(
        val x1: Int,
        val y1: Int,
        val x2: Int,
        val y2: Int,
        val colorHex: String? = null,
        val emoji: String? = null,
        val hollow: Boolean = false,
    ) : MapPaintOp

    data class RectBorder(
        val x1: Int,
        val y1: Int,
        val x2: Int,
        val y2: Int,
        val colorHex: String? = null,
        val emoji: String? = null,
        val thickness: Int = 1,
    ) : MapPaintOp

    data class Line(
        val x1: Int,
        val y1: Int,
        val x2: Int,
        val y2: Int,
        val colorHex: String? = null,
        val emoji: String? = null,
        val brushSize: Int = 1,
    ) : MapPaintOp

    data class Paint(
        val points: List<Pair<Int, Int>>,
        val colorHex: String? = null,
        val emoji: String? = null,
        val brushSize: Int = 1,
    ) : MapPaintOp

    data class FloodFill(
        val x: Int,
        val y: Int,
        val colorHex: String? = null,
        val emoji: String? = null,
    ) : MapPaintOp

    data class Erase(
        val x1: Int? = null,
        val y1: Int? = null,
        val x2: Int? = null,
        val y2: Int? = null,
        val points: List<Pair<Int, Int>> = emptyList(),
        val brushSize: Int = 1,
    ) : MapPaintOp

    data class Circle(
        val cx: Int,
        val cy: Int,
        val radius: Int,
        val colorHex: String? = null,
        val emoji: String? = null,
        val filled: Boolean = true,
    ) : MapPaintOp

    data class Checker(
        val x1: Int,
        val y1: Int,
        val x2: Int,
        val y2: Int,
        val colorHex: String? = null,
        val emoji: String? = null,
        val colorHex2: String? = null,
        val emoji2: String? = null,
    ) : MapPaintOp

    data class Scatter(
        val x1: Int,
        val y1: Int,
        val x2: Int,
        val y2: Int,
        val count: Int,
        val colorHex: String? = null,
        val emoji: String? = null,
        val onlyEmpty: Boolean = false,
        val seed: Long = 42L,
    ) : MapPaintOp
}

data class AiMapTokenSpec(
    val label: String,
    val emoji: String,
    val kind: String = "monster",
    val x: Int,
    val y: Int,
)

data class MapPaintScript(
    val width: Int? = null,
    val height: Int? = null,
    val clearFirst: Boolean = false,
    val clearTokens: Boolean = false,
    val summary: String = "AI painted map",
    val operations: List<MapPaintOp> = emptyList(),
    val directCells: Map<String, GridCell> = emptyMap(),
    val tokens: List<AiMapTokenSpec> = emptyList(),
)

data class MapPaintOutcome(
    val width: Int,
    val height: Int,
    val grid: Map<String, GridCell>,
    val tokens: List<MapToken>,
    val summary: String,
    val operationsApplied: Int,
    val scriptJson: String,
)

object MapPaintEngine {

    fun executeScript(
        script: MapPaintScript,
        currentWidth: Int,
        currentHeight: Int,
        currentGrid: Map<String, GridCell>,
        currentTokens: List<MapToken>,
        ownerId: String,
        scriptJson: String = "",
    ): MapPaintOutcome {
        val targetWidth = (script.width ?: currentWidth).coerceIn(4, 30)
        val targetHeight = (script.height ?: currentHeight).coerceIn(4, 30)

        val nextGrid = if (script.clearFirst) {
            mutableMapOf()
        } else {
            currentGrid.filterKeys { key ->
                val parts = key.split(",")
                val gx = parts.getOrNull(0)?.toIntOrNull() ?: -1
                val gy = parts.getOrNull(1)?.toIntOrNull() ?: -1
                gx in 0 until targetWidth && gy in 0 until targetHeight
            }.toMutableMap()
        }

        var opsApplied = 0

        fun cellKey(x: Int, y: Int): String = "$x,$y"

        fun paintWithBrush(cx: Int, cy: Int, brushSize: Int, cell: GridCell) {
            if (cell.colorHex == null && cell.emoji == null && cell.imageBase64 == null) return
            val sz = brushSize.coerceIn(1, 3)
            val half = sz / 2
            val sx = cx - half
            val sy = cy - half
            for (px in sx..(sx + sz - 1)) {
                for (py in sy..(sy + sz - 1)) {
                    if (px in 0 until targetWidth && py in 0 until targetHeight) {
                        nextGrid[cellKey(px, py)] = cell
                    }
                }
            }
        }

        fun eraseWithBrush(cx: Int, cy: Int, brushSize: Int) {
            val sz = brushSize.coerceIn(1, 3)
            val half = sz / 2
            val sx = cx - half
            val sy = cy - half
            for (px in sx..(sx + sz - 1)) {
                for (py in sy..(sy + sz - 1)) {
                    if (px in 0 until targetWidth && py in 0 until targetHeight) {
                        nextGrid.remove(cellKey(px, py))
                    }
                }
            }
        }

        script.operations.forEach { op ->
            when (op) {
                is MapPaintOp.Rect -> {
                    val cell = GridCell(
                        colorHex = MapColorPalette.normalizeColorHex(op.colorHex),
                        emoji = op.emoji?.takeIf { it.isNotBlank() },
                    )
                    if (cell.colorHex != null || cell.emoji != null) {
                        val minX = min(op.x1, op.x2).coerceIn(0, targetWidth - 1)
                        val maxX = max(op.x1, op.x2).coerceIn(0, targetWidth - 1)
                        val minY = min(op.y1, op.y2).coerceIn(0, targetHeight - 1)
                        val maxY = max(op.y1, op.y2).coerceIn(0, targetHeight - 1)
                        for (x in minX..maxX) {
                            for (y in minY..maxY) {
                                if (!op.hollow || x == minX || x == maxX || y == minY || y == maxY) {
                                    nextGrid[cellKey(x, y)] = cell
                                }
                            }
                        }
                        opsApplied++
                    }
                }

                is MapPaintOp.RectBorder -> {
                    val cell = GridCell(
                        colorHex = MapColorPalette.normalizeColorHex(op.colorHex),
                        emoji = op.emoji?.takeIf { it.isNotBlank() },
                    )
                    if (cell.colorHex != null || cell.emoji != null) {
                        val minX = min(op.x1, op.x2).coerceIn(0, targetWidth - 1)
                        val maxX = max(op.x1, op.x2).coerceIn(0, targetWidth - 1)
                        val minY = min(op.y1, op.y2).coerceIn(0, targetHeight - 1)
                        val maxY = max(op.y1, op.y2).coerceIn(0, targetHeight - 1)
                        val thick = op.thickness.coerceIn(1, 4)
                        for (x in minX..maxX) {
                            for (y in minY..maxY) {
                                val onBorder = (x - minX < thick) || (maxX - x < thick) ||
                                    (y - minY < thick) || (maxY - y < thick)
                                if (onBorder) {
                                    nextGrid[cellKey(x, y)] = cell
                                }
                            }
                        }
                        opsApplied++
                    }
                }

                is MapPaintOp.Line -> {
                    val cell = GridCell(
                        colorHex = MapColorPalette.normalizeColorHex(op.colorHex),
                        emoji = op.emoji?.takeIf { it.isNotBlank() },
                    )
                    if (cell.colorHex != null || cell.emoji != null) {
                        bresenhamLine(op.x1, op.y1, op.x2, op.y2).forEach { (lx, ly) ->
                            paintWithBrush(lx, ly, op.brushSize, cell)
                        }
                        opsApplied++
                    }
                }

                is MapPaintOp.Paint -> {
                    val cell = GridCell(
                        colorHex = MapColorPalette.normalizeColorHex(op.colorHex),
                        emoji = op.emoji?.takeIf { it.isNotBlank() },
                    )
                    if (cell.colorHex != null || cell.emoji != null) {
                        op.points.forEach { (px, py) ->
                            paintWithBrush(px, py, op.brushSize, cell)
                        }
                        if (op.points.isNotEmpty()) opsApplied++
                    }
                }

                is MapPaintOp.FloodFill -> {
                    val sx = op.x.coerceIn(0, targetWidth - 1)
                    val sy = op.y.coerceIn(0, targetHeight - 1)
                    val replacement = GridCell(
                        colorHex = MapColorPalette.normalizeColorHex(op.colorHex),
                        emoji = op.emoji?.takeIf { it.isNotBlank() },
                    )
                    val startKey = cellKey(sx, sy)
                    val target = nextGrid[startKey]
                    if (target != replacement && (replacement.colorHex != null || replacement.emoji != null)) {
                        val queue = ArrayDeque<Pair<Int, Int>>()
                        val visited = HashSet<String>()
                        queue.add(sx to sy)
                        while (queue.isNotEmpty()) {
                            val (x, y) = queue.removeFirst()
                            val k = cellKey(x, y)
                            if (x !in 0 until targetWidth || y !in 0 until targetHeight || k in visited) continue
                            if (nextGrid[k] != target) continue
                            visited.add(k)
                            nextGrid[k] = replacement
                            queue.add(x + 1 to y)
                            queue.add(x - 1 to y)
                            queue.add(x to y + 1)
                            queue.add(x to y - 1)
                        }
                        opsApplied++
                    }
                }

                is MapPaintOp.Erase -> {
                    var touched = false
                    if (op.x1 != null && op.y1 != null && op.x2 != null && op.y2 != null) {
                        val minX = min(op.x1, op.x2).coerceIn(0, targetWidth - 1)
                        val maxX = max(op.x1, op.x2).coerceIn(0, targetWidth - 1)
                        val minY = min(op.y1, op.y2).coerceIn(0, targetHeight - 1)
                        val maxY = max(op.y1, op.y2).coerceIn(0, targetHeight - 1)
                        for (x in minX..maxX) {
                            for (y in minY..maxY) {
                                nextGrid.remove(cellKey(x, y))
                            }
                        }
                        touched = true
                    }
                    op.points.forEach { (px, py) ->
                        eraseWithBrush(px, py, op.brushSize)
                        touched = true
                    }
                    if (touched) opsApplied++
                }

                is MapPaintOp.Circle -> {
                    val cell = GridCell(
                        colorHex = MapColorPalette.normalizeColorHex(op.colorHex),
                        emoji = op.emoji?.takeIf { it.isNotBlank() },
                    )
                    if (cell.colorHex != null || cell.emoji != null) {
                        val r = op.radius.coerceIn(1, max(targetWidth, targetHeight))
                        val rSq = r * r
                        val innerSq = max(0, (r - 1) * (r - 1))
                        for (x in (op.cx - r)..(op.cx + r)) {
                            for (y in (op.cy - r)..(op.cy + r)) {
                                if (x !in 0 until targetWidth || y !in 0 until targetHeight) continue
                                val dx = x - op.cx
                                val dy = y - op.cy
                                val distSq = dx * dx + dy * dy
                                val inside = if (op.filled) {
                                    distSq <= rSq + r / 2
                                } else {
                                    distSq in innerSq..(rSq + r / 2)
                                }
                                if (inside) {
                                    nextGrid[cellKey(x, y)] = cell
                                }
                            }
                        }
                        opsApplied++
                    }
                }

                is MapPaintOp.Checker -> {
                    val cellA = GridCell(
                        colorHex = MapColorPalette.normalizeColorHex(op.colorHex),
                        emoji = op.emoji?.takeIf { it.isNotBlank() },
                    )
                    val cellB = GridCell(
                        colorHex = MapColorPalette.normalizeColorHex(op.colorHex2),
                        emoji = op.emoji2?.takeIf { it.isNotBlank() },
                    )
                    if (cellA.colorHex != null || cellA.emoji != null || cellB.colorHex != null || cellB.emoji != null) {
                        val minX = min(op.x1, op.x2).coerceIn(0, targetWidth - 1)
                        val maxX = max(op.x1, op.x2).coerceIn(0, targetWidth - 1)
                        val minY = min(op.y1, op.y2).coerceIn(0, targetHeight - 1)
                        val maxY = max(op.y1, op.y2).coerceIn(0, targetHeight - 1)
                        for (x in minX..maxX) {
                            for (y in minY..maxY) {
                                val chosen = if ((x + y) % 2 == 0) cellA else cellB
                                if (chosen.colorHex != null || chosen.emoji != null) {
                                    nextGrid[cellKey(x, y)] = chosen
                                }
                            }
                        }
                        opsApplied++
                    }
                }

                is MapPaintOp.Scatter -> {
                    val cell = GridCell(
                        colorHex = MapColorPalette.normalizeColorHex(op.colorHex),
                        emoji = op.emoji?.takeIf { it.isNotBlank() },
                    )
                    if (cell.colorHex != null || cell.emoji != null) {
                        val minX = min(op.x1, op.x2).coerceIn(0, targetWidth - 1)
                        val maxX = max(op.x1, op.x2).coerceIn(0, targetWidth - 1)
                        val minY = min(op.y1, op.y2).coerceIn(0, targetHeight - 1)
                        val maxY = max(op.y1, op.y2).coerceIn(0, targetHeight - 1)
                        val candidates = buildList {
                            for (x in minX..maxX) {
                                for (y in minY..maxY) {
                                    val key = cellKey(x, y)
                                    if (!op.onlyEmpty || nextGrid[key] == null) {
                                        add(x to y)
                                    }
                                }
                            }
                        }
                        if (candidates.isNotEmpty()) {
                            val rng = kotlin.random.Random(op.seed)
                            val targetCount = op.count.coerceIn(1, candidates.size)
                            candidates.shuffled(rng).take(targetCount).forEach { (sx, sy) ->
                                nextGrid[cellKey(sx, sy)] = cell
                            }
                            opsApplied++
                        }
                    }
                }
            }
        }

        if (script.directCells.isNotEmpty()) {
            var directCount = 0
            script.directCells.forEach { (key, cell) ->
                val parts = key.split(",")
                val gx = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
                val gy = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach
                if (gx in 0 until targetWidth && gy in 0 until targetHeight) {
                    val normalized = cell.copy(
                        colorHex = MapColorPalette.normalizeColorHex(cell.colorHex),
                        emoji = cell.emoji?.takeIf { it.isNotBlank() },
                    )
                    if (normalized.colorHex != null || normalized.emoji != null) {
                        nextGrid["$gx,$gy"] = normalized
                        directCount++
                    }
                }
            }
            if (directCount > 0) opsApplied++
        }

        val baseTokens = if (script.clearTokens) emptyList() else currentTokens.map {
            it.copy(x = it.x.coerceIn(0, targetWidth - 1), y = it.y.coerceIn(0, targetHeight - 1))
        }
        val addedTokens = script.tokens.map { spec ->
            val cleanKind = when (spec.kind.lowercase()) {
                "player", "npc", "monster" -> spec.kind.lowercase()
                else -> "monster"
            }
            MapToken(
                id = "tk_${UUID.randomUUID().toString().take(8)}",
                label = spec.label.trim().ifBlank { "Marker" }.take(40),
                emoji = spec.emoji.trim().ifBlank { if (cleanKind == "monster") "👹" else "🧙" }.take(12),
                kind = cleanKind,
                ownerId = ownerId,
                x = spec.x.coerceIn(0, targetWidth - 1),
                y = spec.y.coerceIn(0, targetHeight - 1),
            )
        }
        if (addedTokens.isNotEmpty()) opsApplied++

        return MapPaintOutcome(
            width = targetWidth,
            height = targetHeight,
            grid = nextGrid,
            tokens = baseTokens + addedTokens,
            summary = script.summary.ifBlank { "Painted $opsApplied map operation(s)" },
            operationsApplied = opsApplied,
            scriptJson = scriptJson,
        )
    }

    fun bresenhamLine(x0: Int, y0: Int, x1: Int, y1: Int): List<Pair<Int, Int>> {
        val points = mutableListOf<Pair<Int, Int>>()
        var cx = x0
        var cy = y0
        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy
        while (true) {
            points += cx to cy
            if (cx == x1 && cy == y1) break
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                cx += sx
            }
            if (e2 < dx) {
                err += dx
                cy += sy
            }
        }
        return points
    }

    fun summarizeMapForPrompt(
        width: Int,
        height: Int,
        grid: Map<String, GridCell>,
        tokens: List<MapToken>,
    ): String = buildString {
        appendLine("Current map grid size: ${width}x${height} (x: 0..${width - 1}, y: 0..${height - 1}, origin 0,0 at top-left).")
        if (tokens.isNotEmpty()) {
            appendLine("Current tokens:")
            tokens.take(24).forEach { t ->
                appendLine("  - ${t.emoji} ${t.label} (${t.kind}) at (${t.x}, ${t.y})")
            }
        }
        if (grid.isEmpty()) {
            appendLine("Current painted tiles: none (empty grid).")
        } else {
            val grouped = grid.entries.groupBy { (_, cell) ->
                "${cell.colorHex ?: "none"}|${cell.emoji ?: ""}"
            }
            appendLine("Current painted tiles (${grid.size} occupied):")
            grouped.entries.take(18).forEach { (style, entries) ->
                val sampleCoords = entries.take(18).joinToString(", ") { "(${it.key})" }
                val suffix = if (entries.size > 18) " ... (+${entries.size - 18} more)" else ""
                appendLine("  - Style [$style] x${entries.size}: $sampleCoords$suffix")
            }
        }
    }.trim()
}

object MapPaintJsonParser {
    fun parseScript(rawJson: String): Result<MapPaintScript> = runCatching {
        val obj = JSONObject(rawJson)
        val width = if (obj.has("width") && !obj.isNull("width")) obj.optInt("width", -1).takeIf { it > 0 } else null
        val height = if (obj.has("height") && !obj.isNull("height")) obj.optInt("height", -1).takeIf { it > 0 } else null
        val clearFirst = obj.optBoolean("clearFirst", obj.optBoolean("clear", false))
        val clearTokens = obj.optBoolean("clearTokens", false)
        val summary = obj.optString("summary", "AI painted map").trim()

        val ops = mutableListOf<MapPaintOp>()
        val opsArr = obj.optJSONArray("operations")
            ?: obj.optJSONArray("ops")
            ?: obj.optJSONArray("actions")
        if (opsArr != null) {
            for (i in 0 until opsArr.length()) {
                val o = opsArr.optJSONObject(i) ?: continue
                parseSingleOp(o)?.let { ops += it }
            }
        }

        val directCells = mutableMapOf<String, GridCell>()
        val cellsObj = obj.optJSONObject("cells")
        if (cellsObj != null) {
            val keys = cellsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val co = cellsObj.optJSONObject(k) ?: continue
                val color = co.optString("colorHex").takeIf { it.isNotBlank() }
                    ?: co.optString("color").takeIf { it.isNotBlank() }
                val emoji = co.optString("emoji").takeIf { it.isNotBlank() }
                if (color != null || emoji != null) {
                    directCells[k] = GridCell(colorHex = color, emoji = emoji)
                }
            }
        }

        val tokens = mutableListOf<AiMapTokenSpec>()
        val tokensArr = obj.optJSONArray("tokens")
        if (tokensArr != null) {
            for (i in 0 until tokensArr.length()) {
                val to = tokensArr.optJSONObject(i) ?: continue
                val label = to.optString("label").trim()
                if (label.isNotBlank()) {
                    tokens += AiMapTokenSpec(
                        label = label,
                        emoji = to.optString("emoji", "👹"),
                        kind = to.optString("kind", "monster"),
                        x = to.optInt("x", 0),
                        y = to.optInt("y", 0),
                    )
                }
            }
        }

        require(ops.isNotEmpty() || directCells.isNotEmpty() || tokens.isNotEmpty() || clearFirst) {
            "AI response did not contain any valid map painting operations."
        }

        MapPaintScript(
            width = width,
            height = height,
            clearFirst = clearFirst,
            clearTokens = clearTokens,
            summary = summary,
            operations = ops,
            directCells = directCells,
            tokens = tokens,
        )
    }

    private fun parseSingleOp(o: JSONObject): MapPaintOp? {
        val tool = o.optString("tool").ifBlank { o.optString("type") }.trim().lowercase()
        val colorHex = o.optString("colorHex").takeIf { it.isNotBlank() }
            ?: o.optString("color").takeIf { it.isNotBlank() }
            ?: o.optString("bg").takeIf { it.isNotBlank() }
        val emoji = o.optString("emoji").takeIf { it.isNotBlank() }

        return when (tool) {
            "rect", "fill_rect", "rectangle", "box" -> MapPaintOp.Rect(
                x1 = o.optInt("x1", o.optInt("x", 0)),
                y1 = o.optInt("y1", o.optInt("y", 0)),
                x2 = o.optInt("x2", o.optInt("x1", 0)),
                y2 = o.optInt("y2", o.optInt("y1", 0)),
                colorHex = colorHex,
                emoji = emoji,
                hollow = o.optBoolean("hollow", false),
            )

            "rect_border", "border", "walls", "frame", "outline_rect" -> MapPaintOp.RectBorder(
                x1 = o.optInt("x1", 0),
                y1 = o.optInt("y1", 0),
                x2 = o.optInt("x2", 0),
                y2 = o.optInt("y2", 0),
                colorHex = colorHex,
                emoji = emoji,
                thickness = o.optInt("thickness", 1),
            )

            "line", "draw_line", "path", "corridor", "river", "road" -> MapPaintOp.Line(
                x1 = o.optInt("x1", 0),
                y1 = o.optInt("y1", 0),
                x2 = o.optInt("x2", 0),
                y2 = o.optInt("y2", 0),
                colorHex = colorHex,
                emoji = emoji,
                brushSize = o.optInt("brushSize", 1),
            )

            "paint", "brush", "cell", "cells", "points", "dot" -> {
                val points = parsePoints(o)
                if (points.isEmpty()) null
                else MapPaintOp.Paint(
                    points = points,
                    colorHex = colorHex,
                    emoji = emoji,
                    brushSize = o.optInt("brushSize", 1),
                )
            }

            "flood_fill", "fill", "bucket" -> MapPaintOp.FloodFill(
                x = o.optInt("x", o.optInt("x1", 0)),
                y = o.optInt("y", o.optInt("y1", 0)),
                colorHex = colorHex,
                emoji = emoji,
            )

            "erase", "clear_cell", "clear_rect", "eraser" -> {
                val hasRect = o.has("x1") && o.has("y1") && o.has("x2") && o.has("y2")
                val points = if (hasRect) emptyList() else parsePoints(o)
                MapPaintOp.Erase(
                    x1 = if (hasRect) o.optInt("x1") else null,
                    y1 = if (hasRect) o.optInt("y1") else null,
                    x2 = if (hasRect) o.optInt("x2") else null,
                    y2 = if (hasRect) o.optInt("y2") else null,
                    points = points,
                    brushSize = o.optInt("brushSize", 1),
                )
            }

            "circle", "disc", "ring" -> MapPaintOp.Circle(
                cx = o.optInt("cx", o.optInt("x", 0)),
                cy = o.optInt("cy", o.optInt("y", 0)),
                radius = o.optInt("radius", 2),
                colorHex = colorHex,
                emoji = emoji,
                filled = o.optBoolean("filled", tool != "ring"),
            )

            "checker", "checkerboard", "tile_pattern" -> {
                val colorHex2 = o.optString("colorHex2").takeIf { it.isNotBlank() }
                    ?: o.optString("color2").takeIf { it.isNotBlank() }
                val emoji2 = o.optString("emoji2").takeIf { it.isNotBlank() }
                MapPaintOp.Checker(
                    x1 = o.optInt("x1", 0),
                    y1 = o.optInt("y1", 0),
                    x2 = o.optInt("x2", 0),
                    y2 = o.optInt("y2", 0),
                    colorHex = colorHex,
                    emoji = emoji,
                    colorHex2 = colorHex2,
                    emoji2 = emoji2,
                )
            }

            "scatter", "sprinkle", "forest", "trees" -> MapPaintOp.Scatter(
                x1 = o.optInt("x1", 0),
                y1 = o.optInt("y1", 0),
                x2 = o.optInt("x2", 0),
                y2 = o.optInt("y2", 0),
                count = o.optInt("count", 6),
                colorHex = colorHex,
                emoji = emoji,
                onlyEmpty = o.optBoolean("onlyEmpty", false),
                seed = o.optLong("seed", 42L),
            )

            else -> null
        }
    }

    private fun parsePoints(o: JSONObject): List<Pair<Int, Int>> {
        val points = mutableListOf<Pair<Int, Int>>()
        val ptsArr: JSONArray? = o.optJSONArray("points")
        if (ptsArr != null) {
            for (i in 0 until ptsArr.length()) {
                val pairArr = ptsArr.optJSONArray(i)
                if (pairArr != null && pairArr.length() >= 2) {
                    points += pairArr.optInt(0) to pairArr.optInt(1)
                } else {
                    val ptObj = ptsArr.optJSONObject(i)
                    if (ptObj != null && ptObj.has("x") && ptObj.has("y")) {
                        points += ptObj.optInt("x") to ptObj.optInt("y")
                    }
                }
            }
        }
        if (points.isEmpty() && o.has("x") && o.has("y")) {
            points += o.optInt("x") to o.optInt("y")
        }
        return points
    }
}
