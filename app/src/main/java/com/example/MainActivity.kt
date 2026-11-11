package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notifications.PushReminderManager
import com.example.ui.screens.*
import com.example.ui.theme.HealtyChronosTheme
import com.example.ui.theme.NavyDark
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MedicationViewModel
import com.example.ui.viewmodel.RegistrarMedicamentoViewModel
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAppCheck(this, intent)
        PushReminderManager.createNotificationChannel(this)

        setContent {
            val medicationViewModel: MedicationViewModel = viewModel()
            // Un solo formulario de medicamentos para registrar y editar.
            val registrarMedicamentoViewModel: RegistrarMedicamentoViewModel = viewModel()
            val currentScreen by medicationViewModel.currentScreen.collectAsStateWithLifecycle()
            val currentFirebaseUser by medicationViewModel.currentFirebaseUser.collectAsStateWithLifecycle()
            val authLoading by medicationViewModel.authLoading.collectAsStateWithLifecycle()
            val authError by medicationViewModel.authError.collectAsStateWithLifecycle()
            val medications by medicationViewModel.medications.collectAsStateWithLifecycle()
            val userProfile by medicationViewModel.userProfile.collectAsStateWithLifecycle()
            val notificationMessage by medicationViewModel.notificationSuccessMessage.collectAsStateWithLifecycle()

            // Estados del Asistente Virtual IA (Modo Texto)
            val aiMessages by medicationViewModel.aiMessages.collectAsStateWithLifecycle()
            val isAiLoading by medicationViewModel.isAiLoading.collectAsStateWithLifecycle()

            // Runtime permission launcher for notifications on Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Permission granted or denied handled */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            HealtyChronosTheme(darkTheme = userProfile?.highContrastEnabled != false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NavyDark)
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                        },
                        label = "screen_transition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            AppScreen.LOGIN -> {
                                LoginScreen(
                                    onLoginSuccess = {
                                        medicationViewModel.navigateTo(AppScreen.DASHBOARD)
                                    },
                                    onFirebaseSignIn = { email, pass ->
                                        medicationViewModel.signInWithFirebase(email, pass) {
                                            medicationViewModel.navigateTo(AppScreen.DASHBOARD)
                                        }
                                    },
                                    onFirebaseSignUp = { email, pass, name ->
                                        medicationViewModel.signUpWithFirebase(email, pass, name) {
                                            medicationViewModel.navigateTo(AppScreen.DASHBOARD)
                                        }
                                    },
                                    onNavigateToRegistroPaciente = {
                                        medicationViewModel.navigateTo(AppScreen.REGISTRO_PACIENTE)
                                    },
                                    isLoading = authLoading,
                                    errorMessage = authError,
                                    onClearError = {
                                        medicationViewModel.clearAuthError()
                                    },
                                    emergencyPhone = userProfile?.emergencyContactPhone ?: "+57 310 456 7890"
                                )
                            }

                            AppScreen.REGISTRO_PACIENTE -> {
                                RegistroPacienteScreen(
                                    onRegistroExitoso = { paciente ->
                                        medicationViewModel.onNewUserRegistered(paciente)
                                        medicationViewModel.navigateTo(AppScreen.DASHBOARD)
                                    },
                                    onVolver = {
                                        medicationViewModel.navigateTo(AppScreen.LOGIN)
                                    }
                                )
                            }

                            AppScreen.DASHBOARD -> {
                                DashboardMedicamentosScreen(
                                    patientName = userProfile?.fullName ?: "Sr. Sebastian",
                                    emergencyPhone = userProfile?.emergencyContactPhone ?: "+57 310 456 7890",
                                    onNavigateToRegistrar = {
                                        registrarMedicamentoViewModel.nuevoRegistro()
                                        medicationViewModel.navigateTo(AppScreen.ADD_MEDICATION)
                                    },
                                    onNavigateToCalendario = {
                                        medicationViewModel.navigateTo(AppScreen.CALENDARIO)
                                    },
                                    onNavigateToPerfil = {
                                        medicationViewModel.navigateTo(AppScreen.PROFILE)
                                    },
                                    onNavigateToHistorial = {
                                        medicationViewModel.navigateTo(AppScreen.HISTORIAL_CUMPLIMIENTO)
                                    },
                                    onNavigateToAsistente = {
                                        medicationViewModel.navigateTo(AppScreen.AI_ASSISTANT)
                                    },
                                    onEditarMedicamento = { med ->
                                        registrarMedicamentoViewModel.cargarParaEditar(
                                            id = med.id,
                                            nombre = med.nombre,
                                            dosis = med.dosis,
                                            horaToma = med.horaToma,
                                            categoria = med.categoria,
                                            indicaciones = med.indicaciones
                                        )
                                        medicationViewModel.navigateTo(AppScreen.ADD_MEDICATION)
                                    }
                                )
                            }

                            AppScreen.CALENDARIO -> {
                                CalendarioMedicacionesScreen(
                                    patientName = userProfile?.fullName ?: "Sr. Sebastian",
                                    emergencyPhone = userProfile?.emergencyContactPhone ?: "+57 310 456 7890",
                                    onNavigateToDashboard = {
                                        medicationViewModel.navigateTo(AppScreen.DASHBOARD)
                                    },
                                    onNavigateToHistorial = {
                                        medicationViewModel.navigateTo(AppScreen.HISTORIAL_CUMPLIMIENTO)
                                    },
                                    onNavigateToPerfil = {
                                        medicationViewModel.navigateTo(AppScreen.PROFILE)
                                    },
                                    onNavigateToRegistrar = {
                                        registrarMedicamentoViewModel.nuevoRegistro()
                                        medicationViewModel.navigateTo(AppScreen.ADD_MEDICATION)
                                    }
                                )
                            }

                            AppScreen.HISTORIAL_CUMPLIMIENTO -> {
                                HistorialCumplimientoScreen(
                                    patientName = userProfile?.fullName ?: "Sr. Sebastian Sosa",
                                    patientAge = userProfile?.age ?: 73,
                                    bloodType = userProfile?.bloodType ?: "O+",
                                    doctorName = userProfile?.doctorName ?: "Dra. Sara Martinez (Cardióloga)",
                                    emergencyContact = userProfile?.emergencyContactName ?: "Isabela Sosa (Hija)",
                                    emergencyPhone = userProfile?.emergencyContactPhone ?: "+57 310 456 7890",
                                    onNavigateToDashboard = {
                                        medicationViewModel.navigateTo(AppScreen.DASHBOARD)
                                    },
                                    onNavigateToCalendario = {
                                        medicationViewModel.navigateTo(AppScreen.CALENDARIO)
                                    },
                                    onNavigateToPerfil = {
                                        medicationViewModel.navigateTo(AppScreen.PROFILE)
                                    }
                                )
                            }

                            AppScreen.ADD_MEDICATION -> {
                                RegistrarMedicamentoScreen(
                                    viewModel = registrarMedicamentoViewModel,
                                    onMedicamentoGuardado = { _ ->
                                        medicationViewModel.navigateTo(AppScreen.DASHBOARD)
                                    },
                                    onSyncLocal = { name, dosage, scheduleTime, category, instructions ->
                                        val original = registrarMedicamentoViewModel.datosOriginales
                                        if (registrarMedicamentoViewModel.enModoEdicion && original != null) {
                                            // Edición: ya se actualizó en Firestore; se ajusta la copia local y la alarma.
                                            medicationViewModel.actualizarMedicamentoLocal(
                                                nombreAnterior = original.nombre,
                                                horaAnterior = original.horaToma,
                                                nombre = name,
                                                dosis = dosage,
                                                horaToma = scheduleTime,
                                                categoria = category,
                                                indicaciones = instructions
                                            )
                                        } else {
                                            // Registro nuevo: ya quedó en Firestore; aquí solo se guarda local y se programa la alarma.
                                            medicationViewModel.addMedication(
                                                name = name,
                                                dosage = dosage,
                                                scheduleTime = scheduleTime,
                                                frequencyHours = 24,
                                                instructions = instructions,
                                                category = category,
                                                requiresFood = true,
                                                sincronizarFirestore = false
                                            )
                                        }
                                    },
                                    onVolver = {
                                        medicationViewModel.navigateTo(AppScreen.DASHBOARD)
                                    }
                                )
                            }

                            AppScreen.PROFILE -> {
                                ProfileScreen(
                                    profile = userProfile,
                                    firebaseUser = currentFirebaseUser,
                                    onSaveProfile = { updated ->
                                        medicationViewModel.saveProfile(updated)
                                    },
                                    onTestPushNotification = {
                                        medicationViewModel.testPushNotification()
                                    },
                                    onNavigate = { screen ->
                                        medicationViewModel.navigateTo(screen)
                                    },
                                    onLogout = {
                                        medicationViewModel.signOutFirebase()
                                    }
                                )
                            }

                            AppScreen.AI_ASSISTANT -> {
                                AsistenteVirtualScreen(
                                    patientName = userProfile?.fullName.orEmpty(),
                                    // Sin contacto registrado, el botón llama a la línea de emergencias 123.
                                    emergencyPhone = userProfile?.emergencyContactPhone?.takeIf { it.isNotBlank() }
                                        ?: com.example.ai.AsistenteMedicacionManager.LINEA_EMERGENCIAS,
                                    messages = aiMessages,
                                    isLoading = isAiLoading,
                                    onSendMessage = { query ->
                                        medicationViewModel.askAssistant(query)
                                    },
                                    onBack = {
                                        medicationViewModel.navigateTo(AppScreen.DASHBOARD)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
