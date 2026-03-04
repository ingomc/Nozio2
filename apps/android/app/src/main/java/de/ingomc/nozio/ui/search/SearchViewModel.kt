package de.ingomc.nozio.ui.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.repository.DiaryRepository
import de.ingomc.nozio.data.repository.CustomFoodInput
import de.ingomc.nozio.data.repository.FoodRepository
import de.ingomc.nozio.widget.CalorieWidgetProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SearchUiState(
    val query: String = "",
    val results: List<FoodItem> = emptyList(),
    val recentSuggestions: List<FoodItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFood: FoodItem? = null,
    val showBottomSheet: Boolean = false,
    val showQuickAddSheet: Boolean = false,
    val showCreateCustomFoodSheet: Boolean = false,
    val addedSuccessfully: Boolean = false,
    val showBarcodeResultsSheet: Boolean = false,
    val isSubmittingCustomFood: Boolean = false
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val appContext: Context,
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository
) : ViewModel() {
    private companion object {
        const val MIN_SEARCH_QUERY_LENGTH = 3
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val selectedDate = MutableStateFlow(LocalDate.now())

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(400)
                .distinctUntilChanged()
                .filter { it.length >= MIN_SEARCH_QUERY_LENGTH }
                .collectLatest { query ->
                    performSearch(query)
                }
        }
        loadRecentlyAddedFoods()
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            error = null,
            addedSuccessfully = false,
            showBarcodeResultsSheet = false
        )
        _searchQuery.value = query
        if (query.length < MIN_SEARCH_QUERY_LENGTH) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            val results = foodRepository.searchFood(query)
            _uiState.value = _uiState.value.copy(results = results, isLoading = false)
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Suche fehlgeschlagen. Bitte prüfe deine Internetverbindung.",
                isLoading = false
            )
        }
    }

    fun selectFood(food: FoodItem) {
        _uiState.value = _uiState.value.copy(
            selectedFood = food,
            showBottomSheet = true,
            addedSuccessfully = false,
            showBarcodeResultsSheet = false
        )
    }

    fun dismissBottomSheet() {
        _uiState.value = _uiState.value.copy(showBottomSheet = false, selectedFood = null)
    }

    fun showQuickAddSheet() {
        _uiState.value = _uiState.value.copy(
            showQuickAddSheet = true,
            showCreateCustomFoodSheet = false,
            addedSuccessfully = false
        )
    }

    fun dismissQuickAddSheet() {
        _uiState.value = _uiState.value.copy(showQuickAddSheet = false)
    }

    fun showCreateCustomFoodSheet() {
        _uiState.value = _uiState.value.copy(
            showCreateCustomFoodSheet = true,
            showQuickAddSheet = false,
            addedSuccessfully = false
        )
    }

    fun dismissCreateCustomFoodSheet() {
        _uiState.value = _uiState.value.copy(showCreateCustomFoodSheet = false)
    }

    fun dismissBarcodeResultsSheet() {
        _uiState.value = _uiState.value.copy(showBarcodeResultsSheet = false)
    }

    fun setSelectedDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun addFood(mealType: MealType, amountInGrams: Double) {
        val food = _uiState.value.selectedFood ?: return
        viewModelScope.launch {
            val storedFood = foodRepository.ensureFoodStored(food)
            diaryRepository.addEntry(
                date = selectedDate.value,
                mealType = mealType,
                foodItemId = storedFood.id,
                amountInGrams = amountInGrams
            )
            val recentSuggestions = diaryRepository.getRecentlyAddedFoods()
            _searchQuery.value = ""
            _uiState.value = _uiState.value.copy(
                query = "",
                results = emptyList(),
                recentSuggestions = recentSuggestions,
                showBottomSheet = false,
                selectedFood = null,
                addedSuccessfully = true
            )
            CalorieWidgetProvider.updateAll(appContext)
        }
    }

    fun addQuickEntry(
        mealType: MealType,
        calories: Double,
        protein: Double,
        fat: Double,
        carbs: Double,
        name: String?
    ) {
        viewModelScope.launch {
            val quickFood = FoodItem(
                name = name?.trim()?.ifBlank { null } ?: "Quick Add",
                caloriesPer100g = calories,
                proteinPer100g = protein,
                fatPer100g = fat,
                carbsPer100g = carbs,
                source = FoodSource.CUSTOM
            )
            val storedFood = foodRepository.ensureFoodStored(quickFood)
            diaryRepository.addEntry(
                date = selectedDate.value,
                mealType = mealType,
                foodItemId = storedFood.id,
                amountInGrams = 100.0
            )
            val recentSuggestions = diaryRepository.getRecentlyAddedFoods()
            _searchQuery.value = ""
            _uiState.value = _uiState.value.copy(
                query = "",
                results = emptyList(),
                recentSuggestions = recentSuggestions,
                showQuickAddSheet = false,
                addedSuccessfully = true
            )
            CalorieWidgetProvider.updateAll(appContext)
        }
    }

    fun createCustomFood(input: CustomFoodInput) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSubmittingCustomFood = true,
                error = null,
                addedSuccessfully = false
            )
            try {
                val createdFood = foodRepository.createCustomFood(input)
                _uiState.value = _uiState.value.copy(
                    isSubmittingCustomFood = false,
                    showCreateCustomFoodSheet = false,
                    selectedFood = createdFood,
                    showBottomSheet = true
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingCustomFood = false,
                    error = "Eigenes Produkt konnte nicht gespeichert werden."
                )
            }
        }
    }

    fun onAddedMessageShown() {
        if (_uiState.value.addedSuccessfully) {
            _uiState.value = _uiState.value.copy(addedSuccessfully = false)
        }
    }

    fun onBarcodeScanned(barcode: String) {
        val normalizedBarcode = barcode.filter(Char::isDigit)
        if (normalizedBarcode.isBlank()) return

        _searchQuery.value = ""
        _uiState.value = _uiState.value.copy(
            query = normalizedBarcode,
            isLoading = true,
            error = null,
            addedSuccessfully = false,
            showBarcodeResultsSheet = false
        )

        viewModelScope.launch {
            val food = foodRepository.getFoodByBarcode(normalizedBarcode)
            val results = food?.let { listOf(it) } ?: emptyList()
            _uiState.value = _uiState.value.copy(
                results = results,
                isLoading = false,
                error = if (food == null) "Kein Produkt für Barcode $normalizedBarcode gefunden." else null,
                showBarcodeResultsSheet = results.isNotEmpty()
            )
        }
    }

    private fun loadRecentlyAddedFoods() {
        viewModelScope.launch {
            val recentSuggestions = diaryRepository.getRecentlyAddedFoods()
            _uiState.value = _uiState.value.copy(recentSuggestions = recentSuggestions)
        }
    }

    class Factory(
        private val appContext: Context,
        private val foodRepository: FoodRepository,
        private val diaryRepository: DiaryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(appContext, foodRepository, diaryRepository) as T
        }
    }
}
