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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PushReminderManager.createNotificationChannel(this)

        setContent {
            val medicationViewModel: MedicationViewModel = viewModel()
            val currentScreen by medicationViewModel.currentScreen.collectAsStateWithLifecycle()
            val currentFirebaseUser by medicationViewModel.currentFirebaseUser.collectAsStateWithLifecycle()
            val authLoading by medicationViewModel.authLoading.collectAsStateWithLifecycle()
            val authError by medicationViewModel.authError.collectAsStateWithLifecycle()
            val medications by medicationViewModel.medications.collectAsStateWithLifecycle()
            val userProfile by medicationViewModel.userProfile.collectAsStateWithLifecycle()
            val aiMessages by medicationViewModel.aiMessages.collectAsStateWithLifecycle()
            val isAiLoading by medicationViewModel.isAiLoading.collectAsStateWithLifecycle()
            val isSpeaking by medicationViewModel.isSpeaking.collectAsStateWithLifecycle()
            val notificationMessage by medicationViewModel.notificationSuccessMessage.collectAsStateWithLifecycle()

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
                                        userProfile?.let { current ->
                                            medicationViewModel.saveProfile(
                                                current.copy(
                                                    fullName = paciente.nombre,
                                                    emergencyContactName = paciente.contactoEmergencia.nombre,
                                                    emergencyContactPhone = paciente.contactoEmergencia.telefono
                                                )
                                            )
                                        }
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
                                        medicationViewModel.navigateTo(AppScreen.ADD_MEDICATION)
                                    },
                                    onNavigateToAsistenteIa = {
                                        medicationViewModel.navigateTo(AppScreen.AI_ASSISTANT)
                                    },
                                    onNavigateToPerfil = {
                                        medicationViewModel.navigateTo(AppScreen.PROFILE)
                                    },
                                    onNavigateToHistorial = {
                                        medicationViewModel.navigateTo(AppScreen.HISTORIAL_CUMPLIMIENTO)
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
                                    onNavigateToAsistenteIa = {
                                        medicationViewModel.navigateTo(AppScreen.AI_ASSISTANT)
                                    },
                                    onNavigateToPerfil = {
                                        medicationViewModel.navigateTo(AppScreen.PROFILE)
                                    }
                                )
                            }

                            AppScreen.ADD_MEDICATION -> {
                                RegistrarMedicamentoScreen(
                                    onMedicamentoGuardado = { _ ->
                                        medicationViewModel.navigateTo(AppScreen.DASHBOARD)
                                    },
                                    onSyncLocal = { name, dosage, scheduleTime, category, instructions ->
                                        medicationViewModel.addMedication(
                                            name = name,
                                            dosage = dosage,
                                            scheduleTime = scheduleTime,
                                            frequencyHours = 12,
                                            instructions = instructions,
                                            category = category,
                                            requiresFood = true
                                        )
                                    },
                                    onVolver = {
                                        medicationViewModel.navigateTo(AppScreen.DASHBOARD)
                                    }
                                )
                            }

                            AppScreen.AI_ASSISTANT -> {
                                AIAssistantScreen(
                                    messages = aiMessages,
                                    isLoading = isAiLoading,
                                    isSpeaking = isSpeaking,
                                    onSendMessage = { prompt ->
                                        medicationViewModel.askAI(prompt)
                                    },
                                    onSpeak = { text ->
                                        medicationViewModel.speak(text)
                                    },
                                    onStopSpeaking = {
                                        medicationViewModel.stopSpeaking()
                                    },
                                    onBack = {
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
                        }
                    }
                }
            }
        }
    }
}
