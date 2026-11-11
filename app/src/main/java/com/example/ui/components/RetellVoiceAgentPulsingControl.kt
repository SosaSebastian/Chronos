package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Componente UI Composable que muestra una animación pulsante fluida y reactiva
 * cuando el agente de voz de Retell AI está activo, y provee un botón accesible para
 * alternar el estado del micrófono (Silenciar / Activar).
 *
 * @param isVoiceActive Indica si el agente de voz de Retell AI está actualmente en llamada o activo.
 * @param isMicMuted Indica si el micrófono del usuario está silenciado.
 * @param onToggleMic Función callback invocada al presionar el botón de micrófono.
 * @param modifier Modificador de diseño.
 * @param agentName Nombre identificador del agente o paciente.
 * @param callDurationSeconds Duración de la llamada activa en segundos para el cronómetro.
 * @param isCompact Modo compacto para barras flotantes o chat.
 */
@Composable
fun RetellVoiceAgentPulsingControl(
    isVoiceActive: Boolean,
    isMicMuted: Boolean,
    onToggleMic: () -> Unit,
    modifier: Modifier = Modifier,
    agentName: String = "Agente Retell AI",
    callDurationSeconds: Int = 0,
    isCompact: Boolean = false
) {
    // Animación de pulso continuo para anillos concéntricos
    val infiniteTransition = rememberInfiniteTransition(label = "retellPulsingTransition")

    val pulseScaleOuter by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScaleOuter"
    )

    val pulseAlphaOuter by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlphaOuter"
    )

    val pulseScaleInner by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScaleInner"
    )

    // Alturas animadas para las 5 barras del ecualizador de voz
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(400, delayMillis = 0, easing = FastOutLinearInEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(550, delayMillis = 100, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(tween(350, delayMillis = 50, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar3"
    )
    val bar4Height by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(tween(480, delayMillis = 150, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "bar4"
    )
    val bar5Height by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(420, delayMillis = 80, easing = FastOutLinearInEasing), RepeatMode.Reverse),
        label = "bar5"
    )

    val minutes = callDurationSeconds / 60
    val seconds = callDurationSeconds % 60
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)

    val micButtonColor by animateColorAsState(
        targetValue = if (isMicMuted) StatusUrgent else MintInteractive,
        animationSpec = tween(300),
        label = "micButtonColor"
    )

    val micButtonTextColor by animateColorAsState(
        targetValue = if (isMicMuted) TextWhite else NavyDark,
        animationSpec = tween(300),
        label = "micButtonTextColor"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isVoiceActive) 1.5.dp else 1.dp,
                brush = Brush.horizontalGradient(
                    if (isVoiceActive) {
                        listOf(MintInteractive, BrandGreenAccent)
                    } else {
                        listOf(NavySurfaceVariant, NavySurfaceVariant)
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("retell_voice_pulsing_control")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 12.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Contenedor de la animación pulsante con ondas concéntricas
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 48.dp else 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVoiceActive) {
                        // Onda exterior que se expande y desvanece
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 48.dp else 56.dp)
                                .scale(pulseScaleOuter)
                                .alpha(pulseAlphaOuter)
                                .clip(CircleShape)
                                .background(MintInteractive)
                        )

                        // Onda interior pulsante
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 40.dp else 48.dp)
                                .scale(pulseScaleInner)
                                .clip(CircleShape)
                                .background(MintInteractive.copy(alpha = 0.35f))
                        )
                    }

                    // Núcleo central con ícono de agente de voz
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 36.dp else 44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isVoiceActive) {
                                    Brush.linearGradient(listOf(MintInteractive, BrandGreenAccent))
                                } else {
                                    Brush.linearGradient(listOf(NavySurfaceVariant, NavyDark))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVoiceActive) Icons.Default.RecordVoiceOver else Icons.Default.PhoneInTalk,
                            contentDescription = if (isVoiceActive) "Agente de voz Retell AI hablando" else "Agente de voz inactivo",
                            tint = if (isVoiceActive) NavyDark else TextLightBlue,
                            modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Información textual del estado de la voz
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isVoiceActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusSuccess)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isVoiceActive) "Voz en Vivo Activa" else "Agente Retell AI en Espera",
                            color = TextWhite,
                            fontSize = if (isCompact) 14.sp else 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (isVoiceActive) {
                            if (isMicMuted) "Micrófono silenciado • Agente escuchando en pausa" else "Comunicación bidireccional activa • $formattedDuration"
                        } else {
                            "Listo para llamadas y chat directo"
                        },
                        color = if (isVoiceActive) {
                            if (isMicMuted) StatusUrgent else MintInteractive
                        } else {
                            TextLightBlue
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (isVoiceActive) {
                        Text(
                            text = "Voz humana configurada • $agentName",
                            color = TextLightBlue.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Barras animadas de ecualizador de audio si está activo y no silenciado
                if (isVoiceActive && !isMicMuted) {
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        AudioWaveBar(barHeight = bar1Height.dp)
                        AudioWaveBar(barHeight = bar2Height.dp)
                        AudioWaveBar(barHeight = bar3Height.dp)
                        AudioWaveBar(barHeight = bar4Height.dp)
                        AudioWaveBar(barHeight = bar5Height.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón interactivo accesible para alternar el micrófono (Mute / Unmute)
            Button(
                onClick = onToggleMic,
                colors = ButtonDefaults.buttonColors(
                    containerColor = micButtonColor,
                    contentColor = micButtonTextColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_toggle_mic_pulsing")
                    .semantics {
                        contentDescription = if (isMicMuted) {
                            "Activar micrófono. Actualmente silenciado."
                        } else {
                            "Silenciar micrófono. Actualmente activo."
                        }
                    }
            ) {
                Icon(
                    imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isMicMuted) "Activar Micrófono (Silenciado)" else "Silenciar Micrófono (Activo)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AudioWaveBar(barHeight: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(barHeight)
            .clip(RoundedCornerShape(2.dp))
            .background(MintInteractive)
    )
}
