package com.sukshma.samanvaya.rag

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "JsonRagEngine"

/**
 * JsonRagEngine — simple offline JSON-based retrieval.
 *
 * For the hackathon demo this is the primary RAG path on Android.
 * It loads pre-built JSON knowledge bases from assets/ and performs
 * keyword + fuzzy matching to retrieve relevant chunks.
 *
 * This is NOT a vector search — it's a reliable keyword lookup.
 * The Python FAISS backend provides semantic search for the dashboard.
 *
 * Knowledge bases loaded:
 *   assets/kb_medicines.json
 *   assets/kb_hazards.json
 *   assets/kb_navigation.json
 */
class JsonRagEngine(private val context: Context) : RagEngine {

    private val gson = Gson()
    private val allChunks = mutableListOf<RagChunk>()

    override var isReady: Boolean = false
    override val indexSize: Int get() = allChunks.size

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            loadKnowledgeBase("kb_medicines.json", "medicine")
            loadKnowledgeBase("kb_hazards.json", "hazard")
            loadKnowledgeBase("kb_navigation.json", "navigation")
            isReady = true
            Log.d(TAG, "RAG initialized: ${allChunks.size} chunks loaded")
            true
        } catch (e: Exception) {
            Log.e(TAG, "RAG init failed: ${e.message}")
            false
        }
    }

    private fun loadKnowledgeBase(assetFile: String, domain: String) {
        try {
            val json = context.assets.open(assetFile).bufferedReader().readText()
            val type = object : TypeToken<List<JsonKbEntry>>() {}.type
            val entries: List<JsonKbEntry> = gson.fromJson(json, type)
            entries.forEach { entry ->
                allChunks.add(
                    RagChunk(
                        id         = "${domain}_${entry.id}",
                        content    = entry.content,
                        domain     = domain,
                        source     = entry.source,
                        confidence = 1f
                    )
                )
            }
            Log.d(TAG, "Loaded $assetFile: ${entries.size} entries")
        } catch (e: Exception) {
            Log.w(TAG, "Could not load $assetFile: ${e.message} — continuing without it")
        }
    }

    override suspend fun retrieve(query: String, topK: Int): RagResult =
        withContext(Dispatchers.Default) {
            val startMs = System.currentTimeMillis()

            if (allChunks.isEmpty()) {
                return@withContext RagResult(
                    success   = false,
                    chunks    = emptyList(),
                    latencyMs = System.currentTimeMillis() - startMs,
                    query     = query,
                    indexSize = 0
                )
            }

            val queryLower = query.lowercase()
            val queryTokens = queryLower.split(Regex("\\s+")).filter { it.length > 2 }

            // Score each chunk by keyword overlap
            val scored = allChunks.map { chunk ->
                val contentLower = chunk.content.lowercase()
                val score = queryTokens.sumOf { token ->
                    val pts: Int = when {
                        contentLower.contains(token) -> 2
                        levenshteinSimilar(token, contentLower) -> 1
                        else -> 0
                    }
                    pts
                }.toFloat()
                Pair(chunk, score)
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(topK)

            val latency = System.currentTimeMillis() - startMs
            Log.d(TAG, "RAG query '${query.take(50)}' → ${scored.size} hits in ${latency}ms")

            RagResult(
                success   = true,
                chunks    = scored.map { it.first },
                latencyMs = latency,
                query     = query,
                indexSize = allChunks.size
            )
        }

    /** Simple check: does the content contain a word within edit-distance 2 of token? */
    private fun levenshteinSimilar(token: String, content: String): Boolean {
        if (token.length < 4) return false
        return content.split(Regex("\\s+")).any { word ->
            word.length >= token.length - 2 && levenshtein(token, word) <= 2
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i-1][j-1]
                       else minOf(dp[i-1][j], dp[i][j-1], dp[i-1][j-1]) + 1
        }
        return dp[a.length][b.length]
    }

    data class JsonKbEntry(
        val id: String,
        val content: String,
        val source: String
    )
}
