package org.jw.library.auto.data.meeting

import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Snapshot tests that pin exact audio file names for upcoming weeks.
 *
 * When you update meeting_sections.json, run these tests first.
 * A failure here means a chapter/lesson number is wrong — fix the JSON, not the test.
 *
 * Cross-check schedule against: https://wol.jw.org/en/wol/meetings/r1/lp-e
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WeeklyContentSnapshotTest {

    private val provider = MeetingSectionsProvider(ApplicationProvider.getApplicationContext())

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun bibleFiles(weekStr: String): List<String> =
        provider.bibleReadingUrls(LocalDate.parse(weekStr)).map { it.substringAfterLast('/') }

    private fun cbsFiles(weekStr: String): List<String> =
        provider.congregationStudyUrls(LocalDate.parse(weekStr)).map { it.substringAfterLast('/') }

    // ── Sequence continuity ───────────────────────────────────────────────────

    @Test
    fun `bible reading chapter numbers are strictly sequential across all weeks`() {
        val allWeeks = listOf(
            "2025-11-03", "2025-11-10", "2025-11-17", "2025-11-24",
            "2025-12-01", "2025-12-08", "2025-12-15", "2025-12-22", "2025-12-29",
            "2026-01-05", "2026-01-12", "2026-01-19", "2026-01-26",
            "2026-02-02", "2026-02-09", "2026-02-16", "2026-02-23",
            "2026-03-02", "2026-03-09", "2026-03-16", "2026-03-23",
            // 2026-03-30 (Memorial): John 12 — book change, skip from sequence check
            "2026-04-06", "2026-04-13", "2026-04-20", "2026-04-27"
        )
        // For Isaiah weeks, track that chapter numbers never skip or repeat
        var lastIsaiahChapter = 0
        for (week in allWeeks) {
            val files = bibleFiles(week)
            for (file in files) {
                if (!file.contains("Isa_E_")) continue
                val chap = file.removePrefix("bi12_23_Isa_E_").removeSuffix(".mp3").toIntOrNull() ?: continue
                assertTrue(
                    "Isaiah chapter $chap in week $week comes before or equals previous chapter $lastIsaiahChapter",
                    chap > lastIsaiahChapter
                )
                lastIsaiahChapter = chap
            }
        }
    }

    @Test
    fun `cbs lesson numbers are strictly sequential across all weeks`() {
        val allWeeks = listOf(
            "2025-11-03", "2025-11-10", "2025-11-17", "2025-11-24",
            "2025-12-01", "2025-12-08", "2025-12-15", "2025-12-22", "2025-12-29",
            "2026-01-05", "2026-01-12", "2026-01-19", "2026-01-26",
            "2026-02-02", "2026-02-09", "2026-02-16", "2026-02-23",
            "2026-03-02", "2026-03-09", "2026-03-16", "2026-03-23",
            // 2026-03-30 (Memorial): empty CBS — skip
            "2026-04-06", "2026-04-13", "2026-04-20", "2026-04-27"
        )
        var lastLesson = 0
        for (week in allWeeks) {
            val files = cbsFiles(week)
            for (file in files) {
                val lesson = file.removePrefix("lfb_E_").removeSuffix(".mp3").toIntOrNull() ?: continue
                assertTrue(
                    "CBS lesson $lesson in week $week comes before or equals previous lesson $lastLesson",
                    lesson > lastLesson
                )
                lastLesson = lesson
            }
        }
    }

    // ── Exact snapshots for current + upcoming weeks ──────────────────────────

    @Test
    fun `mar_02_2026 bible reading is Isaiah 41 and 42`() {
        assertEquals(listOf("bi12_23_Isa_E_41.mp3", "bi12_23_Isa_E_42.mp3"), bibleFiles("2026-03-02"))
    }

    @Test
    fun `mar_02_2026 cbs lessons are 66 and 67`() {
        assertEquals(listOf("lfb_E_066.mp3", "lfb_E_067.mp3"), cbsFiles("2026-03-02"))
    }

    @Test
    fun `mar_09_2026 bible reading is Isaiah 43 and 44`() {
        assertEquals(listOf("bi12_23_Isa_E_43.mp3", "bi12_23_Isa_E_44.mp3"), bibleFiles("2026-03-09"))
    }

    @Test
    fun `mar_09_2026 cbs lessons are 68 and 69`() {
        assertEquals(listOf("lfb_E_068.mp3", "lfb_E_069.mp3"), cbsFiles("2026-03-09"))
    }

    @Test
    fun `mar_16_2026 bible reading is Isaiah 45 46 and 47`() {
        assertEquals(listOf("bi12_23_Isa_E_45.mp3", "bi12_23_Isa_E_46.mp3", "bi12_23_Isa_E_47.mp3"), bibleFiles("2026-03-16"))
    }

    @Test
    fun `mar_16_2026 cbs lessons are 70 and 71`() {
        assertEquals(listOf("lfb_E_070.mp3", "lfb_E_071.mp3"), cbsFiles("2026-03-16"))
    }

    @Test
    fun `mar_23_2026 bible reading is Isaiah 48 and 49`() {
        assertEquals(listOf("bi12_23_Isa_E_48.mp3", "bi12_23_Isa_E_49.mp3"), bibleFiles("2026-03-23"))
    }

    @Test
    fun `mar_23_2026 cbs lessons are 72 and 73`() {
        assertEquals(listOf("lfb_E_072.mp3", "lfb_E_073.mp3"), cbsFiles("2026-03-23"))
    }

    @Test
    fun `mar_30_2026 memorial week bible reading is John 12`() {
        assertEquals(listOf("bi12_43_Joh_E_12.mp3"), bibleFiles("2026-03-30"))
    }

    @Test
    fun `mar_30_2026 memorial week has no cbs`() {
        assertTrue(cbsFiles("2026-03-30").isEmpty())
    }

    @Test
    fun `apr_06_2026 bible reading is Isaiah 50 and 51`() {
        assertEquals(listOf("bi12_23_Isa_E_50.mp3", "bi12_23_Isa_E_51.mp3"), bibleFiles("2026-04-06"))
    }

    @Test
    fun `apr_06_2026 cbs lessons are 74 and 75`() {
        assertEquals(listOf("lfb_E_074.mp3", "lfb_E_075.mp3"), cbsFiles("2026-04-06"))
    }

    @Test
    fun `apr_13_2026 bible reading is Isaiah 52 and 53`() {
        assertEquals(listOf("bi12_23_Isa_E_52.mp3", "bi12_23_Isa_E_53.mp3"), bibleFiles("2026-04-13"))
    }

    @Test
    fun `apr_13_2026 cbs lessons are 76 and 77`() {
        assertEquals(listOf("lfb_E_076.mp3", "lfb_E_077.mp3"), cbsFiles("2026-04-13"))
    }

    @Test
    fun `apr_20_2026 bible reading is Isaiah 54 and 55`() {
        assertEquals(listOf("bi12_23_Isa_E_54.mp3", "bi12_23_Isa_E_55.mp3"), bibleFiles("2026-04-20"))
    }

    @Test
    fun `apr_20_2026 cbs lessons are 78 and 79`() {
        assertEquals(listOf("lfb_E_078.mp3", "lfb_E_079.mp3"), cbsFiles("2026-04-20"))
    }

    @Test
    fun `apr_27_2026 bible reading is Isaiah 56 and 57`() {
        assertEquals(listOf("bi12_23_Isa_E_56.mp3", "bi12_23_Isa_E_57.mp3"), bibleFiles("2026-04-27"))
    }

    @Test
    fun `apr_27_2026 cbs lessons are 80 and 81`() {
        assertEquals(listOf("lfb_E_080.mp3", "lfb_E_081.mp3"), cbsFiles("2026-04-27"))
    }
}
