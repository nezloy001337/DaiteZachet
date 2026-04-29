package com.example.daitezachet.levels

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.Spike
import com.example.daitezachet.engine.SpikeDir

/**
 * Level14 — «СЕЙФ»
 *
 * Спавн: x=44..88px (xr≈0.023..0.046). Кнопка RST: xr=0.18.
 *
 * Запрещённые зоны для надгробий (умер внутри — платформа не создаётся):
 *   Барьер 1: xr = 0.295..0.395
 *   Барьер 2: xr = 0.575..0.675
 *   После каждого барьера зона свободна — надгробие ставится нормально.
 *
 * Платформа с ключом: yr=0.68 — достижима прыжком с пола (расчёт: макс yr<0.674+запас).
 *
 * Барьеры: зазор GAP_TOP=0.530..GAP_BOT=0.660
 *   Прыжок с пола yr≈0.90 → у барьера yr=0.558..0.627 → проходит ✓
 *   Прыжок с пола без надгробия (yr≈0.96) → yr=0.64..0.71 → не проходит ✗
 */
class Level14 : Level() {

    override val number   = 14
    override val hintText = "Умри у барьера — пройди сквозь щель"

    // ── Персистентное состояние ───────────────────────────────────────────
    private val ghostPlatforms = mutableListOf<Pair<Float, Float>>()
    private var lastAliveXr    = 0.04f
    private var lastAliveYr    = 0.90f
    private var hadFirstFrame  = false
    private var resetRequested = false

    // ── Динамика ──────────────────────────────────────────────────────────
    private val fallingSpikes = mutableListOf<FloatArray>()  // [xPx, yPx]
    private var dropTimer     = 0f

    private val springSpikes  = mutableListOf<FloatArray>()  // [xPx, curY, baseY, phase]
    private var springTimer   = 0f

    companion object {
        private const val PLAT_W        = 0.075f
        private const val PLAT_H        = 0.022f
        private const val MIN_PLAT_YR   = 0.60f
        private const val MAX_PLAT_YR   = 0.86f
        private const val DROP_INTERVAL = 1.8f
        private const val FALL_SPEED    = 680f
        private const val GAP_TOP       = 0.530f
        private const val GAP_BOT       = 0.660f
        private const val SPRING_TRAVEL = 45f
        private const val SPRING_PERIOD = 1.0f

        // Уменьшенные буферы: только сам барьер ±0.03 с каждой стороны
        // Зона сразу после барьера остаётся свободной
        private val FORBIDDEN_X_ZONES = listOf(
            0.295f..0.395f,   // барьер 1 (0.335..0.375) ± 0.04/0.02
            0.575f..0.675f    // барьер 2 (0.615..0.655) ± 0.04/0.02
        )
    }

    override fun setup(engine: GameEngine) {

        // 1. Полный сброс по запросу
        if (resetRequested) {
            ghostPlatforms.clear()
            resetRequested = false
            hadFirstFrame  = false
        }

        // 2. Сохранить надгробие по последней живой позиции
        if (hadFirstFrame) {
            val xr = (lastAliveXr - PLAT_W / 2f).coerceIn(0.04f, 1f - PLAT_W - 0.04f)
            val yr = lastAliveYr.coerceIn(MIN_PLAT_YR, MAX_PLAT_YR)

            val centerXr    = xr + PLAT_W / 2f
            val inForbidden = FORBIDDEN_X_ZONES.any { zone -> centerXr in zone }
            val isDuplicate = ghostPlatforms.any { (px, _) ->
                kotlin.math.abs(px - xr) < 0.06f
            }
            if (!inForbidden && !isDuplicate) {
                ghostPlatforms.add(xr to yr)
            }
        }

        // 3. Сбросить локальное состояние
        hadFirstFrame = false
        lastAliveXr   = 0.04f
        lastAliveYr   = 0.90f
        fallingSpikes.clear()
        springSpikes.clear()
        dropTimer   = 0f
        springTimer = 0f

        // 4. Восстановить надгробия
        for ((xr, yr) in ghostPlatforms) {
            engine.addPlatform(xr, yr, xr + PLAT_W, yr + PLAT_H)
        }

        // ── БАРЬЕР 1: x = 0.335..0.375 ───────────────────────────────────
        buildBarrier(engine, 0.335f, 0.375f)

        // ── БАРЬЕР 2: x = 0.615..0.655 ───────────────────────────────────
        buildBarrier(engine, 0.615f, 0.655f)

        // ── ЗОНА 1 (x: 0..0.335) ─────────────────────────────────────────
        engine.addSpikesFloor(0.20f, 0.33f)
        engine.addSpikesAt(0.20f, 0.33f, 0.0f, SpikeDir.DOWN)

        // ── ЗОНА 2 (x: 0.375..0.615) ─────────────────────────────────────
        engine.addSpikesFloor(0.38f, 0.61f)
        listOf(0.435f, 0.495f, 0.555f).forEachIndexed { i, xr ->
            val xPx   = engine.room.w * xr
            val baseY = engine.room.h - engine.room.wallThick - 52f
            springSpikes.add(floatArrayOf(xPx, baseY, baseY, i * 0.38f))
        }

        // ── ЗОНА 3 (x: 0.655..0.97) ──────────────────────────────────────
        engine.addSpikesFloor(0.66f, 0.93f)

        // Платформа с ключом опущена до yr=0.68 — достижима прыжком с пола.
        // Расчёт: макс высота прыжка 264px, пол yr≈0.896 → верх прыжка yr≈0.582.
        // Низ игрока в верхней точке: yr≈0.650. Платформа на 0.68 → запас 25px ✓
        engine.addPlatform(0.73f, 0.68f, 0.84f, 0.702f)
        // Шипы по краям — прыгать строго в центр
        engine.addSpikesAt(0.730f, 0.756f, 0.702f, SpikeDir.UP)
        engine.addSpikesAt(0.814f, 0.840f, 0.702f, SpikeDir.UP)
        // Ключ в центре платформы
        engine.placeKey(xr = 0.785f, yr = 0.68f, id = 1, color = Color.rgb(255, 215, 0))
        engine.openDoorWhenKey(1)

        // ── КНОПКА RST ────────────────────────────────────────────────────
        val floorYr = (engine.room.h - engine.room.wallThick) / engine.room.h
        engine.placeButton(0.18f, floorYr)
        engine.button.hidden = false

        engine.onButtonPressed = { eng ->
            resetRequested    = true
            eng.player.isDead = true
        }

        // ── Логика тика ───────────────────────────────────────────────────
        engine.onUpdate = { eng, dt ->

            hadFirstFrame = true
            lastAliveXr   = eng.player.x / eng.room.w
            lastAliveYr   = eng.player.y / eng.room.h

            val pb     = eng.player.bounds
            val floorY = eng.room.h - eng.room.wallThick - 24f

            // Падающие шипы (зона 1)
            dropTimer += dt
            if (dropTimer >= DROP_INTERVAL) {
                dropTimer = 0f
                val xPx = eng.room.w * (0.11f + Math.random().toFloat() * 0.21f)
                fallingSpikes.add(floatArrayOf(xPx, eng.room.wallThick + 30f))
            }
            val toRemove = mutableListOf<FloatArray>()
            for (fs in fallingSpikes) {
                fs[1] += FALL_SPEED * dt
                if (RectF.intersects(pb, RectF(fs[0], fs[1], fs[0] + 28f, fs[1] + 24f))) {
                    eng.player.isDead = true
                }
                if (fs[1] >= floorY) {
                    eng.spikes.add(Spike(fs[0], floorY, dir = SpikeDir.UP))
                    toRemove.add(fs)
                }
            }
            fallingSpikes.removeAll(toRemove)

            // Пружинные шипы (зона 2)
            springTimer += dt
            for (ss in springSpikes) {
                val phase = ((springTimer + ss[3]) % SPRING_PERIOD) / SPRING_PERIOD
                val ext   = if (phase < 0.5f) phase * 2f else (1f - phase) * 2f
                ss[1] = ss[2] - ext * SPRING_TRAVEL
                if (RectF.intersects(pb, RectF(ss[0], ss[1], ss[0] + 28f, ss[1] + 24f))) {
                    eng.player.isDead = true
                }
            }
        }

        engine.winCondition = { eng ->
            if (eng.door.isOpen && isPlayerAtDoor(eng)) {
                ghostPlatforms.clear()
                true
            } else false
        }
    }

