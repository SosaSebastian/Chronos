package com.example

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AsistenteMedicacionManager
import com.example.data.local.AppDatabase
import com.example.data.local.MedicationEntity
import com.example.data.model.FirebaseErrorMapper
import com.example.data.repository.MedicationRepository
import com.example.notifications.MedicationAlarmReceiver
import com.example.notifications.MedicationAlarmScheduler
import com.example.notifications.MedicationNotificationActionReceiver
import com.example.util.DosisInvalidaException
import com.example.util.ValidadorDosis
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Pruebas de funcionamiento — sección 5 del checklist y plan de pruebas del paper (CP01–CP05).
 *
 *  - PF01–PF04: persistencia y consistencia de los datos (Room).
 *  - CP01: dosis negativa o inválida.
 *  - CP02: confirmar dosis desde la notificación.
 *  - CP03: asistente IA sin conexión.
 *  - CP05: reinicio del dispositivo.
 *
 * Ejecutar: ./gradlew testDebugUnitTest --tests "com.example.PruebasFuncionamientoTest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasFuncionamientoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun losartan() = MedicationEntity(
        name = "Losartán",
        dosage = "50 mg",
        scheduleTime = "08:00 AM",
        category = "Presión",
        instructions = "Tomar después del desayuno"
    )

    private fun metformina() = MedicationEntity(
        name = "Metformina",
        dosage = "850 mg",
        scheduleTime = "01:00 PM",
        category = "Diabetes"
    )

    private fun baseEnMemoria(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    /** Espera (sin bloquear para siempre) a que una condición asíncrona se cumpla. */
    private fun esperarHasta(maxMs: Long, condicion: () -> Boolean): Boolean {
        val limite = System.currentTimeMillis() + maxMs
        while (System.currentTimeMillis() < limite) {
            if (condicion()) return true
            Thread.sleep(25)
        }
        return condicion()
    }

    /**
     * Los receptores de notificaciones y del reinicio usan la base única de la app (AppDatabase.getDatabase).
     * Se cierra y se limpia después de cada prueba para que no se mezclen datos entre pruebas.
     */
    @After
    fun limpiarBaseUnica() {
        try {
            val campo = AppDatabase::class.java.getDeclaredField("INSTANCE")
            campo.isAccessible = true
            (campo.get(null) as? AppDatabase)?.close()
            campo.set(null, null)
        } catch (_: Exception) {
            // Si la base única no se usó en esta prueba, no hay nada que limpiar.
        }
        context.deleteDatabase("healthy_chronos_database")
    }

    // ───────────────────────── Persistencia y consistencia ─────────────────────────

    // PF01: CRUD completo en la base local — crear, consultar, actualizar y eliminar.
    @Test
    fun pf01_crudCompletoDeMedicamentos() = runBlocking {
        val db = baseEnMemoria()
        val repo = MedicationRepository(db.medicationDao(), db.userDao())

        // Crear
        val id = repo.addMedication(losartan())
        assertTrue("El medicamento creado debe tener un id válido", id > 0)

        // Consultar
        val guardado = db.medicationDao().getMedicationById(id)
        assertNotNull(guardado)
        assertEquals("Losartán", guardado!!.name)
        assertEquals("50 mg", guardado.dosage)
        assertEquals("08:00 AM", guardado.scheduleTime)

        // Actualizar
        repo.updateMedication(guardado.copy(dosage = "100 mg", scheduleTime = "09:00 AM"))
        val actualizado = db.medicationDao().getMedicationById(id)!!
        assertEquals("100 mg", actualizado.dosage)
        assertEquals("09:00 AM", actualizado.scheduleTime)

        // Eliminar
        repo.deleteMedication(id)
        assertNull("Después de eliminar no debe existir", db.medicationDao().getMedicationById(id))
        assertTrue(repo.medications.first().isEmpty())

        db.close()
    }

    // PF02: los datos siguen ahí después de cerrar y volver a abrir la base (como al cerrar la app).
    @Test
    fun pf02_losDatosPersistenAlCerrarYAbrirLaApp() = runBlocking {
        val nombreArchivo = "prueba_persistencia.db"
        context.deleteDatabase(nombreArchivo)

        val primeraSesion = Room.databaseBuilder(context, AppDatabase::class.java, nombreArchivo)
            .allowMainThreadQueries()
            .build()
        primeraSesion.medicationDao().insertMedication(losartan())
        primeraSesion.medicationDao().insertMedication(metformina())
        primeraSesion.close()

        val segundaSesion = Room.databaseBuilder(context, AppDatabase::class.java, nombreArchivo)
            .allowMainThreadQueries()
            .build()
        val recuperados = segundaSesion.medicationDao().getAllMedicationsList()
        assertEquals("Deben recuperarse los 2 medicamentos guardados", 2, recuperados.size)
        assertTrue(recuperados.any { it.name == "Losartán" && it.dosage == "50 mg" })
        assertTrue(recuperados.any { it.name == "Metformina" && it.dosage == "850 mg" })

        segundaSesion.close()
        context.deleteDatabase(nombreArchivo)
        Unit
    }

    // PF03: marcar como tomada guarda la hora de la toma; desmarcar la borra (datos consistentes).
    @Test
    fun pf03_marcarYDesmarcarTomaEsConsistente() = runBlocking {
        val db = baseEnMemoria()
        val repo = MedicationRepository(db.medicationDao(), db.userDao())
        val id = repo.addMedication(losartan())

        repo.toggleTakenStatus(id, true)
        val tomada = db.medicationDao().getMedicationById(id)!!
        assertTrue(tomada.isTakenToday)
        assertNotNull("Una dosis tomada debe guardar la hora de la toma", tomada.lastTakenTimestamp)

        repo.toggleTakenStatus(id, false)
        val desmarcada = db.medicationDao().getMedicationById(id)!!
        assertFalse(desmarcada.isTakenToday)
        assertNull("Una dosis no tomada no debe tener hora de toma", desmarcada.lastTakenTimestamp)

        db.close()
    }

    // PF04: el reinicio diario deja todas las dosis pendientes y la lista muestra primero las pendientes.
    @Test
    fun pf04_reinicioDiarioYOrdenDeLaLista() = runBlocking {
        val db = baseEnMemoria()
        val repo = MedicationRepository(db.medicationDao(), db.userDao())
        val idLosartan = repo.addMedication(losartan())
        repo.addMedication(metformina())

        repo.toggleTakenStatus(idLosartan, true)
        val lista = repo.medications.first()
        assertEquals("La dosis pendiente debe salir primero", "Metformina", lista.first().name)
        assertTrue(lista.last().isTakenToday)

        repo.resetAllDailyTakens()
        assertTrue(
            "Al empezar el día, ninguna dosis debe quedar marcada como tomada",
            repo.getAllMedicationsList().none { it.isTakenToday }
        )

        db.close()
    }

    // ───────────────────────── Plan de pruebas del paper ─────────────────────────

    // CP01: una dosis negativa, cero o sin cantidad lanza DosisInvalidaException y no cierra la app.
    @Test
    fun cp01_dosisNegativaOInvalidaSeRechaza() {
        val invalidas = listOf("-5 mg", "- 5 mg", "0 mg", "0,0 mg", "mg", "   ")
        for (dosis in invalidas) {
            try {
                ValidadorDosis.validar(dosis)
                fail("La dosis \"$dosis\" debió rechazarse")
            } catch (e: DosisInvalidaException) {
                assertNotNull("El error debe traer un mensaje para el usuario", e.message)
            }
        }

        assertEquals(50.0, ValidadorDosis.validar("50 mg"), 0.0)
        assertEquals(0.5, ValidadorDosis.validar("0,5 mg"), 0.0)
        assertEquals(600.0, ValidadorDosis.validar("600 mg / 400 UI"), 0.0)
        assertEquals(15.0, ValidadorDosis.validar("15 UI"), 0.0)
    }

    // CP02: "Marcar como tomada" desde la notificación actualiza el historial en menos de 1 segundo.
    @Test
    fun cp02_confirmarDosisDesdeLaNotificacion() = runBlocking {
        val db = AppDatabase.getDatabase(context, this)
        val id = db.medicationDao().insertMedication(losartan())

        val intent = Intent(context, MedicationNotificationActionReceiver::class.java).apply {
            action = MedicationNotificationActionReceiver.ACTION_MARK_TAKEN
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_ID, id)
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_NAME, "Losartán")
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_DOSAGE, "50 mg")
            putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_SCHEDULE_TIME, "08:00 AM")
        }

        val inicio = System.currentTimeMillis()
        MedicationNotificationActionReceiver().onReceive(context, intent)

        val actualizada = esperarHasta(1000) {
            runBlocking { db.medicationDao().getMedicationById(id)?.isTakenToday == true }
        }
        val demora = System.currentTimeMillis() - inicio

        assertTrue("La dosis debe quedar como tomada en menos de 1 s (tardó $demora ms)", actualizada)
        assertNotNull(db.medicationDao().getMedicationById(id)?.lastTakenTimestamp)
    }

    // CP03: sin conexión (o si Gemini falla) el asistente responde igual y no bloquea la app.
    @Test
    fun cp03_asistenteSinConexionRespondeConRespaldoLocal() = runBlocking {
        val asistente = AsistenteMedicacionManager(context)
        val meds = listOf(losartan(), metformina())

        // Motor de respaldo local: es el que responde cuando no hay internet o Gemini falla.
        val local = asistente.generarRespuestaLocalRAG("¿Qué me toca hoy?", meds, "María")
        assertTrue("El respaldo debe usar los medicamentos del paciente", local.contains("Losartán") && local.contains("Metformina"))

        // Flujo completo: nunca lanza excepción y siempre devuelve texto.
        val respuesta = withTimeout(40_000) {
            asistente.responderConsulta(
                preguntaUsuario = "¿Qué me toca hoy?",
                medicamentosActivos = meds,
                nombrePaciente = "María",
                telefonoEmergencia = "+57 310 000 0000"
            )
        }
        assertTrue("El asistente nunca debe quedar sin respuesta", respuesta.texto.isNotBlank())
        assertFalse(respuesta.esDelUsuario)

        // Los errores de red de Firebase se traducen a un mensaje claro para el usuario.
        val mensaje = FirebaseErrorMapper.map(FirebaseNetworkException("sin red"))
        assertEquals("Sin conexión a internet. Por favor verifica tu red y reintenta.", mensaje)
    }

    // CP05: al reiniciar el teléfono se vuelven a programar las alarmas de los medicamentos guardados.
    @Test
    fun cp05_alReiniciarSeReprogramanLasAlarmas() = runBlocking {
        val db = AppDatabase.getDatabase(context, this)
        db.medicationDao().insertMedication(losartan())
        db.medicationDao().insertMedication(metformina())

        val alarmas = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        assertTrue("Antes del reinicio no hay alarmas", alarmas.scheduledAlarms.isEmpty())

        MedicationAlarmReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        val reprogramadas = esperarHasta(3000) { alarmas.scheduledAlarms.size >= 2 }
        assertTrue(
            "Deben quedar 2 alarmas programadas después del reinicio (hay ${alarmas.scheduledAlarms.size})",
            reprogramadas
        )
    }
}
