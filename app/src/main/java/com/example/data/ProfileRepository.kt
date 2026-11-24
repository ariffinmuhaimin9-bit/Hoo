package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.UserProfile
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

private val Context.guestDataStore: DataStore<Preferences> by preferencesDataStore(name = "miniverse_guest_profile")

class CloudProfileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Sila log masuk dengan Google terlebih dahulu.")
    }

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureProfileExists(defaultName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val initial = UserProfile(
                    userId = uid,
                    displayName = defaultName.trim().ifBlank { "Pemain Google" }.take(30)
                )
                docRef.set(initial.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomProfile(
        displayName: String,
        avatarEmoji: String,
        avatarColorHex: String,
        bio: String,
        favoriteBadge: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val path = "users/$uid"
        return try {
            require(displayName.trim().length in 1..30) { "Nama paparan mesti antara 1 hingga 30 aksara." }
            require(bio.trim().length <= 140) { "Bio maksimum 140 aksara." }
            val docRef = db.collection("users").document(uid)
            val updates = UserProfile(
                displayName = displayName,
                avatarEmoji = avatarEmoji,
                avatarColorHex = avatarColorHex,
                bio = bio,
                favoriteBadge = favoriteBadge
            ).toProfileUpdateMap()
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(gameId: String, currentFavorites: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val path = "users/$uid"
        return try {
            val updated = if (currentFavorites.contains(gameId)) {
                currentFavorites - gameId
            } else {
                (currentFavorites + gameId).distinct().take(150)
            }
            db.collection("users").document(uid).update(
                mapOf(
                    "favoriteGameIds" to updated,
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

    suspend fun recordGameScore(pointsEarned: Long): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val path = "users/$uid"
        return try {
            val safePoints = pointsEarned.coerceAtLeast(0L)
            db.collection("users").document(uid).update(
                mapOf(
                    "totalScore" to FieldValue.increment(safePoints),
                    "gamesPlayed" to FieldValue.increment(1L),
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
}

class LocalGuestRepository(context: Context) {
    private val dataStore = context.applicationContext.guestDataStore

    private object Keys {
        val IS_GUEST_ACTIVE = booleanPreferencesKey("is_guest_active")
        val DISPLAY_NAME = stringPreferencesKey("guest_display_name")
        val AVATAR_EMOJI = stringPreferencesKey("guest_avatar_emoji")
        val AVATAR_COLOR = stringPreferencesKey("guest_avatar_color")
        val BIO = stringPreferencesKey("guest_bio")
        val BADGE = stringPreferencesKey("guest_badge")
        val TOTAL_SCORE = longPreferencesKey("guest_total_score")
        val GAMES_PLAYED = longPreferencesKey("guest_games_played")
        val FAVORITES = stringSetPreferencesKey("guest_favorites")
        val DARK_MODE = booleanPreferencesKey("pref_dark_mode")
        val MUTED = booleanPreferencesKey("pref_muted")
    }

    val isGuestActive: Flow<Boolean> = dataStore.data.map { it[Keys.IS_GUEST_ACTIVE] ?: false }
    val darkModeFlow: Flow<Boolean> = dataStore.data.map { it[Keys.DARK_MODE] ?: true }
    val mutedFlow: Flow<Boolean> = dataStore.data.map { it[Keys.MUTED] ?: false }

    val guestProfileFlow: Flow<UserProfile> = dataStore.data.map { prefs ->
        UserProfile(
            id = "guest_local",
            userId = "guest_local",
            displayName = prefs[Keys.DISPLAY_NAME] ?: "Tetamu Ceria",
            avatarEmoji = prefs[Keys.AVATAR_EMOJI] ?: "🦊",
            avatarColorHex = prefs[Keys.AVATAR_COLOR] ?: "#38BDF8",
            bio = prefs[Keys.BIO] ?: "Bermain dalam Mod Tetamu di peranti ini!",
            favoriteBadge = prefs[Keys.BADGE] ?: "Pemain Tetamu",
            totalScore = prefs[Keys.TOTAL_SCORE] ?: 0L,
            gamesPlayed = prefs[Keys.GAMES_PLAYED] ?: 0L,
            favoriteGameIds = (prefs[Keys.FAVORITES] ?: emptySet()).toList()
        )
    }

    suspend fun setGuestActive(active: Boolean, initialName: String? = null) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_GUEST_ACTIVE] = active
            if (!initialName.isNullOrBlank()) {
                prefs[Keys.DISPLAY_NAME] = initialName.trim().take(30)
            }
        }
    }

    suspend fun updateGuestProfile(
        displayName: String,
        avatarEmoji: String,
        avatarColorHex: String,
        bio: String,
        favoriteBadge: String
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = displayName.trim().ifBlank { "Tetamu Ceria" }.take(30)
            prefs[Keys.AVATAR_EMOJI] = avatarEmoji.ifBlank { "🦊" }.take(16)
            prefs[Keys.AVATAR_COLOR] = avatarColorHex.ifBlank { "#38BDF8" }.take(12)
            prefs[Keys.BIO] = bio.trim().take(140)
            prefs[Keys.BADGE] = favoriteBadge.trim().ifBlank { "Pemain Tetamu" }.take(40)
        }
    }

    suspend fun toggleGuestFavorite(gameId: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES] ?: emptySet()
            prefs[Keys.FAVORITES] = if (current.contains(gameId)) current - gameId else current + gameId
        }
    }

    suspend fun recordGuestScore(pointsEarned: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.TOTAL_SCORE] = (prefs[Keys.TOTAL_SCORE] ?: 0L) + pointsEarned.coerceAtLeast(0L)
            prefs[Keys.GAMES_PLAYED] = (prefs[Keys.GAMES_PLAYED] ?: 0L) + 1L
        }
    }

    suspend fun setDarkMode(dark: Boolean) {
        dataStore.edit { it[Keys.DARK_MODE] = dark }
    }

    suspend fun setMuted(muted: Boolean) {
        dataStore.edit { it[Keys.MUTED] = muted }
    }
}
