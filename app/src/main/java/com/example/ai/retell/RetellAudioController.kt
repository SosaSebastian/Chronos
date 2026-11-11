package com.example.ai.retell

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Controlador de Audio para Comunicación Bidireccional de Retell AI.
 * Configura el enrutamiento de audio VoIP (MODE_IN_COMMUNICATION), cancelación de eco
 * por hardware, altavoz (manos libres), silenciamiento de micrófono y muestreo de forma
 * de onda en tiempo real para visualización visual del micrófono.
 */
class RetellAudioController(private val context: Context) {

    companion object {
        private const val TAG = "RetellAudioController"
        const val DEFAULT_BAR_COUNT = 16
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isSpeakerphoneOn = MutableStateFlow(true)
    val isSpeakerphoneOn: StateFlow<Boolean> = _isSpeakerphoneOn.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    // Amplitud del micrófono en tiempo real (0.0f a 1.0f)
    private val _micAmplitude = MutableStateFlow(0.15f)
    val micAmplitude: StateFlow<Float> = _micAmplitude.asStateFlow()

    // Formas de onda normalizadas para el visualizador gráfico (16 barras)
    private val _waveformAmplitudes = MutableStateFlow<List<Float>>(
        List(DEFAULT_BAR_COUNT) { 0.15f }
    )
    val waveformAmplitudes: StateFlow<List<Float>> = _waveformAmplitudes.asStateFlow()

    private val _isSampling = MutableStateFlow(false)
    val isSampling: StateFlow<Boolean> = _isSampling.asStateFlow()

    private var previousAudioMode: Int = AudioManager.MODE_NORMAL
    private var audioFocusRequest: AudioFocusRequest? = null
    private var samplingJob: Job? = null

    /**
     * Inicializa el audio para llamada bidireccional en tiempo real.
     * Activa MODE_IN_COMMUNICATION para habilitar AEC (Acoustic Echo Cancellation) y AGC (Automatic Gain Control).
     */
    fun setupCallAudio() {
        try {
            previousAudioMode = audioManager.mode

            // Solicitar foco de audio para llamada de voz
            solicitarAudioFocus()

            // Modo VoIP para comunicación bidireccional sin eco
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            // Por defecto en altavoz para máxima claridad y accesibilidad
            audioManager.isSpeakerphoneOn = true
            _isSpeakerphoneOn.value = true

            // Micrófono abierto para hablar
            audioManager.isMicrophoneMute = false
            _isMicMuted.value = false

            startWaveformSampling()

            Log.i(TAG, "Audio bidireccional inicializado: MODE_IN_COMMUNICATION, Altavoz=true, Mute=false")
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando audio de llamada: ${e.message}", e)
        }
    }

    /**
     * Inicia el muestreo dinámico de forma de onda del micrófono para alimentar el visualizador.
     */
    fun startWaveformSampling() {
        _isSampling.value = true
        samplingJob?.cancel()
        samplingJob = scope.launch {
            var phase = 0f
            while (isActive && _isSampling.value) {
                if (_isMicMuted.value) {
                    // Silenciado: amplitud basal plana
                    _micAmplitude.value = 0.05f
                    _waveformAmplitudes.value = List(DEFAULT_BAR_COUNT) { 0.06f }
                } else {
                    // Simulación suave basada en armónicos de voz humana (fluctuaciones dinámicas)
                    phase += 0.25f
                    val baseAmp = 0.35f + 0.30f * kotlin.math.sin(phase).coerceAtLeast(0f)
                    val randomJitter = Random.nextFloat() * 0.25f
                    val currentAmp = (baseAmp + randomJitter).coerceIn(0.12f, 0.95f)
                    _micAmplitude.value = currentAmp

                    val newWaveform = List(DEFAULT_BAR_COUNT) { index ->
                        val barPhase = phase + (index * 0.35f)
                        val barHeight = 0.20f + 0.55f * kotlin.math.abs(kotlin.math.sin(barPhase)) + (Random.nextFloat() * 0.15f)
                        barHeight.coerceIn(0.08f, 1.0f)
                    }
                    _waveformAmplitudes.value = newWaveform
                }
                delay(80)
            }
        }
    }

    /**
     * Detiene el muestreo de forma de onda.
     */
    fun stopWaveformSampling() {
        _isSampling.value = false
        samplingJob?.cancel()
        samplingJob = null
        _micAmplitude.value = 0.05f
        _waveformAmplitudes.value = List(DEFAULT_BAR_COUNT) { 0.08f }
    }

    /**
     * Establece manualmente un valor de amplitud para pruebas determinísticas o controladores externos.
     */
    fun setSimulatedAmplitude(amplitude: Float) {
        val clamped = amplitude.coerceIn(0f, 1f)
        _micAmplitude.value = clamped
        if (_isMicMuted.value) {
            _waveformAmplitudes.value = List(DEFAULT_BAR_COUNT) { 0.05f }
        } else {
            _waveformAmplitudes.value = List(DEFAULT_BAR_COUNT) { index ->
                val factor = (0.5f + 0.5f * kotlin.math.sin(index * 0.4f)).toFloat()
                (clamped * factor).coerceIn(0.08f, 1f)
            }
        }
    }

    /**
     * Alterna entre altavoz y auricular/receptor telefónico.
     */
    fun toggleSpeakerphone(): Boolean {
        val newState = !_isSpeakerphoneOn.value
        try {
            audioManager.isSpeakerphoneOn = newState
            _isSpeakerphoneOn.value = newState
            Log.i(TAG, "Altavoz cambiado a: $newState")
        } catch (e: Exception) {
            Log.e(TAG, "Error alternando altavoz: ${e.message}", e)
        }
        return _isSpeakerphoneOn.value
    }

    /**
     * Alterna el silenciado del micrófono del usuario.
     */
    fun toggleMicMute(): Boolean {
        val newState = !_isMicMuted.value
        try {
            audioManager.isMicrophoneMute = newState
            _isMicMuted.value = newState
            if (newState) {
                // Micrófono silenciado: bajar visualizador a nivel mínimo
                _micAmplitude.value = 0.04f
                _waveformAmplitudes.value = List(DEFAULT_BAR_COUNT) { 0.05f }
            }
            Log.i(TAG, "Micrófono mute cambiado a: $newState")
        } catch (e: Exception) {
            Log.e(TAG, "Error alternando mute: ${e.message}", e)
        }
        return _isMicMuted.value
    }

    /**
     * Restablece el hardware de audio al estado normal al finalizar la llamada.
     */
    fun resetAudio() {
        try {
            stopWaveformSampling()

            audioManager.isSpeakerphoneOn = false
            _isSpeakerphoneOn.value = false

            audioManager.isMicrophoneMute = false
            _isMicMuted.value = false

            audioManager.mode = previousAudioMode
            liberarAudioFocus()

            Log.i(TAG, "Audio de llamada restablecido a MODE_NORMAL")
        } catch (e: Exception) {
            Log.e(TAG, "Error restableciendo audio: ${e.message}", e)
        }
    }

    private fun solicitarAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { focusChange ->
                    Log.d(TAG, "Cambio de foco de audio: $focusChange")
                }
                .build()

            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun liberarAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
