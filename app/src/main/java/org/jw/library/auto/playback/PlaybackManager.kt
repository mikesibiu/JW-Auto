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
    private val onPlaybackStateChange: (Int) -> Unit
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

    val mediaSession: MediaSessionCompat = MediaSessionCompat(context, "JWLibraryAuto").apply {
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlayFromMediaId(mediaId: String?, extras: android.os.Bundle?) {
                mediaId?.let { id ->
                    val title = extras?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: id
                    val playlist = extras?.getStringArrayList(KEY_PLAYLIST)
                    when {
                        !playlist.isNullOrEmpty() ->
                            play(MediaContent(id = id, title = title, playlistUrls = playlist))
                        extras?.getString(KEY_STREAM_URI) != null -> {
                            val uri = extras.getString(KEY_STREAM_URI)
                            play(MediaContent(id = id, title = title, streamUrl = uri))
                        }
                    }
                }
            }

            override fun onPlay() {
                player.playWhenReady = true
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            }

            override fun onPause() {
                player.playWhenReady = false
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }

            override fun onStop() {
                player.stop()
                updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
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
        createNotificationChannel()
    }

    fun release() {
        mediaSession.release()
        player.release()
    }

    fun play(content: MediaContent) {
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
        player.playWhenReady = true
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, content.id)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, content.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, content.subtitle ?: context.getString(R.string.app_name))
                .build()
        )
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }

    fun buildRootPlaybackState() = PlaybackStateCompat.Builder()
        .setActions(
            PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP
        )
        .setState(PlaybackStateCompat.STATE_PAUSED, 0L, 1.0f)
        .build()

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
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, player.currentPosition, 1.0f)
                .build()
        )
        onPlaybackStateChange(state)
    }

    companion object {
        const val CHANNEL_ID = "jwlibraryauto.playback"
        const val KEY_STREAM_URI = "stream_uri"
        const val KEY_PLAYLIST = "playlist_urls"
        private const val TAG = "PlaybackManager"
    }
}
