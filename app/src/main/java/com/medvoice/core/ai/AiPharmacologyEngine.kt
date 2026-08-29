package com.medvoice.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.medvoice.core.vision.ImagePreprocessingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

enum class AiEngineTier {
    ON_DEVICE_MEDGEMMA_INT4,
    CLOUD_MEDGEMMA_HOSTED,
    OFFLINE_REGEX_DETERMINISTIC
}

data class ExtractedMedicineComposition(
    val brandName: String,
    val activeSalts: List<String>,
    val strengthMg: Double,
    val dosageForm: String,
    val therapeuticCategory: String,
    val confidenceScore: Float,
    val sourceTier: AiEngineTier,
    val vernacularInstructionEn: String = "",
    val vernacularInstructionHi: String = "",
    val routeOfAdministration: String = "ORAL"
)

class AiPharmacologyEngine(private val context: Context? = null) {

    var activeTier: AiEngineTier = AiEngineTier.ON_DEVICE_MEDGEMMA_INT4
    var cloudMedGemmaApiKey: String = ""
    var cloudModelName: String = "qwen/qwen3.8-27b"
    var allowCloudPrivacyEgress: Boolean = true

    init {
        if (context != null) {
            activeTier = if (com.medvoice.core.device.HardwareCapabilities.isLocalSlmCapable(context)) {
                AiEngineTier.ON_DEVICE_MEDGEMMA_INT4
            } else {
                AiEngineTier.CLOUD_MEDGEMMA_HOSTED
            }
            Log.d("AiPharmacologyEngine", "Hardware auto-detected AiEngineTier: $activeTier")
        }
    }

