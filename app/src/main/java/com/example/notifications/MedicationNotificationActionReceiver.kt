package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver para procesar acciones interactivas directas desde la notificación
 * ("Tomar Dosis" y "Posponer 10 min") sin necesidad de abrir la aplicación completa.
 */
class MedicationNotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MedActionReceiver"
        const val ACTION_MARK_TAKEN = "com.example.notifications.ACTION_MARK_TAKEN"
        const val ACTION_SNOOZE = "com.example.notifications.ACTION_SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val medicationId = intent.getLongExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_ID, -1L)
        val name = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_NAME) ?: "Medicamento"
        val dosage = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_DOSAGE) ?: ""
        val scheduleTime = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_SCHEDULE_TIME) ?: ""
        val notificationId = intent.getIntExtra("notification_id", -1)

        val notificationManager = NotificationManagerCompat.from(context)

        // Cancelar la notificación original
        if (notificationId != -1) {
            notificationManager.cancel(notificationId)
        }

        when (action) {
            ACTION_MARK_TAKEN -> {
                Log.d(TAG, "Marcando dosis como tomada para medicamento id: $medicationId ($name)")
                if (medicationId > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context, this)
                            db.medicationDao().setTakenStatus(medicationId, true, System.currentTimeMillis())
                            Log.d(TAG, "Estado de toma actualizado exitosamente en Room para $name")

                            // Mostrar confirmación breve
                            showConfirmationNotification(context, name)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error actualizando estado en base de datos: ${e.message}")
                        }
                    }
                }
            }
            ACTION_SNOOZE -> {
                Log.d(TAG, "Posponiendo alarma de medicamento id: $medicationId por 10 minutos")
                MedicationAlarmScheduler.scheduleSnoozeAlarm(
                    context = context,
                    medicationId = medicationId,
                    name = name,
                    dosage = dosage,
                    scheduleTime = scheduleTime,
                    snoozeMinutes = 10
                )
            }
        }
    }

    private fun showConfirmationNotification(context: Context, medicationName: String) {
        try {
            val builder = NotificationCompat.Builder(context, PushReminderManager.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.checkbox_on_background)
                .setContentTitle("✓ Dosis Registrada")
                .setContentText("Has marcado $medicationName como tomada a tiempo.")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setTimeoutAfter(8000) // Se autocancela en 8 segundos

            val manager = NotificationManagerCompat.from(context)
            manager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
        } catch (_: SecurityException) {
            // Permiso no disponible
        }
    }
}
