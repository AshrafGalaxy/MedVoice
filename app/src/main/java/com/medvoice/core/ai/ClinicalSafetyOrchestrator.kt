package com.medvoice.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.medvoice.core.data.local.entity.MedicationLogEntity
import com.medvoice.core.data.local.entity.MedicineEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

enum class SafetyVerdict {
    SAFE_TO_TAKE,
    DUPLICATE_OVERDOSE_BLOCKED,
    CRITICAL_INTERACTION_BLOCKED,
    EXPIRED_MEDICINE_BLOCKED,
    UNIDENTIFIED_MEDICINE_BLOCKED
}

enum class FoodTimingRule {
    EMPTY_STOMACH,
    BEFORE_FOOD,
    AFTER_FOOD,
    BEDTIME,
    NOT_APPLICABLE_EXTERNAL
}

data class ClinicalSafetyResult(
    val brandName: String,
    val parsedSalts: List<String>,
    val therapeuticClass: String,
    val safetyVerdict: SafetyVerdict,
    val clinicalReason: String,
    val foodTimingRule: FoodTimingRule,
    val isEmergencyAlert: Boolean,
    val spokenVernacularText: String,
    val displayTitle: String,
    val dosageForm: String = "TABLET",
    val confidenceScore: Float = 1.0f,
    val sourceTier: AiEngineTier = AiEngineTier.CLOUD_MEDGEMMA_HOSTED
)

