package de.ingomc.nozio.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "supplement_plan_items",
    indices = [Index("scheduledMinutesOfDay"), Index("dayPart")]
)
data class SupplementPlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dayPart: SupplementDayPart,
    val scheduledMinutesOfDay: Int,
    val amountValue: Double,
    val amountUnit: SupplementAmountUnit
)
