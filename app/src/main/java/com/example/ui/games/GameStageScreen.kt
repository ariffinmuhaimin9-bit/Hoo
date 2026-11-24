package com.example.ui.games

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HubExtrasState
import com.example.model.GameItem
import com.example.model.HubFeaturesCatalog
import com.example.network.WifiRoomManager
import com.example.network.WifiRoomState
import com.example.ui.components.CircularJoystick
import com.example.ui.components.GameModeType
import com.example.utils.SoundHelper
import kotlinx.coroutines.delay

@Composable
fun GameStageScreen(
    game: GameItem,
    gameMode: GameModeType,
    botDiff: String,
    roomCode: String,
    isLandscape: Boolean,
    extras: HubExtrasState,
    wifiState: WifiRoomState,
    wifiManager: WifiRoomManager,
    onToggleOrientation: () -> Unit,
    onBackToHub: () -> Unit,
    onOpenRoomService: () -> Unit,
    onOpenHowToPlay: () -> Unit,
    onRecordScore: (Int) -> Unit
) {
    var score by remember(game.id) { mutableStateOf(0) }
    var submittedScore by remember(game.id) { mutableStateOf(0) }
    var statusText by remember(game.id, gameMode, botDiff, roomCode) {
        mutableStateOf(
            when {
                wifiState.isConnectedToRoom -> "WiFi LAN (${wifiState.pingMs}ms) • Bilik ${wifiState.roomCode}"
                gameMode == GameModeType.VS_BOT -> "Mod: VS BOT ($botDiff)"
                gameMode == GameModeType.LOCAL -> "Mod: Local 2 Pemain"
                else -> "Bilik Rakan: $roomCode"
            }
        )
    }
    var restartTrigger by remember(game.id) { mutableStateOf(0) }
    val input = remember(game.id, restartTrigger) { GameInputState() }
    val focusRequester = remember { FocusRequester() }

    val joystickAccent = remember(extras.equippedJoystickId) {
        HubFeaturesCatalog.STORE_ITEMS.firstOrNull { it.id == extras.equippedJoystickId }?.accentColor
            ?: Color(0xFF38BDF8)
    }
    val joystickSizeDp = (122f * extras.joystickScale).dp

    // Sync incoming WiFi opponent coordinates & score into GameInputState
    LaunchedEffect(wifiState.remoteMoveX, wifiState.remoteMoveY, wifiState.remoteScore, wifiState.remoteName) {
        input.remotePlayerName = wifiState.remoteName
        input.remoteMoveX = wifiState.remoteMoveX
        input.remoteMoveY = wifiState.remoteMoveY
        input.remoteScore = wifiState.remoteScore
    }

    // Broadcast local analog joystick + score to WiFi Room peers at ~12Hz when connected
    LaunchedEffect(wifiState.isConnectedToRoom, game.id) {
        while (wifiState.isConnectedToRoom) {
            wifiManager.sendGameplayTelemetry(input.moveX, input.moveY, score)
            delay(85L)
        }
    }

    fun flushScore() {
        val delta = score - submittedScore
        if (delta > 0) {
            onRecordScore(delta)
            submittedScore = score
        }
    }

    BackHandler {
        flushScore()
        SoundHelper.playPop()
        onBackToHub()
    }

    DisposableEffect(game.id) {
        onDispose { input.reset() }
    }

    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(focusRequester)
            .focusTarget()
            .onPreviewKeyEvent { keyEvent ->
                val isDown = keyEvent.type == KeyEventType.KeyDown
                val isUp = keyEvent.type == KeyEventType.KeyUp
                when (keyEvent.key.nativeKeyCode) {
                    AndroidKeyEvent.KEYCODE_A, AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (isDown) input.moveX = -1f else if (isUp && input.moveX < 0f) input.moveX = 0f
                        true
                    }
                    AndroidKeyEvent.KEYCODE_D, AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (isDown) input.moveX = 1f else if (isUp && input.moveX > 0f) input.moveX = 0f
                        true
                    }
                    AndroidKeyEvent.KEYCODE_W, AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                        if (isDown) input.moveY = -1f else if (isUp && input.moveY < 0f) input.moveY = 0f
                        true
                    }
                    AndroidKeyEvent.KEYCODE_S, AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (isDown) input.moveY = 1f else if (isUp && input.moveY > 0f) input.moveY = 0f
                        true
                    }
                    AndroidKeyEvent.KEYCODE_SPACE, AndroidKeyEvent.KEYCODE_ENTER -> {
                        if (isDown) input.triggerAction()
                        true
                    }
                    else -> false
                }
            }
            .padding(if (isLandscape) 8.dp else 10.dp)
    ) {
        val compactLandscape = isLandscape || maxWidth > maxHeight * 1.15f

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 6.dp else 8.dp)
        ) {
            // Stage Header
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = if (compactLandscape) 6.dp else 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { flushScore(); SoundHelper.playPop(); onBackToHub() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(40.dp).testTag("stage_back_button")
                        ) {
                            Text("⬅️ Katalog", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = { SoundHelper.playPop(); onOpenRoomService() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(40.dp).testTag("stage_room_button")
                        ) {
                            Text(
                                text = if (wifiState.isConnectedToRoom) "📶 WiFi (${wifiState.pingMs}ms)" else "🏠 Mod / Bilik",
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Button(
                            onClick = { SoundHelper.playPop(); onOpenHowToPlay() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(40.dp).testTag("stage_howto_button")
                        ) {
                            Text("❓ Panduan", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = { SoundHelper.playPop(); onToggleOrientation() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(40.dp).testTag("stage_orientation_button")
                        ) {
                            Text(
                                text = if (isLandscape) "📱 Potret" else "🔄 Landskap",
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Skor: $score",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 180.dp)
                        )
                    }
                }
            }

            if (compactLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val leftPane: @Composable () -> Unit = {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                CircularJoystick(
                                    input = input,
                                    accentColor = joystickAccent,
                                    joystickSize = joystickSizeDp,
                                    onDirectionHint = { statusText = it }
                                )
                            }
                        }
                    }
                    val rightPane: @Composable () -> Unit = {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ActionPulseButton { input.triggerAction(); SoundHelper.playPop() }
                                Button(
                                    onClick = {
                                        flushScore()
                                        score = 0
                                        submittedScore = 0
                                        SoundHelper.playPop()
                                        restartTrigger++
                                        statusText = "Permainan dimulakan semula!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(48.dp).testTag("stage_restart_button")
                                ) {
                                    Text("🔄 Semula", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }

                    if (!extras.joystickOnRight) leftPane() else rightPane()

                    GameCanvasFrame(
                        game = game,
                        input = input,
                        gameMode = gameMode,
                        botDiff = botDiff,
                        restartTrigger = restartTrigger,
                        onScoreUpdated = { score = it },
                        onStatusChanged = { statusText = it },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )

                    if (!extras.joystickOnRight) rightPane() else leftPane()
                }
            } else {
                // Portrait: Top Canvas + Bottom 360 Circular Joystick & Action Bar
                GameCanvasFrame(
                    game = game,
                    input = input,
                    gameMode = gameMode,
                    botDiff = botDiff,
                    restartTrigger = restartTrigger,
                    onScoreUpdated = { score = it },
                    onStatusChanged = { statusText = it },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val joystickBlock: @Composable () -> Unit = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularJoystick(
                                    input = input,
                                    accentColor = joystickAccent,
                                    joystickSize = joystickSizeDp,
                                    onDirectionHint = { statusText = it }
                                )
                                Text("Joystick 360°", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        val actionBlock: @Composable () -> Unit = {
                            Column(
                                horizontalAlignment = if (extras.joystickOnRight) Alignment.Start else Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ActionPulseButton { input.triggerAction(); SoundHelper.playPop() }
                                OutlinedButton(
                                    onClick = {
                                        flushScore()
                                        score = 0
                                        submittedScore = 0
                                        SoundHelper.playPop()
                                        restartTrigger++
                                        statusText = "Permainan dimulakan semula!"
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(44.dp).testTag("stage_restart_button")
                                ) {
                                    Text("🔄 Mula Semula", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        if (!extras.joystickOnRight) {
                            joystickBlock()
                            actionBlock()
                        } else {
                            actionBlock()
                            joystickBlock()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCanvasFrame(
    game: GameItem,
    input: GameInputState,
    gameMode: GameModeType,
    botDiff: String,
    restartTrigger: Int,
    onScoreUpdated: (Int) -> Unit,
    onStatusChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF090D16))
            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        key(game.id, restartTrigger) {
            when (game.id) {
                "sumo_3d" -> SumoBumperGame(input = input, gameMode = gameMode, botDiff = botDiff, onScoreUpdated = onScoreUpdated, onStatusChanged = onStatusChanged)
                "sky_parkour" -> SkyParkourGame(input = input, onScoreUpdated = onScoreUpdated, onStatusChanged = onStatusChanged)
                "bowling_3d" -> BowlingGame(input = input, onScoreUpdated = onScoreUpdated, onStatusChanged = onStatusChanged)
                "pong_neon" -> PongGame(input = input, gameMode = gameMode, botDiff = botDiff, onScoreUpdated = onScoreUpdated, onStatusChanged = onStatusChanged)
                "tictactoe" -> TicTacToeGame(gameMode = gameMode, botDiff = botDiff, onScoreUpdated = onScoreUpdated, onStatusChanged = onStatusChanged)
                "reflex_test" -> ReflexGame(gameMode = gameMode, botDiff = botDiff, onScoreUpdated = onScoreUpdated, onStatusChanged = onStatusChanged)
                "imposter_party" -> ImposterPartyGame(gameMode = gameMode, onScoreUpdated = onScoreUpdated, onStatusChanged = onStatusChanged)
                "uno_cards" -> UnoCardsGame(gameMode = gameMode, botDiff = botDiff, onScoreUpdated = onScoreUpdated, onStatusChanged = onStatusChanged)
                "hide_seek_3d" -> HideSeekGame(input = input, onScoreUpdated = onScoreUpdated, onStatusChanged = onStatusChanged)
                else -> GenericInteractiveEngine(game = game, input = input, gameMode = gameMode, botDiff = botDiff, onScoreUpdated = onScoreUpdated, onStatusChanged = onStatusChanged)
            }
        }
    }
}

@Composable
private fun ActionPulseButton(
    onTrigger: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiary,
        shadowElevation = 6.dp,
        modifier = Modifier
            .size(width = 114.dp, height = 56.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onTrigger()
                        tryAwaitRelease()
                    }
                )
            }
            .testTag("stage_action_button")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "⚡ AKSI",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
