package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ai.AiModelDefaults
import com.example.ai.AiProvider
import com.example.ai.CustomAiProviderProfile
import com.example.ai.UnorouterClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

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
    val customAiProviders: List<CustomAiProviderProfile> = emptyList(),
    val activeCustomProviderId: String? = null,
    val providerModels: Map<String, String> = emptyMap(),
) {
    val activeCustomProvider: CustomAiProviderProfile?
        get() = activeCustomProviderId?.let { id -> customAiProviders.firstOrNull { it.id == id } }

    val activeProviderLabel: String
        get() = activeCustomProvider?.name?.takeIf { it.isNotBlank() } ?: aiProvider.label
}

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val CUSTOM_ACCENT = longPreferencesKey("custom_accent_argb")
        val TEXT_SCALE = stringPreferencesKey("text_scale")
        val LANGUAGE = stringPreferencesKey("language_code")
        val QUICK_MAP_ASSETS = stringPreferencesKey("quick_map_assets_json")
        val QUICK_MAP_BLOCKS = stringPreferencesKey("quick_map_blocks_json")

        // Legacy unscoped keys (migrated once to the first authenticated user, never shared across accounts)
        val LEGACY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        val LEGACY_AI_MODEL = stringPreferencesKey("ai_model")
        val LEGACY_AI_ENDPOINT_URL = stringPreferencesKey("ai_endpoint_url")
        val LEGACY_AI_API_KEY = stringPreferencesKey("ai_api_key")
        val LEGACY_AI_IMAGE_ENDPOINT_URL = stringPreferencesKey("ai_image_endpoint_url")
        val LEGACY_AI_IMAGE_MODEL = stringPreferencesKey("ai_image_model")
        val LEGACY_AI_IMAGE_API_KEY = stringPreferencesKey("ai_image_api_key")
        val LEGACY_OWNER_CLAIMED = stringPreferencesKey("legacy_ai_owner_uid")
    }

    private class UserAiKeys(uid: String) {
        private val prefix = "u_${uid}_"
        val aiProvider = stringPreferencesKey("${prefix}ai_provider")
        val aiModel = stringPreferencesKey("${prefix}ai_model")
        val aiEndpointUrl = stringPreferencesKey("${prefix}ai_endpoint_url")
        val aiApiKey = stringPreferencesKey("${prefix}ai_api_key")
        val aiImageEndpointUrl = stringPreferencesKey("${prefix}ai_image_endpoint_url")
        val aiImageModel = stringPreferencesKey("${prefix}ai_image_model")
        val aiImageApiKey = stringPreferencesKey("${prefix}ai_image_api_key")
        val customProvidersJson = stringPreferencesKey("${prefix}ai_custom_providers_json")
        val activeCustomProviderId = stringPreferencesKey("${prefix}ai_active_custom_provider_id")
        val providerModelsJson = stringPreferencesKey("${prefix}ai_provider_models_json")
        val providerKeysJson = stringPreferencesKey("${prefix}ai_provider_keys_json")
    }

    private val activeUserIdFlow = MutableStateFlow<String?>(null)

    private val brokenTextModels = setOf(
        "k-exaone-2.5:free",
        "gemini-3-pro:free",
        "gemini-3.8-flash",
        "google/gemini-2.0-flash-exp:free",
    )

    private val brokenImageModels = setOf(
        "gemini-3-pro:free",
    )

    suspend fun setActiveUserId(uid: String?) {
        val cleanUid = uid?.trim()?.takeIf { it.isNotEmpty() }
        activeUserIdFlow.value = cleanUid
        if (cleanUid != null) {
            migrateLegacyAiSettingsIfNeeded(cleanUid)
        }
    }

    private suspend fun migrateLegacyAiSettingsIfNeeded(uid: String) {
        context.storyWeaverDataStore.edit { prefs ->
            if (prefs.contains(Keys.LEGACY_OWNER_CLAIMED)) return@edit
            val hasLegacy = prefs.contains(Keys.LEGACY_AI_API_KEY) ||
                prefs.contains(Keys.LEGACY_AI_ENDPOINT_URL) ||
                prefs.contains(Keys.LEGACY_AI_MODEL) ||
                prefs.contains(Keys.LEGACY_AI_PROVIDER) ||
                prefs.contains(Keys.LEGACY_AI_IMAGE_API_KEY)
            prefs[Keys.LEGACY_OWNER_CLAIMED] = uid
            if (!hasLegacy) return@edit

            val uk = UserAiKeys(uid)
            prefs[Keys.LEGACY_AI_PROVIDER]?.let { if (!prefs.contains(uk.aiProvider)) prefs[uk.aiProvider] = it }
            prefs[Keys.LEGACY_AI_MODEL]?.let { if (!prefs.contains(uk.aiModel)) prefs[uk.aiModel] = it }
            prefs[Keys.LEGACY_AI_ENDPOINT_URL]?.let { if (!prefs.contains(uk.aiEndpointUrl)) prefs[uk.aiEndpointUrl] = it }
            prefs[Keys.LEGACY_AI_API_KEY]?.let { if (!prefs.contains(uk.aiApiKey)) prefs[uk.aiApiKey] = it }
            prefs[Keys.LEGACY_AI_IMAGE_ENDPOINT_URL]?.let { if (!prefs.contains(uk.aiImageEndpointUrl)) prefs[uk.aiImageEndpointUrl] = it }
            prefs[Keys.LEGACY_AI_IMAGE_MODEL]?.let { if (!prefs.contains(uk.aiImageModel)) prefs[uk.aiImageModel] = it }
            prefs[Keys.LEGACY_AI_IMAGE_API_KEY]?.let { if (!prefs.contains(uk.aiImageApiKey)) prefs[uk.aiImageApiKey] = it }

            prefs.remove(Keys.LEGACY_AI_PROVIDER)
            prefs.remove(Keys.LEGACY_AI_MODEL)
            prefs.remove(Keys.LEGACY_AI_ENDPOINT_URL)
            prefs.remove(Keys.LEGACY_AI_API_KEY)
            prefs.remove(Keys.LEGACY_AI_IMAGE_ENDPOINT_URL)
            prefs.remove(Keys.LEGACY_AI_IMAGE_MODEL)
            prefs.remove(Keys.LEGACY_AI_IMAGE_API_KEY)
        }
    }

    private fun defaultModelFor(provider: AiProvider): String = when (provider) {
        AiProvider.OPENROUTER -> AiModelDefaults.DEFAULT_OPENROUTER_MODEL
        AiProvider.UNOROUTER -> AiModelDefaults.DEFAULT_UNOROUTER_MODEL
        AiProvider.GEMINI -> AiModelDefaults.DEFAULT_GEMINI_MODEL
    }

    private fun defaultEndpointFor(provider: AiProvider): String = when (provider) {
        AiProvider.OPENROUTER -> AiModelDefaults.DEFAULT_OPENROUTER_ENDPOINT
        AiProvider.UNOROUTER -> AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT
        AiProvider.GEMINI -> AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT
    }

    private fun sanitizeModelForProvider(rawModel: String, provider: AiProvider): String {
        val trimmed = rawModel.trim()
        return when {
            trimmed.isEmpty() -> defaultModelFor(provider)
            trimmed in brokenTextModels && provider != AiProvider.GEMINI -> defaultModelFor(provider)
            else -> trimmed
        }
    }

    val settingsFlow: Flow<UserSettings> = combine(
        context.storyWeaverDataStore.data,
        activeUserIdFlow
    ) { prefs, uid ->
        val themePreset = ThemePreset.fromKey(prefs[Keys.THEME_PRESET])
        val customAccentArgb = prefs[Keys.CUSTOM_ACCENT]?.takeIf { it != 0L }
        val textScale = TextScalePreset.fromKey(prefs[Keys.TEXT_SCALE])
        val languageCode = prefs[Keys.LANGUAGE] ?: "en"

        if (uid.isNullOrBlank()) {
            return@combine UserSettings(
                themePreset = themePreset,
                customAccentArgb = customAccentArgb,
                textScale = textScale,
                languageCode = languageCode,
            )
        }

        val uk = UserAiKeys(uid)
        val customProviders = AiModelDefaults.decodeCustomProviders(prefs[uk.customProvidersJson])
        val rawActiveCustomId = prefs[uk.activeCustomProviderId]?.trim()?.takeIf { it.isNotEmpty() }
        val activeCustom = rawActiveCustomId?.let { id -> customProviders.firstOrNull { it.id == id } }
        val providerModels = AiModelDefaults.decodeStringMap(prefs[uk.providerModelsJson])

        val provider = activeCustom?.let { AiModelDefaults.inferBuiltInProviderForUrl(it.endpointUrl) }
            ?: prefs[uk.aiProvider]?.let { AiProvider.fromKey(it) }
            ?: AiProvider.UNOROUTER

        val rawEndpoint = activeCustom?.endpointUrl
            ?: prefs[uk.aiEndpointUrl]?.takeIf { it.isNotBlank() }
            ?: defaultEndpointFor(provider)

        val rawModel = activeCustom?.selectedModel?.takeIf { it.isNotBlank() }
            ?: prefs[uk.aiModel]?.trim().orEmpty()
        val resolvedModel = if (activeCustom != null) {
            rawModel.ifBlank { activeCustom.models.firstOrNull().orEmpty() }
        } else {
            sanitizeModelForProvider(rawModel, provider)
        }

        val resolvedApiKey = activeCustom?.apiKey ?: prefs[uk.aiApiKey].orEmpty()

        val rawImageModel = prefs[uk.aiImageModel]?.trim().orEmpty()
        val resolvedImageModel = when {
            rawImageModel.isEmpty() || rawImageModel in brokenImageModels -> AiModelDefaults.DEFAULT_IMAGE_MODEL
            else -> rawImageModel
        }

        UserSettings(
            themePreset = themePreset,
            customAccentArgb = customAccentArgb,
            textScale = textScale,
            languageCode = languageCode,
            aiProvider = provider,
            aiModel = resolvedModel,
            aiEndpointUrl = rawEndpoint,
            aiApiKey = resolvedApiKey,
            aiImageEndpointUrl = prefs[uk.aiImageEndpointUrl]?.takeIf { it.isNotBlank() }
                ?: AiModelDefaults.DEFAULT_IMAGE_ENDPOINT,
            aiImageModel = resolvedImageModel,
            aiImageApiKey = prefs[uk.aiImageApiKey].orEmpty(),
            customAiProviders = customProviders,
            activeCustomProviderId = activeCustom?.id,
            providerModels = providerModels,
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

    private suspend fun editForActiveUser(block: (MutablePreferences, UserAiKeys) -> Unit) {
        val uid = activeUserIdFlow.value ?: return
        val uk = UserAiKeys(uid)
        context.storyWeaverDataStore.edit { prefs ->
            block(prefs, uk)
        }
    }

    private fun saveCurrentBuiltInStateIfActive(prefs: MutablePreferences, uk: UserAiKeys) {
        val activeCustomId = prefs[uk.activeCustomProviderId]?.trim().orEmpty()
        if (activeCustomId.isNotEmpty()) return
        val currentProvider = AiProvider.fromKey(prefs[uk.aiProvider])
        val currentEndpoint = prefs[uk.aiEndpointUrl].orEmpty()
        if (!AiModelDefaults.isBuiltInDefaultEndpoint(currentEndpoint)) return

        val currentModel = prefs[uk.aiModel]?.trim().orEmpty()
        if (currentModel.isNotEmpty() && currentModel !in brokenTextModels) {
            val modelsMap = AiModelDefaults.decodeStringMap(prefs[uk.providerModelsJson]).toMutableMap()
            modelsMap[currentProvider.key] = currentModel
            prefs[uk.providerModelsJson] = AiModelDefaults.encodeStringMap(modelsMap)
        }

        val currentKey = prefs[uk.aiApiKey]?.trim().orEmpty()
        val keysMap = AiModelDefaults.decodeStringMap(prefs[uk.providerKeysJson]).toMutableMap()
        if (currentKey.isNotEmpty()) keysMap[currentProvider.key] = currentKey else keysMap.remove(currentProvider.key)
        prefs[uk.providerKeysJson] = AiModelDefaults.encodeStringMap(keysMap)
    }

    suspend fun setAiProvider(provider: AiProvider) {
        editForActiveUser { prefs, uk ->
            saveCurrentBuiltInStateIfActive(prefs, uk)
            prefs.remove(uk.activeCustomProviderId)
            prefs[uk.aiProvider] = provider.key
            prefs[uk.aiEndpointUrl] = defaultEndpointFor(provider)

            val modelsMap = AiModelDefaults.decodeStringMap(prefs[uk.providerModelsJson])
            val savedModel = modelsMap[provider.key].orEmpty()
            val nextModel = when (provider) {
                AiProvider.GEMINI -> {
                    if (savedModel.startsWith("gemini-", ignoreCase = true)) savedModel
                    else AiModelDefaults.DEFAULT_GEMINI_MODEL
                }
                AiProvider.OPENROUTER, AiProvider.UNOROUTER -> {
                    if (savedModel.isNotBlank() && savedModel !in brokenTextModels && !savedModel.startsWith("gemini-", ignoreCase = true)) {
                        savedModel
                    } else {
                        defaultModelFor(provider)
                    }
                }
            }
            prefs[uk.aiModel] = nextModel

            val keysMap = AiModelDefaults.decodeStringMap(prefs[uk.providerKeysJson])
            val savedKey = keysMap[provider.key].orEmpty()
            if (savedKey.isEmpty()) prefs.remove(uk.aiApiKey)
            else prefs[uk.aiApiKey] = savedKey
        }
    }

    suspend fun setAiModel(model: String) {
        val clean = model.trim()
        if (clean.isEmpty()) return
        editForActiveUser { prefs, uk ->
            prefs[uk.aiModel] = clean
            val activeCustomId = prefs[uk.activeCustomProviderId]?.trim().orEmpty()
            if (activeCustomId.isNotEmpty()) {
                val list = AiModelDefaults.decodeCustomProviders(prefs[uk.customProvidersJson])
                val updated = list.map { p ->
                    if (p.id == activeCustomId) {
                        val mergedModels = (p.allModels() + clean).distinct()
                        p.copy(selectedModel = clean, models = mergedModels)
                    } else {
                        p
                    }
                }
                prefs[uk.customProvidersJson] = AiModelDefaults.encodeCustomProviders(updated)
            } else {
                val provider = AiProvider.fromKey(prefs[uk.aiProvider])
                if (AiModelDefaults.isBuiltInDefaultEndpoint(prefs[uk.aiEndpointUrl].orEmpty())) {
                    val modelsMap = AiModelDefaults.decodeStringMap(prefs[uk.providerModelsJson]).toMutableMap()
                    modelsMap[provider.key] = clean
                    prefs[uk.providerModelsJson] = AiModelDefaults.encodeStringMap(modelsMap)
                }
            }
        }
    }

    /**
     * Every website endpoint owns its own profile (key + models). Picking an endpoint therefore
     * switches to the registered profile for that website (or creates one) instead of rewriting
     * whichever profile happened to be active.
     */
    suspend fun setAiEndpointUrl(url: String) {
        val clean = url.trim().ifBlank { AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT }
        if (AiModelDefaults.isBuiltInDefaultEndpoint(clean)) {
            setAiProvider(AiModelDefaults.inferBuiltInProviderForUrl(clean))
            return
        }
        if (!UnorouterClient.isValidEndpointUrl(clean)) return
        editForActiveUser { prefs, uk ->
            val list = AiModelDefaults.decodeCustomProviders(prefs[uk.customProvidersJson])
            val activeId = prefs[uk.activeCustomProviderId]?.trim().orEmpty()
            val active = list.firstOrNull { it.id == activeId }
            val host = AiModelDefaults.hostOf(clean)
            val target = list.firstOrNull { it.endpointUrl.equals(clean, ignoreCase = true) }
                ?: active?.takeIf { AiModelDefaults.hostOf(it.endpointUrl) == host }?.copy(endpointUrl = clean)
                ?: list.firstOrNull { AiModelDefaults.hostOf(it.endpointUrl) == host }
                ?: AiModelDefaults.defaultModelsForHost(clean).let { defaults ->
                    CustomAiProviderProfile(
                        id = "custom_${UUID.randomUUID().toString().take(10)}",
                        name = AiModelDefaults.inferProviderNameFromUrl(clean).take(40),
                        endpointUrl = clean,
                        apiKey = "",
                        selectedModel = defaults.firstOrNull().orEmpty(),
                        models = defaults,
                    )
                }
            val nextList = if (list.any { it.id == target.id }) {
                list.map { if (it.id == target.id) target else it }
            } else {
                (list + target).takeLast(25)
            }
            prefs[uk.customProvidersJson] = AiModelDefaults.encodeCustomProviders(nextList)
            saveCurrentBuiltInStateIfActive(prefs, uk)
            activateCustomProfile(prefs, uk, target)
        }
    }

    suspend fun setAiApiKey(apiKey: String) {
        val clean = apiKey.trim()
        editForActiveUser { prefs, uk ->
            if (clean.isEmpty()) prefs.remove(uk.aiApiKey)
            else prefs[uk.aiApiKey] = clean

            val activeCustomId = prefs[uk.activeCustomProviderId]?.trim().orEmpty()
            if (activeCustomId.isNotEmpty()) {
                val list = AiModelDefaults.decodeCustomProviders(prefs[uk.customProvidersJson])
                val updated = list.map { p ->
                    if (p.id == activeCustomId) p.copy(apiKey = clean) else p
                }
                prefs[uk.customProvidersJson] = AiModelDefaults.encodeCustomProviders(updated)
            } else {
                val provider = AiProvider.fromKey(prefs[uk.aiProvider])
                if (AiModelDefaults.isBuiltInDefaultEndpoint(prefs[uk.aiEndpointUrl].orEmpty())) {
                    val keysMap = AiModelDefaults.decodeStringMap(prefs[uk.providerKeysJson]).toMutableMap()
                    if (clean.isEmpty()) keysMap.remove(provider.key) else keysMap[provider.key] = clean
                    prefs[uk.providerKeysJson] = AiModelDefaults.encodeStringMap(keysMap)
                }
            }
        }
    }

    suspend fun saveCustomAiProvider(
        profile: CustomAiProviderProfile,
        selectImmediately: Boolean = true,
    ) {
        val cleanUrl = profile.endpointUrl.trim()
        if (!UnorouterClient.isValidEndpointUrl(cleanUrl)) return
        val cleanId = profile.id.trim().ifBlank { "custom_${UUID.randomUUID().toString().take(10)}" }
        val cleanModels = profile.allModels()
        val chosenModel = profile.selectedModel.trim()
            .ifBlank { cleanModels.firstOrNull().orEmpty() }
            .ifBlank { AiModelDefaults.defaultModelsForHost(cleanUrl).firstOrNull().orEmpty() }
        val mergedModels = (listOf(chosenModel) + cleanModels)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val cleanProfile = profile.copy(
            id = cleanId,
            name = profile.name.trim().ifBlank { AiModelDefaults.inferProviderNameFromUrl(cleanUrl) }.take(40),
            endpointUrl = cleanUrl,
            apiKey = profile.apiKey.trim(),
            selectedModel = chosenModel,
            models = mergedModels,
        )

        editForActiveUser { prefs, uk ->
            val currentList = AiModelDefaults.decodeCustomProviders(prefs[uk.customProvidersJson])
            val nextList = if (currentList.any { it.id == cleanProfile.id }) {
                currentList.map { if (it.id == cleanProfile.id) cleanProfile else it }
            } else {
                (currentList + cleanProfile).takeLast(25)
            }
            prefs[uk.customProvidersJson] = AiModelDefaults.encodeCustomProviders(nextList)

            val shouldActivate = selectImmediately || prefs[uk.activeCustomProviderId] == cleanProfile.id
            if (shouldActivate) {
                saveCurrentBuiltInStateIfActive(prefs, uk)
                activateCustomProfile(prefs, uk, cleanProfile)
            }
        }
    }

    suspend fun selectCustomAiProvider(profileId: String) {
        editForActiveUser { prefs, uk ->
            val currentList = AiModelDefaults.decodeCustomProviders(prefs[uk.customProvidersJson])
            val target = currentList.firstOrNull { it.id == profileId } ?: return@editForActiveUser
            saveCurrentBuiltInStateIfActive(prefs, uk)
            activateCustomProfile(prefs, uk, target)
        }
    }

    suspend fun deleteCustomAiProvider(profileId: String) {
        editForActiveUser { prefs, uk ->
            val currentList = AiModelDefaults.decodeCustomProviders(prefs[uk.customProvidersJson])
            val remaining = currentList.filterNot { it.id == profileId }
            prefs[uk.customProvidersJson] = AiModelDefaults.encodeCustomProviders(remaining)
            if (prefs[uk.activeCustomProviderId] == profileId) {
                prefs.remove(uk.activeCustomProviderId)
                val fallbackProvider = AiProvider.UNOROUTER
                prefs[uk.aiProvider] = fallbackProvider.key
                prefs[uk.aiEndpointUrl] = defaultEndpointFor(fallbackProvider)
                val modelsMap = AiModelDefaults.decodeStringMap(prefs[uk.providerModelsJson])
                prefs[uk.aiModel] = modelsMap[fallbackProvider.key]
                    ?.takeIf { it.isNotBlank() && it !in brokenTextModels }
                    ?: defaultModelFor(fallbackProvider)
                val keysMap = AiModelDefaults.decodeStringMap(prefs[uk.providerKeysJson])
                val savedKey = keysMap[fallbackProvider.key].orEmpty()
                if (savedKey.isEmpty()) prefs.remove(uk.aiApiKey) else prefs[uk.aiApiKey] = savedKey
            }
        }
    }

    private fun activateCustomProfile(
        prefs: MutablePreferences,
        uk: UserAiKeys,
        profile: CustomAiProviderProfile,
    ) {
        prefs[uk.activeCustomProviderId] = profile.id
        prefs[uk.aiProvider] = AiModelDefaults.inferBuiltInProviderForUrl(profile.endpointUrl).key
        prefs[uk.aiEndpointUrl] = profile.endpointUrl
        if (profile.apiKey.isBlank()) prefs.remove(uk.aiApiKey)
        else prefs[uk.aiApiKey] = profile.apiKey

        val modelToUse = profile.selectedModel.trim()
            .ifBlank { profile.models.firstOrNull().orEmpty() }
            .ifBlank { AiModelDefaults.defaultModelsForHost(profile.endpointUrl).firstOrNull().orEmpty() }
        if (modelToUse.isNotBlank()) prefs[uk.aiModel] = modelToUse
        else prefs.remove(uk.aiModel) // never keep the previous website's model
    }

    suspend fun setAiImageEndpointUrl(url: String) {
        val clean = url.trim().ifBlank { AiModelDefaults.DEFAULT_IMAGE_ENDPOINT }
        editForActiveUser { prefs, uk -> prefs[uk.aiImageEndpointUrl] = clean }
    }

    suspend fun setAiImageModel(model: String) {
        val clean = model.trim()
        if (clean.isEmpty()) return
        editForActiveUser { prefs, uk -> prefs[uk.aiImageModel] = clean }
    }

    suspend fun setAiImageApiKey(apiKey: String) {
        val clean = apiKey.trim()
        editForActiveUser { prefs, uk ->
            if (clean.isEmpty()) prefs.remove(uk.aiImageApiKey)
            else prefs[uk.aiImageApiKey] = clean
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

