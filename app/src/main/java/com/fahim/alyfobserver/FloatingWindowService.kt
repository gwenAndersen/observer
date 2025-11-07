package com.fahim.alyfobserver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.LooksTwo
import androidx.compose.material.icons.filled.Looks3
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Web
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.ClipboardManager

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.onFocusChanged

import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.os.Handler
import android.os.Looper
import android.graphics.Rect // Added for keyboard detection
import android.view.ViewTreeObserver // Added for keyboard detection
import android.webkit.JavascriptInterface
import com.fahim.alyfobserver.ui.theme.NewAndroidProjectTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.google.accompanist.web.WebView
import android.webkit.WebView
import com.google.accompanist.web.rememberWebViewState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.LifecycleRegistry
import android.os.Bundle



enum class OverlayLayoutState { MAIN, TEXT_LAYOUT, DATA_LAYOUT, WEB_VIEW_LAYOUT, HEART_LAYOUT }

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
    private var isIdle = mutableStateOf(false)
    private var isExpanded = mutableStateOf(false)
    private var showTextLayout = mutableStateOf(false)
    private var showDataLayout = mutableStateOf(false)
    private var showWebViewLayout = mutableStateOf(false)
    private var showHeartLayout = mutableStateOf(false)
    private var currentLayoutState = mutableStateOf(OverlayLayoutState.MAIN)
    private var isOverlayInputFocused = mutableStateOf(false)
    private val dataRows = mutableStateListOf<DataRow>()
    private var screenHeight = 0 // Added for keyboard detection
    private var lastYPosition = 100 // Added to store last manual Y position
    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable: Runnable = Runnable {
        if (isExpanded.value) {
            isExpanded.value = false
            idleHandler.postDelayed(idleRunnable, 500) // Give it time to collapse
        } else {
            isIdle.value = true
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
        lifecycleScope.launch {
            dataRows.addAll(DataStoreManager.load(this@FloatingWindowService))
        } // Re-enable loading saved data
        updateWebViewData() // Update WebView with loaded data
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

        
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("FloatingWindowService", "onStartCommand: Service is started.\n")
        return START_NOT_STICKY
    }

    private fun handleLinkClick(link: String) {
        Log.d("FloatingWindowService", "Handling link click: $link")
        if (link.startsWith("http://") || link.startsWith("https://")) {
            // Open URL in browser
            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(browserIntent)
        } else if (link.contains(".")) {
            // Assume it's an app package name
            val launchIntent = packageManager.getLaunchIntentForPackage(link)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                Log.e("FloatingWindowService", "Could not find app for package: $link")
                // Optionally, show a toast to the user
            }
        }
    }

    private fun resetIdleTimer() {
        isIdle.value = false
        idleHandler.removeCallbacks(idleRunnable)
        idleHandler.postDelayed(idleRunnable, 20000) // 20 seconds
    }

    private fun saveDataRows() {
        lifecycleScope.launch {
            DataStoreManager.save(this@FloatingWindowService, dataRows)
        }
        updateWebViewData() // Call to update WebView after saving
    }

    private fun updateWebViewData() {
        val jsonData = kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(DataRow.serializer()), dataRows.toList())
        overlayView?.post { // Ensure this runs on the UI thread
            webViewInstance?.evaluateJavascript("window.updateDataFromAndroid('$jsonData')") { result ->
                Log.d("FloatingWindowService", "WebView update result: $result")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("FloatingWindowService", "onDestroy: Service is being destroyed.")
        unregisterReceiver(broadcastReceiver)
        hideOverlay()
        _viewModelStore.clear()
    }

    private fun updateOverlayFlags() {
        params.flags = if (isOverlayInputFocused.value) {
            // If an input field is focused, make the overlay focusable and allow IME interaction
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY or
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        } else {
            // Otherwise, make it non-focusable and non-touch-modal
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        windowManager.updateViewLayout(overlayView, params)
    }

    private fun showOverlay() {
        Log.d("FloatingWindowService", "showOverlay: Attempting to show overlay.")
        if (overlayView == null) {
            overlayView = ComposeView(this).apply {
                // Set up LifecycleOwner, ViewModelStoreOwner, and SavedStateRegistryOwner
                lifecycleOwner = ServiceLifecycleOwner()
                
                lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
                lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(this@FloatingWindowService)
                setViewTreeSavedStateRegistryOwner(this@FloatingWindowService)

                setContent {
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
                                    isIdle = isIdle.value,
                                    isExpanded = isExpanded,
                                    showTextLayout = showTextLayout.value,
                                    showDataLayout = showDataLayout.value,
                                    showWebViewLayout = showWebViewLayout.value,
                                    showHeartLayout = showHeartLayout.value,
                                    dataRows = dataRows,
                                    clipboardButtonLayout = clipboardButtonLayout,
                                    heartButtonLayout = heartButtonLayout,
                                    onClose = { hideOverlay() },
                                    onDrag = { dragAmount ->
                                        params.x = (params.x + dragAmount.x).roundToInt()
                                        params.y = (params.y + dragAmount.y).roundToInt()
                                        lastYPosition = params.y // Update last known Y position
                                        windowManager.updateViewLayout(this, params)
                                    },
                                    onWriteClick = { showTextLayout.value = true; currentLayoutState.value = OverlayLayoutState.TEXT_LAYOUT },
                                    onDumpClick = { /* TODO: Implement dump UI */ },
                                    onToggleExpand = {
                                        isExpanded.value = !isExpanded.value
                                        Log.d("FloatingWindowService", "isExpanded toggled to: ${isExpanded.value}")
                                        resetIdleTimer()
                                    },
                                    onToggleTextLayout = { showTextLayout.value = !showTextLayout.value; currentLayoutState.value = if (showTextLayout.value) OverlayLayoutState.TEXT_LAYOUT else OverlayLayoutState.MAIN },
                                    onToggleDataLayout = { showDataLayout.value = !showDataLayout.value; currentLayoutState.value = if (showDataLayout.value) OverlayLayoutState.DATA_LAYOUT else OverlayLayoutState.MAIN },
                                    onToggleWebViewLayout = { showWebViewLayout.value = !showWebViewLayout.value; currentLayoutState.value = if (showWebViewLayout.value) OverlayLayoutState.WEB_VIEW_LAYOUT else OverlayLayoutState.MAIN },
                                    onToggleHeartLayout = { showHeartLayout.value = !showHeartLayout.value; currentLayoutState.value = if (showHeartLayout.value) OverlayLayoutState.HEART_LAYOUT else OverlayLayoutState.MAIN },
                                    onPasteText = { text -> pasteText(text) },
                                    saveData = { saveDataRows() },
                                    onLinkClick = { link -> handleLinkClick(link) },
                                    currentLayoutState = currentLayoutState.value,
                                    onRestoreLayout = { state ->
                                        isIdle.value = false
                                        isExpanded.value = true
                                        when (state) {
                                            OverlayLayoutState.MAIN -> { showTextLayout.value = false; showDataLayout.value = false; showWebViewLayout.value = false; showHeartLayout.value = false }
                                            OverlayLayoutState.TEXT_LAYOUT -> { showTextLayout.value = true; showDataLayout.value = false; showWebViewLayout.value = false; showHeartLayout.value = false }
                                            OverlayLayoutState.DATA_LAYOUT -> { showTextLayout.value = false; showDataLayout.value = true; showWebViewLayout.value = false; showHeartLayout.value = false }
                                            OverlayLayoutState.WEB_VIEW_LAYOUT -> { showTextLayout.value = false; showDataLayout.value = false; showWebViewLayout.value = true; showHeartLayout.value = false }
                                            OverlayLayoutState.HEART_LAYOUT -> { showTextLayout.value = false; showDataLayout.value = false; showWebViewLayout.value = false; showHeartLayout.value = true }
                                        }
                                        currentLayoutState.value = state
                                        resetIdleTimer()
                                    },
                                    onSaveDataFromWebView = { jsonData ->
                                        lifecycleScope.launch {
                                            val newDataRows = Json.decodeFromString(ListSerializer(DataRow.serializer()), jsonData)
                                            dataRows.clear()
                                            dataRows.addAll(newDataRows)
                                            saveDataRows()
                                        }
                                    },
                                    onLoadDataForWebView = { ->
                                        Json.encodeToString(ListSerializer(DataRow.serializer()), dataRows.toList())
                                    },
                                    onWebViewCreated = { webView -> webViewInstance = webView },
                                    onInputFocusChanged = { isFocused ->
                                        isOverlayInputFocused.value = isFocused
                                        updateOverlayFlags()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            windowManager.addView(overlayView, params)
            overlayView?.viewTreeObserver?.addOnGlobalLayoutListener(globalLayoutListener)
            screenHeight = windowManager.defaultDisplay.height // Initialize screenHeight
            resetIdleTimer()
        }
    }

    private fun hideOverlay() {
        Log.d("FloatingWindowService", "hideOverlay: Attempting to hide overlay.")
        idleHandler.removeCallbacks(idleRunnable)
        overlayView?.let {
            windowManager.removeView(it)
            it.viewTreeObserver?.removeOnGlobalLayoutListener(globalLayoutListener) // Remove listener
            Log.d("FloatingWindowService", "hideOverlay: Overlay view removed successfully.")
        }
        overlayView = null
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
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
    isIdle: Boolean,
    isExpanded: MutableState<Boolean>,
    showTextLayout: Boolean,
    showDataLayout: Boolean,
    showWebViewLayout: Boolean,
    showHeartLayout: Boolean,
    dataRows: SnapshotStateList<DataRow>,
    clipboardButtonLayout: List<ButtonConfig>,
    heartButtonLayout: List<ButtonConfig>,
    onClose: () -> Unit,
    onDrag: (Offset) -> Unit,
    onWriteClick: () -> Unit,
    onDumpClick: () -> Unit,
    onToggleExpand: () -> Unit,
    onToggleTextLayout: () -> Unit,
    onToggleDataLayout: () -> Unit,
    onToggleWebViewLayout: () -> Unit,
    onToggleHeartLayout: () -> Unit,
    onPasteText: (String) -> Unit,
    saveData: () -> Unit,
    onLinkClick: (String) -> Unit,
    currentLayoutState: OverlayLayoutState,
    onRestoreLayout: (OverlayLayoutState) -> Unit,
    onSaveDataFromWebView: (String) -> Unit,
    onLoadDataForWebView: () -> String,
    onWebViewCreated: (android.webkit.WebView) -> Unit,
    onInputFocusChanged: (Boolean) -> Unit // New parameter
) {
    val alpha by animateFloatAsState(if (isIdle) 0.5f else 1f)
    val size by animateDpAsState(if (isIdle) 40.dp else 56.dp)

    if (showTextLayout) {
        TextLayout(
            onPasteText = onPasteText,
            onToggleTextLayout = onToggleTextLayout,
            onInputFocusChanged = onInputFocusChanged,
            isIdle = isIdle,
            alpha = alpha,
            size = size,
            onRestoreLayout = onRestoreLayout,
            currentLayoutState = currentLayoutState,
            buttonLayout = clipboardButtonLayout
        )
    } else if (showDataLayout) {
        DataLayout(dataRows = dataRows, onToggleDataLayout = onToggleDataLayout, saveData = saveData, onLinkClick = onLinkClick, onInputFocusChanged = onInputFocusChanged)
    } else if (showHeartLayout) {
        TextLayout(
            onPasteText = onPasteText,
            onToggleTextLayout = onToggleHeartLayout, // Use the new toggle function
            onInputFocusChanged = onInputFocusChanged,
            isIdle = isIdle,
            alpha = alpha,
            size = size,
            onRestoreLayout = onRestoreLayout,
            currentLayoutState = currentLayoutState,
            buttonLayout = heartButtonLayout // Use the new button layout
        )
    } else if (showWebViewLayout) {
                                        WebViewLayout(context = context, onToggleWebViewLayout = onToggleWebViewLayout, onSaveDataFromWebView = onSaveDataFromWebView, onLoadDataForWebView = onLoadDataForWebView, onWebViewCreated = onWebViewCreated)
    } else {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .alpha(alpha)
                .pointerInput(Unit) {
                    detectDragGestures {
                        change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount)
                    }
                }
                .clickable(enabled = isIdle) { // Handle click when idle
                    onRestoreLayout(currentLayoutState)
                }
        ) {
            if (isExpanded.value) {
                Column { // Need to wrap the content in a Column or similar
                    FloatingActionButton(
                        onClick = {
                            onDumpClick()
                            onToggleExpand()
                        },
                        modifier = Modifier.size(size)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Dump UI")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    FloatingActionButton(
                        onClick = {
                            onWriteClick()
                            onToggleExpand()
                        },
                        modifier = Modifier.size(size)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Write Text")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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
                }
            }

            FloatingActionButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(size)
            ) {
                if (isIdle && currentLayoutState == OverlayLayoutState.TEXT_LAYOUT) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Restore Clipboard")
                } else {
                    Icon(if (isExpanded.value) Icons.Default.Close else Icons.Default.Add, contentDescription = "Toggle")
                }
            }
        }
    }
}

@Composable
fun WebViewLayout(context: Context, onToggleWebViewLayout: () -> Unit, onSaveDataFromWebView: (String) -> Unit, onLoadDataForWebView: () -> String, onWebViewCreated: (android.webkit.WebView) -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        val webViewState = rememberWebViewState(url = "file:///android_asset/data_layout.html")
        WebView(
            state = webViewState,
            modifier = Modifier.fillMaxWidth(0.95f).height(400.dp),
            onCreated = { webView ->
                onWebViewCreated(webView) // Call the callback to pass the WebView instance
                webView.settings.javaScriptEnabled = true // Enable JavaScript
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

@Composable
fun TextLayout(
    onPasteText: (String) -> Unit,
    onToggleTextLayout: () -> Unit,
    onInputFocusChanged: (Boolean) -> Unit,
    isIdle: Boolean,
    alpha: Float,
    size: Dp,
    onRestoreLayout: (OverlayLayoutState) -> Unit,
    currentLayoutState: OverlayLayoutState,
    buttonLayout: List<ButtonConfig>
) {
    if (isIdle) {
        FloatingActionButton(
            onClick = { onRestoreLayout(currentLayoutState) },
            modifier = Modifier
                .padding(16.dp)
                .alpha(alpha)
                .size(size)
        ) {
            Icon(Icons.Default.ContentPaste, contentDescription = "Restore Clipboard")
        }
    } else {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .alpha(alpha)
        ) {
            val buttons = buttonLayout.toMutableList()
            while (buttons.isNotEmpty()) {
                val button = buttons.removeFirst()
                if (button.id == "paste_star" && buttons.firstOrNull()?.id == "paste_sparkle_combo") {
                    val secondButton = buttons.removeFirst()
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { onPasteText(button.text) },
                        ) {
                            Text(button.emoji ?: "")
                        }
                        FloatingActionButton(
                            onClick = { onPasteText(secondButton.text) },
                        ) {
                            Text(secondButton.emoji ?: "")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    FloatingActionButton(
                        onClick = { onPasteText(button.text) },
                    ) {
                        Text(button.emoji ?: "")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            FloatingActionButton(
                onClick = onToggleTextLayout,
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Text Layout")
            }
        }
    }
}

@Composable
fun DataLayout(dataRows: SnapshotStateList<DataRow>, onToggleDataLayout: () -> Unit, saveData: () -> Unit, onLinkClick: (String) -> Unit, onInputFocusChanged: (Boolean) -> Unit) {

    // Ensure there's always at least one empty row
    if (dataRows.isEmpty()) {
        dataRows.add(DataRow(1, "", "Green"))
    }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text("Type", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.2f), color = MaterialTheme.colorScheme.onSurface)
                Text("Link", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), color = MaterialTheme.colorScheme.onSurface)
                Text("State", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.2f), color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.weight(0.1f)) // For the remove button
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                itemsIndexed(dataRows) { index, row ->
                    DataRowItem(
                        dataRow = row,
                        onTypeChange = { newType ->
                            val updatedRow = row.copy(type = newType)
                            dataRows[index] = updatedRow
                            saveData()
                        },
                        onLinkChange = { newLink ->
                            val updatedRow = row.copy(link = newLink)
                            dataRows[index] = updatedRow
                            // Add a new empty row if the current one is being filled and it's the last one
                            if (index == dataRows.lastIndex && newLink.isNotBlank()) {
                                dataRows.add(DataRow(1, "", "Green"))
                            }
                            saveData()
                        },
                        onStateChange = { newState ->
                            val updatedRow = row.copy(state = newState)
                            dataRows[index] = updatedRow
                            saveData()
                        },
                        onRemove = {
                            if (dataRows.size > 1) { // Don't remove the last row if it's the only one
                                dataRows.removeAt(index)
                                saveData()
                            }
                        },
                        onLinkClick = { link -> onLinkClick(link) },
                        onInputFocusChanged = onInputFocusChanged
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            FloatingActionButton(onClick = onToggleDataLayout) {
                Icon(Icons.Default.Close, contentDescription = "Close Data Layout")
            }
        }
    }
}

@Composable
fun DataRowItem(
    dataRow: DataRow,
    onTypeChange: (Int) -> Unit,
    onLinkChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onRemove: () -> Unit,
    onLinkClick: (String) -> Unit,
    onInputFocusChanged: (Boolean) -> Unit // New parameter
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (dataRow.type) {
                1 -> Icons.Default.LooksOne
                2 -> Icons.Default.LooksTwo
                3 -> Icons.Default.Looks3
                else -> Icons.Default.LooksOne // Default case
            },
            contentDescription = "Type ${dataRow.type}",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable {
                onTypeChange(if (dataRow.type == 3) 1 else dataRow.type + 1)
            }.weight(0.2f)
        )

        OutlinedTextField(
            value = dataRow.link,
            onValueChange = onLinkChange,
            modifier = Modifier
                .weight(0.5f)
                .onFocusChanged { focusState ->
                    onInputFocusChanged(focusState.isFocused)
                }
                .clickable {
                    onLinkClick(dataRow.link)
                },
            singleLine = true,
            readOnly = false // Make it editable
        )

        Icon(
            imageVector = if (dataRow.state == "Green") Icons.Default.Done else Icons.Default.Close,
            contentDescription = "State ${dataRow.state}",
            tint = if (dataRow.state == "Green") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.clickable {
                onStateChange(if (dataRow.state == "Green") "Red" else "Green")
            }.weight(0.2f)
        )

        val clipboardManager: ClipboardManager = LocalClipboardManager.current
        IconButton(onClick = {
            val textToCopy = "${dataRow.type} ${dataRow.link} ${dataRow.state}"
            clipboardManager.setText(AnnotatedString(textToCopy))
        }) {
            Icon(Icons.Default.ContentPaste, contentDescription = "Copy Row", tint = MaterialTheme.colorScheme.onSurface)
        }
        IconButton(onClick = onRemove, modifier = Modifier.weight(0.1f)) {
            Icon(Icons.Default.Delete, contentDescription = "Remove Row", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Serializable
data class DataRow(
    var type: Int,
    var link: String,
    var state: String
)

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
