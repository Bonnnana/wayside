package com.wayside.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wayside.data.PlaceCategory
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.WaysideMotion

/** Category identity: a coloured dot plus the label. Never decorative. */
@Composable
fun CategoryChip(
    category: PlaceCategory,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = modifier
            .clip(shape)
            .background(category.color.copy(alpha = 0.14f))
            .border(1.dp, category.color.copy(alpha = 0.32f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(category.color),
        )
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Selectable pill used for interests, map filters and saved filters. */
@Composable
fun WaysideChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val container by animateColorAsState(
        targetValue = if (selected) scheme.primary else Color.Transparent,
        animationSpec = WaysideMotion.tweenQuick(),
        label = "chipContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) scheme.onPrimary else scheme.onSurface,
        animationSpec = WaysideMotion.tweenQuick(),
        label = "chipContent",
    )
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(container)
            .border(1.dp, if (selected) Color.Transparent else wayside.hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

/** A chip that floats over the map — same shape, but reads on a busy background. */
@Composable
fun MapFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val wayside = LocalWaysideColors.current
    val shape = MaterialTheme.shapes.medium
    val container by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.surface,
        animationSpec = WaysideMotion.tweenQuick(),
        label = "mapChipContainer",
    )
    val content = if (selected) scheme.onPrimary else scheme.onSurface
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(container)
            .border(1.dp, if (selected) Color.Transparent else wayside.hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = content)
    }
}
