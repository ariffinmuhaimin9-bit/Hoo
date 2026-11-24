package com.example.ui.games

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GameModeType
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay

data class UnoCard(val color: Color, val colorName: String, val value: String)

private val UNO_COLORS = listOf(
    Color(0xFFEF4444) to "Merah",
    Color(0xFF38BDF8) to "Biru",
    Color(0xFF22C55E) to "Hijau",
    Color(0xFFF59E0B) to "Kuning"
)
private val UNO_VALUES = listOf("1", "2", "3", "5", "7", "9", "SKIP", "+2")

private fun randomUnoCard(): UnoCard {
    val (c, name) = UNO_COLORS.random()
    return UnoCard(c, name, UNO_VALUES.random())
}

@Composable
fun UnoCardsGame(
    gameMode: GameModeType,
    botDiff: String,
    onScoreUpdated: (Int) -> Unit,
    onStatusChanged: (String) -> Unit
) {
    var topCard by remember { mutableStateOf(UnoCard(Color(0xFFEF4444), "Merah", "7")) }
    val playerHand = remember { mutableStateListOf<UnoCard>().apply { repeat(5) { add(randomUnoCard()) } } }
    val botHand = remember { mutableStateListOf<UnoCard>().apply { repeat(5) { add(randomUnoCard()) } } }
    var isPlayerTurn by remember { mutableStateOf(true) }
    var score by remember { mutableStateOf(0) }
    var unoShouted by remember { mutableStateOf(false) }

    fun canPlay(card: UnoCard, top: UnoCard): Boolean =
        card.colorName == top.colorName || card.value == top.value || card.value == "+2"

    LaunchedEffect(gameMode) {
        onStatusChanged("Padankan warna atau nombor! Jika tiada kad sesuai, tekan Ambil Kad.")
    }

    // BOT / P2 automated turn when VS BOT
    LaunchedEffect(isPlayerTurn, gameMode) {
        if (!isPlayerTurn && gameMode != GameModeType.LOCAL) {
            delay(if (botDiff.contains("Pakar")) 450L else 750L)
            val matchIdx = botHand.indexOfFirst { canPlay(it, topCard) }
            if (matchIdx >= 0) {
                val played = botHand.removeAt(matchIdx)
                topCard = played
                SoundHelper.playPop()
                if (played.value == "+2") {
                    repeat(2) { playerHand.add(randomUnoCard()) }
                    onStatusChanged("🤖 BOT main ${played.colorName} +2! Anda ambil 2 kad.")
                } else if (played.value == "SKIP") {
                    onStatusChanged("🤖 BOT main SKIP! BOT jalan lagi.")
                    delay(600L)
                    // stays bot turn via re-trigger if needed or pass back cleanly
                }
                if (botHand.isEmpty()) {
                    SoundHelper.playBoing()
                    onStatusChanged("🤖 BOT habis kad dulu! Pusingan baharu dimulakan.")
                    repeat(5) { botHand.add(randomUnoCard()) }
                    playerHand.clear()
                    repeat(5) { playerHand.add(randomUnoCard()) }
                }
            } else {
                botHand.add(randomUnoCard())
                onStatusChanged("🤖 BOT ambil 1 kad. Giliran anda!")
            }
            isPlayerTurn = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🃏 Kad Warna Uno",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFF1E293B)) {
                Text(
                    text = if (gameMode == GameModeType.LOCAL) "👥 Giliran: ${if (isPlayerTurn) "P1" else "P2"}"
                    else "🤖 Kad BOT: ${botHand.size}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE68A),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Center Table: Top Card + Draw + UNO button
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = topCard.color,
                modifier = Modifier
                    .size(84.dp, 118.dp)
                    .border(3.dp, Color.White, RoundedCornerShape(16.dp)),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(topCard.value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(topCard.colorName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        SoundHelper.playPop()
                        if (isPlayerTurn || gameMode == GameModeType.LOCAL) {
                            val activeHand = if (gameMode == GameModeType.LOCAL && !isPlayerTurn) botHand else playerHand
                            activeHand.add(randomUnoCard())
                            isPlayerTurn = !isPlayerTurn
                            onStatusChanged("Ambil 1 kad baharu!")
                        }
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("➕ Ambil Kad", maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = {
                        unoShouted = true
                        SoundHelper.playFanfare()
                        score += 10
                        onScoreUpdated(score)
                        onStatusChanged("📢 UNO!! Bonus +10 mata!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("📢 Jerit UNO!", color = Color.Black, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
                }
            }
        }

        val currentHand = if (gameMode == GameModeType.LOCAL && !isPlayerTurn) botHand else playerHand
        Text(
            text = if (gameMode == GameModeType.LOCAL) {
                "Kad Pemain ${if (isPlayerTurn) "1" else "2"} (${currentHand.size}):"
            } else {
                "Kad Anda (${playerHand.size}):"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            currentHand.forEachIndexed { index, card ->
                val playable = canPlay(card, topCard)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = card.color,
                    modifier = Modifier
                        .size(68.dp, 98.dp)
                        .border(
                            width = if (playable) 3.dp else 1.5.dp,
                            color = if (playable) Color.White else Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = gameMode == GameModeType.LOCAL || isPlayerTurn) {
                            if (playable) {
                                SoundHelper.playCoin()
                                topCard = card
                                currentHand.removeAt(index)
                                score += 15
                                onScoreUpdated(score)
                                if (card.value == "+2") {
                                    val targetHand = if (isPlayerTurn) botHand else playerHand
                                    repeat(2) { targetHand.add(randomUnoCard()) }
                                }
                                if (currentHand.isEmpty()) {
                                    SoundHelper.playFanfare()
                                    score += 50
                                    onScoreUpdated(score)
                                    onStatusChanged("🏆 UNO MENANG! (+50 Bonus) Kad diisi semula.")
                                    repeat(5) { currentHand.add(randomUnoCard()) }
                                } else {
                                    onStatusChanged("Kad ${card.colorName} ${card.value} diletak!")
                                    if (card.value != "SKIP") {
                                        isPlayerTurn = !isPlayerTurn
                                    } else {
                                        onStatusChanged("⚡ SKIP! Teruskan giliran anda!")
                                    }
                                }
                            } else {
                                SoundHelper.playBoing()
                                onStatusChanged("Kad tak sepadan — pilih warna ${topCard.colorName} atau nilai ${topCard.value}.")
                            }
                        },
                    shadowElevation = if (playable) 6.dp else 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(card.value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(card.colorName, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                }
            }
        }
    }
}
