package com.fahim.alyfobserver

import android.content.Context
import android.content.Intent // Added import
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
import androidx.compose.foundation.lazy.itemsIndexed // Added import
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons // Added import
import androidx.compose.material.icons.filled.Delete // Added import
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment // Added import
import com.fahim.alyfobserver.ButtonConfig

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
    val buttons = remember { mutableStateListOf<ButtonConfig>() }

    Text("Simplified Button Editor")
}