package com.wayside.features.authentication.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wayside.ui.components.AuthErrorText
import com.wayside.ui.components.AuthScaffold
import com.wayside.ui.components.WaysideButton
import com.wayside.ui.components.WaysideButtonStyle
import com.wayside.ui.components.WaysideTextField
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val state by viewModel.uiState.collectAsState()
    val isBusy by viewModel.isBusyState.collectAsState()

    LoginContent(
        email = state.email,
        password = state.password,
        error = state.error,
        canSubmit = state.canSubmit,
        isBusy = isBusy,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit,
        onForgotPassword = viewModel::onForgotPassword,
        onRegister = viewModel::onRegister,
        // Only shown when there's actually somewhere sensible for it to go — see
        // LoginViewModel.canGoBackToOnboarding.
        onBack = if (viewModel.canGoBackToOnboarding()) viewModel::onBack else null,
    )
}

@Composable
private fun LoginContent(
    email: String,
    password: String,
    error: String?,
    canSubmit: Boolean,
    isBusy: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    onBack: (() -> Unit)?,
) {
    AuthScaffold(
        title = "Welcome",
        subtitle = "Sign in to pick up your saved places and routes.",
        onBack = onBack,
        footer = {
            WaysideButton(
                text = if (isBusy) "Signing in…" else "Sign in",
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = canSubmit && !isBusy,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                WaysideButton(
                    text = "Create an account",
                    onClick = onRegister,
                    style = WaysideButtonStyle.Ghost,
                    enabled = !isBusy,
                )
            }
        },
    ) {
        WaysideTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            keyboardType = KeyboardType.Email,
            enabled = !isBusy,
            isError = error != null,
        )
        Spacer(Modifier.height(16.dp))
        WaysideTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            isPassword = true,
            imeAction = ImeAction.Done,
            enabled = !isBusy,
            isError = error != null,
        )

        Spacer(Modifier.height(12.dp))
        AuthErrorText(error)

        WaysideButton(
            text = "Forgot password?",
            onClick = onForgotPassword,
            style = WaysideButtonStyle.Ghost,
            enabled = !isBusy,
        )
    }
}

@Preview(name = "Login — light")
@Composable
private fun LoginPreview() {
    WaysideTheme(ThemeMode.Light) {
        LoginContent(
            email = "marta@example.com",
            password = "secret123",
            error = null,
            canSubmit = true,
            isBusy = false,
            onEmailChange = {}, onPasswordChange = {}, onSubmit = {},
            onForgotPassword = {}, onRegister = {}, onBack = {},
        )
    }
}

@Preview(name = "Login — error, dark")
@Composable
private fun LoginErrorPreview() {
    WaysideTheme(ThemeMode.Dark) {
        LoginContent(
            email = "marta@example.com",
            password = "wrong",
            error = "That email and password don't match.",
            canSubmit = true,
            isBusy = false,
            onEmailChange = {}, onPasswordChange = {}, onSubmit = {},
            onForgotPassword = {}, onRegister = {}, onBack = {},
        )
    }
}
