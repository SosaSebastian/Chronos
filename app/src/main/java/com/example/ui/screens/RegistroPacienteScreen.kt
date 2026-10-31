package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Paciente
import com.example.ui.theme.*
import com.example.ui.viewmodel.PacienteViewModel
import com.example.ui.viewmodel.RegistroPacienteUiState

/**
 * Pantalla 1: Formulario de Registro de Usuario y Ficha Clínica del Adulto Mayor.
 * Autentica al usuario en Firebase Authentication y guarda su ficha médica
 * en Cloud Firestore en la ruta `users/{userId}`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroPacienteScreen(
    viewModel: PacienteViewModel = viewModel(),
    onRegistroExitoso: (Paciente) -> Unit = {},
    onVolver: () -> Unit = {}
) {
    BackHandler { onVolver() }

    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isLoading = uiState is RegistroPacienteUiState.Loading

    // Escuchar el estado de éxito
    LaunchedEffect(uiState) {
        if (uiState is RegistroPacienteUiState.Success) {
            onRegistroExitoso((uiState as RegistroPacienteUiState.Success).paciente)
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
                        text = "Ficha Clínica y Registro",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver,
                        enabled = !isLoading,
                        modifier = Modifier.testTag("btn_volver_registro")
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
                .testTag("pantalla_registro_paciente"),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Encabezado
            Text(
                text = "Registro del Adulto Mayor",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = "Crea tu cuenta de acceso y completa los datos médicos requeridos para tu cuidado.",
                fontSize = 15.sp,
                color = TextLightBlue,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Banner de Error (si existe)
            val errorMessage = when (uiState) {
                is RegistroPacienteUiState.Error -> (uiState as RegistroPacienteUiState.Error).message
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
                            modifier = Modifier.size(24.dp)
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
            // SECCIÓN 1: CREDENCIALES DE ACCESO
            // =========================================================================
            SeccionTitulo(icono = Icons.Default.Lock, titulo = "1. Credenciales de Acceso")

            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Correo Electrónico
                    Text(
                        text = "Correo Electrónico *",
                        color = MintInteractive,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = formState.email,
                        onValueChange = { viewModel.updateEmail(it) },
                        enabled = !isLoading,
                        placeholder = { Text("ejemplo@correo.com", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = MintInteractive)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = campoTextoColores(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_registro_email")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Contraseña
                    Text(
                        text = "Contraseña de Acceso (mínimo 6 caracteres) *",
                        color = MintInteractive,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = formState.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        enabled = !isLoading,
                        placeholder = { Text("••••••••", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = MintInteractive)
                        },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                Icon(
                                    imageVector = if (formState.passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (formState.passwordVisible) "Ocultar" else "Mostrar",
                                    tint = TextLightBlue
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (formState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = campoTextoColores(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_registro_password")
                    )
                }
            }

            // =========================================================================
            // SECCIÓN 2: DATOS DEL ADULTO MAYOR
            // =========================================================================
            SeccionTitulo(icono = Icons.Default.Person, titulo = "2. Datos del Adulto Mayor")

            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Nombre Completo
                    Text(
                        text = "Nombre Completo *",
                        color = MintInteractive,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = formState.nombre,
                        onValueChange = { viewModel.updateNombre(it) },
                        enabled = !isLoading,
                        placeholder = { Text("Ej: Sebastian Sosa", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = MintInteractive)
                        },
                        singleLine = true,
                        colors = campoTextoColores(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_registro_nombre")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Edad en años
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Edad (años) *",
                                color = MintInteractive,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = formState.edad,
                                onValueChange = { viewModel.updateEdad(it) },
                                enabled = !isLoading,
                                placeholder = { Text("73", color = TextMuted) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = campoTextoColores(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_registro_edad")
                            )
                        }

                        // Tipo de Sangre (Selector Dropdown)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tipo de Sangre *",
                                color = MintInteractive,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            ExposedDropdownMenuBox(
                                expanded = formState.tipoSangreExpanded,
                                onExpandedChange = { if (!isLoading) viewModel.setTipoSangreExpanded(it) }
                            ) {
                                OutlinedTextField(
                                    value = formState.tipoSangre,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = formState.tipoSangreExpanded)
                                    },
                                    colors = campoTextoColores(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("input_registro_tiposangre")
                                )

                                ExposedDropdownMenu(
                                    expanded = formState.tipoSangreExpanded,
                                    onDismissRequest = { viewModel.setTipoSangreExpanded(false) },
                                    modifier = Modifier.background(NavySurface)
                                ) {
                                    viewModel.tiposDeSangre.forEach { tipo ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = tipo,
                                                    color = if (tipo == formState.tipoSangre) MintInteractive else TextWhite,
                                                    fontWeight = if (tipo == formState.tipoSangre) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = { viewModel.updateTipoSangre(tipo) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SECCIÓN 3: MÉDICO TRATANTE
            // =========================================================================
            SeccionTitulo(icono = Icons.Default.MedicalServices, titulo = "3. Médico Tratante")

            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Nombre del Médico
                    Text(
                        text = "Nombre del Médico *",
                        color = MintInteractive,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = formState.medicoNombre,
                        onValueChange = { viewModel.updateMedicoNombre(it) },
                        enabled = !isLoading,
                        placeholder = { Text("Ej: Dra. Sara Martinez", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.Healing, contentDescription = null, tint = MintInteractive)
                        },
                        singleLine = true,
                        colors = campoTextoColores(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_registro_medico_nombre")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Especialidad
                    Text(
                        text = "Especialidad Médica",
                        color = MintInteractive,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = formState.medicoEspecialidad,
                        onValueChange = { viewModel.updateMedicoEspecialidad(it) },
                        enabled = !isLoading,
                        placeholder = { Text("Ej: Cardióloga / Geriatra", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = MintInteractive)
                        },
                        singleLine = true,
                        colors = campoTextoColores(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_registro_medico_especialidad")
                    )
                }
            }

            // =========================================================================
            // SECCIÓN 4: CONTACTO DE EMERGENCIA
            // =========================================================================
            SeccionTitulo(icono = Icons.Default.PhoneInTalk, titulo = "4. Contacto de Emergencia")

            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Nombre del Familiar
                    Text(
                        text = "Nombre del Familiar o Responsable *",
                        color = MintInteractive,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = formState.contactoNombre,
                        onValueChange = { viewModel.updateContactoNombre(it) },
                        enabled = !isLoading,
                        placeholder = { Text("Ej: Isabela Sosa", color = TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Default.People, contentDescription = null, tint = MintInteractive)
                        },
                        singleLine = true,
                        colors = campoTextoColores(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_registro_contacto_nombre")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Parentesco
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Parentesco",
                                color = MintInteractive,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = formState.contactoParentesco,
                                onValueChange = { viewModel.updateContactoParentesco(it) },
                                enabled = !isLoading,
                                placeholder = { Text("Ej: Hija", color = TextMuted) },
                                singleLine = true,
                                colors = campoTextoColores(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_registro_contacto_parentesco")
                            )
                        }

                        // Teléfono
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = "Teléfono *",
                                color = MintInteractive,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = formState.contactoTelefono,
                                onValueChange = { viewModel.updateContactoTelefono(it) },
                                enabled = !isLoading,
                                placeholder = { Text("+57 310...", color = TextMuted) },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = MintInteractive)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = campoTextoColores(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_registro_contacto_telefono")
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // ACCIÓN PRINCIPAL: CREAR CUENTA Y GUARDAR PERFIL
            // =========================================================================
            Button(
                onClick = {
                    viewModel.registrarYCrearPerfil(onSuccess = onRegistroExitoso)
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
                    .testTag("btn_crear_cuenta_guardar_perfil")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        color = NavyDark,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Registrando en Firebase...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = NavyDark
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Crear Cuenta y Guardar Perfil",
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
private fun SeccionTitulo(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = MintInteractive,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = titulo,
            color = TextLightBlue,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun campoTextoColores() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    disabledTextColor = TextMuted,
    focusedBorderColor = MintInteractive,
    unfocusedBorderColor = NavySurfaceVariant,
    disabledBorderColor = NavySurfaceVariant.copy(alpha = 0.5f),
    focusedLabelColor = MintInteractive,
    cursorColor = MintInteractive
)
