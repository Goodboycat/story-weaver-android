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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.ai.AiModelDefaults
import com.example.ai.CustomAiProviderProfile
import com.example.ai.UnorouterClient
import java.util.UUID

@Composable
fun RegisterCustomApiDialog(
    initialProfile: CustomAiProviderProfile? = null,
    existingProviders: List<CustomAiProviderProfile> = emptyList(),
    onSave: (CustomAiProviderProfile) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var editingProfileId by remember(initialProfile) { mutableStateOf(initialProfile?.id) }
    var curlInput by remember(initialProfile) { mutableStateOf("") }
    var nameInput by remember(initialProfile) { mutableStateOf(initialProfile?.name.orEmpty()) }
    var urlInput by remember(initialProfile) { mutableStateOf(initialProfile?.endpointUrl.orEmpty()) }
    var keyInput by remember(initialProfile) { mutableStateOf(initialProfile?.apiKey.orEmpty()) }
    var modelsInput by remember(initialProfile) {
        mutableStateOf(initialProfile?.allModels()?.joinToString(", ").orEmpty())
    }
    var lastAutoFilledKey by remember(initialProfile) { mutableStateOf(initialProfile?.apiKey.orEmpty()) }
    var lastAutoFilledModels by remember(initialProfile) {
        mutableStateOf(initialProfile?.allModels()?.joinToString(", ").orEmpty())
    }
    var lastAutoFilledName by remember(initialProfile) { mutableStateOf(initialProfile?.name.orEmpty()) }
    var showWebsiteDropdown by remember { mutableStateOf(false) }
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
    val selectedExistingProfile = existingProviders.firstOrNull { it.id == editingProfileId } ?: initialProfile
    val selectedWebsiteLabel = selectedExistingProfile?.name?.takeIf { it.isNotBlank() }
        ?: existingProviders.firstOrNull {
            it.endpointUrl.equals(urlInput.trim(), ignoreCase = true) &&
                (nameInput.isBlank() || it.name.equals(nameInput.trim(), ignoreCase = true))
        }?.name
        ?: AiModelDefaults.presetWebsiteEndpoints.firstOrNull { (_, u) ->
            urlInput.trim().equals(u, ignoreCase = true)
        }?.first
        ?: if (urlInput.isNotBlank()) AiModelDefaults.inferProviderNameFromUrl(urlInput) else "Select Website Provider"

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
                text = if (editingProfileId == null) "Register Custom API" else "Edit Website Provider",
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

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showWebsiteDropdown = true },
                        modifier = Modifier.fillMaxWidth().testTag("custom_api_website_dropdown_button")
                    ) {
                        Text(
                            text = "Website Provider: $selectedWebsiteLabel",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select website provider")
                    }
                    DropdownMenu(
                        expanded = showWebsiteDropdown,
                        onDismissRequest = { showWebsiteDropdown = false }
                    ) {
                        if (existingProviders.isNotEmpty()) {
                            Text(
                                text = "Added Website Providers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            existingProviders.forEach { profile ->
                                val isSelected = editingProfileId == profile.id
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                text = "${profile.displayHost()} • ${if (profile.apiKey.isNotBlank()) "API key saved" else "No key saved"}",
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
                                    onClick = {
                                        editingProfileId = profile.id
                                        nameInput = profile.name
                                        urlInput = profile.endpointUrl
                                        keyInput = profile.apiKey
                                        modelsInput = profile.allModels().joinToString(", ")
                                        lastAutoFilledName = profile.name
                                        lastAutoFilledKey = profile.apiKey
                                        lastAutoFilledModels = modelsInput
                                        statusMessage = "Loaded saved provider \"${profile.name}\" and its API key."
                                        errorMessage = null
                                        showWebsiteDropdown = false
                                    },
                                    modifier = Modifier.testTag("custom_api_dialog_saved_${profile.id}")
                                )
                            }
                            HorizontalDivider()
                        }

                        Text(
                            text = "Preset Website Providers",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        AiModelDefaults.presetWebsiteEndpoints.forEach { (siteLabel, siteUrl) ->
                            val matchingSaved = existingProviders.firstOrNull {
                                it.endpointUrl.equals(siteUrl, ignoreCase = true) ||
                                    AiModelDefaults.hostOf(it.endpointUrl) == AiModelDefaults.hostOf(siteUrl)
                            }
                            val savedPresetKey = matchingSaved?.apiKey.orEmpty()
                            val isSelected = (editingProfileId != null && editingProfileId == matchingSaved?.id) ||
                                (editingProfileId == null && urlInput.trim().equals(siteUrl, ignoreCase = true))
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(siteLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            text = if (savedPresetKey.isNotBlank()) "$siteUrl • API key saved" else siteUrl,
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
                                onClick = {
                                    if (matchingSaved != null) {
                                        editingProfileId = matchingSaved.id
                                        nameInput = matchingSaved.name
                                        urlInput = matchingSaved.endpointUrl
                                        keyInput = matchingSaved.apiKey
                                        modelsInput = matchingSaved.allModels().joinToString(", ")
                                        lastAutoFilledName = matchingSaved.name
                                        lastAutoFilledKey = matchingSaved.apiKey
                                        lastAutoFilledModels = modelsInput
                                    } else {
                                        editingProfileId = initialProfile?.id
                                        urlInput = siteUrl
                                        if (nameInput.isBlank() || nameInput == lastAutoFilledName) {
                                            nameInput = siteLabel
                                            lastAutoFilledName = siteLabel
                                        }
                                        if (keyInput.isBlank() || keyInput == lastAutoFilledKey) {
                                            keyInput = savedPresetKey
                                            lastAutoFilledKey = savedPresetKey
                                        }
                                        val defaults = AiModelDefaults.defaultModelsForHost(siteUrl).joinToString(", ")
                                        if (modelsInput.isBlank() || modelsInput == lastAutoFilledModels) {
                                            modelsInput = defaults
                                            lastAutoFilledModels = defaults
                                        }
                                    }
                                    statusMessage = if (savedPresetKey.isNotBlank()) {
                                        "Loaded saved API key for $siteLabel."
                                    } else {
                                        null
                                    }
                                    errorMessage = null
                                    showWebsiteDropdown = false
                                },
                                modifier = Modifier.testTag("custom_api_preset_${siteLabel.lowercase()}")
                            )
                        }
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

                val deletableId = editingProfileId
                if (deletableId != null && onDelete != null) {
                    OutlinedButton(
                        onClick = {
                            onDelete(deletableId)
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
                    val matchingExisting = existingProviders.firstOrNull { it.id == editingProfileId } ?: initialProfile
                    val selectedModel = matchingExisting?.selectedModel
                        ?.takeIf { it.isNotBlank() && it in fallbackModels }
                        ?: fallbackModels.firstOrNull().orEmpty()
                    val finalName = nameInput.trim()
                        .ifBlank { AiModelDefaults.inferProviderNameFromUrl(finalUrl) }
                    onSave(
                        CustomAiProviderProfile(
                            id = editingProfileId ?: "custom_${UUID.randomUUID().toString().take(10)}",
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
                Text(if (editingProfileId == null) "Register & Use" else "Save & Use")
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
