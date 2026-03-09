package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.DiaryDao
import de.ingomc.nozio.data.local.DiaryEntry
import de.ingomc.nozio.data.local.DiaryEntryWithFood
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.MealType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DiaryRepositoryTest {

    @Test
    fun moveEntry_updatesDateAndMealType() = runTest {
        val dao = FakeDiaryDao()
        val repository = DiaryRepository(dao)
        val targetDate = LocalDate.parse("2026-03-09")

        repository.moveEntry(
            entryId = 42L,
            date = targetDate,
            mealType = MealType.SNACK
        )

        assertEquals(42L, dao.lastMovedEntryId)
        assertEquals(targetDate, dao.lastMovedDate)
        assertEquals(MealType.SNACK, dao.lastMovedMealType)
    }

    @Test
    fun addEntry_insertsGivenDateMealAndAmount() = runTest {
        val dao = FakeDiaryDao()
        val repository = DiaryRepository(dao)
        val date = LocalDate.parse("2026-03-05")

        repository.addEntry(
            date = date,
            mealType = MealType.DINNER,
            foodItemId = 7L,
            amountInGrams = 180.0
        )

        assertEquals(date, dao.lastInsertedEntry?.date)
        assertEquals(MealType.DINNER, dao.lastInsertedEntry?.mealType)
        assertEquals(7L, dao.lastInsertedEntry?.foodItemId)
        assertEquals(180.0, dao.lastInsertedEntry?.amountInGrams ?: 0.0, 0.001)
    }
}

private class FakeDiaryDao : DiaryDao {
    var lastInsertedEntry: DiaryEntry? = null
    var lastMovedEntryId: Long? = null
    var lastMovedDate: LocalDate? = null
    var lastMovedMealType: MealType? = null

    override suspend fun insert(entry: DiaryEntry): Long {
        lastInsertedEntry = entry
        return 1L
    }

    override suspend fun deleteById(entryId: Long) = Unit

    override suspend fun updateAmountInGrams(entryId: Long, amountInGrams: Double) = Unit

    override suspend fun updateDateAndMealType(entryId: Long, date: LocalDate, mealType: MealType) {
        lastMovedEntryId = entryId
        lastMovedDate = date
        lastMovedMealType = mealType
    }

    override fun getEntriesWithFoodForDate(date: LocalDate): Flow<List<DiaryEntryWithFood>> = flowOf(emptyList())

    override suspend fun getRecentlyAddedFoods(limit: Int): List<FoodItem> = emptyList()

    override suspend fun getFrequentlyAddedFoods(limit: Int): List<FoodItem> = emptyList()

    override suspend fun getAllRaw(): List<DiaryEntry> = emptyList()

    override suspend fun insertAll(entries: List<DiaryEntry>) = Unit

    override suspend fun deleteAll() = Unit
}
