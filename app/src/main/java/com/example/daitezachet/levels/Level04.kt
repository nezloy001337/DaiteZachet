package com.example.daitezachet.levels

import com.example.daitezachet.engine.GameEngine

/**
 * Уровень 4 — "Кнопка не нужна".
 *
 * Весь пол в шипах — по земле не пройти.
 * Четыре ступени ведут наверх-вправо лестницей.
 * Ключ лежит на самой высокой ступени.
 *
 * Твист: кнопку нажать невозможно (она в шипах).
 *        Ключ сам открывает дверь — без кнопки.
 *
 *  ╔═══════════════════════════════╗
 *  ║                   [KEY]──P4  ║
 *  ║              ────P3          ║
 *  ║         ────P2               ║
 *  ║    ────P1                    ║
 *  ║▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲ ▲║
 *  ╚═══════════════════════════════╝
 */
class Level04 : Level() {
    override val number   = 4
    override val hintText = "Нажми кнопку?"   // вопрос намеренный

    override fun setup(engine: GameEngine) {
        // Лестница — каждая ступень короче и тоньше предыдущей
        //           x1r   yr    x2r   h
        engine.addPlatform(0.04f, 0.72f, 0.28f, h = 60f)  // длинная  толстая
        engine.addPlatform(0.30f, 0.55f, 0.46f, h = 30f)  // средняя  стандартная
        engine.addPlatform(0.48f, 0.38f, 0.58f, h = 14f)  // короткая тонкая
        engine.addPlatform(0.60f, 0.22f, 0.66f, h = 8f)   // крошечная нить — на ней ключ

        // Ключ на вершине лестницы
        engine.placeKey(0.80f, 0.22f)

        // Весь пол в шипах — идти по земле нельзя
        // Оставляем зазор у стен, чтобы не спавниться на шипах
        engine.addSpikesFloor(0.08f, 0.86f)

        // Ключ подобран → дверь открывается сама (кнопка тут ни при чём)
        engine.onUpdate = { e, _ ->
            if (e.player.hasKey && !e.door.isOpen) {
                e.door.isOpen = true
            }
        }

        // Кнопка — ловушка: нажатие закрывает дверь обратно
        engine.onButtonPressed = { e -> e.door.isOpen = false }

        engine.winCondition = { e ->
            e.player.hasKey && e.door.isOpen && isPlayerAtDoor(e)
        }
    }
}
