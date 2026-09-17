package com.wayside.repositories

import android.util.Log
import com.wayside.data.Place
import com.wayside.data.Route
import com.wayside.data.Units
import com.wayside.data.repositories.SavedPlaceRepository
import com.wayside.managers.interfaces.LocationManager
import com.wayside.models.ui.GeoPoint
import com.wayside.ui.WaysideUiState
import com.wayside.ui.hasRoute
import com.wayside.ui.placeOrNull
import com.wayside.ui.stopsInOrder
import com.wayside.ui.theme.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * App-wide UI state — theme, detour budget, saved places, chosen stops, interests.
 *
 * A singleton rather than a ViewModel because every screen reads the same instance: the theme
 * picker in Profile has to repaint Home, and a place saved on the detail screen has to show as
 * saved in the list behind it. A per-screen ViewModel would give each its own copy.
 */
@Singleton
class AppStateRepository @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val savedPlaceRepository: SavedPlaceRepository,
    private val locationManager: LocationManager,
) {

    private val _state = MutableStateFlow(WaysideUiState())
    val state: StateFlow<WaysideUiState> = _state.asStateFlow()

    /**
     * Survives every screen, so the fetch isn't tied to whichever one happened to be visible
     * when it started. A change of budget cancels the load it supersedes.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loadJob: Job? = null
    private var stopsJob: Job? = null

    init {

        // Saved places are owned by the database, not by this class: the flow is the single
        // source of truth, so a write and the state update can't drift apart.
        savedPlaceRepository.observeSaved()
            .onEach { saved ->
                _state.update { current ->
                    current.copy(
                        savedIds = saved.map { it.id }.toSet(),
                        savedCache = saved.associateBy { it.id },
                    )
                }
            }
            .launchIn(scope)

        locationManager.currentLocation
            .onEach { point -> _state.update { it.copy(currentLocation = point) } }
            .launchIn(scope)

        onLocationPermissionChanged(locationManager.hasPermission)
    }

    /** Called once the permission dialog has been answered, either way. */
    fun onLocationPermissionChanged(granted: Boolean) {
        Log.i("WaysideLocation", "Permission granted: $granted")
        _state.update { it.copy(hasLocationPermission = granted) }
        if (granted) locationManager.start() else locationManager.stop()
    }

    /**
     * Recomputes the drawn route so it runs start → stop → stop → destination.
     *
     * Called on every change to the stops, because the line is the plan: a stop that isn't on
     * the drawn route is just a pin the driver has to remember by themselves.
     */
    private fun refreshStopsRoute() {
        stopsJob?.cancel()

        val current = _state.value
        if (current.stops.isEmpty()) {
            _state.update { it.copy(stopsLeg = null, isRoutingStops = false) }
            return
        }

        stopsJob = scope.launch {
            _state.update { it.copy(isRoutingStops = true) }

            val ordered = current.stopsInOrder
            val response = placeRepository.directions(
                origin = current.origin,
                destination = current.destination,
                originPlaceId = current.originPlaceId,
                destinationPlaceId = current.destinationPlaceId,
                stops = ordered.map { it.id },
                headingFor = current.destination,
            )

            _state.update {
                it.copy(
                    stopsLeg = if (response.isSuccessful) response.assertedData else it.stopsLeg,
                    isRoutingStops = false,
                )
            }
        }
    }

    /** The driver is setting off: arrival times count from this moment. */
    fun startRoute() = _state.update { it.copy(departureEpochMillis = System.currentTimeMillis()) }

    /** Moves a stop earlier or later in the drive. */
    fun moveStop(placeId: String, by: Int) {
        _state.update { current ->
            val index = current.stops.indexOf(placeId)
            val target = index + by
            if (index < 0 || target !in current.stops.indices) return@update current

            val reordered = current.stops.toMutableList().apply {
                removeAt(index)
                add(target, placeId)
            }
            current.copy(stops = reordered)
        }
        refreshStopsRoute()
    }

    private fun GeoPoint.asWaypoint() = "$latitude,$longitude"

    /** Sets where the drive goes, then asks the API what is worth stopping at along it. */
    fun setRoute(
        origin: String,
        destination: String,
        originPlaceId: String? = null,
        destinationPlaceId: String? = null,
        originLabel: String? = null,
    ) {
        val from = origin.trim()
        val to = destination.trim()
        if (from.isEmpty() || to.isEmpty()) return

        _state.update { current ->
            current.copy(
                origin = from,
                destination = to,
                originPlaceId = originPlaceId,
                destinationPlaceId = destinationPlaceId,
                originLabel = originLabel,
                // A new plan is being made now, so arrival times count from now.
                departureEpochMillis = System.currentTimeMillis(),
                // Stops and the selected pin belong to the old drive.
                stops = emptyList(),
                selectedPinId = null,
                places = emptyList(),
                stopsLeg = null,
            )
        }
        refreshPlaces()
    }

    /** Drives the same road the other way. */
    fun swapRoute() = with(_state.value) {
        // The label belongs to the origin, so swapping leaves it behind with it.
        setRoute(destination, origin, destinationPlaceId, originPlaceId, originLabel = null)
    }

    /** Re-fetches suggestions for the current drive and budget. */
    fun refreshPlaces() {
        loadJob?.cancel()

        val plan = _state.value
        if (!plan.hasRoute) {
            // Nothing to ask for: there is no drive until the driver picks one.
            _state.update { it.copy(places = emptyList(), isLoadingPlaces = false, placesError = null) }
            return
        }

        loadJob = scope.launch {
            val current = _state.value
            _state.update { it.copy(isLoadingPlaces = true, placesError = null) }

            val response = placeRepository.suggestions(
                origin = current.origin,
                destination = current.destination,
                // Asked for at the widest budget, once. Moving the slider then filters what
                // is already here instead of spending another handful of billed searches on
                // a question the API has effectively already answered.
                budgetMinutes = Route.BUDGETS.max(),
                originPlaceId = current.originPlaceId,
                destinationPlaceId = current.destinationPlaceId,
            )

            _state.update { state ->
                if (response.isSuccessful) {
                    val plan = response.assertedData
                    state.copy(
                        places = plan.places,
                        origin = plan.origin,
                        destination = plan.destination,
                        driveMinutes = plan.driveMinutes,
                        distanceKm = plan.distanceKm,
                        routePoints = plan.routePoints,
                        originPoint = plan.originPoint,
                        destinationPoint = plan.destinationPoint,
                        // A stop that is no longer suggested can't stay on the route.
                        stops = state.stops.filter { id -> plan.places.any { it.id == id } },
                        stopsLeg = null,
                        isLoadingPlaces = false,
                        placesError = null,
                    )
                } else {
                    state.copy(
                        isLoadingPlaces = false,
                        // Keep whatever was already on screen; only report an empty failure.
                        placesError = if (state.places.isEmpty()) {
                            response.userMessage.ifBlank {
                                if (response.statusCode == HTTP_NOT_FOUND) NOT_FOUND_MESSAGE else FAILED_MESSAGE
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) = _state.update { it.copy(themeMode = mode) }

    fun setBudget(minutes: Int) {
        _state.update { current ->
            // A pin that no longer fits the budget should not stay selected.
            val stillFits = current.selectedPinId?.let { id ->
                current.placeOrNull(id)?.let { it.detourMinutes <= minutes }
            } ?: true
            current.copy(
                budget = minutes,
                selectedPinId = if (stillFits) current.selectedPinId else null,
                // A stop that no longer fits the budget comes off the route with it.
                stops = current.stops.filter { id ->
                    current.places.firstOrNull { it.id == id }?.let { it.detourMinutes <= minutes } ?: true
                },
            )
        }
        // No refetch: the places for every budget are already here.
        refreshStopsRoute()
    }

    fun selectPin(id: String?) = _state.update { it.copy(selectedPinId = id) }

    /** Writes to the database; the flow in [init] is what updates the state. */
    fun toggleSaved(place: Place) {
        scope.launch {
            if (place.id in _state.value.savedIds) {
                savedPlaceRepository.remove(place.id)
            } else {
                savedPlaceRepository.save(place)
            }
        }
    }

    fun removeSaved(placeId: String) {
        scope.launch { savedPlaceRepository.remove(placeId) }
    }

    fun toggleStop(id: String) {
        _state.update { current ->
            if (id in current.stops) {
                current.copy(stops = current.stops - id)
            } else {
                // Inserted where the road meets it, so the default order is the drive itself.
                // The driver can move it afterwards.
                val place = current.places.firstOrNull { it.id == id } ?: return@update current
                val ordered = current.stopsInOrder
                val index = ordered.indexOfFirst { it.routeProgress > place.routeProgress }
                    .takeIf { it >= 0 } ?: current.stops.size

                current.copy(stops = current.stops.toMutableList().apply { add(index, id) })
            }
        }
        refreshStopsRoute()
    }

    fun removeStop(id: String) {
        _state.update { it.copy(stops = it.stops - id) }
        refreshStopsRoute()
    }

    fun clearStops() {
        _state.update { it.copy(stops = emptyList(), stopsLeg = null) }
    }

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

    /**
     * Drops per-session choices on sign-out. Theme and units are device preferences, so they
     * stay — and so does location: it's a permission granted to this device, not something the
     * account owns, so signing out must not make the app forget it and gate on it again next
     * time. Re-read live rather than carried over, so a permission revoked in system settings
     * mid-session is picked up too, not just preserved.
     */
    fun clearSessionState() {
        _state.update { current ->
            WaysideUiState(
                themeMode = current.themeMode,
                units = current.units,
                hasLocationPermission = locationManager.hasPermission,
            )
        }
        // Saved places belong to the account that saved them.
        scope.launch { savedPlaceRepository.clear() }
        refreshPlaces()
    }

    private companion object {
        const val FAILED_MESSAGE = "Couldn't load places right now."
        const val NOT_FOUND_MESSAGE =
            "We couldn't find that drive. Pick both places from the suggestions as you type."
        const val HTTP_NOT_FOUND = 404
    }
}
