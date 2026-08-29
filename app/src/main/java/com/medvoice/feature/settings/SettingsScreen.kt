package com.medvoice.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import com.medvoice.core.ai.OnDeviceEligibilityStatus
import com.medvoice.core.ai.ModelDownloadStatus
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.medvoice.core.ai.AiEngineTier
import com.medvoice.core.audio.VoiceGender
import com.medvoice.feature.scanner.ScanViewModel
import com.medvoice.ui.theme.AccentBorder
import com.medvoice.ui.theme.AlertRed
import com.medvoice.ui.theme.BackgroundCharcoal
import com.medvoice.ui.theme.ReticleCyan
import com.medvoice.ui.theme.SafeGreen
import com.medvoice.ui.theme.SurfaceCardDark
import com.medvoice.ui.theme.SurfaceCardElevated
import com.medvoice.ui.theme.TextMuted
import com.medvoice.ui.theme.TextWhite
import com.medvoice.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: ScanViewModel) {
    val locale by viewModel.selectedLocale.collectAsState()
    val selectedGender by viewModel.selectedGender.collectAsState()
    val caregiverPhone by viewModel.caregiverPhone.collectAsState()
    val patientName by viewModel.patientName.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var phoneInput by remember { mutableStateOf(caregiverPhone) }
    var nameInput by remember { mutableStateOf(patientName) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var isSavedRecently by remember { mutableStateOf(false) }

    val isCloudApiKeyConfigured by viewModel.isCloudApiKeyConfigured.collectAsState()
    val activeAiTier by viewModel.activeAiTier.collectAsState()
    val hardwareReport by viewModel.hardwareReport.collectAsState()
    val selectedConditions by viewModel.selectedConditions.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    var allowCloudPrivacy by remember { mutableStateOf(viewModel.aiEngine.allowCloudPrivacyEgress) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTts()
        }
    }

    LaunchedEffect(caregiverPhone, patientName) {
        phoneInput = caregiverPhone
        nameInput = patientName
    }

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCharcoal)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(SafeGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = SafeGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (locale == "hi") "सेटिंग्स और वॉयस स्टूडियो" else "Settings & Voice Studio",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (locale == "hi") "आवाज, आपातकालीन संपर्क एवं प्रोफाइल" else "Vernacular voice, SOS & health profile",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Language Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (locale == "hi") "भाषा चुनें (App Language)" else "App Language (भाषा)",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setLocale("en")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (locale == "en") SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Text("English", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setLocale("hi")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (locale == "hi") SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Text("हिंदी (Hindi)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Voice Customization Studio Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (locale == "hi") "आवाज चयन (Google Neural HD)" else "Vernacular Voice Assistant",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Voice Gender Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setVoiceGender(VoiceGender.FEMALE)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedGender == VoiceGender.FEMALE) SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = TextWhite, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (locale == "hi") "महिला (Female)" else "Female (Warm)",
                            fontSize = 12.sp,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setVoiceGender(VoiceGender.MALE)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedGender == VoiceGender.MALE) SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = TextWhite, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (locale == "hi") "पुरुष (Male)" else "Male (Clear)",
                            fontSize = 12.sp,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dynamic Test Voice / Stop Preview Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleVoicePreview()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSpeaking) AlertRed else ReticleCyan
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isSpeaking) TextWhite else BackgroundCharcoal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSpeaking) {
                            if (locale == "hi") "आवाज बंद करें (Stop Audio)" else "Stop Voice Preview"
                        } else {
                            if (locale == "hi") "आवाज का नमूना सुनें (Test Audio)" else "Play Spoken Voice Preview"
                        },
                        color = if (isSpeaking) TextWhite else BackgroundCharcoal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Chronic Medical Conditions Card (Disease-Drug Matrix)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MedicalServices, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (locale == "hi") "क्रोनिक मेडिकल प्रोफाइल (Health Profile)" else "Patient Chronic Health Profile",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (locale == "hi") "सक्रिय बीमारियों के अनुसार परस्परविरोधी दवाएं रोकी जाएंगी" else "Clinical engine blocks drugs conflicting with your conditions",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                val conditionsList = listOf(
                    "Type-2 Diabetes" to (if (locale == "hi") "टाइप-2 डायबिटीज (Diabetes)" else "Type-2 Diabetes"),
                    "Hypertension (BP)" to (if (locale == "hi") "हाई ब्लड प्रेशर (BP)" else "Hypertension (BP)"),
                    "Thyroid Disorder" to (if (locale == "hi") "थायरॉइड विकार (Thyroid)" else "Thyroid Disorder"),
                    "Cardiac / Heart Condition" to (if (locale == "hi") "हृदय रोग (Cardiac)" else "Cardiac / Heart Condition")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    conditionsList.forEach { (canonicalKey, localizedLabel) ->
                        val isSelected = selectedConditions.contains(canonicalKey)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.togglePatientCondition(canonicalKey)
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) SurfaceCardElevated else SurfaceCardDark,
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) SafeGreen else AccentBorder.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(
                                                if (isSelected) SafeGreen else SurfaceCardDark,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) SafeGreen else TextMuted.copy(alpha = 0.5f),
                                                RoundedCornerShape(6.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = BackgroundCharcoal, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = localizedLabel,
                                        color = if (isSelected) TextWhite else TextMuted,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.5.sp
                                    )
                                }
                                if (isSelected) {
                                    Surface(
                                        color = SafeGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (locale == "hi") "सुरक्षा सक्रिय" else "ACTIVE GUARD",
                                            color = SafeGreen,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Medical AI Reasoning Engine
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (locale == "hi") "मेडिकल एआई मॉडल (Medical AI)" else "Medical AI Engine",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    if (activeAiTier != AiEngineTier.CLOUD_MEDGEMMA_HOSTED) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.refreshHardwareAudit()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Hardware Audit", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (locale == "hi") "एआई इंजन चयन और सेटिंग्स" else "Select active reasoning engine and runtime settings",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Reactive AI Tier Selector Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setAiTier(AiEngineTier.ON_DEVICE_MEDGEMMA_INT4)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeAiTier == AiEngineTier.ON_DEVICE_MEDGEMMA_INT4) SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = if (activeAiTier == AiEngineTier.ON_DEVICE_MEDGEMMA_INT4) BackgroundCharcoal else TextWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (locale == "hi") "ऑन-डिवाइस" else "On-Device",
                            fontSize = 13.sp,
                            color = if (activeAiTier == AiEngineTier.ON_DEVICE_MEDGEMMA_INT4) BackgroundCharcoal else TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setAiTier(AiEngineTier.CLOUD_MEDGEMMA_HOSTED)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeAiTier == AiEngineTier.CLOUD_MEDGEMMA_HOSTED) SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = if (activeAiTier == AiEngineTier.CLOUD_MEDGEMMA_HOSTED) BackgroundCharcoal else TextWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (locale == "hi") "क्लाउड" else "Cloud",
                            fontSize = 13.sp,
                            color = if (activeAiTier == AiEngineTier.CLOUD_MEDGEMMA_HOSTED) BackgroundCharcoal else TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live Hardware Diagnostics Card (ONLY rendered when On-Device AI is selected)
                if (activeAiTier != AiEngineTier.CLOUD_MEDGEMMA_HOSTED) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceCardElevated,
                        border = BorderStroke(1.dp, AccentBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Memory, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (locale == "hi") "हार्डवेयर स्पेक्स" else "Hardware Diagnostics",
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    color = if (hardwareReport.eligibilityStatus == OnDeviceEligibilityStatus.FULLY_ELIGIBLE) SafeGreen.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (hardwareReport.eligibilityStatus == OnDeviceEligibilityStatus.FULLY_ELIGIBLE) "✓ 6GB+ ELIGIBLE" else "AUDIT CHECK",
                                        color = if (hardwareReport.eligibilityStatus == OnDeviceEligibilityStatus.FULLY_ELIGIBLE) SafeGreen else AlertRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Clean 2-column tiles with no text wrapping
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Left Tile: Device Model & CPU
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceCardDark.copy(alpha = 0.6f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "📱 ${hardwareReport.deviceModel}",
                                            color = TextWhite,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "⚡ CPU: ${hardwareReport.cpuCores} Cores (${if (hardwareReport.is64Bit) "64-bit" else "32-bit"})",
                                            color = TextMuted,
                                            fontSize = 10.5.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // Right Tile: RAM & Battery
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceCardDark.copy(alpha = 0.6f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "💾 RAM: ${hardwareReport.totalRamGb} GB",
                                            color = if (hardwareReport.isRamEligible) SafeGreen else WarningAmber,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "🔋 ${hardwareReport.batteryPct}% Battery • ${hardwareReport.batteryTempCelsius}°C",
                                            color = if (hardwareReport.isThermalSafe) SafeGreen else WarningAmber,
                                            fontSize = 10.5.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1B Instruct Neural Model Management (ONLY in On-Device AI Mode)
                    val modelStatus by viewModel.modelDownloadStatus.collectAsState()

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceCardElevated,
                        border = BorderStroke(1.dp, if (modelStatus is ModelDownloadStatus.Ready) SafeGreen.copy(alpha = 0.5f) else AccentBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = if (modelStatus is ModelDownloadStatus.Ready) SafeGreen else ReticleCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (locale == "hi") "1B ऑन-डिवाइस मॉडल" else "1B Neural Model",
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    color = when (modelStatus) {
                                        is ModelDownloadStatus.Ready -> SafeGreen.copy(alpha = 0.15f)
                                        is ModelDownloadStatus.Downloading -> ReticleCyan.copy(alpha = 0.15f)
                                        is ModelDownloadStatus.Error -> AlertRed.copy(alpha = 0.15f)
                                        else -> SurfaceCardDark
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = when (modelStatus) {
                                            is ModelDownloadStatus.Ready -> "✓ READY"
                                            is ModelDownloadStatus.Downloading -> "DOWNLOADING"
                                            is ModelDownloadStatus.Error -> "ERROR"
                                            else -> "OPTIONAL"
                                        },
                                        color = when (modelStatus) {
                                            is ModelDownloadStatus.Ready -> SafeGreen
                                            is ModelDownloadStatus.Downloading -> ReticleCyan
                                            is ModelDownloadStatus.Error -> AlertRed
                                            else -> TextMuted
                                        },
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (locale == "hi") {
                                    "Qwen 2.5 1.5B (INT4) मॉडल डाउनलोड करें ताकि इंटरनेट के बिना फोन पर ही न्यूरल रीजनिंग और जटिल फॉर्मूलेशन विश्लेषण हो सके।"
                                } else {
                                    "Download Qwen 2.5 1.5B (INT4 Quantized) to run full neural chemical reasoning directly on-device with zero internet."
                                },
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            when (val status = modelStatus) {
                                is ModelDownloadStatus.NotDownloaded -> {
                                    val isEligible = hardwareReport.totalRamMb >= 3500L
                                    Column {
                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.startModelDownload()
                                            },
                                            enabled = isEligible,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SafeGreen,
                                                disabledContainerColor = SurfaceCardDark
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(42.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, tint = if (isEligible) TextWhite else TextMuted, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (locale == "hi") "1B मॉडल डाउनलोड करें (~380 MB)" else "Download 1B Instruct Model (~380 MB)",
                                                color = if (isEligible) TextWhite else TextMuted,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (!isEligible) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "⚠️ Device has ${hardwareReport.totalRamGb}GB RAM. 1B neural inference requires 4GB+ RAM. Standard on-device SQLite engine active.",
                                                color = WarningAmber,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                }

                                is ModelDownloadStatus.Downloading -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Downloading: ${(status.progress * 100).toInt()}% (${status.downloadedBytes / (1024 * 1024)}MB / ${status.totalBytes / (1024 * 1024)}MB)",
                                                color = TextWhite,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (status.formattedSpeed.isNotBlank()) {
                                                Text(
                                                    text = status.formattedSpeed,
                                                    color = ReticleCyan,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        LinearProgressIndicator(
                                            progress = { status.progress },
                                            modifier = Modifier.fillMaxWidth().height(6.dp),
                                            color = SafeGreen,
                                            trackColor = SurfaceCardDark
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.cancelModelDownload()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(alpha = 0.2f)),
                                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(36.dp)
                                        ) {
                                            Text(
                                                text = if (locale == "hi") "डाउनलोड रद्द करें" else "Cancel Download",
                                                color = AlertRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                is ModelDownloadStatus.Ready -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "📦 Model Size: ${String.format(java.util.Locale.US, "%.1f", status.sizeMb)} MB",
                                                color = TextWhite,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "• Stored locally in sandbox • Zero network required",
                                                color = SafeGreen,
                                                fontSize = 10.5.sp
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.deleteDownloadedModel()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Delete", color = AlertRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                is ModelDownloadStatus.Error -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = status.message,
                                            color = AlertRed,
                                            fontSize = 11.sp
                                        )
                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.startModelDownload()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(38.dp)
                                        ) {
                                            Text("Retry Download", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Unified Live Runtime Status Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (activeAiTier == AiEngineTier.CLOUD_MEDGEMMA_HOSTED) SafeGreen.copy(alpha = 0.12f) else SurfaceCardElevated,
                    border = BorderStroke(1.dp, if (activeAiTier == AiEngineTier.CLOUD_MEDGEMMA_HOSTED) SafeGreen.copy(alpha = 0.4f) else AccentBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (activeAiTier == AiEngineTier.CLOUD_MEDGEMMA_HOSTED) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cloud, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (locale == "hi") "सक्रिय: क्लाउड विजन एआई (Qwen 2.5 27B)" else "Active: Cloud Vision AI (Qwen 2.5 27B)",
                                    color = SafeGreen,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Ultra-fast neural reasoning (<400ms)\n• Pre-configured Demo Gateway active with zero setup",
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, tint = ReticleCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (locale == "hi") "सक्रिय: ऑन-डिवाइस इंजन (Qwen 1.5B / SQLite FTS5)" else "Active: On-Device Engine (Qwen 1.5B / SQLite FTS5)",
                                    color = ReticleCyan,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• 100% Offline (Zero Network Required) • Local SQLite FTS5 & Pharmacopeia\n• Edge Privacy: No biometric or medication data leaves device.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Privacy Guardrail Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Privacy Guardrail (Data Egress)", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (allowCloudPrivacy) "Cloud processing allowed" else "100% On-Device Only (No data egress)",
                            color = if (allowCloudPrivacy) ReticleCyan else SafeGreen,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = allowCloudPrivacy,
                        onCheckedChange = {
                            allowCloudPrivacy = it
                            viewModel.setCloudPrivacyEgress(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = SafeGreen
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Caregiver SOS Emergency Contact Setup with Real SMS Permission Check
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (locale == "hi") "केयरगिवर आपातकालीन विवरण" else "Caregiver Emergency SOS Details",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (locale == "hi") "आपातकालीन स्थिति में स्वचालित एसएमएस अलर्ट" else "Direct cellular SMS alert on critical drug risk",
                            color = TextMuted,
                            fontSize = 11.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (!hasSmsPermission) {
                                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasSmsPermission) SafeGreen.copy(alpha = 0.2f) else AlertRed.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 30.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hasSmsPermission) Icons.Default.Check else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (hasSmsPermission) SafeGreen else AlertRed,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasSmsPermission) "SMS Ready" else "Grant SMS",
                                color = if (hasSmsPermission) SafeGreen else AlertRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Patient Name Label
                Text(
                    text = if (locale == "hi") "रोगी का नाम" else "Patient Name",
                    color = if (nameError != null) AlertRed else TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Patient Name Sleek 44dp Input Field
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCardElevated,
                    border = BorderStroke(
                        1.dp,
                        when {
                            nameError != null -> AlertRed
                            nameInput.isNotBlank() -> SafeGreen
                            else -> AccentBorder
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = when {
                                nameError != null -> AlertRed
                                nameInput.isNotBlank() -> SafeGreen
                                else -> TextMuted
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (nameInput.isEmpty()) {
                                Text(
                                    text = if (locale == "hi") "रोगी का नाम दर्ज करें" else "Enter patient name",
                                    color = TextMuted.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                            BasicTextField(
                                value = nameInput,
                                onValueChange = {
                                    nameInput = it
                                    if (nameError != null) nameError = null
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    keyboardType = KeyboardType.Text
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(SafeGreen),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (nameInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    nameInput = ""
                                    nameError = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                if (nameError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = nameError ?: "",
                            color = AlertRed,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Caregiver Mobile Label
                Text(
                    text = if (locale == "hi") "केयरगिवर मोबाइल नंबर" else "Caregiver Mobile Number",
                    color = if (phoneError != null) AlertRed else TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Caregiver Phone Sleek 44dp Input Field
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCardElevated,
                    border = BorderStroke(
                        1.dp,
                        when {
                            phoneError != null -> AlertRed
                            phoneInput.isNotBlank() -> SafeGreen
                            else -> AccentBorder
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = when {
                                phoneError != null -> AlertRed
                                phoneInput.isNotBlank() -> SafeGreen
                                else -> TextMuted
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (phoneInput.isEmpty()) {
                                Text(
                                    text = if (locale == "hi") "10 अंकों का मोबाइल नंबर दर्ज करें" else "Enter 10-digit mobile number",
                                    color = TextMuted.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                            BasicTextField(
                                value = phoneInput,
                                onValueChange = {
                                    phoneInput = it
                                    if (phoneError != null) phoneError = null
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(SafeGreen),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (phoneInput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    phoneInput = ""
                                    phoneError = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                if (phoneError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = phoneError ?: "",
                            color = AlertRed,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save Button with Dynamic Green Mark Confirmation State
                Button(
                    onClick = {
                        val trimmedName = nameInput.trim()
                        val rawPhone = phoneInput.trim().replace(" ", "").replace("-", "")

                        var hasError = false
                        if (trimmedName.length < 2) {
                            nameError = if (locale == "hi") "कृपया मान्य नाम दर्ज करें (कम से कम 2 अक्षर)" else "Please enter a valid name (min 2 characters)"
                            hasError = true
                        } else {
                            nameError = null
                        }

                        val indianRegex = Regex("""^(\+91|0)?[6-9]\d{9}$""")
                        val generalPhoneRegex = Regex("""^\+?[1-9]\d{6,14}$""")
                        if (!indianRegex.matches(rawPhone) && !generalPhoneRegex.matches(rawPhone)) {
                            phoneError = if (locale == "hi") "कृपया मान्य 10 अंकों का मोबाइल नंबर दर्ज करें" else "Please enter a valid 10-digit mobile number"
                            hasError = true
                        } else {
                            phoneError = null
                        }

                        if (!hasError) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val normalizedPhone = if (rawPhone.length == 10 && rawPhone.all { it.isDigit() }) {
                                "+91$rawPhone"
                            } else rawPhone

                            phoneInput = normalizedPhone
                            viewModel.updateCaregiverInfo(trimmedName, normalizedPhone)
                            isSavedRecently = true
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(2500)
                                isSavedRecently = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSavedRecently) SafeGreen else SafeGreen.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                ) {
                    Icon(
                        imageVector = if (isSavedRecently) Icons.Default.CheckCircle else Icons.Default.Save,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSavedRecently) {
                            if (locale == "hi") "सफलतापूर्वक सहेजा गया! ✓" else "Details Saved Successfully! ✓"
                        } else {
                            if (locale == "hi") "केयरगिवर विवरण सहेजें" else "Save Caregiver Details"
                        },
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Test SMS Trigger Button
                if (caregiverPhone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!hasSmsPermission) {
                                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                            } else {
                                viewModel.testEmergencySms()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = if (hasSmsPermission) SafeGreen else AlertRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (locale == "hi") "परीक्षण आपातकालीन एसएमएस भेजें" else "Test Emergency SOS Alert to Caregiver",
                            color = TextWhite,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
