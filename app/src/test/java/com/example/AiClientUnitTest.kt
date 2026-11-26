package com.example

import com.example.ai.AiModelDefaults
import com.example.ai.AiProvider
import com.example.ai.UnorouterClient
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
    fun `default Unorouter model matches k-exaone-2_5 free`() {
        assertEquals("k-exaone-2.5:free", AiModelDefaults.DEFAULT_UNOROUTER_MODEL)
        assertEquals("https://api.unorouter.com/v1/chat/completions", AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT)
        assertTrue(AiModelDefaults.presetUnorouterModels.contains("k-exaone-2.5:free"))
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
}
