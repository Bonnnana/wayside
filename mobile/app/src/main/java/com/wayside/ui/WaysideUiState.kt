package com.wayside.ui

import com.wayside.data.Place
import com.wayside.data.Units
import com.wayside.models.ui.GeoPoint
import com.wayside.models.ui.RouteLeg
import java.time.Instant
import java.time.ZoneId
import com.wayside.ui.theme.ThemeMode

/**
 * App-wide UI state. Held by [com.wayside.repositories.AppStateRepository] — screens receive it
 * as a value, so they stay previewable.
 *
 * [places] and the drive figures come from the API; everything else is the user's own state and
 * lives only on the device.
 */
data class WaysideUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val budget: Int = 15,
    val selectedPinId: String? = null,
    val savedIds: Set<String> = emptySet(),
    /**
     * The saved places themselves. Saved is not a view of the current suggestions — lowering
     * the detour budget must not empty the Saved tab — so a place is kept here when it is
     * saved and read back from here when it falls out of the suggestion list.
     */
    val savedCache: Map<String, Place> = emptyMap(),
    val stops: List<String> = emptyList(),
    val interests: Set<String> = setOf("Coffee", "Food", "Nature", "Viewpoints"),
    val savedFilter: String = "All",
    val units: Units = Units.Kilometres,

    /** Suggestions for the current drive and budget, as returned by `/routes/suggestions`. */
    val places: List<Place> = emptyList(),
    /** Empty until the driver picks a drive. There is no sensible default destination. */
    val origin: String = "",
    val destination: String = "",
    /** Set when the user picked from the dropdown; routing prefers these over the text. */
    val originPlaceId: String? = null,
    val destinationPlaceId: String? = null,
    /**
     * What to call the origin on screen. Set when the drive starts from the driver's own
     * position, where [origin] is a "lat,lng" pair that means nothing to a person.
     */
    val originLabel: String? = null,
    /** The road between them, as drawn. Empty until a route comes back. */
    val routePoints: List<GeoPoint> = emptyList(),
    val originPoint: GeoPoint? = null,
    val destinationPoint: GeoPoint? = null,
    val driveMinutes: Int = 0,
    val distanceKm: Double = 0.0,
    /**
     * When the drive starts, as milliseconds since the epoch. Set when a route is planned and
     * again when the driver sets off, so arrival times are counted from the phone's own clock
     * rather than from a departure time baked into the app.
     */
    val departureEpochMillis: Long = System.currentTimeMillis(),
    /** Where the driver is, once a permission has been granted and a fix has arrived. */
    val currentLocation: GeoPoint? = null,
    val hasLocationPermission: Boolean = false,

    /**
     * The drive as planned *through the chosen stops*: start → stop → stop → destination.
     * Null when no stops are chosen, and the plain origin-to-destination route is drawn.
     */
    val stopsLeg: RouteLeg? = null,
    val isRoutingStops: Boolean = false,

    val isLoadingPlaces: Boolean = false,
    /** Non-null when the last load failed and there is nothing cached to fall back on. */
    val placesError: String? = null,
)

/** The origin as a person should read it. */
val WaysideUiState.originText: String
    get() = originLabel ?: origin

/** True once there is a drive to show anything about. */
val WaysideUiState.hasRoute: Boolean
    get() = origin.isNotBlank() && destination.isNotBlank()

/**
 * The suggestions that fit the current detour budget.
 *
 * [places] holds everything the API found at the widest budget, so narrowing the budget is a
 * filter rather than another round of billed searches.
 */
val WaysideUiState.placesWithinBudget: List<Place>
    get() = places.filter { it.detourMinutes <= budget }

/** The line the map should draw: the leg in progress if there is one, otherwise the plan. */
val WaysideUiState.drawnRoutePoints: List<GeoPoint>
    get() = stopsLeg?.points?.takeIf { it.isNotEmpty() } ?: routePoints

/** Saved places, preferring the fresh copy when the place is also in the current suggestions. */
val WaysideUiState.savedPlaces: List<Place>
    get() = savedIds
        .mapNotNull { id -> places.firstOrNull { it.id == id } ?: savedCache[id] }
        .sortedBy { it.routeProgress }

/** The place behind a selected pin or a navigation argument, if it is still in the list. */
fun WaysideUiState.placeOrNull(id: String?): Place? = places.firstOrNull { it.id == id }

/**
 * Stops in the order the driver arranged them.
 *
 * A new stop is inserted where the road meets it, which is the sensible default, but after
 * that the list itself is the order — reordering by route progress would silently undo a
 * choice the driver made deliberately.
 */
val WaysideUiState.stopsInOrder: List<Place>
    get() = stops.mapNotNull { id -> places.firstOrNull { it.id == id } ?: savedCache[id] }

val WaysideUiState.totalDetourMinutes: Int
    get() = stopsInOrder.sumOf { it.detourMinutes }

/** The departure, as minutes past midnight in the phone's own time zone. */
val WaysideUiState.departureMinutes: Int
    get() = Instant.ofEpochMilli(departureEpochMillis)
        .atZone(ZoneId.systemDefault())
        .let { it.hour * 60 + it.minute }

val WaysideUiState.arrivalMinutes: Int
    get() = departureMinutes + (stopsLeg?.driveMinutes ?: driveMinutes) + totalDetourMinutes

/** Arrival with no stops taken — the baseline a detour is measured against. */
val WaysideUiState.fastestArrivalMinutes: Int
    get() = departureMinutes + driveMinutes

/** Clock time you reach a stop: drive time to it, plus every detour taken before it. */
fun WaysideUiState.etaFor(place: Place): Int {
    val ordered = stopsInOrder
    val detoursBefore = ordered.takeWhile { it.id != place.id }.sumOf { it.detourMinutes }
    val driveSoFar = (driveMinutes * place.routeProgress).toInt()
    return departureMinutes + driveSoFar + detoursBefore
}
