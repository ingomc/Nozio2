package de.ingomc.nozio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SupplementIntakeDao {
    @Query("SELECT supplementId FROM supplement_intakes WHERE date = :date")
    fun observeTakenSupplementIds(date: LocalDate): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(intake: SupplementIntakeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SupplementIntakeEntity>)

    @Query("DELETE FROM supplement_intakes WHERE date = :date AND supplementId = :supplementId")
    suspend fun deleteForDate(date: LocalDate, supplementId: Long)

    @Query("SELECT * FROM supplement_intakes ORDER BY date ASC, supplementId ASC")
    suspend fun getAllRaw(): List<SupplementIntakeEntity>

    @Query("DELETE FROM supplement_intakes")
    suspend fun deleteAll()
}
