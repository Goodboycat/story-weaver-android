package com.example.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.UUID

enum class AiProvider(val key: String, val label: String) {
    UNOROUTER("unorouter", "Unorouter"),
    OPENROUTER("openrouter", "OpenRouter"),
    GEMINI("gemini", "Gemini (Firebase AI)"),
    ;

    companion object {
        fun fromKey(key: String?): AiProvider =
            entries.firstOrNull { it.key == key } ?: UNOROUTER
    }
}

enum class AiChatMode {
    TEXT,
    IMAGE,
}

data class CustomAiProviderProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val endpointUrl: String = "",
    val apiKey: String = "",
    val selectedModel: String = "",
    val models: List<String> = emptyList(),
) {
    fun allModels(): List<String> =
        (listOf(selectedModel) + models)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun displayHost(): String =
        runCatching { URI(endpointUrl.trim()).host.orEmpty() }
            .getOrDefault("")
            .removePrefix("www.")
}

object AiModelDefaults {
    const val DEFAULT_UNOROUTER_ENDPOINT = "https://api.unorouter.com/v1/chat/completions"
    const val DEFAULT_UNOROUTER_MODEL = "nemotron-3-ultra-550b-a55b:free"

    const val DEFAULT_OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
    const val DEFAULT_OPENROUTER_MODEL = "nemotron-3-ultra-550b-a55b:free"

    const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"

    const val DEFAULT_IMAGE_ENDPOINT = DEFAULT_UNOROUTER_ENDPOINT
    const val DEFAULT_IMAGE_MODEL = "absolutereality:free"

    val presetWebsiteEndpoints = listOf(
        "Unorouter" to DEFAULT_UNOROUTER_ENDPOINT,
        "OpenRouter" to DEFAULT_OPENROUTER_ENDPOINT,
        "OpenAI" to "https://api.openai.com/v1/chat/completions",
        "Groq" to "https://api.groq.com/openai/v1/chat/completions",
        "DeepSeek" to "https://api.deepseek.com/chat/completions",
        "Together" to "https://api.together.xyz/v1/chat/completions",
        "Mistral" to "https://api.mistral.ai/v1/chat/completions",
    )

    val presetUnorouterModels = listOf(
        "nemotron-3-ultra-550b-a55b:free",
        "gpt-6.1-sol:free",
    )

    val presetOpenRouterModels = listOf(
        "nemotron-3-ultra-550b-a55b:free",
        "gpt-6.1-sol:free",
        "openai/gpt-4o-mini",
        "anthropic/claude-3.5-sonnet",
    )

    val presetGeminiModels = listOf(
        "gemini-2.5-flash",
        "gemini-2.5-pro",
    )

    val presetImageModels = listOf(
        "absolutereality:free",
    )

    fun inferProviderNameFromUrl(endpointUrl: String): String {
        val host = runCatching { URI(endpointUrl.trim()).host.orEmpty().lowercase() }
            .getOrDefault("")
        return when {
            host.contains("openai.com") -> "OpenAI"
            host.contains("openrouter.ai") -> "OpenRouter"
            host.contains("unorouter.com") -> "Unorouter"
            host.contains("groq.com") -> "Groq"
            host.contains("deepseek.com") -> "DeepSeek"
            host.contains("together.xyz") -> "Together AI"
            host.contains("mistral.ai") -> "Mistral"
            host.contains("generativelanguage.googleapis.com") -> "Google AI"
            host.contains("fireworks.ai") -> "Fireworks"
            host.isNotBlank() -> {
                val base = host.removePrefix("api.").removePrefix("www.")
                    .substringBefore(".")
                base.replaceFirstChar { it.uppercase() }.ifBlank { "Custom API" }
            }
            else -> "Custom API"
        }
    }

    fun inferBuiltInProviderForUrl(endpointUrl: String): AiProvider {
        return if (endpointUrl.contains("openrouter.ai", ignoreCase = true)) {
            AiProvider.OPENROUTER
        } else {
            AiProvider.UNOROUTER
        }
    }

    fun defaultModelsForHost(endpointUrl: String): List<String> {
        val host = runCatching { URI(endpointUrl.trim()).host.orEmpty().lowercase() }
            .getOrDefault("")
        return when {
            host.contains("api.openai.com") -> listOf("gpt-4o-mini", "gpt-4o", "o4-mini")
            host.contains("api.groq.com") -> listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant")
            host.contains("api.deepseek.com") -> listOf("deepseek-chat", "deepseek-reasoner")
            host.contains("api.mistral.ai") -> listOf("mistral-small-latest", "mistral-large-latest")
            host.contains("openrouter.ai") -> presetOpenRouterModels
            host.contains("unorouter.com") -> presetUnorouterModels
            else -> emptyList()
        }
    }

