package com.medvoice.core.domain.engine

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedExpiryInfo(
    val rawText: String,
    val expiryDateString: String?,
    val isExpired: Boolean,
    val batchNumber: String?
)

object ExpiryParser {

    private val EXPIRY_PATTERNS = listOf(
        Pattern.compile("""\b(?:EXP|EXPIRY|EXP\.|USE\s*BEFORE|BEST\s*BEFORE)\s*[:.\-\/]?\s*([0-1]?[0-9][\/\-\.][2-9][0-9](?:[0-9]{2})?)\b""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\b([0-1]?[0-9][\/\-\.][2-9][0-9](?:[0-9]{2})?)\s*(?:EXP|EXPIRY)\b""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\b(?:EXP|EXPIRY)\s*[:.\-\/]?\s*([A-Z]{3,4}[\/\-\.][2-9][0-9](?:[0-9]{2})?)\b""", Pattern.CASE_INSENSITIVE)
    )

    private val BATCH_PATTERNS = listOf(
        Pattern.compile("""\b(?:B\.?\s*NO\.?|BATCH|LOT|B\/N)\s*[:.\-\/]?\s*([A-Z0-9\-]+)\b""", Pattern.CASE_INSENSITIVE)
    )

    fun parse(text: String): ParsedExpiryInfo {
        var detectedExpiry: String? = null
        var isExpired = false
        var batchNumber: String? = null

        for (pattern in EXPIRY_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                detectedExpiry = matcher.group(1)
                break
            }
        }

        for (pattern in BATCH_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                batchNumber = matcher.group(1)
                break
            }
        }

        if (detectedExpiry != null) {
            isExpired = checkIfExpired(detectedExpiry)
        }

        return ParsedExpiryInfo(
            rawText = text,
            expiryDateString = detectedExpiry,
            isExpired = isExpired,
            batchNumber = batchNumber
        )
    }

    private fun checkIfExpired(dateStr: String): Boolean {
        val formats = listOf("MM/yy", "MM/yyyy", "MM-yy", "MM-yyyy", "MM.yy", "MM.yyyy", "MMM/yy", "MMM/yyyy")
        val now = Date()

        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                sdf.isLenient = false
                val parsed = sdf.parse(dateStr)
                if (parsed != null) {
                    return parsed.before(now)
                }
            } catch (_: Exception) {
                // Try next format
            }
        }
        return false
    }
}
