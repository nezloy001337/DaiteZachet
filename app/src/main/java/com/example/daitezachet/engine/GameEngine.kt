package com.example.daitezachet.engine

import android.graphics.RectF

class GameEngine(val room: Room) {

    companion object {
        const val GRAVITY = 1600f
    }

    val player = Player(room.playerSpawnX, room.playerSpawnY)
    val button = Button(room.buttonRect)
    val door   = Door(room.doorRect)

    var key: Key? = null
    val spikes    = mutableListOf<Spike>()
    val platforms = mutableListOf<RectF>()   // задаются каждым уровнем в setup()

    var levelComplete = false

    // Input flags — written from UI thread, read from game thread (volatile for visibility)
    @Volatile var moveLeft     = false
    @Volatile var moveRight    = false
    @Volatile var jumpRequested = false

    // Level hooks — set by Level.setup()
    var onButtonPressed:  ((GameEngine) -> Unit)? = null
    var onButtonReleased: ((GameEngine) -> Unit)? = null
    var onUpdate:         ((GameEngine, Float) -> Unit)? = null
    var winCondition:     ((GameEngine) -> Boolean)? = null

    // -------------------------------------------------------------------------

    fun update(dt: Float) {
        if (levelComplete || player.isDead) return
        onUpdate?.invoke(this, dt)
        applyPhysics(dt)
        checkInteractions()
    }

    // -------------------------------------------------------------------------
    // Physics

    private fun applyPhysics(dt: Float) {
        player.vx = when {
            moveLeft && !moveRight  -> -Player.MOVE_SPEED
            moveRight && !moveLeft  -> Player.MOVE_SPEED
            else                    -> 0f
        }

        if (jumpRequested && player.isOnGround) {
            player.vy = Player.JUMP_FORCE
            player.isOnGround = false
        }
        jumpRequested = false

        player.vy += GRAVITY * dt
        player.vy  = player.vy.coerceAtMost(1200f)   // terminal velocity

        player.isOnGround = false

        // Horizontal step → resolve
        player.x += player.vx * dt
        val solids = buildSolids()
        solids.forEach { resolveH(it) }

        // Vertical step → resolve
        player.y += player.vy * dt
        solids.forEach { resolveV(it) }
    }

    private fun buildSolids(): List<RectF> {
        val list = ArrayList<RectF>(room.staticSolids.size + platforms.size + 1)
        list.addAll(room.staticSolids)
        list.addAll(platforms)
        if (!door.isOpen) list.add(door.bounds)
        return list
    }

    private fun resolveH(rect: RectF) {
        val pb = player.bounds
        if (!RectF.intersects(pb, rect)) return
        val overlapL = pb.right  - rect.left
        val overlapR = rect.right - pb.left
        val overlapT = pb.bottom - rect.top
        val overlapB = rect.bottom - pb.top
        // Если вертикальное перекрытие меньше горизонтального — игрок стоит
        // сверху/снизу поверхности; горизонтальная ось разрешается в resolveV.
        if (minOf(overlapT, overlapB) <= minOf(overlapL, overlapR)) return
        if (overlapL < overlapR) {
            player.x -= overlapL
            if (player.vx > 0f) player.vx = 0f
        } else {
            player.x += overlapR
            if (player.vx < 0f) player.vx = 0f
        }
    }

    private fun resolveV(rect: RectF) {
        val pb = player.bounds
        if (!RectF.intersects(pb, rect)) return
        val overlapT = pb.bottom - rect.top
        val overlapB = rect.bottom - pb.top
        val overlapL = pb.right  - rect.left
        val overlapR = rect.right - pb.left
        // Если горизонтальное перекрытие меньше вертикального — это боковое
        // столкновение со стеной; вертикальная ось разрешается в resolveH.
        if (minOf(overlapL, overlapR) < minOf(overlapT, overlapB)) return
        if (overlapT < overlapB) {
            player.y -= overlapT
            player.vy = 0f
            player.isOnGround = true
        } else {
            player.y += overlapB
            if (player.vy < 0f) player.vy = 0f
        }
    }

