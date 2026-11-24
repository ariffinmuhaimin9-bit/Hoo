package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.model.ProfileDefaults
import com.example.utils.SoundHelper
import com.google.firebase.auth.FirebaseUser

@Composable
fun AuthScreen(
    onGoogleSuccess: (FirebaseUser) -> Unit,
    onContinueAsGuest: (guestName: String, guestAvatar: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showGuestCustomize by remember { mutableStateOf(false) }
    var guestName by remember { mutableStateOf("Tetamu Ceria") }
    var guestAvatar by remember { mutableStateOf("🦊") }

    // Silent auto sign-in check on startup
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onGoogleSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "authMascot")
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFF1E1B4B).copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "Banner MiniVerse Hub",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xCC0F172A))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = guestAvatar,
                            fontSize = 36.sp,
                            modifier = Modifier.offset(y = bobOffset.dp)
                        )
                        Column {
                            Text(
                                text = "MiniVerse Hub",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "134+ Game 3D & 2D • Ramah Semua Umur",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }
                }

                Text(
                    text = "Pilih cara masuk untuk menyimpan profil tersuai dan skor anda!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Google Sign-In Button
                Button(
                    onClick = {
                        SoundHelper.playPop()
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = { user ->
                                isLoading = false
                                SoundHelper.playFanfare()
                                onGoogleSuccess(user)
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "🔐 Log Masuk dengan Google (Awan)",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Guest Mode Customization Toggle
                if (!showGuestCustomize) {
                    OutlinedButton(
                        onClick = {
                            SoundHelper.playPop()
                            showGuestCustomize = true
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("guest_mode_button")
                    ) {
                        Text(
                            text = "🎮 Main sebagai Tetamu (Guest)",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "✨ Profil Pantas Tetamu",
                                style = MaterialTheme.typography.titleMedium
                            )
                            OutlinedTextField(
                                value = guestName,
                                onValueChange = { if (it.length <= 30) guestName = it },
                                label = { Text("Nama Samaran Anda") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("guest_name_input")
                            )
                            Text("Pilih Avatar:", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileDefaults.AVATARS.forEach { emoji ->
                                    val selected = guestAvatar == emoji
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .clickable {
                                                SoundHelper.playPop()
                                                guestAvatar = emoji
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 24.sp)
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    SoundHelper.playFanfare()
                                    onContinueAsGuest(
                                        guestName.trim().ifBlank { "Tetamu Ceria" },
                                        guestAvatar
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("guest_start_button")
                            ) {
                                Text(
                                    text = "Mula Main Sekarang! 🚀",
                                    fontWeight = FontWeight.Bold,
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
}
