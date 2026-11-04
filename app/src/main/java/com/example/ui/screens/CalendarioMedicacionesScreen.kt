package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.calendar.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CalendarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioMedicacionesScreen(
    calendarViewModel: CalendarViewModel = viewModel(),
    patientName: String = "Sr. Sebastian",
    emergencyPhone: String = "+57 310 456 7890",
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToHistorial: () -> Unit = {},
    onNavigateToPerfil: () -> Unit = {},
    onNavigateToRegistrar: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by calendarViewModel.uiState.collectAsStateWithLifecycle()

    // Manejo de botón Atrás del sistema
    BackHandler {
        onNavigateToDashboard()
    }

    Scaffold(
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Calendario de Medicaciones",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Planificación y tomas de $patientName",
                            color = TextLightBlue,
                            fontSize = 13.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavySurface,
                    titleContentColor = TextWhite
                ),
                actions = {
                    // Botón para ir al día de hoy
                    OutlinedButton(
                        onClick = { calendarViewModel.goToToday() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MintInteractive
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MintInteractive),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("btn_go_to_today")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Ir a hoy",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hoy",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(StatusUrgent)
                            .testTag("btn_emergency_call_calendar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Llamada de emergencia",
                            tint = TextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToRegistrar,
                containerColor = MintInteractive,
                contentColor = NavyDark,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Programar Medicamento",
                        modifier = Modifier.size(26.dp)
                    )
                },
                text = {
                    Text(
                        text = "Programar Medicamento",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("fab_add_medication_calendar")
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
                        Text(text = "Medicamentos", fontSize = 12.sp)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = TextLightBlue,
                        unselectedTextColor = TextLightBlue
                    )
                )

                NavigationBarItem(
                    selected = true,
                    onClick = { /* Ya estamos en el Calendario */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendario"
                        )
                    },
                    label = {
                        Text(
                            text = "Calendario",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                    onClick = onNavigateToHistorial,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Cumplimiento"
                        )
                    },
                    label = {
                        Text(text = "Cumplimiento", fontSize = 12.sp)
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
                        Text(text = "Mi Perfil", fontSize = 12.sp)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Mensaje de feedback temporal
            uiState.feedbackMessage?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MintInteractive, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MintInteractive,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = { calendarViewModel.clearFeedbackMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar mensaje",
                                    tint = TextLightBlue
                                )
                            }
                        }
                    }
                }
            }

            // Encabezado selector de Mes y Año con controles táctiles grandes
            item {
                MonthHeaderCard(
                    monthTitle = uiState.monthTitle,
                    onPreviousMonth = { calendarViewModel.previousMonth() },
                    onNextMonth = { calendarViewModel.nextMonth() }
                )
            }

            // Cuadrícula completa de días del calendario
            item {
                CalendarGridCard(
                    days = uiState.daysGrid,
                    onSelectDay = { year, month, day ->
                        calendarViewModel.selectDate(year, month, day)
                    }
                )
            }

            // Leyenda de colores del calendario
            item {
                CalendarLegendRow()
            }

            // Resumen de la fecha seleccionada
            item {
                SelectedDaySummaryHeader(
                    selectedDateFormatted = uiState.selectedDateFormatted,
                    isSelectedToday = uiState.isSelectedDayToday,
                    isSelectedPast = uiState.isSelectedDayPast,
                    isSelectedFuture = uiState.isSelectedDayFuture,
                    totalDoses = uiState.totalDosesCount,
                    takenDoses = uiState.takenDosesCount,
                    pendingDoses = uiState.pendingDosesCount,
                    adherencePercent = uiState.adherencePercentToday
                )
            }

            // Filtros de dosis (Todas / Pendientes / Completadas)
            item {
                DoseFilterChipsRow(
                    selectedFilter = uiState.activeFilter,
                    onSelectFilter = { calendarViewModel.setFilter(it) },
                    totalCount = uiState.totalDosesCount,
                    pendingCount = uiState.pendingDosesCount,
                    takenCount = uiState.takenDosesCount
                )
            }

            // Lista de medicamentos por jornada (Mañana, Tarde, Noche)
            if (uiState.filteredDoses.isEmpty()) {
                item {
                    EmptyDayDosesCard(
                        isFilterApplied = uiState.activeFilter != CalendarDoseFilter.TODAS,
                        onAddMedication = onNavigateToRegistrar
                    )
                }
            } else {
                TimeOfDayCategory.values().forEach { timeOfDay ->
                    val dosesInTimeOfDay = uiState.dosesByTimeOfDay[timeOfDay] ?: emptyList()
                    if (dosesInTimeOfDay.isNotEmpty()) {
                        item {
                            TimeOfDayHeader(timeOfDay = timeOfDay, count = dosesInTimeOfDay.size)
                        }

                        items(dosesInTimeOfDay, key = { "${it.medicationId}_${it.scheduleTime}" }) { dose ->
                            MedicationDoseCalendarCard(
                                dose = dose,
                                onToggle = {
                                    calendarViewModel.toggleMedicationTaken(dose.medicationId, dose.isTaken)
                                }
                            )
                        }
                    }
                }
            }

            // Resumen de Adherencia Mensual
            uiState.monthlySummary?.let { summary ->
                item {
                    MonthlyAdherenceGlanceCard(summary = summary)
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Espacio para el FAB y NavigationBar
            }
        }
    }
}

