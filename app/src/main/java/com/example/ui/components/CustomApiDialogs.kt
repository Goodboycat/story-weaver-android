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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.example.ai.AiModelDefaults
import com.example.ai.CustomAiProviderProfile
import com.example.ai.UnorouterClient
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterCustomApiDialog(
    initialProfile: CustomAiProviderProfile? = null,
    onSave: (CustomAiProviderProfile) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var curlInput by remember(initialProfile) { mutableStateOf("") }
    var nameInput by remember(initialProfile) { mutableStateOf(initialProfile?.name.orEmpty()) }
    var urlInput by remember(initialProfile) { mutableStateOf(initialProfile?.endpointUrl.orEmpty()) }
    var keyInput by remember(initialProfile) { mutableStateOf(initialProfile?.apiKey.orEmpty()) }
    var modelsInput by remember(initialProfile) {
        mutableStateOf(initialProfile?.allModels()?.joinToString(", ").orEmpty())
    }
    var showKey by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun applyParsedCurl(rawCurl: String): Boolean {
        val parsed = UnorouterClient.parseCurlCommand(rawCurl) ?: return false
        var updatedSomething = false
        parsed.endpointUrl?.let { url ->
            if (UnorouterClient.isValidEndpointUrl(url)) {
                urlInput = url
                if (nameInput.isBlank()) {
                    nameInput = AiModelDefaults.inferProviderNameFromUrl(url)
                }
                updatedSomething = true
            }
        }
        parsed.apiKey?.let { key ->
            keyInput = key
            updatedSomething = true
        }
        parsed.model?.let { model ->
            val currentModels = modelsInput.split(",", "\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val merged = (listOf(model) + currentModels).distinct()
            modelsInput = merged.joinToString(", ")
            updatedSomething = true
        }
        return updatedSomething
    }

    val urlValid = UnorouterClient.isValidEndpointUrl(urlInput)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AddLink,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (initialProfile == null) "Register Custom API" else "Edit Custom API",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Save a personal AI provider link, cURL, API key, and model list to your account so you can switch between providers anytime from any Model menu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = curlInput,
                    onValueChange = {
                        curlInput = it
                        errorMessage = null
                    },
                    label = { Text("Paste cURL Command (Optional)") },
                    placeholder = {
                        Text("curl https://api.openai.com/v1/chat/completions -H \"Authorization: Bearer sk-...\" -d '{\"model\":\"gpt-4o-mini\"}'")
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("custom_api_curl_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (applyParsedCurl(curlInput)) {
                                statusMessage = "Extracted endpoint, key, and model from cURL."
                                errorMessage = null
                            } else {
                                errorMessage = "Could not detect an https:// URL, Bearer token, or \"model\" field in cURL."
                            }
                        },
                        enabled = curlInput.isNotBlank(),
                        modifier = Modifier.testTag("extract_custom_api_curl_button")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Extract from cURL")
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Quick Provider Links",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AiModelDefaults.presetWebsiteEndpoints.forEach { (siteLabel, siteUrl) ->
                        FilterChip(
                            selected = urlInput.trim().equals(siteUrl, ignoreCase = true),
                            onClick = {
                                urlInput = siteUrl
                                if (nameInput.isBlank()) nameInput = siteLabel
                                val defaults = AiModelDefaults.defaultModelsForHost(siteUrl)
                                if (modelsInput.isBlank() && defaults.isNotEmpty()) {
                                    modelsInput = defaults.joinToString(", ")
                                }
                                errorMessage = null
                            },
                            label = { Text(siteLabel) },
                            modifier = Modifier.testTag("custom_api_preset_${siteLabel.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Provider Name") },
                    placeholder = { Text("e.g. OpenAI, Groq, DeepSeek, My Proxy") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_api_name_input")
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        errorMessage = null
                    },
                    label = { Text("Provider Link / Endpoint URL") },
                    placeholder = { Text("https://api.openai.com/v1/chat/completions") },
                    isError = urlInput.isNotBlank() && !urlValid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_api_url_input")
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Personal API Key") },
                    placeholder = { Text("sk-... / Bearer token") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = { showKey = !showKey },
                            modifier = Modifier.testTag("toggle_custom_api_key_visibility")
                        ) {
                            Icon(
                                imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showKey) "Hide API key" else "Show API key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("custom_api_key_input")
                )

                OutlinedTextField(
                    value = modelsInput,
                    onValueChange = { modelsInput = it },
                    label = { Text("Model IDs for this Provider (comma separated)") },
                    placeholder = { Text("gpt-4o-mini, gpt-4o") },
                    minLines = 1,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("custom_api_models_input")
                )

                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (urlValid) {
                    val previewModel = modelsInput.split(",", "\n")
                        .map { it.trim() }
                        .firstOrNull { it.isNotEmpty() }
                        ?: "model-id"
                    val maskedKey = if (keyInput.isNotBlank()) "Bearer ••••••••" else "Bearer <from Secrets or blank>"
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "POST ${urlInput.trim()}\nAuthorization: $maskedKey\n{\"model\": \"$previewModel\"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (initialProfile != null && onDelete != null) {
                    OutlinedButton(
                        onClick = {
                            onDelete(initialProfile.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("delete_custom_api_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remove This Custom API")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!UnorouterClient.isValidEndpointUrl(urlInput) && curlInput.isNotBlank()) {
                        applyParsedCurl(curlInput)
                    }
                    val finalUrl = urlInput.trim()
                    if (!UnorouterClient.isValidEndpointUrl(finalUrl)) {
                        errorMessage = "Please enter a valid https:// provider endpoint URL (or paste a valid cURL)."
                        return@Button
                    }
                    val parsedModels = modelsInput.split(",", "\n")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    val fallbackModels = parsedModels.ifEmpty {
                        AiModelDefaults.defaultModelsForHost(finalUrl)
                    }
                    val selectedModel = initialProfile?.selectedModel
                        ?.takeIf { it.isNotBlank() && it in fallbackModels }
                        ?: fallbackModels.firstOrNull().orEmpty()
                    val finalName = nameInput.trim()
                        .ifBlank { AiModelDefaults.inferProviderNameFromUrl(finalUrl) }
                    onSave(
                        CustomAiProviderProfile(
                            id = initialProfile?.id ?: "custom_${UUID.randomUUID().toString().take(10)}",
                            name = finalName,
                            endpointUrl = finalUrl,
                            apiKey = keyInput.trim(),
                            selectedModel = selectedModel,
                            models = fallbackModels,
                        )
                    )
                    onDismiss()
                },
                enabled = urlInput.isNotBlank() || curlInput.isNotBlank(),
                modifier = Modifier.testTag("save_custom_api_button")
            ) {
                Text(if (initialProfile == null) "Register & Use" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CurlImportDialog(
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
