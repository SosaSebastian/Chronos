package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Aviso de dosis omitida (CP07 del plan de pruebas).
 *
 * Cuando suena el recordatorio de un medicamento se programa una verificación unos minutos después.
 * Si para entonces la dosis no se marcó como tomada, la app envía los datos a un webhook de n8n,
 * que manda el correo al usuario registrado.
 */
object DosisOmitidaNotifier {

    private const val TAG = "DosisOmitida"

    const val ACTION_VERIFICAR_DOSIS = "com.example.notifications.ACTION_VERIFICAR_DOSIS"
    const val EXTRA_ES_PRUEBA = "extra_es_prueba"
    const val EXTRA_MINUTOS_ESPERA = "extra_minutos_espera"

    /** Minutos que se espera a que el paciente confirme la dosis antes de avisar. */
    const val MINUTOS_ESPERA = 30

    /** Para la alarma de prueba de "Mi Perfil" se espera solo 1 minuto, así se ve en la demo. */
    const val MINUTOS_ESPERA_PRUEBA = 1

    /** URL del webhook de n8n. Se define en .env como N8N_WEBHOOK_URL. */
    val webhookUrl: String
        get() = BuildConfig.N8N_WEBHOOK_URL

    /**
     * Programa la verificación de la dosis para dentro de [minutos] minutos.
     */
    fun programarVerificacion(
        context: Context,
        medicationId: Long,
        nombre: String,
        dosis: String,
        horaProgramada: String,
        esPrueba: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val minutos = if (esPrueba) MINUTOS_ESPERA_PRUEBA else MINUTOS_ESPERA
        val disparo = System.currentTimeMillis() + minutos * 60_000L

        val intent = Intent(context, DosisOmitidaReceiver::class.java).apply {
            action = ACTION_VERIFICAR_DOSIS
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_ID, medicationId)
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_NAME, nombre)
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_DOSAGE, dosis)
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_SCHEDULE_TIME, horaProgramada)
            putExtra(EXTRA_ES_PRUEBA, esPrueba)
            putExtra(EXTRA_MINUTOS_ESPERA, minutos)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ((medicationId + 3000) % 50000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (MedicationAlarmScheduler.canScheduleExactAlarms(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, disparo, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, disparo, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, disparo, pendingIntent)
            }
            Log.d(TAG, "Verificación de $nombre programada en $minutos min")
        } catch (e: SecurityException) {
            Log.e(TAG, "Sin permiso para programar la verificación: ${e.message}")
        }
    }

    /**
     * Arma el JSON que recibe el webhook de n8n. Función pura, para poder probarla.
     */
    fun construirPayload(
        nombrePaciente: String,
        correoPaciente: String,
        uid: String,
        contactoNombre: String,
        contactoParentesco: String,
        contactoTelefono: String,
        medicationId: Long,
        nombreMedicamento: String,
        dosis: String,
        horaProgramada: String,
        minutosSinConfirmar: Int,
        esPrueba: Boolean,
        ahora: Date = Date()
    ): JSONObject {
        val fecha = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(ahora)
        return JSONObject().apply {
            put("evento", "DOSIS_OMITIDA")
            put("esPrueba", esPrueba)
            put("fechaHora", fecha)
            put("minutosSinConfirmar", minutosSinConfirmar)
            put("paciente", JSONObject().apply {
                put("uid", uid)
                put("nombre", nombrePaciente)
                put("correo", correoPaciente)
            })
            put("contactoEmergencia", JSONObject().apply {
                put("nombre", contactoNombre)
                put("parentesco", contactoParentesco)
                put("telefono", contactoTelefono)
            })
            put("medicamento", JSONObject().apply {
                put("id", medicationId)
                put("nombre", nombreMedicamento)
                put("dosis", dosis)
                put("horaProgramada", horaProgramada)
            })
        }
    }

    /**
     * Envía el JSON al webhook con un POST. Debe llamarse fuera del hilo principal.
     * @return el código HTTP de la respuesta, o -1 si no hubo conexión.
     */
    fun enviarAN8n(url: String, payload: JSONObject): Int {
        var conexion: HttpURLConnection? = null
        return try {
            conexion = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            conexion.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val codigo = conexion.responseCode
            Log.d(TAG, "Webhook de n8n respondió $codigo")
            codigo
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo enviar el aviso a n8n ($url): ${e.message}")
            -1
        } finally {
            conexion?.disconnect()
        }
    }
}
