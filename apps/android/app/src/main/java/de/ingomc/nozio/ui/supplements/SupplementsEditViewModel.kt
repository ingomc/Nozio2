package de.ingomc.nozio.ui.supplements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.ingomc.nozio.data.local.SupplementAmountUnit
import de.ingomc.nozio.data.local.SupplementDayPart
import de.ingomc.nozio.data.repository.SupplementPlanItem
import de.ingomc.nozio.data.repository.SupplementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SupplementsEditUiState(
    val items: List<SupplementPlanItem> = emptyList(),
    val errorMessage: String? = null
)

class SupplementsEditViewModel(
    private val supplementRepository: SupplementRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplementsEditUiState())
    val uiState: StateFlow<SupplementsEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            supplementRepository.observePlanItems().collect { items ->
                _uiState.value = _uiState.value.copy(items = items)
            }
        }
    }

    fun savePlanItem(
        id: Long,
        name: String,
        dayPart: SupplementDayPart,
        scheduledMinutesOfDay: Int,
        amountValue: Double,
        amountUnit: SupplementAmountUnit
    ) {
        viewModelScope.launch {
            runCatching {
                supplementRepository.upsertPlanItem(
                    id = id,
                    name = name,
                    dayPart = dayPart,
                    scheduledMinutesOfDay = scheduledMinutesOfDay,
                    amountValue = amountValue,
                    amountUnit = amountUnit
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = "Supplement konnte nicht gespeichert werden.")
            }
        }
    }

    fun deletePlanItem(id: Long) {
        viewModelScope.launch {
            supplementRepository.deletePlanItem(id)
        }
    }

    fun clearError() {
        if (_uiState.value.errorMessage == null) return
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    class Factory(
        private val supplementRepository: SupplementRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SupplementsEditViewModel(
                supplementRepository = supplementRepository
            ) as T
        }
    }
}
