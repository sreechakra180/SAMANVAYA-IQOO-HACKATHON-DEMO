package com.sukshma.samanvaya.speech

// ─── Domain models ────────────────────────────────────────────────────────────

enum class SupportedLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी"),
    TELUGU("te", "తెలుగు")
}

data class AudioData(
    val pcmData: ShortArray,
    val sampleRate: Int = 16000,
    val channelCount: Int = 1
)

data class SpeechResult(
    val success: Boolean,
    val transcript: String,
    val confidence: Float,
    val language: SupportedLanguage,
    val latencyMs: Long,
    val model: String,
    val error: String? = null
)

// ─── STT Interface ─────────────────────────────────────────────────────────────

/**
 * SpeechRecognizerEngine — abstraction over local STT models.
 *
 * Implementations:
 *  - WhisperEngine      : Whisper-tiny (primary, 39MB)
 *  - AndroidSTTEngine   : Android SpeechRecognizer (fallback, online-capable)
 *  - StubSTTEngine      : testing stub
 */
interface SpeechRecognizerEngine {
    val modelName: String
    val isReady: Boolean

    suspend fun transcribe(audio: AudioData): SpeechResult
    suspend fun initialize(): Boolean
    fun release()
}

// ─── TTS Interface ──────────────────────────────────────────────────────────

/**
 * TextToSpeechEngine — abstraction over local TTS.
 * Primary: Android offline TTS (always available, no model needed).
 */
interface TextToSpeechEngine {
    fun speak(text: String, language: SupportedLanguage = SupportedLanguage.ENGLISH)
    fun stop()
    val isSpeaking: Boolean
}
