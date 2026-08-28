package com.medvoice.feature.history

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medvoice.feature.scanner.ScanViewModel
import com.medvoice.ui.components.StatusBadge
import com.medvoice.ui.components.StatusType
import com.medvoice.ui.theme.AlertRed
import com.medvoice.ui.theme.BackgroundCharcoal
import com.medvoice.ui.theme.SafeGreen
import com.medvoice.ui.theme.SurfaceCardDark
import com.medvoice.ui.theme.SurfaceCardElevated
import com.medvoice.ui.theme.TextMuted
import com.medvoice.ui.theme.TextWhite
import java.text.SimpleDateFormat
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
    val haptic = LocalHapticFeedback.current
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCharcoal)
            .padding(16.dp)
    ) {
        // 1. Header Row
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
                        text = if (locale == "hi") "केयरगिवर ऑडिट लॉग" else "Caregiver Audit Log",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (locale == "hi") "रोगी: $patientName" else "Patient: $patientName",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (logs.isNotEmpty()) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.clearLogs()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Logs",
                        tint = TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. SOS Safety Guardrail Card
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
                    contentDescription = "Safety Guardrail",
                    tint = SafeGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (locale == "hi") "100% ऑन-डिवाइस एज सुरक्षा सक्रिय" else "100% On-Device Edge Safety Active",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = SafeGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                    Text(
                        text = if (locale == "hi") "आपातकालीन अलर्ट सीधे सेलुलर एसएमएस द्वारा भेजे जाते हैं ($caregiverPhone)" else "Emergency SOS alerts route via Cellular SMS ($caregiverPhone)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Log List
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (locale == "hi") "आज का कोई रिकॉर्ड नहीं है।" else "No medication intake logs recorded today.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (locale == "hi") "दवा पट्टी को कैमरे के सामने स्कैन करें" else "Scan blister pack on camera to log intake",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 13.sp
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    val isTaken = log.status == "TAKEN"
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
                                    imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isTaken) SafeGreen else AlertRed,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.scannedBrandName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatusBadge(
                                            text = if (isTaken) (if (locale == "hi") "स्वीकृत" else "TAKEN") else (if (locale == "hi") "अवरुद्ध" else "BLOCKED"),
                                            statusType = if (isTaken) StatusType.SAFE else StatusType.DANGER
                                        )
                                        if (log.voiceConfirmed) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "Voice Confirmed",
                                                    tint = SafeGreen,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "Voice",
                                                    color = SafeGreen,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        if (log.sosSmsDispatched) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Phone,
                                                    contentDescription = "SMS Dispatched",
                                                    tint = AlertRed,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "SOS",
                                                    color = AlertRed,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = timeFormatter.format(Date(log.intakeTimestamp)),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Consistent, Elegant Return to Scanner Button (Balanced ~48dp height)
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
