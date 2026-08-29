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
        val category: String // ALLOPATHIC, TOPICAL_DERMATOLOGY, HOMEOPATHIC_AYURVEDIC
    )

    private val masterLexicon: Map<String, MatchedCompound> = mapOf(
        // Common Allopathic Oral Medicines
        "PARACETAMOL" to MatchedCompound("Paracetamol", "ANALGESIC_ANTIPYRETIC"),
        "ACETAMINOPHEN" to MatchedCompound("Paracetamol", "ANALGESIC_ANTIPYRETIC"),
        "IBUPROFEN" to MatchedCompound("Ibuprofen", "NSAID_ANALGESIC"),
        "ACECLOFENAC" to MatchedCompound("Aceclofenac", "NSAID_ANALGESIC"),
        "DICLOFENAC" to MatchedCompound("Diclofenac", "NSAID_ANALGESIC"),
        "ASPIRIN" to MatchedCompound("Aspirin", "ANTIPLATELET"),
        "METFORMIN" to MatchedCompound("Metformin", "ANTIDIABETIC"),
        "GLIMEPIRIDE" to MatchedCompound("Glimepiride", "ANTIDIABETIC"),
        "VILDAGLIPTIN" to MatchedCompound("Vildagliptin", "ANTIDIABETIC"),
        "SITAGLIPTIN" to MatchedCompound("Sitagliptin", "ANTIDIABETIC"),
        "DAPAGLIFLOZIN" to MatchedCompound("Dapagliflozin", "ANTIDIABETIC"),
        "EMPAGLIFLOZIN" to MatchedCompound("Empagliflozin", "ANTIDIABETIC"),
        "TELMISARTAN" to MatchedCompound("Telmisartan", "ANTIHYPERTENSIVE"),
        "AMLODIPINE" to MatchedCompound("Amlodipine", "ANTIHYPERTENSIVE"),
        "LOSARTAN" to MatchedCompound("Losartan", "ANTIHYPERTENSIVE"),
        "ATORVASTATIN" to MatchedCompound("Atorvastatin", "LIPID_LOWERING"),
        "ROSUVASTATIN" to MatchedCompound("Rosuvastatin", "LIPID_LOWERING"),
        "PANTOPRAZOLE" to MatchedCompound("Pantoprazole", "PPI_ANTACID"),
        "RABEPRAZOLE" to MatchedCompound("Rabeprazole", "PPI_ANTACID"),
        "OMEPRAZOLE" to MatchedCompound("Omeprazole", "PPI_ANTACID"),
        "LEVOTHYROXINE" to MatchedCompound("Levothyroxine", "THYROID_HORMONE"),
        "AZITHROMYCIN" to MatchedCompound("Azithromycin", "ANTIBIOTIC"),
        "AMOXICILLIN" to MatchedCompound("Amoxicillin", "ANTIBIOTIC"),
        "CEFIXIME" to MatchedCompound("Cefixime", "ANTIBIOTIC"),
        "CIPROFLOXACIN" to MatchedCompound("Ciprofloxacin", "ANTIBIOTIC"),
        "OFLOXACIN" to MatchedCompound("Ofloxacin", "ANTIBIOTIC"),
        "LEVOFLOXACIN" to MatchedCompound("Levofloxacin", "ANTIBIOTIC"),
        "CETIRIZINE" to MatchedCompound("Cetirizine", "ANTIHISTAMINE"),
        "LEVOCETIRIZINE" to MatchedCompound("Levocetirizine", "ANTIHISTAMINE"),
        "MONTELUKAST" to MatchedCompound("Montelukast", "ANTIASTHMATIC"),
        "DOMPERIDONE" to MatchedCompound("Domperidone", "ANTIEMETIC"),
        "DEXTROMETHORPHAN" to MatchedCompound("Dextromethorphan", "COUGH_SUPPRESSANT"),
        "GUAIFENESIN" to MatchedCompound("Guaifenesin", "EXPECTORANT"),
        "AMBROXOL" to MatchedCompound("Ambroxol", "MUCOLYTIC"),
        "SUCRALFATE" to MatchedCompound("Sucralfate", "ANTIULCER"),
        "MAGALDRATE" to MatchedCompound("Magaldrate", "ANTACID"),
        "SIMETHICONE" to MatchedCompound("Simethicone", "ANTIFLATULENT"),
        "LACTULOSE" to MatchedCompound("Lactulose", "LAXATIVE"),
        "XYLOMETAZOLINE" to MatchedCompound("Xylometazoline", "NASAL_DECONGESTANT"),
        "SALBUTAMOL" to MatchedCompound("Salbutamol", "BRONCHODILATOR"),
        "BUDESONIDE" to MatchedCompound("Budesonide", "CORTICOSTEROID"),
        "CLAVULANIC ACID" to MatchedCompound("Clavulanic Acid", "BETA_LACTAMASE_INHIBITOR"),
        "CLAVULANATE" to MatchedCompound("Clavulanic Acid", "BETA_LACTAMASE_INHIBITOR"),
        "FEXOFENADINE" to MatchedCompound("Fexofenadine", "ANTIHISTAMINE"),
        "BILASTINE" to MatchedCompound("Bilastine", "ANTIHISTAMINE"),
        "CHOLECALCIFEROL" to MatchedCompound("Cholecalciferol (Vitamin D3)", "VITAMIN_SUPPLEMENT"),
        "CALCIUM" to MatchedCompound("Calcium Carbonate", "MINERAL_SUPPLEMENT"),
        "CLOPIDOGREL" to MatchedCompound("Clopidogrel", "ANTIPLATELET"),
        "RAMIPRIL" to MatchedCompound("Ramipril", "ANTIHYPERTENSIVE"),
        "METOPROLOL" to MatchedCompound("Metoprolol", "ANTIHYPERTENSIVE"),
        "PROPRANOLOL" to MatchedCompound("Propranolol", "ANTIHYPERTENSIVE"),
        "FLUCONAZOLE" to MatchedCompound("Fluconazole", "ANTIFUNGAL"),
        "ITRACONAZOLE" to MatchedCompound("Itraconazole", "ANTIFUNGAL"),
        "TRAMADOL" to MatchedCompound("Tramadol", "OPIOID_ANALGESIC"),
        "PREGABALIN" to MatchedCompound("Pregabalin", "NEUROPATHIC_ANALGESIC"),
        "GABAPENTIN" to MatchedCompound("Gabapentin", "NEUROPATHIC_ANALGESIC"),
        "ALPRAZOLAM" to MatchedCompound("Alprazolam", "ANXIOLYTIC"),
        "CLONAZEPAM" to MatchedCompound("Clonazepam", "ANXIOLYTIC"),
        "METHYLCOBALAMIN" to MatchedCompound("Methylcobalamin (Vitamin B12)", "VITAMIN_SUPPLEMENT"),
        "CYANOCOBALAMIN" to MatchedCompound("Vitamin B12", "VITAMIN_SUPPLEMENT"),
        "FOLIC ACID" to MatchedCompound("Folic Acid (Vitamin B9)", "VITAMIN_SUPPLEMENT"),

        // Dermatology, Antifungal & Wound Care Compounds
        "KETOCONAZOLE" to MatchedCompound("Ketoconazole", "ANTIFUNGAL"),
        "CLOTRIMAZOLE" to MatchedCompound("Clotrimazole", "ANTIFUNGAL"),
        "MINOXIDIL" to MatchedCompound("Minoxidil", "VASODILATOR_HAIR"),
        "SALICYLIC ACID" to MatchedCompound("Salicylic Acid", "KERATOLYTIC"),
        "ZINC PYRITHIONE" to MatchedCompound("Zinc Pyrithione", "ANTISEBORRHEIC"),
        "COAL TAR" to MatchedCompound("Coal Tar", "ANTISEBORRHEIC"),
        "PERMETHRIN" to MatchedCompound("Permethrin", "ANTIPARASITIC"),
        "CLOBETASOL" to MatchedCompound("Clobetasol Propionate", "CORTICOSTEROID"),
        "BETAMETHASONE" to MatchedCompound("Betamethasone", "CORTICOSTEROID"),
        "MUPIROCIN" to MatchedCompound("Mupirocin", "ANTIBIOTIC"),
        "POVIDONE IODINE" to MatchedCompound("Povidone Iodine", "ANTISEPTIC"),
        "BENZOYL PEROXIDE" to MatchedCompound("Benzoyl Peroxide", "ANTIACNE"),
        "CALAMINE" to MatchedCompound("Calamine", "SOOTHING_PROTECTIVE"),

        // Homeopathic & Herbal Active Molecules
        "THUJA OCCIDENTALIS" to MatchedCompound("Thuja Occidentalis", "HOMEOPATHIC_BOTANICAL"),
        "THUJA" to MatchedCompound("Thuja Occidentalis", "HOMEOPATHIC_BOTANICAL"),
        "NATRUM MURIATICUM" to MatchedCompound("Natrum Muriaticum", "HOMEOPATHIC_MINERAL"),
        "CANTHARIS" to MatchedCompound("Cantharis", "HOMEOPATHIC_BOTANICAL"),
        "COCHLEARIA ARMORACIA" to MatchedCompound("Cochlearia Armoracia", "HOMEOPATHIC_BOTANICAL"),
        "COCHLEARIA" to MatchedCompound("Cochlearia Armoracia", "HOMEOPATHIC_BOTANICAL"),
        "ARNICA MONTANA" to MatchedCompound("Arnica Montana", "HOMEOPATHIC_BOTANICAL"),
        "ARNICA" to MatchedCompound("Arnica Montana", "HOMEOPATHIC_BOTANICAL"),
        "CALENDULA OFFICINALIS" to MatchedCompound("Calendula Officinalis", "HOMEOPATHIC_BOTANICAL"),
        "CALENDULA" to MatchedCompound("Calendula Officinalis", "HOMEOPATHIC_BOTANICAL"),
        "CINERARIA MARITIMA" to MatchedCompound("Cineraria Maritima", "HOMEOPATHIC_BOTANICAL"),
        "EUPHRASIA" to MatchedCompound("Euphrasia Officinalis", "HOMEOPATHIC_BOTANICAL"),
        "BERBERIS AQUIFOLIUM" to MatchedCompound("Berberis Aquifolium", "HOMEOPATHIC_BOTANICAL"),
        "BERBERIS" to MatchedCompound("Berberis Aquifolium", "HOMEOPATHIC_BOTANICAL"),

        // Ayurvedic & Herbal Active Formulations
        "LIV 52" to MatchedCompound("Liv 52 Herbal Formulation", "AYURVEDIC_BOTANICAL"),
        "LIV-52" to MatchedCompound("Liv 52 Herbal Formulation", "AYURVEDIC_BOTANICAL"),
        "LIV52" to MatchedCompound("Liv 52 Herbal Formulation", "AYURVEDIC_BOTANICAL"),
        "HERBAL" to MatchedCompound("Herbal Active Compound", "AYURVEDIC_BOTANICAL"),
        "ASHWAGANDHA" to MatchedCompound("Ashwagandha", "AYURVEDIC_BOTANICAL"),
        "TULSI" to MatchedCompound("Tulsi", "AYURVEDIC_BOTANICAL"),
        "NEEM" to MatchedCompound("Neem Extract", "AYURVEDIC_BOTANICAL"),
        "TRIPHALA" to MatchedCompound("Triphala", "AYURVEDIC_BOTANICAL")
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
