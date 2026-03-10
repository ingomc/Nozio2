package de.ingomc.nozio.ui.profile

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.ingomc.nozio.ui.theme.nozioColors
import de.ingomc.nozio.ui.theme.expressiveTopAppBarColors
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

internal enum class WeightRange(val label: String, val days: Long?) {
    DAYS_14("14T", 14),
    DAYS_60("60T", 60),
    DAYS_180("180T", 180),
    YEAR_1("1J", 365)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onEditGoals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val appContext = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val appBarState = rememberTopAppBarState()
    val appBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(appBarState)
    var selectedWeightRange by remember { mutableStateOf(WeightRange.DAYS_60) }
    var pendingProfileImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var profileImageLoading by remember { mutableStateOf(false) }
    val profileImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        profileImageLoading = true
        coroutineScope.launch {
            val decodedBitmap = withContext(Dispatchers.IO) {
                decodeProfileImageForEditing(appContext, uri)
            }
            pendingProfileImageBitmap = decodedBitmap
            profileImageLoading = false
        }
    }

    val filteredBodyMetricHistory = remember(state.bodyMetricHistory, selectedWeightRange) {
        filterBodyMetricHistory(state.bodyMetricHistory, selectedWeightRange)
    }
    val chartBodyMetricHistory = remember(state.bodyMetricHistory, selectedWeightRange) {
        filterBodyMetricHistory(state.bodyMetricHistory, selectedWeightRange, spanMultiplier = 3)
    }
    val filteredWeightHistory = remember(filteredBodyMetricHistory) {
        filteredBodyMetricHistory.mapNotNull { point ->
            point.weightKg?.let { WeightHistoryPoint(date = point.date, weightKg = it) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(appBarScrollBehavior.nestedScrollConnection)
    ) {
        TopAppBar(
            colors = expressiveTopAppBarColors(),
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
                    todaySteps = state.todaySteps,
                    profileImagePath = state.profileImagePath,
                    profileImageUpdatedAt = state.profileImageUpdatedAt,
                    isLoadingProfileImage = profileImageLoading,
                    onProfileImageClick = { profileImagePickerLauncher.launch("image/*") }
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
                            label = { Text(range.label) },
                            colors = solidSelectionChipColors()
                        )
                    }
                }
            }

            if (filteredBodyMetricHistory.isEmpty()) {
                item {
                    Text(
                        text = "Noch keine Gewichts- oder KFA-Einträge vorhanden.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item {
                    BodyMetricsChart(
                        points = chartBodyMetricHistory,
                        selectedRange = selectedWeightRange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    val weightValues = filteredBodyMetricHistory.mapNotNull { it.weightKg }
                    val bodyFatValues = filteredBodyMetricHistory.mapNotNull { it.bodyFatPercent }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (weightValues.isNotEmpty()) {
                            val latest = weightValues.last()
                            val min = weightValues.minOrNull() ?: latest
                            val max = weightValues.maxOrNull() ?: latest
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Gewicht letzter: ${formatWeight(latest)} kg",
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
                        if (bodyFatValues.isNotEmpty()) {
                            val latest = bodyFatValues.last()
                            val min = bodyFatValues.minOrNull() ?: latest
                            val max = bodyFatValues.maxOrNull() ?: latest
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "KFA letzter: ${formatWeight(latest)} %",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Min: ${formatWeight(min)} %",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Max: ${formatWeight(max)} %",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        pendingProfileImageBitmap?.let { sourceBitmap ->
            ProfileImageCropDialog(
                sourceBitmap = sourceBitmap,
                onDismiss = { pendingProfileImageBitmap = null },
                onConfirm = { cropSpec ->
                    viewModel.saveProfileImage(sourceBitmap, cropSpec)
                    pendingProfileImageBitmap = null
                }
            )
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
            colors = expressiveTopAppBarColors()
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
    todaySteps: Long,
    profileImagePath: String?,
    profileImageUpdatedAt: Long,
    isLoadingProfileImage: Boolean,
    onProfileImageClick: () -> Unit
) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.nozioColors.surface2
        )
    ) {
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
                        .clip(CircleShape)
                        .clickable(onClick = onProfileImageClick)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImagePath != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(profileImagePath))
                                .memoryCacheKey("profile-image-$profileImageUpdatedAt")
                                .diskCacheKey("profile-image-$profileImageUpdatedAt")
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profilbild",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    if (isLoadingProfileImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    }
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
                    Text(
                        text = if (isLoadingProfileImage) "Bild wird geladen..." else "Profilbild antippen zum Ändern",
                        style = MaterialTheme.typography.bodySmall,
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
private fun ProfileImageCropDialog(
    sourceBitmap: Bitmap,
    onDismiss: () -> Unit,
    onConfirm: (ProfileImageCropSpec) -> Unit
) {
    var zoom by remember(sourceBitmap) { mutableFloatStateOf(1f) }
    var offsetX by remember(sourceBitmap) { mutableFloatStateOf(0f) }
    var offsetY by remember(sourceBitmap) { mutableFloatStateOf(0f) }
    val imageBitmap = remember(sourceBitmap) { sourceBitmap.asImageBitmap() }
    val cropWindow = remember(sourceBitmap.width, sourceBitmap.height, zoom, offsetX, offsetY) {
        computeProfileImageCropWindow(
            imageWidth = sourceBitmap.width,
            imageHeight = sourceBitmap.height,
            cropSpec = ProfileImageCropSpec(
                zoom = zoom,
                offsetX = offsetX,
                offsetY = offsetY
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Profilbild zuschneiden") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawImage(
                            image = imageBitmap,
                            srcOffset = IntOffset(cropWindow.left, cropWindow.top),
                            srcSize = IntSize(cropWindow.size, cropWindow.size),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                        )
                    }
                }

                Text(
                    text = "Zoom ${String.format(Locale.GERMAN, "%.1f", zoom)}x",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = zoom,
                    onValueChange = { zoom = it },
                    valueRange = 1f..3f
                )

                Text(
                    text = "Ausschnitt horizontal",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = offsetX,
                    onValueChange = { offsetX = it },
                    valueRange = -1f..1f,
                    enabled = zoom > 1f
                )

                Text(
                    text = "Ausschnitt vertikal",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = offsetY,
                    onValueChange = { offsetY = it },
                    valueRange = -1f..1f,
                    enabled = zoom > 1f
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        ProfileImageCropSpec(
                            zoom = zoom,
                            offsetX = offsetX,
                            offsetY = offsetY
                        )
                    )
                }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
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
            style = MaterialTheme.typography.titleMedium,
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.nozioColors.surface2
        )
    ) {
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.nozioColors.surface2
        )
    ) {
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
private fun BodyMetricsChart(
    points: List<BodyMetricHistoryPoint>,
    selectedRange: WeightRange,
    modifier: Modifier = Modifier
) {
    val weightLineColor = MaterialTheme.colorScheme.primary
    val weightPointColor = MaterialTheme.colorScheme.primary
    val bodyFatLineColor = MaterialTheme.colorScheme.tertiary
    val bodyFatPointColor = MaterialTheme.colorScheme.tertiary
    val selectedPointColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val tooltipBgColor = MaterialTheme.colorScheme.surfaceVariant
    val tooltipTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisTextColorArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMAN) }
    val tooltipDateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yy", Locale.GERMAN) }
    val density = LocalDensity.current
    val tooltipDateTextSizePx = with(density) { 10.sp.toPx() }
    val tooltipValueTextSizePx = with(density) { 12.sp.toPx() }
    val visibleDaysInRange = selectedRange.days ?: 365L
    val totalDaysInWindow = visibleDaysInRange * 3L
    val latestDate = points.last().date
    val windowStartDate = latestDate.minusDays(totalDaysInWindow - 1)
    val chartPoints = remember(points, selectedRange, windowStartDate, latestDate) {
        aggregateChartPointsForRange(points, selectedRange, windowStartDate, latestDate)
    }
    val firstDay = windowStartDate.toEpochDay()
    val dayRange = (totalDaysInWindow - 1).coerceAtLeast(1L)
    val weightValues = chartPoints.mapNotNull { it.weightKg }
    val bodyFatValues = chartPoints.mapNotNull { it.bodyFatPercent }
    val weightAxis = computeAxisRange(weightValues, pad = 1.0)
    val bodyFatAxis = computeAxisRange(bodyFatValues, pad = 1.0)
    val xAxisTicks = remember(points, selectedRange) {
        buildXAxisTicks(
            start = windowStartDate,
            end = latestDate,
            range = selectedRange
        )
    }
    val chartScrollState = rememberScrollState()
    var chartSize by remember(chartPoints) { mutableStateOf(IntSize.Zero) }
    var selectedPointIndex by remember(chartPoints) { mutableIntStateOf(-1) }

    LaunchedEffect(selectedRange, chartScrollState.maxValue) {
        if (chartScrollState.maxValue > 0) {
            chartScrollState.scrollTo(chartScrollState.maxValue)
        }
    }

    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (weightAxis != null) {
                AxisLabelsColumn(
                    top = formatWeight(weightAxis.max),
                    mid = formatWeight(weightAxis.mid),
                    bottom = formatWeight(weightAxis.min),
                    modifier = Modifier
                        .height(220.dp)
                        .padding(start = 4.dp, end = 8.dp),
                    horizontalAlignment = Alignment.End
                )
            }

            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val chartWidth = maxWidth * 3
                Box(modifier = Modifier.horizontalScroll(chartScrollState)) {
                    Column(modifier = Modifier.width(chartWidth)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .onSizeChanged { chartSize = it }
                            .pointerInput(chartPoints, chartSize, weightAxis, bodyFatAxis) {
                                detectTapGestures { tapOffset ->
                                    if (chartPoints.isEmpty() || chartSize.width <= 0 || chartSize.height <= 0) return@detectTapGestures
                                    val left = with(density) { 16.dp.toPx() }
                                    val right = chartSize.width.toFloat() - with(density) { 16.dp.toPx() }
                                    val top = with(density) { 16.dp.toPx() }
                                    val bottom = chartSize.height.toFloat() - with(density) { 20.dp.toPx() }
                                    val plotWidth = (right - left).coerceAtLeast(1f)
                                    val plotHeight = (bottom - top).coerceAtLeast(1f)

                                    var closestIndex = 0
                                    var closestDistance = Float.MAX_VALUE
                                    chartPoints.forEachIndexed { index, point ->
                                        val dayOffset = (point.date.toEpochDay() - firstDay).toFloat()
                                        val x = left + (dayOffset / dayRange.toFloat()) * plotWidth
                                        point.weightKg?.let { weightValue ->
                                            val y = yForValue(weightValue, weightAxis, bottom, plotHeight)
                                            val dx = tapOffset.x - x
                                            val dy = tapOffset.y - y
                                            val distance = (dx * dx) + (dy * dy)
                                            if (distance < closestDistance) {
                                                closestDistance = distance
                                                closestIndex = index
                                            }
                                        }
                                        point.bodyFatPercent?.let { bodyFatValue ->
                                            val y = yForValue(bodyFatValue, bodyFatAxis, bottom, plotHeight)
                                            val dx = tapOffset.x - x
                                            val dy = tapOffset.y - y
                                            val distance = (dx * dx) + (dy * dy)
                                            if (distance < closestDistance) {
                                                closestDistance = distance
                                                closestIndex = index
                                            }
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

                        drawMetricLine(
                            points = chartPoints,
                            firstDay = firstDay,
                            dayRange = dayRange,
                            left = left,
                            plotWidth = plotWidth,
                            plotBottom = plotBottom,
                            plotHeight = plotHeight,
                            axis = weightAxis,
                            valueSelector = { it.weightKg },
                            color = weightLineColor
                        )
                        drawMetricLine(
                            points = chartPoints,
                            firstDay = firstDay,
                            dayRange = dayRange,
                            left = left,
                            plotWidth = plotWidth,
                            plotBottom = plotBottom,
                            plotHeight = plotHeight,
                            axis = bodyFatAxis,
                            valueSelector = { it.bodyFatPercent },
                            color = bodyFatLineColor
                        )

                        drawMetricPoints(
                            points = chartPoints,
                            selectedPointIndex = selectedPointIndex,
                            firstDay = firstDay,
                            dayRange = dayRange,
                            left = left,
                            plotWidth = plotWidth,
                            plotBottom = plotBottom,
                            plotHeight = plotHeight,
                            axis = weightAxis,
                            valueSelector = { it.weightKg },
                            defaultColor = weightPointColor,
                            selectedColor = selectedPointColor
                        )
                        drawMetricPoints(
                            points = chartPoints,
                            selectedPointIndex = selectedPointIndex,
                            firstDay = firstDay,
                            dayRange = dayRange,
                            left = left,
                            plotWidth = plotWidth,
                            plotBottom = plotBottom,
                            plotHeight = plotHeight,
                            axis = bodyFatAxis,
                            valueSelector = { it.bodyFatPercent },
                            defaultColor = bodyFatPointColor,
                            selectedColor = selectedPointColor
                        )

                        if (selectedPointIndex in chartPoints.indices) {
                            val selectedPoint = chartPoints[selectedPointIndex]
                            val selectedDayOffset = (selectedPoint.date.toEpochDay() - firstDay).toFloat()
                            val x = left + (selectedDayOffset / dayRange.toFloat()) * plotWidth
                            val y = selectedPoint.weightKg?.let {
                                yForValue(it, weightAxis, plotBottom, plotHeight)
                            } ?: selectedPoint.bodyFatPercent?.let {
                                yForValue(it, bodyFatAxis, plotBottom, plotHeight)
                            } ?: plotTop

                            drawIntoCanvas { canvas ->
                                val labelPaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    color = tooltipTextColor.toArgb()
                                    textSize = tooltipDateTextSizePx
                                }
                                val valuePaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    color = tooltipTextColor.toArgb()
                                    textSize = tooltipValueTextSizePx
                                    typeface = android.graphics.Typeface.create(
                                        android.graphics.Typeface.DEFAULT,
                                        android.graphics.Typeface.BOLD
                                    )
                                }
                                val tooltipLines = buildList {
                                    add(selectedPoint.date.format(tooltipDateFormatter))
                                    selectedPoint.weightKg?.let { add("Gewicht: ${formatWeight(it)} kg") }
                                    selectedPoint.bodyFatPercent?.let { add("KFA: ${formatWeight(it)} %") }
                                }
                                val bubbleTextWidth = tooltipLines.maxOfOrNull { text ->
                                    if (text.startsWith("Gewicht:") || text.startsWith("KFA:")) {
                                        valuePaint.measureText(text)
                                    } else {
                                        labelPaint.measureText(text)
                                    }
                                } ?: 0f
                                val lineHeight = max(
                                    labelPaint.fontMetrics.run { bottom - top },
                                    valuePaint.fontMetrics.run { bottom - top }
                                )
                                val lineSpacing = 3.dp.toPx()
                                val paddingH = 8.dp.toPx()
                                val paddingV = 6.dp.toPx()
                                val bubbleWidth = bubbleTextWidth + (paddingH * 2f)
                                val bubbleHeight =
                                    (lineHeight * tooltipLines.size) +
                                        (lineSpacing * (tooltipLines.size - 1)) +
                                        (paddingV * 2f)
                                val bubbleX = (x - bubbleWidth / 2f).coerceIn(left, right - bubbleWidth)
                                val bubbleY = (y - bubbleHeight - 10.dp.toPx()).coerceAtLeast(plotTop)

                                drawRoundRect(
                                    color = tooltipBgColor,
                                    topLeft = Offset(bubbleX, bubbleY),
                                    size = androidx.compose.ui.geometry.Size(bubbleWidth, bubbleHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx(), 10.dp.toPx())
                                )

                                var baselineY = bubbleY + paddingV - labelPaint.fontMetrics.top
                                tooltipLines.forEach { line ->
                                    val isValueLine = line.startsWith("Gewicht:") || line.startsWith("KFA:")
                                    val paint = if (isValueLine) valuePaint else labelPaint
                                    canvas.nativeCanvas.drawText(line, bubbleX + paddingH, baselineY, paint)
                                    baselineY += lineHeight + lineSpacing
                                }
                            }
                        }
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    ) {
                        val left = 16.dp.toPx()
                        val right = size.width - 16.dp.toPx()
                        val plotWidth = (right - left).coerceAtLeast(1f)
                        val textPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = axisTextColorArgb
                            textSize = with(density) { 10.sp.toPx() }
                        }
                        val minLabelGap = 6.dp.toPx()
                        data class TickLayout(
                            val date: LocalDate,
                            val x: Float,
                            val text: String,
                            val textWidth: Float,
                            val textX: Float
                        )
                        val tickLayouts = xAxisTicks.map { tickDate ->
                            val dayOffset = (tickDate.toEpochDay() - firstDay).toFloat()
                            val x = left + (dayOffset / dayRange.toFloat()) * plotWidth
                            val label = tickDate.format(dateFormatter)
                            val textWidth = textPaint.measureText(label)
                            val textX = (x - textWidth / 2f).coerceIn(left, right - textWidth)
                            TickLayout(
                                date = tickDate,
                                x = x,
                                text = label,
                                textWidth = textWidth,
                                textX = textX
                            )
                        }
                        val lastIndex = tickLayouts.lastIndex
                        val endLabelStart = tickLayouts.lastOrNull()?.textX ?: Float.POSITIVE_INFINITY
                        var lastDrawnLabelEnd = Float.NEGATIVE_INFINITY

                        tickLayouts.forEachIndexed { index, tick ->
                            val isBoundary = index == 0 || index == lastIndex
                            if (!isBoundary) {
                                val overlapsPrevious = tick.textX < (lastDrawnLabelEnd + minLabelGap)
                                val overlapsEnd = (tick.textX + tick.textWidth + minLabelGap) > endLabelStart
                                if (overlapsPrevious || overlapsEnd) return@forEachIndexed
                            }
                            drawLine(
                                color = gridColor,
                                start = Offset(tick.x, 0f),
                                end = Offset(tick.x, 6.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(tick.text, tick.textX, 20.dp.toPx(), textPaint)
                            }
                            lastDrawnLabelEnd = max(lastDrawnLabelEnd, tick.textX + tick.textWidth)
                        }
                    }
                    }
                }
            }

            if (bodyFatAxis != null) {
                AxisLabelsColumn(
                    top = formatWeight(bodyFatAxis.max),
                    mid = formatWeight(bodyFatAxis.mid),
                    bottom = formatWeight(bodyFatAxis.min),
                    modifier = Modifier
                        .height(220.dp)
                        .padding(start = 8.dp, end = 4.dp),
                    horizontalAlignment = Alignment.Start
                )
            }
        }
    }
}

