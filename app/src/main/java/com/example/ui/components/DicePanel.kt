package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppStrings
import com.example.data.DiceRoll

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiceRollerBottomSheet(
    strings: AppStrings,
    rolls: List<DiceRoll>,
    onDismiss: () -> Unit,
    onRoll: (diceTerms: List<Pair<Int, Int>>, modifierSteps: List<Pair<Char, Int>>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dieTypes = listOf(4, 6, 8, 10, 12, 20, 100)
    val dicePool = remember { mutableStateListOf<Pair<Int, Int>>() } // (count, sides)
    val modifiers = remember { mutableStateListOf<Pair<Char, Int>>() }
    var pendingOp by remember { mutableStateOf('+') }
    var modNumber by remember { mutableIntStateOf(0) }
    var showGuide by remember { mutableStateOf(false) }

    val expressionPreview = remember(dicePool.toList(), modifiers.toList(), modNumber, pendingOp) {
        val base = if (dicePool.isEmpty()) "Tap a die below"
        else dicePool.joinToString(" + ") { (c, s) -> "${c}d$s" }
        val mods = modifiers.joinToString("") { (op, v) -> " $op $v" }
        val activeMod = if (modNumber > 0) " $pendingOp $modNumber" else ""
        "$base$mods$activeMod"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(strings.diceRoller, style = MaterialTheme.typography.headlineSmall)
                }
                IconButton(
                    onClick = { showGuide = true },
                    modifier = Modifier.testTag("dice_idea_guide_button")
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = strings.ideaGuide)
                }
            }

            // Expression display
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = expressionPreview,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (modNumber > 0) {
                                modNumber /= 10
                            } else if (modifiers.isNotEmpty()) {
                                modifiers.removeAt(modifiers.lastIndex)
                            } else if (dicePool.isNotEmpty()) {
                                val last = dicePool.last()
                                if (last.first > 1) {
                                    dicePool[dicePool.lastIndex] = last.first - 1 to last.second
                                } else {
                                    dicePool.removeAt(dicePool.lastIndex)
                                }
                            }
                        },
                        modifier = Modifier.testTag("dice_backspace_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = {
                            dicePool.clear()
                            modifiers.clear()
                            modNumber = 0
                        },
                        modifier = Modifier.testTag("dice_clear_button")
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = strings.clear)
                    }
                }
            }

            // Tap-based Dice Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                dieTypes.forEach { sides ->
                    FilledTonalButton(
                        onClick = {
                            val idx = dicePool.indexOfLast { it.second == sides }
                            if (idx >= 0) {
                                val cur = dicePool[idx]
                                dicePool[idx] = (cur.first + 1) to sides
                            } else {
                                dicePool.add(1 to sides)
                            }
                        },
                        modifier = Modifier.testTag("die_d${sides}_button")
                    ) {
                        Text("d$sides", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Chained operator + quick modifier digits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf('+', '-', '×', '÷').forEach { op ->
                    val selected = pendingOp == op
                    OutlinedButton(
                        onClick = {
                            if (modNumber > 0) {
                                modifiers.add(pendingOp to modNumber)
                                modNumber = 0
                            }
                            pendingOp = op
                        },
                        modifier = Modifier.weight(1f).testTag("dice_op_${op}_button")
                    ) {
                        Text(
                            op.toString(),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(1, 2, 3, 5, 10).forEach { inc ->
                    OutlinedButton(
                        onClick = { modNumber += inc },
                        modifier = Modifier.weight(1f).testTag("dice_mod_plus_${inc}")
                    ) {
                        Text("+$inc", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Button(
                onClick = {
                    val finalMods = modifiers.toMutableList()
                    if (modNumber > 0) finalMods.add(pendingOp to modNumber)
                    if (dicePool.isNotEmpty()) {
                        onRoll(dicePool.toList(), finalMods)
                        modNumber = 0
                    }
                },
                enabled = dicePool.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("dice_roll_now_button")
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.rollNow, style = MaterialTheme.typography.titleMedium)
            }

            HorizontalDivider()

            Text(
                "Shared Table Roll Log",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (rolls.isEmpty()) {
                    item {
                        Text(
                            "No rolls yet. Tap a die above and roll to share with the table!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(rolls, key = { it.id }) { r ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${r.rollerName} • ${r.expression}",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        r.breakdown,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    r.total.toString(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGuide) {
        IdeaGuideDialog(guideKey = "dice", onDismiss = { showGuide = false })
    }
}
