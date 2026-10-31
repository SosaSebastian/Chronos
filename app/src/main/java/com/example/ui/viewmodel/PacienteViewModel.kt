package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ContactoEmergencia
import com.example.data.model.Medico
import com.example.data.model.Paciente
import com.example.data.model.Resource
import com.example.data.repository.FirebaseMedicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado reactivo del formulario de registro del paciente y su ficha médica.
 */
data class RegistroPacienteFormState(
    val email: String = "",
    val password: String = "",
    val nombre: String = "Sebastian Sosa",
    val edad: String = "73",
    val tipoSangre: String = "O+",
    val medicoNombre: String = "Dra. Sara Martinez",
    val medicoEspecialidad: String = "Cardióloga",
    val contactoNombre: String = "Isabela Sosa",
    val contactoParentesco: String = "Hija",
    val contactoTelefono: String = "+57 310 456 7890",
    val passwordVisible: Boolean = false,
    val tipoSangreExpanded: Boolean = false,
    val validationError: String? = null
)

/**
 * Estados de la UI durante el proceso de registro y guardado en Firebase.
 */
sealed interface RegistroPacienteUiState {
    object Idle : RegistroPacienteUiState
    object Loading : RegistroPacienteUiState
    data class Success(val paciente: Paciente) : RegistroPacienteUiState
    data class Error(val message: String) : RegistroPacienteUiState
}

/**
 * ViewModel encargado de la lógica de negocio y validación del formulario
 * de registro de usuario en Firebase Authentication y su ficha clínica en Cloud Firestore.
 */
class PacienteViewModel(
    private val repository: FirebaseMedicationRepository = FirebaseMedicationRepository()
) : ViewModel() {

    private val _formState = MutableStateFlow(RegistroPacienteFormState())
    val formState: StateFlow<RegistroPacienteFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<RegistroPacienteUiState>(RegistroPacienteUiState.Idle)
    val uiState: StateFlow<RegistroPacienteUiState> = _uiState.asStateFlow()

    // Opciones estándar para el selector de tipo de sangre
    val tiposDeSangre = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    fun updateEmail(value: String) {
        _formState.value = _formState.value.copy(email = value, validationError = null)
        clearErrorIfPresent()
    }

    fun updatePassword(value: String) {
        _formState.value = _formState.value.copy(password = value, validationError = null)
        clearErrorIfPresent()
    }

    fun togglePasswordVisibility() {
        _formState.value = _formState.value.copy(passwordVisible = !_formState.value.passwordVisible)
    }

    fun updateNombre(value: String) {
        _formState.value = _formState.value.copy(nombre = value, validationError = null)
        clearErrorIfPresent()
    }

    fun updateEdad(value: String) {
        val filtered = value.filter { it.isDigit() }
        _formState.value = _formState.value.copy(edad = filtered, validationError = null)
        clearErrorIfPresent()
    }

    fun updateTipoSangre(value: String) {
        _formState.value = _formState.value.copy(tipoSangre = value, tipoSangreExpanded = false, validationError = null)
        clearErrorIfPresent()
    }

    fun setTipoSangreExpanded(expanded: Boolean) {
        _formState.value = _formState.value.copy(tipoSangreExpanded = expanded)
    }

    fun updateMedicoNombre(value: String) {
        _formState.value = _formState.value.copy(medicoNombre = value, validationError = null)
        clearErrorIfPresent()
    }

    fun updateMedicoEspecialidad(value: String) {
        _formState.value = _formState.value.copy(medicoEspecialidad = value, validationError = null)
        clearErrorIfPresent()
    }

    fun updateContactoNombre(value: String) {
        _formState.value = _formState.value.copy(contactoNombre = value, validationError = null)
        clearErrorIfPresent()
    }

    fun updateContactoParentesco(value: String) {
        _formState.value = _formState.value.copy(contactoParentesco = value, validationError = null)
        clearErrorIfPresent()
    }

    fun updateContactoTelefono(value: String) {
        _formState.value = _formState.value.copy(contactoTelefono = value, validationError = null)
        clearErrorIfPresent()
    }

    private fun clearErrorIfPresent() {
        if (_uiState.value is RegistroPacienteUiState.Error) {
            _uiState.value = RegistroPacienteUiState.Idle
        }
    }

    /**
     * Valida los campos obligatorios del formulario.
     * @return null si todo es válido, o el mensaje de error correspondiente.
     */
    private fun validateForm(): String? {
        val s = _formState.value
        val email = s.email.trim()
        val pass = s.password.trim()

        if (email.isBlank()) return "El correo electrónico es obligatorio (*)."
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Ingresa un correo electrónico con formato válido (*)."
        }
        if (pass.isBlank()) return "La contraseña es obligatoria (*)."
        if (pass.length < 6) return "La contraseña debe tener al menos 6 caracteres (*)."
        if (s.nombre.trim().isBlank()) return "El nombre completo del adulto mayor es obligatorio (*)."
        val edadInt = s.edad.toIntOrNull()
        if (edadInt == null || edadInt <= 0 || edadInt > 120) {
            return "Ingresa una edad válida en años (*)."
        }
        if (s.tipoSangre.trim().isBlank()) return "Selecciona el tipo de sangre (*)."
        if (s.medicoNombre.trim().isBlank()) return "El nombre del médico tratante es obligatorio (*)."
        if (s.contactoNombre.trim().isBlank()) return "El nombre del contacto de emergencia es obligatorio (*)."
        if (s.contactoTelefono.trim().isBlank()) return "El teléfono de emergencia es obligatorio (*)."

        return null
    }

    /**
     * Dispara la autenticación en Firebase Auth y la creación del perfil en Firestore.
     */
    fun registrarYCrearPerfil(onSuccess: (Paciente) -> Unit = {}) {
        val validation = validateForm()
        if (validation != null) {
            _formState.value = _formState.value.copy(validationError = validation)
            _uiState.value = RegistroPacienteUiState.Error(validation)
            return
        }

        val s = _formState.value
        val paciente = Paciente(
            email = s.email.trim(),
            nombre = s.nombre.trim(),
            edad = s.edad.toInt(),
            tipoSangre = s.tipoSangre.trim(),
            medico = Medico(
                nombre = s.medicoNombre.trim(),
                especialidad = s.medicoEspecialidad.trim()
            ),
            contactoEmergencia = ContactoEmergencia(
                nombre = s.contactoNombre.trim(),
                parentesco = s.contactoParentesco.trim(),
                telefono = s.contactoTelefono.trim()
            )
        )

        _uiState.value = RegistroPacienteUiState.Loading

        viewModelScope.launch {
            val result = repository.registerUserAndCreateProfile(
                email = s.email.trim(),
                password = s.password.trim(),
                paciente = paciente
            )

            when (result) {
                is Resource.Success -> {
                    _uiState.value = RegistroPacienteUiState.Success(result.data)
                    onSuccess(result.data)
                }
                is Resource.Error -> {
                    _uiState.value = RegistroPacienteUiState.Error(result.message)
                }
                is Resource.Loading -> {
                    _uiState.value = RegistroPacienteUiState.Loading
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = RegistroPacienteUiState.Idle
        _formState.value = _formState.value.copy(validationError = null)
    }
}
