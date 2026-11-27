package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ai.AiModelDefaults
import com.example.ai.AiProvider
import com.example.ai.UnorouterClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.storyWeaverDataStore by preferencesDataStore(name = "story_weaver_prefs")

enum class ThemePreset(val key: String, val label: String) {
    DARK("dark", "Obsidian Dark"),
    LIGHT("light", "Parchment Light"),
    MIDNIGHT("midnight", "Midnight Arcane"),
    FOREST("forest", "Emerald Forest"),
    SUNSET("sunset", "Dragon Sunset"),
    ;

    companion object {
        fun fromKey(key: String?): ThemePreset =
            entries.firstOrNull { it.key == key } ?: DARK
    }
}

enum class TextScalePreset(val key: String, val label: String, val factor: Float) {
    SMALL("small", "Compact", 0.90f),
    MEDIUM("medium", "Standard", 1.0f),
    LARGE("large", "Large", 1.14f),
    XLARGE("xlarge", "Extra Large", 1.28f),
    ;

    companion object {
        fun fromKey(key: String?): TextScalePreset =
            entries.firstOrNull { it.key == key } ?: MEDIUM
    }
}

data class UserSettings(
    val themePreset: ThemePreset = ThemePreset.DARK,
    val customAccentArgb: Long? = null,
    val textScale: TextScalePreset = TextScalePreset.MEDIUM,
    val languageCode: String = "en",
    val aiProvider: AiProvider = AiProvider.UNOROUTER,
    val aiModel: String = AiModelDefaults.DEFAULT_UNOROUTER_MODEL,
    val aiEndpointUrl: String = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT,
    val aiApiKey: String = "",
    val aiImageEndpointUrl: String = AiModelDefaults.DEFAULT_IMAGE_ENDPOINT,
    val aiImageModel: String = AiModelDefaults.DEFAULT_IMAGE_MODEL,
    val aiImageApiKey: String = "",
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val CUSTOM_ACCENT = longPreferencesKey("custom_accent_argb")
        val TEXT_SCALE = stringPreferencesKey("text_scale")
        val LANGUAGE = stringPreferencesKey("language_code")
        val QUICK_MAP_ASSETS = stringPreferencesKey("quick_map_assets_json")
        val QUICK_MAP_BLOCKS = stringPreferencesKey("quick_map_blocks_json")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_ENDPOINT_URL = stringPreferencesKey("ai_endpoint_url")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_IMAGE_ENDPOINT_URL = stringPreferencesKey("ai_image_endpoint_url")
        val AI_IMAGE_MODEL = stringPreferencesKey("ai_image_model")
        val AI_IMAGE_API_KEY = stringPreferencesKey("ai_image_api_key")
    }

    private val brokenTextModels = setOf(
        "k-exaone-2.5:free",
        "gemini-3-pro:free",
        "gemini-3.8-flash",
        "google/gemini-2.0-flash-exp:free",
    )

    private val brokenImageModels = setOf(
        "gemini-3-pro:free",
    )

    val settingsFlow: Flow<UserSettings> = context.storyWeaverDataStore.data.map { prefs ->
        val storedKey = prefs[Keys.AI_API_KEY].orEmpty()
        val provider = prefs[Keys.AI_PROVIDER]?.let { AiProvider.fromKey(it) } ?: AiProvider.UNOROUTER
        val defaultModel = when (provider) {
            AiProvider.OPENROUTER -> AiModelDefaults.DEFAULT_OPENROUTER_MODEL
            AiProvider.UNOROUTER -> AiModelDefaults.DEFAULT_UNOROUTER_MODEL
            AiProvider.GEMINI -> AiModelDefaults.DEFAULT_GEMINI_MODEL
        }
        val defaultEndpoint = if (provider == AiProvider.OPENROUTER) {
            AiModelDefaults.DEFAULT_OPENROUTER_ENDPOINT
        } else {
            AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT
        }
        val rawModel = prefs[Keys.AI_MODEL]?.trim().orEmpty()
        val resolvedModel = when {
            rawModel.isEmpty() -> defaultModel
            rawModel in brokenTextModels && provider != AiProvider.GEMINI -> AiModelDefaults.DEFAULT_UNOROUTER_MODEL
            else -> rawModel
        }
        val rawImageModel = prefs[Keys.AI_IMAGE_MODEL]?.trim().orEmpty()
        val resolvedImageModel = when {
            rawImageModel.isEmpty() || rawImageModel in brokenImageModels -> AiModelDefaults.DEFAULT_IMAGE_MODEL
            else -> rawImageModel
        }
        UserSettings(
            themePreset = ThemePreset.fromKey(prefs[Keys.THEME_PRESET]),
            customAccentArgb = prefs[Keys.CUSTOM_ACCENT]?.takeIf { it != 0L },
            textScale = TextScalePreset.fromKey(prefs[Keys.TEXT_SCALE]),
            languageCode = prefs[Keys.LANGUAGE] ?: "en",
            aiProvider = provider,
            aiModel = resolvedModel,
            aiEndpointUrl = prefs[Keys.AI_ENDPOINT_URL]?.takeIf { it.isNotBlank() } ?: defaultEndpoint,
            aiApiKey = storedKey,
            aiImageEndpointUrl = prefs[Keys.AI_IMAGE_ENDPOINT_URL]?.takeIf { it.isNotBlank() }
                ?: AiModelDefaults.DEFAULT_IMAGE_ENDPOINT,
            aiImageModel = resolvedImageModel,
            aiImageApiKey = prefs[Keys.AI_IMAGE_API_KEY].orEmpty(),
        )
    }

    val quickMapAssetsFlow: Flow<List<QuickMapAsset>> = context.storyWeaverDataStore.data.map { prefs ->
        JsonCodecs.decodeQuickAssets(prefs[Keys.QUICK_MAP_ASSETS] ?: "[]")
    }

    val quickMapBlocksFlow: Flow<List<QuickMapBlock>> = context.storyWeaverDataStore.data.map { prefs ->
        JsonCodecs.decodeQuickBlocks(prefs[Keys.QUICK_MAP_BLOCKS] ?: "[]")
    }

    suspend fun setThemePreset(preset: ThemePreset) {
        context.storyWeaverDataStore.edit { it[Keys.THEME_PRESET] = preset.key }
    }

    suspend fun setCustomAccent(argb: Long?) {
        context.storyWeaverDataStore.edit { prefs ->
            if (argb == null) prefs.remove(Keys.CUSTOM_ACCENT)
            else prefs[Keys.CUSTOM_ACCENT] = argb
        }
    }

    suspend fun setTextScale(scale: TextScalePreset) {
        context.storyWeaverDataStore.edit { it[Keys.TEXT_SCALE] = scale.key }
    }

    suspend fun setLanguage(code: String) {
        context.storyWeaverDataStore.edit { it[Keys.LANGUAGE] = code }
    }

    suspend fun setAiProvider(provider: AiProvider) {
        context.storyWeaverDataStore.edit { prefs ->
            prefs[Keys.AI_PROVIDER] = provider.key
            val currentModel = prefs[Keys.AI_MODEL].orEmpty()
            val currentEndpoint = prefs[Keys.AI_ENDPOINT_URL].orEmpty()
            when (provider) {
                AiProvider.OPENROUTER -> {
                    if (currentModel.isBlank() || currentModel in brokenTextModels || currentModel.startsWith("gemini-")) {
                        prefs[Keys.AI_MODEL] = AiModelDefaults.DEFAULT_OPENROUTER_MODEL
                    }
                    if (currentEndpoint.isBlank() || currentEndpoint == AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT) {
                        prefs[Keys.AI_ENDPOINT_URL] = AiModelDefaults.DEFAULT_OPENROUTER_ENDPOINT
                    }
                }
                AiProvider.UNOROUTER -> {
                    if (currentModel.isBlank() || currentModel in brokenTextModels || currentModel.startsWith("gemini-")) {
                        prefs[Keys.AI_MODEL] = AiModelDefaults.DEFAULT_UNOROUTER_MODEL
                    }
                    if (currentEndpoint.isBlank() || currentEndpoint == AiModelDefaults.DEFAULT_OPENROUTER_ENDPOINT) {
                        prefs[Keys.AI_ENDPOINT_URL] = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT
                    }
                }
                AiProvider.GEMINI -> {
                    if (currentModel.isBlank() || !currentModel.startsWith("gemini-")) {
                        prefs[Keys.AI_MODEL] = AiModelDefaults.DEFAULT_GEMINI_MODEL
                    }
                }
            }
        }
    }

    suspend fun setAiModel(model: String) {
        val clean = model.trim()
        if (clean.isEmpty()) return
        context.storyWeaverDataStore.edit { it[Keys.AI_MODEL] = clean }
    }

    suspend fun setAiEndpointUrl(url: String) {
        val clean = url.trim().ifBlank { AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT }
        context.storyWeaverDataStore.edit { it[Keys.AI_ENDPOINT_URL] = clean }
    }

    suspend fun setAiApiKey(apiKey: String) {
        val clean = apiKey.trim()
        context.storyWeaverDataStore.edit { prefs ->
            if (clean.isEmpty()) prefs.remove(Keys.AI_API_KEY)
            else prefs[Keys.AI_API_KEY] = clean
        }
    }

    suspend fun setAiImageEndpointUrl(url: String) {
        val clean = url.trim().ifBlank { AiModelDefaults.DEFAULT_IMAGE_ENDPOINT }
        context.storyWeaverDataStore.edit { it[Keys.AI_IMAGE_ENDPOINT_URL] = clean }
    }

    suspend fun setAiImageModel(model: String) {
        val clean = model.trim()
        if (clean.isEmpty()) return
        context.storyWeaverDataStore.edit { it[Keys.AI_IMAGE_MODEL] = clean }
    }

    suspend fun setAiImageApiKey(apiKey: String) {
        val clean = apiKey.trim()
        context.storyWeaverDataStore.edit { prefs ->
            if (clean.isEmpty()) prefs.remove(Keys.AI_IMAGE_API_KEY)
            else prefs[Keys.AI_IMAGE_API_KEY] = clean
        }
    }

    suspend fun saveQuickMapAssets(assets: List<QuickMapAsset>) {
        context.storyWeaverDataStore.edit {
            it[Keys.QUICK_MAP_ASSETS] = JsonCodecs.encodeQuickAssets(assets.take(16))
        }
    }

    suspend fun saveQuickMapBlocks(blocks: List<QuickMapBlock>) {
        context.storyWeaverDataStore.edit {
            it[Keys.QUICK_MAP_BLOCKS] = JsonCodecs.encodeQuickBlocks(blocks.take(6))
        }
    }
}
