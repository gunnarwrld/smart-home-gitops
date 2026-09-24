package com.smarthome.gitops.presentation

/**
 * Sealed class representing every possible UI state of the Smart Home monitor.
 *
 * The ViewModel publishes one of these states via StateFlow; the Compose UI
 * observes it and renders accordingly.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 *  State Machine:
 *
 *   [Loading] ──► [Normal]  (no open PRs or score < 40)
 *       │               ▲
 *       │               │  GitOps loop closure after Force Merge/Reject
 *       └──────────► [SecurityAlert]  (score ≥ 40)
 *                        │
 *                   [Error] (network/API failure, surfaced via Snackbar too)
 * ─────────────────────────────────────────────────────────────────────────────
 */
sealed class UiState {

    /** Initial state while the first poll is in-flight. */
    object Loading : UiState()

    /**
     * Green state — no adversarial attacks detected.
     * @param openPrCount     Number of currently open PRs (shown in UI).
     * @param lastCheckedTime Human-readable timestamp of last poll.
     * @param pollIntervalSec The polling interval in seconds (shown for transparency).
     */
    data class Normal(
        val openPrCount: Int = 0,
        val lastCheckedTime: String = "",
        val pollIntervalSec: Int = 30
    ) : UiState()

    /**
     * Red alert state — DeceptionDetector fired an alarm.
     *
     * @param score                 Confidence score 0–100%.
     * @param rawText               The exact adversarial comment text.
     * @param matchedPatterns       List of triggered threat pattern names.
     * @param prNumber              The Pull Request number that triggered the alert.
     * @param prTitle               The Pull Request title.
     * @param commenterLogin        GitHub username of the comment author.
     * @param lastCheckedTime       Human-readable timestamp of when alert was found.
     * @param isOperationInProgress True while Force Merge or Force Reject is in-flight.
     *                              Disables both action buttons and shows a spinner to
     *                              prevent double-submit and confusing concurrent calls.
     */
    data class SecurityAlert(
        val score: Int,
        val rawText: String,
        val matchedPatterns: List<String>,
        val prNumber: Int,
        val prTitle: String,
        val commenterLogin: String,
        val lastCheckedTime: String,
        val isOperationInProgress: Boolean = false
    ) : UiState()

    /**
     * Error state — network failure or API error during a poll cycle.
     * Write-operation errors are surfaced separately via [OperationResult]
     * (Snackbar) so the existing alert state is preserved.
     *
     * @param message  Short error description to display.
     */
    data class Error(
        val message: String
    ) : UiState()
}

/**
 * One-shot event emitted by MainViewModel after each write operation
 * (Force Merge / Force Reject). Consumed by the UI Snackbar.
 *
 * Using a sealed class (not a boolean) keeps the result self-documenting
 * and easy to pattern-match in the collect lambda.
 */
sealed class OperationResult {
    /** The write succeeded — PR was closed (and file updated for Force Merge). */
    data class Success(val message: String) : OperationResult()

    /** The write failed — a human-readable error is provided for the Snackbar. */
    data class Failure(val message: String) : OperationResult()
}
