package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class StoryInvite(
    @DocumentId val id: String = "",
    val storyId: String = "",
    val title: String = "",
    val systemName: String = "",
    val gmUserId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val systemName: String = "",
    val joinCode: String = "",
    val gmUserId: String = "",
    val memberUids: List<String> = emptyList(),
    val mapWidth: Long = 12L,
    val mapHeight: Long = 12L,
    val mapGridJson: String = "{}",
    val mapTokensJson: String = "[]",
    val mapBackgroundUrl: String? = null,
    val guideProgressJson: String = "[]",
    val nextSessionAtMillis: Long = 0L,
    val nextSessionNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Character(
    @DocumentId val id: String = "",
    val storyId: String = "",
    val gmUserId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val kind: String = "player", // "player" | "npc" | "monster"
    val name: String = "",
    val avatarEmoji: String = "🛡️",
    val avatarUrl: String? = null,
    val roleTitle: String = "",
    val level: Long = 1L,
    val hpCurrent: Long = 10L,
    val hpMax: Long = 10L,
    val ac: Long = 10L,
    val notes: String = "",
    val inventoryJson: String = "[]",
    val missionsJson: String = "[]",
    val powersJson: String = "[]",
    val equipmentJson: String = "{}",
    val statsJson: String = "{}",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class WorldItem(
    @DocumentId val id: String = "",
    val storyId: String = "",
    val gmUserId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val category: String = "location", // "location" | "power" | "note"
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val iconName: String = "Place",
    val iconEmoji: String = "🏰",
    val imageUrl: String? = null,
    val sortOrder: Long = 0L,
    val tagsJson: String = "[]",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class DiceRoll(
    @DocumentId val id: String = "",
    val storyId: String = "",
    val gmUserId: String = "",
    val memberUids: List<String> = emptyList(),
    val userId: String = "",
    val rollerName: String = "",
    val expression: String = "",
    val total: Long = 0L,
    val breakdown: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val storyId: String = "",
    val gmUserId: String = "",
    val memberUids: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "Player",
    val body: String = "",
    val messageType: String = "chat", // "chat" | "roll" | "system"
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val storyId: String = "",
    val gmUserId: String = "",
    val memberUids: List<String> = emptyList(),
    val actorId: String = "",
    val actorName: String = "",
    val actionType: String = "system",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

// --- Local Structured JSON Sub-Models ---

data class GridCell(
    val colorHex: String? = null,
    val emoji: String? = null,
    val imageBase64: String? = null,
    val label: String? = null,
)

data class MapToken(
    val id: String,
    val label: String,
    val emoji: String,
    val kind: String, // "player" | "npc" | "monster"
    val ownerId: String = "",
    val x: Int,
    val y: Int,
    val colorHex: String = "#F59E0B",
)

data class InventoryEntry(
    val id: String,
    val name: String,
    val quantity: Int = 1,
    val notes: String = "",
    val emoji: String = "🎒",
)

data class MissionEntry(
    val id: String,
    val title: String,
    val status: String = "active", // "active" | "completed"
    val description: String = "",
)

data class PowerEntry(
    val id: String,
    val name: String,
    val cost: String = "",
    val description: String = "",
    val emoji: String = "✨",
)

enum class EquipmentSlot(val key: String, val label: String, val emoji: String) {
    HEAD("head", "Head", "🪖"),
    EYES("eyes", "Eyes / Face", "🎭"),
    NECK("neck", "Neck / Amulet", "📿"),
    SHOULDERS("shoulders", "Shoulders", "🛡️"),
    CHEST("chest", "Chest / Armor", "🦺"),
    BACK("back", "Cloak / Back", "🧥"),
    WRISTS("wrists", "Wrists", "⌚"),
    HANDS("hands", "Hands / Gloves", "🧤"),
    WAIST("waist", "Waist / Belt", "🥋"),
    LEGS("legs", "Legs / Greaves", "👖"),
    FEET("feet", "Feet / Boots", "🥾"),
    WEAPON_MAIN("weapon_main", "Main Hand / Ring", "⚔️"),
}

data class EquipmentSlotItem(
    val name: String = "",
    val bonus: String = "",
    val notes: String = "",
    val emoji: String = "",
    val imageUrl: String? = null,
)

data class QuickMapAsset(
    val id: String,
    val label: String,
    val imageBase64: String,
)

data class QuickMapBlock(
    val id: String,
    val label: String = "",
    val colorHex: String? = null,
    val emoji: String? = null,
    val imageBase64: String? = null,
)

data class MapBackgroundLayer(
    val id: String,
    val label: String = "Map Layer",
    val imageBase64: String,
    val gridX: Float = 0f,
    val gridY: Float = 0f,
    val widthCells: Float = 12f,
    val heightCells: Float = 12f,
    val opacity: Float = 1f,
)

data class ScheduledSessionRecord(
    val storyId: String,
    val storyTitle: String,
    val sessionAtMillis: Long,
    val note: String,
)

// --- Safe JSON Codecs ---

object JsonCodecs {
    fun decodeGrid(json: String): Map<String, GridCell> {
        if (json.isBlank() || json == "{}") return emptyMap()
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, GridCell>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val cellObj = obj.optJSONObject(key) ?: continue
                result[key] = GridCell(
                    colorHex = cellObj.optString("colorHex").takeIf { it.isNotBlank() },
                    emoji = cellObj.optString("emoji").takeIf { it.isNotBlank() },
                    imageBase64 = cellObj.optString("imageBase64").takeIf { it.isNotBlank() },
                    label = cellObj.optString("label").takeIf { it.isNotBlank() },
                )
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun encodeGrid(grid: Map<String, GridCell>): String {
        val root = JSONObject()
        grid.forEach { (key, cell) ->
            if (cell.colorHex != null || cell.emoji != null || cell.imageBase64 != null || cell.label != null) {
                val obj = JSONObject()
                cell.colorHex?.let { obj.put("colorHex", it) }
                cell.emoji?.let { obj.put("emoji", it) }
                cell.imageBase64?.let { obj.put("imageBase64", it) }
                cell.label?.let { obj.put("label", it) }
                root.put(key, obj)
            }
        }
        return root.toString()
    }

    fun decodeTokens(json: String): List<MapToken> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { idx ->
                val o = arr.optJSONObject(idx) ?: return@mapNotNull null
                MapToken(
                    id = o.optString("id"),
                    label = o.optString("label", "Token"),
                    emoji = o.optString("emoji", "🧙"),
                    kind = o.optString("kind", "player"),
                    ownerId = o.optString("ownerId", ""),
                    x = o.optInt("x", 0),
                    y = o.optInt("y", 0),
                    colorHex = o.optString("colorHex", "#F59E0B"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeTokens(tokens: List<MapToken>): String {
        val arr = JSONArray()
        tokens.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("label", t.label)
                put("emoji", t.emoji)
                put("kind", t.kind)
                put("ownerId", t.ownerId)
                put("x", t.x)
                put("y", t.y)
                put("colorHex", t.colorHex)
            })
        }
        return arr.toString()
    }

    fun decodeStringSet(json: String): Set<String> {
        if (json.isBlank() || json == "[]") return emptySet()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun encodeStringSet(items: Set<String>): String {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        return arr.toString()
    }

    fun decodeStringList(json: String): List<String> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeStringList(items: List<String>): String {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        return arr.toString()
    }

    fun decodeInventory(json: String): List<InventoryEntry> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                InventoryEntry(
                    id = o.optString("id", "inv_$i"),
                    name = o.optString("name"),
                    quantity = o.optInt("quantity", 1),
                    notes = o.optString("notes", ""),
                    emoji = o.optString("emoji", "🎒"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeInventory(items: List<InventoryEntry>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("quantity", item.quantity)
                put("notes", item.notes)
                put("emoji", item.emoji)
            })
        }
        return arr.toString()
    }

    fun decodeMissions(json: String): List<MissionEntry> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                MissionEntry(
                    id = o.optString("id", "mis_$i"),
                    title = o.optString("title"),
                    status = o.optString("status", "active"),
                    description = o.optString("description", ""),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeMissions(items: List<MissionEntry>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("status", item.status)
                put("description", item.description)
            })
        }
        return arr.toString()
    }

    fun decodePowers(json: String): List<PowerEntry> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                PowerEntry(
                    id = o.optString("id", "pow_$i"),
                    name = o.optString("name"),
                    cost = o.optString("cost", ""),
                    description = o.optString("description", ""),
                    emoji = o.optString("emoji", "✨"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodePowers(items: List<PowerEntry>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("cost", item.cost)
                put("description", item.description)
                put("emoji", item.emoji)
            })
        }
        return arr.toString()
    }

    fun decodeEquipment(json: String): Map<String, EquipmentSlotItem> {
        if (json.isBlank() || json == "{}") return emptyMap()
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, EquipmentSlotItem>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val slotObj = obj.optJSONObject(key) ?: continue
                result[key] = EquipmentSlotItem(
                    name = slotObj.optString("name", ""),
                    bonus = slotObj.optString("bonus", ""),
                    notes = slotObj.optString("notes", ""),
                    emoji = slotObj.optString("emoji", ""),
                    imageUrl = slotObj.optString("imageUrl").takeIf { it.isNotBlank() },
                )
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun encodeEquipment(map: Map<String, EquipmentSlotItem>): String {
        val root = JSONObject()
        map.forEach { (slot, item) ->
            if (
                item.name.isNotBlank() ||
                item.bonus.isNotBlank() ||
                item.notes.isNotBlank() ||
                item.emoji.isNotBlank() ||
                !item.imageUrl.isNullOrBlank()
            ) {
                root.put(slot, JSONObject().apply {
                    put("name", item.name)
                    put("bonus", item.bonus)
                    put("notes", item.notes)
                    if (item.emoji.isNotBlank()) put("emoji", item.emoji)
                    if (!item.imageUrl.isNullOrBlank()) put("imageUrl", item.imageUrl)
                })
            }
        }
        return root.toString()
    }

    fun decodeStats(json: String): Map<String, String> {
        if (json.isBlank() || json == "{}") return defaultStats()
        return try {
            val obj = JSONObject(json)
            val result = linkedMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = obj.optString(key, "")
            }
            result.ifEmpty { defaultStats() }
        } catch (_: Exception) {
            defaultStats()
        }
    }

    fun defaultStats(): Map<String, String> = linkedMapOf(
        "STR" to "10",
        "DEX" to "10",
        "CON" to "10",
        "INT" to "10",
        "WIS" to "10",
        "CHA" to "10",
    )

    fun encodeStats(stats: Map<String, String>): String {
        val root = JSONObject()
        stats.forEach { (k, v) -> if (k.isNotBlank()) root.put(k, v) }
        return root.toString()
    }

    fun decodeQuickAssets(json: String): List<QuickMapAsset> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                QuickMapAsset(
                    id = o.optString("id", "qa_$i"),
                    label = o.optString("label", "Tile"),
                    imageBase64 = o.optString("imageBase64"),
                ).takeIf { it.imageBase64.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeQuickAssets(items: List<QuickMapAsset>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                put("imageBase64", item.imageBase64)
            })
        }
        return arr.toString()
    }

    fun decodeQuickBlocks(json: String): List<QuickMapBlock> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val color = o.optString("colorHex").takeIf { it.isNotBlank() }
                val emoji = o.optString("emoji").takeIf { it.isNotBlank() }
                val img = o.optString("imageBase64").takeIf { it.isNotBlank() }
                if (color == null && emoji == null && img == null) null
                else QuickMapBlock(
                    id = o.optString("id", "qb_$i"),
                    label = o.optString("label", ""),
                    colorHex = color,
                    emoji = emoji,
                    imageBase64 = img,
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeQuickBlocks(items: List<QuickMapBlock>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                item.colorHex?.let { put("colorHex", it) }
                item.emoji?.let { put("emoji", it) }
                item.imageBase64?.let { put("imageBase64", it) }
            })
        }
        return arr.toString()
    }

    fun decodeMapBackgroundLayers(
        raw: String?,
        fallbackWidth: Int = 12,
        fallbackHeight: Int = 12,
    ): List<MapBackgroundLayer> {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return emptyList()
        if (!trimmed.startsWith("[")) {
            return listOf(
                MapBackgroundLayer(
                    id = "bg_legacy",
                    label = "Map Background",
                    imageBase64 = trimmed,
                    gridX = 0f,
                    gridY = 0f,
                    widthCells = fallbackWidth.toFloat().coerceAtLeast(1f),
                    heightCells = fallbackHeight.toFloat().coerceAtLeast(1f),
                    opacity = 1f,
                )
            )
        }
        return try {
            val arr = JSONArray(trimmed)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val img = o.optString("imageBase64").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                MapBackgroundLayer(
                    id = o.optString("id", "bg_$i"),
                    label = o.optString("label", "Map ${i + 1}"),
                    imageBase64 = img,
                    gridX = o.optDouble("gridX", 0.0).toFloat(),
                    gridY = o.optDouble("gridY", 0.0).toFloat(),
                    widthCells = o.optDouble("widthCells", fallbackWidth.toDouble()).toFloat().coerceAtLeast(1f),
                    heightCells = o.optDouble("heightCells", fallbackHeight.toDouble()).toFloat().coerceAtLeast(1f),
                    opacity = o.optDouble("opacity", 1.0).toFloat().coerceIn(0.15f, 1f),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeMapBackgroundLayers(layers: List<MapBackgroundLayer>): String? {
        if (layers.isEmpty()) return null
        val arr = JSONArray()
        layers.forEach { layer ->
            arr.put(JSONObject().apply {
                put("id", layer.id)
                put("label", layer.label)
                put("imageBase64", layer.imageBase64)
                put("gridX", layer.gridX.toDouble())
                put("gridY", layer.gridY.toDouble())
                put("widthCells", layer.widthCells.toDouble())
                put("heightCells", layer.heightCells.toDouble())
                put("opacity", layer.opacity.toDouble())
            })
        }
        return arr.toString()
    }

    fun decodeScheduledSessions(json: String): List<ScheduledSessionRecord> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val sid = o.optString("storyId")
                val ts = o.optLong("sessionAtMillis", 0L)
                if (sid.isBlank() || ts <= 0L) null
                else ScheduledSessionRecord(
                    storyId = sid,
                    storyTitle = o.optString("storyTitle", "Campaign"),
                    sessionAtMillis = ts,
                    note = o.optString("note", ""),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeScheduledSessions(items: List<ScheduledSessionRecord>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("storyId", item.storyId)
                put("storyTitle", item.storyTitle)
                put("sessionAtMillis", item.sessionAtMillis)
                put("note", item.note)
            })
        }
        return arr.toString()
    }
}
