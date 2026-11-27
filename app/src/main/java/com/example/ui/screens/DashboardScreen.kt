package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppStrings
import com.example.data.Story
import com.example.data.TextScalePreset
import com.example.data.ThemePreset
import com.example.data.UserSettings
import com.example.notifications.SessionNotificationScheduler
import com.example.viewmodel.DashboardViewModel
import com.example.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    strings: AppStrings,
    settings: UserSettings,
    currentUserId: String,
    userEmail: String?,
    onOpenStory: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var storyToDelete by remember { mutableStateOf<Story?>(null) }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(strings.appTitle, style = MaterialTheme.typography.headlineSmall)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = strings.settings)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Action Row: Create & Join Campaign
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showCreateDialog = true },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("create_campaign_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.createCampaign, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { showJoinDialog = true },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("join_campaign_button")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.joinCampaign, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                when (val state = storiesState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    is UiState.Success -> {
                        val allStories = state.data
                        val context = LocalContext.current
                        LaunchedEffect(allStories) {
                            SessionNotificationScheduler.syncAllCampaigns(context, allStories)
                        }
                        val gmStories = allStories.filter { it.gmUserId == currentUserId }
                        val playerStories = allStories.filter { it.gmUserId != currentUserId }

                        if (allStories.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(strings.campaignsTitle, style = MaterialTheme.typography.headlineSmall)
                                        Text(
                                            strings.emptyCampaigns,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (gmStories.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = strings.gmCampaigns,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    items(gmStories, key = { it.id }) { story ->
                                        CampaignCard(
                                            story = story,
                                            isGm = true,
                                            strings = strings,
                                            onOpen = { onOpenStory(story.id) },
                                            onDelete = { storyToDelete = story }
                                        )
                                    }
                                }

                                if (playerStories.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = strings.playerCampaigns,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                    items(playerStories, key = { it.id }) { story ->
                                        CampaignCard(
                                            story = story,
                                            isGm = false,
                                            strings = strings,
                                            onOpen = { onOpenStory(story.id) },
                                            onDelete = null
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

    if (showCreateDialog) {
        CreateCampaignDialog(
            strings = strings,
            isBusy = isBusy,
            onDismiss = { showCreateDialog = false },
            onCreate = { title, desc, sys ->
                viewModel.createStory(title, desc, sys) { id ->
                    showCreateDialog = false
                    onOpenStory(id)
                }
            }
        )
    }

    if (showJoinDialog) {
        JoinCampaignDialog(
            strings = strings,
            isBusy = isBusy,
            onDismiss = { showJoinDialog = false },
            onJoin = { code ->
                viewModel.joinStory(code) { id ->
                    showJoinDialog = false
                    onOpenStory(id)
                }
            }
        )
    }

    if (storyToDelete != null) {
        AlertDialog(
            onDismissRequest = { storyToDelete = null },
            title = { Text("Delete Campaign?") },
            text = { Text("Delete \"${storyToDelete!!.title}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStory(storyToDelete!!)
                        storyToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_story_button")
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { storyToDelete = null }) { Text(strings.cancel) }
            }
        )
    }

    if (showSettings) {
        SettingsSheet(
            strings = strings,
            settings = settings,
            userEmail = userEmail,
            onSelectTheme = { viewModel.setThemePreset(it) },
            onSelectAccent = { viewModel.setCustomAccent(it) },
            onSelectTextScale = { viewModel.setTextScale(it) },
            onSelectLanguage = { viewModel.setLanguage(it) },
            onSelectAiProvider = { viewModel.setAiProvider(it) },
            onSelectAiModel = { viewModel.setAiModel(it) },
            onSelectAiEndpointUrl = { viewModel.setAiEndpointUrl(it) },
            onSelectAiApiKey = { viewModel.setAiApiKey(it) },
            onSelectAiImageEndpointUrl = { viewModel.setAiImageEndpointUrl(it) },
            onSelectAiImageModel = { viewModel.setAiImageModel(it) },
            onSelectAiImageApiKey = { viewModel.setAiImageApiKey(it) },
            onSignOut = {
                showSettings = false
                onSignOut()
            },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun CampaignCard(
    story: Story,
    isGm: Boolean,
    strings: AppStrings,
    onOpen: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val clipboard = LocalClipboardManager.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("campaign_card_${story.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = story.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${story.systemName} • ${if (isGm) strings.gmMode else strings.viewAsPlayer}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isGm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_story_${story.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = strings.delete,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (story.description.isNotBlank()) {
                Text(
                    text = story.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (story.nextSessionAtMillis > System.currentTimeMillis()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Next Play: ${SessionNotificationScheduler.formatSessionDateTime(story.nextSessionAtMillis)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.clickable {
                        clipboard.setText(AnnotatedString(story.joinCode))
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${strings.joinCodeLabel}: ${story.joinCode}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Join Code",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${story.memberUids.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateCampaignDialog(
    strings: AppStrings,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, systemName: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var systemName by remember { mutableStateOf("D&D 5e") }
    val presets = listOf("D&D 5e", "Pathfinder 2e", "Call of Cthulhu", "Blades in the Dark", "Homebrew")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.createCampaign, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Campaign Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("create_story_title_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = systemName == preset,
                            onClick = { systemName = preset },
                            label = { Text(preset) }
                        )
                    }
                }
                OutlinedTextField(
                    value = systemName,
                    onValueChange = { systemName = it },
                    label = { Text("RPG System") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Premise / Hook") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("create_story_desc_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, description, systemName) },
                enabled = title.isNotBlank() && !isBusy,
                modifier = Modifier.testTag("confirm_create_story_button")
            ) {
                Text(strings.createCampaign)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun JoinCampaignDialog(
    strings: AppStrings,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onJoin: (code: String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.joinCampaign, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter the 6-character Join Code shared by your Game Master.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(20) },
                    label = { Text(strings.joinCodeLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("join_code_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(code) },
                enabled = code.trim().length >= 4 && !isBusy,
                modifier = Modifier.testTag("confirm_join_story_button")
            ) {
                Text(strings.joinCampaign)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
