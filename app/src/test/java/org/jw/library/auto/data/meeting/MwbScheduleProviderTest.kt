package org.jw.library.auto.data.meeting

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for MwbScheduleProvider parsing helpers.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MwbScheduleProviderTest {

    private val provider = MwbScheduleProvider(ApplicationProvider.getApplicationContext())

    // ── parseCbsLessons ───────────────────────────────────────────────────────

    @Test
    fun `parseCbsLessons plain two-lesson range`() {
        val html = "Congregation Bible Study (30 min.) lfb lessons 66-67 Concluding"
        assertEquals(listOf(66, 67), provider.parseCbsLessons(html))
    }

    @Test
    fun `parseCbsLessons with section intro prefix`() {
        val html = "Congregation Bible Study (30 min.) lfb intro to section 11 and lessons 68-69 Concluding"
        assertEquals(listOf(68, 69), provider.parseCbsLessons(html))
    }

    @Test
    fun `parseCbsLessons single lesson`() {
        val html = "Congregation Bible Study (30 min.) lfb lessons 80 Concluding"
        assertEquals(listOf(80), provider.parseCbsLessons(html))
    }

    @Test
    fun `parseCbsLessons two-lesson range with en-dash`() {
        val html = "Congregation Bible Study (30 min.) lfb lessons 80–81 Concluding"
        assertEquals(listOf(80, 81), provider.parseCbsLessons(html))
    }

    @Test
    fun `parseCbsLessons no CBS marker returns empty`() {
        assertEquals(emptyList<Int>(), provider.parseCbsLessons("No relevant content here"))
    }

    // ── parseHeaderChapter ────────────────────────────────────────────────────

    @Test
    fun `parseHeaderChapter extracts chapter range from confirmed wol format`() {
        // Confirmed live format: "MARCH 2-8 ISAIAH 41-42"
        val html = "MARCH 2-8 ISAIAH 41-42"
        val result = provider.parseHeaderChapter(html)!!
        assertEquals(23, result.first)   // Isaiah
        assertEquals(41, result.second)  // start
        assertEquals(42, result.third)   // end — from header directly
    }

    @Test
    fun `parseHeaderChapter skips MARCH date token and finds ISAIAH range`() {
        val html = "MARCH 9-15 ISAIAH 43-44 some content"
        val result = provider.parseHeaderChapter(html)!!
        assertEquals(23, result.first)
        assertEquals(43, result.second)
        assertEquals(44, result.third)
    }

    @Test
    fun `parseHeaderChapter falls back to single chapter when no range`() {
        val html = "MARCH 2-8 ISAIAH 41 some content without range"
        val result = provider.parseHeaderChapter(html)!!
        assertEquals(23, result.first)
        assertEquals(41, result.second)
        assertNull(result.third)
    }

    @Test
    fun `parseHeaderChapter returns null when no Bible book found`() {
        assertNull(provider.parseHeaderChapter("No Bible reference here at all 42:5"))
    }

    // ── parseBibleReadingEndChapter ───────────────────────────────────────────

    @Test
    fun `parseBibleReadingEndChapter extracts chapter from Isa 42 reference`() {
        val html = "Bible Reading (4 min.) Isa 42:1-13 ( th study 11 ) APPLY YOURSELF"
        assertEquals(42, provider.parseBibleReadingEndChapter(html))
    }

    @Test
    fun `parseBibleReadingEndChapter extracts chapter from Isa 44 reference`() {
        val html = "Bible Reading (4 min.) Isa 44:9-20 ( th study 10 ) APPLY YOURSELF"
        assertEquals(44, provider.parseBibleReadingEndChapter(html))
    }

    @Test
    fun `parseBibleReadingEndChapter returns null when marker absent`() {
        assertNull(provider.parseBibleReadingEndChapter("No Bible Reading section here"))
    }

    // ── Integration: header + reading end chapter ─────────────────────────────

    @Test
    fun `full week March 2-8 header range takes priority over reading line`() {
        // Confirmed live format: header has "ISAIAH 41-42"; reading line has "Isa 42:1-13"
        val html = """
            MARCH 2-8 ISAIAH 41-42
            Congregation Bible Study (30 min.) lfb lessons 66-67 Concluding
            Bible Reading (4 min.) Isa 42:1-13 ( th study 11 )
        """.trimIndent()
        val header = provider.parseHeaderChapter(html)!!
        assertEquals(23, header.first)   // Isaiah
        assertEquals(41, header.second)  // start from header
        assertEquals(42, header.third)   // end from header — no fallback needed
        assertEquals(listOf(66, 67), provider.parseCbsLessons(html))
    }

    @Test
    fun `full week March 9-15 header range takes priority`() {
        val html = """
            MARCH 9-15 ISAIAH 43-44
            Congregation Bible Study (30 min.) lfb intro to section 11 and lessons 68-69 Concluding
            Bible Reading (4 min.) Isa 44:9-20 ( th study 10 )
        """.trimIndent()
        val header = provider.parseHeaderChapter(html)!!
        assertEquals(23, header.first)
        assertEquals(43, header.second)
        assertEquals(44, header.third)
        assertEquals(listOf(68, 69), provider.parseCbsLessons(html))
    }

    @Test
    fun `reading line is used when header has no range`() {
        // Fallback: header only has single chapter, reading line provides end
        val html = """
            MARCH 2-8 ISAIAH 41
            Congregation Bible Study (30 min.) lfb lessons 66-67 Concluding
            Bible Reading (4 min.) Isa 42:1-13 ( th study 11 )
        """.trimIndent()
        val header = provider.parseHeaderChapter(html)!!
        val endCh = provider.parseBibleReadingEndChapter(html)
        assertEquals(41, header.second)
        assertNull(header.third)          // no range in header
        assertEquals(42, endCh)           // but reading line gives us 42
    }
}
