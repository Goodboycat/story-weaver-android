package com.example.ui.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.AiChatUiState
import com.example.ai.AiPromptTemplates
import com.example.ai.AiProvider
import com.example.ai.EmojiHelper
import com.example.data.JsonCodecs
import com.example.data.Story
import com.example.data.UserSettings
import com.example.data.WorldItem
import com.example.ui.components.AiImageGenerateDialog
import com.example.ui.components.AiImportDialog
import com.example.ui.components.AllEmojiPickerDialog
import com.example.ui.components.IdeaGuideDialog
import com.example.ui.components.ImagePickerUtil
import com.example.viewmodel.AiUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorldListTab(
    story: Story,
    category: String, // "location" | "power" | "note"
    items: List<WorldItem>,
    currentUserId: String,
    isGmMode: Boolean,
    aiState: AiUiState,
    userSettings: UserSettings,
    aiChatState: AiChatUiState,
    onSelectAiProvider: (AiProvider) -> Unit,
    onSelectAiModel: (String) -> Unit,
    onSelectAiImageModel: (String) -> Unit,
    onGenerateAiImage: (prompt: String, maxDimension: Int, onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    onSendAiChat: (String) -> Unit,
    onSaveItem: (WorldItem) -> Unit,
    onReorder: (List<WorldItem>) -> Unit,
    onDeleteItems: (List<String>) -> Unit,
    onImportAi: (rawText: String, startOrder: Int, useRegex: Boolean, onDone: () -> Unit) -> Unit,
    onResetAiState: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val categoryItems = remember(items, category) { items.filter { it.category == category } }
    var searchQuery by remember { mutableStateOf("") }
    val visible = remember(categoryItems, searchQuery) {
        if (searchQuery.isBlank()) categoryItems
        else categoryItems.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.subtitle.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    var multiSelect by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var editingItem by remember { mutableStateOf<WorldItem?>(null) }
    var showAiImport by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    val guideKey = when (category) {
        "location" -> "locations"
        "power" -> "powers"
        else -> "notes"
    }
    val defaultEmoji = when (category) {
        "location" -> "🏰"
        "power" -> "✨"
        else -> "📜"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search ${category}s...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("search_${category}_input")
                )
                IconButton(onClick = { showGuide = true }) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "Idea Guide")
                }
                if (isGmMode) {
                    IconButton(onClick = {
                        multiSelect = !multiSelect
                        selectedIds = emptySet()
                    }) {
                        Icon(Icons.Default.Checklist, contentDescription = "Select")
                    }
                }
            }

            if (isGmMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            editingItem = WorldItem(
                                storyId = story.id,
                                gmUserId = story.gmUserId,
                                ownerId = currentUserId,
                                memberUids = story.memberUids,
                                category = category,
                                iconEmoji = defaultEmoji,
                                sortOrder = categoryItems.size.toLong()
                            )
                        },
                        modifier = Modifier.weight(1f).testTag("create_${category}_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New ${category.replaceFirstChar { it.uppercase() }}")
                    }
                    OutlinedButton(
                        onClick = {
                            onResetAiState()
                            showAiImport = true
                        },
                        modifier = Modifier.weight(1f).testTag("ai_import_${category}_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Import")
                    }
                }
            }

            if (multiSelect && selectedIds.isNotEmpty()) {
                Button(
                    onClick = {
                        onDeleteItems(selectedIds.toList())
                        selectedIds = emptySet()
                        multiSelect = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete Selected (${selectedIds.size})")
                }
            }
        }

        if (visible.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No ${category}s yet. Create cards or import with AI!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(visible, key = { _, item -> item.id }) { index, item ->
                    val tags = remember(item.tagsJson) { JsonCodecs.decodeStringList(item.tagsJson) }
                    val itemBmp = remember(item.imageUrl) { ImagePickerUtil.decodeDataUrlBitmap(item.imageUrl) }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (multiSelect) {
                                    selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
                                } else if (isGmMode) {
                                    editingItem = item
                                }
                            }
                            .testTag("world_card_${item.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (multiSelect) {
                                    Checkbox(
                                        checked = item.id in selectedIds,
                                        onCheckedChange = { checked ->
                                            selectedIds = if (checked) selectedIds + item.id else selectedIds - item.id
                                        }
                                    )
                                }
                                if (itemBmp != null) {
                                    Image(
                                        bitmap = itemBmp.asImageBitmap(),
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Text(item.iconEmoji.ifBlank { defaultEmoji }, style = MaterialTheme.typography.titleLarge)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.subtitle.isNotBlank()) {
                                        Text(
                                            item.subtitle,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (isGmMode && searchQuery.isBlank()) {
                                    IconButton(
                                        enabled = index > 0,
                                        onClick = {
                                            val list = categoryItems.toMutableList()
                                            val swapped = list.removeAt(index)
                                            list.add(index - 1, swapped)
                                            onReorder(list)
                                        }
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        enabled = index < visible.lastIndex,
                                        onClick = {
                                            val list = categoryItems.toMutableList()
                                            val swapped = list.removeAt(index)
                                            list.add(index + 1, swapped)
                                            onReorder(list)
                                        }
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            if (item.description.isNotBlank()) {
                                Text(
                                    item.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (tags.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    tags.forEach { tag ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                "#$tag",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
    }

    if (editingItem != null) {
        val current = editingItem!!
        var title by remember(current) { mutableStateOf(current.title) }
        var subtitle by remember(current) { mutableStateOf(current.subtitle) }
        var description by remember(current) { mutableStateOf(current.description) }
        var emoji by remember(current) { mutableStateOf(current.iconEmoji.ifBlank { defaultEmoji }) }
        var imageUrl by remember(current) { mutableStateOf(current.imageUrl) }
        var tagsText by remember(current) { mutableStateOf(JsonCodecs.decodeStringList(current.tagsJson).joinToString(", ")) }
        var showEmojiPicker by remember(current) { mutableStateOf(false) }
        var showImageGen by remember(current) { mutableStateOf(false) }

        val worldImagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    val b64 = ImagePickerUtil.uriToResizedBase64DataUrl(context, uri, maxDimension = 320)
                    if (b64 != null) imageUrl = b64
                }
            }
        }
        val previewBmp = remember(imageUrl) { ImagePickerUtil.decodeDataUrlBitmap(imageUrl) }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("${if (current.id.isBlank()) "New" else "Edit"} ${category.replaceFirstChar { it.uppercase() }}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (previewBmp != null) {
                                    Image(
                                        bitmap = previewBmp.asImageBitmap(),
                                        contentDescription = title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(emoji, style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedButton(
                                onClick = { showImageGen = true },
                                modifier = Modifier.testTag("generate_world_image_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("AI Image")
                            }
                            OutlinedButton(
                                onClick = {
                                    worldImagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Upload")
                            }
                            OutlinedButton(
                                onClick = { showEmojiPicker = true },
                                modifier = Modifier.testTag("open_all_emojis_world_button")
                            ) {
                                Icon(Icons.Default.EmojiEmotions, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("All Emojis")
                            }
                        }
                    }

                    if (imageUrl != null) {
                        TextButton(onClick = { imageUrl = null }) { Text("Use Emoji Instead") }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("🏰", "🍺", "🌲", "⛰️", "🏛️", "✨", "🔥", "❄️", "⚡", "📜", "👑", "☠️").forEach { em ->
                            FilterChip(
                                selected = emoji == em && imageUrl == null,
                                onClick = {
                                    emoji = em
                                    imageUrl = null
                                },
                                label = { Text(em) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (current.id.isBlank()) {
                                emoji = EmojiHelper.suggestEmoji(it, defaultEmoji)
                            }
                        },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("world_item_title_input")
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Subtitle / Type") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().testTag("world_item_desc_input")
                    )
                    OutlinedTextField(
                        value = tagsText,
                        onValueChange = { tagsText = it },
                        label = { Text("Tags (comma separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onSaveItem(
                            current.copy(
                                title = title.trim().ifBlank { "Untitled" },
                                subtitle = subtitle.trim(),
                                description = description.trim(),
                                iconEmoji = emoji,
                                imageUrl = imageUrl,
                                tagsJson = JsonCodecs.encodeStringList(tags),
                            )
                        )
                        editingItem = null
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.testTag("save_world_item_button")
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text("Cancel") }
            }
        )

        if (showEmojiPicker) {
            AllEmojiPickerDialog(
                currentEmoji = emoji,
                onSelect = { picked ->
                    emoji = picked
                    imageUrl = null
                },
                onDismiss = { showEmojiPicker = false }
            )
        }

        if (showImageGen) {
            val defaultWorldPrompt = buildString {
                append("Fantasy tabletop RPG $category artwork: ")
                append(title.trim().ifBlank { category })
                if (subtitle.isNotBlank()) append(" (${subtitle.trim()})")
                if (description.isNotBlank()) append(". ${description.trim().take(180)}")
                append(". Rich fantasy illustration, no text.")
            }
            AiImageGenerateDialog(
                title = "Generate ${category.replaceFirstChar { it.uppercase() }} Artwork",
                defaultPrompt = defaultWorldPrompt,
                userSettings = userSettings,
                onSelectImageModel = onSelectAiImageModel,
                onGenerate = { prompt, onOk, onErr ->
                    onGenerateAiImage(prompt, 320, onOk, onErr)
                },
                onImageGenerated = { dataUrl ->
                    imageUrl = dataUrl
                    if (current.id.isNotBlank() && title.isNotBlank()) {
                        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        val saved = current.copy(
                            title = title.trim(),
                            subtitle = subtitle.trim(),
                            description = description.trim(),
                            iconEmoji = emoji,
                            imageUrl = dataUrl,
                            tagsJson = JsonCodecs.encodeStringList(tags),
                        )
                        onSaveItem(saved)
                        editingItem = saved
                    }
                },
                onDismiss = { showImageGen = false }
            )
        }
    }

    if (showAiImport) {
        AiImportDialog(
            title = "Import ${category.replaceFirstChar { it.uppercase() }}s",
            promptTemplate = AiPromptTemplates.worldItemPrompt(category),
            placeholder = "Paste notes separated by blank lines or paragraphs...",
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSelectProvider = onSelectAiProvider,
            onSelectModel = onSelectAiModel,
            onSendAiChat = onSendAiChat,
            onDismiss = { showAiImport = false },
            onImport = { raw, useRegex ->
                onImportAi(raw, categoryItems.size, useRegex) {
                    showAiImport = false
                }
            }
        )
    }

    if (showGuide) {
        IdeaGuideDialog(guideKey = guideKey, onDismiss = { showGuide = false })
    }
}
