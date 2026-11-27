package com.example.ai

import android.util.Log
import com.example.data.Character
import com.example.data.EquipmentSlot
import com.example.data.EquipmentSlotItem
import com.example.data.InventoryEntry
import com.example.data.JsonCodecs
import com.example.data.MissionEntry
import com.example.data.PowerEntry
import com.example.data.Story
import com.example.data.WorldItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class CharacterSheetAiTab(
    val label: String,
    val dialogTitle: String,
    val createPlaceholder: String,
    val editPlaceholder: String,
) {
    INFO(
        label = "Info",
        dialogTitle = "AI Character Info",
        createPlaceholder = "Example: Level 5 Elven Ranger named Lyra in Silverwood, HP 38, AC 15...",
        editPlaceholder = "Example: Level up to 6 (+8 Max HP), move location to Shadowfen, and update role to Shadow Stalker...",
    ),
    NOTES(
        label = "Notes",
        dialogTitle = "AI Character Notes",
        createPlaceholder = "Example: Write a dramatic backstory as an exiled paladin seeking a lost relic...",
        editPlaceholder = "Example: Rewrite in bullet points and add a new session twist about meeting the Dragon King...",
    ),
    INVENTORY(
        label = "Inventory",
        dialogTitle = "AI Inventory",
        createPlaceholder = "Example: 2 Healing Potions, Rope (50 ft), Silver Dagger, Thieves' Tools...",
        editPlaceholder = "Example: Use 1 Healing Potion, upgrade Silver Dagger to Runic Dagger, and add Torch x3...",
    ),
    MISSIONS(
        label = "Missions",
        dialogTitle = "AI Missions & Quests",
        createPlaceholder = "Example: Find the Sunstone in Sunken Crypt; Escort Merchant Guild to Ironhold...",
        editPlaceholder = "Example: Mark the escort quest as completed and add a new quest to confront Lord Malakor...",
    ),
    POWERS(
        label = "Powers",
        dialogTitle = "AI Powers & Abilities",
        createPlaceholder = "Example: Arcane Bolt (2 Mana) - Fires a bolt of lightning [#magic, #lightning]...",
        editPlaceholder = "Example: Upgrade Arcane Bolt to 3 Mana with chain lightning damage and add #storm tag...",
    ),
    GEAR_AND_STATS(
        label = "Gear & Stats",
        dialogTitle = "AI Gear & Stats",
        createPlaceholder = "Example: Mithril chainmail (+2 AC), shadow cloak, flaming longsword; STR 16, DEX 14...",
        editPlaceholder = "Example: Replace chest armor with Dragonscale Plate (+3 AC), remove cloak, and set STR to 18...",
    );

    fun promptTemplate(): String = when (this) {
        INFO -> "Update or create character info: Name, Class/Role, Location, Level, HP, Max HP, AC, and matching Emoji."
        NOTES -> "Write or edit backstory, personality traits, bonds, flaws, and session notes for this character."
        INVENTORY -> "List carried items, quantities, notes, and emojis (e.g. Healing Potion x2 - Restores 2d4+2 HP)."
        MISSIONS -> "List quests/objectives with status (active or completed) and description."
        POWERS -> "List abilities/spells as: Name (Cost) - Effect Description [#tag1, #tag2]."
        GEAR_AND_STATS -> AiPromptTemplates.equipmentPrompt() + "\nAlso include any stats (STR, DEX, CON, INT, WIS, CHA, etc.)."
    }
}

object AiEditAssistService {
    private const val TAG = "AiEditAssistService"

    private suspend fun requestJsonObject(
        systemPrompt: String,
        userPrompt: String,
        provider: AiProvider,
        modelName: String,
        endpointUrl: String,
        apiKeyOverride: String,
    ): String {
        return when (provider) {
            AiProvider.OPENROUTER, AiProvider.UNOROUTER -> {
                val fallback = if (provider == AiProvider.OPENROUTER) {
                    AiModelDefaults.DEFAULT_OPENROUTER_MODEL
                } else {
                    AiModelDefaults.DEFAULT_UNOROUTER_MODEL
                }
                val resolved = modelName.trim().ifBlank { fallback }
                val raw = UnorouterClient.sendChatCompletion(
                    model = resolved,
                    messages = listOf("system" to systemPrompt, "user" to userPrompt),
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
                    model.generateContent("$systemPrompt\n\n$userPrompt")
                }
                UnorouterClient.extractJsonObject(
                    response.text ?: throw IllegalStateException("Empty AI response.")
                )
            }
        }
    }

