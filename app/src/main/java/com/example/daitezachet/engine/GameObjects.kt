package com.example.daitezachet.engine

import android.graphics.RectF

class Button(rect: RectF) {
    val bounds     = RectF(rect)
    var isPressed  = false
    var pressCount = 0
    var hidden     = false   // true → не рисуется и не срабатывает
}

class Door(rect: RectF) {
    val bounds = RectF(rect)
    var isOpen = false
}

class Key(x: Float, y: Float) {
    companion object { const val SIZE = 30f }
    val bounds      = RectF(x, y, x + SIZE, y + SIZE)
    var isCollected = false
}

/**
 * @param flipped  true = spike points downward (hangs from ceiling / platform underside)
 */
class Spike(x: Float, y: Float, val width: Float = 28f, val height: Float = 24f, val flipped: Boolean = false) {
    val bounds = RectF(x, y, x + width, y + height)
}
