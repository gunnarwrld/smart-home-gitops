package com.smarthome.gitops.presentation

/**
 * Sealed class representing the two possible UI states of the Smart Home monitor.
 *
 * The ViewModel publishes one of these states via StateFlow; the Compose UI
 * observes it and renders accordingly.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 *  State Machine:
 *
 *   [Loading] ──► [Normal]  (no open PRs or score < 40)
 *       │                        │
 *       └──────────────────► [SecurityAlert]  (score ≥ 40)
 *                                │
 *                           [Error] (network/API failure)
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
     * @param score           Confidence score 0–100%.
     * @param rawText         The exact adversarial comment text.
     * @param matchedPatterns List of triggered threat pattern names.
     * @param prNumber        The Pull Request number that triggered the alert.
     * @param prTitle         The Pull Request title.
     * @param commenterLogin  GitHub username of the comment author.
     * @param lastCheckedTime Human-readable timestamp of when alert was found.
     */
    data class SecurityAlert(
        val score: Int,
        val rawText: String,
        val matchedPatterns: List<String>,
        val prNumber: Int,
        val prTitle: String,
        val commenterLogin: String,
        val lastCheckedTime: String
    ) : UiState()

    /**
     * Error state — network failure or API error.
     * @param message  Short error description to display.
     */
    data class Error(
        val message: String
    ) : UiState()
}
