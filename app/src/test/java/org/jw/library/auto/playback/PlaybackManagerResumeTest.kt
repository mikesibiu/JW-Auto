package org.jw.library.auto.playback

import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.jw.library.auto.data.model.MediaContent
import org.junit.Assert.assertEquals
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
