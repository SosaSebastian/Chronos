package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.MensajeChat
import com.example.ai.voz.EstadoEscucha
import com.example.ui.theme.*

/**
 * Pantalla del Asistente Virtual de Medicación, por texto y por voz.
 * Diseñada para adultos mayores con tipografía grande, alto contraste y botones grandes.
 *
 * Voz: el micrófono escucha la pregunta, la envía al mismo asistente del chat y,
 * si el parlante está activo, lee la respuesta en voz alta. Con el modo conversación,
 * después de responder vuelve a escuchar solo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenteVirtualScreen(
    patientName: String = "",
    emergencyPhone: String = com.example.ai.AsistenteMedicacionManager.LINEA_EMERGENCIAS,
    messages: List<MensajeChat>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit,
    // ── Voz ──
    estadoEscucha: EstadoEscucha = EstadoEscucha.INACTIVO,
    textoParcial: String = "",
    nivelVoz: Float = 0f,
    asistenteHablando: Boolean = false,
    lecturaEnVozAlta: Boolean = true,
    modoConversacion: Boolean = false,
    avisoVoz: String? = null,
    onIniciarEscucha: () -> Unit = {},
    onDetenerEscucha: () -> Unit = {},
    onCancelarVoz: () -> Unit = {},
    onAlternarLectura: () -> Unit = {},
    onAlternarModoConversacion: () -> Unit = {},
    onLeerMensaje: (MensajeChat) -> Unit = {},
    onPermisoMicrofonoNegado: () -> Unit = {},
    onDescartarAvisoVoz: () -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var textInput by remember { mutableStateOf("") }
    val escuchando = estadoEscucha != EstadoEscucha.INACTIVO

    // Permiso del micrófono: se pide la primera vez que el paciente toca el micrófono.
    val permisoMicrofono = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) onIniciarEscucha() else onPermisoMicrofonoNegado()
    }
    val tocarMicrofono: () -> Unit = {
        val tienePermiso = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (tienePermiso) onIniciarEscucha() else permisoMicrofono.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Al salir de la pantalla se apagan el micrófono y la voz.
    DisposableEffect(Unit) {
        onDispose { onCancelarVoz() }
    }

    // Auto-scroll al último mensaje al recibir respuestas
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    BackHandler {
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
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (patientName.isNotBlank()) "Para $patientName • Texto y voz" else "Seguimiento de tus medicamentos",
                                color = TextLightBlue,
                                fontSize = 12.sp
                            )
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
                    // Parlante: leer en voz alta las respuestas a preguntas hechas por voz
                    IconButton(
                        onClick = onAlternarLectura,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_toggle_lectura")
                    ) {
                        Icon(
                            imageVector = if (lecturaEnVozAlta) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (lecturaEnVozAlta) "Desactivar respuestas en voz alta" else "Activar respuestas en voz alta",
                            tint = if (lecturaEnVozAlta) MintInteractive else TextMuted
                        )
                    }
                    // Modo conversación: después de responder vuelve a escuchar solo
                    IconButton(
                        onClick = onAlternarModoConversacion,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_toggle_conversacion")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = if (modoConversacion) "Desactivar modo conversación" else "Activar modo conversación",
                            tint = if (modoConversacion) MintInteractive else TextMuted
                        )
                    }
                    // Botón de llamada rápida de emergencia
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$emergencyPhone")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(StatusUrgent)
                            .testTag("btn_emergency_call_assistant")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Llamar a contacto de emergencia",
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
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
                    // Aviso cuando la voz falla (sin permiso, sin conexión, no se entendió...)
                    if (avisoVoz != null) {
                        AvisoVozCard(mensaje = avisoVoz, onCerrar = onDescartarAvisoVoz)
                    }

                    // Mientras el asistente lee una respuesta en voz alta
                    if (asistenteHablando && !escuchando) {
                        HablandoBar(onDetener = onCancelarVoz)
                    }

                    // Mientras el micrófono escucha, este panel reemplaza al teclado
                    if (escuchando) {
                        PanelEscuchando(
                            estado = estadoEscucha,
                            textoParcial = textoParcial,
                            nivel = nivelVoz,
                            modoConversacion = modoConversacion,
                            onEnviar = onDetenerEscucha,
                            onCancelar = onCancelarVoz
                        )
                    } else {
                        // Preguntas sugeridas en carrusel horizontal
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
                            SuggestionChipItem("¿Para qué sirve mi medicina?") {
                                onSendMessage("¿Para qué sirve mi medicamento principal?")
                            }
                            SuggestionChipItem("¿Debo tomar con comida?") {
                                onSendMessage("¿Cuáles de mis medicamentos debo tomar con alimentos?")
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
                            // Campo de texto accesible para adultos mayores
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = {
                                    Text(
                                        text = "Escribe tu duda aquí...",
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

                            // Sin texto escrito: micrófono para preguntar por voz
                            if (textInput.isBlank()) {
                                IconButton(
                                    onClick = tocarMicrofono,
                                    enabled = !isLoading,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(if (isLoading) NavySurfaceVariant else MintInteractive)
                                        .testTag("btn_mic_assistant")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Preguntar por voz",
                                        tint = if (isLoading) TextMuted else NavyDark,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
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
                Spacer(modifier = Modifier.height(8.dp))
                DisclaimerCard()
            }

            // La bienvenida (con el aviso médico) es el primer mensaje de la lista, viene del ViewModel.
            items(messages, key = { it.id }) { msg ->
                if (msg.esDelUsuario) {
                    UserMessageBubble(text = msg.texto)
                } else {
                    AssistantMessageBubble(
                        message = msg,
                        emergencyPhone = emergencyPhone,
                        onEscuchar = { onLeerMensaje(msg) }
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
                text = "Recuerda: Tu asistente te guía con tus horarios y dudas, por texto o por voz, pero no reemplaza a tu médico tratante.",
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
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun AssistantMessageBubble(
    message: MensajeChat,
    emergencyPhone: String,
    onEscuchar: () -> Unit = {}
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
                // Etiqueta de emergencia si aplica
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 23.sp
                )

                // Escuchar esta respuesta en voz alta
                TextButton(
                    onClick = onEscuchar,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.testTag("btn_escuchar_${message.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MintInteractive,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Escuchar", color = MintInteractive, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                // Botón directo de emergencia si el síntoma requiere auxilio
                if (isEmergency) {
                    Spacer(modifier = Modifier.height(10.dp))
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
                    text = "Pensando tu respuesta...",
                    color = TextLightBlue,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/** Panel que reemplaza el teclado mientras el micrófono escucha. */
