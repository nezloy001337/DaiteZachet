package com.example.daitezachet.levels

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.SpikeDir

/**
 * Уровень 7 — «Код».
 *
 * ════════════════════════════════════════════════════════
 * УНИКАЛЬНАЯ МЕХАНИКА: три кнопки, строгий порядок нажатий.
 *
 * На экране показан «код» — порядок, в котором нужно нажать
 * кнопки B1 → B2 → B3 (или другой). Код генерируется случайно
 * при каждом старте/рестарте — выучить нельзя, нужно читать.
 *
 * Каждое ВЕРНОЕ нажатие убирает один «слой» шипов на полу,
 * открывая проход к следующей кнопке.
 * ОШИБКА в порядке → весь пол мгновенно засыпается шипами.
 *
 * ════════════════════════════════════════════════════════
 * Схема:
 *
 *  ╔══════════════════════════════════════════════════════╗
 *  ║ ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ потолочные шипы   ║
 *  ║                                                      ║
 *  ║  ══P1══         ══P2══         ══P3══                ║
 *  ║  [B1]   ▲▲▲▲▲  [B2]  ▲▲▲▲▲   [B3]  ▲▲▲▲▲       ▶  ║
 *  ║         zone1         zone2          zone3            ║
 *  ╚══════════════════════════════════════════════════════╝
 *
 *  P1..P3 — платформы с кнопками B1..B3, на разной высоте.
 *  zone1..3 — участки шипов на полу, убираются по одному.
 *  Код показан вверху экрана (например «B2 → B1 → B3»).
 *
 * ════════════════════════════════════════════════════════
 * Чем отличается от предыдущих:
 *  L1–L3: одна кнопка, порядок не важен.
 *  L4–L5: кнопок нет или 1; прыжки и ключи.
 *  L6:    движущиеся платформы.
 *  L7:    три кнопки, случайный код, динамические шипы —
 *         ни платформ-лифтов, ни ключей. Память + маршрут.
 */
class Level07 : Level() {
    override val number   = 7
    override val hintText = "Следуй коду. Ошибёшься — пожалеешь"

    // ── Код: порядок кнопок (0=B1, 1=B2, 2=B3) ───────────────────────────────
    private var code = intArrayOf(0, 1, 2)
    private var step = 0

    private val currentTarget get() = if (step < 3) code[step] else -1

    // ── Три платформы: x1,y1,x2,y2, btnX,btnY (в долях) ─────────────────────
    private val platDefs = arrayOf(
        floatArrayOf(0.04f, 0.65f, 0.20f, 0.69f,  0.04f, 0.65f),  // P1 — низкая
        floatArrayOf(0.36f, 0.50f, 0.52f, 0.54f,  0.36f, 0.50f),  // P2 — средняя
        floatArrayOf(0.68f, 0.35f, 0.84f, 0.39f,  0.68f, 0.35f)   // P3 — высокая
    )

    // ── Зоны шипов на полу ────────────────────────────────────────────────────
    private val zoneFloorX = arrayOf(
        0.22f to 0.34f,
        0.54f to 0.66f,
        0.86f to 0.96f
    )
    private val zoneSpikeIndices = Array(3) { mutableListOf<Int>() }

    // ── Прямоугольники кнопок (заполняются в setup) ───────────────────────────
    private val buttonRects  = Array(3) { RectF() }
    private val buttonActive = BooleanArray(3)   // предыдущее состояние нажатия

    // ── Состояние ловушки и вспышки ───────────────────────────────────────────
    private var trapAdded    = false
    private var flashTimer   = 0f
    private var flashIsError = false

    // ── Цвета и подписи кнопок ───────────────────────────────────────────────
    private val btnColors = intArrayOf(
        Color.rgb(255, 80,  80),
        Color.rgb(80,  180, 255),
        Color.rgb(80,  210, 100)
    )
    private val btnLabels = arrayOf("B1", "B2", "B3")

    // ─────────────────────────────────────────────────────────────────────────

