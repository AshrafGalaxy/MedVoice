package com.medvoice.core.domain.engine

import com.medvoice.core.ai.MedGemmaOrchestrator
import com.medvoice.core.ai.MedGemmaSafetyResult
import com.medvoice.core.ai.SafetyVerdict
import com.medvoice.core.data.local.dao.MedicineDao
import com.medvoice.core.data.local.entity.MedicineEntity
import java.util.Locale

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

    data class ExpiredMedicineBlocked(
        val safetyResult: MedGemmaSafetyResult,
        val matchedMedicine: MedicineEntity?,
        val expiryDateString: String?
    ) : SafetyEvaluationResult() {
        val brandName: String get() = safetyResult.brandName
        val alertMessage: String get() = safetyResult.spokenVernacularText
        val clinicalReason: String get() = safetyResult.clinicalReason
    }

    data class UnidentifiedMedicineBlocked(
        val safetyResult: MedGemmaSafetyResult
    ) : SafetyEvaluationResult() {
        val alertMessage: String get() = safetyResult.spokenVernacularText
        val clinicalReason: String get() = safetyResult.clinicalReason
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

        val combinedText = tokens.joinToString(" ")

        // Step 1: Scan for Expiry Date across all tokens
        val expiryInfo = ExpiryParser.parse(combinedText)

        // Strict Pre-Filter Gate: Must contain pharmaceutical strength, dosage form, pharmacopeia standards, or active chemical
        val hasPharmaMarkers = medGemmaOrchestrator.aiPharmacologyEngine.containsPharmaceuticalMarkers(combinedText)
        if (!hasPharmaMarkers) {
            return SafetyEvaluationResult.NoMatchFound
        }

        var matchedMedicine: MedicineEntity? = null

        // Step 2: Strict Catalog Lookup on clean non-noise lines
        for (rawToken in tokens) {
            val cleanLine = rawToken.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
            if (cleanLine.length in 4..40) {
                matchedMedicine = medicineDao.searchCatalog(cleanLine)
                    ?: medicineDao.findMedicineByFts(cleanLine)
                if (matchedMedicine != null) break
            }
        }

        // Query active 24-hour medication logs for interaction matrix
        val threshold24h = System.currentTimeMillis() - (24 * 3600 * 1000L)
        val recentLogs = medicineDao.getRecentLogs(threshold24h)

        // Step 3: Deep MedGemma Clinical Reasoning (Preserves authentic packaging text)
        val safetyResult = medGemmaOrchestrator.evaluateSafety(
            scannedText = combinedText,
            matchedMedicine = matchedMedicine,
            recentLogs = recentLogs,
            locale = locale,
            expiryDate = expiryInfo.expiryDateString,
            isExpired = expiryInfo.isExpired
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
            SafetyVerdict.EXPIRED_MEDICINE_BLOCKED -> SafetyEvaluationResult.ExpiredMedicineBlocked(
                safetyResult = safetyResult,
                matchedMedicine = matchedMedicine,
                expiryDateString = expiryInfo.expiryDateString
            )
            SafetyVerdict.UNIDENTIFIED_MEDICINE_BLOCKED -> SafetyEvaluationResult.UnidentifiedMedicineBlocked(
                safetyResult = safetyResult
            )
        }
    }
}
