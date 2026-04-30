package com.example.daitezachet.levels

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.Spike
import com.example.daitezachet.engine.SpikeDir

/**
 * Level 9 - locked key room under pressure.
 *
 * Core mechanics:
 * - The exit door is moved to the left side of the room.
 * - The original right-side door opening is sealed so the player must return left.
 * - The key is visible inside a closed room on the right.
 * - The key room is boxed in by a ceiling, floor, right wall, and a removable left gate.
 * - Pressing the button removes the gate and starts the pressure trap.
 * - Pressure spikes move down from the ceiling and up from below the floor.
 *
 * Intended route:
 *   Spawn -> jump across the small parkour platforms -> press the button ->
 *   enter the opened key room -> collect the key -> escape back left ->
 *   reach the relocated door before the spike press closes the room.
 *
 * Layout sketch:
 *
 *   +----------------------------------------------------------+
 *   | LEFT EXIT                                        vvvvvvv |
 *   | [DOOR]                         +-----------+            |
 *   |                                |    KEY    |            |
 *   |                                |           |            |
 *   |        P1      P2 [BTN]   P3   |           |            |
 *   |              ^^^^^^   ^^^^^^   +-----G-----+            |
 *   |        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^            |
 *   +----------------------------------------------------------+
 *
 *   G = removable gate opened by the button.
 *   v = moving ceiling spikes, ^ = floor/platform spikes.
 */
class Level09 : Level() {
    override val number = 9
    override val hintText = "Кнопка откроет ключ"

    private val topSpikes = mutableListOf<Spike>()
    private val bottomSpikes = mutableListOf<Spike>()
    private var gate: RectF? = null
    private var pressureStarted = false
    private var pressureTime = 0f

    override fun setup(engine: GameEngine) {
        topSpikes.clear()
        bottomSpikes.clear()
        gate = null
        pressureStarted = false
        pressureTime = 0f

        engine.door.bounds.set(0f, engine.room.doorTop, engine.room.wallThick, engine.room.doorBottom)
        engine.door.isOpen = false
        engine.door.visuallyOpen = true

        buildRoom(engine)
        buildPressureSpikes(engine)

        engine.onButtonPressed = { e ->
            if (!pressureStarted) {
                pressureStarted = true
                openKeyRoom(e)
            }
        }

        engine.onUpdate = { e, dt ->
            if (pressureStarted) {
                pressureTime += dt
                val shift = e.room.h * 0.075f * dt
                topSpikes.forEach { it.bounds.offset(0f, shift) }
                bottomSpikes.forEach { it.bounds.offset(0f, -shift) }
            }
        }

        engine.doorCondition = { e -> e.player.hasKey }
        engine.winCondition = { e -> e.door.isOpen && isPlayerAtLeftDoor(e) }
    }

    private fun buildRoom(engine: GameEngine) {
        engine.addPlatform(0.08f, 0.68f, 0.24f, 0.72f)
        engine.addPlatform(0.29f, 0.74f, 0.42f, 0.78f)
        engine.addPlatform(0.46f, 0.62f, 0.58f, 0.66f)
        engine.addPlatform(0.62f, 0.56f, 0.70f, 0.60f)

        engine.placeButton(xr = 0.50f, yr = 0.62f)

        val w = engine.room.w
        val h = engine.room.h

        engine.platforms.add(RectF(w - engine.room.wallThick, engine.room.doorTop, w, engine.room.doorBottom))

        engine.platforms.add(RectF(w * 0.72f, h * 0.30f, w * 0.96f, h * 0.34f))
        engine.platforms.add(RectF(w * 0.72f, h * 0.56f, w * 0.96f, h * 0.60f))
        engine.platforms.add(RectF(w * 0.92f, h * 0.30f, w * 0.96f, h * 0.60f))
        gate = RectF(w * 0.72f, h * 0.34f, w * 0.76f, h * 0.56f).also {
            engine.platforms.add(it)
        }
        engine.placeKey(0.84f, 0.56f)

        engine.addSpikesFloor(0.12f, 0.86f)
        engine.addSpikesAt(0.29f, 0.42f, yr = 0.74f, dir = SpikeDir.DOWN)
        engine.addSpikesAt(0.46f, 0.58f, yr = 0.62f, dir = SpikeDir.DOWN)
        engine.addSpikesWall(0.08f, 0.34f, xr = 0.97f, dir = SpikeDir.LEFT)
    }

    private fun openKeyRoom(engine: GameEngine) {
        gate?.let { engine.platforms.remove(it) }
        gate = null
    }

    private fun buildPressureSpikes(engine: GameEngine) {
        val left = engine.room.wallThick + 8f
        val right = engine.room.w - engine.room.wallThick - 8f
        val topStartY = engine.room.wallThick + 4f
        val bottomStartY = engine.room.h + 70f

        var x = left
        while (x + 28f <= right) {
            Spike(x, topStartY, dir = SpikeDir.DOWN).also {
                topSpikes.add(it)
                engine.spikes.add(it)
            }
            Spike(x, bottomStartY, dir = SpikeDir.UP).also {
                bottomSpikes.add(it)
                engine.spikes.add(it)
            }
            x += 42f
        }
    }

    private fun isPlayerAtLeftDoor(engine: GameEngine): Boolean {
        val pb = engine.player.bounds
        val door = engine.door.bounds
        return pb.left <= engine.room.wallThick + 12f &&
                pb.bottom > door.top &&
                pb.top < door.bottom
    }

    override fun draw(canvas: Canvas, engine: GameEngine, paint: Paint) {
        val danger = if (pressureStarted) (pressureTime / 12.0f).coerceIn(0f, 1f) else 0f
        if (danger > 0f) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((danger * 115).toInt(), 220, 40, 40)
            canvas.drawRect(0f, 0f, engine.room.w, engine.room.h, paint)
        }

        if (pressureStarted && pressureTime < 1.2f) {
            paint.color = Color.WHITE
            paint.textSize = 42f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("ПРОХОД ОТКРЫТ", engine.room.w / 2f, engine.room.h * 0.20f, paint)
        }
    }
}
