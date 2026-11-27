package com.example

import com.example.ai.CharacterParserHelpers
import com.example.data.EquipmentSlot
import com.example.data.JsonCodecs
import com.example.data.MapPaintEngine
import com.example.data.MapPaintJsonParser
import com.example.data.Story
import com.example.ui.tabs.toEntityRefOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ParserRobolectricTest {

    @Test
    fun `map parser and codecs strip null strings and plain English words from tile emojis`() {
        val aiJson = """
            {
              "width": 6,
              "height": 6,
              "clearFirst": true,
              "summary": "Forest Glade",
              "operations": [
                {"tool": "rect", "x1": 0, "y1": 0, "x2": 5, "y2": 5, "colorHex": "#14532D", "emoji": "null"},
                {"tool": "paint", "colorHex": "#1E293B", "emoji": "tree", "points": [[1, 1]]},
                {"tool": "paint", "colorHex": "#0284C7", "emoji": "🌊", "points": [[2, 2]]}
              ],
              "tokens": []
            }
        """.trimIndent()
        val script = MapPaintJsonParser.parseScript(aiJson).getOrThrow()
        val outcome = MapPaintEngine.executeScript(script, 6, 6, emptyMap(), emptyList(), "gm")
        assertEquals("#14532D", outcome.grid["0,0"]?.colorHex)
        assertNull(outcome.grid["0,0"]?.emoji)
        assertNull(outcome.grid["1,1"]?.emoji)
        assertEquals("🌊", outcome.grid["2,2"]?.emoji)

        val decodedLegacy = JsonCodecs.decodeGrid("""{"0,0":{"colorHex":"#14532D","emoji":"null"}}""")
        assertNull(decodedLegacy["0,0"]?.emoji)
    }

    @Test
    fun `character parser extracts wrapped stats, equipment slots, inventory, powers, location and world registrations`() {
        val brannikBlock = """
            ⚒️ Brannik Coil
            Class: Salvage Smith / Dwarf
            LVL: 6 | HP: 61 | AC: 16
            Location: Emberforge Foundry
            Emoji: ⚒️
            Powers: Blue Window: Temper Item,
            Scrapguard, Faultline Hammer
            Gear: Folding forge kit, heavy hammer,
            reinforced work apron
            Stats: STR 17, DEX 10, CON 16, INT 15, WIS
            12, CHA 11
            Campaign: Purify the World (custom)
        """.trimIndent()

        val story = Story(id = "s1", gmUserId = "gm1", memberUids = listOf("gm1"))
        val character = CharacterParserHelpers.parseSingleCharacterBlockRegex(
            block = brannikBlock,
            kind = "npc",
            story = story,
            ownerId = "gm1",
        )

        assertEquals("Brannik Coil", character.name)
        assertEquals("Emberforge Foundry", character.locationName)
        assertEquals(6L, character.level)
        assertEquals(61L, character.hpMax)
        assertEquals(16L, character.ac)

        val stats = JsonCodecs.decodeStats(character.statsJson)
        assertEquals("17", stats["STR"])
        assertEquals("10", stats["DEX"])
        assertEquals("16", stats["CON"])
        assertEquals("15", stats["INT"])
        assertEquals("12", stats["WIS"])
        assertEquals("11", stats["CHA"])

        val equip = JsonCodecs.decodeEquipment(character.equipmentJson)
        assertEquals("heavy hammer", equip[EquipmentSlot.WEAPON_MAIN.key]?.name)
        assertEquals("reinforced work apron", equip[EquipmentSlot.CHEST.key]?.name)

        val inv = JsonCodecs.decodeInventory(character.inventoryJson)
        assertEquals(3, inv.size)
        assertTrue(inv.any { it.name.equals("Folding forge kit", ignoreCase = true) })

        val powers = JsonCodecs.decodePowers(character.powersJson)
        assertEquals(3, powers.size)
        assertTrue(powers.any { it.name == "Blue Window: Temper Item" && it.description.isNotBlank() && it.tags.isNotEmpty() })

        val bundle = CharacterParserHelpers.buildWorldBundleFromCharacters(listOf(character), story, "gm1")
        assertTrue(bundle.locations.any { it.title == "Emberforge Foundry" && it.category == "location" })
        assertEquals(3, bundle.powers.size)
        assertTrue(bundle.items.any { it.title.equals("heavy hammer", ignoreCase = true) && it.category == "item" })
        assertTrue(bundle.items.any { it.title.equals("Folding forge kit", ignoreCase = true) && it.category == "item" })
    }

    @Test
    fun `chat reactions codec and entity reference snapshots preserve power details for players`() {
        val reactions = mapOf("🔥" to listOf("u1", "u2"), "🎲" to listOf("u1"))
        val encoded = JsonCodecs.encodeReactions(reactions)
        val decoded = JsonCodecs.decodeReactions(encoded)
        assertEquals(listOf("u1", "u2"), decoded["🔥"])
        assertEquals(listOf("u1"), decoded["🎲"])

        val powerItem = com.example.data.WorldItem(
            id = "pw1",
            storyId = "s1",
            gmUserId = "gm1",
            ownerId = "gm1",
            category = "power",
            title = "Astral Cleave",
            subtitle = "3 MP • Bonus Action",
            description = "Unleash a radiant arc dealing 3d8 force damage.",
            iconEmoji = "✨",
            tagsJson = JsonCodecs.encodeStringList(listOf("Radiant", "Melee", "Burst")),
        )
        val catalog = com.example.ui.tabs.buildEntityReferenceCatalog(emptyList(), listOf(powerItem))
        assertEquals(1, catalog.size)
        val ref = catalog.first()
        assertEquals("power", ref.refType)
        assertEquals("Astral Cleave", ref.title)
        assertEquals("Unleash a radiant arc dealing 3d8 force damage.", ref.description)
        assertEquals(listOf("Radiant", "Melee", "Burst"), ref.tags)

        val chatMsg = com.example.data.ChatMessage(
            id = "m1",
            refType = ref.refType,
            refId = ref.refId,
            refTitle = ref.title,
            refSubtitle = ref.subtitle,
            refEmoji = ref.emoji,
            refDescription = ref.description,
            refTagsJson = JsonCodecs.encodeStringList(ref.tags),
        )
        val restored = chatMsg.toEntityRefOrNull()
        assertEquals(ref.title, restored?.title)
        assertEquals(ref.description, restored?.description)
        assertEquals(listOf("Radiant", "Melee", "Burst"), restored?.tags)
    }
}
