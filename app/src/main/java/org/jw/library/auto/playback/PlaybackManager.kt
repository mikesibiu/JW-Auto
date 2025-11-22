package org.jw.library.auto.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.jw.library.auto.R
import org.jw.library.auto.data.model.MediaContent

class PlaybackManager(
    private val context: Context,
    private val onPlaybackStateChange: (Int, Long) -> Unit
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        .build()

    private fun skipBy(offsetMs: Long) {
        val rawDuration = player.duration.takeIf { it > 0 } ?: C.TIME_UNSET
        val target = (player.currentPosition + offsetMs).coerceAtLeast(0L)
        val clamped = if (rawDuration != C.TIME_UNSET) target.coerceAtMost(rawDuration) else target
        player.seekTo(clamped)
        updatePlaybackState(currentStateFromPlayer())
    }

    private val skipBackCustomAction: PlaybackStateCompat.CustomAction by lazy {
        PlaybackStateCompat.CustomAction.Builder(
            ACTION_SKIP_BACK,
            context.getString(R.string.action_rewind_30),
            android.R.drawable.ic_media_rew
        ).build()
    }

    private val skipForwardCustomAction: PlaybackStateCompat.CustomAction by lazy {
        PlaybackStateCompat.CustomAction.Builder(
            ACTION_SKIP_FORWARD,
            context.getString(R.string.action_forward_2_min),
            android.R.drawable.ic_media_ff
        ).build()
    }

    private fun currentStateFromPlayer(): Int {
        return when (player.playbackState) {
            Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            Player.STATE_READY -> if (player.playWhenReady) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
            Player.STATE_IDLE -> if (player.playWhenReady) PlaybackStateCompat.STATE_CONNECTING else PlaybackStateCompat.STATE_NONE
            else -> PlaybackStateCompat.STATE_NONE
        }
    }

    private fun maybeUpdateMetadataDuration() {
        val duration = player.duration
        if (duration <= 0) return
        val current = mediaSession.controller.metadata ?: return
        if (current.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) == duration) return
        val updated = MediaMetadataCompat.Builder(current)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            .build()
        mediaSession.setMetadata(updated)
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback error", error)
                updatePlaybackState(PlaybackStateCompat.STATE_ERROR)
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                    events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) ||
                    events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
                ) {
                    updatePlaybackState(currentStateFromPlayer())
                }
                if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                    events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)
                ) {
                    maybeUpdateMetadataDuration()
                }
            }
        })
        createNotificationChannel()
    }

    val mediaSession: MediaSessionCompat = MediaSessionCompat(context, "JWLibraryAuto").apply {
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlayFromMediaId(mediaId: String?, extras: android.os.Bundle?) {
                mediaId?.let { id ->
                    val title = extras?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: id
                    val playlist = extras?.getStringArrayList(KEY_PLAYLIST)
                    val lastPosition = extras?.getLong(KEY_LAST_POSITION, 0L) ?: 0L
                    when {
                        !playlist.isNullOrEmpty() ->
                            play(MediaContent(id = id, title = title, playlistUrls = playlist), lastPosition)
                        extras?.getString(KEY_STREAM_URI) != null -> {
                            val uri = extras.getString(KEY_STREAM_URI)
                            play(MediaContent(id = id, title = title, streamUrl = uri), lastPosition)
                        }
                    }
                }
            }

            override fun onPlay() {
                player.playWhenReady = true
                updatePlaybackState(currentStateFromPlayer())
            }

            override fun onPause() {
                player.playWhenReady = false
                updatePlaybackState(currentStateFromPlayer())
            }

            override fun onStop() {
                player.stop()
                updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
            }

            override fun onFastForward() {
                skipBy(SKIP_FORWARD_MS)
            }

            override fun onRewind() {
                skipBy(-SKIP_BACK_MS)
            }

            override fun onSeekTo(pos: Long) {
                player.seekTo(pos.coerceAtLeast(0L))
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            }

            override fun onCustomAction(action: String?, extras: android.os.Bundle?) {
                when (action) {
                    ACTION_SKIP_BACK -> skipBy(-SKIP_BACK_MS)
                    ACTION_SKIP_FORWARD -> skipBy(SKIP_FORWARD_MS)
                }
            }
        })
        setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        setPlaybackState(buildRootPlaybackState())
        isActive = true
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback error", error)
                updatePlaybackState(PlaybackStateCompat.STATE_ERROR)
            }
        })
    }

    fun release() {
        mediaSession.release()
        player.release()
    }

    fun play(content: MediaContent, startPositionMs: Long = 0L) {
        val playlist = when {
            content.playlistUrls.isNotEmpty() -> content.playlistUrls
            content.streamUrl != null -> listOf(content.streamUrl)
            else -> emptyList()
        }
        if (playlist.isEmpty()) {
            Log.w(TAG, "No media URLs found for ${content.id}")
            return
        }

        val mediaItems = playlist.mapIndexed { index, uri ->
            MediaItem.Builder()
                .setMediaId("${content.id}-$index")
                .setUri(uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(content.title)
                        .setSubtitle(content.subtitle)
                        .build()
                )
                .build()
        }
        player.setMediaItems(mediaItems)
        player.prepare()
        if (startPositionMs > 0L) {
            player.seekTo(startPositionMs)
        }
        player.playWhenReady = true
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, content.id)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, content.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, content.subtitle ?: context.getString(R.string.app_name))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1L)
                .build()
        )
        updatePlaybackState(currentStateFromPlayer())
    }

    fun buildRootPlaybackState(): PlaybackStateCompat = buildPlaybackState(
        PlaybackStateCompat.STATE_PAUSED,
        0L
    )

    fun buildNotification(): Notification {
        val controller = mediaSession.controller
        val mediaStyle = MediaStyle().setMediaSession(mediaSession.sessionToken)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(controller.metadata?.description?.title ?: context.getString(R.string.app_name))
            .setContentText(controller.metadata?.description?.subtitle)
            .setSmallIcon(R.drawable.ic_notification)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(controller.sessionActivity)
            .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_rew,
                    context.getString(R.string.action_rewind_30),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_REWIND)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_pause,
                    context.getString(R.string.action_pause),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PAUSE)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_stop,
                    context.getString(R.string.action_stop),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_ff,
                    context.getString(R.string.action_forward_2_min),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_FAST_FORWARD)
                )
            )
            .setStyle(mediaStyle)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updatePlaybackState(state: Int) {
        mediaSession.setPlaybackState(
            buildPlaybackState(state, player.currentPosition)
        )
        onPlaybackStateChange(state, player.currentPosition)
    }

    private fun buildPlaybackState(state: Int, position: Long): PlaybackStateCompat {
        val builder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_FAST_FORWARD or
                    PlaybackStateCompat.ACTION_REWIND or
                    PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, position, 1.0f)
            .addCustomAction(skipBackCustomAction)
            .addCustomAction(skipForwardCustomAction)
        return builder.build()
    }

    companion object {
        const val CHANNEL_ID = "jwlibraryauto.playback"
        const val KEY_STREAM_URI = "stream_uri"
        const val KEY_PLAYLIST = "playlist_urls"
        const val KEY_LAST_POSITION = "last_position_ms"
        private const val TAG = "PlaybackManager"
        private const val SKIP_FORWARD_MS = 2 * 60 * 1000L
        private const val SKIP_BACK_MS = 30 * 1000L
        private const val ACTION_SKIP_BACK = "org.jw.library.auto.action.SKIP_BACK"
        private const val ACTION_SKIP_FORWARD = "org.jw.library.auto.action.SKIP_FORWARD"
    }

}
