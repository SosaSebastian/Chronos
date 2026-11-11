package com.example.ai.voz

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado del micrófono del asistente. */
enum class EstadoEscucha { INACTIVO, ESCUCHANDO, PROCESANDO }

/**
 * Paso 1 del asistente de voz: escuchar.
 * Usa el reconocedor de voz de Android (SpeechRecognizer) en español de Colombia,
 * muestra lo que el paciente va diciendo y entrega el texto final.
 * Debe usarse desde el hilo principal (así lo exige Android).
 */
class ReconocedorVoz(
    context: Context,
    // Se puede reemplazar en las pruebas para simular un celular sin reconocimiento de voz.
    private val hayReconocedor: (Context) -> Boolean = { SpeechRecognizer.isRecognitionAvailable(it) }
) {

    private val appContext = context.applicationContext
    private var reconocedor: SpeechRecognizer? = null

    private val _estado = MutableStateFlow(EstadoEscucha.INACTIVO)
    val estado: StateFlow<EstadoEscucha> = _estado.asStateFlow()

    /** Lo que se va reconociendo mientras el paciente habla. */
    private val _textoParcial = MutableStateFlow("")
    val textoParcial: StateFlow<String> = _textoParcial.asStateFlow()

    /** Volumen de la voz de 0 a 1, para la animación del micrófono. */
    private val _nivel = MutableStateFlow(0f)
    val nivel: StateFlow<Float> = _nivel.asStateFlow()

    fun disponible(): Boolean = try {
        hayReconocedor(appContext)
    } catch (e: Exception) {
        false
    }

    /**
     * Empieza a escuchar. Cuando el paciente termina de hablar llama a [alReconocer] con el texto;
     * si algo falla llama a [alFallar] con un mensaje claro. Nunca cierra la app.
     */
    fun escuchar(alReconocer: (String) -> Unit, alFallar: (String) -> Unit) {
        cancelar()
        if (!disponible()) {
            alFallar(VozUtils.SIN_RECONOCEDOR)
            return
        }
        val sr = try {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo crear el reconocedor: ${e.message}")
            null
        }
        if (sr == null) {
            alFallar(VozUtils.SIN_RECONOCEDOR)
            return
        }
        reconocedor = sr

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _estado.value = EstadoEscucha.ESCUCHANDO
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) {
                _nivel.value = VozUtils.normalizarNivel(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                _estado.value = EstadoEscucha.PROCESANDO
                _nivel.value = 0f
            }

            override fun onError(error: Int) {
                // Solo se avisa si este reconocedor sigue siendo el activo (no uno cancelado a propósito).
                if (reconocedor !== sr) return
                terminar()
                alFallar(VozUtils.mensajeDeError(error))
            }

            override fun onResults(results: Bundle?) {
                if (reconocedor !== sr) return
                val texto = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                terminar()
                if (texto.isBlank()) {
                    alFallar(VozUtils.mensajeDeError(SpeechRecognizer.ERROR_NO_MATCH))
                } else {
                    alReconocer(texto)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val parcial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!parcial.isNullOrBlank()) _textoParcial.value = parcial
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        _textoParcial.value = ""
        _estado.value = EstadoEscucha.ESCUCHANDO
        try {
            sr.startListening(VozUtils.crearIntentReconocimiento())
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo iniciar la escucha: ${e.message}")
            terminar()
            alFallar(VozUtils.mensajeDeError(SpeechRecognizer.ERROR_CLIENT))
        }
    }

    /** El paciente terminó de hablar: se procesa lo que alcanzó a decir. */
    fun detener() {
        if (_estado.value == EstadoEscucha.ESCUCHANDO) {
            _estado.value = EstadoEscucha.PROCESANDO
            try {
                reconocedor?.stopListening()
            } catch (e: Exception) {
                cancelar()
            }
        }
    }

    /** Cancela sin enviar nada (por ejemplo, al salir de la pantalla). */
    fun cancelar() {
        val sr = reconocedor
        reconocedor = null
        try {
            sr?.cancel()
            sr?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error al cancelar el reconocedor: ${e.message}")
        }
        _estado.value = EstadoEscucha.INACTIVO
        _nivel.value = 0f
        _textoParcial.value = ""
    }

    private fun terminar() {
        val sr = reconocedor
        reconocedor = null
        try {
            sr?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error al liberar el reconocedor: ${e.message}")
        }
        _estado.value = EstadoEscucha.INACTIVO
        _nivel.value = 0f
    }

    fun liberar() = cancelar()

    private companion object {
        const val TAG = "ReconocedorVoz"
    }
}
