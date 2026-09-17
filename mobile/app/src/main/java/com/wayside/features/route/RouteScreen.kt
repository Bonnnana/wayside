package com.wayside.features.route

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayside.data.Place
import com.wayside.data.Route
import com.wayside.data.formatClock
import com.wayside.data.formatDistance
import com.wayside.data.SAMPLE_PLACES
import com.wayside.ui.WaysideUiState
import com.wayside.ui.drawnRoutePoints
import com.wayside.ui.originText
import com.wayside.ui.placesWithinBudget
import com.wayside.ui.stopsInOrder
import com.wayside.ui.fastestArrivalMinutes
import com.wayside.ui.totalDetourMinutes
import com.wayside.ui.components.CircleIconButton
import com.wayside.ui.components.DetourSegmentedSelector
import com.wayside.ui.components.EmptyStateIllustration
import com.wayside.ui.components.PlaceCard
import com.wayside.ui.components.SheetHeight
import com.wayside.ui.components.WaysideMap
import com.wayside.ui.components.WaysideBottomSheet
import com.wayside.ui.components.WaysideButton
import com.wayside.ui.components.WaysideButtonStyle
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideMotion
import com.wayside.ui.theme.WaysideTheme

private val SHEET_PEEK = 132.dp
private val SHEET_HALF = 380.dp
private val SHEET_EXPANDED = 540.dp

/**
 * Corridor stroke widths per budget. The spec calls for 12/70/150/214 px — expressed
 * here in dp so it animates with [animateDpAsState] and lands on those pixel widths
 * at the 3x density the design was drawn at.
 */
private fun corridorWidthFor(budget: Int): Dp = when (budget) {
    0 -> 4.dp
    15 -> 23.dp
    30 -> 50.dp
    else -> 71.dp
}

@Composable
fun RouteScreen(viewModel: RouteViewModel, bottomPadding: Dp) {
    val state by viewModel.state.collectAsState()

    RouteContent(
        state = state,
        photoUrl = viewModel::photoUrl,
        onBack = viewModel::onBack,
        onBudgetChange = viewModel::onBudgetChange,
        onSelectPin = viewModel::onSelectPin,
        onToggleStop = viewModel::onToggleStop,
        onOpenPlace = viewModel::onOpenPlace,
        onStartRoute = viewModel::onStartRoute,
        onSwapRoute = viewModel::onSwapRoute,
        onEditRoute = viewModel::onEditRoute,
        onMoveStop = viewModel::onMoveStop,
        onRemoveStop = viewModel::onRemoveStop,
        onRetry = viewModel::onRetry,
        bottomPadding = bottomPadding,
    )
}

