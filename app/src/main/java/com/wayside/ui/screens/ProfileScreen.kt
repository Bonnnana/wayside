package com.wayside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayside.data.INTERESTS
import com.wayside.data.Units
import com.wayside.ui.WaysideUiState
import com.wayside.ui.components.DetourSegmentedSelector
import com.wayside.ui.components.WaysideButton
import com.wayside.ui.components.WaysideButtonStyle
import com.wayside.ui.components.WaysideChip
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    state: WaysideUiState,
    onToggleInterest: (String) -> Unit,
    onBudgetChange: (Int) -> Unit,
    onUnitsChange: (Units) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSignOut: () -> Unit,
    bottomPadding: Dp,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "MB",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onPrimary,
                )
            }
            Spacer(Modifier.size(14.dp))
            Column {
                Text(
                    text = "Marta Bento",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "12 detours taken · 47 places discovered",
                    style = MaterialTheme.typography.labelSmall,
                    color = wayside.textMuted,
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        SettingsSection(title = "Interests") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                INTERESTS.forEach { interest ->
                    WaysideChip(
                        label = interest,
                        selected = interest in state.interests,
                        onClick = { onToggleInterest(interest) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SettingsSection(
            title = "Default detour budget",
            caption = "New routes start at +${state.budget} min — " +
                "${com.wayside.data.Route.budgetHint(state.budget)}.",
        ) {
            DetourSegmentedSelector(
                budget = state.budget,
                onBudgetChange = onBudgetChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsSection(title = "Units") {
            OptionSegmentedRow(
                options = Units.entries.map { it.label },
                selectedIndex = Units.entries.indexOf(state.units),
                onSelect = { onUnitsChange(Units.entries[it]) },
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsSection(title = "Theme") {
            OptionSegmentedRow(
                options = ThemeMode.entries.map { it.label },
                selectedIndex = ThemeMode.entries.indexOf(state.themeMode),
                onSelect = { onThemeModeChange(ThemeMode.entries[it]) },
            )
        }

        Spacer(Modifier.height(24.dp))
        WaysideButton(
            text = "Sign out",
            onClick = onSignOut,
            style = WaysideButtonStyle.Ghost,
            contentColorOverride = scheme.error,
        )
        Spacer(Modifier.height(bottomPadding + 24.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    caption: String? = null,
    content: @Composable () -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(scheme.surface)
            .border(1.dp, wayside.hairline, MaterialTheme.shapes.large)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurface,
        )
        if (caption != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionSegmentedRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = scheme.primary,
                    activeContentColor = scheme.onPrimary,
                    activeBorderColor = scheme.primary,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = wayside.textMuted,
                    inactiveBorderColor = wayside.hairline,
                ),
                icon = {},
                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}

@Preview(name = "Profile · light", showBackground = true, device = "id:pixel_7")
@Composable
private fun ProfileLightPreview() {
    WaysideTheme(ThemeMode.Light) {
        ProfileScreen(
            state = WaysideUiState(),
            onToggleInterest = {},
            onBudgetChange = {},
            onUnitsChange = {},
            onThemeModeChange = {},
            onSignOut = {},
            bottomPadding = 80.dp,
        )
    }
}

@Preview(name = "Profile · dark", showBackground = true, device = "id:pixel_7")
@Composable
private fun ProfileDarkPreview() {
    WaysideTheme(ThemeMode.Dark) {
        ProfileScreen(
            state = WaysideUiState(themeMode = ThemeMode.Dark, units = Units.Miles, budget = 45),
            onToggleInterest = {},
            onBudgetChange = {},
            onUnitsChange = {},
            onThemeModeChange = {},
            onSignOut = {},
            bottomPadding = 80.dp,
        )
    }
}
