package org.jw.library.auto.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.jw.library.auto.data.api.ApiClient
import org.jw.library.auto.data.api.JWOrgContentUrls
import org.jw.library.auto.data.cache.CachedContent
import org.jw.library.auto.data.cache.ContentDatabase
import org.jw.library.auto.data.model.MediaContent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repository for fetching JW.org content dynamically via API
 * Cache-first strategy: Check cache → Try API → Fall back to hard-coded URLs
 */
class JWOrgRepository(
    private val context: Context,
    private val apiService: org.jw.library.auto.data.api.JWOrgApiService = ApiClient.jwOrgApi
) {
    private val yearMonthFormat = DateTimeFormatter.ofPattern("yyyyMM")
    private val gson = Gson()
    private val contentDao by lazy { ContentDatabase.getDatabase(context).contentDao() }

    companion object {
        private const val TAG = "JWOrgRepository"
        private const val PUB_WORKBOOK = "mwb"
        private const val PUB_WATCHTOWER = "w"
        private const val PUB_LESSONS = "lfb"
    }

    /**
     * Fetch meeting workbook audio URL for a specific week
     * Cache-first: Check cache → Try API → Fall back to hard-coded URL
     */
    suspend fun getMeetingWorkbookUrl(weekStart: LocalDate): String {
        val weekStartStr = weekStart.toString()
        val cacheKey = CachedContent.cacheKey(CachedContent.TYPE_WORKBOOK, weekStartStr)

        // 1. Check cache first
        val cached = contentDao.getByKey(cacheKey)
        if (cached != null && !cached.isExpired()) {
            Log.d(TAG, "Cache hit for workbook $weekStart")
            return cached.url ?: fallbackWorkbookUrl(weekStart)
        }

        // 2. Try API
        return try {
            val yearMonth = yearMonthFormat.format(weekStart)
            val response = apiService.getPublicationMedia(
                pub = PUB_WORKBOOK,
                issue = yearMonth
            )

            val url = response.files?.values?.firstOrNull()
                ?.mp3Files?.firstOrNull()
                ?.file?.url
                ?: fallbackWorkbookUrl(weekStart)

            // Cache the result
            cacheUrl(cacheKey, CachedContent.TYPE_WORKBOOK, weekStartStr, url, weekStart)

            url
        } catch (e: Exception) {
            Log.w(TAG, "API fetch failed for workbook $weekStart, using fallback", e)
            // If we have stale cache, use it
            if (cached != null) {
                Log.d(TAG, "Using stale cache for workbook $weekStart")
                return cached.url ?: fallbackWorkbookUrl(weekStart)
            }
            fallbackWorkbookUrl(weekStart)
        }
    }

    /**
     * Fetch Watchtower study audio URL for a specific week
     * Cache-first: Check cache → Try API → Fall back to hard-coded URL
     */
    suspend fun getWatchtowerUrl(weekStart: LocalDate): String {
        val weekStartStr = weekStart.toString()
        val cacheKey = CachedContent.cacheKey(CachedContent.TYPE_WATCHTOWER, weekStartStr)

        // 1. Check cache first
        val cached = contentDao.getByKey(cacheKey)
        if (cached != null && !cached.isExpired()) {
            Log.d(TAG, "Cache hit for watchtower $weekStart")
            return cached.url ?: fallbackWatchtowerUrl(weekStart)
        }

        // 2. Try API
        return try {
            val yearMonth = yearMonthFormat.format(weekStart)
            val response = apiService.getPublicationMedia(
                pub = PUB_WATCHTOWER,
                issue = yearMonth
            )

            val url = response.files?.values?.firstOrNull()
                ?.mp3Files?.firstOrNull()
                ?.file?.url
                ?: fallbackWatchtowerUrl(weekStart)

            // Cache the result
            cacheUrl(cacheKey, CachedContent.TYPE_WATCHTOWER, weekStartStr, url, weekStart)

            url
        } catch (e: Exception) {
            Log.w(TAG, "API fetch failed for watchtower $weekStart, using fallback", e)
            // If we have stale cache, use it
            if (cached != null) {
                Log.d(TAG, "Using stale cache for watchtower $weekStart")
                return cached.url ?: fallbackWatchtowerUrl(weekStart)
            }
            fallbackWatchtowerUrl(weekStart)
        }
    }

    /**
     * Fetch Bible reading URLs for a specific week
     * Returns list of chapter URLs (for multi-chapter readings)
     * Cache-first with fallback to hard-coded
     */
    suspend fun getBibleReadingUrls(weekStart: LocalDate): List<String> {
        val weekStartStr = weekStart.toString()
        val cacheKey = CachedContent.cacheKey(CachedContent.TYPE_BIBLE_READING, weekStartStr)

        // 1. Check cache first
        val cached = contentDao.getByKey(cacheKey)
        if (cached != null && !cached.isExpired() && cached.playlistUrls != null) {
            Log.d(TAG, "Cache hit for bible reading $weekStart")
            val type = object : TypeToken<List<String>>() {}.type
            return gson.fromJson(cached.playlistUrls, type)
        }

        // 2. For now, use fallback (hard-coded URLs)
        // Future: fetch from API when available
        val urls = fallbackBibleReadingUrls(weekStart)

        // Cache the result
        if (urls.isNotEmpty()) {
            cachePlaylist(cacheKey, CachedContent.TYPE_BIBLE_READING, weekStartStr, urls, weekStart)
        }

        return urls
    }

    /**
     * Fetch Congregation Bible Study URLs for a specific week
     * Returns list of lesson URLs
     * Cache-first with fallback to hard-coded
     */
    suspend fun getCongregationStudyUrls(weekStart: LocalDate): List<String> {
        val weekStartStr = weekStart.toString()
        val cacheKey = CachedContent.cacheKey(CachedContent.TYPE_CONGREGATION_STUDY, weekStartStr)

        // 1. Check cache first
        val cached = contentDao.getByKey(cacheKey)
        if (cached != null && !cached.isExpired() && cached.playlistUrls != null) {
            Log.d(TAG, "Cache hit for congregation study $weekStart")
            val type = object : TypeToken<List<String>>() {}.type
            return gson.fromJson(cached.playlistUrls, type)
        }

        // 2. For now, use fallback (hard-coded URLs)
        // Future: fetch from API when available
        val urls = fallbackCongregationStudyUrls(weekStart)

        // Cache the result
        if (urls.isNotEmpty()) {
            cachePlaylist(cacheKey, CachedContent.TYPE_CONGREGATION_STUDY, weekStartStr, urls, weekStart)
        }

        return urls
    }

    // Helper methods for caching
    private suspend fun cacheUrl(
        cacheKey: String,
        contentType: String,
        weekStart: String,
        url: String,
        weekStartDate: LocalDate
    ) {
        val now = System.currentTimeMillis()
        val isInFuture = weekStartDate.isAfter(LocalDate.now())
        val ttl = if (isInFuture) CachedContent.TTL_FUTURE_MILLIS else CachedContent.TTL_PAST_MILLIS

        val cachedContent = CachedContent(
            cacheKey = cacheKey,
            contentType = contentType,
            weekStart = weekStart,
            url = url,
            playlistUrls = null,
            fetchedAt = now,
            expiresAt = now + ttl
        )

        contentDao.insert(cachedContent)
        Log.d(TAG, "Cached $contentType for $weekStart (TTL: ${ttl / 1000 / 60 / 60 / 24} days)")
    }

    private suspend fun cachePlaylist(
        cacheKey: String,
        contentType: String,
        weekStart: String,
        urls: List<String>,
        weekStartDate: LocalDate
    ) {
        val now = System.currentTimeMillis()
        val isInFuture = weekStartDate.isAfter(LocalDate.now())
        val ttl = if (isInFuture) CachedContent.TTL_FUTURE_MILLIS else CachedContent.TTL_PAST_MILLIS

        val cachedContent = CachedContent(
            cacheKey = cacheKey,
            contentType = contentType,
            weekStart = weekStart,
            url = null,
            playlistUrls = gson.toJson(urls),
            fetchedAt = now,
            expiresAt = now + ttl
        )

        contentDao.insert(cachedContent)
        Log.d(TAG, "Cached $contentType playlist for $weekStart (${urls.size} items, TTL: ${ttl / 1000 / 60 / 60 / 24} days)")
    }

    // Fallback methods delegate to existing hard-coded URLs
    private fun fallbackWorkbookUrl(weekStart: LocalDate): String {
        return JWOrgContentUrls.meetingWorkbookUrl(weekStart)
    }

    private fun fallbackWatchtowerUrl(weekStart: LocalDate): String {
        return JWOrgContentUrls.watchtowerStudyUrl(weekStart)
    }

    private fun fallbackBibleReadingUrls(weekStart: LocalDate): List<String> {
        return JWOrgContentUrls.bibleReadingUrls(weekStart)
    }

    private fun fallbackCongregationStudyUrls(weekStart: LocalDate): List<String> {
        return JWOrgContentUrls.congregationStudyUrls(weekStart)
    }
}
