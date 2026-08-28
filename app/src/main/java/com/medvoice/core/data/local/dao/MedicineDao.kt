package com.medvoice.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medvoice.core.data.local.entity.MedicationLogEntity
import com.medvoice.core.data.local.entity.MedicineEntity

@Dao
interface MedicineDao {

    @Query("""
        SELECT * FROM medicines 
        WHERE brand_name LIKE :query || '%' 
           OR raw_composition LIKE '%' || :query || '%' 
        LIMIT 1
    """)
    suspend fun searchCatalog(query: String): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE brand_name LIKE :query || '%' LIMIT 1")
    suspend fun findMedicineByPrefix(query: String): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE brand_name LIKE '%' || :query || '%' OR raw_composition LIKE '%' || :query || '%' LIMIT 1")
    suspend fun findMedicineByFts(query: String): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getMedicineById(id: Long): MedicineEntity?

    @Query("""
        SELECT DISTINCT m.* 
        FROM medicines m 
        INNER JOIN medication_logs l ON m.id = l.medicine_id
        ORDER BY m.brand_name ASC
    """)
    suspend fun getCabinetMedicines(): List<MedicineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logIntake(log: MedicationLogEntity): Long

    @Query("SELECT * FROM medication_logs ORDER BY intake_timestamp DESC LIMIT 50")
    suspend fun getAllLogs(): List<MedicationLogEntity>

    @Query("SELECT * FROM medication_logs WHERE intake_timestamp >= :thresholdTime ORDER BY intake_timestamp DESC")
    suspend fun getRecentLogs(thresholdTime: Long): List<MedicationLogEntity>

    @Query("DELETE FROM medication_logs")
    suspend fun clearAllLogs()
}
