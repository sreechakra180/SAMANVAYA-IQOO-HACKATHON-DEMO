package com.sukshma.samanvaya.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukshma.samanvaya.ui.theme.SamanvayaColors

@Composable
fun HomeScreen(
    onStartAssist: () -> Unit,
    onViewMemory:  () -> Unit,
    onDiagnostics: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SamanvayaColors.DeepIndigo)
    ) {
        // Subtle radial glow behind the CTA
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SamanvayaColors.AccentPrimary.copy(alpha = 0.12f),
                            SamanvayaColors.DeepIndigo
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Offline badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SamanvayaColors.AccentSuccess.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(SamanvayaColors.AccentSuccess, CircleShape)
                        )
                        Text(
                            "LOCAL AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = SamanvayaColors.AccentSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Settings / Diagnostics
                IconButton(onClick = onDiagnostics) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Diagnostics",
                        tint = SamanvayaColors.TextSecondary
                    )
                }
            }

            // ── Hero ───────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "SAMANVAYA",
                    style = MaterialTheme.typography.displayLarge,
                    color = SamanvayaColors.TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Text(
                    "Offline Accessibility Copilot",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SamanvayaColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Point • Ask • Understand",
                    style = MaterialTheme.typography.titleMedium,
                    color = SamanvayaColors.AccentPrimary,
                    letterSpacing = 1.5.sp
                )
            }

            // ── Primary CTA ────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick    = onStartAssist,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape      = RoundedCornerShape(20.dp),
                    colors     = ButtonDefaults.buttonColors(
                        containerColor = SamanvayaColors.AccentPrimary
                    )
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "TAP AND SPEAK",
                        style    = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick   = onViewMemory,
                        modifier  = Modifier.weight(1f).height(52.dp),
                        shape     = RoundedCornerShape(14.dp),
                        colors    = ButtonDefaults.outlinedButtonColors(
                            contentColor = SamanvayaColors.TextPrimary
                        )
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Session Memory", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ── Footer capability chips ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    Pair(Icons.Default.Visibility, "Vision"),
                    Pair(Icons.Default.Mic, "Voice"),
                    Pair(Icons.Default.Psychology, "Reasoning"),
                    Pair(Icons.Default.Storage, "Memory"),
                    Pair(Icons.Default.WifiOff, "Offline")
                ).forEach { (icon, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = SamanvayaColors.AccentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = SamanvayaColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
