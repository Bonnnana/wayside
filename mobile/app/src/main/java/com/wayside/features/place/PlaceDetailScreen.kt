package com.wayside.features.place

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wayside.data.Place
import com.wayside.data.Units
import com.wayside.data.formatDistance
import com.wayside.data.placeById
import com.wayside.ui.components.AiInsightsBlock
import com.wayside.ui.components.BookmarkToggle
import com.wayside.ui.components.CategoryChip
import com.wayside.ui.components.CircleIconButton
import com.wayside.ui.components.DetourBadge
import com.wayside.ui.components.PlacePhotoPager
import com.wayside.ui.components.RatingLine
import com.wayside.ui.components.WaysideButton
import com.wayside.ui.components.WaysideButtonStyle
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

@Composable
fun PlaceDetailScreen(viewModel: PlaceDetailViewModel) {
    val state by viewModel.uiState.collectAsState()
    val appState by viewModel.appState.collectAsState()
    val place = state.place

    when {
        // The summary from the suggestion list stands in until the detail lands, so opening
        // a card never flashes an empty screen.
        place != null -> PlaceDetailContent(
            place = place,
            photoUrl = viewModel::photoUrl,
            saved = place.id in appState.savedIds,
            added = place.id in appState.stops,
            units = appState.units,
            onBack = viewModel::onBack,
            onToggleSaved = viewModel::onToggleSaved,
            onToggleStop = viewModel::onToggleStop,
        )

        state.isLoading -> PlaceDetailLoading()

        else -> PlaceDetailError(
            message = state.error ?: "Couldn't load this place.",
            onRetry = viewModel::onRetry,
        )
    }
}

@Composable
private fun PlaceDetailContent(
    place: Place,
    photoUrl: (String, Int) -> String,
    saved: Boolean,
    added: Boolean,
    units: Units,
    onBack: () -> Unit,
    onToggleSaved: () -> Unit,
    onToggleStop: () -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Box {
                PlacePhotoPager(
                    placeId = place.id,
                    photoCount = place.photoCount,
                    photoUrl = photoUrl,
                    placeholderColor = place.category.color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    CircleIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                    Spacer(Modifier.weight(1f))
                    BookmarkToggle(saved = saved, onClick = onToggleSaved)
                }
                DetourBadge(
                    minutes = place.detourMinutes,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                )
            }

            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CategoryChip(place.category)
                    Text(
                        text = "${formatDistance(place.distanceKm, units)} off route",
                        style = MaterialTheme.typography.labelSmall,
                        color = wayside.textMuted,
                    )
                }
                Spacer(Modifier.height(10.dp))
                RatingLine(
                    rating = place.rating,
                    ratingCount = place.ratingCount,
                    trailing = "open until ${place.openUntil}",
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = place.highlight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(20.dp))
                AiInsightsBlock(
                    summary = place.aiSummary,
                    tags = place.tags,
                    reviewThemes = place.reviewThemes,
                )
                Spacer(Modifier.height(140.dp))
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(scheme.surface)
                .navigationBarsPadding(),
        ) {
            HorizontalDivider(color = wayside.hairline)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Saving lives solely in the BookmarkToggle up in the header now — this bar is
                // just the route action.
                WaysideButton(
                    text = if (added) {
                        "Remove from route"
                    } else {
                        "Add to route · +${place.detourMinutes} min"
                    },
                    onClick = onToggleStop,
                    style = if (added) WaysideButtonStyle.Danger else WaysideButtonStyle.Cta,
                    modifier = Modifier.weight(1f),
                    horizontalPadding = 12.dp,
                )
            }
        }
    }
}

@Preview(name = "Place detail · light", showBackground = true, device = "id:pixel_7")
@Composable
private fun PlaceDetailLightPreview() {
    WaysideTheme(ThemeMode.Light) {
        PlaceDetailContent(
            place = placeById("queluz"),
            photoUrl = { _, _ -> "" },
            saved = true,
            added = false,
            units = Units.Kilometres,
            onBack = {},
            onToggleSaved = {},
            onToggleStop = {},
        )
    }
}

@Preview(name = "Place detail · dark", showBackground = true, device = "id:pixel_7")
@Composable
private fun PlaceDetailDarkPreview() {
    WaysideTheme(ThemeMode.Dark) {
        PlaceDetailContent(
            place = placeById("peninha"),
            photoUrl = { _, _ -> "" },
            saved = false,
            added = true,
            units = Units.Kilometres,
            onBack = {},
            onToggleSaved = {},
            onToggleStop = {},
        )
    }
}
