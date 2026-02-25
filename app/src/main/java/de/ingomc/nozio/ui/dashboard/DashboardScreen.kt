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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.ingomc.nozio.data.local.MealType
import java.time.LocalDate
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
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

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
                        text = today.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            // Calorie Ring
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CalorieRing(
                        consumed = state.totalCalories,
                        goal = state.preferences.calorieGoal
                    )
                }
            }

            // Macro Bars
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    MacroBar(
                        label = "Kohlenhydrate",
                        consumed = state.totalCarbs,
                        goal = state.preferences.carbsGoal,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                }
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

