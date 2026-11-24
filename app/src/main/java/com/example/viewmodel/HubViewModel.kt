package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CloudProfileRepository
import com.example.data.LocalGuestRepository
import com.example.data.RoomRepository
import com.example.model.GameRoom
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class HubViewModel(
    private val cloudProfileRepository: CloudProfileRepository,
    private val localGuestRepository: LocalGuestRepository,
    private val roomRepository: RoomRepository,
    private val currentUserId: String?,
    private val defaultDisplayName: String
) : ViewModel() {

    val isGoogleAuthenticated: Boolean = !currentUserId.isNullOrBlank()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _activeRoomCode = MutableStateFlow<String?>(null)
    val activeRoomCode: StateFlow<String?> = _activeRoomCode.asStateFlow()

    val profileState: StateFlow<UiState<UserProfile>> = if (!currentUserId.isNullOrBlank()) {
        cloudProfileRepository.observeProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile>> { profile ->
                UiState.Success(
                    profile ?: UserProfile(
                        userId = currentUserId,
                        displayName = defaultDisplayName.ifBlank { "Pemain Google" }
                    )
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing profile", rootError)
                emit(UiState.Error(rootError.message ?: "Gagal memuatkan profil awan"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
    } else {
        localGuestRepository.guestProfileFlow
            .map<UserProfile, UiState<UserProfile>> { UiState.Success(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val cloudRoomState: StateFlow<GameRoom?> = if (!currentUserId.isNullOrBlank()) {
        _activeRoomCode
            .flatMapLatest { code ->
                if (code.isNullOrBlank()) flowOf(null)
                else roomRepository.observeRoom(code).catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing room $code", firestoreError ?: error)
                    emit(null)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = null
            )
    } else {
        MutableStateFlow<GameRoom?>(null).asStateFlow()
    }

    init {
        if (!currentUserId.isNullOrBlank()) {
            viewModelScope.launch {
                cloudProfileRepository.ensureProfileExists(defaultDisplayName)
            }
        }
    }

    fun updateCustomProfile(
        displayName: String,
        avatarEmoji: String,
        avatarColorHex: String,
        bio: String,
        favoriteBadge: String,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            if (isGoogleAuthenticated) {
                val res = cloudProfileRepository.updateCustomProfile(
                    displayName, avatarEmoji, avatarColorHex, bio, favoriteBadge
                )
                if (res.isSuccess) {
                    _statusBanner.value = "Profil Google berjaya dikemas kini!"
                    onDone(true, null)
                } else {
                    onDone(false, res.exceptionOrNull()?.localizedMessage ?: "Gagal menyimpan profil.")
                }
            } else {
                localGuestRepository.updateGuestProfile(
                    displayName, avatarEmoji, avatarColorHex, bio, favoriteBadge
                )
                _statusBanner.value = "Profil Tetamu disimpan di peranti!"
                onDone(true, null)
            }
        }
    }

    fun toggleFavorite(gameId: String, currentFavorites: List<String>) {
        viewModelScope.launch {
            if (isGoogleAuthenticated) {
                cloudProfileRepository.toggleFavorite(gameId, currentFavorites)
            } else {
                localGuestRepository.toggleGuestFavorite(gameId)
            }
        }
    }

    fun recordScore(points: Long) {
        if (points <= 0L) return
        viewModelScope.launch {
            if (isGoogleAuthenticated) {
                cloudProfileRepository.recordGameScore(points)
            } else {
                localGuestRepository.recordGuestScore(points)
            }
        }
    }

    fun createCloudRoom(
        roomCode: String,
        hostName: String,
        hostAvatar: String,
        gameId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (!isGoogleAuthenticated) {
            onResult(false, "Log masuk Google diperlukan untuk bilik awan masa nyata.")
            return
        }
        viewModelScope.launch {
            val res = roomRepository.createRoom(roomCode, hostName, hostAvatar, gameId)
            if (res.isSuccess) {
                _activeRoomCode.value = roomCode
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.localizedMessage)
            }
        }
    }

    fun joinCloudRoom(
        roomCode: String,
        guestName: String,
        guestAvatar: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (!isGoogleAuthenticated) {
            onResult(false, "Log masuk Google diperlukan untuk menyertai bilik awan.")
            return
        }
        viewModelScope.launch {
            val res = roomRepository.joinRoom(roomCode, guestName, guestAvatar)
            if (res.isSuccess) {
                _activeRoomCode.value = roomCode
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.localizedMessage)
            }
        }
    }

    fun toggleCloudGuestReady(roomCode: String, ready: Boolean) {
        if (!isGoogleAuthenticated) return
        viewModelScope.launch {
            roomRepository.toggleGuestReady(roomCode, ready)
        }
    }

    fun sendCloudEmote(roomCode: String, emote: String, senderName: String) {
        if (!isGoogleAuthenticated) return
        viewModelScope.launch {
            roomRepository.sendQuickEmote(roomCode, emote, senderName)
        }
    }

    fun updateCloudRoomStatus(roomCode: String, status: String, gameId: String? = null) {
        if (!isGoogleAuthenticated) return
        viewModelScope.launch {
            roomRepository.updateRoomStateByHost(roomCode, status, gameId)
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    private companion object {
        const val TAG = "HubViewModel"
    }
}
