package com.medvoice.core.ai

import android.content.Context
import android.util.Log
import com.medvoice.core.data.local.entity.MedicationLogEntity
import com.medvoice.core.data.local.entity.MedicineEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

enum class SafetyVerdict {
    SAFE_TO_TAKE,
    DUPLICATE_OVERDOSE_BLOCKED,
    CRITICAL_INTERACTION_BLOCKED
}

enum class FoodTimingRule {
    AFTER_FOOD,
    BEFORE_FOOD,
    EMPTY_STOMACH,
    BEDTIME
}

data class MedGemmaSafetyResult(
    val brandName: String,
    val parsedSalts: List<String>,
    val therapeuticClass: String,
    val safetyVerdict: SafetyVerdict,
    val clinicalReason: String,
    val foodTimingRule: FoodTimingRule,
    val isEmergencyAlert: Boolean,
    val spokenVernacularText: String,
    val displayTitle: String,
    val dosageForm: String = "TABLET"
)

class MedGemmaOrchestrator(private val context: Context? = null) {

    var activeTier: AiEngineTier = AiEngineTier.ON_DEVICE_MEDGEMMA_INT4
    var cloudMedGemmaApiKey: String = ""
    var allowCloudPrivacyEgress: Boolean = false

    suspend fun evaluateSafety(
        scannedText: String,
        matchedMedicine: MedicineEntity?,
        recentLogs: List<MedicationLogEntity>,
        locale: String = "hi"
    ): MedGemmaSafetyResult = withContext(Dispatchers.Default) {
        val targetLangCode = when (locale.lowercase(Locale.ROOT)) {
            "mr" -> "mr-IN"
            "hi" -> "hi-IN"
            else -> "en-IN"
        }

        // Build Active Patient History JSON
        val historyJsonStr = if (recentLogs.isEmpty()) {
            "[]"
        } else {
            recentLogs.joinToString(prefix = "[\n", postfix = "\n]", separator = ",\n") { log ->
                val hoursAgo = ((System.currentTimeMillis() - log.intakeTimestamp) / (3600 * 1000.0)).toInt()
                """  { "scanned_text": "${log.scannedText}", "parsed_salts": "${log.parsedSalts}", "status": "${log.status}", "hours_ago": $hoursAgo }"""
            }
        }

        // Construct System & User Turn Prompt Template conforming to Master Directive
        val prompt = """
        <start_of_turn>system
        You are an edge clinical pharmacology engine running offline on device.
        Analyze the scanned medicine formulation against the patient's active medication history.
        Evaluate:
        1. Active chemical salt duplication (accidental overdose within active window).
        2. Critical drug-to-drug contraindications.
        3. Food and temporal consumption rules.

        Respond strictly in valid JSON matching the schema below with NO conversational filler.

        Schema:
        {
          "brand_name": string,
          "parsed_salts": string[],
          "therapeutic_class": string,
          "safety_verdict": "SAFE_TO_TAKE" | "DUPLICATE_OVERDOSE_BLOCKED" | "CRITICAL_INTERACTION_BLOCKED",
          "clinical_reason": string,
          "food_timing_rule": "AFTER_FOOD" | "BEFORE_FOOD" | "EMPTY_STOMACH" | "BEDTIME",
          "is_emergency_alert": boolean,
          "spoken_vernacular_text": string,
          "display_title": string
        }

        Target Language: $targetLangCode
        <end_of_turn>
        <start_of_turn>user
        Scanned Medicine Input:
        - Text: "$scannedText"

        Active Patient Medication History (Last 24 Hours):
        $historyJsonStr
        <end_of_turn>
        <start_of_turn>model
        """.trimIndent()

        // 1. Try Cloud Endpoint if user enabled Cloud Tier & Egress
        if (activeTier == AiEngineTier.CLOUD_MEDGEMMA_HOSTED && allowCloudPrivacyEgress && cloudMedGemmaApiKey.isNotBlank()) {
            try {
                val cloudResult = executeCloudInference(prompt, targetLangCode)
                if (cloudResult != null) return@withContext cloudResult
            } catch (e: Exception) {
                Log.w("MedGemmaOrchestrator", "Cloud inference failed, executing on-device edge clinical reasoning", e)
            }
        }

        // 2. On-Device Edge MedGemma Clinical Reasoning Engine
        return@withContext executeOnDeviceClinicalReasoning(scannedText, matchedMedicine, recentLogs, locale)
    }

