package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(onLoaded: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    var captionIndex by remember { mutableStateOf(0) }
    var isJumping by remember { mutableStateOf(false) }

    val captions = remember {
        listOf(
            "Sedang memujuk BOT supaya tidak menipu...",
            "Menggilap bola 3D sampai berkilat...",
            "Menyusun blok logik sebelum lawan bangun...",
            "Menyiapkan snek maya untuk semua pemain...",
            "Memeriksa pelancar MiniVerse Hub..."
        )
    }

    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(30)
            progress = (progress + 0.025f).coerceAtMost(1f)
        }
        delay(220)
        onLoaded()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1400)
            captionIndex = (captionIndex + 1) % captions.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mascot")
    val bounceAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -16f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    val spinAngle by animateFloatAsState(
        targetValue = if (isJumping) 360f else 0f,
        animationSpec = tween(500),
        label = "spin"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF0F172A))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.09f))
                    .clickable {
                        isJumping = true
                        SoundHelper.playBoing()
                    }
                    .testTag("loading_mascot_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🦊",
                    fontSize = 66.sp,
                    modifier = Modifier
                        .offset(y = if (isJumping) (-28).dp else bounceAnim.dp)
                        .rotate(spinAngle)
                )
                LaunchedEffect(isJumping) {
                    if (isJumping) {
                        delay(550)
                        isJumping = false
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "MiniVerse Hub",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = captions[captionIndex],
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFDE68A),
                textAlign = TextAlign.Center,
                minLines = 2,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFBBF24), Color(0xFF38BDF8), Color(0xFFA855F7))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "💡 Ketik maskot di atas untuk lompatan ajaib!",
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}
