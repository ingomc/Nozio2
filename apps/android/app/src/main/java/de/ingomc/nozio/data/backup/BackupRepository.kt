package de.ingomc.nozio.data.backup

import de.ingomc.nozio.data.local.DailyActivity
import de.ingomc.nozio.data.local.DiaryEntry
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.local.MealTemplateEntity
import de.ingomc.nozio.data.local.MealTemplateIngredientEntity
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.local.RecipeAmountUnit
import de.ingomc.nozio.data.local.SupplementAmountUnit
import de.ingomc.nozio.data.local.SupplementDayPart
import de.ingomc.nozio.data.local.SupplementIntakeEntity
import de.ingomc.nozio.data.local.SupplementPlanItemEntity
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface RestoreResult {
    data class Success(
        val foodCount: Int,
        val diaryEntryCount: Int,
        val dailyActivityCount: Int,
        val restoredFromEpochMs: Long,
        val supplementPlanCount: Int = 0,
        val supplementIntakeCount: Int = 0,
        val mealTemplateCount: Int = 0,
        val mealTemplateIngredientCount: Int = 0
    ) : RestoreResult

    data class Error(val message: String) : RestoreResult
}

interface BackupRepository {
    suspend fun createBackupJson(): String
    suspend fun restoreFromBackupJson(json: String): RestoreResult
}

