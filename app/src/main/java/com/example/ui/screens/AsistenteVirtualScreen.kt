package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.MensajeChat
import com.example.ai.retell.*
import com.example.ui.components.RetellAudioWaveformVisualizer
import com.example.ui.components.RetellAudioWaveformVisualizerCompact
import com.example.ui.components.RetellVoiceAgentPulsingControl
import com.example.ui.theme.*

/**
 * Pantalla completa del Asistente Virtual Inteligente de Medicación con Retell AI.
 * Ofrece:
 * 1. Comunicación de voz bidireccional en tiempo real con Retell AI.
 * 2. Visualizador reactivo de forma de onda de audio del micrófono.
 * 3. Transcripción en tiempo real de voz sincronizada en el chat.
 * 4. Salida de voz audible para todas las respuestas generadas.
 * 5. Consulta y anclaje estricto a los medicamentos, dosis y citas registrados en la base de datos.
 * 6. Guardrails de emergencia y fuera de contexto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenteVirtualScreen(
    patientName: String = "",
    emergencyPhone: String = com.example.ai.AsistenteMedicacionManager.LINEA_EMERGENCIAS,
    messages: List<MensajeChat>,
    isLoading: Boolean,
    retellCallState: RetellCallState = RetellCallState.DISCONNECTED,
    retellWebCallUrl: String? = null,
    isRetellConfigured: Boolean = false,
    retellApiKey: String = "",
    retellAgentId: String = "",
    isSpeakerphoneOn: Boolean = true,
    isMicMuted: Boolean = false,
    callDurationSeconds: Int = 0,
    retellAgentSource: RetellAgentSource = RetellAgentSource.NONE,
    liveTranscriptions: List<RetellUtterance> = emptyList(),
    connectionLatencyMs: Long = 0L,
    connectionQuality: RetellConnectionQuality = RetellConnectionQuality.DISCONNECTED,
    isSpeaking: Boolean = false,
    isVoiceAudioEnabled: Boolean = true,
    micAmplitude: Float = 0f,
    waveformAmplitudes: List<Float> = emptyList(),
    onStartRetellCall: () -> Unit = {},
    onEndRetellCall: () -> Unit = {},
    onToggleSpeakerphone: () -> Unit = {},
    onToggleMicMute: () -> Unit = {},
    onToggleVoiceAudio: () -> Unit = {},
    onSpeakMessage: (String) -> Unit = {},
    onStopSpeaking: () -> Unit = {},
    onResetRetellToEnv: () -> Unit = {},
    onSaveRetellCredentials: (String, String) -> Unit = { _, _ -> },
    onTestRetellConnection: ((Boolean, String) -> Unit) -> Unit = {},
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var textInput by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Auto-scroll al último mensaje al recibir nuevas respuestas o transcripciones
    LaunchedEffect(messages.size, liveTranscriptions.size, isLoading) {
        val totalItems = messages.size + (if (liveTranscriptions.isNotEmpty()) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems)
        }
    }

    BackHandler {
        if (retellCallState == RetellCallState.IN_CALL) {
            onEndRetellCall()
        }
        onBack()
    }

    Scaffold(
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MintInteractive),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = NavyDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Asistente de Medicación",
                                color = TextWhite,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (patientName.isNotBlank()) "Para $patientName" else "Salud y Medicamentos",
                                    color = TextLightBlue,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isVoiceAudioEnabled) StatusSuccess else TextMuted)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isVoiceAudioEnabled) "Voz activa" else "Sin audio",
                                    color = if (isVoiceAudioEnabled) MintInteractive else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_assistant_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al panel",
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    // Botón para alternar la salida de voz en voz alta
                    IconButton(
                        onClick = onToggleVoiceAudio,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_toggle_voice_audio")
                    ) {
                        Icon(
                            imageVector = if (isVoiceAudioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isVoiceAudioEnabled) "Desactivar respuesta por voz" else "Activar respuesta por voz",
                            tint = if (isVoiceAudioEnabled) MintInteractive else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Botón de configuración de credenciales de Retell AI
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_retell_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Configuración del agente de voz Retell AI",
                            tint = TextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Botón de llamada directa de emergencias
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$emergencyPhone")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(StatusUrgent)
                            .testTag("btn_emergency_call_assistant")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Llamar a línea de emergencias",
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavySurface,
                    titleContentColor = TextWhite
                )
            )
        },
        bottomBar = {
            Surface(
                color = NavySurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Sugerencias rápidas para consultar los medicamentos registrados
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChipItem("¿Qué pastillas me tocan hoy?") {
                            onSendMessage("¿Qué pastillas me tocan hoy?")
                        }
                        SuggestionChipItem("¿A qué hora tomo mi dosis?") {
                            onSendMessage("¿A qué hora debo tomar mis medicamentos registrados?")
                        }
                        SuggestionChipItem("¿Debo tomar con comida?") {
                            onSendMessage("¿Cuáles de mis medicamentos debo tomar con alimentos?")
                        }
                        SuggestionChipItem("¿Cuándo es mi próxima cita?") {
                            onSendMessage("¿Cuándo es mi próxima cita médica?")
                        }
                        SuggestionChipItem("¿Qué hago si olvidé una dosis?") {
                            onSendMessage("¿Qué debo hacer si olvidé tomar mi medicamento a la hora?")
                        }
                    }

                    // Fila de entrada de texto
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = {
                                Text(
                                    text = "Pregunta sobre tus medicamentos...",
                                    color = TextLightBlue.copy(alpha = 0.7f),
                                    fontSize = 15.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_assistant_query"),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NavyDark,
                                unfocusedContainerColor = NavyDark,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = MintInteractive,
                                unfocusedBorderColor = NavySurfaceVariant,
                                cursorColor = MintInteractive
                            ),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Botón de Enviar Mensaje
                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    val query = textInput.trim()
                                    textInput = ""
                                    onSendMessage(query)
                                }
                            },
                            enabled = textInput.isNotBlank() && !isLoading,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (textInput.isNotBlank()) MintInteractive else NavySurfaceVariant)
                                .testTag("btn_send_query")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar pregunta",
                                tint = if (textInput.isNotBlank()) NavyDark else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                DisclaimerCard()
            }

            // Hero Banner de Retell AI con animación pulsante y visualizador de forma de onda
            item {
                RetellAiHeroCard(
                    retellCallState = retellCallState,
                    isRetellConfigured = isRetellConfigured,
                    retellAgentId = retellAgentId,
                    retellAgentSource = retellAgentSource,
                    isSpeakerphoneOn = isSpeakerphoneOn,
                    isMicMuted = isMicMuted,
                    callDurationSeconds = callDurationSeconds,
                    waveformAmplitudes = waveformAmplitudes,
                    micAmplitude = micAmplitude,
                    connectionLatencyMs = connectionLatencyMs,
                    connectionQuality = connectionQuality,
                    onStartCall = onStartRetellCall,
                    onEndCall = onEndRetellCall,
                    onToggleSpeaker = onToggleSpeakerphone,
                    onToggleMic = onToggleMicMute,
                    onOpenSettings = { showSettingsDialog = true }
                )
            }

            // Indicador de voz activa cuando el asistente está hablando en voz alta
            if (isSpeaking) {
                item {
                    SpeakingVoiceIndicatorCard(
                        waveform = waveformAmplitudes,
                        onStopSpeaking = onStopSpeaking
                    )
                }
            }

            // Transcripciones en tiempo real proporcionadas por el agente de voz de Retell AI
            if (liveTranscriptions.isNotEmpty()) {
                item {
                    RetellRealTimeTranscriptionCard(transcriptions = liveTranscriptions)
                }
            }

            // Mensajes de la conversación en texto
            items(messages, key = { it.id }) { msg ->
                if (msg.esDelUsuario) {
                    UserMessageBubble(text = msg.texto)
                } else {
                    AssistantMessageBubble(
                        message = msg,
                        emergencyPhone = emergencyPhone,
                        onSpeak = { onSpeakMessage(msg.texto) }
                    )
                }
            }

            // Indicador de carga amigable
            if (isLoading) {
                item {
                    LoadingAssistantBubble()
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Diálogo interactivo de llamada bidireccional activa con Retell AI (WebRTC)
    if (retellCallState == RetellCallState.IN_CALL && !retellWebCallUrl.isNullOrBlank()) {
        RetellBidirectionalCallDialog(
            webCallUrl = retellWebCallUrl,
            callDurationSeconds = callDurationSeconds,
            isSpeakerphoneOn = isSpeakerphoneOn,
            isMicMuted = isMicMuted,
            waveform = waveformAmplitudes,
            micAmplitude = micAmplitude,
            onToggleSpeaker = onToggleSpeakerphone,
            onToggleMic = onToggleMicMute,
            onHangup = onEndRetellCall
        )
    }

    // Diálogo de configuración y verificación de credenciales de Retell AI
    if (showSettingsDialog) {
        RetellSettingsDialog(
            initialApiKey = retellApiKey,
            initialAgentId = retellAgentId,
            agentSource = retellAgentSource,
            onDismiss = { showSettingsDialog = false },
            onSave = { key, agent ->
                onSaveRetellCredentials(key, agent)
                showSettingsDialog = false
            },
            onResetToEnv = {
                onResetRetellToEnv()
                showSettingsDialog = false
            },
            onTestConnection = onTestRetellConnection
        )
    }
}

/**
 * Hero Card de Retell AI con controles de voz, animación de pulso y visualizador de ondas.
 */
