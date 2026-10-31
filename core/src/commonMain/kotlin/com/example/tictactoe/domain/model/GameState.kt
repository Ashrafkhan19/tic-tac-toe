package com.example.tictactoe.domain.model

/** One round of tic-tac-toe: the board plus whose turn it is. Immutable; moves return a new state. */
data class GameState(
    val board: Board = Board(),
    val turn: Mark = Mark.X,
) {
    val outcome: Outcome = board.outcome()
    val isOver: Boolean get() = outcome != Outcome.InProgress

    /** Returns the same instance if the move is illegal or the game is over. */
    fun play(index: Int): GameState {
        if (isOver || index !in board.cells.indices || board.cells[index] != null) return this
        return GameState(board.place(index, turn), turn.other)
    }
}
