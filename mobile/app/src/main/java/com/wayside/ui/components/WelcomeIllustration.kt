package com.wayside.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import com.wayside.data.PlaceCategory
import com.wayside.ui.theme.LocalWaysideColors

/**
 * The illustrated backdrop shared by welcome and login: a low sun with concentric rings,
 * three layered hills, and a winding road with a dashed petrol overlay running up it.
 * Two category dots sit on the road as stand-in pins.
 */
@Composable
fun WelcomeIllustration(
    modifier: Modifier = Modifier,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    val dark = wayside.isDark

    val sky = if (dark) Color(0xFF1A3038) else Color(0xFFF3E3C8)
    val skyTop = if (dark) Color(0xFF12242A) else Color(0xFFFCF5E7)
    val hillBack = if (dark) Color(0xFF23434A) else lerp(scheme.primary, Color.White, 0.80f)
    val hillMid = if (dark) Color(0xFF1F4149) else lerp(scheme.primary, Color.White, 0.62f)
    val hillFront = if (dark) Color(0xFF17343B) else lerp(scheme.primary, Color.White, 0.42f)

    // smoothPath's very first curve segment always starts with zero velocity (its control
    // point and start point are the same), which starves the dash pattern right at the start —
    // the actual bug behind the line looking disconnected from the pin, no matter how close the
    // path's literal start sat to it. So the path still starts a short, deliberately tiny step
    // before the food pin (keeping the pin on the normal, non-degenerate second segment, same
    // as the viewpoints pin below) instead of exactly on it — short enough that it's covered by
    // the pin's own halo, but long enough for the dash pattern to be in its usual rhythm by the
    // time it's visible.
    val roadPoints = remember {
        listOf(
            Offset(0.315f, 0.515f),
            Offset(0.32f, 0.52f),
            Offset(0.26f, 0.44f),
            Offset(0.40f, 0.37f),
            Offset(0.34f, 0.30f),
            Offset(0.50f, 0.24f),
        )
    }

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(skyTop, sky),
            ),
        )

        // Sun with concentric rings, top-right.
        val sunCenter = Offset(w * 0.79f, h * 0.13f)
        val sunRadius = w * 0.085f
        listOf(2.6f, 2.0f, 1.5f).forEach { multiplier ->
            drawCircle(
                color = wayside.accent.copy(alpha = if (dark) 0.16f else 0.22f),
                radius = sunRadius * multiplier,
                center = sunCenter,
                style = Stroke(width = w * 0.004f),
            )
        }
        drawCircle(color = wayside.accent, radius = sunRadius, center = sunCenter)

        // Three layered hills, stacked in the middle third so the copy block below stays clear.
        drawPath(
            path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.36f)
                quadraticTo(w * 0.30f, h * 0.22f, w * 0.60f, h * 0.34f)
                quadraticTo(w * 0.84f, h * 0.43f, w, h * 0.30f)
                lineTo(w, h)
                close()
            },
            color = hillBack,
        )
        drawPath(
            path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.52f)
                quadraticTo(w * 0.34f, h * 0.36f, w * 0.68f, h * 0.52f)
                quadraticTo(w * 0.88f, h * 0.60f, w, h * 0.49f)
                lineTo(w, h)
                close()
            },
            color = hillMid,
        )
        drawPath(
            path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.70f)
                quadraticTo(w * 0.40f, h * 0.56f, w, h * 0.72f)
                lineTo(w, h)
                close()
            },
            color = hillFront,
        )

        // The road, and the two pins that make it a Wayside road. The bottom one sits right
        // where the road starts (see roadPoints above) and is drawn noticeably larger — a "you
        // are here" anchor for the drive rather than just another stop — so the road reads as
        // starting from it rather than floating near it.
        drawWindingRoad(
            roadColor = if (dark) Color(0xFFE8DFCC) else Color.White,
            dashColor = scheme.primary,
            points = roadPoints,
        )
        drawCircle(color = Color.White, radius = w * 0.028f, center = Offset(w * 0.37f, h * 0.335f))
        drawCircle(
            color = PlaceCategory.Viewpoints.color,
            radius = w * 0.019f,
            center = Offset(w * 0.37f, h * 0.335f),
        )
        drawCircle(color = Color.White, radius = w * 0.046f, center = Offset(w * 0.29f, h * 0.48f))
        drawCircle(
            color = PlaceCategory.Food.color,
            radius = w * 0.032f,
            center = Offset(w * 0.29f, h * 0.48f),
        )
    }
}

/** Small circular app mark: a petrol disc with the Wayside road inside it. */
@Composable
fun AppMark(
    modifier: Modifier = Modifier,
    cornerFraction: Float = 0.5f,
) {
    val scheme = MaterialTheme.colorScheme
    val wayside = LocalWaysideColors.current
    Canvas(modifier) {
        val radius = size.minDimension * cornerFraction
        drawRoundRect(
            color = scheme.primary,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
        )
        val road = Path().apply {
            moveTo(size.width * 0.30f, size.height * 0.80f)
            quadraticTo(
                size.width * 0.62f,
                size.height * 0.66f,
                size.width * 0.42f,
                size.height * 0.46f,
            )
            quadraticTo(
                size.width * 0.26f,
                size.height * 0.30f,
                size.width * 0.68f,
                size.height * 0.24f,
            )
        }
        drawPath(
            road,
            color = scheme.onPrimary,
            style = Stroke(width = size.minDimension * 0.09f),
        )
        drawCircle(
            color = wayside.accent,
            radius = size.minDimension * 0.075f,
            center = Offset(size.width * 0.68f, size.height * 0.24f),
        )
    }
}

/** Reusable vertical scrim so copy stays legible over the illustration. */
fun scrimBrush(surface: Color): androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.Brush.verticalGradient(
        0f to Color.Transparent,
        0.35f to surface.copy(alpha = 0.55f),
        0.6f to surface.copy(alpha = 0.92f),
        1f to surface,
    )
