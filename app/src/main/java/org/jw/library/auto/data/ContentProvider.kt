package org.jw.library.auto.data

import android.content.Context
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import org.jw.library.auto.R
import org.jw.library.auto.data.model.ContentCategory
import org.jw.library.auto.data.model.MediaContent
import org.jw.library.auto.data.model.MeetingContentType
import org.jw.library.auto.util.WeekCalculator
import org.jw.library.auto.data.api.JWOrgContentUrls

/**
 * Provides content hierarchy for Android Auto browsing
 */
class ContentProvider(private val context: Context) {

    companion object {
        const val ROOT_ID = "root"
        const val MEDIA_ID_SEPARATOR = "|"
    }

    /**
     * Get root-level categories
     */
    fun getRootCategories(): List<MediaItem> {
        return ContentCategory.values().map { category ->
            createBrowsableMediaItem(
                id = category.id,
                title = context.getString(category.titleResId),
                subtitle = getCategorySubtitle(category)
            )
        }
    }

    /**
     * Get content for a specific category
     */
    fun getContentForCategory(categoryId: String): List<MediaItem> {
        return when (categoryId) {
            ContentCategory.THIS_WEEK.id -> getMeetingContent(WeekCalculator.getCurrentWeek())
            ContentCategory.LAST_WEEK.id -> getMeetingContent(WeekCalculator.getLastWeek())
            ContentCategory.NEXT_WEEK.id -> getMeetingContent(WeekCalculator.getNextWeek())
            ContentCategory.SONGS.id -> getKingdomSongs()
            ContentCategory.DRAMAS.id -> getBibleDramas()
            else -> emptyList()
        }
    }

    /**
     * Get meeting content for a specific week
     */
    private fun getMeetingContent(weekInfo: org.jw.library.auto.data.model.WeekInfo): List<MediaItem> {
        val weekLabel = WeekCalculator.formatWeekRange(weekInfo)

        return MeetingContentType.values().map { type ->
            createPlayableMediaItem(
                id = "${type.id}${MEDIA_ID_SEPARATOR}${weekInfo.weekStart}",
                title = context.getString(type.titleResId),
                subtitle = weekLabel,
                mediaUri = getMockMediaUri(type, weekInfo) // TODO: Replace with actual API call
            )
        }
    }

    /**
     * Get Kingdom Songs
     */
    private fun getKingdomSongs(): List<MediaItem> {
        // Demo: Sample songs with real URL structure
        return listOf(
            createPlayableMediaItem(
                id = "song${MEDIA_ID_SEPARATOR}1",
                title = "Jehovah's Attributes",
                subtitle = "Song 1",
                mediaUri = JWOrgContentUrls.getSongUrl(1)
            ),
            createPlayableMediaItem(
                id = "song${MEDIA_ID_SEPARATOR}2",
                title = "Jehovah Is Your Name",
                subtitle = "Song 2",
                mediaUri = JWOrgContentUrls.getSongUrl(2)
            )
        )
    }

    /**
     * Get Bible Dramas
     */
    private fun getBibleDramas(): List<MediaItem> {
        // Demo: Sample Bible drama
        return listOf(
            createPlayableMediaItem(
                id = "drama${MEDIA_ID_SEPARATOR}creation",
                title = "The Bible's Story of Creation",
                subtitle = "Bible Drama",
                mediaUri = "https://download-a.akamaihd.net/files/media_audio/bd/nwtsty_E_bd_01_r720P.mp3" // Sample URL
            )
        )
    }

    /**
     * Create a browsable media item (folder)
     */
    private fun createBrowsableMediaItem(
        id: String,
        title: String,
        subtitle: String? = null
    ): MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(id)
            .setTitle(title)
            .setSubtitle(subtitle)
            .build()

        return MediaItem(description, MediaItem.FLAG_BROWSABLE)
    }

    /**
     * Create a playable media item (audio file)
     */
    private fun createPlayableMediaItem(
        id: String,
        title: String,
        subtitle: String? = null,
        mediaUri: String
    ): MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(id)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setMediaUri(android.net.Uri.parse(mediaUri))
            .build()

        return MediaItem(description, MediaItem.FLAG_PLAYABLE)
    }

    /**
     * Get subtitle for category
     */
    private fun getCategorySubtitle(category: ContentCategory): String? {
        return when (category) {
            ContentCategory.THIS_WEEK -> WeekCalculator.formatWeekRange(WeekCalculator.getCurrentWeek())
            ContentCategory.LAST_WEEK -> WeekCalculator.formatWeekRange(WeekCalculator.getLastWeek())
            ContentCategory.NEXT_WEEK -> WeekCalculator.formatWeekRange(WeekCalculator.getNextWeek())
            else -> null
        }
    }

    /**
     * Get media URI from jw.org content URLs
     * DEMO: Uses sample URLs from JWOrgContentUrls
     * PRODUCTION: Replace JWOrgContentUrls with real API integration
     */
    private fun getMockMediaUri(type: MeetingContentType, weekInfo: org.jw.library.auto.data.model.WeekInfo): String {
        return JWOrgContentUrls.getContentUrl(type, weekInfo)
    }

    /**
     * Get current date string for display
     */
    private fun getCurrentDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        val month = calendar.getDisplayName(
            java.util.Calendar.MONTH,
            java.util.Calendar.LONG,
            java.util.Locale.getDefault()
        )
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val year = calendar.get(java.util.Calendar.YEAR)
        return "$month $day, $year"
    }
}
