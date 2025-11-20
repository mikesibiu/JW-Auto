package org.jw.library.auto.data.api

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class JWOrgContentUrlsTest {
    @Test
    fun `meeting workbook url uses override when available`() {
        val novemberWeek = LocalDate.of(2025, 11, 3)
        val decemberWeek = LocalDate.of(2025, 12, 1)

        assertEquals(
            "https://cfp2.jw-cdn.org/a/b5898cd/1/o/mwb_E_202511_01.mp3",
            JWOrgContentUrls.meetingWorkbookUrl(novemberWeek)
        )
        assertEquals(
            "https://cfp2.jw-cdn.org/a/5fc7877/1/o/mwb_E_202511_05.mp3",
            JWOrgContentUrls.meetingWorkbookUrl(decemberWeek)
        )
    }

    @Test
    fun `watchtower falls back to default when no override exists`() {
        val week = LocalDate.of(2026, 6, 1)
        assertEquals(
            "https://b.jw-cdn.org/files/media_audio/w/202401_w_E.mp3",
            JWOrgContentUrls.watchtowerStudyUrl(week)
        )
    }

    @Test
    fun `bible reading playlist exposes sequential chapters`() {
        val playlist = JWOrgContentUrls.bibleReadingUrls(LocalDate.of(2025, 11, 10))
        assertEquals(
            listOf(
                "https://cfp2.jw-cdn.org/a/a509da/1/o/bi12_22_Ca_E_03.mp3",
                "https://cfp2.jw-cdn.org/a/a985a6/1/o/bi12_22_Ca_E_04.mp3",
                "https://cfp2.jw-cdn.org/a/71cb20/1/o/bi12_22_Ca_E_05.mp3",
            ),
            playlist
        )
    }
}
