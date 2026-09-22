package com.sukshma.samanvaya.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

private const val TAG = "AndroidTTSEngine"

/**
 * AndroidTTSEngine — uses Android's built-in offline TTS.
 *
 * Why this first: It's always available, always offline, supports Hindi.
 * No model download needed. Reliable for demo.
 *
 * Language priority: Hindi > Telugu > English.
 * If the device TTS engine doesn't have Hindi voices installed,
 * falls back to English with a visible warning.
 */
class AndroidTTSEngine(private val context: Context) : TextToSpeechEngine {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    @Volatile
    override var isSpeaking: Boolean = false
        private set

    private val hindiLocale  = Locale("hi", "IN")
    private val teluguLocale = Locale("te", "IN")
    private val englishLocale = Locale.ENGLISH

    fun initialize(onReady: (Boolean) -> Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                Log.d(TAG, "TTS initialized successfully")

                // Check Hindi availability
                val hindiResult = tts?.isLanguageAvailable(hindiLocale)
                Log.d(TAG, "Hindi TTS available: $hindiResult")

                onReady(true)
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
                onReady(false)
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?)  { isSpeaking = true }
            override fun onDone(utteranceId: String?)   { isSpeaking = false }
            override fun onError(utteranceId: String?)  { isSpeaking = false }
        })
    }

    override fun speak(text: String, language: SupportedLanguage) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not ready — cannot speak")
            return
        }

        val locale = when (language) {
            SupportedLanguage.HINDI   -> hindiLocale
            SupportedLanguage.TELUGU  -> teluguLocale
            SupportedLanguage.ENGLISH -> englishLocale
        }

        val setResult = tts?.setLanguage(locale)
        if (setResult == TextToSpeech.LANG_MISSING_DATA ||
            setResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language $language not available, falling back to English")
            tts?.setLanguage(englishLocale)
        }

        // Trim to avoid reading giant paragraphs
        val trimmedText = if (text.length > 300) text.take(300) + "." else text

        tts?.speak(
            trimmedText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            UUID.randomUUID().toString()
        )
    }

    override fun stop() {
        tts?.stop()
        isSpeaking = false
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
