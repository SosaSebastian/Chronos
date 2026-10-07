package com.example.data.repository

import com.example.data.local.MedicationDao
import com.example.data.local.MedicationEntity
import com.example.data.local.UserDao
import com.example.data.local.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val userDao: UserDao
) {
    val medications: Flow<List<MedicationEntity>> = medicationDao.getAllMedications()
    val userProfile: Flow<UserProfileEntity?> = userDao.getUserProfile()

    suspend fun addMedication(medication: MedicationEntity): Long {
        return medicationDao.insertMedication(medication)
    }

    suspend fun updateMedication(medication: MedicationEntity) {
        medicationDao.updateMedication(medication)
    }

    suspend fun toggleTakenStatus(id: Long, isTaken: Boolean) {
        val timestamp = if (isTaken) System.currentTimeMillis() else null
        medicationDao.setTakenStatus(id, isTaken, timestamp)
    }

    suspend fun deleteMedication(id: Long) {
        medicationDao.deleteMedication(id)
    }

    suspend fun saveProfile(profile: UserProfileEntity) {
        userDao.saveUserProfile(profile)
    }

    suspend fun resetAllDailyTakens() {
        medicationDao.resetAllDailyTakens()
    }
}
