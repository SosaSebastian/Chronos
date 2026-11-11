package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,
    val scheduleTime: String,
    val frequencyHours: Int = 24,
    val instructions: String = "",
    val category: String = "General",
    val isTakenToday: Boolean = false,
    val lastTakenTimestamp: Long? = null,
    val requiresFood: Boolean = true,
    val daysOfWeek: String = "ALL",
    val isEveryday: Boolean = true
) {
    /**
     * Determina si la dosis de este medicamento está programada para la fecha dada.
     */
    fun appliesToCalendarDay(cal: Calendar): Boolean {
        if (isEveryday || daysOfWeek.isBlank() || daysOfWeek.equals("ALL", ignoreCase = true) || daysOfWeek.equals("Todos los días", ignoreCase = true)) {
            return true
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dayName = when (dayOfWeek) {
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            Calendar.SUNDAY -> "Domingo"
            else -> ""
        }
        return daysOfWeek.contains(dayName, ignoreCase = true) ||
                (dayOfWeek == Calendar.MONDAY && daysOfWeek.contains("Lun", true)) ||
                (dayOfWeek == Calendar.TUESDAY && daysOfWeek.contains("Mar", true)) ||
                (dayOfWeek == Calendar.WEDNESDAY && (daysOfWeek.contains("Mié", true) || daysOfWeek.contains("Mie", true))) ||
                (dayOfWeek == Calendar.THURSDAY && daysOfWeek.contains("Jue", true)) ||
                (dayOfWeek == Calendar.FRIDAY && daysOfWeek.contains("Vie", true)) ||
                (dayOfWeek == Calendar.SATURDAY && (daysOfWeek.contains("Sáb", true) || daysOfWeek.contains("Sab", true))) ||
                (dayOfWeek == Calendar.SUNDAY && daysOfWeek.contains("Dom", true))
    }
}

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "Sr Sebastian Sosa",
    val age: Int = 73,
    val doctorName: String = "Dra. Sara Martinez (Cardióloga)",
    val emergencyContactName: String = "Hija Isabela Sosa",
    val emergencyContactPhone: String = "+57 310 456 7890",
    val bloodType: String = "O+",
    val highContrastEnabled: Boolean = true,
    val voiceEnabled: Boolean = true
)
