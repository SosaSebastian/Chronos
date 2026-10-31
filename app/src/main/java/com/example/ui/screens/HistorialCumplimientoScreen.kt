package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.CategoriaCumplimiento
import com.example.data.model.DiaCumplimiento
import com.example.data.model.EstadoRegistroToma
import com.example.data.model.HistorialCumplimientoUiState
import com.example.data.model.PeriodoFiltro
import com.example.data.model.RegistroTomaDetalle
import com.example.ui.theme.*
import com.example.ui.viewmodel.HistorialCumplimientoViewModel
import com.example.util.DatosPacienteInforme

/**
 * Pantalla `HistorialCumplimientoScreen`:
 * Visualización completa y accesible de datos de adherencia y cumplimiento
 * farmacológico para personas adultas mayores y cuidadores en Healty Chronos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialCumplimientoScreen(
    viewModel: HistorialCumplimientoViewModel = viewModel(),
    patientName: String = "Sr. Sebastian Sosa",
    patientAge: Int = 73,
    bloodType: String = "O+",
    doctorName: String = "Dra. Sara Martinez (Cardióloga)",
    emergencyContact: String = "Isabela Sosa (Hija)",
    emergencyPhone: String = "+57 310 456 7890",
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToAsistenteIa: () -> Unit = {},
    onNavigateToPerfil: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val datosPaciente = remember(patientName, patientAge, bloodType, doctorName, emergencyContact, emergencyPhone) {
        val doctorSplit = doctorName.split("(")
        val docName = doctorSplit[0].trim()
        val docSpec = if (doctorSplit.size > 1) doctorSplit[1].replace(")", "").trim() else "Cardióloga"
        DatosPacienteInforme(
            nombreCompleto = patientName,
            edad = patientAge,
            tipoSangre = bloodType,
            medicoTratante = docName,
            especialidadMedico = docSpec,
            contactoEmergencia = emergencyContact,
            telefonoEmergencia = emergencyPhone
        )
    }

    // Manejo de botón atrás físico/gestual
    BackHandler {
        onNavigateToDashboard()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("historial_cumplimiento_screen"),
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavySurface,
                    titleContentColor = TextWhite,
                    navigationIconContentColor = TextWhite
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateToDashboard,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("historial_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver a medicamentos",
                            tint = TextWhite
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Historial de Cumplimiento",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Seguimiento semanal de $patientName",
                            fontSize = 13.sp,
                            color = TextLightBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Botón para exportar directamente el reporte médico en PDF
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Generando PDF para ${datosPaciente.medicoTratante}...", Toast.LENGTH_SHORT).show()
                            viewModel.exportarPdfMedico(
                                context = context,
                                paciente = datosPaciente,
                                onSuccess = {
                                    Toast.makeText(context, "Reporte PDF listo para compartir", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Error al generar PDF: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        enabled = !uiState.isGeneratingPdf,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("export_pdf_topbar_btn")
                    ) {
                        if (uiState.isGeneratingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MintInteractive,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Exportar reporte PDF para médico",
                                tint = MintInteractive
                            )
                        }
                    }

                    // Botón para compartir el reporte médico con la familia o doctora como texto
                    IconButton(
                        onClick = {
                            val reporteTexto = viewModel.generarTextoReporteCompartir(patientName)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, reporteTexto)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Compartir reporte médico de adherencia")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("share_report_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir reporte con el médico o familia",
                            tint = TextLightBlue
                        )
                    }

                    // Botón de recarga
                    IconButton(
                        onClick = { viewModel.cargarEstadisticas() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("refresh_stats_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar estadísticas",
                            tint = TextLightBlue
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = NavySurface,
                contentColor = TextWhite,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToDashboard,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = "Medicamentos"
                        )
                    },
                    label = {
                        Text(text = "Medicamentos", fontSize = 13.sp)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = TextLightBlue,
                        unselectedTextColor = TextLightBlue
                    )
                )

                NavigationBarItem(
                    selected = true,
                    onClick = { /* Ya estamos aquí */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Historial"
                        )
                    },
                    label = {
                        Text(text = "Cumplimiento", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NavyDark,
                        selectedTextColor = MintInteractive,
                        indicatorColor = MintInteractive,
                        unselectedIconColor = TextLightBlue,
                        unselectedTextColor = TextLightBlue
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToAsistenteIa,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Asistente IA"
                        )
                    },
                    label = {
                        Text(text = "Asistente IA", fontSize = 13.sp)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = TextLightBlue,
                        unselectedTextColor = TextLightBlue
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToPerfil,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Mi Perfil"
                        )
                    },
                    label = {
                        Text(text = "Mi Perfil", fontSize = 13.sp)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = TextLightBlue,
                        unselectedTextColor = TextLightBlue
                    )
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
        ) {
            // 1. Selector de períodos
            item {
                SelectorPeriodoRow(
                    periodoSeleccionado = uiState.periodoSeleccionado,
                    onSeleccionar = { viewModel.seleccionarPeriodo(it) }
                )
            }

            // 2. Tarjeta Hero con Indicador Radial / Donut de Cumplimiento General
            item {
                GraficoRadialHeroCard(
                    uiState = uiState
                )
            }

            // 3. Resumen de 3 KPIs Clave (Racha, Dosis a tiempo, Pendientes)
            item {
                ResumenKpiTresColumnas(uiState = uiState)
            }

            // 4. Gráfico de Barras Interactivo de los 7 Días de la Semana
            item {
                GraficoBarrasSemanalCard(
                    dias = uiState.diasHistorial,
                    diaSeleccionado = uiState.diaSeleccionado,
                    metaPorcentaje = uiState.metaPorcentaje,
                    onDiaClick = { viewModel.seleccionarDia(it) }
                )
            }

            // 5. Detalle del Día Seleccionado
            item {
                uiState.diaSeleccionado?.let { dia ->
                    DetalleDiaSeleccionadoCard(dia = dia)
                }
            }

            // 6. Cumplimiento por Categoría Médica (Presión, Diabetes, etc.)
            item {
                CumplimientoCategoriasCard(
                    categorias = uiState.cumplimientoPorCategoria
                )
            }

            // 7. Tarjeta Principal de Exportación de Reporte Médico Oficial en PDF
            item {
                TarjetaExportarPdfMedico(
                    paciente = datosPaciente,
                    isGenerating = uiState.isGeneratingPdf,
                    onExportarPdf = {
                        Toast.makeText(context, "Generando reporte PDF para ${datosPaciente.medicoTratante}...", Toast.LENGTH_SHORT).show()
                        viewModel.exportarPdfMedico(
                            context = context,
                            paciente = datosPaciente,
                            onSuccess = {
                                Toast.makeText(context, "Reporte PDF médico listo para enviar", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onCompartirTexto = {
                        val reporteTexto = viewModel.generarTextoReporteCompartir(patientName)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, reporteTexto)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Compartir reporte con el médico o familia")
                        context.startActivity(shareIntent)
                    }
                )
            }

            // 8. Consejo Médico Geriátrico & Resumen
            item {
                ConsejoMedicoGeriatricoCard(
                    consejo = uiState.consejoGeriátrico,
                    medicamentoConstante = uiState.medicamentoMasConstante,
                    medicamentoAtencion = uiState.medicamentoAtencion,
                    onCompartir = {
                        val reporteTexto = viewModel.generarTextoReporteCompartir(patientName)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, reporteTexto)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Compartir reporte con el médico")
                        context.startActivity(shareIntent)
                    }
                )
            }
        }
    }
}

