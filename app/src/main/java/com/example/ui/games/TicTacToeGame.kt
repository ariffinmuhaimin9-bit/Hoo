package com.example.ui.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GameModeType
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay

@Composable
fun TicTacToeGame(
    gameMode: GameModeType,
    botDiff: String,
    onScoreUpdated: (Int) -> Unit,
    onStatusChanged: (String) -> Unit
) {
    var board by remember { mutableStateOf(List(9) { "" }) }
    var currentTurn by remember { mutableStateOf("X") } // "X" = P1, "O" = BOT or P2
    var playerWins by remember { mutableStateOf(0) }
    var opponentWins by remember { mutableStateOf(0) }
    var roundBanner by remember { mutableStateOf("Giliran Pemain X") }
    var roundLocked by remember { mutableStateOf(false) }

    val winCombos = remember {
        listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
    }

    fun checkWin(b: List<String>, player: String): List<Int>? =
        winCombos.firstOrNull { c -> b[c[0]] == player && b[c[1]] == player && b[c[2]] == player }

    fun findSmartMove(b: List<String>): Int {
        val empty = b.indices.filter { b[it].isBlank() }
        if (empty.isEmpty()) return -1
        if (botDiff.contains("Santai")) return empty.random()

        // 1. Try to win as O
        for (idx in empty) {
            val test = b.toMutableList().apply { this[idx] = "O" }
            if (checkWin(test, "O") != null) return idx
        }
        // 2. Block X from winning
        for (idx in empty) {
            val test = b.toMutableList().apply { this[idx] = "X" }
            if (checkWin(test, "X") != null) return idx
        }
        if (botDiff.contains("Pakar")) {
            if (b[4].isBlank()) return 4
            val corners = listOf(0, 2, 6, 8).filter { b[it].isBlank() }
            if (corners.isNotEmpty()) return corners.random()
        }
        return empty.random()
    }

    LaunchedEffect(gameMode, botDiff) {
        board = List(9) { "" }
        currentTurn = "X"
        roundLocked = false
        val startMsg = if (gameMode == GameModeType.LOCAL) {
            "Local 2P Pass & Play: Giliran Pemain X!"
        } else {
            "VS BOT ($botDiff): Giliran Anda (X)!"
        }
        roundBanner = startMsg
        onStatusChanged(startMsg)
    }

    // BOT Turn
    LaunchedEffect(currentTurn, roundLocked, gameMode) {
        if (gameMode != GameModeType.LOCAL && currentTurn == "O" && !roundLocked && board.any { it.isBlank() }) {
            delay(450L)
            val botMove = findSmartMove(board)
            if (botMove >= 0) {
                val newBoard = board.toMutableList()
                newBoard[botMove] = "O"
                board = newBoard
                SoundHelper.playPop()

                when {
                    checkWin(newBoard, "O") != null -> {
                        opponentWins++
                        roundLocked = true
                        SoundHelper.playBoing()
                        val msg = "🤖 BOT (O) menang! \"Hehe cuba lagi!\""
                        roundBanner = msg
                        onStatusChanged(msg)
                        delay(1100L)
                        board = List(9) { "" }
                        currentTurn = "X"
                        roundLocked = false
                        roundBanner = "Pusingan Baru • Giliran Anda (X)"
                    }
                    newBoard.all { it.isNotBlank() } -> {
                        roundLocked = true
                        val msg = "🤝 Seri! Papan dikosongkan."
                        roundBanner = msg
                        onStatusChanged(msg)
                        delay(1000L)
                        board = List(9) { "" }
                        currentTurn = "X"
                        roundLocked = false
                        roundBanner = "Pusingan Baru • Giliran Anda (X)"
                    }
                    else -> {
                        currentTurn = "X"
                        roundBanner = "Giliran Anda (X)"
                        onStatusChanged("Giliran Anda (X)")
                    }
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        val cellSize = ((minOf(maxWidth, maxHeight) - 110.dp) / 3f).coerceIn(58.dp, 96.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Text(
                        text = "❌ Pemain 1: $playerWins",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Text(
                        text = if (gameMode == GameModeType.LOCAL) "⭕ Pemain 2: $opponentWins" else "🤖 BOT: $opponentWins",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Text(
                text = roundBanner,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFDE68A)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (col in 0..2) {
                            val idx = row * 3 + col
                            val cell = board[idx]
                            val canClick = cell.isBlank() && !roundLocked && (gameMode == GameModeType.LOCAL || currentTurn == "X")
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(
                                        width = 2.dp,
                                        color = when (cell) {
                                            "X" -> Color(0xFF38BDF8)
                                            "O" -> Color(0xFFEF4444)
                                            else -> Color.White.copy(alpha = 0.18f)
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .testTag("ttt_cell_$idx")
                                    .clickable(enabled = canClick) {
                                        SoundHelper.playPop()
                                        val placed = currentTurn
                                        val newBoard = board.toMutableList()
                                        newBoard[idx] = placed
                                        board = newBoard

                                        if (checkWin(newBoard, placed) != null) {
                                            SoundHelper.playFanfare()
                                            if (placed == "X") {
                                                playerWins++
                                                onScoreUpdated(playerWins * 15)
                                                roundBanner = if (gameMode == GameModeType.LOCAL) "🎉 Pemain X Menang!" else "🎉 Anda Menang! BOT terkejut!"
                                            } else {
                                                opponentWins++
                                                roundBanner = "🎉 Pemain O Menang!"
                                            }
                                            onStatusChanged(roundBanner)
                                            board = List(9) { "" }
                                            currentTurn = "X"
                                        } else if (newBoard.all { it.isNotBlank() }) {
                                            roundBanner = "🤝 Seri! Cuba pusingan baharu."
                                            onStatusChanged(roundBanner)
                                            board = List(9) { "" }
                                            currentTurn = "X"
                                        } else {
                                            currentTurn = if (placed == "X") "O" else "X"
                                            roundBanner = if (gameMode == GameModeType.LOCAL) {
                                                "Pass & Play • Giliran Pemain $currentTurn"
                                            } else {
                                                "Giliran BOT (O)..."
                                            }
                                            onStatusChanged(roundBanner)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cell,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (cell == "X") Color(0xFF38BDF8) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
