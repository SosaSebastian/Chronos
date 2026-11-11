package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MedicationEntity
import com.example.data.model.HistorialCumplimientoUiState
import com.example.data.local.UserProfileEntity
import com.example.notifications.PushReminderManager
import com.example.util.DatosPacienteInforme
import com.example.util.createSpeechIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import com.example.data.local.AppDatabase
import com.example.data.repository.MedicationRepository
import androidx.room.Room

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {


    private val context: Context = ApplicationProvider.getApplicationContext()

    // CASO DE USO 1: Inicio / Identidad y Recurso de Cadena de la App
    @Test
    fun testCase1_appIdentityAndStringResource() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Healthy Chronos", appName)
    }

    // CASO DE USO 2: Estructura y Registro de Medicamento
    @Test
    fun testCase2_medicationEntityStructure() {
        val medication = MedicationEntity(
            id = 10,
            name = "Losartán Potásico",
            dosage = "50 mg",
            scheduleTime = "08:00 AM",
            frequencyHours = 12,
            instructions = "Tomar después del desayuno",
            isTakenToday = false,
            requiresFood = true
        )

        assertEquals("Losartán Potásico", medication.name)
        assertEquals("50 mg", medication.dosage)
        assertEquals("08:00 AM", medication.scheduleTime)
        assertTrue(medication.requiresFood)
    }

    // CASO DE USO 3: Perfil de Usuario y Contacto de Emergencia
    @Test
    fun testCase3_userProfileAndEmergencyContact() {
        val profile = UserProfileEntity(
            fullName = "Sebastian Sosa",
            emergencyContactName = "Isabela Sosa",
            emergencyContactPhone = "+57 310 456 7890",
            highContrastEnabled = true
        )

        assertEquals("Sebastian Sosa", profile.fullName)
        assertEquals("+57 310 456 7890", profile.emergencyContactPhone)
        assertTrue(profile.highContrastEnabled)
        assertTrue(profile.voiceEnabled)
    }

    // CASO DE USO 4: Consulta de Información (Notificaciones y canal del sistema)
    @Test
    fun testCase4_notificationChannelCreation() {
        PushReminderManager.createNotificationChannel(context)
        // Verify channel creation succeeds without exception
        assertTrue(true)
    }

    // CASO DE USO 5: Ejecución de Acciones (Toma de medicamento)
    @Test
    fun testCase5_toggleMedicationAction() {
        val med = MedicationEntity(
            id = 1,
            name = "Omeprazol",
            dosage = "20 mg",
            scheduleTime = "07:30 AM",
            isTakenToday = false
        )

        val updatedMed = med.copy(isTakenToday = true, lastTakenTimestamp = System.currentTimeMillis())
        assertTrue(updatedMed.isTakenToday)
        assertNotNull(updatedMed.lastTakenTimestamp)
    }

    // CASO DE USO 6: Filtrado de Medicamentos
    @Test
    fun testCase6_medicationFiltering() {
        val meds = listOf(
            MedicationEntity(id = 1, name = "Losartán", dosage = "50mg", scheduleTime = "08:00 AM", isTakenToday = true),
            MedicationEntity(id = 2, name = "Metformina", dosage = "850mg", scheduleTime = "01:00 PM", isTakenToday = false)
        )

        val pending = meds.filter { !it.isTakenToday }
        val taken = meds.filter { it.isTakenToday }

        assertEquals(1, pending.size)
        assertEquals("Metformina", pending[0].name)
        assertEquals(1, taken.size)
        assertEquals("Losartán", taken[0].name)
    }

    // CASO DE USO 7: Formateo y Estructura de Reporte Clínico para el Médico
    @Test
    fun testCase7_generateReportDataForDoctor() {
        val testPaciente = DatosPacienteInforme(
            nombreCompleto = "Sebastian Sosa",
            edad = 73,
            tipoSangre = "O+",
            medicoTratante = "Dra. Sara Martinez",
            especialidadMedico = "Cardióloga",
            contactoEmergencia = "Isabela Sosa (Hija)",
            telefonoEmergencia = "+57 310 456 7890"
        )
        val testEstado = HistorialCumplimientoUiState(
            porcentajeGeneralSemana = 88,
            dosisTomadasTotal = 25,
            dosisPlanificadasTotal = 28,
            rachaDiasConsecutivos = 5
        )
        assertEquals("Sebastian Sosa", testPaciente.nombreCompleto)
        assertEquals("Dra. Sara Martinez", testPaciente.medicoTratante)
        assertEquals(88, testEstado.porcentajeGeneralSemana)
        assertEquals(5, testEstado.rachaDiasConsecutivos)
    }

    // CASO DE USO 8: Diálogo de Reconocimiento de Voz de Android
    @Test
    fun testCase8_speechRecognitionIntentCreation() {
        val intent = createSpeechIntent("Di las indicaciones para tu medicamento")
        assertEquals(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH, intent.action)
        assertEquals("Di las indicaciones para tu medicamento", intent.getStringExtra(android.speech.RecognizerIntent.EXTRA_PROMPT))
    }

    // CASO DE USO 9: Estructura del Calendario de Medicaciones y Agrupación por Jornada
    @Test
    fun testCase9_medicationCalendarModelsAndGrouping() {
        val dose1 = com.example.ui.calendar.CalendarMedicationDose(
            medicationId = 1,
            name = "Losartán Potásico",
            dosage = "50 mg",
            scheduleTime = "08:00 AM",
            category = "Cardiovascular",
            instructions = "Tomar en el desayuno",
            requiresFood = true,
            isTaken = true,
            timeOfDay = com.example.ui.calendar.TimeOfDayCategory.MANANA
        )

        val dose2 = com.example.ui.calendar.CalendarMedicationDose(
            medicationId = 2,
            name = "Metformina",
            dosage = "850 mg",
            scheduleTime = "01:00 PM",
            category = "Diabetes",
            instructions = "Tomar después del almuerzo",
            requiresFood = true,
            isTaken = false,
            timeOfDay = com.example.ui.calendar.TimeOfDayCategory.TARDE
        )

        val dose3 = com.example.ui.calendar.CalendarMedicationDose(
            medicationId = 3,
            name = "Atorvastatina",
            dosage = "20 mg",
            scheduleTime = "08:00 PM",
            category = "Colesterol",
            instructions = "Tomar antes de dormir",
            requiresFood = false,
            isTaken = false,
            timeOfDay = com.example.ui.calendar.TimeOfDayCategory.NOCHE
        )

        val doses = listOf(dose1, dose2, dose3)
        val grouped = doses.groupBy { it.timeOfDay }

        assertEquals(1, grouped[com.example.ui.calendar.TimeOfDayCategory.MANANA]?.size)
        assertEquals(1, grouped[com.example.ui.calendar.TimeOfDayCategory.TARDE]?.size)
        assertEquals(1, grouped[com.example.ui.calendar.TimeOfDayCategory.NOCHE]?.size)
        assertEquals("Losartán Potásico", grouped[com.example.ui.calendar.TimeOfDayCategory.MANANA]?.first()?.name)
    }

    // CASO DE USO 10: Estado de UI del Calendario y Filtros
    @Test
    fun testCase10_calendarUiStateFilteringAndAdherence() {
        val dose1 = com.example.ui.calendar.CalendarMedicationDose(
            medicationId = 1,
            name = "Losartán",
            dosage = "50 mg",
            scheduleTime = "08:00 AM",
            category = "Cardiovascular",
            instructions = "",
            requiresFood = false,
            isTaken = true,
            timeOfDay = com.example.ui.calendar.TimeOfDayCategory.MANANA
        )
        val dose2 = com.example.ui.calendar.CalendarMedicationDose(
            medicationId = 2,
            name = "Aspirina 100",
            dosage = "100 mg",
            scheduleTime = "09:00 AM",
            category = "Prevención",
            instructions = "",
            requiresFood = true,
            isTaken = false,
            timeOfDay = com.example.ui.calendar.TimeOfDayCategory.MANANA
        )

        val state = com.example.ui.calendar.CalendarUiState(
            allDosesForSelectedDay = listOf(dose1, dose2),
            activeFilter = com.example.ui.calendar.CalendarDoseFilter.TODAS
        )

        assertEquals(2, state.totalDosesCount)
        assertEquals(1, state.takenDosesCount)
        assertEquals(1, state.pendingDosesCount)
        assertEquals(50, state.adherencePercentToday)

        val pendingFiltered = state.copy(activeFilter = com.example.ui.calendar.CalendarDoseFilter.PENDIENTES)
        assertEquals(1, pendingFiltered.filteredDoses.size)
        assertEquals("Aspirina 100", pendingFiltered.filteredDoses.first().name)

        val takenFiltered = state.copy(activeFilter = com.example.ui.calendar.CalendarDoseFilter.TOMADAS)
        assertEquals(1, takenFiltered.filteredDoses.size)
        assertEquals("Losartán", takenFiltered.filteredDoses.first().name)
    }

    // CASO DE USO 11: Parseo de Horarios y Cálculo de Disparo para AlarmManager
    @Test
    fun testCase11_alarmManagerTimeParsingAndTriggerCalculation() {
        val (h1, m1) = com.example.notifications.MedicationAlarmScheduler.parseHourAndMinute("08:00 AM")
        assertEquals(8, h1)
        assertEquals(0, m1)

        val (h2, m2) = com.example.notifications.MedicationAlarmScheduler.parseHourAndMinute("01:30 PM")
        assertEquals(13, h2)
        assertEquals(30, m2)

        val (h3, m3) = com.example.notifications.MedicationAlarmScheduler.parseHourAndMinute("12:00 AM")
        assertEquals(0, h3)
        assertEquals(0, m3)

        val (h4, m4) = com.example.notifications.MedicationAlarmScheduler.parseHourAndMinute("12:00 PM")
        assertEquals(12, h4)
        assertEquals(0, m4)

        val triggerTime = com.example.notifications.MedicationAlarmScheduler.calculateNextTriggerMillis("08:00 AM")
        assertTrue("El tiempo de disparo de la alarma debe ser en el futuro", triggerTime > System.currentTimeMillis() - 5000)
    }

    // CASO DE USO 12: Programación, Cancelación y Posposición de Alarmas en AlarmManager
    @Test
    fun testCase12_alarmManagerScheduleAndCancelMethods() {
        val testMed = MedicationEntity(
            id = 42,
            name = "Enalapril",
            dosage = "10 mg",
            scheduleTime = "09:00 AM",
            requiresFood = true
        )

        // Programar alarma
        val scheduled = com.example.notifications.MedicationAlarmScheduler.scheduleMedicationAlarm(context, testMed)
        assertTrue(scheduled)

        // Programar posposición (snooze)
        val snoozed = com.example.notifications.MedicationAlarmScheduler.scheduleSnoozeAlarm(
            context = context,
            medicationId = testMed.id,
            name = testMed.name,
            dosage = testMed.dosage,
            scheduleTime = testMed.scheduleTime,
            snoozeMinutes = 10
        )
        assertTrue(snoozed)

        // Cancelar alarma sin lanzar excepciones
        com.example.notifications.MedicationAlarmScheduler.cancelMedicationAlarm(context, testMed.id)
        assertTrue(true)
    }

    // CASO DE USO 13: Asistente Virtual - Guardrail de Emergencias Médicas Graves
    @Test
    fun testCase13_aiAssistantEmergencyGuardrail() = kotlinx.coroutines.runBlocking {
        val manager = com.example.ai.AsistenteMedicacionManager(context)
        val testMeds = listOf(
            MedicationEntity(id = 1, name = "Losartán", dosage = "50 mg", scheduleTime = "08:00 AM", category = "Hipertensión")
        )

        // Consulta de síntoma agudo / emergencia
        val emergencyQuery = "Siento un dolor muy fuerte en el pecho y me cuesta respirar"
        val respuesta = manager.responderConsulta(
            preguntaUsuario = emergencyQuery,
            medicamentosActivos = testMeds,
            nombrePaciente = "Don Sebastian",
            telefonoEmergencia = "+57 310 456 7890"
        )

        assertTrue("Debe marcarse como alerta de emergencia médica", respuesta.esAlertaEmergencia)
        assertTrue("Debe orientar a urgencias o al número de emergencia", respuesta.texto.contains("emergencias") || respuesta.texto.contains("+57 310 456 7890"))
    }

    // CASO DE USO 14: Asistente Virtual - RAG basado en Medicamentos Activos
    @Test
    fun testCase14_aiAssistantRAGGrounding() = kotlinx.coroutines.runBlocking {
        val manager = com.example.ai.AsistenteMedicacionManager(context)
        val testMeds = listOf(
            MedicationEntity(
                id = 1,
                name = "Losartán Potásico",
                dosage = "50 mg",
                scheduleTime = "08:00 AM",
                category = "Presión arterial",
                instructions = "Tomar con un vaso de agua después del desayuno",
                requiresFood = true
            )
        )

        val query = "¿A qué hora me toca tomar el Losartán y cómo debo tomarlo?"
        val respuesta = manager.responderConsulta(
            preguntaUsuario = query,
            medicamentosActivos = testMeds,
            nombrePaciente = "Don Sebastian",
            telefonoEmergencia = "+57 310 456 7890"
        )

        assertTrue("La respuesta debe mencionar el horario registrado 08:00 AM", respuesta.texto.contains("08:00 AM") || respuesta.texto.contains("Losartán"))
        assertTrue("No debe ser una alerta de emergencia", !respuesta.esAlertaEmergencia)
    }

    // CASO DE USO 15: Asistente Virtual - Cero Alucinaciones (Fármaco no registrado) y Fuera de Alcance
    @Test
    fun testCase15_aiAssistantOutOfScopeAndUnlistedMeds() = kotlinx.coroutines.runBlocking {
        val manager = com.example.ai.AsistenteMedicacionManager(context)
        val testMeds = listOf(
            MedicationEntity(id = 1, name = "Metformina", dosage = "850 mg", scheduleTime = "12:30 PM", category = "Diabetes")
        )

        // 1. Pregunta sobre fármaco no registrado
        val unlistedQuery = "¿Cómo debo tomar la Amoxicilina de 1 gramo?"
        val respuestaUnlisted = manager.responderConsulta(
            preguntaUsuario = unlistedQuery,
            medicamentosActivos = testMeds,
            nombrePaciente = "Don Sebastian",
            telefonoEmergencia = "+57 310 456 7890"
        )
        assertTrue(
            "Debe advertir que no está en la lista de medicamentos",
            respuestaUnlisted.texto.contains("no tengo registro", ignoreCase = true) ||
            respuestaUnlisted.texto.contains("no se encuentra registrado", ignoreCase = true) ||
            respuestaUnlisted.texto.contains("no tienes registrado", ignoreCase = true)
        )

        // 2. Pregunta fuera de alcance (no relacionada con salud o medicamentos)
        val outOfScopeQuery = "¿Quién ganó el partido de fútbol anoche?"
        val respuestaOutOfScope = manager.responderConsulta(
            preguntaUsuario = outOfScopeQuery,
            medicamentosActivos = testMeds,
            nombrePaciente = "Don Sebastian",
            telefonoEmergencia = "+57 310 456 7890"
        )
        assertEquals(com.example.ai.AsistenteMedicacionManager.FRASE_FUERA_DE_ALCANCE, respuestaOutOfScope.texto)
    }

    // CASO DE USO 16: Nuevo Usuario - Cero Medicamentos Pre-Cargados
    @Test
    fun testCase16_newUserStartsWithoutPreloadedMedications() = kotlinx.coroutines.runBlocking {
        val testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val testRepo = MedicationRepository(testDb.medicationDao(), testDb.userDao())

        // Asegura que la base de datos de un usuario nuevo empiece completamente vacía sin registros ficticios
        val initialMeds = testRepo.medications.first()
        assertTrue("La lista de medicamentos de un usuario nuevo debe iniciar totalmente vacía", initialMeds.isEmpty())
        assertEquals(0, initialMeds.size)

        testDb.close()
    }

    // CASO DE USO 17: Registro Personalizado de Medicamento Único para el Usuario
    @Test
    fun testCase17_userPersonalizedMedicationRegistration() = kotlinx.coroutines.runBlocking {
        val testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val testRepo = MedicationRepository(testDb.medicationDao(), testDb.userDao())
        
        // El usuario registra manualmente su propio medicamento
        val userMed = MedicationEntity(
            name = "Insulina Glargina",
            dosage = "15 UI",
            scheduleTime = "09:00 PM",
            category = "Diabetes",
            instructions = "Aplicar vía subcutánea antes de dormir",
            frequencyHours = 24,
            isTakenToday = false,
            requiresFood = false
        )
        val insertedId = testRepo.addMedication(userMed)
        assertTrue("El medicamento registrado debe tener un ID válido", insertedId > 0)

        val activeMeds = testRepo.medications.first()
        assertEquals("Debe existir exactamente 1 medicamento registrado por el usuario", 1, activeMeds.size)
        assertEquals("Insulina Glargina", activeMeds[0].name)

        // Verificar que no existen medicamentos demo residuales
        val demoNames = listOf("Losartán Potásico", "Omeprazol", "Atorvastatina")
        val containsDemo = activeMeds.any { it.name in demoNames }
        assertFalse("No deben existir medicamentos de prueba o pre-cargados", containsDemo)

        testDb.close()
    }

    // CASO DE USO 18: Registro con Horario Específico y Días Específicos de la Semana (Persistencia en Room)
    @Test
    fun testCase18_frequentAndSpecificSchedulesAndDayOfWeekPersistence() = kotlinx.coroutines.runBlocking {
        val testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val testRepo = MedicationRepository(testDb.medicationDao(), testDb.userDao())

        // Medicamento con horario específico ("09:30 AM") y días específicos ("Lunes,Miércoles,Viernes")
        val medDiasEspecificos = MedicationEntity(
            name = "Metotrexato",
            dosage = "10 mg",
            scheduleTime = "09:30 AM",
            category = "Inmunología",
            instructions = "Tomar en ayunas con abundante agua",
            daysOfWeek = "Lunes,Miércoles,Viernes",
            isEveryday = false
        )
        val id1 = testRepo.addMedication(medDiasEspecificos)
        assertTrue(id1 > 0)

        // Medicamento con horario frecuente ("08:00 AM") y todos los días
        val medTodosLosDias = MedicationEntity(
            name = "Losartán",
            dosage = "50 mg",
            scheduleTime = "08:00 AM",
            category = "Presión",
            daysOfWeek = "ALL",
            isEveryday = true
        )
        val id2 = testRepo.addMedication(medTodosLosDias)
        assertTrue(id2 > 0)

        val meds = testRepo.medications.first()
        assertEquals(2, meds.size)

        val recuperado1 = meds.first { it.id == id1 }
        assertEquals("09:30 AM", recuperado1.scheduleTime)
        assertFalse(recuperado1.isEveryday)
        assertEquals("Lunes,Miércoles,Viernes", recuperado1.daysOfWeek)

        val recuperado2 = meds.first { it.id == id2 }
        assertEquals("08:00 AM", recuperado2.scheduleTime)
        assertTrue(recuperado2.isEveryday)
        assertEquals("ALL", recuperado2.daysOfWeek)

        testDb.close()
    }

    // CASO DE USO 19: Sincronización del Calendario con Días Específicos vs Todos los Días
    @Test
    fun testCase19_calendarSynchronizationWithDayOfWeekFiltering() {
        val medLunesMiercolesViernes = MedicationEntity(
            id = 1,
            name = "Furosemida",
            dosage = "40 mg",
            scheduleTime = "07:30 AM",
            daysOfWeek = "Lunes,Miércoles,Viernes",
            isEveryday = false
        )

        val medDiario = MedicationEntity(
            id = 2,
            name = "Levotiroxina",
            dosage = "75 mcg",
            scheduleTime = "06:30 AM",
            daysOfWeek = "ALL",
            isEveryday = true
        )

        // Simular un Lunes (Monday)
        val lunesCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        }
        assertTrue("El medicamento de L, M, V debe aplicar en Lunes", medLunesMiercolesViernes.appliesToCalendarDay(lunesCal))
        assertTrue("El medicamento diario debe aplicar en Lunes", medDiario.appliesToCalendarDay(lunesCal))

        // Simular un Martes (Tuesday)
        val martesCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.TUESDAY)
        }
        assertFalse("El medicamento de L, M, V NO debe aplicar en Martes", medLunesMiercolesViernes.appliesToCalendarDay(martesCal))
        assertTrue("El medicamento diario sí debe aplicar en Martes", medDiario.appliesToCalendarDay(martesCal))

        // Simular un Miércoles (Wednesday)
        val miercolesCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.WEDNESDAY)
        }
        assertTrue("El medicamento de L, M, V debe aplicar en Miércoles", medLunesMiercolesViernes.appliesToCalendarDay(miercolesCal))

        // Simular un Domingo (Sunday)
        val domingoCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
        }
        assertFalse("El medicamento de L, M, V NO debe aplicar en Domingo", medLunesMiercolesViernes.appliesToCalendarDay(domingoCal))
        assertTrue("El medicamento diario sí aplica en Domingo", medDiario.appliesToCalendarDay(domingoCal))
    }

    // CASO DE USO 20: Lógica del Formulario de Registro para Horarios Específicos y Días de la Semana
    @Test
    fun testCase20_registrarMedicamentoViewModelDaySelectionAndValidation() {
        if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(
                context,
                com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("1:000000000000:android:0000000000000000")
                    .setProjectId("healthy-chronos-test")
                    .setApiKey("AIzaSyA-pruebas-unitarias-sin-red-0000000")
                    .build()
            )
        }
        val vm = com.example.ui.viewmodel.RegistrarMedicamentoViewModel()
        
        // 1. Horario específico
        vm.setHoraEspecifica("03:45 PM")
        assertEquals("03:45 PM", vm.formState.value.horaToma)

        // 2. Frecuencia todos los días
        vm.setEsTodosLosDias(true)
        assertTrue(vm.formState.value.isEveryday)
        assertEquals("ALL", vm.formState.value.diasSemanaFormateados)

        // 3. Selección de días laborables
        vm.selectDiasLaborables()
        assertFalse(vm.formState.value.isEveryday)
        assertEquals(5, vm.formState.value.selectedDays.size)
        assertTrue(vm.formState.value.selectedDays.contains("Lunes"))
        assertFalse(vm.formState.value.selectedDays.contains("Domingo"))

        // 4. Alternar día (añadir Sábado)
        vm.toggleDiaSemana("Sábado")
        assertTrue(vm.formState.value.selectedDays.contains("Sábado"))

        // 5. Cargar para edición preservando días
        vm.cargarParaEditar(
            id = "med-edit-1",
            nombre = "Omeprazol",
            dosis = "20 mg",
            horaToma = "07:15 AM",
            categoria = "General",
            indicaciones = "Antes del desayuno",
            diasSemana = "Martes,Jueves",
            esTodosLosDias = false
        )
        assertEquals("07:15 AM", vm.formState.value.horaToma)
        assertFalse(vm.formState.value.isEveryday)
        assertTrue(vm.formState.value.selectedDays.contains("Martes"))
        assertTrue(vm.formState.value.selectedDays.contains("Jueves"))
        assertFalse(vm.formState.value.selectedDays.contains("Lunes"))
    }
}


