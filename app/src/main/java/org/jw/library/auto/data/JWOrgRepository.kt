package org.jw.library.auto.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import org.jw.library.auto.BuildConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jw.library.auto.data.api.ApiClient
import org.jw.library.auto.data.api.JWOrgContentUrls
import org.jw.library.auto.data.api.MediatorApiClient
import org.jw.library.auto.data.api.MediatorApiService
import org.jw.library.auto.data.api.MediatorMediaItem
import org.jw.library.auto.data.bible.BibleAudioParser
import org.jw.library.auto.data.bible.BibleBooks
import org.jw.library.auto.data.cache.CachedContent
import org.jw.library.auto.data.cache.CachedSong
import org.jw.library.auto.data.cache.ContentDatabase
import org.jw.library.auto.data.meeting.LfbLessonCatalog
import org.jw.library.auto.data.meeting.MeetingSectionsProvider
import org.jw.library.auto.data.meeting.MwbScheduleProvider
import java.util.Locale
import org.jw.library.auto.data.model.BibleDrama
import org.jw.library.auto.data.model.KingdomSong
import org.jw.library.auto.data.model.MediaContent
import org.jw.library.auto.data.model.api.MediaFile
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.jvm.Volatile

/**
 * Repository for fetching JW.org content dynamically via API
 * Cache-first strategy: Check cache → Try API → Fall back to hard-coded URLs
 */
data class BroadcastingProgram(
    val id: String,
    val title: String,
    val streamUrl: String,
    val publishedDate: String? = null
)

