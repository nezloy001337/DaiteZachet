package com.example.daitezachet.levels

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.daitezachet.engine.GameEngine

/**
 * Уровень 1 — «Невидимые платформы».
 *
 * Механики: После нажатия кнопки платформы становятся невидимыми,
 * но сохраняют физику (игрок может по ним ходить по памяти).
 *
 *
 *  ╔═══════════════════════════════════╗
 *  ║             ──     ──     ──      ║
 *  ║        ──      ──     ──     _★_  ║    ← платформа с ключом
 *  ║☻ [btn]  ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲   ▶ ║  ← выход
 *  ╚═══════════════════════════════════╝  ← шипы на полу
 */
class Level11 : Level() {

    override val number = 11
    override val hintText = "Запомни маршрут"

    private var hiddenMode = false

    private val keyColor = Color.rgb(255, 215, 0)

    private val overlayPaint = Paint().apply {
        color = Color.rgb(18, 18, 28)
        style = Paint.Style.FILL
        isAntiAlias = false
    }

    override fun setup(engine: GameEngine) {

        hiddenMode = false

        // -----------------------------
        // Платформы (маршрут по памяти)
        // -----------------------------

        engine.addPlatform(0f, 0.85f, 0.10f, 0.83f)
        engine.addPlatform(0.15f, 0.80f, 0.20f, 0.83f)
        engine.addPlatform(0.25f, 0.55f, 0.30f, 0.58f)
        engine.addPlatform(0.40f, 0.60f, 0.45f, 0.63f)
        engine.addPlatform(0.50f, 0.50f, 0.55f, 0.53f)
        engine.addPlatform(0.60f, 0.30f, 0.65f, 0.33f)
        engine.addPlatform(0.73f, 0.40f, 0.75f, 0.43f)
        engine.addPlatform(0.80f, 0.40f, 0.90f, 0.43f)

        engine.placeKey(0.85f, 0.40f, id = 1, color = keyColor)

        // Шипы внизу (вся зона опасная)
        engine.addSpikesFloor(0.20f, 0.95f)

        // -----------------------------
        // Кнопка: запускает "невидимость"
        // -----------------------------
        engine.placeButton(0.10f, 0.95f)

        engine.onButtonPressed = { e ->
            hiddenMode = true
        }

        // Дверь
        engine.openDoorWhenAnyKey()

        engine.winCondition = { e ->
            e.door.isOpen && e.player.hasKey && isPlayerAtDoor(e)
        }
    }

    override fun draw(canvas: Canvas, engine: GameEngine, paint: Paint) {

        if (!hiddenMode) return

        // небольшой запас, чтобы убрать артефакты
        val pad = 3f

        engine.platforms.forEach { p ->
            canvas.drawRect(
                p.left - pad,
                p.top - pad,
                p.right + pad,
                p.bottom + pad,
                overlayPaint
            )
        }
    }
}