package com.wayside.features.profile

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wayside.ui.components.AuthErrorText
import com.wayside.ui.components.AuthScaffold
import com.wayside.ui.components.WaysideButton
import com.wayside.ui.components.WaysideTextField
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme

@Composable
fun EditProfileScreen(viewModel: EditProfileViewModel) {
    val state by viewModel.uiState.collectAsState()
    val isBusy by viewModel.isBusyState.collectAsState()

    EditProfileContent(
        state = state,
        isBusy = isBusy,
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onSubmit = viewModel::onSubmit,
        onBack = viewModel::onBack,
    )
}

@Composable
private fun EditProfileContent(
    state: EditProfileUiState,
    isBusy: Boolean,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    AuthScaffold(
        title = "Edit profile",
        subtitle = "Update the name shown on your account.",
        onBack = onBack,
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
                imeAction = ImeAction.Done,
                enabled = !isBusy,
            )
        }

        Spacer(Modifier.height(20.dp))
        AuthErrorText(state.error)

        WaysideButton(
            text = if (isBusy) "Saving…" else "Save changes",
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSubmit && !isBusy,
        )
    }
}

@Preview(name = "Edit profile")
@Composable
private fun EditProfilePreview() {
    WaysideTheme(ThemeMode.Light) {
        EditProfileContent(
            state = EditProfileUiState(firstName = "Marta", lastName = "Bento"),
            isBusy = false,
            onFirstNameChange = {},
            onLastNameChange = {},
            onSubmit = {},
            onBack = {},
        )
    }
}
