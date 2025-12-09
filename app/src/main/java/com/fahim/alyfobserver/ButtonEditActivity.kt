package com.fahim.alyfobserver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fahim.alyfobserver.ui.theme.NewAndroidProjectTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ButtonEditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewAndroidProjectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ButtonEditor()
                }
            }
        }
    }

    companion object {
        const val ACTION_BUTTON_LAYOUT_UPDATED = "com.fahim.alyfobserver.ACTION_BUTTON_LAYOUT_UPDATED"
    }
}

@Composable
fun ButtonEditor() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedLayout by remember { mutableStateOf(LayoutType.CLIPBOARD) }

    val clipboardButtons = remember { mutableStateListOf<ButtonConfig>() }
    val heartButtons = remember { mutableStateListOf<ButtonConfig>() }

    LaunchedEffect(Unit) {
        clipboardButtons.addAll(DataStoreManager.loadButtonLayout(context))
        heartButtons.addAll(DataStoreManager.loadHeartButtonLayout(context))
    }

    val currentButtons = if (selectedLayout == LayoutType.CLIPBOARD) clipboardButtons else heartButtons

    Column(modifier = Modifier.padding(16.dp)) {
        // Layout selector
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Select Layout:")
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { selectedLayout = LayoutType.CLIPBOARD }) {
                Text("Clipboard")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { selectedLayout = LayoutType.HEART }) {
                Text("Heart")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(currentButtons) { index, button ->
                ButtonConfigItem(
                    buttonConfig = button,
                    onUpdate = { updatedButton ->
                        currentButtons[index] = updatedButton
                    },
                    onDelete = {
                        currentButtons.removeAt(index)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = {
                val newId = "new_button_${System.currentTimeMillis()}"
                currentButtons.add(ButtonConfig(id = newId, text = "New Button", emoji = "🆕"))
            }) {
                Text("Add New")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                coroutineScope.launch {
                    if (selectedLayout == LayoutType.CLIPBOARD) {
                        DataStoreManager.saveButtonLayout(context, clipboardButtons.toList())
                    } else {
                        DataStoreManager.saveHeartButtonLayout(context, heartButtons.toList())
                    }
                    // Notify the service that the layout has been updated
                    val intent = Intent(ButtonEditActivity.ACTION_BUTTON_LAYOUT_UPDATED)
                    context.sendBroadcast(intent)
                }
            }) {
                Text("Save")
            }
        }
    }
}

@Composable
fun ButtonConfigItem(
    buttonConfig: ButtonConfig,
    onUpdate: (ButtonConfig) -> Unit,
    onDelete: () -> Unit
) {
    var id by remember(buttonConfig) { mutableStateOf(buttonConfig.id) }
    var text by remember(buttonConfig) { mutableStateOf(buttonConfig.text) }
    var emoji by remember(buttonConfig) { mutableStateOf(buttonConfig.emoji ?: "") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = id,
                onValueChange = {
                    id = it
                    onUpdate(buttonConfig.copy(id = it))
                },
                label = { Text("ID") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = emoji,
                onValueChange = {
                    emoji = it
                    onUpdate(buttonConfig.copy(emoji = it))
                },
                label = { Text("Emoji") },
                modifier = Modifier.width(80.dp)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onUpdate(buttonConfig.copy(text = it))
            },
            label = { Text("Text") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5
        )
    }
}

enum class LayoutType {
    CLIPBOARD,
    HEART
}