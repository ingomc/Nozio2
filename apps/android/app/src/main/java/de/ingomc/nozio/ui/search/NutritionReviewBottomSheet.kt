package de.ingomc.nozio.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.ingomc.nozio.ui.common.bringIntoViewOnFocus
import de.ingomc.nozio.ui.theme.nozioColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionReviewBottomSheet(
    scanResult: NutritionScanResult,
    applyButtonLabel: String,
    onDismiss: () -> Unit,
    onApply: (CustomFoodDraft) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f

    val caloriesDetected = scanResult.fields[NutritionFieldKey.CALORIES]
    val proteinDetected = scanResult.fields[NutritionFieldKey.PROTEIN]
    val carbsDetected = scanResult.fields[NutritionFieldKey.CARBS]
    val fatDetected = scanResult.fields[NutritionFieldKey.FAT]
    val sugarDetected = scanResult.fields[NutritionFieldKey.SUGAR]

    var name by rememberSaveable(scanResult.rawText) { mutableStateOf(scanResult.productName.orEmpty()) }
    var brand by rememberSaveable(scanResult.rawText) { mutableStateOf(scanResult.brand.orEmpty()) }

    var includeCalories by rememberSaveable(scanResult.rawText) { mutableStateOf(caloriesDetected != null) }
    var includeProtein by rememberSaveable(scanResult.rawText) { mutableStateOf(proteinDetected != null) }
    var includeCarbs by rememberSaveable(scanResult.rawText) { mutableStateOf(carbsDetected != null) }
    var includeFat by rememberSaveable(scanResult.rawText) { mutableStateOf(fatDetected != null) }
    var includeSugar by rememberSaveable(scanResult.rawText) { mutableStateOf(sugarDetected != null) }

    var calories by rememberSaveable(scanResult.rawText) {
        mutableStateOf(caloriesDetected?.let { NutritionLabelParser.formatValue(it.value) }.orEmpty())
    }
    var protein by rememberSaveable(scanResult.rawText) {
        mutableStateOf(proteinDetected?.let { NutritionLabelParser.formatValue(it.value) }.orEmpty())
    }
    var carbs by rememberSaveable(scanResult.rawText) {
        mutableStateOf(carbsDetected?.let { NutritionLabelParser.formatValue(it.value) }.orEmpty())
    }
    var fat by rememberSaveable(scanResult.rawText) {
        mutableStateOf(fatDetected?.let { NutritionLabelParser.formatValue(it.value) }.orEmpty())
    }
    var sugar by rememberSaveable(scanResult.rawText) {
        mutableStateOf(sugarDetected?.let { NutritionLabelParser.formatValue(it.value) }.orEmpty())
    }

    val caloriesValue = parseDecimalInput(calories)
    val proteinValue = parseDecimalInput(protein)
    val carbsValue = parseDecimalInput(carbs)
    val fatValue = parseDecimalInput(fat)
    val sugarValue = parseDecimalInput(sugar)

    val canApply = (!includeCalories || (caloriesValue != null && caloriesValue >= 0)) &&
        (!includeProtein || (proteinValue != null && proteinValue >= 0)) &&
        (!includeCarbs || (carbsValue != null && carbsValue >= 0)) &&
        (!includeFat || (fatValue != null && fatValue >= 0)) &&
        (!includeSugar || (sugarValue != null && sugarValue >= 0))

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
                text = "Scan pruefen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Erkannte Werte anpassen und in Eigenes Produkt uebernehmen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            if (scanResult.warnings.isNotEmpty()) {
                scanResult.warnings.forEach { warning ->
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name optional") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus()
                    .padding(bottom = 10.dp)
            )

            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Marke optional") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus()
                    .padding(bottom = 12.dp)
            )

            ReviewValueField(
                label = "Kalorien pro 100g",
                unit = "kcal",
                value = calories,
                include = includeCalories,
                confidence = caloriesDetected?.confidence,
                onIncludeChange = { includeCalories = it },
                onValueChange = { calories = it }
            )
            ReviewValueField(
                label = "Eiweiss pro 100g",
                unit = "g",
                value = protein,
                include = includeProtein,
                confidence = proteinDetected?.confidence,
                onIncludeChange = { includeProtein = it },
                onValueChange = { protein = it }
            )
            ReviewValueField(
                label = "Kohlenhydrate pro 100g",
                unit = "g",
                value = carbs,
                include = includeCarbs,
                confidence = carbsDetected?.confidence,
                onIncludeChange = { includeCarbs = it },
                onValueChange = { carbs = it }
            )
            ReviewValueField(
                label = "Fett pro 100g",
                unit = "g",
                value = fat,
                include = includeFat,
                confidence = fatDetected?.confidence,
                onIncludeChange = { includeFat = it },
                onValueChange = { fat = it }
            )
            ReviewValueField(
                label = "Davon Zucker pro 100g",
                unit = "g",
                value = sugar,
                include = includeSugar,
                confidence = sugarDetected?.confidence,
                onIncludeChange = { includeSugar = it },
                onValueChange = { sugar = it }
            )

            Button(
                onClick = {
                    onApply(
                        CustomFoodDraft(
                            name = name.ifBlank { null },
                            brand = brand.ifBlank { null },
                            caloriesPer100g = caloriesValue.takeIf { includeCalories },
                            proteinPer100g = proteinValue.takeIf { includeProtein },
                            carbsPer100g = carbsValue.takeIf { includeCarbs },
                            fatPer100g = fatValue.takeIf { includeFat },
                            sugarPer100g = sugarValue.takeIf { includeSugar }
                        )
                    )
                },
                enabled = canApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(applyButtonLabel)
            }
        }
    }
}

@Composable
private fun ReviewValueField(
    label: String,
    unit: String,
    value: String,
    include: Boolean,
    confidence: Float?,
    onIncludeChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (confidence != null) {
                    Text(
                        text = "${(confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Switch(checked = include, onCheckedChange = onIncludeChange)
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = include,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text(unit) },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnFocus()
        )
    }
}
