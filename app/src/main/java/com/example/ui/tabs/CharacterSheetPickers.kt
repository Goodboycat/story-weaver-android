package com.example.ui.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.data.Character
import com.example.data.EquipmentSlot
import com.example.data.EquipmentSlotItem
import com.example.data.JsonCodecs
import com.example.data.PowerEntry
import com.example.data.UserSettings
import com.example.data.WorldItem
import com.example.ui.components.AiImageGenerateDialog
import com.example.ui.components.ImagePickerUtil
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun RegisteredLocationDropdownButton(
    worldItems: List<WorldItem>,
    onSelectLocation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locations = remember(worldItems) {
        worldItems.filter { it.category == "location" }
    }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = locations.isNotEmpty(),
            modifier = Modifier.testTag("pick_registered_location_dropdown_button")
        ) {
            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (locations.isEmpty()) "No Locations" else "Locations (${locations.size})")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            locations.forEach { loc ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("${loc.iconEmoji.ifBlank { "🏰" }} ${loc.title}")
                            if (loc.subtitle.isNotBlank()) {
                                Text(
                                    text = loc.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelectLocation(loc.title)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RegisteredUnitDropdownButton(
    allCharacters: List<Character>,
    currentCharacterId: String,
    onSelectUnit: (Character) -> Unit,
    modifier: Modifier = Modifier,
) {
    val candidates = remember(allCharacters, currentCharacterId) {
        allCharacters.filter { it.id != currentCharacterId }
    }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = candidates.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("pick_registered_unit_dropdown_button")
        ) {
            Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (candidates.isEmpty()) {
                    "No Other Registered Units Yet"
                } else {
                    "Select Registered Unit (Players / NPCs / Monsters)"
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            candidates.forEach { unit ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("${unit.avatarEmoji} ${unit.name} [${unit.kind.uppercase()}]")
                            Text(
                                text = buildString {
                                    append(unit.roleTitle.ifBlank { unit.kind })
                                    if (unit.locationName.isNotBlank()) append(" • ${unit.locationName}")
                                    append(" • HP ${unit.hpMax} AC ${unit.ac}")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    onClick = {
                        onSelectUnit(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RegisteredPowerDropdownButton(
    worldItems: List<WorldItem>,
    onSelectPower: (PowerEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val registeredPowers = remember(worldItems) {
        worldItems.filter { it.category == "power" }
    }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = registeredPowers.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("pick_registered_power_dropdown_button")
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (registeredPowers.isEmpty()) {
                    "No Registered Powers Yet"
                } else {
                    "Add from Registered Powers (${registeredPowers.size})"
                },
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            registeredPowers.forEach { item ->
                val tags = JsonCodecs.decodeStringList(item.tagsJson)
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("${item.iconEmoji.ifBlank { "✨" }} ${item.title}")
                            if (item.description.isNotBlank()) {
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelectPower(
                            PowerEntry(
                                id = UUID.randomUUID().toString().take(8),
                                name = item.title,
                                cost = item.subtitle,
                                description = item.description,
                                emoji = item.iconEmoji.ifBlank { "✨" },
                                tags = tags,
                            )
                        )
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RegisteredItemDropdownButton(
    worldItems: List<WorldItem>,
    buttonLabel: String = "Add from Registered Items",
    onSelectItem: (WorldItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val registeredItems = remember(worldItems) {
        worldItems.filter { it.category == "item" }
    }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = registeredItems.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("pick_registered_item_dropdown_button")
        ) {
            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (registeredItems.isEmpty()) {
                    "No Registered Items Yet"
                } else {
                    "$buttonLabel (${registeredItems.size})"
                },
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            registeredItems.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("${item.iconEmoji.ifBlank { "⚔️" }} ${item.title}")
                            if (item.subtitle.isNotBlank() || item.description.isNotBlank()) {
                                Text(
                                    text = item.subtitle.ifBlank { item.description },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelectItem(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun EquipmentSilhouetteGrid(
    equipment: Map<String, EquipmentSlotItem>,
    onSlotClick: (EquipmentSlot) -> Unit,
) {
    val rows = listOf(
        listOf(EquipmentSlot.HEAD, EquipmentSlot.EYES),
        listOf(EquipmentSlot.NECK, EquipmentSlot.SHOULDERS, EquipmentSlot.BACK),
        listOf(EquipmentSlot.WRISTS, EquipmentSlot.CHEST, EquipmentSlot.HANDS),
        listOf(EquipmentSlot.WAIST, EquipmentSlot.WEAPON_MAIN),
        listOf(EquipmentSlot.LEGS, EquipmentSlot.FEET),
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rows.forEach { rowSlots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowSlots.forEach { slot ->
                        val equipped = equipment[slot.key]
                        val hasItem = equipped != null && (equipped.name.isNotBlank() || !equipped.imageUrl.isNullOrBlank())
                        val slotBmp = remember(equipped?.imageUrl) {
                            ImagePickerUtil.decodeDataUrlBitmap(equipped?.imageUrl)
                        }
                        val displayEmoji = equipped?.emoji?.takeIf { it.isNotBlank() } ?: slot.emoji

                        Surface(
                            color = if (hasItem) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 1.dp,
                                    color = if (hasItem) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .clickable { onSlotClick(slot) }
                                .testTag("equipment_slot_${slot.key}")
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (slotBmp != null) {
                                    Image(
                                        bitmap = slotBmp.asImageBitmap(),
                                        contentDescription = equipped?.name ?: slot.label,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Text(displayEmoji, style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    text = slot.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = equipped?.name?.ifBlank { slot.label } ?: "Empty",
                                    style = MaterialTheme.typography.bodySmall,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EquipmentSlotEditorDialog(
    slot: EquipmentSlot,
    currentItem: EquipmentSlotItem,
    canEdit: Boolean,
    characterId: String,
    userSettings: UserSettings,
    worldItems: List<WorldItem>,
    onSelectAiImageModel: (String) -> Unit,
    onGenerateAiImage: (prompt: String, maxDimension: Int, onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    onRequestPickEmoji: (onPicked: (String) -> Unit) -> Unit,
    onOpenFullImage: (title: String, imageUrl: String) -> Unit,
    onAutoSaveSlot: (EquipmentSlotItem) -> Unit,
    onApplySlot: (EquipmentSlotItem?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var itemName by remember(slot, currentItem) { mutableStateOf(currentItem.name) }
    var itemBonus by remember(slot, currentItem) { mutableStateOf(currentItem.bonus) }
    var itemNotes by remember(slot, currentItem) { mutableStateOf(currentItem.notes) }
    var itemEmoji by remember(slot, currentItem) { mutableStateOf(currentItem.emoji.ifBlank { slot.emoji }) }
    var itemImageUrl by remember(slot, currentItem) { mutableStateOf(currentItem.imageUrl) }
    var showSlotImageGen by remember(slot) { mutableStateOf(false) }

    val slotImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val b64 = ImagePickerUtil.uriToResizedBase64DataUrl(context, uri, maxDimension = 180, quality = 72)
                if (b64 != null) {
                    itemImageUrl = b64
                    val nextItem = EquipmentSlotItem(
                        name = itemName.trim().ifBlank { slot.label },
                        bonus = itemBonus.trim(),
                        notes = itemNotes.trim(),
                        emoji = itemEmoji,
                        imageUrl = b64,
                    )
                    if (characterId.isNotBlank()) {
                        onAutoSaveSlot(nextItem)
                    }
                }
            }
        }
    }

    val slotBitmap = remember(itemImageUrl) { ImagePickerUtil.decodeDataUrlBitmap(itemImageUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${itemEmoji.ifBlank { slot.emoji }} ${slot.label}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !itemImageUrl.isNullOrBlank()) {
                                onOpenFullImage(itemName.ifBlank { slot.label }, itemImageUrl!!)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (slotBitmap != null) {
                                Image(
                                    bitmap = slotBitmap.asImageBitmap(),
                                    contentDescription = itemName.ifBlank { slot.label },
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(itemEmoji.ifBlank { slot.emoji }, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }

                    if (canEdit) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedButton(
                                onClick = { showSlotImageGen = true },
                                modifier = Modifier.testTag("generate_equipment_image_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("AI Image")
                            }
                            OutlinedButton(
                                onClick = {
                                    slotImagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("upload_equipment_image_button")
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Upload")
                            }
                            OutlinedButton(
                                onClick = {
                                    onRequestPickEmoji { picked -> itemEmoji = picked }
                                },
                                modifier = Modifier.testTag("open_all_emojis_equipment_button")
                            ) {
                                Icon(Icons.Default.EmojiEmotions, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("All Emojis ($itemEmoji)")
                            }
                        }
                    }
                }

                if (itemImageUrl != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onOpenFullImage(itemName.ifBlank { slot.label }, itemImageUrl!!) }) {
                            Text("View / Save Image")
                        }
                        if (canEdit) {
                            TextButton(onClick = { itemImageUrl = null }) {
                                Text("Remove Image")
                            }
                        }
                    }
                }

                if (canEdit) {
                    RegisteredItemDropdownButton(
                        worldItems = worldItems,
                        buttonLabel = "Equip from Items Tab",
                        onSelectItem = { picked ->
                            itemName = picked.title
                            if (picked.subtitle.isNotBlank()) itemBonus = picked.subtitle
                            if (picked.description.isNotBlank()) itemNotes = picked.description
                            if (picked.iconEmoji.isNotBlank()) itemEmoji = picked.iconEmoji
                            if (!picked.imageUrl.isNullOrBlank()) itemImageUrl = picked.imageUrl
                        }
                    )
                }

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { if (canEdit) itemName = it },
                    readOnly = !canEdit,
                    label = { Text("Equipped Item") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("equipment_item_name_input")
                )
                OutlinedTextField(
                    value = itemBonus,
                    onValueChange = { if (canEdit) itemBonus = it },
                    readOnly = !canEdit,
                    label = { Text("Bonus (e.g. +1 AC)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = itemNotes,
                    onValueChange = { if (canEdit) itemNotes = it },
                    readOnly = !canEdit,
                    label = { Text("Properties / Lore") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (canEdit) {
                Button(
                    onClick = {
                        if (
                            itemName.isBlank() &&
                            itemBonus.isBlank() &&
                            itemNotes.isBlank() &&
                            itemImageUrl.isNullOrBlank() &&
                            itemEmoji == slot.emoji
                        ) {
                            onApplySlot(null)
                        } else {
                            onApplySlot(
                                EquipmentSlotItem(
                                    name = itemName.trim(),
                                    bonus = itemBonus.trim(),
                                    notes = itemNotes.trim(),
                                    emoji = itemEmoji,
                                    imageUrl = itemImageUrl,
                                )
                            )
                        }
                    },
                    modifier = Modifier.testTag("apply_equipment_slot_button")
                ) { Text("Apply") }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (canEdit) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (showSlotImageGen) {
        val defaultEquipPrompt = buildString {
            append("Detailed dark fantasy RPG equipment icon illustration for ${slot.label}: ")
            append(itemName.trim().ifBlank { slot.label })
            if (itemBonus.isNotBlank()) append(" (${itemBonus.trim()})")
            if (itemNotes.isNotBlank()) append(". ${itemNotes.trim().take(160)}")
            append(". Centered item artwork, no text, rich fantasy lighting.")
        }
        AiImageGenerateDialog(
            title = "Generate ${slot.label} Artwork",
            defaultPrompt = defaultEquipPrompt,
            userSettings = userSettings,
            onSelectImageModel = onSelectAiImageModel,
            onGenerate = { prompt, onOk, onErr ->
                onGenerateAiImage(prompt, 180, onOk, onErr)
            },
            onImageGenerated = { dataUrl ->
                itemImageUrl = dataUrl
                val effectiveName = itemName.trim().ifBlank { slot.label }
                if (itemName.isBlank()) itemName = effectiveName
                val nextItem = EquipmentSlotItem(
                    name = effectiveName,
                    bonus = itemBonus.trim(),
                    notes = itemNotes.trim(),
                    emoji = itemEmoji,
                    imageUrl = dataUrl,
                )
                if (characterId.isNotBlank()) {
                    onAutoSaveSlot(nextItem)
                }
            },
            onDismiss = { showSlotImageGen = false }
        )
    }
}
