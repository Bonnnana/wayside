package com.wayside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wayside.ui.theme.LocalWaysideColors

/** The amber "+14 min" pill. Time cost, stated plainly, wherever a place appears. */
@Composable
fun DetourBadge(
    minutes: Int,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
) {
    val wayside = LocalWaysideColors.current
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(wayside.accent)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showIcon) {
            Icon(
                Icons.Rounded.Schedule,
                contentDescription = null,
                tint = wayside.onAccent,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = "+$minutes min",
            style = MaterialTheme.typography.labelMedium,
            color = wayside.onAccent,
        )
    }
}
