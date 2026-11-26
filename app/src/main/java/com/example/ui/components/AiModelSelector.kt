package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
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
import com.example.ai.AiModelDefaults
import com.example.ai.AiProvider
import com.example.ai.UnorouterClient

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
    showAdvancedApiConfig: Boolean = false,
) {
    var draftModel by remember(selectedModel) { mutableStateOf(selectedModel) }
    var draftEndpoint by remember(endpointUrl) { mutableStateOf(endpointUrl) }
    var draftApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var showApiKeyText by remember { mutableStateOf(false) }
    var showCurlDialog by remember { mutableStateOf(false) }
    var curlFeedback by remember { mutableStateOf<String?>(null) }

    val presets = if (selectedProvider == AiProvider.UNOROUTER) {
        AiModelDefaults.presetUnorouterModels
    } else {
        AiModelDefaults.presetGeminiModels
    }
    val unorouterReady = remember(apiKey) { UnorouterClient.isConfigured(apiKey) }
    val endpointValid = UnorouterClient.isValidEndpointUrl(draftEndpoint)

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

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AiProvider.entries.forEach { provider ->
                    FilterChip(
                        selected = selectedProvider == provider,
                        onClick = { onSelectProvider(provider) },
                        label = { Text(provider.label) },
                        modifier = Modifier.testTag("ai_provider_chip_${provider.key}")
                    )
                }
            }

            if (selectedProvider == AiProvider.UNOROUTER && !unorouterReady) {
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
                                "Enter your API key in Settings (free, no billing plan needed) or use Gemini (Firebase AI)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            } else if (selectedProvider == AiProvider.UNOROUTER && showAdvancedApiConfig) {
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
                        Text(
                            text = if (apiKey.isNotBlank()) {
                                "Using custom API key saved on this device."
                            } else {
                                "Using build secret UNOROUTER_API_KEY (override below anytime)."
                            },
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
                    label = { Text("Model ID") },
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

            if (!compact) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = selectedModel == preset,
                            onClick = {
                                draftModel = preset
                                onSelectModel(preset)
                            },
                            label = {
                                Text(
                                    text = preset,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("ai_preset_model_$preset")
                        )
                    }
                }
            }

            if (showAdvancedApiConfig && selectedProvider == AiProvider.UNOROUTER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draftEndpoint,
                        onValueChange = { draftEndpoint = it },
                        label = { Text("API Endpoint URL") },
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
                        label = { Text("API Key (Bearer Token)") },
                        placeholder = { Text("sk-...") },
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
                                draftEndpoint = AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT
                                draftModel = AiModelDefaults.DEFAULT_UNOROUTER_MODEL
                                draftApiKey = ""
                                onSelectEndpointUrl?.invoke(AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT)
                                onSelectModel(AiModelDefaults.DEFAULT_UNOROUTER_MODEL)
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
                    val maskedKey = if (UnorouterClient.isConfigured(draftApiKey)) {
                        "Bearer ••••••••"
                    } else {
                        "Bearer <not set>"
                    }
                    Text(
                        text = "POST ${draftEndpoint.trim().ifBlank { AiModelDefaults.DEFAULT_UNOROUTER_ENDPOINT }}\n" +
                            "Authorization: $maskedKey\n" +
                            "{\"model\": \"${draftModel.trim().ifBlank { AiModelDefaults.DEFAULT_UNOROUTER_MODEL }}\", \"messages\": [...]}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
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
                    onSelectProvider(AiProvider.UNOROUTER)
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
                        Text("curl https://api.unorouter.com/v1/chat/completions -H \"Authorization: Bearer sk-...\" -d '{\"model\":\"k-exaone-2.5:free\"}'")
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
