package com.medvoice

import com.medvoice.core.data.local.dao.ContraindicationResult
import com.medvoice.core.data.local.dao.MedicineDao
import com.medvoice.core.data.local.dao.MedicineQueryResult
import com.medvoice.core.data.local.entity.MedicationLogEntity
import com.medvoice.core.domain.engine.SafetyEvaluationEngine
import com.medvoice.core.domain.engine.SafetyEvaluationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeMedicineDao : MedicineDao {
    private val logs = mutableListOf<MedicationLogEntity>()

    private val sampleMedicines = listOf(
        MedicineQueryResult(
            id = 1,
            brand_name = "Glycomet-SR 500",
            dosage_form = "TABLET",
            strength_mg = 500.0,
            primary_salt_id = 1,
            is_high_risk = false,
            vernacular_usage_en = "This is your diabetes blood sugar tablet.",
            vernacular_usage_hi = "यह आपकी शुगर की गोली है।",
            vernacular_usage_mr = "हे तुमचे साखरेचे औषध आहे.",
            salt_name = "Metformin Hydrochloride",
            therapeutic_class = "ANTIDIABETIC",
            max_daily_dose_mg = 2000.0,
            active_window_hours = 10.0,
            vernacular_salt_desc_en = "Diabetes blood sugar control medicine",
            vernacular_salt_desc_hi = "शुगर नियंत्रित करने की दवा",
            vernacular_salt_desc_mr = "रक्तातील साखर नियंत्रित करणारे औषध",
            rule_code = "AFTER_MEAL",
            food_relation = "AFTER_FOOD",
            vernacular_instruction_en = "Take 1 tablet with water after your meal.",
            vernacular_instruction_hi = "खाना खाने के बाद एक गोली पानी के साथ लें।",
            vernacular_instruction_mr = "जेवणानंतर एक गोळी पाण्यासोबत घ्या."
        ),
        MedicineQueryResult(
            id = 2,
            brand_name = "Gluconorm-SR 500",
            dosage_form = "TABLET",
            strength_mg = 500.0,
            primary_salt_id = 1, // Same active salt: Metformin!
            is_high_risk = false,
            vernacular_usage_en = "This is your diabetes blood sugar tablet.",
            vernacular_usage_hi = "यह आपकी शुगर की गोली है।",
            vernacular_usage_mr = "हे तुमचे साखरेचे औषध आहे.",
            salt_name = "Metformin Hydrochloride",
            therapeutic_class = "ANTIDIABETIC",
            max_daily_dose_mg = 2000.0,
            active_window_hours = 10.0,
            vernacular_salt_desc_en = "Diabetes blood sugar control medicine",
            vernacular_salt_desc_hi = "शुगर नियंत्रित करने की दवा",
            vernacular_salt_desc_mr = "रक्तातील साखर नियंत्रित करणारे औषध",
            rule_code = "AFTER_MEAL",
            food_relation = "AFTER_FOOD",
            vernacular_instruction_en = "Take 1 tablet with water after your meal.",
            vernacular_instruction_hi = "खाना खाने के बाद एक गोली पानी के साथ लें।",
            vernacular_instruction_mr = "जेवणानंतर एक गोळी पाण्यासोबत घ्या."
        ),
        MedicineQueryResult(
            id = 10,
            brand_name = "Combiflam",
            dosage_form = "TABLET",
            strength_mg = 400.0,
            primary_salt_id = 7, // Ibuprofen
            is_high_risk = false,
            vernacular_usage_en = "This is for pain and fever relief.",
            vernacular_usage_hi = "यह दर्द और बुखार की दवा है।",
            vernacular_usage_mr = "हे अंगदुखी आणि तापाचे औषध आहे.",
            salt_name = "Ibuprofen",
            therapeutic_class = "NSAID_ANALGESIC",
            max_daily_dose_mg = 2400.0,
            active_window_hours = 8.0,
            vernacular_salt_desc_en = "Pain and anti-inflammatory relief",
            vernacular_salt_desc_hi = "दर्द और सूजन की दवा",
            vernacular_salt_desc_mr = "वेदना आणि सूज कमी करणारे औषध",
            rule_code = "AFTER_MEAL",
            food_relation = "AFTER_FOOD",
            vernacular_instruction_en = "Take strictly after food.",
            vernacular_instruction_hi = "खाना खाने के बाद लें।",
            vernacular_instruction_mr = "जेवण झाल्यावरच घ्या."
        ),
        MedicineQueryResult(
            id = 11,
            brand_name = "Ecosprin 75",
            dosage_form = "TABLET",
            strength_mg = 75.0,
            primary_salt_id = 8, // Aspirin
            is_high_risk = false,
            vernacular_usage_en = "This is your blood thinner aspirin tablet.",
            vernacular_usage_hi = "यह खून पतला करने की गोली है।",
            vernacular_usage_mr = "हे रक्त पातळ करण्याचे औषध आहे.",
            salt_name = "Aspirin",
            therapeutic_class = "ANTIPLATELET",
            max_daily_dose_mg = 325.0,
            active_window_hours = 24.0,
            vernacular_salt_desc_en = "Blood thinner antiplatelet agent",
            vernacular_salt_desc_hi = "खून पतला करने की दवा",
            vernacular_salt_desc_mr = "रक्त पातळ करणारे औषध",
            rule_code = "AFTER_MEAL",
            food_relation = "AFTER_FOOD",
            vernacular_instruction_en = "Take after food.",
            vernacular_instruction_hi = "खाना खाने के बाद लें।",
            vernacular_instruction_mr = "जेवण झाल्यावरच घ्या."
        )
    )

    override suspend fun findMedicineByPrefix(query: String): MedicineQueryResult? {
        return sampleMedicines.firstOrNull { it.brand_name.startsWith(query, ignoreCase = true) }
    }

    override suspend fun findMedicineByFts(query: String): MedicineQueryResult? {
        return sampleMedicines.firstOrNull { it.brand_name.contains(query, ignoreCase = true) }
    }

    override suspend fun getRecentActiveDose(saltId: Long, thresholdTime: Long): MedicationLogEntity? {
        return logs.firstOrNull { it.resolvedSaltId == saltId && it.status == "TAKEN" && it.intakeTimestamp >= thresholdTime }
    }

    override suspend fun checkContraindications(newSaltId: Long, thresholdTime: Long): ContraindicationResult? {
        val activeRecentLogs = logs.filter { it.status == "TAKEN" && it.intakeTimestamp >= thresholdTime }
        for (log in activeRecentLogs) {
            if ((newSaltId == 7L && log.resolvedSaltId == 8L) || (newSaltId == 8L && log.resolvedSaltId == 7L)) {
                return ContraindicationResult(
                    severity_level = "CRITICAL",
                    clinical_risk_mechanism = "Aspirin + Ibuprofen induces severe gastrointestinal ulceration",
                    spoken_warning_en = "Warning! Taking Aspirin and Combiflam together creates a severe risk of internal stomach bleeding.",
                    spoken_warning_hi = "सावधान! एस्पिरिन और कॉम्बीफ्लेम साथ में लेने से पेट में ब्लीडिंग का खतरा है।",
                    spoken_warning_mr = "सावधान! एस्पिरिन आणि कॉम्बीफ्लेम एकत्र घेतल्यास पोटात अंतर्गत रक्तस्त्रावाचा मोठा धोका आहे."
                )
            }
        }
        return null
    }

    override suspend fun logIntake(log: MedicationLogEntity): Long {
        logs.add(0, log)
        return logs.size.toLong()
    }

    override suspend fun getAllLogs(): List<MedicationLogEntity> = logs

    override suspend fun clearAllLogs() {
        logs.clear()
    }

    override suspend fun getAllMedicines(): List<MedicineQueryResult> = sampleMedicines
}

