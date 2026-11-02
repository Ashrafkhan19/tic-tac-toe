package com.example.tictactoe.domain.focus

data class MatrixPuzzle(
    val id: String,
    val title: String,
    val ruleDescription: String,
    val gridIcons: List<String>, // 4 icons for 2x2 grid (top-left, top-right, bottom-left, empty "?")
    val options: List<String>,
    val correctOptionIndex: Int
)

/**
 * Visual Matrix Reasoning Engine (Raven's Progressive Matrices).
 * Tests non-verbal abstract fluid reasoning and pattern induction.
 */
class MatrixLogicEngine {
    val allPuzzles = listOf(
        MatrixPuzzle(
            id = "rotation-1",
            title = "Arrow Rotation",
            ruleDescription = "Each shape rotates clockwise by 90 degrees.",
            gridIcons = listOf("↑", "→", "↓", "?"),
            options = listOf("←", "→", "↑", "↓"),
            correctOptionIndex = 0
        ),
        MatrixPuzzle(
            id = "progression-1",
            title = "Dot Progression",
            ruleDescription = "Each step adds one circle.",
            gridIcons = listOf("●", "●●", "●●●", "?"),
            options = listOf("●●", "●●●●", "●", "●●●"),
            correctOptionIndex = 1
        ),
        MatrixPuzzle(
            id = "morph-1",
            title = "Shape Progression",
            ruleDescription = "Sides increase: Triangle (3) -> Square (4) -> Pentagon (5) -> ?",
            gridIcons = listOf("▲", "■", "⬟", "?"),
            options = listOf("●", "⬢ (Hexagon)", "▲", "◆"),
            correctOptionIndex = 1
        ),
        MatrixPuzzle(
            id = "shading-1",
            title = "Polarity Inversion",
            ruleDescription = "Opposite shapes invert fill state.",
            gridIcons = listOf("○", "●", "□", "?"),
            options = listOf("■", "○", "▲", "△"),
            correctOptionIndex = 0
        )
    )

    fun submitAnswer(puzzleId: String, chosenIndex: Int): Boolean {
        val puzzle = allPuzzles.find { it.id == puzzleId } ?: return false
        return chosenIndex == puzzle.correctOptionIndex
    }
}