    // Known Chemical & Botanical Dictionary for On-Device Clinical Tokenizer
    private val knownChemicalDictionary = mapOf(
        // Topical Hair & Scalp Solutions (Anti-Dandruff, Hair Oils, Lotions)
        "DANDRUFF AID" to Triple("Anti-Dandruff Scalp Care", 0.0, "TOPICAL_LOTION"),
        "BAKSON" to Triple("Homeopathic Healthcare", 0.0, "TOPICAL_LOTION"),
        "THUJA" to Triple("Homeopathic Scalp & Skin Care", 0.0, "TOPICAL_LOTION"),
        "COCHLEARIA" to Triple("Homeopathic Hair Care", 0.0, "TOPICAL_LOTION"),
        "CANTHARIS" to Triple("Homeopathic Hair & Burn Care", 0.0, "TOPICAL_LOTION"),
        "KETOCONAZOLE" to Triple("Antifungal Scalp Care", 2.0, "SHAMPOO"),
        "MINOXIDIL" to Triple("Hair Regrowth Solution", 5.0, "TOPICAL_LOTION"),
        "SALICYLIC ACID" to Triple("Keratolytic Scalp Solution", 2.0, "TOPICAL_LOTION"),
        "ZINC PYRITHIONE" to Triple("Anti-Dandruff Scalp Care", 1.0, "SHAMPOO"),

        // Ophthalmic / Eye Drops
        "EUPHRASIA" to Triple("Ophthalmic Eye Care", 0.0, "EYE_DROPS"),
        "CINERARIA" to Triple("Ophthalmic Eye Drops", 0.0, "EYE_DROPS"),
        "MARITIMA" to Triple("Ophthalmic Eye Drops", 0.0, "EYE_DROPS"),
        "CARBOXYMETHYLCELLULOSE" to Triple("Artificial Tears / Eye Lubricant", 0.5, "EYE_DROPS"),
        "MOXIFLOXACIN" to Triple("Ophthalmic Antibiotic", 0.5, "EYE_DROPS"),
        "TOBRAMYCIN" to Triple("Ophthalmic Antibiotic", 0.3, "EYE_DROPS"),
        "TIMOLOL" to Triple("Glaucoma Eye Drops", 0.5, "EYE_DROPS"),
        "OLOPATADINE" to Triple("Antiallergic Eye Drops", 0.1, "EYE_DROPS"),

        // Nasal / Inhalers
        "XYLOMETAZOLINE" to Triple("Nasal Decongestant", 0.1, "NASAL_SPRAY"),
        "OXYMETAZOLINE" to Triple("Nasal Decongestant", 0.05, "NASAL_SPRAY"),
        "SALBUTAMOL" to Triple("Bronchodilator Inhaler", 100.0, "INHALER"),
        "BUDESONIDE" to Triple("Corticosteroid Inhaler", 200.0, "INHALER"),
        "FLUTICASONE" to Triple("Nasal Spray / Inhaler", 50.0, "NASAL_SPRAY"),

        // Syrups & Tonics
        "DEXTROMETHORPHAN" to Triple("Cough Suppressant Syrup", 10.0, "SYRUP"),
        "GUAIFENESIN" to Triple("Expectorant Cough Syrup", 100.0, "SYRUP"),
        "AMBROXOL" to Triple("Mucolytic Syrup", 30.0, "SYRUP"),
        "SUCRALFATE" to Triple("Stomach Ulcer Suspension", 1000.0, "SYRUP"),
        "MAGALDRATE" to Triple("Antacid Suspension", 400.0, "SYRUP"),
        "SIMETHICONE" to Triple("Antiflatulent Gas Relief", 40.0, "SYRUP"),
        "LACTULOSE" to Triple("Laxative Syrup", 10.0, "SYRUP"),
        "CYPROHEPTADINE" to Triple("Appetite Stimulant Tonic", 2.0, "TONIC"),

        // Topical Ointments & Gels
        "CLOTRIMAZOLE" to Triple("Antifungal Cream", 1.0, "OINTMENT"),
        "BETAMETHASONE" to Triple("Topical Corticosteroid", 0.05, "OINTMENT"),
        "CLOBETASOL" to Triple("Topical Corticosteroid", 0.05, "OINTMENT"),
        "MUPIROCIN" to Triple("Antibacterial Ointment", 2.0, "OINTMENT"),
        "POVIDONE" to Triple("Antiseptic Solution / Ointment", 5.0, "OINTMENT"),
        "VOLINI" to Triple("Pain Relief Gel", 0.0, "GEL"),
        "MOOV" to Triple("Pain Relief Ointment", 0.0, "OINTMENT"),

        // Common Oral Solid Forms
        "METFORMIN" to Triple("Antidiabetic (Sugar Control)", 500.0, "TABLET"),
        "GLIMEPIRIDE" to Triple("Antidiabetic (Sugar Control)", 2.0, "TABLET"),
        "VILDAGLIPTIN" to Triple("Antidiabetic (Sugar Control)", 50.0, "TABLET"),
        "SITAGLIPTIN" to Triple("Antidiabetic (Sugar Control)", 100.0, "TABLET"),
        "DAPAGLIFLOZIN" to Triple("Antidiabetic / Kidney Protection", 10.0, "TABLET"),
        "PARACETAMOL" to Triple("Analgesic / Fever Reducer", 650.0, "TABLET"),
        "IBUPROFEN" to Triple("NSAID Pain Reliever", 400.0, "TABLET"),
        "ACECLOFENAC" to Triple("NSAID Pain Reliever", 100.0, "TABLET"),
        "DICLOFENAC" to Triple("NSAID Pain Reliever", 50.0, "TABLET"),
        "ASPIRIN" to Triple("Blood Thinner / Antiplatelet", 75.0, "TABLET"),
        "ECOSPRIN" to Triple("Blood Thinner / Antiplatelet", 75.0, "TABLET"),
        "LEVOTHYROXINE" to Triple("Thyroid Hormone", 50.0, "TABLET"),
        "THYRONORM" to Triple("Thyroid Hormone", 50.0, "TABLET"),
        "PANTOPRAZOLE" to Triple("Proton Pump Inhibitor (Gas/Acidity)", 40.0, "TABLET"),
        "RABEPRAZOLE" to Triple("Proton Pump Inhibitor (Gas/Acidity)", 20.0, "TABLET"),
        "OMEPRAZOLE" to Triple("Proton Pump Inhibitor (Gas/Acidity)", 20.0, "CAPSULE"),
        "ATORVASTATIN" to Triple("Cholesterol Statin", 10.0, "TABLET"),
        "ROSUVASTATIN" to Triple("Cholesterol Statin", 10.0, "TABLET"),
        "AMLODIPINE" to Triple("BP Lowering Medicine", 5.0, "TABLET"),
        "TELMISARTAN" to Triple("BP / Heart Protection", 40.0, "TABLET"),
        "LOSARTAN" to Triple("BP Lowering Medicine", 50.0, "TABLET"),
        "AZITHROMYCIN" to Triple("Macrolide Antibiotic", 500.0, "TABLET"),
        "AMOXICILLIN" to Triple("Penicillin Antibiotic", 500.0, "CAPSULE"),
        "CEFIXIME" to Triple("Cephalosporin Antibiotic", 200.0, "TABLET"),
        "CIPROFLOXACIN" to Triple("Quinolone Antibiotic", 500.0, "TABLET"),
        "OFLOXACIN" to Triple("Quinolone Antibiotic", 200.0, "TABLET"),
        "CETIRIZINE" to Triple("Antihistamine / Allergy", 10.0, "TABLET"),
        "LEVOCETIRIZINE" to Triple("Antihistamine / Allergy", 5.0, "TABLET"),
        "MONTELUKAST" to Triple("Antiasthmatic / Antiallergic", 10.0, "TABLET"),
        "DOMPERIDONE" to Triple("Antiemetic / Nausea Relief", 10.0, "TABLET")
    )

