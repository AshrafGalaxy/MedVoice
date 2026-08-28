package com.medvoice.core.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.medvoice.MainActivity
import com.medvoice.R
import com.medvoice.core.audio.VernacularTtsManager

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("medicine_name") ?: "Prescription Medicine"
        val reminderId = intent.getIntExtra("reminder_id", 100)
        val vernacularEn = intent.getStringExtra("vernacular_en") ?: "It is time to take your prescribed medicine."
        val vernacularHi = intent.getStringExtra("vernacular_hi") ?: "दवा लेने का समय हो गया है।"

        val prefs = context.getSharedPreferences("medvoice_prefs", Context.MODE_PRIVATE)
        val locale = prefs.getString("selected_locale", "en") ?: "en"
        val spokenText = if (locale == "hi") vernacularHi else vernacularEn

        Log.d("MedVoice_AlarmReceiver", "Medication Alarm fired for $medicineName ($spokenText)")

        // 1. Show High-Priority Senior Heads-Up Notification
        showNotification(context, reminderId, medicineName, spokenText)

        // 2. Play Vernacular Voice Guidance Aloud
        playSpokenReminder(context, spokenText, locale)
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "medvoice_reminders_channel"

        val channel = NotificationChannel(
            channelId,
            "MedVoice Daily Medication Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority vernacular voice reminders for senior prescriptions"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
        }
        notificationManager.createNotificationChannel(channel)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ Medicine Time: $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun playSpokenReminder(context: Context, text: String, locale: String) {
        try {
            val prefs = context.getSharedPreferences("medvoice_prefs", Context.MODE_PRIVATE)
            val genderName = prefs.getString("voice_gender", "MALE") ?: "MALE"
            val gender = try {
                com.medvoice.core.audio.VoiceGender.valueOf(genderName)
            } catch (_: Exception) {
                com.medvoice.core.audio.VoiceGender.MALE
            }

            var ttsManager: VernacularTtsManager? = null
            ttsManager = VernacularTtsManager(context) { success ->
                if (success) {
                    ttsManager?.selectedGender = gender
                    ttsManager?.speak(text, locale) {
                        ttsManager?.shutdown()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MedVoice_AlarmReceiver", "Failed to trigger voice announcement", e)
        }
    }
}
