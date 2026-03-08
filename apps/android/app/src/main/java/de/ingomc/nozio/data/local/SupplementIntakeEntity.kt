package de.ingomc.nozio.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDate

@Entity(
    tableName = "supplement_intakes",
    primaryKeys = ["date", "supplementId"],
    foreignKeys = [
        ForeignKey(
            entity = SupplementPlanItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("supplementId"), Index("date")]
)
data class SupplementIntakeEntity(
    val date: LocalDate,
    val supplementId: Long,
    val takenAtEpochMs: Long
)
