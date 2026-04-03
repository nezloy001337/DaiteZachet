package com.example.daitezachet.levels

import com.example.daitezachet.engine.GameEngine
import com.example.daitezachet.engine.Spike

/**
 * Уровень 3 — Ловушка.
 *
 * Дверь изначально ОТКРЫТА. Ключ на P2.
 * Нажатие кнопки ЗАКРЫВАЕТ дверь и засыпает весь пол шипами навсегда.
 * Нужно собрать ключ и пройти в дверь, ни разу не наступив на кнопку.
 *
 * Хитрость: кнопка стоит на полу по дороге к двери — нужно обходить через P1.
 */
class Level03 : Level() {
    override val number   = 3
    override val hintText = "Не трогай кнопку!"

    override fun setup(engine: GameEngine) {
        engine.addPlatform(0.04f, 0.68f, 0.44f, 0.72f)   // P1
        engine.addPlatform(0.52f, 0.42f, 0.92f, 0.46f)   // P2

        engine.placeKey(0.72f, 0.42f)
        engine.door.isOpen = true

        engine.onButtonPressed = { e ->
            e.door.isOpen = false
            if (e.spikes.isEmpty()) {
                // Заспавнить шипы по всему полу
                val y = e.room.h - e.room.wallThick - 24f
                var x = e.room.w * 0.04f
                while (x + 28f <= e.room.w * 0.90f) {
                    e.spikes.add(Spike(x, y)); x += 32f
                }
            }
        }

        engine.winCondition = { e ->
            e.player.hasKey && e.door.isOpen && isPlayerAtDoor(e)
        }
    }
}
