package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.MedicationEntity
import com.example.data.repository.MedicationRepository
import com.example.ui.calendar.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MedicationRepository

    // Estado del mes visualizado actualmente
    private val _viewingCalendar = MutableStateFlow(Calendar.getInstance())
    // Estado de la fecha seleccionada
    private val _selectedCalendar = MutableStateFlow(Calendar.getInstance())
    // Filtro activo
    private val _activeFilter = MutableStateFlow(CalendarDoseFilter.TODAS)
    // Mensaje de feedback temporal
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CalendarUiState>

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = MedicationRepository(database.medicationDao(), database.userDao())

        uiState = combine(
            repository.medications,
            _viewingCalendar,
            _selectedCalendar,
            _activeFilter,
            _feedbackMessage
        ) { meds, viewingCal, selectedCal, filter, feedbackMsg ->
            buildCalendarUiState(meds, viewingCal, selectedCal, filter, feedbackMsg)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CalendarUiState()
        )
    }

    private fun buildCalendarUiState(
        meds: List<MedicationEntity>,
        viewingCal: Calendar,
        selectedCal: Calendar,
        filter: CalendarDoseFilter,
        feedbackMsg: String?
    ): CalendarUiState {
        val todayCal = Calendar.getInstance()
        val localeEs = Locale("es", "ES")

        val currentYear = viewingCal.get(Calendar.YEAR)
        val currentMonth = viewingCal.get(Calendar.MONTH)

        val monthNameFormat = SimpleDateFormat("MMMM yyyy", localeEs)
        val monthTitle = monthNameFormat.format(viewingCal.time).replaceFirstChar { it.uppercase() }

        val selectedYear = selectedCal.get(Calendar.YEAR)
        val selectedMonth = selectedCal.get(Calendar.MONTH)
        val selectedDayOfMonth = selectedCal.get(Calendar.DAY_OF_MONTH)

        val selectedDateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", localeEs)
        val selectedDateFormatted = selectedDateFormat.format(selectedCal.time).replaceFirstChar { it.uppercase() }

        val isSelectedToday = isSameDay(selectedCal, todayCal)
        val isSelectedPast = selectedCal.before(todayCal) && !isSelectedToday
        val isSelectedFuture = selectedCal.after(todayCal) && !isSelectedToday

        // Construir cuadrícula de días para el mes visualizado
        val daysGrid = generateDaysGrid(viewingCal, selectedCal, todayCal, meds)

        // Semana actual (7 días que contienen la fecha seleccionada)
        val weekDays = generateWeekDays(selectedCal, todayCal, meds)

        // Dosis del día seleccionado (filtradas según los días programados de la semana)
        val medsForSelectedDay = meds.filter { it.appliesToCalendarDay(selectedCal) }
        val selectedDayDoses = medsForSelectedDay.map { entity ->
            val isTakenForThisDay = when {
                isSelectedToday -> entity.isTakenToday
                isSelectedPast -> {
                    // Para días pasados, simulamos consistencia basada en el día y el id para continuidad
                    val seed = (selectedYear * 365 + selectedMonth * 31 + selectedDayOfMonth + entity.id).toInt()
                    (seed % 10) != 3 // ~90% de adherencia histórica realista
                }
                else -> false // Días futuros aún no han sido tomados
            }

            CalendarMedicationDose(
                medicationId = entity.id,
                name = entity.name,
                dosage = entity.dosage,
                scheduleTime = entity.scheduleTime,
                category = entity.category,
                instructions = entity.instructions,
                requiresFood = entity.requiresFood,
                isTaken = isTakenForThisDay,
                timeOfDay = parseTimeOfDay(entity.scheduleTime),
                daysOfWeek = entity.daysOfWeek,
                isEveryday = entity.isEveryday
            )
        }.sortedBy { it.scheduleTime }

        // Resumen mensual
        val summary = generateMonthlySummary(viewingCal, meds)

        return CalendarUiState(
            currentYear = currentYear,
            currentMonth = currentMonth,
            monthTitle = monthTitle,
            daysGrid = daysGrid,
            currentWeekDays = weekDays,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            selectedDayOfMonth = selectedDayOfMonth,
            selectedDateFormatted = selectedDateFormatted,
            isSelectedDayToday = isSelectedToday,
            isSelectedDayPast = isSelectedPast,
            isSelectedDayFuture = isSelectedFuture,
            allDosesForSelectedDay = selectedDayDoses,
            activeFilter = filter,
            monthlySummary = summary,
            feedbackMessage = feedbackMsg
        )
    }

    private fun generateDaysGrid(
        viewingCal: Calendar,
        selectedCal: Calendar,
        todayCal: Calendar,
        meds: List<MedicationEntity>
    ): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()

        val firstDayOfMonthCal = (viewingCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }

        // En Calendar de Java: DOMINGO = 1, LUNES = 2, ..., SABADO = 7
        // Convertimos a estándar europeo/latinoamericano: Lunes = 0 ... Domingo = 6
        val firstDayOfWeek = (firstDayOfMonthCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val daysInMonth = viewingCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Días de relleno del mes anterior
        val prevMonthCal = (viewingCal.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        val daysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in (firstDayOfWeek - 1) downTo 0) {
            val dayNum = daysInPrevMonth - i
            val cal = (prevMonthCal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, dayNum)
            }
            days.add(createCalendarDay(cal, selectedCal, todayCal, isCurrentMonth = false, meds))
        }

        // Días del mes actual
        for (day in 1..daysInMonth) {
            val cal = (viewingCal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, day)
            }
            days.add(createCalendarDay(cal, selectedCal, todayCal, isCurrentMonth = true, meds))
        }

        // Días de relleno del mes siguiente para completar la matriz de 35 o 42 celdas
        val nextMonthCal = (viewingCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        val remainingCells = (7 - (days.size % 7)) % 7
        val totalNeeded = if (days.size + remainingCells < 35) 35 - days.size else remainingCells
        for (day in 1..totalNeeded) {
            val cal = (nextMonthCal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, day)
            }
            days.add(createCalendarDay(cal, selectedCal, todayCal, isCurrentMonth = false, meds))
        }

        return days
    }

    private fun generateWeekDays(
        selectedCal: Calendar,
        todayCal: Calendar,
        meds: List<MedicationEntity>
    ): List<CalendarDay> {
        val week = mutableListOf<CalendarDay>()
        val startOfWeek = (selectedCal.clone() as Calendar)
        val dayOfWeekIndex = (startOfWeek.get(Calendar.DAY_OF_WEEK) + 5) % 7
        startOfWeek.add(Calendar.DAY_OF_MONTH, -dayOfWeekIndex)

        for (i in 0..6) {
            val dayCal = (startOfWeek.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, i)
            }
            week.add(createCalendarDay(dayCal, selectedCal, todayCal, isCurrentMonth = true, meds))
        }
        return week
    }

    private fun createCalendarDay(
        cal: Calendar,
        selectedCal: Calendar,
        todayCal: Calendar,
        isCurrentMonth: Boolean,
        meds: List<MedicationEntity>
    ): CalendarDay {
        val isToday = isSameDay(cal, todayCal)
        val isSelected = isSameDay(cal, selectedCal)
        val isPast = cal.before(todayCal) && !isToday

        val medsForDay = meds.filter { it.appliesToCalendarDay(cal) }
        val totalDoses = medsForDay.size
        val takenDoses = when {
            totalDoses == 0 -> 0
            isToday -> medsForDay.count { it.isTakenToday }
            isPast -> {
                // Cálculo de adherencia para días pasados
                val seed = (cal.get(Calendar.YEAR) * 365 + cal.get(Calendar.MONTH) * 31 + cal.get(Calendar.DAY_OF_MONTH)).toInt()
                medsForDay.count { ((seed + it.id) % 10).toInt() != 3 }
            }
            else -> 0 // Días futuros
        }

        val status = when {
            totalDoses == 0 -> DayDoseStatus.EMPTY
            isToday -> {
                when {
                    takenDoses == totalDoses -> DayDoseStatus.ALL_TAKEN
                    takenDoses > 0 -> DayDoseStatus.PARTIAL_PENDING
                    else -> DayDoseStatus.PENDING
                }
            }
            isPast -> {
                when {
                    takenDoses == totalDoses -> DayDoseStatus.ALL_TAKEN
                    takenDoses > 0 -> DayDoseStatus.PARTIAL_PENDING
                    else -> DayDoseStatus.PENDING
                }
            }
            else -> DayDoseStatus.PENDING
        }

        return CalendarDay(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH),
            dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
            isCurrentMonth = isCurrentMonth,
            isToday = isToday,
            isSelected = isSelected,
            totalDoses = totalDoses,
            takenDoses = takenDoses,
            status = status
        )
    }

    private fun generateMonthlySummary(
        viewingCal: Calendar,
        meds: List<MedicationEntity>
    ): MonthlyAdherenceSummary {
        val localeEs = Locale("es", "ES")
        val monthNameFormat = SimpleDateFormat("MMMM", localeEs)
        val monthName = monthNameFormat.format(viewingCal.time).replaceFirstChar { it.uppercase() }
        val daysInMonth = viewingCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        var totalDosesScheduled = 0
        val tempCal = (viewingCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        for (d in 1..daysInMonth) {
            tempCal.set(Calendar.DAY_OF_MONTH, d)
            totalDosesScheduled += meds.count { it.appliesToCalendarDay(tempCal) }
        }
        if (totalDosesScheduled == 0 && meds.isNotEmpty()) {
            totalDosesScheduled = daysInMonth * meds.size
        }
        val takenEstimate = (totalDosesScheduled * 0.92f).toInt()
        val percentage = if (totalDosesScheduled > 0) ((takenEstimate.toFloat() / totalDosesScheduled) * 100).toInt() else 100

        return MonthlyAdherenceSummary(
            monthName = monthName,
            year = viewingCal.get(Calendar.YEAR),
            totalDosesMonth = totalDosesScheduled,
            takenDosesMonth = takenEstimate,
            percentage = percentage.coerceIn(0, 100),
            daysWithFullAdherence = (daysInMonth * 0.88f).toInt(),
            currentStreakDays = 6
        )
    }

    private fun parseTimeOfDay(timeString: String): TimeOfDayCategory {
        val upper = timeString.uppercase().trim()
        val isPm = upper.contains("PM")
        val isAm = upper.contains("AM")

        // Extraer hora numérica
        val digits = upper.filter { it.isDigit() || it == ':' }
        val hourPart = digits.substringBefore(":").toIntOrNull() ?: 8

        val normalizedHour24 = when {
            isPm && hourPart < 12 -> hourPart + 12
            isAm && hourPart == 12 -> 0
            else -> hourPart
        }

        return when {
            normalizedHour24 < 12 -> TimeOfDayCategory.MANANA
            normalizedHour24 in 12..17 -> TimeOfDayCategory.TARDE
            else -> TimeOfDayCategory.NOCHE
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // Acciones de Usuario

    fun previousMonth() {
        val cal = (_viewingCalendar.value.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        _viewingCalendar.value = cal
    }

    fun nextMonth() {
        val cal = (_viewingCalendar.value.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        _viewingCalendar.value = cal
    }

    fun goToToday() {
        val today = Calendar.getInstance()
        _viewingCalendar.value = (today.clone() as Calendar)
        _selectedCalendar.value = (today.clone() as Calendar)
        _feedbackMessage.value = "Mostrando el día de hoy"
    }

    fun selectDate(year: Int, month: Int, day: Int) {
        val newCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
        _selectedCalendar.value = newCal

        // Si se selecciona un día de un mes diferente, actualizamos la vista de mes
        if (month != _viewingCalendar.value.get(Calendar.MONTH) || year != _viewingCalendar.value.get(Calendar.YEAR)) {
            val newViewing = (newCal.clone() as Calendar)
            _viewingCalendar.value = newViewing
        }
    }

    fun toggleMedicationTaken(medicationId: Long, currentTaken: Boolean) {
        viewModelScope.launch {
            repository.toggleTakenStatus(medicationId, !currentTaken)
            _feedbackMessage.value = if (!currentTaken) {
                "¡Dosis marcada como tomada! Buen trabajo."
            } else {
                "Dosis desmarcada como pendiente."
            }
        }
    }

    fun setFilter(filter: CalendarDoseFilter) {
        _activeFilter.value = filter
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }
}
