package com.medvoice.feature.scanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun ScanScreen(viewModel: ScanViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val locale by viewModel.selectedLocale.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val cameraExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundCharcoal
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Bar: Title & Language Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (locale == "hi") "कैमरा स्कैनर 📷" else "Live Camera Scanner 📷",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    )
                    Text(
                        text = if (locale == "hi") "दवा की पट्टी सीधे कैमरे के सामने रखें" else "Point camera at medicine blister pack",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SafeGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // English Button
                    Button(
                        onClick = { viewModel.setLocale("en") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (locale == "en") SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("EN", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))

                    // Hindi Button
                    Button(
                        onClick = { viewModel.setLocale("hi") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (locale == "hi") SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("हिंदी", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Quick Demo Pills Strip (Instant one-click scenario testing)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (locale == "hi") "त्वरित परीक्षण (Quick Test):" else "Quick Test Scenarios:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted, fontSize = 11.sp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val demoMedicines = listOf(
                        "Glycomet-SR 500" to "Diabetes (Safe)",
                        "Gluconorm-SR 500" to "Metformin (Trap)",
                        "Thyronorm 50mcg" to "Thyroid (Rule)",
                        "Ecosprin 75" to "Aspirin (Take 1st)",
                        "Combiflam" to "Pain (Conflict)",
                        "Shelcal 500" to "Calcium",
                        "Orofer XT" to "Iron (Conflict)"
                    )
                    demoMedicines.forEach { (brand, desc) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.simulateScan(brand)
                            },
                            label = { Text("$brand ($desc)", fontSize = 11.sp, color = TextWhite) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceCardDark,
                                labelColor = TextWhite
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Camera Viewport & Dynamic Reticle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .border(2.dp, AccentBorder, RoundedCornerShape(16.dp))
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
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
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
                        .border(width = 3.dp, color = reticleColor, shape = RoundedCornerShape(14.dp))
                )
            }

            // Bottom Accessible Action Card (WCAG AAA Compliance)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                when (val state = uiState) {
                    is ScanUiState.Scanning -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (locale == "hi") "दवा की पट्टी कैमरे के सामने रखें..." else "Hold medicine blister pack in front of camera...",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextMuted,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    is ScanUiState.SafeDetected -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SafeGreen, RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.brandName,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                            )
                            Text(
                                text = if (locale == "hi") "घटक: ${state.saltName}" else "Active: ${state.saltName}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextWhite.copy(alpha = 0.9f),
                                    fontSize = 15.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.instructionText,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.confirmDoseTaken(state.medicineId, state.saltId, state.brandName)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TextWhite),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SafeGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (locale == "hi") "ले ली (Confirm Taken)" else "Confirm Taken (घेतली)",
                                    color = SafeGreen,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is ScanUiState.DuplicateAlert -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AlertRed, RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = TextWhite,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (locale == "hi") "सावधान! दोबारा न लें" else "Warning! Do Not Retake",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.alertMessage,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextWhite,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.resetScanner()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TextWhite),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = AlertRed
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (locale == "hi") "अगली दवा स्कैन करें (Scan Next)" else "Scan Next Medicine",
                                    color = AlertRed,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is ScanUiState.ConflictAlert -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AlertRed, RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = TextWhite,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (locale == "hi") "गंभीर दवा परस्परविरोध!" else "Critical Drug Interaction!",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.alertMessage,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = TextWhite,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.resetScanner()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TextWhite),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (locale == "hi") "समझ गए (Dismiss)" else "Understood (Dismiss)",
                                    color = AlertRed,
                                    fontSize = 17.sp,
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
