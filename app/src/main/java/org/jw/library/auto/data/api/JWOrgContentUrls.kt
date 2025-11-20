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

    fun bibleReadingUrls(weekStart: LocalDate): List<String> {
        val key = weekStart.toString()
        return MEETING_SECTIONS[key]?.bibleReading ?: emptyList()
    }

    fun congregationStudyUrls(weekStart: LocalDate): List<String> {
        val key = weekStart.toString()
        return MEETING_SECTIONS[key]?.congregationStudy ?: emptyList()
    }

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

    private data class MeetingSections(
        val bibleReading: List<String>,
        val congregationStudy: List<String>,
    )

    private val MEETING_SECTIONS = mapOf(
        "2025-11-03" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/6f232a3/1/o/bi12_22_Ca_E_02.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/7f4ac57/1/o/lfb_E_033.mp3",
                "https://cfp2.jw-cdn.org/a/759436/1/o/lfb_E_034.mp3",
            )
        ),
        "2025-11-10" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/a509da/1/o/bi12_22_Ca_E_03.mp3",
                "https://cfp2.jw-cdn.org/a/a985a6/1/o/bi12_22_Ca_E_04.mp3",
                "https://cfp2.jw-cdn.org/a/71cb20/1/o/bi12_22_Ca_E_05.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/0a687c/1/o/lfb_E_035.mp3",
                "https://cfp2.jw-cdn.org/a/15065e/1/o/lfb_E_036.mp3",
            )
        ),
        "2025-11-17" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/7fb7bc/1/o/bi12_22_Ca_E_06.mp3",
                "https://cfp2.jw-cdn.org/a/66c957e/1/o/bi12_22_Ca_E_07.mp3",
                "https://cfp2.jw-cdn.org/a/ca3686/1/o/bi12_22_Ca_E_08.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/403ddc0/1/o/lfb_E_037.mp3",
                "https://cfp2.jw-cdn.org/a/63395b/1/o/lfb_E_038.mp3",
            )
        ),
        "2025-11-24" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/b19272/1/o/bi12_23_Isa_E_01.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/51d9f2/1/o/lfb_E_039.mp3",
                "https://cfp2.jw-cdn.org/a/d16450/1/o/lfb_E_040.mp3",
            )
        ),
        "2025-12-01" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/2d02b6/1/o/bi12_23_Isa_E_02.mp3",
                "https://cfp2.jw-cdn.org/a/d2d454/1/o/bi12_23_Isa_E_03.mp3",
                "https://cfp2.jw-cdn.org/a/022522/1/o/bi12_23_Isa_E_04.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/f68f93/1/o/lfb_E_041.mp3",
                "https://cfp2.jw-cdn.org/a/4dd7c77/1/o/lfb_E_042.mp3",
            )
        ),
        "2025-12-08" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/0727398/1/o/bi12_23_Isa_E_05.mp3",
                "https://cfp2.jw-cdn.org/a/8af58d/1/o/bi12_23_Isa_E_06.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/3c11f2/1/o/lfb_E_043.mp3",
                "https://cfp2.jw-cdn.org/a/08e860/1/o/lfb_E_044.mp3",
            )
        ),
        "2025-12-15" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/ea50425/1/o/bi12_23_Isa_E_07.mp3",
                "https://cfp2.jw-cdn.org/a/af69fa/1/o/bi12_23_Isa_E_08.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/208f71/1/o/lfb_E_045.mp3",
            )
        ),
        "2025-12-22" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/571d9f/1/o/bi12_23_Isa_E_09.mp3",
                "https://cfp2.jw-cdn.org/a/5880ca/1/o/bi12_23_Isa_E_10.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/0c8eeb/1/o/lfb_E_046.mp3",
                "https://cfp2.jw-cdn.org/a/87e49a/1/o/lfb_E_047.mp3",
            )
        ),
        "2025-12-29" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/f94f77/1/o/bi12_23_Isa_E_11.mp3",
                "https://cfp2.jw-cdn.org/a/7ac043/1/o/bi12_23_Isa_E_12.mp3",
                "https://cfp2.jw-cdn.org/a/d132f1/1/o/bi12_23_Isa_E_13.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/0073379/1/o/lfb_E_048.mp3",
                "https://cfp2.jw-cdn.org/a/5c22bf/1/o/lfb_E_049.mp3",
            )
        ),
        "2026-01-05" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/2adc6f0/1/o/bi12_23_Isa_E_14.mp3",
                "https://cfp2.jw-cdn.org/a/146977/1/o/bi12_23_Isa_E_15.mp3",
                "https://cfp2.jw-cdn.org/a/e71f21/1/o/bi12_23_Isa_E_16.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/3cdb4b1/1/o/lfb_E_050.mp3",
                "https://cfp2.jw-cdn.org/a/48d261a/1/o/lfb_E_051.mp3",
            )
        ),
        "2026-01-12" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/08e9fb/1/o/bi12_23_Isa_E_17.mp3",
                "https://cfp2.jw-cdn.org/a/dfd597/1/o/bi12_23_Isa_E_18.mp3",
                "https://cfp2.jw-cdn.org/a/4f20ec3/1/o/bi12_23_Isa_E_19.mp3",
                "https://cfp2.jw-cdn.org/a/c871a1/1/o/bi12_23_Isa_E_20.mp3",
                "https://cfp2.jw-cdn.org/a/f74e414/1/o/bi12_23_Isa_E_21.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/93b9f9a/1/o/lfb_E_052.mp3",
                "https://cfp2.jw-cdn.org/a/ee61fe/1/o/lfb_E_053.mp3",
            )
        ),
        "2026-01-19" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/cad88b/1/o/bi12_23_Isa_E_22.mp3",
                "https://cfp2.jw-cdn.org/a/8692cc1/1/o/bi12_23_Isa_E_23.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/e109b19/1/o/lfb_E_054.mp3",
            )
        ),
        "2026-01-26" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/9bd174/1/o/bi12_23_Isa_E_24.mp3",
                "https://cfp2.jw-cdn.org/a/5c0ff1c/1/o/bi12_23_Isa_E_25.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/a1b212/1/o/lfb_E_055.mp3",
            )
        ),
        "2026-02-02" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/e18340/1/o/bi12_23_Isa_E_26.mp3",
                "https://cfp2.jw-cdn.org/a/96003f/1/o/bi12_23_Isa_E_27.mp3",
                "https://cfp2.jw-cdn.org/a/2d1b1fc/1/o/bi12_23_Isa_E_28.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/8d3330/1/o/lfb_E_056.mp3",
                "https://cfp2.jw-cdn.org/a/c786616/1/o/lfb_E_057.mp3",
            )
        ),
        "2026-02-09" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/c4f37e/1/o/bi12_23_Isa_E_29.mp3",
                "https://cfp2.jw-cdn.org/a/e2a8c7/1/o/bi12_23_Isa_E_30.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/7fd902/1/o/lfb_E_058.mp3",
                "https://cfp2.jw-cdn.org/a/4c2f33/1/o/lfb_E_059.mp3",
            )
        ),
        "2026-02-16" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/2fd33a/1/o/bi12_23_Isa_E_31.mp3",
                "https://cfp2.jw-cdn.org/a/9f5d01/1/o/bi12_23_Isa_E_32.mp3",
                "https://cfp2.jw-cdn.org/a/7fa8dc/1/o/bi12_23_Isa_E_33.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/416c29/1/o/lfb_E_060.mp3",
                "https://cfp2.jw-cdn.org/a/82c8ab/1/o/lfb_E_061.mp3",
            )
        ),
        "2026-02-23" to MeetingSections(
            bibleReading = listOf(
                "https://cfp2.jw-cdn.org/a/4baacf/1/o/bi12_23_Isa_E_34.mp3",
                "https://cfp2.jw-cdn.org/a/ca7e1e/1/o/bi12_23_Isa_E_35.mp3",
            ),
            congregationStudy = listOf(
                "https://cfp2.jw-cdn.org/a/14cb06c/1/o/lfb_E_062.mp3",
                "https://cfp2.jw-cdn.org/a/e3530e/1/o/lfb_E_063.mp3",
            )
        ),
    )
}
