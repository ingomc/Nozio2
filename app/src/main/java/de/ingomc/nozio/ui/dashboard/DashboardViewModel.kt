package de.ingomc.nozio.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.local.DiaryEntryWithFood
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.repository.DiaryRepository
import de.ingomc.nozio.data.repository.HealthConnectRepository
import de.ingomc.nozio.data.repository.UserPreferences
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class DashboardUiState(
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val entriesByMeal: Map<MealType, List<DiaryEntryWithFood>> = MealType.entries.associateWith { emptyList() },
    val preferences: UserPreferences = UserPreferences(),
    val totalSteps: Long = 0L,
    val activeCalories: Double = 0.0,
    val healthDebugLogs: List<String> = emptyList()
)

class DashboardViewModel(
    private val diaryRepository: DiaryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val healthConnectRepository: HealthConnectRepository
) : ViewModel() {

    private val totalSteps = MutableStateFlow(0L)
    private val activeCalories = MutableStateFlow(0.0)
    private val healthDebugLogs = MutableStateFlow<List<String>>(emptyList())

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshHealthData()
        viewModelScope.launch {
            combine(
                diaryRepository.getDaySummary(LocalDate.now()),
                userPreferencesRepository.userPreferences,
                totalSteps,
                activeCalories,
                healthDebugLogs
            ) { summary, prefs, stepsValue, activeCaloriesValue, debugLogs ->
                DashboardUiState(
                    totalCalories = summary.totalCalories,
                    totalProtein = summary.totalProtein,
                    totalFat = summary.totalFat,
                    totalCarbs = summary.totalCarbs,
                    entriesByMeal = summary.entriesByMeal,
                    preferences = prefs,
                    totalSteps = stepsValue,
                    activeCalories = activeCaloriesValue,
                    healthDebugLogs = debugLogs
                )
            }.collect { _uiState.value = it }
        }
    }

    fun refreshHealthData() {
        viewModelScope.launch {
            logHealthEvent("Lese Health-Daten...")
            totalSteps.value = healthConnectRepository.getTodaySteps()
            activeCalories.value = healthConnectRepository.getTodayActiveCalories()
            logHealthEvent("Daten gelesen: Schritte=${totalSteps.value}, Aktivitäts-kcal=${"%.0f".format(activeCalories.value)}")
        }
    }

    fun logHealthEvent(message: String) {
        val timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        healthDebugLogs.value = (healthDebugLogs.value + "[$timestamp] $message").takeLast(80)
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            diaryRepository.deleteEntry(entryId)
        }
    }

    class Factory(
        private val diaryRepository: DiaryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val healthConnectRepository: HealthConnectRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(diaryRepository, userPreferencesRepository, healthConnectRepository) as T
        }
    }
}
