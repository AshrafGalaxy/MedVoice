package com.medvoice.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

enum class VoiceGender {
    FEMALE,
    MALE
}

enum class VoiceEngineMode {
    OFFLINE_DEVICE,
    HYBRID_SARVAM_AI,
    HYBRID_ELEVENLABS
}

class VernacularTtsManager(
    private val context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var mediaPlayer: MediaPlayer? = null

    // Voice preferences
    var selectedGender: VoiceGender = VoiceGender.FEMALE
    var speechRate: Float = 0.88f // Senior-friendly clarity
    var engineMode: VoiceEngineMode = VoiceEngineMode.OFFLINE_DEVICE
    var sarvamApiKey: String = "" // Optional user key for Sarvam AI
    var elevenLabsApiKey: String = "" // Optional user key for ElevenLabs

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
                tts?.setSpeechRate(speechRate)
                isInitialized = true
                Log.d("VernacularTtsManager", "On-device TTS initialized successfully")
                onInitComplete(true)
            } else {
                Log.e("VernacularTtsManager", "TTS Initialization failed with status: $status")
                onInitComplete(false)
            }
        }
    }

    fun speak(text: String, languageCode: String = "en", onDone: () -> Unit = {}) {
        stop()

        // 1. Try Hybrid Online AI if enabled and API key is present
        if (engineMode == VoiceEngineMode.HYBRID_SARVAM_AI && sarvamApiKey.isNotBlank()) {
            speakViaSarvamAi(text, languageCode, onDone, fallback = {
                speakOffline(text, languageCode, onDone)
            })
            return
        }

        // 2. Default to 100% Offline Tuned On-Device Engine
        speakOffline(text, languageCode, onDone)
    }

    private fun speakOffline(text: String, languageCode: String, onDone: () -> Unit) {
        if (!isInitialized || tts == null) {
            onDone()
            return
        }

        val targetLocale = when (languageCode.lowercase()) {
            "hi", "hi-in" -> Locale("hi", "IN")
            "mr", "mr-in" -> Locale("mr", "IN")
            else -> Locale("en", "IN")
        }

        val langResult = tts?.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.ENGLISH)
        }

        // Select best available on-device voice matching gender and Indian locale
        selectOptimalOnDeviceVoice(targetLocale, selectedGender)

        // Pitch tuning: Warm tone for female, deeper tone for male
        when (selectedGender) {
            VoiceGender.FEMALE -> tts?.setPitch(1.08f)
            VoiceGender.MALE -> tts?.setPitch(0.85f)
        }
        tts?.setSpeechRate(speechRate)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onDone() }
            override fun onError(utteranceId: String?) { onDone() }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MEDVOICE_OFFLINE_${System.currentTimeMillis()}")
    }

    private fun selectOptimalOnDeviceVoice(locale: Locale, gender: VoiceGender) {
        try {
            val voices: Set<Voice>? = tts?.voices
            if (voices.isNullOrEmpty()) return

            val matchingVoices = voices.filter { voice ->
                voice.locale.language.equals(locale.language, ignoreCase = true) && !voice.isNetworkConnectionRequired
            }

            if (matchingVoices.isEmpty()) return

            val preferredVoice = matchingVoices.find { voice ->
                val name = voice.name.lowercase()
                when (gender) {
                    VoiceGender.FEMALE -> name.contains("female") || name.contains("hie") || name.contains("ene") || name.contains("mrf")
                    VoiceGender.MALE -> name.contains("male") || name.contains("hid") || name.contains("enc") || name.contains("mre")
                }
            } ?: matchingVoices.first()

            tts?.voice = preferredVoice
            Log.d("VernacularTtsManager", "Selected on-device voice: ${preferredVoice.name} for $gender in $locale")
        } catch (e: Exception) {
            Log.w("VernacularTtsManager", "Voice selection fallback", e)
        }
    }

    /**
     * Optional Sarvam AI Bulbul integration with graceful fallback
     */
    private fun speakViaSarvamAi(text: String, languageCode: String, onDone: () -> Unit, fallback: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val targetLang = if (languageCode.startsWith("hi")) "hi-IN" else "en-IN"
                val speaker = if (selectedGender == VoiceGender.FEMALE) "meera" else "arvind"

                val url = URL("https://api.sarvam.ai/text-to-speech")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("api-subscription-key", sarvamApiKey)
                conn.doOutput = true
                conn.connectTimeout = 3000
                conn.readTimeout = 5000

                val payload = """
                    {
                        "inputs": ["${text.replace("\"", "\\\"")}"],
                        "target_language_code": "$targetLang",
                        "speaker": "$speaker",
                        "model": "bulbul:v1"
                    }
                """.trimIndent()

                conn.outputStream.use { it.write(payload.toByteArray()) }

                if (conn.responseCode == 200) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    // Parse base64 audio and stream via MediaPlayer
                    val audioBase64 = Regex("\"audios\":\\s*\\[\\s*\"([^\"]+)\"").find(responseStr)?.groupValues?.get(1)
                    if (!audioBase64.isNullOrBlank()) {
                        val audioBytes = android.util.Base64.decode(audioBase64, android.util.Base64.DEFAULT)
                        val tempFile = File(context.cacheDir, "sarvam_tts_${System.currentTimeMillis()}.wav")
                        FileOutputStream(tempFile).use { it.write(audioBytes) }

                        withContext(Dispatchers.Main) {
                            playAudioFile(tempFile, onDone)
                        }
                        return@launch
                    }
                }
                // Fallback if response wasn't 200
                withContext(Dispatchers.Main) { fallback() }
            } catch (e: Exception) {
                Log.w("VernacularTtsManager", "Sarvam AI failed or offline, using fallback", e)
                withContext(Dispatchers.Main) { fallback() }
            }
        }
    }

    private fun playAudioFile(file: File, onDone: () -> Unit) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    file.delete()
                    onDone()
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("VernacularTtsManager", "MediaPlayer playback failed", e)
            onDone()
        }
    }

    fun stop() {
        tts?.stop()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
