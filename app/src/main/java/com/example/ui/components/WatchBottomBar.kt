package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.SoundHelper

enum class WatchTab(val id: String, val icon: String, val label: String) {
    HOME("home", "🏠", "Utama"),
    PLAY("play", "🎮", "Main"),
    WIFI("wifi", "📶", "WiFi Room"),
    STORE("store", "🛒", "Kedai"),
    BACKPACK("backpack", "🎒", "Beg"),
    PEOPLE("people", "👥", "Rakan"),
    CHALLENGES("challenges", "🏆", "Cabaran"),
    FEATURES("features", "✨", "100+ Fitur")
}

@Composable
fun WatchBottomBar(
    selectedTab: WatchTab,
    unreadAlerts: Int,
    wifiConnected: Boolean,
    onSelectTab: (WatchTab) -> Unit
) {
    // Rec Room Watch Menu inspired Coral-Orange rounded bar
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFEA580C), Color(0xFFF97316), Color(0xFFEA580C))
                    )
                )
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WatchTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.28f)
                                else Color.Transparent
                            )
                            .clickable {
                                SoundHelper.playPop()
                                onSelectTab(tab)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("watch_tab_${tab.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box {
                                Text(tab.icon, fontSize = 18.sp)
                                if (tab == WatchTab.CHALLENGES && unreadAlerts > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-2).dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$unreadAlerts", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                                if (tab == WatchTab.WIFI && wifiConnected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 5.dp, y = (-2).dp)
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E))
                                    )
                                }
                            }
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = Color.White,
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
