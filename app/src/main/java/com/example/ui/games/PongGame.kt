package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.components.GameModeType
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay

@Composable
fun PongGame(
    input: GameInputState,
    gameMode: GameModeType,
    botDiff: String,
    onScoreUpdated: (Int) -> Unit,
    onStatusChanged: (String) -> Unit
) {
    // Normalized 0f..1f coordinates so resizing/orientation never breaks bounds!
    var playerX by remember { mutableStateOf(0.5f) }
    var topPaddleX by remember { mutableStateOf(0.5f) }
    var ballX by remember { mutableStateOf(0.5f) }
    var ballY by remember { mutableStateOf(0.5f) }
    var ballVx by remember { mutableStateOf(0.011f) }
    var ballVy by remember { mutableStateOf(0.015f) }
    var score by remember { mutableStateOf(0) }

    val botTrackSpeed = when {
        botDiff.contains("Santai") -> 0.055f
        botDiff.contains("Pakar") -> 0.18f
        else -> 0.11f
    }

    LaunchedEffect(gameMode, botDiff) {
        onStatusChanged(
            if (gameMode == GameModeType.LOCAL) "Local 2P: Leret bahagian atas (P2) & bawah (P1)!"
            else "VS BOT ($botDiff): Tangkis bola neon dengan paddle biru!"
        )
        while (true) {
            delay(16L)
            playerX = (playerX + input.moveX * 0.025f).coerceIn(0.12f, 0.88f)

            ballX += ballVx
            ballY += ballVy

            if (ballX <= 0.04f || ballX >= 0.96f) {
                ballVx = -ballVx
                ballX = ballX.coerceIn(0.04f, 0.96f)
                SoundHelper.playPop()
            }

            if (gameMode != GameModeType.LOCAL) {
                topPaddleX += (ballX - topPaddleX) * botTrackSpeed
                topPaddleX = topPaddleX.coerceIn(0.12f, 0.88f)
            }

            // Bottom paddle collision (y ~ 0.90)
            if (ballVy > 0f && ballY in 0.87f..0.93f && kotlin.math.abs(ballX - playerX) < 0.14f) {
                ballVy = -kotlin.math.abs(ballVy) * 1.03f
                ballVx += (ballX - playerX) * 0.04f
                score += 5
                onScoreUpdated(score)
                SoundHelper.playCoin()
                onStatusChanged("🏓 Rali mantap! Skor: $score")
            }

            // Top paddle collision (y ~ 0.10)
            if (ballVy < 0f && ballY in 0.07f..0.13f && kotlin.math.abs(ballX - topPaddleX) < 0.14f) {
                ballVy = kotlin.math.abs(ballVy)
                SoundHelper.playPop()
            }

            // Out of bounds
            if (ballY < 0.02f) {
                score += 15
                onScoreUpdated(score)
                SoundHelper.playFanfare()
                onStatusChanged("🎉 Gol! Lawan terlepas bola! (+15)")
                ballX = 0.5f; ballY = 0.5f; ballVx = 0.011f; ballVy = 0.014f
            } else if (ballY > 0.98f) {
                SoundHelper.playBoing()
                onStatusChanged("😅 Terlepas! Cuba lagi!")
                ballX = 0.5f; ballY = 0.5f; ballVx = -0.011f; ballVy = -0.014f
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(gameMode) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dxNorm = dragAmount.x / size.width.coerceAtLeast(1)
                    if (gameMode == GameModeType.LOCAL && change.position.y < size.height / 2f) {
                        topPaddleX = (topPaddleX + dxNorm).coerceIn(0.12f, 0.88f)
                    } else {
                        playerX = (playerX + dxNorm).coerceIn(0.12f, 0.88f)
                    }
                }
            }
    ) {
        drawRect(color = Color(0xFF090D16), size = size)

        // Center dashed line
        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 4f
        )

        val paddleW = size.width * 0.22f
        val paddleH = 16f

        // Top Paddle (Bot / P2)
        drawRoundRect(
            color = Color(0xFFEF4444),
            topLeft = Offset(topPaddleX * size.width - paddleW / 2f, size.height * 0.08f),
            size = Size(paddleW, paddleH),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Bottom Paddle (P1)
        drawRoundRect(
            color = Color(0xFF38BDF8),
            topLeft = Offset(playerX * size.width - paddleW / 2f, size.height * 0.90f),
            size = Size(paddleW, paddleH),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Glowing Neon Ball
        val bx = ballX * size.width
        val by = ballY * size.height
        drawCircle(color = Color(0xFFFBBF24).copy(alpha = 0.3f), radius = 20f, center = Offset(bx, by))
        drawCircle(color = Color(0xFFFBBF24), radius = 11f, center = Offset(bx, by))
    }
}
