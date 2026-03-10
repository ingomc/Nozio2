package de.ingomc.nozio.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import de.ingomc.nozio.data.local.DiaryEntryWithFood
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.ui.common.bringIntoViewOnFocus
import de.ingomc.nozio.ui.theme.nozioColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MealCard(
    mealType: MealType,
    entries: List<DiaryEntryWithFood>,
    onAddClick: () -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onUpdateEntryAmount: (Long, Double) -> Unit,
    onCopyEntry: (DiaryEntryWithFood, LocalDate, MealType, Double) -> Unit,
    onDragStarted: (DiaryEntryWithFood, Offset) -> Unit,
    onDragMoved: (Offset) -> Unit,
    onDragEnded: () -> Unit,
    onMealBoundsChanged: (MealType, Rect) -> Unit,
    draggedEntryId: Long?,
    draggedOffsetY: Float,
    isDropTargetHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val totalCalories = entries.sumOf { it.calories }
    var editingEntry by remember { mutableStateOf<DiaryEntryWithFood?>(null) }
    var copyingEntry by remember { mutableStateOf<DiaryEntryWithFood?>(null) }
    val isDraggingInThisCard = draggedEntryId != null && entries.any { it.entryId == draggedEntryId }
    val mealCardColor = if (isDropTargetHighlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val mealCardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDraggingInThisCard) 10f else 0f)
            .onGloballyPositioned { coordinates ->
                onMealBoundsChanged(mealType, coordinates.boundsInRoot())
            }
            .shadow(
                elevation = 1.dp,
                shape = mealCardShape,
                clip = false
            )
            .background(
                color = mealCardColor,
                shape = mealCardShape
            )
            .then(
                if (isDraggingInThisCard) {
                    Modifier
                } else {
                    Modifier.clip(mealCardShape)
                }
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
                            onCopy = { copyingEntry = entry },
                            onEdit = { editingEntry = entry },
                            onDragStarted = { start -> onDragStarted(entry, start) },
                            onDragMoved = onDragMoved,
                            onDragEnded = {
                                onDragEnded()
                            },
                            isBeingDragged = draggedEntryId == entry.entryId,
                            dragOffsetY = draggedOffsetY
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
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

    copyingEntry?.let { entry ->
        CopyEntryBottomSheet(
            entry = entry,
            initialMealType = mealType,
            onDismiss = { copyingEntry = null },
            onSave = { date, targetMeal, amount ->
                onCopyEntry(entry, date, targetMeal, amount)
                copyingEntry = null
            }
        )
    }
}

@Composable
private fun SwipeRevealEntryRow(
    entry: DiaryEntryWithFood,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDragStarted: (Offset) -> Unit,
    onDragMoved: (Offset) -> Unit,
    onDragEnded: () -> Unit,
    isBeingDragged: Boolean,
    dragOffsetY: Float
) {
    val actionSlotWidth = 52.dp
    val actionButtonSize = 44.dp
    val rowShape = RoundedCornerShape(0.dp)
    val actionShape = RoundedCornerShape(18.dp)
    val density = LocalDensity.current
    val maxRevealPx = remember(density) { with(density) { actionSlotWidth.toPx() * 2f } }
    var offsetX by remember(entry.entryId) { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "entryRevealOffset")
    val revealProgress = (-animatedOffset / maxRevealPx).coerceIn(0f, 1f)
    val actionBackgroundAlpha = ((revealProgress - 0.12f) / 0.88f).coerceIn(0f, 1f)
    val copyActionAlpha = ((revealProgress - 0.10f) / 0.55f).coerceIn(0f, 1f)
    val deleteActionAlpha = ((revealProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .matchParentSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(actionSlotWidth * 2)
                    .clip(actionShape)
                    .alpha(actionBackgroundAlpha)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                MaterialTheme.colorScheme.surfaceContainer,
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(actionSlotWidth * 2)
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
                            .alpha(copyActionAlpha)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(enabled = copyActionAlpha > 0.2f, onClick = onCopy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Eintrag kopieren",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
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
                            .alpha(deleteActionAlpha)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable(enabled = deleteActionAlpha > 0.2f, onClick = onDelete),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eintrag löschen",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = animatedOffset.roundToInt(),
                        y = if (isBeingDragged) dragOffsetY.roundToInt() else 0
                    )
                }
                .zIndex(if (isBeingDragged) 100f else 0f)
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
                .onGloballyPositioned { coordinates ->
                    layoutCoordinates = coordinates
                }
                .pointerInput(entry.entryId) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            layoutCoordinates?.let { coordinates ->
                                onDragStarted(coordinates.localToRoot(offset))
                            }
                        },
                        onDragEnd = onDragEnded,
                        onDragCancel = onDragEnded,
                        onDrag = { change, _ ->
                            change.consume()
                            layoutCoordinates?.let { coordinates ->
                                onDragMoved(coordinates.localToRoot(change.position))
                            }
                        }
                    )
                }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
            ) {
                Text(
                    text = entry.foodName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${entry.calories.toInt()} kcal, KH: ${entry.carbs.toInt()}g, EW: ${entry.protein.toInt()}g, F: ${entry.fat.toInt()}g",
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
private fun CopyEntryBottomSheet(
    entry: DiaryEntryWithFood,
    initialMealType: MealType,
    onDismiss: () -> Unit,
    onSave: (LocalDate, MealType, Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by remember { mutableStateOf(false) }
    var targetDate by remember(entry.entryId) { mutableStateOf(LocalDate.now()) }
    var targetMeal by remember(entry.entryId) { mutableStateOf(initialMealType) }
    var amountText by remember(entry.entryId) { mutableStateOf(entry.amountInGrams.toInt().toString()) }
    val amount = amountText.toDoubleOrNull()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)

    if (showDatePicker) {
        val initialDateMillis = remember(targetDate) {
            targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            targetDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.nozioColors.surface2,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
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
                text = "Eintrag kopieren",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { showDatePicker = true }
            ) {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = null)
                Text(
                    text = targetDate.format(dateFormatter),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = "Mahlzeit",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MealType.entries.forEach { meal ->
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = targetMeal == meal,
                        onClick = { targetMeal = meal },
                        label = {
                            Text(
                                text = meal.displayName.substringAfter(' '),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
            OutlinedTextField(
                value = amountText,
                onValueChange = { value -> amountText = value.filter { it.isDigit() } },
                label = { Text("Gramm") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
                Button(
                    onClick = { onSave(targetDate, targetMeal, amount ?: 0.0) },
                    enabled = (amount ?: 0.0) > 0.0
                ) {
                    Text("Kopieren")
                }
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
        sheetState = sheetState,
        containerColor = MaterialTheme.nozioColors.surface2,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
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
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus()
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
