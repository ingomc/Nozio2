package de.ingomc.nozio.data.backup

import de.ingomc.nozio.data.local.DailyActivity
import de.ingomc.nozio.data.local.DiaryEntry
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.local.MealTemplateEntity
import de.ingomc.nozio.data.local.MealTemplateIngredientEntity
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.local.RecipeAmountUnit
import de.ingomc.nozio.data.local.SupplementAmountUnit
import de.ingomc.nozio.data.local.SupplementDayPart
import de.ingomc.nozio.data.local.SupplementIntakeEntity
import de.ingomc.nozio.data.local.SupplementPlanItemEntity
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
            ),
            supplementPlanItems = mutableListOf(
                SupplementPlanItemEntity(
                    id = 1,
                    name = "Magnesium",
                    dayPart = SupplementDayPart.EVENING,
                    scheduledMinutesOfDay = 21 * 60,
                    amountValue = 1.0,
                    amountUnit = SupplementAmountUnit.TABLET
                )
            ),
            supplementIntakes = mutableListOf(
                SupplementIntakeEntity(
                    date = LocalDate.parse("2026-03-05"),
                    supplementId = 1,
                    takenAtEpochMs = 1710000000000
                )
            )
        )
        val repository = BackupRepositoryImpl(store, appVersionName = "0.6.0")

        val json = repository.createBackupJson()

        assertTrue(json.contains("\"schemaVersion\":3"))
        assertTrue(json.contains("\"foodItems\""))
        assertTrue(json.contains("\"diaryEntries\""))
        assertTrue(json.contains("\"dailyActivities\""))
        assertTrue(json.contains("\"supplementPlanItems\""))
        assertTrue(json.contains("\"supplementIntakes\""))
        assertTrue(json.contains("\"mealTemplates\""))
        assertTrue(json.contains("\"mealTemplateIngredients\""))
    }

    @Test
    fun restoreFromBackupJson_v2_replacesExistingDataIncludingSupplements() = runTest {
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
            ),
            supplementPlanItems = mutableListOf(
                SupplementPlanItemEntity(
                    id = 10,
                    name = "Alt",
                    dayPart = SupplementDayPart.PRE_BREAKFAST,
                    scheduledMinutesOfDay = 7 * 60,
                    amountValue = 1.0,
                    amountUnit = SupplementAmountUnit.CAPSULE
                )
            ),
            supplementIntakes = mutableListOf(
                SupplementIntakeEntity(
                    date = LocalDate.parse("2026-01-01"),
                    supplementId = 10,
                    takenAtEpochMs = 1700000000000
                )
            )
        )
        val repository = BackupRepositoryImpl(store, appVersionName = "0.6.0")

        val restoreJson = """
            {
              "schemaVersion": 2,
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
              ],
              "supplementPlanItems": [
                {
                  "id": 501,
                  "name": "Omega 3",
                  "dayPart": "MIDDAY",
                  "scheduledMinutesOfDay": 780,
                  "amountValue": 2.0,
                  "amountUnit": "CAPSULE"
                }
              ],
              "supplementIntakes": [
                {
                  "dateIso": "2026-03-05",
                  "supplementId": 501,
                  "takenAtEpochMs": 1710000001000
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
        assertEquals(1, store.supplementPlanItems.size)
        assertEquals("Omega 3", store.supplementPlanItems.first().name)
        assertEquals(1, store.supplementIntakes.size)
        assertEquals(501, store.supplementIntakes.first().supplementId)
    }

    @Test
    fun restoreFromBackupJson_v1_stillWorksAndClearsSupplements() = runTest {
        val store = FakeTrackingDataStore(
            foods = mutableListOf(),
            diaryEntries = mutableListOf(),
            dailyActivities = mutableListOf(),
            supplementPlanItems = mutableListOf(
                SupplementPlanItemEntity(
                    id = 9,
                    name = "Alt",
                    dayPart = SupplementDayPart.LATE,
                    scheduledMinutesOfDay = 23 * 60,
                    amountValue = 1.0,
                    amountUnit = SupplementAmountUnit.TABLET
                )
            ),
            supplementIntakes = mutableListOf(
                SupplementIntakeEntity(
                    date = LocalDate.parse("2026-01-01"),
                    supplementId = 9,
                    takenAtEpochMs = 1710000000000
                )
            )
        )
        val repository = BackupRepositoryImpl(store, appVersionName = "0.6.0")
        val restoreJson = """
            {
              "schemaVersion": 1,
              "createdAtEpochMs": 1710000000000,
              "appVersionName": "0.6.0",
              "foodItems": [],
              "diaryEntries": [],
              "dailyActivities": []
            }
        """.trimIndent()

        val result = repository.restoreFromBackupJson(restoreJson)

        assertTrue(result is RestoreResult.Success)
        assertTrue(store.supplementPlanItems.isEmpty())
        assertTrue(store.supplementIntakes.isEmpty())
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
            dailyActivities = mutableListOf(),
            supplementPlanItems = mutableListOf(),
            supplementIntakes = mutableListOf()
        )
        val repository = BackupRepositoryImpl(store, appVersionName = "0.6.0")
        val invalidSchemaJson = """
            {
              "schemaVersion": 99,
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

    @Test
    fun restoreFromBackupJson_v3_restoresMealTemplates() = runTest {
        val store = FakeTrackingDataStore(
            foods = mutableListOf(),
            diaryEntries = mutableListOf(),
            dailyActivities = mutableListOf(),
            supplementPlanItems = mutableListOf(),
            supplementIntakes = mutableListOf()
        )
        val repository = BackupRepositoryImpl(store, appVersionName = "0.7.0")

        val restoreJson = """
            {
              "schemaVersion": 3,
              "createdAtEpochMs": 1710000000000,
              "appVersionName": "0.7.0",
              "foodItems": [
                {
                  "id": 10,
                  "name": "Milch",
                  "caloriesPer100g": 64.0,
                  "proteinPer100g": 3.3,
                  "fatPer100g": 3.5,
                  "carbsPer100g": 4.8,
                  "source": "OPEN_FOOD_FACTS"
                }
              ],
              "diaryEntries": [],
              "dailyActivities": [],
              "supplementPlanItems": [],
              "supplementIntakes": [],
              "mealTemplates": [
                {
                  "id": 1,
                  "name": "Proteinshake",
                  "defaultMealType": "BREAKFAST",
                  "createdAtEpochMs": 1710000000000,
                  "updatedAtEpochMs": 1710000000000
                }
              ],
              "mealTemplateIngredients": [
                {
                  "id": 1,
                  "templateId": 1,
                  "foodItemId": 10,
                  "position": 0,
                  "defaultAmountValue": 300.0,
                  "amountUnit": "MILLILITER"
                }
              ]
            }
        """.trimIndent()

        val result = repository.restoreFromBackupJson(restoreJson)

        assertTrue(result is RestoreResult.Success)
        val success = result as RestoreResult.Success
        assertEquals(1, success.mealTemplateCount)
        assertEquals(1, success.mealTemplateIngredientCount)
        assertEquals(1, store.mealTemplates.size)
        assertEquals("Proteinshake", store.mealTemplates.first().name)
        assertEquals(MealType.BREAKFAST, store.mealTemplates.first().defaultMealType)
        assertEquals(1, store.mealTemplateIngredients.size)
        assertEquals(RecipeAmountUnit.MILLILITER, store.mealTemplateIngredients.first().amountUnit)
    }

    @Test
    fun restoreFromBackupJson_v2_clearsMealTemplates() = runTest {
        val store = FakeTrackingDataStore(
            foods = mutableListOf(),
            diaryEntries = mutableListOf(),
            dailyActivities = mutableListOf(),
            supplementPlanItems = mutableListOf(),
            supplementIntakes = mutableListOf(),
            mealTemplates = mutableListOf(
                MealTemplateEntity(
                    id = 1,
                    name = "Alt",
                    defaultMealType = MealType.LUNCH,
                    createdAtEpochMs = 1710000000000,
                    updatedAtEpochMs = 1710000000000
                )
            ),
            mealTemplateIngredients = mutableListOf(
                MealTemplateIngredientEntity(
                    id = 1,
                    templateId = 1,
                    foodItemId = 10,
                    position = 0,
                    defaultAmountValue = 100.0,
                    amountUnit = RecipeAmountUnit.GRAM
                )
            )
        )
        val repository = BackupRepositoryImpl(store, appVersionName = "0.6.0")
        val restoreJson = """
            {
              "schemaVersion": 2,
              "createdAtEpochMs": 1710000000000,
              "appVersionName": "0.6.0",
              "foodItems": [],
              "diaryEntries": [],
              "dailyActivities": [],
              "supplementPlanItems": [],
              "supplementIntakes": []
            }
        """.trimIndent()

        val result = repository.restoreFromBackupJson(restoreJson)

        assertTrue(result is RestoreResult.Success)
        assertTrue(store.mealTemplates.isEmpty())
        assertTrue(store.mealTemplateIngredients.isEmpty())
    }

    @Test
    fun createBackupJson_includesMealTemplateData() = runTest {
        val store = FakeTrackingDataStore(
            foods = mutableListOf(),
            diaryEntries = mutableListOf(),
            dailyActivities = mutableListOf(),
            supplementPlanItems = mutableListOf(),
            supplementIntakes = mutableListOf(),
            mealTemplates = mutableListOf(
                MealTemplateEntity(
                    id = 1,
                    name = "Shake",
                    defaultMealType = MealType.BREAKFAST,
                    createdAtEpochMs = 1710000000000,
                    updatedAtEpochMs = 1710000000000
                )
            ),
            mealTemplateIngredients = mutableListOf(
                MealTemplateIngredientEntity(
                    id = 1,
                    templateId = 1,
                    foodItemId = 10,
                    position = 0,
                    defaultAmountValue = 300.0,
                    amountUnit = RecipeAmountUnit.MILLILITER
                )
            )
        )
        val repository = BackupRepositoryImpl(store, appVersionName = "0.7.0")

        val json = repository.createBackupJson()

        assertTrue(json.contains("\"schemaVersion\":3"))
        assertTrue(json.contains("\"Shake\""))
        assertTrue(json.contains("\"MILLILITER\""))
    }

    private class FakeTrackingDataStore(
        val foods: MutableList<FoodItem>,
        val diaryEntries: MutableList<DiaryEntry>,
        val dailyActivities: MutableList<DailyActivity>,
        val supplementPlanItems: MutableList<SupplementPlanItemEntity>,
        val supplementIntakes: MutableList<SupplementIntakeEntity>,
        val mealTemplates: MutableList<MealTemplateEntity> = mutableListOf(),
        val mealTemplateIngredients: MutableList<MealTemplateIngredientEntity> = mutableListOf()
    ) : TrackingDataStore {
        override suspend fun getAllFoods(): List<FoodItem> = foods.toList()
        override suspend fun getAllDiaryEntries(): List<DiaryEntry> = diaryEntries.toList()
        override suspend fun getAllDailyActivities(): List<DailyActivity> = dailyActivities.toList()
        override suspend fun getAllSupplementPlanItems(): List<SupplementPlanItemEntity> = supplementPlanItems.toList()
        override suspend fun getAllSupplementIntakes(): List<SupplementIntakeEntity> = supplementIntakes.toList()
        override suspend fun getAllMealTemplates(): List<MealTemplateEntity> = mealTemplates.toList()
        override suspend fun getAllMealTemplateIngredients(): List<MealTemplateIngredientEntity> = mealTemplateIngredients.toList()

        override suspend fun replaceTrackingData(
            foodItems: List<FoodItem>,
            diaryEntries: List<DiaryEntry>,
            dailyActivities: List<DailyActivity>,
            supplementPlanItems: List<SupplementPlanItemEntity>,
            supplementIntakes: List<SupplementIntakeEntity>,
            mealTemplates: List<MealTemplateEntity>,
            mealTemplateIngredients: List<MealTemplateIngredientEntity>
        ) {
            foods.clear()
            foods.addAll(foodItems)
            this.diaryEntries.clear()
            this.diaryEntries.addAll(diaryEntries)
            this.dailyActivities.clear()
            this.dailyActivities.addAll(dailyActivities)
            this.supplementPlanItems.clear()
            this.supplementPlanItems.addAll(supplementPlanItems)
            this.supplementIntakes.clear()
            this.supplementIntakes.addAll(supplementIntakes)
            this.mealTemplates.clear()
            this.mealTemplates.addAll(mealTemplates)
            this.mealTemplateIngredients.clear()
            this.mealTemplateIngredients.addAll(mealTemplateIngredients)
        }
    }
}
