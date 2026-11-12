package com.example.ai.voz

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.ai.MensajeChat

/**
 * Reglas puras del asistente de voz (sin hardware), para poder probarlas con pruebas unitarias.
 *
 * Flujo de voz del asistente:
 *  1. Escuchar: ReconocedorVoz convierte la voz del paciente en texto (reconocedor de Android, es-CO).
 *  2. Pensar: ese texto entra al MISMO AsistenteMedicacionManager del chat (filtros, aviso médico, Gemini, respaldo).
 *  3. Hablar: LectorVoz lee la respuesta con la voz de Android, ya limpia con [limpiarParaVoz].
 */
/** Datos de una voz del celular, sin depender de Android, para poder probar cómo se elige. */
data class OpcionVoz(
    val nombre: String,
    val idioma: String,
    val pais: String,
    val calidad: Int,
    val requiereInternet: Boolean,
    val instalada: Boolean
)

object VozUtils {

    /** Idioma principal del reconocimiento y de la lectura. */
    const val IDIOMA = "es-CO"

    /** Acentos en orden de preferencia: el latino de EE. UU. de Google es el más cercano al colombiano cuando no hay es-CO. */
    val ACENTOS_PREFERIDOS = listOf("es-CO", "es-US", "es-MX", "es-419", "es-ES")

    /** Velocidad y tono de la voz (1.0 = normal). */
    const val VELOCIDAD = 1.0f
    const val TONO = 1.0f

    /**
     * Escoge la voz más parecida a una voz colombiana y menos robótica:
     * 1) acento (Colombia, luego latino, España de último), 2) calidad, 3) instalada en el celular.
     * Devuelve null si no hay voces en español.
     */
    fun elegirMejorVoz(voces: List<OpcionVoz>): OpcionVoz? {
        fun rangoAcento(v: OpcionVoz): Int {
            // Algunos motores reportan el país en 3 letras (COL, USA, MEX).
            val pais = when (v.pais.uppercase()) {
                "COL" -> "CO"; "USA" -> "US"; "MEX" -> "MX"; "ESP" -> "ES"
                else -> v.pais
            }
            val etiqueta = "es-$pais".lowercase()
            val i = ACENTOS_PREFERIDOS.indexOfFirst { it.lowercase() == etiqueta }
            return if (i >= 0) i else ACENTOS_PREFERIDOS.size
        }
        return voces
            .filter { it.idioma.equals("es", ignoreCase = true) || it.idioma.equals("spa", ignoreCase = true) }
            .filter { it.instalada || it.requiereInternet }
            .sortedWith(
                compareBy<OpcionVoz> { rangoAcento(it) }
                    .thenByDescending { it.calidad }
                    .thenBy { it.requiereInternet } // a igual calidad, la que funciona sin internet
            )
            .firstOrNull()
    }

    /** Largo máximo de cada fragmento que se manda al lector (Android acepta hasta ~4000 caracteres). */
    const val MAX_FRAGMENTO = 3500

    const val SIN_RECONOCEDOR =
        "Este celular no tiene reconocimiento de voz disponible. Puedes escribir tu pregunta."
    const val SIN_PERMISO =
        "Para hablar con el asistente necesito permiso para usar el micrófono. Puedes activarlo en los ajustes o escribir tu pregunta."

