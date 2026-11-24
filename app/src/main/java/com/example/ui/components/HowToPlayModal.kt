package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameItem
import com.example.utils.SoundHelper

@Composable
fun HowToPlayModal(
    game: GameItem,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${game.emoji} Cara Bermain: ${game.title}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            SoundHelper.playPop()
                            onDismiss()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Text("🎯 Matlamat Utama:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = game.goalHint,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("🕹️ Kawalan Asas (Sentuhan & Papan Kekunci):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = game.controlHint,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("🏆 Peraturan Mod 1P (vs BOT) vs 2P (Local / Bilik):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = game.modeHint,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        SoundHelper.playPop()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Faham, Jom Main! 🚀", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
