package org.jw.library.auto.data.model

/**
 * Represents a media item in the JW Library Auto content hierarchy
 */
data class MediaContent(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val iconUri: String? = null,
    val mediaUri: String? = null,
    val isPlayable: Boolean = false,
    val isBrowsable: Boolean = false,
    val duration: Long? = null,
    val language: String = "en"
)

/**
 * Content categories for organizing meeting materials
 */
enum class ContentCategory(val id: String, val titleResId: Int) {
    THIS_WEEK("this_week", org.jw.library.auto.R.string.category_this_week),
    LAST_WEEK("last_week", org.jw.library.auto.R.string.category_last_week),
    NEXT_WEEK("next_week", org.jw.library.auto.R.string.category_next_week),
    SONGS("songs", org.jw.library.auto.R.string.category_songs),
    DRAMAS("dramas", org.jw.library.auto.R.string.category_dramas)
}

/**
 * Types of meeting content
 */
enum class MeetingContentType(val id: String, val titleResId: Int) {
    BIBLE_READING("bible_reading", org.jw.library.auto.R.string.content_bible_reading),
    WATCHTOWER("watchtower", org.jw.library.auto.R.string.content_watchtower),
    CBS("cbs", org.jw.library.auto.R.string.content_cbs),
    WORKBOOK("workbook", org.jw.library.auto.R.string.content_workbook)
}

/**
 * Represents a week for meeting content
 */
data class WeekInfo(
    val weekStart: Long, // Timestamp of Monday
    val weekEnd: Long,   // Timestamp of Sunday
    val isCurrentWeek: Boolean = false
) {
    fun getWeekLabel(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = weekStart
        val month = calendar.getDisplayName(
            java.util.Calendar.MONTH,
            java.util.Calendar.SHORT,
            java.util.Locale.getDefault()
        )
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val year = calendar.get(java.util.Calendar.YEAR)
        return "$month $day, $year"
    }

    /**
     * Get the week start date formatted as YYYY-MM-DD for URL lookup
     */
    fun getWeekKey(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = weekStart
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1  // Calendar.MONTH is 0-based
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        return String.format("%04d-%02d-%02d", year, month, day)
    }
}
