package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.MensajeChat
import com.example.ui.theme.*

/**
 * Pantalla del Asistente Virtual de Medicación (Modo Texto Exclusivo).
 * Diseñada para adultos mayores con tipografía grande, alto contraste,
 * sugerencias rápidas en texto y botones de acción claros sin sintetizador de voz robótico.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenteVirtualScreen(
    patientName: String = "Sr. Sebastian",
    emergencyPhone: String = "+57 310 456 7890",
    messages: List<MensajeChat>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var textInput by remember { mutableStateOf("") }

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
                                text = "Para $patientName • En texto",
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
                Spacer(modifier = Modifier.height(8.dp))
                DisclaimerCard()
            }

            // Mensaje de bienvenida inicial en texto
            item {
                AssistantMessageBubble(
                    message = MensajeChat(
                        texto = "¡Hola $patientName! Soy tu asistente de medicación en texto. Estoy aquí para recordarte tus horarios, dosis y ayudarte a resolver cualquier duda sobre tus pastillas de hoy en forma clara y sencilla.",
                        esDelUsuario = false
                    ),
                    emergencyPhone = emergencyPhone
                )
            }

            items(messages, key = { it.id }) { msg ->
                if (msg.esDelUsuario) {
                    UserMessageBubble(text = msg.texto)
                } else {
                    AssistantMessageBubble(
                        message = msg,
                        emergencyPhone = emergencyPhone
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
                text = "Recuerda: Tu asistente te guía con tus horarios y dudas en texto, pero no reemplaza a tu médico tratante.",
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
    emergencyPhone: String
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
