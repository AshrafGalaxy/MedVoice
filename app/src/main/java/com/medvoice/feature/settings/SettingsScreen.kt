package com.medvoice.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

    var medGemmaKeyInput by remember { mutableStateOf(viewModel.aiEngine.cloudMedGemmaApiKey) }
    var allowCloudPrivacy by remember { mutableStateOf(viewModel.aiEngine.allowCloudPrivacyEgress) }

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
                    text = if (locale == "hi") "आवाज, आपातकालीन संपर्क और AI" else "Vernacular voice, SOS & MedGemma AI",
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
                        modifier = Modifier.size(20.dp)
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

                // Test Voice Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.testVoicePreview()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ReticleCyan),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundCharcoal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (locale == "hi") "आवाज का नमूना सुनें (Test Audio)" else "Play Spoken Voice Preview",
                        color = BackgroundCharcoal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. MedGemma AI Medical Reasoning Engine
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (locale == "hi") "मेडिकल एआई मॉडल (Medical AI)" else "Medical AI Engine (SLM / Cloud)",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (locale == "hi") "ऑन-डिवाइस MedGemma INT4 और Groq Qwen 3.8 27B मॉडल" else "On-Device MedGemma INT4 & Groq Qwen 3.8 27B LPU",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.setAiTier(AiEngineTier.ON_DEVICE_MEDGEMMA_INT4)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.aiEngine.activeTier == AiEngineTier.ON_DEVICE_MEDGEMMA_INT4) SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = TextWhite, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("On-Device INT4", fontSize = 11.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.setAiTier(AiEngineTier.CLOUD_MEDGEMMA_HOSTED)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.aiEngine.activeTier == AiEngineTier.CLOUD_MEDGEMMA_HOSTED) SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Cloud, contentDescription = null, tint = TextWhite, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cloud Qwen 27B", fontSize = 11.sp, color = TextWhite, fontWeight = FontWeight.Bold)
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
                            text = if (allowCloudPrivacy) "Cloud processing allowed" else "100% On-Device Only (No cloud egress)",
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

                if (allowCloudPrivacy || viewModel.aiEngine.activeTier == AiEngineTier.CLOUD_MEDGEMMA_HOSTED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Groq Model: qwen/qwen3.8-27b (27B Parameters)",
                        color = ReticleCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Groq API Key",
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceCardElevated,
                            border = BorderStroke(1.dp, if (medGemmaKeyInput.isNotBlank()) SafeGreen else AccentBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = if (medGemmaKeyInput.isNotBlank()) SafeGreen else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (medGemmaKeyInput.isEmpty()) {
                                        Text(
                                            text = "Enter Groq API Key (gsk_...)",
                                            color = TextMuted.copy(alpha = 0.6f),
                                            fontSize = 12.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = medGemmaKeyInput,
                                        onValueChange = {
                                            medGemmaKeyInput = it
                                        },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        cursorBrush = SolidColor(SafeGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setCloudMedGemmaApiKey(medGemmaKeyInput.trim())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = TextWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Save",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Senior Setup & Tour Replay
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (locale == "hi") "प्रारंभिक सेटअप और विज़ार्ड" else "Senior Setup & Onboarding Tour",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (locale == "hi") "भाषा, आवाज और स्वास्थ्य प्रोफ़ाइल विज़ार्ड दोबारा शुरू करें" else "Restart the accessible onboarding wizard for language & voice baseline",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.restartOnboarding()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                ) {
                    Text(
                        text = if (locale == "hi") "ऑनबोर्डिंग विज़ार्ड पुनः प्रारंभ करें" else "Restart Onboarding Wizard",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
