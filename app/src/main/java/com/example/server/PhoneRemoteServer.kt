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
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
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
}

class PhoneRemoteServer(
    private val context: Context,
    private val port: Int = 8088,
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
                serverSocket = ServerSocket(port)
                Log.d("PhoneRemoteServer", "Server started on port $port")
                while (isActive && serverSocket?.isClosed == false) {
                    val client = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClient(client)
                    }
                }
            } catch (e: Exception) {
                Log.e("PhoneRemoteServer", "Server error", e)
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
        return "http://${getLocalIpAddress()}:$port"
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
                } else if (method == "POST" && path == "/api/key") {
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val key = json?.optString("key") ?: ""
                    mainHandler.post { listener.onDpadKey(key) }
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
    padding: 16px;
    min-height: 100vh;
    user-select: none;
  }
  header {
    width: 100%;
    max-width: 420px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #21262d;
  }
  .brand { display: flex; align-items: center; gap: 8px; font-weight: 700; font-size: 1.1rem; }
  .badge { background: #f5c518; color: #000; font-size: 0.75rem; padding: 2px 6px; border-radius: 4px; font-weight: 800; }
  .status { font-size: 0.8rem; color: #3fb950; display: flex; align-items: center; gap: 4px; }
  .dot { width: 8px; height: 8px; background: #3fb950; border-radius: 50%; }

  .tabs {
    display: flex;
    width: 100%;
    max-width: 420px;
    background: #161b22;
    border-radius: 12px;
    padding: 4px;
    margin-bottom: 12px;
  }
  .tab-btn {
    flex: 1;
    padding: 10px;
    background: transparent;
    border: none;
    color: #8b949e;
    font-weight: 600;
    font-size: 0.9rem;
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

  /* D-Pad Controller */
  .dpad-container {
    position: relative;
    width: 260px;
    height: 260px;
    background: #161b22;
    border-radius: 50%;
    box-shadow: 0 8px 24px rgba(0,0,0,0.6), inset 0 2px 4px rgba(255,255,255,0.05);
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 16px 0;
  }
  .dpad-btn {
    position: absolute;
    background: #21262d;
    border: 1px solid #30363d;
    color: #f0f6fc;
    font-size: 1.4rem;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: background 0.1s, transform 0.1s;
  }
  .dpad-btn:active { background: #e50914; transform: scale(0.92); }
  .dpad-up { top: 12px; left: 85px; width: 90px; height: 70px; border-radius: 45px 45px 12px 12px; }
  .dpad-down { bottom: 12px; left: 85px; width: 90px; height: 70px; border-radius: 12px 12px 45px 45px; }
  .dpad-left { left: 12px; top: 85px; width: 70px; height: 90px; border-radius: 45px 12px 12px 45px; }
  .dpad-right { right: 12px; top: 85px; width: 70px; height: 90px; border-radius: 12px 45px 45px 12px; }
  .dpad-center {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: #f5c518;
    color: #000;
    font-weight: 800;
    font-size: 1.1rem;
    z-index: 10;
    border: none;
    box-shadow: 0 4px 12px rgba(245,197,24,0.4);
  }
  .dpad-center:active { transform: scale(0.9); background: #d4a713; }

  /* Trackpad */
  .trackpad-box {
    width: 100%;
    height: 280px;
    background: #161b22;
    border: 2px dashed #30363d;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #8b949e;
    font-size: 0.95rem;
    touch-action: none;
    margin: 12px 0;
    position: relative;
  }
  .trackpad-box:active { border-color: #f5c518; }

  /* Media & Quick Buttons */
  .btn-row {
    display: flex;
    width: 100%;
    gap: 10px;
    margin-bottom: 12px;
    justify-content: center;
  }
  .control-btn {
    flex: 1;
    padding: 14px 10px;
    background: #21262d;
    border: 1px solid #30363d;
    border-radius: 10px;
    color: #f0f6fc;
    font-size: 1rem;
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
    border-radius: 14px;
    padding: 14px;
    margin-bottom: 12px;
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
  <div class="brand">
    <span>🎬 StreamIMDb</span>
    <span class="badge">TV REMOTE</span>
  </div>
  <div class="status">
    <div class="dot"></div>
    <span>TV Connected</span>
  </div>
</header>

<div class="tabs">
  <button class="tab-btn active" onclick="switchTab('dpad')">D-Pad</button>
  <button class="tab-btn" onclick="switchTab('trackpad')">Trackpad</button>
  <button class="tab-btn" onclick="switchTab('search')">Search / ID</button>
</div>

<!-- D-Pad Panel -->
<div id="dpad-panel" class="panel active">
  <div class="dpad-container">
    <button class="dpad-btn dpad-up" onclick="sendKey('up')">▲</button>
    <button class="dpad-btn dpad-left" onclick="sendKey('left')">◀</button>
    <button class="dpad-btn dpad-right" onclick="sendKey('right')">▶</button>
    <button class="dpad-btn dpad-down" onclick="sendKey('down')">▼</button>
    <button class="dpad-center" onclick="sendKey('enter')">OK</button>
  </div>

  <div class="btn-row">
    <button class="control-btn" onclick="sendKey('back')">↩ Back</button>
    <button class="control-btn" onclick="sendKey('home')">⌂ Home</button>
    <button class="control-btn" onclick="sendKey('menu')">☰ Menu</button>
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
      <span class="chip" onclick="quickWatch('tt1375666', 'Inception')">Inception</span>
      <span class="chip" onclick="quickWatch('tt0816692', 'Interstellar')">Interstellar</span>
      <span class="chip" onclick="quickWatch('tt0468569', 'The Dark Knight')">Dark Knight</span>
      <span class="chip" onclick="quickWatch('tt15398776', 'Oppenheimer')">Oppenheimer</span>
      <span class="chip" onclick="quickWatch('tt0499549', 'Avatar')">Avatar</span>
      <span class="chip" onclick="quickWatch('tt1877830', 'The Batman')">The Batman</span>
    </div>
  </div>
</div>

<!-- Common Media Controls -->
<div style="width: 100%; max-width: 420px; margin-top: 8px;">
  <div class="btn-row">
    <button class="control-btn" onclick="sendKey('rewind')">⏪ -10s</button>
    <button class="control-btn gold" onclick="sendKey('play')">⏯ Play / Pause</button>
    <button class="control-btn" onclick="sendKey('forward')">+10s ⏩</button>
  </div>
  <div class="btn-row">
    <button class="control-btn" onclick="sendKey('fullscreen')">⛶ Fullscreen</button>
    <button class="control-btn" onclick="sendKey('zoom_in')">🔍+ Zoom In</button>
    <button class="control-btn" onclick="sendKey('zoom_out')">🔍- Zoom Out</button>
  </div>
</div>

<script>
function sendKey(k) {
  if (navigator.vibrate) navigator.vibrate(20);
  fetch('/api/key', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ key: k })
  });
}

function sendMouseClick() {
  if (navigator.vibrate) navigator.vibrate(25);
  fetch('/api/mouse', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action: 'click' })
  });
}

function sendScroll(delta) {
  fetch('/api/mouse', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action: 'scroll', dy: delta })
  });
}

function sendMovie() {
  var input = document.getElementById('movie-input').value.trim();
  if (!input) return;
  if (navigator.vibrate) navigator.vibrate(30);
  if (input.toLowerCase().startsWith('tt') || input.toLowerCase().startsWith('http')) {
    var url = input.startsWith('http') ? input : ('https://streamimdb.ru/movie/' + input);
    fetch('/api/load', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ url: url })
    });
  } else {
    fetch('/api/load', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ search: input })
    });
  }
  document.getElementById('movie-input').value = '';
}

function quickWatch(imdbId, title) {
  if (navigator.vibrate) navigator.vibrate(25);
  fetch('/api/load', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ url: 'https://streamimdb.ru/movie/' + imdbId })
  });
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
      fetch('/api/mouse', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'move', dx: dx, dy: dy })
      });
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
