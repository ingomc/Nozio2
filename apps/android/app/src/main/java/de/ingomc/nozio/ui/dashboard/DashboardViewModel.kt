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
import de.ingomc.nozio.data.repository.SupplementPlanItem
import de.ingomc.nozio.data.repository.SupplementRepository
import de.ingomc.nozio.data.repository.SupplementTimelineItem
import de.ingomc.nozio.data.repository.UserPreferences
import de.ingomc.nozio.data.repository.UserPreferencesRepository
import de.ingomc.nozio.widget.CalorieWidgetProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

private const val ACTIVE_CALORIE_FACTOR = 0.8

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
    val supplementTimelineItems: List<SupplementTimelineItem> = emptyList(),
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
    private val dailyActivityRepository: DailyActivityRepository,
    private val supplementRepository: SupplementRepository
) : ViewModel() {
    private data class DashboardBaseData(
        val selectedDate: LocalDate,
        val summary: DaySummary,
        val prefs: UserPreferences,
        val stepsForDate: Long,
        val weightForDate: Double?,
        val bodyFatForDate: Double?,
        val latestWeightUpToDate: LatestWeightEntry?,
        val latestBodyFatUpToDate: LatestBodyFatEntry?,
        val supplementPlanItems: List<SupplementPlanItem>,
        val takenSupplementIds: Set<Long>
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
            val takenSupplementIdsFlow = selectedDateState.flatMapLatest { date ->
                supplementRepository.observeTakenSupplementIds(date)
            }
            val supplementPlanFlow = supplementRepository.observePlanItems()

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
            val supplementFlow = combine(
                supplementPlanFlow,
                takenSupplementIdsFlow
            ) { planItems, takenIds ->
                planItems to takenIds
            }

            combine(dayAndPrefsFlow, activityForDateFlow, latestUpToDateFlow, supplementFlow) { dayAndPrefs, activityForDate, latestUpToDate, supplements ->
                val (selectedDate, summary, prefs) = dayAndPrefs
                val (stepsForDate, weightForDate, bodyFatForDate) = activityForDate
                val (latestWeightUpToDate, latestBodyFatUpToDate) = latestUpToDate
                val (supplementPlanItems, takenSupplementIds) = supplements
                DashboardBaseData(
                    selectedDate = selectedDate,
                    summary = summary,
                    prefs = prefs,
                    stepsForDate = stepsForDate,
                    weightForDate = weightForDate,
                    bodyFatForDate = bodyFatForDate,
                    latestWeightUpToDate = latestWeightUpToDate,
                    latestBodyFatUpToDate = latestBodyFatUpToDate,
                    supplementPlanItems = supplementPlanItems,
                    takenSupplementIds = takenSupplementIds
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
                    supplementTimelineItems = buildSupplementTimelineItems(
                        planItems = base.supplementPlanItems,
                        takenIds = base.takenSupplementIds
                    ),
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

    fun saveWeightAndBodyFatForSelectedDate(weightKg: Double?, bodyFatPercent: Double?) {
        viewModelScope.launch {
            if (weightKg == null && bodyFatPercent == null) return@launch

            val selectedDate = selectedDateState.value
            if (weightKg != null && bodyFatPercent != null) {
                dailyActivityRepository.saveWeightAndBodyFatForDate(
                    date = selectedDate,
                    weightKg = weightKg,
                    bodyFatPercent = bodyFatPercent
                )
            } else if (weightKg != null) {
                dailyActivityRepository.saveWeightForDate(
                    date = selectedDate,
                    weightKg = weightKg
                )
            } else if (bodyFatPercent != null) {
                dailyActivityRepository.saveBodyFatForDate(
                    date = selectedDate,
                    bodyFatPercent = bodyFatPercent
                )
            }

            CalorieWidgetProvider.updateAll(appContext)
            _uiState.value = _uiState.value.copy(weightSaved = true)
        }
    }

    fun setIncludeActivityCaloriesInBudget(enabled: Boolean) {
        viewModelScope.launch {
            val currentPrefs = userPreferencesRepository.userPreferences.firstOrNull() ?: return@launch
            if (currentPrefs.includeActivityCaloriesInBudget == enabled) return@launch
            userPreferencesRepository.updatePreferences(
                currentPrefs.copy(includeActivityCaloriesInBudget = enabled)
            )
            CalorieWidgetProvider.updateAll(appContext)
        }
    }

    fun toggleSupplementTaken(supplementId: Long, taken: Boolean) {
        viewModelScope.launch {
            supplementRepository.setTaken(selectedDateState.value, supplementId, taken)
        }
    }

    private fun estimateActiveCalories(steps: Long, weightKg: Double, bodyFatPercent: Double): Double {
        val safeWeightKg = weightKg.coerceIn(35.0, 250.0)
        val safeBodyFat = bodyFatPercent.coerceIn(3.0, 60.0)
        val leanMassKg = safeWeightKg * (1.0 - safeBodyFat / 100.0)
        val kcalPerStep = (0.015 + (0.00057 * leanMassKg)) * ACTIVE_CALORIE_FACTOR
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

    fun copyEntry(
        foodItemId: Long,
        date: LocalDate,
        mealType: MealType,
        amountInGrams: Double
    ) {
        viewModelScope.launch {
            diaryRepository.addEntry(
                date = date,
                mealType = mealType,
                foodItemId = foodItemId,
                amountInGrams = amountInGrams
            )
            CalorieWidgetProvider.updateAll(appContext)
        }
    }

    fun moveEntry(entryId: Long, date: LocalDate, mealType: MealType) {
        viewModelScope.launch {
            diaryRepository.moveEntry(entryId, date, mealType)
            CalorieWidgetProvider.updateAll(appContext)
        }
    }

    class Factory(
        private val appContext: Context,
        private val diaryRepository: DiaryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val dailyActivityRepository: DailyActivityRepository,
        private val supplementRepository: SupplementRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                appContext,
                diaryRepository,
                userPreferencesRepository,
                dailyActivityRepository,
                supplementRepository
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

internal fun buildSupplementTimelineItems(
    planItems: List<SupplementPlanItem>,
    takenIds: Set<Long>
): List<SupplementTimelineItem> {
    return planItems.sortedWith(
        compareBy<SupplementPlanItem> { it.scheduledMinutesOfDay }
            .thenBy { it.dayPart.sortOrder }
            .thenBy { it.id }
    ).map { item ->
        SupplementTimelineItem(
            id = item.id,
            name = item.name,
            dayPart = item.dayPart,
            scheduledMinutesOfDay = item.scheduledMinutesOfDay,
            amountValue = item.amountValue,
            amountUnit = item.amountUnit,
            isTaken = takenIds.contains(item.id)
        )
    }
}

internal fun resolveSupplementTimelineInitialIndex(
    selectedDate: LocalDate,
    items: List<SupplementTimelineItem>,
    now: LocalTime = LocalTime.now()
): Int {
    if (items.isEmpty()) return 0
    if (selectedDate != LocalDate.now()) return 0
    val nowMinutes = (now.hour * 60) + now.minute
    return items.indices.minByOrNull { index ->
        abs(items[index].scheduledMinutesOfDay - nowMinutes)
    } ?: 0
}
