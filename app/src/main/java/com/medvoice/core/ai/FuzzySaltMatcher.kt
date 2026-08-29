package com.medvoice.core.ai

import java.util.Locale
import kotlin.math.min

/**
 * 100% On-Device Deterministic Levenshtein & Fuzzy Salt Matcher.
 * Matches noisy, curved, or OCR-scrambled packaging strings to canonical pharmaceutical compounds.
 */
object FuzzySaltMatcher {

    data class MatchedCompound(
        val canonicalName: String,
        val category: String, // ALLOPATHIC, TOPICAL_DERMATOLOGY, HOMEOPATHIC_AYURVEDIC
        val defaultRoute: String // ORAL, TOPICAL, OPHTHALMIC, NASAL
    )

    private val masterLexicon: Map<String, MatchedCompound> = mapOf(
        // Common Allopathic Oral Medicines
        "PARACETAMOL" to MatchedCompound("Paracetamol", "ANALGESIC_ANTIPYRETIC", "ORAL"),
        "ACETAMINOPHEN" to MatchedCompound("Paracetamol", "ANALGESIC_ANTIPYRETIC", "ORAL"),
        "IBUPROFEN" to MatchedCompound("Ibuprofen", "NSAID_ANALGESIC", "ORAL"),
        "ACECLOFENAC" to MatchedCompound("Aceclofenac", "NSAID_ANALGESIC", "ORAL"),
        "DICLOFENAC" to MatchedCompound("Diclofenac", "NSAID_ANALGESIC", "ORAL"),
        "ASPIRIN" to MatchedCompound("Aspirin", "ANTIPLATELET", "ORAL"),
        "METFORMIN" to MatchedCompound("Metformin", "ANTIDIABETIC", "ORAL"),
        "GLIMEPIRIDE" to MatchedCompound("Glimepiride", "ANTIDIABETIC", "ORAL"),
        "VILDAGLIPTIN" to MatchedCompound("Vildagliptin", "ANTIDIABETIC", "ORAL"),
        "SITAGLIPTIN" to MatchedCompound("Sitagliptin", "ANTIDIABETIC", "ORAL"),
        "DAPAGLIFLOZIN" to MatchedCompound("Dapagliflozin", "ANTIDIABETIC", "ORAL"),
        "EMPAGLIFLOZIN" to MatchedCompound("Empagliflozin", "ANTIDIABETIC", "ORAL"),
        "TELMISARTAN" to MatchedCompound("Telmisartan", "ANTIHYPERTENSIVE", "ORAL"),
        "AMLODIPINE" to MatchedCompound("Amlodipine", "ANTIHYPERTENSIVE", "ORAL"),
        "LOSARTAN" to MatchedCompound("Losartan", "ANTIHYPERTENSIVE", "ORAL"),
        "ATORVASTATIN" to MatchedCompound("Atorvastatin", "LIPID_LOWERING", "ORAL"),
        "ROSUVASTATIN" to MatchedCompound("Rosuvastatin", "LIPID_LOWERING", "ORAL"),
        "PANTOPRAZOLE" to MatchedCompound("Pantoprazole", "PPI_ANTACID", "ORAL"),
        "RABEPRAZOLE" to MatchedCompound("Rabeprazole", "PPI_ANTACID", "ORAL"),
        "OMEPRAZOLE" to MatchedCompound("Omeprazole", "PPI_ANTACID", "ORAL"),
        "LEVOTHYROXINE" to MatchedCompound("Levothyroxine", "THYROID_HORMONE", "ORAL"),
        "AZITHROMYCIN" to MatchedCompound("Azithromycin", "ANTIBIOTIC", "ORAL"),
        "AMOXICILLIN" to MatchedCompound("Amoxicillin", "ANTIBIOTIC", "ORAL"),
        "CEFIXIME" to MatchedCompound("Cefixime", "ANTIBIOTIC", "ORAL"),
        "CIPROFLOXACIN" to MatchedCompound("Ciprofloxacin", "ANTIBIOTIC", "ORAL"),
        "OFLOXACIN" to MatchedCompound("Ofloxacin", "ANTIBIOTIC", "ORAL"),
        "LEVOFLOXACIN" to MatchedCompound("Levofloxacin", "ANTIBIOTIC", "ORAL"),
        "CETIRIZINE" to MatchedCompound("Cetirizine", "ANTIHISTAMINE", "ORAL"),
        "LEVOCETIRIZINE" to MatchedCompound("Levocetirizine", "ANTIHISTAMINE", "ORAL"),
        "MONTELUKAST" to MatchedCompound("Montelukast", "ANTIASTHMATIC", "ORAL"),
        "DOMPERIDONE" to MatchedCompound("Domperidone", "ANTIEMETIC", "ORAL"),
        "DEXTROMETHORPHAN" to MatchedCompound("Dextromethorphan", "COUGH_SUPPRESSANT", "ORAL"),
        "GUAIFENESIN" to MatchedCompound("Guaifenesin", "EXPECTORANT", "ORAL"),
        "AMBROXOL" to MatchedCompound("Ambroxol", "MUCOLYTIC", "ORAL"),
        "SUCRALFATE" to MatchedCompound("Sucralfate", "ANTIULCER", "ORAL"),
        "MAGALDRATE" to MatchedCompound("Magaldrate", "ANTACID", "ORAL"),
        "SIMETHICONE" to MatchedCompound("Simethicone", "ANTIFLATULENT", "ORAL"),
        "LACTULOSE" to MatchedCompound("Lactulose", "LAXATIVE", "ORAL"),
        "XYLOMETAZOLINE" to MatchedCompound("Xylometazoline", "NASAL_DECONGESTANT", "NASAL"),
        "SALBUTAMOL" to MatchedCompound("Salbutamol", "BRONCHODILATOR", "RESPIRATORY"),
        "BUDESONIDE" to MatchedCompound("Budesonide", "CORTICOSTEROID", "RESPIRATORY"),

        // Topical, Hair & Dermatology Formulations
        "KETOCONAZOLE" to MatchedCompound("Ketoconazole", "ANTIFUNGAL_TOPICAL", "TOPICAL"),
        "CLOTRIMAZOLE" to MatchedCompound("Clotrimazole", "ANTIFUNGAL_TOPICAL", "TOPICAL"),
        "MINOXIDIL" to MatchedCompound("Minoxidil", "HAIR_REGROWTH_TOPICAL", "TOPICAL"),
        "SALICYLIC ACID" to MatchedCompound("Salicylic Acid", "KERATOLYTIC_TOPICAL", "TOPICAL"),
        "ZINC PYRITHIONE" to MatchedCompound("Zinc Pyrithione", "ANTIDANDRUFF_TOPICAL", "TOPICAL"),
        "COAL TAR" to MatchedCompound("Coal Tar", "ANTIDANDRUFF_TOPICAL", "TOPICAL"),
        "PERMETHRIN" to MatchedCompound("Permethrin", "ANTIPARASITIC_TOPICAL", "TOPICAL"),
        "CLOBETASOL" to MatchedCompound("Clobetasol Propionate", "STEROID_TOPICAL", "TOPICAL"),
        "BETAMETHASONE" to MatchedCompound("Betamethasone", "STEROID_TOPICAL", "TOPICAL"),
        "MUPIROCIN" to MatchedCompound("Mupirocin", "ANTIBIOTIC_TOPICAL", "TOPICAL"),
        "POVIDONE IODINE" to MatchedCompound("Povidone Iodine", "ANTISEPTIC_TOPICAL", "TOPICAL"),
        "BENZOYL PEROXIDE" to MatchedCompound("Benzoyl Peroxide", "ANTIACNE_TOPICAL", "TOPICAL"),
        "CALAMINE" to MatchedCompound("Calamine", "SOOTHING_TOPICAL", "TOPICAL"),

        // Homeopathic & Herbal Compounds (e.g. Bakson Dandruff Aid, Eye Drops, Arnica)
        "THUJA OCCIDENTALIS" to MatchedCompound("Thuja Occidentalis", "HOMEOPATHIC_SCALP", "TOPICAL"),
        "THUJA" to MatchedCompound("Thuja Occidentalis", "HOMEOPATHIC_SCALP", "TOPICAL"),
        "CANTHARIS" to MatchedCompound("Cantharis", "HOMEOPATHIC_SCALP", "TOPICAL"),
        "COCHLEARIA ARMORACIA" to MatchedCompound("Cochlearia Armoracia", "HOMEOPATHIC_SCALP", "TOPICAL"),
        "COCHLEARIA" to MatchedCompound("Cochlearia Armoracia", "HOMEOPATHIC_SCALP", "TOPICAL"),
        "ARNICA MONTANA" to MatchedCompound("Arnica Montana", "HOMEOPATHIC_PAIN_HAIR", "TOPICAL"),
        "ARNICA" to MatchedCompound("Arnica Montana", "HOMEOPATHIC_PAIN_HAIR", "TOPICAL"),
        "CALENDULA OFFICINALIS" to MatchedCompound("Calendula Officinalis", "HOMEOPATHIC_HEALING", "TOPICAL"),
        "CALENDULA" to MatchedCompound("Calendula Officinalis", "HOMEOPATHIC_HEALING", "TOPICAL"),
        "CINERARIA MARITIMA" to MatchedCompound("Cineraria Maritima", "HOMEOPATHIC_OPHTHALMIC", "OPHTHALMIC"),
        "CINERARIA" to MatchedCompound("Cineraria Maritima", "HOMEOPATHIC_OPHTHALMIC", "OPHTHALMIC"),
        "EUPHRASIA" to MatchedCompound("Euphrasia Officinalis", "HOMEOPATHIC_OPHTHALMIC", "OPHTHALMIC"),
        "BERBERIS AQUIFOLIUM" to MatchedCompound("Berberis Aquifolium", "HOMEOPATHIC_SKIN", "TOPICAL"),
        "BERBERIS" to MatchedCompound("Berberis Aquifolium", "HOMEOPATHIC_SKIN", "TOPICAL")
    )

