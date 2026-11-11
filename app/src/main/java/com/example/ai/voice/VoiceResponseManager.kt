package com.example.ai.voice

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

/**
 * Gestor del Formato de Voz para el Agente IA de Healthy Chronos.
 * Soluciona la ausencia de audio en las respuestas: convierte automáticamente el texto
 * del agente a voz humana audible en español, permitiendo escuchar las respuestas en voz alta,
 * pausarlas o reproducirlas a demanda.
 */
class VoiceResponseManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceResponseManager"
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isVoiceAudioEnabled = MutableStateFlow(true)
    val isVoiceAudioEnabled: StateFlow<Boolean> = _isVoiceAudioEnabled.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastSpokenText = MutableStateFlow<String?>(null)
    val lastSpokenText: StateFlow<String?> = _lastSpokenText.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var tts: TextToSpeech? = null
    private var pendingSpeechText: String? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo inicializar TextToSpeech nativo: ${e.message}")
            _isInitialized.value = true // Para pruebas o entornos simulados
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                // Intentar configurar idioma español preferente
                val spanishLocales = listOf(
                    Locale("es", "ES"),
                    Locale("es", "US"),
                    Locale("es", "CO"),
                    Locale("es", "MX"),
                    Locale("es"),
                    Locale.getDefault()
                )

                var matched = false
                for (loc in spanishLocales) {
                    val result = tts?.setLanguage(loc)
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        matched = true
                        Log.i(TAG, "TTS configurado con idioma: ${loc.displayName}")
                        break
                    }
                }

                if (!matched) {
                    tts?.setLanguage(Locale.getDefault())
                }

                // Ajustar velocidad y tono para adultos mayores (tono cálido, cadencia ligeramente pausada)
                tts?.setSpeechRate(0.92f)
                tts?.setPitch(0.98f)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        scope.launch { _isSpeaking.value = true }
                    }

                    override fun onDone(utteranceId: String?) {
                        scope.launch { _isSpeaking.value = false }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        scope.launch { _isSpeaking.value = false }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.w(TAG, "Error en reproducción TTS: $errorCode")
                        scope.launch { _isSpeaking.value = false }
                    }
                })

                _isInitialized.value = true
                Log.i(TAG, "TextToSpeech inicializado con éxito.")

                pendingSpeechText?.let { text ->
                    pendingSpeechText = null
                    speak(text)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error configurando TTS: ${e.message}")
                _isInitialized.value = true
            }
        } else {
            Log.w(TAG, "Fallo al inicializar TextToSpeech (status=$status)")
            _isInitialized.value = true
        }
    }

    /**
     * Alterna la activación general de respuestas de audio por voz.
     */
    fun toggleVoiceAudio(): Boolean {
        val newState = !_isVoiceAudioEnabled.value
        _isVoiceAudioEnabled.value = newState
        if (!newState && _isSpeaking.value) {
            stop()
        }
        return newState
    }

    fun setVoiceAudioEnabled(enabled: Boolean) {
        _isVoiceAudioEnabled.value = enabled
        if (!enabled && _isSpeaking.value) {
            stop()
        }
    }

    /**
     * Reproduce el texto del agente en voz alta si el audio está habilitado.
     * Limpia formatos de texto o etiquetas especiales para que suene fluido.
     */
    fun speak(texto: String, onComplete: () -> Unit = {}) {
        if (!_isVoiceAudioEnabled.value) {
            Log.d(TAG, "Reproducción de voz desactivada por el usuario.")
            return
        }

        val textoLimpio = limpiarTextoParaVoz(texto)
        if (textoLimpio.isBlank()) return

        _lastSpokenText.value = textoLimpio

        if (!_isInitialized.value) {
            pendingSpeechText = textoLimpio
            return
        }

        try {
            val utteranceId = UUID.randomUUID().toString()
            _isSpeaking.value = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                tts?.speak(textoLimpio, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            } else {
                val params = HashMap<String, String>().apply {
                    put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }
                @Suppress("DEPRECATION")
                tts?.speak(textoLimpio, TextToSpeech.QUEUE_FLUSH, params)
            }

            // En entornos de prueba donde TTS no emite callbacks, se registra el evento
            Log.i(TAG, "Reproduciendo respuesta por voz: ${textoLimpio.take(40)}...")
        } catch (e: Exception) {
            Log.e(TAG, "Error al reproducir voz: ${e.message}", e)
            _isSpeaking.value = false
        }
    }

    /**
     * Detiene inmediatamente cualquier reproducción de voz en curso.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    /**
     * Limpia etiquetas internas, enlaces y caracteres especiales para locución natural.
     */
    fun limpiarTextoParaVoz(texto: String): String {
        return texto
            .replace("[ALERTA DE EMERGENCIA]", "Atención importante:")
            .replace(Regex("""\[.*?\]"""), "")
            .replace(Regex("""[*#_~`]"""), "")
            .replace("https://", "")
            .replace("http://", "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        _isSpeaking.value = false
    }
}
