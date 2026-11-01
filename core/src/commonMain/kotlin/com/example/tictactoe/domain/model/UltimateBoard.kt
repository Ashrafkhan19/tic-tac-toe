package com.example.tictactoe.domain.model

data class UltimateBoard(
    val subBoards: List<GridBoard> = List(9) { GridBoard(size = 3, winLength = 3) },
    val macroBoard: GridBoard = GridBoard(size = 3, winLength = 3),
    val activeMacroIndex: Int? = null,
) {
    init {
        require(subBoards.size == 9) { "UltimateBoard requires 9 subBoards" }
    }

    fun legalMoves(): List<GameMove.Ultimate> {
        if (outcome() !is Outcome.InProgress) return emptyList()
        val targetIndices = if (activeMacroIndex != null && subBoards[activeMacroIndex].outcome() is Outcome.InProgress) {
            listOf(activeMacroIndex)
        } else {
            subBoards.indices.filter { subBoards[it].outcome() is Outcome.InProgress }
        }
        return targetIndices.flatMap { macro ->
            subBoards[macro].emptyCells.map { micro -> GameMove.Ultimate(macro, micro) }
        }
    }

    fun place(move: GameMove.Ultimate, mark: Mark): UltimateBoard {
        require(move in legalMoves()) { "Illegal move $move in UltimateBoard" }
        val updatedSub = subBoards[move.macroIndex].place(move.microIndex, mark)
        val newSubBoards = subBoards.toMutableList().also { it[move.macroIndex] = updatedSub }

        val subOutcome = updatedSub.outcome()
        val newMacroBoard = when (subOutcome) {
            is Outcome.Won -> macroBoard.place(move.macroIndex, subOutcome.winner)
            else -> macroBoard
        }

        val nextTarget = move.microIndex
        val nextActive = if (newSubBoards[nextTarget].outcome() is Outcome.InProgress) {
            nextTarget
        } else {
            null // Free choice if target sub-board is complete
        }

        return copy(
            subBoards = newSubBoards,
            macroBoard = newMacroBoard,
            activeMacroIndex = nextActive,
        )
    }

    fun outcome(): Outcome {
        val macroOutcome = macroBoard.outcome()
        if (macroOutcome is Outcome.Won) return macroOutcome
        val anyInProgress = subBoards.any { it.outcome() is Outcome.InProgress }
        return if (!anyInProgress || macroOutcome is Outcome.Draw) Outcome.Draw else Outcome.InProgress
    }
}
