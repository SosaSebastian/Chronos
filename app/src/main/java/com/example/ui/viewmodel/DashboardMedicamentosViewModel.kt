package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.MedicationEntity
import com.example.data.model.Medicamento
import com.example.data.model.Resource
import com.example.data.repository.FirebaseMedicationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Modelo de UI que unifica los medicamentos para la pantalla del adulto mayor,
 * tanto si provienen de Cloud Firestore como de la base de datos local Room.
 */
data class MedicamentoUiModel(
    val id: String,
    val localId: Long = 0L,
    val nombre: String,
    val dosis: String,
    val horaToma: String,
    val categoria: String,
    val indicaciones: String,
    val activo: Boolean = true,
    val isTakenToday: Boolean = false,
    val proximaTomaTexto: String = "",
    val estadoUrgencia: EstadoToma = EstadoToma.NORMAL
)

enum class EstadoToma {
    NORMAL,
    PROXIMO,
    TOMADO
}

/**
 * Filtros de estado de toma para el adulto mayor.
 */
enum class FiltroEstadoToma(val label: String) {
    TODOS("Todos"),
    PENDIENTES("Pendientes"),
    TOMADOS("Tomados")
}

/**
 * Estado general de la pantalla DashboardMedicamentos.
 */
data class DashboardMedicamentosUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val categoriaSeleccionada: String = "Todas",
    val filtroEstado: FiltroEstadoToma = FiltroEstadoToma.TODOS,
    val categoriasDisponibles: List<String> = listOf("Todas", "Presión", "Diabetes", "Corazón", "Vitaminas"),
    val totalMedicamentos: Int = 0,
    val tomadosHoy: Int = 0,
    val pendientesHoy: Int = 0,
    val medicamentosFiltrados: List<MedicamentoUiModel> = emptyList(),
    val errorMessage: String? = null,
    val feedbackMessage: String? = null
)

/**
 * ViewModel que gestiona la lista filtrable de medicamentos, el cálculo
 * del horario de la próxima toma y la sincronización con Firestore y Room.
 */
class DashboardMedicamentosViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "DashboardMedViewModel"
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseRepo: FirebaseMedicationRepository = FirebaseMedicationRepository()
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val medicationDao = database.medicationDao()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoria = MutableStateFlow("Todas")
    private val _selectedFiltroEstado = MutableStateFlow(FiltroEstadoToma.TODOS)
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // Lista en memoria de medicamentos sincronizados (Firestore + Room)
    private val _rawMedicamentos = MutableStateFlow<List<MedicamentoUiModel>>(emptyList())

    private data class FilterResult(
        val searchQuery: String,
        val categoriaSeleccionada: String,
        val filtroEstado: FiltroEstadoToma,
        val categoriasDisponibles: List<String>,
        val total: Int,
        val tomados: Int,
        val pendientes: Int,
        val filtrados: List<MedicamentoUiModel>
    )

    private val _filterFlow = combine(
        _rawMedicamentos,
        _searchQuery,
        _selectedCategoria,
        _selectedFiltroEstado
    ) { rawList, query, cat, filtroEstado ->
        // Calcular categorías dinámicas
        val uniqueCategories = mutableSetOf("Todas", "Presión", "Diabetes", "Corazón", "Vitaminas")
        rawList.forEach { if (it.categoria.isNotBlank()) uniqueCategories.add(it.categoria) }

        // Filtrar por texto de búsqueda
        val queryClean = query.trim().lowercase(Locale.getDefault())
        val filteredBySearch = if (queryClean.isBlank()) {
            rawList
        } else {
            rawList.filter {
                it.nombre.lowercase(Locale.getDefault()).contains(queryClean) ||
                it.categoria.lowercase(Locale.getDefault()).contains(queryClean) ||
                it.indicaciones.lowercase(Locale.getDefault()).contains(queryClean) ||
                it.dosis.lowercase(Locale.getDefault()).contains(queryClean)
            }
        }

        // Filtrar por categoría
        val filteredByCategory = if (cat == "Todas") {
            filteredBySearch
        } else {
            filteredBySearch.filter { it.categoria.equals(cat, ignoreCase = true) }
        }

        // Filtrar por estado de toma
        val filteredByStatus = when (filtroEstado) {
            FiltroEstadoToma.TODOS -> filteredByCategory
            FiltroEstadoToma.PENDIENTES -> filteredByCategory.filter { !it.isTakenToday && it.activo }
            FiltroEstadoToma.TOMADOS -> filteredByCategory.filter { it.isTakenToday }
        }

        val total = rawList.size
        val tomados = rawList.count { it.isTakenToday }
        val pendientes = rawList.count { !it.isTakenToday && it.activo }

        FilterResult(
            searchQuery = query,
            categoriaSeleccionada = cat,
            filtroEstado = filtroEstado,
            categoriasDisponibles = uniqueCategories.toList(),
            total = total,
            tomados = tomados,
            pendientes = pendientes,
            filtrados = filteredByStatus
        )
    }

    val uiState: StateFlow<DashboardMedicamentosUiState> = combine(
        _filterFlow,
        _isLoading,
        _errorMessage,
        _feedbackMessage
    ) { filterResult, loading, error, feedback ->
        DashboardMedicamentosUiState(
            isLoading = loading,
            searchQuery = filterResult.searchQuery,
            categoriaSeleccionada = filterResult.categoriaSeleccionada,
            filtroEstado = filterResult.filtroEstado,
            categoriasDisponibles = filterResult.categoriasDisponibles,
            totalMedicamentos = filterResult.total,
            tomadosHoy = filterResult.tomados,
            pendientesHoy = filterResult.pendientes,
            medicamentosFiltrados = filterResult.filtrados,
            errorMessage = error,
            feedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardMedicamentosUiState(isLoading = true)
    )

    init {
        cargarMedicamentos()
        observarMedicamentosLocales()
        observarMedicamentosFirestore()
    }

    /**
     * Carga inicial y observación continua de la base de datos local Room.
     */
    private fun observarMedicamentosLocales() {
        viewModelScope.launch {
            medicationDao.getAllMedications().collect { localEntities ->
                // Si Firestore está vacío o sin conexión, se usan las entidades locales
                val currentUid = auth.currentUser?.uid
                if (currentUid.isNullOrBlank() || _rawMedicamentos.value.isEmpty()) {
                    val converted = localEntities.map { entity ->
                        entityToUiModel(entity)
                    }
                    if (converted.isNotEmpty() || _rawMedicamentos.value.isEmpty()) {
                        _rawMedicamentos.value = converted
                    }
                }
            }
        }
    }

    /**
     * Observa en tiempo real los medicamentos en Cloud Firestore users/{userId}/medicamentos
     */
    private fun observarMedicamentosFirestore() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                firebaseRepo.observeActiveMedicamentos(uid).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            if (_rawMedicamentos.value.isEmpty()) {
                                _isLoading.value = true
                            }
                        }
                        is Resource.Success -> {
                            _isLoading.value = false
                            val firestoreList = resource.data
                            if (firestoreList.isNotEmpty()) {
                                _rawMedicamentos.value = firestoreList.map { med ->
                                    firestoreToUiModel(med)
                                }
                            }
                        }
                        is Resource.Error -> {
                            _isLoading.value = false
                            Log.w(TAG, "Observación de Firestore: ${resource.message}")
                        }
                    }
                }
            }
        }
    }

    fun cargarMedicamentos() {
        viewModelScope.launch {
            _isLoading.value = true
            val uid = auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                when (val result = firebaseRepo.getActiveMedicamentos(uid)) {
                    is Resource.Success -> {
                        if (result.data.isNotEmpty()) {
                            _rawMedicamentos.value = result.data.map { firestoreToUiModel(it) }
                        }
                    }
                    is Resource.Error -> {
                        Log.w(TAG, "Error cargando de Firestore: ${result.message}")
                    }
                    else -> {}
                }
            }
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategoria(categoria: String) {
        _selectedCategoria.value = categoria
    }

    fun selectFiltroEstado(filtro: FiltroEstadoToma) {
        _selectedFiltroEstado.value = filtro
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Marca o desmarca un medicamento como tomado hoy.
     */
    fun toggleMedicationTaken(medication: MedicamentoUiModel) {
        val newTakenState = !medication.isTakenToday
        viewModelScope.launch {
            // Actualización optimista en memoria
            _rawMedicamentos.value = _rawMedicamentos.value.map { item ->
                if (item.id == medication.id) {
                    item.copy(
                        isTakenToday = newTakenState,
                        proximaTomaTexto = calcularTextoProximaToma(item.horaToma, newTakenState)
                    )
                } else item
            }

            // Actualizar en Room si existe ID local
            if (medication.localId != 0L) {
                medicationDao.setTakenStatus(medication.localId, newTakenState, System.currentTimeMillis())
            }

            // Feedback audible/visual
            _feedbackMessage.value = if (newTakenState) {
                "¡Excelente! Tomaste ${medication.nombre} (${medication.dosis})."
            } else {
                "Marcaste ${medication.nombre} como pendiente."
            }
        }
    }

    /**
     * Elimina un medicamento tanto en Firestore como en Room.
     */
    fun deleteMedication(medication: MedicamentoUiModel) {
        viewModelScope.launch {
            _rawMedicamentos.value = _rawMedicamentos.value.filter { it.id != medication.id }

            // Eliminar en Firestore
            val uid = auth.currentUser?.uid
            if (!uid.isNullOrBlank() && medication.id.isNotBlank()) {
                firebaseRepo.deleteMedicamento(uid, medication.id)
            }

            // Eliminar en Room
            if (medication.localId != 0L) {
                medicationDao.deleteMedication(medication.localId)
            }

            _feedbackMessage.value = "Se eliminó ${medication.nombre} de tus recordatorios."
        }
    }

    /**
     * Convierte un Medicamento de Firestore a MedicamentoUiModel.
     */
    private fun firestoreToUiModel(med: Medicamento): MedicamentoUiModel {
        return MedicamentoUiModel(
            id = med.id,
            localId = 0L,
            nombre = med.nombre,
            dosis = med.dosis,
            horaToma = med.horaToma,
            categoria = if (med.categoria.isNotBlank()) med.categoria else "General",
            indicaciones = med.indicaciones,
            activo = med.activo,
            isTakenToday = false,
            proximaTomaTexto = calcularTextoProximaToma(med.horaToma, false),
            estadoUrgencia = calcularEstadoUrgencia(med.horaToma, false)
        )
    }

    /**
     * Convierte una entidad de Room a MedicamentoUiModel.
     */
    private fun entityToUiModel(entity: MedicationEntity): MedicamentoUiModel {
        return MedicamentoUiModel(
            id = entity.id.toString(),
            localId = entity.id,
            nombre = entity.name,
            dosis = entity.dosage,
            horaToma = entity.scheduleTime,
            categoria = entity.category,
            indicaciones = entity.instructions,
            activo = true,
            isTakenToday = entity.isTakenToday,
            proximaTomaTexto = calcularTextoProximaToma(entity.scheduleTime, entity.isTakenToday),
            estadoUrgencia = calcularEstadoUrgencia(entity.scheduleTime, entity.isTakenToday)
        )
    }

    /**
     * Calcula una etiqueta amigable y comprensible para el horario de la próxima toma,
     * adaptada con cariño para adultos mayores.
     */
    private fun calcularTextoProximaToma(horaToma: String, isTakenToday: Boolean): String {
        if (isTakenToday) {
            return "Tomado hoy ✓ • Próxima toma mañana a las $horaToma"
        }
        if (horaToma.isBlank()) {
            return "Horario no especificado"
        }

        return try {
            val format12 = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            val format24 = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = try {
                format12.parse(horaToma)
            } catch (e: Exception) {
                format24.parse(horaToma)
            }

            if (date != null) {
                val calHora = Calendar.getInstance().apply { time = date }
                val calNow = Calendar.getInstance()

                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, calHora.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, calHora.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                }

                if (calNow.before(targetCal)) {
                    val diffMillis = targetCal.timeInMillis - calNow.timeInMillis
                    val hours = (diffMillis / (1000 * 60 * 60)).toInt()
                    val minutes = ((diffMillis / (1000 * 60)) % 60).toInt()

                    when {
                        hours == 0 && minutes <= 30 -> "¡Toca pronto! En $minutes min ($horaToma)"
                        hours == 0 -> "Hoy en $minutes min ($horaToma)"
                        hours == 1 -> "Hoy en 1 hora y $minutes min ($horaToma)"
                        else -> "Hoy a las $horaToma (en $hours horas)"
                    }
                } else {
                    "Hoy a las $horaToma (programada)"
                }
            } else {
                "Hoy a las $horaToma"
            }
        } catch (e: Exception) {
            "Hoy a las $horaToma"
        }
    }

    private fun calcularEstadoUrgencia(horaToma: String, isTakenToday: Boolean): EstadoToma {
        if (isTakenToday) return EstadoToma.TOMADO
        // Podríamos comprobar si está dentro de la próxima hora
        return EstadoToma.NORMAL
    }
}
