package com.example.auth

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) continuation.resumeWithException(exception)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}

class FirebaseAuthManager {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signIn(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val cleanEmail = email.trim()
            val cleanPass = pass.trim()
            if (cleanEmail.isBlank() || cleanPass.isBlank()) {
                return Result.failure(IllegalArgumentException("Por favor completa el correo y la contraseña."))
            }

            val result = auth.signInWithEmailAndPassword(cleanEmail, cleanPass).awaitTask()
            val user = result.user ?: throw IllegalStateException("No se pudo obtener el usuario.")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Error en signIn: ${e.message}", e)
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    suspend fun signUp(email: String, pass: String, displayName: String): Result<FirebaseUser> {
        return try {
            val cleanEmail = email.trim()
            val cleanPass = pass.trim()
            if (cleanEmail.isBlank() || cleanPass.isBlank()) {
                return Result.failure(IllegalArgumentException("Por favor completa el correo y la contraseña."))
            }
            if (cleanPass.length < 6) {
                return Result.failure(IllegalArgumentException("La contraseña debe tener al menos 6 caracteres."))
            }

            val result = auth.createUserWithEmailAndPassword(cleanEmail, cleanPass).awaitTask()
            val user = result.user ?: throw IllegalStateException("Error al crear el usuario en Firebase.")

            if (displayName.isNotBlank()) {
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName.trim())
                        .build()
                    user.updateProfile(profileUpdates).awaitTask()
                } catch (pe: Exception) {
                    Log.w("FirebaseAuthManager", "No se pudo actualizar el nombre de perfil: ${pe.message}")
                }
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Error en signUp: ${e.message}", e)
            Result.failure(Exception(mapAuthError(e)))
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Error en signOut: ${e.message}")
        }
    }

    private fun mapAuthError(e: Throwable): String {
        return when (e) {
            is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil. Debe tener mínimo 6 caracteres."
            is FirebaseAuthInvalidCredentialsException -> "Credenciales inválidas o correo electrónico mal estructurado."
            is FirebaseAuthUserCollisionException -> "Este correo electrónico ya está registrado. Por favor inicia sesión."
            is FirebaseAuthException -> {
                when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "El formato del correo electrónico no es válido."
                    "ERROR_WRONG_PASSWORD" -> "Contraseña incorrecta. Por favor inténtalo de nuevo."
                    "ERROR_USER_NOT_FOUND" -> "No existe ninguna cuenta registrada con este correo."
                    "ERROR_USER_DISABLED" -> "Esta cuenta de usuario ha sido inhabilitada."
                    "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos fallidos. Intenta más tarde."
                    "ERROR_OPERATION_NOT_ALLOWED" -> "El proveedor de correo/contraseña no está habilitado en Firebase Console."
                    else -> e.localizedMessage ?: "Error de autenticación con Firebase."
                }
            }
            else -> e.localizedMessage ?: "Ocurrió un error al conectar con Firebase."
        }
    }
}
