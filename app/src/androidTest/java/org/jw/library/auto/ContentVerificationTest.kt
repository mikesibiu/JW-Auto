package org.jw.library.auto

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.jw.library.auto.service.JWLibraryAutoService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * On-device end-to-end content verification.
 *
 * Connects as a real MediaBrowser to JWLibraryAutoService, loads "This Week"
 * content, and asserts the CBS and bible reading items resolve correct filenames.
 *
 * Catches what unit tests cannot: runtime cache state, service startup sequence,
 * WeekCalculator using the real device date, and the full fallback chain.
 *
 * Run with: ./gradlew connectedDebugAndroidTest
 *
 * Rule: if a wrong URL reaches the user, reproduce it here first, then fix it.
 */
@RunWith(AndroidJUnit4::class)
class ContentVerificationTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val componentName = ComponentName(context, JWLibraryAutoService::class.java)

    @Test
    fun thisWeek_cbs_doesNotPlayLesson57() {
        val cbsUrls = loadCbsUrls()
        assertFalse(
            "CBS resolved lfb_E_057 (wrong lesson — this was the data-mapping bug). Got: $cbsUrls",
            cbsUrls.any { it.contains("lfb_E_057") }
        )
    }

    @Test
    fun thisWeek_cbs_isLesson68OrLater() {
        val cbsUrls = loadCbsUrls()
        assertTrue(
            "CBS lesson is lower than expected for the current week (week of Mar 9+). Got: $cbsUrls",
            cbsUrls.any { url ->
                val lesson = Regex("lfb_E_(\\d+)").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                lesson >= 68
            }
        )
    }

    @Test
    fun thisWeek_bibleReading_doesNotPlayIsaiah40OrEarlier() {
        val bibleUrls = loadBibleUrls()
        assertFalse(
            "Bible reading resolved Isaiah 40 or earlier (wrong chapter for current week). Got: $bibleUrls",
            bibleUrls.any { url ->
                val chapter = Regex("Isa_E_(\\d+)").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
                chapter <= 40
            }
        )
    }

    @Test
    fun thisWeek_allFourItemsPresent() {
        val ids = loadThisWeekItems().map { it.mediaId }
        assertTrue("Missing this-reading. Got: $ids", ids.any { it == "this-reading" })
        assertTrue("Missing this-watchtower. Got: $ids", ids.any { it == "this-watchtower" })
        assertTrue("Missing this-cbs. Got: $ids", ids.any { it == "this-cbs" })
        assertTrue("Missing this-workbook. Got: $ids", ids.any { it == "this-workbook" })
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun loadCbsUrls(): List<String> {
        val item = loadThisWeekItems().find { it.mediaId == "this-cbs" }
        assertNotNull("this-cbs item not found in This Week content", item)
        return urlsFromExtras(item!!)
    }

    private fun loadBibleUrls(): List<String> {
        val item = loadThisWeekItems().find { it.mediaId == "this-reading" }
        assertNotNull("this-reading item not found in This Week content", item)
        return urlsFromExtras(item!!)
    }

    private fun urlsFromExtras(item: MediaBrowserCompat.MediaItem): List<String> {
        val playlist = item.description.extras?.getStringArrayList("playlist_urls")?.toList()
        val single = item.description.extras?.getString("stream_uri")
        return playlist ?: listOfNotNull(single)
    }

    private fun loadThisWeekItems(): List<MediaBrowserCompat.MediaItem> {
        val latch = CountDownLatch(1)
        var result: List<MediaBrowserCompat.MediaItem>? = null
        var error: String? = null
        var browser: MediaBrowserCompat? = null

        val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                browser!!.subscribe(
                    "this_week",
                    object : MediaBrowserCompat.SubscriptionCallback() {
                        override fun onChildrenLoaded(
                            parentId: String,
                            children: MutableList<MediaBrowserCompat.MediaItem>
                        ) {
                            result = children
                            latch.countDown()
                        }
                        override fun onError(parentId: String) {
                            error = "onChildrenLoaded error for $parentId"
                            latch.countDown()
                        }
                        override fun onError(parentId: String, options: Bundle) {
                            error = "onChildrenLoaded error for $parentId"
                            latch.countDown()
                        }
                    }
                )
            }
            override fun onConnectionFailed() {
                error = "MediaBrowser connection failed"
                latch.countDown()
            }
        }

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            browser = MediaBrowserCompat(context, componentName, connectionCallback, null)
            browser!!.connect()
        }

        val completed = latch.await(20, TimeUnit.SECONDS)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { browser!!.disconnect() }

        assertTrue("Timed out waiting 20s for This Week content to load from service", completed)
        assertTrue("Service returned error: $error", error == null)
        assertNotNull("No items returned from This Week", result)
        return result!!
    }
}