class BackupRepositoryImpl(
    private val dataStore: TrackingDataStore,
    private val appVersionName: String,
    private val json: Json = Json {
        ignoreUnknownKeys = false
        prettyPrint = false
        explicitNulls = false
        encodeDefaults = true
    }
) : BackupRepository {
    override suspend fun createBackupJson(): String {
        val payload = BackupPayloadV3(
            createdAtEpochMs = System.currentTimeMillis(),
            appVersionName = appVersionName,
            foodItems = dataStore.getAllFoods().map { it.toDto() },
            diaryEntries = dataStore.getAllDiaryEntries().map { it.toDto() },
            dailyActivities = dataStore.getAllDailyActivities().map { it.toDto() },
            supplementPlanItems = dataStore.getAllSupplementPlanItems().map { it.toDto() },
            supplementIntakes = dataStore.getAllSupplementIntakes().map { it.toDto() },
            mealTemplates = dataStore.getAllMealTemplates().map { it.toDto() },
            mealTemplateIngredients = dataStore.getAllMealTemplateIngredients().map { it.toDto() }
        )
        return json.encodeToString(BackupPayloadV3.serializer(), payload)
    }

    override suspend fun restoreFromBackupJson(json: String): RestoreResult {
        val schemaVersion = parseSchemaVersion(json) ?: return RestoreResult.Error("Backup-Datei ist ungueltig.")

        val payload = when (schemaVersion) {
            SchemaVersions.V1 -> decodePayloadV1(json) ?: return RestoreResult.Error("Backup-Datei ist ungueltig.")
            SchemaVersions.V2 -> decodePayloadV2(json) ?: return RestoreResult.Error("Backup-Datei ist ungueltig.")
            SchemaVersions.V3 -> decodePayloadV3(json) ?: return RestoreResult.Error("Backup-Datei ist ungueltig.")
            else -> return RestoreResult.Error("Backup-Version wird nicht unterstuetzt.")
        }

        val foodItems = try {
            payload.foodItems.map { it.toEntity() }
        } catch (_: IllegalArgumentException) {
            return RestoreResult.Error("Backup-Datei enthaelt ungueltige Lebensmittel-Daten.")
        }
        val diaryEntries = try {
            payload.diaryEntries.map { it.toEntity() }
        } catch (_: IllegalArgumentException) {
            return RestoreResult.Error("Backup-Datei enthaelt ungueltige Tagebuch-Daten.")
        }
        val dailyActivities = try {
            payload.dailyActivities.map { it.toEntity() }
        } catch (_: IllegalArgumentException) {
            return RestoreResult.Error("Backup-Datei enthaelt ungueltige Aktivitaets-Daten.")
        }
        val supplementPlanItems = try {
            payload.supplementPlanItems.map { it.toEntity() }
        } catch (_: IllegalArgumentException) {
            return RestoreResult.Error("Backup-Datei enthaelt ungueltige Supplement-Plan-Daten.")
        }
        val supplementIntakes = try {
            payload.supplementIntakes.map { it.toEntity() }
        } catch (_: IllegalArgumentException) {
            return RestoreResult.Error("Backup-Datei enthaelt ungueltige Supplement-Intake-Daten.")
        }
        val mealTemplates = try {
            payload.mealTemplates.map { it.toEntity() }
        } catch (_: IllegalArgumentException) {
            return RestoreResult.Error("Backup-Datei enthaelt ungueltige Rezept-Daten.")
        }
        val mealTemplateIngredients = try {
            payload.mealTemplateIngredients.map { it.toEntity() }
        } catch (_: IllegalArgumentException) {
            return RestoreResult.Error("Backup-Datei enthaelt ungueltige Rezept-Zutaten-Daten.")
        }

        return runCatching {
            dataStore.replaceTrackingData(
                foodItems = foodItems,
                diaryEntries = diaryEntries,
                dailyActivities = dailyActivities,
                supplementPlanItems = supplementPlanItems,
                supplementIntakes = supplementIntakes,
                mealTemplates = mealTemplates,
                mealTemplateIngredients = mealTemplateIngredients
            )
            RestoreResult.Success(
                foodCount = foodItems.size,
                diaryEntryCount = diaryEntries.size,
                dailyActivityCount = dailyActivities.size,
                restoredFromEpochMs = payload.createdAtEpochMs,
                supplementPlanCount = supplementPlanItems.size,
                supplementIntakeCount = supplementIntakes.size,
                mealTemplateCount = mealTemplates.size,
                mealTemplateIngredientCount = mealTemplateIngredients.size
            )
        }.getOrElse {
            RestoreResult.Error("Wiederherstellung fehlgeschlagen.")
        }
    }

    private fun parseSchemaVersion(rawJson: String): Int? {
        return runCatching {
            json.parseToJsonElement(rawJson)
                .jsonObject["schemaVersion"]
                ?.jsonPrimitive
                ?.intOrNull
        }.getOrNull()
    }

    private fun decodePayloadV1(rawJson: String): NormalizedBackupPayload? {
        return runCatching {
            val payload = json.decodeFromString(BackupPayloadV1.serializer(), rawJson)
            NormalizedBackupPayload(
                createdAtEpochMs = payload.createdAtEpochMs,
                foodItems = payload.foodItems,
                diaryEntries = payload.diaryEntries,
                dailyActivities = payload.dailyActivities,
                supplementPlanItems = emptyList(),
                supplementIntakes = emptyList(),
                mealTemplates = emptyList(),
                mealTemplateIngredients = emptyList()
            )
        }.getOrNull()
    }

    private fun decodePayloadV2(rawJson: String): NormalizedBackupPayload? {
        return runCatching {
            val payload = json.decodeFromString(BackupPayloadV2.serializer(), rawJson)
            NormalizedBackupPayload(
                createdAtEpochMs = payload.createdAtEpochMs,
                foodItems = payload.foodItems,
                diaryEntries = payload.diaryEntries,
                dailyActivities = payload.dailyActivities,
                supplementPlanItems = payload.supplementPlanItems,
                supplementIntakes = payload.supplementIntakes,
                mealTemplates = emptyList(),
                mealTemplateIngredients = emptyList()
            )
        }.getOrNull()
    }

    private fun decodePayloadV3(rawJson: String): NormalizedBackupPayload? {
        return runCatching {
            val payload = json.decodeFromString(BackupPayloadV3.serializer(), rawJson)
            NormalizedBackupPayload(
                createdAtEpochMs = payload.createdAtEpochMs,
                foodItems = payload.foodItems,
                diaryEntries = payload.diaryEntries,
                dailyActivities = payload.dailyActivities,
                supplementPlanItems = payload.supplementPlanItems,
                supplementIntakes = payload.supplementIntakes,
                mealTemplates = payload.mealTemplates,
                mealTemplateIngredients = payload.mealTemplateIngredients
            )
        }.getOrNull()
    }
}

private data class NormalizedBackupPayload(
    val createdAtEpochMs: Long,
    val foodItems: List<FoodItemBackupDto>,
    val diaryEntries: List<DiaryEntryBackupDto>,
    val dailyActivities: List<DailyActivityBackupDto>,
    val supplementPlanItems: List<SupplementPlanItemBackupDto>,
    val supplementIntakes: List<SupplementIntakeBackupDto>,
    val mealTemplates: List<MealTemplateBackupDto>,
    val mealTemplateIngredients: List<MealTemplateIngredientBackupDto>
)

private fun FoodItem.toDto(): FoodItemBackupDto = FoodItemBackupDto(
    id = id,
    name = name,
    caloriesPer100g = caloriesPer100g,
    proteinPer100g = proteinPer100g,
    fatPer100g = fatPer100g,
    carbsPer100g = carbsPer100g,
    imageUrl = imageUrl,
    barcode = barcode,
    servingSize = servingSize,
    servingQuantity = servingQuantity,
    packageSize = packageSize,
    packageQuantity = packageQuantity,
    isFavorite = isFavorite,
    source = source.name
)

