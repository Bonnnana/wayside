package com.wayside.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.wayside.ui.theme.LocalWaysideColors

/**
 * The shell every auth screen sits in: back arrow, display headline, supporting line, then the
 * form. Scrolls and lifts above the keyboard so the submit button stays reachable on short
 * screens.
 *
 * @param footer Pinned to the bottom of the screen, outside the scrolling area — the primary
 *   action button and whatever sits directly under it (a "New here? Create an account" link,
 *   say), so it's always in the same place instead of drifting with however long the form is.
 *   Falls back to flowing at the end of [content] when null, unchanged from before this existed.
 * @param onBackgroundTap Notified on a tap that no field, button or other child inside [content]
 *   already consumed — a screen with its own dropdown (e.g. search suggestions) uses this to
 *   dismiss it on an outside tap, same as tapping away from any other overlay.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onBackgroundTap: (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val wayside = LocalWaysideColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            // The navigation host keeps the previous screen mounted underneath during the
            // transition, so an auth screen without its own background shows through to it.
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onBackgroundTap != null) {
                        // A plain tap only — press-and-drag to scroll still reaches the field or
                        // list beneath it, this only ever fires for a tap nothing else claimed.
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(onTap = { onBackgroundTap() })
                        }
                    } else {
                        Modifier
                    },
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            if (onBack != null) {
                Spacer(Modifier.height(8.dp))
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(if (onBack != null) 16.dp else 72.dp))

            Text(text = title, style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = wayside.textMuted,
            )

            Spacer(Modifier.height(32.dp))
            content()
            Spacer(Modifier.height(32.dp))
        }

        if (footer != null) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                footer()
            }
        }
    }
}

/**
 * Text field styled to the Wayside palette. `isPassword` adds the reveal toggle — a plain
 * `visualTransformation` parameter would make every caller rebuild the same toggle.
 */
@Composable
fun WaysideTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Unspecified,
    autoCorrectEnabled: Boolean? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    var revealed by remember { mutableStateOf(false) }
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        visualTransformation = when {
            !isPassword || revealed -> VisualTransformation.None
            else -> PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction,
            // Exact-string fields (a reset code) must opt out — the IME will otherwise
            // capitalise or "correct" what the user typed and the server rejects it.
            capitalization = capitalization,
            autoCorrectEnabled = autoCorrectEnabled,
        ),
        trailingIcon = if (!isPassword) null else {
            {
                IconButton(onClick = { revealed = !revealed }) {
                    Icon(
                        imageVector = if (revealed) {
                            Icons.Rounded.VisibilityOff
                        } else {
                            Icons.Rounded.Visibility
                        },
                        contentDescription = if (revealed) "Hide password" else "Show password",
                        tint = wayside.textMuted,
                    )
                }
            }
        },
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = scheme.primary,
            unfocusedBorderColor = wayside.hairline,
            focusedLabelColor = scheme.primary,
            unfocusedLabelColor = wayside.textMuted,
            cursorColor = scheme.primary,
        ),
    )
}

/** Inline error under a form. Blank messages render nothing rather than an empty red gap. */
@Composable
fun AuthErrorText(message: String?, modifier: Modifier = Modifier) {
    if (message.isNullOrBlank()) return

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.Top) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(12.dp))
    }
}
