package com.example.ui.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PersonPinCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.Character
import com.example.data.GridCell
import com.example.data.JsonCodecs
import com.example.data.MapToken
import com.example.data.QuickMapAsset
import com.example.data.Story
import com.example.ui.components.IdeaGuideDialog
import com.example.ui.components.ImagePickerUtil
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GridMapTab(
    story: Story,
    characters: List<Character>,
    quickAssets: List<QuickMapAsset>,
    currentUserId: String,
    isGmMode: Boolean,
    onSyncMap: (width: Int, height: Int, grid: Map<String, GridCell>, tokens: List<MapToken>, backgroundUrl: String?, immediate: Boolean) -> Unit,
    onAddQuickAsset: (QuickMapAsset) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var width by remember(story.id, story.mapWidth) { mutableIntStateOf(story.mapWidth.toInt().coerceIn(4, 30)) }
    var height by remember(story.id, story.mapHeight) { mutableIntStateOf(story.mapHeight.toInt().coerceIn(4, 30)) }
    var grid by remember(story.id, story.mapGridJson) { mutableStateOf(JsonCodecs.decodeGrid(story.mapGridJson)) }
    var tokens by remember(story.id, story.mapTokensJson) { mutableStateOf(JsonCodecs.decodeTokens(story.mapTokensJson)) }
    var backgroundUrl by remember(story.id, story.mapBackgroundUrl) { mutableStateOf(story.mapBackgroundUrl) }

    var activeTool by remember(isGmMode) {
        mutableStateOf(if (isGmMode) MapTool.PAN_ZOOM else MapTool.MOVE_TOKEN)
    }
    var brushSize by remember { mutableIntStateOf(1) }
    var selectedColorHex by remember { mutableStateOf<String?>("#334155") }
    var selectedEmoji by remember { mutableStateOf<String?>(null) }
    var selectedAssetBase64 by remember { mutableStateOf<String?>(null) }
    var rectStart by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var selectedTokenId by remember { mutableStateOf<String?>(null) }

    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var showResizeDialog by remember { mutableStateOf(false) }
    var showTokenPicker by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }

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
                    selectedColorHex = null
                    activeTool = MapTool.BRUSH
                }
            }
        }
    }

    val bgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val b64 = ImagePickerUtil.uriToResizedBase64DataUrl(context, uri, maxDimension = 520, quality = 72)
                if (b64 != null) {
                    backgroundUrl = b64
                    onSyncMap(width, height, grid, tokens, b64, true)
                }
            }
        }
    }

    fun currentBrushCell(): GridCell = GridCell(
        colorHex = selectedColorHex,
        emoji = selectedEmoji,
        imageBase64 = selectedAssetBase64,
    )

    fun cellKey(x: Int, y: Int) = "$x,$y"

    fun applyPaint(cx: Int, cy: Int) {
        val next = grid.toMutableMap()
        val half = brushSize / 2
        val startX = cx - half
        val startY = cy - half
        val endX = startX + brushSize - 1
        val endY = startY + brushSize - 1
        for (px in startX..endX) {
            for (py in startY..endY) {
                if (px in 0 until width && py in 0 until height) {
                    val key = cellKey(px, py)
                    if (activeTool == MapTool.ERASER) {
                        next.remove(key)
                    } else {
                        next[key] = currentBrushCell()
                    }
                }
            }
        }
        grid = next
        onSyncMap(width, height, next, tokens, backgroundUrl, false)
    }

    fun applyFloodFill(sx: Int, sy: Int) {
        val startKey = cellKey(sx, sy)
        val targetCell = grid[startKey]
        val replacement = currentBrushCell()
        if (targetCell == replacement) return
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
        onSyncMap(width, height, next, tokens, backgroundUrl, false)
    }

    fun applyRectFill(a: Pair<Int, Int>, b: Pair<Int, Int>) {
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
        onSyncMap(width, height, next, tokens, backgroundUrl, false)
    }

    fun handleGridTap(gx: Int, gy: Int) {
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
                    onSyncMap(width, height, grid, updated, backgroundUrl, true)
                }
            }
            MapTool.PAN_ZOOM -> {
                val tappedToken = tokens.firstOrNull { it.x == gx && it.y == gy }
                if (tappedToken != null && (isGmMode || tappedToken.ownerId == currentUserId)) {
                    selectedTokenId = tappedToken.id
                    activeTool = MapTool.MOVE_TOKEN
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Map Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                IconButton(onClick = { showResizeDialog = true }, modifier = Modifier.testTag("map_resize_button")) {
                    Icon(Icons.Default.GridOn, contentDescription = "Resize Grid")
                }
                IconButton(
                    onClick = {
                        bgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.testTag("map_background_picker_button")
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Map Background")
                }
                IconButton(
                    onClick = {
                        grid = emptyMap()
                        onSyncMap(width, height, emptyMap(), tokens, backgroundUrl, true)
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

        // GM Tile Palette + Brush Size + Quick Asset Bar
        if (isGmMode && (activeTool == MapTool.BRUSH || activeTool == MapTool.FLOOD_FILL || activeTool == MapTool.RECT_FILL)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brush sizes
                listOf(1, 2, 3).forEach { sz ->
                    FilterChip(
                        selected = brushSize == sz,
                        onClick = { brushSize = sz },
                        label = { Text("${sz}x$sz") }
                    )
                }
                // Color terrain tiles
                listOf("#334155", "#1E293B", "#14532D", "#1E3A8A", "#7C2D12", "#78350F", "#4C1D95").forEach { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val selected = selectedColorHex == hex && selectedEmoji == null && selectedAssetBase64 == null
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(color)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                                shape = MaterialTheme.shapes.small
                            )
                            .clickable {
                                selectedColorHex = hex
                                selectedEmoji = null
                                selectedAssetBase64 = null
                            }
                    )
                }
                // Terrain Emojis
                listOf("🌲", "⛰️", "🌊", "🔥", "🧱", "🚪", "💎", "☠️", "🕳️", "🏰").forEach { em ->
                    FilterChip(
                        selected = selectedEmoji == em,
                        onClick = {
                            selectedEmoji = em
                            selectedColorHex = null
                            selectedAssetBase64 = null
                        },
                        label = { Text(em) }
                    )
                }
                // Custom Image Tiles Quick Bar
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
                                    selectedAssetBase64 = asset.imageBase64
                                    selectedEmoji = null
                                    selectedColorHex = null
                                }
                        )
                    }
                }
                IconButton(
                    onClick = {
                        tileAssetPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.testTag("map_upload_tile_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload Tile Asset")
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
                            onSyncMap(width, height, grid, updated, backgroundUrl, true)
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
            val bgBitmap = remember(backgroundUrl) { ImagePickerUtil.decodeDataUrlBitmap(backgroundUrl) }

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
                    .pointerInput(activeTool, width, height, grid, tokens, selectedTokenId, scale, panOffset) {
                        detectTapGestures { tapOffset ->
                            val localX = (tapOffset.x - panOffset.x) / scale
                            val localY = (tapOffset.y - panOffset.y) / scale
                            val gx = (localX / baseCellPx).toInt()
                            val gy = (localY / baseCellPx).toInt()
                            if (localX >= 0f && localY >= 0f) {
                                handleGridTap(gx, gy)
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
                    if (bgBitmap != null) {
                        Image(
                            bitmap = bgBitmap.asImageBitmap(),
                            contentDescription = "Map Background",
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.TopStart,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Painted Color Tiles & Grid Lines
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
                                        color = c.copy(alpha = 0.85f),
                                        topLeft = Offset(x * baseCellPx, y * baseCellPx),
                                        size = Size(baseCellPx, baseCellPx)
                                    )
                                }
                                drawRect(
                                    color = Color.White.copy(alpha = 0.14f),
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

                    // Cell Emojis & Custom Image Tiles
                    grid.forEach { (key, cell) ->
                        val parts = key.split(",")
                        val gx = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
                        val gy = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach
                        if (gx !in 0 until width || gy !in 0 until height) return@forEach
                        if (cell.imageBase64 != null || cell.emoji != null) {
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
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (cell.emoji != null) {
                                    Text(cell.emoji)
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
    }

    if (showResizeDialog) {
        var wText by remember { mutableStateOf(width.toString()) }
        var hText by remember { mutableStateOf(height.toString()) }
        AlertDialog(
            onDismissRequest = { showResizeDialog = false },
            title = { Text("Resize Grid (4-30)") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wText,
                        onValueChange = { wText = it.filter { c -> c.isDigit() } },
                        label = { Text("Width") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hText,
                        onValueChange = { hText = it.filter { c -> c.isDigit() } },
                        label = { Text("Height") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val nw = (wText.toIntOrNull() ?: width).coerceIn(4, 30)
                    val nh = (hText.toIntOrNull() ?: height).coerceIn(4, 30)
                    val cleanedGrid = grid.filterKeys { k ->
                        val p = k.split(",")
                        val x = p.getOrNull(0)?.toIntOrNull() ?: -1
                        val y = p.getOrNull(1)?.toIntOrNull() ?: -1
                        x in 0 until nw && y in 0 until nh
                    }
                    val cleanedTokens = tokens.map {
                        it.copy(x = it.x.coerceIn(0, nw - 1), y = it.y.coerceIn(0, nh - 1))
                    }
                    width = nw; height = nh; grid = cleanedGrid; tokens = cleanedTokens
                    onSyncMap(nw, nh, cleanedGrid, cleanedTokens, backgroundUrl, true)
                    showResizeDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showResizeDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showTokenPicker) {
        val placeableChars = if (isGmMode) characters else characters.filter { it.ownerId == currentUserId }
        AlertDialog(
            onDismissRequest = { showTokenPicker = false },
            title = { Text("Place Token on Map") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (placeableChars.isEmpty()) {
                        Text("Create a character first or place a quick token.")
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
                                    onSyncMap(width, height, grid, updated, backgroundUrl, true)
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
    }

    if (showGuide) {
        IdeaGuideDialog(guideKey = "map", onDismiss = { showGuide = false })
    }
}
