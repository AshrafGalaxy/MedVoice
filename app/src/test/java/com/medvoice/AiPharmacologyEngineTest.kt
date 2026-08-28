package com.medvoice

import com.medvoice.core.ai.AiPharmacologyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPharmacologyEngineTest {

    @Test
    fun testMetforminExtractionFromMessyOcr() {
        val engine = AiPharmacologyEngine()
        val messyOcr = "CADILA PHARMACEUTICALS\nGLYCOMET SR 500\nMETFORMIN HYDROCHLORIDE PROLONGED RELEASE TABLETS IP 500 MG\nEXP 12/28"

        val result = engine.runClinicalDeterministicParser(messyOcr)
        assertNotNull(result)
        assertTrue(result!!.activeSalts.contains("METFORMIN"))
        assertEquals(500.0, result.strengthMg, 0.01)
        assertEquals("TABLET", result.dosageForm)
    }

    @Test
    fun testIbuprofenAndParacetamolExtraction() {
        val engine = AiPharmacologyEngine()
        val messyOcr = "COMBIFLAM TABLET\nIBUPROFEN 400MG + PARACETAMOL 325MG\nSANOFI INDIA"

        val result = engine.runClinicalDeterministicParser(messyOcr)
        assertNotNull(result)
        assertTrue(result!!.activeSalts.contains("IBUPROFEN"))
        assertTrue(result.activeSalts.contains("PARACETAMOL"))
        assertEquals("TABLET", result.dosageForm)
    }

    @Test
    fun testThyroxineEmptyStomachHormoneExtraction() {
        val engine = AiPharmacologyEngine()
        val messyOcr = "THYRONORM 50 MCG\nLEVOTHYROXINE SODIUM TABLETS\nCIPLA LTD"

        val result = engine.runClinicalDeterministicParser(messyOcr)
        assertNotNull(result)
        assertTrue(result!!.activeSalts.contains("LEVOTHYROXINE"))
        assertEquals("Thyroid Hormone", result.therapeuticCategory)
    }

    @Test
    fun testPantoprazoleAntacidCapsuleExtraction() {
        val engine = AiPharmacologyEngine()
        val messyOcr = "PAN-D CAPSULE\nPANTOPRAZOLE GASTRO-RESISTANT 40MG\nALKEM LABS"

        val result = engine.runClinicalDeterministicParser(messyOcr)
        assertNotNull(result)
        assertTrue(result!!.activeSalts.contains("PANTOPRAZOLE"))
        assertEquals("CAPSULE", result.dosageForm)
    }
}
