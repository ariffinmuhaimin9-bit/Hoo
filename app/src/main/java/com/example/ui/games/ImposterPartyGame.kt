package com.example.ui.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GameModeType
import com.example.utils.SoundHelper

data class SuspectCrew(
    val name: String,
    val emoji: String,
    val alibi: String,
    val isImposter: Boolean
)

@Composable
fun ImposterPartyGame(
    gameMode: GameModeType,
    onScoreUpdated: (Int) -> Unit,
    onStatusChanged: (String) -> Unit
) {
    val scenarios = remember {
        listOf(
            "Kes Biskut Hilang" to listOf(
                SuspectCrew("Kuki", "🦊", "Saya tengah mengira bintang di taman.", false),
                SuspectCrew("Milo", "🐼", "Saya nampak kesan coklat dekat pintu dapur!", false),
                SuspectCrew("Bob", "🐸", "Biskut? Er... saya cuma lap mulut dengan tisu...", true),
                SuspectCrew("Nana", "🐰", "Saya sibuk menanam lobak dari tadi.", false)
            ),
            "Kes Jus Oren Berkilau" to listOf(
                SuspectCrew("Zuzu", "🐱", "Saya terdengar bunyi straw disedut dengan laju.", false),
                SuspectCrew("Toby", "🐶", "Lidah saya berwarna oren sebab makan gula-gula oren... agaknya?", true),
                SuspectCrew("Lili", "🦄", "Saya tengah melukis pelangi di bilik muzik.", false),
                SuspectCrew("Bibi", "🐧", "Saya menjaga peti ais supaya kekal sejuk!", false)
            ),
            "Kes Topi Ajaib Tertukar" to listOf(
                SuspectCrew("Oyen", "🐯", "Topi saya besar sangat, asyik jatuh ke mata!", true),
                SuspectCrew("Kiko", "🐨", "Saya baru bangun tidur dari dahan pokok.", false),
                SuspectCrew("Mimi", "🐥", "Saya mengira bulu pelepah saya.", false),
                SuspectCrew("Rara", "🐙", "Saya pegang lapan cawan teh sekali gus.", false)
            )
        )
    }

    var roundIdx by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    val currentCase = scenarios[roundIdx % scenarios.size]
    var hintText by remember { mutableStateOf("Baca petunjuk setiap rakan dan undi siapa yang menyamar!") }

    LaunchedEffect(roundIdx, gameMode) {
        onStatusChanged("${currentCase.first} (${if (gameMode == GameModeType.LOCAL) "Undi Bersama" else "Mod Detektif"})")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("🕵️ ${currentCase.first}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text(
            text = hintText,
            fontSize = 13.sp,
            color = Color(0xFFFDE68A),
            textAlign = TextAlign.Center
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(currentCase.second) { suspect ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            SoundHelper.playPop()
                            if (suspect.isImposter) {
                                SoundHelper.playFanfare()
                                score += 25
                                onScoreUpdated(score)
                                hintText = "🎉 Betul! ${suspect.name} ialah Si Peniru! (+25 mata) — Kes baharu dimuatkan!"
                                onStatusChanged("Syabas Detektif! Skor: $score")
                                roundIdx++
                            } else {
                                SoundHelper.playBoing()
                                hintText = "😅 Ops! ${suspect.name} jujur. Cuba perhatikan alasan yang pelik!"
                                onStatusChanged(hintText)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(suspect.emoji, fontSize = 32.sp)
                        Text(suspect.name, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                        Text(
                            text = "\"${suspect.alibi}\"",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF38BDF8).copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "Undi Sekarang",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
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
