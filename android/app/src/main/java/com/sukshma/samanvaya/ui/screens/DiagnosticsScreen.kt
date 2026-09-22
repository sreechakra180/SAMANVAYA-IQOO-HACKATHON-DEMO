package com.sukshma.samanvaya.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukshma.samanvaya.ui.theme.SamanvayaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics", color = SamanvayaColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = SamanvayaColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SamanvayaColors.DeepIndigo)
            )
        },
        containerColor = SamanvayaColors.DeepIndigo
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Network status — the most important diagnostic
            DiagCard("NETWORK STATUS") {
                DiagRow("Internet calls", "0", SamanvayaColors.AccentSuccess)
                DiagRow("External connections", "BLOCKED", SamanvayaColors.AccentSuccess)
                DiagRow("Status", "OFFLINE (LOCAL ONLY)", SamanvayaColors.AccentSuccess)
                Text(
                    "⚠ Internet permission is declared ONLY for local WebSocket (Office Kit dashboard). " +
                    "No AI inference calls leave this device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = SamanvayaColors.TextDisabled
                )
            }

            // Vision model
            DiagCard("VISION ENGINE") {
                DiagRow("Model", "Stub (NOT REAL — Milestone 4)", SamanvayaColors.AccentWarning)
                DiagRow("Status", "READY", SamanvayaColors.AccentSuccess)
                DiagRow("Last latency", "--ms", SamanvayaColors.TextSecondary)
                DiagRow("NPU", "Pending real model load", SamanvayaColors.TextDisabled)
            }

            // STT
            DiagCard("SPEECH RECOGNITION") {
                DiagRow("Model", "Whisper-tiny (pending)", SamanvayaColors.AccentWarning)
                DiagRow("Android STT", "Available", SamanvayaColors.AccentSuccess)
                DiagRow("Languages", "EN, HI (TE optional)", SamanvayaColors.TextSecondary)
            }

            // RAG
            DiagCard("OFFLINE RAG") {
                DiagRow("Engine", "JsonRagEngine (keyword)", SamanvayaColors.AccentPrimary)
                DiagRow("Index size", "0 chunks (KB not yet in assets)", SamanvayaColors.AccentWarning)
                DiagRow("FAISS (Python)", "Ready via backend/", SamanvayaColors.AccentSuccess)
            }

            // Reasoning
            DiagCard("REASONING ENGINE") {
                DiagRow("Model", "Rule-Based Fallback", SamanvayaColors.AccentWarning)
                DiagRow("Qwen2-1.5B", "Pending (Milestone 6)", SamanvayaColors.TextDisabled)
            }

            // Sensors
            DiagCard("SENSORS") {
                DiagRow("Accelerometer", "Pending init", SamanvayaColors.TextSecondary)
                DiagRow("Gyroscope", "Pending init", SamanvayaColors.TextSecondary)
                DiagRow("Haptics", "Available", SamanvayaColors.AccentSuccess)
            }

            // Memory
            DiagCard("SESSION MEMORY") {
                DiagRow("Database", "SQLite (Room)", SamanvayaColors.AccentSuccess)
                DiagRow("Events this session", "0", SamanvayaColors.TextSecondary)
                DiagRow("Retention", "5-turn rolling window", SamanvayaColors.AccentPrimary)
            }

            // Milestone tracker
            DiagCard("BUILD MILESTONES") {
                listOf(
                    Pair("M1: Project shell + all screens", true),
                    Pair("M2: CameraX live preview", false),
                    Pair("M3: Voice input (AudioRecord)", false),
                    Pair("M4: Local VLM (MiniCPM-V / Florence-2)", false),
                    Pair("M5: Offline RAG (JSON KB)", false),
                    Pair("M6: Local LLM (Qwen2-1.5B)", false),
                    Pair("M7: TTS + full audio loop", false),
                    Pair("M8: Session memory integration", false),
                    Pair("M9: Sensors + haptics", false),
                    Pair("M10: Airplane mode validation", false),
                    Pair("M11: Office Kit dashboard", false),
                    Pair("M12: Demo hardening", false),
                ).forEach { (label, done) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(8.dp).background(
                                if (done) SamanvayaColors.AccentSuccess else SamanvayaColors.TextDisabled,
                                CircleShape
                            )
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (done) SamanvayaColors.TextPrimary else SamanvayaColors.TextDisabled
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DiagCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SamanvayaColors.SurfaceCard
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = SamanvayaColors.AccentPrimary,
                fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = SamanvayaColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.labelSmall,
            color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}
