package com.example.powercounter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.powercounter.data.Player
import com.example.powercounter.data.PlayerRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel: Управляет состоянием UI и обрабатывает бизнес-логику.
 * Является посредником между View (Composable) и Model (Repository).
 */
class MunchkinViewModel(private val repository: PlayerRepository) : ViewModel() {

    val playersState: StateFlow<List<Player>> = repository.playersFlow

    private fun updatePlayers(updatedPlayers: List<Player>) {
        viewModelScope.launch {
            repository.savePlayers(updatedPlayers)
        }
    }

    fun addPlayer() {
        val currentPlayers = playersState.value
        if (currentPlayers.size < 6) {
            val newId = (currentPlayers.maxOfOrNull { it.id } ?: 0) + 1
            val newPlayer = Player(id = newId, name = "Игрок ${currentPlayers.size + 1}", cardColorIndex = 0)
            updatePlayers(currentPlayers + newPlayer)
        }
    }

    fun resetPlayers() {
        updatePlayers(listOf(Player(id = 1, name = "Игрок 1")))
    }

    fun deletePlayer(player: Player) {
        val currentPlayers = playersState.value
        if (currentPlayers.size > 1) {
            updatePlayers(currentPlayers.filter { it.id != player.id })
        }
    }

    fun updatePlayer(updatedPlayer: Player) {
        val currentPlayers = playersState.value
        val updatedList = currentPlayers.map {
            if (it.id == updatedPlayer.id) updatedPlayer else it
        }
        updatePlayers(updatedList)
    }
}

/**
 * Фабрика для создания ViewModel с зависимостью (репозиторием).
 * Это стандартный подход в Android для ViewModel с параметрами в конструкторе.
 */
class MunchkinViewModelFactory(private val repository: PlayerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MunchkinViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MunchkinViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
