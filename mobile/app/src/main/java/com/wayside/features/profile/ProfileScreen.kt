package com.wayside.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayside.data.INTERESTS
import com.wayside.models.api.auth.UserResponse
import com.wayside.ui.WaysideUiState
import com.wayside.ui.components.CircleIconButton
import com.wayside.ui.components.DetourSegmentedSelector
import com.wayside.ui.components.WaysideChip
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel, bottomPadding: Dp) {
    val state by viewModel.state.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    ProfileContent(
        state = state,
        user = currentUser,
        onEditProfile = viewModel::onEditProfile,
        onToggleInterest = viewModel::onToggleInterest,
        onBudgetChange = viewModel::onBudgetChange,
        onThemeModeChange = viewModel::onThemeModeChange,
        onOpenSaved = viewModel::onOpenSaved,
        onSignOut = viewModel::onSignOut,
        bottomPadding = bottomPadding,
    )
}

@Composable
private fun ProfileContent(
    state: WaysideUiState,
    user: UserResponse?,
    onEditProfile: () -> Unit,
    onToggleInterest: (String) -> Unit,
    onBudgetChange: (Int) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenSaved: () -> Unit,
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
        val fullName = user?.fullName?.takeIf { it.isNotBlank() } ?: "Your account"
        val initials = fullName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }

        Spacer(Modifier.height(20.dp))

        // A banner-style header, set off from the body by its own tint, with a real name and
        // email pulled straight from the signed-in account rather than placeholder copy.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(scheme.primary.copy(alpha = 0.08f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onPrimary,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = user?.email ?: "Signed out",
                    style = MaterialTheme.typography.labelSmall,
                    color = wayside.textMuted,
                )
            }
            CircleIconButton(
                icon = Icons.Rounded.Edit,
                contentDescription = "Edit profile",
                onClick = onEditProfile,
                size = 36.dp,
                tint = wayside.textMuted,
                container = scheme.surface,
            )
        }

        Spacer(Modifier.height(20.dp))
        SettingsSection(
            icon = Icons.Rounded.GridView,
            title = "Your interests",
            headerValue = "${state.interests.size} selected",
            caption = "These rank what surfaces along your routes.",
        ) {
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
            icon = Icons.Rounded.Schedule,
            title = "Default detour budget",
            headerValue = "+${state.budget} min",
            headerValueColor = wayside.accent,
            caption = "Preselected every time you plan a route.",
        ) {
            DetourSegmentedSelector(
                budget = state.budget,
                onBudgetChange = onBudgetChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))
        InlineSettingRow(icon = Icons.Rounded.DarkMode, title = "Theme") {
            ThemeMode.entries.forEach { mode ->
                WaysideChip(
                    label = mode.label,
                    selected = state.themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(scheme.surface)
                .border(1.dp, wayside.hairline, MaterialTheme.shapes.large),
        ) {
            SettingsRow(
                icon = Icons.Rounded.Bookmark,
                label = "Saved places",
                onClick = onOpenSaved,
            )
            HorizontalDivider(color = wayside.hairline, thickness = 1.dp)
            SettingsRow(
                icon = Icons.Rounded.Logout,
                label = "Sign out",
                onClick = onSignOut,
                tint = scheme.error,
            )
        }

        Spacer(Modifier.height(bottomPadding + 24.dp))
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color? = null,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    val iconTint = tint ?: wayside.textMuted
    val labelTint = tint ?: scheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = labelTint,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    headerValue: String? = null,
    headerValueColor: Color? = null,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = wayside.textMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (headerValue != null) {
                Text(
                    text = headerValue,
                    style = MaterialTheme.typography.labelLarge,
                    color = headerValueColor ?: wayside.textMuted,
                )
            }
        }
        if (caption != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
                modifier = Modifier.padding(start = 26.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/**
 * The compact "icon + label + inline pills" row the design uses for Theme — a single line
 * rather than [SettingsSection]'s two-tier card, since there's nothing else to say about it.
 */
@Composable
private fun InlineSettingRow(
    icon: ImageVector,
    title: String,
    options: @Composable RowScope.() -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(scheme.surface)
            .border(1.dp, wayside.hairline, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = wayside.textMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options()
        }
    }
}

@Preview(name = "Profile · light", showBackground = true, device = "id:pixel_7")
@Composable
private fun ProfileLightPreview() {
    WaysideTheme(ThemeMode.Light) {
        ProfileContent(
            state = WaysideUiState(),
            user = UserResponse(id = "1", email = "marta@example.com", firstName = "Marta", lastName = "Bento"),
            onEditProfile = {},
            onToggleInterest = {},
            onBudgetChange = {},
            onThemeModeChange = {},
            onOpenSaved = {},
            onSignOut = {},
            bottomPadding = 80.dp,
        )
    }
}

@Preview(name = "Profile · dark", showBackground = true, device = "id:pixel_7")
@Composable
private fun ProfileDarkPreview() {
    WaysideTheme(ThemeMode.Dark) {
        ProfileContent(
            state = WaysideUiState(themeMode = ThemeMode.Dark, budget = 45),
            user = UserResponse(id = "1", email = "marta@example.com", firstName = "Marta", lastName = "Bento"),
            onEditProfile = {},
            onToggleInterest = {},
            onBudgetChange = {},
            onThemeModeChange = {},
            onOpenSaved = {},
            onSignOut = {},
            bottomPadding = 80.dp,
        )
    }
}
