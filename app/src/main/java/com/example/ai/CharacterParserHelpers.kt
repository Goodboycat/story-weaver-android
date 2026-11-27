package com.example.ai

import com.example.data.Character
import com.example.data.EquipmentSlot
import com.example.data.EquipmentSlotItem
import com.example.data.InventoryEntry
import com.example.data.JsonCodecs
import com.example.data.MissionEntry
import com.example.data.PowerEntry
import com.example.data.Story
import com.example.data.WorldItem
import java.util.UUID

data class CharacterImportBundle(
    val characters: List<Character>,
    val locations: List<WorldItem>,
    val powers: List<WorldItem>,
    val items: List<WorldItem>,
)

data class ParsedGearItem(
    val name: String,
    val bonus: String = "",
    val notes: String = "",
    val emoji: String = "⚔️",
    val quantity: Int = 1,
    val explicitSlot: EquipmentSlot? = null,
)

object CharacterParserHelpers {

    private val sectionHeaderRegex = Regex(
        """^\s*(class|role|species|ancestry|type|lvl|level|cr|hp|ac|emoji|location|place|region|home|settlement|area|powers|power|abilities|ability|spells|gear|equipment|inventory|items|loadout|stats|stat|attributes|campaign|world|description|lore|notes|backstory|mission|missions|quest|quests|objective|objectives)\s*:\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )

    private val inlinePipeSegmentRegex = Regex(
        """^\s*(class|role|species|ancestry|type|lvl|level|cr|hp|ac|emoji|location|place|region|home|settlement|area)\s*:\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )

    private val statPairRegex = Regex("""\b([A-Za-z]{2,12})\s*[:=]?\s*([+-]?\d+)\b""")

    private val standardStatKeys = setOf("STR", "DEX", "CON", "INT", "WIS", "CHA")

    private fun canonicalSection(rawKey: String): String = when (rawKey.lowercase()) {
        "class", "role", "species", "ancestry", "type" -> "role"
        "lvl", "level", "cr" -> "level"
        "hp" -> "hp"
        "ac" -> "ac"
        "emoji" -> "emoji"
        "location", "place", "region", "home", "settlement", "area" -> "location"
        "powers", "power", "abilities", "ability", "spells" -> "powers"
        "gear", "equipment", "inventory", "items", "loadout" -> "gear"
        "stats", "stat", "attributes" -> "stats"
        "mission", "missions", "quest", "quests", "objective", "objectives" -> "missions"
        "campaign", "world" -> "campaign"
        else -> "notes"
    }

