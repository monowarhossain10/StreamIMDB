package com.example.server

import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

interface WebSocketCommandListener {
    fun onPlaybackCommand(action: String)
    fun onVolumeCommand(action: String)
    fun onNavigationCommand(action: String, value: String?)
    fun onMouseCommand(action: String, dx: Float, dy: Float)
}

class WebSocketServer(
    val port: Int = 8089,
    private val listener: WebSocketCommandListener
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeClients = ConcurrentHashMap.newKeySet<Socket>()

    fun start() {
        if (serverJob != null) return
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.d("WebSocketServer", "WebSocket Server listening on port $port")
                while (isActive && serverSocket?.isClosed == false) {
                    val client = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClient(client)
                    }
                }
            } catch (e: Exception) {
                Log.e("WebSocketServer", "Server error", e)
            }
        }
    }

    fun stop() {
        try {
            for (client in activeClients) {
                runCatching { client.close() }
            }
            activeClients.clear()
            serverSocket?.close()
        } catch (_: Exception) {}
        serverJob?.cancel()
        serverJob = null
    }

    fun broadcast(message: String) {
        val bytes = buildTextFrame(message)
        for (client in activeClients) {
            try {
                client.getOutputStream().write(bytes)
                client.getOutputStream().flush()
            } catch (_: Exception) {
                activeClients.remove(client)
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            // 1. Perform WebSocket Handshake
            if (!performHandshake(input, output)) {
                socket.close()
                return
            }

            activeClients.add(socket)
            Log.d("WebSocketServer", "Client connected to WebSocket: ${socket.inetAddress}")

            // Send initial connection welcome state
            sendText(output, """{"type":"connected","message":"Connected to StreamIMDb Android TV WebSocket Server"}""")

            // 2. Read WebSocket Frames
            while (serverJob?.isActive == true && !socket.isClosed) {
                val message = readFrame(input) ?: break
                handleIncomingMessage(message, output)
            }
        } catch (e: Exception) {
            Log.d("WebSocketServer", "Client disconnected: ${e.message}")
        } finally {
            activeClients.remove(socket)
            runCatching { socket.close() }
        }
    }

    private fun performHandshake(input: InputStream, output: OutputStream): Boolean {
        val reader = java.io.BufferedReader(java.io.InputStreamReader(input))
        val firstLine = reader.readLine() ?: return false
        if (!firstLine.startsWith("GET")) return false

        var line = reader.readLine()
        var secKey: String? = null
        while (!line.isNullOrEmpty()) {
            val lower = line.lowercase()
            if (lower.startsWith("sec-websocket-key:")) {
                secKey = line.substring(18).trim()
            }
            line = reader.readLine()
        }

        if (secKey == null) return false

        // Calculate Accept Key (RFC 6455)
        val magicGuid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        val sha1 = MessageDigest.getInstance("SHA-1")
        val hash = sha1.digest((secKey + magicGuid).toByteArray(Charsets.UTF_8))
        val acceptKey = Base64.encodeToString(hash, Base64.NO_WRAP)

        val response = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"

        output.write(response.toByteArray(Charsets.UTF_8))
        output.flush()
        return true
    }

    private fun readFrame(input: InputStream): String? {
        val b0 = input.read()
        if (b0 == -1) return null

        val opcode = b0 and 0x0F
        if (opcode == 0x8) { // Connection close
            return null
        }

        val b1 = input.read()
        if (b1 == -1) return null

        val isMasked = (b1 and 0x80) != 0
        var payloadLen = (b1 and 0x7F).toLong()

        if (payloadLen == 126L) {
            val hi = input.read()
            val lo = input.read()
            if (hi == -1 || lo == -1) return null
            payloadLen = ((hi shl 8) or lo).toLong()
        } else if (payloadLen == 127L) {
            var len = 0L
            for (i in 0 until 8) {
                val b = input.read()
                if (b == -1) return null
                len = (len shl 8) or (b and 0xFF).toLong()
            }
            payloadLen = len
        }

        if (payloadLen > 1024 * 1024) return null // Max 1MB payload

        val mask = ByteArray(4)
        if (isMasked) {
            var readMask = 0
            while (readMask < 4) {
                val r = input.read(mask, readMask, 4 - readMask)
                if (r == -1) return null
                readMask += r
            }
        }

        val payload = ByteArray(payloadLen.toInt())
        var totalRead = 0
        while (totalRead < payload.size) {
            val r = input.read(payload, totalRead, payload.size - totalRead)
            if (r == -1) return null
            totalRead += r
        }

        if (isMasked) {
            for (i in payload.indices) {
                payload[i] = (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
            }
        }

        return String(payload, Charsets.UTF_8)
    }

    private fun sendText(output: OutputStream, text: String) {
        val frame = buildTextFrame(text)
        output.write(frame)
        output.flush()
    }

    private fun buildTextFrame(text: String): ByteArray {
        val payload = text.toByteArray(Charsets.UTF_8)
        val len = payload.size
        return when {
            len <= 125 -> {
                val frame = ByteArray(2 + len)
                frame[0] = 0x81.toByte()
                frame[1] = len.toByte()
                System.arraycopy(payload, 0, frame, 2, len)
                frame
            }
            len <= 65535 -> {
                val frame = ByteArray(4 + len)
                frame[0] = 0x81.toByte()
                frame[1] = 126.toByte()
                frame[2] = ((len shr 8) and 0xFF).toByte()
                frame[3] = (len and 0xFF).toByte()
                System.arraycopy(payload, 0, frame, 4, len)
                frame
            }
            else -> {
                val frame = ByteArray(10 + len)
                frame[0] = 0x81.toByte()
                frame[1] = 127.toByte()
                val longLen = len.toLong()
                for (i in 0 until 8) {
                    frame[2 + i] = ((longLen shr (56 - i * 8)) and 0xFF).toByte()
                }
                System.arraycopy(payload, 0, frame, 10, len)
                frame
            }
        }
    }

    private fun handleIncomingMessage(raw: String, output: OutputStream) {
        try {
            val json = JSONObject(raw)
            val type = json.optString("type")
            val command = json.optString("command")
            val action = json.optString("action").ifBlank { command }

            when (type.lowercase()) {
                "playback" -> {
                    mainHandler.post { listener.onPlaybackCommand(action) }
                    sendText(output, """{"type":"ack","category":"playback","action":"$action","status":"success"}""")
                }
                "volume" -> {
                    mainHandler.post { listener.onVolumeCommand(action) }
                    sendText(output, """{"type":"ack","category":"volume","action":"$action","status":"success"}""")
                }
                "navigation" -> {
                    val value = json.optString("value").takeIf { it.isNotBlank() }
                        ?: json.optString("url").takeIf { it.isNotBlank() }
                        ?: json.optString("query").takeIf { it.isNotBlank() }
                    mainHandler.post { listener.onNavigationCommand(action, value) }
                    sendText(output, """{"type":"ack","category":"navigation","action":"$action","status":"success"}""")
                }
                "mouse" -> {
                    val dx = json.optDouble("dx", 0.0).toFloat()
                    val dy = json.optDouble("dy", 0.0).toFloat()
                    mainHandler.post { listener.onMouseCommand(action, dx, dy) }
                }
                else -> {
                    // Check direct command keywords
                    when (command.lowercase()) {
                        "volume_up", "vol_up" -> {
                            mainHandler.post { listener.onVolumeCommand("up") }
                        }
                        "volume_down", "vol_down" -> {
                            mainHandler.post { listener.onVolumeCommand("down") }
                        }
                        "mute", "volume_mute" -> {
                            mainHandler.post { listener.onVolumeCommand("mute") }
                        }
                        "play", "pause", "play_pause", "toggle_play" -> {
                            mainHandler.post { listener.onPlaybackCommand("play_pause") }
                        }
                        "forward", "seek_forward", "fast_forward" -> {
                            mainHandler.post { listener.onPlaybackCommand("forward") }
                        }
                        "rewind", "seek_rewind" -> {
                            mainHandler.post { listener.onPlaybackCommand("rewind") }
                        }
                        "fullscreen" -> {
                            mainHandler.post { listener.onPlaybackCommand("fullscreen") }
                        }
                        "up", "down", "left", "right", "enter", "select", "back", "home" -> {
                            mainHandler.post { listener.onNavigationCommand(command, null) }
                        }
                        else -> {
                            if (action.isNotBlank()) {
                                mainHandler.post { listener.onNavigationCommand(action, json.optString("value")) }
                            }
                        }
                    }
                    sendText(output, """{"type":"ack","command":"${command.ifBlank { action }}","status":"received"}""")
                }
            }
        } catch (e: Exception) {
            // If message was plain text string instead of JSON
            val clean = raw.trim().lowercase()
            when (clean) {
                "vol_up", "volume_up" -> mainHandler.post { listener.onVolumeCommand("up") }
                "vol_down", "volume_down" -> mainHandler.post { listener.onVolumeCommand("down") }
                "mute" -> mainHandler.post { listener.onVolumeCommand("mute") }
                "play", "pause", "play_pause" -> mainHandler.post { listener.onPlaybackCommand("play_pause") }
                "forward" -> mainHandler.post { listener.onPlaybackCommand("forward") }
                "rewind" -> mainHandler.post { listener.onPlaybackCommand("rewind") }
                "fullscreen" -> mainHandler.post { listener.onPlaybackCommand("fullscreen") }
                "up", "down", "left", "right", "enter", "back", "home" -> mainHandler.post { listener.onNavigationCommand(clean, null) }
            }
            sendText(output, """{"type":"ack","raw":"$clean","status":"handled"}""")
        }
    }
}