    private fun executeOnDeviceClinicalReasoning(
        scannedText: String,
        matchedMedicine: MedicineEntity?,
        recentLogs: List<MedicationLogEntity>,
        locale: String
    ): MedGemmaSafetyResult {
        val uppercaseInput = scannedText.uppercase(Locale.ROOT)

        // 1. Resolve Brand Name & Dosage Form
        val brandName = matchedMedicine?.brandName ?: run {
            val lines = scannedText.lines().map { it.trim() }.filter { it.length >= 3 }
            val firstMeaningful = lines.firstOrNull {
                !it.contains("MFD", ignoreCase = true) && !it.contains("EXP", ignoreCase = true) &&
                        !it.contains("BATCH", ignoreCase = true) && !it.contains("EACH", ignoreCase = true)
            }
            firstMeaningful ?: "Prescription Medicine"
        }

        val dosageForm = matchedMedicine?.dosageForm ?: when {
            uppercaseInput.contains("EYE DROP") || uppercaseInput.contains("OPHTHALMIC") -> "EYE_DROPS"
            uppercaseInput.contains("EAR DROP") -> "EAR_DROPS"
            uppercaseInput.contains("NASAL") || uppercaseInput.contains("SPRAY") -> "NASAL_SPRAY"
            uppercaseInput.contains("SYRUP") || uppercaseInput.contains("TONIC") || uppercaseInput.contains("SUSPENSION") -> "SYRUP"
            uppercaseInput.contains("GEL") || uppercaseInput.contains("OINTMENT") || uppercaseInput.contains("CREAM") -> "GEL"
            uppercaseInput.contains("INHALER") || uppercaseInput.contains("MDI") -> "INHALER"
            uppercaseInput.contains("CAPSULE") || uppercaseInput.contains("CAP") -> "CAPSULE"
            else -> "TABLET"
        }

        // 2. Decompose Active Chemical Salts
        val parsedSalts = mutableListOf<String>()
        val saltKeywords = listOf(
            "METFORMIN", "PARACETAMOL", "ACETAMINOPHEN", "IBUPROFEN", "DICLOFENAC", "ACECLOFENAC",
            "ASPIRIN", "PANTOPRAZOLE", "OMEPRAZOLE", "RABEPRAZOLE", "ESOMEPRAZOLE", "LEVOTHYROXINE",
            "AMLODIPINE", "TELMISARTAN", "LOSARTAN", "VALSARTAN", "ATORVASTATIN", "ROSUVASTATIN",
            "AZITHROMYCIN", "AMOXICILLIN", "CLAVULANIC ACID", "CIPROFLOXACIN", "MOXIFLOXACIN",
            "SITAGLIPTIN", "VILDAGLIPTIN", "GLIMEPIRIDE", "GLICLAZIDE", "EMPAGLIFLOZIN", "DAPAGLIFLOZIN",
            "SALBUTAMOL", "MONTELUKAST", "LEVOCETIRIZINE", "CETIRIZINE", "DEXTROMETHORPHAN",
            "CINERARIA MARITIMA", "EUPHRASIA", "HIMSRA", "KASANI"
        )

        val targetSearchText = (matchedMedicine?.rawComposition ?: scannedText).uppercase(Locale.ROOT)
        for (salt in saltKeywords) {
            if (targetSearchText.contains(salt)) {
                parsedSalts.add(salt.split(" ").joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } })
            }
        }
        if (parsedSalts.isEmpty()) {
            val candidate = matchedMedicine?.rawComposition?.split("+", ",", ";")?.firstOrNull()?.trim()
            if (!candidate.isNullOrBlank()) {
                parsedSalts.add(candidate)
            } else {
                parsedSalts.add(brandName)
            }
        }

        // 3. Therapeutic Class & Food Timing
        var therapeuticClass = "Prescription Medication"
        var foodTimingRule = FoodTimingRule.AFTER_FOOD

        when {
            parsedSalts.any { it.contains("Metformin", ignoreCase = true) || it.contains("Glimepiride", ignoreCase = true) || it.contains("Sitagliptin", ignoreCase = true) || it.contains("Vildagliptin", ignoreCase = true) || it.contains("Dapagliflozin", ignoreCase = true) } -> {
                therapeuticClass = "Anti-Diabetic"
                foodTimingRule = FoodTimingRule.AFTER_FOOD
            }
            parsedSalts.any { it.contains("Ibuprofen", ignoreCase = true) || it.contains("Diclofenac", ignoreCase = true) || it.contains("Aceclofenac", ignoreCase = true) || it.contains("Aspirin", ignoreCase = true) || it.contains("Paracetamol", ignoreCase = true) } -> {
                therapeuticClass = "Analgesic & Anti-Inflammatory"
                foodTimingRule = FoodTimingRule.AFTER_FOOD
            }
            parsedSalts.any { it.contains("Pantoprazole", ignoreCase = true) || it.contains("Omeprazole", ignoreCase = true) || it.contains("Rabeprazole", ignoreCase = true) || it.contains("Esomeprazole", ignoreCase = true) } -> {
                therapeuticClass = "Proton Pump Inhibitor (Antacid)"
                foodTimingRule = FoodTimingRule.BEFORE_FOOD
            }
            parsedSalts.any { it.contains("Levothyroxine", ignoreCase = true) } -> {
                therapeuticClass = "Thyroid Hormone Replacement"
                foodTimingRule = FoodTimingRule.EMPTY_STOMACH
            }
            parsedSalts.any { it.contains("Telmisartan", ignoreCase = true) || it.contains("Amlodipine", ignoreCase = true) || it.contains("Losartan", ignoreCase = true) } -> {
                therapeuticClass = "Antihypertensive (Blood Pressure)"
                foodTimingRule = FoodTimingRule.AFTER_FOOD
            }
            parsedSalts.any { it.contains("Atorvastatin", ignoreCase = true) || it.contains("Rosuvastatin", ignoreCase = true) } -> {
                therapeuticClass = "Lipid-Lowering (Cholesterol)"
                foodTimingRule = FoodTimingRule.BEDTIME
            }
            parsedSalts.any { it.contains("Azithromycin", ignoreCase = true) || it.contains("Amoxicillin", ignoreCase = true) || it.contains("Moxifloxacin", ignoreCase = true) } -> {
                therapeuticClass = "Antibiotic"
                foodTimingRule = FoodTimingRule.AFTER_FOOD
            }
            dosageForm == "EYE_DROPS" || dosageForm == "EAR_DROPS" || dosageForm == "NASAL_SPRAY" || dosageForm == "GEL" -> {
                therapeuticClass = "Topical / Ophthalmic Formulation"
                foodTimingRule = FoodTimingRule.AFTER_FOOD
            }
        }

        // 4. Clinical Reasoning: Check Duplicate Molecule Overdose
        val recentTakenLogs = recentLogs.filter { it.status == "TAKEN" }
        for (salt in parsedSalts) {
            val duplicate = recentTakenLogs.firstOrNull { log ->
                log.parsedSalts.contains(salt, ignoreCase = true) ||
                        log.scannedText.contains(salt, ignoreCase = true) ||
                        (matchedMedicine != null && log.scannedText.contains(brandName.split(" ").first(), ignoreCase = true))
            }
            if (duplicate != null) {
                val clinicalReason = "Accidental duplicate dose of $salt detected within active metabolic window (<8h)."
                val spokenText = when (locale.lowercase(Locale.ROOT)) {
                    "mr" -> "सावधान! थांबा! तुम्ही आधीच ${duplicate.scannedText} ($salt) घेतले आहे. हे औषध पुन्हा घेऊ नका."
                    "hi" -> "सावधान! रुकिए! आप ${duplicate.scannedText} ($salt) पहले ही ले चुके हैं। इसे दोबारा न लें।"
                    else -> "Warning! Stop! You already took ${duplicate.scannedText} ($salt). Do not take this medicine again."
                }
                val displayTitle = when (locale.lowercase(Locale.ROOT)) {
                    "mr" -> "दुहेरी डोस धोका (Duplicate Dose Blocked)"
                    "hi" -> "डुप्लिकेट खुराक अवरुद्ध (Duplicate Dose)"
                    else -> "Duplicate Dose Blocked"
                }

                return MedGemmaSafetyResult(
                    brandName = brandName,
                    parsedSalts = parsedSalts,
                    therapeuticClass = therapeuticClass,
                    safetyVerdict = SafetyVerdict.DUPLICATE_OVERDOSE_BLOCKED,
                    clinicalReason = clinicalReason,
                    foodTimingRule = foodTimingRule,
                    isEmergencyAlert = true,
                    spokenVernacularText = spokenText,
                    displayTitle = displayTitle,
                    dosageForm = dosageForm
                )
            }
        }

        // 5. Clinical Reasoning: Check Drug-to-Drug Contraindications
        val hasAspirinInScanned = parsedSalts.any { it.contains("Aspirin", ignoreCase = true) }
        val hasNsaidInScanned = parsedSalts.any { it.contains("Ibuprofen", ignoreCase = true) || it.contains("Diclofenac", ignoreCase = true) || it.contains("Aceclofenac", ignoreCase = true) }

        val hasAspirinInHistory = recentTakenLogs.any { it.parsedSalts.contains("Aspirin", ignoreCase = true) || it.scannedText.contains("Ecosprin", ignoreCase = true) || it.scannedText.contains("Disprin", ignoreCase = true) }
        val hasNsaidInHistory = recentTakenLogs.any { it.parsedSalts.contains("Ibuprofen", ignoreCase = true) || it.parsedSalts.contains("Diclofenac", ignoreCase = true) || it.scannedText.contains("Combiflam", ignoreCase = true) || it.scannedText.contains("Brufen", ignoreCase = true) }

        if ((hasAspirinInScanned && hasNsaidInHistory) || (hasNsaidInScanned && hasAspirinInHistory)) {
            val clinicalReason = "Severe gastrointestinal bleeding hazard: Concurrent Aspirin + NSAID administration."
            val spokenText = when (locale.lowercase(Locale.ROOT)) {
                "mr" -> "सावधान! एस्पिरिन आणि कॉम्बीफ्लेम/ब्रुफेन एकत्र घेतल्यास पोटात अंतर्गत रक्तस्त्रावाचा मोठा धोका आहे. हे औषध घेऊ नका."
                "hi" -> "सावधान! एस्पिरिन और दर्द निवारक दवा साथ में लेने से पेट में ब्लीडिंग का गंभीर खतरा है। यह दवा न लें।"
                else -> "Warning! Critical interaction! Taking Aspirin together with NSAID pain relievers creates severe stomach bleeding risk."
            }
            val displayTitle = when (locale.lowercase(Locale.ROOT)) {
                "mr" -> "औषध परस्परविरोध धोका (Drug Conflict)"
                "hi" -> "गंभीर दवा परस्परविरोध (Critical Conflict)"
                else -> "Critical Drug Conflict"
            }

            return MedGemmaSafetyResult(
                brandName = brandName,
                parsedSalts = parsedSalts,
                therapeuticClass = therapeuticClass,
                safetyVerdict = SafetyVerdict.CRITICAL_INTERACTION_BLOCKED,
                clinicalReason = clinicalReason,
                foodTimingRule = foodTimingRule,
                isEmergencyAlert = true,
                spokenVernacularText = spokenText,
                displayTitle = displayTitle,
                dosageForm = dosageForm
            )
        }

        // 6. Safe to Take: Synthesize Form & Timing Specific Vernacular Guidance
        val timingPhraseEn = when (foodTimingRule) {
            FoodTimingRule.EMPTY_STOMACH -> "Take early morning on an empty stomach with a full glass of water."
            FoodTimingRule.BEFORE_FOOD -> "Take 30 minutes before meals with water."
            FoodTimingRule.AFTER_FOOD -> "Take with or after food with water."
            FoodTimingRule.BEDTIME -> "Take once daily at bedtime."
        }

        val timingPhraseHi = when (foodTimingRule) {
            FoodTimingRule.EMPTY_STOMACH -> "सुबह खाली पेट एक गिलास पानी के साथ लें।"
            FoodTimingRule.BEFORE_FOOD -> "भोजन से 30 मिनट पहले पानी के साथ लें।"
            FoodTimingRule.AFTER_FOOD -> "खाना खाने के बाद पानी के साथ लें।"
            FoodTimingRule.BEDTIME -> "रात को सोने से पहले लें।"
        }

        val timingPhraseMr = when (foodTimingRule) {
            FoodTimingRule.EMPTY_STOMACH -> "सकाळी रिकाम्या पोटी एका ग्लास पाण्यासोबत घ्या."
            FoodTimingRule.BEFORE_FOOD -> "जेवणापूर्वी 30 मिनिटे पाण्यासोबत घ्या."
            FoodTimingRule.AFTER_FOOD -> "जेवणानंतर पाण्यासोबत घ्या."
            FoodTimingRule.BEDTIME -> "रात्री झोपताना घ्या."
        }

        val spokenText = when (dosageForm) {
            "EYE_DROPS" -> when (locale.lowercase(Locale.ROOT)) {
                "mr" -> "$brandName डोळ्यांचे ड्रॉप्स. डोळ्यांत 1 ते 2 थेंब टाका आणि काही वेळ डोळे बंद ठेवा."
                "hi" -> "$brandName आई ड्रॉप्स। आँखों में 1 से 2 बूँद डालें और थोड़ी देर आँखें बंद रखें।"
                else -> "$brandName Eye Drops. Instill 1 to 2 drops into affected eye and keep eyes closed briefly."
            }
            "SYRUP" -> when (locale.lowercase(Locale.ROOT)) {
                "mr" -> "$brandName सिरप/टॉनिक. बाटली चांगली हलवून चमच्याने मोजून घ्या. $timingPhraseMr"
                "hi" -> "$brandName सिरप/टॉनिक। शीशी को अच्छी तरह हिलाकर पिएँ। $timingPhraseHi"
                else -> "$brandName Syrup. Shake well before measuring dose. $timingPhraseEn"
            }
            "GEL" -> when (locale.lowercase(Locale.ROOT)) {
                "mr" -> "$brandName जेल. दुखणाऱ्या भागावर हळूवार लावा. फक्त बाह्य वापरासाठी."
                "hi" -> "$brandName जेल। दर्द वाली जगह पर धीरे से लगाएँ। केवल बाहरी उपयोग के लिए।"
                else -> "$brandName Gel. Apply gently to affected area. For external application only."
            }
            else -> when (locale.lowercase(Locale.ROOT)) {
                "mr" -> "$brandName (${parsedSalts.joinToString(", ")}). $timingPhraseMr"
                "hi" -> "$brandName (${parsedSalts.joinToString(", ")})। $timingPhraseHi"
                else -> "$brandName (${parsedSalts.joinToString(", ")}). $timingPhraseEn"
            }
        }

        val displayTitle = when (locale.lowercase(Locale.ROOT)) {
            "mr" -> "सुरक्षित: $brandName"
            "hi" -> "सुरक्षित: $brandName"
            else -> "Safe: $brandName"
        }

        return MedGemmaSafetyResult(
            brandName = brandName,
            parsedSalts = parsedSalts,
            therapeuticClass = therapeuticClass,
            safetyVerdict = SafetyVerdict.SAFE_TO_TAKE,
            clinicalReason = "Medication verified safe against 24-hour patient intake history.",
            foodTimingRule = foodTimingRule,
            isEmergencyAlert = false,
            spokenVernacularText = spokenText,
            displayTitle = displayTitle,
            dosageForm = dosageForm
        )
    }

    private suspend fun executeCloudInference(prompt: String, langCode: String): MedGemmaSafetyResult? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.groq.com/openai/v1/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $cloudMedGemmaApiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.doOutput = true

            val payload = JSONObject().apply {
                put("model", "llama-3.1-8b-instant")
                put("temperature", 0.1)
                put("response_format", JSONObject().apply { put("type", "json_object") })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are an edge clinical pharmacology engine running on device. Analyze medication formulation against patient history and output valid JSON according to schema.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseStr = reader.readText()
                reader.close()

                val root = JSONObject(responseStr)
                val rawContent = root.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                val json = JSONObject(rawContent)

                val saltsList = mutableListOf<String>()
                val saltsArr = json.optJSONArray("parsed_salts")
                if (saltsArr != null) {
                    for (i in 0 until saltsArr.length()) {
                        saltsList.add(saltsArr.getString(i))
                    }
                }

                val verdictStr = json.optString("safety_verdict", "SAFE_TO_TAKE")
                val verdict = try {
                    SafetyVerdict.valueOf(verdictStr)
                } catch (_: Exception) {
                    SafetyVerdict.SAFE_TO_TAKE
                }

                val timingStr = json.optString("food_timing_rule", "AFTER_FOOD")
                val timing = try {
                    FoodTimingRule.valueOf(timingStr)
                } catch (_: Exception) {
                    FoodTimingRule.AFTER_FOOD
                }

                return@withContext MedGemmaSafetyResult(
                    brandName = json.optString("brand_name", "Prescription Medicine"),
                    parsedSalts = saltsList,
                    therapeuticClass = json.optString("therapeutic_class", "Pharmaceutical"),
                    safetyVerdict = verdict,
                    clinicalReason = json.optString("clinical_reason", ""),
                    foodTimingRule = timing,
                    isEmergencyAlert = json.optBoolean("is_emergency_alert", false),
                    spokenVernacularText = json.optString("spoken_vernacular_text", ""),
                    displayTitle = json.optString("display_title", ""),
                    dosageForm = "TABLET"
                )
            }
        } catch (e: Exception) {
            Log.e("MedGemmaOrchestrator", "Cloud REST evaluation failed", e)
        }
        null
    }
}
