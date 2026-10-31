package com.example.tictactoe.domain.progress

interface ProgressRepository {
    fun load(): PlayerProgress
    fun save(progress: PlayerProgress)
}

class InMemoryProgressRepository(initial: PlayerProgress = PlayerProgress()) : ProgressRepository {
    private var current: PlayerProgress = initial
    override fun load(): PlayerProgress = current
    override fun save(progress: PlayerProgress) {
        current = progress
    }
}
