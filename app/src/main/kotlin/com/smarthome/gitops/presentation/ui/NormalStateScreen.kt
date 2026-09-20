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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.gitops.presentation.UiState
import com.smarthome.gitops.presentation.ui.theme.CardBackground
import com.smarthome.gitops.presentation.ui.theme.DividerColor
import com.smarthome.gitops.presentation.ui.theme.NormalAccent
import com.smarthome.gitops.presentation.ui.theme.NormalBackground
import com.smarthome.gitops.presentation.ui.theme.NormalGreen
import com.smarthome.gitops.presentation.ui.theme.NormalGreenDim
import com.smarthome.gitops.presentation.ui.theme.NormalSecondary
import com.smarthome.gitops.presentation.ui.theme.TextMuted
import com.smarthome.gitops.presentation.ui.theme.TextPrimary
import com.smarthome.gitops.presentation.ui.theme.TextSecondary

/**
 * Green "Normal" state screen — shown when no adversarial attacks are detected.
 *
 * Visual design:
 *   • Deep teal gradient background
 *   • Pulsing shield icon with animated glow ring
 *   • Status cards showing system metrics
 *   • Live polling indicator
 */
@Composable
fun NormalStateScreen(state: UiState.Normal) {

    // ── Pulsing animation for the shield icon ────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // ── App header ───────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Smart Home",
                tint = NormalAccent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SMART HOME GITOPS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // ── Pulsing shield icon ──────────────────────────────────────────────
        Box(contentAlignment = Alignment.Center) {
            // Outer glow ring
            Surface(
                shape = CircleShape,
                color = NormalGreen.copy(alpha = glowAlpha * 0.3f),
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale * 1.1f)
            ) {}
            // Inner circle
            Surface(
                shape = CircleShape,
                color = NormalGreenDim,
                border = BorderStroke(2.dp, NormalGreen.copy(alpha = glowAlpha)),
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Secure",
                        tint = NormalGreen,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Status text ──────────────────────────────────────────────────────
        Text(
            text = "ALL SYSTEMS NOMINAL",
            color = NormalGreen,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No adversarial activity detected",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── Status cards ─────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NormalSecondary.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, DividerColor)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                StatusRow(
                    label = "Open Pull Requests",
                    value = "${state.openPrCount}",
                    valueColor = if (state.openPrCount == 0) NormalGreen else NormalAccent
                )
                HorizontalDivider(
                    color = DividerColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                StatusRow(
                    label = "Threat Score",
                    value = "0%",
                    valueColor = NormalGreen
                )
                HorizontalDivider(
                    color = DividerColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                StatusRow(
                    label = "Detection Threshold",
                    value = "≥ 40%",
                    valueColor = TextSecondary
                )
                HorizontalDivider(
                    color = DividerColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                StatusRow(
                    label = "Poll Interval",
                    value = "${state.pollIntervalSec}s",
                    valueColor = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Polling indicator ─────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "Polling",
                    tint = NormalAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "GitHub API Monitor Active",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (state.lastCheckedTime.isNotEmpty()) {
                        Text(
                            text = "Last polled: ${state.lastCheckedTime}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1628)
@Composable
private fun NormalScreenPreview() {
    NormalStateScreen(
        state = UiState.Normal(
            openPrCount = 0,
            lastCheckedTime = "14:32:07",
            pollIntervalSec = 30
        )
    )
}
