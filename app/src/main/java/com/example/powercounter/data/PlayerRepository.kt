package com.example.powercounter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Создаем экземпляр DataStore на уровне всего приложения
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "munchkin_settings")

/**
 * Model: Репозиторий отвечает за чтение и запись данных игроков.
 * Он абстрагирует ViewModel от источника данных (в данном случае, DataStore).
 */
class PlayerRepository(context: Context) {
    private val dataStore = context.dataStore
    private val playersKey = stringPreferencesKey("players_list")

    // Поток (Flow), который читает данные из DataStore и десериализует их
    val playersFlow: StateFlow<List<Player>> = dataStore.data
        .map { preferences ->
            val jsonString = preferences[playersKey]
            if (jsonString != null) {
                try {
                    Json.decodeFromString<List<Player>>(jsonString)
                } catch (e: Exception) {
                    // Возвращаем список по умолчанию, если данные повреждены
                    createDefaultPlayerList()
                }
            } else {
                createDefaultPlayerList()
            }
        }.stateIn(
            scope = CoroutineScope(Dispatchers.IO),
            started = SharingStarted.Eagerly,
            initialValue = createDefaultPlayerList()
        )

    // Асинхронная функция для сохранения списка игроков
    suspend fun savePlayers(players: List<Player>) {
        val jsonString = Json.encodeToString(players)
        dataStore.edit { preferences ->
            preferences[playersKey] = jsonString
        }
    }

    private fun createDefaultPlayerList() = listOf(Player(id = 1, name = "Игрок 1"))
}
