package com.medvoice

import com.medvoice.core.ai.AiPharmacologyEngine
import com.medvoice.core.domain.engine.SafetyEvaluationEngine
import com.medvoice.core.domain.engine.SafetyEvaluationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalOcrParsingTest {

    @Test
    fun testRealWorldEyeDropScanEvaluation() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val aiEngine = AiPharmacologyEngine()
        val safetyEngine = SafetyEvaluationEngine(fakeDao, aiEngine)

        val ocrTokens = listOf(
            "MARITIMA EUPHRASIA EYE DROPS",
            "CINERARIA MARITIMA & EUPHRASIA OPHTHALMIC",
            "10 ML STERILE SOLUTION",
            "FOR EXTERNAL USE ONLY",
            "MFD BY SBL PVT LTD"
        )

        val result = safetyEngine.evaluateCandidateTokens(ocrTokens)
        assertTrue("Real eye drop packaging must resolve to SafeToTake", result is SafetyEvaluationResult.SafeToTake)
        val safe = result as SafetyEvaluationResult.SafeToTake
        assertTrue(safe.medicine.brand_name.contains("MARITIMA", ignoreCase = true) || safe.medicine.brand_name.contains("EUPHRASIA", ignoreCase = true))
        assertEquals("EYE_DROPS", safe.medicine.dosage_form)
        assertTrue(safe.vernacularInstructionHi.contains("आई ड्रॉप्स") || safe.vernacularInstructionHi.contains("आँखों"))
        assertTrue(safe.vernacularInstructionEn.contains("eye drop", ignoreCase = true))
    }

    @Test
    fun testRealWorldCoughSyrupScanEvaluation() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val aiEngine = AiPharmacologyEngine()
        val safetyEngine = SafetyEvaluationEngine(fakeDao, aiEngine)

        val ocrTokens = listOf(
            "BENADRYL COUGH FORMULA SYRUP",
            "DEXTROMETHORPHAN HYDROBROMIDE 10MG",
            "NET VOL 100 ML",
            "SHAKE WELL BEFORE USE"
        )

        val result = safetyEngine.evaluateCandidateTokens(ocrTokens)
        assertTrue(result is SafetyEvaluationResult.SafeToTake)
        val safe = result as SafetyEvaluationResult.SafeToTake
        assertEquals("SYRUP", safe.medicine.dosage_form)
        assertTrue(safe.vernacularInstructionHi.contains("सिरप") || safe.vernacularInstructionHi.contains("पीने"))
    }

    @Test
    fun testRealWorldTopicalGelScanEvaluation() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val aiEngine = AiPharmacologyEngine()
        val safetyEngine = SafetyEvaluationEngine(fakeDao, aiEngine)

        val ocrTokens = listOf(
            "VOLINI PAIN RELIEF GEL",
            "DICLOFENAC DIETHYLAMINE GEL 30GM",
            "SUN PHARMA"
        )

        val result = safetyEngine.evaluateCandidateTokens(ocrTokens)
        assertTrue(result is SafetyEvaluationResult.SafeToTake)
        val safe = result as SafetyEvaluationResult.SafeToTake
        assertEquals("GEL", safe.medicine.dosage_form)
        assertTrue(safe.vernacularInstructionHi.contains("मलहम") || safe.vernacularInstructionHi.contains("जेल"))
    }
}
