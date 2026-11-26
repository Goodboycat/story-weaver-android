package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.ai.AiChatUiState
import com.example.ai.AiProvider
import com.example.data.UserSettings
import com.example.viewmodel.AiUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiImportDialog(
    title: String,
    promptTemplate: String,
    placeholder: String,
    aiState: AiUiState,
    userSettings: UserSettings,
    aiChatState: AiChatUiState,
    onSelectProvider: (AiProvider) -> Unit,
    onSelectModel: (String) -> Unit,
    onSendAiChat: (String) -> Unit,
    onDismiss: () -> Unit,
    onImport: (rawText: String, useRegexFallback: Boolean) -> Unit,
) {
    // 0 = AI Parse, 1 = AI Chat, 2 = Fast Regex
    var mode by remember { mutableIntStateOf(0) }
    var inputText by remember { mutableStateOf("") }
    var chatDraft by remember { mutableStateOf("") }
    var showModelConfig by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    var copiedTemplate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${userSettings.aiProvider.label} • ${userSettings.aiModel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = mode == 0,
                        onClick = { mode = 0 },
                        label = { Text("AI Parse") },
                        modifier = Modifier.testTag("ai_mode_gemini_chip")
                    )
                    FilterChip(
                        selected = mode == 1,
                        onClick = { mode = 1 },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("AI Chat") },
                        modifier = Modifier.testTag("ai_mode_chat_chip")
                    )
                    FilterChip(
                        selected = mode == 2,
                        onClick = { mode = 2 },
                        label = { Text("Fast Regex") },
                        modifier = Modifier.testTag("ai_mode_regex_chip")
                    )
                    FilterChip(
                        selected = showModelConfig,
                        onClick = { showModelConfig = !showModelConfig },
                        leadingIcon = {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Model") },
                        modifier = Modifier.testTag("ai_import_model_toggle_chip")
                    )
                }

                AnimatedVisibility(visible = showModelConfig) {
                    AiModelSelectorBar(
                        selectedProvider = userSettings.aiProvider,
                        selectedModel = userSettings.aiModel,
                        onSelectProvider = onSelectProvider,
                        onSelectModel = onSelectModel,
                        apiKey = userSettings.aiApiKey,
                    )
                }

                if (mode == 1) {
                    // Interactive AI Chat inside the AI Parser
                    if (aiChatState.messages.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Chat with ${userSettings.aiModel} to brainstorm or generate entries, then tap \"Use in Parser\" on any reply to import it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            aiChatState.messages.takeLast(8).forEach { msg ->
                                val isUser = msg.role == "user"
                                Surface(
                                    color = if (isUser) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (isUser) "You" else "AI (${msg.modelUsed.ifBlank { userSettings.aiModel }})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(msg.content, style = MaterialTheme.typography.bodySmall)
                                        if (!isUser) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        inputText = msg.content
                                                        mode = 0
                                                    },
                                                    modifier = Modifier.testTag("use_ai_reply_in_parser_button")
                                                ) {
                                                    Text("Use in Parser")
                                                }
                                                TextButton(
                                                    onClick = {
                                                        inputText = msg.content
                                                        onImport(msg.content, false)
                                                    },
                                                    enabled = aiState !is AiUiState.Loading
                                                ) {
                                                    Text("Import Now")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (aiChatState.errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = aiChatState.errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (aiChatState.isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Asking ${userSettings.aiModel}...", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = chatDraft,
                            onValueChange = { chatDraft = it },
                            placeholder = { Text("Ask AI to create entries...") },
                            maxLines = 3,
                            modifier = Modifier.weight(1f).testTag("ai_parser_chat_input")
                        )
                        IconButton(
                            onClick = {
                                val text = chatDraft.trim()
                                if (text.isNotEmpty()) {
                                    onSendAiChat(text)
                                    chatDraft = ""
                                }
                            },
                            enabled = chatDraft.isNotBlank() && !aiChatState.isLoading,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("ai_parser_chat_send_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send to AI")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(promptTemplate))
                            copiedTemplate = true
                        },
                        modifier = Modifier.fillMaxWidth().testTag("copy_prompt_template_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (copiedTemplate) "Prompt Template Copied!" else "Copy Prompt Template")
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("Paste notes, stat blocks, or descriptions") },
                        placeholder = { Text(placeholder) },
                        minLines = 5,
                        maxLines = 9,
                        modifier = Modifier.fillMaxWidth().testTag("ai_import_input")
                    )
                }

                when (aiState) {
                    is AiUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Parsing with ${userSettings.aiModel}...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is AiUiState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = aiState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    is AiUiState.Success -> {
                        Text(
                            text = aiState.summary,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    AiUiState.Idle -> Unit
                }
            }
        },
        confirmButton = {
            if (mode != 1) {
                Button(
                    onClick = { onImport(inputText, mode == 2) },
                    enabled = inputText.isNotBlank() && aiState !is AiUiState.Loading,
                    modifier = Modifier.testTag("confirm_ai_import_button")
                ) {
                    Text(if (mode == 2) "Parse & Import" else "AI Parse & Import")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_ai_import_button")) {
                Text("Close")
            }
        }
    )
}
