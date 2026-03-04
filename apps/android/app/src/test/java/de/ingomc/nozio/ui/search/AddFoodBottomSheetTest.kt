package de.ingomc.nozio.ui.search

import de.ingomc.nozio.data.local.FoodItem
import org.junit.Assert.assertEquals
import org.junit.Test

class AddFoodBottomSheetTest {

    @Test
    fun formatQuickAmount_usesSelectedUnitForGenericAmount() {
        val food = baseFood()

        val label = formatQuickAmount(food, amount = 125.0, unit = "ml")

        assertEquals("125ml", label)
    }

    @Test
    fun formatQuickAmount_usesSelectedUnitForPackageFallback() {
        val food = baseFood().copy(packageQuantity = 250.0, packageSize = null)

        val label = formatQuickAmount(food, amount = 250.0, unit = "ml")

        assertEquals("1x Packung (250ml)", label)
    }

    private fun baseFood() = FoodItem(
        name = "Test Food",
        caloriesPer100g = 100.0,
        proteinPer100g = 10.0,
        fatPer100g = 5.0,
        carbsPer100g = 20.0
    )
}
