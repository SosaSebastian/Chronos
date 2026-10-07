package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val requiresFood: Boolean = true
)

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
