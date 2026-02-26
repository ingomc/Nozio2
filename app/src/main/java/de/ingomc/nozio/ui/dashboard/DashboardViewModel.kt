package de.ingomc.nozio.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.local.DiaryEntryWithFood
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.repository.DailyActivityRepository
import de.ingomc.nozio.data.repository.DiaryRepository
import de.ingomc.nozio.data.repository.UserPreferences
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val entriesByMeal: Map<MealType, List<DiaryEntryWithFood>> = MealType.entries.associateWith { emptyList() },
    val preferences: UserPreferences = UserPreferences(),
    val totalSteps: Long = 0L,
    val activeCalories: Double = 0.0,
    val stepsInput: String = "0",
    val stepsSaved: Boolean = false
)

class DashboardViewModel(
    private val diaryRepository: DiaryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dailyActivityRepository: DailyActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val selectedDateState = MutableStateFlow(LocalDate.now())
    private val stepsInputState = MutableStateFlow("0")
    private val hasEditedStepsInput = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val daySummaryFlow = selectedDateState.flatMapLatest { date ->
                diaryRepository.getDaySummary(date)
            }
            val stepsForDateFlow = selectedDateState.flatMapLatest { date ->
                dailyActivityRepository.getStepsForDate(date)
            }
            val stepsEditorFlow = combine(stepsInputState, hasEditedStepsInput) { stepsInput, edited ->
                stepsInput to edited
            }

            combine(
                selectedDateState,
                daySummaryFlow,
                userPreferencesRepository.userPreferences,
                stepsForDateFlow,
                stepsEditorFlow
            ) { selectedDate, summary, prefs, stepsForDate, stepsEditor ->
                val (stepsInput, edited) = stepsEditor
                val estimatedActiveCalories = estimateActiveCalories(
                    steps = stepsForDate,
                    weightKg = prefs.currentWeightKg,
                    bodyFatPercent = prefs.bodyFatPercent
                )
                val effectiveStepsInput = if (edited) stepsInput else stepsForDate.toString()
                DashboardUiState(
                    selectedDate = selectedDate,
                    totalCalories = summary.totalCalories,
                    totalProtein = summary.totalProtein,
                    totalFat = summary.totalFat,
                    totalCarbs = summary.totalCarbs,
                    entriesByMeal = summary.entriesByMeal,
                    preferences = prefs,
                    totalSteps = stepsForDate,
                    activeCalories = estimatedActiveCalories,
                    stepsInput = effectiveStepsInput,
                    stepsSaved = _uiState.value.stepsSaved
                )
            }.collect { _uiState.value = it }
        }
    }

    fun selectDate(date: LocalDate) {
        if (selectedDateState.value == date) return
        selectedDateState.value = date
        hasEditedStepsInput.value = false
        stepsInputState.value = "0"
        _uiState.value = _uiState.value.copy(selectedDate = date, stepsSaved = false)
    }

    fun onStepsInputChange(value: String) {
        val sanitized = value.filter { it.isDigit() }
        stepsInputState.value = sanitized
        hasEditedStepsInput.value = true
        _uiState.value = _uiState.value.copy(stepsInput = sanitized, stepsSaved = false)
    }

    fun saveStepsForSelectedDate() {
        viewModelScope.launch {
            val parsed = _uiState.value.stepsInput.toLongOrNull() ?: 0L
            dailyActivityRepository.saveStepsForDate(selectedDateState.value, parsed)
            hasEditedStepsInput.value = false
            stepsInputState.value = parsed.toString()
            _uiState.value = _uiState.value.copy(stepsSaved = true)
        }
    }

    private fun estimateActiveCalories(steps: Long, weightKg: Double, bodyFatPercent: Double): Double {
        val safeWeightKg = weightKg.coerceIn(35.0, 250.0)
        val safeBodyFat = bodyFatPercent.coerceIn(3.0, 60.0)
        val leanMassKg = safeWeightKg * (1.0 - safeBodyFat / 100.0)
        val kcalPerStep = 0.015 + (0.00057 * leanMassKg)
        return (steps * kcalPerStep).coerceAtLeast(0.0)
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            diaryRepository.deleteEntry(entryId)
        }
    }

    fun updateEntryAmount(entryId: Long, amountInGrams: Double) {
        viewModelScope.launch {
            diaryRepository.updateEntryAmount(entryId, amountInGrams)
        }
    }

    class Factory(
        private val diaryRepository: DiaryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val dailyActivityRepository: DailyActivityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(diaryRepository, userPreferencesRepository, dailyActivityRepository) as T
        }
    }
}
