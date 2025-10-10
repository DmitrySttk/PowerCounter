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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.powercounter.ui.theme.PowerCounterTheme
import kotlin.random.Random

// 1. Новая, расширенная палитра приглушенных цветов (18 вариантов)
val cardColors = listOf(
    // Основные
    Color(0x00000000), // Черный (по умолчанию)
    Color(0xFF5f798d), // Пыльный синий
    Color(0xFF667d60), // Болотный зеленый
    Color(0xFF8d6b62), // Теплый терракотовый
    Color(0xFF8d6e89), // Пыльный лиловый
    Color(0xFF9d875c), // Приглушенный охристый
    Color(0xFF5a6e8a), // Глубокий серо-синий
    Color(0xFFa1a1a1), // Светло-серый
    Color(0xFF7a6c5d), // Кофейный
    Color(0xFF4a5e5a), // Темно-бирюзовый
    Color(0xFF9a7e6e), // Розово-коричневый
    Color(0xFF635f6d), // Шиферный
    Color(0xFFb0a38f), // Бежевый
    Color(0xFF7e8a97), // Прохладный стальной
    Color(0xFF9c8c82), // Пепельно-розовый
    Color(0xFF566573), // Графитовый
    Color(0xFFa49e97), // Каменный
    Color(0xFF8c9288)  // Шалфейный
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PowerCounterTheme {
                MunchkinPowerCounter()
            }
        }
    }
}

data class Player(
    val id: Int,
    val name: String,
    val level: Int = 1,
    val gear: Int = 0,
    val cardColorIndex: Int = 0 // Индекс цвета в списке cardColors
) {
    val totalPower: Int
        get() = level + gear
    val color: Color
        get() = cardColors[cardColorIndex]
}

@Composable
fun MunchkinPowerCounter(modifier: Modifier = Modifier) {
    var players by remember { mutableStateOf(listOf(Player(id = 1, name = "Игрок 1"))) }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TopAppBar(
                players = players,
                onAddPlayer = {
                    if (players.size < 6) {
                        val newId = (players.maxOfOrNull { it.id } ?: 0) + 1
                        // 2. Устанавливаем серый цвет по умолчанию (индекс 0)
                        players = players + Player(id = newId, name = "Игрок ${players.size + 1}", cardColorIndex = 0)
                    }
                },
                onReset = {
                    players = listOf(Player(id = 1, name = "Игрок 1"))
                }
            )
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp) // Уменьшаем расстояние между карточками
            ) {
                items(players, key = { it.id }) { player ->
                    PlayerCard(
                        player = player,
                        onLevelChange = { newLevel ->
                            players = players.map {
                                if (it.id == player.id) it.copy(level = newLevel) else it
                            }
                        },
                        onGearChange = { newGear ->
                            players = players.map {
                                if (it.id == player.id) it.copy(gear = newGear) else it
                            }
                        },
                        onNameChange = { newName ->
                            players = players.map {
                                if (it.id == player.id) it.copy(name = newName) else it
                            }
                        },
                        onDeletePlayer = {
                            if (players.size > 1) {
                                players = players.filter { it.id != player.id }
                            }
                        },
                        onChangeColor = { newColorIndex ->
                            players = players.map {
                                if (it.id == player.id) it.copy(cardColorIndex = newColorIndex) else it
                            }
                        }
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
            .padding(horizontal = 16.dp, vertical = 4.dp), // Уменьшил вертикальный отступ
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
        colors = CardDefaults.cardColors(containerColor = player.color)
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
                            tempName = "" // Очищаем поле при входе в режим редактирования
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
                    Counter(label = "Уровень", value = player.level, onValueChange = { onLevelChange(it) }, valueRange = 1..10)
                    Counter(label = "Шмотки", value = player.gear, onValueChange = { onGearChange(it) })
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
    valueRange: IntRange? = null
) {
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
        Text(
            "$label: $value",
            style = MaterialTheme.typography.bodyLarge,
            // modifier = Modifier.width(90.dp), // <<<--- ЭТА СТРОКА УДАЛЕНА
            textAlign = TextAlign.Start
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(onColorSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Выберите цвет", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

                // Разбиваем список цветов на чанки (ряды) по 6 элементов
                cardColors.chunked(6).forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        rowColors.forEach { color ->
                            val index = cardColors.indexOf(color)
                            Box(
                                modifier = Modifier
                                    .padding(4.dp) // Добавляем отступ вокруг каждого кружка
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
        var players by remember {
            mutableStateOf(
                (1..6).map { Player(id = it, name = "Игрок $it", level = it, gear = it * 2, cardColorIndex = it - 1) }
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            TopAppBar(players = players, onAddPlayer = {}, onReset = {})
            players.forEach { player ->
                PlayerCard(player = player, onLevelChange = {}, onGearChange = {}, onNameChange = {}, onDeletePlayer = {}, onChangeColor = {})
            }
        }
    }
}
