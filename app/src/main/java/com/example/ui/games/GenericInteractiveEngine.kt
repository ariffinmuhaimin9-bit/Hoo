package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameItem
import com.example.ui.components.GameModeType
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private enum class SubEngineMode {
    SPACE_SHOOTER,
    RACER_DRIFT,
    GOAL_ARENA,
    COLLECT_360,
    QUIZ_PUZZLE
}

@Composable
fun GenericInteractiveEngine(
    game: GameItem,
    input: GameInputState,
    gameMode: GameModeType,
    botDiff: String,
    onScoreUpdated: (Int) -> Unit,
    onStatusChanged: (String) -> Unit
) {
    val engineMode = remember(game.id, game.category) {
        val id = game.id.lowercase()
        when {
            id.contains("space") || id.contains("jet") || id.contains("tank") || id.contains("cannon") ||
                id.contains("meteor") || id.contains("asteroid") || id.contains("pirate") || id.contains("laser") -> SubEngineMode.SPACE_SHOOTER
            id.contains("car") || id.contains("kart") || id.contains("race") || id.contains("ski") ||
                id.contains("kayak") || id.contains("hover") || id.contains("runner") || id.contains("sprint") -> SubEngineMode.RACER_DRIFT
            id.contains("soccer") || id.contains("hockey") || id.contains("golf") || id.contains("basket") ||
                id.contains("penalty") || id.contains("dodge") || id.contains("takraw") || id.contains("badminton") -> SubEngineMode.GOAL_ARENA
            game.category == "teka" || game.category == "papan" -> SubEngineMode.QUIZ_PUZZLE
            else -> SubEngineMode.COLLECT_360
        }
    }

    var playerX by remember(game.id) { mutableStateOf(0.5f) }
    var playerY by remember(game.id) { mutableStateOf(0.75f) }
    var targetX by remember(game.id) { mutableStateOf(0.30f) }
    var targetY by remember(game.id) { mutableStateOf(0.28f) }
    var obstacleX by remember(game.id) { mutableStateOf(0.65f) }
    var obstacleY by remember(game.id) { mutableStateOf(0.15f) }
    var bulletX by remember(game.id) { mutableStateOf(-1f) }
    var bulletY by remember(game.id) { mutableStateOf(-1f) }
    var botX by remember(game.id) { mutableStateOf(0.78f) }
    var botY by remember(game.id) { mutableStateOf(0.30f) }

    var score by remember(game.id) { mutableStateOf(0) }
    var botScore by remember(game.id) { mutableStateOf(0) }
    var combo by remember(game.id) { mutableStateOf(1) }
    var lastActionHandled by remember(game.id) { mutableStateOf(input.actionCount) }
    var angleRad by remember(game.id) { mutableStateOf(0f) }

    val primaryTheme = when (game.category) {
        "3d" -> Color(0xFFA855F7)
        "aksi" -> Color(0xFFEF4444)
        "papan" -> Color(0xFF38BDF8)
        "teka" -> Color(0xFF22C55E)
        else -> Color(0xFFF59E0B)
    }

    val botPace = when {
        botDiff.contains("Santai") -> 0.0055f
        botDiff.contains("Pakar") -> 0.015f
        else -> 0.010f
    }

    LaunchedEffect(game.id, gameMode, botDiff) {
        val hint = when (engineMode) {
            SubEngineMode.SPACE_SHOOTER -> "Guna Joystick 360° & tekan ⚡ AKSI untuk tembak sasaran!"
            SubEngineMode.RACER_DRIFT -> "Pandu litar 360°, elak halangan merah & kutip syiling turbo!"
            SubEngineMode.GOAL_ARENA -> "Tolak bola ke gawang emas atas menggunakan pergerakan 360°!"
            SubEngineMode.QUIZ_PUZZLE -> "Gerakkan penunjuk ke petak sasaran bernombor / bintang logik!"
            SubEngineMode.COLLECT_360 -> "Teroka arena 360° & kutip bintang emas sebelum lawan!"
        }
        onStatusChanged("${game.title}: $hint")

        while (true) {
            delay(20L)
            angleRad += 0.06f

            // Smooth 360 analog joystick movement
            playerX = (playerX + input.moveX * 0.024f).coerceIn(0.07f, 0.93f)
            playerY = (playerY + input.moveY * 0.024f).coerceIn(0.12f, 0.90f)

            // Action button trigger
            if (input.actionCount != lastActionHandled) {
                lastActionHandled = input.actionCount
                when (engineMode) {
                    SubEngineMode.SPACE_SHOOTER -> {
                        bulletX = playerX
                        bulletY = playerY - 0.04f
                        SoundHelper.playPop()
                    }
                    else -> {
                        // Dash toward target!
                        val dx = targetX - playerX
                        val dy = targetY - playerY
                        val d = hypot(dx, dy).coerceAtLeast(0.01f)
                        playerX = (playerX + (dx / d) * 0.14f).coerceIn(0.07f, 0.93f)
                        playerY = (playerY + (dy / d) * 0.14f).coerceIn(0.12f, 0.90f)
                        SoundHelper.playBoing()
                    }
                }
            }

            // Bullet movement in shooter mode
            if (engineMode == SubEngineMode.SPACE_SHOOTER && bulletY > 0f) {
                bulletY -= 0.045f
                if (hypot(bulletX - targetX, bulletY - targetY) < 0.10f) {
                    SoundHelper.playCoin()
                    score += 15 * combo
                    combo = (combo + 1).coerceAtMost(5)
                    onScoreUpdated(score)
                    bulletY = -1f
                    targetX = Random.nextFloat() * 0.76f + 0.12f
                    targetY = Random.nextFloat() * 0.45f + 0.15f
                    onStatusChanged("💥 Tembakan tepat! Kombo x$combo • Skor: $score")
                }
            }

            // Racer obstacle falling
            if (engineMode == SubEngineMode.RACER_DRIFT || engineMode == SubEngineMode.SPACE_SHOOTER) {
                obstacleY += 0.011f
                if (obstacleY > 0.95f) {
                    obstacleY = 0.08f
                    obstacleX = Random.nextFloat() * 0.80f + 0.10f
                }
                if (hypot(playerX - obstacleX, playerY - obstacleY) < 0.085f) {
                    SoundHelper.playBoing()
                    combo = 1
                    obstacleY = 0.08f
                    obstacleX = Random.nextFloat() * 0.80f + 0.10f
                    onStatusChanged("😅 Terlanggar halangan! Kombo diset semula.")
                }
            }

            // Rival Bot or Remote WiFi opponent
            if (input.remotePlayerName != null) {
                botX = (0.5f + input.remoteMoveX * 0.4f).coerceIn(0.08f, 0.92f)
                botY = (0.5f + input.remoteMoveY * 0.35f).coerceIn(0.14f, 0.88f)
                botScore = input.remoteScore
            } else {
                val bdx = targetX - botX
                val bdy = targetY - botY
                val bdist = hypot(bdx, bdy).coerceAtLeast(0.01f)
                botX = (botX + (bdx / bdist) * botPace).coerceIn(0.08f, 0.92f)
                botY = (botY + (bdy / bdist) * botPace).coerceIn(0.14f, 0.88f)
            }

            // Target capture
            if (hypot(playerX - targetX, playerY - targetY) < 0.10f) {
                SoundHelper.playCoin()
                score += 10 * combo
                combo = (combo + 1).coerceAtMost(5)
                onScoreUpdated(score)
                targetX = Random.nextFloat() * 0.76f + 0.12f
                targetY = Random.nextFloat() * 0.65f + 0.16f
                onStatusChanged("⭐ ${game.title}: Kombo x$combo • Skor: $score")
            } else if (input.remotePlayerName == null && hypot(botX - targetX, botY - targetY) < 0.085f) {
                botScore += 10
                combo = 1
                SoundHelper.playPop()
                targetX = Random.nextFloat() * 0.76f + 0.12f
                targetY = Random.nextFloat() * 0.65f + 0.16f
                onStatusChanged("🤖 Lawan dapat bintang! (Anda $score - $botScore Lawan)")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(game.id) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        playerX = (playerX + drag.x / size.width.coerceAtLeast(1)).coerceIn(0.07f, 0.93f)
                        playerY = (playerY + drag.y / size.height.coerceAtLeast(1)).coerceIn(0.12f, 0.90f)
                    }
                }
                .pointerInput(game.id) {
                    detectTapGestures { offset ->
                        playerX = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0.07f, 0.93f)
                        playerY = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0.12f, 0.90f)
                        SoundHelper.playPop()
                    }
                }
        ) {
            // Dynamic atmospheric backdrop
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF0B1120), primaryTheme.copy(alpha = 0.25f), Color(0xFF090D16))
                ),
                size = size
            )

            // Genre visuals
            when (engineMode) {
                SubEngineMode.RACER_DRIFT -> {
                    // Road lanes
                    drawLine(Color.White.copy(alpha = 0.22f), Offset(size.width * 0.2f, 0f), Offset(size.width * 0.2f, size.height), 4f)
                    drawLine(Color.White.copy(alpha = 0.22f), Offset(size.width * 0.8f, 0f), Offset(size.width * 0.8f, size.height), 4f)
                    drawLine(Color(0xFFFBBF24).copy(alpha = 0.45f), Offset(size.width * 0.5f, 0f), Offset(size.width * 0.5f, size.height), 3f)
                }
                SubEngineMode.GOAL_ARENA -> {
                    // Field & Goal box
                    drawRect(
                        color = Color(0xFF22C55E).copy(alpha = 0.18f),
                        topLeft = Offset(size.width * 0.1f, size.height * 0.12f),
                        size = Size(size.width * 0.8f, size.height * 0.78f),
                        style = Stroke(width = 4f)
                    )
                    drawRect(
                        color = Color(0xFFFBBF24).copy(alpha = 0.35f),
                        topLeft = Offset(size.width * 0.35f, size.height * 0.08f),
                        size = Size(size.width * 0.30f, 18f)
                    )
                }
                else -> {
                    for (i in 1..5) {
                        val y = size.height * (i / 6f)
                        drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, y), Offset(size.width, y), 2f)
                    }
                }
            }

            // Obstacle in Shooter / Racer
            if (engineMode == SubEngineMode.RACER_DRIFT || engineMode == SubEngineMode.SPACE_SHOOTER) {
                val ox = obstacleX * size.width
                val oy = obstacleY * size.height
                drawRoundRect(
                    color = Color(0xFFDC2626),
                    topLeft = Offset(ox - 20f, oy - 20f),
                    size = Size(40f, 40f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }

            // Bullet
            if (engineMode == SubEngineMode.SPACE_SHOOTER && bulletY > 0f) {
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = 9f,
                    center = Offset(bulletX * size.width, bulletY * size.height)
                )
            }

            // Orbiting Star Target
            val tx = targetX * size.width
            val ty = targetY * size.height
            val pulse = 26f + 5f * sin(angleRad)
            drawCircle(Color(0xFFFBBF24).copy(alpha = 0.25f), radius = pulse + 10f, center = Offset(tx, ty))
            drawCircle(Color(0xFFFBBF24), radius = 18f, center = Offset(tx, ty))
            drawCircle(Color.White, radius = 7f, center = Offset(tx + 8f * cos(angleRad), ty + 8f * sin(angleRad)))

            // Bot / WiFi Opponent Token
            val bx = botX * size.width
            val by = botY * size.height
            drawRoundRect(
                color = if (input.remotePlayerName != null) Color(0xFF22C55E) else Color(0xFFEF4444),
                topLeft = Offset(bx - 21f, by - 21f),
                size = Size(42f, 42f),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // Player Token
            val px = playerX * size.width
            val py = playerY * size.height
            drawCircle(primaryTheme.copy(alpha = 0.35f), radius = 36f, center = Offset(px, py))
            drawCircle(primaryTheme, radius = 24f, center = Offset(px, py))
            drawCircle(Color.White, radius = 24f, center = Offset(px, py), style = Stroke(width = 3f))
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Text(
                    text = "${game.emoji} ${game.title} • Anda: $score vs ${input.remotePlayerName ?: "BOT"}: $botScore",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFDE68A),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
