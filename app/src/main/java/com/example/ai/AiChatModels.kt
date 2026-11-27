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
    const val DEFAULT_UNOROUTER_MODEL = "k-exaone-2.5:free"

    const val DEFAULT_OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
    const val DEFAULT_OPENROUTER_MODEL = "google/gemini-2.0-flash-exp:free"

    const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"

    const val DEFAULT_IMAGE_ENDPOINT = DEFAULT_UNOROUTER_ENDPOINT
    const val DEFAULT_IMAGE_MODEL = "gemini-3-pro:free"

    val presetWebsiteEndpoints = listOf(
        "Unorouter" to DEFAULT_UNOROUTER_ENDPOINT,
        "OpenRouter" to DEFAULT_OPENROUTER_ENDPOINT,
        "OpenAI" to "https://api.openai.com/v1/chat/completions",
    )

    val presetUnorouterModels = listOf(
        "k-exaone-2.5:free",
        "gemini-3-pro:free",
        "deepseek/deepseek-chat",
        "meta-llama/llama-3.3-70b-instruct:free",
        "qwen/qwen-2.5-72b-instruct:free",
        "openai/gpt-4o-mini",
    )

    val presetOpenRouterModels = listOf(
        "google/gemini-2.0-flash-exp:free",
        "meta-llama/llama-3.3-70b-instruct:free",
        "deepseek/deepseek-chat",
        "qwen/qwen-2.5-72b-instruct:free",
        "openai/gpt-4o-mini",
    )

    val presetGeminiModels = listOf(
        "gemini-3.8-flash",
        "gemini-2.5-flash",
        "gemini-2.5-pro",
    )

    val presetImageModels = listOf(
        "gemini-3-pro:free",
        "google/gemini-2.5-flash-image-preview",
        "google/gemini-2.5-flash-image",
        "black-forest-labs/flux.2-flex",
        "black-forest-labs/flux.2-pro",
        "openai/gpt-5-image-mini",
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
