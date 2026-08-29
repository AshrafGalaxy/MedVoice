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
    companion object {
        private val GENERIC_OR_COSMETIC_WORDS = setOf(
            "AQUA", "WATER", "GLYCERIN", "ZINC", "COPPER", "MAGNESIUM", "SODIUM", "POTASSIUM",
            "ACID", "ALCOHOL", "EXTRACT", "OIL", "CREAM", "LOTION", "MOISTURIZER", "MOISTURISER",
            "WASH", "FACEWASH", "FACE", "BODY", "CLEANSER", "SERUM", "SUNSCREEN", "SUN", "GEL",
            "SHAMPOO", "BOTTLE", "PACK", "NET", "BATCH", "MFG", "DATE", "EXP", "LTD", "PVT",
            "PHARMACEUTICALS", "LABORATORIES", "HEALTHCARE", "MINIMALIST", "DERMA", "CARE", "DAILY"
        )
    }

    suspend fun evaluateCandidateTokens(
        tokens: List<String>,
        locale: String = "hi",
        isExplicitSnap: Boolean = false,
        bitmap: android.graphics.Bitmap? = null,
        patientConditions: Set<String> = emptySet()
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

        // Step 1: Cosmetic / Skincare Non-Pharmaceutical Guard
        val isCosmeticOrSkincare = Regex(
            """\b(?:moisturizer|moisturiser|face\s*wash|body\s*wash|cleanser|sunscreen|body\s*lotion|serum|facewash|skin\s*cleanser)\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(combinedText) && !Regex(
            """\b(?:schedule\s+[hghx]|i\.?p\.?|b\.?p\.?|u\.?s\.?p\.?|rx\s+only|oral\s+drops|injection|tablets?|capsules?|syrup)\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(combinedText)

        if (isCosmeticOrSkincare) {
            val spokenText = if (locale == "hi") {
                "यह कोई दवा नहीं है। यह स्किनकेयर या कॉस्मेटिक उत्पाद है। इसका सेवन न करें।"
            } else {
                "This item is a cosmetic skincare product, not a pharmaceutical medication. Do not ingest."
            }
            val displayTitle = if (locale == "hi") {
                "कॉस्मेटिक उत्पाद (Skincare / Cosmetic)"
            } else {
                "Cosmetic Skincare Product"
            }
            return SafetyEvaluationResult.UnidentifiedMedicineBlocked(
                ClinicalSafetyResult(
                    brandName = if (locale == "hi") "कॉस्मेटिक / स्किनकेयर उत्पाद" else "Cosmetic Skincare Product",
                    parsedSalts = emptyList(),
                    therapeuticClass = "COSMETIC_NON_DRUG",
                    safetyVerdict = SafetyVerdict.UNIDENTIFIED_MEDICINE_BLOCKED,
                    clinicalReason = if (locale == "hi") "स्कैन किया गया उत्पाद स्किनकेयर/कॉस्मेटिक है (जैसे मॉइस्चराइज़र या फेसवॉश)। यह डॉक्टर द्वारा निर्धारित दवा नहीं है।" else "The scanned item is a cosmetic skincare product (e.g. moisturizer/cleanser), not a regulated prescription drug.",
                    foodTimingRule = FoodTimingRule.AFTER_FOOD,
                    isEmergencyAlert = false,
                    spokenVernacularText = spokenText,
                    displayTitle = displayTitle,
                    dosageForm = "COSMETIC"
                )
            )
        }

        // Step 2: Scan for Expiry Date across all tokens
        val expiryInfo = ExpiryParser.parse(combinedText)

        // Strict Pre-Filter Gate: Must contain pharmaceutical strength, dosage form, pharmacopeia standards, or active chemical
        val hasPharmaMarkers = clinicalOrchestrator.aiPharmacologyEngine.containsPharmaceuticalMarkers(combinedText)
        if (!hasPharmaMarkers && !isExplicitSnap) {
            return SafetyEvaluationResult.NoMatchFound
        }

        var matchedMedicine: MedicineEntity? = null
        val upperCombined = combinedText.uppercase(Locale.ROOT)

        // Step 3: Strict Multi-Token Catalog Lookup with Two-Way Verification
        for (rawToken in tokens.take(8)) {
            val cleanLine = rawToken.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim().replace(Regex("\\s+"), " ")
            val words = cleanLine.split(" ").filter { it.isNotBlank() }

            // Skip isolated single generic words (like "Magnesium", "Zinc", "Aqua")
            if (words.size == 1 && (cleanLine.length < 5 || GENERIC_OR_COSMETIC_WORDS.contains(cleanLine.uppercase(Locale.ROOT)))) {
                continue
            }

            if (cleanLine.length in 4..40) {
                val candidate = medicineDao.searchCatalog(cleanLine)
                    ?: medicineDao.findMedicineByFts(cleanLine)

                if (candidate != null) {
                    // Two-Way Safety Verification:
                    // Check if candidate's brandName or primary active salt actually appears in the scanned text!
                    val candBrand = candidate.brandName.uppercase(Locale.ROOT).split(" ").firstOrNull() ?: ""
                    val candSalt = candidate.rawComposition.uppercase(Locale.ROOT).split(" ").take(2).joinToString(" ")

                    val isBrandVerified = candBrand.length >= 3 && upperCombined.contains(candBrand)
                    val isSaltVerified = candSalt.length >= 5 && upperCombined.contains(candSalt)

                    if (isBrandVerified || isSaltVerified) {
                        matchedMedicine = candidate
                        break
                    }
                }
            }
        }

        // Query active 24-hour medication logs for interaction matrix
        val threshold24h = System.currentTimeMillis() - (24 * 3600 * 1000L)
        val recentLogs = medicineDao.getRecentLogs(threshold24h)

        // Step 4: Deep Clinical Safety Reasoning (Preserves authentic packaging text)
        val safetyResult = clinicalOrchestrator.evaluateSafety(
            scannedText = combinedText,
            matchedMedicine = matchedMedicine,
            recentLogs = recentLogs,
            locale = locale,
            expiryDate = expiryInfo.expiryDateString,
            isExpired = expiryInfo.isExpired,
            bitmap = bitmap,
            patientConditions = patientConditions
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
