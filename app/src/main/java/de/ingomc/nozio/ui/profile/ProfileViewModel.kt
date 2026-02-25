package de.ingomc.nozio.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.repository.UserPreferences
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val calorieGoal: String = "2000",
    val proteinGoal: String = "75",
    val fatGoal: String = "65",
    val carbsGoal: String = "250",
    val saved: Boolean = false
)

class ProfileViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collect { prefs ->
                _uiState.value = ProfileUiState(
                    calorieGoal = prefs.calorieGoal.toInt().toString(),
                    proteinGoal = prefs.proteinGoal.toInt().toString(),
                    fatGoal = prefs.fatGoal.toInt().toString(),
                    carbsGoal = prefs.carbsGoal.toInt().toString()
                )
            }
        }
    }

    fun onCalorieGoalChange(value: String) {
        _uiState.value = _uiState.value.copy(calorieGoal = value, saved = false)
    }

    fun onProteinGoalChange(value: String) {
        _uiState.value = _uiState.value.copy(proteinGoal = value, saved = false)
    }

    fun onFatGoalChange(value: String) {
        _uiState.value = _uiState.value.copy(fatGoal = value, saved = false)
    }

    fun onCarbsGoalChange(value: String) {
        _uiState.value = _uiState.value.copy(carbsGoal = value, saved = false)
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            userPreferencesRepository.updatePreferences(
                UserPreferences(
                    calorieGoal = state.calorieGoal.toDoubleOrNull() ?: 2000.0,
                    proteinGoal = state.proteinGoal.toDoubleOrNull() ?: 75.0,
                    fatGoal = state.fatGoal.toDoubleOrNull() ?: 65.0,
                    carbsGoal = state.carbsGoal.toDoubleOrNull() ?: 250.0
                )
            )
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    class Factory(
        private val userPreferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(userPreferencesRepository) as T
        }
    }
}

