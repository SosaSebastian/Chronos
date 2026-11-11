package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.Paciente
import com.example.data.model.Resource
import com.example.data.repository.FirebaseMedicationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Se ejecuta unos minutos después del recordatorio. Si la dosis sigue sin confirmarse,
 * envía el aviso de dosis omitida al webhook de n8n, que manda el correo al usuario registrado.
 */
class DosisOmitidaReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DosisOmitida"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DosisOmitidaNotifier.ACTION_VERIFICAR_DOSIS) return

        val medicationId = intent.getLongExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_ID, -1L)
        val nombre = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_NAME) ?: "Medicamento"
        val dosis = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_DOSAGE) ?: ""
        val hora = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_SCHEDULE_TIME) ?: ""
        val esPrueba = intent.getBooleanExtra(DosisOmitidaNotifier.EXTRA_ES_PRUEBA, false)
        val minutos = intent.getIntExtra(DosisOmitidaNotifier.EXTRA_MINUTOS_ESPERA, DosisOmitidaNotifier.MINUTOS_ESPERA)

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context.applicationContext, scope)

                // 1. Si la dosis ya se marcó como tomada, no se avisa nada.
                val medicamento = db.medicationDao().getMedicationById(medicationId)
                if (medicamento?.isTakenToday == true) {
                    Log.d(TAG, "$nombre ya fue tomada. No se envía aviso.")
                    return@launch
                }

                // 2. El correo de destino es el de la cuenta registrada en Firebase.
                val usuario = FirebaseAuth.getInstance().currentUser
                val correo = usuario?.email.orEmpty()
                if (correo.isBlank()) {
                    Log.w(TAG, "No hay sesión iniciada con correo. No se puede enviar el aviso.")
                    return@launch
                }

                // 3. Datos del paciente: primero Firestore; si no responde en 8 s, el perfil local.
                val paciente: Paciente? = withTimeoutOrNull(8_000) {
                    when (val r = FirebaseMedicationRepository().getPacienteProfile(usuario!!.uid)) {
                        is Resource.Success -> r.data
                        else -> null
                    }
                }
                val perfilLocal = if (paciente == null) db.userDao().getUserProfile().first() else null

                val payload = DosisOmitidaNotifier.construirPayload(
                    nombrePaciente = paciente?.nombre?.takeIf { it.isNotBlank() }
                        ?: perfilLocal?.fullName
                        ?: usuario!!.displayName.orEmpty(),
                    correoPaciente = correo,
                    uid = usuario!!.uid,
                    contactoNombre = paciente?.contactoEmergencia?.nombre ?: perfilLocal?.emergencyContactName.orEmpty(),
                    contactoParentesco = paciente?.contactoEmergencia?.parentesco.orEmpty(),
                    contactoTelefono = paciente?.contactoEmergencia?.telefono ?: perfilLocal?.emergencyContactPhone.orEmpty(),
                    medicationId = medicationId,
                    nombreMedicamento = medicamento?.name ?: nombre,
                    dosis = medicamento?.dosage ?: dosis,
                    horaProgramada = medicamento?.scheduleTime ?: hora,
                    minutosSinConfirmar = minutos,
                    esPrueba = esPrueba
                )

                // 4. Envío al webhook de n8n.
                val codigo = DosisOmitidaNotifier.enviarAN8n(DosisOmitidaNotifier.webhookUrl, payload)
                Log.d(TAG, "Aviso de dosis omitida de $nombre enviado. Respuesta: $codigo")
            } catch (e: Exception) {
                Log.e(TAG, "Error al verificar la dosis omitida: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
