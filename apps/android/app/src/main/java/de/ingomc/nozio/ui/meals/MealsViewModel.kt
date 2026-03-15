package de.ingomc.nozio.ui.meals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.local.RecipeAmountUnit
import de.ingomc.nozio.data.repository.FoodRepository
import de.ingomc.nozio.data.repository.MealTemplateDetail
import de.ingomc.nozio.data.repository.MealTemplateIngredientDetail
import de.ingomc.nozio.data.repository.MealTemplateRepository
import de.ingomc.nozio.data.repository.MealTemplateSummary
import de.ingomc.nozio.data.repository.SaveIngredientInput
import de.ingomc.nozio.data.repository.SaveMealTemplateInput
import de.ingomc.nozio.data.repository.TrackIngredientInput
import de.ingomc.nozio.data.repository.TrackMealTemplateInput
import de.ingomc.nozio.data.repository.TrackMealTemplateResult
import de.ingomc.nozio.data.repository.normalizeToGrams
import de.ingomc.nozio.ui.search.WidgetRefreshDelegate
import de.ingomc.nozio.widget.CalorieWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MealsUiState(
    val templates: List<MealTemplateSummary> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val showEditor: Boolean = false,
    val showTracker: Boolean = false,
    val showIngredientPicker: Boolean = false,
    val editorState: EditorState = EditorState(),
    val trackerState: TrackerState = TrackerState(),
    val ingredientSearchQuery: String = "",
    val ingredientSearchResults: List<FoodItem> = emptyList(),
    val ingredientSearchLoading: Boolean = false,
    val snackbarMessage: String? = null
)

data class EditorState(
    val templateId: Long = 0,
    val name: String = "",
    val defaultMealType: MealType = MealType.BREAKFAST,
    val ingredients: List<EditorIngredient> = emptyList()
)

data class EditorIngredient(
    val foodItemId: Long,
    val foodName: String,
    val amountValue: String = "100",
    val amountUnit: RecipeAmountUnit = RecipeAmountUnit.GRAM,
    val caloriesPer100g: Double = 0.0,
    val proteinPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val servingQuantity: Double? = null,
    val packageQuantity: Double? = null
)

data class TrackerState(
    val templateId: Long = 0,
    val templateName: String = "",
    val mealType: MealType = MealType.BREAKFAST,
    val ingredients: List<TrackerIngredient> = emptyList()
) {
    val totalCalories: Double get() = ingredients.sumOf { it.calories }
    val totalProtein: Double get() = ingredients.sumOf { it.protein }
    val totalFat: Double get() = ingredients.sumOf { it.fat }
    val totalCarbs: Double get() = ingredients.sumOf { it.carbs }
}

data class TrackerIngredient(
    val foodItemId: Long,
    val foodName: String,
    val amountValue: String,
    val amountUnit: RecipeAmountUnit,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val servingQuantity: Double?,
    val packageQuantity: Double?
) {
    private val parsedAmount: Double get() = amountValue.toDoubleOrNull() ?: 0.0
    private val grams: Double get() = normalizeToGrams(parsedAmount, amountUnit, servingQuantity, packageQuantity)
    val calories: Double get() = caloriesPer100g * grams / 100.0
    val protein: Double get() = proteinPer100g * grams / 100.0
    val fat: Double get() = fatPer100g * grams / 100.0
    val carbs: Double get() = carbsPer100g * grams / 100.0
}

