package com.example

import com.example.ai.AiChatMessage
import com.example.ai.AiEditAssistService
import com.example.ai.CharacterSheetAiTab
import com.example.data.Character
import com.example.data.ChatMessage
import com.example.data.EquipmentSlot
import com.example.data.InventoryEntry
import com.example.data.JsonCodecs
import com.example.data.MissionEntry
import com.example.data.PowerEntry
import com.example.data.SearchFilterUtil
import com.example.data.Story
import com.example.data.WorldItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SearchAndEditAssistTest {

    @Test
    fun `search filter matches words and hashtag queries across characters, world items, and chats`() {
        val pyromancer = Character(
            id = "c1",
            name = "Ignis Emberheart",
            roleTitle = "Evocation Mage",
            locationName = "Ashen Keep",
            notes = "Guards the Obsidian Brazier.",
            powersJson = JsonCodecs.encodePowers(
                listOf(PowerEntry(id = "p1", name = "Flame Lash", description = "2d6 fire", tags = listOf("fire", "ranged")))
            ),
            inventoryJson = JsonCodecs.encodeInventory(
                listOf(InventoryEntry(id = "i1", name = "Ruby Focus", quantity = 1))
            ),
            missionsJson = JsonCodecs.encodeMissions(
                listOf(MissionEntry(id = "mis1", title = "Relight the Beacon", status = "active"))
            ),
            statsJson = JsonCodecs.encodeStats(mapOf("INT" to "18", "STR" to "8")),
        )

        assertTrue(SearchFilterUtil.matchesCharacterSearch(pyromancer, "#fire"))
        assertTrue(SearchFilterUtil.matchesCharacterSearch(pyromancer, "fire"))
        assertTrue(SearchFilterUtil.matchesCharacterSearch(pyromancer, "Ruby Focus"))
        assertTrue(SearchFilterUtil.matchesCharacterSearch(pyromancer, "Relight"))
        assertTrue(SearchFilterUtil.matchesCharacterSearch(pyromancer, "Obsidian"))
        assertFalse(SearchFilterUtil.matchesCharacterSearch(pyromancer, "#frost"))

        val worldPower = WorldItem(
            id = "w1",
            category = "power",
            title = "Frost Nova",
            subtitle = "3 Mana",
            description = "Freezes nearby foes.",
            tagsJson = JsonCodecs.encodeStringList(listOf("ice", "crowd-control")),
        )
        assertTrue(SearchFilterUtil.matchesWorldItemSearch(worldPower, "#ice"))
        assertTrue(SearchFilterUtil.matchesWorldItemSearch(worldPower, "crowd-control"))
        assertFalse(SearchFilterUtil.matchesWorldItemSearch(worldPower, "fire"))

        val partyMsg = ChatMessage(
            id = "m1",
            senderName = "GM",
            body = "The shrine awakens!",
            refTitle = "Frost Nova",
            refTagsJson = JsonCodecs.encodeStringList(listOf("ice", "aura")),
        )
        assertTrue(SearchFilterUtil.matchesChatMessageSearch(partyMsg, "#aura"))
        assertTrue(SearchFilterUtil.matchesChatMessageSearch(partyMsg, "shrine"))
        assertFalse(SearchFilterUtil.matchesChatMessageSearch(partyMsg, "#fire"))

        val aiMsg = AiChatMessage(
            id = "am1",
            role = "assistant",
            content = "Generated encounter for #undead crypt.",
            modelUsed = "gemini-2.5-flash",
        )
        assertTrue(SearchFilterUtil.matchesAiChatMessageSearch(aiMsg, "#undead"))
        assertTrue(SearchFilterUtil.matchesAiChatMessageSearch(aiMsg, "undead"))
        assertFalse(SearchFilterUtil.matchesAiChatMessageSearch(aiMsg, "dragon"))
    }

    @Test
    fun `character sheet tab regex assist creates entries on all 6 tabs`() {
        val story = Story(id = "s1", gmUserId = "gm1", memberUids = listOf("gm1"))
        val base = Character(
            id = "c1",
            storyId = "s1",
            gmUserId = "gm1",
            ownerId = "gm1",
            name = "Lyra",
            roleTitle = "Scout",
            level = 1L,
            hpCurrent = 15L,
            hpMax = 15L,
            ac = 12L,
            notes = "Initial note",
        )

        val infoUpdated = AiEditAssistService.assistCharacterTabRegex(
            current = base,
            tab = CharacterSheetAiTab.INFO,
            rawText = "Lyra Moonshadow\nClass: Horizon Ranger\nLocation: Silverwood\nLVL: 5 | HP: 42 | AC: 16",
            story = story,
        )
        assertEquals("Lyra Moonshadow", infoUpdated.name)
        assertEquals("Horizon Ranger", infoUpdated.roleTitle)
        assertEquals("Silverwood", infoUpdated.locationName)
        assertEquals(5L, infoUpdated.level)
        assertEquals(42L, infoUpdated.hpMax)
        assertEquals(16L, infoUpdated.ac)

        val notesUpdated = AiEditAssistService.assistCharacterTabRegex(
            current = infoUpdated,
            tab = CharacterSheetAiTab.NOTES,
            rawText = "Discovered the hidden spire.",
            story = story,
        )
        assertTrue(notesUpdated.notes.contains("Initial note"))
        assertTrue(notesUpdated.notes.contains("Discovered the hidden spire."))

        val invUpdated = AiEditAssistService.assistCharacterTabRegex(
            current = notesUpdated,
            tab = CharacterSheetAiTab.INVENTORY,
            rawText = "Healing Potion, Grappling Hook",
            story = story,
        )
        assertEquals(2, JsonCodecs.decodeInventory(invUpdated.inventoryJson).size)

        val missionsUpdated = AiEditAssistService.assistCharacterTabRegex(
            current = invUpdated,
            tab = CharacterSheetAiTab.MISSIONS,
            rawText = "Find the Sunstone - Search the sunken crypt",
            story = story,
        )
        val missions = JsonCodecs.decodeMissions(missionsUpdated.missionsJson)
        assertEquals(1, missions.size)
        assertEquals("Find the Sunstone", missions.first().title)

        val powersUpdated = AiEditAssistService.assistCharacterTabRegex(
            current = missionsUpdated,
            tab = CharacterSheetAiTab.POWERS,
            rawText = "Moonlight Arrow (1 MP) - Radiant shot [#radiant, #ranged]",
            story = story,
        )
        val powers = JsonCodecs.decodePowers(powersUpdated.powersJson)
        assertEquals(1, powers.size)
        assertEquals("Moonlight Arrow", powers.first().name)

        val gearUpdated = AiEditAssistService.assistCharacterTabRegex(
            current = powersUpdated,
            tab = CharacterSheetAiTab.GEAR_AND_STATS,
            rawText = "Gear: Elven Longbow, Shadow Cloak\nStats: DEX 18, WIS 15",
            story = story,
        )
        val equip = JsonCodecs.decodeEquipment(gearUpdated.equipmentJson)
        val stats = JsonCodecs.decodeStats(gearUpdated.statsJson)
        assertTrue(equip.containsKey(EquipmentSlot.WEAPON_MAIN.key))
        assertTrue(equip.containsKey(EquipmentSlot.BACK.key))
        assertEquals("18", stats["DEX"])
        assertEquals("15", stats["WIS"])
    }
}
