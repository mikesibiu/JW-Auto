package org.jw.library.auto.data.cache

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * Synchronous reader for cached content
 * Used by ContentRepository to quickly read cached URLs without async/await
 */
class CachedContentReader(context: Context) {
    private val contentDao = ContentDatabase.getDatabase(context).contentDao()
    private val gson = Gson()

    /**
     * Get cached workbook URL synchronously
     * Returns null if cache miss or expired
     */
    fun getWorkbookUrl(weekStart: LocalDate): String? = runBlocking {
        val key = CachedContent.cacheKey(CachedContent.TYPE_WORKBOOK, weekStart.toString())
        val cached = contentDao.getByKey(key)
        if (cached != null && !cached.isExpired()) {
            cached.url
        } else {
            null
        }
    }

    /**
     * Get cached watchtower URL synchronously
     * Returns null if cache miss or expired
     */
    fun getWatchtowerUrl(weekStart: LocalDate): String? = runBlocking {
        val key = CachedContent.cacheKey(CachedContent.TYPE_WATCHTOWER, weekStart.toString())
        val cached = contentDao.getByKey(key)
        if (cached != null && !cached.isExpired()) {
            cached.url
        } else {
            null
        }
    }

    /**
     * Get cached Bible reading URLs synchronously
     * Returns null if cache miss or expired
     */
    fun getBibleReadingUrls(weekStart: LocalDate): List<String>? = runBlocking {
        val key = CachedContent.cacheKey(CachedContent.TYPE_BIBLE_READING, weekStart.toString())
        val cached = contentDao.getByKey(key)
        if (cached != null && !cached.isExpired() && cached.playlistUrls != null) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(cached.playlistUrls, type)
        } else {
            null
        }
    }

    /**
     * Get cached congregation study URLs synchronously
     * Returns null if cache miss or expired
     */
    fun getCongregationStudyUrls(weekStart: LocalDate): List<String>? = runBlocking {
        val key = CachedContent.cacheKey(CachedContent.TYPE_CONGREGATION_STUDY, weekStart.toString())
        val cached = contentDao.getByKey(key)
        if (cached != null && !cached.isExpired() && cached.playlistUrls != null) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(cached.playlistUrls, type)
        } else {
            null
        }
    }
}
