package com.example

import com.example.ai.AiPromptTemplates
import com.example.data.AiMapTokenSpec
import com.example.data.GridCell
import com.example.data.MAP_DEFAULT_SIZE
import com.example.data.MAP_MIN_SIZE
import com.example.data.MapPaintEngine
import com.example.data.MapPaintOp
import com.example.data.MapPaintScript
import com.example.data.MapToken
import com.example.data.SavedMap
import com.example.data.Story
import com.example.data.WorldItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapAndSyncUnitTest {

    @Test
    fun `executeScript supports grids larger than 30x30 and 64x64 without clamping`() {
        val script = MapPaintScript(
            width = 120,
            height = 90,
            clearFirst = true,
            summary = "Vast kingdom valley",
            operations = listOf(
                MapPaintOp.Rect(x1 = 0, y1 = 0, x2 = 119, y2 = 89, colorHex = "#15803D"),
                MapPaintOp.RectBorder(x1 = 70, y1 = 50, x2 = 115, y2 = 85, colorHex = "#64748B", emoji = "🧱"),
                MapPaintOp.Paint(points = listOf(110 to 80, 75 to 68), colorHex = "#CBD5E1", emoji = "🏰"),
            ),
            tokens = listOf(
                AiMapTokenSpec(label = "Guard Captain", emoji = "🛡️", kind = "npc", x = 112, y = 82)
            ),
        )

        val outcome = MapPaintEngine.executeScript(
            script = script,
            currentWidth = 12,
            currentHeight = 12,
            currentGrid = emptyMap(),
            currentTokens = emptyList(),
            ownerId = "gm_1",
        )

        assertEquals(120, outcome.width)
        assertEquals(90, outcome.height)
        assertEquals(120 * 90, outcome.grid.size)
        val citadelCell = outcome.grid["110,80"]
        assertNotNull(citadelCell)
        assertEquals("#CBD5E1", citadelCell?.colorHex)
        assertEquals("🏰", citadelCell?.emoji)
        assertEquals("🧱", outcome.grid["115,85"]?.emoji)
        assertNotNull(outcome.grid["75,68"])
        assertEquals(1, outcome.tokens.size)
        assertEquals(112, outcome.tokens.first().x)
        assertEquals(82, outcome.tokens.first().y)
    }

    @Test
    fun `executeScript with omitted width and height preserves large existing grid`() {
        val existingGrid = mapOf(
            "40,35" to GridCell(colorHex = "#15803D"),
            "68,52" to GridCell(colorHex = "#334155", emoji = "🌲"),
        )
        val script = MapPaintScript(
            width = null,
            height = null,
            clearFirst = false,
            summary = "Added watchtower",
            operations = listOf(
                MapPaintOp.Paint(points = listOf(72 to 56), colorHex = "#94A3B8", emoji = "🗼")
            ),
        )

        val outcome = MapPaintEngine.executeScript(
            script = script,
            currentWidth = 80,
            currentHeight = 60,
            currentGrid = existingGrid,
            currentTokens = emptyList(),
            ownerId = "gm_1",
        )

        assertEquals(80, outcome.width)
        assertEquals(60, outcome.height)
        assertEquals("#15803D", outcome.grid["40,35"]?.colorHex)
        assertEquals("🌲", outcome.grid["68,52"]?.emoji)
        assertEquals("🗼", outcome.grid["72,56"]?.emoji)
    }

    @Test
    fun `executeScript supports minimum 1x1 grid and line circle beyond 30x30`() {
        val tinyOutcome = MapPaintEngine.executeScript(
            script = MapPaintScript(
                width = 1,
                height = 1,
                clearFirst = true,
                operations = listOf(MapPaintOp.Paint(points = listOf(0 to 0), colorHex = "#EAB308", emoji = "⭐")),
            ),
            currentWidth = 12,
            currentHeight = 12,
            currentGrid = emptyMap(),
            currentTokens = emptyList(),
            ownerId = "gm_1",
        )
        assertEquals(1, tinyOutcome.width)
        assertEquals(1, tinyOutcome.height)
        assertEquals("⭐", tinyOutcome.grid["0,0"]?.emoji)

        val wideOutcome = MapPaintEngine.executeScript(
            script = MapPaintScript(
                width = 75,
                height = 55,
                clearFirst = true,
                operations = listOf(
                    MapPaintOp.Line(x1 = 10, y1 = 10, x2 = 70, y2 = 50, colorHex = "#0284C7", emoji = "🌊"),
                    MapPaintOp.Circle(cx = 65, cy = 45, radius = 3, colorHex = "#7C3AED", emoji = "✨", filled = true),
                ),
            ),
            currentWidth = 12,
            currentHeight = 12,
            currentGrid = emptyMap(),
            currentTokens = emptyList(),
            ownerId = "gm_1",
        )
        assertEquals(75, wideOutcome.width)
        assertEquals(55, wideOutcome.height)
        assertEquals("🌊", wideOutcome.grid["10,10"]?.emoji)
        assertEquals("🌊", wideOutcome.grid["70,50"]?.emoji)
        assertEquals("✨", wideOutcome.grid["65,45"]?.emoji)
    }

    @Test
    fun `models preserve unrestricted dimensions and location map sync fields`() {
        assertEquals(1, MAP_MIN_SIZE)
        assertEquals(12, MAP_DEFAULT_SIZE)

        val story = Story(
            id = "story_1",
            title = "Chronicles",
            mapWidth = 120L,
            mapHeight = 90L,
            activeMapId = "map_1",
            activeMapName = "Ironkeep",
            activeLocationId = "loc_1",
        )
        val location = WorldItem(
            id = "loc_1",
            storyId = "story_1",
            category = "location",
            title = "Ironkeep",
            linkedMapId = "map_1",
        )
        val savedMap = SavedMap(
            id = "map_1",
            storyId = "story_1",
            name = "Ironkeep",
            linkedLocationId = location.id,
            linkedLocationTitle = location.title,
            width = 120L,
            height = 90L,
        )
        val token = MapToken(id = "t1", label = "Scout", emoji = "🏹", kind = "player", x = 115, y = 88)

        assertEquals(savedMap.id, location.linkedMapId)
        assertEquals(location.id, savedMap.linkedLocationId)
        assertEquals(location.title, savedMap.linkedLocationTitle)
        assertEquals(savedMap.id, story.activeMapId)
        assertEquals(location.id, story.activeLocationId)
        assertEquals(120L, story.mapWidth)
        assertEquals(90L, story.mapHeight)
        assertEquals(115, token.x)
        assertEquals(88, token.y)
    }

    @Test
    fun `map architect prompt communicates unrestricted AI grid sizing`() {
        val prompt = AiPromptTemplates.mapArchitectSystemPrompt("Add a sprawling city")
        assertTrue(prompt.contains("freely choose any positive integer"))
    }
}
