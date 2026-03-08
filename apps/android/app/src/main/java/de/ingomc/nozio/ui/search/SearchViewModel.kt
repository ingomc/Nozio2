package de.ingomc.nozio.ui.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.FoodSource
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.repository.CustomFoodInput
import de.ingomc.nozio.data.repository.DiaryRepository
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
import kotlin.math.roundToInt

enum class AddConfirmationSource {
    STANDARD,
    QUICK_ADD
}

data class AddConfirmationState(
    val bannerId: Long,
    val entryId: Long,
    val title: String,
    val mealType: MealType,
    val amountLabel: String?,
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int,
    val source: AddConfirmationSource
)

fun interface WidgetRefreshDelegate {
    suspend fun refresh()
}

data class SearchUiState(
    val query: String = "",
    val results: List<FoodItem> = emptyList(),
    val recentSuggestions: List<FoodItem> = emptyList(),
    val frequentSuggestions: List<FoodItem> = emptyList(),
    val favoriteSuggestions: List<FoodItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFood: FoodItem? = null,
    val showBottomSheet: Boolean = false,
    val showQuickAddSheet: Boolean = false,
    val showCreateCustomFoodSheet: Boolean = false,
    val activeAddConfirmation: AddConfirmationState? = null,
    val showBarcodeResultsSheet: Boolean = false,
    val isSubmittingCustomFood: Boolean = false
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository,
    private val widgetRefreshDelegate: WidgetRefreshDelegate
) : ViewModel() {
    private companion object {
        const val MIN_SEARCH_QUERY_LENGTH = 3
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private var nextBannerId = 1L

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
        refreshSuggestionLists()
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            error = null,
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
            showBarcodeResultsSheet = false
        )
    }

    fun dismissBottomSheet() {
        _uiState.value = _uiState.value.copy(showBottomSheet = false, selectedFood = null)
    }

    fun showQuickAddSheet() {
        _uiState.value = _uiState.value.copy(
            showQuickAddSheet = true,
            showCreateCustomFoodSheet = false
        )
    }

    fun dismissQuickAddSheet() {
        _uiState.value = _uiState.value.copy(showQuickAddSheet = false)
    }

    fun showCreateCustomFoodSheet() {
        _uiState.value = _uiState.value.copy(
            showCreateCustomFoodSheet = true,
            showQuickAddSheet = false
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

    fun addFood(mealType: MealType, amountInGrams: Double, amountLabel: String) {
        val food = _uiState.value.selectedFood ?: return
        viewModelScope.launch {
            val storedFood = foodRepository.ensureFoodStored(food)
            val entryId = diaryRepository.addEntry(
                date = selectedDate.value,
                mealType = mealType,
                foodItemId = storedFood.id,
                amountInGrams = amountInGrams
            )
            val suggestions = loadSuggestions()
            _searchQuery.value = ""
            _uiState.value = _uiState.value.copy(
                query = "",
                results = emptyList(),
                recentSuggestions = suggestions.recent,
                frequentSuggestions = suggestions.frequent,
                favoriteSuggestions = suggestions.favorites,
                showBottomSheet = false,
                selectedFood = null,
                activeAddConfirmation = storedFood.toAddConfirmation(
                    bannerId = nextBannerId++,
                    entryId = entryId,
                    mealType = mealType,
                    amountInGrams = amountInGrams,
                    amountLabel = amountLabel,
                    source = AddConfirmationSource.STANDARD
                )
            )
            widgetRefreshDelegate.refresh()
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
            val entryId = diaryRepository.addEntry(
                date = selectedDate.value,
                mealType = mealType,
                foodItemId = storedFood.id,
                amountInGrams = 100.0
            )
            val suggestions = loadSuggestions()
            _searchQuery.value = ""
            _uiState.value = _uiState.value.copy(
                query = "",
                results = emptyList(),
                recentSuggestions = suggestions.recent,
                frequentSuggestions = suggestions.frequent,
                favoriteSuggestions = suggestions.favorites,
                showQuickAddSheet = false,
                activeAddConfirmation = AddConfirmationState(
                    bannerId = nextBannerId++,
                    entryId = entryId,
                    title = storedFood.name,
                    mealType = mealType,
                    amountLabel = null,
                    calories = calories.roundToInt(),
                    protein = protein.roundToInt(),
                    fat = fat.roundToInt(),
                    carbs = carbs.roundToInt(),
                    source = AddConfirmationSource.QUICK_ADD
                )
            )
            widgetRefreshDelegate.refresh()
        }
    }

    fun createCustomFood(input: CustomFoodInput) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSubmittingCustomFood = true,
                error = null
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

    fun dismissAddConfirmation() {
        if (_uiState.value.activeAddConfirmation != null) {
            _uiState.value = _uiState.value.copy(activeAddConfirmation = null)
        }
    }

    fun undoLastAddedFood() {
        val confirmation = _uiState.value.activeAddConfirmation ?: return
        viewModelScope.launch {
            diaryRepository.deleteEntry(confirmation.entryId)
            val suggestions = loadSuggestions()
            _uiState.value = _uiState.value.copy(
                recentSuggestions = suggestions.recent,
                frequentSuggestions = suggestions.frequent,
                favoriteSuggestions = suggestions.favorites,
                activeAddConfirmation = null
            )
            widgetRefreshDelegate.refresh()
        }
    }

    fun toggleSelectedFoodFavorite() {
        val selectedFood = _uiState.value.selectedFood ?: return
        viewModelScope.launch {
            val storedFood = foodRepository.ensureFoodStored(selectedFood)
            val updatedFood = foodRepository.setFavorite(
                foodId = storedFood.id,
                isFavorite = !storedFood.isFavorite
            ) ?: storedFood.copy(isFavorite = !storedFood.isFavorite)
            val suggestions = loadSuggestions()
            _uiState.value = _uiState.value.copy(
                selectedFood = updatedFood,
                recentSuggestions = suggestions.recent,
                frequentSuggestions = suggestions.frequent,
                favoriteSuggestions = suggestions.favorites
            )
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

    private fun refreshSuggestionLists() {
        viewModelScope.launch {
            val suggestions = loadSuggestions()
            _uiState.value = _uiState.value.copy(
                recentSuggestions = suggestions.recent,
                frequentSuggestions = suggestions.frequent,
                favoriteSuggestions = suggestions.favorites
            )
        }
    }

    private suspend fun loadSuggestions(): SuggestionLists {
        return SuggestionLists(
            recent = diaryRepository.getRecentlyAddedFoods(),
            frequent = diaryRepository.getFrequentlyAddedFoods(),
            favorites = foodRepository.getFavoriteFoods()
        )
    }

    private data class SuggestionLists(
        val recent: List<FoodItem>,
        val frequent: List<FoodItem>,
        val favorites: List<FoodItem>
    )

    class Factory(
        private val appContext: Context,
        private val foodRepository: FoodRepository,
        private val diaryRepository: DiaryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(
                foodRepository = foodRepository,
                diaryRepository = diaryRepository,
                widgetRefreshDelegate = WidgetRefreshDelegate {
                    CalorieWidgetProvider.updateAll(appContext)
                }
            ) as T
        }
    }
}

private fun FoodItem.toAddConfirmation(
    bannerId: Long,
    entryId: Long,
    mealType: MealType,
    amountInGrams: Double,
    amountLabel: String,
    source: AddConfirmationSource
): AddConfirmationState {
    val multiplier = amountInGrams / 100.0
    return AddConfirmationState(
        bannerId = bannerId,
        entryId = entryId,
        title = name,
        mealType = mealType,
        amountLabel = amountLabel,
        calories = (caloriesPer100g * multiplier).roundToInt(),
        protein = (proteinPer100g * multiplier).roundToInt(),
        fat = (fatPer100g * multiplier).roundToInt(),
        carbs = (carbsPer100g * multiplier).roundToInt(),
        source = source
    )
}
