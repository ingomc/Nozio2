package de.ingomc.nozio.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.repository.DailyActivityRepository
import de.ingomc.nozio.data.repository.UserPreferences
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import de.ingomc.nozio.widget.CalorieWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class WeightHistoryPoint(
    val date: LocalDate,
    val weightKg: Double
)

data class ProfileUiState(
    val calorieGoal: String = "2000",
    val proteinGoal: String = "75",
    val fatGoal: String = "65",
    val carbsGoal: String = "250",
    val currentWeightKg: String = "80",
    val bodyFatPercent: String = "20",
    val todaySteps: Long = 0L,
    val latestWeightKg: Double? = null,
    val weightTrendDeltaKg: Double? = null,
    val weightTrendWeeksEstimate: Int? = null,
    val goalSummaryItems: List<String> = emptyList(),
    val saved: Boolean = false,
    val hasChanges: Boolean = false,
    val weightHistory: List<WeightHistoryPoint> = emptyList()
)

class ProfileViewModel(
    private val appContext: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dailyActivityRepository: DailyActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var baselinePreferences: UserPreferences? = null

    init {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.userPreferences,
                dailyActivityRepository.getLatestWeightEntry(),
                dailyActivityRepository.getWeightHistory(),
                dailyActivityRepository.getStepsForDate(LocalDate.now())
            ) { prefs, latestWeightEntry, history, todaySteps ->
                val effectiveCurrentWeight = latestWeightEntry?.weightKg ?: prefs.currentWeightKg
                val effectivePrefs = prefs.copy(currentWeightKg = effectiveCurrentWeight)
                val weightHistory = history.map { WeightHistoryPoint(it.date, it.weightKg) }
                val trend = computeWeightTrend(
                    weightHistory = weightHistory,
                    goalWeightKg = effectivePrefs.currentWeightKg,
                    latestWeightKg = latestWeightEntry?.weightKg
                )
                CombinedProfileData(
                    prefs = effectivePrefs,
                    todaySteps = todaySteps,
                    latestWeightKg = latestWeightEntry?.weightKg,
                    weightHistory = weightHistory,
                    weightTrendDeltaKg = trend.deltaKg,
                    weightTrendWeeksEstimate = trend.weeksEstimate,
                    goalSummaryItems = buildGoalSummaryItems(effectivePrefs, todaySteps)
                )
            }.collectLatest { combined ->
                baselinePreferences = combined.prefs
                _uiState.value = ProfileUiState(
                    calorieGoal = combined.prefs.calorieGoal.toInt().toString(),
                    proteinGoal = combined.prefs.proteinGoal.toInt().toString(),
                    fatGoal = combined.prefs.fatGoal.toInt().toString(),
                    carbsGoal = combined.prefs.carbsGoal.toInt().toString(),
                    currentWeightKg = formatWeightForInput(combined.prefs.currentWeightKg),
                    bodyFatPercent = formatDecimalForInput(combined.prefs.bodyFatPercent),
                    todaySteps = combined.todaySteps,
                    latestWeightKg = combined.latestWeightKg,
                    weightTrendDeltaKg = combined.weightTrendDeltaKg,
                    weightTrendWeeksEstimate = combined.weightTrendWeeksEstimate,
                    goalSummaryItems = combined.goalSummaryItems,
                    hasChanges = false,
                    weightHistory = combined.weightHistory
                )
            }
        }
    }

    fun onCalorieGoalChange(value: String) {
        updateState(_uiState.value.copy(calorieGoal = value, saved = false))
    }

    fun onProteinGoalChange(value: String) {
        updateState(_uiState.value.copy(proteinGoal = value, saved = false))
    }

    fun onFatGoalChange(value: String) {
        updateState(_uiState.value.copy(fatGoal = value, saved = false))
    }

    fun onCarbsGoalChange(value: String) {
        updateState(_uiState.value.copy(carbsGoal = value, saved = false))
    }

    fun onCurrentWeightKgChange(value: String) {
        updateState(_uiState.value.copy(currentWeightKg = value, saved = false))
    }

    fun onBodyFatPercentChange(value: String) {
        updateState(_uiState.value.copy(bodyFatPercent = value, saved = false))
    }

    fun save() {
        val state = _uiState.value
        if (!state.hasChanges) return

        viewModelScope.launch {
            userPreferencesRepository.updatePreferences(
                state.toPreferences(baselinePreferences)
            )
            CalorieWidgetProvider.updateAll(appContext)
            _uiState.value = _uiState.value.copy(saved = true, hasChanges = false)
        }
    }

    private fun updateState(candidate: ProfileUiState) {
        _uiState.value = candidate.copy(hasChanges = hasChanges(candidate))
    }

    private fun hasChanges(state: ProfileUiState): Boolean {
        val baseline = baselinePreferences ?: return false
        val current = state.toPreferences(baseline)
        return current != baseline
    }

    private fun ProfileUiState.toPreferences(base: UserPreferences?): UserPreferences {
        val fallback = base ?: UserPreferences()
        return fallback.copy(
            calorieGoal = calorieGoal.toDoubleOrNull() ?: 2000.0,
            proteinGoal = proteinGoal.toDoubleOrNull() ?: 75.0,
            fatGoal = fatGoal.toDoubleOrNull() ?: 65.0,
            carbsGoal = carbsGoal.toDoubleOrNull() ?: 250.0,
            currentWeightKg = currentWeightKg.toDoubleOrNull() ?: 80.0,
            bodyFatPercent = bodyFatPercent.toDoubleOrNull() ?: 20.0
        )
    }

    private fun formatWeightForInput(value: Double): String {
        return String.format(Locale.US, "%.1f", value)
    }

    private fun formatDecimalForInput(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }

    class Factory(
        private val appContext: Context,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val dailyActivityRepository: DailyActivityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(appContext, userPreferencesRepository, dailyActivityRepository) as T
        }
    }
}

