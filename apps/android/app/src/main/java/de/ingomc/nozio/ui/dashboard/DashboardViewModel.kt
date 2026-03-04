package de.ingomc.nozio.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.local.DiaryEntryWithFood
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.repository.DailyActivityRepository
import de.ingomc.nozio.data.repository.DaySummary
import de.ingomc.nozio.data.repository.DiaryRepository
import de.ingomc.nozio.data.repository.LatestWeightEntry
import de.ingomc.nozio.data.repository.UserPreferences
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import de.ingomc.nozio.widget.CalorieWidgetProvider
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
    val stepsSaved: Boolean = false,
    val weightForDate: Double? = null,
    val latestWeightEntry: LatestWeightEntry? = null,
    val weightSaved: Boolean = false
)

class DashboardViewModel(
    private val appContext: Context,
    private val diaryRepository: DiaryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dailyActivityRepository: DailyActivityRepository
) : ViewModel() {
    private data class DashboardBaseData(
        val selectedDate: LocalDate,
        val summary: DaySummary,
        val prefs: UserPreferences,
        val stepsForDate: Long,
        val weightForDate: Double?,
        val latestWeightEntry: LatestWeightEntry?
    )

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val selectedDateState = MutableStateFlow(LocalDate.now())

    init {
        viewModelScope.launch {
            val daySummaryFlow = selectedDateState.flatMapLatest { date ->
                diaryRepository.getDaySummary(date)
            }
            val stepsForDateFlow = selectedDateState.flatMapLatest { date ->
                dailyActivityRepository.getStepsForDate(date)
            }
            val weightForDateFlow = selectedDateState.flatMapLatest { date ->
                dailyActivityRepository.getWeightForDate(date)
            }
            val latestWeightEntryFlow = dailyActivityRepository.getLatestWeightEntry()

            val dayAndPrefsFlow = combine(
                selectedDateState,
                daySummaryFlow,
                userPreferencesRepository.userPreferences
            ) { selectedDate, summary, prefs ->
                Triple(selectedDate, summary, prefs)
            }
            val activityAndWeightFlow = combine(
                stepsForDateFlow,
                weightForDateFlow,
                latestWeightEntryFlow
            ) { stepsForDate, weightForDate, latestWeightEntry ->
                Triple(stepsForDate, weightForDate, latestWeightEntry)
            }
            val baseFlow = combine(dayAndPrefsFlow, activityAndWeightFlow) { dayAndPrefs, activityAndWeight ->
                val (selectedDate, summary, prefs) = dayAndPrefs
                val (stepsForDate, weightForDate, latestWeightEntry) = activityAndWeight
                DashboardBaseData(
                    selectedDate = selectedDate,
                    summary = summary,
                    prefs = prefs,
                    stepsForDate = stepsForDate,
                    weightForDate = weightForDate,
                    latestWeightEntry = latestWeightEntry
                )
            }

            baseFlow.collect { base ->
                val selectedDate = base.selectedDate
                val summary = base.summary
                val prefs = base.prefs
                val stepsForDate = base.stepsForDate
                val weightForDate = base.weightForDate
                val latestWeightEntry = base.latestWeightEntry
                val estimatedActiveCalories = estimateActiveCalories(
                    steps = stepsForDate,
                    weightKg = prefs.currentWeightKg,
                    bodyFatPercent = prefs.bodyFatPercent
                )
                _uiState.value = DashboardUiState(
                    selectedDate = selectedDate,
                    totalCalories = summary.totalCalories,
                    totalProtein = summary.totalProtein,
                    totalFat = summary.totalFat,
                    totalCarbs = summary.totalCarbs,
                    entriesByMeal = summary.entriesByMeal,
                    preferences = prefs,
                    totalSteps = stepsForDate,
                    activeCalories = estimatedActiveCalories,
                    stepsSaved = _uiState.value.stepsSaved,
                    weightForDate = weightForDate,
                    latestWeightEntry = latestWeightEntry,
                    weightSaved = _uiState.value.weightSaved
                )
            }
        }
    }

    fun selectDate(date: LocalDate) {
        if (selectedDateState.value == date) return
        selectedDateState.value = date
        _uiState.value = _uiState.value.copy(selectedDate = date, stepsSaved = false, weightSaved = false)
    }

    fun saveStepsForSelectedDate(steps: Long) {
        viewModelScope.launch {
            dailyActivityRepository.saveStepsForDate(selectedDateState.value, steps)
            CalorieWidgetProvider.updateAll(appContext)
            _uiState.value = _uiState.value.copy(stepsSaved = true)
        }
    }

    fun saveWeightForSelectedDate(weightKg: Double) {
        viewModelScope.launch {
            dailyActivityRepository.saveWeightForDate(selectedDateState.value, weightKg)
            CalorieWidgetProvider.updateAll(appContext)
            _uiState.value = _uiState.value.copy(weightSaved = true)
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
            CalorieWidgetProvider.updateAll(appContext)
        }
    }

    fun updateEntryAmount(entryId: Long, amountInGrams: Double) {
        viewModelScope.launch {
            diaryRepository.updateEntryAmount(entryId, amountInGrams)
            CalorieWidgetProvider.updateAll(appContext)
        }
    }

    class Factory(
        private val appContext: Context,
        private val diaryRepository: DiaryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val dailyActivityRepository: DailyActivityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                appContext,
                diaryRepository,
                userPreferencesRepository,
                dailyActivityRepository
            ) as T
        }
    }
}
