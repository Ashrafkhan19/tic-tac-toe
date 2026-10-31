package com.example.tictactoe.domain.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

fun interface TodayProvider {
    fun today(): LocalDate
}

/** Today's date in the device's current default time zone. */
class SystemTodayProvider : TodayProvider {
    override fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
}
