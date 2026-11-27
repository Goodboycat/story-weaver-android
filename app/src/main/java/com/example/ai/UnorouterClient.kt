package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

data class ParsedCurlConfig(
    val endpointUrl: String? = null,
    val apiKey: String? = null,
    val model: String? = null,
)

object UnorouterClient {
    private const val TAG = "UnorouterClient"
    private const val PLACEHOLDER_UNOROUTER = "UNOROUTER_API_KEY_DEFAULT_VALUE"
    private const val PLACEHOLDER_OPENROUTER = "OPENROUTER_API_KEY_DEFAULT_VALUE"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    fun hasBuildConfigKey(): Boolean {
        val key = BuildConfig.UNOROUTER_API_KEY.trim()
        return key.isNotEmpty() && key != PLACEHOLDER_UNOROUTER
    }

    fun hasOpenRouterBuildConfigKey(): Boolean {
        val key = BuildConfig.OPENROUTER_API_KEY.trim()
        return key.isNotEmpty() && key != PLACEHOLDER_OPENROUTER
    }

    fun resolveApiKey(
        userApiKey: String,
        provider: AiProvider = AiProvider.UNOROUTER,
    ): String {
        val custom = userApiKey.trim()
        if (custom.isNotEmpty()) return custom
        val openRouterKey = BuildConfig.OPENROUTER_API_KEY.trim()
            .takeIf { it.isNotEmpty() && it != PLACEHOLDER_OPENROUTER }
        val unorouterKey = BuildConfig.UNOROUTER_API_KEY.trim()
            .takeIf { it.isNotEmpty() && it != PLACEHOLDER_UNOROUTER }
        return when (provider) {
            AiProvider.OPENROUTER -> openRouterKey ?: unorouterKey.orEmpty()
            AiProvider.UNOROUTER -> unorouterKey ?: openRouterKey.orEmpty()
            AiProvider.GEMINI -> ""
        }
    }

    fun resolveEndpointUrl(
        provider: AiProvider,
        rawEndpointUrl: String,
    ): String {
        val trimmed = rawEndpointUrl.trim()
        return when (provider) {
            AiProvider.OPENROUTER -> {
                if (trimmed.isEmpty() || trimmed == AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT) {
                    AiModelDefaults.DEFAULT_OPENROUTER_ENDPOINT
                } else {
                    trimmed
                }
            }
            AiProvider.UNOROUTER -> {
                if (trimmed.isEmpty() || trimmed == AiModelDefaults.DEFAULT_OPENROUTER_ENDPOINT) {
                    AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT
                } else {
                    trimmed
                }
            }
            AiProvider.GEMINI -> trimmed.ifBlank { AiModelDefaults.DEFAULT_OPENROUTER_ENDPOINT }
        }
    }

    fun isConfigured(
        userApiKey: String = "",
        provider: AiProvider = AiProvider.UNOROUTER,
    ): Boolean = resolveApiKey(userApiKey, provider).isNotEmpty()

