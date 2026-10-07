package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MedicationEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    patientName: String = "Don Roberto",
    emergencyPhone: String = "+57 310 456 7890",
    medications: List<MedicationEntity>,
    notificationMessage: String?,
    onClearNotificationMessage: () -> Unit,
    onToggleMedication: (Long, Boolean) -> Unit,
    onDeleteMedication: (Long) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onTestPushNotification: () -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("TODOS") }

    val filteredList = remember(medications, selectedFilter) {
        when (selectedFilter) {
            "PENDIENTES" -> medications.filter { !it.isTakenToday }
            "TOMADOS" -> medications.filter { it.isTakenToday }
            else -> medications
        }
    }

    val totalCount = medications.size
    val takenCount = medications.count { it.isTakenToday }
    val progressFraction = if (totalCount > 0) takenCount.toFloat() / totalCount else 0f

    Scaffold(
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavySurface,
                    titleContentColor = TextWhite
                ),
                title = {
                    Column {
                        Text(
                            text = "Healty Chronos",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "Hola, $patientName",
                            fontSize = 15.sp,
                            color = MintInteractive
                        )
                    }
                },
                actions = {
                    // Quick Emergency Dial
                    IconButton(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$emergencyPhone"))
                            context.startActivity(dialIntent)
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(StatusUrgent)
                            .testTag("dashboard_emergency_btn")
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Llamar contacto de emergencia",
                            tint = TextWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(AppScreen.AI_ASSISTANT) },
                containerColor = MintInteractive,
                contentColor = NavyDark,
                icon = {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                },
                text = {
                    Text(
                        text = "Consultar con IA",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("ai_assistant_fab")
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
                    onClick = { /* Already on dashboard */ },
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = "Dashboard") },
                    label = { Text("Medicamentos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
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
                    onClick = { onNavigate(AppScreen.AI_ASSISTANT) },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "Asistente IA") },
                    label = { Text("Asistente IA", fontSize = 13.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = TextLightBlue,
                        unselectedTextColor = TextLightBlue
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigate(AppScreen.PROFILE) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Mi Perfil", fontSize = 13.sp) },
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
            // Push Notification Feedback Banner if fired
            AnimatedVisibility(visible = notificationMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurfaceElevated),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MintInteractive)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MintInteractive)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = notificationMessage ?: "",
                                color = TextWhite,
                                fontSize = 14.sp
                            )
                        }
                        IconButton(onClick = onClearNotificationMessage) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextLightBlue)
                        }
                    }
                }
            }

            // Daily Progress Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("progress_card"),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavySurfaceVariant))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tus tomas de hoy",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Surface(
                            color = if (takenCount == totalCount && totalCount > 0) StatusSuccess.copy(alpha = 0.2f) else NavySurfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "$takenCount de $totalCount tomadas",
                                color = if (takenCount == totalCount && totalCount > 0) MintInteractive else TextLightBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp)),
                        color = MintInteractive,
                        trackColor = NavySurfaceVariant
                    )

                    if (takenCount == totalCount && totalCount > 0) {
                        Text(
                            text = "🎉 ¡Excelente Don Roberto! Has completado todas tus tomas del día.",
                            color = MintInteractive,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }

            // Action Quick Buttons (+ Agregar Medicina, Probar Push)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onNavigate(AppScreen.ADD_MEDICATION) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintInteractive,
                        contentColor = NavyDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("add_medication_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar Medicina", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = onTestPushNotification,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MintInteractive),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MintInteractive)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("test_push_btn")
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Probar Alerta Push", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("TODOS", "PENDIENTES", "TOMADOS").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = when (filter) {
                                    "TODOS" -> "Todos ($totalCount)"
                                    "PENDIENTES" -> "Pendientes (${totalCount - takenCount})"
                                    else -> "Tomados ($takenCount)"
                                },
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MintInteractive,
                            selectedLabelColor = NavyDark,
                            containerColor = NavySurface,
                            labelColor = TextWhite
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) MintInteractive else NavySurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Medication List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MintInteractive,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (selectedFilter == "PENDIENTES") "No tienes tomas pendientes en este momento." else "No hay medicamentos en esta sección.",
                            color = TextLightBlue,
                            fontSize = 17.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("medications_list"),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredList, key = { it.id }) { med ->
                        MedicationItemCard(
                            medication = med,
                            onToggle = { onToggleMedication(med.id, med.isTakenToday) },
                            onDelete = { onDeleteMedication(med.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationItemCard(
    medication: MedicationEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBorderColor by animateColorAsState(
        targetValue = if (medication.isTakenToday) StatusSuccess else NavySurfaceVariant,
        label = "cardBorderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, cardBorderColor, RoundedCornerShape(18.dp))
            .testTag("medication_card_${medication.id}"),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Name, Dosage, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = medication.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            color = NavySurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = medication.dosage,
                                color = MintInteractive,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Category
                    Text(
                        text = medication.category,
                        color = TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar medicina",
                        tint = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Time & Instructions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MintInteractive,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Hora de toma: ${medication.scheduleTime}",
                    color = MintInteractive,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (medication.instructions.isNotBlank()) {
                Text(
                    text = medication.instructions,
                    color = TextLightBlue,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Food recommendation badge + Take Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = NavySurfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (medication.requiresFood) Icons.Default.Restaurant else Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = if (medication.requiresFood) StatusPending else MintInteractive,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (medication.requiresFood) "Con alimentos" else "En ayunas",
                            color = TextWhite,
                            fontSize = 13.sp
                        )
                    }
                }

                // Action Button: "Tomar" or "Tomado ✓"
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (medication.isTakenToday) StatusSuccess else MintInteractive,
                        contentColor = NavyDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("toggle_taken_btn_${medication.id}")
                ) {
                    Icon(
                        if (medication.isTakenToday) Icons.Default.Check else Icons.Default.MedicalServices,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (medication.isTakenToday) "Tomado ✓" else "Tomar Ahora",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
