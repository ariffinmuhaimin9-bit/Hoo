package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameItem
import com.example.model.LobbyRulesConfig
import com.example.model.RoomInvitation
import com.example.model.TeamSlotColor
import com.example.model.UserProfile
import com.example.network.WifiConnectionRole
import com.example.network.WifiRoomManager
import com.example.network.WifiRoomState
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay

@Composable
fun WifiRoomScreen(
    wifiState: WifiRoomState,
    wifiManager: WifiRoomManager,
    userProfile: UserProfile,
    selectedGame: GameItem,
    onLaunchWifiGame: (GameItem) -> Unit,
    onOpenGamePicker: () -> Unit
) {
    var subTab by remember { mutableStateOf(0) } // 0: Lobi & Pasukan, 1: Imbas & IP, 2: Peraturan Info, 3: Sembang
    var manualIp by remember { mutableStateOf(wifiState.localIp.takeIf { it != "0.0.0.0" } ?: "192.168.1.10") }
    var manualPort by remember { mutableStateOf("8989") }
    var chatInput by remember { mutableStateOf("") }
    var roomCodeInput by remember { mutableStateOf(wifiState.roomCode) }

    LaunchedEffect(Unit) {
        wifiManager.refreshNetworkInfo()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // WiFi Hardware & Socket Diagnostics Header (Tools Wajib WiFi Room)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFFDE047).copy(alpha = 0.14f),
            border = BorderStroke(1.5.dp, Color(0xFFFBBF24).copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📶 WiFi Room LAN (${if (wifiState.isWifiConnected) "Aktif" else "Hotspot / Semak WiFi"})",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (wifiState.isConnectedToRoom) Color(0xFF22C55E) else Color(0xFF64748B)
                    ) {
                        Text(
                            text = when (wifiState.role) {
                                WifiConnectionRole.HOST -> "HOST • ${wifiState.players.size} Pemain"
                                WifiConnectionRole.CLIENT -> "SAMBUNG • ${wifiState.pingMs}ms"
                                WifiConnectionRole.NONE -> "SEDIA"
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Text(
                    text = "IP Peranti: ${wifiState.localIp}:${wifiState.listenPort} • ${wifiState.statusLog}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            SoundHelper.playPop()
                            wifiManager.hostWifiRoom(
                                roomCode = roomCodeInput.ifBlank { "4040" },
                                hostName = userProfile.displayName,
                                hostAvatar = userProfile.avatarEmoji,
                                gameId = selectedGame.id
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("wifi_host_button")
                    ) {
                        Text("📡 Host WiFi", color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = {
                            SoundHelper.playPop()
                            subTab = 1
                            wifiManager.scanWifiRooms()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("wifi_scan_button")
                    ) {
                        Text("🔍 Imbas Bilik", color = Color.Black, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = {
                            SoundHelper.playPop()
                            wifiManager.refreshNetworkInfo()
                        },
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("🔄 Semak IP", maxLines = 1, softWrap = false)
                    }
                    if (wifiState.isConnectedToRoom) {
                        OutlinedButton(
                            onClick = {
                                SoundHelper.playBoing()
                                wifiManager.stopAll()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("✖ Putuskan", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        // Sub-navigation tabs (Custom Game / Scan & IP / Lobby Info / Chat)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                0 to "🎮 Custom Game & Pasukan",
                1 to "🌐 Imbas & Sambung IP",
                2 to "⚙️ Lobby Info & Peraturan",
                3 to "💬 Sembang (${wifiState.chatMessages.size})"
            ).forEach { (idx, label) ->
                val sel = subTab == idx
                FilterChip(
                    selected = sel,
                    onClick = {
                        SoundHelper.playPop()
                        subTab = idx
                    },
                    label = { Text(label, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) }
                )
            }
        }

        when (subTab) {
            0 -> CustomGameTeamsPane(
                wifiState = wifiState,
                wifiManager = wifiManager,
                userProfile = userProfile,
                selectedGame = selectedGame,
                onLaunchWifiGame = onLaunchWifiGame,
                onOpenGamePicker = onOpenGamePicker,
                modifier = Modifier.weight(1f)
            )
            1 -> ScanAndDirectIpPane(
                wifiState = wifiState,
                wifiManager = wifiManager,
                userProfile = userProfile,
                manualIp = manualIp,
                onManualIpChange = { manualIp = it },
                manualPort = manualPort,
                onManualPortChange = { manualPort = it },
                roomCodeInput = roomCodeInput,
                onRoomCodeChange = { roomCodeInput = it },
                onSwitchToLobby = { subTab = 0 },
                modifier = Modifier.weight(1f)
            )
            2 -> LobbyInfoRulesPane(
                rules = wifiState.rules,
                onUpdateRules = { wifiManager.updateRules(it) },
                modifier = Modifier.weight(1f)
            )
            3 -> WifiChatPane(
                wifiState = wifiState,
                chatInput = chatInput,
                onChatInputChange = { chatInput = it },
                onSend = { msg ->
                    wifiManager.sendChat(userProfile.displayName, msg)
                    chatInput = ""
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CustomGameTeamsPane(
    wifiState: WifiRoomState,
    wifiManager: WifiRoomManager,
    userProfile: UserProfile,
    selectedGame: GameItem,
    onLaunchWifiGame: (GameItem) -> Unit,
    onOpenGamePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Inspired by Party Animals Custom Game Team Lobby (Image 10) + Mirror Ready Up (Image 5)
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFFEF3C7),
            border = BorderStroke(2.dp, Color(0xFFF59E0B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CUSTOM GAME • ${selectedGame.emoji} ${selectedGame.title}",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Pilih pasukan warna anda atau tambah BOT untuk ujian!",
                            fontSize = 11.sp,
                            color = Color(0xFF78350F)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                SoundHelper.playPop()
                                wifiManager.updateRules(wifiState.rules.copy(isLocked = !wifiState.rules.isLocked))
                            },
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text(if (wifiState.rules.isLocked) "🔒 Kunci" else "🔓 Terbuka", fontSize = 11.sp, color = Color(0xFF92400E), maxLines = 1, softWrap = false)
                        }
                    }
                }

                // Action Row: Pick Game, Invite, Add Bot, Ready, Start!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            SoundHelper.playPop()
                            onOpenGamePicker()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("🎲 Tukar Game", color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = {
                            SoundHelper.playCoin()
                            wifiManager.sendGameInvitation(userProfile.displayName, selectedGame.id, selectedGame.title)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("📨 Jemput Rakan", color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = {
                            SoundHelper.playPop()
                            wifiManager.addTestBotToWifiRoom()
                        },
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("➕ Tambah BOT", color = Color(0xFF78350F), maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = {
                            SoundHelper.playPop()
                            wifiManager.toggleReady()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("✅ Sedia", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = {
                            SoundHelper.playFanfare()
                            wifiManager.broadcastHostGameStart(selectedGame.id)
                            onLaunchWifiGame(selectedGame)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF84CC16)),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("wifi_start_game_button")
                    ) {
                        Text("▶ MULA MAIN", color = Color(0xFF1A2E05), fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                    }
                }

                // 4 Team Cards + Spectator Card
                TeamSlotColor.entries.forEach { team ->
                    val members = wifiState.players.filter { it.team == team }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Color(team.hex)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                SoundHelper.playPop()
                                wifiManager.selectTeam(team)
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color(team.hex)
                                ) {
                                    Text(
                                        text = "PASUKAN ${team.label}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = "Ketik untuk masuk pasukan (${members.size})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            if (members.isEmpty()) {
                                Text(
                                    text = "KOSONG — Menunggu pemain...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                members.forEach { slot ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${slot.avatar} ${slot.name} ${if (slot.isHost) "(HOST)" else ""}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (slot.isReady) Color(0xFF22C55E) else Color(0xFFEF4444)
                                        ) {
                                            Text(
                                                text = if (slot.isReady) "READY" else "BELUM",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanAndDirectIpPane(
    wifiState: WifiRoomState,
    wifiManager: WifiRoomManager,
    userProfile: UserProfile,
    manualIp: String,
    onManualIpChange: (String) -> Unit,
    manualPort: String,
    onManualPortChange: (String) -> Unit,
    roomCodeInput: String,
    onRoomCodeChange: (String) -> Unit,
    onSwitchToLobby: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Direct Host / Join IP Panel (Inspired by Image 9)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🔌 Sambungan IP Terus (Hotspot & WiFi LAN)", fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(
                    text = "Jika peranti rakan berada pada Hotspot atau WiFi sama, masukkan alamat IP Host yang dipaparkan di atas skrin rakan.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = onManualIpChange,
                        label = { Text("Alamat IP Host") },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = manualPort,
                        onValueChange = { onManualPortChange(it.filter { c -> c.isDigit() }.take(5)) },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = roomCodeInput,
                        onValueChange = { onRoomCodeChange(it.filter { c -> c.isDigit() }.take(4)) },
                        label = { Text("Kod 4-Digit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            SoundHelper.playPop()
                            wifiManager.joinWifiRoomByIp(
                                hostIp = manualIp,
                                port = manualPort.toIntOrNull() ?: 8989,
                                playerName = userProfile.displayName,
                                playerAvatar = userProfile.avatarEmoji
                            )
                            onSwitchToLobby()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(54.dp)
                            .testTag("wifi_direct_join_button")
                    ) {
                        Text("SAMBUNG IP", color = Color.Black, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // Discovered Active WiFi Rooms via NSD
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📡 Bilik Aktif Dijumpai (${wifiState.discoveredRooms.size})", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    if (wifiState.isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
                if (wifiState.discoveredRooms.isEmpty()) {
                    Text(
                        text = "Tiada bilik WiFi ditemui secara automatik. Tekan 'Host WiFi' di satu telefon dan 'Imbas Bilik' di telefon kedua, atau guna Sambungan IP Terus.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    wifiState.discoveredRooms.forEach { room ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("🏠 Bilik #${room.roomCode} • Host: ${room.hostName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${room.hostIp}:${room.port}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = {
                                        SoundHelper.playCoin()
                                        wifiManager.joinWifiRoomByIp(
                                            hostIp = room.hostIp,
                                            port = room.port,
                                            playerName = userProfile.displayName,
                                            playerAvatar = userProfile.avatarEmoji
                                        )
                                        onSwitchToLobby()
                                    },
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("Quick Join", maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbyInfoRulesPane(
    rules: LobbyRulesConfig,
    onUpdateRules: (LobbyRulesConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    // Inspired by Party Animals "LOBBY INFO" yellow card (Reference Image 2)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFEF3C7),
        border = BorderStroke(2.dp, Color(0xFFFBBF24)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("LOBBY INFO & PERATURAN", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color(0xFFB45309))
            RuleToggleRow("Pilih Semula Watak Setiap Game", rules.reselectCharacterEveryGame) {
                onUpdateRules(rules.copy(reselectCharacterEveryGame = it))
            }
            RuleToggleRow("Pasukan Rawak (Random Teams)", rules.randomTeams) {
                onUpdateRules(rules.copy(randomTeams = it))
            }
            RuleToggleRow("Matikan Knock-Out", rules.disableKnockOut) {
                onUpdateRules(rules.copy(disableKnockOut = it))
            }
            RuleToggleRow("Matikan Senjata Mainan", rules.disableWeapons) {
                onUpdateRules(rules.copy(disableWeapons = it))
            }
            RuleToggleRow("Lindungi Rakan Sepasukan (No Friendly Fire)", rules.disableFriendlyFire) {
                onUpdateRules(rules.copy(disableFriendlyFire = it))
            }
            RuleToggleRow("Matikan Balingan Item", rules.disableItemThrow) {
                onUpdateRules(rules.copy(disableItemThrow = it))
            }
            HorizontalDivider(color = Color(0xFFF59E0B).copy(alpha = 0.3f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Maximum HP: ${rules.maxHpPercent}%", fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(50, 100, 150, 200).forEach { hp ->
                        FilterChip(
                            selected = rules.maxHpPercent == hp,
                            onClick = { onUpdateRules(rules.copy(maxHpPercent = hp)) },
                            label = { Text("$hp%", maxLines = 1, softWrap = false) }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stamina Maksimum: ${rules.maxStaminaPercent}%", fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(100, 150, 200).forEach { st ->
                        FilterChip(
                            selected = rules.maxStaminaPercent == st,
                            onClick = { onUpdateRules(rules.copy(maxStaminaPercent = st)) },
                            label = { Text("$st%", maxLines = 1, softWrap = false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF451A03), modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { SoundHelper.playPop(); onCheckedChange(it) })
    }
}

@Composable
private fun WifiChatPane(
    wifiState: WifiRoomState,
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickPhrases = listOf("Jom mula!", "Sedia!", "GGWP! 🎉", "Rematch!", "Hebat!")
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(wifiState.chatMessages, key = { it.id }) { msg ->
                    Text(
                        text = "${msg.sender}: ${msg.text}",
                        fontSize = 13.sp,
                        fontWeight = if (msg.isSystem) FontWeight.Bold else FontWeight.Normal,
                        color = if (msg.isSystem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickPhrases.forEach { q ->
                OutlinedButton(
                    onClick = { SoundHelper.playPop(); onSend(q) },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(q, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { if (it.length <= 120) onChatInputChange(it) },
                placeholder = { Text("Tulis mesej bilik WiFi...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (chatInput.isNotBlank()) {
                        SoundHelper.playPop()
                        onSend(chatInput)
                    }
                },
                modifier = Modifier.height(52.dp)
            ) {
                Text("Hantar", maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
fun InvitationPopupDialog(
    invite: RoomInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    // Inspired by SmartFoxServer Game Invitation Popup (Reference Image 7)
    var secondsLeft by remember(invite) { mutableStateOf(invite.expiresInSec) }

    LaunchedEffect(invite) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        onDecline()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF59E0B),
            border = BorderStroke(3.dp, Color(0xFF78350F)),
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📨 JEMPUTAN PERMAINAN", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF451A03))
                HorizontalDivider(color = Color(0xFF78350F))
                Text(
                    text = "Anda dijemput oleh ${invite.hostName} untuk bermain '${invite.gameTitle}'!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF451A03),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Jemputan akan tamat dalam $secondsLeft saat...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF78350F)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { SoundHelper.playFanfare(); onAccept() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Terima", color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = { SoundHelper.playPop(); onDecline() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Tolak", color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}
