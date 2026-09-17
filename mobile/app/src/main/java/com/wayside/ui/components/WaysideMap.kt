package com.wayside.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.wayside.R
import com.wayside.data.Place
import com.wayside.models.ui.GeoPoint
import com.wayside.utils.Geo
import kotlinx.coroutines.launch
import com.wayside.ui.theme.LocalWaysideColors

/**
 * The real map, wearing the Wayside palette.
 *
 * Google's own colours would fight everything else on screen, so the map is restyled from
 * `res/raw/map_style_*.json` — built from the same tokens as `LocalWaysideColors` — and its
 * business POIs and transit are switched off, because the whole point of this screen is the
 * places *we* suggest.
 *
 * @param corridorWidth the translucent "detour budget" band drawn under the route. Animate it
 *   to show a wider budget opening up more of the map.
 */
@Composable
fun WaysideMap(
    places: List<Place>,
    modifier: Modifier = Modifier,
    selectedPlaceId: String? = null,
    corridorWidth: Dp = 0.dp,
    /**
     * Numbers the markers in route order instead of colouring them by category — what the
     * active route wants, where the question is "which stop is next", not "what kind is it".
     */
    numbered: Boolean = false,
    /** The road, as returned by the API. Drawn in preference to a line through the places. */
    routePoints: List<GeoPoint> = emptyList(),
    originPoint: GeoPoint? = null,
    destinationPoint: GeoPoint? = null,
    /** Enables the system blue dot. Only pass true once a location permission is granted. */
    showMyLocation: Boolean = false,
    /**
     * Where the driver is. Drawn as our own marker as well as the system dot, because the
     * system dot is off whenever the permission isn't granted — and a map with no "you" on it
     * is the one thing this app can't afford.
     */
    myLocation: GeoPoint? = null,
    /**
     * How far Google's own controls and logo should be pushed in from each edge.
     *
     * The map draws full-bleed behind the status bar and behind whatever the screen overlays
     * on it, so without this the recentre button lands under the system bar and can't be
     * tapped. Each screen knows what it has stacked on top; the status-bar inset is added here.
     */
    topContentPadding: Dp = 0.dp,
    bottomContentPadding: Dp = 0.dp,
    onPlaceClick: (String) -> Unit = {},
    onMapClick: () -> Unit = {},
) {
    val wayside = LocalWaysideColors.current
    val context = LocalContext.current
    val density = LocalDensity.current

    val style = remember(wayside.isDark) {
        MapStyleOptions.loadRawResourceStyle(
            context,
            if (wayside.isDark) R.raw.map_style_dark else R.raw.map_style_light,
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_CENTRE, DEFAULT_ZOOM)
    }

    var mapLoaded by remember { mutableStateOf(false) }

    // The camera moves itself once per thing worth framing, and then leaves the driver alone.
    // Location updates arrive every few seconds and jitter by metres even standing still, so
    // re-framing on every fix drags the map out from under anyone trying to pan it.
    var centredOnDriver by remember { mutableStateOf(false) }
    var framedRoute by remember(routePoints) { mutableStateOf(false) }

    // The road if we have it; otherwise a line through the places in route order, which is
    // better than nothing but is not where anyone actually drives.
    val line = remember(routePoints, places) {
        if (routePoints.isNotEmpty()) {
            routePoints.map { LatLng(it.latitude, it.longitude) }
        } else {
            places.sortedBy { it.routeProgress }.map { LatLng(it.latitude, it.longitude) }
        }
    }

    // How far along the route the driver has got. Kept here, and reset only when the route
    // itself changes, so progress can never slide backwards.
    var progressIndex by remember(routePoints) { mutableIntStateOf(0) }

    LaunchedEffect(routePoints, myLocation) {
        if (routePoints.isNotEmpty()) {
            progressIndex = Geo.progressIndex(routePoints, myLocation, progressIndex)
        }
    }

    // What's left of the drive. Shrinks as the driver makes progress along it.
    val remaining = remember(line, routePoints, progressIndex) {
        if (routePoints.isEmpty()) {
            line
        } else {
            routePoints.drop(progressIndex).map { LatLng(it.latitude, it.longitude) }
        }
    }

    // The gap between where the driver is and where the drive begins. Drawn dashed, because
    // it isn't part of the route — it's the walk or drive to the start of it.
    val approach = remember(myLocation, originPoint) {
        val start = originPoint
        val tooFar = myLocation != null && start != null &&
            Geo.distanceMetres(myLocation, start) > FRAME_WITH_DRIVER_LIMIT_M

        if (myLocation == null || start == null || tooFar || Geo.isAtPoint(myLocation, start)) {
            emptyList()
        } else {
            listOf(
                LatLng(myLocation.latitude, myLocation.longitude),
                LatLng(start.latitude, start.longitude),
            )
        }
    }

    // The camera has to hold the whole drive, not just the places along it. The driver is
    // included only when they are near it — framing Skopje and Lisbon together zooms out to
    // half of Europe and makes both useless.
    val framed = remember(line, places, myLocation) {
        val route = line + places.map { LatLng(it.latitude, it.longitude) }
        val me = myLocation?.takeIf { point ->
            originPoint == null ||
                Geo.distanceMetres(point, originPoint) < FRAME_WITH_DRIVER_LIMIT_M
        }
        route + listOfNotNull(me?.let { LatLng(it.latitude, it.longitude) })
    }

    // newLatLngBounds needs a laid-out map, so this waits for onMapLoaded rather than firing
    // as soon as the places arrive.
    LaunchedEffect(mapLoaded, framed, myLocation) {
        if (!mapLoaded) return@LaunchedEffect

        // Nothing planned yet: the only thing worth looking at is where the driver is.
        if (framed.isEmpty()) {
            val me = myLocation ?: return@LaunchedEffect
            if (centredOnDriver) return@LaunchedEffect
            centredOnDriver = true
            runCatching {
                cameraPositionState.animate(
                    // Wider than the recentre button gives: with nothing planned, the useful
                    // view is the area you're in, not the street you're on.
                    CameraUpdateFactory.newLatLngZoom(LatLng(me.latitude, me.longitude), INITIAL_ZOOM),
                )
            }
            return@LaunchedEffect
        }

        if (framedRoute) return@LaunchedEffect
        framedRoute = true

        val bounds = LatLngBounds.builder()
            .apply { framed.forEach(::include) }
            .build()
        val padding = with(density) { BOUNDS_PADDING.roundToPx() }
        runCatching { cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, padding)) }
    }

    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val scope = rememberCoroutineScope()

    Box(modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentPadding = PaddingValues(
                top = statusBars.calculateTopPadding() + topContentPadding,
                bottom = bottomContentPadding,
                start = 8.dp,
                end = 8.dp,
            ),
            properties = MapProperties(
                mapStyleOptions = style,
                // Off deliberately. This layer is what logs `DEVELOPER_ERROR` through Play
                // services on some devices, and we draw the driver ourselves anyway.
                isMyLocationEnabled = false,
            ),
            uiSettings = MapUiSettings(
                // The screen has its own controls; Google's would sit under our sheet anyway.
                compassEnabled = false,
                mapToolbarEnabled = false,
                zoomControlsEnabled = false,
                // Ours instead, below — Google's routes through a Play services client we don't
                // control, and it logs DEVELOPER_ERROR on plenty of devices that are set up fine.
                myLocationButtonEnabled = false,
            ),
            onMapLoaded = { mapLoaded = true },
            onMapClick = { onMapClick() },
        ) {
            if (remaining.size >= 2) {
                val corridorPx = with(density) { corridorWidth.toPx() }
                if (corridorPx > 0f) {
                    Polyline(
                        points = remaining,
                        color = wayside.accent.copy(alpha = 0.18f),
                        width = corridorPx,
                    )
                }
                Polyline(
                    points = remaining,
                    color = wayside.accent,
                    width = with(density) { ROUTE_WIDTH.toPx() },
                )
            }

            if (approach.size == 2) {
                Polyline(
                    points = approach,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    width = with(density) { APPROACH_WIDTH.toPx() },
                    // Dashed and in the primary colour: clearly a different kind of line from the
                    // amber route, so nobody reads it as part of the drive.
                    pattern = listOf(Dash(with(density) { 18.dp.toPx() }), Gap(with(density) { 10.dp.toPx() })),
                )
            }

            val endpointPx = with(density) { ENDPOINT_SIZE.roundToPx() }
            val startColour = MaterialTheme.colorScheme.primary.toArgb()
            val youColour = MaterialTheme.colorScheme.primary.toArgb()
            val youPx = with(density) { YOU_SIZE.roundToPx() }
            val endColour = wayside.accent.toArgb()

            originPoint?.let { point ->
                Marker(
                    state = remember(point) { MarkerState(LatLng(point.latitude, point.longitude)) },
                    title = "Start",
                    icon = remember(point, startColour) {
                        BitmapDescriptorFactory.fromBitmap(
                            endpointPin(startColour, endpointPx, doubleRing = false),
                        )
                    },
                    zIndex = 2f,
                )
            }

            myLocation?.let { point ->
                Marker(
                    state = remember(point) { MarkerState(LatLng(point.latitude, point.longitude)) },
                    title = "You",
                    icon = remember(youColour, youPx) {
                        BitmapDescriptorFactory.fromBitmap(youPin(youColour, youPx))
                    },
                    // Centred on the point: this is a position, not a pin dropped on one.
                    anchor = Offset(0.5f, 0.5f),
                    zIndex = 3f,
                )
            }

            destinationPoint?.let { point ->
                Marker(
                    state = remember(point) { MarkerState(LatLng(point.latitude, point.longitude)) },
                    title = "Destination",
                    // A second ring, so the two ends of the drive are never mistaken for each other.
                    icon = remember(point, endColour) {
                        BitmapDescriptorFactory.fromBitmap(
                            endpointPin(endColour, endpointPx, doubleRing = true),
                        )
                    },
                    zIndex = 2f,
                )
            }

            val pinSizePx = with(density) { PIN_SIZE.roundToPx() }

            places.forEachIndexed { index, place ->
                // Keyed, because the list changes whenever the budget does — without it a marker
                // would inherit the remembered state of whichever place used to be in that slot.
                key(place.id) {
                    if (numbered) {
                        Marker(
                            state = remember { MarkerState(LatLng(place.latitude, place.longitude)) },
                            title = place.name,
                            snippet = "+${place.detourMinutes} min detour",
                            icon = remember(place.id, index) {
                                BitmapDescriptorFactory.fromBitmap(
                                    numberedPin(
                                        number = index + 1,
                                        color = place.category.color.toArgb(),
                                        sizePx = pinSizePx,
                                    ),
                                )
                            },
                            zIndex = if (place.id == selectedPlaceId) 1f else 0f,
                            onClick = {
                                onPlaceClick(place.id)
                                // Consumed: we drive selection ourselves rather than letting the
                                // map recentre and pop its own info window.
                                true
                            },
                        )
                    } else {
                        // A composable marker rather than a plain colour-hued default pin, so
                        // the category glyph (fork-and-knife, tree, …) shows up on the map
                        // itself — see MapPin.
                        MarkerComposable(
                            state = remember { MarkerState(LatLng(place.latitude, place.longitude)) },
                            title = place.name,
                            snippet = "+${place.detourMinutes} min detour",
                            zIndex = if (place.id == selectedPlaceId) 1f else 0f,
                            onClick = {
                                onPlaceClick(place.id)
                                true
                            },
                        ) {
                            MapPin(
                                color = place.category.color,
                                icon = place.category.icon,
                                selected = place.id == selectedPlaceId,
                                width = PIN_SIZE,
                            )
                        }
                    }
                }
            }
        }

        // Recentres on the driver. Worth its own control because the drive is often framed
        // somewhere else entirely — a route in Portugal while the phone is in Skopje.
        if (showMyLocation) {
            FilledIconButton(
                enabled = myLocation != null,
                onClick = {
                    val point = myLocation ?: return@FilledIconButton
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(point.latitude, point.longitude),
                                RECENTRE_ZOOM,
                            ),
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = bottomContentPadding),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = "Centre on my location",
                )
            }
        }
    }
}

