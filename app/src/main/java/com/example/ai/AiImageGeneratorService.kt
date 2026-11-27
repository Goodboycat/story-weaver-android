package com.example.ai

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.ui.components.ImagePickerUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

sealed interface ImageExtractResult {
    data class DataUrlOrBase64(val raw: String) : ImageExtractResult
    data class RemoteUrl(val url: String) : ImageExtractResult
}

object AiImageGeneratorService {
    private const val TAG = "AiImageGenService"
    private const val PLACEHOLDER_OPENROUTER = "OPENROUTER_API_KEY_DEFAULT_VALUE"
    private const val PLACEHOLDER_UNOROUTER = "UNOROUTER_API_KEY_DEFAULT_VALUE"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    fun resolveImageApiKey(
        imageApiKeyOverride: String = "",
        chatApiKeyOverride: String = "",
        endpointUrl: String = AiModelDefaults.DEFAULT_IMAGE_ENDPOINT,
    ): String {
        val imgCustom = imageApiKeyOverride.trim()
        if (imgCustom.isNotEmpty()) return imgCustom

        val chatCustom = chatApiKeyOverride.trim()
        val openRouterSecret = BuildConfig.OPENROUTER_API_KEY.trim()
            .takeIf { it.isNotEmpty() && it != PLACEHOLDER_OPENROUTER }
        val unorouterSecret = BuildConfig.UNOROUTER_API_KEY.trim()
            .takeIf { it.isNotEmpty() && it != PLACEHOLDER_UNOROUTER }

        return if (endpointUrl.contains("openrouter.ai", ignoreCase = true)) {
            openRouterSecret ?: chatCustom.takeIf { it.isNotEmpty() } ?: unorouterSecret.orEmpty()
        } else {
            chatCustom.takeIf { it.isNotEmpty() } ?: unorouterSecret ?: openRouterSecret.orEmpty()
        }
    }

    fun isConfigured(
        imageApiKeyOverride: String = "",
        chatApiKeyOverride: String = "",
        endpointUrl: String = AiModelDefaults.DEFAULT_IMAGE_ENDPOINT,
    ): Boolean = resolveImageApiKey(imageApiKeyOverride, chatApiKeyOverride, endpointUrl).isNotEmpty()

    suspend fun generateImageDataUrl(
        prompt: String,
        model: String = AiModelDefaults.DEFAULT_IMAGE_MODEL,
        endpointUrl: String = AiModelDefaults.DEFAULT_IMAGE_ENDPOINT,
        imageApiKeyOverride: String = "",
        chatApiKeyOverride: String = "",
        maxDimension: Int = 320,
        quality: Int = 78,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanPrompt = prompt.trim()
            if (cleanPrompt.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Image prompt cannot be empty."))
            }

            val resolvedUrl = endpointUrl.trim().ifBlank { AiModelDefaults.DEFAULT_IMAGE_ENDPOINT }
            if (!UnorouterClient.isValidEndpointUrl(resolvedUrl)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Image endpoint URL must start with https://.")
                )
            }

