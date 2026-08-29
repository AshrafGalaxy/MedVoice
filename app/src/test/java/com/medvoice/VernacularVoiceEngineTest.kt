package com.medvoice

import com.medvoice.core.audio.VernacularPhoneticEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VernacularVoiceEngineTest {

    @Test
    fun testDynamicNumberToHindiWords() {
        assertEquals("शून्य", VernacularPhoneticEngine.numberToHindiWords(0))
        assertEquals("एक", VernacularPhoneticEngine.numberToHindiWords(1))
        assertEquals("पाँच", VernacularPhoneticEngine.numberToHindiWords(5))
        assertEquals("दस", VernacularPhoneticEngine.numberToHindiWords(10))
        assertEquals("पंद्रह", VernacularPhoneticEngine.numberToHindiWords(15))
        assertEquals("बीस", VernacularPhoneticEngine.numberToHindiWords(20))
        assertEquals("पच्चीस", VernacularPhoneticEngine.numberToHindiWords(25))
        assertEquals("चालीस", VernacularPhoneticEngine.numberToHindiWords(40))
        assertEquals("पचहत्तर", VernacularPhoneticEngine.numberToHindiWords(75))
        assertEquals("एक सौ", VernacularPhoneticEngine.numberToHindiWords(100))
        assertEquals("पाँच सौ", VernacularPhoneticEngine.numberToHindiWords(500))
        assertEquals("छह सौ पच्चीस", VernacularPhoneticEngine.numberToHindiWords(625))
        assertEquals("छह सौ पचास", VernacularPhoneticEngine.numberToHindiWords(650))
        assertEquals("एक हज़ार", VernacularPhoneticEngine.numberToHindiWords(1000))
    }

    @Test
    fun testDynamicMedicalUnitsAndDosageExpansionHindi() {
        val input = "Take 1 tab of 500mg OD after food"
        val output = VernacularPhoneticEngine.processVernacularSpeechText(input, "hi")

        assertTrue(output.contains("पाँच सौ मिलीग्राम"))
        assertTrue(output.contains("दिन में एक बार"))
        assertTrue(output.contains("गोली"))
    }

    @Test
    fun testDynamicPhoneticTransliterationForMedicalBrandsAndSalts() {
        val dolo = VernacularPhoneticEngine.transliterateTokenToDevanagari("Dolo")
        val crocin = VernacularPhoneticEngine.transliterateTokenToDevanagari("Crocin")
        val bakson = VernacularPhoneticEngine.transliterateTokenToDevanagari("Bakson")
        val metformin = VernacularPhoneticEngine.transliterateTokenToDevanagari("Metformin")
        val pantoprazole = VernacularPhoneticEngine.transliterateTokenToDevanagari("Pantoprazole")
        val atorvastatin = VernacularPhoneticEngine.transliterateTokenToDevanagari("Atorvastatin")
        val telmisartan = VernacularPhoneticEngine.transliterateTokenToDevanagari("Telmisartan")

        // Validate that tokens are converted to Devanagari script (Unicode range 0900-097F)
        assertTrue(dolo.all { it in '\u0900'..'\u097F' || it.isWhitespace() })
        assertTrue(crocin.all { it in '\u0900'..'\u097F' || it.isWhitespace() })
        assertTrue(bakson.all { it in '\u0900'..'\u097F' || it.isWhitespace() })
        assertTrue(metformin.all { it in '\u0900'..'\u097F' || it.isWhitespace() })
        assertTrue(pantoprazole.endsWith("प्राज़ोल"))
        assertTrue(atorvastatin.endsWith("स्टेटिन"))
        assertTrue(telmisartan.endsWith("सार्टन"))
    }

    @Test
    fun testAcousticBreathingPauses() {
        val rawHindi = "नमस्ते अशोक जी! यह दवा सुरक्षित है। भोजन के बाद 1 गोली लीजिए।"
        val processed = VernacularPhoneticEngine.processVernacularSpeechText(rawHindi, "hi")

        // Verifies breath pauses (comma spacing after exclamation and danda)
        assertTrue(processed.contains("! ,"))
        assertTrue(processed.contains("। ,"))
        assertTrue(processed.contains("एक गोली"))
    }
}
