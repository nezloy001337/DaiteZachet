package com.example.daitezachet.levels

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.SpikeDir

class Level05 : Level() {
    override val number = 5
    override val hintText = "Собери 3 кристалла → кнопка → уклоняйся от пуль!"
    val reg = 0
    private enum class Phase { RUN, PAUSE_BEFORE_BULLETS, BULLETS }
    private var currentPhase = Phase.RUN

    private var waveRightEdge = 0f
    private var waveSpeed = 80f
    private val waveAcceleration = 40f
    private var fallPenalty = false
    private var buttonPressed = false
    private var pauseTimer = 0f

    private data class TimedSpike(val bounds: RectF, var timer: Float = 1.0f)
    private val pendingSpikes = mutableListOf<TimedSpike>()
    private var staticPlatformsCount = 0

    private data class Bullet(val bounds: RectF, var x: Float, val y: Float, val speed: Float)
    private val bullets = mutableListOf<Bullet>()
    private var bulletSpawnTimer = 0f
    private var bulletSpawnInterval = 1.2f
    private val bulletSpawnIntervalNormal = 1.2f
    private val bulletSpawnIntervalFast = 0.7f
    private val bulletSpeed = 240f
    private var waveBulletTimer = 0f
    private var isWaveBullet = false

    private data class PlatformTimer(val platform: RectF, var timeOnPlatform: Float)
    private val platformTimers = mutableListOf<PlatformTimer>()
    private var exitDoorPlaced = false
    private var levelCompletedFlag = false

    override fun setup(engine: GameEngine) {
        currentPhase = Phase.RUN
        waveRightEdge = 0f
        waveSpeed = 80f
        fallPenalty = false
        buttonPressed = false
        pauseTimer = 0f
        pendingSpikes.clear()
        bullets.clear()
        platformTimers.clear()
        bulletSpawnTimer = 0f
        bulletSpawnInterval = 1.2f
        waveBulletTimer = 0f
        isWaveBullet = false
        exitDoorPlaced = false
        levelCompletedFlag = false

        val w = engine.room.w
        val h = engine.room.h

        engine.addPlatform(0.02f, 0.80f, 0.12f, 0.84f)
        engine.addPlatform(0.20f, 0.55f, 0.30f, 0.59f)
        engine.addPlatform(0.45f, 0.70f, 0.55f, 0.74f)
        engine.addPlatform(0.70f, 0.55f, 0.80f, 0.59f)
        engine.addPlatform(0.88f, 0.80f, 0.98f, 0.84f)

        engine.placeKey(0.25f, 0.53f, id = 1, color = Color.rgb(255, 80, 80))
        engine.placeKey(0.50f, 0.68f, id = 2, color = Color.rgb(80, 140, 255))
        engine.placeKey(0.75f, 0.53f, id = 3, color = Color.rgb(80, 255, 80))

        engine.placeButton(xr = 0.90f, yr = 0.80f)
        engine.button.hidden = false
//d'


        engine.onButtonPressed = { e ->
            if (currentPhase == Phase.RUN && !buttonPressed && e.collectedKeys.size >= 3) {
                buttonPressed = true
                currentPhase = Phase.PAUSE_BEFORE_BULLETS
                pauseTimer = 1.5f
                waveSpeed = 0f
                waveRightEdge = 0f
                e.platforms.clear()
                e.spikes.clear()
                e.keys.removeAll { it.id in 1..3 }
                e.button.hidden = true
                staticPlatformsCount = 0
            }
        }

        staticPlatformsCount = engine.platforms.size

        // Отключаем стандартное условие победы
        engine.winCondition = { false }

        engine.onUpdate = { e, dt ->
            val player = e.player
            val room = e.room

            when (currentPhase) {
                Phase.RUN -> {
                    waveSpeed += waveAcceleration * dt
                    waveRightEdge += waveSpeed * dt
                    if (player.bounds.right < waveRightEdge) {
                        player.isDead = true
                    } else {
                        val spikeWidth = (waveRightEdge / room.w).coerceIn(0f, 0.98f)
                        if (spikeWidth > 0.02f) {
                            e.addSpikesFloor(0.00f, spikeWidth)
                        }
                        val toRemove = mutableListOf<RectF>()
                        for (i in staticPlatformsCount until e.platforms.size) {
                            val plat = e.platforms[i]
                            if (!RectF.intersects(player.bounds, plat)) {
                                if (pendingSpikes.none { it.bounds == plat }) {
                                    pendingSpikes.add(TimedSpike(plat))
                                }
                            } else {
                                pendingSpikes.removeAll { it.bounds == plat }
                            }
                        }

                        val iter = pendingSpikes.iterator()
                        while (iter.hasNext()) {
                            val ps = iter.next()
                            ps.timer -= dt
                            if (ps.timer <= 0f) {
                                val leftRel = ps.bounds.left / room.w
                                val rightRel = ps.bounds.right / room.w
                                val topRel = ps.bounds.top / room.h
                                e.addSpikesAt(leftRel, rightRel, topRel, SpikeDir.UP)
                                toRemove.add(ps.bounds)
                                iter.remove()
                            }
                        }
                        e.platforms.removeAll(toRemove)

                        val floorTop = room.h - room.wallThick
                        val onRoomFloor = player.bounds.bottom >= floorTop - 5f
                        if (onRoomFloor && !isPlayerOnAnyPlatform(e)) {
                            if (!fallPenalty) {
                                waveSpeed *= 1.5f
                                fallPenalty = true
                            }
                        } else {
                            fallPenalty = false
                        }
                    }
                }

                Phase.PAUSE_BEFORE_BULLETS -> {
                    pauseTimer -= dt
                    if (pauseTimer <= 0f) {
                        currentPhase = Phase.BULLETS
                        buildPhase2Arena(e, room)
                        staticPlatformsCount = e.platforms.size
                    }
                }

                Phase.BULLETS -> {
                    waveBulletTimer -= dt
                    if (waveBulletTimer <= 0f) {
                        isWaveBullet = false
                        bulletSpawnInterval = bulletSpawnIntervalNormal
                    }
                    if (!isWaveBullet && kotlin.random.Random.nextFloat() < 0.002f) {
                        isWaveBullet = true
                        bulletSpawnInterval = bulletSpawnIntervalFast
                        waveBulletTimer = 2.5f
                    }

                    bulletSpawnTimer += dt
                    if (bulletSpawnTimer >= bulletSpawnInterval) {
                        bulletSpawnTimer = 0f
                        val yPos = (0.2f + kotlin.random.Random.nextFloat() * 0.6f) * room.h
                        val bulletRect = RectF(room.w, yPos - 10f, room.w + 15f, yPos + 10f)
                        bullets.add(Bullet(bulletRect, room.w.toFloat(), yPos, bulletSpeed))
                    }

                    val bulletIterator = bullets.iterator()
                    while (bulletIterator.hasNext()) {
                        val b = bulletIterator.next()
                        b.x -= b.speed * dt
                        b.bounds.offsetTo(b.x, b.y - 10f)
                        if (b.x + b.bounds.width() < 0f) {
                            bulletIterator.remove()
                        } else if (RectF.intersects(player.bounds, b.bounds)) {
                            player.isDead = true
                        }
                    }

                    updatePlatformTimers(e, dt)

                    if (e.collectedKeys.size >= 5 && !exitDoorPlaced) {
                        e.door.isOpen = true
                        e.door.visuallyOpen = true
                        exitDoorPlaced = true
                    }
                    // Проверка прохождения уровня
                    if (!levelCompletedFlag && e.door.isOpen) {
                        if (RectF.intersects(player.bounds, e.door.bounds)) {
                            levelCompletedFlag = true
                            e.levelComplete = true
                        }
                    }
                }
            }
        }
    }

