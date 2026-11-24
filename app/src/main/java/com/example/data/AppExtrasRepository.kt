package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.BuddyProfileItem
import com.example.model.HubFeaturesCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.extrasDataStore: DataStore<Preferences> by preferencesDataStore(name = "miniverse_hub_extras")

data class HubExtrasState(
    val coins: Int = 250,
    val gems: Int = 15,
    val xp: Int = 120,
    val equippedJoystickId: String = "joy_neon",
    val equippedHatId: String = "",
    val joystickScale: Float = 1.0f, // 0.85f, 1.0f, 1.2f
    val joystickOnRight: Boolean = false,
    val ownedItemIds: Set<String> = setOf("joy_neon"),
    val claimedQuestIds: Set<String> = emptySet(),
    val recentGameIds: List<String> = listOf("sumo_3d", "sky_parkour", "uno_cards"),
    val handle: String = "@pemain_ria",
    val onlineVisible: Boolean = true,
    val statusState: String = "Tersedia",
    val experienceTier: String = "Novice",
    val ratingStars: Int = 4,
    val cheerCount: Int = 12,
    val dailyStreakClaimed: Boolean = false,
    val customBuddies: List<String> = emptyList()
) {
    val level: Int get() = 1 + (xp / 150)
    val levelProgress: Float get() = (xp % 150) / 150f
}

class AppExtrasRepository(context: Context) {
    private val dataStore = context.applicationContext.extrasDataStore

    private object Keys {
        val COINS = intPreferencesKey("coins")
        val GEMS = intPreferencesKey("gems")
        val XP = intPreferencesKey("xp")
        val EQUIPPED_JOY = stringPreferencesKey("equipped_joystick")
        val EQUIPPED_HAT = stringPreferencesKey("equipped_hat")
        val JOY_SCALE = floatPreferencesKey("joystick_scale")
        val JOY_RIGHT = booleanPreferencesKey("joystick_on_right")
        val OWNED_ITEMS = stringSetPreferencesKey("owned_items")
        val CLAIMED_QUESTS = stringSetPreferencesKey("claimed_quests")
        val RECENT_GAMES = stringPreferencesKey("recent_games_csv")
        val HANDLE = stringPreferencesKey("player_handle")
        val ONLINE_VISIBLE = booleanPreferencesKey("online_visible")
        val STATUS_STATE = stringPreferencesKey("status_state")
        val EXPERIENCE_TIER = stringPreferencesKey("experience_tier")
        val RATING_STARS = intPreferencesKey("rating_stars")
        val CHEER_COUNT = intPreferencesKey("cheer_count")
        val DAILY_STREAK_CLAIMED = booleanPreferencesKey("daily_streak_claimed")
        val CUSTOM_BUDDIES = stringSetPreferencesKey("custom_buddies")
    }

    val stateFlow: Flow<HubExtrasState> = dataStore.data.map { p ->
        val recentCsv = p[Keys.RECENT_GAMES] ?: "sumo_3d,sky_parkour,uno_cards"
        HubExtrasState(
            coins = p[Keys.COINS] ?: 250,
            gems = p[Keys.GEMS] ?: 15,
            xp = p[Keys.XP] ?: 120,
            equippedJoystickId = p[Keys.EQUIPPED_JOY] ?: "joy_neon",
            equippedHatId = p[Keys.EQUIPPED_HAT] ?: "",
            joystickScale = p[Keys.JOY_SCALE] ?: 1.0f,
            joystickOnRight = p[Keys.JOY_RIGHT] ?: false,
            ownedItemIds = p[Keys.OWNED_ITEMS] ?: setOf("joy_neon"),
            claimedQuestIds = p[Keys.CLAIMED_QUESTS] ?: emptySet(),
            recentGameIds = recentCsv.split(",").filter { it.isNotBlank() }.take(8),
            handle = p[Keys.HANDLE] ?: "@pemain_ria",
            onlineVisible = p[Keys.ONLINE_VISIBLE] ?: true,
            statusState = p[Keys.STATUS_STATE] ?: "Tersedia",
            experienceTier = p[Keys.EXPERIENCE_TIER] ?: "Novice",
            ratingStars = p[Keys.RATING_STARS] ?: 4,
            cheerCount = p[Keys.CHEER_COUNT] ?: 12,
            dailyStreakClaimed = p[Keys.DAILY_STREAK_CLAIMED] ?: false,
            customBuddies = (p[Keys.CUSTOM_BUDDIES] ?: emptySet()).toList()
        )
    }

