package de.ingomc.nozio.data.backup

import de.ingomc.nozio.data.local.DailyActivity
import de.ingomc.nozio.data.local.DiaryEntry
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.NozioDatabase

interface TrackingDataStore {
    suspend fun getAllFoods(): List<FoodItem>
    suspend fun getAllDiaryEntries(): List<DiaryEntry>
    suspend fun getAllDailyActivities(): List<DailyActivity>
    suspend fun replaceTrackingData(
        foodItems: List<FoodItem>,
        diaryEntries: List<DiaryEntry>,
        dailyActivities: List<DailyActivity>
    )
}

class RoomTrackingDataStore(
    private val database: NozioDatabase
) : TrackingDataStore {
    override suspend fun getAllFoods(): List<FoodItem> = database.foodDao().getAll()

    override suspend fun getAllDiaryEntries(): List<DiaryEntry> = database.diaryDao().getAllRaw()

    override suspend fun getAllDailyActivities(): List<DailyActivity> = database.dailyActivityDao().getAll()

    override suspend fun replaceTrackingData(
        foodItems: List<FoodItem>,
        diaryEntries: List<DiaryEntry>,
        dailyActivities: List<DailyActivity>
    ) {
        database.replaceTrackingData(
            foodItems = foodItems,
            diaryEntries = diaryEntries,
            dailyActivities = dailyActivities
        )
    }
}
