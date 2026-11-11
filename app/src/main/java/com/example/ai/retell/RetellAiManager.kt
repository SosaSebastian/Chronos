package com.example.ai.retell

import android.content.Context
import android.util.Log
import com.example.data.local.MedicationEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Gestor oficial del SDK de Retell AI en Android.
 * Habilita la comunicación de voz bidireccional en tiempo real, integrando el ID de agente
 * directamente desde las variables de entorno configuradas (.env / BuildConfig.RETELL_AGENT_ID)
 * o desde las preferencias locales del usuario.
 */
class RetellAiManager(private val context: Context) {

    companion object {
        private const val TAG = "RetellAiManager"
        private const val BASE_URL = "https://api.retellai.com/"
        private const val PREFS_NAME = "retell_ai_prefs"
        private const val KEY_RETELL_API_KEY = "retell_api_key"
        private const val KEY_RETELL_AGENT_ID = "retell_agent_id"

        private const val PLACEHOLDER_KEY_1 = "YOUR_RETELL_API_KEY"
        private const val PLACEHOLDER_KEY_2 = "MY_RETELL_API_KEY"
        private const val PLACEHOLDER_AGENT_1 = "YOUR_RETELL_AGENT_ID"
        private const val PLACEHOLDER_AGENT_2 = "MY_RETELL_AGENT_ID"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Main)

    // Controlador de audio para hardware, enrutamiento VoIP y cancelación de eco
    val audioController = RetellAudioController(context)
    val isSpeakerphoneOn: StateFlow<Boolean> = audioController.isSpeakerphoneOn
    val isMicMuted: StateFlow<Boolean> = audioController.isMicMuted
    val micAmplitude: StateFlow<Float> = audioController.micAmplitude
    val waveformAmplitudes: StateFlow<List<Float>> = audioController.waveformAmplitudes

