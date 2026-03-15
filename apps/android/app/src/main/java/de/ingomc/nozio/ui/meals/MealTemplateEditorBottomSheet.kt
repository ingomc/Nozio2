package de.ingomc.nozio.ui.meals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.local.RecipeAmountUnit
import de.ingomc.nozio.ui.theme.nozioColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTemplateEditorBottomSheet(
    state: EditorState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onIngredientAmountChange: (Int, String) -> Unit,
    onIngredientUnitChange: (Int, RecipeAmountUnit) -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onAddIngredient: () -> Unit,
    onSave: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.nozioColors.surface2,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = if (state.templateId == 0L) "Neues Rezept" else "Rezept bearbeiten",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Rezeptname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Text(
                    text = "Default-Mahlzeit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MealType.entries.forEach { mealType ->
                        FilterChip(
                            selected = state.defaultMealType == mealType,
                            onClick = { onMealTypeChange(mealType) },
                            label = { Text(mealType.displayName) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    text = "Zutaten",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(state.ingredients) { index, ingredient ->
                IngredientRow(
                    ingredient = ingredient,
                    onAmountChange = { onIngredientAmountChange(index, it) },
                    onUnitChange = { onIngredientUnitChange(index, it) },
                    onRemove = { onRemoveIngredient(index) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                OutlinedButton(
                    onClick = onAddIngredient,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Zutat hinzufuegen")
                } 
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.name.isNotBlank() && state.ingredients.isNotEmpty()
                ) {
                    Text("Speichern")
                }
            }
        }
    }
}

@Composable
private fun IngredientRow(
    ingredient: EditorIngredient,
    onAmountChange: (String) -> Unit,
    onUnitChange: (RecipeAmountUnit) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ingredient.foodName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ingredient.amountValue,
                    onValueChange = onAmountChange,
                    modifier = Modifier.width(80.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                UnitChipRow(
                    selected = ingredient.amountUnit,
                    onSelect = onUnitChange
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Entfernen")
        }
    }
}

@Composable
fun UnitChipRow(
    selected: RecipeAmountUnit,
    onSelect: (RecipeAmountUnit) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        RecipeAmountUnit.entries.forEach { unit ->
            FilterChip(
                selected = selected == unit,
                onClick = { onSelect(unit) },
                label = { Text(unit.displayName) }
            )
        }
    }
}
