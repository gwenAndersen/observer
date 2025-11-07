package com.fahim.alyfobserver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.fahim.alyfobserver.ui.theme.NewAndroidProjectTheme

class ButtonEditTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewAndroidProjectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DraggableButtonScreen(this)
                }
            }
        }
    }
}

@Composable
fun DraggableButtonScreen(activity: ComponentActivity) {
    // ... content of DraggableButtonScreen
}

@Composable
fun DraggableButton(buttonConfig: DraggableButtonConfig, onDragEnd: () -> Unit) {
    // ... content of DraggableButton
}

@Preview(showBackground = true)
@Composable
fun ButtonEditTestPreview() {
    NewAndroidProjectTheme {
        DraggableButtonScreen(ComponentActivity())
    }
}