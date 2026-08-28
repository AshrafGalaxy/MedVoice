package com.medvoice.core.domain.engine

import kotlin.math.min

object FuzzyMedicineMatcher {

    /**
     * Calculates the Levenshtein edit distance between two strings.
     */
    fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val a = s1.trim().lowercase()
        val b = s2.trim().lowercase()

        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val dp = Array(a.length + 1) { IntArray(b.length + 1) }

        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }

    /**
     * Returns a normalized similarity ratio from 0.0 (completely different) to 1.0 (exact match).
     */
    fun calculateSimilarity(s1: String, s2: String): Double {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val distance = calculateLevenshteinDistance(s1, s2)
        return (maxLen - distance).toDouble() / maxLen
    }

    /**
     * Checks if a candidate token matches a target brand with at least [threshold] similarity.
     */
    fun isFuzzyMatch(candidate: String, targetBrand: String, threshold: Double = 0.70): Boolean {
        if (candidate.isBlank() || targetBrand.isBlank()) return false
        val cleanCandidate = candidate.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
        val cleanTarget = targetBrand.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()

        if (cleanCandidate.contains(cleanTarget) || cleanTarget.contains(cleanCandidate)) {
            return true
        }

        val targetAlphaOnly = targetBrand.replace("[^a-zA-Z]".toRegex(), "").lowercase()
        val candidateAlphaOnly = candidate.replace("[^a-zA-Z]".toRegex(), "").lowercase()

        if (candidateAlphaOnly.isNotEmpty() && targetAlphaOnly.isNotEmpty()) {
            if (calculateSimilarity(candidateAlphaOnly, targetAlphaOnly) >= threshold) {
                return true
            }
        }

        return calculateSimilarity(cleanCandidate, cleanTarget) >= threshold
    }
}
