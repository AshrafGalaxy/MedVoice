package com.medvoice.core.domain.engine

import com.medvoice.core.data.local.dao.ContraindicationResult
import com.medvoice.core.data.local.dao.MedicineDao
import com.medvoice.core.data.local.dao.MedicineQueryResult
import com.medvoice.core.data.local.entity.MedicationLogEntity

sealed class SafetyEvaluationResult {
    data class SafeToTake(
        val medicine: MedicineQueryResult,
        val vernacularInstructionHi: String,
        val vernacularInstructionMr: String
    ) : SafetyEvaluationResult()

    data class DuplicateDoseBlocked(
        val medicine: MedicineQueryResult,
        val recentLog: MedicationLogEntity,
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
    private val medicineDao: MedicineDao
) {
    suspend fun evaluateCandidateTokens(tokens: List<String>): SafetyEvaluationResult {
        var matchedMedicine: MedicineQueryResult? = null

        // Step 1: Normalize & search tokens against Master Pharmacopeia DB
        for (rawToken in tokens) {
            val words = rawToken.split(Regex("[\\s,/\\-]+")).filter { it.length >= 3 }
            // Try full line token
            val cleanLine = rawToken.replace(Regex("[^a-zA-Z0-9]"), "").trim()
            if (cleanLine.length >= 3) {
                matchedMedicine = medicineDao.findMedicineByPrefix(cleanLine)
                    ?: medicineDao.findMedicineByFts(cleanLine)
                if (matchedMedicine != null) break
            }

            // Try individual significant words
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

        if (matchedMedicine == null) {
            return SafetyEvaluationResult.NoMatchFound
        }

        val currentTime = System.currentTimeMillis()
        val activeWindowMillis = (matchedMedicine.active_window_hours * 3600 * 1000).toLong()
        val threshold = currentTime - activeWindowMillis

        // Step 2: Check for Duplicate Active Molecule within therapeutic window
        val recentDose = medicineDao.getRecentActiveDose(matchedMedicine.primary_salt_id, threshold)
        if (recentDose != null) {
            val saltName = matchedMedicine.salt_name
            val previousBrand = recentDose.scannedBrandName
            val spokenAlertHi = "सावधान! रुकिए! आप $previousBrand ($saltName) पहले ही ले चुके हैं। इसे दोबारा न लें।"
            val spokenAlertMr = "सावधान! थांबा! तुम्ही आधीच $previousBrand ($saltName) घेतले आहे. हे औषध पुन्हा घेऊ नका."

            return SafetyEvaluationResult.DuplicateDoseBlocked(
                medicine = matchedMedicine,
                recentLog = recentDose,
                spokenAlertHi = spokenAlertHi,
                spokenAlertMr = spokenAlertMr
            )
        }

        // Step 3: Check Drug-to-Drug Interaction Matrix
        val contraindication = medicineDao.checkContraindications(matchedMedicine.primary_salt_id, threshold)
        if (contraindication != null) {
            return SafetyEvaluationResult.CriticalInteractionBlocked(
                medicine = matchedMedicine,
                conflict = contraindication
            )
        }

        // Step 4: Formulate Validated Safe Guidance
        val instructionHi = "${matchedMedicine.vernacular_usage_hi} ${matchedMedicine.vernacular_instruction_hi}".trim()
        val instructionMr = "${matchedMedicine.vernacular_usage_mr} ${matchedMedicine.vernacular_instruction_mr}".trim()

        return SafetyEvaluationResult.SafeToTake(
            medicine = matchedMedicine,
            vernacularInstructionHi = instructionHi,
            vernacularInstructionMr = instructionMr
        )
    }
}
