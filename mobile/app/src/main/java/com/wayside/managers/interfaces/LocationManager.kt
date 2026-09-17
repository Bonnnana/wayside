package com.wayside.managers.interfaces

import com.wayside.models.ui.GeoPoint
import kotlinx.coroutines.flow.StateFlow

/**
 * Where the driver is.
 *
 * A road-trip app that can't answer that can't keep an arrival time honest, so this is a
 * singleton the whole app shares rather than something each screen asks for separately.
 */
interface LocationManager {
    /** Null until a permission is granted and the first fix arrives. */
    val currentLocation: StateFlow<GeoPoint?>

    val hasPermission: Boolean

    /** Starts listening. Safe to call repeatedly; does nothing without permission. */
    fun start()

    fun stop()

    /** A single fix, for a one-off question like "start the drive from here". */
    suspend fun awaitLocation(): GeoPoint?
}
