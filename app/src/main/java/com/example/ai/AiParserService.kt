package com.example.ai

import android.util.Log
import com.example.data.Character
import com.example.data.EquipmentSlot
import com.example.data.EquipmentSlotItem
import com.example.data.GridCell
import com.example.data.InventoryEntry
import com.example.data.JsonCodecs
import com.example.data.MapColorPalette
import com.example.data.MapPaintEngine
import com.example.data.MapPaintJsonParser
import com.example.data.MapPaintOutcome
import com.example.data.MapToken
import com.example.data.MissionEntry
import com.example.data.PowerEntry
import com.example.data.Story
import com.example.data.WorldItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

object EmojiHelper {
    private val keywordMap = listOf(
        listOf("fire", "flame", "dragon", "ember", "inferno", "pyro") to "🔥",
        listOf("ice", "frost", "snow", "glacier", "winter", "cold") to "❄️",
        listOf("lightning", "storm", "thunder", "shock", "arc") to "⚡",
        listOf("poison", "venom", "toxic", "acid", "plague") to "🧪",
        listOf("heal", "cure", "holy", "light", "radiant", "cleric", "paladin") to "✨",
        listOf("shadow", "dark", "void", "necromancy", "rogue", "stealth", "assassin") to "🗡️",
        listOf("sword", "blade", "warrior", "fighter", "knight", "martial") to "⚔️",
        listOf("shield", "armor", "guard", "bastion", "defender") to "🛡️",
        listOf("bow", "arrow", "ranger", "archer", "hunter") to "🏹",
        listOf("magic", "arcane", "wizard", "mage", "sorcerer", "spell", "rune") to "🔮",
        listOf("castle", "keep", "fortress", "citadel", "tower") to "🏰",
        listOf("tavern", "inn", "pub", "ale", "hearth") to "🍺",
        listOf("forest", "woods", "grove", "druid", "tree") to "🌲",
        listOf("mountain", "peak", "cavern", "dungeon", "cave") to "⛰️",
        listOf("ship", "sea", "port", "ocean", "pirate", "harbor") to "⚓",
        listOf("village", "town", "city", "market", "square") to "🏘️",
        listOf("temple", "shrine", "altar", "cathedral", "ruins") to "🏛️",
        listOf("undead", "skeleton", "zombie", "lich", "ghost") to "💀",
        listOf("beast", "wolf", "spider", "goblin", "orc", "monster") to "🐺",
        listOf("gold", "coin", "treasure", "loot", "gem", "crown") to "👑",
        listOf("book", "lore", "scroll", "clue", "mystery", "journal") to "📜",
    )

    fun suggestEmoji(text: String, fallback: String = "📜"): String {
        val lower = text.lowercase()
        for ((keywords, emoji) in keywordMap) {
            if (keywords.any { lower.contains(it) }) return emoji
        }
        return fallback
    }

    fun extractLeadingEmoji(text: String): String? {
        val trimmed = text.trim().removePrefix("#").removePrefix("-").removePrefix("*").trim()
        if (trimmed.isEmpty()) return null
        val firstCp = trimmed.codePointAt(0)
        val isEmojiStart = firstCp > 0x2600 ||
            java.lang.Character.getType(firstCp) == java.lang.Character.OTHER_SYMBOL.toInt()
        if (!isEmojiStart) return null
        var idx = java.lang.Character.charCount(firstCp)
        while (idx < trimmed.length) {
            val cp = trimmed.codePointAt(idx)
            val isJoinerOrSelector = cp == 0xFE0F || cp == 0x200D || (cp in 0x1F3FB..0x1F3FF)
            if (isJoinerOrSelector) {
                idx += java.lang.Character.charCount(cp)
                if (cp == 0x200D && idx < trimmed.length) {
                    val nextCp = trimmed.codePointAt(idx)
                    idx += java.lang.Character.charCount(nextCp)
                }
            } else {
                break
            }
        }
        return trimmed.substring(0, idx)
    }

    fun stripLeadingEmoji(text: String): String {
        val clean = text.trim().removePrefix("#").removePrefix("-").removePrefix("*").trim()
        val em = extractLeadingEmoji(clean) ?: return clean
        return clean.removePrefix(em).trim()
    }
}

object AiPromptTemplates {
    fun parserSystemInstructions(contextHint: String? = null): String = buildString {
        appendLine("STORY WEAVER PARSER RULES & SYMBOLS:")
        appendLine("Whenever you generate characters, NPCs, monsters, locations, powers, items, notes, or equipment loadouts, ALWAYS include clear parser symbols and matching emojis so the user can import them directly:")
        appendLine("- Characters / NPCs / Monsters (separate multiple entries with a blank line):")
        appendLine("  <Emoji> <Name>")
        appendLine("  Class: <Role/Class/Species>")
        appendLine("  Location: <Location Name>")
        appendLine("  LVL: <integer> | HP: <integer> | AC: <integer>")
        appendLine("  Emoji: <single fitting Unicode emoji>")
        appendLine("  Powers: <Power Name> (<Cost>) - <Full Description> [#tag1, #tag2], <Next Power...>")
        appendLine("  Missions: <Mission Title> - <Objective details>, <Another Quest> (completed) - <Details>")
        appendLine("  Gear: <comma-separated tools, weapons, armor & equipment>")
        appendLine("  Stats: STR <n>, DEX <n>, CON <n>, INT <n>, WIS <n>, CHA <n>")
        appendLine("- Locations / Powers / Items / Notes (separate multiple cards with a blank line):")
        appendLine("  # <Emoji> <Title>")
        appendLine("  Subtitle: <Category/Type>")
        appendLine("  Tags: <tag1, tag2>")
        appendLine("  <Detailed description lines>")
        appendLine("- Equipment Loadout (12 exact slot symbols — ALWAYS use these exact slot keys):")
        appendLine("  Slot keys: head, eyes, neck, shoulders, chest, back, wrists, hands, waist, legs, feet, weapon_main")
        appendLine("  Format each slot as: <slotKey>: <Emoji> <Item Name> (<Bonus>) - <Properties/Notes>")
        if (!contextHint.isNullOrBlank()) {
            appendLine("Active section focus: $contextHint")
        }
    }.trim()

