package com.example.model

import androidx.compose.ui.graphics.Color

enum class TeamSlotColor(val label: String, val hex: Long) {
    RED("MERAH", 0xFFEF4444),
    BLUE("BIRU", 0xFF38BDF8),
    YELLOW("KUNING", 0xFFF59E0B),
    GREEN("HIJAU", 0xFF22C55E),
    SPECTATOR("PENONTON", 0xFF94A3B8)
}

data class LobbyRulesConfig(
    val reselectCharacterEveryGame: Boolean = false,
    val randomTeams: Boolean = false,
    val disableKnockOut: Boolean = false,
    val disableWeapons: Boolean = false,
    val disableFriendlyFire: Boolean = true,
    val disableItemThrow: Boolean = false,
    val maxHpPercent: Int = 100,
    val maxStaminaPercent: Int = 100,
    val staminaRegenPercent: Int = 100,
    val teamCount: Int = 4,
    val isLocked: Boolean = false
)

data class WifiPlayerSlot(
    val id: String,
    val name: String,
    val avatar: String,
    val team: TeamSlotColor,
    val isReady: Boolean,
    val isHost: Boolean,
    val isBot: Boolean = false,
    val pingMs: Int = 0
)

data class DiscoveredWifiRoom(
    val serviceName: String,
    val roomCode: String,
    val hostName: String,
    val hostIp: String,
    val port: Int,
    val gameId: String = "sumo_3d"
)

data class LobbyChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val isSystem: Boolean = false
)

data class RoomInvitation(
    val hostName: String,
    val gameId: String,
    val gameTitle: String,
    val hostIp: String,
    val port: Int,
    val expiresInSec: Int = 15
)

data class BuddyProfileItem(
    val id: String,
    val name: String,
    val handle: String,
    val avatar: String,
    val status: String, // "Tersedia", "Sedang Main", "Sibuk"
    val currentGameTitle: String,
    val ratingStars: Int,
    val experience: String,
    val isOnline: Boolean
)

data class StoreCosmeticItem(
    val id: String,
    val title: String,
    val emoji: String,
    val category: String, // "Joystick", "Bingkai", "Topi", "Tema"
    val priceCoins: Int,
    val accentColor: Color,
    val description: String
)

data class DailyQuestItem(
    val id: String,
    val title: String,
    val rewardCoins: Int,
    val rewardXp: Int,
    val targetCount: Int
)

data class HubFeatureInfo(
    val number: Int,
    val category: String,
    val title: String,
    val detail: String
)

object HubFeaturesCatalog {
    val STORE_ITEMS = listOf(
        StoreCosmeticItem("joy_neon", "Joystick Neon Siber", "🕹️", "Joystick", 0, Color(0xFF38BDF8), "Kulit lalai bercahaya biru neon 360°."),
        StoreCosmeticItem("joy_gold", "Joystick Emas Diraja", "👑", "Joystick", 120, Color(0xFFFBBF24), "Cincin emas berkilau mewah."),
        StoreCosmeticItem("joy_flame", "Joystick Api Parti", "🔥", "Joystick", 150, Color(0xFFF97316), "Gelung oren berapi pantas."),
        StoreCosmeticItem("joy_emerald", "Joystick Zamrud", "💚", "Joystick", 140, Color(0xFF22C55E), "Sentuhan hijau segar tenang."),
        StoreCosmeticItem("joy_galaxy", "Joystick Galaksi Ungu", "🌌", "Joystick", 200, Color(0xFFA855F7), "Aura kosmik ungu bertenaga."),
        StoreCosmeticItem("joy_rose", "Joystick Bunga Sakura", "🌸", "Joystick", 180, Color(0xFFEC4899), "Warna merah jambu ceria."),
        StoreCosmeticItem("hat_crown", "Mahkota Juara", "👑", "Topi", 220, Color(0xFFFBBF24), "Lencana mahkota di profil anda."),
        StoreCosmeticItem("hat_wizard", "Topi Ahli Sihir", "🧙", "Topi", 160, Color(0xFF8B5CF6), "Gaya ajaib untuk peminat teka-teki."),
        StoreCosmeticItem("hat_party", "Topi Parti Konfeti", "🥳", "Topi", 100, Color(0xFFEC4899), "Meriahkan setiap bilik WiFi!"),
        StoreCosmeticItem("hat_headset", "Set Kepala Pro", "🎧", "Topi", 175, Color(0xFF38BDF8), "Khas untuk pemain arena pantas."),
        StoreCosmeticItem("frame_rainbow", "Bingkai Pelangi", "🌈", "Bingkai", 250, Color(0xFFF59E0B), "Bingkai profil berwarna ceria."),
        StoreCosmeticItem("frame_coral", "Bingkai Rec Coral", "🧡", "Bingkai", 90, Color(0xFFFF6B35), "Diinspirasikan daripada Watch Menu!"),
        StoreCosmeticItem("theme_party", "Tema Pesta Kuning", "💛", "Tema", 130, Color(0xFFFDE047), "Suasana ceria ala Custom Game."),
        StoreCosmeticItem("theme_cyber", "Tema Malam Siber", "🌃", "Tema", 130, Color(0xFF06B6D4), "Kilauan biru & ungu neon.")
    )