@Composable
private fun AxisLabelsColumn(
    top: String,
    mid: String,
    bottom: String,
    modifier: Modifier,
    horizontalAlignment: Alignment.Horizontal
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = top,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = mid,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = bottom,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class AxisRange(
    val min: Double,
    val max: Double,
    val mid: Double
)

private fun computeAxisRange(values: List<Double>, pad: Double): AxisRange? {
    if (values.isEmpty()) return null
    val min = values.minOrNull() ?: return null
    val max = values.maxOrNull() ?: return null
    val paddedMin = min - pad
    val paddedMax = max + pad
    val range = (paddedMax - paddedMin).takeIf { it > 0.0 } ?: (pad * 2.0)
    val safeMax = paddedMin + range
    val mid = paddedMin + (range / 2.0)
    return AxisRange(min = paddedMin, max = safeMax, mid = mid)
}

private fun yForValue(value: Double, axis: AxisRange?, plotBottom: Float, plotHeight: Float): Float {
    val resolvedAxis = axis ?: return plotBottom
    val range = (resolvedAxis.max - resolvedAxis.min).coerceAtLeast(0.0001)
    val normalized = ((value - resolvedAxis.min) / range).toFloat()
    return plotBottom - (normalized * plotHeight)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMetricLine(
    points: List<BodyMetricHistoryPoint>,
    firstDay: Long,
    dayRange: Long,
    left: Float,
    plotWidth: Float,
    plotBottom: Float,
    plotHeight: Float,
    axis: AxisRange?,
    valueSelector: (BodyMetricHistoryPoint) -> Double?,
    color: Color
) {
    if (axis == null) return
    var activePath: Path? = null
    points.forEach { point ->
        val value = valueSelector(point)
        if (value == null) {
            activePath?.let {
                drawPath(path = it, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
            activePath = null
            return@forEach
        }

        val dayOffset = (point.date.toEpochDay() - firstDay).toFloat()
        val x = left + (dayOffset / dayRange.toFloat()) * plotWidth
        val y = yForValue(value, axis, plotBottom, plotHeight)
        val path = activePath ?: Path().also { it.moveTo(x, y) }
        if (activePath != null) path.lineTo(x, y)
        activePath = path
    }
    activePath?.let {
        drawPath(path = it, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMetricPoints(
    points: List<BodyMetricHistoryPoint>,
    selectedPointIndex: Int,
    firstDay: Long,
    dayRange: Long,
    left: Float,
    plotWidth: Float,
    plotBottom: Float,
    plotHeight: Float,
    axis: AxisRange?,
    valueSelector: (BodyMetricHistoryPoint) -> Double?,
    defaultColor: Color,
    selectedColor: Color
) {
    if (axis == null) return
    points.forEachIndexed { index, point ->
        val value = valueSelector(point) ?: return@forEachIndexed
        val dayOffset = (point.date.toEpochDay() - firstDay).toFloat()
        val x = left + (dayOffset / dayRange.toFloat()) * plotWidth
        val y = yForValue(value, axis, plotBottom, plotHeight)
        drawCircle(
            color = if (index == selectedPointIndex) selectedColor else defaultColor,
            radius = if (index == selectedPointIndex) 6.dp.toPx() else 4.dp.toPx(),
            center = Offset(x, y)
        )
    }
}

internal fun filterBodyMetricHistory(
    points: List<BodyMetricHistoryPoint>,
    range: WeightRange,
    spanMultiplier: Int = 1
): List<BodyMetricHistoryPoint> {
    if (points.isEmpty()) return emptyList()
    val sorted = points.sortedBy { it.date }
    val days = range.days ?: 365L
    val maxDate = sorted.last().date
    val effectiveDays = (days * spanMultiplier.coerceAtLeast(1)).coerceAtLeast(1L)
    val minDate = maxDate.minusDays(effectiveDays - 1)
    return sorted.filter { !it.date.isBefore(minDate) }
}

internal fun buildXAxisTicks(start: LocalDate, end: LocalDate, range: WeightRange): List<LocalDate> {
    if (end.isBefore(start)) return emptyList()
    val ticks = linkedSetOf(start, end)

    when (range) {
        WeightRange.DAYS_14 -> {
            var current = start
            while (!current.isAfter(end)) {
                ticks.add(current)
                current = current.plusDays(2)
            }
        }
        WeightRange.DAYS_60 -> {
            var current = start
            while (!current.isAfter(end)) {
                ticks.add(current)
                current = current.plusDays(7)
            }
        }
        WeightRange.DAYS_180 -> {
            var current = start
            while (!current.isAfter(end)) {
                ticks.add(current)
                current = current.plusDays(14)
            }
        }
        WeightRange.YEAR_1 -> {
            var currentMonth = YearMonth.from(start)
            val endMonth = YearMonth.from(end)
            while (!currentMonth.isAfter(endMonth)) {
                val tick = currentMonth.atDay(1)
                if (!tick.isBefore(start) && !tick.isAfter(end)) {
                    ticks.add(tick)
                }
                currentMonth = currentMonth.plusMonths(1)
            }
        }
    }

    return ticks.toList().sorted()
}

private fun formatWeight(value: Double): String {
    return String.format(Locale.GERMAN, "%.1f", value)
}

internal fun aggregateChartPointsForRange(
    points: List<BodyMetricHistoryPoint>,
    range: WeightRange,
    windowStartDate: LocalDate,
    latestDate: LocalDate
): List<BodyMetricHistoryPoint> {
    if (points.isEmpty()) return emptyList()
    val sorted = points.sortedBy { it.date }
    if (range == WeightRange.DAYS_14) return sorted

    val grouped = when (range) {
        WeightRange.YEAR_1 -> {
            sorted.groupBy { YearMonth.from(it.date) }.toSortedMap().map { (_, bucketPoints) -> bucketPoints }
        }
        else -> {
            val rangeStartEpoch = windowStartDate.toEpochDay()
            sorted.groupBy { point ->
                ((point.date.toEpochDay() - rangeStartEpoch) / 7L).toInt().coerceAtLeast(0)
            }.toSortedMap().map { (_, bucketPoints) -> bucketPoints }
        }
    }

    return grouped.mapNotNull { bucketPoints ->
        val weightValues = bucketPoints.mapNotNull { it.weightKg }
        val bodyFatValues = bucketPoints.mapNotNull { it.bodyFatPercent }
        val avgWeight = weightValues.takeIf { it.isNotEmpty() }?.average()
        val avgBodyFat = bodyFatValues.takeIf { it.isNotEmpty() }?.average()
        if (avgWeight == null && avgBodyFat == null) return@mapNotNull null

        val bucketEndDate = bucketPoints.maxOf { it.date }.coerceAtMost(latestDate)
        BodyMetricHistoryPoint(
            date = bucketEndDate,
            weightKg = avgWeight?.let { (it * 10.0).roundToInt() / 10.0 },
            bodyFatPercent = avgBodyFat?.let { (it * 10.0).roundToInt() / 10.0 }
        )
    }
}

@Composable
private fun solidSelectionChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
)