/**
 * Fila de selector de períodos accesibles (7 días, 14 días, mes).
 */
@Composable
fun SelectorPeriodoRow(
    periodoSeleccionado: PeriodoFiltro,
    onSeleccionar: (PeriodoFiltro) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("period_selector_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PeriodoFiltro.values().forEach { periodo ->
            val isSelected = periodo == periodoSeleccionado
            val containerColor = if (isSelected) MintInteractive else NavySurface
            val textColor = if (isSelected) NavyDark else TextLightBlue
            val borderModifier = if (isSelected) Modifier else Modifier.border(1.dp, NavySurfaceVariant, RoundedCornerShape(12.dp))

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .then(borderModifier)
                    .clickable { onSeleccionar(periodo) }
                    .testTag("filter_period_${periodo.name.lowercase()}"),
                shape = RoundedCornerShape(12.dp),
                color = containerColor
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = periodo.titulo,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta Hero con Indicador Radial / Donut de Cumplimiento General.
 */
@Composable
fun GraficoRadialHeroCard(
    uiState: HistorialCumplimientoUiState
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = uiState.porcentajeGeneralSemana.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "radial_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_radial_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(MintInteractive.copy(alpha = 0.4f), NavySurfaceVariant)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Adherencia General",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Surface(
                    color = if (uiState.porcentajeGeneralSemana >= uiState.metaPorcentaje) MintInteractive.copy(alpha = 0.15f) else StatusPending.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.porcentajeGeneralSemana >= uiState.metaPorcentaje) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (uiState.porcentajeGeneralSemana >= uiState.metaPorcentaje) MintInteractive else StatusPending,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (uiState.porcentajeGeneralSemana >= uiState.metaPorcentaje) "Meta Superada" else "En Progreso",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.porcentajeGeneralSemana >= uiState.metaPorcentaje) MintInteractive else StatusPending
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Gráfico Circular de Anillo / Donut
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(190.dp)
                    .testTag("donut_chart_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 20.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                    val arcSize = Size(diameter, diameter)

                    // 1. Pista de fondo inactiva
                    drawArc(
                        color = Color(0xFF1B3050),
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 2. Arco de progreso activo
                    val sweepProgress = (animatedPercentage / 100f) * 260f
                    val arcColor = when {
                        animatedPercentage >= 80f -> MintInteractive
                        animatedPercentage >= 60f -> StatusPending
                        else -> StatusUrgent
                    }

                    drawArc(
                        color = arcColor,
                        startAngle = 140f,
                        sweepAngle = sweepProgress.coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Texto Central de alto contraste
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${uiState.porcentajeGeneralSemana}%",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite
                    )
                    Text(
                        text = "A TIEMPO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MintInteractive,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Meta: ${uiState.metaPorcentaje}%",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "${uiState.dosisTomadasTotal} de ${uiState.dosisPlanificadasTotal} tomas completadas correctamente",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextLightBlue,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Tres tarjetas métricas de apoyo rápido (Racha, Dosis Tomadas, Omitidas).
 */
@Composable
fun ResumenKpiTresColumnas(
    uiState: HistorialCumplimientoUiState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kpi_summary_row"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // KPI 1: Racha actual
        Card(
            modifier = Modifier
                .weight(1f)
                .testTag("kpi_streak"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFFFF9E00).copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF9E00),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${uiState.rachaDiasConsecutivos} Días",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Racha activa",
                    fontSize = 12.sp,
                    color = TextLightBlue
                )
            }
        }

        // KPI 2: Dosis Tomadas
        Card(
            modifier = Modifier
                .weight(1f)
                .testTag("kpi_taken"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MintInteractive.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MintInteractive,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${uiState.dosisTomadasTotal}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "Tomadas",
                    fontSize = 12.sp,
                    color = TextLightBlue
                )
            }
        }

        // KPI 3: Omitidas / Pendientes
        Card(
            modifier = Modifier
                .weight(1f)
                .testTag("kpi_missed"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (uiState.dosisOmitidasTotal == 0) MintInteractive.copy(alpha = 0.15f) else StatusUrgent.copy(alpha = 0.18f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.dosisOmitidasTotal == 0) Icons.Default.DoneAll else Icons.Default.PriorityHigh,
                        contentDescription = null,
                        tint = if (uiState.dosisOmitidasTotal == 0) MintInteractive else StatusUrgent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${uiState.dosisOmitidasTotal}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.dosisOmitidasTotal == 0) TextWhite else StatusUrgent
                )
                Text(
                    text = "Omitidas",
                    fontSize = 12.sp,
                    color = TextLightBlue
                )
            }
        }
    }
}

