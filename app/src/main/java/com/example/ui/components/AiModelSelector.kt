package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ai.UnorouterClient

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
) {
    var draftModel by remember(selectedModel) { mutableStateOf(selectedModel) }
    var draftEndpoint by remember(endpointUrl) { mutableStateOf(endpointUrl) }
    var draftApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var showApiKeyText by remember { mutableStateOf(false) }
    var showCurlDialog by remember { mutableStateOf(false) }
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

    val presets = when (selectedProvider) {
        AiProvider.OPENROUTER -> AiModelDefaults.presetOpenRouterModels
        AiProvider.UNOROUTER -> AiModelDefaults.presetUnorouterModels
        AiProvider.GEMINI -> AiModelDefaults.presetGeminiModels
    }
    val isExternalRest = selectedProvider != AiProvider.GEMINI
    val apiReady = remember(apiKey, selectedProvider) {
        UnorouterClient.isConfigured(apiKey, selectedProvider)
    }
    val endpointValid = UnorouterClient.isValidEndpointUrl(draftEndpoint)
    val imageEndpointValid = UnorouterClient.isValidEndpointUrl(draftImageEndpoint)
    val imageApiReady = remember(imageApiKey, apiKey, imageEndpointUrl) {
        AiImageGeneratorService.isConfigured(imageApiKey, apiKey, imageEndpointUrl)
    }

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

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showProviderMenu = true },
                    modifier = Modifier.fillMaxWidth().testTag("ai_provider_dropdown_button")
                ) {
                    Text(
                        text = "Provider: ${selectedProvider.label}",
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
                        DropdownMenuItem(
                            text = { Text(provider.label) },
                            leadingIcon = {
                                if (selectedProvider == provider) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            },
                            onClick = {
                                onSelectProvider(provider)
                                showProviderMenu = false
                            },
                            modifier = Modifier.testTag("ai_provider_option_${provider.key}")
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
                            text = if (showAdvancedApiConfig) {
                                "Enter your API key below (or paste a cURL command) to enable this endpoint."
                            } else {
                                "Configure UNOROUTER_API_KEY in AI Studio Secrets or enter your key in Settings."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            } else if (isExternalRest && showAdvancedApiConfig) {
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
                            apiKey.isNotBlank() -> "Using custom API key saved on this device."
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
                    placeholder = { Text(AiModelDefaults.DEFAULT_UNOROUTER_MODEL) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("ai_model_input")
                )
                Button(
                    onClick = {
                        val trimmed = draftModel.trim()
                        if (trimmed.isNotEmpty()) {
                            onSelectModel(trimmed)
                        }
                    },
                    enabled = draftModel.isNotBlank() && draftModel.trim() != selectedModel,
                    modifier = Modifier.testTag("apply_ai_model_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Apply model", modifier = Modifier.size(16.dp))
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showTextModelMenu = true },
                    modifier = Modifier.fillMaxWidth().testTag("ai_model_dropdown_button")
                ) {
                    Text(
                        text = "Choose Model ($selectedModel)",
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
                                onSelectModel(preset)
                                showTextModelMenu = false
                            },
                            modifier = Modifier.testTag("ai_preset_model_$preset")
                        )
                    }
                }
            }

            if (showAdvancedApiConfig && isExternalRest) {
                if (onSelectEndpointUrl != null) {
                    val activeSiteLabel = AiModelDefaults.presetWebsiteEndpoints
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
                                        onSelectEndpointUrl.invoke(siteUrl)
                                        curlFeedback = "Switched website endpoint to $siteLabel."
                                        showEndpointMenu = false
                                    },
                                    modifier = Modifier.testTag("ai_website_preset_${siteLabel.lowercase()}")
                                )
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
                                onSelectEndpointUrl?.invoke(trimmed)
                                curlFeedback = "Endpoint URL updated."
                            }
                        },
                        enabled = onSelectEndpointUrl != null && endpointValid && draftEndpoint.trim() != endpointUrl,
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
                            onSelectApiKey?.invoke(trimmed)
                            curlFeedback = if (trimmed.isEmpty()) "Custom API key cleared." else "API key saved."
                        },
                        enabled = onSelectApiKey != null && draftApiKey.trim() != apiKey.trim(),
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
                                onSelectEndpointUrl?.invoke(defUrl)
                                onSelectModel(defModel)
                                onSelectApiKey?.invoke("")
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

            if (showAdvancedApiConfig && onSelectImageModel != null) {
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
                                onSelectImageModel(trimmed)
                                imageFeedback = "Image model updated."
                            }
                        },
                        enabled = draftImageModel.isNotBlank() && draftImageModel.trim() != imageModel,
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
                                    onSelectImageModel(preset)
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
                                onSelectImageEndpointUrl?.invoke(trimmed)
                                imageFeedback = "Image endpoint updated."
                            }
                        },
                        enabled = onSelectImageEndpointUrl != null && imageEndpointValid && draftImageEndpoint.trim() != imageEndpointUrl,
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
                            onSelectImageApiKey?.invoke(trimmed)
                            imageFeedback = if (trimmed.isEmpty()) "Custom image key cleared." else "Image API key saved."
                        },
                        enabled = onSelectImageApiKey != null && draftImageApiKey.trim() != imageApiKey.trim(),
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
                    val targetProvider = if (parsed.endpointUrl?.contains("openrouter.ai", ignoreCase = true) == true) {
                        AiProvider.OPENROUTER
                    } else {
                        AiProvider.UNOROUTER
                    }
                    onSelectProvider(targetProvider)
                    parsed.endpointUrl?.let { url ->
                        if (UnorouterClient.isValidEndpointUrl(url)) {
                            draftEndpoint = url
                            onSelectEndpointUrl?.invoke(url)
                        }
                    }
                    parsed.apiKey?.let { key ->
                        draftApiKey = key
                        onSelectApiKey?.invoke(key)
                    }
                    parsed.model?.let { model ->
                        draftModel = model
                        onSelectModel(model)
                    }
                    curlFeedback = "Aligned endpoint, API key, and model from cURL."
                    showCurlDialog = false
                    true
                }
            }
        )
    }
}

@Composable
private fun CurlImportDialog(
    onDismiss: () -> Unit,
    onApply: (String) -> Boolean,
) {
    var curlText by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from cURL", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Paste an OpenAI-compatible curl command to auto-fill the Endpoint URL, Bearer API key, and Model ID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = curlText,
                    onValueChange = {
                        curlText = it
                        parseError = false
                    },
                    label = { Text("cURL command") },
                    placeholder = {
                        Text("curl https://openrouter.ai/api/v1/chat/completions -H \"Authorization: Bearer sk-...\" -d '{\"model\":\"...\"}'")
                    },
                    minLines = 4,
                    maxLines = 8,
                    isError = parseError,
                    modifier = Modifier.fillMaxWidth().testTag("curl_import_input")
                )
                if (parseError) {
                    Text(
                        text = "Could not detect an https:// URL, Bearer token, or \"model\" field.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ok = onApply(curlText)
                    if (!ok) parseError = true
                },
                enabled = curlText.isNotBlank(),
                modifier = Modifier.testTag("confirm_curl_import_button")
            ) {
                Text("Apply cURL")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
