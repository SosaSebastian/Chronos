package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.MedicationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Gestor del sistema de alarmas mediante [AlarmManager] para programar recordatorios
 * puntuales y recurrentes de medicamentos en Healty Chronos.
 */
object MedicationAlarmScheduler {

    private const val TAG = "MedAlarmScheduler"

    const val ACTION_MEDICATION_REMINDER = "com.example.notifications.ACTION_MEDICATION_REMINDER"
    const val ACTION_TEST_REMINDER = "com.example.notifications.ACTION_TEST_REMINDER"

    const val EXTRA_MEDICATION_ID = "extra_medication_id"
    const val EXTRA_MEDICATION_NAME = "extra_medication_name"
    const val EXTRA_MEDICATION_DOSAGE = "extra_medication_dosage"
    const val EXTRA_MEDICATION_SCHEDULE_TIME = "extra_medication_schedule_time"
    const val EXTRA_MEDICATION_INSTRUCTIONS = "extra_medication_instructions"
    const val EXTRA_MEDICATION_REQUIRES_FOOD = "extra_medication_requires_food"

    /**
     * Verifica si la aplicación tiene permiso para programar alarmas exactas (Android 12+ / API 31+).
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    /**
     * Programa una alarma para un medicamento en su horario programado diario.
     */
    fun scheduleMedicationAlarm(context: Context, medication: MedicationEntity): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false

        val triggerAtMillis = calculateNextTriggerMillis(medication.scheduleTime)
        val pendingIntent = createAlarmPendingIntent(context, medication)

        return try {
            if (canScheduleExactAlarms(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
            Log.d(TAG, "Alarma programada con éxito para ${medication.name} a las ${medication.scheduleTime} (trigger: $triggerAtMillis)")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Error de seguridad al programar alarma: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error general al programar alarma: ${e.message}")
            false
        }
    }

    /**
     * Cancela la alarma asociada a un medicamento específico.
     */
    fun cancelMedicationAlarm(context: Context, medicationId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_MEDICATION_REMINDER
        }
        val requestCode = (medicationId % 100000).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Alarma cancelada para medicamento con id $medicationId")
        }
    }

    /**
     * Cancela todas las alarmas programadas en el sistema para todos los medicamentos locales.
     */
    fun cancelAllAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context, this)
                val allMeds = db.medicationDao().getAllMedicationsList()
                for (med in allMeds) {
                    cancelMedicationAlarm(context, med.id)
                }
                Log.d(TAG, "Todas las alarmas han sido canceladas.")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelando todas las alarmas: ${e.message}")
            }
        }
    }

    /**
     * Reprograma una alarma para un medicamento pospuesto (ej. 10 minutos).
     */
    fun scheduleSnoozeAlarm(
        context: Context,
        medicationId: Long,
        name: String,
        dosage: String,
        scheduleTime: String,
        snoozeMinutes: Int = 10
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_MEDICATION_REMINDER
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_MEDICATION_NAME, name)
            putExtra(EXTRA_MEDICATION_DOSAGE, dosage)
            putExtra(EXTRA_MEDICATION_SCHEDULE_TIME, "$scheduleTime (Pospuesto $snoozeMinutes min)")
            putExtra(EXTRA_MEDICATION_INSTRUCTIONS, "Recordatorio pospuesto. ¡Es momento de tomar tu dosis!")
            putExtra(EXTRA_MEDICATION_REQUIRES_FOOD, false)
        }

        val requestCode = ((medicationId + 90000) % 100000).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (canScheduleExactAlarms(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
            Log.d(TAG, "Alarma de posposición programada para $name en $snoozeMinutes minutos.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al programar posposición: ${e.message}")
            false
        }
    }

    /**
     * Programa una alarma de prueba rápida que se dispara en [delaySeconds] segundos.
     */
    fun scheduleTestAlarm(
        context: Context,
        medicationName: String = "Losartán Potásico",
        dosage: String = "50 mg",
        delaySeconds: Int = 10
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val triggerAtMillis = System.currentTimeMillis() + (delaySeconds * 1000L)

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_TEST_REMINDER
            putExtra(EXTRA_MEDICATION_ID, 9999L)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_MEDICATION_DOSAGE, dosage)
            putExtra(EXTRA_MEDICATION_SCHEDULE_TIME, "Prueba AlarmManager ($delaySeconds s)")
            putExtra(EXTRA_MEDICATION_INSTRUCTIONS, "Alarma de prueba verificada correctamente mediante AlarmManager.")
            putExtra(EXTRA_MEDICATION_REQUIRES_FOOD, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (canScheduleExactAlarms(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
            Log.d(TAG, "Alarma de prueba configurada para dentro de $delaySeconds segundos.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al programar alarma de prueba: ${e.message}")
            false
        }
    }

    /**
     * Reprograma todas las alarmas de los medicamentos almacenados en la base de datos local.
     * Útil tras reiniciar el dispositivo o actualizar el paquete.
     */
    fun rescheduleAllAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context, this)
                val allMeds = db.medicationDao().getAllMedicationsList()
                Log.d(TAG, "Reprogramando alarmas para ${allMeds.size} medicamentos.")
                for (med in allMeds) {
                    scheduleMedicationAlarm(context, med)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al reprogramar todas las alarmas: ${e.message}")
            }
        }
    }

    /**
     * Calcula la próxima marca de tiempo (milisegundos) en la que debe sonar la alarma diaria.
     */
    fun calculateNextTriggerMillis(scheduleTime: String): Long {
        val now = Calendar.getInstance()
        val parsedTime = parseHourAndMinute(scheduleTime)

        val triggerCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parsedTime.first)
            set(Calendar.MINUTE, parsedTime.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Si la hora ya pasó hoy, se programa para mañana a la misma hora
        if (triggerCal.before(now)) {
            triggerCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return triggerCal.timeInMillis
    }

    /**
     * Extrae hora (0-23) y minuto (0-59) de cadenas como "08:00 AM", "8:30 PM", "14:15", etc.
     */
    fun parseHourAndMinute(timeString: String): Pair<Int, Int> {
        val clean = timeString.trim().uppercase()
        val isPm = clean.contains("PM")
        val isAm = clean.contains("AM")

        val digits = clean.filter { it.isDigit() || it == ':' }
        val parts = digits.split(":")
        var hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        if (isPm && hour < 12) {
            hour += 12
        } else if (isAm && hour == 12) {
            hour = 0
        }

        return Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    private fun createAlarmPendingIntent(context: Context, medication: MedicationEntity): PendingIntent {
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_MEDICATION_REMINDER
            putExtra(EXTRA_MEDICATION_ID, medication.id)
            putExtra(EXTRA_MEDICATION_NAME, medication.name)
            putExtra(EXTRA_MEDICATION_DOSAGE, medication.dosage)
            putExtra(EXTRA_MEDICATION_SCHEDULE_TIME, medication.scheduleTime)
            putExtra(EXTRA_MEDICATION_INSTRUCTIONS, medication.instructions)
            putExtra(EXTRA_MEDICATION_REQUIRES_FOOD, medication.requiresFood)
        }

        val requestCode = (medication.id % 100000).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
