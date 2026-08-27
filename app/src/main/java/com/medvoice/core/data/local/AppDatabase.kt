package com.medvoice.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.medvoice.core.data.local.dao.MedicineDao
import com.medvoice.core.data.local.entity.ActiveSaltEntity
import com.medvoice.core.data.local.entity.FoodRuleEntity
import com.medvoice.core.data.local.entity.MedicationLogEntity
import com.medvoice.core.data.local.entity.MedicineEntity
import com.medvoice.core.data.local.entity.SaltContraindicationEntity

@Database(
    entities = [
        MedicineEntity::class,
        ActiveSaltEntity::class,
        FoodRuleEntity::class,
        SaltContraindicationEntity::class,
        MedicationLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medvoice_master.db"
                )
                .createFromAsset("databases/medvoice_master.db")
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
