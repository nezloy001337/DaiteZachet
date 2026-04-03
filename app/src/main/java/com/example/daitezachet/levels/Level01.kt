package com.example.daitezachet.levels

import com.example.daitezachet.engine.GameEngine

/**
 * Уровень 1 — Обучение.
 * Нет шипов, нет ключа. Нажми кнопку → дверь открывается → выйди.
 *
 * Платформы:
 *   P1 — длинная левая, средняя высота   (удобна для разгона)
 *   P2 — правая,  высокая               (над ней нет шипов, просто пейзаж)
 */
class Level01 : Level() {
    override val number   = 1
    override val hintText = "Нажми кнопку"

    override fun setup(engine: GameEngine) {
        engine.addPlatform(0.04f, 0.68f, 0.44f)   // P1: левая, чуть выше середины
        engine.addPlatform(0.52f, 0.42f, 0.92f)   // P2: правая, высоко

        engine.onButtonPressed = { e -> e.door.isOpen = true }

        engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }
    }
}
