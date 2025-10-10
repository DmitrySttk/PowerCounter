package com.example.powercounter

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.powercounter.ui.theme.PowerCounterTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// --- 1. НАСТРОЙКА DATASTORE ---
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "munchkin_settings")

// --- 2. МОДЕЛЬ ДАННЫХ ---
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

val cardColors = listOf(
    Color(0xFF6c757d), Color(0xFF5f798d), Color(0xFF667d60),
    Color(0xFF8d6b62), Color(0xFF8d6e89), Color(0xFF9d875c),
    Color(0xFF5a6e8a), Color(0xFFa1a1a1), Color(0xFF7a6c5d),
    Color(0xFF4a5e5a), Color(0xFF9a7e6e), Color(0xFF635f6d),
    Color(0xFFb0a38f), Color(0xFF7e8a97), Color(0xFF9c8c82),
    Color(0xFF566573), Color(0xFFa49e97), Color(0xFF8c9288)
)

// --- 3. РЕПОЗИТОРИЙ ---
class PlayerRepository(private val dataStore: DataStore<Preferences>) {
    private val playersKey = stringPreferencesKey("players_list")

    val playersFlow: StateFlow<List<Player>> = dataStore.data
        .map { preferences ->
            val jsonString = preferences[playersKey]
            if (jsonString != null) {
                Json.decodeFromString<List<Player>>(jsonString)
            } else {
                listOf(Player(id = 1, name = "Игрок 1"))
            }
        }.stateIn(
            scope = CoroutineScope(Dispatchers.IO),
            started = SharingStarted.Eagerly,
            initialValue = listOf(Player(id = 1, name = "Игрок 1"))
        )

    suspend fun savePlayers(players: List<Player>) {
        val jsonString = Json.encodeToString(players)
        dataStore.edit { preferences ->
            preferences[playersKey] = jsonString
        }
    }
}

// --- 4. VIEWMODEL ---
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

class MunchkinViewModelFactory(private val repository: PlayerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MunchkinViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MunchkinViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- ACTIVITY ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repository = PlayerRepository(dataStore = applicationContext.dataStore)
            val viewModel: MunchkinViewModel = viewModel(factory = MunchkinViewModelFactory(repository))

            PowerCounterTheme {
                MunchkinPowerCounter(viewModel)
            }
        }
    }
}

// --- UI ---
@Composable
fun MunchkinPowerCounter(viewModel: MunchkinViewModel) {
    val players by viewModel.playersState.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TopAppBar(
                players = players,
                onAddPlayer = { viewModel.addPlayer() },
                onReset = { viewModel.resetPlayers() }
            )
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(players, key = { it.id }) { player ->
                    PlayerCard(
                        player = player,
                        onLevelChange = { newLevel -> viewModel.updatePlayer(player.copy(level = newLevel)) },
                        onGearChange = { newGear -> viewModel.updatePlayer(player.copy(gear = newGear)) },
                        onNameChange = { newName -> viewModel.updatePlayer(player.copy(name = newName)) },
                        onDeletePlayer = { viewModel.deletePlayer(player) },
                        onChangeColor = { newColorIndex -> viewModel.updatePlayer(player.copy(cardColorIndex = newColorIndex)) }
                    )
                }
            }
        }
    }
}

@Composable
fun TopAppBar(players: List<Player>, onAddPlayer: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAddPlayer, enabled = players.size < 6) {
            Icon(Icons.Default.Add, contentDescription = "Добавить игрока")
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onReset) {
            Icon(Icons.Default.Delete, contentDescription = "Сбросить всех игроков")
        }
    }
}

@Composable
fun PlayerCard(
    player: Player,
    onLevelChange: (Int) -> Unit,
    onGearChange: (Int) -> Unit,
    onNameChange: (String) -> Unit,
    onDeletePlayer: () -> Unit,
    onChangeColor: (Int) -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember(player.name) { mutableStateOf(player.name) }
    var isColorDialogShown by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    if (isColorDialogShown) {
        ColorPickerDialog(
            onColorSelected = { newColorIndex ->
                onChangeColor(newColorIndex)
                isColorDialogShown = false
            },
            onDismiss = { isColorDialogShown = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColors[player.cardColorIndex])
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { isColorDialogShown = true }, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Palette, contentDescription = "Сменить цвет", modifier = Modifier.size(20.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (isEditingName) {
                        LaunchedEffect(Unit) {
                            tempName = ""
                            focusRequester.requestFocus()
                        }
                        BasicTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center, color = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused && tempName.isNotBlank()) {
                                        onNameChange(tempName)
                                        isEditingName = false
                                    }
                                },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (tempName.isNotBlank()) {
                                    onNameChange(tempName)
                                }
                                isEditingName = false
                                focusManager.clearFocus()
                            })
                        )
                    } else {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isEditingName = true }
                        )
                    }
                }
                IconButton(onClick = onDeletePlayer, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Удалить игрока", modifier = Modifier.size(20.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Counter(
                        label = "Уровень",
                        value = player.level,
                        onValueChange = { onLevelChange(it) },
                        valueRange = 1..10,
                        isEditable = false // Уровень не редактируем вручную
                    )
                    Counter(
                        label = "Шмотки",
                        value = player.gear,
                        onValueChange = { onGearChange(it) },
                        isEditable = true // Шмотки можно редактировать
                    )
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${player.totalPower}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }
    }
}

@Composable
fun Counter(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange? = null,
    isEditable: Boolean = false
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempValue by remember(value) { mutableStateOf(value.toString()) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(36.dp)
    ) {
        OutlinedButton(
            onClick = {
                val newValue = value - 1
                if (valueRange == null || newValue in valueRange) onValueChange(newValue)
            },
            modifier = Modifier.size(32.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("-")
        }
        Spacer(modifier = Modifier.width(4.dp))
        OutlinedButton(
            onClick = {
                val newValue = value + 1
                if (valueRange == null || newValue in valueRange) onValueChange(newValue)
            },
            modifier = Modifier.size(32.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("+")
        }
        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier.clickable(enabled = isEditable) {
                isEditing = true
            }
        ) {
            if (isEditing) {
                // Очищаем поле при входе в режим редактирования
                LaunchedEffect(Unit) {
                    tempValue = ""
                    focusRequester.requestFocus()
                }
                BasicTextField(
                    value = tempValue,
                    onValueChange = { tempValue = it.filter { char -> char.isDigit() } },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Start),
                    modifier = Modifier
                        .width(90.dp)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        val newValue = tempValue.toIntOrNull() ?: value
                        onValueChange(newValue)
                        isEditing = false
                        focusManager.clearFocus()
                    })
                )
            } else {
                Text(
                    "$label: $value",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(onColorSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Выберите цвет", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                cardColors.chunked(6).forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        rowColors.forEach { color ->
                            val index = cardColors.indexOf(color)
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { onColorSelected(index) }
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MunchkinPowerCounterPreview() {
    PowerCounterTheme {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            val player = Player(1, "Игрок 1", 5, 10, 0)
            TopAppBar(players = listOf(player), onAddPlayer = {}, onReset = {})
            PlayerCard(
                player = player,
                onLevelChange = {},
                onGearChange = {},
                onNameChange = {},
                onDeletePlayer = {},
                onChangeColor = {}
            )
        }
    }
}
