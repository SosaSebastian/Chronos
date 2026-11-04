package com.example.ui.calendar

import java.util.Calendar

/**
 * Estado general de las tomas de un día en el calendario.
 */
enum class DayDoseStatus {
    ALL_TAKEN,       // Todas las dosis tomadas (Verde menta)
    PARTIAL_PENDING, // Algunas tomadas, otras pendientes (Ámbar)
    PENDING,         // Programadas pero ninguna tomada aún (Amarillo / Pendiente)
    EMPTY            // Sin medicamentos programados para este día
}

/**
 * Jornada del día para agrupar tomas cronológicamente.
 */
enum class TimeOfDayCategory(val label: String, val rangeLabel: String) {
    MANANA("Mañana", "06:00 AM - 11:59 AM"),
    TARDE("Tarde", "12:00 PM - 05:59 PM"),
    NOCHE("Noche", "06:00 PM - 11:59 PM")
}

/**
 * Filtro de visualización de dosis en el día seleccionado.
 */
enum class CalendarDoseFilter(val label: String) {
    TODAS("Todas"),
    PENDIENTES("Pendientes"),
    TOMADAS("Completadas")
}

/**
 * Celda individual de día en la cuadrícula del calendario.
 */
data class CalendarDay(
    val year: Int,
    val month: Int, // 0 = Enero, 11 = Diciembre
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val totalDoses: Int,
    val takenDoses: Int,
    val status: DayDoseStatus
)

/**
 * Dosis de medicamento programada para una fecha específica.
 */
data class CalendarMedicationDose(
    val medicationId: Long,
    val name: String,
    val dosage: String,
    val scheduleTime: String,
    val category: String,
    val instructions: String,
    val requiresFood: Boolean,
    val isTaken: Boolean,
    val timeOfDay: TimeOfDayCategory
)

/**
 * Resumen mensual de adherencia terapéutica para el paciente.
 */
data class MonthlyAdherenceSummary(
    val monthName: String,
    val year: Int,
    val totalDosesMonth: Int,
    val takenDosesMonth: Int,
    val percentage: Int,
    val daysWithFullAdherence: Int,
    val currentStreakDays: Int
)

/**
 * Estado de la UI para la pantalla del Calendario de Medicaciones.
 */
data class CalendarUiState(
    val currentYear: Int = 2026,
    val currentMonth: Int = Calendar.NOVEMBER,
    val monthTitle: String = "Noviembre 2026",
    val daysGrid: List<CalendarDay> = emptyList(),
    val currentWeekDays: List<CalendarDay> = emptyList(),
    val selectedYear: Int = 2026,
    val selectedMonth: Int = Calendar.NOVEMBER,
    val selectedDayOfMonth: Int = 2,
    val selectedDateFormatted: String = "Lunes, 2 de Noviembre de 2026",
    val isSelectedDayToday: Boolean = true,
    val isSelectedDayPast: Boolean = false,
    val isSelectedDayFuture: Boolean = false,
    val allDosesForSelectedDay: List<CalendarMedicationDose> = emptyList(),
    val activeFilter: CalendarDoseFilter = CalendarDoseFilter.TODAS,
    val monthlySummary: MonthlyAdherenceSummary? = null,
    val feedbackMessage: String? = null
) {
    val filteredDoses: List<CalendarMedicationDose>
        get() = when (activeFilter) {
            CalendarDoseFilter.TODAS -> allDosesForSelectedDay
            CalendarDoseFilter.PENDIENTES -> allDosesForSelectedDay.filter { !it.isTaken }
            CalendarDoseFilter.TOMADAS -> allDosesForSelectedDay.filter { it.isTaken }
        }

    val dosesByTimeOfDay: Map<TimeOfDayCategory, List<CalendarMedicationDose>>
        get() = filteredDoses.groupBy { it.timeOfDay }

    val totalDosesCount: Int get() = allDosesForSelectedDay.size
    val takenDosesCount: Int get() = allDosesForSelectedDay.count { it.isTaken }
    val pendingDosesCount: Int get() = allDosesForSelectedDay.count { !it.isTaken }
    val adherencePercentToday: Int
        get() = if (totalDosesCount > 0) ((takenDosesCount.toFloat() / totalDosesCount) * 100).toInt() else 100
}
