package com.example.tv

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CinemaDarkBackground
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.CinemaTextPrimary
import com.example.ui.theme.CinemaTextSecondary
import com.example.ui.theme.ImdbGold

class StreamWebController(val context: Context) {
    var webView: WebView? = null
    var isLoading by mutableStateOf(true)
    var isInitialLoading by mutableStateOf(true)
    var loadingProgress by mutableIntStateOf(10)
    var currentTitle by mutableStateOf("StreamIMDb TV")
    var currentUrl by mutableStateOf("https://streamimdb.ru/")
    var blockedAdsCount by mutableIntStateOf(0)
    var textZoom by mutableIntStateOf(100)
    var isFullscreenVideo by mutableStateOf(false)
    var customVideoView: View? = null
    var customViewCallback: WebChromeClient.CustomViewCallback? = null

    fun loadUrl(url: String) {
        val target = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.lowercase().startsWith("tt") -> "https://streamimdb.ru/movie/$url"
            else -> "https://streamimdb.ru/?s=" + java.net.URLEncoder.encode(url, "UTF-8")
        }
        currentUrl = target
        isLoading = true
        loadingProgress = 15
        webView?.loadUrl(target)
    }

    fun goBack(): Boolean {
        if (isFullscreenVideo) {
            exitFullscreenVideo()
            return true
        }
        if (webView?.canGoBack() == true) {
            webView?.goBack()
            return true
        }
        return false
    }

    fun goForward() {
        if (webView?.canGoForward() == true) {
            webView?.goForward()
        }
    }

    fun reload() {
        isLoading = true
        loadingProgress = 10
        webView?.reload()
    }

    fun goHome() {
        loadUrl("https://streamimdb.ru/")
    }

    fun setZoom(percent: Int) {
        textZoom = percent
        webView?.settings?.textZoom = percent
    }

    fun playPauseVideo() {
        val js = """
            (function() {
                var videos = document.querySelectorAll('video');
                if (videos.length > 0) {
                    for (var i = 0; i < videos.length; i++) {
                        if (videos[i].paused) {
                            videos[i].play();
                        } else {
                            videos[i].pause();
                        }
                    }
                    return;
                }
                var iframes = document.querySelectorAll('iframe');
                for (var j = 0; j < iframes.length; j++) {
                    try {
                        var doc = iframes[j].contentWindow.document;
                        var v = doc.querySelector('video');
                        if (v) {
                            if (v.paused) v.play(); else v.pause();
                        }
                    } catch(e) {}
                }
            })();
        """.trimIndent()
        webView?.evaluateJavascript(js, null)
    }

    fun seekRelative(seconds: Int) {
        val js = """
            (function() {
                var videos = document.querySelectorAll('video');
                for (var i = 0; i < videos.length; i++) {
                    videos[i].currentTime += $seconds;
                }
                var iframes = document.querySelectorAll('iframe');
                for (var j = 0; j < iframes.length; j++) {
                    try {
                        var doc = iframes[j].contentWindow.document;
                        var v = doc.querySelector('video');
                        if (v) {
                            v.currentTime += $seconds;
                        }
                    } catch(e) {}
                }
            })();
        """.trimIndent()
        webView?.evaluateJavascript(js, null)
    }

    fun toggleFullscreenVideo() {
        val js = """
            (function() {
                var v = document.querySelector('video');
                if (v) {
                    if (document.fullscreenElement) {
                        document.exitFullscreen();
                    } else if (v.requestFullscreen) {
                        v.requestFullscreen();
                    } else if (v.webkitRequestFullscreen) {
                        v.webkitRequestFullscreen();
                    }
                }
            })();
        """.trimIndent()
        webView?.evaluateJavascript(js, null)
    }

    fun exitFullscreenVideo() {
        customViewCallback?.onCustomViewHidden()
        customVideoView = null
        customViewCallback = null
        isFullscreenVideo = false
    }

    fun scrollBy(dx: Int, dy: Int) {
        webView?.scrollBy(dx, dy)
    }

    fun clickFocusedElement() {
        val js = """
            (function() {
                var el = document.activeElement;
                if (el && el !== document.body) {
                    el.click();
                }
            })();
        """.trimIndent()
        webView?.evaluateJavascript(js, null)
    }

    fun destroy() {
        try {
            webView?.let { wv ->
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.clearHistory()
                wv.removeAllViews()
                wv.destroy()
            }
        } catch (_: Exception) {}
        webView = null
    }
}

