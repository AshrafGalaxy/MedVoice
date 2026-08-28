package com.medvoice.core.domain.engine

import com.medvoice.core.ai.FoodTimingRule
import com.medvoice.core.ai.MedGemmaOrchestrator
import com.medvoice.core.ai.MedGemmaSafetyResult
import com.medvoice.core.ai.SafetyVerdict
import com.medvoice.core.data.local.dao.MedicineDao
import com.medvoice.core.data.local.entity.MedicineEntity

sealed class SafetyEvaluationResult {
    data class SafeToTake(
        val safetyResult: MedGemmaSafetyResult,
        val matchedMedicine: MedicineEntity?
    ) : SafetyEvaluationResult() {
        val brandName: String get() = safetyResult.brandName
        val saltName: String get() = safetyResult.parsedSalts.joinToString(", ")
        val instructionText: String get() = safetyResult.spokenVernacularText
        val displayTitle: String get() = safetyResult.displayTitle
        val dosageForm: String get() = safetyResult.dosageForm
    }

    data class DuplicateDoseBlocked(
        val safetyResult: MedGemmaSafetyResult,
        val matchedMedicine: MedicineEntity?
    ) : SafetyEvaluationResult() {
        val brandName: String get() = safetyResult.brandName
        val saltName: String get() = safetyResult.parsedSalts.joinToString(", ")
        val alertMessage: String get() = safetyResult.spokenVernacularText
        val clinicalReason: String get() = safetyResult.clinicalReason
    }

    data class CriticalInteractionBlocked(
        val safetyResult: MedGemmaSafetyResult,
        val matchedMedicine: MedicineEntity?
    ) : SafetyEvaluationResult() {
        val brandName: String get() = safetyResult.brandName
        val saltName: String get() = safetyResult.parsedSalts.joinToString(", ")
        val conflictRisk: String get() = safetyResult.clinicalReason
        val alertMessage: String get() = safetyResult.spokenVernacularText
    }

    data object NoMatchFound : SafetyEvaluationResult()
}

class SafetyEvaluationEngine(
    private val medicineDao: MedicineDao,
    private val medGemmaOrchestrator: MedGemmaOrchestrator = MedGemmaOrchestrator()
) {
    suspend fun evaluateCandidateTokens(
        tokens: List<String>,
        locale: String = "hi"
    ): SafetyEvaluationResult {
        if (tokens.isEmpty()) return SafetyEvaluationResult.NoMatchFound

        var matchedMedicine: MedicineEntity? = null

        // Step 1: Two-Tier Lookup - Try Fast FTS5 Catalog Search (<5ms)
        for (rawToken in tokens) {
            val words = rawToken.split(Regex("[\\s,/\\-]+")).filter { it.length >= 3 }
            val cleanLine = rawToken.replace(Regex("[^a-zA-Z0-9]"), "").trim()
            if (cleanLine.length >= 3) {
                matchedMedicine = medicineDao.searchCatalog(cleanLine)
                    ?: medicineDao.findMedicineByPrefix(cleanLine)
                    ?: medicineDao.findMedicineByFts(cleanLine)
                if (matchedMedicine != null) break
            }

            for (word in words) {
                val cleanWord = word.replace(Regex("[^a-zA-Z0-9]"), "").trim()
                if (cleanWord.length >= 3) {
                    matchedMedicine = medicineDao.searchCatalog(cleanWord)
                        ?: medicineDao.findMedicineByPrefix(cleanWord)
                        ?: medicineDao.findMedicineByFts(cleanWord)
                    if (matchedMedicine != null) break
                }
            }
            if (matchedMedicine != null) break
        }

        // Determine Scanned Chemical Formulation Text
        val scannedFormulation = if (matchedMedicine != null) {
            "${matchedMedicine.brandName} - ${matchedMedicine.rawComposition}"
        } else {
            // Zero-Shot Fallback: Treat raw OCR block directly as chemical formulation
            tokens.joinToString("\n")
        }

        // Query active 24-hour medication logs
        val threshold24h = System.currentTimeMillis() - (24 * 3600 * 1000L)
        val recentLogs = medicineDao.getRecentLogs(threshold24h)

        // Step 2: MedGemma Clinical Reasoning Engine
        val safetyResult = medGemmaOrchestrator.evaluateSafety(
            scannedText = scannedFormulation,
            matchedMedicine = matchedMedicine,
            recentLogs = recentLogs,
            locale = locale
        )

        return when (safetyResult.safetyVerdict) {
            SafetyVerdict.SAFE_TO_TAKE -> SafetyEvaluationResult.SafeToTake(
                safetyResult = safetyResult,
                matchedMedicine = matchedMedicine
            )
            SafetyVerdict.DUPLICATE_OVERDOSE_BLOCKED -> SafetyEvaluationResult.DuplicateDoseBlocked(
                safetyResult = safetyResult,
                matchedMedicine = matchedMedicine
            )
            SafetyVerdict.CRITICAL_INTERACTION_BLOCKED -> SafetyEvaluationResult.CriticalInteractionBlocked(
                safetyResult = safetyResult,
                matchedMedicine = matchedMedicine
            )
        }
    }
}
