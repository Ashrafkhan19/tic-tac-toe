package com.example.tictactoe.domain.model

sealed interface Outcome {
    data object InProgress : Outcome
    data object Draw : Outcome
    data class Won(val winner: Mark, val line: List<Int>) : Outcome
}

private val WIN_LINES = listOf(
    listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
    listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
    listOf(0, 4, 8), listOf(2, 4, 6),
)

data class Board(val cells: List<Mark?> = List(CELL_COUNT) { null }) {
    val emptyCells: List<Int> get() = cells.indices.filter { cells[it] == null }

    fun place(index: Int, mark: Mark): Board {
        require(index in cells.indices && cells[index] == null) { "Cell $index is not available" }
        return Board(cells.toMutableList().also { it[index] = mark })
    }

    fun outcome(): Outcome {
        for (line in WIN_LINES) {
            val mark = cells[line[0]] ?: continue
            if (cells[line[1]] == mark && cells[line[2]] == mark) return Outcome.Won(mark, line)
        }
        return if (emptyCells.isEmpty()) Outcome.Draw else Outcome.InProgress
    }

    companion object {
        const val CELL_COUNT = 9
    }
}
