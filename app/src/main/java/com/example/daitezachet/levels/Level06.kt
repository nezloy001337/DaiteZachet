package com.example.daitezachet.levels

import android.graphics.Color
import android.graphics.RectF
import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.SpikeDir

/**
 * Уровень 6 — «Движущиеся платформы».
 *
 * Механики: вертикальные платформы с разными фазами движения,
 *           кнопка-ловушка (ускоряет платформы до нереальной скорости),
 *           правильный порядок действий открывает горизонтальную фазу.
 *
 * Вертикальная фаза (до кнопки):
 *   7 вертикальных платформ над шипами пола и потолочными шипами.
 *   Нужно добраться до правой полки и взять ключ.
 *
 * Ловушка — кнопка до ключа:
 *   Дверь открывается, но вертикальные платформы резко ускоряются — пройти почти нереально.
 *
 * Правильный порядок:
 *   1. Пройти 7 вертикальных платформ → взять ключ на правой полке
 *   2. Вернуться к кнопке → нажать
 *   3. Три горизонтальные платформы, каждая курсирует в своей трети маршрута.
 *      Нужно ловить момент и перепрыгивать: левая полка → A → B → C → правая полка → дверь.
 *
 * Схема вертикальной фазы (движение вверх-вниз):
 *   ╔═════════════════════════════════════════════════════════════╗
 *   ║  ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ потолочные шипы ║
 *   ║     │     │     │     │     │     │     │                   ║
 *   ║     │     │     │     │     │     │     │  7 платформ       ║
 *   ║     │     │     │     │     │     │     │  движутся         ║
 *   ║     │     │     │     │     │     │     │  ВВЕРХ-ВНИЗ       ║
 *   ║     │     │     │     │     │     │     │                   ║
 *   ║ [кнп]                                      [ключ]           ║
 *   ║  ═══                                        ═══          ▶  ║
 *   ║                                                             ║
 *   ║     ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ шипы пола      ║
 *   ╚═════════════════════════════════════════════════════════════╝
 *
 * Схема горизонтальной фазы (после нажатия кнопки с ключом):
 *   ╔═════════════════════════════════════════════════════════════╗
 *   ║  ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼ потолочные шипы ║
 *   ║                                                             ║
 *   ║  [кнп]   ←──A──→    │   ←──B──→    │   ←──C──→   [ключ]   ║
 *   ║   ═══     ┌────┐    │    ┌────┐    │    ┌────┐     ═══  ▶  ║
 *   ║           └────┘         └────┘         └────┘             ║
 *   ║     ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲ шипы пола      ║
 *   ╚═════════════════════════════════════════════════════════════╝
 *   ▶ — дверь  ▼ — потолочные шипы  ▲ — шипы пола  A, B, C — горизонтальные платформы
 */
class Level06 : Level() {
    override val number   = 6
    override val hintText = "Советую сначала ключ, потом кнопку"

    // ── Внутренние модели движения ────────────────────────────────────────────

    private data class VMover(
        var y: Float,
        val minY: Float,
        val maxY: Float,
        var speed: Float,
        var goingDown: Boolean
    )

    private data class HMover(
        var x: Float,
        val minX: Float,
        val maxX: Float,
        val speed: Float,
        var goingRight: Boolean,
        val y: Float,
        val platW: Float,
        val platH: Float
    )

    // ── Конфигурация вертикальных платформ ────────────────────────────────────

    // X-диапазоны вертикальных платформ (ширина каждой ≈ 0.065w)
    private val vPlatXRanges = listOf(
        0.146f to 0.211f,
        0.247f to 0.312f,
        0.348f to 0.413f,
        0.449f to 0.514f,
        0.550f to 0.615f,
        0.651f to 0.716f,
        0.752f to 0.817f
    )

    // Начальные фазы (0..1 в диапазоне minY..maxY) и скорости для каждой платформы
    private val vPlatPhases     = listOf(0.00f, 0.60f, 0.25f, 0.78f, 0.42f, 0.88f, 0.15f)
    private val vPlatBaseSpeeds = listOf(185f,  245f,  205f,  265f,  220f,  250f,  195f)

