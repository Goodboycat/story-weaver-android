package com.example.ui.tabs

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AppStrings
import com.example.data.Character
import com.example.data.ChatEntityRef
import com.example.data.ChatMessage
import com.example.data.JsonCodecs
import com.example.data.Story
import com.example.data.WorldItem
import com.example.ui.components.IdeaGuideDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val QUICK_REACTION_EMOJIS = listOf("👍", "❤️", "🔥", "⚔️", "🎲", "😂")

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChatTab(
    story: Story,
    strings: AppStrings,
    messages: List<ChatMessage>,
    characters: List<Character> = emptyList(),
    worldItems: List<WorldItem> = emptyList(),
    currentUserId: String,
    userDisplayName: String = "",
    isGmMode: Boolean = false,
    activeAiModelLabel: String,
    focusMessageId: String? = null,
    onConsumeFocusMessage: () -> Unit = {},
    onOpenAiChat: () -> Unit,
    onSendMessage: (body: String, speakerName: String?, replyTo: ChatMessage?, entityRef: ChatEntityRef?, pinned: Boolean) -> Unit,
    onTogglePinMessage: (ChatMessage) -> Unit = {},
    onToggleReaction: (ChatMessage, String) -> Unit = { _, _ -> },
    onDeleteMessage: (String) -> Unit = {},
    onPinCharacter: (Character, targetUserId: String, note: String) -> Unit = { _, _, _ -> },
    onSlashRoll: (rawCommand: String, rollerName: String) -> Boolean = { _, _ -> false },
    onJumpToEntityTab: (refType: String, query: String) -> Unit = { _, _ -> },
    onNotify: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var showGuide by remember { mutableStateOf(false) }
    var voiceCardExpanded by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("all") } // "all" | "pinned" | "ref" | "roll"

    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    var attachedRef by remember { mutableStateOf<ChatEntityRef?>(null) }
    var selectedSpeaker by remember { mutableStateOf<Character?>(null) }

    var showRefPicker by remember { mutableStateOf(false) }
    var showPinnedBoard by remember { mutableStateOf(false) }
    var showPinCharPicker by remember { mutableStateOf(false) }
    var pinDialogTarget by remember { mutableStateOf<Character?>(null) }
    var inspectingRef by remember { mutableStateOf<ChatEntityRef?>(null) }
    var inspectingChar by remember { mutableStateOf<Character?>(null) }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }

    val catalog = remember(characters, worldItems) {
        buildEntityReferenceCatalog(characters, worldItems)
    }
    val pinnedMessages = remember(messages) { messages.filter { it.pinned } }
    val myTargetedPins = remember(pinnedMessages, currentUserId) {
        pinnedMessages.filter { it.targetUserId == currentUserId && it.targetCharacterName.isNotBlank() }
    }

    val visibleMessages = remember(messages, activeFilter, searchQuery) {
        messages.filter { msg ->
            val filterOk = when (activeFilter) {
                "pinned" -> msg.pinned
                "ref" -> msg.refTitle.isNotBlank() || msg.messageType == "ref"
                "roll" -> msg.messageType == "roll"
                else -> true
            }
            val q = searchQuery.trim()
            val qOk = q.isBlank() ||
                msg.body.contains(q, ignoreCase = true) ||
                msg.senderName.contains(q, ignoreCase = true) ||
                msg.refTitle.contains(q, ignoreCase = true) ||
                msg.refDescription.contains(q, ignoreCase = true) ||
                msg.refTagsJson.contains(q, ignoreCase = true) ||
                msg.targetCharacterName.contains(q, ignoreCase = true)
            filterOk && qOk
        }
    }

    val isOnline = remember(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    LaunchedEffect(visibleMessages.size, activeFilter, searchQuery) {
        if (visibleMessages.isNotEmpty() && searchQuery.isBlank() && activeFilter == "all" && focusMessageId == null) {
            listState.animateScrollToItem(visibleMessages.lastIndex)
        }
    }

    LaunchedEffect(focusMessageId, visibleMessages) {
        if (!focusMessageId.isNullOrBlank()) {
            activeFilter = "all"
            searchQuery = ""
            val idx = visibleMessages.indexOfFirst { it.id == focusMessageId }
            if (idx >= 0) {
                listState.animateScrollToItem(idx)
                highlightedMessageId = focusMessageId
                delay(1800L)
                highlightedMessageId = null
            }
            onConsumeFocusMessage()
        }
    }

    fun copyMessageText(msg: ChatMessage) {
        val formatted = buildString {
            append(msg.body)
            if (msg.refTitle.isNotBlank() && !msg.body.contains(msg.refTitle)) {
                append("\n[${refCategoryLabel(msg.refType)}] ${msg.refEmoji} ${msg.refTitle}")
                if (msg.refSubtitle.isNotBlank()) append(" — ${msg.refSubtitle}")
                if (msg.refDescription.isNotBlank()) append("\n${msg.refDescription}")
                val tags = JsonCodecs.decodeStringList(msg.refTagsJson)
                if (tags.isNotEmpty()) append("\nTags: ${tags.joinToString(", ") { "#$it" }}")
            }
        }
        clipboard.setText(AnnotatedString(formatted))
        onNotify("Copied message to clipboard")
    }

    fun jumpToMessage(targetId: String) {
        activeFilter = "all"
        searchQuery = ""
        scope.launch {
            val idx = messages.indexOfFirst { it.id == targetId }
            if (idx >= 0) {
                listState.animateScrollToItem(idx)
                highlightedMessageId = targetId
                delay(1800L)
                highlightedMessageId = null
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!isOnline) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Offline — messages will sync once reconnected", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Compact Voice + AI Bar (collapsible for more chat space)
        val roomSlug = "aistudio_rpg_${story.id}"
        val roomUrl = "https://meet.jit.si/$roomSlug#config.startWithVideoMuted=true&config.startAudioOnly=true"

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { voiceCardExpanded = !voiceCardExpanded }
                    ) {
                        Icon(
                            if (voiceCardExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Room Tools",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${strings.voiceRoom} & Tools",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenAiChat,
                            modifier = Modifier.size(36.dp).testTag("open_ai_chat_button")
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI Chat ($activeAiModelLabel)",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { showSearchBar = !showSearchBar },
                            modifier = Modifier.size(36.dp).testTag("toggle_chat_search_button")
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search Chat",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { showGuide = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = strings.ideaGuide,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = voiceCardExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(roomUrl))
                                    runCatching { context.startActivity(intent) }
                                },
                                modifier = Modifier.weight(1f).testTag("open_voice_room_button")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Join Audio", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    onSendMessage(
                                        "🎙️ Voice Room is active! Tap Join Audio above to talk with the party.",
                                        selectedSpeaker?.name,
                                        null,
                                        null,
                                        false
                                    )
                                },
                                modifier = Modifier.testTag("ping_voice_room_button")
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ping", maxLines = 1)
                            }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(roomUrl))
                                    onNotify("Copied voice room link")
                                },
                                modifier = Modifier.testTag("copy_voice_room_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Voice Room Link")
                            }
                        }
                        OutlinedButton(
                            onClick = onOpenAiChat,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "AI Assistant Chat ($activeAiModelLabel)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Filter Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "all" to "All (${messages.size})",
                        "pinned" to "📌 Pinned (${pinnedMessages.size})",
                        "ref" to "📚 Refs",
                        "roll" to "🎲 Rolls",
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = activeFilter == key,
                            onClick = { activeFilter = key },
                            label = { Text(label, maxLines = 1, softWrap = false) },
                            modifier = Modifier.testTag("chat_filter_$key")
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = showSearchBar) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search messages, powers, locations, characters...") },
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
                    .testTag("chat_search_input")
            )
        }

        // Persistent Discord-Style Pinned Banner
        if (pinnedMessages.isNotEmpty()) {
            Surface(
                color = if (myTargetedPins.isNotEmpty()) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { showPinnedBoard = true }
                    .testTag("chat_pinned_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (myTargetedPins.isNotEmpty()) {
                            "📌 Pinned for You: ${myTargetedPins.first().targetCharacterName} (+${pinnedMessages.size} total)"
                        } else {
                            val latest = pinnedMessages.last()
                            "📌 ${pinnedMessages.size} Pinned • ${latest.targetCharacterName.ifBlank { latest.refTitle.ifBlank { latest.body } }}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (visibleMessages.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (messages.isEmpty()) {
                        "Party chat is ready. Message the party, @reference a Power/Item/Location, or roll /r 1d20!"
                    } else {
                        "No messages match this filter."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleMessages, key = { it.id }) { msg ->
                    ChatMessageItem(
                        msg = msg,
                        currentUserId = currentUserId,
                        isGmMode = isGmMode,
                        isHighlighted = highlightedMessageId == msg.id,
                        timeFormatter = timeFormatter,
                        onReply = { replyingTo = msg },
                        onCopy = { copyMessageText(msg) },
                        onTogglePin = { onTogglePinMessage(msg) },
                        onToggleReaction = { emoji -> onToggleReaction(msg, emoji) },
                        onDelete = { onDeleteMessage(msg.id) },
                        onJumpToReply = { replyId -> jumpToMessage(replyId) },
                        onInspectRef = { ref -> inspectingRef = ref },
                    )
                }
            }
        }

        ChatComposer(
            sendDescription = strings.sendMessage,
            userDisplayName = userDisplayName,
            currentUserId = currentUserId,
            characters = characters,
            catalog = catalog,
            replyingTo = replyingTo,
            attachedRef = attachedRef,
            selectedSpeaker = selectedSpeaker,
            onClearReply = { replyingTo = null },
            onClearRef = { attachedRef = null },
            onSelectSpeaker = { selectedSpeaker = it },
            onSelectAttachedRef = { attachedRef = it },
            onOpenRefPicker = { showRefPicker = true },
            onOpenPinCharacter = { showPinCharPicker = true },
            onSendMessage = { text ->
                val speakerLabel = selectedSpeaker?.let { "${it.avatarEmoji} ${it.name}" }
                if (text.startsWith("/r ", ignoreCase = true) || text.startsWith("/roll ", ignoreCase = true)) {
                    val roller = speakerLabel ?: userDisplayName.ifBlank { "Player" }
                    if (onSlashRoll(text, roller)) {
                        return@ChatComposer
                    }
                }
                onSendMessage(text, speakerLabel, replyingTo, attachedRef, false)
                replyingTo = null
                attachedRef = null
            }
        )
    }

    if (showGuide) {
        IdeaGuideDialog(guideKey = "chat", onDismiss = { showGuide = false })
    }

    if (showRefPicker) {
        ReferencePickerDialog(
            catalog = catalog,
            onAttach = { attachedRef = it },
            onSendImmediately = { ref, pin ->
                if (pin && ref.refType in listOf("player", "npc", "monster")) {
                    val ch = characters.firstOrNull { it.id == ref.refId }
                    if (ch != null) {
                        pinDialogTarget = ch
                    } else {
                        onSendMessage("", selectedSpeaker?.name, replyingTo, ref, true)
                    }
                } else {
                    val speakerLabel = selectedSpeaker?.let { "${it.avatarEmoji} ${it.name}" }
                    onSendMessage(
                        "${ref.emoji.ifBlank { "📚" }} Shared ${refCategoryLabel(ref.refType)}: ${ref.title}",
                        speakerLabel,
                        replyingTo,
                        ref,
                        pin
                    )
                    replyingTo = null
                }
            },
            onDismiss = { showRefPicker = false }
        )
    }

    if (showPinnedBoard) {
        PinnedBoardDialog(
            pinnedMessages = pinnedMessages,
            characters = characters,
            currentUserId = currentUserId,
            onJumpToMessage = { jumpToMessage(it.id) },
            onUnpinMessage = { onTogglePinMessage(it) },
            onOpenCharacter = { inspectingChar = it },
            onCopyMessage = { copyMessageText(it) },
            onDismiss = { showPinnedBoard = false }
        )
    }

    if (showPinCharPicker) {
        AlertDialog(
            onDismissRequest = { showPinCharPicker = false },
            title = { Text("Select Character to Pin") },
            text = {
                if (characters.isEmpty()) {
                    Text("No characters in this campaign yet.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)
                    ) {
                        items(characters, key = { it.id }) { ch ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showPinCharPicker = false
                                        pinDialogTarget = ch
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(ch.avatarEmoji.ifBlank { "🛡️" }, style = MaterialTheme.typography.titleMedium)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ch.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${ch.kind.uppercase()} • ${ch.roleTitle.ifBlank { "Sheet" }} • HP ${ch.hpCurrent}/${ch.hpMax}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPinCharPicker = false }) { Text("Close") }
            }
        )
    }

    if (pinDialogTarget != null) {
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

    if (inspectingRef != null) {
        val ref = inspectingRef!!
        val canJump = isGmMode || ref.refType == "player"
        EntityDetailSheetDialog(
            ref = ref,
            canJumpToTab = canJump,
            onCopyFormatted = {
                val formatted = buildString {
                    append("${ref.emoji} ${ref.title}")
                    if (ref.subtitle.isNotBlank()) append("\n${ref.subtitle}")
                    if (ref.description.isNotBlank()) append("\n${ref.description}")
                    if (ref.tags.isNotEmpty()) append("\nTags: ${ref.tags.joinToString(", ") { "#$it" }}")
                }
                clipboard.setText(AnnotatedString(formatted))
                onNotify("Copied ${ref.title} details")
            },
            onJumpToTab = { onJumpToEntityTab(ref.refType, ref.title) },
            onDismiss = { inspectingRef = null }
        )
    }

    if (inspectingChar != null) {
        val ch = inspectingChar!!
        EntityDetailSheetDialog(
            ref = characterToEntityRef(ch),
            canJumpToTab = isGmMode || ch.kind == "player",
            onCopyFormatted = {
                val r = characterToEntityRef(ch)
                clipboard.setText(AnnotatedString("${r.emoji} ${r.title}\n${r.subtitle}\n${r.description}"))
                onNotify("Copied ${ch.name}")
            },
            onJumpToTab = { onJumpToEntityTab(ch.kind, ch.name) },
            onDismiss = { inspectingChar = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ChatMessageItem(
    msg: ChatMessage,
    currentUserId: String,
    isGmMode: Boolean,
    isHighlighted: Boolean,
    timeFormatter: SimpleDateFormat,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleReaction: (String) -> Unit,
    onDelete: () -> Unit,
    onJumpToReply: (String) -> Unit,
    onInspectRef: (ChatEntityRef) -> Unit,
) {
    val isMine = msg.senderId == currentUserId
    val isRoll = msg.messageType == "roll"
    val isSystem = msg.messageType == "system"
    val isForMe = msg.pinned && msg.targetUserId == currentUserId

    var menuExpanded by remember { mutableStateOf(false) }
    var showReactionBar by remember { mutableStateOf(false) }

    val baseColor = when {
        isForMe -> MaterialTheme.colorScheme.tertiaryContainer
        msg.pinned -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
        isSystem -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        isRoll -> MaterialTheme.colorScheme.primaryContainer
        isMine -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
        else -> MaterialTheme.colorScheme.surface
    }
    val bubbleColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.30f) else baseColor,
        label = "msg_highlight"
    )
    val reactions = remember(msg.reactionsJson) { JsonCodecs.decodeReactions(msg.reactionsJson) }
    val entityRef = remember(msg.id, msg.refTitle, msg.refDescription, msg.refTagsJson) { msg.toEntityRefOrNull() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = when {
            isSystem -> Arrangement.Center
            isMine -> Arrangement.End
            else -> Arrangement.Start
        }
    ) {
        Surface(
            color = bubbleColor,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = if (msg.pinned) 3.dp else 1.dp,
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .then(
                    if (isHighlighted || isForMe) {
                        Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.medium
                        )
                    } else Modifier
                )
                .clip(MaterialTheme.shapes.medium)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menuExpanded = true }
                )
                .testTag("chat_message_${msg.id}")
        ) {
            Column(
                modifier = Modifier.padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Discord-style quoted reply header
                if (msg.replyToSnippet.isNotBlank() || msg.replyToSenderName.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                            .clickable(enabled = msg.replyToMessageId.isNotBlank()) {
                                onJumpToReply(msg.replyToMessageId)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .heightIn(min = 16.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${msg.replyToSenderName.ifBlank { "Reply" }}: ${msg.replyToSnippet}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (msg.pinned) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "${msg.senderName} (${msg.senderRole})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val timeStr = msg.createdAt?.toDate()?.let { timeFormatter.format(it) } ?: ""
                        if (timeStr.isNotBlank()) {
                            Text(
                                timeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }
                        IconButton(
                            onClick = onReply,
                            modifier = Modifier.size(28.dp).testTag("reply_msg_${msg.id}")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Reply,
                                contentDescription = "Reply to message",
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(28.dp).testTag("copy_msg_${msg.id}")
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(28.dp).testTag("menu_msg_${msg.id}")
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Message actions",
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Reply") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onReply()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy Text") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onCopy()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (msg.pinned) "Unpin" else "Pin to Chat") },
                                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onTogglePin()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("React with Emoji") },
                                    leadingIcon = { Icon(Icons.Default.AddReaction, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        showReactionBar = !showReactionBar
                                    }
                                )
                                if (isMine || isGmMode) {
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onDelete()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Body
                if (msg.body.isNotBlank()) {
                    Text(
                        text = msg.body,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Rich Cross-Tab Entity Embed (Powers with descriptions & tags, Locations, Items, Notes, Characters)
                if (entityRef != null) {
                    ReferencedEntityCard(
                        ref = entityRef,
                        isPinned = msg.pinned,
                        isTargetedToCurrentUser = isForMe,
                        onClick = { onInspectRef(entityRef) }
                    )
                }

                // Reactions Row + Picker
                AnimatedVisibility(visible = showReactionBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QUICK_REACTION_EMOJIS.forEach { em ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.clickable {
                                    onToggleReaction(em)
                                    showReactionBar = false
                                }
                            ) {
                                Text(em, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }

                if (reactions.isNotEmpty() || showReactionBar) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        reactions.forEach { (emoji, uids) ->
                            val reactedByMe = currentUserId in uids
                            Surface(
                                color = if (reactedByMe) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .clickable { onToggleReaction(emoji) }
                            ) {
                                Text(
                                    text = "$emoji ${uids.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (reactedByMe) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.clickable { showReactionBar = !showReactionBar }
                        ) {
                            Icon(
                                Icons.Default.AddReaction,
                                contentDescription = "Add Reaction",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp).size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatComposer(
    sendDescription: String,
    userDisplayName: String,
    currentUserId: String,
    characters: List<Character>,
    catalog: List<ChatEntityRef>,
    replyingTo: ChatMessage?,
    attachedRef: ChatEntityRef?,
    selectedSpeaker: Character?,
    onClearReply: () -> Unit,
    onClearRef: () -> Unit,
    onSelectSpeaker: (Character?) -> Unit,
    onSelectAttachedRef: (ChatEntityRef) -> Unit,
    onOpenRefPicker: () -> Unit,
    onOpenPinCharacter: () -> Unit,
    onSendMessage: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var speakerMenuExpanded by remember { mutableStateOf(false) }

    val myCharacters = remember(characters, currentUserId) {
        characters.filter { it.ownerId == currentUserId || it.kind == "player" }
    }

    // Inline @mention autocomplete
    val mentionQuery = remember(draft) {
        val atIdx = draft.lastIndexOf('@')
        if (atIdx >= 0 && (atIdx == 0 || draft[atIdx - 1].isWhitespace())) {
            val sub = draft.substring(atIdx + 1)
            if (!sub.contains('\n')) sub else null
        } else null
    }
    val mentionMatches = remember(mentionQuery, catalog) {
        if (mentionQuery == null) emptyList()
        else catalog.filter {
            mentionQuery.isBlank() ||
                it.title.contains(mentionQuery, ignoreCase = true) ||
                it.refType.contains(mentionQuery, ignoreCase = true)
        }.take(8)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Inline @ autocomplete strip
        AnimatedVisibility(visible = mentionMatches.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                mentionMatches.forEach { entry ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.clickable {
                            onSelectAttachedRef(entry)
                            val atIdx = draft.lastIndexOf('@')
                            if (atIdx >= 0) {
                                draft = draft.substring(0, atIdx) + "@${entry.title} "
                            }
                        }
                    ) {
                        Text(
                            text = "${entry.emoji} ${entry.title} (${refCategoryLabel(entry.refType)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Replying Banner
        if (replyingTo != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().testTag("chat_reply_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Replying to ${replyingTo.senderName}: ${replyingTo.body.ifBlank { replyingTo.refTitle }}",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClearReply, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Reply", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Attached Entity Reference Banner
        if (attachedRef != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().testTag("chat_attached_ref_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(attachedRef.emoji, style = MaterialTheme.typography.bodyMedium)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${refCategoryLabel(attachedRef.refType)}: ${attachedRef.title}",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (attachedRef.tags.isNotEmpty()) {
                            Text(
                                text = attachedRef.tags.joinToString(" ") { "#$it" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onClearRef, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Reference", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Quick Action Toolbar: Speaker As, @Reference Picker, Pin Character
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .clickable { speakerMenuExpanded = true }
                        .testTag("chat_speaker_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(
                            text = selectedSpeaker?.let { "${it.avatarEmoji} ${it.name}" }
                                ?: "As: ${userDisplayName.ifBlank { "You" }}",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                DropdownMenu(
                    expanded = speakerMenuExpanded,
                    onDismissRequest = { speakerMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("${userDisplayName.ifBlank { "Default User" }} (Default)") },
                        onClick = {
                            onSelectSpeaker(null)
                            speakerMenuExpanded = false
                        }
                    )
                    myCharacters.forEach { ch ->
                        DropdownMenuItem(
                            text = { Text("${ch.avatarEmoji} ${ch.name}") },
                            onClick = {
                                onSelectSpeaker(ch)
                                speakerMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .clickable(onClick = onOpenRefPicker)
                    .testTag("chat_open_ref_picker_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AlternateEmail,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Reference Entry",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (characters.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .clickable(onClick = onOpenPinCharacter)
                        .testTag("chat_pin_char_shortcut_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Pin Character",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Input + Send Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Message party... (@ref or /roll 1d20)") },
                maxLines = 3,
                modifier = Modifier.weight(1f).testTag("chat_message_input")
            )
            Button(
                onClick = {
                    val text = draft.trim()
                    if (text.isNotEmpty() || attachedRef != null) {
                        onSendMessage(text)
                        draft = ""
                    }
                },
                enabled = draft.isNotBlank() || attachedRef != null,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("chat_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = sendDescription)
            }
        }
    }
}