    private val noisePatterns = listOf(
        Regex("""(?i)\b(?:store in a cool|dry place|protected from light|keep out of reach of children)\b.*"""),
        Regex("""(?i)\b(?:mfg\.? lic|mfd\.? by|marketed by|batch no|exp\.? date|mrp|pkd|mfg date|inclusive of all taxes)\b.*"""),
        Regex("""(?i)\b(?:for external use only|ophthalmic use|sterile|homoeopathic medicine|ayurvedic medicine|schedule h)\b.*"""),
        Regex("""(?i)\b(?:net vol|net wt|dosage|direction for use|composition|each ml contains|each tablet contains)\b.*"""),
        Regex("""(?i)\b(?:ip|bp|usp|ph\.? eur|ltd|pvt|pharmaceuticals|laboratories|healthcare)\b""")
    )

    fun containsPharmaceuticalMarkers(text: String): Boolean {
        if (text.isBlank()) return false
        val upper = text.uppercase(Locale.ROOT)

        if (knownChemicalDictionary.keys.any { upper.contains(it) }) return true
        if (FuzzySaltMatcher.findBestMatch(text) != null) return true

        val hasStrength = Regex("""\b\d+(?:\.\d+)?\s*(?:mg|mcg|µg|gm|g|ml|iu|%)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)
        val hasDosageForm = Regex("""\b(?:tablets?|capsules?|syrup|lotion|shampoo|ointment|drops?|inhaler|injection|suspension|elixir|gel|emulgel|tonics?|respicaps?|dispersible|dandruff|aid)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)
        val hasPharmaMarker = Regex("""\b(?:i\.?p\.?|b\.?p\.?|u\.?s\.?p\.?|ph\.?\s*eur|schedule\s+[hghx]|mfg\.?\s*lic|batch\s*no|exp\.?\s*date|composition|each\s+contains|each\s+film\s+coated|for\s+external\s+use|rx\s+only|homoeopathic|ayurvedic)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)

        return (hasStrength && (hasDosageForm || hasPharmaMarker)) || (hasDosageForm && hasPharmaMarker) || hasDosageForm
    }

    suspend fun parsePrescriptionText(
        rawOcrText: String,
        bitmap: Bitmap? = null
    ): ExtractedMedicineComposition? = withContext(Dispatchers.IO) {
        if (rawOcrText.isBlank() && bitmap == null) return@withContext null

        when (activeTier) {
            AiEngineTier.ON_DEVICE_MEDGEMMA_INT4 -> {
                runClinicalDeterministicParser(rawOcrText)
            }

            AiEngineTier.CLOUD_MEDGEMMA_HOSTED -> {
                if (allowCloudPrivacyEgress && cloudMedGemmaApiKey.isNotBlank()) {
                    if (bitmap != null) {
                        runMultimodalVision(bitmap, rawOcrText) ?: runCloudMedGemma(rawOcrText) ?: runClinicalDeterministicParser(rawOcrText)
                    } else {
                        runCloudMedGemma(rawOcrText) ?: runClinicalDeterministicParser(rawOcrText)
                    }
                } else {
                    runClinicalDeterministicParser(rawOcrText)
                }
            }

            AiEngineTier.OFFLINE_REGEX_DETERMINISTIC -> {
                runClinicalDeterministicParser(rawOcrText)
            }
        }
    }

    /**
     * Groq Multimodal Visual AI Engine (llama-3.2-11b-vision-preview).
     * Extracts full pharmaceutical label information directly from the high-resolution packaging image.
     */
    private suspend fun runMultimodalVision(
        bitmap: Bitmap,
        rawOcrText: String
    ): ExtractedMedicineComposition? = withContext(Dispatchers.IO) {
        try {
            val base64Image = ImagePreprocessingEngine.toBase64Jpeg(bitmap, maxDimension = 1024, quality = 80)
            val systemPrompt = """
                You are a strict clinical medicine label vision analyzer.
                Analyze the provided medicine packaging photo and OCR text.
                Return ONLY valid JSON matching this schema:
                {
                  "is_medicine": true,
                  "confidence_score": 0.98,
                  "brand_name": "Exact Brand Name (e.g. Bakson's Dandruff Aid, Dolo 650)",
                  "active_salts": ["Active Ingredient 1", "Active Ingredient 2"],
                  "strength_mg": 0.0,
                  "dosage_form": "TOPICAL_LOTION | TABLET | CAPSULE | SYRUP | OINTMENT | GEL | EYE_DROPS | SHAMPOO | INHALER",
                  "route_of_administration": "EXTERNAL_TOPICAL | ORAL | OPHTHALMIC | NASAL",
                  "therapeutic_class": "Therapeutic Class (e.g. Anti-Dandruff Scalp Care, Analgesic)"
                }
                If the image is NOT medicine packaging, return {"is_medicine": false, "confidence_score": 0.0}.
            """.trimIndent()

            val contentArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", "Identify this medicine packaging. Additional OCR text: $rawOcrText")
                })
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$base64Image")
                    })
                })
            }

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", contentArray)
                })
            }

            val requestJson = JSONObject().apply {
                put("model", "llama-3.2-11b-vision-preview")
                put("temperature", 0.0)
                put("response_format", JSONObject().put("type", "json_object"))
                put("messages", messages)
            }

            val url = URL("https://api.groq.com/openai/v1/chat/completions")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 7000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${cloudMedGemmaApiKey.trim()}")
                setRequestProperty("User-Agent", "MedVoice-Vision/1.0")
            }

            connection.outputStream.use { os ->
                val input = requestJson.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val groqResp = JSONObject(responseBody)
                val choices = groqResp.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val content = choices.getJSONObject(0).optJSONObject("message")?.optString("content", "") ?: ""
                    if (content.isNotBlank()) {
                        val cleanJson = if (content.contains("{")) content.substring(content.indexOf("{"), content.lastIndexOf("}") + 1) else content
                        val json = JSONObject(cleanJson)
                        if (json.optBoolean("is_medicine", false)) {
                            val brand = json.optString("brand_name", "").trim()
                            val saltsArray = json.optJSONArray("active_salts")
                            val saltsList = mutableListOf<String>()
                            if (saltsArray != null) {
                                for (i in 0 until saltsArray.length()) {
                                    val s = saltsArray.getString(i).trim()
                                    if (s.isNotBlank()) saltsList.add(s)
                                }
                            }
                            if (brand.isNotBlank() && saltsList.isNotEmpty()) {
                                val dosageForm = json.optString("dosage_form", "TABLET").uppercase(Locale.US)
                                val route = json.optString("route_of_administration", "ORAL").uppercase(Locale.US)
                                val instructions = generateVernacularGuidance(brand, dosageForm)

                                Log.d("AiPharmacologyEngine", "Groq Visual AI successfully recognized: $brand ($dosageForm / $route)")
                                return@withContext ExtractedMedicineComposition(
                                    brandName = brand,
                                    activeSalts = saltsList,
                                    strengthMg = json.optDouble("strength_mg", 0.0),
                                    dosageForm = dosageForm,
                                    therapeuticCategory = json.optString("therapeutic_class", "PHARMACEUTICAL"),
                                    confidenceScore = json.optDouble("confidence_score", 0.98).toFloat(),
                                    sourceTier = AiEngineTier.CLOUD_MEDGEMMA_HOSTED,
                                    vernacularInstructionEn = instructions.first,
                                    vernacularInstructionHi = instructions.second,
                                    routeOfAdministration = route
                                )
                            }
                        }
                    }
                }
            } else {
                val err = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "HTTP $responseCode"
                Log.w("AiPharmacologyEngine", "Groq Vision API returned $responseCode: $err")
            }
            null
        } catch (e: Exception) {
            Log.w("AiPharmacologyEngine", "Groq Vision exception: ${e.message}")
            null
        }
    }

    private suspend fun runCloudMedGemma(text: String): ExtractedMedicineComposition? = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are a strict clinical pharmacology verification system.
                Analyze the scanned OCR text from medicine packaging.
                If authentic medicine, return JSON:
                {
                  "is_medicine": true,
                  "confidence_score": 0.96,
                  "brand_name": "Exact Brand Name",
                  "active_salts": ["Active Salt 1", "Active Salt 2"],
                  "strength_mg": 500.0,
                  "dosage_form": "TOPICAL_LOTION | TABLET | CAPSULE | SYRUP | OINTMENT | GEL | EYE_DROPS | SHAMPOO | INHALER",
                  "route_of_administration": "EXTERNAL_TOPICAL | ORAL | OPHTHALMIC",
                  "therapeutic_class": "Therapeutic Class"
                }
                Otherwise return {"is_medicine": false, "confidence_score": 0.0}
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("model", cloudModelName.ifBlank { "qwen/qwen3.8-27b" })
                put("temperature", 0.0)
                put("response_format", JSONObject().put("type", "json_object"))
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Extract authentic pharmaceutical details from this scanned packaging OCR text:\n$text")
                    })
                }
                put("messages", messages)
            }

            val url = URL("https://api.groq.com/openai/v1/chat/completions")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 9000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${cloudMedGemmaApiKey.trim()}")
                setRequestProperty("User-Agent", "MedVoice-EdgeAI/1.0")
            }

            connection.outputStream.use { os ->
                val input = requestJson.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                return@withContext null
            }

            val groqResp = JSONObject(responseBody)
            val choices = groqResp.optJSONArray("choices")
            if (choices == null || choices.length() == 0) return@withContext null

            val message = choices.getJSONObject(0).optJSONObject("message")
            val content = message?.optString("content", "") ?: ""
            if (content.isBlank()) return@withContext null

            val cleanJsonStr = if (content.contains("{")) {
                content.substring(content.indexOf("{"), content.lastIndexOf("}") + 1)
            } else content

            val json = JSONObject(cleanJsonStr)
            val isMedicine = json.optBoolean("is_medicine", false)
            val confidence = json.optDouble("confidence_score", 0.0).toFloat()

            if (!isMedicine || confidence < 0.80f) return@withContext null

            val brand = json.optString("brand_name", "").trim()
            val saltsArray = json.optJSONArray("active_salts")
            val saltsList = mutableListOf<String>()
            if (saltsArray != null) {
                for (i in 0 until saltsArray.length()) {
                    val s = saltsArray.getString(i).trim()
                    if (s.isNotBlank()) saltsList.add(s)
                }
            }

            if (brand.isBlank() || saltsList.isEmpty()) return@withContext null

            val dosageForm = json.optString("dosage_form", "TABLET").uppercase(Locale.US)
            val route = json.optString("route_of_administration", "ORAL").uppercase(Locale.US)
            val instructions = generateVernacularGuidance(brand, dosageForm)

            ExtractedMedicineComposition(
                brandName = brand,
                activeSalts = saltsList,
                strengthMg = json.optDouble("strength_mg", 500.0),
                dosageForm = dosageForm,
                therapeuticCategory = json.optString("therapeutic_class", "PHARMACEUTICAL"),
                confidenceScore = confidence,
                sourceTier = AiEngineTier.CLOUD_MEDGEMMA_HOSTED,
                vernacularInstructionEn = instructions.first,
                vernacularInstructionHi = instructions.second,
                routeOfAdministration = route
            )
        } catch (e: Exception) {
            Log.w("AiPharmacologyEngine", "Groq Qwen 3.8 27B exception: ${e.message}")
            null
        }
    }

    /**
     * 100% On-Device Deterministic & Fuzzy Clinical Parser.
     * Incorporates FuzzySaltMatcher with Levenshtein tolerance.
     */
    fun runClinicalDeterministicParser(text: String): ExtractedMedicineComposition? {
        val rawLines = text.lines().map { it.trim() }.filter { it.length >= 2 }
        if (rawLines.isEmpty()) return null

        val upperText = text.uppercase(Locale.US)
        val detectedSalts = mutableListOf<String>()
        var primaryCategory = "GENERAL HEALTHCARE"
        var defaultStrength = 0.0
        var detectedForm = "TABLET"
        var route = "ORAL"

        // 1. Direct dictionary match
        for ((chemical, meta) in knownChemicalDictionary) {
            if (upperText.contains(chemical)) {
                detectedSalts.add(chemical)
                primaryCategory = meta.first
                if (meta.second > 0.0) defaultStrength = meta.second
                detectedForm = meta.third
            }
        }

        // 2. On-Device Fuzzy Salt Matcher (Levenshtein)
        val fuzzyMatches = FuzzySaltMatcher.extractAllSalts(text)
        for (m in fuzzyMatches) {
            if (!detectedSalts.contains(m.canonicalName)) {
                detectedSalts.add(m.canonicalName)
                primaryCategory = m.category
                if (m.defaultRoute == "TOPICAL") {
                    route = "EXTERNAL_TOPICAL"
                    if (detectedForm == "TABLET") detectedForm = "TOPICAL_LOTION"
                }
            }
        }

        // 3. Refine Dosage Form & Route from Keywords
        when {
            upperText.contains("DANDRUFF") || upperText.contains("HAIR TONIC") || upperText.contains("SCALP") -> {
                detectedForm = "TOPICAL_LOTION"
                route = "EXTERNAL_TOPICAL"
                primaryCategory = "ANTI-DANDRUFF SCALP CARE"
            }
            upperText.contains("SHAMPOO") -> {
                detectedForm = "SHAMPOO"
                route = "EXTERNAL_TOPICAL"
            }
            upperText.contains("LOTION") -> {
                detectedForm = "TOPICAL_LOTION"
                route = "EXTERNAL_TOPICAL"
            }
            upperText.contains("EYE DROP") || upperText.contains("OPHTHALMIC") -> {
                detectedForm = "EYE_DROPS"
                route = "OPHTHALMIC"
            }
            upperText.contains("EAR DROP") -> {
                detectedForm = "EAR_DROPS"
                route = "OTIC"
            }
            upperText.contains("NASAL") || upperText.contains("SPRAY") -> {
                detectedForm = "NASAL_SPRAY"
                route = "NASAL"
            }
            upperText.contains("SYRUP") || upperText.contains("SUSPENSION") -> {
                detectedForm = "SYRUP"
                route = "ORAL"
            }
            upperText.contains("TONIC") || upperText.contains("ELIXIR") -> {
                detectedForm = "TONIC"
                route = "ORAL"
            }
            upperText.contains("GEL") -> {
                detectedForm = "GEL"
                route = "EXTERNAL_TOPICAL"
            }
            upperText.contains("OINTMENT") || upperText.contains("CREAM") -> {
                detectedForm = "OINTMENT"
                route = "EXTERNAL_TOPICAL"
            }
            upperText.contains("INHALER") || upperText.contains("RESPICAPS") -> {
                detectedForm = "INHALER"
                route = "RESPIRATORY"
            }
            upperText.contains("CAPSULE") || upperText.contains("CAP") -> {
                detectedForm = "CAPSULE"
                route = "ORAL"
            }
        }

        if (detectedSalts.isEmpty() && !containsPharmaceuticalMarkers(text)) {
            return null
        }

        // 4. Extract Clean Brand Name
        val cleanCandidateLines = rawLines.map { line ->
            var cleaned = line
            for (noise in noisePatterns) {
                cleaned = noise.replace(cleaned, "").trim()
            }
            cleaned
        }.filter { it.length >= 3 && !it.startsWith("₹") && !it.matches(Regex("""^\d+$""")) }

        val firstCleanLine = cleanCandidateLines.firstOrNull() ?: rawLines.first()
        val brandWords = firstCleanLine.split(Regex("""[\s,/\-]+""")).filter { it.length >= 2 }
        val brandName = brandWords.take(4).joinToString(" ")

        val finalBrand = if (brandName.isNotBlank() && brandName != "Composition") {
            brandName
        } else (detectedSalts.firstOrNull() ?: "Scanned Medicine")

        val instructions = generateVernacularGuidance(finalBrand, detectedForm)

        return ExtractedMedicineComposition(
            brandName = finalBrand,
            activeSalts = if (detectedSalts.isNotEmpty()) detectedSalts else listOf(finalBrand),
            strengthMg = defaultStrength,
            dosageForm = detectedForm,
            therapeuticCategory = primaryCategory,
            confidenceScore = if (detectedSalts.isNotEmpty()) 0.95f else 0.82f,
            sourceTier = AiEngineTier.OFFLINE_REGEX_DETERMINISTIC,
            vernacularInstructionEn = instructions.first,
            vernacularInstructionHi = instructions.second,
            routeOfAdministration = route
        )
    }

    fun generateVernacularGuidance(brand: String, dosageForm: String): Pair<String, String> {
        return when (dosageForm) {
            "TOPICAL_LOTION", "SHAMPOO", "SCALP_SOLUTION" -> Pair(
                "This is a topical hair/scalp lotion ($brand). Apply gently to the scalp. For external application only. Do not swallow or drink.",
                "यह सिर और त्वचा पर लगाने की दवा ($brand) है। सिर पर हल्के हाथों से लगाएँ। यह केवल बाहरी उपयोग के लिए है, इसे पिएँ नहीं।"
            )
            "EYE_DROPS", "DROPS" -> Pair(
                "This is an ophthalmic eye drop ($brand). Instill 1 to 2 drops into the eye as prescribed.",
                "यह आँखों की दवाई (आई ड्रॉप्स: $brand) है। डॉक्टर के निर्देशानुसार आँखों में 1 से 2 बूँद डालें।"
            )
            "EAR_DROPS" -> Pair(
                "This is an ear drop ($brand). Instill 2 to 3 drops into the ear canal.",
                "यह कान की दवाई ($brand) है। कान में 2 से 3 बूँद डालें।"
            )
            "NASAL_SPRAY" -> Pair(
                "This is a nasal spray ($brand). Spray 1 to 2 puffs into each nostril as needed.",
                "यह नेजल स्प्रे ($brand) है। प्रत्येक नथुने में 1 से 2 स्प्रे करें।"
            )
            "SYRUP", "TONIC" -> Pair(
                "This is an oral syrup or health tonic ($brand). Shake well and take measured 5ml to 10ml after food.",
                "यह पीने का सिरप/टॉनिक ($brand) है। बोतल हिलाकर नाप के 5ml से 10ml भोजन के बाद लें।"
            )
            "OINTMENT", "GEL" -> Pair(
                "This is a topical pain relief gel/ointment ($brand). Apply a thin layer to the affected area. For external use only.",
                "यह लगाने की मलहम/जेल ($brand) है। प्रभावित स्थान पर हल्के हाथों से लगाएँ। केवल बाहरी उपयोग के लिए।"
            )
            "INHALER" -> Pair(
                "This is a respiratory inhaler ($brand). Inhale deeply for 1 to 2 puffs as directed.",
                "यह इनहेलर ($brand) है। गहरी साँस लेते हुए 1 से 2 पफ लें।"
            )
            "CAPSULE" -> Pair(
                "This is an oral capsule ($brand). Swallow whole with water after meals.",
                "यह कैप्सूल ($brand) है। खाना खाने के बाद पानी के साथ पूरा निगल लें।"
            )
            else -> Pair(
                "This is your prescribed medicine ($brand). Take with water after your meal.",
                "यह आपकी दवा ($brand) है। खाना खाने के बाद पानी के साथ लें।"
            )
        }
    }
}
