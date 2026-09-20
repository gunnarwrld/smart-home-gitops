package com.smarthome.gitops.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.gitops.data.repository.GitHubRepository
import com.smarthome.gitops.domain.DeceptionDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "MainViewModel"

/** How often the app polls GitHub (in milliseconds). */
private const val POLL_INTERVAL_MS = 30_000L

/**
 * ─────────────────────────────────────────────────────────────────────────────
 *  MainViewModel  —  Presentation Layer
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Responsibilities (MVVM Contract):
 *   • Owns the [UiState] — exposed as a [StateFlow] for Compose to observe.
 *   • Starts and maintains the background polling loop (every 30 seconds).
 *   • Delegates ALL network I/O to [GitHubRepository] on [Dispatchers.IO].
 *   • Delegates ALL text analysis to [DeceptionDetector] on [Dispatchers.Default].
 *   • Contains ZERO UI code (no Context, no View references).
 *
 * Polling Architecture:
 *   viewModelScope.launch {
 *       while(true) {
 *           // Runs on Dispatchers.IO — keeps network off the UI main thread
 *           withContext(Dispatchers.IO) { repository.fetchAllOpenPrComments() }
 *           delay(POLL_INTERVAL_MS)
 *       }
 *   }
 */
class MainViewModel : ViewModel() {

    private val repository = GitHubRepository()

    // ─── Reactive State ──────────────────────────────────────────────────────

    /**
     * Backing mutable state — only the ViewModel can write to this.
     * Compose UI observes [uiState] (immutable read-only view).
     */
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)

    /**
     * Public read-only StateFlow. Compose collects this with
     * `val state by viewModel.uiState.collectAsStateWithLifecycle()`.
     */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ─── Background Polling Job ──────────────────────────────────────────────

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    /**
     * Starts the continuous 30-second polling loop in [viewModelScope].
     *
     * The entire network call is executed on [Dispatchers.IO] so that
     * the Android main (UI) thread is NEVER blocked by network I/O.
     *
     * Coroutine lifecycle: automatically cancelled when the ViewModel is
     * cleared (activity destroyed / app closed).
     */
    fun startPolling() {
        // Cancel any existing job before starting a new one
        pollingJob?.cancel()

        pollingJob = viewModelScope.launch {
            Log.d(TAG, "Polling loop started — interval: ${POLL_INTERVAL_MS / 1000}s")

            while (true) {
                performPollCycle()
                Log.d(TAG, "Sleeping ${POLL_INTERVAL_MS / 1000}s until next poll…")
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Executes a single poll-and-analyze cycle:
     * 1. Fetch comments on [Dispatchers.IO]
     * 2. Analyze each comment body with [DeceptionDetector] on [Dispatchers.Default]
     * 3. Emit the worst result (highest confidence score) to [_uiState]
     */
    private suspend fun performPollCycle() {
        try {
            val timestamp = currentTimestamp()
            Log.d(TAG, "Poll cycle starting at $timestamp")

            // ── Step 1: Fetch from network on IO dispatcher ──────────────────
            val commentsWithPrInfo = withContext(Dispatchers.IO) {
                repository.fetchAllOpenPrComments()
            }

            Log.d(TAG, "Fetched ${commentsWithPrInfo.size} total comment(s)")

            if (commentsWithPrInfo.isEmpty()) {
                // No open PRs or no comments — definitely Normal
                val openPrCount = withContext(Dispatchers.IO) {
                    repository.fetchOpenPrCount()
                }
                _uiState.value = UiState.Normal(
                    openPrCount = openPrCount,
                    lastCheckedTime = timestamp
                )
                Log.d(TAG, "State → Normal (no comments to analyze)")
                return
            }

            // ── Step 2: Run DeceptionDetector on Dispatchers.Default ─────────
            // Analyze all comment bodies and pick the highest-scoring result
            val worstResult = commentsWithPrInfo
                .mapNotNull { info ->
                    val result = DeceptionDetector.analyze(info.comment.body)
                    Log.d(
                        TAG,
                        "Comment by ${info.comment.user.login}: score=${result.score}%, " +
                            "patterns=${result.matchedPatterns}"
                    )
                    // Attach PR info for the UI
                    if (result.score > 0) Pair(result, info) else null
                }
                .maxByOrNull { (result, _) -> result.score }

            // ── Step 3: Update UI state ──────────────────────────────────────
            if (worstResult != null) {
                val (result, info) = worstResult
                if (result.isAlarm) {
                    Log.w(TAG, "⚠ ALARM: score=${result.score}% — switching to SecurityAlert")
                    _uiState.value = UiState.SecurityAlert(
                        score = result.score,
                        rawText = result.rawText,
                        matchedPatterns = result.matchedPatterns,
                        prNumber = info.prNumber,
                        prTitle = info.prTitle,
                        commenterLogin = info.comment.user.login,
                        lastCheckedTime = timestamp
                    )
                    return
                }
            }

            // No alarm triggered — Normal state
            _uiState.value = UiState.Normal(
                openPrCount = commentsWithPrInfo.map { it.prNumber }.distinct().size,
                lastCheckedTime = timestamp
            )
            Log.d(TAG, "State → Normal (score below threshold)")

        } catch (e: Exception) {
            Log.e(TAG, "Poll cycle failed: ${e.message}", e)
            _uiState.value = UiState.Error(
                message = "Network error: ${e.message ?: "Unknown error"}"
            )
        }
    }

    /** Formats the current time for display in the UI. */
    private fun currentTimestamp(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        Log.d(TAG, "ViewModel cleared — polling job cancelled")
    }
}
