package de.ingomc.nozio.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.ingomc.nozio.data.local.MealType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddBottomSheet(
    preselectedMealType: MealType?,
    onDismiss: () -> Unit,
    onAdd: (MealType, Double, Double, Double, Double, String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf(resolveInitialMealType(preselectedMealType)) }

    val caloriesValue = calories.toDoubleOrNull()
    val proteinValue = protein.toDoubleOrNull() ?: 0.0
    val fatValue = fat.toDoubleOrNull() ?: 0.0
    val carbsValue = carbs.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Quick Add",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Trage nur die Gesamtwerte ein. Eiweiss, Fett und Kohlenhydrate sind optional.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name optional") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            MacroField(
                value = calories,
                label = "Kalorien",
                suffix = "kcal",
                onValueChange = { calories = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            MacroField(
                value = protein,
                label = "Eiweiss optional",
                suffix = "g",
                onValueChange = { protein = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            MacroField(
                value = carbs,
                label = "Kohlenhydrate optional",
                suffix = "g",
                onValueChange = { carbs = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            MacroField(
                value = fat,
                label = "Fett optional",
                suffix = "g",
                onValueChange = { fat = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Button(
                onClick = {
                    onAdd(
                        selectedMealType,
                        caloriesValue ?: 0.0,
                        proteinValue,
                        fatValue,
                        carbsValue,
                        name.ifBlank { null }
                    )
                },
                enabled = caloriesValue != null && caloriesValue > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Eintragen")
            }
        }
    }
}

@Composable
private fun MacroField(
    value: String,
    label: String,
    suffix: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