    // 4.2x — делает ловушку почти непроходимой, но технически возможной
    private val trapSpeedMultiplier = 4.2f

    // ── Конфигурация полок ────────────────────────────────────────────────────

    private val shelfYr       = 0.72f   // Y верхней грани полок (доля высоты)
    private val shelfThickRel = 0.04f   // толщина полки (доля высоты)
    private val leftShelfXr   = 0.04f to 0.11f   // левая полка (кнопка)
    private val rightShelfXr  = 0.85f to 0.96f   // правая полка (ключ)

    // ── Константы для определения «игрок на платформе» ────────────────────────

    private val onPlatformTopTolerance    = 4f   // px выше верхней грани
    private val onPlatformBottomTolerance = 8f   // px ниже верхней грани

    // ── Высота платформ (и вертикальных, и горизонтальных) ────────────────────

    private val platH = 12f

    // ── Изменяемое состояние уровня (всегда сбрасывается в начале setup) ──────

    private val vMovers = mutableListOf<VMover>()
    private val hMovers = mutableListOf<HMover>()

    private var trapTriggered    = false
    private var correctTriggered = false

    // Количество статических платформ — фиксируется после добавления полок в setup(),
    // чтобы onUpdate знал, сколько платформ не трогать при обновлении.
    private var staticPlatCount = 0

    // ── Основная логика ───────────────────────────────────────────────────────

    override fun setup(engine: GameEngine) {
        // Сброс изменяемого состояния
        vMovers.clear()
        hMovers.clear()
        trapTriggered    = false
        correctTriggered = false

        val h = engine.room.h
        val w = engine.room.w

        // Инициализация вертикальных мувéров
        val minY = h * 0.05f
        val maxY = h * 0.76f
        for (i in vPlatXRanges.indices) {
            val startY = minY + (maxY - minY) * vPlatPhases[i]
            vMovers.add(VMover(startY, minY, maxY, vPlatBaseSpeeds[i], goingDown = vPlatPhases[i] >= 0.5f))
        }

        // Шипы на потолке и полу
        engine.addSpikesAt(0.03f, 0.97f, yr = 0.00f, dir = SpikeDir.DOWN)
        engine.addSpikesFloor(0.09f, 0.88f)

        // Левая полка — кнопка
        engine.addPlatform(leftShelfXr.first, shelfYr, leftShelfXr.second, shelfYr + shelfThickRel)
        engine.placeButton(xr = leftShelfXr.first, yr = shelfYr)

        // Правая полка — золотой ключ
        engine.addPlatform(rightShelfXr.first, shelfYr, rightShelfXr.second, shelfYr + shelfThickRel)
        engine.placeKey(0.89f, shelfYr, id = 1, color = Color.rgb(255, 215, 0))

        // Запоминаем количество статических платформ, чтобы onUpdate не трогал их при очистке
        staticPlatCount = engine.platforms.size

        // Кнопка: правильный порядок (ключ есть) → горизонтальная фаза;
        //         ловушка (ключа нет) → вертикальные платформы ускоряются
        engine.onButtonPressed = { e ->
            if (e.player.hasKey) {
                if (!correctTriggered) {
                    correctTriggered = true
                    e.door.isOpen = true
                    buildHMovers(w, h)
                }
            } else {
                if (!trapTriggered) {
                    trapTriggered = true
                    e.door.isOpen = true
                    vMovers.forEach { it.speed *= trapSpeedMultiplier }
                }
            }
        }

        engine.onUpdate = { e, dt ->
            // Очищаем все движущиеся платформы, оставляя только статические полки
            if (e.platforms.size > staticPlatCount)
                e.platforms.subList(staticPlatCount, e.platforms.size).clear()

            if (correctTriggered) {
                tickHMovers(e, dt)
            } else {
                tickVMovers(e, dt)
            }
        }

        engine.winCondition = { e ->
            e.door.isOpen && e.player.hasKey && isPlayerAtDoor(e)
        }
    }

    // ── Тик горизонтальной фазы ───────────────────────────────────────────────

