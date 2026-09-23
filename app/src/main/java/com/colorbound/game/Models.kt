package com.colorbound.game

data class Level(
    val id: Int,
    val size: Int,
    val difficulty: String,
    val colors: Int,
    val grid: List<List<Int>>,
    val solution: Set<Cell>,
    val startingClues: Set<Cell>,
    val maxMistakes: Int = 3,
    val patternFamily: String = "",
    val estimatedDifficulty: Double = 1.0
)

data class Cell(val row: Int, val col: Int)

data class Reveal(val cell: Cell, val isItem: Boolean)

data class HintMessage(val title: String, val body: String, val cells: List<Cell> = emptyList())

enum class Screen { HOME, MAP, GAME, SETTINGS }

enum class Difficulty(val label: String) {
    VERY_EASY("Very Easy"), EASY("Easy"), NORMAL("Normal"), MEDIUM("Medium"),
    HARD("Hard"), VERY_HARD("Very Hard"), EXPERT("Expert"), MASTER("Master")
}

fun difficultyLabel(raw: String): String = raw.replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
