package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val CUSTOM_ACCENT = longPreferencesKey("custom_accent_argb")
        val TEXT_SCALE = stringPreferencesKey("text_scale")
        val LANGUAGE = stringPreferencesKey("language_code")
        val QUICK_MAP_ASSETS = stringPreferencesKey("quick_map_assets_json")
    }

    val settingsFlow: Flow<UserSettings> = context.storyWeaverDataStore.data.map { prefs ->
        UserSettings(
            themePreset = ThemePreset.fromKey(prefs[Keys.THEME_PRESET]),
            customAccentArgb = prefs[Keys.CUSTOM_ACCENT]?.takeIf { it != 0L },
            textScale = TextScalePreset.fromKey(prefs[Keys.TEXT_SCALE]),
            languageCode = prefs[Keys.LANGUAGE] ?: "en",
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

    suspend fun saveQuickMapAssets(assets: List<QuickMapAsset>) {
        context.storyWeaverDataStore.edit {
            it[Keys.QUICK_MAP_ASSETS] = JsonCodecs.encodeQuickAssets(assets.take(16))
        }
    }
}
