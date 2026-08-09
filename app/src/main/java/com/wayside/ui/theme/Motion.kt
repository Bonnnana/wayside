package com.wayside.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/** Single source of truth for Wayside motion: 200–300ms FastOutSlowIn everywhere. */
object WaysideMotion {
    const val Quick = 200
    const val Standard = 250
    const val Slow = 300

    fun <T> tweenQuick() = tween<T>(durationMillis = Quick, easing = FastOutSlowInEasing)
    fun <T> tweenStandard() = tween<T>(durationMillis = Standard, easing = FastOutSlowInEasing)
    fun <T> tweenSlow() = tween<T>(durationMillis = Slow, easing = FastOutSlowInEasing)

    /** Bottom sheets settle with a little bounce. */
    fun sheetSpring() = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )
}

/** Buttons and tappable cards scale to 0.97 while held. */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = WaysideMotion.tweenQuick(),
        label = "pressScale",
    )
    return this.scale(scale)
}
