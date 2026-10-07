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
                                    emergencyPhone = userProfile?.emergencyContactPhone ?: "+57 310 456 7890"
                                )
                            }

                            AppScreen.DASHBOARD -> {
                                DashboardScreen(
                                    patientName = userProfile?.fullName ?: "Don Roberto",
                                    emergencyPhone = userProfile?.emergencyContactPhone ?: "+57 310 456 7890",
                                    medications = medications,
                                    notificationMessage = notificationMessage,
                                    onClearNotificationMessage = {
                                        medicationViewModel.clearNotificationMessage()
                                    },
                                    onToggleMedication = { id, isTaken ->
                                        medicationViewModel.toggleMedication(id, isTaken)
                                    },
                                    onDeleteMedication = { id ->
                                        medicationViewModel.deleteMedication(id)
                                    },
                                    onNavigate = { screen ->
                                        medicationViewModel.navigateTo(screen)
                                    },
                                    onTestPushNotification = {
                                        medicationViewModel.testPushNotification()
                                    }
                                )
                            }

                            AppScreen.ADD_MEDICATION -> {
                                AddMedicationScreen(
                                    onBack = {
                                        medicationViewModel.navigateTo(AppScreen.DASHBOARD)
                                    },
                                    onSave = { name, dosage, scheduleTime, freq, instructions, category, requiresFood ->
                                        medicationViewModel.addMedication(
                                            name = name,
                                            dosage = dosage,
                                            scheduleTime = scheduleTime,
                                            frequencyHours = freq,
                                            instructions = instructions,
                                            category = category,
                                            requiresFood = requiresFood
                                        )
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
                                        medicationViewModel.navigateTo(AppScreen.LOGIN)
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
