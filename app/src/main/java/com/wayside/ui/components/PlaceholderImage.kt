package com.wayside.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import com.wayside.ui.theme.LocalWaysideColors

/**
 * Stands in for a photo everywhere a photo would go: a flat Canvas landscape
 * mixed from the category colour, so cards stay recognisable per category.
 */
@Composable
fun PlaceholderImage(
    color: Color,
    modifier: Modifier = Modifier,
    seed: Int = 0,
) {
    val dark = LocalWaysideColors.current.isDark
    val skyTop = lerp(color, if (dark) Color(0xFF10201F) else Color.White, if (dark) 0.55f else 0.62f)
    val skyBottom = lerp(color, if (dark) Color(0xFF10201F) else Color.White, if (dark) 0.30f else 0.28f)
    val sun = lerp(color, Color.White, if (dark) 0.35f else 0.72f)
    val hillBack = lerp(color, if (dark) Color.Black else Color.White, if (dark) 0.34f else 0.10f)
    val hillMid = color
    val hillFront = lerp(color, Color.Black, if (dark) 0.42f else 0.24f)

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawRect(brush = Brush.verticalGradient(listOf(skyTop, skyBottom)))

        // Sun sits left or right depending on the seed, so a row of cards varies.
        val sunX = if (seed % 2 == 0) w * 0.76f else w * 0.24f
        drawCircle(color = sun, radius = h * 0.16f, center = Offset(sunX, h * 0.30f))

        val backCrest = h * (0.56f + (seed % 3) * 0.03f)
        drawPath(
            path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, backCrest + h * 0.10f)
                quadraticTo(w * 0.28f, backCrest - h * 0.14f, w * 0.55f, backCrest + h * 0.04f)
                quadraticTo(w * 0.80f, backCrest + h * 0.18f, w, backCrest - h * 0.02f)
                lineTo(w, h)
                close()
            },
            color = hillBack,
        )

        val midCrest = h * (0.70f - (seed % 2) * 0.04f)
        drawPath(
            path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, midCrest + h * 0.06f)
                quadraticTo(w * 0.34f, midCrest - h * 0.16f, w * 0.66f, midCrest + h * 0.06f)
                quadraticTo(w * 0.86f, midCrest + h * 0.16f, w, midCrest)
                lineTo(w, h)
                close()
            },
            color = hillMid,
        )

        drawPath(
            path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.86f)
                quadraticTo(w * 0.42f, h * 0.74f, w, h * 0.90f)
                lineTo(w, h)
                close()
            },
            color = hillFront,
        )

        // A pale road catching the light on the front hill.
        drawPath(
            path = Path().apply {
                moveTo(w * 0.36f, h)
                quadraticTo(w * 0.48f, h * 0.93f, w * 0.44f, h * 0.86f)
            },
            color = skyTop.copy(alpha = 0.55f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = h * 0.035f),
        )
    }
}

/** Flat illustration used by the empty states — nothing here yet, but not nothing to look at. */
@Composable
fun EmptyStateIllustration(
    modifier: Modifier = Modifier,
) {
    val wayside = LocalWaysideColors.current
    val ink = if (wayside.isDark) Color(0xFFF5EEE1) else Color(0xFF14262B)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = h * 0.022f)

        drawCircle(
            color = wayside.accent.copy(alpha = 0.18f),
            radius = h * 0.30f,
            center = Offset(w * 0.5f, h * 0.42f),
        )
        // A road that runs out.
        drawPath(
            path = Path().apply {
                moveTo(w * 0.18f, h * 0.86f)
                quadraticTo(w * 0.42f, h * 0.70f, w * 0.50f, h * 0.48f)
                quadraticTo(w * 0.58f, h * 0.28f, w * 0.80f, h * 0.22f)
            },
            color = ink.copy(alpha = 0.22f),
            style = stroke,
        )
        drawCircle(
            color = ink.copy(alpha = 0.30f),
            radius = h * 0.045f,
            center = Offset(w * 0.18f, h * 0.86f),
        )
        drawRect(
            color = wayside.accent,
            topLeft = Offset(w * 0.74f, h * 0.16f),
            size = Size(h * 0.09f, h * 0.09f),
        )
    }
}
