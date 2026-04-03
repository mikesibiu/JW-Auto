package org.jw.library.auto.data.meeting

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jw.library.auto.data.api.ApiClient
import org.jw.library.auto.data.api.JWOrgApiService
import org.jw.library.auto.data.bible.BibleBooks
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Fetches the weekly meeting schedule (Bible reading chapters + CBS lesson numbers)
 * dynamically from the MWB publication API and wol.jw.org docid pages.
 *
 * Flow per week:
 *  1. Call GETPUBMEDIALINKS?pub=mwb&issue=YYYYMM → docid for target week
 *  2. Fetch wol.jw.org/en/wol/d/r1/lp-e/{docid} → parse Bible reading + CBS lessons
 *  3. Cache parsed results in SharedPreferences (keyed by week date string)
 *
 * Bible reading chapters are derived from two signals in the wol.jw.org page:
 *  - Header "MARCH 2-8 ISAIAH 41" → start chapter (41)
 *  - Bible reading line "Isa 42:1-13" → end chapter (42)
 *  - Fallback: next week's header start − 1 if Bible reading line is absent
 */
class MwbScheduleProvider(
    private val context: Context,
    private val api: JWOrgApiService = ApiClient.jwOrgApi,
    private val httpClient: OkHttpClient = defaultHttpClient()
) {
    data class WeekSchedule(
        val bibleBookNumber: Int,
        val bibleStartChapter: Int,
        val bibleEndChapter: Int,
        val cbsLessons: List<Int>
    )

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val issueFormat = DateTimeFormatter.ofPattern("yyyyMM")
    private val monthDayEnglish = DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH)

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun getSchedule(weekStart: LocalDate): WeekSchedule? {
        val key = weekStart.toString()
        loadCached(key)?.let { return it }
        return try {
            fetchAndCache(weekStart)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to fetch dynamic schedule for $weekStart", t)
            null
        }
    }

    // ── Parsing helpers (internal so tests can call them) ─────────────────────

    internal fun parseCbsLessons(html: String): List<Int> {
        val markerIdx = html.indexOf("Congregation Bible Study")
        if (markerIdx < 0) return emptyList()
        val excerpt = html.substring(markerIdx, minOf(markerIdx + 500, html.length))
        // Matches "lfb lessons 66-67" and "lfb intro to section 11 and lessons 68-69"
        val match = Regex("lessons\\s+(\\d+)(?:[-–](\\d+))?", RegexOption.IGNORE_CASE)
            .find(excerpt) ?: return emptyList()
        val first = match.groupValues[1].toIntOrNull() ?: return emptyList()
        val last = match.groupValues[2].toIntOrNull() ?: first
        return (first..last).toList()
    }

    /**
     * Parse the header book+chapter from a wol.jw.org workbook week page.
     * The section header looks like "MARCH 2-8 ISAIAH 41" — only start chapter.
     * Returns (bookNumber, startChapter, null); end chapter comes from
     * [parseBibleReadingEndChapter] or the consecutive-week fallback.
     * Uses findAll so "MARCH 2-8" is skipped when "MARCH" is not a Bible book.
     */
    internal fun parseHeaderChapter(html: String): Triple<Int, Int, Int?>? {
        val searchArea = html.take(3000)
        // Find "BookName CH" — iterate all matches so date tokens like "MARCH 2" are skipped
        for (match in Regex(
            "([A-Za-z]+(?:\\s+[A-Za-z]+)?)\\s+(\\d+)(?::\\d+)?",
            RegexOption.IGNORE_CASE
        ).findAll(searchArea)) {
            val book = BibleBooks.findByName(match.groupValues[1])
            val chapter = match.groupValues[2].toIntOrNull()
            if (book != null && chapter != null) {
                return Triple(book.number, chapter, null)
            }
        }
        return null
    }

    /**
     * Extracts the end chapter of the Bible reading from the wol.jw.org page.
     * The reading line looks like "Bible Reading (4 min.) Isa 42:1-13" —
     * the chapter of the cited verse is the last chapter in the weekly range.
     */
    internal fun parseBibleReadingEndChapter(html: String): Int? {
        val markerIdx = html.indexOf("Bible Reading")
        if (markerIdx < 0) return null
        val excerpt = html.substring(markerIdx, minOf(markerIdx + 300, html.length))
        // "Isa 42:1-13" or "Isaiah 42:1" — capture the chapter number after the book abbrev
        val match = Regex("([A-Za-z]+)\\s+(\\d+):\\d+").find(excerpt) ?: return null
        val book = BibleBooks.findByName(match.groupValues[1])
        val chapter = match.groupValues[2].toIntOrNull()
        return if (book != null && chapter != null) chapter else null
    }

    // ── Private implementation ────────────────────────────────────────────────

    private suspend fun fetchAndCache(weekStart: LocalDate): WeekSchedule? {
        val issueYearMonth = workbookIssueYearMonth(weekStart)
        val response = api.getPublicationMedia(pub = "mwb", issue = issueYearMonth)
        val allTracks = response.files?.values?.flatMap { it.mp3Files.orEmpty() }.orEmpty()

        // Build ordered list of (weekDate, docid) for the issue
        val weekDocids = allTracks.mapNotNull { track ->
            val docid = track.docid ?: return@mapNotNull null
            val date = parseMwbTrackDate(track.title, weekStart.year) ?: return@mapNotNull null
            date to docid
        }.sortedBy { it.first }

        // Find this week's docid
        val thisEntry = weekDocids.firstOrNull { it.first == weekStart } ?: run {
            Log.w(TAG, "Docid not found for $weekStart in issue $issueYearMonth")
            return null
        }

        // Fetch this week's page
        val thisHtml = fetchWolPage(thisEntry.second) ?: return null
        val cbsLessons = parseCbsLessons(thisHtml)
        val headerInfo = parseHeaderChapter(thisHtml)
            ?: return null.also { Log.w(TAG, "No header chapter for $weekStart") }

        val bookNum = headerInfo.first
        val startChapter = headerInfo.second

        // Primary: extract end chapter from Bible reading assignment ("Isa 42:1-13" → 42)
        val readingEndCh = parseBibleReadingEndChapter(thisHtml)
        val endChapter = when {
            readingEndCh != null && readingEndCh >= startChapter -> readingEndCh
            else -> {
                // Fallback: derive from next week's starting chapter − 1
                val nextDate = nextWeekStart(weekStart, weekDocids)
                val nextStart = if (nextDate != null) {
                    val nextDocid = weekDocids.firstOrNull { it.first == nextDate }?.second
                        ?: fetchNextIssueFirstDocid(weekStart)
                    nextDocid?.let { fetchWolPage(it) }?.let { parseHeaderChapter(it) }?.second
                } else null

                if (nextStart != null && nextStart > startChapter) nextStart - 1
                else startChapter
            }
        }

        val schedule = WeekSchedule(bookNum, startChapter, endChapter, cbsLessons)
        saveCache(weekStart.toString(), schedule)
        return schedule
    }

    /** Parse the week-start date from an MWB track title like "March 9-15" or "April 27–May 3". */
    private fun parseMwbTrackDate(title: String?, year: Int): LocalDate? {
        if (title.isNullOrBlank()) return null
        // Try "Month Day-Day" format (same month), e.g. "March 9-15"
        val sameMonth = Regex("^([A-Za-z]+)\\s+(\\d+)[–\\-]\\d+").find(title.trim())
        if (sameMonth != null) {
            return runCatching {
                val month = sameMonth.groupValues[1]
                val day = sameMonth.groupValues[2].toInt()
                LocalDate.parse("$month $day $year",
                    DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.ENGLISH))
            }.getOrNull()
        }
        // Try "Month Day–Month Day" cross-month format, e.g. "April 27–May 3"
        val crossMonth = Regex("^([A-Za-z]+)\\s+(\\d+)[–\\-]").find(title.trim())
        if (crossMonth != null) {
            return runCatching {
                val month = crossMonth.groupValues[1]
                val day = crossMonth.groupValues[2].toInt()
                LocalDate.parse("$month $day $year",
                    DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.ENGLISH))
            }.getOrNull()
        }
        return null
    }

    private fun nextWeekStart(weekStart: LocalDate, weekDocids: List<Pair<LocalDate, Long>>): LocalDate? =
        weekDocids.map { it.first }.sorted().firstOrNull { it > weekStart }

    private suspend fun fetchNextIssueFirstDocid(currentWeekStart: LocalDate): Long? {
        return try {
            val nextIssueMonth = nextWorkbookIssueYearMonth(currentWeekStart)
            val response = api.getPublicationMedia(pub = "mwb", issue = nextIssueMonth)
            response.files?.values?.flatMap { it.mp3Files.orEmpty() }
                ?.minByOrNull { it.track ?: Int.MAX_VALUE }?.docid
        } catch (t: Throwable) {
            Log.w(TAG, "Could not fetch next issue for chapter range", t)
            null
        }
    }

    private fun fetchWolPage(docid: Long): String? {
        val url = "https://wol.jw.org/en/wol/d/r1/lp-e/$docid"
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android) JWLibraryAuto/1.0")
                .build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "wol.jw.org ${resp.code} for docid $docid")
                    return null
                }
                resp.body?.string()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to fetch wol.jw.org for docid $docid", t)
            null
        }
    }

    // ── Workbook issue helpers (mirrors JWOrgRepository logic) ───────────────

    private fun workbookIssueYearMonth(weekStart: LocalDate): String {
        val month = weekStart.monthValue
        val issueMonth = if (month % 2 == 1) month else month - 1
        return "${weekStart.year}${issueMonth.toString().padStart(2, '0')}"
    }

    private fun nextWorkbookIssueYearMonth(weekStart: LocalDate): String {
        val month = weekStart.monthValue
        val issueMonth = if (month % 2 == 1) month else month - 1
        val nextIssueMonth = if (issueMonth + 2 > 12) issueMonth - 10 else issueMonth + 2
        val year = if (issueMonth + 2 > 12) weekStart.year + 1 else weekStart.year
        return "${year}${nextIssueMonth.toString().padStart(2, '0')}"
    }

    // ── SharedPreferences cache ───────────────────────────────────────────────

    private fun loadCached(weekKey: String): WeekSchedule? {
        val json = prefs.getString(weekKey, null) ?: return null
        return try {
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("lessons")
            WeekSchedule(
                bibleBookNumber = obj.getInt("bookNum"),
                bibleStartChapter = obj.getInt("startCh"),
                bibleEndChapter = obj.getInt("endCh"),
                cbsLessons = (0 until arr.length()).map { arr.getInt(it) }
            )
        } catch (_: Throwable) { null }
    }

    private fun saveCache(weekKey: String, schedule: WeekSchedule) {
        val lessons = JSONArray().apply { schedule.cbsLessons.forEach { put(it) } }
        val obj = JSONObject()
            .put("bookNum", schedule.bibleBookNumber)
            .put("startCh", schedule.bibleStartChapter)
            .put("endCh", schedule.bibleEndChapter)
            .put("lessons", lessons)
        prefs.edit().putString(weekKey, obj.toString()).apply()
    }

    companion object {
        private const val TAG = "MwbScheduleProvider"
        private const val PREFS = "mwb_schedule_cache"

        private fun defaultHttpClient() = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
