package com.example.tictactoe.domain.model

enum class Mark {
    X, O;

    val other: Mark get() = if (this == X) O else X
}

enum class Mode { TwoPlayer, VsComputer }

enum class Difficulty { Easy, Medium, Unbeatable }
