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
    val mealReminderHour: String = "19",
    val mealReminderMinute: String = "00",
    val saved: Boolean = false,
    val hasChanges: Boolean = false
)

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val onReminderChanged: (enabled: Boolean, hour: Int, minute: Int) -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var baselineState = SettingsUiState()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collectLatest { prefs ->
                val freshState = SettingsUiState(
                    themeMode = prefs.themeMode,
                    mealReminderEnabled = prefs.mealReminderEnabled,
                    mealReminderHour = prefs.mealReminderHour.toString(),
                    mealReminderMinute = "%02d".format(prefs.mealReminderMinute)
                )
                baselineState = freshState
                _uiState.value = freshState
            }
        }
    }

    fun onThemeModeChange(mode: AppThemeMode) = update { it.copy(themeMode = mode, saved = false) }
    fun onMealReminderEnabledChange(enabled: Boolean) = update { it.copy(mealReminderEnabled = enabled, saved = false) }
    fun onMealReminderHourChange(hour: String) = update { it.copy(mealReminderHour = hour.filter(Char::isDigit), saved = false) }
    fun onMealReminderMinuteChange(minute: String) = update { it.copy(mealReminderMinute = minute.filter(Char::isDigit), saved = false) }

    fun save() {
        val state = _uiState.value
        if (!state.hasChanges) return

        val hour = state.mealReminderHour.toIntOrNull()?.coerceIn(0, 23) ?: 19
        val minute = state.mealReminderMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0

        viewModelScope.launch {
            val currentPrefs = userPreferencesRepository.userPreferences.firstOrNull() ?: return@launch
            userPreferencesRepository.updatePreferences(
                currentPrefs.copy(
                    themeMode = state.themeMode,
                    mealReminderEnabled = state.mealReminderEnabled,
                    mealReminderHour = hour,
                    mealReminderMinute = minute
                )
            )
            onReminderChanged(state.mealReminderEnabled, hour, minute)
            _uiState.value = _uiState.value.copy(
                mealReminderHour = hour.toString(),
                mealReminderMinute = "%02d".format(minute),
                saved = true,
                hasChanges = false
            )
        }
    }

    private fun update(transform: (SettingsUiState) -> SettingsUiState) {
        val candidate = transform(_uiState.value)
        _uiState.value = candidate.copy(hasChanges = hasChanges(candidate))
    }

    private fun hasChanges(candidate: SettingsUiState): Boolean {
        return candidate.themeMode != baselineState.themeMode ||
            candidate.mealReminderEnabled != baselineState.mealReminderEnabled ||
            candidate.mealReminderHour != baselineState.mealReminderHour ||
            candidate.mealReminderMinute != baselineState.mealReminderMinute
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
