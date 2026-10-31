package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MintInteractive
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusUrgent

/**
 * Filtro de periodo de visualización de cumplimiento.
 */
enum class PeriodoFiltro(val titulo: String, val dias: Int) {
    SEMANA_7_DIAS("Última Semana", 7),
    QUINCENA_14_DIAS("Últimos 14 días", 14),
    MES_ACTUAL("Mes Actual", 30)
}

/**
 * Estado específico de la toma de un medicamento individual.
 */
enum class EstadoRegistroToma(val etiqueta: String, val color: Color) {
    A_TIEMPO("Tomada a tiempo", MintInteractive),
    RETRASADA("Tomada con retraso", StatusPending),
    OMITIDA("Omitida", StatusUrgent),
    PENDIENTE("Pendiente hoy", StatusPending)
}

/**
 * Detalle de la toma de un medicamento en un día específico.
 */
data class RegistroTomaDetalle(
    val id: String,
    val nombreMedicamento: String,
    val dosis: String,
    val horaProgramada: String,
    val horaTomada: String? = null,
    val categoria: String = "General",
    val estado: EstadoRegistroToma = EstadoRegistroToma.A_TIEMPO
)

/**
 * Representa el resumen de cumplimiento de un día individual.
 */
data class DiaCumplimiento(
    val id: String,
    val diaNombre: String,        // ej: "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Hoy"
    val fechaCorta: String,       // ej: "24 Oct"
    val fechaCompleta: String,    // ej: "Lunes 24 de Octubre"
    val porcentaje: Float,        // 0f .. 100f
    val dosisTomadas: Int,
    val dosisTotal: Int,
    val esHoy: Boolean = false,
    val detalles: List<RegistroTomaDetalle> = emptyList()
)

/**
 * Desglose de cumplimiento por categoría médica (Presión, Diabetes, etc.).
 */
data class CategoriaCumplimiento(
    val categoria: String,
    val porcentaje: Int,
    val dosisTomadas: Int,
    val dosisTotal: Int,
    val color: Color = MintInteractive
)

/**
 * Estado general de la pantalla de Historial de Cumplimiento.
 */
data class HistorialCumplimientoUiState(
    val isLoading: Boolean = false,
    val periodoSeleccionado: PeriodoFiltro = PeriodoFiltro.SEMANA_7_DIAS,
    val porcentajeGeneralSemana: Int = 88,
    val metaPorcentaje: Int = 80,
    val rachaDiasConsecutivos: Int = 5,
    val dosisTomadasTotal: Int = 25,
    val dosisPlanificadasTotal: Int = 28,
    val dosisOmitidasTotal: Int = 3,
    val diasHistorial: List<DiaCumplimiento> = emptyList(),
    val diaSeleccionado: DiaCumplimiento? = null,
    val cumplimientoPorCategoria: List<CategoriaCumplimiento> = emptyList(),
    val medicamentoMasConstante: String = "Losartán 50 mg (100%)",
    val medicamentoAtencion: String? = "Metformina (75% - Requiere atención)",
    val consejoGeriátrico: String = "Excelente adherencia esta semana. Mantener el horario matutino de la presión arterial reduce significativamente riesgos cardiovasculares.",
    val feedbackMensaje: String? = null,
    val isGeneratingPdf: Boolean = false,
    val pdfGeneradoRuta: String? = null
)