    /**
     * Extracts multi-line sections cleanly even when Powers, Gear, or Stats wrap across lines
     * (e.g., "Stats: STR 17, DEX 10, CON 16, INT 15, WIS\n12, CHA 11").
     */
    fun extractFieldSections(block: String): Map<String, String> {
        val sections = linkedMapOf<String, StringBuilder>()
        var activeKey: String? = null

        block.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach

            val headerMatch = sectionHeaderRegex.matchEntire(trimmed)
            if (headerMatch != null) {
                val rawHeader = headerMatch.groupValues[1]
                val rest = headerMatch.groupValues[2].trim()
                if (rest.contains("|")) {
                    val parts = trimmed.split("|")
                    var anySubHeader = false
                    parts.forEachIndexed { idx, part ->
                        val pTrim = part.trim()
                        val sub = inlinePipeSegmentRegex.matchEntire(pTrim)
                        if (sub != null) {
                            val key = canonicalSection(sub.groupValues[1])
                            val value = sub.groupValues[2].trim()
                            val sb = sections.getOrPut(key) { StringBuilder() }
                            if (sb.isNotEmpty() && value.isNotEmpty()) sb.append(" ")
                            sb.append(value)
                            activeKey = key
                            anySubHeader = true
                        } else if (idx == 0) {
                            val key = canonicalSection(rawHeader)
                            val sb = sections.getOrPut(key) { StringBuilder() }
                            if (sb.isNotEmpty() && pTrim.isNotEmpty()) sb.append(" ")
                            sb.append(pTrim.substringAfter(":", pTrim).trim())
                            activeKey = key
                        } else if (!anySubHeader && activeKey != null) {
                            sections[activeKey]?.append(" ")?.append(pTrim)
                        }
                    }
                } else {
                    val key = canonicalSection(rawHeader)
                    val sb = sections.getOrPut(key) { StringBuilder() }
                    if (sb.isNotEmpty() && rest.isNotEmpty()) sb.append("\n")
                    sb.append(rest)
                    activeKey = key
                }
            } else if (activeKey != null) {
                val sb = sections[activeKey]!!
                if (sb.isNotEmpty()) {
                    val isListContinuation = activeKey == "missions" ||
                        trimmed.startsWith("-") ||
                        trimmed.startsWith("*") ||
                        trimmed.startsWith("•") ||
                        Regex("""^\d+[.)]\s+""").containsMatchIn(trimmed)
                    sb.append(if (isListContinuation) "\n" else " ")
                }
                sb.append(trimmed)
            }
        }
        return sections.mapValues { it.value.toString().trim() }
    }

    fun parseStatsMap(
        rawStats: String?,
        seed: Map<String, String> = JsonCodecs.defaultStats(),
    ): MutableMap<String, String> {
        val result = linkedMapOf<String, String>().apply {
            putAll(seed.ifEmpty { JsonCodecs.defaultStats() })
        }
        val text = rawStats?.trim().orEmpty()
        if (text.isEmpty()) return result

        statPairRegex.findAll(text).forEach { match ->
            val key = match.groupValues[1].uppercase()
            val value = match.groupValues[2]
            if (key in standardStatKeys || key.length in 2..6) {
                if (key !in setOf("HP", "AC", "LVL", "LV", "CR", "XP")) {
                    result[key] = value
                }
            }
        }
        return result
    }

    fun splitListTokens(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var parenDepth = 0
        var bracketDepth = 0
        raw.forEach { ch ->
            when (ch) {
                '(' -> {
                    parenDepth++
                    current.append(ch)
                }
                ')' -> {
                    parenDepth = (parenDepth - 1).coerceAtLeast(0)
                    current.append(ch)
                }
                '[' -> {
                    bracketDepth++
                    current.append(ch)
                }
                ']' -> {
                    bracketDepth = (bracketDepth - 1).coerceAtLeast(0)
                    current.append(ch)
                }
                ',', ';', '\n' -> {
                    if (parenDepth == 0 && bracketDepth == 0) {
                        val token = current.toString().trim().removePrefix("-").removePrefix("*").trim()
                        if (token.isNotBlank()) result += token
                        current.clear()
                    } else {
                        current.append(ch)
                    }
                }
                else -> current.append(ch)
            }
        }
        val tail = current.toString().trim().removePrefix("-").removePrefix("*").trim()
        if (tail.isNotBlank()) result += tail
        return result
    }

    fun parsePowerEntries(rawPowers: String?): List<PowerEntry> {
        val tokens = splitListTokens(rawPowers.orEmpty())
        return tokens.mapNotNull { token -> parseSinglePowerEntry(token) }
    }

    fun parseSinglePowerEntry(rawToken: String): PowerEntry? {
        var working = rawToken.trim()
        if (working.isEmpty()) return null

        val leadingEmoji = EmojiHelper.extractLeadingEmoji(working)
        if (leadingEmoji != null) {
            working = working.removePrefix(leadingEmoji).trim()
        }

        // Extract explicit [tag1, tag2]
        val tagList = mutableListOf<String>()
        val bracketMatch = Regex("""\[([^\]]+)]\s*$""").find(working)
        if (bracketMatch != null) {
            bracketMatch.groupValues[1].split(",", "#")
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotBlank() }
                .forEach { tagList += it }
            working = working.removeRange(bracketMatch.range).trim()
        }

        // Extract trailing - Description or : Description (careful: "Blue Window: Temper Item" is title if short)
        var description = ""
        val dashIdx = working.indexOf(" - ").takeIf { it > 0 } ?: working.indexOf(" — ").takeIf { it > 0 }
        if (dashIdx != null) {
            description = working.substring(dashIdx + 3).trim()
            working = working.substring(0, dashIdx).trim()
        }

        // Extract (Cost)
        var cost = ""
        val parenMatch = Regex("""\(([^)]+)\)""").find(working)
        if (parenMatch != null) {
            cost = parenMatch.groupValues[1].trim()
            working = working.removeRange(parenMatch.range).replace(Regex("\\s+"), " ").trim()
        }

        val cleanName = working.trim()
        if (cleanName.isBlank()) return null

        val emoji = leadingEmoji ?: EmojiHelper.suggestEmoji("$cleanName $description", "✨")
        val finalDescription = description.ifBlank {
            if (cleanName.contains(":")) {
                "Signature power (${cleanName.substringBefore(":").trim()}) — ${cleanName.substringAfter(":").trim()}."
            } else {
                "Combat & utility ability: $cleanName."
            }
        }
        val finalTags = if (tagList.isNotEmpty()) {
            tagList.distinct()
        } else {
            suggestTags("$cleanName $finalDescription $cost", defaultTag = "power")
        }

        return PowerEntry(
            id = "pow_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            cost = cost,
            description = finalDescription,
            emoji = emoji,
            tags = finalTags,
        )
    }

    fun parseMissionEntries(rawMissions: String?): List<MissionEntry> {
        val tokens = splitListTokens(rawMissions.orEmpty())
        return tokens.mapNotNull { token -> parseSingleMissionEntry(token) }
    }

    fun parseSingleMissionEntry(rawToken: String): MissionEntry? {
        var working = rawToken.trim()
            .replace(Regex("""^\d+[.)]\s*"""), "")
            .removePrefix("[ ]")
            .removePrefix("[x]")
            .removePrefix("[X]")
            .trim()
        if (working.isEmpty() || working.equals("none", ignoreCase = true) || working.equals("n/a", ignoreCase = true)) {
            return null
        }

        val leadingEmoji = EmojiHelper.extractLeadingEmoji(working)
        var completed = leadingEmoji == "✅" || rawToken.trim().startsWith("[x]", ignoreCase = true)
        if (leadingEmoji != null) {
            working = working.removePrefix(leadingEmoji).trim()
        }

        val statusBracket = Regex("""\[(completed|done|finished|active|pending|open)]""", RegexOption.IGNORE_CASE).find(working)
        if (statusBracket != null) {
            val tag = statusBracket.groupValues[1].lowercase()
            if (tag in setOf("completed", "done", "finished")) completed = true
            working = working.removeRange(statusBracket.range).replace(Regex("\\s+"), " ").trim()
        }

        val statusParen = Regex("""\((completed|done|finished|active|pending|open)\)""", RegexOption.IGNORE_CASE).find(working)
        if (statusParen != null) {
            val tag = statusParen.groupValues[1].lowercase()
            if (tag in setOf("completed", "done", "finished")) completed = true
            working = working.removeRange(statusParen.range).replace(Regex("\\s+"), " ").trim()
        }

        var description = ""
        val dashIdx = working.indexOf(" - ").takeIf { it > 0 } ?: working.indexOf(" — ").takeIf { it > 0 }
        if (dashIdx != null) {
            description = working.substring(dashIdx + 3).trim()
            working = working.substring(0, dashIdx).trim()
        }

        val cleanTitle = working.trim()
        if (cleanTitle.isBlank()) return null

        return MissionEntry(
            id = "mis_${UUID.randomUUID().toString().take(8)}",
            title = cleanTitle.take(160),
            status = if (completed) "completed" else "active",
            description = description.take(600),
        )
    }

    private val conversationalLineRegex = listOf(
        Regex("""^(?:sure|certainly|of course|alright|okay|ok|great|gladly|absolutely)[!,.:\s]*.*$""", RegexOption.IGNORE_CASE),
        Regex("""^(?:here\s+(?:you\s+have|you\s+go|are|is|'s)\b.*|below\s+(?:are|is)\b.*|following\s+(?:are|is)\b.*)$""", RegexOption.IGNORE_CASE),
        Regex("""^(?:let me know|feel free|hope this helps|if you(?:'d| would)? like|would you like|do you want|i (?:can|have created|generated|made)\b).*$""", RegexOption.IGNORE_CASE),
        Regex("""^[-*_=#]{3,}\s*$"""),
    )

    private val fillerTitleKeywordsRegex = Regex(
        """\b(?:here\s+(?:you\s+have|you\s+go|are|is)|let me know|feel free|hope this helps|for your campaign|below are)\b""",
        RegexOption.IGNORE_CASE
    )

    fun isConversationalFillerLine(line: String): Boolean {
        val clean = line.trim().removePrefix("#").removePrefix("*").removePrefix("-").trim()
        if (clean.isEmpty()) return true
        if (sectionHeaderRegex.matches(clean)) return false
        if (Regex("""(?:HP|AC|LVL|Level|CR)[:\s]+\d+""", RegexOption.IGNORE_CASE).containsMatchIn(clean)) return false
        if (conversationalLineRegex.any { it.matches(clean) }) return true
        if (clean.endsWith(":") && fillerTitleKeywordsRegex.containsMatchIn(clean)) return true
        return false
    }

    fun isConversationalFillerBlock(block: String): Boolean {
        val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return true
        val hasStructuredFields = lines.any { line ->
            sectionHeaderRegex.matches(line) ||
                Regex("""(?:HP|AC|LVL|Level|CR)[:\s]+\d+""", RegexOption.IGNORE_CASE).containsMatchIn(line)
        }
        if (hasStructuredFields) {
            return stripLeadingFillerLines(block).isBlank()
        }
        return lines.all { isConversationalFillerLine(it) } ||
            fillerTitleKeywordsRegex.containsMatchIn(lines.first())
    }

    fun stripLeadingFillerLines(block: String): String {
        val lines = block.lines()
        var idx = 0
        while (idx < lines.size) {
            val trimmed = lines[idx].trim()
            if (trimmed.isEmpty() || isConversationalFillerLine(trimmed)) {
                idx++
            } else {
                break
            }
        }
        var end = lines.size
        while (end > idx) {
            val trimmed = lines[end - 1].trim()
            if (trimmed.isEmpty() || isConversationalFillerLine(trimmed)) {
                end--
            } else {
                break
            }
        }
        return if (idx >= end) "" else lines.subList(idx, end).joinToString("\n").trim()
    }

    fun isConversationalFillerTitle(title: String): Boolean {
        val clean = title.trim()
        if (clean.isBlank()) return true
        return isConversationalFillerLine(clean) || fillerTitleKeywordsRegex.containsMatchIn(clean)
    }

    fun parseGearEntries(rawGear: String?): List<ParsedGearItem> {
        val tokens = splitListTokens(rawGear.orEmpty())
        return tokens.mapNotNull { token -> parseSingleGearItem(token) }
    }

    fun parseSingleGearItem(rawToken: String): ParsedGearItem? {
        var working = rawToken.trim()
        if (working.isEmpty()) return null

        // Check explicit slotKey prefix e.g. "weapon_main: Heavy Hammer"
        var explicitSlot: EquipmentSlot? = null
        val prefix = working.substringBefore(":", "").trim().lowercase()
        val matchedExplicit = EquipmentSlot.entries.firstOrNull { it.key == prefix }
        if (matchedExplicit != null && working.contains(":")) {
            explicitSlot = matchedExplicit
            working = working.substringAfter(":").trim()
        }

        val leadingEmoji = EmojiHelper.extractLeadingEmoji(working)
        if (leadingEmoji != null) {
            working = working.removePrefix(leadingEmoji).trim()
        }

        var quantity = 1
        val qtyPrefixMatch = Regex("""^(\d+)\s*[xX×]\s+(.+)$""").matchEntire(working)
        if (qtyPrefixMatch != null) {
            quantity = qtyPrefixMatch.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
            working = qtyPrefixMatch.groupValues[2].trim()
        }

        var notes = ""
        val dashIdx = working.indexOf(" - ").takeIf { it > 0 } ?: working.indexOf(" — ").takeIf { it > 0 }
        if (dashIdx != null) {
            notes = working.substring(dashIdx + 3).trim()
            working = working.substring(0, dashIdx).trim()
        }

        var bonus = ""
        val parenMatch = Regex("""\(([^)]+)\)""").find(working)
        if (parenMatch != null) {
            bonus = parenMatch.groupValues[1].trim()
            working = working.removeRange(parenMatch.range).replace(Regex("\\s+"), " ").trim()
        }

        val cleanName = working.trim()
        if (cleanName.isBlank()) return null

        val inferredSlot = explicitSlot ?: classifyEquipmentSlot(cleanName)
        val fallbackEmoji = inferredSlot?.emoji ?: EmojiHelper.suggestEmoji(cleanName, "⚔️")
        val emoji = leadingEmoji ?: EmojiHelper.suggestEmoji(cleanName, fallbackEmoji)

        return ParsedGearItem(
            name = cleanName,
            bonus = bonus,
            notes = notes,
            emoji = emoji,
            quantity = quantity,
            explicitSlot = inferredSlot,
        )
    }

    fun classifyEquipmentSlot(itemName: String): EquipmentSlot? {
        val lower = itemName.lowercase()
        return when {
            listOf("hammer", "warhammer", "maul", "sword", "longsword", "shortsword", "broadsword", "greatsword", "blade", "axe", "battleaxe", "greataxe", "handaxe", "dagger", "knife", "bow", "longbow", "shortbow", "crossbow", "staff", "wand", "rod", "mace", "flail", "spear", "halberd", "lance", "pike", "club", "rapier", "scimitar", "katana", "trident", "whip", "pistol", "rifle", "weapon", "ring", "signet")
                .any { Regex("\\b${Regex.escape(it)}s?\\b").containsMatchIn(lower) } -> EquipmentSlot.WEAPON_MAIN

            listOf("shield", "buckler", "pauldron", "epaulet", "shoulder")
                .any { Regex("\\b${Regex.escape(it)}s?\\b").containsMatchIn(lower) } -> EquipmentSlot.SHOULDERS

            listOf("apron", "armor", "armour", "mail", "chainmail", "plate", "breastplate", "cuirass", "jerkin", "tunic", "robe", "vest", "hauberk", "brigandine", "coat", "leather")
                .any { Regex("\\b${Regex.escape(it)}s?\\b").containsMatchIn(lower) } -> EquipmentSlot.CHEST

            listOf("helm", "helmet", "hat", "hood", "crown", "circlet", "cap", "coif", "tiara")
                .any { Regex("\\b${Regex.escape(it)}s?\\b").containsMatchIn(lower) } -> EquipmentSlot.HEAD

            listOf("goggles", "visor", "mask", "glasses", "spectacles", "monocle", "eyepatch", "blindfold")
                .any { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lower) } -> EquipmentSlot.EYES

            listOf("amulet", "necklace", "pendant", "choker", "locket", "torc", "talisman", "medallion", "collar")
                .any { Regex("\\b${Regex.escape(it)}s?\\b").containsMatchIn(lower) } -> EquipmentSlot.NECK

            listOf("cloak", "cape", "mantle", "shroud", "wings")
                .any { Regex("\\b${Regex.escape(it)}s?\\b").containsMatchIn(lower) } -> EquipmentSlot.BACK

            listOf("bracer", "vambrace", "bracelet", "wrist", "cuff")
                .any { Regex("\\b${Regex.escape(it)}s?\\b").containsMatchIn(lower) } -> EquipmentSlot.WRISTS

            listOf("glove", "gauntlet", "mitt")
                .any { Regex("\\b${Regex.escape(it)}s?\\b").containsMatchIn(lower) } -> EquipmentSlot.HANDS

            listOf("belt", "sash", "girdle", "waistband")
                .any { Regex("\\b${Regex.escape(it)}s?\\b").containsMatchIn(lower) } -> EquipmentSlot.WAIST

            listOf("greaves", "leggings", "pants", "trousers", "kilt", "cuisses")
                .any { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lower) } -> EquipmentSlot.LEGS

            listOf("boot", "shoe", "sabaton", "sandal", "slipper")
                .any { Regex("\\b${Regex.escape(it)}s?\\b").containsMatchIn(lower) } -> EquipmentSlot.FEET

            else -> null
        }
    }

    fun buildEquipmentMap(
        gearItems: List<ParsedGearItem>,
        existing: Map<String, EquipmentSlotItem> = emptyMap(),
    ): Map<String, EquipmentSlotItem> {
        val result = existing.toMutableMap()
        gearItems.forEach { gear ->
            val slot = gear.explicitSlot ?: return@forEach
            if (!result.containsKey(slot.key) || result[slot.key]?.name.isNullOrBlank()) {
                result[slot.key] = EquipmentSlotItem(
                    name = gear.name,
                    bonus = gear.bonus,
                    notes = gear.notes,
                    emoji = gear.emoji.ifBlank { slot.emoji },
                )
            }
        }
        return result
    }

    fun suggestTags(text: String, defaultTag: String): List<String> {
        val lower = text.lowercase()
        val tags = mutableListOf<String>()
        if (listOf("fire", "flame", "ember", "forge", "temper", "heat").any { lower.contains(it) }) tags += "fire"
        if (listOf("ice", "frost", "cold").any { lower.contains(it) }) tags += "frost"
        if (listOf("shield", "guard", "armor", "apron", "ward", "ac").any { lower.contains(it) }) tags += "defense"
        if (listOf("hammer", "sword", "axe", "faultline", "strike", "attack", "bow", "weapon").any { lower.contains(it) }) tags += "combat"
        if (listOf("forge", "smith", "craft", "kit", "tool", "repair", "temper").any { lower.contains(it) }) tags += "craft"
        if (listOf("magic", "arcane", "spell", "rune", "window").any { lower.contains(it) }) tags += "arcane"
        if (listOf("heal", "cure", "holy", "aid").any { lower.contains(it) }) tags += "support"
        if (tags.isEmpty()) tags += defaultTag
        return tags.take(3)
    }

    fun parseSingleCharacterBlockRegex(
        block: String,
        kind: String,
        story: Story,
        ownerId: String,
    ): Character {
        val cleanBlock = stripLeadingFillerLines(block).ifBlank { block }
        val lines = cleanBlock.lines().map { it.trim() }.filter { it.isNotBlank() }
        val firstLineRaw = lines.firstOrNull()
            ?.removePrefix("#")?.removePrefix("-")?.removePrefix("*")?.trim()
            ?: "Hero"
        val leadingEmoji = EmojiHelper.extractLeadingEmoji(firstLineRaw)
        val firstLine = EmojiHelper.stripLeadingEmoji(firstLineRaw)
        val name = firstLine.substringBefore("(").substringBefore(" - ").trim().ifBlank { "Hero" }

        val sections = extractFieldSections(cleanBlock)
        val hpMatch = Regex("""HP[:\s]+(\d+)""", RegexOption.IGNORE_CASE).find(cleanBlock)
        val acMatch = Regex("""AC[:\s]+(\d+)""", RegexOption.IGNORE_CASE).find(cleanBlock)
        val lvlMatch = Regex("""(?:LVL|Level|CR)[:\s]+(\d+)""", RegexOption.IGNORE_CASE).find(cleanBlock)

        val hp = sections["hp"]?.let { Regex("""\d+""").find(it)?.value?.toLongOrNull() }
            ?: hpMatch?.groupValues?.get(1)?.toLongOrNull()
            ?: 15L
        val ac = sections["ac"]?.let { Regex("""\d+""").find(it)?.value?.toLongOrNull() }
            ?: acMatch?.groupValues?.get(1)?.toLongOrNull()
            ?: 12L
        val lvl = sections["level"]?.let { Regex("""\d+""").find(it)?.value?.toLongOrNull() }
            ?: lvlMatch?.groupValues?.get(1)?.toLongOrNull()
            ?: 1L
        val role = sections["role"]?.trim()?.takeIf { it.isNotBlank() }
            ?: kind.replaceFirstChar { it.uppercase() }
        val locationName = sections["location"]?.trim()?.take(120).orEmpty()

        val emojiCandidate = sections["emoji"]?.trim()
            ?.let { EmojiHelper.extractLeadingEmoji(it) ?: it.split(" ").firstOrNull() }
            ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        val emoji = emojiCandidate
            ?: leadingEmoji
            ?: EmojiHelper.suggestEmoji(cleanBlock, if (kind == "monster") "👹" else "🛡️")

        val powersList = parsePowerEntries(sections["powers"])
        val missionsList = parseMissionEntries(sections["missions"])
        val gearEntries = parseGearEntries(sections["gear"])
        val equipmentMap = buildEquipmentMap(gearEntries)
        val invList = gearEntries.map { g ->
            InventoryEntry(
                id = "inv_${UUID.randomUUID().toString().take(8)}",
                name = g.name,
                quantity = g.quantity,
                notes = buildString {
                    if (g.bonus.isNotBlank()) append(g.bonus)
                    if (g.notes.isNotBlank()) {
                        if (isNotEmpty()) append(" — ")
                        append(g.notes)
                    }
                    if (isEmpty() && g.explicitSlot != null) {
                        append("Slot: ${g.explicitSlot.label}")
                    }
                },
                emoji = g.emoji,
            )
        }

        val parsedStats = parseStatsMap(sections["stats"])

        return Character(
            storyId = story.id,
            gmUserId = story.gmUserId,
            ownerId = ownerId,
            memberUids = story.memberUids,
            kind = kind,
            name = name.take(100),
            avatarEmoji = emoji.take(16),
            roleTitle = role.take(100),
            locationName = locationName,
            level = lvl,
            hpCurrent = hp,
            hpMax = hp,
            ac = ac,
            notes = cleanBlock.take(5000),
            powersJson = JsonCodecs.encodePowers(powersList),
            missionsJson = JsonCodecs.encodeMissions(missionsList),
            inventoryJson = JsonCodecs.encodeInventory(invList),
            equipmentJson = JsonCodecs.encodeEquipment(equipmentMap),
            statsJson = JsonCodecs.encodeStats(parsedStats),
        )
    }

    /**
     * Re-parses a character's Notes field and fills/updates locationName, stats, powers,
     * inventory, and equipment slots without erasing existing manual values unnecessarily.
     */
    fun applyParsedNotesToCharacter(current: Character, story: Story): Character {
        if (current.notes.isBlank()) return current
        val parsed = parseSingleCharacterBlockRegex(
            block = current.notes,
            kind = current.kind,
            story = story,
            ownerId = current.ownerId.ifBlank { story.gmUserId },
        )
        val existingStats = JsonCodecs.decodeStats(current.statsJson).toMutableMap()
        val parsedStats = JsonCodecs.decodeStats(parsed.statsJson)
        val defaults = JsonCodecs.defaultStats()
        parsedStats.forEach { (k, v) ->
            val prev = existingStats[k]
            if (prev == null || prev == defaults[k] || v != defaults[k]) {
                existingStats[k] = v
            }
        }

        val existingEquipment = JsonCodecs.decodeEquipment(current.equipmentJson).toMutableMap()
        val parsedEquipment = JsonCodecs.decodeEquipment(parsed.equipmentJson)
        parsedEquipment.forEach { (slotKey, slotItem) ->
            if (existingEquipment[slotKey]?.name.isNullOrBlank()) {
                existingEquipment[slotKey] = slotItem
            }
        }

        val existingPowers = JsonCodecs.decodePowers(current.powersJson).toMutableList()
        val existingPowerKeys = existingPowers.map { it.name.trim().lowercase() }.toMutableSet()
        JsonCodecs.decodePowers(parsed.powersJson).forEach { p ->
            if (existingPowerKeys.add(p.name.trim().lowercase())) {
                existingPowers += p
            }
        }

        val existingInv = JsonCodecs.decodeInventory(current.inventoryJson).toMutableList()
        val existingInvKeys = existingInv.map { it.name.trim().lowercase() }.toMutableSet()
        JsonCodecs.decodeInventory(parsed.inventoryJson).forEach { item ->
            if (existingInvKeys.add(item.name.trim().lowercase())) {
                existingInv += item
            }
        }

        val existingMissions = JsonCodecs.decodeMissions(current.missionsJson).toMutableList()
        val existingMissionKeys = existingMissions.map { it.title.trim().lowercase() }.toMutableSet()
        JsonCodecs.decodeMissions(parsed.missionsJson).forEach { m ->
            if (existingMissionKeys.add(m.title.trim().lowercase())) {
                existingMissions += m
            }
        }

        val nextRole = if (current.roleTitle.isBlank()) parsed.roleTitle else current.roleTitle
        val nextLoc = current.locationName.ifBlank { parsed.locationName }

        return current.copy(
            roleTitle = nextRole,
            locationName = nextLoc,
            level = if (current.level <= 1L && parsed.level > 1L) parsed.level else current.level,
            hpCurrent = if (current.hpCurrent == 10L && parsed.hpCurrent != 15L) parsed.hpCurrent else current.hpCurrent,
            hpMax = if (current.hpMax == 10L && parsed.hpMax != 15L) parsed.hpMax else current.hpMax,
            ac = if (current.ac == 10L && parsed.ac != 12L) parsed.ac else current.ac,
            statsJson = JsonCodecs.encodeStats(existingStats),
            equipmentJson = JsonCodecs.encodeEquipment(existingEquipment),
            powersJson = JsonCodecs.encodePowers(existingPowers),
            missionsJson = JsonCodecs.encodeMissions(existingMissions),
            inventoryJson = JsonCodecs.encodeInventory(existingInv),
        )
    }

    /**
     * Generates full WorldItem records (Locations, Powers, Items) extracted from imported Characters
     * so they auto-register into the corresponding tabs.
     */
    fun buildWorldBundleFromCharacters(
        characters: List<Character>,
        story: Story,
        ownerId: String,
    ): CharacterImportBundle {
        val locationMap = linkedMapOf<String, WorldItem>()
        val powerMap = linkedMapOf<String, WorldItem>()
        val itemMap = linkedMapOf<String, WorldItem>()

        characters.forEach { char ->
            val locName = char.locationName.trim()
            if (locName.isNotBlank()) {
                val locKey = locName.lowercase()
                locationMap.putIfAbsent(
                    locKey,
                    WorldItem(
                        storyId = story.id,
                        gmUserId = story.gmUserId,
                        ownerId = ownerId,
                        memberUids = story.memberUids,
                        category = "location",
                        title = locName.take(120),
                        subtitle = "Associated with ${char.name}".take(120),
                        description = "Home or active region noted for ${char.name} (${char.roleTitle}).".take(1000),
                        iconName = "Place",
                        iconEmoji = EmojiHelper.suggestEmoji(locName, "🏰"),
                        sortOrder = locationMap.size.toLong(),
                        tagsJson = JsonCodecs.encodeStringList(listOf(char.kind, char.name).filter { it.isNotBlank() }),
                    )
                )
            }

            JsonCodecs.decodePowers(char.powersJson).forEach { pow ->
                val pKey = pow.name.trim().lowercase()
                if (pKey.isNotBlank()) {
                    val tags = pow.tags.ifEmpty { suggestTags("${pow.name} ${pow.description}", "power") }
                    powerMap.putIfAbsent(
                        pKey,
                        WorldItem(
                            storyId = story.id,
                            gmUserId = story.gmUserId,
                            ownerId = ownerId,
                            memberUids = story.memberUids,
                            category = "power",
                            title = pow.name.trim().take(120),
                            subtitle = pow.cost.ifBlank { "${char.name} Power" }.take(120),
                            description = pow.description.ifBlank { "Ability of ${char.name}: ${pow.name}." }.take(1000),
                            iconName = "AutoAwesome",
                            iconEmoji = pow.emoji.ifBlank { EmojiHelper.suggestEmoji(pow.name, "✨") }.take(16),
                            sortOrder = powerMap.size.toLong(),
                            tagsJson = JsonCodecs.encodeStringList(tags),
                        )
                    )
                }
            }

            JsonCodecs.decodeEquipment(char.equipmentJson).forEach { (slotKey, slotItem) ->
                val cleanTitle = slotItem.name.trim()
                val iKey = cleanTitle.lowercase()
                if (cleanTitle.isNotBlank()) {
                    val slotLabel = EquipmentSlot.entries.firstOrNull { it.key == slotKey }?.label ?: "Equipment"
                    val desc = buildString {
                        if (slotItem.bonus.isNotBlank()) append("Bonus: ${slotItem.bonus}. ")
                        if (slotItem.notes.isNotBlank()) append(slotItem.notes)
                        else append("Equipped in $slotLabel slot (${char.name}).")
                    }
                    itemMap.putIfAbsent(
                        iKey,
                        WorldItem(
                            storyId = story.id,
                            gmUserId = story.gmUserId,
                            ownerId = ownerId,
                            memberUids = story.memberUids,
                            category = "item",
                            title = cleanTitle.take(120),
                            subtitle = slotItem.bonus.ifBlank { slotLabel }.take(120),
                            description = desc.take(1000),
                            iconName = "Build",
                            iconEmoji = slotItem.emoji.ifBlank { EmojiHelper.suggestEmoji(cleanTitle, "⚔️") }.take(16),
                            imageUrl = slotItem.imageUrl,
                            sortOrder = itemMap.size.toLong(),
                            tagsJson = JsonCodecs.encodeStringList(suggestTags("$cleanTitle $slotLabel", "equipment")),
                        )
                    )
                }
            }

            JsonCodecs.decodeInventory(char.inventoryJson).forEach { inv ->
                val cleanTitle = inv.name.trim()
                val iKey = cleanTitle.lowercase()
                if (cleanTitle.isNotBlank()) {
                    val slot = classifyEquipmentSlot(cleanTitle)
                    val roleLabel = slot?.label ?: "Tool / Gear"
                    itemMap.putIfAbsent(
                        iKey,
                        WorldItem(
                            storyId = story.id,
                            gmUserId = story.gmUserId,
                            ownerId = ownerId,
                            memberUids = story.memberUids,
                            category = "item",
                            title = cleanTitle.take(120),
                            subtitle = roleLabel.take(120),
                            description = inv.notes.ifBlank { "Carried by ${char.name} (x${inv.quantity})." }.take(1000),
                            iconName = "Build",
                            iconEmoji = inv.emoji.ifBlank { slot?.emoji ?: EmojiHelper.suggestEmoji(cleanTitle, "🛠️") }.take(16),
                            sortOrder = itemMap.size.toLong(),
                            tagsJson = JsonCodecs.encodeStringList(suggestTags("$cleanTitle $roleLabel", "tool")),
                        )
                    )
                }
            }
        }

        return CharacterImportBundle(
            characters = characters,
            locations = locationMap.values.toList(),
            powers = powerMap.values.toList(),
            items = itemMap.values.toList(),
        )
    }
}
