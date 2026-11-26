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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
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
import com.example.ai.AiChatUiState
import com.example.data.AppStrings
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
import com.example.viewmodel.StoryViewModel
import com.example.viewmodel.UiState

private enum class StoryTabId {
    GUIDE, PLAYERS, NPCS, MONSTERS, MAP, LOCATIONS, POWERS, NOTES, CHAT, LOGS
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
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val viewAsPlayer by viewModel.viewAsPlayer.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val aiChatState by viewModel.aiChatState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var showDiceRoller by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showAiChat by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    val story = (storyState as? UiState.Success)?.data
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
                StoryTabSpec(StoryTabId.MAP, strings.tabMap, Icons.Default.Map),
                StoryTabSpec(StoryTabId.LOCATIONS, strings.tabLocations, Icons.Default.Place),
                StoryTabSpec(StoryTabId.POWERS, strings.tabPowers, Icons.Default.AutoAwesome),
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
                                        NavigationRailItem(
                                            selected = safeIndex == idx,
                                            onClick = { selectedTabIndex = idx },
                                            icon = { Icon(spec.icon, contentDescription = spec.label) },
                                            label = { Text(spec.label) },
                                            modifier = Modifier.testTag("story_tab_${spec.id.name.lowercase()}")
                                        )
                                    }
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    StoryTabContent(
                                        tabId = activeTab.id,
                                        story = loadedStory,
                                        strings = strings,
                                        isGmMode = isGmMode,
                                        viewModel = viewModel,
                                        characters = characters,
                                        worldItems = worldItems,
                                        messages = messages,
                                        logs = logs,
                                        quickAssets = quickAssets,
                                        aiState = aiState,
                                        userSettings = userSettings,
                                        aiChatState = aiChatState,
                                        onOpenAiChat = { showAiChat = true },
                                    )
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                ScrollableTabRow(
                                    selectedTabIndex = safeIndex,
                                    edgePadding = 8.dp
                                ) {
                                    tabs.forEachIndexed { idx, spec ->
                                        Tab(
                                            selected = safeIndex == idx,
                                            onClick = { selectedTabIndex = idx },
                                            icon = { Icon(spec.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            text = { Text(spec.label) },
                                            modifier = Modifier.testTag("story_tab_${spec.id.name.lowercase()}")
                                        )
                                    }
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    StoryTabContent(
                                        tabId = activeTab.id,
                                        story = loadedStory,
                                        strings = strings,
                                        isGmMode = isGmMode,
                                        viewModel = viewModel,
                                        characters = characters,
                                        worldItems = worldItems,
                                        messages = messages,
                                        logs = logs,
                                        quickAssets = quickAssets,
                                        aiState = aiState,
                                        userSettings = userSettings,
                                        aiChatState = aiChatState,
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
            onSendMessage = { prompt -> viewModel.sendAiChatMessage(story, prompt) },
            onClearChat = { viewModel.clearAiChat() },
            onSelectProvider = { viewModel.setAiProvider(it) },
            onSelectModel = { viewModel.setAiModel(it) },
            onPostToPartyChat = { reply ->
                viewModel.sendChatMessage(story, "🤖 AI (${userSettings.aiModel}):\n$reply")
            },
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
private fun StoryTabContent(
    tabId: StoryTabId,
    story: com.example.data.Story,
    strings: AppStrings,
    isGmMode: Boolean,
    viewModel: StoryViewModel,
    characters: List<com.example.data.Character>,
    worldItems: List<com.example.data.WorldItem>,
    messages: List<com.example.data.ChatMessage>,
    logs: List<com.example.data.ActivityLog>,
    quickAssets: List<com.example.data.QuickMapAsset>,
    aiState: com.example.viewmodel.AiUiState,
    userSettings: UserSettings,
    aiChatState: AiChatUiState,
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
            currentUserId = viewModel.currentUserId,
            isGmMode = isGmMode,
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onSelectAiModel = { viewModel.setAiModel(it) },
            onSendAiChat = { viewModel.sendAiChatMessage(story, it, "player characters") },
            onSaveCharacter = { viewModel.saveCharacter(story, it) },
            onDeleteCharacters = { viewModel.deleteCharacters(story, it) },
            onImportAi = { raw, regex, done ->
                viewModel.importCharactersWithAi(story, raw, "player", regex, done)
            },
            onParseEquipmentAi = { raw, onParsed -> viewModel.parseEquipmentWithAi(raw, onParsed) },
            onResetAiState = { viewModel.resetAiState() }
        )
        StoryTabId.NPCS -> EntitiesTab(
            story = story,
            kind = "npc",
            characters = characters,
            currentUserId = viewModel.currentUserId,
            isGmMode = isGmMode,
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onSelectAiModel = { viewModel.setAiModel(it) },
            onSendAiChat = { viewModel.sendAiChatMessage(story, it, "NPCs") },
            onSaveCharacter = { viewModel.saveCharacter(story, it) },
            onDeleteCharacters = { viewModel.deleteCharacters(story, it) },
            onImportAi = { raw, regex, done ->
                viewModel.importCharactersWithAi(story, raw, "npc", regex, done)
            },
            onParseEquipmentAi = { raw, onParsed -> viewModel.parseEquipmentWithAi(raw, onParsed) },
            onResetAiState = { viewModel.resetAiState() }
        )
        StoryTabId.MONSTERS -> EntitiesTab(
            story = story,
            kind = "monster",
            characters = characters,
            currentUserId = viewModel.currentUserId,
            isGmMode = isGmMode,
            aiState = aiState,
            userSettings = userSettings,
            aiChatState = aiChatState,
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onSelectAiModel = { viewModel.setAiModel(it) },
            onSendAiChat = { viewModel.sendAiChatMessage(story, it, "monsters & encounters") },
            onSaveCharacter = { viewModel.saveCharacter(story, it) },
            onDeleteCharacters = { viewModel.deleteCharacters(story, it) },
            onImportAi = { raw, regex, done ->
                viewModel.importCharactersWithAi(story, raw, "monster", regex, done)
            },
            onParseEquipmentAi = { raw, onParsed -> viewModel.parseEquipmentWithAi(raw, onParsed) },
            onResetAiState = { viewModel.resetAiState() }
        )
        StoryTabId.MAP -> GridMapTab(
            story = story,
            characters = characters,
            quickAssets = quickAssets,
            currentUserId = viewModel.currentUserId,
            isGmMode = isGmMode,
            onSyncMap = { w, h, g, t, bg, imm ->
                viewModel.syncMapDebounced(story, w, h, g, t, bg, imm)
            },
            onAddQuickAsset = { viewModel.addQuickMapAsset(it) }
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
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onSelectAiModel = { viewModel.setAiModel(it) },
            onSendAiChat = { viewModel.sendAiChatMessage(story, it, "locations") },
            onSaveItem = { viewModel.saveWorldItem(story, it) },
            onReorder = { viewModel.reorderWorldItems(story, it) },
            onDeleteItems = { viewModel.deleteWorldItems(story, it) },
            onImportAi = { raw, start, regex, done ->
                viewModel.importWorldItemsWithAi(story, raw, "location", start, regex, done)
            },
            onResetAiState = { viewModel.resetAiState() }
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
            onSendAiChat = { viewModel.sendAiChatMessage(story, it, "powers & spells") },
            onSaveItem = { viewModel.saveWorldItem(story, it) },
            onReorder = { viewModel.reorderWorldItems(story, it) },
            onDeleteItems = { viewModel.deleteWorldItems(story, it) },
            onImportAi = { raw, start, regex, done ->
                viewModel.importWorldItemsWithAi(story, raw, "power", start, regex, done)
            },
            onResetAiState = { viewModel.resetAiState() }
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
            onSendAiChat = { viewModel.sendAiChatMessage(story, it, "campaign notes") },
            onSaveItem = { viewModel.saveWorldItem(story, it) },
            onReorder = { viewModel.reorderWorldItems(story, it) },
            onDeleteItems = { viewModel.deleteWorldItems(story, it) },
            onImportAi = { raw, start, regex, done ->
                viewModel.importWorldItemsWithAi(story, raw, "note", start, regex, done)
            },
            onResetAiState = { viewModel.resetAiState() }
        )
        StoryTabId.CHAT -> ChatTab(
            story = story,
            strings = strings,
            messages = messages,
            currentUserId = viewModel.currentUserId,
            activeAiModelLabel = userSettings.aiModel,
            onOpenAiChat = onOpenAiChat,
            onSendMessage = { viewModel.sendChatMessage(story, it) }
        )
        StoryTabId.LOGS -> LogsTab(logs = logs)
    }
}
