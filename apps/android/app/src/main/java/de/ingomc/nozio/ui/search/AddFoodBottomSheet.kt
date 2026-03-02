package de.ingomc.nozio.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.MealType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddFoodBottomSheet(
    food: FoodItem,
    preselectedMealType: MealType?,
    onDismiss: () -> Unit,
    onAdd: (MealType, Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("100") }
    var amount by remember { mutableDoubleStateOf(100.0) }
    var selectedMealType by remember { mutableStateOf(preselectedMealType ?: MealType.BREAKFAST) }

    val quickAmounts = buildList {
        addAll(listOf(50.0, 100.0, 150.0, 200.0, 250.0, 300.0))
        food.servingQuantity?.takeIf { it > 0 }?.let { add(it) }
        food.packageQuantity?.takeIf { it > 0 }?.let { add(it) }
    }.distinct().sorted()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Food name
            Text(
                text = food.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Nutrition per 100g
            Text(
                text = "Pro 100g: ${food.caloriesPer100g.toInt()} kcal · E ${food.proteinPer100g.toInt()}g · F ${food.fatPer100g.toInt()}g · K ${food.carbsPer100g.toInt()}g",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (food.servingSize != null || food.packageSize != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = buildMetaLine(food),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Amount input
            Text(
                text = "Menge",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { value ->
                    amountText = value
                    amount = value.toDoubleOrNull() ?: 0.0
                },
                label = { Text("Menge (g/ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick amount chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                quickAmounts.forEach { quickAmount ->
                    FilterChip(
                        selected = amount == quickAmount,
                        onClick = {
                            amount = quickAmount
                            amountText = quickAmount.roundToInt().toString()
                        },
                        label = { Text(formatQuickAmount(food, quickAmount)) }
                    )
                }
            }

            // Calculated nutrition
            if (amount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val cal = food.caloriesPer100g * amount / 100.0
                val prot = food.proteinPer100g * amount / 100.0
                val fat = food.fatPer100g * amount / 100.0
                val carbs = food.carbsPer100g * amount / 100.0

                Text(
                    text = "Für ${amount.toInt()}g: ${cal.toInt()} kcal · E ${prot.toInt()}g · F ${fat.toInt()}g · K ${carbs.toInt()}g",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Meal type selection
            Text(
                text = "Mahlzeit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MealType.entries.forEach { mealType ->
                    FilterChip(
                        selected = selectedMealType == mealType,
                        onClick = { selectedMealType = mealType },
                        label = { Text(mealType.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Add button
            Button(
                onClick = { onAdd(selectedMealType, amount) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = amount > 0
            ) {
                Text("Hinzufügen")
            }
        }
    }
}

private fun buildMetaLine(food: FoodItem): String {
    val parts = mutableListOf<String>()
    food.servingSize?.let { parts += "Portion $it" }
    food.packageSize?.let { parts += "Packung $it" }
    return parts.joinToString(" · ")
}

private fun formatQuickAmount(food: FoodItem, amount: Double): String {
    val rounded = amount.roundToInt()
    return when {
        food.packageQuantity != null && kotlin.math.abs(food.packageQuantity - amount) < 0.01 ->
            food.packageSize?.let { "1x $it" } ?: "Packung"
        food.servingQuantity != null && kotlin.math.abs(food.servingQuantity - amount) < 0.01 ->
            food.servingSize?.let { "1x $it" } ?: "Portion"
        else -> "${rounded}g"
    }
}