private data class CombinedProfileData(
    val prefs: UserPreferences,
    val todaySteps: Long,
    val latestWeightKg: Double?,
    val weightHistory: List<WeightHistoryPoint>,
    val weightTrendDeltaKg: Double?,
    val weightTrendWeeksEstimate: Int?,
    val goalSummaryItems: List<String>
)

internal data class WeightTrendSummary(
    val deltaKg: Double?,
    val weeksEstimate: Int?
)

internal fun computeWeightTrend(
    weightHistory: List<WeightHistoryPoint>,
    goalWeightKg: Double,
    latestWeightKg: Double?
): WeightTrendSummary {
    if (weightHistory.size < 2) return WeightTrendSummary(deltaKg = null, weeksEstimate = null)

    val start = weightHistory.first()
    val end = weightHistory.last()
    val delta = end.weightKg - start.weightKg
    val days = (end.date.toEpochDay() - start.date.toEpochDay()).coerceAtLeast(1L)
    val weeks = days / 7.0
    if (weeks <= 0.0) return WeightTrendSummary(deltaKg = delta, weeksEstimate = null)

    val weeklyDelta = delta / weeks
    val latest = latestWeightKg ?: end.weightKg
    val remainingToGoal = goalWeightKg - latest
    val movingTowardGoal =
        (remainingToGoal > 0 && weeklyDelta > 0) || (remainingToGoal < 0 && weeklyDelta < 0)

    val weeksEstimate = if (movingTowardGoal && abs(weeklyDelta) >= 0.05) {
        (abs(remainingToGoal / weeklyDelta)).roundToInt().coerceAtLeast(1)
    } else {
        null
    }

    return WeightTrendSummary(deltaKg = delta, weeksEstimate = weeksEstimate)
}

internal fun buildGoalSummaryItems(
    preferences: UserPreferences,
    todaySteps: Long
): List<String> {
    val weight = String.format(Locale.GERMAN, "%.1f", preferences.currentWeightKg)
    val bodyFat = String.format(Locale.GERMAN, "%.1f", preferences.bodyFatPercent)
    return listOf(
        "Kalorien: ${preferences.calorieGoal.toInt()} kcal",
        "Makros: EW ${preferences.proteinGoal.toInt()} g · Fett ${preferences.fatGoal.toInt()} g · KH ${preferences.carbsGoal.toInt()} g",
        "Gewicht: $weight kg",
        "KFA: $bodyFat %",
        "Heute Schritte: $todaySteps"
    )
}
