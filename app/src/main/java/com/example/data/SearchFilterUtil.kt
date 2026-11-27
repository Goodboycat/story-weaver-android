package com.example.data

import com.example.ai.AiChatMessage

object SearchFilterUtil {

    private fun normalizedNeedles(query: String): List<String> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val stripped = q.removePrefix("#").trim()
        return if (stripped.isNotEmpty() && !stripped.equals(q, ignoreCase = true)) {
            listOf(q, stripped)
        } else {
            listOf(q)
        }
    }

    private fun containsAnyNeedle(haystack: String, needles: List<String>): Boolean {
        if (haystack.isBlank() || needles.isEmpty()) return false
        return needles.any { haystack.contains(it, ignoreCase = true) }
    }

    fun matchesCharacterSearch(char: Character, query: String): Boolean {
        val needles = normalizedNeedles(query)
        if (needles.isEmpty()) return true
        if (
            containsAnyNeedle(char.name, needles) ||
            containsAnyNeedle(char.roleTitle, needles) ||
            containsAnyNeedle(char.locationName, needles) ||
            containsAnyNeedle(char.notes, needles)
        ) {
            return true
        }
        val powers = JsonCodecs.decodePowers(char.powersJson)
        if (powers.any { p ->
                containsAnyNeedle(p.name, needles) ||
                    containsAnyNeedle(p.cost, needles) ||
                    containsAnyNeedle(p.description, needles) ||
                    p.tags.any { tag -> containsAnyNeedle(tag, needles) }
            }
        ) {
            return true
        }
        val inventory = JsonCodecs.decodeInventory(char.inventoryJson)
        if (inventory.any { item ->
                containsAnyNeedle(item.name, needles) || containsAnyNeedle(item.notes, needles)
            }
        ) {
            return true
        }
        val missions = JsonCodecs.decodeMissions(char.missionsJson)
        if (missions.any { m ->
                containsAnyNeedle(m.title, needles) || containsAnyNeedle(m.description, needles)
            }
        ) {
            return true
        }
        val stats = JsonCodecs.decodeStats(char.statsJson)
        if (stats.any { (k, v) ->
                containsAnyNeedle(k, needles) || containsAnyNeedle(v, needles)
            }
        ) {
            return true
        }
        return false
    }

    fun matchesWorldItemSearch(item: WorldItem, query: String): Boolean {
        val needles = normalizedNeedles(query)
        if (needles.isEmpty()) return true
        if (
            containsAnyNeedle(item.title, needles) ||
            containsAnyNeedle(item.subtitle, needles) ||
            containsAnyNeedle(item.description, needles)
        ) {
            return true
        }
        val tags = JsonCodecs.decodeStringList(item.tagsJson)
        return tags.any { tag -> containsAnyNeedle(tag, needles) }
    }

    fun matchesChatMessageSearch(msg: ChatMessage, query: String): Boolean {
        val needles = normalizedNeedles(query)
        if (needles.isEmpty()) return true
        if (
            containsAnyNeedle(msg.body, needles) ||
            containsAnyNeedle(msg.senderName, needles) ||
            containsAnyNeedle(msg.refTitle, needles) ||
            containsAnyNeedle(msg.refSubtitle, needles) ||
            containsAnyNeedle(msg.refDescription, needles) ||
            containsAnyNeedle(msg.targetCharacterName, needles)
        ) {
            return true
        }
        val tags = JsonCodecs.decodeStringList(msg.refTagsJson)
        return tags.any { tag -> containsAnyNeedle(tag, needles) }
    }

    fun matchesAiChatMessageSearch(msg: AiChatMessage, query: String): Boolean {
        val needles = normalizedNeedles(query)
        if (needles.isEmpty()) return true
        return containsAnyNeedle(msg.content, needles) ||
            containsAnyNeedle(msg.modelUsed, needles)
    }
}