    /** Intent del reconocedor: español de Colombia, texto libre y resultados parciales para mostrar lo que se va diciendo. */
    fun crearIntentReconocimiento(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, IDIOMA)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, IDIOMA)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

    /** Mensaje claro para el paciente según el código de error del reconocedor de Android. */
    fun mensajeDeError(codigo: Int): String = when (codigo) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "No te escuché bien. Toca el micrófono e intenta de nuevo, hablando un poco más cerca."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
            "No pude reconocer tu voz porque hay un problema de conexión. Revisa tu internet o escribe tu pregunta."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SIN_PERMISO
        SpeechRecognizer.ERROR_AUDIO ->
            "No pude usar el micrófono. Revisa que ninguna otra app lo esté usando."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS ->
            "El reconocimiento de voz está ocupado. Espera un momento y vuelve a intentarlo."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            "El reconocimiento de voz en español no está disponible en este celular. Puedes escribir tu pregunta."
        else -> "No pude escucharte en este momento. Intenta de nuevo o escribe tu pregunta."
    }

    /** Convierte el volumen del micrófono (dB que entrega Android, aprox. -2 a 10) en un nivel de 0 a 1 para la animación. */
    fun normalizarNivel(rmsDb: Float): Float =
        if (rmsDb.isNaN()) 0f else ((rmsDb + 2f) / 12f).coerceIn(0f, 1f)

    /**
     * Prepara el texto de una respuesta para leerlo en voz alta:
     * quita etiquetas internas, emojis, viñetas y marcas de formato, y expande unidades
     * para que la voz diga "50 miligramos" y no "50 eme ge".
     */
    fun limpiarParaVoz(texto: String): String {
        var t = texto
            // Etiquetas internas del asistente, por ejemplo [ALERTA DE EMERGENCIA]
            .replace(Regex("\\[[A-ZÁÉÍÓÚÑ ]{3,}]"), " ")
            // Emojis y símbolos gráficos
            .replace(Regex("[\\p{So}\\p{Cs}\\uFE0F\\u200D]"), " ")
            // Marcas de formato
            .replace(Regex("[*#_`>]+"), " ")
            // Unidades de dosis
            .replace(Regex("(\\d)\\s*mcg\\b", RegexOption.IGNORE_CASE), "$1 microgramos")
            .replace(Regex("(\\d)\\s*mg\\b", RegexOption.IGNORE_CASE), "$1 miligramos")
            .replace(Regex("(\\d)\\s*ml\\b", RegexOption.IGNORE_CASE), "$1 mililitros")
            .replace(Regex("\\bUI\\b"), "unidades internacionales")

        // Cada línea es una idea: se une con punto para que la voz haga la pausa.
        val lineas = t.lines()
            .map { it.trim().trimStart('•', '-', '–', '·').trim() }
            .filter { it.isNotBlank() }
        t = buildString {
            lineas.forEachIndexed { i, linea ->
                append(linea)
                if (i < lineas.lastIndex) append(if (linea.last() in ".:;,!?") " " else ". ")
            }
        }
        return t.replace(Regex("\\s{2,}"), " ")
            .replace(Regex("\\s+([.,;:!?])"), "$1")
            .replace(Regex("\\.{2,}"), ".")
            .trim()
    }

    /** Parte un texto largo en fragmentos por oraciones, sin pasar de [max] caracteres cada uno. */
    fun dividirEnFragmentos(texto: String, max: Int = MAX_FRAGMENTO): List<String> {
        if (texto.isBlank()) return emptyList()
        if (texto.length <= max) return listOf(texto)
        val fragmentos = mutableListOf<String>()
        val actual = StringBuilder()
        for (oracion in texto.split(Regex("(?<=[.!?])\\s+"))) {
            // Una oración más larga que el máximo se corta a la fuerza.
            val piezas = if (oracion.length > max) oracion.chunked(max) else listOf(oracion)
            for (pieza in piezas) {
                if (actual.isNotEmpty() && actual.length + 1 + pieza.length > max) {
                    fragmentos += actual.toString()
                    actual.clear()
                }
                if (actual.isNotEmpty()) actual.append(' ')
                actual.append(pieza)
            }
        }
        if (actual.isNotEmpty()) fragmentos += actual.toString()
        return fragmentos
    }

    /**
     * Modo conversación: después de responder, ¿el asistente vuelve a escuchar solo?
     * Solo si el modo está activo, la pregunta llegó por voz y la respuesta no es una alerta de emergencia
     * (en una emergencia el paciente debe ver la alerta y llamar, no seguir conversando).
     */
    fun debeVolverAEscuchar(modoConversacion: Boolean, preguntaPorVoz: Boolean, respuesta: MensajeChat): Boolean =
        modoConversacion && preguntaPorVoz && !respuesta.esDelUsuario && !respuesta.esAlertaEmergencia
}
