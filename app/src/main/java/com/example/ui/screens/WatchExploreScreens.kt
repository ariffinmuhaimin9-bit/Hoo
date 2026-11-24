package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HubExtrasState
import com.example.data.buildBuddyList
import com.example.model.BuddyProfileItem
import com.example.model.HubFeaturesCatalog
import com.example.model.UserProfile
import com.example.utils.SoundHelper

@Composable
fun StoreScreen(
    extras: HubExtrasState,
    onBuyOrEquip: (id: String, price: Int, category: String) -> Unit
) {
    var filterCat by remember { mutableStateOf("Semua") }
    val categories = listOf("Semua", "Joystick", "Topi", "Bingkai", "Tema")
    val items = remember(filterCat) {
        if (filterCat == "Semua") HubFeaturesCatalog.STORE_ITEMS
        else HubFeaturesCatalog.STORE_ITEMS.filter { it.category == filterCat }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛒 Kedai Kosmetik & Kulit Joystick", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFFBBF24).copy(alpha = 0.2f)) {
                Text(
                    text = "🪙 ${extras.coins} • 💎 ${extras.gems}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = filterCat == cat,
                    onClick = { SoundHelper.playPop(); filterCat = cat },
                    label = { Text(cat, maxLines = 1, softWrap = false) }
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 165.dp),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val owned = extras.ownedItemIds.contains(item.id)
                val equipped = extras.equippedJoystickId == item.id || extras.equippedHatId == item.id
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, if (equipped) item.accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.emoji, fontSize = 32.sp)
                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Text(item.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        Button(
                            onClick = {
                                SoundHelper.playCoin()
                                onBuyOrEquip(item.id, item.priceCoins, item.category)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    equipped -> Color(0xFF22C55E)
                                    owned -> MaterialTheme.colorScheme.primary
                                    else -> item.accentColor
                                }
                            ),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Text(
                                text = when {
                                    equipped -> "✅ Dipakai"
                                    owned -> "Pakai"
                                    else -> "Beli ${item.priceCoins} 🪙"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackpackScreen(
    extras: HubExtrasState,
    onEquipItem: (id: String, category: String) -> Unit,
    onUpdateJoystickPrefs: (scale: Float, onRight: Boolean) -> Unit
) {
    val ownedList = remember(extras.ownedItemIds) {
        HubFeaturesCatalog.STORE_ITEMS.filter { extras.ownedItemIds.contains(it.id) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🎒 Beg Galas & Tetapan Joystick 360°", fontWeight = FontWeight.Black, fontSize = 18.sp)

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🕹️ Penyesuaian Joystick Bulat 360°", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Saiz Joystick:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.85f to "Kecil", 1.0f to "Sederhana", 1.2f to "Besar").forEach { (sc, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(extras.joystickScale - sc) < 0.05f,
                            onClick = { SoundHelper.playPop(); onUpdateJoystickPrefs(sc, extras.joystickOnRight) },
                            label = { Text(label, maxLines = 1, softWrap = false) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Letak Joystick di Sebelah Kanan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = extras.joystickOnRight,
                        onCheckedChange = { SoundHelper.playPop(); onUpdateJoystickPrefs(extras.joystickScale, it) }
                    )
                }
            }
        }

        Text("📦 Koleksi Anda (${ownedList.size}):", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        ownedList.forEach { item ->
            val active = extras.equippedJoystickId == item.id || extras.equippedHatId == item.id
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (active) Color(0xFF22C55E) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.emoji, fontSize = 28.sp)
                        Column {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(item.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = { SoundHelper.playCoin(); onEquipItem(item.id, item.category) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (active) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text(if (active) "Dipakai" else "Pakai", maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengesScreen(
    profile: UserProfile,
    extras: HubExtrasState,
    onClaimDailyStreak: () -> Unit,
    onClaimQuest: (questId: String, coins: Int, xp: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🏆 Cabaran Harian & Ganjaran", fontWeight = FontWeight.Black, fontSize = 18.sp)

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF59E0B).copy(alpha = 0.16f),
            border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🎁 Bonus Log Masuk Harian!", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("+100 Syiling 🪙, +5 Permata 💎, +50 XP", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { SoundHelper.playFanfare(); onClaimDailyStreak() },
                    enabled = !extras.dailyStreakClaimed,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(if (extras.dailyStreakClaimed) "Sudah Tuntut" else "Tuntut!", maxLines = 1, softWrap = false)
                }
            }
        }

        HubFeaturesCatalog.DAILY_QUESTS.forEach { quest ->
            val claimed = extras.claimedQuestIds.contains(quest.id)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(quest.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Ganjaran: +${quest.rewardCoins} 🪙 • +${quest.rewardXp} XP", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = { SoundHelper.playCoin(); onClaimQuest(quest.id, quest.rewardCoins, quest.rewardXp) },
                        enabled = !claimed,
                        modifier = Modifier.height(42.dp)
                    ) {
                        Text(if (claimed) "Selesai ✓" else "Tuntut", maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        Text("🎖️ Statistik Semasa: Tahap ${extras.level} (${extras.xp} XP) • ${profile.gamesPlayed} Permainan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun BuddiesPeopleScreen(
    extras: HubExtrasState,
    onAddBuddy: (String) -> Unit,
    onInviteBuddy: (BuddyProfileItem) -> Unit,
    onCheerProfile: () -> Unit
) {
    var buddyNameInput by remember { mutableStateOf("") }
    val buddies = remember(extras.customBuddies) { buildBuddyList(extras.customBuddies) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("👥 Senarai Rakan & Jemputan (Buddies)", fontWeight = FontWeight.Black, fontSize = 18.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = buddyNameInput,
                onValueChange = { if (it.length <= 24) buddyNameInput = it },
                placeholder = { Text("Nama rakan baharu...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (buddyNameInput.isNotBlank()) {
                        SoundHelper.playCoin()
                        onAddBuddy(buddyNameInput)
                        buddyNameInput = ""
                    }
                },
                modifier = Modifier.height(52.dp)
            ) {
                Text("➕ Tambah", maxLines = 1, softWrap = false)
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(buddies, key = { it.id }) { b ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(b.avatar, fontSize = 24.sp)
                            }
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(10.dp).clip(CircleShape)
                                            .background(if (b.isOnline) Color(0xFF22C55E) else Color(0xFF94A3B8))
                                    )
                                    Text("${b.name} (${b.handle})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Text(
                                    text = "${b.status} • ${b.currentGameTitle} • ${"★".repeat(b.ratingStars)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = { SoundHelper.playCoin(); onCheerProfile() }) {
                                Text("👍", fontSize = 18.sp)
                            }
                            Button(
                                onClick = { SoundHelper.playPop(); onInviteBuddy(b) },
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text("Jemput", fontSize = 12.sp, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturesRegistryScreen() {
    var search by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Semua") }
    val cats = listOf("Semua", "WiFi Room", "Kawalan 360°", "Watch Menu UI", "Profil & Sosial", "Kedai & Cabaran", "Katalog 202 Game")
    val filtered = remember(search, selectedCat) {
        HubFeaturesCatalog.ALL_105_FEATURES.filter { f ->
            (selectedCat == "Semua" || f.category == selectedCat) &&
                (search.isBlank() || f.title.contains(search, ignoreCase = true) || f.category.contains(search, ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("✨ Direktori ${HubFeaturesCatalog.ALL_105_FEATURES.size} Fitur Baharu", fontWeight = FontWeight.Black, fontSize = 18.sp)
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Cari fitur baharu...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cats.forEach { cat ->
                FilterChip(
                    selected = selectedCat == cat,
                    onClick = { SoundHelper.playPop(); selectedCat = cat },
                    label = { Text(cat, maxLines = 1, softWrap = false) }
                )
            }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(filtered, key = { it.number }) { feat ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = "${feat.number}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Column {
                            Text(feat.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${feat.category} • ${feat.detail}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
