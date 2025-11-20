package org.jw.library.auto.service

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import android.util.Log
import org.jw.library.auto.background.ContentSyncScheduler
import org.jw.library.auto.data.ContentRepository
import org.jw.library.auto.playback.PlaybackManager
import java.util.ArrayList

class JWLibraryAutoService : MediaBrowserServiceCompat() {
    private lateinit var contentRepository: ContentRepository
    private lateinit var playbackManager: PlaybackManager

    override fun onCreate() {
        super.onCreate()
        contentRepository = ContentRepository(this)
        playbackManager = PlaybackManager(this) { state ->
            when (state) {
                PlaybackStateCompat.STATE_PLAYING ->
                    startForeground(NOTIFICATION_ID, playbackManager.buildNotification())
                PlaybackStateCompat.STATE_PAUSED ->
                    stopForegroundCompat(removeNotification = false)
                PlaybackStateCompat.STATE_STOPPED ->
                    stopForegroundCompat(removeNotification = true)
            }
        }
        sessionToken = playbackManager.mediaSession.sessionToken

        // Schedule background content sync
        ContentSyncScheduler.schedulePeriodicSync(this)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return if (isClientAllowed(clientPackageName, clientUid)) {
            BrowserRoot(ContentRepository.ROOT_ID, null)
        } else {
            null
        }
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
                    if (content.playlistUrls.isNotEmpty()) {
                        putStringArrayList(
                            PlaybackManager.KEY_PLAYLIST,
                            ArrayList(content.playlistUrls)
                        )
                    }
                })
                .build()

            MediaBrowserCompat.MediaItem(
                description,
                if (content.isBrowsable) MediaBrowserCompat.MediaItem.FLAG_BROWSABLE else MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }

        result.sendResult(children.toMutableList())
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
        private val ALLOWED_CLIENT_PREFIXES = setOf(
            "com.google.android.projection.gearhead", // Android Auto host + validator processes
            "com.google.android.car.kitvalidator",    // Validation utility
            "com.google.android.googlequicksearchbox", // Assistant
            "com.google.android.gms" // Play Services sometimes mediates
        )
        private const val TAG = "JWLibraryAutoService"
    }

    private fun stopForegroundCompat(removeNotification: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(removeNotification)
        }
    }

    private fun isClientAllowed(clientPackage: String?, clientUid: Int): Boolean {
        if (clientPackage == null) return false
        if (clientPackage == packageName) return true
        if (ALLOWED_CLIENT_PREFIXES.none { clientPackage.startsWith(it) }) {
            Log.w(TAG, "Rejecting media client $clientPackage")
            return false
        }
        val packagesForUid = packageManager.getPackagesForUid(clientUid) ?: return false
        return packagesForUid.contains(clientPackage)
    }
}
