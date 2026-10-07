package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserProfileEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: UserProfileEntity?,
    onSaveProfile: (UserProfileEntity) -> Unit,
    onTestPushNotification: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val currentProfile = profile ?: UserProfileEntity()

    var voiceEnabled by remember(currentProfile.voiceEnabled) {
        mutableStateOf(currentProfile.voiceEnabled)
    }
    var highContrastEnabled by remember(currentProfile.highContrastEnabled) {
        mutableStateOf(currentProfile.highContrastEnabled)
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
                    Text("Perfil y Ajustes", fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
                    onClick = { onNavigate(AppScreen.DASHBOARD) },
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = "Dashboard") },
                    label = { Text("Medicamentos", fontSize = 13.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = TextLightBlue,
                        unselectedTextColor = TextLightBlue
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigate(AppScreen.AI_ASSISTANT) },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "Asistente IA") },
                    label = { Text("Asistente IA", fontSize = 13.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = TextLightBlue,
                        unselectedTextColor = TextLightBlue
                    )
                )

                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already here */ },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Mi Perfil", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NavyDark,
                        selectedTextColor = MintInteractive,
                        indicatorColor = MintInteractive,
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            // Patient Identity Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavySurfaceVariant)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(NavySurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MintInteractive,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(
                            text = currentProfile.fullName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Edad: ${currentProfile.age} años • Tipo Sangre: ${currentProfile.bloodType}",
                            fontSize = 15.sp,
                            color = TextLightBlue,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = currentProfile.doctorName,
                            fontSize = 14.sp,
                            color = MintInteractive,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Emergency Contact Section
            Text(
                text = "Contacto de Emergencia",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MintInteractive
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavySurfaceVariant)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentProfile.emergencyContactName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = currentProfile.emergencyContactPhone,
                                fontSize = 16.sp,
                                color = TextLightBlue,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${currentProfile.emergencyContactPhone}"))
                                context.startActivity(dialIntent)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = StatusUrgent,
                                contentColor = TextWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("call_emergency_profile_btn")
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Llamar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Push Notifications Section
            Text(
                text = "Servicio Externo de Recordatorios",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MintInteractive
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavySurfaceVariant)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Sistema de Notificaciones Push Activo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Envía alertas sonoras y con vibración para recordar la hora exacta de cada medicamento.",
                        fontSize = 14.sp,
                        color = TextLightBlue,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    Button(
                        onClick = onTestPushNotification,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintInteractive,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("trigger_push_test_btn")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Simular Notificación Push de Medicamento", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Accessibility Options
            Text(
                text = "Opciones de Accesibilidad",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MintInteractive
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavySurfaceVariant)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Voice Reader Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lectura en voz alta (TTS)", fontSize = 16.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)
                            Text("Lee automáticamente las respuestas de la IA", fontSize = 13.sp, color = TextLightBlue)
                        }
                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = {
                                voiceEnabled = it
                                onSaveProfile(currentProfile.copy(voiceEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NavyDark,
                                checkedTrackColor = MintInteractive
                            )
                        )
                    }

                    HorizontalDivider(
                        color = NavySurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // High Contrast Theme Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modo Alto Contraste", fontSize = 16.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)
                            Text("Optimizado en Azul Marino, Menta y Blanco", fontSize = 13.sp, color = TextLightBlue)
                        }
                        Switch(
                            checked = highContrastEnabled,
                            onCheckedChange = {
                                highContrastEnabled = it
                                onSaveProfile(currentProfile.copy(highContrastEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NavyDark,
                                checkedTrackColor = MintInteractive
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Logout Button
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusUrgent),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(StatusUrgent)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("logout_btn")
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar Sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
