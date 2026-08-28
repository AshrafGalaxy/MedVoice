package com.medvoice.core.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MedicationBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("MedVoice_BootReceiver", "Phone rebooted, restoring daily medication alarms")
            val scheduler = MedicationAlarmScheduler(context)
            scheduler.scheduleAllReminders()
        }
    }
}
