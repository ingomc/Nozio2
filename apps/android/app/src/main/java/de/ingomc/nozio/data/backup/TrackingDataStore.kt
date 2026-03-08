package de.ingomc.nozio.data.backup

import de.ingomc.nozio.data.local.DailyActivity
import de.ingomc.nozio.data.local.DiaryEntry
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.NozioDatabase
import de.ingomc.nozio.data.local.SupplementIntakeEntity
import de.ingomc.nozio.data.local.SupplementPlanItemEntity

interface TrackingDataStore {
    suspend fun getAllFoods(): List<FoodItem>
    suspend fun getAllDiaryEntries(): List<DiaryEntry>
    suspend fun getAllDailyActivities(): List<DailyActivity>
    suspend fun getAllSupplementPlanItems(): List<SupplementPlanItemEntity>
    suspend fun getAllSupplementIntakes(): List<SupplementIntakeEntity>
    suspend fun replaceTrackingData(
        foodItems: List<FoodItem>,
        diaryEntries: List<DiaryEntry>,
        dailyActivities: List<DailyActivity>,
        supplementPlanItems: List<SupplementPlanItemEntity>,
        supplementIntakes: List<SupplementIntakeEntity>
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

    override suspend fun replaceTrackingData(
        foodItems: List<FoodItem>,
        diaryEntries: List<DiaryEntry>,
        dailyActivities: List<DailyActivity>,
        supplementPlanItems: List<SupplementPlanItemEntity>,
        supplementIntakes: List<SupplementIntakeEntity>
    ) {
        database.replaceTrackingData(
            foodItems = foodItems,
            diaryEntries = diaryEntries,
            dailyActivities = dailyActivities,
            supplementPlanItems = supplementPlanItems,
            supplementIntakes = supplementIntakes
        )
    }
}
