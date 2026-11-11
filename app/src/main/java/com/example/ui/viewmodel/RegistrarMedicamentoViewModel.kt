package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Medicamento
import com.example.data.model.Resource
import com.example.data.repository.FirebaseMedicationRepository
import com.example.util.DosisInvalidaException
import com.example.util.ValidadorDosis
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado del formulario de registro de dosis y horario de medicamentos.
 */
data class RegistrarMedicamentoFormState(
    val nombre: String = "",
    val dosis: String = "50 mg",
    val horaToma: String = "08:00 AM",
    val categoria: String = "Presión",
    val indicaciones: String = "Tomar con un vaso con agua después del desayuno",
    val activo: Boolean = true,
    val isEveryday: Boolean = true,
    val selectedDays: Set<String> = setOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"),
    val validationError: String? = null
) {
    val diasSemanaFormateados: String
        get() = if (isEveryday || selectedDays.size == 7) "ALL" else selectedDays.joinToString(",")
}

/**
 * Estados de la interfaz gráfica durante el guardado en Firestore.
 */
sealed interface RegistrarMedicamentoUiState {
    object Idle : RegistrarMedicamentoUiState
    object Loading : RegistrarMedicamentoUiState
    data class Success(val medicamentoId: String) : RegistrarMedicamentoUiState
    data class Error(val message: String) : RegistrarMedicamentoUiState
}

/**
 * ViewModel encargado del registro de medicamentos en la subcolección:
 * `users/{userId}/medicamentos`
 */
