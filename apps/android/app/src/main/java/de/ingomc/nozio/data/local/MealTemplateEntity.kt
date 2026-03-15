package de.ingomc.nozio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_templates")
data class MealTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultMealType: MealType,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
