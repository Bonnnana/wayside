package com.wayside.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wayside.data.Route
import com.wayside.ui.theme.LocalWaysideColors

/**
 * The signature control: how much extra time you are willing to spend today.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetourSegmentedSelector(
    budget: Int,
    onBudgetChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    options: List<Int> = Route.BUDGETS,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, minutes ->
            SegmentedButton(
                selected = budget == minutes,
                onClick = { onBudgetChange(minutes) },
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
                label = {
                    Text(
                        text = "+$minutes",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}
