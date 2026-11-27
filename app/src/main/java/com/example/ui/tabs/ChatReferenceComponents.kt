package com.example.ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Character
import com.example.data.ChatEntityRef
import com.example.data.ChatMessage
import com.example.data.JsonCodecs
import com.example.data.WorldItem

fun characterToEntityRef(character: Character): ChatEntityRef {
    val powers = JsonCodecs.decodePowers(character.powersJson)
    val tags = buildList {
        if (character.locationName.isNotBlank()) add(character.locationName)
        powers.take(4).forEach { add(it.name) }
    }
    return ChatEntityRef(
        refType = character.kind,
        refId = character.id,
        title = character.name,
        subtitle = buildString {
            append(character.roleTitle.ifBlank { character.kind.uppercase() })
            append(" • Lvl ${character.level} • HP ${character.hpCurrent}/${character.hpMax} • AC ${character.ac}")
            if (character.locationName.isNotBlank()) append(" • 📍 ${character.locationName}")
        },
        emoji = character.avatarEmoji.ifBlank {
            when (character.kind) {
                "monster" -> "👹"
                "npc" -> "🧙"
                else -> "🛡️"
            }
        },
        description = character.notes.trim().take(1500),
        tags = tags,
        ownerId = character.ownerId,
    )
}

fun worldItemToEntityRef(item: WorldItem): ChatEntityRef {
    return ChatEntityRef(
        refType = item.category,
        refId = item.id,
        title = item.title,
        subtitle = item.subtitle,
        emoji = item.iconEmoji.ifBlank {
            when (item.category) {
                "location" -> "🏰"
                "power" -> "✨"
                "item" -> "⚔️"
                else -> "📜"
            }
        },
        description = item.description.trim().take(1800),
        tags = JsonCodecs.decodeStringList(item.tagsJson),
        ownerId = item.ownerId,
    )
}

fun buildEntityReferenceCatalog(
    characters: List<Character>,
    worldItems: List<WorldItem>,
): List<ChatEntityRef> {
    val out = mutableListOf<ChatEntityRef>()
    val seen = mutableSetOf<Pair<String, String>>()

    worldItems.forEach { item ->
        val key = item.category to item.title.trim().lowercase()
        if (seen.add(key)) {
            out += worldItemToEntityRef(item)
        }
    }

    // Include any powers defined directly inside character sheets too
    characters.forEach { ch ->
        JsonCodecs.decodePowers(ch.powersJson).forEach { pw ->
            val key = "power" to pw.name.trim().lowercase()
            if (pw.name.isNotBlank() && seen.add(key)) {
                out += ChatEntityRef(
                    refType = "power",
                    refId = "${ch.id}:${pw.id}",
                    title = pw.name,
                    subtitle = pw.cost.ifBlank { "${ch.name}'s Power" },
                    emoji = pw.emoji.ifBlank { "✨" },
                    description = pw.description,
                    tags = pw.tags.ifEmpty { listOf(ch.name) },
                    ownerId = ch.ownerId,
                )
            }
        }
    }

    characters.forEach { ch ->
        val key = ch.kind to "${ch.id}_${ch.name.trim().lowercase()}"
        if (seen.add(key)) {
            out += characterToEntityRef(ch)
        }
    }

    return out
}

fun ChatMessage.toEntityRefOrNull(): ChatEntityRef? {
    if (refTitle.isBlank()) return null
    return ChatEntityRef(
        refType = refType.ifBlank { "note" },
        refId = refId,
        title = refTitle,
        subtitle = refSubtitle,
        emoji = refEmoji.ifBlank { refDefaultEmoji(refType) },
        description = refDescription,
        tags = JsonCodecs.decodeStringList(refTagsJson),
        ownerId = targetUserId,
    )
}

fun refDefaultEmoji(type: String): String = when (type) {
    "power" -> "⚡"
    "location" -> "📍"
    "item" -> "⚔️"
    "note" -> "📜"
    "player" -> "🛡️"
    "npc" -> "🧙"
    "monster" -> "👹"
    else -> "📚"
}

