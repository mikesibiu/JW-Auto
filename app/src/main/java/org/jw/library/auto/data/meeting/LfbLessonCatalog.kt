package org.jw.library.auto.data.meeting

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Lightweight helper to map lfb_E_0xx filenames to spoken lesson numbers/titles
 * by scraping jw.org’s public Play & Download listing for the LFB publication.
 *
 * Source (EN):
 * https://www.jw.org/download/?output=html&pub=lfb&fileformat=MP3&alllangs=0&langwritten=E&txtCMSLang=E&isBible=0
 *
 * We cache the mapping in SharedPreferences for 30 days to avoid repeated fetches.
 */
class LfbLessonCatalog(private val context: Context) {
    data class LessonInfo(val number: Int, val title: String)

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    fun lessonInfoFor(filename: String): LessonInfo? = loadMap()[filename]

    fun lessonInfoForFilenames(filenames: List<String>): Map<String, LessonInfo> {
        val map = loadMap()
        return filenames.associateWith { map[it] }.filterValues { it != null } as Map<String, LessonInfo>
    }

    private fun loadMap(): Map<String, LessonInfo> {
        val now = System.currentTimeMillis()
        val cachedAt = prefs.getLong(KEY_FETCHED_AT, 0L)
        val stale = now - cachedAt > TTL_MS
        val cachedJson = prefs.getString(KEY_JSON, null)
        if (!stale && !cachedJson.isNullOrBlank()) {
            return decode(cachedJson)
        }
        return try {
            val html = fetchCatalogHtml()
            val map = parse(html)
            prefs.edit().putString(KEY_JSON, encode(map)).putLong(KEY_FETCHED_AT, now).apply()
            map
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to fetch LFB catalog; using stale/empty cache", t)
            if (!cachedJson.isNullOrBlank()) decode(cachedJson) else emptyMap()
        }
    }

    private fun fetchCatalogHtml(): String {
        val req = Request.Builder()
            .url(SRC_URL)
            .header("User-Agent", UA)
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            return resp.body?.string() ?: error("empty body")
        }
    }

    private fun parse(html: String): Map<String, LessonInfo> {
        val map = mutableMapOf<String, LessonInfo>()
        // Pattern: 59—Four Boys Who Obeyed Jehovah ... href=".../lfb_E_070.mp3"
        val regex = Regex(
            pattern = ">\\s*(\\d{1,3})\u2014([^<]+?)<(?s).*?href=\"[^\"]*(lfb_E_\\d{3}\\.mp3)\"",
            options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        regex.findAll(html).forEach { m ->
            val num = m.groupValues[1].toIntOrNull() ?: return@forEach
            val title = m.groupValues[2].trim()
            val file = m.groupValues[3]
            map[file] = LessonInfo(num, title)
        }
        return map
    }

    private fun encode(map: Map<String, LessonInfo>): String {
        val arr = JSONArray()
        map.forEach { (k, v) ->
            arr.put(JSONObject().put("file", k).put("num", v.number).put("title", v.title))
        }
        return arr.toString()
    }

    private fun decode(json: String): Map<String, LessonInfo> {
        val arr = JSONArray(json)
        val out = mutableMapOf<String, LessonInfo>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out[o.getString("file")] = LessonInfo(o.getInt("num"), o.getString("title"))
        }
        return out
    }

    companion object {
        private const val TAG = "LfbLessonCatalog"
        private const val PREFS = "lfb_catalog_cache"
        private const val KEY_JSON = "json"
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val SRC_URL = "https://www.jw.org/download/?output=html&pub=lfb&fileformat=MP3&alllangs=0&langwritten=E&txtCMSLang=E&isBible=0"
        private const val UA = "Mozilla/5.0 (Android) JWLibraryAuto/1.0"
        private val TTL_MS = TimeUnit.DAYS.toMillis(30)
    }
}

