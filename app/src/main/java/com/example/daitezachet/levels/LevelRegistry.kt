package com.example.daitezachet.levels

object LevelRegistry {
    private val levels: List<Level> = listOf(
        Level02(), // 1 Андриана легкий 1
        Level03(), // 3 Ани легкий 2
        Level04(), // 4 Викин ниже среднего 5
        Level05(), // 5 средне Полины 8
        Level06(), // 6 Даши сложновато 12
        Level07(), // Стаса ниже среднего 4
        Level08(), // Насти сложновато 11
        Level09(), //легкий Антона 3
        Level15(), // сложный Миши 15
    )


    val count: Int get() = levels.size

    /** Returns the level for the given 1-based number. */
    fun get(number: Int): Level = levels[number - 1]
}
