package de.ingomc.nozio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplementDao {
    @Query(
        """
        SELECT * FROM supplement_plan_items
        ORDER BY scheduledMinutesOfDay ASC, dayPart ASC, id ASC
        """
    )
    fun observePlanItems(): Flow<List<SupplementPlanItemEntity>>

    @Query(
        """
        SELECT * FROM supplement_plan_items
        ORDER BY scheduledMinutesOfDay ASC, dayPart ASC, id ASC
        """
    )
    suspend fun getAllRaw(): List<SupplementPlanItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SupplementPlanItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SupplementPlanItemEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM supplement_plan_items WHERE id = :id)")
    suspend fun existsById(id: Long): Boolean

    @Query("DELETE FROM supplement_plan_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM supplement_plan_items")
    suspend fun deleteAll()
}
