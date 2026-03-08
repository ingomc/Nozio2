package de.ingomc.nozio.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayloadV1(
    val schemaVersion: Int = SCHEMA_VERSION,
    val createdAtEpochMs: Long,
    val appVersionName: String,
    val foodItems: List<FoodItemBackupDto>,
    val diaryEntries: List<DiaryEntryBackupDto>,
    val dailyActivities: List<DailyActivityBackupDto>
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class FoodItemBackupDto(
    val id: Long,
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val barcode: String? = null,
    val servingSize: String? = null,
    val servingQuantity: Double? = null,
    val packageSize: String? = null,
    val packageQuantity: Double? = null,
    val isFavorite: Boolean = false,
    val source: String
)

@Serializable
data class DiaryEntryBackupDto(
    val id: Long,
    val dateIso: String,
    val mealType: String,
    val foodItemId: Long,
    val amountInGrams: Double
)

@Serializable
data class DailyActivityBackupDto(
    val dateIso: String,
    val steps: Long,
    val weightKg: Double? = null
)
