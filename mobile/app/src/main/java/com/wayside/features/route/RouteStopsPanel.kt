package com.wayside.features.route

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wayside.data.Place
import com.wayside.data.SAMPLE_PLACES
import com.wayside.ui.components.CircleIconButton
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

/**
 * The stops, in the order they will be driven.
 *
 * This is the plan made visible: the same order the route line follows and the same order the
 * arrival time is built from. Moving a row moves the road.
 */
@Composable
fun RouteStopsPanel(
    origin: String,
    destination: String,
    stops: List<Place>,
    isRouting: Boolean,
    onMoveStop: (String, Int) -> Unit,
    onRemoveStop: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (stops.isEmpty()) return

    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, MaterialTheme.shapes.large, clip = false)
            .clip(MaterialTheme.shapes.large)
            .background(scheme.surface)
            .border(1.dp, wayside.hairline, MaterialTheme.shapes.large)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Your route",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (isRouting) {
                Text(
                    text = "Recalculating…",
                    style = MaterialTheme.typography.labelSmall,
                    color = wayside.textMuted,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        EndpointRow(label = origin, isStart = true)

        stops.forEachIndexed { index, place ->
            StopRow(
                number = index + 1,
                place = place,
                canMoveUp = index > 0,
                canMoveDown = index < stops.lastIndex,
                onMoveUp = { onMoveStop(place.id, -1) },
                onMoveDown = { onMoveStop(place.id, 1) },
                onRemove = { onRemoveStop(place.id) },
            )
        }

        EndpointRow(label = destination, isStart = false)
    }
}

@Composable
private fun EndpointRow(label: String, isStart: Boolean) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mirrors the map: a filled disc starts the drive, a ringed one ends it.
        Spacer(
            Modifier
                .size(if (isStart) 12.dp else 14.dp)
                .clip(CircleShape)
                .background(if (isStart) scheme.primary else wayside.accent)
                .then(
                    if (isStart) Modifier else Modifier.border(3.dp, scheme.surface, CircleShape),
                ),
        )
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                text = if (isStart) "Start" else "Destination",
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
            )
        }
    }
}

@Composable
private fun StopRow(
    number: Int,
    place: Place,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(place.category.color),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.surface,
            )
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
            )
            Text(
                text = "+${place.detourMinutes} min",
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
            )
        }

        if (canMoveUp) {
            CircleIconButton(
                icon = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Move ${place.name} earlier",
                onClick = onMoveUp,
                size = 32.dp,
                tint = wayside.textMuted,
            )
        }
        if (canMoveDown) {
            CircleIconButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Move ${place.name} later",
                onClick = onMoveDown,
                size = 32.dp,
                tint = wayside.textMuted,
            )
        }
        CircleIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "Remove ${place.name}",
            onClick = onRemove,
            size = 32.dp,
            tint = wayside.textMuted,
        )
    }
}

@Preview(name = "Route stops", showBackground = true)
@Composable
private fun RouteStopsPanelPreview() {
    WaysideTheme(ThemeMode.Light) {
        RouteStopsPanel(
            origin = "Alfama, Lisbon",
            destination = "Sintra",
            stops = SAMPLE_PLACES.take(3),
            isRouting = false,
            onMoveStop = { _, _ -> },
            onRemoveStop = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
