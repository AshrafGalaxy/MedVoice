package com.medvoice.feature.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.medvoice.feature.navigation.MedVoiceTab
import com.medvoice.feature.scanner.ScanViewModel
import com.medvoice.ui.components.MedVoiceLogo
import com.medvoice.ui.components.StatusBadge
import com.medvoice.ui.components.StatusType
import com.medvoice.ui.components.TimeOfDayIcon
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
import java.util.Calendar

@Composable
fun HomeScreen(viewModel: ScanViewModel) {
    val locale by viewModel.selectedLocale.collectAsState()
    val caregiverPhone by viewModel.caregiverPhone.collectAsState()
    val patientName by viewModel.patientName.collectAsState()
    val logs by viewModel.medicationLogs.collectAsState()
    val cabinetMedicines by viewModel.cabinetMedicines.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

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

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingText = when {
        currentHour < 12 -> if (locale == "hi") "शुभ प्रभात" else "Good Morning"
        currentHour < 17 -> if (locale == "hi") "शुभ दोपहर" else "Good Afternoon"
        else -> if (locale == "hi") "शुभ संध्या" else "Good Evening"
    }

    val conflictCount = logs.count { it.status.startsWith("BLOCKED") || it.status.startsWith("CONFLICT") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCharcoal)
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        // 1. Responsive Top Bar: Logo, Greeting, and Patient Name
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MedVoiceLogo(size = 44)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeOfDayIcon(hour = currentHour)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = greetingText,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = ReticleCyan,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
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
                        fontSize = 18.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Hero Action Card: Point & Scan Strip
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = SafeGreen.copy(alpha = 0.2f),
                    ambientColor = Color.Black.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.navigateToTab(MedVoiceTab.SCANNER)
                },
            color = SurfaceCardDark,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF10B981), Color(0xFF047857))
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFF34D399).copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CenterFocusStrong,
                        contentDescription = "Scan",
                        tint = TextWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (locale == "hi") "दवा पट्टी स्कैन करें" else "Point & Scan Any Medicine",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (locale == "hi") "तुरंत भारतीय आवाज में निर्देश सुनें" else "Live camera OCR & vernacular voice guidance",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(SafeGreen.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        if (conflictCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AlertRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = AlertRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (locale == "hi") "सुरक्षा अलर्ट: $conflictCount परस्परविरोध" else "Safety Alert: $conflictCount Hazards",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = AlertRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = if (locale == "hi") "असुरक्षित खुराक स्वतः अवरुद्ध" else "Unsafe intake blocked",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                    StatusBadge(
                        text = if (locale == "hi") "$conflictCount अवरुद्ध" else "$conflictCount Blocked",
                        statusType = StatusType.DANGER
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3.5 Daily Voice Alarms & Reminder Card (AlarmManager)
        val isRemindersEnabled by viewModel.isDailyRemindersEnabled.collectAsState()
        val timeSlots by viewModel.prescriptionTimeSlots.collectAsState()

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
                                text = if (locale == "hi") "समय पर बोलकर दवा याद दिलाएगा" else "Auto-announces prescription times aloud",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Switch(
                        checked = isRemindersEnabled,
                        onCheckedChange = { viewModel.toggleDailyReminders(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = SafeGreen
                        )
                    )
                }

                if (isRemindersEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (locale == "hi") "दवा घोषणा समय (समय बदलने के लिए टैप करें)" else "Prescription Announcement Slots (Tap to Edit)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2x2 Interactive Time Slots Grid
                    val chunkedSlots = timeSlots.chunked(2)
                    chunkedSlots.forEach { rowSlots ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSlots.forEach { slot ->
                                val (slotIcon, iconTint) = when (slot.slotId) {
                                    "morning" -> Icons.Default.WbSunny to Color(0xFFFBBF24)
                                    "afternoon" -> Icons.Default.LightMode to Color(0xFFF59E0B)
                                    "evening" -> Icons.Default.NightsStay to ReticleCyan
                                    else -> Icons.Default.Bedtime to Color(0xFF818CF8)
                                }

                                val isPm = slot.hour >= 12
                                val displayHour = when {
                                    slot.hour == 0 -> 12
                                    slot.hour > 12 -> slot.hour - 12
                                    else -> slot.hour
                                }
                                val formattedTime = String.format(java.util.Locale.ROOT, "%02d:%02d %s", displayHour, slot.minute, if (isPm) "PM" else "AM")
                                val slotTitle = if (locale == "hi") slot.titleHi else slot.titleEn

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            android.app.TimePickerDialog(
                                                context,
                                                { _, selectedHour, selectedMinute ->
                                                    viewModel.updateSlotTime(slot.slotId, selectedHour, selectedMinute)
                                                },
                                                slot.hour,
                                                slot.minute,
                                                false
                                            ).show()
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceCardElevated,
                                    border = BorderStroke(1.dp, AccentBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .background(iconTint.copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = slotIcon,
                                                    contentDescription = null,
                                                    tint = iconTint,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = slotTitle,
                                                    color = TextWhite,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = formattedTime,
                                                    color = SafeGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Time",
                                            tint = TextMuted.copy(alpha = 0.6f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.triggerTestAlarm()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReticleCyan.copy(alpha = 0.14f),
                            contentColor = ReticleCyan
                        ),
                        border = BorderStroke(1.dp, ReticleCyan.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = ReticleCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (locale == "hi") "अलार्म आवाज जांचें (2s Preview)" else "Test Spoken Alarm (2s)",
                            color = ReticleCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
        if (cabinetMedicines.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AccentBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(ReticleCyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = ReticleCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (locale == "hi") "कोई सक्रिय दवा समय-सारणी में नहीं है" else "No Active Medications in Schedule",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (locale == "hi") "दवा पट्टी को कैमरे से स्कैन करके अपनी दैनिक समय-सारणी में जोड़ें" else "Scan your medicine blister pack on camera to add it to your daily schedule",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 12.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.navigateToTab(MedVoiceTab.SCANNER)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (locale == "hi") "दवा स्कैन करें" else "Scan Medicine to Add",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            cabinetMedicines.take(4).forEach { med ->
                val isTaken = logs.any { it.scannedBrandName.contains(med.brandName.split(" ").first(), ignoreCase = true) && it.status == "TAKEN" }
                val foodTimingRule = med.manufacturer?.ifBlank { "AFTER_FOOD" } ?: "AFTER_FOOD"
                val slotTiming = when (foodTimingRule) {
                    "BEFORE_FOOD", "BEFORE_BREAKFAST", "EMPTY_STOMACH" -> if (locale == "hi") "🌅 खाली पेट / भोजन से पहले" else "🌅 Empty Stomach / Before Food"
                    "AFTER_BREAKFAST" -> if (locale == "hi") "🌅 सुबह • नाश्ते के बाद" else "🌅 Morning • After Breakfast"
                    "AFTER_LUNCH" -> if (locale == "hi") "☀️ दोपहर • भोजन के बाद" else "☀️ Afternoon • After Lunch"
                    "BEFORE_DINNER" -> if (locale == "hi") "🌙 संध्या • भोजन से पहले" else "🌙 Evening • Before Dinner"
                    "AFTER_DINNER" -> if (locale == "hi") "🌙 रात • भोजन के बाद" else "🌙 Night • After Food"
                    "BEDTIME" -> if (locale == "hi") "💤 सोते समय • पानी के साथ" else "💤 Bedtime • With Water"
                    else -> if (locale == "hi") "🍽️ भोजन के बाद लें" else "🍽️ Take After Food"
                }

                val formLabel = when (med.dosageForm ?: "TABLET") {
                    "EYE_DROPS" -> if (locale == "hi") "👁️ आई ड्रॉप्स" else "👁️ Eye Drops"
                    "SYRUP" -> if (locale == "hi") "🧪 सिरप" else "🧪 Syrup"
                    "GEL" -> if (locale == "hi") "🧴 जेल" else "🧴 Gel"
                    else -> if (locale == "hi") "💊 खाने की गोली (Tablet)" else "💊 Oral Tablet"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTaken) SurfaceCardElevated.copy(alpha = 0.5f) else SurfaceCardDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isTaken) SafeGreen.copy(alpha = 0.4f) else AccentBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = if (isTaken) SafeGreen.copy(alpha = 0.15f) else ReticleCyan.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = formLabel,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isTaken) SafeGreen else ReticleCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    )
                                }

                                Text(
                                    text = slotTiming,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = med.brandName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = med.rawComposition,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        if (isTaken) {
                            StatusBadge(
                                text = if (locale == "hi") "ले ली गई" else "✓ TAKEN",
                                statusType = StatusType.SAFE
                            )
                        } else {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.navigateToTab(MedVoiceTab.SCANNER)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(38.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = TextWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
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

        // 5. Caregiver Emergency SOS Status Card with Live SMS Permission Check
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (caregiverPhone.isNotBlank()) SafeGreen.copy(alpha = 0.35f) else AccentBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SafeGreen.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Caregiver SOS",
                                tint = SafeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (locale == "hi") "केयरगिवर आपातकालीन SOS" else "Caregiver Emergency SOS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                            Text(
                                text = if (caregiverPhone.isNotBlank()) caregiverPhone else (if (locale == "hi") "नंबर सेट नहीं है (सेटिंग्स में जोड़ें)" else "No phone configured (Tap Settings)"),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (caregiverPhone.isNotBlank()) SafeGreen else TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Surface(
                        color = if (caregiverPhone.isNotBlank() && hasSmsPermission) SafeGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (caregiverPhone.isNotBlank() && hasSmsPermission) "✓ ACTIVE" else "CONFIG",
                            color = if (caregiverPhone.isNotBlank() && hasSmsPermission) SafeGreen else WarningAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (caregiverPhone.isBlank()) {
                            viewModel.navigateToTab(MedVoiceTab.SETTINGS)
                        } else if (!hasSmsPermission) {
                            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                        } else {
                            viewModel.testEmergencySms()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (caregiverPhone.isBlank()) SurfaceCardElevated else if (!hasSmsPermission) WarningAmber.copy(alpha = 0.2f) else SafeGreen.copy(alpha = 0.18f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (caregiverPhone.isBlank()) AccentBorder else if (!hasSmsPermission) WarningAmber.copy(alpha = 0.6f) else SafeGreen.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = if (caregiverPhone.isBlank()) Icons.Default.Edit else Icons.Default.Phone,
                        contentDescription = null,
                        tint = if (caregiverPhone.isBlank()) TextMuted else if (!hasSmsPermission) WarningAmber else SafeGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (caregiverPhone.isBlank()) {
                            if (locale == "hi") "फोन नंबर सेट करें (Settings)" else "Configure Caregiver Phone"
                        } else if (!hasSmsPermission) {
                            if (locale == "hi") "SMS अनुमति दें (Grant Access)" else "Grant Cellular SMS Permission"
                        } else {
                            if (locale == "hi") "आपातकालीन SMS भेजें (Test SOS)" else "Dispatch Test Emergency SOS SMS"
                        },
                        color = if (caregiverPhone.isBlank()) TextWhite else if (!hasSmsPermission) WarningAmber else TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
