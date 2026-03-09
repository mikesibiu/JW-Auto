package org.jw.library.auto.data

import android.content.Context
import androidx.annotation.StringRes
import org.jw.library.auto.R
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
        // Static JW.org sample that reliably returns 200 for fallback playback (Kingdom Song)
        private const val SAMPLE_AUDIO = "https://cfp2.jw-cdn.org/a/7f4ac57/1/o/lfb_E_033.mp3"
    }

    suspend fun getChildren(parentId: String): List<MediaContent> = when {
        parentId == ROOT_ID -> listOf(
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

        return listOf(
            MediaContent(
                id = "$prefix-reading",
                title = "$labelPrefix " + context.getString(R.string.content_bible_reading),
                streamUrl = biblePlaylist.firstOrNull() ?: SAMPLE_AUDIO,
                playlistUrls = biblePlaylist,
            ),
            MediaContent(
                id = "$prefix-watchtower",
                title = "$labelPrefix " + context.getString(R.string.content_watchtower),
                streamUrl = watchtowerUrl,
            ),
            MediaContent(
                id = "$prefix-cbs",
                title = "$labelPrefix " + context.getString(R.string.content_cbs),
                streamUrl = congregationPlaylist.firstOrNull() ?: SAMPLE_AUDIO,
                playlistUrls = congregationPlaylist,
            ),
            MediaContent(
                id = "$prefix-workbook",
                title = "$labelPrefix " + context.getString(R.string.content_workbook),
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
                title = context.getString(R.string.songs_group_label, first, last),
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
            title = context.getString(R.string.category_songs) + ": Demo",
            streamUrl = SAMPLE_AUDIO,
        )
    )

    private suspend fun bibleBooks(testament: Testament): List<MediaContent> =
        BibleBooks.booksFor(testament).map { book ->
            val audio = jwOrgRepository.getBibleBookAudio(book.number)
            val playlist = mutableListOf<String>()
            audio.intro?.let { playlist.add(it) }
            playlist.addAll(audio.chapters)
            val streamUrl = playlist.firstOrNull() ?: SAMPLE_AUDIO
            val bookId = "bible-${testament.name.lowercase()}-${book.number}"
            MediaContent(
                id = bookId,
                title = book.abbreviation,
                subtitle = book.title,
                streamUrl = streamUrl,
                playlistUrls = playlist
            )
        }

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
                    title = context.getString(R.string.error_content_unavailable),
                    streamUrl = SAMPLE_AUDIO,
                )
            )
        }
        return combined
    }

    private fun category(id: String, @StringRes title: Int) = MediaContent(
        id = id,
        title = context.getString(title),
        isBrowsable = true,
    )
}
