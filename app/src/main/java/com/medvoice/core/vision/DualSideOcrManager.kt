package com.medvoice.core.vision

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class OcrSideCapture(
    val sideIndex: Int, // 1 for Front, 2 for Back
    val rawText: String,
    val textLines: List<String>,
    val highResBitmap: Bitmap? = null
)

class DualSideOcrManager {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    var side1Capture: OcrSideCapture? = null
        private set
    var side2Capture: OcrSideCapture? = null
        private set

    /**
     * Process high-resolution bitmap with:
     * 1. Multi-orientation OCR pass (0° normal + 90° vertical + 270° vertical) to capture curved and side text.
     * 2. Glare and contrast enhancement via ImagePreprocessingEngine.
     */
    suspend fun processHighResBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        sideIndex: Int = 1
    ): OcrSideCapture = withContext(Dispatchers.Default) {
        val enhancedBitmap = ImagePreprocessingEngine.enhanceContrastForOcr(bitmap)
        
        // Pass 1: Primary orientation
        val pass1Lines = recognizeTextFromBitmap(enhancedBitmap, rotationDegrees)
        
        // Pass 2: Vertical 90° orientation for vertical side-printed compositions
        val rotated90 = ImagePreprocessingEngine.rotateBitmap(enhancedBitmap, 90f)
        val pass2Lines = recognizeTextFromBitmap(rotated90, 0)

        // Pass 3: Vertical 270° orientation
        val rotated270 = ImagePreprocessingEngine.rotateBitmap(enhancedBitmap, 270f)
        val pass3Lines = recognizeTextFromBitmap(rotated270, 0)

        // Combine and deduplicate extracted lines
        val combinedLines = (pass1Lines + pass2Lines + pass3Lines).distinct().filter { it.isNotBlank() && it.length > 2 }
        val rawText = combinedLines.joinToString("\n")

        val capture = OcrSideCapture(
            sideIndex = sideIndex,
            rawText = rawText,
            textLines = combinedLines,
            highResBitmap = bitmap
        )

        if (sideIndex == 1) {
            side1Capture = capture
        } else {
            side2Capture = capture
        }

        Log.d("DualSideOcrManager", "Side $sideIndex Multi-Pass OCR Extracted ${combinedLines.size} unique lines: ${combinedLines.take(4)}")
        capture
    }

    private suspend fun recognizeTextFromBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int
    ): List<String> = suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val lines = visionText.textBlocks.flatMap { block ->
                    block.lines.map { it.text.trim() }
                }.filter { it.isNotBlank() }
                continuation.resume(lines)
            }
            .addOnFailureListener { error ->
                Log.w("DualSideOcrManager", "ML Kit sub-pass failed: ${error.message}")
                continuation.resume(emptyList())
            }
    }

    /**
     * Combine tokens and text from available sides (Side 1 only or Side 1 + Side 2).
     */
    fun getSynthesizedTokens(): List<String> {
        val tokens1 = side1Capture?.textLines ?: emptyList()
        val tokens2 = side2Capture?.textLines ?: emptyList()
        return (tokens1 + tokens2).distinct()
    }

    fun getSynthesizedRawText(): String {
        val text1 = side1Capture?.rawText ?: ""
        val text2 = side2Capture?.rawText ?: ""
        return when {
            text1.isNotBlank() && text2.isNotBlank() -> "FRONT SIDE:\n$text1\n\nBACK SIDE:\n$text2"
            text1.isNotBlank() -> text1
            else -> text2
        }
    }

    fun getLatestBitmap(): Bitmap? {
        return side2Capture?.highResBitmap ?: side1Capture?.highResBitmap
    }

    fun clear() {
        side1Capture = null
        side2Capture = null
    }
}
