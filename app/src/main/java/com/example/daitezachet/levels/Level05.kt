package com.example.daitezachet.levels

import android.graphics.Color
import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.SpikeDir

/**
 * Уровень 5 — «Только золотой».
 *
 * Дверь открывается только от золотого ключа (id=1).
 * Кнопки нет.
 *
 * Особенности:
 *  - P1: шипы на правой половине верхней грани → нельзя встать правее середины
 *  - P_col: вертикальная колонна-стена; шипы на ПРАВОМ боку → нельзя подходить справа
 *  - P2: шипы снизу (висят) → больно прыгать снизу в платформу; сверху безопасно
 *
 *  ╔══════════════════════════════════╗
 *  ║                [★]              ║  ← золотой ключ
 *  ║                ──────P2──────   ║
 *  ║                ▼▼▼▼▼▼▼▼▼▼▼▼    ║  ← шипы снизу P2
 *  ║   ──P1──  │▶▶                  ║  ← вертикальная колонна, шипы справа
 *  ║   ▲▲▲P1   │                    ║  ← шипы на правой части P1
 *  ╚══════════════════════════════════╝
 *
 *  Маршрут: запрыгнуть на левую часть P1 → перепрыгнуть колонну сверху →
 *           приземлиться на P2 сверху → взять ключ → к двери.
 */
class Level05 : Level() {
    override val number   = 5
    override val hintText = "Только золотой"

    override fun setup(engine: GameEngine) {
        engine.button.hidden = true

        // P1 — горизонтальная платформа слева
        engine.addPlatform(0.08f, 0.65f, 0.38f, 0.69f)

        // Шипы на правой половине верхней грани P1 — только левая часть безопасна
        engine.addSpikesAt(0.23f, 0.38f, yr = 0.65f, dir = SpikeDir.UP)

        // P_col — вертикальная колонна по центру (от y=0.40 до y=0.92)
        engine.addPlatform(0.44f, 0.40f, 0.49f, 0.92f)

        // Шипы на ПРАВОМ боку колонны — нельзя подходить справа
        engine.addSpikesWall(0.40f, 0.92f, xr = 0.49f, dir = SpikeDir.RIGHT)

        // P2 — высокая платформа справа от колонны (на одном уровне с её вершиной)
        engine.addPlatform(0.53f, 0.40f, 0.90f, 0.44f)

        // Шипы СНИЗУ P2 — больно прыгать снизу, сверху приземляться можно
        engine.addSpikesAt(0.53f, 0.90f, yr = 0.40f, dir = SpikeDir.DOWN)

        // Золотой ключ на P2
        engine.placeKey(0.72f, 0.40f, id = 1, color = Color.rgb(255, 215, 0))

        // Дверь открывается только от золотого ключа
        engine.openDoorWhenKey(1)

        engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }
    }
}
