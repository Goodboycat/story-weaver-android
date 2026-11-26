package com.example.ai

import android.util.Log
import com.example.data.Character
import com.example.data.EquipmentSlot
import com.example.data.EquipmentSlotItem
import com.example.data.InventoryEntry
import com.example.data.JsonCodecs
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
}

object AiPromptTemplates {
    fun characterPrompt(kind: String): String = """
        Convert the following notes into structured $kind entries for a tabletop RPG.
        For each entry, extract: Name, Role/Class, Level, HP, AC, Notes, Stats (STR/DEX/CON/INT/WIS/CHA), Powers, and Gear.
    """.trimIndent()

    fun worldItemPrompt(category: String): String = """
        Convert the following campaign notes into structured $category cards.
        For each card, include Title, Subtitle/Tag, and vivid usable tabletop Description.
    """.trimIndent()

    fun equipmentPrompt(): String = """
        Map the following character gear description into the 12 RPG equipment slots:
        head, eyes, neck, shoulders, chest, back, wrists, hands, waist, legs, feet, weapon_main.
    """.trimIndent()
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
                        "avatarEmoji" to Schema.string(description = "Single matching emoji"),
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
                        "iconEmoji" to Schema.string(description = "Single matching emoji"),
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
                    ),
                    optionalProperties = listOf("bonus", "notes")
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
            append("You are an expert tabletop RPG co-GM and worldbuilding assistant inside Story Weaver. ")
            if (story != null) {
                append("Active campaign: \"${story.title}\" (${story.systemName}). ")
                if (story.description.isNotBlank()) {
                    append("Premise: ${story.description}. ")
                }
            }
            if (!contextHint.isNullOrBlank()) {
                append("Current section: $contextHint. ")
            }
            append("Give vivid, structured, ready-to-play tabletop responses (stat blocks, hooks, lore, items, or rulings) that fit easily into the app's parser.")
        }

        return when (provider) {
            AiProvider.UNOROUTER -> {
                val payloadMessages = buildList {
                    add("system" to systemPrompt)
                    history.takeLast(16).forEach { msg ->
                        if (msg.role == "user" || msg.role == "assistant") {
                            add(msg.role to msg.content)
                        }
                    }
                }
                val resolvedModel = modelName.trim().ifBlank { AiModelDefaults.DEFAULT_UNOROUTER_MODEL }
                UnorouterClient.sendChatCompletion(
                    model = resolvedModel,
                    messages = payloadMessages,
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
            AiProvider.UNOROUTER -> {
                val sys = """
                    You are a strict JSON parser for a tabletop RPG app.
                    Return ONLY a valid JSON object matching this schema (no markdown or extra prose):
                    {"items":[{"name":"string","roleTitle":"string","level":1,"hpMax":12,"ac":12,"notes":"string","avatarEmoji":"🛡️","powers":["string"],"inventory":["string"]}]}
                """.trimIndent()
                val userPrompt = "${AiPromptTemplates.characterPrompt(kind)}\n\nCampaign: ${story.title} (${story.systemName})\n\nInput:\n$rawText"
                val resolved = modelName.trim().ifBlank { AiModelDefaults.DEFAULT_UNOROUTER_MODEL }
                val raw = UnorouterClient.sendChatCompletion(
                    model = resolved,
                    messages = listOf("system" to sys, "user" to userPrompt),
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
                    val pName = powersArr.optString(idx).trim()
                    if (pName.isBlank()) null else PowerEntry(
                        id = "pow_${UUID.randomUUID().toString().take(8)}",
                        name = pName,
                        emoji = EmojiHelper.suggestEmoji(pName, "✨")
                    )
                }
            } else emptyList()

            val invArr = o.optJSONArray("inventory")
            val invList = if (invArr != null) {
                (0 until invArr.length()).mapNotNull { idx ->
                    val iName = invArr.optString(idx).trim()
                    if (iName.isBlank()) null else InventoryEntry(
                        id = "inv_${UUID.randomUUID().toString().take(8)}",
                        name = iName,
                        emoji = EmojiHelper.suggestEmoji(iName, "🎒")
                    )
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
            AiProvider.UNOROUTER -> {
                val sys = """
                    You are a strict JSON parser for a tabletop RPG app.
                    Return ONLY a valid JSON object matching this schema (no markdown or extra prose):
                    {"items":[{"title":"string","subtitle":"string","description":"string","iconEmoji":"📜","tags":["tag1","tag2"]}]}
                """.trimIndent()
                val userPrompt = "${AiPromptTemplates.worldItemPrompt(category)}\n\nCampaign: ${story.title} (${story.systemName})\n\nInput:\n$rawText"
                val resolved = modelName.trim().ifBlank { AiModelDefaults.DEFAULT_UNOROUTER_MODEL }
                val raw = UnorouterClient.sendChatCompletion(
                    model = resolved,
                    messages = listOf("system" to sys, "user" to userPrompt),
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
            AiProvider.UNOROUTER -> {
                val sys = """
                    You are a strict JSON parser for a tabletop RPG app.
                    Allowed slotKey values: $allowedSlots.
                    Return ONLY a valid JSON object matching this schema (no markdown or extra prose):
                    {"slots":[{"slotKey":"chest","itemName":"string","bonus":"+1 AC","notes":"string"}]}
                """.trimIndent()
                val userPrompt = "${AiPromptTemplates.equipmentPrompt()}\n\nEquipment description:\n$rawText"
                val resolved = modelName.trim().ifBlank { AiModelDefaults.DEFAULT_UNOROUTER_MODEL }
                val raw = UnorouterClient.sendChatCompletion(
                    model = resolved,
                    messages = listOf("system" to sys, "user" to userPrompt),
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
            val name = o.optString("itemName").trim()
            if (key.isNotBlank() && name.isNotBlank()) {
                result[key] = EquipmentSlotItem(
                    name = name,
                    bonus = o.optString("bonus", ""),
                    notes = o.optString("notes", ""),
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

    // Fast offline Regex parser so users can also parse structured bullet lists without waiting
    fun parseCharactersRegex(
        rawText: String,
        kind: String,
        story: Story,
        ownerId: String,
    ): List<Character> {
        val blocks = rawText.trim().split(Regex("\\n\\s*\\n+")).filter { it.isNotBlank() }
        return blocks.map { block ->
            val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            val firstLine = lines.firstOrNull()?.removePrefix("-")?.removePrefix("*")?.trim() ?: "Hero"
            val name = firstLine.substringBefore("(").substringBefore("-").trim().ifBlank { "Hero" }
            val hpMatch = Regex("""HP[:\s]+(\d+)""", RegexOption.IGNORE_CASE).find(block)
            val acMatch = Regex("""AC[:\s]+(\d+)""", RegexOption.IGNORE_CASE).find(block)
            val lvlMatch = Regex("""(?:LVL|Level|CR)[:\s]+(\d+)""", RegexOption.IGNORE_CASE).find(block)
            val roleMatch = Regex("""(?:Class|Role|Type)[:\s]+([^\n]+)""", RegexOption.IGNORE_CASE).find(block)

            val hp = hpMatch?.groupValues?.get(1)?.toLongOrNull() ?: 15L
            val ac = acMatch?.groupValues?.get(1)?.toLongOrNull() ?: 12L
            val lvl = lvlMatch?.groupValues?.get(1)?.toLongOrNull() ?: 1L
            val role = roleMatch?.groupValues?.get(1)?.trim() ?: kind.replaceFirstChar { it.uppercase() }

            Character(
                storyId = story.id,
                gmUserId = story.gmUserId,
                ownerId = ownerId,
                memberUids = story.memberUids,
                kind = kind,
                name = name.take(100),
                avatarEmoji = EmojiHelper.suggestEmoji(block, if (kind == "monster") "👹" else "🛡️"),
                roleTitle = role.take(100),
                level = lvl,
                hpCurrent = hp,
                hpMax = hp,
                ac = ac,
                notes = block.take(5000),
                statsJson = JsonCodecs.encodeStats(JsonCodecs.defaultStats()),
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
            val title = lines.firstOrNull()?.removePrefix("#")?.removePrefix("-")?.trim()?.take(120) ?: "Entry"
            val body = lines.drop(1).joinToString("\n").ifBlank { block }
            WorldItem(
                storyId = story.id,
                gmUserId = story.gmUserId,
                ownerId = ownerId,
                memberUids = story.memberUids,
                category = category,
                title = title,
                subtitle = category.replaceFirstChar { it.uppercase() },
                description = body.take(10_000),
                iconName = "AutoAwesome",
                iconEmoji = EmojiHelper.suggestEmoji(block, fallbackEmoji),
                sortOrder = (startOrder + idx).toLong(),
            )
        }
    }

    private companion object {
        const val TAG = "AiParserService"
    }
}
