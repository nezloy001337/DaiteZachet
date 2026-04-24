package com.example.daitezachet.levels

object LevelRegistry {
    private val levels: List<Level> = listOf(
        Level02(), // 1
        Level03(), // 3
        Level04(), // 4
        Level05(), // 5
        Level06(), // 6
        Level07(),
    )


    val count: Int get() = levels.size

    /** Returns the level for the given 1-based number. */
    fun get(number: Int): Level = levels[number - 1]
}
