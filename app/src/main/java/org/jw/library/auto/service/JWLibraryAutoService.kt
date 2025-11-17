package org.jw.library.auto.service

import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.media.MediaBrowserServiceCompat
import org.jw.library.auto.data.ContentRepository
import org.jw.library.auto.data.model.MediaContent
import org.jw.library.auto.playback.PlaybackManager

class JWLibraryAutoService : MediaBrowserServiceCompat() {
    private lateinit var contentRepository: ContentRepository
    private lateinit var playbackManager: PlaybackManager

    override fun onCreate() {
        super.onCreate()
        contentRepository = ContentRepository(this)
        playbackManager = PlaybackManager(this)
        sessionToken = playbackManager.mediaSession.sessionToken
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        // Allow Android Auto and any whitelisted media clients to connect
        return BrowserRoot(ContentRepository.ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        val children = contentRepository.getChildren(parentId).map { content ->
            val description = MediaDescriptionCompat.Builder()
                .setMediaId(content.id)
                .setTitle(content.title)
                .setSubtitle(content.subtitle)
                .setExtras(Bundle().apply {
                    content.streamUrl?.let { putString(PlaybackManager.KEY_STREAM_URI, it) }
                    putString(MediaMetadataCompat.METADATA_KEY_TITLE, content.title)
                })
                .build()

            MediaBrowserCompat.MediaItem(
                description,
                if (content.isBrowsable) MediaBrowserCompat.MediaItem.FLAG_BROWSABLE else MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }

        result.sendResult(children.toMutableList())
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        super.onPlayFromMediaId(mediaId, extras)
        val streamUrl = extras?.getString(PlaybackManager.KEY_STREAM_URI)
        val title = extras?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        if (mediaId != null && streamUrl != null) {
            playbackManager.play(
                MediaContent(
                    id = mediaId,
                    title = title ?: mediaId,
                    streamUrl = streamUrl
                )
            )
            startForeground(NOTIFICATION_ID, playbackManager.buildNotification())
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        playbackManager.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return if (SERVICE_INTERFACE == intent?.action) super.onBind(intent) else null
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
