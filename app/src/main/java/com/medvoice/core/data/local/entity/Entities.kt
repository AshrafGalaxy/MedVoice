package com.medvoice.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "brand_name") val brandName: String,
    @ColumnInfo(name = "manufacturer") val manufacturer: String?,
    @ColumnInfo(name = "dosage_form") val dosageForm: String,
    @ColumnInfo(name = "strength_mg") val strengthMg: Double,
    @ColumnInfo(name = "primary_salt_id") val primarySaltId: Long,
    @ColumnInfo(name = "secondary_salt_id") val secondarySaltId: Long?,
    @ColumnInfo(name = "timing_rule_id") val timingRuleId: Long,
    @ColumnInfo(name = "is_high_risk") val isHighRisk: Boolean,
    @ColumnInfo(name = "vernacular_usage_en") val vernacularUsageEn: String,
    @ColumnInfo(name = "vernacular_usage_hi") val vernacularUsageHi: String,
    @ColumnInfo(name = "vernacular_usage_mr") val vernacularUsageMr: String
)

@Entity(tableName = "active_salts")
data class ActiveSaltEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "salt_name") val saltName: String,
    @ColumnInfo(name = "therapeutic_class") val therapeuticClass: String,
    @ColumnInfo(name = "max_daily_dose_mg") val maxDailyDoseMg: Double,
    @ColumnInfo(name = "half_life_hours") val halfLifeHours: Double,
    @ColumnInfo(name = "active_window_hours") val activeWindowHours: Double,
    @ColumnInfo(name = "vernacular_salt_desc_en") val vernacularSaltDescEn: String,
    @ColumnInfo(name = "vernacular_salt_desc_hi") val vernacularSaltDescHi: String,
    @ColumnInfo(name = "vernacular_salt_desc_mr") val vernacularSaltDescMr: String
)

@Entity(tableName = "food_temporal_rules")
data class FoodRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "rule_code") val ruleCode: String,
    @ColumnInfo(name = "food_relation") val foodRelation: String,
    @ColumnInfo(name = "lead_time_minutes") val leadTimeMinutes: Int,
    @ColumnInfo(name = "dietary_restriction") val dietaryRestriction: String?,
    @ColumnInfo(name = "vernacular_instruction_en") val vernacularInstructionEn: String,
    @ColumnInfo(name = "vernacular_instruction_hi") val vernacularInstructionHi: String,
    @ColumnInfo(name = "vernacular_instruction_mr") val vernacularInstructionMr: String
)

@Entity(tableName = "salt_contraindications")
data class SaltContraindicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "salt_a_id") val saltAId: Long,
    @ColumnInfo(name = "salt_b_id") val saltBId: Long,
    @ColumnInfo(name = "severity_level") val severityLevel: String,
    @ColumnInfo(name = "clinical_risk_mechanism") val clinicalRiskMechanism: String,
    @ColumnInfo(name = "spoken_warning_en") val spokenWarningEn: String,
    @ColumnInfo(name = "spoken_warning_hi") val spokenWarningHi: String,
    @ColumnInfo(name = "spoken_warning_mr") val spokenWarningMr: String
)

@Entity(tableName = "medication_logs")
data class MedicationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "medicine_id") val medicineId: Long,
    @ColumnInfo(name = "scanned_brand_name") val scannedBrandName: String,
    @ColumnInfo(name = "resolved_salt_id") val resolvedSaltId: Long,
    @ColumnInfo(name = "intake_timestamp") val intakeTimestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "voice_confirmed") val voiceConfirmed: Boolean = false,
    @ColumnInfo(name = "sos_sms_dispatched") val sosSmsDispatched: Boolean = false
)
