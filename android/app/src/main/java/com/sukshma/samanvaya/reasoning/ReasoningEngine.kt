package com.sukshma.samanvaya.reasoning

import com.sukshma.samanvaya.memory.SessionEvent

// ─── Domain models ────────────────────────────────────────────────────────────

data class ReasoningResult(
    val success: Boolean,
    val response: String,            // Full response for TTS + display
    val summary: String,             // Short version for session memory
    val confidence: Float,           // Propagated from vision
    val latencyMs: Long,
    val model: String,
    val safetyFlags: List<String> = emptyList(),  // Any safety warnings triggered
    val error: String? = null
)

// ─── Interface ────────────────────────────────────────────────────────────────

/**
 * ReasoningEngine — local LLM abstraction.
 *
 * Implementations:
 *  - Qwen2ReasoningEngine   : Qwen2-1.5B GGUF (primary)
 *  - RuleBasedReasoningEngine: template-based fallback (no model needed)
 *  - StubReasoningEngine    : testing stub
 */
interface ReasoningEngine {
    val modelName: String
    val isReady: Boolean

    /**
     * Produces a natural language response given:
     * @param visionContext  what the model saw (from VisionResult.text)
     * @param retrievedContext top-k RAG chunks (empty if RAG miss)
     * @param memory          rolling session context
     * @param userQuery       what the user asked
     * @param visionConfidence confidence of the vision result (affects output policy)
     */
    suspend fun reason(
        visionContext: String,
        retrievedContext: List<String>,
        memory: List<SessionEvent>,
        userQuery: String,
        visionConfidence: Float
    ): ReasoningResult

    suspend fun initialize(): Boolean
    fun release()
}

// ─── Confidence + Safety Policy ───────────────────────────────────────────────

object ResponsePolicy {

    const val HIGH_CONFIDENCE_THRESHOLD   = 0.85f
    const val MEDIUM_CONFIDENCE_THRESHOLD = 0.60f

    fun shouldRespondWithDetails(confidence: Float) = confidence >= HIGH_CONFIDENCE_THRESHOLD
    fun shouldRespondWithCaution(confidence: Float) = confidence >= MEDIUM_CONFIDENCE_THRESHOLD

    /** Medical safety disclaimer — always appended to medicine responses */
    const val MEDICAL_DISCLAIMER =
        "Please verify with a pharmacist or doctor before taking any medication."

    /** Response when confidence is too low */
    fun lowConfidenceResponse(query: String) =
        "I can see something, but I am not confident enough to identify it accurately. " +
        "Please try pointing the camera closer or in better lighting."

    /** Response when item is recognized but not in knowledge base */
    fun noKbResponse(label: String) =
        "I can see $label, but I don't have verified information about it in my offline database. " +
        "Please consult a professional for guidance."

    /**
     * Detect if a query is asking for medical advice we must NOT provide.
     * Returns the restricted topic name, or null if safe.
     */
    fun detectRestrictedMedicalQuery(query: String): String? {
        val lower = query.lowercase()
        return when {
            lower.contains("diagnos")   -> "diagnosis"
            lower.contains("treat")     -> "treatment"
            lower.contains("prescrib")  -> "prescription"
            lower.contains("side effect") && lower.contains("take") -> "medical advice"
            lower.contains("overdos")   -> "overdose guidance"
            else                        -> null
        }
    }
}
