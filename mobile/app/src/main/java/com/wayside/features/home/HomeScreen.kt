package com.wayside.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.data.PlaceCategory
import com.wayside.data.SAMPLE_PLACES
import com.wayside.ui.WaysideUiState
import com.wayside.ui.drawnRoutePoints
import com.wayside.ui.hasRoute
import com.wayside.ui.placesWithinBudget
import com.wayside.ui.components.MapFilterChip
import com.wayside.ui.components.WaysideMap
import com.wayside.ui.components.WaysideSearchBar
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

private val HOME_FILTERS = listOf(
    "All" to null,
    "Food" to PlaceCategory.Food,
    "Nature" to PlaceCategory.Nature,
    "Culture" to PlaceCategory.Culture,
    "Viewpoints" to PlaceCategory.Viewpoints,
    "Hidden gems" to PlaceCategory.HiddenGem,
)

@Composable
fun HomeScreen(viewModel: HomeViewModel, bottomPadding: Dp) {
    val state by viewModel.state.collectAsState()

    HomeContent(
        state = state,
        onPlaceClick = viewModel::onPlaceClick,
        onPlanDetour = viewModel::onPlanDetour,
        onSearch = viewModel::onSearch,
        onRetry = viewModel::onRetry,
        bottomPadding = bottomPadding,
    )
}

@Composable
private fun HomeContent(
    state: WaysideUiState,
    onPlaceClick: (String) -> Unit,
    onPlanDetour: () -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    bottomPadding: Dp,
) {
    var filter by remember { mutableStateOf("All") }
    val activeCategory = HOME_FILTERS.firstOrNull { it.first == filter }?.second

    Box(Modifier.fillMaxSize()) {
        // Only the places matching the chip filter are drawn; a real map has no equivalent
        // of fading a pin out in place, so the marker simply isn't there.
        val visible = remember(state.places, state.budget, activeCategory) {
            state.placesWithinBudget.filter { activeCategory == null || it.category == activeCategory }
        }

        WaysideMap(
            places = visible,
            modifier = Modifier.fillMaxSize(),
            selectedPlaceId = state.selectedPinId,
            routePoints = state.drawnRoutePoints,
            originPoint = state.originPoint,
            destinationPoint = state.destinationPoint,
            showMyLocation = state.hasLocationPermission,
            // The search bar and the category chips sit over the top of the map. The recentre
            // button lives at the bottom, level with "Plan a detour" — the Saved shortcut that
            // used to sit next to it is gone now that Saved is a bottom-nav tab.
            topContentPadding = 112.dp,
            bottomContentPadding = bottomPadding + 18.dp,
            myLocation = state.currentLocation,
            onPlaceClick = onPlaceClick,
        )

        Column(
            Modifier
                .statusBarsPadding()
                .padding(top = 8.dp),
        ) {
            WaysideSearchBar(
                placeholder = "Where to?",
                onClick = onSearch,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(HOME_FILTERS, key = { it.first }) { (label, _) ->
                    MapFilterChip(
                        label = label,
                        selected = filter == label,
                        onClick = { filter = label },
                    )
                }
            }

            // A map with no markers is ambiguous — still loading, nothing nearby, or the
            // request failed. Say which.
            val status = when {
                // Nothing to say when no drive is planned: the search bar above already asks.
                !state.hasRoute -> null
                state.placesError != null -> state.placesError
                state.isLoadingPlaces && state.places.isEmpty() -> "Finding places along the drive…"
                else -> null
            }

            if (status != null) {
                Spacer(Modifier.height(12.dp))
                MapStatusBanner(
                    message = status,
                    onRetry = if (state.placesError != null) onRetry else null,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = onPlanDetour,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = bottomPadding + 18.dp)
                .padding(start = 20.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.AltRoute, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text("Plan a detour", style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Sits over the map, so it carries its own surface rather than relying on the background. */
@Composable
private fun MapStatusBanner(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(scheme.surface)
            .border(1.dp, wayside.hairline, MaterialTheme.shapes.large)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelMedium,
            color = wayside.textMuted,
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null) {
            Text(
                text = "Try again",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.primary,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Preview(name = "Home · light", showBackground = true, device = "id:pixel_7")
@Composable
private fun HomeScreenLightPreview() {
    WaysideTheme(ThemeMode.Light) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            HomeContent(
                state = WaysideUiState(places = SAMPLE_PLACES),
                onPlaceClick = {},
                onPlanDetour = {},
                onSearch = {},
                onRetry = {},
                bottomPadding = 80.dp,
            )
        }
    }
}

@Preview(name = "Home · dark", showBackground = true, device = "id:pixel_7")
@Composable
private fun HomeScreenDarkPreview() {
    WaysideTheme(ThemeMode.Dark) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            HomeContent(
                state = WaysideUiState(places = SAMPLE_PLACES, selectedPinId = "queluz"),
                onPlaceClick = {},
                onPlanDetour = {},
                onSearch = {},
                onRetry = {},
                bottomPadding = 80.dp,
            )
        }
    }
}
