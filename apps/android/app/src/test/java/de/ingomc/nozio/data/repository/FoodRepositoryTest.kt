package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.FoodDao
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.remote.FoodApi
import de.ingomc.nozio.data.remote.FoodBarcodeResponseDto
import de.ingomc.nozio.data.remote.FoodSearchItemDto
import de.ingomc.nozio.data.remote.FoodSearchResponseDto
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
        assertEquals(1, foodDao.insertedItems.size)
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
        )
    ) : FoodApi {
        override suspend fun searchFoods(query: String, limit: Int): FoodSearchResponseDto = searchResponse

        override suspend fun getFoodByBarcode(barcode: String): FoodBarcodeResponseDto {
            return barcodeResponse ?: throw IllegalStateException("not found")
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
    }
}