    private fun tickHMovers(e: GameEngine, dt: Float) {
        for (m in hMovers) {
            val prevX = m.x

            if (m.goingRight) {
                m.x += m.speed * dt
                if (m.x >= m.maxX) { m.x = m.maxX; m.goingRight = false }
            } else {
                m.x -= m.speed * dt
                if (m.x <= m.minX) { m.x = m.minX; m.goingRight = true }
            }

            e.platforms.add(RectF(m.x, m.y, m.x + m.platW, m.y + m.platH))

            // Тащим игрока вместе с платформой
            if (e.player.isOnGround && isPlayerOnHMover(e, m, prevX)) {
                e.player.x += (m.x - prevX)
            }
        }
    }

    // ── Тик вертикальной фазы ─────────────────────────────────────────────────

    private fun tickVMovers(e: GameEngine, dt: Float) {
        for (i in vMovers.indices) {
            val m = vMovers[i]
            val prevY = m.y

            if (m.goingDown) {
                m.y += m.speed * dt
                if (m.y >= m.maxY) { m.y = m.maxY; m.goingDown = false }
            } else {
                m.y -= m.speed * dt
                if (m.y <= m.minY) { m.y = m.minY; m.goingDown = true }
            }

            val (x1r, x2r) = vPlatXRanges[i]
            e.platforms.add(RectF(e.room.w * x1r, m.y, e.room.w * x2r, m.y + platH))

            // Тащим игрока вместе с платформой
            if (e.player.isOnGround && isPlayerOnVMover(e, i, prevY)) {
                e.player.y += (m.y - prevY)
            }
        }
    }

    // ── Вспомогательные функции проверки «игрок на платформе» ─────────────────

    private fun isPlayerOnHMover(e: GameEngine, m: HMover, prevX: Float): Boolean {
        val pb = e.player.bounds
        return pb.bottom >= m.y - onPlatformTopTolerance &&
                pb.bottom <= m.y + onPlatformBottomTolerance &&
                pb.right  >  prevX &&
                pb.left   <  prevX + m.platW
    }

    private fun isPlayerOnVMover(e: GameEngine, index: Int, prevY: Float): Boolean {
        val pb = e.player.bounds
        val (x1r, x2r) = vPlatXRanges[index]
        val prevTop   = prevY
        val prevLeft  = e.room.w * x1r
        val prevRight = e.room.w * x2r
        return pb.bottom >= prevTop  - onPlatformTopTolerance &&
                pb.bottom <= prevTop  + onPlatformBottomTolerance &&
                pb.right  >  prevLeft &&
                pb.left   <  prevRight
    }

    // ── Построение горизонтальных мувéров ─────────────────────────────────────

    /**
     * Создаёт три горизонтальные платформы.
     * Маршрут между полками делится на три равные зоны; каждая платформа
     * курсирует только в своей зоне, что даёт чёткий ритм прыжков A→B→C.
     */
    private fun buildHMovers(w: Float, h: Float) {
        hMovers.clear()

        val platY = h * shelfYr
        val platW = w * (vPlatXRanges[0].second - vPlatXRanges[0].first)  // та же ширина, что у вертикальных

        val spanLeft  = w * leftShelfXr.second   // правый край левой полки
        val spanRight = w * rightShelfXr.first   // левый край правой полки
        val zoneW     = (spanRight - spanLeft) / 3f

        val speeds      = floatArrayOf(280f, 320f, 295f)
        val startPhases = floatArrayOf(0.0f, 0.55f, 0.25f)
        val startRight  = booleanArrayOf(true, false, true)

        for (i in 0..2) {
            val minX   = spanLeft + i * zoneW
            val maxX   = minX + zoneW - platW
            val startX = minX + (maxX - minX) * startPhases[i]
            hMovers.add(
                HMover(
                    x          = startX,
                    minX       = minX,
                    maxX       = maxX,
                    speed      = speeds[i],
                    goingRight = startRight[i],
                    y          = platY,
                    platW      = platW,
                    platH      = platH
                )
            )
        }
    }
}