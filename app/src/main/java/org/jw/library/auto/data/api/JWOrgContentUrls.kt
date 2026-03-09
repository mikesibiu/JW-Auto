package org.jw.library.auto.data.api

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object JWOrgContentUrls {
    private val YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM")

    // Fallback targets allow demo playback even when specific week URLs are missing
    private const val WORKBOOK_FALLBACK = "https://b.jw-cdn.org/files/media_audio/mwb/202412_mwb_E.mp3"
    private const val WATCHTOWER_FALLBACK = "https://b.jw-cdn.org/files/media_audio/w/202401_w_E.mp3"

    fun meetingWorkbookUrl(weekStart: LocalDate): String {
        val key = weekStart.toString()
        val override = WORKBOOK_OVERRIDES[key]
        if (override != null) return override
        val yearMonth = YEAR_MONTH_FORMAT.format(weekStart)
        return "https://b.jw-cdn.org/files/media_audio/mwb/${yearMonth}_mwb_E.mp3"
    }

    fun watchtowerStudyUrl(weekStart: LocalDate): String {
        val key = weekStart.toString()
        val override = WATCHTOWER_OVERRIDES[key]
        if (override != null) return override
        return WATCHTOWER_FALLBACK
    }

    /** Returns the override URL if one is present for this week, null otherwise. */
    fun watchtowerOverrideUrl(weekStart: LocalDate): String? =
        WATCHTOWER_OVERRIDES[weekStart.toString()]

    private val WORKBOOK_OVERRIDES = mapOf(
        "2025-11-03" to "https://cfp2.jw-cdn.org/a/b5898cd/1/o/mwb_E_202511_01.mp3",
        "2025-11-10" to "https://cfp2.jw-cdn.org/a/056fb19/1/o/mwb_E_202511_02.mp3",
        "2025-11-17" to "https://cfp2.jw-cdn.org/a/52f6fc0/1/o/mwb_E_202511_03.mp3",
        "2025-11-24" to "https://cfp2.jw-cdn.org/a/80c47d/1/o/mwb_E_202511_04.mp3",
        "2025-12-01" to "https://cfp2.jw-cdn.org/a/5fc7877/1/o/mwb_E_202511_05.mp3",
        "2025-12-08" to "https://cfp2.jw-cdn.org/a/8c02906/1/o/mwb_E_202511_06.mp3",
        "2025-12-15" to "https://cfp2.jw-cdn.org/a/28b09db/1/o/mwb_E_202511_07.mp3",
        "2025-12-22" to "https://cfp2.jw-cdn.org/a/6f747e2/1/o/mwb_E_202511_08.mp3",
        "2025-12-29" to "https://cfp2.jw-cdn.org/a/2aa888/1/o/mwb_E_202511_09.mp3",
        "2026-01-05" to "https://cfp2.jw-cdn.org/a/394ee97/1/o/mwb_E_202601_01.mp3",
        "2026-01-12" to "https://cfp2.jw-cdn.org/a/884cd3/1/o/mwb_E_202601_02.mp3",
        "2026-01-19" to "https://cfp2.jw-cdn.org/a/d8bc927/1/o/mwb_E_202601_03.mp3",
        "2026-01-26" to "https://cfp2.jw-cdn.org/a/611194/1/o/mwb_E_202601_04.mp3",
        "2026-02-02" to "https://cfp2.jw-cdn.org/a/bbeaa61/1/o/mwb_E_202601_05.mp3",
        "2026-02-09" to "https://cfp2.jw-cdn.org/a/b72739/1/o/mwb_E_202601_06.mp3",
        "2026-02-16" to "https://cfp2.jw-cdn.org/a/c97c260/1/o/mwb_E_202601_07.mp3",
        "2026-02-23" to "https://cfp2.jw-cdn.org/a/ce27e58/1/o/mwb_E_202601_08.mp3",
    )

    private val WATCHTOWER_OVERRIDES = mapOf(
        "2025-11-10" to "https://cfp2.jw-cdn.org/a/861929/1/o/w_E_202509_01.mp3",
        "2025-11-17" to "https://cfp2.jw-cdn.org/a/6a6aca/1/o/w_E_202509_02.mp3",
        "2025-11-24" to "https://cfp2.jw-cdn.org/a/155ba6/1/o/w_E_202509_03.mp3",
        "2025-12-01" to "https://cfp2.jw-cdn.org/a/4edd2f/1/o/w_E_202509_04.mp3",
        "2025-12-08" to "https://cfp2.jw-cdn.org/a/bc2fdcd/1/o/w_E_202510_02.mp3",
        "2025-12-15" to "https://cfp2.jw-cdn.org/a/dba45f/1/o/w_E_202510_03.mp3",
        "2025-12-22" to "https://cfp2.jw-cdn.org/a/d9f278/1/o/w_E_202510_04.mp3",
        "2025-12-29" to "https://cfp2.jw-cdn.org/a/208489/1/o/w_E_202510_05.mp3",
        "2026-01-05" to "https://cfp2.jw-cdn.org/a/cba6bc/1/o/w_E_202511_01.mp3",
        "2026-01-12" to "https://cfp2.jw-cdn.org/a/cd10e9/1/o/w_E_202511_03.mp3",
        "2026-01-19" to "https://cfp2.jw-cdn.org/a/46091e/1/o/w_E_202511_04.mp3",
        "2026-01-26" to "https://cfp2.jw-cdn.org/a/c69c8d8/3/o/w_E_202511_05.mp3",
        "2026-02-02" to "https://cfp2.jw-cdn.org/a/6fa2e3/1/o/w_E_202512_01.mp3",
        "2026-02-09" to "https://cfp2.jw-cdn.org/a/e58ace/1/o/w_E_202512_02.mp3",
        "2026-02-16" to "https://cfp2.jw-cdn.org/a/3f127c5/1/o/w_E_202512_03.mp3",
        "2026-02-23" to "https://cfp2.jw-cdn.org/a/d62b6e/1/o/w_E_202512_04.mp3",
        "2026-03-02" to "https://cfp2.jw-cdn.org/a/b72bcf/1/o/w_E_202601_01.mp3",
        "2026-03-09" to "https://cfp2.jw-cdn.org/a/10da6f/1/o/w_E_202601_02.mp3",
        "2026-03-16" to "https://cfp2.jw-cdn.org/a/c9fd64/1/o/w_E_202601_03.mp3",
        "2026-03-23" to "https://cfp2.jw-cdn.org/a/4e84476/3/o/w_E_202601_04.mp3",
        "2026-03-30" to "https://cfp2.jw-cdn.org/a/1b40b22/1/o/w_E_202601_05.mp3",
        "2026-04-06" to "https://cfp2.jw-cdn.org/a/b72976b/1/o/w_E_202602_01.mp3",
        "2026-04-13" to "https://cfp2.jw-cdn.org/a/94e731/1/o/w_E_202602_02.mp3",
        "2026-04-20" to "https://cfp2.jw-cdn.org/a/f4acbdf/1/o/w_E_202602_03.mp3",
        "2026-04-27" to "https://cfp2.jw-cdn.org/a/3cf4546/1/o/w_E_202602_04.mp3",
    )

}
