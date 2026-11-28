package com.example.ui.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.AiChatUiState
import com.example.ai.AiPromptTemplates
import com.example.ai.AiProvider
import com.example.ai.CharacterSheetAiTab
import com.example.data.Character
import com.example.data.ChatMessage
import com.example.data.EquipmentSlotItem
import com.example.data.JsonCodecs
import com.example.data.SearchFilterUtil
import com.example.data.Story
import com.example.data.UserSettings
import com.example.data.WorldItem
import com.example.ui.components.AiEditableTarget
import com.example.ui.components.AiImportDialog
import com.example.ui.components.FullImageViewerDialog
import com.example.ui.components.IdeaGuideDialog
import com.example.ui.components.ImagePickerUtil
import com.example.viewmodel.AiUiState

@Composable
fun EntitiesTab(
    story: Story,
    kind: String, // "player" | "npc" | "monster"
    characters: List<Character>,
    worldItems: List<WorldItem> = emptyList(),
    messages: List<ChatMessage> = emptyList(),
    pinnedCharacterIds: Set<String> = emptySet(),
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
    onSaveCharacter: (Character) -> Unit,
    onDeleteCharacters: (List<String>) -> Unit,
    onImportAi: (rawText: String, useRegex: Boolean, onDone: () -> Unit) -> Unit,
    onParseEquipmentAi: (rawText: String, onParsed: (Map<String, EquipmentSlotItem>) -> Unit) -> Unit,
    onEditCharactersAi: ((targets: List<Character>, instruction: String, onDone: () -> Unit) -> Unit)? = null,
    onAssistTabAi: ((current: Character, tab: CharacterSheetAiTab, prompt: String, isEditExisting: Boolean, useRegex: Boolean, onDone: (Character) -> Unit) -> Unit)? = null,
    onResetAiState: () -> Unit,
    onJumpToLocation: (String) -> Unit = {},
    onShareCharacterToChat: ((Character) -> Unit)? = null,
    onPinCharacter: ((Character, targetUserId: String, note: String) -> Unit)? = null,
    onUnpinCharacter: ((String) -> Unit)? = null,
    aiConfigActions: com.example.ui.components.AiProviderConfigActions? = null,
) {
    val filtered = remember(characters, kind) { characters.filter { it.kind == kind } }
    var searchQuery by remember { mutableStateOf("") }
    val visibleList = remember(filtered, searchQuery) {
        filtered.filter { SearchFilterUtil.matchesCharacterSearch(it, searchQuery) }
    }
    val deletableVisible = remember(visibleList, isGmMode, currentUserId) {
        visibleList.filter { isGmMode || it.ownerId == currentUserId }
    }
    val canMultiSelect = isGmMode || filtered.any { it.ownerId == currentUserId }

    var multiSelect by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var editingCharacter by remember { mutableStateOf<Character?>(null) }
    var pinDialogTarget by remember { mutableStateOf<Character?>(null) }
    var fullScreenImage by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showAiImport by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

    val guideKey = when (kind) {
        "npc" -> "npcs"
        "monster" -> "monsters"
        else -> "players"
    }
    val canCreate = isGmMode || kind == "player"

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
                    placeholder = { Text("Search ${kind}s, #tags, powers...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("search_${kind}_input")
                )
                IconButton(
                    onClick = { showGuide = true },
                    modifier = Modifier.testTag("guide_${kind}_button")
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "Idea Guide")
                }
                if (canMultiSelect) {
                    IconButton(
                        onClick = {
                            multiSelect = !multiSelect
                            selectedIds = emptySet()
                        },
                        modifier = Modifier.testTag("multiselect_${kind}_button")
                    ) {
                        Icon(
                            Icons.Default.Checklist,
                            contentDescription = "Multi-select",
                            tint = if (multiSelect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (canCreate) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            editingCharacter = Character(
                                storyId = story.id,
                                gmUserId = story.gmUserId,
                                ownerId = currentUserId,
                                memberUids = story.memberUids,
                                kind = kind,
                                avatarEmoji = when (kind) {
                                    "monster" -> "🐺"
                                    "npc" -> "🧙"
                                    else -> "🛡️"
                                },
                                statsJson = JsonCodecs.encodeStats(JsonCodecs.defaultStats())
                            )
                        },
                        modifier = Modifier.weight(1f).testTag("create_${kind}_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New ${kind.replaceFirstChar { it.uppercase() }}")
                    }

                    OutlinedButton(
                        onClick = {
                            onResetAiState()
                            showAiImport = true
                        },
                        modifier = Modifier.weight(1f).testTag("ai_import_${kind}_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Create / Edit")
                    }
                }
            }

            if (multiSelect) {
                val visibleIds = deletableVisible.map { it.id }.toSet()
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
                        enabled = deletableVisible.isNotEmpty(),
                        modifier = Modifier.testTag("select_all_${kind}_button")
                    ) {
                        Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (allVisibleSelected) "Deselect All" else "Select All (${deletableVisible.size})")
                    }
                    if (selectedIds.isNotEmpty()) {
                        TextButton(
                            onClick = { selectedIds = emptySet() },
                            modifier = Modifier.testTag("clear_selection_${kind}_button")
                        ) {
                            Text("Clear (${selectedIds.size})")
                        }
                    }
                }
                if (selectedIds.isNotEmpty()) {
                    Button(
                        onClick = {
                            onDeleteCharacters(selectedIds.toList())
                            selectedIds = emptySet()
                            multiSelect = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("batch_delete_${kind}_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Delete Selected (${selectedIds.size})")
                    }
                }
            }
        }

        if (visibleList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (searchQuery.isNotBlank()) {
                        "No ${kind}s match \"$searchQuery\"."
                    } else {
                        "No ${kind}s yet. Create one or use AI Create / Edit!"
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
                items(visibleList, key = { it.id }) { char ->
                    val canEditThis = isGmMode || char.ownerId == currentUserId
                    val avatarBitmap = remember(char.avatarUrl) {
                        ImagePickerUtil.decodeDataUrlBitmap(char.avatarUrl)
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (multiSelect) {
                                    if (canEditThis) {
                                        selectedIds = if (char.id in selectedIds) selectedIds - char.id else selectedIds + char.id
                                    }
                                } else {
                                    editingCharacter = char
                                }
                            }
                            .testTag("character_card_${char.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (multiSelect) {
                                Checkbox(
                                    checked = char.id in selectedIds,
                                    enabled = canEditThis,
                                    onCheckedChange = { checked ->
                                        if (canEditThis) {
                                            selectedIds = if (checked) selectedIds + char.id else selectedIds - char.id
                                        }
                                    }
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = !multiSelect && !char.avatarUrl.isNullOrBlank()) {
                                        fullScreenImage = char.name to char.avatarUrl!!
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (avatarBitmap != null) {
                                        Image(
                                            bitmap = avatarBitmap.asImageBitmap(),
                                            contentDescription = char.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(char.avatarEmoji, style = MaterialTheme.typography.titleLarge)
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        char.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (char.id in pinnedCharacterIds) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                "📌 Pinned",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    buildString {
                                        append("${char.roleTitle.ifBlank { kind.uppercase() }} • Lvl ${char.level} • HP ${char.hpCurrent}/${char.hpMax} • AC ${char.ac}")
                                        if (char.locationName.isNotBlank()) append(" • 📍 ${char.locationName}")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!multiSelect) {
                                if (onShareCharacterToChat != null) {
                                    IconButton(
                                        onClick = { onShareCharacterToChat(char) },
                                        modifier = Modifier.size(34.dp).testTag("share_char_${char.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Campaign,
                                            contentDescription = "Share to Chat",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (onPinCharacter != null) {
                                    val isPinned = char.id in pinnedCharacterIds
                                    IconButton(
                                        onClick = {
                                            if (isPinned && onUnpinCharacter != null) {
                                                onUnpinCharacter(char.id)
                                            } else {
                                                pinDialogTarget = char
                                            }
                                        },
                                        modifier = Modifier.size(34.dp).testTag("pin_char_${char.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.PushPin,
                                            contentDescription = if (isPinned) "Unpin Character" else "Pin Character",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (!canEditThis) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        "View",
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

    if (editingCharacter != null) {
        val target = editingCharacter!!
        val canEdit = isGmMode || target.ownerId == currentUserId
        CharacterSheetDialog(
            initialCharacter = target,
            story = story,
            allCharacters = characters,
            worldItems = worldItems,
            canEdit = canEdit,
            isPinned = target.id in pinnedCharacterIds,
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSelectAiProvider = onSelectAiProvider,
            onSelectAiModel = onSelectAiModel,
            onSelectAiImageModel = onSelectAiImageModel,
            onGenerateAiImage = onGenerateAiImage,
            onSendAiChat = onSendAiChat,
            onParseEquipmentAi = onParseEquipmentAi,
            onAssistTabAi = onAssistTabAi,
            onResetAiState = onResetAiState,
            onJumpToLocation = { loc ->
                editingCharacter = null
                onJumpToLocation(loc)
            },
            onShareToChat = onShareCharacterToChat,
            onPinToggle = if (target.id.isNotBlank() && onPinCharacter != null) {
                {
                    if (target.id in pinnedCharacterIds && onUnpinCharacter != null) {
                        onUnpinCharacter(target.id)
                    } else {
                        pinDialogTarget = target
                    }
                }
            } else null,
            onAutoSave = { updated ->
                onSaveCharacter(updated)
                editingCharacter = updated
            },
            onSave = { updated ->
                onSaveCharacter(updated)
                editingCharacter = null
            },
            onDismiss = { editingCharacter = null },
            aiConfigActions = aiConfigActions,
        )
    }

    if (pinDialogTarget != null && onPinCharacter != null) {
        PinCharacterDialog(
            character = pinDialogTarget!!,
            partyCharacters = characters,
            messages = messages,
            onConfirmPin = { targetUid, note ->
                onPinCharacter(pinDialogTarget!!, targetUid, note)
            },
            onDismiss = { pinDialogTarget = null }
        )
    }

    if (showAiImport) {
        val editableChars = remember(filtered, isGmMode, currentUserId) {
            filtered.filter { isGmMode || it.ownerId == currentUserId }
        }
        val editableTargets = remember(editableChars) {
            editableChars.map { ch ->
                val tags = JsonCodecs.decodePowers(ch.powersJson).flatMap { it.tags }.distinct().take(4)
                val sub = buildString {
                    append("${ch.roleTitle.ifBlank { ch.kind.uppercase() }} • Lvl ${ch.level}")
                    if (ch.locationName.isNotBlank()) append(" • ${ch.locationName}")
                    if (tags.isNotEmpty()) append(" • ${tags.joinToString(" ") { "#$it" }}")
                }
                AiEditableTarget(
                    id = ch.id,
                    label = "${ch.avatarEmoji} ${ch.name}",
                    subtitle = sub,
                )
            }
        }
        AiImportDialog(
            title = "AI ${kind.replaceFirstChar { it.uppercase() }}s",
            promptTemplate = AiPromptTemplates.characterPrompt(kind),
            placeholder = "Paste character notes or stat blocks:\nGoblin Skirmisher (HP: 14, AC: 13)\nClass: Ambush Raider...",
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSelectProvider = onSelectAiProvider,
            onSelectModel = onSelectAiModel,
            onSendAiChat = onSendAiChat,
            onDismiss = { showAiImport = false },
            onImport = { raw, useRegex ->
                onImportAi(raw, useRegex) {
                    showAiImport = false
                }
            },
            existingTargets = editableTargets,
            canEditExisting = onEditCharactersAi != null && editableTargets.isNotEmpty(),
            editPromptPlaceholder = "Example: Level up selected units by 1, add Fireball power, or change location to Ironhold...",
            onEditExisting = if (onEditCharactersAi != null) {
                { selectedIdsToEdit, instruction ->
                    val targets = editableChars.filter { it.id in selectedIdsToEdit }
                    onEditCharactersAi(targets, instruction) {
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
            fileNamePrefix = "storyweaver_${kind}",
            onDismiss = { fullScreenImage = null }
        )
    }
}
