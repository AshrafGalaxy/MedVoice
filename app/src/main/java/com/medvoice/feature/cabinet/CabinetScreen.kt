package com.medvoice.feature.cabinet

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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.medvoice.feature.scanner.ScanViewModel
import com.medvoice.ui.theme.AccentBorder
import com.medvoice.ui.theme.BackgroundCharcoal
import com.medvoice.ui.theme.ReticleCyan
import com.medvoice.ui.theme.SafeGreen
import com.medvoice.ui.theme.SurfaceCardDark
import com.medvoice.ui.theme.SurfaceCardElevated
import com.medvoice.ui.theme.TextMuted
import com.medvoice.ui.theme.TextWhite

data class CabinetMedicineItem(
    val brand: String,
    val salt: String,
    val therapeuticClass: String,
    val timingRuleEn: String,
    val timingRuleHi: String,
    val usageEn: String,
    val usageHi: String,
    val daysRemaining: Int
)

@Composable
fun CabinetScreen(viewModel: ScanViewModel) {
    val locale by viewModel.selectedLocale.collectAsState()
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }

    val masterCabinet = listOf(
        CabinetMedicineItem(
            brand = "Thyronorm 50mcg",
            salt = "Levothyroxine Sodium",
            therapeuticClass = "THYROID",
            timingRuleEn = "Strictly 45 mins before morning tea/breakfast",
            timingRuleHi = "सुबह खाली पेट, 45 मिनट तक चाय या नाश्ता न करें",
            usageEn = "This is your thyroid tablet to be taken early morning on an empty stomach.",
            usageHi = "यह सुबह खाली पेट लेने वाली थायराइड की गोली है।",
            daysRemaining = 24
        ),
        CabinetMedicineItem(
            brand = "Glycomet-SR 500",
            salt = "Metformin Hydrochloride",
            therapeuticClass = "ANTIDIABETIC",
            timingRuleEn = "Take 1 tablet with water after your meal",
            timingRuleHi = "खाना खाने के बाद 1 गोली पानी के साथ लें",
            usageEn = "This is your diabetes blood sugar tablet. Take after breakfast.",
            usageHi = "यह आपकी शुगर की गोली है। नाश्ते के बाद लें।",
            daysRemaining = 12
        ),
        CabinetMedicineItem(
            brand = "Telma 40",
            salt = "Telmisartan",
            therapeuticClass = "ANTIHYPERTENSIVE",
            timingRuleEn = "Take once daily in the morning with water",
            timingRuleHi = "रोजाना सुबह 1 गोली पानी के साथ लें",
            usageEn = "This is your blood pressure and cardiac protection medication.",
            usageHi = "यह ब्लड प्रेशर और हृदय सुरक्षा की दवा है।",
            daysRemaining = 18
        ),
        CabinetMedicineItem(
            brand = "Shelcal 500",
            salt = "Calcium Carbonate",
            therapeuticClass = "SUPPLEMENT",
            timingRuleEn = "Take after lunch. Keep 2 hours gap from Iron",
            timingRuleHi = "दोपहर खाने के बाद लें। आयरन की दवा से 2 घंटे का अंतर रखें",
            usageEn = "This is your calcium bone supplement tablet.",
            usageHi = "यह हड्डियों की मजबूती के लिए कैल्शियम की गोली है।",
            daysRemaining = 7
        ),
        CabinetMedicineItem(
            brand = "Atorva 10",
            salt = "Atorvastatin",
            therapeuticClass = "STATIN",
            timingRuleEn = "Take 30 minutes before sleep at night",
            timingRuleHi = "रात को सोने से 30 मिनट पहले लें",
            usageEn = "This is your nighttime cholesterol lowering medication.",
            usageHi = "यह कोलेस्ट्रॉल की रात की दवा है।",
            daysRemaining = 20
        ),
        CabinetMedicineItem(
            brand = "Pan 40",
            salt = "Pantoprazole Sodium",
            therapeuticClass = "ANTACID_PPI",
            timingRuleEn = "Take 30 minutes before breakfast",
            timingRuleHi = "सुबह नाश्ते से 30 मिनट पहले लें",
            usageEn = "This is your antacid and gas relief tablet.",
            usageHi = "यह गैस और एसिडिटी की गोली है।",
            daysRemaining = 15
        )
    )

    val filteredList = masterCabinet.filter {
        it.brand.contains(searchQuery, ignoreCase = true) ||
                it.salt.contains(searchQuery, ignoreCase = true) ||
                it.therapeuticClass.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCharcoal)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (locale == "hi") "मेरी दवा पेटी 💊" else "My Medicine Cabinet 💊",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )
                Text(
                    text = if (locale == "hi") "सक्रिय दवाएं और खुराक नियम" else "Active Prescriptions & Dosage Rules",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (locale == "hi") "दवा या साल्ट खोजें..." else "Search medicine or active salt...",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextMuted
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = SafeGreen,
                unfocusedBorderColor = AccentBorder,
                focusedContainerColor = SurfaceCardDark,
                unfocusedContainerColor = SurfaceCardDark
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Medicine Cards List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredList) { item ->
                val timing = if (locale == "hi") item.timingRuleHi else item.timingRuleEn
                val spokenText = if (locale == "hi") "${item.brand}। ${item.usageHi}। ${item.timingRuleHi}" else "${item.brand}. ${item.usageEn} ${item.timingRuleEn}"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.brand,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                )
                                Text(
                                    text = "Active: ${item.salt}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = ReticleCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            // Speaker Button: Read out dosage
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.ttsManager.speak(spokenText, locale)
                                },
                                modifier = Modifier
                                    .background(SafeGreen, RoundedCornerShape(10.dp))
                                    .size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Read Aloud",
                                    tint = TextWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Timing Rule Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceCardElevated, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "⏰ $timing",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Refill badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Class: ${item.therapeuticClass}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = if (item.daysRemaining <= 7) "⚠️ Refill in ${item.daysRemaining} days" else "📦 ${item.daysRemaining} days left",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (item.daysRemaining <= 7) com.medvoice.ui.theme.WarningAmber else SafeGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
