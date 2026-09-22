package com.sukshma.samanvaya.vision

import android.graphics.Bitmap

// ─── Domain models ────────────────────────────────────────────────────────────

data class DetectedObject(
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox? = null
)

data class BoundingBox(
    val left: Float, val top: Float,
    val right: Float, val bottom: Float
)

/**
 * Result of a local vision inference call.
 *
 * [success]     — whether inference completed without error
 * [text]        — raw textual description / OCR output from the model
 * [objects]     — detected objects with confidence
 * [confidence]  — overall confidence for the primary result (0.0–1.0)
 * [latencyMs]   — actual measured inference latency
 * [model]       — which model was used (for display in diagnostics)
 * [source]      — "vision_only" | "vision+retrieval" | "fallback"
 * [error]       — error message if !success
 */
data class VisionResult(
    val success: Boolean,
    val text: String?,
    val objects: List<DetectedObject> = emptyList(),
    val confidence: Float = 0f,
    val latencyMs: Long = 0L,
    val model: String = "unknown",
    val source: String = "vision_only",
    val error: String? = null
) {
    /** Confidence label for UI display */
    val confidenceLabel: String get() = when {
        confidence >= 0.85f -> "HIGH"
        confidence >= 0.60f -> "MEDIUM"
        else                -> "LOW"
    }

    /** Whether confidence is sufficient to surface a result to the user */
    val isActionable: Boolean get() = confidence >= 0.60f
}

// ─── Engine interface ──────────────────────────────────────────────────────────

/**
 * VisionEngine — abstraction over local vision models.
 *
 * Implementations:
 *  - Florence2Engine  : fast captioning + object detection (fallback)
 *  - MiniCPMEngine    : deep visual understanding (primary)
 *  - StubVisionEngine : testing / demo stub
 */
interface VisionEngine {
    val modelName: String
    val isReady: Boolean

    /** Analyse a captured frame, optionally guided by a user query */
    suspend fun analyze(image: Bitmap, query: String? = null): VisionResult

    /** Called during app startup — loads model weights into memory */
    suspend fun initialize(): Boolean

    /** Release model resources */
    fun release()
}
