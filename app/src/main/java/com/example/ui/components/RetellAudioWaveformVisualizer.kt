package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandGreenAccent
import com.example.ui.theme.MintInteractive
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.StatusUrgent
import com.example.ui.theme.TextLightBlue
import com.example.ui.theme.TextWhite

/**
 * Componente visualizador de forma de onda de audio en tiempo real que reacciona
 * directamente a la entrada del micrófono del agente de voz Retell AI.
 * Proporciona retroalimentación visual inmediata durante conversaciones de voz y llamadas bidireccionales.
 */
@Composable
fun RetellAudioWaveformVisualizer(
    waveform: List<Float>,
    micAmplitude: Float = 0f,
    isVoiceActive: Boolean = true,
    isMicMuted: Boolean = false,
    modifier: Modifier = Modifier,
    height: Dp = 68.dp,
    barCount: Int = 16,
    showStatusBadge: Boolean = true
) {
    val effectiveWaveform = if (waveform.isEmpty()) {
        List(barCount) { 0.12f }
    } else {
        waveform.take(barCount).let { list ->
            if (list.size < barCount) list + List(barCount - list.size) { 0.12f } else list
        }
    }

    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isMicMuted) 0.05f else micAmplitude.coerceIn(0.05f, 1f),
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "amplitudeAnim"
    )

    val activePrimaryColor by animateColorAsState(
        targetValue = if (isMicMuted) StatusUrgent else MintInteractive,
        animationSpec = tween(200),
        label = "waveformColor"
    )

    val activeSecondaryColor by animateColorAsState(
        targetValue = if (isMicMuted) StatusUrgent.copy(alpha = 0.5f) else BrandGreenAccent,
        animationSpec = tween(200),
        label = "waveformSecColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NavySurface)
            .border(
                width = 1.dp,
                color = if (isMicMuted) StatusUrgent.copy(alpha = 0.4f) else MintInteractive.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("audio_waveform_visualizer")
            .semantics {
                contentDescription = if (isMicMuted) {
                    "Visualizador de audio de Retell AI: micrófono silenciado"
                } else {
                    "Visualizador de audio en tiempo real de Retell AI reaccionando al micrófono, amplitud ${(animatedAmplitude * 100).toInt()}%"
                }
            }
    ) {
        if (showStatusBadge) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isMicMuted) StatusUrgent.copy(alpha = 0.2f) else MintInteractive.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isMicMuted) StatusUrgent else MintInteractive,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMicMuted) "Micrófono Silenciado" else "Entrada de Voz • Micrófono Activo",
                        color = if (isMicMuted) StatusUrgent else TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = NavyDark,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isMicMuted) StatusUrgent.copy(alpha = 0.3f) else MintInteractive.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isMicMuted) StatusUrgent else MintInteractive,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isMicMuted) "Mute" else "${(animatedAmplitude * 100).toInt()}%",
                            color = if (isMicMuted) StatusUrgent else MintInteractive,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Lienzo de Forma de Onda en Tiempo Real
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(8.dp))
                .background(NavyDark.copy(alpha = 0.65f))
                .testTag("waveform_canvas")
        ) {
            val totalBars = effectiveWaveform.size
            if (totalBars == 0) return@Canvas

            val availableWidth = size.width
            val availableHeight = size.height
            val spacing = 4.dp.toPx()
            val barWidth = ((availableWidth - (spacing * (totalBars + 1))) / totalBars).coerceAtLeast(3.dp.toPx())
            val minBarHeight = 6.dp.toPx()

            val brush = Brush.verticalGradient(
                colors = listOf(activePrimaryColor, activeSecondaryColor),
                startY = 0f,
                endY = availableHeight
            )

            for (i in 0 until totalBars) {
                val amp = if (isMicMuted) 0.08f else effectiveWaveform[i].coerceIn(0.08f, 1.0f)
                val calculatedHeight = (availableHeight * amp).coerceIn(minBarHeight, availableHeight - 4.dp.toPx())
                val x = spacing + i * (barWidth + spacing)
                val y = (availableHeight - calculatedHeight) / 2f

                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, calculatedHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

/**
 * Versión compacta para incrustar en barras de navegación, encabezados de chat o burbujas.
 */
@Composable
fun RetellAudioWaveformVisualizerCompact(
    waveform: List<Float>,
    isMicMuted: Boolean = false,
    modifier: Modifier = Modifier,
    barCount: Int = 12,
    height: Dp = 28.dp,
    tint: Color = MintInteractive
) {
    val effectiveWaveform = if (waveform.isEmpty()) {
        List(barCount) { 0.15f }
    } else {
        waveform.take(barCount).let { list ->
            if (list.size < barCount) list + List(barCount - list.size) { 0.15f } else list
        }
    }

    Canvas(
        modifier = modifier
            .height(height)
            .testTag("audio_waveform_visualizer_compact")
            .semantics {
                contentDescription = if (isMicMuted) "Micrófono en silencio" else "Onda de audio del micrófono"
            }
    ) {
        val totalBars = effectiveWaveform.size
        if (totalBars == 0) return@Canvas

        val availableWidth = size.width
        val availableHeight = size.height
        val spacing = 3.dp.toPx()
        val barWidth = ((availableWidth - (spacing * (totalBars + 1))) / totalBars).coerceAtLeast(2.dp.toPx())
        val minBarHeight = 4.dp.toPx()

        val barColor = if (isMicMuted) StatusUrgent else tint

        for (i in 0 until totalBars) {
            val amp = if (isMicMuted) 0.1f else effectiveWaveform[i].coerceIn(0.1f, 1.0f)
            val calculatedHeight = (availableHeight * amp).coerceIn(minBarHeight, availableHeight)
            val x = spacing + i * (barWidth + spacing)
            val y = (availableHeight - calculatedHeight) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, calculatedHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
