package com.example.daitezachet.engine

import android.graphics.RectF
import android.view.MotionEvent

/**
 * Three virtual buttons at the bottom of the screen:
 *   [  LEFT  ]  [  RIGHT  ]  [  JUMP  ]
 *
 * @param controlsTop  Y-coordinate where the control strip begins (typically screenH * 0.78)
 */
class InputHandler(
    private val screenW: Float,
    private val screenH: Float,
    controlsTop: Float = screenH * 0.78f
) {
    val btnLeft  = RectF(0f,             controlsTop, screenW * 0.30f, screenH)
    val btnRight = RectF(screenW * 0.35f, controlsTop, screenW * 0.65f, screenH)
    val btnJump  = RectF(screenW * 0.70f, controlsTop, screenW,          screenH)

    // Track which pointer IDs are currently down
    private val activePointers = mutableSetOf<Int>()

    fun onTouch(event: MotionEvent, engine: GameEngine): Boolean {
        val action = event.actionMasked
        val idx    = event.actionIndex
        val pid    = event.getPointerId(idx)

        when (action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                activePointers.add(pid)
                val x = event.getX(idx); val y = event.getY(idx)
                if (btnJump.contains(x, y)) engine.jumpRequested = true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> activePointers.remove(pid)
            MotionEvent.ACTION_CANCEL -> {
                activePointers.clear()
                engine.moveLeft  = false
                engine.moveRight = false
                return true
            }
        }

        // Recompute direction from all still-active pointers
        var left = false; var right = false
        for (i in 0 until event.pointerCount) {
            if (event.getPointerId(i) !in activePointers) continue
            val x = event.getX(i); val y = event.getY(i)
            if (btnLeft.contains(x, y))  left  = true
            if (btnRight.contains(x, y)) right = true
        }
        engine.moveLeft  = left
        engine.moveRight = right
        return true
    }
}
