package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.HubExtrasState
import com.example.model.GameItem
import com.example.model.GameRepository
import com.example.model.HubFeaturesCatalog
import com.example.model.UserProfile
import com.example.ui.components.WatchTab
import com.example.utils.SoundHelper

@Composable
fun MainHubScreen(
    userProfile: UserProfile,
    extras: HubExtrasState,
    isGoogleUser: Boolean,
    isLandscape: Boolean,
    showWatchHomeHeader: Boolean,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onSelectGame: (GameItem) -> Unit,
    onOpenRoomService: () -> Unit,
    onOpenProfile: () -> Unit,
    onNavigateWatchTab: (WatchTab) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }

    val allGames = remember { GameRepository.getAllGames() }
    val recentGames = remember(extras.recentGameIds, allGames) {
        extras.recentGameIds.mapNotNull { id -> allGames.firstOrNull { it.id == id } }
    }
    val hatEmoji = remember(extras.equippedHatId) {
        HubFeaturesCatalog.STORE_ITEMS.firstOrNull { it.id == extras.equippedHatId }?.emoji ?: ""
    }

    val filteredGames = remember(searchQuery, selectedCategory, favorites) {
        allGames.filter { game ->
            val matchesSearch = game.title.contains(searchQuery, ignoreCase = true) ||
                game.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategory) {
                "all" -> true
                "playable" -> game.isPlayable
                "3d" -> game.is3D
                "fav" -> favorites.contains(game.id)
                else -> game.category == selectedCategory
            }
            matchesSearch && matchesCategory
        }
    }

    val avatarRingColor = remember(userProfile.avatarColorHex) {
        try {
            Color(android.graphics.Color.parseColor(userProfile.avatarColorHex))
        } catch (_: Exception) {
            Color(0xFF38BDF8)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = if (isLandscape) 12.dp else 14.dp, vertical = if (isLandscape) 6.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 10.dp)
    ) {
        // Watch Menu Player Header (Level Badge, @handle, Coins, Gems, Quick Profile)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, avatarRingColor.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { SoundHelper.playPop(); onOpenProfile() }
                .testTag("hub_profile_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEA580C),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${extras.level}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(avatarRingColor.copy(alpha = 0.25f))
                            .border(2.dp, avatarRingColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${hatEmoji}${userProfile.avatarEmoji}", fontSize = if (hatEmoji.isNotEmpty()) 18.sp else 22.sp)
                    }
                    Column {
                        Text(
                            text = "${userProfile.displayName} • ${extras.handle}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "🪙 ${extras.coins} • 💎 ${extras.gems} • ${if (isGoogleUser) "☁️ Awan" else "📱 Tetamu"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                OutlinedButton(
                    onClick = { SoundHelper.playPop(); onOpenProfile() },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(38.dp).testTag("hub_edit_profile_button")
                ) {
                    Text("✏️ Profil", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }

        // Watch Menu Shortcut Grid & Continue Playing Shelves (Shown on Utama tab in Portrait)
        if (showWatchHomeHeader && !isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HubFeaturesCatalog.WATCH_SHORTCUTS.forEach { (id, icon, label) ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                        modifier = Modifier
                            .clickable {
                                SoundHelper.playPop()
                                val target = when (id) {
                                    "play" -> WatchTab.PLAY
                                    "wifi" -> WatchTab.WIFI
                                    "store" -> WatchTab.STORE
                                    "challenges" -> WatchTab.CHALLENGES
                                    "people" -> WatchTab.PEOPLE
                                    "backpack" -> WatchTab.BACKPACK
                                    else -> WatchTab.FEATURES
                                }
                                onNavigateWatchTab(target)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(icon, fontSize = 20.sp)
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            // Continue Playing Shelf (Inspired by Rec Room Watch Menu Image 3)
            if (recentGames.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("▶️ Teruskan Bermain", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text(
                            text = "📶 Buka WiFi Room →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { SoundHelper.playPop(); onNavigateWatchTab(WatchTab.WIFI) }
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentGames, key = { "rec_${it.id}" }) { rg ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable { SoundHelper.playPop(); onSelectGame(rg) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(rg.emoji, fontSize = 22.sp)
                                    Text(rg.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Compact Hero Banner on Play tab (or when not Home)
        if (!showWatchHomeHeader && !isLandscape) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "Banner MiniVerse",
                        contentScale = ContentScale.Crop,
                        alpha = 0.26f,
                        modifier = Modifier.matchParentSize()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Color(0xBB0F172A), Color(0xBB1E1B4B))))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🎮 ${allGames.size} Game Sedia Main!", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                            Text("Termasuk 68+ game baharu & Joystick 360°!", fontSize = 11.sp, color = Color(0xFFFDE68A))
                        }
                        Button(
                            onClick = { SoundHelper.playPop(); onOpenRoomService() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text("🏠 Room", color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        // Search & Random Game
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari dari ${allGames.size} game...") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("hub_search_input"),
                shape = RoundedCornerShape(14.dp)
            )
            Button(
                onClick = {
                    SoundHelper.playCoin()
                    val pool = filteredGames.ifEmpty { allGames }
                    if (pool.isNotEmpty()) onSelectGame(pool.random())
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(52.dp).testTag("hub_random_button")
            ) {
                Text("🎲 Rawak", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
        }

        // Anti-Squish Horizontally Scrollable Category Pills
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "all" to "Semua (${allGames.size})",
                "3d" to "3D",
                "aksi" to "Aksi",
                "papan" to "Papan",
                "teka" to "Teka-Teki",
                "parti" to "Parti",
                "fav" to "⭐ Kegemaran"
            ).forEach { (key, label) ->
                val selected = selectedCategory == key
                Button(
                    onClick = { SoundHelper.playPop(); selectedCategory = key },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(42.dp).testTag("category_pill_$key")
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }

        if (filteredGames.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("🔍 Tiada permainan dijumpai. Cuba kata kunci lain.", fontWeight = FontWeight.Bold)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isLandscape) 200.dp else 235.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredGames, key = { it.id }) { game ->
                    val isFav = favorites.contains(game.id)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { SoundHelper.playPop(); onSelectGame(game) }
                            .testTag("game_card_${game.id}"),
                        shadowElevation = 3.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(game.emoji, fontSize = 26.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (game.is3D) {
                                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFA855F7).copy(alpha = 0.2f)) {
                                            Text("3D", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFA855F7), maxLines = 1, softWrap = false)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF22C55E).copy(alpha = 0.2f)) {
                                        Text("PLAY", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF22C55E), maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                            Text(game.title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(game.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { SoundHelper.playCoin(); onToggleFavorite(game.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text(if (isFav) "⭐" else "☆", fontSize = 18.sp)
                                }
                                Button(
                                    onClick = { SoundHelper.playPop(); onSelectGame(game) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text("Main 🎮", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
