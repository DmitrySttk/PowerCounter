package com.example.powercounter.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * Model: Определяет структуру данных для игрока.
 * Этот класс является Serializable для сохранения в DataStore.
 */
@Serializable
data class Player(
    val id: Int,
    val name: String,
    val level: Int = 1,
    val gear: Int = 0,
    val cardColorIndex: Int = 0
) {
    val totalPower: Int
        get() = level + gear
}

/**
 * Источник данных для цветов.
 */
val cardColors = listOf(
    Color(0xFF6c757d), Color(0xFF5f798d), Color(0xFF667d60),
    Color(0xFF8d6b62), Color(0xFF8d6e89), Color(0xFF9d875c),
    Color(0xFF5a6e8a), Color(0xFFa1a1a1), Color(0xFF7a6c5d),
    Color(0xFF4a5e5a), Color(0xFF9a7e6e), Color(0xFF635f6d),
    Color(0xFFb0a38f), Color(0xFF7e8a97), Color(0xFF9c8c82),
    Color(0xFF566573), Color(0xFFa49e97), Color(0xFF8c9288)
)
