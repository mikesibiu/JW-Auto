package org.jw.library.auto.data

import android.content.Context
import android.util.Log
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
import org.jw.library.auto.data.meeting.MeetingSectionsProvider
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

    data class BibleBookAudio(val intro: String?, val chapters: List<String>)

    @Volatile private var bibleCatalog: Map<Int, BibleBookAudio>? = null

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
        private const val MEDIATOR_LANGUAGE = "E"
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
    suspend fun getMeetingWorkbookUrl(weekStart: LocalDate): String {
        val weekStartStr = weekStart.toString()
        val cacheKey = CachedContent.cacheKey(CachedContent.TYPE_WORKBOOK, weekStartStr)

        val cached = contentDao.getByKey(cacheKey)
        if (cached != null && !cached.isExpired()) {
            Log.d(TAG, "Cache hit for workbook $weekStart")
            return cached.url ?: fallbackWorkbookUrl(weekStart)
        }

        return try {
            val issueYearMonth = workbookIssueYearMonth(weekStart)
            val track = workbookTrack(weekStart)
            Log.d(TAG, "Fetching workbook $weekStart — issue=$issueYearMonth track=$track")
            val response = apiService.getPublicationMedia(
                pub = PUB_WORKBOOK,
                issue = issueYearMonth,
                track = track
            )
            val url = response.files?.values?.firstOrNull()
                ?.mp3Files?.firstOrNull()
                ?.file?.url
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
     * Cache → override map (known-good hand-curated URLs) → API → fallback.
     *
     * The study Watchtower is published ~2 months before the meeting date, so
     * querying the API by the meeting-week's month returns the wrong issue.
     * The override map (maintained in JWOrgContentUrls) contains the exact
     * correct URLs for each week and is always preferred over the API.
     */
    suspend fun getWatchtowerUrl(weekStart: LocalDate): String {
        val weekStartStr = weekStart.toString()
        val cacheKey = CachedContent.cacheKey(CachedContent.TYPE_WATCHTOWER, weekStartStr)

        val cached = contentDao.getByKey(cacheKey)
        if (cached != null && !cached.isExpired()) {
            Log.d(TAG, "Cache hit for watchtower $weekStart")
            return cached.url ?: fallbackWatchtowerUrl(weekStart)
        }

        // Use the override map first — it has the correct publication issue for each week.
        val overrideUrl = JWOrgContentUrls.watchtowerOverrideUrl(weekStart)
        if (overrideUrl != null) {
            Log.d(TAG, "Using Watchtower override for $weekStart")
            cacheUrl(cacheKey, CachedContent.TYPE_WATCHTOWER, weekStartStr, overrideUrl, weekStart)
            return overrideUrl
        }

        // No override — fall back to API for weeks beyond the override map.
        return try {
            val yearMonth = yearMonthFormat.format(weekStart)
            val response = apiService.getPublicationMedia(pub = PUB_WATCHTOWER, issue = yearMonth)
            val url = response.files?.values?.firstOrNull()
                ?.mp3Files?.firstOrNull()
                ?.file?.url
                ?: fallbackWatchtowerUrl(weekStart)
            cacheUrl(cacheKey, CachedContent.TYPE_WATCHTOWER, weekStartStr, url, weekStart)
            url
        } catch (e: Exception) {
            Log.w(TAG, "API fetch failed for watchtower $weekStart, using fallback", e)
            if (cached != null) return cached.url ?: fallbackWatchtowerUrl(weekStart)
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
        // JSON-based provider has more up-to-date week data than the inline map
        val fromJson = meetingSectionsProvider.bibleReadingUrls(weekStart)
        if (fromJson.isNotEmpty()) return fromJson
        return JWOrgContentUrls.bibleReadingUrls(weekStart)
    }

    private fun fallbackCongregationStudyUrls(weekStart: LocalDate): List<String> {
        val fromJson = meetingSectionsProvider.congregationStudyUrls(weekStart)
        if (fromJson.isNotEmpty()) return fromJson
        return JWOrgContentUrls.congregationStudyUrls(weekStart)
    }

    suspend fun getBibleBookAudio(bookNumber: Int): BibleBookAudio {
        return ensureBibleCatalog()[bookNumber] ?: BibleBookAudio(intro = null, chapters = emptyList())
    }

    private suspend fun ensureBibleCatalog(): Map<Int, BibleBookAudio> {
        bibleCatalog?.let { return it }
        return try {
            val response = apiService.getPublicationMedia(pub = PUB_BIBLE)
            val mp3Files = response.files?.values?.flatMap { it.mp3Files.orEmpty() }.orEmpty()
            val grouped = BibleAudioParser.groupByBook(mp3Files)
            bibleCatalog = grouped
            grouped
        } catch (e: Exception) {
            Log.w(TAG, "Failed to build Bible catalog", e)
            emptyMap()
        }
    }

    suspend fun getKingdomSongs(forceRefresh: Boolean = false): List<KingdomSong> {
        val cached = songDao.getAllSongs()
        val now = System.currentTimeMillis()
        val stale = cached.isEmpty() || now - cached.minOf { it.fetchedAt } > SONG_CACHE_TTL_MILLIS
        if (cached.isNotEmpty() && !forceRefresh && !stale) {
            Log.d(TAG, "Using cached kingdom songs (${cached.size})")
            return cached.toDomain()
        }

        return try {
            val songs = fetchSongsFromApi()
            if (songs.isNotEmpty()) {
                persistSongs(songs, now)
                songs
            } else {
                Log.w(TAG, "JW.org returned no kingdom songs; falling back to cache")
                cached.toDomain()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh kingdom songs", e)
            cached.toDomain()
        }
    }

    private suspend fun fetchSongsFromApi(): List<KingdomSong> {
        val response = apiService.getPublicationMedia(pub = PUB_SONGBOOK)
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

    private suspend fun persistSongs(songs: List<KingdomSong>, fetchedAt: Long) {
        val cachedSongs = songs.map { song ->
            CachedSong(
                number = song.number,
                title = song.title,
                url = song.url,
                language = "E",
                fetchedAt = fetchedAt
            )
        }
        songDao.deleteAll()
        songDao.insertAll(cachedSongs)
        Log.d(TAG, "Cached ${songs.size} kingdom songs")
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
                language = MEDIATOR_LANGUAGE,
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
            val response = mediatorService.getCategory(MEDIATOR_LANGUAGE, CATEGORY_DRAMAS)
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
                language = MEDIATOR_LANGUAGE,
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