@Composable
fun PanelEscuchando(
    estado: EstadoEscucha,
    textoParcial: String,
    nivel: Float,
    modoConversacion: Boolean,
    onEnviar: () -> Unit,
    onCancelar: () -> Unit
) {
    // Pulso suave constante + el círculo crece con el volumen de la voz
    val pulso = rememberInfiniteTransition(label = "pulso_mic")
    val escalaPulso by pulso.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "escala_pulso"
    )
    val escalaVoz by animateFloatAsState(targetValue = 1f + nivel * 0.35f, label = "escala_voz")
    val procesando = estado == EstadoEscucha.PROCESANDO

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("panel_escuchando")
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(if (procesando) 1f else escalaPulso * escalaVoz)
                    .clip(CircleShape)
                    .background(MintInteractive.copy(alpha = 0.25f))
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MintInteractive)
            ) {
                if (procesando) {
                    CircularProgressIndicator(color = NavyDark, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                } else {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = NavyDark, modifier = Modifier.size(30.dp))
                }
            }
        }
        Text(
            text = if (procesando) "Entendiendo lo que dijiste..." else "Te escucho, habla con calma",
            color = TextWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        if (modoConversacion) {
            Text(text = "Modo conversación activo", color = MintInteractive, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = textoParcial.ifBlank { "Por ejemplo: ¿qué medicamentos me tocan hoy?" },
            color = if (textoParcial.isBlank()) TextLightBlue.copy(alpha = 0.7f) else TextWhite,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("texto_parcial_voz")
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("btn_cancelar_voz")
            ) {
                Text("Cancelar", color = TextLightBlue, fontSize = 15.sp)
            }
            Button(
                onClick = onEnviar,
                enabled = !procesando,
                colors = ButtonDefaults.buttonColors(containerColor = MintInteractive, contentColor = NavyDark),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("btn_detener_escucha")
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ya terminé", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Barra que aparece mientras el asistente lee una respuesta en voz alta. */
@Composable
fun HablandoBar(onDetener: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NavySurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("barra_hablando")
    ) {
        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MintInteractive, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Leyendo la respuesta...", color = TextWhite, fontSize = 14.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onDetener, modifier = Modifier.testTag("btn_callar_voz")) {
            Text("Detener", color = MintInteractive, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Mensaje claro cuando la voz no funciona; el paciente siempre puede escribir. */
@Composable
fun AvisoVozCard(mensaje: String, onCerrar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StatusUrgent.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("aviso_voz")
    ) {
        Icon(Icons.Default.MicOff, contentDescription = null, tint = StatusUrgent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = mensaje, color = TextWhite, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onCerrar, modifier = Modifier.minimumInteractiveComponentSize()) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar aviso", tint = TextLightBlue, modifier = Modifier.size(18.dp))
        }
    }
}
