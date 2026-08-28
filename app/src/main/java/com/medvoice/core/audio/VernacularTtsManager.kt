package com.medvoice.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

enum class VoiceGender {
    FEMALE,
    MALE
}

class VernacularTtsManager(
    private val context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    // Voice configuration: On-Device High-Definition Google Neural Engine
    var selectedGender: VoiceGender = VoiceGender.MALE
    var speechRate: Float = 0.95f // Fast, crisp, responsive cadence
    var speechPitch: Float = 1.0f // Standard natural pitch

    companion object {
        private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
        private const val TAG = "VernacularTtsManager"
    }

    init {
        initializeGoogleNeuralTts()
    }

    /**
     * Explicit Engine Binding: Bind directly to Google Speech Synthesis (com.google.android.tts)
     * with graceful fallback to system default if Google engine is missing.
     */
    private fun initializeGoogleNeuralTts() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
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
     * Fast, On-Device Google Neural Spoken Voice Entrypoint
     */
    fun speak(text: String, languageCode: String = "en", onDone: () -> Unit = {}) {
        stop()

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

        // Voice Selection: Select best offline Google Neural voice matching gender and Indian locale
        selectOptimalGoogleNeuralVoice(targetLocale, selectedGender)

        // Set Cadence and Pitch
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

            val localMatchingVoices = voices.filter { voice ->
                voice.locale.language.equals(locale.language, ignoreCase = true) && !voice.isNetworkConnectionRequired
            }

            if (localMatchingVoices.isEmpty()) return

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
                Log.d(TAG, "Selected Google Neural Voice: ${bestVoice.name} (Quality=${bestVoice.quality}) for $gender in $locale")
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
                .replace(Regex("""\b1\s+गोली\b"""), "एक गोली")
                .replace(Regex("""\b2\s+गोली\b"""), "दो गोली")
                .replace(Regex("""\b1/2\s+गोली\b"""), "आधी गोली")
                .replace(Regex("""\b500\s+मिलीग्राम\b"""), "पाँच सौ मिलीग्राम")
                .replace(Regex("""\b650\s+मिलीग्राम\b"""), "छह सौ पचास मिलीग्राम")
                .replace(Regex("""\b1000\s+मिलीग्राम\b"""), "एक हज़ार मिलीग्राम")
        } else {
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

    fun stop() {
        tts?.stop()
        abandonAudioFocus()
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
