package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.model.GameRoom
import com.example.model.ProfileDefaults
import com.example.model.UserProfile
import com.example.utils.SoundHelper

enum class GameModeType { VS_BOT, LOCAL, LOBBY }

@Composable
fun RoomServiceDialog(
    currentMode: GameModeType,
    botDiff: String,
    roomCode: String,
    userProfile: UserProfile,
    isGoogleUser: Boolean,
    cloudRoom: GameRoom?,
    selectedGameId: String,
    onModeSelected: (GameModeType) -> Unit,
    onBotDiffChanged: (String) -> Unit,
    onRoomCodeChanged: (String) -> Unit,
    onCreateCloudRoom: (code: String, hostName: String, hostAvatar: String, gameId: String, onDone: (Boolean, String?) -> Unit) -> Unit,
    onJoinCloudRoom: (code: String, guestName: String, guestAvatar: String, onDone: (Boolean, String?) -> Unit) -> Unit,
    onToggleCloudReady: (code: String, ready: Boolean) -> Unit,
    onSendCloudEmote: (code: String, emote: String, senderName: String) -> Unit,
    onStartCloudGame: (code: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(currentMode) }
    var diffChoice by remember { mutableStateOf(botDiff) }
    var codeInput by remember { mutableStateOf(roomCode) }
    var lobbyCode by remember { mutableStateOf(if (roomCode.length == 4) roomCode else (1000..9999).random().toString()) }
    var isReady by remember { mutableStateOf(false) }
    var testBotJoined by remember { mutableStateOf(false) }
    var lastEmote by remember { mutableStateOf("Jom mula!") }
    var lobbyFeedback by remember { mutableStateOf<String?>(null) }
    var myAvatar by remember { mutableStateOf(userProfile.avatarEmoji) }

    val emotes = listOf("Jom mula!", "Hebat gila!", "Alamak!", "Nak rematch!", "GGWP! 🎉")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏠 Room Service & Mod",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        onClick = {
                            SoundHelper.playPop()
                            onDismiss()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Mode Tabs (Anti-squish nowrap scrollable row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        GameModeType.VS_BOT to "🤖 VS BOT",
                        GameModeType.LOCAL to "👥 Local 2P",
                        GameModeType.LOBBY to "🌐 Bilik Rakan"
                    ).forEach { (mode, label) ->
                        val isSelected = selectedTab == mode
                        Button(
                            onClick = {
                                SoundHelper.playPop()
                                selectedTab = mode
                                onModeSelected(mode)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                when (selectedTab) {
                    GameModeType.VS_BOT -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Pilih Tahap Cabaran BOT:", style = MaterialTheme.typography.titleMedium)
                            listOf(
                                "Santai (Mudah)" to "BOT bermain santai dan memberi reaksi lucu apabila kalah.",
                                "Mencabar (Sederhana)" to "BOT bermain seimbang — sesuai untuk latihan pantas.",
                                "Pakar (Sukar)" to "BOT bertindak pantas dan bijak untuk cabaran maksimum!"
                            ).forEach { (title, desc) ->
                                val active = diffChoice == title
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(2.dp, if (active) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            SoundHelper.playPop()
                                            diffChoice = title
                                            onBotDiffChanged(title)
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    GameModeType.LOCAL -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Mod Local Multiplayer (1 Peranti)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "• Permainan Papan & Strategi (Tic-Tac-Toe, Kad Uno, Imposter): mod Pass & Play bergilir-gilir.\n• Permainan Aksi & Refleks (Pong Neon, Ujian Refleks, Sumo): kawalan berkongsi / dua zon sentuh pada satu skrin!",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    GameModeType.LOBBY -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = if (isGoogleUser) "🌐 Bilik Rakan 4-Digit (Disegerakkan ke Awan)"
                                else "🌐 Bilik Rakan 4-Digit (Simulasi Setempat / Tetamu)",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Kod Bilik Anda:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = cloudRoom?.roomCode ?: lobbyCode,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            SoundHelper.playPop()
                                            lobbyCode = (1000..9999).random().toString()
                                            codeInput = lobbyCode
                                            onRoomCodeChanged(lobbyCode)
                                            if (isGoogleUser) {
                                                onCreateCloudRoom(lobbyCode, userProfile.displayName, myAvatar, selectedGameId) { ok, err ->
                                                    lobbyFeedback = if (ok) "Bilik awan $lobbyCode dibuka!" else err
                                                }
                                            } else {
                                                lobbyFeedback = "Bilik simulasi $lobbyCode dicipta!"
                                            }
                                        },
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("✨ Cipta Bilik", maxLines = 1, softWrap = false)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = codeInput,
                                    onValueChange = {
                                        val digits = it.filter { ch -> ch.isDigit() }.take(4)
                                        codeInput = digits
                                        if (digits.length == 4) onRoomCodeChanged(digits)
                                    },
                                    label = { Text("Kod Bilik 4-Digit") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        SoundHelper.playPop()
                                        if (codeInput.length == 4) {
                                            onRoomCodeChanged(codeInput)
                                            if (isGoogleUser) {
                                                onJoinCloudRoom(codeInput, userProfile.displayName, myAvatar) { ok, err ->
                                                    lobbyFeedback = if (ok) "Berjaya sertai bilik $codeInput!" else err
                                                }
                                            } else {
                                                lobbyFeedback = "Menyertai bilik simulasi $codeInput!"
                                            }
                                        } else {
                                            lobbyFeedback = "Sila masukkan tepat 4 angka."
                                        }
                                    },
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Text("Sertai", maxLines = 1, softWrap = false)
                                }
                            }

                            // Player Slots
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Slot Pemain Dalam Bilik:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "1. ${cloudRoom?.hostAvatar ?: myAvatar} ${cloudRoom?.hostName ?: userProfile.displayName} (Host • ✅ Sedia)",
                                        fontSize = 13.sp
                                    )
                                    val guestLabel = when {
                                        cloudRoom?.guestName != null ->
                                            "2. ${cloudRoom.guestAvatar ?: "🐼"} ${cloudRoom.guestName} (${if (cloudRoom.guestReady) "✅ Ready" else "⏳ Menunggu"})"
                                        testBotJoined ->
                                            "2. 🤖 BOT Rakan Uji (✅ Ready)"
                                        else ->
                                            "2. ⏳ Menunggu rakan menyertai kod ${cloudRoom?.roomCode ?: lobbyCode}..."
                                    }
                                    Text(text = guestLabel, fontSize = 13.sp)
                                }
                            }

                            Text("Pilih Avatar Bilik:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileDefaults.AVATARS.take(8).forEach { av ->
                                    val sel = myAvatar == av
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                SoundHelper.playPop()
                                                myAvatar = av
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(av, fontSize = 22.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isReady = !isReady
                                        SoundHelper.playPop()
                                        if (isGoogleUser && cloudRoom != null && cloudRoom.guestUid != null) {
                                            onToggleCloudReady(cloudRoom.roomCode, isReady)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isReady) Color(0xFF22C55E) else MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(if (isReady) "✅ Ready!" else "✋ Sedia (Ready)", maxLines = 1, softWrap = false)
                                }
                                OutlinedButton(
                                    onClick = {
                                        SoundHelper.playCoin()
                                        testBotJoined = !testBotJoined
                                        lobbyFeedback = if (testBotJoined) "Rakan uji BOT masuk bilik!" else "Rakan uji BOT keluar."
                                    },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(
                                        text = if (testBotJoined) "🤖 Buang Rakan Uji" else "➕ Tambah Rakan Uji / BOT",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Safe Quick Emote Bar (No free text for all-ages safety)
                            Text("Quick Emote (Mesra Semua Umur):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                emotes.forEach { em ->
                                    OutlinedButton(
                                        onClick = {
                                            lastEmote = em
                                            SoundHelper.playPop()
                                            if (isGoogleUser && cloudRoom != null) {
                                                onSendCloudEmote(cloudRoom.roomCode, em, userProfile.displayName)
                                            }
                                        },
                                        modifier = Modifier.height(44.dp)
                                    ) {
                                        Text(em, fontSize = 12.sp, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                            Text(
                                text = "💬 ${cloudRoom?.lastEmoteSender ?: userProfile.displayName}: \"${cloudRoom?.lastEmote ?: lastEmote}\"",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (lobbyFeedback != null) {
                                Text(lobbyFeedback!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        SoundHelper.playFanfare()
                        if (selectedTab == GameModeType.LOBBY && isGoogleUser && cloudRoom != null) {
                            onStartCloudGame(cloudRoom.roomCode)
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("room_service_confirm_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Simpan & Mula Main! 🎮", fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
