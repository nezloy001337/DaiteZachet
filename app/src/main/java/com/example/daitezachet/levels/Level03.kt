package com.example.daitezachet.levels

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.daitezachet.engine.GameEngine

class Level03 : Level() {

    override val number = 3
    override val hintText = "Запомни код за 2 секунды!"

    private enum class State {
        SHOW_CODE,
        CHOOSE,
        DEAD,
        SUCCESS
    }

    private var state = State.SHOW_CODE

    private enum class PortalColor(val color: Int) {
        RED(Color.rgb(255, 80, 80)),
        BLUE(Color.rgb(80, 140, 255)),
        GREEN(Color.rgb(80, 255, 80))
    }

    private val code = mutableListOf<PortalColor>()
    private var currentStep = 0

    private var codeTimer = 2f

    private var leftColor: PortalColor? = null
    private var rightColor: PortalColor? = null

    private val leftRect = RectF()
    private val rightRect = RectF()

    private var portalCooldown = 0f
    private var pulse = 0f

    override fun setup(engine: GameEngine) {

        val w = engine.room.w
        val h = engine.room.h

        state = State.SHOW_CODE
        codeTimer = 2f
        currentStep = 0
        portalCooldown = 0f
        pulse = 0f

        engine.button.hidden = true
        engine.spikes.clear()
        engine.platforms.clear()

        // ПОЛ
        engine.addPlatform(0f, 0.9f, 1f, 1f)

        // КОД
        val colors = PortalColor.values().toList()
        code.clear()
        repeat(3) { code.add(colors.random()) }

        // ИГРОК В ЦЕНТРЕ
        resetPlayerToCenter(engine)

        setupPortals(engine, 0)

        engine.onUpdate = { e, dt ->

            pulse += dt

            when (state) {

                State.SHOW_CODE -> {
                    codeTimer -= dt
                    if (codeTimer <= 0f) {
                        state = State.CHOOSE
                    }
                }

                State.CHOOSE -> {

                    if (portalCooldown > 0f) portalCooldown -= dt

                    val pb = e.player.bounds

                    if (portalCooldown <= 0f) {

                        if (leftColor != null && RectF.intersects(pb, leftRect)) {
                            handleChoice(leftColor!!, e)
                            portalCooldown = 0.3f
                        }

                        if (rightColor != null && RectF.intersects(pb, rightRect)) {
                            handleChoice(rightColor!!, e)
                            portalCooldown = 0.3f
                        }
                    }
                }

                State.DEAD -> {}

                State.SUCCESS -> {
                    e.door.isOpen = true
                }
            }
        }

        engine.winCondition = {
            state == State.SUCCESS && it.door.isOpen && isPlayerAtDoor(it)
        }
    }

    // ─────────────────────────────
    // ЦЕНТР
    // ─────────────────────────────
    private fun resetPlayerToCenter(e: GameEngine) {
        e.player.vx = 0f
        e.player.vy = 0f

        e.player.x = e.room.w * 0.5f - 22f
        e.player.y = e.room.h * 0.7f
    }

    // ─────────────────────────────
    // ПОРТАЛЫ
    // ─────────────────────────────
    private fun setupPortals(e: GameEngine, stage: Int) {

        val w = e.room.w
        val h = e.room.h

        val pw = 100f
        val ph = 120f
        val y = h * 0.9f - ph

        leftRect.set(w * 0.2f, y, w * 0.2f + pw, y + ph)
        rightRect.set(w * 0.7f, y, w * 0.7f + pw, y + ph)

        val correct = code[stage]
        val wrong = PortalColor.values().filter { it != correct }.random()

        if (kotlin.random.Random.nextBoolean()) {
            leftColor = correct
            rightColor = wrong
        } else {
            leftColor = wrong
            rightColor = correct
        }
    }

    // ─────────────────────────────
    // ЛОГИКА ВЫБОРА
    // ─────────────────────────────
    private fun handleChoice(color: PortalColor, e: GameEngine) {

        if (color == code[currentStep]) {

            currentStep++

            if (currentStep >= 3) {

                state = State.SUCCESS
                leftColor = null
                rightColor = null

                // 👉 ФИНАЛ: только центр, НЕ к двери
                resetPlayerToCenter(e)

            } else {

                setupPortals(e, currentStep)

                // 👉 после этапа тоже в центр
                resetPlayerToCenter(e)
            }

        } else {
            teleportToDeath(e)
        }
    }

    // ─────────────────────────────
    // СМЕРТЬ (ШИПЫ)
    // ─────────────────────────────
    private fun teleportToDeath(e: GameEngine) {

        state = State.DEAD

        leftColor = null
        rightColor = null

        e.platforms.clear()
        e.spikes.clear()

        e.addSpikesFloor(0f, 1f)

        e.player.x = e.room.w * 0.5f
        e.player.y = e.room.h * 0.2f
        e.player.vy = 900f
    }

    // ─────────────────────────────
    // DRAW
    // ─────────────────────────────
    override fun draw(canvas: Canvas, engine: GameEngine, paint: Paint) {

        val w = engine.room.w
        val h = engine.room.h

        if (state == State.SHOW_CODE) {

            paint.color = Color.argb(220, 0, 0, 0)
            canvas.drawRect(0f, 0f, w, h, paint)

            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 40f
            paint.color = Color.WHITE
            canvas.drawText("ЗАПОМНИ КОД", w / 2, h * 0.4f, paint)

            val xs = floatArrayOf(w * 0.3f, w * 0.5f, w * 0.7f)

            for (i in 0..2) {
                paint.textSize = 70f
                paint.color = code[i].color
                canvas.drawCircle(xs[i], h * 0.5f, 28f, paint)

                if (i < 2) {
                    paint.textSize = 40f
                    paint.color = Color.WHITE
                    canvas.drawText("→", (xs[i] + xs[i + 1]) / 2, h * 0.5f, paint)
                }
            }
        }

        if (state == State.CHOOSE) {

            drawPortal(canvas, paint, leftRect, leftColor)
            drawPortal(canvas, paint, rightRect, rightColor)

            paint.color = Color.WHITE
            paint.textSize = 36f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Выбор ${currentStep + 1} / 3", w / 2, h * 0.15f, paint)
        }

        if (state == State.SUCCESS) {

            paint.color = Color.GREEN
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            canvas.drawRect(engine.door.bounds, paint)

            paint.style = Paint.Style.FILL
            paint.textSize = 40f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("ВЫХОД ▶", w / 2, h * 0.2f, paint)
        }
    }

    // ─────────────────────────────
    // ПОРТАЛ
    // ─────────────────────────────
    private fun drawPortal(canvas: Canvas, paint: Paint, rect: RectF, color: PortalColor?) {

        if (color == null) return

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(120,
            Color.red(color.color),
            Color.green(color.color),
            Color.blue(color.color))
        canvas.drawRoundRect(rect, 25f, 25f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = color.color
        canvas.drawRoundRect(rect, 25f, 25f, paint)
    }
}