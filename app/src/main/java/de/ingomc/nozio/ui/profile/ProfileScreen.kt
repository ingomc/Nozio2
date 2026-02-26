package de.ingomc.nozio.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private enum class WeightRange(val label: String, val days: Long?) {
    WEEK("7T", 7),
    MONTH("30T", 30),
    QUARTER("90T", 90),
    ALL("Alle", null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val appBarState = rememberTopAppBarState()
    val appBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(appBarState)
    val proteinGoal = state.proteinGoal.toDoubleOrNull() ?: 0.0
    val fatGoal = state.fatGoal.toDoubleOrNull() ?: 0.0
    val carbsGoal = state.carbsGoal.toDoubleOrNull() ?: 0.0
    val macroCalories = (proteinGoal * 4.0) + (carbsGoal * 4.0) + (fatGoal * 9.0)
    var selectedWeightRange by remember { mutableStateOf(WeightRange.MONTH) }
    val filteredWeightHistory = remember(state.weightHistory, selectedWeightRange) {
        filterWeightHistory(state.weightHistory, selectedWeightRange)
    }

    Column(
        modifier = modifier
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
                Text(
                    text = "Profil & Ziele",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tagesziele",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Lege deine täglichen Ernährungsziele fest. Diese werden im Dashboard als Referenzwerte angezeigt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = state.calorieGoal,
                onValueChange = viewModel::onCalorieGoalChange,
                label = { Text("Kalorienziel (kcal)") },
                supportingText = {
                    Text(
                        text = "Aus Makro-Zielen: ${macroCalories.toInt()} kcal (EW ${proteinGoal.toInt()}g x4 + KH ${carbsGoal.toInt()}g x4 + Fett ${fatGoal.toInt()}g x9)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.proteinGoal,
                    onValueChange = viewModel::onProteinGoalChange,
                    label = { Text("EW (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = state.fatGoal,
                    onValueChange = viewModel::onFatGoalChange,
                    label = { Text("Fett (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = state.carbsGoal,
                    onValueChange = viewModel::onCarbsGoalChange,
                    label = { Text("KH (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = state.currentWeightKg,
                onValueChange = viewModel::onCurrentWeightKgChange,
                label = { Text("Aktuelles Gewicht (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.bodyFatPercent,
                onValueChange = viewModel::onBodyFatPercentChange,
                label = { Text("KFA geschaetzt (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Gewichtsverlauf",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeightRange.entries.forEach { range ->
                    FilterChip(
                        selected = selectedWeightRange == range,
                        onClick = { selectedWeightRange = range },
                        label = { Text(range.label) }
                    )
                }
            }

            if (filteredWeightHistory.isEmpty()) {
                Text(
                    text = "Noch keine Gewichts-Einträge vorhanden.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                WeightChart(
                    points = filteredWeightHistory,
                    modifier = Modifier.fillMaxWidth()
                )

                val latest = filteredWeightHistory.last().weightKg
                val min = filteredWeightHistory.minOf { it.weightKg }
                val max = filteredWeightHistory.maxOf { it.weightKg }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Letzter: ${formatWeight(latest)} kg",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Min: ${formatWeight(min)} kg",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Max: ${formatWeight(max)} kg",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = state.hasChanges
            ) {
                Text(if (state.saved) "Gespeichert ✓" else "Speichern")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WeightChart(
    points: List<WeightHistoryPoint>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMAN) }
    val chartWidth = max(320, points.size * 56).dp
    val minWeight = points.minOf { it.weightKg }
    val maxWeight = points.maxOf { it.weightKg }
    val weightRange = (maxWeight - minWeight).takeIf { it > 0.0 } ?: 1.0

    Card(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.width(chartWidth)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    val left = 16.dp.toPx()
                    val right = size.width - 16.dp.toPx()
                    val top = 16.dp.toPx()
                    val bottom = size.height - 20.dp.toPx()
                    val plotWidth = (right - left).coerceAtLeast(1f)
                    val plotHeight = (bottom - top).coerceAtLeast(1f)
                    val xStep = if (points.size > 1) plotWidth / (points.size - 1) else 0f

                    for (i in 0..3) {
                        val y = top + (plotHeight / 3f) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(left, y),
                            end = Offset(right, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val path = Path()
                    points.forEachIndexed { index, point ->
                        val x = left + index * xStep
                        val normalized = ((point.weightKg - minWeight) / weightRange).toFloat()
                        val y = bottom - (normalized * plotHeight)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    points.forEachIndexed { index, point ->
                        val x = left + index * xStep
                        val normalized = ((point.weightKg - minWeight) / weightRange).toFloat()
                        val y = bottom - (normalized * plotHeight)
                        drawCircle(
                            color = pointColor,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = points.first().date.format(dateFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = points.last().date.format(dateFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun filterWeightHistory(
    points: List<WeightHistoryPoint>,
    range: WeightRange
): List<WeightHistoryPoint> {
    if (points.isEmpty()) return emptyList()
    val sorted = points.sortedBy { it.date }
    val days = range.days ?: return sorted
    val maxDate = sorted.last().date
    val minDate = maxDate.minusDays(days - 1)
    return sorted.filter { !it.date.isBefore(minDate) }
}

private fun formatWeight(value: Double): String {
    return String.format(Locale.GERMAN, "%.1f", value)
}