            val apiKey = resolveImageApiKey(imageApiKeyOverride, chatApiKeyOverride, resolvedUrl)
            if (apiKey.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "No API key configured for $resolvedUrl. Add UNOROUTER_API_KEY / OPENROUTER_API_KEY in AI Studio Secrets or enter an API key in Settings."
                    )
                )
            }

            val cleanModel = model.trim().ifBlank { AiModelDefaults.DEFAULT_IMAGE_MODEL }
            val isImagesEndpoint = resolvedUrl.endsWith("/images/generations", ignoreCase = true)

            val rawResponse = if (isImagesEndpoint) {
                executeImagesGenerationsRequest(resolvedUrl, apiKey, cleanModel, cleanPrompt)
            } else {
                val firstTry = executeChatModalitiesRequest(
                    url = resolvedUrl,
                    apiKey = apiKey,
                    model = cleanModel,
                    prompt = cleanPrompt,
                    modalities = listOf("image", "text")
                )
                firstTry.recoverCatching { err ->
                    val msg = err.message.orEmpty().lowercase()
                    if (msg.contains("modalit") || msg.contains("text")) {
                        executeChatModalitiesRequest(
                            url = resolvedUrl,
                            apiKey = apiKey,
                            model = cleanModel,
                            prompt = cleanPrompt,
                            modalities = listOf("image")
                        ).getOrThrow()
                    } else {
                        throw err
                    }
                }
            }.getOrElse { err ->
                val rawErr = err.localizedMessage ?: "Failed to generate image."
                return@withContext Result.failure(
                    IllegalStateException(
                        "Model \"$cleanModel\" cannot generate images ($rawErr). Switch to an image model such as ${AiModelDefaults.DEFAULT_IMAGE_MODEL}, or switch Chat mode to Text."
                    )
                )
            }

            val extracted = extractImageCandidateFromResponse(rawResponse)
                ?: return@withContext Result.failure(
                    IllegalStateException(
                        "Model \"$cleanModel\" is a text model (returned text only, no image output). Switch to an image model such as ${AiModelDefaults.DEFAULT_IMAGE_MODEL}, or switch Chat mode to Text."
                    )
                )

            val rawBytes: ByteArray = when (extracted) {
                is ImageExtractResult.DataUrlOrBase64 -> {
                    val payload = if (extracted.raw.contains(",")) extracted.raw.substringAfter(",") else extracted.raw
                    Base64.decode(payload.trim(), Base64.DEFAULT)
                }
                is ImageExtractResult.RemoteUrl -> {
                    downloadImageBytes(extracted.url)
                }
            }

            val compressedDataUrl = ImagePickerUtil.bytesToResizedBase64DataUrl(
                bytes = rawBytes,
                maxDimension = maxDimension,
                quality = quality,
            ) ?: return@withContext Result.failure(
                IllegalStateException("Could not decode generated image bytes.")
            )

            Result.success(compressedDataUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(
                IllegalStateException(e.localizedMessage ?: "Failed to generate image.")
            )
        }
    }

    private fun executeChatModalitiesRequest(
        url: String,
        apiKey: String,
        model: String,
        prompt: String,
        modalities: List<String>,
    ): Result<String> {
        val payload = JSONObject().apply {
            put("model", model)
            put(
                "messages",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }
                )
            )
            put("modalities", JSONArray(modalities))
        }
        return postJson(url, apiKey, payload)
    }

    private fun executeImagesGenerationsRequest(
        url: String,
        apiKey: String,
        model: String,
        prompt: String,
    ): Result<String> {
        val payload = JSONObject().apply {
            put("model", model)
            put("prompt", prompt)
            put("n", 1)
            put("size", "512x512")
            put("response_format", "b64_json")
        }
        return postJson(url, apiKey, payload)
    }

    private fun postJson(
        url: String,
        apiKey: String,
        payload: JSONObject,
    ): Result<String> {
        val builder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
        if (url.contains("openrouter.ai", ignoreCase = true)) {
            builder
                .addHeader("HTTP-Referer", "https://storyweaver.app")
                .addHeader("X-Title", "Story Weaver")
        }
        val req = builder.post(payload.toString().toRequestBody(JSON_MEDIA_TYPE)).build()
        UnorouterClient.httpClient.newCall(req).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val apiErr = runCatching {
                    val obj = JSONObject(rawBody)
                    obj.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: obj.optString("message").takeIf { it.isNotBlank() }
                }.getOrNull()
                return Result.failure(IllegalStateException(apiErr ?: "HTTP ${response.code} from image API"))
            }
            return Result.success(rawBody)
        }
    }

    private fun downloadImageBytes(imageUrl: String): ByteArray {
        val req = Request.Builder().url(imageUrl).get().build()
        UnorouterClient.httpClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Failed to download generated image (HTTP ${response.code}).")
            }
            return response.body?.bytes()
                ?: throw IllegalStateException("Empty image download response.")
        }
    }

    fun extractImageCandidateFromResponse(rawBody: String): ImageExtractResult? {
        val root = runCatching { JSONObject(rawBody) }.getOrNull() ?: return null

        // 1. Standard OpenAI /images/generations format: data[0].b64_json or data[0].url
        val dataArr = root.optJSONArray("data")
        if (dataArr != null && dataArr.length() > 0) {
            val firstData = dataArr.optJSONObject(0)
            val b64 = firstData?.optString("b64_json")?.takeIf { it.isNotBlank() }
            if (b64 != null) return ImageExtractResult.DataUrlOrBase64(b64)
            val url = firstData?.optString("url")?.takeIf { it.isNotBlank() }
            if (url != null) {
                return if (url.startsWith("data:image/", ignoreCase = true)) {
                    ImageExtractResult.DataUrlOrBase64(url)
                } else {
                    ImageExtractResult.RemoteUrl(url)
                }
            }
        }

        // 2. OpenRouter /chat/completions format: choices[0].message.images[...]
        val firstChoice = root.optJSONArray("choices")?.optJSONObject(0) ?: return null
        val msg = firstChoice.optJSONObject("message") ?: return null

        val imagesArr = msg.optJSONArray("images")
        if (imagesArr != null && imagesArr.length() > 0) {
            for (i in 0 until imagesArr.length()) {
                val item = imagesArr.opt(i)
                when (item) {
                    is JSONObject -> {
                        val nestedUrl = item.optJSONObject("image_url")?.optString("url")
                            ?.takeIf { it.isNotBlank() }
                            ?: item.optString("url").takeIf { it.isNotBlank() }
                            ?: item.optString("b64_json").takeIf { it.isNotBlank() }
                        if (nestedUrl != null) {
                            return toExtractResult(nestedUrl)
                        }
                    }
                    is String -> if (item.isNotBlank()) return toExtractResult(item)
                }
            }
        }

        // 3. Content parts array or embedded data URL / image URL string
        val contentAny = msg.opt("content")
        if (contentAny is JSONArray) {
            for (i in 0 until contentAny.length()) {
                val part = contentAny.optJSONObject(i) ?: continue
                val url = part.optJSONObject("image_url")?.optString("url")?.takeIf { it.isNotBlank() }
                    ?: part.optString("url").takeIf { it.isNotBlank() }
                    ?: part.optString("b64_json").takeIf { it.isNotBlank() }
                if (url != null) return toExtractResult(url)
            }
        } else if (contentAny is String && contentAny.isNotBlank()) {
            val dataUriMatch = Regex("""data:image/[a-zA-Z0-9.+-]+;base64,[A-Za-z0-9+/=]+""").find(contentAny)?.value
            if (dataUriMatch != null) return ImageExtractResult.DataUrlOrBase64(dataUriMatch)

            val mdImgMatch = Regex("""!\[[^\]]*]\((https://[^)\s]+)\)""").find(contentAny)?.groupValues?.getOrNull(1)
            if (mdImgMatch != null) return ImageExtractResult.RemoteUrl(mdImgMatch)

            val bareImgUrl = Regex("""https://[^\s'"]+\.(?:png|jpg|jpeg|webp)(?:\?[^\s'"]*)?""", RegexOption.IGNORE_CASE)
                .find(contentAny)?.value
            if (bareImgUrl != null) return ImageExtractResult.RemoteUrl(bareImgUrl)
        }

        return null
    }

    private fun toExtractResult(value: String): ImageExtractResult {
        val trimmed = value.trim()
        return if (trimmed.startsWith("https://", ignoreCase = true) || trimmed.startsWith("http://", ignoreCase = true)) {
            ImageExtractResult.RemoteUrl(trimmed)
        } else {
            ImageExtractResult.DataUrlOrBase64(trimmed)
        }
    }
}
