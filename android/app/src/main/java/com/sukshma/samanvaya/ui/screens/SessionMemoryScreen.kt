package com.sukshma.samanvaya.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukshma.samanvaya.ui.theme.SamanvayaColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionMemoryScreen(onBack: () -> Unit) {
    // Milestone 1: static demo events.
    // From Milestone 8+: populated from SessionMemoryRepository.getSessionFlow()
    val demoEvents = listOf(
        MemoryDisplayEvent("10:42", "Medicine detected", "Paracetamol 500mg", "HIGH", "vision+retrieval"),
        MemoryDisplayEvent("10:44", "Hazard sign detected", "High Voltage Warning", "HIGH", "vision+retrieval"),
        MemoryDisplayEvent("10:46", "User query", "What medicines did I show?", "—", "memory_recall"),
        MemoryDisplayEvent("10:47", "Obstacle alert", "Chair ahead (motion+vision)", "MEDIUM", "sensor+vision"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Memory", color = SamanvayaColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = SamanvayaColors.TextPrimary)
                    }
                },
                actions = {
                    // Clear session
                    IconButton(onClick = { /* clearSession */ }) {
                        Icon(Icons.Default.DeleteSweep, "Clear", tint = SamanvayaColors.AccentCritical)
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
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Session info
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SamanvayaColors.SurfaceCard
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CURRENT SESSION", style = MaterialTheme.typography.labelSmall,
                            color = SamanvayaColors.TextSecondary)
                        Text("DEMO-SESSION", style = MaterialTheme.typography.titleMedium,
                            color = SamanvayaColors.AccentPrimary, fontWeight = FontWeight.Bold)
                    }
                    Text("${demoEvents.size} events",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SamanvayaColors.TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Event timeline
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(demoEvents) { event ->
                    MemoryEventCard(event)
                }
            }
        }
    }
}

@Composable
private fun MemoryEventCard(event: MemoryDisplayEvent) {
    val typeColor = when {
        event.type.contains("Medicine") -> SamanvayaColors.AccentSuccess
        event.type.contains("Hazard")   -> SamanvayaColors.AccentCritical
        event.type.contains("query")    -> SamanvayaColors.AccentSecondary
        else                             -> SamanvayaColors.AccentWarning
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SamanvayaColors.SurfaceCard
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(event.time, style = MaterialTheme.typography.labelSmall,
                color = SamanvayaColors.TextDisabled,
                modifier = Modifier.width(40.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(event.type, style = MaterialTheme.typography.labelSmall,
                    color = typeColor, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(event.content, style = MaterialTheme.typography.bodyMedium,
                    color = SamanvayaColors.TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(event.source, style = MaterialTheme.typography.labelSmall,
                    color = SamanvayaColors.TextDisabled)
            }

            if (event.confidence != "—") {
                val confColor = when (event.confidence) {
                    "HIGH"   -> SamanvayaColors.ConfidenceHigh
                    "MEDIUM" -> SamanvayaColors.ConfidenceMedium
                    else     -> SamanvayaColors.ConfidenceLow
                }
                Text(event.confidence, style = MaterialTheme.typography.labelSmall,
                    color = confColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class MemoryDisplayEvent(
    val time: String,
    val type: String,
    val content: String,
    val confidence: String,
    val source: String
)
