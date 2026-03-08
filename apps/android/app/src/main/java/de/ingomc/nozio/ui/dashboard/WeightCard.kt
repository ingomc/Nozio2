package de.ingomc.nozio.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun WeightCard(
    weightKg: Double?,
    weightDate: LocalDate?,
    isWeightFallback: Boolean,
    bodyFatPercent: Double?,
    bodyFatDate: LocalDate?,
    isBodyFatFallback: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN) }
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Gewicht bearbeiten",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricValue(
                        label = "Gewicht",
                        value = weightKg?.let { "${formatWeight(it)} kg" } ?: "--,- kg",
                        standDate = if (isWeightFallback) weightDate else null,
                        subdued = isWeightFallback,
                        dateFormatter = dateFormatter,
                        modifier = Modifier
                    )
                    MetricValue(
                        label = "KFA",
                        value = bodyFatPercent?.let { "${formatWeight(it)} %" } ?: "--,- %",
                        standDate = if (isBodyFatFallback) bodyFatDate else null,
                        subdued = isBodyFatFallback,
                        dateFormatter = dateFormatter,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricValue(
    label: String,
    value: String,
    standDate: LocalDate?,
    subdued: Boolean,
    dateFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    val valueColor = if (subdued) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = if (subdued) FontWeight.SemiBold else FontWeight.Bold,
            color = valueColor
        )
        if (standDate != null) {
            Text(
                text = "Stand: ${standDate.format(dateFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightInputBottomSheet(
    initialWeightKg: Double,
    initialBodyFatPercent: Double,
    latestWeightEntryDate: LocalDate?,
    latestWeightEntryWeightKg: Double?,
    latestBodyFatEntryDate: LocalDate?,
    latestBodyFatEntryPercent: Double?,
    onDismiss: () -> Unit,
    onSave: (Double?, Double?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialWeight = initialWeightKg.coerceIn(20.0, 400.0)
    val initialBodyFat = initialBodyFatPercent.coerceIn(3.0, 60.0)
    var selectedWeight by remember { mutableDoubleStateOf(initialWeight) }
    var selectedBodyFat by remember { mutableDoubleStateOf(initialBodyFat) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN) }
    val weightChanged = hasMeaningfulDelta(initialWeight, selectedWeight)
    val bodyFatChanged = hasMeaningfulDelta(initialBodyFat, selectedBodyFat)
    val hasAnyPendingChanges = weightChanged || bodyFatChanged

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Gewicht & KFA eintragen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (latestWeightEntryDate != null && latestWeightEntryWeightKg != null) {
                    "Letztes Gewicht am ${latestWeightEntryDate.format(dateFormatter)}: ${formatWeight(latestWeightEntryWeightKg)} kg"
                } else {
                    "Noch kein früheres Gewicht"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (latestBodyFatEntryDate != null && latestBodyFatEntryPercent != null) {
                    "Letzter KFA am ${latestBodyFatEntryDate.format(dateFormatter)}: ${formatWeight(latestBodyFatEntryPercent)} %"
                } else {
                    "Noch kein früherer KFA"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            AdjustableMetricRow(
                title = "Gewicht",
                value = "${formatWeight(selectedWeight)} kg",
                hasPendingChange = weightChanged,
                onDecrease = { selectedWeight = (selectedWeight - 0.1).coerceIn(20.0, 400.0) },
                onIncrease = { selectedWeight = (selectedWeight + 0.1).coerceIn(20.0, 400.0) },
                decreaseDescription = "0,1 kg weniger",
                increaseDescription = "0,1 kg mehr"
            )

            Spacer(modifier = Modifier.height(12.dp))

            AdjustableMetricRow(
                title = "KFA",
                value = "${formatWeight(selectedBodyFat)} %",
                hasPendingChange = bodyFatChanged,
                onDecrease = { selectedBodyFat = (selectedBodyFat - 0.1).coerceIn(3.0, 60.0) },
                onIncrease = { selectedBodyFat = (selectedBodyFat + 0.1).coerceIn(3.0, 60.0) },
                decreaseDescription = "0,1 % weniger",
                increaseDescription = "0,1 % mehr"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val updatedWeight = selectedWeight.takeIf { weightChanged }
                    val updatedBodyFat = selectedBodyFat.takeIf { bodyFatChanged }
                    onSave(updatedWeight, updatedBodyFat)
                },
                enabled = hasAnyPendingChanges,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Werte speichern")
            }
        }
    }
}

@Composable
private fun AdjustableMetricRow(
    title: String,
    value: String,
    hasPendingChange: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String,
    increaseDescription: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (hasPendingChange) {
            DirtyIndicatorDot()
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, tonalElevation = 0.dp, shadowElevation = 0.dp) {
            IconButton(onClick = onDecrease) {
                Icon(Icons.Default.Remove, contentDescription = decreaseDescription)
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Surface(shape = CircleShape, tonalElevation = 0.dp, shadowElevation = 0.dp) {
            IconButton(onClick = onIncrease) {
                Icon(Icons.Default.Add, contentDescription = increaseDescription)
            }
        }
    }
}

@Composable
private fun DirtyIndicatorDot(size: Dp = 6.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
    )
}

private fun hasMeaningfulDelta(initialValue: Double, currentValue: Double): Boolean {
    return abs(currentValue - initialValue) >= 0.05
}

private fun formatWeight(value: Double): String {
    return String.format(Locale.GERMAN, "%.1f", value)
}
