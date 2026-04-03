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
    fun `parseHeaderChapter extracts Isaiah start chapter from date header`() {
        // wol.jw.org style: "MARCH 2-8 ISAIAH 41"
        val html = "MARCH 2-8 ISAIAH 41"
        val result = provider.parseHeaderChapter(html)!!
        assertEquals(23, result.first)  // Isaiah
        assertEquals(41, result.second)
        assertNull(result.third)
    }

    @Test
    fun `parseHeaderChapter skips MARCH date token and finds ISAIAH`() {
        val html = "MARCH 9-15 ISAIAH 43 some content"
        val result = provider.parseHeaderChapter(html)!!
        assertEquals(23, result.first)
        assertEquals(43, result.second)
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
    fun `full week March 2-8 parses start 41 end 42`() {
        val html = """
            MARCH 2-8 ISAIAH 41
            Congregation Bible Study (30 min.) lfb lessons 66-67 Concluding
            Bible Reading (4 min.) Isa 42:1-13 ( th study 11 )
        """.trimIndent()
        val header = provider.parseHeaderChapter(html)!!
        val endCh = provider.parseBibleReadingEndChapter(html)
        assertEquals(23, header.first)   // Isaiah
        assertEquals(41, header.second)  // start
        assertEquals(42, endCh)          // end
        assertEquals(listOf(66, 67), provider.parseCbsLessons(html))
    }

    @Test
    fun `full week March 9-15 parses start 43 end 44`() {
        val html = """
            MARCH 9-15 ISAIAH 43
            Congregation Bible Study (30 min.) lfb intro to section 11 and lessons 68-69 Concluding
            Bible Reading (4 min.) Isa 44:9-20 ( th study 10 )
        """.trimIndent()
        val header = provider.parseHeaderChapter(html)!!
        val endCh = provider.parseBibleReadingEndChapter(html)
        assertEquals(23, header.first)
        assertEquals(43, header.second)
        assertEquals(44, endCh)
        assertEquals(listOf(68, 69), provider.parseCbsLessons(html))
    }
}
