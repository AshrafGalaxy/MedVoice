package com.medvoice

import com.medvoice.core.ai.MedGemmaOrchestrator
import com.medvoice.core.ai.SafetyVerdict
import com.medvoice.core.data.local.dao.MedicineDao
import com.medvoice.core.data.local.entity.MedicationLogEntity
import com.medvoice.core.data.local.entity.MedicineEntity
import com.medvoice.core.domain.engine.SafetyEvaluationEngine
import com.medvoice.core.domain.engine.SafetyEvaluationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeMedicineDao : MedicineDao {
    private val logs = mutableListOf<MedicationLogEntity>()

    private val sampleMedicines = mutableListOf(
        MedicineEntity(
            id = 1,
            brandName = "Glycomet-SR 500",
            rawComposition = "Metformin Hydrochloride 500mg SR",
            manufacturer = "USV Ltd",
            dosageForm = "TABLET"
        ),
        MedicineEntity(
            id = 2,
            brandName = "Gluconorm-SR 500",
            rawComposition = "Metformin Hydrochloride 500mg",
            manufacturer = "Lupin Ltd",
            dosageForm = "TABLET"
        ),
        MedicineEntity(
            id = 10,
            brandName = "Combiflam",
            rawComposition = "Ibuprofen 400mg + Paracetamol 325mg",
            manufacturer = "Sanofi India",
            dosageForm = "TABLET"
        ),
        MedicineEntity(
            id = 11,
            brandName = "Ecosprin 75",
            rawComposition = "Aspirin 75mg Gastro-resistant",
            manufacturer = "USV Ltd",
            dosageForm = "TABLET"
        ),
        MedicineEntity(
            id = 12,
            brandName = "Maritima Euphrasia Eye Drops",
            rawComposition = "Cineraria Maritima + Euphrasia Ophthalmic 10ml",
            manufacturer = "SBL Pvt Ltd",
            dosageForm = "EYE_DROPS"
        )
    )

    override suspend fun searchCatalog(query: String): MedicineEntity? {
        val q = query.trim().lowercase()
        return sampleMedicines.firstOrNull {
            it.brandName.lowercase().contains(q) || it.rawComposition.lowercase().contains(q)
        }
    }

    override suspend fun findMedicineByPrefix(query: String): MedicineEntity? {
        val q = query.trim().lowercase()
        return sampleMedicines.firstOrNull { it.brandName.lowercase().startsWith(q) }
    }

    override suspend fun findMedicineByFts(query: String): MedicineEntity? {
        return searchCatalog(query)
    }

    override suspend fun getMedicineById(id: Long): MedicineEntity? {
        return sampleMedicines.firstOrNull { it.id == id }
    }

    override suspend fun getAllMedicines(): List<MedicineEntity> = sampleMedicines

    override suspend fun insertMedicine(medicine: MedicineEntity): Long {
        sampleMedicines.add(medicine)
        return medicine.id
    }

    override suspend fun logIntake(log: MedicationLogEntity): Long {
        logs.add(0, log)
        return logs.size.toLong()
    }

    override suspend fun getAllLogs(): List<MedicationLogEntity> = logs

    override suspend fun getRecentLogs(thresholdTime: Long): List<MedicationLogEntity> {
        return logs.filter { it.intakeTimestamp >= thresholdTime }
    }

    override suspend fun clearAllLogs() {
        logs.clear()
    }
}

class SafetyEngineTest {

    @Test
    fun testSafeFirstDoseEvaluation() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val ocrTokens = listOf("CADILA", "GLYCOMET-SR", "500", "METFORMIN")
        val result = engine.evaluateCandidateTokens(ocrTokens, "hi")

        assertTrue("First dose of Glycomet must be safe to take", result is SafetyEvaluationResult.SafeToTake)
        val safe = result as SafetyEvaluationResult.SafeToTake
        assertEquals("Glycomet-SR 500", safe.brandName)
        assertTrue(safe.saltName.contains("Metformin", ignoreCase = true))
        assertEquals(SafetyVerdict.SAFE_TO_TAKE, safe.safetyResult.safetyVerdict)
        assertTrue("Must include spoken dosage instructions", safe.instructionText.isNotBlank())
    }

    @Test
    fun testDuplicateDoseBlockedWithinWindow() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        // Log prior intake of Glycomet (Metformin) taken 1 hour ago
        fakeDao.logIntake(
            MedicationLogEntity(
                medicineId = 1,
                scannedText = "Glycomet-SR 500",
                parsedSalts = "Metformin Hydrochloride",
                intakeTimestamp = System.currentTimeMillis() - (1 * 3600 * 1000L),
                status = "TAKEN"
            )
        )

        // Patient scans Gluconorm (also Metformin!)
        val ocrTokens = listOf("LUPIN", "GLUCONORM-SR", "500", "METFORMIN")
        val result = engine.evaluateCandidateTokens(ocrTokens, "hi")

        assertTrue("Duplicate Metformin dose must be blocked", result is SafetyEvaluationResult.DuplicateDoseBlocked)
        val blocked = result as SafetyEvaluationResult.DuplicateDoseBlocked
        assertEquals(SafetyVerdict.DUPLICATE_OVERDOSE_BLOCKED, blocked.safetyResult.safetyVerdict)
        assertTrue(blocked.alertMessage.contains("सावधान") || blocked.alertMessage.contains("रुकिए") || blocked.alertMessage.contains("Warning"))
    }

    @Test
    fun testCriticalDrugDrugInteractionBlocked() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        // Patient took Ecosprin 75 (Aspirin) 2 hours ago
        fakeDao.logIntake(
            MedicationLogEntity(
                medicineId = 11,
                scannedText = "Ecosprin 75",
                parsedSalts = "Aspirin",
                intakeTimestamp = System.currentTimeMillis() - (2 * 3600 * 1000L),
                status = "TAKEN"
            )
        )

        // Patient attempts to take Combiflam (Ibuprofen NSAID!)
        val ocrTokens = listOf("SANOFI", "COMBIFLAM", "IBUPROFEN", "400MG")
        val result = engine.evaluateCandidateTokens(ocrTokens, "hi")

        assertTrue("Aspirin + Ibuprofen concurrent intake must trigger critical interaction alert", result is SafetyEvaluationResult.CriticalInteractionBlocked)
        val conflict = result as SafetyEvaluationResult.CriticalInteractionBlocked
        assertEquals(SafetyVerdict.CRITICAL_INTERACTION_BLOCKED, conflict.safetyResult.safetyVerdict)
        assertTrue(conflict.conflictRisk.contains("bleeding", ignoreCase = true) || conflict.conflictRisk.contains("hazard", ignoreCase = true))
    }

    @Test
    fun testZeroShotUnlistedPackagingEvaluation() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        // Completely unlisted foreign / rare blister pack
        val unlistedOcr = listOf(
            "NOVARTIS GALVUS MET",
            "VILDAGLIPTIN 50MG + METFORMIN HCL 500MG",
            "EXP 09/2028"
        )
        val result = engine.evaluateCandidateTokens(unlistedOcr, "hi")

        assertTrue("Unlisted real blister pack must resolve zero-shot to SafeToTake", result is SafetyEvaluationResult.SafeToTake)
        val safe = result as SafetyEvaluationResult.SafeToTake
        assertTrue(safe.saltName.contains("Metformin", ignoreCase = true) || safe.saltName.contains("Vildagliptin", ignoreCase = true))
    }
}
