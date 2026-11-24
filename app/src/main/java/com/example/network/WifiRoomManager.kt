package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.model.DiscoveredWifiRoom
import com.example.model.LobbyChatMessage
import com.example.model.LobbyRulesConfig
import com.example.model.RoomInvitation
import com.example.model.TeamSlotColor
import com.example.model.WifiPlayerSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

enum class WifiConnectionRole { NONE, HOST, CLIENT }

data class WifiRoomState(
    val isWifiConnected: Boolean = false,
    val localIp: String = "0.0.0.0",
    val listenPort: Int = 8989,
    val role: WifiConnectionRole = WifiConnectionRole.NONE,
    val isHosting: Boolean = false,
    val isScanning: Boolean = false,
    val isConnectedToRoom: Boolean = false,
    val roomCode: String = "4040",
    val activeGameId: String = "sumo_3d",
    val roomStatus: String = "waiting", // "waiting", "playing"
    val pingMs: Int = 0,
    val statusLog: String = "Sedia untuk cipta atau imbas Bilik WiFi LAN.",
    val discoveredRooms: List<DiscoveredWifiRoom> = emptyList(),
    val players: List<WifiPlayerSlot> = emptyList(),
    val chatMessages: List<LobbyChatMessage> = emptyList(),
    val rules: LobbyRulesConfig = LobbyRulesConfig(),
    val pendingInvite: RoomInvitation? = null,
    val remoteMoveX: Float = 0f,
    val remoteMoveY: Float = 0f,
    val remoteScore: Int = 0,
    val remoteName: String? = null
)

class WifiRoomManager(context: Context) {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(WifiRoomState())
    val state: StateFlow<WifiRoomState> = _state.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var clientSocket: Socket? = null
    private var clientWriter: BufferedWriter? = null
    private val connectedClients = CopyOnWriteArrayList<ClientHandler>()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var pingJob: Job? = null

    private var localPlayerName = "Pemain"
    private var localPlayerAvatar = "🦊"
    private var localPlayerTeam = TeamSlotColor.RED
    private var localReady = false

    private class ClientHandler(
        val id: String,
        val socket: Socket,
        val writer: BufferedWriter
    )

    init {
        refreshNetworkInfo()
    }

    fun refreshNetworkInfo() {
        val ip = detectLocalIpv4()
        val wifiOk = isWifiOrHotspotUp(ip)
        _state.value = _state.value.copy(
            isWifiConnected = wifiOk,
            localIp = ip
        )
    }

