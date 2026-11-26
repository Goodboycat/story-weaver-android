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
    val aiProvider: AiProvider = if (UnorouterClient.hasBuildConfigKey()) AiProvider.UNOROUTER else AiProvider.GEMINI,
    val aiModel: String = if (UnorouterClient.hasBuildConfigKey()) AiModelDefaults.DEFAULT_UNOROUTER_MODEL else AiModelDefaults.DEFAULT_GEMINI_MODEL,
    val aiEndpointUrl: String = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT,
    val aiApiKey: String = "",
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val CUSTOM_ACCENT = longPreferencesKey("custom_accent_argb")
        val TEXT_SCALE = stringPreferencesKey("text_scale")
        val LANGUAGE = stringPreferencesKey("language_code")
        val QUICK_MAP_ASSETS = stringPreferencesKey("quick_map_assets_json")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_ENDPOINT_URL = stringPreferencesKey("ai_endpoint_url")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
    }

    val settingsFlow: Flow<UserSettings> = context.storyWeaverDataStore.data.map { prefs ->
        val storedKey = prefs[Keys.AI_API_KEY].orEmpty()
        val hasAnyKey = UnorouterClient.isConfigured(storedKey)
        val defaultProvider = if (hasAnyKey) AiProvider.UNOROUTER else AiProvider.GEMINI
        val provider = prefs[Keys.AI_PROVIDER]?.let { AiProvider.fromKey(it) } ?: defaultProvider
        val defaultModel = if (provider == AiProvider.UNOROUTER) {
            AiModelDefaults.DEFAULT_UNOROUTER_MODEL
        } else {
            AiModelDefaults.DEFAULT_GEMINI_MODEL
        }
        UserSettings(
            themePreset = ThemePreset.fromKey(prefs[Keys.THEME_PRESET]),
            customAccentArgb = prefs[Keys.CUSTOM_ACCENT]?.takeIf { it != 0L },
            textScale = TextScalePreset.fromKey(prefs[Keys.TEXT_SCALE]),
            languageCode = prefs[Keys.LANGUAGE] ?: "en",
            aiProvider = provider,
            aiModel = prefs[Keys.AI_MODEL]?.takeIf { it.isNotBlank() } ?: defaultModel,
            aiEndpointUrl = prefs[Keys.AI_ENDPOINT_URL]?.takeIf { it.isNotBlank() }
                ?: AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT,
            aiApiKey = storedKey,
        )
    }

    val quickMapAssetsFlow: Flow<List<QuickMapAsset>> = context.storyWeaverDataStore.data.map { prefs ->
        JsonCodecs.decodeQuickAssets(prefs[Keys.QUICK_MAP_ASSETS] ?: "[]")
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
            if (provider == AiProvider.UNOROUTER && (currentModel.isBlank() || currentModel.startsWith("gemini"))) {
                prefs[Keys.AI_MODEL] = AiModelDefaults.DEFAULT_UNOROUTER_MODEL
            } else if (provider == AiProvider.GEMINI && (currentModel.isBlank() || !currentModel.startsWith("gemini"))) {
                prefs[Keys.AI_MODEL] = AiModelDefaults.DEFAULT_GEMINI_MODEL
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

    suspend fun saveQuickMapAssets(assets: List<QuickMapAsset>) {
        context.storyWeaverDataStore.edit {
            it[Keys.QUICK_MAP_ASSETS] = JsonCodecs.encodeQuickAssets(assets.take(16))
        }
    }
}
