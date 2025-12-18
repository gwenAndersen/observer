package com.fahim.alyfobserver.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahim.alyfobserver.ui.theme.NewAndroidProjectTheme

class AiChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewAndroidProjectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AiChatScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(viewModel: AiChatViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var textInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AI Chat") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    MessageBubble(message)
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            }
                        }
                    )
                )
                Spacer(modifier = Modifier.widthIn(8.dp))
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(textInput)
                            textInput = ""
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