/**
 * Gráfico de Barras Semanal interactivo desarrollado en Jetpack Compose Canvas.
 * Permite al adulto mayor tocar cualquier día para consultar sus tomas específicas.
 */
@Composable
fun GraficoBarrasSemanalCard(
    dias: List<DiaCumplimiento>,
    diaSeleccionado: DiaCumplimiento?,
    metaPorcentaje: Int,
    onDiaClick: (DiaCumplimiento) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bar_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cumplimiento por Día",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Toca un día para inspeccionar las dosis",
                        fontSize = 13.sp,
                        color = TextLightBlue
                    )
                }

                // Indicador de meta
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MintInteractive, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Meta: $metaPorcentaje%",
                        fontSize = 12.sp,
                        color = MintInteractive,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Gráfico de Barras con Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .testTag("weekly_bar_chart_canvas")
            ) {
                val diasAMostrar = dias.takeLast(7)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(diasAMostrar) {
                            detectTapGestures { offset ->
                                val slotWidth = size.width / diasAMostrar.size
                                val tappedIndex = (offset.x / slotWidth).toInt().coerceIn(0, diasAMostrar.size - 1)
                                onDiaClick(diasAMostrar[tappedIndex])
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val chartBottom = height - 36.dp.toPx()
                    val chartTop = 22.dp.toPx()
                    val availableChartHeight = chartBottom - chartTop
                    val count = diasAMostrar.size
                    val slotWidth = width / count
                    val barWidth = 26.dp.toPx()

                    // Línea horizontal punteada de Meta Médica (80%)
                    val metaY = chartBottom - (availableChartHeight * (metaPorcentaje / 100f))
                    drawLine(
                        color = MintInteractive.copy(alpha = 0.5f),
                        start = Offset(0f, metaY),
                        end = Offset(width, metaY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )

                    // Línea base inferior del gráfico
                    drawLine(
                        color = Color(0xFF1B3050),
                        start = Offset(0f, chartBottom),
                        end = Offset(width, chartBottom),
                        strokeWidth = 2.dp.toPx()
                    )

                    diasAMostrar.forEachIndexed { index, dia ->
                        val centerX = (index * slotWidth) + (slotWidth / 2)
                        val isSelected = dia.id == diaSeleccionado?.id
                        val pct = dia.porcentaje.coerceIn(0f, 100f)
                        val barHeight = (availableChartHeight * (pct / 100f)).coerceAtLeast(6.dp.toPx())
                        val barTop = chartBottom - barHeight

                        // Color según nivel de adherencia
                        val barColor = when {
                            pct >= 80f -> MintInteractive
                            pct >= 50f -> StatusPending
                            else -> StatusUrgent
                        }

                        // Fondo tenue de la columna si está seleccionada
                        if (isSelected) {
                            drawRoundRect(
                                color = MintInteractive.copy(alpha = 0.12f),
                                topLeft = Offset(centerX - (slotWidth / 2) + 2.dp.toPx(), chartTop),
                                size = Size(slotWidth - 4.dp.toPx(), chartBottom - chartTop + 30.dp.toPx()),
                                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                            )
                        }

                        // Barra principal de cumplimiento con esquinas redondeadas
                        drawRoundRect(
                            color = if (isSelected) barColor else barColor.copy(alpha = 0.85f),
                            topLeft = Offset(centerX - (barWidth / 2), barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )

                        // Si está seleccionado, resaltar con borde blanco suave
                        if (isSelected) {
                            drawRoundRect(
                                color = TextWhite,
                                topLeft = Offset(centerX - (barWidth / 2), barTop),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                // Superposición de texto para etiquetas accesibles encima y debajo de cada barra
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    diasAMostrar.forEach { dia ->
                        val isSelected = dia.id == diaSeleccionado?.id
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onDiaClick(dia) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Porcentaje en la parte superior
                            Text(
                                text = "${dia.porcentaje.toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MintInteractive else TextLightBlue,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            // Etiqueta del día en la parte inferior
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Text(
                                    text = dia.diaNombre,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isSelected) MintInteractive else if (dia.esHoy) TextWhite else TextLightBlue
                                )
                                Text(
                                    text = dia.fechaCorta.take(2),
                                    fontSize = 11.sp,
                                    color = if (isSelected) TextWhite else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detalle del día seleccionado con lista clara de medicamentos tomados o pendientes.
 */
@Composable
fun DetalleDiaSeleccionadoCard(
    dia: DiaCumplimiento
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_day_detail_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(MintInteractive.copy(alpha = 0.5f), NavySurfaceVariant)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MintInteractive,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = dia.fechaCompleta,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                    Text(
                        text = "${dia.dosisTomadas} de ${dia.dosisTotal} medicamentos administrados (${dia.porcentaje.toInt()}%)",
                        fontSize = 13.sp,
                        color = TextLightBlue
                    )
                }

                Surface(
                    color = if (dia.porcentaje >= 80f) MintInteractive.copy(alpha = 0.15f) else StatusUrgent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (dia.porcentaje >= 80f) "Cumplido" else "Incompleto",
                        color = if (dia.porcentaje >= 80f) MintInteractive else StatusUrgent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = NavySurfaceVariant, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            // Lista de tomas registradas en este día
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                dia.detalles.forEach { toma ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NavyDark.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ícono de estado
                        val (icon, tint) = when (toma.estado) {
                            EstadoRegistroToma.A_TIEMPO -> Pair(Icons.Default.CheckCircle, MintInteractive)
                            EstadoRegistroToma.RETRASADA -> Pair(Icons.Default.Schedule, StatusPending)
                            EstadoRegistroToma.OMITIDA -> Pair(Icons.Default.Cancel, StatusUrgent)
                            EstadoRegistroToma.PENDIENTE -> Pair(Icons.Default.HourglassEmpty, StatusPending)
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${toma.nombreMedicamento} (${toma.dosis})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Horario: ${toma.horaProgramada}",
                                    fontSize = 13.sp,
                                    color = TextLightBlue
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = NavySurfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = toma.categoria,
                                        fontSize = 11.sp,
                                        color = TextLightBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Badge de resultado
                        Text(
                            text = toma.estado.etiqueta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = tint
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de Desglose de Cumplimiento por Categoría Médica.
 */
@Composable
fun CumplimientoCategoriasCard(
    categorias: List<CategoriaCumplimiento>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("categories_adherence_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = MintInteractive,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Cumplimiento por Condición",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
            Text(
                text = "Seguimiento específico según tu tratamiento médico",
                fontSize = 13.sp,
                color = TextLightBlue
            )

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                categorias.forEach { item ->
                    val animProgress by animateFloatAsState(
                        targetValue = item.porcentaje / 100f,
                        animationSpec = tween(700),
                        label = "cat_progress"
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.categoria,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "${item.porcentaje}% (${item.dosisTomadas}/${item.dosisTotal})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = item.color
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { animProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = item.color,
                            trackColor = Color(0xFF1B3050)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Consejo Geriátrico y Botón para Enviar Informe al Médico.
 */
@Composable
fun ConsejoMedicoGeriatricoCard(
    consejo: String,
    medicamentoConstante: String,
    medicamentoAtencion: String?,
    onCompartir: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("geriatric_advice_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MintInteractive.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = MintInteractive,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Recomendación para tu Salud",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = consejo,
                fontSize = 15.sp,
                color = TextLightBlue,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(14.dp))

            // Resumen de mejor medicamento y atención
            Surface(
                color = NavyDark,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Más consistente: $medicamentoConstante",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                    }

                    if (medicamentoAtencion != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = StatusPending,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = medicamentoAtencion,
                                fontSize = 13.sp,
                                color = StatusPending
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Botón grande accesible de compartir
            Button(
                onClick = onCompartir,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("share_summary_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintInteractive,
                    contentColor = NavyDark
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Enviar Reporte a Médico / Familia",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Tarjeta accesible y destacada para la exportación y compartición del Reporte Clínico en PDF.
 */
@Composable
fun TarjetaExportarPdfMedico(
    paciente: DatosPacienteInforme,
    isGenerating: Boolean,
    onExportarPdf: () -> Unit,
    onCompartirTexto: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_export_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    MintInteractive.copy(alpha = 0.65f),
                    NavySurfaceVariant
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(MintInteractive.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Icon",
                        tint = MintInteractive,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reporte Médico Oficial en PDF",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Para entregar a ${paciente.medicoTratante}",
                        fontSize = 13.sp,
                        color = TextLightBlue
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Genera un documento PDF estructurado y legible con tu porcentaje de cumplimiento semanal (88%), desglose día a día, registro de dosis y lista completa de medicamentos para tu control médico.",
                fontSize = 14.sp,
                color = TextLightBlue,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(14.dp))

            // Beneficios y secciones del documento oficial
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyDark.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MintInteractive,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Tabla de cumplimiento diario de los últimos 7 días",
                        fontSize = 13.sp,
                        color = TextWhite
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MintInteractive,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Esquema posológico activo, dosis y horarios exactos",
                        fontSize = 13.sp,
                        color = TextWhite
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MintInteractive,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Espacio formal para firma y sello médico profesional",
                        fontSize = 13.sp,
                        color = TextWhite
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Botón principal de exportar PDF
            Button(
                onClick = onExportarPdf,
                enabled = !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("export_pdf_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintInteractive,
                    contentColor = NavyDark,
                    disabledContainerColor = MintInteractive.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = NavyDark,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Generando PDF...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Exportar y Compartir PDF",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Botón secundario para compartir texto rápido
            OutlinedButton(
                onClick = onCompartirTexto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("share_text_quick_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MintInteractive.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    tint = MintInteractive,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Compartir Resumen como Texto",
                    color = MintInteractive,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