class SafetyEngineTest {

    @Test
    fun testSafeFirstDoseEvaluation() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val engine = SafetyEvaluationEngine(fakeDao)

        val result = engine.evaluateCandidateTokens(listOf("Glycomet-SR", "500mg"))

        assertTrue("First scan of Glycomet must be safe", result is SafetyEvaluationResult.SafeToTake)
        val safeResult = result as SafetyEvaluationResult.SafeToTake
        assertEquals("Glycomet-SR 500", safeResult.medicine.brand_name)
        assertTrue(safeResult.vernacularInstructionEn.contains("diabetes blood sugar"))
        assertTrue(safeResult.vernacularInstructionHi.contains("शुगर की गोली"))
    }

    @Test
    fun testDuplicateBrandTrapDetection() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val engine = SafetyEvaluationEngine(fakeDao)

        // Step 1: User takes Glycomet-SR 500 (Metformin)
        fakeDao.logIntake(
            MedicationLogEntity(
                medicineId = 1,
                scannedBrandName = "Glycomet-SR 500",
                resolvedSaltId = 1, // Metformin
                intakeTimestamp = System.currentTimeMillis() - 30 * 60 * 1000, // 30 mins ago
                status = "TAKEN"
            )
        )

        // Step 2: User accidentally scans Gluconorm-SR 500 (also Metformin!)
        val result = engine.evaluateCandidateTokens(listOf("Gluconorm-SR"))

        assertTrue("Gluconorm must be blocked due to duplicate Metformin active window", result is SafetyEvaluationResult.DuplicateDoseBlocked)
        val duplicateAlert = result as SafetyEvaluationResult.DuplicateDoseBlocked
        assertEquals("Gluconorm-SR 500", duplicateAlert.medicine.brand_name)
        assertTrue(duplicateAlert.spokenAlertEn.contains("already took Glycomet-SR 500"))
        assertTrue(duplicateAlert.spokenAlertHi.contains("पहले ही ले चुके हैं"))
    }

    @Test
    fun testCriticalDrugContraindication() = runBlocking {
        val fakeDao = FakeMedicineDao()
        val engine = SafetyEvaluationEngine(fakeDao)

        // Step 1: User takes Ecosprin 75 (Aspirin)
        fakeDao.logIntake(
            MedicationLogEntity(
                medicineId = 11,
                scannedBrandName = "Ecosprin 75",
                resolvedSaltId = 8, // Aspirin
                intakeTimestamp = System.currentTimeMillis() - 60 * 60 * 1000,
                status = "TAKEN"
            )
        )

        // Step 2: User scans Combiflam (Ibuprofen)
        val result = engine.evaluateCandidateTokens(listOf("Combiflam"))

        assertTrue("Combiflam + Aspirin must trigger Critical Interaction Blocked", result is SafetyEvaluationResult.CriticalInteractionBlocked)
        val conflictAlert = result as SafetyEvaluationResult.CriticalInteractionBlocked
        assertEquals("CRITICAL", conflictAlert.conflict.severity_level)
        assertTrue(conflictAlert.conflict.spoken_warning_en.contains("internal stomach bleeding"))
    }

    @Test
    fun testFuzzyMatchingResilience() {
        val cleanTarget = "Glycomet-SR 500"
        // Typical OCR distortions under poor lighting
        val noisyOcr1 = "Glycomtt-SR"
        val noisyOcr2 = "G1ycomet-SR 500"
        val wrongBrand = "Paracetamol"

        assertTrue("Noisy OCR 'Glycomtt-SR' should match 'Glycomet-SR 500'", 
            com.medvoice.core.domain.engine.FuzzyMedicineMatcher.isFuzzyMatch(noisyOcr1, cleanTarget, 0.70))

        assertTrue("Noisy OCR 'G1ycomet-SR 500' should match 'Glycomet-SR 500'", 
            com.medvoice.core.domain.engine.FuzzyMedicineMatcher.isFuzzyMatch(noisyOcr2, cleanTarget, 0.75))

        org.junit.Assert.assertFalse("Completely different medicine should not match", 
            com.medvoice.core.domain.engine.FuzzyMedicineMatcher.isFuzzyMatch(wrongBrand, cleanTarget, 0.75))
    }

    @Test
    fun testExpiryDateParsing() {
        val sampleOcrValid = "B.NO 4492A EXP: 12/2028 MFG: 01/2024"
        val sampleOcrExpired = "LOT 992 EXP: 01/2020"

        val parsedValid = com.medvoice.core.domain.engine.ExpiryParser.parse(sampleOcrValid)
        assertEquals("12/2028", parsedValid.expiryDateString)
        assertEquals("4492A", parsedValid.batchNumber)
        org.junit.Assert.assertFalse("Date 12/2028 should not be expired", parsedValid.isExpired)

        val parsedExpired = com.medvoice.core.domain.engine.ExpiryParser.parse(sampleOcrExpired)
        assertEquals("01/2020", parsedExpired.expiryDateString)
        assertTrue("Date 01/2020 should be marked as expired", parsedExpired.isExpired)
    }
}