    suspend fun recordPlaySession(gameId: String, pointsEarned: Int) {
        dataStore.edit { p ->
            val currentCoins = p[Keys.COINS] ?: 250
            val currentXp = p[Keys.XP] ?: 120
            val coinBonus = (pointsEarned / 4).coerceIn(5, 80)
            p[Keys.COINS] = currentCoins + coinBonus
            p[Keys.XP] = currentXp + 25 + (pointsEarned / 5).coerceAtMost(60)
            val prev = (p[Keys.RECENT_GAMES] ?: "").split(",").filter { it.isNotBlank() }
            val updated = (listOf(gameId) + prev.filter { it != gameId }).take(8)
            p[Keys.RECENT_GAMES] = updated.joinToString(",")
        }
    }

    suspend fun buyOrEquipItem(itemId: String, price: Int, category: String): String {
        var resultMsg = "Berjaya!"
        dataStore.edit { p ->
            val owned = (p[Keys.OWNED_ITEMS] ?: setOf("joy_neon")).toMutableSet()
            val coins = p[Keys.COINS] ?: 250
            if (owned.contains(itemId)) {
                if (category == "Joystick") p[Keys.EQUIPPED_JOY] = itemId
                else if (category == "Topi") p[Keys.EQUIPPED_HAT] = itemId
                resultMsg = "Item dipakai!"
            } else if (coins >= price) {
                p[Keys.COINS] = coins - price
                owned.add(itemId)
                p[Keys.OWNED_ITEMS] = owned
                if (category == "Joystick") p[Keys.EQUIPPED_JOY] = itemId
                else if (category == "Topi") p[Keys.EQUIPPED_HAT] = itemId
                resultMsg = "Berjaya beli & pakai!"
            } else {
                resultMsg = "Syiling tidak mencukupi (Perlu $price 🪙)"
            }
        }
        return resultMsg
    }

    suspend fun setJoystickSettings(scale: Float, onRight: Boolean) {
        dataStore.edit { p ->
            p[Keys.JOY_SCALE] = scale.coerceIn(0.8f, 1.3f)
            p[Keys.JOY_RIGHT] = onRight
        }
    }

    suspend fun claimQuest(questId: String, rewardCoins: Int, rewardXp: Int): Boolean {
        var claimed = false
        dataStore.edit { p ->
            val current = (p[Keys.CLAIMED_QUESTS] ?: emptySet()).toMutableSet()
            if (!current.contains(questId)) {
                current.add(questId)
                p[Keys.CLAIMED_QUESTS] = current
                p[Keys.COINS] = (p[Keys.COINS] ?: 250) + rewardCoins
                p[Keys.XP] = (p[Keys.XP] ?: 120) + rewardXp
                claimed = true
            }
        }
        return claimed
    }

    suspend fun claimDailyStreak(): Boolean {
        var granted = false
        dataStore.edit { p ->
            if (p[Keys.DAILY_STREAK_CLAIMED] != true) {
                p[Keys.DAILY_STREAK_CLAIMED] = true
                p[Keys.COINS] = (p[Keys.COINS] ?: 250) + 100
                p[Keys.GEMS] = (p[Keys.GEMS] ?: 15) + 5
                p[Keys.XP] = (p[Keys.XP] ?: 120) + 50
                granted = true
            }
        }
        return granted
    }

    suspend fun updateSocialProfileFields(
        handle: String,
        onlineVisible: Boolean,
        statusState: String,
        experienceTier: String,
        ratingStars: Int
    ) {
        dataStore.edit { p ->
            val cleanHandle = handle.trim().let { if (it.startsWith("@")) it else "@$it" }.take(20)
            p[Keys.HANDLE] = cleanHandle.ifBlank { "@pemain_ria" }
            p[Keys.ONLINE_VISIBLE] = onlineVisible
            p[Keys.STATUS_STATE] = statusState
            p[Keys.EXPERIENCE_TIER] = experienceTier
            p[Keys.RATING_STARS] = ratingStars.coerceIn(1, 5)
        }
    }

    suspend fun addCheer(): Int {
        var next = 0
        dataStore.edit { p ->
            next = (p[Keys.CHEER_COUNT] ?: 12) + 1
            p[Keys.CHEER_COUNT] = next
        }
        return next
    }

    suspend fun addBuddy(name: String) {
        val clean = name.trim().take(24)
        if (clean.isBlank()) return
        dataStore.edit { p ->
            val set = (p[Keys.CUSTOM_BUDDIES] ?: emptySet()).toMutableSet()
            set.add(clean)
            p[Keys.CUSTOM_BUDDIES] = set
        }
    }
}

fun buildBuddyList(customNames: List<String>): List<BuddyProfileItem> {
    val custom = customNames.mapIndexed { idx, name ->
        BuddyProfileItem(
            id = "custom_$idx",
            name = name,
            handle = "@${name.lowercase().replace(" ", "_")}",
            avatar = "🦊",
            status = "Tersedia",
            currentGameTitle = "Lobi MiniVerse",
            ratingStars = 4,
            experience = "Amateur",
            isOnline = true
        )
    }
    return custom + HubFeaturesCatalog.DEFAULT_BUDDIES
}
