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
import de.ingomc.nozio.data.repository.LatestBodyFatEntry
import de.ingomc.nozio.data.repository.LatestWeightEntry
import de.ingomc.nozio.data.repository.UserPreferences
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import de.ingomc.nozio.widget.CalorieWidgetProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val bodyFatForDate: Double? = null,
    val displayWeightKg: Double? = null,
    val displayWeightDate: LocalDate? = null,
    val isWeightFallback: Boolean = false,
    val displayBodyFatPercent: Double? = null,
    val displayBodyFatDate: LocalDate? = null,
    val isBodyFatFallback: Boolean = false,
    val weightSaved: Boolean = false
)

internal data class ResolvedMetric(
    val value: Double?,
    val date: LocalDate?,
    val isFallback: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
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
        val bodyFatForDate: Double?,
        val latestWeightUpToDate: LatestWeightEntry?,
        val latestBodyFatUpToDate: LatestBodyFatEntry?
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
            val bodyFatForDateFlow = selectedDateState.flatMapLatest { date ->
                dailyActivityRepository.getBodyFatForDate(date)
            }
            val latestWeightUpToDateFlow = selectedDateState.flatMapLatest { date ->
                dailyActivityRepository.getLatestWeightEntryUpTo(date)
            }
            val latestBodyFatUpToDateFlow = selectedDateState.flatMapLatest { date ->
                dailyActivityRepository.getLatestBodyFatEntryUpTo(date)
            }

            val dayAndPrefsFlow = combine(
                selectedDateState,
                daySummaryFlow,
                userPreferencesRepository.userPreferences
            ) { selectedDate, summary, prefs ->
                Triple(selectedDate, summary, prefs)
            }
            val activityForDateFlow = combine(
                stepsForDateFlow,
                weightForDateFlow,
                bodyFatForDateFlow
            ) { stepsForDate, weightForDate, bodyFatForDate ->
                Triple(stepsForDate, weightForDate, bodyFatForDate)
            }
            val latestUpToDateFlow = combine(
                latestWeightUpToDateFlow,
                latestBodyFatUpToDateFlow
            ) { latestWeightUpToDate, latestBodyFatUpToDate ->
                latestWeightUpToDate to latestBodyFatUpToDate
            }

            combine(dayAndPrefsFlow, activityForDateFlow, latestUpToDateFlow) { dayAndPrefs, activityForDate, latestUpToDate ->
                val (selectedDate, summary, prefs) = dayAndPrefs
                val (stepsForDate, weightForDate, bodyFatForDate) = activityForDate
                val (latestWeightUpToDate, latestBodyFatUpToDate) = latestUpToDate
                DashboardBaseData(
                    selectedDate = selectedDate,
                    summary = summary,
                    prefs = prefs,
                    stepsForDate = stepsForDate,
                    weightForDate = weightForDate,
                    bodyFatForDate = bodyFatForDate,
                    latestWeightUpToDate = latestWeightUpToDate,
                    latestBodyFatUpToDate = latestBodyFatUpToDate
                )
            }.collect { base ->
                val resolvedWeight = resolveMetric(
                    selectedDate = base.selectedDate,
                    valueForDate = base.weightForDate,
                    fallbackEntry = base.latestWeightUpToDate?.let { it.weightKg to it.date }
                )
                val resolvedBodyFat = resolveMetric(
                    selectedDate = base.selectedDate,
                    valueForDate = base.bodyFatForDate,
                    fallbackEntry = base.latestBodyFatUpToDate?.let { it.bodyFatPercent to it.date }
                )

                val effectiveWeightKg = resolvedWeight.value ?: base.prefs.currentWeightKg
                val effectiveBodyFatPercent = resolvedBodyFat.value ?: base.prefs.bodyFatPercent

                val estimatedActiveCalories = estimateActiveCalories(
                    steps = base.stepsForDate,
                    weightKg = effectiveWeightKg,
                    bodyFatPercent = effectiveBodyFatPercent
                )

                _uiState.value = DashboardUiState(
                    selectedDate = base.selectedDate,
                    totalCalories = base.summary.totalCalories,
                    totalProtein = base.summary.totalProtein,
                    totalFat = base.summary.totalFat,
                    totalCarbs = base.summary.totalCarbs,
                    entriesByMeal = base.summary.entriesByMeal,
                    preferences = base.prefs,
                    totalSteps = base.stepsForDate,
                    activeCalories = estimatedActiveCalories,
                    stepsSaved = _uiState.value.stepsSaved,
                    weightForDate = base.weightForDate,
                    bodyFatForDate = base.bodyFatForDate,
                    displayWeightKg = resolvedWeight.value,
                    displayWeightDate = resolvedWeight.date,
                    isWeightFallback = resolvedWeight.isFallback,
                    displayBodyFatPercent = resolvedBodyFat.value,
                    displayBodyFatDate = resolvedBodyFat.date,
                    isBodyFatFallback = resolvedBodyFat.isFallback,
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

    fun saveWeightAndBodyFatForSelectedDate(weightKg: Double, bodyFatPercent: Double?) {
        viewModelScope.launch {
            dailyActivityRepository.saveWeightAndBodyFatForDate(
                date = selectedDateState.value,
                weightKg = weightKg,
                bodyFatPercent = bodyFatPercent
            )
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

internal fun resolveMetric(
    selectedDate: LocalDate,
    valueForDate: Double?,
    fallbackEntry: Pair<Double, LocalDate>?
): ResolvedMetric {
    if (valueForDate != null) {
        return ResolvedMetric(
            value = valueForDate,
            date = selectedDate,
            isFallback = false
        )
    }
    val fallbackValue = fallbackEntry?.first
    val fallbackDate = fallbackEntry?.second
    if (fallbackValue != null && fallbackDate != null) {
        return ResolvedMetric(
            value = fallbackValue,
            date = fallbackDate,
            isFallback = fallbackDate != selectedDate
        )
    }
    return ResolvedMetric(value = null, date = null, isFallback = false)
}
