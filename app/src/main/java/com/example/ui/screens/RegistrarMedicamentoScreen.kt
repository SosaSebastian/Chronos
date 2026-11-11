package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import com.example.ui.viewmodel.RegistrarMedicamentoUiState
import com.example.ui.viewmodel.RegistrarMedicamentoViewModel
import com.example.util.VoiceDictationButton

/**
 * Formulario de Registro de Dosis, Horario y Frecuencia Semanal (`RegistrarMedicamentoScreen.kt`).
 * Permite:
 * 1. Selección de horarios frecuentes (chips rápidos) o selección de un horario específico (TimePicker).
 * 2. Selección de frecuencia: "Todos los días" o días específicos de la semana (L, M, X, J, V, S, D).
 * 3. Persistencia en la base de datos local (Room) y en la nube (Cloud Firestore).
 * 4. Sincronización automática con el Calendario de Medicaciones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarMedicamentoScreen(
    viewModel: RegistrarMedicamentoViewModel = viewModel(),
    onMedicamentoGuardado: (String) -> Unit = {},
    onSyncLocal: ((
        nombre: String,
        dosis: String,
        horaToma: String,
        categoria: String,
        indicaciones: String,
        diasSemana: String,
        esTodosLosDias: Boolean
    ) -> Unit)? = null,
    onVolver: () -> Unit = {}
) {
    BackHandler { onVolver() }

    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isLoading = uiState is RegistrarMedicamentoUiState.Loading
    val idEdicion by viewModel.medicamentoEnEdicionId.collectAsStateWithLifecycle()
    val enEdicion = idEdicion != null

    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Notificar éxito al guardar y sincronizar con Room y Calendario
    LaunchedEffect(uiState) {
        if (uiState is RegistrarMedicamentoUiState.Success) {
            val medId = (uiState as RegistrarMedicamentoUiState.Success).medicamentoId
            onSyncLocal?.invoke(
                formState.nombre,
                formState.dosis,
                formState.horaToma,
                formState.categoria,
                formState.indicaciones,
                formState.diasSemanaFormateados,
                formState.isEveryday || formState.selectedDays.size == 7
            )
            onMedicamentoGuardado(medId)
        }
    }

    Scaffold(
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavySurface,
                    titleContentColor = TextWhite
                ),
                title = {
                    Text(
                        text = if (enEdicion) "Editar Medicamento" else "Registrar Dosis y Horario",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("btn_volver_medicamento")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MintInteractive
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("pantalla_registrar_medicamento"),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Encabezado
            Text(
                text = if (enEdicion) "Editar Medicamento" else "Registrar Dosis y Horario",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = if (enEdicion) {
                    "Cambia los datos, horarios o días; el recordatorio y calendario se sincronizan automáticamente."
                } else {
                    "Configura horarios frecuentes o específicos y los días de toma para sincronizar tu calendario."
                },
                fontSize = 14.sp,
                color = TextLightBlue,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // Banner de Error si existe
            val errorMessage = when (uiState) {
                is RegistrarMedicamentoUiState.Error -> (uiState as RegistrarMedicamentoUiState.Error).message
                else -> formState.validationError
            }
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusUrgent.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, StatusUrgent, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusUrgent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage,
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // =========================================================================
            // 1. CAMPO NOMBRE DEL MEDICAMENTO + CHIPS FRECUENTES + BOTÓN DE VOZ
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nombre del Medicamento *",
                    color = MintInteractive,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                VoiceDictationButton(
                    label = "Dictar",
                    promptText = "Di el nombre de tu medicamento...",
                    testTag = "btn_dictar_nombre_med",
                    containerColor = NavySurfaceVariant,
                    contentColor = MintInteractive,
                    onSpeechResult = { recognizedText ->
                        viewModel.updateNombre(recognizedText)
                    }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = formState.nombre,
                onValueChange = { viewModel.updateNombre(it) },
                enabled = !isLoading,
                placeholder = { Text("Ej: Losartán, Metformina...", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Medication, contentDescription = null, tint = MintInteractive)
                },
                singleLine = true,
                colors = campoMedColores(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_medicamento_nombre")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fila de chips rápidos frecuentes
            Text(
                text = "Frecuentes:",
                color = TextLightBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.medicamentosFrecuentes.forEach { medFrecuente ->
                    val isSelected = formState.nombre.equals(medFrecuente, ignoreCase = true)
                    SuggestionChip(
                        onClick = {
                            if (!isLoading) viewModel.selectMedicamentoFrecuente(medFrecuente)
                        },
                        label = {
                            Text(
                                text = medFrecuente,
                                color = if (isSelected) NavyDark else TextWhite,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) MintInteractive else NavySurfaceVariant
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = if (isSelected) MintInteractive else NavySurfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("chip_med_$medFrecuente")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // 2. CAMPO DOSIS / CONCENTRACIÓN
            // =========================================================================
            Text(
                text = "Dosis / Concentración *",
                color = MintInteractive,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = formState.dosis,
                onValueChange = { viewModel.updateDosis(it) },
                enabled = !isLoading,
                placeholder = { Text("50 mg", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Science, contentDescription = null, tint = MintInteractive)
                },
                singleLine = true,
                colors = campoMedColores(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_medicamento_dosis")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // 3. SELECTOR DE HORARIO: FRECUENTES O ESPECÍFICO
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hora de la Toma *",
                    color = MintInteractive,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = { showTimePickerDialog = true },
                    modifier = Modifier.testTag("btn_seleccionar_hora_especifica")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = null,
                        tint = MintInteractive,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hora Específica",
                        color = MintInteractive,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Tarjeta de hora activa destacada
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MintInteractive, RoundedCornerShape(14.dp))
                    .clickable { showTimePickerDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MintInteractive.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Reloj",
                                tint = MintInteractive,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Hora programada",
                                color = TextLightBlue,
                                fontSize = 12.sp
                            )
                            Text(
                                text = formState.horaToma,
                                color = TextWhite,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { showTimePickerDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MintInteractive.copy(alpha = 0.2f),
                            contentColor = MintInteractive
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Cambiar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fila de chips de horarios frecuentes
            Text(
                text = "Horarios Frecuentes:",
                color = TextLightBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.horariosFrecuentes.forEach { horaChip ->
                    val isSelected = formState.horaToma == horaChip
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isLoading) viewModel.selectHoraToma(horaChip)
                        },
                        label = {
                            Text(
                                text = horaChip,
                                color = if (isSelected) NavyDark else TextWhite,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NavyDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MintInteractive,
                            containerColor = NavySurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) MintInteractive else NavySurfaceVariant
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.testTag("chip_hora_$horaChip")
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // =========================================================================
            // 4. SELECTOR DE DÍAS DE LA SEMANA: TODOS LOS DÍAS O DÍAS ESPECÍFICOS
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Días de la Toma *",
                    color = MintInteractive,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Sincroniza con Calendario",
                    color = TextLightBlue,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Selector principal de modalidad: Todos los días vs Días específicos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = formState.isEveryday,
                    onClick = {
                        if (!isLoading) viewModel.setEsTodosLosDias(true)
                    },
                    label = {
                        Text(
                            text = "Todos los días",
                            color = if (formState.isEveryday) NavyDark else TextWhite,
                            fontWeight = if (formState.isEveryday) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (formState.isEveryday) NavyDark else MintInteractive,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MintInteractive,
                        containerColor = NavySurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = formState.isEveryday,
                        borderColor = if (formState.isEveryday) MintInteractive else NavySurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("chip_frecuencia_todos_los_dias")
                )

                FilterChip(
                    selected = !formState.isEveryday,
                    onClick = {
                        if (!isLoading) viewModel.setEsTodosLosDias(false)
                    },
                    label = {
                        Text(
                            text = "Días específicos",
                            color = if (!formState.isEveryday) NavyDark else TextWhite,
                            fontWeight = if (!formState.isEveryday) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (!formState.isEveryday) NavyDark else MintInteractive,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MintInteractive,
                        containerColor = NavySurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = !formState.isEveryday,
                        borderColor = if (!formState.isEveryday) MintInteractive else NavySurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("chip_frecuencia_dias_especificos")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grilla de los 7 días de la semana (L, M, X, J, V, S, D)
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (!formState.isEveryday) MintInteractive.copy(alpha = 0.5f) else NavySurfaceVariant,
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (formState.isEveryday) {
                            "Frecuencia diaria activa: Todos los días (Lunes a Domingo)"
                        } else {
                            "Toca los días que debes tomar este medicamento:"
                        },
                        color = TextLightBlue,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // 7 botones circulares/redondeados para Lunes a Domingo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val diasAbreviados = listOf(
                            "Lunes" to "L",
                            "Martes" to "M",
                            "Miércoles" to "X",
                            "Jueves" to "J",
                            "Viernes" to "V",
                            "Sábado" to "S",
                            "Domingo" to "D"
                        )

                        diasAbreviados.forEach { (nombreLargo, inicial) ->
                            val isSelected = formState.isEveryday || formState.selectedDays.contains(nombreLargo)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MintInteractive else NavySurfaceVariant)
                                    .clickable {
                                        if (!isLoading) viewModel.toggleDiaSemana(nombreLargo)
                                    }
                                    .testTag("chip_dia_$nombreLargo"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = inicial,
                                    color = if (isSelected) NavyDark else TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Atajos de selección rápida y resumen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { viewModel.selectDiasLaborables() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("btn_dias_laborables")
                            ) {
                                Text("L a V", fontSize = 12.sp, color = MintInteractive)
                            }
                            TextButton(
                                onClick = { viewModel.selectFinDeSemana() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("btn_dias_fin_de_semana")
                            ) {
                                Text("Fin de Semana", fontSize = 12.sp, color = MintInteractive)
                            }
                        }

                        Text(
                            text = if (formState.isEveryday || formState.selectedDays.size == 7) {
                                "Diario (7 días)"
                            } else {
                                "${formState.selectedDays.size} días/semana"
                            },
                            color = TextLightBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!formState.isEveryday && formState.selectedDays.isNotEmpty()) {
                        Text(
                            text = "Días: " + formState.selectedDays.joinToString(", "),
                            color = MintInteractive,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // 5. SELECTOR DE CATEGORÍA MÉDICA
            // =========================================================================
            Text(
                text = "Categoría Médica *",
                color = MintInteractive,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.categoriasMedicas.forEach { cat ->
                    val isSelected = formState.categoria == cat
                    val iconVector = when (cat) {
                        "Presión" -> Icons.Default.Speed
                        "Diabetes" -> Icons.Default.Bloodtype
                        "Corazón" -> Icons.Default.Favorite
                        "Vitaminas" -> Icons.Default.Eco
                        else -> Icons.Default.Category
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isLoading) viewModel.selectCategoria(cat)
                        },
                        label = {
                            Text(
                                text = cat,
                                color = if (isSelected) NavyDark else TextWhite,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = if (isSelected) NavyDark else MintInteractive,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MintInteractive,
                            containerColor = NavySurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) MintInteractive else NavySurfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("chip_cat_$cat")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // 6. INDICACIONES ESPECIALES (CAMPO MULTILÍNEA + GRABACIÓN DE VOZ ANDROID)
            // =========================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Indicaciones Especiales",
                    color = MintInteractive,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                VoiceDictationButton(
                    label = "Dictar por voz",
                    promptText = "Di las indicaciones médicas para tu medicamento...",
                    testTag = "btn_dictar_indicaciones",
                    containerColor = MintInteractive,
                    contentColor = NavyDark,
                    onSpeechResult = { recognizedText ->
                        val current = formState.indicaciones.trim()
                        val updated = if (current.isBlank()) recognizedText else "$current. $recognizedText"
                        viewModel.updateIndicaciones(updated)
                    }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = formState.indicaciones,
                onValueChange = { viewModel.updateIndicaciones(it) },
                enabled = !isLoading,
                placeholder = {
                    Text(
                        text = "Ej: Tomar con un vaso con agua después del desayuno...",
                        color = TextMuted
                    )
                },
                minLines = 3,
                maxLines = 5,
                colors = campoMedColores(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_medicamento_indicaciones")
            )

            Spacer(modifier = Modifier.height(28.dp))

            // =========================================================================
            // ACCIÓN: BOTÓN GUARDAR MEDICAMENTO EN FIRESTORE Y LOCAL
            // =========================================================================
            Button(
                onClick = {
                    viewModel.guardarMedicamento(onSuccess = onMedicamentoGuardado)
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintInteractive,
                    contentColor = NavyDark,
                    disabledContainerColor = NavySurfaceVariant,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_guardar_medicamento")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = NavyDark,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Guardando...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = NavyDark
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (enEdicion) "Guardar Cambios" else "Guardar Medicamento",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    // Diálogo accesible para seleccionar horario específico
    if (showTimePickerDialog) {
        TimePickerCustomDialog(
            initialTime = formState.horaToma,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { horaSeleccionada ->
                viewModel.setHoraEspecifica(horaSeleccionada)
                showTimePickerDialog = false
            }
        )
    }
}

/**
 * Diálogo interactivo accesible para seleccionar una hora específica (horas 1-12, minutos 0-55 y AM/PM).
 */
