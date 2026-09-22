package com.sukshma.samanvaya.ui.theme

import androidx.compose.ui.graphics.Color

// SAMANVAYA brand palette — dark cinematic, high contrast, accessibility-first
object SamanvayaColors {
    // Primary
    val DeepIndigo        = Color(0xFF0D0F1A)  // Background — near-black
    val SurfaceCard       = Color(0xFF141726)  // Card surface
    val SurfaceElevated   = Color(0xFF1C1F35)  // Elevated surface

    // Accent — electric blue (visible, energetic, accessible)
    val AccentPrimary     = Color(0xFF4F8EF7)  // Primary action
    val AccentSecondary   = Color(0xFF7B61FF)  // Secondary accent — purple
    val AccentSuccess     = Color(0xFF3DDC84)  // Online / success — green
    val AccentWarning     = Color(0xFFFFB547)  // Warning — amber
    val AccentCritical    = Color(0xFFFF5252)  // Hazard / critical — red

    // Offline indicator
    val OfflineBadge      = Color(0xFF3DDC84)  // Green = offline/local is GOOD
    val CloudBadge        = Color(0xFFFF5252)  // Red = cloud = not allowed

    // Text
    val TextPrimary       = Color(0xFFF0F2FF)  // High contrast white
    val TextSecondary     = Color(0xFF9BA4C4)  // Muted text
    val TextDisabled      = Color(0xFF4A5070)  // Disabled text

    // Confidence indicators
    val ConfidenceHigh    = Color(0xFF3DDC84)  // >= 85%
    val ConfidenceMedium  = Color(0xFFFFB547)  // 60-84%
    val ConfidenceLow     = Color(0xFFFF5252)  // < 60%
}