// Known ad and tracker host fragments commonly encountered on video streaming portals
private val BLOCKED_HOST_PATTERNS = listOf(
    "doubleclick.net", "adservice", "popads", "popcash", "propellerads",
    "adcash", "trafficjunky", "exoclick", "yadro.ru", "betwinner",
    "1xbet", "melbet", "adkeeper", "clickadu", "onclickmega", "syndication",
    "adserver", "advert", "directrev", "mgid", "zeroredirect"
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StreamTvWebView(
    modifier: Modifier = Modifier,
    controller: StreamWebController,
    onPageFinished: (title: String, url: String) -> Unit = { _, _ -> },
    fullscreenContainer: FrameLayout
) {
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    controller.webView = this

                    // In virtualized cloud / emulator environments lacking DRM rendernodes (/dev/dri/renderD128),
                    // setting the software layer suppresses MESA rendernode errors and prevents renderer crashes.
                    try {
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    } catch (_: Exception) {}

                    // Focus handling setup for TV remote D-Pad navigation
                    isFocusable = true
                    isFocusableInTouchMode = true
                    isClickable = true
                    requestFocus()

                    with(settings) {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        setSupportZoom(true)
                        textZoom = controller.textZoom
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        allowFileAccess = true
                        allowContentAccess = true

                        // Modern TV / Desktop User Agent so the site serves full-fledged player and layout
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 SmartTV"

                        // Enable Android WebView TV spatial navigation
                        try {
                            val method = javaClass.getMethod("setSpatialNavigationEnabled", Boolean::class.javaPrimitiveType)
                            method.invoke(this, true)
                        } catch (_: Exception) {}
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            controller.loadingProgress = newProgress
                            controller.isLoading = newProgress < 100
                            if (newProgress >= 100) {
                                controller.isInitialLoading = false
                            }
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            if (!title.isNullOrBlank()) {
                                controller.currentTitle = title
                            }
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (view == null) return
                            controller.customVideoView = view
                            controller.customViewCallback = callback
                            controller.isFullscreenVideo = true

                            fullscreenContainer.removeAllViews()
                            fullscreenContainer.addView(
                                view,
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            )
                            fullscreenContainer.visibility = View.VISIBLE
                        }

                        override fun onHideCustomView() {
                            fullscreenContainer.removeAllViews()
                            fullscreenContainer.visibility = View.GONE
                            controller.exitFullscreenVideo()
                        }

                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            controller.isLoading = true
                            if (url != null) controller.currentUrl = url
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            controller.isLoading = false
                            controller.isInitialLoading = false
                            val effectiveUrl = url ?: controller.currentUrl
                            controller.currentUrl = effectiveUrl
                            val title = view?.title ?: "StreamIMDb TV"
                            controller.currentTitle = title
                            onPageFinished(title, effectiveUrl)

                            // Inject high-contrast TV focus highlight styling and auto-tabindex for D-Pad navigation
                            val tvFocusAndCleanCss = """
                                (function() {
                                    var style = document.getElementById('tv-focus-style');
                                    if (!style) {
                                        style = document.createElement('style');
                                        style.id = 'tv-focus-style';
                                        style.innerHTML = `
                                            /* High-contrast TV Remote D-Pad Focus Highlight */
                                            :focus, :focus-visible, a:focus, button:focus, input:focus, select:focus, textarea:focus, [tabindex]:focus, [role="button"]:focus, .card:focus, .poster:focus, .movie:focus, .film:focus, .item:focus, video:focus {
                                                outline: 4px solid #F5C518 !important;
                                                outline-offset: 4px !important;
                                                box-shadow: 0 0 20px rgba(245, 197, 24, 0.95), 0 0 35px rgba(229, 9, 20, 0.6) !important;
                                                transform: scale(1.05) !important;
                                                transition: transform 0.15s ease, outline 0.15s ease, box-shadow 0.15s ease !important;
                                                z-index: 99999 !important;
                                                border-radius: 6px !important;
                                            }
                                            
                                            /* Ad cleanup */
                                            .ad-banner, .popup-banner, [id*="banner"], [class*="banner-ad"] { display: none !important; }
                                            body { overflow-x: hidden !important; }
                                        `;
                                        document.head.appendChild(style);
                                    }

                                    // Ensure all selectable movie elements have tabindex for remote D-Pad navigation
                                    function makeElementsFocusable() {
                                        var selectors = 'a, button, input, select, textarea, [role="button"], video, iframe, .card, .poster, .movie, .film, .item';
                                        document.querySelectorAll(selectors).forEach(function(el) {
                                            if (!el.hasAttribute('tabindex')) {
                                                el.setAttribute('tabindex', '0');
                                            }
                                        });
                                    }
                                    makeElementsFocusable();
                                    
                                    // Observe dynamic DOM changes for newly rendered movie cards
                                    try {
                                        var obs = new MutationObserver(makeElementsFocusable);
                                        obs.observe(document.body, { childList: true, subtree: true });
                                    } catch(e) {}

                                    // Guard WebGL context creation to prevent Mesa driver rendernode crashes
                                    try {
                                        if (window.HTMLCanvasElement) {
                                            var originalGetContext = HTMLCanvasElement.prototype.getContext;
                                            HTMLCanvasElement.prototype.getContext = function(type, attributes) {
                                                try {
                                                    return originalGetContext.apply(this, arguments);
                                                } catch(err) {
                                                    console.warn('Canvas context fallback:', err);
                                                    return null;
                                                }
                                            };
                                        }
                                    } catch(e) {}
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(tvFocusAndCleanCss, null)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val uri = request?.url ?: return false
                            val host = uri.host?.lowercase() ?: return false

                            // Check if URL is an intrusive ad network
                            if (BLOCKED_HOST_PATTERNS.any { pattern -> host.contains(pattern) }) {
                                controller.blockedAdsCount++
                                return true // Suppress ad redirect
                            }

                            // Allow legitimate navigation
                            if (host.contains("streamimdb") ||
                                host.contains("vidapi") ||
                                host.contains("vidsrc") ||
                                host.contains("imdb.com") ||
                                host.contains("tmdb.org") ||
                                host.contains("google.com") ||
                                host.contains("cloudflare.com")
                            ) {
                                return false
                            }

                            return false
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val host = request?.url?.host?.lowercase() ?: ""
                            if (BLOCKED_HOST_PATTERNS.any { host.contains(it) }) {
                                controller.blockedAdsCount++
                                return WebResourceResponse("text/plain", "UTF-8", null)
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: RenderProcessGoneDetail?
                        ): Boolean {
                            view?.let { wv ->
                                (wv.parent as? ViewGroup)?.removeView(wv)
                                wv.destroy()
                            }
                            return true
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                controller.isLoading = false
                            }
                        }
                    }

                    loadUrl(controller.currentUrl)
                }
            },
            update = { webView ->
                controller.webView = webView
            }
        )

        // Material Design Progress Indicator Overlay for Initial Page Loading
        AnimatedVisibility(
            visible = controller.isInitialLoading || (controller.isLoading && controller.loadingProgress < 85),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CinemaDarkBackground.copy(alpha = 0.88f))
                    .testTag("initial_loading_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .padding(24.dp)
                        .shadow(24.dp, shape = RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ImdbGold.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(28.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Cinema Logo Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CinemaRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Material Design 3 Circular Progress Indicator
                        CircularProgressIndicator(
                            progress = { (controller.loadingProgress.coerceIn(5, 100)) / 100f },
                            modifier = Modifier
                                .size(54.dp)
                                .testTag("material_loading_progress_indicator"),
                            color = ImdbGold,
                            trackColor = CinemaSurfaceVariant,
                            strokeWidth = 5.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Loading StreamIMDb",
                            color = CinemaTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Preparing movie streams & D-Pad navigation…",
                            color = CinemaTextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Linear progress line with percentage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LinearProgressIndicator(
                                progress = { (controller.loadingProgress.coerceIn(5, 100)) / 100f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = ImdbGold,
                                trackColor = CinemaSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${controller.loadingProgress}%",
                                color = ImdbGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
