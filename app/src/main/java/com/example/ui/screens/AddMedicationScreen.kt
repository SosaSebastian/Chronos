package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    onBack: () -> Unit,
    onSave: (
        name: String,
        dosage: String,
        scheduleTime: String,
        frequencyHours: Int,
        instructions: String,
        category: String,
        requiresFood: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("50 mg") }
    var scheduleTime by remember { mutableStateOf("08:00 AM") }
    var frequencyHours by remember { mutableIntStateOf(12) }
    var instructions by remember { mutableStateOf("Tomar con un vaso con agua después del desayuno") }
    var category by remember { mutableStateOf("Presión Arterial") }
    var requiresFood by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = NavyDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavySurface,
                    titleContentColor = TextWhite
                ),
                title = {
                    Text("Nuevo Medicamento", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
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
        ) {
            Spacer(Modifier.height(12.dp))

            Text(
                text = "Registrar Dosis y Horario",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = "Llena los datos para programar tus recordatorios",
                fontSize = 16.sp,
                color = TextLightBlue,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // Medication Name
            Text("Nombre del Medicamento *", color = MintInteractive, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                placeholder = { Text("Ej: Losartán, Metformina...", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = MintInteractive,
                    unfocusedBorderColor = NavySurfaceVariant,
                    focusedContainerColor = NavySurface,
                    unfocusedContainerColor = NavySurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 8.dp)
                    .testTag("med_name_input")
            )

            // Quick Name Chips
            Text("Frecuentes:", color = TextLightBlue, fontSize = 13.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Metformina", "Enalapril", "Calcio D3", "Aspirina").forEach { chipName ->
                    SuggestionChip(
                        onClick = { name = chipName },
                        label = { Text(chipName, fontSize = 12.sp, color = TextWhite) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = NavySurfaceVariant),
                        border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = NavySurfaceVariant)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Dosage
            Text("Dosis / Concentración *", color = MintInteractive, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                placeholder = { Text("Ej: 50 mg, 1 tableta, 5 ml", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = MintInteractive,
                    unfocusedBorderColor = NavySurfaceVariant,
                    focusedContainerColor = NavySurface,
                    unfocusedContainerColor = NavySurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 8.dp)
                    .testTag("med_dosage_input")
            )

            Spacer(Modifier.height(10.dp))

            // Schedule Time
            Text("Hora de la Toma *", color = MintInteractive, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = scheduleTime,
                onValueChange = { scheduleTime = it },
                placeholder = { Text("Ej: 08:00 AM", color = TextMuted) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = MintInteractive) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = MintInteractive,
                    unfocusedBorderColor = NavySurfaceVariant,
                    focusedContainerColor = NavySurface,
                    unfocusedContainerColor = NavySurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 8.dp)
                    .testTag("med_time_input")
            )

            // Time presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("07:00 AM", "08:00 AM", "01:00 PM", "08:00 PM").forEach { timeOption ->
                    FilterChip(
                        selected = scheduleTime == timeOption,
                        onClick = { scheduleTime = timeOption },
                        label = { Text(timeOption, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MintInteractive,
                            selectedLabelColor = NavyDark,
                            containerColor = NavySurface,
                            labelColor = TextWhite
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Category
            Text("Categoría Médica", color = MintInteractive, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Presión", "Diabetes", "Corazón", "Vitaminas").forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MintInteractive,
                            selectedLabelColor = NavyDark,
                            containerColor = NavySurface,
                            labelColor = TextWhite
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Instructions
            Text("Indicaciones Especiales", color = MintInteractive, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                placeholder = { Text("Ej: Tomar con alimentos, no acostarse después de tomar", color = TextMuted) },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = MintInteractive,
                    unfocusedBorderColor = NavySurfaceVariant,
                    focusedContainerColor = NavySurface,
                    unfocusedContainerColor = NavySurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 12.dp)
                    .testTag("med_instructions_input")
            )

            // Switch: Requires Food
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¿Tomar con alimentos?",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (requiresFood) "Sí, después de comer" else "No, en ayunas",
                            color = if (requiresFood) MintInteractive else TextMuted,
                            fontSize = 14.sp
                        )
                    }

                    Switch(
                        checked = requiresFood,
                        onCheckedChange = { requiresFood = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NavyDark,
                            checkedTrackColor = MintInteractive,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = NavySurfaceVariant
                        )
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = StatusUrgent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Por favor ingresa el nombre de la medicina"
                    } else {
                        onSave(name, dosage, scheduleTime, frequencyHours, instructions, category, requiresFood)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintInteractive,
                    contentColor = NavyDark
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_medication_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Guardar Medicamento", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