class MealsViewModel(
    private val mealTemplateRepository: MealTemplateRepository,
    private val foodRepository: FoodRepository,
    private val widgetRefreshDelegate: WidgetRefreshDelegate
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealsUiState())
    val uiState: StateFlow<MealsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mealTemplateRepository.observeTemplates().collect { templates ->
                _uiState.update { it.copy(templates = templates) }
            }
        }
    }

    fun setSelectedDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // ── Editor ──────────────────────────────────────────────────────

    fun openNewEditor() {
        _uiState.update {
            it.copy(
                showEditor = true,
                editorState = EditorState()
            )
        }
    }

    fun openEditEditor(templateId: Long) {
        viewModelScope.launch {
            val detail = mealTemplateRepository.getTemplateDetail(templateId) ?: return@launch
            _uiState.update {
                it.copy(
                    showEditor = true,
                    editorState = EditorState(
                        templateId = detail.id,
                        name = detail.name,
                        defaultMealType = detail.defaultMealType,
                        ingredients = detail.ingredients.map { ing -> ing.toEditorIngredient() }
                    )
                )
            }
        }
    }

    fun closeEditor() {
        _uiState.update { it.copy(showEditor = false) }
    }

    fun updateEditorName(name: String) {
        _uiState.update { it.copy(editorState = it.editorState.copy(name = name)) }
    }

    fun updateEditorMealType(mealType: MealType) {
        _uiState.update { it.copy(editorState = it.editorState.copy(defaultMealType = mealType)) }
    }

    fun updateEditorIngredientAmount(index: Int, amount: String) {
        _uiState.update { state ->
            val ingredients = state.editorState.ingredients.toMutableList()
            if (index in ingredients.indices) {
                ingredients[index] = ingredients[index].copy(amountValue = amount)
            }
            state.copy(editorState = state.editorState.copy(ingredients = ingredients))
        }
    }

    fun updateEditorIngredientUnit(index: Int, unit: RecipeAmountUnit) {
        _uiState.update { state ->
            val ingredients = state.editorState.ingredients.toMutableList()
            if (index in ingredients.indices) {
                ingredients[index] = ingredients[index].copy(amountUnit = unit)
            }
            state.copy(editorState = state.editorState.copy(ingredients = ingredients))
        }
    }

    fun removeEditorIngredient(index: Int) {
        _uiState.update { state ->
            val ingredients = state.editorState.ingredients.toMutableList()
            if (index in ingredients.indices) {
                ingredients.removeAt(index)
            }
            state.copy(editorState = state.editorState.copy(ingredients = ingredients))
        }
    }

    fun saveTemplate() {
        val editor = _uiState.value.editorState
        if (editor.name.isBlank()) return

        viewModelScope.launch {
            runCatching {
                mealTemplateRepository.saveTemplate(
                    SaveMealTemplateInput(
                        id = editor.templateId,
                        name = editor.name,
                        defaultMealType = editor.defaultMealType,
                        ingredients = editor.ingredients.map { ing ->
                            SaveIngredientInput(
                                foodItemId = ing.foodItemId,
                                amountValue = ing.amountValue.toDoubleOrNull() ?: 0.0,
                                amountUnit = ing.amountUnit
                            )
                        }
                    )
                )
            }.onSuccess {
                _uiState.update { it.copy(showEditor = false, snackbarMessage = "Rezept gespeichert") }
            }
        }
    }

    fun deleteTemplate(templateId: Long) {
        viewModelScope.launch {
            mealTemplateRepository.deleteTemplate(templateId)
            _uiState.update { it.copy(snackbarMessage = "Rezept geloescht") }
        }
    }

    // ── Ingredient Picker ───────────────────────────────────────────

    fun openIngredientPicker() {
        _uiState.update {
            it.copy(
                showIngredientPicker = true,
                ingredientSearchQuery = "",
                ingredientSearchResults = emptyList()
            )
        }
    }

    fun closeIngredientPicker() {
        _uiState.update { it.copy(showIngredientPicker = false) }
    }

    fun updateIngredientSearchQuery(query: String) {
        _uiState.update { it.copy(ingredientSearchQuery = query) }
        if (query.length >= 2) {
            searchIngredients(query)
        } else {
            _uiState.update { it.copy(ingredientSearchResults = emptyList()) }
        }
    }

    private fun searchIngredients(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(ingredientSearchLoading = true) }
            runCatching {
                foodRepository.searchFood(query)
            }.onSuccess { results ->
                _uiState.update { it.copy(ingredientSearchResults = results, ingredientSearchLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(ingredientSearchLoading = false) }
            }
        }
    }

    fun selectIngredient(food: FoodItem, forTracker: Boolean = false) {
        viewModelScope.launch {
            val storedFood = foodRepository.ensureFoodStored(food)
            val ingredient = EditorIngredient(
                foodItemId = storedFood.id,
                foodName = storedFood.name,
                amountValue = "100",
                amountUnit = RecipeAmountUnit.GRAM,
                caloriesPer100g = storedFood.caloriesPer100g,
                proteinPer100g = storedFood.proteinPer100g,
                fatPer100g = storedFood.fatPer100g,
                carbsPer100g = storedFood.carbsPer100g,
                servingQuantity = storedFood.servingQuantity,
                packageQuantity = storedFood.packageQuantity
            )

            if (forTracker) {
                _uiState.update { state ->
                    val trackerIngredients = state.trackerState.ingredients.toMutableList()
                    trackerIngredients.add(ingredient.toTrackerIngredient())
                    state.copy(
                        showIngredientPicker = false,
                        trackerState = state.trackerState.copy(ingredients = trackerIngredients)
                    )
                }
            } else {
                _uiState.update { state ->
                    val editorIngredients = state.editorState.ingredients.toMutableList()
                    editorIngredients.add(ingredient)
                    state.copy(
                        showIngredientPicker = false,
                        editorState = state.editorState.copy(ingredients = editorIngredients)
                    )
                }
            }
        }
    }

    // ── Tracker ─────────────────────────────────────────────────────

    fun openTracker(templateId: Long) {
        viewModelScope.launch {
            val detail = mealTemplateRepository.getTemplateDetail(templateId) ?: return@launch
            _uiState.update {
                it.copy(
                    showTracker = true,
                    trackerState = TrackerState(
                        templateId = detail.id,
                        templateName = detail.name,
                        mealType = detail.defaultMealType,
                        ingredients = detail.ingredients.map { ing -> ing.toTrackerIngredient() }
                    )
                )
            }
        }
    }

    fun closeTracker() {
        _uiState.update { it.copy(showTracker = false) }
    }

    fun updateTrackerMealType(mealType: MealType) {
        _uiState.update { it.copy(trackerState = it.trackerState.copy(mealType = mealType)) }
    }

    fun updateTrackerIngredientAmount(index: Int, amount: String) {
        _uiState.update { state ->
            val ingredients = state.trackerState.ingredients.toMutableList()
            if (index in ingredients.indices) {
                ingredients[index] = ingredients[index].copy(amountValue = amount)
            }
            state.copy(trackerState = state.trackerState.copy(ingredients = ingredients))
        }
    }

    fun updateTrackerIngredientUnit(index: Int, unit: RecipeAmountUnit) {
        _uiState.update { state ->
            val ingredients = state.trackerState.ingredients.toMutableList()
            if (index in ingredients.indices) {
                ingredients[index] = ingredients[index].copy(amountUnit = unit)
            }
            state.copy(trackerState = state.trackerState.copy(ingredients = ingredients))
        }
    }

    fun removeTrackerIngredient(index: Int) {
        _uiState.update { state ->
            val ingredients = state.trackerState.ingredients.toMutableList()
            if (index in ingredients.indices) {
                ingredients.removeAt(index)
            }
            state.copy(trackerState = state.trackerState.copy(ingredients = ingredients))
        }
    }

    fun trackMeal() {
        val tracker = _uiState.value.trackerState
        val date = _uiState.value.selectedDate

        viewModelScope.launch {
            val result = mealTemplateRepository.trackTemplate(
                TrackMealTemplateInput(
                    date = date,
                    mealType = tracker.mealType,
                    ingredients = tracker.ingredients.map { ing ->
                        TrackIngredientInput(
                            foodItemId = ing.foodItemId,
                            amountValue = ing.amountValue.toDoubleOrNull() ?: 0.0,
                            amountUnit = ing.amountUnit,
                            servingQuantity = ing.servingQuantity,
                            packageQuantity = ing.packageQuantity
                        )
                    }
                )
            )
            when (result) {
                is TrackMealTemplateResult.Success -> {
                    widgetRefreshDelegate.refresh()
                    _uiState.update {
                        it.copy(
                            showTracker = false,
                            snackbarMessage = "${result.trackedCount} Zutaten getrackt"
                        )
                    }
                }
                is TrackMealTemplateResult.Error -> {
                    _uiState.update { it.copy(snackbarMessage = result.message) }
                }
            }
        }
    }

    class Factory(
        private val appContext: Context,
        private val mealTemplateRepository: MealTemplateRepository,
        private val foodRepository: FoodRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MealsViewModel(
                mealTemplateRepository = mealTemplateRepository,
                foodRepository = foodRepository,
                widgetRefreshDelegate = WidgetRefreshDelegate {
                    CalorieWidgetProvider.updateAll(appContext)
                }
            ) as T
        }
    }
}

