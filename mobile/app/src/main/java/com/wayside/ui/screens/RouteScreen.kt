package com.wayside.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.wayside.data.SAMPLE_PLACES
import com.wayside.data.arrivalMinutes
import com.wayside.data.formatClock
import com.wayside.data.formatDistance
import com.wayside.data.totalDetourMinutes
import com.wayside.ui.WaysideUiState
import com.wayside.ui.components.CircleIconButton
import com.wayside.ui.components.DetourSegmentedSelector
import com.wayside.ui.components.EmptyStateIllustration
import com.wayside.ui.components.MapAnchor
import com.wayside.ui.components.MapPin
import com.wayside.ui.components.PlaceCard
import com.wayside.ui.components.SheetHeight
import com.wayside.ui.components.StylizedMap
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
fun RouteScreen(
    state: WaysideUiState,
    onBack: () -> Unit,
    onBudgetChange: (Int) -> Unit,
    onSelectPin: (String?) -> Unit,
    onToggleStop: (String) -> Unit,
    onOpenPlace: (String) -> Unit,
    onStartRoute: () -> Unit,
    bottomPadding: Dp,
) {
    var swapped by remember { mutableStateOf(false) }
    // Opens at peek so the corridor and the pins are the first thing you see.
    var sheetHeight by remember { mutableStateOf(SheetHeight.Peek) }

    val suggestions = remember(state.budget) {
        SAMPLE_PLACES.filter { it.detourMinutes <= state.budget }
    }
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
        StylizedMap(
            modifier = Modifier.fillMaxSize(),
            showRoute = true,
            corridorWidth = corridorWidth,
        ) { mapWidth, mapHeight ->
            SAMPLE_PLACES.forEach { place ->
                val fits = place.detourMinutes <= state.budget
                MapAnchor(
                    x = place.mapX,
                    y = place.mapY,
                    mapWidth = mapWidth,
                    mapHeight = mapHeight,
                    anchorWidth = 30.dp,
                    anchorHeight = 40.dp,
                ) {
                    AnimatedVisibility(
                        visible = fits,
                        enter = fadeIn(WaysideMotion.tweenStandard()) +
                            scaleIn(WaysideMotion.tweenStandard(), initialScale = 0.6f),
                        exit = fadeOut(WaysideMotion.tweenStandard()) +
                            scaleOut(WaysideMotion.tweenStandard(), targetScale = 0.6f),
                    ) {
                        MapPin(
                            color = place.category.color,
                            saved = place.id in state.savedIds,
                            selected = state.selectedPinId == place.id,
                            onClick = { onSelectPin(place.id) },
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            RoutePlanCard(
                origin = if (swapped) Route.DESTINATION else Route.ORIGIN,
                destination = if (swapped) Route.ORIGIN else Route.DESTINATION,
                onBack = onBack,
                onSwap = { swapped = !swapped },
            )
            Spacer(Modifier.height(10.dp))
            DetourBudgetCard(
                budget = state.budget,
                onBudgetChange = onBudgetChange,
                units = state.units,
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
                        "+${totalDetourMinutes(state.stops)} min",
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
        Column(Modifier.weight(1f)) {
            RouteField(label = "From", value = origin, dotColor = scheme.primary)
            Spacer(Modifier.height(8.dp))
            RouteField(label = "To", value = destination, dotColor = wayside.accent)
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
        Text(
            text = "Fastest route ${Route.DRIVE_MINUTES} min · " +
                "${formatDistance(Route.DISTANCE_KM, units)} · " +
                "arrive ${formatClock(arrivalMinutes(emptyList()))}",
            style = MaterialTheme.typography.labelSmall,
            color = wayside.textMuted,
        )
    }
}

@Composable
private fun SuggestionsSheetContent(
    suggestions: List<Place>,
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
                text = "Nothing fits a +0 min budget",
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
            WaysideButton(
                text = "Try +15 min",
                onClick = onTryFifteen,
                style = WaysideButtonStyle.Secondary,
                height = 46.dp,
            )
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
            )
        }
    }
}

@Preview(name = "Route · light", showBackground = true, device = "id:pixel_7")
@Composable
private fun RouteScreenLightPreview() {
    WaysideTheme(ThemeMode.Light) {
        RouteScreen(
            state = WaysideUiState(budget = 15, selectedPinId = "queluz", stops = listOf("queluz")),
            onBack = {},
            onBudgetChange = {},
            onSelectPin = {},
            onToggleStop = {},
            onOpenPlace = {},
            onStartRoute = {},
            bottomPadding = 80.dp,
        )
    }
}

@Preview(name = "Route · dark", showBackground = true, device = "id:pixel_7")
@Composable
private fun RouteScreenDarkPreview() {
    WaysideTheme(ThemeMode.Dark) {
        RouteScreen(
            state = WaysideUiState(budget = 45),
            onBack = {},
            onBudgetChange = {},
            onSelectPin = {},
            onToggleStop = {},
            onOpenPlace = {},
            onStartRoute = {},
            bottomPadding = 80.dp,
        )
    }
}

@Preview(name = "Route · +0 empty state", showBackground = true, device = "id:pixel_7")
@Composable
private fun RouteScreenEmptyPreview() {
    WaysideTheme(ThemeMode.Light) {
        RouteScreen(
            state = WaysideUiState(budget = 0),
            onBack = {},
            onBudgetChange = {},
            onSelectPin = {},
            onToggleStop = {},
            onOpenPlace = {},
            onStartRoute = {},
            bottomPadding = 80.dp,
        )
    }
}
