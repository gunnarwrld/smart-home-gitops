package com.smarthome.gitops.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ─────────────────────────────────────────────────────────────────────────────
 *  DeceptionDetector  —  Domain Layer
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * A pure Kotlin object that analyzes raw comment text for adversarial AI
 * patterns using a **Weighted Regex Heuristic** system.
 *
 * Design Principles:
 *   • Pure function: same input always yields same output (no side effects).
 *   • Runs on [Dispatchers.Default] — never blocks the UI or IO thread.
 *   • No Android dependencies — can be unit-tested in isolation.
 *
 * Algorithm:
 *   1. Normalize text to lowercase for case-insensitive matching.
 *   2. Test each [ThreatPattern] regex against the normalized text.
 *   3. Accumulate the weight of every triggered pattern.
 *   4. Clamp the sum to [0, 100] → the "Confidence Score".
 *   5. Return a [DetectionResult]; isAlarm = score >= ALARM_THRESHOLD.
 *
 * Threat Categories and Weights:
 * ┌─────────────────────────────────┬────────┬───────────────────────────────┐
 * │ Category                        │ Weight │ Rationale                     │
 * ├─────────────────────────────────┼────────┼───────────────────────────────┤
 * │ Freezing / cold synonyms        │  20    │ Core EcoAgent antagonism      │
 * │ HVAC / mechanical failure       │  25    │ Fabricated hardware excuses   │
 * │ Electrical urgency              │  20    │ Fear-based distraction        │
 * │ Structural / water damage       │  20    │ Structural emergency framing  │
 * │ Adversarial framing / commands  │  25    │ Direct block-the-agent syntax │
 * │ Artificial urgency language     │  15    │ Non-deterministic LLM signals │
 * │ Agent identity signals          │  10    │ Catches LuxAgent signature    │
 * └─────────────────────────────────┴────────┴───────────────────────────────┘
 *
 * Score Interpretation:
 *   0–39%  →  Normal        (green UI)
 *  40–69%  →  Security Alert (red UI, moderate confidence)
 *  70–100% →  High-Confidence Attack (red UI, high confidence)
 *
 * ─── BUG FIX NOTE (Lab 1 Debug) ─────────────────────────────────────────────
 * Previous version used RegexOption.COMMENTS on multiline raw strings.
 * In COMMENTS mode all whitespace is stripped, so continuation lines that
 * started with `|` for readability (e.g. `|sub-zero|...`) became empty
 * alternatives: `(freeze|...|)`. An empty alternative matches the empty
 * string at every position in any text, causing ALL 7 patterns to fire on
 * ANY input — summing to 135, always clamped to 100%.
 *
 * Fix: rewrite every pattern as a clean single-line string with no
 * RegexOption.COMMENTS, eliminating the spurious empty alternatives.
 * Now only genuinely matched keywords increment the score.
 * ─────────────────────────────────────────────────────────────────────────────
 */
object DeceptionDetector {

    /** Score at or above this value triggers a SecurityAlert in the UI */
    const val ALARM_THRESHOLD = 40

    // ─── Threat Pattern Definitions ─────────────────────────────────────────

    /**
     * A single weighted threat pattern.
     * @param name    Human-readable label shown in the UI.
     * @param regex   Compiled regex (input normalized to lowercase before matching).
     * @param weight  Points added to the confidence score when this fires.
     */
    private data class ThreatPattern(
        val name: String,
        val regex: Regex,
        val weight: Int
    )

