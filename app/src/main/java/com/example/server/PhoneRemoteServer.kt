package com.example.server

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

interface RemoteActionListener {
    fun onDpadKey(key: String)
    fun onMouseMove(dx: Float, dy: Float)
    fun onMouseClick()
    fun onMouseScroll(deltaY: Float)
    fun onLoadUrl(url: String)
    fun onSearchQuery(query: String)
    fun onVolumeAction(action: String) // "up", "down", "mute"
    fun onPlaybackAction(action: String) // "play", "pause", "play_pause", "forward", "rewind", "fullscreen"
    fun onVoiceSearchAction()
}

class PhoneRemoteServer(
    private val context: Context,
    private val httpPort: Int = 8088,
    private val wsPort: Int = 8089,
    private val listener: RemoteActionListener
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun start() {
        if (serverJob != null) return
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(httpPort))
                }
                Log.d("PhoneRemoteServer", "HTTP Server started on port $httpPort")
                while (isActive && serverSocket?.isClosed == false) {
                    val client = try {
                        serverSocket?.accept()
                    } catch (_: Exception) {
                        null
                    } ?: break
                    launch(Dispatchers.IO) {
                        handleClient(client)
                    }
                }
            } catch (e: Exception) {
                Log.w("PhoneRemoteServer", "HTTP server stopped or port unavailable: ${e.message}")
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverJob?.cancel()
        serverJob = null
    }

    fun getLocalIpAddress(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                val ipInt = wifiManager.connectionInfo.ipAddress
                if (ipInt != 0) {
                    return String.format(
                        "%d.%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff,
                        ipInt shr 24 and 0xff
                    )
                }
            }

            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr.hostAddress.indexOf(':') < 0) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    fun getServerUrl(): String {
        return "http://${getLocalIpAddress()}:$httpPort"
    }

    fun getWebSocketUrl(): String {
        return "ws://${getLocalIpAddress()}:$wsPort"
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val out = s.getOutputStream()

                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0]
                val path = parts[1]

                // Read headers
                var contentLength = 0
                var line = reader.readLine()
                while (!line.isNullOrEmpty()) {
                    if (line.lowercase().startsWith("content-length:")) {
                        contentLength = line.substring(15).trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine()
                }

                // Read body if POST
                val body = if (contentLength > 0) {
                    val charArray = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val count = reader.read(charArray, read, contentLength - read)
                        if (count == -1) break
                        read += count
                    }
                    String(charArray, 0, read)
                } else ""

                if (method == "GET" && (path == "/" || path.startsWith("/?"))) {
                    sendResponse(out, 200, "text/html; charset=UTF-8", getRemoteHtml())
                } else if (method == "GET" && (path == "/download" || path == "/apk" || path == "/StreamIMDb-TV.apk" || path == "/app-debug.apk")) {
                    sendApkResponse(out)
                } else if (method == "GET" && (path == "/logo.jpg" || path == "/logo.png" || path == "/favicon.ico")) {
                    sendLogoResponse(out)
                } else if (method == "POST" && path == "/api/key") {
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val key = json?.optString("key") ?: ""
                    mainHandler.post { listener.onDpadKey(key) }
                    sendResponse(out, 200, "application/json", """{"status":"ok"}""")
                } else if (method == "POST" && path == "/api/volume") {
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val action = json?.optString("action") ?: ""
                    mainHandler.post { listener.onVolumeAction(action) }
                    sendResponse(out, 200, "application/json", """{"status":"ok"}""")
                } else if (method == "POST" && path == "/api/playback") {
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val action = json?.optString("action") ?: ""
                    mainHandler.post { listener.onPlaybackAction(action) }
                    sendResponse(out, 200, "application/json", """{"status":"ok"}""")
                } else if (method == "POST" && path == "/api/voice") {
                    mainHandler.post { listener.onVoiceSearchAction() }
                    sendResponse(out, 200, "application/json", """{"status":"ok"}""")
                } else if (method == "POST" && path == "/api/mouse") {
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    if (json != null) {
                        val action = json.optString("action")
                        val dx = json.optDouble("dx", 0.0).toFloat()
                        val dy = json.optDouble("dy", 0.0).toFloat()
                        mainHandler.post {
                            when (action) {
                                "move" -> listener.onMouseMove(dx, dy)
                                "click" -> listener.onMouseClick()
                                "scroll" -> listener.onMouseScroll(dy)
                            }
                        }
                    }
                    sendResponse(out, 200, "application/json", """{"status":"ok"}""")
                } else if (method == "POST" && path == "/api/load") {
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val url = json?.optString("url") ?: ""
                    val search = json?.optString("search") ?: ""
                    mainHandler.post {
                        if (url.isNotBlank()) {
                            listener.onLoadUrl(url)
                        } else if (search.isNotBlank()) {
                            listener.onSearchQuery(search)
                        }
                    }
                    sendResponse(out, 200, "application/json", """{"status":"ok"}""")
                } else {
                    sendResponse(out, 404, "text/plain", "Not Found")
                }
            }
        } catch (_: Exception) {}
    }

    private fun sendResponse(out: OutputStream, code: Int, contentType: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val statusText = if (code == 200) "OK" else "Not Found"
        val header = "HTTP/1.1 $code $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun sendApkResponse(out: OutputStream) {
        val apkFile = File("/app/applet/public/StreamIMDb-TV.apk").takeIf { it.exists() }
            ?: File("public/StreamIMDb-TV.apk").takeIf { it.exists() }
            ?: File(".build-outputs/app-debug.apk").takeIf { it.exists() }
            ?: File("app/build/outputs/apk/debug/app-debug.apk").takeIf { it.exists() }
            ?: File(context.applicationInfo.sourceDir).takeIf { it.exists() }

        if (apkFile != null && apkFile.exists()) {
            val length = apkFile.length()
            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/vnd.android.package-archive\r\n" +
                    "Content-Disposition: attachment; filename=\"StreamIMDb-TV.apk\"\r\n" +
                    "Content-Length: $length\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(header.toByteArray(Charsets.UTF_8))
            apkFile.inputStream().use { input ->
                input.copyTo(out)
            }
            out.flush()
        } else {
            sendResponse(out, 404, "text/plain", "APK download currently not available.")
        }
    }

    private fun sendLogoResponse(out: OutputStream) {
        val logoFile = File("/app/applet/app/src/main/res/drawable/ic_streamimdb_logo.jpg").takeIf { it.exists() }
            ?: File("app/src/main/res/drawable/ic_streamimdb_logo.jpg").takeIf { it.exists() }
            ?: File("public/streamimdb_logo.jpg").takeIf { it.exists() }

        if (logoFile != null && logoFile.exists()) {
            val length = logoFile.length()
            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: image/jpeg\r\n" +
                    "Content-Length: $length\r\n" +
                    "Cache-Control: public, max-age=86400\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(header.toByteArray(Charsets.UTF_8))
            logoFile.inputStream().use { input ->
                input.copyTo(out)
            }
            out.flush()
        } else {
            sendResponse(out, 404, "text/plain", "Logo not found")
        }
    }

    private fun getRemoteHtml(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>StreamIMDb TV Remote</title>
<style>
  * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; margin: 0; padding: 0; }
  body {
    background: #0d1117;
    color: #f0f6fc;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 14px;
    min-height: 100vh;
    user-select: none;
  }
  header {
    width: 100%;
    max-width: 420px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
    padding-bottom: 8px;
    border-bottom: 1px solid #21262d;
  }
  .brand { display: flex; align-items: center; gap: 8px; font-weight: 700; font-size: 1.05rem; }
  .badge { background: #f5c518; color: #000; font-size: 0.72rem; padding: 2px 6px; border-radius: 4px; font-weight: 800; }
  .status-badge {
    font-size: 0.75rem;
    padding: 4px 8px;
    border-radius: 12px;
    background: #1f2937;
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
  }
  .dot { width: 8px; height: 8px; border-radius: 50%; background: #3fb950; }
  .dot.connecting { background: #f5c518; }
  .dot.offline { background: #f85149; }

  .tabs {
    display: flex;
    width: 100%;
    max-width: 420px;
    background: #161b22;
    border-radius: 10px;
    padding: 3px;
    margin-bottom: 10px;
  }
  .tab-btn {
    flex: 1;
    padding: 9px;
    background: transparent;
    border: none;
    color: #8b949e;
    font-weight: 600;
    font-size: 0.88rem;
    border-radius: 8px;
    cursor: pointer;
  }
  .tab-btn.active {
    background: #21262d;
    color: #f0f6fc;
    box-shadow: 0 2px 8px rgba(0,0,0,0.4);
  }

  .panel { display: none; width: 100%; max-width: 420px; flex-direction: column; align-items: center; }
  .panel.active { display: flex; }

  /* Volume Controller Section */
  .volume-card {
    width: 100%;
    max-width: 420px;
    background: #161b22;
    border: 1px solid #21262d;
    border-radius: 12px;
    padding: 10px 14px;
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .volume-label { font-size: 0.85rem; font-weight: 700; color: #8b949e; display: flex; align-items: center; gap: 6px; }
  .volume-btn-group { display: flex; gap: 8px; }
  .vol-btn {
    background: #21262d;
    border: 1px solid #30363d;
    color: #f0f6fc;
    padding: 8px 14px;
    border-radius: 8px;
    font-weight: 700;
    font-size: 0.95rem;
    cursor: pointer;
    transition: background 0.1s;
  }
  .vol-btn:active { background: #e50914; }

  /* D-Pad Controller */
  .dpad-container {
    position: relative;
    width: 250px;
    height: 250px;
    background: #161b22;
    border-radius: 50%;
    box-shadow: 0 8px 24px rgba(0,0,0,0.6), inset 0 2px 4px rgba(255,255,255,0.05);
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 10px 0;
  }
  .dpad-btn {
    position: absolute;
    background: #21262d;
    border: 1px solid #30363d;
    color: #f0f6fc;
    font-size: 1.3rem;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: background 0.1s, transform 0.1s;
  }
  .dpad-btn:active { background: #e50914; transform: scale(0.92); }
  .dpad-up { top: 10px; left: 80px; width: 90px; height: 68px; border-radius: 45px 45px 12px 12px; }
  .dpad-down { bottom: 10px; left: 80px; width: 90px; height: 68px; border-radius: 12px 12px 45px 45px; }
  .dpad-left { left: 10px; top: 80px; width: 68px; height: 90px; border-radius: 45px 12px 12px 45px; }
  .dpad-right { right: 10px; top: 80px; width: 68px; height: 90px; border-radius: 12px 45px 45px 12px; }
  .dpad-center {
    width: 76px;
    height: 76px;
    border-radius: 50%;
    background: #f5c518;
    color: #000;
    font-weight: 800;
    font-size: 1.05rem;
    z-index: 10;
    border: none;
    box-shadow: 0 4px 12px rgba(245,197,24,0.4);
  }
  .dpad-center:active { transform: scale(0.9); background: #d4a713; }

  /* Trackpad */
  .trackpad-box {
    width: 100%;
    height: 270px;
    background: #161b22;
    border: 2px dashed #30363d;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #8b949e;
    font-size: 0.92rem;
    touch-action: none;
    margin: 10px 0;
    position: relative;
  }
  .trackpad-box:active { border-color: #f5c518; }

  /* Media & Quick Buttons */
  .btn-row {
    display: flex;
    width: 100%;
    gap: 8px;
    margin-bottom: 10px;
    justify-content: center;
  }
  .control-btn {
    flex: 1;
    padding: 12px 8px;
    background: #21262d;
    border: 1px solid #30363d;
    border-radius: 8px;
    color: #f0f6fc;
    font-size: 0.95rem;
    font-weight: 600;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    transition: background 0.1s;
  }
  .control-btn:active { background: #e50914; }
  .control-btn.gold { background: #2f2a15; border-color: #f5c518; color: #f5c518; }
  .control-btn.gold:active { background: #f5c518; color: #000; }

  /* Search Card */
  .input-card {
    width: 100%;
    background: #161b22;
    border: 1px solid #21262d;
    border-radius: 12px;
    padding: 14px;
    margin-bottom: 10px;
  }
  .input-label { font-size: 0.85rem; color: #8b949e; margin-bottom: 8px; font-weight: 600; }
  .input-group { display: flex; gap: 8px; }
  .text-input {
    flex: 1;
    background: #0d1117;
    border: 1px solid #30363d;
    border-radius: 8px;
    padding: 10px 12px;
    color: #f0f6fc;
    font-size: 0.95rem;
    outline: none;
  }
  .text-input:focus { border-color: #f5c518; }
  .submit-btn {
    background: #e50914;
    border: none;
    border-radius: 8px;
    color: #fff;
    padding: 0 16px;
    font-weight: 700;
    cursor: pointer;
  }
  .submit-btn:active { background: #b81d24; }

  .quick-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 10px;
  }
  .chip {
    background: #21262d;
    border-radius: 16px;
    padding: 5px 10px;
    font-size: 0.78rem;
    color: #c9d1d9;
    border: 1px solid #30363d;
    cursor: pointer;
  }
  .chip:active { background: #f5c518; color: #000; }
</style>
</head>
<body>

<header>
  <div class="brand" style="display:flex; align-items:center; gap:10px;">
    <img src="/logo.jpg" alt="StreamIMDb" style="width:38px; height:38px; border-radius:8px; object-fit:cover; border:1px solid #30363d; box-shadow:0 2px 8px rgba(0,0,0,0.5);" />
    <div style="display:flex; align-items:center; gap:4px;">
      <span style="color:#ffffff; font-weight:900; font-size:1.1rem; letter-spacing:0.5px;">STREAM</span><span style="color:#f5c518; font-weight:900; font-size:1.1rem; letter-spacing:0.5px;">IMDB</span>
      <span class="badge" style="margin-left:4px;">TV</span>
    </div>
  </div>
  <div id="ws-status" class="status-badge">
    <div id="ws-dot" class="dot connecting"></div>
    <span id="ws-text">Connecting WebSocket...</span>
  </div>
</header>

<!-- TV Volume Control Bar -->
<div class="volume-card">
  <div class="volume-label">
    <span>🔊 TV Volume</span>
  </div>
  <div class="volume-btn-group">
    <button class="vol-btn" onclick="sendVolume('down')">Vol –</button>
    <button class="vol-btn" onclick="sendVolume('mute')">Mute 🔇</button>
    <button class="vol-btn" onclick="sendVolume('up')">Vol +</button>
  </div>
</div>

<div class="tabs">
  <button class="tab-btn active" onclick="switchTab('dpad')">D-Pad</button>
  <button class="tab-btn" onclick="switchTab('trackpad')">Trackpad</button>
  <button class="tab-btn" onclick="switchTab('search')">Search / ID</button>
</div>

<!-- D-Pad Panel -->
<div id="dpad-panel" class="panel active">
  <div class="dpad-container">
    <button class="dpad-btn dpad-up" onclick="sendNavigation('up')">▲</button>
    <button class="dpad-btn dpad-left" onclick="sendNavigation('left')">◀</button>
    <button class="dpad-btn dpad-right" onclick="sendNavigation('right')">▶</button>
    <button class="dpad-btn dpad-down" onclick="sendNavigation('down')">▼</button>
    <button class="dpad-center" onclick="sendNavigation('enter')">OK</button>
  </div>

  <div class="btn-row">
    <button class="control-btn" onclick="sendNavigation('back')">↩ Back</button>
    <button class="control-btn" onclick="sendNavigation('home')">⌂ Home</button>
    <button class="control-btn" onclick="sendNavigation('menu')">☰ Menu</button>
  </div>
</div>

<!-- Trackpad Panel -->
<div id="trackpad-panel" class="panel">
  <div id="trackpad" class="trackpad-box">
    <span>🖱️ Drag to Move Cursor on TV</span>
    <span style="font-size:0.8rem; margin-top:4px;">Tap to Click</span>
  </div>
  <div class="btn-row">
    <button class="control-btn gold" onclick="sendMouseClick()">Left Click (Tap)</button>
    <button class="control-btn" onclick="sendScroll(-100)">▲ Scroll Up</button>
    <button class="control-btn" onclick="sendScroll(100)">▼ Scroll Down</button>
  </div>
</div>

<!-- Search / IMDb ID Panel -->
<div id="search-panel" class="panel">
  <div class="input-card">
    <div class="input-label">Type Movie Title or IMDb ID (e.g. tt1375666)</div>
    <div class="input-group">
      <input id="movie-input" type="text" class="text-input" placeholder="e.g. Inception or tt0816692">
      <button class="submit-btn" onclick="sendMovie()">Watch</button>
    </div>
    <div class="quick-chips">
      <span class="chip" onclick="quickWatch('tt1375666')">Inception</span>
      <span class="chip" onclick="quickWatch('tt0816692')">Interstellar</span>
      <span class="chip" onclick="quickWatch('tt0468569')">Dark Knight</span>
      <span class="chip" onclick="quickWatch('tt15398776')">Oppenheimer</span>
      <span class="chip" onclick="quickWatch('tt0499549')">Avatar</span>
      <span class="chip" onclick="quickWatch('tt1877830')">The Batman</span>
    </div>
    <div style="margin-top: 10px;">
      <button class="control-btn red" style="width:100%; font-size:1rem; padding:10px; background: #e50914; color: #fff;" onclick="triggerVoiceSearch()">🎤 Speak with TV Remote / Voice Search</button>
    </div>
  </div>
</div>

<!-- Playback Controls -->
<div style="width: 100%; max-width: 420px; margin-top: 4px;">
  <div class="btn-row">
    <button class="control-btn" onclick="sendPlayback('rewind')">⏪ -10s</button>
    <button class="control-btn gold" onclick="sendPlayback('play_pause')">⏯ Play / Pause</button>
    <button class="control-btn" onclick="sendPlayback('forward')">+10s ⏩</button>
  </div>
  <div class="btn-row">
    <button class="control-btn" onclick="sendPlayback('fullscreen')">⛶ Fullscreen</button>
    <button class="control-btn" onclick="sendNavigation('zoom_in')">🔍+ Zoom</button>
    <button class="control-btn" onclick="sendNavigation('zoom_out')">🔍- Zoom</button>
  </div>

  <!-- Download APK & GitHub Links -->
  <div style="margin-top: 14px; background: #161b22; border: 1px solid #30363d; border-radius: 12px; padding: 12px; text-align: center;">
    <div style="font-size: 0.8rem; color: #8b949e; margin-bottom: 8px; font-weight: 600;">STREAMIMDB REPOSITORY & APP DOWNLOAD</div>
    <a href="/download" download="StreamIMDb-TV.apk" style="display: block; width: 100%; padding: 10px 0; background: #238636; color: #ffffff; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 0.95rem; margin-bottom: 8px;">
      📲 Download TV App (StreamIMDb-TV.apk)
    </a>
    <div style="display: flex; justify-content: center; gap: 16px; font-size: 0.8rem;">
      <a href="https://github.com/hmonowar32/StreamIMDB" target="_blank" style="color: #58a6ff; text-decoration: none;">★ GitHub Repository</a>
      <a href="/download" style="color: #f5c518; text-decoration: none;">Direct APK Link</a>
    </div>
  </div>
</div>

<script>
// Local Network WebSocket Connection
var ws = null;
var wsPort = $wsPort;
var wsConnected = false;

function initWebSocket() {
  var host = window.location.hostname || '127.0.0.1';
  var wsUrl = 'ws://' + host + ':' + wsPort;
  
  try {
    ws = new WebSocket(wsUrl);
    
    ws.onopen = function() {
      wsConnected = true;
      document.getElementById('ws-dot').className = 'dot';
      document.getElementById('ws-text').innerText = 'WebSocket Connected';
    };
    
    ws.onclose = function() {
      wsConnected = false;
      document.getElementById('ws-dot').className = 'dot offline';
      document.getElementById('ws-text').innerText = 'Reconnecting WS...';
      setTimeout(initWebSocket, 2000);
    };
    
    ws.onerror = function() {
      wsConnected = false;
    };
    
    ws.onmessage = function(event) {
      try {
        var msg = JSON.parse(event.data);
        console.log('WS Message:', msg);
      } catch(e) {}
    };
  } catch(e) {
    setTimeout(initWebSocket, 2000);
  }
}

initWebSocket();

function sendWsOrHttp(payload, httpUrl, httpBody) {
  if (navigator.vibrate) navigator.vibrate(20);
  if (wsConnected && ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(payload));
  } else {
    fetch(httpUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(httpBody)
    });
  }
}

function sendVolume(action) {
  sendWsOrHttp(
    { type: 'volume', action: action },
    '/api/volume',
    { action: action }
  );
}

function sendPlayback(action) {
  sendWsOrHttp(
    { type: 'playback', action: action },
    '/api/playback',
    { action: action }
  );
}

function sendNavigation(action) {
  sendWsOrHttp(
    { type: 'navigation', action: action },
    '/api/key',
    { key: action }
  );
}

function sendMouseClick() {
  sendWsOrHttp(
    { type: 'mouse', action: 'click' },
    '/api/mouse',
    { action: 'click' }
  );
}

function sendScroll(delta) {
  sendWsOrHttp(
    { type: 'mouse', action: 'scroll', dy: delta },
    '/api/mouse',
    { action: 'scroll', dy: delta }
  );
}

function sendMovie() {
  var input = document.getElementById('movie-input').value.trim();
  if (!input) return;
  if (input.toLowerCase().startsWith('tt') || input.toLowerCase().startsWith('http')) {
    var url = input.startsWith('http') ? input : ('https://streamimdb.ru/movie/' + input);
    sendWsOrHttp(
      { type: 'navigation', action: 'load_url', value: url },
      '/api/load',
      { url: url }
    );
  } else {
    sendWsOrHttp(
      { type: 'navigation', action: 'search', value: input },
      '/api/load',
      { search: input }
    );
  }
  document.getElementById('movie-input').value = '';
}

function quickWatch(imdbId) {
  var url = 'https://streamimdb.ru/movie/' + imdbId;
  sendWsOrHttp(
    { type: 'navigation', action: 'load_url', value: url },
    '/api/load',
    { url: url }
  );
}

function triggerVoiceSearch() {
  sendWsOrHttp(
    { type: 'navigation', action: 'voice_search' },
    '/api/voice',
    {}
  );
}

function switchTab(tab) {
  var buttons = document.querySelectorAll('.tab-btn');
  var panels = document.querySelectorAll('.panel');
  buttons.forEach(function(b) { b.classList.remove('active'); });
  panels.forEach(function(p) { p.classList.remove('active'); });

  if (tab === 'dpad') {
    buttons[0].classList.add('active');
    document.getElementById('dpad-panel').classList.add('active');
  } else if (tab === 'trackpad') {
    buttons[1].classList.add('active');
    document.getElementById('trackpad-panel').classList.add('active');
  } else if (tab === 'search') {
    buttons[2].classList.add('active');
    document.getElementById('search-panel').classList.add('active');
  }
}

// Trackpad Touch Handling
var tp = document.getElementById('trackpad');
var lastTouchX = null;
var lastTouchY = null;
var touchMoved = false;

tp.addEventListener('touchstart', function(e) {
  if (e.touches.length === 1) {
    lastTouchX = e.touches[0].clientX;
    lastTouchY = e.touches[0].clientY;
    touchMoved = false;
  }
}, { passive: false });

tp.addEventListener('touchmove', function(e) {
  if (e.touches.length === 1 && lastTouchX !== null) {
    e.preventDefault();
    var currentX = e.touches[0].clientX;
    var currentY = e.touches[0].clientY;
    var dx = (currentX - lastTouchX) * 2.2;
    var dy = (currentY - lastTouchY) * 2.2;
    if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
      touchMoved = true;
      sendWsOrHttp(
        { type: 'mouse', action: 'move', dx: dx, dy: dy },
        '/api/mouse',
        { action: 'move', dx: dx, dy: dy }
      );
      lastTouchX = currentX;
      lastTouchY = currentY;
    }
  }
}, { passive: false });

tp.addEventListener('touchend', function(e) {
  if (!touchMoved) {
    sendMouseClick();
  }
  lastTouchX = null;
  lastTouchY = null;
});
</script>
</body>
</html>
        """.trimIndent()
    }
}
