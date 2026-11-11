package com.example.ai.retell

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Petición para crear una llamada web interactiva con Retell AI.
 * Endpoint: POST https://api.retellai.com/v2/create-web-call
 */
@JsonClass(generateAdapter = true)
data class CreateWebCallRequest(
    @param:Json(name = "agent_id")
    val agentId: String,
    @param:Json(name = "metadata")
    val metadata: Map<String, String> = emptyMap(),
    @param:Json(name = "retell_llm_dynamic_variables")
    val dynamicVariables: Map<String, String> = emptyMap()
)

/**
 * Respuesta de Retell AI con las credenciales de sesión WebRTC para la llamada.
 */
@JsonClass(generateAdapter = true)
data class CreateWebCallResponse(
    @param:Json(name = "call_id")
    val callId: String = "",
    @param:Json(name = "access_token")
    val accessToken: String = "",
    @param:Json(name = "agent_id")
    val agentId: String = "",
    @param:Json(name = "call_status")
    val callStatus: String? = null
)

/**
 * Respuesta con detalles del agente configurado en el panel de Retell AI.
 */
@JsonClass(generateAdapter = true)
data class RetellAgentResponse(
    @param:Json(name = "agent_id")
    val agentId: String = "",
    @param:Json(name = "agent_name")
    val agentName: String? = null,
    @param:Json(name = "voice_id")
    val voiceId: String? = null,
    @param:Json(name = "language")
    val language: String? = null
)

/**
 * Petición para crear una sesión de chat directo con el Agente de Retell AI.
 * Endpoint: POST https://api.retellai.com/create-chat
 */
@JsonClass(generateAdapter = true)
data class CreateChatRequest(
    @param:Json(name = "agent_id")
    val agentId: String,
    @param:Json(name = "metadata")
    val metadata: Map<String, String> = emptyMap(),
    @param:Json(name = "retell_llm_dynamic_variables")
    val dynamicVariables: Map<String, String> = emptyMap()
)

/**
 * Respuesta con el ID de sesión de chat en Retell AI.
 */
@JsonClass(generateAdapter = true)
data class CreateChatResponse(
    @param:Json(name = "chat_id")
    val chatId: String = "",
    @param:Json(name = "agent_id")
    val agentId: String = "",
    @param:Json(name = "chat_status")
    val chatStatus: String? = null
)

/**
 * Petición para enviar un mensaje de texto al agente y obtener su respuesta oficial en el chat.
 * Endpoint: POST https://api.retellai.com/create-chat-completion
 */
@JsonClass(generateAdapter = true)
data class CreateChatCompletionRequest(
    @param:Json(name = "chat_id")
    val chatId: String,
    @param:Json(name = "content")
    val content: String
)

/**
 * Representación de un mensaje en el chat con Retell AI.
 */
@JsonClass(generateAdapter = true)
data class RetellChatMessage(
    @param:Json(name = "role")
    val role: String = "",
    @param:Json(name = "content")
    val content: String = "",
    @param:Json(name = "message_id")
    val messageId: String? = null
)

/**
 * Respuesta del completado de chat de Retell AI con los mensajes de la conversación.
 */
@JsonClass(generateAdapter = true)
data class CreateChatCompletionResponse(
    @param:Json(name = "chat_id")
    val chatId: String = "",
    @param:Json(name = "messages")
    val messages: List<RetellChatMessage> = emptyList()
)

/**
 * Utterance individual en el transcript de una llamada de Retell AI.
 */
@JsonClass(generateAdapter = true)
data class RetellUtterance(
    @param:Json(name = "role")
    val role: String = "",
    @param:Json(name = "content")
    val content: String = "",
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis()
) {
    val isUser: Boolean
        get() = role.equals("user", ignoreCase = true)

    val labelText: String
        get() = if (isUser) "Usuario (Tú)" else "Agente IA (Retell AI)"
}

/**
 * Respuesta detallada de llamada en Retell AI (incluye transcripción exacta de la voz).
 */
@JsonClass(generateAdapter = true)
data class GetCallResponse(
    @param:Json(name = "call_id")
    val callId: String = "",
    @param:Json(name = "agent_id")
    val agentId: String = "",
    @param:Json(name = "call_status")
    val callStatus: String? = null,
    @param:Json(name = "transcript")
    val transcript: String? = null,
    @param:Json(name = "transcript_object")
    val transcriptObject: List<RetellUtterance>? = null
)

/**
 * Estados del ciclo de vida de la llamada bidireccional en tiempo real con Retell AI.
 */
enum class RetellCallState {
    DISCONNECTED,
    CONFIGURING,
    CONNECTING,
    IN_CALL,
    ENDED,
    ERROR
}

/**
 * Origen del Agent ID utilizado por el SDK de Retell AI.
 */
enum class RetellAgentSource {
    ENVIRONMENT,  // Inyectado desde .env / BuildConfig.RETELL_AGENT_ID
    MANUAL,       // Ingresado manualmente en el diálogo de configuración
    NONE          // No configurado aún
}

/**
 * Nivel de calidad de conexión o latencia de red con el agente de Retell AI.
 */
enum class RetellConnectionQuality(val label: String) {
    EXCELLENT("Excelente"),
    GOOD("Buena"),
    MODERATE("Moderada"),
    POOR("Inestable"),
    CONNECTING("Conectando..."),
    DISCONNECTED("Desconectado")
}

/**
 * Estado reactivo completo de conexión y latencia en milisegundos con Retell AI.
 */
data class RetellConnectionStatus(
    val callState: RetellCallState = RetellCallState.DISCONNECTED,
    val isConnected: Boolean = false,
    val latencyMs: Long = 0L,
    val quality: RetellConnectionQuality = RetellConnectionQuality.DISCONNECTED,
    val statusText: String = "Desconectado"
)
