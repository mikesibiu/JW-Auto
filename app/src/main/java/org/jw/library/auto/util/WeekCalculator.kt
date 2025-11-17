package org.jw.library.auto.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeekCalculator {
    data class WeekInfo(val weekStart: LocalDate, val weekEnd: LocalDate, val label: String)

    fun currentWeek(today: LocalDate = LocalDate.now()): WeekInfo {
        val startOfWeek = today.with(DayOfWeek.MONDAY)
        val endOfWeek = today.with(DayOfWeek.SUNDAY)
        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        val label = "${formatter.format(startOfWeek)} - ${formatter.format(endOfWeek)}"
        return WeekInfo(startOfWeek, endOfWeek, label)
    }
}
