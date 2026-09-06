package com.example

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.data.WatchRepository
import com.example.data.WatchlistRepository
import com.example.server.PhoneRemoteServer
import com.example.server.RemoteActionListener
import com.example.server.WebSocketCommandListener
import com.example.server.WebSocketServer
import com.example.tv.StreamTvWebView
import com.example.tv.StreamWebController
import com.example.tv.VirtualCursorController
import com.example.ui.components.BookmarksHistoryDialog
import com.example.ui.components.PhoneRemoteDialog
import com.example.ui.components.QuickSearchDialog
import com.example.ui.components.RecentlyWatchedShelf
import com.example.ui.components.TvHeaderOverlay
import com.example.ui.components.VirtualCursorOverlay
import com.example.ui.components.WatchlistMenuOverlay
import com.example.ui.theme.CinemaDarkBackground
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), RemoteActionListener, WebSocketCommandListener {

    companion object {
        init {
            try {
                android.system.Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
                android.system.Os.setenv("MESA_LOADER_DRIVER_OVERRIDE", "llvmpipe", true)
                android.system.Os.setenv("GALLIUM_DRIVER", "llvmpipe", true)
            } catch (_: Throwable) {}
        }
    }

    private lateinit var webController: StreamWebController
    private val cursorController = VirtualCursorController()
    private lateinit var watchRepository: WatchRepository
    private lateinit var watchlistRepository: WatchlistRepository
    private var phoneRemoteServer: PhoneRemoteServer? = null
    private var webSocketServer: WebSocketServer? = null
    private lateinit var fullscreenContainer: FrameLayout
    private var audioManager: AudioManager? = null
    private var isSearchDialogVisible by mutableStateOf(false)
    private var isWatchlistOverlayVisible by mutableStateOf(false)
    private var isBookmarksDialogVisible by mutableStateOf(false)

    // Android TV Voice Search Contract
    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenQuery = matches?.firstOrNull()?.trim()
            if (!spokenQuery.isNullOrBlank()) {
                Toast.makeText(this, "Voice Search: \"$spokenQuery\"", Toast.LENGTH_SHORT).show()
                webController.loadUrl(spokenQuery)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Android SplashScreen API - branded cinema startup experience
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep TV screen awake during movie playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        webController = StreamWebController(this)
        watchRepository = WatchRepository(this)
        watchlistRepository = WatchlistRepository(this)

        // Hold branded splash screen while initial WebView engine loads
        var keepSplashOnScreen = true
        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            while (webController.isInitialLoading && System.currentTimeMillis() - startTime < 2500) {
                delay(50)
            }
            keepSplashOnScreen = false
        }
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        handleSearchIntent(intent)

        fullscreenContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }

        // Initialize and start micro HTTP server for phone companion web client (port 8088)
        phoneRemoteServer = PhoneRemoteServer(this, httpPort = 8088, wsPort = 8089, listener = this).apply {
            start()
        }

        // Initialize and start WebSocket server for mobile phone apps and real-time remote commands (port 8089)
        webSocketServer = WebSocketServer(port = 8089, listener = this).apply {
            start()
        }

        setContent {
            MyApplicationTheme {
                val bookmarks by watchRepository.bookmarks.collectAsState()
                val history by watchRepository.history.collectAsState()
                val watchlist by watchlistRepository.watchlist.collectAsState(initial = emptyList())
                val watchlistCount by watchlistRepository.count.collectAsState(initial = 0)
                val isCurrentInWatchlist by watchlistRepository.observeIsWatchlisted(webController.currentUrl).collectAsState(initial = false)

                var showPhoneRemoteDialog by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CinemaDarkBackground)
                        .onGloballyPositioned { coordinates ->
                            cursorController.updateScreenSize(
                                coordinates.size.width.toFloat(),
                                coordinates.size.height.toFloat()
                            )
                        }
                ) {
                    // Main Movie Stream WebView (with Material Design Progress Indicator and TV Focus Handling)
                    StreamTvWebView(
                        modifier = Modifier.fillMaxSize(),
                        controller = webController,
                        onPageFinished = { title, url ->
                            watchRepository.addHistory(title, url)
                        },
                        fullscreenContainer = fullscreenContainer
                    )

                    // Fullscreen Video Player Container for WebChromeClient
                    AndroidView(
                        factory = { fullscreenContainer },
                        modifier = Modifier.fillMaxSize()
                    )

                    // TV Header / HUD Navigation
                    if (!webController.isFullscreenVideo) {
                        TvHeaderOverlay(
                            webController = webController,
                            cursorController = cursorController,
                            phoneServerUrl = phoneRemoteServer?.getServerUrl() ?: "",
                            onOpenSearch = { isSearchDialogVisible = true },
                            onVoiceSearch = { launchVoiceSearch() },
                            onOpenBookmarks = { isBookmarksDialogVisible = true },
                            onOpenWatchlist = { isWatchlistOverlayVisible = true },
                            watchlistCount = watchlistCount,
                            onOpenPhoneRemote = { showPhoneRemoteDialog = true },
                            onToggleBookmark = {
                                watchRepository.toggleBookmark(
                                    webController.currentTitle,
                                    webController.currentUrl
                                )
                            },
                            isCurrentBookmarked = watchRepository.isBookmarked(webController.currentUrl),
                            onToggleRecentlyWatched = {
                                // Toggle recently watched shelf
                            },
                            recentlyWatchedCount = minOf(history.size, 10)
                        )
                    }

                    // On-screen Virtual Cursor Pointer (active when cursor mode is enabled)
                    if (!webController.isFullscreenVideo) {
                        VirtualCursorOverlay(controller = cursorController)
                    }

                    // Recently Watched Shelf (Last 10 Played Movies)
                    if (!webController.isFullscreenVideo && !isWatchlistOverlayVisible && !isSearchDialogVisible && !isBookmarksDialogVisible) {
                        RecentlyWatchedShelf(
                            history = history,
                            onSelectMovie = { url ->
                                webController.loadUrl(url)
                            },
                            onAddToWatchlist = { item ->
                                lifecycleScope.launch {
                                    watchlistRepository.addToWatchlist(item.title, item.url, item.imdbId)
                                    Toast.makeText(this@MainActivity, "Saved to Watchlist", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    // Custom Menu Overlay: Room-backed Watchlist
                    if (isWatchlistOverlayVisible) {
                        WatchlistMenuOverlay(
                            watchlist = watchlist,
                            currentTitle = webController.currentTitle,
                            currentUrl = webController.currentUrl,
                            isCurrentInWatchlist = isCurrentInWatchlist,
                            onSelectMovie = { url ->
                                webController.loadUrl(url)
                            },
                            onRemoveFromWatchlist = { id ->
                                lifecycleScope.launch {
                                    watchlistRepository.removeFromWatchlist(id)
                                }
                            },
                            onAddCurrentToWatchlist = {
                                lifecycleScope.launch {
                                    watchlistRepository.addToWatchlist(webController.currentTitle, webController.currentUrl)
                                    Toast.makeText(this@MainActivity, "Saved to Watchlist", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDismiss = { isWatchlistOverlayVisible = false }
                        )
                    }

                    // Dialogs
                    if (isSearchDialogVisible) {
                        QuickSearchDialog(
                            onDismiss = { isSearchDialogVisible = false },
                            onSelectUrl = { url ->
                                webController.loadUrl(url)
                            },
                            onStartVoiceSearch = {
                                launchVoiceSearch()
                            }
                        )
                    }

                    if (isBookmarksDialogVisible) {
                        BookmarksHistoryDialog(
                            history = history,
                            bookmarks = bookmarks,
                            onSelectUrl = { url ->
                                webController.loadUrl(url)
                            },
                            onDismiss = { isBookmarksDialogVisible = false }
                        )
                    }

                    if (showPhoneRemoteDialog) {
                        PhoneRemoteDialog(
                            serverUrl = phoneRemoteServer?.getServerUrl() ?: "http://127.0.0.1:8088",
                            wsUrl = phoneRemoteServer?.getWebSocketUrl() ?: "ws://127.0.0.1:8089",
                            onDismiss = { showPhoneRemoteDialog = false }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        webController.webView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        webController.webView?.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        phoneRemoteServer?.stop()
        webSocketServer?.stop()
        webController.destroy()
    }

    // --- Android TV Search & Voice Search Integration ---
    /**
     * Triggered when the user presses the search / microphone button on the Android TV remote.
     */
    override fun onSearchRequested(): Boolean {
        launchVoiceSearch()
        return true
    }

    /**
     * Launches the speech recognition dialog allowing users to speak movie titles
     * using the TV remote's built-in microphone.
     */
    fun launchVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_search_prompt))
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            voiceSearchLauncher.launch(intent)
        } catch (_: Exception) {
            // Fallback to text search dialog if voice recognition service is not present
            isSearchDialogVisible = true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSearchIntent(intent)
    }

    private fun handleSearchIntent(intent: Intent?) {
        if (intent == null) return
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            if (!query.isNullOrBlank()) {
                webController.loadUrl(query)
            }
        }
    }

    // --- Physical Keyboard and Android TV Remote Key Handling ---
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val webView = webController.webView
            val step = if (event.repeatCount > 0) 35f else 22f

            // 1. Physical Keyboard Space Key: Play/Pause video (only when user is not typing in search dialog)
            if (event.keyCode == KeyEvent.KEYCODE_SPACE && !isSearchDialogVisible) {
                webController.playPauseVideo()
                return true
            }

            // 2. Physical Keyboard Escape / TV Back Key handling with overlay dismissal
            if (event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
                if (isWatchlistOverlayVisible) {
                    isWatchlistOverlayVisible = false
                    return true
                }
                if (isBookmarksDialogVisible) {
                    isBookmarksDialogVisible = false
                    return true
                }
                if (isSearchDialogVisible) {
                    isSearchDialogVisible = false
                    return true
                }
                if (webController.isFullscreenVideo) {
                    webController.exitFullscreenVideo()
                    return true
                }
                if (webController.goBack()) {
                    return true
                }
            }

            // Mode 1: Virtual Cursor Mode (mouse pointer emulation)
            if (cursorController.isCursorMode) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        cursorController.move(0f, -step, webView)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        cursorController.move(0f, step, webView)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        cursorController.move(-step, 0f, webView)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        cursorController.move(step, 0f, webView)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        if (webView != null) {
                            cursorController.performClick(webView)
                            return true
                        }
                    }
                }
            } else {
                // Mode 2: D-Pad Native Focus Navigation Mode
                // Direct arrow navigation to move highlight across links, movie cards, and player controls
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        webController.clickFocusedElement()
                        webView?.dispatchKeyEvent(event)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (webView != null) {
                            val handled = webView.dispatchKeyEvent(event)
                            if (handled) return true
                            // If unhandled by web view focus, smoothly scroll for physical keyboard arrows
                            when (event.keyCode) {
                                KeyEvent.KEYCODE_DPAD_UP -> webView.scrollBy(0, -120)
                                KeyEvent.KEYCODE_DPAD_DOWN -> webView.scrollBy(0, 120)
                                KeyEvent.KEYCODE_DPAD_LEFT -> webView.scrollBy(-120, 0)
                                KeyEvent.KEYCODE_DPAD_RIGHT -> webView.scrollBy(120, 0)
                            }
                            return true
                        }
                    }
                }
            }

            // Page Up / Down navigation
            when (event.keyCode) {
                KeyEvent.KEYCODE_PAGE_UP -> {
                    webView?.pageUp(false)
                    return true
                }
                KeyEvent.KEYCODE_PAGE_DOWN -> {
                    webView?.pageDown(false)
                    return true
                }
            }

            // Physical Keyboard Media & Convenience Hotkeys (when search input is not active)
            if (!isSearchDialogVisible) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_W -> {
                        isWatchlistOverlayVisible = !isWatchlistOverlayVisible
                        return true
                    }
                    KeyEvent.KEYCODE_F -> {
                        webController.toggleFullscreenVideo()
                        return true
                    }
                    KeyEvent.KEYCODE_M -> {
                        onVolumeCommand("mute")
                        return true
                    }
                    KeyEvent.KEYCODE_K -> {
                        webController.playPauseVideo()
                        return true
                    }
                    KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_LEFT_BRACKET -> {
                        webController.seekRelative(-10)
                        return true
                    }
                    KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_RIGHT_BRACKET -> {
                        webController.seekRelative(10)
                        return true
                    }
                    KeyEvent.KEYCODE_C -> {
                        cursorController.toggleMode()
                        return true
                    }
                    KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_EQUALS -> {
                        onVolumeCommand("up")
                        return true
                    }
                    KeyEvent.KEYCODE_MINUS -> {
                        onVolumeCommand("down")
                        return true
                    }
                }
            }

            // TV Remote Voice & Search Keys
            when (event.keyCode) {
                KeyEvent.KEYCODE_SEARCH,
                KeyEvent.KEYCODE_VOICE_ASSIST,
                KeyEvent.KEYCODE_ASSIST -> {
                    onSearchRequested()
                    return true
                }
            }

            // Media & System Keys
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    webController.playPauseVideo()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    webController.seekRelative(10)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    webController.seekRelative(-10)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    onVolumeCommand("up")
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    onVolumeCommand("down")
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_MUTE -> {
                    onVolumeCommand("mute")
                    return true
                }
                KeyEvent.KEYCODE_MENU -> {
                    cursorController.toggleMode()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (isWatchlistOverlayVisible) {
                        isWatchlistOverlayVisible = false
                        return true
                    }
                    if (isBookmarksDialogVisible) {
                        isBookmarksDialogVisible = false
                        return true
                    }
                    if (isSearchDialogVisible) {
                        isSearchDialogVisible = false
                        return true
                    }
                    if (webController.isFullscreenVideo) {
                        webController.exitFullscreenVideo()
                        return true
                    }
                    if (webController.goBack()) {
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // --- WebSocketCommandListener implementation (Real-Time WebSocket Server) ---
    override fun onPlaybackCommand(action: String) {
        when (action.lowercase()) {
            "play", "pause", "play_pause", "toggle_play" -> webController.playPauseVideo()
            "forward", "seek_forward", "fast_forward" -> webController.seekRelative(10)
            "rewind", "seek_rewind" -> webController.seekRelative(-10)
            "fullscreen" -> webController.toggleFullscreenVideo()
        }
    }

    override fun onVolumeCommand(action: String) {
        try {
            when (action.lowercase()) {
                "up", "raise" -> {
                    audioManager?.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
                "down", "lower" -> {
                    audioManager?.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
                "mute", "toggle_mute" -> {
                    audioManager?.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_TOGGLE_MUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
            }
        } catch (_: Exception) {}
    }

    override fun onNavigationCommand(action: String, value: String?) {
        val webView = webController.webView
        when (action.lowercase()) {
            "load_url" -> value?.let { webController.loadUrl(it) }
            "search" -> value?.let { webController.loadUrl(it) }
            "voice_search", "mic", "voice" -> launchVoiceSearch()
            "up" -> {
                if (cursorController.isCursorMode) {
                    cursorController.move(0f, -35f, webView)
                } else {
                    webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP))
                    webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP))
                }
            }
            "down" -> {
                if (cursorController.isCursorMode) {
                    cursorController.move(0f, 35f, webView)
                } else {
                    webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
                    webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN))
                }
            }
            "left" -> {
                if (cursorController.isCursorMode) {
                    cursorController.move(-35f, 0f, webView)
                } else {
                    webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                    webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
                }
            }
            "right" -> {
                if (cursorController.isCursorMode) {
                    cursorController.move(35f, 0f, webView)
                } else {
                    webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                    webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
                }
            }
            "enter", "select", "ok" -> {
                if (cursorController.isCursorMode) {
                    webView?.let { cursorController.performClick(it) }
                } else {
                    webController.clickFocusedElement()
                    webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
                    webView?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
                }
            }
            "back" -> webController.goBack()
            "home" -> webController.goHome()
            "menu" -> cursorController.toggleMode()
            "zoom_in" -> webController.setZoom((webController.textZoom + 25).coerceAtMost(200))
            "zoom_out" -> webController.setZoom((webController.textZoom - 25).coerceAtLeast(75))
        }
    }

    override fun onMouseCommand(action: String, dx: Float, dy: Float) {
        val webView = webController.webView
        when (action) {
            "move" -> cursorController.move(dx, dy, webView)
            "click" -> webView?.let { cursorController.performClick(it) }
            "scroll" -> webController.scrollBy(0, dy.toInt())
        }
    }

    // --- RemoteActionListener implementation (from Phone HTTP / Companion) ---
    override fun onDpadKey(key: String) {
        onNavigationCommand(key, null)
    }

    override fun onMouseMove(dx: Float, dy: Float) {
        cursorController.move(dx, dy, webController.webView)
    }

    override fun onMouseClick() {
        webController.webView?.let { cursorController.performClick(it) }
    }

    override fun onMouseScroll(deltaY: Float) {
        webController.scrollBy(0, deltaY.toInt())
    }

    override fun onLoadUrl(url: String) {
        webController.loadUrl(url)
    }

    override fun onSearchQuery(query: String) {
        webController.loadUrl(query)
    }

    override fun onVolumeAction(action: String) {
        onVolumeCommand(action)
    }

    override fun onPlaybackAction(action: String) {
        onPlaybackCommand(action)
    }

    override fun onVoiceSearchAction() {
        launchVoiceSearch()
    }
}
