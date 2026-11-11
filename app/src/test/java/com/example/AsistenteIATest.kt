package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AsistenteMedicacionManager
import com.example.ai.AsistenteMedicacionManager.Companion.AVISO_MEDICO
import com.example.ai.AsistenteMedicacionManager.Companion.FRASE_FUERA_DE_ALCANCE
import com.example.ai.MensajeChat
import com.example.data.local.MedicationEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Asistente virtual de texto — criterios 8.4 a 8.6 del checklist y el contexto definido para el agente:
 * alcance, frase de fuera de alcance, aviso médico obligatorio, datos sin inventar y manejo de errores.
 *
 * Ejecutar: ./gradlew testDebugUnitTest --tests "com.example.AsistenteIATest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AsistenteIATest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val asistente = AsistenteMedicacionManager(context)

    private val losartan = MedicationEntity(
        id = 1, name = "Losartán", dosage = "50 mg", scheduleTime = "08:00 AM",
        category = "Presión", instructions = "Después del desayuno", requiresFood = true
    )
    private val metformina = MedicationEntity(
        id = 2, name = "Metformina", dosage = "850 mg", scheduleTime = "01:00 PM", category = "Diabetes"
    )
    private val atorvastatina = MedicationEntity(
        id = 3, name = "Atorvastatina", dosage = "20 mg", scheduleTime = "08:00 PM", category = "Corazón"
    )
    private val meds = listOf(losartan, metformina, atorvastatina)

    /** 10 de noviembre de 2026 a las 10:00 a. m. en Bogotá. */
    private val diezDeLaManana: Date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("America/Bogota") }
        .parse("2026-11-10 10:00")!!

    // IA01: el primer mensaje del asistente incluye el aviso médico obligatorio.
    @Test
    fun ia01_bienvenidaIncluyeAvisoMedico() {
        assertTrue(AsistenteMedicacionManager.MENSAJE_BIENVENIDA.contains(AVISO_MEDICO))
    }

    // IA02: una pregunta ajena a la salud recibe exactamente la frase definida.
    @Test
    fun ia02_fueraDeAlcanceUsaLaFraseExacta() = runBlocking {
        val r = asistente.responderConsulta("Cuéntame un chiste", meds)
        assertEquals(FRASE_FUERA_DE_ALCANCE, r.texto)
        assertFalse(r.esAlertaEmergencia)
    }

    // IA03: si Gemini marca la pregunta como fuera de alcance, también se usa la frase exacta.
    @Test
    fun ia03_geminiFueraDeAlcanceSeNormaliza() {
        val r = asistente.procesarRespuestaGemini("[FUERA DE ALCANCE]", "¿Cuál es la capital de Francia?", "")
        assertEquals(FRASE_FUERA_DE_ALCANCE, r.texto)
    }

    // IA04: una recomendación de Gemini sale sin la etiqueta interna y con el aviso médico al final.
    @Test
    fun ia04_recomendacionLlevaAvisoMedico() {
        val r = asistente.procesarRespuestaGemini(
            "[RECOMENDACION] El Losartán se puede tomar con o sin comida; evita la sal en exceso.",
            "¿Puedo tomar el Losartán con comida?",
            ""
        )
        assertFalse("El usuario no debe ver la etiqueta interna", r.texto.contains("[RECOMENDACION]"))
        assertTrue(r.texto.startsWith("El Losartán"))
        assertTrue(r.texto.endsWith(AVISO_MEDICO))
    }

    // IA05: el aviso no se repite si ya está en la respuesta.
    @Test
    fun ia05_avisoNoSeDuplica() {
        val conAviso = asistente.conAvisoMedico("Tómalo después del desayuno.")
        assertEquals(conAviso, asistente.conAvisoMedico(conAviso))
    }

    // IA06: una respuesta normal que menciona "emergencias" ya no se pinta como alerta roja.
    @Test
    fun ia06_palabraEmergenciasNoActivaAlerta() {
        val r = asistente.procesarRespuestaGemini(
            "Tu próxima toma es la Metformina a la 1:00 p. m. Guarda el número de emergencias a mano.",
            "¿Cuál es mi próxima toma?",
            ""
        )
        assertFalse(r.esAlertaEmergencia)
    }

    // IA07: si Gemini detecta una emergencia con su etiqueta, sí se muestra la alerta.
    @Test
    fun ia07_etiquetaDeEmergenciaActivaAlerta() {
        val r = asistente.procesarRespuestaGemini(
            "[ALERTA DE EMERGENCIA] Llama ya a la línea 123.",
            "tengo los labios hinchados",
            ""
        )
        assertTrue(r.esAlertaEmergencia)
        assertFalse(r.texto.contains("[ALERTA DE EMERGENCIA]"))
    }

    // IA08: preguntar por una alergia no es una emergencia; una reacción con la garganta hinchada sí.
    @Test
    fun ia08_alergiaLeveNoEsEmergencia() {
        assertFalse(asistente.detectarSintomasEmergencia("¿Soy alérgico a la penicilina?"))
        assertFalse(asistente.detectarSintomasEmergencia("Tengo una alergia leve en la piel"))
        assertTrue(asistente.detectarSintomasEmergencia("Tengo una reacción alérgica y se me hinchó la garganta"))
        assertTrue(asistente.detectarSintomasEmergencia("Me tomé todas las pastillas de una vez"))
    }

    // IA09: sin teléfono registrado, la alerta manda a la línea 123 y no a un número inventado.
    @Test
    fun ia09_emergenciaSinContactoUsaLinea123() = runBlocking {
        val r = asistente.responderConsulta("Me duele mucho el pecho", meds, telefonoEmergencia = "")
        assertTrue(r.esAlertaEmergencia)
        assertTrue(r.texto.contains("123"))
        assertFalse(r.texto.contains("456 7890"))
    }

    // IA10: sin internet, una duda de salud no se rechaza como fuera de tema y lleva el aviso médico.
    @Test
    fun ia10_respaldoLocalNoRechazaDudasDeSalud() {
        val texto = asistente.generarRespuestaLocalRAG("Me duele la cabeza, ¿qué hago?", meds, "María")
        assertFalse(texto == FRASE_FUERA_DE_ALCANCE)
        assertTrue(texto.contains(AVISO_MEDICO))
    }

    // IA11: sin internet, calcula la próxima toma con la hora actual y no menciona horarios inventados.
    @Test
    fun ia11_respaldoLocalCalculaLaProximaToma() {
        val texto = asistente.generarRespuestaLocalRAG("¿Qué me falta por tomar hoy?", meds, "María", diezDeLaManana)
        assertTrue(texto.contains("La próxima es Metformina a las 01:00 PM"))
        assertTrue(texto.contains("Atorvastatina (20 mg) a las 08:00 PM"))
    }

    // IA12: sin internet, la consulta de un medicamento da su dosis y hora registradas, con el aviso.
    @Test
    fun ia12_respaldoLocalDaDatosRegistrados() {
        val texto = asistente.generarRespuestaLocalRAG("¿Cómo me tomo el losartan?", meds, "María")
        assertTrue(texto.contains("50 mg"))
        assertTrue(texto.contains("08:00 AM"))
        assertTrue(texto.contains("Después del desayuno"))
        assertTrue(texto.endsWith(AVISO_MEDICO))
    }

    // IA13: las citas médicas no se inventan.
    @Test
    fun ia13_citasNoSeInventan() {
        val texto = asistente.generarRespuestaLocalRAG("¿Cuándo es mi próxima cita médica?", meds, "María")
        assertTrue(texto.contains("no registra citas"))
    }

    // IA14: las instrucciones de Gemini llevan el contexto, la fecha, la hora y los medicamentos reales.
    @Test
    fun ia14_instruccionesDeSistemaCompletas() {
        val instrucciones = asistente.construirInstruccionesSistema(meds, "María Gómez", "+57 310 000 0000", diezDeLaManana)
        assertTrue(instrucciones.contains("asistente virtual experto y empático"))
        assertTrue(instrucciones.contains("Losartán | Dosis: 50 mg | Hora: 08:00 AM"))
        assertTrue(instrucciones.contains("10:00 AM"))
        assertTrue(instrucciones.contains("noviembre de 2026"))
        assertTrue(instrucciones.contains("No inventes horarios"))
        assertTrue(instrucciones.contains("[FUERA DE ALCANCE]"))
    }

    // IA15: el historial que va a Gemini empieza con el usuario y se limita a los últimos 6 mensajes.
    @Test
    fun ia15_historialEmpiezaConElUsuario() {
        val bienvenida = MensajeChat(texto = AsistenteMedicacionManager.MENSAJE_BIENVENIDA, esDelUsuario = false)
        val conversacion = listOf(bienvenida) + (1..8).map { i ->
            MensajeChat(texto = "mensaje $i", esDelUsuario = i % 2 == 1)
        }

        val historial = asistente.construirHistorial(conversacion)

        assertTrue(historial.size <= AsistenteMedicacionManager.MAX_HISTORIAL)
        assertEquals("user", historial.first().role)
    }

    // IA16: una pregunta vacía o enorme no rompe el asistente.
    @Test
    fun ia16_entradasInvalidasNoRompenElAsistente() = runBlocking {
        val vacia = asistente.responderConsulta("   ", meds)
        assertTrue(vacia.texto.isNotBlank())

        val enorme = asistente.responderConsulta("¿Qué me toca hoy? " + "x".repeat(5000), meds)
        assertTrue(enorme.texto.isNotBlank())
    }
}
