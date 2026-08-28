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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medvoice.feature.navigation.MedVoiceTab
import com.medvoice.feature.scanner.ScanViewModel
import com.medvoice.ui.components.MedVoiceLogo
import com.medvoice.ui.components.StatusBadge
import com.medvoice.ui.components.StatusType
import com.medvoice.ui.components.TimeOfDayIcon
import com.medvoice.ui.theme.BackgroundCharcoal
import com.medvoice.ui.theme.ReticleCyan
import com.medvoice.ui.theme.SafeGreen
import com.medvoice.ui.theme.SurfaceCardDark
import com.medvoice.ui.theme.SurfaceCardElevated
import com.medvoice.ui.theme.TextMuted
import com.medvoice.ui.theme.TextWhite
import java.util.Calendar

@Composable
fun HomeScreen(viewModel: ScanViewModel) {
    val locale by viewModel.selectedLocale.collectAsState()
    val caregiverPhone by viewModel.caregiverPhone.collectAsState()
    val patientName by viewModel.patientName.collectAsState()
    val logs by viewModel.medicationLogs.collectAsState()
    val allMedicines by viewModel.allMedicines.collectAsState()
    val haptic = LocalHapticFeedback.current

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingText = when {
        currentHour < 12 -> if (locale == "hi") "शुभ प्रभात" else "Good Morning"
        currentHour < 17 -> if (locale == "hi") "शुभ दोपहर" else "Good Afternoon"
        else -> if (locale == "hi") "शुभ संध्या" else "Good Evening"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCharcoal)
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        // 1. Responsive Top Bar: Logo, Greeting, Patient Name & Language Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MedVoiceLogo(size = 40)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TimeOfDayIcon(hour = currentHour)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = greetingText,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = ReticleCyan,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = patientName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Language Switcher Pills
            Row(
                modifier = Modifier
                    .background(SurfaceCardElevated, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (locale == "en") SafeGreen else BackgroundCharcoal.copy(alpha = 0f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.setLocale("en") }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text("EN", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (locale == "hi") SafeGreen else BackgroundCharcoal.copy(alpha = 0f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.setLocale("hi") }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text("हिंदी", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Hero Action Card: Point & Scan Strip
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, SafeGreen, RoundedCornerShape(16.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.navigateToTab(MedVoiceTab.SCANNER)
                },
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(SafeGreen, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan",
                        tint = TextWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (locale == "hi") "दवा पट्टी स्कैन करें" else "Point & Scan Strip",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = if (locale == "hi") "तुरंत भारतीय आवाज में निर्देश सुनें" else "Instant vernacular audio guidance",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Safety Status Guardrail Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Security",
                    tint = SafeGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (locale == "hi") "सुरक्षा स्थिति: सुरक्षित" else "Safety Status: Active",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = SafeGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        StatusBadge(
                            text = if (locale == "hi") "0 परस्परविरोध" else "0 Conflicts",
                            statusType = StatusType.SAFE
                        )
                    }
                    Text(
                        text = if (locale == "hi") "100% ऑन-डिवाइस एज सुरक्षा • डुप्लिकेट साल्ट ट्रैप सक्रिय" else "100% On-Device Edge Safety • Duplicate Salt Traps Active",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3.5 Daily Voice Alarms & Reminder Card (AlarmManager)
        val isRemindersEnabled by viewModel.isDailyRemindersEnabled.collectAsState()
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(ReticleCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = ReticleCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (locale == "hi") "दैनिक आवाज अलार्म (अलर्ट)" else "Daily Spoken Voice Alarms",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                            Text(
                                text = if (locale == "hi") "समय पर बोलकर दवा याद दिलाएगा (7 AM, 8:30 AM, 1:30 PM, 8 PM)" else "Auto-announces prescription times aloud",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    androidx.compose.material3.Switch(
                        checked = isRemindersEnabled,
                        onCheckedChange = { viewModel.toggleDailyReminders(it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = SafeGreen
                        )
                    )
                }

                if (isRemindersEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (locale == "hi") "अलार्म परीक्षण (2 सेकंड में बोलेगा):" else "Test Alarm (Plays in 2s):",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.triggerTestAlarm()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ReticleCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = BackgroundCharcoal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (locale == "hi") "परीक्षण अलार्म" else "Test Alarm",
                                color = BackgroundCharcoal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Dynamic Daily Routine Section (Loaded from Room Database!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (locale == "hi") "आज की दवा समय-सारणी" else "Today's Medication Schedule",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            )
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = ReticleCyan,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Render Dynamic Medication Schedule from Room
        if (allMedicines.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (locale == "hi") "दवाएं लोड हो रही हैं..." else "Loading prescriptions from master database...",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            allMedicines.take(4).forEach { med ->
                val isTaken = logs.any { it.scannedBrandName.contains(med.brand_name.split(" ").first(), ignoreCase = true) && it.status == "TAKEN" }
                val instruction = if (locale == "hi") med.vernacular_instruction_hi else med.vernacular_instruction_en
                val timing = if (locale == "hi") med.rule_code else med.rule_code

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTaken) SurfaceCardElevated.copy(alpha = 0.6f) else SurfaceCardDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = timing,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isTaken) SafeGreen else ReticleCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = med.brand_name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = instruction,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 12.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isTaken) {
                            StatusBadge(
                                text = if (locale == "hi") "ले ली" else "Taken",
                                statusType = StatusType.SAFE
                            )
                        } else {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.simulateScan(med.brand_name)
                                    viewModel.navigateToTab(MedVoiceTab.SCANNER)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Caregiver Emergency SOS Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Caregiver",
                        tint = SafeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (locale == "hi") "केयरगिवर आपातकालीन SOS" else "Caregiver Emergency SOS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = caregiverPhone,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMuted,
                                fontSize = 12.sp
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
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = if (locale == "hi") "परीक्षण SOS" else "Test SOS",
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
