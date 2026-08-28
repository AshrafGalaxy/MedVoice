package com.medvoice.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medvoice.core.audio.VoiceEngineMode
import com.medvoice.core.audio.VoiceGender
import com.medvoice.feature.scanner.ScanViewModel
import com.medvoice.ui.theme.AccentBorder
import com.medvoice.ui.theme.BackgroundCharcoal
import com.medvoice.ui.theme.ReticleCyan
import com.medvoice.ui.theme.SafeGreen
import com.medvoice.ui.theme.SurfaceCardDark
import com.medvoice.ui.theme.SurfaceCardElevated
import com.medvoice.ui.theme.TextMuted
import com.medvoice.ui.theme.TextWhite

@Composable
fun SettingsScreen(viewModel: ScanViewModel) {
    val locale by viewModel.selectedLocale.collectAsState()
    val selectedGender by viewModel.selectedGender.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val engineMode by viewModel.engineMode.collectAsState()
    val caregiverPhone by viewModel.caregiverPhone.collectAsState()
    val patientName by viewModel.patientName.collectAsState()
    val haptic = LocalHapticFeedback.current

    var phoneInput by remember { mutableStateOf(caregiverPhone) }
    var nameInput by remember { mutableStateOf(patientName) }
    var apiKeyInput by remember { mutableStateOf(viewModel.ttsManager.sarvamApiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCharcoal)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = if (locale == "hi") "सेटिंग्स और वॉयस स्टूडियो" else "Settings & Voice Studio",
            style = MaterialTheme.typography.headlineMedium.copy(
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        )
        Text(
            text = if (locale == "hi") "आवाज, आपातकालीन संपर्क और एआई रीजनिंग कॉन्फ़िगर करें" else "Configure vernacular voice, caregiver SOS & AI reasoning",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextMuted,
                fontSize = 13.sp
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 1. Language Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (locale == "hi") "भाषा चुनें (App Language)" else "App Language (भाषा)",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setLocale("en")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (locale == "en") SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("English", color = TextWhite, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setLocale("hi")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (locale == "hi") SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("हिंदी (Hindi)", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Voice Customization Studio Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = ReticleCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (locale == "hi") "आवाज चयन और पिच" else "Voice Style & Pitch",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Male / Female Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setVoiceGender(VoiceGender.FEMALE)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedGender == VoiceGender.FEMALE) SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Female (Warm)", fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setVoiceGender(VoiceGender.MALE)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedGender == VoiceGender.MALE) SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Male (Clear)", fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Speech Speed Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (locale == "hi") "बोलने की गति (Speech Rate):" else "Speech Rate (Senior Clarity):",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.75f to "0.75x Slow", 0.88f to "0.88x Senior", 1.0f to "1.0x Normal").forEach { (rate, label) ->
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setSpeechRate(rate)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (speechRate == rate) SafeGreen else SurfaceCardElevated
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Text(label, fontSize = 11.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Voice Preview Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.testVoicePreview()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ReticleCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundCharcoal)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (locale == "hi") "आवाज का नमूना सुनें (Preview Voice)" else "Test Voice Preview (Audio)",
                        color = BackgroundCharcoal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. AI Reasoning & Engine Mode (Edge + Cloud Fallback)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (locale == "hi") "एआई रीजनिंग मोड (AI Engine)" else "AI Reasoning Engine",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (locale == "hi") "स्थानीय एज सुरक्षा या क्लाउड हाइब्रिड मॉडल चुनें" else "Select local edge deterministic engine or cloud hybrid fallback",
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
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setEngineMode(VoiceEngineMode.OFFLINE_DEVICE)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (engineMode == VoiceEngineMode.OFFLINE_DEVICE) SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("100% Offline", fontSize = 11.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setEngineMode(VoiceEngineMode.HYBRID_SARVAM_AI, apiKeyInput)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (engineMode == VoiceEngineMode.HYBRID_SARVAM_AI) SafeGreen else SurfaceCardElevated
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Cloud, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cloud Hybrid", fontSize = 11.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }

                if (engineMode == VoiceEngineMode.HYBRID_SARVAM_AI) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            viewModel.setEngineMode(VoiceEngineMode.HYBRID_SARVAM_AI, it)
                        },
                        label = { Text("Cloud API Key (Sarvam AI / Gemini)", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = SafeGreen,
                            unfocusedBorderColor = AccentBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Caregiver SOS Emergency Contact Setup
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (locale == "hi") "केयरगिवर आपातकालीन विवरण" else "Caregiver Emergency SOS Details",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Patient Name (रोगी का नाम)", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = SafeGreen,
                        unfocusedBorderColor = AccentBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Caregiver Mobile Number (फोन नंबर)", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = SafeGreen,
                        unfocusedBorderColor = AccentBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.updateCaregiverInfo(nameInput, phoneInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = TextWhite)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (locale == "hi") "विवरण सहेजें (Save Details)" else "Save Caregiver Details",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Senior Setup & Tour Replay
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (locale == "hi") "प्रारंभिक सेटअप और विज़ार्ड" else "Senior Setup & Onboarding Tour",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
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
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text(
                        text = if (locale == "hi") "ऑनबोर्डिंग विज़ार्ड पुनः प्रारंभ करें" else "Restart Onboarding Wizard",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
