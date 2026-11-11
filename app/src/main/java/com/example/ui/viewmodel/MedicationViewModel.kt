package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.MedicationEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.ContactoEmergencia
import com.example.data.model.Medicamento
import com.example.data.model.Medico
import com.example.data.model.Paciente
import com.example.data.model.Resource
import com.example.data.repository.FirebaseMedicationRepository
import com.example.data.repository.MedicationRepository
import com.example.notifications.MedicationAlarmScheduler
import com.example.notifications.PushReminderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    LOGIN,
    REGISTRO_PACIENTE,
    DASHBOARD,
    CALENDARIO,
    ADD_MEDICATION,
    HISTORIAL_CUMPLIMIENTO,
    PROFILE,
    AI_ASSISTANT
}

class MedicationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MedicationRepository
    val firebaseRepo = FirebaseMedicationRepository()
    val authManager = com.example.auth.FirebaseAuthManager()

    // Asistente Virtual Inteligente con soporte para Voz Externa (Retell AI) y Audio Local
    val asistenteManager = com.example.ai.AsistenteMedicacionManager(application)
    val retellManager = com.example.ai.retell.RetellAiManager(application)
    val voiceResponseManager = com.example.ai.voice.VoiceResponseManager(application)

    val isSpeaking: StateFlow<Boolean> = voiceResponseManager.isSpeaking
    val isVoiceAudioEnabled: StateFlow<Boolean> = voiceResponseManager.isVoiceAudioEnabled
    val micAmplitude: StateFlow<Float> = retellManager.micAmplitude
    val waveformAmplitudes: StateFlow<List<Float>> = retellManager.waveformAmplitudes
    val liveTranscriptions: StateFlow<List<com.example.ai.retell.RetellUtterance>> = retellManager.liveTranscriptions
    val connectionLatencyMs: StateFlow<Long> = retellManager.connectionLatencyMs
    val connectionQuality: StateFlow<com.example.ai.retell.RetellConnectionQuality> = retellManager.connectionQuality

    val currentScreen = MutableStateFlow(AppScreen.LOGIN)

    private val _aiMessages = MutableStateFlow<List<com.example.ai.MensajeChat>>(
        listOf(
            com.example.ai.MensajeChat(
                id = "welcome_msg",
                // Primer mensaje: incluye el aviso médico obligatorio.
                texto = com.example.ai.AsistenteMedicacionManager.MENSAJE_BIENVENIDA,
                esDelUsuario = false
            )
        )
    )
    val aiMessages: StateFlow<List<com.example.ai.MensajeChat>> = _aiMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    val currentFirebaseUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = authManager.authState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = authManager.currentUser
    )

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    val medications: StateFlow<List<MedicationEntity>>
    val userProfile: StateFlow<UserProfileEntity?>

    private val _notificationSuccessMessage = MutableStateFlow<String?>(null)
    val notificationSuccessMessage: StateFlow<String?> = _notificationSuccessMessage.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = MedicationRepository(database.medicationDao(), database.userDao())

        medications = repository.medications.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        userProfile = repository.userProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        // Sincronizar y programar todas las alarmas en el sistema AlarmManager
        MedicationAlarmScheduler.rescheduleAllAlarms(application)
    }

    fun navigateTo(screen: AppScreen) {
        currentScreen.value = screen
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun signInWithFirebase(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            authManager.signIn(email, pass).onSuccess { user ->
                _authLoading.value = false
                // Limpiar datos locales previos para asegurar total aislamiento de medicamentos
                repository.clearAllMedications()
                MedicationAlarmScheduler.cancelAllAlarms(getApplication())

                // Sincronizar perfil y medicamentos específicos del usuario desde Firestore
                launch {
                    when (val profileRes = firebaseRepo.getPacienteProfile(user.uid)) {
                        is Resource.Success -> {
                            val p = profileRes.data
                            repository.saveProfile(
                                UserProfileEntity(
                                    fullName = p.nombre,
                                    age = p.edad,
                                    doctorName = "${p.medico.nombre} (${p.medico.especialidad})".trim(),
                                    emergencyContactName = "${p.contactoEmergencia.nombre} (${p.contactoEmergencia.parentesco})".trim(),
                                    emergencyContactPhone = p.contactoEmergencia.telefono,
                                    bloodType = p.tipoSangre
                                )
                            )
                        }
                        else -> {}
                    }

                    // Cargar únicamente los medicamentos registrados por este usuario
                    when (val medsRes = firebaseRepo.getActiveMedicamentos(user.uid)) {
                        is Resource.Success -> {
                            for (m in medsRes.data) {
                                val entity = MedicationEntity(
                                    name = m.nombre,
                                    dosage = m.dosis,
                                    scheduleTime = m.horaToma,
                                    frequencyHours = 24,
                                    instructions = m.indicaciones,
                                    category = m.categoria,
                                    isTakenToday = false,
                                    requiresFood = true
                                )
                                val id = repository.addMedication(entity)
                                MedicationAlarmScheduler.scheduleMedicationAlarm(
                                    getApplication(),
                                    entity.copy(id = id)
                                )
                            }
                        }
                        else -> {}
                    }
                }

                currentScreen.value = AppScreen.DASHBOARD
                onSuccess()
            }.onFailure { ex ->
                _authLoading.value = false
                _authError.value = ex.message ?: "Error al iniciar sesión con Firebase."
            }
        }
    }

    fun signUpWithFirebase(email: String, pass: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            authManager.signUp(email, pass, name).onSuccess { user ->
                _authLoading.value = false
                val cleanName = name.ifBlank { "Paciente" }

                // Garantizar que la cuenta nueva inicie SIN medicamentos precargados
                repository.clearAllMedications()
                MedicationAlarmScheduler.cancelAllAlarms(getApplication())

                repository.saveProfile(
                    UserProfileEntity(
                        fullName = cleanName,
                        age = 0,
                        doctorName = "",
                        emergencyContactName = "",
                        emergencyContactPhone = "",
                        bloodType = ""
                    )
                )

                val paciente = Paciente(
                    uid = user.uid,
                    email = user.email ?: email,
                    nombre = cleanName,
                    edad = 0,
                    tipoSangre = "",
                    medico = Medico(nombre = "", especialidad = ""),
                    contactoEmergencia = ContactoEmergencia(
                        nombre = "",
                        parentesco = "",
                        telefono = ""
                    )
                )
                launch {
                    firebaseRepo.registerUserAndCreateProfile(email, pass, paciente)
                }
                currentScreen.value = AppScreen.DASHBOARD
                onSuccess()
            }.onFailure { ex ->
                _authLoading.value = false
                _authError.value = ex.message ?: "Error al crear cuenta en Firebase."
            }
        }
    }

    fun onNewUserRegistered(paciente: Paciente) {
        viewModelScope.launch {
            repository.clearAllMedications()
            MedicationAlarmScheduler.cancelAllAlarms(getApplication())
            repository.saveProfile(
                UserProfileEntity(
                    fullName = paciente.nombre,
                    age = paciente.edad,
                    doctorName = "${paciente.medico.nombre} (${paciente.medico.especialidad})".trim(),
                    emergencyContactName = "${paciente.contactoEmergencia.nombre} (${paciente.contactoEmergencia.parentesco})".trim(),
                    emergencyContactPhone = paciente.contactoEmergencia.telefono,
                    bloodType = paciente.tipoSangre
                )
            )
        }
    }

    fun signOutFirebase() {
        authManager.signOut()
        viewModelScope.launch {
            repository.clearAllMedications()
            MedicationAlarmScheduler.cancelAllAlarms(getApplication())
        }
        currentScreen.value = AppScreen.LOGIN
    }

    fun toggleMedication(id: Long, currentTaken: Boolean) {
        viewModelScope.launch {
            repository.toggleTakenStatus(id, !currentTaken)
        }
    }

    fun addMedication(
        name: String,
        dosage: String,
        scheduleTime: String,
        frequencyHours: Int,
        instructions: String,
        category: String,
        requiresFood: Boolean,
        daysOfWeek: String = "ALL",
        isEveryday: Boolean = true,
        // false cuando el medicamento ya se guardó en Firestore (pantalla de registro): evita guardarlo dos veces.
        sincronizarFirestore: Boolean = true
    ) {
        viewModelScope.launch {
            val entity = MedicationEntity(
                name = name,
                dosage = dosage,
                scheduleTime = scheduleTime,
                frequencyHours = frequencyHours,
                instructions = instructions,
                category = category,
                isTakenToday = false,
                requiresFood = requiresFood,
                daysOfWeek = daysOfWeek,
                isEveryday = isEveryday
            )
            val insertedId = repository.addMedication(entity)
            // Programar alarma exacta en el sistema AlarmManager
            MedicationAlarmScheduler.scheduleMedicationAlarm(
                context = getApplication(),
                medication = entity.copy(id = insertedId)
            )

            // Persistencia en subcolección Cloud Firestore users/{userId}/medicamentos
            if (sincronizarFirestore) authManager.currentUser?.uid?.let { uid ->
                val firestoreMed = Medicamento(
                    nombre = name,
                    dosis = dosage,
                    horaToma = scheduleTime,
                    categoria = category,
                    indicaciones = instructions,
                    activo = true,
                    diasSemana = daysOfWeek,
                    esTodosLosDias = isEveryday
                )
                launch {
                    firebaseRepo.addMedicamento(uid, firestoreMed)
                }
            }

            currentScreen.value = AppScreen.DASHBOARD
        }
    }

    /**
     * Después de editar un medicamento en Firestore: actualiza su copia local en Room y
     * reprograma su alarma con la hora nueva.
     */
    fun actualizarMedicamentoLocal(
        nombreAnterior: String,
        horaAnterior: String,
        nombre: String,
        dosis: String,
        horaToma: String,
        categoria: String,
        indicaciones: String,
        diasSemana: String = "ALL",
        esTodosLosDias: Boolean = true
    ) {
        viewModelScope.launch {
            val local = repository.getAllMedicationsList().firstOrNull {
                it.name == nombreAnterior && it.scheduleTime == horaAnterior
            } ?: repository.getAllMedicationsList().firstOrNull { it.name == nombreAnterior }

            if (local == null) {
                currentScreen.value = AppScreen.DASHBOARD
                return@launch
            }

            MedicationAlarmScheduler.cancelMedicationAlarm(getApplication(), local.id)
            val actualizado = local.copy(
                name = nombre,
                dosage = dosis,
                scheduleTime = horaToma,
                category = categoria,
                instructions = indicaciones,
                daysOfWeek = diasSemana,
                isEveryday = esTodosLosDias
            )
            repository.updateMedication(actualizado)
            MedicationAlarmScheduler.scheduleMedicationAlarm(getApplication(), actualizado)
            currentScreen.value = AppScreen.DASHBOARD
        }
    }

    fun deleteMedication(id: Long) {
        viewModelScope.launch {
            MedicationAlarmScheduler.cancelMedicationAlarm(getApplication(), id)
            repository.deleteMedication(id)
        }
    }

    fun saveProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    fun testPushNotification() {
        val nextPending = medications.value.firstOrNull { !it.isTakenToday }
            ?: medications.value.firstOrNull()

        val medName = nextPending?.name ?: "Losartán Potásico"
        val dosage = nextPending?.dosage ?: "50 mg"
        val time = nextPending?.scheduleTime ?: "08:00 AM"

        // Disparar prueba de alarma real con AlarmManager programada a 3 segundos
        val alarmScheduled = MedicationAlarmScheduler.scheduleTestAlarm(
            context = getApplication(),
            medicationName = medName,
            dosage = dosage,
            delaySeconds = 3
        )

        _notificationSuccessMessage.value = if (alarmScheduled) {
            "⏰ Alarma programada con Android AlarmManager. En 3 segundos recibirás la notificación con acciones de 'Tomar Dosis' y 'Posponer'."
        } else {
            PushReminderManager.sendPushReminder(
                context = getApplication(),
                medicationName = medName,
                dosage = dosage,
                scheduleTime = time
            )
            "Recordatorio emitido directamente para $medName."
        }
    }

    fun clearNotificationMessage() {
        _notificationSuccessMessage.value = null
    }

    fun askAssistant(query: String) {
        if (query.isBlank() || _isAiLoading.value) return
        val historialPrevio = _aiMessages.value
        val userMsg = com.example.ai.MensajeChat(
            texto = query,
            esDelUsuario = true
        )
        _aiMessages.value = _aiMessages.value + userMsg

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val currentPatientName = userProfile.value?.fullName.orEmpty()
                val currentEmergencyPhone = userProfile.value?.emergencyContactPhone.orEmpty()
                val currentDoctor = userProfile.value?.doctorName.orEmpty()

                var respuestaTexto: String? = null

                // Si Retell AI está configurado, consultar al agente de voz directamente
                if (retellManager.isConfigured.value) {
                    val retellChatRes = retellManager.enviarMensajeChat(
                        mensajeUsuario = query,
                        pacienteNombre = currentPatientName,
                        medicamentos = medications.value,
                        contactoEmergencia = currentEmergencyPhone,
                        doctorTratante = currentDoctor
                    )
                    retellChatRes.onSuccess { text ->
                        respuestaTexto = text
                    }
                }

                val respuesta = if (!respuestaTexto.isNullOrBlank()) {
                    com.example.ai.MensajeChat(
                        texto = respuestaTexto!!,
                        esDelUsuario = false
                    )
                } else {
                    asistenteManager.responderConsulta(
                        preguntaUsuario = query,
                        medicamentosActivos = medications.value,
                        nombrePaciente = currentPatientName,
                        telefonoEmergencia = currentEmergencyPhone,
                        historial = historialPrevio
                    )
                }
                _aiMessages.value = _aiMessages.value + respuesta

                // Reproducir respuesta por voz si está habilitada
                if (isVoiceAudioEnabled.value) {
                    voiceResponseManager.speak(respuesta.texto)
                }
            } catch (_: Exception) {
                val fallbackMsg = com.example.ai.MensajeChat(
                    texto = "Disculpa, tuve un pequeño inconveniente al consultar la información. Por favor, revisa tu lista de medicamentos o comunícate con tu médico de confianza.",
                    esDelUsuario = false
                )
                _aiMessages.value = _aiMessages.value + fallbackMsg
                if (isVoiceAudioEnabled.value) {
                    voiceResponseManager.speak(fallbackMsg.texto)
                }
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // =========================================================================
    // MÉTODOS DE INTEGRACIÓN RETELL AI Y FORMATO DE VOZ
    // =========================================================================

    fun toggleSpeakerphone(): Boolean = retellManager.toggleSpeakerphone()

    fun toggleMicMute(): Boolean = retellManager.toggleMicMute()

    fun toggleVoiceAudio(): Boolean = voiceResponseManager.toggleVoiceAudio()

    fun setVoiceAudioEnabled(enabled: Boolean) = voiceResponseManager.setVoiceAudioEnabled(enabled)

    fun speakAssistantResponse(texto: String) = voiceResponseManager.speak(texto)

    fun stopSpeaking() = voiceResponseManager.stop()

    fun restablecerCredencialesEntorno() = retellManager.restablecerCredencialesEntorno()

    fun guardarCredencialesRetell(apiKey: String, agentId: String) =
        retellManager.guardarCredenciales(apiKey, agentId)

    fun probarConexionRetell(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = retellManager.probarConexion()
            result.onSuccess { msg -> onResult(true, msg) }
                .onFailure { ex -> onResult(false, ex.localizedMessage ?: "Error de conexión con Retell AI.") }
        }
    }

    fun iniciarLlamadaRetell(
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val patientName = userProfile.value?.fullName.orEmpty().ifBlank { "Paciente" }
            val emergencyPhone = userProfile.value?.emergencyContactPhone.orEmpty().ifBlank { "+57 310 456 7890" }
            val doctor = userProfile.value?.doctorName.orEmpty()

            val result = retellManager.iniciarLlamadaWeb(
                pacienteNombre = patientName,
                medicamentos = medications.value,
                contactoEmergencia = emergencyPhone,
                doctorTratante = doctor
            )

            result.onSuccess { callResponse ->
                val webCallUrl = "https://web-call.retellai.com/?access_token=${callResponse.accessToken}"
                onSuccess(webCallUrl)
            }.onFailure { ex ->
                onError(ex.localizedMessage ?: "No se pudo iniciar la llamada con Retell AI.")
            }
        }
    }

    fun finalizarLlamadaRetell() {
        val lastCallId = retellManager.activeCallId.value
        retellManager.finalizarLlamada()

        if (!lastCallId.isNullOrBlank() && retellManager.isConfigured.value) {
            viewModelScope.launch {
                try {
                    val transRes = retellManager.obtenerTranscripcionLlamada(lastCallId)
                    transRes.onSuccess { utterances ->
                        if (utterances.isNotEmpty()) {
                            val newChatMsgs = utterances.map { u ->
                                com.example.ai.MensajeChat(
                                    texto = u.content,
                                    esDelUsuario = u.role.equals("user", ignoreCase = true)
                                )
                            }
                            _aiMessages.value = _aiMessages.value + newChatMsgs
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceResponseManager.shutdown()
        retellManager.audioController.resetAudio()
    }
}
