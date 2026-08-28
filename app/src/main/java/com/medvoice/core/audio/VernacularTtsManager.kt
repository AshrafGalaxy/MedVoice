package com.medvoice.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
    var selectedGender: VoiceGender = VoiceGender.MALE
    var speechRate: Float = 0.88f // Senior-friendly pacing
    var engineMode: VoiceEngineMode = VoiceEngineMode.HYBRID_SARVAM_AI
    var sarvamApiKey: String = "sk_jvbee2rt_gMpyMqxJ6Xl4IROW8BoWnXHN" // Default provided Sarvam AI Key
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
                Log.d("VernacularTtsManager", "On-device fallback TTS initialized successfully")
                onInitComplete(true)
            } else {
                Log.e("VernacularTtsManager", "TTS Initialization failed with status: $status")
                onInitComplete(false)
            }
        }
    }

    fun speak(text: String, languageCode: String = "en", onDone: () -> Unit = {}) {
        stop()

        // 1. Try Hybrid Online Sarvam AI if enabled and API key is present
        if (engineMode == VoiceEngineMode.HYBRID_SARVAM_AI && sarvamApiKey.isNotBlank()) {
            speakViaSarvamAi(text, languageCode, onDone, fallback = {
                speakOffline(text, languageCode, onDone)
            })
            return
        }

        // 2. Try Hybrid ElevenLabs if enabled
        if (engineMode == VoiceEngineMode.HYBRID_ELEVENLABS && elevenLabsApiKey.isNotBlank()) {
            speakViaElevenLabs(text, onDone, fallback = {
                speakOffline(text, languageCode, onDone)
            })
            return
        }

        // 3. 100% Offline Tuned On-Device Engine
        speakOffline(text, languageCode, onDone)
    }

    /**
     * Sarvam AI Bulbul v3 Indian Vernacular Integration (Hindi / Marathi / English)
     */
    private fun speakViaSarvamAi(text: String, languageCode: String, onDone: () -> Unit, fallback: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val targetLang = when (languageCode.lowercase()) {
                    "mr", "mr-in" -> "mr-IN"
                    "hi", "hi-in" -> "hi-IN"
                    else -> "en-IN"
                }
                val speaker = if (selectedGender == VoiceGender.FEMALE) "meera" else "shubh"

                val url = URL("https://api.sarvam.ai/text-to-speech")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("api-subscription-key", sarvamApiKey.trim())
                    doOutput = true
                    connectTimeout = 4000
                    readTimeout = 7000
                }

                // Construct clean JSON payload
                val payload = """
                    {
                        "inputs": ["${text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")}"],
                        "target_language_code": "$targetLang",
                        "speaker": "$speaker",
                        "model": "bulbul:v3"
                    }
                """.trimIndent()

                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val responseStr = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    
                    // Parse base64 audio and stream via MediaPlayer
                    val audioBase64 = Regex("\"audios\":\\s*\\[\\s*\"([^\"]+)\"").find(responseStr)?.groupValues?.get(1)
                    if (!audioBase64.isNullOrBlank()) {
                        val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                        val tempFile = File(context.cacheDir, "sarvam_tts_${System.currentTimeMillis()}.wav")
                        FileOutputStream(tempFile).use { it.write(audioBytes) }

                        withContext(Dispatchers.Main) {
                            playAudioFile(tempFile, onDone)
                        }
                        return@launch
                    }
                } else {
                    val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.w("VernacularTtsManager", "Sarvam AI HTTP $responseCode: $errorMsg")
                }

                // Graceful fallback to on-device TTS if non-200 or invalid response
                withContext(Dispatchers.Main) { fallback() }
            } catch (e: Exception) {
                Log.w("VernacularTtsManager", "Sarvam AI network error, falling back to on-device TTS", e)
                withContext(Dispatchers.Main) { fallback() }
            }
        }
    }

    private fun speakViaElevenLabs(text: String, onDone: () -> Unit, fallback: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val voiceId = if (selectedGender == VoiceGender.FEMALE) "21m00Tcm4TlvDq8ikWAM" else "pNInz6obpgDQGcFmaJgB"
                val url = URL("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("xi-api-key", elevenLabsApiKey)
                    doOutput = true
                    connectTimeout = 3000
                    readTimeout = 5000
                }

                val payload = """
                    {
                        "text": "${text.replace("\"", "\\\"")}",
                        "model_id": "eleven_multilingual_v2",
                        "voice_settings": {
                            "stability": 0.5,
                            "similarity_boost": 0.75
                        }
                    }
                """.trimIndent()

                conn.outputStream.use { it.write(payload.toByteArray()) }

                if (conn.responseCode == 200) {
                    val tempFile = File(context.cacheDir, "elevenlabs_${System.currentTimeMillis()}.mp3")
                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        playAudioFile(tempFile, onDone)
                    }
                    return@launch
                }
                withContext(Dispatchers.Main) { fallback() }
            } catch (e: Exception) {
                Log.w("VernacularTtsManager", "ElevenLabs failed, using fallback", e)
                withContext(Dispatchers.Main) { fallback() }
            }
        }
    }

    fun openTtsSystemSettings(ctx: Context) {
        try {
            val intent = android.content.Intent("com.android.settings.TTS_SETTINGS").apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            } catch (e: Exception) {
                Log.e("VernacularTtsManager", "Could not open system settings", e)
            }
        }
    }

    private fun speakOffline(text: String, languageCode: String, onDone: () -> Unit) {
        if (!isInitialized || tts == null) {
            onDone()
            return
        }

        val targetLocale = when (languageCode.lowercase()) {
            "hi", "hi-in" -> Locale.Builder().setLanguage("hi").setRegion("IN").build()
            "mr", "mr-in" -> Locale.Builder().setLanguage("mr").setRegion("IN").build()
            else -> Locale.Builder().setLanguage("en").setRegion("IN").build()
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