class RegistrarMedicamentoViewModel(
    private val repository: FirebaseMedicationRepository = FirebaseMedicationRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _formState = MutableStateFlow(RegistrarMedicamentoFormState())
    val formState: StateFlow<RegistrarMedicamentoFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<RegistrarMedicamentoUiState>(RegistrarMedicamentoUiState.Idle)
    val uiState: StateFlow<RegistrarMedicamentoUiState> = _uiState.asStateFlow()

    // Opciones rápidas de medicamentos frecuentes
    val medicamentosFrecuentes = listOf("Metformina", "Enalapril", "Calcio D3", "Aspirina")

    // Opciones rápidas de horarios frecuentes de toma
    val horariosFrecuentes = listOf("07:00 AM", "08:00 AM", "01:00 PM", "08:00 PM", "10:00 PM")

    // Días de la semana para selección personalizada
    val diasSemanaList = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    // Opciones de categorías médicas
    val categoriasMedicas = listOf("Presión", "Diabetes", "Corazón", "Vitaminas")

    // ── Modo edición ──────────────────────────────────────────────────────────
    // Id de Firestore del medicamento que se está editando; null = registro nuevo.
    private val _medicamentoEnEdicionId = MutableStateFlow<String?>(null)
    val medicamentoEnEdicionId: StateFlow<String?> = _medicamentoEnEdicionId.asStateFlow()

    /** Datos del medicamento antes de editarlo; sirven para ubicar su copia local y su alarma. */
    var datosOriginales: RegistrarMedicamentoFormState? = null
        private set

    val enModoEdicion: Boolean
        get() = _medicamentoEnEdicionId.value != null

    /** Abre el formulario vacío para registrar un medicamento nuevo. */
    fun nuevoRegistro() {
        _medicamentoEnEdicionId.value = null
        datosOriginales = null
        _formState.value = RegistrarMedicamentoFormState()
        _uiState.value = RegistrarMedicamentoUiState.Idle
    }

    /** Llena el formulario con un medicamento existente para editarlo. */
    fun cargarParaEditar(
        id: String,
        nombre: String,
        dosis: String,
        horaToma: String,
        categoria: String,
        indicaciones: String,
        diasSemana: String = "ALL",
        esTodosLosDias: Boolean = true
    ) {
        val isAll = esTodosLosDias || diasSemana.isBlank() || diasSemana.equals("ALL", ignoreCase = true) || diasSemana.equals("Todos los días", ignoreCase = true)
        val parsedDays = if (isAll) {
            setOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        } else {
            diasSemana.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        }
        val datos = RegistrarMedicamentoFormState(
            nombre = nombre,
            dosis = dosis,
            horaToma = horaToma,
            categoria = categoria,
            indicaciones = indicaciones,
            isEveryday = isAll,
            selectedDays = if (parsedDays.isEmpty()) setOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo") else parsedDays
        )
        _medicamentoEnEdicionId.value = id
        datosOriginales = datos
        _formState.value = datos
        _uiState.value = RegistrarMedicamentoUiState.Idle
    }

    fun updateNombre(value: String) {
        _formState.value = _formState.value.copy(nombre = value, validationError = null)
        clearErrorIfPresent()
    }

    fun selectMedicamentoFrecuente(nombre: String) {
        val defaultDosis = when (nombre) {
            "Metformina" -> "850 mg"
            "Enalapril" -> "20 mg"
            "Calcio D3" -> "600 mg / 400 UI"
            "Aspirina" -> "100 mg"
            else -> "50 mg"
        }
        val defaultCat = when (nombre) {
            "Metformina" -> "Diabetes"
            "Enalapril" -> "Presión"
            "Calcio D3" -> "Vitaminas"
            "Aspirina" -> "Corazón"
            else -> "Presión"
        }
        _formState.value = _formState.value.copy(
            nombre = nombre,
            dosis = defaultDosis,
            categoria = defaultCat,
            validationError = null
        )
        clearErrorIfPresent()
    }

    fun updateDosis(value: String) {
        _formState.value = _formState.value.copy(dosis = value, validationError = null)
        clearErrorIfPresent()
    }

    fun selectHoraToma(hora: String) {
        _formState.value = _formState.value.copy(horaToma = hora, validationError = null)
        clearErrorIfPresent()
    }

    fun setHoraEspecifica(hora: String) {
        _formState.value = _formState.value.copy(horaToma = hora, validationError = null)
        clearErrorIfPresent()
    }

    fun setEsTodosLosDias(esTodos: Boolean) {
        _formState.value = _formState.value.copy(
            isEveryday = esTodos,
            selectedDays = if (esTodos) setOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo") else _formState.value.selectedDays,
            validationError = null
        )
        clearErrorIfPresent()
    }

    fun toggleDiaSemana(dia: String) {
        val currentDays = if (_formState.value.isEveryday) {
            setOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo").toMutableSet().apply {
                remove(dia)
            }
        } else {
            _formState.value.selectedDays.toMutableSet().apply {
                if (contains(dia)) remove(dia) else add(dia)
            }
        }

        val allSelected = currentDays.size == 7
        _formState.value = _formState.value.copy(
            isEveryday = allSelected,
            selectedDays = currentDays,
            validationError = null
        )
        clearErrorIfPresent()
    }

    fun selectDiasLaborables() {
        _formState.value = _formState.value.copy(
            isEveryday = false,
            selectedDays = setOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes"),
            validationError = null
        )
        clearErrorIfPresent()
    }

    fun selectFinDeSemana() {
        _formState.value = _formState.value.copy(
            isEveryday = false,
            selectedDays = setOf("Sábado", "Domingo"),
            validationError = null
        )
        clearErrorIfPresent()
    }

    fun selectCategoria(categoria: String) {
        _formState.value = _formState.value.copy(categoria = categoria, validationError = null)
        clearErrorIfPresent()
    }

    fun updateIndicaciones(value: String) {
        _formState.value = _formState.value.copy(indicaciones = value)
    }

    private fun clearErrorIfPresent() {
        if (_uiState.value is RegistrarMedicamentoUiState.Error) {
            _uiState.value = RegistrarMedicamentoUiState.Idle
        }
    }

    /**
     * Valida los campos obligatorios marcados con asterisco (*).
     */
    private fun validateForm(): String? {
        val s = _formState.value
        if (s.nombre.trim().isBlank()) return "El nombre del medicamento es obligatorio (*)."
        try {
            ValidadorDosis.validar(s.dosis)
        } catch (e: DosisInvalidaException) {
            return e.message
        }
        if (s.horaToma.trim().isBlank()) return "La hora de la toma es obligatoria (*)."
        if (!s.isEveryday && s.selectedDays.isEmpty()) return "Selecciona al menos un día de la semana para la toma (*)."
        if (s.categoria.trim().isBlank()) return "Selecciona una categoría médica (*)."
        return null
    }

    /**
     * Guarda el medicamento en Cloud Firestore en la subcolección `users/{userId}/medicamentos`.
     */
    fun guardarMedicamento(onSuccess: (String) -> Unit = {}) {
        val validation = validateForm()
        if (validation != null) {
            _formState.value = _formState.value.copy(validationError = validation)
            _uiState.value = RegistrarMedicamentoUiState.Error(validation)
            return
        }

        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            _uiState.value = RegistrarMedicamentoUiState.Error("No hay una sesión activa de usuario.")
            return
        }

        val s = _formState.value
        val medicamento = Medicamento(
            nombre = s.nombre.trim(),
            dosis = s.dosis.trim(),
            horaToma = s.horaToma.trim(),
            categoria = s.categoria.trim(),
            indicaciones = s.indicaciones.trim(),
            activo = s.activo,
            diasSemana = s.diasSemanaFormateados,
            esTodosLosDias = s.isEveryday || s.selectedDays.size == 7
        )

        _uiState.value = RegistrarMedicamentoUiState.Loading

        val idEdicion = _medicamentoEnEdicionId.value
        if (idEdicion != null) {
            viewModelScope.launch {
                when (val result = repository.updateMedicamento(userId, medicamento.copy(id = idEdicion))) {
                    is Resource.Success -> {
                        _uiState.value = RegistrarMedicamentoUiState.Success(idEdicion)
                        onSuccess(idEdicion)
                    }
                    is Resource.Error -> _uiState.value = RegistrarMedicamentoUiState.Error(result.message)
                    is Resource.Loading -> _uiState.value = RegistrarMedicamentoUiState.Loading
                }
            }
            return
        }

        viewModelScope.launch {
            val result = repository.addMedicamento(userId, medicamento)
            when (result) {
                is Resource.Success -> {
                    _uiState.value = RegistrarMedicamentoUiState.Success(result.data)
                    onSuccess(result.data)
                }
                is Resource.Error -> {
                    _uiState.value = RegistrarMedicamentoUiState.Error(result.message)
                }
                is Resource.Loading -> {
                    _uiState.value = RegistrarMedicamentoUiState.Loading
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = RegistrarMedicamentoUiState.Idle
        _formState.value = _formState.value.copy(validationError = null)
    }
}
