package com.example.daitezachet.engine

import android.graphics.RectF

/**
 * Неизменяемая геометрия комнаты: стены, потолок, пол, дверь, кнопка.
 * Платформы — динамические, хранятся в GameEngine и задаются каждым уровнем.
 */
class Room(val w: Float, val h: Float) {

    val wallThick = 30f

    val ceiling  = RectF(0f, 0f, w, wallThick)
    val floor    = RectF(0f, h - wallThick, w, h)
    val leftWall = RectF(0f, 0f, wallThick, h)

    // Правая стена с проёмом для двери
    val doorTop    = h * 0.30f
    val doorBottom = h - wallThick

    val rightWallTop    = RectF(w - wallThick, 0f,         w, doorTop)
    val rightWallBottom = RectF(w - wallThick, doorBottom, w, h)

    // Только базовые стены (платформ нет — их добавляют уровни)
    val staticSolids: List<RectF> = listOf(
        floor, ceiling, leftWall, rightWallTop, rightWallBottom
    )

    val doorRect   = RectF(w - wallThick, doorTop, w, doorBottom)
    val buttonRect = RectF(w * 0.42f, h - wallThick - 24f, w * 0.42f + 54f, h - wallThick)

    val playerSpawnX = wallThick + 14f
    val playerSpawnY = h - wallThick - Player.HEIGHT - 4f
}
