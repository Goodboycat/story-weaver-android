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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.AiChatUiState
import com.example.ai.AiPromptTemplates
import com.example.ai.AiProvider
import com.example.ai.CharacterParserHelpers
import com.example.data.Character
import com.example.data.EquipmentSlot
import com.example.data.EquipmentSlotItem
import com.example.data.InventoryEntry
import com.example.data.JsonCodecs
import com.example.data.MissionEntry
import com.example.data.PowerEntry
import com.example.data.Story
import com.example.data.UserSettings
import com.example.data.WorldItem
import com.example.ui.components.AiImageGenerateDialog
import com.example.ui.components.AiImportDialog
import com.example.ui.components.AllEmojiPickerDialog
import com.example.ui.components.FullImageViewerDialog
import com.example.ui.components.IdeaGuideDialog
import com.example.ui.components.ImagePickerUtil
import com.example.viewmodel.AiUiState
import kotlinx.coroutines.launch
import java.util.UUID

private sealed interface EmojiTarget {
    data object CharacterAvatar : EmojiTarget
    data class InventoryDraft(val onPicked: (String) -> Unit) : EmojiTarget
    data class InventoryExisting(val itemId: String) : EmojiTarget
    data class PowerDraft(val onPicked: (String) -> Unit) : EmojiTarget
    data class PowerExisting(val powerId: String) : EmojiTarget
    data class EquipmentSlotBadge(val onPicked: (String) -> Unit) : EmojiTarget
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CharacterSheetDialog(
    initialCharacter: Character,
    canEdit: Boolean,
    aiState: AiUiState,
    userSettings: UserSettings,
    aiChatState: AiChatUiState,
    onSelectAiProvider: (AiProvider) -> Unit,
    onSelectAiModel: (String) -> Unit,
    onSelectAiImageModel: (String) -> Unit,
    onGenerateAiImage: (prompt: String, maxDimension: Int, onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    onSendAiChat: (String) -> Unit,
    onParseEquipmentAi: (rawText: String, onParsed: (Map<String, EquipmentSlotItem>) -> Unit) -> Unit,
    onResetAiState: () -> Unit,
    story: Story? = null,
    allCharacters: List<Character> = emptyList(),
    worldItems: List<WorldItem> = emptyList(),
    isPinned: Boolean = false,
    onShareToChat: ((Character) -> Unit)? = null,
    onPinToggle: (() -> Unit)? = null,
    onJumpToLocation: ((String) -> Unit)? = null,
    onAutoSave: ((Character) -> Unit)? = null,
    onSave: (Character) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(initialCharacter) { mutableStateOf(initialCharacter.name) }
    var roleTitle by remember(initialCharacter) { mutableStateOf(initialCharacter.roleTitle) }
    var locationName by remember(initialCharacter) { mutableStateOf(initialCharacter.locationName) }
    var avatarEmoji by remember(initialCharacter) { mutableStateOf(initialCharacter.avatarEmoji) }
    var avatarUrl by remember(initialCharacter) { mutableStateOf(initialCharacter.avatarUrl) }
    var levelText by remember(initialCharacter) { mutableStateOf(initialCharacter.level.toString()) }
    var hpCurrentText by remember(initialCharacter) { mutableStateOf(initialCharacter.hpCurrent.toString()) }
    var hpMaxText by remember(initialCharacter) { mutableStateOf(initialCharacter.hpMax.toString()) }
    var acText by remember(initialCharacter) { mutableStateOf(initialCharacter.ac.toString()) }
    var notes by remember(initialCharacter) { mutableStateOf(initialCharacter.notes) }

    var inventory by remember(initialCharacter) { mutableStateOf(JsonCodecs.decodeInventory(initialCharacter.inventoryJson)) }
    var missions by remember(initialCharacter) { mutableStateOf(JsonCodecs.decodeMissions(initialCharacter.missionsJson)) }
    var powers by remember(initialCharacter) { mutableStateOf(JsonCodecs.decodePowers(initialCharacter.powersJson)) }
    var equipment by remember(initialCharacter) { mutableStateOf(JsonCodecs.decodeEquipment(initialCharacter.equipmentJson)) }
    var stats by remember(initialCharacter) { mutableStateOf(JsonCodecs.decodeStats(initialCharacter.statsJson)) }

    var activeTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Info", "Notes", "Inventory", "Missions", "Powers", "Gear & Stats")

    var selectedSlot by remember { mutableStateOf<EquipmentSlot?>(null) }
    var showEquipmentAi by remember { mutableStateOf(false) }
    var showEquipmentGuide by remember { mutableStateOf(false) }
    var emojiTarget by remember { mutableStateOf<EmojiTarget?>(null) }
    var showCharImageGen by remember { mutableStateOf(false) }
    var fullScreenImage by remember { mutableStateOf<Pair<String, String>?>(null) }
    var parseFeedback by remember { mutableStateOf<String?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val b64 = ImagePickerUtil.uriToResizedBase64DataUrl(context, uri, maxDimension = 320)
                if (b64 != null) avatarUrl = b64
            }
        }
    }

