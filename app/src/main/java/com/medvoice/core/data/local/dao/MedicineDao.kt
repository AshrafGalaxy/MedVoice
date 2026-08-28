package com.medvoice.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medvoice.core.data.local.entity.MedicationLogEntity

data class MedicineQueryResult(
    val id: Long,
    val brand_name: String,
    val dosage_form: String,
    val strength_mg: Double,
    val primary_salt_id: Long,
    val is_high_risk: Boolean,
    val vernacular_usage_en: String,
    val vernacular_usage_hi: String,
    val vernacular_usage_mr: String,
    val salt_name: String,
    val therapeutic_class: String,
    val max_daily_dose_mg: Double,
    val active_window_hours: Double,
    val vernacular_salt_desc_en: String,
    val vernacular_salt_desc_hi: String,
    val vernacular_salt_desc_mr: String,
    val rule_code: String,
    val food_relation: String,
    val vernacular_instruction_en: String,
    val vernacular_instruction_hi: String,
    val vernacular_instruction_mr: String
)

data class ContraindicationResult(
    val severity_level: String,
    val clinical_risk_mechanism: String,
    val spoken_warning_en: String,
    val spoken_warning_hi: String,
    val spoken_warning_mr: String
)

@Dao
interface MedicineDao {

    @Query("""
        SELECT m.id, m.brand_name, m.dosage_form, m.strength_mg, m.primary_salt_id, 
               m.is_high_risk, m.vernacular_usage_en, m.vernacular_usage_hi, m.vernacular_usage_mr,
               s.salt_name, s.therapeutic_class, s.max_daily_dose_mg, s.active_window_hours,
               s.vernacular_salt_desc_en, s.vernacular_salt_desc_hi, s.vernacular_salt_desc_mr,
               r.rule_code, r.food_relation, r.vernacular_instruction_en, r.vernacular_instruction_hi, r.vernacular_instruction_mr
        FROM medicines m
        JOIN active_salts s ON m.primary_salt_id = s.id
        JOIN food_temporal_rules r ON m.timing_rule_id = r.id
        WHERE m.brand_name LIKE :query || '%'
        LIMIT 1
    """)
    suspend fun findMedicineByPrefix(query: String): MedicineQueryResult?

    @Query("""
        SELECT m.id, m.brand_name, m.dosage_form, m.strength_mg, m.primary_salt_id, 
               m.is_high_risk, m.vernacular_usage_en, m.vernacular_usage_hi, m.vernacular_usage_mr,
               s.salt_name, s.therapeutic_class, s.max_daily_dose_mg, s.active_window_hours,
               s.vernacular_salt_desc_en, s.vernacular_salt_desc_hi, s.vernacular_salt_desc_mr,
               r.rule_code, r.food_relation, r.vernacular_instruction_en, r.vernacular_instruction_hi, r.vernacular_instruction_mr
        FROM medicines m
        JOIN active_salts s ON m.primary_salt_id = s.id
        JOIN food_temporal_rules r ON m.timing_rule_id = r.id
        WHERE m.brand_name LIKE '%' || :query || '%' OR s.salt_name LIKE '%' || :query || '%'
        LIMIT 1
    """)
    suspend fun findMedicineByFts(query: String): MedicineQueryResult?

    @Query("""
        SELECT * FROM medication_logs
        WHERE resolved_salt_id = :saltId
        AND status = 'TAKEN'
        AND intake_timestamp >= :thresholdTime
        ORDER BY intake_timestamp DESC
        LIMIT 1
    """)
    suspend fun getRecentActiveDose(saltId: Long, thresholdTime: Long): MedicationLogEntity?

    @Query("""
        SELECT c.severity_level, c.clinical_risk_mechanism, c.spoken_warning_en, c.spoken_warning_hi, c.spoken_warning_mr
        FROM salt_contraindications c
        JOIN medication_logs l ON (l.resolved_salt_id = c.salt_b_id OR l.resolved_salt_id = c.salt_a_id)
        WHERE (c.salt_a_id = :newSaltId OR c.salt_b_id = :newSaltId)
        AND l.status = 'TAKEN'
        AND l.intake_timestamp >= :thresholdTime
        LIMIT 1
    """)
    suspend fun checkContraindications(newSaltId: Long, thresholdTime: Long): ContraindicationResult?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logIntake(log: MedicationLogEntity): Long

    @Query("SELECT * FROM medication_logs ORDER BY intake_timestamp DESC LIMIT 50")
    suspend fun getAllLogs(): List<MedicationLogEntity>

    @Query("DELETE FROM medication_logs")
    suspend fun clearAllLogs()

    @Query("""
        SELECT m.id, m.brand_name, m.dosage_form, m.strength_mg, m.primary_salt_id, 
               m.is_high_risk, m.vernacular_usage_en, m.vernacular_usage_hi, m.vernacular_usage_mr,
               s.salt_name, s.therapeutic_class, s.max_daily_dose_mg, s.active_window_hours,
               s.vernacular_salt_desc_en, s.vernacular_salt_desc_hi, s.vernacular_salt_desc_mr,
               r.rule_code, r.food_relation, r.vernacular_instruction_en, r.vernacular_instruction_hi, r.vernacular_instruction_mr
        FROM medicines m
        JOIN active_salts s ON m.primary_salt_id = s.id
        JOIN food_temporal_rules r ON m.timing_rule_id = r.id
        ORDER BY m.id ASC
    """)
    suspend fun getAllMedicines(): List<MedicineQueryResult>
}
