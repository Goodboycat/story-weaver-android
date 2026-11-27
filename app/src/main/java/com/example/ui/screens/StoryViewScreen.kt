package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.AiChatMode
import com.example.ai.AiChatUiState
import com.example.data.AppStrings
import com.example.data.QuickMapBlock
import com.example.data.UserSettings
import com.example.notifications.SessionNotificationScheduler
import com.example.ui.components.AiChatDialog
import com.example.ui.components.DiceRollerBottomSheet
import com.example.ui.components.SessionScheduleDialog
import com.example.ui.tabs.ChatTab
import com.example.ui.tabs.EntitiesTab
import com.example.ui.tabs.GridMapTab
import com.example.ui.tabs.GuideTab
import com.example.ui.tabs.LogsTab
import com.example.ui.tabs.WorldListTab
import com.example.ui.tabs.characterToEntityRef
import com.example.ui.tabs.worldItemToEntityRef
import com.example.viewmodel.AiMapUiState
import com.example.viewmodel.StoryViewModel
import com.example.viewmodel.UiState

private enum class StoryTabId {
    GUIDE, PLAYERS, NPCS, MONSTERS, MAP, LOCATIONS, ITEMS, POWERS, NOTES, CHAT, LOGS
}

private data class StoryTabSpec(
    val id: StoryTabId,
    val label: String,
    val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewScreen(
    viewModel: StoryViewModel,
    strings: AppStrings,
    userDisplayName: String,
    onNavigateBack: () -> Unit,
) {
    val storyState by viewModel.storyState.collectAsStateWithLifecycle()
    val charactersState by viewModel.charactersState.collectAsStateWithLifecycle()
    val worldItemsState by viewModel.worldItemsState.collectAsStateWithLifecycle()
    val rollsState by viewModel.rollsState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val logsState by viewModel.logsState.collectAsStateWithLifecycle()
    val quickAssets by viewModel.quickMapAssets.collectAsStateWithLifecycle()
    val quickBlocks by viewModel.quickMapBlocks.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val viewAsPlayer by viewModel.viewAsPlayer.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val aiMapState by viewModel.aiMapState.collectAsStateWithLifecycle()
    val aiChatState by viewModel.aiChatState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var showDiceRoller by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showAiChat by remember { mutableStateOf(false) }
    var locationJumpQuery by remember { mutableStateOf("") }
    var locationJumpTrigger by remember { mutableIntStateOf(0) }
    var focusChatMessageId by remember { mutableStateOf<String?>(null) }
    var dismissedPinIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    val story = (storyState as? UiState.Success)?.data
    var seenPinIds by remember(story?.id) { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(story?.id, story?.memberUids) {
        if (story != null) {
            viewModel.onStoryObserved(story)
        }
    }

    LaunchedEffect(story?.id, story?.nextSessionAtMillis, story?.nextSessionNote) {
        if (story != null) {
            SessionNotificationScheduler.syncCampaignNotification(context, story)
        }
    }

    val isActualGm = story != null && story.gmUserId == viewModel.currentUserId
    val isGmMode = isActualGm && !viewAsPlayer

    val tabs = remember(isGmMode, strings) {
        if (isGmMode) {
            listOf(
                StoryTabSpec(StoryTabId.GUIDE, strings.tabGuide, Icons.AutoMirrored.Filled.MenuBook),
                StoryTabSpec(StoryTabId.PLAYERS, strings.tabPlayers, Icons.Default.Person),
                StoryTabSpec(StoryTabId.NPCS, strings.tabNpcs, Icons.Default.Groups),
                StoryTabSpec(StoryTabId.MONSTERS, strings.tabMonsters, Icons.Default.BugReport),
                StoryTabSpec(StoryTabId.LOCATIONS, strings.tabLocations, Icons.Default.Place),
                StoryTabSpec(StoryTabId.POWERS, strings.tabPowers, Icons.Default.AutoAwesome),
                StoryTabSpec(StoryTabId.MAP, strings.tabMap, Icons.Default.Map),
                StoryTabSpec(StoryTabId.ITEMS, strings.tabItems, Icons.Default.Build),
                StoryTabSpec(StoryTabId.NOTES, strings.tabNotes, Icons.Default.Description),
                StoryTabSpec(StoryTabId.CHAT, strings.tabChat, Icons.AutoMirrored.Filled.Chat),
                StoryTabSpec(StoryTabId.LOGS, strings.tabLogs, Icons.Default.History),
            )
        } else {
            listOf(
                StoryTabSpec(StoryTabId.PLAYERS, strings.tabPlayers, Icons.Default.Person),
                StoryTabSpec(StoryTabId.MAP, strings.tabMap, Icons.Default.Map),
                StoryTabSpec(StoryTabId.CHAT, strings.tabChat, Icons.AutoMirrored.Filled.Chat),
                StoryTabSpec(StoryTabId.LOGS, strings.tabLogs, Icons.Default.History),
            )
        }
    }

    var selectedTabIndex by remember(isGmMode) { mutableIntStateOf(0) }
    val safeIndex = selectedTabIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
    val activeTab = tabs[safeIndex]

    val characters = (charactersState as? UiState.Success)?.data ?: emptyList()
    val worldItems = (worldItemsState as? UiState.Success)?.data ?: emptyList()
    val rolls = (rollsState as? UiState.Success)?.data ?: emptyList()
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
    val logs = (logsState as? UiState.Success)?.data ?: emptyList()

    val pinnedCharacterIds = remember(messages) {
        messages.filter { it.pinned && it.targetCharacterId.isNotBlank() }
            .map { it.targetCharacterId }
            .toSet()
    }
    val myTargetedPins = remember(messages, viewModel.currentUserId) {
        messages.filter {
            it.pinned &&
                it.targetUserId == viewModel.currentUserId &&
                it.targetCharacterName.isNotBlank()
        }
    }
    LaunchedEffect(story?.id, myTargetedPins) {
        val currentIds = myTargetedPins.map { it.id }.toSet()
        val previous = seenPinIds
        if (previous == null) {
            seenPinIds = currentIds
        } else {
            val newlyArrived = myTargetedPins.filter { it.id !in previous }
            seenPinIds = currentIds
            newlyArrived.lastOrNull()?.let { pin ->
                if (story != null) {
                    SessionNotificationScheduler.showPinnedCharacterNotification(
                        context = context,
                        storyId = story.id,
                        storyTitle = story.title,
                        characterName = pin.targetCharacterName,
                        messageBody = pin.body,
                    )
                    viewModel.showToast("📌 ${pin.targetCharacterName} was pinned in Chat!")
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("story_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = story?.title ?: "Campaign",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (story != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable {
                                    clipboard.setText(AnnotatedString(story.joinCode))
                                }
                            ) {
                                Text(
                                    text = "${story.systemName} • Code: ${story.joinCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy Join Code",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (story != null && activeTab.id == StoryTabId.CHAT) {
                        IconButton(
                            onClick = { showDiceRoller = true },
                            modifier = Modifier.testTag("open_dice_roller_chat_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = strings.diceRoller,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (story != null) {
                        IconButton(
                            onClick = { showScheduleDialog = true },
                            modifier = Modifier.testTag("open_schedule_button")
                        ) {
                            Icon(
                                imageVector = if (story.nextSessionAtMillis > System.currentTimeMillis()) {
                                    Icons.Default.NotificationsActive
                                } else {
                                    Icons.Default.Schedule
                                },
                                contentDescription = "Session Schedule & Alerts",
                                tint = if (story.nextSessionAtMillis > System.currentTimeMillis()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    if (isActualGm) {
                        FilterChip(
                            selected = viewAsPlayer,
                            onClick = { viewModel.toggleViewAsPlayer() },
                            leadingIcon = {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(if (viewAsPlayer) strings.viewAsPlayer else strings.gmMode) },
                            modifier = Modifier.padding(end = 8.dp).testTag("toggle_view_as_player_chip")
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (story != null && activeTab.id != StoryTabId.CHAT) {
                FloatingActionButton(
                    onClick = { showDiceRoller = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("open_dice_roller_fab")
                ) {
                    Icon(Icons.Default.Casino, contentDescription = strings.diceRoller)
                }
            }
        }
    ) { innerPadding ->
        when (val st = storyState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(innerPadding).padding(20.dp), contentAlignment = Alignment.Center) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                        Text(st.message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            is UiState.Success -> {
                val loadedStory = st.data
                if (loadedStory == null) {
                    Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text("Campaign not found or removed.")
                    }
                } else {
                    val activePinAlert = myTargetedPins.lastOrNull { it.id !in dismissedPinIds }
                    fun handleJumpToEntityTab(refType: String, query: String) {
                        val targetId = when (refType) {
                            "location" -> StoryTabId.LOCATIONS
                            "power" -> StoryTabId.POWERS
                            "item" -> StoryTabId.ITEMS
                            "note" -> StoryTabId.NOTES
                            "player" -> StoryTabId.PLAYERS
                            "npc" -> StoryTabId.NPCS
                            "monster" -> StoryTabId.MONSTERS
                            else -> null
                        } ?: return
                        val idx = tabs.indexOfFirst { it.id == targetId }
                        if (idx >= 0) {
                            if (refType == "location") {
                                locationJumpQuery = query
                                locationJumpTrigger++
                            }
                            selectedTabIndex = idx
                        }
                    }

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                    ) {
                        val isWide = maxWidth >= 720.dp
                        if (isWide) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                NavigationRail(
                                    modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())
                                ) {
                                    tabs.forEachIndexed { idx, spec ->
                                        val pinCount = if (spec.id == StoryTabId.CHAT) myTargetedPins.size else 0
                                        NavigationRailItem(
                                            selected = safeIndex == idx,
                                            onClick = { selectedTabIndex = idx },
                                            icon = {
                                                if (pinCount > 0) {
                                                    BadgedBox(badge = { Badge { Text(pinCount.toString()) } }) {
                                                        Icon(spec.icon, contentDescription = spec.label)
                                                    }
                                                } else {
                                                    Icon(spec.icon, contentDescription = spec.label)
                                                }
                                            },
                                            label = { Text(spec.label) },
                                            modifier = Modifier.testTag("story_tab_${spec.id.name.lowercase()}")
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    if (activeTab.id != StoryTabId.CHAT && activePinAlert != null) {
                                        TargetedPinAlertBanner(
                                            characterName = activePinAlert.targetCharacterName,
                                            onViewInChat = {
                                                val chatIdx = tabs.indexOfFirst { it.id == StoryTabId.CHAT }
                                                if (chatIdx >= 0) {
                                                    focusChatMessageId = activePinAlert.id
                                                    selectedTabIndex = chatIdx
                                                }
                                            },
                                            onDismiss = {
                                                dismissedPinIds = dismissedPinIds + activePinAlert.id
                                            }
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        StoryTabContent(
                                            tabId = activeTab.id,
                                            story = loadedStory,
                                            strings = strings,
                                            isGmMode = isGmMode,
                                            userDisplayName = userDisplayName,
                                            viewModel = viewModel,
                                            characters = characters,
                                            worldItems = worldItems,
                                            messages = messages,
                                            pinnedCharacterIds = pinnedCharacterIds,
                                            logs = logs,
                                            quickAssets = quickAssets,
                                            quickBlocks = quickBlocks,
                                            aiState = aiState,
                                            aiMapState = aiMapState,
                                            userSettings = userSettings,
                                            aiChatState = aiChatState,
                                            locationJumpQuery = locationJumpQuery,
                                            locationJumpTrigger = locationJumpTrigger,
                                            focusChatMessageId = focusChatMessageId,
                                            onConsumeFocusMessage = { focusChatMessageId = null },
                                            onJumpToLocation = { loc ->
                                                val locIdx = tabs.indexOfFirst { it.id == StoryTabId.LOCATIONS }
                                                if (locIdx >= 0) {
                                                    locationJumpQuery = loc
                                                    locationJumpTrigger++
                                                    selectedTabIndex = locIdx
                                                }
                                            },
                                            onJumpToEntityTab = ::handleJumpToEntityTab,
                                            onOpenAiChat = { showAiChat = true },
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                ScrollableTabRow(
                                    selectedTabIndex = safeIndex,
                                    edgePadding = 8.dp
                                ) {
                                    tabs.forEachIndexed { idx, spec ->
                                        val pinCount = if (spec.id == StoryTabId.CHAT) myTargetedPins.size else 0
                                        Tab(
                                            selected = safeIndex == idx,
                                            onClick = { selectedTabIndex = idx },
                                            icon = {
                                                if (pinCount > 0) {
                                                    BadgedBox(badge = { Badge { Text(pinCount.toString()) } }) {
                                                        Icon(spec.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    }
                                                } else {
                                                    Icon(spec.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                                }
                                            },
                                            text = { Text(spec.label) },
                                            modifier = Modifier.testTag("story_tab_${spec.id.name.lowercase()}")
                                        )
                                    }
                                }
                                if (activeTab.id != StoryTabId.CHAT && activePinAlert != null) {
                                    TargetedPinAlertBanner(
                                        characterName = activePinAlert.targetCharacterName,
                                        onViewInChat = {
                                            val chatIdx = tabs.indexOfFirst { it.id == StoryTabId.CHAT }
                                            if (chatIdx >= 0) {
                                                focusChatMessageId = activePinAlert.id
                                                selectedTabIndex = chatIdx
                                            }
                                        },
                                        onDismiss = {
                                            dismissedPinIds = dismissedPinIds + activePinAlert.id
                                        }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    StoryTabContent(
                                        tabId = activeTab.id,
                                        story = loadedStory,
                                        strings = strings,
                                        isGmMode = isGmMode,
                                        userDisplayName = userDisplayName,
                                        viewModel = viewModel,
                                        characters = characters,
                                        worldItems = worldItems,
                                        messages = messages,
                                        pinnedCharacterIds = pinnedCharacterIds,
                                        logs = logs,
                                        quickAssets = quickAssets,
                                        quickBlocks = quickBlocks,
                                        aiState = aiState,
                                        aiMapState = aiMapState,
                                        userSettings = userSettings,
                                        aiChatState = aiChatState,
                                        locationJumpQuery = locationJumpQuery,
                                        locationJumpTrigger = locationJumpTrigger,
                                        focusChatMessageId = focusChatMessageId,
                                        onConsumeFocusMessage = { focusChatMessageId = null },
                                        onJumpToLocation = { loc ->
                                            val locIdx = tabs.indexOfFirst { it.id == StoryTabId.LOCATIONS }
                                            if (locIdx >= 0) {
                                                locationJumpQuery = loc
                                                locationJumpTrigger++
                                                selectedTabIndex = locIdx
                                            }
                                        },
                                        onJumpToEntityTab = ::handleJumpToEntityTab,
                                        onOpenAiChat = { showAiChat = true },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDiceRoller && story != null) {
        DiceRollerBottomSheet(
            strings = strings,
            rolls = rolls,
            onDismiss = { showDiceRoller = false },
            onRoll = { terms, mods ->
                viewModel.rollDice(story, terms, mods, userDisplayName)
            }
        )
    }

    if (showAiChat && story != null) {
        AiChatDialog(
            storyTitle = story.title,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSendMessage = { prompt -> viewModel.sendAiChatMessage(story, prompt, mode = AiChatMode.TEXT) },
            onSendImagePrompt = { prompt -> viewModel.sendAiChatMessage(story, prompt, mode = AiChatMode.IMAGE) },
            onClearError = { viewModel.clearAiChatError() },
            onClearChat = { viewModel.clearAiChat() },
            onDeleteMessages = { ids -> viewModel.deleteAiChatMessages(ids) },
            onSelectProvider = { viewModel.setAiProvider(it) },
            onSelectModel = { viewModel.setAiModel(it) },
            onSelectEndpointUrl = { viewModel.setAiEndpointUrl(it) },
            onSelectApiKey = { viewModel.setAiApiKey(it) },
            onSelectImageEndpointUrl = { viewModel.setAiImageEndpointUrl(it) },
            onSelectImageModel = { viewModel.setAiImageModel(it) },
            onSelectImageApiKey = { viewModel.setAiImageApiKey(it) },
            onPostToPartyChat = { reply ->
                viewModel.sendChatMessage(story, "🤖 AI (${userSettings.aiModel}):\n$reply")
            },
            onNotify = { msg -> viewModel.showToast(msg) },
            onDismiss = { showAiChat = false }
        )
    }

    if (showScheduleDialog && story != null) {
        SessionScheduleDialog(
            story = story,
            isGm = isActualGm,
            onSaveSchedule = { sessionMillis, note ->
                viewModel.updateSessionSchedule(story, sessionMillis, note) { savedMillis, savedNote ->
                    val updated = story.copy(
                        nextSessionAtMillis = savedMillis,
                        nextSessionNote = savedNote,
                    )
                    SessionNotificationScheduler.syncCampaignNotification(
                        context = context,
                        story = updated,
                        forceImmediateAnnouncement = savedMillis > 0L,
                    )
                    showScheduleDialog = false
                }
            },
            onDismiss = { showScheduleDialog = false }
        )
    }
}

@Composable
private fun TargetedPinAlertBanner(
    characterName: String,
    onViewInChat: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().testTag("targeted_pin_alert_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = "Your character \"$characterName\" was pinned in Chat!",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onViewInChat) {
                Text("View")
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun StoryTabContent(
    tabId: StoryTabId,
    story: com.example.data.Story,
    strings: AppStrings,
    isGmMode: Boolean,
    userDisplayName: String,
    viewModel: StoryViewModel,
    characters: List<com.example.data.Character>,
    worldItems: List<com.example.data.WorldItem>,
    messages: List<com.example.data.ChatMessage>,
    pinnedCharacterIds: Set<String>,
    logs: List<com.example.data.ActivityLog>,
    quickAssets: List<com.example.data.QuickMapAsset>,
    quickBlocks: List<QuickMapBlock>,
    aiState: com.example.viewmodel.AiUiState,
    aiMapState: AiMapUiState,
    userSettings: UserSettings,
    aiChatState: AiChatUiState,
    locationJumpQuery: String,
    locationJumpTrigger: Int,
    focusChatMessageId: String?,
    onConsumeFocusMessage: () -> Unit,
    onJumpToLocation: (String) -> Unit,
    onJumpToEntityTab: (String, String) -> Unit,
    onOpenAiChat: () -> Unit,
) {
    when (tabId) {
        StoryTabId.GUIDE -> GuideTab(
            story = story,
            onToggleStep = { viewModel.toggleGuideStep(story, it) },
            onResetProgress = { viewModel.resetGuideProgress(story) }
        )
        StoryTabId.PLAYERS -> EntitiesTab(
            story = story,
            kind = "player",
            characters = characters,
            worldItems = worldItems,
            messages = messages,
            pinnedCharacterIds = pinnedCharacterIds,
            currentUserId = viewModel.currentUserId,
            isGmMode = isGmMode,
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onSelectAiModel = { viewModel.setAiModel(it) },
            onSelectAiImageModel = { viewModel.setAiImageModel(it) },
            onGenerateAiImage = { prompt, maxDim, onOk, onErr ->
                viewModel.generateEntityImage(prompt, maxDim, onOk, onErr)
            },
            onSendAiChat = { viewModel.sendAiChatMessage(story, it, "player characters") },
            onSaveCharacter = { viewModel.saveCharacter(story, it) },
            onDeleteCharacters = { viewModel.deleteCharacters(story, it) },
            onImportAi = { raw, regex, done ->
                viewModel.importCharactersWithAi(story, raw, "player", regex, done)
            },
            onParseEquipmentAi = { raw, onParsed -> viewModel.parseEquipmentWithAi(raw, onParsed) },
            onEditCharactersAi = { targets, instruction, done ->
                viewModel.editCharactersWithAi(story, targets, instruction, done)
            },
            onAssistTabAi = { current, tab, prompt, isEdit, useRegex, onApplied ->
                viewModel.assistCharacterTabWithAi(story, current, tab, prompt, isEdit, useRegex, onApplied)
            },
            onResetAiState = { viewModel.resetAiState() },
            onJumpToLocation = onJumpToLocation,
            onShareCharacterToChat = { ch -> viewModel.shareEntityToChat(story, characterToEntityRef(ch)) },
            onPinCharacter = { ch, uid, note -> viewModel.pinCharacterToChat(story, ch, uid, note) },
            onUnpinCharacter = { chId -> viewModel.unpinCharacterFromChat(story, chId) },
        )
        StoryTabId.NPCS -> EntitiesTab(
            story = story,
            kind = "npc",
            characters = characters,
            worldItems = worldItems,
            messages = messages,
            pinnedCharacterIds = pinnedCharacterIds,
            currentUserId = viewModel.currentUserId,
            isGmMode = isGmMode,
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onSelectAiModel = { viewModel.setAiModel(it) },
            onSelectAiImageModel = { viewModel.setAiImageModel(it) },
            onGenerateAiImage = { prompt, maxDim, onOk, onErr ->
                viewModel.generateEntityImage(prompt, maxDim, onOk, onErr)
            },
            onSendAiChat = { viewModel.sendAiChatMessage(story, it, "NPCs") },
            onSaveCharacter = { viewModel.saveCharacter(story, it) },
            onDeleteCharacters = { viewModel.deleteCharacters(story, it) },
            onImportAi = { raw, regex, done ->
                viewModel.importCharactersWithAi(story, raw, "npc", regex, done)
            },
            onParseEquipmentAi = { raw, onParsed -> viewModel.parseEquipmentWithAi(raw, onParsed) },
            onEditCharactersAi = { targets, instruction, done ->
                viewModel.editCharactersWithAi(story, targets, instruction, done)
            },
            onAssistTabAi = { current, tab, prompt, isEdit, useRegex, onApplied ->
                viewModel.assistCharacterTabWithAi(story, current, tab, prompt, isEdit, useRegex, onApplied)
            },
            onResetAiState = { viewModel.resetAiState() },
            onJumpToLocation = onJumpToLocation,
            onShareCharacterToChat = { ch -> viewModel.shareEntityToChat(story, characterToEntityRef(ch)) },
            onPinCharacter = { ch, uid, note -> viewModel.pinCharacterToChat(story, ch, uid, note) },
            onUnpinCharacter = { chId -> viewModel.unpinCharacterFromChat(story, chId) },
        )
        StoryTabId.MONSTERS -> EntitiesTab(
            story = story,
            kind = "monster",
            characters = characters,
            worldItems = worldItems,
            messages = messages,
            pinnedCharacterIds = pinnedCharacterIds,
            currentUserId = viewModel.currentUserId,
            isGmMode = isGmMode,
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onSelectAiModel = { viewModel.setAiModel(it) },
            onSelectAiImageModel = { viewModel.setAiImageModel(it) },
            onGenerateAiImage = { prompt, maxDim, onOk, onErr ->
                viewModel.generateEntityImage(prompt, maxDim, onOk, onErr)
            },
            onSendAiChat = { viewModel.sendAiChatMessage(story, it, "monsters & encounters") },
            onSaveCharacter = { viewModel.saveCharacter(story, it) },
            onDeleteCharacters = { viewModel.deleteCharacters(story, it) },
            onImportAi = { raw, regex, done ->
                viewModel.importCharactersWithAi(story, raw, "monster", regex, done)
            },
            onParseEquipmentAi = { raw, onParsed -> viewModel.parseEquipmentWithAi(raw, onParsed) },
            onEditCharactersAi = { targets, instruction, done ->
                viewModel.editCharactersWithAi(story, targets, instruction, done)
            },
            onAssistTabAi = { current, tab, prompt, isEdit, useRegex, onApplied ->
                viewModel.assistCharacterTabWithAi(story, current, tab, prompt, isEdit, useRegex, onApplied)
            },
            onResetAiState = { viewModel.resetAiState() },
            onJumpToLocation = onJumpToLocation,
            onShareCharacterToChat = { ch -> viewModel.shareEntityToChat(story, characterToEntityRef(ch)) },
            onPinCharacter = { ch, uid, note -> viewModel.pinCharacterToChat(story, ch, uid, note) },
            onUnpinCharacter = { chId -> viewModel.unpinCharacterFromChat(story, chId) },
        )
        StoryTabId.LOCATIONS -> WorldListTab(
            story = story,
            category = "location",
            items = worldItems,
            currentUserId = viewModel.currentUserId,
            isGmMode = isGmMode,
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            initialSearchQuery = locationJumpQuery,
            initialSearchTrigger = locationJumpTrigger,
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onSelectAiModel = { viewModel.setAiModel(it) },
            onSelectAiImageModel = { viewModel.setAiImageModel(it) },
            onGenerateAiImage = { prompt, maxDim, onOk, onErr ->
                viewModel.generateEntityImage(prompt, maxDim, onOk, onErr)
            },
            onSendAiChat = { viewModel.sendAiChatMessage(story, it, "locations") },
            onSaveItem = { viewModel.saveWorldItem(story, it) },
            onReorder = { viewModel.reorderWorldItems(story, it) },
            onDeleteItems = { viewModel.deleteWorldItems(story, it) },
            onImportAi = { raw, start, regex, done ->
                viewModel.importWorldItemsWithAi(story, raw, "location", start, regex, done)
            },
            onEditWorldItemsAi = { targets, instruction, done ->
                viewModel.editWorldItemsWithAi(story, targets, "location", instruction, done)
            },
            onAssistSingleWorldItemAi = { current, prompt, isEdit, regex, onApplied ->
                viewModel.assistSingleWorldItemWithAi(story, current, "location", prompt, isEdit, regex, onApplied)
            },
            onResetAiState = { viewModel.resetAiState() },
            onShareToChat = { item -> viewModel.shareEntityToChat(story, worldItemToEntityRef(item)) },
        )
        StoryTabId.POWERS -> WorldListTab(
            story = story,
            category = "power",
            items = worldItems,
            currentUserId = viewModel.currentUserId,
            isGmMode = isGmMode,
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onSelectAiModel = { viewModel.setAiModel(it) },
            onSelectAiImageModel = { viewModel.setAiImageModel(it) },
            onGenerateAiImage = { prompt, maxDim, onOk, onErr ->
                viewModel.generateEntityImage(prompt, maxDim, onOk, onErr)
            },
            onSendAiChat = { viewModel.sendAiChatMessage(story, it, "powers & spells") },
            onSaveItem = { viewModel.saveWorldItem(story, it) },
            onReorder = { viewModel.reorderWorldItems(story, it) },
            onDeleteItems = { viewModel.deleteWorldItems(story, it) },
            onImportAi = { raw, start, regex, done ->
                viewModel.importWorldItemsWithAi(story, raw, "power", start, regex, done)
            },
            onEditWorldItemsAi = { targets, instruction, done ->
                viewModel.editWorldItemsWithAi(story, targets, "power", instruction, done)
            },
            onAssistSingleWorldItemAi = { current, prompt, isEdit, regex, onApplied ->
                viewModel.assistSingleWorldItemWithAi(story, current, "power", prompt, isEdit, regex, onApplied)
            },
            onResetAiState = { viewModel.resetAiState() },
            onShareToChat = { item -> viewModel.shareEntityToChat(story, worldItemToEntityRef(item)) },
        )
        StoryTabId.MAP -> GridMapTab(
            story = story,
            characters = characters,
            quickAssets = quickAssets,
            quickBlocks = quickBlocks,
            currentUserId = viewModel.currentUserId,
            isGmMode = isGmMode,
            userSettings = userSettings,
            aiMapState = aiMapState,
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onSelectAiModel = { viewModel.setAiModel(it) },
            onSelectAiEndpointUrl = { viewModel.setAiEndpointUrl(it) },
            onSelectAiApiKey = { viewModel.setAiApiKey(it) },
            onClearAiMapError = { viewModel.clearAiMapError() },
            onGenerateAiMap = { prompt, w, h, g, t, replace, tw, th, onApplied ->
                viewModel.generateMapWithAi(story, prompt, w, h, g, t, replace, tw, th, onApplied)
            },
            onSyncMap = { w, h, g, t, bg, imm ->
                viewModel.syncMapDebounced(story, w, h, g, t, bg, imm)
            },
            onAddQuickAsset = { viewModel.addQuickMapAsset(it) },
            onAddQuickBlock = { viewModel.addQuickMapBlock(it) },
        )
        StoryTabId.ITEMS -> WorldListTab(
            story = story,
            category = "item",
            items = worldItems,
            currentUserId = viewModel.currentUserId,
            isGmMode = isGmMode,
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onSelectAiModel = { viewModel.setAiModel(it) },
            onSelectAiImageModel = { viewModel.setAiImageModel(it) },
            onGenerateAiImage = { prompt, maxDim, onOk, onErr ->
                viewModel.generateEntityImage(prompt, maxDim, onOk, onErr)
            },
            onSendAiChat = { viewModel.sendAiChatMessage(story, it, "items, tools, weapons & equipment") },
            onSaveItem = { viewModel.saveWorldItem(story, it) },
            onReorder = { viewModel.reorderWorldItems(story, it) },
            onDeleteItems = { viewModel.deleteWorldItems(story, it) },
            onImportAi = { raw, start, regex, done ->
                viewModel.importWorldItemsWithAi(story, raw, "item", start, regex, done)
            },
            onEditWorldItemsAi = { targets, instruction, done ->
                viewModel.editWorldItemsWithAi(story, targets, "item", instruction, done)
            },
            onAssistSingleWorldItemAi = { current, prompt, isEdit, regex, onApplied ->
                viewModel.assistSingleWorldItemWithAi(story, current, "item", prompt, isEdit, regex, onApplied)
            },
            onResetAiState = { viewModel.resetAiState() },
            onShareToChat = { item -> viewModel.shareEntityToChat(story, worldItemToEntityRef(item)) },
        )
        StoryTabId.NOTES -> WorldListTab(
            story = story,
            category = "note",
            items = worldItems,
            currentUserId = viewModel.currentUserId,
            isGmMode = isGmMode,
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onSelectAiModel = { viewModel.setAiModel(it) },
            onSelectAiImageModel = { viewModel.setAiImageModel(it) },
            onGenerateAiImage = { prompt, maxDim, onOk, onErr ->
                viewModel.generateEntityImage(prompt, maxDim, onOk, onErr)
            },
            onSendAiChat = { viewModel.sendAiChatMessage(story, it, "campaign notes") },
            onSaveItem = { viewModel.saveWorldItem(story, it) },
            onReorder = { viewModel.reorderWorldItems(story, it) },
            onDeleteItems = { viewModel.deleteWorldItems(story, it) },
            onImportAi = { raw, start, regex, done ->
                viewModel.importWorldItemsWithAi(story, raw, "note", start, regex, done)
            },
            onEditWorldItemsAi = { targets, instruction, done ->
                viewModel.editWorldItemsWithAi(story, targets, "note", instruction, done)
            },
            onAssistSingleWorldItemAi = { current, prompt, isEdit, regex, onApplied ->
                viewModel.assistSingleWorldItemWithAi(story, current, "note", prompt, isEdit, regex, onApplied)
            },
            onResetAiState = { viewModel.resetAiState() },
            onShareToChat = { item -> viewModel.shareEntityToChat(story, worldItemToEntityRef(item)) },
        )
        StoryTabId.CHAT -> ChatTab(
            story = story,
            strings = strings,
            messages = messages,
            characters = characters,
            worldItems = worldItems,
            currentUserId = viewModel.currentUserId,
            userDisplayName = userDisplayName,
            isGmMode = isGmMode,
            activeAiModelLabel = userSettings.aiModel,
            focusMessageId = focusChatMessageId,
            onConsumeFocusMessage = onConsumeFocusMessage,
            onOpenAiChat = onOpenAiChat,
            onSendMessage = { body, speaker, replyTo, ref, pinned ->
                viewModel.sendChatMessage(
                    story = story,
                    body = body,
                    speakerName = speaker,
                    replyTo = replyTo,
                    entityRef = ref,
                    pinned = pinned,
                )
            },
            onTogglePinMessage = { msg -> viewModel.togglePinMessage(story, msg) },
            onToggleReaction = { msg, emoji -> viewModel.toggleMessageReaction(story, msg, emoji) },
            onDeleteMessage = { msgId -> viewModel.deleteChatMessage(story, msgId) },
            onDeleteMessages = { msgIds -> viewModel.deleteChatMessages(story, msgIds) },
            onPinCharacter = { ch, uid, note -> viewModel.pinCharacterToChat(story, ch, uid, note) },
            onSlashRoll = { cmd, roller -> viewModel.trySendSlashRoll(story, cmd, roller) },
            onJumpToEntityTab = onJumpToEntityTab,
            onNotify = { msg -> viewModel.showToast(msg) },
        )
        StoryTabId.LOGS -> LogsTab(logs = logs)
    }
}
