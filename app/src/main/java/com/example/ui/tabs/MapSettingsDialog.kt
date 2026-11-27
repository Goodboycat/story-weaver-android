package com.example.ui.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.JsonCodecs
import com.example.data.MapBackgroundLayer
import com.example.ui.components.ImagePickerUtil
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapSettingsDialog(
    initialWidth: Int,
    initialHeight: Int,
    initialLayers: List<MapBackgroundLayer>,
    onApply: (width: Int, height: Int, layers: List<MapBackgroundLayer>) -> Unit,
    onArrangeOnCanvas: (width: Int, height: Int, layers: List<MapBackgroundLayer>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var gridWidth by remember(initialWidth) { mutableIntStateOf(initialWidth.coerceIn(4, 30)) }
    var gridHeight by remember(initialHeight) { mutableIntStateOf(initialHeight.coerceIn(4, 30)) }
    var layers by remember(initialLayers) { mutableStateOf(initialLayers) }
    var isProcessingImages by remember { mutableStateOf(false) }
    var imageStatusMsg by remember { mutableStateOf<String?>(null) }

    val multiBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isProcessingImages = true
                imageStatusMsg = null
                val totalCount = layers.size + uris.size
                val targetDim = when {
                    totalCount <= 1 -> 520
                    totalCount <= 3 -> 360
                    else -> 260
                }
                val quality = if (totalCount <= 2) 72 else 64
                val added = mutableListOf<MapBackgroundLayer>()
                for ((idx, uri) in uris.withIndex()) {
                    val b64 = ImagePickerUtil.uriToResizedBase64DataUrl(
                        context = context,
                        uri = uri,
                        maxDimension = targetDim,
                        quality = quality
                    )
                    if (b64 != null) {
                        val existingCount = layers.size + added.size
                        val defaultSpanW = if (totalCount == 1) gridWidth.toFloat() else (gridWidth / 2f).coerceAtLeast(3f)
                        val defaultSpanH = if (totalCount == 1) gridHeight.toFloat() else (gridHeight / 2f).coerceAtLeast(3f)
                        val col = existingCount % 2
                        val row = existingCount / 2
                        added += MapBackgroundLayer(
                            id = "bg_${UUID.randomUUID().toString().take(8)}",
                            label = "Map Image ${existingCount + 1}",
                            imageBase64 = b64,
                            gridX = if (totalCount == 1) 0f else (col * defaultSpanW).coerceAtMost((gridWidth - 2).toFloat()),
                            gridY = if (totalCount == 1) 0f else (row * defaultSpanH).coerceAtMost((gridHeight - 2).toFloat()),
                            widthCells = defaultSpanW,
                            heightCells = defaultSpanH,
                            opacity = 1f,
                        )
                    } else if (idx == 0) {
                        imageStatusMsg = "One of the selected images could not be decoded."
                    }
                }
                val combined = layers + added
                val encodedSize = JsonCodecs.encodeMapBackgroundLayers(combined)?.length ?: 0
                if (encodedSize > 395_000) {
                    imageStatusMsg = "Too many high-resolution images for sync limit; remove an image or upload fewer at once."
                } else {
                    layers = combined
                    if (added.isNotEmpty()) {
                        imageStatusMsg = "Added ${added.size} map image(s). Adjust X/Y & Size below or drag on canvas."
                    }
                }
                isProcessingImages = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessingImages) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Map Size & Image Layout", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Grid Size Section
                Text("Grid Dimensions (4–30 tiles)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                GridDimensionStepper(
                    label = "Width (Columns)",
                    value = gridWidth,
                    onChange = { gridWidth = it.coerceIn(4, 30) },
                    testTagPrefix = "map_width"
                )
                GridDimensionStepper(
                    label = "Height (Rows)",
                    value = gridHeight,
                    onChange = { gridHeight = it.coerceIn(4, 30) },
                    testTagPrefix = "map_height"
                )

                HorizontalDivider()

                // Map Background Images Section
                Text("Map Background Images", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Upload a single map image or multiple room/section images and position each across the grid.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            multiBgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        enabled = !isProcessingImages,
                        modifier = Modifier.testTag("map_upload_multiple_images_button")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isProcessingImages) "Processing..." else "Upload Image(s)")
                    }
                    if (layers.size > 1) {
                        OutlinedButton(
                            onClick = {
                                val cols = ceil(sqrt(layers.size.toDouble())).toInt().coerceAtLeast(1)
                                val rows = ceil(layers.size.toDouble() / cols).toInt().coerceAtLeast(1)
                                val cellW = (gridWidth.toFloat() / cols).coerceAtLeast(2f)
                                val cellH = (gridHeight.toFloat() / rows).coerceAtLeast(2f)
                                layers = layers.mapIndexed { idx, layer ->
                                    val c = idx % cols
                                    val r = idx / cols
                                    layer.copy(
                                        gridX = c * cellW,
                                        gridY = r * cellH,
                                        widthCells = cellW,
                                        heightCells = cellH,
                                    )
                                }
                            },
                            modifier = Modifier.testTag("map_auto_tile_images_button")
                        ) {
                            Icon(Icons.Default.ViewModule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Auto-Tile")
                        }
                    }
                    if (layers.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onArrangeOnCanvas(gridWidth, gridHeight, layers) },
                            modifier = Modifier.testTag("map_arrange_on_canvas_button")
                        ) {
                            Icon(Icons.Default.OpenWith, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Drag on Map")
                        }
                    }
                }

                if (imageStatusMsg != null) {
                    Text(
                        text = imageStatusMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (layers.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No background images added yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    layers.forEachIndexed { index, layer ->
                        val bmp = remember(layer.imageBase64) { ImagePickerUtil.decodeDataUrlBitmap(layer.imageBase64) }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = layer.label,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(MaterialTheme.shapes.small)
                                                .background(MaterialTheme.colorScheme.surface)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(layer.label, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "Pos (${layer.gridX.roundToInt()}, ${layer.gridY.roundToInt()}) • Size ${layer.widthCells.roundToInt()}x${layer.heightCells.roundToInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mut = layers.toMutableList()
                                                val tmp = mut[index - 1]
                                                mut[index - 1] = mut[index]
                                                mut[index] = tmp
                                                layers = mut
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Layer Back", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < layers.lastIndex) {
                                                val mut = layers.toMutableList()
                                                val tmp = mut[index + 1]
                                                mut[index + 1] = mut[index]
                                                mut[index] = tmp
                                                layers = mut
                                            }
                                        },
                                        enabled = index < layers.lastIndex
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Layer Forward", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { layers = layers.filterNot { it.id == layer.id } }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove Image",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            layers = layers.map {
                                                if (it.id == layer.id) it.copy(
                                                    gridX = 0f,
                                                    gridY = 0f,
                                                    widthCells = gridWidth.toFloat(),
                                                    heightCells = gridHeight.toFloat()
                                                ) else it
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Fit Full Grid", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                // X, Y, Width, Height adjustments in grid cells
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CompactStepper(
                                        label = "X",
                                        value = layer.gridX.roundToInt(),
                                        onMinus = {
                                            layers = layers.map {
                                                if (it.id == layer.id) it.copy(gridX = (it.gridX - 1f).coerceAtLeast(-10f)) else it
                                            }
                                        },
                                        onPlus = {
                                            layers = layers.map {
                                                if (it.id == layer.id) it.copy(gridX = (it.gridX + 1f).coerceAtMost(gridWidth.toFloat())) else it
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    CompactStepper(
                                        label = "Y",
                                        value = layer.gridY.roundToInt(),
                                        onMinus = {
                                            layers = layers.map {
                                                if (it.id == layer.id) it.copy(gridY = (it.gridY - 1f).coerceAtLeast(-10f)) else it
                                            }
                                        },
                                        onPlus = {
                                            layers = layers.map {
                                                if (it.id == layer.id) it.copy(gridY = (it.gridY + 1f).coerceAtMost(gridHeight.toFloat())) else it
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CompactStepper(
                                        label = "W",
                                        value = layer.widthCells.roundToInt(),
                                        onMinus = {
                                            layers = layers.map {
                                                if (it.id == layer.id) it.copy(widthCells = max(1f, it.widthCells - 1f)) else it
                                            }
                                        },
                                        onPlus = {
                                            layers = layers.map {
                                                if (it.id == layer.id) it.copy(widthCells = (it.widthCells + 1f).coerceAtMost(30f)) else it
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    CompactStepper(
                                        label = "H",
                                        value = layer.heightCells.roundToInt(),
                                        onMinus = {
                                            layers = layers.map {
                                                if (it.id == layer.id) it.copy(heightCells = max(1f, it.heightCells - 1f)) else it
                                            }
                                        },
                                        onPlus = {
                                            layers = layers.map {
                                                if (it.id == layer.id) it.copy(heightCells = (it.heightCells + 1f).coerceAtMost(30f)) else it
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Opacity", style = MaterialTheme.typography.labelSmall)
                                    Slider(
                                        value = layer.opacity,
                                        onValueChange = { op ->
                                            layers = layers.map {
                                                if (it.id == layer.id) it.copy(opacity = op.coerceIn(0.15f, 1f)) else it
                                            }
                                        },
                                        valueRange = 0.15f..1f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${(layer.opacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(gridWidth, gridHeight, layers) },
                enabled = !isProcessingImages,
                modifier = Modifier.testTag("apply_map_settings_button")
            ) {
                Text("Apply Map")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessingImages) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GridDimensionStepper(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    testTagPrefix: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            IconButton(
                onClick = { onChange(value - 1) },
                enabled = value > 4,
                modifier = Modifier.size(36.dp).testTag("${testTagPrefix}_minus")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease $label", modifier = Modifier.size(16.dp))
            }
            OutlinedTextField(
                value = value.toString(),
                onValueChange = { raw ->
                    raw.filter { it.isDigit() }.toIntOrNull()?.let { onChange(it) }
                },
                singleLine = true,
                modifier = Modifier.width(64.dp).testTag("${testTagPrefix}_input")
            )
            IconButton(
                onClick = { onChange(value + 1) },
                enabled = value < 30,
                modifier = Modifier.size(36.dp).testTag("${testTagPrefix}_plus")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase $label", modifier = Modifier.size(16.dp))
            }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 4f..30f,
            steps = 25
        )
    }
}

@Composable
private fun CompactStepper(
    label: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$label: $value", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMinus, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease $label", modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onPlus, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Increase $label", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
