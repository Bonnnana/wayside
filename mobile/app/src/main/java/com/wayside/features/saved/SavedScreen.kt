package com.wayside.features.saved

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayside.data.PlaceCategory
import com.wayside.data.SAVED_FILTERS
import com.wayside.data.SAMPLE_PLACES
import com.wayside.ui.WaysideUiState
import com.wayside.ui.savedPlaces
import com.wayside.ui.components.EmptyStateIllustration
import com.wayside.ui.components.SavedPlaceCard
import com.wayside.ui.components.WaysideChip
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

private fun categoryForFilter(filter: String): PlaceCategory? = when (filter) {
    "Food" -> PlaceCategory.Food
    "Nature" -> PlaceCategory.Nature
    "Culture" -> PlaceCategory.Culture
    "Viewpoints" -> PlaceCategory.Viewpoints
    "Hidden gems" -> PlaceCategory.HiddenGem
    else -> null
}

@Composable
fun SavedScreen(viewModel: SavedViewModel, bottomPadding: Dp) {
    val state by viewModel.state.collectAsState()

    SavedContent(
        state = state,
        onFilterChange = viewModel::onFilterChange,
        onUnsave = viewModel::onUnsave,
        onOpenPlace = viewModel::onOpenPlace,
        photoUrl = viewModel::photoUrl,
        bottomPadding = bottomPadding,
    )
}

@Composable
private fun SavedContent(
    state: WaysideUiState,
    onFilterChange: (String) -> Unit,
    onUnsave: (String) -> Unit,
    onOpenPlace: (String) -> Unit,
    photoUrl: (String, Int) -> String,
    bottomPadding: Dp,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    val saved = state.savedPlaces
    val category = categoryForFilter(state.savedFilter)
    val visible = saved.filter { category == null || it.category == category }

    Column(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Saved places",
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${saved.size} ${if (saved.size == 1) "place" else "places"}",
                style = MaterialTheme.typography.labelLarge,
                color = wayside.textMuted,
            )
        }

        if (saved.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                EmptyStateIllustration(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(140.dp),
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Nothing saved yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap the bookmark on a place and it will be waiting here the next " +
                        "time you pass nearby.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = wayside.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.primary.copy(alpha = 0.08f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Bookmarks,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = "We'll suggest these first whenever they're near your route.",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onBackground,
            )
        }
        Spacer(Modifier.height(14.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SAVED_FILTERS, key = { it }) { filter ->
                WaysideChip(
                    label = filter,
                    selected = state.savedFilter == filter,
                    onClick = { onFilterChange(filter) },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = bottomPadding + 20.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(visible, key = { it.id }) { place ->
                SavedPlaceCard(
                    place = place,
                    detourLabel = "+${place.detourMinutes} min from today's route",
                    onClick = { onOpenPlace(place.id) },
                    onUnsave = { onUnsave(place.id) },
                    photoUrl = photoUrl,
                )
            }
        }
    }
}

@Preview(name = "Saved · light", showBackground = true, device = "id:pixel_7")
@Composable
private fun SavedLightPreview() {
    WaysideTheme(ThemeMode.Light) {
        SavedContent(
            state = WaysideUiState(
                places = SAMPLE_PLACES,
                savedIds = setOf("queluz", "peninha", "sacolinha", "poetas"),
            ),
            onFilterChange = {},
            onUnsave = {},
            onOpenPlace = {},
            photoUrl = { _, _ -> "" },
            bottomPadding = 80.dp,
        )
    }
}

@Preview(name = "Saved · dark", showBackground = true, device = "id:pixel_7")
@Composable
private fun SavedDarkPreview() {
    WaysideTheme(ThemeMode.Dark) {
        SavedContent(
            state = WaysideUiState(
                places = SAMPLE_PLACES,
                savedIds = setOf("capuchos", "sabuga", "agualva"),
            ),
            onFilterChange = {},
            onUnsave = {},
            onOpenPlace = {},
            photoUrl = { _, _ -> "" },
            bottomPadding = 80.dp,
        )
    }
}

@Preview(name = "Saved · empty", showBackground = true, device = "id:pixel_7")
@Composable
private fun SavedEmptyPreview() {
    WaysideTheme(ThemeMode.Light) {
        SavedContent(
            state = WaysideUiState(places = SAMPLE_PLACES, savedIds = emptySet()),
            onFilterChange = {},
            onUnsave = {},
            onOpenPlace = {},
            photoUrl = { _, _ -> "" },
            bottomPadding = 80.dp,
        )
    }
}
