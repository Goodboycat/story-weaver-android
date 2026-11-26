package com.example.ai

import java.util.UUID

enum class AiProvider(val key: String, val label: String) {
    UNOROUTER("unorouter", "Unorouter / Custom API"),
    GEMINI("gemini", "Gemini (Firebase AI)"),
    ;

    companion object {
        fun fromKey(key: String?): AiProvider =
            entries.firstOrNull { it.key == key } ?: UNOROUTER
    }
}

object AiModelDefaults {
    const val DEFAULT_UNOROUTER_ENDPOINT = "https://api.unorouter.com/v1/chat/completions"
    const val DEFAULT_UNOROUTER_MODEL = "k-exaone-2.5:free"
    const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"

    val presetUnorouterModels = listOf(
        "k-exaone-2.5:free",
        "deepseek/deepseek-chat",
        "meta-llama/llama-3.3-70b-instruct:free",
        "qwen/qwen-2.5-72b-instruct:free",
        "openai/gpt-4o-mini",
    )

    val presetGeminiModels = listOf(
        "gemini-3.8-flash",
        "gemini-2.5-flash",
        "gemini-2.5-pro",
    )
}

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" | "assistant" | "system"
    val content: String,
    val modelUsed: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
)

data class AiChatUiState(
    val messages: List<AiChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
