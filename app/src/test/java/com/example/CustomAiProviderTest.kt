package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiModelDefaults
import com.example.ai.AiProvider
import com.example.ai.CustomAiProviderProfile
import com.example.ai.UnorouterClient
import com.example.data.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomAiProviderTest {

    @Test
    fun `modelsForSelection returns only active custom provider models and never previous provider models`() {
        val customGroq = CustomAiProviderProfile(
            id = "groq_1",
            name = "Groq",
            endpointUrl = "https://api.groq.com/openai/v1/chat/completions",
            apiKey = "gsk_test",
            selectedModel = "llama-3.3-70b-versatile",
            models = listOf("llama-3.3-70b-versatile", "mixtral-8x7b-32768"),
        )

        val groqModels = AiModelDefaults.modelsForSelection(
            provider = AiProvider.UNOROUTER,
            endpointUrl = customGroq.endpointUrl,
            activeCustomProvider = customGroq,
            currentModel = "llama-3.3-70b-versatile",
        )
        assertEquals(listOf("llama-3.3-70b-versatile", "mixtral-8x7b-32768"), groqModels)
        assertFalse(groqModels.contains(AiModelDefaults.DEFAULT_UNOROUTER_MODEL))

        // Ad-hoc custom endpoint without profile also excludes previous provider's models
        val adHocCustomModels = AiModelDefaults.modelsForSelection(
            provider = AiProvider.UNOROUTER,
            endpointUrl = "https://my-private-gateway.example.com/v1/chat/completions",
            activeCustomProvider = null,
            currentModel = "my-custom-rpg-70b",
        )
        assertEquals(listOf("my-custom-rpg-70b"), adHocCustomModels)
        assertFalse(adHocCustomModels.contains(AiModelDefaults.DEFAULT_UNOROUTER_MODEL))
    }

    @Test
    fun `parseCurlCommand supports escaped quotes, single quotes, and x-api-key headers`() {
        val escapedCurl = """
            curl https://api.groq.com/openai/v1/chat/completions \
              -H "Authorization: Bearer gsk_escaped_123" \
              -d "{\"model\": \"llama-3.3-70b-versatile\", \"messages\": []}"
        """.trimIndent()

        val parsed = UnorouterClient.parseCurlCommand(escapedCurl)
        assertNotNull(parsed)
        assertEquals("https://api.groq.com/openai/v1/chat/completions", parsed?.endpointUrl)
        assertEquals("gsk_escaped_123", parsed?.apiKey)
        assertEquals("llama-3.3-70b-versatile", parsed?.model)

        val xApiKeyCurl = """
            curl 'https://custom-proxy.example.com/v1/chat/completions' -H 'x-api-key: key_abc_999' -d '{"model": "custom-model-v2"}'
        """.trimIndent()
        val parsedX = UnorouterClient.parseCurlCommand(xApiKeyCurl)
        assertEquals("https://custom-proxy.example.com/v1/chat/completions", parsedX?.endpointUrl)
        assertEquals("key_abc_999", parsedX?.apiKey)
        assertEquals("custom-model-v2", parsedX?.model)
    }

    @Test
    fun `custom providers are isolated per signed-in user and switch cleanly with provider-specific models`() = runBlocking {
        val repo = PreferencesRepository(ApplicationProvider.getApplicationContext())

        repo.setActiveUserId("user_alpha")
        val openaiProfile = CustomAiProviderProfile(
            id = "p_openai",
            name = "OpenAI",
            endpointUrl = "https://api.openai.com/v1/chat/completions",
            apiKey = "sk-alpha-openai",
            selectedModel = "gpt-4o-mini",
            models = listOf("gpt-4o-mini", "gpt-4o"),
        )
        val deepseekProfile = CustomAiProviderProfile(
            id = "p_deepseek",
            name = "DeepSeek",
            endpointUrl = "https://api.deepseek.com/chat/completions",
            apiKey = "sk-alpha-deepseek",
            selectedModel = "deepseek-chat",
            models = listOf("deepseek-chat", "deepseek-reasoner"),
        )

        repo.saveCustomAiProvider(openaiProfile, selectImmediately = true)
        repo.saveCustomAiProvider(deepseekProfile, selectImmediately = false)

        val alphaOpenAi = repo.settingsFlow.first()
        assertEquals("p_openai", alphaOpenAi.activeCustomProviderId)
        assertEquals("gpt-4o-mini", alphaOpenAi.aiModel)
        assertEquals("https://api.openai.com/v1/chat/completions", alphaOpenAi.aiEndpointUrl)
        assertEquals(listOf("gpt-4o-mini", "gpt-4o"), AiModelDefaults.modelsForSelection(
            alphaOpenAi.aiProvider, alphaOpenAi.aiEndpointUrl, alphaOpenAi.activeCustomProvider, alphaOpenAi.aiModel
        ))

        // Adding another model while custom provider is active registers it on that provider
        repo.setAiModel("o4-mini")
        val alphaUpdated = repo.settingsFlow.first()
        assertEquals("o4-mini", alphaUpdated.aiModel)
        assertTrue(alphaUpdated.activeCustomProvider?.allModels()?.contains("o4-mini") == true)

        // Switching to second custom provider immediately switches endpoint, key, and model list
        repo.selectCustomAiProvider("p_deepseek")
        val alphaDeepSeek = repo.settingsFlow.first()
        assertEquals("p_deepseek", alphaDeepSeek.activeCustomProviderId)
        assertEquals("deepseek-chat", alphaDeepSeek.aiModel)
        assertEquals("sk-alpha-deepseek", alphaDeepSeek.aiApiKey)
        val deepseekDropdown = AiModelDefaults.modelsForSelection(
            alphaDeepSeek.aiProvider, alphaDeepSeek.aiEndpointUrl, alphaDeepSeek.activeCustomProvider, alphaDeepSeek.aiModel
        )
        assertEquals(listOf("deepseek-chat", "deepseek-reasoner"), deepseekDropdown)
        assertFalse(deepseekDropdown.contains("gpt-4o"))

        // Switching to built-in Gemini resets custom provider and restores Gemini models
        repo.setAiProvider(AiProvider.GEMINI)
        val alphaGemini = repo.settingsFlow.first()
        assertNull(alphaGemini.activeCustomProviderId)
        assertEquals(AiProvider.GEMINI, alphaGemini.aiProvider)
        assertEquals(AiModelDefaults.DEFAULT_GEMINI_MODEL, alphaGemini.aiModel)
        assertEquals(AiModelDefaults.presetGeminiModels, AiModelDefaults.modelsForSelection(
            alphaGemini.aiProvider, alphaGemini.aiEndpointUrl, alphaGemini.activeCustomProvider, alphaGemini.aiModel
        ))

        // Different authenticated user never sees user_alpha's custom APIs or keys
        repo.setActiveUserId("user_beta")
        val betaSettings = repo.settingsFlow.first()
        assertTrue(betaSettings.customAiProviders.isEmpty())
        assertEquals("", betaSettings.aiApiKey)
        assertNull(betaSettings.activeCustomProviderId)

        // Signed-out state never exposes keys or custom APIs
        repo.setActiveUserId(null)
        val signedOut = repo.settingsFlow.first()
        assertTrue(signedOut.customAiProviders.isEmpty())
        assertEquals("", signedOut.aiApiKey)
    }

    @Test
    fun `website provider API keys and models are saved per endpoint and reused when switching back`() = runBlocking {
        val repo = PreferencesRepository(ApplicationProvider.getApplicationContext())
        repo.setActiveUserId("user_keys_reuse")

        val openaiUrl = "https://api.openai.com/v1/chat/completions"
        val groqUrl = "https://api.groq.com/openai/v1/chat/completions"

        // Save API key & model for OpenAI website endpoint
        repo.setAiEndpointUrl(openaiUrl)
        repo.setAiApiKey("sk-openai-saved")
        repo.setAiModel("gpt-4o")

        // Switch to Groq website endpoint (does NOT leak OpenAI key) and save Groq key & model
        repo.setAiEndpointUrl(groqUrl)
        val afterSwitchToGroq = repo.settingsFlow.first()
        assertEquals(groqUrl, afterSwitchToGroq.aiEndpointUrl)
        assertEquals("", afterSwitchToGroq.aiApiKey)
        assertEquals("llama-3.3-70b-versatile", afterSwitchToGroq.aiModel)

        repo.setAiApiKey("gsk-groq-saved")
        repo.setAiModel("llama-3.1-8b-instant")

        // Register a custom website provider and update its key inline
        val myProxy = CustomAiProviderProfile(
            id = "p_proxy",
            name = "My Proxy",
            endpointUrl = "https://proxy.example.com/v1/chat/completions",
            apiKey = "sk-proxy-1",
            selectedModel = "proxy-70b",
            models = listOf("proxy-70b"),
        )
        repo.saveCustomAiProvider(myProxy, selectImmediately = true)
        repo.setAiApiKey("sk-proxy-updated")

        // Switching to OpenAI website endpoint again reuses its saved profile, key & model
        repo.setAiEndpointUrl(openaiUrl)
        val backToOpenAi = repo.settingsFlow.first()
        assertNotNull(backToOpenAi.activeCustomProviderId)
        assertEquals(openaiUrl, backToOpenAi.aiEndpointUrl)
        assertEquals("sk-openai-saved", backToOpenAi.aiApiKey)
        assertEquals("gpt-4o", backToOpenAi.aiModel)

        // Switching to Groq reuses Groq's saved key & model
        repo.setAiEndpointUrl(groqUrl)
        val backToGroq = repo.settingsFlow.first()
        assertEquals("gsk-groq-saved", backToGroq.aiApiKey)
        assertEquals("llama-3.1-8b-instant", backToGroq.aiModel)

        // Tapping My Proxy again reuses its intact endpoint URL and updated API key
        repo.selectCustomAiProvider("p_proxy")
        val backToProxy = repo.settingsFlow.first()
        assertEquals("p_proxy", backToProxy.activeCustomProviderId)
        assertEquals("https://proxy.example.com/v1/chat/completions", backToProxy.aiEndpointUrl)
        assertEquals("sk-proxy-updated", backToProxy.aiApiKey)
        assertEquals("proxy-70b", backToProxy.aiModel)

        // Switching back to built-in Unorouter clears custom profile cleanly
        repo.setAiEndpointUrl(AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT)
        val backToUnorouter = repo.settingsFlow.first()
        assertNull(backToUnorouter.activeCustomProviderId)
        assertEquals(AiProvider.UNOROUTER, backToUnorouter.aiProvider)
    }
}
