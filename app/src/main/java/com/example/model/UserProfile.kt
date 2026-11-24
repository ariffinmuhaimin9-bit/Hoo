package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Pemain Ceria",
    val avatarEmoji: String = "🦊",
    val avatarColorHex: String = "#38BDF8",
    val bio: String = "Sedia untuk bermain di MiniVerse Hub!",
    val favoriteBadge: String = "Pemain Baru",
    val totalScore: Long = 0L,
    val gamesPlayed: Long = 0L,
    val favoriteGameIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "displayName" to displayName.trim().ifBlank { "Pemain Ceria" }.take(30),
        "avatarEmoji" to avatarEmoji.ifBlank { "🦊" }.take(16),
        "avatarColorHex" to avatarColorHex.ifBlank { "#38BDF8" }.take(12),
        "bio" to bio.trim().take(140),
        "favoriteBadge" to favoriteBadge.trim().ifBlank { "Pemain Baru" }.take(40),
        "totalScore" to totalScore.coerceAtLeast(0L),
        "gamesPlayed" to gamesPlayed.coerceAtLeast(0L),
        "favoriteGameIds" to favoriteGameIds.take(150),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toProfileUpdateMap(): Map<String, Any> = mapOf(
        "displayName" to displayName.trim().ifBlank { "Pemain Ceria" }.take(30),
        "avatarEmoji" to avatarEmoji.ifBlank { "🦊" }.take(16),
        "avatarColorHex" to avatarColorHex.ifBlank { "#38BDF8" }.take(12),
        "bio" to bio.trim().take(140),
        "favoriteBadge" to favoriteBadge.trim().ifBlank { "Pemain Baru" }.take(40),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

object ProfileDefaults {
    val AVATARS = listOf(
        "🦊", "🐼", "🤖", "🐱", "🦁", "🐰", "🐸", "🦄", "🐙", "🐧",
        "🐲", "🚀", "🐯", "🐨", "🐶", "🐥", "🦉", "🐝", "👾", "🧙"
    )
    val AVATAR_COLORS = listOf(
        "#38BDF8", "#A855F7", "#FBBF24", "#22C55E", "#F97316",
        "#EC4899", "#EF4444", "#14B8A6", "#6366F1", "#EAB308"
    )
    val BADGES = listOf(
        "Pemain Baru",
        "Juara Sumo",
        "Raja Teka-Teki",
        "Kapten 3D",
        "Bintang Parti",
        "Kilat Refleks",
        "Host WiFi Pro",
        "Pakar Strategi",
        "Penjaga Gelanggang",
        "Wira MiniVerse",
        "Kolektor Emas",
        "Rakan Setia"
    )
    val STATUS_STATES = listOf("Tersedia", "Sedang Main", "Sibuk", "Berehat")
    val EXPERIENCE_TIERS = listOf("Novice", "Amateur", "Pro", "Master", "Legend")
}
