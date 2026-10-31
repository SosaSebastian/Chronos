package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AIAssistantManager
import com.example.ai.ChatMessage
import com.example.ai.MessageSender
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
    ADD_MEDICATION,
    AI_ASSISTANT,
    HISTORIAL_CUMPLIMIENTO,
    PROFILE
}

class MedicationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MedicationRepository
    val firebaseRepo = FirebaseMedicationRepository()
    private val aiManager = AIAssistantManager(application)
    val authManager = com.example.auth.FirebaseAuthManager()

    val currentScreen = MutableStateFlow(AppScreen.LOGIN)

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

    private val _aiMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<ChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

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

        // Initial welcome message from AI Agent for older adults
        _aiMessages.value = listOf(
            ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = "¡Hola! Soy tu Asistente Chronos. Estoy aquí para acompañarte y resolver cualquier duda sobre cómo tomar tus medicinas, con qué alimentos tomarlas o qué hacer si olvidaste una dosis. ¿En qué puedo ayudarte hoy?"
            )
        )
    }

    fun navigateTo(screen: AppScreen) {
        currentScreen.value = screen
    }

    fun login(pin: String): Boolean {
        // Simple, accessible login: any 4-digit PIN or "1234"
        if (pin.length >= 4) {
            currentScreen.value = AppScreen.DASHBOARD
            return true
        }
        return false
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun signInWithFirebase(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val result = authManager.signIn(email, pass)
            _authLoading.value = false
            result.onSuccess { user ->
                val fallbackName = user.displayName?.takeIf { it.isNotBlank() }
                    ?: user.email?.substringBefore("@")
                    ?: "Sr. Sebastian"
                userProfile.value?.let { current ->
                    saveProfile(current.copy(fullName = fallbackName))
                }
                currentScreen.value = AppScreen.DASHBOARD
                onSuccess()
            }.onFailure { ex ->
                _authError.value = ex.message ?: "Error al iniciar sesión con Firebase."
            }
        }
    }

    fun signUpWithFirebase(email: String, pass: String, fullName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val result = authManager.signUp(email, pass, fullName)
            _authLoading.value = false
            result.onSuccess { user ->
                val cleanName = fullName.trim().ifEmpty {
                    user.email?.substringBefore("@") ?: "Sr. Sebastian"
                }
                userProfile.value?.let { current ->
                    saveProfile(current.copy(fullName = cleanName))
                }
                // Sincronizar perfil inicial en Firestore users/{userId}
                val paciente = Paciente(
                    uid = user.uid,
                    email = user.email ?: email,
                    nombre = cleanName,
                    edad = 73,
                    tipoSangre = "O+",
                    medico = Medico(nombre = "Dra. Sara Martinez", especialidad = "Cardióloga"),
                    contactoEmergencia = ContactoEmergencia(
                        nombre = "Isabela Sosa",
                        parentesco = "Hija",
                        telefono = "+57 310 456 7890"
                    )
                )
                launch {
                    firebaseRepo.registerUserAndCreateProfile(email, pass, paciente)
                }
                currentScreen.value = AppScreen.DASHBOARD
                onSuccess()
            }.onFailure { ex ->
                _authError.value = ex.message ?: "Error al crear cuenta en Firebase."
            }
        }
    }

    fun signOutFirebase() {
        authManager.signOut()
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
        requiresFood: Boolean
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
                requiresFood = requiresFood
            )
            repository.addMedication(entity)

            // Persistencia en subcolección Cloud Firestore users/{userId}/medicamentos
            authManager.currentUser?.uid?.let { uid ->
                val firestoreMed = Medicamento(
                    nombre = name,
                    dosis = dosage,
                    horaToma = scheduleTime,
                    categoria = category,
                    indicaciones = instructions,
                    activo = true
                )
                launch {
                    firebaseRepo.addMedicamento(uid, firestoreMed)
                }
            }

            currentScreen.value = AppScreen.DASHBOARD
        }
    }

    fun deleteMedication(id: Long) {
        viewModelScope.launch {
            repository.deleteMedication(id)
        }
    }

    fun askAI(prompt: String) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) return

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = cleanPrompt
        )
        _aiMessages.value = _aiMessages.value + userMessage
        _isAiLoading.value = true

        viewModelScope.launch {
            val patientName = userProfile.value?.fullName ?: "Sr. Sebastian"
            val response = aiManager.askAssistant(cleanPrompt, medications.value, patientName)

            val assistantMessage = ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = response
            )
            _aiMessages.value = _aiMessages.value + assistantMessage
            _isAiLoading.value = false

            // If voice is enabled, read aloud for the senior
            if (userProfile.value?.voiceEnabled != false) {
                aiManager.speak(response)
                _isSpeaking.value = true
            }
        }
    }

    fun speak(text: String) {
        aiManager.speak(text)
        _isSpeaking.value = true
    }

    fun stopSpeaking() {
        aiManager.stopSpeaking()
        _isSpeaking.value = false
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

        val sent = PushReminderManager.sendPushReminder(
            context = getApplication(),
            medicationName = medName,
            dosage = dosage,
            scheduleTime = time
        )

        _notificationSuccessMessage.value = if (sent) {
            "¡Recordatorio Push externo enviado para $medName! Revisa la barra de notificaciones."
        } else {
            "Recordatorio emitido (comprueba los permisos de notificación del dispositivo)."
        }
    }

    fun clearNotificationMessage() {
        _notificationSuccessMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        aiManager.release()
    }
}
