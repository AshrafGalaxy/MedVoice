package com.medvoice

import com.medvoice.core.ai.ClinicalSafetyOrchestrator
import com.medvoice.core.ai.SafetyVerdict
import com.medvoice.core.data.local.dao.MedicineDao
import com.medvoice.core.data.local.entity.CabinetPrescriptionEntity
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
    private val cabinetList = mutableListOf<CabinetPrescriptionEntity>()

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
        val qClean = q.replace(Regex("[^a-z0-9]"), "")
        if (qClean.length < 3) return null
        return sampleMedicines.firstOrNull { med ->
            val b = med.brandName.lowercase()
            val bClean = b.replace(Regex("[^a-z0-9]"), "")
            b == q || b.startsWith(q) || (b.length >= 4 && q.startsWith(b)) ||
            bClean == qClean || bClean.startsWith(qClean) || qClean.startsWith(bClean)
        }
    }

    override suspend fun findMedicineByFts(query: String): MedicineEntity? {
        return searchCatalog(query)
    }

    override suspend fun getMedicineById(id: Long): MedicineEntity? {
        return sampleMedicines.firstOrNull { it.id == id }
    }

    override suspend fun getCabinetMedicines(): List<MedicineEntity> = sampleMedicines

    override suspend fun insertCabinetPrescription(prescription: CabinetPrescriptionEntity): Long {
        cabinetList.add(prescription)
        return prescription.id
    }

    override suspend fun getAllCabinetPrescriptions(): List<CabinetPrescriptionEntity> = cabinetList

    override suspend fun getCabinetPrescriptionById(id: Long): CabinetPrescriptionEntity? = cabinetList.firstOrNull { it.id == id }

    override suspend fun deleteCabinetPrescription(id: Long) {
        cabinetList.removeAll { it.id == id }
    }

    override suspend fun clearCabinetPrescriptions() {
        cabinetList.clear()
    }

    override suspend fun searchCabinetPrescriptions(query: String): List<CabinetPrescriptionEntity> {
        val q = query.lowercase()
        return cabinetList.filter { it.brandName.lowercase().contains(q) || it.rawComposition.lowercase().contains(q) }
    }

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

    private val fakeDao = FakeMedicineDao()

    @Test
    fun testSafeFirstDose_GlycometSR500() = runBlocking {
        fakeDao.clearAllLogs()
        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val scannedTokens = listOf(
            "Glycomet-SR 500",
            "Metformin Hydrochloride Prolonged Release Tablets IP",
            "500 mg",
            "USV Private Limited"
        )

        val result = engine.evaluateCandidateTokens(scannedTokens, locale = "hi", isExplicitSnap = true)
        assertTrue("Expected SafeToTake, got: $result", result is SafetyEvaluationResult.SafeToTake)
        val safeResult = (result as SafetyEvaluationResult.SafeToTake).safetyResult
        assertEquals(SafetyVerdict.SAFE_TO_TAKE, safeResult.safetyVerdict)
        assertEquals("Glycomet-SR 500", result.matchedMedicine?.brandName)
    }

    @Test
    fun testDuplicateDoseDetection_SameBrandWithinActiveWindow() = runBlocking {
        fakeDao.clearAllLogs()
        fakeDao.logIntake(
            MedicationLogEntity(
                medicineId = 1,
                scannedText = "Glycomet-SR 500",
                parsedSalts = "Metformin Hydrochloride",
                status = "TAKEN",
                intakeTimestamp = System.currentTimeMillis() - (2 * 3600 * 1000L) // 2 hours ago
            )
        )

        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val scannedTokens = listOf("Glycomet-SR 500", "Metformin Hydrochloride 500mg")
        val result = engine.evaluateCandidateTokens(scannedTokens, locale = "hi", isExplicitSnap = true)

        assertTrue("Expected DuplicateDoseBlocked, got: $result", result is SafetyEvaluationResult.DuplicateDoseBlocked)
        val dupResult = (result as SafetyEvaluationResult.DuplicateDoseBlocked).safetyResult
        assertEquals(SafetyVerdict.DUPLICATE_OVERDOSE_BLOCKED, dupResult.safetyVerdict)
    }

    @Test
    fun testDuplicateMoleculeDetection_CrossBrandMetformin() = runBlocking {
        fakeDao.clearAllLogs()
        // Patient took Gluconorm-SR 500 3 hours ago
        fakeDao.logIntake(
            MedicationLogEntity(
                medicineId = 2,
                scannedText = "Gluconorm-SR 500",
                parsedSalts = "Metformin Hydrochloride",
                status = "TAKEN",
                intakeTimestamp = System.currentTimeMillis() - (3 * 3600 * 1000L)
            )
        )

        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        // Patient scans Glycomet-SR 500 (Different brand, identical Metformin molecule)
        val scannedTokens = listOf("Glycomet-SR 500", "Metformin Hydrochloride 500mg SR")
        val result = engine.evaluateCandidateTokens(scannedTokens, locale = "hi", isExplicitSnap = true)

        assertTrue("Expected cross-brand duplicate block, got: $result", result is SafetyEvaluationResult.DuplicateDoseBlocked)
        val dup = (result as SafetyEvaluationResult.DuplicateDoseBlocked).safetyResult
        assertEquals(SafetyVerdict.DUPLICATE_OVERDOSE_BLOCKED, dup.safetyVerdict)
    }

    @Test
    fun testPolypharmacyConflict_AspirinAndIbuprofen() = runBlocking {
        fakeDao.clearAllLogs()
        // Patient took Ecosprin 75 1 hour ago
        fakeDao.logIntake(
            MedicationLogEntity(
                medicineId = 11,
                scannedText = "Ecosprin 75",
                parsedSalts = "Aspirin",
                status = "TAKEN",
                intakeTimestamp = System.currentTimeMillis() - (1 * 3600 * 1000L)
            )
        )

        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        // Patient now scans Combiflam (contains Ibuprofen)
        val scannedTokens = listOf("Combiflam", "Ibuprofen 400mg + Paracetamol 325mg")
        val result = engine.evaluateCandidateTokens(scannedTokens, locale = "hi", isExplicitSnap = true)

        assertTrue("Expected CriticalInteractionBlocked, got: $result", result is SafetyEvaluationResult.CriticalInteractionBlocked)
        val conflict = (result as SafetyEvaluationResult.CriticalInteractionBlocked).safetyResult
        assertEquals(SafetyVerdict.CRITICAL_INTERACTION_BLOCKED, conflict.safetyVerdict)
    }

    @Test
    fun testOphthalmicEyeDropClassification() = runBlocking {
        fakeDao.clearAllLogs()
        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val scannedTokens = listOf("Maritima Euphrasia Eye Drops", "Cineraria Maritima + Euphrasia Ophthalmic 10ml")
        val result = engine.evaluateCandidateTokens(scannedTokens, locale = "hi", isExplicitSnap = true)

        assertTrue("Expected SafeToTake for Eye Drops, got: $result", result is SafetyEvaluationResult.SafeToTake)
        val safe = (result as SafetyEvaluationResult.SafeToTake).safetyResult
        assertEquals(SafetyVerdict.SAFE_TO_TAKE, safe.safetyVerdict)
        assertEquals("EYE_DROPS", safe.dosageForm)
    }

    @Test
    fun testExpiredMedicationBlocked() = runBlocking {
        fakeDao.clearAllLogs()
        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val scannedTokens = listOf(
            "Glycomet-SR 500",
            "Metformin Hydrochloride 500mg",
            "EXP. DATE: 01/2020",
            "BATCH NO: BT9921"
        )
        val result = engine.evaluateCandidateTokens(scannedTokens, locale = "hi", isExplicitSnap = true)

        assertTrue("Expected ExpiredMedicineBlocked, got: $result", result is SafetyEvaluationResult.ExpiredMedicineBlocked)
        val expired = (result as SafetyEvaluationResult.ExpiredMedicineBlocked).safetyResult
        assertEquals(SafetyVerdict.EXPIRED_MEDICINE_BLOCKED, expired.safetyVerdict)
    }

    @Test
    fun testNitrateAndPde5FatalInteraction() = runBlocking {
        fakeDao.clearAllLogs()
        // Patient took Sorbitrate 10 2 hours ago
        fakeDao.logIntake(
            MedicationLogEntity(
                medicineId = 13,
                scannedText = "Sorbitrate 10",
                parsedSalts = "Isosorbide Dinitrate",
                status = "TAKEN",
                intakeTimestamp = System.currentTimeMillis() - (2 * 3600 * 1000L)
            )
        )

        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        // Patient scans Sildenafil
        val scannedTokens = listOf("Manforce 50", "Sildenafil Citrate 50mg Tablets")
        val result = engine.evaluateCandidateTokens(scannedTokens, locale = "hi", isExplicitSnap = true)

        assertTrue("Expected Fatal Interaction Block, got: $result", result is SafetyEvaluationResult.CriticalInteractionBlocked)
        val conflict = (result as SafetyEvaluationResult.CriticalInteractionBlocked).safetyResult
        assertEquals(SafetyVerdict.CRITICAL_INTERACTION_BLOCKED, conflict.safetyVerdict)
    }

    @Test
    fun testBaksonDandruffAidTablets_ClassifiedAsOralTablet() = runBlocking {
        fakeDao.clearAllLogs()
        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val scannedTokens = listOf(
            "Bakson's Dandruff Aid Tablets",
            "Dr. Bakshi Homeopathy - B41",
            "Thuja Occidentalis, Natrum Muriaticum",
            "75 Tablets",
            "Composition: Each tablet contains..."
        )
        val result = engine.evaluateCandidateTokens(scannedTokens, locale = "en", isExplicitSnap = true)

        assertTrue("Expected SafeToTake for Oral Homeopathic Tablets, got: $result", result is SafetyEvaluationResult.SafeToTake)
        val safe = (result as SafetyEvaluationResult.SafeToTake).safetyResult
        assertEquals(SafetyVerdict.SAFE_TO_TAKE, safe.safetyVerdict)
        assertEquals("TABLET", safe.dosageForm)
        assertEquals(com.medvoice.core.ai.FoodTimingRule.AFTER_FOOD, safe.foodTimingRule)
    }

    @Test
    fun testExternalTopicalLotion_BaksonDandruffAid() = runBlocking {
        fakeDao.clearAllLogs()
        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val scannedTokens = listOf(
            "Bakson's Dandruff Aid",
            "Thuja Occidentalis, Cantharis, Cochlearia",
            "For External Application Only",
            "Hair and Scalp Care Lotion"
        )
        val result = engine.evaluateCandidateTokens(scannedTokens, locale = "hi", isExplicitSnap = true)

        assertTrue("Expected SafeToTake for Topical Lotion, got: $result", result is SafetyEvaluationResult.SafeToTake)
        val safe = (result as SafetyEvaluationResult.SafeToTake).safetyResult
        assertEquals(SafetyVerdict.SAFE_TO_TAKE, safe.safetyVerdict)
        assertEquals("TOPICAL_LOTION", safe.dosageForm)
        assertEquals(com.medvoice.core.ai.FoodTimingRule.NOT_APPLICABLE_EXTERNAL, safe.foodTimingRule)
        assertTrue("Expected external instruction in Hindi", safe.spokenVernacularText.contains("बाहरी उपयोग") || safe.spokenVernacularText.contains("सिर"))
    }

    @Test
    fun testDiseaseDrugContraindication_HypertensionAndNsaid() = runBlocking {
        fakeDao.clearAllLogs()
        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val scannedTokens = listOf("Brufen 400", "Ibuprofen Tablets IP 400mg")
        val patientConditions = setOf("Hypertension (BP)")

        val result = engine.evaluateCandidateTokens(
            tokens = scannedTokens,
            locale = "en",
            isExplicitSnap = true,
            patientConditions = patientConditions
        )

        assertTrue("Expected CriticalInteractionBlocked for Hypertension + Ibuprofen, got: $result", result is SafetyEvaluationResult.CriticalInteractionBlocked)
        val conflict = (result as SafetyEvaluationResult.CriticalInteractionBlocked).safetyResult
        assertEquals(SafetyVerdict.CRITICAL_INTERACTION_BLOCKED, conflict.safetyVerdict)
        assertTrue("Expected clinical reason to mention Hypertension", conflict.clinicalReason.contains("Hypertension"))
    }

    @Test
    fun testDiseaseDrugContraindication_DiabetesAndCorticosteroid() = runBlocking {
        fakeDao.clearAllLogs()
        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val scannedTokens = listOf("Wysolone 10", "Prednisolone Tablets IP 10mg")
        val patientConditions = setOf("Type-2 Diabetes")

        val result = engine.evaluateCandidateTokens(
            tokens = scannedTokens,
            locale = "en",
            isExplicitSnap = true,
            patientConditions = patientConditions
        )

        assertTrue("Expected CriticalInteractionBlocked for Diabetes + Prednisolone, got: $result", result is SafetyEvaluationResult.CriticalInteractionBlocked)
        val conflict = (result as SafetyEvaluationResult.CriticalInteractionBlocked).safetyResult
        assertEquals(SafetyVerdict.CRITICAL_INTERACTION_BLOCKED, conflict.safetyVerdict)
        assertTrue("Expected clinical reason to mention Diabetes", conflict.clinicalReason.contains("Diabetes"))
    }

    @Test
    fun testDiseaseDrugContraindication_CardiacAndNsaid() = runBlocking {
        fakeDao.clearAllLogs()
        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val scannedTokens = listOf("Voveran 50", "Diclofenac Sodium 50mg Tablets")
        val patientConditions = setOf("Cardiac / Heart Condition")

        val result = engine.evaluateCandidateTokens(
            tokens = scannedTokens,
            locale = "en",
            isExplicitSnap = true,
            patientConditions = patientConditions
        )

        assertTrue("Expected CriticalInteractionBlocked for Cardiac + Diclofenac, got: $result", result is SafetyEvaluationResult.CriticalInteractionBlocked)
        val conflict = (result as SafetyEvaluationResult.CriticalInteractionBlocked).safetyResult
        assertEquals(SafetyVerdict.CRITICAL_INTERACTION_BLOCKED, conflict.safetyVerdict)
        assertTrue("Expected clinical reason to mention Cardiac", conflict.clinicalReason.contains("Cardiac"))
    }

    @Test
    fun testDiseaseDrugContraindication_ThyroidAndCalcium() = runBlocking {
        fakeDao.clearAllLogs()
        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val scannedTokens = listOf("Shelcal 500", "Calcium and Vitamin D3 Tablets")
        val patientConditions = setOf("Thyroid Disorder")

        val result = engine.evaluateCandidateTokens(
            tokens = scannedTokens,
            locale = "en",
            isExplicitSnap = true,
            patientConditions = patientConditions
        )

        assertTrue("Expected CriticalInteractionBlocked for Thyroid + Calcium, got: $result", result is SafetyEvaluationResult.CriticalInteractionBlocked)
        val conflict = (result as SafetyEvaluationResult.CriticalInteractionBlocked).safetyResult
        assertEquals(SafetyVerdict.CRITICAL_INTERACTION_BLOCKED, conflict.safetyVerdict)
        assertTrue("Expected clinical reason to mention Thyroid", conflict.clinicalReason.contains("Thyroid"))
    }

    @Test
    fun testCosmeticSkincareMoisturizer_RejectsFalsePositiveInjectionMatch() = runBlocking {
        fakeDao.clearAllLogs()
        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val scannedTokens = listOf(
            "Minimalist Vitamin B5 Moisturizer",
            "Oil-Free Daily Face Moisturiser with Magnesium Aspartate and Zinc Gluconate",
            "Ingredients: Aqua, Glycerin, Betaine, Magnesium Aspartate, Zinc Gluconate, Copper Gluconate, Sodium Hyaluronate",
            "Net Vol: 50ml"
        )

        val result = engine.evaluateCandidateTokens(
            tokens = scannedTokens,
            locale = "en",
            isExplicitSnap = true
        )

        assertTrue(
            "Expected UnidentifiedMedicineBlocked for cosmetic skincare, got: $result",
            result is SafetyEvaluationResult.UnidentifiedMedicineBlocked
        )
        val nonDrugResult = (result as SafetyEvaluationResult.UnidentifiedMedicineBlocked).safetyResult
        assertEquals(SafetyVerdict.UNIDENTIFIED_MEDICINE_BLOCKED, nonDrugResult.safetyVerdict)
        assertEquals("COSMETIC_NON_DRUG", nonDrugResult.therapeuticClass)
        assertTrue("Expected brandName to not be Magnesium Sulfate Injection", nonDrugResult.brandName != "Magnesium Sulfate 50% Injection")
    }

    @Test
    fun testFaceWashCleanser_ClassifiedAsCosmeticNonDrug() = runBlocking {
        fakeDao.clearAllLogs()
        val orchestrator = ClinicalSafetyOrchestrator()
        val engine = SafetyEvaluationEngine(fakeDao, orchestrator)

        val scannedTokens = listOf(
            "Salicylic Acid Daily Cleanser Face Wash",
            "For Oily & Acne Prone Skin",
            "Aqua, Sodium Laureth Sulfate, Cocamidopropyl Betaine, Salicylic Acid, Glycerin",
            "100ml"
        )

        val result = engine.evaluateCandidateTokens(
            tokens = scannedTokens,
            locale = "en",
            isExplicitSnap = true
        )

        assertTrue(
            "Expected UnidentifiedMedicineBlocked for Face Wash, got: $result",
            result is SafetyEvaluationResult.UnidentifiedMedicineBlocked
        )
        val nonDrugResult = (result as SafetyEvaluationResult.UnidentifiedMedicineBlocked).safetyResult
        assertEquals("COSMETIC_NON_DRUG", nonDrugResult.therapeuticClass)
    }
}
