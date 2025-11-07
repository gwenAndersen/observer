package com.fahim.alyfobserver

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.fahim.alyfobserver.ui.theme.NewAndroidProjectTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
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
}

@Composable
fun ButtonEditor() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val buttons = remember { mutableStateListOf<DraggableButtonConfig>() }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            buttons.addAll(DataStoreManager.loadDraggableButtons(context))
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = {
            val newButton = DraggableButtonConfig(
                id = "button_${buttons.size + 1}",
                text = "New Button Text",
                emoji = "🚀",
                x = 100f,
                y = 100f
            )
            buttons.add(newButton)
        }) {
            Text("Add Button")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            coroutineScope.launch {
                DataStoreManager.saveDraggableButtons(context, buttons)
            }
        }) {
            Text("Save Layout")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(buttons.size) { index ->
                val button = buttons[index]
                var text by remember { mutableStateOf(button.text) }
                var emoji by remember { mutableStateOf(button.emoji) }

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            buttons[index] = button.copy(text = it)
                        },
                        label = { Text("Button Text") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = {
                            emoji = it
                            buttons[index] = button.copy(emoji = it)
                        },
                        label = { Text("Emoji") },
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
        }
    }
}