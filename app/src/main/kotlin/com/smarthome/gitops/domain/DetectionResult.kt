package com.smarthome.gitops.domain

/**
 * Result returned by [DeceptionDetector.analyze].
 *
 * @param score          Confidence score 0–100 (higher = more likely adversarial).
 * @param matchedPatterns List of human-readable descriptions of triggered patterns.
 * @param rawText        The original comment text that was analyzed.
 * @param isAlarm        True when [score] meets or exceeds the alarm threshold (40).
 */
data class DetectionResult(
    val score: Int,
    val matchedPatterns: List<String>,
    val rawText: String,
    val isAlarm: Boolean
)
