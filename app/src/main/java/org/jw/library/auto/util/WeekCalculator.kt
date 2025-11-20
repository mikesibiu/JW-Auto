package org.jw.library.auto.util

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class WeekCalculator(private val clock: Clock = Clock.systemDefaultZone()) {
    data class WeekInfo(val weekStart: LocalDate, val weekEnd: LocalDate, val label: String) {
        fun weekKey(): String = weekStart.toString()
    }

    fun currentWeek(today: LocalDate = LocalDate.now(clock)): WeekInfo = calculateWeek(today)

    fun weekForOffset(offsetWeeks: Long, baseDate: LocalDate = LocalDate.now(clock)): WeekInfo {
        val offsetDate = baseDate.plusWeeks(offsetWeeks)
        return calculateWeek(offsetDate)
    }

    private fun calculateWeek(date: LocalDate): WeekInfo {
        val startOfWeek = date.with(DayOfWeek.MONDAY)
        val endOfWeek = date.with(DayOfWeek.SUNDAY)
        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        val label = "${formatter.format(startOfWeek)} - ${formatter.format(endOfWeek)}"
        return WeekInfo(startOfWeek, endOfWeek, label)
    }
}
