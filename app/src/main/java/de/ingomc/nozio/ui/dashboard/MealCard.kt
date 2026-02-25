package de.ingomc.nozio.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.ingomc.nozio.data.local.DiaryEntryWithFood
import de.ingomc.nozio.data.local.MealType

@Composable
fun MealCard(
    mealType: MealType,
    entries: List<DiaryEntryWithFood>,
    onAddClick: () -> Unit,
    onDeleteEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCalories = entries.sumOf { it.calories }

    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Lebensmittel hinzufügen",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Food entries
            if (entries.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                entries.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.foodName,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                            Text(
                                text = "${entry.amountInGrams.toInt()}g · ${entry.calories.toInt()} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteEntry(entry.entryId) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eintrag löschen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

