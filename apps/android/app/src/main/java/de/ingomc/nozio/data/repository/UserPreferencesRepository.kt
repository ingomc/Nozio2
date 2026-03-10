package de.ingomc.nozio.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class AppThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageValue(value: String?): AppThemeMode {
            return entries.find { it.storageValue == value } ?: SYSTEM
        }
    }
}

data class UserPreferences(
    val calorieGoal: Double = 2000.0,
    val proteinGoal: Double = 75.0,
    val fatGoal: Double = 65.0,
    val carbsGoal: Double = 250.0,
    val currentWeightKg: Double = 80.0,
    val goalStartWeightKg: Double = 80.0,
    val goalTargetWeightKg: Double = 78.0,
    val bodyFatPercent: Double = 20.0,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val includeActivityCaloriesInBudget: Boolean = true,
    val autoBackupEnabled: Boolean = true,
    val mealReminderEnabled: Boolean = false,
    val mealReminderHour: Int = 19,
    val mealReminderMinute: Int = 0,
    val profileImageUpdatedAt: Long = 0L
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val CALORIE_GOAL = doublePreferencesKey("calorie_goal")
        val PROTEIN_GOAL = doublePreferencesKey("protein_goal")
        val FAT_GOAL = doublePreferencesKey("fat_goal")
        val CARBS_GOAL = doublePreferencesKey("carbs_goal")
        val CURRENT_WEIGHT_KG = doublePreferencesKey("current_weight_kg")
        val GOAL_START_WEIGHT_KG = doublePreferencesKey("goal_start_weight_kg")
        val GOAL_TARGET_WEIGHT_KG = doublePreferencesKey("goal_target_weight_kg")
        val BODY_FAT_PERCENT = doublePreferencesKey("body_fat_percent")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val INCLUDE_ACTIVITY_CALORIES_IN_BUDGET = booleanPreferencesKey("include_activity_calories_in_budget")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val MEAL_REMINDER_ENABLED = booleanPreferencesKey("meal_reminder_enabled")
        val MEAL_REMINDER_HOUR = intPreferencesKey("meal_reminder_hour")
        val MEAL_REMINDER_MINUTE = intPreferencesKey("meal_reminder_minute")
        val PROFILE_IMAGE_UPDATED_AT = longPreferencesKey("profile_image_updated_at")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            calorieGoal = prefs[Keys.CALORIE_GOAL] ?: 2000.0,
            proteinGoal = prefs[Keys.PROTEIN_GOAL] ?: 75.0,
            fatGoal = prefs[Keys.FAT_GOAL] ?: 65.0,
            carbsGoal = prefs[Keys.CARBS_GOAL] ?: 250.0,
            currentWeightKg = prefs[Keys.CURRENT_WEIGHT_KG] ?: 80.0,
            goalStartWeightKg = prefs[Keys.GOAL_START_WEIGHT_KG] ?: 80.0,
            goalTargetWeightKg = prefs[Keys.GOAL_TARGET_WEIGHT_KG] ?: 78.0,
            bodyFatPercent = prefs[Keys.BODY_FAT_PERCENT] ?: 20.0,
            themeMode = AppThemeMode.fromStorageValue(prefs[Keys.THEME_MODE]),
            includeActivityCaloriesInBudget = prefs[Keys.INCLUDE_ACTIVITY_CALORIES_IN_BUDGET] ?: true,
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: true,
            mealReminderEnabled = prefs[Keys.MEAL_REMINDER_ENABLED] ?: false,
            mealReminderHour = prefs[Keys.MEAL_REMINDER_HOUR] ?: 19,
            mealReminderMinute = prefs[Keys.MEAL_REMINDER_MINUTE] ?: 0,
            profileImageUpdatedAt = prefs[Keys.PROFILE_IMAGE_UPDATED_AT] ?: 0L
        )
    }

    suspend fun updatePreferences(preferences: UserPreferences) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CALORIE_GOAL] = preferences.calorieGoal
            prefs[Keys.PROTEIN_GOAL] = preferences.proteinGoal
            prefs[Keys.FAT_GOAL] = preferences.fatGoal
            prefs[Keys.CARBS_GOAL] = preferences.carbsGoal
            prefs[Keys.CURRENT_WEIGHT_KG] = preferences.currentWeightKg
            prefs[Keys.GOAL_START_WEIGHT_KG] = preferences.goalStartWeightKg
            prefs[Keys.GOAL_TARGET_WEIGHT_KG] = preferences.goalTargetWeightKg
            prefs[Keys.BODY_FAT_PERCENT] = preferences.bodyFatPercent
            prefs[Keys.THEME_MODE] = preferences.themeMode.storageValue
            prefs[Keys.INCLUDE_ACTIVITY_CALORIES_IN_BUDGET] = preferences.includeActivityCaloriesInBudget
            prefs[Keys.AUTO_BACKUP_ENABLED] = preferences.autoBackupEnabled
            prefs[Keys.MEAL_REMINDER_ENABLED] = preferences.mealReminderEnabled
            prefs[Keys.MEAL_REMINDER_HOUR] = preferences.mealReminderHour.coerceIn(0, 23)
            prefs[Keys.MEAL_REMINDER_MINUTE] = preferences.mealReminderMinute.coerceIn(0, 59)
            prefs[Keys.PROFILE_IMAGE_UPDATED_AT] = preferences.profileImageUpdatedAt
        }
    }

    suspend fun updateProfileImageUpdatedAt(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROFILE_IMAGE_UPDATED_AT] = timestamp
        }
    }
}
