package org.jw.library.auto.playback

import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.jw.library.auto.data.model.MediaContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlaybackManagerResumeTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setup() {
        // Nothing needed for now
    }

    @Test
    fun reResolve_onPlay_withWeeklyId_andQueuedItems() {
        var capturedId: String? = null
        var capturedExtras: Bundle? = null
        val stateTransitions = mutableListOf<Int>()

        val playbackManager = PlaybackManager(
            context = context,
            onPlaybackStateChange = { state, _ -> stateTransitions.add(state) },
            onPlayFromMediaId = { id, extras ->
                capturedId = id
                capturedExtras = extras
            }
        )

        // Seed with a stale queue and weekly mediaId
        val staleUrls = listOf("https://example.invalid/audio/lfb_E_057.mp3")
        playbackManager.play(
            MediaContent(id = "this-cbs", title = "CBS", playlistUrls = staleUrls)
        )

        // Pause to simulate a stopped prior session
        playbackManager.mediaSession.controller.transportControls.pause()

        // Clear prior state transitions recorded during initial play/pause
        stateTransitions.clear()

        // Trigger resume path (onPlay without mediaId)
        playbackManager.handleOnPlay()

        assertEquals("this-cbs", capturedId)
        assertNotNull("onPlay should pass last position via extras", capturedExtras)

        // When we re-resolve, we early-return without directly toggling player state here
        assertTrue(
            "onPlaybackStateChange should not be called as part of resume re-resolve",
            stateTransitions.isEmpty()
        )
    }

    /**
     * Regression: broadcast items (jwb-*, gb-*) weren't playing because
     * onLoadChildren never put KEY_STREAM_URI in the MediaDescription extras.
     * The service's onPlayFromMediaId fallback read KEY_STREAM_URI → null → play() was
     * never called → nothing played.
     *
     * Tests that play() with a stream URL fires onPlaybackStateChange (the path that
     * was never reached before the fix), and that play() with no URLs is a no-op.
     */
    @Test
    fun play_withStreamUrl_firesPlaybackStateChange() {
        val stateTransitions = mutableListOf<Int>()
        val manager = PlaybackManager(
            context = context,
            onPlaybackStateChange = { state, _ -> stateTransitions.add(state) }
        )

        manager.play(MediaContent(id = "jwb-12345", title = "JW Broadcast",
            streamUrl = "https://example.com/broadcast.mp4"))

        assertFalse("Expected state change when stream URL is provided", stateTransitions.isEmpty())
    }

    @Test
    fun play_withNoUrl_doesNotFirePlaybackStateChange() {
        val stateTransitions = mutableListOf<Int>()
        val manager = PlaybackManager(
            context = context,
            onPlaybackStateChange = { state, _ -> stateTransitions.add(state) }
        )

        // No streamUrl and empty playlistUrls — simulates what happened before the fix when
        // KEY_STREAM_URI was absent from extras and the service's when{} had no matching branch
        manager.play(MediaContent(id = "jwb-12345", title = "JW Broadcast", streamUrl = null))

        assertTrue("Expected no state change when no URL is provided", stateTransitions.isEmpty())
    }

    @Test
    fun normalResume_onPlay_whenNoQueuedItems() {
        var capturedId: String? = null
        val stateTransitions = mutableListOf<Int>()

        val playbackManager = PlaybackManager(
            context = context,
            onPlaybackStateChange = { state, _ -> stateTransitions.add(state) },
            onPlayFromMediaId = { id, _ -> capturedId = id }
        )

        // No prior play() call means no queued items and no weekly id in metadata
        playbackManager.handleOnPlay()

        assertNull("onPlayFromMediaId should not be invoked when nothing is queued", capturedId)
        assertTrue("Playback state should be updated when resuming normally", stateTransitions.isNotEmpty())
    }
}
