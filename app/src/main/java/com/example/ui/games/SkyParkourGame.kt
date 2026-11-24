package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay

@Composable
fun SkyParkourGame(
    input: GameInputState,
    onScoreUpdated: (Int) -> Unit,
    onStatusChanged: (String) -> Unit
) {
    // Player in normalized world space (x: -1..1, y: height above platform 0..1)
    var playerX by remember { mutableStateOf(0f) }
    var playerZ by remember { mutableStateOf(0f) } // Forward progress
    var jumpY by remember { mutableStateOf(0f) }
    var jumpVel by remember { mutableStateOf(0f) }
    var score by remember { mutableStateOf(0) }
    var lastActionHandled by remember { mutableStateOf(input.actionCount) }

    val platforms = remember {
        listOf(-0.45f, 0f, 0.45f, -0.35f, 0.35f, 0f, -0.45f, 0.45f, 0f)
    }

    LaunchedEffect(Unit) {
        onStatusChanged("Lompat di atas blok awan 3D! Guna Kiri/Kanan & Lompat.")
        while (true) {
            delay(20L)
            playerX = (playerX + input.moveX * 0.035f).coerceIn(-0.85f, 0.85f)
            playerZ += 0.018f + (if (input.moveY < 0f) 0.012f else 0f)

            if (input.actionCount != lastActionHandled || input.moveY < -0.5f) {
                lastActionHandled = input.actionCount
                if (jumpY <= 0.01f) {
                    jumpVel = 0.085f
                    SoundHelper.playBoing()
                }
            }

            if (jumpY > 0f || jumpVel > 0f) {
                jumpY += jumpVel
                jumpVel -= 0.0055f
                if (jumpY <= 0f) {
                    jumpY = 0f
                    jumpVel = 0f
                    val currentIndex = (playerZ.toInt()) % platforms.size
                    val targetX = platforms[currentIndex]
                    if (kotlin.math.abs(playerX - targetX) < 0.48f) {
                        score += 10
                        onScoreUpdated(score)
                        SoundHelper.playCoin()
                        onStatusChanged("☁️ Mendarat tepat di awan! Skor: $score")
                    } else {
                        SoundHelper.playPop()
                        onStatusChanged("Ops! Terlepas blok awan — cuba lompat tepat!")
                    }
                }
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                if (jumpY <= 0.01f) {
                    jumpVel = 0.085f
                    SoundHelper.playBoing()
                }
            }
    ) {
        drawRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF1E1B4B))),
            size = size
        )

        val horizonY = size.height * 0.25f
        val groundY = size.height * 0.85f

        // Render 5 upcoming 3D perspective cloud blocks
        val baseIdx = playerZ.toInt()
        for (i in 4 downTo 0) {
            val worldIdx = baseIdx + i
            val relZ = (worldIdx - playerZ) + 0.5f
            val t = (1f - (relZ / 5f)).coerceIn(0.05f, 1f)
            val screenY = horizonY + (groundY - horizonY) * t
            val blockW = (size.width * 0.28f) * t
            val blockH = 26f * t
            val laneX = platforms[worldIdx % platforms.size]
            val screenX = size.width / 2f + laneX * (size.width * 0.38f) * t

            // 3D depth edge
            drawRoundRect(
                color = Color(0xFF0284C7),
                topLeft = Offset(screenX - blockW / 2f, screenY + 6f * t),
                size = Size(blockW, blockH),
                cornerRadius = CornerRadius(10f, 10f)
            )
            // Top bright cloud surface
            drawRoundRect(
                color = if (worldIdx % 5 == 4) Color(0xFFFBBF24) else Color(0xFFE0F2FE),
                topLeft = Offset(screenX - blockW / 2f, screenY),
                size = Size(blockW, blockH),
                cornerRadius = CornerRadius(10f, 10f)
            )
        }

        // Player shadow + 3D sphere
        val pxScreen = size.width / 2f + playerX * (size.width * 0.36f)
        val pyBase = groundY - 24f
        val pyJump = pyBase - jumpY * (size.height * 0.42f)

        drawOval(
            color = Color.Black.copy(alpha = (0.4f - jumpY * 0.3f).coerceIn(0.1f, 0.4f)),
            topLeft = Offset(pxScreen - 22f, pyBase + 8f),
            size = Size(44f, 14f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFFBBF24), Color(0xFFD97706)),
                center = Offset(pxScreen - 8f, pyJump - 8f),
                radius = 32f
            ),
            radius = 24f,
            center = Offset(pxScreen, pyJump)
        )
    }
}
