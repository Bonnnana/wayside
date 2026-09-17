package com.wayside.features.route

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wayside.ui.components.WaysideButton
import com.wayside.ui.components.WaysideButtonStyle
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

/**
 * Shown while the driver is off the plan and heading for a stop.
 *
 * It exists to answer two questions without tapping anything: where am I going, and how do I
 * get back on course.
 */
@Composable
fun HeadingForBanner(
    headingFor: String,
    minutes: Int?,
    isReRouting: Boolean,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, MaterialTheme.shapes.large, clip = false)
            .clip(MaterialTheme.shapes.large)
            .background(scheme.surface)
            .border(1.dp, wayside.hairline, MaterialTheme.shapes.large)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isReRouting) "Rerouting…" else "Heading for",
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = headingFor,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
            )
            if (minutes != null && !isReRouting) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$minutes min from here",
                    style = MaterialTheme.typography.labelSmall,
                    color = wayside.textMuted,
                )
            }
        }

        Spacer(Modifier.width(10.dp))
        WaysideButton(
            text = "Finish",
            onClick = onFinish,
            style = WaysideButtonStyle.Secondary,
            height = 40.dp,
            horizontalPadding = 18.dp,
        )
    }
}

@Preview(name = "Heading for", showBackground = true)
@Composable
private fun HeadingForBannerPreview() {
    WaysideTheme(ThemeMode.Light) {
        HeadingForBanner(
            headingFor = "Pastéis de Belém",
            minutes = 11,
            isReRouting = false,
            onFinish = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
