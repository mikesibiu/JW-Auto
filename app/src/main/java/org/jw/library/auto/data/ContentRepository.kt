package org.jw.library.auto.data

import android.content.Context
import androidx.annotation.StringRes
import org.jw.library.auto.R
import org.jw.library.auto.data.api.JWOrgContentUrls
import org.jw.library.auto.data.cache.CachedContentReader
import org.jw.library.auto.data.model.MediaContent
import org.jw.library.auto.util.WeekCalculator

class ContentRepository(
    private val context: Context,
    private val weekCalculator: WeekCalculator = WeekCalculator()
) {
    private val cacheReader = CachedContentReader(context)
    companion object {
        const val ROOT_ID = "root"
        private const val CATEGORY_THIS_WEEK = "this_week"
        private const val CATEGORY_LAST_WEEK = "last_week"
        private const val CATEGORY_NEXT_WEEK = "next_week"
        private const val CATEGORY_SONGS = "songs"
        private const val SAMPLE_AUDIO = "https://download-a.akamaihd.net/files/media_music/9b/mp3/1102018240_un_cbs_64k.mp3"
    }

    fun getChildren(parentId: String): List<MediaContent> = when (parentId) {
        ROOT_ID -> listOf(
            category(CATEGORY_THIS_WEEK, R.string.category_this_week),
            category(CATEGORY_LAST_WEEK, R.string.category_last_week),
            category(CATEGORY_NEXT_WEEK, R.string.category_next_week),
            category(CATEGORY_SONGS, R.string.category_songs),
        )

        CATEGORY_THIS_WEEK -> buildWeeklyContent("this", 0)
        CATEGORY_LAST_WEEK -> buildWeeklyContent("last", -1)
        CATEGORY_NEXT_WEEK -> buildWeeklyContent("next", 1)
        CATEGORY_SONGS -> sampleSongs()
        else -> emptyList()
    }

    private fun buildWeeklyContent(prefix: String, offsetWeeks: Long): List<MediaContent> {
        val weekInfo = weekCalculator.weekForOffset(offsetWeeks)
        val labelPrefix = "${weekInfo.label} |"

        // Try cache first, fall back to hard-coded URLs
        val workbookUrl = cacheReader.getWorkbookUrl(weekInfo.weekStart)
            ?: JWOrgContentUrls.meetingWorkbookUrl(weekInfo.weekStart)
        val watchtowerUrl = cacheReader.getWatchtowerUrl(weekInfo.weekStart)
            ?: JWOrgContentUrls.watchtowerStudyUrl(weekInfo.weekStart)
        val biblePlaylist = cacheReader.getBibleReadingUrls(weekInfo.weekStart)
            ?: JWOrgContentUrls.bibleReadingUrls(weekInfo.weekStart)
        val congregationPlaylist = cacheReader.getCongregationStudyUrls(weekInfo.weekStart)
            ?: JWOrgContentUrls.congregationStudyUrls(weekInfo.weekStart)
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

    private fun sampleSongs(): List<MediaContent> = listOf(
        MediaContent(
            id = "song-1",
            title = context.getString(R.string.category_songs) + ": 001", // Kingdom Song 1
            streamUrl = SAMPLE_AUDIO,
        ),
        MediaContent(
            id = "song-2",
            title = context.getString(R.string.category_songs) + ": 011", // Kingdom Song 11
            streamUrl = SAMPLE_AUDIO,
        ),
        MediaContent(
            id = "song-3",
            title = context.getString(R.string.category_songs) + ": 062", // Kingdom Song 62
            streamUrl = SAMPLE_AUDIO,
        ),
    )

    private fun category(id: String, @StringRes title: Int) = MediaContent(
        id = id,
        title = context.getString(title),
        isBrowsable = true,
    )

}
