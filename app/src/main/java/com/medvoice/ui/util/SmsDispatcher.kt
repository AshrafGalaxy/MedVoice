package com.medvoice.ui.util

import android.content.Context
import android.telephony.SmsManager
import android.util.Log

object SmsDispatcher {
    fun sendEmergencyAlert(
        context: Context,
        recipientPhone: String,
        patientName: String,
        scannedDrug: String,
        conflictDetails: String
    ): Boolean {
        return try {
            if (recipientPhone.isBlank()) return false
            val smsManager = context.getSystemService(SmsManager::class.java) 
                ?: @Suppress("DEPRECATION") SmsManager.getDefault()
                
            val message = "🚨 [MEDVOICE ALERT] $patientName scanned '$scannedDrug'. " +
                    "WARNING: $conflictDetails. Dose was automatically blocked."

            smsManager.sendTextMessage(recipientPhone, null, message, null, null)
            Log.d("MedVoice_SmsDispatcher", "Emergency alert SMS successfully dispatched to $recipientPhone")
            true
        } catch (e: Exception) {
            Log.e("MedVoice_SmsDispatcher", "Failed to dispatch emergency SMS", e)
            false
        }
    }
}
