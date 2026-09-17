package com.wayside.features.authentication.password

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wayside.ui.components.AuthErrorText
import com.wayside.ui.components.AuthScaffold
import com.wayside.ui.components.WaysideButton
import com.wayside.ui.components.WaysideButtonStyle
import com.wayside.ui.components.WaysideTextField
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

@Composable
fun ForgotPasswordScreen(viewModel: ForgotPasswordViewModel) {
    val state by viewModel.uiState.collectAsState()
    val isBusy by viewModel.isBusyState.collectAsState()

    ForgotPasswordContent(
        state = state,
        isBusy = isBusy,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::onSubmit,
        onEnterCode = viewModel::onEnterCode,
        onBack = viewModel::onBack,
    )
}

@Composable
private fun ForgotPasswordContent(
    state: ForgotPasswordUiState,
    isBusy: Boolean,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onEnterCode: () -> Unit,
    onBack: () -> Unit,
) {
    val wayside = LocalWaysideColors.current

    AuthScaffold(
        title = if (state.sent) "Check your email" else "Reset password",
        subtitle = if (state.sent) {
            "If that address has an account, a reset code is on its way."
        } else {
            "We'll email you a code to set a new password."
        },
        onBack = onBack,
    ) {
        if (state.sent) {
            Text(
                text = "The code expires shortly, so use it soon.",
                style = MaterialTheme.typography.bodyMedium,
                color = wayside.textMuted,
            )
            Spacer(Modifier.height(24.dp))
            WaysideButton(
                text = "Enter the code",
                onClick = onEnterCode,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            WaysideButton(
                text = "Send it again",
                onClick = onSubmit,
                style = WaysideButtonStyle.Ghost,
                enabled = !isBusy,
            )
        } else {
            WaysideTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Email",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                enabled = !isBusy,
            )
            Spacer(Modifier.height(20.dp))
            AuthErrorText(state.error)
            WaysideButton(
                text = if (isBusy) "Sending…" else "Send reset code",
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmit && !isBusy,
            )
        }
    }
}

@Composable
fun ResetPasswordScreen(viewModel: ResetPasswordViewModel) {
    val state by viewModel.uiState.collectAsState()
    val isBusy by viewModel.isBusyState.collectAsState()

    ResetPasswordContent(
        state = state,
        isBusy = isBusy,
        onEmailChange = viewModel::onEmailChange,
        onCodeChange = viewModel::onCodeChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSubmit = viewModel::onSubmit,
        onBack = viewModel::onBack,
    )
}

@Composable
private fun ResetPasswordContent(
    state: ResetPasswordUiState,
    isBusy: Boolean,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    AuthScaffold(
        title = "Set a new password",
        subtitle = "Enter the 6-digit code from the email, then choose a new password.",
        onBack = onBack,
    ) {
        WaysideTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = "Email",
            keyboardType = KeyboardType.Email,
            enabled = !isBusy,
        )
        Spacer(Modifier.height(16.dp))
        WaysideTextField(
            value = state.code,
            onValueChange = onCodeChange,
            label = "Reset code",
            keyboardType = KeyboardType.Number,
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            enabled = !isBusy,
        )
        Spacer(Modifier.height(16.dp))
        WaysideTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = "New password",
            isPassword = true,
            enabled = !isBusy,
            isError = state.passwordHint != null,
            supportingText = state.passwordHint ?: "8+ characters, with a number and a symbol",
        )
        Spacer(Modifier.height(16.dp))
        WaysideTextField(
            value = state.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirm password",
            isPassword = true,
            imeAction = ImeAction.Done,
            enabled = !isBusy,
            isError = !state.passwordsMatch,
            supportingText = if (state.passwordsMatch) null else "Passwords don't match",
        )

        Spacer(Modifier.height(20.dp))
        AuthErrorText(state.error)

        WaysideButton(
            text = if (isBusy) "Saving…" else "Save new password",
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSubmit && !isBusy,
        )
    }
}

@Preview(name = "Forgot password")
@Composable
private fun ForgotPreview() {
    WaysideTheme(ThemeMode.Light) {
        ForgotPasswordContent(
            state = ForgotPasswordUiState(email = "marta@example.com"),
            isBusy = false,
            onEmailChange = {}, onSubmit = {}, onEnterCode = {}, onBack = {},
        )
    }
}

@Preview(name = "Forgot password — sent, dark")
@Composable
private fun ForgotSentPreview() {
    WaysideTheme(ThemeMode.Dark) {
        ForgotPasswordContent(
            state = ForgotPasswordUiState(email = "marta@example.com", sent = true),
            isBusy = false,
            onEmailChange = {}, onSubmit = {}, onEnterCode = {}, onBack = {},
        )
    }
}

@Preview(name = "Reset password")
@Composable
private fun ResetPreview() {
    WaysideTheme(ThemeMode.Light) {
        ResetPasswordContent(
            state = ResetPasswordUiState(
                email = "marta@example.com",
                code = "CfDJ8L3E8E…",
                password = "NewPass!45",
                confirmPassword = "NewPass!45",
            ),
            isBusy = false,
            onEmailChange = {}, onCodeChange = {}, onPasswordChange = {},
            onConfirmPasswordChange = {}, onSubmit = {}, onBack = {},
        )
    }
}
