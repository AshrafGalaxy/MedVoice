package com.medvoice.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "brand_name")
    val brandName: String,

    @ColumnInfo(name = "raw_composition")
    val rawComposition: String,

    @ColumnInfo(name = "manufacturer")
    val manufacturer: String? = "Standard Pharma",

    @ColumnInfo(name = "dosage_form")
    val dosageForm: String = "TABLET"
)

@Entity(tableName = "medication_logs")
data class MedicationLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "medicine_id")
    val medicineId: Long = 0,

    @ColumnInfo(name = "scanned_text")
    val scannedText: String,

    @ColumnInfo(name = "parsed_salts")
    val parsedSalts: String = "",

    @ColumnInfo(name = "intake_timestamp")
    val intakeTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "voice_confirmed")
    val voiceConfirmed: Boolean = false,

    @ColumnInfo(name = "sos_sms_dispatched")
    val sosSmsDispatched: Boolean = false
) {
    // Backward-compatibility properties
    val scannedBrandName: String
        get() = scannedText

    val resolvedSaltId: Long
        get() = medicineId
}

@Entity(tableName = "cabinet_prescriptions")
data class CabinetPrescriptionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "brand_name")
    val brandName: String,

    @ColumnInfo(name = "raw_composition")
    val rawComposition: String,

    @ColumnInfo(name = "dosage_form")
    val dosageForm: String = "TABLET",

    @ColumnInfo(name = "food_timing_rule")
    val foodTimingRule: String = "AFTER_FOOD",

    @ColumnInfo(name = "manufacturer")
    val manufacturer: String? = "Prescribed Formulation",

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_timestamp")
    val createdTimestamp: Long = System.currentTimeMillis()
)
