package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.AiImageGeneratorService
import com.example.ai.AiModelDefaults
import com.example.ai.AiProvider
import com.example.ai.CustomAiProviderProfile
import com.example.ai.UnorouterClient

data class AiProviderConfigActions(
    val onSelectProvider: (AiProvider) -> Unit,
    val onSelectModel: (String) -> Unit,
    val onSelectEndpointUrl: (String) -> Unit = {},
    val onSelectApiKey: (String) -> Unit = {},
    val onSelectCustomProvider: (String) -> Unit = {},
    val onSaveCustomProvider: (CustomAiProviderProfile) -> Unit = {},
    val onDeleteCustomProvider: (String) -> Unit = {},
    val onSelectImageEndpointUrl: (String) -> Unit = {},
    val onSelectImageModel: (String) -> Unit = {},
    val onSelectImageApiKey: (String) -> Unit = {},
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiModelSelectorBar(
    selectedProvider: AiProvider,
    selectedModel: String,
    onSelectProvider: (AiProvider) -> Unit,
    onSelectModel: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    endpointUrl: String = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT,
    apiKey: String = "",
    onSelectEndpointUrl: ((String) -> Unit)? = null,
    onSelectApiKey: ((String) -> Unit)? = null,
    imageEndpointUrl: String = AiModelDefaults.DEFAULT_IMAGE_ENDPOINT,
    imageModel: String = AiModelDefaults.DEFAULT_IMAGE_MODEL,
    imageApiKey: String = "",
    onSelectImageEndpointUrl: ((String) -> Unit)? = null,
    onSelectImageModel: ((String) -> Unit)? = null,
    onSelectImageApiKey: ((String) -> Unit)? = null,
    showAdvancedApiConfig: Boolean = false,
    customProviders: List<CustomAiProviderProfile> = emptyList(),
    activeCustomProviderId: String? = null,
    onSelectCustomProvider: ((String) -> Unit)? = null,
    onSaveCustomProvider: ((CustomAiProviderProfile) -> Unit)? = null,
    onDeleteCustomProvider: ((String) -> Unit)? = null,
    configActions: AiProviderConfigActions? = null,
) {
    val effectiveSelectProvider = configActions?.onSelectProvider ?: onSelectProvider
    val effectiveSelectModel = configActions?.onSelectModel ?: onSelectModel
    val effectiveSelectEndpoint = configActions?.onSelectEndpointUrl ?: onSelectEndpointUrl
    val effectiveSelectApiKey = configActions?.onSelectApiKey ?: onSelectApiKey
    val effectiveSelectCustom = configActions?.onSelectCustomProvider ?: onSelectCustomProvider
    val effectiveSaveCustom = configActions?.onSaveCustomProvider ?: onSaveCustomProvider
    val effectiveDeleteCustom = configActions?.onDeleteCustomProvider ?: onDeleteCustomProvider

    val activeCustomProvider = remember(activeCustomProviderId, customProviders) {
        activeCustomProviderId?.let { id -> customProviders.firstOrNull { it.id == id } }
    }

    var draftModel by remember(selectedModel, activeCustomProviderId, selectedProvider) {
        mutableStateOf(selectedModel)
    }
    var draftEndpoint by remember(endpointUrl, activeCustomProviderId, selectedProvider) {
        mutableStateOf(endpointUrl)
    }
    var draftApiKey by remember(apiKey, activeCustomProviderId, selectedProvider) {
        mutableStateOf(apiKey)
    }
    var showApiKeyText by remember { mutableStateOf(false) }
    var showCurlDialog by remember { mutableStateOf(false) }
    var editingCustomProfile by remember { mutableStateOf<CustomAiProviderProfile?>(null) }
    var showRegisterCustomDialog by remember { mutableStateOf(false) }
    var curlFeedback by remember { mutableStateOf<String?>(null) }

    var draftImageEndpoint by remember(imageEndpointUrl) { mutableStateOf(imageEndpointUrl) }
    var draftImageModel by remember(imageModel) { mutableStateOf(imageModel) }
    var draftImageApiKey by remember(imageApiKey) { mutableStateOf(imageApiKey) }
    var showImageKeyText by remember { mutableStateOf(false) }
    var imageFeedback by remember { mutableStateOf<String?>(null) }

    var showProviderMenu by remember { mutableStateOf(false) }
    var showTextModelMenu by remember { mutableStateOf(false) }
    var showEndpointMenu by remember { mutableStateOf(false) }
    var showImageModelMenu by remember { mutableStateOf(false) }

    val presets = remember(selectedProvider, endpointUrl, activeCustomProvider, selectedModel) {
        AiModelDefaults.modelsForSelection(
            provider = selectedProvider,
            endpointUrl = endpointUrl,
            activeCustomProvider = activeCustomProvider,
            currentModel = selectedModel,
        )
    }

    val isExternalRest = selectedProvider != AiProvider.GEMINI || activeCustomProvider != null
    val apiReady = remember(apiKey, selectedProvider, activeCustomProvider) {
        if (activeCustomProvider != null && !AiModelDefaults.isBuiltInHost(activeCustomProvider.endpointUrl)) {
            activeCustomProvider.apiKey.isNotBlank()
        } else {
            UnorouterClient.isConfigured(apiKey, selectedProvider)
        }
    }
    val endpointValid = UnorouterClient.isValidEndpointUrl(draftEndpoint)
    val imageEndpointValid = UnorouterClient.isValidEndpointUrl(draftImageEndpoint)
    val imageApiReady = remember(imageApiKey, apiKey, imageEndpointUrl) {
        AiImageGeneratorService.isConfigured(imageApiKey, apiKey, imageEndpointUrl)
    }

    val activeProviderDisplay = activeCustomProvider?.let {
        "${it.name} (${it.displayHost()})"
    } ?: selectedProvider.label

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "AI Provider & Model",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (effectiveSaveCustom != null) {
                    OutlinedButton(
                        onClick = {
                            editingCustomProfile = null
                            showRegisterCustomDialog = true
                        },
                        modifier = Modifier.testTag("register_custom_api_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Custom API")
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showProviderMenu = true },
                    modifier = Modifier.fillMaxWidth().testTag("ai_provider_dropdown_button")
                ) {
                    Text(
                        text = "Provider: $activeProviderDisplay",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select AI provider")
                }
                DropdownMenu(
                    expanded = showProviderMenu,
                    onDismissRequest = { showProviderMenu = false }
                ) {
                    AiProvider.entries.forEach { provider ->
                        val isSelected = activeCustomProvider == null && selectedProvider == provider
                        DropdownMenuItem(
                            text = { Text(provider.label) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            },
                            onClick = {
                                showTextModelMenu = false
                                showProviderMenu = false
                                effectiveSelectProvider(provider)
                            },
                            modifier = Modifier.testTag("ai_provider_option_${provider.key}")
                        )
                    }

                    if (customProviders.isNotEmpty() && effectiveSelectCustom != null) {
                        HorizontalDivider()
                        customProviders.forEach { profile ->
                            val isSelected = activeCustomProviderId == profile.id
                            val modelCount = profile.allModels().size
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = profile.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${profile.displayHost()} • $modelCount model${if (modelCount == 1) "" else "s"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (effectiveSaveCustom != null) {
                                            IconButton(
                                                onClick = {
                                                    showProviderMenu = false
                                                    editingCustomProfile = profile
                                                    showRegisterCustomDialog = true
                                                },
                                                modifier = Modifier.size(32.dp).testTag("edit_custom_provider_${profile.id}")
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit ${profile.name}", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        if (effectiveDeleteCustom != null) {
                                            IconButton(
                                                onClick = {
                                                    showProviderMenu = false
                                                    effectiveDeleteCustom(profile.id)
                                                },
                                                modifier = Modifier.size(32.dp).testTag("delete_custom_provider_${profile.id}")
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete ${profile.name}", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    showTextModelMenu = false
                                    showProviderMenu = false
                                    effectiveSelectCustom(profile.id)
                                },
                                modifier = Modifier.testTag("ai_custom_provider_option_${profile.id}")
                            )
                        }
                    }

                    if (effectiveSaveCustom != null) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Register New Custom API (cURL / Link)") },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                showProviderMenu = false
                                editingCustomProfile = null
                                showRegisterCustomDialog = true
                            },
                            modifier = Modifier.testTag("menu_register_custom_api")
                        )
                    }
                }
            }

            if (isExternalRest && !apiReady) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (activeCustomProvider != null) {
                                "No API key saved for ${activeCustomProvider.name}. Tap Edit or enter your key in Custom API."
                            } else if (showAdvancedApiConfig) {
                                "Enter your API key below (or register a Custom API with cURL) to enable this endpoint."
                            } else {
                                "Tap Custom API above to enter a key/cURL or configure UNOROUTER_API_KEY in AI Studio Secrets."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            } else if (isExternalRest && (showAdvancedApiConfig || activeCustomProvider != null)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        val activeSecretLabel = when {
                            activeCustomProvider != null ->
                                "Using saved API \"${activeCustomProvider.name}\" (${activeCustomProvider.displayHost()})."
                            apiKey.isNotBlank() -> "Using custom API key saved for your account."
                            selectedProvider == AiProvider.OPENROUTER && UnorouterClient.hasOpenRouterBuildConfigKey() ->
                                "Using OPENROUTER_API_KEY from AI Studio Secrets."
                            UnorouterClient.hasBuildConfigKey() ->
                                "Using UNOROUTER_API_KEY from AI Studio Secrets."
                            else -> "Using OpenRouter build secret from AI Studio Secrets."
                        }
                        Text(
                            text = activeSecretLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draftModel,
                    onValueChange = { draftModel = it },
                    label = { Text("Chat / Parser Model ID") },
                    placeholder = {
                        Text(presets.firstOrNull() ?: AiModelDefaults.DEFAULT_UNOROUTER_MODEL)
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("ai_model_input")
                )
                Button(
                    onClick = {
                        val trimmed = draftModel.trim()
                        if (trimmed.isNotEmpty()) {
                            effectiveSelectModel(trimmed)
                        }
                    },
                    enabled = draftModel.isNotBlank() && draftModel.trim() != selectedModel,
                    modifier = Modifier.testTag("apply_ai_model_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Apply model", modifier = Modifier.size(16.dp))
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                val modelButtonLabel = if (selectedModel.isNotBlank()) {
                    "Choose Model ($selectedModel)"
                } else {
                    "Choose Model (Add or pick model)"
                }
                OutlinedButton(
                    onClick = { showTextModelMenu = true },
                    modifier = Modifier.fillMaxWidth().testTag("ai_model_dropdown_button")
                ) {
                    Text(
                        text = modelButtonLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Open model list")
                }
                DropdownMenu(
                    expanded = showTextModelMenu,
                    onDismissRequest = { showTextModelMenu = false }
                ) {
                    if (presets.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Type a model ID above and tap ✓ to add it") },
                            onClick = { showTextModelMenu = false }
                        )
                    } else {
                        presets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset) },
                                leadingIcon = {
                                    if (selectedModel == preset) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                },
                                onClick = {
                                    draftModel = preset
                                    effectiveSelectModel(preset)
                                    showTextModelMenu = false
                                },
                                modifier = Modifier.testTag("ai_preset_model_$preset")
                            )
                        }
                    }
                }
            }

            if (showAdvancedApiConfig && isExternalRest) {
                if (effectiveSelectEndpoint != null) {
                    val activeSiteLabel = activeCustomProvider?.name?.takeIf { it.isNotBlank() }
                        ?: AiModelDefaults.presetWebsiteEndpoints
                        .firstOrNull { (_, u) -> endpointUrl.equals(u, ignoreCase = true) }?.first
                        ?: "Custom Endpoint"
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showEndpointMenu = true },
                            modifier = Modifier.fillMaxWidth().testTag("ai_website_dropdown_button")
                        ) {
                            Text(
                                text = "Website Provider: $activeSiteLabel",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose website provider")
                        }
                        DropdownMenu(
                            expanded = showEndpointMenu,
                            onDismissRequest = { showEndpointMenu = false }
                        ) {
                            AiModelDefaults.presetWebsiteEndpoints.forEach { (siteLabel, siteUrl) ->
                                DropdownMenuItem(
                                    text = { Text("$siteLabel ($siteUrl)") },
                                    leadingIcon = {
                                        if (endpointUrl.equals(siteUrl, ignoreCase = true)) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    onClick = {
                                        draftEndpoint = siteUrl
                                        effectiveSelectEndpoint(siteUrl)
                                        curlFeedback = "Switched website endpoint to $siteLabel."
                                        showEndpointMenu = false
                                    },
                                    modifier = Modifier.testTag("ai_website_preset_${siteLabel.lowercase()}")
                                )
                            }
                            if (customProviders.isNotEmpty()) {
                                HorizontalDivider()
                                customProviders.forEach { profile ->
                                    DropdownMenuItem(
                                        text = { Text("${profile.name} (${profile.displayHost()})") },
                                        leadingIcon = {
                                            if (activeCustomProviderId == profile.id) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                            }
                                        },
                                        onClick = {
                                            showEndpointMenu = false
                                            draftEndpoint = profile.endpointUrl
                                            if (effectiveSelectCustom != null) effectiveSelectCustom(profile.id)
                                            else effectiveSelectEndpoint(profile.endpointUrl)
                                            curlFeedback = "Switched website provider to ${profile.name}."
                                        },
                                        modifier = Modifier.testTag("ai_website_custom_${profile.id}")
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draftEndpoint,
                        onValueChange = { draftEndpoint = it },
                        label = { Text("Website Provider / Endpoint URL") },
                        placeholder = { Text(AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT) },
                        isError = draftEndpoint.isNotBlank() && !endpointValid,
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("ai_endpoint_input")
                    )
                    Button(
                        onClick = {
                            val trimmed = draftEndpoint.trim()
                            if (UnorouterClient.isValidEndpointUrl(trimmed)) {
                                effectiveSelectEndpoint?.invoke(trimmed)
                                curlFeedback = "Endpoint URL updated."
                            }
                        },
                        enabled = effectiveSelectEndpoint != null && endpointValid && draftEndpoint.trim() != endpointUrl,
                        modifier = Modifier.testTag("apply_ai_endpoint_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Apply endpoint URL", modifier = Modifier.size(16.dp))
                    }
                }
                if (draftEndpoint.isNotBlank() && !endpointValid) {
                    Text(
                        text = "Endpoint URL must start with https://",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draftApiKey,
                        onValueChange = { draftApiKey = it },
                        label = { Text("Chat / Parser API Key Override") },
                        placeholder = { Text("Leave blank to use Secrets key") },
                        singleLine = true,
                        visualTransformation = if (showApiKeyText) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { showApiKeyText = !showApiKeyText },
                                modifier = Modifier.testTag("toggle_api_key_visibility_button")
                            ) {
                                Icon(
                                    imageVector = if (showApiKeyText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showApiKeyText) "Hide API key" else "Show API key"
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("ai_api_key_input")
                    )
                    Button(
                        onClick = {
                            val trimmed = draftApiKey.trim()
                            effectiveSelectApiKey?.invoke(trimmed)
                            curlFeedback = if (trimmed.isEmpty()) "Custom API key cleared." else "API key saved."
                        },
                        enabled = effectiveSelectApiKey != null && draftApiKey.trim() != apiKey.trim(),
                        modifier = Modifier.testTag("apply_ai_api_key_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save API key", modifier = Modifier.size(16.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showCurlDialog = true },
                        modifier = Modifier.weight(1f).testTag("paste_curl_button")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import cURL", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (apiKey.isNotBlank() || endpointUrl != AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT) {
                        OutlinedButton(
                            onClick = {
                                val defUrl = if (selectedProvider == AiProvider.OPENROUTER) {
                                    AiModelDefaults.DEFAULT_OPENROUTER_ENDPOINT
                                } else {
                                    AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT
                                }
                                val defModel = if (selectedProvider == AiProvider.OPENROUTER) {
                                    AiModelDefaults.DEFAULT_OPENROUTER_MODEL
                                } else {
                                    AiModelDefaults.DEFAULT_UNOROUTER_MODEL
                                }
                                draftEndpoint = defUrl
                                draftModel = defModel
                                draftApiKey = ""
                                effectiveSelectEndpoint?.invoke(defUrl)
                                effectiveSelectModel(defModel)
                                effectiveSelectApiKey?.invoke("")
                                curlFeedback = "Reset endpoint, model, and custom key."
                            },
                            modifier = Modifier.testTag("reset_api_defaults_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }

                if (curlFeedback != null) {
                    Text(
                        text = curlFeedback!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val maskedKey = if (UnorouterClient.isConfigured(draftApiKey, selectedProvider)) {
                        "Bearer ••••••••"
                    } else {
                        "Bearer <not set>"
                    }
                    val previewUrl = UnorouterClient.resolveEndpointUrl(selectedProvider, draftEndpoint)
                    Text(
                        text = "POST $previewUrl\n" +
                            "Authorization: $maskedKey\n" +
                            "{\"model\": \"${draftModel.trim().ifBlank { AiModelDefaults.DEFAULT_UNOROUTER_MODEL }}\", \"messages\": [...]}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (showAdvancedApiConfig && (configActions != null || onSelectImageModel != null)) {
                val effectiveSelectImgEndpoint = configActions?.onSelectImageEndpointUrl ?: onSelectImageEndpointUrl
                val effectiveSelectImgModel = configActions?.onSelectImageModel ?: onSelectImageModel
                val effectiveSelectImgKey = configActions?.onSelectImageApiKey ?: onSelectImageApiKey

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "AI Image Generator API",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = if (imageApiReady) {
                        if (imageApiKey.isNotBlank()) "Using custom Image API key."
                        else "Ready using configured Secrets / API key."
                    } else {
                        "Add UNOROUTER_API_KEY / OPENROUTER_API_KEY in AI Studio Secrets or enter an API key below."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draftImageModel,
                        onValueChange = { draftImageModel = it },
                        label = { Text("Image Generator Model") },
                        placeholder = { Text(AiModelDefaults.DEFAULT_IMAGE_MODEL) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("ai_image_model_input")
                    )
                    Button(
                        onClick = {
                            val trimmed = draftImageModel.trim()
                            if (trimmed.isNotEmpty()) {
                                effectiveSelectImgModel?.invoke(trimmed)
                                imageFeedback = "Image model updated."
                            }
                        },
                        enabled = effectiveSelectImgModel != null && draftImageModel.isNotBlank() && draftImageModel.trim() != imageModel,
                        modifier = Modifier.testTag("apply_ai_image_model_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Apply image model", modifier = Modifier.size(16.dp))
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showImageModelMenu = true },
                        modifier = Modifier.fillMaxWidth().testTag("ai_image_model_dropdown_button")
                    ) {
                        Text(
                            text = "Choose Image Model ($imageModel)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Open image model list")
                    }
                    DropdownMenu(
                        expanded = showImageModelMenu,
                        onDismissRequest = { showImageModelMenu = false }
                    ) {
                        AiModelDefaults.presetImageModels.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset) },
                                leadingIcon = {
                                    if (imageModel == preset) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                },
                                onClick = {
                                    draftImageModel = preset
                                    effectiveSelectImgModel?.invoke(preset)
                                    showImageModelMenu = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draftImageEndpoint,
                        onValueChange = { draftImageEndpoint = it },
                        label = { Text("Image API Endpoint URL") },
                        placeholder = { Text(AiModelDefaults.DEFAULT_IMAGE_ENDPOINT) },
                        isError = draftImageEndpoint.isNotBlank() && !imageEndpointValid,
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("ai_image_endpoint_input")
                    )
                    Button(
                        onClick = {
                            val trimmed = draftImageEndpoint.trim()
                            if (UnorouterClient.isValidEndpointUrl(trimmed)) {
                                effectiveSelectImgEndpoint?.invoke(trimmed)
                                imageFeedback = "Image endpoint updated."
                            }
                        },
                        enabled = effectiveSelectImgEndpoint != null && imageEndpointValid && draftImageEndpoint.trim() != imageEndpointUrl,
                        modifier = Modifier.testTag("apply_ai_image_endpoint_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Apply image endpoint", modifier = Modifier.size(16.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draftImageApiKey,
                        onValueChange = { draftImageApiKey = it },
                        label = { Text("Image API Key Override (Optional)") },
                        placeholder = { Text("Uses OPENROUTER_API_KEY if blank") },
                        singleLine = true,
                        visualTransformation = if (showImageKeyText) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showImageKeyText = !showImageKeyText }) {
                                Icon(
                                    imageVector = if (showImageKeyText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showImageKeyText) "Hide image key" else "Show image key"
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("ai_image_api_key_input")
                    )
                    Button(
                        onClick = {
                            val trimmed = draftImageApiKey.trim()
                            effectiveSelectImgKey?.invoke(trimmed)
                            imageFeedback = if (trimmed.isEmpty()) "Custom image key cleared." else "Image API key saved."
                        },
                        enabled = effectiveSelectImgKey != null && draftImageApiKey.trim() != imageApiKey.trim(),
                        modifier = Modifier.testTag("apply_ai_image_api_key_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save image API key", modifier = Modifier.size(16.dp))
                    }
                }

                if (imageFeedback != null) {
                    Text(
                        text = imageFeedback!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showCurlDialog) {
        CurlImportDialog(
            onDismiss = { showCurlDialog = false },
            onApply = { rawCurl ->
                val parsed = UnorouterClient.parseCurlCommand(rawCurl)
                if (parsed == null) {
                    false
                } else {
                    val parsedUrl = parsed.endpointUrl
                    if (effectiveSaveCustom != null && parsedUrl != null && UnorouterClient.isValidEndpointUrl(parsedUrl)) {
                        val parsedModel = parsed.model?.trim().orEmpty()
                        val profile = CustomAiProviderProfile(
                            name = AiModelDefaults.inferProviderNameFromUrl(parsedUrl),
                            endpointUrl = parsedUrl,
                            apiKey = parsed.apiKey.orEmpty(),
                            selectedModel = parsedModel,
                            models = listOfNotNull(parsedModel.takeIf { it.isNotEmpty() }),
                        )
                        effectiveSaveCustom(profile)
                        curlFeedback = "Registered & switched to ${profile.name}."
                    } else {
                        val targetProvider = if (parsedUrl?.contains("openrouter.ai", ignoreCase = true) == true) {
                            AiProvider.OPENROUTER
                        } else {
                            AiProvider.UNOROUTER
                        }
                        effectiveSelectProvider(targetProvider)
                        parsedUrl?.let { url ->
                            if (UnorouterClient.isValidEndpointUrl(url)) {
                                draftEndpoint = url
                                effectiveSelectEndpoint?.invoke(url)
                            }
                        }
                        parsed.apiKey?.let { key ->
                            draftApiKey = key
                            effectiveSelectApiKey?.invoke(key)
                        }
                        parsed.model?.let { model ->
                            draftModel = model
                            effectiveSelectModel(model)
                        }
                        curlFeedback = "Aligned endpoint, API key, and model from cURL."
                    }
                    showCurlDialog = false
                    true
                }
            }
        )
    }

    if (showRegisterCustomDialog && effectiveSaveCustom != null) {
        RegisterCustomApiDialog(
            initialProfile = editingCustomProfile,
            onSave = { profile ->
                effectiveSaveCustom(profile)
                curlFeedback = "Saved custom API \"${profile.name}\"."
            },
            onDelete = effectiveDeleteCustom,
            onDismiss = {
                showRegisterCustomDialog = false
                editingCustomProfile = null
            }
        )
    }
}
