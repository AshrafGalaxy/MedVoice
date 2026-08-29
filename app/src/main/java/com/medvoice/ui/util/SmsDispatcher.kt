package com.medvoice.ui.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat

enum class SmsDispatchStatus {
    SENT_VIA_CELLULAR_BACKGROUND,
    OPENED_IN_SMS_APP,
    MISSING_PHONE_NUMBER,
    FAILED
}

object SmsDispatcher {

    fun sendEmergencyAlert(
        context: Context,
        recipientPhone: String,
        patientName: String,
        scannedDrug: String,
        conflictDetails: String
    ): SmsDispatchStatus {
        val targetPhone = recipientPhone.trim().ifBlank {
            // Fallback to shared preferences if available
            val prefs = context.getSharedPreferences("medvoice_prefs", Context.MODE_PRIVATE)
            prefs.getString("caregiver_phone", "") ?: ""
        }

        if (targetPhone.isBlank()) {
            Log.w("MedVoice_SmsDispatcher", "No caregiver phone number configured for SOS dispatch.")
            return SmsDispatchStatus.MISSING_PHONE_NUMBER
        }

        val message = "🚨 [MEDVOICE EMERGENCY SOS] Patient $patientName scanned '$scannedDrug'. " +
                "CRITICAL WARNING: $conflictDetails. Dose was automatically blocked."

        val hasSendSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        // 1. Try background direct cellular SMS if permission is granted
        if (hasSendSmsPermission) {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                // If message length exceeds standard 160 characters, divide and send multipart SMS
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(targetPhone, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(targetPhone, null, message, null, null)
                }

                Log.d("MedVoice_SmsDispatcher", "Emergency alert SMS directly dispatched via cellular to $targetPhone")
                return SmsDispatchStatus.SENT_VIA_CELLULAR_BACKGROUND
            } catch (e: Exception) {
                Log.e("MedVoice_SmsDispatcher", "Background SmsManager failed, attempting Intent fallback", e)
            }
        }

        // 2. Seamless Intent fallback: Opens default SMS app with pre-filled recipient and alert body
        return try {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(targetPhone)}")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(smsIntent)
            Log.d("MedVoice_SmsDispatcher", "Launched SMS intent fallback for $targetPhone")
            SmsDispatchStatus.OPENED_IN_SMS_APP
        } catch (e: Exception) {
            Log.e("MedVoice_SmsDispatcher", "Failed to launch SMS intent fallback", e)
            SmsDispatchStatus.FAILED
        }
    }
}
