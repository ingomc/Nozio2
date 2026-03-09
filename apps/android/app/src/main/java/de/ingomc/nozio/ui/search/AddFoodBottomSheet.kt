package de.ingomc.nozio.ui.search

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import kotlin.math.roundToInt
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.ui.common.bringIntoViewOnFocus

private enum class AmountUnitType {
    GRAM,
    MILLILITER,
    PORTION,
    PACKAGE
}

private data class AmountUnit(
    val type: AmountUnitType,
    val label: String,
    val multiplier: Double = 1.0
)

private data class QuickAmountOption(
    val amount: Double,
    val unit: AmountUnit
)

private const val DEFAULT_AMOUNT_UNIT_LABEL = "g"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddFoodBottomSheet(
    food: FoodItem,
    preselectedMealType: MealType?,
    onDismiss: () -> Unit,
    onAdd: (MealType, Double, String) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f
    val availableAmountUnits = remember(food) { buildAmountUnits(food) }
    var amountText by remember { mutableStateOf("100") }
    var amount by remember { mutableDoubleStateOf(100.0) }
    var selectedAmountUnit by remember { mutableStateOf(availableAmountUnits.first()) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var selectedMealType by remember { mutableStateOf(resolveInitialMealType(preselectedMealType)) }
    val normalizedAmount = remember(amount, selectedAmountUnit) { amount * selectedAmountUnit.multiplier }

    val quickAmounts = remember(food, availableAmountUnits) {
        buildQuickAmountOptions(availableAmountUnits)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
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
            food.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = "Produktbild",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                            is AsyncImagePainter.State.Error -> ProductImagePlaceholder()
                            else -> ProductImageShimmer()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } ?: run {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    ProductImagePlaceholder()
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (food.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (food.isFavorite) "Als Favorit entfernen" else "Als Favorit markieren",
                        tint = if (food.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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

            // Meal type selection
            Text(
                text = "Mahlzeit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MealType.entries) { mealType ->
                    FilterChip(
                        selected = selectedMealType == mealType,
                        onClick = { selectedMealType = mealType },
                        colors = solidSelectionChipColors(),
                        label = { Text(mealType.displayName) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Amount input
            Text(
                text = "Menge",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedAmountUnit.type == AmountUnitType.PORTION || selectedAmountUnit.type == AmountUnitType.PACKAGE) {
                    IconButton(
                        onClick = {
                            val nextAmount = (amount - 1.0).coerceAtLeast(0.0)
                            amount = nextAmount
                            amountText = formatAmountValue(nextAmount)
                        },
                        enabled = amount > 0,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Menge verringern"
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { value ->
                        amountText = value
                        amount = value.toDoubleOrNull() ?: 0.0
                    },
                    label = { Text("Wert") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .bringIntoViewOnFocus()
                )

                if (selectedAmountUnit.type == AmountUnitType.PORTION || selectedAmountUnit.type == AmountUnitType.PACKAGE) {
                    IconButton(
                        onClick = {
                            val nextAmount = amount + 1.0
                            amount = nextAmount
                            amountText = formatAmountValue(nextAmount)
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Menge erhöhen"
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = unitMenuExpanded,
                    onExpandedChange = { unitMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAmountUnit.label,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Einheit") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(
                                type = MenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            )
                            .width(148.dp)
                    )

                    DropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false }
                    ) {
                        availableAmountUnits.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.label) },
                                onClick = {
                                    selectedAmountUnit = unit
                                    val defaultAmount = defaultAmountForUnit(unit)
                                    amount = defaultAmount
                                    amountText = formatAmountValue(defaultAmount)
                                    unitMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick amount chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                quickAmounts.forEach { quickAmount ->
                    FilterChip(
                        selected = amount == quickAmount.amount && selectedAmountUnit == quickAmount.unit,
                        onClick = {
                            selectedAmountUnit = quickAmount.unit
                            amount = quickAmount.amount
                            amountText = formatAmountValue(quickAmount.amount)
                        },
                        colors = solidSelectionChipColors(),
                        label = { Text(formatQuickAmount(food, quickAmount)) }
                    )
                }
            }

            // Calculated nutrition
            if (amount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val cal = food.caloriesPer100g * normalizedAmount / 100.0
                val prot = food.proteinPer100g * normalizedAmount / 100.0
                val fat = food.fatPer100g * normalizedAmount / 100.0
                val carbs = food.carbsPer100g * normalizedAmount / 100.0

                Text(
                    text = "Für ${formatAmountWithUnit(amount, selectedAmountUnit, food)}: ${cal.toInt()} kcal · E ${prot.toInt()}g · F ${fat.toInt()}g · K ${carbs.toInt()}g",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Add button
            Button(
                onClick = {
                    onAdd(
                        selectedMealType,
                        normalizedAmount,
                        formatAmountWithUnit(amount, selectedAmountUnit, food)
                    )
                },
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

@Composable
private fun ProductImageShimmer() {
    val transition = rememberInfiniteTransition(label = "productImageShimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing)
        ),
        label = "productImageShimmerOffset"
    )
    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(shimmerX - 320f, 0f),
        end = Offset(shimmerX, 0f)
    )

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .background(shimmerBrush)
    )
}

@Composable
private fun ProductImagePlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Kein Produktbild",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildMetaLine(food: FoodItem): String {
    val parts = mutableListOf<String>()
    food.servingSize?.takeIf { it.isNotBlank() }?.let { parts += "Portion $it" }
        ?: food.servingQuantity?.takeIf { it > 0 }?.let { parts += "Portion ${formatAmountValue(it)} g" }
    food.packageSize?.takeIf { it.isNotBlank() }?.let { parts += "Packung $it" }
        ?: food.packageQuantity?.takeIf { it > 0 }?.let { parts += "Packung ${formatAmountValue(it)} g" }
    return parts.joinToString(" · ")
}

private fun formatQuickAmount(food: FoodItem, option: QuickAmountOption): String {
    val rounded = formatAmountValue(option.amount)
    return when (option.unit.type) {
        AmountUnitType.PORTION -> {
            val suffix = if (kotlin.math.abs(option.amount - 1.0) < 0.01) "Portion" else "Portionen"
            val size = food.servingSize?.takeIf { it.isNotBlank() } ?: "${formatAmountValue(option.unit.multiplier)} g"
            "$rounded $suffix"
                .let { "$it ($size)" }
        }
        AmountUnitType.PACKAGE -> {
            val suffix = if (kotlin.math.abs(option.amount - 1.0) < 0.01) "Packung" else "Packungen"
            val size = food.packageSize?.takeIf { it.isNotBlank() } ?: "${formatAmountValue(option.unit.multiplier)} g"
            "$rounded $suffix"
                .let { "$it ($size)" }
        }
        AmountUnitType.GRAM -> "${rounded}g"
        AmountUnitType.MILLILITER -> "${rounded}ml"
    }
}

internal fun formatQuickAmount(food: FoodItem, amount: Double, unit: String): String {
    val normalizedUnit = unit.lowercase()
    val packageQuantity = food.packageQuantity
    return if (packageQuantity != null && kotlin.math.abs(packageQuantity - amount) < 0.01) {
        "1x Packung (${formatAmountValue(packageQuantity)}$normalizedUnit)"
    } else {
        "${formatAmountValue(amount)}$normalizedUnit"
    }
}

private fun buildQuickAmountOptions(units: List<AmountUnit>): List<QuickAmountOption> = buildList {
    units.firstOrNull { it.type == AmountUnitType.GRAM }?.let { gramUnit ->
        add(QuickAmountOption(100.0, gramUnit))
    }
    units.firstOrNull { it.type == AmountUnitType.MILLILITER }?.let { milliliterUnit ->
        add(QuickAmountOption(100.0, milliliterUnit))
    }
    units.firstOrNull { it.type == AmountUnitType.PORTION }?.let { portionUnit ->
        add(QuickAmountOption(1.0, portionUnit))
    }
    units.firstOrNull { it.type == AmountUnitType.PACKAGE }?.let { packageUnit ->
        add(QuickAmountOption(1.0, packageUnit))
    }
}

private fun buildAmountUnits(food: FoodItem): List<AmountUnit> = buildList {
    add(AmountUnit(type = AmountUnitType.GRAM, label = DEFAULT_AMOUNT_UNIT_LABEL))
    add(AmountUnit(type = AmountUnitType.MILLILITER, label = "ml"))
    food.servingQuantity?.takeIf { it > 0 }?.let {
        add(AmountUnit(type = AmountUnitType.PORTION, label = "Portion", multiplier = it))
    }
    food.packageQuantity?.takeIf { it > 0 }?.let {
        add(AmountUnit(type = AmountUnitType.PACKAGE, label = "Packung", multiplier = it))
    }
}

private fun defaultAmountForUnit(unit: AmountUnit): Double = when (unit.type) {
    AmountUnitType.GRAM, AmountUnitType.MILLILITER -> 100.0
    AmountUnitType.PORTION, AmountUnitType.PACKAGE -> 1.0
}

private fun formatAmountWithUnit(amount: Double, unit: AmountUnit, food: FoodItem): String {
    val roundedAmount = formatAmountValue(amount)
    return when (unit.type) {
        AmountUnitType.GRAM -> "${roundedAmount}g"
        AmountUnitType.MILLILITER -> "${roundedAmount}ml"
        AmountUnitType.PORTION -> {
            val suffix = if (kotlin.math.abs(amount - 1.0) < 0.01) "Portion" else "Portionen"
            val size = food.servingSize?.takeIf { it.isNotBlank() } ?: "${formatAmountValue(unit.multiplier)} g"
            "$roundedAmount $suffix ($size)"
        }
        AmountUnitType.PACKAGE -> {
            val suffix = if (kotlin.math.abs(amount - 1.0) < 0.01) "Packung" else "Packungen"
            val size = food.packageSize?.takeIf { it.isNotBlank() } ?: "${formatAmountValue(unit.multiplier)} g"
            "$roundedAmount $suffix ($size)"
        }
    }
}

@Composable
private fun solidSelectionChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
)

private fun formatAmountValue(amount: Double): String {
    val rounded = amount.roundToInt().toDouble()
    return if (kotlin.math.abs(amount - rounded) < 0.01) {
        rounded.roundToInt().toString()
    } else {
        amount.toString()
    }
}
