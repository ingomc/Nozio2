package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.FoodDao
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.remote.OpenFoodFactsApi
import de.ingomc.nozio.data.remote.OpenFoodFactsProduct

class FoodRepository(
    private val api: OpenFoodFactsApi,
    private val foodDao: FoodDao
) {
    suspend fun searchFood(query: String): List<FoodItem> {
        if (query.isBlank()) return emptyList()

        return try {
            val response = api.searchProducts(searchTerms = query)
            val foodItems = response.products
                .filter { it.hasValidData() }
                .map { it.toFoodItem() }

            if (foodItems.isNotEmpty()) {
                val ids = foodDao.insertAll(foodItems)
                foodItems.mapIndexed { index, item -> item.copy(id = ids[index]) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            // Fallback to local cache if network fails
            foodDao.searchByName(query)
        }
    }

    private fun OpenFoodFactsProduct.hasValidData(): Boolean {
        val name = productNameDe ?: productName
        return !name.isNullOrBlank() && nutriments != null &&
                (nutriments.energyKcal100g != null && nutriments.energyKcal100g >= 0)
    }

    private fun OpenFoodFactsProduct.toFoodItem(): FoodItem {
        val displayName = buildString {
            val name = productNameDe?.takeIf { it.isNotBlank() } ?: productName ?: "Unbekannt"
            append(name)
            if (!brands.isNullOrBlank()) {
                append(" ($brands)")
            }
        }
        return FoodItem(
            name = displayName,
            caloriesPer100g = nutriments?.energyKcal100g ?: 0.0,
            proteinPer100g = nutriments?.proteins100g ?: 0.0,
            fatPer100g = nutriments?.fat100g ?: 0.0,
            carbsPer100g = nutriments?.carbohydrates100g ?: 0.0,
            barcode = barcode,
            source = FoodSource.OPEN_FOOD_FACTS
        )
    }
}

