package com.example.model

data class GameItem(
    val id: String,
    val title: String,
    val category: String, // "3d", "aksi", "papan", "teka", "parti"
    val emoji: String,
    val description: String,
    val is3D: Boolean = false,
    val isPlayable: Boolean = true,
    val goalHint: String = "Kumpul mata tertinggi atau kalahkan lawan mengikut peraturan permainan.",
    val controlHint: String = "Gunakan D-Pad / sentuhan skrin atau kekunci WASD / Anak Panah + Space.",
    val modeHint: String = "VS BOT (Santai/Sederhana/Pakar), Local 2 Pemain (1 Peranti), atau Bilik Rakan 4-Digit."
)
