package com.sukshma.samanvaya.rag

// ─── Domain models ────────────────────────────────────────────────────────────

data class RagChunk(
    val id: String,
    val content: String,
    val domain: String,         // "medicine" | "hazard" | "navigation" | "accessibility"
    val source: String,         // e.g. "WHO Essential Medicines List"
    val confidence: Float = 1f
)

data class RagResult(
    val success: Boolean,
    val chunks: List<RagChunk>,
    val latencyMs: Long,
    val query: String,
    val indexSize: Int
)

// ─── Interface ────────────────────────────────────────────────────────────────

/**
 * RagEngine — offline retrieval-augmented generation interface.
 *
 * Implementations:
 *  - FaissRagEngine : production RAG with FAISS index + sentence-transformers
 *                     (runs via Python backend or pre-computed on device)
 *  - JsonRagEngine  : simple JSON lookup (hackathon quick path)
 *  - StubRagEngine  : testing stub
 *
 * The Android app uses JsonRagEngine for the hackathon demo.
 * The Python backend (backend/rag_server.py) uses FAISS + sentence-transformers
 * for the Office Kit dashboard and richer retrieval.
 */
interface RagEngine {
    val isReady: Boolean
    val indexSize: Int

    suspend fun retrieve(query: String, topK: Int = 3): RagResult
    suspend fun initialize(): Boolean
}
