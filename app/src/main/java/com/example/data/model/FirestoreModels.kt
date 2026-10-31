package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Modelo de contacto de emergencia para el adulto mayor.
 */
data class ContactoEmergencia(
    val nombre: String = "",
    val parentesco: String = "",
    val telefono: String = ""
)

/**
 * Modelo del médico tratante del paciente.
 */
data class Medico(
    val nombre: String = "",
    val especialidad: String = ""
)

/**
 * Modelo del perfil del adulto mayor almacenado en:
 * Colección: `users/{userId}`
 */
data class Paciente(
    val uid: String = "",
    val email: String = "",
    val nombre: String = "",
    val edad: Int = 0,
    val tipoSangre: String = "",
    val medico: Medico = Medico(),
    val contactoEmergencia: ContactoEmergencia = ContactoEmergencia(),
    @ServerTimestamp
    val creadoEn: Date? = null
)

/**
 * Modelo de medicamento almacenado en la subcolección:
 * Subcolección: `users/{userId}/medicamentos/{medicamentoId}`
 */
data class Medicamento(
    @DocumentId
    val id: String = "",
    val nombre: String = "",
    val dosis: String = "",
    val horaToma: String = "",
    val categoria: String = "",
    val indicaciones: String = "",
    val activo: Boolean = true,
    @ServerTimestamp
    val creadoEn: Date? = null
)