    // -------------------------------------------------------------------------
    // Interactions

    private fun checkInteractions() {
        val pb = player.bounds

        // Spikes → instant death
        for (spike in spikes) {
            if (RectF.intersects(pb, spike.bounds)) {
                player.isDead = true
                return
            }
        }

        // Key pickup
        key?.let {
            if (!it.isCollected && RectF.intersects(pb, it.bounds)) {
                it.isCollected  = true
                player.hasKey   = true
            }
        }

        // Button — only triggers when player stands on it and button is visible
        if (!button.hidden) {
            val wasPressed = button.isPressed
            button.isPressed = player.isOnGround && RectF.intersects(pb, button.bounds)
            when {
                button.isPressed && !wasPressed -> { button.pressCount++; onButtonPressed?.invoke(this) }
                !button.isPressed && wasPressed -> onButtonReleased?.invoke(this)
            }
        }

        // Win check (only relevant when door is open)
        if (door.isOpen) {
            val win = winCondition?.invoke(this) ?: playerInsideDoor(pb)
            if (win) levelComplete = true
        }
    }

    // -------------------------------------------------------------------------
    // Helpers для Level.setup() — координаты в долях (0f..1f) от размеров комнаты

    /**
     * Добавить платформу.
     * @param x1r   левый край  (0.0 – 1.0, доля ширины экрана)
     * @param yr    верхняя грань (0.0 – 1.0, доля высоты игровой области)
     * @param x2r   правый край (0.0 – 1.0)
     * @param h     толщина в пикселях (по умолчанию = wallThick ≈ 30px)
     */
    fun addPlatform(x1r: Float, yr: Float, x2r: Float, h: Float = room.wallThick) {
        platforms.add(RectF(room.w * x1r, room.h * yr, room.w * x2r, room.h * yr + h))
    }

    /**
     * Ряд шипов на полу между x1r и x2r (доли ширины).
     */
    fun addSpikesFloor(x1r: Float, x2r: Float) {
        val y = room.h - room.wallThick - 24f
        var x = room.w * x1r
        while (x + 28f <= room.w * x2r) { spikes.add(Spike(x, y)); x += 32f }
    }

    /**
     * Ряд шипов на любой горизонтальной поверхности.
     * yr — доля высоты поверхности (верх платформы или пол).
     * flipped = true → шипы смотрят вниз (с потолка / снизу платформы).
     */
    fun addSpikesAt(x1r: Float, x2r: Float, yr: Float, flipped: Boolean = false) {
        val y = if (flipped) room.h * yr + room.wallThick
                else         room.h * yr - 24f
        var x = room.w * x1r
        while (x + 28f <= room.w * x2r) { spikes.add(Spike(x, y, flipped = flipped)); x += 32f }
    }

    /**
     * Разместить ключ. xr/yr — центр в долях от размеров комнаты.
     * Ключ появляется чуть выше точки yr (удобно совмещать с yr платформы).
     */
    fun placeKey(xr: Float, yr: Float) {
        key = Key(room.w * xr - Key.SIZE / 2f, room.h * yr - Key.SIZE - 6f)
    }

    // -------------------------------------------------------------------------

    fun playerInsideDoor(pb: RectF = player.bounds): Boolean =
        pb.right  >= door.bounds.left  &&
        pb.left   <  door.bounds.right &&
        pb.bottom >  door.bounds.top   &&
        pb.top    <  door.bounds.bottom

    // -------------------------------------------------------------------------
    // Reset

    fun reset() {
        player.reset(room.playerSpawnX, room.playerSpawnY)
        button.isPressed  = false
        button.pressCount = 0
        button.hidden     = false
        door.isOpen       = false
        key?.isCollected  = false
        key               = null
        spikes.clear()
        platforms.clear()
        levelComplete  = false
        moveLeft       = false
        moveRight      = false
        jumpRequested  = false
        // Callbacks are NOT cleared — Level.setup() will re-set them after reset()
    }
}
