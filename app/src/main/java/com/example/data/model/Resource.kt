package com.example.data.model

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Estado genérico para operaciones asíncronas con Firebase.
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
}

/**
 * Utilidad centralizada para mapear excepciones de Firebase a mensajes amigables en español.
 */
object FirebaseErrorMapper {
    fun map(throwable: Throwable): String {
        return when (throwable) {
            is FirebaseNetworkException ->
                "Sin conexión a internet. Por favor verifica tu red y reintenta."
            is FirebaseAuthWeakPasswordException ->
                "La contraseña es muy débil. Debe tener al menos 6 caracteres."
            is FirebaseAuthInvalidCredentialsException ->
                "El correo o la contraseña ingresados son incorrectos."
            is FirebaseAuthUserCollisionException ->
                "Ya existe una cuenta registrada con este correo electrónico."
            is FirebaseAuthException ->
                when (throwable.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> "No se encontró ningún usuario con ese correo."
                    "ERROR_WRONG_PASSWORD" -> "Contraseña incorrecta."
                    "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos fallidos. Intenta más tarde."
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Error de red al conectar con los servidores de autenticación."
                    else -> throwable.localizedMessage ?: "Error de autenticación: ${throwable.errorCode}"
                }
            is FirebaseFirestoreException ->
                when (throwable.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        "Permisos denegados. No tienes autorización para consultar o modificar estos datos."
                    FirebaseFirestoreException.Code.UNAVAILABLE ->
                        "El servicio de base de datos no está disponible temporalmente. Modo sin conexión."
                    FirebaseFirestoreException.Code.NOT_FOUND ->
                        "El registro solicitado no fue encontrado en la nube."
                    FirebaseFirestoreException.Code.ALREADY_EXISTS ->
                        "El documento que intentas crear ya existe en Firestore."
                    else -> throwable.localizedMessage ?: "Error en la base de datos Firestore (${throwable.code})."
                }
            else -> throwable.localizedMessage ?: "Ocurrió un error inesperado al procesar la solicitud."
        }
    }
}
