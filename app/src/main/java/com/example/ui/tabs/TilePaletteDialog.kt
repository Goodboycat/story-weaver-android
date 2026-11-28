package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.TileAtlas

/** kind = "tile" | "line" | "obj" | "sprite" (the GridCell / MapToken field it fills). */
data class TilePick(val kind: String, val name: String)

private val KIND_LABELS = listOf("tile" to "Terrain", "line" to "Lines", "obj" to "Objects", "sprite" to "Sprites")

/**
 * Tileset picker: kind tabs -> category chips (atlas `cat`) -> name grid with previews + search.
 * [kinds] limits the tabs (map brush: tile/line/obj; token picker: sprite).
 */
@Composable
fun TilePaletteDialog(
    atlas: TileAtlas,
    kinds: List<String> = listOf("tile", "line", "obj"),
    initialKind: String = kinds.first(),
    onPick: (TilePick) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(initialKind) }
    var query by remember { mutableStateOf("") }
    val cats: Map<String, List<String>> = remember(kind) {
        when (kind) {
            "tile" -> atlas.terrainCats.mapValues { e -> e.value.map { it.name } }
            "line" -> atlas.lineCats.mapValues { e -> e.value.map { it.name } }
            "obj" -> atlas.objectCats.mapValues { e -> e.value.map { it.name } }
            else -> atlas.spriteCats.mapValues { e -> e.value.map { it.name } }
        }
    }
    var cat by remember(kind) { mutableStateOf(cats.keys.firstOrNull().orEmpty()) }
    val names = remember(kind, cat, query) {
        val q = query.trim().lowercase().replace(' ', '_')
        if (q.isEmpty()) cats[cat].orEmpty() else cats.values.flatten().filter { it.contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Tileset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (kinds.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        KIND_LABELS.filter { it.first in kinds }.forEach { (k, label) ->
                            FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(label) })
                        }
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search all (e.g. tree, snow, dragon)") },
                    modifier = Modifier.fillMaxWidth().testTag("tile_palette_search"),
                )
                if (query.isBlank()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        cats.keys.forEach { c ->
                            FilterChip(
                                selected = cat == c,
                                onClick = { cat = c },
                                label = {
                                    Text(
                                        c.removePrefix("kingdom_").removePrefix("dim_").replace('_', ' '),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(64.dp),
                    modifier = Modifier.fillMaxWidth().height(340.dp).testTag("tile_palette_grid"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(names, key = { it }) { name ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .border(1.dp, Color.White.copy(alpha = 0.15f), MaterialTheme.shapes.small)
                                .background(Color(0xFF2E3B2E), MaterialTheme.shapes.small)
                                .clickable { onPick(TilePick(kind, name)) }
                                .padding(4.dp),
                        ) {
                            TilePreview(atlas, kind, name, 48.dp)
                            Text(
                                text = name.replace('_', ' '),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
    )
}
