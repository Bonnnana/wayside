package com.wayside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wayside.data.Place
import com.wayside.data.Route
import com.wayside.data.arrivalMinutes
import com.wayside.data.etaForStop
import com.wayside.data.formatClock
import com.wayside.data.stopsInRouteOrder
import com.wayside.data.totalDetourMinutes
import com.wayside.ui.WaysideUiState
import com.wayside.ui.components.CircleIconButton
import com.wayside.ui.components.MapAnchor
import com.wayside.ui.components.NumberedStopMarker
import com.wayside.ui.components.StylizedMap
import com.wayside.ui.components.WaysideButton
import com.wayside.ui.components.WaysideButtonStyle
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

@Composable
fun ActiveRouteScreen(
    state: WaysideUiState,
    onBack: () -> Unit,
    onRemoveStop: (String) -> Unit,
    onEnd: () -> Unit,
    onEditStops: () -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    val stops = stopsInRouteOrder(state.stops)
    val totalDetour = totalDetourMinutes(state.stops)

    Box(Modifier.fillMaxSize()) {
        StylizedMap(
            modifier = Modifier.fillMaxSize(),
            showRoute = true,
            corridorWidth = 0.dp,
        ) { mapWidth, mapHeight ->
            stops.forEachIndexed { index, place ->
                MapAnchor(
                    x = place.mapX,
                    y = place.mapY,
                    mapWidth = mapWidth,
                    mapHeight = mapHeight,
                    anchorWidth = 30.dp,
                    anchorHeight = 30.dp,
                ) {
                    NumberedStopMarker(number = index + 1)
                }
            }
        }

        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .shadow(8.dp, MaterialTheme.shapes.large, clip = false)
                .clip(MaterialTheme.shapes.large)
                .background(scheme.surface)
                .border(1.dp, wayside.hairline, MaterialTheme.shapes.large)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                size = 36.dp,
                container = scheme.surfaceContainerHigh,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${Route.ORIGIN.substringBefore(",")} → ${Route.DESTINATION}",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Arrive ${formatClock(arrivalMinutes(state.stops))} · " +
                        "${Route.DRIVE_MINUTES} min drive + $totalDetour min of stops",
                    style = MaterialTheme.typography.labelSmall,
                    color = wayside.textMuted,
                )
            }
            WaysideButton(
                text = "End",
                onClick = onEnd,
                style = WaysideButtonStyle.Ghost,
                contentColorOverride = scheme.error,
                horizontalPadding = 12.dp,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), clip = false)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(scheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Today's stops",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "+$totalDetour min total",
                    style = MaterialTheme.typography.labelLarge,
                    color = wayside.accent,
                )
            }
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                TimelineEndpoint(
                    icon = Icons.Rounded.MyLocation,
                    title = Route.ORIGIN,
                    subtitle = "Leave ${formatClock(Route.DEPARTURE_MINUTES)}",
                    accent = scheme.primary,
                )
                stops.forEachIndexed { index, place ->
                    TimelineStopRow(
                        number = index + 1,
                        place = place,
                        eta = formatClock(etaForStop(state.stops, place)),
                        onRemove = { onRemoveStop(place.id) },
                    )
                }
                if (stops.isEmpty()) {
                    Text(
                        text = "No stops yet. Everything here is the fastest way there.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = wayside.textMuted,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
                TimelineEndpoint(
                    icon = Icons.Rounded.Flag,
                    title = Route.DESTINATION,
                    subtitle = "Arrive ${formatClock(arrivalMinutes(state.stops))}",
                    accent = wayside.accent,
                )
            }
            Spacer(Modifier.height(16.dp))
            WaysideButton(
                text = "Edit stops",
                onClick = onEditStops,
                style = WaysideButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp,
            )
        }
    }
}

@Composable
private fun TimelineEndpoint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
            )
        }
    }
}

@Composable
private fun TimelineStopRow(
    number: Int,
    place: Place,
    eta: String,
    onRemove: () -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberedStopMarker(number = number)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
            )
            Text(
                text = "ETA $eta · +${place.detourMinutes} min detour",
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
            )
        }
        CircleIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "Remove ${place.name}",
            onClick = onRemove,
            size = 30.dp,
            tint = wayside.textMuted,
            container = scheme.surfaceContainerHigh,
        )
    }
}

@Preview(name = "Active route · light", showBackground = true, device = "id:pixel_7")
@Composable
private fun ActiveRouteLightPreview() {
    WaysideTheme(ThemeMode.Light) {
        ActiveRouteScreen(
            state = WaysideUiState(stops = listOf("queluz", "poetas")),
            onBack = {},
            onRemoveStop = {},
            onEnd = {},
            onEditStops = {},
        )
    }
}

@Preview(name = "Active route · dark", showBackground = true, device = "id:pixel_7")
@Composable
private fun ActiveRouteDarkPreview() {
    WaysideTheme(ThemeMode.Dark) {
        ActiveRouteScreen(
            state = WaysideUiState(stops = listOf("sacolinha", "capuchos", "peninha")),
            onBack = {},
            onRemoveStop = {},
            onEnd = {},
            onEditStops = {},
        )
    }
}
