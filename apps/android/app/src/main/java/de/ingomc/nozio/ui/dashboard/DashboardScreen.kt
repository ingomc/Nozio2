package de.ingomc.nozio.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.local.DiaryEntryWithFood
import de.ingomc.nozio.ui.theme.nozioColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddFood: (MealType) -> Unit,
    onEditSupplements: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showStepsSheet by rememberSaveable { mutableStateOf(false) }
    var pendingSteps by rememberSaveable { mutableStateOf(0L) }
    var showWeightSheet by rememberSaveable { mutableStateOf(false) }
    var pendingWeight by remember { mutableDoubleStateOf(0.0) }
    var pendingBodyFat by remember { mutableDoubleStateOf(20.0) }
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)
    val appBarState = rememberTopAppBarState()
    val appBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(appBarState)
    var draggedEntry by remember { mutableStateOf<DiaryEntryWithFood?>(null) }
    var draggedStartPosition by remember { mutableStateOf<Offset?>(null) }
    var draggedPosition by remember { mutableStateOf<Offset?>(null) }
    val draggedOffsetY = draggedStartPosition?.let { start ->
        draggedPosition?.let { current ->
            current.y - start.y
        }
    } ?: 0f
    val mealBounds = remember { mutableStateMapOf<MealType, Rect>() }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerStateForDate(state.selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.selectDate(millisToLocalDate(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showWeightSheet) {
        WeightInputBottomSheet(
            initialWeightKg = pendingWeight,
            initialBodyFatPercent = pendingBodyFat,
            latestWeightEntryDate = state.displayWeightDate,
            latestWeightEntryWeightKg = state.displayWeightKg,
            latestBodyFatEntryDate = state.displayBodyFatDate,
            latestBodyFatEntryPercent = state.displayBodyFatPercent,
            onDismiss = { showWeightSheet = false },
            onSave = { selectedWeight, selectedBodyFat ->
                viewModel.saveWeightAndBodyFatForSelectedDate(selectedWeight, selectedBodyFat)
                showWeightSheet = false
            }
        )
    }

    if (showStepsSheet) {
        StepsInputBottomSheet(
            initialSteps = pendingSteps,
            includeActivityCaloriesInBudget = state.preferences.includeActivityCaloriesInBudget,
            onIncludeActivityCaloriesInBudgetChange = viewModel::setIncludeActivityCaloriesInBudget,
            onDismiss = { showStepsSheet = false },
            onSave = { selectedSteps ->
                viewModel.saveStepsForSelectedDate(selectedSteps)
                showStepsSheet = false
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(appBarScrollBehavior.nestedScrollConnection)
        ) {
            TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            ),
            scrollBehavior = appBarScrollBehavior,
            title = {
                Column {
                    Text(
                        text = "Nozio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDashboardDateLabel(state.selectedDate, dateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Datum wählen"
                    )
                }
            }
        )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            item {
                val consumedCalories = state.totalCalories
                val burnedCalories = state.activeCalories
                val budgetBonusCalories = if (state.preferences.includeActivityCaloriesInBudget) burnedCalories else 0.0
                val ringConsumedCalories = consumedCalories - budgetBonusCalories
                val calorieDelta = state.preferences.calorieGoal - consumedCalories + budgetBonusCalories
                val centerValue = if (calorieDelta >= 0.0) {
                    calorieDelta.toInt().toString()
                } else {
                    (-calorieDelta).toInt().toString()
                }
                val centerLabel = if (calorieDelta >= 0.0) "Übrig" else "Zu viel gegessen"

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TopMetric(
                                value = consumedCalories.toInt().toString(),
                                label = "Gegessen"
                            )
                            CalorieRing(
                                consumed = ringConsumedCalories,
                                goal = state.preferences.calorieGoal,
                                centerValue = centerValue,
                                centerLabel = centerLabel
                            )
                            TopMetric(
                                value = burnedCalories.toInt().toString(),
                                label = "Verbrannt"
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MacroBar(
                                label = "Kohlenhydrate",
                                consumed = state.totalCarbs,
                                goal = state.preferences.carbsGoal,
                                color = MaterialTheme.nozioColors.macroPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            MacroBar(
                                label = "Eiweiß",
                                consumed = state.totalProtein,
                                goal = state.preferences.proteinGoal,
                                color = MaterialTheme.nozioColors.macroPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            MacroBar(
                                label = "Fett",
                                consumed = state.totalFat,
                                goal = state.preferences.fatGoal,
                                color = MaterialTheme.nozioColors.macroPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                DashboardSectionHeader(
                    title = "Supplements",
                    onEditClick = onEditSupplements
                )
            }

            item {
                SupplementsTimelineCard(
                    selectedDate = state.selectedDate,
                    items = state.supplementTimelineItems,
                    onToggleTaken = viewModel::toggleSupplementTaken
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Meal Cards
            item {
                Text(
                    text = "Ernährung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(MealType.entries) { mealType ->
                MealCard(
                    mealType = mealType,
                    entries = state.entriesByMeal[mealType] ?: emptyList(),
                    onAddClick = { onAddFood(mealType) },
                    onDeleteEntry = { viewModel.deleteEntry(it) },
                    onUpdateEntryAmount = { entryId, amount ->
                        viewModel.updateEntryAmount(entryId, amount)
                    },
                    onCopyEntry = { entry, date, targetMeal, amount ->
                        viewModel.copyEntry(
                            foodItemId = entry.foodItemId,
                            date = date,
                            mealType = targetMeal,
                            amountInGrams = amount
                        )
                    },
                    onDragStarted = { entry, start ->
                        draggedEntry = entry
                        draggedStartPosition = start
                        draggedPosition = start
                    },
                    onDragMoved = { position ->
                        draggedPosition = draggedPosition?.let { current ->
                            Offset(x = current.x, y = position.y)
                        } ?: position
                    },
                    onDragEnded = {
                        val entry = draggedEntry
                        val position = draggedPosition
                        val targetMeal = if (position != null) {
                            mealBounds.entries
                                .firstOrNull { (_, bounds) -> bounds.contains(position) }
                                ?.key
                        } else {
                            null
                        }
                        if (entry != null && targetMeal != null && targetMeal != entry.mealType) {
                            viewModel.moveEntry(
                                entryId = entry.entryId,
                                date = state.selectedDate,
                                mealType = targetMeal
                            )
                        }
                        draggedEntry = null
                        draggedStartPosition = null
                        draggedPosition = null
                    },
                    onMealBoundsChanged = { meal, bounds ->
                        mealBounds[meal] = bounds
                    },
                    draggedEntryId = draggedEntry?.entryId,
                    draggedOffsetY = draggedOffsetY,
                    isDropTargetHighlighted = draggedEntry != null &&
                        (draggedPosition?.let { position ->
                            mealBounds[mealType]?.contains(position) == true
                        } == true)
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Activity Card
            item {
                DashboardSectionHeader(
                    title = "Aktivität",
                    onEditClick = {
                        pendingSteps = state.totalSteps
                        showStepsSheet = true
                    }
                )
            }

            item {
                ActivityCard(
                    steps = state.totalSteps,
                    activeCalories = state.activeCalories,
                    onClick = {
                        pendingSteps = state.totalSteps
                        showStepsSheet = true
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                DashboardSectionHeader(
                    title = "Gewicht & KFA",
                    onEditClick = {
                        pendingWeight = state.displayWeightKg
                            ?: state.preferences.currentWeightKg
                        pendingBodyFat = state.displayBodyFatPercent
                            ?: state.preferences.bodyFatPercent
                        showWeightSheet = true
                    }
                )
            }

            item {
                WeightCard(
                    weightKg = state.displayWeightKg,
                    weightDate = state.displayWeightDate,
                    isWeightFallback = state.isWeightFallback,
                    bodyFatPercent = state.displayBodyFatPercent,
                    bodyFatDate = state.displayBodyFatDate,
                    isBodyFatFallback = state.isBodyFatFallback,
                    onClick = {
                        pendingWeight = state.displayWeightKg
                            ?: state.preferences.currentWeightKg
                        pendingBodyFat = state.displayBodyFatPercent
                            ?: state.preferences.bodyFatPercent
                        showWeightSheet = true
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

private fun formatDashboardDateLabel(
    date: LocalDate,
    formatter: DateTimeFormatter
): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Heute"
        today.minusDays(1) -> "Gestern"
        else -> date.format(formatter)
    }
}

@Composable
private fun TopMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberDatePickerStateForDate(date: LocalDate): DatePickerState {
    val initialDateMillis = remember(date) {
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    return androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )
}

@Composable
private fun DashboardSectionHeader(
    title: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = onEditClick) {
            Text("BEARBEITEN")
        }
    }
}

private fun millisToLocalDate(millis: Long): LocalDate {
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}
