package com.example.daitezachet.levels

import android.graphics.Color
import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.SpikeDir

/**
 * Уровень 5 — «Нужен только золотой».
 *
 * Три ключа: золотой (нужный), красный и зелёный (ложные).
 * Дверь открывается ТОЛЬКО от золотого ключа (id=1).
 * Шипы на потолке и боковые шипы на левой стене.
 * Кнопка скрыта — её нет.
 *
 *  ╔════════════════════════════════╗
 *  ║▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼ ▼║  ← шипы на потолке
 *  ║              ──P2──           ║
 *  ║◀◀    ──P1──  [G]  [R]  [★]  ║  ← боковые шипы слева; ★=золотой
 *  ║                               ║
 *  ╚════════════════════════════════╝
 */
class Level05 : Level() {
    override val number   = 5
    override val hintText = "Нужен только золотой ключ"

    override fun setup(engine: GameEngine) {
        engine.button.hidden = true

        // Платформа посередине — трамплин к потолочной зоне
        engine.addPlatform(0.20f, 0.55f, 0.52f)   // P1 — слева
        engine.addPlatform(0.40f, 0.30f, 0.72f)   // P2 — высокая по центру

        // Три ключа: зелёный (id=3), красный (id=2), золотой (id=1)
        engine.placeKey(0.30f, 0.55f, id = 3, color = Color.rgb(60, 220, 80))    // зелёный — ложный
        engine.placeKey(0.55f, 0.30f, id = 2, color = Color.rgb(255, 80, 80))    // красный — ложный
        engine.placeKey(0.80f, 0.30f, id = 1, color = Color.rgb(255, 215, 0))    // золотой — нужный

        // Шипы на потолке по всей ширине (кроме крайних зон)
        engine.addSpikesAt(0.06f, 0.92f, yr = 0.0f, dir = SpikeDir.DOWN)

        // Боковые шипы на левой стене — нельзя прижаться к левой стене прыгая
        engine.addSpikesWall(0.08f, 0.55f, xr = 0.04f, dir = SpikeDir.RIGHT)

        // Дверь открывается только от золотого ключа (id=1)
        engine.openDoorWhenKey(1)

        engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }
    }
}
