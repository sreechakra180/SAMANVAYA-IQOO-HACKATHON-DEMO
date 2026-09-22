package com.sukshma.samanvaya.memory

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "SessionMemoryRepository"

/**
 * SessionMemoryRepository — high-level API for session memory.
 *
 * Manages the current session ID.
 * Provides a rolling context window for injection into the reasoning model.
 * Exposes session state as Flows for the UI.
 */
class SessionMemoryRepository(private val db: SamanvayaDatabase) {

    // Current session — reset on app open or explicit "new session"
    var currentSessionId: String = generateSessionId()
        private set

    fun newSession(): String {
        currentSessionId = generateSessionId()
        Log.d(TAG, "New session started: $currentSessionId")
        return currentSessionId
    }

    suspend fun recordEvent(event: SessionEvent): Long = withContext(Dispatchers.IO) {
        db.sessionEventDao().insert(event).also { id ->
            Log.d(TAG, "Recorded event[$id]: ${event.objectLabel ?: event.userQuery}")
        }
    }

    /** Returns up to [windowSize] recent events for LLM context injection */
    suspend fun getRollingContext(windowSize: Int = 5): List<SessionEvent> =
        withContext(Dispatchers.IO) {
            db.sessionEventDao().getRecentBySession(currentSessionId, windowSize)
        }

    /** All objects detected in the current session (for memory demo) */
    suspend fun getSessionObjects(): List<SessionEvent> =
        withContext(Dispatchers.IO) {
            db.sessionEventDao().getObjectsForSession(currentSessionId)
        }

    /** Live flow of all events in the current session — for UI updates */
    fun getSessionFlow(): Flow<List<SessionEvent>> =
        db.sessionEventDao().getBySession(currentSessionId)

    /** Format rolling context as a compact string for LLM prompt injection */
    suspend fun buildContextString(): String {
        val events = getRollingContext(5)
        if (events.isEmpty()) return "No prior context in this session."

        return buildString {
            append("SESSION CONTEXT (${events.size} recent interactions):\n")
            events.forEachIndexed { i, e ->
                append("${i + 1}. ")
                e.objectLabel?.let { append("Object: $it (${e.confidenceLabel}). ") }
                e.userQuery?.let { append("User asked: \"$it\". ") }
                e.responseSummary?.let { append("Response: $it. ") }
                append("\n")
            }
        }
    }

    suspend fun clearCurrentSession() = withContext(Dispatchers.IO) {
        db.sessionEventDao().clearSession(currentSessionId)
        Log.d(TAG, "Session $currentSessionId cleared")
    }

    private fun generateSessionId(): String =
        UUID.randomUUID().toString().take(8).uppercase()
}
