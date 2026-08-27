package com.medvoice

import com.medvoice.core.domain.engine.ExpiryParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpiryParserTest {

    @Test
    fun testValidExpiryParsing() {
        val ocrSample = "GLYCOMET-SR 500 B.No. 4920 EXP. 12/2029"
        val result = ExpiryParser.parse(ocrSample)

        assertNotNull(result.expiryDateString)
        assertEquals("12/2029", result.expiryDateString)
        assertFalse("Future expiry should not be expired", result.isExpired)
        assertEquals("4920", result.batchNumber)
    }

    @Test
    fun testPastExpiredParsing() {
        val ocrSample = "THYRONORM 50MCG LOT-831 EXPIRY: 01/2020"
        val result = ExpiryParser.parse(ocrSample)

        assertNotNull(result.expiryDateString)
        assertEquals("01/2020", result.expiryDateString)
        assertTrue("Past date (01/2020) must be detected as expired", result.isExpired)
    }
}
