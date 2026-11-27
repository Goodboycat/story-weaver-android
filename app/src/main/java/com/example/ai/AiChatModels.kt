package com.example.ai

import java.util.UUID

enum class AiProvider(val key: String, val label: String) {
    UNOROUTER("unorouter", "Unorouter / Custom API"),
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
    )

    val presetUnorouterModels = listOf(
        "nemotron-3-ultra-550b-a55b:free",
        "gpt-6.1-sol:free",
    )

    val presetOpenRouterModels = listOf(
        "nemotron-3-ultra-550b-a55b:free",
        "gpt-6.1-sol:free",
    )

    val presetGeminiModels = listOf(
        "gemini-2.5-flash",
        "gemini-2.5-pro",
    )

    val presetImageModels = listOf(
        "absolutereality:free",
    )
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
