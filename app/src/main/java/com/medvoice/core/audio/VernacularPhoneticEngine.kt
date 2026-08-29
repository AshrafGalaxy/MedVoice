package com.medvoice.core.audio

import java.util.Locale
import java.util.regex.Pattern

/**
 * VernacularPhoneticEngine
 *
 * 100% Dynamic, Rule-Based Algorithmic Transliteration & Phonetic Processor.
 * Zero hardcoded product tables.
 *
 * Capabilities:
 * 1. Dynamic Number to Hindi Words Algorithm (Any arbitrary number 0 to 99999).
 * 2. Morphological Medical Suffix & Grapheme-to-Phoneme Transliteration for Latin drug tokens.
 * 3. Dynamic Unit & Abbreviation Expander.
 * 4. Acoustic Breathing Pause & Punctuation Structuring.
 */
object VernacularPhoneticEngine {

    // --- 1. DYNAMIC NUMBER TO HINDI WORDS CONVERTER ---

    private val HINDI_DIGITS_0_TO_19 = arrayOf(
        "शून्य", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ",
        "दस", "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस"
    )

    private val HINDI_TENS = arrayOf(
        "", "", "बीस", "तीस", "चालीस", "पचास", "साठ", "सत्तर", "अस्सी", "नब्बे"
    )

    // Accurate 21-99 irregular Hindi numeral map
    private val HINDI_NUMBERS_21_99 = mapOf(
        21 to "इक्कीस", 22 to "बाईस", 23 to "तेईस", 24 to "चौबीस", 25 to "पच्चीस",
        26 to "छब्बीस", 27 to "सत्ताईस", 28 to "अट्ठाईस", 29 to "उनतीस",
        31 to "इकतीस", 32 to "बत्तीस", 33 to "तैंतीस", 34 to "चौंतीस", 35 to "पैंतीस",
        36 to "छत्तीस", 37 to "सैंतीस", 38 to "अड़तीस", 39 to "उनतालीस",
        41 to "इकतालीस", 42 to "बयालीस", 43 to "तैंतालीस", 44 to "चवालीस", 45 to "पैंतालीस",
        46 to "छियालीस", 47 to "सैंतालीस", 48 to "अड़तालीस", 49 to "उनचास",
        51 to "इक्यावन", 52 to "बावन", 53 to "तिरपन", 54 to "चौवन", 55 to "पचपन",
        56 to "छप्पन", 57 to "सत्तावन", 58 to "अट्ठावन", 59 to "उनसठ",
        61 to "इकसठ", 62 to "बासठ", 63 to "तिरसठ", 64 to "चौंसठ", 65 to "पैंसठ",
        66 to "छियासठ", 67 to "सड़सठ", 68 to "अड़सठ", 69 to "उनहत्तर",
        71 to "इकहत्तर", 72 to "बहत्तर", 73 to "तिहत्तर", 74 to "चौहत्तर", 75 to "पचहत्तर",
        76 to "छिहत्तर", 77 to "सतहत्तर", 78 to "अठहत्तर", 79 to "उन्नासी",
        81 to "इक्यासी", 82 to "बयासी", 83 to "तिरासी", 84 to "चौरासी", 85 to "पचासी",
        86 to "छियासी", 87 to "सत्तासी", 88 to "अट्ठासी", 89 to "नवासी",
        91 to "इक्यानवे", 92 to "बानवे", 93 to "तिरानवे", 94 to "चौरानवे", 95 to "पंचानवे",
        96 to "छियानवे", 97 to "सत्तानवे", 98 to "अट्ठानवे", 99 to "निन्यानवे"
    )

