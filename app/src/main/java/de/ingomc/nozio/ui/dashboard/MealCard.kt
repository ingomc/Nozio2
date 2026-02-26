package de.ingomc.nozio.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.ingomc.nozio.data.local.DiaryEntryWithFood
import de.ingomc.nozio.data.local.MealType
import kotlin.math.roundToInt

@Composable
fun MealCard(
    mealType: MealType,
    entries: List<DiaryEntryWithFood>,
    onAddClick: () -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onUpdateEntryAmount: (Long, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCalories = entries.sumOf { it.calories }
    var editingEntry by remember { mutableStateOf<DiaryEntryWithFood?>(null) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = mealType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${totalCalories.toInt()} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Lebensmittel hinzufügen",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Food entries
            if (entries.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Column {
                    entries.forEach { entry ->
                        SwipeRevealEntryRow(
                            entry = entry,
                            onDelete = { onDeleteEntry(entry.entryId) },
                            onCopy = {},
                            onEdit = { editingEntry = entry }
                        )
                    }
                }
            }
        }
    }

    editingEntry?.let { entry ->
        EditEntryBottomSheet(
            entry = entry,
            onDismiss = { editingEntry = null },
            onSave = { amount ->
                onUpdateEntryAmount(entry.entryId, amount)
                editingEntry = null
            }
        )
    }
}

@Composable
private fun SwipeRevealEntryRow(
    entry: DiaryEntryWithFood,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit
) {
    val actionSlotWidth = 52.dp
    val actionButtonSize = 44.dp
    val rowShape = RoundedCornerShape(14.dp)
    val actionShape = RoundedCornerShape(18.dp)
    val density = LocalDensity.current
    val maxRevealPx = remember(density) { with(density) { actionSlotWidth.toPx() * 2f } }
    var offsetX by remember(entry.entryId) { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "entryRevealOffset")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .heightIn(min = 64.dp)
            .clip(rowShape)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(actionSlotWidth * 2)
                    .clip(actionShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(actionSlotWidth)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(actionButtonSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            .clickable(onClick = onCopy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Eintrag kopieren",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .width(actionSlotWidth)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(actionButtonSize)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            .clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eintrag löschen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .fillMaxWidth()
                .clip(rowShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable {
                    if (offsetX == 0f) {
                        onEdit()
                    } else {
                        offsetX = 0f
                    }
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val next = (offsetX + delta).coerceIn(-maxRevealPx, 0f)
                        offsetX = next
                    },
                    onDragStopped = {
                        offsetX = if (offsetX < -maxRevealPx * 0.45f) -maxRevealPx else 0f
                    }
                )
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = entry.foodName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "KH: ${entry.carbs.toInt()}g, EW: ${entry.protein.toInt()}g, F: ${entry.fat.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEntryBottomSheet(
    entry: DiaryEntryWithFood,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember(entry.entryId) { mutableStateOf(entry.amountInGrams.toInt().toString()) }
    val amount = amountText.toDoubleOrNull()

    LaunchedEffect(entry.entryId) {
        amountText = entry.amountInGrams.toInt().toString()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = entry.foodName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Menge anpassen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { value -> amountText = value.filter { it.isDigit() } },
                label = { Text("Gramm") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val safeAmount = amount ?: 0.0
            Text(
                text = "${safeAmount.toInt()}g · ${(entry.caloriesPer100g * safeAmount / 100.0).toInt()} kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
                Button(
                    onClick = { onSave(safeAmount) },
                    enabled = safeAmount > 0.0
                ) {
                    Text("Speichern")
                }
            }
        }
    }
}
