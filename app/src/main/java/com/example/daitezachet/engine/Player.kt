package com.example.daitezachet.engine

import android.graphics.RectF

class Player(startX: Float, startY: Float) {

    companion object {
        const val WIDTH      = 44f
        const val HEIGHT     = 58f
        const val MOVE_SPEED = 480f   // шире экран в альбомном — быстрее движение
        const val JUMP_FORCE = -920f  // рассчитано под gameH ≈ 0.80 * 1080
    }

    var x: Float = startX
    var y: Float = startY
    var vx: Float = 0f
    var vy: Float = 0f
    var isOnGround: Boolean = false
    var hasKey: Boolean = false
    var isDead: Boolean = false

    val bounds: RectF get() = RectF(x, y, x + WIDTH, y + HEIGHT)

    fun reset(startX: Float, startY: Float) {
        x = startX; y = startY
        vx = 0f;    vy = 0f
        isOnGround = false
        hasKey     = false
        isDead     = false
    }
}
