package de.ingomc.nozio.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.local.DiaryEntryWithFood
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.repository.DiaryRepository
import de.ingomc.nozio.data.repository.UserPreferences
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val entriesByMeal: Map<MealType, List<DiaryEntryWithFood>> = MealType.entries.associateWith { emptyList() },
    val preferences: UserPreferences = UserPreferences()
)

class DashboardViewModel(
    private val diaryRepository: DiaryRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                diaryRepository.getDaySummary(LocalDate.now()),
                userPreferencesRepository.userPreferences
            ) { summary, prefs ->
                DashboardUiState(
                    totalCalories = summary.totalCalories,
                    totalProtein = summary.totalProtein,
                    totalFat = summary.totalFat,
                    totalCarbs = summary.totalCarbs,
                    entriesByMeal = summary.entriesByMeal,
                    preferences = prefs
                )
            }.collect { _uiState.value = it }
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            diaryRepository.deleteEntry(entryId)
        }
    }

    class Factory(
        private val diaryRepository: DiaryRepository,
        private val userPreferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(diaryRepository, userPreferencesRepository) as T
        }
    }
}

