package com.example.daitezachet.levels

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.Spike
import com.example.daitezachet.engine.SpikeDir

class Level12 : Level() {

    override val number   = 12
    override val hintText = "Собери все ключи(следуй радуге)"

    companion object {
        var APPROACH_SPEED = 342f
        var RETURN_SPEED   = 180f
        private const val SPIKE_W = 28f
        private const val SPIKE_H = 24f
    }

    private var rise         = false
    private var topOffset    = 0f
    private var bottomOffset = 0f
    private val topSpikes    = mutableListOf<Spike>()
    private val bottomSpikes = mutableListOf<Spike>()
    private var topOriginY    = 0f
    private var bottomOriginY = 0f

    private var prevKeyCount = 0

    private val bridgePlatform1 = RectF()
    private val bridgePlatform2 = RectF()
    private val bridgePlatform3 = RectF()

    override fun setup(engine: GameEngine) {
        topSpikes.clear(); bottomSpikes.clear()
        rise = false; topOffset = 0f; bottomOffset = 0f; prevKeyCount = 0

        engine.platforms.clear(); engine.spikes.clear()
        engine.door.bounds.set(engine.room.doorRect)

        val w = engine.room.w; val h = engine.room.h; val wt = engine.room.wallThick

        topOriginY    = wt - 30f
        bottomOriginY = h - 25f

        var x = wt + 4f
        while (x + SPIKE_W <= w - wt) {
            val s = Spike(x, topOriginY, SPIKE_W, SPIKE_H, SpikeDir.DOWN)
            engine.spikes.add(s); topSpikes.add(s); x += SPIKE_W + 2f
        }
        x = wt + 4f
        while (x + SPIKE_W <= w - wt) {
            val s = Spike(x, bottomOriginY, SPIKE_W, SPIKE_H, SpikeDir.UP)
            engine.spikes.add(s); bottomSpikes.add(s); x += SPIKE_W + 2f
        }
        //Лестница
        engine.addPlatform(0.0f, 0.70f, 0.31f, 0.74f)
        engine.addPlatform(0.1f, 0.45f, 0.40f, 0.49f)
        engine.addPlatform(0.0f, 0.20f, 0.31f, 0.24f)
        //Стены
        engine.addPlatform(0.40f, 0.20f, 0.415f, 1f)
        engine.addPlatform(0.92f, 0f, 0.935f, 0.80f)
        engine.addPlatform(0.86f, 0.20f, 0.875f, 1f)


        engine.addPlatform(0.45f, 0.44f, 0.6f, 0.48f)

        engine.placeKey(0.2f, 0.94f, id = 0, color = Color.WHITE) //1
        engine.placeKey(0.50f, 0.44f, id = 1, color = Color.RED) //2
        engine.placeKey(0.04f, 0.18f, id = 2, color = Color.CYAN) //5
        engine.placeKey(0.2f, 0.18f, id = 3, color = Color.YELLOW) //3


        bridgePlatform1.set(w * 0.74f, h * 0.70f, w * 0.86f, h * 0.74f)
        bridgePlatform2.set(w * 0.295f, h * 0f, w * 0.31f, h * 0.20f)
        bridgePlatform3.set(w * 0.74f, h * 0.30f, w * 0.86f, h * 0.34f)

        engine.platforms.add(bridgePlatform2)
        engine.placeButton(xr = 0.6375f, yr = 0.96f)
        engine.onButtonPressed = { e ->
            rise = !rise
            if (e.button.pressCount % 2 == 1) {
                engine.platforms.add(bridgePlatform1)
                engine.platforms.add(bridgePlatform3)
            }
            else {
                engine.platforms.remove(bridgePlatform1)
                engine.platforms.remove(bridgePlatform3)
            }
            if (e.button.pressCount == 1) {
                e.placeKey(0.80f, 0.94f, id = 10, color = Color.BLUE) //6
                e.placeKey(0.04f, 0.94f, id = 11, color = Color.GREEN) //4
                e.placeKey(0.80f, 0.30f, id = 12, color = Color.MAGENTA) //7
                engine.platforms.remove(bridgePlatform2)
            }
        }

        engine.onUpdate = { e, dt ->
            updateSpikes(e, dt)
            val cur = e.collectedKeys.size
            if (cur > prevKeyCount) { rise = !rise; prevKeyCount = cur }
        }

        engine.doorCondition = { e -> e.keys.isNotEmpty() && e.keys.all { it.isCollected } }
        engine.winCondition  = { e -> e.door.isOpen && isPlayerAtDoor(e) }
    }

    private fun updateSpikes(engine: GameEngine, dt: Float) {
        val maxTop    = engine.room.h * 0.95f
        val maxBottom = engine.room.h * 0.95f
        if (!rise) {
            if (bottomOffset > 0f) {
                bottomOffset = (bottomOffset - RETURN_SPEED * dt).coerceAtLeast(0f); applyBottom()
            }
            if (bottomOffset == 0f && topOffset < maxTop) {
                topOffset = (topOffset + APPROACH_SPEED * dt).coerceAtMost(maxTop); applyTop()
            }
        } else {
            if (topOffset > 0f) {
                topOffset = (topOffset - RETURN_SPEED * dt).coerceAtLeast(0f); applyTop()
            }
            if (topOffset == 0f && bottomOffset < maxBottom) {
                bottomOffset = (bottomOffset + APPROACH_SPEED * dt).coerceAtMost(maxBottom); applyBottom()
            }
        }
    }

    private fun applyTop() {
        val y = topOriginY + topOffset
        topSpikes.forEach { it.bounds.set(it.bounds.left, y, it.bounds.right, y + SPIKE_H) }
    }

    private fun applyBottom() {
        val y = bottomOriginY - bottomOffset
        bottomSpikes.forEach { it.bounds.set(it.bounds.left, y, it.bounds.right, y + SPIKE_H) }
    }

    override fun draw(canvas: Canvas, engine: GameEngine, paint: Paint) {
        // счётчик ключей
        val total     = engine.keys.size
        val collected = engine.keys.count { it.isCollected }
        paint.textSize = 26f
        paint.color = if (collected == total && total > 0) Color.argb(220, 80, 255, 120)
        else Color.argb(220, 255, 255, 255)
        canvas.drawText("Ключей $collected/$total", engine.room.w / 2f, engine.room.h * 0.16f, paint)
    }
}