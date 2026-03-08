package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.FoodDao
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.remote.FoodApi
import de.ingomc.nozio.data.remote.CreateCustomFoodRequestDto
import de.ingomc.nozio.data.remote.FoodSearchItemDto

data class CustomFoodInput(
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val caloriesPer100g: Double,
    val proteinPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val sugarPer100g: Double,
    val servingSize: String? = null,
    val servingQuantity: Double? = null,
    val packageSize: String? = null,
    val packageQuantity: Double? = null
)

class FoodRepository(
    private val api: FoodApi,
    private val foodDao: FoodDao
) {
    suspend fun searchFood(query: String): List<FoodItem> {
        if (query.isBlank()) return emptyList()

        val response = api.searchFoods(query = query)
        return response.items
            .filter { it.hasValidData() }
            .map { it.toFoodItem() }
    }

    suspend fun ensureFoodStored(foodItem: FoodItem): FoodItem {
        if (foodItem.id > 0) return foodItem

        val barcode = foodItem.barcode?.takeIf { it.isNotBlank() }
        if (barcode != null) {
            val localMatch = foodDao.getByBarcode(barcode)
            if (localMatch != null) return localMatch
        }

        val id = foodDao.insert(foodItem)
        return foodItem.copy(id = id)
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

    suspend fun createCustomFood(input: CustomFoodInput): FoodItem {
        val response = api.createCustomFood(
            CreateCustomFoodRequestDto(
                name = input.name.trim(),
                brand = input.brand?.trim()?.ifBlank { null },
                barcode = input.barcode?.filter(Char::isDigit)?.ifBlank { null },
                caloriesPer100g = input.caloriesPer100g,
                proteinPer100g = input.proteinPer100g,
                fatPer100g = input.fatPer100g,
                carbsPer100g = input.carbsPer100g,
                sugarPer100g = input.sugarPer100g,
                servingSize = input.servingSize?.trim()?.ifBlank { null },
                servingQuantity = input.servingQuantity,
                packageSize = input.packageSize?.trim()?.ifBlank { null },
                packageQuantity = input.packageQuantity
            )
        )
        val storedFood = response.item.toFoodItem()
        val id = foodDao.insert(storedFood)
        return storedFood.copy(id = id)
    }

    suspend fun getFavoriteFoods(limit: Int = 50): List<FoodItem> {
        return foodDao.getFavorites(limit)
    }

    suspend fun setFavorite(foodId: Long, isFavorite: Boolean): FoodItem? {
        if (foodId <= 0) return null
        foodDao.setFavorite(foodId = foodId, isFavorite = isFavorite)
        return foodDao.getById(foodId)
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
            imageUrl = imageUrl,
            barcode = barcode,
            servingSize = servingSize,
            servingQuantity = servingQuantity,
            packageSize = packageSize,
            packageQuantity = packageQuantity,
            source = source.toFoodSource()
        )
    }

    private fun String.toFoodSource(): FoodSource {
        return when (this) {
            "CUSTOM" -> FoodSource.CUSTOM
            "SELF_HOSTED_OFF" -> FoodSource.SELF_HOSTED_OFF
            else -> FoodSource.OPEN_FOOD_FACTS
        }
    }
}
