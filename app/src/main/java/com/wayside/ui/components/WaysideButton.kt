package com.wayside.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.pressScale

enum class WaysideButtonStyle {
    /** Filled petrol — the one action that moves you forward. */
    Primary,

    /** Outlined petrol — an alternative that is still a real choice. */
    Secondary,

    /** Filled amber — start the route, add the stop. */
    Cta,

    /** Text only — skip, not now, sign out. */
    Ghost,

    /** Filled clay — take the stop back off the route. */
    Danger,
}

@Composable
fun WaysideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: WaysideButtonStyle = WaysideButtonStyle.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    height: Dp = 54.dp,
    horizontalPadding: Dp = 24.dp,
    contentColorOverride: Color? = null,
) {
    WaysideButton(
        onClick = onClick,
        modifier = modifier,
        style = style,
        enabled = enabled,
        height = height,
        horizontalPadding = horizontalPadding,
        contentColorOverride = contentColorOverride,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun WaysideButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: WaysideButtonStyle = WaysideButtonStyle.Primary,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    horizontalPadding: Dp = 24.dp,
    contentColorOverride: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }

    val container: Color
    val content0: Color
    var border: BorderStroke? = null
    when (style) {
        WaysideButtonStyle.Primary -> {
            container = scheme.primary
            content0 = scheme.onPrimary
        }
        WaysideButtonStyle.Secondary -> {
            container = Color.Transparent
            content0 = scheme.primary
            border = BorderStroke(1.5.dp, scheme.primary)
        }
        WaysideButtonStyle.Cta -> {
            container = wayside.accent
            content0 = wayside.onAccent
        }
        WaysideButtonStyle.Ghost -> {
            container = Color.Transparent
            content0 = wayside.textMuted
        }
        WaysideButtonStyle.Danger -> {
            container = scheme.error
            content0 = scheme.onError
        }
    }
    val shape = MaterialTheme.shapes.extraLarge
    val resolvedContent = contentColorOverride ?: content0

    Row(
        modifier = modifier
            .pressScale(interaction)
            .clip(shape)
            .background(if (enabled) container else container.copy(alpha = 0.38f))
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = resolvedContent),
                enabled = enabled,
                onClick = onClick,
            )
            .then(
                if (style == WaysideButtonStyle.Ghost) {
                    Modifier.defaultMinSize(minHeight = 44.dp)
                } else {
                    Modifier.height(height)
                },
            )
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides
                if (enabled) resolvedContent else resolvedContent.copy(alpha = 0.5f),
        ) {
            content()
        }
    }
}
