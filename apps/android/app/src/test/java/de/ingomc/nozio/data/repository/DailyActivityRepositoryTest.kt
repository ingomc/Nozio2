package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.DailyActivity
import de.ingomc.nozio.data.local.DailyActivityDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DailyActivityRepositoryTest {

    @Test
    fun getBodyFatHistory_mapsOnlyBodyFatEntriesInAscendingOrder() = runTest {
        val dao = FakeDailyActivityDao(
            bodyFatHistory = listOf(
                DailyActivity(LocalDate.parse("2026-03-01"), steps = 1000, weightKg = 81.2, bodyFatPercent = 21.4),
                DailyActivity(LocalDate.parse("2026-03-05"), steps = 1200, weightKg = 80.7, bodyFatPercent = null),
                DailyActivity(LocalDate.parse("2026-03-07"), steps = 2000, weightKg = null, bodyFatPercent = 21.0)
            )
        )
        val repository = DailyActivityRepository(dao)

        val history = repository.getBodyFatHistory().first()

        assertEquals(2, history.size)
        assertEquals(LocalDate.parse("2026-03-01"), history[0].date)
        assertEquals(21.4, history[0].bodyFatPercent, 0.001)
        assertEquals(LocalDate.parse("2026-03-07"), history[1].date)
        assertEquals(21.0, history[1].bodyFatPercent, 0.001)
    }
}

private class FakeDailyActivityDao(
    private val bodyFatHistory: List<DailyActivity>
) : DailyActivityDao {
    private val activityByDate = MutableStateFlow<DailyActivity?>(null)

    override fun getByDate(date: LocalDate): Flow<DailyActivity?> = activityByDate

    override suspend fun getByDateNow(date: LocalDate): DailyActivity? = activityByDate.value

    override fun getWeightHistory(): Flow<List<DailyActivity>> = flowOf(emptyList())

    override fun getBodyFatHistory(): Flow<List<DailyActivity>> = flowOf(bodyFatHistory)

    override fun getLatestWeightEntry(): Flow<DailyActivity?> = flowOf(null)

    override fun getLatestWeightEntryUpTo(date: LocalDate): Flow<DailyActivity?> = flowOf(null)

    override fun getLatestBodyFatEntry(): Flow<DailyActivity?> = flowOf(null)

    override fun getLatestBodyFatEntryUpTo(date: LocalDate): Flow<DailyActivity?> = flowOf(null)

    override suspend fun upsert(activity: DailyActivity) = Unit

    override suspend fun upsertAll(activities: List<DailyActivity>) = Unit

    override suspend fun getAll(): List<DailyActivity> = emptyList()

    override suspend fun deleteAll() = Unit
}
