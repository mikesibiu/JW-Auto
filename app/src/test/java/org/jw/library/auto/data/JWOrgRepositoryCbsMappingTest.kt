package org.jw.library.auto.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Verifies that CBS lesson numbers from the Meeting Workbook are remapped to the
 * correct lfb_E_### files using jw.org's LFB catalog. Requires internet.
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [28])
class JWOrgRepositoryCbsMappingTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repo = JWOrgRepository(context)

    private fun List<String>.files() = this.map { it.substringAfterLast('/') }

    @Test
    fun cbs_mapping_last_this_next_weeks_are_correct() = runBlocking {
        // Last week: 2026-03-09 → lessons 68–69 → lfb_E_080/081
        val last = repo.getCongregationStudyUrls(LocalDate.parse("2026-03-09")).files()
        assertEquals(listOf("lfb_E_080.mp3", "lfb_E_081.mp3"), last)

        // This week: 2026-03-16 → lessons 70–71 → lfb_E_082/083
        val thisWeek = repo.getCongregationStudyUrls(LocalDate.parse("2026-03-16")).files()
        assertEquals(listOf("lfb_E_082.mp3", "lfb_E_083.mp3"), thisWeek)

        // Next week: 2026-03-23 → lessons 72–73 → lfb_E_084/085
        val next = repo.getCongregationStudyUrls(LocalDate.parse("2026-03-23")).files()
        assertEquals(listOf("lfb_E_084.mp3", "lfb_E_085.mp3"), next)
    }

    @Test
    fun cbs_memorial_week_is_empty() = runBlocking {
        // Memorial week has no CBS
        val memorial = repo.getCongregationStudyUrls(LocalDate.parse("2026-03-30")).files()
        assertTrue(memorial.isEmpty())
    }
}