fun refCategoryLabel(type: String): String = when (type) {
    "power" -> "POWER"
    "location" -> "LOCATION"
    "item" -> "ITEM"
    "note" -> "NOTE"
    "player" -> "PLAYER"
    "npc" -> "NPC"
    "monster" -> "MONSTER"
    else -> "REF"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReferencedEntityCard(
    ref: ChatEntityRef,
    isPinned: Boolean = false,
    isTargetedToCurrentUser: Boolean = false,
    onClick: () -> Unit = {},
) {
    val containerColor = when {
        isTargetedToCurrentUser -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        isPinned -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = "${refDefaultEmoji(ref.refType)} ${refCategoryLabel(ref.refType)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (isPinned) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "📌 Pinned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (isTargetedToCurrentUser) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "Your Character",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = ref.emoji.ifBlank { refDefaultEmoji(ref.refType) },
                    style = MaterialTheme.typography.titleMedium
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ref.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (ref.subtitle.isNotBlank()) {
                        Text(
                            text = ref.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (ref.description.isNotBlank()) {
                Text(
                    text = ref.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (ref.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    ref.tags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReferencePickerDialog(
    catalog: List<ChatEntityRef>,
    onAttach: (ChatEntityRef) -> Unit,
    onSendImmediately: (ChatEntityRef, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    val categories = listOf(
        "all" to "All",
        "power" to "Powers",
        "location" to "Locations",
        "item" to "Items",
        "note" to "Notes",
        "player" to "Players",
        "npc" to "NPCs",
        "monster" to "Monsters",
    )

    val filtered = remember(catalog, searchQuery, selectedCategory) {
        catalog.filter { item ->
            val catOk = selectedCategory == "all" || item.refType == selectedCategory
            val q = searchQuery.trim()
            val qOk = q.isBlank() ||
                item.title.contains(q, ignoreCase = true) ||
                item.subtitle.contains(q, ignoreCase = true) ||
                item.description.contains(q, ignoreCase = true) ||
                item.tags.any { it.contains(q, ignoreCase = true) }
            catOk && qOk
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reference Campaign Entry") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search power, location, item, NPC...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("chat_ref_search_input")
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedCategory == key,
                            onClick = { selectedCategory = key },
                            label = { Text(label, maxLines = 1, softWrap = false) },
                            modifier = Modifier.testTag("chat_ref_filter_$key")
                        )
                    }
                }
                if (filtered.isEmpty()) {
                    Text(
                        "No entries found for this filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        items(filtered, key = { "${it.refType}_${it.refId}_${it.title}" }) { entry ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAttach(entry)
                                        onDismiss()
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(entry.emoji, style = MaterialTheme.typography.titleMedium)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = buildString {
                                                    append(refCategoryLabel(entry.refType))
                                                    if (entry.subtitle.isNotBlank()) append(" • ${entry.subtitle}")
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                onSendImmediately(entry, false)
                                                onDismiss()
                                            }
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Share to Chat",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (entry.refType in listOf("player", "npc", "monster")) {
                                            IconButton(
                                                onClick = {
                                                    onSendImmediately(entry, true)
                                                    onDismiss()
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.PushPin,
                                                    contentDescription = "Pin Character",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                    if (entry.description.isNotBlank()) {
                                        Text(
                                            entry.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (entry.tags.isNotEmpty()) {
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            entry.tags.take(5).forEach { tag ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = MaterialTheme.shapes.extraSmall
                                                ) {
                                                    Text(
                                                        "#$tag",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun PinCharacterDialog(
    character: Character,
    partyCharacters: List<Character>,
    messages: List<ChatMessage>,
    onConfirmPin: (targetUserId: String, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Collect distinct known party user IDs -> friendly label from player characters & recent senders
    val recipientOptions = remember(character, partyCharacters, messages) {
        val labelByUid = linkedMapOf<String, String>()
        if (character.ownerId.isNotBlank()) {
            val matchingChars = partyCharacters
                .filter { it.ownerId == character.ownerId && it.kind == "player" }
                .joinToString(", ") { it.name }
            val senderLabel = messages.lastOrNull { it.senderId == character.ownerId }?.senderName
            val suffix = matchingChars.ifBlank { senderLabel ?: "Owner" }
            labelByUid[character.ownerId] = "Character Owner ($suffix)"
        }
        partyCharacters.forEach { ch ->
            if (ch.ownerId.isNotBlank() && !labelByUid.containsKey(ch.ownerId)) {
                labelByUid[ch.ownerId] = "${ch.avatarEmoji} ${ch.name}'s Player"
            }
        }
        messages.forEach { m ->
            if (m.senderId.isNotBlank() && !labelByUid.containsKey(m.senderId)) {
                labelByUid[m.senderId] = "${m.senderName} (${m.senderRole})"
            }
        }
        labelByUid.entries.map { it.key to it.value }
    }

    var selectedTargetUid by remember(character) { mutableStateOf(character.ownerId) }
    var note by remember(character) { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val selectedLabel = recipientOptions.firstOrNull { it.first == selectedTargetUid }?.second
        ?: if (selectedTargetUid.isBlank()) "All Party Members" else selectedTargetUid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pin Character to Chat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(character.avatarEmoji.ifBlank { "🛡️" }, style = MaterialTheme.typography.headlineSmall)
                    Column {
                        Text(character.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${character.roleTitle.ifBlank { character.kind.uppercase() }} • Lvl ${character.level} • HP ${character.hpCurrent}/${character.hpMax}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (recipientOptions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Notify Player:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(selectedLabel, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                recipientOptions.forEach { (uid, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedTargetUid = uid
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note for player (optional)") },
                    placeholder = { Text("e.g. You're spotlighted! Roll stealth.") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("pin_character_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmPin(selectedTargetUid, note.trim())
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_pin_character_button")
            ) {
                Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Pin & Notify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PinnedBoardDialog(
    pinnedMessages: List<ChatMessage>,
    characters: List<Character>,
    currentUserId: String,
    onJumpToMessage: (ChatMessage) -> Unit,
    onUnpinMessage: (ChatMessage) -> Unit,
    onOpenCharacter: (Character) -> Unit,
    onCopyMessage: (ChatMessage) -> Unit,
    onDismiss: () -> Unit,
) {
    val sorted = remember(pinnedMessages, currentUserId) {
        pinnedMessages.sortedWith(
            compareByDescending<ChatMessage> { it.targetUserId == currentUserId }
                .thenByDescending { it.targetCharacterId.isNotBlank() }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📌 Pinned Board (${pinnedMessages.size})") },
        text = {
            if (sorted.isEmpty()) {
                Text(
                    "Nothing is pinned yet. Long-press or tap any message, or pin a Character sheet to highlight it for its player!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    items(sorted, key = { it.id }) { msg ->
                        val isForMe = msg.targetUserId == currentUserId
                        val linkedChar = characters.firstOrNull {
                            it.id == msg.targetCharacterId || (msg.refType in listOf("player", "npc", "monster") && it.id == msg.refId)
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isForMe) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isForMe && msg.targetCharacterName.isNotBlank()) {
                                            "📌 Pinned for you • ${msg.targetCharacterName}"
                                        } else if (msg.targetCharacterName.isNotBlank()) {
                                            "📌 Character: ${msg.targetCharacterName}"
                                        } else {
                                            "📌 From ${msg.senderName}"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { onUnpinMessage(msg) }) {
                                        Text("Unpin")
                                    }
                                }
                                if (linkedChar != null) {
                                    Text(
                                        text = "Live Stats: Lvl ${linkedChar.level} • HP ${linkedChar.hpCurrent}/${linkedChar.hpMax} • AC ${linkedChar.ac}" +
                                            if (linkedChar.locationName.isNotBlank()) " • 📍 ${linkedChar.locationName}" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                if (msg.body.isNotBlank()) {
                                    Text(msg.body, style = MaterialTheme.typography.bodySmall)
                                }
                                val ref = msg.toEntityRefOrNull()
                                if (ref != null) {
                                    ReferencedEntityCard(
                                        ref = ref,
                                        isPinned = true,
                                        isTargetedToCurrentUser = isForMe,
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onJumpToMessage(msg)
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Jump", maxLines = 1)
                                    }
                                    if (linkedChar != null) {
                                        OutlinedButton(
                                            onClick = {
                                                onDismiss()
                                                onOpenCharacter(linkedChar)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Open Sheet", maxLines = 1)
                                        }
                                    } else {
                                        IconButton(onClick = { onCopyMessage(msg) }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntityDetailSheetDialog(
    ref: ChatEntityRef,
    canJumpToTab: Boolean,
    onCopyFormatted: () -> Unit,
    onJumpToTab: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(ref.emoji.ifBlank { refDefaultEmoji(ref.refType) }, style = MaterialTheme.typography.headlineSmall)
                Column {
                    Text(ref.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        refCategoryLabel(ref.refType),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (ref.subtitle.isNotBlank()) {
                    Text(
                        ref.subtitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (ref.description.isNotBlank()) {
                    Text(ref.description, style = MaterialTheme.typography.bodyMedium)
                }
                if (ref.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ref.tags.forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    "#$tag",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = {
                    onCopyFormatted()
                    onDismiss()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
                if (canJumpToTab) {
                    Button(onClick = {
                        onDismiss()
                        onJumpToTab()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Open Tab")
                    }
                } else {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        },
        dismissButton = {
            if (canJumpToTab) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}
