package com.medvoice

import android.app.Application
import android.util.Log
import com.medvoice.core.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedVoiceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("MedVoice_App", "Initializing MedVoice On-Device Engine...")

        // Warm up SQLite Master Database on Dispatchers.IO
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(this@MedVoiceApp)
                val testQuery = db.medicineDao().findMedicineByFts("Glycomet")
                Log.d("MedVoice_App", "Master Database Pre-warmed. Sample match: ${testQuery?.brandName}")
            } catch (e: Exception) {
                Log.e("MedVoice_App", "Database Warm-up error", e)
            }
        }
    }
}
