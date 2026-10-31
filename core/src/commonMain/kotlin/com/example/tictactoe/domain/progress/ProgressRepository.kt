package com.example.tictactoe.domain.progress

/** Loads and saves the player's [PlayerProgress]. */
interface ProgressRepository {
    /** Never throws: missing or unreadable data yields defaults (or whatever fields could still be read). */
    fun load(): PlayerProgress
    fun save(progress: PlayerProgress)
}

/** Keeps progress in memory only. For tests and previews; nothing survives the process. */
class InMemoryProgressRepository(initial: PlayerProgress = PlayerProgress()) : ProgressRepository {
    private var current: PlayerProgress = initial
    override fun load(): PlayerProgress = current
    override fun save(progress: PlayerProgress) {
        current = progress
    }
}
