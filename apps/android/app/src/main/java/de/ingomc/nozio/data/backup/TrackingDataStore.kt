package de.ingomc.nozio.data.backup

import de.ingomc.nozio.data.local.DailyActivity
import de.ingomc.nozio.data.local.DiaryEntry
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.MealTemplateEntity
import de.ingomc.nozio.data.local.MealTemplateIngredientEntity
import de.ingomc.nozio.data.local.NozioDatabase
import de.ingomc.nozio.data.local.SupplementIntakeEntity
import de.ingomc.nozio.data.local.SupplementPlanItemEntity

interface TrackingDataStore {
    suspend fun getAllFoods(): List<FoodItem>
    suspend fun getAllDiaryEntries(): List<DiaryEntry>
    suspend fun getAllDailyActivities(): List<DailyActivity>
    suspend fun getAllSupplementPlanItems(): List<SupplementPlanItemEntity>
    suspend fun getAllSupplementIntakes(): List<SupplementIntakeEntity>
    suspend fun getAllMealTemplates(): List<MealTemplateEntity>
    suspend fun getAllMealTemplateIngredients(): List<MealTemplateIngredientEntity>
    suspend fun replaceTrackingData(
        foodItems: List<FoodItem>,
        diaryEntries: List<DiaryEntry>,
        dailyActivities: List<DailyActivity>,
        supplementPlanItems: List<SupplementPlanItemEntity>,
        supplementIntakes: List<SupplementIntakeEntity>,
        mealTemplates: List<MealTemplateEntity> = emptyList(),
        mealTemplateIngredients: List<MealTemplateIngredientEntity> = emptyList()
    )
}

class RoomTrackingDataStore(
    private val database: NozioDatabase
) : TrackingDataStore {
    override suspend fun getAllFoods(): List<FoodItem> = database.foodDao().getAll()

    override suspend fun getAllDiaryEntries(): List<DiaryEntry> = database.diaryDao().getAllRaw()

    override suspend fun getAllDailyActivities(): List<DailyActivity> = database.dailyActivityDao().getAll()

    override suspend fun getAllSupplementPlanItems(): List<SupplementPlanItemEntity> =
        database.supplementDao().getAllRaw()

    override suspend fun getAllSupplementIntakes(): List<SupplementIntakeEntity> =
        database.supplementIntakeDao().getAllRaw()

    override suspend fun getAllMealTemplates(): List<MealTemplateEntity> =
        database.mealTemplateDao().getAllRaw()

    override suspend fun getAllMealTemplateIngredients(): List<MealTemplateIngredientEntity> =
        database.mealTemplateDao().getAllIngredientsRaw()

    override suspend fun replaceTrackingData(
        foodItems: List<FoodItem>,
        diaryEntries: List<DiaryEntry>,
        dailyActivities: List<DailyActivity>,
        supplementPlanItems: List<SupplementPlanItemEntity>,
        supplementIntakes: List<SupplementIntakeEntity>,
        mealTemplates: List<MealTemplateEntity>,
        mealTemplateIngredients: List<MealTemplateIngredientEntity>
    ) {
        database.replaceTrackingData(
            foodItems = foodItems,
            diaryEntries = diaryEntries,
            dailyActivities = dailyActivities,
            supplementPlanItems = supplementPlanItems,
            supplementIntakes = supplementIntakes,
            mealTemplates = mealTemplates,
            mealTemplateIngredients = mealTemplateIngredients
        )
    }
}
