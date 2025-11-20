package org.jw.library.auto.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached content entity for Room database
 * Stores audio URLs with TTL for offline support
 */
@Entity(tableName = "cached_content")
data class CachedContent(
    @PrimaryKey
    val cacheKey: String,  // Format: "TYPE:WEEK_START" e.g., "workbook:2025-11-03"

    val contentType: String,  // workbook, watchtower, bible_reading, congregation_study

    val weekStart: String,  // ISO date format: YYYY-MM-DD

    val url: String?,  // Single URL for workbook/watchtower

    val playlistUrls: String?,  // JSON array of URLs for bible reading/CBS

    val fetchedAt: Long,  // System.currentTimeMillis() when fetched

    val expiresAt: Long  // System.currentTimeMillis() when cache expires
) {
    companion object {
        const val TYPE_WORKBOOK = "workbook"
        const val TYPE_WATCHTOWER = "watchtower"
        const val TYPE_BIBLE_READING = "bible_reading"
        const val TYPE_CONGREGATION_STUDY = "congregation_study"

        // TTL: 7 days for future content, 30 days for past content
        const val TTL_FUTURE_MILLIS = 7L * 24 * 60 * 60 * 1000  // 7 days
        const val TTL_PAST_MILLIS = 30L * 24 * 60 * 60 * 1000   // 30 days

        fun cacheKey(contentType: String, weekStart: String): String {
            return "$contentType:$weekStart"
        }
    }

    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }

    fun isStale(): Boolean {
        // Consider stale if > 7 days old, even if not expired
        val sevenDaysAgo = System.currentTimeMillis() - TTL_FUTURE_MILLIS
        return fetchedAt < sevenDaysAgo
    }
}
