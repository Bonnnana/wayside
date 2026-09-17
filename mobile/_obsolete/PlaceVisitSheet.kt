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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
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
import com.wayside.data.Units
import com.wayside.data.formatDistance
import com.wayside.ui.components.CircleIconButton
import com.wayside.ui.components.DetourBadge
import com.wayside.ui.components.WaysideButton
import com.wayside.ui.components.WaysideButtonStyle
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

/**
 * What a tapped pin opens: enough about the place to decide, and the decision itself.
 *
 * "Visit" is not the same as adding a stop. Adding a stop plans the place into the drive;
 * visiting means going there now, so the map reroutes and the planned destination waits.
 */
@Composable
fun PlaceVisitSheet(
    place: Place,
    units: Units,
    added: Boolean,
    isReRouting: Boolean,
    onVisit: () -> Unit,
    onToggleStop: () -> Unit,
    onOpenDetail: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, MaterialTheme.shapes.extraLarge, clip = false)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(scheme.surface)
            .border(1.dp, wayside.hairline, MaterialTheme.shapes.extraLarge)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Spacer(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(place.category.color),
                    )
                    Text(
                        text = place.category.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = wayside.textMuted,
                    )
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = wayside.accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "${place.rating} (${place.ratingCount})",
                        style = MaterialTheme.typography.labelMedium,
                        color = wayside.textMuted,
                    )
                }
            }
            CircleIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Close",
                onClick = onDismiss,
                tint = wayside.textMuted,
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DetourBadge(minutes = place.detourMinutes)
            Text(
                text = "${formatDistance(place.distanceKm, units)} off route",
                style = MaterialTheme.typography.labelMedium,
                color = wayside.textMuted,
            )
        }

        if (place.highlight.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = place.highlight,
                style = MaterialTheme.typography.bodyMedium,
                color = wayside.textMuted,
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WaysideButton(
                text = if (isReRouting) "Rerouting…" else "Visit",
                onClick = onVisit,
                enabled = !isReRouting,
                style = WaysideButtonStyle.Cta,
                height = 46.dp,
                modifier = Modifier.weight(1f),
            )
            WaysideButton(
                text = if (added) "Added" else "Add stop",
                onClick = onToggleStop,
                style = WaysideButtonStyle.Secondary,
                height = 46.dp,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))
        WaysideButton(
            text = "See details",
            onClick = onOpenDetail,
            style = WaysideButtonStyle.Ghost,
            height = 40.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "Visit sheet", showBackground = true)
@Composable
private fun PlaceVisitSheetPreview() {
    WaysideTheme(ThemeMode.Light) {
        PlaceVisitSheet(
            place = SAMPLE_PLACES.first(),
            units = Units.Kilometres,
            added = false,
            isReRouting = false,
            onVisit = {},
            onToggleStop = {},
            onOpenDetail = {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
