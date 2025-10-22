package com.fahim.alyfobserver

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.fahim.alyfobserver.ui.theme.NewAndroidProjectTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("MainActivity", "onCreate: Activity is being created.")

        val serviceIntent = Intent(this, FloatingWindowService::class.java)
        startService(serviceIntent)
        android.util.Log.d("MainActivity", "onCreate: Attempted to start FloatingWindowService.")

        setContent {
            NewAndroidProjectTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting(
                        "This is the main activity.",
                        onOverlayClick = { requestOverlayPermission() },
                        onAccessibilityClick = { requestAccessibilityPermission() },
                        onShowOverlayClick = {
                            val intent = Intent(MyAccessibilityService.ACTION_SHOW_OVERLAY)
                            intent.setPackage(packageName)
                            sendBroadcast(intent)
                        }
                    )
                }
            }
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                // Permission is already granted
            }
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier, onOverlayClick: () -> Unit, onAccessibilityClick: () -> Unit, onShowOverlayClick: () -> Unit) {
    val context = LocalContext.current
    val overlayEnabled = CheckOverlayPermission(context)
    val accessibilityEnabled = CheckAccessibilityPermission(context)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
        Button(
            onClick = onOverlayClick,
            colors = ButtonDefaults.buttonColors(containerColor = if (overlayEnabled) Color.Green else Color.Red)
        ) {
            Text("Overlay Permission")
        }
        Button(
            onClick = onAccessibilityClick,
            colors = ButtonDefaults.buttonColors(containerColor = if (accessibilityEnabled) Color.Green else Color.Red)
        ) {
            Text("Accessibility Permission")
        }
        Button(
            onClick = onShowOverlayClick,
        ) {
            Text("Show Overlay")
        }
    }
}

@Composable
fun CheckOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

@Composable
fun CheckAccessibilityPermission(context: Context): Boolean {
    val accessibilityService = ComponentName(context, MyAccessibilityService::class.java)
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return enabledServices?.contains(accessibilityService.flattenToString()) == true
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NewAndroidProjectTheme {
        Greeting(
            "This is the main activity.",
            onOverlayClick = { },
            onAccessibilityClick = { },
            onShowOverlayClick = { }
        )
    }
}
