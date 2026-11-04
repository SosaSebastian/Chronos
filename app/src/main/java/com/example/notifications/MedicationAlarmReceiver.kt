package com.example.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

/**
 * BroadcastReceiver activado por el [android.app.AlarmManager] para disparar
 * notificaciones push prioritarias cuando llega la hora de un medicamento,
 * así como tras el encendido/reinicio del dispositivo.
 */
class MedicationAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MedAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive con acción: $action")

        when (action) {
            MedicationAlarmScheduler.ACTION_MEDICATION_REMINDER,
            MedicationAlarmScheduler.ACTION_TEST_REMINDER -> {
                handleMedicationReminder(context, intent)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "Dispositivo reiniciado. Reprogramando alarmas de medicamentos...")
                MedicationAlarmScheduler.rescheduleAllAlarms(context)
            }
            else -> {
                Log.d(TAG, "Acción no reconocida: $action")
            }
        }
    }

    private fun handleMedicationReminder(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_ID, -1L)
        val name = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_NAME) ?: "Medicamento"
        val dosage = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_DOSAGE) ?: ""
        val scheduleTime = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_SCHEDULE_TIME) ?: ""
        val instructions = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_INSTRUCTIONS) ?: ""
        val requiresFood = intent.getBooleanExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_REQUIRES_FOOD, false)

        // Asegurar que el canal de notificaciones esté creado
        PushReminderManager.createNotificationChannel(context)

        // Intent principal al tocar la notificación (abre la app)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_NAV_MEDICATION_ID", medicationId)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            (medicationId % 50000).toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción 1: Marcar dosis como tomada directamente desde la notificación
        val markTakenIntent = Intent(context, MedicationNotificationActionReceiver::class.java).apply {
            action = MedicationNotificationActionReceiver.ACTION_MARK_TAKEN
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_ID, medicationId)
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_NAME, name)
            putExtra("notification_id", (medicationId % 50000).toInt())
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            ((medicationId + 1000) % 50000).toInt(),
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción 2: Posponer recordatorio por 10 minutos
        val snoozeIntent = Intent(context, MedicationNotificationActionReceiver::class.java).apply {
            action = MedicationNotificationActionReceiver.ACTION_SNOOZE
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_ID, medicationId)
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_NAME, name)
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_DOSAGE, dosage)
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_SCHEDULE_TIME, scheduleTime)
            putExtra("notification_id", (medicationId % 50000).toInt())
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            ((medicationId + 2000) % 50000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val foodNote = if (requiresFood) " • Tomar con alimentos 🥪" else ""
        val extraNote = if (instructions.isNotBlank()) "\nIndicación: $instructions" else ""

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, PushReminderManager.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Hora de tu medicamento: $name")
            .setContentText("Dosis: $dosage • $scheduleTime$foodNote")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🔔 Recordatorio Puntual de Healty Chronos:\nEs momento de tomar tu $name ($dosage) programado a las $scheduleTime.$foodNote$extraNote\n\nPresiona 'Marcar como tomada' o 'Posponer' según corresponda.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "✓ Tomar Dosis",
                markTakenPendingIntent
            )
            .addAction(
                android.R.drawable.ic_popup_sync,
                "⏱ Posponer 10 min",
                snoozePendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (medicationId % 50000).toInt().coerceAtLeast(1)
            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Notificación emitida con éxito con id: $notificationId para $name")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permiso de notificaciones denegado: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error emitiendo notificación de recordatorio: ${e.message}")
        }

        // Si es una alarma programada normal (no de prueba), reprogramar para mañana a la misma hora
        if (intent.action == MedicationAlarmScheduler.ACTION_MEDICATION_REMINDER && medicationId > 0) {
            val nextDayMillis = MedicationAlarmScheduler.calculateNextTriggerMillis(scheduleTime)
            // Ya calculateNextTriggerMillis calcula la próxima hora futura (que será mañana)
            Log.d(TAG, "Programación de siguiente toma para $name mañana (trigger: $nextDayMillis)")
        }
    }
}
