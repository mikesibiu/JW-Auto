package org.jw.library.auto.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.PlaybackException

/**
 * Manages audio playback using ExoPlayer and MediaSession
 */
class PlaybackManager(
    context: Context,
    private val mediaSession: MediaSessionCompat,
    private val onPlaybackStateChanged: (Int) -> Unit
) {

    // Use application context to avoid memory leaks
    private val appContext: Context = context.applicationContext
    private var exoPlayer: ExoPlayer? = null
    private var currentMediaUri: Uri? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    companion object {
        private const val TAG = "PlaybackManager"
    }

    /**
     * Audio focus change listener
     */
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Resume playback at full volume
                exoPlayer?.let {
                    it.volume = 1.0f
                    if (it.playbackState == Player.STATE_READY && !it.playWhenReady) {
                        it.playWhenReady = true
                    }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus permanently - pause
                exoPlayer?.playWhenReady = false
                setPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost focus temporarily - pause
                exoPlayer?.playWhenReady = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower volume temporarily (navigation, notifications)
                exoPlayer?.volume = 0.3f
            }
        }
    }

    init {
        initializePlayer()
    }

    /**
     * Initialize ExoPlayer
     */
    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(appContext).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    updatePlaybackState()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlaybackState()
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Playback error: ${error.message}", error)
                    setPlaybackState(PlaybackStateCompat.STATE_ERROR)
                }
            })
        }
    }

    /**
     * Get MediaSession callback
     */
    fun getMediaSessionCallback(): MediaSessionCompat.Callback {
        return object : MediaSessionCompat.Callback() {

            override fun onPlay() {
                if (requestAudioFocus()) {
                    exoPlayer?.let { player ->
                        player.playWhenReady = true
                        setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    }
                } else {
                    Log.w(TAG, "Failed to gain audio focus")
                    setPlaybackState(PlaybackStateCompat.STATE_ERROR)
                }
            }

            override fun onPause() {
                exoPlayer?.let { player ->
                    player.playWhenReady = false
                    setPlaybackState(PlaybackStateCompat.STATE_PAUSED)
                }
            }

            override fun onStop() {
                exoPlayer?.let { player ->
                    player.stop()
                    player.clearMediaItems()
                    setPlaybackState(PlaybackStateCompat.STATE_STOPPED)
                }
            }

            override fun onPlayFromMediaId(mediaId: String, extras: android.os.Bundle?) {
                // Parse the media description from extras
                val mediaUri = extras?.getString("mediaUri")
                val title = extras?.getString("title")?.take(500) // Limit length
                val subtitle = extras?.getString("subtitle")?.take(500)

                if (mediaUri != null) {
                    val validatedUri = validateMediaUri(mediaUri)
                    if (validatedUri != null) {
                        playFromUri(validatedUri, title, subtitle)
                    } else {
                        Log.e(TAG, "Invalid media URI rejected: $mediaUri")
                        setPlaybackState(PlaybackStateCompat.STATE_ERROR)
                    }
                }
            }

            override fun onPlayFromUri(uri: Uri, extras: android.os.Bundle?) {
                val validatedUri = validateMediaUri(uri.toString())
                if (validatedUri != null) {
                    val title = extras?.getString("title")?.take(500)
                    val subtitle = extras?.getString("subtitle")?.take(500)
                    playFromUri(validatedUri, title, subtitle)
                } else {
                    Log.e(TAG, "Invalid URI rejected: $uri")
                    setPlaybackState(PlaybackStateCompat.STATE_ERROR)
                }
            }

            override fun onSkipToNext() {
                // TODO: Implement playlist navigation
                Log.d(TAG, "Skip to next - not yet implemented")
            }

            override fun onSkipToPrevious() {
                // TODO: Implement playlist navigation
                Log.d(TAG, "Skip to previous - not yet implemented")
            }

            override fun onSeekTo(pos: Long) {
                exoPlayer?.seekTo(pos)
            }
        }
    }

    /**
     * Play audio from URI
     */
    private fun playFromUri(uri: Uri, title: String?, subtitle: String?) {
        exoPlayer?.let { player ->
            currentMediaUri = uri

            // Update metadata
            updateMetadata(title, subtitle)

            // Prepare media item
            val mediaItem = MediaItem.fromUri(uri)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true

            setPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
        }
    }

    /**
     * Update playback state based on player state
     */
    private fun updatePlaybackState() {
        exoPlayer?.let { player ->
            val state = when {
                player.playbackState == Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
                player.playbackState == Player.STATE_READY && player.playWhenReady -> PlaybackStateCompat.STATE_PLAYING
                player.playbackState == Player.STATE_READY && !player.playWhenReady -> PlaybackStateCompat.STATE_PAUSED
                player.playbackState == Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
                else -> PlaybackStateCompat.STATE_NONE
            }

            setPlaybackState(state, player.currentPosition)
        }
    }

    /**
     * Set playback state
     */
    private fun setPlaybackState(state: Int, position: Long = 0) {
        val actions = getAvailableActions(state)

        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, position, 1.0f)
            .setActions(actions)
            .build()

        mediaSession.setPlaybackState(playbackState)
        onPlaybackStateChanged(state)
    }

    /**
     * Get available actions based on current state
     */
    private fun getAvailableActions(state: Int): Long {
        var actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_URI or
                PlaybackStateCompat.ACTION_PLAY_PAUSE

        when (state) {
            PlaybackStateCompat.STATE_PLAYING -> {
                actions = actions or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                actions = actions or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO
            }
            PlaybackStateCompat.STATE_STOPPED,
            PlaybackStateCompat.STATE_NONE -> {
                actions = actions or PlaybackStateCompat.ACTION_PLAY
            }
        }

        return actions
    }

    /**
     * Update media metadata
     */
    private fun updateMetadata(title: String?, subtitle: String?) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title ?: "Unknown")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, subtitle ?: "JW Library")
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, subtitle)
            .putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                exoPlayer?.duration ?: 0
            )
            .build()

        mediaSession.setMetadata(metadata)
    }

    /**
     * Request audio focus
     */
    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusListener)
                .setWillPauseWhenDucked(false)
                .build()

            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    /**
     * Abandon audio focus
     */
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    /**
     * Validate media URI to prevent security issues
     */
    private fun validateMediaUri(uriString: String): Uri? {
        return try {
            val uri = Uri.parse(uriString)

            // Only allow HTTPS URIs from approved domains
            if (uri.scheme != "https") {
                Log.w(TAG, "Non-HTTPS URI rejected: $uri")
                return null
            }

            // Whitelist approved domains
            val allowedHosts = setOf(
                "jw.org",
                "download-a.akamaihd.net",
                "download.jw.org"
            )

            val host = uri.host?.lowercase() ?: return null
            val isAllowed = allowedHosts.any { allowedHost ->
                host == allowedHost || host.endsWith(".$allowedHost")
            }

            if (!isAllowed) {
                Log.w(TAG, "Unauthorized host rejected: $host")
                return null
            }

            uri
        } catch (e: Exception) {
            Log.e(TAG, "Invalid URI format: $uriString", e)
            null
        }
    }

    /**
     * Release resources
     */
    fun release() {
        abandonAudioFocus()
        try {
            exoPlayer?.let { player ->
                player.stop()
                player.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing ExoPlayer", e)
        } finally {
            exoPlayer = null
            currentMediaUri = null
        }
    }
}
