package de.ingomc.nozio.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import de.ingomc.nozio.data.repository.CustomFoodInput
import de.ingomc.nozio.ui.common.bringIntoViewOnFocus
import de.ingomc.nozio.ui.theme.nozioColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCustomFoodBottomSheet(
    isSubmitting: Boolean,
    initial: CustomFoodDraft? = null,
    prefilledBarcode: String? = null,
    onScanNutrition: (() -> Unit)? = null,
    onScanBarcode: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (CustomFoodInput) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    var name by rememberSaveable(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var brand by rememberSaveable(initial) { mutableStateOf(initial?.brand.orEmpty()) }
    var barcode by rememberSaveable(initial) { mutableStateOf("") }
    var calories by rememberSaveable(initial) { mutableStateOf(initial?.caloriesPer100g?.let(::formatDraftNumber).orEmpty()) }
    var protein by rememberSaveable(initial) { mutableStateOf(initial?.proteinPer100g?.let(::formatDraftNumber).orEmpty()) }
    var fat by rememberSaveable(initial) { mutableStateOf(initial?.fatPer100g?.let(::formatDraftNumber).orEmpty()) }
    var carbs by rememberSaveable(initial) { mutableStateOf(initial?.carbsPer100g?.let(::formatDraftNumber).orEmpty()) }
    var sugar by rememberSaveable(initial) { mutableStateOf(initial?.sugarPer100g?.let(::formatDraftNumber).orEmpty()) }
    var servingSize by rememberSaveable(initial) { mutableStateOf("") }
    var servingQuantity by rememberSaveable(initial) { mutableStateOf("") }
    var packageSize by rememberSaveable(initial) { mutableStateOf("") }
    var packageQuantity by rememberSaveable(initial) { mutableStateOf("") }
    LaunchedEffect(prefilledBarcode) {
        if (!prefilledBarcode.isNullOrBlank()) {
            barcode = prefilledBarcode.filter(Char::isDigit)
        }
    }

    val caloriesValue = parseDecimalInput(calories)
    val proteinValue = parseDecimalInput(protein)
    val fatValue = parseDecimalInput(fat)
    val carbsValue = parseDecimalInput(carbs)
    val sugarValue = parseDecimalInput(sugar)
    val servingQuantityValue = parseDecimalInput(servingQuantity)
    val packageQuantityValue = parseDecimalInput(packageQuantity)
    val hasServingQuantity = servingQuantityValue != null
    val hasPackageQuantity = packageQuantityValue != null
    val servingQuantityInvalid = servingQuantity.isNotBlank() && servingQuantityValue == null
    val packageQuantityInvalid = packageQuantity.isNotBlank() && packageQuantityValue == null
    val macrosValid =
        proteinValue != null && proteinValue >= 0 &&
            fatValue != null && fatValue >= 0 &&
            carbsValue != null && carbsValue >= 0 &&
            sugarValue != null && sugarValue >= 0 &&
            sugarValue <= carbsValue

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
                text = "Eigenes Produkt",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Lege ein eigenes Produkt an, damit du es später schnell wiederfinden und hinzufügen kannst.",
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
                    Text(
                        text = "Naehrwert-Tabelle scannen",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            ProductField(name, "Name", onValueChange = { name = it })
            ProductField(brand, "Marke optional", onValueChange = { brand = it })
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it.filter(Char::isDigit) },
                label = { Text("Barcode optional") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                trailingIcon = if (onScanBarcode != null) {
                    {
                        IconButton(onClick = onScanBarcode) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Barcode scannen"
                            )
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier
                    .bringIntoViewOnFocus()
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )
            ProductField(
                calories,
                "Kalorien pro 100g",
                placeholder = "z. B. 450",
                suffix = "kcal",
                keyboardType = KeyboardType.Number,
                onValueChange = { calories = it }
            )
            ProductField(
                protein,
                "Eiweiss pro 100g",
                placeholder = "z. B. 22,5",
                suffix = "g",
                keyboardType = KeyboardType.Number,
                onValueChange = { protein = it }
            )
            ProductField(
                carbs,
                "Kohlenhydrate pro 100g",
                placeholder = "z. B. 55",
                suffix = "g",
                keyboardType = KeyboardType.Number,
                onValueChange = { carbs = it }
            )
            ProductField(
                sugar,
                "Davon Zucker pro 100g",
                placeholder = "z. B. 12",
                suffix = "g",
                keyboardType = KeyboardType.Number,
                isError = sugarValue != null && carbsValue != null && sugarValue > carbsValue,
                supportingText = if (sugarValue != null && carbsValue != null && sugarValue > carbsValue) {
                    "Zucker kann nicht hoeher als Kohlenhydrate sein."
                } else {
                    null
                },
                onValueChange = { sugar = it }
            )
            ProductField(
                fat,
                "Fett pro 100g",
                placeholder = "z. B. 9",
                suffix = "g",
                keyboardType = KeyboardType.Number,
                onValueChange = { fat = it }
            )
            ProductField(
                servingQuantity,
                "Portionsmenge in g (optional)",
                placeholder = "z. B. 30",
                suffix = "g",
                keyboardType = KeyboardType.Number,
                isError = servingQuantityInvalid,
                supportingText = if (servingQuantityInvalid) {
                    "Bitte eine gueltige Zahl eingeben (Komma oder Punkt)."
                } else {
                    null
                },
                onValueChange = { servingQuantity = it }
            )
            ProductField(
                servingSize,
                "Portionsname (optional)",
                placeholder = "z. B. 1 Riegel",
                enabled = hasServingQuantity,
                supportingText = if (!hasServingQuantity) {
                    "Wird nur gespeichert, wenn eine Portionsmenge gesetzt ist."
                } else {
                    null
                },
                onValueChange = { servingSize = it }
            )
            ProductField(
                packageQuantity,
                "Packungsmenge in g (optional)",
                placeholder = "z. B. 250",
                suffix = "g",
                keyboardType = KeyboardType.Number,
                isError = packageQuantityInvalid,
                supportingText = if (packageQuantityInvalid) {
                    "Bitte eine gueltige Zahl eingeben (Komma oder Punkt)."
                } else {
                    null
                },
                onValueChange = { packageQuantity = it }
            )
            ProductField(
                packageSize,
                "Packungsname (optional)",
                placeholder = "z. B. 1 Packung",
                enabled = hasPackageQuantity,
                supportingText = if (!hasPackageQuantity) {
                    "Wird nur gespeichert, wenn eine Packungsmenge gesetzt ist."
                } else {
                    null
                },
                onValueChange = { packageSize = it }
            )

            Button(
                onClick = {
                    onSave(
                        CustomFoodInput(
                            name = name.trim(),
                            brand = brand.ifBlank { null },
                            barcode = barcode.ifBlank { null },
                            caloriesPer100g = caloriesValue ?: 0.0,
                            proteinPer100g = proteinValue ?: 0.0,
                            fatPer100g = fatValue ?: 0.0,
                            carbsPer100g = carbsValue ?: 0.0,
                            sugarPer100g = sugarValue ?: 0.0,
                            servingSize = if (hasServingQuantity) {
                                servingSize.ifBlank { null }
                            } else {
                                null
                            },
                            servingQuantity = servingQuantityValue,
                            packageSize = if (hasPackageQuantity) {
                                packageSize.ifBlank { null }
                            } else {
                                null
                            },
                            packageQuantity = packageQuantityValue
                        )
                    )
                },
                enabled = !isSubmitting &&
                    name.isNotBlank() &&
                    caloriesValue != null &&
                    caloriesValue >= 0 &&
                    !servingQuantityInvalid &&
                    !packageQuantityInvalid &&
                    macrosValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(48.dp)
            ) {
                if (isSubmitting) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Text("Produkt speichern")
                }
            }
        }
    }
}

@Composable
private fun ProductField(
    value: String,
    label: String,
    placeholder: String? = null,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(label) },
        placeholder = placeholder?.let { text -> { Text(text) } },
        suffix = suffix?.let { { Text(it) } },
        isError = isError,
        supportingText = supportingText?.let { text -> { Text(text) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier
            .bringIntoViewOnFocus()
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    )
}

internal fun parseDecimalInput(value: String): Double? {
    val normalized = value
        .trim()
        .replace(',', '.')
        .replace(" ", "")
    if (normalized.isBlank()) return null
    return normalized.toDoubleOrNull()
}

private fun formatDraftNumber(value: Double): String {
    return NutritionLabelParser.formatValue(value)
}
