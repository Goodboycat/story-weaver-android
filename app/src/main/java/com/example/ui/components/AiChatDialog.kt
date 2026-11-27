package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.AiChatMode
import com.example.ai.AiChatUiState
import com.example.ai.AiModelDefaults
import com.example.ai.AiProvider
import com.example.data.SearchFilterUtil
import com.example.data.UserSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatDialog(
    storyTitle: String,
    userSettings: UserSettings,
    aiChatState: AiChatUiState,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onDeleteMessages: ((Set<String>) -> Unit)? = null,
    onSelectProvider: (AiProvider) -> Unit,
    onSelectModel: (String) -> Unit,
    onSendImagePrompt: ((String) -> Unit)? = null,
    onClearError: (() -> Unit)? = null,
    onSelectEndpointUrl: ((String) -> Unit)? = null,
    onSelectApiKey: ((String) -> Unit)? = null,
    onSelectImageEndpointUrl: ((String) -> Unit)? = null,
    onSelectImageModel: ((String) -> Unit)? = null,
    onSelectImageApiKey: ((String) -> Unit)? = null,
    onPostToPartyChat: ((String) -> Unit)? = null,
    onNotify: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    var chatMode by remember { mutableStateOf(AiChatMode.TEXT) }
    var showModelPicker by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var multiSelect by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var pendingSafImageDataUrl by remember { mutableStateOf<String?>(null) }
    var fullScreenImage by remember { mutableStateOf<Pair<String, String>?>(null) }
    var galleryFeedback by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    val visibleMessages = remember(aiChatState.messages, searchQuery) {
        aiChatState.messages.filter { SearchFilterUtil.matchesAiChatMessageSearch(it, searchQuery) }
    }

    val lastUserPrompt = remember(aiChatState.messages) {
        aiChatState.messages.lastOrNull { it.role == "user" }
            ?.content
            ?.removePrefix("🎨 [Image] ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    val safCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val dataUrl = pendingSafImageDataUrl
        pendingSafImageDataUrl = null
        if (uri != null && dataUrl != null) {
            scope.launch {
                ImagePickerUtil.writeDataUrlToUri(context, uri, dataUrl)
                    .onSuccess {
                        galleryFeedback = "Saved image to device storage."
                        onNotify?.invoke("Saved image to device storage.")
                    }
                    .onFailure { err ->
                        galleryFeedback = err.localizedMessage ?: "Could not save image."
                    }
            }
        }
    }

    LaunchedEffect(aiChatState.messages.size) {
        if (aiChatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(aiChatState.messages.lastIndex)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_ai_chat_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close AI Chat")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "AI Assistant Chat",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val activeSub = if (chatMode == AiChatMode.IMAGE && onSendImagePrompt != null) {
                                "Image • ${userSettings.aiImageModel}"
                            } else {
                                "${userSettings.aiProvider.label} • ${userSettings.aiModel}"
                            }
                            Text(
                                text = activeSub,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        if (aiChatState.messages.isNotEmpty()) {
                            IconButton(
                                onClick = { showSearchBar = !showSearchBar },
                                modifier = Modifier.testTag("search_ai_chat_button")
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search AI Chat",
                                    tint = if (showSearchBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (onDeleteMessages != null) {
                                IconButton(
                                    onClick = {
                                        multiSelect = !multiSelect
                                        selectedIds = emptySet()
                                        if (multiSelect) showSearchBar = true
                                    },
                                    modifier = Modifier.testTag("multiselect_ai_chat_button")
                                ) {
                                    Icon(
                                        Icons.Default.Checklist,
                                        contentDescription = "Select AI Chat messages",
                                        tint = if (multiSelect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        FilterChip(
                            selected = showModelPicker,
                            onClick = { showModelPicker = !showModelPicker },
                            leadingIcon = {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Model") },
                            modifier = Modifier.testTag("toggle_ai_model_picker_button")
                        )
                        IconButton(
                            onClick = {
                                onClearChat()
                                selectedIds = emptySet()
                                multiSelect = false
                            },
                            enabled = aiChatState.messages.isNotEmpty(),
                            modifier = Modifier.testTag("clear_ai_chat_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear AI Chat")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedVisibility(visible = showModelPicker) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        AiModelSelectorBar(
                            selectedProvider = userSettings.aiProvider,
                            selectedModel = userSettings.aiModel,
                            onSelectProvider = onSelectProvider,
                            onSelectModel = onSelectModel,
                            endpointUrl = userSettings.aiEndpointUrl,
                            apiKey = userSettings.aiApiKey,
                            onSelectEndpointUrl = onSelectEndpointUrl,
                            onSelectApiKey = onSelectApiKey,
                            imageEndpointUrl = userSettings.aiImageEndpointUrl,
                            imageModel = userSettings.aiImageModel,
                            imageApiKey = userSettings.aiImageApiKey,
                            onSelectImageEndpointUrl = onSelectImageEndpointUrl,
                            onSelectImageModel = onSelectImageModel,
                            onSelectImageApiKey = onSelectImageApiKey,
                            showAdvancedApiConfig = onSelectEndpointUrl != null || onSelectApiKey != null,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                if (galleryFeedback != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = galleryFeedback!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { galleryFeedback = null }) {
                                Text("OK")
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = showSearchBar && aiChatState.messages.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search AI chat messages or #tags...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("ai_chat_search_input")
                    )
                }

                if (multiSelect && onDeleteMessages != null && aiChatState.messages.isNotEmpty()) {
                    val visibleIds = visibleMessages.map { it.id }.toSet()
                    val allVisibleSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selectedIds }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    selectedIds = if (allVisibleSelected) selectedIds - visibleIds else visibleIds
                                },
                                enabled = visibleMessages.isNotEmpty(),
                                modifier = Modifier.testTag("select_all_ai_chat_button")
                            ) {
                                Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (allVisibleSelected) "Deselect All" else "Select All (${visibleMessages.size})")
                            }
                            if (selectedIds.isNotEmpty()) {
                                TextButton(
                                    onClick = { selectedIds = emptySet() },
                                    modifier = Modifier.testTag("clear_selection_ai_chat_button")
                                ) {
                                    Text("Clear (${selectedIds.size})")
                                }
                            }
                        }
                        if (selectedIds.isNotEmpty()) {
                            Button(
                                onClick = {
                                    onDeleteMessages(selectedIds)
                                    selectedIds = emptySet()
                                    multiSelect = false
                                },
                                modifier = Modifier.fillMaxWidth().testTag("batch_delete_ai_chat_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Delete Selected (${selectedIds.size})")
                            }
                        }
                    }
                }

                if (aiChatState.messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Ask AI about \"$storyTitle\"",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Use Text mode (${AiModelDefaults.DEFAULT_UNOROUTER_MODEL} or gpt-6.1-sol:free) for lore, parser-ready NPCs/gear/locations, or <svg> artwork in code — or switch to Image mode (${AiModelDefaults.DEFAULT_IMAGE_MODEL} on Unorouter) to generate downloadable images in chat.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (visibleMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No AI chat messages match \"$searchQuery\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleMessages, key = { it.id }) { msg ->
                            val isUser = msg.role == "user"
                            val isSelected = msg.id in selectedIds
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = when {
                                    multiSelect -> Arrangement.Start
                                    isUser -> Arrangement.End
                                    else -> Arrangement.Start
                                }
                            ) {
                                if (multiSelect) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedIds = if (checked) selectedIds + msg.id else selectedIds - msg.id
                                        },
                                        modifier = Modifier.testTag("select_ai_chat_msg_${msg.id}")
                                    )
                                }
                                Surface(
                                    color = if (isUser) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .then(if (multiSelect) Modifier.weight(1f) else Modifier.fillMaxWidth(0.92f))
                                        .clickable(enabled = multiSelect) {
                                            selectedIds = if (isSelected) selectedIds - msg.id else selectedIds + msg.id
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isUser) "You" else "AI (${msg.modelUsed.ifBlank { userSettings.aiModel }})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            IconButton(
                                                onClick = {
                                                    val copyText = if (isUser) {
                                                        msg.content.removePrefix("🎨 [Image] ").trim()
                                                    } else {
                                                        msg.content
                                                    }
                                                    clipboard.setText(AnnotatedString(copyText))
                                                    galleryFeedback = if (isUser) "Copied prompt to clipboard." else "Copied reply to clipboard."
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = if (isUser) "Copy prompt" else "Copy reply",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        val msgBitmap = remember(msg.imageDataUrl) {
                                            ImagePickerUtil.decodeDataUrlBitmap(msg.imageDataUrl)
                                        }
                                        if (msgBitmap != null && msg.imageDataUrl != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(MaterialTheme.shapes.medium)
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .clickable {
                                                        fullScreenImage = "AI Artwork" to msg.imageDataUrl
                                                    }
                                            ) {
                                                Image(
                                                    bitmap = msgBitmap.asImageBitmap(),
                                                    contentDescription = "Generated AI image — tap to enlarge",
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 140.dp, max = 300.dp)
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        ImagePickerUtil.saveDataUrlToGallery(
                                                            context = context,
                                                            dataUrl = msg.imageDataUrl,
                                                            displayNamePrefix = "storyweaver_ai"
                                                        ).onSuccess { resMsg ->
                                                            galleryFeedback = resMsg
                                                            onNotify?.invoke(resMsg)
                                                        }.onFailure { err ->
                                                            if (err.message == "NEEDS_SAF") {
                                                                pendingSafImageDataUrl = msg.imageDataUrl
                                                                safCreateLauncher.launch("storyweaver_ai_${System.currentTimeMillis()}.png")
                                                            } else {
                                                                galleryFeedback = err.localizedMessage ?: "Failed to save to gallery."
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.testTag("download_chat_image_button_${msg.id}")
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Download to Gallery")
                                            }
                                        }

                                        Text(
                                            text = msg.content,
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        if (!isUser && onPostToPartyChat != null) {
                                            TextButton(
                                                onClick = { onPostToPartyChat(msg.content) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Share to Party Chat", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (aiChatState.isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = if (chatMode == AiChatMode.IMAGE) {
                                "Generating image with ${userSettings.aiImageModel}..."
                            } else {
                                "Thinking with ${userSettings.aiModel}..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onSendImagePrompt != null || lastUserPrompt != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onSendImagePrompt != null) {
                            FilterChip(
                                selected = chatMode == AiChatMode.TEXT,
                                onClick = { chatMode = AiChatMode.TEXT },
                                leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Text") },
                                modifier = Modifier.testTag("ai_chat_mode_text")
                            )
                            FilterChip(
                                selected = chatMode == AiChatMode.IMAGE,
                                onClick = { chatMode = AiChatMode.IMAGE },
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Image") },
                                modifier = Modifier.testTag("ai_chat_mode_image")
                            )
                        }
                        if (lastUserPrompt != null) {
                            Spacer(Modifier.weight(1f))
                            OutlinedButton(
                                onClick = {
                                    draft = lastUserPrompt
                                    clipboard.setText(AnnotatedString(lastUserPrompt))
                                    galleryFeedback = "Loaded & copied last prompt."
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("reuse_last_prompt_button")
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Last Prompt", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = {
                            Text(
                                if (chatMode == AiChatMode.IMAGE) {
                                    "Describe an image to generate..."
                                } else {
                                    "Ask AI or request <svg> art in code..."
                                }
                            )
                        },
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_chat_input")
                    )
                    Button(
                        onClick = {
                            val text = draft.trim()
                            if (text.isNotEmpty()) {
                                if (chatMode == AiChatMode.IMAGE && onSendImagePrompt != null) {
                                    onSendImagePrompt(text)
                                } else {
                                    onSendMessage(text)
                                }
                                draft = ""
                            }
                        },
                        enabled = draft.isNotBlank() && !aiChatState.isLoading,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("ai_chat_send_button")
                    ) {
                        Icon(
                            imageVector = if (chatMode == AiChatMode.IMAGE) Icons.Default.Image else Icons.AutoMirrored.Filled.Send,
                            contentDescription = if (chatMode == AiChatMode.IMAGE) "Generate image" else "Send to AI"
                        )
                    }
                }
            }
        }
    }

    if (aiChatState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { onClearError?.invoke() },
            icon = {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Model Error") },
            text = {
                Text(
                    text = aiChatState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearError?.invoke()
                        showModelPicker = true
                    },
                    modifier = Modifier.testTag("ai_chat_error_switch_model_button")
                ) {
                    Text("Change Model")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onClearError?.invoke() },
                    modifier = Modifier.testTag("ai_chat_error_dismiss_button")
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    fullScreenImage?.let { (imgTitle, imgUrl) ->
        FullImageViewerDialog(
            title = imgTitle,
            imageUrl = imgUrl,
            fileNamePrefix = "storyweaver_ai",
            onDismiss = { fullScreenImage = null },
            onNotify = { msg ->
                galleryFeedback = msg
                onNotify?.invoke(msg)
            }
        )
    }
}
