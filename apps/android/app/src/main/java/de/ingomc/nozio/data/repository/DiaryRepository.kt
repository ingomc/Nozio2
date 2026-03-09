package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.DiaryDao
import de.ingomc.nozio.data.local.DiaryEntry
import de.ingomc.nozio.data.local.DiaryEntryWithFood
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.MealType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class DaySummary(
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val entriesByMeal: Map<MealType, List<DiaryEntryWithFood>> = emptyMap()
)

class DiaryRepository(private val diaryDao: DiaryDao) {

    fun getDaySummary(date: LocalDate): Flow<DaySummary> {
        return diaryDao.getEntriesWithFoodForDate(date).map { entries ->
            DaySummary(
                totalCalories = entries.sumOf { it.calories },
                totalProtein = entries.sumOf { it.protein },
                totalFat = entries.sumOf { it.fat },
                totalCarbs = entries.sumOf { it.carbs },
                entriesByMeal = MealType.entries.associateWith { mealType ->
                    entries.filter { it.mealType == mealType }
                }
            )
        }
    }

    suspend fun addEntry(
        date: LocalDate,
        mealType: MealType,
        foodItemId: Long,
        amountInGrams: Double
    ): Long {
        return diaryDao.insert(
            DiaryEntry(
                date = date,
                mealType = mealType,
                foodItemId = foodItemId,
                amountInGrams = amountInGrams
            )
        )
    }

    suspend fun deleteEntry(entryId: Long) {
        diaryDao.deleteById(entryId)
    }

    suspend fun updateEntryAmount(entryId: Long, amountInGrams: Double) {
        diaryDao.updateAmountInGrams(entryId, amountInGrams)
    }

    suspend fun moveEntry(entryId: Long, date: LocalDate, mealType: MealType) {
        diaryDao.updateDateAndMealType(entryId, date, mealType)
    }

    suspend fun getRecentlyAddedFoods(limit: Int = 8): List<FoodItem> {
        return diaryDao.getRecentlyAddedFoods(limit)
    }

    suspend fun getFrequentlyAddedFoods(limit: Int = 8): List<FoodItem> {
        return diaryDao.getFrequentlyAddedFoods(limit)
    }
}
