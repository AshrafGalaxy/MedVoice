package com.medvoice.feature.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import com.medvoice.core.vision.TextAnalyzer
import com.medvoice.ui.theme.AlertRed
import com.medvoice.ui.theme.BackgroundCharcoal
import com.medvoice.ui.theme.ReticleCyan
import com.medvoice.ui.theme.SafeGreen
import com.medvoice.ui.theme.SurfaceCardDark
import com.medvoice.ui.theme.TextMuted
import com.medvoice.ui.theme.TextWhite
import java.util.Locale

@Composable
fun ScanScreen(viewModel: ScanViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val locale by viewModel.selectedLocale.collectAsState()
    val liveOcrSnippet by viewModel.liveOcrSnippet.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val isVoiceListening by viewModel.isVoiceListening.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val cameraExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Reactive Torch Toggle
    LaunchedEffect(isTorchOn, cameraControl) {
        try {
            cameraControl?.enableTorch(isTorchOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundCharcoal
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Camera Viewport Layer
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
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
                                        imageAnalysis,
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

                    // High-Contrast Dynamic Reticle
                    val reticleColor = when (uiState) {
                        is ScanUiState.SafeDetected -> SafeGreen
                        is ScanUiState.DuplicateAlert, is ScanUiState.ConflictAlert, is ScanUiState.ExpiredAlert -> AlertRed
                        is ScanUiState.UnidentifiedAlert -> Color(0xFFFF8B00)
                        is ScanUiState.AnalyzingSnap -> SafeGreen
                        else -> ReticleCyan
                    }

                    Box(
                        modifier = Modifier
                            .size(280.dp, 160.dp)
                            .align(Alignment.Center)
                            .border(width = 3.dp, color = reticleColor, shape = RoundedCornerShape(16.dp))
                    )

                    // Live OCR HUD Viewfinder Overlay (Real-Time Reading Feedback)
                    if (uiState is ScanUiState.Scanning && liveOcrSnippet.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(top = 180.dp, start = 20.dp, end = 20.dp),
                            color = Color(0xE60B0F17),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.6f))
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
                } else {
                    // Full-Screen Accessible Permission Recovery View (WCAG AAA)
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
                                .height(80.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = TextWhite, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (locale == "hi") "अनुमति दें (Open Settings)" else "Grant Permission in Settings",
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. UI Overlay Layer (Header at top, Action Panel at bottom)
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar (Transparent background to let camera show through)
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
                                text = if (locale == "hi") "100% ऑन-डिवाइस क्लिनिकल एआई" else "100% On-Device Clinical AI",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = SafeGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Flash Torch Toggle
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

                // Bottom Accessible Action Area (Senior 80dp Touch Targets)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    when (val state = uiState) {
                        is ScanUiState.Scanning -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Instructional guidance banner
                                Surface(
                                    color = Color(0xD9121824),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.4f)),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Text(
                                        text = if (locale == "hi") "📸 दवा पर कैमरा रखें और बटन दबाएं" else "📸 Point at medicine & tap Snap",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextWhite,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }

                                // Prominent High-Resolution Snap Shutter Button (WCAG AAA 80dp Target)
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

                        is ScanUiState.AnalyzingSnap -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
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
                                            text = if (locale == "hi") "हाई-रेजोल्यूशन क्लिनिकल ओसीआर" else "High-Resolution Clinical Analysis",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = SafeGreen,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
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

                            var isAddedToCabinet by remember { mutableStateOf(false) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SafeGreen, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Top Row: Dosage Form Badge + Dismiss Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = dosageBadgeText,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.resetScanner()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = TextWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.brandName,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
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
                                Spacer(modifier = Modifier.height(10.dp))

                                if (isVoiceListening) {
                                    Surface(
                                        color = Color(0x33000000),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Voice Listening",
                                                tint = TextWhite,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (locale == "hi") "आवाज सुन रहे हैं... 'हाँ ले ली' बोलें" else "Listening... Say 'Yes taken' to confirm",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                // Primary Action: Confirm Dose Taken
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.confirmDoseTaken(state.medicineId, state.saltId, state.brandName, state.rawComposition)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TextWhite),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SafeGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (locale == "hi") "ले ली (Confirm Taken)" else "Confirm Taken",
                                        color = SafeGreen,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Secondary Action Row: Add to Cabinet & Optional Scan Back Side
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Add to Active Cabinet
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.addScannedMedicineToCabinet(
                                                brandName = state.brandName,
                                                rawComposition = if (state.rawComposition.isNotBlank()) state.rawComposition else state.saltName,
                                                dosageForm = state.dosageForm,
                                                foodTimingRule = state.timingRuleCode
                                            )
                                            isAddedToCabinet = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.22f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isAddedToCabinet) Icons.Default.CheckCircle else Icons.Default.Add,
                                            contentDescription = null,
                                            tint = TextWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isAddedToCabinet) {
                                                if (locale == "hi") "जोड़ा गया! ✓" else "Added! ✓"
                                            } else {
                                                if (locale == "hi") "+ पेटी में जोड़ें" else "+ Cabinet"
                                            },
                                            color = TextWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }

                                    // Scan Back Side (Optional Dual-Side enrichment)
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.snapPhoto(imageCapture, context, sideIndex = 2)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.22f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = TextWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (locale == "hi") "+ दूसरी तरफ" else "+ Scan Back",
                                            color = TextWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        is ScanUiState.DuplicateAlert -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AlertRed, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = TextWhite,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.resetScanner()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = TextWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
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
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.resetScanner()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TextWhite),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = AlertRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (locale == "hi") "अगली दवा स्कैन करें" else "Scan Next Medicine",
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
                                    .padding(14.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = TextWhite,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.resetScanner()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = TextWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
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
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.resetScanner()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
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

                        is ScanUiState.ExpiredAlert -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AlertRed, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = TextWhite,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.resetScanner()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = TextWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (locale == "hi") "समाप्त दवा (Expired Drug)!" else "Expired Drug Blocked!",
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
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.resetScanner()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TextWhite),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = if (locale == "hi") "खुराक रोकी गई (Dismiss)" else "Dose Blocked (Dismiss)",
                                        color = AlertRed,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        is ScanUiState.UnidentifiedAlert -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFD97706), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = TextWhite,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.resetScanner()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = TextWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (locale == "hi") "दवा की पहचान नहीं हो सकी" else "Unidentified Medicine",
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
                                Spacer(modifier = Modifier.height(10.dp))

                                // Primary: Retake Snap
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
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (locale == "hi") "📸 दोबारा फोटो लें (Retake)" else "📸 Retake Photo",
                                        color = Color(0xFFD97706),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Secondary: Scan Back Side
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.snapPhoto(imageCapture, context, sideIndex = 2)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.22f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = TextWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (locale == "hi") "दूसरी तरफ (घटक / साल्ट) स्कैन करें" else "Scan Back Side (Active Salts)",
                                        color = TextWhite,
                                        fontSize = 13.sp,
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
}
