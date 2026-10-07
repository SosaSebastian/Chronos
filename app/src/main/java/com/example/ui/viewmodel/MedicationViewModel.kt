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
    DASHBOARD,
    ADD_MEDICATION,
    AI_ASSISTANT,
    PROFILE
}

class MedicationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MedicationRepository
    private val aiManager = AIAssistantManager(application)

    val currentScreen = MutableStateFlow(AppScreen.LOGIN)

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
            val patientName = userProfile.value?.fullName ?: "Don Roberto"
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
