package com.example.tv

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class RemoteMode {
    CURSOR, DPAD
}

enum class CursorSpeed(val multiplier: Float, val label: String) {
    SLOW(1.0f, "1x"),
    NORMAL(2.0f, "2x"),
    FAST(3.5f, "3.5x")
}

class VirtualCursorController {

    var isCursorMode by mutableStateOf(true)
    var cursorX by mutableFloatStateOf(640f)
    var cursorY by mutableFloatStateOf(360f)
    var isClicking by mutableStateOf(false)
    var screenWidth by mutableFloatStateOf(1920f)
    var screenHeight by mutableFloatStateOf(1080f)
    var cursorSpeed by mutableStateOf(CursorSpeed.NORMAL)

    fun updateScreenSize(w: Float, h: Float) {
        screenWidth = w.coerceAtLeast(100f)
        screenHeight = h.coerceAtLeast(100f)
        if (cursorX > screenWidth) cursorX = screenWidth / 2
        if (cursorY > screenHeight) cursorY = screenHeight / 2
    }

    fun move(dx: Float, dy: Float, targetView: View? = null) {
        val speed = cursorSpeed.multiplier
        cursorX = (cursorX + dx * speed).coerceIn(10f, screenWidth - 10f)
        cursorY = (cursorY + dy * speed).coerceIn(10f, screenHeight - 10f)

        // Edge scrolling
        if (targetView != null) {
            val edgeMargin = 70f
            if (cursorY < edgeMargin) {
                targetView.scrollBy(0, -25)
            } else if (cursorY > screenHeight - edgeMargin) {
                targetView.scrollBy(0, 25)
            }
        }
    }

    fun cycleSpeed() {
        cursorSpeed = when (cursorSpeed) {
            CursorSpeed.SLOW -> CursorSpeed.NORMAL
            CursorSpeed.NORMAL -> CursorSpeed.FAST
            CursorSpeed.FAST -> CursorSpeed.SLOW
        }
    }

    fun toggleMode() {
        isCursorMode = !isCursorMode
    }

    fun performClick(targetView: View) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        val downEvent = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_DOWN,
            cursorX,
            cursorY,
            0
        )
        val upEvent = MotionEvent.obtain(
            downTime,
            eventTime + 50,
            MotionEvent.ACTION_UP,
            cursorX,
            cursorY,
            0
        )

        isClicking = true
        targetView.dispatchTouchEvent(downEvent)
        targetView.postDelayed({
            targetView.dispatchTouchEvent(upEvent)
            downEvent.recycle()
            upEvent.recycle()
            isClicking = false
        }, 50)
    }
}
