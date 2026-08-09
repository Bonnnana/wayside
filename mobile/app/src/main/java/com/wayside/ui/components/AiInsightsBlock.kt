package com.wayside.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wayside.data.ReviewTheme
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.WaysideMotion
import kotlinx.coroutines.delay

private const val ANALYSING_MILLIS = 1400L

/**
 * Shows a shimmer skeleton while the summary "arrives", then cross-fades to the
 * real content. There is no model behind it — the delay is local state only.
 */
@Composable
fun AiInsightsBlock(
    summary: String,
    tags: List<String>,
    reviewThemes: List<ReviewTheme>,
    modifier: Modifier = Modifier,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    var ready by remember(summary) { mutableStateOf(false) }
    LaunchedEffect(summary) {
        ready = false
        delay(ANALYSING_MILLIS)
        ready = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.primary.copy(alpha = 0.08f))
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = if (ready) "What people actually say" else "Analyzing reviews…",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.primary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Crossfade(
            targetState = ready,
            animationSpec = WaysideMotion.tweenSlow(),
            label = "aiInsights",
        ) { isReady ->
            if (isReady) {
                Column {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { tag ->
                            Box(
                                Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(scheme.surface)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = wayside.textMuted,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Top review themes",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    val max = reviewThemes.maxOfOrNull { it.count } ?: 1
                    reviewThemes.forEach { theme ->
                        ReviewThemeBar(theme, max)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            } else {
                ShimmerSkeleton()
            }
        }
    }
}

@Composable
private fun ReviewThemeBar(theme: ReviewTheme, max: Int) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = theme.label,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = theme.count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(scheme.onSurface.copy(alpha = 0.08f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(theme.count.toFloat() / max.toFloat())
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(scheme.primary),
            )
        }
    }
}

@Composable
private fun ShimmerSkeleton() {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerProgress",
    )
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        listOf(1f, 0.92f, 0.64f).forEachIndexed { index, fraction ->
            val alpha = 0.10f + 0.14f * ((progress + index * 0.22f) % 1f)
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(13.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(scheme.onSurface.copy(alpha = alpha)),
            )
        }
    }
}
