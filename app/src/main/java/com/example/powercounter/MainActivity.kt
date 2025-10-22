package com.example.powercounter

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.powercounter.data.Player
import com.example.powercounter.data.PlayerRepository
import com.example.powercounter.data.cardColors
import com.example.powercounter.ui.MunchkinViewModel
import com.example.powercounter.ui.MunchkinViewModelFactory
import com.example.powercounter.ui.theme.PowerCounterTheme


/**
 * Activity теперь отвечает только за запуск и предоставление зависимостей (Repository).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Создаем репозиторий один раз
            val repository = remember { PlayerRepository(applicationContext) }
            // Создаем ViewModel через фабрику
            val viewModel: MunchkinViewModel = viewModel(factory = MunchkinViewModelFactory(repository))

            PowerCounterTheme {
                MunchkinPowerCounter(viewModel)
            }
        }
    }
}

// --- UI (VIEW) ---

@Composable
fun MunchkinPowerCounter(viewModel: MunchkinViewModel) {
    // View подписывается на состояние из ViewModel
    val players by viewModel.playersState.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TopAppBar(
                players = players,
                // View просто сообщает ViewModel о действии пользователя
                onAddPlayer = { viewModel.addPlayer() },
                onReset = { viewModel.resetPlayers() }
            )
            // Corrected Code
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(space = 1.dp, alignment = Alignment.Top)
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
                        isEditable = false
                    )
                    Counter(
                        label = "Шмотки",
                        value = player.gear,
                        onValueChange = { onGearChange(it) },
                        isEditable = true
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
