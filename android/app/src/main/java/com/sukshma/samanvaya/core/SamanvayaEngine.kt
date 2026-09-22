package com.sukshma.samanvaya.core

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import com.sukshma.samanvaya.accessibility.HapticEngine
import com.sukshma.samanvaya.accessibility.HapticLevel
import com.sukshma.samanvaya.memory.SessionEvent
import com.sukshma.samanvaya.memory.SessionMemoryRepository
import com.sukshma.samanvaya.memory.SamanvayaDatabase
import com.sukshma.samanvaya.rag.JsonRagEngine
import com.sukshma.samanvaya.reasoning.RuleBasedReasoningEngine
import com.sukshma.samanvaya.sensors.MotionIntensity
import com.sukshma.samanvaya.sensors.SensorContextProvider
import com.sukshma.samanvaya.speech.AndroidTTSEngine
import com.sukshma.samanvaya.speech.SupportedLanguage
import com.sukshma.samanvaya.vision.StubVisionEngine
import com.sukshma.samanvaya.vision.VisionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "SamanvayaEngine"

// ─── UI State ─────────────────────────────────────────────────────────────────

enum class EngineStatus { LOADING, READY, PROCESSING, ERROR }

data class ProcessingResult(
    val id: Long = 0,
    val objectLabel: String?,
    val response: String,
    val confidence: Float,
    val confidenceLabel: String,
    val visionModel: String,
    val reasoningModel: String,
    val source: String,
    val visionLatencyMs: Long,
    val ragLatencyMs: Long,
    val reasoningLatencyMs: Long,
    val isSimulated: Boolean,        // true if any stub/fallback used
    val safetyFlags: List<String>
)

data class DiagnosticsState(
    val networkOffline: Boolean = true,
    val externalCallCount: Int = 0,
    val visionModelName: String = "Not loaded",
    val visionModelReady: Boolean = false,
    val ragReady: Boolean = false,
    val ragIndexSize: Int = 0,
    val reasoningModelName: String = "Not loaded",
    val sensorAvailable: Boolean = false,
    val lastVisionLatencyMs: Long = 0,
    val lastRagLatencyMs: Long = 0,
    val lastReasoningLatencyMs: Long = 0
)

// ─── Main Engine ──────────────────────────────────────────────────────────────

/**
 * SamanvayaEngine — central coordinator for all AI pipelines.
 *
 * Wires together:
 *   VisionEngine → RagEngine → ReasoningEngine → TTS → HapticEngine → Memory
 *
 * Designed for use from a ViewModel via coroutines.
 * All heavy operations run on Dispatchers.IO or Dispatchers.Default.
 */
