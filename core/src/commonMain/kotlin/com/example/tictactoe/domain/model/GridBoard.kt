package com.example.tictactoe.domain.model

data class GridBoard(
    val size: Int,
    val winLength: Int,
    val cells: List<Mark?> = List(size * size) { null }
) {
    init {
        require(size >= 3) { "Size must be at least 3" }
        require(winLength in 3..size) { "Win length must be between 3 and $size" }
        require(cells.size == size * size) { "Cells count must be ${size * size}" }
    }

    val emptyCells: List<Int> get() = cells.indices.filter { cells[it] == null }

    val winLines: List<List<Int>> = computeWinLines(size, winLength)

    fun place(index: Int, mark: Mark): GridBoard {
        require(index in cells.indices && cells[index] == null) { "Cell $index is not available" }
        return copy(cells = cells.toMutableList().also { it[index] = mark })
    }

    fun outcome(): Outcome {
        for (line in winLines) {
            val first = cells[line[0]] ?: continue
            if (line.all { cells[it] == first }) {
                return Outcome.Won(first, line)
            }
        }
        return if (emptyCells.isEmpty()) Outcome.Draw else Outcome.InProgress
    }

    companion object {
        private val cache = mutableMapOf<Pair<Int, Int>, List<List<Int>>>()

        fun computeWinLines(size: Int, winLength: Int): List<List<Int>> =
            cache.getOrPut(size to winLength) {
                val lines = mutableListOf<List<Int>>()
                // Rows
                for (r in 0 until size) {
                    for (c in 0..size - winLength) {
                        lines.add((0 until winLength).map { r * size + (c + it) })
                    }
                }
                // Columns
                for (c in 0 until size) {
                    for (r in 0..size - winLength) {
                        lines.add((0 until winLength).map { (r + it) * size + c })
                    }
                }
                // Main Diagonals (\)
                for (r in 0..size - winLength) {
                    for (c in 0..size - winLength) {
                        lines.add((0 until winLength).map { (r + it) * size + (c + it) })
                    }
                }
                // Anti-Diagonals (/)
                for (r in 0..size - winLength) {
                    for (c in winLength - 1 until size) {
                        lines.add((0 until winLength).map { (r + it) * size + (c - it) })
                    }
                }
                lines
            }
    }
}
