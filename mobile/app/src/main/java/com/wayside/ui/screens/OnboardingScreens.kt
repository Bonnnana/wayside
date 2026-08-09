package com.wayside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wayside.data.INTERESTS
import com.wayside.ui.components.WaysideButton
import com.wayside.ui.components.WaysideButtonStyle
import com.wayside.ui.components.WaysideChip
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

@Composable
fun OnboardingPermissionScreen(
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        PageIndicator(activeIndex = 1, count = 3)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(scheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.NearMe,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Wayside works along your route",
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "With your location we can tell which stops are genuinely on the way, " +
                "and how many minutes each one really costs you.",
            style = MaterialTheme.typography.bodyMedium,
            color = wayside.textMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Your location stays on this device. Nothing is shared.",
            style = MaterialTheme.typography.labelSmall,
            color = wayside.textMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        WaysideButton(
            text = "Allow location",
            onClick = onAllow,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        WaysideButton(
            text = "Not now",
            onClick = onNotNow,
            style = WaysideButtonStyle.Ghost,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingInterestsScreen(
    interests: Set<String>,
    onToggleInterest: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        PageIndicator(activeIndex = 2, count = 3)
        Spacer(Modifier.height(40.dp))
        Text(
            text = "What makes you pull over?",
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Pick a few. We'll lead with these when something fits your budget.",
            style = MaterialTheme.typography.bodyMedium,
            color = wayside.textMuted,
        )
        Spacer(Modifier.height(28.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            INTERESTS.forEach { interest ->
                WaysideChip(
                    label = interest,
                    selected = interest in interests,
                    onClick = { onToggleInterest(interest) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "${interests.size} selected",
            style = MaterialTheme.typography.labelSmall,
            color = wayside.textMuted,
        )
        Spacer(Modifier.height(12.dp))
        WaysideButton(
            text = "Continue",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Preview(name = "Permission · light", showBackground = true, device = "id:pixel_7")
@Composable
private fun PermissionLightPreview() {
    WaysideTheme(ThemeMode.Light) {
        OnboardingPermissionScreen(onAllow = {}, onNotNow = {})
    }
}

@Preview(name = "Permission · dark", showBackground = true, device = "id:pixel_7")
@Composable
private fun PermissionDarkPreview() {
    WaysideTheme(ThemeMode.Dark) {
        OnboardingPermissionScreen(onAllow = {}, onNotNow = {})
    }
}

@Preview(name = "Interests · light", showBackground = true, device = "id:pixel_7")
@Composable
private fun InterestsLightPreview() {
    WaysideTheme(ThemeMode.Light) {
        OnboardingInterestsScreen(
            interests = setOf("Food", "Nature", "Viewpoints"),
            onToggleInterest = {},
            onContinue = {},
        )
    }
}

@Preview(name = "Interests · dark", showBackground = true, device = "id:pixel_7")
@Composable
private fun InterestsDarkPreview() {
    WaysideTheme(ThemeMode.Dark) {
        OnboardingInterestsScreen(
            interests = setOf("Coffee", "Markets"),
            onToggleInterest = {},
            onContinue = {},
        )
    }
}
