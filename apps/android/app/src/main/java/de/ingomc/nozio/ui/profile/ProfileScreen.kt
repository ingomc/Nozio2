package de.ingomc.nozio.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.ingomc.nozio.ui.theme.nozioColors
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
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
    onEditGoals: () -> Unit,
    onOpenLegalInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val appBarState = rememberTopAppBarState()
    val appBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(appBarState)
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
                    text = "Profil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                ProfileSummaryCard(
                    currentWeightKg = state.latestWeightKg,
                    bodyFatPercent = state.latestBodyFatPercent,
                    calorieGoal = state.calorieGoal.toDoubleOrNull()?.toInt(),
                    todaySteps = state.todaySteps
                )
            }

            item {
                SectionHeader(
                    title = "Mein Fortschritt",
                    actionLabel = null,
                    onActionClick = null
                )
            }

            item {
                WeightProgressCard(
                    points = filteredWeightHistory,
                    startWeightKg = state.goalStartWeightKg.toDoubleOrNull(),
                    latestWeightKg = state.latestWeightKg,
                    targetWeightKg = state.goalTargetWeightKg.toDoubleOrNull(),
                    trendDeltaKg = state.weightTrendDeltaKg,
                    weeksEstimate = state.weightTrendWeeksEstimate
                )
            }

            item {
                SectionHeader(
                    title = "Meine Ziele",
                    actionLabel = "Bearbeiten",
                    onActionClick = onEditGoals
                )
            }

            item {
                GoalsSummaryCard(items = state.goalSummaryItems)
            }

            item {
                Text(
                    text = "Gewichtsverlauf",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
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
            }

            if (filteredWeightHistory.isEmpty()) {
                item {
                    Text(
                        text = "Noch keine Gewichts-Einträge vorhanden.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item {
                    WeightChart(
                        points = filteredWeightHistory,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
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
            }

            item {
                OutlinedButton(
                    onClick = onOpenLegalInfo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Rechtliche Hinweise")
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditGoalsScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val proteinGoal = state.proteinGoal.toDoubleOrNull() ?: 0.0
    val fatGoal = state.fatGoal.toDoubleOrNull() ?: 0.0
    val carbsGoal = state.carbsGoal.toDoubleOrNull() ?: 0.0
    val macroCalories = (proteinGoal * 4.0) + (carbsGoal * 4.0) + (fatGoal * 9.0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Meine Ziele bearbeiten",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            )
        )

        EditableGoalsSection(
            state = state,
            macroCalories = macroCalories,
            onCalorieGoalChange = viewModel::onCalorieGoalChange,
            onProteinGoalChange = viewModel::onProteinGoalChange,
            onFatGoalChange = viewModel::onFatGoalChange,
            onCarbsGoalChange = viewModel::onCarbsGoalChange,
            onGoalStartWeightKgChange = viewModel::onGoalStartWeightKgChange,
            onGoalTargetWeightKgChange = viewModel::onGoalTargetWeightKgChange,
            onSave = viewModel::save
        )
    }
}

@Composable
private fun ProfileSummaryCard(
    currentWeightKg: Double?,
    bodyFatPercent: Double?,
    calorieGoal: Int?,
    todaySteps: Long
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Dein Profil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Gewicht ${currentWeightKg?.let { formatWeight(it) + " kg" } ?: "--"} · KFA ${bodyFatPercent?.let { formatWeight(it) + " %" } ?: "--"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Kalorienziel ${calorieGoal ?: 0} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetric(
                    label = "kcal Ziel",
                    value = "${calorieGoal ?: 0}",
                    modifier = Modifier.weight(1f)
                )
                SummaryMetric(
                    label = "Schritte heute",
                    value = todaySteps.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String?,
    onActionClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionLabel.uppercase(Locale.GERMAN))
            }
        }
    }
}

@Composable
private fun WeightProgressCard(
    points: List<WeightHistoryPoint>,
    startWeightKg: Double?,
    latestWeightKg: Double?,
    targetWeightKg: Double?,
    trendDeltaKg: Double?,
    weeksEstimate: Int?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (points.isEmpty()) {
                Text(
                    text = "Noch keine Gewichts-Einträge für die Auswertung.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val startWeight = startWeightKg ?: points.first().weightKg
            val currentWeight = latestWeightKg ?: points.last().weightKg
            val targetWeight = targetWeightKg ?: currentWeight
            val denominator = (targetWeight - startWeight)
            val progress = if (abs(denominator) < 0.001) 1f
            else ((currentWeight - startWeight) / denominator).toFloat().coerceIn(0f, 1f)

            val headline = when {
                trendDeltaKg == null -> "Noch nicht genug Daten für einen Trend"
                abs(trendDeltaKg) < 0.05 -> "Dein Gewicht ist aktuell unverändert"
                trendDeltaKg > 0 -> "Du hast ${formatWeight(abs(trendDeltaKg))} kg zugenommen"
                else -> "Du hast ${formatWeight(abs(trendDeltaKg))} kg abgenommen"
            }

            Text(
                text = headline,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            weeksEstimate?.let {
                Text(
                    text = "Noch $it Wochen bis zum Ziel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.nozioColors.ringTrack.copy(alpha = 0.35f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Start ${formatWeight(startWeight)} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ziel ${formatWeight(targetWeight)} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Aktuell ${formatWeight(currentWeight)} kg",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GoalsSummaryCard(items: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun EditableGoalsSection(
    state: ProfileUiState,
    macroCalories: Double,
    onCalorieGoalChange: (String) -> Unit,
    onProteinGoalChange: (String) -> Unit,
    onFatGoalChange: (String) -> Unit,
    onCarbsGoalChange: (String) -> Unit,
    onGoalStartWeightKgChange: (String) -> Unit,
    onGoalTargetWeightKgChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Tagesziele bearbeiten",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = state.calorieGoal,
            onValueChange = onCalorieGoalChange,
            label = { Text("Kalorienziel (kcal)") },
            supportingText = {
                Text(
                    text = "Aus Makro-Zielen: ${macroCalories.toInt()} kcal",
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
                onValueChange = onProteinGoalChange,
                label = { Text("EW (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.fatGoal,
                onValueChange = onFatGoalChange,
                label = { Text("Fett (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.carbsGoal,
                onValueChange = onCarbsGoalChange,
                label = { Text("KH (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.goalStartWeightKg,
                onValueChange = onGoalStartWeightKgChange,
                label = { Text("Startgewicht (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.goalTargetWeightKg,
                onValueChange = onGoalTargetWeightKgChange,
                label = { Text("Zielgewicht (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = state.hasChanges
        ) {
            Text(if (state.saved) "Gespeichert ✓" else "Speichern")
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
    val selectedPointColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val tooltipBgColor = MaterialTheme.colorScheme.surfaceVariant
    val tooltipTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMAN) }
    val tooltipDateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yy", Locale.GERMAN) }
    val density = LocalDensity.current
    val tooltipDateTextSizePx = with(density) { 11.sp.toPx() }
    val tooltipWeightTextSizePx = with(density) { 13.sp.toPx() }
    val chartWidth = max(320, points.size * 56).dp
    val firstDay = points.first().date.toEpochDay()
    val lastDay = points.last().date.toEpochDay()
    val dayRange = (lastDay - firstDay).coerceAtLeast(1L)
    val minWeight = points.minOf { it.weightKg }
    val maxWeight = points.maxOf { it.weightKg }
    val paddedMinWeight = minWeight - 1.0
    val paddedMaxWeight = maxWeight + 1.0
    val weightRange = (paddedMaxWeight - paddedMinWeight).takeIf { it > 0.0 } ?: 2.0
    val midWeight = paddedMinWeight + (weightRange / 2.0)
    var chartSize by remember(points) { mutableStateOf(IntSize.Zero) }
    var selectedPointIndex by remember(points) { mutableIntStateOf(-1) }

    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .height(220.dp)
                    .padding(start = 4.dp, end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatWeight(paddedMaxWeight),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatWeight(midWeight),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatWeight(paddedMinWeight),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.width(chartWidth)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .onSizeChanged { chartSize = it }
                            .pointerInput(points, paddedMinWeight, weightRange, chartSize) {
                                detectTapGestures { tapOffset ->
                                    if (points.isEmpty() || chartSize.width <= 0 || chartSize.height <= 0) return@detectTapGestures
                                    val left = with(density) { 16.dp.toPx() }
                                    val right = chartSize.width.toFloat() - with(density) { 16.dp.toPx() }
                                    val top = with(density) { 16.dp.toPx() }
                                    val bottom = chartSize.height.toFloat() - with(density) { 20.dp.toPx() }
                                    val plotWidth = (right - left).coerceAtLeast(1f)
                                    val plotHeight = (bottom - top).coerceAtLeast(1f)

                                    var closestIndex = 0
                                    var closestDistance = Float.MAX_VALUE
                                    points.forEachIndexed { index, point ->
                                        val dayOffset = (point.date.toEpochDay() - firstDay).toFloat()
                                        val x = left + (dayOffset / dayRange.toFloat()) * plotWidth
                                        val normalized = ((point.weightKg - paddedMinWeight) / weightRange).toFloat()
                                        val y = bottom - (normalized * plotHeight)
                                        val dx = tapOffset.x - x
                                        val dy = tapOffset.y - y
                                        val distance = (dx * dx) + (dy * dy)
                                        if (distance < closestDistance) {
                                            closestDistance = distance
                                            closestIndex = index
                                        }
                                    }
                                    selectedPointIndex = closestIndex
                                }
                            }
                    ) {
                        val left = 16.dp.toPx()
                        val right = size.width - 16.dp.toPx()
                        val plotTop = 16.dp.toPx()
                        val plotBottom = size.height - 20.dp.toPx()
                        val plotWidth = (right - left).coerceAtLeast(1f)
                        val plotHeight = (plotBottom - plotTop).coerceAtLeast(1f)

                        for (i in 0..3) {
                            val y = plotTop + (plotHeight / 3f) * i
                            drawLine(
                                color = gridColor,
                                start = Offset(left, y),
                                end = Offset(right, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val path = Path()
                        points.forEachIndexed { index, point ->
                            val dayOffset = (point.date.toEpochDay() - firstDay).toFloat()
                            val x = left + (dayOffset / dayRange.toFloat()) * plotWidth
                            val normalized = ((point.weightKg - paddedMinWeight) / weightRange).toFloat()
                            val y = plotBottom - (normalized * plotHeight)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        points.forEachIndexed { index, point ->
                            val dayOffset = (point.date.toEpochDay() - firstDay).toFloat()
                            val x = left + (dayOffset / dayRange.toFloat()) * plotWidth
                            val normalized = ((point.weightKg - paddedMinWeight) / weightRange).toFloat()
                            val y = plotBottom - (normalized * plotHeight)
                            drawCircle(
                                color = if (index == selectedPointIndex) selectedPointColor else pointColor,
                                radius = if (index == selectedPointIndex) 6.dp.toPx() else 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }

                        if (selectedPointIndex in points.indices) {
                            val selectedPoint = points[selectedPointIndex]
                            val tooltipDate = selectedPoint.date.format(tooltipDateFormatter)
                            val tooltipWeight = "${formatWeight(selectedPoint.weightKg)} kg"
                            val selectedDayOffset = (selectedPoint.date.toEpochDay() - firstDay).toFloat()
                            val x = left + (selectedDayOffset / dayRange.toFloat()) * plotWidth
                            val normalized = ((selectedPoint.weightKg - paddedMinWeight) / weightRange).toFloat()
                            val y = plotBottom - (normalized * plotHeight)

                            drawIntoCanvas { canvas ->
                                val datePaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    color = tooltipTextColor.toArgb()
                                    textSize = tooltipDateTextSizePx
                                }
                                val weightPaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    color = tooltipTextColor.toArgb()
                                    textSize = tooltipWeightTextSizePx
                                    typeface = android.graphics.Typeface.create(
                                        android.graphics.Typeface.DEFAULT,
                                        android.graphics.Typeface.BOLD
                                    )
                                }

                                val dateWidth = datePaint.measureText(tooltipDate)
                                val weightWidth = weightPaint.measureText(tooltipWeight)
                                val bubbleTextWidth = maxOf(dateWidth, weightWidth)
                                val dateHeight = datePaint.fontMetrics.run { this.bottom - this.top }
                                val weightHeight = weightPaint.fontMetrics.run { this.bottom - this.top }
                                val lineSpacing = 2.dp.toPx()
                                val paddingH = 8.dp.toPx()
                                val paddingV = 6.dp.toPx()
                                val bubbleWidth = bubbleTextWidth + (paddingH * 2f)
                                val bubbleHeight = dateHeight + lineSpacing + weightHeight + (paddingV * 2f)
                                val bubbleX = (x - bubbleWidth / 2f).coerceIn(left, right - bubbleWidth)
                                val bubbleY = (y - bubbleHeight - 10.dp.toPx()).coerceAtLeast(plotTop)

                                drawRoundRect(
                                    color = tooltipBgColor,
                                    topLeft = Offset(bubbleX, bubbleY),
                                    size = androidx.compose.ui.geometry.Size(bubbleWidth, bubbleHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
                                )

                                val dateBaselineY = bubbleY + paddingV - datePaint.fontMetrics.top
                                canvas.nativeCanvas.drawText(
                                    tooltipDate,
                                    bubbleX + paddingH,
                                    dateBaselineY,
                                    datePaint
                                )
                                canvas.nativeCanvas.drawText(
                                    tooltipWeight,
                                    bubbleX + paddingH,
                                    dateBaselineY + lineSpacing + weightHeight,
                                    weightPaint
                                )
                            }
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
