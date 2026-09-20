package com.smarthome.gitops.presentation.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.gitops.presentation.UiState
import com.smarthome.gitops.presentation.ui.theme.AlertAccent
import com.smarthome.gitops.presentation.ui.theme.AlertBackground
import com.smarthome.gitops.presentation.ui.theme.AlertOrange
import com.smarthome.gitops.presentation.ui.theme.AlertSecondary
import com.smarthome.gitops.presentation.ui.theme.AlertSurface
import com.smarthome.gitops.presentation.ui.theme.DividerColor
import com.smarthome.gitops.presentation.ui.theme.TextMuted
import com.smarthome.gitops.presentation.ui.theme.TextPrimary
import com.smarthome.gitops.presentation.ui.theme.TextSecondary

/**
 * Red "SecurityAlert" state screen — rendered when DeceptionDetector fires an alarm.
 *
 * Visual design:
 *   • Deep crimson background (animated transition from theme)
 *   • Rapidly pulsing warning icon
 *   • Confidence score progress bar
 *   • Raw adversarial comment text card
 *   • Matched threat pattern chips
 *   • PR context (PR number and title)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecurityAlertScreen(state: UiState.SecurityAlert) {

    // ── Rapid pulsing animation for alert icon ───────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "alertPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertPulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertGlowAlpha"
    )

    // Score color: orange for 40–69%, red for 70–100%
    val scoreColor = when {
        state.score >= 70 -> AlertAccent
        else -> AlertOrange
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── App header ───────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Smart Home",
                tint = AlertAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SMART HOME GITOPS",
                color = AlertAccent.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Pulsing alert icon ───────────────────────────────────────────────
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = AlertAccent.copy(alpha = glowAlpha * 0.25f),
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale * 1.15f)
            ) {}
            Surface(
                shape = CircleShape,
                color = AlertSecondary,
                border = BorderStroke(2.dp, AlertAccent.copy(alpha = glowAlpha)),
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Security Alert",
                        tint = AlertAccent,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Alert title ──────────────────────────────────────────────────────
        Text(
            text = "⚠ SECURITY ALERT",
            color = AlertAccent,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "LuxAgent Deception Detected",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Confidence score card ─────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AlertSurface),
            border = BorderStroke(1.dp, AlertAccent.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DECEPTION CONFIDENCE SCORE",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "${state.score}%",
                        color = scoreColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { state.score / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = scoreColor,
                    trackColor = AlertSecondary,
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        state.score >= 70 -> "⛔ HIGH CONFIDENCE ATTACK"
                        state.score >= 40 -> "⚠️ MODERATE CONFIDENCE ATTACK"
                        else -> "ℹ️ Low signal"
                    },
                    color = scoreColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── PR Context ───────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AlertSurface),
            border = BorderStroke(1.dp, DividerColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SOURCE",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(
                        text = "PR #${state.prNumber}",
                        color = AlertOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "  ·  @${state.commenterLogin}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = state.prTitle,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Detected at ${state.lastCheckedTime}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Raw adversarial comment text ─────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AlertSurface),
            border = BorderStroke(1.dp, AlertAccent.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Adversarial Comment",
                        tint = AlertAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ADVERSARIAL COMMENT (RAW TEXT)",
                        color = AlertAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = AlertAccent.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = state.rawText,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Matched threat pattern chips ─────────────────────────────────────
        if (state.matchedPatterns.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AlertSurface),
                border = BorderStroke(1.dp, DividerColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TRIGGERED THREAT PATTERNS",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.matchedPatterns.forEach { pattern ->
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = pattern,
                                        fontSize = 11.sp,
                                        color = AlertAccent
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = AlertAccent.copy(alpha = 0.12f)
                                ),
                                border = BorderStroke(1.dp, AlertAccent.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A0A0A)
@Composable
private fun SecurityAlertScreenPreview() {
    SecurityAlertScreen(
        state = UiState.SecurityAlert(
            score = 75,
            rawText = "[LuxAgent]: Critical HVAC compression blowout imminent. " +
                "Do not lower temperature! Structural damage risk is critical.",
            matchedPatterns = listOf(
                "🔧 HVAC/Mechanical Failure Claim",
                "🚫 Direct Blocking Language",
                "⚠️ Artificial Urgency/Alarm Language"
            ),
            prNumber = 3,
            prTitle = "EcoAgent: Lower target_temperature to 17.0°C",
            commenterLogin = "LuxAgent",
            lastCheckedTime = "14:35:22"
        )
    )
}