    private fun buildPhase2Arena(e: GameEngine, room: com.example.daitezachet.engine.Room) {
        e.platforms.clear()
        e.spikes.clear()
        e.keys.clear()
        e.button.hidden = true
        bullets.clear()
        platformTimers.clear()

        val platformsData = listOf(
            0.85f to 0.45f, 0.72f to 0.65f, 0.58f to 0.55f,
            0.44f to 0.70f, 0.30f to 0.50f, 0.16f to 0.60f, 0.05f to 0.45f
        )
        for ((x, y) in platformsData) {
            val left = x - 0.06f
            val right = x + 0.06f
            val top = y
            val bottom = y + 0.04f
            e.addPlatform(left, top, right, bottom)
        }

        e.placeKey(0.78f, 0.43f, id = 4, color = Color.rgb(255, 180, 100))
        e.placeKey(0.40f, 0.48f, id = 5, color = Color.rgb(255, 180, 100))

        for (i in 0 until e.platforms.size) {
            platformTimers.add(PlatformTimer(e.platforms[i], 0f))
        }
    }

    private fun updatePlatformTimers(e: GameEngine, dt: Float) {
        val toRemovePlat = mutableListOf<RectF>()
        val toRemoveTimer = mutableListOf<PlatformTimer>()

        for (timer in platformTimers) {
            if (RectF.intersects(e.player.bounds, timer.platform)) {
                timer.timeOnPlatform += dt
                if (timer.timeOnPlatform >= 1.2f) {
                    val leftRel = timer.platform.left / e.room.w
                    val rightRel = timer.platform.right / e.room.w
                    val topRel = timer.platform.top / e.room.h
                    e.addSpikesAt(leftRel, rightRel, topRel, SpikeDir.UP)
                    toRemovePlat.add(timer.platform)
                    toRemoveTimer.add(timer)
                }
            } else {
                timer.timeOnPlatform = 0f
            }
        }

        e.platforms.removeAll(toRemovePlat)
        platformTimers.removeAll(toRemoveTimer)
    }

    private fun isPlayerOnAnyPlatform(e: GameEngine): Boolean {
        for (plat in e.platforms) {
            if (RectF.intersects(e.player.bounds, plat)) {
                return true
            }
        }
        return false
    }

    override fun draw(canvas: Canvas, engine: GameEngine, paint: Paint) {
        if (currentPhase == Phase.BULLETS) {
            paint.color = Color.RED
            paint.style = Paint.Style.FILL
            for (bullet in bullets) {
                canvas.drawCircle(bullet.bounds.centerX(), bullet.bounds.centerY(), 6f, paint)
            }
            paint.color = Color.YELLOW
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            for (bullet in bullets) {
                canvas.drawCircle(bullet.bounds.centerX(), bullet.bounds.centerY(), 8f, paint)
            }
        }
    }
}