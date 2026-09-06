package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tv.VirtualCursorController
import com.example.ui.theme.CursorGlow
import com.example.ui.theme.ImdbGold
import kotlin.math.roundToInt

@Composable
fun VirtualCursorOverlay(
    controller: VirtualCursorController,
    modifier: Modifier = Modifier
) {
    if (!controller.isCursorMode) return

    val clickScale by animateFloatAsState(
        targetValue = if (controller.isClicking) 0.75f else 1.0f,
        animationSpec = spring(),
        label = "cursorClickScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("virtual_cursor_overlay")
    ) {
        // Neon Target Cursor Pointer
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (controller.cursorX - 24).roundToInt(),
                        (controller.cursorY - 24).roundToInt()
                    )
                }
                .size(48.dp)
                .scale(clickScale),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 3

                // Outer pulsing glow
                drawCircle(
                    color = CursorGlow,
                    radius = radius + 6f
                )

                // High-visibility golden TV ring
                drawCircle(
                    color = ImdbGold,
                    radius = radius,
                    style = Stroke(width = 3.5f)
                )

                // Center crosshair / precision pointer dot
                drawCircle(
                    color = Color.White,
                    radius = 4f
                )

                // Precision crosshairs
                drawLine(
                    color = ImdbGold,
                    start = Offset(center.x - radius - 5f, center.y),
                    end = Offset(center.x - radius + 5f, center.y),
                    strokeWidth = 2.5f
                )
                drawLine(
                    color = ImdbGold,
                    start = Offset(center.x + radius - 5f, center.y),
                    end = Offset(center.x + radius + 5f, center.y),
                    strokeWidth = 2.5f
                )
                drawLine(
                    color = ImdbGold,
                    start = Offset(center.x, center.y - radius - 5f),
                    end = Offset(center.x, center.y - radius + 5f),
                    strokeWidth = 2.5f
                )
                drawLine(
                    color = ImdbGold,
                    start = Offset(center.x, center.y + radius - 5f),
                    end = Offset(center.x, center.y + radius + 5f),
                    strokeWidth = 2.5f
                )
            }
        }
    }
}