    fun characterPrompt(kind: String): String = """
        Convert the following notes into structured $kind entries for a tabletop RPG.
        Ignore any conversational filler, greetings, intro sentences (e.g., "Here you have 10 NPCs..."), or sign-offs — ONLY extract actual characters.
        For each entry, extract: Name, Role/Class, Location (locationName), Level, HP, AC, Notes, matching Emoji symbol (avatarEmoji), Stats (STR, DEX, CON, INT, WIS, CHA, plus custom stats), Missions / Quests (missions: title, status active/completed, description), complete Powers (name, cost, description, emoji, tags), and Gear (mapped into inventory AND 12-slot equipment: head, eyes, neck, shoulders, chest, back, wrists, hands, waist, legs, feet, weapon_main).
    """.trimIndent()

    fun worldItemPrompt(category: String): String = """
        Convert the following campaign notes into structured $category cards.
        Ignore any conversational filler, greetings, intro counts (e.g., "Here are 5 locations..."), or sign-offs.
        For each card, include Title, Subtitle/Tag, matching Emoji symbol (iconEmoji), Tags, and vivid usable tabletop Description.
    """.trimIndent()

    fun equipmentPrompt(): String = """
        Map the following character gear description into the 12 exact RPG equipment slot keys:
        head, eyes, neck, shoulders, chest, back, wrists, hands, waist, legs, feet, weapon_main.
        Include an appropriate single Unicode emoji symbol for every equipped item.
    """.trimIndent()

    fun mapArchitectSystemPrompt(request: String? = null): String = buildString {
        val max = com.example.data.MAP_MAX_SIZE
        appendLine("You are the Map Architect of Story Weaver: you build and edit GBA-style 16px tile maps for a tabletop RPG.")
        appendLine("You write an ordered JSON program of painting ops. Return ONLY one valid JSON object (no markdown, no commentary):")
        appendLine(
            """
            {
              "width": 24, "height": 20, "clearFirst": true, "clearTokens": false,
              "summary": "Short description of the map or of the edit",
              "operations": [
                {"tool": "biome", "x1": 0, "y1": 0, "x2": 23, "y2": 19, "terrains": {"grass": 5, "tall_grass": 2, "flower_meadow": 1}, "props": {"tree_oak": 3, "bush": 2, "rock": 1}, "density": 0.06, "seed": 7},
                {"tool": "circle", "cx": 5, "cy": 14, "radius": 3, "filled": true, "tile": "water"},
                {"tool": "line", "line": "river", "points": [[5, 11], [5, 4], [12, 0]]},
                {"tool": "line", "line": "road", "x1": 0, "y1": 9, "x2": 23, "y2": 9},
                {"tool": "paint", "points": [[5, 9]], "line": "wood_bridge"},
                {"tool": "rect", "x1": 14, "y1": 12, "x2": 20, "y2": 17, "tile": "farmland"},
                {"tool": "rect_border", "x1": 13, "y1": 11, "x2": 21, "y2": 18, "line": "wood_fence", "thickness": 1},
                {"tool": "building", "x1": 15, "y1": 2, "x2": 19, "y2": 5, "roof": "roof_red", "wall": "plaster_wall", "door": "door_wood_closed_s"},
                {"tool": "place", "obj": "human_inn", "x": 8, "y": 5},
                {"tool": "place", "obj": "well_stone", "points": [[12, 10]]},
                {"tool": "scatter", "x1": 0, "y1": 0, "x2": 23, "y2": 3, "count": 8, "obj": "tree_pine"},
                {"tool": "kingdom", "x1": 2, "y1": 2, "x2": 21, "y2": 17, "kingdom": "elven", "ground": "lush_grass", "count": 6},
                {"tool": "flood_fill", "x": 3, "y": 3, "tile": "sand"},
                {"tool": "erase", "x1": 7, "y1": 1, "x2": 8, "y2": 1}
              ],
              "tokens": [
                {"label": "Gate Guard", "sprite": "guard", "emoji": "🛡️", "kind": "npc", "x": 11, "y": 16}
              ]
            }
            """.trimIndent()
        )
        appendLine("(The example shows every op once; it is NOT a sensible map. Use only the ops you need.)")
        appendLine()
        appendLine("OPS:")
        appendLine("- Shape ops rect, rect_border (thickness), line (x1,y1,x2,y2 or a \"points\" polyline, brushSize 1-3), circle (cx, cy, radius, filled), paint (points), scatter (count, onlyEmpty), flood_fill (x, y), checker. Each takes exactly ONE of \"tile\" (terrain), \"line\" (line set) or \"obj\" (object).")
        appendLine("- place: one object at x,y or at every point in \"points\". x,y is the object's TOP-LEFT cell; a 2x3 tree at (4,4) covers 4..5 x 4..6.")
        appendLine("- building: exterior = roof terrain footprint + one wall row at the bottom + door at bottom-centre (\"doorX\" to move it). With \"floor\" (or tool \"room\") it is an INTERIOR: floor + wall ring + door gap. Keys: roof, wall, door, window, floor.")
        appendLine("- biome: natural area. \"terrains\" = weighted names (first listed = lowest ground, good for water), \"scale\" = patch size (default 5), \"props\" = weighted objects scattered at \"density\" (0..0.6; never on liquid, walls, cliffs, roads or objects already there), \"seed\". Leave out \"terrains\" to scatter props on the ground that is ALREADY painted (edits, or decorating after other ops).")
        appendLine("- kingdom: a whole settlement in the box: optional \"ground\", rampart ring (\"wall\" line, \"walls\": false for an open village) with a south gate, main road (\"road\" line), castle (\"castle\": false to skip) and \"count\" buildings of that \"kingdom\" style. Needs about 16x16 or more.")
        appendLine("- erase: clears every layer of the cells in the box.")
        appendLine()
        appendLine("RULES:")
        appendLine("- Grid: width and height 4..$max. Coordinates are 0-indexed, x right, y down. Stay inside the grid, including the full size of big objects.")
        appendLine("- A cell has 3 layers: terrain, line, object. A new terrain replaces the whole cell; lines and objects stack on top of what is there.")
        appendLine("- Ops run in order. Layer order: ground -> liquid -> path terrain -> raised (cliff, forest, walls, roofs) -> lines (rivers, roads, fences, ramparts) -> objects -> tokens.")
        appendLine("- Use ONLY names from the catalogue below. Never numbers or tile indices. If nothing fits, pick the closest name.")
        appendLine("- Do not overlap objects. Keep doors reachable: put a road or path cell in front of every door. Leave walkable space.")
        appendLine("- Rivers and roads must be continuous; cross a river with a bridge line cell painted on the river cell.")
        appendLine("- kingdom: every building door gets its own road spur to the main road automatically; do not draw those yourself.")
        appendLine("- Prefer few big ops (biome, rect, kingdom, building) over many single cells. A full map is usually 8-40 ops.")
        appendLine("- NEW map: clearFirst=true and start with a biome or rect that covers the whole grid, so no cell stays empty.")
        appendLine("- EDIT: clearFirst=false, keep width/height unless asked, and return ONLY the ops that change something. Use the coordinates from CURRENT MAP STATE; never repaint untouched areas.")
        appendLine("- tokens: [] unless the user asks for characters, NPCs or monsters. Give each a \"sprite\" from the catalogue plus a fallback \"emoji\"; kind = player | npc | monster.")
        appendLine("- Content rule: no demons or hellish content. Evil realms are a Corrupted Kingdom or Dark Kingdom (kingdom \"dark\" / \"undead\").")
        appendLine("- Legacy: \"colorHex\" (#RRGGBB) and \"emoji\" still work on shape ops; use them only when the user asks for plain colours or emoji.")
        appendLine()
        appendLine(com.example.data.GuidesContent.MAP_AI_RECIPES)
        appendLine()
        appendLine(com.example.data.MapTileCatalog.promptGuide(request))
    }.trim()
}

