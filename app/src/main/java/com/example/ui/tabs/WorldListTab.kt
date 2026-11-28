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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.AiChatUiState
import com.example.ai.AiPromptTemplates
import com.example.ai.AiProvider
import com.example.ai.EmojiHelper
import com.example.data.JsonCodecs
import com.example.data.SavedMap
import com.example.data.SearchFilterUtil
import com.example.data.Story
import com.example.data.UserSettings
import com.example.data.WorldItem
import com.example.ui.components.AiEditableTarget
import com.example.ui.components.AiImageGenerateDialog
import com.example.ui.components.AiImportDialog
import com.example.ui.components.AllEmojiPickerDialog
import com.example.ui.components.FullImageViewerDialog
import com.example.ui.components.IdeaGuideDialog
import com.example.ui.components.ImagePickerUtil
import com.example.viewmodel.AiUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorldListTab(
    story: Story,
    category: String, // "location" | "power" | "note" | "item"
    items: List<WorldItem>,
    currentUserId: String,
    isGmMode: Boolean,
    aiState: AiUiState,
    userSettings: UserSettings,
    aiChatState: AiChatUiState,
    initialSearchQuery: String = "",
    initialSearchTrigger: Int = 0,
    onSelectAiProvider: (AiProvider) -> Unit,
    onSelectAiModel: (String) -> Unit,
    onSelectAiImageModel: (String) -> Unit,
    onGenerateAiImage: (prompt: String, maxDimension: Int, onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    onSendAiChat: (String) -> Unit,
    onSaveItem: (WorldItem) -> Unit,
    onReorder: (List<WorldItem>) -> Unit,
    onDeleteItems: (List<String>) -> Unit,
    onImportAi: (rawText: String, startOrder: Int, useRegex: Boolean, onDone: () -> Unit) -> Unit,
    onEditWorldItemsAi: ((targets: List<WorldItem>, instruction: String, onDone: () -> Unit) -> Unit)? = null,
    onAssistSingleWorldItemAi: ((current: WorldItem, prompt: String, isEditExisting: Boolean, useRegex: Boolean, onApplied: (WorldItem) -> Unit) -> Unit)? = null,
    onResetAiState: () -> Unit,
    onShareToChat: ((WorldItem) -> Unit)? = null,
    aiConfigActions: com.example.ui.components.AiProviderConfigActions? = null,
    savedMaps: List<SavedMap> = emptyList(),
    activeMapId: String = "",
    onSaveLocationAndGenerateMap: ((WorldItem) -> Unit)? = null,
    onOpenSavedMap: ((SavedMap) -> Unit)? = null,
    onGenerateLocationMap: ((WorldItem) -> Unit)? = null,
    onLinkLocationToMap: ((SavedMap, WorldItem?) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val categoryItems = remember(items, category) { items.filter { it.category == category } }
    var searchQuery by remember { mutableStateOf(initialSearchQuery) }
    LaunchedEffect(initialSearchQuery, initialSearchTrigger) {
        if (initialSearchTrigger > 0 || initialSearchQuery.isNotBlank()) {
            searchQuery = initialSearchQuery
        }
    }
    val visible = remember(categoryItems, searchQuery) {
        categoryItems.filter { SearchFilterUtil.matchesWorldItemSearch(it, searchQuery) }
    }

    var multiSelect by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var editingItem by remember { mutableStateOf<WorldItem?>(null) }
    var fullScreenImage by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showAiImport by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    val guideKey = when (category) {
        "location" -> "locations"
        "power" -> "powers"
        "item" -> "items"
        else -> "notes"
    }
    val defaultEmoji = when (category) {
        "location" -> "🏰"
        "power" -> "✨"
        "item" -> "⚔️"
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
                    placeholder = { Text("Search ${category}s, #tags...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("search_${category}_input")
                )
                IconButton(onClick = { showGuide = true }) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "Idea Guide")
                }
                if (isGmMode) {
                    IconButton(
                        onClick = {
                            multiSelect = !multiSelect
                            selectedIds = emptySet()
                        },
                        modifier = Modifier.testTag("multiselect_${category}_button")
                    ) {
                        Icon(
                            Icons.Default.Checklist,
                            contentDescription = "Select",
                            tint = if (multiSelect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        Text("AI Create / Edit")
                    }
                }
            }

            if (multiSelect) {
                val visibleIds = visible.map { it.id }.toSet()
                val allVisibleSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selectedIds }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedIds = if (allVisibleSelected) {
                                selectedIds - visibleIds
                            } else {
                                visibleIds
                            }
                        },
                        enabled = visible.isNotEmpty(),
                        modifier = Modifier.testTag("select_all_${category}_button")
                    ) {
                        Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (allVisibleSelected) "Deselect All" else "Select All (${visible.size})")
                    }
                    if (selectedIds.isNotEmpty()) {
                        TextButton(
                            onClick = { selectedIds = emptySet() },
                            modifier = Modifier.testTag("clear_selection_${category}_button")
                        ) {
                            Text("Clear (${selectedIds.size})")
                        }
                    }
                }
                if (selectedIds.isNotEmpty()) {
                    Button(
                        onClick = {
                            onDeleteItems(selectedIds.toList())
                            selectedIds = emptySet()
                            multiSelect = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("batch_delete_${category}_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Delete Selected (${selectedIds.size})")
                    }
                }
            }
        }

        if (visible.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (searchQuery.isNotBlank()) {
                        "No ${category}s match \"$searchQuery\"."
                    } else {
                        "No ${category}s yet. Create cards or use AI Create / Edit!"
                    },
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
                                            .clickable(enabled = !multiSelect && !item.imageUrl.isNullOrBlank()) {
                                                fullScreenImage = item.title to item.imageUrl!!
                                            }
                                    )
                                } else {
                                    Text(item.iconEmoji.ifBlank { defaultEmoji }, style = MaterialTheme.typography.titleLarge)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 3,
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
                                if (!multiSelect && onShareToChat != null) {
                                    IconButton(
                                        onClick = { onShareToChat(item) },
                                        modifier = Modifier.testTag("share_world_${item.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Campaign,
                                            contentDescription = "Reference in Party Chat",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
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
                            if (category == "location") {
                                val linkedMap = remember(savedMaps, item.id, item.linkedMapId) {
                                    savedMaps.firstOrNull {
                                        (item.linkedMapId.isNotBlank() && it.id == item.linkedMapId) ||
                                            it.linkedLocationId == item.id
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (linkedMap != null) {
                                        val isActiveBoard = linkedMap.id == activeMapId
                                        Surface(
                                            color = if (isActiveBoard) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                            } else {
                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                            },
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "🗺️ ${linkedMap.name} (${linkedMap.width}×${linkedMap.height})" +
                                                    if (isActiveBoard) " • Active" else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        if (onOpenSavedMap != null) {
                                            TextButton(
                                                onClick = { onOpenSavedMap(linkedMap) },
                                                modifier = Modifier.testTag("open_location_map_${item.id}")
                                            ) {
                                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Open Map", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    } else if (isGmMode && onGenerateLocationMap != null) {
                                        Text(
                                            text = "No synced map yet",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = { onGenerateLocationMap(item) },
                                            modifier = Modifier.testTag("generate_location_map_${item.id}")
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("AI Map", style = MaterialTheme.typography.labelSmall)
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
        var showSingleAiEdit by remember(current) { mutableStateOf(false) }
        var autoGenerateMapOnSave by remember(current) { mutableStateOf(false) }
        var showLinkMapMenu by remember(current) { mutableStateOf(false) }
        val linkedMap = remember(savedMaps, current.id, current.linkedMapId) {
            if (category == "location") {
                savedMaps.firstOrNull {
                    (current.linkedMapId.isNotBlank() && it.id == current.linkedMapId) ||
                        (current.id.isNotBlank() && it.linkedLocationId == current.id)
                }
            } else null
        }

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
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !imageUrl.isNullOrBlank()) {
                                    fullScreenImage = title.ifBlank { category } to imageUrl!!
                                }
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
                            if (onAssistSingleWorldItemAi != null) {
                                OutlinedButton(
                                    onClick = {
                                        onResetAiState()
                                        showSingleAiEdit = true
                                    },
                                    modifier = Modifier.testTag("ai_edit_single_${category}_button")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("AI Create / Edit")
                                }
                            }
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
                                Text("All Emojis ($emoji)")
                            }
                        }
                    }

                    if (imageUrl != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { fullScreenImage = title.ifBlank { category } to imageUrl!! }) {
                                Text("View / Save Image")
                            }
                            TextButton(onClick = { imageUrl = null }) { Text("Use Emoji Instead") }
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
                    if (category == "location") {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Synced Map",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (linkedMap != null) {
                                    Text(
                                        text = "🗺️ ${linkedMap.name} (${linkedMap.width}×${linkedMap.height})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (onOpenSavedMap != null) {
                                            OutlinedButton(
                                                onClick = {
                                                    onOpenSavedMap(linkedMap)
                                                    editingItem = null
                                                }
                                            ) {
                                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Open Map", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        if (onGenerateLocationMap != null && current.id.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    onGenerateLocationMap(current)
                                                    editingItem = null
                                                }
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Regenerate AI Map", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                } else if (current.id.isNotBlank()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (onGenerateLocationMap != null) {
                                            OutlinedButton(
                                                onClick = {
                                                    onGenerateLocationMap(current)
                                                    editingItem = null
                                                }
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("AI Generate Map", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        if (savedMaps.isNotEmpty() && onLinkLocationToMap != null) {
                                            Box {
                                                OutlinedButton(onClick = { showLinkMapMenu = true }) {
                                                    Text("Link Existing Map", style = MaterialTheme.typography.labelSmall)
                                                }
                                                DropdownMenu(
                                                    expanded = showLinkMapMenu,
                                                    onDismissRequest = { showLinkMapMenu = false }
                                                ) {
                                                    savedMaps.forEach { map ->
                                                        DropdownMenuItem(
                                                            text = { Text("${map.name} (${map.width}×${map.height})") },
                                                            onClick = {
                                                                onLinkLocationToMap(map, current)
                                                                showLinkMapMenu = false
                                                                editingItem = null
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            autoGenerateMapOnSave = !autoGenerateMapOnSave
                                        }
                                    ) {
                                        Checkbox(
                                            checked = autoGenerateMapOnSave,
                                            onCheckedChange = { autoGenerateMapOnSave = it }
                                        )
                                        Text(
                                            text = "Auto-generate synced AI map on save",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        val built = current.copy(
                            title = title.trim().ifBlank { "Untitled" },
                            subtitle = subtitle.trim(),
                            description = description.trim(),
                            iconEmoji = emoji,
                            imageUrl = imageUrl,
                            tagsJson = JsonCodecs.encodeStringList(tags),
                        )
                        if (category == "location" && autoGenerateMapOnSave && linkedMap == null && onSaveLocationAndGenerateMap != null) {
                            onSaveLocationAndGenerateMap(built)
                        } else {
                            onSaveItem(built)
                        }
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

        if (showSingleAiEdit && onAssistSingleWorldItemAi != null) {
            val draftTags = tagsText.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
            val draftItem = current.copy(
                title = title.trim(),
                subtitle = subtitle.trim(),
                description = description.trim(),
                iconEmoji = emoji,
                imageUrl = imageUrl,
                tagsJson = JsonCodecs.encodeStringList(draftTags),
            )
            val hasDraftContent = current.id.isNotBlank() || title.isNotBlank() || description.isNotBlank()
            AiImportDialog(
                title = "AI ${category.replaceFirstChar { it.uppercase() }} Assistant",
                promptTemplate = AiPromptTemplates.worldItemPrompt(category),
                placeholder = "Describe this ${category} or paste notes to populate its fields...",
                aiState = aiState,
                userSettings = userSettings,
                aiChatState = aiChatState,
                onSelectProvider = onSelectAiProvider,
                onSelectModel = onSelectAiModel,
                onSendAiChat = onSendAiChat,
                onDismiss = { showSingleAiEdit = false },
                onImport = { raw, useRegex ->
                    onAssistSingleWorldItemAi(draftItem, raw, false, useRegex) { updated ->
                        title = updated.title
                        subtitle = updated.subtitle
                        description = updated.description
                        emoji = updated.iconEmoji.ifBlank { defaultEmoji }
                        tagsText = JsonCodecs.decodeStringList(updated.tagsJson).joinToString(", ")
                        if (category == "location" && !useRegex && linkedMap == null) {
                            autoGenerateMapOnSave = true
                        }
                        showSingleAiEdit = false
                    }
                },
                canEditExisting = hasDraftContent,
                defaultEditMode = current.id.isNotBlank(),
                showTargetPickerInEditMode = false,
                editPromptPlaceholder = "Example: Make the description more mysterious, add secret passages, and add #dungeon tag...",
                onEditExisting = { _, instruction ->
                    onAssistSingleWorldItemAi(draftItem, instruction, true, false) { updated ->
                        title = updated.title
                        subtitle = updated.subtitle
                        description = updated.description
                        emoji = updated.iconEmoji.ifBlank { defaultEmoji }
                        tagsText = JsonCodecs.decodeStringList(updated.tagsJson).joinToString(", ")
                        showSingleAiEdit = false
                    }
                },
                aiConfigActions = aiConfigActions,
            )
        }
    }

    if (showAiImport) {
        val editableTargets = remember(categoryItems) {
            categoryItems.map { item ->
                val tags = JsonCodecs.decodeStringList(item.tagsJson).take(4)
                val sub = buildString {
                    if (item.subtitle.isNotBlank()) append(item.subtitle)
                    if (tags.isNotEmpty()) {
                        if (isNotEmpty()) append(" • ")
                        append(tags.joinToString(" ") { "#$it" })
                    }
                }
                AiEditableTarget(
                    id = item.id,
                    label = "${item.iconEmoji.ifBlank { defaultEmoji }} ${item.title}",
                    subtitle = sub,
                )
            }
        }
        AiImportDialog(
            title = "AI ${category.replaceFirstChar { it.uppercase() }}s",
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
            },
            existingTargets = editableTargets,
            canEditExisting = onEditWorldItemsAi != null && editableTargets.isNotEmpty(),
            editPromptPlaceholder = "Example: Expand lore details, add #haunted tag, or rebalance damage/rarity...",
            onEditExisting = if (onEditWorldItemsAi != null) {
                { selectedIdsToEdit, instruction ->
                    val targets = categoryItems.filter { it.id in selectedIdsToEdit }
                    onEditWorldItemsAi(targets, instruction) {
                        showAiImport = false
                    }
                }
            } else null,
            aiConfigActions = aiConfigActions,
        )
    }

    if (showGuide) {
        IdeaGuideDialog(guideKey = guideKey, onDismiss = { showGuide = false })
    }

    if (fullScreenImage != null) {
        val (imgTitle, imgUrl) = fullScreenImage!!
        FullImageViewerDialog(
            title = imgTitle,
            imageUrl = imgUrl,
            fileNamePrefix = "storyweaver_${category}",
            onDismiss = { fullScreenImage = null }
        )
    }
}
