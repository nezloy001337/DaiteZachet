package com.example.daitezachet.levels

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.Spike
import com.example.daitezachet.engine.SpikeDir
import kotlin.math.abs

class Level14 : Level() {

    override val number   = 14
    override val hintText = "Умри у барьера — пройди сквозь щель"

    private data class GhostPlatform(
        val xr: Float,
        val yr: Float,
        var hp: Int = GHOST_HP,
        var breaking: Boolean = false,
        var breakTimer: Float = 0f,
        var touchedLastFrame: Boolean = false
    )

    private val ghostPlatforms = mutableListOf<GhostPlatform>()
    private var lastAliveXr    = 0.04f
    private var lastAliveYr    = 0.90f
    private var hadFirstFrame  = false
    private var resetRequested = false

    private val fallingSpikes = mutableListOf<FloatArray>() // [xPx, yPx]
    private var dropTimer     = 0f

    private val springSpikes  = mutableListOf<FloatArray>() // [xPx, curY, baseY, phase]
    private var springTimer   = 0f

    // Динамические шипы у платформы ключа
    // [x, y, dirSign], dirSign: +1 вправо, -1 влево
    private val keyTrapSpikes = mutableListOf<FloatArray>()
    private var keyTrapArmed  = false
    private var keyTrapTimer  = 0f
    private var keyWasTaken   = false

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

        private const val GHOST_HP            = 2
        private const val GHOST_BREAK_DELAY   = 0.22f
        private const val KEY_TRAP_SPEED      = 64f
        private const val KEY_TRAP_WARN_TIME  = 0.55f

