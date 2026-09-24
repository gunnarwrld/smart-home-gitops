package com.smarthome.gitops.presentation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.gitops.presentation.MainViewModel
import com.smarthome.gitops.presentation.OperationResult
import com.smarthome.gitops.presentation.UiState
import com.smarthome.gitops.presentation.ui.theme.AlertAccent
import com.smarthome.gitops.presentation.ui.theme.AlertBackground
import com.smarthome.gitops.presentation.ui.theme.NormalAccent
import com.smarthome.gitops.presentation.ui.theme.NormalBackground

/**
 * Root Composable — entry point for the entire UI hierarchy.
 *
 * ── MVVM Compliance ──────────────────────────────────────────────────────────
 * This is the only place where the ViewModel is obtained (via viewModel()).
 * Child composables receive only the state data and event lambdas they need —
 * no ViewModel references are passed down the tree.
 *
 * ── State Observation ────────────────────────────────────────────────────────
 * Uses `collectAsStateWithLifecycle` (lifecycle-aware) to observe the
 * ViewModel's StateFlow. This automatically pauses collection when the
 * app is backgrounded, saving battery.
 *
 * ── Lab 2 additions ──────────────────────────────────────────────────────────
 * • Scaffold with SnackbarHost — displays operation results (success/failure)
 *   after Force Merge or Force Reject without blocking the main UI.
 * • LaunchedEffect that collects the one-shot [OperationResult] SharedFlow
 *   and calls snackbarHostState.showSnackbar() with the result message.
 * • Passes onForceMerge / onForceReject lambdas to SecurityAlertScreen.
 */
@Composable
fun SmartHomeApp(
    viewModel: MainViewModel = viewModel()
) {
    // ── Observe ViewModel state ───────────────────────────────────────────────
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── Snackbar infrastructure ───────────────────────────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect one-shot OperationResult events → display as Snackbar
    LaunchedEffect(Unit) {
        viewModel.operationResult.collect { result ->
            val message = when (result) {
                is OperationResult.Success -> result.message
                is OperationResult.Failure -> "⚠ ${result.message}"
            }
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // ── Animate background color between states ───────────────────────────────
    val isAlert = uiState is UiState.SecurityAlert
    val bgColor by animateColorAsState(
        targetValue = if (isAlert) AlertBackground else NormalBackground,
        animationSpec = tween(durationMillis = 800),
        label = "backgroundColorAnimation"
    )

    // ── Scaffold hosts the Snackbar and the screen content ───────────────────
    Scaffold(
        containerColor = bgColor,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                // Custom Snackbar tint: green for success (starts with ✓), red for failure
                val isSuccess = data.visuals.message.startsWith("✓")
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isSuccess)
                        NormalAccent.copy(alpha = 0.95f)
                    else
                        AlertAccent.copy(alpha = 0.95f)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Dispatch to correct screen composable ─────────────────────────
            when (val state = uiState) {
                is UiState.Loading -> LoadingScreen()
                is UiState.Normal -> NormalStateScreen(state = state)
                is UiState.SecurityAlert -> SecurityAlertScreen(
                    state = state,
                    onForceMerge = { viewModel.forceMerge(state.prNumber) },
                    onForceReject = { viewModel.forceReject(state.prNumber) }
                )
                is UiState.Error -> ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.startPolling() }
                )
            }
        }
    }
}
