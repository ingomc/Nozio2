package de.ingomc.nozio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    @Query("UPDATE diary_entries SET amountInGrams = :amountInGrams WHERE id = :entryId")
    suspend fun updateAmountInGrams(entryId: Long, amountInGrams: Double)

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

    @Query(
        """
        SELECT f.*
        FROM diary_entries d
        INNER JOIN food_items f ON d.foodItemId = f.id
        GROUP BY d.foodItemId
        ORDER BY MAX(d.id) DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentlyAddedFoods(limit: Int): List<FoodItem>

    @Query(
        """
        SELECT f.*
        FROM diary_entries d
        INNER JOIN food_items f ON d.foodItemId = f.id
        GROUP BY d.foodItemId
        ORDER BY COUNT(*) DESC, MAX(d.id) DESC
        LIMIT :limit
        """
    )
    suspend fun getFrequentlyAddedFoods(limit: Int): List<FoodItem>

    @Query("SELECT * FROM diary_entries ORDER BY id ASC")
    suspend fun getAllRaw(): List<DiaryEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<DiaryEntry>)

    @Query("DELETE FROM diary_entries")
    suspend fun deleteAll()
}