    val DAILY_QUESTS = listOf(
        DailyQuestItem("q_play3", "Main 3 Permainan Hari Ini", 80, 120, 3),
        DailyQuestItem("q_score100", "Kumpul 100 Mata Skor", 100, 150, 100),
        DailyQuestItem("q_wifi", "Buka / Cuba Bilik WiFi", 90, 100, 1),
        DailyQuestItem("q_fav", "Simpan 2 Permainan Kegemaran", 50, 80, 2),
        DailyQuestItem("q_profile", "Kemas Kini Profil & Mood", 60, 75, 1)
    )

    val DEFAULT_BUDDIES = listOf(
        BuddyProfileItem("b_lapo", "Lapo", "@lapo_my", "🐼", "Tersedia", "3D Sumo Bumper", 4, "Pro", true),
        BuddyProfileItem("b_mary", "Mary", "@mary_ria", "🦊", "Sedang Main", "Kad Warna Uno", 5, "Master", true),
        BuddyProfileItem("b_joe", "Joe", "@joe_arcade", "🤖", "Berehat", "Pong Neon", 3, "Amateur", true),
        BuddyProfileItem("b_ann", "Ann", "@ann_puzzle", "🐰", "Sibuk", "3D Hide & Seek", 4, "Legend", false)
    )

    val WATCH_SHORTCUTS = listOf(
        Triple("play", "🎮", "Main"),
        Triple("wifi", "📶", "WiFi Room"),
        Triple("store", "🛒", "Kedai"),
        Triple("challenges", "🏆", "Cabaran"),
        Triple("people", "👥", "Rakan"),
        Triple("backpack", "🎒", "Beg"),
        Triple("features", "✨", "100+ Fitur")
    )

