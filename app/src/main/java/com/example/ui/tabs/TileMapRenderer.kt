package com.example.ui.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.data.GridCell
import com.example.data.TileAtlas
import kotlin.math.roundToInt

/*
 * Kotlin port of tools/tileset/render.py (the spec). Quarter-tile autotiling:
 * every cell is cut into four 8x8 quadrants, each picks its source tile from 3 neighbours.
 * Draw order: terrain -> lines -> objects (sorted by bottom row). Sprites are drawn by the token layer.
 */

private fun DrawScope.blit(atlas: TileAtlas, idx: Int, sx: Int, sy: Int, sw: Int, sh: Int, x0: Float, y0: Float, x1: Float, y1: Float) {
    val l = x0.roundToInt(); val t = y0.roundToInt()
    val w = x1.roundToInt() - l; val h = y1.roundToInt() - t
    if (w <= 0 || h <= 0) return
    drawImage(
        image = atlas.image,
        srcOffset = IntOffset((idx % atlas.cols) * atlas.tile + sx, (idx / atlas.cols) * atlas.tile + sy),
        srcSize = IntSize(sw, sh),
        dstOffset = IntOffset(l, t),
        dstSize = IntSize(w, h),
        filterQuality = FilterQuality.None,
    )
}

/** One whole 16x16 tile into the cell rectangle at (cx, cy) in cell units. */
fun DrawScope.drawTile(atlas: TileAtlas, idx: Int, cx: Float, cy: Float, cellPx: Float) =
    blit(atlas, idx, 0, 0, atlas.tile, atlas.tile, cx * cellPx, cy * cellPx, (cx + 1) * cellPx, (cy + 1) * cellPx)

private fun tileHash(x: Int, y: Int): Int {
    val n = (x.toLong() * 73856093L) xor (y.toLong() * 19349663L)
    return ((n xor (n shr 7)) and 0x7FFFFFFFL).toInt()
}

// quadrant x, y (0/1), neighbour dx, dy
private val QUADS = arrayOf(intArrayOf(0, 0, -1, -1), intArrayOf(1, 0, 1, -1), intArrayOf(0, 1, -1, 1), intArrayOf(1, 1, 1, 1))

fun DrawScope.drawTileMap(atlas: TileAtlas, grid: Map<String, GridCell>, width: Int, height: Int, cellPx: Float) {
    if (width <= 0 || height <= 0) return
    val half = atlas.tile / 2
    // Resolve names once per frame into flat arrays.
    val terr = arrayOfNulls<TileAtlas.Terrain>(width * height)
    val line = arrayOfNulls<String>(width * height)
    val objs = ArrayList<Triple<TileAtlas.Obj, Int, Int>>()
    grid.forEach { (key, cell) ->
        if (cell.tile == null && cell.line == null && cell.obj == null) return@forEach
        val c = key.indexOf(',')
        if (c <= 0) return@forEach
        val x = key.substring(0, c).toIntOrNull() ?: return@forEach
        val y = key.substring(c + 1).toIntOrNull() ?: return@forEach
        if (x !in 0 until width || y !in 0 until height) return@forEach
        cell.tile?.let { terr[y * width + x] = atlas.terrains[it] }
        cell.line?.let { if (atlas.lines.containsKey(it)) line[y * width + x] = it }
        cell.obj?.let { n -> atlas.objects[n]?.let { objs.add(Triple(it, x, y)) } }
    }
    fun tn(x: Int, y: Int) = terr[y.coerceIn(0, height - 1) * width + x.coerceIn(0, width - 1)]

    for (y in 0 until height) {
        for (x in 0 until width) {
            val t = terr[y * width + x] ?: continue
            for (q in QUADS) {
                val nh = tn(x + q[2], y); val nv = tn(x, y + q[3]); val nd = tn(x + q[2], y + q[3])
                val ch = nh != null && nh.p >= t.p
                val cv = nv != null && nv.p >= t.p
                val cd = nd != null && nd.p >= t.p
                val sx = q[0] * half; val sy = q[1] * half
                val x0 = (x + q[0] * 0.5f) * cellPx; val y0 = (y + q[1] * 0.5f) * cellPx
                val x1 = (x + (q[0] + 1) * 0.5f) * cellPx; val y1 = (y + (q[1] + 1) * 0.5f) * cellPx
                val src: Int
                if (ch && cv && cd) {
                    src = t.i + 4 + tileHash(x, y) % t.n
                } else {
                    src = t.i + (if (ch && cv) 3 else if (ch) 1 else if (cv) 2 else 0)
                    // highest lower neighbour shows through the transparent rim
                    var under: TileAtlas.Terrain? = null
                    if (nh != null && !ch) under = nh
                    if (nv != null && !cv && (under == null || nv.p > under.p)) under = nv
                    if (nd != null && !cd && (under == null || nd.p > under.p)) under = nd
                    if (under != null) blit(atlas, under.i + 4, sx, sy, half, half, x0, y0, x1, y1)
                }
                blit(atlas, src, sx, sy, half, half, x0, y0, x1, y1)
            }
        }
    }

    for (y in 0 until height) {
        for (x in 0 until width) {
            val name = line[y * width + x] ?: continue
            fun same(nx: Int, ny: Int) = nx in 0 until width && ny in 0 until height && line[ny * width + nx] == name
            var bits = 0
            if (same(x, y - 1)) bits = bits or 1
            if (same(x + 1, y)) bits = bits or 2
            if (same(x, y + 1)) bits = bits or 4
            if (same(x - 1, y)) bits = bits or 8
            drawTile(atlas, atlas.lines.getValue(name).i + bits, x.toFloat(), y.toFloat(), cellPx)
        }
    }

    objs.sortBy { it.third + it.first.h }
    for ((o, x, y) in objs) {
        for (j in 0 until o.h) for (i in 0 until o.w) {
            drawTile(atlas, o.i + j * atlas.cols + i, (x + i).toFloat(), (y + j).toFloat(), cellPx)
        }
    }
}

/** Sprite facing [dir] (0 down, 1 left, 2 right, 3 up) with its top-left at cell (cx, cy). */
fun DrawScope.drawSprite(atlas: TileAtlas, s: TileAtlas.Sprite, dir: Int, cx: Float, cy: Float, cellPx: Float) {
    val d = dir.coerceIn(0, 3)
    for (j in 0 until s.h) for (k in 0 until s.w) {
        drawTile(atlas, s.i + d * s.w + j * atlas.cols + k, cx + k, cy + j, cellPx)
    }
}

/** Small preview of any atlas entry (terrain / line / object / sprite), fitted into [size]. */
@Composable
fun TilePreview(atlas: TileAtlas, kind: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val px = this.size.minDimension
        when (kind) {
            "tile" -> atlas.terrains[name]?.let { drawTile(atlas, it.i + 4, 0f, 0f, px) }
            "line" -> atlas.lines[name]?.let { drawTile(atlas, it.i + 10, 0f, 0f, px) } // E+W run
            "obj" -> atlas.objects[name]?.let { o ->
                val c = px / maxOf(o.w, o.h)
                val ox = (px - o.w * c) / 2f / c; val oy = (px - o.h * c) / 2f / c
                for (j in 0 until o.h) for (i in 0 until o.w) drawTile(atlas, o.i + j * atlas.cols + i, ox + i, oy + j, c)
            }
            "sprite" -> atlas.sprites[name]?.let { s ->
                val c = px / maxOf(s.w, s.h)
                drawSprite(atlas, s, 0, 0f, 0f, c)
            }
        }
    }
}
