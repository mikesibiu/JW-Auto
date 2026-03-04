package org.jw.library.auto.data.meeting

import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MeetingSectionsProviderTest {

    private val provider = MeetingSectionsProvider(ApplicationProvider.getApplicationContext())

    @Test
    fun `returns bible and cbs entries for known week`() {
        val week = LocalDate.parse("2025-12-01")
        val bible = provider.bibleReadingUrls(week)
        val cbs = provider.congregationStudyUrls(week)
        assertEquals(3, bible.size)
        assertEquals(2, cbs.size)
    }

    @Test
    fun `returns empty lists for unknown week`() {
        val week = LocalDate.parse("2024-01-01")
        assertTrue(provider.bibleReadingUrls(week).isEmpty())
        assertTrue(provider.congregationStudyUrls(week).isEmpty())
    }
}
