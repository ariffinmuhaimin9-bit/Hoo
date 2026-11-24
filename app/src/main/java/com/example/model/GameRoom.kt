package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class GameRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val hostUid: String = "",
    val hostName: String = "",
    val hostAvatar: String = "🦊",
    val guestUid: String? = null,
    val guestName: String? = null,
    val guestAvatar: String? = null,
    val guestReady: Boolean = false,
    val gameId: String = "sumo_3d",
    val status: String = "waiting", // "waiting", "playing", "finished"
    val lastEmote: String = "Jom mula!",
    val lastEmoteSender: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "roomCode" to roomCode,
        "hostUid" to hostUid,
        "hostName" to hostName.trim().ifBlank { "Host" }.take(30),
        "hostAvatar" to hostAvatar.ifBlank { "🦊" }.take(16),
        "guestReady" to false,
        "gameId" to gameId.ifBlank { "sumo_3d" }.take(60),
        "status" to "waiting",
        "lastEmote" to lastEmote.take(60),
        "lastEmoteSender" to lastEmoteSender.take(30),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
