package org.jw.library.auto.data

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import org.jw.library.auto.R
import java.util.Locale
import org.jw.library.auto.data.model.KingdomSong
import org.jw.library.auto.data.model.MediaContent
import org.jw.library.auto.data.bible.BibleBooks
import org.jw.library.auto.data.bible.BibleBooks.Testament
import org.jw.library.auto.util.WeekCalculator

class ContentRepository(
    private val context: Context,
    private val weekCalculator: WeekCalculator = WeekCalculator()
) {
    private val jwOrgRepository = JWOrgRepository(context)

    companion object {
        const val ROOT_ID = "root"
        const val LANG_TOGGLE_ID = "lang-toggle"
        private const val CATEGORY_WEEKLY_MEETINGS = "weekly_meetings"
        const val CATEGORY_THIS_WEEK = "this_week"
        const val CATEGORY_LAST_WEEK = "last_week"
        const val CATEGORY_NEXT_WEEK = "next_week"
        private const val CATEGORY_BIBLE_AND_SONGS = "bible_and_songs"
        private const val CATEGORY_SONGS = "songs"
        private const val SONGS_GROUP_PREFIX = "songs_group_"
        private const val SONG_GROUP_SIZE = 20
        private const val CATEGORY_HEBREW_SCRIPTURES = "hebrew_scriptures"
        private const val CATEGORY_GREEK_SCRIPTURES = "greek_scriptures"
        private const val CATEGORY_BROADCASTING = "broadcasting"
        private val BIBLE_BOOK_ID_REGEX = Regex("bible-(hebrew|greek)-\\d+")
        private const val CHAPTER_GROUP_PREFIX = "-cg-"
        private const val CHAPTER_GROUP_SIZE = 10
        // Static JW.org sample that reliably returns 200 for fallback playback (Kingdom Song)
        private const val SAMPLE_AUDIO = "https://cfp2.jw-cdn.org/a/7f4ac57/1/o/lfb_E_033.mp3"
    }

    suspend fun clearCacheIfVersionChanged() = jwOrgRepository.clearCacheIfVersionChanged()

    fun clearLanguageCaches() = jwOrgRepository.clearLanguageCaches()

    /**
     * Returns a context whose locale matches the active content language so that
     * getString() resolves the correct values-ro/ or values/ resource strings
     * regardless of the device's system locale.
     */
    private fun localizedContext(): Context {
        if (!LanguagePreference.isRomanian(context)) return context
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale("ro"))
        return context.createConfigurationContext(config)
    }

    suspend fun getChildren(parentId: String): List<MediaContent> = when {
        parentId == ROOT_ID -> listOf(
            MediaContent(
                id = LANG_TOGGLE_ID,
                title = LanguagePreference.toggleLabel(context),
                isBrowsable = false,
            ),
            category(CATEGORY_WEEKLY_MEETINGS, R.string.category_weekly_meetings),
            category(CATEGORY_BIBLE_AND_SONGS, R.string.category_bible_and_songs),
            category(CATEGORY_BROADCASTING, R.string.category_broadcasting),
        )

        parentId == CATEGORY_WEEKLY_MEETINGS -> listOf(
            category(CATEGORY_THIS_WEEK, R.string.category_this_week),
            category(CATEGORY_LAST_WEEK, R.string.category_last_week),
            category(CATEGORY_NEXT_WEEK, R.string.category_next_week),
        )
        parentId == CATEGORY_THIS_WEEK -> buildWeeklyContent("this", 0)
        parentId == CATEGORY_LAST_WEEK -> buildWeeklyContent("last", -1)
        parentId == CATEGORY_NEXT_WEEK -> buildWeeklyContent("next", 1)

        parentId == CATEGORY_BIBLE_AND_SONGS -> listOf(
            category(CATEGORY_HEBREW_SCRIPTURES, R.string.category_hebrew_scriptures),
            category(CATEGORY_GREEK_SCRIPTURES, R.string.category_greek_scriptures),
            category(CATEGORY_SONGS, R.string.category_songs),
        )
        parentId == CATEGORY_HEBREW_SCRIPTURES -> bibleBooks(Testament.HEBREW)
        parentId == CATEGORY_GREEK_SCRIPTURES -> bibleBooks(Testament.GREEK)
        parentId.matches(BIBLE_BOOK_ID_REGEX) -> bibleChapters(parentId)
        parentId.contains(CHAPTER_GROUP_PREFIX) -> bibleChapterGroup(parentId)
        parentId == CATEGORY_SONGS -> loadSongGroups()

        parentId.startsWith(SONGS_GROUP_PREFIX) -> {
            val idx = parentId.removePrefix(SONGS_GROUP_PREFIX).toIntOrNull()
            if (idx != null) loadSongGroup(idx) else emptyList()
        }

        parentId == CATEGORY_BROADCASTING -> loadBroadcastingContent()

        else -> emptyList()
    }

    private suspend fun buildWeeklyContent(prefix: String, offsetWeeks: Long): List<MediaContent> {
        val weekInfo = weekCalculator.weekForOffset(offsetWeeks)
        val labelPrefix = "${weekInfo.label} |"

        // JWOrgRepository handles the full cache → API → JSON fallback → hard-coded chain
        val workbookUrl = jwOrgRepository.getMeetingWorkbookUrl(weekInfo.weekStart)
        val watchtowerUrl = jwOrgRepository.getWatchtowerUrl(weekInfo.weekStart)
        val biblePlaylist = jwOrgRepository.getBibleReadingUrls(weekInfo.weekStart)
        val congregationPlaylist = jwOrgRepository.getCongregationStudyUrls(weekInfo.weekStart)
        // Show workbook numbering from the original meeting_sections.json (not the remapped URLs).
        val cbsSubtitle = try {
            val provider = org.jw.library.auto.data.meeting.MeetingSectionsProvider(context)
            val workbookFiles = provider.congregationStudyUrls(weekInfo.weekStart)
                .map { it.substringAfterLast('/') }
            val lessonNums = workbookFiles.mapNotNull {
                Regex("lfb_E_(\\d{3})\\.mp3", RegexOption.IGNORE_CASE)
                    .find(it)?.groupValues?.get(1)?.toIntOrNull()
            }.distinct().sorted()
            when (lessonNums.size) {
                0 -> null
                1 -> "lfb lesson ${lessonNums.first()}"
                else -> "lfb lessons ${lessonNums.first()}–${lessonNums.last()}"
            }
        } catch (_: Throwable) { null }

        return listOf(
            MediaContent(
                id = "$prefix-reading",
                title = "$labelPrefix " + localizedContext().getString(R.string.content_bible_reading),
                streamUrl = biblePlaylist.firstOrNull() ?: SAMPLE_AUDIO,
                playlistUrls = biblePlaylist,
            ),
            MediaContent(
                id = "$prefix-watchtower",
                title = "$labelPrefix " + localizedContext().getString(R.string.content_watchtower),
                streamUrl = watchtowerUrl,
            ),
            MediaContent(
                id = "$prefix-cbs",
                title = "$labelPrefix " + localizedContext().getString(R.string.content_cbs),
                subtitle = cbsSubtitle,
                streamUrl = congregationPlaylist.firstOrNull() ?: SAMPLE_AUDIO,
                playlistUrls = congregationPlaylist,
            ),
            MediaContent(
                id = "$prefix-workbook",
                title = "$labelPrefix " + localizedContext().getString(R.string.content_workbook),
                streamUrl = workbookUrl,
            ),
        )
    }

    private suspend fun loadSongGroups(): List<MediaContent> {
        val songs = jwOrgRepository.getKingdomSongs()
        if (songs.isEmpty()) return fallbackSongs()
        return songs.chunked(SONG_GROUP_SIZE).mapIndexed { index, group ->
            val first = group.first().number.toString().padStart(3, '0')
            val last = group.last().number.toString().padStart(3, '0')
            MediaContent(
                id = "$SONGS_GROUP_PREFIX$index",
                title = localizedContext().getString(R.string.songs_group_label, first, last),
                isBrowsable = true,
            )
        }
    }

    private suspend fun loadSongGroup(index: Int): List<MediaContent> {
        val songs = jwOrgRepository.getKingdomSongs()
        return songs.chunked(SONG_GROUP_SIZE).getOrNull(index)
            ?.map { it.toMediaContent() }
            ?: emptyList()
    }

    private fun KingdomSong.toMediaContent(): MediaContent {
        val displayNumber = number.toString().padStart(3, '0')
        val titleText = "$displayNumber - $title"
        return MediaContent(
            id = "song-$number",
            title = titleText,
            streamUrl = url,
        )
    }

    private fun fallbackSongs(): List<MediaContent> = listOf(
        MediaContent(
            id = "song-demo-1",
            title = localizedContext().getString(R.string.category_songs) + ": Demo",
            streamUrl = SAMPLE_AUDIO,
        )
    )

    private fun bibleBooks(testament: Testament): List<MediaContent> =
        BibleBooks.booksFor(testament).map { book ->
            MediaContent(
                id = "bible-${testament.name.lowercase()}-${book.number}",
                title = book.abbreviation,
                subtitle = book.title,
                isBrowsable = true,
            )
        }

    /**
     * Returns the content of a Bible book folder.
     * Books with > CHAPTER_GROUP_SIZE chapters show "Chapters 1–20" sub-folders (like Kingdom Songs).
     * Shorter books show chapters directly.
     */
    private suspend fun bibleChapters(bookId: String): List<MediaContent> {
        val audio = jwOrgRepository.getBibleBookAudio(bookIdToBookNumber(bookId) ?: return emptyList())
        if (audio.chapters.isEmpty()) return emptyList()

        if (audio.chapters.size <= CHAPTER_GROUP_SIZE) {
            return chapterItems(bookId, audio, 0, audio.chapters.size - 1)
        }

        // Group into folders of CHAPTER_GROUP_SIZE
        return audio.chapters.chunked(CHAPTER_GROUP_SIZE).mapIndexed { index, _ ->
            val first = index * CHAPTER_GROUP_SIZE + 1
            val last = minOf(first + CHAPTER_GROUP_SIZE - 1, audio.chapters.size)
            MediaContent(
                id = "$bookId${CHAPTER_GROUP_PREFIX}$index",
                title = localizedContext().getString(R.string.bible_chapter_group_label, first, last),
                isBrowsable = true,
            )
        }
    }

    /** Returns individual chapter items for a chapter-group folder. */
    private suspend fun bibleChapterGroup(groupId: String): List<MediaContent> {
        val cgIdx = groupId.indexOf(CHAPTER_GROUP_PREFIX)
        if (cgIdx < 0) return emptyList()
        val bookId = groupId.substring(0, cgIdx)
        val groupIndex = groupId.substring(cgIdx + CHAPTER_GROUP_PREFIX.length).toIntOrNull()
            ?: return emptyList()
        val audio = jwOrgRepository.getBibleBookAudio(bookIdToBookNumber(bookId) ?: return emptyList())
        val startIndex = groupIndex * CHAPTER_GROUP_SIZE
        val endIndex = minOf(startIndex + CHAPTER_GROUP_SIZE - 1, audio.chapters.size - 1)
        if (startIndex > endIndex) return emptyList()
        return chapterItems(bookId, audio, startIndex, endIndex)
    }

    /** Builds playable chapter MediaContent items for a range of 0-based chapter indices. */
    private fun chapterItems(
        bookId: String,
        audio: JWOrgRepository.BibleBookAudio,
        startIndex: Int,
        endIndex: Int,
    ): List<MediaContent> {
        val allUrls = buildList {
            audio.intro?.let { add(it) }
            addAll(audio.chapters)
        }
        val introOffset = if (audio.intro != null) 1 else 0
        return (startIndex..endIndex).map { index ->
            val chapterNumber = index + 1
            MediaContent(
                id = "$bookId-ch-$chapterNumber",
                title = localizedContext().getString(R.string.bible_chapter_label, chapterNumber),
                streamUrl = audio.chapters[index],
                // Playlist from this chapter to end of book — pick up where you left off
                playlistUrls = allUrls.drop(introOffset + index),
            )
        }
    }

    private fun bookIdToBookNumber(bookId: String): Int? =
        bookId.split("-").lastOrNull()?.toIntOrNull()

    private suspend fun loadBroadcastingContent(): List<MediaContent> {
        val programs = jwOrgRepository.getMonthlyPrograms()
        val updates = jwOrgRepository.getGoverningBodyUpdates()

        val combined = (programs + updates)
            .sortedByDescending { it.publishedDate }
            .map { item ->
                MediaContent(
                    id = item.id,
                    title = item.title,
                    streamUrl = item.streamUrl,
                )
            }

        if (combined.isEmpty()) {
            return listOf(
                MediaContent(
                    id = "jwb-unavailable",
                    title = localizedContext().getString(R.string.error_content_unavailable),
                    streamUrl = SAMPLE_AUDIO,
                )
            )
        }
        return combined
    }

    private fun category(id: String, @StringRes title: Int) = MediaContent(
        id = id,
        title = localizedContext().getString(title),
        isBrowsable = true,
    )
}
