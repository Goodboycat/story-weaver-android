package com.example

import com.example.ai.AiImageGeneratorService
import com.example.ai.AiModelDefaults
import com.example.ai.AiPromptTemplates
import com.example.ai.AiProvider
import com.example.ai.EmojiHelper
import com.example.ai.UnorouterClient
import com.example.data.AiMapTokenSpec
import com.example.data.EquipmentSlot
import com.example.data.GridCell
import com.example.data.MapColorPalette
import com.example.data.MapPaintEngine
import com.example.data.MapPaintOp
import com.example.data.MapPaintScript
import com.example.ui.components.EmojiCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiClientUnitTest {

    @Test
    fun `extractJsonObject strips markdown code fences cleanly`() {
        val raw = """
            Here is your JSON:
            ```json
            {"items":[{"name":"Goblin","hpMax":12}]}
            ```
        """.trimIndent()
        val extracted = UnorouterClient.extractJsonObject(raw)
        assertEquals("""{"items":[{"name":"Goblin","hpMax":12}]}""", extracted)
    }

    @Test
    fun `default provider models match expected presets`() {
        assertEquals("nemotron-3-ultra-550b-a55b:free", AiModelDefaults.DEFAULT_UNOROUTER_MODEL)
        assertEquals("https://api.unorouter.com/v1/chat/completions", AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT)
        assertTrue(AiModelDefaults.presetUnorouterModels.contains("nemotron-3-ultra-550b-a55b:free"))
        assertTrue(AiModelDefaults.presetUnorouterModels.contains("gpt-6.1-sol:free"))

        assertEquals("https://openrouter.ai/api/v1/chat/completions", AiModelDefaults.DEFAULT_OPENROUTER_ENDPOINT)
        assertEquals("absolutereality:free", AiModelDefaults.DEFAULT_IMAGE_MODEL)
        assertTrue(AiModelDefaults.presetImageModels.contains("absolutereality:free"))
        assertEquals(AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT, AiModelDefaults.DEFAULT_IMAGE_ENDPOINT)
        assertTrue(AiModelDefaults.presetWebsiteEndpoints.any { it.second == AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT })
        assertTrue(AiModelDefaults.presetWebsiteEndpoints.any { it.second == AiModelDefaults.DEFAULT_OPENROUTER_ENDPOINT })
        assertEquals(AiProvider.OPENROUTER, AiProvider.fromKey("openrouter"))
        assertEquals(AiProvider.UNOROUTER, AiProvider.fromKey("unorouter"))
        assertEquals(AiProvider.GEMINI, AiProvider.fromKey("gemini"))
    }

    @Test
    fun `parseCurlCommand extracts endpoint url, bearer token, and model`() {
        val sampleCurl = """
            curl https://api.unorouter.com/v1/chat/completions \
              -H "Authorization: Bearer sk-test-dummy-token-12345" \
              -H "Content-Type: application/json" \
              -d '{
                "model": "k-exaone-2.5:free",
                "messages": [{"role": "user", "content": "Hello!"}]
              }'
        """.trimIndent()

        val parsed = UnorouterClient.parseCurlCommand(sampleCurl)
        assertNotNull(parsed)
        assertEquals("https://api.unorouter.com/v1/chat/completions", parsed?.endpointUrl)
        assertEquals("sk-test-dummy-token-12345", parsed?.apiKey)
        assertEquals("k-exaone-2.5:free", parsed?.model)
    }

    @Test
    fun `parseCurlCommand returns null for empty or unrecognized input`() {
        assertNull(UnorouterClient.parseCurlCommand(""))
        assertNull(UnorouterClient.parseCurlCommand("hello world without url or headers"))
    }

    @Test
    fun `isValidEndpointUrl accepts https urls and rejects insecure or malformed urls`() {
        assertTrue(UnorouterClient.isValidEndpointUrl("https://api.unorouter.com/v1/chat/completions"))
        assertTrue(UnorouterClient.isValidEndpointUrl("https://openrouter.ai/api/v1/chat/completions"))
        assertFalse(UnorouterClient.isValidEndpointUrl("http://api.unorouter.com/v1/chat/completions"))
        assertFalse(UnorouterClient.isValidEndpointUrl("not-a-url"))
        assertFalse(UnorouterClient.isValidEndpointUrl(""))
    }

    @Test
    fun `resolveApiKey prefers user override when provided`() {
        val resolved = UnorouterClient.resolveApiKey("  sk-user-override-999  ")
        assertEquals("sk-user-override-999", resolved)
        assertTrue(UnorouterClient.isConfigured("sk-user-override-999"))
    }

    @Test
    fun `resolveImageApiKey prioritizes image override over chat override`() {
        val resolved = AiImageGeneratorService.resolveImageApiKey(
            imageApiKeyOverride = "  sk-or-image-key  ",
            chatApiKeyOverride = "sk-chat-fallback",
            endpointUrl = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT
        )
        assertEquals("sk-or-image-key", resolved)
        assertTrue(AiImageGeneratorService.isConfigured(imageApiKeyOverride = "sk-or-image-key"))

        val chatFallback = AiImageGeneratorService.resolveImageApiKey(
            imageApiKeyOverride = "",
            chatApiKeyOverride = "sk-unorouter-chat-key",
            endpointUrl = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT
        )
        assertEquals("sk-unorouter-chat-key", chatFallback)
        assertTrue(com.example.ai.SvgCodeToImageUtil.containsSvgMarkup("<svg viewBox=\"0 0 100 100\"><circle cx=\"50\" cy=\"50\" r=\"20\"/></svg>"))
    }

    @Test
    fun `parser instructions include all 12 equipment slot symbols and parser markers`() {
        val instructions = AiPromptTemplates.parserSystemInstructions("Equipment")
        EquipmentSlot.entries.forEach { slot ->
            assertTrue("Missing slot symbol ${slot.key} in parser instructions", instructions.contains(slot.key))
        }
        assertTrue(instructions.contains("Powers:"))
        assertTrue(instructions.contains("Gear:"))
        assertTrue(instructions.contains("Stats:"))
        assertTrue(instructions.contains("Subtitle:"))
        assertTrue(instructions.contains("Tags:"))
        assertTrue(instructions.contains("Emoji:"))
        assertTrue(instructions.contains("Active section focus: Equipment"))
    }

    @Test
    fun `emoji helper extracts and strips leading emojis accurately`() {
        assertEquals("🐉", EmojiHelper.extractLeadingEmoji("# 🐉 Elder Red Dragon"))
        assertEquals("Elder Red Dragon", EmojiHelper.stripLeadingEmoji("# 🐉 Elder Red Dragon"))
        assertEquals("⚔️", EmojiHelper.extractLeadingEmoji("⚔️ Sunblade"))
        assertEquals("Sunblade", EmojiHelper.stripLeadingEmoji("⚔️ Sunblade"))
        assertNull(EmojiHelper.extractLeadingEmoji("Plain Title"))
        assertEquals("🔥", EmojiHelper.suggestEmoji("Ancient Fire Dragon"))
    }

    @Test
    fun `emoji catalog contains unique symbols across categories and all view`() {
        val all = EmojiCatalog.allUniqueEntries
        assertTrue(all.size > 400)
        assertEquals(all.size, all.map { it.symbol }.distinct().size)
        EmojiCatalog.categories.forEachIndexed { idx, cat ->
            val filtered = EmojiCatalog.filterEntries(idx + 1, "")
            assertEquals(
                "Duplicate symbols in category ${cat.id}",
                filtered.size,
                filtered.map { it.symbol }.distinct().size
            )
        }
        val dragonResults = EmojiCatalog.filterEntries(0, "dragon")
        assertTrue(dragonResults.any { it.symbol == "🐉" })
    }

    @Test
    fun `map color palette has expanded swatches and normalizes custom or named hexes`() {
        assertTrue(MapColorPalette.swatches.size >= 36)
        assertEquals("#7C3AED", MapColorPalette.normalizeColorHex("#7c3aed"))
        assertEquals("#AABBCC", MapColorPalette.normalizeColorHex("abc"))
        assertEquals("#1E293B", MapColorPalette.normalizeColorHex("1e293b"))
        assertEquals("#14532D", MapColorPalette.normalizeColorHex("forest"))
        assertEquals("#EA580C", MapColorPalette.normalizeColorHex("lava"))
        assertNull(MapColorPalette.normalizeColorHex("transparent"))
        assertNull(MapColorPalette.normalizeColorHex("none"))
        assertNull(MapColorPalette.normalizeColorHex("not-a-color"))
    }

    @Test
    fun `map paint engine executes rect border line flood fill circle erase and tokens`() {
        val script = MapPaintScript(
            width = 10,
            height = 10,
            clearFirst = true,
            summary = "Dungeon Courtyard",
            operations = listOf(
                MapPaintOp.Rect(x1 = 0, y1 = 0, x2 = 9, y2 = 9, colorHex = "#5C4033"),
                MapPaintOp.RectBorder(x1 = 0, y1 = 0, x2 = 9, y2 = 9, colorHex = "#334155", emoji = "🧱"),
                MapPaintOp.Erase(x1 = 4, y1 = 0, x2 = 5, y2 = 0),
                MapPaintOp.Line(x1 = 4, y1 = 1, x2 = 4, y2 = 8, colorHex = "#0284C7", emoji = "🌊"),
                MapPaintOp.Circle(cx = 7, cy = 7, radius = 1, colorHex = "#7C3AED", emoji = "✨", filled = true),
                MapPaintOp.Paint(points = listOf(2 to 2), colorHex = "#FF00AA", emoji = "🔥"),
                MapPaintOp.FloodFill(x = 1, y = 1, colorHex = "#14532D", emoji = "🌲"),
            ),
            tokens = listOf(
                AiMapTokenSpec(label = "Guard", emoji = "🛡️", kind = "npc", x = 4, y = 1)
            )
        )

        val outcome = MapPaintEngine.executeScript(
            script = script,
            currentWidth = 12,
            currentHeight = 12,
            currentGrid = mapOf("11,11" to GridCell(colorHex = "#000000")),
            currentTokens = emptyList(),
            ownerId = "gm_1",
        )

        assertEquals(10, outcome.width)
        assertEquals(10, outcome.height)
        assertNull(outcome.grid["4,0"]) // Doorway erased
        assertEquals("🧱", outcome.grid["0,0"]?.emoji)
        assertEquals("#334155", outcome.grid["0,0"]?.colorHex)
        assertEquals("🌊", outcome.grid["4,5"]?.emoji)
        assertEquals("✨", outcome.grid["7,7"]?.emoji)
        assertEquals("#FF00AA", outcome.grid["2,2"]?.colorHex)
        assertEquals("🌲", outcome.grid["1,1"]?.emoji)
        assertEquals(1, outcome.tokens.size)
        assertEquals("Guard", outcome.tokens.first().label)
    }

    @Test
    fun `map architect prompt describes painting tools and arbitrary hex support`() {
        val sys = AiPromptTemplates.mapArchitectSystemPrompt()
        assertTrue(sys.contains("rect_border"))
        assertTrue(sys.contains("flood_fill"))
        assertTrue(sys.contains("scatter"))
        assertTrue(sys.contains("checker"))
        assertTrue(sys.contains("biome"))
        assertTrue(sys.contains("building"))
        assertTrue(sys.contains("kingdom"))
        assertTrue(sys.contains("#RRGGBB"))
    }
}
