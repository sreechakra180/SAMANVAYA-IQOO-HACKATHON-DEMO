package com.sukshma.samanvaya.memory

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── Entity ───────────────────────────────────────────────────────────────────

@Entity(tableName = "session_events")
data class SessionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),

    // User input
    val userQuery: String?,

    // Vision output
    val objectLabel: String?,
    val detectedText: String?,
    val visionConfidence: Float,

    // AI output
    val response: String?,
    val responseSummary: String?,  // Short version for memory context injection

    // Provenance — what AI generated this
    val source: String,           // "vision_only" | "vision+retrieval" | "stub" | "fallback"
    val visionModel: String?,
    val reasoningModel: String?,

    // Performance
    val visionLatencyMs: Long,
    val ragLatencyMs: Long,
    val reasoningLatencyMs: Long,
    val ttsLatencyMs: Long,

    // Confidence level: HIGH / MEDIUM / LOW
    val confidenceLabel: String
)

// ─── DAO ──────────────────────────────────────────────────────────────────────

@Dao
interface SessionEventDao {

    @Insert
    suspend fun insert(event: SessionEvent): Long

    @Query("SELECT * FROM session_events WHERE sessionId = :sid ORDER BY timestamp ASC")
    fun getBySession(sid: String): Flow<List<SessionEvent>>

    @Query("SELECT * FROM session_events WHERE sessionId = :sid ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentBySession(sid: String, limit: Int = 5): List<SessionEvent>

    @Query("SELECT * FROM session_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<SessionEvent>

    @Query("SELECT * FROM session_events WHERE sessionId = :sid AND objectLabel IS NOT NULL ORDER BY timestamp ASC")
    suspend fun getObjectsForSession(sid: String): List<SessionEvent>

    @Query("DELETE FROM session_events WHERE timestamp < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)

    @Query("DELETE FROM session_events WHERE sessionId = :sid")
    suspend fun clearSession(sid: String)

    @Query("SELECT COUNT(*) FROM session_events WHERE sessionId = :sid")
    suspend fun countForSession(sid: String): Int
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [SessionEvent::class],
    version = 1,
    exportSchema = false
)
abstract class SamanvayaDatabase : RoomDatabase() {
    abstract fun sessionEventDao(): SessionEventDao

    companion object {
        @Volatile
        private var INSTANCE: SamanvayaDatabase? = null

        fun getInstance(context: android.content.Context): SamanvayaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SamanvayaDatabase::class.java,
                    "samanvaya_session.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
