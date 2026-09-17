package com.wayside.features.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wayside.models.ui.PlaceSuggestion
import com.wayside.ui.components.AuthErrorText
import com.wayside.ui.components.AuthScaffold
import com.wayside.ui.components.CircleIconButton
import com.wayside.ui.components.WaysideButton
import com.wayside.ui.components.WaysideButtonStyle
import com.wayside.ui.components.WaysideTextField
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    val state by viewModel.uiState.collectAsState()

    SearchContent(
        state = state,
        onOriginChange = viewModel::onOriginChange,
        onDestinationChange = viewModel::onDestinationChange,
        onSuggestionPicked = viewModel::onSuggestionPicked,
        onUseMyLocation = viewModel::onUseMyLocation,
        onSwap = viewModel::onSwap,
        onSubmit = viewModel::onSubmit,
        onDismissSuggestions = viewModel::onDismissSuggestions,
        onBack = viewModel::onBack,
    )
}

@Composable
private fun SearchContent(
    state: SearchUiState,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onSuggestionPicked: (PlaceSuggestion) -> Unit,
    onUseMyLocation: () -> Unit,
    onSwap: () -> Unit,
    onSubmit: () -> Unit,
    onDismissSuggestions: () -> Unit = {},
    onBack: () -> Unit,
) {
    val wayside = LocalWaysideColors.current

    AuthScaffold(
        title = "Where are you driving?",
        subtitle = "Start typing and pick a place from the list, so the route goes where you mean.",
        onBack = onBack,
        onBackgroundTap = onDismissSuggestions,
        footer = {
            AuthErrorText(state.error)
            WaysideButton(
                text = "Find detours",
                onClick = onSubmit,
                enabled = state.canSubmit,
                style = WaysideButtonStyle.Cta,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Places are found along the drive between these two, so a longer route " +
                    "opens up more of them.",
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
            )
        },
    ) {
        WaysideTextField(
            value = state.origin,
            onValueChange = onOriginChange,
            label = "From",
        )
        if (state.activeField == SearchField.Origin) {
            SuggestionList(state.suggestions, onSuggestionPicked)
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            WaysideButton(
                text = if (state.isLocatingMe) "Finding you…" else "Use my location",
                onClick = onUseMyLocation,
                enabled = !state.isLocatingMe,
                style = WaysideButtonStyle.Ghost,
                leadingIcon = Icons.Rounded.MyLocation,
                height = 38.dp,
                horizontalPadding = 12.dp,
            )
        }

        Spacer(Modifier.height(4.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            CircleIconButton(
                icon = Icons.Rounded.SwapVert,
                contentDescription = "Swap origin and destination",
                onClick = onSwap,
                tint = wayside.textMuted,
            )
        }
        Spacer(Modifier.height(12.dp))

        WaysideTextField(
            value = state.destination,
            onValueChange = onDestinationChange,
            label = "To",
            imeAction = ImeAction.Done,
        )
        if (state.activeField == SearchField.Destination) {
            SuggestionList(state.suggestions, onSuggestionPicked)
        }

    }
}

/**
 * Sits directly under the field it belongs to. Capped in height so a long list can't push the
 * other field off the screen — the point is to pick and move on.
 */
@Composable
private fun SuggestionList(
    suggestions: List<PlaceSuggestion>,
    onPick: (PlaceSuggestion) -> Unit,
) {
    if (suggestions.isEmpty()) return

    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    Spacer(Modifier.height(6.dp))
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.surface)
            .border(1.dp, wayside.hairline, MaterialTheme.shapes.medium),
    ) {
        items(suggestions, key = { it.placeId }) { suggestion ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(suggestion) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = suggestion.primaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                )
                if (suggestion.secondaryText.isNotBlank()) {
                    Text(
                        text = suggestion.secondaryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = wayside.textMuted,
                    )
                }
            }
        }
    }
}

@Preview(name = "Search · suggestions", showBackground = true, device = "id:pixel_7")
@Composable
private fun SearchPreview() {
    WaysideTheme(ThemeMode.Light) {
        SearchContent(
            state = SearchUiState(
                origin = "Alfama",
                destination = "Sin",
                activeField = SearchField.Destination,
                suggestions = listOf(
                    PlaceSuggestion("1", "Sintra", "Portugal"),
                    PlaceSuggestion("2", "Sintra National Palace", "Largo Rainha Dona Amélia"),
                ),
            ),
            onOriginChange = {},
            onDestinationChange = {},
            onSuggestionPicked = {},
            onUseMyLocation = {},
            onSwap = {},
            onSubmit = {},
            onBack = {},
        )
    }
}

@Preview(name = "Search · empty, dark", showBackground = true, device = "id:pixel_7")
@Composable
private fun SearchDarkPreview() {
    WaysideTheme(ThemeMode.Dark) {
        SearchContent(
            state = SearchUiState(),
            onOriginChange = {},
            onDestinationChange = {},
            onSuggestionPicked = {},
            onUseMyLocation = {},
            onSwap = {},
            onSubmit = {},
            onBack = {},
        )
    }
}