private fun MealTemplateIngredientDetail.toEditorIngredient() = EditorIngredient(
    foodItemId = foodItemId,
    foodName = foodName,
    amountValue = defaultAmountValue.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() },
    amountUnit = amountUnit,
    caloriesPer100g = caloriesPer100g,
    proteinPer100g = proteinPer100g,
    fatPer100g = fatPer100g,
    carbsPer100g = carbsPer100g,
    servingQuantity = servingQuantity,
    packageQuantity = packageQuantity
)

private fun MealTemplateIngredientDetail.toTrackerIngredient() = TrackerIngredient(
    foodItemId = foodItemId,
    foodName = foodName,
    amountValue = defaultAmountValue.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() },
    amountUnit = amountUnit,
    caloriesPer100g = caloriesPer100g,
    proteinPer100g = proteinPer100g,
    fatPer100g = fatPer100g,
    carbsPer100g = carbsPer100g,
    servingQuantity = servingQuantity,
    packageQuantity = packageQuantity
)

private fun EditorIngredient.toTrackerIngredient() = TrackerIngredient(
    foodItemId = foodItemId,
    foodName = foodName,
    amountValue = amountValue,
    amountUnit = amountUnit,
    caloriesPer100g = caloriesPer100g,
    proteinPer100g = proteinPer100g,
    fatPer100g = fatPer100g,
    carbsPer100g = carbsPer100g,
    servingQuantity = servingQuantity,
    packageQuantity = packageQuantity
)