    private fun isWifiOrHotspotUp(detectedIp: String): Boolean {
        if (detectedIp != "0.0.0.0" && !detectedIp.startsWith("127.")) return true
        val cm = connectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun detectLocalIpv4(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "0.0.0.0"
            var fallback = "0.0.0.0"
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        if (intf.name.contains("wlan", ignoreCase = true) ||
                            intf.name.contains("ap", ignoreCase = true) ||
                            host.startsWith("192.168.")
                        ) {
                            return host
                        }
                        fallback = host
                    }
                }
            }
            return fallback
        } catch (e: Exception) {
            Log.w(TAG, "IP detection error: ${e.message}")
            return "0.0.0.0"
        }
    }

    fun hostWifiRoom(
        roomCode: String,
        hostName: String,
        hostAvatar: String,
        gameId: String
    ) {
        stopAll()
        refreshNetworkInfo()
        localPlayerName = hostName.trim().ifBlank { "Host" }.take(24)
        localPlayerAvatar = hostAvatar.ifBlank { "🦊" }
        localPlayerTeam = TeamSlotColor.RED
        localReady = true

        scope.launch {
            try {
                val srv = try {
                    ServerSocket(8989)
                } catch (_: Exception) {
                    ServerSocket(0)
                }
                serverSocket = srv
                val actualPort = srv.localPort
                val ip = detectLocalIpv4()
                val hostSlot = WifiPlayerSlot(
                    id = "host",
                    name = localPlayerName,
                    avatar = localPlayerAvatar,
                    team = localPlayerTeam,
                    isReady = true,
                    isHost = true,
                    pingMs = 1
                )

                _state.value = _state.value.copy(
                    isWifiConnected = isWifiOrHotspotUp(ip),
                    localIp = ip,
                    listenPort = actualPort,
                    role = WifiConnectionRole.HOST,
                    isHosting = true,
                    isConnectedToRoom = true,
                    roomCode = roomCode,
                    activeGameId = gameId,
                    roomStatus = "waiting",
                    players = listOf(hostSlot),
                    chatMessages = listOf(
                        LobbyChatMessage(
                            id = System.nanoTime().toString(),
                            sender = "Sistem WiFi",
                            text = "Bilik WiFi $roomCode dibuka di $ip:$actualPort! Rakan di WiFi sama boleh tekan Imbas atau sambung IP.",
                            isSystem = true
                        )
                    ),
                    statusLog = "Menunggu rakan WiFi di $ip:$actualPort"
                )

                registerNsdService(roomCode, localPlayerName, actualPort)

                acceptJob = scope.launch {
                    while (isActive && !srv.isClosed) {
                        try {
                            val client = srv.accept()
                            val clientId = "p_${System.currentTimeMillis() % 10000}"
                            val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream()))
                            val handler = ClientHandler(clientId, client, writer)
                            connectedClients.add(handler)
                            listenToClient(handler)
                        } catch (e: Exception) {
                            if (!srv.isClosed) Log.w(TAG, "Accept error: ${e.message}")
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    statusLog = "Gagal memulakan Pelayan WiFi: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun listenToClient(handler: ClientHandler) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(handler.socket.getInputStream()))
                while (isActive && !handler.socket.isClosed) {
                    val line = reader.readLine() ?: break
                    handleIncomingProtocol(line, senderHandler = handler)
                }
            } catch (_: Exception) {
            } finally {
                connectedClients.remove(handler)
                val updated = _state.value.players.filterNot { it.id == handler.id }
                _state.value = _state.value.copy(players = updated)
                broadcastStateFromHost()
            }
        }
    }

    fun scanWifiRooms() {
        stopDiscoveryOnly()
        refreshNetworkInfo()
        acquireMulticastLock()

        val found = mutableListOf<DiscoveredWifiRoom>()
        _state.value = _state.value.copy(
            isScanning = true,
            discoveredRooms = emptyList(),
            statusLog = "Mengimbas bilik MiniVerse pada WiFi / Hotspot..."
        )

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "NSD Discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (!service.serviceType.contains("_miniverse")) return
                try {
                    nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.w(TAG, "Resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(resolved: NsdServiceInfo) {
                            val hostAddr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                resolved.hostAddresses.firstOrNull { it is Inet4Address }?.hostAddress
                                    ?: resolved.hostAddresses.firstOrNull()?.hostAddress
                            } else {
                                @Suppress("DEPRECATION")
                                resolved.host?.hostAddress
                            } ?: return

                            val parts = resolved.serviceName.split("_")
                            val code = parts.getOrNull(1) ?: "4040"
                            val hName = parts.getOrNull(2) ?: resolved.serviceName
                            val room = DiscoveredWifiRoom(
                                serviceName = resolved.serviceName,
                                roomCode = code,
                                hostName = hName,
                                hostIp = hostAddr,
                                port = resolved.port
                            )
                            synchronized(found) {
                                if (found.none { it.hostIp == room.hostIp && it.port == room.port }) {
                                    found.add(room)
                                    _state.value = _state.value.copy(
                                        discoveredRooms = found.toList(),
                                        statusLog = "Dijumpai ${found.size} bilik WiFi aktif!"
                                    )
                                }
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.w(TAG, "Resolve trigger error: ${e.message}")
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                synchronized(found) {
                    found.removeAll { it.serviceName == service.serviceName }
                    _state.value = _state.value.copy(discoveredRooms = found.toList())
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _state.value = _state.value.copy(isScanning = false)
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _state.value = _state.value.copy(
                    isScanning = false,
                    statusLog = "Imbasan automatik disekat penghala ($errorCode). Guna Sambung IP Terus!"
                )
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        discoveryListener = listener
        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isScanning = false,
                statusLog = "Tidak dapat mula imbasan NSD: ${e.localizedMessage}. Guna IP Manual!"
            )
        }
    }

    fun joinWifiRoomByIp(
        hostIp: String,
        port: Int,
        playerName: String,
        playerAvatar: String
    ) {
        stopAll()
        refreshNetworkInfo()
        val cleanIp = hostIp.trim()
        if (cleanIp.isBlank()) {
            _state.value = _state.value.copy(statusLog = "Sila masukkan IP Host yang sah (cth: 192.168.1.12).")
            return
        }
        localPlayerName = playerName.trim().ifBlank { "Rakan WiFi" }.take(24)
        localPlayerAvatar = playerAvatar.ifBlank { "🐼" }
        localPlayerTeam = TeamSlotColor.BLUE
        localReady = false

        scope.launch {
            try {
                _state.value = _state.value.copy(statusLog = "Menyambung ke $cleanIp:$port...")
                val sock = Socket()
                sock.connect(InetSocketAddress(cleanIp, port), 4500)
                clientSocket = sock
                val writer = BufferedWriter(OutputStreamWriter(sock.getOutputStream()))
                clientWriter = writer

                _state.value = _state.value.copy(
                    role = WifiConnectionRole.CLIENT,
                    isHosting = false,
                    isConnectedToRoom = true,
                    statusLog = "Berjaya disambung ke Bilik WiFi $cleanIp:$port!"
                )

                sendLineFromClient("JOIN|$localPlayerName|$localPlayerAvatar|${localPlayerTeam.name}")
                startPingLoop()

                val reader = BufferedReader(InputStreamReader(sock.getInputStream()))
                while (isActive && !sock.isClosed) {
                    val line = reader.readLine() ?: break
                    handleIncomingProtocol(line, senderHandler = null)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isConnectedToRoom = false,
                    role = WifiConnectionRole.NONE,
                    statusLog = "Gagal sambung ke $cleanIp:$port (${e.localizedMessage ?: "Masa tamat"})."
                )
            }
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && _state.value.isConnectedToRoom) {
                sendLineFromClient("PING|${System.currentTimeMillis()}")
                delay(2500L)
            }
        }
    }

    fun selectTeam(team: TeamSlotColor) {
        localPlayerTeam = team
        if (_state.value.role == WifiConnectionRole.HOST) {
            val updated = _state.value.players.map {
                if (it.id == "host") it.copy(team = team) else it
            }
            _state.value = _state.value.copy(players = updated)
            broadcastStateFromHost()
        } else if (_state.value.role == WifiConnectionRole.CLIENT) {
            scope.launch {
                sendLineFromClient("READY|$localPlayerName|$localReady|${team.name}")
            }
        }
    }

    fun toggleReady() {
        localReady = !localReady
        if (_state.value.role == WifiConnectionRole.HOST) {
            val updated = _state.value.players.map {
                if (it.id == "host") it.copy(isReady = localReady) else it
            }
            _state.value = _state.value.copy(players = updated)
            broadcastStateFromHost()
        } else if (_state.value.role == WifiConnectionRole.CLIENT) {
            scope.launch {
                sendLineFromClient("READY|$localPlayerName|$localReady|${localPlayerTeam.name}")
            }
        }
    }

    fun addTestBotToWifiRoom() {
        val cur = _state.value.players
        val botId = "bot_${cur.size + 1}"
        val nextTeam = TeamSlotColor.entries[(cur.size) % 4]
        val bot = WifiPlayerSlot(
            id = botId,
            name = "BOT Uji #${cur.count { it.isBot } + 1}",
            avatar = "🤖",
            team = nextTeam,
            isReady = true,
            isHost = false,
            isBot = true,
            pingMs = 0
        )
        _state.value = _state.value.copy(
            players = cur + bot,
            chatMessages = _state.value.chatMessages + LobbyChatMessage(
                id = System.nanoTime().toString(),
                sender = "Sistem WiFi",
                text = "${bot.name} menyertai pasukan ${nextTeam.label}!",
                isSystem = true
            )
        )
        if (_state.value.role == WifiConnectionRole.HOST) {
            broadcastStateFromHost()
        }
    }

    fun updateRules(newRules: LobbyRulesConfig) {
        _state.value = _state.value.copy(rules = newRules)
        if (_state.value.role == WifiConnectionRole.HOST) {
            broadcastStateFromHost()
        }
    }

    fun sendChat(sender: String, text: String) {
        val clean = text.replace("|", " ").trim().take(120)
        if (clean.isBlank()) return
        if (_state.value.role == WifiConnectionRole.HOST) {
            appendChat(sender, clean, false)
            broadcastRaw("CHAT|$sender|$clean")
        } else if (_state.value.role == WifiConnectionRole.CLIENT) {
            scope.launch { sendLineFromClient("CHAT|$sender|$clean") }
        } else {
            appendChat(sender, clean, false)
        }
    }

    fun sendGameInvitation(hostName: String, gameId: String, gameTitle: String) {
        val st = _state.value
        val line = "INVITE|${hostName.replace("|", " ")}|$gameId|${gameTitle.replace("|", " ")}|${st.localIp}|${st.listenPort}"
        if (st.role == WifiConnectionRole.HOST) {
            broadcastRaw(line)
            appendChat("Sistem WiFi", "Jemputan bermain '$gameTitle' dihantar ke semua rakan!", true)
        } else {
            // Preview invitation locally when testing solo
            _state.value = st.copy(
                pendingInvite = RoomInvitation(hostName, gameId, gameTitle, st.localIp, st.listenPort, 15)
            )
        }
    }

    fun dismissInvitation() {
        _state.value = _state.value.copy(pendingInvite = null)
    }

    fun broadcastHostGameStart(gameId: String) {
        _state.value = _state.value.copy(activeGameId = gameId, roomStatus = "playing")
        if (_state.value.role == WifiConnectionRole.HOST) {
            broadcastStateFromHost()
        }
    }

    fun sendGameplayTelemetry(moveX: Float, moveY: Float, score: Int) {
        if (!_state.value.isConnectedToRoom) return
        val msg = "INPUT|$localPlayerName|$moveX|$moveY|$score"
        if (_state.value.role == WifiConnectionRole.HOST) {
            broadcastRaw(msg)
        } else if (_state.value.role == WifiConnectionRole.CLIENT) {
            scope.launch { sendLineFromClient(msg) }
        }
    }

    private fun handleIncomingProtocol(line: String, senderHandler: ClientHandler?) {
        val p = line.split("|")
        when (p.firstOrNull()) {
            "PING" -> {
                val ts = p.getOrNull(1) ?: "0"
                senderHandler?.let { writeToClient(it, "PONG|$ts") }
            }
            "PONG" -> {
                val ts = p.getOrNull(1)?.toLongOrNull() ?: return
                val rtt = (System.currentTimeMillis() - ts).coerceIn(1L, 999L).toInt()
                _state.value = _state.value.copy(pingMs = rtt)
            }
            "JOIN" -> {
                if (senderHandler != null) {
                    val name = p.getOrNull(1) ?: "Rakan"
                    val avatar = p.getOrNull(2) ?: "🐼"
                    val team = runCatching { TeamSlotColor.valueOf(p.getOrNull(3) ?: "BLUE") }
                        .getOrDefault(TeamSlotColor.BLUE)
                    val slot = WifiPlayerSlot(
                        id = senderHandler.id,
                        name = name,
                        avatar = avatar,
                        team = team,
                        isReady = false,
                        isHost = false,
                        pingMs = 12
                    )
                    val updated = _state.value.players.filterNot { it.id == senderHandler.id } + slot
                    _state.value = _state.value.copy(players = updated)
                    appendChat("Sistem WiFi", "$avatar $name menyertai Bilik WiFi!", true)
                    broadcastStateFromHost()
                }
            }
            "READY" -> {
                if (senderHandler != null) {
                    val ready = p.getOrNull(2)?.toBooleanStrictOrNull() ?: false
                    val team = runCatching { TeamSlotColor.valueOf(p.getOrNull(3) ?: "BLUE") }
                        .getOrDefault(TeamSlotColor.BLUE)
                    val updated = _state.value.players.map {
                        if (it.id == senderHandler.id) it.copy(isReady = ready, team = team) else it
                    }
                    _state.value = _state.value.copy(players = updated)
                    broadcastStateFromHost()
                }
            }
            "CHAT" -> {
                val sender = p.getOrNull(1) ?: "Rakan"
                val text = p.getOrNull(2) ?: ""
                appendChat(sender, text, false)
                if (_state.value.role == WifiConnectionRole.HOST && senderHandler != null) {
                    broadcastRaw(line, excludeId = senderHandler.id)
                }
            }
            "INVITE" -> {
                val hostName = p.getOrNull(1) ?: "Host"
                val gameId = p.getOrNull(2) ?: "sumo_3d"
                val gameTitle = p.getOrNull(3) ?: "MiniVerse Game"
                val ip = p.getOrNull(4) ?: _state.value.localIp
                val port = p.getOrNull(5)?.toIntOrNull() ?: 8989
                _state.value = _state.value.copy(
                    pendingInvite = RoomInvitation(hostName, gameId, gameTitle, ip, port, 15)
                )
            }
            "STATE" -> {
                // Format: STATE|roomCode|gameId|status|locked|maxHp|playersSerialized
                val code = p.getOrNull(1) ?: _state.value.roomCode
                val gId = p.getOrNull(2) ?: _state.value.activeGameId
                val status = p.getOrNull(3) ?: "waiting"
                val locked = p.getOrNull(4)?.toBooleanStrictOrNull() ?: false
                val maxHp = p.getOrNull(5)?.toIntOrNull() ?: 100
                val playersRaw = p.getOrNull(6) ?: ""
                val parsedPlayers = playersRaw.split(";").filter { it.isNotBlank() }.mapNotNull { entry ->
                    val cols = entry.split(",")
                    if (cols.size >= 6) {
                        WifiPlayerSlot(
                            id = cols[0],
                            name = cols[1],
                            avatar = cols[2],
                            team = runCatching { TeamSlotColor.valueOf(cols[3]) }.getOrDefault(TeamSlotColor.BLUE),
                            isReady = cols[4].toBooleanStrictOrNull() ?: false,
                            isHost = cols[5].toBooleanStrictOrNull() ?: false
                        )
                    } else null
                }
                _state.value = _state.value.copy(
                    roomCode = code,
                    activeGameId = gId,
                    roomStatus = status,
                    rules = _state.value.rules.copy(isLocked = locked, maxHpPercent = maxHp),
                    players = if (parsedPlayers.isNotEmpty()) parsedPlayers else _state.value.players
                )
            }
            "INPUT" -> {
                val name = p.getOrNull(1) ?: "Lawan"
                val mx = p.getOrNull(2)?.toFloatOrNull() ?: 0f
                val my = p.getOrNull(3)?.toFloatOrNull() ?: 0f
                val rScore = p.getOrNull(4)?.toIntOrNull() ?: 0
                _state.value = _state.value.copy(
                    remoteName = name,
                    remoteMoveX = mx,
                    remoteMoveY = my,
                    remoteScore = rScore
                )
                if (_state.value.role == WifiConnectionRole.HOST && senderHandler != null) {
                    broadcastRaw(line, excludeId = senderHandler.id)
                }
            }
        }
    }

    private fun broadcastStateFromHost() {
        val st = _state.value
        val serializedPlayers = st.players.joinToString(";") {
            "${it.id},${it.name.replace(",", " ").replace(";", " ")},${it.avatar},${it.team.name},${it.isReady},${it.isHost}"
        }
        val line = "STATE|${st.roomCode}|${st.activeGameId}|${st.roomStatus}|${st.rules.isLocked}|${st.rules.maxHpPercent}|$serializedPlayers"
        broadcastRaw(line)
    }

    private fun broadcastRaw(line: String, excludeId: String? = null) {
        scope.launch {
            for (c in connectedClients) {
                if (c.id == excludeId) continue
                writeToClient(c, line)
            }
        }
    }

    private fun writeToClient(handler: ClientHandler, line: String) {
        try {
            handler.writer.write(line)
            handler.writer.newLine()
            handler.writer.flush()
        } catch (_: Exception) {
            connectedClients.remove(handler)
        }
    }

    private suspend fun sendLineFromClient(line: String) {
        try {
            clientWriter?.let {
                it.write(line)
                it.newLine()
                it.flush()
            }
        } catch (_: Exception) {}
    }

    private fun appendChat(sender: String, text: String, isSystem: Boolean) {
        val next = (_state.value.chatMessages + LobbyChatMessage(
            id = System.nanoTime().toString(),
            sender = sender,
            text = text,
            isSystem = isSystem
        )).takeLast(40)
        _state.value = _state.value.copy(chatMessages = next)
    }

    private fun registerNsdService(roomCode: String, hostName: String, port: Int) {
        try {
            val cleanName = hostName.replace(Regex("[^A-Za-z0-9]"), "").take(10).ifBlank { "Host" }
            val info = NsdServiceInfo().apply {
                serviceName = "MiniVerse_${roomCode}_$cleanName"
                serviceType = SERVICE_TYPE
                setPort(port)
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                    Log.d(TAG, "NSD registered: ${nsdServiceInfo.serviceName}")
                }
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "NSD registration failed: $errorCode")
                }
                override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            }
            registrationListener = listener
            nsdManager?.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.w(TAG, "registerNsdService error: ${e.message}")
        }
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("miniverse_wifi_nsd")?.apply {
                    setReferenceCounted(false)
                }
            }
            multicastLock?.takeIf { !it.isHeld }?.acquire()
        } catch (_: Exception) {}
    }

    private fun stopDiscoveryOnly() {
        discoveryListener?.let {
            try { nsdManager?.stopServiceDiscovery(it) } catch (_: Exception) {}
        }
        discoveryListener = null
        try {
            multicastLock?.takeIf { it.isHeld }?.release()
        } catch (_: Exception) {}
    }

    fun stopAll() {
        pingJob?.cancel()
        acceptJob?.cancel()
        stopDiscoveryOnly()
        registrationListener?.let {
            try { nsdManager?.unregisterService(it) } catch (_: Exception) {}
        }
        registrationListener = null
        connectedClients.forEach { try { it.socket.close() } catch (_: Exception) {} }
        connectedClients.clear()
        try { clientSocket?.close() } catch (_: Exception) {}
        clientSocket = null
        clientWriter = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        _state.value = _state.value.copy(
            role = WifiConnectionRole.NONE,
            isHosting = false,
            isScanning = false,
            isConnectedToRoom = false,
            players = emptyList(),
            pingMs = 0
        )
    }

    private companion object {
        const val TAG = "WifiRoomManager"
        const val SERVICE_TYPE = "_miniverse._tcp."
    }
}
