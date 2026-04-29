package com.example.daitezachet.levels

import android.graphics.Color
import com.example.daitezachet.engine.GameEngine

/**
 * Уровень 5 — «Ложный выбор».
 *
 * Красный и синий кристаллы — правильные.
 * Золотой кристалл выглядит как лёгкая награда на очевидном пути,
 * но при подборе мгновенно убивает игрока.
 *
 * Смысл уровня: не всё очевидное ведёт к победе
 */

class Level05 : Level() {
    override val number   = 5
    override val hintText = "Соберите 2 ключа"

    override fun setup(engine: GameEngine) {
        val trapKeyId = 99
        val requiredKeys = setOf(1, 2)

        engine.button.hidden = true

        // Левая колонна вверх
        engine.addPlatform(0.05f, 0.78f, 0.20f, 0.82f)  // старт
        engine.addPlatform(0.05f, 0.62f, 0.20f, 0.66f)  // красный
        engine.addPlatform(0.05f, 0.46f, 0.20f, 0.50f)

        // Верхняя перемычка с золотым
        engine.addPlatform(0.30f, 0.38f, 0.70f, 0.42f)  // золотой в центре!

        // Правая колонна вниз
        engine.addPlatform(0.75f, 0.46f, 0.90f, 0.50f)
        engine.addPlatform(0.75f, 0.62f, 0.90f, 0.66f)  // синий
        engine.addPlatform(0.75f, 0.78f, 0.90f, 0.82f)  // выход

        engine.placeKey(0.12f, 0.62f, id = 1, color = Color.rgb(255, 80, 80))
        engine.placeKey(0.50f, 0.38f, id = trapKeyId, color = Color.rgb(255, 215, 0))
        engine.placeKey(0.82f, 0.62f, id = 2, color = Color.rgb(80, 140, 255))

        // Шипы на полу под верхними платформами
        // Под левой колонной
        engine.addSpikesFloor(0.05f, 0.20f)
        // Под верхней перемычкой с золотым кристаллом
        engine.addSpikesFloor(0.30f, 0.70f)
        // Под правой колонной
        engine.addSpikesFloor(0.75f, 0.90f)

        // Дополнительные шипы на полу в центре (как в оригинале)
        engine.addSpikesFloor(0.22f, 0.73f)

        engine.doorCondition = { e -> requiredKeys.all { it in e.collectedKeys } }
        engine.onUpdate = { e, _ ->
            if (trapKeyId in e.collectedKeys) e.player.isDead = true
        }
        engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }
    }
}
