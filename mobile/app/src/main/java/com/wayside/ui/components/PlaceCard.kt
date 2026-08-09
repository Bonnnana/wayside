package com.wayside.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayside.data.Place
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.WaysideMotion
import com.wayside.ui.theme.pressScale

/**
 * The suggestion card used in the suggestions sheet.
 *
 * @param ringed draws a 3dp primary ring — set when the matching map pin is selected.
 */
@Composable
fun PlaceCard(
    place: Place,
    added: Boolean,
    onClick: () -> Unit,
    onToggleAdd: () -> Unit,
    modifier: Modifier = Modifier,
    ringed: Boolean = false,
    width: Dp = 250.dp,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val interaction = remember { MutableInteractionSource() }
    val ringColor by animateColorAsState(
        targetValue = if (ringed) scheme.primary else Color.Transparent,
        animationSpec = WaysideMotion.tweenStandard(),
        label = "cardRing",
    )

    Column(
        modifier = modifier
            .width(width)
            .pressScale(interaction)
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
            .background(scheme.surface)
            .border(if (ringed) 3.dp else 1.dp, if (ringed) ringColor else wayside.hairline, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Box {
            PlaceholderImage(
                color = place.category.color,
                seed = place.name.length,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
            )
            AddStopButton(
                added = added,
                onClick = onToggleAdd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
            )
        }
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryChip(place.category)
                Spacer(Modifier.weight(1f))
                DetourBadge(place.detourMinutes, showIcon = false)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            RatingLine(place.rating, place.ratingCount)
            Spacer(Modifier.height(8.dp))
            Text(
                text = place.highlight,
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Grid card used on the saved screen. */
@Composable
fun SavedPlaceCard(
    place: Place,
    detourLabel: String,
    onClick: () -> Unit,
    onUnsave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .pressScale(interaction)
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, wayside.hairline, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Box {
            PlaceholderImage(
                color = place.category.color,
                seed = place.name.length,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
            )
            CircleIconButton(
                icon = Icons.Rounded.Bookmark,
                contentDescription = "Remove from saved",
                onClick = onUnsave,
                tint = scheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                size = 30.dp,
            )
        }
        Column(Modifier.padding(12.dp)) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            CategoryChip(place.category)
            Spacer(Modifier.height(8.dp))
            Text(
                text = detourLabel,
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
            )
        }
    }
}

@Composable
fun RatingLine(
    rating: Double,
    ratingCount: Int,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    val wayside = LocalWaysideColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Rounded.Star,
            contentDescription = null,
            tint = wayside.accent,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = buildString {
                append("%.1f".format(rating))
                append(" · ")
                append("%,d".format(ratingCount))
                append(" ratings")
                if (trailing != null) {
                    append(" · ")
                    append(trailing)
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = wayside.textMuted,
        )
    }
}

/** Amber add button that turns green with a check once the stop is on the route. */
@Composable
fun AddStopButton(
    added: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
) {
    val wayside = LocalWaysideColors.current
    val container by animateColorAsState(
        targetValue = if (added) wayside.success else wayside.accent,
        animationSpec = WaysideMotion.tweenStandard(),
        label = "addStopContainer",
    )
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (added) Icons.Rounded.Check else Icons.Rounded.Add,
            contentDescription = if (added) "Remove from route" else "Add to route",
            tint = if (added) Color.White else wayside.onAccent,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** Circular icon button that floats over artwork and maps. */
@Composable
fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color? = null,
    container: Color? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(container ?: scheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: scheme.onSurface,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
fun BookmarkToggle(
    saved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val scheme = MaterialTheme.colorScheme
    CircleIconButton(
        icon = if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
        contentDescription = if (saved) "Saved" else "Save for later",
        onClick = onClick,
        modifier = modifier,
        size = size,
        tint = if (saved) scheme.primary else scheme.onSurface,
    )
}