    private fun serializeCharacterForPrompt(c: Character): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("roleTitle", c.roleTitle)
        put("locationName", c.locationName)
        put("level", c.level)
        put("hpCurrent", c.hpCurrent)
        put("hpMax", c.hpMax)
        put("ac", c.ac)
        put("avatarEmoji", c.avatarEmoji)
        put("notes", c.notes.take(2000))
        put("stats", JSONObject(JsonCodecs.decodeStats(c.statsJson)))
        put("powers", JSONArray().apply {
            JsonCodecs.decodePowers(c.powersJson).forEach { p ->
                put(JSONObject().apply {
                    put("name", p.name)
                    put("cost", p.cost)
                    put("description", p.description)
                    put("emoji", p.emoji)
                    put("tags", JSONArray(p.tags))
                })
            }
        })
        put("missions", JSONArray().apply {
            JsonCodecs.decodeMissions(c.missionsJson).forEach { m ->
                put(JSONObject().apply {
                    put("title", m.title)
                    put("status", m.status)
                    put("description", m.description)
                })
            }
        })
        put("inventory", JSONArray().apply {
            JsonCodecs.decodeInventory(c.inventoryJson).forEach { inv ->
                put(JSONObject().apply {
                    put("name", inv.name)
                    put("quantity", inv.quantity)
                    put("notes", inv.notes)
                    put("emoji", inv.emoji)
                })
            }
        })
        put("equipment", JSONArray().apply {
            JsonCodecs.decodeEquipment(c.equipmentJson).forEach { (slotKey, item) ->
                put(JSONObject().apply {
                    put("slotKey", slotKey)
                    put("itemName", item.name)
                    put("bonus", item.bonus)
                    put("notes", item.notes)
                    put("emoji", item.emoji)
                })
            }
        })
    }

    suspend fun editCharactersWithAi(
        targets: List<Character>,
        instruction: String,
        story: Story,
        provider: AiProvider,
        modelName: String,
        endpointUrl: String,
        apiKeyOverride: String,
    ): Result<List<Character>> = try {
        if (targets.isEmpty()) {
            return Result.failure(IllegalArgumentException("Select at least one unit to edit."))
        }
        val existingArr = JSONArray().apply {
            targets.forEach { put(serializeCharacterForPrompt(it)) }
        }
        val sys = """
            You are a tabletop RPG campaign editor inside Story Weaver.
            You are editing ALREADY EXISTING character/NPC/monster sheets.
            Return ONLY a valid JSON object matching this schema (no markdown fences or commentary):
            {"items":[{"id":"existing_id","name":"string","roleTitle":"string","locationName":"string","level":1,"hpCurrent":12,"hpMax":12,"ac":12,"avatarEmoji":"🛡️","notes":"string","stats":{"STR":"10","DEX":"10","CON":"10","INT":"10","WIS":"10","CHA":"10"},"powers":[{"name":"string","cost":"string","description":"string","emoji":"✨","tags":["tag1"]}],"missions":[{"title":"string","status":"active","description":"string"}],"inventory":[{"name":"string","quantity":1,"notes":"string","emoji":"🎒"}],"equipment":[{"slotKey":"weapon_main","itemName":"string","bonus":"+1","notes":"string","emoji":"⚔️"}]}]}
            CRITICAL RULES:
            - Preserve the exact "id" of each edited unit.
            - Keep all existing fields, powers, missions, inventory, equipment, and stats intact UNLESS the user's edit instruction explicitly modifies, adds, or removes them.
            - Allowed slotKey values: ${EquipmentSlot.entries.joinToString(", ") { it.key }}.
        """.trimIndent()
        val userPrompt = """
            Campaign: "${story.title}" (${story.systemName})
            Existing units JSON:
            $existingArr

            Edit instruction:
            ${instruction.trim()}
        """.trimIndent()

        val jsonText = requestJsonObject(sys, userPrompt, provider, modelName, endpointUrl, apiKeyOverride)
        val root = JSONObject(jsonText)
        val arr = root.getJSONArray("items")
        val targetById = targets.associateBy { it.id }
        val updatedList = mutableListOf<Character>()

        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val returnedId = o.optString("id").trim()
            val base = targetById[returnedId]
                ?: targets.firstOrNull { it.name.equals(o.optString("name").trim(), ignoreCase = true) }
                ?: if (targets.size == 1) targets.first() else null
                ?: continue

            val nextName = o.optString("name").trim().ifBlank { base.name }.take(100)
            val nextRole = if (o.has("roleTitle")) o.optString("roleTitle", base.roleTitle).trim().take(100) else base.roleTitle
            val nextLoc = if (o.has("locationName")) {
                o.optString("locationName", base.locationName).trim().takeIf { !it.equals("null", true) }?.take(120) ?: ""
            } else base.locationName
            val nextLvl = if (o.has("level")) o.optInt("level", base.level.toInt()).coerceIn(0, 999).toLong() else base.level
            val nextHpMax = if (o.has("hpMax")) o.optInt("hpMax", base.hpMax.toInt()).coerceIn(0, 99999).toLong() else base.hpMax
            val nextHpCur = if (o.has("hpCurrent")) o.optInt("hpCurrent", nextHpMax.toInt()).coerceIn(-999, 99999).toLong() else nextHpMax
            val nextAc = if (o.has("ac")) o.optInt("ac", base.ac.toInt()).coerceIn(0, 999).toLong() else base.ac
            val nextEmoji = o.optString("avatarEmoji")
                .takeIf { it.isNotBlank() && !it.equals("null", true) }
                ?.take(16) ?: base.avatarEmoji
            val nextNotes = if (o.has("notes")) o.optString("notes", base.notes).take(20000) else base.notes

            val nextStats = parseStatsFromJson(o.opt("stats"), JsonCodecs.decodeStats(base.statsJson))
            val nextPowers = if (o.has("powers")) parsePowersFromJsonArray(o.optJSONArray("powers"), nextName) else JsonCodecs.decodePowers(base.powersJson)
            val nextMissions = if (o.has("missions")) parseMissionsFromJsonArray(o.optJSONArray("missions")) else JsonCodecs.decodeMissions(base.missionsJson)
            val nextInventory = if (o.has("inventory")) parseInventoryFromJsonArray(o.optJSONArray("inventory")) else JsonCodecs.decodeInventory(base.inventoryJson)
            val baseEquip = JsonCodecs.decodeEquipment(base.equipmentJson)
            val nextEquipment = if (o.has("equipment")) {
                parseEquipmentFromJson(o.opt("equipment"), preserveImagesFrom = baseEquip)
            } else baseEquip

            updatedList += base.copy(
                name = nextName,
                roleTitle = nextRole,
                locationName = nextLoc,
                level = nextLvl,
                hpCurrent = nextHpCur,
                hpMax = nextHpMax,
                ac = nextAc,
                avatarEmoji = nextEmoji,
                notes = nextNotes,
                statsJson = JsonCodecs.encodeStats(nextStats),
                powersJson = JsonCodecs.encodePowers(nextPowers),
                missionsJson = JsonCodecs.encodeMissions(nextMissions),
                inventoryJson = JsonCodecs.encodeInventory(nextInventory),
                equipmentJson = JsonCodecs.encodeEquipment(nextEquipment),
            )
        }

        if (updatedList.isEmpty()) {
            Result.failure(IllegalStateException("AI did not return updates for the selected unit(s)."))
        } else {
            Result.success(updatedList.distinctBy { it.id })
        }
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI character edit timed out", e)
        Result.failure(IllegalStateException("AI edit timed out after 60 seconds. Please try again."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "AI character edit failed", e)
        Result.failure(e)
    }

    suspend fun editWorldItemsWithAi(
        targets: List<WorldItem>,
        category: String,
        instruction: String,
        story: Story,
        provider: AiProvider,
        modelName: String,
        endpointUrl: String,
        apiKeyOverride: String,
    ): Result<List<WorldItem>> = try {
        if (targets.isEmpty()) {
            return Result.failure(IllegalArgumentException("Select at least one $category card to edit."))
        }
        val existingArr = JSONArray().apply {
            targets.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("subtitle", item.subtitle)
                    put("description", item.description)
                    put("iconEmoji", item.iconEmoji)
                    put("tags", JSONArray(JsonCodecs.decodeStringList(item.tagsJson)))
                })
            }
        }
        val sys = """
            You are a tabletop RPG campaign editor inside Story Weaver.
            You are editing ALREADY EXISTING $category cards.
            Return ONLY a valid JSON object matching this schema (no markdown fences or commentary):
            {"items":[{"id":"existing_id","title":"string","subtitle":"string","description":"string","iconEmoji":"📜","tags":["tag1","tag2"]}]}
            CRITICAL RULES:
            - Preserve the exact "id" of each edited card.
            - Keep existing details intact unless the user's edit instruction asks to change, extend, or refine them.
        """.trimIndent()
        val userPrompt = """
            Campaign: "${story.title}" (${story.systemName})
            Category: $category
            Existing cards JSON:
            $existingArr

            Edit instruction:
            ${instruction.trim()}
        """.trimIndent()

        val jsonText = requestJsonObject(sys, userPrompt, provider, modelName, endpointUrl, apiKeyOverride)
        val root = JSONObject(jsonText)
        val arr = root.getJSONArray("items")
        val targetById = targets.associateBy { it.id }
        val updatedList = mutableListOf<WorldItem>()

        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val returnedId = o.optString("id").trim()
            val base = targetById[returnedId]
                ?: targets.firstOrNull { it.title.equals(o.optString("title").trim(), ignoreCase = true) }
                ?: if (targets.size == 1) targets.first() else null
                ?: continue

            val nextTitle = o.optString("title").trim().ifBlank { base.title }.take(120)
            val nextSub = if (o.has("subtitle")) o.optString("subtitle", base.subtitle).trim().take(120) else base.subtitle
            val nextDesc = if (o.has("description")) o.optString("description", base.description).trim().take(10000) else base.description
            val nextEmoji = o.optString("iconEmoji")
                .takeIf { it.isNotBlank() && !it.equals("null", true) }
                ?.take(16) ?: base.iconEmoji
            val tagsArr = o.optJSONArray("tags")
            val nextTags = if (tagsArr != null) {
                (0 until tagsArr.length()).mapNotNull { idx ->
                    tagsArr.optString(idx).trim().removePrefix("#").takeIf { it.isNotBlank() && !it.equals("null", true) }
                }
            } else {
                JsonCodecs.decodeStringList(base.tagsJson)
            }

            updatedList += base.copy(
                title = nextTitle,
                subtitle = nextSub,
                description = nextDesc,
                iconEmoji = nextEmoji,
                tagsJson = JsonCodecs.encodeStringList(nextTags),
            )
        }

        if (updatedList.isEmpty()) {
            Result.failure(IllegalStateException("AI did not return updates for the selected $category card(s)."))
        } else {
            Result.success(updatedList.distinctBy { it.id })
        }
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI world item edit timed out", e)
        Result.failure(IllegalStateException("AI edit timed out after 60 seconds. Please try again."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "AI world item edit failed", e)
        Result.failure(e)
    }

    suspend fun assistCharacterTabWithAi(
        current: Character,
        tab: CharacterSheetAiTab,
        prompt: String,
        isEditExisting: Boolean,
        story: Story?,
        provider: AiProvider,
        modelName: String,
        endpointUrl: String,
        apiKeyOverride: String,
    ): Result<Character> = try {
        val campaignHeader = if (story != null) "Campaign: \"${story.title}\" (${story.systemName})" else "Tabletop RPG Character"
        val charSnapshot = serializeCharacterForPrompt(current)

        when (tab) {
            CharacterSheetAiTab.INFO -> {
                val sys = """
                    You are a tabletop RPG character sheet assistant.
                    Return ONLY a valid JSON object (no markdown fences):
                    {"name":"string","roleTitle":"string","locationName":"string","level":1,"hpCurrent":10,"hpMax":10,"ac":10,"avatarEmoji":"🛡️"}
                    ${if (isEditExisting) "Edit the character's Info fields per the user's request while keeping unmentioned values unchanged." else "Create/populate the character's Info fields from the user's prompt."}
                """.trimIndent()
                val userPrompt = "$campaignHeader\nCurrent Character:\n$charSnapshot\n\nRequest:\n${prompt.trim()}"
                val o = JSONObject(requestJsonObject(sys, userPrompt, provider, modelName, endpointUrl, apiKeyOverride))
                val nextHpMax = if (o.has("hpMax")) o.optInt("hpMax", current.hpMax.toInt()).coerceIn(0, 99999).toLong() else current.hpMax
                val nextHpCur = if (o.has("hpCurrent")) o.optInt("hpCurrent", nextHpMax.toInt()).coerceIn(-999, 99999).toLong() else nextHpMax
                Result.success(
                    current.copy(
                        name = o.optString("name").trim().ifBlank { current.name }.take(100),
                        roleTitle = if (o.has("roleTitle")) o.optString("roleTitle", current.roleTitle).trim().take(100) else current.roleTitle,
                        locationName = if (o.has("locationName")) o.optString("locationName", current.locationName).trim().takeIf { !it.equals("null", true) }?.take(120) ?: "" else current.locationName,
                        level = if (o.has("level")) o.optInt("level", current.level.toInt()).coerceIn(0, 999).toLong() else current.level,
                        hpCurrent = nextHpCur,
                        hpMax = nextHpMax,
                        ac = if (o.has("ac")) o.optInt("ac", current.ac.toInt()).coerceIn(0, 999).toLong() else current.ac,
                        avatarEmoji = o.optString("avatarEmoji").takeIf { it.isNotBlank() && !it.equals("null", true) }?.take(16) ?: current.avatarEmoji,
                    )
                )
            }

            CharacterSheetAiTab.NOTES -> {
                val sys = """
                    You are a tabletop RPG character writer.
                    Return ONLY a valid JSON object (no markdown fences):
                    {"notes":"string"}
                    ${if (isEditExisting) "Edit, extend, or rewrite the existing notes according to the instruction and return the complete updated notes text." else "Write rich backstory, personality traits, or session notes based on the prompt."}
                """.trimIndent()
                val userPrompt = "$campaignHeader\nCharacter: ${current.name} (${current.roleTitle}, Lvl ${current.level})\nExisting Notes:\n${current.notes}\n\nRequest:\n${prompt.trim()}"
                val o = JSONObject(requestJsonObject(sys, userPrompt, provider, modelName, endpointUrl, apiKeyOverride))
                val generatedNotes = o.optString("notes").trim()
                if (generatedNotes.isBlank()) {
                    Result.failure(IllegalStateException("AI returned empty notes."))
                } else {
                    val finalNotes = if (!isEditExisting && current.notes.isNotBlank()) {
                        "${current.notes.trim()}\n\n$generatedNotes"
                    } else {
                        generatedNotes
                    }
                    Result.success(current.copy(notes = finalNotes.take(20000)))
                }
            }

            CharacterSheetAiTab.INVENTORY -> {
                val existingInv = JsonCodecs.decodeInventory(current.inventoryJson)
                val sys = """
                    You are a tabletop RPG inventory assistant.
                    Return ONLY a valid JSON object (no markdown fences):
                    {"items":[{"name":"string","quantity":1,"notes":"string","emoji":"🎒"}]}
                    ${if (isEditExisting) "Return the COMPLETE updated inventory list after applying the user's edits (keep untouched items intact)." else "Return ONLY the NEW inventory items to add."}
                """.trimIndent()
                val userPrompt = "$campaignHeader\nCharacter: ${current.name} (${current.roleTitle})\nExisting Inventory:\n${charSnapshot.optJSONArray("inventory")}\n\nRequest:\n${prompt.trim()}"
                val o = JSONObject(requestJsonObject(sys, userPrompt, provider, modelName, endpointUrl, apiKeyOverride))
                val hasKey = o.has("items") || o.has("inventory")
                val parsed = parseInventoryFromJsonArray(o.optJSONArray("items") ?: o.optJSONArray("inventory"))
                val nextInv = when {
                    isEditExisting && hasKey -> parsed
                    isEditExisting -> existingInv
                    else -> existingInv + parsed
                }
                Result.success(current.copy(inventoryJson = JsonCodecs.encodeInventory(nextInv)))
            }

            CharacterSheetAiTab.MISSIONS -> {
                val existingMissions = JsonCodecs.decodeMissions(current.missionsJson)
                val sys = """
                    You are a tabletop RPG quest & mission assistant.
                    Return ONLY a valid JSON object (no markdown fences):
                    {"missions":[{"title":"string","status":"active","description":"string"}]}
                    status must be "active" or "completed".
                    ${if (isEditExisting) "Return the COMPLETE updated missions list after applying the user's edits (keep untouched missions intact)." else "Return ONLY the NEW missions/quests to add."}
                """.trimIndent()
                val userPrompt = "$campaignHeader\nCharacter: ${current.name} (${current.roleTitle})\nExisting Missions:\n${charSnapshot.optJSONArray("missions")}\n\nRequest:\n${prompt.trim()}"
                val o = JSONObject(requestJsonObject(sys, userPrompt, provider, modelName, endpointUrl, apiKeyOverride))
                val hasKey = o.has("missions") || o.has("items")
                val parsed = parseMissionsFromJsonArray(o.optJSONArray("missions") ?: o.optJSONArray("items"))
                val nextMissions = when {
                    isEditExisting && hasKey -> parsed
                    isEditExisting -> existingMissions
                    else -> existingMissions + parsed
                }
                Result.success(current.copy(missionsJson = JsonCodecs.encodeMissions(nextMissions)))
            }

            CharacterSheetAiTab.POWERS -> {
                val existingPowers = JsonCodecs.decodePowers(current.powersJson)
                val sys = """
                    You are a tabletop RPG abilities & spells designer.
                    Return ONLY a valid JSON object (no markdown fences):
                    {"powers":[{"name":"string","cost":"string","description":"string","emoji":"✨","tags":["tag1","tag2"]}]}
                    ${if (isEditExisting) "Return the COMPLETE updated powers list after applying the user's edits (keep untouched powers intact)." else "Return ONLY the NEW powers/abilities to add."}
                """.trimIndent()
                val userPrompt = "$campaignHeader\nCharacter: ${current.name} (${current.roleTitle})\nExisting Powers:\n${charSnapshot.optJSONArray("powers")}\n\nRequest:\n${prompt.trim()}"
                val o = JSONObject(requestJsonObject(sys, userPrompt, provider, modelName, endpointUrl, apiKeyOverride))
                val hasKey = o.has("powers") || o.has("items")
                val parsed = parsePowersFromJsonArray(o.optJSONArray("powers") ?: o.optJSONArray("items"), current.name)
                val nextPowers = when {
                    isEditExisting && hasKey -> parsed
                    isEditExisting -> existingPowers
                    else -> existingPowers + parsed
                }
                Result.success(current.copy(powersJson = JsonCodecs.encodePowers(nextPowers)))
            }

            CharacterSheetAiTab.GEAR_AND_STATS -> {
                val existingEquip = JsonCodecs.decodeEquipment(current.equipmentJson)
                val existingStats = JsonCodecs.decodeStats(current.statsJson)
                val allowedSlots = EquipmentSlot.entries.joinToString(", ") { it.key }
                val sys = """
                    You are a tabletop RPG equipment & character stats assistant.
                    Allowed slotKey symbols: $allowedSlots.
                    Return ONLY a valid JSON object (no markdown fences):
                    {"slots":[{"slotKey":"chest","itemName":"string","bonus":"+1 AC","notes":"string","emoji":"🦺"}],"stats":{"STR":"10","DEX":"10","CON":"10","INT":"10","WIS":"10","CHA":"10"}}
                    ${if (isEditExisting) "Return the COMPLETE updated equipment slots and stats after applying the user's edits (keep untouched slots and stats intact)." else "Return the new/updated equipment slots and any stat changes requested."}
                """.trimIndent()
                val userPrompt = """
                    $campaignHeader
                    Character: ${current.name} (${current.roleTitle})
                    Existing Equipment: ${charSnapshot.optJSONArray("equipment")}
                    Existing Stats: ${charSnapshot.optJSONObject("stats")}

                    Request:
                    ${prompt.trim()}
                """.trimIndent()
                val o = JSONObject(requestJsonObject(sys, userPrompt, provider, modelName, endpointUrl, apiKeyOverride))
                val hasEquipKey = o.has("slots") || o.has("equipment")
                val parsedSlots = parseEquipmentFromJson(o.opt("slots") ?: o.opt("equipment"), preserveImagesFrom = existingEquip)
                val nextEquip = when {
                    isEditExisting && hasEquipKey -> parsedSlots
                    isEditExisting -> existingEquip
                    else -> existingEquip.toMutableMap().apply { putAll(parsedSlots) }
                }
                val nextStats = parseStatsFromJson(o.opt("stats"), existingStats)
                Result.success(
                    current.copy(
                        equipmentJson = JsonCodecs.encodeEquipment(nextEquip),
                        statsJson = JsonCodecs.encodeStats(nextStats),
                    )
                )
            }
        }
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI character tab assist timed out", e)
        Result.failure(IllegalStateException("AI request timed out after 60 seconds. Please try again."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "AI character tab assist failed", e)
        Result.failure(e)
    }

    fun assistCharacterTabRegex(
        current: Character,
        tab: CharacterSheetAiTab,
        rawText: String,
        story: Story?,
    ): Character {
        val stubStory = story ?: Story(
            id = current.storyId,
            gmUserId = current.gmUserId,
            memberUids = current.memberUids,
        )
        return when (tab) {
            CharacterSheetAiTab.INFO -> {
                val parsed = CharacterParserHelpers.parseSingleCharacterBlockRegex(
                    block = rawText,
                    kind = current.kind,
                    story = stubStory,
                    ownerId = current.ownerId.ifBlank { stubStory.gmUserId },
                )
                current.copy(
                    name = parsed.name.takeIf { it.isNotBlank() && it != "Hero" } ?: current.name,
                    roleTitle = parsed.roleTitle.ifBlank { current.roleTitle },
                    locationName = parsed.locationName.ifBlank { current.locationName },
                    level = if (parsed.level > 1L) parsed.level else current.level,
                    hpCurrent = if (parsed.hpCurrent != 15L) parsed.hpCurrent else current.hpCurrent,
                    hpMax = if (parsed.hpMax != 15L) parsed.hpMax else current.hpMax,
                    ac = if (parsed.ac != 12L) parsed.ac else current.ac,
                    avatarEmoji = parsed.avatarEmoji.ifBlank { current.avatarEmoji },
                )
            }
            CharacterSheetAiTab.NOTES -> {
                val addition = rawText.trim()
                val next = if (current.notes.isBlank()) addition else "${current.notes.trim()}\n\n$addition"
                current.copy(notes = next.take(20000))
            }
            CharacterSheetAiTab.INVENTORY -> {
                val sections = CharacterParserHelpers.extractFieldSections(rawText)
                val source = sections["gear"]?.takeIf { it.isNotBlank() } ?: rawText
                val existing = JsonCodecs.decodeInventory(current.inventoryJson)
                val parsed = CharacterParserHelpers.parseGearEntries(source).map { g ->
                    InventoryEntry(
                        id = "inv_${UUID.randomUUID().toString().take(8)}",
                        name = g.name,
                        quantity = g.quantity,
                        notes = listOfNotNull(g.bonus.takeIf { it.isNotBlank() }, g.notes.takeIf { it.isNotBlank() }).joinToString(" — "),
                        emoji = g.emoji,
                    )
                }
                current.copy(inventoryJson = JsonCodecs.encodeInventory(existing + parsed))
            }
            CharacterSheetAiTab.MISSIONS -> {
                val sections = CharacterParserHelpers.extractFieldSections(rawText)
                val source = sections["missions"]?.takeIf { it.isNotBlank() } ?: rawText
                val existing = JsonCodecs.decodeMissions(current.missionsJson)
                val parsed = CharacterParserHelpers.parseMissionEntries(source)
                current.copy(missionsJson = JsonCodecs.encodeMissions(existing + parsed))
            }
            CharacterSheetAiTab.POWERS -> {
                val sections = CharacterParserHelpers.extractFieldSections(rawText)
                val source = sections["powers"]?.takeIf { it.isNotBlank() } ?: rawText
                val existing = JsonCodecs.decodePowers(current.powersJson)
                val parsed = CharacterParserHelpers.parsePowerEntries(source)
                current.copy(powersJson = JsonCodecs.encodePowers(existing + parsed))
            }
            CharacterSheetAiTab.GEAR_AND_STATS -> {
                val sections = CharacterParserHelpers.extractFieldSections(rawText)
                val gearSource = sections["gear"] ?: if (sections.isEmpty()) {
                    rawText
                } else {
                    rawText.lines()
                        .filterNot { line ->
                            val trimmed = line.trim().lowercase()
                            trimmed.startsWith("stats:") || trimmed.startsWith("stat:") || trimmed.startsWith("attributes:")
                        }
                        .joinToString("\n")
                }
                val statsSource = sections["stats"] ?: rawText
                val existingEquip = JsonCodecs.decodeEquipment(current.equipmentJson).toMutableMap()
                val gear = CharacterParserHelpers.parseGearEntries(gearSource)
                val mapped = CharacterParserHelpers.buildEquipmentMap(gear, existingEquip)
                val existingStats = JsonCodecs.decodeStats(current.statsJson)
                val parsedStats = CharacterParserHelpers.parseStatsMap(statsSource, existingStats)
                current.copy(
                    equipmentJson = JsonCodecs.encodeEquipment(mapped),
                    statsJson = JsonCodecs.encodeStats(parsedStats),
                )
            }
        }
    }

    private fun parseStatsFromJson(raw: Any?, seed: Map<String, String>): Map<String, String> {
        val result = linkedMapOf<String, String>().apply { putAll(seed) }
        when (raw) {
            is JSONObject -> {
                val keys = raw.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = raw.optString(key).trim()
                    if (key.isNotBlank() && value.isNotBlank() && !value.equals("null", true)) {
                        result[key.uppercase()] = value
                    }
                }
            }
            is String -> {
                if (raw.isNotBlank() && !raw.equals("null", true)) {
                    result.putAll(CharacterParserHelpers.parseStatsMap(raw, result))
                }
            }
        }
        return result
    }

    private fun parsePowersFromJsonArray(arr: JSONArray?, charName: String): List<PowerEntry> {
        if (arr == null) return emptyList()
        val list = mutableListOf<PowerEntry>()
        for (idx in 0 until arr.length()) {
            val item = arr.opt(idx)
            if (item is JSONObject) {
                val pName = item.optString("name").trim()
                if (pName.isNotBlank()) {
                    val pDesc = item.optString("description", "").trim()
                    val pCost = item.optString("cost", "").trim()
                    val pEmoji = item.optString("emoji")
                        .takeIf { it.isNotBlank() && !it.equals("null", true) }
                        ?: EmojiHelper.suggestEmoji("$pName $pDesc", "✨")
                    val tagsArr = item.optJSONArray("tags")
                    val pTags = if (tagsArr != null) {
                        (0 until tagsArr.length()).mapNotNull { ti ->
                            tagsArr.optString(ti).trim().removePrefix("#").takeIf { it.isNotBlank() && !it.equals("null", true) }
                        }
                    } else emptyList()
                    list += PowerEntry(
                        id = "pow_${UUID.randomUUID().toString().take(8)}",
                        name = EmojiHelper.stripLeadingEmoji(pName).ifBlank { pName },
                        cost = pCost,
                        description = pDesc.ifBlank { "Ability of $charName: $pName." },
                        emoji = pEmoji,
                        tags = pTags.ifEmpty { CharacterParserHelpers.suggestTags("$pName $pDesc", "power") },
                    )
                }
            } else {
                CharacterParserHelpers.parseSinglePowerEntry(arr.optString(idx))?.let { list += it }
            }
        }
        return list
    }

    private fun parseMissionsFromJsonArray(arr: JSONArray?): List<MissionEntry> {
        if (arr == null) return emptyList()
        val list = mutableListOf<MissionEntry>()
        for (idx in 0 until arr.length()) {
            val item = arr.opt(idx)
            if (item is JSONObject) {
                val mTitle = item.optString("title").ifBlank { item.optString("name") }.trim()
                if (mTitle.isNotBlank()) {
                    val rawStatus = item.optString("status", "active").trim().lowercase()
                    val status = if (rawStatus in setOf("completed", "done", "finished")) "completed" else "active"
                    val desc = item.optString("description", "").trim()
                    list += MissionEntry(
                        id = "mis_${UUID.randomUUID().toString().take(8)}",
                        title = mTitle.take(160),
                        status = status,
                        description = desc.take(600),
                    )
                }
            } else {
                CharacterParserHelpers.parseSingleMissionEntry(arr.optString(idx))?.let { list += it }
            }
        }
        return list
    }

    private fun parseInventoryFromJsonArray(arr: JSONArray?): List<InventoryEntry> {
        if (arr == null) return emptyList()
        val list = mutableListOf<InventoryEntry>()
        for (idx in 0 until arr.length()) {
            val item = arr.opt(idx)
            if (item is JSONObject) {
                val name = item.optString("name").ifBlank { item.optString("itemName") }.trim()
                if (name.isNotBlank()) {
                    val qty = item.optInt("quantity", 1).coerceAtLeast(1)
                    val notes = item.optString("notes", "").trim()
                    val emoji = item.optString("emoji").takeIf { it.isNotBlank() && !it.equals("null", true) }
                        ?: EmojiHelper.suggestEmoji(name, "🎒")
                    list += InventoryEntry(
                        id = "inv_${UUID.randomUUID().toString().take(8)}",
                        name = EmojiHelper.stripLeadingEmoji(name).ifBlank { name },
                        quantity = qty,
                        notes = notes,
                        emoji = emoji,
                    )
                }
            } else {
                CharacterParserHelpers.parseSingleGearItem(arr.optString(idx))?.let { g ->
                    list += InventoryEntry(
                        id = "inv_${UUID.randomUUID().toString().take(8)}",
                        name = g.name,
                        quantity = g.quantity,
                        notes = listOfNotNull(g.bonus.takeIf { it.isNotBlank() }, g.notes.takeIf { it.isNotBlank() }).joinToString(" — "),
                        emoji = g.emoji,
                    )
                }
            }
        }
        return list
    }

    private fun parseEquipmentFromJson(
        raw: Any?,
        preserveImagesFrom: Map<String, EquipmentSlotItem>,
    ): Map<String, EquipmentSlotItem> {
        val result = mutableMapOf<String, EquipmentSlotItem>()
        when (raw) {
            is JSONArray -> {
                for (idx in 0 until raw.length()) {
                    val eo = raw.optJSONObject(idx) ?: continue
                    val key = eo.optString("slotKey").trim()
                    val validSlot = EquipmentSlot.entries.firstOrNull { it.key == key } ?: continue
                    val itemName = eo.optString("itemName").ifBlank { eo.optString("name") }.trim()
                    val cleanName = EmojiHelper.stripLeadingEmoji(itemName).ifBlank { itemName }
                    if (cleanName.isNotBlank()) {
                        val emoji = eo.optString("emoji").takeIf { it.isNotBlank() && !it.equals("null", true) }
                            ?: EmojiHelper.suggestEmoji(cleanName, validSlot.emoji)
                        result[key] = EquipmentSlotItem(
                            name = cleanName,
                            bonus = eo.optString("bonus", "").trim(),
                            notes = eo.optString("notes", "").trim(),
                            emoji = emoji,
                            imageUrl = preserveImagesFrom[key]?.imageUrl,
                        )
                    }
                }
            }
            is JSONObject -> {
                val keys = raw.keys()
                while (keys.hasNext()) {
                    val key = keys.next().trim()
                    val validSlot = EquipmentSlot.entries.firstOrNull { it.key == key } ?: continue
                    val eo = raw.optJSONObject(key)
                    if (eo != null) {
                        val itemName = eo.optString("itemName").ifBlank { eo.optString("name") }.trim()
                        val cleanName = EmojiHelper.stripLeadingEmoji(itemName).ifBlank { itemName }
                        if (cleanName.isNotBlank()) {
                            val emoji = eo.optString("emoji").takeIf { it.isNotBlank() && !it.equals("null", true) }
                                ?: EmojiHelper.suggestEmoji(cleanName, validSlot.emoji)
                            result[key] = EquipmentSlotItem(
                                name = cleanName,
                                bonus = eo.optString("bonus", "").trim(),
                                notes = eo.optString("notes", "").trim(),
                                emoji = emoji,
                                imageUrl = preserveImagesFrom[key]?.imageUrl,
                            )
                        }
                    } else {
                        CharacterParserHelpers.parseSingleGearItem(raw.optString(key))?.let { g ->
                            result[key] = EquipmentSlotItem(
                                name = g.name,
                                bonus = g.bonus,
                                notes = g.notes,
                                emoji = g.emoji,
                                imageUrl = preserveImagesFrom[key]?.imageUrl,
                            )
                        }
                    }
                }
            }
        }
        return result
    }
}
