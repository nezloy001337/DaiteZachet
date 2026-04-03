package com.example.daitezachet.levels

import com.example.daitezachet.engine.GameEngine

/**
 * Уровень 2 — Ключ и шипы.
 *
 * Шипы закрывают большую часть пола. Ключ лежит на высокой платформе.
 * Кнопка открывает дверь ТОЛЬКО если у игрока есть ключ.
 *
 * Маршрут: прыжок на P1 → прыжок на P2 (ключ) →
 *          спрыгнуть вправо от шипов → дойти до кнопки → выйти.
 */
class Level02 : Level() {
    override val number   = 2
    override val hintText = "Сначала найди ключ"

    override fun setup(engine: GameEngine) {
        engine.addPlatform(0.04f, 0.68f, 0.44f)   // P1
        engine.addPlatform(0.52f, 0.42f, 0.92f)   // P2

        // Ключ на P2
        engine.placeKey(0.72f, 0.42f)

        // Шипы от стены до правее кнопки — оставляем зазор у спавна и у кнопки
        engine.addSpikesFloor(0.10f, 0.38f)        // опасная зона пола

        engine.onButtonPressed = { e ->
            if (e.player.hasKey) e.door.isOpen = true
        }

        engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }
    }
}
