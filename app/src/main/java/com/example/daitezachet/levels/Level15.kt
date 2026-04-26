package com.example.daitezachet.levels

import android.graphics.*
import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.SpikeDir
import kotlin.math.*

class Level15 : Level() {
    override val number = 13
    override val hintText = "ФИНАЛЬНЫЙ ПОДЪЁМ"

    private var chopperX = 0f
    private var chopperY = 0f
    private val CHOPPER_SIZE = 50f

    private class Missile(var x: Float, var y: Float, val vx: Float, val vy: Float)
    private val missiles = mutableListOf<Missile>()

    private var attackTimer = 0f
    private var secondaryShotTimer = 0f
    private var pendingSecondaryShots = 0
    private var currentKeyStep = 1

    override fun setup(engine: GameEngine) {

        engine.button.hidden = true
        currentKeyStep = 1
        attackTimer = 0f
        pendingSecondaryShots = 0
        missiles.clear()

        chopperX = engine.room.w / 2
        chopperY = 100f

        // Базовые платформы для сбора первых ключей
        setupInitialPlatforms(engine)

        engine.onUpdate = { e, dt ->
            attackTimer += dt

            // 1. ДВИЖЕНИЕ ВЕРТОЛЕТА
            // В финальной фазе скорость преследования увеличивается в 3 раза
            val isOverdrive = currentKeyStep > 4
            val speedBase = if (isOverdrive) 450f else 160f

            val dx = e.player.x - chopperX
            val dy = e.player.y - chopperY
            val dist = sqrt(dx*dx + dy*dy)

            if (dist > 5) {
                chopperX += (dx / dist) * speedBase * dt
                chopperY += (dy / dist) * speedBase * dt
            }

            // Таран
            if (dist < CHOPPER_SIZE + 20f) e.player.isDead = true

            // 2. ЛОГИКА СТРЕЛЬБЫ
            val cooldown = if (isOverdrive) 1.0f else 2.2f
            if (attackTimer >= cooldown) {
                firePattern(e)
                attackTimer = 0f
            }

            // Задержка для очереди (Фаза 2)
            if (pendingSecondaryShots > 0) {
                secondaryShotTimer += dt
                if (secondaryShotTimer >= 0.3f) {
                    spawnMissile(e, 0f)
                    pendingSecondaryShots--
                    secondaryShotTimer = 0f
                }
            }

            // 3. ОБНОВЛЕНИЕ РАКЕТ
            e.spikes.clear()
            val it = missiles.iterator()
            while (it.hasNext()) {
                val m = it.next()
                m.x += m.vx * dt
                m.y += m.vy * dt
                val rx = m.x / e.room.w
                val ry = m.y / e.room.h
                e.addSpikesAt(rx - 0.01f, rx + 0.01f, ry, SpikeDir.UP)
                if (m.x < -100 || m.x > e.room.w + 100 || m.y < -100 || m.y > e.room.h + 100) it.remove()
            }

            // 4. ФАЗА ПАРКУРА И КЛЮЧИ
            updateKeySequence(e)

            if (isOverdrive) {
                // Перемещаем дверь в самый верх (30% - 10% от верха комнаты)
                e.door.bounds.set(e.room.w - 35f, 50f, e.room.w, 200f)
                e.door.isOpen = true
            }
        }

        engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }
    }

    private fun setupInitialPlatforms(e: GameEngine) {
        e.platforms.clear()
        // Пол
        e.addPlatform(0.0f, 0.95f, 1.0f, 1.00f)
        e.addPlatform(0.99f, 200f / e.room.h, 1.00f, 1.00f)
        // Углы для ключей
        e.addPlatform(0.05f, 0.70f, 0.20f, 0.73f) // К1
        e.addPlatform(0.80f, 0.70f, 0.95f, 0.73f) // К2
        e.addPlatform(0.80f, 0.45f, 0.95f, 0.48f) // К3
        e.addPlatform(0.05f, 0.45f, 0.20f, 0.48f) // К4
    }

    private fun setupFinalParkour(e: GameEngine) {
        e.addPlatform(0.40f, 0.80f, 0.50f, 0.82f)
        e.addPlatform(0.65f, 0.65f, 0.75f, 0.67f)
        e.addPlatform(0.45f, 0.50f, 0.55f, 0.52f)
        e.addPlatform(0.25f, 0.35f, 0.35f, 0.37f)
        e.addPlatform(0.60f, 0.25f, 0.75f, 0.27f)
        e.addPlatform(0.85f, 0.20f, 0.97f, 0.22f)
        e.addPlatform(0.99f, 200f / e.room.h, 1.00f, 1.00f)
    }

    private fun firePattern(e: GameEngine) {
        when (currentKeyStep) {
            1 -> spawnMissile(e, 0f)
            2 -> { spawnMissile(e, 0f); pendingSecondaryShots = 1 }
            3 -> { spawnMissile(e, -12f); spawnMissile(e, 12f) }
            4 -> { spawnMissile(e, -20f); spawnMissile(e, 0f); spawnMissile(e, 20f) }
            else -> { // ВЕЕР ОВЕРДРАЙВА
                for (a in listOf(-35f, -15f, 0f, 15f, 35f)) spawnMissile(e, a)
            }
        }
    }

    private fun spawnMissile(e: GameEngine, angleDeg: Float) {
        val px = e.player.x + 22f
        val py = e.player.y + 29f
        val angle = atan2(py - chopperY, px - chopperX) + Math.toRadians(angleDeg.toDouble()).toFloat()
        val speed = if (currentKeyStep > 4) 650f else 550f
        missiles.add(Missile(chopperX, chopperY, cos(angle) * speed, sin(angle) * speed))
    }

    private fun updateKeySequence(e: GameEngine) {
        if (e.keys.isEmpty() && currentKeyStep <= 4) {
            when (currentKeyStep) {
                1 -> e.placeKey(0.12f, 0.70f, id = 1)
                2 -> e.placeKey(0.88f, 0.70f, id = 2)
                3 -> e.placeKey(0.88f, 0.45f, id = 3)
                4 -> e.placeKey(0.12f, 0.45f, id = 4)
            }
        }
        if (e.collectedKeys.contains(currentKeyStep)) {
            currentKeyStep++
            e.keys.clear()
            if (currentKeyStep > 4) setupFinalParkour(e) // Создаем паркур при переходе в финал
        }
    }

    override fun draw(canvas: Canvas, engine: GameEngine, paint: Paint) {
        // Ракеты
        paint.color = Color.RED
        for (m in missiles) canvas.drawCircle(m.x, m.y, 13f, paint)

        // Вертолет
        paint.color = if (currentKeyStep > 4) Color.RED else Color.rgb(40, 60, 40)
        canvas.drawCircle(chopperX, chopperY, CHOPPER_SIZE, paint)

        // Лопасти
        paint.color = Color.BLACK
        val rot = (System.currentTimeMillis() % 400) / 400f * PI * 2
        val bx = cos(rot).toFloat() * 90f
        val by = sin(rot).toFloat() * 10f
        canvas.drawLine(chopperX - bx, chopperY - by, chopperX + bx, chopperY + by, paint)

        // Индикация двери (если в овердрайве)
        if (currentKeyStep > 4) {
            paint.color = Color.GREEN
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f
            canvas.drawRect(engine.door.bounds, paint)
            paint.style = Paint.Style.FILL

            paint.textSize = 50f
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("ВЫХОД ТАМ! ↑", engine.room.w - 50f, 250f, paint)
        }

        // HUD
        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        val status = if (currentKeyStep > 4) "!!! ОВЕРДРАЙВ !!!" else "ФАЗА $currentKeyStep"
        canvas.drawText(status, engine.room.w / 2, 80f, paint)
    }
}