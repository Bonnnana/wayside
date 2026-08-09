package com.wayside.ui

import androidx.lifecycle.ViewModel
import com.wayside.data.Units
import com.wayside.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WaysideUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val budget: Int = 15,
    val selectedPinId: String? = null,
    val savedIds: Set<String> = setOf("queluz", "peninha"),
    val stops: List<String> = emptyList(),
    val interests: Set<String> = setOf("Coffee", "Food", "Nature", "Viewpoints"),
    val savedFilter: String = "All",
    val units: Units = Units.Kilometres,
)

/** Everything is in memory — this app is UI only. */
class WaysideViewModel : ViewModel() {

    private val _state = MutableStateFlow(WaysideUiState())
    val state: StateFlow<WaysideUiState> = _state.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = _state.update { it.copy(themeMode = mode) }

    fun setBudget(minutes: Int) = _state.update { current ->
        // A pin that no longer fits the budget should not stay selected.
        val stillFits = current.selectedPinId?.let { id ->
            com.wayside.data.placeOrNull(id)?.let { it.detourMinutes <= minutes }
        } ?: true
        current.copy(
            budget = minutes,
            selectedPinId = if (stillFits) current.selectedPinId else null,
        )
    }

    fun selectPin(id: String?) = _state.update { it.copy(selectedPinId = id) }

    fun toggleSaved(id: String) = _state.update { current ->
        current.copy(
            savedIds = if (id in current.savedIds) current.savedIds - id else current.savedIds + id,
        )
    }

    fun toggleStop(id: String) = _state.update { current ->
        current.copy(
            stops = if (id in current.stops) current.stops - id else current.stops + id,
        )
    }

    fun removeStop(id: String) = _state.update { it.copy(stops = it.stops - id) }

    fun clearStops() = _state.update { it.copy(stops = emptyList()) }

    fun toggleInterest(interest: String) = _state.update { current ->
        current.copy(
            interests = if (interest in current.interests) {
                current.interests - interest
            } else {
                current.interests + interest
            },
        )
    }

    fun setSavedFilter(filter: String) = _state.update { it.copy(savedFilter = filter) }

    fun setUnits(units: Units) = _state.update { it.copy(units = units) }
}
