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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.AiImageGeneratorService
import com.example.ai.AiModelDefaults
import com.example.data.UserSettings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiImageGenerateDialog(
    title: String,
    defaultPrompt: String,
    userSettings: UserSettings,
    onSelectImageModel: (String) -> Unit,
    onGenerate: (prompt: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    onImageGenerated: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var prompt by remember(defaultPrompt) { mutableStateOf(defaultPrompt) }
    var draftModel by remember(userSettings.aiImageModel) { mutableStateOf(userSettings.aiImageModel) }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val hasKey = remember(userSettings.aiImageApiKey, userSettings.aiApiKey, userSettings.aiImageEndpointUrl) {
        AiImageGeneratorService.isConfigured(
            imageApiKeyOverride = userSettings.aiImageApiKey,
            chatApiKeyOverride = userSettings.aiApiKey,
            endpointUrl = userSettings.aiImageEndpointUrl,
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!hasKey) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Configure UNOROUTER_API_KEY / OPENROUTER_API_KEY in AI Studio Secrets or enter an API key in Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Text(
                    text = "Generated artwork will be pasted directly onto this item's profile. Text-only models will show an error.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    enabled = !isGenerating,
                    label = { Text("Image Prompt") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("ai_image_prompt_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draftModel,
                        onValueChange = { draftModel = it },
                        enabled = !isGenerating,
                        label = { Text("Model ID (editable)") },
                        placeholder = { Text(AiModelDefaults.DEFAULT_IMAGE_MODEL) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("ai_image_dialog_model_input")
                    )
                    Button(
                        onClick = {
                            val clean = draftModel.trim()
                            if (clean.isNotEmpty()) onSelectImageModel(clean)
                        },
                        enabled = !isGenerating && draftModel.isNotBlank() && draftModel.trim() != userSettings.aiImageModel,
                        modifier = Modifier.testTag("apply_ai_image_dialog_model_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Apply model", modifier = Modifier.size(16.dp))
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AiModelDefaults.presetImageModels.forEach { model ->
                        FilterChip(
                            selected = userSettings.aiImageModel == model,
                            enabled = !isGenerating,
                            onClick = {
                                draftModel = model
                                onSelectImageModel(model)
                            },
                            label = {
                                Text(
                                    text = model.substringAfter("/"),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanModel = draftModel.trim()
                    if (cleanModel.isNotEmpty() && cleanModel != userSettings.aiImageModel) {
                        onSelectImageModel(cleanModel)
                    }
                    isGenerating = true
                    errorMessage = null
                    onGenerate(
                        prompt,
                        { dataUrl ->
                            isGenerating = false
                            onImageGenerated(dataUrl)
                            onDismiss()
                        },
                        { err ->
                            isGenerating = false
                            errorMessage = err
                        }
                    )
                },
                enabled = !isGenerating && prompt.isNotBlank(),
                modifier = Modifier.testTag("confirm_generate_ai_image_button")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Generate & Paste")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGenerating
            ) {
                Text("Cancel")
            }
        }
    )
}
