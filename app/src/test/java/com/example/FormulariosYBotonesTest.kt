package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.HealtyChronosTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pruebas de interfaz — checklist 5.3: "Los botones, formularios y componentes funcionan correctamente".
 * Se prueban sobre la pantalla de inicio de sesión real (LoginScreen) con Compose Testing + Robolectric.
 *
 * Ejecutar: ./gradlew testDebugUnitTest --tests "com.example.FormulariosYBotonesTest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class FormulariosYBotonesTest {

    @get:Rule
    val compose = createComposeRule()

    // Lo que la pantalla le entrega a la app cuando el usuario toca los botones.
    private var credencialesLogin: Pair<String, String>? = null
    private var datosRegistro: Triple<String, String, String>? = null
    private var entradasDirectas = 0

    private fun abrirLogin(errorServidor: String? = null, cargando: Boolean = false) {
        compose.setContent {
            HealtyChronosTheme(darkTheme = true) {
                LoginScreen(
                    onLoginSuccess = { entradasDirectas++ },
                    onFirebaseSignIn = { email, pass -> credencialesLogin = email to pass },
                    onFirebaseSignUp = { email, pass, nombre -> datosRegistro = Triple(email, pass, nombre) },
                    errorMessage = errorServidor,
                    isLoading = cargando
                )
            }
        }
    }

    // UI01: con el formulario vacío, "Iniciar Sesión" muestra un aviso y no intenta entrar.
    @Test
    fun ui01_loginVacioMuestraAvisoYNoEnvia() {
        abrirLogin()

        compose.onNodeWithTag("login_button").performScrollTo().performClick()

        compose.onNodeWithText("Por favor ingresa tu correo y contraseña.").assertExists()
        assertNull("No debe llamar a Firebase con campos vacíos", credencialesLogin)
    }

    // UI02: con correo y contraseña, el botón envía exactamente lo que escribió el usuario.
    @Test
    fun ui02_loginConDatosEnviaCredenciales() {
        abrirLogin()

        compose.onNodeWithTag("email_input").performTextInput("maria.gomez@correo.com")
        compose.onNodeWithTag("password_input").performTextInput("clave123")
        compose.onNodeWithTag("login_button").performScrollTo().performClick()

        assertEquals("maria.gomez@correo.com" to "clave123", credencialesLogin)
    }

    // UI03: la pestaña "Crear Cuenta" cambia el formulario y valida la contraseña de mínimo 6 caracteres.
    @Test
    fun ui03_crearCuentaValidaLongitudDeContrasena() {
        abrirLogin()

        compose.onNodeWithTag("tab_sign_up").performClick()
        compose.onNodeWithTag("signup_email_input").performScrollTo().performTextInput("maria.gomez@correo.com")
        compose.onNodeWithTag("signup_password_input").performScrollTo().performTextInput("123")
        compose.onNodeWithTag("signup_button").performScrollTo().performClick()

        compose.onNodeWithText("La contraseña debe tener al menos 6 caracteres.").assertExists()
        assertNull("No debe crear la cuenta con una contraseña corta", datosRegistro)
    }

    // UI04: con datos válidos, "Registrarme en Firebase" envía nombre, correo y contraseña.
    @Test
    fun ui04_crearCuentaConDatosValidosEnviaRegistro() {
        abrirLogin()

        compose.onNodeWithTag("tab_sign_up").performClick()
        compose.onNodeWithTag("signup_name_input").performScrollTo().performTextClearance()
        compose.onNodeWithTag("signup_name_input").performTextInput("María Gómez")
        compose.onNodeWithTag("signup_email_input").performScrollTo().performTextInput("maria.gomez@correo.com")
        compose.onNodeWithTag("signup_password_input").performScrollTo().performTextInput("clave123")
        compose.onNodeWithTag("signup_button").performScrollTo().performClick()

        assertEquals(Triple("maria.gomez@correo.com", "clave123", "María Gómez"), datosRegistro)
    }

    // UI05: el botón de acceso asistido entra a la app sin pedir credenciales.
    @Test
    fun ui05_botonDeAccesoAsistidoEntraALaApp() {
        abrirLogin()

        compose.onNodeWithTag("demo_access_button").performScrollTo().performClick()

        assertEquals(1, entradasDirectas)
    }

    // UI06: el botón de emergencia está visible y es accesible para lectores de pantalla.
    @Test
    fun ui06_botonDeEmergenciaVisibleYAccesible() {
        abrirLogin()

        compose.onNodeWithTag("login_emergency_call_btn").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Llamar emergencia").assertExists()
    }

    // UI07: si Firebase responde con error, la pantalla lo muestra al usuario.
    @Test
    fun ui07_errorDelServidorSeMuestraEnPantalla() {
        abrirLogin(errorServidor = "El correo o la contraseña ingresados son incorrectos.")

        compose.onNodeWithText("El correo o la contraseña ingresados son incorrectos.").assertExists()
    }

    // UI08: mientras valida con Firebase, el botón se desactiva para evitar envíos dobles.
    @Test
    fun ui08_botonSeDesactivaMientrasCarga() {
        abrirLogin(cargando = true)

        compose.onNodeWithTag("login_button").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Verificando con Firebase...").assertExists()
        assertTrue(credencialesLogin == null)
    }
}
