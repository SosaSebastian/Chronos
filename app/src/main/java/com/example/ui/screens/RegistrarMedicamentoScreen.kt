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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import com.example.ui.viewmodel.RegistrarMedicamentoUiState
import com.example.ui.viewmodel.RegistrarMedicamentoViewModel

/**
 * Pantalla 2: Formulario de Registro de Dosis y Horario (`RegistrarMedicamentoScreen.kt`)
 * Replica fielmente el diseño visual de Healty Chronos y persiste el medicamento
 * en Cloud Firestore en la subcolección `users/{userId}/medicamentos`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarMedicamentoScreen(
    viewModel: RegistrarMedicamentoViewModel = viewModel(),
    onMedicamentoGuardado: (String) -> Unit = {},
    onSyncLocal: ((nombre: String, dosis: String, horaToma: String, categoria: String, indicaciones: String) -> Unit)? = null,
    onVolver: () -> Unit = {}
) {
    BackHandler { onVolver() }

    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isLoading = uiState is RegistrarMedicamentoUiState.Loading

    // Notificar éxito al guardar y sincronizar
    LaunchedEffect(uiState) {
        if (uiState is RegistrarMedicamentoUiState.Success) {
            val medId = (uiState as RegistrarMedicamentoUiState.Success).medicamentoId
            onSyncLocal?.invoke(
                formState.nombre,
                formState.dosis,
                formState.horaToma,
                formState.categoria,
                formState.indicaciones
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
                        text = "Registrar Dosis y Horario",
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
                text = "Registrar Dosis y Horario",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = "Llena los datos para programar tus recordatorios",
                fontSize = 15.sp,
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
            // 1. CAMPO NOMBRE DEL MEDICAMENTO + CHIPS FRECUENTES
            // =========================================================================
            Text(
                text = "Nombre del Medicamento *",
                color = MintInteractive,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
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
            // 3. SELECTOR DE HORA (CAMPO CON RELOJ + CHIPS DE HORARIOS FRECUENTES)
            // =========================================================================
            Text(
                text = "Hora de la Toma *",
                color = MintInteractive,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Tarjeta de hora activa destacada
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MintInteractive, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MintInteractive.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Reloj",
                            tint = MintInteractive,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Hora seleccionada",
                            color = TextLightBlue,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formState.horaToma,
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
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

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // 4. SELECTOR DE CATEGORÍA MÉDICA
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
            // 5. INDICACIONES ESPECIALES (CAMPO MULTILÍNEA)
            // =========================================================================
            Text(
                text = "Indicaciones Especiales",
                color = MintInteractive,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
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
            // ACCIÓN: BOTÓN GUARDAR MEDICAMENTO EN FIRESTORE
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
                        text = "Guardando en Firestore...",
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
                        text = "Guardar Medicamento",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
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
