package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.DailyActivity
import de.ingomc.nozio.data.local.DailyActivityDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class WeightEntry(
    val date: LocalDate,
    val weightKg: Double
)

data class LatestWeightEntry(
    val date: LocalDate,
    val weightKg: Double
)

class DailyActivityRepository(
    private val dailyActivityDao: DailyActivityDao
) {
    fun getStepsForDate(date: LocalDate): Flow<Long> {
        return dailyActivityDao.getByDate(date).map { it?.steps ?: 0L }
    }

    fun getWeightForDate(date: LocalDate): Flow<Double?> {
        return dailyActivityDao.getByDate(date).map { it?.weightKg }
    }

    fun getWeightHistory(): Flow<List<WeightEntry>> {
        return dailyActivityDao.getWeightHistory().map { activities ->
            activities.mapNotNull { activity ->
                val weight = activity.weightKg ?: return@mapNotNull null
                WeightEntry(date = activity.date, weightKg = weight)
            }
        }
    }

    fun getLatestWeightEntry(): Flow<LatestWeightEntry?> {
        return dailyActivityDao.getLatestWeightEntry().map { activity ->
            val weight = activity?.weightKg ?: return@map null
            LatestWeightEntry(date = activity.date, weightKg = weight)
        }
    }

    suspend fun saveStepsForDate(date: LocalDate, steps: Long) {
        val existing = dailyActivityDao.getByDateNow(date)
        dailyActivityDao.upsert(
            DailyActivity(
                date = date,
                steps = steps.coerceAtLeast(0L),
                weightKg = existing?.weightKg
            )
        )
    }

    suspend fun saveWeightForDate(date: LocalDate, weightKg: Double) {
        val existing = dailyActivityDao.getByDateNow(date)
        dailyActivityDao.upsert(
            DailyActivity(
                date = date,
                steps = existing?.steps ?: 0L,
                weightKg = weightKg.coerceIn(20.0, 400.0)
            )
        )
    }
}
