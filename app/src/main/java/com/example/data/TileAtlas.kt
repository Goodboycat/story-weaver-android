package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * The procedural 16px tileset (assets/tileset/master.png + atlas.json), loaded once and cached.
 * Never hard-code tile indices: always look names up here. Tile index i -> x = i % cols, y = i / cols.
 */
class TileAtlas(
    val bitmap: Bitmap,
    val tile: Int,
    val cols: Int,
    val terrains: Map<String, Terrain>,
    val lines: Map<String, Line>,
    val objects: Map<String, Obj>,
    val sprites: Map<String, Sprite>,
) {
    /** 8 tiles: isolated, horizontal, vertical, cross + [n] full variants. [p] = priority. */
    data class Terrain(val name: String, val i: Int, val n: Int, val p: Int, val cat: String, val colorHex: String)
    /** 16 tiles indexed by NESW bitmask (N=1, E=2, S=4, W=8). */
    data class Line(val name: String, val i: Int, val kind: String)
    data class Obj(val name: String, val i: Int, val w: Int, val h: Int, val cat: String, val solid: Boolean)
    /** 4 directions side by side (down, left, right, up); direction d starts at tile i + d * w. */
    data class Sprite(val name: String, val i: Int, val w: Int, val h: Int, val cat: String)

    val image: ImageBitmap by lazy { bitmap.asImageBitmap() }

    val terrainCats: Map<String, List<Terrain>> by lazy { terrains.values.groupBy { it.cat } }
    val lineCats: Map<String, List<Line>> by lazy { lines.values.groupBy { it.kind } }
    val objectCats: Map<String, List<Obj>> by lazy { objects.values.groupBy { it.cat } }
    val spriteCats: Map<String, List<Sprite>> by lazy { sprites.values.groupBy { it.cat } }

    companion object {
        const val DIR = "tileset"

        @Volatile
        private var cached: TileAtlas? = null

        /** Already-loaded atlas or null (never blocks). */
        fun peek(): TileAtlas? = cached

        suspend fun load(context: Context): TileAtlas? = cached ?: withContext(Dispatchers.IO) { loadBlocking(context) }

        @Synchronized
        fun loadBlocking(context: Context): TileAtlas? {
            cached?.let { return it }
            return try {
                val assets = context.applicationContext.assets
                val json = JSONObject(assets.open("$DIR/atlas.json").bufferedReader().use { it.readText() })
                val opts = BitmapFactory.Options().apply {
                    inScaled = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bmp = assets.open("$DIR/master.png").use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

                fun <T> section(key: String, make: (String, JSONObject) -> T): Map<String, T> {
                    val out = LinkedHashMap<String, T>()
                    val o = json.optJSONObject(key) ?: return out
                    val it = o.keys()
                    while (it.hasNext()) {
                        val name = it.next()
                        val v = o.optJSONObject(name) ?: continue
                        out[name] = make(name, v)
                    }
                    return out
                }

                TileAtlas(
                    bitmap = bmp,
                    tile = json.optInt("tile", 16),
                    cols = json.optInt("cols", 64),
                    terrains = section("terrains") { n, v ->
                        Terrain(n, v.optInt("i"), v.optInt("n", 4).coerceAtLeast(1), v.optInt("p"), v.optString("cat", "misc"), v.optString("c", "#808080"))
                    },
                    lines = section("lines") { n, v -> Line(n, v.optInt("i"), v.optString("kind", "line")) },
                    objects = section("objects") { n, v ->
                        Obj(n, v.optInt("i"), v.optInt("w", 1), v.optInt("h", 1), v.optString("cat", "misc"), v.optInt("solid", 0) != 0)
                    },
                    sprites = section("sprites") { n, v ->
                        Sprite(n, v.optInt("i"), v.optInt("w", 1), v.optInt("h", 1), v.optString("cat", "misc"))
                    },
                ).also { cached = it }
            } catch (_: Exception) {
                null
            }
        }
    }
}
