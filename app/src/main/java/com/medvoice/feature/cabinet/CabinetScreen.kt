package com.medvoice.feature.cabinet

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.medvoice.core.data.local.entity.CabinetPrescriptionEntity
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
import java.util.Locale

@Composable
fun CabinetScreen(viewModel: ScanViewModel) {
    val locale by viewModel.selectedLocale.collectAsState()
    val cabinetPrescriptions by viewModel.cabinetPrescriptions.collectAsState()
    val legacyCabinetMedicines by viewModel.cabinetMedicines.collectAsState()
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Unified prescription list (active user cabinet + legacy)
    val displayList = if (cabinetPrescriptions.isNotEmpty()) {
        cabinetPrescriptions.filter {
            it.brandName.contains(searchQuery, ignoreCase = true) ||
                    it.rawComposition.contains(searchQuery, ignoreCase = true)
        }
    } else {
        legacyCabinetMedicines.map {
            CabinetPrescriptionEntity(
                id = it.id,
                brandName = it.brandName,
                rawComposition = it.rawComposition,
                dosageForm = it.dosageForm,
                foodTimingRule = it.manufacturer ?: "AFTER_FOOD"
            )
        }.filter {
            it.brandName.contains(searchQuery, ignoreCase = true) ||
                    it.rawComposition.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCharcoal)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // 1. Responsive Header with + Add Medicine Action
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
                        .size(36.dp)
                        .background(SafeGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (locale == "hi") "मेरी दवा पेटी" else "My Medicine Cabinet",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (locale == "hi") "सक्रिय दवाएं और रासायनिक संरचना" else "Active Prescriptions & Formulations",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Sleek "+ Add Medicine" Pill Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = TextWhite,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (locale == "hi") "नई दवा" else "+ Add",
                    color = TextWhite,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Sleek Compact Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceCardDark,
            border = BorderStroke(1.dp, if (searchQuery.isNotEmpty()) SafeGreen else AccentBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (searchQuery.isNotEmpty()) SafeGreen else TextMuted,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = if (locale == "hi") "दवा या साल्ट खोजें..." else "Search medicine or active salt...",
                            color = TextMuted.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(SafeGreen),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
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

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Medicine Cards List Loaded Dynamically from Room
        if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = TextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (locale == "hi") "दवा पेटी में कोई दवा नहीं है" else "No active prescriptions in cabinet.",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (locale == "hi") "ऊपर '+ नई दवा' दबाएं या कैमरे से स्कैन करें।" else "Tap '+ Add' above or scan any strip with the camera.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayList, key = { it.id }) { item ->
                    val dosageFormLabel = when (item.dosageForm.uppercase(Locale.US)) {
                        "EYE_DROPS", "DROPS" -> "👁️ Eye Drops"
                        "SYRUP", "TONIC" -> "🧪 Syrup / Tonic"
                        "GEL", "OINTMENT" -> "🧴 Topical Gel"
                        "INHALER" -> "🫁 Inhaler"
                        "CAPSULE" -> "💊 Capsule"
                        else -> "💊 Tablet"
                    }

                    val foodTimingLabel = when (item.foodTimingRule.uppercase(Locale.US)) {
                        "BEFORE_FOOD" -> if (locale == "hi") "🥣 भोजन से पहले" else "🥣 Before Food"
                        "EMPTY_STOMACH" -> if (locale == "hi") "☀️ खाली पेट" else "☀️ Empty Stomach"
                        "BEDTIME" -> if (locale == "hi") "🌙 सोने से पहले" else "🌙 Bedtime"
                        else -> if (locale == "hi") "🍽️ भोजन के बाद" else "🍽️ After Food"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.brandName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.5.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.rawComposition,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = ReticleCyan,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Delete Button
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.removeCabinetPrescription(item.id)
                                        },
                                        modifier = Modifier
                                            .background(AlertRed.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                                            .border(1.dp, AlertRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = AlertRed,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Speaker Button: Read out dosage instruction
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.speakCabinetInstruction(item)
                                        },
                                        modifier = Modifier
                                            .background(SafeGreen.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                                            .border(1.dp, SafeGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Read Aloud",
                                            tint = SafeGreen,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Badges Row: Dosage Form + Food Timing
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = SurfaceCardElevated,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = dosageFormLabel,
                                        color = TextWhite,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    )
                                }

                                Surface(
                                    color = SafeGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = foodTimingLabel,
                                        color = SafeGreen,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Add Medicine Modal Dialog
    if (showAddDialog) {
        AddMedicineDialog(
            locale = locale,
            onDismiss = { showAddDialog = false },
            onSave = { name, salts, form, timing ->
                viewModel.addManualMedicineToCabinet(name, salts, form, timing)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddMedicineDialog(
    locale: String,
    onDismiss: () -> Unit,
    onSave: (name: String, salts: String, form: String, timing: String) -> Unit
) {
    var medicineName by remember { mutableStateOf("") }
    var activeSalts by remember { mutableStateOf("") }
    var selectedForm by remember { mutableStateOf("TABLET") }
    var selectedTiming by remember { mutableStateOf("AFTER_FOOD") }
    var nameError by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val dosageForms = listOf(
        "TABLET" to ("💊 " + (if (locale == "hi") "गोली (Tablet)" else "Tablet")),
        "CAPSULE" to ("💊 " + (if (locale == "hi") "कैप्सूल (Capsule)" else "Capsule")),
        "SYRUP" to ("🧪 " + (if (locale == "hi") "सिरप (Syrup)" else "Syrup")),
        "EYE_DROPS" to ("👁️ " + (if (locale == "hi") "आई ड्रॉप्स (Drops)" else "Eye Drops")),
        "GEL" to ("🧴 " + (if (locale == "hi") "मलहम / जेल" else "Gel/Cream")),
        "INHALER" to ("🫁 " + (if (locale == "hi") "इनहेलर" else "Inhaler"))
    )

    val timingRules = listOf(
        "AFTER_FOOD" to ("🍽️ " + (if (locale == "hi") "भोजन के बाद" else "After Food")),
        "BEFORE_FOOD" to ("🥣 " + (if (locale == "hi") "भोजन से पहले" else "Before Food")),
        "EMPTY_STOMACH" to ("☀️ " + (if (locale == "hi") "खाली पेट" else "Empty Stomach")),
        "BEDTIME" to ("🌙 " + (if (locale == "hi") "सोने से पहले" else "Bedtime"))
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceCardDark,
            border = BorderStroke(1.dp, AccentBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (locale == "hi") "दवा पेटी में जोड़ें" else "Add Active Medicine",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Medicine Name Input (44dp Pill)
                Text(
                    text = if (locale == "hi") "दवा का नाम (Medicine Brand Name)" else "Medicine Brand Name",
                    color = if (nameError != null) AlertRed else TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceCardElevated,
                    border = BorderStroke(1.dp, if (nameError != null) AlertRed else if (medicineName.isNotBlank()) SafeGreen else AccentBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = if (nameError != null) AlertRed else if (medicineName.isNotBlank()) SafeGreen else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (medicineName.isEmpty()) {
                                Text(
                                    text = if (locale == "hi") "उदा. Dolo 650 या Pan 40" else "e.g. Augmentin 625, Pan 40",
                                    color = TextMuted.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            }
                            BasicTextField(
                                value = medicineName,
                                onValueChange = {
                                    medicineName = it
                                    if (nameError != null) nameError = null
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(SafeGreen),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                if (nameError != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(text = nameError ?: "", color = AlertRed, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Active Salt / Chemical Composition Input (44dp Pill)
                Text(
                    text = if (locale == "hi") "घटक / रासायनिक साल्ट (Active Composition)" else "Active Chemical Composition",
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceCardElevated,
                    border = BorderStroke(1.dp, if (activeSalts.isNotBlank()) SafeGreen else AccentBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = if (activeSalts.isNotBlank()) SafeGreen else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (activeSalts.isEmpty()) {
                                Text(
                                    text = if (locale == "hi") "उदा. Paracetamol 650mg" else "e.g. Paracetamol 650mg",
                                    color = TextMuted.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            }
                            BasicTextField(
                                value = activeSalts,
                                onValueChange = { activeSalts = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(SafeGreen),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Dosage Form Selector Chips
                Text(
                    text = if (locale == "hi") "दवा का प्रकार (Dosage Form)" else "Dosage Form",
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dosageForms) { (formCode, formLabel) ->
                        val isSelected = selectedForm == formCode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) SafeGreen else SurfaceCardElevated,
                            border = BorderStroke(1.dp, if (isSelected) SafeGreen else AccentBorder),
                            modifier = Modifier.clickable { selectedForm = formCode }
                        ) {
                            Text(
                                text = formLabel,
                                color = if (isSelected) TextWhite else TextMuted,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Food Timing Selector Chips
                Text(
                    text = if (locale == "hi") "सेवन का समय (Food Timing Rule)" else "Food Timing Rule",
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(timingRules) { (timingCode, timingLabel) ->
                        val isSelected = selectedTiming == timingCode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) SafeGreen else SurfaceCardElevated,
                            border = BorderStroke(1.dp, if (isSelected) SafeGreen else AccentBorder),
                            modifier = Modifier.clickable { selectedTiming = timingCode }
                        ) {
                            Text(
                                text = timingLabel,
                                color = if (isSelected) TextWhite else TextMuted,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Action Button
                Button(
                    onClick = {
                        val trimmedName = medicineName.trim()
                        if (trimmedName.length < 2) {
                            nameError = if (locale == "hi") "कृपया मान्य दवा का नाम दर्ज करें" else "Please enter a valid medicine name"
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave(trimmedName, activeSalts.trim(), selectedForm, selectedTiming)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (locale == "hi") "पेटी में सहेजें (Save Medicine)" else "Save to Cabinet",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
