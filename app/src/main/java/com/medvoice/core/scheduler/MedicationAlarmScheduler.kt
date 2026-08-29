package com.medvoice.core.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

data class PrescriptionSlotConfig(
    val slotId: String, // "morning", "afternoon", "evening", "bedtime"
    val titleEn: String,
    val titleHi: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean,
    val timingRule: String
)

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
        return if (name.isNotBlank() && name != "Senior Patient" && name != "User") {
            "$name, "
        } else {
            ""
        }
    }

    private fun getPatientPrefixHi(): String {
        val name = prefs.getString("patient_name", "")?.trim() ?: ""
        return if (name.isNotBlank() && name != "Senior Patient" && name != "User") {
            "$name जी, "
        } else {
            ""
        }
    }

    /**
     * Retrieve user-customized or default daily prescription time slot configurations
     */
    fun getSlotConfigs(): List<PrescriptionSlotConfig> {
        return listOf(
            PrescriptionSlotConfig(
                slotId = "morning",
                titleEn = "Morning",
                titleHi = "सुबह",
                hour = prefs.getInt("alarm_slot_morning_hour", 8),
                minute = prefs.getInt("alarm_slot_morning_minute", 0),
                isEnabled = prefs.getBoolean("alarm_slot_morning_enabled", true),
                timingRule = "AFTER_BREAKFAST"
            ),
            PrescriptionSlotConfig(
                slotId = "afternoon",
                titleEn = "Afternoon",
                titleHi = "दोपहर",
                hour = prefs.getInt("alarm_slot_afternoon_hour", 13),
                minute = prefs.getInt("alarm_slot_afternoon_minute", 30),
                isEnabled = prefs.getBoolean("alarm_slot_afternoon_enabled", true),
                timingRule = "AFTER_LUNCH"
            ),
            PrescriptionSlotConfig(
                slotId = "evening",
                titleEn = "Evening",
                titleHi = "संध्या / रात",
                hour = prefs.getInt("alarm_slot_evening_hour", 20),
                minute = prefs.getInt("alarm_slot_evening_minute", 0),
                isEnabled = prefs.getBoolean("alarm_slot_evening_enabled", true),
                timingRule = "BEFORE_DINNER"
            ),
            PrescriptionSlotConfig(
                slotId = "bedtime",
                titleEn = "Bedtime",
                titleHi = "सोते समय",
                hour = prefs.getInt("alarm_slot_bedtime_hour", 21),
                minute = prefs.getInt("alarm_slot_bedtime_minute", 30),
                isEnabled = prefs.getBoolean("alarm_slot_bedtime_enabled", true),
                timingRule = "BEDTIME"
            )
        )
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
     * Dynamically schedule alarms using user-configured slot times & cabinet medicines
     */
    fun scheduleRemindersForMedicines(medicines: List<com.medvoice.core.data.local.entity.MedicineEntity> = emptyList()) {
        val pfxEn = getPatientPrefixEn()
        val pfxHi = getPatientPrefixHi()
        val slots = getSlotConfigs()

        slots.forEachIndexed { index, slot ->
            if (!slot.isEnabled) {
                cancelReminder(201 + index)
                return@forEachIndexed
            }

            val med = medicines.getOrNull(index)
            val medName = med?.brandName ?: "Prescribed Medication"
            val hasCustomMed = med != null

            val enPhrase = when (slot.slotId) {
                "morning" -> {
                    if (hasCustomMed) {
                        val isThyroid = medName.contains("Thyro", ignoreCase = true) || (med?.rawComposition?.contains("Levothyroxine", ignoreCase = true) == true)
                        if (isThyroid) {
                            "${pfxEn}it is morning medicine time. Please take $medName on an empty stomach with water."
                        } else {
                            "${pfxEn}morning medicine time. Please take $medName after breakfast."
                        }
                    } else {
                        "${pfxEn}it is morning medicine time. Please take your prescribed morning doses."
                    }
                }
                "afternoon" -> {
                    if (hasCustomMed) "${pfxEn}afternoon medicine time. Please take $medName after lunch."
                    else "${pfxEn}it is afternoon medicine time. Please take your post-lunch doses."
                }
                "evening" -> {
                    if (hasCustomMed) "${pfxEn}evening medicine time. Please take $medName before dinner."
                    else "${pfxEn}it is evening dinner medicine time. Please take your evening doses."
                }
                else -> {
                    if (hasCustomMed) "${pfxEn}bedtime medicine time. Please take $medName before sleeping."
                    else "${pfxEn}it is bedtime. Please take your bedtime medication before sleeping."
                }
            }

            val hiPhrase = when (slot.slotId) {
                "morning" -> {
                    if (hasCustomMed) "${pfxHi}सुबह की दवा का समय हो गया है। कृपया नाश्ते के बाद $medName लें।"
                    else "${pfxHi}सुबह की दवा का समय हो गया है। कृपया अपनी निर्धारित दवाएं लें।"
                }
                "afternoon" -> {
                    if (hasCustomMed) "${pfxHi}दोपहर की दवा का समय हो गया है। कृपया भोजन के बाद $medName लें।"
                    else "${pfxHi}दोपहर की दवा का समय हो गया है। कृपया दोपहर की दवाएं लें।"
                }
                "evening" -> {
                    if (hasCustomMed) "${pfxHi}शाम की दवा का समय हो गया है। कृपया रात के खाने से पहले $medName लें।"
                    else "${pfxHi}शाम की दवा का समय हो गया है। कृपया रात की दवाएं लें।"
                }
                else -> {
                    if (hasCustomMed) "${pfxHi}सोने का समय हो गया है। कृपया सोने से पहले $medName लें।"
                    else "${pfxHi}सोने का समय हो गया है। कृपया सोने से पहले की दवा लें।"
                }
            }

            val reminder = ScheduledReminder(
                id = 201 + index,
                medicineName = medName,
                hour = slot.hour,
                minute = slot.minute,
                timingRule = slot.timingRule,
                vernacularEn = enPhrase,
                vernacularHi = hiPhrase
            )
            scheduleReminder(reminder)
        }
    }

    /**
     * Schedule all default prescription reminders at once
     */
    fun scheduleAllReminders() {
        scheduleRemindersForMedicines(emptyList())
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