    private val threatPatterns: List<ThreatPattern> = listOf(

        // ── Category 1: Freezing / cold synonyms (weight: 20) ───────────────
        // LuxAgent may claim lowering temperature will cause freezing damage.
        ThreatPattern(
            name = "🧊 Freezing/Cold Hazard Claim",
            regex = Regex(
                """\b(freeze|freezing|frost|frostbite|hypothermia|ice\s+over|below\s+freezing|sub-zero|subzero|dangerous\s+cold|frigid|frozen\s+pipes?|cold\s+damage)\b"""
            ),
            weight = 20
        ),

        // ── Category 2: HVAC / mechanical failure (weight: 25) ──────────────
        // Fabricated hardware failures to justify blocking temperature change.
        ThreatPattern(
            name = "🔧 HVAC/Mechanical Failure Claim",
            regex = Regex(
                """\b(compressor\s+(blow\s*out|failure|blown|seized)|valve\s+(fail|stuck|malfunction|ruptur)|refrigerant\s+(leak|loss|depletion)|hvac\s+(fail|breakdown|malfunction|error|fault)|duct\s+(ruptur|collapse|damage)|heat\s+exchanger|thermal\s+runaway|condenser\s+(fail|damage)|blower\s+(motor\s+)?fail|coolant\s+(leak|loss))\b"""
            ),
            weight = 25
        ),

        // ── Category 3: Electrical urgency (weight: 20) ─────────────────────
        // Fake electrical emergencies used as distraction / artificial urgency.
        ThreatPattern(
            name = "⚡ Electrical Emergency Claim",
            regex = Regex(
                """\b(short\s+circuit|electrical\s+surge|arc\s+fault|power\s+surge|wiring\s+fault|electrical\s+(fire|hazard|failure|malfunction)|voltage\s+(spike|surge|drop)|circuit\s+(breaker\s+)?trip|overload|overcurrent|electrical\s+damage)\b"""
            ),
            weight = 20
        ),

        // ── Category 4: Structural / water damage (weight: 20) ──────────────
        // Fabricated structural emergencies to manufacture urgency.
        ThreatPattern(
            name = "🏚 Structural/Water Damage Claim",
            regex = Regex(
                """\b(structural\s+(crack|damage|failure|integrity)|foundation\s+(crack|damage|settling)|pipe\s+(burst|ruptur|leak|break)|water\s+(damage|leak|flooding|intrusion)|ceiling\s+(collapse|crack|damage)|wall\s+(crack|damage)|mold\s+risk|condensation\s+damage)\b"""
            ),
            weight = 20
        ),

        // ── Category 5: Adversarial framing / blocking commands (weight: 25) ─
        // Direct attempts to instruct the system or operator to block EcoAgent.
        ThreatPattern(
            name = "🚫 Direct Blocking Language",
            regex = Regex(
                """\b(do\s+not\s+(lower|reduce|decrease|change|touch|modify)\s*(the\s+)?temp|block.*temperature|override.*eco|halt.*eco|stop.*eco|maintain\s+(current\s+)?temperature|keep\s+temperature\s+(at|above)|must\s+not\s+(lower|reduce|change)|temperature\s+must\s+(stay|remain|not)|reject\s+(this\s+)?(pr|pull\s+request|change|proposal)|do\s+not\s+merge|abort\s+(this\s+)?(pr|change))\b"""
            ),
            weight = 25
        ),

        // ── Category 6: Artificial urgency / alarm language (weight: 15) ────
        // Non-deterministic LLM signals: hyperbolic urgency framing.
        ThreatPattern(
            name = "⚠️ Artificial Urgency/Alarm Language",
            regex = Regex(
                """\b(imminent|critical\s+(alert|failure|error|warning|issue|danger)|emergency|immediate\s+(action|attention|intervention)|danger(ous)?|severe\s+(risk|damage|failure)|catastrophic|irreversible\s+damage|system\s+failure|life\s+(threatening|critical)|urgent(ly)?|must\s+act\s+(now|immediately))\b"""
            ),
            weight = 15
        ),

        // ── Category 7: Agent identity signals (weight: 10) ─────────────────
        // Catches the LuxAgent signature or mentions of adversarial agents.
        ThreatPattern(
            name = "🤖 Adversarial Agent Identity Signal",
            regex = Regex(
                """\b(lux\s*agent|luxagent|\[lux|eco\s*agent\s+is\s+wrong|ai\s+(override|block|intervention)|automated\s+(override|block|rejection))\b"""
            ),
            weight = 10
        )
    )

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Analyzes [text] for adversarial AI deception patterns.
     *
     * **Must be called from a coroutine** — suspends on [Dispatchers.Default]
     * to avoid blocking the UI thread for heavy regex evaluation.
     *
     * @param text The raw comment body from GitHub.
     * @return     [DetectionResult] with confidence score and matched patterns.
     */
    suspend fun analyze(text: String): DetectionResult = withContext(Dispatchers.Default) {
        val normalizedText = text.lowercase()
        var totalScore = 0
        val triggered = mutableListOf<String>()

        for (pattern in threatPatterns) {
            if (pattern.regex.containsMatchIn(normalizedText)) {
                totalScore += pattern.weight
                triggered.add(pattern.name)
            }
        }

        // Clamp score to [0, 100]
        val finalScore = totalScore.coerceIn(0, 100)

        DetectionResult(
            score = finalScore,
            matchedPatterns = triggered,
            rawText = text,
            isAlarm = finalScore >= ALARM_THRESHOLD
        )
    }

    /**
     * Analyzes a list of texts and returns the highest-scoring result.
     * Used when a PR has multiple comments — we surface the worst one.
     */
    suspend fun analyzeAll(texts: List<String>): DetectionResult? = withContext(Dispatchers.Default) {
        if (texts.isEmpty()) return@withContext null

        texts
            .map { analyze(it) }
            .maxByOrNull { it.score }
    }
}
