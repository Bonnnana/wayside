package com.wayside.features.authentication.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

@Composable
fun RegisterScreen(viewModel: RegisterViewModel) {
    val state by viewModel.uiState.collectAsState()
    val isBusy by viewModel.isBusyState.collectAsState()

    RegisterContent(
        state = state,
        isBusy = isBusy,
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSubmit = viewModel::onSubmit,
        onSignIn = viewModel::onSignIn,
        onBack = viewModel::onBack,
    )
}

@Composable
private fun RegisterContent(
    state: RegisterUiState,
    isBusy: Boolean,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
) {
    val wayside = LocalWaysideColors.current

    AuthScaffold(
        title = "Create an account",
        subtitle = "So your saved places and routes follow you between devices.",
        onBack = onBack,
        footer = {
            WaysideButton(
                text = if (isBusy) "Creating account…" else "Create account",
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmit && !isBusy,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Already have one?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = wayside.textMuted,
                )
                WaysideButton(
                    text = "Sign in",
                    onClick = onSignIn,
                    style = WaysideButtonStyle.Ghost,
                    enabled = !isBusy,
                )
            }
        },
    ) {
        Row(Modifier.fillMaxWidth()) {
            WaysideTextField(
                value = state.firstName,
                onValueChange = onFirstNameChange,
                label = "First name",
                modifier = Modifier.weight(1f),
                enabled = !isBusy,
            )
            Spacer(Modifier.width(12.dp))
            WaysideTextField(
                value = state.lastName,
                onValueChange = onLastNameChange,
                label = "Last name",
                modifier = Modifier.weight(1f),
                enabled = !isBusy,
            )
        }

        Spacer(Modifier.height(16.dp))
        WaysideTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = "Email",
            keyboardType = KeyboardType.Email,
            enabled = !isBusy,
        )

        Spacer(Modifier.height(16.dp))
        WaysideTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = "Password",
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
    }
}

@Preview(name = "Register — light")
@Composable
private fun RegisterPreview() {
    WaysideTheme(ThemeMode.Light) {
        RegisterContent(
            state = RegisterUiState(
                firstName = "Marta",
                lastName = "Bento",
                email = "marta@example.com",
                password = "Wayside!23",
                confirmPassword = "Wayside!23",
            ),
            isBusy = false,
            onFirstNameChange = {}, onLastNameChange = {}, onEmailChange = {},
            onPasswordChange = {}, onConfirmPasswordChange = {},
            onSubmit = {}, onSignIn = {}, onBack = {},
        )
    }
}

@Preview(name = "Register — weak password, dark")
@Composable
private fun RegisterWeakPreview() {
    WaysideTheme(ThemeMode.Dark) {
        RegisterContent(
            state = RegisterUiState(
                firstName = "Marta",
                lastName = "Bento",
                email = "marta@example.com",
                password = "wayside",
                confirmPassword = "way",
            ),
            isBusy = false,
            onFirstNameChange = {}, onLastNameChange = {}, onEmailChange = {},
            onPasswordChange = {}, onConfirmPasswordChange = {},
            onSubmit = {}, onSignIn = {}, onBack = {},
        )
    }
}
