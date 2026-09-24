package com.smarthome.gitops.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.gitops.data.repository.GitHubRepository
import com.smarthome.gitops.domain.DeceptionDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
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
 *   • Exposes [operationResult] as a [SharedFlow] for one-shot Snackbar events.
 *   • Contains ZERO UI code (no Context, no View references).
 *
 * ─── Lab 1: Polling Architecture ────────────────────────────────────────────
 *   viewModelScope.launch {
 *       while(true) {
 *           performPollCycle()   // IO → Default → emit UiState
 *           delay(30_000)
 *       }
 *   }
 *
 * ─── Lab 2: Write Actions ────────────────────────────────────────────────────
 *   forceReject(prNumber)  → PATCH PR closed, then immediate poll
 *   forceMerge(prNumber)   → GET file, modify JSON, PUT file, PATCH PR, poll
 *   Both actions:
 *     • Disable UI buttons via isOperationInProgress = true
 *     • Emit OperationResult via SharedFlow (→ Snackbar in UI)
 *     • Trigger an immediate poll on success to close the GitOps loop
 *     • Never crash on network/HTTP errors — map to human-readable messages
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

    /**
     * One-shot event channel for write operation outcomes.
     * SharedFlow with replay=0 — each event is delivered exactly once to
     * the active collector (the Snackbar LaunchedEffect in SmartHomeApp).
     */
    private val _operationResult = MutableSharedFlow<OperationResult>()
    val operationResult: SharedFlow<OperationResult> = _operationResult.asSharedFlow()

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
            val worstResult = withContext(Dispatchers.Default) {
                commentsWithPrInfo
                    .mapNotNull { info ->
                        val result = DeceptionDetector.analyze(info.comment.body)
                        Log.d(
                            TAG,
                            "Comment by ${info.comment.user.login}: score=${result.score}%, " +
                                "patterns=${result.matchedPatterns}"
                        )
                        if (result.score > 0) Pair(result, info) else null
                    }
                    .maxByOrNull { (result, _) -> result.score }
            }

            // ── Step 3: Update UI state ──────────────────────────────────────
            if (worstResult != null) {
                val (result, info) = worstResult
                if (result.isAlarm) {
                    Log.w(TAG, "⚠ ALARM: score=${result.score}% — switching to SecurityAlert")
                    // Preserve isOperationInProgress if we are already in alert state
                    val inProgress = (_uiState.value as? UiState.SecurityAlert)
                        ?.isOperationInProgress ?: false
                    _uiState.value = UiState.SecurityAlert(
                        score = result.score,
                        rawText = result.rawText,
                        matchedPatterns = result.matchedPatterns,
                        prNumber = info.prNumber,
                        prTitle = info.prTitle,
                        commenterLogin = info.comment.user.login,
                        lastCheckedTime = timestamp,
                        isOperationInProgress = inProgress
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

    // ─── Lab 2: Write Actions ────────────────────────────────────────────────

    /**
     * Force Reject — closes the Pull Request without merging.
     *
     * Flow:
     *   1. Disable buttons (isOperationInProgress = true)
     *   2. PATCH PR → state="closed"   (IO dispatcher)
     *   3. Emit OperationResult → Snackbar
     *   4. Trigger immediate poll → ViewModel detects no open alarming PR
     *      → UiState transitions to Normal (GitOps loop closed)
     */
    fun forceReject(prNumber: Int) {
        viewModelScope.launch {
            setOperationInProgress(true)
            Log.d(TAG, "Force Reject initiated for PR #$prNumber")

            try {
                withContext(Dispatchers.IO) {
                    repository.closePullRequest(prNumber)
                }
                Log.d(TAG, "Force Reject succeeded — PR #$prNumber closed")
                _operationResult.emit(
                    OperationResult.Success("✓ PR #$prNumber rejected and closed")
                )
                // Immediate poll closes the GitOps loop — no need to wait 30 s
                performPollCycle()

            } catch (e: HttpException) {
                val msg = httpErrorMessage(e, prNumber, "Reject")
                Log.e(TAG, "Force Reject HTTP error: $msg", e)
                _operationResult.emit(OperationResult.Failure(msg))
                setOperationInProgress(false)
            } catch (e: IOException) {
                Log.e(TAG, "Force Reject network I/O error: ${e.message}", e)
                _operationResult.emit(
                    OperationResult.Failure("Network error — check your connection")
                )
                setOperationInProgress(false)
            } catch (e: Exception) {
                Log.e(TAG, "Force Reject unexpected error: ${e.message}", e)
                _operationResult.emit(
                    OperationResult.Failure("Unexpected error: ${e.message ?: "Unknown"}")
                )
                setOperationInProgress(false)
            }
        }
    }

    /**
     * Force Merge — applies the EcoAgent's proposed change to house_config.json
     * and then closes the Pull Request.
     *
     * Flow:
     *   1. Disable buttons (isOperationInProgress = true)
     *   2. GET house_config.json → current blob SHA + Base64 content
     *   3. Decode → modify JSON (temp=17.0, last_updated_by=Android-Operator)
     *   4. PUT updated file → direct commit to main branch
     *   5. PATCH PR → state="closed"
     *   6. Emit OperationResult → Snackbar
     *   7. Trigger immediate poll → UiState transitions to Normal
     */
    fun forceMerge(prNumber: Int) {
        viewModelScope.launch {
            setOperationInProgress(true)
            Log.d(TAG, "Force Merge initiated for PR #$prNumber")

            try {
                withContext(Dispatchers.IO) {
                    repository.updateFileAndClosePr(prNumber)
                }
                Log.d(TAG, "Force Merge succeeded — PR #$prNumber closed, house_config.json updated")
                _operationResult.emit(
                    OperationResult.Success("✓ Merged — temperature set to 17.0°C")
                )
                // Immediate poll closes the GitOps loop
                performPollCycle()

            } catch (e: HttpException) {
                val msg = httpErrorMessage(e, prNumber, "Merge")
                Log.e(TAG, "Force Merge HTTP error: $msg", e)
                _operationResult.emit(OperationResult.Failure(msg))
                setOperationInProgress(false)
            } catch (e: IOException) {
                Log.e(TAG, "Force Merge network I/O error: ${e.message}", e)
                _operationResult.emit(
                    OperationResult.Failure("Network error — check your connection")
                )
                setOperationInProgress(false)
            } catch (e: Exception) {
                Log.e(TAG, "Force Merge unexpected error: ${e.message}", e)
                _operationResult.emit(
                    OperationResult.Failure("Unexpected error: ${e.message ?: "Unknown"}")
                )
                setOperationInProgress(false)
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Flips the isOperationInProgress flag on the current SecurityAlert state.
     * If the current state is not SecurityAlert this is a safe no-op.
     */
    private fun setOperationInProgress(inProgress: Boolean) {
        val current = _uiState.value
        if (current is UiState.SecurityAlert) {
            _uiState.value = current.copy(isOperationInProgress = inProgress)
        }
    }

    /**
     * Maps an [HttpException] to a human-readable message suitable for a Snackbar.
     * Covers the most common GitHub API error codes a student will encounter.
     */
    private fun httpErrorMessage(e: HttpException, prNumber: Int, action: String): String =
        when (e.code()) {
            401 -> "Authentication failed — check your GitHub token"
            403 -> "Access denied — token may lack 'repo' scope, or rate limited"
            404 -> "PR #$prNumber not found — already closed?"
            422 -> "Unprocessable request — PR may be in an invalid state"
            else -> "HTTP ${e.code()} on Force $action: ${e.message()}"
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
