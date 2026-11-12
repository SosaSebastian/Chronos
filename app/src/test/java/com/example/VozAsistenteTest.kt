package com.example

import android.Manifest
import android.app.Application
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ai.MensajeChat
import com.example.ai.voz.EstadoEscucha
import com.example.ai.voz.LectorVoz
import com.example.ai.voz.OpcionVoz
import com.example.ai.voz.ReconocedorVoz
import com.example.ai.voz.VozUtils
import com.example.ui.screens.AsistenteVirtualScreen
import com.example.ui.theme.HealtyChronosTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Asistente de voz (VZ01–VZ11): escuchar → pensar (mismo asistente del chat) → hablar.
 * Las pruebas no usan el micrófono real: prueban las reglas, los fallos controlados y la pantalla.
 *
 * Ejecutar: ./gradlew testDebugUnitTest --tests "com.example.VozAsistenteTest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class VozAsistenteTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    // VZ01: la respuesta se limpia antes de leerla: sin etiquetas, emojis, viñetas ni asteriscos, y con unidades completas.
    @Test
    fun vz01_textoLimpioParaLeer() {
        val limpio = VozUtils.limpiarParaVoz(
            "[RECOMENDACION] 💊 Tu **Losartán** de 50 mg va a las 08:00 AM.\n• Metformina 850mg con comida\n• Vitamina D 1000 UI"
        )
        assertFalse(limpio.contains("RECOMENDACION"))
        assertFalse(limpio.contains("💊"))
        assertFalse(limpio.contains("*"))
        assertFalse(limpio.contains("•"))
        assertTrue(limpio.contains("50 miligramos"))
        assertTrue(limpio.contains("850 miligramos"))
        assertTrue(limpio.contains("1000 unidades internacionales"))
        assertEquals("Llama ya al 123.", VozUtils.limpiarParaVoz("⚠️ [ALERTA DE EMERGENCIA] Llama ya al 123."))
    }

    // VZ02: una respuesta larga se parte por oraciones sin pasar el límite del lector y sin perder texto.
    @Test
    fun vz02_respuestaLargaEnFragmentos() {
        val largo = (1..400).joinToString(" ") { "Oración número $it." }
        val fragmentos = VozUtils.dividirEnFragmentos(largo, 500)
        assertTrue(fragmentos.size > 1)
        assertTrue(fragmentos.all { it.length <= 500 })
        assertEquals(largo, fragmentos.joinToString(" "))
        assertTrue(VozUtils.dividirEnFragmentos("   ").isEmpty())
    }

    // VZ03: cada error del reconocedor tiene un mensaje claro para el paciente.
    @Test
    fun vz03_mensajesDeErrorClaros() {
        assertTrue(VozUtils.mensajeDeError(SpeechRecognizer.ERROR_NO_MATCH).startsWith("No te escuché"))
        assertTrue(VozUtils.mensajeDeError(SpeechRecognizer.ERROR_NETWORK).contains("conexión"))
        assertEquals(VozUtils.SIN_PERMISO, VozUtils.mensajeDeError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
        assertTrue(VozUtils.mensajeDeError(999).isNotBlank())
    }

    // VZ04: el reconocimiento se pide en español de Colombia y con resultados parciales.
    @Test
    fun vz04_intentEnEspanolConParciales() {
        val intent = VozUtils.crearIntentReconocimiento()
        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, intent.action)
        assertEquals("es-CO", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertTrue(intent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
    }

    // VZ05: modo conversación: vuelve a escuchar solo si está activo, la pregunta fue por voz y NO es una emergencia.
    @Test
    fun vz05_modoConversacionNoSigueEnEmergencia() {
        val normal = MensajeChat(texto = "Tu Losartán va a las 8.", esDelUsuario = false)
        val emergencia = MensajeChat(texto = "Llama al 123.", esDelUsuario = false, esAlertaEmergencia = true)
        assertTrue(VozUtils.debeVolverAEscuchar(true, true, normal))
        assertFalse(VozUtils.debeVolverAEscuchar(false, true, normal))
        assertFalse(VozUtils.debeVolverAEscuchar(true, false, normal))
        assertFalse(VozUtils.debeVolverAEscuchar(true, true, emergencia))
    }

    // VZ06: sin servicio de reconocimiento, avisa con un mensaje y no se cierra la app.
    @Test
    fun vz06_sinReconocedorAvisaSinCerrar() {
        val reconocedor = ReconocedorVoz(app, hayReconocedor = { false })
        var aviso: String? = null
        var texto: String? = null
        reconocedor.escuchar(alReconocer = { texto = it }, alFallar = { aviso = it })
        assertEquals(VozUtils.SIN_RECONOCEDOR, aviso)
        assertEquals(null, texto)
        assertEquals(EstadoEscucha.INACTIVO, reconocedor.estado.value)
        reconocedor.liberar()
    }

    // VZ07: el lector no intenta leer una respuesta vacía (solo emojis) y avisa que terminó; detener nunca falla.
    @Test
    fun vz07_lectorConTextoVacioTerminaYDetieneSinFallar() {
        val lector = LectorVoz(app)
        var terminado = false
        lector.hablar("💊 ✅") { terminado = true }
        assertTrue(terminado)
        assertFalse(lector.hablando.value)
        lector.detener()
        lector.liberar()
    }

    // VZ11: se elige la voz más parecida a la colombiana y de mejor calidad (latina antes que de España; nunca una sin instalar).
    @Test
    fun vz11_eligeVozLatinaDeMejorCalidad() {
        val voces = listOf(
            OpcionVoz("es-es-x-eea-local", "es", "ES", calidad = 500, requiereInternet = false, instalada = true),
            OpcionVoz("es-us-x-esc-local", "es", "US", calidad = 300, requiereInternet = false, instalada = true),
            OpcionVoz("es-us-x-sfb-network", "es", "US", calidad = 400, requiereInternet = true, instalada = false),
            OpcionVoz("es-us-x-esd-local", "es", "US", calidad = 400, requiereInternet = false, instalada = false),
            OpcionVoz("en-us-x-iol-local", "en", "US", calidad = 500, requiereInternet = false, instalada = true)
        )
        assertEquals("es-us-x-sfb-network", VozUtils.elegirMejorVoz(voces)?.nombre)
        // Si hay voz de Colombia, gana aunque tenga menos calidad.
        val conColombia = voces + OpcionVoz("es-co-x-local", "spa", "COL", calidad = 300, requiereInternet = false, instalada = true)
        assertEquals("es-co-x-local", VozUtils.elegirMejorVoz(conColombia)?.nombre)
        // Sin voces en español no se cambia nada.
        assertEquals(null, VozUtils.elegirMejorVoz(voces.filter { it.idioma == "en" }))
    }

    private var escuchas = 0
    private var envios = 0
    private var cancelaciones = 0
    private var lecturas = 0

    private fun abrirAsistente(estado: EstadoEscucha = EstadoEscucha.INACTIVO, parcial: String = "", aviso: String? = null) {
        compose.setContent {
            HealtyChronosTheme(darkTheme = true) {
                AsistenteVirtualScreen(
                    patientName = "María",
                    messages = listOf(MensajeChat(id = "m1", texto = "Hola María.", esDelUsuario = false)),
                    isLoading = false,
                    onSendMessage = {},
                    onBack = {},
                    estadoEscucha = estado,
                    textoParcial = parcial,
                    avisoVoz = aviso,
                    onIniciarEscucha = { escuchas++ },
                    onDetenerEscucha = { envios++ },
                    onCancelarVoz = { cancelaciones++ },
                    onLeerMensaje = { lecturas++ }
                )
            }
        }
    }

    // VZ08: con el campo vacío aparece el micrófono; con permiso, tocarlo empieza a escuchar. Al escribir, aparece Enviar.
    @Test
    fun vz08_microfonoEmpiezaAEscuchar() {
        shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)
        abrirAsistente()
        compose.onNodeWithTag("btn_mic_assistant").assertIsDisplayed().performClick()
        assertEquals(1, escuchas)

        compose.onNodeWithTag("input_assistant_query").performTextInput("¿Qué me toca hoy?")
        compose.onNodeWithTag("btn_send_query").assertIsDisplayed()
    }

    // VZ09: mientras escucha se ve lo que el paciente va diciendo, y puede enviar o cancelar.
    @Test
    fun vz09_panelEscuchandoMuestraTextoYBotones() {
        abrirAsistente(estado = EstadoEscucha.ESCUCHANDO, parcial = "qué medicamentos me tocan")
        compose.onNodeWithTag("panel_escuchando").assertIsDisplayed()
        compose.onNodeWithText("qué medicamentos me tocan").assertIsDisplayed()
        compose.onNodeWithTag("btn_detener_escucha").performClick()
        assertEquals(1, envios)
        compose.onNodeWithTag("btn_cancelar_voz").performClick()
        assertTrue(cancelaciones >= 1)
    }

    // VZ10: el aviso de voz se muestra, y cada respuesta tiene su botón "Escuchar".
    @Test
    fun vz10_avisoVisibleYBotonEscuchar() {
        abrirAsistente(aviso = VozUtils.SIN_PERMISO)
        compose.onNodeWithTag("aviso_voz").assertIsDisplayed()
        compose.onNodeWithTag("btn_escuchar_m1").performClick()
        assertEquals(1, lecturas)
    }
}
