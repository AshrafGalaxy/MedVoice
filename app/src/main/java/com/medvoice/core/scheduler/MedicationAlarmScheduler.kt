package com.medvoice.core.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

data class ScheduledReminder(
    val id: Int,
    val medicineName: String,
    val hour: Int,
    val minute: Int,
    val timingRule: String,
    val vernacularEn: String,
    val vernacularHi: String
)

class MedicationAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val prefs = context.getSharedPreferences("medvoice_prefs", Context.MODE_PRIVATE)

    private fun getPatientPrefixEn(): String {
        val name = prefs.getString("patient_name", "")?.trim() ?: ""
        return if (name.isNotBlank() && name != "Senior Patient") {
            "$name, "
        } else {
            ""
        }
    }

    private fun getPatientPrefixHi(): String {
        val name = prefs.getString("patient_name", "")?.trim() ?: ""
        return if (name.isNotBlank() && name != "Senior Patient") {
            "$name जी, "
        } else {
            ""
        }
    }

    /**
     * Default Senior Daily Prescriptions Schedule:
     * Now strictly dynamic - returns empty by default so new accounts have no mock alarms.
     */
    fun getDefaultPrescriptionReminders(): List<ScheduledReminder> {
        return emptyList()
    }

    /**
     * Schedule exact recurring alarm using AlarmManager
     */
    fun scheduleReminder(reminder: ScheduledReminder) {
        if (alarmManager == null) {
            Log.e("MedVoice_Scheduler", "AlarmManager service not available")
            return
        }

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = "com.medvoice.ACTION_MEDICATION_REMINDER"
            putExtra("reminder_id", reminder.id)
            putExtra("medicine_name", reminder.medicineName)
            putExtra("timing_rule", reminder.timingRule)
            putExtra("vernacular_en", reminder.vernacularEn)
            putExtra("vernacular_hi", reminder.vernacularHi)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If time has already passed today, schedule for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("MedVoice_Scheduler", "Scheduled alarm for ${reminder.medicineName} at ${reminder.hour}:${reminder.minute}")
        } catch (e: SecurityException) {
            Log.w("MedVoice_Scheduler", "Cannot schedule exact alarm, using inexact fallback", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    /**
     * Dynamically schedule alarms for user's actual registered medications
     */
    fun scheduleRemindersForMedicines(medicines: List<com.medvoice.core.data.local.entity.MedicineEntity>) {
        if (medicines.isEmpty()) {
            scheduleAllReminders()
            return
        }

        val pfxEn = getPatientPrefixEn()
        val pfxHi = getPatientPrefixHi()

        medicines.take(4).forEachIndexed { index, med ->
            val (hour, minute, timing, enPhrase, hiPhrase) = when (index) {
                0 -> {
                    val isThyroid = med.brandName.contains("Thyro", ignoreCase = true) || med.rawComposition.contains("Levothyroxine", ignoreCase = true)
                    if (isThyroid) {
                        Tuple5(7, 0, "STRICT_EMPTY_STOMACH",
                            "${pfxEn}it is 7:00 AM. Please take ${med.brandName} on an empty stomach with half glass water.",
                            "${pfxHi}सुबह के 7 बज गए हैं। कृपया ${med.brandName} खाली पेट आधे गिलास पानी के साथ लें।")
                    } else {
                        Tuple5(8, 30, "AFTER_MEAL",
                            "${pfxEn}morning medicine time. Please take ${med.brandName} after breakfast.",
                            "${pfxHi}सुबह नाश्ते के बाद का समय। कृपया ${med.brandName} लें।")
                    }
                }
                1 -> Tuple5(13, 30, "AFTER_MEAL",
                    "${pfxEn}afternoon medicine time. Please take ${med.brandName} after lunch.",
                    "${pfxHi}दोपहर की दवा का समय। कृपया ${med.brandName} भोजन के बाद लें।")
                2 -> Tuple5(20, 0, "BEFORE_MEAL",
                    "${pfxEn}night medicine time. Please take ${med.brandName} before dinner.",
                    "${pfxHi}रात की दवा का समय। कृपया ${med.brandName} रात के खाने से पहले लें।")
                else -> Tuple5(21, 30, "BEDTIME",
                    "${pfxEn}bedtime medicine time. Please take ${med.brandName} before sleeping.",
                    "${pfxHi}सोने से पहले की दवा का समय। कृपया ${med.brandName} लें।")
            }

            val reminder = ScheduledReminder(
                id = 200 + index,
                medicineName = med.brandName,
                hour = hour,
                minute = minute,
                timingRule = timing,
                vernacularEn = enPhrase,
                vernacularHi = hiPhrase
            )
            scheduleReminder(reminder)
        }
    }

    private data class Tuple5(val hour: Int, val minute: Int, val timing: String, val en: String, val hi: String)

    /**
     * Schedule all default prescription reminders at once
     */
    fun scheduleAllReminders() {
        val reminders = getDefaultPrescriptionReminders()
        reminders.forEach { scheduleReminder(it) }
    }

    /**
     * Trigger instant test alarm (fires in 2 seconds for live demo/testing)
     */
    fun triggerInstantTestAlarm() {
        val pfxEn = getPatientPrefixEn()
        val pfxHi = getPatientPrefixHi()

        val testReminder = ScheduledReminder(
            id = 999,
            medicineName = "Glycomet-SR 500 (Live Test)",
            hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            minute = Calendar.getInstance().get(Calendar.MINUTE),
            timingRule = "AFTER_MEAL",
            vernacularEn = "${pfxEn}this is a test medication reminder. Please take Glycomet 500mg after food.",
            vernacularHi = "${pfxHi}यह दवा याद दिलाने का परीक्षण अलार्म है। कृपया भोजन के बाद ग्लाइकोमेट 500mg लें।"
        )

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = "com.medvoice.ACTION_MEDICATION_REMINDER"
            putExtra("reminder_id", testReminder.id)
            putExtra("medicine_name", testReminder.medicineName)
            putExtra("timing_rule", testReminder.timingRule)
            putExtra("vernacular_en", testReminder.vernacularEn)
            putExtra("vernacular_hi", testReminder.vernacularHi)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            testReminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + 2000L // 2 seconds from now
        try {
            alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            Log.d("MedVoice_Scheduler", "Instant test alarm scheduled for 2s from now")
        } catch (e: Exception) {
            Log.e("MedVoice_Scheduler", "Failed to schedule test alarm", e)
            // Fallback send broadcast directly
            context.sendBroadcast(intent)
        }
    }

    /**
     * Cancel scheduled alarm
     */
    fun cancelReminder(reminderId: Int) {
        val intent = Intent(context, MedicationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
    }
}