@Composable
private fun RouteContent(
    state: WaysideUiState,
    photoUrl: (String, Int) -> String,
    onBack: () -> Unit,
    onBudgetChange: (Int) -> Unit,
    onSelectPin: (String?) -> Unit,
    onToggleStop: (String) -> Unit,
    onOpenPlace: (String) -> Unit,
    onStartRoute: () -> Unit,
    onSwapRoute: () -> Unit,
    onEditRoute: () -> Unit,
    onMoveStop: (String, Int) -> Unit,
    onRemoveStop: (String) -> Unit,
    onRetry: () -> Unit,
    bottomPadding: Dp,
) {
    // Opens at peek so the corridor and the pins are the first thing you see.
    var sheetHeight by remember { mutableStateOf(SheetHeight.Peek) }

    // Everything the API found is already here; the budget is a filter over it.
    val suggestions = remember(state.places, state.budget) { state.placesWithinBudget }
    val corridorWidth by animateDpAsState(
        targetValue = corridorWidthFor(state.budget),
        animationSpec = WaysideMotion.tweenSlow(),
        label = "corridorWidth",
    )
    val listState = rememberLazyListState()

    // Tapping a pin lifts the sheet far enough to show cards, then scrolls to the match.
    LaunchedEffect(state.selectedPinId, suggestions) {
        val index = suggestions.indexOfFirst { it.id == state.selectedPinId }
        if (index >= 0) {
            if (sheetHeight == SheetHeight.Peek) sheetHeight = SheetHeight.Half
            listState.animateScrollToItem(index)
        }
    }

    val sheetAnchor = when (sheetHeight) {
        SheetHeight.Peek -> SHEET_PEEK
        SheetHeight.Half -> SHEET_HALF
        SheetHeight.Expanded -> SHEET_EXPANDED
    }
    val ctaOffset by animateDpAsState(
        targetValue = sheetAnchor,
        animationSpec = WaysideMotion.tweenStandard(),
        label = "ctaOffset",
    )

    Box(Modifier.fillMaxSize()) {
        WaysideMap(
            places = suggestions,
            modifier = Modifier.fillMaxSize(),
            selectedPlaceId = state.selectedPinId,
            corridorWidth = corridorWidth,
            routePoints = state.drawnRoutePoints,
            originPoint = state.originPoint,
            destinationPoint = state.destinationPoint,
            showMyLocation = state.hasLocationPermission,
            // The route card and the stops panel sit over the top of the map.
            topContentPadding = 210.dp,
            bottomContentPadding = SHEET_PEEK,
            myLocation = state.currentLocation,
            onPlaceClick = { id -> onSelectPin(id) },
            onMapClick = { onSelectPin(null) },
        )

        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            RoutePlanCard(
                origin = state.originText,
                destination = state.destination,
                onBack = onBack,
                onSwap = onSwapRoute,
                onEdit = onEditRoute,
            )
            Spacer(Modifier.height(10.dp))
            DetourBudgetCard(
                budget = state.budget,
                onBudgetChange = onBudgetChange,
                units = state.units,
                driveMinutes = state.driveMinutes,
                distanceKm = state.distanceKm,
                fastestArrival = state.fastestArrivalMinutes,
            )
        }

        WaysideBottomSheet(
            sheetHeight = sheetHeight,
            onSheetHeightChange = { sheetHeight = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding),
            peek = SHEET_PEEK,
            half = SHEET_HALF,
            expanded = SHEET_EXPANDED,
        ) {
            SuggestionsSheetContent(
                suggestions = suggestions,
                photoUrl = photoUrl,
                isLoading = state.isLoadingPlaces,
                error = state.placesError,
                onRetry = onRetry,
                budget = state.budget,
                stops = state.stops,
                selectedPinId = state.selectedPinId,
                listState = listState,
                onCardClick = onOpenPlace,
                onToggleStop = onToggleStop,
                onSelectPin = onSelectPin,
                onTryFifteen = { onBudgetChange(15) },
            )
        }

        RouteStopsPanel(
            origin = state.originText,
            destination = state.destination,
            stops = state.stopsInOrder,
            isRouting = state.isRoutingStops,
            onMoveStop = onMoveStop,
            onRemoveStop = onRemoveStop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 132.dp, start = 16.dp, end = 16.dp),
        )

        val stopCount = state.stops.size
        AnimatedVisibility(
            visible = stopCount > 0,
            enter = fadeIn(WaysideMotion.tweenStandard()) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(WaysideMotion.tweenQuick()) + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + ctaOffset + 14.dp),
        ) {
            WaysideButton(
                onClick = onStartRoute,
                style = WaysideButtonStyle.Cta,
                modifier = Modifier.shadow(10.dp, MaterialTheme.shapes.extraLarge, clip = false),
                height = 50.dp,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Start route · $stopCount ${if (stopCount == 1) "stop" else "stops"} · " +
                        "+${state.totalDetourMinutes} min",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun RoutePlanCard(
    origin: String,
    destination: String,
    onBack: () -> Unit,
    onSwap: () -> Unit,
    onEdit: () -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, MaterialTheme.shapes.large, clip = false)
            .clip(MaterialTheme.shapes.large)
            .background(scheme.surface)
            .border(1.dp, wayside.hairline, MaterialTheme.shapes.large)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
            size = 36.dp,
            container = scheme.surfaceContainerHigh,
        )
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onEdit),
        ) {
            RouteField(
                label = "From",
                value = origin.ifBlank { "Where from?" },
                dotColor = scheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            RouteField(
                label = "To",
                value = destination.ifBlank { "Where to?" },
                dotColor = wayside.accent,
            )
        }
        Spacer(Modifier.width(8.dp))
        CircleIconButton(
            icon = Icons.Rounded.SwapVert,
            contentDescription = "Swap start and destination",
            onClick = onSwap,
            size = 36.dp,
            tint = scheme.primary,
            container = scheme.surfaceContainerHigh,
        )
    }
}

@Composable
private fun RouteField(
    label: String,
    value: String,
    dotColor: androidx.compose.ui.graphics.Color,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
            )
        }
    }
}

@Composable
private fun DetourBudgetCard(
    budget: Int,
    onBudgetChange: (Int) -> Unit,
    units: com.wayside.data.Units,
    driveMinutes: Int,
    distanceKm: Double,
    fastestArrival: Int,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, MaterialTheme.shapes.large, clip = false)
            .clip(MaterialTheme.shapes.large)
            .background(scheme.surface)
            .border(1.dp, wayside.hairline, MaterialTheme.shapes.large)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Detour budget",
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Route.budgetHint(budget),
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
                textAlign = TextAlign.End,
            )
        }
        Spacer(Modifier.height(10.dp))
        DetourSegmentedSelector(
            budget = budget,
            onBudgetChange = onBudgetChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        // Only once there is a drive: "0 min · 0.0 km" is worse than saying nothing.
        if (driveMinutes > 0) {
            Text(
                text = "Fastest route $driveMinutes min · " +
                    "${formatDistance(distanceKm, units)} · " +
                    "arrive ${formatClock(fastestArrival)}",
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
            )
        }
    }
}

