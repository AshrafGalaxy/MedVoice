package com.medvoice.core.domain.engine

import com.medvoice.core.ai.AiPharmacologyEngine
import com.medvoice.core.data.local.dao.ContraindicationResult
import com.medvoice.core.data.local.dao.MedicineDao
import com.medvoice.core.data.local.dao.MedicineQueryResult
import com.medvoice.core.data.local.entity.MedicationLogEntity
import kotlin.math.absoluteValue

sealed class SafetyEvaluationResult {
    data class SafeToTake(
        val medicine: MedicineQueryResult,
        val vernacularInstructionEn: String,
        val vernacularInstructionHi: String,
        val vernacularInstructionMr: String
    ) : SafetyEvaluationResult()

    data class DuplicateDoseBlocked(
        val medicine: MedicineQueryResult,
        val recentLog: MedicationLogEntity,
        val spokenAlertEn: String,
        val spokenAlertHi: String,
        val spokenAlertMr: String
    ) : SafetyEvaluationResult()

    data class CriticalInteractionBlocked(
        val medicine: MedicineQueryResult,
        val conflict: ContraindicationResult
    ) : SafetyEvaluationResult()

    data object NoMatchFound : SafetyEvaluationResult()
}

class SafetyEvaluationEngine(
    private val medicineDao: MedicineDao,
    private val aiPharmacologyEngine: AiPharmacologyEngine = AiPharmacologyEngine()
) {
    suspend fun evaluateCandidateTokens(tokens: List<String>): SafetyEvaluationResult {
        if (tokens.isEmpty()) return SafetyEvaluationResult.NoMatchFound

        var matchedMedicine: MedicineQueryResult? = null

        // Step 1: Fast Lookup in Local SQLite Pharmacopeia DB (<5ms)
        for (rawToken in tokens) {
            val words = rawToken.split(Regex("[\\s,/\\-]+")).filter { it.length >= 3 }
            val cleanLine = rawToken.replace(Regex("[^a-zA-Z0-9]"), "").trim()
            if (cleanLine.length >= 3) {
                matchedMedicine = medicineDao.findMedicineByPrefix(cleanLine)
                    ?: medicineDao.findMedicineByFts(cleanLine)
                if (matchedMedicine != null) break
            }

            for (word in words) {
                val cleanWord = word.replace(Regex("[^a-zA-Z0-9]"), "").trim()
                if (cleanWord.length >= 3) {
                    matchedMedicine = medicineDao.findMedicineByPrefix(cleanWord)
                        ?: medicineDao.findMedicineByFts(cleanWord)
                    if (matchedMedicine != null) break
                }
            }
            if (matchedMedicine != null) break
        }

        // Step 2: Graceful Universal Fallback - Invoke AI Pharmacology Engine for Unlisted Real Medicines
        if (matchedMedicine == null) {
            val combinedOcr = tokens.joinToString("\n")
            val composition = aiPharmacologyEngine.parsePrescriptionText(combinedOcr)
            if (composition != null && composition.brandName.isNotBlank() && composition.brandName != "Scanned Medicine") {
                val dynamicSaltId = composition.activeSalts.firstOrNull()?.uppercase()?.hashCode()?.toLong()?.absoluteValue?.coerceAtLeast(1000L) ?: 9999L
                val dynamicMedId = composition.brandName.uppercase().hashCode().toLong().absoluteValue.coerceAtLeast(1000L)

                matchedMedicine = MedicineQueryResult(
                    id = dynamicMedId,
                    brand_name = composition.brandName,
                    dosage_form = composition.dosageForm,
                    strength_mg = composition.strengthMg,
                    primary_salt_id = dynamicSaltId,
                    is_high_risk = false,
                    vernacular_usage_en = composition.therapeuticCategory,
                    vernacular_usage_hi = composition.therapeuticCategory,
                    vernacular_usage_mr = composition.therapeuticCategory,
                    salt_name = composition.activeSalts.joinToString(", "),
                    therapeutic_class = composition.therapeuticCategory,
                    max_daily_dose_mg = 2000.0,
                    active_window_hours = 8.0,
                    vernacular_salt_desc_en = composition.therapeuticCategory,
                    vernacular_salt_desc_hi = composition.therapeuticCategory,
                    vernacular_salt_desc_mr = composition.therapeuticCategory,
                    rule_code = composition.dosageForm,
                    food_relation = "AS_DIRECTED",
                    vernacular_instruction_en = composition.vernacularInstructionEn,
                    vernacular_instruction_hi = composition.vernacularInstructionHi,
                    vernacular_instruction_mr = composition.vernacularInstructionMr
                )
            }
        }

        if (matchedMedicine == null) {
            return SafetyEvaluationResult.NoMatchFound
        }

        val currentTime = System.currentTimeMillis()
        val activeWindowMillis = (matchedMedicine.active_window_hours * 3600 * 1000).toLong()
        val threshold = currentTime - activeWindowMillis

        // Step 3: Check for Duplicate Active Molecule within therapeutic window
        val recentDose = medicineDao.getRecentActiveDose(matchedMedicine.primary_salt_id, threshold)
        if (recentDose != null) {
            val saltName = matchedMedicine.salt_name
            val previousBrand = recentDose.scannedBrandName
            val spokenAlertEn = "Warning! Stop! You already took $previousBrand ($saltName). Do not take this medicine again."
            val spokenAlertHi = "सावधान! रुकिए! आप $previousBrand ($saltName) पहले ही ले चुके हैं। इसे दोबारा न लें।"
            val spokenAlertMr = "सावधान! थांबा! तुम्ही आधीच $previousBrand ($saltName) घेतले आहे. हे औषध पुन्हा घेऊ नका."

            return SafetyEvaluationResult.DuplicateDoseBlocked(
                medicine = matchedMedicine,
                recentLog = recentDose,
                spokenAlertEn = spokenAlertEn,
                spokenAlertHi = spokenAlertHi,
                spokenAlertMr = spokenAlertMr
            )
        }

        // Step 4: Check Drug-to-Drug Interaction Matrix
        val contraindication = medicineDao.checkContraindications(matchedMedicine.primary_salt_id, threshold)
        if (contraindication != null) {
            return SafetyEvaluationResult.CriticalInteractionBlocked(
                medicine = matchedMedicine,
                conflict = contraindication
            )
        }

        // Step 5: Formulate Validated Safe Guidance
        val instructionEn = listOf(matchedMedicine.vernacular_usage_en, matchedMedicine.vernacular_instruction_en)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val instructionHi = listOf(matchedMedicine.vernacular_usage_hi, matchedMedicine.vernacular_instruction_hi)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val instructionMr = listOf(matchedMedicine.vernacular_usage_mr, matchedMedicine.vernacular_instruction_mr)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        return SafetyEvaluationResult.SafeToTake(
            medicine = matchedMedicine,
            vernacularInstructionEn = instructionEn,
            vernacularInstructionHi = instructionHi,
            vernacularInstructionMr = instructionMr
        )
    }
}
