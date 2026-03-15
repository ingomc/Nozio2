package de.ingomc.nozio.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayloadV1(
    val schemaVersion: Int = SchemaVersions.V1,
    val createdAtEpochMs: Long,
    val appVersionName: String,
    val foodItems: List<FoodItemBackupDto>,
    val diaryEntries: List<DiaryEntryBackupDto>,
    val dailyActivities: List<DailyActivityBackupDto>
)

@Serializable
data class BackupPayloadV2(
    val schemaVersion: Int = SchemaVersions.V2,
    val createdAtEpochMs: Long,
    val appVersionName: String,
    val foodItems: List<FoodItemBackupDto>,
    val diaryEntries: List<DiaryEntryBackupDto>,
    val dailyActivities: List<DailyActivityBackupDto>,
    val supplementPlanItems: List<SupplementPlanItemBackupDto>,
    val supplementIntakes: List<SupplementIntakeBackupDto>
)

@Serializable
data class BackupPayloadV3(
    val schemaVersion: Int = SchemaVersions.V3,
    val createdAtEpochMs: Long,
    val appVersionName: String,
    val foodItems: List<FoodItemBackupDto>,
    val diaryEntries: List<DiaryEntryBackupDto>,
    val dailyActivities: List<DailyActivityBackupDto>,
    val supplementPlanItems: List<SupplementPlanItemBackupDto>,
    val supplementIntakes: List<SupplementIntakeBackupDto>,
    val mealTemplates: List<MealTemplateBackupDto>,
    val mealTemplateIngredients: List<MealTemplateIngredientBackupDto>
)

object SchemaVersions {
    const val V1 = 1
    const val V2 = 2
    const val V3 = 3
}

@Serializable
data class FoodItemBackupDto(
    val id: Long,
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val imageUrl: String? = null,
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
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null
)

@Serializable
data class SupplementPlanItemBackupDto(
    val id: Long,
    val name: String,
    val dayPart: String,
    val scheduledMinutesOfDay: Int,
    val amountValue: Double,
    val amountUnit: String
)

@Serializable
data class SupplementIntakeBackupDto(
    val dateIso: String,
    val supplementId: Long,
    val takenAtEpochMs: Long
)

@Serializable
data class MealTemplateBackupDto(
    val id: Long,
    val name: String,
    val defaultMealType: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Serializable
data class MealTemplateIngredientBackupDto(
    val id: Long,
    val templateId: Long,
    val foodItemId: Long,
    val position: Int,
    val defaultAmountValue: Double,
    val amountUnit: String
)
