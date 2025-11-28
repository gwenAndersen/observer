package com.fahim.alyfobserver

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MyAccessibilityService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            serviceScope.launch(Dispatchers.IO) {
                when (intent?.action) {
                    ACTION_PASTE_TEXT -> {
                        val textToPaste = intent.getStringExtra("textToPaste")
                        if (textToPaste != null) {
                            performPaste(textToPaste)
                        }
                    }
                    ACTION_DUMP_UI_TREE -> {
                        dumpUiTree()
                    }
                    // Add other actions here later
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_PASTE_TEXT)
            addAction(ACTION_DUMP_UI_TREE)
            // Add other actions here later
        }
        registerReceiver(broadcastReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        Log.d("MyAccessibilityService", "Accessibility service connected and broadcast receiver registered.")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        serviceJob.cancel()
        Log.d("MyAccessibilityService", "Accessibility service disconnected and broadcast receiver unregistered.")
    }

    private fun performPaste(text: String) {
        val focusedNode = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null && focusedNode.isEditable) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d("MyAccessibilityService", "Pasted text: $text")
        } else {
            Log.d("MyAccessibilityService", "No editable focused node found for pasting.")
        }
    }

    private fun dumpUiTree() {
        val rootNode = rootInActiveWindow ?: return
        Log.d("MyAccessibilityService", "--- UI Tree Dump Start ---")
        dumpNode(rootNode, 0)
        Log.d("MyAccessibilityService", "--- UI Tree Dump End ---")
    }

    private fun dumpNode(node: AccessibilityNodeInfo, depth: Int) {
        val indent = "  ".repeat(depth)
        val nodeInfo = StringBuilder()
        nodeInfo.append(indent).append("Class: ").append(node.className)
        if (node.viewIdResourceName != null) {
            nodeInfo.append(", ID: ").append(node.viewIdResourceName)
        }
        if (node.text != null) {
            nodeInfo.append(", Text: \"").append(node.text).append("\"")
        }
        if (node.contentDescription != null) {
            nodeInfo.append(", Content-Desc: \"").append(node.contentDescription).append("\"")
        }
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        nodeInfo.append(", Bounds: ").append(bounds.toShortString())
        Log.d("MyAccessibilityService", nodeInfo.toString())

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                dumpNode(it, depth + 1)
            }
        }
    }

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.fahim.alyfobserver.ACTION_SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.fahim.alyfobserver.ACTION_HIDE_OVERLAY"
        const val ACTION_FIND_AND_WRITE = "com.fahim.alyfobserver.ACTION_FIND_AND_WRITE"
        const val ACTION_DUMP_UI_TREE = "com.fahim.alyfobserver.ACTION_DUMP_UI_TREE"
        const val ACTION_PASTE_TEXT = "com.fahim.alyfobserver.ACTION_PASTE_TEXT"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used
    }

    override fun onInterrupt() {
        // Not used
    }
}