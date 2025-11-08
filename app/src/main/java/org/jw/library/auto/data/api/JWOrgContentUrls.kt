package org.jw.library.auto.data.api

import org.jw.library.auto.data.model.MeetingContentType
import org.jw.library.auto.data.model.WeekInfo

/**
 * Provides audio content URLs from jw.org
 *
 * VALIDATED URLS - Updated November 7, 2025:
 * - Meeting Workbook: ✅ Verified working (Nov 3-9, 2025)
 * - Watchtower: ✅ Verified working (Nov 2025 issue)
 * - Bible Audio: ⚠️ Old URLs are 404, needs update
 * - CBS: ⚠️ Needs update for current study book
 * - Kingdom Songs: ⚠️ Needs validation
 *
 * URL Pattern (Updated):
 * - Meeting Workbook: https://cfp2.jw-cdn.org/a/[hash]/1/o/mwb_E_[YYYYMM]_[week].mp3
 * - Watchtower: https://cfp2.jw-cdn.org/a/[hash]/1/o/w_E_[YYYYMM]_[article].mp3
 * - Bible audio: (Old pattern broken, needs new API research)
 */
object JWOrgContentUrls {

    /**
     * Bible reading audio URLs (New World Translation)
     * These are SAMPLE URLs - replace with actual working URLs from jw.org
     */
    private val BIBLE_AUDIO_SAMPLES = mapOf(
        "Genesis_1" to "https://download-a.akamaihd.net/files/media_audio/01/nwtsty_E_01_r720P.mp3",
        "Exodus_1" to "https://download-a.akamaihd.net/files/media_audio/02/nwtsty_E_02_r720P.mp3",
        "Matthew_1" to "https://download-a.akamaihd.net/files/media_audio/40/nwtsty_E_40_r720P.mp3"
    )

    /**
     * Meeting Workbook audio URLs - November-December 2025
     * Updated: November 7, 2025 - All weeks validated
     */
    private val WORKBOOK_URLS = mapOf(
        "2025-11-03" to "https://cfp2.jw-cdn.org/a/b5898cd/1/o/mwb_E_202511_01.mp3",  // Nov 3-9
        "2025-11-10" to "https://cfp2.jw-cdn.org/a/056fb19/1/o/mwb_E_202511_02.mp3",  // Nov 10-16
        "2025-11-17" to "https://cfp2.jw-cdn.org/a/52f6fc0/1/o/mwb_E_202511_03.mp3",  // Nov 17-23
        "2025-11-24" to "https://cfp2.jw-cdn.org/a/80c47d/1/o/mwb_E_202511_04.mp3",   // Nov 24-30
        "2025-12-01" to "https://cfp2.jw-cdn.org/a/5fc7877/1/o/mwb_E_202511_05.mp3",  // Dec 1-7
        "2025-12-08" to "https://cfp2.jw-cdn.org/a/8c02906/1/o/mwb_E_202511_06.mp3",  // Dec 8-14
        "2025-12-15" to "https://cfp2.jw-cdn.org/a/28b09db/1/o/mwb_E_202511_07.mp3",  // Dec 15-21
        "2025-12-22" to "https://cfp2.jw-cdn.org/a/6f747e2/1/o/mwb_E_202511_08.mp3",  // Dec 22-28
        "2025-12-29" to "https://cfp2.jw-cdn.org/a/2aa888/1/o/mwb_E_202511_09.mp3"    // Dec 29-Jan 4
    )

    /**
     * Watchtower Study audio URLs - November 2025
     * Updated: November 7, 2025
     * Study dates: January 5 - February 1, 2026
     */
    private val WATCHTOWER_URLS = mapOf(
        "2026-01-05" to "https://cfp2.jw-cdn.org/a/cba6bc/1/o/w_E_202511_01.mp3",     // Jan 5-11: Maintain Your Joy in Old Age
        "2026-01-12" to "https://cfp2.jw-cdn.org/a/cd10e9/1/o/w_E_202511_03.mp3",     // Jan 12-18: Maintain Your Joy as a Caregiver
        "2026-01-19" to "https://cfp2.jw-cdn.org/a/46091e/1/o/w_E_202511_04.mp3",     // Jan 19-25: Consider Our Sympathetic High Priest
        "2026-01-26" to "https://cfp2.jw-cdn.org/a/c69c8d8/3/o/w_E_202511_05.mp3"     // Jan 26-Feb 1: "You Are Someone Very Precious"!
    )

