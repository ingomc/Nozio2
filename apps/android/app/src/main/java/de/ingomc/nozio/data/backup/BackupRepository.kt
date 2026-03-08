package de.ingomc.nozio.data.backup

import de.ingomc.nozio.data.local.DailyActivity
import de.ingomc.nozio.data.local.DiaryEntry
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.local.MealType
import java.time.LocalDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed interface RestoreResult {
    data class Success(
        val foodCount: Int,
        val diaryEntryCount: Int,
        val dailyActivityCount: Int,
        val restoredFromEpochMs: Long
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
        val payload = BackupPayloadV1(
            createdAtEpochMs = System.currentTimeMillis(),
            appVersionName = appVersionName,
            foodItems = dataStore.getAllFoods().map { it.toDto() },
            diaryEntries = dataStore.getAllDiaryEntries().map { it.toDto() },
            dailyActivities = dataStore.getAllDailyActivities().map { it.toDto() }
        )
        return json.encodeToString(BackupPayloadV1.serializer(), payload)
    }

    override suspend fun restoreFromBackupJson(json: String): RestoreResult {
        val payload = try {
            this.json.decodeFromString(BackupPayloadV1.serializer(), json)
        } catch (_: SerializationException) {
            return RestoreResult.Error("Backup-Datei ist ungueltig.")
        } catch (_: IllegalArgumentException) {
            return RestoreResult.Error("Backup-Datei ist ungueltig.")
        }

        if (payload.schemaVersion != BackupPayloadV1.SCHEMA_VERSION) {
            return RestoreResult.Error("Backup-Version wird nicht unterstuetzt.")
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

        return runCatching {
            dataStore.replaceTrackingData(
                foodItems = foodItems,
                diaryEntries = diaryEntries,
                dailyActivities = dailyActivities
            )
            RestoreResult.Success(
                foodCount = foodItems.size,
                diaryEntryCount = diaryEntries.size,
                dailyActivityCount = dailyActivities.size,
                restoredFromEpochMs = payload.createdAtEpochMs
            )
        }.getOrElse {
            RestoreResult.Error("Wiederherstellung fehlgeschlagen.")
        }
    }
}

private fun FoodItem.toDto(): FoodItemBackupDto = FoodItemBackupDto(
    id = id,
    name = name,
    caloriesPer100g = caloriesPer100g,
    proteinPer100g = proteinPer100g,
    fatPer100g = fatPer100g,
    carbsPer100g = carbsPer100g,
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
    weightKg = weightKg
)

private fun DailyActivityBackupDto.toEntity(): DailyActivity = DailyActivity(
    date = LocalDate.parse(dateIso),
    steps = steps,
    weightKg = weightKg
)
