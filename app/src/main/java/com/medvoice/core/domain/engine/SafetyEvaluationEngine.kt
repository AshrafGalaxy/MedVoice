package com.medvoice.core.domain.engine

import com.medvoice.core.ai.ClinicalSafetyOrchestrator
import com.medvoice.core.ai.ClinicalSafetyResult
import com.medvoice.core.ai.FoodTimingRule
import com.medvoice.core.ai.SafetyVerdict
import com.medvoice.core.data.local.dao.MedicineDao
import com.medvoice.core.data.local.entity.MedicineEntity
import java.util.Locale

sealed class SafetyEvaluationResult {
    data class SafeToTake(
        val safetyResult: ClinicalSafetyResult,
        val matchedMedicine: MedicineEntity?
    ) : SafetyEvaluationResult() {
        val brandName: String get() = safetyResult.brandName
        val saltName: String get() = safetyResult.parsedSalts.joinToString(", ")
        val instructionText: String get() = safetyResult.spokenVernacularText
        val displayTitle: String get() = safetyResult.displayTitle
        val dosageForm: String get() = safetyResult.dosageForm
        val sourceTier: com.medvoice.core.ai.AiEngineTier get() = safetyResult.sourceTier
    }

    data class DuplicateDoseBlocked(
        val safetyResult: ClinicalSafetyResult,
        val matchedMedicine: MedicineEntity?
    ) : SafetyEvaluationResult() {
        val brandName: String get() = safetyResult.brandName
        val saltName: String get() = safetyResult.parsedSalts.joinToString(", ")
        val alertMessage: String get() = safetyResult.spokenVernacularText
        val clinicalReason: String get() = safetyResult.clinicalReason
    }

    data class CriticalInteractionBlocked(
        val safetyResult: ClinicalSafetyResult,
        val matchedMedicine: MedicineEntity?
    ) : SafetyEvaluationResult() {
        val brandName: String get() = safetyResult.brandName
        val saltName: String get() = safetyResult.parsedSalts.joinToString(", ")
        val conflictRisk: String get() = safetyResult.clinicalReason
        val alertMessage: String get() = safetyResult.spokenVernacularText
    }

    data class ExpiredMedicineBlocked(
        val safetyResult: ClinicalSafetyResult,
        val matchedMedicine: MedicineEntity?,
        val expiryDateString: String?
    ) : SafetyEvaluationResult() {
        val brandName: String get() = safetyResult.brandName
        val alertMessage: String get() = safetyResult.spokenVernacularText
        val clinicalReason: String get() = safetyResult.clinicalReason
    }

    data class UnidentifiedMedicineBlocked(
        val safetyResult: ClinicalSafetyResult
    ) : SafetyEvaluationResult() {
        val alertMessage: String get() = safetyResult.spokenVernacularText
        val clinicalReason: String get() = safetyResult.clinicalReason
    }

    data object NoMatchFound : SafetyEvaluationResult()
}

class SafetyEvaluationEngine(
    private val medicineDao: MedicineDao,
    private val clinicalOrchestrator: ClinicalSafetyOrchestrator = ClinicalSafetyOrchestrator()
) {
    suspend fun evaluateCandidateTokens(
        tokens: List<String>,
        locale: String = "hi",
        isExplicitSnap: Boolean = false,
        bitmap: android.graphics.Bitmap? = null
    ): SafetyEvaluationResult {
        if (tokens.isEmpty()) {
            return if (isExplicitSnap) {
                SafetyEvaluationResult.UnidentifiedMedicineBlocked(
                    ClinicalSafetyResult(
                        brandName = if (locale == "hi") "लिखावट नहीं मिली" else "No Text Found",
                        parsedSalts = emptyList(),
                        therapeuticClass = "UNIDENTIFIED",
                        safetyVerdict = SafetyVerdict.UNIDENTIFIED_MEDICINE_BLOCKED,
                        clinicalReason = if (locale == "hi") "कृपया दवा की पट्टी को रोशनी में कैमरे के पास लाएं और दोबारा फोटो लें।" else "No clear text found on packaging. Please snap closer under good lighting.",
                        foodTimingRule = FoodTimingRule.AFTER_FOOD,
                        isEmergencyAlert = false,
                        spokenVernacularText = if (locale == "hi") "दवा पर कोई लिखावट नहीं दिखी। कृपया पट्टी को रोशनी में दोबारा स्कैन करें।" else "No legible text detected on packaging. Please scan the label clearly again.",
                        displayTitle = if (locale == "hi") "पहचान में असमर्थ (Unidentified)" else "Unidentified Medicine",
                        dosageForm = "UNKNOWN"
                    )
                )
            } else {
                SafetyEvaluationResult.NoMatchFound
            }
        }

        val combinedText = tokens.joinToString(" ")

        // Step 1: Scan for Expiry Date across all tokens
        val expiryInfo = ExpiryParser.parse(combinedText)

        // Strict Pre-Filter Gate: Must contain pharmaceutical strength, dosage form, pharmacopeia standards, or active chemical
        val hasPharmaMarkers = clinicalOrchestrator.aiPharmacologyEngine.containsPharmaceuticalMarkers(combinedText)
        if (!hasPharmaMarkers && !isExplicitSnap) {
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

        // Step 3: Deep Clinical Safety Reasoning (Preserves authentic packaging text)
        val safetyResult = clinicalOrchestrator.evaluateSafety(
            scannedText = combinedText,
            matchedMedicine = matchedMedicine,
            recentLogs = recentLogs,
            locale = locale,
            expiryDate = expiryInfo.expiryDateString,
            isExpired = expiryInfo.isExpired,
            bitmap = bitmap
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
