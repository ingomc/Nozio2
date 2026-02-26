package de.ingomc.nozio.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.repository.DailyActivityRepository
import de.ingomc.nozio.data.repository.UserPreferences
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

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
    val saved: Boolean = false,
    val hasChanges: Boolean = false,
    val weightHistory: List<WeightHistoryPoint> = emptyList()
)

class ProfileViewModel(
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
                dailyActivityRepository.getLatestWeightEntry()
            ) { prefs, latestWeightEntry ->
                val effectiveCurrentWeight = latestWeightEntry?.weightKg ?: prefs.currentWeightKg
                prefs.copy(currentWeightKg = effectiveCurrentWeight)
            }.collectLatest { effectivePrefs ->
                baselinePreferences = effectivePrefs
                _uiState.value = ProfileUiState(
                    calorieGoal = effectivePrefs.calorieGoal.toInt().toString(),
                    proteinGoal = effectivePrefs.proteinGoal.toInt().toString(),
                    fatGoal = effectivePrefs.fatGoal.toInt().toString(),
                    carbsGoal = effectivePrefs.carbsGoal.toInt().toString(),
                    currentWeightKg = formatWeightForInput(effectivePrefs.currentWeightKg),
                    bodyFatPercent = formatDecimalForInput(effectivePrefs.bodyFatPercent),
                    hasChanges = false,
                    weightHistory = _uiState.value.weightHistory
                )
            }
        }
        viewModelScope.launch {
            dailyActivityRepository.getWeightHistory().collectLatest { history ->
                _uiState.value = _uiState.value.copy(
                    weightHistory = history.map { WeightHistoryPoint(it.date, it.weightKg) }
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
                state.toPreferences()
            )
            _uiState.value = _uiState.value.copy(saved = true, hasChanges = false)
        }
    }

    private fun updateState(candidate: ProfileUiState) {
        _uiState.value = candidate.copy(hasChanges = hasChanges(candidate))
    }

    private fun hasChanges(state: ProfileUiState): Boolean {
        val baseline = baselinePreferences ?: return false
        val current = state.toPreferences()
        return current != baseline
    }

    private fun ProfileUiState.toPreferences(): UserPreferences = UserPreferences(
        calorieGoal = calorieGoal.toDoubleOrNull() ?: 2000.0,
        proteinGoal = proteinGoal.toDoubleOrNull() ?: 75.0,
        fatGoal = fatGoal.toDoubleOrNull() ?: 65.0,
        carbsGoal = carbsGoal.toDoubleOrNull() ?: 250.0,
        currentWeightKg = currentWeightKg.toDoubleOrNull() ?: 80.0,
        bodyFatPercent = bodyFatPercent.toDoubleOrNull() ?: 20.0
    )

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
        private val userPreferencesRepository: UserPreferencesRepository,
        private val dailyActivityRepository: DailyActivityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(userPreferencesRepository, dailyActivityRepository) as T
        }
    }
}
