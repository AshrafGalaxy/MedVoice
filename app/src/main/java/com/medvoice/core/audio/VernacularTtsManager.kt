package com.medvoice.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Base64
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
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    // Senior-optimized cadence and natural acoustic properties
    var selectedGender: VoiceGender = VoiceGender.MALE
    var speechRate: Float = 0.88f // 0.88x pacing for elderly comprehension and Devanagari phrasing
    var speechPitch: Float = 1.0f // Standard natural pitch
    var engineMode: VoiceEngineMode = VoiceEngineMode.HYBRID_SARVAM_AI
    var sarvamApiKey: String = "sk_jvbee2rt_gMpyMqxJ6Xl4IROW8BoWnXHN"
    var elevenLabsApiKey: String = ""

    companion object {
        private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
        private const val TAG = "VernacularTtsManager"
    }

    init {
        initializeGoogleNeuralTts()
    }

    /**
     * Explicit Engine Binding: Bind to Google Neural Speech Synthesis (com.google.android.tts)
     * with graceful fallback to system default if Google engine is missing.
     */
    private fun initializeGoogleNeuralTts() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Step 1: Attempt explicit binding to Google Speech Synthesis
                val initListener = TextToSpeech.OnInitListener { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        configureTtsInstance()
                        isInitialized = true
                        Log.d(TAG, "Google High-Definition Neural TTS Engine initialized successfully")
                        onInitComplete(true)
                    } else {
                        Log.w(TAG, "Google TTS explicit binding failed (status=$status). Falling back to system default.")
                        fallbackToDefaultTts()
                    }
                }

                tts = TextToSpeech(context.applicationContext, initListener, GOOGLE_TTS_PACKAGE)
            } catch (e: Exception) {
                Log.e(TAG, "Exception binding to Google TTS, falling back to default", e)
                fallbackToDefaultTts()
            }
        }
    }

    private fun fallbackToDefaultTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureTtsInstance()
                isInitialized = true
                Log.d(TAG, "Default System TTS initialized successfully")
                onInitComplete(true)
            } else {
                Log.e(TAG, "All TTS initialization attempts failed with status: $status")
                onInitComplete(false)
            }
        }
    }

    private fun configureTtsInstance() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        tts?.setAudioAttributes(audioAttributes)
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(speechPitch)
    }

    /**
     * Main Spoken Voice Entrypoint
     */
    fun speak(text: String, languageCode: String = "en", onDone: () -> Unit = {}) {
        stop()

        // 1. Try Hybrid Online Sarvam AI (Studio-Grade Indian Vernacular)
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

        // 3. 100% Offline Google Neural On-Device Engine
        speakOffline(text, languageCode, onDone)
    }

    /**
     * Sarvam AI Bulbul v3 Indian Vernacular Integration (Hindi / Indian English)
     */
    private fun speakViaSarvamAi(text: String, languageCode: String, onDone: () -> Unit, fallback: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val targetLang = if (languageCode.lowercase().startsWith("hi")) "hi-IN" else "en-IN"
                val speaker = if (selectedGender == VoiceGender.FEMALE) "meera" else "shubh"

                val url = URL("https://api.sarvam.ai/text-to-speech")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("api-subscription-key", sarvamApiKey.trim())
                    doOutput = true
                    connectTimeout = 3500
                    readTimeout = 6500
                }

                val cleanText = preprocessMedicalText(text, targetLang)
                val payload = """
                    {
                        "inputs": ["${cleanText.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")}"],
                        "target_language_code": "$targetLang",
                        "speaker": "$speaker",
                        "model": "bulbul:v3"
                    }
                """.trimIndent()

                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val responseStr = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
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
                    Log.w(TAG, "Sarvam AI HTTP $responseCode: $errorMsg")
                }

                // Fallback to Google Neural On-Device
                withContext(Dispatchers.Main) { fallback() }
            } catch (e: Exception) {
                Log.w(TAG, "Sarvam AI network error, falling back to Google Neural On-Device", e)
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

                val cleanText = preprocessMedicalText(text, "en-IN")
                val payload = """
                    {
                        "text": "${cleanText.replace("\"", "\\\"")}",
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
                Log.w(TAG, "ElevenLabs failed, using fallback", e)
                withContext(Dispatchers.Main) { fallback() }
            }
        }
    }

    /**
     * 100% Offline Google High-Definition Neural Speech Synthesis
     */
    private fun speakOffline(text: String, languageCode: String, onDone: () -> Unit) {
        if (!isInitialized || tts == null) {
            onDone()
            return
        }

        val isHindi = languageCode.lowercase().startsWith("hi")
        val targetLocale = if (isHindi) {
            Locale.Builder().setLanguage("hi").setRegion("IN").build()
        } else {
            Locale.Builder().setLanguage("en").setRegion("IN").build()
        }

        val langResult = tts?.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.ENGLISH)
        }

        // Voice Selection & Highest-Quality Local Filtering
        selectOptimalGoogleNeuralVoice(targetLocale, selectedGender)

        // Set Senior Cadence (0.88x) and Natural Pitch (1.0x)
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(speechPitch)

        // Text Pre-Processing for Natural Breathing Pauses & Medical Expansion
        val preprocessedText = preprocessMedicalText(text, if (isHindi) "hi-IN" else "en-IN")

        requestAudioFocus()

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                abandonAudioFocus()
                onDone()
            }
            override fun onError(utteranceId: String?) {
                abandonAudioFocus()
                onDone()
            }
        })

        tts?.speak(preprocessedText, TextToSpeech.QUEUE_FLUSH, null, "MEDVOICE_GOOGLE_NEURAL_${System.currentTimeMillis()}")
    }

    /**
     * Highest-Quality Local Neural Voice Selector
     * - Inspects device installed voice catalog for hi-IN and en-IN
     * - Ensures voice does not require network connection (!voice.isNetworkConnectionRequired)
     * - Sorts by quality score (VERY_HIGH > HIGH > NORMAL) and gender variant
     */
    private fun selectOptimalGoogleNeuralVoice(locale: Locale, gender: VoiceGender) {
        try {
            val voices: Set<Voice>? = tts?.voices
            if (voices.isNullOrEmpty()) return

            // Filter for matching language and 100% offline local availability
            val localMatchingVoices = voices.filter { voice ->
                voice.locale.language.equals(locale.language, ignoreCase = true) && !voice.isNetworkConnectionRequired
            }

            if (localMatchingVoices.isEmpty()) return

            // Score and sort voices by quality attribute and neural gender names
            val bestVoice = localMatchingVoices.sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenByDescending { voice ->
                        val name = voice.name.lowercase()
                        when (gender) {
                            VoiceGender.FEMALE -> when {
                                name.contains("female") -> 3
                                name.contains("hie") || name.contains("ene") || name.contains("network-f") -> 2
                                name.contains("wavenet") || name.contains("neural") -> 1
                                else -> 0
                            }
                            VoiceGender.MALE -> when {
                                name.contains("male") -> 3
                                name.contains("hid") || name.contains("enc") || name.contains("network-m") -> 2
                                name.contains("wavenet") || name.contains("neural") -> 1
                                else -> 0
                            }
                        }
                    }
            ).firstOrNull()

            if (bestVoice != null) {
                tts?.voice = bestVoice
                Log.d(TAG, "Selected Google Neural Voice: ${bestVoice.name} (Quality=${bestVoice.quality}, Latency=${bestVoice.latency}) for $gender in $locale")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Voice discovery exception, using engine default", e)
        }
    }

    /**
     * Text Pre-Processing for Natural Delivery:
     * - Expands medical units and abbreviations in memory (mg -> milligram / मिलीग्राम)
     * - Inserts explicit breath pauses (commas and periods) between medicine name, dosage, and warnings
     */
    fun preprocessMedicalText(rawText: String, langCode: String): String {
        var processed = rawText.trim()
        val isHindi = langCode.startsWith("hi", ignoreCase = true)

        if (isHindi) {
            // Expand medical abbreviations to natural Devanagari words
            processed = processed
                .replace(Regex("""(?i)\b(\d+)\s*mg\b"""), "$1 मिलीग्राम")
                .replace(Regex("""(?i)\b(\d+)\s*mcg\b"""), "$1 माइक्रोग्राम")
                .replace(Regex("""(?i)\b(\d+)\s*ml\b"""), "$1 मिलीलीटर")
                .replace(Regex("""(?i)\b(\d+)\s*gm\b"""), "$1 ग्राम")
                .replace(Regex("""(?i)\bmg\b"""), "मिलीग्राम")
                .replace(Regex("""(?i)\btab\b|\btabs\b"""), "गोली")
                .replace(Regex("""(?i)\bcap\b|\bcaps\b"""), "कैप्सूल")
                .replace(Regex("""(?i)\b-?SR\b"""), " सस्टेन्ड रिलीज़ ")
                .replace(Regex("""(?i)\b-?CR\b"""), " कंट्रोल्ड रिलीज़ ")
                .replace(Regex("""(?i)\b-?ER\b"""), " एक्सटेंडेड रिलीज़ ")
                // Number conversions for key dosage numbers
                .replace(Regex("""\b1\s+गोली\b"""), "एक गोली")
                .replace(Regex("""\b2\s+गोली\b"""), "दो गोली")
                .replace(Regex("""\b1/2\s+गोली\b"""), "आधी गोली")
                .replace(Regex("""\b500\s+मिलीग्राम\b"""), "पाँच सौ मिलीग्राम")
                .replace(Regex("""\b650\s+मिलीग्राम\b"""), "छह सौ पचास मिलीग्राम")
                .replace(Regex("""\b1000\s+मिलीग्राम\b"""), "एक हज़ार मिलीग्राम")
        } else {
            // Expand medical abbreviations to full English terms
            processed = processed
                .replace(Regex("""(?i)\b(\d+)\s*mg\b"""), "$1 milligrams")
                .replace(Regex("""(?i)\b(\d+)\s*mcg\b"""), "$1 micrograms")
                .replace(Regex("""(?i)\b(\d+)\s*ml\b"""), "$1 milliliters")
                .replace(Regex("""(?i)\b(\d+)\s*gm\b"""), "$1 grams")
                .replace(Regex("""(?i)\bmg\b"""), "milligrams")
                .replace(Regex("""(?i)\btab\b|\btabs\b"""), "tablet")
                .replace(Regex("""(?i)\bcap\b|\bcaps\b"""), "capsule")
                .replace(Regex("""(?i)\b-?SR\b"""), " sustained release ")
                .replace(Regex("""(?i)\b-?CR\b"""), " controlled release ")
                .replace(Regex("""(?i)\b-?ER\b"""), " extended release ")
        }

        // Insert natural breathing pauses around punctuation and alert markers
        processed = processed
            .replace("!", "! , ")
            .replace("।", "। , ")
            .replace(":", ": , ")
            .replace("\n", " , ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

        return processed
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setOnAudioFocusChangeListener { /* transient playback */ }
                    .build()
                audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ACCESSIBILITY,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus request failed", e)
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus abandon failed", e)
        }
    }

    private fun playAudioFile(file: File, onDone: () -> Unit) {
        try {
            requestAudioFocus()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    file.delete()
                    abandonAudioFocus()
                    onDone()
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    mediaPlayer = null
                    file.delete()
                    abandonAudioFocus()
                    onDone()
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer playback failed", e)
            abandonAudioFocus()
            onDone()
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
                Log.e(TAG, "Could not open system settings", e)
            }
        }
    }

    fun stop() {
        tts?.stop()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        abandonAudioFocus()
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
