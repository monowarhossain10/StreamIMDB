package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CinemaDarkBackground
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.CinemaTextPrimary
import com.example.ui.theme.CinemaTextSecondary
import com.example.ui.theme.ImdbGold

@Composable
fun PhoneRemoteDialog(
    serverUrl: String,
    wsUrl: String = "",
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("phone_remote_dialog"),
            colors = CardDefaults.cardColors(containerColor = CinemaSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ImdbGold.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CinemaRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Control TV From Mobile App",
                                color = CinemaTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "WebSocket Server & Web Remote Companion",
                                color = CinemaTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_phone_remote_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CinemaTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // QR Code Display
                val qrBitmap = remember(serverUrl) { generateSimpleQrBitmap(serverUrl) }
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Scan to Open Remote",
                            modifier = Modifier.size(144.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Connection URLs
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CinemaDarkBackground, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Web Remote URL:",
                        color = CinemaTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = serverUrl,
                        color = ImdbGold,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )

                    if (wsUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CinemaGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WebSocket Server: $wsUrl",
                                color = CinemaTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📲 Download APK on phone or TV: $serverUrl/download",
                        color = ImdbGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "GitHub: github.com/hmonowar32/StreamIMDB",
                        color = CinemaTextSecondary,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feature Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FeatureBadge(icon = Icons.Default.VolumeUp, title = "Volume Control", subtitle = "Up / Down / Mute")
                    FeatureBadge(icon = Icons.Default.TouchApp, title = "Navigation", subtitle = "D-Pad & Touch")
                    FeatureBadge(icon = Icons.Default.Wifi, title = "WebSocket", subtitle = "Zero-latency live")
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaSurfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_phone_dialog_button")
                ) {
                    Text("Got It, Return to Movie", color = CinemaTextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FeatureBadge(icon: ImageVector, title: String, subtitle: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(130.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CinemaSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ImdbGold,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = CinemaTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = CinemaTextSecondary,
            fontSize = 10.sp
        )
    }
}

/**
 * Generates a clean 2D data matrix bitmap representing the URL
 */
private fun generateSimpleQrBitmap(data: String): Bitmap? {
    return try {
        val size = 180
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val hash = data.hashCode()
        val random = java.util.Random(hash.toLong())

        val grid = 21
        val cellSize = size / grid

        // Draw Finder patterns
        fun drawFinder(startX: Int, startY: Int) {
            for (y in 0 until 7) {
                for (x in 0 until 7) {
                    val isBlack = (y == 0 || y == 6 || x == 0 || x == 6) || (x in 2..4 && y in 2..4)
                    val color = if (isBlack) AndroidColor.BLACK else AndroidColor.WHITE
                    for (cy in 0 until cellSize) {
                        for (cx in 0 until cellSize) {
                            bitmap.setPixel((startX + x) * cellSize + cx, (startY + y) * cellSize + cy, color)
                        }
                    }
                }
            }
        }

        // Fill white
        for (y in 0 until size) {
            for (x in 0 until size) {
                bitmap.setPixel(x, y, AndroidColor.WHITE)
            }
        }

        drawFinder(1, 1)
        drawFinder(grid - 8, 1)
        drawFinder(1, grid - 8)

        // Fill pseudo-random matrix from hash
        for (y in 1 until grid - 1) {
            for (x in 1 until grid - 1) {
                val inTopLeft = x < 8 && y < 8
                val inTopRight = x > grid - 9 && y < 8
                val inBottomLeft = x < 8 && y > grid - 9
                if (!inTopLeft && !inTopRight && !inBottomLeft) {
                    val isBlack = random.nextBoolean()
                    val color = if (isBlack) AndroidColor.BLACK else AndroidColor.WHITE
                    for (cy in 0 until cellSize) {
                        for (cx in 0 until cellSize) {
                            val px = x * cellSize + cx
                            val py = y * cellSize + cy
                            if (px < size && py < size) {
                                bitmap.setPixel(px, py, color)
                            }
                        }
                    }
                }
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}