private fun FoodItemBackupDto.toEntity(): FoodItem = FoodItem(
    id = id,
    name = name,
    caloriesPer100g = caloriesPer100g,
    proteinPer100g = proteinPer100g,
    fatPer100g = fatPer100g,
    carbsPer100g = carbsPer100g,
    imageUrl = imageUrl,
    barcode = barcode,
    servingSize = servingSize,
    servingQuantity = servingQuantity,
    packageSize = packageSize,
    packageQuantity = packageQuantity,
    isFavorite = isFavorite,
    source = FoodSource.valueOf(source)
)

private fun DiaryEntry.toDto(): DiaryEntryBackupDto = DiaryEntryBackupDto(
    id = id,
    dateIso = date.toString(),
    mealType = mealType.name,
    foodItemId = foodItemId,
    amountInGrams = amountInGrams
)

private fun DiaryEntryBackupDto.toEntity(): DiaryEntry = DiaryEntry(
    id = id,
    date = LocalDate.parse(dateIso),
    mealType = MealType.valueOf(mealType),
    foodItemId = foodItemId,
    amountInGrams = amountInGrams
)

private fun DailyActivity.toDto(): DailyActivityBackupDto = DailyActivityBackupDto(
    dateIso = date.toString(),
    steps = steps,
    weightKg = weightKg,
    bodyFatPercent = bodyFatPercent
)

private fun DailyActivityBackupDto.toEntity(): DailyActivity = DailyActivity(
    date = LocalDate.parse(dateIso),
    steps = steps,
    weightKg = weightKg,
    bodyFatPercent = bodyFatPercent
)

private fun SupplementPlanItemEntity.toDto(): SupplementPlanItemBackupDto = SupplementPlanItemBackupDto(
    id = id,
    name = name,
    dayPart = dayPart.name,
    scheduledMinutesOfDay = scheduledMinutesOfDay,
    amountValue = amountValue,
    amountUnit = amountUnit.name
)

private fun SupplementPlanItemBackupDto.toEntity(): SupplementPlanItemEntity {
    require(scheduledMinutesOfDay in 0..1439) { "Invalid supplement time" }
    require(name.isNotBlank()) { "Supplement name cannot be blank" }
    require(amountValue > 0.0) { "Supplement amount must be > 0" }
    return SupplementPlanItemEntity(
        id = id,
        name = name.trim(),
        dayPart = SupplementDayPart.valueOf(dayPart),
        scheduledMinutesOfDay = scheduledMinutesOfDay,
        amountValue = amountValue,
        amountUnit = SupplementAmountUnit.valueOf(amountUnit)
    )
}

private fun SupplementIntakeEntity.toDto(): SupplementIntakeBackupDto = SupplementIntakeBackupDto(
    dateIso = date.toString(),
    supplementId = supplementId,
    takenAtEpochMs = takenAtEpochMs
)

private fun SupplementIntakeBackupDto.toEntity(): SupplementIntakeEntity = SupplementIntakeEntity(
    date = LocalDate.parse(dateIso),
    supplementId = supplementId,
    takenAtEpochMs = takenAtEpochMs
)

private fun MealTemplateEntity.toDto(): MealTemplateBackupDto = MealTemplateBackupDto(
    id = id,
    name = name,
    defaultMealType = defaultMealType.name,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

private fun MealTemplateBackupDto.toEntity(): MealTemplateEntity {
    require(name.isNotBlank()) { "Meal template name cannot be blank" }
    return MealTemplateEntity(
        id = id,
        name = name.trim(),
        defaultMealType = MealType.valueOf(defaultMealType),
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

private fun MealTemplateIngredientEntity.toDto(): MealTemplateIngredientBackupDto = MealTemplateIngredientBackupDto(
    id = id,
    templateId = templateId,
    foodItemId = foodItemId,
    position = position,
    defaultAmountValue = defaultAmountValue,
    amountUnit = amountUnit.name
)

private fun MealTemplateIngredientBackupDto.toEntity(): MealTemplateIngredientEntity = MealTemplateIngredientEntity(
    id = id,
    templateId = templateId,
    foodItemId = foodItemId,
    position = position,
    defaultAmountValue = defaultAmountValue,
    amountUnit = RecipeAmountUnit.valueOf(amountUnit)
)
