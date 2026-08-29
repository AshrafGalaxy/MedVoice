package com.medvoice.core.vision

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextAnalyzer(
    private val onTextDetected: (List<String>) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastAnalyzedTimestamp = 0L

    // Temporal Stabilization Queue
    private val frameBuffer = mutableListOf<List<String>>()
    private val requiredStableFrames = 2

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()

        // Rate-limit frame analysis to ~8 FPS (every 120ms) to conserve battery and prevent thermal throttling
        if (currentTimestamp - lastAnalyzedTimestamp < 120L) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = visionText.textBlocks.flatMap { block ->
                        block.lines.map { it.text.trim() }
                    }.filter { it.isNotBlank() && it.length >= 2 }

                    if (lines.isNotEmpty()) {
                        onTextDetected(lines)
                    }
                }
                .addOnFailureListener { error ->
                    Log.e("TextAnalyzer", "OCR extraction failed", error)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    lastAnalyzedTimestamp = currentTimestamp
                }
        } else {
            imageProxy.close()
        }
    }

    private fun checkStability(buffer: List<List<String>>): Boolean {
        val firstFrameText = buffer.first().joinToString(" ")
        val lastFrameText = buffer.last().joinToString(" ")
        
        val firstWords = firstFrameText.split(Regex("\\s+")).toSet()
        val lastWords = lastFrameText.split(Regex("\\s+")).toSet()

        if (firstWords.isEmpty() || lastWords.isEmpty()) return false

        val intersection = firstWords.intersect(lastWords)
        val similarity = intersection.size.toFloat() / maxOf(firstWords.size, lastWords.size).toFloat()
        
        return similarity >= 0.4f
    }
}
