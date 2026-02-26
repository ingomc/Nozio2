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
import kotlinx.coroutines.launch
import java.time.LocalDate

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
            userPreferencesRepository.userPreferences.collectLatest { prefs ->
                baselinePreferences = prefs
                _uiState.value = ProfileUiState(
                    calorieGoal = prefs.calorieGoal.toInt().toString(),
                    proteinGoal = prefs.proteinGoal.toInt().toString(),
                    fatGoal = prefs.fatGoal.toInt().toString(),
                    carbsGoal = prefs.carbsGoal.toInt().toString(),
                    currentWeightKg = prefs.currentWeightKg.toString(),
                    bodyFatPercent = prefs.bodyFatPercent.toString(),
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
