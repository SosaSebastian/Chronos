package com.example.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.BuildConfig
import com.example.data.local.MedicationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

enum class MessageSender {
    USER, ASSISTANT, SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AIAssistantManager(private val context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = textToSpeech?.setLanguage(Locale("es", "ES"))
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        isTtsReady = true
                    }
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    fun speak(text: String) {
        if (isTtsReady && textToSpeech != null) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HealtyChronosTTS")
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    suspend fun askAssistant(
        userPrompt: String,
        medications: List<MedicationEntity>,
        patientName: String = "Don Roberto"
    ): String = withContext(Dispatchers.IO) {
        val trimmed = userPrompt.trim()
        val lower = trimmed.lowercase()

        // Local Guardrail Check 1: Temas fuera de alcance no médicos (Fútbol, política, chistes, código, etc.)
        if (isOutOfScopeQuestion(lower)) {
            return@withContext "Hola $patientName. Como tu Asistente de Healty Chronos, mi labor está dedicada exclusivamente a orientarte sobre tus medicamentos programados, horarios y cuidados de salud en casa. No puedo responder consultas sobre otros temas. ¿Tienes alguna pregunta sobre tus pastillas de hoy?"
        }

        // Local Guardrail Check 2: Síntomas agudos de alarma o solicitud de diagnóstico/receta
        if (isEmergencyOrPrescriptionRequest(lower)) {
            return@withContext "⚠️ ATENCIÓN MÉDICA:\nComo asistente virtual no puedo recetar nuevos medicamentos ni diagnosticar enfermedades. Si estás experimentando dolor agudo, dificultad para respirar o malestar intenso, por favor presiona el botón rojo de llamada de emergencia en la barra superior o comunícate de inmediato con tu médico de cabecera."
        }

        // Try Calling Gemini API (gemini-3.5-flash)
        val apiKey = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String) ?: ""
        } catch (_: Throwable) {
            ""
        }

        if (!apiKey.isNullOrBlank() && !apiKey.contains("MY_GEMINI_API_KEY")) {
            try {
                val medSummary = medications.joinToString(separator = "\n") { med ->
                    "- ${med.name} (${med.dosage}): a las ${med.scheduleTime}. Instrucciones: ${med.instructions}. Estado hoy: ${if (med.isTakenToday) "Tomado" else "Pendiente"}."
                }

                val systemPrompt = """
                    Eres 'Chronos IA', un asistente geriátrico y farmacéutico cálido, paciente y claro para $patientName.
                    Medicamentos actuales del paciente:
                    $medSummary
                    
                    REGLAS OBLIGATORIAS:
                    1. Habla en español claro, usando letra comprensible, frases cortas y tono respetuoso y cariñoso.
                    2. Responde sobre cómo tomar sus medicamentos (con agua, con comida o en ayunas), advertencias alimentarias y qué hacer si olvidó una toma.
                    3. NUNCA inventes dosis ni recetes medicamentos no registrados.
                    4. Si la consulta está fuera de alcance médico o es una urgencia, aplica el protocolo de seguridad remitiendo a su médico o contacto de emergencia.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = trimmed)))
                    ),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )

                val response = GeminiClient.api.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!responseText.isNullOrBlank()) {
                    return@withContext responseText.trim()
                }
            } catch (e: Exception) {
                // Fallback gracefully to built-in clinical rule engine
            }
        }

        // Fallback Clinical Knowledge Base for Older Adults
        generateSeniorClinicalResponse(trimmed, lower, medications, patientName)
    }

    private fun isOutOfScopeQuestion(text: String): Boolean {
        val outOfScopeKeywords = listOf(
            "fútbol", "partido", "presidente", "política", "elecciones",
            "receta de cocina", "pizza", "chiste", "cuéntame un chiste",
            "quién ganó", "dólar", "criptomoneda", "programa en python", "código"
        )
        return outOfScopeKeywords.any { text.contains(it) }
    }

    private fun isEmergencyOrPrescriptionRequest(text: String): Boolean {
        val emergencyKeywords = listOf(
            "recétame", "que me recetes", "recetar", "dolor en el pecho",
            "infarto", "me ahogo", "no puedo respirar", "desmayo",
            "qué antibiótico tomo", "cambia mi dosis", "aumentar dosis"
        )
        return emergencyKeywords.any { text.contains(it) }
    }

    private fun generateSeniorClinicalResponse(
        original: String,
        text: String,
        medications: List<MedicationEntity>,
        patientName: String
    ): String {
        return when {
            text.contains("como tomo") || text.contains("cómo tomo") || text.contains("instrucciones") -> {
                val medNames = medications.map { it.name }.joinToString(", ")
                "Estimado $patientName, aquí tienes las instrucciones principales para tus medicamentos ($medNames):\n\n" +
                medications.joinToString(separator = "\n\n") { med ->
                    "💊 *${med.name} (${med.dosage})*:\nProgramado para las ${med.scheduleTime}. ${med.instructions}."
                } + "\n\n💡 Recuerda siempre tomarlos con abundante agua tibia o al clima para facilitar la deglución."
            }

            text.contains("olvid") || text.contains("olvide") || text.contains("olvidé") -> {
                "Si olvidaste una dosis, la regla de oro general es: si faltan pocas horas para tu siguiente toma, NO tomes doble dosis para compensar. Tómala solo si te acordaste poco tiempo después. Ante cualquier duda, consulta con tu médico o enfermera acompañante."
            }

            text.contains("comida") || text.contains("alimento") || text.contains("ayunas") || text.contains("desayuno") -> {
                "Sobre tus alimentos:\n" +
                medications.joinToString(separator = "\n") { med ->
                    if (med.requiresFood) "• ${med.name}: Tomar con o después de los alimentos para cuidar tu estómago."
                    else "• ${med.name}: Se recomienda en ayunas o con el estómago liviano."
                } + "\n⚠️ Evita tomar la Atorvastatina con jugo de pomelo/toronja ya que puede aumentar su concentración."
            }

            text.contains("que me toca") || text.contains("qué me toca") || text.contains("hoy") || text.contains("pendientes") -> {
                val pending = medications.filter { !it.isTakenToday }
                if (pending.isEmpty()) {
                    "¡Excelente noticia, $patientName! Has completado todas las tomas programadas para hoy. Tu compromiso con tu salud es admirable."
                } else {
                    "Actualmente tienes ${pending.size} toma(s) pendiente(s) para hoy:\n" +
                    pending.joinToString(separator = "\n") { "• ${it.name} (${it.dosage}) a las ${it.scheduleTime}" }
                }
            }

            else -> {
                "Con gusto te oriento, $patientName. Tus medicamentos actuales son: " +
                medications.joinToString { it.name } +
                ". Recuerda mantener un vaso con agua a mano y tomar cada píldora según las horas indicadas en tu pantalla principal. ¿Deseas saber cómo tomar alguno de ellos con la comida?"
            }
        }
    }
}
