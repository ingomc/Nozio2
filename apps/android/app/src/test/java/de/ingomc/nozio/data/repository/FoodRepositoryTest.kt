package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.FoodDao
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.remote.FoodApi
import de.ingomc.nozio.data.remote.CreateCustomFoodRequestDto
import de.ingomc.nozio.data.remote.CreateCustomFoodResponseDto
import de.ingomc.nozio.data.remote.FoodBarcodeResponseDto
import de.ingomc.nozio.data.remote.FoodSearchItemDto
import de.ingomc.nozio.data.remote.FoodSearchResponseDto
import de.ingomc.nozio.data.remote.VisionFoodAnalyzeRequestDto
import de.ingomc.nozio.data.remote.VisionNutritionParseRequestDto
import de.ingomc.nozio.data.remote.VisionNutritionParseResponseDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodRepositoryTest {
    @Test
    fun searchFood_mapsApiResponseAndCachesResults() = runTest {
        val api = FakeFoodApi(
            searchResponse = FoodSearchResponseDto(
                items = listOf(
                    FoodSearchItemDto(
                        id = "off-1",
                        name = "Haferflocken",
                        brand = "Ja",
                        displayName = "Haferflocken (Ja)",
                        barcode = "123",
                        caloriesPer100g = 370.0,
                        proteinPer100g = 13.5,
                        fatPer100g = 7.0,
                        carbsPer100g = 58.0,
                        packageSize = "500 g",
                        packageQuantity = 500.0,
                        source = "SELF_HOSTED_OFF"
                    )
                ),
                totalEstimated = 1
            )
        )
        val foodDao = FakeFoodDao()
        val repository = FoodRepository(api, foodDao)

        val results = repository.searchFood("hafer")

        assertEquals(1, results.size)
        assertEquals(FoodSource.SELF_HOSTED_OFF, results.first().source)
        assertEquals("500 g", results.first().packageSize)
        assertEquals(0, foodDao.insertedItems.size)
    }

    @Test
    fun getFoodByBarcode_returnsNullWhenApiAndCacheMiss() = runTest {
        val repository = FoodRepository(
            api = FakeFoodApi(barcodeResponse = null),
            foodDao = FakeFoodDao()
        )

        val result = repository.getFoodByBarcode("999")

        assertNull(result)
    }

    @Test
    fun createCustomFood_mapsCustomSourceAndStoresLocally() = runTest {
        val foodDao = FakeFoodDao()
        val repository = FoodRepository(
            api = FakeFoodApi(
                customFoodResponse = CreateCustomFoodResponseDto(
                    item = FoodSearchItemDto(
                        id = "custom-1",
                        name = "Protein Porridge",
                        brand = "Nozio",
                        displayName = "Protein Porridge (Nozio)",
                        caloriesPer100g = 410.0,
                        proteinPer100g = 26.0,
                        fatPer100g = 9.0,
                        carbsPer100g = 48.0,
                        source = "CUSTOM"
                    )
                )
            ),
            foodDao = foodDao
        )

        val result = repository.createCustomFood(
            CustomFoodInput(
                name = "Protein Porridge",
                brand = "Nozio",
                caloriesPer100g = 410.0,
                proteinPer100g = 26.0,
                fatPer100g = 9.0,
                carbsPer100g = 48.0,
                sugarPer100g = 5.0
            )
        )

        assertEquals(FoodSource.CUSTOM, result.source)
        assertEquals(1, foodDao.insertedItems.size)
        assertEquals("Protein Porridge (Nozio)", result.name)
    }

    @Test
    fun parseNutritionFromImage_mapsVisionResponse() = runTest {
        val repository = FoodRepository(
            api = FakeFoodApi(),
            foodDao = FakeFoodDao()
        )

        val result = repository.parseNutritionFromImage("abc123", "de")

        assertEquals(420.0, result.caloriesPer100g)
        assertEquals(24.0, result.proteinPer100g)
        assertEquals("gemini-2.0-flash", result.model)
    }

    @Test
    fun analyzeFoodFromImage_mapsVisionResponseWithHints() = runTest {
        val repository = FoodRepository(
            api = FakeFoodApi(),
            foodDao = FakeFoodDao()
        )

        val result = repository.analyzeFoodFromImage(
            imageBase64 = "abc123",
            locale = "de",
            portionSize = "medium",
            hints = listOf("Haehnchen")
        )

        assertEquals(150.0, result.caloriesPer100g)
        assertEquals(22.0, result.proteinPer100g)
        assertEquals("1 Teller", result.servingSize)
        assertEquals(350.0, result.servingQuantity)
        assertEquals(525.0, result.caloriesPerServing)
        assertEquals("gemini-2.0-flash", result.model)
    }

    private class FakeFoodApi(
        private val searchResponse: FoodSearchResponseDto = FoodSearchResponseDto(),
        private val barcodeResponse: FoodBarcodeResponseDto? = FoodBarcodeResponseDto(
            item = FoodSearchItemDto(
                id = "off-1",
                name = "Haferflocken",
                brand = "Ja",
                displayName = "Haferflocken (Ja)",
                barcode = "123",
                caloriesPer100g = 370.0,
                proteinPer100g = 13.5,
                fatPer100g = 7.0,
                carbsPer100g = 58.0,
                servingSize = "50 g",
                servingQuantity = 50.0,
                source = "SELF_HOSTED_OFF"
            )
        ),
        private val customFoodResponse: CreateCustomFoodResponseDto = CreateCustomFoodResponseDto(
            item = FoodSearchItemDto(
                id = "custom-1",
                name = "Custom Food",
                displayName = "Custom Food",
                caloriesPer100g = 100.0,
                proteinPer100g = 0.0,
                fatPer100g = 0.0,
                carbsPer100g = 0.0,
                source = "CUSTOM"
            )
        )
    ) : FoodApi {
        override suspend fun searchFoods(query: String, limit: Int): FoodSearchResponseDto = searchResponse

        override suspend fun getFoodByBarcode(barcode: String): FoodBarcodeResponseDto {
            return barcodeResponse ?: throw IllegalStateException("not found")
        }

        override suspend fun createCustomFood(request: CreateCustomFoodRequestDto): CreateCustomFoodResponseDto {
            return customFoodResponse
        }

        override suspend fun parseNutritionFromImage(request: VisionNutritionParseRequestDto): VisionNutritionParseResponseDto {
            return VisionNutritionParseResponseDto(
                name = "Protein Bar",
                caloriesPer100g = 420.0,
                proteinPer100g = 24.0,
                carbsPer100g = 38.0,
                fatPer100g = 16.0,
                sugarPer100g = 6.0,
                confidence = 0.91,
                model = "gemini-2.0-flash",
                warnings = emptyList()
            )
        }

        override suspend fun analyzeFoodFromImage(request: VisionFoodAnalyzeRequestDto): VisionNutritionParseResponseDto {
            return VisionNutritionParseResponseDto(
                name = "Haehnchen mit Reis",
                caloriesPer100g = 150.0,
                proteinPer100g = 22.0,
                carbsPer100g = 15.0,
                fatPer100g = 3.0,
                sugarPer100g = 0.0,
                servingSize = "1 Teller",
                servingQuantity = 350.0,
                caloriesPerServing = 525.0,
                proteinPerServing = 77.0,
                carbsPerServing = 52.0,
                fatPerServing = 10.0,
                confidence = 0.72,
                model = "gemini-2.0-flash",
                warnings = listOf("Geschaetzte Werte")
            )
        }
    }

    private class FakeFoodDao : FoodDao {
        val insertedItems = mutableListOf<FoodItem>()
        private val storage = mutableListOf<FoodItem>()

        override suspend fun insert(foodItem: FoodItem): Long {
            insertedItems += foodItem
            val stored = foodItem.copy(id = (storage.size + 1).toLong())
            storage += stored
            return stored.id
        }

        override suspend fun insertAll(foodItems: List<FoodItem>): List<Long> {
            return foodItems.map { insert(it) }
        }

        override suspend fun searchByName(query: String): List<FoodItem> {
            return storage.filter { it.name.contains(query, ignoreCase = true) }
        }

        override suspend fun getById(id: Long): FoodItem? = storage.firstOrNull { it.id == id }

        override suspend fun getByBarcode(barcode: String): FoodItem? = storage.firstOrNull { it.barcode == barcode }

        override suspend fun getAll(): List<FoodItem> = storage.toList()

        override suspend fun getFavorites(limit: Int): List<FoodItem> {
            return storage.filter { it.isFavorite }.take(limit)
        }

        override suspend fun getFoodsMissingImageUrl(limit: Int): List<FoodItem> {
            return storage
                .filter { it.imageUrl.isNullOrBlank() && !it.barcode.isNullOrBlank() }
                .take(limit)
        }

        override suspend fun setFavorite(foodId: Long, isFavorite: Boolean) {
            val index = storage.indexOfFirst { it.id == foodId }
            if (index >= 0) {
                storage[index] = storage[index].copy(isFavorite = isFavorite)
            }
        }

        override suspend fun insertAllWithIds(foodItems: List<FoodItem>) {
            storage += foodItems
        }

        override suspend fun deleteAll() {
            storage.clear()
        }
    }
}
