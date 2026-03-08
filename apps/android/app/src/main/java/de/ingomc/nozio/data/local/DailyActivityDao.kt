package de.ingomc.nozio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyActivityDao {
    @Query("SELECT * FROM daily_activity WHERE date = :date LIMIT 1")
    fun getByDate(date: LocalDate): Flow<DailyActivity?>

    @Query("SELECT * FROM daily_activity WHERE date = :date LIMIT 1")
    suspend fun getByDateNow(date: LocalDate): DailyActivity?

    @Query("SELECT * FROM daily_activity WHERE weightKg IS NOT NULL ORDER BY date ASC")
    fun getWeightHistory(): Flow<List<DailyActivity>>

    @Query("SELECT * FROM daily_activity WHERE bodyFatPercent IS NOT NULL ORDER BY date ASC")
    fun getBodyFatHistory(): Flow<List<DailyActivity>>

    @Query("SELECT * FROM daily_activity WHERE weightKg IS NOT NULL ORDER BY date DESC LIMIT 1")
    fun getLatestWeightEntry(): Flow<DailyActivity?>

    @Query("SELECT * FROM daily_activity WHERE weightKg IS NOT NULL AND date <= :date ORDER BY date DESC LIMIT 1")
    fun getLatestWeightEntryUpTo(date: LocalDate): Flow<DailyActivity?>

    @Query("SELECT * FROM daily_activity WHERE bodyFatPercent IS NOT NULL ORDER BY date DESC LIMIT 1")
    fun getLatestBodyFatEntry(): Flow<DailyActivity?>

    @Query("SELECT * FROM daily_activity WHERE bodyFatPercent IS NOT NULL AND date <= :date ORDER BY date DESC LIMIT 1")
    fun getLatestBodyFatEntryUpTo(date: LocalDate): Flow<DailyActivity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(activity: DailyActivity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(activities: List<DailyActivity>)

    @Query("SELECT * FROM daily_activity ORDER BY date ASC")
    suspend fun getAll(): List<DailyActivity>

    @Query("DELETE FROM daily_activity")
    suspend fun deleteAll()
}
