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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(activity: DailyActivity)
}
