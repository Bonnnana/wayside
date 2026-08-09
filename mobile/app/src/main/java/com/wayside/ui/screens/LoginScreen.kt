package com.wayside.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wayside.R
import com.wayside.ui.components.AppMark
import com.wayside.ui.components.WaysideButton
import com.wayside.ui.components.WaysideButtonStyle
import com.wayside.ui.components.WelcomeIllustration
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.ThemeMode
import com.wayside.ui.theme.WaysideTheme
import kotlinx.coroutines.delay

private const val SIGN_IN_MILLIS = 1300L

@Composable
fun LoginScreen(
    onSignedInWithGoogle: () -> Unit,
    onEmail: () -> Unit,
    onGuest: () -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    var signingIn by remember { mutableStateOf(false) }

    LaunchedEffect(signingIn) {
        if (signingIn) {
            delay(SIGN_IN_MILLIS)
            signingIn = false
            onSignedInWithGoogle()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        WelcomeIllustration(
            Modifier
                .fillMaxSize()
                .alpha(0.5f),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            AppMark(
                Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(24.dp)),
                cornerFraction = 0.32f,
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Welcome to Wayside",
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onBackground,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Sign in and your saved places travel with you.",
                style = MaterialTheme.typography.bodyMedium,
                color = wayside.textMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))

            WaysideButton(
                onClick = { if (!signingIn) signingIn = true },
                style = WaysideButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
                contentColorOverride = scheme.onSurface,
            ) {
                Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    if (signingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = scheme.primary,
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_google_g),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (signingIn) "Signing in…" else "Continue with Google",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Spacer(Modifier.height(12.dp))
            WaysideButton(
                text = "Continue with email",
                onClick = onEmail,
                style = WaysideButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            WaysideButton(
                text = "Continue as guest",
                onClick = onGuest,
                style = WaysideButtonStyle.Ghost,
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = "By continuing you agree to the terms of service and the privacy policy.",
                style = MaterialTheme.typography.labelSmall,
                color = wayside.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 18.dp),
            )
        }
    }
}

@Preview(name = "Login · light", showBackground = true, device = "id:pixel_7")
@Composable
private fun LoginScreenLightPreview() {
    WaysideTheme(ThemeMode.Light) {
        LoginScreen(onSignedInWithGoogle = {}, onEmail = {}, onGuest = {})
    }
}

@Preview(name = "Login · dark", showBackground = true, device = "id:pixel_7")
@Composable
private fun LoginScreenDarkPreview() {
    WaysideTheme(ThemeMode.Dark) {
        LoginScreen(onSignedInWithGoogle = {}, onEmail = {}, onGuest = {})
    }
}
