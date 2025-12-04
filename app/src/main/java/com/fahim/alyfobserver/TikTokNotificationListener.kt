package com.fahim.alyfobserver

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class TikTokNotificationListener : NotificationListenerService() {

    companion object {
        const val TIKTOK_PACKAGE_NAME = "com.zhiliaoapp.musically"
        const val ACTION_TIKTOK_NOTIFICATION = "com.fahim.alyfobserver.ACTION_TIKTOK_NOTIFICATION"
        const val ACTION_GENERAL_NOTIFICATION = "com.fahim.alyfobserver.ACTION_GENERAL_NOTIFICATION"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d("TikTokNotificationListener", "onNotificationPosted: Received notification for package: ${sbn.packageName}")
        val notification = sbn.notification
        val extras = notification.extras
        val packageName = sbn.packageName
        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        // Broadcast general notification details
        Log.d("TikTokNotificationListener", "Sending general notification broadcast.")
        val generalIntent = Intent(ACTION_GENERAL_NOTIFICATION).apply {
            putExtra("package", packageName)
            putExtra("title", title)
            putExtra("text", text)
        }.setPackage(this.packageName)
        sendBroadcast(generalIntent)
        Log.d("TikTokNotificationListener", "General notification broadcast sent for package: $packageName")

        Log.d("NotificationListener", "--- Notification Posted ---")
        Log.d("NotificationListener", "Package: $packageName")
        Log.d("NotificationListener", "Title: $title")
        Log.d("NotificationListener", "Text: $text")

        notification.actions?.forEachIndexed { index, action ->
            Log.d("NotificationListener", "  Action $index: ${action.title}")
            if (action.remoteInputs != null) {
                Log.d("NotificationListener", "    Has RemoteInput: true")
                action.remoteInputs.forEachIndexed { riIndex, remoteInput ->
                    Log.d("NotificationListener", "      RemoteInput $riIndex - resultKey: ${remoteInput.resultKey}, label: ${remoteInput.label}")
                }
            } else {
                Log.d("NotificationListener", "    Has RemoteInput: false")
            }
        }
        Log.d("NotificationListener", "--------------------------")

        // Original TikTok filtering logic remains, but will now be more informed by the general logs
        if (sbn.packageName == TIKTOK_PACKAGE_NAME) {
            Log.d("TikTokNotificationListener", "onNotificationPosted: TikTok package detected. Checking for reply action.")
            Log.d("TikTokNotificationListener", "TikTok notification posted: $title - $text")

            val replyAction = notification.actions?.find { action ->
                val hasReplyRemoteInput = action.remoteInputs?.any { remoteInput ->
                    val isKeyMatch = remoteInput.resultKey == "key_text_reply"
                    Log.d("TikTokNotificationListener", "  Checking RemoteInput for action '${action.title}': resultKey='${remoteInput.resultKey}', match='key_text_reply' == $isKeyMatch")
                    isKeyMatch
                } ?: false
                Log.d("TikTokNotificationListener", "  Action '${action.title}' has reply RemoteInput: $hasReplyRemoteInput")
                hasReplyRemoteInput
            }
            
            Log.d("TikTokNotificationListener", "onNotificationPosted: Reply action found: ${replyAction != null}")

            if (replyAction != null) {
                Log.d("TikTokNotificationListener", "Replyable TikTok notification found")

                val remoteInput = replyAction.remoteInputs?.firstOrNull { it.resultKey == "key_text_reply" }
                if (remoteInput != null) {
                    Log.d("TikTokNotificationListener", "RemoteInput found. Preparing to send broadcast.")
                    val intent = Intent(ACTION_TIKTOK_NOTIFICATION).apply {
                        putExtra("title", title)
                        putExtra("text", text)
                        putExtra("reply_action", replyAction.actionIntent)
                        val remoteInputBundle = Bundle()
                        remoteInputBundle.putString("resultKey", remoteInput.resultKey)
                        remoteInputBundle.putCharSequence("label", remoteInput.label)
                        putExtra("remote_input_bundle", remoteInputBundle)
                    }.setPackage(this.packageName)
                    Log.d("TikTokNotificationListener", "Sending ACTION_TIKTOK_NOTIFICATION broadcast.")
                    sendBroadcast(intent)
                    Log.d("TikTokNotificationListener", "Broadcast sent to FloatingWindowService.")
                } else {
                    Log.e("TikTokNotificationListener", "Error: Reply action found, but the remoteInput with the correct key was null. This should not happen if hasReplyRemoteInput was true.")
                }
            } else {
                Log.d("TikTokNotificationListener", "No replyable TikTok notification found.")
            }
        }
    }
}
