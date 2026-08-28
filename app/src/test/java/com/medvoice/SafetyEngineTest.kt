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
        ),
        MedicineEntity(
            id = 13,
            brandName = "Sorbitrate 10",
            rawComposition = "Isosorbide Dinitrate 10mg",
            manufacturer = "Abbott",
            dosageForm = "TABLET"
        ),
        MedicineEntity(
            id = 14,
            brandName = "Telma 40",
            rawComposition = "Telmisartan 40mg",
            manufacturer = "Glenmark",
            dosageForm = "TABLET"
        ),
        MedicineEntity(
            id = 15,
            brandName = "Warf 5",
            rawComposition = "Warfarin Sodium 5mg",
            manufacturer = "Cipla",
            dosageForm = "TABLET"
        ),
        MedicineEntity(
            id = 16,
            brandName = "Cifran 500",
            rawComposition = "Ciprofloxacin 500mg",
            manufacturer = "Sun Pharma",
            dosageForm = "TABLET"
        )
    )

    override suspend fun searchCatalog(query: String): MedicineEntity? {
        val q = query.trim().lowercase()
        if (q.length < 3) return null
        return sampleMedicines.firstOrNull { med ->
            val brandWords = med.brandName.lowercase().split(Regex("[\\s,/\\-]+"))
            val saltWords = med.rawComposition.lowercase().split(Regex("[\\s,/\\-]+"))
            brandWords.any { it == q || it.startsWith(q) } || saltWords.any { it == q || it.startsWith(q) }
        }
    }


    override suspend fun findMedicineByFts(query: String): MedicineEntity? {
        return searchCatalog(query)
    }

    override suspend fun getMedicineById(id: Long): MedicineEntity? {
        return sampleMedicines.firstOrNull { it.id == id }
    }

    override suspend fun getCabinetMedicines(): List<MedicineEntity> = sampleMedicines

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

    override suspend fun deleteLogsForMedicine(medicineId: Long) {
        logs.removeAll { it.medicineId == medicineId }
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
    fun testFailClosedUnidentifiedGibberishScanBlocked() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        // Random non-medicine barcode or noise text
        val noiseTokens = listOf("ACME COURIER 12345", "DO NOT BEND", "TRACKING XYZ")
        val result = engine.evaluateCandidateTokens(noiseTokens, "hi")

        assertTrue("Unidentified noisy scan must fail closed to UnidentifiedMedicineBlocked", result is SafetyEvaluationResult.UnidentifiedMedicineBlocked)
        val blocked = result as SafetyEvaluationResult.UnidentifiedMedicineBlocked
        assertTrue(blocked.alertMessage.contains("पहचान") || blocked.alertMessage.contains("Unidentified"))
    }

    @Test
    fun testExpiredMedicineBlocked() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        // Packaging with expired date
        val expiredTokens = listOf(
            "CROXIN 500",
            "PARACETAMOL 500MG",
            "EXP 01/2021",
            "BATCH B992"
        )
        val result = engine.evaluateCandidateTokens(expiredTokens, "hi")

        assertTrue("Expired packaging must be blocked from consumption", result is SafetyEvaluationResult.ExpiredMedicineBlocked)
        val expired = result as SafetyEvaluationResult.ExpiredMedicineBlocked
        assertEquals(SafetyVerdict.EXPIRED_MEDICINE_BLOCKED, expired.safetyResult.safetyVerdict)
        assertTrue(expired.alertMessage.contains("समाप्त") || expired.alertMessage.contains("एक्सपायर") || expired.alertMessage.contains("expired", ignoreCase = true))
    }

    @Test
    fun testPolypharmacyNitrateSildenafilBlocked() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        // Patient on Sorbitrate (Nitrate)
        fakeDao.logIntake(
            MedicationLogEntity(
                medicineId = 13,
                scannedText = "Sorbitrate 10",
                parsedSalts = "Isosorbide Dinitrate",
                intakeTimestamp = System.currentTimeMillis() - (1 * 3600 * 1000L),
                status = "TAKEN"
            )
        )

        // Patient scans Sildenafil (PDE5 Inhibitor)
        val sildenafilTokens = listOf("MANFORCE 50", "SILDENAFIL CITRATE 50MG", "MANKIND")
        val result = engine.evaluateCandidateTokens(sildenafilTokens, "hi")

        assertTrue("Nitrate + Sildenafil must trigger fatal interaction block", result is SafetyEvaluationResult.CriticalInteractionBlocked)
        val conflict = result as SafetyEvaluationResult.CriticalInteractionBlocked
        assertTrue(conflict.conflictRisk.contains("hypotension", ignoreCase = true))
    }

    @Test
    fun testPolypharmacyArbSpironolactoneBlocked() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        // Patient on Telmisartan (ARB)
        fakeDao.logIntake(
            MedicationLogEntity(
                medicineId = 14,
                scannedText = "Telma 40",
                parsedSalts = "Telmisartan",
                intakeTimestamp = System.currentTimeMillis() - (2 * 3600 * 1000L),
                status = "TAKEN"
            )
        )

        // Patient scans Spironolactone (Aldactone)
        val spiroTokens = listOf("ALDACTONE 25", "SPIRONOLACTONE 25MG", "RPG LIFE")
        val result = engine.evaluateCandidateTokens(spiroTokens, "hi")

        assertTrue("ARB + Spironolactone must trigger hyperkalemia hazard block", result is SafetyEvaluationResult.CriticalInteractionBlocked)
        val conflict = result as SafetyEvaluationResult.CriticalInteractionBlocked
        assertTrue(conflict.conflictRisk.contains("hyperkalemia", ignoreCase = true))
    }

    @Test
    fun testPolypharmacyWarfarinNsaidBlocked() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        // Patient on Warfarin anticoagulant
        fakeDao.logIntake(
            MedicationLogEntity(
                medicineId = 15,
                scannedText = "Warf 5",
                parsedSalts = "Warfarin Sodium",
                intakeTimestamp = System.currentTimeMillis() - (3 * 3600 * 1000L),
                status = "TAKEN"
            )
        )

        // Patient attempts to take Diclofenac
        val nsaidTokens = listOf("VOVERAN 50", "DICLOFENAC SODIUM 50MG", "NOVARTIS")
        val result = engine.evaluateCandidateTokens(nsaidTokens, "hi")

        assertTrue("Warfarin + NSAID must trigger hemorrhage risk block", result is SafetyEvaluationResult.CriticalInteractionBlocked)
        val conflict = result as SafetyEvaluationResult.CriticalInteractionBlocked
        assertTrue(conflict.conflictRisk.contains("hemorrhage", ignoreCase = true))
    }

    @Test
    fun testPolypharmacyQuinoloneAntacidBlocked() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        // Patient took Gelusil antacid
        fakeDao.logIntake(
            MedicationLogEntity(
                medicineId = 99,
                scannedText = "Gelusil Antacid",
                parsedSalts = "Aluminium Hydroxide + Magnesium",
                intakeTimestamp = System.currentTimeMillis() - (1 * 3600 * 1000L),
                status = "TAKEN"
            )
        )

        // Patient scans Ciprofloxacin
        val ciproTokens = listOf("CIFRAN 500", "CIPROFLOXACIN 500MG", "SUN PHARMA")
        val result = engine.evaluateCandidateTokens(ciproTokens, "hi")

        assertTrue("Quinolone + Antacid must trigger absorption chelation conflict", result is SafetyEvaluationResult.CriticalInteractionBlocked)
        val conflict = result as SafetyEvaluationResult.CriticalInteractionBlocked
        assertTrue(conflict.conflictRisk.contains("chelate", ignoreCase = true) || conflict.conflictRisk.contains("absorption", ignoreCase = true))
    }

    @Test
    fun testZeroShotUnlistedPackagingEvaluation() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val orchestrator = MedGemmaOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        // Completely unlisted real packaging
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