@Composable
fun RetellAiHeroCard(
    retellCallState: RetellCallState,
    isRetellConfigured: Boolean,
    retellAgentId: String,
    retellAgentSource: RetellAgentSource,
    isSpeakerphoneOn: Boolean,
    isMicMuted: Boolean,
    callDurationSeconds: Int,
    waveformAmplitudes: List<Float>,
    micAmplitude: Float,
    connectionLatencyMs: Long,
    connectionQuality: RetellConnectionQuality,
    onStartCall: () -> Unit,
    onEndCall: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleMic: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isInCall = retellCallState == RetellCallState.IN_CALL
    val isConnecting = retellCallState == RetellCallState.CONNECTING

    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    if (isInCall) {
                        listOf(MintInteractive, BrandGreenAccent)
                    } else {
                        listOf(NavySurfaceVariant, MintInteractive.copy(alpha = 0.4f))
                    }
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("retell_ai_hero_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Fila superior: Título, fuente del agente e indicador de calidad de conexión
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isInCall) StatusSuccess else if (isRetellConfigured) MintInteractive else StatusWarning)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isInCall) "Llamada de Voz en Vivo" else "Agente de Voz Retell AI",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Indicador de Calidad de Conexión y Latencia
                RetellConnectionQualityIndicator(
                    quality = connectionQuality,
                    latencyMs = connectionLatencyMs
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Componente de control de pulsaciones y micrófono
            RetellVoiceAgentPulsingControl(
                isVoiceActive = isInCall,
                isMicMuted = isMicMuted,
                onToggleMic = onToggleMic,
                agentName = if (retellAgentId.isNotBlank()) "ID: ${retellAgentId.take(8)}..." else "Retell AI",
                callDurationSeconds = callDurationSeconds,
                isCompact = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Visualizador de Forma de Onda en Tiempo Real reaccionando al micrófono
            RetellAudioWaveformVisualizer(
                waveform = waveformAmplitudes,
                micAmplitude = micAmplitude,
                isVoiceActive = isInCall,
                isMicMuted = isMicMuted,
                height = 54.dp,
                barCount = 18,
                showStatusBadge = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de acción principales de llamada
            if (isInCall) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón para colgar llamada
                    Button(
                        onClick = onEndCall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusUrgent,
                            contentColor = TextWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_end_retell_call")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Finalizar Llamada", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Botón altavoz
                    FilledTonalButton(
                        onClick = onToggleSpeaker,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isSpeakerphoneOn) MintInteractive else NavySurfaceVariant,
                            contentColor = if (isSpeakerphoneOn) NavyDark else TextWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(46.dp)
                            .testTag("btn_toggle_speakerphone")
                    ) {
                        Icon(
                            imageVector = if (isSpeakerphoneOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isSpeakerphoneOn) "Altavoz" else "Auricular", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón para iniciar llamada bidireccional con Retell AI
                    Button(
                        onClick = onStartCall,
                        enabled = !isConnecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintInteractive,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_start_retell_call")
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NavyDark,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Conectando voz...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hablar por Voz (Retell AI)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Botón de acceso a ajustes
                    OutlinedButton(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MintInteractive.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MintInteractive),
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("btn_open_settings_hero")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurar", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Indicador visual de Calidad de Conexión y Latencia con Retell AI.
 */
@Composable
fun RetellConnectionQualityIndicator(
    quality: RetellConnectionQuality,
    latencyMs: Long,
    modifier: Modifier = Modifier
) {
    val dotColor = when (quality) {
        RetellConnectionQuality.EXCELLENT -> StatusSuccess
        RetellConnectionQuality.GOOD -> MintInteractive
        RetellConnectionQuality.MODERATE -> StatusWarning
        RetellConnectionQuality.POOR, RetellConnectionQuality.DISCONNECTED -> StatusUrgent
        RetellConnectionQuality.CONNECTING -> BrandGreenAccent
    }

    Surface(
        color = NavyDark,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, dotColor.copy(alpha = 0.35f)),
        modifier = modifier.testTag("connection_quality_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (latencyMs > 0) "${quality.label} • ${latencyMs}ms" else quality.label,
                color = TextLightBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Indicador animado cuando el asistente está leyendo en voz alta una respuesta.
 */
@Composable
fun SpeakingVoiceIndicatorCard(
    waveform: List<Float>,
    onStopSpeaking: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MintInteractive.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .testTag("speaking_voice_indicator")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MintInteractive),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = NavyDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Respondiendo por voz...",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Voz configurada activa • Escuchando respuesta",
                        color = TextLightBlue,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                RetellAudioWaveformVisualizerCompact(
                    waveform = waveform,
                    modifier = Modifier.width(50.dp)
                )
            }

            TextButton(
                onClick = onStopSpeaking,
                modifier = Modifier.testTag("btn_stop_speaking")
            ) {
                Text("Detener", color = StatusUrgent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * LazyColumn / Card de transcripciones en tiempo real del agente de voz de Retell AI.
 */
@Composable
fun RetellRealTimeTranscriptionCard(transcriptions: List<RetellUtterance>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MintInteractive.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .testTag("RetellRealTimeTranscriptionLazyColumn")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = MintInteractive,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transcripción de Voz en Tiempo Real (Retell AI)",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mostramos los últimos enunciados de voz en vivo
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                transcriptions.takeLast(6).forEach { u ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (u.isUser) NavyDark.copy(alpha = 0.5f) else MintInteractive.copy(alpha = 0.1f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = if (u.isUser) NavySurfaceVariant else MintInteractive,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (u.isUser) "Usuario" else "Agente IA",
                                color = if (u.isUser) TextWhite else NavyDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = u.content,
                            color = TextWhite,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionChipItem(label: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextLightBlue
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = NavyDark
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = MintInteractive.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.minimumInteractiveComponentSize()
    )
}

@Composable
fun DisclaimerCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MintInteractive,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Recuerda: Tu asistente te guía con tus horarios y dudas basándose estrictamente en tus medicamentos registrados.",
                color = TextLightBlue,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun UserMessageBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(MintInteractive)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                color = NavyDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
fun AssistantMessageBubble(
    message: MensajeChat,
    emergencyPhone: String,
    onSpeak: () -> Unit
) {
    val context = LocalContext.current
    val isEmergency = message.esAlertaEmergencia

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isEmergency) NavySurface.copy(alpha = 0.95f) else NavySurface
            ),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            modifier = Modifier
                .widthIn(max = 330.dp)
                .border(
                    width = if (isEmergency) 2.dp else 1.dp,
                    color = if (isEmergency) StatusUrgent else MintInteractive.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Etiqueta de emergencia médica
                if (isEmergency) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusUrgent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusUrgent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ATENCIÓN MÉDICA PRIORITARIA",
                            color = StatusUrgent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = message.texto,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fila de acciones al pie de la burbuja: Botón para escuchar la respuesta por voz
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MintInteractive)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Agente IA",
                            color = TextLightBlue,
                            fontSize = 11.sp
                        )
                    }

                    // Botón para escuchar/reproducir la respuesta por voz
                    IconButton(
                        onClick = onSpeak,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(NavyDark)
                            .testTag("btn_speak_assistant_message")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Escuchar respuesta en voz alta",
                            tint = MintInteractive,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Botón directo de emergencia si el síntoma requiere auxilio
                if (isEmergency) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$emergencyPhone")
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusUrgent,
                            contentColor = TextWhite
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("btn_emergency_call_bubble")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Llamar a Emergencias", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingAssistantBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MintInteractive,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Consultando información y sintetizando voz...",
                    color = TextLightBlue,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Diálogo de llamada WebRTC bidireccional interactiva con Retell AI.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RetellBidirectionalCallDialog(
    webCallUrl: String,
    callDurationSeconds: Int,
    isSpeakerphoneOn: Boolean,
    isMicMuted: Boolean,
    waveform: List<Float>,
    micAmplitude: Float,
    onToggleSpeaker: () -> Unit,
    onToggleMic: () -> Unit,
    onHangup: () -> Unit
) {
    val minutes = callDurationSeconds / 60
    val seconds = callDurationSeconds % 60
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)

    Dialog(
        onDismissRequest = onHangup,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NavyDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header de la llamada
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(MintInteractive),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = NavyDark,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Agente de Voz Retell AI",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "En llamada • $formattedDuration",
                        color = MintInteractive,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // WebView invisible / incrustado para ejecutar la sesión de voz WebRTC de Retell AI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                webChromeClient = object : WebChromeClient() {
                                    override fun onPermissionRequest(request: PermissionRequest?) {
                                        request?.grant(request.resources)
                                    }
                                }
                                webViewClient = WebViewClient()
                                loadUrl(webCallUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay con Visualizador de Onda
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        RetellAudioWaveformVisualizer(
                            waveform = waveform,
                            micAmplitude = micAmplitude,
                            isVoiceActive = true,
                            isMicMuted = isMicMuted,
                            height = 60.dp,
                            barCount = 20,
                            showStatusBadge = true
                        )
                    }
                }

                // Barra de Controles de Llamada
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Silenciar / Activar micrófono
                    IconButton(
                        onClick = onToggleMic,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isMicMuted) StatusUrgent else NavySurface)
                            .testTag("btn_call_toggle_mic")
                    ) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isMicMuted) "Activar micrófono" else "Silenciar micrófono",
                            tint = if (isMicMuted) TextWhite else MintInteractive,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Botón para colgar llamada
                    IconButton(
                        onClick = onHangup,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(StatusUrgent)
                            .testTag("btn_call_hangup")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Finalizar llamada",
                            tint = TextWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Altavoz
                    IconButton(
                        onClick = onToggleSpeaker,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerphoneOn) MintInteractive else NavySurface)
                            .testTag("btn_call_toggle_speaker")
                    ) {
                        Icon(
                            imageVector = if (isSpeakerphoneOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = "Alternar altavoz",
                            tint = if (isSpeakerphoneOn) NavyDark else TextWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Diálogo para configurar y probar las credenciales del Agente de Retell AI.
 */
@Composable
fun RetellSettingsDialog(
    initialApiKey: String,
    initialAgentId: String,
    agentSource: RetellAgentSource,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onResetToEnv: () -> Unit,
    onTestConnection: ((Boolean, String) -> Unit) -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(initialApiKey) }
    var agentIdInput by remember { mutableStateOf(initialAgentId) }
    var testResultMsg by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isTestSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MintInteractive)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configuración Retell AI", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Configura tu Agent ID y API Key de Retell AI para la comunicación de voz bidireccional y chat directo.",
                    color = TextLightBlue,
                    fontSize = 13.sp
                )

                // Indicador de origen del Agent ID
                Surface(
                    color = NavyDark,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MintInteractive, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (agentSource) {
                                RetellAgentSource.ENVIRONMENT -> "Origen: Variables de entorno (.env)"
                                RetellAgentSource.MANUAL -> "Origen: Configuración manual guardada"
                                RetellAgentSource.NONE -> "Origen: Sin configurar"
                            },
                            color = TextLightBlue,
                            fontSize = 11.sp
                        )
                    }
                }

                // Campo API Key
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Retell API Key") },
                    placeholder = { Text("key_xxxxxxxxxx") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = MintInteractive,
                        unfocusedBorderColor = NavySurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_retell_api_key")
                )

                // Campo Agent ID
                OutlinedTextField(
                    value = agentIdInput,
                    onValueChange = { agentIdInput = it },
                    label = { Text("Retell Agent ID") },
                    placeholder = { Text("agent_xxxxxxxxxx") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = MintInteractive,
                        unfocusedBorderColor = NavySurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_retell_agent_id")
                )

                // Resultado del test de conexión si existe
                testResultMsg?.let { msg ->
                    Surface(
                        color = if (isTestSuccess) StatusSuccess.copy(alpha = 0.2f) else StatusUrgent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isTestSuccess) StatusSuccess else StatusUrgent
                        )
                    ) {
                        Text(
                            text = msg,
                            color = if (isTestSuccess) MintInteractive else StatusUrgent,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Botón para probar la conexión
                OutlinedButton(
                    onClick = {
                        isTesting = true
                        testResultMsg = null
                        onTestConnection { success, message ->
                            isTesting = false
                            isTestSuccess = success
                            testResultMsg = message
                        }
                    },
                    enabled = !isTesting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_test_retell_connection")
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MintInteractive, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verificando agente...")
                    } else {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Probar Conexión con Retell AI")
                    }
                }

                // Botón para restablecer a los valores del .env
                TextButton(
                    onClick = onResetToEnv,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_reset_retell_env")
                ) {
                    Text("Restablecer a valores de entorno (.env)", color = TextLightBlue, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(apiKeyInput, agentIdInput) },
                colors = ButtonDefaults.buttonColors(containerColor = MintInteractive, contentColor = NavyDark),
                modifier = Modifier.testTag("btn_save_retell_config")
            ) {
                Text("Guardar y Aplicar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = TextLightBlue)
            }
        },
        containerColor = NavySurface
    )
}
