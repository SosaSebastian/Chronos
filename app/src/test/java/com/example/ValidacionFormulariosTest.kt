package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.PacienteViewModel
import com.example.ui.viewmodel.RegistrarMedicamentoUiState
import com.example.ui.viewmodel.RegistrarMedicamentoViewModel
import com.example.ui.viewmodel.RegistroPacienteUiState
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Validación de formularios en los ViewModels — checklist 3.5 y 5.3, y CP01 del paper.
 * Ninguna de estas pruebas llega a la red: todas se detienen en la validación o en la falta de sesión.
 *
 * Ejecutar: ./gradlew testDebugUnitTest --tests "com.example.ValidacionFormulariosTest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ValidacionFormulariosTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Los ViewModels piden FirebaseAuth y Firestore al crearse; se inicia un Firebase de prueba sin red. */
    private var appDePrueba: FirebaseApp? = null

    @Before
    fun iniciarFirebaseDePrueba() {
        if (FirebaseApp.getApps(context).isEmpty()) {
            appDePrueba = FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:000000000000:android:0000000000000000")
                    .setProjectId("healthy-chronos-test")
                    .setApiKey("AIzaSyA-pruebas-unitarias-sin-red-0000000")
                    .build()
            )
        }
    }

    @After
    fun cerrarFirebaseDePrueba() {
        // Solo se borra la app de prueba creada aquí, para no afectar otras pruebas.
        appDePrueba?.delete()
        appDePrueba = null
    }

    // ───────────────────────── Registrar medicamento ─────────────────────────

    private fun errorDeMedicamento(vm: RegistrarMedicamentoViewModel): String {
        vm.guardarMedicamento()
        val estado = vm.uiState.value
        assertTrue("Se esperaba un error de validación y llegó $estado", estado is RegistrarMedicamentoUiState.Error)
        return (estado as RegistrarMedicamentoUiState.Error).message
    }

    // VF01: el nombre del medicamento es obligatorio.
    @Test
    fun vf01_medicamentoSinNombre() {
        val vm = RegistrarMedicamentoViewModel()
        vm.updateNombre("   ")
        assertEquals("El nombre del medicamento es obligatorio (*).", errorDeMedicamento(vm))
    }

    // VF02 (CP01): una dosis negativa se rechaza con un mensaje y la app sigue funcionando.
    @Test
    fun vf02_dosisNegativaSeRechaza() {
        val vm = RegistrarMedicamentoViewModel()
        vm.updateNombre("Losartán")
        vm.updateDosis("-5 mg")
        assertEquals("La dosis debe ser mayor que cero (*).", errorDeMedicamento(vm))
        assertEquals("La dosis debe ser mayor que cero (*).", vm.formState.value.validationError)
    }

    // VF03: una dosis sin cantidad se rechaza.
    @Test
    fun vf03_dosisSinCantidad() {
        val vm = RegistrarMedicamentoViewModel()
        vm.updateNombre("Losartán")
        vm.updateDosis("mg")
        assertEquals("La dosis debe incluir una cantidad, por ejemplo 50 mg (*).", errorDeMedicamento(vm))
    }

    // VF04: al corregir el campo, el error desaparece.
    @Test
    fun vf04_elErrorSeLimpiaAlCorregir() {
        val vm = RegistrarMedicamentoViewModel()
        vm.updateNombre("")
        vm.guardarMedicamento()
        assertTrue(vm.uiState.value is RegistrarMedicamentoUiState.Error)

        vm.updateNombre("Losartán")
        assertEquals(RegistrarMedicamentoUiState.Idle, vm.uiState.value)
        assertEquals(null, vm.formState.value.validationError)
    }

    // VF05: un formulario válido sin sesión iniciada no se guarda y avisa, sin cerrar la app.
    @Test
    fun vf05_formularioValidoSinSesionNoSeGuarda() {
        val vm = RegistrarMedicamentoViewModel()
        vm.updateNombre("Losartán")
        vm.updateDosis("50 mg")
        assertEquals("No hay una sesión activa de usuario.", errorDeMedicamento(vm))
    }

    // ───────────────────────── Editar medicamento (CRUD: actualizar) ─────────────────────────

    // VF10: al editar, el formulario se llena con los datos del medicamento y queda en modo edición.
    @Test
    fun vf10_cargarParaEditarLlenaElFormulario() {
        val vm = RegistrarMedicamentoViewModel()
        vm.cargarParaEditar("med-123", "Losartán", "50 mg", "08:00 AM", "Presión", "Después del desayuno")

        assertTrue(vm.enModoEdicion)
        assertEquals("med-123", vm.medicamentoEnEdicionId.value)
        assertEquals("Losartán", vm.formState.value.nombre)
        assertEquals("50 mg", vm.formState.value.dosis)
        assertEquals("08:00 AM", vm.formState.value.horaToma)
        assertEquals("Losartán", vm.datosOriginales?.nombre)
    }

    // VF11: la edición también valida los datos (no se puede dejar una dosis negativa).
    @Test
    fun vf11_editarConDosisInvalidaSeRechaza() {
        val vm = RegistrarMedicamentoViewModel()
        vm.cargarParaEditar("med-123", "Losartán", "50 mg", "08:00 AM", "Presión", "")
        vm.updateDosis("-50 mg")
        assertEquals("La dosis debe ser mayor que cero (*).", errorDeMedicamento(vm))
        assertTrue("Sigue en modo edición tras el error", vm.enModoEdicion)
    }

    // VF12: "nuevo registro" sale del modo edición y limpia el formulario, aunque antes se haya guardado algo.
    @Test
    fun vf12_nuevoRegistroSaleDelModoEdicion() {
        val vm = RegistrarMedicamentoViewModel()
        vm.cargarParaEditar("med-123", "Losartán", "50 mg", "08:00 AM", "Presión", "")
        vm.nuevoRegistro()

        assertFalse(vm.enModoEdicion)
        assertEquals(null, vm.datosOriginales)
        assertEquals("", vm.formState.value.nombre)
        assertEquals(RegistrarMedicamentoUiState.Idle, vm.uiState.value)
    }

    // ───────────────────────── Registro del paciente ─────────────────────────

    private fun pacienteValido(): PacienteViewModel = PacienteViewModel().apply {
        updateEmail("maria.gomez@correo.com")
        updatePassword("clave123")
        updateNombre("María Gómez")
        updateEdad("72")
        updateTipoSangre("O+")
        updateMedicoNombre("Dra. Sara Martínez")
        updateContactoNombre("Isabela Gómez")
        updateContactoTelefono("+57 310 000 0000")
    }

    private fun errorDePaciente(vm: PacienteViewModel): String {
        vm.registrarYCrearPerfil()
        val estado = vm.uiState.value
        assertTrue("Se esperaba un error de validación y llegó $estado", estado is RegistroPacienteUiState.Error)
        return (estado as RegistroPacienteUiState.Error).message
    }

    // VF06: el correo debe tener formato válido.
    @Test
    fun vf06_correoConFormatoInvalido() {
        val vm = pacienteValido().apply { updateEmail("maria.correo.com") }
        assertEquals("Ingresa un correo electrónico con formato válido (*).", errorDePaciente(vm))
    }

    // VF07: la contraseña debe tener al menos 6 caracteres.
    @Test
    fun vf07_contrasenaCorta() {
        val vm = pacienteValido().apply { updatePassword("123") }
        assertEquals("La contraseña debe tener al menos 6 caracteres (*).", errorDePaciente(vm))
    }

    // VF08: la edad debe estar entre 1 y 120 años; las letras y el signo menos se descartan al escribir.
    @Test
    fun vf08_edadFueraDeRango() {
        val vm = pacienteValido().apply { updateEdad("150") }
        assertEquals("Ingresa una edad válida en años (*).", errorDePaciente(vm))

        vm.updateEdad("-7a")
        assertEquals("El campo edad solo acepta dígitos", "7", vm.formState.value.edad)
    }

    // VF09: el teléfono de emergencia es obligatorio.
    @Test
    fun vf09_telefonoDeEmergenciaObligatorio() {
        val vm = pacienteValido().apply { updateContactoTelefono("") }
        assertEquals("El teléfono de emergencia es obligatorio (*).", errorDePaciente(vm))
    }
}
