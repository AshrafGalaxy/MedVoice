package com.medvoice

import com.medvoice.core.ai.MedGemmaOrchestrator
import com.medvoice.core.data.local.entity.MedicineEntity
import com.medvoice.core.domain.engine.SafetyEvaluationEngine
import com.medvoice.core.domain.engine.SafetyEvaluationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalOcrParsingTest {

    @Test
    fun testRealWorldEyeDropScanEvaluation() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val safetyEngine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val ocrTokens = listOf(
            "MARITIMA EUPHRASIA EYE DROPS",
            "CINERARIA MARITIMA & EUPHRASIA OPHTHALMIC",
            "10 ML STERILE SOLUTION",
            "FOR EXTERNAL USE ONLY",
            "MFD BY SBL PVT LTD"
        )

        val result = safetyEngine.evaluateCandidateTokens(ocrTokens, "hi")
        assertTrue("Real eye drop packaging must resolve to SafeToTake", result is SafetyEvaluationResult.SafeToTake)
        val safe = result as SafetyEvaluationResult.SafeToTake
        assertTrue(safe.brandName.contains("MARITIMA", ignoreCase = true) || safe.brandName.contains("EUPHRASIA", ignoreCase = true))
        assertEquals("EYE_DROPS", safe.dosageForm)
        assertTrue(safe.instructionText.contains("आई ड्रॉप्स") || safe.instructionText.contains("आँखों") || safe.instructionText.contains("Drop"))
    }

    @Test
    fun testRealWorldCoughSyrupScanEvaluation() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val safetyEngine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val ocrTokens = listOf(
            "BENADRYL COUGH FORMULA SYRUP",
            "DEXTROMETHORPHAN HYDROBROMIDE 10MG",
            "NET VOL 100 ML",
            "SHAKE WELL BEFORE USE"
        )

        val result = safetyEngine.evaluateCandidateTokens(ocrTokens, "hi")
        assertTrue(result is SafetyEvaluationResult.SafeToTake)
        val safe = result as SafetyEvaluationResult.SafeToTake
        assertEquals("SYRUP", safe.dosageForm)
        assertTrue(safe.instructionText.contains("सिरप") || safe.instructionText.contains("पिएँ") || safe.instructionText.contains("Syrup"))
    }

    @Test
    fun testRealWorldTopicalGelScanEvaluation() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val safetyEngine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val ocrTokens = listOf(
            "VOLINI PAIN RELIEF GEL",
            "DICLOFENAC DIETHYLAMINE GEL 30GM",
            "SUN PHARMA"
        )

        val result = safetyEngine.evaluateCandidateTokens(ocrTokens, "hi")
        assertTrue(result is SafetyEvaluationResult.SafeToTake)
        val safe = result as SafetyEvaluationResult.SafeToTake
        assertEquals("GEL", safe.dosageForm)
        assertTrue(safe.instructionText.contains("जेल") || safe.instructionText.contains("लगाएँ") || safe.instructionText.contains("Gel"))
    }

    @Test
    fun testAutoPersistDiscoveredMedicineInDao() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val eyeDropMed = MedicineEntity(
            id = 5001L,
            brandName = "Maritima Euphrasia Eye Drops",
            rawComposition = "Cineraria Maritima + Euphrasia Ophthalmic 10ml",
            manufacturer = "SBL Pvt Ltd",
            dosageForm = "EYE_DROPS"
        )
        val insertedId = fakeDao.insertMedicine(eyeDropMed)
        assertEquals(5001L, insertedId)
        val fetched = fakeDao.getMedicineById(5001L)
        assertEquals("Maritima Euphrasia Eye Drops", fetched?.brandName)
    }
}
