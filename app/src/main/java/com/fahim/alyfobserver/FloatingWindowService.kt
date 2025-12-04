package com.fahim.alyfobserver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.LooksTwo
import androidx.compose.material.icons.filled.Looks3
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.fahim.alyfobserver.ui.theme.NewAndroidProjectTheme
import com.google.accompanist.web.rememberWebViewState
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

import kotlin.math.roundToInt
import com.google.accompanist.web.WebView
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import android.app.PendingIntent
import android.app.RemoteInput

data class TikTokNotification(
    val title: String?,
    val text: String?,
    val replyAction: PendingIntent?,
    val remoteInputBundle: Bundle?
)


enum class OverlayLayoutState { MAIN, TEXT_LAYOUT, DATA_LAYOUT, WEB_VIEW_LAYOUT, HEART_LAYOUT, TIKTOK_LAYOUT, NOTIFICATION_LIST_LAYOUT }

class ServiceLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle get() = registry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        registry.handleLifecycleEvent(event)
    }
}

class FloatingWindowService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {

    private var webViewInstance: WebView? = null
    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams
    private var isExpanded = mutableStateOf(false)
    private var offsetX by mutableStateOf(0f)
    private var offsetY by mutableStateOf(0f)
    private var showTextLayout = mutableStateOf(false)
    private var showDataLayout = mutableStateOf(false)
    private var showWebViewLayout = mutableStateOf(false)
    private var showHeartLayout = mutableStateOf(false)
    private var showTikTokLayout = mutableStateOf(false)
    private var showNotificationListLayout = mutableStateOf(false)
    private val notificationList = mutableStateListOf<String>()
    private var currentTikTokNotification = mutableStateOf<TikTokNotification?>(null)
    private var currentLayoutState = mutableStateOf(OverlayLayoutState.MAIN)
    private var isOverlayInputFocused = mutableStateOf(false)
    private var isMinimized = mutableStateOf(false)
    private var isFullScreen by mutableStateOf(false) // Added back
    
    private var screenHeight = 0 // Added for keyboard detection
    private var lastYPosition = 100 // Added to store last manual Y position
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable: Runnable = Runnable {
        if (isExpanded.value) {
            isExpanded.value = false
            idleHandler.postDelayed(idleRunnable, 500) // Give it time to collapse
        } else {
            isMinimized.value = true
        }
    }

