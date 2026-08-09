package com.wayside.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayside.data.PlaceCategory
import com.wayside.data.SAMPLE_PLACES
import com.wayside.ui.WaysideUiState
import com.wayside.ui.components.MapAnchor
import com.wayside.ui.components.MapFilterChip
import com.wayside.ui.components.MapPin
import com.wayside.ui.components.StylizedMap
import com.wayside.ui.components.UserLocationDot
import com.wayside.ui.components.WaysideSearchBar
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideMotion
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
fun HomeScreen(
    state: WaysideUiState,
    onPlaceClick: (String) -> Unit,
    onPlanDetour: () -> Unit,
    onOpenSaved: () -> Unit,
    onSearch: () -> Unit,
    bottomPadding: Dp,
) {
    var filter by remember { mutableStateOf("All") }
    val activeCategory = HOME_FILTERS.firstOrNull { it.first == filter }?.second

    val pulseTransition = rememberInfiniteTransition(label = "userDot")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "userDotPulse",
    )

    Box(Modifier.fillMaxSize()) {
        StylizedMap(Modifier.fillMaxSize()) { mapWidth, mapHeight ->
            MapAnchor(
                x = 0.15f,
                y = 0.76f,
                mapWidth = mapWidth,
                mapHeight = mapHeight,
                anchorWidth = 56.dp,
                anchorHeight = 28.dp,
            ) {
                UserLocationDot(pulse = pulse)
            }
            SAMPLE_PLACES.forEach { place ->
                val visible = activeCategory == null || place.category == activeCategory
                MapAnchor(
                    x = place.mapX,
                    y = place.mapY,
                    mapWidth = mapWidth,
                    mapHeight = mapHeight,
                    anchorWidth = 30.dp,
                    anchorHeight = 40.dp,
                ) {
                    FadingPin(visible = visible) {
                        MapPin(
                            color = place.category.color,
                            saved = place.id in state.savedIds,
                            selected = state.selectedPinId == place.id,
                            onClick = if (visible) {
                                { onPlaceClick(place.id) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }

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
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 18.dp)
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ExtendedFloatingActionButton(
                onClick = onPlanDetour,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(Icons.AutoMirrored.Rounded.AltRoute, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text("Plan a detour", style = MaterialTheme.typography.titleSmall)
            }
            FloatingActionButton(
                onClick = onOpenSaved,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(Icons.Rounded.Bookmarks, contentDescription = "Saved places")
            }
        }
    }
}

@Composable
private fun FadingPin(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = WaysideMotion.tweenStandard(),
        label = "pinAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = WaysideMotion.tweenStandard(),
        label = "pinScale",
    )
    Box(
        Modifier.graphicsLayer {
            this.alpha = alpha
            scaleX = scale
            scaleY = scale
        },
    ) {
        content()
    }
}

@Preview(name = "Home · light", showBackground = true, device = "id:pixel_7")
@Composable
private fun HomeScreenLightPreview() {
    WaysideTheme(ThemeMode.Light) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            HomeScreen(
                state = WaysideUiState(),
                onPlaceClick = {},
                onPlanDetour = {},
                onOpenSaved = {},
                onSearch = {},
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
            HomeScreen(
                state = WaysideUiState(selectedPinId = "queluz"),
                onPlaceClick = {},
                onPlanDetour = {},
                onOpenSaved = {},
                onSearch = {},
                bottomPadding = 80.dp,
            )
        }
    }
}
