package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
    PIN_FAST_ACCESS
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onFirebaseSignIn: (email: String, pass: String) -> Unit = { _, _ -> },
    onFirebaseSignUp: (email: String, pass: String, fullName: String) -> Unit = { _, _, _ -> },
    onNavigateToRegistroPaciente: () -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    emergencyPhone: String = "+57 310 456 7890"
) {
    val context = LocalContext.current
    var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }

    // Form inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("Sr Sebastian Sosa") }
    var pin by remember { mutableStateOf("1234") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val displayError = errorMessage ?: localError

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("login_screen"),
        color = NavyDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / Brand
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(NavySurfaceVariant)
                        .border(2.dp, MintInteractive, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = "Logo Healty Chronos",
                        tint = MintInteractive,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Healty Chronos",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Gestión Inteligente de Medicación",
                    fontSize = 17.sp,
                    color = MintInteractive,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MintInteractive)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Conectado a Firebase Auth (chronos-a3c60)",
                        fontSize = 12.sp,
                        color = TextLightBlue
                    )
                }
            }

            // Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(24.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(NavySurfaceVariant)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Auth Mode Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NavySurfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                authMode = AuthMode.SIGN_IN
                                localError = null
                                onClearError()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (authMode == AuthMode.SIGN_IN) MintInteractive else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (authMode == AuthMode.SIGN_IN) NavyDark else TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("tab_sign_in")
                        ) {
                            Text("Entrar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = {
                                authMode = AuthMode.SIGN_UP
                                localError = null
                                onClearError()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (authMode == AuthMode.SIGN_UP) MintInteractive else androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = if (authMode == AuthMode.SIGN_UP) NavyDark else TextWhite
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("tab_sign_up")
                        ) {
                            Text("Crear Cuenta", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    when (authMode) {
                        AuthMode.SIGN_IN -> {
                            Text(
                                text = "Iniciar Sesión",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "Ingresa con tu correo y contraseña registrados",
                                fontSize = 14.sp,
                                color = TextLightBlue,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )

                            // Email Field
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    localError = null
                                    onClearError()
                                },
                                label = { Text("Correo Electrónico", fontSize = 15.sp, color = TextLightBlue) },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = MintInteractive)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = MintInteractive,
                                    unfocusedBorderColor = NavySurfaceVariant,
                                    focusedLabelColor = MintInteractive
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Password Field
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    localError = null
                                    onClearError()
                                },
                                label = { Text("Contraseña", fontSize = 15.sp, color = TextLightBlue) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = MintInteractive)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                            tint = TextLightBlue
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = MintInteractive,
                                    unfocusedBorderColor = NavySurfaceVariant,
                                    focusedLabelColor = MintInteractive
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input")
                            )

                            if (displayError != null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = StatusUrgent.copy(alpha = 0.15f)),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(StatusUrgent)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = StatusUrgent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = displayError,
                                            color = TextWhite,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Sign In Button
                            Button(
                                onClick = {
                                    if (email.isBlank() || password.isBlank()) {
                                        localError = "Por favor ingresa tu correo y contraseña."
                                    } else {
                                        onFirebaseSignIn(email, password)
                                    }
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MintInteractive,
                                    contentColor = NavyDark
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("login_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = NavyDark,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Verificando con Firebase...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = NavyDark)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Iniciar Sesión", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        AuthMode.SIGN_UP -> {
                            Text(
                                text = "Crear Nueva Cuenta",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "Registra un nuevo usuario en Firebase Auth",
                                fontSize = 14.sp,
                                color = TextLightBlue,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )

                            // Name Field
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = {
                                    fullName = it
                                    localError = null
                                    onClearError()
                                },
                                label = { Text("Nombre del Paciente / Titular", fontSize = 15.sp, color = TextLightBlue) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MintInteractive)
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = MintInteractive,
                                    unfocusedBorderColor = NavySurfaceVariant,
                                    focusedLabelColor = MintInteractive
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_name_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Email Field
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    localError = null
                                    onClearError()
                                },
                                label = { Text("Correo Electrónico", fontSize = 15.sp, color = TextLightBlue) },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = MintInteractive)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = MintInteractive,
                                    unfocusedBorderColor = NavySurfaceVariant,
                                    focusedLabelColor = MintInteractive
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_email_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Password Field
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    localError = null
                                    onClearError()
                                },
                                label = { Text("Contraseña (mínimo 6 caracteres)", fontSize = 15.sp, color = TextLightBlue) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = MintInteractive)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                                            tint = TextLightBlue
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = MintInteractive,
                                    unfocusedBorderColor = NavySurfaceVariant,
                                    focusedLabelColor = MintInteractive
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_password_input")
                            )

                            if (displayError != null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = StatusUrgent.copy(alpha = 0.15f)),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(StatusUrgent)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = StatusUrgent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = displayError,
                                            color = TextWhite,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Sign Up Button
                            Button(
                                onClick = {
                                    if (email.isBlank() || password.isBlank()) {
                                        localError = "Por favor ingresa correo y contraseña."
                                    } else if (password.length < 6) {
                                        localError = "La contraseña debe tener al menos 6 caracteres."
                                    } else {
                                        onFirebaseSignUp(email, password, fullName)
                                    }
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MintInteractive,
                                    contentColor = NavyDark
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("signup_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = NavyDark,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Creando Usuario...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = NavyDark)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Registrarme en Firebase", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = onNavigateToRegistroPaciente,
                                enabled = !isLoading,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MintInteractive
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, MintInteractive),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("btn_ir_registro_ficha_completa")
                            ) {
                                Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = MintInteractive)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Registro con Ficha Médica Completa",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        AuthMode.PIN_FAST_ACCESS -> {
                            Text(
                                text = "Acceso Rápido Asistido",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "Para uso rápido en tablet o celular del adulto mayor",
                                fontSize = 14.sp,
                                color = TextLightBlue,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )

                            OutlinedTextField(
                                value = pin,
                                onValueChange = {
                                    if (it.length <= 6) {
                                        pin = it
                                        localError = null
                                    }
                                },
                                label = { Text("PIN de Acceso (Ej: 1234)", fontSize = 15.sp, color = TextLightBlue) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = MintInteractive)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = MintInteractive,
                                    unfocusedBorderColor = NavySurfaceVariant,
                                    focusedLabelColor = MintInteractive
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pin_input")
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = onLoginSuccess,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MintInteractive,
                                    contentColor = NavyDark
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("pin_login_button")
                            ) {
                                Text("Ingresar con PIN", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary Fast / Assisted Access toggle
                    if (authMode != AuthMode.PIN_FAST_ACCESS) {
                        OutlinedButton(
                            onClick = {
                                onLoginSuccess()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MintInteractive),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MintInteractive)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("demo_access_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintInteractive)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Acceso Rápido Asistido (Don Sebastian)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Bottom Emergency Direct Dial
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = MintInteractive,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Línea de Emergencia Familiar",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = emergencyPhone,
                                fontSize = 13.sp,
                                color = TextLightBlue
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$emergencyPhone"))
                            context.startActivity(dialIntent)
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = StatusUrgent,
                            contentColor = TextWhite
                        ),
                        modifier = Modifier.testTag("login_emergency_call_btn")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Llamar emergencia")
                    }
                }
            }
        }
    }
}
