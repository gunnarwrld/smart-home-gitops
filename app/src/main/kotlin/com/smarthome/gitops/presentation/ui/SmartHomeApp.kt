package com.smarthome.gitops.presentation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.gitops.presentation.MainViewModel
import com.smarthome.gitops.presentation.UiState
import com.smarthome.gitops.presentation.ui.theme.AlertBackground
import com.smarthome.gitops.presentation.ui.theme.NormalBackground

/**
 * Root Composable — entry point for the entire UI hierarchy.
 *
 * ── MVVM Compliance ──────────────────────────────────────────────────────────
 * This is the only place where the ViewModel is obtained (via viewModel()).
 * Child composables receive only the state data they need — no ViewModel
 * references are passed down the tree.
 *
 * ── State Observation ────────────────────────────────────────────────────────
 * Uses `collectAsStateWithLifecycle` (lifecycle-aware) to observe the
 * ViewModel's StateFlow. This automatically pauses collection when the
 * app is backgrounded, saving battery.
 */
@Composable
fun SmartHomeApp(
    viewModel: MainViewModel = viewModel()
) {
    // ── Observe ViewModel state ───────────────────────────────────────────────
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── Animate background color between states ───────────────────────────────
    val isAlert = uiState is UiState.SecurityAlert
    val bgColor by animateColorAsState(
        targetValue = if (isAlert) AlertBackground else NormalBackground,
        animationSpec = tween(durationMillis = 800),
        label = "backgroundColorAnimation"
    )

    // ── Dispatch to correct screen composable ────────────────────────────────
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        when (val state = uiState) {
            is UiState.Loading -> LoadingScreen()
            is UiState.Normal -> NormalStateScreen(state = state)
            is UiState.SecurityAlert -> SecurityAlertScreen(state = state)
            is UiState.Error -> ErrorScreen(
                message = state.message,
                onRetry = { viewModel.startPolling() }
            )
        }
    }
}
