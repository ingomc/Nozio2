package de.ingomc.nozio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.repository.AppThemeMode
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val mealReminderEnabled: Boolean = false,
    val mealReminderHour: Int = 19,
    val mealReminderMinute: Int = 0
)

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val onReminderChanged: (enabled: Boolean, hour: Int, minute: Int) -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collectLatest { prefs ->
                _uiState.value = SettingsUiState(
                    themeMode = prefs.themeMode,
                    mealReminderEnabled = prefs.mealReminderEnabled,
                    mealReminderHour = prefs.mealReminderHour,
                    mealReminderMinute = prefs.mealReminderMinute
                )
            }
        }
    }

    fun onThemeModeChange(mode: AppThemeMode) {
        viewModelScope.launch {
            val currentPrefs = userPreferencesRepository.userPreferences.firstOrNull() ?: return@launch
            if (currentPrefs.themeMode == mode) return@launch
            userPreferencesRepository.updatePreferences(currentPrefs.copy(themeMode = mode))
        }
    }

    fun onMealReminderEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            val currentPrefs = userPreferencesRepository.userPreferences.firstOrNull() ?: return@launch
            if (currentPrefs.mealReminderEnabled == enabled) return@launch
            userPreferencesRepository.updatePreferences(currentPrefs.copy(mealReminderEnabled = enabled))
            onReminderChanged(enabled, currentPrefs.mealReminderHour, currentPrefs.mealReminderMinute)
        }
    }

    fun onMealReminderTimeChange(hour: Int, minute: Int) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)

        viewModelScope.launch {
            val currentPrefs = userPreferencesRepository.userPreferences.firstOrNull() ?: return@launch
            if (currentPrefs.mealReminderHour == safeHour && currentPrefs.mealReminderMinute == safeMinute) {
                return@launch
            }
            userPreferencesRepository.updatePreferences(
                currentPrefs.copy(
                    mealReminderHour = safeHour,
                    mealReminderMinute = safeMinute
                )
            )
            onReminderChanged(currentPrefs.mealReminderEnabled, safeHour, safeMinute)
        }
    }

    class Factory(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val onReminderChanged: (enabled: Boolean, hour: Int, minute: Int) -> Unit
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(userPreferencesRepository, onReminderChanged) as T
        }
    }
}
