package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [MedicationEntity::class, UserProfileEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "healthy_chronos_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.medicationDao(), database.userDao())
                    }
                }
            }

            suspend fun populateInitialData(medicationDao: MedicationDao, userDao: UserDao) {
                userDao.saveUserProfile(
                    UserProfileEntity(
                        id = 1,
                        fullName = "Sr. Sebastian Sosa",
                        age = 73,
                        doctorName = "Dra. Sara Martinez (Cardióloga)",
                        emergencyContactName = "Isabela Sosa (Hija)",
                        emergencyContactPhone = "+57 310 456 7890",
                        bloodType = "O+",
                        highContrastEnabled = true,
                        voiceEnabled = true
                    )
                )

                medicationDao.insertMedication(
                    MedicationEntity(
                        name = "Losartán Potásico",
                        dosage = "50 mg",
                        scheduleTime = "08:00 AM",
                        frequencyHours = 12,
                        instructions = "Tomar después del desayuno con abundante agua",
                        category = "Presión Arterial",
                        isTakenToday = false,
                        requiresFood = true
                    )
                )

                medicationDao.insertMedication(
                    MedicationEntity(
                        name = "Omeprazol",
                        dosage = "20 mg",
                        scheduleTime = "07:30 AM",
                        frequencyHours = 24,
                        instructions = "Tomar en ayunas 30 minutos antes del desayuno",
                        category = "Protector Gástrico",
                        isTakenToday = true,
                        requiresFood = false
                    )
                )

                medicationDao.insertMedication(
                    MedicationEntity(
                        name = "Atorvastatina",
                        dosage = "20 mg",
                        scheduleTime = "09:00 PM",
                        frequencyHours = 24,
                        instructions = "Tomar en la noche con agua. Evitar consumo con jugo de pomelo",
                        category = "Colesterol",
                        isTakenToday = false,
                        requiresFood = false
                    )
                )
            }
        }
    }
}
