package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.voz.EstadoEscucha
import com.example.ai.voz.LectorVoz
import com.example.ai.voz.ReconocedorVoz
import com.example.ai.voz.VozUtils
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

    // Asistente Virtual Inteligente (texto y voz: la voz usa el mismo asistente)
    val asistenteManager = com.example.ai.AsistenteMedicacionManager(application)

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

    /**
     * Envía una pregunta al asistente. [porVoz] = true cuando llegó por el micrófono:
     * entonces la respuesta también se lee en voz alta (si el parlante está activo).
     * Texto y voz usan el mismo AsistenteMedicacionManager, con los mismos filtros y el aviso médico.
     */
    fun askAssistant(query: String, porVoz: Boolean = false) {
        if (query.isBlank() || _isAiLoading.value) return
        // Conversación previa (sin la pregunta nueva) para que el asistente entienda preguntas de seguimiento.
        val historialPrevio = _aiMessages.value
        val userMsg = com.example.ai.MensajeChat(
            texto = query,
            esDelUsuario = true
        )
        _aiMessages.value = _aiMessages.value + userMsg

        viewModelScope.launch {
            _isAiLoading.value = true
            val respuesta = try {
                // Sin datos inventados: si el perfil no ha cargado, el asistente usa la línea de emergencias 123.
                val currentPatientName = userProfile.value?.fullName.orEmpty()
                val currentEmergencyPhone = userProfile.value?.emergencyContactPhone.orEmpty()
                asistenteManager.responderConsulta(
                    preguntaUsuario = query,
                    medicamentosActivos = medications.value,
                    nombrePaciente = currentPatientName,
                    telefonoEmergencia = currentEmergencyPhone,
                    historial = historialPrevio
                )
            } catch (_: Exception) {
                com.example.ai.MensajeChat(
                    texto = "Disculpa, tuve un pequeño inconveniente al consultar la información. Por favor, revisa tu lista de medicamentos o comunícate con tu médico de confianza.",
                    esDelUsuario = false
                )
            } finally {
                _isAiLoading.value = false
            }
            _aiMessages.value = _aiMessages.value + respuesta
            responderPorVoz(respuesta, porVoz)
        }
    }

    // =========================================================================
    // ASISTENTE DE VOZ: escuchar (ReconocedorVoz) → pensar (askAssistant) → hablar (LectorVoz)
    // =========================================================================

    // Se crean solo al abrir el asistente, para no iniciar el micrófono ni la voz en el resto de la app.
    private val reconocedorVozDelegate = lazy { ReconocedorVoz(getApplication<Application>()) }
    private val reconocedorVoz by reconocedorVozDelegate
    private val lectorVozDelegate = lazy { LectorVoz(getApplication<Application>()) }
    private val lectorVoz by lectorVozDelegate

    val estadoEscucha: StateFlow<EstadoEscucha> get() = reconocedorVoz.estado
    val textoParcialVoz: StateFlow<String> get() = reconocedorVoz.textoParcial
    val nivelVoz: StateFlow<Float> get() = reconocedorVoz.nivel
    val asistenteHablando: StateFlow<Boolean> get() = lectorVoz.hablando

    /** Parlante: si está activo, las respuestas a preguntas hechas por voz se leen en voz alta. */
    private val _lecturaEnVozAlta = MutableStateFlow(true)
    val lecturaEnVozAlta: StateFlow<Boolean> = _lecturaEnVozAlta.asStateFlow()

    /** Modo conversación: después de responder, el asistente vuelve a escuchar solo. */
    private val _modoConversacion = MutableStateFlow(false)
    val modoConversacion: StateFlow<Boolean> = _modoConversacion.asStateFlow()

    /** Mensaje para el paciente cuando la voz falla (sin permiso, sin conexión, no se entendió...). */
    private val _avisoVoz = MutableStateFlow<String?>(null)
    val avisoVoz: StateFlow<String?> = _avisoVoz.asStateFlow()

    fun iniciarEscuchaVoz() {
        if (_isAiLoading.value) return
        lectorVoz.detener()
        _avisoVoz.value = null
        reconocedorVoz.escuchar(
            alReconocer = { texto -> askAssistant(texto, porVoz = true) },
            alFallar = { mensaje -> _avisoVoz.value = mensaje }
        )
    }

    /** El paciente terminó de hablar: se envía lo que dijo. */
    fun detenerEscuchaVoz() = reconocedorVoz.detener()

    /** Corta todo (micrófono y voz). Se usa al salir de la pantalla o al tocar "Detener". */
    fun cancelarVoz() {
        if (reconocedorVozDelegate.isInitialized()) reconocedorVoz.cancelar()
        if (lectorVozDelegate.isInitialized()) lectorVoz.detener()
    }

    /** Botón "Escuchar" de cada respuesta: la lee en voz alta, aunque se haya preguntado escribiendo. */
    fun leerMensajeEnVozAlta(mensaje: com.example.ai.MensajeChat) {
        reconocedorVoz.cancelar()
        lectorVoz.hablar(mensaje.texto)
    }

    fun alternarLecturaEnVozAlta() {
        _lecturaEnVozAlta.value = !_lecturaEnVozAlta.value
        if (!_lecturaEnVozAlta.value) lectorVoz.detener()
    }

    fun alternarModoConversacion() {
        _modoConversacion.value = !_modoConversacion.value
    }

    fun avisarPermisoMicrofonoNegado() {
        _avisoVoz.value = VozUtils.SIN_PERMISO
    }

    fun descartarAvisoVoz() {
        _avisoVoz.value = null
    }

    private fun responderPorVoz(respuesta: com.example.ai.MensajeChat, porVoz: Boolean) {
        if (!porVoz) return
        val volverAEscuchar = VozUtils.debeVolverAEscuchar(_modoConversacion.value, porVoz, respuesta)
        if (_lecturaEnVozAlta.value) {
            lectorVoz.hablar(respuesta.texto) {
                if (volverAEscuchar && _modoConversacion.value) iniciarEscuchaVoz()
            }
        } else if (volverAEscuchar) {
            iniciarEscuchaVoz()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (reconocedorVozDelegate.isInitialized()) reconocedorVoz.liberar()
        if (lectorVozDelegate.isInitialized()) lectorVoz.liberar()
    }

}
