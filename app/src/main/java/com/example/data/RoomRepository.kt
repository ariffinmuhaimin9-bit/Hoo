package com.example.data

import com.example.model.GameRoom
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

class RoomRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Sila log masuk Google untuk menggunakan Bilik Rakan Awan.")
    }

    fun observeRoom(roomCode: String): Flow<GameRoom?> = flow {
        val cleanCode = roomCode.filter { it.isDigit() }.take(4)
        if (cleanCode.length != 4 || auth.currentUser?.uid.isNullOrBlank()) {
            emit(null)
            return@flow
        }
        val roomId = "room_$cleanCode"
        val path = "rooms/$roomId"
        emitAll(
            db.collection("rooms").document(roomId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(GameRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createRoom(
        roomCode: String,
        hostName: String,
        hostAvatar: String,
        gameId: String
    ): Result<GameRoom> {
        val cleanCode = roomCode.filter { it.isDigit() }.take(4)
        val roomId = "room_$cleanCode"
        val path = "rooms/$roomId"
        return try {
            val uid = requireUserId()
            require(cleanCode.length == 4) { "Kod bilik mesti 4 digit." }
            val room = GameRoom(
                id = roomId,
                roomCode = cleanCode,
                hostUid = uid,
                hostName = hostName.trim().ifBlank { "Host" }.take(30),
                hostAvatar = hostAvatar.ifBlank { "🦊" }.take(16),
                gameId = gameId,
                status = "waiting",
                lastEmote = "Jom mula!",
                lastEmoteSender = hostName.trim().ifBlank { "Host" }.take(30)
            )
            db.collection("rooms").document(roomId).set(room.toCreateMap()).await()
            Result.success(room)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinRoom(
        roomCode: String,
        guestName: String,
        guestAvatar: String
    ): Result<Unit> {
        val cleanCode = roomCode.filter { it.isDigit() }.take(4)
        val roomId = "room_$cleanCode"
        val path = "rooms/$roomId"
        return try {
            val uid = requireUserId()
            require(cleanCode.length == 4) { "Kod bilik mesti 4 digit." }
            val docRef = db.collection("rooms").document(roomId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("Bilik $cleanCode tidak dijumpai."))
            }
            val currentHost = snap.getString("hostUid")
            val existingGuest = snap.getString("guestUid")
            if (currentHost == uid || existingGuest == uid) {
                return Result.success(Unit)
            }
            if (!existingGuest.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Bilik $cleanCode sudah penuh!"))
            }
            docRef.update(
                mapOf(
                    "guestUid" to uid,
                    "guestName" to guestName.trim().ifBlank { "Rakan" }.take(30),
                    "guestAvatar" to guestAvatar.ifBlank { "🐼" }.take(16),
                    "guestReady" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleGuestReady(roomCode: String, ready: Boolean): Result<Unit> {
        val roomId = "room_${roomCode.filter { it.isDigit() }.take(4)}"
        val path = "rooms/$roomId"
        return try {
            requireUserId()
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "guestReady" to ready,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendQuickEmote(roomCode: String, emote: String, senderName: String): Result<Unit> {
        val roomId = "room_${roomCode.filter { it.isDigit() }.take(4)}"
        val path = "rooms/$roomId"
        return try {
            requireUserId()
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "lastEmote" to emote.take(60),
                    "lastEmoteSender" to senderName.trim().ifBlank { "Pemain" }.take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRoomStateByHost(roomCode: String, status: String, gameId: String? = null): Result<Unit> {
        val roomId = "room_${roomCode.filter { it.isDigit() }.take(4)}"
        val path = "rooms/$roomId"
        return try {
            requireUserId()
            val payload = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!gameId.isNullOrBlank()) payload["gameId"] = gameId.take(60)
            if (status == "waiting") payload["guestReady"] = false
            db.collection("rooms").document(roomId).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
