package com.wayside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

/** The dots along the bottom of Welcome and onboarding. Shared, so it lives here. */
@Composable
fun PageIndicator(
    activeIndex: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val wayside = LocalWaysideColors.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val active = index == activeIndex
            Box(
                Modifier
                    .height(7.dp)
                    .width(if (active) 22.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) scheme.primary else wayside.textMuted.copy(alpha = 0.3f),
                    ),
            )
        }
    }
}

@Preview(name = "Page indicator", showBackground = true)
@Composable
private fun PageIndicatorPreview() {
    WaysideTheme(ThemeMode.Light) {
        PageIndicator(activeIndex = 1, count = 3)
    }
}