    fun isBuiltInDefaultEndpoint(endpointUrl: String): Boolean {
        val clean = endpointUrl.trim().lowercase()
        return clean.isEmpty() ||
            clean == DEFAULT_UNOROUTER_ENDPOINT.lowercase() ||
            clean == DEFAULT_OPENROUTER_ENDPOINT.lowercase()
    }

    /**
     * Returns only the models relevant to the currently selected provider / custom API profile,
     * never leaking the previous provider's models when a custom provider is active.
     */
    fun modelsForSelection(
        provider: AiProvider,
        endpointUrl: String,
        activeCustomProvider: CustomAiProviderProfile?,
        currentModel: String,
    ): List<String> {
        val cleanCurrent = currentModel.trim()
        if (activeCustomProvider != null) {
            val profileModels = activeCustomProvider.allModels()
            return (profileModels + listOfNotNull(cleanCurrent.takeIf { it.isNotEmpty() })).distinct()
        }
        if (provider == AiProvider.GEMINI) {
            return (presetGeminiModels + listOfNotNull(
                cleanCurrent.takeIf { it.startsWith("gemini-", ignoreCase = true) }
            )).distinct()
        }
        val cleanUrl = endpointUrl.trim()
        if (!isBuiltInDefaultEndpoint(cleanUrl)) {
            val hostDefaults = defaultModelsForHost(cleanUrl)
            return (listOfNotNull(cleanCurrent.takeIf { it.isNotEmpty() }) + hostDefaults).distinct()
        }
        val base = when (provider) {
            AiProvider.OPENROUTER -> presetOpenRouterModels
            AiProvider.UNOROUTER -> presetUnorouterModels
            AiProvider.GEMINI -> presetGeminiModels
        }
        return (base + listOfNotNull(
            cleanCurrent.takeIf { it.isNotEmpty() && !it.startsWith("gemini-", ignoreCase = true) }
        )).distinct()
    }

    fun encodeCustomProviders(list: List<CustomAiProviderProfile>): String {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("endpointUrl", p.endpointUrl)
                    put("apiKey", p.apiKey)
                    put("selectedModel", p.selectedModel)
                    put("models", JSONArray(p.allModels()))
                }
            )
        }
        return arr.toString()
    }

    fun decodeCustomProviders(rawJson: String?): List<CustomAiProviderProfile> {
        val text = rawJson?.trim().orEmpty()
        if (text.isEmpty() || text == "[]") return emptyList()
        return runCatching {
            val arr = JSONArray(text)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id").trim().ifBlank { UUID.randomUUID().toString() }
                    val endpointUrl = obj.optString("endpointUrl").trim()
                    if (endpointUrl.isEmpty()) continue
                    val name = obj.optString("name").trim()
                        .ifBlank { inferProviderNameFromUrl(endpointUrl) }
                    val apiKey = obj.optString("apiKey").trim()
                    val selectedModel = obj.optString("selectedModel").trim()
                    val modelsArr = obj.optJSONArray("models")
                    val models = buildList {
                        if (modelsArr != null) {
                            for (j in 0 until modelsArr.length()) {
                                val m = modelsArr.optString(j).trim()
                                if (m.isNotEmpty()) add(m)
                            }
                        }
                        if (selectedModel.isNotEmpty()) add(selectedModel)
                    }.distinct()
                    add(
                        CustomAiProviderProfile(
                            id = id,
                            name = name,
                            endpointUrl = endpointUrl,
                            apiKey = apiKey,
                            selectedModel = selectedModel.ifBlank { models.firstOrNull().orEmpty() },
                            models = models,
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeStringMap(map: Map<String, String>): String {
        val obj = JSONObject()
        map.forEach { (k, v) ->
            if (k.isNotBlank() && v.isNotBlank()) obj.put(k, v)
        }
        return obj.toString()
    }

    fun decodeStringMap(rawJson: String?): Map<String, String> {
        val text = rawJson?.trim().orEmpty()
        if (text.isEmpty() || text == "{}") return emptyMap()
        return runCatching {
            val obj = JSONObject(text)
            val result = linkedMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k).trim()
                if (k.isNotBlank() && v.isNotEmpty()) result[k] = v
            }
            result
        }.getOrDefault(emptyMap())
    }
}

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" | "assistant" | "system"
    val content: String,
    val imageDataUrl: String? = null,
    val modelUsed: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
)

data class AiChatUiState(
    val messages: List<AiChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