    /**
     * Congregation Bible Study audio
     * TODO: Update with current study book
     */
    private const val CBS_SAMPLE = "https://download-a.akamaihd.net/files/media_audio/bf/bhs_E.mp3"

    /**
     * Sample Kingdom Song
     */
    private const val SONG_SAMPLE = "https://download-a.akamaihd.net/files/media_audio/sng/nwtsty_E_sng_001.mp3"

    /**
     * Get audio URL for specific meeting content type and week
     *
     * Uses weekInfo.getWeekKey() to select the correct URL for that week
     * Automatically selects the right content based on the week's start date
     */
    fun getContentUrl(type: MeetingContentType, weekInfo: WeekInfo): String {
        // Format the week start date as YYYY-MM-DD for lookup
        val weekKey = weekInfo.getWeekKey()

        return when (type) {
            MeetingContentType.BIBLE_READING -> {
                // Demo: Returns Genesis 1
                // TODO: Calculate which Bible chapters are assigned for this week
                BIBLE_AUDIO_SAMPLES["Genesis_1"] ?: BIBLE_AUDIO_SAMPLES.values.first()
            }
            MeetingContentType.WATCHTOWER -> {
                // Look up Watchtower by week start date (study dates are in January 2026)
                WATCHTOWER_URLS[weekKey] ?: WATCHTOWER_URLS.values.firstOrNull() ?: ""
            }
            MeetingContentType.CBS -> CBS_SAMPLE
            MeetingContentType.WORKBOOK -> {
                // Look up Workbook by week start date (Nov-Dec 2025)
                WORKBOOK_URLS[weekKey] ?: WORKBOOK_URLS.values.firstOrNull() ?: ""
            }
        }
    }

    /**
     * Get Kingdom Song URL by song number
     */
    fun getSongUrl(songNumber: Int): String {
        // Demo: Always returns song 1
        // TODO: Format with actual song number (001-151)
        return SONG_SAMPLE
    }

    /**
     * Check if URL is valid and accessible
     * Returns true for demo purposes
     */
    suspend fun isUrlAccessible(url: String): Boolean {
        // TODO: Implement HEAD request to check if URL exists
        return true
    }

    /**
     * Get available date ranges for content
     * Useful for debugging and displaying to users
     */
    fun getAvailableWeeks(): Map<String, List<String>> {
        return mapOf(
            "Workbook" to WORKBOOK_URLS.keys.sorted(),
            "Watchtower" to WATCHTOWER_URLS.keys.sorted()
        )
    }

    /**
     * Check if content is available for a specific week
     */
    fun hasContentForWeek(type: MeetingContentType, weekKey: String): Boolean {
        return when (type) {
            MeetingContentType.WORKBOOK -> WORKBOOK_URLS.containsKey(weekKey)
            MeetingContentType.WATCHTOWER -> WATCHTOWER_URLS.containsKey(weekKey)
            MeetingContentType.BIBLE_READING -> BIBLE_AUDIO_SAMPLES.isNotEmpty()
            MeetingContentType.CBS -> true
        }
    }

    /**
     * Helper to get actual URLs from jw.org
     * Instructions for manual URL collection:
     *
     * 1. Open browser to: https://www.jw.org/en/library/jw-meeting-workbook/
     * 2. Find current month's workbook
     * 3. Click "Audio download options"
     * 4. Right-click MP3 download → "Copy link address"
     * 5. Update WORKBOOK_SAMPLE constant above
     *
     * Repeat for other content types:
     * - Bible: https://www.jw.org/en/library/bible/study-bible/books/
     * - Watchtower: https://www.jw.org/en/library/magazines/
     */
}