    private fun buildBarrier(engine: GameEngine, x1r: Float, x2r: Float) {
        // Ширина барьера: ровно 2 шипа (шаг 32px, шип 28px)
        // x2r не используется — вычисляем сами: x1r + 2*32px / room.w
        // При room.w = 1920: 64/1920 = 0.0333f
        val twoSpikes = 64f / engine.room.w  // всегда 2 шипа независимо от разрешения
        val x2 = x1r + twoSpikes

        // Шипы снизу: от пола до GAP_BOT
        var yr = 1.0f
        while (yr > GAP_BOT - 0.01f) {
            engine.addSpikesAt(x1r, x2, yr, SpikeDir.UP)
            yr -= 0.08f
        }
        // Шипы сверху: от потолка до GAP_TOP
        yr = 0.0f
        while (yr < GAP_TOP + 0.01f) {
            engine.addSpikesAt(x1r, x2, yr, SpikeDir.DOWN)
            yr += 0.08f
        }
    }

    override fun draw(canvas: Canvas, engine: GameEngine, paint: Paint) {
        val path = android.graphics.Path()
        paint.style = Paint.Style.FILL

        // Падающие шипы (острие вниз, красные)
        paint.color = Color.rgb(220, 60, 60)
        for (fs in fallingSpikes) {
            path.rewind()
            path.moveTo(fs[0] + 14f, fs[1] + 24f)
            path.lineTo(fs[0] + 28f, fs[1])
            path.lineTo(fs[0],       fs[1])
            path.close()
            canvas.drawPath(path, paint)
        }

        // Пружинные шипы (острие вверх, оранжевые)
        paint.color = Color.rgb(255, 140, 0)
        for (ss in springSpikes) {
            path.rewind()
            path.moveTo(ss[0] + 14f, ss[1])
            path.lineTo(ss[0] + 28f, ss[1] + 24f)
            path.lineTo(ss[0],       ss[1] + 24f)
            path.close()
            canvas.drawPath(path, paint)
        }

        // Подсветка запрещённых зон (полупрозрачный красный)
        paint.color = Color.argb(35, 255, 0, 0)
        for (zone in FORBIDDEN_X_ZONES) {
            canvas.drawRect(
                engine.room.w * zone.start,
                engine.room.wallThick,
                engine.room.w * zone.endInclusive,
                engine.room.h - engine.room.wallThick,
                paint
            )
        }

        // Подпись RST над кнопкой
        paint.color     = Color.argb(200, 120, 180, 255)
        paint.textSize  = 22f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            "RST",
            engine.room.w * 0.18f,
            engine.room.h - engine.room.wallThick - 30f,
            paint
        )

        // Метки зон
        paint.color     = Color.argb(80, 255, 255, 255)
        paint.textSize  = 28f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("I",   engine.room.w * 0.17f,  engine.room.h * 0.12f, paint)
        canvas.drawText("II",  engine.room.w * 0.495f, engine.room.h * 0.12f, paint)
        canvas.drawText("III", engine.room.w * 0.81f,  engine.room.h * 0.12f, paint)
    }
}