package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun BowlingGame(
    input: GameInputState,
    onScoreUpdated: (Int) -> Unit,
    onStatusChanged: (String) -> Unit
) {
    var aimX by remember { mutableStateOf(0f) } // -0.6..0.6
    var ballZ by remember { mutableStateOf(0f) } // 0 = start, 1 = pins
    var isRolling by remember { mutableStateOf(false) }
    var pinsUp by remember { mutableStateOf(List(10) { true }) }
    var score by remember { mutableStateOf(0) }
    var lastActionHandled by remember { mutableStateOf(input.actionCount) }

    // 10 pin coordinates (x, rowDepth)
    val pinCoords = remember {
        listOf(
            0f to 0f,
            -0.14f to 0.08f, 0.14f to 0.08f,
            -0.28f to 0.16f, 0f to 0.16f, 0.28f to 0.16f,
            -0.42f to 0.24f, -0.14f to 0.24f, 0.14f to 0.24f, 0.42f to 0.24f
        )
    }

    LaunchedEffect(Unit) {
        onStatusChanged("Halakan kiri/kanan & leret ke atas (atau tekan Aksi) untuk baling!")
        while (true) {
            delay(20L)
            if (!isRolling) {
                aimX = (aimX + input.moveX * 0.025f).coerceIn(-0.6f, 0.6f)
                if (input.actionCount != lastActionHandled || input.moveY < -0.5f) {
                    lastActionHandled = input.actionCount
                    isRolling = true
                    ballZ = 0f
                    SoundHelper.playPop()
                }
            } else {
                ballZ += 0.045f
                if (ballZ >= 1f) {
                    // Hit pins near aimX
                    var knocked = 0
                    val updated = pinsUp.mapIndexed { idx, up ->
                        if (!up) false
                        else {
                            val px = pinCoords[idx].first
                            if (abs(px - aimX) < 0.34f) {
                                knocked++
                                false
                            } else true
                        }
                    }
                    pinsUp = updated
                    if (knocked > 0) {
                        score += knocked * 5
                        onScoreUpdated(score)
                        SoundHelper.playFanfare()
                        onStatusChanged(
                            if (updated.none { it }) "🎳 STRIKE!! Semua pin jatuh! (+${knocked * 5})"
                            else "🎳 Kena $knocked pin! (+${knocked * 5})"
                        )
                    } else {
                        SoundHelper.playBoing()
                        onStatusChanged("Ops, masuk longkang! Cuba halakan ke tengah.")
                    }
                    if (updated.none { it }) {
                        delay(700L)
                        pinsUp = List(10) { true }
                    }
                    isRolling = false
                    ballZ = 0f
                }
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (!isRolling) {
                        aimX = (aimX + dragAmount.x / size.width.coerceAtLeast(1) * 1.4f).coerceIn(-0.6f, 0.6f)
                        if (dragAmount.y < -18f) {
                            isRolling = true
                            ballZ = 0f
                            SoundHelper.playPop()
                        }
                    }
                }
            }
    ) {
        drawRect(color = Color(0xFF0F172A), size = size)

        val topW = size.width * 0.36f
        val botW = size.width * 0.84f
        val topY = size.height * 0.16f
        val botY = size.height * 0.92f
        val cx = size.width / 2f

        // 3D Perspective Bowling Lane
        val lanePath = Path().apply {
            moveTo(cx - topW / 2f, topY)
            lineTo(cx + topW / 2f, topY)
            lineTo(cx + botW / 2f, botY)
            lineTo(cx - botW / 2f, botY)
            close()
        }
        drawPath(
            path = lanePath,
            brush = Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFFF59E0B)))
        )

        // Gutters
        drawLine(Color(0xFF334155), Offset(cx - topW / 2f, topY), Offset(cx - botW / 2f, botY), strokeWidth = 10f)
        drawLine(Color(0xFF334155), Offset(cx + topW / 2f, topY), Offset(cx + botW / 2f, botY), strokeWidth = 10f)

        // Draw 10 pins with perspective
        pinCoords.forEachIndexed { idx, (px, depth) ->
            if (pinsUp[idx]) {
                val py = topY + (0.28f - depth) * (size.height * 0.28f)
                val sx = cx + px * (topW * 0.72f)
                // Pin body + red stripe
                drawCircle(color = Color.White, radius = 12f, center = Offset(sx, py))
                drawCircle(color = Color(0xFFEF4444), radius = 5f, center = Offset(sx, py - 4f))
            }
        }

        // Aim guide line
        if (!isRolling) {
            drawLine(
                color = Color(0xFF38BDF8).copy(alpha = 0.65f),
                start = Offset(cx + aimX * (botW * 0.36f), botY - 30f),
                end = Offset(cx + aimX * (topW * 0.72f), topY + 25f),
                strokeWidth = 4f
            )
        }

        // Bowling ball with perspective scale
        val scale = 1f - ballZ * 0.58f
        val ballRadius = 28f * scale
        val ballY = (botY - 36f) - ballZ * (botY - topY - 60f)
        val laneWidthAtZ = botW + (topW - botW) * ballZ
        val ballX = cx + aimX * (laneWidthAtZ * 0.38f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE879F9), Color(0xFFA855F7), Color(0xFF581C87)),
                center = Offset(ballX - ballRadius * 0.3f, ballY - ballRadius * 0.3f),
                radius = ballRadius * 1.3f
            ),
            radius = ballRadius,
            center = Offset(ballX, ballY)
        )
    }
}
