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
    private const val PLACEHOLDER_VALUE = "UNOROUTER_API_KEY_DEFAULT_VALUE"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun hasBuildConfigKey(): Boolean {
        val key = BuildConfig.UNOROUTER_API_KEY.trim()
        return key.isNotEmpty() && key != PLACEHOLDER_VALUE
    }

    fun resolveApiKey(userApiKey: String): String {
        val custom = userApiKey.trim()
        if (custom.isNotEmpty()) return custom
        val buildKey = BuildConfig.UNOROUTER_API_KEY.trim()
        return if (buildKey != PLACEHOLDER_VALUE) buildKey else ""
    }

    fun isConfigured(userApiKey: String = ""): Boolean =
        resolveApiKey(userApiKey).isNotEmpty()

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
        endpointUrl: String = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT,
        apiKeyOverride: String = "",
        temperature: Double = 0.7,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resolvedUrl = endpointUrl.trim().ifBlank { AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT }
            if (!isValidEndpointUrl(resolvedUrl)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid API endpoint URL. It must start with https://.")
                )
            }

            val apiKey = resolveApiKey(apiKeyOverride)
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "No API key configured. Open Settings to enter your API key (or set UNOROUTER_API_KEY in AI Studio Secrets), or switch to Gemini (Firebase AI)."
                    )
                )
            }

            val cleanModel = model.trim().ifBlank { AiModelDefaults.DEFAULT_UNOROUTER_MODEL }
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

            val request = Request.Builder()
                .url(resolvedUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
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
                    return@withContext Result.failure(IllegalStateException(msg))
                }

                val root = JSONObject(rawBody)
                val choices = root.optJSONArray("choices")
                val firstChoice = choices?.optJSONObject(0)
                val messageObj = firstChoice?.optJSONObject("message")
                val content = messageObj?.optString("content")?.trim()
                if (content.isNullOrBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("API returned an empty response for model \"$cleanModel\".")
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
