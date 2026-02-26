package de.ingomc.nozio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_activity")
data class DailyActivity(
    @PrimaryKey val date: LocalDate,
    val steps: Long
)
