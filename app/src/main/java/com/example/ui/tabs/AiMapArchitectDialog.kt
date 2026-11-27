package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.AiProvider
import com.example.data.GridCell
import com.example.data.MapColorPalette
import com.example.data.UserSettings
import com.example.ui.components.AiModelSelectorBar
import com.example.viewmodel.AiMapUiState
import kotlin.math.roundToInt

private val AI_MAP_NEW_PRESETS = listOf(
    "🏰 Dungeon" to "Create a stone dungeon with 3 connected rooms, hallways, border walls, wooden doors 🚪, treasure chests 💎, and skeleton traps 💀.",
    "🍺 Tavern" to "Create a cozy wooden tavern interior with stone walls, checker wood floors, bar counter 🍺, dining tables 🪑, and a warm hearth 🔥.",
    "🌲 Forest River" to "Create a lush forest glade with a winding diagonal blue river 🌊, a wooden bridge across the middle, scattered pine trees 🌲, and a campfire 🔥.",
    "🌋 Lava Arena" to "Create an obsidian boss arena surrounded by glowing lava rivers 🔥, stone pillars 🪨, and a purple arcane summoning circle ✨ in the center.",
    "❄️ Frozen Shrine" to "Create a snow & glacier ice temple courtyard with icy walls ❄️, frozen pools, ancient pillars 🏛️, and a glowing shrine ✨.",
    "🏜️ Desert Oasis" to "Create a desert sandstone canyon with golden dunes, a central turquoise oasis lagoon 🌊, palm trees 🌴, and stone ruins 🏛️.",
)

