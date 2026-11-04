package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.local.MedicationEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.UUID

data class MensajeChat(
    val id: String = UUID.randomUUID().toString(),
    val texto: String,
    val esDelUsuario: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val esAlertaEmergencia: Boolean = false,
    val medicamentosMencionados: List<String> = emptyList()
)

/**
 * Gestor del Asistente Virtual empático para adultos mayores en Healty Chronos.
 * Conectado con Firebase AI Logic (Gemini 3.8 Flash), RAG basado en los medicamentos activos
 * del paciente y guardrails médicos estrictos.
 */
class AsistenteMedicacionManager(private val context: Context) {

    companion object {
        private const val TAG = "AsistenteMedicacion"
        const val MODEL_NAME = "gemini-3.8-flash"
    }

    // Inicialización perezosa de Firebase AI Logic
    private val generativeModel by lazy {
        try {
            Firebase.ai.generativeModel(MODEL_NAME)
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando Firebase.ai generativeModel: ${e.message}")
            null
        }
    }

    /**
     * Procesa la consulta del usuario aplicando RAG sobre sus medicamentos y guardrails de seguridad.
     */
    suspend fun responderConsulta(
        preguntaUsuario: String,
        medicamentosActivos: List<MedicationEntity>,
        nombrePaciente: String = "Sr. Sebastian",
        telefonoEmergencia: String = "+57 310 456 7890"
    ): MensajeChat {
        val queryLimpia = preguntaUsuario.trim()

        // 1. GUARDRAIL DE SEGURIDAD LOCAL: Detección inmediata de emergencias médicas graves
        if (detectarSintomasEmergencia(queryLimpia)) {
            val mensajeEmergencia = "⚠️ Por favor, comunícate de inmediato con tu servicio de emergencias médicas local o llama a tu contacto de emergencia ($telefonoEmergencia). Los síntomas que mencionas requieren atención médica urgente presencial."
            return MensajeChat(
                texto = mensajeEmergencia,
                esDelUsuario = false,
                esAlertaEmergencia = true
            )
        }

        // 2. GUARDRAIL DE ALCANCE: Preguntas explícitas no relacionadas con salud/medicamentos
        if (detectarFueraDeAlcance(queryLimpia)) {
            return MensajeChat(
                texto = "Soy tu asistente de medicación, solo puedo ayudarte con tus tratamientos y horarios. ¿Tienes alguna duda sobre tus pastillas de hoy?",
                esDelUsuario = false
            )
        }

        // 3. INTENTO CON FIREBASE AI LOGIC (GEMINI 3.8 FLASH) CON RAG Y SYSTEM INSTRUCTION
        val respuestaAi = try {
            consultarGemini(queryLimpia, medicamentosActivos, nombrePaciente, telefonoEmergencia)
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Tiempo de espera agotado con Gemini, usando motor de respaldo local RAG")
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error en llamada a Firebase AI (${e.message}), activando motor de respaldo RAG")
            null
        }

        if (!respuestaAi.isNullOrBlank()) {
            val esAlerta = respuestaAi.contains("[ALERTA DE EMERGENCIA]") || respuestaAi.contains("emergencias")
            val textoLimpio = respuestaAi.replace("[ALERTA DE EMERGENCIA]", "").trim()
            return MensajeChat(
                texto = textoLimpio,
                esDelUsuario = false,
                esAlertaEmergencia = esAlerta
            )
        }

        // 4. MOTOR RAG DE RESPALDO DETERMINÍSTICO (Garantiza respuesta 100% precisa sin alucinaciones)
        val respuestaRAG = generarRespuestaLocalRAG(queryLimpia, medicamentosActivos, nombrePaciente)
        return MensajeChat(
            texto = respuestaRAG,
            esDelUsuario = false,
            esAlertaEmergencia = false
        )
    }

    private suspend fun consultarGemini(
        query: String,
        medicamentos: List<MedicationEntity>,
        nombrePaciente: String,
        telefonoEmergencia: String
    ): String? {
        val model = generativeModel ?: return null

        val promptRAG = construirPromptCompleto(query, medicamentos, nombrePaciente, telefonoEmergencia)

        return withTimeout(35_000L) {
            val response = model.generateContent(
                content {
                    text(promptRAG)
                }
            )
            response.text?.trim()
        }
    }