        private val FORBIDDEN_X_ZONES = listOf(
            0.295f..0.395f,
            0.575f..0.675f
        )
    }

    override fun setup(engine: GameEngine) {

        if (resetRequested) {
            ghostPlatforms.clear()
            resetRequested = false
            hadFirstFrame  = false
        }

        if (hadFirstFrame) {
            val xr = (lastAliveXr - PLAT_W / 2f).coerceIn(0.04f, 1f - PLAT_W - 0.04f)
            val yr = lastAliveYr.coerceIn(MIN_PLAT_YR, MAX_PLAT_YR)

            val centerXr    = xr + PLAT_W / 2f
            val inForbidden = FORBIDDEN_X_ZONES.any { zone -> centerXr in zone }
            val isDuplicate = ghostPlatforms.any { gp -> abs(gp.xr - xr) < 0.06f && abs(gp.yr - yr) < 0.05f }

            if (!inForbidden && !isDuplicate) {
                ghostPlatforms.add(GhostPlatform(xr, yr))
            }
        }

        hadFirstFrame = false
        lastAliveXr   = 0.04f
        lastAliveYr   = 0.90f
        fallingSpikes.clear()
        springSpikes.clear()
        dropTimer   = 0f
        springTimer = 0f

        keyTrapSpikes.clear()
        keyTrapArmed = false
        keyTrapTimer = 0f
        keyWasTaken  = false

        rebuildGhostPlatforms(engine)

        buildBarrier(engine, 0.335f, 0.375f)
        buildBarrier(engine, 0.615f, 0.655f)

        engine.addSpikesFloor(0.20f, 0.33f)
        engine.addSpikesAt(0.20f, 0.33f, 0.0f, SpikeDir.DOWN)

        engine.addSpikesFloor(0.38f, 0.61f)
        listOf(0.435f, 0.495f, 0.555f).forEachIndexed { i, xr ->
            val xPx   = engine.room.w * xr
            val baseY = engine.room.h - engine.room.wallThick - 52f
            springSpikes.add(floatArrayOf(xPx, baseY, baseY, i * 0.38f))
        }

        engine.addSpikesFloor(0.66f, 0.93f)

        engine.addPlatform(0.73f, 0.68f, 0.84f, 0.702f)
        engine.addSpikesAt(0.730f, 0.756f, 0.702f, SpikeDir.UP)
        engine.addSpikesAt(0.814f, 0.840f, 0.702f, SpikeDir.UP)
        engine.placeKey(xr = 0.785f, yr = 0.68f, id = 1, color = Color.rgb(255, 215, 0))
        engine.openDoorWhenKey(1)

        val floorYr = (engine.room.h - engine.room.wallThick) / engine.room.h
        engine.placeButton(0.18f, floorYr)
        engine.button.hidden = false

        engine.onButtonPressed = { eng ->
            resetRequested    = true
            eng.player.isDead = true
        }

        engine.onUpdate = { eng, dt ->
            hadFirstFrame = true
            lastAliveXr   = eng.player.x / eng.room.w
            lastAliveYr   = eng.player.y / eng.room.h

            val pb     = eng.player.bounds
            val floorY = eng.room.h - eng.room.wallThick - 24f

            updateGhostPlatforms(eng, dt)

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

            springTimer += dt
            for (ss in springSpikes) {
                val phase = ((springTimer + ss[3]) % SPRING_PERIOD) / SPRING_PERIOD
                val ext   = if (phase < 0.5f) phase * 2f else (1f - phase) * 2f
                ss[1] = ss[2] - ext * SPRING_TRAVEL
                if (RectF.intersects(pb, RectF(ss[0], ss[1], ss[0] + 28f, ss[1] + 24f))) {
                    eng.player.isDead = true
                }
            }

            updateKeyTrap(eng, dt)
        }

        engine.winCondition = { eng ->
            if (eng.door.isOpen && isPlayerAtDoor(eng)) {
                ghostPlatforms.clear()
                true
            } else false
        }
    }

    private fun rebuildGhostPlatforms(engine: GameEngine) {
        ghostPlatforms.removeAll { it.hp <= 0 }
        for (gp in ghostPlatforms) {
            engine.addPlatform(gp.xr, gp.yr, gp.xr + PLAT_W, gp.yr + PLAT_H)
        }
    }

    private fun updateGhostPlatforms(engine: GameEngine, dt: Float) {
        val pb = engine.player.bounds
        var changed = false

        for (gp in ghostPlatforms) {
            val rect = RectF(
                engine.room.w * gp.xr,
                engine.room.h * gp.yr,
                engine.room.w * (gp.xr + PLAT_W),
                engine.room.h * (gp.yr + PLAT_H)
            )

            val standingOnTop =
                pb.bottom >= rect.top - 10f &&
                        pb.bottom <= rect.top + 18f &&
                        pb.right > rect.left + 6f &&
                        pb.left < rect.right - 6f &&
                        engine.player.vy >= 0f

            if (standingOnTop && !gp.touchedLastFrame && !gp.breaking) {
                gp.hp--
                gp.breaking = true
                gp.breakTimer = GHOST_BREAK_DELAY
            }

            gp.touchedLastFrame = standingOnTop

            if (gp.breaking) {
                gp.breakTimer -= dt
                if (gp.breakTimer <= 0f) {
                    gp.breaking = false
                    if (gp.hp <= 0) {
                        changed = true
                    }
                }
            }
        }

        val before = ghostPlatforms.size
        ghostPlatforms.removeAll { it.hp <= 0 && !it.breaking }
        if (ghostPlatforms.size != before) changed = true

        if (changed) {
            engine.platforms.clear()
            rebuildGhostPlatforms(engine)
        }
    }

    private fun updateKeyTrap(engine: GameEngine, dt: Float) {
        val pb = engine.player.bounds

        if (engine.player.hasKey && !keyWasTaken) {
            keyWasTaken = true
            keyTrapArmed = true
            keyTrapTimer = 0f
            keyTrapSpikes.clear()

            val y = engine.room.h * 0.702f - 24f

            keyTrapSpikes.add(floatArrayOf(engine.room.w * 0.730f, y, +1f))
            keyTrapSpikes.add(floatArrayOf(engine.room.w * 0.814f, y, -1f))
        }

        if (!keyTrapArmed) return

        keyTrapTimer += dt
        if (keyTrapTimer < KEY_TRAP_WARN_TIME) return

        val leftLimit  = engine.room.w * 0.762f
        val rightLimit = engine.room.w * 0.782f

        for (sp in keyTrapSpikes) {
            sp[0] += sp[2] * KEY_TRAP_SPEED * dt
            if (sp[2] > 0f) sp[0] = sp[0].coerceAtMost(leftLimit)
            else sp[0] = sp[0].coerceAtLeast(rightLimit)

            val rect = RectF(sp[0], sp[1], sp[0] + 28f, sp[1] + 24f)
            if (RectF.intersects(pb, rect)) {
                engine.player.isDead = true
            }
        }
    }

    private fun buildBarrier(engine: GameEngine, x1r: Float, x2r: Float) {
        val twoSpikes = 64f / engine.room.w
        val x2 = x1r + twoSpikes

        var yr = 1.0f
        while (yr > GAP_BOT - 0.01f) {
            engine.addSpikesAt(x1r, x2, yr, SpikeDir.UP)
            yr -= 0.08f
        }

        yr = 0.0f
        while (yr < GAP_TOP + 0.01f) {
            engine.addSpikesAt(x1r, x2, yr, SpikeDir.DOWN)
            yr += 0.08f
        }
    }

    override fun draw(canvas: Canvas, engine: GameEngine, paint: Paint) {
        val path = android.graphics.Path()
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(220, 60, 60)
        for (fs in fallingSpikes) {
            path.rewind()
            path.moveTo(fs[0] + 14f, fs[1] + 24f)
            path.lineTo(fs[0] + 28f, fs[1])
            path.lineTo(fs[0], fs[1])
            path.close()
            canvas.drawPath(path, paint)
        }

        paint.color = Color.rgb(255, 140, 0)
        for (ss in springSpikes) {
            path.rewind()
            path.moveTo(ss[0] + 14f, ss[1])
            path.lineTo(ss[0] + 28f, ss[1] + 24f)
            path.lineTo(ss[0], ss[1] + 24f)
            path.close()
            canvas.drawPath(path, paint)
        }

        paint.color = if (keyTrapTimer < KEY_TRAP_WARN_TIME)
            Color.argb(120, 255, 80, 80)
        else
            Color.rgb(255, 40, 120)

        for (sp in keyTrapSpikes) {
            path.rewind()
            path.moveTo(sp[0] + 14f, sp[1])
            path.lineTo(sp[0] + 28f, sp[1] + 24f)
            path.lineTo(sp[0], sp[1] + 24f)
            path.close()
            canvas.drawPath(path, paint)
        }

        for (gp in ghostPlatforms) {
            val l = engine.room.w * gp.xr
            val t = engine.room.h * gp.yr
            val r = engine.room.w * (gp.xr + PLAT_W)
            val b = engine.room.h * (gp.yr + PLAT_H)

            paint.color = when {
                gp.hp >= 2 -> Color.argb(170, 180, 180, 200)
                gp.breaking -> Color.argb(120, 255, 120, 120)
                else -> Color.argb(145, 210, 160, 160)
            }
            canvas.drawRect(l, t, r, b, paint)

            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                gp.hp.coerceAtLeast(0).toString(),
                (l + r) / 2f,
                t - 6f,
                paint
            )
        }

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

        paint.color     = Color.argb(200, 120, 180, 255)
        paint.textSize  = 22f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            "RST",
            engine.room.w * 0.18f,
            engine.room.h - engine.room.wallThick - 30f,
            paint
        )

        paint.color     = Color.argb(80, 255, 255, 255)
        paint.textSize  = 28f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("I", engine.room.w * 0.17f, engine.room.h * 0.12f, paint)
        canvas.drawText("II", engine.room.w * 0.495f, engine.room.h * 0.12f, paint)
        canvas.drawText("III", engine.room.w * 0.81f, engine.room.h * 0.12f, paint)
    }
}