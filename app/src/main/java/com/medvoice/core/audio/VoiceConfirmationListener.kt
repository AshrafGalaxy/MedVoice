package com.medvoice.core.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class VoiceConfirmationListener(
    private val context: Context,
    private val onConfirmed: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    var isListening = false
        private set

    fun startListening(locale: String = "hi", onListeningStateChanged: (Boolean) -> Unit = {}) {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w("VoiceConfirmation", "Speech recognition not available on device")
                return@post
            }

            try {
                stopListeningInternal()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            onListeningStateChanged(true)
                            Log.d("VoiceConfirmation", "Ready for speech confirmation")
                        }

                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            isListening = false
                            onListeningStateChanged(false)
                        }

                        override fun onError(error: Int) {
                            isListening = false
                            onListeningStateChanged(false)
                            Log.d("VoiceConfirmation", "Speech error code: $error")
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            onListeningStateChanged(false)
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val recognizedText = matches.joinToString(" ").lowercase(Locale.ROOT)
                                Log.d("VoiceConfirmation", "Recognized voice confirmation: $recognizedText")

                                val affirmativeKeywords = listOf(
                                    "हाँ", "ले ली", "खा ली", "पी ली", "हो गई", "ली", "ले लिया", "खा लिया",
                                    "yes", "taken", "done", "took it", "i took it", "le li", "haan", "haa"
                                )
                                if (affirmativeKeywords.any { recognizedText.contains(it) }) {
                                    onConfirmed()
                                }
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val targetLanguage = if (locale.startsWith("hi", ignoreCase = true)) "hi-IN" else "en-IN"
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLanguage)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("VoiceConfirmation", "Failed to start speech recognizer", e)
                isListening = false
                onListeningStateChanged(false)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
        } catch (e: Exception) {
            Log.e("VoiceConfirmation", "Error stopping speech recognizer", e)
        }
    }
}
