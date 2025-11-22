package org.jw.library.auto.data.cache

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cache reader that surfaces suspend functions so callers can keep IO off the main thread.
 */
class CachedContentReader(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val contentDao = ContentDatabase.getDatabase(context).contentDao()
    private val gson = Gson()

    suspend fun getWorkbookUrl(weekStart: LocalDate): String? = withContext(ioDispatcher) {
        val key = CachedContent.cacheKey(CachedContent.TYPE_WORKBOOK, weekStart.toString())
        val cached = contentDao.getByKey(key)
        if (cached != null && !cached.isExpired()) cached.url else null
    }

    suspend fun getWatchtowerUrl(weekStart: LocalDate): String? = withContext(ioDispatcher) {
        val key = CachedContent.cacheKey(CachedContent.TYPE_WATCHTOWER, weekStart.toString())
        val cached = contentDao.getByKey(key)
        if (cached != null && !cached.isExpired()) cached.url else null
    }

    suspend fun getBibleReadingUrls(weekStart: LocalDate): List<String>? = withContext(ioDispatcher) {
        val key = CachedContent.cacheKey(CachedContent.TYPE_BIBLE_READING, weekStart.toString())
        val cached = contentDao.getByKey(key)
        if (cached != null && !cached.isExpired() && cached.playlistUrls != null) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(cached.playlistUrls, type)
        } else {
            null
        }
    }

    suspend fun getCongregationStudyUrls(weekStart: LocalDate): List<String>? = withContext(ioDispatcher) {
        val key = CachedContent.cacheKey(CachedContent.TYPE_CONGREGATION_STUDY, weekStart.toString())
        val cached = contentDao.getByKey(key)
        if (cached != null && !cached.isExpired() && cached.playlistUrls != null) {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(cached.playlistUrls, type)
        } else {
            null
        }
    }
}
