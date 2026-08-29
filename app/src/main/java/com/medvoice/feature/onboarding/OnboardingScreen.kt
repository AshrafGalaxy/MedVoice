package com.medvoice.feature.onboarding

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medvoice.core.audio.VoiceGender
import com.medvoice.feature.scanner.ScanViewModel
import com.medvoice.ui.components.MedVoiceLogo
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
fun OnboardingScreen(
    viewModel: ScanViewModel,
    onOnboardingComplete: () -> Unit
) {
    val locale by viewModel.selectedLocale.collectAsState()
    val selectedGender by viewModel.selectedGender.collectAsState()
    val haptic = LocalHapticFeedback.current

    var currentStep by remember { mutableIntStateOf(1) }
    var patientNameInput by remember {
        mutableStateOf(
            viewModel.patientName.value.takeIf { it != "Senior Patient" && it != "User" } ?: ""
        )
    }
    var phoneInput by remember { mutableStateOf(viewModel.caregiverPhone.value) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val selectedConditions = remember { mutableStateListOf("Type-2 Diabetes", "Hypertension (BP)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCharcoal)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header Branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            MedVoiceLogo(size = 56)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "MedVoice",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            )
            Text(
                text = if (locale == "hi") "100% ऑफलाइन एज दवा सुरक्षा सहायक" else "100% Offline Edge Medication Safety",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = SafeGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Step Content Switcher
        when (currentStep) {
            1 -> {
                // Step 1: Language Selection
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (locale == "hi") "अपनी पसंदीदा भाषा चुनें" else "Choose Your Preferred Language",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (locale == "hi") "दवा के निर्देश आपकी चुनी हुई भाषा में बोले जाएंगे।" else "Dosage instructions will be spoken aloud in this language.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            fontSize = 14.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Language Option: English
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (locale == "en") 2.dp else 1.dp,
                                color = if (locale == "en") SafeGreen else AccentBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setLocale("en")
                                viewModel.ttsManager.speak("Welcome to MedVoice. Your offline medication safety assistant.", "en")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (locale == "en") SurfaceCardElevated else SurfaceCardDark
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("English (Indian)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Spoken guidance in Indian English", color = TextMuted, fontSize = 13.sp)
                            }
                            if (locale == "en") {
                                Icon(Icons.Default.Check, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(26.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Language Option: Hindi
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (locale == "hi") 2.dp else 1.dp,
                                color = if (locale == "hi") SafeGreen else AccentBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setLocale("hi")
                                viewModel.ttsManager.speak("मेडवॉयस में आपका स्वागत है। आपकी ऑफ़लाइन दवा सुरक्षा सहायक।", "hi")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (locale == "hi") SurfaceCardElevated else SurfaceCardDark
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("हिंदी (Hindi)", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("सरल हिंदी में बोलकर दवा के निर्देश", color = TextMuted, fontSize = 13.sp)
                            }
                            if (locale == "hi") {
                                Icon(Icons.Default.Check, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                }
            }

            2 -> {
                // Step 2: Voice Personalization
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (locale == "hi") "आवाज शैली और गति" else "Voice Style & Senior Speed",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (locale == "hi") "बुजुर्गों के लिए स्पष्ट और धीमी आवाज का चयन करें।" else "Select warm, clear audio optimized for senior hearing.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            fontSize = 14.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Voice Gender Options
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
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = TextWhite, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
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
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = TextWhite, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Male (Clear)", fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundCharcoal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (locale == "hi") "आवाज का नमूना सुनें (Test Voice)" else "Play Spoken Voice Sample",
                            color = BackgroundCharcoal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            3 -> {
                // Step 3: Medical Baseline & Caregiver Emergency Setup
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (locale == "hi") "स्वास्थ्य स्थिति और केयरगिवर" else "Medical Profile & Caregiver SOS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (locale == "hi") "आपातकालीन स्थिति में तुरंत परिवार को ऑफलाइन एसएमएस भेजा जाएगा।" else "Direct cellular SMS alerts are sent to caregiver if duplicate doses occur.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            fontSize = 13.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Existing Health Condition Chips
                    val conditionsList = listOf(
                        "Type-2 Diabetes",
                        "Hypertension (BP)",
                        "Thyroid Disorder",
                        "Cardiac / Heart Condition"
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        conditionsList.forEach { cond ->
                            val isSelected = selectedConditions.contains(cond)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedConditions.remove(cond)
                                        else selectedConditions.add(cond)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) SurfaceCardElevated else SurfaceCardDark
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, SafeGreen) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cond, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(20.dp))
                                    }
                                }
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
                                patientNameInput.isNotBlank() -> SafeGreen
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
                                    patientNameInput.isNotBlank() -> SafeGreen
                                    else -> TextMuted
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (patientNameInput.isEmpty()) {
                                    Text(
                                        text = if (locale == "hi") "रोगी का नाम दर्ज करें" else "Enter patient name",
                                        color = TextMuted.copy(alpha = 0.7f),
                                        fontSize = 13.sp
                                    )
                                }
                                BasicTextField(
                                    value = patientNameInput,
                                    onValueChange = {
                                        patientNameInput = it
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
                            if (patientNameInput.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        patientNameInput = ""
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

                    // Caregiver Phone Label
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
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Footer: Next / Complete Button
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (currentStep < 3) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentStep++
                    } else {
                        val trimmedName = patientNameInput.trim()
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

                            viewModel.updateCaregiverInfo(trimmedName, normalizedPhone)
                            onOnboardingComplete()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (currentStep < 3) {
                            if (locale == "hi") "आगे बढ़ें (Next)" else "Continue (आगे बढ़ें)"
                        } else {
                            if (locale == "hi") "शुरू करें (Get Started)" else "Get Started (शुरू करें)"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (currentStep < 3) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (currentStep > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { currentStep-- },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BackgroundCharcoal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (locale == "hi") "पीछे जाएं (Back)" else "Back",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
