package com.wayside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wayside.ui.theme.LocalWaysideColors

/**
 * Blocks the app until location is granted.
 *
 * Wayside can't tell which stops are on the way without it, so — same pattern as the Bluetooth
 * gate in Zinga — this is not dismissable: no scrim tap, no back press. [canRequestPermission]
 * distinguishes the two states a denial leaves us in: `true` means the system will still show
 * its own prompt if asked again, so the button re-asks; `false` means it won't ("don't ask
 * again", or the OS has otherwise given up on us), so the only way forward is the device's own
 * app settings.
 */
@Composable
fun LocationRequiredDialog(
    canRequestPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(scheme.surface)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(scheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.LocationOff,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Location access needed",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Wayside can't tell which stops are on your way without it. " +
                    "Turn location on to keep going.",
                style = MaterialTheme.typography.bodyMedium,
                color = wayside.textMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            WaysideButton(
                text = if (canRequestPermission) "Allow location" else "Open settings",
                onClick = if (canRequestPermission) onRequestPermission else onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

