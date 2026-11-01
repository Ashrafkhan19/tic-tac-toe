package com.example.tictactoe.domain.model

enum class GameVariant(val displayName: String) {
    CLASSIC_3X3("Classic (3×3)"),
    GRID_4X4("Grid (4×4)"),
    GRID_5X5("Grid (5×5)"),
    ULTIMATE("Ultimate");
}

sealed interface GameMove {
    data class Grid(val index: Int) : GameMove
    data class Ultimate(val macroIndex: Int, val microIndex: Int) : GameMove
}
