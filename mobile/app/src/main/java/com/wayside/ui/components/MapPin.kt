package com.wayside.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.WaysideMotion

/**
 * Teardrop map pin in the category colour.
 *
 * Default: the place's category glyph (fork-and-knife for food, a tree for nature, and so on —
 * see [com.wayside.data.PlaceCategory.icon]). Saved: bookmark glyph, taking over the same slot.
 * Selected: 3dp primary ring and a slightly larger footprint, so the tapped pin reads at a
 * glance.
 */
@Composable
fun MapPin(
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Place,
    selected: Boolean = false,
    saved: Boolean = false,
    width: Dp = 30.dp,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val wayside = LocalWaysideColors.current
    val interaction = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = WaysideMotion.tweenQuick(),
        label = "pinScale",
    )
    val height = width * 1.35f

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(Modifier.size(width = width, height = height)) {
            val w = size.width
            val h = size.height
            val r = w / 2f
            val teardrop = Path().apply {
                // Head circle, then a tail that meets the ground at the bottom centre.
                addArc(
                    Rect(0f, 0f, w, w),
                    150f,
                    240f,
                )
                lineTo(w / 2f, h)
                close()
            }
            if (selected) {
                drawCircle(
                    color = scheme.primary,
                    radius = r + 3.dp.toPx(),
                    center = Offset(r, r),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
            drawPath(teardrop, color = scheme.surface)
            val inset = 2.5.dp.toPx()
            val innerTeardrop = Path().apply {
                addArc(Rect(inset, inset, w - inset, w - inset), 150f, 240f)
                lineTo(w / 2f, h - inset * 1.4f)
                close()
            }
            drawPath(innerTeardrop, color = color)
        }
        Icon(
            imageVector = if (saved) Icons.Rounded.Bookmark else icon,
            contentDescription = null,
            tint = scheme.surface,
            modifier = Modifier
                .size(width * 0.44f)
                .offset(y = width * 0.28f),
        )
    }
}

/** The user's own position: a petrol dot with a slow pulse behind it. */
@Composable
fun UserLocationDot(
    modifier: Modifier = Modifier,
    pulse: Float,
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(56.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = scheme.primary.copy(alpha = 0.22f * (1f - pulse)),
                radius = (size.minDimension / 2f) * (0.35f + pulse * 0.65f),
                center = c,
            )
            drawCircle(color = scheme.surface, radius = 9.dp.toPx(), center = c)
            drawCircle(color = scheme.primary, radius = 6.dp.toPx(), center = c)
        }
    }
}

/** Numbered amber marker for a confirmed stop on the active route. */
@Composable
fun NumberedStopMarker(
    number: Int,
    modifier: Modifier = Modifier,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .width(30.dp)
            .height(30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(30.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = scheme.surface, radius = size.minDimension / 2f, center = c)
            drawCircle(color = wayside.accent, radius = size.minDimension / 2f - 2.dp.toPx(), center = c)
        }
        androidx.compose.material3.Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = wayside.onAccent,
        )
    }
}