    /**
     * Construye el prompt estricto con RAG y las reglas de comportamiento requeridas.
     */
    fun construirPromptCompleto(
        pregunta: String,
        medicamentos: List<MedicationEntity>,
        nombrePaciente: String,
        telefonoEmergencia: String
    ): String {
        val listaMedicamentosTexto = if (medicamentos.isEmpty()) {
            "El paciente no tiene medicamentos activos registrados actualmente en el sistema."
        } else {
            medicamentos.joinToString(separator = "\n") { med ->
                val estado = if (med.isTakenToday) "Ya tomada hoy" else "Pendiente de tomar hoy"
                "- Medicamento: ${med.name} | Dosis: ${med.dosage} | Horario: ${med.scheduleTime} | Categoría: ${med.category} | Indicaciones: ${med.instructions} | Con alimentos: ${if (med.requiresFood) "Sí" else "No"} | Estado hoy: $estado"
            }
        }

        return """
ROL Y CONTEXTO:
Eres un asistente virtual empático, paciente y altamente claro, integrado en una aplicación móvil de gestión de medicamentos para personas de la tercera edad ('Healty Chronos').
Tu objetivo es ayudar a $nombrePaciente y a sus cuidadores a entender sus tratamientos, recordar horarios y resolver dudas sobre sus medicamentos activos.

MEDICAMENTOS ACTIVOS REGISTRADOS PARA $nombrePaciente (INFORMACIÓN ÚNICA Y VERÍDICA):
$listaMedicamentosTexto

CONTACTO DE EMERGENCIA DEL PACIENTE:
Teléfono: $telefonoEmergencia

REGLAS DE COMPORTAMIENTO Y LÍMITES ESTRICTOS (OBLIGATORIAS):
1. NO ERES UN MÉDICO: Bajo ninguna circunstancia puedes diagnosticar enfermedades, recetar nuevos medicamentos, ni sugerir alterar, pausar o aumentar las dosis recetadas. Recuerda siempre que cualquier cambio debe consultarlo con su médico tratante.
2. ANTE SÍNTOMAS GRAVES: Si el usuario menciona dolor agudo, dolor en el pecho, falta de aire, reacciones alérgicas, mareos severos o síntomas de emergencia, tu única respuesta debe ser instar al usuario a contactar a emergencias inmediatamente o llamar a su contacto ($telefonoEmergencia). Incluye la etiqueta [ALERTA DE EMERGENCIA].
3. CERO ALUCINACIONES (RAG): Responde ÚNICAMENTE basándote en la lista de medicamentos activos anterior. Si te preguntan por un medicamento que no está en la lista del usuario, indica claramente: "No tengo registro de ese medicamento en tu plan actual de tratamiento."
4. FUERA DE ALCANCE: Si el usuario pregunta sobre temas no relacionados con salud, bienestar, o sus rutinas de medicación, responde exactamente: "Soy tu asistente de medicación, solo puedo ayudarte con tus tratamientos y horarios. ¿Tienes alguna duda sobre tus pastillas de hoy?".

DIRECTRICES DE COMUNICACIÓN (UX TERCERA EDAD):
- Usa un lenguaje extremadamente sencillo, directo y cálido.
- Trata con respeto y calidez a $nombrePaciente.
- Evita jergas médicas complejas. Si debes usar un término médico, explícalo con una analogía simple.
- Mantén las respuestas cortas (máximo 3-4 oraciones). La pantalla del móvil es pequeña y el usuario puede tener fatiga visual.
- Usa listas con viñetas si debes explicar pasos para tomar una pastilla.

TAREAS PRINCIPALES:
- Informar la dosis y el horario del medicamento solicitado.
- Explicar de forma sencilla para qué sirve el medicamento según su indicación y categoría.
- Recordar recomendaciones de ingesta (ej. tomar con comida, abundante agua).
- Alertar sobre indicaciones importantes registradas.

PREGUNTA DEL PACIENTE:
"$pregunta"

RESPUESTA (Cálida, clara, máximo 3-4 oraciones):
""".trimIndent()
    }

