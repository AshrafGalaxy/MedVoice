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
    val thumbnailBitmap: Bitmap? = null
)

class DualSideOcrManager {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    var side1Capture: OcrSideCapture? = null
        private set
    var side2Capture: OcrSideCapture? = null
        private set

    /**
     * Process high-resolution bitmap captured directly from CameraX ImageCapture.
     */
    suspend fun processHighResBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        sideIndex: Int = 1
    ): OcrSideCapture = withContext(Dispatchers.Default) {
        val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)

        suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val rawText = visionText.text
                    val lines = visionText.textBlocks.flatMap { block ->
                        block.lines.map { it.text.trim() }
                    }.filter { it.isNotBlank() }

                    val capture = OcrSideCapture(
                        sideIndex = sideIndex,
                        rawText = rawText,
                        textLines = lines,
                        thumbnailBitmap = bitmap
                    )

                    if (sideIndex == 1) {
                        side1Capture = capture
                    } else {
                        side2Capture = capture
                    }

                    Log.d("DualSideOcrManager", "Side $sideIndex OCR Extracted ${lines.size} lines: ${lines.take(3)}")
                    continuation.resume(capture)
                }
                .addOnFailureListener { error ->
                    Log.e("DualSideOcrManager", "ML Kit High-Res OCR failed on side $sideIndex", error)
                    val emptyCapture = OcrSideCapture(sideIndex, "", emptyList(), bitmap)
                    continuation.resume(emptyCapture)
                }
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

    fun clear() {
        side1Capture = null
        side2Capture = null
    }
}