/**
 * Cabecera para navegar entre meses con controles táctiles de tamaño accesible.
 */
@Composable
fun MonthHeaderCard(
    monthTitle: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NavySurfaceVariant)
                    .testTag("btn_prev_month")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Mes anterior",
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = monthTitle,
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Toca un día para ver sus tomas",
                    color = TextLightBlue,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NavySurfaceVariant)
                    .testTag("btn_next_month")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Mes siguiente",
                    tint = TextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Cuadrícula de 7 columnas que representa el calendario mensual.
 */
@Composable
fun CalendarGridCard(
    days: List<CalendarDay>,
    onSelectDay: (year: Int, month: Int, day: Int) -> Unit
) {
    val weekDaysLabels = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Nombres de los días de la semana
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekDaysLabels.forEach { label ->
                    Text(
                        text = label,
                        color = if (label == "Sáb" || label == "Dom") MintInteractive else TextLightBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(color = NavySurfaceVariant, thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))

            // Celdas del calendario organizadas en filas de 7 días
            val rows = days.chunked(7)
            rows.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CalendarDayCell(
                                day = day,
                                onClick = { onSelectDay(day.year, day.month, day.dayOfMonth) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Celda individual de día en el calendario interactivo.
 */
@Composable
fun CalendarDayCell(
    day: CalendarDay,
    onClick: () -> Unit
) {
    val isSelected = day.isSelected
    val isToday = day.isToday
    val isCurrentMonth = day.isCurrentMonth

    val cellBackground = when {
        isSelected -> MintInteractive
        isToday -> NavySurfaceVariant
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> NavyDark
        !isCurrentMonth -> TextMuted.copy(alpha = 0.5f)
        isToday -> MintInteractive
        else -> TextWhite
    }

    val textWeight = when {
        isSelected || isToday -> FontWeight.ExtraBold
        else -> FontWeight.Medium
    }

    val borderModifier = when {
        isSelected -> Modifier.border(2.dp, TextWhite, RoundedCornerShape(10.dp))
        isToday -> Modifier.border(1.5.dp, MintInteractive, RoundedCornerShape(10.dp))
        else -> Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(cellBackground)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            color = textColor,
            fontSize = 14.sp,
            fontWeight = textWeight,
            textAlign = TextAlign.Center
        )

        // Indicador de adherencia / estado de medicamentos
        if (day.totalDoses > 0 && isCurrentMonth) {
            Spacer(modifier = Modifier.height(2.dp))
            val dotColor = when (day.status) {
                DayDoseStatus.ALL_TAKEN -> if (isSelected) NavyDark else StatusSuccess
                DayDoseStatus.PARTIAL_PENDING -> if (isSelected) NavyDark else StatusPending
                DayDoseStatus.PENDING -> if (isSelected) NavyDark else StatusPending.copy(alpha = 0.7f)
                DayDoseStatus.EMPTY -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

/**
 * Fila explicativa con la leyenda de colores del calendario.
 */
@Composable
fun CalendarLegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = StatusSuccess, label = "Todas tomadas")
        LegendItem(color = StatusPending, label = "Tomas pendientes")
        LegendItem(color = MintInteractive, isBorder = true, label = "Día de hoy")
    }
}

@Composable
fun LegendItem(color: Color, isBorder: Boolean = false, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isBorder) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, color, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = TextLightBlue,
            fontSize = 11.sp
        )
    }
}

/**
 * Resumen del día seleccionado con fecha completa y contadores.
 */
@Composable
fun SelectedDaySummaryHeader(
    selectedDateFormatted: String,
    isSelectedToday: Boolean,
    isSelectedPast: Boolean,
    isSelectedFuture: Boolean,
    totalDoses: Int,
    takenDoses: Int,
    pendingDoses: Int,
    adherencePercent: Int
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isSelectedToday) "📅 Hoy" else "📅 Día Seleccionado",
                            color = MintInteractive,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSelectedPast) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Histórico",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        } else if (isSelectedFuture) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Programación futura",
                                color = StatusPending,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Text(
                        text = selectedDateFormatted,
                        color = TextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Porcentaje de adherencia
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$adherencePercent%",
                        color = if (adherencePercent == 100) StatusSuccess else StatusPending,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Cumplimiento",
                        color = TextLightBlue,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila de métricas rápidas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    icon = Icons.Default.MedicalServices,
                    label = "Programadas",
                    value = "$totalDoses",
                    tint = TextLightBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.CheckCircle,
                    label = "Tomadas",
                    value = "$takenDoses",
                    tint = StatusSuccess,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.Schedule,
                    label = "Pendientes",
                    value = "$pendingDoses",
                    tint = StatusPending,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(NavySurfaceVariant)
            .padding(vertical = 8.dp, horizontal = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = value,
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    color = TextLightBlue,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Fila de filtros para mostrar tomas: Todas, Pendientes o Completadas.
 */
@Composable
fun DoseFilterChipsRow(
    selectedFilter: CalendarDoseFilter,
    onSelectFilter: (CalendarDoseFilter) -> Unit,
    totalCount: Int,
    pendingCount: Int,
    takenCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CalendarDoseFilter.values().forEach { filter ->
            val count = when (filter) {
                CalendarDoseFilter.TODAS -> totalCount
                CalendarDoseFilter.PENDIENTES -> pendingCount
                CalendarDoseFilter.TOMADAS -> takenCount
            }
            val isSelected = selectedFilter == filter

            FilterChip(
                selected = isSelected,
                onClick = { onSelectFilter(filter) },
                label = {
                    Text(
                        text = "${filter.label} ($count)",
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MintInteractive,
                    selectedLabelColor = NavyDark,
                    containerColor = NavySurface,
                    labelColor = TextLightBlue
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (isSelected) MintInteractive else NavySurfaceVariant,
                    enabled = true,
                    selected = isSelected
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            )
        }
    }
}

/**
 * Encabezado de la Jornada del día (Mañana, Tarde, Noche).
 */
@Composable
fun TimeOfDayHeader(
    timeOfDay: TimeOfDayCategory,
    count: Int
) {
    val icon = when (timeOfDay) {
        TimeOfDayCategory.MANANA -> Icons.Default.WbSunny
        TimeOfDayCategory.TARDE -> Icons.Default.Brightness5
        TimeOfDayCategory.NOCHE -> Icons.Default.NightlightRound
    }

    val iconColor = when (timeOfDay) {
        TimeOfDayCategory.MANANA -> Color(0xFFFFD166)
        TimeOfDayCategory.TARDE -> Color(0xFFF77F00)
        TimeOfDayCategory.NOCHE -> Color(0xFF9D4EDD)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${timeOfDay.label} (${timeOfDay.rangeLabel})",
            color = TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "$count ${if (count == 1) "toma" else "tomas"}",
            color = TextLightBlue,
            fontSize = 12.sp
        )
    }
}

/**
 * Tarjeta individual de una toma de medicamento en el calendario.
 */
@Composable
fun MedicationDoseCalendarCard(
    dose: CalendarMedicationDose,
    onToggle: () -> Unit
) {
    val isTaken = dose.isTaken

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isTaken) NavySurfaceVariant.copy(alpha = 0.85f) else NavySurface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isTaken) 1.dp else 1.5.dp,
                color = if (isTaken) StatusSuccess.copy(alpha = 0.5f) else MintInteractive,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Fila superior: Nombre, Categoría y Hora
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dose.name,
                        color = TextWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Dosis: ${dose.dosage}",
                        color = MintInteractive,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Hora programada
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavyDark)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isTaken) StatusSuccess else StatusPending,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dose.scheduleTime,
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Indicaciones y aviso de alimentos
            if (dose.instructions.isNotBlank() || dose.requiresFood) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavySurfaceVariant.copy(alpha = 0.5f))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (dose.requiresFood) Icons.Default.Restaurant else Icons.Default.Info,
                        contentDescription = null,
                        tint = TextLightBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            dose.instructions.isNotBlank() -> dose.instructions
                            dose.requiresFood -> "Tomar con alimentos o después de comer."
                            else -> "Tomar con un vaso de agua."
                        },
                        color = TextLightBlue,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            HorizontalDivider(color = NavySurfaceVariant, thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // Estado y botón táctil grande de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Etiqueta de estado
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (isTaken) StatusSuccess else StatusPending,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTaken) "Completada ✓" else "Pendiente de tomar",
                        color = if (isTaken) StatusSuccess else StatusPending,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Botón interactivo accesible (>= 48dp de área táctil)
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTaken) NavySurfaceVariant else MintInteractive,
                        contentColor = if (isTaken) TextWhite else NavyDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("btn_toggle_dose_${dose.medicationId}")
                ) {
                    Text(
                        text = if (isTaken) "Desmarcar" else "Marcar como tomada",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Estado vacío cuando no hay medicamentos que coincidan con la fecha o filtro.
 */
@Composable
fun EmptyDayDosesCard(
    isFilterApplied: Boolean,
    onAddMedication: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EventAvailable,
                contentDescription = null,
                tint = MintInteractive,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isFilterApplied) "No hay dosis con este filtro" else "Sin tomas para este día",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isFilterApplied)
                    "Prueba cambiando el filtro a 'Todas' para ver todas las dosis del día."
                else
                    "Puedes agregar un nuevo medicamento o dosis recurrente para mantener tu tratamiento al día.",
                color = TextLightBlue,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddMedication,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintInteractive,
                    contentColor = NavyDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("btn_empty_add_medication")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Agregar Medicamento", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

/**
 * Tarjeta de vistazo mensual con resumen de adherencia y racha de días.
 */
@Composable
fun MonthlyAdherenceGlanceCard(summary: MonthlyAdherenceSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD166),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Resumen de ${summary.monthName} ${summary.year}",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MintInteractive.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${summary.percentage}% Cumplido",
                        color = MintInteractive,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de progreso de adherencia
            LinearProgressIndicator(
                progress = { summary.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MintInteractive,
                trackColor = NavySurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🔥 Racha consecutiva: ${summary.currentStreakDays} días",
                    color = TextLightBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Días cumplidos: ${summary.daysWithFullAdherence}",
                    color = TextLightBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