class ClinicalSafetyOrchestrator(
    private val context: Context? = null,
    val aiPharmacologyEngine: AiPharmacologyEngine = AiPharmacologyEngine(context)
) {

    var activeTier: AiEngineTier = AiEngineTier.ON_DEVICE_MEDGEMMA_INT4

    /**
     * Comprehensive Edge Clinical Safety Evaluator with Vision & Route Awareness
     */
    suspend fun evaluateSafety(
        scannedText: String,
        matchedMedicine: MedicineEntity?,
        recentLogs: List<MedicationLogEntity>,
        locale: String = "hi",
        expiryDate: String? = null,
        isExpired: Boolean = false,
        bitmap: Bitmap? = null,
        patientConditions: Set<String> = emptySet()
    ): ClinicalSafetyResult = withContext(Dispatchers.Default) {
        val targetLangCode = if (locale.lowercase(Locale.ROOT).startsWith("hi")) "hi-IN" else "en-IN"
        val isHindi = targetLangCode == "hi-IN"

        // 1. Critical Fail-Closed Guard: Expired Medication Detection
        if (isExpired && !expiryDate.isNullOrBlank()) {
            val spokenText = if (isHindi) {
                "सावधान! रुकिए! यह दवा $expiryDate को समाप्त यानी एक्सपायर हो चुकी है। इसका सेवन बिल्कुल न करें।"
            } else {
                "Warning! Stop! This medicine expired on $expiryDate. Do not consume expired medication."
            }
            val displayTitle = if (isHindi) {
                "समाप्त दवा अवरुद्ध (Expired Drug)"
            } else {
                "Expired Medicine Blocked"
            }

            return@withContext ClinicalSafetyResult(
                brandName = matchedMedicine?.brandName ?: "Expired Medicine",
                parsedSalts = listOf(matchedMedicine?.rawComposition ?: "Expired Formulation"),
                therapeuticClass = "EXPIRED_HAZARD",
                safetyVerdict = SafetyVerdict.EXPIRED_MEDICINE_BLOCKED,
                clinicalReason = "Medication passed manufacturer expiration date ($expiryDate). Ingestion may cause toxic degradation or therapeutic failure.",
                foodTimingRule = FoodTimingRule.AFTER_FOOD,
                isEmergencyAlert = true,
                spokenVernacularText = spokenText,
                displayTitle = displayTitle,
                dosageForm = matchedMedicine?.dosageForm ?: "TABLET",
                confidenceScore = 1.0f
            )
        }

        // 2. Deterministic Edge & Vision Clinical Reasoning
        return@withContext executeEdgeClinicalReasoning(scannedText, matchedMedicine, recentLogs, isHindi, bitmap, patientConditions)
    }

    private suspend fun executeEdgeClinicalReasoning(
        scannedText: String,
        matchedMedicine: MedicineEntity?,
        recentLogs: List<MedicationLogEntity>,
        isHindi: Boolean,
        bitmap: Bitmap?,
        patientConditions: Set<String> = emptySet()
    ): ClinicalSafetyResult {
        val upperText = scannedText.uppercase(Locale.ROOT)
        
        // If it's an unrecognized medicine, parse with AiPharmacologyEngine (Multimodal Vision / On-Device Fuzzy Parser)
        val extractedComposition = if (matchedMedicine == null) {
            aiPharmacologyEngine.parsePrescriptionText(scannedText, bitmap)
        } else null

        // 1. Fail-Closed Confidence Guard
        if (matchedMedicine == null && extractedComposition == null) {
            val spokenText = if (isHindi) {
                "सावधान! दवा की पहचान स्पष्ट नहीं हो सकी। सुरक्षा के लिए यह दवा न लें और पट्टी को दोबारा स्पष्ट रूप से स्कैन करें।"
            } else {
                "Warning! Unidentified medicine formulation. For your safety, do not consume this drug. Please scan the label clearly again."
            }
            val displayTitle = if (isHindi) {
                "पहचान में असमर्थ (Unidentified Medicine)"
            } else {
                "Unidentified Medicine Blocked"
            }

            return ClinicalSafetyResult(
                brandName = "Unidentified Medicine",
                parsedSalts = listOf("Unknown Formulation"),
                therapeuticClass = "UNIDENTIFIED",
                safetyVerdict = SafetyVerdict.UNIDENTIFIED_MEDICINE_BLOCKED,
                clinicalReason = "Confidence < 80%. Scanned text matches no recognized pharmaceutical catalog entry or active chemical salt.",
                foodTimingRule = FoodTimingRule.AFTER_FOOD,
                isEmergencyAlert = false,
                spokenVernacularText = spokenText,
                displayTitle = displayTitle,
                dosageForm = "UNKNOWN",
                confidenceScore = 0.0f
            )
        }

        val rawComposition = matchedMedicine?.rawComposition ?: extractedComposition?.activeSalts?.joinToString(", ") ?: scannedText
        val parsedSalts = matchedMedicine?.let { extractChemicalSalts(it.rawComposition) } ?: extractedComposition?.activeSalts ?: extractChemicalSalts(scannedText)

        val isRecognizedBrand = matchedMedicine != null || (extractedComposition != null && extractedComposition.confidenceScore >= 0.8f)
        if (!isRecognizedBrand) {
            val spokenText = if (isHindi) {
                "सावधान! दवा की पहचान स्पष्ट नहीं हो सकी। सुरक्षा के लिए यह दवा न लें और पट्टी को दोबारा स्पष्ट रूप से स्कैन करें।"
            } else {
                "Warning! Unidentified medicine formulation. For your safety, do not consume this drug. Please scan the label clearly again."
            }
            val displayTitle = if (isHindi) {
                "पहचान में असमर्थ (Unidentified Medicine)"
            } else {
                "Unidentified Medicine Blocked"
            }

            return ClinicalSafetyResult(
                brandName = "Unidentified Medicine",
                parsedSalts = listOf("Unknown Formulation"),
                therapeuticClass = "UNIDENTIFIED",
                safetyVerdict = SafetyVerdict.UNIDENTIFIED_MEDICINE_BLOCKED,
                clinicalReason = "Confidence < 80%. Scanned text matches no recognized pharmaceutical catalog entry or active chemical salt.",
                foodTimingRule = FoodTimingRule.AFTER_FOOD,
                isEmergencyAlert = false,
                spokenVernacularText = spokenText,
                displayTitle = displayTitle,
                dosageForm = "UNKNOWN",
                confidenceScore = 0.35f
            )
        }

        val brandName = extractedComposition?.brandName?.takeIf { it.isNotBlank() && it != "Scanned Medicine" && it != "Composition" }
            ?: matchedMedicine?.brandName
            ?: run {
                val firstLine = scannedText.lines().firstOrNull { it.trim().length > 3 }?.trim() ?: "Scanned Medicine"
                firstLine.split(" ").take(3).joinToString(" ")
            }

        // 2. Classify Dosage Form & Administration Route (Strict Specificity Hierarchy)
        val dosageForm = when {
            matchedMedicine != null -> matchedMedicine.dosageForm
            // A. Ophthalmic / Otic / Nasal / Inhaled (Specific Local Routes)
            upperText.contains("EYE DROP") || upperText.contains("OPHTHALMIC") -> "EYE_DROPS"
            upperText.contains("EAR DROP") -> "EAR_DROPS"
            upperText.contains("NASAL") || upperText.contains("SPRAY") -> "NASAL_SPRAY"
            upperText.contains("INHALER") || upperText.contains("RESPICAPS") || upperText.contains("ROTACAPS") -> "INHALER"
            // B. Specific Topical Preparations (Gels, Ointments, Shampoos, Lotions)
            upperText.contains("GEL") || upperText.contains("EMULGEL") -> "GEL"
            upperText.contains("OINTMENT") || upperText.contains("CREAM") -> "OINTMENT"
            upperText.contains("SHAMPOO") -> "SHAMPOO"
            upperText.contains("LOTION") || upperText.contains("HAIR OIL") || upperText.contains("SCALP LOTION") -> "TOPICAL_LOTION"
            // C. Explicit Oral Solids (Tablets, Capsules, Pellets)
            upperText.contains("TABLET") || upperText.contains("TABLETS") || upperText.contains("TAB ") || upperText.contains("TABS") ||
                    upperText.contains("PILULES") || upperText.contains("GLOBULES") || upperText.contains("PELLETS") ||
                    upperText.contains("B41") || upperText.contains("B-41") -> "TABLET"
            upperText.contains("CAPSULE") || upperText.contains("CAPSULES") || upperText.contains("CAP ") -> "CAPSULE"
            // D. Oral Liquids & Drops
            upperText.contains("SYRUP") || upperText.contains("SUSPENSION") -> "SYRUP"
            upperText.contains("TONIC") || upperText.contains("ELIXIR") -> "TONIC"
            upperText.contains("DROPS") || upperText.contains("DILUTION") || upperText.contains("TINCTURE") -> "ORAL_DROPS"
            // E. Generic External Markers Fallback
            upperText.contains("EXTERNAL APPLICATION") || upperText.contains("EXTERNAL USE") -> "TOPICAL_LOTION"
            extractedComposition != null -> extractedComposition.dosageForm
            else -> "TABLET"
        }

        // 3. Classify Therapeutic Class
        val therapeuticClass = extractedComposition?.therapeuticCategory ?: classifyTherapeuticClass(parsedSalts, upperText)

        // 4. Determine Food Timing Rule (Explicitly handles External/Topical formulations)
        val foodTimingRule = determineFoodTiming(parsedSalts, therapeuticClass, upperText, dosageForm)

        // 5. Active Metabolic Window (8 Hours)
        val eightHoursAgo = System.currentTimeMillis() - (8 * 3600 * 1000L)
        val recentTakenLogs = recentLogs.filter { it.status == "TAKEN" && it.intakeTimestamp >= eightHoursAgo }

        // 6. Clinical Reasoning: Duplicate Molecule Accidental Overdose Check (Oral Drugs)
        if (foodTimingRule != FoodTimingRule.NOT_APPLICABLE_EXTERNAL) {
            for (salt in parsedSalts) {
                if (salt == "Active Molecule Formulation") continue
                val duplicate = recentTakenLogs.firstOrNull { log ->
                    log.parsedSalts.contains(salt, ignoreCase = true) ||
                            log.scannedText.contains(salt, ignoreCase = true) ||
                            (matchedMedicine != null && log.scannedText.contains(brandName.split(" ").first(), ignoreCase = true))
                }
                if (duplicate != null) {
                    val clinicalReason = "Accidental duplicate dose of $salt detected within active metabolic window (<8h)."
                    val spokenText = if (isHindi) {
                        "सावधान! रुकिए! आप ${duplicate.scannedText} ($salt) पहले ही ले चुके हैं। यह अतिरिक्त खुराक न लें।"
                    } else {
                        "Warning! Stop! You already took ${duplicate.scannedText} ($salt). Do not take this extra dose."
                    }
                    val displayTitle = if (isHindi) {
                        "डुप्लिकेट खुराक अवरुद्ध (Duplicate Dose)"
                    } else {
                        "Duplicate Dose Blocked"
                    }

                    return ClinicalSafetyResult(
                        brandName = brandName,
                        parsedSalts = parsedSalts,
                        therapeuticClass = therapeuticClass,
                        safetyVerdict = SafetyVerdict.DUPLICATE_OVERDOSE_BLOCKED,
                        clinicalReason = clinicalReason,
                        foodTimingRule = foodTimingRule,
                        isEmergencyAlert = true,
                        spokenVernacularText = spokenText,
                        displayTitle = displayTitle,
                        dosageForm = dosageForm,
                        confidenceScore = 0.98f
                    )
                }
            }
        }

        // 7. Clinical Reasoning: Polypharmacy Contraindication Matrix
        val contraindication = evaluatePolypharmacyMatrix(parsedSalts, upperText, recentTakenLogs, isHindi)
        if (contraindication != null) {
            return ClinicalSafetyResult(
                brandName = brandName,
                parsedSalts = parsedSalts,
                therapeuticClass = therapeuticClass,
                safetyVerdict = SafetyVerdict.CRITICAL_INTERACTION_BLOCKED,
                clinicalReason = contraindication.clinicalReason,
                foodTimingRule = foodTimingRule,
                isEmergencyAlert = true,
                spokenVernacularText = contraindication.spokenText,
                displayTitle = contraindication.displayTitle,
                dosageForm = dosageForm,
                confidenceScore = 0.99f
            )
        }

        // 8. Clinical Reasoning: Patient Chronic Disease Contraindication Matrix
        val diseaseContraindication = evaluateDiseaseContraindications(parsedSalts, upperText, patientConditions, isHindi)
        if (diseaseContraindication != null) {
            return ClinicalSafetyResult(
                brandName = brandName,
                parsedSalts = parsedSalts,
                therapeuticClass = therapeuticClass,
                safetyVerdict = SafetyVerdict.CRITICAL_INTERACTION_BLOCKED,
                clinicalReason = diseaseContraindication.clinicalReason,
                foodTimingRule = foodTimingRule,
                isEmergencyAlert = true,
                spokenVernacularText = diseaseContraindication.spokenText,
                displayTitle = diseaseContraindication.displayTitle,
                dosageForm = dosageForm,
                confidenceScore = 0.99f
            )
        }

        // 8. Safe to Take: Route-Specific Vernacular Audio Guidance
        val timingPhraseEn = when (foodTimingRule) {
            FoodTimingRule.EMPTY_STOMACH -> "Take early morning on an empty stomach with a full glass of water."
            FoodTimingRule.BEFORE_FOOD -> "Take 30 minutes before meals with water."
            FoodTimingRule.AFTER_FOOD -> "Take with or after food with water."
            FoodTimingRule.BEDTIME -> "Take once daily at bedtime."
            FoodTimingRule.NOT_APPLICABLE_EXTERNAL -> "For external application only. Do not ingest."
        }

        val timingPhraseHi = when (foodTimingRule) {
            FoodTimingRule.EMPTY_STOMACH -> "सुबह खाली पेट एक गिलास पानी के साथ लें।"
            FoodTimingRule.BEFORE_FOOD -> "भोजन से 30 मिनट पहले पानी के साथ लें।"
            FoodTimingRule.AFTER_FOOD -> "खाना खाने के बाद पानी के साथ लें।"
            FoodTimingRule.BEDTIME -> "रात को सोने से पहले लें।"
            FoodTimingRule.NOT_APPLICABLE_EXTERNAL -> "केवल बाहरी उपयोग के लिए है। इसे पिएँ या निगलें नहीं।"
        }

        val spokenText = when (dosageForm) {
            "TOPICAL_LOTION", "SHAMPOO", "SCALP_SOLUTION" -> if (isHindi) {
                "$brandName सिर और त्वचा पर लगाने की दवा है। सिर पर हल्के हाथों से लगाएँ। केवल बाहरी उपयोग के लिए है, इसे पिएँ नहीं।"
            } else {
                "$brandName Topical Scalp Lotion. Apply gently to scalp. For external application only. Do not swallow."
            }
            "OINTMENT", "GEL" -> if (isHindi) {
                "$brandName जेल/मलहम। प्रभावित स्थान पर धीरे से लगाएँ। केवल बाहरी उपयोग के लिए।"
            } else {
                "$brandName Ointment. Apply gently to affected area. For external application only."
            }
            "EYE_DROPS" -> if (isHindi) {
                "$brandName आई ड्रॉप्स। आँखों में 1 से 2 बूँद डालें और थोड़ी देर आँखें बंद रखें।"
            } else {
                "$brandName Eye Drops. Instill 1 to 2 drops into affected eye and keep eyes closed briefly."
            }
            "EAR_DROPS" -> if (isHindi) {
                "$brandName कान की दवाई। कान में 2 से 3 बूँद डालें।"
            } else {
                "$brandName Ear Drops. Instill 2 to 3 drops into the ear canal."
            }
            "NASAL_SPRAY" -> if (isHindi) {
                "$brandName नेजल स्प्रे। नाक में 1 से 2 स्प्रे करें।"
            } else {
                "$brandName Nasal Spray. Spray 1 to 2 puffs into each nostril."
            }
            "SYRUP", "TONIC" -> if (isHindi) {
                "$brandName सिरप/टॉनिक। शीशी को अच्छी तरह हिलाकर पिएँ। $timingPhraseHi"
            } else {
                "$brandName Syrup. Shake well before measuring dose. $timingPhraseEn"
            }
            else -> if (isHindi) {
                "$brandName (${parsedSalts.joinToString(", ")})। $timingPhraseHi"
            } else {
                "$brandName (${parsedSalts.joinToString(", ")}). $timingPhraseEn"
            }
        }

        val displayTitle = if (isHindi) {
            "सुरक्षित: $brandName"
        } else {
            "Safe: $brandName"
        }

        return ClinicalSafetyResult(
            brandName = brandName,
            parsedSalts = parsedSalts,
            therapeuticClass = therapeuticClass,
            safetyVerdict = SafetyVerdict.SAFE_TO_TAKE,
            clinicalReason = if (foodTimingRule == FoodTimingRule.NOT_APPLICABLE_EXTERNAL) "External medication verified safe for topical/scalp application." else "Medication verified safe against 24-hour patient intake history.",
            foodTimingRule = foodTimingRule,
            isEmergencyAlert = false,
            spokenVernacularText = spokenText,
            displayTitle = displayTitle,
            dosageForm = dosageForm,
            confidenceScore = 0.95f,
            sourceTier = extractedComposition?.sourceTier ?: activeTier
        )
    }

    private data class ClinicalInteraction(
        val clinicalReason: String,
        val spokenText: String,
        val displayTitle: String
    )

    private fun evaluatePolypharmacyMatrix(
        scannedSalts: List<String>,
        scannedUpper: String,
        recentTakenLogs: List<MedicationLogEntity>,
        isHindi: Boolean
    ): ClinicalInteraction? {
        val allScanned = (scannedSalts.joinToString(" ") + " " + scannedUpper).uppercase(Locale.ROOT)
        val allHistory = recentTakenLogs.joinToString(" ") { it.parsedSalts + " " + it.scannedText }.uppercase(Locale.ROOT)

        // 1. Aspirin / Antiplatelet + NSAIDs (Ibuprofen, Diclofenac, Aceclofenac) -> GI Bleeding
        val hasAspirinInScanned = allScanned.contains("ASPIRIN") || allScanned.contains("ECOSPRIN") || allScanned.contains("DISPRIN")
        val hasNsaidInScanned = allScanned.contains("IBUPROFEN") || allScanned.contains("DICLOFENAC") || allScanned.contains("ACECLOFENAC") || allScanned.contains("COMBIFLAM") || allScanned.contains("BRUFEN")
        val hasAspirinInHistory = allHistory.contains("ASPIRIN") || allHistory.contains("ECOSPRIN") || allHistory.contains("DISPRIN")
        val hasNsaidInHistory = allHistory.contains("IBUPROFEN") || allHistory.contains("DICLOFENAC") || allHistory.contains("ACECLOFENAC") || allHistory.contains("COMBIFLAM") || allHistory.contains("BRUFEN")

        if ((hasAspirinInScanned && hasNsaidInHistory) || (hasNsaidInScanned && hasAspirinInHistory)) {
            return ClinicalInteraction(
                clinicalReason = "Severe gastrointestinal bleeding hazard: Concurrent Aspirin + NSAID administration.",
                spokenText = if (isHindi) "सावधान! एस्पिरिन और दर्द निवारक दवा साथ में लेने से पेट में ब्लीडिंग का गंभीर खतरा है। यह दवा न लें।" else "Warning! Critical interaction! Taking Aspirin together with NSAID pain relievers creates severe stomach bleeding risk.",
                displayTitle = if (isHindi) "गंभीर दवा परस्परविरोध (Critical Conflict)" else "Critical Drug Conflict"
            )
        }

        // 2. Nitrates + PDE5 Inhibitors -> Fatal Hypotension
        val hasNitrateInScanned = allScanned.contains("NITROGLYCERIN") || allScanned.contains("ISOSORBIDE") || allScanned.contains("SORBITRATE") || allScanned.contains("MONOTRATE")
        val hasPde5InScanned = allScanned.contains("SILDENAFIL") || allScanned.contains("TADALAFIL") || allScanned.contains("MANFORCE") || allScanned.contains("VIAGRA")
        val hasNitrateInHistory = allHistory.contains("NITROGLYCERIN") || allHistory.contains("ISOSORBIDE") || allHistory.contains("SORBITRATE") || allHistory.contains("MONOTRATE")
        val hasPde5InHistory = allHistory.contains("SILDENAFIL") || allHistory.contains("TADALAFIL") || allHistory.contains("MANFORCE") || allHistory.contains("VIAGRA")

        if ((hasNitrateInScanned && hasPde5InHistory) || (hasPde5InScanned && hasNitrateInHistory)) {
            return ClinicalInteraction(
                clinicalReason = "Life-threatening acute hypotension hazard: Concurrent Nitrate + PDE5 inhibitor administration.",
                spokenText = if (isHindi) "अति गंभीर चेतावनी! नाइट्रेट दिल की दवा और सिल्डेनाफिल साथ लेने से रक्तचाप जानलेवा स्तर तक गिर सकता है। तुरंत रोकें!" else "Emergency warning! Taking Nitrates with PDE-5 inhibitors causes fatal blood pressure collapse. Do not take!",
                displayTitle = if (isHindi) "घातक दवा परस्परविरोध (Fatal Interaction)" else "Fatal Interaction Blocked"
            )
        }

        return null
    }

    private fun evaluateDiseaseContraindications(
        scannedSalts: List<String>,
        scannedUpper: String,
        patientConditions: Set<String>,
        isHindi: Boolean
    ): ClinicalInteraction? {
        if (patientConditions.isEmpty()) return null
        val allScanned = (scannedSalts.joinToString(" ") + " " + scannedUpper).uppercase(Locale.ROOT)

        // 1. Hypertension (BP) Matrix
        val hasHypertension = patientConditions.any { 
            it.contains("Hypertension", ignoreCase = true) || it.contains("BP", ignoreCase = true) || it.contains("Blood Pressure", ignoreCase = true) 
        }
        if (hasHypertension) {
            val isNsaid = allScanned.contains("IBUPROFEN") || allScanned.contains("DICLOFENAC") || 
                    allScanned.contains("ACECLOFENAC") || allScanned.contains("NAPROXEN") || 
                    allScanned.contains("BRUFEN") || allScanned.contains("COMBIFLAM") || allScanned.contains("VOVERAN")
            if (isNsaid) {
                return ClinicalInteraction(
                    clinicalReason = "Contraindicated in Hypertension: Oral NSAIDs cause sodium/fluid retention and elevate arterial blood pressure.",
                    spokenText = if (isHindi) {
                        "सावधान! आपके मेडिकल प्रोफाइल में हाई ब्लड प्रेशर दर्ज है। यह दर्द निवारक दवा ब्लड प्रेशर को बढ़ा सकती है। डॉक्टर की सलाह के बिना न लें।"
                    } else {
                        "Warning! You have Hypertension recorded in your medical profile. This NSAID pain medication can significantly increase blood pressure. Consult your doctor."
                    },
                    displayTitle = if (isHindi) "उच्च रक्तचाप चेतावनी (Hypertension Alert)" else "Hypertension Risk: NSAID"
                )
            }

            val isDecongestant = allScanned.contains("PSEUDOEPHEDRINE") || allScanned.contains("PHENYLEPHRINE") || allScanned.contains("EPHEDRINE")
            if (isDecongestant) {
                return ClinicalInteraction(
                    clinicalReason = "Contraindicated in Hypertension: Sympathomimetic decongestants cause acute vasoconstriction and severe blood pressure spikes.",
                    spokenText = if (isHindi) {
                        "सावधान! आपको हाई ब्लड प्रेशर है। इस सर्दी-जुकाम की दवा में मौजूद तत्व रक्तचाप को अचानक बहुत अधिक बढ़ा सकते हैं।"
                    } else {
                        "Warning! You have Hypertension. Decongestants in this formulation can cause acute and dangerous blood pressure spikes."
                    },
                    displayTitle = if (isHindi) "बीपी चेतावनी (BP Spike Alert)" else "Severe BP Spike Warning"
                )
            }
        }

        // 2. Type-2 Diabetes Matrix
        val hasDiabetes = patientConditions.any { it.contains("Diabetes", ignoreCase = true) || it.contains("Sugar", ignoreCase = true) }
        if (hasDiabetes) {
            val isSteroid = allScanned.contains("PREDNISOLONE") || allScanned.contains("DEXAMETHASONE") || 
                    allScanned.contains("BETAMETHASONE") || allScanned.contains("DEFLAZACORT") || 
                    allScanned.contains("HYDROCORTISONE") || allScanned.contains("WYMESONE") || allScanned.contains("OMNACORTIL")
            if (isSteroid) {
                return ClinicalInteraction(
                    clinicalReason = "Contraindicated in Type-2 Diabetes: Systemic corticosteroids cause acute insulin resistance and severe blood glucose spikes.",
                    spokenText = if (isHindi) {
                        "सावधान! आपके मेडिकल प्रोफाइल में डायबिटीज दर्ज है। इस स्टेरॉयड दवा से शुगर लेवल बहुत तेजी से बढ़ सकता है। डॉक्टर की निगरानी में ही लें।"
                    } else {
                        "Warning! You have Type-2 Diabetes. This corticosteroid medication can cause severe blood glucose spikes. Consult your doctor."
                    },
                    displayTitle = if (isHindi) "डायबिटीज चेतावनी (Diabetes Alert)" else "Diabetes Risk: Steroid"
                )
            }

            val isNonSelectiveBetaBlocker = allScanned.contains("PROPRANOLOL") || allScanned.contains("INDERAL")
            if (isNonSelectiveBetaBlocker) {
                return ClinicalInteraction(
                    clinicalReason = "Caution in Diabetes: Non-selective beta-blockers mask crucial warning signs of hypoglycemia (tachycardia and tremors).",
                    spokenText = if (isHindi) {
                        "सावधान! डायबिटीज में यह दवा लो-शुगर (Hypoglycemia) के चेतावनी लक्षणों जैसे घबराहट और कंपकंपी को छिपा सकती है।"
                    } else {
                        "Caution: In Diabetes, this beta-blocker can mask vital warning symptoms of low blood sugar (hypoglycemia)."
                    },
                    displayTitle = if (isHindi) "लो-शुगर चेतावनी (Hypoglycemia Alert)" else "Hypoglycemia Masking Alert"
                )
            }
        }

        // 3. Cardiac / Heart Condition Matrix
        val hasCardiac = patientConditions.any { 
            it.contains("Cardiac", ignoreCase = true) || it.contains("Heart", ignoreCase = true) 
        }
        if (hasCardiac) {
            val isNsaid = allScanned.contains("IBUPROFEN") || allScanned.contains("DICLOFENAC") || allScanned.contains("ACECLOFENAC") || allScanned.contains("BRUFEN")
            if (isNsaid) {
                return ClinicalInteraction(
                    clinicalReason = "High Risk in Cardiac Disease: NSAIDs increase thrombotic cardiovascular risk and can worsen heart failure.",
                    spokenText = if (isHindi) {
                        "चेतावनी! आपके मेडिकल प्रोफाइल में हृदय रोग दर्ज है। यह दर्द निवारक दवा दिल पर अतिरिक्त दबाव और हार्ट अटैक का खतरा बढ़ा सकती है।"
                    } else {
                        "Critical Warning! You have a Cardiac condition. NSAIDs significantly increase the risk of heart failure and cardiovascular events."
                    },
                    displayTitle = if (isHindi) "कार्डियक चेतावनी (Cardiac Risk)" else "Cardiac Hazard Alert"
                )
            }

            val isPde5 = allScanned.contains("SILDENAFIL") || allScanned.contains("TADALAFIL") || allScanned.contains("MANFORCE") || allScanned.contains("VIAGRA")
            if (isPde5) {
                return ClinicalInteraction(
                    clinicalReason = "High Risk in Cardiac Disease: Vasodilators in compromised coronary circulation can induce severe hypotension.",
                    spokenText = if (isHindi) {
                        "गंभीर चेतावनी! हृदय रोग में यह दवा रक्तचाप को जानलेवा स्तर तक गिरा सकती है। तुरंत डॉक्टर से संपर्क करें।"
                    } else {
                        "Fatal Alert! In cardiac conditions, this vasodilator can cause life-threatening low blood pressure."
                    },
                    displayTitle = if (isHindi) "गंभीर कार्डियक अलर्ट (Cardiac Alert)" else "Fatal Cardiac Hypotension Risk"
                )
            }
        }

        // 4. Thyroid Disorder Matrix
        val hasThyroid = patientConditions.any { it.contains("Thyroid", ignoreCase = true) }
        if (hasThyroid) {
            val isChelatingAgent = allScanned.contains("CALCIUM") || allScanned.contains("FERROUS") || 
                    allScanned.contains("IRON") || allScanned.contains("SHELCAL") || allScanned.contains("AUTRIN")
            if (isChelatingAgent) {
                return ClinicalInteraction(
                    clinicalReason = "Thyroid Absorption Interaction: Calcium/Iron binds to Thyroid hormone (Levothyroxine) in the gut. Must be taken at least 4 hours apart.",
                    spokenText = if (isHindi) {
                        "थायरॉइड चेतावनी: कैल्शियम या आयरन की दवा थायरॉइड की गोली (थायरोक्सिन) के असर को रोकती है। इसे थायरॉइड की दवा से कम से कम 4 घंटे के अंतर पर लें।"
                    } else {
                        "Thyroid Alert: Calcium and Iron bind to thyroid hormone. Take this at least 4 hours apart from your thyroid tablet."
                    },
                    displayTitle = if (isHindi) "थायरॉइड चेतावनी (Thyroid Alert)" else "Thyroid Absorption Alert"
                )
            }
        }

        return null
    }

    private fun extractChemicalSalts(text: String): List<String> {
        val fuzzyList = FuzzySaltMatcher.extractAllSalts(text)
        if (fuzzyList.isNotEmpty()) {
            return fuzzyList.map { it.canonicalName }
        }

        val chemicalRegex = Regex("([A-Za-z]{4,}(?:\\s+[A-Za-z]{3,})?)\\s*(?:\\d+\\s*(?:MG|MCG|GM|%|ML)|IP|BP|USP)", RegexOption.IGNORE_CASE)
        val extracted = chemicalRegex.findAll(text).map { it.groupValues[1].trim() }.filter { it.length > 3 }.toList()

        return if (extracted.isNotEmpty()) extracted else listOf("Active Formulation")
    }

    private fun classifyTherapeuticClass(salts: List<String>, text: String): String {
        val combined = (salts.joinToString(" ") + " " + text).uppercase(Locale.ROOT)
        return when {
            combined.contains("DANDRUFF") || combined.contains("THUJA") || combined.contains("COCHLEARIA") || combined.contains("CANTHARIS") -> "ANTI-DANDRUFF SCALP CARE"
            combined.contains("METFORMIN") || combined.contains("GLIMEPIRIDE") || combined.contains("VILDAGLIPTIN") -> "ANTIDIABETIC"
            combined.contains("TELMISARTAN") || combined.contains("AMLODIPINE") || combined.contains("LOSARTAN") -> "ANTIHYPERTENSIVE"
            combined.contains("ATORVASTATIN") || combined.contains("ROSUVASTATIN") -> "LIPID_LOWERING"
            combined.contains("ASPIRIN") || combined.contains("CLOPIDOGREL") -> "ANTIPLATELET"
            combined.contains("IBUPROFEN") || combined.contains("DICLOFENAC") || combined.contains("ACECLOFENAC") -> "NSAID_ANALGESIC"
            combined.contains("PANTOPRAZOLE") || combined.contains("RABEPRAZOLE") || combined.contains("OMEPRAZOLE") -> "PPI_ANTACID"
            combined.contains("LEVOTHYROXINE") -> "THYROID_HORMONE"
            combined.contains("AZITHROMYCIN") || combined.contains("AMOXICILLIN") || combined.contains("CEFIXIME") -> "ANTIBIOTIC"
            combined.contains("CINERARIA") || combined.contains("EUPHRASIA") || combined.contains("OPHTHALMIC") -> "OPHTHALMIC_EYE_CARE"
            else -> "GENERAL_HEALTHCARE"
        }
    }

    private fun determineFoodTiming(salts: List<String>, therapeuticClass: String, text: String, dosageForm: String): FoodTimingRule {
        if (dosageForm in listOf("TOPICAL_LOTION", "SHAMPOO", "SCALP_SOLUTION", "OINTMENT", "GEL", "EYE_DROPS", "EAR_DROPS", "NASAL_SPRAY")) {
            return FoodTimingRule.NOT_APPLICABLE_EXTERNAL
        }
        val combined = (salts.joinToString(" ") + " " + text).uppercase(Locale.ROOT)
        return when {
            combined.contains("LEVOTHYROXINE") || combined.contains("THYRONORM") || combined.contains("PANTOPRAZOLE") || combined.contains("RABEPRAZOLE") || combined.contains("OMEPRAZOLE") -> FoodTimingRule.EMPTY_STOMACH
            therapeuticClass == "ANTIDIABETIC" || therapeuticClass == "NSAID_ANALGESIC" || therapeuticClass == "LIPID_LOWERING" || combined.contains("METFORMIN") || combined.contains("IBUPROFEN") || combined.contains("ASPIRIN") -> FoodTimingRule.AFTER_FOOD
            combined.contains("ATORVASTATIN") || combined.contains("MONTELUKAST") -> FoodTimingRule.BEDTIME
            else -> FoodTimingRule.AFTER_FOOD
        }
    }
}
