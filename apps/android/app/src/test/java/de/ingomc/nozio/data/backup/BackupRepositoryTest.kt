package de.ingomc.nozio.data.backup

import de.ingomc.nozio.data.local.DailyActivity
import de.ingomc.nozio.data.local.DiaryEntry
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.local.MealType
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRepositoryTest {

    @Test
    fun createBackupJson_includesAllTrackingData() = runTest {
        val store = FakeTrackingDataStore(
            foods = mutableListOf(
                FoodItem(
                    id = 10,
                    name = "Haferflocken",
                    caloriesPer100g = 370.0,
                    proteinPer100g = 13.0,
                    fatPer100g = 7.0,
                    carbsPer100g = 59.0,
                    source = FoodSource.CUSTOM
                )
            ),
            diaryEntries = mutableListOf(
                DiaryEntry(
                    id = 21,
                    date = LocalDate.parse("2026-03-05"),
                    mealType = MealType.BREAKFAST,
                    foodItemId = 10,
                    amountInGrams = 80.0
                )
            ),
            dailyActivities = mutableListOf(
                DailyActivity(date = LocalDate.parse("2026-03-05"), steps = 7000, weightKg = 79.4)
            )
        )
        val repository = BackupRepositoryImpl(store, appVersionName = "0.6.0")

        val json = repository.createBackupJson()

        assertTrue(json.contains("\"schemaVersion\":1"))
        assertTrue(json.contains("\"foodItems\""))
        assertTrue(json.contains("\"diaryEntries\""))
        assertTrue(json.contains("\"dailyActivities\""))
    }

    @Test
    fun restoreFromBackupJson_replacesExistingData() = runTest {
        val store = FakeTrackingDataStore(
            foods = mutableListOf(
                FoodItem(
                    id = 1,
                    name = "Alt",
                    caloriesPer100g = 1.0,
                    proteinPer100g = 1.0,
                    fatPer100g = 1.0,
                    carbsPer100g = 1.0
                )
            ),
            diaryEntries = mutableListOf(
                DiaryEntry(
                    id = 2,
                    date = LocalDate.parse("2026-01-01"),
                    mealType = MealType.SNACK,
                    foodItemId = 1,
                    amountInGrams = 1.0
                )
            ),
            dailyActivities = mutableListOf(
                DailyActivity(date = LocalDate.parse("2026-01-01"), steps = 1, weightKg = null)
            )
        )
        val repository = BackupRepositoryImpl(store, appVersionName = "0.6.0")

        val restoreJson = """
            {
              "schemaVersion": 1,
              "createdAtEpochMs": 1710000000000,
              "appVersionName": "0.6.0",
              "foodItems": [
                {
                  "id": 44,
                  "name": "Neue Banane",
                  "caloriesPer100g": 89.0,
                  "proteinPer100g": 1.1,
                  "fatPer100g": 0.3,
                  "carbsPer100g": 23.0,
                  "source": "OPEN_FOOD_FACTS"
                }
              ],
              "diaryEntries": [
                {
                  "id": 77,
                  "dateIso": "2026-03-05",
                  "mealType": "LUNCH",
                  "foodItemId": 44,
                  "amountInGrams": 120.0
                }
              ],
              "dailyActivities": [
                {
                  "dateIso": "2026-03-05",
                  "steps": 9000,
                  "weightKg": 78.8
                }
              ]
            }
        """.trimIndent()

        val result = repository.restoreFromBackupJson(restoreJson)

        assertTrue(result is RestoreResult.Success)
        assertEquals(1, store.foods.size)
        assertEquals("Neue Banane", store.foods.first().name)
        assertEquals(1, store.diaryEntries.size)
        assertEquals(44, store.diaryEntries.first().foodItemId)
        assertEquals(1, store.dailyActivities.size)
        assertEquals(9000, store.dailyActivities.first().steps)
    }

    @Test
    fun restoreFromBackupJson_withWrongSchema_returnsErrorAndDoesNotMutate() = runTest {
        val store = FakeTrackingDataStore(
            foods = mutableListOf(
                FoodItem(
                    id = 1,
                    name = "Bestehend",
                    caloriesPer100g = 50.0,
                    proteinPer100g = 1.0,
                    fatPer100g = 1.0,
                    carbsPer100g = 10.0
                )
            ),
            diaryEntries = mutableListOf(),
            dailyActivities = mutableListOf()
        )
        val repository = BackupRepositoryImpl(store, appVersionName = "0.6.0")
        val invalidSchemaJson = """
            {
              "schemaVersion": 2,
              "createdAtEpochMs": 1710000000000,
              "appVersionName": "0.6.0",
              "foodItems": [],
              "diaryEntries": [],
              "dailyActivities": []
            }
        """.trimIndent()

        val result = repository.restoreFromBackupJson(invalidSchemaJson)

        assertTrue(result is RestoreResult.Error)
        assertEquals(1, store.foods.size)
        assertEquals("Bestehend", store.foods.first().name)
    }

    private class FakeTrackingDataStore(
        val foods: MutableList<FoodItem>,
        val diaryEntries: MutableList<DiaryEntry>,
        val dailyActivities: MutableList<DailyActivity>
    ) : TrackingDataStore {
        override suspend fun getAllFoods(): List<FoodItem> = foods.toList()
        override suspend fun getAllDiaryEntries(): List<DiaryEntry> = diaryEntries.toList()
        override suspend fun getAllDailyActivities(): List<DailyActivity> = dailyActivities.toList()

        override suspend fun replaceTrackingData(
            foodItems: List<FoodItem>,
            diaryEntries: List<DiaryEntry>,
            dailyActivities: List<DailyActivity>
        ) {
            foods.clear()
            foods.addAll(foodItems)
            this.diaryEntries.clear()
            this.diaryEntries.addAll(diaryEntries)
            this.dailyActivities.clear()
            this.dailyActivities.addAll(dailyActivities)
        }
    }
}
