package com.example

import com.example.notifications.DosisOmitidaNotifier
import com.sun.net.httpserver.HttpServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.Date

/**
 * Aviso de dosis omitida hacia n8n (CP07). Se prueba el JSON y el envío contra un servidor HTTP local
 * que hace de webhook de n8n.
 *
 * Ejecutar: ./gradlew testDebugUnitTest --tests "com.example.DosisOmitidaTest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DosisOmitidaTest {

    private fun payloadDePrueba(): JSONObject = DosisOmitidaNotifier.construirPayload(
        nombrePaciente = "María Gómez",
        correoPaciente = "maria.gomez@correo.com",
        uid = "uid-maria",
        contactoNombre = "Isabela Gómez",
        contactoParentesco = "Hija",
        contactoTelefono = "+57 310 000 0000",
        medicationId = 7L,
        nombreMedicamento = "Losartán",
        dosis = "50 mg",
        horaProgramada = "08:00 AM",
        minutosSinConfirmar = 30,
        esPrueba = false,
        ahora = Date(0)
    )

    // DO01: el JSON lleva todo lo que n8n necesita para armar el correo.
    @Test
    fun do01_payloadTieneLosDatosDelCorreo() {
        val json = payloadDePrueba()

        assertEquals("DOSIS_OMITIDA", json.getString("evento"))
        assertEquals("maria.gomez@correo.com", json.getJSONObject("paciente").getString("correo"))
        assertEquals("María Gómez", json.getJSONObject("paciente").getString("nombre"))
        assertEquals("Losartán", json.getJSONObject("medicamento").getString("nombre"))
        assertEquals("50 mg", json.getJSONObject("medicamento").getString("dosis"))
        assertEquals("08:00 AM", json.getJSONObject("medicamento").getString("horaProgramada"))
        assertEquals("+57 310 000 0000", json.getJSONObject("contactoEmergencia").getString("telefono"))
        assertEquals(30, json.getInt("minutosSinConfirmar"))
        assertTrue(json.getString("fechaHora").startsWith("19"))
    }

    // DO02: el POST llega al webhook con el JSON completo y se recibe 200.
    @Test
    fun do02_envioAlWebhookLlegaCompleto() {
        var cuerpoRecibido = ""
        var tipoRecibido = ""
        val servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/webhook/dosis-omitida") { intercambio ->
            cuerpoRecibido = intercambio.requestBody.readBytes().toString(Charsets.UTF_8)
            tipoRecibido = intercambio.requestHeaders.getFirst("Content-Type").orEmpty()
            val respuesta = """{"ok":true}""".toByteArray()
            intercambio.sendResponseHeaders(200, respuesta.size.toLong())
            intercambio.responseBody.use { it.write(respuesta) }
        }
        servidor.start()
        try {
            val url = "http://127.0.0.1:${servidor.address.port}/webhook/dosis-omitida"
            val codigo = DosisOmitidaNotifier.enviarAN8n(url, payloadDePrueba())

            assertEquals(200, codigo)
            assertTrue(tipoRecibido.startsWith("application/json"))
            val recibido = JSONObject(cuerpoRecibido)
            assertEquals("DOSIS_OMITIDA", recibido.getString("evento"))
            assertEquals("Losartán", recibido.getJSONObject("medicamento").getString("nombre"))
        } finally {
            servidor.stop(0)
        }
    }

    // DO03: si n8n está apagado, la app no se cae: el envío devuelve -1.
    @Test
    fun do03_n8nApagadoNoRompeLaApp() {
        // Se toma un puerto libre y se cierra, así nadie escucha en él.
        val puertoLibre = java.net.ServerSocket(0).use { it.localPort }

        val codigo = DosisOmitidaNotifier.enviarAN8n("http://127.0.0.1:$puertoLibre/webhook/dosis-omitida", payloadDePrueba())

        assertEquals(-1, codigo)
    }
}
