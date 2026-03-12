package de.ingomc.nozio.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.ui.common.bringIntoViewOnFocus
import de.ingomc.nozio.ui.theme.nozioColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddBottomSheet(
    preselectedMealType: MealType?,
    initial: QuickAddDraft? = null,
    onScanNutrition: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onAdd: (MealType, Double, Double, Double, Double, String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    var name by rememberSaveable(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var calories by rememberSaveable(initial) { mutableStateOf(initial?.calories?.let(NutritionLabelParser::formatValue).orEmpty()) }
    var protein by rememberSaveable(initial) { mutableStateOf(initial?.protein?.let(NutritionLabelParser::formatValue).orEmpty()) }
    var fat by rememberSaveable(initial) { mutableStateOf(initial?.fat?.let(NutritionLabelParser::formatValue).orEmpty()) }
    var carbs by rememberSaveable(initial) { mutableStateOf(initial?.carbs?.let(NutritionLabelParser::formatValue).orEmpty()) }
    var selectedMealType by remember { mutableStateOf(resolveInitialMealType(preselectedMealType)) }
    LaunchedEffect(preselectedMealType) {
        selectedMealType = resolveInitialMealType(preselectedMealType)
    }

    val caloriesValue = parseDecimalInput(calories)
    val proteinValue = parseDecimalInput(protein) ?: 0.0
    val fatValue = parseDecimalInput(fat) ?: 0.0
    val carbsValue = parseDecimalInput(carbs) ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.nozioColors.surface2,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .verticalScroll(rememberScrollState())
                .imePadding()
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
            if (onScanNutrition != null) {
                Button(
                    onClick = onScanNutrition,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null
                    )
                    Text(text = "Naehrwert-Tabelle scannen", modifier = Modifier.padding(start = 8.dp))
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name optional") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus()
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
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewOnFocus()
    )
}
