package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.data.GuidesContent
import com.example.viewmodel.AiUiState

@Composable
fun IdeaGuideDialog(
    guideKey: String,
    onDismiss: () -> Unit,
) {
    val guide = GuidesContent.ideaGuides[guideKey] ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = guide.title,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column {
                Text(guide.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    guide.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Spark Ideas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                guide.prompts.forEach { prompt ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "• $prompt",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Quick Checklist",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                guide.checklist.forEach { check ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(check, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_idea_guide_button")
            ) {
                Text("Got it")
            }
        }
    )
}

@Composable
fun AiImportDialog(
    title: String,
    promptTemplate: String,
    placeholder: String,
    aiState: AiUiState,
    onDismiss: () -> Unit,
    onImport: (rawText: String, useRegexFallback: Boolean) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    var useRegexOnly by remember { mutableStateOf(false) }
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
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !useRegexOnly,
                        onClick = { useRegexOnly = false },
                        label = { Text("Gemini AI") },
                        modifier = Modifier.testTag("ai_mode_gemini_chip")
                    )
                    FilterChip(
                        selected = useRegexOnly,
                        onClick = { useRegexOnly = true },
                        label = { Text("Fast Regex") },
                        modifier = Modifier.testTag("ai_mode_regex_chip")
                    )
                }

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

                when (aiState) {
                    is AiUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Parsing with Gemini...", style = MaterialTheme.typography.bodySmall)
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
            Button(
                onClick = { onImport(inputText, useRegexOnly) },
                enabled = inputText.isNotBlank() && aiState !is AiUiState.Loading,
                modifier = Modifier.testTag("confirm_ai_import_button")
            ) {
                Text(if (useRegexOnly) "Parse & Import" else "AI Parse & Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_ai_import_button")) {
                Text("Close")
            }
        }
    )
}
