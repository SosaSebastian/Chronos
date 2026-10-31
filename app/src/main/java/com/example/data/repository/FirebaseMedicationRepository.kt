package com.example.data.repository

import android.util.Log
import com.example.auth.awaitTask
import com.example.data.model.FirebaseErrorMapper
import com.example.data.model.Medicamento
import com.example.data.model.Paciente
import com.example.data.model.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio de producción para persistencia de datos en Firebase
 * (Firebase Authentication + Cloud Firestore NoSQL).
 */
class FirebaseMedicationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "FirebaseMedicationRepo"
        private const val COLLECTION_USERS = "users"
        private const val SUBCOLLECTION_MEDICAMENTOS = "medicamentos"
    }

    /**
     * UID del usuario autenticado actualmente.
     */
    val currentUserId: String?
        get() = auth.currentUser?.uid

    // =========================================================================
    // 1. REGISTRO DE USUARIO Y CREACIÓN DE PERFIL ATÓMICA/SECUENCIAL
    // =========================================================================

    /**
     * Registra un nuevo adulto mayor en Firebase Authentication y genera
     * de forma secuencial su documento correspondiente en `users/{userId}`.
     *
     * @param email Correo electrónico para acceso.
     * @param password Contraseña de acceso (mínimo 6 caracteres).
     * @param paciente Objeto de dominio con los datos del paciente.
     * @return [Resource.Success] con el [Paciente] sincronizado o [Resource.Error].
     */
    suspend fun registerUserAndCreateProfile(
        email: String,
        password: String,
        paciente: Paciente
    ): Resource<Paciente> {
        return try {
            val cleanEmail = email.trim()
            val cleanPass = password.trim()

            if (cleanEmail.isBlank() || cleanPass.isBlank()) {
                return Resource.Error("Por favor ingresa un correo y contraseña válidos.")
            }
            if (cleanPass.length < 6) {
                return Resource.Error("La contraseña debe tener un mínimo de 6 caracteres.")
            }

            // Paso 1: Crear usuario en Firebase Authentication
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, cleanPass).awaitTask()
            val firebaseUser = authResult.user
                ?: throw IllegalStateException("Firebase Authentication no retornó un usuario válido.")

            val userId = firebaseUser.uid

            // Actualizar DisplayName en Auth para consistencia
            if (paciente.nombre.isNotBlank()) {
                try {
                    val profileChange = UserProfileChangeRequest.Builder()
                        .setDisplayName(paciente.nombre.trim())
                        .build()
                    firebaseUser.updateProfile(profileChange).awaitTask()
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo actualizar el displayName en Auth: ${e.message}")
                }
            }

            // Paso 2: Crear el documento en Firestore `users/{userId}`
            val finalPaciente = paciente.copy(
                uid = userId,
                email = cleanEmail
            )

            val userDocRef = firestore.collection(COLLECTION_USERS).document(userId)
            
            // Usamos un mapa para garantizar FieldValue.serverTimestamp() en creadoEn
            val userPayload = hashMapOf<String, Any>(
                "uid" to finalPaciente.uid,
                "email" to finalPaciente.email,
                "nombre" to finalPaciente.nombre,
                "edad" to finalPaciente.edad,
                "tipoSangre" to finalPaciente.tipoSangre,
                "medico" to hashMapOf(
                    "nombre" to finalPaciente.medico.nombre,
                    "especialidad" to finalPaciente.medico.especialidad
                ),
                "contactoEmergencia" to hashMapOf(
                    "nombre" to finalPaciente.contactoEmergencia.nombre,
                    "parentesco" to finalPaciente.contactoEmergencia.parentesco,
                    "telefono" to finalPaciente.contactoEmergencia.telefono
                ),
                "creadoEn" to FieldValue.serverTimestamp()
            )

            userDocRef.set(userPayload).awaitTask()
            Log.d(TAG, "Perfil de paciente creado con éxito en users/$userId")

            Resource.Success(finalPaciente)
        } catch (e: Exception) {
            Log.e(TAG, "Error en registerUserAndCreateProfile: ${e.message}", e)
            Resource.Error(FirebaseErrorMapper.map(e), e)
        }
    }

    /**
     * Obtiene el perfil del paciente desde `users/{userId}`.
     */
    suspend fun getPacienteProfile(userId: String): Resource<Paciente> {
        return try {
            val snapshot = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .awaitTask()

            if (!snapshot.exists()) {
                return Resource.Error("No se encontró el perfil del paciente.")
            }

            val paciente = snapshot.toObject(Paciente::class.java)
                ?: return Resource.Error("Error al procesar la información del paciente.")

            Resource.Success(paciente)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener perfil de paciente: ${e.message}", e)
            Resource.Error(FirebaseErrorMapper.map(e), e)
        }
    }

    // =========================================================================
    // 2. GESTIÓN DE MEDICAMENTOS (SUBCOLECCIÓN users/{userId}/medicamentos)
    // =========================================================================

    /**
     * Agrega un nuevo medicamento en la subcolección `users/{userId}/medicamentos`.
     * El ID del documento es generado automáticamente por Firestore.
     *
     * @param userId Identificador UID del usuario.
     * @param medicamento Datos del medicamento ingresados en el formulario.
     * @return [Resource.Success] con el ID autogenerado del medicamento.
     */
    suspend fun addMedicamento(userId: String, medicamento: Medicamento): Resource<String> {
        return try {
            if (userId.isBlank()) {
                return Resource.Error("Sesión inválida: el ID de usuario no puede estar vacío.")
            }
            if (medicamento.nombre.isBlank()) {
                return Resource.Error("Por favor ingresa el nombre del medicamento.")
            }
            if (medicamento.dosis.isBlank()) {
                return Resource.Error("Por favor especifica la dosis (ej. 50 mg).")
            }

            val medicamentosCollection = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_MEDICAMENTOS)

            val docRef = medicamentosCollection.document()
            val generatedId = docRef.id

            val payload = hashMapOf<String, Any>(
                "id" to generatedId,
                "nombre" to medicamento.nombre.trim(),
                "dosis" to medicamento.dosis.trim(),
                "horaToma" to medicamento.horaToma.trim(),
                "categoria" to medicamento.categoria.trim(),
                "indicaciones" to medicamento.indicaciones.trim(),
                "activo" to medicamento.activo,
                "creadoEn" to FieldValue.serverTimestamp()
            )

            docRef.set(payload).awaitTask()
            Log.d(TAG, "Medicamento agregado con éxito: $generatedId para el usuario $userId")

            Resource.Success(generatedId)
        } catch (e: Exception) {
            Log.e(TAG, "Error al agregar medicamento: ${e.message}", e)
            Resource.Error(FirebaseErrorMapper.map(e), e)
        }
    }

    /**
     * Consulta una sola vez la lista de medicamentos activos del paciente,
     * ordenados por hora de toma.
     *
     * @param userId Identificador UID del usuario.
     */
    suspend fun getActiveMedicamentos(userId: String): Resource<List<Medicamento>> {
        return try {
            if (userId.isBlank()) {
                return Resource.Error("Sesión inválida: el ID de usuario no puede estar vacío.")
            }

            val querySnapshot = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_MEDICAMENTOS)
                .whereEqualTo("activo", true)
                .get()
                .awaitTask()

            val lista = querySnapshot.toObjects(Medicamento::class.java)
                .sortedBy { it.horaToma }

            Resource.Success(lista)
        } catch (e: Exception) {
            Log.e(TAG, "Error al consultar medicamentos activos: ${e.message}", e)
            Resource.Error(FirebaseErrorMapper.map(e), e)
        }
    }

    /**
     * Observa en tiempo real mediante un Flow reactivo los medicamentos activos
     * de la subcolección `users/{userId}/medicamentos`.
     *
     * @param userId Identificador UID del paciente.
     */
    fun observeActiveMedicamentos(userId: String): Flow<Resource<List<Medicamento>>> = callbackFlow {
        trySend(Resource.Loading)

        if (userId.isBlank()) {
            trySend(Resource.Error("ID de usuario no autenticado."))
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_MEDICAMENTOS)
            .whereEqualTo("activo", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error en snapshot de medicamentos: ${error.message}", error)
                    trySend(Resource.Error(FirebaseErrorMapper.map(error), error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val medicamentos = snapshot.toObjects(Medicamento::class.java)
                        .sortedBy { it.horaToma }
                    trySend(Resource.Success(medicamentos))
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Actualiza el estado activo/inactivo de un medicamento.
     */
    suspend fun toggleMedicamentoStatus(
        userId: String,
        medicamentoId: String,
        activo: Boolean
    ): Resource<Unit> {
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_MEDICAMENTOS)
                .document(medicamentoId)
                .update("activo", activo)
                .awaitTask()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al cambiar estado del medicamento: ${e.message}", e)
            Resource.Error(FirebaseErrorMapper.map(e), e)
        }
    }

    /**
     * Elimina físicamente un medicamento de la subcolección en Firestore.
     */
    suspend fun deleteMedicamento(userId: String, medicamentoId: String): Resource<Unit> {
        return try {
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_MEDICAMENTOS)
                .document(medicamentoId)
                .delete()
                .awaitTask()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar medicamento: ${e.message}", e)
            Resource.Error(FirebaseErrorMapper.map(e), e)
        }
    }
}
