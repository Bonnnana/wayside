package com.wayside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayside.ui.components.AppMark
import com.wayside.ui.components.WaysideButton
import com.wayside.ui.components.WaysideButtonStyle
import com.wayside.ui.components.WelcomeIllustration
import com.wayside.ui.components.scrimBrush
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize().background(scheme.background)) {
        WelcomeIllustration(Modifier.fillMaxSize())
        // Scrim only over the lower half, so the hills and the road stay readable above it.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.52f)
                .background(scrimBrush(scheme.background)),
        )

        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppMark(Modifier.size(30.dp).clip(CircleShape))
            Text(
                text = "Wayside",
                style = MaterialTheme.typography.displaySmall,
                color = scheme.onBackground,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = "Travel isn't just about arriving.",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                ),
                color = scheme.onBackground,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Wayside reads the road ahead and offers the stops worth the " +
                    "extra minutes. You decide how many you have.",
                style = MaterialTheme.typography.bodyMedium,
                color = wayside.textMuted,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Proof(icon = Icons.Rounded.Schedule, text = "+0 to +45 min")
                Proof(icon = Icons.Rounded.AutoAwesome, text = "Picked for you")
            }
            Spacer(Modifier.height(26.dp))
            PageIndicator(activeIndex = 0, count = 3)
            Spacer(Modifier.height(20.dp))
            WaysideButton(
                text = "Get started",
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            WaysideButton(
                text = "Skip",
                onClick = onSkip,
                style = WaysideButtonStyle.Ghost,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun Proof(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(17.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onBackground,
        )
    }
}

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
                    .background(if (active) scheme.primary else wayside.textMuted.copy(alpha = 0.3f)),
            )
        }
    }
}

@Preview(name = "Welcome · light", showBackground = true, device = "id:pixel_7")
@Composable
private fun WelcomeScreenLightPreview() {
    WaysideTheme(ThemeMode.Light) {
        WelcomeScreen(onGetStarted = {}, onSkip = {})
    }
}

@Preview(name = "Welcome · dark", showBackground = true, device = "id:pixel_7")
@Composable
private fun WelcomeScreenDarkPreview() {
    WaysideTheme(ThemeMode.Dark) {
        WelcomeScreen(onGetStarted = {}, onSkip = {})
    }
}
