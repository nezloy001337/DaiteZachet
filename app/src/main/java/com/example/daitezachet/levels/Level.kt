package com.example.daitezachet.levels

import android.graphics.Canvas
import android.graphics.Paint
import com.example.daitezachet.engine.GameEngine

/**
 * Base class for every level.
 *
 * Each level lives in the same room (same geometry, same button position,
 * same door). What changes is HOW the door opens and what obstacles exist.
 *
 * Implement [setup] to configure engine callbacks and place objects
 * (key, spikes). [draw] is optional for custom HUD overlays.
 */
abstract class Level {
    abstract val number: Int
    abstract val hintText: String

    /**
     * Called once when the level starts (and again after each death/reset).
     * Set engine.onButtonPressed, engine.winCondition, engine.key, engine.spikes, etc.
     */
    abstract fun setup(engine: GameEngine)

    /**
     * Optional: draw extra elements on top of the game (debug info, timers…).
     * [paint] is the shared HUD paint — reset its state before use.
     */
    open fun draw(canvas: Canvas, engine: GameEngine, paint: Paint) {}

    // Shared helper — avoids duplicating the bounds check in every level
    protected fun isPlayerAtDoor(engine: GameEngine): Boolean =
        engine.playerInsideDoor()
}
