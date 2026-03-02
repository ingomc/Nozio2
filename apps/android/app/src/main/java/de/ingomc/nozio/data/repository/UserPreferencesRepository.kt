package de.ingomc.nozio.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val calorieGoal: Double = 2000.0,
    val proteinGoal: Double = 75.0,
    val fatGoal: Double = 65.0,
    val carbsGoal: Double = 250.0,
    val currentWeightKg: Double = 80.0,
    val bodyFatPercent: Double = 20.0
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val CALORIE_GOAL = doublePreferencesKey("calorie_goal")
        val PROTEIN_GOAL = doublePreferencesKey("protein_goal")
        val FAT_GOAL = doublePreferencesKey("fat_goal")
        val CARBS_GOAL = doublePreferencesKey("carbs_goal")
        val CURRENT_WEIGHT_KG = doublePreferencesKey("current_weight_kg")
        val BODY_FAT_PERCENT = doublePreferencesKey("body_fat_percent")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            calorieGoal = prefs[Keys.CALORIE_GOAL] ?: 2000.0,
            proteinGoal = prefs[Keys.PROTEIN_GOAL] ?: 75.0,
            fatGoal = prefs[Keys.FAT_GOAL] ?: 65.0,
            carbsGoal = prefs[Keys.CARBS_GOAL] ?: 250.0,
            currentWeightKg = prefs[Keys.CURRENT_WEIGHT_KG] ?: 80.0,
            bodyFatPercent = prefs[Keys.BODY_FAT_PERCENT] ?: 20.0
        )
    }

    suspend fun updatePreferences(preferences: UserPreferences) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CALORIE_GOAL] = preferences.calorieGoal
            prefs[Keys.PROTEIN_GOAL] = preferences.proteinGoal
            prefs[Keys.FAT_GOAL] = preferences.fatGoal
            prefs[Keys.CARBS_GOAL] = preferences.carbsGoal
            prefs[Keys.CURRENT_WEIGHT_KG] = preferences.currentWeightKg
            prefs[Keys.BODY_FAT_PERCENT] = preferences.bodyFatPercent
        }
    }
}
