package org.jw.library.auto.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Test for JWOrgRepository - verifies API integration works
 * Note: These tests make real network calls to jw.org
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class JWOrgRepositoryTest {

    private val repository = JWOrgRepository(ApplicationProvider.getApplicationContext())

    @Test
    fun `getMeetingWorkbookUrl returns valid URL`() = runBlocking {
        // Test with a known week
        val weekStart = LocalDate.of(2025, 11, 3) // Nov 3, 2025 (Monday)
        val url = repository.getMeetingWorkbookUrl(weekStart)

        println("Meeting Workbook URL for $weekStart: $url")
        assert(url.isNotEmpty())
        assert(url.startsWith("https://"))
        assert(url.contains(".mp3"))
    }

    @Test
    fun `getWatchtowerUrl returns valid URL`() = runBlocking {
        val weekStart = LocalDate.of(2025, 11, 10) // Nov 10, 2025
        val url = repository.getWatchtowerUrl(weekStart)

        println("Watchtower URL for $weekStart: $url")
        assert(url.isNotEmpty())
        assert(url.startsWith("https://"))
        assert(url.contains(".mp3"))
    }

    @Test
    fun `getBibleReadingUrls returns playlist`() = runBlocking {
        val weekStart = LocalDate.of(2025, 11, 10) // Nov 10, 2025
        val urls = repository.getBibleReadingUrls(weekStart)

        println("Bible Reading URLs for $weekStart: ${urls.size} chapters")
        urls.forEachIndexed { index, url ->
            println("  [$index] $url")
        }

        assert(urls.isNotEmpty())
        urls.forEach { url ->
            assert(url.startsWith("https://"))
            assert(url.contains(".mp3"))
        }
    }

    @Test
    fun `getCongregationStudyUrls returns playlist`() = runBlocking {
        val weekStart = LocalDate.of(2025, 11, 10)
        val urls = repository.getCongregationStudyUrls(weekStart)

        println("Congregation Study URLs for $weekStart: ${urls.size} lessons")
        urls.forEachIndexed { index, url ->
            println("  [$index] $url")
        }

        assert(urls.isNotEmpty())
        urls.forEach { url ->
            assert(url.startsWith("https://"))
            assert(url.contains(".mp3"))
        }
    }
}