@Composable
fun TimePickerCustomDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Parseo inicial
    val upper = initialTime.uppercase().trim()
    val isInitPm = upper.contains("PM")
    val digits = upper.filter { it.isDigit() || it == ':' }
    val initialHour = digits.substringBefore(":").toIntOrNull()?.coerceIn(1, 12) ?: 8
    val initialMinute = digits.substringAfter(":", "00").take(2).toIntOrNull()?.coerceIn(0, 59) ?: 0

    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf((initialMinute / 5) * 5) }
    var isPm by remember { mutableStateOf(isInitPm) }

    val formattedPreview = String.format("%02d:%02d %s", selectedHour, selectedMinute, if (isPm) "PM" else "AM")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, MintInteractive, RoundedCornerShape(20.dp))
                .testTag("dialog_selector_hora_especifica")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MintInteractive, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Seleccionar Horario Específico",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Previsualización destacada
                Surface(
                    color = NavyDark,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MintInteractive.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formattedPreview,
                        color = MintInteractive,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Selector de Hora (1 a 12)
                Text(
                    text = "Hora (1 a 12):",
                    color = TextLightBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            selectedHour = if (selectedHour <= 1) 12 else selectedHour - 1
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NavySurfaceVariant)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Menos una hora", tint = TextWhite)
                    }

                    Text(
                        text = String.format("%02d", selectedHour),
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            selectedHour = if (selectedHour >= 12) 1 else selectedHour + 1
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NavySurfaceVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Más una hora", tint = TextWhite)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de Minutos (00 a 55 en saltos de 5)
                Text(
                    text = "Minutos:",
                    color = TextLightBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            selectedMinute = if (selectedMinute <= 0) 55 else selectedMinute - 5
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NavySurfaceVariant)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Menos cinco minutos", tint = TextWhite)
                    }

                    Text(
                        text = String.format("%02d", selectedMinute),
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            selectedMinute = if (selectedMinute >= 55) 0 else selectedMinute + 5
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NavySurfaceVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Más cinco minutos", tint = TextWhite)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selector AM / PM
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !isPm,
                        onClick = { isPm = false },
                        label = {
                            Text(
                                text = "AM (Mañana)",
                                color = if (!isPm) NavyDark else TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MintInteractive,
                            containerColor = NavyDark
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = isPm,
                        onClick = { isPm = true },
                        label = {
                            Text(
                                text = "PM (Tarde/Noche)",
                                color = if (isPm) NavyDark else TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MintInteractive,
                            containerColor = NavyDark
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botones de acción del diálogo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = TextLightBlue)
                    }

                    Button(
                        onClick = {
                            onConfirm(formattedPreview)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintInteractive,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_confirmar_hora_especifica")
                    ) {
                        Text("Confirmar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun campoMedColores() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    disabledTextColor = TextMuted,
    focusedBorderColor = MintInteractive,
    unfocusedBorderColor = NavySurfaceVariant,
    disabledBorderColor = NavySurfaceVariant.copy(alpha = 0.5f),
    focusedLabelColor = MintInteractive,
    cursorColor = MintInteractive
)