@Composable
private fun SuggestionsSheetContent(
    suggestions: List<Place>,
    photoUrl: (String, Int) -> String,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    budget: Int,
    stops: List<String>,
    selectedPinId: String?,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onCardClick: (String) -> Unit,
    onToggleStop: (String) -> Unit,
    onSelectPin: (String?) -> Unit,
    onTryFifteen: () -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    // An empty list means three different things, and saying "nothing fits your budget" when
    // the request actually failed is the kind of lie that sends you looking in the wrong place.
    if (suggestions.isEmpty() && (isLoading || error != null)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (error == null) {
                CircularProgressIndicator(color = scheme.primary)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Finding places along the drive…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = wayside.textMuted,
                )
            } else {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = wayside.textMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                WaysideButton(
                    text = "Try again",
                    onClick = onRetry,
                    style = WaysideButtonStyle.Secondary,
                    height = 46.dp,
                )
            }
        }
        return
    }

    if (suggestions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EmptyStateIllustration(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(96.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Nothing fits a +$budget min budget",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Give the drive a quarter of an hour and four stops open up along " +
                    "this stretch.",
                style = MaterialTheme.typography.bodyMedium,
                color = wayside.textMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            if (budget < 15) {
                WaysideButton(
                    text = "Try +15 min",
                    onClick = onTryFifteen,
                    style = WaysideButtonStyle.Secondary,
                    height = 46.dp,
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${suggestions.size} stops worth a look",
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "within +$budget min",
            style = MaterialTheme.typography.labelSmall,
            color = wayside.textMuted,
        )
    }
    Spacer(Modifier.height(10.dp))
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(suggestions, key = { _, place -> place.id }) { _, place ->
            PlaceCard(
                place = place,
                added = place.id in stops,
                ringed = place.id == selectedPinId,
                onClick = {
                    onSelectPin(place.id)
                    onCardClick(place.id)
                },
                onToggleAdd = { onToggleStop(place.id) },
                photoUrl = photoUrl,
            )
        }
    }
}

@Preview(name = "Route · light", showBackground = true, device = "id:pixel_7")
@Composable
private fun RouteScreenLightPreview() {
    WaysideTheme(ThemeMode.Light) {
        RouteContent(
            state = WaysideUiState(
                places = SAMPLE_PLACES,
                budget = 15,
                selectedPinId = "queluz",
                stops = listOf("queluz"),
            ),
            photoUrl = { _, _ -> "" },
            onBack = {},
            onBudgetChange = {},
            onSelectPin = {},
            onToggleStop = {},
            onOpenPlace = {},
            onStartRoute = {},
            onSwapRoute = {},
            onEditRoute = {},
            onMoveStop = { _, _ -> },
            onRemoveStop = {},
            onRetry = {},
            bottomPadding = 80.dp,
        )
    }
}

@Preview(name = "Route · dark", showBackground = true, device = "id:pixel_7")
@Composable
private fun RouteScreenDarkPreview() {
    WaysideTheme(ThemeMode.Dark) {
        RouteContent(
            state = WaysideUiState(places = SAMPLE_PLACES, budget = 45),
            photoUrl = { _, _ -> "" },
            onBack = {},
            onBudgetChange = {},
            onSelectPin = {},
            onToggleStop = {},
            onOpenPlace = {},
            onStartRoute = {},
            onSwapRoute = {},
            onEditRoute = {},
            onMoveStop = { _, _ -> },
            onRemoveStop = {},
            onRetry = {},
            bottomPadding = 80.dp,
        )
    }
}

@Preview(name = "Route · +0 empty state", showBackground = true, device = "id:pixel_7")
@Composable
private fun RouteScreenEmptyPreview() {
    WaysideTheme(ThemeMode.Light) {
        RouteContent(
            state = WaysideUiState(places = SAMPLE_PLACES, budget = 0),
            photoUrl = { _, _ -> "" },
            onBack = {},
            onBudgetChange = {},
            onSelectPin = {},
            onToggleStop = {},
            onOpenPlace = {},
            onStartRoute = {},
            onSwapRoute = {},
            onEditRoute = {},
            onMoveStop = { _, _ -> },
            onRemoveStop = {},
            onRetry = {},
            bottomPadding = 80.dp,
        )
    }
}
