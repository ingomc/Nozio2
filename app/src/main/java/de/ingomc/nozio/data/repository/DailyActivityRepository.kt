package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.DailyActivity
import de.ingomc.nozio.data.local.DailyActivityDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class DailyActivityRepository(
    private val dailyActivityDao: DailyActivityDao
) {
    fun getStepsForDate(date: LocalDate): Flow<Long> {
        return dailyActivityDao.getByDate(date).map { it?.steps ?: 0L }
    }

    suspend fun saveStepsForDate(date: LocalDate, steps: Long) {
        dailyActivityDao.upsert(DailyActivity(date = date, steps = steps.coerceAtLeast(0L)))
    }
}
