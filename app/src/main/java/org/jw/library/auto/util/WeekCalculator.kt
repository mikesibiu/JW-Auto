package org.jw.library.auto.util

import org.jw.library.auto.data.model.WeekInfo
import java.util.Calendar

/**
 * Utility for calculating meeting weeks
 * Meeting weeks run from Monday to Sunday
 */
object WeekCalculator {

    /**
     * Get the current meeting week
     */
    fun getCurrentWeek(): WeekInfo {
        val calendar = Calendar.getInstance()
        return getWeekInfo(calendar, isCurrentWeek = true)
    }

    /**
     * Get last week's meeting week
     */
    fun getLastWeek(): WeekInfo {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        return getWeekInfo(calendar, isCurrentWeek = false)
    }

    /**
     * Get next week's meeting week
     */
    fun getNextWeek(): WeekInfo {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        return getWeekInfo(calendar, isCurrentWeek = false)
    }

    /**
     * Calculate week info from a calendar instance
     */
    private fun getWeekInfo(calendar: Calendar, isCurrentWeek: Boolean): WeekInfo {
        // Ensure we're using Monday-based weeks regardless of locale
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.minimalDaysInFirstWeek = 4 // ISO 8601 standard

        // Get the current day of week (1=Sunday, 2=Monday, ..., 7=Saturday)
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Calculate days to subtract to get to Monday
        val daysFromMonday = when (currentDayOfWeek) {
            Calendar.SUNDAY -> 6  // Go back 6 days
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 0
        }

        // Set to Monday at start of day
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis

        // Set to Sunday at end of day
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val weekEnd = calendar.timeInMillis

        return WeekInfo(weekStart, weekEnd, isCurrentWeek)
    }

    /**
     * Format week for display (e.g., "November 4-10, 2025")
     */
    fun formatWeekRange(weekInfo: WeekInfo): String {
        val startCal = Calendar.getInstance().apply { timeInMillis = weekInfo.weekStart }
        val endCal = Calendar.getInstance().apply { timeInMillis = weekInfo.weekEnd }

        val month = startCal.getDisplayName(
            Calendar.MONTH,
            Calendar.LONG,
            java.util.Locale.getDefault()
        )
        val startDay = startCal.get(Calendar.DAY_OF_MONTH)
        val endDay = endCal.get(Calendar.DAY_OF_MONTH)
        val year = startCal.get(Calendar.YEAR)

        return "$month $startDay-$endDay, $year"
    }
}
