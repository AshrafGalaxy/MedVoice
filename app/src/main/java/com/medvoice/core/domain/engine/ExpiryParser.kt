package com.medvoice.core.domain.engine

import java.text.SimpleDateFormat
import java.util.Calendar
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

    // 1. Direct Expiry Patterns (Explicitly labeled as EXP / EXPIRY / USE BEFORE)
    private val DIRECT_EXPIRY_PATTERNS = listOf(
        Pattern.compile("""\b(?:EXP\s*DATE|EXPIRY\s*DATE|USE\s*BEFORE\s*DATE|BEST\s*BEFORE\s*DATE|EXP|EXPIRY|EXP\.|USE\s*BEFORE|BEST\s*BEFORE)\s*(?:DATE)?\s*[:.\-\/]?\s*([0-1]?[0-9][\/\-\.][2-9][0-9](?:[0-9]{2})?)\b""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\b(?:EXP\s*DATE|EXPIRY\s*DATE|EXP|EXPIRY|EXP\.)\s*(?:DATE)?\s*[:.\-\/]?\s*([A-Za-z]{3,4}[\/\-\.][2-9][0-9](?:[0-9]{2})?)\b""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\b([0-1]?[0-9][\/\-\.][2-9][0-9](?:[0-9]{2})?)\s*(?:EXP|EXPIRY)\b""", Pattern.CASE_INSENSITIVE)
    )

    // 2. Manufacturing / Packaging Date Patterns (MFG / MFD / PKD)
    private val MFG_PATTERNS = listOf(
        Pattern.compile("""\b(?:MFG\s*DATE|MFD\s*DATE|PKD\s*DATE|MFG|MFD|PKD|PACKED|MANUFACTURED)\s*(?:DATE)?\s*[:.\-\/]?\s*([0-1]?[0-9][\/\-\.][2-9][0-9](?:[0-9]{2})?)\b""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\b(?:MFG\s*DATE|MFD\s*DATE|PKD\s*DATE|MFG|MFD|PKD)\s*(?:DATE)?\s*[:.\-\/]?\s*([A-Za-z]{3,4}[\/\-\.][2-9][0-9](?:[0-9]{2})?)\b""", Pattern.CASE_INSENSITIVE)
    )

    // 3. Relative Shelf-Life Patterns (e.g., "36 MONTHS FROM MFG", "24 MONTHS FROM PKD", "2 YEARS FROM DATE OF MFG")
    private val RELATIVE_SHELF_LIFE_PATTERNS = listOf(
        Pattern.compile("""\b(?:BEST\s*BEFORE|USE\s*WITHIN|EXPIRY|SHELF\s*LIFE|EXP)\s*(?:IS|:)?\s*(\d{1,2})\s*(?:MONTHS?|MTHS?)\s*(?:FROM|OF|AFTER)?\s*(?:DATE\s*OF\s*)?(?:MFG|MFD|PKD|PACKAGING|DATE)?\b""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\b(\d{1,2})\s*(?:MONTHS?|MTHS?)\s*(?:FROM|OF|AFTER)\s*(?:DATE\s*OF\s*)?(?:MFG|MFD|PKD|DATE|PACKAGING)\b""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\b(?:BEST\s*BEFORE|USE\s*WITHIN|EXPIRY)\s*(?:IS|:)?\s*(\d{1,2})\s*(?:YEARS?|YRS?)\s*(?:FROM|OF|AFTER)?\s*(?:DATE\s*OF\s*)?(?:MFG|MFD|PKD|DATE)?\b""", Pattern.CASE_INSENSITIVE)
    )

    // 4. Batch / Lot Number Patterns
    private val BATCH_PATTERNS = listOf(
        Pattern.compile("""\b(?:BATCH\s*NO\.?|B\.?\s*NO\.?|BATCH|LOT|B\/N)\s*[:.\-\/]?\s*([A-Za-z0-9\-]+)\b""", Pattern.CASE_INSENSITIVE)
    )

    fun parse(text: String): ParsedExpiryInfo {
        var detectedExpiry: String? = null
        var isExpired = false
        var batchNumber: String? = null

        // Step A: Parse Batch Number
        for (pattern in BATCH_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                batchNumber = matcher.group(1)?.trim()
                break
            }
        }

        // Step B: Check for Direct Explicit Expiry Date
        for (pattern in DIRECT_EXPIRY_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank()) {
                    detectedExpiry = candidate
                    break
                }
            }
        }

        // Step C: Check for Manufacturing Date
        var detectedMfg: String? = null
        for (pattern in MFG_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank()) {
                    detectedMfg = candidate
                    break
                }
            }
        }

        // Step D: Check for Relative Shelf Life (e.g., 36 months from MFG)
        var relativeMonths: Int? = null
        for (pattern in RELATIVE_SHELF_LIFE_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val numStr = matcher.group(1)
                val fullMatch = matcher.group(0)?.lowercase(Locale.ROOT) ?: ""
                val num = numStr?.toIntOrNull()
                if (num != null) {
                    relativeMonths = if (fullMatch.contains("year") || fullMatch.contains("yr")) num * 12 else num
                    break
                }
            }
        }

        // Step E: Compute Dynamic Expiry if Relative Shelf Life + MFG is present
        if (detectedMfg != null && relativeMonths != null) {
            val computed = computeExpiryFromMfg(detectedMfg, relativeMonths)
            if (computed != null) {
                detectedExpiry = computed.first
                isExpired = computed.second
                return ParsedExpiryInfo(
                    rawText = text,
                    expiryDateString = detectedExpiry,
                    isExpired = isExpired,
                    batchNumber = batchNumber
                )
            }
        }

        // Step F: Validate Direct Expiry Date
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

    private fun computeExpiryFromMfg(mfgStr: String, validityMonths: Int): Pair<String, Boolean>? {
        val parsedDate = parseAnyDate(mfgStr) ?: return null
        val calendar = Calendar.getInstance().apply {
            time = parsedDate
            add(Calendar.MONTH, validityMonths)
            // End of the target expiration month
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        val outSdf = SimpleDateFormat("MM/yyyy", Locale.ENGLISH)
        val formattedExpiry = outSdf.format(calendar.time)
        val isExpired = calendar.time.before(Date())
        return Pair(formattedExpiry, isExpired)
    }

    private fun parseAnyDate(dateStr: String): Date? {
        val formats = listOf(
            "MM/yy", "MM/yyyy", "MM-yy", "MM-yyyy", "MM.yy", "MM.yyyy",
            "MMM/yy", "MMM/yyyy", "MMM-yy", "MMM-yyyy", "MMM.yy", "MMM.yyyy",
            "MMM yyyy", "MMM yy"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                sdf.isLenient = false
                val parsed = sdf.parse(dateStr)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    private fun checkIfExpired(dateStr: String): Boolean {
        val parsed = parseAnyDate(dateStr) ?: return false
        val calendar = Calendar.getInstance().apply {
            time = parsed
            // End of the expiration month is the true cutoff
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        return calendar.time.before(Date())
    }
}