private val AI_MAP_EDIT_PRESETS = listOf(
    "🌉 Add Bridge" to "Keep the current terrain and add a wooden bridge across the water or central hazard.",
    "💎 Secret Room" to "Carve and connect a secret treasure room on one side with stone walls, a hidden door 🚪, and chests 💎.",
    "🧱 Wall Border" to "Keep the interior intact and add sturdy stone border walls with 2 doorway openings around the map edges.",
    "🔥 Traps & Camp" to "Add a glowing campfire 🔥, scatter a few obstacles, and place hazard traps 🕳️ in strategic spots on the existing map.",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiMapArchitectDialog(
    currentWidth: Int,
    currentHeight: Int,
    hasExistingTiles: Boolean,
    userSettings: UserSettings,
    aiMapState: AiMapUiState,
    onSelectProvider: (AiProvider) -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectEndpointUrl: (String) -> Unit,
    onSelectApiKey: (String) -> Unit,
    onGenerate: (
        prompt: String,
        replaceEntireMap: Boolean,
        targetWidth: Int?,
        targetHeight: Int?,
    ) -> Unit,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    aiConfigActions: com.example.ui.components.AiProviderConfigActions? = null,
) {
    var prompt by remember { mutableStateOf("") }
    var replaceEntireMap by remember { mutableStateOf(!hasExistingTiles) }
    var useCustomSize by remember { mutableStateOf(false) }
    var sizePresetLabel by remember { mutableStateOf("Keep") }
    var draftW by remember { mutableIntStateOf(currentWidth) }
    var draftH by remember { mutableIntStateOf(currentHeight) }
    var showModelConfig by remember { mutableStateOf(false) }

    val isLoading = aiMapState is AiMapUiState.Loading
    val errorMsg = (aiMapState as? AiMapUiState.Error)?.message

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("AI Map Architect") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${userSettings.activeProviderLabel} • ${userSettings.aiModel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = showModelConfig,
                        onClick = { showModelConfig = !showModelConfig },
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        label = { Text("Model & API") },
                        modifier = Modifier.testTag("ai_map_toggle_model_button")
                    )
                }

                AnimatedVisibility(visible = showModelConfig) {
                    AiModelSelectorBar(
                        selectedProvider = userSettings.aiProvider,
                        selectedModel = userSettings.aiModel,
                        onSelectProvider = onSelectProvider,
                        onSelectModel = onSelectModel,
                        endpointUrl = userSettings.aiEndpointUrl,
                        apiKey = userSettings.aiApiKey,
                        onSelectEndpointUrl = onSelectEndpointUrl,
                        onSelectApiKey = onSelectApiKey,
                        compact = false,
                        showAdvancedApiConfig = true,
                        customProviders = userSettings.customAiProviders,
                        activeCustomProviderId = userSettings.activeCustomProviderId,
                        configActions = aiConfigActions,
                    )
                }

                Text(
                    text = "Describe the game map you want. The AI codes painting commands (rects, borders, lines, circles, fills, scatter, erasers) with any hex color & emoji.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Build mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !replaceEntireMap,
                        onClick = { replaceEntireMap = false },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Edit Current Map") },
                        modifier = Modifier.weight(1f).testTag("ai_map_mode_edit")
                    )
                    FilterChip(
                        selected = replaceEntireMap,
                        onClick = { replaceEntireMap = true },
                        leadingIcon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("New Map (Clear)") },
                        modifier = Modifier.weight(1f).testTag("ai_map_mode_new")
                    )
                }

                // Target Grid Size selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Grid Size",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val sizeOptions = listOf(
                            "Keep (${currentWidth}x$currentHeight)" to (null to null),
                            "12x12" to (12 to 12),
                            "16x14" to (16 to 14),
                            "20x16" to (20 to 16),
                            "24x18" to (24 to 18),
                        )
                        sizeOptions.forEach { (label, dims) ->
                            val shortKey = if (dims.first == null) "Keep" else label
                            FilterChip(
                                selected = !useCustomSize && sizePresetLabel == shortKey,
                                onClick = {
                                    useCustomSize = false
                                    sizePresetLabel = shortKey
                                    if (dims.first != null && dims.second != null) {
                                        draftW = dims.first!!
                                        draftH = dims.second!!
                                    } else {
                                        draftW = currentWidth
                                        draftH = currentHeight
                                    }
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        FilterChip(
                            selected = useCustomSize,
                            onClick = { useCustomSize = true },
                            label = { Text("Custom (${draftW}x$draftH)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    AnimatedVisibility(visible = useCustomSize) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Width: $draftW", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = draftW.toFloat(),
                                onValueChange = { draftW = it.roundToInt().coerceIn(4, 30) },
                                valueRange = 4f..30f,
                            )
                            Text("Height: $draftH", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = draftH.toFloat(),
                                onValueChange = { draftH = it.roundToInt().coerceIn(4, 30) },
                                valueRange = 4f..30f,
                            )
                        }
                    }
                }

                // Quick Prompt Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (replaceEntireMap) "Quick Map Ideas" else "Quick Edit Ideas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val presets = if (replaceEntireMap || !hasExistingTiles) AI_MAP_NEW_PRESETS else AI_MAP_EDIT_PRESETS
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        presets.forEach { (label, fullText) ->
                            FilterChip(
                                selected = prompt == fullText,
                                onClick = {
                                    prompt = fullText
                                    onClearError()
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = {
                        prompt = it
                        if (errorMsg != null) onClearError()
                    },
                    label = { Text("Map instructions for AI") },
                    placeholder = {
                        Text(
                            if (replaceEntireMap) {
                                "e.g., Haunted crypt with 3 chambers, purple arcane altar ✨, bone piles 💀, and stone corridors..."
                            } else {
                                "e.g., Add a lava moat along the east side and a stone drawbridge at row 6..."
                            }
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().testTag("ai_map_prompt_input")
                )

                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Coding painting tools with ${userSettings.aiModel}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (errorMsg != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetW = if (useCustomSize || sizePresetLabel != "Keep") draftW else null
                    val targetH = if (useCustomSize || sizePresetLabel != "Keep") draftH else null
                    onGenerate(prompt.trim(), replaceEntireMap, targetW, targetH)
                },
                enabled = prompt.isNotBlank() && !isLoading,
                modifier = Modifier.testTag("ai_map_generate_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (replaceEntireMap) "Generate Map" else "Apply AI Edit")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
                modifier = Modifier.testTag("ai_map_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiMapCodeViewerDialog(
    summary: String,
    scriptJson: String,
    grid: Map<String, GridCell>,
    onPickBrushBlock: (colorHex: String?, emoji: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val uniqueBlocks = remember(grid) {
        grid.values
            .filter { it.colorHex != null || it.emoji != null }
            .distinctBy { "${it.colorHex.orEmpty()}|${it.emoji.orEmpty()}" }
            .take(16)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Code, contentDescription = null) },
        title = { Text("AI Map Painting Code") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (uniqueBlocks.isNotEmpty()) {
                    Text(
                        text = "Blocks painted by AI (tap to load into brush):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uniqueBlocks.forEach { cell ->
                            val bg = cell.colorHex?.let { hex ->
                                runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
                            } ?: MaterialTheme.colorScheme.surfaceVariant
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(bg)
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), MaterialTheme.shapes.small)
                                    .clickable {
                                        onPickBrushBlock(cell.colorHex, cell.emoji)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell.emoji != null) {
                                    Text(cell.emoji)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (copied) "Copied code JSON!" else "Executed Tool Script",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(scriptJson))
                            copied = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy map code", modifier = Modifier.size(16.dp))
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = scriptJson.ifBlank { "{}" },
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(10.dp)
                    )
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
fun ExpandedColorPaletteDialog(
    currentColorHex: String?,
    onSelectColor: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var hexInput by remember(currentColorHex) {
        mutableStateOf(currentColorHex ?: "#334155")
    }
    val normalizedPreview = remember(hexInput) {
        MapColorPalette.normalizeColorHex(hexInput)
    }
    val previewColor = remember(normalizedPreview) {
        normalizedPreview?.let {
            runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Palette, contentDescription = null) },
        title = { Text("Block Color Palette & Custom Hex") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(previewColor ?: MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.5.dp, Color.White.copy(alpha = 0.45f), MaterialTheme.shapes.small)
                    )
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { hexInput = it },
                        label = { Text("Any Hex Color (#RRGGBB)") },
                        placeholder = { Text("#7C3AED") },
                        singleLine = true,
                        isError = hexInput.isNotBlank() && previewColor == null,
                        modifier = Modifier.weight(1f).testTag("map_custom_hex_input")
                    )
                    Button(
                        onClick = {
                            normalizedPreview?.let {
                                onSelectColor(it)
                                onDismiss()
                            }
                        },
                        enabled = previewColor != null,
                        modifier = Modifier.testTag("map_apply_custom_hex_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Use custom color", modifier = Modifier.size(16.dp))
                    }
                }

                MapColorPalette.swatches.groupBy { it.category }.forEach { (category, swatches) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            swatches.forEach { swatch ->
                                val c = Color(android.graphics.Color.parseColor(swatch.hex))
                                val isSelected = currentColorHex.equals(swatch.hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(c)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.35f),
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .clickable {
                                            onSelectColor(swatch.hex)
                                            onDismiss()
                                        }
                                        .testTag("palette_swatch_${swatch.hex}")
                                )
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
