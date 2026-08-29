package com.medvoice.feature.history

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medvoice.core.data.local.entity.MedicationLogEntity
import com.medvoice.feature.scanner.ScanViewModel
import com.medvoice.ui.components.StatusBadge
import com.medvoice.ui.components.StatusType
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CaregiverAuditScreen(
    viewModel: ScanViewModel,
    onBackToScanner: () -> Unit = {}
) {
    val logs by viewModel.medicationLogs.collectAsState()
    val locale by viewModel.selectedLocale.collectAsState()
    val caregiverPhone by viewModel.caregiverPhone.collectAsState()
    val patientName by viewModel.patientName.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val takenCount = logs.count { it.status == "TAKEN" }
    val blockedCount = logs.count { it.status.startsWith("BLOCKED") || it.status.startsWith("CONFLICT") }
    val sosCount = logs.count { it.sosSmsDispatched }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCharcoal)
            .padding(16.dp)
    ) {
        // 1. Header Row: Title, Patient Name & Action Buttons (Share, Clear)
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
                        .size(38.dp)
                        .background(SafeGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Audit",
                        tint = SafeGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (locale == "hi") "दवा ऑडिट और इतिहास" else "Medication Audit & History",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (locale == "hi") "रोगी: $patientName" else "Patient: $patientName",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (logs.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            shareAuditReport(context, patientName, caregiverPhone, logs, locale)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Report",
                            tint = ReticleCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.clearLogs()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Logs",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Summary KPI Metrics Bar (Taken, Blocked, SOS)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Taken KPI
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = SurfaceCardElevated,
                border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(SafeGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "$takenCount",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (locale == "hi") "खुराक ली" else "Taken",
                            color = SafeGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Blocked KPI
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = SurfaceCardElevated,
                border = BorderStroke(1.dp, if (blockedCount > 0) AlertRed.copy(alpha = 0.4f) else AccentBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (blockedCount > 0) AlertRed.copy(alpha = 0.2f) else TextMuted.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = if (blockedCount > 0) AlertRed else TextMuted, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "$blockedCount",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (locale == "hi") "सुरक्षा ब्लॉक" else "Guarded",
                            color = if (blockedCount > 0) AlertRed else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // SOS KPI
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = SurfaceCardElevated,
                border = BorderStroke(1.dp, if (sosCount > 0) ReticleCyan.copy(alpha = 0.4f) else AccentBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (sosCount > 0) ReticleCyan.copy(alpha = 0.2f) else TextMuted.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = if (sosCount > 0) ReticleCyan else TextMuted, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "$sosCount",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (locale == "hi") "SOS भेजे" else "SOS Alerts",
                            color = if (sosCount > 0) ReticleCyan else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Log List with Date & Time stamps
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (locale == "hi") "कोई दवा रिकॉर्ड दर्ज नहीं है।" else "No medication intake logs recorded.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (locale == "hi") "दवा की पर्ची या शीशी को कैमरे से स्कैन करें" else "Scan medicine packaging on camera to log verified intake",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 12.5.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { log ->
                    val isTaken = log.status == "TAKEN"
                    val formattedDateTime = formatLogDateTime(log.intakeTimestamp, locale)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isTaken) SafeGreen.copy(alpha = 0.3f) else AlertRed.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Brand Name + Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isTaken) SafeGreen else AlertRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = log.scannedBrandName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.5.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                StatusBadge(
                                    text = if (isTaken) {
                                        if (locale == "hi") "स्वीकृत" else "TAKEN"
                                    } else if (log.status.contains("DUPLICATE")) {
                                        if (locale == "hi") "अवरुद्ध • अतिरिक्त खुराक" else "BLOCKED • DUPLICATE"
                                    } else if (log.status.contains("EXPIRED")) {
                                        if (locale == "hi") "अवरुद्ध • समाप्त दवा" else "BLOCKED • EXPIRED"
                                    } else {
                                        if (locale == "hi") "अवरुद्ध • परस्परविरोध" else "BLOCKED • CONFLICT"
                                    },
                                    statusType = if (isTaken) StatusType.SAFE else StatusType.DANGER
                                )
                            }

                            // Middle Row: Active Chemical Formulation Subtitle
                            if (log.parsedSalts.isNotBlank() && log.parsedSalts != log.scannedBrandName) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = log.parsedSalts,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom Row: Date & Time Stamp + Tags (Voice, SOS)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Full Date & Time
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = "Timestamp",
                                        tint = ReticleCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = formattedDateTime,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = ReticleCyan,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }

                                // Tags
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (log.voiceConfirmed) {
                                        Surface(
                                            color = SafeGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Mic, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("Voice", color = SafeGreen, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (log.sosSmsDispatched) {
                                        Surface(
                                            color = AlertRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Phone, contentDescription = null, tint = AlertRed, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("SOS SMS", color = AlertRed, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Return to Camera Scanner Button (48dp height)
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onBackToScanner()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = TextWhite,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (locale == "hi") "कैमरा स्कैनर पर जाएं" else "Open Camera Scanner",
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Human-Friendly Date & Timestamp Formatter (e.g. "Today • 05:30 PM", "Yesterday • 09:15 PM", "28 Aug 2026 • 04:12 PM")
 */
private fun formatLogDateTime(timestampMs: Long, locale: String): String {
    if (timestampMs <= 0) return "--:--"

    val logCalendar = Calendar.getInstance().apply { timeInMillis = timestampMs }
    val todayCalendar = Calendar.getInstance()

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US).format(Date(timestampMs))

    val isSameDay = logCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
            logCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)

    if (isSameDay) {
        val todayLabel = if (locale == "hi") "आज" else "Today"
        return "$todayLabel • $timeFormat"
    }

    val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = logCalendar.get(Calendar.YEAR) == yesterdayCalendar.get(Calendar.YEAR) &&
            logCalendar.get(Calendar.DAY_OF_YEAR) == yesterdayCalendar.get(Calendar.DAY_OF_YEAR)

    if (isYesterday) {
        val yesterdayLabel = if (locale == "hi") "कल" else "Yesterday"
        return "$yesterdayLabel • $timeFormat"
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(timestampMs))
    return "$dateFormat • $timeFormat"
}

/**
 * Exports and shares the clinical medication intake audit report via Android Sharesheet
 */
private fun shareAuditReport(
    context: Context,
    patientName: String,
    caregiverPhone: String,
    logs: List<MedicationLogEntity>,
    locale: String
) {
    try {
        val sb = StringBuilder()
        sb.append("📋 MEDVOICE CLINICAL MEDICATION AUDIT REPORT\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Patient: $patientName\n")
        if (caregiverPhone.isNotBlank()) sb.append("Caregiver SOS Contact: $caregiverPhone\n")
        sb.append("Generated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

        logs.forEachIndexed { index, log ->
            val statusTag = if (log.status == "TAKEN") "[✓ TAKEN]" else "[⚠️ BLOCKED: ${log.status}]"
            val dt = formatLogDateTime(log.intakeTimestamp, "en")
            sb.append("${index + 1}. $statusTag ${log.scannedBrandName}\n")
            if (log.parsedSalts.isNotBlank() && log.parsedSalts != log.scannedBrandName) {
                sb.append("   Composition: ${log.parsedSalts}\n")
            }
            sb.append("   Timestamp: $dt\n")
            if (log.voiceConfirmed) sb.append("   Voice Confirmed: Yes\n")
            if (log.sosSmsDispatched) sb.append("   Emergency SOS SMS: Dispatched\n")
            sb.append("\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Report secured on-device via MedVoice Edge AI.")

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "MedVoice Medication Audit Report - $patientName")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(sendIntent, "Share Clinical Audit Report")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
