package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.AiChatUiState
import com.example.ai.AiProvider
import com.example.data.UserSettings
import com.example.viewmodel.AiUiState

data class AiEditableTarget(
    val id: String,
    val label: String,
    val subtitle: String = "",
)

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
    existingTargets: List<AiEditableTarget> = emptyList(),
    canEditExisting: Boolean = existingTargets.isNotEmpty(),
    defaultEditMode: Boolean = false,
    showTargetPickerInEditMode: Boolean = true,
    editPromptPlaceholder: String = "Describe how to edit, extend, or update the existing info...",
    onEditExisting: ((selectedIds: List<String>, instruction: String) -> Unit)? = null,
) {
    // 0 = AI Parse/Edit, 1 = AI Chat, 2 = Fast Regex
    var mode by remember { mutableIntStateOf(0) }
    var isEditMode by remember(defaultEditMode, canEditExisting) {
        mutableStateOf(defaultEditMode && canEditExisting && onEditExisting != null)
    }
    var inputText by remember { mutableStateOf("") }
    var chatDraft by remember { mutableStateOf("") }
    var showModelConfig by remember { mutableStateOf(false) }
    var targetQuery by remember { mutableStateOf("") }
    var selectedTargetIds by remember(existingTargets) {
        mutableStateOf(
            if (existingTargets.size == 1) setOf(existingTargets.first().id) else emptySet()
        )
    }
    val clipboard = LocalClipboardManager.current
    var copiedTemplate by remember { mutableStateOf(false) }

    val supportsEditAction = onEditExisting != null && canEditExisting
    val filteredTargets = remember(existingTargets, targetQuery) {
        val q = targetQuery.trim()
        val stripped = q.removePrefix("#").trim()
        if (q.isEmpty()) existingTargets
        else existingTargets.filter { t ->
            t.label.contains(q, ignoreCase = true) ||
                t.subtitle.contains(q, ignoreCase = true) ||
                (stripped.isNotEmpty() && (t.label.contains(stripped, ignoreCase = true) || t.subtitle.contains(stripped, ignoreCase = true)))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.AutoAwesome,
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
                if (supportsEditAction) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = !isEditMode,
                            onClick = { isEditMode = false },
                            leadingIcon = {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Create / Add") },
                            modifier = Modifier.testTag("ai_action_create_chip")
                        )
                        FilterChip(
                            selected = isEditMode,
                            onClick = {
                                isEditMode = true
                                if (mode == 2) mode = 0
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Edit Existing") },
                            modifier = Modifier.testTag("ai_action_edit_chip")
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = mode == 0,
                        onClick = { mode = 0 },
                        label = { Text(if (isEditMode) "AI Edit" else "AI Parse") },
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
                    if (!isEditMode) {
                        FilterChip(
                            selected = mode == 2,
                            onClick = { mode = 2 },
                            label = { Text("Fast Regex") },
                            modifier = Modifier.testTag("ai_mode_regex_chip")
                        )
                    }
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
                                text = "Chat with ${userSettings.aiModel} to brainstorm or generate entries, then tap \"Move to AI Parser\" on any reply to review and apply it.",
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
                                            OutlinedButton(
                                                onClick = {
                                                    inputText = msg.content
                                                    mode = 0
                                                },
                                                modifier = Modifier.testTag("use_ai_reply_in_parser_button")
                                            ) {
                                                Text("Move to AI Parser")
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
                            placeholder = { Text("Ask AI to create or edit entries...") },
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
                    if (isEditMode && showTargetPickerInEditMode && existingTargets.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Select existing item(s) to edit (${selectedTargetIds.size} selected)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedTextField(
                                    value = targetQuery,
                                    onValueChange = { targetQuery = it },
                                    placeholder = { Text("Filter by name or #tag...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        if (targetQuery.isNotEmpty()) {
                                            IconButton(onClick = { targetQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear filter", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("ai_edit_target_search_input")
                                )
                                val visibleIds = filteredTargets.map { it.id }.toSet()
                                val allVisibleSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selectedTargetIds }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedTargetIds = if (allVisibleSelected) {
                                                selectedTargetIds - visibleIds
                                            } else {
                                                visibleIds
                                            }
                                        },
                                        enabled = filteredTargets.isNotEmpty(),
                                        modifier = Modifier.testTag("ai_edit_select_all_button")
                                    ) {
                                        Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (allVisibleSelected) "Deselect All" else "Select All (${filteredTargets.size})")
                                    }
                                    if (selectedTargetIds.isNotEmpty()) {
                                        TextButton(
                                            onClick = { selectedTargetIds = emptySet() },
                                            modifier = Modifier.testTag("ai_edit_clear_selection_button")
                                        ) {
                                            Text("Clear")
                                        }
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 150.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (filteredTargets.isEmpty()) {
                                        Text(
                                            text = "No matching entries.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    } else {
                                        filteredTargets.forEach { t ->
                                            val checked = t.id in selectedTargetIds
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedTargetIds = if (checked) {
                                                            selectedTargetIds - t.id
                                                        } else {
                                                            selectedTargetIds + t.id
                                                        }
                                                    }
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Checkbox(
                                                    checked = checked,
                                                    onCheckedChange = { isChecked ->
                                                        selectedTargetIds = if (isChecked) {
                                                            selectedTargetIds + t.id
                                                        } else {
                                                            selectedTargetIds - t.id
                                                        }
                                                    }
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = t.label,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (t.subtitle.isNotBlank()) {
                                                        Text(
                                                            text = t.subtitle,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!isEditMode) {
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
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = {
                            Text(
                                if (isEditMode) {
                                    "Describe edits or additions for existing info"
                                } else {
                                    "Paste notes, stat blocks, or descriptions"
                                }
                            )
                        },
                        placeholder = {
                            Text(if (isEditMode) editPromptPlaceholder else placeholder)
                        },
                        minLines = 4,
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
                            Text(
                                text = if (isEditMode) "Updating with ${userSettings.aiModel}..." else "Parsing with ${userSettings.aiModel}...",
                                style = MaterialTheme.typography.bodySmall
                            )
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
                if (isEditMode && onEditExisting != null) {
                    val canSubmitEdit = inputText.isNotBlank() &&
                        aiState !is AiUiState.Loading &&
                        (!showTargetPickerInEditMode || selectedTargetIds.isNotEmpty())
                    Button(
                        onClick = { onEditExisting(selectedTargetIds.toList(), inputText) },
                        enabled = canSubmitEdit,
                        modifier = Modifier.testTag("confirm_ai_import_button")
                    ) {
                        Text(
                            if (showTargetPickerInEditMode) {
                                "AI Edit (${selectedTargetIds.size})"
                            } else {
                                "AI Apply Edit"
                            }
                        )
                    }
                } else {
                    Button(
                        onClick = { onImport(inputText, mode == 2) },
                        enabled = inputText.isNotBlank() && aiState !is AiUiState.Loading,
                        modifier = Modifier.testTag("confirm_ai_import_button")
                    ) {
                        Text(if (mode == 2) "Parse & Import" else "AI Parse & Import")
                    }
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