    fun isValidEndpointUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (!trimmed.startsWith("https://")) return false
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return false
        return !uri.host.isNullOrBlank()
    }

    fun parseCurlCommand(curlText: String): ParsedCurlConfig? {
        val normalized = curlText.replace("\\\r\n", " ").replace("\\\n", " ").trim()
        if (normalized.isEmpty()) return null

        val urlMatch = Regex("""https://[^\s'"\\]+""").find(normalized)?.value
        val bearerMatch = Regex(
            """Authorization\s*:\s*Bearer\s+([^\s'"\\]+)""",
            RegexOption.IGNORE_CASE
        ).find(normalized)?.groupValues?.getOrNull(1)
        val modelMatch = Regex(
            """"model"\s*:\s*"([^"]+)"""",
            RegexOption.IGNORE_CASE
        ).find(normalized)?.groupValues?.getOrNull(1)

        if (urlMatch == null && bearerMatch == null && modelMatch == null) return null
        return ParsedCurlConfig(
            endpointUrl = urlMatch?.trim(),
            apiKey = bearerMatch?.trim(),
            model = modelMatch?.trim(),
        )
    }

    suspend fun sendChatCompletion(
        model: String,
        messages: List<Pair<String, String>>,
        provider: AiProvider = AiProvider.UNOROUTER,
        endpointUrl: String = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT,
        apiKeyOverride: String = "",
        temperature: Double = 0.7,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resolvedUrl = resolveEndpointUrl(provider, endpointUrl)
            if (!isValidEndpointUrl(resolvedUrl)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid API endpoint URL. It must start with https://.")
                )
            }

            val apiKey = resolveApiKey(apiKeyOverride, provider)
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "No API key configured. Open Settings or the Model & API panel to enter your UnoRouter API key."
                    )
                )
            }

            val fallbackModel = if (provider == AiProvider.OPENROUTER) {
                AiModelDefaults.DEFAULT_OPENROUTER_MODEL
            } else {
                AiModelDefaults.DEFAULT_UNOROUTER_MODEL
            }
            val cleanModel = model.trim().ifBlank { fallbackModel }

            val messagesArray = JSONArray()
            messages.forEach { (role, content) ->
                if (content.isNotBlank()) {
                    messagesArray.put(
                        JSONObject().apply {
                            put("role", role)
                            put("content", content)
                        }
                    )
                }
            }

            val payload = JSONObject().apply {
                put("model", cleanModel)
                put("messages", messagesArray)
                put("temperature", temperature)
            }

            val requestBuilder = Request.Builder()
                .url(resolvedUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
            if (resolvedUrl.contains("openrouter.ai", ignoreCase = true)) {
                requestBuilder
                    .addHeader("HTTP-Referer", "https://storyweaver.app")
                    .addHeader("X-Title", "Story Weaver")
            }

            val request = requestBuilder
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val apiErr = runCatching {
                        val errObj = JSONObject(rawBody)
                        errObj.optJSONObject("error")?.optString("message")
                            ?.takeIf { it.isNotBlank() }
                            ?: errObj.optString("message").takeIf { it.isNotBlank() }
                    }.getOrNull()
                    val msg = apiErr ?: "HTTP ${response.code} from API endpoint"
                    val lowerMsg = msg.lowercase()
                    val formatted = if (
                        lowerMsg.contains("image") ||
                        lowerMsg.contains("modalit") ||
                        lowerMsg.contains("text")
                    ) {
                        "Model \"$cleanModel\" rejected text output ($msg). If this is an image model, switch Chat mode to Image or choose a text model such as ${AiModelDefaults.DEFAULT_UNOROUTER_MODEL}."
                    } else {
                        msg
                    }
                    return@withContext Result.failure(IllegalStateException(formatted))
                }

                val root = JSONObject(rawBody)
                val choices = root.optJSONArray("choices")
                val firstChoice = choices?.optJSONObject(0)
                val messageObj = firstChoice?.optJSONObject("message")
                val imagesArr = messageObj?.optJSONArray("images")
                val hasImages = imagesArr != null && imagesArr.length() > 0
                val dataArr = root.optJSONArray("data")
                val hasImageData = dataArr != null && dataArr.length() > 0

                val content = messageObj?.optString("content")?.trim().orEmpty()
                val isRawImageDataUri = content.startsWith("data:image/", ignoreCase = true)

                if ((hasImages && content.isBlank()) || hasImageData || isRawImageDataUri) {
                    return@withContext Result.failure(
                        IllegalStateException(
                            "Model \"$cleanModel\" is an image-generation model and returned image output instead of text. Switch Chat mode to Image, or select a text model such as ${AiModelDefaults.DEFAULT_UNOROUTER_MODEL}."
                        )
                    )
                }

                if (content.isBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("API returned an empty text response for model \"$cleanModel\".")
                    )
                }
                Result.success(content)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "API chat completion failed", e)
            Result.failure(
                IllegalStateException(e.localizedMessage ?: "Failed to reach API endpoint.")
            )
        }
    }

    fun extractJsonObject(rawText: String): String {
        val trimmed = rawText.trim()
        val fenceRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val fenced = fenceRegex.find(trimmed)?.groupValues?.getOrNull(1)?.trim()
        val candidate = fenced ?: trimmed
        val firstBrace = candidate.indexOf('{')
        val lastBrace = candidate.lastIndexOf('}')
        return if (firstBrace >= 0 && lastBrace > firstBrace) {
            candidate.substring(firstBrace, lastBrace + 1)
        } else {
            candidate
        }
    }
}
