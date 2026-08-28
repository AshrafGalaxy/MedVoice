package com.medvoice.feature.scanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.medvoice.core.vision.TextAnalyzer
import com.medvoice.ui.theme.AccentBorder
import com.medvoice.ui.theme.AlertRed
import com.medvoice.ui.theme.BackgroundCharcoal
import com.medvoice.ui.theme.ReticleCyan
import com.medvoice.ui.theme.SafeGreen
import com.medvoice.ui.theme.SurfaceCardDark
import com.medvoice.ui.theme.SurfaceCardElevated
import com.medvoice.ui.theme.TextMuted
import com.medvoice.ui.theme.TextWhite
import java.util.Locale

@Composable
fun ScanScreen(viewModel: ScanViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val locale by viewModel.selectedLocale.collectAsState()
    val liveOcrSnippet by viewModel.liveOcrSnippet.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val cameraExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundCharcoal
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Header Bar with Language Switcher & Torch Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(SafeGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scanner",
                            tint = SafeGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (locale == "hi") "यूनिवर्सल मेडिसिन स्कैनर" else "Universal Medicine Scanner",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (locale == "hi") "पट्टी • ड्रॉप्स • सिरप • मलम" else "Strips • Drops • Tonics • Ointments",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SafeGreen,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Controls: Torch + Language Pills
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleTorch()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceCardElevated, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch",
                            tint = if (isTorchOn) Color(0xFFFFD700) else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .background(SurfaceCardElevated, RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (locale == "en") SafeGreen else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setLocale("en")
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("EN", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (locale == "hi") SafeGreen else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setLocale("hi")
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("हिंदी", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 2. Camera Viewport & Dynamic Reticle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .border(1.5.dp, AccentBorder, RoundedCornerShape(18.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor, TextAnalyzer { tokens ->
                                        viewModel.processOcrTokens(tokens)
                                    })
                                }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                                camera.cameraControl.enableTorch(isTorchOn)
                            } catch (exc: Exception) {
                                exc.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // High-Contrast Dynamic Reticle
                val reticleColor = when (uiState) {
                    is ScanUiState.SafeDetected -> SafeGreen
                    is ScanUiState.DuplicateAlert, is ScanUiState.ConflictAlert -> AlertRed
                    else -> ReticleCyan
                }

                Box(
                    modifier = Modifier
                        .size(280.dp, 160.dp)
                        .align(Alignment.Center)
                        .border(width = 3.dp, color = reticleColor, shape = RoundedCornerShape(16.dp))
                )

                // Live OCR HUD Viewfinder Overlay
                if (uiState is ScanUiState.Scanning && liveOcrSnippet.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                        color = Color(0xE60B0F17),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = liveOcrSnippet,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3. Bottom Accessible Action Area (Scroll-Safe & Zero Clipping)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                when (val state = uiState) {
                    is ScanUiState.Scanning -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (locale == "hi") "दवा की शीशी, पट्टी या डिब्बा कैमरे के सामने रखें..." else "Point camera at any medicine strip, drop, or bottle...",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextMuted,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    is ScanUiState.SafeDetected -> {
                        val dosageBadgeText = when (state.dosageForm.uppercase(Locale.US)) {
                            "EYE_DROPS", "DROPS" -> if (locale == "hi") "👁️ आई ड्रॉप्स (Eye Drops)" else "👁️ Ophthalmic Eye Drops"
                            "EAR_DROPS" -> if (locale == "hi") "👂 ईयर ड्रॉप्स (Ear Drops)" else "👂 Ear Drops"
                            "NASAL_SPRAY" -> if (locale == "hi") "👃 नेजल स्प्रे (Nasal Spray)" else "👃 Nasal Spray"
                            "SYRUP", "TONIC" -> if (locale == "hi") "🧪 टॉनिक / सिरप (Syrup)" else "🧪 Oral Tonic / Syrup"
                            "OINTMENT", "GEL" -> if (locale == "hi") "🧴 मलहम / जेल (Ointment)" else "🧴 Topical Gel / Ointment"
                            "INHALER" -> if (locale == "hi") "🫁 इनहेलर (Inhaler)" else "🫁 Respiratory Inhaler"
                            "CAPSULE" -> if (locale == "hi") "💊 कैप्सूल (Capsule)" else "💊 Oral Capsule"
                            else -> if (locale == "hi") "💊 खाने की गोली (Tablet)" else "💊 Oral Tablet"
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SafeGreen, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Dosage Form Pill
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = dosageBadgeText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.brandName,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (locale == "hi") "घटक: ${state.saltName}" else "Active: ${state.saltName}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextWhite.copy(alpha = 0.9f),
                                    fontSize = 13.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.instructionText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.confirmDoseTaken(state.medicineId, state.saltId, state.brandName)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TextWhite),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SafeGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (locale == "hi") "ले ली (Confirm Taken)" else "Confirm Taken",
                                    color = SafeGreen,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is ScanUiState.DuplicateAlert -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AlertRed, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = TextWhite,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (locale == "hi") "सावधान! दोबारा न लें" else "Warning! Do Not Retake",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.alertMessage,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.resetScanner()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TextWhite),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = AlertRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (locale == "hi") "अगली दवा स्कैन करें" else "Scan Next Medicine",
                                    color = AlertRed,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is ScanUiState.ConflictAlert -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AlertRed, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = TextWhite,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (locale == "hi") "गंभीर दवा परस्परविरोध!" else "Critical Drug Interaction!",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.alertMessage,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.resetScanner()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TextWhite),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (locale == "hi") "समझ गए (Dismiss)" else "Understood (Dismiss)",
                                    color = AlertRed,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
