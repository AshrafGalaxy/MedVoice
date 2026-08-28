package com.medvoice.feature.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    onBackToScanner: () -> Unit
) {
    val logs by viewModel.medicationLogs.collectAsState()
    val locale by viewModel.selectedLocale.collectAsState()
    val caregiverPhone by viewModel.caregiverPhone.collectAsState()
    val patientName by viewModel.patientName.collectAsState()
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCharcoal)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToScanner) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (locale == "hi") "दैनिक दवा ऑडिट लॉग" else "Daily Medication Audit Log",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                    Text(
                        text = "$patientName • " + if (locale == "hi") "केयरगिवर डैशबोर्ड" else "Caregiver Dashboard",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            IconButton(onClick = { viewModel.clearLogs() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Logs",
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SOS Safety Guardrail Banner
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
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (locale == "hi") "100% ऑन-डिवाइस एज सुरक्षा सक्रिय" else "100% On-Device Edge Safety Active",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = SafeGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = if (locale == "hi") "आपातकालीन अलर्ट सीधे सेलुलर एसएमएस द्वारा भेजे जाते हैं ($caregiverPhone)" else "Emergency SOS alerts route via Cellular SMS ($caregiverPhone)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Log List
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (locale == "hi") "आज का कोई रिकॉर्ड नहीं है।\nस्कैनर पर जाकर दवा स्कैन करें।" else "No logs recorded today.\nScan medicines on the camera scanner to log.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TextMuted,
                        fontSize = 17.sp
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
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

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
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
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = log.scannedBrandName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    when (log.status) {
                                        "TAKEN" -> StatusBadge(
                                            text = if (locale == "hi") "ले ली (Confirmed)" else "Taken (Confirmed)",
                                            statusType = StatusType.SAFE
                                        )
                                        "BLOCKED_DUPLICATE" -> StatusBadge(
                                            text = if (locale == "hi") "डबल डोज ब्लॉक (SOS Sent)" else "Duplicate Blocked (SOS Sent)",
                                            statusType = StatusType.DANGER
                                        )
                                        "CONFLICT_WARNED" -> StatusBadge(
                                            text = if (locale == "hi") "ड्रग कॉन्फ्लिक्ट चेतावनी" else "Conflict Warned",
                                            statusType = StatusType.WARNING
                                        )
                                        else -> Text(text = log.status, color = TextMuted, fontSize = 12.sp)
                                    }
                                }
                            }

                            Text(
                                text = timeFormatter.format(Date(log.intakeTimestamp)),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Return to Camera Scanner Button
        Button(
            onClick = onBackToScanner,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (locale == "hi") "कैमरा स्कैनर पर वापस जाएं" else "Return to Camera Scanner",
                color = TextWhite,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
