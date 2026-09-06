package com.example.tv

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class StreamWebController(val context: Context) {
    var webView: WebView? = null
    var isLoading by mutableStateOf(true)
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
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                controller.webView = this

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
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        controller.isLoading = newProgress < 100
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
                        val effectiveUrl = url ?: controller.currentUrl
                        controller.currentUrl = effectiveUrl
                        val title = view?.title ?: "StreamIMDb TV"
                        controller.currentTitle = title
                        onPageFinished(title, effectiveUrl)

                        // Auto-inject CSS to hide annoying banner overlays and maximize player on TV
                        val cleanupJs = """
                            (function() {
                                var style = document.createElement('style');
                                style.innerHTML = `
                                    .ad-banner, .popup-banner, [id*="banner"], [class*="banner-ad"] { display: none !important; }
                                    body { overflow-x: hidden !important; }
                                `;
                                document.head.appendChild(style);
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(cleanupJs, null)
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

                        // Allow default navigation if not obviously an ad
                        return false
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val host = request?.url?.host?.lowercase() ?: ""
                        if (BLOCKED_HOST_PATTERNS.any { host.contains(it) }) {
                            controller.blockedAdsCount++
                            // Return empty response for blocked ad scripts
                            return WebResourceResponse("text/plain", "UTF-8", null)
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }

                loadUrl(controller.currentUrl)
            }
        },
        update = { webView ->
            controller.webView = webView
        }
    )
}