class JWOrgRepository(
    private val context: Context,
    private val apiService: org.jw.library.auto.data.api.JWOrgApiService = ApiClient.jwOrgApi,
    private val mediatorService: MediatorApiService = MediatorApiClient.service,
    private val dramaPageUrl: String = "https://www.jw.org/en/library/videos/#en/categories/VODDramatizations",
    private val dramaHttpClient: OkHttpClient = OkHttpClient()
) {
    private val yearMonthFormat = DateTimeFormatter.ofPattern("yyyyMM")
    private val gson = Gson()
    private val contentDao by lazy { ContentDatabase.getDatabase(context).contentDao() }
    private val songDao by lazy { ContentDatabase.getDatabase(context).songDao() }
    private val meetingSectionsProvider by lazy { MeetingSectionsProvider(context) }
    private val mwbScheduleProvider by lazy { MwbScheduleProvider(context, apiService) }
    // One LfbLessonCatalog per language — created on demand
    private val lfbCatalogByLang = mutableMapOf<String, LfbLessonCatalog>()
    private fun lfbCatalog(lang: String = langCode) =
        lfbCatalogByLang.getOrPut(lang) { LfbLessonCatalog(context, apiService, lang) }

    /** Active content language code derived from user preference / device locale. */
    val langCode: String get() = LanguagePreference.get(context)

    data class BibleBookAudio(val intro: String?, val chapters: List<String>)

    @Volatile private var bibleCatalog: Map<Int, BibleBookAudio>? = null
    @Volatile private var bibleCatalogLang: String? = null

    /** Clear in-memory caches that are language-specific (called after language toggle). */
    fun clearLanguageCaches() {
        bibleCatalog = null
        bibleCatalogLang = null
        lfbCatalogByLang.clear()
    }

    companion object {
        private const val TAG = "JWOrgRepository"
        private const val PUB_WORKBOOK = "mwb"
        private const val PUB_WATCHTOWER = "w"
        private const val PUB_LESSONS = "lfb"
        private const val PUB_SONGBOOK = "sjjc"  // Vocal version (with singing)
        private const val PUB_BIBLE = "nwt"
        private const val SONG_CACHE_TTL_MILLIS = 30L * 24 * 60 * 60 * 1000
        private val SONG_NUMBER_REGEX = Regex("(\\d+)")

        // Mediator API categories
        private const val CATEGORY_MONTHLY_PROGRAMS = "StudioMonthlyPrograms"
        private const val CATEGORY_GB_UPDATES = "StudioNewsReports"
        private const val CATEGORY_DRAMAS = "VOXDramas"
        private const val PREFERRED_VIDEO_QUALITY = "240p"
    }

    /**
     * Fetch meeting workbook audio URL for a specific week.
     * Cache → API (with correct issue + track) → hard-coded fallback.
     *
     * The workbook is a bimonthly publication (odd months: Jan, Mar, May …).
     * Each week within an issue is a separate track, so we calculate the
     * issue year-month and the 1-based track number from the week date.
     */
    private suspend fun fetchWorkbookUrl(issue: String, track: Int, lang: String): String? =
        try {
            apiService.getPublicationMedia(pub = PUB_WORKBOOK, issue = issue, track = track,
                langwritten = lang, txtCMSLang = lang)
                .files?.values?.firstOrNull()?.mp3Files?.firstOrNull()?.file?.url
        } catch (_: Exception) { null }

    suspend fun getMeetingWorkbookUrl(weekStart: LocalDate): String {
        val weekStartStr = weekStart.toString()

        val lang = langCode
        val cacheKey = CachedContent.cacheKey(CachedContent.TYPE_WORKBOOK, weekStartStr, lang)
        val cached = contentDao.getByKey(cacheKey)
        if (cached != null && !cached.isExpired()) {
            Log.d(TAG, "Cache hit for workbook $weekStart [$lang]")
            return cached.url ?: fallbackWorkbookUrl(weekStart)
        }

        return try {
            val issueYearMonth = workbookIssueYearMonth(weekStart)
            val track = workbookTrack(weekStart)
            Log.d(TAG, "Fetching workbook $weekStart — issue=$issueYearMonth track=$track lang=$lang")
            // Try requested language first; Romanian MWB may not be published — fall back to English
            val url = fetchWorkbookUrl(issueYearMonth, track, lang)
                ?: fetchWorkbookUrl(issueYearMonth, track, LanguagePreference.LANG_ENGLISH)
                ?: fallbackWorkbookUrl(weekStart)
            cacheUrl(cacheKey, CachedContent.TYPE_WORKBOOK, weekStartStr, url, weekStart)
            url
        } catch (e: Exception) {
            Log.w(TAG, "API fetch failed for workbook $weekStart, using fallback", e)
            if (cached != null) return cached.url ?: fallbackWorkbookUrl(weekStart)
            fallbackWorkbookUrl(weekStart)
        }
    }

    /**
     * The workbook is published on odd months and covers ~2 months each issue.
     * Round the week's month down to the nearest odd month to get the issue code.
     */
    private fun workbookIssueYearMonth(weekStart: LocalDate): String {
        val month = weekStart.monthValue
        val issueMonth = if (month % 2 == 1) month else month - 1
        return "${weekStart.year}${issueMonth.toString().padStart(2, '0')}"
    }

    /**
     * The track number is the 1-based index of the week within its issue,
     * counting from the first Monday of the issue month.
     */
    private fun workbookTrack(weekStart: LocalDate): Int {
        val month = weekStart.monthValue
        val issueMonth = if (month % 2 == 1) month else month - 1
        val issueFirstDay = LocalDate.of(weekStart.year, issueMonth, 1)
        val firstMonday = issueFirstDay.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
        return (ChronoUnit.WEEKS.between(firstMonday, weekStart) + 1).toInt()
    }

    /**
     * Fetch Watchtower study audio URL for a specific week.
     * Cache → dynamic API (issue = meeting date − 2 months, match by track title) →
     * override map → generic fallback.
     *
     * The study Watchtower is published ~2 months before the meeting date.
     * The API track titles include the meeting date in parentheses, e.g.
     * "Speak the Truth Graciously (March 2-8)", so we can match precisely.
     * We try −2 months first, then −3 months to cover occasional edge cases.
     */
    suspend fun getWatchtowerUrl(weekStart: LocalDate): String {
        val weekStartStr = weekStart.toString()
        val lang = langCode
        val cacheKey = CachedContent.cacheKey(CachedContent.TYPE_WATCHTOWER, weekStartStr, lang)

        val cached = contentDao.getByKey(cacheKey)
        if (cached != null && !cached.isExpired()) {
            Log.d(TAG, "Cache hit for watchtower $weekStart [$lang]")
            return cached.url ?: fallbackWatchtowerUrl(weekStart)
        }

        // Dynamic: find WT issue ~2 months before, match track title to this week
        val dynamicUrl = fetchWatchtowerDynamic(weekStart, lang)
        if (dynamicUrl != null) {
            Log.d(TAG, "Dynamic Watchtower resolved for $weekStart [$lang]")
            cacheUrl(cacheKey, CachedContent.TYPE_WATCHTOWER, weekStartStr, dynamicUrl, weekStart)
            return dynamicUrl
        }

        // Override map safety net — English only (Romanian uses dynamic only)
        if (lang == LanguagePreference.LANG_ENGLISH) {
            val overrideUrl = JWOrgContentUrls.watchtowerOverrideUrl(weekStart)
            if (overrideUrl != null) {
                Log.d(TAG, "Using Watchtower override for $weekStart")
                cacheUrl(cacheKey, CachedContent.TYPE_WATCHTOWER, weekStartStr, overrideUrl, weekStart)
                return overrideUrl
            }
        }

        if (cached != null) return cached.url ?: fallbackWatchtowerUrl(weekStart)
        return fallbackWatchtowerUrl(weekStart)
    }

    private suspend fun fetchWatchtowerDynamic(weekStart: LocalDate, lang: String): String? {
        val day = weekStart.dayOfMonth
        // Language-agnostic: EN titles use "(March 2-8)", RO titles use "(2-8 martie)"
        // Both contain the day number right after "(" or after "MonthName " — match either.
        val pattern = Regex("\\((?:[A-Za-z]+ )?$day[^0-9]")

        for (monthsBack in 2..3) {
            val issueDate = weekStart.minusMonths(monthsBack.toLong())
            val issue = yearMonthFormat.format(issueDate)
            try {
                val response = apiService.getPublicationMedia(pub = PUB_WATCHTOWER, issue = issue,
                    langwritten = lang, txtCMSLang = lang)
                val tracks = response.files?.values?.flatMap { it.mp3Files.orEmpty() }.orEmpty()
                val url = tracks.firstOrNull { pattern.containsMatchIn(it.title ?: "") }?.file?.url
                if (url != null) {
                    Log.d(TAG, "Watchtower for $weekStart found in issue $issue [$lang]")
                    return url
                }
            } catch (e: Exception) {
                Log.w(TAG, "WT dynamic fetch failed for issue $issue", e)
            }
        }
        return null
    }

    /**
     * Returns Bible reading URLs for a specific week.
     * Dynamic first (MWB API → wol.jw.org schedule page → NWT Bible catalog) →
     * falls back to meeting_sections.json if dynamic fetch fails.
     */
    suspend fun getBibleReadingUrls(weekStart: LocalDate): List<String> {
        val dynamic = fetchDynamicBibleReadingUrls(weekStart)
        if (dynamic.isNotEmpty()) {
            Log.i(TAG, "CONTENT_CHECK bible_reading $weekStart -> ${dynamic.map { it.substringAfterLast('/') }} [dynamic]")
            return dynamic
        }
        val fallback = fallbackBibleReadingUrls(weekStart)
        Log.i(TAG, "CONTENT_CHECK bible_reading $weekStart -> ${fallback.map { it.substringAfterLast('/') }} [json]")
        return fallback
    }

    private suspend fun fetchDynamicBibleReadingUrls(weekStart: LocalDate): List<String> {
        return try {
            val schedule = mwbScheduleProvider.getSchedule(weekStart) ?: return emptyList()
            val catalog = ensureBibleCatalog()
            val bookAudio = catalog[schedule.bibleBookNumber] ?: return emptyList()
            (schedule.bibleStartChapter..schedule.bibleEndChapter).mapNotNull { chapter ->
                val idx = chapter - 1
                bookAudio.chapters.getOrNull(idx)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Dynamic bible reading failed for $weekStart", t)
            emptyList()
        }
    }

    /**
     * Returns Congregation Bible Study URLs for a specific week.
     * Dynamic first (wol.jw.org schedule page → lesson numbers → LFB catalog URLs) →
     * falls back to meeting_sections.json + LFB catalog remapping.
     */
    suspend fun getCongregationStudyUrls(weekStart: LocalDate): List<String> {
        val dynamic = fetchDynamicCbsUrls(weekStart)
        if (dynamic.isNotEmpty()) {
            Log.i(TAG, "CONTENT_CHECK congregation_study $weekStart RESOLVED=${dynamic.map { it.substringAfterLast('/') }} [dynamic]")
            return dynamic
        }

        // Fallback: derive lesson numbers from meeting_sections.json filenames → LFB catalog
        val jsonUrls = fallbackCongregationStudyUrls(weekStart)
        val lessonNumbers = jsonUrls.mapNotNull {
            Regex("lfb_E_(\\d{3})\\.mp3", RegexOption.IGNORE_CASE)
                .find(it)?.groupValues?.get(1)?.toIntOrNull()
        }
        val workbookLabel = when (lessonNumbers.size) {
            0 -> ""
            1 -> "lfb ${lessonNumbers.first()}"
            else -> "lfb ${lessonNumbers.first()}–${lessonNumbers.last()}"
        }
        val remapped = try {
            val resolved = lfbCatalog(langCode).urlsForLessonNumbers(lessonNumbers)
            if (resolved.size == lessonNumbers.size) resolved else emptyList()
        } catch (_: Throwable) { emptyList() }

        val finalUrls = if (remapped.isNotEmpty()) remapped else jsonUrls
        Log.i(TAG, "CONTENT_CHECK congregation_study $weekStart WORKBOOK=$workbookLabel RESOLVED=${finalUrls.map { it.substringAfterLast('/') }} [json]")
        return finalUrls
    }

    private suspend fun fetchDynamicCbsUrls(weekStart: LocalDate): List<String> {
        return try {
            val schedule = mwbScheduleProvider.getSchedule(weekStart) ?: return emptyList()
            if (schedule.cbsLessons.isEmpty()) return emptyList()
            val lang = langCode
            val resolved = lfbCatalog(lang).urlsForLessonNumbers(schedule.cbsLessons)
            if (resolved.size == schedule.cbsLessons.size) resolved else emptyList()
        } catch (t: Throwable) {
            Log.w(TAG, "Dynamic CBS fetch failed for $weekStart", t)
            emptyList()
        }
    }
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

    private fun fallbackBibleReadingUrls(weekStart: LocalDate): List<String> =
        meetingSectionsProvider.bibleReadingUrls(weekStart)

    private fun fallbackCongregationStudyUrls(weekStart: LocalDate): List<String> =
        meetingSectionsProvider.congregationStudyUrls(weekStart)

    /**
     * Clears the content cache if the APK version code has changed since the last run.
     * Must be called via runBlocking(Dispatchers.IO) from Service.onCreate() so the cache
     * is guaranteed clean before any content request executes.
     */
    suspend fun clearCacheIfVersionChanged() {
        val prefs = context.getSharedPreferences("jw_app_state", MODE_PRIVATE)
        val stored = prefs.getInt("version_code", -1)
        val current = BuildConfig.VERSION_CODE
        if (stored != current) {
            Log.i(TAG, "Version changed ($stored → $current), clearing content cache")
            contentDao.deleteAll()
            prefs.edit().putInt("version_code", current).apply()
        }
    }

    suspend fun getBibleBookAudio(bookNumber: Int): BibleBookAudio {
        return ensureBibleCatalog()[bookNumber] ?: BibleBookAudio(intro = null, chapters = emptyList())
    }

    private suspend fun ensureBibleCatalog(): Map<Int, BibleBookAudio> {
        val lang = langCode
        if (bibleCatalog != null && bibleCatalogLang == lang) return bibleCatalog!!
        return try {
            val response = apiService.getPublicationMedia(pub = PUB_BIBLE,
                langwritten = lang, txtCMSLang = lang)
            val mp3Files = response.files?.values?.flatMap { it.mp3Files.orEmpty() }.orEmpty()
            val grouped = BibleAudioParser.groupByBook(mp3Files)
            bibleCatalog = grouped
            bibleCatalogLang = lang
            grouped
        } catch (e: Exception) {
            Log.w(TAG, "Failed to build Bible catalog", e)
            emptyMap()
        }
    }

    suspend fun getKingdomSongs(forceRefresh: Boolean = false): List<KingdomSong> {
        val lang = langCode
        val cached = songDao.getSongsByLanguage(lang)
        val now = System.currentTimeMillis()
        val stale = cached.isEmpty() || now - cached.minOf { it.fetchedAt } > SONG_CACHE_TTL_MILLIS
        if (cached.isNotEmpty() && !forceRefresh && !stale) {
            Log.d(TAG, "Using cached kingdom songs (${cached.size}) [$lang]")
            return cached.toDomain()
        }

        return try {
            val songs = fetchSongsFromApi(lang)
            if (songs.isNotEmpty()) {
                persistSongs(songs, now, lang)
                songs
            } else {
                Log.w(TAG, "JW.org returned no kingdom songs [$lang]; falling back to cache")
                cached.toDomain()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh kingdom songs", e)
            cached.toDomain()
        }
    }

    private suspend fun fetchSongsFromApi(lang: String): List<KingdomSong> {
        val response = apiService.getPublicationMedia(pub = PUB_SONGBOOK,
            langwritten = lang, txtCMSLang = lang)
        val mp3Files = response.files?.values?.flatMap { it.mp3Files.orEmpty() }.orEmpty()
        return mp3Files.mapNotNull { file ->
            val url = file.file?.url ?: return@mapNotNull null
            val number = parseTrackNumber(file) ?: return@mapNotNull null
            val title = file.title?.takeIf { it.isNotBlank() }
                ?: file.label?.takeIf { it.isNotBlank() }
                ?: "Kingdom Song $number"
            KingdomSong(number = number, title = title, url = url)
        }.sortedBy { it.number }
    }

    private suspend fun persistSongs(songs: List<KingdomSong>, fetchedAt: Long, lang: String) {
        val cachedSongs = songs.map { song ->
            CachedSong(
                number = song.number,
                title = song.title,
                url = song.url,
                language = lang,
                fetchedAt = fetchedAt
            )
        }
        songDao.deleteSongsByLanguage(lang)
        songDao.insertAll(cachedSongs)
        Log.d(TAG, "Cached ${songs.size} kingdom songs [$lang]")
    }

    private fun parseTrackNumber(file: MediaFile): Int? {
        file.track?.let { return it }
        listOfNotNull(file.title, file.label).forEach { text ->
            val match = SONG_NUMBER_REGEX.find(text)
            if (match != null) {
                return match.value.toIntOrNull()
            }
        }
        return null
    }

    private fun parseBookNumber(file: MediaFile): Int? {
        file.bookNumber?.let { return it }
        val text = listOfNotNull(file.label, file.title).joinToString(" ")
        return BibleBooks.findByName(text)?.number
    }

    private fun List<CachedSong>.toDomain(): List<KingdomSong> =
        this.sortedBy { it.number }.map { KingdomSong(number = it.number, title = it.title, url = it.url) }

    /**
     * Fetch JW Broadcasting Monthly Programs
     * Returns list of programs sorted by most recent first
     * Uses 240p MP4 for audio-only playback (smallest file size)
     */
    suspend fun getMonthlyPrograms(): List<BroadcastingProgram> {
        return try {
            val response = mediatorService.getCategory(
                language = langCode,
                category = CATEGORY_MONTHLY_PROGRAMS
            )
            response.items().mapNotNull { item ->
                val url = item.files?.find { it.label == PREFERRED_VIDEO_QUALITY }?.url
                    ?: item.files?.firstOrNull()?.url
                    ?: return@mapNotNull null
                val id = item.guid ?: item.naturalKey ?: return@mapNotNull null
                val title = item.title ?: "JW Broadcasting"
                BroadcastingProgram(
                    id = "jwb-$id",
                    title = title,
                    streamUrl = url,
                    publishedDate = item.firstPublished
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch monthly programs", e)
            emptyList()
        }
    }

    /**
     * Fetch Bible Dramas from Mediator API (VOXDramas category) with HTML fallback.
     * Mediator is preferred; if it returns nothing the drama page is scraped for
     * __NEXT_DATA__ JSON.
     */
    suspend fun getBibleDramas(forceRefresh: Boolean = false): List<BibleDrama> {
        val mediatorDramas = try {
            val response = mediatorService.getCategory(langCode, CATEGORY_DRAMAS)
            response.items().mapNotNull { item ->
                val url = item.files?.firstOrNull()?.url ?: return@mapNotNull null
                val id = item.guid ?: item.naturalKey ?: return@mapNotNull null
                BibleDrama(
                    id = "broadcast-$id",
                    title = item.title ?: "Drama",
                    streamUrl = url,
                    durationSeconds = item.durationSeconds?.toInt()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Mediator drama fetch failed", e)
            emptyList()
        }
        if (mediatorDramas.isNotEmpty()) return mediatorDramas

        return try {
            val request = Request.Builder().url(dramaPageUrl).build()
            val html = dramaHttpClient.newCall(request).execute().body?.string() ?: return emptyList()
            parseNextDataDramas(html)
        } catch (e: Exception) {
            Log.w(TAG, "HTML drama fetch failed", e)
            emptyList()
        }
    }

    private fun parseNextDataDramas(html: String): List<BibleDrama> {
        val startMarker = """<script id="__NEXT_DATA__" type="application/json">"""
        val start = html.indexOf(startMarker).let { if (it < 0) return emptyList() else it + startMarker.length }
        val end = html.indexOf("</script>", start).let { if (it < 0) return emptyList() else it }
        return try {
            val root = gson.fromJson(html.substring(start, end), com.google.gson.JsonObject::class.java)
            val files = root
                ?.getAsJsonObject("props")
                ?.getAsJsonObject("pageProps")
                ?.getAsJsonObject("listData")
                ?.getAsJsonArray("files") ?: return emptyList()
            files.mapNotNull { el ->
                val obj = el.asJsonObject
                val title = obj.get("title")?.asString ?: return@mapNotNull null
                val url = obj.get("fileUrl")?.asString ?: return@mapNotNull null
                BibleDrama(
                    id = "drama-${url.hashCode()}",
                    title = title,
                    streamUrl = url,
                    description = obj.get("description")?.asString,
                    durationSeconds = obj.get("duration")?.asInt
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse __NEXT_DATA__ dramas", e)
            emptyList()
        }
    }

    /**
     * Fetch Governing Body Updates
     * Returns list of updates sorted by most recent first
     * Uses 240p MP4 for audio-only playback
     */
    suspend fun getGoverningBodyUpdates(): List<BroadcastingProgram> {
        return try {
            val response = mediatorService.getCategory(
                language = langCode,
                category = CATEGORY_GB_UPDATES
            )
            response.items().mapNotNull { item ->
                val url = item.files?.find { it.label == PREFERRED_VIDEO_QUALITY }?.url
                    ?: item.files?.firstOrNull()?.url
                    ?: return@mapNotNull null
                val id = item.guid ?: item.naturalKey ?: return@mapNotNull null
                val title = item.title ?: "Governing Body Update"
                BroadcastingProgram(
                    id = "gb-$id",
                    title = title,
                    streamUrl = url,
                    publishedDate = item.firstPublished
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch GB updates", e)
            emptyList()
        }
    }
}
