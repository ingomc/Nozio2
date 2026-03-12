package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.FoodDao
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.remote.FoodApi
import de.ingomc.nozio.data.remote.CreateCustomFoodRequestDto
import de.ingomc.nozio.data.remote.FoodSearchItemDto
import de.ingomc.nozio.data.remote.VisionNutritionParseRequestDto
import java.net.SocketTimeoutException
import org.json.JSONObject
import retrofit2.HttpException

data class NutritionParseResult(
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

class VisionScanException(
    val backendCode: String?,
    override val message: String
) : Exception(message)

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
        if (foodItem.id > 0) {
            return if (foodItem.imageUrl.isNullOrBlank() && !foodItem.barcode.isNullOrBlank()) {
                getFoodByBarcode(foodItem.barcode) ?: foodItem
            } else {
                foodItem
            }
        }

        val barcode = foodItem.barcode?.takeIf { it.isNotBlank() }
        if (barcode != null) {
            val localMatch = foodDao.getByBarcode(barcode)
            if (localMatch != null) {
                if (localMatch.imageUrl.isNullOrBlank() && !foodItem.imageUrl.isNullOrBlank()) {
                    val merged = localMatch.copy(imageUrl = foodItem.imageUrl)
                    foodDao.insert(merged)
                    return merged
                }
                return localMatch
            }
        }

        val id = foodDao.insert(foodItem)
        return foodItem.copy(id = id)
    }

    suspend fun getFoodByBarcode(barcode: String): FoodItem? {
        if (barcode.isBlank()) return null

        val localMatch = foodDao.getByBarcode(barcode)
        if (localMatch != null && !localMatch.imageUrl.isNullOrBlank()) return localMatch

        return try {
            val response = api.getFoodByBarcode(barcode = barcode)
            val product = response.item.takeIf { it.hasValidData() } ?: return null

            val foodItem = product.toFoodItem()
            if (localMatch != null) {
                val merged = localMatch.copy(
                    name = foodItem.name,
                    caloriesPer100g = foodItem.caloriesPer100g,
                    proteinPer100g = foodItem.proteinPer100g,
                    fatPer100g = foodItem.fatPer100g,
                    carbsPer100g = foodItem.carbsPer100g,
                    imageUrl = foodItem.imageUrl ?: localMatch.imageUrl,
                    servingSize = foodItem.servingSize ?: localMatch.servingSize,
                    servingQuantity = foodItem.servingQuantity ?: localMatch.servingQuantity,
                    packageSize = foodItem.packageSize ?: localMatch.packageSize,
                    packageQuantity = foodItem.packageQuantity ?: localMatch.packageQuantity,
                    source = foodItem.source
                )
                foodDao.insert(merged)
                merged
            } else {
                val id = foodDao.insert(foodItem)
                foodItem.copy(id = id)
            }
        } catch (_: Exception) {
            localMatch
        }
    }

    suspend fun backfillMissingImageUrls(limit: Int = 120): Int {
        val missingFoods = foodDao.getFoodsMissingImageUrl(limit)
        var updatedCount = 0
        missingFoods.forEach { food ->
            val barcode = food.barcode ?: return@forEach
            val refreshed = getFoodByBarcode(barcode)
            if (!refreshed?.imageUrl.isNullOrBlank()) {
                updatedCount += 1
            }
        }
        return updatedCount
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

    suspend fun parseNutritionFromImage(
        imageBase64: String,
        locale: String = "de"
    ): NutritionParseResult {
        try {
            val response = api.parseNutritionFromImage(
                VisionNutritionParseRequestDto(
                    imageBase64 = imageBase64,
                    locale = locale
                )
            )
            return NutritionParseResult(
                name = response.name,
                brand = response.brand,
                caloriesPer100g = response.caloriesPer100g,
                proteinPer100g = response.proteinPer100g,
                carbsPer100g = response.carbsPer100g,
                fatPer100g = response.fatPer100g,
                sugarPer100g = response.sugarPer100g,
                confidence = response.confidence,
                model = response.model,
                warnings = response.warnings
            )
        } catch (exception: HttpException) {
            throw mapVisionHttpException(exception)
        } catch (_: SocketTimeoutException) {
            throw VisionScanException(
                backendCode = "TIMEOUT",
                message = "Vision-Scan hat zu lange gedauert (Timeout). Bitte erneut versuchen."
            )
        } catch (exception: Exception) {
            throw VisionScanException(
                backendCode = null,
                message = "Vision-Scan fehlgeschlagen (${exception.javaClass.simpleName})."
            )
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

    private fun mapVisionHttpException(exception: HttpException): VisionScanException {
        val status = exception.code()
        val errorBody = exception.response()?.errorBody()?.string().orEmpty()
        val backendCode = extractApiErrorCode(errorBody)
        val backendMessage = extractApiErrorMessage(errorBody)

        val message = when (backendCode) {
            "VISION_UNAVAILABLE" -> {
                val detail = backendMessage?.takeIf { it.isNotBlank() }
                if (detail != null) {
                    "Vision-Service aktuell nicht erreichbar: $detail (VISION_UNAVAILABLE)."
                } else {
                    "Vision-Service aktuell nicht erreichbar (VISION_UNAVAILABLE)."
                }
            }
            "VISION_PARSE_FAILED" -> "Vision konnte das Bild nicht auswerten (VISION_PARSE_FAILED)."
            "IMAGE_TOO_LARGE" -> "Bild ist zu groß fuer Vision-Scan (IMAGE_TOO_LARGE)."
            "INVALID_BODY" -> "Scan-Bild war ungueltig fuer Vision-Scan (INVALID_BODY)."
            "UNAUTHORIZED" -> "API-Key abgelehnt (UNAUTHORIZED)."
            else -> {
                val suffix = if (!backendCode.isNullOrBlank()) " ($backendCode)" else ""
                val detail = backendMessage?.takeIf { it.isNotBlank() }
                if (detail != null) {
                    "Vision-Request fehlgeschlagen: $detail$suffix [HTTP $status]."
                } else {
                    "Vision-Request fehlgeschlagen [HTTP $status]$suffix."
                }
            }
        }

        return VisionScanException(
            backendCode = backendCode,
            message = message
        )
    }

    private fun extractApiErrorCode(errorBody: String): String? {
        return try {
            JSONObject(errorBody).optJSONObject("error")?.optString("code")?.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractApiErrorMessage(errorBody: String): String? {
        return try {
            JSONObject(errorBody).optJSONObject("error")?.optString("message")?.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }
}
