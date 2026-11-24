package com.example

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutGoogle
import com.example.data.AppExtrasRepository
import com.example.data.CloudProfileRepository
import com.example.data.HubExtrasState
import com.example.data.LocalGuestRepository
import com.example.data.RoomRepository
import com.example.model.GameItem
import com.example.model.GameRepository
import com.example.model.UserProfile
import com.example.network.WifiRoomManager
import com.example.ui.MainHubScreen
import com.example.ui.components.*
import com.example.ui.games.GameStageScreen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.SoundHelper
import com.example.viewmodel.HubViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var wifiRoomManager: WifiRoomManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContext = applicationContext
        val localGuestRepo = LocalGuestRepository(appContext)
        val extrasRepo = AppExtrasRepository(appContext)
        wifiRoomManager = WifiRoomManager(appContext)

        val firestoreDb = FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
        val cloudProfileRepo = CloudProfileRepository(firestoreDb, Firebase.auth)
        val roomRepo = RoomRepository(firestoreDb, Firebase.auth)

        setContent {
            val scope = rememberCoroutineScope()
            val config = LocalConfiguration.current
            val systemLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

            val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
                initialValue = Firebase.auth.currentUser
            )
            val isGuestActive by localGuestRepo.isGuestActive.collectAsStateWithLifecycle(initialValue = false)
            val isDarkTheme by localGuestRepo.darkModeFlow.collectAsStateWithLifecycle(initialValue = true)
            val isMuted by localGuestRepo.mutedFlow.collectAsStateWithLifecycle(initialValue = false)
            val extras by extrasRepo.stateFlow.collectAsStateWithLifecycle(initialValue = HubExtrasState())
            val wifiState by wifiRoomManager.state.collectAsStateWithLifecycle()

            LaunchedEffect(isMuted) {
                SoundHelper.isMuted = isMuted
            }

            var forceLandscape by remember { mutableStateOf(false) }
            val effectiveLandscape = forceLandscape || systemLandscape
            var isLoading by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                if (isLoading) {
                    LoadingScreen(onLoaded = { isLoading = false })
                } else if (currentUser == null && !isGuestActive) {
                    AuthScreen(
                        onGoogleSuccess = { scope.launch { localGuestRepo.setGuestActive(false) } },
                        onContinueAsGuest = { guestName, guestAvatar ->
                            scope.launch {
                                localGuestRepo.updateGuestProfile(
                                    displayName = guestName,
                                    avatarEmoji = guestAvatar,
                                    avatarColorHex = "#38BDF8",
                                    bio = "Bermain dalam Mod Tetamu di peranti ini!",
                                    favoriteBadge = "Pemain Tetamu"
                                )
                                localGuestRepo.setGuestActive(true, guestName)
                            }
                        }
                    )
                } else {
                    val authKey = currentUser?.uid ?: "guest_local"
                    key(authKey) {
                        val hubViewModel: HubViewModel = viewModel(
                            key = "hub_$authKey",
                            factory = viewModelFactory {
                                addInitializer(HubViewModel::class) {
                                    HubViewModel(
                                        cloudProfileRepository = cloudProfileRepo,
                                        localGuestRepository = localGuestRepo,
                                        roomRepository = roomRepo,
                                        currentUserId = currentUser?.uid,
                                        defaultDisplayName = currentUser?.displayName ?: "Pemain Google"
                                    )
                                }
                            }
                        )

                        val profileUiState by hubViewModel.profileState.collectAsStateWithLifecycle()
                        val cloudRoom by hubViewModel.cloudRoomState.collectAsStateWithLifecycle()
                        val statusBanner by hubViewModel.statusBanner.collectAsStateWithLifecycle()
                        var localToast by remember { mutableStateOf<String?>(null) }

                        val activeProfile = when (val state = profileUiState) {
                            is UiState.Success -> state.data
                            else -> UserProfile(
                                userId = authKey,
                                displayName = currentUser?.displayName ?: "Pemain MiniVerse"
                            )
                        }

                        var currentScreen by remember { mutableStateOf("hub") } // "hub", "stage"
                        var activeWatchTab by remember { mutableStateOf(WatchTab.HOME) }
                        var selectedGame by remember { mutableStateOf<GameItem?>(null) }
                        val allGames = remember { GameRepository.getAllGames() }
                        val currentLobbyGame = selectedGame ?: allGames.first()

                        var showRoomDialog by remember { mutableStateOf(false) }
                        var showHowToPlayModal by remember { mutableStateOf(false) }
                        var showProfileDialog by remember { mutableStateOf(false) }

                        var gameMode by remember { mutableStateOf(GameModeType.VS_BOT) }
                        var botDiff by remember { mutableStateOf("Mencabar (Sederhana)") }
                        var roomCode by remember { mutableStateOf("1234") }

                        val toggleOrientation: () -> Unit = {
                            forceLandscape = !forceLandscape
                            requestedOrientation = if (forceLandscape) {
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        }

                        val unreadQuests = com.example.model.HubFeaturesCatalog.DAILY_QUESTS.count {
                            !extras.claimedQuestIds.contains(it.id)
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            Scaffold(
                                contentWindowInsets = WindowInsets.safeDrawing,
                                topBar = {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shadowElevation = 4.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .statusBarsPadding()
                                                .padding(
                                                    horizontal = 10.dp,
                                                    vertical = if (effectiveLandscape) 4.dp else 6.dp
                                                ),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "🎮 MiniVerse",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = if (effectiveLandscape) 14.sp else 16.sp,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = MaterialTheme.colorScheme.tertiary
                                                ) {
                                                    Text(
                                                        text = "${allGames.size} GAME",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Black,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = { SoundHelper.playPop(); showProfileDialog = true },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(38.dp).testTag("top_profile_button")
                                                ) {
                                                    Text("${activeProfile.avatarEmoji} Profil", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                                }
                                                Button(
                                                    onClick = {
                                                        val nextMuted = !isMuted
                                                        SoundHelper.isMuted = nextMuted
                                                        scope.launch { localGuestRepo.setMuted(nextMuted) }
                                                        if (!nextMuted) SoundHelper.playPop()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(38.dp).testTag("top_mute_button")
                                                ) {
                                                    Text(if (isMuted) "🔇" else "🔊", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                                }
                                                Button(
                                                    onClick = {
                                                        SoundHelper.playPop()
                                                        scope.launch { localGuestRepo.setDarkMode(!isDarkTheme) }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(38.dp).testTag("top_theme_button")
                                                ) {
                                                    Text(if (isDarkTheme) "🌙" else "☀️", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                                }
                                                Button(
                                                    onClick = { SoundHelper.playPop(); toggleOrientation() },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(38.dp).testTag("top_orientation_button")
                                                ) {
                                                    Text(
                                                        text = if (effectiveLandscape) "📱 Potret" else "🔄 Landskap",
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                                Button(
                                                    onClick = {
                                                        SoundHelper.playPop()
                                                        if (currentScreen == "hub") activeWatchTab = WatchTab.WIFI
                                                        else showRoomDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (wifiState.isConnectedToRoom) Color(0xFF22C55E) else MaterialTheme.colorScheme.tertiary
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(38.dp).testTag("top_room_button")
                                                ) {
                                                    Text(
                                                        text = if (wifiState.isConnectedToRoom) "📶 WiFi" else "🏠 Room",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (wifiState.isConnectedToRoom) Color.White else Color.Black,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                bottomBar = {
                                    if (currentScreen == "hub") {
                                        WatchBottomBar(
                                            selectedTab = activeWatchTab,
                                            unreadAlerts = unreadQuests,
                                            wifiConnected = wifiState.isConnectedToRoom,
                                            onSelectTab = { activeWatchTab = it }
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .padding(innerPadding)
                                        .fillMaxSize()
                                ) {
                                    if (currentScreen == "hub") {
                                        when (activeWatchTab) {
                                            WatchTab.HOME, WatchTab.PLAY -> MainHubScreen(
                                                userProfile = activeProfile,
                                                extras = extras,
                                                isGoogleUser = hubViewModel.isGoogleAuthenticated,
                                                isLandscape = effectiveLandscape,
                                                showWatchHomeHeader = activeWatchTab == WatchTab.HOME,
                                                favorites = activeProfile.favoriteGameIds.toSet(),
                                                onToggleFavorite = { gameId ->
                                                    hubViewModel.toggleFavorite(gameId, activeProfile.favoriteGameIds)
                                                },
                                                onSelectGame = { game ->
                                                    selectedGame = game
                                                    currentScreen = "stage"
                                                },
                                                onOpenRoomService = { showRoomDialog = true },
                                                onOpenProfile = { showProfileDialog = true },
                                                onNavigateWatchTab = { activeWatchTab = it }
                                            )
                                            WatchTab.WIFI -> WifiRoomScreen(
                                                wifiState = wifiState,
                                                wifiManager = wifiRoomManager,
                                                userProfile = activeProfile,
                                                selectedGame = currentLobbyGame,
                                                onLaunchWifiGame = { game ->
                                                    selectedGame = game
                                                    gameMode = GameModeType.LOBBY
                                                    currentScreen = "stage"
                                                },
                                                onOpenGamePicker = { activeWatchTab = WatchTab.PLAY }
                                            )
                                            WatchTab.STORE -> StoreScreen(
                                                extras = extras,
                                                onBuyOrEquip = { id, price, cat ->
                                                    scope.launch {
                                                        localToast = extrasRepo.buyOrEquipItem(id, price, cat)
                                                    }
                                                }
                                            )
                                            WatchTab.BACKPACK -> BackpackScreen(
                                                extras = extras,
                                                onEquipItem = { id, cat ->
                                                    scope.launch {
                                                        localToast = extrasRepo.buyOrEquipItem(id, 0, cat)
                                                    }
                                                },
                                                onUpdateJoystickPrefs = { sc, right ->
                                                    scope.launch {
                                                        extrasRepo.setJoystickSettings(sc, right)
                                                        localToast = "Tetapan Joystick 360° dikemas kini!"
                                                    }
                                                }
                                            )
                                            WatchTab.PEOPLE -> BuddiesPeopleScreen(
                                                extras = extras,
                                                onAddBuddy = { name ->
                                                    scope.launch {
                                                        extrasRepo.addBuddy(name)
                                                        localToast = "$name ditambah ke senarai rakan!"
                                                    }
                                                },
                                                onInviteBuddy = { buddy ->
                                                    wifiRoomManager.sendGameInvitation(
                                                        hostName = activeProfile.displayName,
                                                        gameId = currentLobbyGame.id,
                                                        gameTitle = currentLobbyGame.title
                                                    )
                                                    localToast = "Jemputan dihantar ke ${buddy.name}!"
                                                },
                                                onCheerProfile = {
                                                    scope.launch {
                                                        val total = extrasRepo.addCheer()
                                                        localToast = "👍 Cheer berjaya! Jumlah sokongan: $total"
                                                    }
                                                }
                                            )
                                            WatchTab.CHALLENGES -> ChallengesScreen(
                                                profile = activeProfile,
                                                extras = extras,
                                                onClaimDailyStreak = {
                                                    scope.launch {
                                                        if (extrasRepo.claimDailyStreak()) {
                                                            localToast = "🎁 +100 Syiling & +5 Permata dituntut!"
                                                        }
                                                    }
                                                },
                                                onClaimQuest = { qId, coins, xp ->
                                                    scope.launch {
                                                        if (extrasRepo.claimQuest(qId, coins, xp)) {
                                                            localToast = "🏆 Misi selesai! +$coins 🪙, +$xp XP!"
                                                        }
                                                    }
                                                }
                                            )
                                            WatchTab.FEATURES -> FeaturesRegistryScreen()
                                        }
                                    } else if (currentScreen == "stage" && selectedGame != null) {
                                        GameStageScreen(
                                            game = selectedGame!!,
                                            gameMode = gameMode,
                                            botDiff = botDiff,
                                            roomCode = cloudRoom?.roomCode ?: wifiState.roomCode,
                                            isLandscape = effectiveLandscape,
                                            extras = extras,
                                            wifiState = wifiState,
                                            wifiManager = wifiRoomManager,
                                            onToggleOrientation = toggleOrientation,
                                            onBackToHub = { currentScreen = "hub" },
                                            onOpenRoomService = { showRoomDialog = true },
                                            onOpenHowToPlay = { showHowToPlayModal = true },
                                            onRecordScore = { pts ->
                                                hubViewModel.recordScore(pts.toLong())
                                                scope.launch {
                                                    extrasRepo.recordPlaySession(selectedGame!!.id, pts)
                                                }
                                            }
                                        )
                                    }

                                    val bannerText = localToast ?: statusBanner
                                    if (bannerText != null) {
                                        Snackbar(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(12.dp),
                                            action = {
                                                TextButton(onClick = {
                                                    localToast = null
                                                    hubViewModel.clearBanner()
                                                }) {
                                                    Text("OK", maxLines = 1, softWrap = false)
                                                }
                                            }
                                        ) {
                                            Text(bannerText)
                                        }
                                    }
                                }
                            }

                            // Pending WiFi Game Invitation Popup (Reference Image 7)
                            wifiState.pendingInvite?.let { invite ->
                                InvitationPopupDialog(
                                    invite = invite,
                                    onAccept = {
                                        wifiRoomManager.dismissInvitation()
                                        val targetGame = allGames.firstOrNull { it.id == invite.gameId } ?: allGames.first()
                                        selectedGame = targetGame
                                        gameMode = GameModeType.LOBBY
                                        currentScreen = "stage"
                                    },
                                    onDecline = { wifiRoomManager.dismissInvitation() }
                                )
                            }

                            if (showRoomDialog) {
                                RoomServiceDialog(
                                    currentMode = gameMode,
                                    botDiff = botDiff,
                                    roomCode = roomCode,
                                    userProfile = activeProfile,
                                    isGoogleUser = hubViewModel.isGoogleAuthenticated,
                                    cloudRoom = cloudRoom,
                                    selectedGameId = selectedGame?.id ?: "sumo_3d",
                                    onModeSelected = { gameMode = it },
                                    onBotDiffChanged = { botDiff = it },
                                    onRoomCodeChanged = { roomCode = it },
                                    onCreateCloudRoom = { code, hName, hAvatar, gId, onDone ->
                                        hubViewModel.createCloudRoom(code, hName, hAvatar, gId, onDone)
                                    },
                                    onJoinCloudRoom = { code, gName, gAvatar, onDone ->
                                        hubViewModel.joinCloudRoom(code, gName, gAvatar, onDone)
                                    },
                                    onToggleCloudReady = { code, ready ->
                                        hubViewModel.toggleCloudGuestReady(code, ready)
                                    },
                                    onSendCloudEmote = { code, emote, sender ->
                                        hubViewModel.sendCloudEmote(code, emote, sender)
                                    },
                                    onStartCloudGame = { code ->
                                        if (cloudRoom?.hostUid == currentUser?.uid) {
                                            hubViewModel.updateCloudRoomStatus(code, "playing", selectedGame?.id)
                                        }
                                    },
                                    onDismiss = { showRoomDialog = false }
                                )
                            }

                            if (showProfileDialog) {
                                ProfileDialog(
                                    profile = activeProfile,
                                    extras = extras,
                                    isGoogleUser = hubViewModel.isGoogleAuthenticated,
                                    onSaveProfile = { name, emoji, colorHex, bio, badge, handle, onlineVis, stState, expTier, stars, onDone ->
                                        scope.launch {
                                            extrasRepo.updateSocialProfileFields(handle, onlineVis, stState, expTier, stars)
                                        }
                                        hubViewModel.updateCustomProfile(name, emoji, colorHex, bio, badge, onDone)
                                    },
                                    onCheer = {
                                        scope.launch {
                                            val total = extrasRepo.addCheer()
                                            localToast = "👍 Cheer ditambah! ($total)"
                                        }
                                    },
                                    onUpgradeToGoogle = { scope.launch { localGuestRepo.setGuestActive(false) } },
                                    onSignOut = {
                                        showProfileDialog = false
                                        currentScreen = "hub"
                                        if (hubViewModel.isGoogleAuthenticated) {
                                            signOutGoogle(CredentialManager.create(appContext), scope) {
                                                scope.launch { localGuestRepo.setGuestActive(false) }
                                            }
                                        } else {
                                            scope.launch { localGuestRepo.setGuestActive(false) }
                                        }
                                    },
                                    onDismiss = { showProfileDialog = false }
                                )
                            }

                            if (showHowToPlayModal && selectedGame != null) {
                                HowToPlayModal(
                                    game = selectedGame!!,
                                    onDismiss = { showHowToPlayModal = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (::wifiRoomManager.isInitialized) {
            wifiRoomManager.stopAll()
        }
        SoundHelper.release()
        super.onDestroy()
    }
}
