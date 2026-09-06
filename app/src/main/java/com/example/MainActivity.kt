package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.WatchRepository
import com.example.server.PhoneRemoteServer
import com.example.server.RemoteActionListener
import com.example.tv.StreamTvWebView
import com.example.tv.StreamWebController
import com.example.tv.VirtualCursorController
import com.example.ui.components.BookmarksHistoryDialog
import com.example.ui.components.PhoneRemoteDialog
import com.example.ui.components.QuickSearchDialog
import com.example.ui.components.TvHeaderOverlay
import com.example.ui.components.VirtualCursorOverlay
import com.example.ui.theme.CinemaDarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity(), RemoteActionListener {

    private lateinit var webController: StreamWebController
    private val cursorController = VirtualCursorController()
    private lateinit var watchRepository: WatchRepository
    private var phoneRemoteServer: PhoneRemoteServer? = null
    private lateinit var fullscreenContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep TV screen awake during movie playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        webController = StreamWebController(this)
        watchRepository = WatchRepository(this)

        fullscreenContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }

        // Initialize and start micro HTTP server for remote phone controller
        phoneRemoteServer = PhoneRemoteServer(this, 8088, this).apply {
            start()
        }

        setContent {
            MyApplicationTheme {
                val bookmarks by watchRepository.bookmarks.collectAsState()
                val history by watchRepository.history.collectAsState()

                var showSearchDialog by remember { mutableStateOf(false) }
                var showBookmarksDialog by remember { mutableStateOf(false) }
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
                    // Main Movie Stream WebView
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
                            onOpenSearch = { showSearchDialog = true },
                            onOpenBookmarks = { showBookmarksDialog = true },
                            onOpenPhoneRemote = { showPhoneRemoteDialog = true },
                            onToggleBookmark = {
                                watchRepository.toggleBookmark(
                                    webController.currentTitle,
                                    webController.currentUrl
                                )
                            },
                            isCurrentBookmarked = watchRepository.isBookmarked(webController.currentUrl)
                        )
                    }

                    // On-screen Virtual Cursor Pointer
                    if (!webController.isFullscreenVideo) {
                        VirtualCursorOverlay(controller = cursorController)
                    }

                    // Dialogs
                    if (showSearchDialog) {
                        QuickSearchDialog(
                            onDismiss = { showSearchDialog = false },
                            onSelectUrl = { url ->
                                webController.loadUrl(url)
                            }
                        )
                    }

                    if (showBookmarksDialog) {
                        BookmarksHistoryDialog(
                            history = history,
                            bookmarks = bookmarks,
                            onSelectUrl = { url ->
                                webController.loadUrl(url)
                            },
                            onDismiss = { showBookmarksDialog = false }
                        )
                    }

                    if (showPhoneRemoteDialog) {
                        PhoneRemoteDialog(
                            serverUrl = phoneRemoteServer?.getServerUrl() ?: "http://127.0.0.1:8088",
                            onDismiss = { showPhoneRemoteDialog = false }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        phoneRemoteServer?.stop()
    }

    // --- Android TV Physical Remote Key Handling ---
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val webView = webController.webView
            val step = if (event.repeatCount > 0) 35f else 22f

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
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (webView != null) {
                            cursorController.performClick(webView)
                            return true
                        }
                    }
                }
            }

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
                KeyEvent.KEYCODE_MENU -> {
                    cursorController.toggleMode()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (webController.goBack()) {
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // --- RemoteActionListener implementation (from Phone Web Remote) ---
    override fun onDpadKey(key: String) {
        val webView = webController.webView
        when (key) {
            "up" -> cursorController.move(0f, -35f, webView)
            "down" -> cursorController.move(0f, 35f, webView)
            "left" -> cursorController.move(-35f, 0f, webView)
            "right" -> cursorController.move(35f, 0f, webView)
            "enter" -> webView?.let { cursorController.performClick(it) }
            "back" -> webController.goBack()
            "home" -> webController.goHome()
            "play" -> webController.playPauseVideo()
            "forward" -> webController.seekRelative(10)
            "rewind" -> webController.seekRelative(-10)
            "fullscreen" -> webController.toggleFullscreenVideo()
            "zoom_in" -> webController.setZoom((webController.textZoom + 25).coerceAtMost(200))
            "zoom_out" -> webController.setZoom((webController.textZoom - 25).coerceAtLeast(75))
            "menu" -> cursorController.toggleMode()
        }
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
}
