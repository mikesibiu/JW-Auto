package org.jw.library.auto.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.jw.library.auto.data.cache.CachedContent
import org.jw.library.auto.data.cache.ContentDatabase

/**
 * Tests for cache-bust correctness and stale-URL protection.
 *
 * Catches:
 * - Cache-bust not running or not clearing data
 * - Stale Room cache served instead of JSON fallback after version change
 * - Wrong CBS/bible chapter returned when cache is cold (guards against JSON data errors slipping through)
 *
 * Rule: whenever a cached-URL bug reaches the user, add a test here that would have caught it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CacheBustAndFetchTest {

    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        // Wipe cache and stored version before each test
        ContentDatabase.getDatabase(context).contentDao().deleteAll()
        context.getSharedPreferences("jw_app_state", Context.MODE_PRIVATE)
            .edit().remove("version_code").apply()
    }

    // ── Test 2: cache-bust clears stale data ─────────────────────────────────

    @Test
    fun `clearCacheIfVersionChanged wipes all cached content when version changes`() = runBlocking {
        val dao = ContentDatabase.getDatabase(context).contentDao()

        // Pre-populate with a stale wrong entry
        dao.insert(fakeCbsEntry("2026-03-09", """["https://wrong/lfb_E_057.mp3"]"""))
        assertEquals(1, dao.count())

        // stored version is -1 (cleared in setUp), current is BuildConfig.VERSION_CODE → triggers clear
        val repo = JWOrgRepository(context)
        withContext(Dispatchers.IO) { repo.clearCacheIfVersionChanged() }

        assertEquals("Cache should be empty after version-change bust", 0, dao.count())
    }

    @Test
    fun `clearCacheIfVersionChanged does NOT wipe cache when version is unchanged`() = runBlocking {
        val dao = ContentDatabase.getDatabase(context).contentDao()

        // Store the current version first so no change is detected
        val currentVersion = org.jw.library.auto.BuildConfig.VERSION_CODE
        context.getSharedPreferences("jw_app_state", Context.MODE_PRIVATE)
            .edit().putInt("version_code", currentVersion).apply()

        dao.insert(fakeCbsEntry("2026-03-09", """["https://cfp2.jw-cdn.org/a/x/1/o/lfb_E_068.mp3"]"""))
        assertEquals(1, dao.count())

        val repo = JWOrgRepository(context)
        withContext(Dispatchers.IO) { repo.clearCacheIfVersionChanged() }

        assertEquals("Cache should be untouched when version is unchanged", 1, dao.count())
    }

    // ── Test 3: cold cache returns correct URL from JSON ─────────────────────

    @Test
    fun `getCongregationStudyUrls returns lfb_068 and lfb_069 for mar_09 when cache is empty`() = runBlocking {
        val repo = JWOrgRepository(context)
        val urls = withContext(Dispatchers.IO) {
            repo.getCongregationStudyUrls(LocalDate.parse("2026-03-09"))
        }
        assertTrue("Expected lfb_E_068 in results: $urls", urls.any { it.contains("lfb_E_068") })
        assertTrue("Expected lfb_E_069 in results: $urls", urls.any { it.contains("lfb_E_069") })
    }

    @Test
    fun `getBibleReadingUrls returns Isa_43 and Isa_44 for mar_09 when cache is empty`() = runBlocking {
        val repo = JWOrgRepository(context)
        val urls = withContext(Dispatchers.IO) {
            repo.getBibleReadingUrls(LocalDate.parse("2026-03-09"))
        }
        assertTrue("Expected Isa_E_43 in results: $urls", urls.any { it.contains("Isa_E_43") })
        assertTrue("Expected Isa_E_44 in results: $urls", urls.any { it.contains("Isa_E_44") })
    }

    // ── Test 4: stale cached URL is not served after cache clear ──────────────

    @Test
    fun `getCongregationStudyUrls returns JSON data not stale cached URL after cache clear`() = runBlocking {
        val dao = ContentDatabase.getDatabase(context).contentDao()

        // Simulate what was in cache before the data fix
        dao.insert(fakeCbsEntry("2026-03-09", """["https://wrong/lfb_E_057.mp3"]"""))

        // Simulate cache-bust (version change wipes the DB)
        dao.deleteAll()

        // Next fetch must return correct JSON data, not the deleted stale entry
        val repo = JWOrgRepository(context)
        val urls = withContext(Dispatchers.IO) {
            repo.getCongregationStudyUrls(LocalDate.parse("2026-03-09"))
        }
        assertFalse("Stale lfb_E_057 must not be returned after cache clear", urls.any { it.contains("lfb_E_057") })
        assertTrue("Fresh lfb_E_068 must be returned from JSON", urls.any { it.contains("lfb_E_068") })
    }

    @Test
    fun `getBibleReadingUrls returns JSON data not stale cached URL after cache clear`() = runBlocking {
        val dao = ContentDatabase.getDatabase(context).contentDao()

        // Simulate the wrong Isaiah 40 that was cached before the data fix
        val key = CachedContent.cacheKey(CachedContent.TYPE_BIBLE_READING, "2026-03-09")
        dao.insert(CachedContent(
            cacheKey = key,
            contentType = CachedContent.TYPE_BIBLE_READING,
            weekStart = "2026-03-09",
            url = null,
            playlistUrls = """["https://wrong/bi12_23_Isa_E_40.mp3"]""",
            fetchedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + CachedContent.TTL_FUTURE_MILLIS
        ))

        dao.deleteAll()

        val repo = JWOrgRepository(context)
        val urls = withContext(Dispatchers.IO) {
            repo.getBibleReadingUrls(LocalDate.parse("2026-03-09"))
        }
        assertFalse("Stale Isa_E_40 must not be returned", urls.any { it.contains("Isa_E_40") })
        assertTrue("Correct Isa_E_43 must be returned from JSON", urls.any { it.contains("Isa_E_43") })
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun fakeCbsEntry(weekStart: String, playlistJson: String) = CachedContent(
        cacheKey = CachedContent.cacheKey(CachedContent.TYPE_CONGREGATION_STUDY, weekStart),
        contentType = CachedContent.TYPE_CONGREGATION_STUDY,
        weekStart = weekStart,
        url = null,
        playlistUrls = playlistJson,
        fetchedAt = System.currentTimeMillis(),
        expiresAt = System.currentTimeMillis() + CachedContent.TTL_FUTURE_MILLIS
    )
}
