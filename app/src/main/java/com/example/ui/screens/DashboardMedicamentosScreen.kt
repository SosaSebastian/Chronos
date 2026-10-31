package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardMedicamentosUiState
import com.example.ui.viewmodel.DashboardMedicamentosViewModel
import com.example.ui.viewmodel.FiltroEstadoToma
import com.example.ui.viewmodel.MedicamentoUiModel

/**
 * Pantalla principal `DashboardMedicamentosScreen.kt`
 *
 * Muestra una lista filtrable de los medicamentos registrados mediante [LazyColumn]
 * y un diseño tipo tarjetas ([Card]) optimizado para adultos mayores, destacando
 * el horario de la próxima toma, dosis, indicaciones e interactivos de alto contraste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardMedicamentosScreen(
    viewModel: DashboardMedicamentosViewModel = viewModel(),
    patientName: String = "Sr. Sebastian",
    emergencyPhone: String = "+57 310 456 7890",
    onNavigateToRegistrar: () -> Unit = {},
    onNavigateToAsistenteIa: () -> Unit = {},
    onNavigateToPerfil: () -> Unit = {},
    onNavigateToHistorial: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var medicamentoParaEliminar by remember { mutableStateOf<MedicamentoUiModel?>(null) }

    // Diálogo accesible de confirmación para eliminar
    medicamentoParaEliminar?.let { med ->
        AlertDialog(
            onDismissRequest = { medicamentoParaEliminar = null },
            containerColor = NavySurface,
            titleContentColor = TextWhite,
            textContentColor = TextLightBlue,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = StatusUrgent,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "¿Eliminar medicamento?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar ${med.nombre} (${med.dosis}) de tus recordatorios diarios?",
                    fontSize = 16.sp,
                    color = TextLightBlue
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMedication(med)
                        medicamentoParaEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusUrgent,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("confirm_delete_btn")
                ) {
                    Text("Sí, eliminar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { medicamentoParaEliminar = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NavySurfaceVariant)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("cancel_delete_btn")
                ) {
                    Text("Cancelar", fontSize = 15.sp)
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_medicamentos_screen"),
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavySurface,
                    titleContentColor = TextWhite
                ),
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Healty Chronos",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MintInteractive.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Dosis & Horarios",
                                    color = MintInteractive,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Hola, $patientName",
                            fontSize = 14.sp,
                            color = TextLightBlue
                        )
                    }
                },
                actions = {
                    // Botón de llamada rápida de emergencia para el adulto mayor
                    IconButton(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$emergencyPhone"))
                            context.startActivity(dialIntent)
                        },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(StatusUrgent)
                            .testTag("dashboard_emergency_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Llamar contacto de emergencia",
                            tint = TextWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Botón para ver y exportar reporte de adherencia en PDF
                    IconButton(
                        onClick = onNavigateToHistorial,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dashboard_historial_topbar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Ver cumplimiento y exportar PDF",
                            tint = MintInteractive
                        )
                    }

                    // Botón de recarga manual
                    IconButton(
                        onClick = { viewModel.cargarMedicamentos() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dashboard_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar lista",
                            tint = TextLightBlue
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
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                },
                text = {
                    Text(
                        text = "Registrar Medicamento",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("add_medication_fab")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = NavySurface,
                contentColor = TextWhite,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Ya estamos en el dashboard */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = "Medicamentos"
                        )
                    },
                    label = {
                        Text(
                            text = "Medicamentos",
                            fontSize = 13.sp,
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
                            contentDescription = "Historial y Cumplimiento"
                        )
                    },
                    label = {
                        Text(text = "Cumplimiento", fontSize = 13.sp)
                    },
                    colors = NavigationBarItemDefaults.colors(
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Mensaje de feedback temporal (al marcar dosis o eliminar)
            AnimatedVisibility(
                visible = uiState.feedbackMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("feedback_banner"),
                    colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MintInteractive)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MintInteractive,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = uiState.feedbackMessage ?: "",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = { viewModel.clearFeedbackMessage() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar mensaje",
                                tint = TextLightBlue
                            )
                        }
                    }
                }
            }

            // 1. Tarjeta Resumen de Progreso Diario
            TarjetaProgresoDiario(
                totalMedicamentos = uiState.totalMedicamentos,
                tomados = uiState.tomadosHoy,
                pendientes = uiState.pendientesHoy,
                patientName = patientName,
                onNavigateToHistorial = onNavigateToHistorial
            )

            // 2. Buscador de Medicamentos en tiempo real
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("search_medication_input"),
                placeholder = {
                    Text(
                        text = "Buscar medicina (ej: Losartán, Metformina)...",
                        color = TextMuted,
                        fontSize = 15.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MintInteractive
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar búsqueda",
                                tint = TextLightBlue
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NavySurface,
                    unfocusedContainerColor = NavySurface,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = MintInteractive,
                    unfocusedBorderColor = NavySurfaceVariant,
                    cursorColor = MintInteractive
                )
            )

            // 3. Fila de Filtro de Categorías
            FilaFiltrosCategoria(
                categorias = uiState.categoriasDisponibles,
                categoriaSeleccionada = uiState.categoriaSeleccionada,
                onSelectCategoria = { viewModel.selectCategoria(it) }
            )

            // 4. Fila de Filtro por Estado (Todos, Pendientes, Tomados)
            FilaFiltroEstado(
                filtroActual = uiState.filtroEstado,
                totalCount = uiState.totalMedicamentos,
                pendientesCount = uiState.pendientesHoy,
                tomadosCount = uiState.tomadosHoy,
                onSelectFiltro = { viewModel.selectFiltroEstado(it) }
            )

            Spacer(Modifier.height(6.dp))

            // 5. Lista de Medicamentos (LazyColumn) con Tarjetas y Horario de Próxima Toma
            if (uiState.isLoading && uiState.medicamentosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MintInteractive)
                }
            } else if (uiState.medicamentosFiltrados.isEmpty()) {
                // Estado vacío amigable
                EstadoVacioMedicamentos(
                    searchQuery = uiState.searchQuery,
                    categoriaSeleccionada = uiState.categoriaSeleccionada,
                    onLimpiarFiltros = {
                        viewModel.setSearchQuery("")
                        viewModel.selectCategoria("Todas")
                        viewModel.selectFiltroEstado(FiltroEstadoToma.TODOS)
                    },
                    onRegistrarMedicamento = onNavigateToRegistrar
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("lista_medicamentos_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp)
                ) {
                    items(
                        items = uiState.medicamentosFiltrados,
                        key = { it.id }
                    ) { med ->
                        MedicamentoCard(
                            medicamento = med,
                            onToggleTomado = { viewModel.toggleMedicationTaken(med) },
                            onEliminar = { medicamentoParaEliminar = med }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de progreso diario que motiva al adulto mayor y le da claridad sobre sus dosis.
 */
@Composable
fun TarjetaProgresoDiario(
    totalMedicamentos: Int,
    tomados: Int,
    pendientes: Int,
    patientName: String,
    onNavigateToHistorial: () -> Unit = {}
) {
    val progresoFraction = if (totalMedicamentos > 0) tomados.toFloat() / totalMedicamentos else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("tarjeta_progreso_diario"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(NavySurfaceVariant)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Control de Hoy",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Surface(
                    color = if (tomados == totalMedicamentos && totalMedicamentos > 0) {
                        StatusSuccess.copy(alpha = 0.2f)
                    } else {
                        NavySurfaceVariant
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$tomados de $totalMedicamentos tomadas",
                        color = if (tomados == totalMedicamentos && totalMedicamentos > 0) {
                            MintInteractive
                        } else {
                            TextLightBlue
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progresoFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MintInteractive,
                trackColor = NavySurfaceVariant
            )

            if (tomados == totalMedicamentos && totalMedicamentos > 0) {
                Text(
                    text = "🎉 ¡Felicidades! Has cumplido con todas tus tomas programadas.",
                    color = MintInteractive,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (pendientes > 0) {
                Text(
                    text = "Tienes $pendientes ${if (pendientes == 1) "toma pendiente" else "tomas pendientes"} para el día de hoy.",
                    color = StatusPending,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Botón interactivo directo para ver el historial y cumplimiento semanal
            OutlinedButton(
                onClick = onNavigateToHistorial,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("dashboard_view_history_btn"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MintInteractive,
                    containerColor = NavySurfaceElevated.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintInteractive.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MintInteractive
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Ver Gráficos y Cumplimiento Semanal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintInteractive
                )
            }
        }
    }
}

/**
 * Fila horizontal desplazable de filtros por categoría.
 */
@Composable
fun FilaFiltrosCategoria(
    categorias: List<String>,
    categoriaSeleccionada: String,
    onSelectCategoria: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categorias.forEach { cat ->
            val isSelected = categoriaSeleccionada.equals(cat, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategoria(cat) },
                label = {
                    Text(
                        text = cat,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = obtenerIconoCategoria(cat),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MintInteractive,
                    selectedLabelColor = NavyDark,
                    selectedLeadingIconColor = NavyDark,
                    containerColor = NavySurface,
                    labelColor = TextWhite,
                    iconColor = TextLightBlue
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) MintInteractive else NavySurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("filtro_categoria_${cat.lowercase()}")
            )
        }
    }
}

/**
 * Fila de filtros por estado (Todos, Pendientes, Tomados).
 */
@Composable
fun FilaFiltroEstado(
    filtroActual: FiltroEstadoToma,
    totalCount: Int,
    pendientesCount: Int,
    tomadosCount: Int,
    onSelectFiltro: (FiltroEstadoToma) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FiltroEstadoToma.values().forEach { filtro ->
            val isSelected = filtroActual == filtro
            val count = when (filtro) {
                FiltroEstadoToma.TODOS -> totalCount
                FiltroEstadoToma.PENDIENTES -> pendientesCount
                FiltroEstadoToma.TOMADOS -> tomadosCount
            }

            FilterChip(
                selected = isSelected,
                onClick = { onSelectFiltro(filtro) },
                label = {
                    Text(
                        text = "${filtro.label} ($count)",
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NavySurfaceElevated,
                    selectedLabelColor = MintInteractive,
                    containerColor = NavySurface,
                    labelColor = TextLightBlue
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) MintInteractive else NavySurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("filtro_estado_${filtro.name.lowercase()}")
            )
        }
    }
}

/**
 * Componente principal: Tarjeta de Medicamento ([Card]) con el horario de la próxima toma
 * especialmente destacado para garantizar visibilidad y evitar olvidos.
 */
@Composable
fun MedicamentoCard(
    medicamento: MedicamentoUiModel,
    onToggleTomado: () -> Unit,
    onEliminar: () -> Unit
) {
    val cardBorderColor by animateColorAsState(
        targetValue = if (medicamento.isTakenToday) StatusSuccess else NavySurfaceVariant,
        label = "cardBorderColorAnim"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, cardBorderColor, RoundedCornerShape(20.dp))
            .testTag("medicamento_card_${medicamento.id}"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Fila 1: Encabezado del medicamento (Nombre, Dosis, Categoría y Botón Eliminar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = medicamento.nombre,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Dosis badge
                        Surface(
                            color = MintInteractive.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = medicamento.dosis,
                                color = MintInteractive,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Categoría chip pequeño
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = obtenerIconoCategoria(medicamento.categoria),
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = medicamento.categoria,
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Botón discreto para eliminar
                IconButton(
                    onClick = onEliminar,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("btn_eliminar_${medicamento.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar medicamento",
                        tint = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // =========================================================================
            // SECCIÓN DESTACADA: HORARIO DE LA PRÓXIMA TOMA
            // =========================================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("proxima_toma_banner_${medicamento.id}"),
                color = if (medicamento.isTakenToday) {
                    NavySurfaceElevated
                } else {
                    NavyDark
                },
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (medicamento.isTakenToday) StatusSuccess.copy(alpha = 0.4f) else MintInteractive.copy(alpha = 0.4f)
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ícono circular de reloj grande
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (medicamento.isTakenToday) StatusSuccess.copy(alpha = 0.2f) else MintInteractive.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (medicamento.isTakenToday) Icons.Default.CheckCircle else Icons.Default.AccessTimeFilled,
                            contentDescription = null,
                            tint = if (medicamento.isTakenToday) StatusSuccess else MintInteractive,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (medicamento.isTakenToday) "Dosis Completada" else "Horario de Próxima Toma",
                            fontSize = 12.sp,
                            color = TextLightBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = medicamento.horaToma,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (medicamento.isTakenToday) StatusSuccess else MintInteractive
                        )
                        Text(
                            text = medicamento.proximaTomaTexto,
                            fontSize = 13.sp,
                            color = if (medicamento.isTakenToday) TextLightBlue else StatusPending,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Indicaciones especiales si existen
            if (medicamento.indicaciones.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextLightBlue,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = medicamento.indicaciones,
                        color = TextLightBlue,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Fila de Acción Principal: Botón grande accesible "Marcar como Tomado"
            Button(
                onClick = onToggleTomado,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (medicamento.isTakenToday) StatusSuccess else MintInteractive,
                    contentColor = NavyDark
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("toggle_tomado_btn_${medicamento.id}")
            ) {
                Icon(
                    imageVector = if (medicamento.isTakenToday) Icons.Default.Check else Icons.Default.CheckCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (medicamento.isTakenToday) "Tomado ✓ (Toca para desmarcar)" else "Marcar como Tomado",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Vista mostrada cuando no hay medicamentos o los filtros no devuelven resultados.
 */
@Composable
fun EstadoVacioMedicamentos(
    searchQuery: String,
    categoriaSeleccionada: String,
    onLimpiarFiltros: () -> Unit,
    onRegistrarMedicamento: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .testTag("estado_vacio_container"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(NavySurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (searchQuery.isNotBlank() || categoriaSeleccionada != "Todas") {
                        Icons.Default.SearchOff
                    } else {
                        Icons.Default.Medication
                    },
                    contentDescription = null,
                    tint = MintInteractive,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (searchQuery.isNotBlank() || categoriaSeleccionada != "Todas") {
                    "No encontramos medicamentos con estos filtros"
                } else {
                    "Aún no tienes medicamentos programados"
                },
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (searchQuery.isNotBlank() || categoriaSeleccionada != "Todas") {
                    "Intenta buscar con otro nombre o selecciona la categoría 'Todas'."
                } else {
                    "Registra tus dosis y horarios para que Healty Chronos te avise puntualmente."
                },
                fontSize = 15.sp,
                color = TextLightBlue,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            if (searchQuery.isNotBlank() || categoriaSeleccionada != "Todas") {
                OutlinedButton(
                    onClick = onLimpiarFiltros,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MintInteractive),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MintInteractive)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_limpiar_filtros")
                ) {
                    Icon(imageVector = Icons.Default.FilterAltOff, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Restablecer Filtros", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onRegistrarMedicamento,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintInteractive,
                        contentColor = NavyDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("btn_primer_medicamento")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Registrar Primer Medicamento", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Devuelve un icono representativo de Material Symbols para cada categoría médica.
 */
private fun obtenerIconoCategoria(categoria: String): ImageVector {
    return when (categoria.trim().lowercase()) {
        "presión", "presion" -> Icons.Default.Favorite
        "diabetes" -> Icons.Default.Bloodtype
        "corazón", "corazon" -> Icons.Default.HealthAndSafety
        "vitaminas" -> Icons.Default.Spa
        "todas" -> Icons.Default.ListAlt
        else -> Icons.Default.Medication
    }
}
