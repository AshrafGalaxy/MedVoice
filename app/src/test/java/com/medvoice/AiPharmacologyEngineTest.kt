package com.medvoice

import com.medvoice.core.ai.AiPharmacologyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPharmacologyEngineTest {

    @Test
    fun testMaritimaEuphrasiaEyeDropExtraction() {
        val engine = AiPharmacologyEngine()
        val eyeDropOcr = "MARITIMA EUPHRASIA EYE DROPS\nCINERARIA MARITIMA & EUPHRASIA OPHTHALMIC EYE CARE\n10 ML STERILE SOLUTION\nFOR EXTERNAL USE ONLY"

        val result = engine.runClinicalDeterministicParser(eyeDropOcr)
        assertNotNull(result)
        assertTrue(result!!.brandName.contains("MARITIMA", ignoreCase = true) || result.brandName.contains("EUPHRASIA", ignoreCase = true))
        assertTrue(result.activeSalts.any { it.contains("EUPHRASIA") || it.contains("MARITIMA") || it.contains("CINERARIA") })
        assertEquals("EYE_DROPS", result.dosageForm)
        assertTrue(result.vernacularInstructionHi.contains("आई ड्रॉप्स") || result.vernacularInstructionHi.contains("आँखों"))
        assertTrue(result.vernacularInstructionEn.contains("eye drop", ignoreCase = true))
    }

    @Test
    fun testCoughSyrupExtraction() {
        val engine = AiPharmacologyEngine()
        val syrupOcr = "BENADRYL COUGH FORMULA SYRUP\nDEXTROMETHORPHAN HYDROBROMIDE 10MG\nNET VOL 100 ML\nSHAKE WELL BEFORE USE"

        val result = engine.runClinicalDeterministicParser(syrupOcr)
        assertNotNull(result)
        assertTrue(result!!.activeSalts.contains("DEXTROMETHORPHAN"))
        assertEquals("SYRUP", result.dosageForm)
        assertTrue(result.vernacularInstructionHi.contains("सिरप") || result.vernacularInstructionHi.contains("पीने"))
    }

    @Test
    fun testPainReliefGelExtraction() {
        val engine = AiPharmacologyEngine()
        val gelOcr = "VOLINI PAIN RELIEF GEL\nDICLOFENAC DIETHYLAMINE GEL 30GM\nFOR EXTERNAL USE ONLY"

        val result = engine.runClinicalDeterministicParser(gelOcr)
        assertNotNull(result)
        assertTrue(result!!.activeSalts.any { it.contains("DICLOFENAC") || it.contains("VOLINI") })
        assertEquals("GEL", result.dosageForm)
        assertTrue(result.vernacularInstructionHi.contains("मलहम") || result.vernacularInstructionHi.contains("जेल"))
    }

    @Test
    fun testNasalSprayExtraction() {
        val engine = AiPharmacologyEngine()
        val sprayOcr = "OTRIVIN ADULT NASAL SPRAY\nXYLOMETAZOLINE HYDROCHLORIDE 0.1%\n10 ML"

        val result = engine.runClinicalDeterministicParser(sprayOcr)
        assertNotNull(result)
        assertTrue(result!!.activeSalts.contains("XYLOMETAZOLINE"))
        assertEquals("NASAL_SPRAY", result.dosageForm)
        assertTrue(result.vernacularInstructionHi.contains("नेजल स्प्रे") || result.vernacularInstructionHi.contains("नथुने"))
    }

    @Test
    fun testAyurvedicLiverTonicExtraction() {
        val engine = AiPharmacologyEngine()
        val tonicOcr = "LIV 52 HERBAL HEALTH TONIC\nHIMALAYA WELLNESS\n200 ML"

        val result = engine.runClinicalDeterministicParser(tonicOcr)
        assertNotNull(result)
        assertTrue(result!!.activeSalts.any { it.contains("LIV 52") || it.contains("HERBAL") })
        assertEquals("TONIC", result.dosageForm)
    }

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
        assertTrue(result.therapeuticCategory.contains("THYROID", ignoreCase = true))
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
