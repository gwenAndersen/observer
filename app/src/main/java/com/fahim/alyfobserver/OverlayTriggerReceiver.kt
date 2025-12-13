package com.fahim.alyfobserver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class OverlayTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("OverlayTriggerReceiver", "Broadcast received with action: ${intent.action}")
        if (intent.action == "com.fahim.alyfobserver.ACTION_TRIGGER_OVERLAY") {
            Log.d("OverlayTriggerReceiver", "Starting FloatingWindowService to show overlay.")
            val serviceIntent = Intent(context, FloatingWindowService::class.java).apply {
                action = "com.fahim.alyfobserver.ACTION_TRIGGER_OVERLAY"
            }
            context.startService(serviceIntent)
        }
    }
}
