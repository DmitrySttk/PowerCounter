package com.example.powercounter.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable/**
 * Model: Определяет структуру данных для игрока.
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
 * Новая, приглушенная и отсортированная по тонам палитра.
 * Цвета подобраны так, чтобы белый текст хорошо читался.
 */
val cardColors = listOf(
    // --- Серые/Нейтральные ---
    Color(0xFF6A7077), // Спокойный серый (по умолчанию)
    Color(0xFF53585E), // Темно-серый
    Color(0xFF7D8C9B), // Пыльный серо-синий

    // --- Синие/Фиолетовые ---
    Color(0xFF5A7D9A), // Приглушенный синий
    Color(0xFF6B6A94), // Лавандовый
    Color(0xFF8E6B8E), // Пыльный фиолетовый

    // --- Зеленые ---
    Color(0xFF607E6D), // Шалфейный
    Color(0xFF4C7C75), // Морская пена
    Color(0xFF7A8C6F), // Оливковый

    // --- Красные/Розовые ---
    Color(0xFFa26c6c), // Приглушенный красный
    Color(0xFF9E6B7E), // Пыльная роза
    Color(0xFFa06d8a), // Лиловый

    // --- Коричневые/Оранжевые ---
    Color(0xFFa87c5b), // Мягкий оранжевый
    Color(0xFF7d6c63), // Кофейный
    Color(0xFF9b8a75), // Песочный

    // --- Другие темные ---
    Color(0xFF5C6B73), // Шифер
    Color(0xFF757575), // Теплый серый
    Color(0xFF8d8d81)  // Хаки
)
