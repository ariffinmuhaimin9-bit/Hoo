package com.example.ui.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GameModeType
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ReflexGame(
    gameMode: GameModeType,
    botDiff: String,
    onScoreUpdated: (Int) -> Unit,
    onStatusChanged: (String) -> Unit
) {
    var isGreen by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(0L) }
    var bestScoreMs by remember { mutableStateOf(9999L) }
    var totalPoints by remember { mutableStateOf(0) }
    var p1Wins by remember { mutableStateOf(0) }
    var p2Wins by remember { mutableStateOf(0) }
    var roundMessage by remember { mutableStateOf("Tunggu sehingga skrin bertukar HIJAU...") }
    var roundCycle by remember { mutableStateOf(0) }

    val botTargetMs = when {
        botDiff.contains("Santai") -> 520L
        botDiff.contains("Pakar") -> 240L
        else -> 360L
    }

    LaunchedEffect(roundCycle, gameMode, botDiff) {
        isGreen = false
        roundMessage = "🔴 Tunggu... bersedia..."
        delay(Random.nextLong(1400L, 3300L))
        isGreen = true
        startTime = System.currentTimeMillis()
        roundMessage = "🟢 TEKAN SEKARANG! ⚡"
        onStatusChanged("HIJAU! Tekan pantas sekarang!")

        if (gameMode != GameModeType.LOCAL) {
            delay(botTargetMs)
            if (isGreen) {
                isGreen = false
                p2Wins++
                SoundHelper.playBoing()
                roundMessage = "🤖 BOT ($botTargetMs ms) lebih pantas kali ini!"
                onStatusChanged(roundMessage)
                delay(1200L)
                roundCycle++
            }
        } else {
            delay(2500L)
            if (isGreen) {
                isGreen = false
                roundMessage = "⏱️ Masa tamat! Cuba lagi."
                delay(900L)
                roundCycle++
            }
        }
    }

    fun handleTap(playerIdx: Int) {
        if (isGreen) {
            val reactionTime = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
            isGreen = false
            SoundHelper.playCoin()
            if (reactionTime < bestScoreMs) {
                bestScoreMs = reactionTime
            }
            if (playerIdx == 1) {
                p1Wins++
                val pts = ((700L - reactionTime.coerceAtMost(650L)) / 10L).toInt().coerceAtLeast(10)
                totalPoints += pts
                onScoreUpdated(totalPoints)
                roundMessage = if (gameMode == GameModeType.LOCAL) {
                    "🎉 Pemain 1 Menang! (${reactionTime}ms)"
                } else {
                    "⚡ Hebat! ${reactionTime}ms (BOT: ${botTargetMs}ms)"
                }
            } else {
                p2Wins++
                roundMessage = "🎉 Pemain 2 Menang! (${reactionTime}ms)"
            }
            onStatusChanged(roundMessage)
            roundCycle++
        } else {
            SoundHelper.playBoing()
            roundMessage = if (gameMode == GameModeType.LOCAL) {
                "⚠️ Pemain $playerIdx tekan terlalu awal!"
            } else {
                "⚠️ Terlalu awal! Tunggu warna HIJAU."
            }
            onStatusChanged(roundMessage)
        }
    }

    if (gameMode == GameModeType.LOCAL) {
        // Dual-Zone Split Screen for 2 Players on 1 Device
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isGreen) Color(0xFF22C55E) else Color(0xFFDC2626),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .testTag("reflex_p2_zone")
                    .clickable { handleTap(2) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PEMAIN 2 (Menang: $p2Wins)", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                        Text(roundMessage, color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isGreen) Color(0xFF16A34A) else Color(0xFFB91C1C),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .testTag("reflex_p1_zone")
                    .clickable { handleTap(1) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PEMAIN 1 (Menang: $p1Wins)", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                        Text(roundMessage, color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isGreen) Color(0xFF22C55E) else Color(0xFFEF4444))
                .testTag("reflex_single_zone")
                .clickable { handleTap(1) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (isGreen) "🟢 TEKAN SEKARANG!" else "🔴 TUNGGU HIJAU...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = roundMessage,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Rekod Pantas: ${if (bestScoreMs == 9999L) "-" else "$bestScoreMs ms"} • Anda $p1Wins - $p2Wins BOT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
