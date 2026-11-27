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
        listOf("beast", "wolf", "spider", "goblin", "orc", "monster") to "👹",
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
        appendLine("Whenever you generate characters, NPCs, monsters, locations, powers, notes, or equipment loadouts, ALWAYS include clear parser symbols and matching emojis so the user can import them directly:")
        appendLine("- Characters / NPCs / Monsters (separate multiple entries with a blank line):")
        appendLine("  <Emoji> <Name>")
        appendLine("  Class: <Role/Class/Species>")
        appendLine("  LVL: <integer> | HP: <integer> | AC: <integer>")
        appendLine("  Emoji: <single fitting Unicode emoji>")
        appendLine("  Powers: <comma-separated abilities>")
        appendLine("  Gear: <comma-separated items>")
        appendLine("  Stats: STR <n>, DEX <n>, CON <n>, INT <n>, WIS <n>, CHA <n>")
        appendLine("- Locations / Powers / Notes (separate multiple cards with a blank line):")
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
        For each entry, extract: Name, Role/Class, Level, HP, AC, Notes, matching Emoji symbol (avatarEmoji), Stats (STR/DEX/CON/INT/WIS/CHA), Powers, and Gear.
    """.trimIndent()

    fun worldItemPrompt(category: String): String = """
        Convert the following campaign notes into structured $category cards.
        For each card, include Title, Subtitle/Tag, matching Emoji symbol (iconEmoji), Tags, and vivid usable tabletop Description.
    """.trimIndent()

    fun equipmentPrompt(): String = """
        Map the following character gear description into the 12 exact RPG equipment slot keys:
        head, eyes, neck, shoulders, chest, back, wrists, hands, waist, legs, feet, weapon_main.
        Include an appropriate single Unicode emoji symbol for every equipped item.
    """.trimIndent()

    fun mapArchitectSystemPrompt(): String = buildString {
        appendLine("You are a procedural tabletop RPG Game Map Programmer inside Story Weaver.")
        appendLine("You build and edit tile maps by writing an ordered JSON program of painting tool calls — exactly like coding a 2D game map.")
        appendLine("Return ONLY a valid JSON object (no markdown fences, no commentary) with this schema:")
        appendLine(
            """
            {
              "width": 16,
              "height": 14,
              "clearFirst": true,
              "clearTokens": false,
              "summary": "Short description of the painted map or edit",
              "operations": [
                {"tool": "rect", "x1": 0, "y1": 0, "x2": 15, "y2": 13, "colorHex": "#15803D", "emoji": null, "hollow": false},
                {"tool": "rect_border", "x1": 1, "y1": 1, "x2": 14, "y2": 12, "colorHex": "#334155", "emoji": "🧱", "thickness": 1},
                {"tool": "line", "x1": 0, "y1": 6, "x2": 15, "y2": 6, "colorHex": "#0284C7", "emoji": "🌊", "brushSize": 2},
                {"tool": "circle", "cx": 8, "cy": 7, "radius": 3, "filled": true, "colorHex": "#4C1D95", "emoji": "✨"},
                {"tool": "checker", "x1": 3, "y1": 3, "x2": 6, "y2": 6, "colorHex": "#5C4033", "emoji": null, "colorHex2": "#78350F", "emoji2": null},
                {"tool": "scatter", "x1": 1, "y1": 1, "x2": 14, "y2": 12, "count": 10, "colorHex": "#14532D", "emoji": "🌲", "onlyEmpty": true},
                {"tool": "flood_fill", "x": 5, "y": 5, "colorHex": "#78350F", "emoji": null},
                {"tool": "paint", "points": [[7, 1], [8, 1]], "colorHex": "#78350F", "emoji": "🚪", "brushSize": 1},
                {"tool": "erase", "x1": 7, "y1": 1, "x2": 8, "y2": 1}
              ],
              "tokens": [
                {"label": "Goblin Scout", "emoji": "👺", "kind": "monster", "x": 5, "y": 4}
              ]
            }
            """.trimIndent()
        )
        appendLine()
        appendLine("PAINTING ENGINE RULES:")
        appendLine("- Grid bounds: width 4..30, height 4..30. Coordinates are 0-indexed: x in 0..width-1, y in 0..height-1.")
        appendLine("- Operations execute in order! Paint base terrain/floors first (e.g. rect), then walls/borders (rect_border), then rivers/roads/corridors (line), then cutouts/doorways (erase or paint), then details/furniture/trees/traps/treasures (paint, circle, scatter, checker).")
        appendLine("- Composite blocks: every tile has an optional background block colorHex (#RRGGBB or null) AND an optional foreground Unicode emoji on top.")
        appendLine("- Use clearFirst=true when creating a brand-new map; use clearFirst=false when editing/adding to an existing map.")
        appendLine("- Keep tokens array empty [] unless the user explicitly asks to place characters/monsters/NPCs on the map.")
        appendLine()
        appendLine(MapColorPalette.palettePromptGuide())
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
                        "level" to Schema.integer(description = "Level or challenge rating"),
                        "hpMax" to Schema.integer(description = "Maximum hit points"),
                        "ac" to Schema.integer(description = "Armor class"),
                        "notes" to Schema.string(description = "Concise tabletop notes and lore"),
                        "avatarEmoji" to Schema.string(description = "Single matching Unicode emoji symbol"),
                        "powers" to Schema.array(items = Schema.string(), description = "List of key abilities"),
                        "inventory" to Schema.array(items = Schema.string(), description = "Key carried items"),
                    ),
                    optionalProperties = listOf("avatarEmoji", "powers", "inventory")
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
                    Return ONLY a valid JSON object matching this schema (no markdown or extra prose):
                    {"items":[{"name":"string","roleTitle":"string","level":1,"hpMax":12,"ac":12,"notes":"string","avatarEmoji":"🛡️","powers":["string"],"inventory":["string"]}]}
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
        val parsed = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val name = o.optString("name", "Unnamed").trim().ifBlank { "Unnamed" }
            val role = o.optString("roleTitle", kind.replaceFirstChar { it.uppercase() })
            val level = o.optInt("level", 1).coerceIn(1, 999).toLong()
            val hp = o.optInt("hpMax", 12).coerceIn(1, 9999).toLong()
            val ac = o.optInt("ac", 12).coerceIn(1, 99).toLong()
            val notes = o.optString("notes", "")
            val emoji = o.optString("avatarEmoji").takeIf { it.isNotBlank() }
                ?: EmojiHelper.suggestEmoji("$name $role $notes", if (kind == "monster") "👹" else "🛡️")

            val powersArr = o.optJSONArray("powers")
            val powersList = if (powersArr != null) {
                (0 until powersArr.length()).mapNotNull { idx ->
                    val pRaw = powersArr.optString(idx).trim()
                    if (pRaw.isBlank()) null else {
                        val pEmoji = EmojiHelper.extractLeadingEmoji(pRaw) ?: EmojiHelper.suggestEmoji(pRaw, "✨")
                        PowerEntry(
                            id = "pow_${UUID.randomUUID().toString().take(8)}",
                            name = EmojiHelper.stripLeadingEmoji(pRaw).ifBlank { pRaw },
                            emoji = pEmoji
                        )
                    }
                }
            } else emptyList()

            val invArr = o.optJSONArray("inventory")
            val invList = if (invArr != null) {
                (0 until invArr.length()).mapNotNull { idx ->
                    val iRaw = invArr.optString(idx).trim()
                    if (iRaw.isBlank()) null else {
                        val iEmoji = EmojiHelper.extractLeadingEmoji(iRaw) ?: EmojiHelper.suggestEmoji(iRaw, "🎒")
                        InventoryEntry(
                            id = "inv_${UUID.randomUUID().toString().take(8)}",
                            name = EmojiHelper.stripLeadingEmoji(iRaw).ifBlank { iRaw },
                            emoji = iEmoji
                        )
                    }
                }
            } else emptyList()

            Character(
                storyId = story.id,
                gmUserId = story.gmUserId,
                ownerId = ownerId,
                memberUids = story.memberUids,
                kind = kind,
                name = name,
                avatarEmoji = emoji,
                roleTitle = role,
                level = level,
                hpCurrent = hp,
                hpMax = hp,
                ac = ac,
                notes = notes,
                powersJson = JsonCodecs.encodePowers(powersList),
                inventoryJson = JsonCodecs.encodeInventory(invList),
                statsJson = JsonCodecs.encodeStats(JsonCodecs.defaultStats()),
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
        val parsed = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val title = o.optString("title", "Untitled").trim().ifBlank { "Untitled" }
            val subtitle = o.optString("subtitle", category.replaceFirstChar { it.uppercase() })
            val desc = o.optString("description", "")
            val fallbackEmoji = when (category) {
                "location" -> "🏰"
                "power" -> "✨"
                else -> "📜"
            }
            val emoji = o.optString("iconEmoji").takeIf { it.isNotBlank() }
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
        val blocks = rawText.trim().split(Regex("\\n\\s*\\n+")).filter { it.isNotBlank() }
        return blocks.map { block ->
            val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            val firstLineRaw = lines.firstOrNull()?.removePrefix("#")?.removePrefix("-")?.removePrefix("*")?.trim() ?: "Hero"
            val leadingEmoji = EmojiHelper.extractLeadingEmoji(firstLineRaw)
            val firstLine = EmojiHelper.stripLeadingEmoji(firstLineRaw)
            val name = firstLine.substringBefore("(").substringBefore(" - ").trim().ifBlank { "Hero" }
            val hpMatch = Regex("""HP[:\s]+(\d+)""", RegexOption.IGNORE_CASE).find(block)
            val acMatch = Regex("""AC[:\s]+(\d+)""", RegexOption.IGNORE_CASE).find(block)
            val lvlMatch = Regex("""(?:LVL|Level|CR)[:\s]+(\d+)""", RegexOption.IGNORE_CASE).find(block)
            val roleMatch = Regex("""(?:Class|Role|Type)[:\s]+([^\n|]+)""", RegexOption.IGNORE_CASE).find(block)
            val emojiMatch = Regex("""Emoji[:\s]+([^\s\n]+)""", RegexOption.IGNORE_CASE).find(block)
            val powersMatch = Regex("""Powers?[:\s]+([^\n]+)""", RegexOption.IGNORE_CASE).find(block)
            val gearMatch = Regex("""(?:Gear|Inventory)[:\s]+([^\n]+)""", RegexOption.IGNORE_CASE).find(block)
            val statsMatch = Regex("""Stats[:\s]+([^\n]+)""", RegexOption.IGNORE_CASE).find(block)

            val hp = hpMatch?.groupValues?.get(1)?.toLongOrNull() ?: 15L
            val ac = acMatch?.groupValues?.get(1)?.toLongOrNull() ?: 12L
            val lvl = lvlMatch?.groupValues?.get(1)?.toLongOrNull() ?: 1L
            val role = roleMatch?.groupValues?.get(1)?.trim() ?: kind.replaceFirstChar { it.uppercase() }
            val emoji = emojiMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
                ?: leadingEmoji
                ?: EmojiHelper.suggestEmoji(block, if (kind == "monster") "👹" else "🛡️")

            val powersList = powersMatch?.groupValues?.get(1)
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.map { p ->
                    PowerEntry(
                        id = "pow_${UUID.randomUUID().toString().take(8)}",
                        name = EmojiHelper.stripLeadingEmoji(p).ifBlank { p },
                        emoji = EmojiHelper.extractLeadingEmoji(p) ?: EmojiHelper.suggestEmoji(p, "✨")
                    )
                }
                ?: emptyList()

            val invList = gearMatch?.groupValues?.get(1)
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.map { item ->
                    InventoryEntry(
                        id = "inv_${UUID.randomUUID().toString().take(8)}",
                        name = EmojiHelper.stripLeadingEmoji(item).ifBlank { item },
                        emoji = EmojiHelper.extractLeadingEmoji(item) ?: EmojiHelper.suggestEmoji(item, "🎒")
                    )
                }
                ?: emptyList()

            val parsedStats = JsonCodecs.defaultStats().toMutableMap()
            statsMatch?.groupValues?.get(1)?.let { statsLine ->
                Regex("""([A-Za-z]{2,8})[:\s]+(-?\d+)""").findAll(statsLine).forEach { m ->
                    parsedStats[m.groupValues[1].uppercase()] = m.groupValues[2]
                }
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
                level = lvl,
                hpCurrent = hp,
                hpMax = hp,
                ac = ac,
                notes = block.take(5000),
                powersJson = JsonCodecs.encodePowers(powersList),
                inventoryJson = JsonCodecs.encodeInventory(invList),
                statsJson = JsonCodecs.encodeStats(parsedStats),
            )
        }
    }

    fun parseWorldItemsRegex(
        rawText: String,
        category: String,
        story: Story,
        ownerId: String,
        startOrder: Int,
    ): List<WorldItem> {
        val blocks = rawText.trim().split(Regex("\\n\\s*\\n+")).filter { it.isNotBlank() }
        val fallbackEmoji = when (category) {
            "location" -> "🏰"
            "power" -> "✨"
            else -> "📜"
        }
        return blocks.mapIndexed { idx, block ->
            val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            val rawFirst = lines.firstOrNull()?.removePrefix("#")?.removePrefix("-")?.removePrefix("*")?.trim() ?: "Entry"
            val leadingEmoji = EmojiHelper.extractLeadingEmoji(rawFirst)
            val title = EmojiHelper.stripLeadingEmoji(rawFirst).ifBlank { rawFirst }.take(120)
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
        val sys = AiPromptTemplates.mapArchitectSystemPrompt()
        val effectiveW = (targetWidth ?: currentWidth).coerceIn(4, 30)
        val effectiveH = (targetHeight ?: currentHeight).coerceIn(4, 30)
        val modeInstruction = if (replaceEntireMap) {
            "MODE: CREATE NEW MAP (set \"clearFirst\": true). Target size: ${effectiveW}x${effectiveH} (adjust width/height within 4..30 only if requested)."
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

        val script = MapPaintJsonParser.parseScript(jsonText).getOrThrow()
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
