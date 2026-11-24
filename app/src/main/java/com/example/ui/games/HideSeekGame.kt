package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.random.Random

@Composable
fun HideSeekGame(
    input: GameInputState,
    onScoreUpdated: (Int) -> Unit,
    onStatusChanged: (String) -> Unit
) {
    var seekerX by remember { mutableStateOf(0.2f) }
    var seekerY by remember { mutableStateOf(0.5f) }
    var hiderX by remember { mutableStateOf(0.8f) }
    var hiderY by remember { mutableStateOf(0.5f) }
    var score by remember { mutableStateOf(0) }
    var lastActionHandled by remember { mutableStateOf(input.actionCount) }

    // 3D obstacle blocks
    val bushes = remember {
        listOf(
            Offset(0.32f, 0.30f),
            Offset(0.68f, 0.32f),
            Offset(0.50f, 0.68f)
        )
    }

    LaunchedEffect(Unit) {
        onStatusChanged("Kejar & tangkap rakan sembunyi di taman 3D! Guna D-Pad + Aksi.")
        while (true) {
            delay(25L)
            seekerX = (seekerX + input.moveX * 0.018f).coerceIn(0.08f, 0.92f)
            seekerY = (seekerY + input.moveY * 0.018f).coerceIn(0.12f, 0.88f)

            // Hider runs away gently
            val dx = hiderX - seekerX
            val dy = hiderY - seekerY
            val d = hypot(dx, dy).coerceAtLeast(0.01f)
            val flee = if (d < 0.35f) 0.012f else 0.005f
            hiderX = (hiderX + (dx / d) * flee + (Random.nextFloat() - 0.5f) * 0.01f).coerceIn(0.1f, 0.9f)
            hiderY = (hiderY + (dy / d) * flee + (Random.nextFloat() - 0.5f) * 0.01f).coerceIn(0.15f, 0.85f)

            val caught = hypot(seekerX - hiderX, seekerY - hiderY) < 0.10f ||
                (input.actionCount != lastActionHandled && hypot(seekerX - hiderX, seekerY - hiderY) < 0.18f)
            lastActionHandled = input.actionCount

            if (caught) {
                score += 25
                onScoreUpdated(score)
                SoundHelper.playFanfare()
                onStatusChanged("🙈 Jumpa pun! (+25 • Skor: $score)")
                hiderX = Random.nextFloat() * 0.7f + 0.15f
                hiderY = Random.nextFloat() * 0.6f + 0.2f
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    seekerX = (seekerX + dragAmount.x / size.width.coerceAtLeast(1)).coerceIn(0.08f, 0.92f)
                    seekerY = (seekerY + dragAmount.y / size.height.coerceAtLeast(1)).coerceIn(0.12f, 0.88f)
                }
            }
    ) {
        drawRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF064E3B), Color(0xFF0F172A))),
            size = size
        )

        // 3D bushes / cover boxes
        bushes.forEach { b ->
            val bx = b.x * size.width
            val by = b.y * size.height
            drawRoundRect(
                color = Color(0xFF065F46),
                topLeft = Offset(bx - 36f, by - 16f),
                size = Size(72f, 44f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = Color(0xFF10B981),
                topLeft = Offset(bx - 36f, by - 26f),
                size = Size(72f, 38f),
                cornerRadius = CornerRadius(14f, 14f)
            )
        }

        // Hider (Purple 3D sphere)
        val hx = hiderX * size.width
        val hy = hiderY * size.height
        drawCircle(Color.Black.copy(alpha = 0.35f), 20f, Offset(hx, hy + 10f))
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White, Color(0xFFA855F7), Color(0xFF6B21A8))),
            radius = 22f,
            center = Offset(hx, hy)
        )

        // Seeker (Sky Blue 3D sphere + radar ring)
        val sx = seekerX * size.width
        val sy = seekerY * size.height
        drawCircle(Color(0xFF38BDF8).copy(alpha = 0.25f), 42f, Offset(sx, sy))
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White, Color(0xFF38BDF8), Color(0xFF0369A1))),
            radius = 22f,
            center = Offset(sx, sy)
        )
    }
}
