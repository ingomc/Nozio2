package de.ingomc.nozio.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FoodSearchResponseDto(
    val items: List<FoodSearchItemDto> = emptyList(),
    @SerialName("totalEstimated") val totalEstimated: Int = 0
)

@Serializable
data class FoodBarcodeResponseDto(
    val item: FoodSearchItemDto
)

@Serializable
data class CreateCustomFoodRequestDto(
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

@Serializable
data class CreateCustomFoodResponseDto(
    val item: FoodSearchItemDto
)

@Serializable
data class VisionNutritionParseRequestDto(
    val imageBase64: String,
    val locale: String = "de"
)

@Serializable
data class VisionNutritionParseResponseDto(
    val name: String? = null,
    val brand: String? = null,
    val caloriesPer100g: Double? = null,
    val proteinPer100g: Double? = null,
    val carbsPer100g: Double? = null,
    val fatPer100g: Double? = null,
    val sugarPer100g: Double? = null,
    val confidence: Double,
    val model: String,
    val warnings: List<String> = emptyList()
)

@Serializable
data class FoodSearchItemDto(
    val id: String,
    val name: String,
    val brand: String? = null,
    val displayName: String,
    val imageUrl: String? = null,
    val barcode: String? = null,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val sugarPer100g: Double = 0.0,
    val servingSize: String? = null,
    val servingQuantity: Double? = null,
    val packageSize: String? = null,
    val packageQuantity: Double? = null,
    val source: String
)
