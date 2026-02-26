package de.ingomc.nozio.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.ingomc.nozio.data.local.MealType
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
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

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

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Nozio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.selectedDate.format(dateFormatter),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val consumedCalories = state.totalCalories
                val burnedCalories = state.activeCalories
                val remainingCalories = (state.preferences.calorieGoal - consumedCalories + burnedCalories)
                    .coerceAtLeast(0.0)

                Card(modifier = Modifier.fillMaxWidth()) {
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
                                consumed = consumedCalories,
                                goal = state.preferences.calorieGoal,
                                centerValue = remainingCalories.toInt().toString(),
                                centerLabel = "Übrig"
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
                                color = Color(0xFF2196F3),
                                modifier = Modifier.weight(1f)
                            )
                            MacroBar(
                                label = "Eiweiß",
                                consumed = state.totalProtein,
                                goal = state.preferences.proteinGoal,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )
                            MacroBar(
                                label = "Fett",
                                consumed = state.totalFat,
                                goal = state.preferences.fatGoal,
                                color = Color(0xFFFF9800),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Activity Card
            item {
                ActivityCard(
                    steps = state.totalSteps,
                    activeCalories = state.activeCalories,
                    stepsInput = state.stepsInput,
                    stepsSaved = state.stepsSaved,
                    onStepsInputChange = viewModel::onStepsInputChange,
                    onSaveSteps = viewModel::saveStepsForSelectedDate
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Meal Cards
            items(MealType.entries) { mealType ->
                MealCard(
                    mealType = mealType,
                    entries = state.entriesByMeal[mealType] ?: emptyList(),
                    onAddClick = { onAddFood(mealType) },
                    onDeleteEntry = { viewModel.deleteEntry(it) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
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

private fun millisToLocalDate(millis: Long): LocalDate {
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}
