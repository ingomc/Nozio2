package de.ingomc.nozio.data.repository

import de.ingomc.nozio.data.local.DailyActivity
import de.ingomc.nozio.data.local.DailyActivityDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import kotlin.math.round

data class WeightEntry(
    val date: LocalDate,
    val weightKg: Double
)

data class LatestWeightEntry(
    val date: LocalDate,
    val weightKg: Double
)

data class LatestBodyFatEntry(
    val date: LocalDate,
    val bodyFatPercent: Double
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

    fun getBodyFatForDate(date: LocalDate): Flow<Double?> {
        return dailyActivityDao.getByDate(date).map { it?.bodyFatPercent }
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

    fun getLatestWeightEntryUpTo(date: LocalDate): Flow<LatestWeightEntry?> {
        return dailyActivityDao.getLatestWeightEntryUpTo(date).map { activity ->
            val weight = activity?.weightKg ?: return@map null
            LatestWeightEntry(date = activity.date, weightKg = weight)
        }
    }

    fun getLatestBodyFatEntry(): Flow<LatestBodyFatEntry?> {
        return dailyActivityDao.getLatestBodyFatEntry().map { activity ->
            val bodyFat = activity?.bodyFatPercent ?: return@map null
            LatestBodyFatEntry(date = activity.date, bodyFatPercent = bodyFat)
        }
    }

    fun getLatestBodyFatEntryUpTo(date: LocalDate): Flow<LatestBodyFatEntry?> {
        return dailyActivityDao.getLatestBodyFatEntryUpTo(date).map { activity ->
            val bodyFat = activity?.bodyFatPercent ?: return@map null
            LatestBodyFatEntry(date = activity.date, bodyFatPercent = bodyFat)
        }
    }

    suspend fun saveStepsForDate(date: LocalDate, steps: Long) {
        val existing = dailyActivityDao.getByDateNow(date)
        dailyActivityDao.upsert(
            DailyActivity(
                date = date,
                steps = steps.coerceAtLeast(0L),
                weightKg = existing?.weightKg,
                bodyFatPercent = existing?.bodyFatPercent
            )
        )
    }

    suspend fun saveWeightForDate(date: LocalDate, weightKg: Double) {
        val existing = dailyActivityDao.getByDateNow(date)
        val roundedWeight = round(weightKg.coerceIn(20.0, 400.0) * 10.0) / 10.0
        dailyActivityDao.upsert(
            DailyActivity(
                date = date,
                steps = existing?.steps ?: 0L,
                weightKg = roundedWeight,
                bodyFatPercent = existing?.bodyFatPercent
            )
        )
    }

    suspend fun saveBodyFatForDate(date: LocalDate, bodyFatPercent: Double) {
        val existing = dailyActivityDao.getByDateNow(date)
        val roundedBodyFat = round(bodyFatPercent.coerceIn(3.0, 60.0) * 10.0) / 10.0
        dailyActivityDao.upsert(
            DailyActivity(
                date = date,
                steps = existing?.steps ?: 0L,
                weightKg = existing?.weightKg,
                bodyFatPercent = roundedBodyFat
            )
        )
    }

    suspend fun saveWeightAndBodyFatForDate(
        date: LocalDate,
        weightKg: Double,
        bodyFatPercent: Double?
    ) {
        val existing = dailyActivityDao.getByDateNow(date)
        val roundedWeight = round(weightKg.coerceIn(20.0, 400.0) * 10.0) / 10.0
        val roundedBodyFat = bodyFatPercent?.let { round(it.coerceIn(3.0, 60.0) * 10.0) / 10.0 }
        dailyActivityDao.upsert(
            DailyActivity(
                date = date,
                steps = existing?.steps ?: 0L,
                weightKg = roundedWeight,
                bodyFatPercent = roundedBodyFat ?: existing?.bodyFatPercent
            )
        )
    }
}
