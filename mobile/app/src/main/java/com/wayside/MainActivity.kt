package com.wayside

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wayside.ui.WaysideViewModel
import com.wayside.ui.screens.WaysideApp
import com.wayside.ui.theme.WaysideTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Hoisted here so the theme selector on the profile screen repaints the whole app.
            val viewModel: WaysideViewModel = viewModel()
            val state by viewModel.state.collectAsState()
            WaysideTheme(themeMode = state.themeMode) {
                WaysideApp(viewModel = viewModel)
            }
        }
    }
}
