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

        var matchedMedicine: MedicineEntity? = null

        // Step 2: Two-Tier Lookup - Try Fast SQLite Catalog Search (<5ms)
        val strengthRegex = Regex("^\\d+\\s*(?:mg|mcg|gm|g|ml|l|%|tab|cap|tabs|caps)?$", RegexOption.IGNORE_CASE)
        val commonCounterIons = setOf(
            "SODIUM", "HYDROCHLORIDE", "HCL", "POTASSIUM", "CALCIUM", "SUCCINATE",
            "CITRATE", "MALEATE", "SULPHATE", "SULFATE", "PHOSPHATE", "ACETATE",
            "NITRATE", "HYDRATE", "MESYLATE", "TARTRATE", "FUMARATE", "CHLORIDE",
            "TABLET", "TABLETS", "CAPSULE", "CAPSULES", "SYRUP", "DROPS", "SOLUTION",
            "SUSPENSION", "CREAM", "OINTMENT", "GEL", "EMULGEL", "INJECTION", "INHALER",
            "LIMITED", "LTD", "PVT", "PHARMA", "LABORATORIES", "INDIA"
        )

        // 1. Try full line exact FTS matches first to avoid false positives
        for (rawToken in tokens) {
            val cleanLine = rawToken.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
            if (cleanLine.length >= 5) {
                matchedMedicine = medicineDao.searchCatalog(cleanLine)
                    ?: medicineDao.findMedicineByFts(cleanLine)
                if (matchedMedicine != null) break
            }
        }

        // 2. Strict phrase and token matching if full line fails
        if (matchedMedicine == null) {
            for (rawToken in tokens) {
                val words = rawToken.split(Regex("[\\s,/\\-]+")).map { 
                    it.replace(Regex("[^a-zA-Z0-9]"), "").trim() 
                }.filter { 
                    it.length >= 5 && // Strict minimum 5 characters to avoid wild 3-letter guesses
                    !it.matches(strengthRegex) && 
                    !it.all { char -> char.isDigit() } &&
                    !commonCounterIons.contains(it.uppercase(Locale.ROOT))
                }

                // Try two-word phrases first (e.g., "Pan 40" but digits are filtered, so "Cadila Glycomet")
                for (i in 0 until words.size - 1) {
                    val phrase = "${words[i]} ${words[i+1]}"
                    if (phrase.length >= 6) {
                        matchedMedicine = medicineDao.findMedicineByFts(phrase)
                        if (matchedMedicine != null) break
                    }
                }
                if (matchedMedicine != null) break

                // Try individual long tokens
                for (word in words) {
                    if (word.length >= 5) {
                        matchedMedicine = medicineDao.searchCatalog(word)
                            ?: medicineDao.findMedicineByFts(word)
                        if (matchedMedicine != null) break
                    }
                }
                if (matchedMedicine != null) break
            }
        }

        // Determine Scanned Chemical Formulation Text
        val scannedFormulation = if (matchedMedicine != null) {
            "${matchedMedicine.brandName} - ${matchedMedicine.rawComposition}"
        } else {
            tokens.joinToString("\n")
        }

        // Query active 24-hour medication logs
        val threshold24h = System.currentTimeMillis() - (24 * 3600 * 1000L)
        val recentLogs = medicineDao.getRecentLogs(threshold24h)

        // Step 3: MedGemma Clinical Reasoning Engine with Expiry Guard
        val safetyResult = medGemmaOrchestrator.evaluateSafety(
            scannedText = scannedFormulation,
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
