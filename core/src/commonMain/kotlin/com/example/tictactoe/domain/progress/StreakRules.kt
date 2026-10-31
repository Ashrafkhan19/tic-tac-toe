package com.example.tictactoe.domain.progress

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class StreakState(
    val current: Int = 0,
    val best: Int = 0,
    val freezes: Int = 0,
    val lastCompleted: LocalDate? = null,
)
