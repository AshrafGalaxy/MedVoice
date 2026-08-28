package com.medvoice.core.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.medvoice.core.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("MedVoice_BootReceiver", "Phone rebooted, restoring daily medication alarms from database")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val allMedicines = db.medicineDao().getAllMedicines()
                    val scheduler = MedicationAlarmScheduler(context)
                    scheduler.scheduleRemindersForMedicines(allMedicines)
                    Log.d("MedVoice_BootReceiver", "Restored alarms for ${allMedicines.size} active prescriptions")
                } catch (e: Exception) {
                    Log.e("MedVoice_BootReceiver", "Failed to restore alarms from database, fallback to defaults", e)
                    val scheduler = MedicationAlarmScheduler(context)
                    scheduler.scheduleAllReminders()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
