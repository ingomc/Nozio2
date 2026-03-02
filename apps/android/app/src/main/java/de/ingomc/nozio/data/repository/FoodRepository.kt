package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.FoodDao
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.remote.FoodApi
import de.ingomc.nozio.data.remote.FoodSearchItemDto

class FoodRepository(
    private val api: FoodApi,
    private val foodDao: FoodDao
) {
    suspend fun searchFood(query: String): List<FoodItem> {
        if (query.isBlank()) return emptyList()

        return try {
            val response = api.searchFoods(query = query)
            val foodItems = response.items
                .filter { it.hasValidData() }
                .map { it.toFoodItem() }

            if (foodItems.isNotEmpty()) {
                val ids = foodDao.insertAll(foodItems)
                foodItems.mapIndexed { index, item -> item.copy(id = ids[index]) }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            foodDao.searchByName(query)
        }
    }

    suspend fun getFoodByBarcode(barcode: String): FoodItem? {
        if (barcode.isBlank()) return null

        val localMatch = foodDao.getByBarcode(barcode)
        if (localMatch != null) return localMatch

        return try {
            val response = api.getFoodByBarcode(barcode = barcode)
            val product = response.item.takeIf { it.hasValidData() } ?: return null

            val foodItem = product.toFoodItem()
            val id = foodDao.insert(foodItem)
            foodItem.copy(id = id)
        } catch (_: Exception) {
            foodDao.getByBarcode(barcode)
        }
    }

    private fun FoodSearchItemDto.hasValidData(): Boolean {
        return displayName.isNotBlank() && caloriesPer100g >= 0
    }

    private fun FoodSearchItemDto.toFoodItem(): FoodItem {
        return FoodItem(
            name = displayName,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            fatPer100g = fatPer100g,
            carbsPer100g = carbsPer100g,
            barcode = barcode,
            servingSize = servingSize,
            servingQuantity = servingQuantity,
            packageSize = packageSize,
            packageQuantity = packageQuantity,
            source = FoodSource.SELF_HOSTED_OFF
        )
    }
}
