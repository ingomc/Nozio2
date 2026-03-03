package de.ingomc.nozio.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import de.ingomc.nozio.data.repository.CustomFoodInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCustomFoodBottomSheet(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (CustomFoodInput) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("") }
    var servingQuantity by remember { mutableStateOf("") }
    var packageSize by remember { mutableStateOf("") }
    var packageQuantity by remember { mutableStateOf("") }

    val caloriesValue = calories.toDoubleOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .verticalScroll(rememberScrollState())
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

            ProductField(name, "Name", onValueChange = { name = it })
            ProductField(brand, "Marke optional", onValueChange = { brand = it })
            ProductField(
                barcode,
                "Barcode optional",
                keyboardType = KeyboardType.Number,
                onValueChange = { barcode = it.filter(Char::isDigit) }
            )
            ProductField(
                calories,
                "Kalorien pro 100g",
                suffix = "kcal",
                keyboardType = KeyboardType.Number,
                onValueChange = { calories = it }
            )
            ProductField(
                protein,
                "Eiweiss pro 100g",
                suffix = "g",
                keyboardType = KeyboardType.Number,
                onValueChange = { protein = it }
            )
            ProductField(
                carbs,
                "Kohlenhydrate pro 100g",
                suffix = "g",
                keyboardType = KeyboardType.Number,
                onValueChange = { carbs = it }
            )
            ProductField(
                fat,
                "Fett pro 100g",
                suffix = "g",
                keyboardType = KeyboardType.Number,
                onValueChange = { fat = it }
            )
            ProductField(
                servingSize,
                "Portionsanzeige optional",
                onValueChange = { servingSize = it }
            )
            ProductField(
                servingQuantity,
                "Portionsmenge optional",
                suffix = "g",
                keyboardType = KeyboardType.Number,
                onValueChange = { servingQuantity = it }
            )
            ProductField(
                packageSize,
                "Packungsanzeige optional",
                onValueChange = { packageSize = it }
            )
            ProductField(
                packageQuantity,
                "Packungsmenge optional",
                suffix = "g",
                keyboardType = KeyboardType.Number,
                onValueChange = { packageQuantity = it }
            )

            Button(
                onClick = {
                    onSave(
                        CustomFoodInput(
                            name = name.trim(),
                            brand = brand.ifBlank { null },
                            barcode = barcode.ifBlank { null },
                            caloriesPer100g = caloriesValue ?: 0.0,
                            proteinPer100g = protein.toDoubleOrNull() ?: 0.0,
                            fatPer100g = fat.toDoubleOrNull() ?: 0.0,
                            carbsPer100g = carbs.toDoubleOrNull() ?: 0.0,
                            servingSize = servingSize.ifBlank { null },
                            servingQuantity = servingQuantity.toDoubleOrNull(),
                            packageSize = packageSize.ifBlank { null },
                            packageQuantity = packageQuantity.toDoubleOrNull()
                        )
                    )
                },
                enabled = !isSubmitting && name.isNotBlank() && caloriesValue != null && caloriesValue >= 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(48.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
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
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    )
}
