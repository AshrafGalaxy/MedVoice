package com.medvoice.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class VernacularTtsManager(
    context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
                tts?.setSpeechRate(0.9f) // Slightly slower rate for elderly comprehension
                isInitialized = true
                onInitComplete(true)
            } else {
                Log.e("VernacularTtsManager", "TTS Initialization failed with status: $status")
                onInitComplete(false)
            }
        }
    }

    fun speak(text: String, languageCode: String = "mr-IN", onDone: () -> Unit = {}) {
        if (!isInitialized || tts == null) return

        val locale = when (languageCode) {
            "mr-IN", "mr" -> Locale("mr", "IN")
            "hi-IN", "hi" -> Locale("hi", "IN")
            else -> Locale("en", "IN")
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to Hindi if Marathi voice pack is missing on device
            tts?.setLanguage(Locale("hi", "IN"))
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onDone() }
            override fun onError(utteranceId: String?) {}
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MEDVOICE_UTTERANCE_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