class SamanvayaEngine(private val app: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── Component instances ─────────────────────────────────────────────────
    val visionEngine: VisionEngine = StubVisionEngine(app)
    // TODO Milestone 4: replace with MiniCPMEngine(app)

    val ragEngine = JsonRagEngine(app)
    val reasoningEngine = RuleBasedReasoningEngine()
    // TODO Milestone 6: replace with Qwen2ReasoningEngine(app)

    val ttsEngine = AndroidTTSEngine(app)
    val hapticEngine = HapticEngine(app)
    val sensorProvider = SensorContextProvider(app)

    val db = SamanvayaDatabase.getInstance(app)
    val memory = SessionMemoryRepository(db)

    // ── State ───────────────────────────────────────────────────────────────
    private val _status = MutableStateFlow(EngineStatus.LOADING)
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    private val _diagnostics = MutableStateFlow(DiagnosticsState())
    val diagnostics: StateFlow<DiagnosticsState> = _diagnostics.asStateFlow()

    private val _lastResult = MutableStateFlow<ProcessingResult?>(null)
    val lastResult: StateFlow<ProcessingResult?> = _lastResult.asStateFlow()

    var activeLanguage: SupportedLanguage = SupportedLanguage.HINDI

    // ── Initialization ──────────────────────────────────────────────────────

    fun initialize() {
        scope.launch {
            Log.d(TAG, "Initializing SAMANVAYA engine...")
            _status.value = EngineStatus.LOADING

            // TTS — synchronous callback
            ttsEngine.initialize { ready ->
                Log.d(TAG, "TTS ready: $ready")
                updateDiagnostics()
            }

            // Vision
            val visionOk = visionEngine.initialize()
            Log.d(TAG, "Vision engine ready: $visionOk (${visionEngine.modelName})")

            // RAG
            val ragOk = ragEngine.initialize()
            Log.d(TAG, "RAG ready: $ragOk (${ragEngine.indexSize} chunks)")

            // Reasoning (rule-based is always ready)
            reasoningEngine.initialize()

            // Sensors
            sensorProvider.start()

            updateDiagnostics()
            _status.value = EngineStatus.READY

            // Welcome audio
            ttsEngine.speak(
                "SAMANVAYA is ready. Point the camera and speak your question.",
                SupportedLanguage.ENGLISH
            )
        }
    }

    // ── Main pipeline ────────────────────────────────────────────────────────

    suspend fun process(frame: Bitmap, query: String): ProcessingResult {
        _status.value = EngineStatus.PROCESSING

        return try {
            val motionCtx = sensorProvider.currentMotionContext()

            // ── Step 1: Vision ──────────────────────────────────────────────
            val visionResult = visionEngine.analyze(frame, query)
            Log.d(TAG, "[VISION] model=${visionResult.model} latency=${visionResult.latencyMs}ms conf=${visionResult.confidence}")

            if (!visionResult.success) {
                _status.value = EngineStatus.READY
                return errorResult("Vision inference failed: ${visionResult.error}")
            }

            // ── Step 2: RAG ─────────────────────────────────────────────────
            val ragQuery = buildRagQuery(visionResult.text, query)
            val ragResult = ragEngine.retrieve(ragQuery, topK = 3)
            Log.d(TAG, "[RAG] hits=${ragResult.chunks.size} latency=${ragResult.latencyMs}ms")

            // ── Step 3: Reasoning ───────────────────────────────────────────
            val memoryContext = memory.getRollingContext(5)
            val reasoningResult = reasoningEngine.reason(
                visionContext     = visionResult.text ?: "",
                retrievedContext  = ragResult.chunks.map { it.content },
                memory            = memoryContext,
                userQuery         = query,
                visionConfidence  = visionResult.confidence
            )
            Log.d(TAG, "[REASONING] model=${reasoningResult.model} latency=${reasoningResult.latencyMs}ms")

            // ── Step 4: Haptic feedback ─────────────────────────────────────
            val hapticLevel = when {
                reasoningResult.safetyFlags.isNotEmpty() -> HapticLevel.CRITICAL
                motionCtx.isMovingFast                   -> HapticLevel.WARNING
                else                                      -> HapticLevel.INFO
            }
            hapticEngine.vibrate(hapticLevel)

            // ── Step 5: TTS ─────────────────────────────────────────────────
            val startTts = System.currentTimeMillis()
            ttsEngine.speak(reasoningResult.response, activeLanguage)
            val ttsLatency = System.currentTimeMillis() - startTts

            // ── Step 6: Memory ──────────────────────────────────────────────
            val savedId = memory.recordEvent(
                SessionEvent(
                    sessionId          = memory.currentSessionId,
                    userQuery          = query,
                    objectLabel        = extractObjectLabel(visionResult.text),
                    detectedText       = visionResult.text,
                    visionConfidence   = visionResult.confidence,
                    response           = reasoningResult.response,
                    responseSummary    = reasoningResult.summary,
                    source             = visionResult.source,
                    visionModel        = visionResult.model,
                    reasoningModel     = reasoningResult.model,
                    visionLatencyMs    = visionResult.latencyMs,
                    ragLatencyMs       = ragResult.latencyMs,
                    reasoningLatencyMs = reasoningResult.latencyMs,
                    ttsLatencyMs       = ttsLatency,
                    confidenceLabel    = visionResult.confidenceLabel
                )
            )

            // ── Step 7: Update diagnostics ──────────────────────────────────
            _diagnostics.value = _diagnostics.value.copy(
                lastVisionLatencyMs    = visionResult.latencyMs,
                lastRagLatencyMs       = ragResult.latencyMs,
                lastReasoningLatencyMs = reasoningResult.latencyMs
            )

            val result = ProcessingResult(
                id                 = savedId,
                objectLabel        = extractObjectLabel(visionResult.text),
                response           = reasoningResult.response,
                confidence         = visionResult.confidence,
                confidenceLabel    = visionResult.confidenceLabel,
                visionModel        = visionResult.model,
                reasoningModel     = reasoningResult.model,
                source             = visionResult.source,
                visionLatencyMs    = visionResult.latencyMs,
                ragLatencyMs       = ragResult.latencyMs,
                reasoningLatencyMs = reasoningResult.latencyMs,
                isSimulated        = visionResult.model.contains("Stub", true) ||
                                     visionResult.model.contains("SIMULATED", true),
                safetyFlags        = reasoningResult.safetyFlags
            )

            _lastResult.value = result
            _status.value = EngineStatus.READY
            result

        } catch (e: Exception) {
            Log.e(TAG, "Pipeline error: ${e.message}", e)
            _status.value = EngineStatus.ERROR
            errorResult("Processing error: ${e.message}")
        }
    }

    fun newSession() {
        memory.newSession()
        NetworkGuard.reset()
        _lastResult.value = null
    }

    fun shutdown() {
        ttsEngine.shutdown()
        visionEngine.release()
        reasoningEngine.release()
        sensorProvider.stop()
    }

    private fun updateDiagnostics() {
        _diagnostics.value = DiagnosticsState(
            networkOffline      = true,  // We enforce offline — internet = local WebSocket only
            externalCallCount   = NetworkGuard.getExternalCallCount(),
            visionModelName     = visionEngine.modelName,
            visionModelReady    = visionEngine.isReady,
            ragReady            = ragEngine.isReady,
            ragIndexSize        = ragEngine.indexSize,
            reasoningModelName  = reasoningEngine.modelName,
            sensorAvailable     = sensorProvider.currentMotionContext().sensorAvailable
        )
    }

    private fun buildRagQuery(visionText: String?, userQuery: String): String =
        listOfNotNull(visionText?.take(100), userQuery).joinToString(" ")

    private fun extractObjectLabel(text: String?): String? {
        if (text.isNullOrBlank()) return null
        // Simple heuristic: take first sentence or first 60 chars
        return text.split(".").firstOrNull()?.trim()?.take(60)
    }

    private fun errorResult(msg: String) = ProcessingResult(
        objectLabel        = null,
        response           = msg,
        confidence         = 0f,
        confidenceLabel    = "LOW",
        visionModel        = visionEngine.modelName,
        reasoningModel     = reasoningEngine.modelName,
        source             = "error",
        visionLatencyMs    = 0,
        ragLatencyMs       = 0,
        reasoningLatencyMs = 0,
        isSimulated        = true,
        safetyFlags        = emptyList()
    )
}
