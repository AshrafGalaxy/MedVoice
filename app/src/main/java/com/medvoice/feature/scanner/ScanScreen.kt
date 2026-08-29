package com.medvoice.feature.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.medvoice.core.ai.AiEngineTier
import com.medvoice.ui.theme.AccentBorder
import com.medvoice.ui.theme.AlertRed
import com.medvoice.ui.theme.BackgroundCharcoal
import com.medvoice.ui.theme.ReticleCyan
import com.medvoice.ui.theme.SafeGreen
import com.medvoice.ui.theme.SurfaceCardDark
import com.medvoice.ui.theme.TextMuted
import com.medvoice.ui.theme.TextWhite
import com.medvoice.ui.theme.WarningAmber
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    val uiState by viewModel.uiState.collectAsState()
    val locale by viewModel.selectedLocale.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val isVoiceListening by viewModel.isVoiceListening.collectAsState()
    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()
    val activeAiTier by viewModel.activeAiTier.collectAsState()

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTts()
            try {
                cameraExecutor.shutdown()
            } catch (_: Exception) {}
        }
    }

    // Reactive Torch Toggle
    LaunchedEffect(isTorchOn, cameraControl) {
        try {
            cameraControl?.enableTorch(isTorchOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val isResultState = uiState is ScanUiState.SafeDetected ||
            uiState is ScanUiState.DuplicateAlert ||
            uiState is ScanUiState.ConflictAlert ||
            uiState is ScanUiState.ExpiredAlert ||
            uiState is ScanUiState.UnidentifiedAlert

    Surface(
        modifier = modifier.fillMaxSize(),
        color = BackgroundCharcoal
    ) {
        if (isResultState) {
            // Full Screen Dedicated Clinical Analysis Dashboard (Camera Closed)
            FullClinicalResultScreen(
                uiState = uiState,
                locale = locale,
                isVoiceListening = isVoiceListening,
                isSpeaking = isSpeaking,
                viewModel = viewModel
            )
        } else {
            // Full Screen Live Camera Viewport
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                try {
                                    cameraProvider.unbindAll()
                                    val camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                    cameraControl = camera.cameraControl
                                    camera.cameraControl.enableTorch(isTorchOn)
                                } catch (exc: Exception) {
                                    exc.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Dynamic Reticle
                    Box(
                        modifier = Modifier
                            .size(280.dp, 160.dp)
                            .align(Alignment.Center)
                            .border(
                                width = 3.dp,
                                color = if (uiState is ScanUiState.AnalyzingSnap) SafeGreen else ReticleCyan,
                                shape = RoundedCornerShape(16.dp)
                            )
                    )

                    // Viewfinder Guidance Banner
                    if (uiState is ScanUiState.Scanning) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(top = 190.dp, start = 20.dp, end = 20.dp),
                            color = Color(0xE60B0F17),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ReticleCyan.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = ReticleCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (locale == "hi") "पट्टी को बॉक्स में रखें और नीचे बटन दबाएं" else "Align medicine label & tap Snap to analyze",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextWhite,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // Full-Screen Permission Recovery View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SurfaceCardDark)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (locale == "hi") "कैमरा अनुमति आवश्यक है" else "Camera Permission Required",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (locale == "hi") "दवाइयों की पर्ची और शीशी स्कैन करने के लिए कृपया सेटिंग्स में जाकर कैमरा अनुमति दें।" else "To scan medicine labels and blister packs offline, please grant camera access in system settings.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextMuted,
                                fontSize = 14.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = TextWhite, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (locale == "hi") "अनुमति दें (Open Settings)" else "Grant Permission in Settings",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Camera Top Header & Bottom Shutter Controls
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x99000000))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (activeAiTier == AiEngineTier.CLOUD_MEDGEMMA_HOSTED) {
                                        if (locale == "hi") "☁️ क्लाउड विजन एआई • Qwen 27B" else "☁️ Cloud Vision AI • Qwen 27B"
                                    } else {
                                        if (locale == "hi") "⚡ 100% ऑन-डिवाइस क्लिनिकल इंजन" else "⚡ On-Device Clinical Safety"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (activeAiTier == AiEngineTier.CLOUD_MEDGEMMA_HOSTED) SafeGreen else ReticleCyan,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Torch Toggle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isTorchOn) SafeGreen.copy(alpha = 0.25f) else Color(0x66FFFFFF),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleTorch()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Torch Toggle",
                                tint = if (isTorchOn) SafeGreen else TextWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Bottom Shutter Controls
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                    ) {
                        when (val state = uiState) {
                            is ScanUiState.Scanning -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        color = Color(0xD9121824),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.4f)),
                                        modifier = Modifier.padding(bottom = 14.dp)
                                    ) {
                                        Text(
                                            text = if (locale == "hi") "📸 दवा पर कैमरा रखें और बटन दबाएं" else "📸 Align label & tap Shutter to analyze",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = TextWhite,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }

                                    // Snap Shutter Button
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .background(SafeGreen)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.snapPhoto(imageCapture, context, sideIndex = 1)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Snap Photo",
                                            tint = TextWhite,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            is ScanUiState.ScanningSide2 -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        color = Color(0xD9121824),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, ReticleCyan.copy(alpha = 0.6f)),
                                        modifier = Modifier.padding(bottom = 14.dp)
                                    ) {
                                        Text(
                                            text = if (locale == "hi") "🔄 दूसरी तरफ (घटक/साल्ट) पर रखें और फोटो लें" else "🔄 Point at back side (Ingredients) & tap Snap",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = TextWhite,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.resetScanner()
                                            },
                                            modifier = Modifier
                                                .size(52.dp)
                                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cancel",
                                                tint = TextWhite,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(76.dp)
                                                .clip(CircleShape)
                                                .background(ReticleCyan)
                                                .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.snapPhoto(imageCapture, context, sideIndex = 2)
                                            },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Snap Back Side",
                                                tint = BackgroundCharcoal,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            is ScanUiState.AnalyzingSnap -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = SafeGreen,
                                            strokeWidth = 3.dp
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = state.stageMessage,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = TextWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (locale == "hi") "हाई-रेजोल्यूशन क्लिनिकल ओसीआर" else "High-Resolution Clinical OCR",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = SafeGreen,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Full-Screen Material 3 Clinical Result Dashboard
 */
@Composable
private fun FullClinicalResultScreen(
    uiState: ScanUiState,
    locale: String,
    isVoiceListening: Boolean,
    isSpeaking: Boolean,
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isAddedToCabinet by remember { mutableStateOf(false) }

    when (uiState) {
        is ScanUiState.SafeDetected -> {
            val dosageBadgeText = when (uiState.dosageForm.uppercase(Locale.US)) {
                "TOPICAL_LOTION", "SCALP_SOLUTION" -> if (locale == "hi") "🧴 सिर/त्वचा पर लगाने की लोशन" else "🧴 Topical Scalp Lotion"
                "SHAMPOO" -> if (locale == "hi") "🧴 औषधीय शैम्पू (Shampoo)" else "🧴 Medicated Scalp Shampoo"
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
                modifier = modifier
                    .fillMaxSize()
                    .background(BackgroundCharcoal)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row: Status Badge + AI Engine + Dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = SafeGreen,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (locale == "hi") "✓ सुरक्षित खुराक" else "✓ SAFE TO TAKE",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            color = SurfaceCardDark,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AccentBorder)
                        ) {
                            Text(
                                text = if (uiState.sourceTier == AiEngineTier.CLOUD_MEDGEMMA_HOSTED) "☁️ Qwen 27B Vision" else "⚡ On-Device SQLite",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ReticleCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.resetScanner()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0x33FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card 1: Medicine Brand & Formulation
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = uiState.brandName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            color = SafeGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = dosageBadgeText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = SafeGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (locale == "hi") "सक्रिय रासायनिक घटक (Active Salts):" else "Active Chemical Composition:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = uiState.saltName.ifBlank { uiState.rawComposition },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card 2: Therapeutic Purpose & Indication
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, AccentBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(ReticleCyan.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = ReticleCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (locale == "hi") "दवा का मुख्य उपयोग (Indication)" else "Therapeutic Purpose",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = uiState.therapeuticCategory,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card 3: Administration & Food Timing
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, AccentBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(SafeGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = SafeGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (locale == "hi") "सेवन का सही नियम (How to Take)" else "Dosage & Intake Rule",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = uiState.instructionText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = SafeGreen,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        if (uiState.clinicalAdvice.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0x1A00875A),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = SafeGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = uiState.clinicalAdvice,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextWhite,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card 4: Storage Advice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, AccentBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(WarningAmber.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (locale == "hi") "सुरक्षित रख-रखाव (Storage)" else "Storage Instructions",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = uiState.storageAdvice,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextWhite,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card 5: Audio Assistant Transcript & Replay
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, AccentBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isSpeaking) {
                                    viewModel.stopTts()
                                } else {
                                    viewModel.ttsManager.speak(uiState.instructionText, locale)
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(if (isSpeaking) AlertRed else SafeGreen, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = "Voice Guide",
                                tint = TextWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSpeaking) {
                                    if (locale == "hi") "आवाज बोली जा रही है... (रोकने के लिए दबाएं)" else "Speaking... (Tap to stop)"
                                } else {
                                    if (locale == "hi") "मार्गदर्शन दोबारा सुनें (Replay Voice Guide)" else "Replay Voice Guide"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isSpeaking) AlertRed else ReticleCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = uiState.instructionText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.5.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (isVoiceListening) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0x3300875A),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Listening",
                                tint = SafeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (locale == "hi") "आवाज सुन रहे हैं... 'हाँ ले ली' बोलें" else "Listening... Say 'Yes taken' to confirm",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Action Button: Confirm Dose Taken (Optimized 52dp height)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.confirmDoseTaken(uiState.medicineId, uiState.saltId, uiState.brandName, uiState.rawComposition)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (locale == "hi") "दवा ले ली (Confirm Taken)" else "Confirm Dose Taken",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action Row (Clean single icons, no duplicate '+' characters)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Add to Cabinet
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.addScannedMedicineToCabinet(
                                brandName = uiState.brandName,
                                rawComposition = if (uiState.rawComposition.isNotBlank()) uiState.rawComposition else uiState.saltName,
                                dosageForm = uiState.dosageForm,
                                foodTimingRule = uiState.timingRuleCode
                            )
                            isAddedToCabinet = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAddedToCabinet) SafeGreen.copy(alpha = 0.25f) else SurfaceCardDark
                        ),
                        border = BorderStroke(1.dp, if (isAddedToCabinet) SafeGreen else AccentBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isAddedToCabinet) Icons.Default.Check else Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = if (isAddedToCabinet) SafeGreen else TextWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAddedToCabinet) {
                                if (locale == "hi") "जोड़ा गया! ✓" else "Saved! ✓"
                            } else {
                                if (locale == "hi") "पेटी में जोड़ें" else "Save to Cabinet"
                            },
                            color = if (isAddedToCabinet) SafeGreen else TextWhite,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Scan Next Medicine
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.resetScanner()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardDark),
                        border = BorderStroke(1.dp, AccentBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = ReticleCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (locale == "hi") "अगली दवा स्कैन करें" else "Scan Next",
                            color = TextWhite,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        is ScanUiState.DuplicateAlert -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(BackgroundCharcoal)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, AlertRed)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(AlertRed.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (locale == "hi") "सावधान! अतिरिक्त खुराक अवरुद्ध" else "Warning! Duplicate Dose Blocked",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = AlertRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = uiState.alertMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextWhite,
                                fontSize = 14.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.resetScanner()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = TextWhite, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (locale == "hi") "अगली दवा स्कैन करें" else "Scan Next Medicine",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        is ScanUiState.ConflictAlert -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(BackgroundCharcoal)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, AlertRed)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(AlertRed.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (locale == "hi") "गंभीर दवा परस्परविरोध!" else "Critical Drug Interaction!",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = AlertRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = uiState.alertMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextWhite,
                                fontSize = 14.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.resetScanner()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (locale == "hi") "समझ गए (Dismiss)" else "Understood (Dismiss)",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        is ScanUiState.ExpiredAlert -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(BackgroundCharcoal)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, AlertRed)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(AlertRed.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (locale == "hi") "समाप्त दवा अवरुद्ध (Expired)!" else "Expired Drug Blocked!",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = AlertRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = uiState.alertMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextWhite,
                                fontSize = 14.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.resetScanner()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (locale == "hi") "खुराक रोकी गई (Dismiss)" else "Dose Blocked (Dismiss)",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        is ScanUiState.UnidentifiedAlert -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(BackgroundCharcoal)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, WarningAmber)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(WarningAmber.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (locale == "hi") "दवा की पहचान नहीं हो सकी" else "Unidentified Medicine",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = WarningAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = uiState.alertMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextWhite,
                                fontSize = 14.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Retake Snap
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.resetScanner()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = TextWhite, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (locale == "hi") "दोबारा फोटो लें (Retake Photo)" else "Retake Photo",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Scan Back Side
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.prepareScanSide2()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardDark),
                            border = BorderStroke(1.dp, AccentBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = ReticleCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (locale == "hi") "दूसरी तरफ (घटक / साल्ट) स्कैन करें" else "Scan Back Side (Active Salts)",
                                color = TextWhite,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        else -> {}
    }
}
