package com.example.ai.voz

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Paso 3 del asistente de voz: hablar.
 * Lee las respuestas del asistente con la voz de Android (TextToSpeech) en español,
 * después de limpiarlas con [VozUtils.limpiarParaVoz]. No necesita internet ni claves.
 */
class LectorVoz(context: Context) : TextToSpeech.OnInitListener {

    private val principal = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var listo = false
    private var fallo = false

    /** Lectura pedida antes de que el motor de voz terminara de iniciar. */
    private var pendiente: Pair<String, () -> Unit>? = null

    /** Número de la lectura actual: evita que el aviso de una lectura vieja (ya detenida) dispare acciones. */
    private var sesion = 0
    private var alTerminar: (() -> Unit)? = null

    private val _hablando = MutableStateFlow(false)
    val hablando: StateFlow<Boolean> = _hablando.asStateFlow()

    init {
        tts = try {
            TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo iniciar el lector de voz: ${e.message}")
            fallo = true
            null
        }
    }

    override fun onInit(status: Int) {
        val motor = tts
        if (status != TextToSpeech.SUCCESS || motor == null) {
            fallo = true
            pendiente?.second?.invoke()
            pendiente = null
            return
        }
        // Español de Colombia; si el celular no lo tiene, otro español.
        val idiomas = listOf(Locale.forLanguageTag(VozUtils.IDIOMA), Locale.forLanguageTag("es-US"), Locale.forLanguageTag("es-ES"), Locale.forLanguageTag("es"))
        listo = idiomas.any { idioma ->
            val r = motor.setLanguage(idioma)
            r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
        }
        if (!listo) {
            fallo = true
            pendiente?.second?.invoke()
            pendiente = null
            return
        }
        motor.setSpeechRate(0.95f) // un poco más pausado, pensado en adultos mayores
        motor.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) = finalizar(utteranceId)

            @Deprecated("Requerido por versiones antiguas de Android")
            override fun onError(utteranceId: String?) = finalizar(utteranceId)

            override fun onError(utteranceId: String?, errorCode: Int) = finalizar(utteranceId)
        })
        pendiente?.let { (texto, despues) -> hablar(texto, despues) }
        pendiente = null
    }

    /** Lee [texto] en voz alta. Al terminar llama a [despues] (también si no se pudo leer). */
    fun hablar(texto: String, despues: () -> Unit = {}) {
        val limpio = VozUtils.limpiarParaVoz(texto)
        if (limpio.isBlank() || fallo) {
            despues()
            return
        }
        val motor = tts
        if (!listo || motor == null) {
            pendiente = limpio to despues
            return
        }
        sesion++
        alTerminar = despues
        _hablando.value = true
        val fragmentos = VozUtils.dividirEnFragmentos(limpio)
        fragmentos.forEachIndexed { i, fragmento ->
            val id = if (i == fragmentos.lastIndex) "fin_$sesion" else "parte_${sesion}_$i"
            motor.speak(fragmento, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, id)
        }
    }

    private fun finalizar(utteranceId: String?) {
        principal.post {
            if (utteranceId != "fin_$sesion") return@post
            _hablando.value = false
            val despues = alTerminar
            alTerminar = null
            despues?.invoke()
        }
    }

    /** Calla la voz sin avisar a nadie (por ejemplo, cuando el paciente vuelve a tocar el micrófono). */
    fun detener() {
        sesion++
        alTerminar = null
        pendiente = null
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error al detener la voz: ${e.message}")
        }
        _hablando.value = false
    }

    fun liberar() {
        detener()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Error al cerrar el lector de voz: ${e.message}")
        }
        tts = null
        listo = false
    }

    private companion object {
        const val TAG = "LectorVoz"
    }
}
