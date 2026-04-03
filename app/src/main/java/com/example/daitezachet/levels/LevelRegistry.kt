package com.example.daitezachet.levels

object LevelRegistry {
    private val levels: List<Level> = listOf(
        Level01(),
        Level02(),
        Level03(),
        Level04(),
        Level05()
    )

    val count: Int get() = levels.size

    /** Returns the level for the given 1-based number. */
    fun get(number: Int): Level = levels[number - 1]
}