/** The driver. A disc with a white collar, so it reads on both map themes. */
private fun youPin(color: Int, sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = sizePx / 2f

    val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        alpha = 45
    }
    val collar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
    }
    val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    canvas.drawCircle(radius, radius, radius, halo)
    canvas.drawCircle(radius, radius, radius * 0.52f, collar)
    canvas.drawCircle(radius, radius, radius * 0.38f, dot)

    return bitmap
}

/**
 * The two ends of the drive. The start is a filled disc; the destination gets a second ring
 * inside it, so the ends are distinguishable at a glance and without relying on colour.
 */
private fun endpointPin(color: Int, sizePx: Int, doubleRing: Boolean): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = sizePx / 2f
    val ringWidth = sizePx * 0.12f

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = ringWidth
    }

    canvas.drawCircle(radius, radius, radius - ringWidth / 2, fill)
    canvas.drawCircle(radius, radius, radius - ringWidth / 2, white)

    if (doubleRing) {
        canvas.drawCircle(radius, radius, radius * 0.42f, white)
    }

    return bitmap
}

/**
 * A numbered stop pin, drawn rather than loaded: a filled circle in the category colour, a
 * white ring so it reads against both map themes, and the stop number in the middle.
 */
private fun numberedPin(number: Int, color: Int, sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = sizePx / 2f
    val ringWidth = sizePx * 0.1f

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = ringWidth
    }
    val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = sizePx * 0.48f
        isFakeBoldText = true
    }

    canvas.drawCircle(radius, radius, radius - ringWidth / 2, fill)
    canvas.drawCircle(radius, radius, radius - ringWidth / 2, ring)
    // Centre on the text's own box, not on the font's line box.
    val baseline = radius - (label.descent() + label.ascent()) / 2
    canvas.drawText(number.toString(), radius, baseline, label)

    return bitmap
}


/** Roughly the midpoint of the Lisbon → Sintra drive, before any places have loaded. */
private val DEFAULT_CENTRE = LatLng(38.7550, -9.3300)
private const val DEFAULT_ZOOM = 10.5f
private val BOUNDS_PADDING = 72.dp
private val ROUTE_WIDTH = 5.dp
private val PIN_SIZE = 34.dp
private val ENDPOINT_SIZE = 26.dp
private val YOU_SIZE = 30.dp
private val APPROACH_WIDTH = 4.dp
private const val RECENTRE_ZOOM = 15.5f

/** The opening view, before any drive is planned — noticeably wider than the recenter zoom. */
private const val INITIAL_ZOOM = 11f

/** Beyond this, the driver is not "near the drive" — they are somewhere else. 150km. */
private const val FRAME_WITH_DRIVER_LIMIT_M = 150_000.0
