package de.ingomc.nozio.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.local.FoodItem
import de.ingomc.nozio.data.local.MealType
import de.ingomc.nozio.data.repository.DiaryRepository
import de.ingomc.nozio.data.repository.FoodRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SearchUiState(
    val query: String = "",
    val results: List<FoodItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFood: FoodItem? = null,
    val showBottomSheet: Boolean = false,
    val addedSuccessfully: Boolean = false
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(400)
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query, error = null, addedSuccessfully = false)
        _searchQuery.value = query
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            val results = foodRepository.searchFood(query)
            _uiState.value = _uiState.value.copy(results = results, isLoading = false)
        } catch (e: Exception) {
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
            addedSuccessfully = false
        )
    }

    fun dismissBottomSheet() {
        _uiState.value = _uiState.value.copy(showBottomSheet = false, selectedFood = null)
    }

    fun addFood(mealType: MealType, amountInGrams: Double) {
        val food = _uiState.value.selectedFood ?: return
        viewModelScope.launch {
            diaryRepository.addEntry(
                date = LocalDate.now(),
                mealType = mealType,
                foodItemId = food.id,
                amountInGrams = amountInGrams
            )
            _uiState.value = _uiState.value.copy(
                showBottomSheet = false,
                selectedFood = null,
                addedSuccessfully = true
            )
        }
    }

    class Factory(
        private val foodRepository: FoodRepository,
        private val diaryRepository: DiaryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(foodRepository, diaryRepository) as T
        }
    }
}

