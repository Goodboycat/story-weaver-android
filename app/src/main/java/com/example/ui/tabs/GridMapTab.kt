package com.example.ui.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonPinCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiProvider
import com.example.data.Character
import com.example.data.GridCell
import com.example.data.JsonCodecs
import com.example.data.MapBackgroundLayer
import com.example.data.MapColorPalette
import com.example.data.MapPaintOutcome
import com.example.data.MapToken
import com.example.data.QuickMapAsset
import com.example.data.QuickMapBlock
import com.example.data.Story
import com.example.data.UserSettings
import com.example.data.sanitizeMapEmoji
import com.example.ui.components.AllEmojiPickerDialog
import com.example.ui.components.IdeaGuideDialog
import com.example.ui.components.ImagePickerUtil
import com.example.viewmodel.AiMapUiState
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class MapTool {
    PAN_ZOOM,
    BRUSH,
    ERASER,
    FLOOD_FILL,
    RECT_FILL,
    MOVE_TOKEN,
    ARRANGE_IMAGES,
}

private data class AiMapUndoSnapshot(
    val width: Int,
    val height: Int,
    val grid: Map<String, GridCell>,
    val tokens: List<MapToken>,
    val summary: String,
    val scriptJson: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GridMapTab(
    story: Story,
    characters: List<Character>,
    quickAssets: List<QuickMapAsset>,
    quickBlocks: List<QuickMapBlock>,
    currentUserId: String,
    isGmMode: Boolean,
    userSettings: UserSettings,
    aiMapState: AiMapUiState,
    onSelectAiProvider: (AiProvider) -> Unit,
    onSelectAiModel: (String) -> Unit,
    onSelectAiEndpointUrl: (String) -> Unit,
    onSelectAiApiKey: (String) -> Unit,
    onClearAiMapError: () -> Unit,
    onGenerateAiMap: (
        prompt: String,
        currentWidth: Int,
        currentHeight: Int,
        currentGrid: Map<String, GridCell>,
        currentTokens: List<MapToken>,
        replaceEntireMap: Boolean,
        targetWidth: Int?,
        targetHeight: Int?,
        onApplied: (MapPaintOutcome) -> Unit,
    ) -> Unit,
    onSyncMap: (width: Int, height: Int, grid: Map<String, GridCell>, tokens: List<MapToken>, backgroundUrl: String?, immediate: Boolean) -> Unit,
    onAddQuickAsset: (QuickMapAsset) -> Unit,
    onAddQuickBlock: (QuickMapBlock) -> Unit,
    aiConfigActions: com.example.ui.components.AiProviderConfigActions? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var width by remember(story.id, story.mapWidth) { mutableIntStateOf(story.mapWidth.toInt().coerceIn(4, 30)) }
    var height by remember(story.id, story.mapHeight) { mutableIntStateOf(story.mapHeight.toInt().coerceIn(4, 30)) }
    var grid by remember(story.id, story.mapGridJson) { mutableStateOf(JsonCodecs.decodeGrid(story.mapGridJson)) }
    var tokens by remember(story.id, story.mapTokensJson) { mutableStateOf(JsonCodecs.decodeTokens(story.mapTokensJson)) }
    var bgLayers by remember(story.id, story.mapBackgroundUrl) {
        mutableStateOf(
            JsonCodecs.decodeMapBackgroundLayers(
                story.mapBackgroundUrl,
                story.mapWidth.toInt().coerceIn(4, 30),
                story.mapHeight.toInt().coerceIn(4, 30)
            )
        )
    }

    var activeTool by remember(isGmMode) {
        mutableStateOf(if (isGmMode) MapTool.PAN_ZOOM else MapTool.MOVE_TOKEN)
    }
    var brushSize by remember { mutableIntStateOf(1) }
    var selectedColorHex by remember { mutableStateOf<String?>("#334155") }
    var selectedEmoji by remember { mutableStateOf<String?>(null) }
    var selectedAssetBase64 by remember { mutableStateOf<String?>(null) }
    var rectStart by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectedTokenId by remember { mutableStateOf<String?>(null) }
    var selectedBgLayerId by remember { mutableStateOf<String?>(null) }

    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var showMapSettingsDialog by remember { mutableStateOf(false) }
    var showAiMapDialog by remember { mutableStateOf(false) }
    var showAiCodeDialog by remember { mutableStateOf(false) }
    var showColorPaletteDialog by remember { mutableStateOf(false) }
    var aiUndoSnapshot by remember { mutableStateOf<AiMapUndoSnapshot?>(null) }
    var showTokenPicker by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var showMapEmojiPicker by remember { mutableStateOf(false) }

    val tileAssetPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val b64 = ImagePickerUtil.uriToResizedBase64DataUrl(context, uri, maxDimension = 128, quality = 74)
                if (b64 != null) {
                    val asset = QuickMapAsset(
                        id = "qa_${UUID.randomUUID().toString().take(8)}",
                        label = "Tile",
                        imageBase64 = b64
                    )
                    onAddQuickAsset(asset)
                    selectedAssetBase64 = b64
                    selectedEmoji = null
                    activeTool = MapTool.BRUSH
                }
            }
        }
    }

    fun encodedBackground(): String? = JsonCodecs.encodeMapBackgroundLayers(bgLayers)

    fun currentBrushCell(): GridCell = GridCell(
        colorHex = selectedColorHex,
        emoji = selectedEmoji,
        imageBase64 = selectedAssetBase64,
    )

    fun saveCurrentComboToQuickInventory() {
        if (selectedColorHex == null && selectedEmoji == null && selectedAssetBase64 == null) return
        onAddQuickBlock(
            QuickMapBlock(
                id = "qb_${UUID.randomUUID().toString().take(8)}",
                label = selectedEmoji ?: selectedColorHex.orEmpty(),
                colorHex = selectedColorHex,
                emoji = selectedEmoji,
                imageBase64 = selectedAssetBase64,
            )
        )
    }

    fun cellKey(x: Int, y: Int) = "$x,$y"

    fun applyPaint(cx: Int, cy: Int, recordQuickBlock: Boolean = true) {
        val next = grid.toMutableMap()
        val half = brushSize / 2
        val startX = cx - half
        val startY = cy - half
        val endX = startX + brushSize - 1
        val endY = startY + brushSize - 1
        val brushCell = currentBrushCell()
        var changed = false
        for (px in startX..endX) {
            for (py in startY..endY) {
                if (px in 0 until width && py in 0 until height) {
                    val key = cellKey(px, py)
                    if (activeTool == MapTool.ERASER) {
                        if (next.remove(key) != null) changed = true
                    } else {
                        if (next[key] != brushCell) {
                            next[key] = brushCell
                            changed = true
                        }
                    }
                }
            }
        }
        if (changed) {
            if (activeTool != MapTool.ERASER && recordQuickBlock) {
                saveCurrentComboToQuickInventory()
            }
            grid = next
            onSyncMap(width, height, next, tokens, encodedBackground(), false)
        }
    }

    fun applyFloodFill(sx: Int, sy: Int) {
        val startKey = cellKey(sx, sy)
        val targetCell = grid[startKey]
        val replacement = currentBrushCell()
        if (targetCell == replacement) return
        saveCurrentComboToQuickInventory()
        val next = grid.toMutableMap()
        val queue = ArrayDeque<Pair<Int, Int>>()
        val visited = HashSet<String>()
        queue.add(sx to sy)
        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            val key = cellKey(x, y)
            if (x !in 0 until width || y !in 0 until height || key in visited) continue
            if (next[key] != targetCell) continue
            visited.add(key)
            next[key] = replacement
            queue.add(x + 1 to y)
            queue.add(x - 1 to y)
            queue.add(x to y + 1)
            queue.add(x to y - 1)
        }
        grid = next
        onSyncMap(width, height, next, tokens, encodedBackground(), false)
    }

    fun applyRectFill(a: Pair<Int, Int>, b: Pair<Int, Int>) {
        saveCurrentComboToQuickInventory()
        val next = grid.toMutableMap()
        val minX = min(a.first, b.first).coerceIn(0, width - 1)
        val maxX = max(a.first, b.first).coerceIn(0, width - 1)
        val minY = min(a.second, b.second).coerceIn(0, height - 1)
        val maxY = max(a.second, b.second).coerceIn(0, height - 1)
        val cell = currentBrushCell()
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                next[cellKey(x, y)] = cell
            }
        }
        grid = next
        onSyncMap(width, height, next, tokens, encodedBackground(), false)
    }

    fun handleGridTap(gx: Int, gy: Int, localCellFloatX: Float = gx.toFloat(), localCellFloatY: Float = gy.toFloat()) {
        if (activeTool == MapTool.ARRANGE_IMAGES && isGmMode) {
            val hit = bgLayers.lastOrNull { layer ->
                localCellFloatX in layer.gridX..(layer.gridX + layer.widthCells) &&
                    localCellFloatY in layer.gridY..(layer.gridY + layer.heightCells)
            }
            selectedBgLayerId = hit?.id
            return
        }
        if (gx !in 0 until width || gy !in 0 until height) return
        when (activeTool) {
            MapTool.BRUSH, MapTool.ERASER -> if (isGmMode) applyPaint(gx, gy)
            MapTool.FLOOD_FILL -> if (isGmMode) applyFloodFill(gx, gy)
            MapTool.RECT_FILL -> if (isGmMode) {
                val start = rectStart
                if (start == null) {
                    rectStart = gx to gy
                } else {
                    applyRectFill(start, gx to gy)
                    rectStart = null
                }
            }
            MapTool.MOVE_TOKEN -> {
                val tappedToken = tokens.firstOrNull { it.x == gx && it.y == gy }
                if (tappedToken != null) {
                    val canMove = isGmMode || tappedToken.ownerId == currentUserId
                    if (canMove) {
                        selectedTokenId = if (selectedTokenId == tappedToken.id) null else tappedToken.id
                    }
                } else if (selectedTokenId != null) {
                    val updated = tokens.map { t ->
                        if (t.id == selectedTokenId) t.copy(x = gx, y = gy) else t
                    }
                    tokens = updated
                    selectedTokenId = null
                    onSyncMap(width, height, grid, updated, encodedBackground(), true)
                }
            }
            MapTool.PAN_ZOOM -> {
                val tappedToken = tokens.firstOrNull { it.x == gx && it.y == gy }
                if (tappedToken != null && (isGmMode || tappedToken.ownerId == currentUserId)) {
                    selectedTokenId = tappedToken.id
                    activeTool = MapTool.MOVE_TOKEN
                }
            }
            MapTool.ARRANGE_IMAGES -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Map Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = activeTool == MapTool.PAN_ZOOM,
                onClick = { activeTool = MapTool.PAN_ZOOM },
                leadingIcon = { Icon(Icons.Default.OpenWith, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Pan") },
                modifier = Modifier.testTag("map_tool_pan")
            )
            FilterChip(
                selected = activeTool == MapTool.MOVE_TOKEN,
                onClick = { activeTool = MapTool.MOVE_TOKEN },
                leadingIcon = { Icon(Icons.Default.PersonPinCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Tokens") },
                modifier = Modifier.testTag("map_tool_tokens")
            )
            if (isGmMode) {
                FilterChip(
                    selected = activeTool == MapTool.BRUSH,
                    onClick = { activeTool = MapTool.BRUSH },
                    leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Paint") },
                    modifier = Modifier.testTag("map_tool_brush")
                )
                FilterChip(
                    selected = activeTool == MapTool.ERASER,
                    onClick = { activeTool = MapTool.ERASER },
                    leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Erase") },
                    modifier = Modifier.testTag("map_tool_eraser")
                )
                FilterChip(
                    selected = activeTool == MapTool.FLOOD_FILL,
                    onClick = { activeTool = MapTool.FLOOD_FILL },
                    leadingIcon = { Icon(Icons.Default.FormatColorFill, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Fill") },
                    modifier = Modifier.testTag("map_tool_fill")
                )
                FilterChip(
                    selected = activeTool == MapTool.RECT_FILL,
                    onClick = { activeTool = MapTool.RECT_FILL; rectStart = null },
                    leadingIcon = { Icon(Icons.Default.CropSquare, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(if (rectStart == null) "Rect" else "Tap End") },
                    modifier = Modifier.testTag("map_tool_rect")
                )
                if (bgLayers.isNotEmpty()) {
                    FilterChip(
                        selected = activeTool == MapTool.ARRANGE_IMAGES,
                        onClick = {
                            activeTool = MapTool.ARRANGE_IMAGES
                            if (selectedBgLayerId == null) selectedBgLayerId = bgLayers.lastOrNull()?.id
                        },
                        leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Move Map Img") },
                        modifier = Modifier.testTag("map_tool_arrange_images")
                    )
                }
                Button(
                    onClick = {
                        onClearAiMapError()
                        showAiMapDialog = true
                    },
                    modifier = Modifier.testTag("map_ai_architect_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AI Map")
                }
                OutlinedButton(
                    onClick = { showMapSettingsDialog = true },
                    modifier = Modifier.testTag("map_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Map (${width}x$height)")
                }
                IconButton(
                    onClick = {
                        grid = emptyMap()
                        onSyncMap(width, height, emptyMap(), tokens, encodedBackground(), true)
                    },
                    modifier = Modifier.testTag("map_clear_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Grid")
                }
            }
            OutlinedButton(
                onClick = { showTokenPicker = true },
                modifier = Modifier.testTag("map_add_token_button")
            ) {
                Text("+ Token")
            }
            IconButton(onClick = { showGuide = true }) {
                Icon(Icons.Default.Lightbulb, contentDescription = "Map Guide")
            }
        }

        // GM Composite Block Painter Bar: Block Color (Bg) + Emoji/Image (Fg)
        if (isGmMode && (activeTool == MapTool.BRUSH || activeTool == MapTool.FLOOD_FILL || activeTool == MapTool.RECT_FILL)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1: Preview + Brush Size + Background Block Color Palette
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Live Composite Block Preview
                        CompositeBlockPreview(
                            colorHex = selectedColorHex,
                            emoji = selectedEmoji,
                            imageBase64 = selectedAssetBase64,
                            sizeDp = 38.dp,
                            isSelected = true,
                            onClick = { saveCurrentComboToQuickInventory() },
                            modifier = Modifier.testTag("map_composite_block_preview")
                        )
                        OutlinedButton(
                            onClick = { saveCurrentComboToQuickInventory() },
                            enabled = selectedColorHex != null || selectedEmoji != null || selectedAssetBase64 != null,
                            modifier = Modifier.testTag("map_save_quick_block_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Block", style = MaterialTheme.typography.labelSmall)
                        }

                        listOf(1, 2, 3).forEach { sz ->
                            FilterChip(
                                selected = brushSize == sz,
                                onClick = { brushSize = sz },
                                label = { Text("${sz}x$sz") }
                            )
                        }

                        // Background Block Color "None" swatch
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = if (selectedColorHex == null) 2.5.dp else 1.dp,
                                    color = if (selectedColorHex == null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .clickable { selectedColorHex = null }
                                .testTag("map_bg_color_none"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = "Transparent Block Background",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilterChip(
                            selected = selectedColorHex != null && MapColorPalette.hexList.none { it.equals(selectedColorHex, ignoreCase = true) },
                            onClick = { showColorPaletteDialog = true },
                            leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(15.dp)) },
                            label = {
                                Text(
                                    text = if (selectedColorHex != null && MapColorPalette.hexList.none { it.equals(selectedColorHex, ignoreCase = true) }) {
                                        selectedColorHex!!
                                    } else {
                                        "All Colors"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("map_open_palette_dialog_button")
                        )

                        MapColorPalette.hexList.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val selected = selectedColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(color)
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .clickable { selectedColorHex = hex }
                                    .testTag("map_bg_color_$hex")
                            )
                        }
                    }

                    // Row 2: Overlay Emoji OR Tile Image (rendered ABOVE selected background color)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overlay:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = selectedEmoji == null && selectedAssetBase64 == null,
                            onClick = {
                                selectedEmoji = null
                                selectedAssetBase64 = null
                            },
                            label = { Text("Color Only", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("map_overlay_none")
                        )
                        FilterChip(
                            selected = selectedEmoji != null && selectedAssetBase64 == null,
                            onClick = { showMapEmojiPicker = true },
                            leadingIcon = {
                                Icon(Icons.Default.EmojiEmotions, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = {
                                Text(
                                    text = if (selectedEmoji != null && selectedAssetBase64 == null) "Emoji: $selectedEmoji" else "All Emojis",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("map_open_all_emojis_button")
                        )
                        quickAssets.forEach { asset ->
                            val bmp = remember(asset.imageBase64) { ImagePickerUtil.decodeDataUrlBitmap(asset.imageBase64) }
                            if (bmp != null) {
                                val selected = selectedAssetBase64 == asset.imageBase64
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = asset.label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .clickable {
                                            selectedAssetBase64 = if (selected) null else asset.imageBase64
                                            selectedEmoji = null
                                        }
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                tileAssetPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.size(36.dp).testTag("map_upload_tile_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload Overlay Image", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        if (activeTool == MapTool.ARRANGE_IMAGES && isGmMode) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Drag any uploaded map image to position it on the grid. Use Map (${width}x$height) to resize.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showMapSettingsDialog = true }) {
                        Text("Layout")
                    }
                }
            }
        }

        if (aiUndoSnapshot != null && isGmMode) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth().testTag("map_ai_result_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = aiUndoSnapshot!!.summary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { showAiCodeDialog = true },
                        modifier = Modifier.testTag("map_ai_view_code_button")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Code")
                    }
                    TextButton(
                        onClick = {
                            val snap = aiUndoSnapshot ?: return@TextButton
                            width = snap.width
                            height = snap.height
                            grid = snap.grid
                            tokens = snap.tokens
                            aiUndoSnapshot = null
                            onSyncMap(snap.width, snap.height, snap.grid, snap.tokens, encodedBackground(), true)
                        },
                        modifier = Modifier.testTag("map_ai_undo_button")
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Undo")
                    }
                    TextButton(onClick = { aiUndoSnapshot = null }) {
                        Text("OK")
                    }
                }
            }
        }

        if (selectedTokenId != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Token selected — tap any tile to move it there", style = MaterialTheme.typography.bodySmall)
                    if (isGmMode) {
                        TextButton(onClick = {
                            val updated = tokens.filterNot { it.id == selectedTokenId }
                            tokens = updated
                            selectedTokenId = null
                            onSyncMap(width, height, grid, updated, encodedBackground(), true)
                        }) {
                            Text("Remove Token")
                        }
                    }
                }
            }
        }

        // Interactive Grid Canvas
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clipToBounds()
        ) {
            val density = LocalDensity.current
            val baseCellPx = with(density) { 36.dp.toPx() }
            val boardWidthPx = width * baseCellPx
            val boardHeightPx = height * baseCellPx

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(activeTool) {
                        if (activeTool == MapTool.PAN_ZOOM) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.6f, 2.8f)
                                panOffset = Offset(
                                    (panOffset.x + pan.x).coerceIn(-boardWidthPx, boardWidthPx),
                                    (panOffset.y + pan.y).coerceIn(-boardHeightPx, boardHeightPx)
                                )
                            }
                        }
                    }
                    .pointerInput(activeTool, isGmMode, width, height, selectedColorHex, selectedEmoji, selectedAssetBase64, brushSize, scale, panOffset, bgLayers, selectedBgLayerId) {
                        if (isGmMode && (activeTool == MapTool.BRUSH || activeTool == MapTool.ERASER || activeTool == MapTool.ARRANGE_IMAGES)) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val localX = (startOffset.x - panOffset.x) / scale
                                    val localY = (startOffset.y - panOffset.y) / scale
                                    val cfX = localX / baseCellPx
                                    val cfY = localY / baseCellPx
                                    if (activeTool == MapTool.ARRANGE_IMAGES) {
                                        val hit = bgLayers.lastOrNull { layer ->
                                            cfX in layer.gridX..(layer.gridX + layer.widthCells) &&
                                                cfY in layer.gridY..(layer.gridY + layer.heightCells)
                                        }
                                        if (hit != null) selectedBgLayerId = hit.id
                                    } else if (localX >= 0f && localY >= 0f) {
                                        applyPaint(cfX.toInt(), cfY.toInt(), recordQuickBlock = true)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (activeTool == MapTool.ARRANGE_IMAGES) {
                                        val activeId = selectedBgLayerId ?: bgLayers.lastOrNull()?.id
                                        if (activeId != null) {
                                            val dxCells = dragAmount.x / (baseCellPx * scale)
                                            val dyCells = dragAmount.y / (baseCellPx * scale)
                                            bgLayers = bgLayers.map { layer ->
                                                if (layer.id == activeId) {
                                                    layer.copy(
                                                        gridX = (layer.gridX + dxCells).coerceIn(-10f, width.toFloat()),
                                                        gridY = (layer.gridY + dyCells).coerceIn(-10f, height.toFloat())
                                                    )
                                                } else layer
                                            }
                                        }
                                    } else {
                                        val localX = (change.position.x - panOffset.x) / scale
                                        val localY = (change.position.y - panOffset.y) / scale
                                        if (localX >= 0f && localY >= 0f) {
                                            applyPaint((localX / baseCellPx).toInt(), (localY / baseCellPx).toInt(), recordQuickBlock = false)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (activeTool == MapTool.ARRANGE_IMAGES) {
                                        onSyncMap(width, height, grid, tokens, encodedBackground(), true)
                                    }
                                }
                            )
                        }
                    }
                    .pointerInput(activeTool, width, height, grid, tokens, selectedTokenId, scale, panOffset, bgLayers) {
                        detectTapGestures { tapOffset ->
                            val localX = (tapOffset.x - panOffset.x) / scale
                            val localY = (tapOffset.y - panOffset.y) / scale
                            val cfX = localX / baseCellPx
                            val cfY = localY / baseCellPx
                            if (localX >= 0f && localY >= 0f) {
                                handleGridTap(cfX.toInt(), cfY.toInt(), cfX, cfY)
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = panOffset.x
                            translationY = panOffset.y
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                        }
                        .size(with(density) { boardWidthPx.toDp() }, with(density) { boardHeightPx.toDp() })
                ) {
                    // Multi-image background layers arranged on the grid
                    bgLayers.forEach { layer ->
                        val bmp = remember(layer.imageBase64) { ImagePickerUtil.decodeDataUrlBitmap(layer.imageBase64) }
                        if (bmp != null) {
                            val isSelectedLayer = activeTool == MapTool.ARRANGE_IMAGES && selectedBgLayerId == layer.id
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = layer.label,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (layer.gridX * baseCellPx).roundToInt(),
                                            (layer.gridY * baseCellPx).roundToInt()
                                        )
                                    }
                                    .size(
                                        width = with(density) { (layer.widthCells * baseCellPx).toDp() },
                                        height = with(density) { (layer.heightCells * baseCellPx).toDp() }
                                    )
                                    .alpha(layer.opacity)
                                    .then(
                                        if (isSelectedLayer) {
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }

                    // Painted Background Color Tiles & Grid Lines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (x in 0 until width) {
                            for (y in 0 until height) {
                                val cell = grid[cellKey(x, y)]
                                if (cell?.colorHex != null) {
                                    val c = try {
                                        Color(android.graphics.Color.parseColor(cell.colorHex))
                                    } catch (_: Exception) {
                                        Color.DarkGray
                                    }
                                    drawRect(
                                        color = c.copy(alpha = 0.88f),
                                        topLeft = Offset(x * baseCellPx, y * baseCellPx),
                                        size = Size(baseCellPx, baseCellPx)
                                    )
                                }
                                drawRect(
                                    color = Color.White.copy(alpha = 0.16f),
                                    topLeft = Offset(x * baseCellPx, y * baseCellPx),
                                    size = Size(baseCellPx, baseCellPx),
                                    style = Stroke(width = 1f)
                                )
                            }
                        }
                        rectStart?.let { (rx, ry) ->
                            drawRect(
                                color = Color.Yellow,
                                topLeft = Offset(rx * baseCellPx, ry * baseCellPx),
                                size = Size(baseCellPx, baseCellPx),
                                style = Stroke(width = 4f)
                            )
                        }
                    }

                    // Cell Foreground Overlays (Emoji or Image ABOVE the Background Block Color)
                    grid.forEach { (key, cell) ->
                        val parts = key.split(",")
                        val gx = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
                        val gy = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach
                        if (gx !in 0 until width || gy !in 0 until height) return@forEach
                        val cleanCellEmoji = remember(cell.emoji) { sanitizeMapEmoji(cell.emoji) }
                        if (cell.imageBase64 != null || cleanCellEmoji != null) {
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((gx * baseCellPx).roundToInt(), (gy * baseCellPx).roundToInt()) }
                                    .size(with(density) { baseCellPx.toDp() }),
                                contentAlignment = Alignment.Center
                            ) {
                                val bmp = remember(cell.imageBase64) { ImagePickerUtil.decodeDataUrlBitmap(cell.imageBase64) }
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(if (cell.colorHex != null) 3.dp else 0.dp)
                                            .clip(MaterialTheme.shapes.extraSmall)
                                    )
                                } else if (cleanCellEmoji != null) {
                                    Text(text = cleanCellEmoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    // Character / Creature Tokens
                    tokens.forEach { token ->
                        if (token.x in 0 until width && token.y in 0 until height) {
                            val isSelected = token.id == selectedTokenId
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .offset { IntOffset((token.x * baseCellPx).roundToInt(), (token.y * baseCellPx).roundToInt()) }
                                    .size(with(density) { baseCellPx.toDp() })
                                    .padding(2.dp)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (token.kind == "monster") Color(0xFFF43F5E)
                                        else if (token.kind == "npc") Color(0xFF38BDF8)
                                        else Color(0xFFF59E0B),
                                        shape = CircleShape
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(token.emoji)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Block Inventory Bar docked below the map (rolls off oldest block when adding a 7th)
        if (isGmMode) {
            HorizontalDivider()
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_quick_block_inventory_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.padding(end = 4.dp)) {
                        Text(
                            text = "Quick Blocks (${quickBlocks.size}/6)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Oldest drops at 6",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            saveCurrentComboToQuickInventory()
                            activeTool = MapTool.BRUSH
                        },
                        enabled = selectedColorHex != null || selectedEmoji != null || selectedAssetBase64 != null,
                        modifier = Modifier.testTag("map_quick_inventory_add_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }

                    if (quickBlocks.isEmpty()) {
                        Text(
                            text = "Paint or tap + Add to store composite color+emoji blocks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        quickBlocks.forEachIndexed { idx, block ->
                            val isActive = block.colorHex == selectedColorHex &&
                                block.emoji == selectedEmoji &&
                                block.imageBase64 == selectedAssetBase64 &&
                                (activeTool == MapTool.BRUSH || activeTool == MapTool.FLOOD_FILL || activeTool == MapTool.RECT_FILL)
                            CompositeBlockPreview(
                                colorHex = block.colorHex,
                                emoji = block.emoji,
                                imageBase64 = block.imageBase64,
                                sizeDp = 44.dp,
                                isSelected = isActive,
                                onClick = {
                                    selectedColorHex = block.colorHex
                                    selectedEmoji = block.emoji
                                    selectedAssetBase64 = block.imageBase64
                                    if (activeTool != MapTool.FLOOD_FILL && activeTool != MapTool.RECT_FILL) {
                                        activeTool = MapTool.BRUSH
                                    }
                                },
                                modifier = Modifier.testTag("map_quick_block_$idx")
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMapSettingsDialog) {
        MapSettingsDialog(
            initialWidth = width,
            initialHeight = height,
            initialLayers = bgLayers,
            onApply = { nw, nh, updatedLayers ->
                val cleanedGrid = grid.filterKeys { k ->
                    val p = k.split(",")
                    val x = p.getOrNull(0)?.toIntOrNull() ?: -1
                    val y = p.getOrNull(1)?.toIntOrNull() ?: -1
                    x in 0 until nw && y in 0 until nh
                }
                val cleanedTokens = tokens.map {
                    it.copy(x = it.x.coerceIn(0, nw - 1), y = it.y.coerceIn(0, nh - 1))
                }
                width = nw
                height = nh
                grid = cleanedGrid
                tokens = cleanedTokens
                bgLayers = updatedLayers
                onSyncMap(nw, nh, cleanedGrid, cleanedTokens, JsonCodecs.encodeMapBackgroundLayers(updatedLayers), true)
                showMapSettingsDialog = false
            },
            onArrangeOnCanvas = { nw, nh, updatedLayers ->
                val cleanedGrid = grid.filterKeys { k ->
                    val p = k.split(",")
                    val x = p.getOrNull(0)?.toIntOrNull() ?: -1
                    val y = p.getOrNull(1)?.toIntOrNull() ?: -1
                    x in 0 until nw && y in 0 until nh
                }
                val cleanedTokens = tokens.map {
                    it.copy(x = it.x.coerceIn(0, nw - 1), y = it.y.coerceIn(0, nh - 1))
                }
                width = nw
                height = nh
                grid = cleanedGrid
                tokens = cleanedTokens
                bgLayers = updatedLayers
                selectedBgLayerId = updatedLayers.lastOrNull()?.id
                onSyncMap(nw, nh, cleanedGrid, cleanedTokens, JsonCodecs.encodeMapBackgroundLayers(updatedLayers), true)
                showMapSettingsDialog = false
                activeTool = MapTool.ARRANGE_IMAGES
            },
            onDismiss = { showMapSettingsDialog = false }
        )
    }

    if (showTokenPicker) {
        val placeableChars = if (isGmMode) characters else characters.filter { it.ownerId == currentUserId }
        var customTokenName by remember { mutableStateOf("") }
        var customTokenEmoji by remember { mutableStateOf("🧙") }
        var showCustomTokenEmojiPicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTokenPicker = false },
            title = { Text("Place Token on Map") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { showCustomTokenEmojiPicker = true },
                            label = { Text(customTokenEmoji) },
                            modifier = Modifier.testTag("map_custom_token_emoji_button")
                        )
                        OutlinedTextField(
                            value = customTokenName,
                            onValueChange = { customTokenName = it },
                            label = { Text("Custom Token Label") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val newToken = MapToken(
                                    id = "tk_${UUID.randomUUID().toString().take(8)}",
                                    label = customTokenName.trim().ifBlank { "Marker" },
                                    emoji = customTokenEmoji,
                                    kind = "npc",
                                    ownerId = currentUserId,
                                    x = (width / 2).coerceIn(0, width - 1),
                                    y = (height / 2).coerceIn(0, height - 1),
                                )
                                val updated = tokens + newToken
                                tokens = updated
                                onSyncMap(width, height, grid, updated, encodedBackground(), true)
                                showTokenPicker = false
                            },
                            modifier = Modifier.testTag("map_add_custom_token_button")
                        ) { Text("Add") }
                    }

                    if (placeableChars.isEmpty()) {
                        Text("Or create a character sheet first to place named tokens.")
                    }
                    placeableChars.forEach { ch ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newToken = MapToken(
                                        id = "tk_${UUID.randomUUID().toString().take(8)}",
                                        label = ch.name,
                                        emoji = ch.avatarEmoji,
                                        kind = ch.kind,
                                        ownerId = ch.ownerId,
                                        x = (width / 2).coerceIn(0, width - 1),
                                        y = (height / 2).coerceIn(0, height - 1),
                                    )
                                    val updated = tokens + newToken
                                    tokens = updated
                                    onSyncMap(width, height, grid, updated, encodedBackground(), true)
                                    showTokenPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(ch.avatarEmoji)
                                Text("${ch.name} (${ch.kind})")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTokenPicker = false }) { Text("Close") }
            }
        )

        if (showCustomTokenEmojiPicker) {
            AllEmojiPickerDialog(
                currentEmoji = customTokenEmoji,
                title = "Select Token Emoji",
                onSelect = { picked -> customTokenEmoji = picked },
                onDismiss = { showCustomTokenEmojiPicker = false }
            )
        }
    }

    if (showMapEmojiPicker) {
        AllEmojiPickerDialog(
            currentEmoji = selectedEmoji ?: "🏰",
            title = "Select Terrain Emoji",
            onSelect = { picked ->
                selectedEmoji = picked
                selectedAssetBase64 = null
            },
            onDismiss = { showMapEmojiPicker = false }
        )
    }

    if (showAiMapDialog) {
        AiMapArchitectDialog(
            currentWidth = width,
            currentHeight = height,
            hasExistingTiles = grid.isNotEmpty(),
            userSettings = userSettings,
            aiMapState = aiMapState,
            onSelectProvider = onSelectAiProvider,
            onSelectModel = onSelectAiModel,
            onSelectEndpointUrl = onSelectAiEndpointUrl,
            onSelectApiKey = onSelectAiApiKey,
            onGenerate = { prompt, replaceEntireMap, targetW, targetH ->
                val prevW = width
                val prevH = height
                val prevGrid = grid
                val prevTokens = tokens
                onGenerateAiMap(
                    prompt,
                    prevW,
                    prevH,
                    prevGrid,
                    prevTokens,
                    replaceEntireMap,
                    targetW,
                    targetH,
                ) { outcome ->
                    aiUndoSnapshot = AiMapUndoSnapshot(
                        width = prevW,
                        height = prevH,
                        grid = prevGrid,
                        tokens = prevTokens,
                        summary = outcome.summary,
                        scriptJson = outcome.scriptJson,
                    )
                    width = outcome.width
                    height = outcome.height
                    grid = outcome.grid
                    tokens = outcome.tokens
                    showAiMapDialog = false
                    onSyncMap(
                        outcome.width,
                        outcome.height,
                        outcome.grid,
                        outcome.tokens,
                        encodedBackground(),
                        true,
                    )
                }
            },
            onClearError = onClearAiMapError,
            onDismiss = {
                onClearAiMapError()
                showAiMapDialog = false
            },
            aiConfigActions = aiConfigActions,
        )
    }

    if (showAiCodeDialog && aiUndoSnapshot != null) {
        AiMapCodeViewerDialog(
            summary = aiUndoSnapshot!!.summary,
            scriptJson = aiUndoSnapshot!!.scriptJson,
            grid = grid,
            onPickBrushBlock = { colorHex, emoji ->
                selectedColorHex = colorHex
                selectedEmoji = emoji
                selectedAssetBase64 = null
                activeTool = MapTool.BRUSH
            },
            onDismiss = { showAiCodeDialog = false }
        )
    }

    if (showColorPaletteDialog) {
        ExpandedColorPaletteDialog(
            currentColorHex = selectedColorHex,
            onSelectColor = { hex ->
                selectedColorHex = hex
                if (activeTool != MapTool.FLOOD_FILL && activeTool != MapTool.RECT_FILL) {
                    activeTool = MapTool.BRUSH
                }
            },
            onDismiss = { showColorPaletteDialog = false }
        )
    }

    if (showGuide) {
        IdeaGuideDialog(guideKey = "map", onDismiss = { showGuide = false })
    }
}

@Composable
private fun CompositeBlockPreview(
    colorHex: String?,
    emoji: String?,
    imageBase64: String?,
    sizeDp: androidx.compose.ui.unit.Dp,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = remember(colorHex) {
        if (colorHex != null) {
            runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
                .getOrElse { Color(0xFF334155) }
        } else {
            Color.Transparent
        }
    }
    val bmp = remember(imageBase64) { ImagePickerUtil.decodeDataUrlBitmap(imageBase64) }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(MaterialTheme.shapes.small)
            .background(if (colorHex != null) bgColor else MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val cleanEmoji = remember(emoji) { sanitizeMapEmoji(emoji) }
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Block image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (colorHex != null) 3.dp else 0.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
            )
        } else if (cleanEmoji != null) {
            Text(text = cleanEmoji, fontSize = 19.sp)
        }
    }
}
