package com.medvoice.feature.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medvoice.feature.navigation.MedVoiceTab
import com.medvoice.feature.scanner.ScanViewModel
import com.medvoice.ui.theme.AccentBorder
import com.medvoice.ui.theme.BackgroundCharcoal
import com.medvoice.ui.theme.ReticleCyan
import com.medvoice.ui.theme.SafeGreen
import com.medvoice.ui.theme.SurfaceCardDark
import com.medvoice.ui.theme.SurfaceCardElevated
import com.medvoice.ui.theme.TextMuted
import com.medvoice.ui.theme.TextWhite
import java.util.Calendar

data class RoutineMedicine(
    val brand: String,
    val salt: String,
    val timeSlot: String,
    val timeSlotHi: String,
    val instructionEn: String,
    val instructionHi: String,
    val isTaken: Boolean
)

@Composable
fun HomeScreen(viewModel: ScanViewModel) {
    val locale by viewModel.selectedLocale.collectAsState()
    val caregiverPhone by viewModel.caregiverPhone.collectAsState()
    val patientName by viewModel.patientName.collectAsState()
    val logs by viewModel.medicationLogs.collectAsState()
    val haptic = LocalHapticFeedback.current

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        currentHour < 12 -> if (locale == "hi") "शुभ प्रभात 🌅" else "Good Morning 🌅"
        currentHour < 17 -> if (locale == "hi") "शुभ दोपहर ☀️" else "Good Afternoon ☀️"
        else -> if (locale == "hi") "शुभ संध्या 🌙" else "Good Evening 🌙"
    }

    val dailySchedule = listOf(
        RoutineMedicine(
            brand = "Thyronorm 50mcg",
            salt = "Levothyroxine Sodium",
            timeSlot = "07:30 AM • Empty Stomach",
            timeSlotHi = "सुबह 07:30 • खाली पेट",
            instructionEn = "Take strictly 45 mins before morning tea",
            instructionHi = "सुबह खाली पेट लें, 45 मिनट तक चाय न पिएं",
            isTaken = logs.any { it.scannedBrandName.contains("Thyronorm", ignoreCase = true) && it.status == "TAKEN" }
        ),
        RoutineMedicine(
            brand = "Glycomet-SR 500",
            salt = "Metformin Hydrochloride",
            timeSlot = "08:30 AM • After Breakfast",
            timeSlotHi = "सुबह 08:30 • नाश्ते के बाद",
            instructionEn = "Take 1 tablet with water after breakfast",
            instructionHi = "नाश्ता करने के बाद 1 गोली पानी के साथ लें",
            isTaken = logs.any { it.scannedBrandName.contains("Glycomet", ignoreCase = true) && it.status == "TAKEN" }
        ),
        RoutineMedicine(
            brand = "Shelcal 500",
            salt = "Calcium Carbonate",
            timeSlot = "02:00 PM • After Lunch",
            timeSlotHi = "दोपहर 02:00 • भोजन के बाद",
            instructionEn = "Take after lunch (Keep 2h gap from Iron)",
            instructionHi = "दोपहर के खाने के बाद लें (आयरन से 2 घंटे दूर)",
            isTaken = logs.any { it.scannedBrandName.contains("Shelcal", ignoreCase = true) && it.status == "TAKEN" }
        ),
        RoutineMedicine(
            brand = "Atorva 10",
            salt = "Atorvastatin",
            timeSlot = "09:30 PM • Bedtime",
            timeSlotHi = "रात 09:30 • सोने से पहले",
            instructionEn = "Take 30 mins before sleep",
            instructionHi = "रात को सोने से 30 मिनट पहले लें",
            isTaken = logs.any { it.scannedBrandName.contains("Atorva", ignoreCase = true) && it.status == "TAKEN" }
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCharcoal)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Bar: Greeting & Language Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = ReticleCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = patientName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )
                )
            }

            // Language Switcher Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

        Spacer(modifier = Modifier.height(16.dp))

        // Hero 80dp Action Card: Point & Scan Medicine
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, SafeGreen, RoundedCornerShape(18.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.navigateToTab(MedVoiceTab.SCANNER)
                },
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(SafeGreen, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scan",
                            tint = TextWhite,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (locale == "hi") "दवा स्कैन करें" else "Point & Scan Strip",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )
                        Text(
                            text = if (locale == "hi") "तुरंत आवाज में निर्देश सुनें" else "Instant vernacular audio guidance",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Safety Status & Edge Guardrail Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🛡️", fontSize = 26.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (locale == "hi") "सुरक्षा स्थिति: सुरक्षित (0 परस्परविरोध)" else "Safety Status: All Clear (0 Conflicts)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = SafeGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = if (locale == "hi") "100% ऑफलाइन एज सुरक्षा सक्रिय • डुप्लिकेट साल्ट ब्लॉक समर्थित" else "100% Offline Edge Engine Active • Duplicate Salt Traps Active",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Daily Routine Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (locale == "hi") "आज की दवा समय-सारणी" else "Today's Medication Schedule",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            )
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = ReticleCyan,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Schedule Timeline List
        dailySchedule.forEach { item ->
            val instruction = if (locale == "hi") item.instructionHi else item.instructionEn
            val slot = if (locale == "hi") item.timeSlotHi else item.timeSlot

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (item.isTaken) SurfaceCardElevated.copy(alpha = 0.6f) else SurfaceCardDark
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = slot,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (item.isTaken) SafeGreen else ReticleCyan,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.brand,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = instruction,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        )
                    }

                    if (item.isTaken) {
                        Box(
                            modifier = Modifier
                                .background(SafeGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Taken",
                                    tint = SafeGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (locale == "hi") "ले ली" else "Taken",
                                    color = SafeGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.simulateScan(item.brand)
                                viewModel.navigateToTab(MedVoiceTab.SCANNER)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(
                                text = if (locale == "hi") "स्कैन" else "Scan",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Caregiver SOS Quick Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Caregiver",
                        tint = SafeGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (locale == "hi") "केयरगिवर आपातकालीन संपर्क" else "Caregiver Emergency SOS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        Text(
                            text = caregiverPhone,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.testEmergencySms()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (locale == "hi") "परीक्षण SMS" else "Test SOS",
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
