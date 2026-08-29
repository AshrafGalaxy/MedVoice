package com.medvoice.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medvoice.core.data.local.entity.CabinetPrescriptionEntity
import com.medvoice.core.data.local.entity.MedicationLogEntity
import com.medvoice.core.data.local.entity.MedicineEntity

@Dao
interface MedicineDao {

    @Query("""
        SELECT * FROM medicines 
        WHERE LOWER(brand_name) = LOWER(:query)
           OR LOWER(brand_name) = LOWER(:query) || ' tablet'
           OR LOWER(brand_name) = LOWER(:query) || ' capsule'
           OR LOWER(brand_name) = LOWER(:query) || ' syrup'
           OR LOWER(raw_composition) = LOWER(:query)
        ORDER BY 
           CASE 
             WHEN LOWER(brand_name) = LOWER(:query) THEN 1
             WHEN LOWER(raw_composition) = LOWER(:query) THEN 2
             ELSE 3
           END
        LIMIT 1
    """)
    suspend fun searchCatalog(query: String): MedicineEntity?

    @Query("""
        SELECT * FROM medicines 
        WHERE LOWER(brand_name) = LOWER(:query)
           OR LOWER(raw_composition) = LOWER(:query)
        LIMIT 1
    """)
    suspend fun findMedicineByFts(query: String): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getMedicineById(id: Long): MedicineEntity?

    // Legacy cabinet query
    @Query("""
        SELECT DISTINCT m.* 
        FROM medicines m 
        INNER JOIN medication_logs l ON m.id = l.medicine_id
        ORDER BY m.brand_name ASC
    """)
    suspend fun getCabinetMedicines(): List<MedicineEntity>

    // Active Cabinet Prescriptions CRUD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCabinetPrescription(prescription: CabinetPrescriptionEntity): Long

    @Query("SELECT * FROM cabinet_prescriptions WHERE is_active = 1 ORDER BY brand_name ASC")
    suspend fun getAllCabinetPrescriptions(): List<CabinetPrescriptionEntity>

    @Query("SELECT * FROM cabinet_prescriptions WHERE id = :id LIMIT 1")
    suspend fun getCabinetPrescriptionById(id: Long): CabinetPrescriptionEntity?

    @Query("DELETE FROM cabinet_prescriptions WHERE id = :id")
    suspend fun deleteCabinetPrescription(id: Long)

    @Query("""
        SELECT * FROM cabinet_prescriptions 
        WHERE is_active = 1 
          AND (brand_name LIKE '%' || :query || '%' OR raw_composition LIKE '%' || :query || '%')
        ORDER BY brand_name ASC
    """)
    suspend fun searchCabinetPrescriptions(query: String): List<CabinetPrescriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logIntake(log: MedicationLogEntity): Long

    @Query("SELECT * FROM medication_logs ORDER BY intake_timestamp DESC LIMIT 100")
    suspend fun getAllLogs(): List<MedicationLogEntity>

    @Query("SELECT * FROM medication_logs WHERE intake_timestamp >= :thresholdTime ORDER BY intake_timestamp DESC")
    suspend fun getRecentLogs(thresholdTime: Long): List<MedicationLogEntity>

    @Query("DELETE FROM medication_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM medication_logs WHERE medicine_id = :medicineId")
    suspend fun deleteLogsForMedicine(medicineId: Long)
}