class AiParserService {
    private val characterSchema = Schema.obj(
        properties = mapOf(
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Character or creature name"),
                        "roleTitle" to Schema.string(description = "Class, role, or creature type"),
                        "locationName" to Schema.string(description = "Home, settlement, or current location name"),
                        "level" to Schema.integer(description = "Level or challenge rating"),
                        "hpMax" to Schema.integer(description = "Maximum hit points"),
                        "ac" to Schema.integer(description = "Armor class"),
                        "notes" to Schema.string(description = "Concise tabletop notes and lore"),
                        "avatarEmoji" to Schema.string(description = "Single matching Unicode emoji symbol"),
                        "stats" to Schema.string(description = "Stats line e.g. STR 17, DEX 10, CON 16, INT 15, WIS 12, CHA 11"),
                        "powers" to Schema.array(
                            items = Schema.string(),
                            description = "Abilities formatted as 'Name (Cost) - Description [tag1, tag2]'"
                        ),
                        "missions" to Schema.array(
                            items = Schema.string(),
                            description = "Quests or missions formatted as 'Mission Title - Description'"
                        ),
                        "inventory" to Schema.array(items = Schema.string(), description = "Carried tools, weapons, armor, and items"),
                    ),
                    optionalProperties = listOf("locationName", "avatarEmoji", "stats", "powers", "missions", "inventory")
                )
            )
        )
    )

    private val worldItemSchema = Schema.obj(
        properties = mapOf(
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Entry title"),
                        "subtitle" to Schema.string(description = "Short tag or subtype"),
                        "description" to Schema.string(description = "Detailed tabletop description"),
                        "iconEmoji" to Schema.string(description = "Single matching Unicode emoji symbol"),
                        "tags" to Schema.array(items = Schema.string(), description = "1-4 short tags"),
                    ),
                    optionalProperties = listOf("iconEmoji", "tags")
                )
            )
        )
    )

    private val equipmentSchema = Schema.obj(
        properties = mapOf(
            "slots" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "slotKey" to Schema.enumeration(
                            values = EquipmentSlot.entries.map { it.key },
                            description = "Target equipment slot key"
                        ),
                        "itemName" to Schema.string(description = "Item name"),
                        "bonus" to Schema.string(description = "Stat or mechanical bonus e.g. +1 AC"),
                        "notes" to Schema.string(description = "Short item properties"),
                        "emoji" to Schema.string(description = "Single matching Unicode emoji symbol for the item"),
                    ),
                    optionalProperties = listOf("bonus", "notes", "emoji")
                )
            )
        )
    )

    suspend fun chatWithAi(
        history: List<AiChatMessage>,
        story: Story?,
        provider: AiProvider,
        modelName: String,
        endpointUrl: String = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT,
        apiKeyOverride: String = "",
        contextHint: String? = null,
    ): Result<String> {
        val systemPrompt = buildString {
            appendLine("You are an expert tabletop RPG co-GM and worldbuilding assistant inside Story Weaver.")
            if (story != null) {
                appendLine("Active campaign: \"${story.title}\" (${story.systemName}).")
                if (story.description.isNotBlank()) {
                    appendLine("Premise: ${story.description}.")
                }
            }
            appendLine()
            appendLine("VISUAL CODE OUTPUT: If the user asks you to create an image, emblem, icon, or map diagram in code, output clean self-contained <svg viewBox=\"0 0 512 512\">...</svg> vector markup so the app renders it directly in chat for gallery download.")
            appendLine()
            append(AiPromptTemplates.parserSystemInstructions(contextHint))
        }

        return when (provider) {
            AiProvider.OPENROUTER, AiProvider.UNOROUTER -> {
                val payloadMessages = buildList {
                    add("system" to systemPrompt)
                    history.takeLast(16).forEach { msg ->
                        if (msg.role == "user" || msg.role == "assistant") {
                            add(msg.role to msg.content)
                        }
                    }
                }
                val fallback = if (provider == AiProvider.OPENROUTER) {
                    AiModelDefaults.DEFAULT_OPENROUTER_MODEL
                } else {
                    AiModelDefaults.DEFAULT_UNOROUTER_MODEL
                }
                val resolvedModel = modelName.trim().ifBlank { fallback }
                UnorouterClient.sendChatCompletion(
                    model = resolvedModel,
                    messages = payloadMessages,
                    provider = provider,
                    endpointUrl = endpointUrl,
                    apiKeyOverride = apiKeyOverride,
                )
            }
            AiProvider.GEMINI -> {
                try {
                    val resolvedModel = modelName.trim().ifBlank { AiModelDefaults.DEFAULT_GEMINI_MODEL }
                    val chatModel = Firebase.ai.generativeModel(modelName = resolvedModel)
                    val transcript = buildString {
                        appendLine(systemPrompt)
                        appendLine()
                        history.takeLast(16).forEach { msg ->
                            val speaker = if (msg.role == "user") "User" else "Assistant"
                            appendLine("$speaker: ${msg.content}")
                        }
                        append("Assistant:")
                    }
                    val response = withTimeout(60_000L) {
                        chatModel.generateContent(transcript)
                    }
                    val text = response.text?.trim()
                        ?: throw IllegalStateException("Empty Gemini response.")
                    Result.success(text)
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "Gemini chat timed out", e)
                    Result.failure(IllegalStateException("AI chat timed out after 60 seconds."))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Gemini chat failed", e)
                    Result.failure(e)
                }
            }
        }
    }

    suspend fun parseCharactersBundleWithAi(
        rawText: String,
        kind: String,
        story: Story,
        ownerId: String,
        provider: AiProvider,
        modelName: String,
        endpointUrl: String = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT,
        apiKeyOverride: String = "",
    ): Result<CharacterImportBundle> = parseCharactersWithAi(
        rawText = rawText,
        kind = kind,
        story = story,
        ownerId = ownerId,
        provider = provider,
        modelName = modelName,
        endpointUrl = endpointUrl,
        apiKeyOverride = apiKeyOverride,
    ).map { chars ->
        CharacterParserHelpers.buildWorldBundleFromCharacters(chars, story, ownerId)
    }

    suspend fun parseCharactersWithAi(
        rawText: String,
        kind: String,
        story: Story,
        ownerId: String,
        provider: AiProvider,
        modelName: String,
        endpointUrl: String = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT,
        apiKeyOverride: String = "",
    ): Result<List<Character>> = try {
        val jsonText = when (provider) {
            AiProvider.OPENROUTER, AiProvider.UNOROUTER -> {
                val sys = """
                    You are a strict JSON parser for a tabletop RPG app.
                    ${AiPromptTemplates.parserSystemInstructions(kind)}
                    CRITICAL: Ignore conversational wrapper prose, greetings, intro lines (such as "Here you have 10 NPCs...", "Sure! Here are..."), horizontal dividers, or sign-offs ("Let me know if you need more"). NEVER create an entry out of intro/outro text.
                    Return ONLY a valid JSON object matching this schema (no markdown or extra prose):
                    {"items":[{"name":"string","roleTitle":"string","locationName":"string","level":1,"hpMax":12,"ac":12,"notes":"string","avatarEmoji":"🛡️","stats":{"STR":10,"DEX":10,"CON":10,"INT":10,"WIS":10,"CHA":10},"powers":[{"name":"string","cost":"string","description":"string","emoji":"✨","tags":["tag1"]}],"missions":[{"title":"string","status":"active","description":"string"}],"inventory":["string"],"equipment":[{"slotKey":"weapon_main","itemName":"string","bonus":"+1 Attack","notes":"string","emoji":"⚔️"}]}]}
                """.trimIndent()
                val userPrompt = "${AiPromptTemplates.characterPrompt(kind)}\n\nCampaign: ${story.title} (${story.systemName})\n\nInput:\n$rawText"
                val fallback = if (provider == AiProvider.OPENROUTER) {
                    AiModelDefaults.DEFAULT_OPENROUTER_MODEL
                } else {
                    AiModelDefaults.DEFAULT_UNOROUTER_MODEL
                }
                val resolved = modelName.trim().ifBlank { fallback }
                val raw = UnorouterClient.sendChatCompletion(
                    model = resolved,
                    messages = listOf("system" to sys, "user" to userPrompt),
                    provider = provider,
                    endpointUrl = endpointUrl,
                    apiKeyOverride = apiKeyOverride,
                    temperature = 0.2
                ).getOrThrow()
                UnorouterClient.extractJsonObject(raw)
            }
            AiProvider.GEMINI -> {
                val resolved = modelName.trim().ifBlank { AiModelDefaults.DEFAULT_GEMINI_MODEL }
                val model = Firebase.ai.generativeModel(
                    modelName = resolved,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = characterSchema
                    }
                )
                val prompt = "${AiPromptTemplates.characterPrompt(kind)}\n\nCampaign: ${story.title} (${story.systemName})\n\nInput:\n$rawText"
                val response = withTimeout(60_000L) { model.generateContent(prompt) }
                response.text ?: throw IllegalStateException("Empty AI response.")
            }
        }

        val root = JSONObject(jsonText)
        val arr = root.getJSONArray("items")
        val rawSectionsFallback = CharacterParserHelpers.extractFieldSections(rawText)
        val parsed = (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val name = o.optString("name", "Unnamed").trim().ifBlank { "Unnamed" }
            if (CharacterParserHelpers.isConversationalFillerTitle(name)) return@mapNotNull null
            val role = o.optString("roleTitle", kind.replaceFirstChar { it.uppercase() })
            val locationName = o.optString("locationName", "")
                .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: rawSectionsFallback["location"].orEmpty()
            val level = o.optInt("level", 1).coerceIn(1, 999).toLong()
            val hp = o.optInt("hpMax", 12).coerceIn(1, 9999).toLong()
            val ac = o.optInt("ac", 12).coerceIn(1, 99).toLong()
            val notes = o.optString("notes", "").ifBlank { rawText.take(4000) }
            val emoji = o.optString("avatarEmoji")
                .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: EmojiHelper.suggestEmoji("$name $role $notes", if (kind == "monster") "🐺" else "🛡️")

            // Parse powers (object or rich formatted string)
            val powersArr = o.optJSONArray("powers")
            val powersList = mutableListOf<PowerEntry>()
            if (powersArr != null) {
                for (idx in 0 until powersArr.length()) {
                    val item = powersArr.opt(idx)
                    if (item is JSONObject) {
                        val pName = item.optString("name").trim()
                        if (pName.isNotBlank()) {
                            val pDesc = item.optString("description", "").trim()
                            val pCost = item.optString("cost", "").trim()
                            val pEmoji = item.optString("emoji")
                                .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                                ?: EmojiHelper.suggestEmoji("$pName $pDesc", "✨")
                            val tagsArr = item.optJSONArray("tags")
                            val pTags = if (tagsArr != null) {
                                (0 until tagsArr.length()).mapNotNull { ti ->
                                    tagsArr.optString(ti).trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                                }
                            } else emptyList()
                            powersList += PowerEntry(
                                id = "pow_${UUID.randomUUID().toString().take(8)}",
                                name = EmojiHelper.stripLeadingEmoji(pName).ifBlank { pName },
                                cost = pCost,
                                description = pDesc.ifBlank { "Ability of $name: $pName." },
                                emoji = pEmoji,
                                tags = pTags.ifEmpty { CharacterParserHelpers.suggestTags("$pName $pDesc", "power") },
                            )
                        }
                    } else {
                        val pRaw = powersArr.optString(idx).trim()
                        CharacterParserHelpers.parseSinglePowerEntry(pRaw)?.let { powersList += it }
                    }
                }
            }
            if (powersList.isEmpty() && rawSectionsFallback["powers"] != null && arr.length() == 1) {
                powersList += CharacterParserHelpers.parsePowerEntries(rawSectionsFallback["powers"])
            }

            // Parse inventory & equipment
            val invArr = o.optJSONArray("inventory")
            val gearItems = mutableListOf<ParsedGearItem>()
            if (invArr != null) {
                for (idx in 0 until invArr.length()) {
                    val iRaw = invArr.optString(idx).trim()
                    CharacterParserHelpers.parseSingleGearItem(iRaw)?.let { gearItems += it }
                }
            }
            if (gearItems.isEmpty() && rawSectionsFallback["gear"] != null && arr.length() == 1) {
                gearItems += CharacterParserHelpers.parseGearEntries(rawSectionsFallback["gear"])
            }
            val invList = gearItems.map { g ->
                InventoryEntry(
                    id = "inv_${UUID.randomUUID().toString().take(8)}",
                    name = g.name,
                    quantity = g.quantity,
                    notes = listOfNotNull(
                        g.bonus.takeIf { it.isNotBlank() },
                        g.notes.takeIf { it.isNotBlank() },
                    ).joinToString(" — "),
                    emoji = g.emoji,
                )
            }

            val equipmentMap = CharacterParserHelpers.buildEquipmentMap(gearItems).toMutableMap()
            val eqArr = o.optJSONArray("equipment")
            if (eqArr != null) {
                for (idx in 0 until eqArr.length()) {
                    val eo = eqArr.optJSONObject(idx) ?: continue
                    val slotKey = eo.optString("slotKey").trim()
                    val itemName = eo.optString("itemName").trim()
                    val validSlot = EquipmentSlot.entries.firstOrNull { it.key == slotKey }
                    if (validSlot != null && itemName.isNotBlank()) {
                        equipmentMap[slotKey] = EquipmentSlotItem(
                            name = EmojiHelper.stripLeadingEmoji(itemName).ifBlank { itemName },
                            bonus = eo.optString("bonus", ""),
                            notes = eo.optString("notes", ""),
                            emoji = eo.optString("emoji").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                                ?: EmojiHelper.suggestEmoji(itemName, validSlot.emoji),
                        )
                    }
                }
            } else {
                val eqObj = o.optJSONObject("equipment")
                if (eqObj != null) {
                    val keys = eqObj.keys()
                    while (keys.hasNext()) {
                        val slotKey = keys.next()
                        val validSlot = EquipmentSlot.entries.firstOrNull { it.key == slotKey } ?: continue
                        val slotVal = eqObj.optJSONObject(slotKey)
                        if (slotVal != null) {
                            val itemName = slotVal.optString("name").ifBlank { slotVal.optString("itemName") }.trim()
                            if (itemName.isNotBlank()) {
                                equipmentMap[slotKey] = EquipmentSlotItem(
                                    name = itemName,
                                    bonus = slotVal.optString("bonus", ""),
                                    notes = slotVal.optString("notes", ""),
                                    emoji = slotVal.optString("emoji").ifBlank { validSlot.emoji },
                                )
                            }
                        } else {
                            val itemStr = eqObj.optString(slotKey).trim()
                            CharacterParserHelpers.parseSingleGearItem(itemStr)?.let { g ->
                                equipmentMap[slotKey] = EquipmentSlotItem(
                                    name = g.name,
                                    bonus = g.bonus,
                                    notes = g.notes,
                                    emoji = g.emoji,
                                )
                            }
                        }
                    }
                }
            }

            // Parse stats (supports object map OR string, with rawText fallback)
            val parsedStats = JsonCodecs.defaultStats().toMutableMap()
            val statsObj = o.optJSONObject("stats")
            if (statsObj != null) {
                val keys = statsObj.keys()
                while (keys.hasNext()) {
                    val sk = keys.next().trim().uppercase()
                    val sv = statsObj.optString(sk).takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                        ?: statsObj.optString(keys.toString()).takeIf { it.isNotBlank() }
                    if (!sv.isNullOrBlank()) parsedStats[sk] = sv
                }
            } else {
                val statsStr = o.optString("stats", "")
                if (statsStr.isNotBlank() && !statsStr.equals("null", ignoreCase = true)) {
                    parsedStats.putAll(CharacterParserHelpers.parseStatsMap(statsStr, parsedStats))
                }
            }
            if (arr.length() == 1 && rawSectionsFallback["stats"] != null) {
                val defaults = JsonCodecs.defaultStats()
                val rawParsed = CharacterParserHelpers.parseStatsMap(rawSectionsFallback["stats"])
                rawParsed.forEach { (k, v) ->
                    if (parsedStats[k] == defaults[k] || !parsedStats.containsKey(k)) {
                        parsedStats[k] = v
                    }
                }
            }

            // Parse missions / quests
            val missionsArr = o.optJSONArray("missions") ?: o.optJSONArray("quests")
            val missionsList = mutableListOf<MissionEntry>()
            if (missionsArr != null) {
                for (idx in 0 until missionsArr.length()) {
                    val mItem = missionsArr.opt(idx)
                    if (mItem is JSONObject) {
                        val mTitle = mItem.optString("title").ifBlank { mItem.optString("name") }.trim()
                        if (mTitle.isNotBlank()) {
                            val rawStatus = mItem.optString("status", "active").trim().lowercase()
                            val cleanStatus = if (rawStatus in setOf("completed", "done", "finished")) "completed" else "active"
                            val mDesc = mItem.optString("description", "").trim()
                            missionsList += MissionEntry(
                                id = "mis_${UUID.randomUUID().toString().take(8)}",
                                title = mTitle.take(160),
                                status = cleanStatus,
                                description = mDesc.take(600),
                            )
                        }
                    } else {
                        val mRaw = missionsArr.optString(idx).trim()
                        CharacterParserHelpers.parseSingleMissionEntry(mRaw)?.let { missionsList += it }
                    }
                }
            }
            if (missionsList.isEmpty() && rawSectionsFallback["missions"] != null && arr.length() == 1) {
                missionsList += CharacterParserHelpers.parseMissionEntries(rawSectionsFallback["missions"])
            }

            Character(
                storyId = story.id,
                gmUserId = story.gmUserId,
                ownerId = ownerId,
                memberUids = story.memberUids,
                kind = kind,
                name = name.take(100),
                avatarEmoji = emoji.take(16),
                roleTitle = role.take(100),
                locationName = locationName.trim().take(120),
                level = level,
                hpCurrent = hp,
                hpMax = hp,
                ac = ac,
                notes = notes.take(5000),
                powersJson = JsonCodecs.encodePowers(powersList),
                missionsJson = JsonCodecs.encodeMissions(missionsList),
                inventoryJson = JsonCodecs.encodeInventory(invList),
                equipmentJson = JsonCodecs.encodeEquipment(equipmentMap),
                statsJson = JsonCodecs.encodeStats(parsedStats),
            )
        }
        Result.success(parsed)
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI character parse timed out", e)
        Result.failure(IllegalStateException("AI parsing timed out after 60 seconds. Please try again."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "AI character parse failed", e)
        Result.failure(e)
    }

    suspend fun parseWorldItemsWithAi(
        rawText: String,
        category: String,
        story: Story,
        ownerId: String,
        startOrder: Int,
        provider: AiProvider,
        modelName: String,
        endpointUrl: String = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT,
        apiKeyOverride: String = "",
    ): Result<List<WorldItem>> = try {
        val jsonText = when (provider) {
            AiProvider.OPENROUTER, AiProvider.UNOROUTER -> {
                val sys = """
                    You are a strict JSON parser for a tabletop RPG app.
                    ${AiPromptTemplates.parserSystemInstructions(category)}
                    CRITICAL: Ignore conversational wrapper prose, greetings, intro lines (such as "Here you have 10...", "Sure! Here are..."), horizontal dividers, or sign-offs ("Let me know if you need more"). NEVER create a card out of intro/outro text.
                    Return ONLY a valid JSON object matching this schema (no markdown or extra prose):
                    {"items":[{"title":"string","subtitle":"string","description":"string","iconEmoji":"📜","tags":["tag1","tag2"]}]}
                """.trimIndent()
                val userPrompt = "${AiPromptTemplates.worldItemPrompt(category)}\n\nCampaign: ${story.title} (${story.systemName})\n\nInput:\n$rawText"
                val fallback = if (provider == AiProvider.OPENROUTER) {
                    AiModelDefaults.DEFAULT_OPENROUTER_MODEL
                } else {
                    AiModelDefaults.DEFAULT_UNOROUTER_MODEL
                }
                val resolved = modelName.trim().ifBlank { fallback }
                val raw = UnorouterClient.sendChatCompletion(
                    model = resolved,
                    messages = listOf("system" to sys, "user" to userPrompt),
                    provider = provider,
                    endpointUrl = endpointUrl,
                    apiKeyOverride = apiKeyOverride,
                    temperature = 0.2
                ).getOrThrow()
                UnorouterClient.extractJsonObject(raw)
            }
            AiProvider.GEMINI -> {
                val resolved = modelName.trim().ifBlank { AiModelDefaults.DEFAULT_GEMINI_MODEL }
                val model = Firebase.ai.generativeModel(
                    modelName = resolved,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = worldItemSchema
                    }
                )
                val prompt = "${AiPromptTemplates.worldItemPrompt(category)}\n\nCampaign: ${story.title} (${story.systemName})\n\nInput:\n$rawText"
                val response = withTimeout(60_000L) { model.generateContent(prompt) }
                response.text ?: throw IllegalStateException("Empty AI response.")
            }
        }

        val root = JSONObject(jsonText)
        val arr = root.getJSONArray("items")
        val parsed = (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val title = o.optString("title", "Untitled").trim().ifBlank { "Untitled" }
            if (CharacterParserHelpers.isConversationalFillerTitle(title)) return@mapNotNull null
            val subtitle = o.optString("subtitle", category.replaceFirstChar { it.uppercase() })
            val desc = o.optString("description", "")
            val fallbackEmoji = when (category) {
                "location" -> "🏰"
                "power" -> "✨"
                "item" -> "⚔️"
                else -> "📜"
            }
            val emoji = o.optString("iconEmoji")
                .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                ?: EmojiHelper.suggestEmoji("$title $subtitle $desc", fallbackEmoji)
            val tagsArr = o.optJSONArray("tags")
            val tags = if (tagsArr != null) {
                (0 until tagsArr.length()).mapNotNull { idx -> tagsArr.optString(idx).takeIf { it.isNotBlank() } }
            } else emptyList()

            WorldItem(
                storyId = story.id,
                gmUserId = story.gmUserId,
                ownerId = ownerId,
                memberUids = story.memberUids,
                category = category,
                title = title,
                subtitle = subtitle,
                description = desc,
                iconName = when (category) {
                    "location" -> "Place"
                    "power" -> "AutoAwesome"
                    "item" -> "Build"
                    else -> "Description"
                },
                iconEmoji = emoji,
                sortOrder = (startOrder + i).toLong(),
                tagsJson = JsonCodecs.encodeStringList(tags),
            )
        }
        Result.success(parsed)
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI world item parse timed out", e)
        Result.failure(IllegalStateException("AI parsing timed out after 60 seconds. Please try again."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "AI world item parse failed", e)
        Result.failure(e)
    }

    suspend fun parseEquipmentWithAi(
        rawText: String,
        provider: AiProvider,
        modelName: String,
        endpointUrl: String = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT,
        apiKeyOverride: String = "",
    ): Result<Map<String, EquipmentSlotItem>> = try {
        val allowedSlots = EquipmentSlot.entries.joinToString(", ") { it.key }
        val jsonText = when (provider) {
            AiProvider.OPENROUTER, AiProvider.UNOROUTER -> {
                val sys = """
                    You are a strict JSON parser for a tabletop RPG app.
                    Allowed slotKey symbols: $allowedSlots.
                    Return ONLY a valid JSON object matching this schema (no markdown or extra prose):
                    {"slots":[{"slotKey":"chest","itemName":"string","bonus":"+1 AC","notes":"string","emoji":"🦺"}]}
                """.trimIndent()
                val userPrompt = "${AiPromptTemplates.equipmentPrompt()}\n\nEquipment description:\n$rawText"
                val fallback = if (provider == AiProvider.OPENROUTER) {
                    AiModelDefaults.DEFAULT_OPENROUTER_MODEL
                } else {
                    AiModelDefaults.DEFAULT_UNOROUTER_MODEL
                }
                val resolved = modelName.trim().ifBlank { fallback }
                val raw = UnorouterClient.sendChatCompletion(
                    model = resolved,
                    messages = listOf("system" to sys, "user" to userPrompt),
                    provider = provider,
                    endpointUrl = endpointUrl,
                    apiKeyOverride = apiKeyOverride,
                    temperature = 0.2
                ).getOrThrow()
                UnorouterClient.extractJsonObject(raw)
            }
            AiProvider.GEMINI -> {
                val resolved = modelName.trim().ifBlank { AiModelDefaults.DEFAULT_GEMINI_MODEL }
                val model = Firebase.ai.generativeModel(
                    modelName = resolved,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = equipmentSchema
                    }
                )
                val prompt = "${AiPromptTemplates.equipmentPrompt()}\n\nEquipment description:\n$rawText"
                val response = withTimeout(60_000L) { model.generateContent(prompt) }
                response.text ?: throw IllegalStateException("Empty AI response.")
            }
        }

        val root = JSONObject(jsonText)
        val arr = root.getJSONArray("slots")
        val result = mutableMapOf<String, EquipmentSlotItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val key = o.optString("slotKey")
            val rawName = o.optString("itemName").trim()
            val cleanName = EmojiHelper.stripLeadingEmoji(rawName).ifBlank { rawName }
            val slotDefaultEmoji = EquipmentSlot.entries.firstOrNull { it.key == key }?.emoji ?: "🎒"
            val emoji = o.optString("emoji").trim().takeIf { it.isNotBlank() }
                ?: EmojiHelper.extractLeadingEmoji(rawName)
                ?: EmojiHelper.suggestEmoji(cleanName, slotDefaultEmoji)
            if (key.isNotBlank() && cleanName.isNotBlank()) {
                result[key] = EquipmentSlotItem(
                    name = cleanName,
                    bonus = o.optString("bonus", ""),
                    notes = o.optString("notes", ""),
                    emoji = emoji,
                )
            }
        }
        Result.success(result)
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI equipment parse timed out", e)
        Result.failure(IllegalStateException("AI parsing timed out after 60 seconds. Please try again."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "AI equipment parse failed", e)
        Result.failure(e)
    }

    // Fast offline Regex parser that aligns with Story Weaver's parser symbols
    fun parseCharactersRegex(
        rawText: String,
        kind: String,
        story: Story,
        ownerId: String,
    ): List<Character> {
        val blocks = rawText.trim()
            .split(Regex("""\n\s*(?:\n+|[-*_]{3,}\s*\n+)"""))
            .map { CharacterParserHelpers.stripLeadingFillerLines(it) }
            .filter { it.isNotBlank() && !CharacterParserHelpers.isConversationalFillerBlock(it) }
        return blocks.map { block ->
            CharacterParserHelpers.parseSingleCharacterBlockRegex(block, kind, story, ownerId)
        }
    }

    fun parseCharactersBundleRegex(
        rawText: String,
        kind: String,
        story: Story,
        ownerId: String,
    ): CharacterImportBundle {
        val chars = parseCharactersRegex(rawText, kind, story, ownerId)
        return CharacterParserHelpers.buildWorldBundleFromCharacters(chars, story, ownerId)
    }

    fun parseWorldItemsRegex(
        rawText: String,
        category: String,
        story: Story,
        ownerId: String,
        startOrder: Int,
    ): List<WorldItem> {
        val blocks = rawText.trim()
            .split(Regex("""\n\s*(?:\n+|[-*_]{3,}\s*\n+)"""))
            .map { CharacterParserHelpers.stripLeadingFillerLines(it) }
            .filter { it.isNotBlank() && !CharacterParserHelpers.isConversationalFillerBlock(it) }
        val fallbackEmoji = when (category) {
            "location" -> "🏰"
            "power" -> "✨"
            "item" -> "⚔️"
            else -> "📜"
        }
        return blocks.mapIndexedNotNull { idx, block ->
            val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            val rawFirst = lines.firstOrNull()?.removePrefix("#")?.removePrefix("-")?.removePrefix("*")?.trim() ?: return@mapIndexedNotNull null
            val leadingEmoji = EmojiHelper.extractLeadingEmoji(rawFirst)
            val title = EmojiHelper.stripLeadingEmoji(rawFirst).ifBlank { rawFirst }.take(120)
            if (CharacterParserHelpers.isConversationalFillerTitle(title)) return@mapIndexedNotNull null
            val subtitleMatch = Regex("""Subtitle[:\s]+([^\n]+)""", RegexOption.IGNORE_CASE).find(block)
            val tagsMatch = Regex("""Tags?[:\s]+([^\n]+)""", RegexOption.IGNORE_CASE).find(block)
            val emojiMatch = Regex("""Emoji[:\s]+([^\s\n]+)""", RegexOption.IGNORE_CASE).find(block)

            val subtitle = subtitleMatch?.groupValues?.get(1)?.trim()
                ?: category.replaceFirstChar { it.uppercase() }
            val tags = tagsMatch?.groupValues?.get(1)
                ?.split(",")
                ?.map { it.trim().removePrefix("#") }
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val bodyLines = lines.drop(1).filterNot { line ->
                line.startsWith("Subtitle:", ignoreCase = true) ||
                    line.startsWith("Tags:", ignoreCase = true) ||
                    line.startsWith("Tag:", ignoreCase = true) ||
                    line.startsWith("Emoji:", ignoreCase = true)
            }
            val body = bodyLines.joinToString("\n").ifBlank { block }
            val iconEmoji = emojiMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
                ?: leadingEmoji
                ?: EmojiHelper.suggestEmoji(block, fallbackEmoji)

            WorldItem(
                storyId = story.id,
                gmUserId = story.gmUserId,
                ownerId = ownerId,
                memberUids = story.memberUids,
                category = category,
                title = title,
                subtitle = subtitle.take(120),
                description = body.take(10_000),
                iconName = "AutoAwesome",
                iconEmoji = iconEmoji.take(16),
                sortOrder = (startOrder + idx).toLong(),
                tagsJson = JsonCodecs.encodeStringList(tags),
            )
        }
    }

    suspend fun generateMapWithAi(
        userPrompt: String,
        story: Story,
        ownerId: String,
        currentWidth: Int,
        currentHeight: Int,
        currentGrid: Map<String, GridCell>,
        currentTokens: List<MapToken>,
        replaceEntireMap: Boolean,
        targetWidth: Int? = null,
        targetHeight: Int? = null,
        provider: AiProvider,
        modelName: String,
        endpointUrl: String = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT,
        apiKeyOverride: String = "",
    ): Result<MapPaintOutcome> = try {
        val sys = AiPromptTemplates.mapArchitectSystemPrompt(userPrompt)
        val effectiveW = (targetWidth ?: currentWidth).coerceIn(4, com.example.data.MAP_MAX_SIZE)
        val effectiveH = (targetHeight ?: currentHeight).coerceIn(4, com.example.data.MAP_MAX_SIZE)
        val modeInstruction = if (replaceEntireMap) {
            "MODE: CREATE NEW MAP (set \"clearFirst\": true). Target size: ${effectiveW}x${effectiveH} (adjust width/height within 4..${com.example.data.MAP_MAX_SIZE} only if requested)."
        } else {
            "MODE: EDIT EXISTING MAP (set \"clearFirst\": false and modify/enhance existing tiles without wiping unrelated parts). Target size: ${effectiveW}x${effectiveH}."
        }
        val mapSnapshot = MapPaintEngine.summarizeMapForPrompt(currentWidth, currentHeight, currentGrid, currentTokens)
        val fullUserPrompt = buildString {
            appendLine("Campaign: \"${story.title}\" (${story.systemName})")
            if (story.description.isNotBlank()) {
                appendLine("Setting: ${story.description}")
            }
            appendLine(modeInstruction)
            appendLine()
            appendLine(mapSnapshot)
            appendLine()
            appendLine("USER MAP REQUEST:")
            appendLine(userPrompt.trim())
        }

        val jsonText = when (provider) {
            AiProvider.OPENROUTER, AiProvider.UNOROUTER -> {
                val fallback = if (provider == AiProvider.OPENROUTER) {
                    AiModelDefaults.DEFAULT_OPENROUTER_MODEL
                } else {
                    AiModelDefaults.DEFAULT_UNOROUTER_MODEL
                }
                val resolved = modelName.trim().ifBlank { fallback }
                val raw = UnorouterClient.sendChatCompletion(
                    model = resolved,
                    messages = listOf("system" to sys, "user" to fullUserPrompt),
                    provider = provider,
                    endpointUrl = endpointUrl,
                    apiKeyOverride = apiKeyOverride,
                    temperature = 0.25,
                ).getOrThrow()
                UnorouterClient.extractJsonObject(raw)
            }
            AiProvider.GEMINI -> {
                val resolved = modelName.trim().ifBlank { AiModelDefaults.DEFAULT_GEMINI_MODEL }
                val model = Firebase.ai.generativeModel(
                    modelName = resolved,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                    }
                )
                val response = withTimeout(60_000L) {
                    model.generateContent("$sys\n\n$fullUserPrompt")
                }
                UnorouterClient.extractJsonObject(
                    response.text ?: throw IllegalStateException("Empty AI response.")
                )
            }
        }

        val script = MapPaintJsonParser.parseScript(jsonText, if (replaceEntireMap) null else currentGrid).getOrThrow()
        val finalScript = script.copy(
            clearFirst = if (replaceEntireMap) true else script.clearFirst,
            width = targetWidth ?: script.width ?: currentWidth,
            height = targetHeight ?: script.height ?: currentHeight,
        )
        val outcome = MapPaintEngine.executeScript(
            script = finalScript,
            currentWidth = currentWidth,
            currentHeight = currentHeight,
            currentGrid = currentGrid,
            currentTokens = currentTokens,
            ownerId = ownerId,
            scriptJson = jsonText,
        )
        Result.success(outcome)
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI map generation timed out", e)
        Result.failure(IllegalStateException("AI map generation timed out after 60 seconds. Please try again."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "AI map generation failed", e)
        Result.failure(e)
    }

    private companion object {
        const val TAG = "AiParserService"
    }
}