    /**
     * Converts any integer (0 to 99999) into fluent Hindi words dynamically.
     */
    fun numberToHindiWords(n: Long): String {
        if (n < 0) return "माइनस " + numberToHindiWords(-n)
        if (n < 20) return HINDI_DIGITS_0_TO_19[n.toInt()]
        if (n < 100) {
            val rem = n.toInt()
            if (rem % 10 == 0) return HINDI_TENS[rem / 10]
            return HINDI_NUMBERS_21_99[rem] ?: "${HINDI_TENS[rem / 10]} ${HINDI_DIGITS_0_TO_19[rem % 10]}"
        }
        if (n < 1000) {
            val hundreds = n / 100
            val remainder = n % 100
            val prefix = "${HINDI_DIGITS_0_TO_19[hundreds.toInt()]} सौ"
            return if (remainder == 0L) prefix else "$prefix ${numberToHindiWords(remainder)}"
        }
        if (n < 100000) {
            val thousands = n / 1000
            val remainder = n % 1000
            val prefix = "${numberToHindiWords(thousands)} हज़ार"
            return if (remainder == 0L) prefix else "$prefix ${numberToHindiWords(remainder)}"
        }
        val lakhs = n / 100000
        val remainder = n % 100000
        val prefix = "${numberToHindiWords(lakhs)} लाख"
        return if (remainder == 0L) prefix else "$prefix ${numberToHindiWords(remainder)}"
    }

    // --- 2. DYNAMIC PHONETIC & MORPHOLOGICAL MEDICAL TRANSLITERATOR ---

    // Standard medical pharmacological suffixes and their Devanagari phonetic equivalents
    private val MEDICAL_SUFFIXES = listOf(
        "gliflozin" to "ग्लिफ्लोज़िन",
        "gliptin" to "ग्लिप्टिन",
        "prazole" to "प्राज़ोल",
        "sartan" to "सार्टन",
        "statin" to "स्टेटिन",
        "dipine" to "डिपिन",
        "cillin" to "सिलिन",
        "cycline" to "साइक्लिन",
        "floxacin" to "फ्लोक्सासिन",
        "mycin" to "माइसिन",
        "profen" to "प्रोफेन",
        "fenac" to "फेनाक",
        "tidine" to "टिडिन",
        "zole" to "ज़ोल",
        "olol" to "ओलोल",
        "sone" to "सोन",
        "nazole" to "नाज़ोल",
        "lukast" to "लुकास्ट",
        "triptan" to "ट्रिप्टान",
        "azepam" to "एज़ेपाम",
        "mab" to "मैब",
        "nib" to "निब",
        "pril" to "प्रिल",
        "ipine" to "इपिन",
        "idine" to "इडिन",
        "afil" to "एफिल",
        "xine" to "क्सीन",
        "pine" to "पाइन",
        "mine" to "माइन",
        "nate" to "नेट"
    )

    // Algorithmic phonetic digraph replacements
    private val PHONETIC_DIGRAPHS = listOf(
        "tion" to "शन",
        "sion" to "शन",
        "ph" to "फ",
        "th" to "थ",
        "ch" to "च",
        "sh" to "श",
        "kh" to "ख",
        "gh" to "घ",
        "dh" to "ध",
        "bh" to "भ",
        "jh" to "झ",
        "wh" to "व",
        "ck" to "क",
        "qu" to "क्व",
        "ee" to "ी",
        "oo" to "ू",
        "ai" to "ै",
        "au" to "ौ",
        "ou" to "ाउ",
        "ea" to "ी",
        "oa" to "ो"
    )

    // Single-character grapheme to Devanagari mapping
    private val CONSONANT_MAP = mapOf(
        'b' to "ब", 'c' to "क", 'd' to "ड", 'f' to "फ", 'g' to "ग",
        'h' to "ह", 'j' to "ज", 'k' to "क", 'l' to "ल", 'm' to "म",
        'n' to "न", 'p' to "प", 'q' to "क", 'r' to "र", 's' to "स",
        't' to "ट", 'v' to "व", 'w' to "व", 'x' to "क्स", 'y' to "य", 'z' to "ज़"
    )