    private lateinit var _viewModelStore: ViewModelStore
    private lateinit var savedStateRegistryController: SavedStateRegistryController
    private lateinit var onBackPressedDispatcher: OnBackPressedDispatcher
    private var clipboardButtonLayout by mutableStateOf<List<ButtonConfig>>(emptyList())
    private var heartButtonLayout by mutableStateOf<List<ButtonConfig>>(emptyList())
    private lateinit var lifecycleOwner: ServiceLifecycleOwner

    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val layoutUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.fahim.alyfobserver.LAYOUT_UPDATED") {
                Log.d("FloatingWindowService", "Received layout update broadcast")
                lifecycleScope.launch {
                    clipboardButtonLayout = DataStoreManager.loadButtonLayout(this@FloatingWindowService)
                    heartButtonLayout = DataStoreManager.loadHeartButtonLayout(this@FloatingWindowService)
                }
            }
        }
    }

    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val rect = Rect()
        overlayView?.getWindowVisibleDisplayFrame(rect)

        val fullscreenNow = rect.top == 0
        if (fullscreenNow != isFullScreen) {
            isFullScreen = fullscreenNow
            if (isFullScreen) {
                hideOverlay()
            } else {
                showOverlay()
            }
        }

        val keyboardHeight = if (rect.bottom > screenHeight) 0 else screenHeight - rect.bottom
        if (keyboardHeight > screenHeight * 0.15) { // If keyboard is likely open (more than 15% of screen height)
            params.y = rect.bottom - (overlayView?.height ?: 0) - 50 // Adjust Y to be above keyboard
        } else {
            // Keyboard is closed, restore last known manual Y position
            params.y = lastYPosition
        }
        windowManager.updateViewLayout(overlayView, params)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("FloatingWindowService", "Received action: ${intent?.action}")
            when (intent?.action) {
                MyAccessibilityService.ACTION_SHOW_OVERLAY -> showOverlay()
                MyAccessibilityService.ACTION_HIDE_OVERLAY -> hideOverlay()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("FloatingWindowService", "onCreate: Service is being created.")

        lifecycleScope.launch {
            clipboardButtonLayout = DataStoreManager.loadButtonLayout(this@FloatingWindowService)
            heartButtonLayout = DataStoreManager.loadHeartButtonLayout(this@FloatingWindowService)
        }

        _viewModelStore = ViewModelStore()
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performRestore(null)
        onBackPressedDispatcher = OnBackPressedDispatcher { /* Handle back press if needed */ }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100 // Start a bit down from the top
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(MyAccessibilityService.ACTION_SHOW_OVERLAY)
        intentFilter.addAction(MyAccessibilityService.ACTION_HIDE_OVERLAY)
        Log.d("FloatingWindowService", "onCreate: Registering broadcast receiver.")
        registerReceiver(broadcastReceiver, intentFilter, RECEIVER_NOT_EXPORTED)

        val layoutUpdateFilter = IntentFilter("com.fahim.alyfobserver.LAYOUT_UPDATED")
        registerReceiver(layoutUpdateReceiver, layoutUpdateFilter, RECEIVER_NOT_EXPORTED)

        val tikTokFilter = IntentFilter(TikTokNotificationListener.ACTION_TIKTOK_NOTIFICATION)
        registerReceiver(tikTokNotificationReceiver, tikTokFilter, RECEIVER_NOT_EXPORTED)
        Log.d("FloatingWindowService", "tikTokNotificationReceiver registered for action: ${TikTokNotificationListener.ACTION_TIKTOK_NOTIFICATION}")

        val generalNotificationFilter = IntentFilter(TikTokNotificationListener.ACTION_GENERAL_NOTIFICATION)
        registerReceiver(generalNotificationReceiver, generalNotificationFilter, RECEIVER_NOT_EXPORTED)
        Log.d("FloatingWindowService", "generalNotificationReceiver registered for action: ${TikTokNotificationListener.ACTION_GENERAL_NOTIFICATION}")
    }

    private val generalNotificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("FloatingWindowService", "generalNotificationReceiver: Received broadcast for action: ${intent?.action}")
            if (intent?.action == TikTokNotificationListener.ACTION_GENERAL_NOTIFICATION) {
                val packageName = intent.getStringExtra("package")
                val title = intent.getStringExtra("title")
                val text = intent.getStringExtra("text")

                if (notificationList.size > 50) {
                    notificationList.clear()
                }

                notificationList.add("[$packageName] $title: $text")
            }
        }
    }

    private val tikTokNotificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("FloatingWindowService", "tikTokNotificationReceiver: Received broadcast for action: ${intent?.action}. Processing TikTok notification.")
            if (intent?.action == TikTokNotificationListener.ACTION_TIKTOK_NOTIFICATION) {
                val title = intent.getStringExtra("title")
                val text = intent.getStringExtra("text")
                val replyAction = intent.getParcelableExtra<PendingIntent>("reply_action")
                val remoteInputBundle = intent.getBundleExtra("remote_input_bundle")

                currentTikTokNotification.value = TikTokNotification(
                    title = title,
                    text = text,
                    replyAction = replyAction,
                    remoteInputBundle = remoteInputBundle
                )
                showTikTokLayout.value = true
                currentLayoutState.value = OverlayLayoutState.TIKTOK_LAYOUT
                Log.d("FloatingWindowService", "tikTokNotificationReceiver: Calling showOverlay() to display TikTok layout.")
                showOverlay()
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("FloatingWindowService", "onStartCommand: Service is started.\n")
        return START_STICKY
    }

    private fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            Log.e("FloatingWindowService", "Could not find app for package: $packageName")
            // Optionally, show a toast to the user
        }
    }

    private fun resetIdleTimer() {
        isMinimized.value = false
        idleHandler.removeCallbacks(idleRunnable)
        idleHandler.postDelayed(idleRunnable, 20000) // 20 seconds
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("FloatingWindowService", "onDestroy: Service is being destroyed.")
        unregisterReceiver(broadcastReceiver)
        unregisterReceiver(layoutUpdateReceiver)
        unregisterReceiver(tikTokNotificationReceiver)
        unregisterReceiver(generalNotificationReceiver)
        hideOverlay()
        _viewModelStore.clear()
    }

    private fun updateOverlayFlags() {
        if (isOverlayInputFocused.value) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        windowManager.updateViewLayout(overlayView, params)
    }

    private fun showOverlay() {
        Log.d("FloatingWindowService", "showOverlay: CALLED")
        if (overlayView == null) {
            Log.d("FloatingWindowService", "showOverlay: overlayView is null, creating new view.")
            overlayView = ComposeView(this).apply {
                Log.d("FloatingWindowService", "showOverlay: ComposeView created.")
                // Set up LifecycleOwner, ViewModelStoreOwner, and SavedStateRegistryOwner
                lifecycleOwner = ServiceLifecycleOwner()
                
                lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
                lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                setViewTreeLifecycleOwner(lifecycleOwner)
                                setViewTreeViewModelStoreOwner(this@FloatingWindowService)
                setViewTreeSavedStateRegistryOwner(this@FloatingWindowService)
                Log.d("FloatingWindowService", "showOverlay: Lifecycle owners set.")

                setContent {
                    Log.d("FloatingWindowService", "showOverlay: setContent CALLED")
                    CompositionLocalProvider(
                        LocalOnBackPressedDispatcherOwner provides ServiceBackPressedDispatcherOwner(
                            onBackPressedDispatcher,
                            lifecycleOwner.lifecycle
                        )
                    ) {
                        NewAndroidProjectTheme {
                            MaterialTheme {
                                OverlayList(
                                    context = this@FloatingWindowService,
                                    isExpanded = isExpanded,
                                    showTextLayout = showTextLayout,
                                    showDataLayout = showDataLayout,
                                    showWebViewLayout = showWebViewLayout,
                                    showHeartLayout = showHeartLayout,
                                    showTikTokLayout = showTikTokLayout,
                                    showNotificationListLayout = showNotificationListLayout,
                                    clipboardButtonLayout = clipboardButtonLayout,
                                    heartButtonLayout = heartButtonLayout,
                                    notificationList = notificationList,
                                    onClose = { hideOverlay() },
                                    onToggleExpand = {
                                        isExpanded.value = !isExpanded.value
                                        Log.d("FloatingWindowService", "isExpanded toggled to: ${isExpanded.value}")
                                        resetIdleTimer()
                                    },
                                    onToggleTextLayout = { showTextLayout.value = !showTextLayout.value; currentLayoutState.value = if (showTextLayout.value) OverlayLayoutState.TEXT_LAYOUT else OverlayLayoutState.MAIN },
                                    onToggleDataLayout = { showDataLayout.value = !showDataLayout.value; currentLayoutState.value = if (showDataLayout.value) OverlayLayoutState.DATA_LAYOUT else OverlayLayoutState.MAIN },
                                    onToggleWebViewLayout = { showWebViewLayout.value = !showWebViewLayout.value; currentLayoutState.value = if (showWebViewLayout.value) OverlayLayoutState.WEB_VIEW_LAYOUT else OverlayLayoutState.MAIN },
                                    onToggleHeartLayout = { showHeartLayout.value = !showHeartLayout.value; currentLayoutState.value = if (showHeartLayout.value) OverlayLayoutState.HEART_LAYOUT else OverlayLayoutState.MAIN },
                                    onToggleNotificationListLayout = { showNotificationListLayout.value = !showNotificationListLayout.value; currentLayoutState.value = if (showNotificationListLayout.value) OverlayLayoutState.NOTIFICATION_LIST_LAYOUT else OverlayLayoutState.MAIN },
                                    onPasteText = { text -> pasteText(text) },
                                    onLaunchTermux = { launchApp("com.termux") },
                                    currentLayoutState = currentLayoutState,
                                    onRestoreLayout = { state ->
                                        isMinimized.value = false
                                        isExpanded.value = true
                                        when (state) {
                                            OverlayLayoutState.MAIN -> { showTextLayout.value = false; showDataLayout.value = false; showWebViewLayout.value = false; showHeartLayout.value = false; showTikTokLayout.value = false; showNotificationListLayout.value = false }
                                            OverlayLayoutState.TEXT_LAYOUT -> { showTextLayout.value = true; showDataLayout.value = false; showWebViewLayout.value = false; showHeartLayout.value = false; showTikTokLayout.value = false; showNotificationListLayout.value = false }
                                            OverlayLayoutState.DATA_LAYOUT -> { showTextLayout.value = false; showDataLayout.value = true; showWebViewLayout.value = false; showHeartLayout.value = false; showTikTokLayout.value = false; showNotificationListLayout.value = false }
                                            OverlayLayoutState.WEB_VIEW_LAYOUT -> { showTextLayout.value = false; showDataLayout.value = false; showWebViewLayout.value = true; showHeartLayout.value = false; showTikTokLayout.value = false; showNotificationListLayout.value = false }
                                            OverlayLayoutState.HEART_LAYOUT -> { showTextLayout.value = false; showDataLayout.value = false; showWebViewLayout.value = false; showHeartLayout.value = true; showTikTokLayout.value = false; showNotificationListLayout.value = false }
                                            OverlayLayoutState.TIKTOK_LAYOUT -> { showTextLayout.value = false; showDataLayout.value = false; showWebViewLayout.value = false; showHeartLayout.value = false; showTikTokLayout.value = true; showNotificationListLayout.value = false }
                                            OverlayLayoutState.NOTIFICATION_LIST_LAYOUT -> { showTextLayout.value = false; showDataLayout.value = false; showWebViewLayout.value = false; showHeartLayout.value = false; showTikTokLayout.value = false; showNotificationListLayout.value = true }
                                        }
                                        currentLayoutState.value = state
                                        resetIdleTimer()
                                    },
                                    onSaveDataFromWebView = {
                                        // No-op
                                    },
                                    onLoadDataForWebView = {
                                        ""
                                     },
                                    onWebViewCreated = { webView -> webViewInstance = webView },
                                    onInputFocusChanged = { isFocused ->
                                        isOverlayInputFocused.value = isFocused
                                        updateOverlayFlags()
                                    },
                                    isMinimized = isMinimized.value,
                                    onIsMinimizedChange = { isMinimized.value = it },
                                    currentTikTokNotification = currentTikTokNotification,
                                    onReply = { replyText ->
                                        currentTikTokNotification.value?.let { notification ->
                                            val remoteInputBundle = notification.remoteInputBundle
                                            val replyAction = notification.replyAction
                                            if (remoteInputBundle != null && replyAction != null) {
                                                val resultKey = remoteInputBundle.getString("resultKey")
                                                if (resultKey != null) {
                                                    val remoteInput = RemoteInput.Builder(resultKey).setLabel(remoteInputBundle.getCharSequence("label")).build()
                                                    val resultBundle = Bundle()
                                                    resultBundle.putCharSequence(remoteInput.resultKey, replyText)
                                                    val replyIntent = Intent()
                                                    RemoteInput.addResultsToIntent(arrayOf(remoteInput), replyIntent, resultBundle)
                                                    try {
                                                        replyAction.send(this@FloatingWindowService, 0, replyIntent)
                                                        showTikTokLayout.value = false
                                                        currentLayoutState.value = OverlayLayoutState.MAIN
                                                    } catch (e: PendingIntent.CanceledException) {
                                                        Log.e("FloatingWindowService", "Could not send reply", e)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    offsetX += dragAmount.x
                                                    offsetY += dragAmount.y
                                                },
                                                onDragEnd = {
                                                    params.x += offsetX.roundToInt()
                                                    params.y += offsetY.roundToInt()
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                    lastYPosition = params.y
                                                    windowManager.updateViewLayout(this@apply, params)
                                                }
                                            )
                                        }
                                )
                            }
                        }
                    }
                    Log.d("FloatingWindowService", "showOverlay: setContent COMPLETED")
                }
            }
            try {
                Log.d("FloatingWindowService", "showOverlay: Attempting to add overlayView to WindowManager.")
                windowManager.addView(overlayView, params)
                Log.d("FloatingWindowService", "showOverlay: addView SUCCEEDED")
                overlayView?.viewTreeObserver?.addOnGlobalLayoutListener(globalLayoutListener)
                screenHeight = resources.displayMetrics.heightPixels
                resetIdleTimer()
            } catch (e: Exception) {
                Log.e("FloatingWindowService", "showOverlay: addView FAILED", e)
            }
        } else {
            Log.d("FloatingWindowService", "showOverlay: overlayView is NOT null, just updating layout.")
            // This is where you might need to force a recomposition if the view already exists
        }
    }

    private fun hideOverlay() {
        Log.d("FloatingWindowService", "hideOverlay: CALLED")
        idleHandler.removeCallbacks(idleRunnable)
        try {
            overlayView?.let {
                windowManager.removeView(it)
                it.viewTreeObserver?.removeOnGlobalLayoutListener(globalLayoutListener)
                Log.d("FloatingWindowService", "hideOverlay: Overlay view removed successfully.")
            }
            overlayView = null
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            Log.d("FloatingWindowService", "hideOverlay: Lifecycle events sent.")
        } catch (e: Exception) {
            Log.e("FloatingWindowService", "hideOverlay: FAILED", e)
        }
    }

    private fun pasteText(text: String) {
        val intent = Intent(MyAccessibilityService.ACTION_PASTE_TEXT).apply {
            putExtra("textToPaste", text)
            setPackage(packageName) // Make the intent explicit
        }
        sendBroadcast(intent)
        Log.d("FloatingWindowService", "Sent broadcast to paste text: $text")
    }

}

@Composable
fun OverlayList(
    context: Context,
    isExpanded: MutableState<Boolean>,
    showTextLayout: MutableState<Boolean>,
    showDataLayout: MutableState<Boolean>,
    showWebViewLayout: MutableState<Boolean>,
    showHeartLayout: MutableState<Boolean>,
    showTikTokLayout: MutableState<Boolean>,
    showNotificationListLayout: MutableState<Boolean>,
    clipboardButtonLayout: List<ButtonConfig>,
    heartButtonLayout: List<ButtonConfig>,
    notificationList: List<String>,
    onClose: () -> Unit,
    onToggleExpand: () -> Unit,
    onToggleTextLayout: () -> Unit,
    onToggleDataLayout: () -> Unit,
    onToggleWebViewLayout: () -> Unit,
    onToggleHeartLayout: () -> Unit,
    onToggleNotificationListLayout: () -> Unit,
    onPasteText: (String) -> Unit,
    onLaunchTermux: () -> Unit,
    currentLayoutState: MutableState<OverlayLayoutState>,
    onRestoreLayout: (OverlayLayoutState) -> Unit,
    onSaveDataFromWebView: (String) -> Unit,
    onLoadDataForWebView: () -> String,
    onWebViewCreated: (WebView) -> Unit,
    onInputFocusChanged: (Boolean) -> Unit,
    isMinimized: Boolean,
    onIsMinimizedChange: (Boolean) -> Unit,
    currentTikTokNotification: MutableState<TikTokNotification?>,
    onReply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(if (isMinimized) 0.5f else 1f)
    val size by animateDpAsState(if (isMinimized) 40.dp else 56.dp)

    val rootModifier = modifier
        .padding(16.dp)
        .alpha(alpha)
        .clickable(enabled = isMinimized) { // Handle click when idle
            onRestoreLayout(currentLayoutState.value)
        }

    if (showTextLayout.value) {
        TextLayout(
            onPasteText = onPasteText,
            onToggleTextLayout = onToggleTextLayout,
            onInputFocusChanged = onInputFocusChanged,
            alpha = alpha,
            size = size,
            onRestoreLayout = onRestoreLayout,
            currentLayoutState = currentLayoutState.value,
            buttonLayout = clipboardButtonLayout,
            onSwapLayout = {
                showTextLayout.value = false
                showHeartLayout.value = true
                currentLayoutState.value = OverlayLayoutState.HEART_LAYOUT
            },
            isMinimized = isMinimized,
            onIsMinimizedChange = onIsMinimizedChange
        )
    } else if (showDataLayout.value) {
        GeneratorLayout(context = context, onToggleDataLayout = onToggleDataLayout, onInputFocusChanged = onInputFocusChanged)
    } else if (showHeartLayout.value) {
        TextLayout(
            onPasteText = onPasteText,
            onToggleTextLayout = onToggleHeartLayout, // Use the new toggle function
            onInputFocusChanged = onInputFocusChanged,
            alpha = alpha,
            size = size,
            onRestoreLayout = onRestoreLayout,
            currentLayoutState = currentLayoutState.value,
            buttonLayout = heartButtonLayout, // Use the new button layout
            onSwapLayout = {
                showHeartLayout.value = false
                showTextLayout.value = true
                currentLayoutState.value = OverlayLayoutState.TEXT_LAYOUT
            },
            isMinimized = isMinimized,
            onIsMinimizedChange = onIsMinimizedChange
        )
    } else if (showWebViewLayout.value) {
        WebViewLayout(
            context = context,
            onToggleWebViewLayout = onToggleWebViewLayout,
            onSaveDataFromWebView = onSaveDataFromWebView,
            onLoadDataForWebView = onLoadDataForWebView,
            onWebViewCreated = onWebViewCreated,
            isMinimized = isMinimized,
            onIsMinimizedChange = onIsMinimizedChange,
            alpha = alpha,
            size = size,
            onLaunchTermux = onLaunchTermux
        )
    } else if (showTikTokLayout.value) {
        TikTokLayout(
            notification = currentTikTokNotification.value,
            onClose = {
                showTikTokLayout.value = false
                currentLayoutState.value = OverlayLayoutState.MAIN
            },
            onReply = onReply
        )
    } else if (showNotificationListLayout.value) {
        NotificationListLayout(
            notifications = notificationList,
            onClose = { onToggleNotificationListLayout() }
        )
    } else {
        Column(modifier = rootModifier) {
            if (isExpanded.value) {
                Column { // Need to wrap the content in a Column or similar
                    
                    FloatingActionButton(
                        onClick = {
                            onClose()
                            onToggleExpand()
                        },
                        modifier = Modifier.size(size)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FloatingActionButton(
                        onClick = onToggleTextLayout,
                        modifier = Modifier.size(size)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Show Paste Layout")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FloatingActionButton(
                        onClick = onToggleHeartLayout,
                        modifier = Modifier.size(size)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Show Paste Layout")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FloatingActionButton(
                        onClick = onToggleDataLayout,
                        modifier = Modifier.size(size)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = "Data")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FloatingActionButton(
                        onClick = onToggleWebViewLayout,
                        modifier = Modifier.size(size)
                    ) {
                        Icon(Icons.Default.Web, contentDescription = "Show WebView Layout")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                     FloatingActionButton(
                        onClick = onToggleNotificationListLayout,
                        modifier = Modifier.size(size)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Show Notification List")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            FloatingActionButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(size)
            ) {
                if (isMinimized && currentLayoutState.value == OverlayLayoutState.TEXT_LAYOUT) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Restore Clipboard")
                } else {
                    Icon(if (isExpanded.value) Icons.Default.Close else Icons.Default.Add, contentDescription = "Toggle")
                }
            }
        }
    }
}

@Composable
fun WebViewLayout(
    context: Context,
    onToggleWebViewLayout: () -> Unit,
    onSaveDataFromWebView: (String) -> Unit,
    onLoadDataForWebView: () -> String,
    onWebViewCreated: (WebView) -> Unit,
    isMinimized: Boolean,
    onIsMinimizedChange: (Boolean) -> Unit,
    alpha: Float,
    size: Dp,
    onLaunchTermux: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val minimizedSize = (minOf(screenWidth, screenHeight) * 0.03f)

    if (isMinimized) {
        FloatingActionButton(
            onClick = { onIsMinimizedChange(false) },
            modifier = Modifier.size(width = minimizedSize, height = minimizedSize * 2)
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Minimized")
        }
    } else {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .alpha(alpha)
        ) {
            Row {
                FloatingActionButton(
                    onClick = { onIsMinimizedChange(true) },
                    modifier = Modifier.size(size / 2)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize Layout")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = onLaunchTermux,
                    modifier = Modifier.size(size / 2)
                ) {
                    Icon(Icons.Filled.Code, contentDescription = "Launch Termux")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            val webViewState = rememberWebViewState(url = "http://localhost:5000/")
            WebView(
                state = webViewState,
                modifier = Modifier.fillMaxWidth(0.95f).height(400.dp),
                onCreated = { webView ->
                    onWebViewCreated(webView)
                    webView.settings.javaScriptEnabled = true
                    webView.settings.loadWithOverviewMode = true
                    webView.settings.useWideViewPort = true
                    webView.addJavascriptInterface(WebAppInterface(context, onSaveDataFromWebView, onLoadDataForWebView), "Android")
                }
            )
            FloatingActionButton(onClick = onToggleWebViewLayout) {
                Icon(Icons.Default.Close, contentDescription = "Close WebView Layout")
            }
        }
    }
}

@Composable
fun TextLayout(
    onPasteText: (String) -> Unit,
    onToggleTextLayout: () -> Unit,
    onInputFocusChanged: (Boolean) -> Unit,
    alpha: Float,
    size: Dp,
    onRestoreLayout: (OverlayLayoutState) -> Unit,
    currentLayoutState: OverlayLayoutState,
    buttonLayout: List<ButtonConfig>,
    onSwapLayout: () -> Unit,
    isMinimized: Boolean,
    onIsMinimizedChange: (Boolean) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val minimizedSize = (minOf(screenWidth, screenHeight) * 0.03f)

    if (isMinimized) {
        FloatingActionButton(
            onClick = { onIsMinimizedChange(false) }, // Restore on click
            modifier = Modifier
                .size(width = minimizedSize, height = minimizedSize * 2)
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Minimized")
        }
    } else {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .alpha(alpha)
        ) {
            FloatingActionButton(
                onClick = { onIsMinimizedChange(true) }, // Minimize on click
                modifier = Modifier
                    .size(size / 2)
                    .align(Alignment.End)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize Layout")
            }
            Spacer(modifier = Modifier.height(8.dp)) // Add a small spacer

            val buttons = buttonLayout.toMutableList()

            // Extract specific buttons by ID
            val starButton = buttons.find { it.id == "paste_star" }
            val moneyButton = buttons.find { it.id == "paste_money" }
            val oneButton = buttons.find { it.id == "paste_one" }
            val whiteCircleButton = buttons.find { it.id == "paste_white_circle" }
            val stopButton = buttons.find { it.id == "paste_stop" }
            val sparkleComboButton = buttons.find { it.id == "paste_sparkle_combo" }
            val affordablePackageButton = buttons.find { it.id == "paste_affordable_package" }

            // Remove extracted buttons from the mutable list to avoid re-rendering
            buttons.removeAll(listOfNotNull(starButton, moneyButton, oneButton, whiteCircleButton, stopButton, sparkleComboButton, affordablePackageButton))

            Row(
                // Remove fillMaxWidth() from the Row itself, let it wrap content
                horizontalArrangement = Arrangement.spacedBy(16.dp), // Fixed space between the two columns
                verticalAlignment = Alignment.Top
            ) {
                // First Column
                Column(
                    horizontalAlignment = Alignment.Start, // Align buttons within this column to the start (left)
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Space between buttons in this column
                    // Remove weight from this column
                ) {
                    starButton?.let {
                        FloatingActionButton(onClick = { onPasteText(it.text) }) { Text(it.emoji ?: "") }
                    }
                    moneyButton?.let {
                        FloatingActionButton(onClick = { onPasteText(it.text) }) { Text(it.emoji ?: "") }
                    }
                    oneButton?.let {
                        FloatingActionButton(onClick = { onPasteText(it.text) }) { Text(it.emoji ?: "") }
                    }
                    whiteCircleButton?.let {
                        FloatingActionButton(onClick = { onPasteText(it.text) }) { Text(it.emoji ?: "") }
                    }
                    stopButton?.let {
                        FloatingActionButton(onClick = { onPasteText(it.text) }) { Text(it.emoji ?: "") }
                    }
                }

                // Remove the Spacer, as Arrangement.spacedBy handles the spacing
                // Spacer(modifier = Modifier.weight(0.5f))

                // Second Column
                Column(
                    horizontalAlignment = Alignment.Start, // Align buttons within this column to the start (left)
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Space between buttons in this column
                ) {
                    sparkleComboButton?.let {
                        FloatingActionButton(onClick = { onPasteText(it.text) }) { Text(it.emoji ?: "") }
                    }
                    affordablePackageButton?.let {
                        FloatingActionButton(onClick = { onPasteText(it.text) }) { Text(it.emoji ?: "") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // Space after the two columns

            // Render any remaining buttons (if any were not explicitly handled above)
            while (buttons.isNotEmpty()) {
                val button = buttons.removeFirst()
                FloatingActionButton(
                    onClick = { onPasteText(button.text) },
                ) {
                    Text(button.emoji ?: "")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            FloatingActionButton(
                onClick = onSwapLayout,
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Layout")
            }
            Spacer(modifier = Modifier.height(16.dp)) // Add a spacer

            FloatingActionButton(
                onClick = onToggleTextLayout,
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Text Layout")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorLayout(context: Context, onToggleDataLayout: () -> Unit, onInputFocusChanged: (Boolean) -> Unit) {
    var transactionText by remember { mutableStateOf("") }
    var linkText by remember { mutableStateOf("") }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Text Generator", style = MaterialTheme.typography.titleMedium)
        
        OutlinedTextField(
            value = transactionText,
            onValueChange = { transactionText = it },
            label = { Text("Transaction Text") },
            modifier = Modifier.fillMaxWidth().onFocusChanged { onInputFocusChanged(it.isFocused) }
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = linkText,
                onValueChange = { linkText = it },
                label = { Text("Link(s)") },
                modifier = Modifier.weight(1f).onFocusChanged { onInputFocusChanged(it.isFocused) },
                singleLine = false,
                minLines = 2
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { 
                if (linkText.isNotBlank()) {
                    val urlRegex = """(https?://\S+)""".toRegex()
                    urlRegex.find(linkText)?.value?.let { linkToOpen ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(linkToOpen)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open First Link")
            }
        }

        Button(
            onClick = {
                val transText = transactionText.trim()
                if (transText.isNotBlank()) {
                    val priceRegex = """(\d+\.?\d*)\s*TK""".toRegex()
                    val lastDigitsRegex = """\*{4}(\d{4})""".toRegex()
                    val urlRegex = """(https?://\S+)""".toRegex()

                    val priceMatch = priceRegex.find(transText)
                    val lastDigitsMatch = lastDigitsRegex.find(transText)
                    
                    val allLinks = urlRegex.findAll(linkText).map { it.value }.joinToString(" ")

                    val price = priceMatch?.groups?.get(1)?.value?.toDoubleOrNull()
                    val lastDigits = lastDigitsMatch?.groups?.get(1)?.value

                    if (price != null && lastDigits != null) {
                        val coinAmount = (price / 150 * 100).toInt()
                        val finalString = "$coinAmount coin ****${lastDigits.take(2)} $allLinks".trim()
                        clipboardManager.setText(AnnotatedString(finalString))
                        Log.d("ClipboardDebug", "Text set to clipboard: $finalString")
                        val currentClipboardText = clipboardManager.getText()
                        Log.d("ClipboardDebug", "Text read from clipboard immediately after setting: $currentClipboardText")
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not parse transaction text", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Copy to Clipboard")
        }

        Button(
            onClick = onToggleDataLayout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Close")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TikTokLayout(
    notification: TikTokNotification?,
    onClose: () -> Unit,
    onReply: (String) -> Unit
) {
    Log.d("FloatingWindowService", "TikTokLayout composable recomposed. Notification: $notification")
    if (notification == null) {
        Log.d("FloatingWindowService", "TikTokLayout: notification is null, returning.")
        return
    }

    var replyText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = notification.title ?: "No Title", fontWeight = FontWeight.Bold)
        Text(text = notification.text ?: "No Text")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = replyText,
            onValueChange = { replyText = it },
            label = { Text("Your reply") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = {
                Log.d("FloatingWindowService", "TikTokLayout: Close button clicked.")
                onClose()
            }) {
                Text("Close")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                Log.d("FloatingWindowService", "TikTokLayout: Reply button clicked with text: $replyText")
                onReply(replyText)
            }) {
                Text("Reply")
            }
        }
    }
}



class ServiceBackPressedDispatcherOwner(
    override val onBackPressedDispatcher: OnBackPressedDispatcher,
    override val lifecycle: Lifecycle
) : OnBackPressedDispatcherOwner

class WebAppInterface(private val mContext: Context, private val onSaveData: (String) -> Unit, private val onLoadData: () -> String) {
    @JavascriptInterface
    fun submitData(data: String) {
        Log.d("WebAppInterface", "Data received from WebView: $data")
        onSaveData(data)
    }

    @JavascriptInterface
    fun loadData(): String {
        Log.d("WebAppInterface", "Requesting data from Android")
        return onLoadData()
    }
}

@Composable
fun NotificationListLayout(
    notifications: List<String>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .background(Color.Black.copy(alpha = 0.7f), shape = MaterialTheme.shapes.medium)
            .padding(8.dp)
    ) {
        Button(onClick = onClose) {
            Text("Close")
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(notifications) { notification ->
                Text(
                    text = notification,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