    /**
     * Finds the closest matching compound in the master lexicon with Levenshtein distance tolerance.
     */
    fun findBestMatch(rawToken: String): MatchedCompound? {
        val cleanToken = rawToken.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9 ]"), " ").trim()
        if (cleanToken.length < 3) return null

        // 1. Direct contains check (e.g. text contains "METFORMIN" or "THUJA")
        for ((key, compound) in masterLexicon) {
            if (cleanToken == key || cleanToken.contains(key)) {
                return compound
            }
        }

        // 2. Token-by-token fuzzy check
        val words = cleanToken.split(" ").filter { it.length >= 4 }
        for (word in words) {
            for ((key, compound) in masterLexicon) {
                val keyWords = key.split(" ")
                for (kw in keyWords) {
                    if (kw.length >= 4) {
                        val dist = levenshteinDistance(word, kw)
                        val maxAllowedDist = when {
                            kw.length <= 4 -> 1
                            kw.length <= 7 -> 2
                            else -> 3
                        }
                        if (dist <= maxAllowedDist) {
                            return compound
                        }
                    }
                }
            }
        }

        return null
    }

    /**
     * Extracts all recognized pharmaceutical salts from a multi-line OCR text string.
     */
    fun extractAllSalts(text: String): List<MatchedCompound> {
        val results = mutableListOf<MatchedCompound>()
        val seenNames = mutableSetOf<String>()
        val lines = text.lines()

        for (line in lines) {
            val clean = line.trim()
            if (clean.length < 3) continue
            val match = findBestMatch(clean)
            if (match != null && seenNames.add(match.canonicalName)) {
                results.add(match)
            }
        }

        return results
    }

    /**
     * Computes classical Levenshtein Edit Distance.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,      // Deletion
                    min(
                        dp[i][j - 1] + 1,  // Insertion
                        dp[i - 1][j - 1] + cost // Substitution
                    )
                )
            }
        }
        return dp[m][n]
    }
}