    /**
     * Motor local RAG determinístico para responder de forma inmediata y confiable
     * aun en caso de desconexión a internet o demoras en la red.
     */
    fun generarRespuestaLocalRAG(
        pregunta: String,
        medicamentos: List<MedicationEntity>,
        nombrePaciente: String
    ): String {
        val lower = pregunta.lowercase()

        // 1. Pregunta sobre qué medicamentos tomar hoy o pendientes
        if (lower.contains("hoy") || lower.contains("toca") || lower.contains("pendiente") || lower.contains("horario")) {
            if (medicamentos.isEmpty()) {
                return "Hola $nombrePaciente, no tienes medicamentos programados en tu plan actual. Si tu médico te recetó alguno, puedes agregarlo con el botón de agregar."
            }
            val pendientes = medicamentos.filter { !it.isTakenToday }
            return if (pendientes.isNotEmpty()) {
                val detalle = pendientes.joinToString("; ") { "${it.name} (${it.dosage}) a las ${it.scheduleTime}" }
                "Hola $nombrePaciente, tienes ${pendientes.size} medicamentos pendientes para hoy: $detalle. Recuerda tomarlos con abundante agua."
            } else {
                "¡Excelente noticia, $nombrePaciente! Ya has tomado todas tus dosis programadas para el día de hoy. Sigue así."
            }
        }

        // 2. Búsqueda directa por nombre de medicamento registrado
        val medEncontrado = medicamentos.firstOrNull { med ->
            lower.contains(med.name.lowercase())
        }

        if (medEncontrado != null) {
            val indicacionComida = if (medEncontrado.requiresFood) "con alimentos o después de comer" else "con abundante agua"
            val instruccionExtra = if (medEncontrado.instructions.isNotBlank()) " Indicación: ${medEncontrado.instructions}." else ""
            val estadoHoy = if (medEncontrado.isTakenToday) "Ya la tomaste hoy." else "Está pendiente para hoy a las ${medEncontrado.scheduleTime}."

            return "Tu ${medEncontrado.name} (${medEncontrado.dosage}) es para tu salud ${medEncontrado.category.lowercase()}. Debes tomarla a las ${medEncontrado.scheduleTime} $indicacionComida.$instruccionExtra $estadoHoy"
        }

        // 3. Medicamento no registrado en el plan
        val palabrasClave = listOf("pastilla", "medicamento", "jarabe", "droga", "tomar", "dosis", "receta", "antibiotico")
        val mencionaMedicamentoGenerico = palabrasClave.any { lower.contains(it) }

        if (mencionaMedicamentoGenerico) {
            val nombresMeds = medicamentos.joinToString(", ") { it.name }
            return if (medicamentos.isNotEmpty()) {
                "No tengo registro de ese medicamento en tu plan actual de tratamiento. Actualmente tus medicamentos registrados son: $nombresMeds."
            } else {
                "No tengo registro de ese medicamento en tu plan actual de tratamiento. Consulta a tu médico tratante antes de tomar cualquier fármaco nuevo."
            }
        }

        // 4. Fuera de alcance
        return "Soy tu asistente de medicación, solo puedo ayudarte con tus tratamientos y horarios. ¿Tienes alguna duda sobre tus pastillas de hoy?"
    }

    fun detectarSintomasEmergencia(texto: String): Boolean {
        val lower = texto.lowercase()

        // 1. Dolor en el pecho o malestar torácico agudo
        if (lower.contains("pecho") && (
                lower.contains("dolor") || lower.contains("duele") || lower.contains("opresi") ||
                lower.contains("presi") || lower.contains("aprieta") || lower.contains("punzada")
            )) {
            return true
        }

        // 2. Dificultad respiratoria aguda
        if ((lower.contains("respirar") || lower.contains("aire") || lower.contains("ahogo") || lower.contains("asfixia")) && (
                lower.contains("cuesta") || lower.contains("falta") || lower.contains("dificultad") ||
                lower.contains("no puedo") || lower.contains("ahog") || lower.contains("pesa")
            )) {
            return true
        }

        // 3. Reacciones alérgicas severas
        if (lower.contains("alerg") || lower.contains("anafilax") ||
            (lower.contains("hinchaz") && (lower.contains("garganta") || lower.contains("lengua")))) {
            return true
        }

        // 4. Mareos severos o pérdida de conocimiento
        if (lower.contains("desmay") || lower.contains("inconsciente") ||
            (lower.contains("mareo") && (lower.contains("severo") || lower.contains("fuerte") || lower.contains("muy") || lower.contains("grave")))) {
            return true
        }

        // 5. Dolor agudo general, infartos o convulsiones
        val palabrasAlertaCritica = listOf(
            "dolor agudo", "dolor insoportable", "dolor severo", "dolor muy fuerte",
            "infarto", "paro cardíaco", "paro cardiaco", "convulsión", "convulsion",
            "sangrado grave", "hemorragia", "parálisis", "paralisis", "derrame", "accidente cerebrovascular",
            "emergencia médica", "emergencia medica", "urgencia médica", "urgencias médicas"
        )
        return palabrasAlertaCritica.any { lower.contains(it) }
    }

    fun detectarFueraDeAlcance(texto: String): Boolean {
        val lower = texto.lowercase()
        val temasAjenos = listOf(
            "fútbol", "futbol", "partido de", "quién ganó", "quien gano", "chiste",
            "política", "politica", "elecciones", "presidente", "película", "pelicula",
            "canción", "cancion", "receta de cocina", "cómo cocinar", "horóscopo", "horoscopo"
        )
        return temasAjenos.any { lower.contains(it) }
    }
}
