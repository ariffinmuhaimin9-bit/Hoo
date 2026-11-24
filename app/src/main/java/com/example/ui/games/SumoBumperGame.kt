package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.components.GameModeType
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.random.Random

@Composable
fun SumoBumperGame(
    input: GameInputState,
    gameMode: GameModeType,
    botDiff: String,
    onScoreUpdated: (Int) -> Unit,
    onStatusChanged: (String) -> Unit
) {
    // Normalized coordinates (-1f..1f) inside floating 3D ring
    var px by remember { mutableStateOf(-0.42f) }
    var py by remember { mutableStateOf(0f) }
    var pvx by remember { mutableStateOf(0f) }
    var pvy by remember { mutableStateOf(0f) }

    var bx by remember { mutableStateOf(0.42f) }
    var by by remember { mutableStateOf(0f) }
    var bvx by remember { mutableStateOf(0f) }
    var bvy by remember { mutableStateOf(0f) }

    var playerScore by remember { mutableStateOf(0) }
    var botScore by remember { mutableStateOf(0) }
    var lastActionHandled by remember { mutableStateOf(input.actionCount) }

    val botSpeed = when {
        botDiff.contains("Santai") -> 0.009f
        botDiff.contains("Pakar") -> 0.019f
        else -> 0.014f
    }

    LaunchedEffect(gameMode, botDiff) {
        onStatusChanged(
            if (gameMode == GameModeType.LOCAL) "Local 2P: Gerak & tolak lawan keluar gelanggang!"
            else "VS BOT ($botDiff): Tolak BOT keluar gelanggang 3D!"
        )
        while (true) {
            delay(16L)
            // Player input force + dash
            val accel = 0.0042f
            pvx = (pvx + input.moveX * accel) * 0.90f
            pvy = (pvy + input.moveY * accel) * 0.90f

            if (input.actionCount != lastActionHandled) {
                lastActionHandled = input.actionCount
                val dx = bx - px
                val dy = by - py
                val d = hypot(dx, dy).coerceAtLeast(0.01f)
                pvx += (dx / d) * 0.032f
                pvy += (dy / d) * 0.032f
                SoundHelper.playBoing()
            }

            // Bot AI
            val toPx = px - bx
            val toPy = py - by
            val dist = hypot(toPx, toPy).coerceAtLeast(0.01f)
            val jitter = if (botDiff.contains("Santai")) Random.nextFloat() * 0.004f - 0.002f else 0f
            bvx = (bvx + (toPx / dist) * botSpeed * 0.25f + jitter) * 0.90f
            bvy = (bvy + (toPy / dist) * botSpeed * 0.25f + jitter) * 0.90f

            px += pvx
            py += pvy
            bx += bvx
            by += bvy

            // Bumper collision
            val cdx = bx - px
            val cdy = by - py
            val cdist = hypot(cdx, cdy)
            val minDist = 0.24f
            if (cdist < minDist && cdist > 0.0001f) {
                val nx = cdx / cdist
                val ny = cdy / cdist
                val overlap = (minDist - cdist) * 0.5f
                px -= nx * overlap
                py -= ny * overlap
                bx += nx * overlap
                by += ny * overlap
                val push = 0.036f
                pvx -= nx * push
                pvy -= ny * push
                bvx += nx * push
                bvy += ny * push
                SoundHelper.playPop()
            }

            // Ring out checks
            if (hypot(bx, by) > 0.92f) {
                playerScore += 10
                onScoreUpdated(playerScore)
                SoundHelper.playFanfare()
                onStatusChanged("😵 BOT terkeluar! \"Alamak licinnya!\" (+10)")
                px = -0.4f; py = 0f; pvx = 0f; pvy = 0f
                bx = 0.4f; by = 0f; bvx = 0f; bvy = 0f
            } else if (hypot(px, py) > 0.92f) {
                botScore += 10
                SoundHelper.playBoing()
                onStatusChanged("🤖 BOT ketawa: \"Hehe cuba lagi!\" (BOT: $botScore)")
                px = -0.4f; py = 0f; pvx = 0f; pvy = 0f
                bx = 0.4f; by = 0f; bvx = 0f; bvy = 0f
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    pvx += (dragAmount.x / size.width.coerceAtLeast(1)) * 0.14f
                    pvy += (dragAmount.y / size.height.coerceAtLeast(1)) * 0.14f
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rx = size.minDimension * 0.40f
        val ry = rx * 0.68f // Pseudo-3D isometric tilt

        // Background sky glow
        drawRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))),
            size = size
        )

        // Floating 3D platform depth thickness
        drawOval(
            color = Color(0xFF1E293B),
            topLeft = Offset(cx - rx, cy - ry + 18f),
            size = Size(rx * 2f, ry * 2f)
        )
        // Platform surface
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
                center = Offset(cx, cy),
                radius = rx
            ),
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2f, ry * 2f)
        )
        // Outer neon ring
        drawOval(
            color = Color(0xFFFBBF24),
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2f, ry * 2f),
            style = Stroke(width = 7f)
        )
        drawOval(
            color = Color(0xFF38BDF8).copy(alpha = 0.4f),
            topLeft = Offset(cx - rx * 0.55f, cy - ry * 0.55f),
            size = Size(rx * 1.1f, ry * 1.1f),
            style = Stroke(width = 3f)
        )

        val ballRadius = (rx * 0.14f).coerceIn(18f, 38f)

        // Draw sorted by Y for 3D depth
        val entities = listOf(
            Triple(px, py, true),
            Triple(bx, by, false)
        ).sortedBy { it.second }

        entities.forEach { (ex, ey, isPlayer) ->
            val screenX = cx + ex * rx
            val screenY = cy + ey * ry
            // Shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(screenX - ballRadius * 0.9f, screenY + ballRadius * 0.35f),
                size = Size(ballRadius * 1.8f, ballRadius * 0.7f)
            )
            // 3D Sphere body
            val mainColor = if (isPlayer) Color(0xFF38BDF8) else Color(0xFFEF4444)
            val darkColor = if (isPlayer) Color(0xFF0369A1) else Color(0xFF991B1B)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, mainColor, darkColor),
                    center = Offset(screenX - ballRadius * 0.3f, screenY - ballRadius * 0.35f),
                    radius = ballRadius * 1.25f
                ),
                radius = ballRadius,
                center = Offset(screenX, screenY)
            )
        }
    }
}
