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
    val goalStartWeightKg: String = "80",
    val goalTargetWeightKg: String = "78",
    val todaySteps: Long = 0L,
    val latestWeightKg: Double? = null,
    val latestBodyFatPercent: Double? = null,
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
                dailyActivityRepository.getLatestBodyFatEntry(),
                dailyActivityRepository.getWeightHistory(),
                dailyActivityRepository.getStepsForDate(LocalDate.now())
            ) { prefs, latestWeightEntry, latestBodyFatEntry, history, todaySteps ->
                val weightHistory = history.map { WeightHistoryPoint(it.date, it.weightKg) }
                val trend = computeWeightTrend(
                    weightHistory = weightHistory,
                    goalWeightKg = prefs.goalTargetWeightKg,
                    latestWeightKg = latestWeightEntry?.weightKg
                )
                CombinedProfileData(
                    prefs = prefs,
                    todaySteps = todaySteps,
                    latestWeightKg = latestWeightEntry?.weightKg,
                    latestBodyFatPercent = latestBodyFatEntry?.bodyFatPercent ?: prefs.bodyFatPercent,
                    weightHistory = weightHistory,
                    weightTrendDeltaKg = trend.deltaKg,
                    weightTrendWeeksEstimate = trend.weeksEstimate,
                    goalSummaryItems = buildGoalSummaryItems(prefs, todaySteps)
                )
            }.collectLatest { combined ->
                baselinePreferences = combined.prefs
                _uiState.value = ProfileUiState(
                    calorieGoal = combined.prefs.calorieGoal.toInt().toString(),
                    proteinGoal = combined.prefs.proteinGoal.toInt().toString(),
                    fatGoal = combined.prefs.fatGoal.toInt().toString(),
                    carbsGoal = combined.prefs.carbsGoal.toInt().toString(),
                    goalStartWeightKg = formatWeightForInput(combined.prefs.goalStartWeightKg),
                    goalTargetWeightKg = formatWeightForInput(combined.prefs.goalTargetWeightKg),
                    todaySteps = combined.todaySteps,
                    latestWeightKg = combined.latestWeightKg,
                    latestBodyFatPercent = combined.latestBodyFatPercent,
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

    fun onGoalStartWeightKgChange(value: String) {
        updateState(_uiState.value.copy(goalStartWeightKg = value, saved = false))
    }

    fun onGoalTargetWeightKgChange(value: String) {
        updateState(_uiState.value.copy(goalTargetWeightKg = value, saved = false))
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
            goalStartWeightKg = goalStartWeightKg.toDoubleOrNull() ?: fallback.goalStartWeightKg,
            goalTargetWeightKg = goalTargetWeightKg.toDoubleOrNull() ?: fallback.goalTargetWeightKg
        )
    }

    private fun formatWeightForInput(value: Double): String {
        return String.format(Locale.US, "%.1f", value)
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
    val latestBodyFatPercent: Double?,
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
    val startWeight = String.format(Locale.GERMAN, "%.1f", preferences.goalStartWeightKg)
    val targetWeight = String.format(Locale.GERMAN, "%.1f", preferences.goalTargetWeightKg)
    return listOf(
        "Kalorien: ${preferences.calorieGoal.toInt()} kcal",
        "Makros: EW ${preferences.proteinGoal.toInt()} g · Fett ${preferences.fatGoal.toInt()} g · KH ${preferences.carbsGoal.toInt()} g",
        "Gewicht: Start $startWeight kg · Ziel $targetWeight kg",
        "Heute Schritte: $todaySteps"
    )
}
