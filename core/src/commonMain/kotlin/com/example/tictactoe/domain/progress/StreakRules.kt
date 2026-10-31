package com.example.tictactoe.domain.progress

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.serialization.Serializable

@Serializable
data class StreakState(
    val current: Int = 0,
    val best: Int = 0,
    val freezes: Int = 0,
    val lastCompleted: LocalDate? = null,
)

private fun StreakState.gapDays(to: LocalDate): Int? =
    lastCompleted?.let { it.daysUntil(to) }

/** Records a daily-challenge completion on [on]. Never lowers or double-counts for earlier/same dates. */
fun StreakState.completed(on: LocalDate): StreakState {
    val gap = gapDays(on)
    var newFreezes = freezes
    val newCurrent = when {
        gap == null -> 1
        gap <= 0 -> return this
        gap == 1 -> current + 1
        gap == 2 && freezes > 0 -> { newFreezes -= 1; current + 1 }
        else -> 1
    }
    if (newCurrent % 7 == 0) newFreezes = minOf(1, newFreezes + 1)
    return copy(
        current = newCurrent,
        best = maxOf(best, newCurrent),
        freezes = newFreezes,
        lastCompleted = on,
    )
}

/** The streak to show on [today]: 0 once it has lapsed beyond what a banked freeze can bridge. */
fun StreakState.displayed(today: LocalDate): Int {
    val gap = gapDays(today) ?: return 0
    return if (gap <= 1 || (gap == 2 && freezes > 0)) current else 0
}

/**
 * True when a streak still shown on [today] is waiting on today's challenge: last completed one or two
 * days ago. False for a lapsed streak and when [lastCompleted] is after [today] (clock moved backwards).
 */
fun StreakState.isAtRisk(today: LocalDate): Boolean =
    displayed(today) > 0 && gapDays(today).let { it != null && it in 1..2 }