    fun buildUpdatedCharacter(): Character = initialCharacter.copy(
        name = name.trim().ifBlank { "Unnamed" },
        roleTitle = roleTitle.trim(),
        locationName = locationName.trim().take(120),
        avatarEmoji = avatarEmoji,
        avatarUrl = avatarUrl,
        level = levelText.toLongOrNull() ?: initialCharacter.level,
        hpCurrent = hpCurrentText.toLongOrNull() ?: initialCharacter.hpCurrent,
        hpMax = hpMaxText.toLongOrNull() ?: initialCharacter.hpMax,
        ac = acText.toLongOrNull() ?: initialCharacter.ac,
        notes = notes,
        inventoryJson = JsonCodecs.encodeInventory(inventory),
        missionsJson = JsonCodecs.encodeMissions(missions),
        powersJson = JsonCodecs.encodePowers(powers),
        equipmentJson = JsonCodecs.encodeEquipment(equipment),
        statsJson = JsonCodecs.encodeStats(stats),
    )

    fun reparseNotesIntoFields() {
        val stubStory = story ?: Story(
            id = initialCharacter.storyId,
            gmUserId = initialCharacter.gmUserId,
            memberUids = initialCharacter.memberUids,
        )
        val merged = CharacterParserHelpers.applyParsedNotesToCharacter(buildUpdatedCharacter(), stubStory)
        roleTitle = merged.roleTitle
        locationName = merged.locationName
        levelText = merged.level.toString()
        hpCurrentText = merged.hpCurrent.toString()
        hpMaxText = merged.hpMax.toString()
        acText = merged.ac.toString()
        stats = JsonCodecs.decodeStats(merged.statsJson)
        equipment = JsonCodecs.decodeEquipment(merged.equipmentJson)
        powers = JsonCodecs.decodePowers(merged.powersJson)
        inventory = JsonCodecs.decodeInventory(merged.inventoryJson)
        parseFeedback = "Parsed stats, gear slots, inventory & powers from Notes."
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
                            modifier = Modifier.testTag("close_character_sheet_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    title = {
                        Text(
                            text = name.ifBlank { "Character Sheet" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        if (onShareToChat != null && initialCharacter.id.isNotBlank()) {
                            IconButton(onClick = { onShareToChat(buildUpdatedCharacter()) }) {
                                Icon(
                                    Icons.Default.Campaign,
                                    contentDescription = "Share to Chat",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (onPinToggle != null && initialCharacter.id.isNotBlank()) {
                            IconButton(onClick = onPinToggle) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = if (isPinned) "Unpin" else "Pin Character",
                                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (canEdit) {
                            Button(
                                onClick = { onSave(buildUpdatedCharacter()) },
                                modifier = Modifier.padding(end = 8.dp).testTag("save_character_sheet_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save")
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text(
                                    "Read Only",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
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
                ScrollableTabRow(
                    selectedTabIndex = activeTab,
                    edgePadding = 12.dp
                ) {
                    subTabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = activeTab == idx,
                            onClick = { activeTab = idx },
                            text = { Text(title) },
                            modifier = Modifier.testTag("char_subtab_$idx")
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (activeTab) {
                        0 -> {
                            // Info Sub-Tab
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                val avatarBitmap = remember(avatarUrl) {
                                    ImagePickerUtil.decodeDataUrlBitmap(avatarUrl)
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (!avatarUrl.isNullOrBlank()) {
                                                fullScreenImage = name.ifBlank { "Character" } to avatarUrl!!
                                            } else if (canEdit) {
                                                avatarPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (avatarBitmap != null) {
                                            Image(
                                                bitmap = avatarBitmap.asImageBitmap(),
                                                contentDescription = name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(avatarEmoji, style = MaterialTheme.typography.headlineMedium)
                                        }
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (canEdit) {
                                            OutlinedButton(
                                                onClick = {
                                                    avatarPicker.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                modifier = Modifier.testTag("pick_avatar_image_button")
                                            ) {
                                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Upload")
                                            }
                                            OutlinedButton(
                                                onClick = { showCharImageGen = true },
                                                modifier = Modifier.testTag("generate_char_avatar_button")
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("AI Image")
                                            }
                                            OutlinedButton(
                                                onClick = { emojiTarget = EmojiTarget.CharacterAvatar },
                                                modifier = Modifier.testTag("open_all_emojis_char_button")
                                            ) {
                                                Icon(Icons.Default.EmojiEmotions, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("All Emojis ($avatarEmoji)")
                                            }
                                        }
                                        if (avatarUrl != null) {
                                            OutlinedButton(
                                                onClick = { fullScreenImage = name.ifBlank { "Character" } to avatarUrl!! },
                                                modifier = Modifier.testTag("view_char_avatar_full_button")
                                            ) {
                                                Text("View / Save")
                                            }
                                        }
                                    }
                                    if (canEdit && avatarUrl != null) {
                                        TextButton(onClick = { avatarUrl = null }) { Text("Use Emoji Instead") }
                                    }
                                    Text(
                                        "Sheet ID: ${initialCharacter.id.ifBlank { "New" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (canEdit && allCharacters.isNotEmpty()) {
                                RegisteredUnitDropdownButton(
                                    allCharacters = allCharacters,
                                    currentCharacterId = initialCharacter.id,
                                    onSelectUnit = { unit ->
                                        name = unit.name
                                        roleTitle = unit.roleTitle
                                        locationName = unit.locationName
                                        avatarEmoji = unit.avatarEmoji
                                        avatarUrl = unit.avatarUrl
                                        levelText = unit.level.toString()
                                        hpCurrentText = unit.hpCurrent.toString()
                                        hpMaxText = unit.hpMax.toString()
                                        acText = unit.ac.toString()
                                        notes = unit.notes
                                        inventory = JsonCodecs.decodeInventory(unit.inventoryJson)
                                        missions = JsonCodecs.decodeMissions(unit.missionsJson)
                                        powers = JsonCodecs.decodePowers(unit.powersJson)
                                        equipment = JsonCodecs.decodeEquipment(unit.equipmentJson)
                                        stats = JsonCodecs.decodeStats(unit.statsJson)
                                    }
                                )
                            }

                            OutlinedTextField(
                                value = name,
                                onValueChange = { if (canEdit) name = it },
                                readOnly = !canEdit,
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("char_name_input")
                            )
                            OutlinedTextField(
                                value = roleTitle,
                                onValueChange = { if (canEdit) roleTitle = it },
                                readOnly = !canEdit,
                                label = { Text("Class / Role / Species") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = locationName,
                                onValueChange = { if (canEdit) locationName = it },
                                readOnly = !canEdit,
                                label = { Text("Location / Region") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("char_location_input")
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (canEdit) {
                                    RegisteredLocationDropdownButton(
                                        worldItems = worldItems,
                                        onSelectLocation = { pickedLoc -> locationName = pickedLoc }
                                    )
                                }
                                if (onJumpToLocation != null && locationName.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            if (canEdit) onSave(buildUpdatedCharacter()) else onDismiss()
                                            onJumpToLocation(locationName.trim())
                                        },
                                        modifier = Modifier.testTag("char_jump_to_location_button")
                                    ) {
                                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Jump to Location")
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = levelText,
                                    onValueChange = { if (canEdit) levelText = it.filter { c -> c.isDigit() } },
                                    readOnly = !canEdit,
                                    label = { Text("LVL / CR") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = hpCurrentText,
                                    onValueChange = { if (canEdit) hpCurrentText = it.filter { c -> c.isDigit() || c == '-' } },
                                    readOnly = !canEdit,
                                    label = { Text("HP") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = hpMaxText,
                                    onValueChange = { if (canEdit) hpMaxText = it.filter { c -> c.isDigit() } },
                                    readOnly = !canEdit,
                                    label = { Text("Max HP") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = acText,
                                    onValueChange = { if (canEdit) acText = it.filter { c -> c.isDigit() } },
                                    readOnly = !canEdit,
                                    label = { Text("AC") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        1 -> {
                            // Notes Sub-Tab
                            if (canEdit && notes.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { reparseNotesIntoFields() },
                                    modifier = Modifier.fillMaxWidth().testTag("reparse_notes_button")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Re-parse Notes -> Stats, Gear & Powers")
                                }
                            }
                            if (parseFeedback != null) {
                                Text(
                                    text = parseFeedback!!,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { if (canEdit) notes = it },
                                readOnly = !canEdit,
                                label = { Text("Backstory, Traits & Session Notes") },
                                minLines = 10,
                                modifier = Modifier.fillMaxWidth().testTag("char_notes_input")
                            )
                        }

                        2 -> {
                            // Inventory Sub-Tab
                            var newItemName by remember { mutableStateOf("") }
                            var newItemQty by remember { mutableStateOf("1") }
                            var newItemEmoji by remember { mutableStateOf("🎒") }
                            if (canEdit) {
                                RegisteredItemDropdownButton(
                                    worldItems = worldItems,
                                    buttonLabel = "Add from Items Tab",
                                    onSelectItem = { picked ->
                                        inventory = inventory + InventoryEntry(
                                            id = UUID.randomUUID().toString().take(8),
                                            name = picked.title,
                                            quantity = 1,
                                            notes = picked.description.ifBlank { picked.subtitle },
                                            emoji = picked.iconEmoji.ifBlank { "🎒" },
                                        )
                                    }
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            emojiTarget = EmojiTarget.InventoryDraft { picked -> newItemEmoji = picked }
                                        },
                                        label = { Text(newItemEmoji) },
                                        modifier = Modifier.testTag("pick_inventory_draft_emoji_button")
                                    )
                                    OutlinedTextField(
                                        value = newItemName,
                                        onValueChange = { newItemName = it },
                                        label = { Text("Add Item") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("char_add_item_input")
                                    )
                                    OutlinedTextField(
                                        value = newItemQty,
                                        onValueChange = { newItemQty = it.filter { c -> c.isDigit() } },
                                        label = { Text("Qty") },
                                        singleLine = true,
                                        modifier = Modifier.width(64.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (newItemName.isNotBlank()) {
                                                inventory = inventory + InventoryEntry(
                                                    id = UUID.randomUUID().toString().take(8),
                                                    name = newItemName.trim(),
                                                    quantity = newItemQty.toIntOrNull() ?: 1,
                                                    emoji = newItemEmoji
                                                )
                                                newItemName = ""
                                                newItemQty = "1"
                                                newItemEmoji = "🎒"
                                            }
                                        },
                                        modifier = Modifier.testTag("char_add_item_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                    }
                                }
                            }
                            if (inventory.isEmpty()) {
                                Text("Inventory is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inventory.forEach { item ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = false,
                                            enabled = canEdit,
                                            onClick = {
                                                if (canEdit) emojiTarget = EmojiTarget.InventoryExisting(item.id)
                                            },
                                            label = { Text(item.emoji) }
                                        )
                                        Text("${item.name} ×${item.quantity}", modifier = Modifier.weight(1f))
                                        if (canEdit) {
                                            IconButton(onClick = {
                                                inventory = inventory.filterNot { it.id == item.id }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Missions Sub-Tab
                            var newMissionTitle by remember { mutableStateOf("") }
                            if (canEdit) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newMissionTitle,
                                        onValueChange = { newMissionTitle = it },
                                        label = { Text("New Quest / Objective") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(onClick = {
                                        if (newMissionTitle.isNotBlank()) {
                                            missions = missions + MissionEntry(
                                                id = UUID.randomUUID().toString().take(8),
                                                title = newMissionTitle.trim()
                                            )
                                            newMissionTitle = ""
                                        }
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                    }
                                }
                            }
                            if (missions.isEmpty()) {
                                Text("No missions tracked yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            missions.forEach { m ->
                                val completed = m.status == "completed"
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = completed,
                                            enabled = canEdit,
                                            onCheckedChange = { checked ->
                                                missions = missions.map {
                                                    if (it.id == m.id) it.copy(status = if (checked) "completed" else "active") else it
                                                }
                                            }
                                        )
                                        Text(m.title, modifier = Modifier.weight(1f))
                                        if (canEdit) {
                                            IconButton(onClick = {
                                                missions = missions.filterNot { it.id == m.id }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        4 -> {
                            // Powers Sub-Tab
                            var pName by remember { mutableStateOf("") }
                            var pCost by remember { mutableStateOf("") }
                            var pDesc by remember { mutableStateOf("") }
                            var pTagsText by remember { mutableStateOf("") }
                            var pEmoji by remember { mutableStateOf("✨") }
                            if (canEdit) {
                                RegisteredPowerDropdownButton(
                                    worldItems = worldItems,
                                    onSelectPower = { pickedPower ->
                                        powers = powers + pickedPower
                                    }
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            emojiTarget = EmojiTarget.PowerDraft { picked -> pEmoji = picked }
                                        },
                                        label = { Text(pEmoji) },
                                        modifier = Modifier.testTag("pick_power_draft_emoji_button")
                                    )
                                    OutlinedTextField(
                                        value = pName,
                                        onValueChange = { pName = it },
                                        label = { Text("Ability / Spell Name") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = pCost,
                                        onValueChange = { pCost = it },
                                        label = { Text("Cost / Cooldown") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            if (pName.isNotBlank()) {
                                                val parsedTags = pTagsText.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
                                                powers = powers + PowerEntry(
                                                    id = UUID.randomUUID().toString().take(8),
                                                    name = pName.trim(),
                                                    cost = pCost.trim(),
                                                    description = pDesc.trim(),
                                                    emoji = pEmoji,
                                                    tags = parsedTags,
                                                )
                                                pName = ""; pCost = ""; pDesc = ""; pTagsText = ""; pEmoji = "✨"
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    ) { Text("Add") }
                                }
                                OutlinedTextField(
                                    value = pDesc,
                                    onValueChange = { pDesc = it },
                                    label = { Text("Effect Description") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = pTagsText,
                                    onValueChange = { pTagsText = it },
                                    label = { Text("Tags (comma separated)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (powers.isEmpty()) {
                                Text("No powers added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            powers.forEach { p ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = false,
                                            enabled = canEdit,
                                            onClick = {
                                                if (canEdit) emojiTarget = EmojiTarget.PowerExisting(p.id)
                                            },
                                            label = { Text(p.emoji) }
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("${p.name}" + if (p.cost.isNotBlank()) " (${p.cost})" else "", style = MaterialTheme.typography.titleMedium)
                                            if (p.description.isNotBlank()) {
                                                Text(p.description, style = MaterialTheme.typography.bodySmall)
                                            }
                                            if (p.tags.isNotEmpty()) {
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    p.tags.forEach { tag ->
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.surface,
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
                                        if (canEdit) {
                                            IconButton(onClick = { powers = powers.filterNot { it.id == p.id } }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        5 -> {
                            // Equipment 12-Slot Body Silhouette & Freeform Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("12-Slot Equipment Silhouette", style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { showEquipmentGuide = true }) {
                                        Icon(Icons.Default.Lightbulb, contentDescription = "Equipment Guide")
                                    }
                                    if (canEdit && notes.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = { reparseNotesIntoFields() },
                                            modifier = Modifier.testTag("gear_parse_notes_button")
                                        ) {
                                            Text("From Notes")
                                        }
                                    }
                                    if (canEdit) {
                                        OutlinedButton(
                                            onClick = {
                                                onResetAiState()
                                                showEquipmentAi = true
                                            },
                                            modifier = Modifier.testTag("equipment_ai_parse_button")
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("AI Loadout")
                                        }
                                    }
                                }
                            }
                            if (parseFeedback != null) {
                                Text(
                                    text = parseFeedback!!,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            EquipmentSilhouetteGrid(
                                equipment = equipment,
                                onSlotClick = { slot -> selectedSlot = slot }
                            )

                            Spacer(Modifier.height(6.dp))
                            Text("Freeform Stats", style = MaterialTheme.typography.titleMedium)

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                stats.forEach { (key, value) ->
                                    OutlinedTextField(
                                        value = value,
                                        onValueChange = { v ->
                                            if (canEdit) stats = stats.toMutableMap().apply { put(key, v) }
                                        },
                                        readOnly = !canEdit,
                                        label = { Text(key) },
                                        singleLine = true,
                                        modifier = Modifier.width(98.dp)
                                    )
                                }
                            }

                            if (canEdit) {
                                var newStatKey by remember { mutableStateOf("") }
                                var newStatVal by remember { mutableStateOf("") }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newStatKey,
                                        onValueChange = { newStatKey = it.uppercase().take(12) },
                                        label = { Text("Stat Name") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = newStatVal,
                                        onValueChange = { newStatVal = it.take(12) },
                                        label = { Text("Value") },
                                        singleLine = true,
                                        modifier = Modifier.width(84.dp)
                                    )
                                    Button(onClick = {
                                        if (newStatKey.isNotBlank()) {
                                            stats = stats.toMutableMap().apply { put(newStatKey.trim(), newStatVal.trim()) }
                                            newStatKey = ""; newStatVal = ""
                                        }
                                    }) { Text("+") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedSlot != null) {
        val slot = selectedSlot!!
        EquipmentSlotEditorDialog(
            slot = slot,
            currentItem = equipment[slot.key] ?: EquipmentSlotItem(),
            canEdit = canEdit,
            characterId = initialCharacter.id,
            userSettings = userSettings,
            worldItems = worldItems,
            onSelectAiImageModel = onSelectAiImageModel,
            onGenerateAiImage = onGenerateAiImage,
            onRequestPickEmoji = { onPicked ->
                emojiTarget = EmojiTarget.EquipmentSlotBadge(onPicked)
            },
            onOpenFullImage = { title, url ->
                fullScreenImage = title to url
            },
            onAutoSaveSlot = { nextItem ->
                equipment = equipment.toMutableMap().apply { put(slot.key, nextItem) }
                onAutoSave?.invoke(buildUpdatedCharacter())
            },
            onApplySlot = { nextItem ->
                equipment = equipment.toMutableMap().apply {
                    if (nextItem == null) remove(slot.key) else put(slot.key, nextItem)
                }
                selectedSlot = null
            },
            onDismiss = { selectedSlot = null }
        )
    }

    if (showCharImageGen) {
        val defaultCharPrompt = buildString {
            append("Fantasy tabletop RPG character portrait of ")
            append(name.trim().ifBlank { "a heroic adventurer" })
            if (roleTitle.isNotBlank()) append(", ${roleTitle.trim()}")
            if (notes.isNotBlank()) append(". ${notes.trim().take(160)}")
            append(". Detailed character art, centered portrait, dramatic fantasy lighting, no text.")
        }
        AiImageGenerateDialog(
            title = "Generate Character Portrait",
            defaultPrompt = defaultCharPrompt,
            userSettings = userSettings,
            onSelectImageModel = onSelectAiImageModel,
            onGenerate = { prompt, onOk, onErr ->
                onGenerateAiImage(prompt, 320, onOk, onErr)
            },
            onImageGenerated = { dataUrl ->
                avatarUrl = dataUrl
                if (initialCharacter.id.isNotBlank()) {
                    onAutoSave?.invoke(buildUpdatedCharacter())
                }
            },
            onDismiss = { showCharImageGen = false }
        )
    }

    if (emojiTarget != null) {
        val target = emojiTarget!!
        AllEmojiPickerDialog(
            currentEmoji = avatarEmoji,
            onSelect = { picked ->
                when (target) {
                    EmojiTarget.CharacterAvatar -> {
                        avatarEmoji = picked
                        avatarUrl = null
                    }
                    is EmojiTarget.InventoryDraft -> target.onPicked(picked)
                    is EmojiTarget.InventoryExisting -> {
                        inventory = inventory.map {
                            if (it.id == target.itemId) it.copy(emoji = picked) else it
                        }
                    }
                    is EmojiTarget.PowerDraft -> target.onPicked(picked)
                    is EmojiTarget.PowerExisting -> {
                        powers = powers.map {
                            if (it.id == target.powerId) it.copy(emoji = picked) else it
                        }
                    }
                    is EmojiTarget.EquipmentSlotBadge -> target.onPicked(picked)
                }
            },
            onDismiss = { emojiTarget = null }
        )
    }

    if (showEquipmentAi) {
        AiImportDialog(
            title = "AI Equipment Loadout Parser",
            promptTemplate = AiPromptTemplates.equipmentPrompt(),
            placeholder = "Example: Wears mithril chainmail (+2 AC), a shadow cloak for stealth, brass spyglass goggles, and wields a flaming longsword...",
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSelectProvider = onSelectAiProvider,
            onSelectModel = onSelectAiModel,
            onSendAiChat = onSendAiChat,
            onDismiss = { showEquipmentAi = false },
            onImport = { rawText, _ ->
                onParseEquipmentAi(rawText) { parsedSlots ->
                    equipment = equipment.toMutableMap().apply { putAll(parsedSlots) }
                    showEquipmentAi = false
                }
            }
        )
    }

    if (showEquipmentGuide) {
        IdeaGuideDialog(guideKey = "equipment", onDismiss = { showEquipmentGuide = false })
    }

    if (fullScreenImage != null) {
        val (imgTitle, imgUrl) = fullScreenImage!!
        FullImageViewerDialog(
            title = imgTitle,
            imageUrl = imgUrl,
            fileNamePrefix = "storyweaver_character",
            onDismiss = { fullScreenImage = null }
        )
    }
}
