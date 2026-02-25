package de.ingomc.nozio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class DiaryEntryWithFood(
    val entryId: Long,
    val date: LocalDate,
    val mealType: MealType,
    val foodItemId: Long,
    val amountInGrams: Double,
    val foodName: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double
) {
    val calories: Double get() = caloriesPer100g * amountInGrams / 100.0
    val protein: Double get() = proteinPer100g * amountInGrams / 100.0
    val fat: Double get() = fatPer100g * amountInGrams / 100.0
    val carbs: Double get() = carbsPer100g * amountInGrams / 100.0
}

@Dao
interface DiaryDao {
    @Insert
    suspend fun insert(entry: DiaryEntry): Long

    @Query("DELETE FROM diary_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: Long)

    @Query(
        """
        SELECT 
            d.id AS entryId, d.date, d.mealType, d.foodItemId, d.amountInGrams,
            f.name AS foodName, f.caloriesPer100g, f.proteinPer100g, f.fatPer100g, f.carbsPer100g
        FROM diary_entries d
        INNER JOIN food_items f ON d.foodItemId = f.id
        WHERE d.date = :date
        ORDER BY d.id ASC
        """
    )
    fun getEntriesWithFoodForDate(date: LocalDate): Flow<List<DiaryEntryWithFood>>
}