    // Estado reactivo de credenciales
    private val _apiKey = MutableStateFlow(resolveInitialApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _agentId = MutableStateFlow(resolveInitialAgentId())
    val agentId: StateFlow<String> = _agentId.asStateFlow()

    private val _agentSource = MutableStateFlow(resolveAgentSource())
    val agentSource: StateFlow<RetellAgentSource> = _agentSource.asStateFlow()

    private val _isConfigured = MutableStateFlow(checkIsConfigured(_apiKey.value, _agentId.value))
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    // Estado de la llamada en vivo bidireccional
    private val _callState = MutableStateFlow(RetellCallState.DISCONNECTED)
    val callState: StateFlow<RetellCallState> = _callState.asStateFlow()

    private val _activeWebCallUrl = MutableStateFlow<String?>(null)
    val activeWebCallUrl: StateFlow<String?> = _activeWebCallUrl.asStateFlow()

    private val _activeCallId = MutableStateFlow<String?>(null)
    val activeCallId: StateFlow<String?> = _activeCallId.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds: StateFlow<Int> = _callDurationSeconds.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    // Transcripciones en tiempo real proporcionadas por el agente de voz de Retell AI
    private val _liveTranscriptions = MutableStateFlow<List<RetellUtterance>>(emptyList())
    val liveTranscriptions: StateFlow<List<RetellUtterance>> = _liveTranscriptions.asStateFlow()

    // Latencia y calidad de conexión en tiempo real con Retell AI
    private val _connectionLatencyMs = MutableStateFlow<Long>(0L)
    val connectionLatencyMs: StateFlow<Long> = _connectionLatencyMs.asStateFlow()

    private val _connectionQuality = MutableStateFlow(
        if (_isConfigured.value) RetellConnectionQuality.GOOD else RetellConnectionQuality.DISCONNECTED
    )
    val connectionQuality: StateFlow<RetellConnectionQuality> = _connectionQuality.asStateFlow()

    private var durationTimerJob: Job? = null
    private var liveTranscriptionJob: Job? = null

    fun actualizarLatencia(latencia: Long) {
        _connectionLatencyMs.value = latencia
        _connectionQuality.value = calcularCalidad(latencia)
    }

    fun calcularCalidad(latencia: Long): RetellConnectionQuality {
        return when {
            latencia <= 0 -> if (_isConfigured.value) RetellConnectionQuality.GOOD else RetellConnectionQuality.DISCONNECTED
            latencia <= 120 -> RetellConnectionQuality.EXCELLENT
            latencia <= 280 -> RetellConnectionQuality.GOOD
            latencia <= 550 -> RetellConnectionQuality.MODERATE
            else -> RetellConnectionQuality.POOR
        }
    }

    private val retrofitService: RetellAiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RetellAiService::class.java)
    }

    private fun getBuildConfigField(fieldName: String): String {
        return try {
            val field = com.example.BuildConfig::class.java.getField(fieldName)
            (field.get(null) as? String)?.trim().orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Resuelve la API Key priorizando preferencias guardadas y luego la variable de entorno.
     */
    private fun resolveInitialApiKey(): String {
        val saved = prefs.getString(KEY_RETELL_API_KEY, "")?.trim().orEmpty()
        if (saved.isNotBlank()) return saved

        val envKey = getBuildConfigField("RETELL_API_KEY")
        return if (isValidCredential(envKey, PLACEHOLDER_KEY_1, PLACEHOLDER_KEY_2)) envKey else ""
    }

    /**
     * Resuelve el Agent ID priorizando el configurado en variables de entorno (.env / BuildConfig)
     * o guardado en preferencias por el usuario.
     */
    private fun resolveInitialAgentId(): String {
        val saved = prefs.getString(KEY_RETELL_AGENT_ID, "")?.trim().orEmpty()
        if (saved.isNotBlank()) return saved

        val envAgent = getBuildConfigField("RETELL_AGENT_ID")
        return if (isValidCredential(envAgent, PLACEHOLDER_AGENT_1, PLACEHOLDER_AGENT_2)) envAgent else ""
    }

    /**
     * Determina el origen del Agent ID para mostrarlo con transparencia en la interfaz.
     */
    private fun resolveAgentSource(): RetellAgentSource {
        val saved = prefs.getString(KEY_RETELL_AGENT_ID, "")?.trim().orEmpty()
        if (saved.isNotBlank()) return RetellAgentSource.MANUAL

        val envAgent = getBuildConfigField("RETELL_AGENT_ID")
        return if (isValidCredential(envAgent, PLACEHOLDER_AGENT_1, PLACEHOLDER_AGENT_2)) {
            RetellAgentSource.ENVIRONMENT
        } else {
            RetellAgentSource.NONE
        }
    }

    private fun isValidCredential(value: String, vararg placeholders: String): Boolean {
        if (value.isBlank()) return false
        for (p in placeholders) {
            if (value.equals(p, ignoreCase = true)) return false
        }
        return true
    }

    private fun checkIsConfigured(key: String, agent: String): Boolean {
        return isValidCredential(key, PLACEHOLDER_KEY_1, PLACEHOLDER_KEY_2) &&
                isValidCredential(agent, PLACEHOLDER_AGENT_1, PLACEHOLDER_AGENT_2)
    }

    /**
     * Guarda credenciales ingresadas manualmente por el usuario.
     */
    fun guardarCredenciales(nuevaApiKey: String, nuevoAgentId: String) {
        val cleanKey = nuevaApiKey.trim()
        val cleanAgent = nuevoAgentId.trim()

        prefs.edit()
            .putString(KEY_RETELL_API_KEY, cleanKey)
            .putString(KEY_RETELL_AGENT_ID, cleanAgent)
            .apply()

        _apiKey.value = cleanKey
        _agentId.value = cleanAgent
        _agentSource.value = RetellAgentSource.MANUAL
        val configured = checkIsConfigured(cleanKey, cleanAgent)
        _isConfigured.value = configured
        _connectionQuality.value = if (configured) RetellConnectionQuality.GOOD else RetellConnectionQuality.DISCONNECTED
        _lastErrorMessage.value = null
    }

    /**
     * Restablece las credenciales a los valores definidos en las variables de entorno (.env).
     */
    fun restablecerCredencialesEntorno() {
        prefs.edit().remove(KEY_RETELL_API_KEY).remove(KEY_RETELL_AGENT_ID).apply()
        _apiKey.value = resolveInitialApiKey()
        _agentId.value = resolveInitialAgentId()
        _agentSource.value = resolveAgentSource()
        val configured = checkIsConfigured(_apiKey.value, _agentId.value)
        _isConfigured.value = configured
        _connectionQuality.value = if (configured) RetellConnectionQuality.GOOD else RetellConnectionQuality.DISCONNECTED
    }

    /**
     * Alterna altavoz/manos libres durante la llamada bidireccional.
     */
    fun toggleSpeakerphone(): Boolean {
        return audioController.toggleSpeakerphone()
    }

    /**
     * Alterna silenciar/activar micrófono durante la llamada bidireccional.
     */
    fun toggleMicMute(): Boolean {
        return audioController.toggleMicMute()
    }

    /**
     * Prueba la conexión con el servidor de Retell AI consultando el agente configurado.
     */
    suspend fun probarConexion(): Result<String> = withContext(Dispatchers.IO) {
        val key = _apiKey.value.trim()
        val agent = _agentId.value.trim()

        if (!isValidCredential(key, PLACEHOLDER_KEY_1, PLACEHOLDER_KEY_2)) {
            _connectionQuality.value = RetellConnectionQuality.DISCONNECTED
            return@withContext Result.failure(Exception("Por favor ingresa una Retell API Key válida."))
        }
        if (!isValidCredential(agent, PLACEHOLDER_AGENT_1, PLACEHOLDER_AGENT_2)) {
            _connectionQuality.value = RetellConnectionQuality.DISCONNECTED
            return@withContext Result.failure(Exception("Por favor ingresa un Retell Agent ID válido."))
        }

        _connectionQuality.value = RetellConnectionQuality.CONNECTING
        val startTime = System.currentTimeMillis()

        try {
            val response = retrofitService.getAgent("Bearer $key", agent)
            val latency = System.currentTimeMillis() - startTime
            _connectionLatencyMs.value = latency

            if (response.isSuccessful && response.body() != null) {
                _connectionQuality.value = calcularCalidad(latency)
                val body = response.body()!!
                val agentName = body.agentName ?: body.agentId
                val msg = "Conexión exitosa con Retell AI ($latency ms). Agente verificado: $agentName"
                Log.i(TAG, msg)
                Result.success(msg)
            } else {
                _connectionQuality.value = RetellConnectionQuality.POOR
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Error de autenticación con Retell AI (${response.code()}): ${errorBody ?: response.message()}"
                Log.w(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            _connectionQuality.value = RetellConnectionQuality.POOR
            Log.e(TAG, "Excepción conectando a Retell AI: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Inicia una llamada web bidireccional de Retell AI inyectando dinámicamente los medicamentos,
     * dosis, horarios y guardrails médicos del paciente.
     */
    suspend fun iniciarLlamadaWeb(
        pacienteNombre: String,
        medicamentos: List<MedicationEntity>,
        contactoEmergencia: String,
        doctorTratante: String = ""
    ): Result<CreateWebCallResponse> = withContext(Dispatchers.IO) {
        val key = _apiKey.value.trim()
        val agent = _agentId.value.trim()

        if (!checkIsConfigured(key, agent)) {
            _callState.value = RetellCallState.CONFIGURING
            return@withContext Result.failure(
                Exception("Falta configurar la API Key o Agent ID de Retell AI en el entorno o ajustes.")
            )
        }

        _callState.value = RetellCallState.CONNECTING
        _connectionQuality.value = RetellConnectionQuality.CONNECTING
        _lastErrorMessage.value = null
        val startTime = System.currentTimeMillis()

        val listaMedicamentos = if (medicamentos.isEmpty()) {
            "El paciente no tiene medicamentos registrados en este momento en su base de datos."
        } else {
            medicamentos.joinToString(separator = "\n") { med ->
                val estado = if (med.isTakenToday) "Ya tomada hoy" else "Pendiente de tomar hoy"
                "- ${med.name} | Dosis: ${med.dosage} | Horario programado: ${med.scheduleTime} | Categoría: ${med.category} | Indicaciones: ${med.instructions} | Con comida: ${if (med.requiresFood) "Sí" else "No"} | Estado: $estado"
            }
        }

        val infoCitasMedico = if (doctorTratante.isNotBlank()) {
            "Médico tratante registrado para citas y controles: $doctorTratante"
        } else {
            "No hay médico tratante ni citas específicas registradas en este momento."
        }

        val dynamicVars = mapOf(
            "nombre_paciente" to pacienteNombre,
            "medicamentos_activos" to listaMedicamentos,
            "citas_medicas_doctor" to infoCitasMedico,
            "contacto_emergencia" to contactoEmergencia,
            "instrucciones_base_datos" to "REGLA OBLIGATORIA (PRIORIDAD ALTA): Cuando el usuario pregunte por sus medicamentos, fechas, citas u horarios, debes consultar estrictamente la información proporcionada en la base de datos del usuario.",
            "guardrail_fuera_contexto" to "Si el usuario requiere información fuera de contexto, menciona explícitamente: 'No puedo responder debido a que no puedo corroborar la información solicitada fuera de tus medicamentos, citas y horarios registrados. Soy tu asistente de medicación, solo puedo orientarte con tus tratamientos y datos de salud en la app.'",
            "guardrail_medico" to "Si el usuario requiere algo muy específico como recomendaciones de medicamentos para tratar enfermedades o temas que debe resolver un profesional de la salud, indícale al usuario que consulte esa respuesta con un especialista o profesional de la salud."
        )

        val metadata = mapOf(
            "app" to "Healthy Chronos",
            "paciente" to pacienteNombre
        )

        try {
            val request = CreateWebCallRequest(
                agentId = agent,
                metadata = metadata,
                dynamicVariables = dynamicVars
            )

            val response = retrofitService.createWebCall("Bearer $key", request)
            val latency = System.currentTimeMillis() - startTime
            _connectionLatencyMs.value = latency

            if (response.isSuccessful && response.body() != null) {
                val callData = response.body()!!
                val webCallUrl = "https://web-call.retellai.com/?access_token=${callData.accessToken}"

                _activeCallId.value = callData.callId
                _activeWebCallUrl.value = webCallUrl
                _callState.value = RetellCallState.IN_CALL
                _connectionQuality.value = calcularCalidad(latency)
                _liveTranscriptions.value = emptyList()

                // Configurar enrutamiento de hardware de audio para llamada VoIP bidireccional
                withContext(Dispatchers.Main) {
                    audioController.setupCallAudio()
                    iniciarTimerDuracion()
                    iniciarPollingTranscripcion(callData.callId)
                }

                Log.i(TAG, "Llamada de Retell AI creada con éxito ($latency ms): ${callData.callId}")
                Result.success(callData)
            } else {
                _connectionQuality.value = RetellConnectionQuality.POOR
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Fallo al iniciar llamada en Retell AI (${response.code()}): ${errorBody ?: response.message()}"
                Log.e(TAG, errorMsg)
                _callState.value = RetellCallState.ERROR
                _lastErrorMessage.value = errorMsg
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            _connectionQuality.value = RetellConnectionQuality.POOR
            Log.e(TAG, "Excepción iniciando llamada con Retell AI: ${e.message}", e)
            _callState.value = RetellCallState.ERROR
            _lastErrorMessage.value = e.localizedMessage ?: "Error de red al conectar con Retell AI."
            Result.failure(e)
        }
    }

    private fun iniciarTimerDuracion() {
        durationTimerJob?.cancel()
        _callDurationSeconds.value = 0
        durationTimerJob = scope.launch {
            while (_callState.value == RetellCallState.IN_CALL) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    /**
     * Inicia el sondeo en tiempo real de la transcripción de la llamada activa con Retell AI.
     */
    private fun iniciarPollingTranscripcion(callId: String) {
        liveTranscriptionJob?.cancel()
        liveTranscriptionJob = scope.launch {
            while (_callState.value == RetellCallState.IN_CALL) {
                delay(2000)
                try {
                    val pollStart = System.currentTimeMillis()
                    val result = obtenerTranscripcionLlamada(callId)
                    val pollLatency = System.currentTimeMillis() - pollStart
                    if (pollLatency > 0) {
                        _connectionLatencyMs.value = pollLatency
                        _connectionQuality.value = calcularCalidad(pollLatency)
                    }
                    result.onSuccess { list ->
                        if (list.isNotEmpty()) {
                            _liveTranscriptions.value = list
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Agrega manualmente una transcripción en vivo (útil para pruebas inmediatas o eco de voz).
     */
    fun agregarTranscripcionEnVivo(utterance: RetellUtterance) {
        _liveTranscriptions.value = _liveTranscriptions.value + utterance
    }

    /**
     * Limpia la lista de transcripciones en vivo.
     */
    fun limpiarTranscripcionesEnVivo() {
        _liveTranscriptions.value = emptyList()
    }

    /**
     * Finaliza la llamada activa y restablece el hardware de audio.
     */
    fun finalizarLlamada() {
        durationTimerJob?.cancel()
        durationTimerJob = null
        liveTranscriptionJob?.cancel()
        liveTranscriptionJob = null
        audioController.resetAudio()

        val lastCallId = _activeCallId.value
        if (!lastCallId.isNullOrBlank()) {
            scope.launch {
                try {
                    val result = obtenerTranscripcionLlamada(lastCallId)
                    result.onSuccess { list ->
                        if (list.isNotEmpty()) {
                            _liveTranscriptions.value = list
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        _callState.value = RetellCallState.ENDED
        _connectionQuality.value = if (_isConfigured.value) RetellConnectionQuality.GOOD else RetellConnectionQuality.DISCONNECTED
        _activeWebCallUrl.value = null
        _activeCallId.value = null
        _callDurationSeconds.value = 0
    }

    private var activeChatId: String? = null

    /**
     * Envía un mensaje de texto directamente al agente configurado en Retell AI,
     * garantizando que la respuesta en el chat provenga directamente de la IA de Retell AI
     * con la misma personalidad, prompt y variables dinámicas que su voz.
     */
    suspend fun enviarMensajeChat(
        mensajeUsuario: String,
        pacienteNombre: String,
        medicamentos: List<MedicationEntity>,
        contactoEmergencia: String,
        doctorTratante: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val key = _apiKey.value.trim()
        val agent = _agentId.value.trim()

        if (!checkIsConfigured(key, agent)) {
            return@withContext Result.failure(
                Exception("Falta configurar la API Key o Agent ID de Retell AI.")
            )
        }

        val listaMedicamentos = if (medicamentos.isEmpty()) {
            "El paciente no tiene medicamentos registrados en este momento en su base de datos."
        } else {
            medicamentos.joinToString(separator = "\n") { med ->
                val estado = if (med.isTakenToday) "Ya tomada hoy" else "Pendiente de tomar hoy"
                "- ${med.name} | Dosis: ${med.dosage} | Horario programado: ${med.scheduleTime} | Categoría: ${med.category} | Indicaciones: ${med.instructions} | Con comida: ${if (med.requiresFood) "Sí" else "No"} | Estado: $estado"
            }
        }

        val infoCitasMedico = if (doctorTratante.isNotBlank()) {
            "Médico tratante registrado para citas y controles: $doctorTratante"
        } else {
            "No hay médico tratante ni citas específicas registradas en este momento."
        }

        val dynamicVars = mapOf(
            "nombre_paciente" to pacienteNombre,
            "medicamentos_activos" to listaMedicamentos,
            "citas_medicas_doctor" to infoCitasMedico,
            "contacto_emergencia" to contactoEmergencia,
            "instrucciones_base_datos" to "REGLA OBLIGATORIA (PRIORIDAD ALTA): Cuando el usuario pregunte por sus medicamentos, fechas, citas u horarios, debes consultar estrictamente la información proporcionada en la base de datos del usuario.",
            "guardrail_fuera_contexto" to "Si el usuario requiere información fuera de contexto, menciona explícitamente: 'No puedo responder debido a que no puedo corroborar la información solicitada fuera de tus medicamentos, citas y horarios registrados. Soy tu asistente de medicación, solo puedo orientarte con tus tratamientos y datos de salud en la app.'",
            "guardrail_medico" to "Si el usuario requiere algo muy específico como recomendaciones de medicamentos para tratar enfermedades o temas que debe resolver un profesional de la salud, indícale al usuario que consulte esa respuesta con un especialista o profesional de la salud."
        )

        val metadata = mapOf(
            "app" to "Healthy Chronos",
            "paciente" to pacienteNombre
        )

        try {
            // Inicializar sesión de chat si aún no existe
            if (activeChatId.isNullOrBlank()) {
                val createReq = CreateChatRequest(
                    agentId = agent,
                    metadata = metadata,
                    dynamicVariables = dynamicVars
                )
                val chatRes = retrofitService.createChat("Bearer $key", createReq)
                if (chatRes.isSuccessful && chatRes.body() != null) {
                    activeChatId = chatRes.body()!!.chatId
                    Log.i(TAG, "Sesión de chat de Retell AI creada: $activeChatId")
                } else {
                    Log.w(TAG, "No se pudo crear chat en Retell AI (${chatRes.code()}): ${chatRes.errorBody()?.string()}")
                }
            }

            // Enviar mensaje al agente si la sesión de chat está activa
            val chatId = activeChatId
            if (!chatId.isNullOrBlank()) {
                val compReq = CreateChatCompletionRequest(
                    chatId = chatId,
                    content = mensajeUsuario
                )
                val compRes = retrofitService.createChatCompletion("Bearer $key", compReq)
                if (compRes.isSuccessful && compRes.body() != null) {
                    val messages = compRes.body()!!.messages
                    val agentMsg = messages.lastOrNull { 
                        it.role.equals("agent", ignoreCase = true) || it.role.equals("assistant", ignoreCase = true) 
                    }?.content
                    if (!agentMsg.isNullOrBlank()) {
                        Log.i(TAG, "Respuesta de chat recibida de Retell AI: $agentMsg")
                        return@withContext Result.success(agentMsg)
                    }
                } else {
                    Log.w(TAG, "Fallo al enviar mensaje a Retell AI (${compRes.code()}): ${compRes.errorBody()?.string()}")
                }
            }

            Result.failure(Exception("No se obtuvo respuesta del chat de Retell AI."))
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en chat directo de Retell AI: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Consulta los datos y transcripción de una llamada de voz en Retell AI
     * para reflejar en el chat exactamente lo que dijo la voz del agente.
     */
    suspend fun obtenerTranscripcionLlamada(callId: String): Result<List<RetellUtterance>> = withContext(Dispatchers.IO) {
        val key = _apiKey.value.trim()
        if (key.isBlank() || callId.isBlank()) {
            return@withContext Result.failure(Exception("Faltan parámetros de consulta de llamada."))
        }

        try {
            val response = retrofitService.getCall("Bearer $key", callId)
            if (response.isSuccessful && response.body() != null) {
                val callData = response.body()!!
                val utterances = mutableListOf<RetellUtterance>()

                // Si viene el objeto estructurado
                callData.transcriptObject?.let {
                    utterances.addAll(it)
                }

                // O si viene la transcripción en texto plano (ej: "Agent: Hola\nUser: Tengo dolor")
                if (utterances.isEmpty() && !callData.transcript.isNullOrBlank()) {
                    val lines = callData.transcript.split("\n")
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("Agent:", ignoreCase = true)) {
                            utterances.add(RetellUtterance("agent", trimmed.substringAfter(":").trim()))
                        } else if (trimmed.startsWith("User:", ignoreCase = true)) {
                            utterances.add(RetellUtterance("user", trimmed.substringAfter(":").trim()))
                        }
                    }
                }

                Result.success(utterances)
            } else {
                Result.failure(Exception("Error al consultar transcripción de llamada: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reinicia el estado a inactivo para permitir una nueva llamada.
     */
    fun resetCallState() {
        durationTimerJob?.cancel()
        durationTimerJob = null
        liveTranscriptionJob?.cancel()
        liveTranscriptionJob = null
        audioController.resetAudio()

        _callState.value = RetellCallState.DISCONNECTED
        _connectionQuality.value = if (_isConfigured.value) RetellConnectionQuality.GOOD else RetellConnectionQuality.DISCONNECTED
        _connectionLatencyMs.value = 0L
        _activeWebCallUrl.value = null
        _activeCallId.value = null
        _callDurationSeconds.value = 0
        _lastErrorMessage.value = null
    }
}