    // Complete 100+ New Features Registry so every feature is discoverable & interactive!
    val ALL_105_FEATURES: List<HubFeatureInfo> by lazy {
        val raw = listOf(
            "WiFi Room" to "Pengesan Alamat IPv4 WiFi & Hotspot Automatik",
            "WiFi Room" to "Penyiaran NSD Android (_miniverse._tcp.) untuk Penemuan Bilik",
            "WiFi Room" to "Pengimbas Bilik WiFi LAN Masa Nyata dengan MulticastLock",
            "WiFi Room" to "Pelayan Soket TCP Tempatan (Port 8989 + Auto Fallback)",
            "WiFi Room" to "Sambungan Terus IP:Port Manual untuk Rangkaian Terhad",
            "WiFi Room" to "Meter Ping / Latensi Masa Nyata (ms) dalam Bilik WiFi",
            "WiFi Room" to "Senarai Active Games dengan Slot Pemain & Penonton",
            "WiFi Room" to "Butang Quick Join Pantas ke Bilik WiFi Tersedia",
            "WiFi Room" to "4 Pasukan Warna Custom Game (Merah, Biru, Kuning, Hijau)",
            "WiFi Room" to "4 Slot Penonton (Spectator) dalam Bilik Tersuai",
            "WiFi Room" to "Kunci / Buka Bilik (Room Lock Toggle)",
            "WiFi Room" to "Panel Lobby Info dengan 10 Peraturan Tersuai",
            "WiFi Room" to "Pilihan Reselect Character Every Game",
            "WiFi Room" to "Pilihan Random Teams Automatik",
            "WiFi Room" to "Suis Disable Knock-Out & Disable Weapons",
            "WiFi Room" to "Suis Disable Friendly Fire & Item Throw",
            "WiFi Room" to "Pelaras Maximum HP (50% - 200%) & Stamina (50% - 200%)",
            "WiFi Room" to "Pelaras Stamina Regen Speed & Bilangan Pasukan (2 / 4)",
            "WiFi Room" to "Sembang Teks Langsung Bilik WiFi & Quick Emotes Selamat",
            "WiFi Room" to "Pop-up Jemputan Permainan (Invitation) dengan Pemasa Undur 15s",
            "WiFi Room" to "Butang Terima / Tolak Jemputan Masa Nyata",
            "WiFi Room" to "Penyegerakan Koordinat & Skor Pemain 2 melalui Soket WiFi",
            "WiFi Room" to "Sokongan Tambah Rakan Uji BOT dalam Bilik WiFi / Awan",
            "WiFi Room" to "Sokongan Host Start Game & Reset Pusingan Bilik",
            "WiFi Room" to "Status Sedia (Ready / Not Ready) Berwarna Hijau & Merah",

            "Kawalan 360°" to "Joystick Bulat Analog 360° Bergerak Bebas",
            "Kawalan 360°" to "Pengesanan Jarak Magnitud Analog (Laju vs Perlahan)",
            "Kawalan 360°" to "Sokongan Multi-Sentuh (Joystick + Butang Aksi Serentak)",
            "Kawalan 360°" to "Animasi Lantunan Kembali ke Tengah Apabila Dilepaskan",
            "Kawalan 360°" to "Penunjuk Sudut & Arah Bercahaya Dinamik",
            "Kawalan 360°" to "Zon Mati (Deadzone) Pintar Mengelak Sentuhan Tidak Sengaja",
            "Kawalan 360°" to "6 Pilihan Kulit Joystick Boleh Dibuka (Neon, Emas, Api, dll)",
            "Kawalan 360°" to "Pilihan Saiz Joystick (Kecil, Sederhana, Besar)",
            "Kawalan 360°" to "Suis Tukar Tangan Kiri / Kanan untuk Keselesaan",
            "Kawalan 360°" to "Sokongan Penuh Papan Kekunci WASD, Anak Panah & Space",

            "Watch Menu UI" to "Bar Navigasi Bawah Gaya Watch Menu Coral-Oren",
            "Watch Menu UI" to "Lencana Tahap (Level) & Bar Kemajuan XP Pemain",
            "Watch Menu UI" to "Paparan @handle Unik di Header Utama",
            "Watch Menu UI" to "Dompet Syiling Emas (Coins) & Permata (Gems) Langsung",
            "Watch Menu UI" to "Loceng Notifikasi dengan Lencana Bilangan Belum Dibaca",
            "Watch Menu UI" to "Grid Ikon Pintasan Pantas (Play, WiFi, Store, Cabaran, Rakan, Beg)",
            "Watch Menu UI" to "Rak Melintang 'Teruskan Bermain' (Continue Playing)",
            "Watch Menu UI" to "Rak Melintang 'Top 12 Pilihan Minggu Ini'",
            "Watch Menu UI" to "Rak Melintang '60+ Permainan Baharu Ditambah'",
            "Watch Menu UI" to "Suis Orientasi Potret <-> Landskap Tanpa Putus",
            "Watch Menu UI" to "Susun Atur Anti-Terhimpit dengan Teks Satu Baris Wajib",
            "Watch Menu UI" to "Tema Mod Gelap & Mod Cerah dengan Aksen Emas & Biru Langit",
            "Watch Menu UI" to "Butang Bisu / Bunyi Audio Sintetik Pantas",
            "Watch Menu UI" to "Skrin Pemuatan Maskot Melompat Interaktif",
            "Watch Menu UI" to "Modal Panduan Cara Bermain Tersuai Mengikut Permainan",

            "Profil & Sosial" to "Log Masuk Google Selamat (Credential Manager) + Simpanan Awan",
            "Profil & Sosial" to "Mod Tetamu Setempat (DataStore) Tanpa Perlu Log Masuk",
            "Profil & Sosial" to "Naik Taraf Akaun Tetamu ke Google Pada Bila-bila Masa",
            "Profil & Sosial" to "Editor Profil Bertab (Sesuaikan Profil, Kad Awam, Akaun)",
            "Profil & Sosial" to "20 Pilihan Avatar Maskot Comel",
            "Profil & Sosial" to "10 Pilihan Warna Bingkai Avatar",
            "Profil & Sosial" to "12 Gelaran / Lencana Pencapaian Pemain",
            "Profil & Sosial" to "Nama Panggilan & @Handle Tersuai dengan Had Aksara",
            "Profil & Sosial" to "Status Kehadiran (Tersedia, Sedang Main, Sibuk, Berehat)",
            "Profil & Sosial" to "Suis Online / Offline pada Profil Rakan",
            "Profil & Sosial" to "Pilihan Tahap Pengalaman (Novice hingga Legend)",
            "Profil & Sosial" to "Sistem Penarafan 1 hingga 5 Bintang",
            "Profil & Sosial" to "Medan Mood / Bio Peribadi Pemain",
            "Profil & Sosial" to "Kad Profil Awam dengan Statistik Sorotan",
            "Profil & Sosial" to "Butang Cheer / Suka (👍) & Kaunter Peminat",
            "Profil & Sosial" to "Butang Hantar Hadiah Maya (🎁) kepada Rakan",
            "Profil & Sosial" to "Senarai Rakan (Buddies) dengan Status & Permainan Aktif",
            "Profil & Sosial" to "Fungsi Tambah Rakan (Add Buddy) & Jemput Main Terus",

            "Kedai & Cabaran" to "Kedai Kosmetik dengan 14 Item Boleh Dibeli",
            "Kedai & Cabaran" to "Beg Galas (Backpack) untuk Mengurus & Memakai Item",
            "Kedai & Cabaran" to "Sistem Ganjaran Syiling Selepas Setiap Pusingan Permainan",
            "Kedai & Cabaran" to "Hadiah Log Masuk Harian (Daily Streak Bonus)",
            "Kedai & Cabaran" to "5 Misi Cabaran Harian dengan Ganjaran Syiling & XP",
            "Kedai & Cabaran" to "12 Lencana Pencapaian Boleh Dibuka Automatik",
            "Kedai & Cabaran" to "Pengira Kombo Skor & Rekod Permainan Terakhir",
            "Kedai & Cabaran" to "Sistem Kegemaran Disegerakkan ke Awan / Peranti",
            "Kedai & Cabaran" to "Penjana Permainan Rawak dengan Animasi Bunyi Syiling",
            "Kedai & Cabaran" to "Penapis Pantas 7 Kategori + Carian Masa Nyata",

            "Katalog 202 Game" to "68 Permainan Baharu Ditambah (Jumlah 202 Permainan!)",
            "Katalog 202 Game" to "15 Arena 3D Baharu Termasuk Last Stand & Perang Cat 3D",
            "Katalog 202 Game" to "15 Permainan Aksi 2D Baharu Termasuk Go-Kart & Lebah Madu",
            "Katalog 202 Game" to "12 Permainan Papan Baharu Termasuk Jutawan Mini & Bingo",
            "Katalog 202 Game" to "12 Teka-Teki Baharu Termasuk Pecah Kod & Teka Emoji",
            "Katalog 202 Game" to "14 Permainan Parti Baharu Termasuk Kentang Panas & Pinata",
            "Katalog 202 Game" to "Enjin Penembak Angkasa & Peluru untuk Kategori Shooter",
            "Katalog 202 Game" to "Enjin Perlumbaan & Elak Halangan untuk Kategori Lumba",
            "Katalog 202 Game" to "Enjin Kutipan Bintang & Gelanggang Sukan 2D",
            "Katalog 202 Game" to "Enjin Kuiz Minda & Teka-Teki Interaktif Bermatlamat",
            "Katalog 202 Game" to "Enjin Papan & Padanan Strategi Bergilir",
            "Katalog 202 Game" to "Enjin Parti Dadu, Roda Impian & Refleks Berbilang Pemain",
            "Katalog 202 Game" to "Sokongan BOT 3 Tahap Kesukaran (Santai, Mencabar, Pakar)",
            "Katalog 202 Game" to "Reaksi Dialog Lucu BOT Ramah Umur Semasa Bermain",
            "Katalog 202 Game" to "Mod Local 2 Pemain Dwi-Zon pada Satu Skrin Telefon",
            "Katalog 202 Game" to "Mod Pass & Play Bergilir untuk Permainan Papan & Kad",
            "Katalog 202 Game" to "Audio Sintetik ToneGenerator untuk Klik, Boing, Coin & Fanfare",
            "Katalog 202 Game" to "Fizik Gelanggang Sumo 3D dengan Impuls Pelanggaran Sebenar",
            "Katalog 202 Game" to "Penjagaan Kawasan Selamat Takuk Skrin & Bar Navigasi Sistem",
            "Katalog 202 Game" to "Direktori 105+ Fitur Baharu Boleh Dicari & Dilayari"
        )
        raw.mapIndexed { idx, (cat, title) ->
            HubFeatureInfo(
                number = idx + 1,
                category = cat,
                title = title,
                detail = "Aktif dan sedia digunakan dalam MiniVerse Hub v2.0."
            )
        }
    }
}
