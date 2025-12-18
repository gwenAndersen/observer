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
import androidx.compose.foundation.layout.Spacer // Added import
import androidx.compose.foundation.layout.height // Added import
import androidx.compose.ui.unit.dp // Added import


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
                        },
                        onEditButtonsClick = {
                            val intent = Intent(this, ButtonEditActivity::class.java)
                            startActivity(intent)
                        },
                        onNotificationListenerClick = { requestNotificationListenerPermission() },
                        onBatteryOptimizationClick = { requestIgnoreBatteryOptimizations() }
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

    private fun requestNotificationListenerPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
    onOverlayClick: () -> Unit,
    onAccessibilityClick: () -> Unit,
    onShowOverlayClick: () -> Unit,
    onEditButtonsClick: () -> Unit,
    onNotificationListenerClick: () -> Unit,
    onBatteryOptimizationClick: () -> Unit
) {
    val context = LocalContext.current
    val overlayEnabled = CheckOverlayPermission(context)
    val accessibilityEnabled = CheckAccessibilityPermission(context)
    val notificationListenerEnabled = CheckNotificationListenerPermission(context)
    val batteryOptimizationDisabled = CheckBatteryOptimization(context)

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
            onClick = onNotificationListenerClick,
            colors = ButtonDefaults.buttonColors(containerColor = if (notificationListenerEnabled) Color.Green else Color.Red)
        ) {
            Text("Notification Listener Permission")
        }
        Button(
            onClick = onBatteryOptimizationClick,
            colors = ButtonDefaults.buttonColors(containerColor = if (batteryOptimizationDisabled) Color.Green else Color.Red)
        ) {
            Text("Battery Optimization")
        }
        Button(
            onClick = onShowOverlayClick,
        ) {
            Text("Show Overlay")
        }
        Spacer(modifier = Modifier.height(16.dp)) // Add a spacer
        Button(
            onClick = onEditButtonsClick,
        ) {
            Text("Edit Buttons")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val intent = Intent(context, com.fahim.alyfobserver.ai.AiChatActivity::class.java)
                context.startActivity(intent)
            }
        ) {
            Text("Open AI Chat")
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

@Composable
fun CheckNotificationListenerPermission(context: Context): Boolean {
    val componentName = ComponentName(context, TikTokNotificationListener::class.java)
    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return enabledListeners?.contains(componentName.flattenToString()) == true
}

@Composable
fun CheckBatteryOptimization(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    return true
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NewAndroidProjectTheme {
        Greeting(
            "This is the main activity.",
            onOverlayClick = { },
            onAccessibilityClick = { },
            onShowOverlayClick = { },
            onEditButtonsClick = { },
            onNotificationListenerClick = { },
            onBatteryOptimizationClick = { }
        )
    }
}