    override fun setup(engine: GameEngine) {
        step         = 0
        trapAdded    = false
        flashTimer   = 0f
        flashIsError = false
        zoneSpikeIndices.forEach { it.clear() }
        buttonActive.fill(false)

        // Случайный код при каждом старте
        code = intArrayOf(0, 1, 2).also { it.shuffle() }

        // Стандартную кнопку движка прячем
        engine.button.hidden = true

        // ── Потолочные шипы ────────────────────────────────────────────────
        engine.addSpikesAt(0.03f, 0.97f, yr = 0.00f, dir = SpikeDir.DOWN)

        // ── Три платформы ──────────────────────────────────────────────────
        for (i in platDefs.indices) {
            val d = platDefs[i]
            engine.addPlatform(d[0], d[1], d[2], d[3])

            val bx = engine.room.w * d[4]
            val by = engine.room.h * d[5]
            buttonRects[i].set(bx, by - 24f, bx + 54f, by)
        }

        // ── Шипы трёх зон ─────────────────────────────────────────────────
        for (z in 0..2) {
            val before = engine.spikes.size
            engine.addSpikesFloor(zoneFloorX[z].first, zoneFloorX[z].second)
            for (idx in before until engine.spikes.size) zoneSpikeIndices[z].add(idx)
        }

        // ── Обновление ────────────────────────────────────────────────────
        engine.onUpdate = { e, dt ->
            if (flashTimer > 0f) flashTimer -= dt
            val pb = e.player.bounds

            for (i in 0..2) {
                val prev = buttonActive[i]
                val now  = e.player.isOnGround && RectF.intersects(pb, buttonRects[i])
                if (now && !prev) onButtonPress(i, e)
                buttonActive[i] = now
            }
        }

        engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }
    }

    // ── Обработка нажатия ────────────────────────────────────────────────────

    private fun onButtonPress(i: Int, e: GameEngine) {
        if (trapAdded) return

        if (i == currentTarget) {
            removeZone(step, e)
            step++
            flashIsError = false
            flashTimer   = 0.35f
            if (step == 3) e.door.isOpen = true
        } else {
            triggerTrap(e)
            flashIsError = true
            flashTimer   = 1.6f
        }
    }

    private fun removeZone(zone: Int, e: GameEngine) {
        val sorted = zoneSpikeIndices[zone].sortedDescending()
        sorted.forEach { if (it < e.spikes.size) e.spikes.removeAt(it) }
        zoneSpikeIndices[zone].clear()
        val removed = sorted.size
        for (z in zone + 1..2) zoneSpikeIndices[z].replaceAll { it - removed }
    }

    private fun triggerTrap(e: GameEngine) {
        if (trapAdded) return
        trapAdded = true
        e.addSpikesFloor(0.00f, 1.00f)
    }

    // ── HUD ──────────────────────────────────────────────────────────────────

    override fun draw(canvas: Canvas, engine: GameEngine, paint: Paint) {
        val w = engine.room.w
        val h = engine.room.h
        paint.textAlign = Paint.Align.CENTER

        // Фоновая плашка кода
        paint.color = Color.argb(200, 0, 0, 0)
        canvas.drawRoundRect(w * 0.10f, h * 0.04f, w * 0.90f, h * 0.17f, 18f, 18f, paint)

        // Надпись «КОД:»
        paint.color    = Color.WHITE
        paint.textSize = 26f
        canvas.drawText("КОД:", w * 0.50f, h * 0.09f, paint)

        // Три элемента кода
        val xs = floatArrayOf(w * 0.28f, w * 0.50f, w * 0.72f)
        for (pos in 0..2) {
            val btnIdx = code[pos]
            val done   = pos < step
            val active = pos == step
            paint.color    = when { done -> Color.argb(110, 255,255,255); active -> btnColors[btnIdx]; else -> Color.argb(150,180,180,180) }
            paint.textSize = if (active) 40f else 28f
            canvas.drawText(btnLabels[btnIdx] + if (done) "✓" else "", xs[pos], h * 0.155f, paint)
            if (pos < 2) {
                paint.color    = Color.argb(130, 255, 255, 255)
                paint.textSize = 22f
                canvas.drawText("→", (xs[pos] + xs[pos + 1]) / 2f, h * 0.155f, paint)
            }
        }

        // Метки и прямоугольники кнопок на платформах
        paint.textSize    = 28f
        paint.strokeWidth = 3f
        for (i in 0..2) {
            val r    = buttonRects[i]
            val cx   = (r.left + r.right) / 2f
            val isNext = currentTarget == i
            val isDone = code.indexOf(i) < step

            paint.color = when { isDone -> Color.argb(90,255,255,255); isNext -> btnColors[i]; else -> Color.argb(140,180,180,180) }
            canvas.drawText(btnLabels[i], cx, r.top - 8f, paint)

            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(r, 6f, 6f, paint)
            paint.style = Paint.Style.FILL
        }

        // Вспышка успеха / ошибки
        if (flashTimer > 0f) {
            val alpha = (flashTimer * 130).toInt().coerceIn(0, 170)
            paint.color = if (flashIsError) Color.argb(alpha, 220, 40, 40) else Color.argb(alpha, 40, 210, 90)
            canvas.drawRect(0f, 0f, w, h, paint)

            paint.color    = Color.WHITE
            paint.textSize = 50f
            canvas.drawText(if (flashIsError) "НЕВЕРНО! ВСЁ РУШИТСЯ" else "ВЕРНО!", w / 2f, h / 2f, paint)
        }
    }
}