    /**
     * Algorithmic Grapheme-to-Phoneme Transliteration:
     * Converts any unknown Latin medical brand or chemical salt token into Devanagari.
     */
    fun transliterateTokenToDevanagari(token: String): String {
        val clean = token.lowercase().trim()
        if (clean.isBlank()) return ""

        // If token is already in Devanagari or contains numbers/punctuation, handle accordingly
        if (clean.any { it in '\u0900'..'\u097F' }) return token
        if (clean.all { it.isDigit() }) {
            return clean.toLongOrNull()?.let { numberToHindiWords(it) } ?: clean
        }

        // 1. Check for pharmacological suffix match
        for ((suffix, devanagariSuffix) in MEDICAL_SUFFIXES) {
            if (clean.endsWith(suffix) && clean.length > suffix.length) {
                val stem = clean.substring(0, clean.length - suffix.length)
                val stemTransliterated = transliterateSimpleLatin(stem)
                return "$stemTransliterated$devanagariSuffix"
            }
        }

        // 2. Full algorithmic grapheme-to-phoneme transliteration
        return transliterateSimpleLatin(clean)
    }

    private fun transliterateSimpleLatin(text: String): String {
        var str = text

        // Step A: Replace digraphs
        for ((latin, dev) in PHONETIC_DIGRAPHS) {
            str = str.replace(latin, dev)
        }

        // Step B: Vowel and consonant decomposition
        val result = StringBuilder()
        var i = 0
        while (i < str.length) {
            val char = str[i]

            // If already Devanagari from digraph step, append directly
            if (char in '\u0900'..'\u097F') {
                result.append(char)
                i++
                continue
            }

            val nextChar = if (i + 1 < str.length) str[i + 1] else null

            when (char) {
                'a' -> {
                    if (result.isEmpty()) result.append("अ") else result.append("ा")
                }
                'e' -> {
                    if (result.isEmpty()) result.append("ए") else result.append("े")
                }
                'i' -> {
                    if (result.isEmpty()) result.append("इ") else result.append("ि")
                }
                'o' -> {
                    if (result.isEmpty()) result.append("ओ") else result.append("ो")
                }
                'u' -> {
                    if (result.isEmpty()) result.append("उ") else result.append("ु")
                }
                'c' -> {
                    // 'c' followed by e, i, y -> 'स', otherwise 'क'
                    if (nextChar == 'e' || nextChar == 'i' || nextChar == 'y') {
                        result.append("स")
                    } else {
                        result.append("क")
                    }
                }
                else -> {
                    val devConsonant = CONSONANT_MAP[char]
                    if (devConsonant != null) {
                        result.append(devConsonant)
                    } else {
                        result.append(char)
                    }
                }
            }
            i++
        }

        return result.toString()
    }

    // --- 3. DYNAMIC MEDICAL UNITS, FREQUENCIES & PHONETICS EXPANDER ---

