package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.onGoogleSignInClicked
import com.example.data.HubExtrasState
import com.example.model.ProfileDefaults
import com.example.model.UserProfile
import com.example.utils.SoundHelper
import com.google.firebase.auth.FirebaseUser

@Composable
fun ProfileDialog(
    profile: UserProfile,
    extras: HubExtrasState,
    isGoogleUser: Boolean,
    onSaveProfile: (
        displayName: String,
        avatarEmoji: String,
        avatarColorHex: String,
        bio: String,
        favoriteBadge: String,
        handle: String,
        onlineVisible: Boolean,
        statusState: String,
        experienceTier: String,
        ratingStars: Int,
        onDone: (Boolean, String?) -> Unit
    ) -> Unit,
    onCheer: () -> Unit,
    onUpgradeToGoogle: (FirebaseUser) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var activeTab by remember { mutableStateOf(0) } // 0: Customize, 1: Kad Awam & Buddy, 2: Akaun
    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var avatarEmoji by remember(profile.avatarEmoji) { mutableStateOf(profile.avatarEmoji) }
    var avatarColorHex by remember(profile.avatarColorHex) { mutableStateOf(profile.avatarColorHex) }
    var bio by remember(profile.bio) { mutableStateOf(profile.bio) }
    var favoriteBadge by remember(profile.favoriteBadge) { mutableStateOf(profile.favoriteBadge) }

    var handle by remember(extras.handle) { mutableStateOf(extras.handle) }
    var onlineVisible by remember(extras.onlineVisible) { mutableStateOf(extras.onlineVisible) }
    var statusState by remember(extras.statusState) { mutableStateOf(extras.statusState) }
    var experienceTier by remember(extras.experienceTier) { mutableStateOf(extras.experienceTier) }
    var ratingStars by remember(extras.ratingStars) { mutableStateOf(extras.ratingStars) }

    var isSaving by remember { mutableStateOf(false) }
    var isLinkingGoogle by remember { mutableStateOf(false) }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }

    val parsedColor = remember(avatarColorHex) {
        try {
            Color(android.graphics.Color.parseColor(avatarColorHex))
        } catch (_: Exception) {
            Color(0xFF38BDF8)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(14.dp),
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
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎨 Tetapan Profil Pemain", style = MaterialTheme.typography.titleLarge)
                    IconButton(
                        onClick = { SoundHelper.playPop(); onDismiss() },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Rec Room Profile Tabs (Image 4 & 6)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        0 to "✨ Sesuaikan Profil",
                        1 to "🪪 Kad Awam & Buddy",
                        2 to "⚙️ Akaun"
                    ).forEach { (idx, label) ->
                        FilterChip(
                            selected = activeTab == idx,
                            onClick = { SoundHelper.playPop(); activeTab = idx },
                            label = { Text(label, maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // Public Profile Banner (Inspired by Image 6)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = parsedColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.5.dp, parsedColor.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(parsedColor.copy(alpha = 0.32f))
                                .border(2.5.dp, parsedColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(avatarEmoji, fontSize = 34.sp)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFEA580C)) {
                                    Text(
                                        text = "Lv.${extras.level}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                                Text(displayName.ifBlank { "Pemain Ceria" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            }
                            Text(
                                text = "$handle • ${if (onlineVisible) "🟢 $statusState" else "⚪ Luar Talian"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "🏅 $favoriteBadge • $experienceTier • ${"★".repeat(ratingStars)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                when (activeTab) {
                    0 -> {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { if (it.length <= 30) displayName = it },
                            label = { Text("Nama Paparan (1-30 aksara)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                        )
                        OutlinedTextField(
                            value = handle,
                            onValueChange = { if (it.length <= 20) handle = it },
                            label = { Text("@Handle Unik") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { if (it.length <= 140) bio = it },
                            label = { Text("Mood / Bio Pemain") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("profile_bio_input")
                        )

                        Text("Pilih Avatar Mascot:", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileDefaults.AVATARS.forEach { emoji ->
                                val selected = avatarEmoji == emoji
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) parsedColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(if (selected) 2.dp else 0.dp, if (selected) parsedColor else Color.Transparent, CircleShape)
                                        .clickable { SoundHelper.playPop(); avatarEmoji = emoji },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 24.sp)
                                }
                            }
                        }

                        Text("Warna Bingkai Avatar:", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProfileDefaults.AVATAR_COLORS.forEach { hex ->
                                val colorObj = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                                val selected = avatarColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(colorObj)
                                        .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.Transparent, CircleShape)
                                        .clickable { SoundHelper.playPop(); avatarColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) Text("✓", color = Color.White, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Text("Gelaran / Lencana:", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileDefaults.BADGES.forEach { badge ->
                                FilterChip(
                                    selected = favoriteBadge == badge,
                                    onClick = { SoundHelper.playPop(); favoriteBadge = badge },
                                    label = { Text(badge, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }

                    1 -> {
                        // Inspired by SmartFox Buddy Profile + Rec Room Public Stats (Images 1 & 6)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tunjuk Status Online kepada Rakan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Switch(checked = onlineVisible, onCheckedChange = { SoundHelper.playPop(); onlineVisible = it })
                        }

                        Text("Status Semasa (State):", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileDefaults.STATUS_STATES.forEach { st ->
                                FilterChip(
                                    selected = statusState == st,
                                    onClick = { SoundHelper.playPop(); statusState = st },
                                    label = { Text(st, maxLines = 1, softWrap = false) }
                                )
                            }
                        }

                        Text("Tahap Pengalaman (Experience):", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileDefaults.EXPERIENCE_TIERS.forEach { tier ->
                                FilterChip(
                                    selected = experienceTier == tier,
                                    onClick = { SoundHelper.playPop(); experienceTier = tier },
                                    label = { Text(tier, maxLines = 1, softWrap = false) }
                                )
                            }
                        }

                        Text("Penarafan Bintang Pemain:", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..5).forEach { star ->
                                Text(
                                    text = if (star <= ratingStars) "★" else "☆",
                                    fontSize = 28.sp,
                                    color = Color(0xFFFBBF24),
                                    modifier = Modifier.clickable { SoundHelper.playCoin(); ratingStars = star }
                                )
                            }
                        }

                        // Stats & Cheer row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile("🏆 Skor", "${profile.totalScore}", Modifier.weight(1f))
                            StatTile("🎮 Main", "${profile.gamesPlayed}", Modifier.weight(1f))
                            StatTile("👍 Cheer", "${extras.cheerCount}", Modifier.weight(1f))
                            StatTile("🪙 Syiling", "${extras.coins}", Modifier.weight(1f))
                        }
                        OutlinedButton(
                            onClick = { SoundHelper.playCoin(); onCheer() },
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text("👍 Beri Cheer & Tambah Semangat! (+1)", maxLines = 1, softWrap = false)
                        }
                    }

                    2 -> {
                        Text(
                            text = if (isGoogleUser) "☁️ Akaun Google Aktif — Data disegerakkan ke Firebase Firestore."
                            else "📱 Mod Tetamu Setempat — Data disimpan selamat pada peranti ini.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isGoogleUser) {
                            OutlinedButton(
                                onClick = {
                                    SoundHelper.playPop()
                                    isLinkingGoogle = true
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = { user ->
                                            isLinkingGoogle = false
                                            SoundHelper.playFanfare()
                                            onUpgradeToGoogle(user)
                                            onDismiss()
                                        },
                                        onAuthError = { msg -> isLinkingGoogle = false; feedbackMsg = msg },
                                        scope = scope,
                                        onAuthCancelled = { isLinkingGoogle = false }
                                    )
                                },
                                enabled = !isLinkingGoogle,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("upgrade_google_button")
                            ) {
                                Text(
                                    text = if (isLinkingGoogle) "Menyambung..." else "🔐 Log Masuk Google (Simpan ke Awan)",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        TextButton(
                            onClick = { SoundHelper.playPop(); onSignOut() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
                        ) {
                            Text(
                                text = if (isGoogleUser) "🚪 Log Keluar Google" else "🚪 Tukar Akaun / Keluar Mod Tetamu",
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (feedbackMsg != null) {
                    Text(feedbackMsg!!, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (displayName.trim().isEmpty()) {
                            feedbackMsg = "Sila masukkan nama paparan anda."
                            return@Button
                        }
                        SoundHelper.playPop()
                        isSaving = true
                        onSaveProfile(
                            displayName, avatarEmoji, avatarColorHex, bio, favoriteBadge,
                            handle, onlineVisible, statusState, experienceTier, ratingStars
                        ) { ok, err ->
                            isSaving = false
                            if (ok) {
                                SoundHelper.playFanfare()
                                onDismiss()
                            } else {
                                feedbackMsg = err ?: "Gagal menyimpan profil."
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_profile_button")
                ) {
                    Text(if (isSaving) "Menyimpan..." else "💾 Simpan Profil Tersuai", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, maxLines = 1, softWrap = false)
            Text(value, fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}