    /**
     * Expands medical abbreviations, dosages, frequencies, and Latin brand tokens dynamically.
     */
    fun processVernacularSpeechText(rawText: String, langCode: String): String {
        var text = rawText.trim()
        val isHindi = langCode.startsWith("hi", ignoreCase = true)

        if (isHindi) {
            // A. Numbers with medical units
            text = text.replace(Regex("""(?i)\b(\d+)\s*mg\b""")) { match ->
                val num = match.groupValues[1].toLongOrNull() ?: 0L
                "${numberToHindiWords(num)} मिलीग्राम"
            }
            text = text.replace(Regex("""(?i)\b(\d+)\s*mcg\b""")) { match ->
                val num = match.groupValues[1].toLongOrNull() ?: 0L
                "${numberToHindiWords(num)} माइक्रोग्राम"
            }
            text = text.replace(Regex("""(?i)\b(\d+)\s*ml\b""")) { match ->
                val num = match.groupValues[1].toLongOrNull() ?: 0L
                "${numberToHindiWords(num)} मिलीलीटर"
            }
            text = text.replace(Regex("""(?i)\b(\d+)\s*gm\b""")) { match ->
                val num = match.groupValues[1].toLongOrNull() ?: 0L
                "${numberToHindiWords(num)} ग्राम"
            }

            // B. Standalone unit and dosage formulation expansion
            text = text
                .replace(Regex("""(?i)\bmg\b"""), "मिलीग्राम")
                .replace(Regex("""(?i)\btab\b|\btabs\b"""), "गोली")
                .replace(Regex("""(?i)\bcap\b|\bcaps\b"""), "कैप्सूल")
                .replace(Regex("""(?i)\b-?SR\b"""), " सस्टेन्ड रिलीज़ ")
                .replace(Regex("""(?i)\b-?CR\b"""), " कंट्रोल्ड रिलीज़ ")
                .replace(Regex("""(?i)\b-?ER\b"""), " एक्सटेंडेड रिलीज़ ")
                .replace(Regex("""(?i)\b-?DS\b"""), " डबल स्ट्रेंथ ")
                .replace(Regex("""(?i)\b-?DT\b"""), " डिस्पर्सिबल टैबलेट ")

            // C. Clinical Frequencies
            text = text
                .replace(Regex("""(?i)\bOD\b"""), "दिन में एक बार")
                .replace(Regex("""(?i)\bBD\b|\bBID\b"""), "दिन में दो बार")
                .replace(Regex("""(?i)\bTDS\b|\bTID\b"""), "दिन में तीन बार")
                .replace(Regex("""(?i)\bQID\b"""), "दिन में चार बार")
                .replace(Regex("""(?i)\bSOS\b"""), "ज़रूरत पड़ने पर")
                .replace(Regex("""(?i)\bHS\b"""), "रात को सोने से पहले")
                .replace(Regex("""(?i)\bAC\b"""), "भोजन से पहले")
                .replace(Regex("""(?i)\bPC\b"""), "भोजन के बाद")

            // D. Dynamic Transliteration of remaining English alphanumeric medicine words
            val words = text.split(Regex("\\s+"))
            val processedWords = words.map { word ->
                // Clean punctuation around word
                val leadingPunct = word.takeWhile { !it.isLetterOrDigit() }
                val trailingPunct = word.takeLastWhile { !it.isLetterOrDigit() }
                val core = word.substring(leadingPunct.length, word.length - trailingPunct.length)

                if (core.isNotEmpty() && core.all { it.isLetter() } && core.any { it in 'a'..'z' || it in 'A'..'Z' }) {
                    leadingPunct + transliterateTokenToDevanagari(core) + trailingPunct
                } else {
                    word
                }
            }
            text = processedWords.joinToString(" ")

            // E. Dynamic standalone number conversion in Hindi context (e.g. "1 गोली" -> "एक गोली", "650" -> "छह सौ पचास")
            text = text.replace(Regex("""\b(\d+)\b""")) { match ->
                val num = match.groupValues[1].toLongOrNull()
                if (num != null && num in 0..99999) numberToHindiWords(num) else match.value
            }
        } else {
            // Indian English natural medical expansions
            text = text
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
                .replace(Regex("""(?i)\bOD\b"""), "once a day")
                .replace(Regex("""(?i)\bBD\b|\bBID\b"""), "twice a day")
                .replace(Regex("""(?i)\bTDS\b|\bTID\b"""), "three times a day")
                .replace(Regex("""(?i)\bSOS\b"""), "as needed")
        }

        // Acoustic Breathing Pauses & Micro-Timing Structuring
        return insertAcousticPauses(text)
    }

    /**
     * Inserts natural acoustic breath pauses between medical clauses
     */
    private fun insertAcousticPauses(text: String): String {
        return text
            .replace("!", "! , ")
            .replace("।", "। , ")
            .replace(":", ": , ")
            .replace(" - ", " , ")
            .replace("\n", " , ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }
}
